package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Simple monochrome glyphs approximating the observed tool icons. */
@Composable
fun ToolGlyph(id: String, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(28.dp)) {
        val w = size.width
        val h = size.height
        val sw = w * 0.075f
        val stroke = Stroke(width = sw)
        when (id) {
            "tune" -> {
                for (i in 0..2) {
                    val y = h * (0.25f + 0.25f * i)
                    drawLine(tint, Offset(w * 0.12f, y), Offset(w * 0.88f, y), sw)
                    drawCircle(tint, w * 0.09f, Offset(w * (0.3f + 0.2f * i), y))
                }
            }
            "details" -> {
                val p = Path().apply {
                    moveTo(w * 0.1f, h * 0.85f); lineTo(w * 0.5f, h * 0.15f); lineTo(w * 0.9f, h * 0.85f); close()
                }
                drawPath(p, tint, style = stroke)
            }
            "curves" -> {
                val p = Path().apply {
                    moveTo(w * 0.12f, h * 0.85f)
                    cubicTo(w * 0.4f, h * 0.85f, w * 0.6f, h * 0.2f, w * 0.88f, h * 0.18f)
                }
                drawPath(p, tint, style = stroke)
            }
            "whitebalance" -> {
                drawCircle(tint, w * 0.38f, Offset(w * 0.5f, h * 0.5f), style = stroke)
                drawArc(tint, 90f, 180f, true, topLeft = Offset(w * 0.12f, h * 0.12f), size = Size(w * 0.76f, h * 0.76f))
            }
            "crop" -> {
                drawLine(tint, Offset(w * 0.3f, h * 0.1f), Offset(w * 0.3f, h * 0.7f), sw)
                drawLine(tint, Offset(w * 0.3f, h * 0.7f), Offset(w * 0.9f, h * 0.7f), sw)
                drawLine(tint, Offset(w * 0.1f, h * 0.3f), Offset(w * 0.7f, h * 0.3f), sw)
                drawLine(tint, Offset(w * 0.7f, h * 0.3f), Offset(w * 0.7f, h * 0.9f), sw)
            }
            "rotate" -> {
                drawArc(tint, 40f, 280f, false, topLeft = Offset(w * 0.15f, h * 0.15f), size = Size(w * 0.7f, h * 0.7f), style = stroke)
                val p = Path().apply {
                    moveTo(w * 0.72f, h * 0.12f); lineTo(w * 0.92f, h * 0.28f); lineTo(w * 0.68f, h * 0.34f); close()
                }
                drawPath(p, tint)
            }
            "perspective" -> {
                val p = Path().apply {
                    moveTo(w * 0.22f, h * 0.2f); lineTo(w * 0.78f, h * 0.2f); lineTo(w * 0.92f, h * 0.8f); lineTo(w * 0.08f, h * 0.8f); close()
                }
                drawPath(p, tint, style = stroke)
            }
            "expand" -> {
                drawLine(tint, Offset(w * 0.15f, h * 0.1f), Offset(w * 0.15f, h * 0.9f), sw)
                drawLine(tint, Offset(w * 0.85f, h * 0.1f), Offset(w * 0.85f, h * 0.9f), sw)
                drawLine(tint, Offset(w * 0.1f, h * 0.15f), Offset(w * 0.9f, h * 0.15f), sw)
                drawLine(tint, Offset(w * 0.1f, h * 0.85f), Offset(w * 0.9f, h * 0.85f), sw)
            }
            "selective" -> {
                drawCircle(tint, w * 0.3f, Offset(w * 0.5f, h * 0.5f), style = stroke)
                drawCircle(tint, w * 0.07f, Offset(w * 0.5f, h * 0.5f))
            }
            "brush" -> {
                drawLine(tint, Offset(w * 0.2f, h * 0.8f), Offset(w * 0.7f, h * 0.3f), sw * 2.2f)
                drawCircle(tint, w * 0.12f, Offset(w * 0.22f, h * 0.78f))
            }
            "healing" -> {
                drawRoundRect(tint, Offset(w * 0.15f, h * 0.38f), Size(w * 0.7f, h * 0.24f), androidx.compose.ui.geometry.CornerRadius(w * 0.12f), style = stroke)
                drawCircle(tint, w * 0.04f, Offset(w * 0.35f, h * 0.5f))
                drawCircle(tint, w * 0.04f, Offset(w * 0.65f, h * 0.5f))
            }
            "hdr" -> {
                val p = Path().apply {
                    moveTo(w * 0.08f, h * 0.8f); lineTo(w * 0.38f, h * 0.32f); lineTo(w * 0.58f, h * 0.62f)
                    lineTo(w * 0.72f, h * 0.42f); lineTo(w * 0.94f, h * 0.8f); close()
                }
                drawPath(p, tint)
            }
            "glamour" -> {
                drawCircle(tint, w * 0.18f, Offset(w * 0.5f, h * 0.5f))
                for (i in 0 until 8) {
                    val a = Math.toRadians((i * 45).toDouble())
                    val x1 = w * 0.5f + (w * 0.3f * Math.cos(a)).toFloat()
                    val y1 = h * 0.5f + (h * 0.3f * Math.sin(a)).toFloat()
                    val x2 = w * 0.5f + (w * 0.44f * Math.cos(a)).toFloat()
                    val y2 = h * 0.5f + (h * 0.44f * Math.sin(a)).toFloat()
                    drawLine(tint, Offset(x1, y1), Offset(x2, y2), sw)
                }
            }
            "tonal" -> {
                drawCircle(tint, w * 0.42f, Offset(w * 0.5f, h * 0.5f), style = stroke)
                drawArc(tint, 180f, 180f, true, topLeft = Offset(w * 0.08f, h * 0.08f), size = Size(w * 0.84f, h * 0.84f))
            }
            "drama" -> {
                drawArc(tint, 180f, 180f, true, topLeft = Offset(w * 0.1f, h * 0.25f), size = Size(w * 0.8f, h * 0.8f))
                drawLine(tint, Offset(w * 0.5f, h * 0.6f), Offset(w * 0.5f, h * 0.92f), sw)
            }
            "vintage" -> {
                val p = Path().apply {
                    moveTo(w * 0.2f, h * 0.85f); lineTo(w * 0.75f, h * 0.85f); lineTo(w * 0.6f, h * 0.35f)
                    lineTo(w * 0.35f, h * 0.35f); close()
                }
                drawPath(p, tint, style = stroke)
                drawLine(tint, Offset(w * 0.16f, h * 0.88f), Offset(w * 0.8f, h * 0.88f), sw * 1.6f)
            }
            "grainy" -> {
                drawRect(tint, Offset(w * 0.12f, h * 0.3f), Size(w * 0.76f, h * 0.4f), style = stroke)
                for (i in 0 until 3) {
                    drawRect(tint, Offset(w * (0.2f + 0.25f * i), h * 0.2f), Size(w * 0.08f, h * 0.1f))
                    drawRect(tint, Offset(w * (0.2f + 0.25f * i), h * 0.7f), Size(w * 0.08f, h * 0.1f))
                }
            }
            "retrolux" -> {
                drawRoundRect(tint, Offset(w * 0.1f, h * 0.32f), Size(w * 0.8f, h * 0.42f), androidx.compose.ui.geometry.CornerRadius(w * 0.08f), style = stroke)
                drawCircle(tint, w * 0.16f, Offset(w * 0.5f, h * 0.53f), style = stroke)
                drawCircle(tint, w * 0.07f, Offset(w * 0.5f, h * 0.53f))
            }
            "grunge" -> {
                drawCircle(tint, w * 0.22f, Offset(w * 0.4f, h * 0.45f))
                drawCircle(tint, w * 0.14f, Offset(w * 0.68f, h * 0.62f))
                drawCircle(tint, w * 0.08f, Offset(w * 0.62f, h * 0.3f))
            }
            "bw" -> {
                drawRect(tint, Offset(w * 0.12f, h * 0.12f), Size(w * 0.76f, h * 0.76f), style = stroke)
                drawRect(tint, Offset(w * 0.12f, h * 0.5f), Size(w * 0.76f, h * 0.38f))
            }
            "bwfilm" -> {
                drawRect(tint, Offset(w * 0.12f, h * 0.3f), Size(w * 0.76f, h * 0.4f), style = stroke)
                for (i in 0 until 4) {
                    drawRect(tint, Offset(w * (0.16f + 0.2f * i), h * 0.16f), Size(w * 0.1f, h * 0.08f))
                    drawRect(tint, Offset(w * (0.16f + 0.2f * i), h * 0.76f), Size(w * 0.1f, h * 0.08f))
                }
            }
            "face" -> {
                drawCircle(tint, w * 0.34f, Offset(w * 0.5f, h * 0.5f), style = stroke)
                drawCircle(tint, w * 0.04f, Offset(w * 0.38f, h * 0.44f))
                drawCircle(tint, w * 0.04f, Offset(w * 0.62f, h * 0.44f))
                drawArc(tint, 20f, 140f, false, topLeft = Offset(w * 0.34f, h * 0.5f), size = Size(w * 0.32f, h * 0.24f), style = stroke)
            }
            "headpose" -> {
                drawLine(tint, Offset(w * 0.5f, h * 0.5f), Offset(w * 0.85f, h * 0.5f), sw)
                drawLine(tint, Offset(w * 0.5f, h * 0.5f), Offset(w * 0.5f, h * 0.15f), sw)
                drawLine(tint, Offset(w * 0.5f, h * 0.5f), Offset(w * 0.3f, h * 0.82f), sw)
                drawCircle(tint, w * 0.13f, Offset(w * 0.5f, h * 0.5f), style = stroke)
            }
            "lensblur" -> {
                drawCircle(tint, w * 0.42f, Offset(w * 0.5f, h * 0.5f), style = Stroke(width = sw * 0.7f))
                drawCircle(tint, w * 0.26f, Offset(w * 0.5f, h * 0.5f), style = Stroke(width = sw * 0.7f))
                drawCircle(tint, w * 0.1f, Offset(w * 0.5f, h * 0.5f))
            }
            "vignette" -> {
                drawRoundRect(tint, Offset(w * 0.12f, h * 0.12f), Size(w * 0.76f, h * 0.76f), androidx.compose.ui.geometry.CornerRadius(w * 0.24f), style = stroke)
                drawCircle(tint, w * 0.12f, Offset(w * 0.5f, h * 0.5f))
            }
            "double" -> {
                drawCircle(tint, w * 0.26f, Offset(w * 0.38f, h * 0.5f), style = stroke)
                drawCircle(tint, w * 0.26f, Offset(w * 0.62f, h * 0.5f), style = stroke)
            }
            "text" -> {
                drawLine(tint, Offset(w * 0.2f, h * 0.25f), Offset(w * 0.8f, h * 0.25f), sw * 1.6f)
                drawLine(tint, Offset(w * 0.5f, h * 0.25f), Offset(w * 0.5f, h * 0.8f), sw * 1.6f)
            }
            "frames" -> {
                drawRect(tint, Offset(w * 0.1f, h * 0.14f), Size(w * 0.8f, h * 0.72f), style = Stroke(width = sw * 1.8f))
            }
            else -> drawCircle(tint, w * 0.3f, Offset(w * 0.5f, h * 0.5f))
        }
    }
}

/** The layered-sheets icon used for the edit stack, optionally with a count badge. */
@Composable
fun StackGlyph(badge: Int, tint: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(26.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(22.dp)) {
            val w = size.width
            val h = size.height
            val sw = w * 0.09f
            for (i in 0..2) {
                val o = i * h * 0.16f
                drawLine(tint, Offset(w * 0.12f, h * 0.22f + o), Offset(w * 0.88f, h * 0.22f + o), sw)
            }
        }
    }
}
