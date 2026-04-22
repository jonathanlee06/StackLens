package com.devbyjonathan.stacklens.screen.main

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devbyjonathan.stacklens.R
import com.devbyjonathan.stacklens.model.CrashCategory
import com.devbyjonathan.stacklens.model.CrashLog
import com.devbyjonathan.stacklens.model.CrashTypeFilter
import com.devbyjonathan.stacklens.model.CustomTimeRange
import com.devbyjonathan.stacklens.model.SortOrder
import com.devbyjonathan.stacklens.model.fake.PreviewData
import com.devbyjonathan.stacklens.screen.list.CrashLogListContent
import com.devbyjonathan.stacklens.screen.list.CrashLogUiState
import com.devbyjonathan.stacklens.screen.list.FilterBottomSheet
import com.devbyjonathan.stacklens.screen.settings.SettingsScreen
import com.devbyjonathan.stacklens.theme.StackLensTheme
import com.devbyjonathan.stacklens.theme.ThemeMode
import com.devbyjonathan.uikit.theme.AppTypography
import com.devbyjonathan.uikit.theme.CodeTypography
import com.devbyjonathan.uikit.theme.scheme

data class BottomNavItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    crashLogUiState: CrashLogUiState,
    currentThemeMode: ThemeMode,
    dynamicColorEnabled: Boolean,
    onRefresh: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCrashClick: (CrashLog) -> Unit,
    onTimeRangeChange: (Int) -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    onTypeFilterChange: (CrashTypeFilter) -> Unit,
    onGroupExpand: (String) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onToggleAiSearch: () -> Unit = {},
    onDismissAiTooltip: () -> Unit = {},
    onSuggestedPromptClick: (String) -> Unit = {},
    onApplyFilterSheet: (
        timeRangeHours: Int,
        customTimeRange: CustomTimeRange?,
        selectedCategories: Set<CrashCategory>,
        selectedPackages: Set<String>,
    ) -> Unit = { _, _, _, _ -> },
) {
    var showFilterSheet by remember { mutableStateOf(false) }

    val navItems = listOf(
        BottomNavItem(
            title = "crashes",
            selectedIcon = Icons.Filled.BugReport,
            unselectedIcon = Icons.Outlined.BugReport
        ),
        BottomNavItem(
            title = "settings",
            selectedIcon = Icons.Filled.Settings,
            unselectedIcon = Icons.Outlined.Settings
        )
    )

    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            if (selectedIndex == 0) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = scheme.background
                    ),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                painter = painterResource(R.drawable.logo_static),
                                contentDescription = "App Icon",
                                tint = scheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(id = R.string.app_name),
                                style = AppTypography.titleLarge.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    },
                    actions = {
                        FilledIconButton(
                            onClick = { showFilterSheet = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = IconButtonColors(
                                containerColor = scheme.surfaceContainer,
                                contentColor = scheme.onSurface,
                                disabledContainerColor = scheme.surfaceContainer,
                                disabledContentColor = scheme.onSurfaceVariant
                            )
                        ) {
                            Icon(
                                Icons.Outlined.FilterAlt,
                                contentDescription = "Filters",
                                tint = scheme.onSurfaceVariant
                            )
                        }
                        FilledIconButton(
                            onClick = onRefresh,
                            shape = RoundedCornerShape(12.dp),
                            colors = IconButtonColors(
                                containerColor = scheme.surfaceContainer,
                                contentColor = scheme.onSurface,
                                disabledContainerColor = scheme.surfaceContainer,
                                disabledContentColor = scheme.onSurfaceVariant
                            )
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = scheme.onSurfaceVariant
                            )
                        }
                    }
                )
            } else {
                // Settings tab runs edge-to-edge; reserve only the status-bar
                // inset so the redesigned SettingsScreen header sits just below it.
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(scheme.background)
                        .windowInsetsTopHeight(WindowInsets.statusBars)
                )
            }
        },
        bottomBar = {
            Column {
                HorizontalDivider(thickness = 1.dp, color = scheme.outlineVariant)
                NavigationBar(
                    containerColor = scheme.background,
                    contentColor = scheme.onSurfaceVariant,
                    tonalElevation = 4.dp
                ) {
                    navItems.forEachIndexed { index, item ->
                        NavigationBarItem(
                            colors = NavigationBarItemColors(
                                selectedIndicatorColor = scheme.primary,
                                selectedIconColor = scheme.onPrimary,
                                selectedTextColor = scheme.primary,
                                unselectedIconColor = scheme.secondary,
                                unselectedTextColor = scheme.secondary,
                                disabledIconColor = scheme.secondaryContainer,
                                disabledTextColor = scheme.secondaryContainer,
                            ),
                            selected = selectedIndex == index,
                            onClick = { selectedIndex = index },
                            icon = {
                                Icon(
                                    imageVector = if (selectedIndex == index) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    style = CodeTypography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        )
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        contentColor = scheme.background
    ) { padding ->
        AnimatedContent(
            targetState = selectedIndex,
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> direction * fullWidth },
                    animationSpec = tween(300)
                ) togetherWith slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -direction * fullWidth },
                    animationSpec = tween(300)
                )
            },
            label = "TabContent"
        ) { index ->
            when (index) {
                0 -> CrashLogListContent(
                    modifier = Modifier.padding(padding),
                    uiState = crashLogUiState,
                    onRefresh = onRefresh,
                    onSearchQueryChange = onSearchQueryChange,
                    onCrashClick = onCrashClick,
                    onTimeRangeChange = onTimeRangeChange,
                    onSortOrderChange = onSortOrderChange,
                    onTypeFilterChange = onTypeFilterChange,
                    onGroupExpand = onGroupExpand,
                    onToggleAiSearch = onToggleAiSearch,
                    onDismissAiTooltip = onDismissAiTooltip,
                    onSuggestedPromptClick = onSuggestedPromptClick
                )
                1 -> SettingsScreen(
                    modifier = Modifier.padding(padding),
                    currentThemeMode = currentThemeMode,
                    dynamicColorEnabled = dynamicColorEnabled,
                    onThemeChange = onThemeChange,
                    onDynamicColorChange = onDynamicColorChange,
                    onTermsClick = onTermsClick,
                    onPrivacyClick = onPrivacyClick
                )
            }
        }
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            uiState = crashLogUiState,
            onDismiss = { showFilterSheet = false },
            onApply = { hours, custom, categories, packages ->
                onApplyFilterSheet(hours, custom, categories, packages)
                showFilterSheet = false
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    StackLensTheme {
        HomeScreen(
            crashLogUiState = PreviewData.sampleUiState,
            currentThemeMode = ThemeMode.SYSTEM,
            dynamicColorEnabled = true,
            onRefresh = {},
            onSearchQueryChange = {},
            onCrashClick = {},
            onTimeRangeChange = {},
            onSortOrderChange = {},
            onTypeFilterChange = {},
            onGroupExpand = {},
            onThemeChange = {},
            onDynamicColorChange = {},
            onTermsClick = {},
            onPrivacyClick = {},
            onToggleAiSearch = {},
            onDismissAiTooltip = {},
            onSuggestedPromptClick = {}
        )
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun HomeScreenDarkPreview() {
    StackLensTheme {
        HomeScreen(
            crashLogUiState = PreviewData.sampleUiState,
            currentThemeMode = ThemeMode.DARK,
            dynamicColorEnabled = true,
            onRefresh = {},
            onSearchQueryChange = {},
            onCrashClick = {},
            onTimeRangeChange = {},
            onSortOrderChange = {},
            onTypeFilterChange = {},
            onGroupExpand = {},
            onThemeChange = {},
            onDynamicColorChange = {},
            onTermsClick = {},
            onPrivacyClick = {},
            onToggleAiSearch = {},
            onDismissAiTooltip = {},
            onSuggestedPromptClick = {}
        )
    }
}
