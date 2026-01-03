package com.devbyjonathan.stacklens.screen.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.devbyjonathan.stacklens.navigation.Screen
import com.devbyjonathan.stacklens.screen.detail.CrashDetailScreen
import com.devbyjonathan.stacklens.screen.list.CrashLogViewModel
import com.devbyjonathan.stacklens.screen.permission.PermissionScreen
import com.devbyjonathan.stacklens.screen.settings.PrivacyScreen
import com.devbyjonathan.stacklens.screen.settings.TermsScreen
import com.devbyjonathan.stacklens.theme.StackLensTheme
import com.devbyjonathan.stacklens.theme.ThemeManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val vm by viewModels<CrashLogViewModel>()

    @Inject
    lateinit var themeManager: ThemeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            val uiState by vm.uiState.collectAsState()
            val selectedCrash by vm.selectedCrash.collectAsState()
            val currentThemeMode by themeManager.themeMode.collectAsState()
            val dynamicColorEnabled by themeManager.dynamicColorEnabled.collectAsState()

            // Check permissions on every resume
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        vm.checkPermissions()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            val startDestination = if (uiState.hasPermissions) {
                Screen.Home.route
            } else {
                Screen.Permission.route
            }

            StackLensTheme(themeManager = themeManager) {
                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    enterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = tween(300)
                        ) + fadeIn(animationSpec = tween(300))
                    },
                    exitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { -it / 3 },
                            animationSpec = tween(300)
                        ) + fadeOut(animationSpec = tween(150))
                    },
                    popEnterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { -it / 3 },
                            animationSpec = tween(300)
                        ) + fadeIn(animationSpec = tween(300))
                    },
                    popExitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = tween(300)
                        ) + fadeOut(animationSpec = tween(150))
                    }
                ) {
                    composable(
                        route = Screen.Permission.route,
                        enterTransition = { fadeIn(animationSpec = tween(300)) },
                        exitTransition = { fadeOut(animationSpec = tween(300)) }
                    ) {
                        PermissionScreen(
                            hasReadLogs = uiState.hasReadLogsPermission,
                            hasUsageStats = uiState.hasUsageStatsPermission,
                            hasDropbox = uiState.hasDropBoxDataPermission,
                            onCheckPermissions = {
                                vm.checkPermissions()
                                if (uiState.hasPermissions) {
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Permission.route) { inclusive = true }
                                    }
                                }
                            }
                        )
                    }

                    composable(
                        route = Screen.Home.route,
                        enterTransition = { fadeIn(animationSpec = tween(300)) },
                        exitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { -it / 3 },
                                animationSpec = tween(300)
                            ) + fadeOut(animationSpec = tween(150))
                        },
                        popEnterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { -it / 3 },
                                animationSpec = tween(300)
                            ) + fadeIn(animationSpec = tween(300))
                        }
                    ) {
                        HomeScreen(
                            crashLogUiState = uiState,
                            currentThemeMode = currentThemeMode,
                            dynamicColorEnabled = dynamicColorEnabled,
                            onRefresh = { vm.refresh() },
                            onSearchQueryChange = { vm.setSearchQuery(it) },
                            onCrashClick = { crash ->
                                vm.selectCrash(crash)
                                navController.navigate(Screen.CrashDetail.route)
                            },
                            onTimeRangeChange = { vm.setTimeRange(it) },
                            onSortOrderChange = { vm.setSortOrder(it) },
                            onTypeFilterChange = { vm.setTypeFilter(it) },
                            onGroupExpand = { vm.toggleGroupExpansion(it) },
                            onThemeChange = { themeManager.setThemeMode(it) },
                            onDynamicColorChange = { themeManager.setDynamicColorEnabled(it) },
                            onTermsClick = { navController.navigate(Screen.Terms.route) },
                            onPrivacyClick = { navController.navigate(Screen.Privacy.route) }
                        )
                    }

                    composable(Screen.CrashDetail.route) {
                        selectedCrash?.let { crash ->
                            CrashDetailScreen(
                                crash = crash,
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                    }

                    composable(Screen.Terms.route) {
                        TermsScreen(
                            onBackClick = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.Privacy.route) {
                        PrivacyScreen(
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
