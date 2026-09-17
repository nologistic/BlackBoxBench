package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private fun Modifier.sz(s: Dp) = this.size(s)

@Composable
fun PlusIcon(tint: Color, size: Dp = 26.dp) {
    Canvas(Modifier.sz(size)) {
        val w = this.size.width
        val t = w * 0.09f
        drawLine(tint, Offset(w * 0.5f, w * 0.16f), Offset(w * 0.5f, w * 0.84f), t, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.16f, w * 0.5f), Offset(w * 0.84f, w * 0.5f), t, StrokeCap.Round)
    }
}

@Composable
fun DotsIcon(tint: Color, size: Dp = 26.dp) {
    Canvas(Modifier.sz(size)) {
        val w = this.size.width
        val r = w * 0.09f
        drawCircle(tint, r, Offset(w * 0.5f, w * 0.22f))
        drawCircle(tint, r, Offset(w * 0.5f, w * 0.5f))
        drawCircle(tint, r, Offset(w * 0.5f, w * 0.78f))
    }
}

@Composable
fun FunnelIcon(tint: Color, size: Dp = 26.dp) {
    Canvas(Modifier.sz(size)) {
        val w = this.size.width
        val t = w * 0.08f
        drawLine(tint, Offset(w * 0.16f, w * 0.26f), Offset(w * 0.84f, w * 0.26f), t, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.30f, w * 0.50f), Offset(w * 0.70f, w * 0.50f), t, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.42f, w * 0.74f), Offset(w * 0.58f, w * 0.74f), t, StrokeCap.Round)
    }
}

@Composable
fun BackIcon(tint: Color, size: Dp = 26.dp) {
    Canvas(Modifier.sz(size)) {
        val w = this.size.width
        val t = w * 0.09f
        drawLine(tint, Offset(w * 0.62f, w * 0.22f), Offset(w * 0.34f, w * 0.5f), t, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.34f, w * 0.5f), Offset(w * 0.62f, w * 0.78f), t, StrokeCap.Round)
    }
}

@Composable
fun PencilIcon(tint: Color, size: Dp = 26.dp) {
    Canvas(Modifier.sz(size)) {
        val w = this.size.width
        val t = w * 0.11f
        drawLine(tint, Offset(w * 0.24f, w * 0.76f), Offset(w * 0.74f, w * 0.26f), t, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.66f, w * 0.20f), Offset(w * 0.80f, w * 0.34f), t, StrokeCap.Round)
        val p = Path().apply {
            moveTo(w * 0.16f, w * 0.84f)
            lineTo(w * 0.26f, w * 0.70f)
            lineTo(w * 0.30f, w * 0.80f)
            close()
        }
        drawPath(p, tint)
    }
}

@Composable
fun CheckIcon(tint: Color, size: Dp = 30.dp, stroke: Float = 0.13f) {
    Canvas(Modifier.sz(size)) {
        val w = this.size.width
        val t = w * stroke
        drawLine(tint, Offset(w * 0.16f, w * 0.52f), Offset(w * 0.42f, w * 0.78f), t, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.42f, w * 0.78f), Offset(w * 0.86f, w * 0.22f), t, StrokeCap.Round)
    }
}

@Composable
fun CloseIcon(tint: Color, size: Dp = 30.dp) {
    Canvas(Modifier.sz(size)) {
        val w = this.size.width
        val t = w * 0.11f
        drawLine(tint, Offset(w * 0.22f, w * 0.22f), Offset(w * 0.78f, w * 0.78f), t, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.78f, w * 0.22f), Offset(w * 0.22f, w * 0.78f), t, StrokeCap.Round)
    }
}

@Composable
fun DownArrowIcon(tint: Color, size: Dp = 22.dp) {
    Canvas(Modifier.sz(size)) {
        val w = this.size.width
        val t = w * 0.10f
        val p = Path().apply {
            moveTo(w * 0.22f, w * 0.38f)
            lineTo(w * 0.5f, w * 0.66f)
            lineTo(w * 0.78f, w * 0.38f)
        }
        drawPath(p, tint, style = Stroke(width = t, cap = StrokeCap.Round))
    }
}

@Composable
fun RightArrowIcon(tint: Color, size: Dp = 22.dp) {
    Canvas(Modifier.sz(size)) {
        val w = this.size.width
        val t = w * 0.10f
        val p = Path().apply {
            moveTo(w * 0.38f, w * 0.22f)
            lineTo(w * 0.66f, w * 0.5f)
            lineTo(w * 0.38f, w * 0.78f)
        }
        drawPath(p, tint, style = Stroke(width = t, cap = StrokeCap.Round))
    }
}

@Composable
fun UpArrowIcon(tint: Color, size: Dp = 22.dp) {
    Canvas(Modifier.sz(size)) {
        val w = this.size.width
        val t = w * 0.10f
        drawLine(tint, Offset(w * 0.5f, w * 0.20f), Offset(w * 0.5f, w * 0.82f), t, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.5f, w * 0.20f), Offset(w * 0.26f, w * 0.44f), t, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.5f, w * 0.20f), Offset(w * 0.74f, w * 0.44f), t, StrokeCap.Round)
    }
}

@Composable
fun PaletteIcon(tint: Color, size: Dp = 26.dp) {
    Canvas(Modifier.sz(size)) {
        val w = this.size.width
        drawCircle(tint, w * 0.42f, Offset(w * 0.5f, w * 0.5f), style = Stroke(width = w * 0.10f))
        drawCircle(Color(0xFFE53935), w * 0.07f, Offset(w * 0.28f, w * 0.36f))
        drawCircle(Color(0xFF43A047), w * 0.07f, Offset(w * 0.70f, w * 0.36f))
        drawCircle(Color(0xFF1E88E5), w * 0.07f, Offset(w * 0.5f, w * 0.72f))
    }
}

@Composable
fun StarIcon(tint: Color, size: Dp = 120.dp) {
    Canvas(Modifier.sz(size)) {
        val w = this.size.width
        val cx = w / 2f
        val cy = w * 0.52f
        val outer = w * 0.46f
        val inner = outer * 0.45f
        val path = Path()
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) outer else inner
            val a = (-90 + i * 36) * Math.PI / 180.0
            val x = (cx + r * Math.cos(a)).toFloat()
            val y = (cy + r * Math.sin(a)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path, tint)
    }
}

@Composable
fun BellIcon(tint: Color, size: Dp = 22.dp) {
    Canvas(Modifier.sz(size)) {
        val w = this.size.width
        val p = Path().apply {
            moveTo(w * 0.26f, w * 0.66f)
            lineTo(w * 0.26f, w * 0.46f)
            cubicTo(w * 0.26f, w * 0.24f, w * 0.74f, w * 0.24f, w * 0.74f, w * 0.46f)
            lineTo(w * 0.74f, w * 0.66f)
            lineTo(w * 0.82f, w * 0.74f)
            lineTo(w * 0.18f, w * 0.74f)
            close()
        }
        drawPath(p, tint)
        drawCircle(tint, w * 0.08f, Offset(w * 0.5f, w * 0.82f))
    }
}

@Composable
fun CalendarIcon(tint: Color, size: Dp = 22.dp) {
    Canvas(Modifier.sz(size)) {
        val w = this.size.width
        val t = w * 0.08f
        drawRoundRect(
            tint, Offset(w * 0.14f, w * 0.20f), Size(w * 0.72f, w * 0.68f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.10f),
            style = Stroke(width = t)
        )
        drawLine(tint, Offset(w * 0.14f, w * 0.42f), Offset(w * 0.86f, w * 0.42f), t)
        drawLine(tint, Offset(w * 0.34f, w * 0.10f), Offset(w * 0.34f, w * 0.28f), t, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.66f, w * 0.10f), Offset(w * 0.66f, w * 0.28f), t, StrokeCap.Round)
    }
}
