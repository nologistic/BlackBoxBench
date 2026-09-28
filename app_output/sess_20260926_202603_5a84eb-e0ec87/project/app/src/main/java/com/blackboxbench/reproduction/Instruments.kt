package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp

private val SEGMENTS: Map<Char, BooleanArray> = mapOf(
    '0' to booleanArrayOf(true, true, true, true, true, true, false),
    '1' to booleanArrayOf(false, true, true, false, false, false, false),
    '2' to booleanArrayOf(true, true, false, true, true, false, true),
    '3' to booleanArrayOf(true, true, true, true, false, false, true),
    '4' to booleanArrayOf(false, true, true, false, false, true, true),
    '5' to booleanArrayOf(true, false, true, true, false, true, true),
    '6' to booleanArrayOf(true, false, true, true, true, true, true),
    '7' to booleanArrayOf(true, true, true, false, false, false, false),
    '8' to booleanArrayOf(true, true, true, true, true, true, true),
    '9' to booleanArrayOf(true, true, true, true, false, true, true),
    '-' to booleanArrayOf(false, false, false, false, false, false, true),
    ' ' to booleanArrayOf(false, false, false, false, false, false, false)
)

/** A red seven-segment digital readout, as used for the mine counter and the timer. */
@Composable
fun SevenSegmentDisplay(text: String, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawRoundRect(
            color = GameColors.segmentPanel,
            size = Size(size.width, size.height),
            cornerRadius = CornerRadius(size.height * 0.18f)
        )
        val padX = size.width * 0.12f
        val padY = size.height * 0.20f
        val innerW = size.width - padX * 2f
        val innerH = size.height - padY * 2f
        if (innerW <= 0f || innerH <= 0f) return@Canvas

        val units = text.sumOf { if (it == ':') 0.42 else 1.0 }.toFloat()
        val gap = innerW * 0.07f
        val totalGap = gap * (text.length - 1).coerceAtLeast(0)
        val unitW = (innerW - totalGap) / units
        val thickness = innerH * 0.135f

        var x = padX
        for (ch in text) {
            val charW = if (ch == ':') unitW * 0.42f else unitW
            if (ch == ':') {
                val dotR = thickness * 0.45f
                drawCircle(GameColors.segmentOn, dotR, Offset(x + charW / 2f, padY + innerH * 0.32f))
                drawCircle(GameColors.segmentOn, dotR, Offset(x + charW / 2f, padY + innerH * 0.68f))
            } else {
                drawSevenSegmentChar(ch, x, padY, charW, innerH, thickness)
            }
            x += charW + gap
        }
    }
}

private fun DrawScope.drawSevenSegmentChar(
    ch: Char,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    t: Float
) {
    val on = SEGMENTS[ch] ?: SEGMENTS.getValue(' ')
    val round = CornerRadius(t / 2f)
    fun seg(index: Int, rect: Rect) {
        drawRoundRect(
            color = if (on[index]) GameColors.segmentOn else GameColors.segmentOff,
            topLeft = rect.topLeft,
            size = rect.size,
            cornerRadius = round
        )
    }
    val inset = t * 0.6f
    val hLen = w - inset * 2f
    val vLen = h / 2f - t * 2f
    // a (top), g (middle), d (bottom)
    seg(0, Rect(x + inset, y, hLen, t))
    seg(6, Rect(x + inset, y + h / 2f - t / 2f, hLen, t))
    seg(3, Rect(x + inset, y + h - t, hLen, t))
    // f (top-left), b (top-right)
    seg(5, Rect(x, y + t * 1.2f, t, vLen))
    seg(1, Rect(x + w - t, y + t * 1.2f, t, vLen))
    // e (bottom-left), c (bottom-right)
    seg(4, Rect(x, y + h / 2f + t * 0.8f, t, vLen))
    seg(2, Rect(x + w - t, y + h / 2f + t * 0.8f, t, vLen))
}

/** The round smiley button that resets the board. */
@Composable
fun FaceButton(emoji: String, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.30f))
            .background(GameColors.faceButton),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, fontSize = (size.value * 0.52f).sp)
    }
}

/** The small orange pennant drawn on flagged cells. */
@Composable
fun FlagIcon(size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val poleX = w * 0.30f
        drawLine(
            color = Color(0xFF6B2A16),
            start = Offset(poleX, h * 0.16f),
            end = Offset(poleX, h * 0.86f),
            strokeWidth = w * 0.07f,
            cap = StrokeCap.Round
        )
        val flag = Path().apply {
            moveTo(poleX, h * 0.16f)
            lineTo(w * 0.82f, h * 0.36f)
            lineTo(poleX, h * 0.56f)
            close()
        }
        drawPath(flag, Color(0xFFF2762A))
    }
}
