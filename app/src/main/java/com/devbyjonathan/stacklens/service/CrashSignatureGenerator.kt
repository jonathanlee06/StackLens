package com.devbyjonathan.stacklens.service

import com.devbyjonathan.stacklens.model.CrashType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates unique signatures for crash logs to enable grouping of similar crashes.
 * Signature format: "{ExceptionType}:{CrashTag}:{topFramesHash}"
 */
@Singleton
class CrashSignatureGenerator @Inject constructor() {

    /**
     * Generate a signature from crash content for grouping similar crashes.
     * Crashes with the same signature are considered duplicates/related.
     */
    fun generateSignature(content: String, crashType: CrashType): String {
        val exceptionType = extractExceptionType(content)
        val topFrames = extractTopStackFrames(content, count = 3)
        val framesHash = if (topFrames.isNotEmpty()) {
            topFrames.joinToString("|").hashCode().toString(16)
        } else {
            content.take(200).hashCode().toString(16)
        }
        return "${exceptionType}:${crashType.tag}:$framesHash"
    }

    /**
     * Extract the exception type from crash content.
     * Returns the exception class name (e.g., "NullPointerException", "IllegalStateException")
     */
    fun extractExceptionType(content: String, crashType: CrashType? = null): String {
        // Try various patterns to extract exception type
        val patterns = listOf(
            // Standard Java exception: "java.lang.NullPointerException: message"
            Regex("([\\w.]*(?:Exception|Error))(?::|\\s|$)"),
            // Caused by pattern: "Caused by: java.lang.NullPointerException"
            Regex("Caused by:\\s*([\\w.]*(?:Exception|Error))"),
            // FATAL EXCEPTION pattern
            Regex("FATAL EXCEPTION:.*\\n.*?([\\w.]*(?:Exception|Error))"),
            // ANR pattern
            Regex("ANR in ([\\w.]+)"),
            // Native crash signal
            Regex("signal\\s+(\\d+)\\s+\\(([A-Z]+)\\)")
        )

        for (pattern in patterns) {
            val match = pattern.find(content)
            if (match != null) {
                val exceptionType = match.groupValues.getOrNull(1) ?: match.groupValues[0]
                // Return just the class name without package
                return exceptionType.substringAfterLast('.')
            }
        }

        // Fallback based on crash type
        return when (crashType) {
            CrashType.DATA_APP_ANR, CrashType.SYSTEM_APP_ANR -> "ANR"
            CrashType.SYSTEM_TOMBSTONE -> "NativeCrash"
            null -> "UnknownException"
            else -> "UnknownException"
        }
    }

    /**
     * Extract the top N stack frames from the crash content.
     * These are used to create a unique hash for grouping.
     */
    private fun extractTopStackFrames(content: String, count: Int): List<String> {
        // Match stack frame patterns like "at com.example.Class.method(File.java:123)"
        val framePattern = Regex("\\s+at\\s+([\\w.$<>]+\\([^)]*\\))")

        return framePattern.findAll(content)
            .map { match ->
                // Normalize the frame by removing line numbers for better grouping
                val frame = match.groupValues[1]
                frame.replace(Regex(":\\d+\\)"), ")")
            }
            .filter { frame ->
                // Filter out common framework frames that don't help with grouping
                !frame.startsWith("android.") &&
                        !frame.startsWith("java.lang.reflect.") &&
                        !frame.startsWith("dalvik.") &&
                        !frame.startsWith("com.android.internal.")
            }
            .take(count)
            .toList()
    }
}
