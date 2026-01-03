package com.devbyjonathan.stacklens.repository

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
    private val signatureGenerator: CrashSignatureGenerator,
) {

    suspend fun getCrashLogs(filter: CrashFilter): List<CrashLog> {
        val logs = crashLogReader.readCrashLogs(
            types = filter.types.toList(),
            sinceHours = filter.timeRangeHours
        )

        return logs.filter { log ->
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
        val logs = crashLogReader.readCrashLogs(
            types = CrashType.appCrashTags + CrashType.anrTags,
            sinceHours = 168 // Last week
        )
        return logs.mapNotNull { it.packageName }.distinct().sorted()
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
}
