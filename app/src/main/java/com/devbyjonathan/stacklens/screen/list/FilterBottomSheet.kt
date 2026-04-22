package com.devbyjonathan.stacklens.screen.list

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devbyjonathan.stacklens.common.CrashTypeBadge
import com.devbyjonathan.stacklens.model.CrashCategory
import com.devbyjonathan.stacklens.model.CrashType
import com.devbyjonathan.stacklens.model.CustomTimeRange
import com.devbyjonathan.stacklens.model.fake.PreviewData
import com.devbyjonathan.stacklens.theme.StackLensTheme
import com.devbyjonathan.uikit.theme.AppTypography
import com.devbyjonathan.uikit.theme.CodeTypography
import com.devbyjonathan.uikit.theme.scheme
import com.devbyjonathan.uikit.theme.typo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class TimeRangePreset(val hours: Int, val label: String)

private val TIME_RANGE_PRESETS = listOf(
    TimeRangePreset(1, "1h"),
    TimeRangePreset(24, "24h"),
    TimeRangePreset(168, "7d"),
    TimeRangePreset(720, "30d"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    uiState: CrashLogUiState,
    onDismiss: () -> Unit,
    onApply: (
        timeRangeHours: Int,
        customTimeRange: CustomTimeRange?,
        selectedCategories: Set<CrashCategory>,
        selectedPackages: Set<String>,
    ) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = scheme.background,
        contentColor = scheme.onBackground,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        FilterBottomSheetContent(
            uiState = uiState,
            onCancel = onDismiss,
            onApply = onApply,
        )
    }
}

@Composable
fun FilterBottomSheetContent(
    uiState: CrashLogUiState,
    onCancel: () -> Unit,
    onApply: (
        timeRangeHours: Int,
        customTimeRange: CustomTimeRange?,
        selectedCategories: Set<CrashCategory>,
        selectedPackages: Set<String>,
    ) -> Unit,
) {
    val filter = uiState.filter

    // Resolve initial sheet state from the committed filter. An empty category/package set
    // on CrashFilter means "no restriction", which we present as "all selected" in the sheet.
    val availablePackages = remember(uiState.filterSheetApps) {
        uiState.filterSheetApps.map { it.packageName }.toSet()
    }
    val initialCategories = if (filter.selectedCategories.isEmpty()) {
        CrashCategory.entries.toSet()
    } else {
        filter.selectedCategories
    }
    val initialPackages = if (filter.selectedPackages.isEmpty()) {
        availablePackages
    } else {
        filter.selectedPackages
    }

    var draftTimeRangeHours by remember { mutableStateOf(filter.timeRangeHours) }
    var draftCustomRange by remember { mutableStateOf(filter.customTimeRange) }
    var draftCategories by remember { mutableStateOf(initialCategories) }
    var draftPackages by remember { mutableStateOf(initialPackages) }
    var showDatePicker by remember { mutableStateOf(false) }

    val activeCount = draftCategories.size + draftPackages.size
    val matchingEvents = computeMatchingEvents(
        appItems = uiState.filterSheetApps,
        categoryCounts = uiState.filterSheetCategoryCounts,
        draftCategories = draftCategories,
        draftPackages = draftPackages,
        allPackages = availablePackages,
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        FilterHeader(
            activeCount = activeCount,
            matchingEvents = matchingEvents,
            onReset = {
                draftTimeRangeHours = 168
                draftCustomRange = null
                draftCategories = CrashCategory.entries.toSet()
                draftPackages = availablePackages
            },
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp),
            contentPadding = PaddingValues(bottom = 12.dp),
        ) {
            item {
                TimeRangeSection(
                    selectedHours = draftTimeRangeHours,
                    customRange = draftCustomRange,
                    onPresetSelected = { hours ->
                        draftTimeRangeHours = hours
                        draftCustomRange = null
                    },
                    onCustomClick = { showDatePicker = true },
                )
            }

            item {
                CrashTypeSection(
                    counts = uiState.filterSheetCategoryCounts,
                    selected = draftCategories,
                    onToggle = { cat ->
                        draftCategories = if (cat in draftCategories) {
                            draftCategories - cat
                        } else {
                            draftCategories + cat
                        }
                    },
                )
            }

            item {
                AppsSectionHeader(count = uiState.filterSheetApps.size)
            }
            items(uiState.filterSheetApps, key = { it.packageName }) { app ->
                AppFilterRow(
                    item = app,
                    checked = app.packageName in draftPackages,
                    onToggle = {
                        draftPackages = if (app.packageName in draftPackages) {
                            draftPackages - app.packageName
                        } else {
                            draftPackages + app.packageName
                        }
                    },
                )
            }
            if (uiState.filterSheetApps.isEmpty()) {
                item {
                    Text(
                        text = "No apps with crashes in this time range.",
                        style = typo.bodySmall,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                }
            }
        }

        FilterFooter(
            matchingEvents = matchingEvents,
            onCancel = onCancel,
            onApply = {
                val normalizedCategories = if (draftCategories == CrashCategory.entries.toSet()) {
                    emptySet()
                } else draftCategories
                val normalizedPackages = if (draftPackages == availablePackages) {
                    emptySet()
                } else draftPackages
                onApply(
                    draftTimeRangeHours,
                    draftCustomRange,
                    normalizedCategories,
                    normalizedPackages,
                )
            },
        )
    }

    if (showDatePicker) {
        CustomRangeDialog(
            initial = draftCustomRange,
            onDismiss = { showDatePicker = false },
            onConfirm = { range ->
                draftCustomRange = range
                showDatePicker = false
            },
        )
    }
}

@Composable
private fun FilterHeader(
    activeCount: Int,
    matchingEvents: Int,
    onReset: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Filters",
                style = AppTypography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                ),
                color = scheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onReset) {
                Text(
                    text = "Reset",
                    style = AppTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onPrimaryContainer,
                )
            }
        }
        Text(
            text = "$activeCount active · matching $matchingEvents events",
            style = typo.bodySmall,
            color = scheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TimeRangeSection(
    selectedHours: Int,
    customRange: CustomTimeRange?,
    onPresetSelected: (Int) -> Unit,
    onCustomClick: () -> Unit,
) {
    val isCustomSelected = customRange != null
    Column(modifier = Modifier.padding(top = 12.dp)) {
        SectionHeader(
            title = "TIME RANGE",
            trailing = timeRangeLabel(selectedHours, customRange),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
        ) {
            items(TIME_RANGE_PRESETS) { preset ->
                val selected = !isCustomSelected && selectedHours == preset.hours
                TimeRangeChip(
                    label = preset.label,
                    selected = selected,
                    onClick = { onPresetSelected(preset.hours) },
                )
            }
            item {
                TimeRangeChip(
                    label = customRange?.let { "Custom" } ?: "Custom…",
                    selected = isCustomSelected,
                    onClick = onCustomClick,
                    leadingIcon = Icons.Default.CalendarToday,
                )
            }
        }
    }
}

@Composable
private fun TimeRangeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    val bg = if (selected) scheme.inverseSurface else Color.Transparent
    val fg = if (selected) scheme.inverseOnSurface else scheme.onSurface
    val borderColor = if (selected) Color.Transparent else scheme.outline

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(percent = 50))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(14.dp),
            )
        } else if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            text = label,
            style = AppTypography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            ),
            color = fg,
        )
    }
}

@Composable
private fun CrashTypeSection(
    counts: Map<CrashCategory, Int>,
    selected: Set<CrashCategory>,
    onToggle: (CrashCategory) -> Unit,
) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        SectionHeader(title = "CRASH TYPE")
        Column {
            CrashCategory.entries.forEach { cat ->
                CrashCategoryRow(
                    category = cat,
                    count = counts[cat] ?: 0,
                    checked = cat in selected,
                    onToggle = { onToggle(cat) },
                )
            }
        }
    }
}

@Composable
private fun CrashCategoryRow(
    category: CrashCategory,
    count: Int,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    val representative = category.crashTypes.firstOrNull() ?: CrashType.DATA_APP_CRASH
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SheetCheckbox(checked = checked)
        Spacer(modifier = Modifier.width(12.dp))
        CrashTypeBadge(type = representative)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = category.displayName,
            style = typo.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = scheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = count.toString(),
            style = CodeTypography.labelLarge.copy(fontWeight = FontWeight.Medium),
            color = scheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AppsSectionHeader(count: Int) {
    Box(modifier = Modifier.padding(top = 16.dp)) {
        SectionHeader(
            title = "APPS",
            trailing = "$count installed",
        )
    }
}

@Composable
private fun AppFilterRow(
    item: AppFilterItem,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SheetCheckbox(checked = checked)
        Spacer(modifier = Modifier.width(12.dp))
        AppInitialAvatar(name = item.appName ?: item.packageName)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.appName ?: item.packageName,
                style = typo.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.packageName,
                style = CodeTypography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = item.count.toString(),
            style = CodeTypography.labelLarge.copy(fontWeight = FontWeight.Medium),
            color = scheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AppInitialAvatar(name: String) {
    val initial = name.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "?"
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(scheme.surfaceContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            style = AppTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = scheme.onSurface,
        )
    }
}

@Composable
private fun SheetCheckbox(checked: Boolean) {
    val bg = if (checked) scheme.inverseSurface else Color.Transparent
    val borderColor = if (checked) Color.Transparent else scheme.outline
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.5.dp, borderColor, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = scheme.inverseOnSurface,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, trailing: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = AppTypography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
            ),
            color = scheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = typo.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FilterFooter(
    matchingEvents: Int,
    onCancel: () -> Unit,
    onApply: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(percent = 50))
                .background(scheme.surfaceContainer)
                .clickable(onClick = onCancel)
                .padding(horizontal = 28.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Cancel",
                style = AppTypography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                ),
                color = scheme.onSurface,
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(percent = 50))
                .background(scheme.inverseSurface)
                .clickable(onClick = onApply)
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Apply · $matchingEvents",
                style = AppTypography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                ),
                color = scheme.inverseOnSurface,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomRangeDialog(
    initial: CustomTimeRange?,
    onDismiss: () -> Unit,
    onConfirm: (CustomTimeRange) -> Unit,
) {
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initial?.startMs,
        initialSelectedEndDateMillis = initial?.endMs,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val start = state.selectedStartDateMillis
                    val end = state.selectedEndDateMillis
                    if (start != null && end != null) {
                        // end is the start-of-day of the selected end date; include the whole day.
                        onConfirm(CustomTimeRange(start, end + (24L * 60 * 60 * 1000) - 1))
                    }
                },
                enabled = state.selectedStartDateMillis != null && state.selectedEndDateMillis != null,
            ) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    ) {
        DateRangePicker(state = state, modifier = Modifier.heightIn(max = 520.dp))
    }
}

private fun timeRangeLabel(hours: Int, custom: CustomTimeRange?): String {
    if (custom != null) {
        val fmt = SimpleDateFormat("MMM d", Locale.getDefault())
        return "${fmt.format(Date(custom.startMs))} – ${fmt.format(Date(custom.endMs))}"
    }
    return when (hours) {
        1 -> "last 1 hour"
        24 -> "last 24 hours"
        72 -> "last 3 days"
        168 -> "last 7 days"
        720 -> "last 30 days"
        else -> "last $hours hours"
    }
}

private fun computeMatchingEvents(
    appItems: List<AppFilterItem>,
    categoryCounts: Map<CrashCategory, Int>,
    draftCategories: Set<CrashCategory>,
    draftPackages: Set<String>,
    allPackages: Set<String>,
): Int {
    // Coarse estimate: sum counts of selected categories, scaled by the ratio of selected
    // packages in the time-range pool. Good enough for the "Apply · N" preview; the committed
    // filter recomputes the accurate total.
    val categoryTotal = draftCategories.sumOf { categoryCounts[it] ?: 0 }
    val poolTotal = appItems.sumOf { it.count }
    if (poolTotal == 0 || draftPackages.size == allPackages.size) return categoryTotal
    val selectedPackageTotal = appItems
        .filter { it.packageName in draftPackages }
        .sumOf { it.count }
    if (categoryTotal == 0) return 0
    // If no packages selected, nothing matches.
    if (selectedPackageTotal == 0) return 0
    // Approximate the intersection assuming uniform distribution.
    return ((categoryTotal.toLong() * selectedPackageTotal) / poolTotal).toInt()
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun FilterBottomSheetContentPreview() {
    StackLensTheme {
        FilterBottomSheetContent(
            uiState = PreviewData.sampleUiState.copy(
                filterSheetCategoryCounts = mapOf(
                    CrashCategory.CRASH to 14,
                    CrashCategory.ANR to 3,
                    CrashCategory.NATIVE to 6,
                ),
                filterSheetApps = listOf(
                    AppFilterItem("com.devbyjonathan.justcrash", "Just Crash", 12),
                    AppFilterItem("com.facebook.katana", "Facebook", 6),
                    AppFilterItem("com.google.android.apps.maps", "Google Maps", 3),
                    AppFilterItem("com.example.shop", "Shopping", 2),
                ),
                filterSheetMatchingCount = 23,
            ),
            onCancel = {},
            onApply = { _, _, _, _ -> },
        )
    }
}

@Preview(showBackground = true, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FilterBottomSheetContentDarkPreview() {
    StackLensTheme {
        FilterBottomSheetContent(
            uiState = PreviewData.sampleUiState.copy(
                filterSheetCategoryCounts = mapOf(
                    CrashCategory.CRASH to 14,
                    CrashCategory.ANR to 3,
                    CrashCategory.NATIVE to 6,
                ),
                filterSheetApps = listOf(
                    AppFilterItem("com.devbyjonathan.justcrash", "Just Crash", 12),
                    AppFilterItem("com.facebook.katana", "Facebook", 6),
                ),
                filterSheetMatchingCount = 23,
            ),
            onCancel = {},
            onApply = { _, _, _, _ -> },
        )
    }
}
