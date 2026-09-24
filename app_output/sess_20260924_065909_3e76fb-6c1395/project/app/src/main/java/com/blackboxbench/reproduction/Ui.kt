package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Colour palette of the reproduced app; switches with the appearance setting. */
data class Palette(
    val dark: Boolean,
    val background: Color,
    val surface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val divider: Color,
    val primary: Color,
    val chipBg: Color,
    val barBg: Color,
    val scrim: Color
)

val LightPalette = Palette(
    dark = false,
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF3C4043),
    textSecondary = Color(0xFF8A8A8E),
    divider = Color(0xFFE4E4E7),
    primary = Color(0xFF2E6DB4),
    chipBg = Color(0xFFEFEFF1),
    barBg = Color(0xFFF4F2F9),
    scrim = Color(0x99000000)
)

val DarkPalette = Palette(
    dark = true,
    background = Color(0xFF121317),
    surface = Color(0xFF1C1D22),
    textPrimary = Color(0xFFE6E6E9),
    textSecondary = Color(0xFF9A9AA0),
    divider = Color(0xFF2E2F35),
    primary = Color(0xFF8AB4F8),
    chipBg = Color(0xFF2A2B31),
    barBg = Color(0xFF1A1B20),
    scrim = Color(0xAA000000)
)

fun chipColor(p: Palette, c: Long): Color = if (p.dark) lighten(Color(c)) else Color(c)

private fun lighten(c: Color): Color = Color(
    red = (c.red * 0.6f + 0.35f).coerceIn(0f, 1f),
    green = (c.green * 0.6f + 0.35f).coerceIn(0f, 1f),
    blue = (c.blue * 0.6f + 0.35f).coerceIn(0f, 1f)
)

// ------------------------------------------------------------------ icons

@Composable
fun RepeatIcon(color: Color, size: androidx.compose.ui.unit.Dp = 22.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round)
        drawArc(
            color = color, startAngle = 200f, sweepAngle = 140f, useCenter = false,
            topLeft = Offset(w * 0.08f, h * 0.12f), size = Size(w * 0.84f, h * 0.5f), style = stroke
        )
        drawArc(
            color = color, startAngle = 20f, sweepAngle = 140f, useCenter = false,
            topLeft = Offset(w * 0.08f, h * 0.38f), size = Size(w * 0.84f, h * 0.5f), style = stroke
        )
        val p1 = Path().apply {
            moveTo(w * 0.10f, h * 0.20f); lineTo(w * 0.10f, h * 0.44f); lineTo(w * 0.32f, h * 0.32f); close()
        }
        val p2 = Path().apply {
            moveTo(w * 0.90f, h * 0.80f); lineTo(w * 0.90f, h * 0.56f); lineTo(w * 0.68f, h * 0.68f); close()
        }
        drawPath(p1, color)
        drawPath(p2, color)
    }
}

@Composable
fun RepeatArrowIcon(color: Color, size: androidx.compose.ui.unit.Dp = 24.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round)
        drawArc(
            color = color, startAngle = 190f, sweepAngle = 130f, useCenter = false,
            topLeft = Offset(w * 0.18f, h * 0.22f), size = Size(w * 0.6f, h * 0.46f), style = stroke
        )
        drawArc(
            color = color, startAngle = 10f, sweepAngle = 130f, useCenter = false,
            topLeft = Offset(w * 0.22f, h * 0.34f), size = Size(w * 0.6f, h * 0.46f), style = stroke
        )
        val p1 = Path().apply {
            moveTo(w * 0.14f, h * 0.42f); lineTo(w * 0.36f, h * 0.42f); lineTo(w * 0.25f, h * 0.22f); close()
        }
        val p2 = Path().apply {
            moveTo(w * 0.86f, h * 0.58f); lineTo(w * 0.64f, h * 0.58f); lineTo(w * 0.75f, h * 0.78f); close()
        }
        drawPath(p1, color)
        drawPath(p2, color)
    }
}

@Composable
fun MicIcon(color: Color, size: androidx.compose.ui.unit.Dp = 24.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.35f, h * 0.12f),
            size = Size(w * 0.30f, h * 0.46f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.15f),
            style = Stroke(width = w * 0.09f)
        )
        drawArc(
            color = color, startAngle = 0f, sweepAngle = 180f, useCenter = false,
            topLeft = Offset(w * 0.22f, h * 0.40f), size = Size(w * 0.56f, h * 0.34f),
            style = Stroke(width = w * 0.09f, cap = StrokeCap.Round)
        )
        drawLine(color, Offset(w * 0.5f, h * 0.76f), Offset(w * 0.5f, h * 0.90f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
    }
}

@Composable
fun SortIcon(color: Color, size: androidx.compose.ui.unit.Dp = 24.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val s = Stroke(width = w * 0.09f, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.32f, h * 0.20f), Offset(w * 0.32f, h * 0.82f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.68f, h * 0.20f), Offset(w * 0.68f, h * 0.82f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
        val up = Path().apply {
            moveTo(w * 0.32f, h * 0.14f); lineTo(w * 0.20f, h * 0.30f); lineTo(w * 0.44f, h * 0.30f); close()
        }
        val down = Path().apply {
            moveTo(w * 0.68f, h * 0.88f); lineTo(w * 0.56f, h * 0.72f); lineTo(w * 0.80f, h * 0.72f); close()
        }
        drawPath(up, color)
        drawPath(down, color)
    }
}

@Composable
fun FlagIcon(color: Color, size: androidx.compose.ui.unit.Dp = 22.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawLine(color, Offset(w * 0.26f, h * 0.10f), Offset(w * 0.26f, h * 0.90f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
        val p = Path().apply {
            moveTo(w * 0.30f, h * 0.14f); lineTo(w * 0.82f, h * 0.26f); lineTo(w * 0.30f, h * 0.46f); close()
        }
        drawPath(p, color, style = Stroke(width = w * 0.08f))
    }
}

@Composable
fun TagIcon(color: Color, size: androidx.compose.ui.unit.Dp = 20.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val p = Path().apply {
            moveTo(w * 0.10f, h * 0.30f)
            lineTo(w * 0.50f, h * 0.10f)
            lineTo(w * 0.90f, h * 0.50f)
            lineTo(w * 0.50f, h * 0.90f)
            close()
        }
        drawPath(p, color, style = Stroke(width = w * 0.09f))
        drawCircle(color, radius = w * 0.07f, center = Offset(w * 0.44f, h * 0.32f))
    }
}

@Composable
fun LabelIcon(color: Color, size: androidx.compose.ui.unit.Dp = 24.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val p = Path().apply {
            moveTo(w * 0.10f, h * 0.34f)
            lineTo(w * 0.62f, h * 0.10f)
            lineTo(w * 0.90f, h * 0.38f)
            lineTo(w * 0.62f, h * 0.66f)
            lineTo(w * 0.10f, h * 0.66f)
            close()
        }
        drawPath(p, color, style = Stroke(width = w * 0.09f))
    }
}

@Composable
fun ClockIcon(color: Color, size: androidx.compose.ui.unit.Dp = 24.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawCircle(color, radius = w * 0.40f, center = Offset(w / 2, h / 2), style = Stroke(width = w * 0.09f))
        drawLine(color, Offset(w / 2, h / 2), Offset(w / 2, h * 0.30f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
        drawLine(color, Offset(w / 2, h / 2), Offset(w * 0.68f, h * 0.58f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
    }
}

@Composable
fun CalendarStartIcon(color: Color, size: androidx.compose.ui.unit.Dp = 24.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawRoundRect(
            color = color, topLeft = Offset(w * 0.12f, h * 0.16f), size = Size(w * 0.66f, h * 0.60f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.10f), style = Stroke(width = w * 0.08f)
        )
        drawLine(color, Offset(w * 0.12f, h * 0.38f), Offset(w * 0.78f, h * 0.38f), strokeWidth = w * 0.08f)
        val p = Path().apply {
            moveTo(w * 0.92f, h * 0.56f); lineTo(w * 0.70f, h * 0.56f); lineTo(w * 0.81f, h * 0.84f); close()
        }
        drawPath(p, color)
    }
}

@Composable
fun SubtaskIcon(color: Color, size: androidx.compose.ui.unit.Dp = 22.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val s = Stroke(width = w * 0.09f, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.24f, h * 0.14f), Offset(w * 0.24f, h * 0.60f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
        drawArc(color, 180f, 90f, false, Offset(w * 0.24f, h * 0.40f), Size(w * 0.40f, h * 0.40f), style = s)
        val p = Path().apply {
            moveTo(w * 0.60f, h * 0.84f); lineTo(w * 0.82f, h * 0.60f); lineTo(w * 0.38f, h * 0.60f); close()
        }
        drawPath(p, color)
    }
}

@Composable
fun AttachmentIcon(color: Color, size: androidx.compose.ui.unit.Dp = 22.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = w * 0.08f, cap = StrokeCap.Round)
        drawArc(color, 200f, 200f, false, Offset(w * 0.18f, h * 0.30f), Size(w * 0.64f, h * 0.55f), style = stroke)
        drawArc(color, 20f, 200f, false, Offset(w * 0.30f, h * 0.18f), Size(w * 0.40f, h * 0.55f), style = stroke)
        drawLine(color, Offset(w * 0.30f, h * 0.46f), Offset(w * 0.30f, h * 0.72f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.50f, h * 0.46f), Offset(w * 0.50f, h * 0.66f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
    }
}

@Composable
fun PaletteIcon(color: Color, size: androidx.compose.ui.unit.Dp = 24.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawCircle(color, radius = w * 0.38f, center = Offset(w / 2, h / 2), style = Stroke(width = w * 0.09f))
        drawCircle(color, radius = w * 0.06f, center = Offset(w * 0.36f, h * 0.38f))
        drawCircle(color, radius = w * 0.06f, center = Offset(w * 0.64f, h * 0.38f))
        drawCircle(color, radius = w * 0.06f, center = Offset(w * 0.50f, h * 0.68f))
    }
}

@Composable
fun FilterIcon(color: Color, size: androidx.compose.ui.unit.Dp = 24.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val s = Stroke(width = w * 0.09f, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.12f, h * 0.28f), Offset(w * 0.88f, h * 0.28f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.24f, h * 0.52f), Offset(w * 0.76f, h * 0.52f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.38f, h * 0.76f), Offset(w * 0.62f, h * 0.76f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
    }
}

@Composable
fun SearchGlyph(color: Color, size: androidx.compose.ui.unit.Dp = 24.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawCircle(color, radius = w * 0.30f, center = Offset(w * 0.42f, h * 0.42f), style = Stroke(width = w * 0.10f))
        drawLine(color, Offset(w * 0.64f, h * 0.64f), Offset(w * 0.88f, h * 0.88f), strokeWidth = w * 0.10f, cap = StrokeCap.Round)
    }
}

@Composable
fun SaveIcon(color: Color, size: androidx.compose.ui.unit.Dp = 24.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val s = Stroke(width = w * 0.09f, cap = StrokeCap.Round)
        drawRoundRect(
            color = color, topLeft = Offset(w * 0.14f, h * 0.14f), size = Size(w * 0.72f, h * 0.72f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.12f), style = s
        )
        drawLine(color, Offset(w * 0.50f, h * 0.24f), Offset(w * 0.50f, h * 0.60f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
        val p = Path().apply {
            moveTo(w * 0.34f, h * 0.46f); lineTo(w * 0.50f, h * 0.66f); lineTo(w * 0.66f, h * 0.46f); close()
        }
        drawPath(p, color)
    }
}

@Composable
fun HomeGlyph(color: Color, size: androidx.compose.ui.unit.Dp = 24.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val p = Path().apply {
            moveTo(w * 0.12f, h * 0.50f); lineTo(w * 0.50f, h * 0.16f); lineTo(w * 0.88f, h * 0.50f)
        }
        drawPath(p, color, style = Stroke(width = w * 0.09f, cap = StrokeCap.Round))
        drawRoundRect(
            color = color, topLeft = Offset(w * 0.24f, h * 0.50f), size = Size(w * 0.52f, h * 0.36f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.06f), style = Stroke(width = w * 0.09f)
        )
    }
}

@Composable
fun GridGlyph(color: Color, size: androidx.compose.ui.unit.Dp = 24.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val s = Stroke(width = w * 0.08f)
        drawRoundRect(color, Offset(w * 0.14f, h * 0.14f), Size(w * 0.28f, h * 0.28f), androidx.compose.ui.geometry.CornerRadius(w * 0.05f), style = s)
        drawRoundRect(color, Offset(w * 0.58f, h * 0.14f), Size(w * 0.28f, h * 0.28f), androidx.compose.ui.geometry.CornerRadius(w * 0.05f), style = s)
        drawRoundRect(color, Offset(w * 0.14f, h * 0.58f), Size(w * 0.28f, h * 0.28f), androidx.compose.ui.geometry.CornerRadius(w * 0.05f), style = s)
        drawRoundRect(color, Offset(w * 0.58f, h * 0.58f), Size(w * 0.28f, h * 0.28f), androidx.compose.ui.geometry.CornerRadius(w * 0.05f), style = s)
    }
}

@Composable
fun DonateGlyph(color: Color, size: androidx.compose.ui.unit.Dp = 24.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val p = Path().apply {
            moveTo(w * 0.50f, h * 0.84f)
            cubicTo(w * 0.10f, h * 0.56f, w * 0.16f, h * 0.18f, w * 0.50f, h * 0.34f)
            cubicTo(w * 0.84f, h * 0.18f, w * 0.90f, h * 0.56f, w * 0.50f, h * 0.84f)
            close()
        }
        drawPath(p, color, style = Stroke(width = w * 0.09f))
    }
}

@Composable
fun CheckGlyph(color: Color, size: androidx.compose.ui.unit.Dp = 20.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val p = Path().apply {
            moveTo(w * 0.18f, h * 0.52f); lineTo(w * 0.42f, h * 0.76f); lineTo(w * 0.84f, h * 0.24f)
        }
        drawPath(p, color, style = Stroke(width = w * 0.14f, cap = StrokeCap.Round))
    }
}

@Composable
fun ChevronUp(color: Color, size: androidx.compose.ui.unit.Dp = 18.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val p = Path().apply {
            moveTo(w * 0.18f, h * 0.66f); lineTo(w * 0.50f, h * 0.32f); lineTo(w * 0.82f, h * 0.66f)
        }
        drawPath(p, color, style = Stroke(width = w * 0.13f, cap = StrokeCap.Round))
    }
}

@Composable
fun ChevronRight(color: Color, size: androidx.compose.ui.unit.Dp = 18.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val p = Path().apply {
            moveTo(w * 0.34f, h * 0.18f); lineTo(w * 0.68f, h * 0.50f); lineTo(w * 0.34f, h * 0.82f)
        }
        drawPath(p, color, style = Stroke(width = w * 0.13f, cap = StrokeCap.Round))
    }
}

@Composable
fun DotsGlyph(color: Color, size: androidx.compose.ui.unit.Dp = 24.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawCircle(color, w * 0.07f, Offset(w / 2, h * 0.24f))
        drawCircle(color, w * 0.07f, Offset(w / 2, h * 0.50f))
        drawCircle(color, w * 0.07f, Offset(w / 2, h * 0.76f))
    }
}

@Composable
fun PlaceGlyph(color: Color, size: androidx.compose.ui.unit.Dp = 24.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val p = Path().apply {
            moveTo(w * 0.50f, h * 0.90f)
            cubicTo(w * 0.16f, h * 0.56f, w * 0.20f, h * 0.12f, w * 0.50f, h * 0.12f)
            cubicTo(w * 0.80f, h * 0.12f, w * 0.84f, h * 0.56f, w * 0.50f, h * 0.90f)
            close()
        }
        drawPath(p, color, style = Stroke(width = w * 0.09f))
        drawCircle(color, w * 0.11f, Offset(w / 2, h * 0.38f))
    }
}

@Composable
fun IconCircle(color: Color, size: androidx.compose.ui.unit.Dp = 24.dp, content: @Composable () -> Unit) {
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) { content() }
}

/** Small circled badge used for the drawer's coloured list icons. */
@Composable
fun GlyphBox(color: Color, text: String, size: androidx.compose.ui.unit.Dp = 22.dp) {
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Text(text = text, color = color, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}
