package com.devbyjonathan.stacklens.screen.main

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.devbyjonathan.stacklens.model.CrashLog
import com.devbyjonathan.stacklens.model.CrashTypeFilter
import com.devbyjonathan.stacklens.model.SortOrder
import com.devbyjonathan.stacklens.model.fake.PreviewData
import com.devbyjonathan.stacklens.screen.list.CrashLogListContent
import com.devbyjonathan.stacklens.screen.list.CrashLogUiState
import com.devbyjonathan.stacklens.screen.settings.SettingsScreen
import com.devbyjonathan.stacklens.theme.StackLensTheme
import com.devbyjonathan.stacklens.theme.ThemeMode
import com.devbyjonathan.uikit.theme.AppTypography

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
    onThemeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit
) {
    val navItems = listOf(
        BottomNavItem(
            title = "Crashes",
            selectedIcon = Icons.Filled.BugReport,
            unselectedIcon = Icons.Outlined.BugReport
        ),
        BottomNavItem(
            title = "Settings",
            selectedIcon = Icons.Filled.Settings,
            unselectedIcon = Icons.Outlined.Settings
        )
    )

    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (selectedIndex == 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                painter = painterResource(R.drawable.logo_static),
                                contentDescription = "App Icon",
                                tint = MaterialTheme.colorScheme.primary
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
                    } else {
                        Text(
                            text = navItems[selectedIndex].title,
                            style = AppTypography.titleLarge.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                },
                actions = {
                    if (selectedIndex == 0) {
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                tonalElevation = 4.dp
            ) {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedIndex == index,
                        onClick = { selectedIndex = index },
                        icon = {
                            Icon(
                                imageVector = if (selectedIndex == index) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title
                            )
                        },
                        label = { Text(item.title) }
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp)
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
                    onTypeFilterChange = onTypeFilterChange
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
            onThemeChange = {},
            onDynamicColorChange = {},
            onTermsClick = {},
            onPrivacyClick = {}
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
            onThemeChange = {},
            onDynamicColorChange = {},
            onTermsClick = {},
            onPrivacyClick = {}
        )
    }
}
