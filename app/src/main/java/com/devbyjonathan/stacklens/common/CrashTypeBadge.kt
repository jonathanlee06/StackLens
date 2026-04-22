package com.devbyjonathan.stacklens.common

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devbyjonathan.stacklens.model.CrashType
import com.devbyjonathan.stacklens.theme.StackLensTheme
import com.devbyjonathan.uikit.theme.GoogleSansCode
import com.devbyjonathan.uikit.theme.scheme
import com.devbyjonathan.uikit.theme.typo

@Composable
fun CrashTypeBadge(type: CrashType) {
    val (color, text) = when (type) {
        CrashType.DATA_APP_CRASH, CrashType.SYSTEM_APP_CRASH ->
            scheme.error to "CRASH"

        CrashType.DATA_APP_ANR, CrashType.SYSTEM_APP_ANR ->
            scheme.tertiary to "ANR"

        CrashType.SYSTEM_TOMBSTONE ->
            scheme.secondary to "NATIVE"

        else ->
            scheme.outline to type.displayName.uppercase()
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = typo.labelSmall.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = color
        )
    }
}

@Composable
fun CrashTypeBadgeDetail(type: CrashType, timestamp: Long) {
    val (color, text) = when (type) {
        CrashType.DATA_APP_CRASH, CrashType.SYSTEM_APP_CRASH ->
            scheme.errorContainer to "CRASH"

        CrashType.DATA_APP_ANR, CrashType.SYSTEM_APP_ANR ->
            scheme.tertiaryContainer to "ANR"

        CrashType.SYSTEM_TOMBSTONE ->
            scheme.secondaryContainer to "NATIVE"

        else ->
            scheme.outline to type.displayName.uppercase()
    }

    Row(
        modifier = Modifier
            .background(
                color = color,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = text,
            style = typo.labelSmall.copy(
                fontFamily = GoogleSansCode,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
            ),
        )
        Text(
            text = "·",
            style = typo.labelSmall.copy(
                fontFamily = GoogleSansCode,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
            ),
        )
        Text(
            text = relativeTime(timestamp),
            style = typo.labelSmall.copy(
                fontFamily = GoogleSansCode,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
            ),
        )
    }
}

private fun relativeTime(timestamp: Long): String {
    val delta = (System.currentTimeMillis() - timestamp).coerceAtLeast(0)
    val seconds = delta / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    return when {
        seconds < 60 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> "${days / 7}w ago"
    }
}

@Preview(showBackground = true)
@Composable
private fun CrashTypeBadgePreview() {
    StackLensTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CrashTypeBadge(type = CrashType.DATA_APP_CRASH)
            CrashTypeBadge(type = CrashType.DATA_APP_ANR)
            CrashTypeBadge(type = CrashType.SYSTEM_TOMBSTONE)
        }
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CrashTypeBadgeDarkPreview() {
    StackLensTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CrashTypeBadge(type = CrashType.DATA_APP_CRASH)
            CrashTypeBadge(type = CrashType.DATA_APP_ANR)
            CrashTypeBadge(type = CrashType.SYSTEM_TOMBSTONE)
        }
    }
}