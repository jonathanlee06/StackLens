package com.devbyjonathan.stacklens.screen.permission

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devbyjonathan.stacklens.theme.StackLensTheme
import com.devbyjonathan.uikit.theme.AppTypography
import com.devbyjonathan.uikit.theme.CodeTypography
import com.devbyjonathan.uikit.theme.inversePrimaryLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionScreen(
    hasReadLogs: Boolean,
    hasUsageStats: Boolean,
    hasDropbox: Boolean,
    onCheckPermissions: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val packageName = context.packageName

    val adbCommand_readLogs = "adb shell pm grant $packageName android.permission.READ_LOGS"
    val adbCommand_readDropBox = "adb shell pm grant $packageName android.permission.READ_DROPBOX_DATA"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Box(modifier = Modifier.statusBarsPadding())
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                    onClick = onCheckPermissions,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Check Permissions",
                        style = AppTypography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp)
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues = contentPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(20.dp)
                ) {
                    Icon(
                        modifier = Modifier.size(30.dp),
                        imageVector = Icons.Default.Security,
                        contentDescription = "Permissions",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Permissions Required",
                    style = AppTypography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 24.sp
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "StackLens needs advanced system permissions to capture crash logs and analyse usage data.",
                    style = AppTypography.bodyMedium.copy(
                        textAlign = TextAlign.Center
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            // Permission status cards
            PermissionCard(
                title = "Usage Stats Access",
                description = "Required to get app names and icons",
                icon = Icons.Default.QueryStats,
                isGranted = hasUsageStats,
                onGrantClick = {
                    // Try to open directly to our app's usage access setting
                    // This works on some devices but not all (OEM dependent)
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Fallback to generic usage access settings
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }
                },
                grantButtonText = "Open Settings"
            )

            Spacer(modifier = Modifier.height(16.dp))

            PermissionCard(
                title = "Read Logs Permission",
                description = "Required to read crash logs from the system",
                icon = Icons.Default.Terminal,
                isGranted = hasReadLogs,
                adbCommand = adbCommand_readLogs,
                onCopyCommand = {
                    clipboardManager.setText(AnnotatedString(adbCommand_readLogs))
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            PermissionCard(
                title = "Read Dropbox Data Permission",
                description = "Required to read crash logs from the system",
                icon = Icons.Default.Archive,
                isGranted = hasDropbox,
                adbCommand = adbCommand_readDropBox,
                onCopyCommand = {
                    clipboardManager.setText(AnnotatedString(adbCommand_readDropBox))
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Instructions
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "How to grant ADB permission",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val steps = listOf(
                        "Enable Developer Options on your phone",
                        "Enable USB Debugging in Developer Options",
                        "Connect your phone to a computer via USB",
                        "Open a terminal/command prompt on your computer",
                        "Run the ADB command shown above",
                        "The app will close automatically - this is normal",
                        "Reopen the app to verify permissions"
                    )

                    steps.forEachIndexed { index, step ->
                        Text(
                            text = "${index + 1}. $step",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    onGrantClick: (() -> Unit)? = null,
    grantButtonText: String? = null,
    adbCommand: String? = null,
    onCopyCommand: (() -> Unit)? = null
) {
    val color = if (isGranted) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = color.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.size(35.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier.weight(1F),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.weight(1F).basicMarquee(),
                        text = title,
                        style = AppTypography.titleMedium,
                        maxLines = 1,
                        color = if (isGranted) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                    Text(
                        text = if (isGranted) "Granted" else "Not Granted",
                        style = AppTypography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = if (isGranted) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = AppTypography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!isGranted) {
                Spacer(modifier = Modifier.height(12.dp))

                if (adbCommand != null && onCopyCommand != null) {
                    // ADB command display
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    ) {
                        SelectionContainer {
                            Text(
                                text = adbCommand,
                                style = CodeTypography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                        onClick = onCopyCommand,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Copy Command",
                            style = AppTypography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else if (onGrantClick != null && grantButtonText != null) {
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                        onClick = onGrantClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = grantButtonText,
                            style = AppTypography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun PermissionScreenPreview() {
    StackLensTheme {
        PermissionScreen(
            hasReadLogs = false,
            hasUsageStats = false,
            hasDropbox = false,
            onCheckPermissions = {}
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PermissionScreenDarkPreview() {
    StackLensTheme {
        PermissionScreen(
            hasReadLogs = false,
            hasUsageStats = false,
            hasDropbox = false,
            onCheckPermissions = {}
        )
    }
}