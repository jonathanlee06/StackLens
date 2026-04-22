package com.devbyjonathan.stacklens.screen.list

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devbyjonathan.stacklens.model.fake.PreviewData
import com.devbyjonathan.stacklens.repository.DayBucket
import com.devbyjonathan.stacklens.repository.EventsTrend
import com.devbyjonathan.stacklens.theme.StackLensTheme
import com.devbyjonathan.uikit.theme.GoogleSans
import com.devbyjonathan.uikit.theme.GoogleSansCode
import com.devbyjonathan.uikit.theme.scheme
import com.devbyjonathan.uikit.theme.typo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun EventsSparklineHeader(
    trend: EventsTrend?,
    windowLabel: String = "7D",
    modifier: Modifier = Modifier,
) {
    val accent = scheme.error
    val onSurfaceVariant = scheme.onSurfaceVariant
    val dateFormat = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(
                color = scheme.surfaceContainer,
                shape = RoundedCornerShape(16.dp),
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                Column() {
                    Text(
                        text = "EVENTS · $windowLabel",
                        style = typo.labelSmall,
                        color = onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = trend?.current?.toString() ?: "—",
                        style = typo.displayLarge.copy(
                            fontFamily = GoogleSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 40.sp
                        ),
                        color = scheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    TrendCaption(trend = trend, accent = accent, muted = onSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Sparkline(
                        points = trend?.buckets?.map { it.count } ?: emptyList(),
                        accent = accent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (trend != null && trend.buckets.isNotEmpty()) {
                            val first = dateFormat.format(Date(trend.buckets.first().dayStartMs))
                            val last = dateFormat.format(Date(trend.buckets.last().dayStartMs))
                            Text(
                                text = "$first",
                                style = typo.labelSmall.copy(fontFamily = GoogleSansCode),
                                color = onSurfaceVariant,
                                fontSize = 12.sp,
                            )
                            Text(
                                text = "$last",
                                style = typo.labelSmall.copy(fontFamily = GoogleSansCode),
                                color = onSurfaceVariant,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendCaption(trend: EventsTrend?, accent: Color, muted: Color) {
    if (trend == null) {
        Text(
            text = "No data yet",
            style = typo.labelMedium,
            color = muted,
        )
        return
    }
    val delta = trend.deltaPercent
    val symbol = when {
        delta > 0.5f -> "↑"
        delta < -0.5f -> "↓"
        else -> "→"
    }
    val color = when {
        delta > 0.5f -> accent
        delta < -0.5f -> scheme.primary
        else -> muted
    }
    val pct = abs(delta).roundToInt()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = when {
                delta > 0.5f -> Icons.Default.ArrowUpward
                delta < -0.5f -> Icons.Default.ArrowDownward
                else -> Icons.AutoMirrored.Filled.ArrowForward
            },
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = "$pct%",
            style = typo.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = " vs prev 7d",
            style = typo.labelMedium,
            color = scheme.onSurfaceVariant,
            fontFamily = GoogleSansCode
        )
    }
}

@Composable
private fun Sparkline(points: List<Int>, accent: Color, modifier: Modifier = Modifier) {
    if (points.isEmpty()) {
        Box(modifier = modifier)
        return
    }
    val max = (points.max()).coerceAtLeast(1)
    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas
        val stepX = size.width / (points.size - 1)
        val line = Path()
        val fill = Path()
        points.forEachIndexed { index, value ->
            val x = index * stepX
            val y = size.height - (value / max.toFloat()) * (size.height - 4f) - 2f
            if (index == 0) {
                line.moveTo(x, y)
                fill.moveTo(x, size.height)
                fill.lineTo(x, y)
            } else {
                line.lineTo(x, y)
                fill.lineTo(x, y)
            }
        }
        fill.lineTo(size.width, size.height)
        fill.close()
        drawPath(path = fill, color = accent.copy(alpha = 0.12f))
        drawPath(
            path = line,
            color = accent,
            style = Stroke(width = 2.dp.toPx()),
        )
        val lastValue = points.last()
        val lastX = (points.size - 1) * stepX
        val lastY = size.height - (lastValue / max.toFloat()) * (size.height - 4f) - 2f
        drawCircle(color = accent, radius = 3.dp.toPx(), center = Offset(lastX, lastY))
    }
}

@Preview(showBackground = true)
@Composable
private fun EventsSparklineHeaderPreview() {
    StackLensTheme {
        EventsSparklineHeader(
            trend = PreviewData.sampleEventsTrend,
            modifier = Modifier.padding(vertical = 16.dp),
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EventsSparklineHeaderDarkPreview() {
    StackLensTheme {
        EventsSparklineHeader(
            trend = PreviewData.sampleEventsTrend,
            modifier = Modifier.padding(vertical = 16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EventsSparklineHeaderEmptyPreview() {
    StackLensTheme {
        EventsSparklineHeader(
            trend = null,
            modifier = Modifier.padding(vertical = 16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EventsSparklineHeaderDownTrendPreview() {
    val dayMs = 24L * 60 * 60 * 1000
    val today = (System.currentTimeMillis() / dayMs) * dayMs
    val counts = listOf(8, 6, 7, 5, 4, 3, 2)
    StackLensTheme {
        EventsSparklineHeader(
            trend = EventsTrend(
                current = counts.sum(),
                previous = 50,
                deltaPercent = -30f,
                buckets = counts.mapIndexed { idx, c ->
                    DayBucket(today - ((counts.size - 1 - idx) * dayMs), c)
                },
            ),
            modifier = Modifier.padding(vertical = 16.dp),
        )
    }
}
