package com.devbyjonathan.stacklens.util

private val FRAME_REGEX = Regex("""\s+at\s+([\w$.<>]+)\.([\w$<>]+)\(([^)]+)\)""")

fun isFrameworkFrame(classOrFrame: String): Boolean {
    return classOrFrame.startsWith("android.") ||
            classOrFrame.startsWith("androidx.") ||
            classOrFrame.startsWith("java.lang.reflect.") ||
            classOrFrame.startsWith("dalvik.") ||
            classOrFrame.startsWith("com.android.internal.") ||
            classOrFrame.startsWith("kotlin.") ||
            classOrFrame.startsWith("kotlinx.")
}

/**
 * Returns the first non-framework stack frame's `File.kt:line` form, e.g. "MainActivity.kt:29",
 * or null when no app-owned frame with a line number is present.
 */
fun extractLikelyLocation(content: String): String? {
    for (match in FRAME_REGEX.findAll(content)) {
        val className = match.groupValues[1]
        val location = match.groupValues[3]
        if (isFrameworkFrame(className)) continue
        val colonIdx = location.lastIndexOf(':')
        if (colonIdx <= 0) continue
        val line = location.substring(colonIdx + 1).toIntOrNull() ?: continue
        val file = location.substring(0, colonIdx)
        return "$file:$line"
    }
    return null
}

/** Count of non-framework frames in the trace. Used as a confidence signal. */
fun countAppFrames(content: String): Int {
    return FRAME_REGEX.findAll(content).count { match ->
        !isFrameworkFrame(match.groupValues[1])
    }
}
