package com.devbyjonathan.stacklens.screen.detail

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devbyjonathan.stacklens.ai.CrashInsight
import com.devbyjonathan.stacklens.ai.Severity
import com.devbyjonathan.stacklens.model.fake.PreviewData
import com.devbyjonathan.stacklens.theme.StackLensTheme
import com.devbyjonathan.stacklens.util.MarkdownText
import com.devbyjonathan.uikit.theme.GoogleSansCode
import com.devbyjonathan.uikit.theme.scheme
import com.devbyjonathan.uikit.theme.typo
import java.util.Locale

@Composable
fun AiInsightContent(
    insight: CrashInsight,
    similarCount: Int,
    onAffectedLineClick: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SourceAttributionCard(insight = insight)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(scheme.surface)
                .border(1.dp, scheme.outlineVariant, shape = RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InsightSectionBlock(label = "ROOT CAUSE", body = insight.rootCause)
            HorizontalDivider(thickness = 1.dp, color = scheme.outlineVariant)
            InsightSectionBlock(label = "SUGGESTED FIX", body = insight.suggestedFix)
        }
        StatsRow(
            severity = insight.severity,
            confidence = insight.confidence,
            similarCount = similarCount,
        )
        insight.affectedLine?.let { line ->
            AffectedLineRow(location = line, onClick = onAffectedLineClick)
        }
    }
}

@Composable
private fun SourceAttributionCard(insight: CrashInsight) {
    Surface(
        color = scheme.primaryContainer,
        shape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomEnd = 16.dp,
            bottomStart = 6.dp
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(scheme.surface.copy(alpha = 0.5f))
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = scheme.onPrimaryContainer,
                        modifier = Modifier.size(12.dp),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Gemini Nano",
                        style = typo.labelMedium.copy(
                            fontFamily = GoogleSansCode
                        ),
                        color = scheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "·",
                        style = typo.labelMedium,
                        color = scheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "on device",
                        style = typo.labelMedium.copy(
                            fontFamily = GoogleSansCode
                        ),
                        color = scheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = String.format(Locale.US, "%.2f", insight.confidence),
                    style = typo.labelMedium.copy(fontFamily = GoogleSansCode),
                    color = scheme.onPrimaryContainer,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            MarkdownText(
                text = insight.summary,
                style = typo.bodyMedium.copy(lineHeight = 21.sp),
                color = scheme.onPrimaryContainer,
                inlineCodeBackground = scheme.onPrimaryContainer.copy(alpha = 0.12f),
                inlineCodeColor = scheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun InsightSectionBlock(label: String, body: String) {
    Column {
        Text(
            text = label,
            style = typo.labelSmall.copy(
                fontFamily = GoogleSansCode
            ),
            color = scheme.tertiary,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
        )
        Spacer(modifier = Modifier.height(6.dp))
        MarkdownText(
            text = body,
            style = typo.bodyMedium.copy(lineHeight = 21.sp),
            color = scheme.onSurface,
        )
    }
}

@Composable
private fun StatsRow(severity: Severity, confidence: Float, similarCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatsCard(
            label = "SEVERITY",
            modifier = Modifier.weight(1f),
        ) {
            SeverityPill(severity = severity)
        }
        StatsCard(
            label = "CONFIDENCE",
            modifier = Modifier.weight(1f),
        ) {
            StatsValueText(text = String.format(Locale.US, "%.2f", confidence))
        }
        StatsCard(
            label = "SIMILAR",
            modifier = Modifier.weight(1f),
        ) {
            StatsValueText(text = similarCount.toString())
        }
    }
}

@Composable
private fun StatsCard(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = scheme.surface,
        ),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
        ) {
            Text(
                text = label,
                style = typo.labelSmall.copy(
                    fontFamily = GoogleSansCode,
                    fontSize = 11.sp,
                    letterSpacing = 0.4.sp,
                ),
                color = scheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
            )
            Spacer(modifier = Modifier.height(6.dp))
            // Shared min-height so the pill (with its own padding) and the bare number
            // texts sit on the same baseline across the three cards.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 28.dp)
                    .wrapContentHeight(Alignment.CenterVertically),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                content()
            }
        }
    }
}

@Composable
private fun StatsValueText(text: String) {
    Text(
        text = text,
        style = typo.titleMedium.copy(
            fontFamily = GoogleSansCode,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
        ),
        color = scheme.onSurface,
        maxLines = 1,
        softWrap = false,
    )
}

@Composable
private fun SeverityPill(severity: Severity) {
    val (label, color) = when (severity) {
        Severity.HIGH -> "High" to scheme.error
        Severity.MEDIUM -> "Medium" to scheme.tertiary
        Severity.LOW -> "Low" to scheme.secondary
    }
    Surface(
        color = color.copy(alpha = 0.18f),
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
            style = typo.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            ),
            color = color,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun AffectedLineRow(location: String, onClick: (() -> Unit)?) {
    val shape = RoundedCornerShape(12.dp)
    val rowModifier = Modifier
        .fillMaxWidth()
        .background(scheme.surface, shape = shape)
        .border(1.dp, scheme.outlineVariant, shape = shape)
    val clickableModifier = if (onClick != null) {
        rowModifier.clickable(onClick = onClick)
    } else rowModifier
    Row(
        modifier = clickableModifier.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
            contentDescription = null,
            tint = scheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Affected ",
            style = typo.bodyMedium,
            color = scheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = location,
            style = typo.bodyMedium.copy(fontFamily = GoogleSansCode),
            color = scheme.error,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.weight(1f))
        if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Open in stack",
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun AiInsightContentPreview() {
    StackLensTheme {
        Column(modifier = Modifier
            .background(scheme.background)
            .padding(vertical = 16.dp)) {
            AiInsightContent(
                insight = PreviewData.sampleInsight,
                similarCount = 12,
                onAffectedLineClick = {},
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 700, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AiInsightContentDarkPreview() {
    StackLensTheme {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            AiInsightContent(
                insight = PreviewData.sampleInsight,
                similarCount = 12,
                onAffectedLineClick = {},
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun AiInsightContentLowSeverityPreview() {
    StackLensTheme {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            AiInsightContent(
                insight = PreviewData.sampleInsight.copy(
                    severity = Severity.LOW,
                    confidence = 0.62f,
                    affectedLine = null,
                ),
                similarCount = 1,
            )
        }
    }
}
