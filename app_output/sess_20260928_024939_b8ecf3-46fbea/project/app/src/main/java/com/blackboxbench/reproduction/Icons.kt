package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun IconBack(tint: Color, dim: Dp = 22.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(dim)) {
        val w = size.width
        val h = size.height
        val p = Path().apply {
            moveTo(w * 0.64f, h * 0.10f)
            lineTo(w * 0.26f, h * 0.5f)
            lineTo(w * 0.64f, h * 0.90f)
        }
        drawPath(p, tint, style = Stroke(width = w * 0.085f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun IconClock(tint: Color, dim: Dp = 16.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(dim)) {
        val w = size.width
        val r = w * 0.40f
        drawCircle(tint, radius = r, center = Offset(w / 2, w / 2), style = Stroke(width = w * 0.09f))
        drawLine(tint, Offset(w / 2, w / 2 - r * 0.55f), Offset(w / 2, w / 2), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
        drawLine(tint, Offset(w / 2, w / 2), Offset(w / 2 + r * 0.5f, w / 2), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
    }
}

@Composable
fun IconMoves(tint: Color, dim: Dp = 17.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(dim)) {
        val w = size.width
        val h = size.height
        drawCircle(tint, radius = w * 0.15f, center = Offset(w * 0.34f, h * 0.24f), style = Stroke(width = w * 0.09f))
        drawCircle(tint, radius = w * 0.12f, center = Offset(w * 0.72f, h * 0.30f), style = Stroke(width = w * 0.08f))
        val body = Path().apply {
            moveTo(w * 0.10f, h * 0.86f)
            cubicTo(w * 0.12f, h * 0.52f, w * 0.56f, h * 0.52f, w * 0.58f, h * 0.86f)
        }
        drawPath(body, tint, style = Stroke(width = w * 0.09f, cap = StrokeCap.Round))
        val body2 = Path().apply {
            moveTo(w * 0.56f, h * 0.86f)
            cubicTo(w * 0.58f, h * 0.60f, w * 0.92f, h * 0.60f, w * 0.94f, h * 0.86f)
        }
        drawPath(body2, tint, style = Stroke(width = w * 0.08f, cap = StrokeCap.Round))
    }
}

@Composable
fun IconStar(tint: Color, dim: Dp = 26.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(dim)) {
        val w = size.width
        val h = size.height
        val cx = w / 2
        val cy = h / 2 + h * 0.04f
        val outer = w * 0.46f
        val inner = outer * 0.40f
        val p = Path()
        for (i in 0 until 10) {
            val angle = (-90.0 + i * 36.0) * Math.PI / 180.0
            val rad = if (i % 2 == 0) outer else inner
            val x = cx + (rad * Math.cos(angle)).toFloat()
            val y = cy + (rad * Math.sin(angle)).toFloat()
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        p.close()
        drawPath(p, tint)
    }
}

@Composable
fun IconHeart(tint: Color, dim: Dp = 26.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(dim)) {
        val w = size.width
        val h = size.height
        val p = Path().apply {
            moveTo(w * 0.5f, h * 0.86f)
            cubicTo(w * 0.06f, h * 0.56f, w * 0.10f, h * 0.10f, w * 0.5f, h * 0.30f)
            cubicTo(w * 0.90f, h * 0.10f, w * 0.94f, h * 0.56f, w * 0.5f, h * 0.86f)
        }
        p.close()
        drawPath(p, tint)
    }
}

@Composable
fun IconLock(tint: Color, dim: Dp = 22.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(dim)) {
        val w = size.width
        val h = size.height
        val shackle = Path().apply {
            moveTo(w * 0.30f, h * 0.52f)
            lineTo(w * 0.30f, h * 0.40f)
            cubicTo(w * 0.30f, h * 0.12f, w * 0.70f, h * 0.12f, w * 0.70f, h * 0.40f)
            lineTo(w * 0.70f, h * 0.52f)
        }
        drawPath(shackle, tint, style = Stroke(width = w * 0.10f, cap = StrokeCap.Round))
        drawRoundRect(
            tint,
            topLeft = Offset(w * 0.18f, h * 0.48f),
            size = Size(w * 0.64f, h * 0.42f),
            cornerRadius = CornerRadius(w * 0.10f)
        )
    }
}

@Composable
fun IconUndo(tint: Color, dim: Dp = 18.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(dim)) {
        val w = size.width
        val h = size.height
        val arc = Path().apply {
            moveTo(w * 0.82f, h * 0.72f)
            cubicTo(w * 0.86f, h * 0.30f, w * 0.30f, h * 0.16f, w * 0.22f, h * 0.42f)
        }
        drawPath(arc, tint, style = Stroke(width = w * 0.10f, cap = StrokeCap.Round))
        val head = Path().apply {
            moveTo(w * 0.10f, h * 0.28f)
            lineTo(w * 0.22f, h * 0.46f)
            lineTo(w * 0.40f, h * 0.32f)
        }
        drawPath(head, tint, style = Stroke(width = w * 0.10f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun IconRestart(tint: Color, dim: Dp = 18.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(dim)) {
        val w = size.width
        val h = size.height
        drawArc(
            color = tint,
            startAngle = -60f,
            sweepAngle = 300f,
            useCenter = false,
            topLeft = Offset(w * 0.14f, h * 0.14f),
            size = Size(w * 0.72f, h * 0.72f),
            style = Stroke(width = w * 0.11f, cap = StrokeCap.Round)
        )
        val head = Path().apply {
            moveTo(w * 0.62f, h * 0.02f)
            lineTo(w * 0.90f, h * 0.20f)
            lineTo(w * 0.62f, h * 0.34f)
        }
        drawPath(head, tint, style = Stroke(width = w * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun IconBulb(tint: Color, dim: Dp = 18.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(dim)) {
        val w = size.width
        val h = size.height
        drawCircle(tint, radius = w * 0.28f, center = Offset(w * 0.5f, h * 0.38f), style = Stroke(width = w * 0.10f))
        drawLine(tint, Offset(w * 0.38f, h * 0.66f), Offset(w * 0.62f, h * 0.66f), strokeWidth = w * 0.10f, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.42f, h * 0.80f), Offset(w * 0.58f, h * 0.80f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
    }
}

@Composable
fun IconWarning(dim: Dp = 44.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(dim)) {
        val w = size.width
        val h = size.height
        val p = Path().apply {
            moveTo(w * 0.5f, h * 0.08f)
            lineTo(w * 0.96f, h * 0.88f)
            lineTo(w * 0.04f, h * 0.88f)
            close()
        }
        drawPath(p, Color(0xFFE5484D), style = Stroke(width = w * 0.06f, join = StrokeJoin.Round))
        drawLine(Color(0xFFE5484D), Offset(w * 0.5f, h * 0.36f), Offset(w * 0.5f, h * 0.62f), strokeWidth = w * 0.06f, cap = StrokeCap.Round)
        drawCircle(Color(0xFFE5484D), radius = w * 0.035f, center = Offset(w * 0.5f, h * 0.74f))
    }
}

@Composable
fun IconRefresh(tint: Color, dim: Dp = 20.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(dim)) {
        val w = size.width
        val h = size.height
        drawArc(
            color = tint,
            startAngle = -70f,
            sweepAngle = 250f,
            useCenter = false,
            topLeft = Offset(w * 0.16f, h * 0.16f),
            size = Size(w * 0.68f, h * 0.68f),
            style = Stroke(width = w * 0.10f, cap = StrokeCap.Round)
        )
        val head = Path().apply {
            moveTo(w * 0.40f, h * 0.02f)
            lineTo(w * 0.68f, h * 0.16f)
            lineTo(w * 0.44f, h * 0.34f)
        }
        drawPath(head, tint, style = Stroke(width = w * 0.10f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun IconCopy(tint: Color, dim: Dp = 18.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(dim)) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            tint,
            topLeft = Offset(w * 0.06f, h * 0.06f),
            size = Size(w * 0.60f, h * 0.60f),
            cornerRadius = CornerRadius(w * 0.10f),
            style = Stroke(width = w * 0.09f)
        )
        drawRoundRect(
            Color(0xFF101114),
            topLeft = Offset(w * 0.30f, h * 0.30f),
            size = Size(w * 0.62f, h * 0.62f),
            cornerRadius = CornerRadius(w * 0.10f)
        )
        drawRoundRect(
            tint,
            topLeft = Offset(w * 0.30f, h * 0.30f),
            size = Size(w * 0.62f, h * 0.62f),
            cornerRadius = CornerRadius(w * 0.10f),
            style = Stroke(width = w * 0.09f)
        )
    }
}

@Composable
fun IconPaste(tint: Color, dim: Dp = 20.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(dim)) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            tint,
            topLeft = Offset(w * 0.14f, h * 0.16f),
            size = Size(w * 0.72f, h * 0.78f),
            cornerRadius = CornerRadius(w * 0.12f),
            style = Stroke(width = w * 0.09f)
        )
        drawRoundRect(
            tint,
            topLeft = Offset(w * 0.32f, h * 0.04f),
            size = Size(w * 0.36f, h * 0.20f),
            cornerRadius = CornerRadius(w * 0.06f)
        )
    }
}

@Composable
fun IconCoffee(tint: Color, dim: Dp = 18.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(dim)) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            tint,
            topLeft = Offset(w * 0.12f, h * 0.34f),
            size = Size(w * 0.60f, h * 0.46f),
            cornerRadius = CornerRadius(w * 0.10f),
            style = Stroke(width = w * 0.09f)
        )
        drawArc(
            color = tint,
            startAngle = -80f,
            sweepAngle = 160f,
            useCenter = false,
            topLeft = Offset(w * 0.66f, h * 0.40f),
            size = Size(w * 0.34f, h * 0.28f),
            style = Stroke(width = w * 0.09f, cap = StrokeCap.Round)
        )
        drawLine(tint, Offset(w * 0.08f, h * 0.88f), Offset(w * 0.78f, h * 0.88f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
    }
}

@Composable
fun IconHome(tint: Color, dim: Dp = 18.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(dim)) {
        val w = size.width
        val h = size.height
        val roof = Path().apply {
            moveTo(w * 0.08f, h * 0.48f)
            lineTo(w * 0.5f, h * 0.12f)
            lineTo(w * 0.92f, h * 0.48f)
        }
        drawPath(roof, tint, style = Stroke(width = w * 0.10f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        val body = Path().apply {
            moveTo(w * 0.20f, h * 0.46f)
            lineTo(w * 0.20f, h * 0.88f)
            lineTo(w * 0.80f, h * 0.88f)
            lineTo(w * 0.80f, h * 0.46f)
        }
        drawPath(body, tint, style = Stroke(width = w * 0.10f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun IconCheck(tint: Color, dim: Dp = 16.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(dim)) {
        val w = size.width
        val h = size.height
        val p = Path().apply {
            moveTo(w * 0.16f, h * 0.54f)
            lineTo(w * 0.42f, h * 0.80f)
            lineTo(w * 0.86f, h * 0.20f)
        }
        drawPath(p, tint, style = Stroke(width = w * 0.12f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun IconDashedCircle(tint: Color, dim: Dp = 20.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(dim)) {
        val w = size.width
        drawCircle(
            tint,
            radius = w * 0.36f,
            center = Offset(w / 2, w / 2),
            style = Stroke(
                width = w * 0.07f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(w * 0.10f, w * 0.10f))
            )
        )
    }
}

/** Draws an "X" cross inside the current cell. */
fun DrawScope.drawCross(color: Color, fraction: Float = 0.30f) {
    val w = size.width
    val h = size.height
    drawLine(
        color,
        Offset(w * fraction, h * fraction),
        Offset(w * (1 - fraction), h * (1 - fraction)),
        strokeWidth = w * 0.11f,
        cap = StrokeCap.Round
    )
    drawLine(
        color,
        Offset(w * (1 - fraction), h * fraction),
        Offset(w * fraction, h * (1 - fraction)),
        strokeWidth = w * 0.11f,
        cap = StrokeCap.Round
    )
}
