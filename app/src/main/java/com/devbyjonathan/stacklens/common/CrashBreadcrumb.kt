package com.devbyjonathan.stacklens.common

import android.content.res.Configuration
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.devbyjonathan.stacklens.theme.StackLensTheme
import com.devbyjonathan.uikit.theme.GoogleSansCode
import com.devbyjonathan.uikit.theme.scheme
import com.devbyjonathan.uikit.theme.typo

/**
 * Short id for a crash — last 5 digits of the timestamp-based long id. Rendered in breadcrumbs.
 */
fun shortCrashId(id: Long): String {
    val mod = (id % 100000).toString().padStart(5, '0')
    return "#$mod"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrashBreadcrumb(
    crashId: Long,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    subRoute: String? = null,
    colors: TopAppBarColors = TopAppBarDefaults.centerAlignedTopAppBarColors(
        containerColor = scheme.background,
    ),
    actions: @Composable RowScope.() -> Unit = {},
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        colors = colors,
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        title = {
            val muted = scheme.onSurfaceVariant
            val emphasised = scheme.onSurface
            val text = buildAnnotatedString {
                withStyle(SpanStyle(color = muted, fontFamily = GoogleSansCode)) {
                    append("crashes")
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
                    append(shortCrashId(crashId))
                }
                if (subRoute != null) {
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
                        append(subRoute)
                    }
                }
            }
            Text(text = text, style = typo.titleMedium)
        },
        actions = actions,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun CrashBreadcrumbPreview() {
    StackLensTheme {
        CrashBreadcrumb(
            crashId = 1761234518440L,
            onBackClick = {},
            actions = {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun CrashBreadcrumbWithSubRoutePreview() {
    StackLensTheme {
        CrashBreadcrumb(
            crashId = 1761234518440L,
            onBackClick = {},
            subRoute = "ai",
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CrashBreadcrumbDarkPreview() {
    StackLensTheme {
        CrashBreadcrumb(
            crashId = 1761234518440L,
            onBackClick = {},
            subRoute = "ai",
        )
    }
}
