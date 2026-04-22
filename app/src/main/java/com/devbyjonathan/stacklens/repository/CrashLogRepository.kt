package com.devbyjonathan.stacklens.repository

import com.devbyjonathan.stacklens.data.local.dao.CrashLogDao
import com.devbyjonathan.stacklens.data.local.entity.CrashLogEntity
import com.devbyjonathan.stacklens.model.CrashFilter
import com.devbyjonathan.stacklens.model.CrashGroup
import com.devbyjonathan.stacklens.model.CrashLog
import com.devbyjonathan.stacklens.model.CrashType
import com.devbyjonathan.stacklens.service.CrashLogReader
import com.devbyjonathan.stacklens.service.CrashSignatureGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

data class DayBucket(val dayStartMs: Long, val count: Int)

data class EventsTrend(
    val current: Int,
    val previous: Int,
    val deltaPercent: Float,
    val buckets: List<DayBucket>,
)

@Singleton
class CrashLogRepository @Inject constructor(
    private val crashLogReader: CrashLogReader,
    private val crashLogDao: CrashLogDao,
    private val signatureGenerator: CrashSignatureGenerator,
) {
    companion object {
        private const val RETENTION_DAYS = 7
        private const val RETENTION_MS = RETENTION_DAYS * 24 * 60 * 60 * 1000L
    }

    suspend fun getCrashLogs(filter: CrashFilter): List<CrashLog> {
        // Clean up old entries first
        cleanupOldEntries()

        // Resolve time window: custom range overrides timeRangeHours when set.
        val now = System.currentTimeMillis()
        val (sinceTimestamp, untilTimestamp) = filter.customTimeRange?.let {
            it.startMs to it.endMs
        } ?: (now - (filter.timeRangeHours * 60 * 60 * 1000L) to now)

        // DropBoxManager reads use sinceHours; for a custom range, read from the window
        // start up to "now" (DropBoxManager can't read future entries anyway).
        val readSinceHours = ((now - sinceTimestamp) / (60 * 60 * 1000L))
            .toInt()
            .coerceAtLeast(1)

        // Read fresh crashes from DropBox
        val freshLogs = try {
            crashLogReader.readCrashLogs(
                types = filter.types.toList(),
                sinceHours = readSinceHours
            )
        } catch (e: SecurityException) {
            // Permission not granted - return only persisted data
            emptyList()
        }

        // Persist new crashes to database
        if (freshLogs.isNotEmpty()) {
            val entities = freshLogs.map { CrashLogEntity.fromCrashLog(it) }
            crashLogDao.insertAll(entities)
        }

        val tags = filter.types.map { it.tag }

        // Get persisted crashes within time range
        val persistedLogs = crashLogDao.getCrashesByTagsSince(tags, sinceTimestamp)
            .map { it.toCrashLog() }

        // Merge and deduplicate (ID is timestamp-based, so duplicates have same ID)
        val allLogs = (freshLogs + persistedLogs)
            .distinctBy { it.id }
            .filter { it.timestamp in sinceTimestamp..untilTimestamp }
            .sortedByDescending { it.timestamp }

        return allLogs.filter { log ->
            // Filter by package name if specified
            val matchesPackage = filter.packageName?.let {
                log.packageName?.contains(it, ignoreCase = true) == true
            } ?: true

            // Filter by search query if specified
            val matchesSearch = filter.searchQuery?.let { query ->
                log.content.contains(query, ignoreCase = true) ||
                        log.packageName?.contains(query, ignoreCase = true) == true ||
                        log.appName?.contains(query, ignoreCase = true) == true
            } ?: true

            // Filter-sheet category selection (empty set == no restriction)
            val matchesCategory = filter.selectedCategories.isEmpty() ||
                    filter.selectedCategories.any { log.tag in it.crashTypes }

            // Filter-sheet package selection (empty set == no restriction)
            val matchesSheetPackage = filter.selectedPackages.isEmpty() ||
                    (log.packageName != null && log.packageName in filter.selectedPackages)

            matchesPackage && matchesSearch && matchesCategory && matchesSheetPackage
        }
    }

    fun getCrashLogsFlow(filter: CrashFilter): Flow<List<CrashLog>> = flow {
        emit(getCrashLogs(filter))
    }

    /**
     * Look up a single persisted crash by its id. Used to re-hydrate the
     * detail screen after the process was killed.
     */
    suspend fun getCrashById(id: Long): CrashLog? {
        return crashLogDao.getCrashById(id)?.toCrashLog()
    }

    /**
     * Get unique packages that have crashed
     */
    suspend fun getCrashedPackages(): List<String> {
        cleanupOldEntries()

        val freshLogs = try {
            crashLogReader.readCrashLogs(
                types = CrashType.appCrashTags + CrashType.anrTags,
                sinceHours = 168 // Last week
            )
        } catch (e: SecurityException) {
            emptyList()
        }

        // Persist to database
        if (freshLogs.isNotEmpty()) {
            val entities = freshLogs.map { CrashLogEntity.fromCrashLog(it) }
            crashLogDao.insertAll(entities)
        }

        // Get from database
        val sinceTimestamp = System.currentTimeMillis() - (RETENTION_DAYS * 24 * 60 * 60 * 1000L)
        val tags = (CrashType.appCrashTags + CrashType.anrTags).map { it.tag }
        val persistedLogs = crashLogDao.getCrashesByTagsSince(tags, sinceTimestamp)
            .map { it.toCrashLog() }

        val allLogs = (freshLogs + persistedLogs).distinctBy { it.id }

        return allLogs.mapNotNull { it.packageName }.distinct().sorted()
    }

    /**
     * Get crash count by type
     */
    suspend fun getCrashStats(sinceHours: Int = 24): Map<CrashType, Int> {
        val logs = crashLogReader.readCrashLogs(sinceHours = sinceHours)
        return logs.groupBy { it.tag }.mapValues { it.value.size }
    }

    /**
     * Get crash logs grouped by signature.
     * Similar crashes are grouped together with occurrence count.
     */
    suspend fun getGroupedCrashLogs(filter: CrashFilter): List<CrashGroup> {
        val logs = getCrashLogs(filter)

        return logs
            .groupBy { crash ->
                signatureGenerator.generateSignature(crash.content, crash.tag)
            }
            .map { (signature, crashes) ->
                val sortedCrashes = crashes.sortedByDescending { it.timestamp }
                val exceptionType = signatureGenerator.extractExceptionType(
                    sortedCrashes.first().content
                )

                CrashGroup(
                    signature = signature,
                    exceptionType = exceptionType,
                    crashes = sortedCrashes,
                    count = crashes.size,
                    firstOccurrence = crashes.minOf { it.timestamp },
                    lastOccurrence = crashes.maxOf { it.timestamp }
                )
            }
            .sortedByDescending { it.lastOccurrence }
    }

    /**
     * Compute an events-over-time summary: daily counts for the current [days]-day window
     * plus a percentage delta vs the prior equal-length window.
     *
     * `buckets` is ordered oldest → newest and length == days. Missing days are returned
     * with count = 0 so the sparkline can render a flat baseline.
     */
    suspend fun getEventsTrend(days: Int = 7): EventsTrend {
        cleanupOldEntries()

        val now = System.currentTimeMillis()
        val dayMs = 24L * 60 * 60 * 1000
        val windowMs = days * dayMs

        val fromTimestamp = now - (2 * windowMs)
        val tags = CrashType.entries.map { it.tag }
        val logs = crashLogDao.getCrashesByTagsSince(tags, fromTimestamp)

        val currentStart = now - windowMs
        val previousStart = now - (2 * windowMs)
        var current = 0
        var previous = 0
        for (log in logs) {
            when {
                log.timestamp >= currentStart -> current++
                log.timestamp >= previousStart -> previous++
            }
        }

        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val countsByDay = HashMap<Long, Int>()
        for (log in logs) {
            if (log.timestamp < currentStart) continue
            val bucketOffsetDays = ((startOfToday - log.timestamp) / dayMs).coerceAtLeast(0)
            val bucketStart = startOfToday - (bucketOffsetDays * dayMs)
            countsByDay[bucketStart] = (countsByDay[bucketStart] ?: 0) + 1
        }
        val buckets = (days - 1 downTo 0).map { offset ->
            val dayStart = startOfToday - (offset * dayMs)
            DayBucket(dayStart, countsByDay[dayStart] ?: 0)
        }

        val delta = if (previous == 0) {
            if (current == 0) 0f else 100f
        } else {
            (current - previous) / previous.toFloat() * 100f
        }
        return EventsTrend(
            current = current,
            previous = previous,
            deltaPercent = delta,
            buckets = buckets
        )
    }

    /**
     * Delete crash logs older than retention period (7 days)
     */
    private suspend fun cleanupOldEntries() {
        val cutoffTimestamp = System.currentTimeMillis() - RETENTION_MS
        crashLogDao.deleteOlderThan(cutoffTimestamp)
    }
}
