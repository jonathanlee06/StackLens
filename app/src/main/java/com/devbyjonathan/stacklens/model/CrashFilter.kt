package com.devbyjonathan.stacklens.model

enum class SortOrder(val displayName: String) {
    NEWEST_FIRST("Newest first"),
    OLDEST_FIRST("Oldest first")
}

enum class CrashTypeFilter(val displayName: String) {
    ALL("All"),
    CRASHES("Crashes"),
    ANRS("ANRs"),
    NATIVE("Native")
}

/**
 * Coarse crash category used by the filter sheet's multi-select UI.
 * Maps to one or more underlying [CrashType]s via [crashTypes].
 */
enum class CrashCategory(
    val displayName: String,
    val badgeLabel: String,
    val crashTypes: List<CrashType>,
) {
    CRASH("Crash", "CRASH", CrashType.appCrashTags),
    ANR("ANR", "ANR", CrashType.anrTags),
    NATIVE("Native crash", "NATIVE", CrashType.nativeTags);

    companion object {
        fun fromCrashType(type: CrashType): CrashCategory? =
            entries.firstOrNull { type in it.crashTypes }
    }
}

/**
 * Inclusive custom time range in epoch milliseconds used by the filter sheet's
 * "Custom…" chip. When non-null on [CrashFilter], takes precedence over [CrashFilter.timeRangeHours].
 */
data class CustomTimeRange(val startMs: Long, val endMs: Long)

data class CrashFilter(
    val types: Set<CrashType> = CrashType.entries.toSet(),
    val packageName: String? = null,
    val searchQuery: String? = null,
    val timeRangeHours: Int = 168, // Default to last 7 days
    val sortOrder: SortOrder = SortOrder.NEWEST_FIRST,
    val typeFilter: CrashTypeFilter = CrashTypeFilter.ALL,
    // Filter-sheet dimensions. Empty sets and null range mean "no restriction".
    val selectedCategories: Set<CrashCategory> = emptySet(),
    val selectedPackages: Set<String> = emptySet(),
    val customTimeRange: CustomTimeRange? = null,
)
