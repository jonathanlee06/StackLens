package com.devbyjonathan.stacklens.screen.list

import android.widget.Space
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devbyjonathan.stacklens.model.CrashLog
import com.devbyjonathan.stacklens.model.CrashType
import com.devbyjonathan.stacklens.model.CrashTypeFilter
import com.devbyjonathan.stacklens.model.SortOrder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrashLogListContent(
    modifier: Modifier = Modifier,
    uiState: CrashLogUiState,
    onRefresh: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCrashClick: (CrashLog) -> Unit,
    onTimeRangeChange: (Int) -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    onTypeFilterChange: (CrashTypeFilter) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showDurationSheet by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        CrashLogList(
            uiState = uiState,
            searchQuery = searchQuery,
            onCrashClick = onCrashClick,
            onSearchQueryChange = {
                searchQuery = it
                onSearchQueryChange(it)
            },
            onTypeFilterChange = {
                onTypeFilterChange(it)
            },
            showDurationSheet = {
                showDurationSheet = true
            },
            showSortSheet = {
                showSortSheet = true
            }
        )
    }

    // Duration Bottom Sheet
    if (showDurationSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDurationSheet = false },
            sheetState = sheetState
        ) {
            DurationOptionsContent(
                selectedHours = uiState.filter.timeRangeHours,
                onSelect = { hours ->
                    onTimeRangeChange(hours)
                    scope.launch {
                        sheetState.hide()
                        showDurationSheet = false
                    }
                }
            )
        }
    }

    // Sort Bottom Sheet
    if (showSortSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSortSheet = false },
            sheetState = sheetState
        ) {
            SortOptionsContent(
                selectedOrder = uiState.filter.sortOrder,
                onSelect = { order ->
                    onSortOrderChange(order)
                    scope.launch {
                        sheetState.hide()
                        showSortSheet = false
                    }
                }
            )
        }
    }
}

@Composable
fun DurationOptionsContent(
    selectedHours: Int,
    onSelect: (Int) -> Unit
) {
    val options = listOf(
        1 to "Last 1 hour",
        6 to "Last 6 hours",
        24 to "Last 24 hours",
        72 to "Last 3 days",
        168 to "Last 7 days"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Time Range",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )

        options.forEach { (hours, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(hours) }
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = if (selectedHours == hours) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                if (selectedHours == hours) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun SortOptionsContent(
    selectedOrder: SortOrder,
    onSelect: (SortOrder) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Sort Order",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )

        SortOrder.entries.forEach { order ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(order) }
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (order) {
                        SortOrder.NEWEST_FIRST -> Icons.Default.ArrowDownward
                        SortOrder.OLDEST_FIRST -> Icons.Default.ArrowUpward
                    },
                    contentDescription = null,
                    tint = if (selectedOrder == order) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = order.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                if (selectedOrder == order) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun CrashTypeFilterRow(
    stats: Map<CrashType, Int>,
    selectedFilter: CrashTypeFilter,
    uiState: CrashLogUiState,
    onFilterChange: (CrashTypeFilter) -> Unit,
    onClickSort: () -> Unit,
    onClickDuration: () -> Unit,
) {
    val totalCount = stats.values.sum()
    val crashCount = stats.filterKeys { it in CrashType.appCrashTags }.values.sum()
    val anrCount = stats.filterKeys { it in CrashType.anrTags }.values.sum()
    val nativeCount = stats.filterKeys { it in CrashType.nativeTags }.values.sum()

    LazyRow(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        item {
            // Duration pill
            FilterChip(
                shape = RoundedCornerShape(50.dp),
                selected = false,
                onClick = { onClickDuration() },
                label = {
                    Text(
                        when (uiState.filter.timeRangeHours) {
                            1 -> "Last 1 hour"
                            6 -> "Last 6 hours"
                            24 -> "Last 24 hours"
                            72 -> "Last 3 days"
                            168 -> "Last 7 days"
                            else -> "Last ${uiState.filter.timeRangeHours} hour(s)"
                        }
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
        item {
            // Sort pill
            FilterChip(
                shape = RoundedCornerShape(50.dp),
                selected = false,
                onClick = onClickSort,
                label = {
                    Text(
                        when (uiState.filter.sortOrder) {
                            SortOrder.NEWEST_FIRST -> "Newest"
                            SortOrder.OLDEST_FIRST -> "Oldest"
                        }
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
        item {
            VerticalDivider(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .height(24.dp),
                thickness = 1.dp
            )
        }

        item {
            CrashTypeFilterChip(
                label = "All",
                count = totalCount,
                color = MaterialTheme.colorScheme.primary,
                selected = selectedFilter == CrashTypeFilter.ALL,
                onClick = { onFilterChange(CrashTypeFilter.ALL) }
            )
        }
        item {
            CrashTypeFilterChip(
                label = "Crashes",
                count = crashCount,
                color = MaterialTheme.colorScheme.error,
                selected = selectedFilter == CrashTypeFilter.CRASHES,
                onClick = { onFilterChange(CrashTypeFilter.CRASHES) }
            )
        }
        item {
            CrashTypeFilterChip(
                label = "ANRs",
                count = anrCount,
                color = MaterialTheme.colorScheme.tertiary,
                selected = selectedFilter == CrashTypeFilter.ANRS,
                onClick = { onFilterChange(CrashTypeFilter.ANRS) }
            )
        }
        item {
            CrashTypeFilterChip(
                label = "Native",
                count = nativeCount,
                color = MaterialTheme.colorScheme.secondary,
                selected = selectedFilter == CrashTypeFilter.NATIVE,
                onClick = { onFilterChange(CrashTypeFilter.NATIVE) }
            )
        }
    }
}

@Composable
fun CrashTypeFilterChip(
    label: String,
    count: Int,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        shape = RoundedCornerShape(50.dp),
        selected = selected,
        onClick = onClick,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = color
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(label)
                if (label != "All" && selected.not()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        color = if (selected) color else color.copy(alpha = 0.2f),
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = count.toString(),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) Color.White else color
                        )
                    }
                }
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color.copy(alpha = 0.15f),
            selectedLabelColor = color
        )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CrashLogList(
    uiState: CrashLogUiState,
    searchQuery: String,
    onCrashClick: (CrashLog) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onTypeFilterChange: (CrashTypeFilter) -> Unit,
    showDurationSheet: () -> Unit,
    showSortSheet: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    onSearchQueryChange(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("Search crashes...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )
        }

        stickyHeader {
            // Crash type filter pills
            CrashTypeFilterRow(
                stats = uiState.stats,
                selectedFilter = uiState.filter.typeFilter,
                uiState = uiState,
                onFilterChange = onTypeFilterChange,
                onClickSort = { showSortSheet() },
                onClickDuration = { showDurationSheet() }
            )
        }

        when {
            uiState.isLoading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            uiState.error != null -> {
                item {
                    ErrorMessage(message = uiState.error)
                }
            }
            uiState.crashLogs.isEmpty() -> {
                item {
                    EmptyState()
                }
            }
            else -> {
                items(uiState.crashLogs, key = { "${it.id}_${it.tag}" }) { crash ->
                    CrashLogItem(crash = crash, onClick = { onCrashClick(crash) })
                }
            }
        }
    }
}

@Composable
fun CrashLogItem(
    crash: CrashLog,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()) }

    val (icon, color) = getCrashTypeIconAndColor(crash.tag)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(
                color = color.copy(alpha = 0.05f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Type icon
            Surface(
                color = color.copy(alpha = 0.15f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = crash.appName ?: crash.packageName ?: "Unknown",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    CrashTypeBadge(type = crash.tag, color = color)
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (crash.appName != null && crash.packageName != null) {
                    Text(
                        text = crash.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val preview = crash.content.lines()
                    .firstOrNull { it.contains("Exception") || it.contains("Error") }
                    ?: crash.content.lines().firstOrNull()
                    ?: ""

                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = color
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = dateFormat.format(Date(crash.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun getCrashTypeIconAndColor(type: CrashType): Pair<ImageVector, Color> {
    return when (type) {
        CrashType.DATA_APP_CRASH, CrashType.SYSTEM_APP_CRASH ->
            Icons.Default.BugReport to MaterialTheme.colorScheme.error
        CrashType.DATA_APP_ANR, CrashType.SYSTEM_APP_ANR ->
            Icons.Default.Timer to MaterialTheme.colorScheme.tertiary
        CrashType.SYSTEM_TOMBSTONE ->
            Icons.Default.Memory to MaterialTheme.colorScheme.secondary
        else ->
            Icons.Default.Error to MaterialTheme.colorScheme.outline
    }
}

@Composable
fun CrashTypeBadge(
    type: CrashType,
    color: Color,
    modifier: Modifier = Modifier
) {
    val text = when (type) {
        CrashType.DATA_APP_CRASH, CrashType.SYSTEM_APP_CRASH -> "CRASH"
        CrashType.DATA_APP_ANR, CrashType.SYSTEM_APP_ANR -> "ANR"
        CrashType.SYSTEM_TOMBSTONE -> "NATIVE"
        else -> type.displayName.uppercase()
    }

    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
fun ErrorMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No crashes found",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your apps are running smoothly!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
