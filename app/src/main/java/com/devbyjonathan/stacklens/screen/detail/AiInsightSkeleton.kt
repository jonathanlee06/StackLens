package com.devbyjonathan.stacklens.screen.detail

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devbyjonathan.stacklens.common.ShimmerBox
import com.devbyjonathan.stacklens.theme.StackLensTheme
import com.devbyjonathan.uikit.theme.scheme

@Composable
fun AiInsightSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ShimmerBox(Modifier
                .fillMaxWidth(0.88f)
                .height(28.dp))
            ShimmerBox(Modifier
                .fillMaxWidth(0.55f)
                .height(28.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SourceAttributionSkeleton()
            InsightPanelSkeleton()
            StatsRowSkeleton()
            AffectedLineSkeleton()
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SourceAttributionSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomEnd = 16.dp,
                    bottomStart = 6.dp,
                )
            )
            .background(scheme.primaryContainer.copy(alpha = 0.45f))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShimmerBox(modifier = Modifier.size(28.dp), shape = CircleShape)
            Spacer(modifier = Modifier.width(8.dp))
            ShimmerBox(Modifier
                .width(140.dp)
                .height(14.dp))
            Spacer(modifier = Modifier.weight(1f))
            ShimmerBox(Modifier
                .width(36.dp)
                .height(14.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ShimmerBox(Modifier
                .fillMaxWidth()
                .height(14.dp))
            ShimmerBox(Modifier
                .fillMaxWidth(0.92f)
                .height(14.dp))
            ShimmerBox(Modifier
                .fillMaxWidth(0.6f)
                .height(14.dp))
        }
    }
}

@Composable
private fun InsightPanelSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.surface)
            .border(1.dp, scheme.outlineVariant, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SkeletonSection(lineWidths = listOf(1f, 0.9f, 0.75f))
        HorizontalDivider(thickness = 1.dp, color = scheme.outlineVariant)
        SkeletonSection(lineWidths = listOf(1f, 0.85f, 0.7f, 0.5f))
    }
}

@Composable
private fun SkeletonSection(lineWidths: List<Float>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ShimmerBox(Modifier
            .width(96.dp)
            .height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            lineWidths.forEach { fraction ->
                ShimmerBox(Modifier
                    .fillMaxWidth(fraction)
                    .height(14.dp))
            }
        }
    }
}

@Composable
private fun StatsRowSkeleton() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(3) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(scheme.surface)
                    .padding(horizontal = 10.dp, vertical = 10.dp),
            ) {
                ShimmerBox(Modifier
                    .width(64.dp)
                    .height(10.dp))
                Spacer(modifier = Modifier.height(10.dp))
                ShimmerBox(Modifier
                    .width(56.dp)
                    .height(20.dp))
            }
        }
    }
}

@Composable
private fun AffectedLineSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(scheme.surface)
            .border(1.dp, scheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShimmerBox(Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        ShimmerBox(Modifier
            .width(56.dp)
            .height(14.dp))
        Spacer(modifier = Modifier.width(8.dp))
        ShimmerBox(Modifier
            .width(120.dp)
            .height(14.dp))
        Spacer(modifier = Modifier.weight(1f))
        ShimmerBox(Modifier.size(18.dp))
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun AiInsightSkeletonPreview() {
    StackLensTheme {
        AiInsightSkeleton(modifier = Modifier.background(scheme.background))
    }
}

@Preview(showBackground = true, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AiInsightSkeletonDarkPreview() {
    StackLensTheme {
        AiInsightSkeleton(modifier = Modifier.background(scheme.background))
    }
}
