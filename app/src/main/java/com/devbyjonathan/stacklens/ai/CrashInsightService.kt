package com.devbyjonathan.stacklens.ai

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.devbyjonathan.stacklens.R
import com.devbyjonathan.stacklens.data.local.dao.CrashInsightDao
import com.devbyjonathan.stacklens.data.local.entity.CrashInsightEntity
import com.devbyjonathan.stacklens.model.CrashLog
import com.devbyjonathan.stacklens.model.CrashTypeFilter
import com.devbyjonathan.stacklens.model.SortOrder
import com.devbyjonathan.stacklens.service.CrashSignatureGenerator
import com.devbyjonathan.stacklens.util.countAppFrames
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

enum class Severity { LOW, MEDIUM, HIGH }

data class CrashInsight(
    val title: String,
    val summary: String,
    val rootCause: String,
    val suggestedFix: String,
    val affectedLine: String?,
    val severity: Severity = Severity.MEDIUM,
    val confidence: Float = 0.7f,
)

sealed class InsightResult {
    data class Success(val insight: CrashInsight) : InsightResult()
    data class Error(val message: String) : InsightResult()
    data object Unavailable : InsightResult()
    data object Loading : InsightResult()
    data object Downloading : InsightResult()
}

sealed class DownloadState {
    data object Idle : DownloadState()
    data object Starting : DownloadState()
    data class InProgress(val bytesDownloaded: Long) : DownloadState()
    data object Completed : DownloadState()
    data class Failed(val error: String) : DownloadState()
}

data class ParsedSearchQuery(
    val timeRangeHours: Int? = null,
    val typeFilter: CrashTypeFilter? = null,
    val searchQuery: String? = null,
    val packageName: String? = null,
    val sortOrder: SortOrder? = null,
)

sealed class ParseResult {
    data class Success(val query: ParsedSearchQuery) : ParseResult()
    data class Fallback(val originalQuery: String) : ParseResult()
    data object Unavailable : ParseResult()
}

@Singleton
class CrashInsightService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val crashInsightDao: CrashInsightDao,
    private val signatureGenerator: CrashSignatureGenerator,
) {
    private var generativeModel: GenerativeModel? = null

    @FeatureStatus
    private var currentStatus: Int = FeatureStatus.UNAVAILABLE

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val inFlightMutex = Mutex()
    private val inFlight = mutableMapOf<Long, Deferred<InsightResult>>()

    companion object {
        private const val TAG = "CrashInsightService"
        private const val NOTIFICATION_CHANNEL_ID = "gemini_nano_download"
        private const val NOTIFICATION_ID = 9001
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "AI Model Download",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows download progress for Gemini Nano AI model"
                setShowBadge(false)
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created: $NOTIFICATION_CHANNEL_ID")
        }
    }

    /**
     * Check if on-device AI (Gemini Nano) is available.
     * Returns true if the feature is ready to use.
     */
    suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Checking Gemini Nano availability...")
            val model = getOrCreateModel()
            currentStatus = model.checkStatus()
            Log.d(TAG, "Gemini Nano status: ${statusToString(currentStatus)}")
            currentStatus == FeatureStatus.AVAILABLE
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check Gemini Nano availability", e)
            false
        }
    }

    /**
     * Get current feature status for UI display.
     */
    @FeatureStatus
    fun getStatus(): Int = currentStatus

    private fun statusToString(@FeatureStatus status: Int): String {
        return when (status) {
            FeatureStatus.AVAILABLE -> "AVAILABLE"
            FeatureStatus.DOWNLOADABLE -> "DOWNLOADABLE"
            FeatureStatus.DOWNLOADING -> "DOWNLOADING"
            FeatureStatus.UNAVAILABLE -> "UNAVAILABLE"
            else -> "UNKNOWN($status)"
        }
    }

    /**
     * Start downloading the model in background with progress notifications.
     * This can continue even if the user leaves the screen.
     */
    fun startBackgroundDownload() {
        serviceScope.launch {
            downloadModelWithProgress()
        }
    }

    /**
     * Download the model if it's in DOWNLOADABLE state.
     * Shows progress via notifications and state flow.
     */
    suspend fun downloadModelWithProgress(): Boolean = withContext(Dispatchers.IO) {
        try {
            val model = getOrCreateModel()
            val status = model.checkStatus()
            Log.d(TAG, "Download requested. Current status: ${statusToString(status)}")

            if (status == FeatureStatus.AVAILABLE) {
                Log.d(TAG, "Model already available, no download needed")
                _downloadState.value = DownloadState.Completed
                return@withContext true
            }

            if (status != FeatureStatus.DOWNLOADABLE && status != FeatureStatus.DOWNLOADING) {
                Log.w(TAG, "Cannot download - status is ${statusToString(status)}")
                _downloadState.value =
                    DownloadState.Failed("Model not available for download on this device")
                return@withContext false
            }

            _downloadState.value = DownloadState.Starting
            showDownloadNotification("Starting download...", 0, true)
            Log.d(TAG, "Starting Gemini Nano model download...")

            model.download().collect { downloadStatus ->
                when (downloadStatus) {
                    is DownloadStatus.DownloadStarted -> {
                        Log.d(TAG, "Download started")
                        _downloadState.value = DownloadState.Starting
                        showDownloadNotification("Download started...", 0, true)
                    }

                    is DownloadStatus.DownloadProgress -> {
                        val bytes = downloadStatus.totalBytesDownloaded
                        val megabytes = bytes / (1024.0 * 1024.0)
                        Log.d(
                            TAG,
                            "Download progress: ${
                                String.format(
                                    java.util.Locale.US,
                                    "%.2f",
                                    megabytes
                                )
                            } MB downloaded"
                        )
                        _downloadState.value = DownloadState.InProgress(bytes)
                        showDownloadNotification(
                            "Downloading: ${
                                String.format(
                                    java.util.Locale.US,
                                    "%.1f",
                                    megabytes
                                )
                            } MB",
                            -1, // Indeterminate since we don't know total size
                            true
                        )
                    }

                    DownloadStatus.DownloadCompleted -> {
                        Log.d(TAG, "Download completed successfully!")
                        _downloadState.value = DownloadState.Completed
                        showDownloadNotification("Download complete!", 100, false)
                        // Dismiss notification after a delay
                        kotlinx.coroutines.delay(2000)
                        cancelDownloadNotification()
                    }

                    is DownloadStatus.DownloadFailed -> {
                        Log.e(TAG, "Download failed: $downloadStatus")
                        _downloadState.value = DownloadState.Failed("Download failed")
                        showDownloadNotification("Download failed", 0, false)
                    }
                }
            }

            currentStatus = model.checkStatus()
            Log.d(TAG, "Post-download status: ${statusToString(currentStatus)}")
            currentStatus == FeatureStatus.AVAILABLE
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download Gemini Nano model", e)
            _downloadState.value = DownloadState.Failed(e.message ?: "Download failed")
            showDownloadNotification("Download failed: ${e.message}", 0, false)
            false
        }
    }

    private fun showDownloadNotification(message: String, progress: Int, ongoing: Boolean) {
        // Check notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "No notification permission, skipping notification")
                return
            }
        }

        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Gemini Nano AI Model")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(ongoing)
            .setAutoCancel(!ongoing)

        when {
            progress == 100 -> {
                // Completed - remove progress bar
                builder.setProgress(0, 0, false)
            }

            progress < 0 -> {
                // Indeterminate progress
                builder.setProgress(0, 0, true)
            }

            else -> {
                // Determinate progress
                builder.setProgress(100, progress, false)
            }
        }

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            Log.w(TAG, "Cannot show notification - permission denied", e)
        }
    }

    private fun cancelDownloadNotification() {
        try {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cancel notification", e)
        }
    }

    private fun getOrCreateModel(): GenerativeModel {
        return generativeModel ?: Generation.getClient().also {
            generativeModel = it
            Log.d(TAG, "GenerativeModel client created")
        }
    }

    /**
     * Analyze a crash log and return AI-powered insights.
     * First checks for cached insight, otherwise calls AI and caches the result.
     * Concurrent calls for the same crash id share a single in-flight generation.
     *
     * @param groupCount number of occurrences of this crash pattern; feeds into the
     *                   deterministic confidence heuristic.
     */
    suspend fun analyzeCrash(crash: CrashLog, groupCount: Int = 1): InsightResult {
        val deferred = inFlightMutex.withLock {
            inFlight[crash.id] ?: serviceScope.async {
                doAnalyzeCrash(crash, groupCount)
            }.also { job ->
                inFlight[crash.id] = job
                job.invokeOnCompletion {
                    serviceScope.launch {
                        inFlightMutex.withLock { inFlight.remove(crash.id) }
                    }
                }
            }
        }
        return deferred.await()
    }

    /**
     * Kick off analysis in the background so the insight is cached by the time
     * the user opens the AI screen. Safe to call multiple times — in-flight
     * dedupe ensures only one generation runs per crash id.
     */
    fun preloadInsight(crash: CrashLog, groupCount: Int = 1) {
        serviceScope.launch {
            runCatching { analyzeCrash(crash, groupCount) }
                .onFailure { Log.w(TAG, "preloadInsight failed for crash ${crash.id}", it) }
        }
    }

    private suspend fun doAnalyzeCrash(crash: CrashLog, groupCount: Int): InsightResult =
        withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting crash analysis for: ${crash.packageName} (id=${crash.id})")

            // Check for cached insight first
            val cachedInsight = crashInsightDao.getInsightForCrash(crash.id)
            if (cachedInsight != null) {
                Log.d(TAG, "Found cached insight for crash ${crash.id}")
                return@withContext InsightResult.Success(cachedInsight.toCrashInsight())
            }
            Log.d(TAG, "No cached insight found, proceeding with AI analysis")

            val model = getOrCreateModel()
            val status = model.checkStatus()
            currentStatus = status
            Log.d(TAG, "Model status before analysis: ${statusToString(status)}")

            when (status) {
                FeatureStatus.UNAVAILABLE -> {
                    Log.w(TAG, "Gemini Nano unavailable on this device")
                    return@withContext InsightResult.Unavailable
                }

                FeatureStatus.DOWNLOADABLE -> {
                    Log.d(TAG, "Model downloadable - starting background download")
                    startBackgroundDownload()
                    return@withContext InsightResult.Downloading
                }

                FeatureStatus.DOWNLOADING -> {
                    Log.d(TAG, "Model currently downloading")
                    return@withContext InsightResult.Downloading
                }

                FeatureStatus.AVAILABLE -> {
                    Log.d(TAG, "Model available - proceeding with analysis")
                }
            }

            val prompt = buildPrompt(crash)
            Log.d(TAG, "Prompt built, length: ${prompt.length} chars")

            val request = generateContentRequest(TextPart(prompt)) {
                temperature = 0.2f
                topK = 16
            }
            Log.d(TAG, "Request created with temperature=0.2, topK=16")

            Log.d(TAG, "Calling generateContent...")
            val startTime = System.currentTimeMillis()
            val response = model.generateContent(request)
            val elapsed = System.currentTimeMillis() - startTime
            Log.d(TAG, "generateContent completed in ${elapsed}ms")

            val text = response.candidates.firstOrNull()?.text
            if (text == null) {
                Log.w(TAG, "Response has no text. Candidates: ${response.candidates.size}")
                return@withContext InsightResult.Error("Empty response from AI")
            }

            Log.d(TAG, "AI Response received (${text.length} chars):\n$text")
            val result = parseInsightResponse(text, crash, groupCount)

            // Cache successful insight
            if (result is InsightResult.Success) {
                val entity = CrashInsightEntity.fromCrashInsight(crash.id, result.insight)
                crashInsightDao.insert(entity)
                Log.d(TAG, "Cached insight for crash ${crash.id}")
            }

            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to analyze crash", e)
            InsightResult.Error(e.message ?: "Failed to analyze crash")
        }
    }

    /**
     * Get cached insight for a crash if available.
     */
    suspend fun getCachedInsight(crashId: Long): CrashInsight? = withContext(Dispatchers.IO) {
        crashInsightDao.getInsightForCrash(crashId)?.toCrashInsight()
    }

    /**
     * Check if a crash has a cached insight.
     */
    suspend fun hasCachedInsight(crashId: Long): Boolean = withContext(Dispatchers.IO) {
        crashInsightDao.getInsightForCrash(crashId) != null
    }

    private fun buildPrompt(crash: CrashLog): String {
        val stackTrace = crash.content.take(1500) // Limit to avoid token limits

        return """
Analyze this Android crash log and provide insights.

Crash Type: ${crash.tag.displayName}
App: ${crash.appName ?: crash.packageName ?: "Unknown"}
Package: ${crash.packageName ?: "Unknown"}

Stack Trace:
$stackTrace

Respond in this exact format:

TITLE: [A short 4–8 word headline naming the issue — noun phrase, no trailing punctuation]

SUMMARY: [One sentence explaining what happened]

ROOT_CAUSE: [The specific cause of the crash]

SUGGESTED_FIX: [How to fix this issue]

AFFECTED_LINE: [The key line from the stack trace, or N/A]

SEVERITY: [HIGH, MEDIUM, or LOW — judge user-facing impact]
""".trimIndent()
    }

    private fun parseInsightResponse(
        response: String,
        crash: CrashLog,
        groupCount: Int,
    ): InsightResult {
        try {
            val summary = extractSection(response, "SUMMARY:") ?: "Unable to determine summary"
            val rootCause =
                extractSection(response, "ROOT_CAUSE:") ?: "Unable to determine root cause"
            val suggestedFix = extractSection(response, "SUGGESTED_FIX:") ?: "Unable to suggest fix"
            val affectedLine = extractSection(response, "AFFECTED_LINE:")?.takeIf {
                it.isNotBlank() && !it.equals("N/A", ignoreCase = true)
            }?.trim()
            val severity = parseSeverity(extractSection(response, "SEVERITY:"))
            val title = extractSection(response, "TITLE:")
                ?.trim()
                ?.trimEnd('.', ',', ';', ':')
                ?.takeIf { it.isNotBlank() }
                ?: deriveFallbackTitle(summary)

            val exceptionType = signatureGenerator.extractExceptionType(crash.content, crash.tag)
            val appFrames = countAppFrames(crash.content)
            val confidence = computeConfidence(
                exceptionType = exceptionType,
                affectedLine = affectedLine,
                appFrameCount = appFrames,
                groupCount = groupCount,
            )

            Log.d(
                TAG,
                "Parsed insight — title='${title.take(40)}' severity=$severity confidence=$confidence appFrames=$appFrames groupCount=$groupCount"
            )
            return InsightResult.Success(
                CrashInsight(
                    title = title,
                    summary = summary.trim(),
                    rootCause = rootCause.trim(),
                    suggestedFix = suggestedFix.trim(),
                    affectedLine = affectedLine,
                    severity = severity,
                    confidence = confidence,
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse AI response", e)
            return InsightResult.Error("Failed to parse AI response")
        }
    }

    /**
     * Last-resort title if the model omits TITLE or emits blank. Take the first clause of the
     * summary up to ~8 words.
     */
    private fun deriveFallbackTitle(summary: String): String {
        val firstClause = summary.substringBefore('.').substringBefore(';').trim()
        val words = firstClause.split(Regex("\\s+")).filter { it.isNotBlank() }
        return if (words.size <= 8) firstClause else words.take(8).joinToString(" ")
    }

    private fun parseSeverity(raw: String?): Severity {
        return when (raw?.trim()?.uppercase()) {
            "HIGH" -> Severity.HIGH
            "LOW" -> Severity.LOW
            "MEDIUM", "MED" -> Severity.MEDIUM
            else -> Severity.MEDIUM
        }
    }

    /**
     * Deterministic confidence in [0f, 1f]. Pure function of parse quality + trace shape +
     * how often this pattern recurs. No probing of model internals.
     */
    internal fun computeConfidence(
        exceptionType: String,
        affectedLine: String?,
        appFrameCount: Int,
        groupCount: Int,
    ): Float {
        var score = 0.50f
        if (exceptionType != "UnknownException") score += 0.15f
        if (affectedLine != null) score += 0.10f
        score += 0.05f * minOf(4, appFrameCount)
        if (groupCount >= 3) score += 0.05f
        return score.coerceIn(0f, 1f)
    }

    private fun extractSection(text: String, marker: String): String? {
        val startIndex = text.indexOf(marker, ignoreCase = true)
        if (startIndex == -1) return null

        val contentStart = startIndex + marker.length
        val markers = listOf(
            "TITLE:",
            "SUMMARY:",
            "ROOT_CAUSE:",
            "SUGGESTED_FIX:",
            "AFFECTED_LINE:",
            "SEVERITY:"
        )
        val nextMarkerIndex = markers
            .filter { !it.equals(marker, ignoreCase = true) }
            .mapNotNull { nextMarker ->
                val idx = text.indexOf(nextMarker, contentStart, ignoreCase = true)
                if (idx > 0) idx else null
            }
            .minOrNull() ?: text.length

        return text.substring(contentStart, nextMarkerIndex).trim()
    }

    /**
     * Parse a natural language search query into structured filters.
     * Returns Fallback if AI parsing fails or nothing meaningful is extracted.
     */
    suspend fun parseNaturalLanguageQuery(query: String): ParseResult =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Parsing natural language query: $query")

                val model = getOrCreateModel()
                val status = model.checkStatus()
                currentStatus = status

                if (status != FeatureStatus.AVAILABLE) {
                    Log.d(TAG, "Gemini Nano not available for query parsing")
                    return@withContext ParseResult.Unavailable
                }

                val prompt = buildQueryParsePrompt(query)
                Log.d(TAG, "Query parse prompt built, length: ${prompt.length} chars")

                val request = generateContentRequest(TextPart(prompt)) {
                    temperature = 0.1f
                    topK = 8
                }

                Log.d(TAG, "Calling generateContent for query parsing...")
                val startTime = System.currentTimeMillis()
                val response = model.generateContent(request)
                val elapsed = System.currentTimeMillis() - startTime
                Log.d(TAG, "Query parsing completed in ${elapsed}ms")

                val text = response.candidates.firstOrNull()?.text
                if (text == null) {
                    Log.w(TAG, "Empty response for query parsing")
                    return@withContext ParseResult.Fallback(query)
                }

                Log.d(TAG, "Query parse response:\n$text")
                parseQueryResponse(text, query)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse natural language query", e)
                ParseResult.Fallback(query)
            }
        }

    private fun buildQueryParsePrompt(query: String): String {
        return """
Parse this crash log search query into structured filters.

Query: "$query"

Extract these fields if mentioned (use NONE if not mentioned):
- TIME_RANGE: Number of hours (1, 6, 24, 72, 168). Map: "last hour"=1, "today"/"yesterday"=24, "3 days"=72, "week"=168
- TYPE_FILTER: One of ALL, CRASHES, ANRS, NATIVE
- SEARCH_QUERY: Exception names, error types (e.g., NullPointerException, OutOfMemory)
- PACKAGE_HINT: App name or package mentioned (e.g., "Gmail", "Chrome")
- SORT_ORDER: NEWEST_FIRST or OLDEST_FIRST

Respond in this exact format:
TIME_RANGE: [value or NONE]
TYPE_FILTER: [value or NONE]
SEARCH_QUERY: [value or NONE]
PACKAGE_HINT: [value or NONE]
SORT_ORDER: [value or NONE]
""".trimIndent()
    }

    private fun parseQueryResponse(response: String, originalQuery: String): ParseResult {
        try {
            val timeRangeStr = extractQueryField(response, "TIME_RANGE:")
            val typeFilterStr = extractQueryField(response, "TYPE_FILTER:")
            val searchQueryStr = extractQueryField(response, "SEARCH_QUERY:")
            val packageHintStr = extractQueryField(response, "PACKAGE_HINT:")
            val sortOrderStr = extractQueryField(response, "SORT_ORDER:")

            val timeRange = timeRangeStr?.toIntOrNull()
            val typeFilter = when (typeFilterStr?.uppercase()) {
                "ALL" -> CrashTypeFilter.ALL
                "CRASHES" -> CrashTypeFilter.CRASHES
                "ANRS" -> CrashTypeFilter.ANRS
                "NATIVE" -> CrashTypeFilter.NATIVE
                else -> null
            }
            val searchQuery = searchQueryStr?.takeIf { it.isNotBlank() }
            val packageName = packageHintStr?.takeIf { it.isNotBlank() }
            val sortOrder = when (sortOrderStr?.uppercase()) {
                "NEWEST_FIRST" -> SortOrder.NEWEST_FIRST
                "OLDEST_FIRST" -> SortOrder.OLDEST_FIRST
                else -> null
            }

            // If nothing meaningful was extracted, fall back to text search
            if (timeRange == null && typeFilter == null && searchQuery == null &&
                packageName == null && sortOrder == null
            ) {
                Log.d(TAG, "No meaningful filters extracted, falling back to text search")
                return ParseResult.Fallback(originalQuery)
            }

            val parsed = ParsedSearchQuery(
                timeRangeHours = timeRange,
                typeFilter = typeFilter,
                searchQuery = searchQuery,
                packageName = packageName,
                sortOrder = sortOrder
            )
            Log.d(TAG, "Parsed query: $parsed")
            return ParseResult.Success(parsed)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse query response", e)
            return ParseResult.Fallback(originalQuery)
        }
    }

    private fun extractQueryField(text: String, marker: String): String? {
        val startIndex = text.indexOf(marker, ignoreCase = true)
        if (startIndex == -1) return null

        val contentStart = startIndex + marker.length
        val lineEnd = text.indexOf('\n', contentStart).takeIf { it > 0 } ?: text.length
        val value = text.substring(contentStart, lineEnd).trim()

        return if (value.equals("NONE", ignoreCase = true) || value.isBlank()) {
            null
        } else {
            value
        }
    }
}
