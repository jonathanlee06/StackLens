package com.devbyjonathan.stacklens

import android.app.Application
import com.devbyjonathan.stacklens.theme.ThemeManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class StackLensApp : Application() {

    @Inject
    lateinit var themeManager: ThemeManager

    override fun onCreate() {
        super.onCreate()

        // Apply the saved theme mode
        val currentTheme = themeManager.getThemeMode()
        themeManager.applyTheme(currentTheme)
    }

}