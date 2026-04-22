package com.devbyjonathan.stacklens.screen.list

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devbyjonathan.stacklens.common.CrashTypeBadge
import com.devbyjonathan.stacklens.model.CrashGroup
import com.devbyjonathan.stacklens.model.CrashLog
import com.devbyjonathan.stacklens.model.fake.PreviewData
import com.devbyjonathan.stacklens.theme.StackLensTheme
import com.devbyjonathan.uikit.theme.GoogleSansCode
import com.devbyjonathan.uikit.theme.scheme
import com.devbyjonathan.uikit.theme.typo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CrashGroupItem(
    group: CrashGroup,
    isExpanded: Boolean,
    onGroupClick: () -> Unit,
    onCrashClick: (CrashLog) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val (icon, color) = getCrashTypeIconAndColor(group.crashType)

    Column(modifier = modifier.fillMaxWidth()) {
        // Group Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(
                    color = color.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable(onClick = onGroupClick)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type icon
                Surface(
                    color = color.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = group.appName ?: group.packageName ?: "Unknown",
                            style = typo.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = scheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        // Occurrence count badge
                        Surface(
                            color = color.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${group.count}x",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = typo.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = color,
                                fontFamily = GoogleSansCode,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Exception type
                    Text(
                        text = group.exceptionType,
                        style = typo.bodyMedium,
                        color = color,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = GoogleSansCode
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Package name and time range
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val packageName = group.packageName
                        if (group.appName != null && packageName != null) {
                            Text(
                                text = packageName,
                                style = typo.bodySmall,
                                color = scheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontFamily = GoogleSansCode,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CrashTypeBadge(type = group.crashType)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = if (isExpanded) {
                                    Icons.Default.KeyboardArrowUp
                                } else {
                                    Icons.Default.KeyboardArrowDown
                                },
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = scheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Time info
                    Text(
                        text = "Last: ${dateFormat.format(Date(group.lastOccurrence))}",
                        style = typo.labelSmall,
                        color = scheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Expanded crashes list
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .padding(start = 32.dp, top = 8.dp)
                    .fillMaxWidth()
            ) {
                group.crashes.forEach { crash ->
                    CrashGroupChildItem(
                        crash = crash,
                        color = color,
                        onClick = { onCrashClick(crash) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun CrashGroupChildItem(
    crash: CrashLog,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 16.dp)
            .background(
                color = color.copy(alpha = 0.03f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Small indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = color.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp)
                    )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                val preview = crash.content.lines()
                    .firstOrNull { it.contains("Exception") || it.contains("Error") }
                    ?: crash.content.lines().firstOrNull()
                    ?: ""

                Text(
                    text = preview,
                    style = typo.bodySmall,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = dateFormat.format(Date(crash.timestamp)),
                    style = typo.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CrashGroupItemAllStates() {
    val now = System.currentTimeMillis()
    val appCrash = PreviewData.sampleCrashLogs[0]
    val anr = PreviewData.sampleCrashLogs[1]
    val tombstone = PreviewData.sampleCrashLogs[2]
    val messenger = PreviewData.sampleCrashLogs[3]

    val expandedGroup = CrashGroup(
        signature = "sig-multi",
        exceptionType = "IllegalStateException",
        crashes = listOf(
            messenger,
            appCrash.copy(id = 101, timestamp = now - 1000 * 60 * 45),
            messenger.copy(id = 102, timestamp = now - 1000 * 60 * 120),
        ),
        count = 8,
        firstOccurrence = now - 1000 * 60 * 60 * 24,
        lastOccurrence = now - 1000 * 60 * 3,
    )
    val anrGroup = CrashGroup(
        signature = "sig-anr",
        exceptionType = "ANR: Input dispatching timed out",
        crashes = listOf(anr),
        count = 3,
        firstOccurrence = now - 1000 * 60 * 60 * 10,
        lastOccurrence = now - 1000 * 60 * 15,
    )
    val tombstoneGroup = CrashGroup(
        signature = "sig-tomb",
        exceptionType = "Native abort: FORTIFY pthread_mutex_lock called on a destroyed mutex",
        crashes = listOf(tombstone),
        count = 1,
        firstOccurrence = now - 1000 * 60 * 60 * 2,
        lastOccurrence = now - 1000 * 60 * 30,
    )
    val noAppNameGroup = CrashGroup(
        signature = "sig-noapp",
        exceptionType = "RuntimeException",
        crashes = listOf(appCrash.copy(appName = null)),
        count = 2,
        firstOccurrence = now - 1000 * 60 * 60,
        lastOccurrence = now - 1000 * 60 * 10,
    )
    val singleCountGroup = PreviewData.sampleCrashGroup.copy(count = 1)

    Column(
        modifier = Modifier.padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PreviewLabel("Collapsed — app crash")
        CrashGroupItem(
            group = PreviewData.sampleCrashGroup,
            isExpanded = false,
            onGroupClick = {},
            onCrashClick = {},
        )

        PreviewLabel("Expanded — multiple children")
        CrashGroupItem(
            group = expandedGroup,
            isExpanded = true,
            onGroupClick = {},
            onCrashClick = {},
        )

        PreviewLabel("Collapsed — ANR")
        CrashGroupItem(
            group = anrGroup,
            isExpanded = false,
            onGroupClick = {},
            onCrashClick = {},
        )

        PreviewLabel("Expanded — tombstone (long text)")
        CrashGroupItem(
            group = tombstoneGroup,
            isExpanded = true,
            onGroupClick = {},
            onCrashClick = {},
        )

        PreviewLabel("Collapsed — missing app name")
        CrashGroupItem(
            group = noAppNameGroup,
            isExpanded = false,
            onGroupClick = {},
            onCrashClick = {},
        )

        PreviewLabel("Collapsed — single occurrence")
        CrashGroupItem(
            group = singleCountGroup,
            isExpanded = false,
            onGroupClick = {},
            onCrashClick = {},
        )
    }
}

@Composable
private fun PreviewLabel(text: String) {
    Text(
        text = text,
        style = typo.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = scheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

@Preview(showBackground = true, heightDp = 1800)
@Composable
private fun CrashGroupItemAllStatesPreview() {
    StackLensTheme {
        CrashGroupItemAllStates()
    }
}

@Preview(showBackground = true, heightDp = 1800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CrashGroupItemAllStatesDarkPreview() {
    StackLensTheme {
        CrashGroupItemAllStates()
    }
}
