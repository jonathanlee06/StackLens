package com.devbyjonathan.stacklens.screen.detail

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.WrapText
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devbyjonathan.stacklens.common.CrashTypeBadge
import com.devbyjonathan.stacklens.model.CrashLog
import com.devbyjonathan.stacklens.model.CrashType
import com.devbyjonathan.stacklens.theme.StackLensTheme
import com.devbyjonathan.uikit.theme.AppTypography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrashDetailScreen(
    crash: CrashLog,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    var wrapText by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        modifier = Modifier.basicMarquee(),
                        text = crash.appName ?: crash.packageName ?: "Crash Details",
                        style = AppTypography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        ),
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                    onClick = {
                        clipboardManager.setText(AnnotatedString(crash.content))
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "copy icon",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Copy log",
                            style = AppTypography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Button(
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Crash Log: ${crash.packageName}")
                            putExtra(Intent.EXTRA_TEXT, crash.content)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share crash log"))
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "share icon",
                            tint = MaterialTheme.colorScheme.primaryContainer
                        )
                        Text(
                            text = "Share Trace",
                            style = AppTypography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.primaryContainer
                        )
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Metadata card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Type:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(80.dp)
                        )
                        CrashTypeBadge(type = crash.tag)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    MetadataRow("Package", crash.packageName ?: "Unknown")
                    MetadataRow("Time", dateFormat.format(Date(crash.timestamp)))
                    crash.processName?.let { MetadataRow("Process", it) }
                    crash.pid?.let { MetadataRow("PID", it.toString()) }
                }
            }

            // Stack trace
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Stack Trace",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        )
                        IconButton(onClick = { wrapText = !wrapText }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.WrapText,
                                contentDescription = if (wrapText) "Disable wrap" else "Enable wrap",
                                tint = if (wrapText) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }

                    Box(
                        modifier = if (wrapText) {
                            Modifier.fillMaxWidth()
                        } else {
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        }
                    ) {
                        SelectionContainer {
                            Text(
                                text = crash.content,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Preview
@Composable
private fun CrashDetailScreenPreview() {
    val sampleCrash = CrashLog(
        id = 1,
        timestamp = System.currentTimeMillis(),
        packageName = "com.example.app",
        appName = "Example App",
        processName = "com.example.app:service",
        pid = 1234,
        tag = CrashType.DATA_APP_CRASH,
        content = """
            FATAL EXCEPTION: main
            Process: com.example.ecomm, PID: 23456
            Caused by: java.lang.NullPointerException: Attempt to invoke virtual method 'java.lang.String com.example.ecomm.data.model.Product.getName()' on a null object reference
                at com.example.ecomm.ui.products.ProductDetailFragment.bindProductData(ProductDetailFragment.kt:145)
                at com.example.ecomm.ui.products.ProductDetailFragment.access${'$'}bindProductData(ProductDetailFragment.kt:32)
                at com.example.ecomm.ui.products.ProductDetailFragment${'$'}onViewCreated${'$'}1${'$'}1.emit(ProductDetailFragment.kt:88)
                at com.example.ecomm.ui.products.ProductDetailFragment${'$'}onViewCreated${'$'}1${'$'}1.emit(ProductDetailFragment.kt:86)
                at kotlinx.coroutines.flow.FlowKt__TransformKt${'$'}onEach${'$'}${'$'}inlined${'$'}unsafeTransform${'$'}1${'$'}2.emit(SafeCollector.common.kt:113)
                at kotlinx.coroutines.flow.FlowKt__ChannelsKt.emitAllImpl${'$'}FlowKt__ChannelsKt(Channels.kt:51)
                at kotlinx.coroutines.flow.FlowKt__ChannelsKt.emitAll(Channels.kt:37)
                at kotlinx.coroutines.flow.FlowKt__ChannelsKt.access${'$'}emitAll(Channels.kt:1)
                at kotlinx.coroutines.flow.FlowKt__ChannelsKt${'$'}emitAll${'$'}1.invokeSuspend(Unknown Source:11)
                at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:33)
                at kotlinx.coroutines.DispatchedTask.run(DispatchedTask.kt:106)
                at android.os.Handler.handleCallback(Handler.java:938)
                at android.os.Handler.dispatchMessage(Handler.java:99)
                at android.os.Looper.loop(Looper.java:223)
                at android.app.ActivityThread.main(ActivityThread.java:7656)
                at java.lang.reflect.Method.invoke(Native Method)
                at com.android.internal.os.RuntimeInit${'$'}MethodAndArgsCaller.run(RuntimeInit.java:592)
                at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:947)
                ...
        """.trimIndent()
    )

    StackLensTheme {
        CrashDetailScreen(crash = sampleCrash, onBackClick = {})
    }
}
