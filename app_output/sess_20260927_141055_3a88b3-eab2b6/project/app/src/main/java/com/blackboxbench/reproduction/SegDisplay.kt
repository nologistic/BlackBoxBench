package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Digit segment sets (a,b,c,d,e,f,g) indexed 0..9. */
private val DIGIT_SEGMENTS: Array<IntArray> = arrayOf(
    intArrayOf(1, 1, 1, 1, 1, 1, 0), // 0
    intArrayOf(0, 1, 1, 0, 0, 0, 0), // 1
    intArrayOf(1, 1, 0, 1, 1, 0, 1), // 2
    intArrayOf(1, 1, 1, 1, 0, 0, 1), // 3
    intArrayOf(0, 1, 1, 0, 0, 1, 1), // 4
    intArrayOf(1, 0, 1, 1, 0, 1, 1), // 5
    intArrayOf(1, 0, 1, 1, 1, 1, 1), // 6
    intArrayOf(1, 1, 1, 0, 0, 0, 0), // 7
    intArrayOf(1, 1, 1, 1, 1, 1, 1), // 8
    intArrayOf(1, 1, 1, 1, 0, 1, 1)  // 9
)

private fun DrawScope.drawHSeg(x0: Float, x1: Float, y: Float, t: Float, color: Color) {
    if (color.alpha == 0f) return
    val p = Path().apply {
        moveTo(x0, y)
        lineTo(x0 + t / 2f, y - t / 2f)
        lineTo(x1 - t / 2f, y - t / 2f)
        lineTo(x1, y)
        lineTo(x1 - t / 2f, y + t / 2f)
        lineTo(x0 + t / 2f, y + t / 2f)
        close()
    }
    drawPath(p, color)
}

private fun DrawScope.drawYSeg(x: Float, y0: Float, y1: Float, t: Float, color: Color) {
    if (color.alpha == 0f) return
    val p = Path().apply {
        moveTo(x, y0)
        lineTo(x + t / 2f, y0 + t / 2f)
        lineTo(x + t / 2f, y1 - t / 2f)
        lineTo(x, y1)
        lineTo(x - t / 2f, y1 - t / 2f)
        lineTo(x - t / 2f, y0 + t / 2f)
        close()
    }
    drawPath(p, color)
}

private fun DrawScope.drawDigit(
    digit: Int,
    on: Color,
    off: Color,
    w: Float,
    h: Float,
    t: Float
) {
    val segs = DIGIT_SEGMENTS[digit]
    val left = t / 2f
    val right = w - t / 2f
    val top = t / 2f
    val mid = h / 2f
    val bottom = h - t / 2f
    fun c(onFlag: Int) = if (onFlag == 1) on else off
    // a
    drawHSeg(left, right, top, t, c(segs[0]))
    // b
    drawYSeg(right, top, mid, t, c(segs[1]))
    // c
    drawYSeg(right, mid, bottom, t, c(segs[2]))
    // d
    drawHSeg(left, right, bottom, t, c(segs[3]))
    // e
    drawYSeg(left, mid, bottom, t, c(segs[4]))
    // f
    drawYSeg(left, top, mid, t, c(segs[5]))
    // g
    drawHSeg(left, right, mid, t, c(segs[6]))
}

/** A single 7-segment character cell. */
@Composable
private fun SegChar(ch: Char, dim: Boolean, digitW: Dp, digitH: Dp, on: Color, off: Color) {
    Canvas(modifier = Modifier.width(digitW).height(digitH)) {
        val w = size.width
        val h = size.height
        val t = w * 0.22f
        val o = if (dim) on.copy(alpha = 0.18f) else on
        when {
            ch.isDigit() -> drawDigit(ch - '0', o, off, w, h, t)
            ch == '-' -> drawHSeg(t / 2f, w - t / 2f, h / 2f, t, o)
            ch == ':' -> {
                val r = t * 0.55f
                drawCircle(o, radius = r, center = Offset(w / 2f, h * 0.32f))
                drawCircle(o, radius = r, center = Offset(w / 2f, h * 0.68f))
            }
        }
    }
}

/** Renders a fixed-width string of digits/'/'/':' as a red 7-segment display. */
@Composable
fun SegText(
    text: String,
    digitW: Dp,
    digitH: Dp,
    on: Color,
    off: Color,
    dimLeadingZeros: Boolean
) {
    val firstSignificant = text.indexOfFirst { it.isDigit() && it != '0' }
    Row {
        text.forEachIndexed { i, ch ->
            val dim = dimLeadingZeros && ch == '0' && (firstSignificant == -1 || i < firstSignificant)
            SegChar(ch, dim, digitW, digitH, on, off)
        }
    }
}
