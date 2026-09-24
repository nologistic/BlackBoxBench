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
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Every glyph used by the app is drawn with primitives so the reproduction stays
 * independent of any icon pack.
 */
private fun DrawScope.stroke(color: Color, width: Float) =
    Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round)

@Composable
private fun Glyph(
    size: Dp,
    modifier: Modifier = Modifier,
    draw: DrawScope.(Float) -> Unit
) {
    Canvas(modifier = modifier.size(size)) { draw(this.size.width) }
}

@Composable
fun IconBackChevron(size: Dp = 26.dp, color: Color = Palette.TextPrimary, modifier: Modifier = Modifier) =
    Glyph(size, modifier) { s ->
        val path = Path().apply {
            moveTo(s * 0.68f, s * 0.14f)
            lineTo(s * 0.30f, s * 0.50f)
            lineTo(s * 0.68f, s * 0.86f)
        }
        drawPath(path, color, style = stroke(color, s * 0.11f))
    }

@Composable
fun IconClock(size: Dp = 18.dp, color: Color = Palette.TextSecondary, modifier: Modifier = Modifier) =
    Glyph(size, modifier) { s ->
        drawCircle(color, radius = s * 0.40f, center = Offset(s / 2, s / 2), style = stroke(color, s * 0.09f))
        drawLine(color, Offset(s / 2, s * 0.28f), Offset(s / 2, s * 0.52f), strokeWidth = s * 0.09f, cap = StrokeCap.Round)
        drawLine(color, Offset(s / 2, s * 0.52f), Offset(s * 0.70f, s * 0.60f), strokeWidth = s * 0.09f, cap = StrokeCap.Round)
    }

@Composable
fun IconMoves(size: Dp = 18.dp, color: Color = Palette.TextSecondary, modifier: Modifier = Modifier) =
    Glyph(size, modifier) { s ->
        val w = s * 0.09f
        drawLine(color, Offset(s * 0.22f, s * 0.34f), Offset(s * 0.78f, s * 0.34f), strokeWidth = w, cap = StrokeCap.Round)
        val up = Path().apply {
            moveTo(s * 0.62f, s * 0.16f); lineTo(s * 0.82f, s * 0.34f); lineTo(s * 0.62f, s * 0.52f)
        }
        drawPath(up, color, style = stroke(color, w))
        drawLine(color, Offset(s * 0.78f, s * 0.68f), Offset(s * 0.22f, s * 0.68f), strokeWidth = w, cap = StrokeCap.Round)
        val down = Path().apply {
            moveTo(s * 0.38f, s * 0.50f); lineTo(s * 0.18f, s * 0.68f); lineTo(s * 0.38f, s * 0.86f)
        }
        drawPath(down, color, style = stroke(color, w))
    }

@Composable
fun IconUndo(size: Dp = 18.dp, color: Color = Palette.TextPrimary, modifier: Modifier = Modifier) =
    Glyph(size, modifier) { s ->
        val w = s * 0.10f
        val path = Path().apply {
            moveTo(s * 0.30f, s * 0.30f)
            lineTo(s * 0.14f, s * 0.52f)
            lineTo(s * 0.36f, s * 0.62f)
        }
        drawPath(path, color, style = stroke(color, w))
        drawArc(
            color = color,
            startAngle = 170f,
            sweepAngle = -190f,
            useCenter = false,
            topLeft = Offset(s * 0.22f, s * 0.30f),
            size = Size(s * 0.60f, s * 0.52f),
            style = stroke(color, w)
        )
    }

@Composable
fun IconRestart(size: Dp = 18.dp, color: Color = Palette.TextPrimary, modifier: Modifier = Modifier) =
    Glyph(size, modifier) { s ->
        val w = s * 0.10f
        drawArc(
            color = color,
            startAngle = -60f,
            sweepAngle = 300f,
            useCenter = false,
            topLeft = Offset(s * 0.16f, s * 0.16f),
            size = Size(s * 0.68f, s * 0.68f),
            style = stroke(color, w)
        )
        val head = Path().apply {
            moveTo(s * 0.50f, s * 0.02f); lineTo(s * 0.84f, s * 0.16f); lineTo(s * 0.50f, s * 0.32f)
        }
        drawPath(head, color, style = stroke(color, w))
    }

@Composable
fun IconBulb(size: Dp = 18.dp, color: Color = Palette.Amber, modifier: Modifier = Modifier) =
    Glyph(size, modifier) { s ->
        val w = s * 0.09f
        drawCircle(color, radius = s * 0.26f, center = Offset(s / 2, s * 0.38f), style = stroke(color, w))
        drawLine(color, Offset(s * 0.40f, s * 0.62f), Offset(s * 0.60f, s * 0.62f), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(color, Offset(s * 0.42f, s * 0.76f), Offset(s * 0.58f, s * 0.76f), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(color, Offset(s * 0.44f, s * 0.90f), Offset(s * 0.56f, s * 0.90f), strokeWidth = w, cap = StrokeCap.Round)
    }

@Composable
fun IconGridGlyph(size: Dp = 26.dp, color: Color = Palette.TextPrimary, modifier: Modifier = Modifier) =
    Glyph(size, modifier) { s ->
        val w = s * 0.08f
        val box = s * 0.30f
        val gap = s * 0.10f
        val start = (s - box * 2 - gap) / 2
        for (r in 0..1) for (c in 0..1) {
            drawRoundRect(
                color = color,
                topLeft = Offset(start + c * (box + gap), start + r * (box + gap)),
                size = Size(box, box),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.04f),
                style = stroke(color, w)
            )
        }
    }

@Composable
fun IconLinesGlyph(size: Dp = 26.dp, color: Color = Palette.TextPrimary, modifier: Modifier = Modifier) =
    Glyph(size, modifier) { s ->
        val w = s * 0.09f
        val xs = listOf(0.24f to 0.30f, 0.42f to 0.50f, 0.30f to 0.72f)
        xs.forEach { (a, y) ->
            drawLine(color, Offset(s * 0.20f, s * y), Offset(s * 0.34f, s * y), strokeWidth = w, cap = StrokeCap.Round)
            drawLine(color, Offset(s * a, s * y), Offset(s * 0.82f, s * y), strokeWidth = w, cap = StrokeCap.Round)
        }
    }

@Composable
fun IconCrossGlyph(size: Dp = 26.dp, color: Color = Palette.Red, modifier: Modifier = Modifier) =
    Glyph(size, modifier) { s ->
        val w = s * 0.11f
        drawLine(color, Offset(s * 0.22f, s * 0.22f), Offset(s * 0.78f, s * 0.78f), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(color, Offset(s * 0.78f, s * 0.22f), Offset(s * 0.22f, s * 0.78f), strokeWidth = w, cap = StrokeCap.Round)
    }

@Composable
fun IconTrophy(size: Dp = 26.dp, color: Color = Palette.TextPrimary, modifier: Modifier = Modifier) =
    Glyph(size, modifier) { s ->
        val w = s * 0.08f
        val cup = Path().apply {
            moveTo(s * 0.32f, s * 0.20f)
            lineTo(s * 0.68f, s * 0.20f)
            lineTo(s * 0.62f, s * 0.56f)
            lineTo(s * 0.38f, s * 0.56f)
            close()
        }
        drawPath(cup, color, style = stroke(color, w))
        drawLine(color, Offset(s * 0.50f, s * 0.56f), Offset(s * 0.50f, s * 0.74f), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(color, Offset(s * 0.32f, s * 0.80f), Offset(s * 0.68f, s * 0.80f), strokeWidth = w, cap = StrokeCap.Round)
    }

@Composable
fun IconCup(size: Dp = 20.dp, color: Color = Palette.TextPrimary, modifier: Modifier = Modifier) =
    Glyph(size, modifier) { s ->
        val w = s * 0.09f
        drawRoundRect(
            color = color,
            topLeft = Offset(s * 0.18f, s * 0.34f),
            size = Size(s * 0.52f, s * 0.40f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.08f),
            style = stroke(color, w)
        )
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(s * 0.62f, s * 0.40f),
            size = Size(s * 0.26f, s * 0.24f),
            style = stroke(color, w)
        )
        drawLine(color, Offset(s * 0.14f, s * 0.84f), Offset(s * 0.78f, s * 0.84f), strokeWidth = w, cap = StrokeCap.Round)
    }

@Composable
fun IconHome(size: Dp = 20.dp, color: Color = Palette.TextPrimary, modifier: Modifier = Modifier) =
    Glyph(size, modifier) { s ->
        val w = s * 0.09f
        val roof = Path().apply {
            moveTo(s * 0.12f, s * 0.48f)
            lineTo(s * 0.50f, s * 0.14f)
            lineTo(s * 0.88f, s * 0.48f)
        }
        drawPath(roof, color, style = stroke(color, w))
        val body = Path().apply {
            moveTo(s * 0.22f, s * 0.46f)
            lineTo(s * 0.22f, s * 0.86f)
            lineTo(s * 0.78f, s * 0.86f)
            lineTo(s * 0.78f, s * 0.46f)
        }
        drawPath(body, color, style = stroke(color, w))
        drawLine(color, Offset(s * 0.50f, s * 0.62f), Offset(s * 0.50f, s * 0.86f), strokeWidth = w)
    }

@Composable
fun IconCopy(size: Dp = 20.dp, color: Color = Palette.TextPrimary, modifier: Modifier = Modifier) =
    Glyph(size, modifier) { s ->
        val w = s * 0.08f
        drawRoundRect(
            color = color,
            topLeft = Offset(s * 0.30f, s * 0.14f),
            size = Size(s * 0.50f, s * 0.58f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.06f),
            style = stroke(color, w)
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(s * 0.18f, s * 0.30f),
            size = Size(s * 0.50f, s * 0.58f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.06f),
            style = stroke(color, w)
        )
    }

@Composable
fun IconClipboard(size: Dp = 20.dp, color: Color = Palette.TextSecondary, modifier: Modifier = Modifier) =
    Glyph(size, modifier) { s ->
        val w = s * 0.08f
        drawRoundRect(
            color = color,
            topLeft = Offset(s * 0.20f, s * 0.18f),
            size = Size(s * 0.60f, s * 0.68f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.06f),
            style = stroke(color, w)
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(s * 0.38f, s * 0.08f),
            size = Size(s * 0.24f, s * 0.16f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.04f),
            style = stroke(color, w)
        )
    }

@Composable
fun IconLock(size: Dp = 22.dp, color: Color = Palette.TextSecondary, modifier: Modifier = Modifier) =
    Glyph(size, modifier) { s ->
        val w = s * 0.09f
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(s * 0.30f, s * 0.16f),
            size = Size(s * 0.40f, s * 0.40f),
            style = stroke(color, w)
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(s * 0.22f, s * 0.44f),
            size = Size(s * 0.56f, s * 0.40f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.08f),
            style = stroke(color, w)
        )
    }

@Composable
fun IconCheck(size: Dp = 18.dp, color: Color = Palette.TextPrimary, modifier: Modifier = Modifier) =
    Glyph(size, modifier) { s ->
        val w = s * 0.13f
        val path = Path().apply {
            moveTo(s * 0.20f, s * 0.54f)
            lineTo(s * 0.42f, s * 0.76f)
            lineTo(s * 0.80f, s * 0.26f)
        }
        drawPath(path, color, style = stroke(color, w))
    }

@Composable
fun IconStar(size: Dp = 22.dp, color: Color = Palette.Amber, modifier: Modifier = Modifier) =
    Glyph(size, modifier) { s ->
        val cx = s / 2
        val cy = s * 0.52f
        val outer = s * 0.42f
        val inner = outer * 0.44f
        val path = Path()
        for (i in 0 until 10) {
            val radius = if (i % 2 == 0) outer else inner
            val angle = (-90.0 + i * 36.0) * Math.PI / 180.0
            val x = (cx + radius * Math.cos(angle)).toFloat()
            val y = (cy + radius * Math.sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path, color)
    }

@Composable
fun IconHeart(size: Dp = 22.dp, color: Color = Palette.Red, modifier: Modifier = Modifier) =
    Glyph(size, modifier) { s ->
        val path = Path().apply {
            moveTo(s * 0.50f, s * 0.86f)
            cubicTo(s * 0.06f, s * 0.56f, s * 0.14f, s * 0.14f, s * 0.50f, s * 0.34f)
            cubicTo(s * 0.86f, s * 0.14f, s * 0.94f, s * 0.56f, s * 0.50f, s * 0.86f)
            close()
        }
        drawPath(path, color)
    }

@Composable
fun IconFilledSquare(size: Dp = 18.dp, color: Color = Palette.Black, modifier: Modifier = Modifier) =
    Glyph(size, modifier) { s ->
        drawRoundRect(
            color = color,
            topLeft = Offset(s * 0.10f, s * 0.10f),
            size = Size(s * 0.80f, s * 0.80f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.10f)
        )
    }

@Composable
fun IconWarning(size: Dp = 40.dp, color: Color = Palette.Red, modifier: Modifier = Modifier) =
    Glyph(size, modifier) { s ->
        val w = s * 0.075f
        val path = Path().apply {
            moveTo(s * 0.50f, s * 0.12f)
            lineTo(s * 0.94f, s * 0.86f)
            lineTo(s * 0.06f, s * 0.86f)
            close()
        }
        drawPath(path, color, style = stroke(color, w))
        drawLine(color, Offset(s * 0.50f, s * 0.40f), Offset(s * 0.50f, s * 0.62f), strokeWidth = w, cap = StrokeCap.Round)
        drawCircle(color, radius = w * 0.62f, center = Offset(s * 0.50f, s * 0.74f))
    }

@Composable
fun IconWarningBadge(size: Dp = 52.dp, color: Color = Palette.Red, modifier: Modifier = Modifier) =
    Glyph(size, modifier) { s ->
        val w = s * 0.055f
        val path = Path().apply {
            moveTo(s * 0.50f, s * 0.14f)
            lineTo(s * 0.92f, s * 0.84f)
            lineTo(s * 0.08f, s * 0.84f)
            close()
        }
        drawPath(path, color, style = stroke(color, w))
        drawLine(color, Offset(s * 0.50f, s * 0.40f), Offset(s * 0.50f, s * 0.62f), strokeWidth = w, cap = StrokeCap.Round)
        drawCircle(color, radius = w * 0.6f, center = Offset(s * 0.50f, s * 0.73f))
    }
