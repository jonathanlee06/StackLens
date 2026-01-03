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
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class CrashInsight(
    val summary: String,
    val rootCause: String,
    val suggestedFix: String,
    val affectedLine: String?,
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

@Singleton
class CrashInsightService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val crashInsightDao: CrashInsightDao,
) {
    private var generativeModel: GenerativeModel? = null

    @FeatureStatus
    private var currentStatus: Int = FeatureStatus.UNAVAILABLE

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

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
     */
    suspend fun analyzeCrash(crash: CrashLog): InsightResult = withContext(Dispatchers.IO) {
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
            val result = parseInsightResponse(text)

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

SUMMARY: [One sentence explaining what happened]

ROOT_CAUSE: [The specific cause of the crash]

SUGGESTED_FIX: [How to fix this issue]

AFFECTED_LINE: [The key line from the stack trace, or N/A]
""".trimIndent()
    }

    private fun parseInsightResponse(response: String): InsightResult {
        try {
            val summary = extractSection(response, "SUMMARY:") ?: "Unable to determine summary"
            val rootCause =
                extractSection(response, "ROOT_CAUSE:") ?: "Unable to determine root cause"
            val suggestedFix = extractSection(response, "SUGGESTED_FIX:") ?: "Unable to suggest fix"
            val affectedLine = extractSection(response, "AFFECTED_LINE:")?.takeIf {
                it.isNotBlank() && !it.equals("N/A", ignoreCase = true)
            }

            Log.d(TAG, "Parsed insight - Summary: ${summary.take(50)}...")
            return InsightResult.Success(
                CrashInsight(
                    summary = summary.trim(),
                    rootCause = rootCause.trim(),
                    suggestedFix = suggestedFix.trim(),
                    affectedLine = affectedLine?.trim()
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse AI response", e)
            return InsightResult.Error("Failed to parse AI response")
        }
    }

    private fun extractSection(text: String, marker: String): String? {
        val startIndex = text.indexOf(marker, ignoreCase = true)
        if (startIndex == -1) return null

        val contentStart = startIndex + marker.length
        val markers = listOf("SUMMARY:", "ROOT_CAUSE:", "SUGGESTED_FIX:", "AFFECTED_LINE:")
        val nextMarkerIndex = markers
            .filter { !it.equals(marker, ignoreCase = true) }
            .mapNotNull { nextMarker ->
                val idx = text.indexOf(nextMarker, contentStart, ignoreCase = true)
                if (idx > 0) idx else null
            }
            .minOrNull() ?: text.length

        return text.substring(contentStart, nextMarkerIndex).trim()
    }
}
