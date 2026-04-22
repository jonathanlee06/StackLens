package com.devbyjonathan.uikit.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Composable-scoped shortcut for `MaterialTheme.colorScheme`.
 *
 * Usage: `scheme.surfaceVariant` instead of `MaterialTheme.colorScheme.surfaceVariant`.
 * Named `scheme` (not `color` / `colors`) to avoid shadowing common Compose parameters
 * like `color: Color` or `colors: ButtonColors`.
 */
val scheme: ColorScheme
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme

/**
 * Composable-scoped shortcut for `MaterialTheme.typography`.
 *
 * Usage: `typo.bodyMedium` instead of `MaterialTheme.typography.bodyMedium`.
 */
val typo: Typography
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.typography
