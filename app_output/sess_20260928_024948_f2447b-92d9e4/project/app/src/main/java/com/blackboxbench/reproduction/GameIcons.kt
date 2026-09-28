package com.blackboxbench.reproduction

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

/** Small vector-ish icons drawn with Canvas, matching the game's flat style. */

fun DrawScope.drawStar(color: Color, c: Offset, r: Float) {
    val path = Path()
    for (i in 0 until 10) {
        val ang = Math.PI / 2 + i * Math.PI / 5
        val rad = if (i % 2 == 0) r else r * 0.45f
        val x = c.x + (rad * kotlin.math.cos(ang)).toFloat()
        val y = c.y - (rad * kotlin.math.sin(ang)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}

fun DrawScope.drawHeart(color: Color, c: Offset, w: Float) {
    val p = Path().apply {
        moveTo(c.x, c.y + w * 0.30f)
        cubicTo(c.x - w * 0.55f, c.y - w * 0.20f, c.x - w * 0.50f, c.y + w * 0.30f, c.x, c.y + w * 0.70f)
        cubicTo(c.x + w * 0.50f, c.y + w * 0.30f, c.x + w * 0.55f, c.y - w * 0.20f, c.x, c.y + w * 0.30f)
        close()
    }
    drawPath(p, color)
}

fun DrawScope.drawLock(color: Color, c: Offset, w: Float) {
    val bodyTop = c.y - w * 0.05f
    val bodyBottom = c.y + w * 0.45f
    val half = w * 0.35f
    drawRoundRect(
        color = color,
        topLeft = Offset(c.x - half, bodyTop),
        size = Size(half * 2, bodyBottom - bodyTop),
        cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
    )
    val arc = Path().apply {
        arcTo(
            rect = Rect(
                c.x - w * 0.22f, bodyTop - w * 0.42f,
                c.x + w * 0.22f, bodyTop + w * 0.05f
            ),
            startAngleDegrees = 180f, sweepAngleDegrees = 180f, forceMoveTo = false
        )
    }
    drawPath(arc, color = color, style = Stroke(width = w * 0.12f))
}

fun DrawScope.drawPause(color: Color, c: Offset, h: Float) {
    val w = h * 0.35f
    drawRoundRect(color, Offset(c.x - w * 0.6f, c.y - h / 2), Size(w * 0.4f, h), cornerRadius = CornerRadius(2f, 2f))
    drawRoundRect(color, Offset(c.x + w * 0.2f, c.y - h / 2), Size(w * 0.4f, h), cornerRadius = CornerRadius(2f, 2f))
}

fun DrawScope.drawBackArrow(color: Color, c: Offset, h: Float) {
    val w = h * 0.9f
    val sw = h * 0.16f
    drawLine(color, Offset(c.x - w / 2 + h * 0.3f, c.y - h / 2), Offset(c.x - w / 2, c.y), strokeWidth = sw, cap = StrokeCap.Round)
    drawLine(color, Offset(c.x - w / 2, c.y), Offset(c.x - w / 2 + h * 0.3f, c.y + h / 2), strokeWidth = sw, cap = StrokeCap.Round)
    drawLine(color, Offset(c.x - w / 2, c.y), Offset(c.x + w / 2, c.y), strokeWidth = sw, cap = StrokeCap.Round)
}

fun DrawScope.drawCross(color: Color, c: Offset, s: Float) {
    val sw = s * 0.22f
    drawLine(color, Offset(c.x - s / 2, c.y - s / 2), Offset(c.x + s / 2, c.y + s / 2), strokeWidth = sw, cap = StrokeCap.Round)
    drawLine(color, Offset(c.x - s / 2, c.y + s / 2), Offset(c.x + s / 2, c.y - s / 2), strokeWidth = sw, cap = StrokeCap.Round)
}
