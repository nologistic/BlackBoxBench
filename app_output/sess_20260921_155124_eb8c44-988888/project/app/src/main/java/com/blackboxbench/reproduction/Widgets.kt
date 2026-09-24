package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val SEGMENTS: Map<Char, String> = mapOf(
    '0' to "abcdef",
    '1' to "bc",
    '2' to "abged",
    '3' to "abgcd",
    '4' to "fgbc",
    '5' to "afgcd",
    '6' to "afgecd",
    '7' to "abc",
    '8' to "abcdefg",
    '9' to "abcdfg",
    '-' to "g",
    ' ' to ""
)

/** One seven segment style digit, exactly like the red LED read outs of the target. */
@Composable
fun LedDigit(digit: Char, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val t = minOf(w * 0.26f, h * 0.13f)
        val pad = t * 0.75f
        val mid = h / 2f
        val left = pad
        val right = w - pad
        val top = pad
        val bottom = h - pad
        val lit = SEGMENTS[digit] ?: ""
        val dim = color.copy(alpha = 0.08f)
        val plan = listOf(
            Triple('a', Offset(left, top), Offset(right, top)),
            Triple('g', Offset(left, mid), Offset(right, mid)),
            Triple('d', Offset(left, bottom), Offset(right, bottom)),
            Triple('f', Offset(left, top), Offset(left, mid)),
            Triple('b', Offset(right, top), Offset(right, mid)),
            Triple('e', Offset(left, mid), Offset(left, bottom)),
            Triple('c', Offset(right, mid), Offset(right, bottom))
        )
        for ((name, start, end) in plan) {
            drawLine(
                color = if (lit.contains(name)) color else dim,
                start = start,
                end = end,
                strokeWidth = t,
                cap = StrokeCap.Round
            )
        }
    }
}

/** Multi character LED read out; ':' renders as a two dot separator. */
@Composable
fun LedDisplay(
    text: String,
    color: Color,
    digitWidth: Dp,
    digitHeight: Dp,
    spacing: Dp = 2.dp,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        for ((i, ch) in text.withIndex()) {
            if (ch == ':') {
                Canvas(Modifier.width(digitWidth * 0.42f).height(digitHeight)) {
                    val r = size.width * 0.32f
                    drawCircle(color, radius = r, center = Offset(size.width / 2f, size.height * 0.33f))
                    drawCircle(color, radius = r, center = Offset(size.width / 2f, size.height * 0.67f))
                }
            } else {
                LedDigit(ch, color, Modifier.width(digitWidth).height(digitHeight))
                if (i != text.lastIndex) Box(Modifier.width(spacing))
            }
        }
    }
}

enum class Face { SMILE, COOL, DEAD }

/** The rounded square reset button with the yellow face in the middle. */
@Composable
fun SmileyButton(
    face: Face,
    size: Dp = 46.dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val current = rememberUpdatedState(onClick)
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.18f))
            .background(Color(0xFF1C4A52))
            .pointerInput(Unit) { detectTapGestures { current.value() } },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = this.size.width
            val cx = w / 2f
            val cy = w / 2f
            val fr = w * 0.31f
            drawCircle(Color(0xFFFFD43B), radius = fr, center = Offset(cx, cy))
            val eyeY = cy - fr * 0.35f
            val eyeDx = fr * 0.42f
            when (face) {
                Face.SMILE -> {
                    drawCircle(Color(0xFF1A1A1A), radius = fr * 0.13f, center = Offset(cx - eyeDx, eyeY))
                    drawCircle(Color(0xFF1A1A1A), radius = fr * 0.13f, center = Offset(cx + eyeDx, eyeY))
                    drawArc(
                        color = Color(0xFF1A1A1A),
                        startAngle = 25f,
                        sweepAngle = 130f,
                        useCenter = false,
                        topLeft = Offset(cx - fr * 0.55f, cy - fr * 0.15f),
                        size = Size(fr * 1.1f, fr * 0.95f),
                        style = Stroke(width = fr * 0.14f, cap = StrokeCap.Round)
                    )
                }
                Face.COOL -> {
                    drawArc(
                        color = Color(0xFF1A1A1A),
                        startAngle = 25f,
                        sweepAngle = 130f,
                        useCenter = false,
                        topLeft = Offset(cx - fr * 0.55f, cy - fr * 0.05f),
                        size = Size(fr * 1.1f, fr * 0.85f),
                        style = Stroke(width = fr * 0.14f, cap = StrokeCap.Round)
                    )
                    drawRoundRect(
                        color = Color(0xFF141414),
                        topLeft = Offset(cx - fr * 0.88f, eyeY - fr * 0.26f),
                        size = Size(fr * 1.76f, fr * 0.44f),
                        cornerRadius = CornerRadius(fr * 0.15f)
                    )
                    drawRoundRect(
                        color = Color(0xFF141414),
                        topLeft = Offset(cx - fr * 0.80f, eyeY - fr * 0.20f),
                        size = Size(fr * 0.62f, fr * 0.34f),
                        cornerRadius = CornerRadius(fr * 0.08f)
                    )
                    drawRoundRect(
                        color = Color(0xFF141414),
                        topLeft = Offset(cx + fr * 0.18f, eyeY - fr * 0.20f),
                        size = Size(fr * 0.62f, fr * 0.34f),
                        cornerRadius = CornerRadius(fr * 0.08f)
                    )
                }
                Face.DEAD -> {
                    val s = fr * 0.30f
                    for (dx in listOf(-eyeDx, eyeDx)) {
                        drawLine(
                            Color(0xFF1A1A1A),
                            Offset(cx + dx - s, eyeY - s),
                            Offset(cx + dx + s, eyeY + s),
                            fr * 0.13f,
                            StrokeCap.Round
                        )
                        drawLine(
                            Color(0xFF1A1A1A),
                            Offset(cx + dx - s, eyeY + s),
                            Offset(cx + dx + s, eyeY - s),
                            fr * 0.13f,
                            StrokeCap.Round
                        )
                    }
                    drawArc(
                        color = Color(0xFF1A1A1A),
                        startAngle = 205f,
                        sweepAngle = 130f,
                        useCenter = false,
                        topLeft = Offset(cx - fr * 0.55f, cy + fr * 0.15f),
                        size = Size(fr * 1.1f, fr * 0.9f),
                        style = Stroke(width = fr * 0.14f, cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}

/** Small orange flag used on flagged cells. */
@Composable
fun FlagGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val poleX = w * 0.34f
        drawLine(
            color = Palette.FlagPole,
            start = Offset(poleX, h * 0.20f),
            end = Offset(poleX, h * 0.80f),
            strokeWidth = w * 0.09f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Palette.FlagPole,
            start = Offset(w * 0.16f, h * 0.80f),
            end = Offset(w * 0.58f, h * 0.80f),
            strokeWidth = w * 0.09f,
            cap = StrokeCap.Round
        )
        val flag = Path().apply {
            moveTo(poleX, h * 0.20f)
            lineTo(w * 0.80f, h * 0.37f)
            lineTo(poleX, h * 0.54f)
            close()
        }
        drawPath(flag, Palette.Flag)
    }
}

/** Soft radial burst used for the cell that was detonated. */
@Composable
fun ExplosionGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val r = minOf(size.width, size.height) / 2f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFF3B0), Color(0xFFFFB020), Color(0xFFE8480C)),
                center = center,
                radius = r
            ),
            radius = r
        )
        val rays = 8
        for (i in 0 until rays) {
            val angle = Math.toRadians((i * 360.0 / rays))
            val inner = r * 0.45f
            val outer = r * 0.95f
            drawLine(
                color = Color(0xFFFFE082),
                start = Offset(center.x + (inner * Math.cos(angle)).toFloat(), center.y + (inner * Math.sin(angle)).toFloat()),
                end = Offset(center.x + (outer * Math.cos(angle)).toFloat(), center.y + (outer * Math.sin(angle)).toFloat()),
                strokeWidth = r * 0.12f,
                cap = StrokeCap.Round
            )
        }
    }
}
