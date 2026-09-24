package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Hand drawn vector glyphs. Everything is painted with primitives so the
 * reproduction never depends on an icon font or an extended icon artifact.
 */
@Composable
fun Ic(kind: String, size: Dp = 24.dp, tint: Color = Color.Unspecified, alpha: Float = 1f) {
    val color = if (tint == Color.Unspecified) LocalContentColor.current else tint
    Canvas(modifier = Modifier.size(size)) {
        drawGlyph(kind, color.copy(alpha = color.alpha * alpha), this.size.minDimension)
    }
}

@Composable
fun IcBadge(kind: String, badge: String, size: Dp = 26.dp, tint: Color = Color.Unspecified, badgeColor: Color) {
    val color = if (tint == Color.Unspecified) LocalContentColor.current else tint
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            drawGlyph(kind, color, this.size.minDimension)
        }
        Text(
            text = badge,
            color = badgeColor,
            fontSize = (size.value * 0.42f).sp,
            modifier = Modifier.padding(top = size * 0.34f, start = size * 0.36f)
        )
    }
}

private fun DrawScope.drawGlyph(kind: String, color: Color, s: Float) {
    val sw = s * 0.085f
    val stroke = Stroke(width = sw, cap = StrokeCap.Round)
    fun p(x: Float, y: Float) = Offset(x * s, y * s)
    fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
        drawLine(color, p(x1, y1), p(x2, y2), strokeWidth = sw, cap = StrokeCap.Round)

    fun poly(fill: Boolean, vararg pts: Pair<Float, Float>, close: Boolean = true) {
        val path = Path()
        pts.forEachIndexed { i, (x, y) ->
            if (i == 0) path.moveTo(x * s, y * s) else path.lineTo(x * s, y * s)
        }
        if (close) path.close()
        drawPath(path, color, style = if (fill) Fill else stroke)
    }

    fun circle(cx: Float, cy: Float, r: Float, fill: Boolean) =
        drawCircle(color, radius = r * s, center = p(cx, cy), style = if (fill) Fill else stroke)

    when (kind) {
        "menu" -> {
            line(0.14f, 0.24f, 0.86f, 0.24f); line(0.14f, 0.5f, 0.86f, 0.5f); line(0.14f, 0.76f, 0.86f, 0.76f)
        }
        "search" -> {
            circle(0.44f, 0.44f, 0.28f, false); line(0.65f, 0.65f, 0.87f, 0.87f)
        }
        "more" -> {
            circle(0.5f, 0.2f, 0.085f, true); circle(0.5f, 0.5f, 0.085f, true); circle(0.5f, 0.8f, 0.085f, true)
        }
        "back" -> {
            line(0.7f, 0.16f, 0.3f, 0.5f); line(0.3f, 0.5f, 0.7f, 0.84f)
        }
        "close" -> {
            line(0.2f, 0.2f, 0.8f, 0.8f); line(0.8f, 0.2f, 0.2f, 0.8f)
        }
        "chevron_up" -> {
            line(0.2f, 0.62f, 0.5f, 0.34f); line(0.5f, 0.34f, 0.8f, 0.62f)
        }
        "chevron_down" -> {
            line(0.2f, 0.38f, 0.5f, 0.66f); line(0.5f, 0.66f, 0.8f, 0.38f)
        }
        "heart", "heart_fill" -> {
            val path = Path()
            path.moveTo(0.5f * s, 0.86f * s)
            path.cubicTo(0.08f * s, 0.6f * s, 0.1f * s, 0.2f * s, 0.32f * s, 0.16f * s)
            path.cubicTo(0.44f * s, 0.14f * s, 0.5f * s, 0.26f * s, 0.5f * s, 0.3f * s)
            path.cubicTo(0.5f * s, 0.26f * s, 0.56f * s, 0.14f * s, 0.68f * s, 0.16f * s)
            path.cubicTo(0.9f * s, 0.2f * s, 0.92f * s, 0.6f * s, 0.5f * s, 0.86f * s)
            path.close()
            drawPath(path, color, style = if (kind == "heart_fill") Fill else stroke)
        }
        "play" -> poly(true, 0.28f to 0.18f, 0.82f to 0.5f, 0.28f to 0.82f)
        "pause" -> {
            drawRect(color, topLeft = p(0.28f, 0.2f), size = Size(0.16f * s, 0.6f * s))
            drawRect(color, topLeft = p(0.56f, 0.2f), size = Size(0.16f * s, 0.6f * s))
        }
        "next" -> {
            poly(true, 0.2f to 0.2f, 0.66f to 0.5f, 0.2f to 0.8f)
            drawRect(color, topLeft = p(0.7f, 0.2f), size = Size(0.11f * s, 0.6f * s))
        }
        "prev" -> {
            drawRect(color, topLeft = p(0.19f, 0.2f), size = Size(0.11f * s, 0.6f * s))
            poly(true, 0.8f to 0.2f, 0.34f to 0.5f, 0.8f to 0.8f)
        }
        "repeat" -> {
            line(0.2f, 0.32f, 0.72f, 0.32f); poly(true, 0.66f to 0.19f, 0.88f to 0.32f, 0.66f to 0.45f)
            line(0.8f, 0.68f, 0.28f, 0.68f); poly(true, 0.34f to 0.55f, 0.12f to 0.68f, 0.34f to 0.81f)
        }
        "shuffle" -> {
            line(0.14f, 0.28f, 0.86f, 0.72f); poly(true, 0.7f to 0.62f, 0.9f to 0.76f, 0.72f to 0.86f)
            line(0.14f, 0.72f, 0.86f, 0.28f); poly(true, 0.72f to 0.14f, 0.9f to 0.24f, 0.7f to 0.38f)
        }
        "note" -> {
            circle(0.34f, 0.74f, 0.15f, true)
            drawRect(color, topLeft = p(0.45f, 0.2f), size = Size(0.1f * s, 0.56f * s))
            line(0.5f, 0.2f, 0.82f, 0.28f)
        }
        "folder" -> {
            poly(false, 0.12f to 0.78f, 0.12f to 0.28f, 0.42f to 0.28f, 0.5f to 0.4f, 0.88f to 0.4f, 0.88f to 0.78f)
        }
        "person" -> {
            circle(0.5f, 0.32f, 0.18f, false)
            val path = Path()
            path.moveTo(0.16f * s, 0.86f * s)
            path.cubicTo(0.16f * s, 0.6f * s, 0.84f * s, 0.6f * s, 0.84f * s, 0.86f * s)
            drawPath(path, color, style = stroke)
        }
        "clock" -> {
            circle(0.5f, 0.5f, 0.38f, false); line(0.5f, 0.5f, 0.5f, 0.26f); line(0.5f, 0.5f, 0.68f, 0.6f)
        }
        "calendar" -> {
            poly(false, 0.14f to 0.76f, 0.14f to 0.28f, 0.86f to 0.28f, 0.86f to 0.76f)
            line(0.14f, 0.42f, 0.86f, 0.42f); line(0.32f, 0.18f, 0.32f, 0.32f); line(0.68f, 0.18f, 0.68f, 0.32f)
        }
        "disc" -> {
            circle(0.5f, 0.5f, 0.44f, true)
            drawCircle(color.copy(alpha = 0.25f), radius = 0.3f * s, center = p(0.5f, 0.5f), style = stroke)
            drawCircle(color.copy(alpha = 0.25f), radius = 0.18f * s, center = p(0.5f, 0.5f), style = stroke)
            drawCircle(Color(0xFFF50057), radius = 0.09f * s, center = p(0.5f, 0.5f))
        }
        "add" -> {
            line(0.5f, 0.18f, 0.5f, 0.82f); line(0.18f, 0.5f, 0.82f, 0.5f)
        }
        "check" -> {
            line(0.18f, 0.54f, 0.42f, 0.76f); line(0.42f, 0.76f, 0.82f, 0.26f)
        }
        "trash" -> {
            poly(false, 0.24f to 0.3f, 0.76f to 0.3f, 0.7f to 0.84f, 0.3f to 0.84f)
            line(0.16f, 0.3f, 0.84f, 0.3f); line(0.4f, 0.18f, 0.6f, 0.18f)
        }
        "edit" -> {
            poly(false, 0.18f to 0.82f, 0.22f to 0.62f, 0.68f to 0.16f, 0.84f to 0.32f, 0.38f to 0.78f)
        }
        "info" -> {
            circle(0.5f, 0.5f, 0.4f, false); circle(0.5f, 0.28f, 0.05f, true); line(0.5f, 0.44f, 0.5f, 0.74f)
        }
        "gear" -> {
            circle(0.5f, 0.5f, 0.22f, false)
            for (i in 0 until 8) {
                val a = Math.toRadians((i * 45).toDouble())
                val x1 = 0.5f + 0.36f * Math.cos(a).toFloat()
                val y1 = 0.5f + 0.36f * Math.sin(a).toFloat()
                val x2 = 0.5f + 0.46f * Math.cos(a).toFloat()
                val y2 = 0.5f + 0.46f * Math.sin(a).toFloat()
                line(x1, y1, x2, y2)
            }
        }
        "refresh" -> {
            drawArc(
                color = color, startAngle = 40f, sweepAngle = 280f, useCenter = false,
                topLeft = p(0.16f, 0.16f), size = Size(0.68f * s, 0.68f * s), style = stroke
            )
            poly(true, 0.78f to 0.22f, 0.94f to 0.42f, 0.68f to 0.44f)
        }
        "list" -> {
            line(0.3f, 0.26f, 0.88f, 0.26f); line(0.3f, 0.5f, 0.88f, 0.5f); line(0.3f, 0.74f, 0.88f, 0.74f)
            circle(0.14f, 0.26f, 0.06f, true); circle(0.14f, 0.5f, 0.06f, true); circle(0.14f, 0.74f, 0.06f, true)
        }
        "share" -> {
            circle(0.74f, 0.22f, 0.11f, true); circle(0.26f, 0.5f, 0.11f, true); circle(0.74f, 0.78f, 0.11f, true)
            line(0.36f, 0.45f, 0.64f, 0.28f); line(0.36f, 0.55f, 0.64f, 0.72f)
        }
        "star" -> poly(
            true,
            0.5f to 0.14f, 0.62f to 0.4f, 0.9f to 0.44f, 0.7f to 0.63f, 0.75f to 0.9f,
            0.5f to 0.77f, 0.25f to 0.9f, 0.3f to 0.63f, 0.1f to 0.44f, 0.38f to 0.4f
        )
        "equalizer" -> {
            drawRect(color, topLeft = p(0.18f, 0.5f), size = Size(0.12f * s, 0.38f * s))
            drawRect(color, topLeft = p(0.44f, 0.24f), size = Size(0.12f * s, 0.64f * s))
            drawRect(color, topLeft = p(0.7f, 0.62f), size = Size(0.12f * s, 0.26f * s))
        }
        "timer" -> {
            circle(0.5f, 0.56f, 0.34f, false); line(0.5f, 0.56f, 0.5f, 0.36f)
            line(0.4f, 0.12f, 0.6f, 0.12f)
        }
        "sort" -> {
            line(0.16f, 0.26f, 0.6f, 0.26f); line(0.16f, 0.5f, 0.48f, 0.5f); line(0.16f, 0.74f, 0.36f, 0.74f)
            poly(true, 0.68f to 0.3f, 0.86f to 0.62f, 0.5f to 0.62f)
        }
        "grid" -> {
            poly(false, 0.16f to 0.46f, 0.16f to 0.16f, 0.46f to 0.16f, 0.46f to 0.46f)
            poly(false, 0.54f to 0.46f, 0.54f to 0.16f, 0.84f to 0.16f, 0.84f to 0.46f)
            poly(false, 0.16f to 0.84f, 0.16f to 0.54f, 0.46f to 0.54f, 0.46f to 0.84f)
            poly(false, 0.54f to 0.84f, 0.54f to 0.54f, 0.84f to 0.54f, 0.84f to 0.84f)
        }
        "sd" -> {
            poly(false, 0.22f to 0.82f, 0.22f to 0.34f, 0.4f to 0.16f, 0.78f to 0.16f, 0.78f to 0.82f)
            line(0.38f, 0.3f, 0.38f, 0.44f); line(0.5f, 0.3f, 0.5f, 0.44f); line(0.62f, 0.3f, 0.62f, 0.44f)
        }
        "drag" -> {
            line(0.22f, 0.35f, 0.78f, 0.35f); line(0.22f, 0.65f, 0.78f, 0.65f)
        }
        "album" -> {
            poly(false, 0.14f to 0.8f, 0.14f to 0.2f, 0.86f to 0.2f, 0.86f to 0.8f)
            circle(0.5f, 0.5f, 0.16f, false)
        }
        "download" -> {
            line(0.5f, 0.16f, 0.5f, 0.6f); poly(true, 0.3f to 0.5f, 0.5f to 0.72f, 0.7f to 0.5f)
            line(0.2f, 0.84f, 0.8f, 0.84f)
        }
        else -> {
            circle(0.5f, 0.5f, 0.36f, false)
        }
    }
}
