package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Indigo = Color(0xFF3F51B5)
val IndigoDark = Color(0xFF303F9F)
val Pink = Color(0xFFE91E63)
val DarkBg = Color(0xFF212121)
val SheetGray = Color(0xFFEBEBEB)
val VinylGray = Color(0xFF4A4A4A)
val GreenCover = Color(0xFF3E6B46)

// ---------- 自绘图标 ----------

@Composable
fun PlayGlyph(modifier: Modifier = Modifier, color: Color = Color.White, playing: Boolean) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        if (playing) {
            // 暂停: 两个竖条
            drawRect(color, topLeft = Offset(w * 0.22f, h * 0.15f), size = Size(w * 0.2f, h * 0.7f))
            drawRect(color, topLeft = Offset(w * 0.58f, h * 0.15f), size = Size(w * 0.2f, h * 0.7f))
        } else {
            // 播放三角
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.28f, h * 0.16f)
                lineTo(w * 0.8f, h * 0.5f)
                lineTo(w * 0.28f, h * 0.84f)
                close()
            }
            drawPath(path, color)
        }
    }
}

@Composable
fun SkipGlyph(modifier: Modifier = Modifier, color: Color = Color.White, next: Boolean) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        if (next) {
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.18f, h * 0.2f); lineTo(w * 0.6f, h * 0.5f); lineTo(w * 0.18f, h * 0.8f); close()
            }
            drawPath(path, color)
            drawRect(color, topLeft = Offset(w * 0.66f, h * 0.2f), size = Size(w * 0.14f, h * 0.6f))
        } else {
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.82f, h * 0.2f); lineTo(w * 0.4f, h * 0.5f); lineTo(w * 0.82f, h * 0.8f); close()
            }
            drawPath(path, color)
            drawRect(color, topLeft = Offset(w * 0.2f, h * 0.2f), size = Size(w * 0.14f, h * 0.6f))
        }
    }
}

@Composable
fun ShuffleGlyph(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val sw = w * 0.07f
        // 两条交叉曲线
        drawLine(color, Offset(w * 0.1f, h * 0.25f), Offset(w * 0.35f, h * 0.25f), strokeWidth = sw)
        drawLine(color, Offset(w * 0.35f, h * 0.25f), Offset(w * 0.62f, h * 0.72f), strokeWidth = sw)
        drawLine(color, Offset(w * 0.62f, h * 0.72f), Offset(w * 0.8f, h * 0.72f), strokeWidth = sw)
        drawLine(color, Offset(w * 0.1f, h * 0.72f), Offset(w * 0.35f, h * 0.72f), strokeWidth = sw)
        drawLine(color, Offset(w * 0.35f, h * 0.72f), Offset(w * 0.62f, h * 0.25f), strokeWidth = sw)
        drawLine(color, Offset(w * 0.62f, h * 0.25f), Offset(w * 0.8f, h * 0.25f), strokeWidth = sw)
        // 箭头
        val a1 = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.92f, h * 0.25f); lineTo(w * 0.74f, h * 0.13f); lineTo(w * 0.74f, h * 0.37f); close()
        }
        drawPath(a1, color)
        val a2 = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.92f, h * 0.72f); lineTo(w * 0.74f, h * 0.6f); lineTo(w * 0.74f, h * 0.84f); close()
        }
        drawPath(a2, color)
    }
}

@Composable
fun RepeatGlyph(modifier: Modifier = Modifier, color: Color, one: Boolean = false) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val sw = w * 0.07f
        // 上箭头(向右) 与 下箭头(向左)
        drawLine(color, Offset(w * 0.25f, h * 0.32f), Offset(w * 0.78f, h * 0.32f), strokeWidth = sw)
        drawLine(color, Offset(w * 0.25f, h * 0.32f), Offset(w * 0.25f, h * 0.55f), strokeWidth = sw)
        val a1 = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.9f, h * 0.32f); lineTo(w * 0.72f, h * 0.2f); lineTo(w * 0.72f, h * 0.44f); close()
        }
        drawPath(a1, color)
        drawLine(color, Offset(w * 0.75f, h * 0.68f), Offset(w * 0.22f, h * 0.68f), strokeWidth = sw)
        drawLine(color, Offset(w * 0.75f, h * 0.68f), Offset(w * 0.75f, h * 0.45f), strokeWidth = sw)
        val a2 = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.1f, h * 0.68f); lineTo(w * 0.28f, h * 0.56f); lineTo(w * 0.28f, h * 0.8f); close()
        }
        drawPath(a2, color)
    }
    if (one) {
        Text("1", style = TextStyle(color = color, fontSize = 9.sp),
            modifier = modifier)
    }
}

@Composable
fun HeartGlyph(modifier: Modifier = Modifier, color: Color, filled: Boolean) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.5f, h * 0.85f)
            cubicTo(w * 0.1f, h * 0.55f, w * 0.05f, h * 0.25f, w * 0.25f, h * 0.14f)
            cubicTo(w * 0.4f, h * 0.06f, w * 0.5f, h * 0.2f, w * 0.5f, h * 0.3f)
            cubicTo(w * 0.5f, h * 0.2f, w * 0.6f, h * 0.06f, w * 0.75f, h * 0.14f)
            cubicTo(w * 0.95f, h * 0.25f, w * 0.9f, h * 0.55f, w * 0.5f, h * 0.85f)
            close()
        }
        if (filled) drawPath(path, color) else drawPath(path, color, style = Stroke(w * 0.08f))
    }
}

// ---------- 封面与黑胶 ----------

@Composable
fun CoverArt(song: Song?, size: Dp, modifier: Modifier = Modifier) {
    val bg = if (song?.greenCover == true) GreenCover else Color.Black
    Box(modifier.size(size).background(bg)) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.toPx() * 0.30f
            val c = center
            drawCircle(VinylGray, r, c)
            drawCircle(Color(0xFF5C5C5C), r * 0.72f, c, style = Stroke(2f))
            drawCircle(Color(0xFF5C5C5C), r * 0.5f, c, style = Stroke(2f))
            drawCircle(Color(0xFF3A3A3A), r * 0.28f, c)
            drawCircle(Color(0xFF666666), r * 0.1f, c)
        }
    }
}

@Composable
fun VinylDisc(rotation: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val r = size.minDimension / 2
        val c = center
        drawCircle(VinylGray, r, c)
        // 沟槽
        for (f in listOf(0.92f, 0.8f, 0.68f, 0.56f)) {
            drawCircle(Color(0xFF5A5A5A), r * f, c, style = Stroke(3f))
        }
        drawCircle(Color(0xFF3A3A3A), r * 0.38f, c)
        drawCircle(Color(0xFF6A6A6A), r * 0.12f, c)
        // 旋转标记
        rotate(rotation, c) {
            drawLine(Color(0xFF7A7A7A), Offset(c.x, c.y - r * 0.5f), Offset(c.x, c.y - r * 0.85f), strokeWidth = 6f)
        }
    }
}

// ---------- 通用列表行 ----------

@Composable
fun SongRow(
    song: Song,
    modifier: Modifier = Modifier,
    titleBold: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null
) {
    Row(
        modifier.fillMaxWidth().height(72.dp).padding(horizontal = 16.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        if (leading != null) { leading(); Spacer(Modifier.width(12.dp)) } else {
            CoverArt(song, 48.dp); Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(song.title, fontSize = 16.sp, maxLines = 1,
                style = if (titleBold) LocalTextStyle.current.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                else LocalTextStyle.current)
            val sub = if (song.album.isEmpty()) song.artist else "${song.artist} · ${song.album}"
            Text(sub, fontSize = 13.sp, color = Color.Gray, maxLines = 1)
        }
        trailing?.invoke()
    }
}

@Composable
fun MenuItemText(text: String, color: Color = Color.Unspecified) {
    Text(text, fontSize = 15.sp, color = color)
}

fun ImageVector?.dummy() = Unit
