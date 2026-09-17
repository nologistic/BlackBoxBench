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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SortGlyph(tint: Color, size: Dp = 22.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.09f
        val cx1 = w * 0.34f
        val cx2 = w * 0.68f
        drawLine(tint, Offset(cx1, h * 0.82f), Offset(cx1, h * 0.2f), sw, StrokeCap.Round)
        drawLine(tint, Offset(cx1 - w * 0.14f, h * 0.34f), Offset(cx1, h * 0.18f), sw, StrokeCap.Round)
        drawLine(tint, Offset(cx1 + w * 0.14f, h * 0.34f), Offset(cx1, h * 0.18f), sw, StrokeCap.Round)
        drawLine(tint, Offset(cx2, h * 0.18f), Offset(cx2, h * 0.8f), sw, StrokeCap.Round)
        drawLine(tint, Offset(cx2 - w * 0.14f, h * 0.66f), Offset(cx2, h * 0.82f), sw, StrokeCap.Round)
        drawLine(tint, Offset(cx2 + w * 0.14f, h * 0.66f), Offset(cx2, h * 0.82f), sw, StrokeCap.Round)
    }
}

@Composable
fun MicGlyph(tint: Color, size: Dp = 22.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.085f
        val cx = w * 0.5f
        val capsule = Rect(cx - w * 0.14f, h * 0.14f, cx + w * 0.14f, h * 0.58f)
        drawRoundRect(
            tint,
            topLeft = capsule.topLeft,
            size = Size(capsule.width, capsule.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.14f),
            style = Stroke(sw)
        )
        drawArc(
            tint,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(cx - w * 0.26f, h * 0.36f),
            size = Size(w * 0.52f, h * 0.4f),
            style = Stroke(sw, cap = StrokeCap.Round)
        )
        drawLine(tint, Offset(cx, h * 0.78f), Offset(cx, h * 0.9f), sw, StrokeCap.Round)
    }
}

@Composable
fun TagGlyph(tint: Color, size: Dp = 20.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.09f
        val p = Path().apply {
            moveTo(w * 0.12f, h * 0.2f)
            lineTo(w * 0.58f, h * 0.2f)
            lineTo(w * 0.9f, h * 0.5f)
            lineTo(w * 0.58f, h * 0.8f)
            lineTo(w * 0.12f, h * 0.8f)
            close()
        }
        drawPath(p, tint, style = Stroke(sw, cap = StrokeCap.Round))
        drawCircle(tint, radius = w * 0.07f, center = Offset(w * 0.28f, h * 0.5f))
    }
}

@Composable
fun FlagGlyph(tint: Color, size: Dp = 20.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.1f
        drawLine(tint, Offset(w * 0.22f, h * 0.85f), Offset(w * 0.22f, h * 0.16f), sw, StrokeCap.Round)
        val p = Path().apply {
            moveTo(w * 0.22f, h * 0.2f)
            lineTo(w * 0.84f, h * 0.2f)
            lineTo(w * 0.66f, h * 0.42f)
            lineTo(w * 0.84f, h * 0.64f)
            lineTo(w * 0.22f, h * 0.64f)
            close()
        }
        drawPath(p, tint)
    }
}

@Composable
fun ClockGlyph(tint: Color, size: Dp = 22.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.085f
        drawCircle(tint, radius = w * 0.4f, center = Offset(w / 2, h / 2), style = Stroke(sw))
        drawLine(tint, Offset(w / 2, h * 0.32f), Offset(w / 2, h / 2), sw, StrokeCap.Round)
        drawLine(tint, Offset(w / 2, h / 2), Offset(w * 0.68f, h * 0.58f), sw, StrokeCap.Round)
    }
}

@Composable
fun SubtaskGlyph(tint: Color, size: Dp = 22.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.09f
        drawLine(tint, Offset(w * 0.2f, h * 0.24f), Offset(w * 0.2f, h * 0.62f), sw, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.2f, h * 0.62f), Offset(w * 0.42f, h * 0.62f), sw, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.34f, h * 0.48f), Offset(w * 0.5f, h * 0.62f), sw, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.34f, h * 0.76f), Offset(w * 0.5f, h * 0.62f), sw, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.5f, h * 0.2f), Offset(w * 0.84f, h * 0.2f), sw, StrokeCap.Round)
    }
}

@Composable
fun PinGlyph(tint: Color, size: Dp = 22.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.085f
        val cx = w * 0.5f
        val p = Path().apply {
            moveTo(cx, h * 0.88f)
            cubicTo(w * 0.16f, h * 0.55f, w * 0.22f, h * 0.14f, cx, h * 0.14f)
            cubicTo(w * 0.78f, h * 0.14f, w * 0.84f, h * 0.55f, cx, h * 0.88f)
            close()
        }
        drawPath(p, tint, style = Stroke(sw))
        drawCircle(tint, radius = w * 0.1f, center = Offset(cx, h * 0.42f), style = Stroke(sw))
    }
}

@Composable
fun AttachmentGlyph(tint: Color, size: Dp = 22.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.085f
        drawLine(tint, Offset(w * 0.3f, h * 0.8f), Offset(w * 0.72f, h * 0.34f), sw, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.16f, h * 0.62f), Offset(w * 0.58f, h * 0.16f), sw, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.3f, h * 0.8f), Offset(w * 0.44f, h * 0.86f), sw, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.58f, h * 0.16f), Offset(w * 0.74f, h * 0.2f), sw, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.44f, h * 0.86f), Offset(w * 0.74f, h * 0.54f), sw, StrokeCap.Round)
    }
}

@Composable
fun RepeatGlyph(tint: Color, size: Dp = 22.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.09f
        drawArc(
            tint, startAngle = 200f, sweepAngle = 200f, useCenter = false,
            topLeft = Offset(w * 0.16f, h * 0.28f), size = Size(w * 0.68f, h * 0.46f),
            style = Stroke(sw, cap = StrokeCap.Round)
        )
        drawArc(
            tint, startAngle = 20f, sweepAngle = 200f, useCenter = false,
            topLeft = Offset(w * 0.16f, h * 0.28f), size = Size(w * 0.68f, h * 0.46f),
            style = Stroke(sw, cap = StrokeCap.Round)
        )
        drawLine(tint, Offset(w * 0.16f, h * 0.3f), Offset(w * 0.1f, h * 0.46f), sw, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.16f, h * 0.3f), Offset(w * 0.3f, h * 0.32f), sw, StrokeCap.Round)
    }
}

@Composable
fun ListGlyph(tint: Color, size: Dp = 22.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.09f
        for (i in 0..2) {
            val y = h * (0.26f + i * 0.24f)
            drawCircle(tint, radius = w * 0.05f, center = Offset(w * 0.16f, y))
            drawLine(tint, Offset(w * 0.32f, y), Offset(w * 0.86f, y), sw, StrokeCap.Round)
        }
    }
}

@Composable
fun PaletteGlyph(tint: Color, size: Dp = 22.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.085f
        drawCircle(tint, radius = w * 0.38f, center = Offset(w / 2, h / 2), style = Stroke(sw))
        drawCircle(tint, radius = w * 0.07f, center = Offset(w * 0.36f, h * 0.34f))
        drawCircle(tint, radius = w * 0.07f, center = Offset(w * 0.64f, h * 0.38f))
        drawCircle(tint, radius = w * 0.07f, center = Offset(w * 0.38f, h * 0.66f))
    }
}

@Composable
fun HeartGlyph(tint: Color, size: Dp = 22.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.085f
        val p = Path().apply {
            moveTo(w * 0.5f, h * 0.82f)
            cubicTo(w * 0.05f, h * 0.5f, w * 0.18f, h * 0.12f, w * 0.5f, h * 0.32f)
            cubicTo(w * 0.82f, h * 0.12f, w * 0.95f, h * 0.5f, w * 0.5f, h * 0.82f)
            close()
        }
        drawPath(p, tint, style = Stroke(sw))
    }
}
