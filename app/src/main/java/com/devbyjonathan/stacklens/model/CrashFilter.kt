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

data class CrashFilter(
    val types: Set<CrashType> = CrashType.entries.toSet(),
    val packageName: String? = null,
    val searchQuery: String? = null,
    val timeRangeHours: Int = 168, // Default to last 7 days
    val sortOrder: SortOrder = SortOrder.NEWEST_FIRST,
    val typeFilter: CrashTypeFilter = CrashTypeFilter.ALL
)
