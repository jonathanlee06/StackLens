package com.devbyjonathan.stacklens.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Syntax highlighting colors for stack traces.
 */
data class StackTraceColors(
    val exception: Color,       // Exception class names (e.g., NullPointerException)
    val causedBy: Color,        // "Caused by:" prefix
    val atKeyword: Color,       // "at" keyword
    val className: Color,       // Class names in stack frames
    val methodName: Color,      // Method names
    val lineNumber: Color,      // Line numbers (e.g., :42)
    val fileName: Color,        // File names (e.g., MainActivity.kt)
    val nativeMethod: Color,    // (Native Method) or (Unknown Source)
    val message: Color,         // Exception message
    val default: Color,         // Default text color
)

/**
 * Highlights a stack trace with syntax coloring.
 */
@Composable
fun highlightStackTrace(
    content: String,
    colors: StackTraceColors,
): AnnotatedString {
    return buildAnnotatedString {
        val lines = content.split("\n")

        lines.forEachIndexed { index, line ->
            when {
                // Exception line: "java.lang.NullPointerException: message"
                line.matches(Regex("^[a-zA-Z][\\w.]*Exception.*")) ||
                        line.matches(Regex("^[a-zA-Z][\\w.]*Error.*")) -> {
                    highlightExceptionLine(line, colors)
                }

                // Caused by line
                line.trimStart().startsWith("Caused by:") -> {
                    highlightCausedByLine(line, colors)
                }

                // Stack frame: "    at com.example.Class.method(File.java:42)"
                line.trimStart().startsWith("at ") -> {
                    highlightStackFrame(line, colors)
                }

                // Suppressed exceptions
                line.trimStart().startsWith("Suppressed:") -> {
                    withStyle(
                        SpanStyle(
                            color = colors.causedBy,
                            fontWeight = FontWeight.SemiBold
                        )
                    ) {
                        append(line)
                    }
                }

                // "... X more" lines
                line.trimStart().matches(Regex("^\\.\\.\\. \\d+ more.*")) -> {
                    withStyle(SpanStyle(color = colors.atKeyword)) {
                        append(line)
                    }
                }

                // Default
                else -> {
                    withStyle(SpanStyle(color = colors.default)) {
                        append(line)
                    }
                }
            }

            // Add newline except for last line
            if (index < lines.size - 1) {
                append("\n")
            }
        }
    }
}

private fun AnnotatedString.Builder.highlightExceptionLine(
    line: String,
    colors: StackTraceColors,
) {
    val colonIndex = line.indexOf(':')
    if (colonIndex > 0) {
        // Exception class name
        withStyle(SpanStyle(color = colors.exception, fontWeight = FontWeight.Bold)) {
            append(line.substring(0, colonIndex))
        }
        // Colon and message
        withStyle(SpanStyle(color = colors.message)) {
            append(line.substring(colonIndex))
        }
    } else {
        // Just exception name without message
        withStyle(SpanStyle(color = colors.exception, fontWeight = FontWeight.Bold)) {
            append(line)
        }
    }
}

private fun AnnotatedString.Builder.highlightCausedByLine(
    line: String,
    colors: StackTraceColors,
) {
    val leadingWhitespace = line.takeWhile { it.isWhitespace() }
    val trimmedLine = line.trimStart()

    append(leadingWhitespace)

    // "Caused by:" prefix
    val causedByPrefix = "Caused by: "
    if (trimmedLine.startsWith(causedByPrefix)) {
        withStyle(SpanStyle(color = colors.causedBy, fontWeight = FontWeight.Bold)) {
            append(causedByPrefix)
        }
        // Rest is exception name + message
        val rest = trimmedLine.substring(causedByPrefix.length)
        val colonIndex = rest.indexOf(':')
        if (colonIndex > 0) {
            withStyle(SpanStyle(color = colors.exception, fontWeight = FontWeight.Bold)) {
                append(rest.substring(0, colonIndex))
            }
            withStyle(SpanStyle(color = colors.message)) {
                append(rest.substring(colonIndex))
            }
        } else {
            withStyle(SpanStyle(color = colors.exception, fontWeight = FontWeight.Bold)) {
                append(rest)
            }
        }
    } else {
        withStyle(SpanStyle(color = colors.causedBy)) {
            append(trimmedLine)
        }
    }
}

private fun AnnotatedString.Builder.highlightStackFrame(
    line: String,
    colors: StackTraceColors,
) {
    val leadingWhitespace = line.takeWhile { it.isWhitespace() }
    val trimmedLine = line.trimStart()

    append(leadingWhitespace)

    // "at " keyword
    withStyle(SpanStyle(color = colors.atKeyword)) {
        append("at ")
    }

    val rest = trimmedLine.substring(3) // Skip "at "

    // Parse: com.example.Class.method(File.java:42)
    val parenStart = rest.indexOf('(')
    val parenEnd = rest.lastIndexOf(')')

    if (parenStart > 0 && parenEnd > parenStart) {
        val methodPart = rest.substring(0, parenStart)
        val locationPart = rest.substring(parenStart + 1, parenEnd)

        // Split class.method
        val lastDot = methodPart.lastIndexOf('.')
        if (lastDot > 0) {
            val className = methodPart.substring(0, lastDot)
            val methodName = methodPart.substring(lastDot + 1)

            // Class name
            withStyle(SpanStyle(color = colors.className)) {
                append(className)
            }
            append(".")
            // Method name
            withStyle(SpanStyle(color = colors.methodName, fontWeight = FontWeight.SemiBold)) {
                append(methodName)
            }
        } else {
            withStyle(SpanStyle(color = colors.methodName)) {
                append(methodPart)
            }
        }

        // Opening paren
        withStyle(SpanStyle(color = colors.default)) {
            append("(")
        }

        // Location: File.java:42 or Native Method
        if (locationPart == "Native Method" || locationPart == "Unknown Source") {
            withStyle(SpanStyle(color = colors.nativeMethod)) {
                append(locationPart)
            }
        } else {
            val colonIndex = locationPart.lastIndexOf(':')
            if (colonIndex > 0) {
                val fileName = locationPart.substring(0, colonIndex)
                val lineNumber = locationPart.substring(colonIndex)

                withStyle(SpanStyle(color = colors.fileName)) {
                    append(fileName)
                }
                withStyle(SpanStyle(color = colors.lineNumber, fontWeight = FontWeight.Bold)) {
                    append(lineNumber)
                }
            } else {
                withStyle(SpanStyle(color = colors.fileName)) {
                    append(locationPart)
                }
            }
        }

        // Closing paren
        withStyle(SpanStyle(color = colors.default)) {
            append(")")
        }

        // Any remaining text after closing paren
        if (parenEnd < rest.length - 1) {
            withStyle(SpanStyle(color = colors.default)) {
                append(rest.substring(parenEnd + 1))
            }
        }
    } else {
        // Couldn't parse, just output as-is
        withStyle(SpanStyle(color = colors.className)) {
            append(rest)
        }
    }
}
