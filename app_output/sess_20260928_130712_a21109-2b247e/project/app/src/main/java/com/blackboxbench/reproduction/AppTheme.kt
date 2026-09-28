package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** AnkiDroid-like palette (observed from the target app). */
object AnkiColors {
    val Primary = Color(0xFF2196F3)
    val PrimaryDark = Color(0xFF1976D2)
    val PrimaryLight = Color(0xFFE3F2FD)
    val DrawerHeader = Color(0xFF2196F3)
    val NewCount = Color(0xFF2196F3)
    val LearnCount = Color(0xFF009688)
    val DueCount = Color(0xFF4CAF50)
    val ZeroCount = Color(0xFFBDBDBD)
    val RowDivider = Color(0xFFE0E0E0)
    val RowHighlight = Color(0xFFE8EEF4)
    val Scrim = Color(0x99000000)
    val AnswerBar = Color(0xFF37474F)
    val Again = Color(0xFFE53935)
    val Hard = Color(0xFF607D8B)
    val Good = Color(0xFF43A047)
    val Easy = Color(0xFF1E88E5)
    val Star = Color(0xFFFFC107)
    val FieldBg = Color(0xFFEFEFEF)
    val TextPrimary = Color(0xFF212121)
    val TextSecondary = Color(0xFF757575)
}

@Composable
fun BenchmarkAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = AnkiColors.Primary,
            onPrimary = Color.White,
            secondary = AnkiColors.PrimaryDark,
            background = Color.White,
            surface = Color.White,
            onSurface = AnkiColors.TextPrimary
        ),
        content = content
    )
}

/** Small canvas-drawn hamburger, independent of any icon font. */
@Composable
fun HamburgerIcon(tint: Color = Color.White, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val stroke = h / 9f
        for (i in 0..2) {
            val y = h * (0.25f + 0.25f * i)
            drawLine(tint, Offset(0f, y), Offset(w, y), strokeWidth = stroke, cap = StrokeCap.Round)
        }
    }
}

/** Vertical three-dot overflow icon. */
@Composable
fun OverflowIcon(tint: Color = Color.White, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val cx = size.width / 2f
        val r = size.width / 9f
        for (i in 0..2) {
            drawCircle(tint, r, Offset(cx, size.height * (0.18f + 0.32f * i)))
        }
    }
}

@Composable
fun SearchGlyph(tint: Color = AnkiColors.TextSecondary, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val r = size.width * 0.32f
        val c = Offset(size.width * 0.42f, size.height * 0.42f)
        drawCircle(tint, r, c, style = Stroke(width = size.width * 0.09f))
        drawLine(
            tint,
            Offset(c.x + r * 0.72f, c.y + r * 0.72f),
            Offset(size.width * 0.94f, size.height * 0.94f),
            strokeWidth = size.width * 0.1f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun EyeGlyph(tint: Color = Color(0xFF616161), modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        drawOval(
            tint,
            topLeft = Offset(0f, h * 0.22f),
            size = androidx.compose.ui.geometry.Size(w, h * 0.56f),
            style = Stroke(width = w * 0.08f)
        )
        drawCircle(tint, w * 0.15f, Offset(w / 2f, h / 2f))
    }
}

/** Simple star (filled or outline). */
@Composable
fun StarGlyph(filled: Boolean, tint: Color = AnkiColors.Star, modifier: Modifier = Modifier) {
    Text(
        text = if (filled) "\u2605" else "\u2606",
        color = if (filled) tint else Color(0xFF9E9E9E),
        fontSize = 20.sp,
        modifier = modifier
    )
}

/** A small glyph text helper used for the few icons without a canvas form. */
@Composable
fun Glyph(text: String, color: Color = Color.White, size: Int = 20, bold: Boolean = false) {
    Text(
        text = text,
        color = color,
        fontSize = size.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
    )
}

@Composable
fun CenteredMessage(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text, color = AnkiColors.TextSecondary, fontSize = 14.sp)
        }
    }
}
