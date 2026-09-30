package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
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

/** Simple line-art glyphs that mimic the observed Tasks.org icon set. */
enum class Glyph { Checklist, Funnel, Calendar, History, Tag, List, Sort, Mic, Inbox, Repeat, Flag, Bell, Note, Link, Paperclip, Timer, CalendarOff, Place }

@Composable
fun GlyphIcon(
    glyph: Glyph,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    stroke: Dp = 1.8.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val s = stroke.toPx()
        val st = Stroke(width = s, cap = StrokeCap.Round)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(tint, Offset(x1 * w, y1 * h), Offset(x2 * w, y2 * h), s, StrokeCap.Round)

        when (glyph) {
            Glyph.Checklist -> {
                drawRoundRect(
                    tint, Offset(w * 0.08f, h * 0.14f), Size(w * 0.84f, h * 0.72f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.16f), style = st
                )
                line(0.28f, 0.40f, 0.44f, 0.56f)
                line(0.44f, 0.56f, 0.74f, 0.32f)
            }
            Glyph.Funnel -> {
                line(0.14f, 0.24f, 0.86f, 0.24f)
                line(0.24f, 0.5f, 0.76f, 0.5f)
                line(0.38f, 0.76f, 0.62f, 0.76f)
            }
            Glyph.Calendar -> {
                drawRoundRect(
                    tint, Offset(w * 0.12f, h * 0.18f), Size(w * 0.76f, h * 0.68f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.12f), style = st
                )
                line(0.12f, 0.40f, 0.88f, 0.40f)
                line(0.32f, 0.10f, 0.32f, 0.26f)
                line(0.68f, 0.10f, 0.68f, 0.26f)
            }
            Glyph.History -> {
                drawCircle(tint, w * 0.32f, Offset(w * 0.5f, h * 0.52f), style = st)
                line(0.5f, 0.52f, 0.5f, 0.30f)
                line(0.5f, 0.52f, 0.68f, 0.60f)
            }
            Glyph.Tag -> {
                val p = Path().apply {
                    moveTo(w * 0.14f, h * 0.20f)
                    lineTo(w * 0.56f, h * 0.20f)
                    lineTo(w * 0.90f, h * 0.52f)
                    lineTo(w * 0.54f, h * 0.86f)
                    lineTo(w * 0.14f, h * 0.50f)
                    close()
                }
                drawPath(p, tint, style = st)
            }
            Glyph.List -> {
                repeat(3) { i ->
                    val y = 0.28f + i * 0.22f
                    drawCircle(tint, w * 0.045f, Offset(w * 0.22f, h * y))
                    line(0.40f, y, 0.84f, y)
                }
            }
            Glyph.Sort -> {
                line(0.32f, 0.24f, 0.32f, 0.80f)
                line(0.32f, 0.24f, 0.20f, 0.38f)
                line(0.32f, 0.24f, 0.44f, 0.38f)
                line(0.68f, 0.76f, 0.68f, 0.20f)
                line(0.68f, 0.76f, 0.56f, 0.62f)
                line(0.68f, 0.76f, 0.80f, 0.62f)
            }
            Glyph.Mic -> {
                drawRoundRect(
                    tint, Offset(w * 0.36f, h * 0.12f), Size(w * 0.28f, h * 0.46f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.14f), style = st
                )
                val p = Path().apply {
                    moveTo(w * 0.22f, h * 0.50f)
                    cubicTo(w * 0.22f, h * 0.84f, w * 0.78f, h * 0.84f, w * 0.78f, h * 0.50f)
                }
                drawPath(p, tint, style = st)
                line(0.5f, 0.80f, 0.5f, 0.94f)
            }
            Glyph.Inbox -> {
                drawRoundRect(
                    tint, Offset(w * 0.10f, h * 0.18f), Size(w * 0.80f, h * 0.64f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.12f), style = st
                )
                val p = Path().apply {
                    moveTo(w * 0.10f, h * 0.56f)
                    lineTo(w * 0.34f, h * 0.56f)
                    cubicTo(w * 0.40f, h * 0.70f, w * 0.60f, h * 0.70f, w * 0.66f, h * 0.56f)
                    lineTo(w * 0.90f, h * 0.56f)
                }
                drawPath(p, tint, style = st)
            }
            Glyph.Repeat -> {
                line(0.24f, 0.34f, 0.70f, 0.34f)
                line(0.70f, 0.34f, 0.70f, 0.62f)
                line(0.70f, 0.66f, 0.30f, 0.66f)
                line(0.30f, 0.66f, 0.30f, 0.50f)
                line(0.70f, 0.34f, 0.58f, 0.24f)
                line(0.30f, 0.66f, 0.42f, 0.76f)
            }
            Glyph.Flag -> {
                line(0.26f, 0.14f, 0.26f, 0.88f)
                val p = Path().apply {
                    moveTo(w * 0.26f, h * 0.18f)
                    lineTo(w * 0.80f, h * 0.18f)
                    lineTo(w * 0.66f, h * 0.40f)
                    lineTo(w * 0.80f, h * 0.62f)
                    lineTo(w * 0.26f, h * 0.62f)
                }
                drawPath(p, tint, style = st)
            }
            Glyph.Bell -> {
                val p = Path().apply {
                    moveTo(w * 0.22f, h * 0.70f)
                    lineTo(w * 0.22f, h * 0.46f)
                    cubicTo(w * 0.22f, h * 0.14f, w * 0.78f, h * 0.14f, w * 0.78f, h * 0.46f)
                    lineTo(w * 0.78f, h * 0.70f)
                    lineTo(w * 0.84f, h * 0.78f)
                    lineTo(w * 0.16f, h * 0.78f)
                    close()
                }
                drawPath(p, tint, style = st)
                line(0.42f, 0.86f, 0.58f, 0.86f)
            }
            Glyph.Note -> {
                repeat(3) { i ->
                    val y = 0.28f + i * 0.22f
                    line(0.22f, y, if (i == 2) 0.62f else 0.80f, y)
                }
            }
            Glyph.Link -> {
                line(0.40f, 0.58f, 0.60f, 0.40f)
                drawCircle(tint, w * 0.16f, Offset(w * 0.30f, h * 0.66f), style = st)
                drawCircle(tint, w * 0.16f, Offset(w * 0.70f, h * 0.32f), style = st)
            }
            Glyph.Paperclip -> {
                val p = Path().apply {
                    moveTo(w * 0.72f, h * 0.46f)
                    lineTo(w * 0.40f, h * 0.78f)
                    cubicTo(w * 0.20f, h * 0.94f, w * 0.04f, h * 0.70f, w * 0.22f, h * 0.50f)
                    lineTo(w * 0.58f, h * 0.16f)
                    cubicTo(w * 0.74f, h * 0.02f, w * 0.92f, h * 0.24f, w * 0.76f, h * 0.40f)
                    lineTo(w * 0.46f, h * 0.70f)
                }
                drawPath(p, tint, style = st)
            }
            Glyph.Timer -> {
                drawCircle(tint, w * 0.34f, Offset(w * 0.5f, h * 0.56f), style = st)
                line(0.5f, 0.56f, 0.5f, 0.38f)
                line(0.42f, 0.10f, 0.58f, 0.10f)
            }
            Glyph.CalendarOff -> {
                drawRoundRect(
                    tint, Offset(w * 0.12f, h * 0.20f), Size(w * 0.76f, h * 0.64f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.12f), style = st
                )
                line(0.12f, 0.42f, 0.88f, 0.42f)
                line(0.24f, 0.10f, 0.76f, 0.92f)
            }
            Glyph.Place -> {
                val p = Path().apply {
                    moveTo(w * 0.5f, h * 0.92f)
                    cubicTo(w * 0.5f, h * 0.92f, w * 0.16f, h * 0.58f, w * 0.16f, h * 0.40f)
                    cubicTo(w * 0.16f, h * 0.14f, w * 0.84f, h * 0.14f, w * 0.84f, h * 0.40f)
                    cubicTo(w * 0.84f, h * 0.58f, w * 0.5f, h * 0.92f, w * 0.5f, h * 0.92f)
                    close()
                }
                drawPath(p, tint, style = st)
                drawCircle(tint, w * 0.09f, Offset(w * 0.5f, h * 0.38f), style = st)
            }
        }
    }
}

@Composable
fun CoreIcon(image: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, size: Dp = 22.dp) {
    Icon(imageVector = image, contentDescription = null, tint = tint, modifier = Modifier.size(size))
}

val IconMenu = Icons.Filled.Menu
val IconSearch = Icons.Filled.Search
val IconMore = Icons.Filled.MoreVert
val IconSettings = Icons.Filled.Settings
val IconAdd = Icons.Filled.Add
val IconBack = Icons.Filled.ArrowBack
val IconTrash = Icons.Filled.Delete
val IconCheck = Icons.Filled.Check
val IconClose = Icons.Filled.Close
val IconUp = Icons.Filled.KeyboardArrowUp
val IconDown = Icons.Filled.KeyboardArrowDown
val IconPlace = Icons.Filled.Place
val IconPlay = Icons.Filled.PlayArrow
