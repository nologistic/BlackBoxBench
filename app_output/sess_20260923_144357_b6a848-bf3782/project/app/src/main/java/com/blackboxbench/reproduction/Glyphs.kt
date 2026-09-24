package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
private fun Glyph(size: Dp, tint: Color, modifier: Modifier = Modifier, draw: DrawScope.(Float, Color) -> Unit) {
    Canvas(modifier = modifier.size(size)) { draw(this.size.minDimension, tint) }
}

@Composable
fun VinylIcon(size: Dp = 24.dp, tint: Color = Color(0xFF9E9E9E), modifier: Modifier = Modifier) {
    Glyph(size, tint, modifier) { s, c ->
        drawCircle(c.copy(alpha = 0.35f), radius = s * 0.48f, center = Offset(s / 2, s / 2))
        drawCircle(c.copy(alpha = 0.55f), radius = s * 0.40f, center = Offset(s / 2, s / 2), style = Stroke(width = s * 0.04f))
        drawCircle(c.copy(alpha = 0.55f), radius = s * 0.30f, center = Offset(s / 2, s / 2), style = Stroke(width = s * 0.04f))
        drawCircle(c.copy(alpha = 0.9f), radius = s * 0.12f, center = Offset(s / 2, s / 2))
        drawCircle(c.copy(alpha = 0.3f), radius = s * 0.04f, center = Offset(s / 2, s / 2))
    }
}

@Composable
fun PlayGlyph(size: Dp = 24.dp, tint: Color = Color.White, modifier: Modifier = Modifier) {
    Glyph(size, tint, modifier) { s, c ->
        val path = Path().apply {
            moveTo(s * 0.28f, s * 0.18f)
            lineTo(s * 0.80f, s * 0.50f)
            lineTo(s * 0.28f, s * 0.82f)
            close()
        }
        drawPath(path, c)
    }
}

@Composable
fun PauseGlyph(size: Dp = 24.dp, tint: Color = Color.White, modifier: Modifier = Modifier) {
    Glyph(size, tint, modifier) { s, c ->
        val w = s * 0.20f
        drawRoundRect(c, topLeft = Offset(s * 0.24f, s * 0.16f), size = Size(w, s * 0.68f), cornerRadius = CornerRadius(w * 0.25f))
        drawRoundRect(c, topLeft = Offset(s * 0.56f, s * 0.16f), size = Size(w, s * 0.68f), cornerRadius = CornerRadius(w * 0.25f))
    }
}

@Composable
fun SkipNextGlyph(size: Dp = 24.dp, tint: Color = Color.White, modifier: Modifier = Modifier) {
    Glyph(size, tint, modifier) { s, c ->
        val path = Path().apply {
            moveTo(s * 0.22f, s * 0.18f)
            lineTo(s * 0.68f, s * 0.50f)
            lineTo(s * 0.22f, s * 0.82f)
            close()
        }
        drawPath(path, c)
        drawRoundRect(c, topLeft = Offset(s * 0.72f, s * 0.18f), size = Size(s * 0.09f, s * 0.64f), cornerRadius = CornerRadius(s * 0.02f))
    }
}

@Composable
fun SkipPrevGlyph(size: Dp = 24.dp, tint: Color = Color.White, modifier: Modifier = Modifier) {
    Glyph(size, tint, modifier) { s, c ->
        val path = Path().apply {
            moveTo(s * 0.78f, s * 0.18f)
            lineTo(s * 0.32f, s * 0.50f)
            lineTo(s * 0.78f, s * 0.82f)
            close()
        }
        drawPath(path, c)
        drawRoundRect(c, topLeft = Offset(s * 0.19f, s * 0.18f), size = Size(s * 0.09f, s * 0.64f), cornerRadius = CornerRadius(s * 0.02f))
    }
}

@Composable
fun ShuffleGlyph(size: Dp = 24.dp, tint: Color = Color.White, modifier: Modifier = Modifier) {
    Glyph(size, tint, modifier) { s, c ->
        val w = s * 0.09f
        drawLine(c, Offset(s * 0.12f, s * 0.28f), Offset(s * 0.38f, s * 0.28f), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(c, Offset(s * 0.38f, s * 0.28f), Offset(s * 0.70f, s * 0.72f), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(c, Offset(s * 0.12f, s * 0.72f), Offset(s * 0.38f, s * 0.72f), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(c, Offset(s * 0.38f, s * 0.72f), Offset(s * 0.70f, s * 0.28f), strokeWidth = w, cap = StrokeCap.Round)
        arrowHead(Offset(s * 0.70f, s * 0.28f), 0f, s * 0.16f, c)
        arrowHead(Offset(s * 0.70f, s * 0.72f), 0f, s * 0.16f, c)
    }
}

private fun DrawScope.arrowHead(tip: Offset, angle: Float, len: Float, color: Color) {
    val path = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(tip.x - len, tip.y - len * 0.62f)
        lineTo(tip.x - len, tip.y + len * 0.62f)
        close()
    }
    drawPath(path, color)
}

@Composable
fun RepeatGlyph(size: Dp = 24.dp, tint: Color = Color.White, one: Boolean = false, modifier: Modifier = Modifier) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Glyph(size, tint) { s, c ->
            val w = s * 0.085f
            drawLine(c, Offset(s * 0.24f, s * 0.24f), Offset(s * 0.68f, s * 0.24f), strokeWidth = w, cap = StrokeCap.Round)
            drawLine(c, Offset(s * 0.68f, s * 0.24f), Offset(s * 0.82f, s * 0.40f), strokeWidth = w, cap = StrokeCap.Round)
            drawLine(c, Offset(s * 0.82f, s * 0.40f), Offset(s * 0.82f, s * 0.62f), strokeWidth = w, cap = StrokeCap.Round)
            drawLine(c, Offset(s * 0.82f, s * 0.62f), Offset(s * 0.68f, s * 0.76f), strokeWidth = w, cap = StrokeCap.Round)
            drawLine(c, Offset(s * 0.68f, s * 0.76f), Offset(s * 0.24f, s * 0.76f), strokeWidth = w, cap = StrokeCap.Round)
            drawLine(c, Offset(s * 0.24f, s * 0.76f), Offset(s * 0.16f, s * 0.60f), strokeWidth = w, cap = StrokeCap.Round)
            val path = Path().apply {
                moveTo(s * 0.62f, s * 0.10f)
                lineTo(s * 0.90f, s * 0.24f)
                lineTo(s * 0.62f, s * 0.38f)
                close()
            }
            drawPath(path, c)
        }
        if (one) {
            val density = LocalDensity.current
            Text(
                "1",
                color = tint,
                fontSize = with(density) { (size * 0.44f).toSp() },
                modifier = Modifier.offset(y = size * 0.04f)
            )
        }
    }
}

@Composable
fun MusicNoteGlyph(size: Dp = 24.dp, tint: Color = Color(0xFF9E9E9E), modifier: Modifier = Modifier) {
    Glyph(size, tint, modifier) { s, c ->
        drawOval(c, topLeft = Offset(s * 0.18f, s * 0.60f), size = Size(s * 0.34f, s * 0.26f))
        drawRoundRect(c, topLeft = Offset(s * 0.46f, s * 0.18f), size = Size(s * 0.09f, s * 0.56f), cornerRadius = CornerRadius(s * 0.02f))
        val flag = Path().apply {
            moveTo(s * 0.55f, s * 0.18f)
            lineTo(s * 0.84f, s * 0.28f)
            lineTo(s * 0.84f, s * 0.42f)
            lineTo(s * 0.55f, s * 0.32f)
            close()
        }
        drawPath(flag, c)
    }
}

@Composable
fun AlbumGlyph(size: Dp = 24.dp, tint: Color = Color(0xFF9E9E9E), modifier: Modifier = Modifier) {
    Glyph(size, tint, modifier) { s, c ->
        drawRoundRect(c, topLeft = Offset(s * 0.14f, s * 0.14f), size = Size(s * 0.72f, s * 0.72f), cornerRadius = CornerRadius(s * 0.10f))
        drawCircle(Color.White.copy(alpha = 0.85f), radius = s * 0.22f, center = Offset(s * 0.5f, s * 0.5f))
        drawCircle(c, radius = s * 0.07f, center = Offset(s * 0.5f, s * 0.5f))
    }
}

@Composable
fun ArtistGlyph(size: Dp = 24.dp, tint: Color = Color(0xFF9E9E9E), modifier: Modifier = Modifier) {
    Glyph(size, tint, modifier) { s, c ->
        drawCircle(c, radius = s * 0.20f, center = Offset(s * 0.5f, s * 0.32f))
        drawArc(
            c,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = true,
            topLeft = Offset(s * 0.16f, s * 0.56f),
            size = Size(s * 0.68f, s * 0.60f)
        )
    }
}

@Composable
fun GenreGlyph(size: Dp = 24.dp, tint: Color = Color(0xFF9E9E9E), modifier: Modifier = Modifier) {
    Glyph(size, tint, modifier) { s, c ->
        drawRoundRect(c, topLeft = Offset(s * 0.18f, s * 0.50f), size = Size(s * 0.14f, s * 0.34f), cornerRadius = CornerRadius(s * 0.03f))
        drawRoundRect(c, topLeft = Offset(s * 0.43f, s * 0.24f), size = Size(s * 0.14f, s * 0.60f), cornerRadius = CornerRadius(s * 0.03f))
        drawRoundRect(c, topLeft = Offset(s * 0.68f, s * 0.40f), size = Size(s * 0.14f, s * 0.44f), cornerRadius = CornerRadius(s * 0.03f))
    }
}

@Composable
fun PlaylistGlyph(size: Dp = 24.dp, tint: Color = Color(0xFF9E9E9E), modifier: Modifier = Modifier) {
    Glyph(size, tint, modifier) { s, c ->
        val w = s * 0.08f
        drawLine(c, Offset(s * 0.16f, s * 0.28f), Offset(s * 0.62f, s * 0.28f), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(c, Offset(s * 0.16f, s * 0.50f), Offset(s * 0.62f, s * 0.50f), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(c, Offset(s * 0.16f, s * 0.72f), Offset(s * 0.44f, s * 0.72f), strokeWidth = w, cap = StrokeCap.Round)
        drawCircle(c, radius = s * 0.10f, center = Offset(s * 0.62f, s * 0.74f))
        drawRoundRect(c, topLeft = Offset(s * 0.70f, s * 0.30f), size = Size(s * 0.07f, s * 0.46f), cornerRadius = CornerRadius(s * 0.02f))
    }
}

@Composable
fun HeartGlyph(size: Dp = 24.dp, tint: Color = Color(0xFF9E9E9E), modifier: Modifier = Modifier) {
    Glyph(size, tint, modifier) { s, c ->
        val path = Path().apply {
            moveTo(s * 0.5f, s * 0.84f)
            cubicTo(s * 0.05f, s * 0.55f, s * 0.12f, s * 0.14f, s * 0.5f, s * 0.34f)
            cubicTo(s * 0.88f, s * 0.14f, s * 0.95f, s * 0.55f, s * 0.5f, s * 0.84f)
            close()
        }
        drawPath(path, c)
    }
}

@Composable
fun RecentGlyph(size: Dp = 24.dp, tint: Color = Color(0xFF9E9E9E), modifier: Modifier = Modifier) {
    Glyph(size, tint, modifier) { s, c ->
        drawRoundRect(
            c, topLeft = Offset(s * 0.14f, s * 0.14f), size = Size(s * 0.72f, s * 0.72f),
            cornerRadius = CornerRadius(s * 0.12f), style = Stroke(width = s * 0.07f)
        )
        drawLine(c, Offset(s * 0.5f, s * 0.30f), Offset(s * 0.5f, s * 0.54f), strokeWidth = s * 0.07f, cap = StrokeCap.Round)
        drawLine(c, Offset(s * 0.5f, s * 0.54f), Offset(s * 0.66f, s * 0.62f), strokeWidth = s * 0.07f, cap = StrokeCap.Round)
    }
}

@Composable
fun ClockGlyph(size: Dp = 24.dp, tint: Color = Color(0xFF9E9E9E), modifier: Modifier = Modifier) {
    Glyph(size, tint, modifier) { s, c ->
        drawCircle(c, radius = s * 0.38f, center = Offset(s * 0.5f, s * 0.5f), style = Stroke(width = s * 0.07f))
        drawLine(c, Offset(s * 0.5f, s * 0.28f), Offset(s * 0.5f, s * 0.52f), strokeWidth = s * 0.07f, cap = StrokeCap.Round)
        drawLine(c, Offset(s * 0.5f, s * 0.52f), Offset(s * 0.66f, s * 0.62f), strokeWidth = s * 0.07f, cap = StrokeCap.Round)
    }
}

@Composable
fun TrendGlyph(size: Dp = 24.dp, tint: Color = Color(0xFF9E9E9E), modifier: Modifier = Modifier) {
    Glyph(size, tint, modifier) { s, c ->
        val w = s * 0.075f
        drawLine(c, Offset(s * 0.14f, s * 0.74f), Offset(s * 0.42f, s * 0.46f), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(c, Offset(s * 0.42f, s * 0.46f), Offset(s * 0.58f, s * 0.62f), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(c, Offset(s * 0.58f, s * 0.62f), Offset(s * 0.86f, s * 0.30f), strokeWidth = w, cap = StrokeCap.Round)
        val path = Path().apply {
            moveTo(s * 0.86f, s * 0.28f)
            lineTo(s * 0.86f, s * 0.50f)
            lineTo(s * 0.64f, s * 0.30f)
            close()
        }
        drawPath(path, c)
    }
}

@Composable
fun AddBoxGlyph(size: Dp = 24.dp, tint: Color = Color(0xFF9E9E9E), modifier: Modifier = Modifier) {
    Glyph(size, tint, modifier) { s, c ->
        drawRoundRect(
            c, topLeft = Offset(s * 0.13f, s * 0.13f), size = Size(s * 0.74f, s * 0.74f),
            cornerRadius = CornerRadius(s * 0.12f), style = Stroke(width = s * 0.07f)
        )
        drawLine(c, Offset(s * 0.5f, s * 0.32f), Offset(s * 0.5f, s * 0.68f), strokeWidth = s * 0.08f, cap = StrokeCap.Round)
        drawLine(c, Offset(s * 0.32f, s * 0.5f), Offset(s * 0.68f, s * 0.5f), strokeWidth = s * 0.08f, cap = StrokeCap.Round)
    }
}

@Composable
fun FolderGlyph(size: Dp = 24.dp, tint: Color = Color(0xFF9E9E9E), modifier: Modifier = Modifier) {
    Glyph(size, tint, modifier) { s, c ->
        drawRoundRect(c, topLeft = Offset(s * 0.10f, s * 0.22f), size = Size(s * 0.34f, s * 0.16f), cornerRadius = CornerRadius(s * 0.04f))
        drawRoundRect(c, topLeft = Offset(s * 0.10f, s * 0.28f), size = Size(s * 0.80f, s * 0.52f), cornerRadius = CornerRadius(s * 0.07f))
    }
}

@Composable
fun DragHandleGlyph(size: Dp = 24.dp, tint: Color = Color(0xFF9E9E9E), modifier: Modifier = Modifier) {
    Glyph(size, tint, modifier) { s, c ->
        for (row in 0..2) {
            for (col in 0..1) {
                drawCircle(c, radius = s * 0.055f, center = Offset(s * (0.38f + col * 0.24f), s * (0.28f + row * 0.22f)))
            }
        }
    }
}

@Composable
fun UndoGlyph(size: Dp = 24.dp, tint: Color = Color.White, modifier: Modifier = Modifier) {
    Glyph(size, tint, modifier) { s, c ->
        drawArc(
            c, startAngle = 160f, sweepAngle = 230f, useCenter = false,
            topLeft = Offset(s * 0.22f, s * 0.26f), size = Size(s * 0.56f, s * 0.52f),
            style = Stroke(width = s * 0.09f, cap = StrokeCap.Round)
        )
        val path = Path().apply {
            moveTo(s * 0.30f, s * 0.16f)
            lineTo(s * 0.30f, s * 0.44f)
            lineTo(s * 0.06f, s * 0.34f)
            close()
        }
        drawPath(path, c)
    }
}

@Composable
fun TimerGlyph(size: Dp = 24.dp, tint: Color = Color.White, modifier: Modifier = Modifier) {
    Glyph(size, tint, modifier) { s, c ->
        drawCircle(c, radius = s * 0.34f, center = Offset(s * 0.5f, s * 0.56f), style = Stroke(width = s * 0.08f))
        drawLine(c, Offset(s * 0.5f, s * 0.56f), Offset(s * 0.5f, s * 0.36f), strokeWidth = s * 0.08f, cap = StrokeCap.Round)
        drawLine(c, Offset(s * 0.38f, s * 0.12f), Offset(s * 0.62f, s * 0.12f), strokeWidth = s * 0.09f, cap = StrokeCap.Round)
    }
}

@Composable
fun EqualizerGlyph(size: Dp = 24.dp, tint: Color = Color(0xFF9E9E9E), modifier: Modifier = Modifier) {
    Glyph(size, tint, modifier) { s, c ->
        drawRoundRect(c, topLeft = Offset(s * 0.18f, s * 0.44f), size = Size(s * 0.12f, s * 0.40f), cornerRadius = CornerRadius(s * 0.03f))
        drawRoundRect(c, topLeft = Offset(s * 0.44f, s * 0.20f), size = Size(s * 0.12f, s * 0.64f), cornerRadius = CornerRadius(s * 0.03f))
        drawRoundRect(c, topLeft = Offset(s * 0.70f, s * 0.34f), size = Size(s * 0.12f, s * 0.50f), cornerRadius = CornerRadius(s * 0.03f))
    }
}

@Composable
fun ChevronGlyph(size: Dp = 20.dp, tint: Color = Color(0xFF9E9E9E), up: Boolean = false, modifier: Modifier = Modifier) {
    Glyph(size, tint, modifier) { s, c ->
        val w = s * 0.10f
        if (up) {
            drawLine(c, Offset(s * 0.24f, s * 0.62f), Offset(s * 0.5f, s * 0.36f), strokeWidth = w, cap = StrokeCap.Round)
            drawLine(c, Offset(s * 0.5f, s * 0.36f), Offset(s * 0.76f, s * 0.62f), strokeWidth = w, cap = StrokeCap.Round)
        } else {
            drawLine(c, Offset(s * 0.36f, s * 0.24f), Offset(s * 0.64f, s * 0.5f), strokeWidth = w, cap = StrokeCap.Round)
            drawLine(c, Offset(s * 0.64f, s * 0.5f), Offset(s * 0.36f, s * 0.76f), strokeWidth = w, cap = StrokeCap.Round)
        }
    }
}

@Composable
fun EqualizerBarsGlyph(size: Dp = 20.dp, tint: Color = Color.White, modifier: Modifier = Modifier) {
    Glyph(size, tint, modifier) { s, c ->
        drawRoundRect(c, topLeft = Offset(s * 0.16f, s * 0.50f), size = Size(s * 0.12f, s * 0.34f), cornerRadius = CornerRadius(s * 0.03f))
        drawRoundRect(c, topLeft = Offset(s * 0.44f, s * 0.22f), size = Size(s * 0.12f, s * 0.62f), cornerRadius = CornerRadius(s * 0.03f))
        drawRoundRect(c, topLeft = Offset(s * 0.72f, s * 0.38f), size = Size(s * 0.12f, s * 0.46f), cornerRadius = CornerRadius(s * 0.03f))
    }
}

@Composable
fun SearchFieldGlyph(size: Dp = 22.dp, tint: Color = Color.White, modifier: Modifier = Modifier) {
    Glyph(size, tint, modifier) { s, c ->
        drawCircle(c, radius = s * 0.28f, center = Offset(s * 0.42f, s * 0.42f), style = Stroke(width = s * 0.09f))
        drawLine(c, Offset(s * 0.62f, s * 0.62f), Offset(s * 0.84f, s * 0.84f), strokeWidth = s * 0.10f, cap = StrokeCap.Round)
    }
}

@Composable
fun MenuGlyph(size: Dp = 24.dp, tint: Color = Color.White, modifier: Modifier = Modifier) {
    Glyph(size, tint, modifier) { s, c ->
        val w = s * 0.08f
        drawLine(c, Offset(s * 0.16f, s * 0.28f), Offset(s * 0.84f, s * 0.28f), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(c, Offset(s * 0.16f, s * 0.50f), Offset(s * 0.84f, s * 0.50f), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(c, Offset(s * 0.16f, s * 0.72f), Offset(s * 0.84f, s * 0.72f), strokeWidth = w, cap = StrokeCap.Round)
    }
}

@Composable
fun MoreVertGlyph(size: Dp = 24.dp, tint: Color = Color.White, modifier: Modifier = Modifier) {
    Glyph(size, tint, modifier) { s, c ->
        drawCircle(c, radius = s * 0.075f, center = Offset(s * 0.5f, s * 0.22f))
        drawCircle(c, radius = s * 0.075f, center = Offset(s * 0.5f, s * 0.5f))
        drawCircle(c, radius = s * 0.075f, center = Offset(s * 0.5f, s * 0.78f))
    }
}

@Composable
fun CloseGlyph(size: Dp = 24.dp, tint: Color = Color.White, modifier: Modifier = Modifier) {
    Glyph(size, tint, modifier) { s, c ->
        val w = s * 0.09f
        drawLine(c, Offset(s * 0.24f, s * 0.24f), Offset(s * 0.76f, s * 0.76f), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(c, Offset(s * 0.76f, s * 0.24f), Offset(s * 0.24f, s * 0.76f), strokeWidth = w, cap = StrokeCap.Round)
    }
}

@Composable
fun BackGlyph(size: Dp = 24.dp, tint: Color = Color.White, modifier: Modifier = Modifier) {
    Glyph(size, tint, modifier) { s, c ->
        val w = s * 0.09f
        drawLine(c, Offset(s * 0.72f, s * 0.5f), Offset(s * 0.28f, s * 0.5f), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(c, Offset(s * 0.28f, s * 0.5f), Offset(s * 0.48f, s * 0.28f), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(c, Offset(s * 0.28f, s * 0.5f), Offset(s * 0.48f, s * 0.72f), strokeWidth = w, cap = StrokeCap.Round)
    }
}

@Composable
fun ShareGlyph(size: Dp = 24.dp, tint: Color = Color(0xFF757575), modifier: Modifier = Modifier) {
    Glyph(size, tint, modifier) { s, c ->
        drawCircle(c, radius = s * 0.11f, center = Offset(s * 0.74f, s * 0.24f))
        drawCircle(c, radius = s * 0.11f, center = Offset(s * 0.26f, s * 0.5f))
        drawCircle(c, radius = s * 0.11f, center = Offset(s * 0.74f, s * 0.76f))
        val w = s * 0.07f
        drawLine(c, Offset(s * 0.35f, s * 0.45f), Offset(s * 0.65f, s * 0.29f), strokeWidth = w)
        drawLine(c, Offset(s * 0.35f, s * 0.55f), Offset(s * 0.65f, s * 0.71f), strokeWidth = w)
    }
}

@Composable
fun DeleteGlyph(size: Dp = 24.dp, tint: Color = Color(0xFFD32F2F), modifier: Modifier = Modifier) {
    Glyph(size, tint, modifier) { s, c ->
        drawRoundRect(c, topLeft = Offset(s * 0.26f, s * 0.30f), size = Size(s * 0.48f, s * 0.52f), cornerRadius = CornerRadius(s * 0.05f))
        drawRoundRect(c, topLeft = Offset(s * 0.18f, s * 0.20f), size = Size(s * 0.64f, s * 0.08f), cornerRadius = CornerRadius(s * 0.03f))
        drawRoundRect(c, topLeft = Offset(s * 0.40f, s * 0.12f), size = Size(s * 0.20f, s * 0.06f), cornerRadius = CornerRadius(s * 0.02f))
    }
}

@Composable
fun EditGlyph(size: Dp = 24.dp, tint: Color = Color(0xFF757575), modifier: Modifier = Modifier) {
    Glyph(size, tint, modifier) { s, c ->
        drawRoundRect(c, topLeft = Offset(s * 0.18f, s * 0.60f), size = Size(s * 0.22f, s * 0.22f), cornerRadius = CornerRadius(s * 0.03f))
        val path = Path().apply {
            moveTo(s * 0.34f, s * 0.70f)
            lineTo(s * 0.78f, s * 0.26f)
            lineTo(s * 0.88f, s * 0.36f)
            lineTo(s * 0.44f, s * 0.80f)
            close()
        }
        drawPath(path, c)
    }
}

@Composable
fun InfoGlyph(size: Dp = 24.dp, tint: Color = Color(0xFF757575), modifier: Modifier = Modifier) {
    Glyph(size, tint, modifier) { s, c ->
        drawCircle(c, radius = s * 0.38f, center = Offset(s * 0.5f, s * 0.5f), style = Stroke(width = s * 0.07f))
        drawCircle(c, radius = s * 0.05f, center = Offset(s * 0.5f, s * 0.32f))
        drawRoundRect(c, topLeft = Offset(s * 0.45f, s * 0.44f), size = Size(s * 0.10f, s * 0.26f), cornerRadius = CornerRadius(s * 0.04f))
    }
}

@Composable
fun SettingsGlyph(size: Dp = 24.dp, tint: Color = Color(0xFF757575), modifier: Modifier = Modifier) {
    Glyph(size, tint, modifier) { s, c ->
        drawCircle(c, radius = s * 0.30f, center = Offset(s * 0.5f, s * 0.5f), style = Stroke(width = s * 0.10f))
        for (i in 0 until 8) {
            val angle = Math.toRadians((i * 45).toDouble())
            val cx = s * 0.5f + (s * 0.40f * kotlin.math.cos(angle)).toFloat()
            val cy = s * 0.5f + (s * 0.40f * kotlin.math.sin(angle)).toFloat()
            drawCircle(c, radius = s * 0.06f, center = Offset(cx, cy))
        }
    }
}

@Composable
fun AboutGlyph(size: Dp = 24.dp, tint: Color = Color(0xFF757575), modifier: Modifier = Modifier) {
    Glyph(size, tint, modifier) { s, c ->
        drawCircle(c, radius = s * 0.38f, center = Offset(s * 0.5f, s * 0.5f), style = Stroke(width = s * 0.07f))
        val path = Path().apply {
            moveTo(s * 0.36f, s * 0.34f)
            quadraticBezierTo(s * 0.36f, s * 0.20f, s * 0.52f, s * 0.22f)
            quadraticBezierTo(s * 0.66f, s * 0.24f, s * 0.64f, s * 0.40f)
            quadraticBezierTo(s * 0.62f, s * 0.48f, s * 0.52f, s * 0.52f)
            lineTo(s * 0.50f, s * 0.62f)
        }
        drawPath(path, c, style = Stroke(width = s * 0.08f, cap = StrokeCap.Round))
        drawCircle(c, radius = s * 0.055f, center = Offset(s * 0.48f, s * 0.76f))
    }
}

@Composable
fun RescanGlyph(size: Dp = 24.dp, tint: Color = Color(0xFF757575), modifier: Modifier = Modifier) {
    Glyph(size, tint, modifier) { s, c ->
        drawArc(
            c, startAngle = 40f, sweepAngle = 280f, useCenter = false,
            topLeft = Offset(s * 0.18f, s * 0.18f), size = Size(s * 0.64f, s * 0.64f),
            style = Stroke(width = s * 0.09f, cap = StrokeCap.Round)
        )
        val path = Path().apply {
            moveTo(s * 0.82f, s * 0.18f)
            lineTo(s * 0.92f, s * 0.44f)
            lineTo(s * 0.64f, s * 0.40f)
            close()
        }
        drawPath(path, c)
    }
}

@Composable
fun LibraryGlyph(size: Dp = 24.dp, tint: Color = Color(0xFFE91E63), modifier: Modifier = Modifier) {
    Glyph(size, tint, modifier) { s, c ->
        drawRoundRect(c, topLeft = Offset(s * 0.14f, s * 0.18f), size = Size(s * 0.72f, s * 0.64f), cornerRadius = CornerRadius(s * 0.10f))
        drawRoundRect(Color.White, topLeft = Offset(s * 0.26f, s * 0.44f), size = Size(s * 0.48f, s * 0.08f), cornerRadius = CornerRadius(s * 0.04f))
        drawRoundRect(Color.White, topLeft = Offset(s * 0.46f, s * 0.28f), size = Size(s * 0.08f, s * 0.40f), cornerRadius = CornerRadius(s * 0.04f))
    }
}
