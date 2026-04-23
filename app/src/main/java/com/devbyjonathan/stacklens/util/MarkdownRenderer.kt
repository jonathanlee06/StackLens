package com.devbyjonathan.stacklens.util

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.devbyjonathan.uikit.theme.GoogleSansCode
import com.devbyjonathan.uikit.theme.scheme

/** Tag used to mark inline-code ranges in the AnnotatedString so they can be drawn manually. */
const val INLINE_CODE_ANNOTATION_TAG: String = "inline-code"

/**
 * Renders a minimal subset of markdown that Gemini Nano commonly emits:
 *   **bold**, *italic*, `code`, and `-`/`*` bullet lines.
 * Not a full CommonMark parser — intentionally small and predictable.
 *
 * @param inlineCodeBackground background color drawn behind `inline code` runs.
 *        Pass `Color.Unspecified` (default) for no background.
 * @param inlineCodeColor text color for `inline code` runs. `Color.Unspecified`
 *        (default) inherits from the surrounding Text.
 * @param inlineCodeFontFamily font family for `inline code`. Defaults to monospace.
 */
fun renderInlineMarkdown(
    source: String,
    inlineCodeBackground: Color = Color.Unspecified,
    inlineCodeColor: Color = Color.Unspecified,
    inlineCodeFontFamily: FontFamily = FontFamily.Monospace,
): AnnotatedString {
    val normalized = normalizeBullets(source)
    val codeStyle = SpanStyle(
        fontFamily = inlineCodeFontFamily,
        background = inlineCodeBackground,
        color = inlineCodeColor,
    )
    return buildAnnotatedString {
        var i = 0
        while (i < normalized.length) {
            val c = normalized[i]

            if (c == '*' && i + 1 < normalized.length && normalized[i + 1] == '*') {
                val end = normalized.indexOf("**", startIndex = i + 2)
                if (end > i + 2) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(normalized.substring(i + 2, end))
                    }
                    i = end + 2
                    continue
                }
            }

            if (c == '*' &&
                (i == 0 || normalized[i - 1] != '*') &&
                i + 1 < normalized.length &&
                normalized[i + 1] != '*' &&
                !normalized[i + 1].isWhitespace()
            ) {
                val end = findItalicClose(normalized, i + 1)
                if (end > i + 1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(normalized.substring(i + 1, end))
                    }
                    i = end + 1
                    continue
                }
            }

            if (c == '`') {
                val end = normalized.indexOf('`', i + 1)
                if (end > i + 1) {
                    // Pad with one space on each side. For the SpanStyle.background path these
                    // spaces give the flat block some breathing room; for MarkdownText they mark
                    // the outer edge of the rounded rectangle that's drawn manually.
                    pushStringAnnotation(INLINE_CODE_ANNOTATION_TAG, "")
                    withStyle(codeStyle) {
                        append(' ')
                        append(normalized.substring(i + 1, end))
                        append(' ')
                    }
                    pop()
                    i = end + 1
                    continue
                }
            }

            append(c)
            i++
        }
    }
}

private fun normalizeBullets(source: String): String = buildString {
    source.lines().forEachIndexed { index, raw ->
        if (index > 0) append('\n')
        val trimmed = raw.trimStart()
        val indent = raw.length - trimmed.length
        val isBullet = (trimmed.startsWith("- ") || trimmed.startsWith("* ")) &&
                !trimmed.startsWith("**")
        if (isBullet) {
            repeat(indent) { append(' ') }
            append("• ")
            append(trimmed.drop(2))
        } else {
            append(raw)
        }
    }
}

/**
 * Composable convenience wrapper around [renderInlineMarkdown] that picks inline-code
 * colors from the current [scheme] and memoizes the result so recompositions don't
 * re-parse the same markdown.
 */
@Composable
fun rememberInlineMarkdown(
    source: String,
    background: Color = scheme.surfaceContainerHighest,
    color: Color = Color.Unspecified,
    fontFamily: FontFamily = GoogleSansCode,
): AnnotatedString = remember(source, background, color, fontFamily) {
    renderInlineMarkdown(source, background, color, fontFamily)
}

/**
 * Drop-in replacement for `Text(rememberInlineMarkdown(source), ...)` that draws **rounded**
 * backgrounds behind inline-code runs. `SpanStyle.background` can only produce flat rectangles;
 * this composable captures the text layout and paints rounded rects via `Modifier.drawBehind`.
 *
 * Handles line wrapping — a code span that breaks across lines gets one rounded block per line.
 *
 * @param inlineCodeBackground pill color. Defaults to `scheme.surfaceContainerHighest`.
 * @param cornerRadius radius of the pill. Defaults to 6.dp.
 * @param inlineCodePadding extra horizontal/vertical padding added around the glyph bounds.
 *        The rendered code already includes a single space on each side, so default is 0.
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    inlineCodeBackground: Color = scheme.surfaceContainerHighest,
    inlineCodeColor: Color = Color.Unspecified,
    inlineCodeFontFamily: FontFamily = GoogleSansCode,
    cornerRadius: Dp = 6.dp,
    inlineCodePadding: PaddingValues = PaddingValues(horizontal = 0.dp, vertical = 1.dp),
) {
    val annotated = remember(text, inlineCodeColor, inlineCodeFontFamily) {
        renderInlineMarkdown(
            source = text,
            inlineCodeBackground = Color.Unspecified,
            inlineCodeColor = inlineCodeColor,
            inlineCodeFontFamily = inlineCodeFontFamily,
        )
    }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val density = LocalDensity.current
    val radiusPx = with(density) { cornerRadius.toPx() }
    val padLeftPx = with(density) {
        inlineCodePadding.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr).toPx()
    }
    val padRightPx = with(density) {
        inlineCodePadding.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr).toPx()
    }
    val padTopPx = with(density) { inlineCodePadding.calculateTopPadding().toPx() }
    val padBottomPx = with(density) { inlineCodePadding.calculateBottomPadding().toPx() }

    Text(
        text = annotated,
        modifier = modifier.drawBehind {
            val lay = layout ?: return@drawBehind
            val ranges = annotated.getStringAnnotations(
                tag = INLINE_CODE_ANNOTATION_TAG,
                start = 0,
                end = annotated.length,
            )
            for (range in ranges) {
                val startLine = lay.getLineForOffset(range.start)
                val endLine = lay.getLineForOffset(range.end - 1)
                for (lineIdx in startLine..endLine) {
                    val lineStart = maxOf(range.start, lay.getLineStart(lineIdx))
                    val lineEnd = minOf(range.end, lay.getLineEnd(lineIdx))
                    if (lineStart >= lineEnd) continue
                    val startBox = lay.getBoundingBox(lineStart)
                    val endBox = lay.getBoundingBox(lineEnd - 1)
                    val left = startBox.left - padLeftPx
                    val right = endBox.right + padRightPx
                    val top = lay.getLineTop(lineIdx) - padTopPx
                    val bottom = lay.getLineBottom(lineIdx) + padBottomPx
                    drawRoundRect(
                        color = inlineCodeBackground,
                        topLeft = Offset(left, top),
                        size = Size(right - left, bottom - top),
                        cornerRadius = CornerRadius(radiusPx, radiusPx),
                    )
                }
            }
        },
        onTextLayout = { layout = it },
        style = style,
        color = color,
    )
}

private fun findItalicClose(text: String, from: Int): Int {
    var j = from
    while (j < text.length) {
        if (text[j] == '*' &&
            text[j - 1] != ' ' &&
            text[j - 1] != '*' &&
            (j + 1 >= text.length || text[j + 1] != '*')
        ) return j
        j++
    }
    return -1
}
