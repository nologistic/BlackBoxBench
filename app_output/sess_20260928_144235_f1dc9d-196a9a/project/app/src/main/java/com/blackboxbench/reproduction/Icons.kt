package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Icon set used by the reproduction: Material core icons where they fit, Canvas glyphs otherwise. */
enum class VlcIcon {
    CONE, BACK, SEARCH, MORE, CHECK, CLOSE, CLEAR, ADD, DELETE, SHARE, INFO, SETTINGS,
    HEART, HEART_FILL, PLAY, REFRESH, LIST, GRID, STAR, LOCK, PERSON, BELL, ARROW_FORWARD,
    ARROW_DOWN, ARROW_RIGHT, DATE, WARNING,
    VIDEO, MUSIC, FOLDER, PLAYLIST, SHUFFLE, REPEAT, EQUALIZER, CAST, SUBTITLES,
    HEADPHONES, RING, CLOCK, QUEUE, MOON, LANGUAGE, TV, TUNE, SORT, DRAG, OPEN_FOLDER, PAUSE,
}

@Composable
fun VlcIconView(icon: VlcIcon, size: Dp, tint: Color, modifier: Modifier = Modifier) {
    val vector: ImageVector? = when (icon) {
        VlcIcon.BACK -> Icons.Default.ArrowBack
        VlcIcon.SEARCH -> Icons.Default.Search
        VlcIcon.MORE -> Icons.Default.MoreVert
        VlcIcon.CHECK -> Icons.Default.Check
        VlcIcon.CLOSE -> Icons.Default.Close
        VlcIcon.CLEAR -> Icons.Default.Clear
        VlcIcon.ADD -> Icons.Default.Add
        VlcIcon.DELETE -> Icons.Default.Delete
        VlcIcon.SHARE -> Icons.Default.Share
        VlcIcon.INFO -> Icons.Default.Info
        VlcIcon.SETTINGS -> Icons.Default.Settings
        VlcIcon.HEART -> Icons.Default.FavoriteBorder
        VlcIcon.HEART_FILL -> Icons.Default.Favorite
        VlcIcon.PLAY -> Icons.Default.PlayArrow
        VlcIcon.REFRESH -> Icons.Default.Refresh
        VlcIcon.LIST -> Icons.Default.List
        VlcIcon.STAR -> Icons.Default.Star
        VlcIcon.LOCK -> Icons.Default.Lock
        VlcIcon.PERSON -> Icons.Default.Person
        VlcIcon.BELL -> Icons.Default.Notifications
        VlcIcon.ARROW_FORWARD -> Icons.Default.ArrowForward
        VlcIcon.ARROW_DOWN -> Icons.Default.KeyboardArrowDown
        VlcIcon.ARROW_RIGHT -> Icons.Default.KeyboardArrowRight
        VlcIcon.DATE -> Icons.Default.DateRange
        VlcIcon.WARNING -> Icons.Default.Warning
        else -> null
    }
    if (vector != null) {
        androidx.compose.material3.Icon(vector, contentDescription = null, tint = tint, modifier = modifier.size(size))
    } else {
        Canvas(modifier = modifier.size(size)) { drawGlyph(icon, tint) }
    }
}

private fun DrawScope.drawGlyph(icon: VlcIcon, tint: Color) {
    val w = size.width
    val h = size.height
    val sw = w * 0.085f
    fun p(x: Float, y: Float) = Offset(x * w, y * h)
    when (icon) {
        VlcIcon.CONE -> {
            val body = Path().apply {
                moveTo(w * 0.30f, h * 0.20f)
                lineTo(w * 0.70f, h * 0.20f)
                lineTo(w * 0.88f, h * 0.88f)
                lineTo(w * 0.12f, h * 0.88f)
                close()
            }
            drawPath(body, tint)
            drawRoundRect(
                color = Color.White,
                topLeft = p(0.32f, 0.40f),
                size = Size(w * 0.36f, h * 0.13f),
            )
            drawRoundRect(
                color = Color.White,
                topLeft = p(0.23f, 0.66f),
                size = Size(w * 0.54f, h * 0.13f),
            )
        }
        VlcIcon.VIDEO -> {
            drawRoundRect(
                color = tint,
                topLeft = p(0.08f, 0.20f),
                size = Size(w * 0.84f, h * 0.60f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.12f),
                style = Stroke(width = sw),
            )
            val tri = Path().apply {
                moveTo(w * 0.42f, h * 0.36f)
                lineTo(w * 0.66f, h * 0.50f)
                lineTo(w * 0.42f, h * 0.64f)
                close()
            }
            drawPath(tri, tint)
        }
        VlcIcon.MUSIC -> {
            drawCircle(tint, radius = w * 0.10f, center = p(0.30f, 0.74f))
            drawCircle(tint, radius = w * 0.10f, center = p(0.66f, 0.66f))
            drawLine(tint, p(0.39f, 0.74f), p(0.39f, 0.28f), strokeWidth = sw)
            drawLine(tint, p(0.75f, 0.66f), p(0.75f, 0.20f), strokeWidth = sw)
            drawLine(tint, p(0.39f, 0.28f), p(0.80f, 0.20f), strokeWidth = sw * 1.6f)
        }
        VlcIcon.FOLDER -> {
            val path = Path().apply {
                moveTo(w * 0.08f, h * 0.26f)
                lineTo(w * 0.42f, h * 0.26f)
                lineTo(w * 0.50f, h * 0.36f)
                lineTo(w * 0.92f, h * 0.36f)
                lineTo(w * 0.92f, h * 0.80f)
                lineTo(w * 0.08f, h * 0.80f)
                close()
            }
            drawPath(path, tint, style = Stroke(width = sw, join = androidx.compose.ui.graphics.StrokeJoin.Round))
        }
        VlcIcon.PLAYLIST -> {
            drawLine(tint, p(0.10f, 0.30f), p(0.62f, 0.30f), strokeWidth = sw, cap = StrokeCap.Round)
            drawLine(tint, p(0.10f, 0.50f), p(0.62f, 0.50f), strokeWidth = sw, cap = StrokeCap.Round)
            drawLine(tint, p(0.10f, 0.70f), p(0.40f, 0.70f), strokeWidth = sw, cap = StrokeCap.Round)
            drawCircle(tint, radius = w * 0.09f, center = p(0.70f, 0.72f))
            drawLine(tint, p(0.79f, 0.72f), p(0.79f, 0.34f), strokeWidth = sw)
            drawLine(tint, p(0.79f, 0.34f), p(0.94f, 0.30f), strokeWidth = sw)
        }
        VlcIcon.SHUFFLE -> {
            drawLine(tint, p(0.10f, 0.30f), p(0.86f, 0.30f), strokeWidth = sw, cap = StrokeCap.Round)
            drawLine(tint, p(0.10f, 0.70f), p(0.40f, 0.70f), strokeWidth = sw, cap = StrokeCap.Round)
            drawLine(tint, p(0.48f, 0.70f), p(0.86f, 0.30f), strokeWidth = sw, cap = StrokeCap.Round)
            val a = Path().apply {
                moveTo(w * 0.76f, h * 0.20f); lineTo(w * 0.92f, h * 0.30f); lineTo(w * 0.76f, h * 0.40f); close()
            }
            drawPath(a, tint)
            val b = Path().apply {
                moveTo(w * 0.76f, h * 0.60f); lineTo(w * 0.92f, h * 0.70f); lineTo(w * 0.76f, h * 0.80f); close()
            }
            drawPath(b, tint)
        }
        VlcIcon.REPEAT -> {
            drawArc(tint, 200f, 200f, false, topLeft = p(0.10f, 0.16f), size = Size(w * 0.80f, h * 0.80f), style = Stroke(width = sw))
            val a = Path().apply {
                moveTo(w * 0.16f, h * 0.18f); lineTo(w * 0.30f, h * 0.30f); lineTo(w * 0.14f, h * 0.38f); close()
            }
            drawPath(a, tint)
        }
        VlcIcon.EQUALIZER -> {
            val hs = listOf(0.42f, 0.72f, 0.30f, 0.60f)
            var x = 0.16f
            hs.forEach { v ->
                drawLine(tint, p(x, 0.86f), p(x, 0.86f - v), strokeWidth = sw * 1.2f, cap = StrokeCap.Round)
                x += 0.22f
            }
        }
        VlcIcon.CAST -> {
            drawRoundRect(tint, topLeft = p(0.06f, 0.14f), size = Size(w * 0.52f, h * 0.44f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.06f), style = Stroke(width = sw))
            drawArc(tint, 180f, 90f, false, topLeft = p(0.50f, 0.50f), size = Size(w * 0.44f, h * 0.44f), style = Stroke(width = sw))
            drawArc(tint, 180f, 90f, false, topLeft = p(0.68f, 0.68f), size = Size(w * 0.26f, h * 0.26f), style = Stroke(width = sw))
            drawCircle(tint, radius = w * 0.05f, center = p(0.58f, 0.86f))
        }
        VlcIcon.SUBTITLES -> {
            drawRoundRect(tint, topLeft = p(0.08f, 0.18f), size = Size(w * 0.84f, h * 0.60f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.10f), style = Stroke(width = sw))
            drawLine(tint, p(0.22f, 0.56f), p(0.46f, 0.56f), strokeWidth = sw, cap = StrokeCap.Round)
            drawLine(tint, p(0.54f, 0.56f), p(0.78f, 0.56f), strokeWidth = sw, cap = StrokeCap.Round)
            drawLine(tint, p(0.22f, 0.68f), p(0.60f, 0.68f), strokeWidth = sw, cap = StrokeCap.Round)
        }
        VlcIcon.HEADPHONES -> {
            drawArc(tint, 180f, 180f, false, topLeft = p(0.12f, 0.18f), size = Size(w * 0.76f, h * 0.70f), style = Stroke(width = sw))
            drawRoundRect(tint, topLeft = p(0.10f, 0.52f), size = Size(w * 0.18f, h * 0.32f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f))
            drawRoundRect(tint, topLeft = p(0.72f, 0.52f), size = Size(w * 0.18f, h * 0.32f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f))
        }
        VlcIcon.PAUSE -> {
            drawRoundRect(
                color = tint,
                topLeft = p(0.26f, 0.18f),
                size = Size(w * 0.16f, h * 0.64f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.04f),
            )
            drawRoundRect(
                color = tint,
                topLeft = p(0.58f, 0.18f),
                size = Size(w * 0.16f, h * 0.64f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.04f),
            )
        }
        VlcIcon.RING -> {
            drawCircle(tint, radius = w * 0.36f, center = p(0.5f, 0.5f), style = Stroke(width = w * 0.09f))
        }
        VlcIcon.CLOCK -> {
            drawCircle(tint, radius = w * 0.40f, center = p(0.5f, 0.5f), style = Stroke(width = sw))
            drawLine(tint, p(0.5f, 0.26f), p(0.5f, 0.52f), strokeWidth = sw, cap = StrokeCap.Round)
            drawLine(tint, p(0.5f, 0.52f), p(0.70f, 0.62f), strokeWidth = sw, cap = StrokeCap.Round)
        }
        VlcIcon.QUEUE -> {
            drawLine(tint, p(0.10f, 0.26f), p(0.68f, 0.26f), strokeWidth = sw, cap = StrokeCap.Round)
            drawLine(tint, p(0.10f, 0.50f), p(0.68f, 0.50f), strokeWidth = sw, cap = StrokeCap.Round)
            drawLine(tint, p(0.10f, 0.74f), p(0.44f, 0.74f), strokeWidth = sw, cap = StrokeCap.Round)
            drawLine(tint, p(0.80f, 0.56f), p(0.80f, 0.86f), strokeWidth = sw, cap = StrokeCap.Round)
            drawLine(tint, p(0.65f, 0.71f), p(0.95f, 0.71f), strokeWidth = sw, cap = StrokeCap.Round)
        }
        VlcIcon.MOON -> {
            val path = Path().apply {
                moveTo(w * 0.72f, h * 0.14f)
                cubicTo(w * 0.34f, h * 0.20f, w * 0.20f, h * 0.62f, w * 0.52f, h * 0.86f)
                cubicTo(w * 0.24f, h * 0.86f, w * 0.06f, h * 0.62f, w * 0.18f, h * 0.34f)
                cubicTo(w * 0.28f, h * 0.12f, w * 0.52f, h * 0.06f, w * 0.72f, h * 0.14f)
                close()
            }
            drawPath(path, tint)
        }
        VlcIcon.LANGUAGE -> {
            drawLine(tint, p(0.08f, 0.76f), p(0.36f, 0.22f), strokeWidth = sw, cap = StrokeCap.Round)
            drawLine(tint, p(0.64f, 0.22f), p(0.92f, 0.76f), strokeWidth = sw, cap = StrokeCap.Round)
            drawLine(tint, p(0.20f, 0.58f), p(0.80f, 0.58f), strokeWidth = sw, cap = StrokeCap.Round)
        }
        VlcIcon.TV -> {
            drawRoundRect(tint, topLeft = p(0.06f, 0.18f), size = Size(w * 0.88f, h * 0.56f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.07f), style = Stroke(width = sw))
            drawLine(tint, p(0.34f, 0.86f), p(0.66f, 0.86f), strokeWidth = sw, cap = StrokeCap.Round)
        }
        VlcIcon.TUNE -> {
            listOf(0.28f, 0.52f, 0.76f).forEachIndexed { i, y ->
                drawLine(tint, p(0.10f, y), p(0.90f, y), strokeWidth = sw * 0.9f, cap = StrokeCap.Round)
                val x = if (i % 2 == 0) 0.34f else 0.66f
                drawCircle(tint, radius = w * 0.10f, center = p(x, y))
                drawCircle(Color.White, radius = w * 0.045f, center = p(x, y))
            }
        }
        VlcIcon.SORT -> {
            drawLine(tint, p(0.08f, 0.24f), p(0.60f, 0.24f), strokeWidth = sw, cap = StrokeCap.Round)
            drawLine(tint, p(0.08f, 0.50f), p(0.48f, 0.50f), strokeWidth = sw, cap = StrokeCap.Round)
            drawLine(tint, p(0.08f, 0.76f), p(0.36f, 0.76f), strokeWidth = sw, cap = StrokeCap.Round)
            drawLine(tint, p(0.78f, 0.20f), p(0.78f, 0.84f), strokeWidth = sw, cap = StrokeCap.Round)
            drawLine(tint, p(0.60f, 0.84f), p(0.78f, 0.84f), strokeWidth = sw, cap = StrokeCap.Round)
        }
        VlcIcon.DRAG -> {
            listOf(0.34f, 0.5f, 0.66f).forEach { y ->
                drawLine(tint, p(0.14f, y), p(0.86f, y), strokeWidth = sw, cap = StrokeCap.Round)
            }
        }
        VlcIcon.OPEN_FOLDER -> {
            val path = Path().apply {
                moveTo(w * 0.08f, h * 0.78f)
                lineTo(w * 0.20f, h * 0.36f)
                lineTo(w * 0.94f, h * 0.36f)
                lineTo(w * 0.82f, h * 0.78f)
                close()
            }
            drawPath(path, tint, style = Stroke(width = sw))
            drawLine(tint, p(0.18f, 0.26f), p(0.56f, 0.26f), strokeWidth = sw)
        }
        else -> {
            drawCircle(tint, radius = w * 0.3f, center = p(0.5f, 0.5f))
        }
    }
}
