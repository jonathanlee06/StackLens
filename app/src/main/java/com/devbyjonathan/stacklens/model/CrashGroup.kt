package com.devbyjonathan.stacklens.model

/**
 * Represents a group of similar crashes identified by the same signature.
 * Used to aggregate duplicate/related crashes for easier analysis.
 */
data class CrashGroup(
    val signature: String,
    val exceptionType: String,
    val crashes: List<CrashLog>,
    val count: Int,
    val firstOccurrence: Long,
    val lastOccurrence: Long,
) {
    /**
     * The most recent crash in this group, used as the representative crash.
     */
    val latestCrash: CrashLog
        get() = crashes.maxByOrNull { it.timestamp } ?: crashes.first()

    /**
     * The app name from the latest crash.
     */
    val appName: String?
        get() = latestCrash.appName

    /**
     * The package name from the latest crash.
     */
    val packageName: String?
        get() = latestCrash.packageName

    /**
     * The crash type from the latest crash.
     */
    val crashType: CrashType
        get() = latestCrash.tag
}
