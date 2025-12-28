package com.devbyjonathan.stacklens.common

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devbyjonathan.stacklens.model.CrashType
import com.devbyjonathan.stacklens.theme.StackLensTheme

@Composable
fun CrashTypeBadge(type: CrashType) {
    val (color, text) = when (type) {
        CrashType.DATA_APP_CRASH, CrashType.SYSTEM_APP_CRASH ->
            MaterialTheme.colorScheme.error to "CRASH"

        CrashType.DATA_APP_ANR, CrashType.SYSTEM_APP_ANR ->
            MaterialTheme.colorScheme.tertiary to "ANR"

        CrashType.SYSTEM_TOMBSTONE ->
            MaterialTheme.colorScheme.secondary to "NATIVE"

        else ->
            MaterialTheme.colorScheme.outline to type.displayName.uppercase()
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = color
        )
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