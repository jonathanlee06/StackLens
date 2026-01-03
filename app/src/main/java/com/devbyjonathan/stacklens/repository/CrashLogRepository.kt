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
import javax.inject.Inject
import javax.inject.Singleton

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

        // Read fresh crashes from DropBox
        val freshLogs = try {
            crashLogReader.readCrashLogs(
                types = filter.types.toList(),
                sinceHours = filter.timeRangeHours
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

        // Calculate time range for query
        val sinceTimestamp = System.currentTimeMillis() - (filter.timeRangeHours * 60 * 60 * 1000L)
        val tags = filter.types.map { it.tag }

        // Get persisted crashes within time range
        val persistedLogs = crashLogDao.getCrashesByTagsSince(tags, sinceTimestamp)
            .map { it.toCrashLog() }

        // Merge and deduplicate (ID is timestamp-based, so duplicates have same ID)
        val allLogs = (freshLogs + persistedLogs)
            .distinctBy { it.id }
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

            matchesPackage && matchesSearch
        }
    }

    fun getCrashLogsFlow(filter: CrashFilter): Flow<List<CrashLog>> = flow {
        emit(getCrashLogs(filter))
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
     * Delete crash logs older than retention period (7 days)
     */
    private suspend fun cleanupOldEntries() {
        val cutoffTimestamp = System.currentTimeMillis() - RETENTION_MS
        crashLogDao.deleteOlderThan(cutoffTimestamp)
    }
}
