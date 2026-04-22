package com.devbyjonathan.stacklens.navigation

sealed class Screen(val route: String) {
    data object Permission : Screen("permission")
    data object Home : Screen("home")
    data object CrashDetail : Screen("crash_detail/{crashId}") {
        const val ARG_CRASH_ID = "crashId"
        fun buildRoute(crashId: Long) = "crash_detail/$crashId"
    }
    data object Terms : Screen("terms")
    data object Privacy : Screen("privacy")
}
