package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
private fun IconCanvas(
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier,
    draw: DrawScope.(Float) -> Unit,
) {
    Canvas(modifier = modifier.size(size)) {
        draw(this.size.minDimension)
    }
}

// --- simple glyphs -------------------------------------------------------

@Composable
fun PlayGlyph(color: Color, size: Dp = 36.dp) = IconCanvas(color, size) { s ->
    val p = Path().apply {
        moveTo(s * 0.28f, s * 0.20f)
        lineTo(s * 0.80f, s * 0.50f)
        lineTo(s * 0.28f, s * 0.80f)
        close()
    }
    drawPath(p, color)
}

@Composable
fun PauseGlyph(color: Color, size: Dp = 36.dp) = IconCanvas(color, size) { s ->
    val w = s * 0.14f
    drawRoundRect(color, topLeft = Offset(s * 0.26f, s * 0.22f), size = Size(w, s * 0.56f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w / 2))
    drawRoundRect(color, topLeft = Offset(s * 0.60f, s * 0.22f), size = Size(w, s * 0.56f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w / 2))
}

@Composable
fun StopGlyph(color: Color, size: Dp = 36.dp) = IconCanvas(color, size) { s ->
    drawRoundRect(color, topLeft = Offset(s * 0.30f, s * 0.30f), size = Size(s * 0.40f, s * 0.40f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.06f))
}

@Composable
fun ResetGlyph(color: Color, size: Dp = 30.dp) = IconCanvas(color, size) { s ->
    val stroke = s * 0.10f
    drawArc(color, startAngle = -60f, sweepAngle = 300f, useCenter = false,
        topLeft = Offset(s * 0.15f, s * 0.15f), size = Size(s * 0.70f, s * 0.70f),
        style = Stroke(width = stroke, cap = StrokeCap.Round))
    val p = Path().apply {
        moveTo(s * 0.85f, s * 0.30f)
        lineTo(s * 0.85f, s * 0.05f)
        lineTo(s * 0.60f, s * 0.18f)
        close()
    }
    drawPath(p, color)
}

@Composable
fun LapGlyph(color: Color, size: Dp = 30.dp) = IconCanvas(color, size) { s ->
    val stroke = s * 0.10f
    drawCircle(color, radius = s * 0.34f, center = Offset(s * 0.5f, s * 0.55f),
        style = Stroke(width = stroke))
    drawLine(color, Offset(s * 0.5f, s * 0.55f), Offset(s * 0.5f, s * 0.33f), stroke, StrokeCap.Round)
    drawLine(color, Offset(s * 0.5f, s * 0.55f), Offset(s * 0.68f, s * 0.62f), stroke, StrokeCap.Round)
    drawRoundRect(color, topLeft = Offset(s * 0.42f, s * 0.08f), size = Size(s * 0.16f, s * 0.08f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.04f))
}

@Composable
fun PlusGlyph(color: Color, size: Dp = 26.dp) = IconCanvas(color, size) { s ->
    val stroke = s * 0.11f
    drawLine(color, Offset(s * 0.5f, s * 0.18f), Offset(s * 0.5f, s * 0.82f), stroke, StrokeCap.Round)
    drawLine(color, Offset(s * 0.18f, s * 0.5f), Offset(s * 0.82f, s * 0.5f), stroke, StrokeCap.Round)
}

@Composable
fun CloseGlyph(color: Color, size: Dp = 22.dp) = IconCanvas(color, size) { s ->
    val stroke = s * 0.13f
    drawLine(color, Offset(s * 0.22f, s * 0.22f), Offset(s * 0.78f, s * 0.78f), stroke, StrokeCap.Round)
    drawLine(color, Offset(s * 0.78f, s * 0.22f), Offset(s * 0.22f, s * 0.78f), stroke, StrokeCap.Round)
}

@Composable
fun CheckGlyph(color: Color, size: Dp = 24.dp) = IconCanvas(color, size) { s ->
    val stroke = s * 0.13f
    val p = Path().apply {
        moveTo(s * 0.18f, s * 0.52f)
        lineTo(s * 0.42f, s * 0.76f)
        lineTo(s * 0.84f, s * 0.24f)
    }
    drawPath(p, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
}

@Composable
fun BackGlyph(color: Color, size: Dp = 26.dp) = IconCanvas(color, size) { s ->
    val stroke = s * 0.10f
    drawLine(color, Offset(s * 0.80f, s * 0.5f), Offset(s * 0.22f, s * 0.5f), stroke, StrokeCap.Round)
    drawLine(color, Offset(s * 0.46f, s * 0.24f), Offset(s * 0.20f, s * 0.5f), stroke, StrokeCap.Round)
    drawLine(color, Offset(s * 0.46f, s * 0.76f), Offset(s * 0.20f, s * 0.5f), stroke, StrokeCap.Round)
}

@Composable
fun MoreGlyph(color: Color, size: Dp = 24.dp) = IconCanvas(color, size) { s ->
    drawCircle(color, s * 0.09f, Offset(s * 0.5f, s * 0.18f))
    drawCircle(color, s * 0.09f, Offset(s * 0.5f, s * 0.5f))
    drawCircle(color, s * 0.09f, Offset(s * 0.5f, s * 0.82f))
}

@Composable
fun ChevronGlyph(color: Color, size: Dp = 22.dp, down: Boolean = true) = IconCanvas(color, size) { s ->
    val stroke = s * 0.12f
    if (down) {
        drawLine(color, Offset(s * 0.24f, s * 0.38f), Offset(s * 0.5f, s * 0.64f), stroke, StrokeCap.Round)
        drawLine(color, Offset(s * 0.76f, s * 0.38f), Offset(s * 0.5f, s * 0.64f), stroke, StrokeCap.Round)
    } else {
        drawLine(color, Offset(s * 0.24f, s * 0.62f), Offset(s * 0.5f, s * 0.36f), stroke, StrokeCap.Round)
        drawLine(color, Offset(s * 0.76f, s * 0.62f), Offset(s * 0.5f, s * 0.36f), stroke, StrokeCap.Round)
    }
}

@Composable
fun TrashGlyph(color: Color, size: Dp = 26.dp) = IconCanvas(color, size) { s ->
    val stroke = s * 0.09f
    drawLine(color, Offset(s * 0.18f, s * 0.28f), Offset(s * 0.82f, s * 0.28f), stroke, StrokeCap.Round)
    drawRoundRect(color, topLeft = Offset(s * 0.30f, s * 0.30f), size = Size(s * 0.40f, s * 0.52f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.05f), style = Stroke(width = stroke))
    drawLine(color, Offset(s * 0.40f, s * 0.18f), Offset(s * 0.60f, s * 0.18f), stroke, StrokeCap.Round)
}

@Composable
fun BackspaceGlyph(color: Color, size: Dp = 28.dp) = IconCanvas(color, size) { s ->
    val stroke = s * 0.09f
    val p = Path().apply {
        moveTo(s * 0.34f, s * 0.22f)
        lineTo(s * 0.86f, s * 0.22f)
        lineTo(s * 0.86f, s * 0.78f)
        lineTo(s * 0.34f, s * 0.78f)
        lineTo(s * 0.12f, s * 0.5f)
        close()
    }
    drawPath(p, color, style = Stroke(width = stroke))
    drawLine(color, Offset(s * 0.46f, s * 0.40f), Offset(s * 0.70f, s * 0.60f), stroke, StrokeCap.Round)
    drawLine(color, Offset(s * 0.70f, s * 0.40f), Offset(s * 0.46f, s * 0.60f), stroke, StrokeCap.Round)
}

@Composable
fun TagGlyph(color: Color, size: Dp = 26.dp) = IconCanvas(color, size) { s ->
    val stroke = s * 0.09f
    val p = Path().apply {
        moveTo(s * 0.14f, s * 0.30f)
        lineTo(s * 0.50f, s * 0.30f)
        lineTo(s * 0.86f, s * 0.62f)
        lineTo(s * 0.50f, s * 0.86f)
        lineTo(s * 0.14f, s * 0.62f)
        close()
    }
    drawPath(p, color, style = Stroke(width = stroke))
}

@Composable
fun BellGlyph(color: Color, size: Dp = 26.dp) = IconCanvas(color, size) { s ->
    val stroke = s * 0.09f
    val p = Path().apply {
        moveTo(s * 0.22f, s * 0.68f)
        lineTo(s * 0.30f, s * 0.34f)
        quadraticBezierTo(s * 0.5f, s * 0.12f, s * 0.70f, s * 0.34f)
        lineTo(s * 0.78f, s * 0.68f)
        close()
    }
    drawPath(p, color, style = Stroke(width = stroke))
    drawLine(color, Offset(s * 0.42f, s * 0.78f), Offset(s * 0.58f, s * 0.78f), stroke, StrokeCap.Round)
}

@Composable
fun VibrateGlyph(color: Color, size: Dp = 26.dp) = IconCanvas(color, size) { s ->
    val stroke = s * 0.09f
    drawRoundRect(color, topLeft = Offset(s * 0.34f, s * 0.28f), size = Size(s * 0.32f, s * 0.44f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.06f), style = Stroke(width = stroke))
    drawLine(color, Offset(s * 0.18f, s * 0.36f), Offset(s * 0.18f, s * 0.64f), stroke, StrokeCap.Round)
    drawLine(color, Offset(s * 0.82f, s * 0.36f), Offset(s * 0.82f, s * 0.64f), stroke, StrokeCap.Round)
}

@Composable
fun CalendarGlyph(color: Color, size: Dp = 26.dp) = IconCanvas(color, size) { s ->
    val stroke = s * 0.08f
    drawRoundRect(color, topLeft = Offset(s * 0.16f, s * 0.22f), size = Size(s * 0.68f, s * 0.60f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.08f), style = Stroke(width = stroke))
    drawLine(color, Offset(s * 0.16f, s * 0.42f), Offset(s * 0.84f, s * 0.42f), stroke, StrokeCap.Round)
    drawLine(color, Offset(s * 0.36f, s * 0.14f), Offset(s * 0.36f, s * 0.30f), stroke, StrokeCap.Round)
    drawLine(color, Offset(s * 0.64f, s * 0.14f), Offset(s * 0.64f, s * 0.30f), stroke, StrokeCap.Round)
}

@Composable
fun GraphGlyph(color: Color, size: Dp = 26.dp) = IconCanvas(color, size) { s ->
    drawRoundRect(color, topLeft = Offset(s * 0.26f, s * 0.46f), size = Size(s * 0.14f, s * 0.34f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.04f))
    drawRoundRect(color, topLeft = Offset(s * 0.46f, s * 0.30f), size = Size(s * 0.14f, s * 0.50f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.04f))
    drawRoundRect(color, topLeft = Offset(s * 0.66f, s * 0.20f), size = Size(s * 0.14f, s * 0.60f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.04f))
}

@Composable
fun MoonGlyph(color: Color, size: Dp = 26.dp) = IconCanvas(color, size) { s ->
    val p = Path().apply {
        moveTo(s * 0.66f, s * 0.16f)
        quadraticBezierTo(s * 0.24f, s * 0.30f, s * 0.40f, s * 0.66f)
        quadraticBezierTo(s * 0.52f, s * 0.90f, s * 0.80f, s * 0.78f)
        quadraticBezierTo(s * 0.48f, s * 0.72f, s * 0.66f, s * 0.16f)
        close()
    }
    drawPath(p, color)
    drawCircle(color, s * 0.05f, Offset(s * 0.72f, s * 0.26f))
    drawCircle(color, s * 0.035f, Offset(s * 0.82f, s * 0.40f))
}

@Composable
fun SunGlyph(color: Color, size: Dp = 26.dp) = IconCanvas(color, size) { s ->
    val stroke = s * 0.09f
    drawCircle(color, s * 0.20f, Offset(s * 0.5f, s * 0.5f), style = Stroke(width = stroke))
    for (i in 0 until 8) {
        val a = Math.toRadians((i * 45).toDouble())
        val dx = Math.cos(a).toFloat(); val dy = Math.sin(a).toFloat()
        drawLine(color, Offset(s * (0.5f + dx * 0.32f), s * (0.5f + dy * 0.32f)),
            Offset(s * (0.5f + dx * 0.44f), s * (0.5f + dy * 0.44f)), stroke, StrokeCap.Round)
    }
}

@Composable
fun PauseCircleGlyph(color: Color, size: Dp = 30.dp) = IconCanvas(color, size) { s ->
    val stroke = s * 0.09f
    drawCircle(color, s * 0.40f, Offset(s * 0.5f, s * 0.5f), style = Stroke(width = stroke))
    drawLine(color, Offset(s * 0.40f, s * 0.36f), Offset(s * 0.40f, s * 0.64f), s * 0.10f, StrokeCap.Round)
    drawLine(color, Offset(s * 0.60f, s * 0.36f), Offset(s * 0.60f, s * 0.64f), s * 0.10f, StrokeCap.Round)
}

// --- bottom navigation icons --------------------------------------------

@Composable
fun AlarmTabIcon(color: Color, size: Dp = 26.dp) = IconCanvas(color, size) { s ->
    val stroke = s * 0.09f
    drawCircle(color, s * 0.32f, Offset(s * 0.5f, s * 0.56f), style = Stroke(width = stroke))
    drawLine(color, Offset(s * 0.5f, s * 0.56f), Offset(s * 0.5f, s * 0.38f), stroke, StrokeCap.Round)
    drawLine(color, Offset(s * 0.5f, s * 0.56f), Offset(s * 0.64f, s * 0.64f), stroke, StrokeCap.Round)
    drawLine(color, Offset(s * 0.16f, s * 0.18f), Offset(s * 0.34f, s * 0.30f), stroke, StrokeCap.Round)
    drawLine(color, Offset(s * 0.84f, s * 0.18f), Offset(s * 0.66f, s * 0.30f), stroke, StrokeCap.Round)
}

@Composable
fun ClockTabIcon(color: Color, size: Dp = 26.dp) = IconCanvas(color, size) { s ->
    val stroke = s * 0.09f
    drawCircle(color, s * 0.38f, Offset(s * 0.5f, s * 0.5f), style = Stroke(width = stroke))
    drawLine(color, Offset(s * 0.5f, s * 0.5f), Offset(s * 0.5f, s * 0.28f), stroke, StrokeCap.Round)
    drawLine(color, Offset(s * 0.5f, s * 0.5f), Offset(s * 0.68f, s * 0.60f), stroke, StrokeCap.Round)
}

@Composable
fun TimerTabIcon(color: Color, size: Dp = 26.dp) = IconCanvas(color, size) { s ->
    val p = Path().apply {
        moveTo(s * 0.24f, s * 0.14f); lineTo(s * 0.76f, s * 0.14f)
        lineTo(s * 0.5f, s * 0.5f); close()
    }
    val q = Path().apply {
        moveTo(s * 0.24f, s * 0.86f); lineTo(s * 0.76f, s * 0.86f)
        lineTo(s * 0.5f, s * 0.5f); close()
    }
    drawPath(p, color); drawPath(q, color)
}

@Composable
fun StopwatchTabIcon(color: Color, size: Dp = 26.dp) = IconCanvas(color, size) { s ->
    val stroke = s * 0.09f
    drawCircle(color, s * 0.34f, Offset(s * 0.5f, s * 0.56f), style = Stroke(width = stroke))
    drawLine(color, Offset(s * 0.5f, s * 0.56f), Offset(s * 0.5f, s * 0.38f), stroke, StrokeCap.Round)
    drawLine(color, Offset(s * 0.42f, s * 0.14f), Offset(s * 0.58f, s * 0.14f), stroke, StrokeCap.Round)
}

@Composable
fun BedtimeTabIcon(color: Color, size: Dp = 26.dp) = IconCanvas(color, size) { s ->
    val stroke = s * 0.09f
    drawLine(color, Offset(s * 0.12f, s * 0.42f), Offset(s * 0.12f, s * 0.78f), stroke, StrokeCap.Round)
    drawLine(color, Offset(s * 0.12f, s * 0.56f), Offset(s * 0.38f, s * 0.34f), stroke, StrokeCap.Round)
    drawLine(color, Offset(s * 0.38f, s * 0.34f), Offset(s * 0.62f, s * 0.58f), stroke, StrokeCap.Round)
    drawRoundRect(color, topLeft = Offset(s * 0.14f, s * 0.50f), size = Size(s * 0.72f, s * 0.30f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.08f), style = Stroke(width = stroke))
}
