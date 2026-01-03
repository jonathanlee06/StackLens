package com.devbyjonathan.stacklens.screen.list

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devbyjonathan.stacklens.model.CrashFilter
import com.devbyjonathan.stacklens.model.CrashGroup
import com.devbyjonathan.stacklens.model.CrashLog
import com.devbyjonathan.stacklens.model.CrashType
import com.devbyjonathan.stacklens.model.CrashTypeFilter
import com.devbyjonathan.stacklens.model.SortOrder
import com.devbyjonathan.stacklens.repository.CrashLogRepository
import com.devbyjonathan.stacklens.util.PermissionChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CrashLogViewModel @Inject constructor(
    private val application: Application,
    private val repository: CrashLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CrashLogUiState())
    val uiState: StateFlow<CrashLogUiState> = _uiState.asStateFlow()

    private val _selectedCrash = MutableStateFlow<CrashLog?>(null)
    val selectedCrash: StateFlow<CrashLog?> = _selectedCrash.asStateFlow()

    init {
        checkPermissions()
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
                var logs = repository.getCrashLogs(filter)
                val stats = repository.getCrashStats(filter.timeRangeHours)

                // Apply type filter
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

                // Load grouped crashes (always enabled)
                val groups = repository.getGroupedCrashLogs(filter).let { allGroups ->
                    // Apply type filter to groups
                    when (filter.typeFilter) {
                        CrashTypeFilter.ALL -> allGroups
                        CrashTypeFilter.CRASHES -> allGroups.filter { it.crashType in CrashType.appCrashTags }
                        CrashTypeFilter.ANRS -> allGroups.filter { it.crashType in CrashType.anrTags }
                        CrashTypeFilter.NATIVE -> allGroups.filter { it.crashType in CrashType.nativeTags }
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    crashLogs = logs,
                    crashGroups = groups,
                    stats = stats
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
        val newFilter = _uiState.value.filter.copy(
            searchQuery = query.ifBlank { null }
        )
        updateFilter(newFilter)
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
)
