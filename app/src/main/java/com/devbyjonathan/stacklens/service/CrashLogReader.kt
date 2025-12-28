package com.devbyjonathan.stacklens.service

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.DropBoxManager
import com.devbyjonathan.stacklens.model.CrashLog
import com.devbyjonathan.stacklens.model.CrashType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrashLogReader @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val dropBoxManager: DropBoxManager by lazy {
        context.getSystemService(Context.DROPBOX_SERVICE) as DropBoxManager
    }

    private val packageManager: PackageManager by lazy {
        context.packageManager
    }

    /**
     * Read crash logs from DropBoxManager
     * @param types List of crash types to query
     * @param sinceHours How many hours back to look
     * @param maxContentLength Maximum characters to read from each entry
     */
    suspend fun readCrashLogs(
        types: List<CrashType> = CrashType.allTags,
        sinceHours: Int = 24,
        maxContentLength: Int = 64 * 1024 // 64KB per entry
    ): List<CrashLog> = withContext(Dispatchers.IO) {
        val crashes = mutableListOf<CrashLog>()
        val sinceTime = System.currentTimeMillis() - (sinceHours * 60 * 60 * 1000L)

        for (type in types) {
            try {
                readEntriesForTag(type, sinceTime, maxContentLength, crashes)
            } catch (e: SecurityException) {
                // Permission not granted - skip this tag
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Sort by timestamp, newest first
        crashes.sortedByDescending { it.timestamp }
    }

    private fun readEntriesForTag(
        type: CrashType,
        sinceTime: Long,
        maxContentLength: Int,
        crashes: MutableList<CrashLog>
    ) {
        var entry: DropBoxManager.Entry? = null
        var currentTime = sinceTime

        try {
            while (true) {
                entry = dropBoxManager.getNextEntry(type.tag, currentTime)
                if (entry == null) break

                val content = entry.getText(maxContentLength) ?: ""
                val timestamp = entry.timeMillis

                // Parse package name from crash content
                val packageName = extractPackageName(content, type)
                val appName = packageName?.let { getAppName(it) }

                crashes.add(
                    CrashLog(
                        id = timestamp,
                        tag = type,
                        packageName = packageName,
                        appName = appName,
                        timestamp = timestamp,
                        content = content,
                        processName = extractProcessName(content),
                        pid = extractPid(content)
                    )
                )

                currentTime = timestamp
                entry.close()
            }
        } finally {
            entry?.close()
        }
    }

    /**
     * Extract package name from crash log content
     */
    private fun extractPackageName(content: String, type: CrashType): String? {
        // Common patterns that appear in most crash types
        fun tryCommonPatterns(): String? {
            // Process: com.example.app or Process: com.example.app:service
            val processMatch = Regex("^Process:\\s*([\\w.]+)", RegexOption.MULTILINE).find(content)
            if (processMatch != null) {
                return processMatch.groupValues[1].split(":").firstOrNull()
            }
            // Package: com.example.app v1 (1.0)
            val packageMatch = Regex("^Package:\\s*([\\w.]+)", RegexOption.MULTILINE).find(content)
            if (packageMatch != null) {
                return packageMatch.groupValues[1]
            }
            return null
        }

        return when (type) {
            CrashType.DATA_APP_CRASH,
            CrashType.SYSTEM_APP_CRASH,
                -> {
                tryCommonPatterns()
            }
            CrashType.DATA_APP_ANR,
            CrashType.SYSTEM_APP_ANR,
                -> {
                // ANR in com.example.app
                val anrMatch = Regex("ANR in ([\\w.]+)").find(content)
                anrMatch?.groupValues?.get(1)?.split(":")?.firstOrNull()
                    ?: tryCommonPatterns()
            }
            CrashType.SYSTEM_TOMBSTONE -> {
                // cmdline: com.example.app or >>> com.example.app <<<
                val cmdlineMatch = Regex("cmdline:\\s*([\\w.]+)").find(content)
                    ?: Regex(">>>\\s*([\\w.]+)").find(content)
                cmdlineMatch?.groupValues?.get(1)
                    ?: tryCommonPatterns()
            }
            else -> {
                tryCommonPatterns()
                    ?: Regex("([a-z][a-z0-9_]*(?:\\.[a-z][a-z0-9_]*){2,})").find(content)?.groupValues?.get(
                        1
                    )
            }
        }
    }

    private fun extractProcessName(content: String): String? {
        val match = Regex("Process:\\s+(.+)").find(content)
        return match?.groupValues?.get(1)?.trim()
    }

    private fun extractPid(content: String): Int? {
        val match = Regex("PID:\\s+(\\d+)").find(content)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun getAppName(packageName: String): String? {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            // App not installed - return package name as fallback
            packageName
        }
    }

    /**
     * Get app icon for a package
     */
    fun getAppIcon(packageName: String): Drawable? {
        return try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }
}
