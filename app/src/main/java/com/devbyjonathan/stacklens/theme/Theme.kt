package com.devbyjonathan.stacklens.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.devbyjonathan.uikit.theme.AppTypography
import com.devbyjonathan.uikit.theme.darkScheme
import com.devbyjonathan.uikit.theme.lightScheme

@Composable
fun StackLensTheme(
    themeManager: ThemeManager? = null,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    // Determine dark theme
    val effectiveDarkTheme = if (themeManager != null) {
        val themeMode by themeManager.themeMode.collectAsState()
        when (themeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
        }
    } else {
        darkTheme
    }

    // Determine dynamic color - collect at top level to avoid delegate issues
    val dynamicColorState = themeManager?.dynamicColorEnabled?.collectAsState()
    val dynamicColorEnabled = dynamicColorState?.value ?: false

    val useDynamicColor = dynamicColorEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = when {
        useDynamicColor && effectiveDarkTheme -> dynamicDarkColorScheme(context)
        useDynamicColor && !effectiveDarkTheme -> dynamicLightColorScheme(context)
        effectiveDarkTheme -> darkScheme
        else -> lightScheme
    }

    // Get current view for window modifications
    val view = LocalView.current
    if (!view.isInEditMode) {
        // Only apply status bar changes if we're not in the design preview
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()

            // Set status bar appearance (light or dark content)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                !effectiveDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
