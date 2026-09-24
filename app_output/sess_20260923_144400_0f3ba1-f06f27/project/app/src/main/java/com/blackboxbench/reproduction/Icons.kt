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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Hand drawn glyphs for the toolbar icons that are not part of material-icons-core. */
@Composable
fun Glyph(modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 22.dp, color: Color = Color(0xFF3B3B3B), kind: String) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = w * 0.085f)
        when (kind) {
            "arrow_back" -> {
                drawLine(color, Offset(w * 0.82f, h * 0.5f), Offset(w * 0.2f, h * 0.5f), strokeWidth = w * 0.09f)
                val head = Path().apply {
                    moveTo(w * 0.44f, h * 0.22f)
                    lineTo(w * 0.18f, h * 0.5f)
                    lineTo(w * 0.44f, h * 0.78f)
                }
                drawPath(head, color, style = Stroke(width = w * 0.09f))
            }
            "search" -> {
                drawCircle(color, radius = w * 0.3f, center = Offset(w * 0.44f, h * 0.44f), style = stroke)
                drawLine(color, Offset(w * 0.66f, h * 0.66f), Offset(w * 0.88f, h * 0.88f), strokeWidth = w * 0.1f)
            }
            "camera" -> {
                val body = Rect(w * 0.08f, h * 0.26f, w * 0.92f, h * 0.86f)
                drawRoundRect(
                    color = color,
                    topLeft = Offset(body.left, body.top),
                    size = Size(body.width, body.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.12f),
                    style = stroke
                )
                val bump = Path().apply {
                    moveTo(w * 0.3f, h * 0.26f)
                    lineTo(w * 0.38f, h * 0.12f)
                    lineTo(w * 0.62f, h * 0.12f)
                    lineTo(w * 0.7f, h * 0.26f)
                }
                drawPath(bump, color, style = stroke)
                drawCircle(color, radius = w * 0.16f, center = Offset(w * 0.5f, h * 0.57f), style = stroke)
            }
            "media" -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.08f, h * 0.18f),
                    size = Size(w * 0.84f, h * 0.64f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.1f),
                    style = stroke
                )
                drawCircle(color, radius = w * 0.07f, center = Offset(w * 0.3f, h * 0.38f))
                val hill = Path().apply {
                    moveTo(w * 0.14f, h * 0.74f)
                    lineTo(w * 0.4f, h * 0.46f)
                    lineTo(w * 0.6f, h * 0.66f)
                    lineTo(w * 0.72f, h * 0.55f)
                    lineTo(w * 0.87f, h * 0.74f)
                    close()
                }
                drawPath(hill, color)
            }
            "folders" -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.06f, h * 0.3f),
                    size = Size(w * 0.88f, h * 0.55f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f),
                    style = stroke
                )
                val tab = Path().apply {
                    moveTo(w * 0.06f, h * 0.3f)
                    lineTo(w * 0.06f, h * 0.18f)
                    lineTo(w * 0.42f, h * 0.18f)
                    lineTo(w * 0.5f, h * 0.3f)
                }
                drawPath(tab, color, style = stroke)
            }
            "sort" -> {
                for (i in 0..2) {
                    val y = h * (0.28f + i * 0.22f)
                    drawLine(color, Offset(w * 0.14f, y), Offset(w * (0.86f - i * 0.18f), y), strokeWidth = w * 0.085f)
                }
            }
            "filter" -> {
                val funnel = Path().apply {
                    moveTo(w * 0.12f, h * 0.2f)
                    lineTo(w * 0.88f, h * 0.2f)
                    lineTo(w * 0.58f, h * 0.55f)
                    lineTo(w * 0.58f, h * 0.85f)
                    lineTo(w * 0.42f, h * 0.75f)
                    lineTo(w * 0.42f, h * 0.55f)
                    close()
                }
                drawPath(funnel, color, style = stroke)
            }
            "names" -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.08f, h * 0.2f),
                    size = Size(w * 0.84f, h * 0.6f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.1f),
                    style = stroke
                )
                drawLine(color, Offset(w * 0.2f, h * 0.4f), Offset(w * 0.8f, h * 0.4f), strokeWidth = w * 0.08f)
                drawLine(color, Offset(w * 0.2f, h * 0.6f), Offset(w * 0.62f, h * 0.6f), strokeWidth = w * 0.08f)
            }
            "pin" -> {
                val path = Path().apply {
                    moveTo(w * 0.5f, h * 0.82f)
                    lineTo(w * 0.5f, h * 0.5f)
                    moveTo(w * 0.3f, h * 0.16f)
                    lineTo(w * 0.7f, h * 0.16f)
                    lineTo(w * 0.62f, h * 0.44f)
                    lineTo(w * 0.38f, h * 0.44f)
                    close()
                }
                drawPath(path, color, style = Stroke(width = w * 0.11f))
            }
            "play" -> {
                val triangle = Path().apply {
                    moveTo(w * 0.3f, h * 0.22f)
                    lineTo(w * 0.78f, h * 0.5f)
                    lineTo(w * 0.3f, h * 0.78f)
                    close()
                }
                drawPath(triangle, color)
            }
            "grid" -> {
                val cell = w * 0.32f
                for (r in 0..1) for (c in 0..1) {
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(w * 0.12f + c * (cell + w * 0.12f), h * 0.12f + r * (cell + h * 0.12f)),
                        size = Size(cell, cell),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.04f),
                        style = stroke
                    )
                }
            }
            "list" -> {
                for (i in 0..2) {
                    val y = h * (0.26f + i * 0.24f)
                    drawCircle(color, radius = w * 0.06f, center = Offset(w * 0.18f, y))
                    drawLine(color, Offset(w * 0.36f, y), Offset(w * 0.86f, y), strokeWidth = w * 0.085f)
                }
            }
            "rotate" -> {
                drawArc(
                    color = color,
                    startAngle = 40f,
                    sweepAngle = 280f,
                    useCenter = false,
                    topLeft = Offset(w * 0.14f, h * 0.14f),
                    size = Size(w * 0.72f, h * 0.72f),
                    style = stroke
                )
                val head = Path().apply {
                    moveTo(w * 0.74f, h * 0.2f)
                    lineTo(w * 0.92f, h * 0.3f)
                    lineTo(w * 0.72f, h * 0.42f)
                    close()
                }
                drawPath(head, color)
            }
            "star_outline" -> drawPath(starPath(w, h), color, style = Stroke(width = w * 0.09f))
            "star_filled" -> drawPath(starPath(w, h), color)
            "pencil" -> {
                val body = Path().apply {
                    moveTo(w * 0.2f, h * 0.8f)
                    lineTo(w * 0.26f, h * 0.6f)
                    lineTo(w * 0.66f, h * 0.2f)
                    lineTo(w * 0.82f, h * 0.36f)
                    lineTo(w * 0.42f, h * 0.76f)
                    close()
                }
                drawPath(body, color)
                drawLine(color, Offset(w * 0.6f, h * 0.26f), Offset(w * 0.76f, h * 0.42f), strokeWidth = w * 0.07f)
            }
            "trash" -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.2f, h * 0.28f),
                    size = Size(w * 0.6f, h * 0.58f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.06f),
                    style = stroke
                )
                drawLine(color, Offset(w * 0.1f, h * 0.24f), Offset(w * 0.9f, h * 0.24f), strokeWidth = w * 0.09f)
                drawLine(color, Offset(w * 0.38f, h * 0.12f), Offset(w * 0.62f, h * 0.12f), strokeWidth = w * 0.09f)
                drawLine(color, Offset(w * 0.42f, h * 0.44f), Offset(w * 0.42f, h * 0.72f), strokeWidth = w * 0.06f)
                drawLine(color, Offset(w * 0.58f, h * 0.44f), Offset(w * 0.58f, h * 0.72f), strokeWidth = w * 0.06f)
            }
            "share" -> {
                drawCircle(color, radius = w * 0.1f, center = Offset(w * 0.26f, h * 0.5f))
                drawCircle(color, radius = w * 0.1f, center = Offset(w * 0.72f, h * 0.24f))
                drawCircle(color, radius = w * 0.1f, center = Offset(w * 0.72f, h * 0.76f))
                drawLine(color, Offset(w * 0.36f, h * 0.45f), Offset(w * 0.62f, h * 0.29f), strokeWidth = w * 0.06f)
                drawLine(color, Offset(w * 0.36f, h * 0.55f), Offset(w * 0.62f, h * 0.71f), strokeWidth = w * 0.06f)
            }
            "info" -> {
                drawCircle(color, radius = w * 0.42f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
                drawCircle(color, radius = w * 0.06f, center = Offset(w * 0.5f, h * 0.32f))
                drawLine(color, Offset(w * 0.5f, h * 0.46f), Offset(w * 0.5f, h * 0.7f), strokeWidth = w * 0.09f)
            }
            "dots" -> {
                for (i in 0..2) {
                    drawCircle(color, radius = w * 0.09f, center = Offset(w * 0.5f, h * (0.22f + i * 0.28f)))
                }
            }
            "check" -> {
                val path = Path().apply {
                    moveTo(w * 0.2f, h * 0.52f)
                    lineTo(w * 0.42f, h * 0.74f)
                    lineTo(w * 0.8f, h * 0.26f)
                }
                drawPath(path, color, style = Stroke(width = w * 0.13f))
            }
            "copy" -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.12f, h * 0.12f),
                    size = Size(w * 0.5f, h * 0.5f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f),
                    style = stroke
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.36f, h * 0.36f),
                    size = Size(w * 0.52f, h * 0.52f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f),
                    style = stroke
                )
            }
            "flip" -> {
                drawLine(color, Offset(w * 0.5f, h * 0.1f), Offset(w * 0.5f, h * 0.9f), strokeWidth = w * 0.07f)
                val left = Path().apply {
                    moveTo(w * 0.4f, h * 0.24f)
                    lineTo(w * 0.1f, h * 0.5f)
                    lineTo(w * 0.4f, h * 0.76f)
                    close()
                }
                drawPath(left, color, style = Stroke(width = w * 0.08f))
                val right = Path().apply {
                    moveTo(w * 0.6f, h * 0.24f)
                    lineTo(w * 0.9f, h * 0.5f)
                    lineTo(w * 0.6f, h * 0.76f)
                    close()
                }
                drawPath(right, color)
            }
            "crop" -> {
                drawLine(color, Offset(w * 0.24f, h * 0.06f), Offset(w * 0.24f, h * 0.76f), strokeWidth = w * 0.08f)
                drawLine(color, Offset(w * 0.06f, h * 0.24f), Offset(w * 0.76f, h * 0.24f), strokeWidth = w * 0.08f)
                drawLine(color, Offset(w * 0.76f, h * 0.24f), Offset(w * 0.76f, h * 0.94f), strokeWidth = w * 0.08f)
                drawLine(color, Offset(w * 0.24f, h * 0.76f), Offset(w * 0.94f, h * 0.76f), strokeWidth = w * 0.08f)
            }
            "lock" -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.16f, h * 0.44f),
                    size = Size(w * 0.68f, h * 0.46f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.1f),
                    style = stroke
                )
                drawArc(
                    color = color,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.3f, h * 0.14f),
                    size = Size(w * 0.4f, h * 0.4f),
                    style = Stroke(width = w * 0.09f)
                )
            }
        }
    }
}

private fun starPath(w: Float, h: Float): Path = Path().apply {
    val cx = w / 2f
    val cy = h / 2f
    val outer = w * 0.46f
    val inner = outer * 0.42f
    for (i in 0 until 10) {
        val radius = if (i % 2 == 0) outer else inner
        val angle = (-90.0 + i * 36.0) * Math.PI / 180.0
        val x = cx + (radius * Math.cos(angle)).toFloat()
        val y = cy + (radius * Math.sin(angle)).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}
