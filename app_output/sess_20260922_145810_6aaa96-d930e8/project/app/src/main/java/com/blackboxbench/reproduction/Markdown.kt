package com.blackboxbench.reproduction

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle

/** One rendered block of the preview. */
sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Ordered(val number: String, val text: String) : MdBlock
    data class Bullet(val text: String) : MdBlock
    data class Quote(val text: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    data object Blank : MdBlock
}

fun parseMarkdown(source: String): List<MdBlock> =
    source.split('\n').map { raw ->
        val line = raw.trimEnd()
        val trimmed = line.trimStart()
        when {
            trimmed.isEmpty() -> MdBlock.Blank
            Regex("^#{1,6}\\s+.*").matches(trimmed) -> {
                val level = trimmed.takeWhile { it == '#' }.length
                MdBlock.Heading(level, trimmed.drop(level).trim())
            }

            Regex("^\\d+\\.\\s+.*").matches(trimmed) -> {
                val number = trimmed.takeWhile { it.isDigit() }
                MdBlock.Ordered(number, trimmed.drop(number.length).trimStart().removePrefix(".").trim())
            }

            Regex("^[-*+]\\s+.*").matches(trimmed) -> MdBlock.Bullet(trimmed.drop(1).trim())
            trimmed.startsWith(">") -> MdBlock.Quote(trimmed.drop(1).trim())
            else -> MdBlock.Paragraph(line)
        }
    }

/** Turns inline markup into styled text for the preview. */
fun inlineStyled(text: String, linkColor: Color): AnnotatedString {
    val builder = AnnotatedString.Builder()
    var i = 0
    while (i < text.length) {
        val rest = text.substring(i)
        when {
            rest.startsWith("**") -> {
                val end = rest.indexOf("**", 2)
                if (end > 0) {
                    builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(inlineStyled(rest.substring(2, end), linkColor))
                    }
                    i += end + 2
                } else {
                    builder.append(rest.first()); i++
                }
            }

            rest.startsWith("*") -> {
                val end = rest.indexOf('*', 1)
                if (end > 0) {
                    builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(rest.substring(1, end))
                    }
                    i += end + 1
                } else {
                    builder.append(rest.first()); i++
                }
            }

            rest.startsWith("`") -> {
                val end = rest.indexOf('`', 1)
                if (end > 0) {
                    builder.withStyle(
                        SpanStyle(fontFamily = FontFamily.Monospace, background = linkColor.copy(alpha = 0.12f))
                    ) { append(rest.substring(1, end)) }
                    i += end + 1
                } else {
                    builder.append(rest.first()); i++
                }
            }

            else -> {
                builder.append(rest.first()); i++
            }
        }
    }
    return builder.toAnnotatedString()
}

/** Colours the markdown markers of the text being edited. */
class MarkdownHighlight(
    private val marker: Color,
    private val accent: Color,
    private val enabled: Boolean,
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        if (!enabled) return TransformedText(text, OffsetMapping.Identity)
        return runCatching {
            val builder = AnnotatedString.Builder(text)
            val raw = text.text
            var lineStart = 0
            raw.split('\n').forEach { line ->
                runCatching { styleLine(builder, line, lineStart) }
                lineStart += line.length + 1
            }
            TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
        }.getOrElse { TransformedText(text, OffsetMapping.Identity) }
    }

    private fun styleLine(builder: AnnotatedString.Builder, line: String, offset: Int) {
        val leading = line.takeWhile { it == ' ' || it == '\t' }.length
        val body = line.drop(leading)
        val bodyStart = offset + leading
        when {
            body.startsWith("#") -> {
                val hashes = body.takeWhile { it == '#' }.length
                builder.addStyle(SpanStyle(color = marker), offset, offset + leading + hashes)
            }

            Regex("^\\d+\\.\\s.*").matches(body) -> {
                val markerEnd = body.indexOf(' ') + 1
                builder.addStyle(SpanStyle(color = marker), bodyStart, bodyStart + markerEnd)
            }

            Regex("^[-*+]\\s.*").matches(body) -> {
                builder.addStyle(SpanStyle(color = marker), bodyStart, bodyStart + 2)
            }

            body.startsWith(">") -> {
                builder.addStyle(SpanStyle(color = marker), bodyStart, bodyStart + 1)
            }

            Regex("^x\\s.*").matches(body) -> {
                builder.addStyle(SpanStyle(color = accent, fontWeight = FontWeight.Bold), bodyStart, bodyStart + 2)
            }

            Regex("^\\([A-Z]\\)\\s.*").matches(body) -> {
                builder.addStyle(SpanStyle(color = accent, fontWeight = FontWeight.Bold), bodyStart, bodyStart + 4)
            }
        }
        var index = line.indexOf("**")
        while (index >= 0) {
            val end = line.indexOf("**", index + 2)
            if (end < 0) break
            builder.addStyle(SpanStyle(color = marker), offset + index, offset + index + 2)
            builder.addStyle(SpanStyle(color = marker), offset + end, offset + end + 2)
            index = line.indexOf("**", end + 2)
        }
    }
}
