package com.devbyjonathan.stacklens.screen.settings

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.devbyjonathan.uikit.theme.GoogleSansCode
import com.devbyjonathan.uikit.theme.scheme
import com.devbyjonathan.uikit.theme.typo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsScreen(
    onBackClick: () -> Unit
) {
    LegalWebViewScreen(
        title = "Terms & Conditions",
        url = "https://stacklens.devbyjonathan.com/terms/", // Replace with your actual URL
        onBackClick = onBackClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(
    onBackClick: () -> Unit
) {
    LegalWebViewScreen(
        title = "Privacy Policy",
        url = "https://stacklens.devbyjonathan.com/privacy/", // Replace with your actual URL
        onBackClick = onBackClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LegalWebViewScreen(
    title: String,
    url: String,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = scheme.background
                ),
                title = {
                    val muted = scheme.onSurfaceVariant
                    val emphasised = scheme.onSurface
                    val text = buildAnnotatedString {
                        withStyle(SpanStyle(color = muted, fontFamily = GoogleSansCode)) {
                            append("legal")
                        }
                        withStyle(SpanStyle(color = muted, fontFamily = GoogleSansCode)) {
                            append(" / ")
                        }
                        withStyle(
                            SpanStyle(
                                color = emphasised,
                                fontFamily = GoogleSansCode,
                                fontWeight = FontWeight.SemiBold,
                            )
                        ) {
                            append(title)
                        }
                    }
                    Text(text = text, style = typo.titleMedium)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        val isDark = isSystemInDarkTheme()
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            factory = { context ->
                WebView(context).apply {
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = true
                    applyDarkening(isDark)
                    loadUrl(url)
                }
            },
            update = { webView ->
                webView.applyDarkening(isDark)
            }
        )
    }
}

private fun WebView.applyDarkening(isDark: Boolean) {
    if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
        WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, isDark)
    }
}
