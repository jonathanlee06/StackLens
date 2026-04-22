package com.devbyjonathan.stacklens.screen.detail

import android.content.Intent
import android.content.res.Configuration
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devbyjonathan.stacklens.ai.CrashInsight
import com.devbyjonathan.stacklens.ai.CrashInsightService
import com.devbyjonathan.stacklens.ai.DownloadState
import com.devbyjonathan.stacklens.ai.InsightResult
import com.devbyjonathan.stacklens.common.CrashBreadcrumb
import com.devbyjonathan.stacklens.model.CrashLog
import com.devbyjonathan.stacklens.model.fake.PreviewData
import com.devbyjonathan.stacklens.theme.StackLensTheme
import com.devbyjonathan.stacklens.util.MarkdownText
import com.devbyjonathan.uikit.theme.AppTypography
import com.devbyjonathan.uikit.theme.GoogleSansCode
import com.devbyjonathan.uikit.theme.scheme
import com.devbyjonathan.uikit.theme.typo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiInsightScreen(
    crash: CrashLog,
    similarCount: Int,
    crashInsightService: CrashInsightService?,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var result by remember { mutableStateOf<InsightResult?>(null) }
    val downloadState by crashInsightService?.downloadState?.collectAsState()
        ?: remember { mutableStateOf(DownloadState.Idle) }

    LaunchedEffect(crashInsightService, crash.id) {
        if (crashInsightService == null) {
            result = InsightResult.Unavailable
            return@LaunchedEffect
        }
        val cached = crashInsightService.getCachedInsight(crash.id)
        if (cached != null) {
            result = InsightResult.Success(cached)
        } else {
            result = InsightResult.Loading
            result = crashInsightService.analyzeCrash(crash, groupCount = similarCount)
        }
    }

    LaunchedEffect(downloadState) {
        if (downloadState is DownloadState.Completed && result is InsightResult.Downloading) {
            result = InsightResult.Loading
            result = crashInsightService?.analyzeCrash(crash, groupCount = similarCount)
                ?: InsightResult.Unavailable
        }
    }

    AiInsightScreenLayout(
        crashId = crash.id,
        similarCount = similarCount,
        result = result,
        onBackClick = onBackClick,
        onCopyInsight = { insight ->
            clipboardManager.setText(AnnotatedString(formatShareText(insight)))
        },
        onShareFix = { insight ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Fix hint: ${crash.packageName}")
                putExtra(Intent.EXTRA_TEXT, formatShareText(insight))
            }
            context.startActivity(Intent.createChooser(intent, "Share fix"))
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiInsightScreenLayout(
    crashId: Long,
    similarCount: Int,
    result: InsightResult?,
    onBackClick: () -> Unit,
    onCopyInsight: (CrashInsight) -> Unit,
    onShareFix: (CrashInsight) -> Unit,
) {
    Scaffold(
        topBar = {
            CrashBreadcrumb(
                crashId = crashId,
                onBackClick = onBackClick,
                subRoute = "ai",
            )
        },
        bottomBar = {
            val insight = (result as? InsightResult.Success)?.insight ?: return@Scaffold
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(scheme.background)
                    .padding(16.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = scheme.surfaceContainer,
                    ),
                    onClick = { insight?.let(onCopyInsight) },
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            modifier = Modifier.size(14.dp),
                            contentDescription = null,
                            tint = scheme.onSurface
                        )
                        Text(
                            text = "Copy insight",
                            modifier = Modifier.basicMarquee(),
                            style = AppTypography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            ),
                            color = scheme.onSurface,
                            maxLines = 1
                        )
                    }
                }
                Button(
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = scheme.primary,
                    ),
                    onClick = { insight?.let(onShareFix) },
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Default.Share,
                            modifier = Modifier.size(14.dp),
                            contentDescription = null,
                            tint = scheme.onPrimary,
                        )
                        Text(
                            text = "Share fix",
                            modifier = Modifier.basicMarquee(),
                            style = AppTypography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            ),
                            color = scheme.onPrimary,
                            maxLines = 1
                        )
                    }
                }
            }
        },
        contentColor = scheme.background,
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = "AI INSIGHT",
                modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                style = typo.labelSmall.copy(
                    fontFamily = GoogleSansCode
                ),
                color = scheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
            when (val r = result) {
                is InsightResult.Success -> {
                    MarkdownText(
                        text = r.insight.title,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = typo.displayLarge.copy(
                            fontWeight = FontWeight.Light,
                            fontSize = 28.sp,
                        ),
                        color = scheme.onSurface,
                    )
                    AiInsightContent(
                        insight = r.insight,
                        similarCount = similarCount,
                        onAffectedLineClick = onBackClick,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                is InsightResult.Loading -> {
                    AiInsightSkeleton()
                }

                is InsightResult.Downloading -> {
                    LoadingBlock(message = "Preparing on-device model…")
                }

                is InsightResult.Error -> {
                    Text(
                        text = r.message,
                        modifier = Modifier.padding(16.dp),
                        style = typo.bodyMedium,
                        color = scheme.error,
                    )
                }

                InsightResult.Unavailable -> {
                    Text(
                        text = "On-device AI is not available on this device. Requires Android 14+ with Gemini Nano support (Pixel 8+, Samsung S24+).",
                        modifier = Modifier.padding(16.dp),
                        style = typo.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }

                null -> Unit
            }
        }
    }
}

@Composable
private fun LoadingBlock(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = typo.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun AiInsightScreenSuccessPreview() {
    StackLensTheme {
        AiInsightScreenLayout(
            crashId = 1761234518440L,
            similarCount = 12,
            result = InsightResult.Success(PreviewData.sampleInsight),
            onBackClick = {},
            onCopyInsight = {},
            onShareFix = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AiInsightScreenSuccessDarkPreview() {
    StackLensTheme {
        AiInsightScreenLayout(
            crashId = 1761234518440L,
            similarCount = 12,
            result = InsightResult.Success(PreviewData.sampleInsight),
            onBackClick = {},
            onCopyInsight = {},
            onShareFix = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun AiInsightScreenLoadingPreview() {
    StackLensTheme {
        AiInsightScreenLayout(
            crashId = 1761234518440L,
            similarCount = 1,
            result = InsightResult.Loading,
            onBackClick = {},
            onCopyInsight = {},
            onShareFix = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun AiInsightScreenUnavailablePreview() {
    StackLensTheme {
        AiInsightScreenLayout(
            crashId = 1761234518440L,
            similarCount = 1,
            result = InsightResult.Unavailable,
            onBackClick = {},
            onCopyInsight = {},
            onShareFix = {},
        )
    }
}

private fun formatShareText(insight: CrashInsight): String {
    return buildString {
        append(insight.title).append("\n\n")
        append("Summary: ").append(insight.summary).append("\n\n")
        append("Root Cause: ").append(insight.rootCause).append("\n\n")
        append("Suggested Fix: ").append(insight.suggestedFix)
        insight.affectedLine?.let { append("\n\nAffected: ").append(it) }
        append("\n\nSeverity: ").append(insight.severity.name)
        append(" · Confidence: ").append(
            String.format(
                java.util.Locale.US,
                "%.2f",
                insight.confidence
            )
        )
    }
}
