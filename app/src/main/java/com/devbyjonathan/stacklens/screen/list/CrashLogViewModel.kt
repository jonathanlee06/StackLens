package com.devbyjonathan.stacklens.screen.list

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devbyjonathan.stacklens.ai.CrashInsightService
import com.devbyjonathan.stacklens.ai.ParseResult
import com.devbyjonathan.stacklens.model.CrashCategory
import com.devbyjonathan.stacklens.model.CrashFilter
import com.devbyjonathan.stacklens.model.CrashGroup
import com.devbyjonathan.stacklens.model.CrashLog
import com.devbyjonathan.stacklens.model.CrashType
import com.devbyjonathan.stacklens.model.CrashTypeFilter
import com.devbyjonathan.stacklens.model.CustomTimeRange
import com.devbyjonathan.stacklens.model.SortOrder
import com.devbyjonathan.stacklens.repository.CrashLogRepository
import com.devbyjonathan.stacklens.repository.EventsTrend
import com.devbyjonathan.stacklens.util.PermissionChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CrashLogViewModel @Inject constructor(
    private val application: Application,
    private val repository: CrashLogRepository,
    private val crashInsightService: CrashInsightService,
    private val sharedPreferences: SharedPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CrashLogUiState())
    val uiState: StateFlow<CrashLogUiState> = _uiState.asStateFlow()

    private val _selectedCrash = MutableStateFlow<CrashLog?>(null)
    val selectedCrash: StateFlow<CrashLog?> = _selectedCrash.asStateFlow()

    private val _isLoadingSelectedCrash = MutableStateFlow(false)
    val isLoadingSelectedCrash: StateFlow<Boolean> = _isLoadingSelectedCrash.asStateFlow()

    private var aiSearchJob: Job? = null

    companion object {
        private const val PREF_AI_TOOLTIP_SHOWN = "ai_search_tooltip_shown"
    }

    init {
        checkPermissions()
        checkAiAvailability()
    }

    fun checkPermissions() {
        val hasReadLogs = PermissionChecker.hasReadLogsPermission(application)
        val hasUsageStats = PermissionChecker.hasUsageStatsPermission(application)
        val hasDropBoxData = PermissionChecker.hasReadDropBoxDataPermission(application)
        val hasPermissions = PermissionChecker.hasAllRequiredPermissions(application)

        _uiState.value = _uiState.value.copy(
            hasPermissions = hasPermissions,
            hasReadLogsPermission = hasReadLogs,
            hasUsageStatsPermission = hasUsageStats,
            hasDropBoxDataPermission = hasDropBoxData
        )

        if (hasPermissions) {
            loadCrashLogs()
        }
    }

    fun loadCrashLogs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val filter = _uiState.value.filter
                // Broad pool: time-range (custom range respected) + search only. Used for
                // filter-sheet counts so badges reflect the whole time range regardless of
                // the category/package selection currently applied.
                val broadFilter = filter.copy(
                    selectedCategories = emptySet(),
                    selectedPackages = emptySet(),
                    packageName = null,
                )
                val broadLogs = repository.getCrashLogs(broadFilter)
                val stats = repository.getCrashStats(filter.timeRangeHours)

                val categoryCounts: Map<CrashCategory, Int> =
                    CrashCategory.entries.associateWith { cat ->
                        broadLogs.count { it.tag in cat.crashTypes }
                    }
                val appFilterItems: List<AppFilterItem> = broadLogs
                    .filter { it.packageName != null }
                    .groupBy { it.packageName!! }
                    .map { (pkg, logs) ->
                        AppFilterItem(
                            packageName = pkg,
                            appName = logs.firstNotNullOfOrNull { it.appName },
                            count = logs.size,
                        )
                    }
                    .sortedWith(
                        compareByDescending<AppFilterItem> { it.count }
                            .thenBy { (it.appName ?: it.packageName).lowercase() }
                    )

                // Apply in-memory dimensional filters to produce the displayed list.
                var logs = broadLogs.filter { log ->
                    val matchesCategory = filter.selectedCategories.isEmpty() ||
                            filter.selectedCategories.any { log.tag in it.crashTypes }
                    val matchesPackage = filter.selectedPackages.isEmpty() ||
                            (log.packageName != null && log.packageName in filter.selectedPackages)
                    matchesCategory && matchesPackage
                }

                // Apply legacy type-pill filter
                logs = when (filter.typeFilter) {
                    CrashTypeFilter.ALL -> logs
                    CrashTypeFilter.CRASHES -> logs.filter { it.tag in CrashType.appCrashTags }
                    CrashTypeFilter.ANRS -> logs.filter { it.tag in CrashType.anrTags }
                    CrashTypeFilter.NATIVE -> logs.filter { it.tag in CrashType.nativeTags }
                }

                // Apply sort order
                logs = when (filter.sortOrder) {
                    SortOrder.NEWEST_FIRST -> logs.sortedByDescending { it.timestamp }
                    SortOrder.OLDEST_FIRST -> logs.sortedBy { it.timestamp }
                }

                // Load grouped crashes (respects new filter fields via repository).
                val groups = repository.getGroupedCrashLogs(filter).let { allGroups ->
                    when (filter.typeFilter) {
                        CrashTypeFilter.ALL -> allGroups
                        CrashTypeFilter.CRASHES -> allGroups.filter { it.crashType in CrashType.appCrashTags }
                        CrashTypeFilter.ANRS -> allGroups.filter { it.crashType in CrashType.anrTags }
                        CrashTypeFilter.NATIVE -> allGroups.filter { it.crashType in CrashType.nativeTags }
                    }
                }

                val eventsTrend = runCatching { repository.getEventsTrend(days = 7) }.getOrNull()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    crashLogs = logs,
                    crashGroups = groups,
                    stats = stats,
                    eventsTrend = eventsTrend,
                    filterSheetCategoryCounts = categoryCounts,
                    filterSheetApps = appFilterItems,
                    filterSheetMatchingCount = logs.size,
                )
            } catch (e: SecurityException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Permission denied. Please grant READ_LOGS permission via ADB.",
                    hasPermissions = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun updateFilter(filter: CrashFilter) {
        _uiState.value = _uiState.value.copy(filter = filter)
        loadCrashLogs()
    }

    fun setSearchQuery(query: String) {
        if (_uiState.value.isAiSearchEnabled && query.isNotBlank()) {
            performAiSearch(query)
        } else {
            val newFilter = _uiState.value.filter.copy(
                searchQuery = query.ifBlank { null }
            )
            updateFilter(newFilter)
        }
    }

    fun setTimeRange(hours: Int) {
        val newFilter = _uiState.value.filter.copy(timeRangeHours = hours)
        updateFilter(newFilter)
    }

    fun setSortOrder(sortOrder: SortOrder) {
        val newFilter = _uiState.value.filter.copy(sortOrder = sortOrder)
        updateFilter(newFilter)
    }

    fun setTypeFilter(typeFilter: CrashTypeFilter) {
        val newFilter = _uiState.value.filter.copy(typeFilter = typeFilter)
        updateFilter(newFilter)
    }

    /**
     * Commit the filter-sheet selection in one shot. The sheet is the new source of truth
     * for time range, category, and package filters, so we also reset [CrashTypeFilter]
     * pills to ALL to avoid the pill and the sheet silently fighting.
     */
    fun applyFilterSheet(
        timeRangeHours: Int,
        customTimeRange: CustomTimeRange?,
        selectedCategories: Set<CrashCategory>,
        selectedPackages: Set<String>,
    ) {
        val newFilter = _uiState.value.filter.copy(
            timeRangeHours = timeRangeHours,
            customTimeRange = customTimeRange,
            selectedCategories = selectedCategories,
            selectedPackages = selectedPackages,
            typeFilter = CrashTypeFilter.ALL,
        )
        updateFilter(newFilter)
    }

    fun toggleCrashType(type: CrashType) {
        val currentTypes = _uiState.value.filter.types.toMutableSet()
        if (currentTypes.contains(type)) {
            currentTypes.remove(type)
        } else {
            currentTypes.add(type)
        }
        val newFilter = _uiState.value.filter.copy(types = currentTypes)
        updateFilter(newFilter)
    }

    fun selectCrash(crash: CrashLog?) {
        _selectedCrash.value = crash
    }

    /**
     * Ensure `selectedCrash` is populated for the given id. Called from the
     * detail screen so the page re-hydrates correctly after process death.
     */
    fun ensureSelectedCrash(id: Long) {
        if (_selectedCrash.value?.id == id) return
        viewModelScope.launch {
            _isLoadingSelectedCrash.value = true
            try {
                _selectedCrash.value = repository.getCrashById(id)
            } finally {
                _isLoadingSelectedCrash.value = false
            }
        }
    }

    fun refresh() {
        loadCrashLogs()
    }

    fun toggleGroupExpansion(signature: String) {
        val currentExpanded = _uiState.value.expandedGroups
        val newExpanded = if (signature in currentExpanded) {
            currentExpanded - signature
        } else {
            currentExpanded + signature
        }
        _uiState.value = _uiState.value.copy(expandedGroups = newExpanded)
    }

    private fun checkAiAvailability() {
        viewModelScope.launch {
            val isAvailable = crashInsightService.isAvailable()
            val tooltipShown = sharedPreferences.getBoolean(PREF_AI_TOOLTIP_SHOWN, false)
            _uiState.value = _uiState.value.copy(
                isAiSearchAvailable = isAvailable,
                showAiTooltip = isAvailable && !tooltipShown
            )
            if (isAvailable) {
                generateSuggestedPrompts()
            }
        }
    }

    fun toggleAiSearchMode() {
        val newEnabled = !_uiState.value.isAiSearchEnabled
        _uiState.value = _uiState.value.copy(
            isAiSearchEnabled = newEnabled,
            showAiTooltip = false
        )
        // Mark tooltip as shown when user first toggles AI mode
        if (newEnabled && !sharedPreferences.getBoolean(PREF_AI_TOOLTIP_SHOWN, false)) {
            sharedPreferences.edit().putBoolean(PREF_AI_TOOLTIP_SHOWN, true).apply()
        }
        // Regenerate prompts when AI mode is enabled
        if (newEnabled) {
            generateSuggestedPrompts()
        }
    }

    fun dismissAiTooltip() {
        _uiState.value = _uiState.value.copy(showAiTooltip = false)
        sharedPreferences.edit().putBoolean(PREF_AI_TOOLTIP_SHOWN, true).apply()
    }

    private fun generateSuggestedPrompts() {
        viewModelScope.launch {
            val groups = _uiState.value.crashGroups.take(5)
            val prompts = mutableListOf<String>()

            // Generate prompts based on crash groups
            for (group in groups) {
                val appName = group.appName ?: continue
                val exceptionType = group.exceptionType.substringAfterLast('.')
                if (exceptionType.isNotBlank() && appName.isNotBlank()) {
                    prompts.add("$exceptionType from $appName")
                }
            }

            // Add some generic prompts if we don't have enough
            if (prompts.size < 3) {
                prompts.add("Crashes from today")
            }
            if (prompts.size < 3) {
                prompts.add("ANRs from last 3 days")
            }

            _uiState.value = _uiState.value.copy(
                suggestedPrompts = prompts.distinct().take(4)
            )
        }
    }

    fun applySuggestedPrompt(prompt: String) {
        performAiSearch(prompt)
    }

    private fun performAiSearch(query: String) {
        aiSearchJob?.cancel()
        aiSearchJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isParsingQuery = true)

            when (val result = crashInsightService.parseNaturalLanguageQuery(query)) {
                is ParseResult.Success -> {
                    val parsed = result.query
                    val currentFilter = _uiState.value.filter
                    val newFilter = currentFilter.copy(
                        timeRangeHours = parsed.timeRangeHours ?: currentFilter.timeRangeHours,
                        typeFilter = parsed.typeFilter ?: currentFilter.typeFilter,
                        searchQuery = parsed.searchQuery ?: parsed.packageName,
                        sortOrder = parsed.sortOrder ?: currentFilter.sortOrder
                    )
                    _uiState.value = _uiState.value.copy(
                        filter = newFilter,
                        isParsingQuery = false
                    )
                    loadCrashLogs()
                }

                is ParseResult.Fallback -> {
                    // Fall back to regular text search
                    val newFilter = _uiState.value.filter.copy(
                        searchQuery = result.originalQuery.ifBlank { null }
                    )
                    _uiState.value = _uiState.value.copy(
                        filter = newFilter,
                        isParsingQuery = false
                    )
                    loadCrashLogs()
                }

                is ParseResult.Unavailable -> {
                    // AI not available, fall back to text search
                    val newFilter = _uiState.value.filter.copy(
                        searchQuery = query.ifBlank { null }
                    )
                    _uiState.value = _uiState.value.copy(
                        filter = newFilter,
                        isParsingQuery = false,
                        isAiSearchAvailable = false,
                        isAiSearchEnabled = false
                    )
                    loadCrashLogs()
                }
            }
        }
    }
}

data class CrashLogUiState(
    val isLoading: Boolean = false,
    val hasPermissions: Boolean = false,
    val hasReadLogsPermission: Boolean = false,
    val hasUsageStatsPermission: Boolean = false,
    val hasDropBoxDataPermission: Boolean = false,
    val crashLogs: List<CrashLog> = emptyList(),
    val crashGroups: List<CrashGroup> = emptyList(),
    val expandedGroups: Set<String> = emptySet(),
    val stats: Map<CrashType, Int> = emptyMap(),
    val filter: CrashFilter = CrashFilter(),
    val error: String? = null,
    val isAiSearchEnabled: Boolean = false,
    val isAiSearchAvailable: Boolean = false,
    val isParsingQuery: Boolean = false,
    val suggestedPrompts: List<String> = emptyList(),
    val showAiTooltip: Boolean = false,
    val eventsTrend: EventsTrend? = null,
    // Filter-sheet state derived from the broad (time-range-only) pool.
    val filterSheetCategoryCounts: Map<CrashCategory, Int> = emptyMap(),
    val filterSheetApps: List<AppFilterItem> = emptyList(),
    val filterSheetMatchingCount: Int = 0,
)

data class AppFilterItem(
    val packageName: String,
    val appName: String?,
    val count: Int,
)
