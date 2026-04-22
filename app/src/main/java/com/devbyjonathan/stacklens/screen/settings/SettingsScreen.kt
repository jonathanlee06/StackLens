package com.devbyjonathan.stacklens.screen.settings

import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devbyjonathan.stacklens.BuildConfig
import com.devbyjonathan.stacklens.R
import com.devbyjonathan.stacklens.theme.StackLensTheme
import com.devbyjonathan.stacklens.theme.ThemeMode
import com.devbyjonathan.stacklens.util.isAtLeastAndroid12
import com.devbyjonathan.uikit.theme.AppTypography
import com.devbyjonathan.uikit.theme.CodeTypography
import com.devbyjonathan.uikit.theme.GoogleSansCode
import com.devbyjonathan.uikit.theme.scheme
import com.devbyjonathan.uikit.theme.typo

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    currentThemeMode: ThemeMode,
    dynamicColorEnabled: Boolean,
    onThemeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onRetentionClick: () -> Unit = {},
    onResolvedCrashesClick: () -> Unit = {},
) {
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAppInfoDialog by remember { mutableStateOf(false) }

    val isDynamicColorSupported = isAtLeastAndroid12()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        SettingsHeader()

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSection(title = "APPEARANCE") {
            SettingsCard {
                SettingsRow(
                    icon = Icons.Outlined.DarkMode,
                    title = "Theme",
                    subtitle = themeSubtitle(currentThemeMode),
                    onClick = { showThemeDialog = true },
                )
                if (isDynamicColorSupported) {
                    SettingsCardDivider()
                    SettingsSwitchRow(
                        icon = Icons.Outlined.Palette,
                        iconTileColor = scheme.primaryContainer,
                        iconTint = scheme.onPrimaryContainer,
                        title = "Dynamic color",
                        subtitle = "Use colors from your wallpaper",
                        checked = dynamicColorEnabled,
                        onCheckedChange = onDynamicColorChange,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSection(title = "LEGAL") {
            SettingsCard {
                SettingsRow(
                    icon = Icons.Outlined.Description,
                    title = "Terms & conditions",
                    onClick = onTermsClick,
                )
                SettingsCardDivider()
                SettingsRow(
                    icon = Icons.Outlined.Shield,
                    title = "Privacy policy",
                    onClick = onPrivacyClick,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSection(title = "ABOUT") {
            SettingsCard {
                SettingsRow(
                    icon = Icons.Outlined.Info,
                    title = "App info",
                    subtitle = "Version ${BuildConfig.VERSION_NAME} · build ${BuildConfig.VERSION_CODE}",
                    onClick = { showAppInfoDialog = true },
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentThemeMode = currentThemeMode,
            onThemeSelected = { theme ->
                onThemeChange(theme)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false },
        )
    }

    if (showAppInfoDialog) {
        AppInfoDialog(
            currentThemeMode = currentThemeMode,
            onDismiss = { showAppInfoDialog = false },
        )
    }
}

@Composable
private fun SettingsHeader() {
    Column {
        Text(
            text = "Settings",
            style = AppTypography.displaySmall.copy(
                fontWeight = FontWeight.Light,
                fontSize = 40.sp,
            ),
            color = scheme.onBackground,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = headerSubtitle(),
            style = CodeTypography.bodyMedium.copy(fontSize = 13.sp),
            color = scheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    trailing: String? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = AppTypography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp,
                    fontSize = 12.sp,
                ),
                color = scheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            if (trailing != null) {
                Text(
                    text = trailing,
                    style = CodeTypography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        content()
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.surface)
            .border(1.dp, scheme.outlineVariant, RoundedCornerShape(16.dp)),
    ) {
        content()
    }
}

@Composable
private fun SettingsCardDivider() {
    HorizontalDivider(
        thickness = 1.dp,
        color = scheme.outlineVariant,
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    iconTileColor: Color = scheme.surfaceContainer,
    iconTint: Color = scheme.onSurface,
    trailing: @Composable (() -> Unit)? = {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = scheme.onSurfaceVariant,
        )
    },
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIconTile(icon = icon, tileColor = iconTileColor, tint = iconTint)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = typo.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = scheme.onSurface,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = typo.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    iconTileColor: Color = scheme.surfaceContainer,
    iconTint: Color = scheme.onSurface,
) {
    SettingsRow(
        icon = icon,
        title = title,
        subtitle = subtitle,
        iconTileColor = iconTileColor,
        iconTint = iconTint,
        onClick = { onCheckedChange(!checked) },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = scheme.surface,
                    checkedTrackColor = scheme.inverseSurface,
                    checkedBorderColor = Color.Transparent,
                    uncheckedThumbColor = scheme.outline,
                    uncheckedTrackColor = scheme.surfaceContainer,
                    uncheckedBorderColor = scheme.outlineVariant,
                ),
            )
        },
    )
}

@Composable
private fun SettingsIconTile(
    icon: ImageVector,
    tileColor: Color,
    tint: Color,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(tileColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}

private fun themeSubtitle(mode: ThemeMode): String = when (mode) {
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
    ThemeMode.SYSTEM -> "System default"
}

private fun headerSubtitle(): String {
    val version = BuildConfig.VERSION_NAME
    val device = Build.DEVICE.ifBlank { "device" }
    val android = "A${Build.VERSION.RELEASE}"
    return "stacklens v$version  ·  $device  ·  $android"
}

@Composable
private fun ThemeSelectionDialog(
    currentThemeMode: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose theme") },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = currentThemeMode == mode,
                                onClick = { onThemeSelected(mode) },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = currentThemeMode == mode,
                            onClick = null,
                        )
                        Text(
                            text = themeSubtitle(mode),
                            style = typo.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun AppInfoDialog(
    currentThemeMode: ThemeMode,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    modifier = Modifier.size(24.dp),
                    painter = if (currentThemeMode == ThemeMode.DARK) {
                        painterResource(R.drawable.logo_dark)
                    } else {
                        painterResource(R.drawable.logo_light)
                    },
                    contentDescription = "App Icon",
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = AppTypography.titleLarge.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                InfoRow("Version", BuildConfig.VERSION_NAME)
                InfoRow("Build", BuildConfig.VERSION_CODE.toString())
                InfoRow("Package", context.packageName)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = typo.bodyMedium,
            color = scheme.onSurfaceVariant,
            fontFamily = GoogleSansCode,
        )
        Text(
            text = value,
            style = typo.bodyMedium,
            fontFamily = GoogleSansCode,
        )
    }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun SettingsScreenPreview() {
    StackLensTheme {
        SettingsScreen(
            currentThemeMode = ThemeMode.SYSTEM,
            dynamicColorEnabled = true,
            onThemeChange = {},
            onDynamicColorChange = {},
            onTermsClick = {},
            onPrivacyClick = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 1100, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsScreenDarkPreview() {
    StackLensTheme {
        SettingsScreen(
            currentThemeMode = ThemeMode.DARK,
            dynamicColorEnabled = true,
            onThemeChange = {},
            onDynamicColorChange = {},
            onTermsClick = {},
            onPrivacyClick = {},
        )
    }
}
