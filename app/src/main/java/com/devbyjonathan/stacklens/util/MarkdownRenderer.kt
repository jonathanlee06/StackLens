package com.devbyjonathan.stacklens.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Renders a minimal subset of markdown that Gemini Nano commonly emits:
 *   **bold**, *italic*, `code`, and `-`/`*` bullet lines.
 * Not a full CommonMark parser — intentionally small and predictable.
 */
fun renderInlineMarkdown(source: String): AnnotatedString {
    val normalized = normalizeBullets(source)
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
                    withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
                        append(normalized.substring(i + 1, end))
                    }
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
