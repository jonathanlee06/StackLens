package com.devbyjonathan.stacklens.screen.list

import android.content.res.Configuration
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devbyjonathan.stacklens.common.ShimmerBox
import com.devbyjonathan.stacklens.theme.StackLensTheme
import com.devbyjonathan.uikit.theme.scheme

private val SkeletonTitleWidths = listOf(0.55f, 0.72f, 0.42f, 0.66f)
private val SkeletonExceptionWidths = listOf(0.75f, 0.60f, 0.82f, 0.50f)
private val SkeletonPackageWidths = listOf(0.45f, 0.38f, 0.52f, 0.32f)

/**
 * Skeleton-loading placeholder for the crash group list. Mirrors [CrashGroupItem]
 * shape (tinted card, 44dp icon tile, two text rows, badge+chevron row) so the
 * layout doesn't shift when real content arrives. Widths vary per row to avoid
 * a uniform "grid" look.
 */
@Composable
fun CrashListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 4,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        repeat(itemCount) { i ->
            CrashGroupSkeletonItem(
                titleWidthFraction = SkeletonTitleWidths[i % SkeletonTitleWidths.size],
                exceptionWidthFraction = SkeletonExceptionWidths[i % SkeletonExceptionWidths.size],
                packageWidthFraction = SkeletonPackageWidths[i % SkeletonPackageWidths.size],
            )
        }
    }
}

@Composable
private fun CrashGroupSkeletonItem(
    titleWidthFraction: Float,
    exceptionWidthFraction: Float,
    packageWidthFraction: Float,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(scheme.onSurface.copy(alpha = 0.04f)),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Type-icon tile placeholder.
            ShimmerBox(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(8.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                // Title row: app name + count badge on the right.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(titleWidthFraction)
                            .height(16.dp),
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    ShimmerBox(
                        modifier = Modifier
                            .width(36.dp)
                            .height(22.dp),
                        shape = RoundedCornerShape(12.dp),
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                // Exception line.
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(exceptionWidthFraction)
                        .height(14.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Package + type badge + chevron row.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(packageWidthFraction)
                            .height(12.dp),
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    ShimmerBox(
                        modifier = Modifier
                            .width(52.dp)
                            .height(20.dp),
                        shape = RoundedCornerShape(4.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    ShimmerBox(
                        modifier = Modifier.size(20.dp),
                        shape = RoundedCornerShape(4.dp),
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                // "Last: ..." timestamp line.
                ShimmerBox(
                    modifier = Modifier
                        .width(140.dp)
                        .height(12.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun CrashListSkeletonPreview() {
    StackLensTheme {
        CrashListSkeleton(modifier = Modifier.padding(vertical = 16.dp))
    }
}

@Preview(showBackground = true, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CrashListSkeletonDarkPreview() {
    StackLensTheme {
        CrashListSkeleton(modifier = Modifier.padding(vertical = 16.dp))
    }
}
