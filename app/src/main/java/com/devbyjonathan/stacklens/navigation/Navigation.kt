package com.devbyjonathan.stacklens.navigation

sealed class Screen(val route: String) {
    data object Permission : Screen("permission")
    data object Home : Screen("home")
    data object CrashDetail : Screen("crash_detail")
    data object Terms : Screen("terms")
    data object Privacy : Screen("privacy")
}
