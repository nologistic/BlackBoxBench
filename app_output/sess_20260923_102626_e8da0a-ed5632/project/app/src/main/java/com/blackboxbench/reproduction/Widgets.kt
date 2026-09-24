package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Seven segment bit masks for the digits 0..9. */
private val SEGMENT_MASKS = intArrayOf(63, 6, 91, 79, 102, 109, 125, 7, 127, 111)

/** Retro LED read-out used by the mine counter and the clock. */
@Composable
fun LedDisplay(text: String, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val h = size.height
        if (h <= 1f) return@Canvas
        val digitW = h * 0.50f
        val gap = h * 0.13f
        val colonW = h * 0.22f
        var total = 0f
        for ((i, ch) in text.withIndex()) {
            total += if (ch == ':') colonW else digitW
            if (i != text.lastIndex) total += gap
        }
        var x = (size.width - total) / 2f
        for (ch in text) {
            if (ch == ':') {
                val r = h * 0.065f
                drawCircle(color, r, Offset(x + colonW / 2f, h * 0.33f))
                drawCircle(color, r, Offset(x + colonW / 2f, h * 0.67f))
                x += colonW + gap
            } else {
                val d = ch - '0'
                if (d in 0..9) drawSevenSegment(d, x, 0f, digitW, h, color)
                x += digitW + gap
            }
        }
    }
}

private fun DrawScope.drawSevenSegment(digit: Int, x: Float, y: Float, w: Float, h: Float, color: Color) {
    val t = h * 0.15f
    val hh = h / 2f
    val radius = CornerRadius(t / 2f, t / 2f)
    val mask = SEGMENT_MASKS[digit]
    fun seg(bit: Int, ox: Float, oy: Float, sw: Float, sh: Float) {
        val c = if (mask and bit != 0) color else color.copy(alpha = 0.07f)
        drawRoundRect(c, Offset(x + ox, y + oy), Size(sw, sh), radius)
    }
    seg(1, t * 0.75f, 0f, w - t * 1.5f, t)
    seg(2, w - t, t * 0.75f, t, hh - t * 1.15f)
    seg(4, w - t, hh + t * 0.4f, t, hh - t * 1.15f)
    seg(8, t * 0.75f, h - t, w - t * 1.5f, t)
    seg(16, 0f, hh + t * 0.4f, t, hh - t * 1.15f)
    seg(32, 0f, t * 0.75f, t, hh - t * 1.15f)
    seg(64, t * 0.75f, hh - t / 2f, w - t * 1.5f, t)
}

enum class FaceState { NORMAL, WON, LOST }

/** Round status button in the middle of the top bar. */
@Composable
fun FaceButton(state: FaceState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val background = when (state) {
        FaceState.NORMAL -> Color(0xFF22303F)
        FaceState.WON -> Color(0xFF1D4A26)
        FaceState.LOST -> Color(0xFF4A2A22)
    }
    Box(
        modifier
            .clip(CircleShape)
            .background(background)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize().padding(5.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = minOf(size.width, size.height) / 2f
            drawCircle(Color(0xFFFFC93C), r, Offset(cx, cy))
            val ink = Color(0xFF2A1A00)
            when (state) {
                FaceState.NORMAL -> {
                    drawCircle(ink, r * 0.13f, Offset(cx - r * 0.36f, cy - r * 0.26f))
                    drawCircle(ink, r * 0.13f, Offset(cx + r * 0.36f, cy - r * 0.26f))
                    drawArc(
                        color = ink,
                        startAngle = 25f,
                        sweepAngle = 130f,
                        useCenter = false,
                        topLeft = Offset(cx - r * 0.45f, cy - r * 0.25f),
                        size = Size(r * 0.9f, r * 0.8f),
                        style = Stroke(width = r * 0.11f, cap = StrokeCap.Round)
                    )
                }

                FaceState.WON -> {
                    drawRoundRect(
                        color = ink,
                        topLeft = Offset(cx - r * 0.62f, cy - r * 0.42f),
                        size = Size(r * 1.24f, r * 0.34f),
                        cornerRadius = CornerRadius(r * 0.12f, r * 0.12f)
                    )
                    drawRoundRect(
                        color = Color(0xFF16202B),
                        topLeft = Offset(cx - r * 0.58f, cy - r * 0.34f),
                        size = Size(r * 0.42f, r * 0.26f),
                        cornerRadius = CornerRadius(r * 0.1f, r * 0.1f)
                    )
                    drawRoundRect(
                        color = Color(0xFF16202B),
                        topLeft = Offset(cx + r * 0.16f, cy - r * 0.34f),
                        size = Size(r * 0.42f, r * 0.26f),
                        cornerRadius = CornerRadius(r * 0.1f, r * 0.1f)
                    )
                    drawArc(
                        color = ink,
                        startAngle = 25f,
                        sweepAngle = 130f,
                        useCenter = false,
                        topLeft = Offset(cx - r * 0.45f, cy - r * 0.15f),
                        size = Size(r * 0.9f, r * 0.7f),
                        style = Stroke(width = r * 0.11f, cap = StrokeCap.Round)
                    )
                }

                FaceState.LOST -> {
                    val stroke = Stroke(width = r * 0.11f, cap = StrokeCap.Round)
                    val e = r * 0.22f
                    drawLine(ink, Offset(cx - r * 0.52f, cy - r * 0.42f), Offset(cx - r * 0.14f, cy - r * 0.06f), stroke.width, StrokeCap.Round)
                    drawLine(ink, Offset(cx - r * 0.14f, cy - r * 0.42f), Offset(cx - r * 0.52f, cy - r * 0.06f), stroke.width, StrokeCap.Round)
                    drawLine(ink, Offset(cx + r * 0.14f, cy - r * 0.42f), Offset(cx + r * 0.52f, cy - r * 0.06f), stroke.width, StrokeCap.Round)
                    drawLine(ink, Offset(cx + r * 0.52f, cy - r * 0.42f), Offset(cx + r * 0.14f, cy - r * 0.06f), stroke.width, StrokeCap.Round)
                    drawArc(
                        color = ink,
                        startAngle = 200f,
                        sweepAngle = 140f,
                        useCenter = false,
                        topLeft = Offset(cx - r * 0.38f, cy + e),
                        size = Size(r * 0.76f, r * 0.6f),
                        style = stroke
                    )
                }
            }
        }
    }
}

/** Orange flag drawn on covered tiles. */
fun DrawScope.flagGlyph(color: Color = Color(0xFFFF6D00)) {
    val w = size.width
    val h = size.height
    val poleX = w * 0.40f
    drawLine(
        color = Color(0xFFD8DEE6),
        start = Offset(poleX, h * 0.24f),
        end = Offset(poleX, h * 0.76f),
        strokeWidth = w * 0.06f,
        cap = StrokeCap.Round
    )
    val path = Path().apply {
        moveTo(poleX, h * 0.24f)
        lineTo(w * 0.74f, h * 0.36f)
        lineTo(poleX, h * 0.50f)
        close()
    }
    drawPath(path, color)
}

/** Round bomb with a lit fuse, shown when a mine is uncovered. */
fun DrawScope.bombGlyph() {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val cy = h * 0.58f
    val r = minOf(w, h) * 0.27f
    drawCircle(Color(0xFF14181D), r, Offset(cx, cy))
    drawCircle(Color(0xFF3B4249), r * 0.42f, Offset(cx - r * 0.3f, cy - r * 0.35f))
    drawLine(
        color = Color(0xFF6B4A2A),
        start = Offset(cx + r * 0.6f, cy - r * 0.7f),
        end = Offset(cx + r * 1.25f, cy - r * 1.5f),
        strokeWidth = w * 0.05f,
        cap = StrokeCap.Round
    )
    drawCircle(Color(0xFFFFC107), r * 0.32f, Offset(cx + r * 1.4f, cy - r * 1.62f))
}

/** Yellow starburst for the mine that ended the game. */
fun DrawScope.burstGlyph(color: Color = Color(0xFFFFEB3B), spikes: Int = 10) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val outer = minOf(size.width, size.height) * 0.46f
    val inner = outer * 0.34f
    val path = Path()
    for (i in 0 until spikes * 2) {
        val radius = if (i % 2 == 0) outer else inner
        val angle = Math.toRadians((i * 180.0 / spikes) - 90.0)
        val px = cx + (radius * Math.cos(angle)).toFloat()
        val py = cy + (radius * Math.sin(angle)).toFloat()
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    drawPath(path, color)
}

/** Small tab glyphs (bomb, clock, bolt, gear). */
enum class MiniIcon { BOMB, CLOCK, BOLT, GEAR }

fun DrawScope.miniIcon(icon: MiniIcon, tint: Color) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f
    val r = minOf(w, h) * 0.34f
    when (icon) {
        MiniIcon.BOMB -> {
            val body = r * 0.92f
            drawCircle(tint, body, Offset(cx, cy + r * 0.22f))
            drawCircle(
                Color(0x66FFFFFF),
                body * 0.28f,
                Offset(cx - body * 0.32f, cy + r * 0.22f - body * 0.34f)
            )
            drawLine(
                Color(0xFFFFB300),
                Offset(cx + body * 0.55f, cy + r * 0.22f - body * 0.55f),
                Offset(cx + r * 1.25f, cy - r * 0.95f),
                strokeWidth = w * 0.12f,
                cap = StrokeCap.Round
            )
            drawCircle(Color(0xFFFFD54F), w * 0.06f, Offset(cx + r * 1.3f, cy - r * 1.0f))
        }

        MiniIcon.CLOCK -> {
            drawCircle(tint, r, Offset(cx, cy), style = Stroke(width = w * 0.09f))
            drawLine(tint, Offset(cx, cy), Offset(cx, cy - r * 0.55f), w * 0.08f, StrokeCap.Round)
            drawLine(tint, Offset(cx, cy), Offset(cx + r * 0.45f, cy + r * 0.2f), w * 0.08f, StrokeCap.Round)
        }

        MiniIcon.BOLT -> {
            val path = Path().apply {
                moveTo(cx + r * 0.35f, cy - r)
                lineTo(cx - r * 0.55f, cy + r * 0.12f)
                lineTo(cx - r * 0.02f, cy + r * 0.12f)
                lineTo(cx - r * 0.3f, cy + r)
                lineTo(cx + r * 0.6f, cy - r * 0.15f)
                lineTo(cx + r * 0.05f, cy - r * 0.15f)
                close()
            }
            drawPath(path, tint)
        }

        MiniIcon.GEAR -> {
            val ring = r * 0.62f
            for (i in 0 until 7) {
                val angle = Math.toRadians(i * (360.0 / 7.0))
                val sx = cx + (ring * 0.75f * Math.cos(angle)).toFloat()
                val sy = cy + (ring * 0.75f * Math.sin(angle)).toFloat()
                val ex = cx + (ring * 1.32f * Math.cos(angle)).toFloat()
                val ey = cy + (ring * 1.32f * Math.sin(angle)).toFloat()
                drawLine(tint, Offset(sx, sy), Offset(ex, ey), w * 0.17f, StrokeCap.Round)
            }
            drawCircle(tint, ring, Offset(cx, cy), style = Stroke(width = w * 0.2f))
        }
    }
}
