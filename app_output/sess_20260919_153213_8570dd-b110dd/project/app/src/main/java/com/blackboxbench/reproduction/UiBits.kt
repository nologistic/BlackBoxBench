package com.blackboxbench.reproduction

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ThumbImage(repo: GalleryRepository, item: MediaItem, modifier: Modifier = Modifier) {
    val bmp: Bitmap? = remember(item.id) { repo.thumb(item) }
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = item.name,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(modifier = modifier.background(Color(0xFF9E9E9E)))
    }
}

@Composable
fun PinBadge(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(22.dp).background(Color(0x99000000), CircleShape)) {
        val w = size.width
        drawCircle(Color.White, radius = w * 0.16f, center = Offset(w * 0.5f, w * 0.34f))
        drawRect(
            Color.White,
            topLeft = Offset(w * 0.42f, w * 0.44f),
            size = Size(w * 0.16f, w * 0.34f)
        )
        drawLine(
            Color.White,
            start = Offset(w * 0.30f, w * 0.86f),
            end = Offset(w * 0.70f, w * 0.86f),
            strokeWidth = w * 0.09f
        )
    }
}

@Composable
fun StarBadge(modifier: Modifier = Modifier, filled: Boolean = true) {
    Canvas(modifier = modifier.size(22.dp).background(Color(0x66000000), CircleShape)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.width * 0.32f
        val path = androidx.compose.ui.graphics.Path()
        for (i in 0 until 10) {
            val angle = Math.PI / 5 * i - Math.PI / 2
            val rr = if (i % 2 == 0) r else r * 0.42f
            val x = cx + (rr * Math.cos(angle)).toFloat()
            val y = cy + (rr * Math.sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        if (filled) drawPath(path, Color.White)
        else drawPath(path, Color.White, style = Stroke(width = size.width * 0.07f))
    }
}

@Composable
fun VideoDurationBadge(seconds: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xB3000000))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(formatDuration(seconds), color = Color.White, fontSize = 12.sp)
    }
}

@Composable
fun CameraGlyph(modifier: Modifier = Modifier, color: Color = Color(0xFF444444)) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color,
            topLeft = Offset(w * 0.08f, h * 0.28f),
            size = Size(w * 0.84f, h * 0.58f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.1f)
        )
        drawRect(color, topLeft = Offset(w * 0.36f, h * 0.14f), size = Size(w * 0.28f, h * 0.18f))
        drawCircle(Color(0xFFF9F9FF), radius = w * 0.16f, center = Offset(w * 0.5f, h * 0.57f))
    }
}

@Composable
fun CheckBadge(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(26.dp).background(Color(0xFF8C9FD0), CircleShape)) {
        val w = size.width
        drawLine(Color.White, Offset(w * 0.28f, w * 0.52f), Offset(w * 0.45f, w * 0.68f), strokeWidth = w * 0.09f)
        drawLine(Color.White, Offset(w * 0.45f, w * 0.68f), Offset(w * 0.74f, w * 0.32f), strokeWidth = w * 0.09f)
    }
}

@Composable
fun CoverWithGradient(
    repo: GalleryRepository,
    coverItem: MediaItem?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    Box(modifier = modifier) {
        if (coverItem != null) {
            ThumbImage(repo, coverItem, Modifier.fillMaxSize())
        } else {
            Box(Modifier.fillMaxSize().background(Color(0xFFCFD8DC)))
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color(0xB3000000)),
                    startY = 160f
                )
            )
        )
        content()
    }
}
