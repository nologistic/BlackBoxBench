package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private fun DrawScope.line(tint: Color, w: Float, x1: Float, y1: Float, x2: Float, y2: Float) =
    drawLine(tint, Offset(this.size.minDimension * x1, this.size.minDimension * y1), Offset(this.size.minDimension * x2, this.size.minDimension * y2), w, StrokeCap.Round)

private fun DrawScope.poly(tint: Color, w: Float, filled: Boolean, vararg points: Pair<Float, Float>) {
    val s = this.size.minDimension
    val path = Path()
    points.forEachIndexed { index, p ->
        if (index == 0) path.moveTo(s * p.first, s * p.second) else path.lineTo(s * p.first, s * p.second)
    }
    path.close()
    drawPath(path, tint, style = if (filled) Fill else Stroke(w, join = StrokeJoin.Round))
}

@Composable
private fun Glyph(
    size: Dp = 22.dp,
    tint: Color = LocalContentColor.current,
    stroke: Dp = 1.7.dp,
    draw: DrawScope.(Color, Float) -> Unit,
) {
    Canvas(modifier = Modifier.size(size)) { draw(tint, stroke.toPx()) }
}

@Composable
fun IcoAdd(size: Dp = 24.dp, tint: Color = LocalContentColor.current) = Glyph(size, tint) { c, w ->
    line(c, w, 0.5f, 0.16f, 0.5f, 0.84f); line(c, w, 0.16f, 0.5f, 0.84f, 0.5f)
}

@Composable
fun IcoClose(size: Dp = 22.dp, tint: Color = LocalContentColor.current) = Glyph(size, tint) { c, w ->
    line(c, w, 0.22f, 0.22f, 0.78f, 0.78f); line(c, w, 0.78f, 0.22f, 0.22f, 0.78f)
}

@Composable
fun IcoCheck(size: Dp = 22.dp, tint: Color = LocalContentColor.current) = Glyph(size, tint) { c, w ->
    line(c, w, 0.2f, 0.55f, 0.42f, 0.76f); line(c, w, 0.42f, 0.76f, 0.82f, 0.26f)
}

@Composable
fun IcoArrowLeft(size: Dp = 24.dp, tint: Color = LocalContentColor.current) = Glyph(size, tint) { c, w ->
    line(c, w, 0.82f, 0.5f, 0.2f, 0.5f); line(c, w, 0.44f, 0.26f, 0.2f, 0.5f); line(c, w, 0.44f, 0.74f, 0.2f, 0.5f)
}

@Composable
fun IcoArrowRight(size: Dp = 24.dp, tint: Color = LocalContentColor.current) = Glyph(size, tint) { c, w ->
    line(c, w, 0.18f, 0.5f, 0.8f, 0.5f); line(c, w, 0.56f, 0.26f, 0.8f, 0.5f); line(c, w, 0.56f, 0.74f, 0.8f, 0.5f)
}

@Composable
fun IcoChevronUp(size: Dp = 22.dp, tint: Color = LocalContentColor.current) = Glyph(size, tint) { c, w ->
    line(c, w, 0.24f, 0.62f, 0.5f, 0.36f); line(c, w, 0.5f, 0.36f, 0.76f, 0.62f)
}

@Composable
fun IcoChevronDown(size: Dp = 22.dp, tint: Color = LocalContentColor.current) = Glyph(size, tint) { c, w ->
    line(c, w, 0.24f, 0.40f, 0.5f, 0.66f); line(c, w, 0.5f, 0.66f, 0.76f, 0.40f)
}

@Composable
fun IcoChevronRight(size: Dp = 20.dp, tint: Color = LocalContentColor.current) = Glyph(size, tint) { c, w ->
    line(c, w, 0.40f, 0.24f, 0.66f, 0.5f); line(c, w, 0.66f, 0.5f, 0.40f, 0.76f)
}

@Composable
fun IcoMore(size: Dp = 24.dp, tint: Color = LocalContentColor.current) = Glyph(size, tint) { c, _ ->
    listOf(0.22f, 0.5f, 0.78f).forEach { y -> drawCircle(c, this.size.minDimension * 0.075f, Offset(this.size.minDimension * 0.5f, this.size.minDimension * y)) }
}

@Composable
fun IcoSearch(size: Dp = 24.dp, tint: Color = LocalContentColor.current) = Glyph(size, tint) { c, w ->
    drawCircle(c, this.size.minDimension * 0.28f, Offset(this.size.minDimension * 0.44f, this.size.minDimension * 0.42f), style = Stroke(w))
    line(c, w, 0.65f, 0.64f, 0.84f, 0.84f)
}

@Composable
fun IcoSort(size: Dp = 24.dp, tint: Color = LocalContentColor.current) = Glyph(size, tint) { c, w ->
    line(c, w, 0.16f, 0.28f, 0.84f, 0.28f); line(c, w, 0.16f, 0.5f, 0.62f, 0.5f); line(c, w, 0.16f, 0.72f, 0.42f, 0.72f)
}

@Composable
fun IcoFolder(size: Dp = 26.dp, tint: Color = LocalContentColor.current, filled: Boolean = false) = Glyph(size, tint) { c, w ->
    val p = Path()
    val s = this.size.minDimension
    p.moveTo(s * 0.12f, s * 0.80f)
    p.lineTo(s * 0.12f, s * 0.24f)
    p.lineTo(s * 0.42f, s * 0.24f)
    p.lineTo(s * 0.50f, s * 0.36f)
    p.lineTo(s * 0.88f, s * 0.36f)
    p.lineTo(s * 0.88f, s * 0.80f)
    p.close()
    drawPath(p, c, style = if (filled) Fill else Stroke(w, join = StrokeJoin.Round))
}

@Composable
fun IcoDoc(size: Dp = 26.dp, tint: Color = LocalContentColor.current) = Glyph(size, tint) { c, w ->
    val s = this.size.minDimension
    val p = Path()
    p.moveTo(s * 0.24f, s * 0.10f)
    p.lineTo(s * 0.60f, s * 0.10f)
    p.lineTo(s * 0.78f, s * 0.30f)
    p.lineTo(s * 0.78f, s * 0.90f)
    p.lineTo(s * 0.24f, s * 0.90f)
    p.close()
    drawPath(p, c, style = Stroke(w, join = StrokeJoin.Round))
    line(c, w, 0.60f, 0.10f, 0.60f, 0.30f)
    line(c, w, 0.60f, 0.30f, 0.78f, 0.30f)
}

@Composable
fun IcoCheckSquare(size: Dp = 26.dp, tint: Color = LocalContentColor.current, filled: Boolean = true) = Glyph(size, tint) { c, w ->
    val s = this.size.minDimension
    if (filled) {
        drawRoundRect(c, Offset(s * 0.14f, s * 0.14f), Size(s * 0.72f, s * 0.72f), CornerRadius(s * 0.14f))
        drawLine(Color.White, Offset(s * 0.30f, s * 0.52f), Offset(s * 0.45f, s * 0.68f), w * 1.3f, StrokeCap.Round)
        drawLine(Color.White, Offset(s * 0.45f, s * 0.68f), Offset(s * 0.72f, s * 0.34f), w * 1.3f, StrokeCap.Round)
    } else {
        drawRoundRect(c, Offset(s * 0.14f, s * 0.14f), Size(s * 0.72f, s * 0.72f), CornerRadius(s * 0.14f), style = Stroke(w))
        drawLine(c, Offset(s * 0.30f, s * 0.52f), Offset(s * 0.45f, s * 0.68f), w, StrokeCap.Round)
        drawLine(c, Offset(s * 0.45f, s * 0.68f), Offset(s * 0.72f, s * 0.34f), w, StrokeCap.Round)
    }
}

@Composable
fun IcoBolt(size: Dp = 26.dp, tint: Color = LocalContentColor.current, filled: Boolean = true) = Glyph(size, tint) { c, w ->
    poly(c, w, filled, 0.60f to 0.06f, 0.22f to 0.56f, 0.45f to 0.56f, 0.38f to 0.94f, 0.78f to 0.42f, 0.53f to 0.42f)
}

@Composable
fun IcoStar(size: Dp = 24.dp, tint: Color = LocalContentColor.current, filled: Boolean = false) = Glyph(size, tint) { c, w ->
    val s = this.size.minDimension
    val path = Path()
    val cx = s / 2f
    val cy = s / 2f
    val outer = s * 0.46f
    val inner = s * 0.20f
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) outer else inner
        val a = (-90.0 + i * 36.0) * PI / 180.0
        val x = cx + (r * cos(a)).toFloat()
        val y = cy + (r * sin(a)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, c, style = if (filled) Fill else Stroke(w, join = StrokeJoin.Round))
}

@Composable
fun IcoUndo(size: Dp = 24.dp, tint: Color = LocalContentColor.current) = Glyph(size, tint) { c, w ->
    val s = this.size.minDimension
    drawArc(c, 150f, 220f, false, Offset(s * 0.18f, s * 0.26f), Size(s * 0.64f, s * 0.56f), style = Stroke(w, cap = StrokeCap.Round))
    line(c, w, 0.20f, 0.34f, 0.14f, 0.58f)
    line(c, w, 0.20f, 0.34f, 0.44f, 0.36f)
}

@Composable
fun IcoRedo(size: Dp = 24.dp, tint: Color = LocalContentColor.current) = Glyph(size, tint) { c, w ->
    val s = this.size.minDimension
    drawArc(c, 170f, 220f, false, Offset(s * 0.18f, s * 0.26f), Size(s * 0.64f, s * 0.56f), style = Stroke(w, cap = StrokeCap.Round))
    line(c, w, 0.80f, 0.34f, 0.86f, 0.58f)
    line(c, w, 0.80f, 0.34f, 0.56f, 0.36f)
}

@Composable
fun IcoSave(size: Dp = 24.dp, tint: Color = LocalContentColor.current) = Glyph(size, tint) { c, w ->
    val s = this.size.minDimension
    drawRoundRect(c, Offset(s * 0.14f, s * 0.14f), Size(s * 0.72f, s * 0.72f), CornerRadius(s * 0.08f), style = Stroke(w))
    drawRect(c, Offset(s * 0.34f, s * 0.14f), Size(s * 0.32f, s * 0.22f), style = Stroke(w))
    drawRect(c, Offset(s * 0.28f, s * 0.52f), Size(s * 0.44f, s * 0.34f), style = Stroke(w))
}

@Composable
fun IcoEye(size: Dp = 24.dp, tint: Color = LocalContentColor.current) = Glyph(size, tint) { c, w ->
    val s = this.size.minDimension
    val p = Path()
    p.moveTo(s * 0.08f, s * 0.5f)
    p.quadraticBezierTo(s * 0.5f, s * 0.14f, s * 0.92f, s * 0.5f)
    p.quadraticBezierTo(s * 0.5f, s * 0.86f, s * 0.08f, s * 0.5f)
    p.close()
    drawPath(p, c, style = Stroke(w, join = StrokeJoin.Round))
    drawCircle(c, s * 0.13f, Offset(s * 0.5f, s * 0.5f))
}

@Composable
fun IcoTrash(size: Dp = 24.dp, tint: Color = LocalContentColor.current) = Glyph(size, tint) { c, w ->
    val s = this.size.minDimension
    line(c, w, 0.14f, 0.26f, 0.86f, 0.26f)
    line(c, w, 0.38f, 0.16f, 0.62f, 0.16f)
    line(c, w, 0.22f, 0.30f, 0.28f, 0.88f)
    line(c, w, 0.78f, 0.30f, 0.72f, 0.88f)
    line(c, w, 0.28f, 0.88f, 0.72f, 0.88f)
    line(c, w, 0.44f, 0.42f, 0.44f, 0.76f)
    line(c, w, 0.58f, 0.42f, 0.58f, 0.76f)
}

@Composable
fun IcoInfo(size: Dp = 24.dp, tint: Color = LocalContentColor.current) = Glyph(size, tint) { c, w ->
    val s = this.size.minDimension
    drawCircle(c, s * 0.40f, Offset(s * 0.5f, s * 0.5f), style = Stroke(w))
    drawCircle(c, s * 0.055f, Offset(s * 0.5f, s * 0.32f))
    line(c, w, 0.5f, 0.46f, 0.5f, 0.70f)
}

@Composable
fun IcoGear(size: Dp = 24.dp, tint: Color = LocalContentColor.current) = Glyph(size, tint) { c, w ->
    val s = this.size.minDimension
    val cx = s * 0.5f
    val cy = s * 0.5f
    drawCircle(c, s * 0.22f, Offset(cx, cy), style = Stroke(w))
    drawCircle(c, s * 0.07f, Offset(cx, cy), style = Stroke(w))
    repeat(8) { i ->
        val a = i * 45.0 * PI / 180.0
        val c1 = cos(a).toFloat()
        val s1 = sin(a).toFloat()
        drawLine(c, Offset(cx + c1 * s * 0.26f, cy + s1 * s * 0.26f), Offset(cx + c1 * s * 0.42f, cy + s1 * s * 0.42f), w, StrokeCap.Round)
    }
}

@Composable
fun IcoRefresh(size: Dp = 24.dp, tint: Color = LocalContentColor.current) = Glyph(size, tint) { c, w ->
    val s = this.size.minDimension
    drawArc(c, 40f, 280f, false, Offset(s * 0.18f, s * 0.18f), Size(s * 0.64f, s * 0.64f), style = Stroke(w, cap = StrokeCap.Round))
    line(c, w, 0.66f, 0.10f, 0.84f, 0.26f)
    line(c, w, 0.66f, 0.10f, 0.62f, 0.34f)
}

@Composable
fun IcoShare(size: Dp = 24.dp, tint: Color = LocalContentColor.current) = Glyph(size, tint) { c, w ->
    val s = this.size.minDimension
    drawCircle(c, s * 0.11f, Offset(s * 0.74f, s * 0.22f), style = Stroke(w))
    drawCircle(c, s * 0.11f, Offset(s * 0.26f, s * 0.5f), style = Stroke(w))
    drawCircle(c, s * 0.11f, Offset(s * 0.74f, s * 0.78f), style = Stroke(w))
    line(c, w, 0.36f, 0.44f, 0.64f, 0.28f)
    line(c, w, 0.36f, 0.56f, 0.64f, 0.72f)
}

@Composable
fun IcoPerson(size: Dp = 24.dp, tint: Color = LocalContentColor.current) = Glyph(size, tint) { c, w ->
    val s = this.size.minDimension
    drawCircle(c, s * 0.17f, Offset(s * 0.5f, s * 0.34f))
    drawArc(c, 190f, 160f, false, Offset(s * 0.22f, s * 0.56f), Size(s * 0.56f, s * 0.42f), style = Stroke(w, cap = StrokeCap.Round))
}

@Composable
fun IcoPeople(size: Dp = 24.dp, tint: Color = LocalContentColor.current) = Glyph(size, tint) { c, w ->
    val s = this.size.minDimension
    drawCircle(c, s * 0.13f, Offset(s * 0.36f, s * 0.34f))
    drawArc(c, 190f, 160f, false, Offset(s * 0.14f, s * 0.54f), Size(s * 0.44f, s * 0.34f), style = Stroke(w, cap = StrokeCap.Round))
    drawCircle(c, s * 0.11f, Offset(s * 0.70f, s * 0.36f))
    drawArc(c, 200f, 150f, false, Offset(s * 0.52f, s * 0.56f), Size(s * 0.38f, s * 0.30f), style = Stroke(w, cap = StrokeCap.Round))
}

@Composable
fun IcoClock(size: Dp = 24.dp, tint: Color = LocalContentColor.current) = Glyph(size, tint) { c, w ->
    val s = this.size.minDimension
    drawCircle(c, s * 0.40f, Offset(s * 0.5f, s * 0.5f), style = Stroke(w))
    line(c, w, 0.5f, 0.5f, 0.5f, 0.26f)
    line(c, w, 0.5f, 0.5f, 0.70f, 0.58f)
}

@Composable
fun IcoKeyboard(size: Dp = 24.dp, tint: Color = LocalContentColor.current) = Glyph(size, tint) { c, w ->
    val s = this.size.minDimension
    drawRoundRect(c, Offset(s * 0.08f, s * 0.26f), Size(s * 0.84f, s * 0.48f), CornerRadius(s * 0.08f), style = Stroke(w))
    listOf(0.24f, 0.42f, 0.58f, 0.76f).forEach { x -> drawCircle(c, s * 0.035f, Offset(s * x, s * 0.42f)) }
    line(c, w, 0.32f, 0.60f, 0.68f, 0.60f)
}

@Composable
fun IcoDownload(size: Dp = 24.dp, tint: Color = LocalContentColor.current) = Glyph(size, tint) { c, w ->
    val s = this.size.minDimension
    line(c, w, 0.5f, 0.14f, 0.5f, 0.62f)
    line(c, w, 0.30f, 0.44f, 0.50f, 0.64f)
    line(c, w, 0.70f, 0.44f, 0.50f, 0.64f)
    line(c, w, 0.20f, 0.84f, 0.80f, 0.84f)
}

@Composable
fun IcoOpenExternal(size: Dp = 24.dp, tint: Color = LocalContentColor.current) = Glyph(size, tint) { c, w ->
    val s = this.size.minDimension
    drawRoundRect(c, Offset(s * 0.16f, s * 0.30f), Size(s * 0.54f, s * 0.54f), CornerRadius(s * 0.06f), style = Stroke(w))
    line(c, w, 0.52f, 0.48f, 0.86f, 0.14f)
    line(c, w, 0.60f, 0.14f, 0.86f, 0.14f)
    line(c, w, 0.86f, 0.14f, 0.86f, 0.40f)
}

@Composable
fun IcoListBullets(size: Dp = 24.dp, tint: Color = LocalContentColor.current) = Glyph(size, tint) { c, w ->
    val s = this.size.minDimension
    listOf(0.24f, 0.5f, 0.76f).forEach { y ->
        drawCircle(c, s * 0.05f, Offset(s * 0.2f, s * y))
        line(c, w, 0.36f, y, 0.84f, y)
    }
}

@Composable
fun IcoIndent(size: Dp = 24.dp, tint: Color = LocalContentColor.current) = Glyph(size, tint) { c, w ->
    val s = this.size.minDimension
    listOf(0.26f, 0.5f, 0.74f).forEach { y -> line(c, w, 0.36f, y, 0.86f, y) }
    line(c, w, 0.14f, 0.34f, 0.14f, 0.66f)
    line(c, w, 0.14f, 0.5f, 0.28f, 0.5f)
}

@Composable
fun IcoCode(size: Dp = 24.dp, tint: Color = LocalContentColor.current) = Glyph(size, tint) { c, w ->
    line(c, w, 0.36f, 0.24f, 0.14f, 0.5f); line(c, w, 0.14f, 0.5f, 0.36f, 0.76f)
    line(c, w, 0.64f, 0.24f, 0.86f, 0.5f); line(c, w, 0.86f, 0.5f, 0.64f, 0.76f)
}

@Composable
fun IcoHome(size: Dp = 26.dp, tint: Color = LocalContentColor.current) = Glyph(size, tint) { c, w ->
    poly(c, w, false, 0.12f to 0.48f, 0.5f to 0.16f, 0.88f to 0.48f)
    line(c, w, 0.22f, 0.46f, 0.22f, 0.86f)
    line(c, w, 0.78f, 0.46f, 0.78f, 0.86f)
    line(c, w, 0.22f, 0.86f, 0.78f, 0.86f)
}

@Composable
fun IcoLock(size: Dp = 24.dp, tint: Color = LocalContentColor.current) = Glyph(size, tint) { c, w ->
    val s = this.size.minDimension
    drawRoundRect(c, Offset(s * 0.22f, s * 0.44f), Size(s * 0.56f, s * 0.42f), CornerRadius(s * 0.08f), style = Stroke(w))
    drawArc(c, 180f, 180f, false, Offset(s * 0.32f, s * 0.20f), Size(s * 0.36f, s * 0.42f), style = Stroke(w, cap = StrokeCap.Round))
}

@Composable
fun IcoPencil(size: Dp = 24.dp, tint: Color = LocalContentColor.current) = Glyph(size, tint) { c, w ->
    line(c, w, 0.20f, 0.80f, 0.72f, 0.24f)
    line(c, w, 0.64f, 0.16f, 0.84f, 0.36f)
    line(c, w, 0.20f, 0.80f, 0.26f, 0.62f)
    line(c, w, 0.20f, 0.80f, 0.38f, 0.74f)
}

@Composable
fun IcoHeart(size: Dp = 26.dp, tint: Color = LocalContentColor.current, filled: Boolean = false) = Glyph(size, tint) { c, w ->
    val s = this.size.minDimension
    val p = Path()
    p.moveTo(s * 0.5f, s * 0.86f)
    p.cubicTo(s * 0.04f, s * 0.56f, s * 0.16f, s * 0.14f, s * 0.5f, s * 0.32f)
    p.cubicTo(s * 0.84f, s * 0.14f, s * 0.96f, s * 0.56f, s * 0.5f, s * 0.86f)
    p.close()
    drawPath(p, c, style = if (filled) Fill else Stroke(w, join = StrokeJoin.Round))
}

@Composable
fun IcoCalendar(size: Dp = 24.dp, tint: Color = LocalContentColor.current) = Glyph(size, tint) { c, w ->
    val s = this.size.minDimension
    drawRoundRect(c, Offset(s * 0.14f, s * 0.22f), Size(s * 0.72f, s * 0.66f), CornerRadius(s * 0.08f), style = Stroke(w))
    line(c, w, 0.14f, 0.42f, 0.86f, 0.42f)
    line(c, w, 0.34f, 0.12f, 0.34f, 0.30f)
    line(c, w, 0.66f, 0.12f, 0.66f, 0.30f)
}

@Composable
fun IcoPalette(size: Dp = 24.dp, tint: Color = LocalContentColor.current) = Glyph(size, tint) { c, w ->
    val s = this.size.minDimension
    drawCircle(c, s * 0.40f, Offset(s * 0.5f, s * 0.5f), style = Stroke(w))
    drawCircle(c, s * 0.055f, Offset(s * 0.38f, s * 0.38f))
    drawCircle(c, s * 0.055f, Offset(s * 0.62f, s * 0.38f))
    drawCircle(c, s * 0.055f, Offset(s * 0.50f, s * 0.64f))
}

@Composable
fun IcoImage(size: Dp = 24.dp, tint: Color = LocalContentColor.current) = Glyph(size, tint) { c, w ->
    val s = this.size.minDimension
    drawRoundRect(c, Offset(s * 0.12f, s * 0.20f), Size(s * 0.76f, s * 0.60f), CornerRadius(s * 0.08f), style = Stroke(w))
    drawCircle(c, s * 0.07f, Offset(s * 0.32f, s * 0.38f))
    poly(c, w, false, 0.20f to 0.76f, 0.44f to 0.52f, 0.62f to 0.76f)
}

@Composable
fun IcoPrint(size: Dp = 24.dp, tint: Color = LocalContentColor.current) = Glyph(size, tint) { c, w ->
    val s = this.size.minDimension
    drawRoundRect(c, Offset(s * 0.16f, s * 0.40f), Size(s * 0.68f, s * 0.34f), CornerRadius(s * 0.06f), style = Stroke(w))
    drawRect(c, Offset(s * 0.30f, s * 0.16f), Size(s * 0.40f, s * 0.24f), style = Stroke(w))
    drawRect(c, Offset(s * 0.30f, s * 0.62f), Size(s * 0.40f, s * 0.26f), style = Stroke(w))
}

@Composable
fun IcoLink(size: Dp = 24.dp, tint: Color = LocalContentColor.current) = Glyph(size, tint) { c, w ->
    val s = this.size.minDimension
    drawArc(c, 40f, 140f, false, Offset(s * 0.40f, s * 0.28f), Size(s * 0.44f, s * 0.44f), style = Stroke(w, cap = StrokeCap.Round))
    drawArc(c, 220f, 140f, false, Offset(s * 0.16f, s * 0.28f), Size(s * 0.44f, s * 0.44f), style = Stroke(w, cap = StrokeCap.Round))
    line(c, w, 0.40f, 0.5f, 0.60f, 0.5f)
}

@Composable
fun IcoTranslate(size: Dp = 24.dp, tint: Color = LocalContentColor.current) = Glyph(size, tint) { c, w ->
    val s = this.size.minDimension
    line(c, w, 0.14f, 0.28f, 0.62f, 0.28f)
    line(c, w, 0.38f, 0.16f, 0.38f, 0.30f)
    line(c, w, 0.50f, 0.28f, 0.30f, 0.72f)
    line(c, w, 0.52f, 0.56f, 0.88f, 0.56f)
    line(c, w, 0.70f, 0.56f, 0.60f, 0.86f)
    line(c, w, 0.70f, 0.56f, 0.80f, 0.86f)
}

@Composable
fun IcoTextLines(size: Dp = 24.dp, tint: Color = LocalContentColor.current) = Glyph(size, tint) { c, w ->
    listOf(0.28f, 0.5f, 0.72f).forEach { y -> line(c, w, 0.20f, y, 0.80f, y) }
}

@Composable
fun IcoBackspace(size: Dp = 24.dp, tint: Color = LocalContentColor.current) = Glyph(size, tint) { c, w ->
    poly(c, w, false, 0.42f to 0.20f, 0.88f to 0.20f, 0.88f to 0.80f, 0.42f to 0.80f, 0.12f to 0.50f)
    line(c, w, 0.52f, 0.38f, 0.76f, 0.62f)
    line(c, w, 0.76f, 0.38f, 0.52f, 0.62f)
}
