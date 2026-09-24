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

/**
 * Hand drawn glyphs for the transport / media controls, so the build only depends
 * on compose-ui plus material-icons-core.
 */
@Composable
private fun Glyph(modifier: Modifier, size: Dp, draw: androidx.compose.ui.graphics.drawscope.DrawScope.(Float) -> Unit) {
    Canvas(modifier = modifier.size(size)) { draw(this.size.minDimension) }
}

@Composable
fun PlayGlyph(color: Color, size: Dp = 32.dp, modifier: Modifier = Modifier) =
    Glyph(modifier, size) { s ->
        val p = Path().apply {
            moveTo(s * 0.18f, s * 0.10f)
            lineTo(s * 0.86f, s * 0.50f)
            lineTo(s * 0.18f, s * 0.90f)
            close()
        }
        drawPath(p, color)
    }

@Composable
fun PauseGlyph(color: Color, size: Dp = 32.dp, modifier: Modifier = Modifier) =
    Glyph(modifier, size) { s ->
        val w = s * 0.22f
        drawRoundRect(color, topLeft = Offset(s * 0.16f, s * 0.12f), size = Size(w, s * 0.76f))
        drawRoundRect(color, topLeft = Offset(s * 0.62f, s * 0.12f), size = Size(w, s * 0.76f))
    }

@Composable
fun NextGlyph(color: Color, size: Dp = 28.dp, modifier: Modifier = Modifier) =
    Glyph(modifier, size) { s ->
        val p = Path().apply {
            moveTo(s * 0.12f, s * 0.12f)
            lineTo(s * 0.68f, s * 0.50f)
            lineTo(s * 0.12f, s * 0.88f)
            close()
        }
        drawPath(p, color)
        drawRoundRect(color, topLeft = Offset(s * 0.74f, s * 0.12f), size = Size(s * 0.14f, s * 0.76f))
    }

@Composable
fun PrevGlyph(color: Color, size: Dp = 28.dp, modifier: Modifier = Modifier) =
    Glyph(modifier, size) { s ->
        val p = Path().apply {
            moveTo(s * 0.88f, s * 0.12f)
            lineTo(s * 0.32f, s * 0.50f)
            lineTo(s * 0.88f, s * 0.88f)
            close()
        }
        drawPath(p, color)
        drawRoundRect(color, topLeft = Offset(s * 0.12f, s * 0.12f), size = Size(s * 0.14f, s * 0.76f))
    }

@Composable
fun ShuffleGlyph(color: Color, size: Dp = 26.dp, modifier: Modifier = Modifier) =
    Glyph(modifier, size) { s ->
        val w = s * 0.10f
        drawLine(color, Offset(s * 0.06f, s * 0.22f), Offset(s * 0.40f, s * 0.50f), w, StrokeCap.Round)
        drawLine(color, Offset(s * 0.40f, s * 0.50f), Offset(s * 0.86f, s * 0.50f), w, StrokeCap.Round)
        drawLine(color, Offset(s * 0.06f, s * 0.78f), Offset(s * 0.36f, s * 0.50f), w, StrokeCap.Round)
        val head = Path().apply {
            moveTo(s * 0.90f, s * 0.36f); lineTo(s * 0.90f, s * 0.64f); lineTo(s * 0.66f, s * 0.50f); close()
        }
        drawPath(head, color)
        drawLine(color, Offset(s * 0.06f, s * 0.22f), Offset(s * 0.30f, s * 0.22f), w, StrokeCap.Round)
        drawLine(color, Offset(s * 0.06f, s * 0.78f), Offset(s * 0.30f, s * 0.78f), w, StrokeCap.Round)
    }

@Composable
fun RepeatGlyph(color: Color, size: Dp = 26.dp, modifier: Modifier = Modifier) =
    Glyph(modifier, size) { s ->
        val w = s * 0.09f
        drawRoundRect(
            color, topLeft = Offset(s * 0.14f, s * 0.22f), size = Size(s * 0.72f, s * 0.56f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.20f), style = Stroke(width = w)
        )
        val head = Path().apply {
            moveTo(s * 0.50f, s * 0.06f); lineTo(s * 0.74f, s * 0.24f); lineTo(s * 0.50f, s * 0.42f); close()
        }
        drawPath(head, color)
    }

@Composable
fun NoteGlyph(color: Color, size: Dp = 26.dp, modifier: Modifier = Modifier) =
    Glyph(modifier, size) { s ->
        drawCircle(color, s * 0.13f, Offset(s * 0.26f, s * 0.74f))
        drawCircle(color, s * 0.13f, Offset(s * 0.70f, s * 0.64f))
        drawLine(color, Offset(s * 0.37f, s * 0.74f), Offset(s * 0.37f, s * 0.24f), s * 0.08f, StrokeCap.Round)
        drawLine(color, Offset(s * 0.81f, s * 0.64f), Offset(s * 0.81f, s * 0.16f), s * 0.08f, StrokeCap.Round)
        drawLine(color, Offset(s * 0.37f, s * 0.24f), Offset(s * 0.81f, s * 0.16f), s * 0.08f, StrokeCap.Round)
    }

@Composable
fun QueueGlyph(color: Color, size: Dp = 26.dp, modifier: Modifier = Modifier) =
    Glyph(modifier, size) { s ->
        val w = s * 0.09f
        listOf(0.24f, 0.50f, 0.76f).forEach { y ->
            drawLine(color, Offset(s * 0.10f, s * y), Offset(s * 0.62f, s * y), w, StrokeCap.Round)
        }
        drawCircle(color, s * 0.09f, Offset(s * 0.84f, s * 0.24f))
        drawCircle(color, s * 0.09f, Offset(s * 0.84f, s * 0.50f))
    }

@Composable
fun VinylGlyph(color: Color, size: Dp = 40.dp, modifier: Modifier = Modifier) =
    Glyph(modifier, size) { s ->
        drawCircle(Color(0xFF151515), s * 0.50f, Offset(s * 0.5f, s * 0.5f))
        drawCircle(color, s * 0.47f, Offset(s * 0.5f, s * 0.5f), style = Stroke(width = s * 0.02f))
        listOf(0.42f, 0.37f, 0.32f, 0.27f).forEach { r ->
            drawCircle(Color(0x33FFFFFF), s * r, Offset(s * 0.5f, s * 0.5f), style = Stroke(width = s * 0.025f))
        }
        drawCircle(Color(0xFFBDBDBD), s * 0.17f, Offset(s * 0.5f, s * 0.5f))
        drawCircle(Color(0xFF6E6E6E), s * 0.05f, Offset(s * 0.5f, s * 0.5f))
    }

@Composable
fun GridGlyph(color: Color, size: Dp = 24.dp, modifier: Modifier = Modifier) =
    Glyph(modifier, size) { s ->
        val c = s * 0.30f
        val g = s * 0.10f
        drawRect(color, Offset(g, g), Size(c, c))
        drawRect(color, Offset(g * 2 + c, g), Size(c, c))
        drawRect(color, Offset(g, g * 2 + c), Size(c, c))
        drawRect(color, Offset(g * 2 + c, g * 2 + c), Size(c, c))
    }

@Composable
fun EqualizerGlyph(color: Color, size: Dp = 26.dp, modifier: Modifier = Modifier) =
    Glyph(modifier, size) { s ->
        val w = s * 0.12f
        drawLine(color, Offset(s * 0.22f, s * 0.14f), Offset(s * 0.22f, s * 0.86f), w, StrokeCap.Round)
        drawLine(color, Offset(s * 0.50f, s * 0.14f), Offset(s * 0.50f, s * 0.86f), w, StrokeCap.Round)
        drawLine(color, Offset(s * 0.78f, s * 0.14f), Offset(s * 0.78f, s * 0.86f), w, StrokeCap.Round)
        drawCircle(color, s * 0.14f, Offset(s * 0.22f, s * 0.36f))
        drawCircle(color, s * 0.14f, Offset(s * 0.50f, s * 0.66f))
        drawCircle(color, s * 0.14f, Offset(s * 0.78f, s * 0.28f))
    }

@Composable
fun TagGlyph(color: Color, size: Dp = 26.dp, modifier: Modifier = Modifier) =
    Glyph(modifier, size) { s ->
        val p = Path().apply {
            moveTo(s * 0.10f, s * 0.18f); lineTo(s * 0.54f, s * 0.18f); lineTo(s * 0.90f, s * 0.50f)
            lineTo(s * 0.54f, s * 0.82f); lineTo(s * 0.10f, s * 0.82f); close()
        }
        drawPath(p, color)
    }

@Composable
fun ClockGlyph(color: Color, size: Dp = 26.dp, modifier: Modifier = Modifier) =
    Glyph(modifier, size) { s ->
        drawCircle(color, s * 0.44f, Offset(s * 0.5f, s * 0.5f), style = Stroke(width = s * 0.09f))
        drawLine(color, Offset(s * 0.5f, s * 0.26f), Offset(s * 0.5f, s * 0.52f), s * 0.08f, StrokeCap.Round)
        drawLine(color, Offset(s * 0.5f, s * 0.52f), Offset(s * 0.70f, s * 0.62f), s * 0.08f, StrokeCap.Round)
    }

@Composable
fun TrendGlyph(color: Color, size: Dp = 26.dp, modifier: Modifier = Modifier) =
    Glyph(modifier, size) { s ->
        val w = s * 0.09f
        drawLine(color, Offset(s * 0.10f, s * 0.78f), Offset(s * 0.42f, s * 0.46f), w, StrokeCap.Round)
        drawLine(color, Offset(s * 0.42f, s * 0.46f), Offset(s * 0.60f, s * 0.64f), w, StrokeCap.Round)
        drawLine(color, Offset(s * 0.60f, s * 0.64f), Offset(s * 0.88f, s * 0.30f), w, StrokeCap.Round)
        val head = Path().apply {
            moveTo(s * 0.92f, s * 0.20f); lineTo(s * 0.68f, s * 0.24f); lineTo(s * 0.86f, s * 0.42f); close()
        }
        drawPath(head, color)
    }

@Composable
fun HeartGlyph(color: Color, size: Dp = 26.dp, filled: Boolean = true, modifier: Modifier = Modifier) =
    Glyph(modifier, size) { s ->
        val p = Path().apply {
            moveTo(s * 0.50f, s * 0.86f)
            cubicTo(s * 0.02f, s * 0.54f, s * 0.10f, s * 0.10f, s * 0.50f, s * 0.32f)
            cubicTo(s * 0.90f, s * 0.10f, s * 0.98f, s * 0.54f, s * 0.50f, s * 0.86f)
            close()
        }
        if (filled) drawPath(p, color) else drawPath(p, color, style = Stroke(width = s * 0.09f))
    }

@Composable
fun FolderGlyph(color: Color, size: Dp = 30.dp, modifier: Modifier = Modifier) =
    Glyph(modifier, size) { s ->
        val p = Path().apply {
            moveTo(s * 0.08f, s * 0.26f); lineTo(s * 0.42f, s * 0.26f); lineTo(s * 0.50f, s * 0.38f)
            lineTo(s * 0.92f, s * 0.38f); lineTo(s * 0.92f, s * 0.82f); lineTo(s * 0.08f, s * 0.82f); close()
        }
        drawPath(p, color)
    }

@Composable
fun LibraryGlyph(color: Color, size: Dp = 30.dp, modifier: Modifier = Modifier) =
    Glyph(modifier, size) { s ->
        drawRoundRect(color, Offset(s * 0.12f, s * 0.24f), Size(s * 0.18f, s * 0.60f))
        drawRoundRect(color, Offset(s * 0.41f, s * 0.24f), Size(s * 0.18f, s * 0.60f))
        val p = Path().apply {
            moveTo(s * 0.82f, s * 0.18f); lineTo(s * 0.94f, s * 0.86f); lineTo(s * 0.72f, s * 0.86f); close()
        }
        drawPath(p, color)
    }

@Composable
fun InfoGlyph(color: Color, size: Dp = 30.dp, modifier: Modifier = Modifier) =
    Glyph(modifier, size) { s ->
        drawCircle(color, s * 0.44f, Offset(s * 0.5f, s * 0.5f), style = Stroke(width = s * 0.08f))
        drawCircle(color, s * 0.06f, Offset(s * 0.5f, s * 0.32f))
        drawLine(color, Offset(s * 0.5f, s * 0.46f), Offset(s * 0.5f, s * 0.70f), s * 0.08f, StrokeCap.Round)
    }

@Composable
fun GearGlyph(color: Color, size: Dp = 30.dp, modifier: Modifier = Modifier) =
    Glyph(modifier, size) { s ->
        drawCircle(color, s * 0.30f, Offset(s * 0.5f, s * 0.5f), style = Stroke(width = s * 0.10f))
        listOf(0f, 45f, 90f, 135f).forEach { angle ->
            val rad = Math.toRadians(angle.toDouble())
            val dx = kotlin.math.cos(rad).toFloat()
            val dy = kotlin.math.sin(rad).toFloat()
            drawLine(
                color,
                Offset(s * (0.5f + dx * 0.30f), s * (0.5f + dy * 0.30f)),
                Offset(s * (0.5f + dx * 0.46f), s * (0.5f + dy * 0.46f)),
                s * 0.10f, StrokeCap.Round
            )
            drawLine(
                color,
                Offset(s * (0.5f - dx * 0.30f), s * (0.5f - dy * 0.30f)),
                Offset(s * (0.5f - dx * 0.46f), s * (0.5f - dy * 0.46f)),
                s * 0.10f, StrokeCap.Round
            )
        }
    }

@Composable
fun SearchGlyph(color: Color, size: Dp = 26.dp, modifier: Modifier = Modifier) =
    Glyph(modifier, size) { s ->
        drawCircle(color, s * 0.30f, Offset(s * 0.42f, s * 0.42f), style = Stroke(width = s * 0.10f))
        drawLine(color, Offset(s * 0.63f, s * 0.63f), Offset(s * 0.88f, s * 0.88f), s * 0.10f, StrokeCap.Round)
    }

@Composable
fun MenuGlyph(color: Color, size: Dp = 26.dp, modifier: Modifier = Modifier) =
    Glyph(modifier, size) { s ->
        val w = s * 0.10f
        listOf(0.24f, 0.50f, 0.76f).forEach { y ->
            drawLine(color, Offset(s * 0.12f, s * y), Offset(s * 0.88f, s * y), w, StrokeCap.Round)
        }
    }

@Composable
fun CloseGlyph(color: Color, size: Dp = 26.dp, modifier: Modifier = Modifier) =
    Glyph(modifier, size) { s ->
        val w = s * 0.10f
        drawLine(color, Offset(s * 0.18f, s * 0.18f), Offset(s * 0.82f, s * 0.82f), w, StrokeCap.Round)
        drawLine(color, Offset(s * 0.82f, s * 0.18f), Offset(s * 0.18f, s * 0.82f), w, StrokeCap.Round)
    }

@Composable
fun BackGlyph(color: Color, size: Dp = 26.dp, modifier: Modifier = Modifier) =
    Glyph(modifier, size) { s ->
        val w = s * 0.10f
        drawLine(color, Offset(s * 0.80f, s * 0.5f), Offset(s * 0.24f, s * 0.5f), w, StrokeCap.Round)
        drawLine(color, Offset(s * 0.24f, s * 0.5f), Offset(s * 0.50f, s * 0.24f), w, StrokeCap.Round)
        drawLine(color, Offset(s * 0.24f, s * 0.5f), Offset(s * 0.50f, s * 0.76f), w, StrokeCap.Round)
    }

@Composable
fun ChevronDownGlyph(color: Color, size: Dp = 26.dp, modifier: Modifier = Modifier) =
    Glyph(modifier, size) { s ->
        val w = s * 0.10f
        drawLine(color, Offset(s * 0.22f, s * 0.36f), Offset(s * 0.5f, s * 0.64f), w, StrokeCap.Round)
        drawLine(color, Offset(s * 0.5f, s * 0.64f), Offset(s * 0.78f, s * 0.36f), w, StrokeCap.Round)
    }

@Composable
fun ChevronUpGlyph(color: Color, size: Dp = 26.dp, modifier: Modifier = Modifier) =
    Glyph(modifier, size) { s ->
        val w = s * 0.10f
        drawLine(color, Offset(s * 0.22f, s * 0.64f), Offset(s * 0.5f, s * 0.36f), w, StrokeCap.Round)
        drawLine(color, Offset(s * 0.5f, s * 0.36f), Offset(s * 0.78f, s * 0.64f), w, StrokeCap.Round)
    }

@Composable
fun DotsGlyph(color: Color, size: Dp = 26.dp, modifier: Modifier = Modifier) =
    Glyph(modifier, size) { s ->
        drawCircle(color, s * 0.08f, Offset(s * 0.5f, s * 0.20f))
        drawCircle(color, s * 0.08f, Offset(s * 0.5f, s * 0.50f))
        drawCircle(color, s * 0.08f, Offset(s * 0.5f, s * 0.80f))
    }

@Composable
fun CheckGlyph(color: Color, size: Dp = 26.dp, modifier: Modifier = Modifier) =
    Glyph(modifier, size) { s ->
        val w = s * 0.12f
        drawLine(color, Offset(s * 0.18f, s * 0.52f), Offset(s * 0.42f, s * 0.76f), w, StrokeCap.Round)
        drawLine(color, Offset(s * 0.42f, s * 0.76f), Offset(s * 0.84f, s * 0.26f), w, StrokeCap.Round)
    }

@Composable
fun PlusGlyph(color: Color, size: Dp = 26.dp, modifier: Modifier = Modifier) =
    Glyph(modifier, size) { s ->
        val w = s * 0.10f
        drawLine(color, Offset(s * 0.5f, s * 0.18f), Offset(s * 0.5f, s * 0.82f), w, StrokeCap.Round)
        drawLine(color, Offset(s * 0.18f, s * 0.5f), Offset(s * 0.82f, s * 0.5f), w, StrokeCap.Round)
    }

@Composable
fun SelectAllGlyph(color: Color, size: Dp = 26.dp, modifier: Modifier = Modifier) =
    Glyph(modifier, size) { s ->
        val w = s * 0.10f
        drawLine(color, Offset(s * 0.10f, s * 0.30f), Offset(s * 0.46f, s * 0.66f), w, StrokeCap.Round)
        drawLine(color, Offset(s * 0.46f, s * 0.66f), Offset(s * 0.90f, s * 0.18f), w, StrokeCap.Round)
        drawLine(color, Offset(s * 0.10f, s * 0.70f), Offset(s * 0.34f, s * 0.92f), w, StrokeCap.Round)
    }

@Composable
fun SaveGlyph(color: Color, size: Dp = 26.dp, modifier: Modifier = Modifier) =
    Glyph(modifier, size) { s ->
        drawRoundRect(color, Offset(s * 0.14f, s * 0.14f), Size(s * 0.72f, s * 0.72f), style = Stroke(width = s * 0.09f))
        drawRect(color, Offset(s * 0.30f, s * 0.14f), Size(s * 0.40f, s * 0.26f))
        drawRect(color, Offset(s * 0.28f, s * 0.56f), Size(s * 0.44f, s * 0.30f))
    }

@Composable
fun BellGlyph(color: Color, size: Dp = 26.dp, modifier: Modifier = Modifier) =
    Glyph(modifier, size) { s ->
        val p = Path().apply {
            moveTo(s * 0.5f, s * 0.12f)
            cubicTo(s * 0.24f, s * 0.12f, s * 0.24f, s * 0.40f, s * 0.22f, s * 0.62f)
            lineTo(s * 0.78f, s * 0.62f)
            cubicTo(s * 0.76f, s * 0.40f, s * 0.76f, s * 0.12f, s * 0.5f, s * 0.12f)
            close()
        }
        drawPath(p, color)
        drawCircle(color, s * 0.09f, Offset(s * 0.5f, s * 0.78f))
    }
