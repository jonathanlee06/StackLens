package com.devbyjonathan.stacklens.theme

import android.content.SharedPreferences
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

@Singleton
class ThemeManager @Inject constructor(
    private val sharedPreferences: SharedPreferences
) {
    companion object {
        private const val THEME_MODE_KEY = "theme_mode"
        private const val DYNAMIC_COLOR_KEY = "dynamic_color"
    }

    private val _themeMode = MutableStateFlow(getThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode

    private val _dynamicColorEnabled = MutableStateFlow(getDynamicColorEnabled())
    val dynamicColorEnabled: StateFlow<Boolean> = _dynamicColorEnabled

    val isDynamicColorSupported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    fun getThemeMode(): ThemeMode {
        val savedValue = sharedPreferences.getString(THEME_MODE_KEY, null)
        return when (savedValue) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.DARK.name -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    fun getDynamicColorEnabled(): Boolean {
        return sharedPreferences.getBoolean(DYNAMIC_COLOR_KEY, false)
    }

    fun setThemeMode(mode: ThemeMode) {
        sharedPreferences.edit {
            putString(THEME_MODE_KEY, mode.name)
        }
        _themeMode.value = mode
        applyTheme(mode)
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        sharedPreferences.edit {
            putBoolean(DYNAMIC_COLOR_KEY, enabled)
        }
        _dynamicColorEnabled.value = enabled
    }

    fun applyTheme(mode: ThemeMode) {
        val nightMode = when (mode) {
            ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }
}