package com.devbyjonathan.stacklens.util

data class CrashMetadata(
    val uid: Int? = null,
    val systemUptimeMs: Long? = null,
    val processRuntimeSec: Long? = null,
    val foreground: Boolean? = null,
)

private val UID_REGEX = Regex("""(?im)^\s*UID:\s*(\d+)""")
private val UPTIME_REGEX = Regex("""(?im)^\s*SystemUptimeMs:\s*(\d+)""")
private val RUNTIME_REGEX = Regex("""(?im)^\s*Process-Runtime:\s*(\d+)""")
private val FOREGROUND_REGEX = Regex("""(?im)^\s*Foreground:\s*(\w+)""")

fun parseCrashMetadata(content: String): CrashMetadata {
    val uid = UID_REGEX.find(content)?.groupValues?.get(1)?.toIntOrNull()
    val uptime = UPTIME_REGEX.find(content)?.groupValues?.get(1)?.toLongOrNull()
    val runtime = RUNTIME_REGEX.find(content)?.groupValues?.get(1)?.toLongOrNull()
    val foreground = FOREGROUND_REGEX.find(content)?.groupValues?.get(1)?.let {
        when (it.lowercase()) {
            "yes", "true" -> true
            "no", "false" -> false
            else -> null
        }
    }
    return CrashMetadata(
        uid = uid,
        systemUptimeMs = uptime,
        processRuntimeSec = runtime,
        foreground = foreground,
    )
}
