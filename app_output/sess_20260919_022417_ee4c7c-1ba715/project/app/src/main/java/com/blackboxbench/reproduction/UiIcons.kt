package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private fun stroke(width: Float, cap: StrokeCap = StrokeCap.Round) = Stroke(width = width, cap = cap)

@Composable
fun LogoCheck(modifier: Modifier = Modifier, size: Dp = 180.dp) {
    Canvas(modifier.size(size)) {
        drawCircle(AppColors.Blue)
        val p = Path().apply {
            moveTo(size.toPx() * 0.28f, size.toPx() * 0.52f)
            lineTo(size.toPx() * 0.44f, size.toPx() * 0.68f)
            lineTo(size.toPx() * 0.74f, size.toPx() * 0.34f)
        }
        drawPath(p, Color.White, style = stroke(size.toPx() * 0.075f, StrokeCap.Round))
    }
}

@Composable
fun SortIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(24.dp)) {
        val w = size.width
        drawLine(tint, Offset(w * 0.35f, w * 0.2f), Offset(w * 0.15f, w * 0.42f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.35f, w * 0.2f), Offset(w * 0.55f, w * 0.42f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.35f, w * 0.2f), Offset(w * 0.35f, w * 0.78f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.68f, w * 0.22f), Offset(w * 0.68f, w * 0.8f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.68f, w * 0.8f), Offset(w * 0.48f, w * 0.58f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.68f, w * 0.8f), Offset(w * 0.88f, w * 0.58f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
    }
}

@Composable
fun MicIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(24.dp)) {
        val w = size.width
        drawRoundRect(tint, topLeft = Offset(w * 0.38f, w * 0.1f), size = androidx.compose.ui.geometry.Size(w * 0.24f, w * 0.5f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.12f), style = stroke(w * 0.08f))
        drawArc(tint, startAngle = 20f, sweepAngle = 140f, useCenter = false,
            topLeft = Offset(w * 0.22f, w * 0.28f), size = androidx.compose.ui.geometry.Size(w * 0.56f, w * 0.5f), style = stroke(w * 0.08f))
        drawLine(tint, Offset(w * 0.5f, w * 0.78f), Offset(w * 0.5f, w * 0.92f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
    }
}

@Composable
fun TagIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(24.dp)) {
        val w = size.width
        val p = Path().apply {
            moveTo(w * 0.15f, w * 0.25f)
            lineTo(w * 0.6f, w * 0.25f)
            lineTo(w * 0.88f, w * 0.5f)
            lineTo(w * 0.6f, w * 0.75f)
            lineTo(w * 0.15f, w * 0.75f)
            close()
        }
        drawPath(p, tint, style = stroke(w * 0.09f))
        drawCircle(tint, radius = w * 0.05f, center = Offset(w * 0.3f, w * 0.5f))
    }
}

@Composable
fun SubtaskIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(24.dp)) {
        val w = size.width
        val p = Path().apply {
            moveTo(w * 0.2f, w * 0.15f)
            lineTo(w * 0.2f, w * 0.6f)
            quadraticTo(w * 0.2f, w * 0.8f, w * 0.4f, w * 0.8f)
            lineTo(w * 0.85f, w * 0.8f)
        }
        drawPath(p, tint, style = stroke(w * 0.09f))
        drawLine(tint, Offset(w * 0.7f, w * 0.65f), Offset(w * 0.85f, w * 0.8f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.7f, w * 0.95f), Offset(w * 0.85f, w * 0.8f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
    }
}

@Composable
fun ClipIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(24.dp)) {
        val w = size.width
        val p = Path().apply {
            moveTo(w * 0.65f, w * 0.15f)
            cubicTo(w * 0.82f, w * 0.15f, w * 0.85f, w * 0.35f, w * 0.65f, w * 0.5f)
            lineTo(w * 0.4f, w * 0.78f)
            cubicTo(w * 0.3f, w * 0.88f, w * 0.15f, w * 0.8f, w * 0.22f, w * 0.68f)
            lineTo(w * 0.55f, w * 0.32f)
        }
        drawPath(p, tint, style = stroke(w * 0.08f))
    }
}

@Composable
fun TimerIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(24.dp)) {
        val w = size.width
        drawCircle(tint, radius = w * 0.34f, center = Offset(w * 0.5f, w * 0.56f), style = stroke(w * 0.08f))
        drawLine(tint, Offset(w * 0.5f, w * 0.56f), Offset(w * 0.5f, w * 0.34f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.5f, w * 0.56f), Offset(w * 0.64f, w * 0.62f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.42f, w * 0.08f), Offset(w * 0.58f, w * 0.08f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
    }
}

@Composable
fun PauseIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(24.dp)) {
        val w = size.width
        drawLine(tint, Offset(w * 0.36f, w * 0.2f), Offset(w * 0.36f, w * 0.8f), strokeWidth = w * 0.12f, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.64f, w * 0.2f), Offset(w * 0.64f, w * 0.8f), strokeWidth = w * 0.12f, cap = StrokeCap.Round)
    }
}

@Composable
fun SaveIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(24.dp)) {
        val w = size.width
        val p = Path().apply {
            moveTo(w * 0.2f, w * 0.12f); lineTo(w * 0.68f, w * 0.12f)
            lineTo(w * 0.84f, w * 0.28f); lineTo(w * 0.84f, w * 0.88f); close()
        }
        drawPath(p, tint, style = stroke(w * 0.08f))
        drawRect(tint, topLeft = Offset(w * 0.36f, w * 0.52f), size = androidx.compose.ui.geometry.Size(w * 0.32f, w * 0.28f), style = stroke(w * 0.07f))
    }
}

@Composable
fun FilterIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(24.dp)) {
        val w = size.width
        drawLine(tint, Offset(w * 0.15f, w * 0.3f), Offset(w * 0.7f, w * 0.3f), strokeWidth = w * 0.1f, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.15f, w * 0.5f), Offset(w * 0.55f, w * 0.5f), strokeWidth = w * 0.1f, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.15f, w * 0.7f), Offset(w * 0.4f, w * 0.7f), strokeWidth = w * 0.1f, cap = StrokeCap.Round)
    }
}

@Composable
fun CloudOffIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(24.dp)) {
        val w = size.width
        val p = Path().apply {
            moveTo(w * 0.3f, w * 0.68f)
            cubicTo(w * 0.12f, w * 0.68f, w * 0.12f, w * 0.45f, w * 0.3f, w * 0.42f)
            cubicTo(w * 0.34f, w * 0.28f, w * 0.52f, w * 0.24f, w * 0.62f, w * 0.34f)
            cubicTo(w * 0.82f, w * 0.36f, w * 0.86f, w * 0.56f, w * 0.72f, w * 0.68f)
            close()
        }
        drawPath(p, tint, style = stroke(w * 0.08f))
        drawLine(tint, Offset(w * 0.18f, w * 0.18f), Offset(w * 0.84f, w * 0.84f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
    }
}

@Composable
fun FlagIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(24.dp)) {
        val w = size.width
        drawLine(tint, Offset(w * 0.3f, w * 0.12f), Offset(w * 0.3f, w * 0.9f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
        val p = Path().apply {
            moveTo(w * 0.3f, w * 0.16f); lineTo(w * 0.78f, w * 0.16f)
            lineTo(w * 0.66f, w * 0.3f); lineTo(w * 0.78f, w * 0.44f); lineTo(w * 0.3f, w * 0.44f); close()
        }
        drawPath(p, tint, style = stroke(w * 0.07f))
    }
}

@Composable
fun BellOffIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(24.dp)) {
        val w = size.width
        val p = Path().apply {
            moveTo(w * 0.5f, w * 0.18f)
            cubicTo(w * 0.3f, w * 0.18f, w * 0.28f, w * 0.4f, w * 0.28f, w * 0.55f)
            lineTo(w * 0.28f, w * 0.66f); lineTo(w * 0.2f, w * 0.74f); lineTo(w * 0.8f, w * 0.74f)
            lineTo(w * 0.72f, w * 0.66f); lineTo(w * 0.72f, w * 0.55f)
            cubicTo(w * 0.72f, w * 0.4f, w * 0.7f, w * 0.18f, w * 0.5f, w * 0.18f)
            close()
        }
        drawPath(p, tint, style = stroke(w * 0.06f))
        drawArc(tint, 0f, 180f, true, topLeft = Offset(w * 0.42f, w * 0.74f),
            size = androidx.compose.ui.geometry.Size(w * 0.16f, w * 0.12f), style = stroke(w * 0.06f))
        drawLine(tint, Offset(w * 0.16f, w * 0.16f), Offset(w * 0.86f, w * 0.86f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
    }
}

@Composable
fun HistoryIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(24.dp)) {
        val w = size.width
        drawArc(tint, 40f, 290f, false, topLeft = Offset(w * 0.14f, w * 0.14f),
            size = androidx.compose.ui.geometry.Size(w * 0.72f, w * 0.72f), style = stroke(w * 0.08f))
        drawLine(tint, Offset(w * 0.2f, w * 0.12f), Offset(w * 0.12f, w * 0.3f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.5f, w * 0.32f), Offset(w * 0.5f, w * 0.52f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.5f, w * 0.52f), Offset(w * 0.66f, w * 0.6f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
    }
}

@Composable
fun AlarmIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(24.dp)) {
        val w = size.width
        drawCircle(tint, radius = w * 0.3f, center = Offset(w * 0.5f, w * 0.56f), style = stroke(w * 0.08f))
        drawLine(tint, Offset(w * 0.5f, w * 0.56f), Offset(w * 0.5f, w * 0.4f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.5f, w * 0.56f), Offset(w * 0.62f, w * 0.62f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.26f, w * 0.16f), Offset(w * 0.16f, w * 0.28f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.74f, w * 0.16f), Offset(w * 0.84f, w * 0.28f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
    }
}

@Composable
fun TargetIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(24.dp)) {
        val w = size.width
        drawCircle(tint, radius = w * 0.32f, center = Offset(w / 2, w / 2), style = stroke(w * 0.08f))
        drawCircle(tint, radius = w * 0.09f, center = Offset(w / 2, w / 2))
        drawLine(tint, Offset(w / 2, w * 0.04f), Offset(w / 2, w * 0.2f), strokeWidth = w * 0.08f)
        drawLine(tint, Offset(w / 2, w * 0.8f), Offset(w / 2, w * 0.96f), strokeWidth = w * 0.08f)
        drawLine(tint, Offset(w * 0.04f, w / 2), Offset(w * 0.2f, w / 2), strokeWidth = w * 0.08f)
        drawLine(tint, Offset(w * 0.8f, w / 2), Offset(w * 0.96f, w / 2), strokeWidth = w * 0.08f)
    }
}

@Composable
fun HomeIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(24.dp)) {
        val w = size.width
        val p = Path().apply {
            moveTo(w * 0.5f, w * 0.14f); lineTo(w * 0.88f, w * 0.5f)
            lineTo(w * 0.76f, w * 0.5f); lineTo(w * 0.76f, w * 0.86f)
            lineTo(w * 0.24f, w * 0.86f); lineTo(w * 0.24f, w * 0.5f)
            lineTo(w * 0.12f, w * 0.5f); close()
        }
        drawPath(p, tint, style = stroke(w * 0.07f))
    }
}

@Composable
fun GridIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(24.dp)) {
        val w = size.width
        val s = w * 0.28f; val g = w * 0.12f
        for (r in 0..1) for (c in 0..1) {
            drawRect(tint, topLeft = Offset(g + c * (s + g), g + r * (s + g)),
                size = androidx.compose.ui.geometry.Size(s, s), style = stroke(w * 0.06f))
        }
    }
}
