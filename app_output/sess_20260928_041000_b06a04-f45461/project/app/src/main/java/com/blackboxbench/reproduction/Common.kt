package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------------------
// Toolbar
// ---------------------------------------------------------------------------

@Composable
fun TopBar(containerColor: Color = Palette.toolbar, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor)
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
fun SearchField(hint: String, modifier: Modifier = Modifier, onTap: () -> Unit = {},
                query: String? = null, onQueryChange: (String) -> Unit = {}) {
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Palette.searchPill)
            .clickable { onTap() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconSearch(Modifier.size(20.dp), Palette.onSurfaceVariant)
        Spacer(Modifier.width(10.dp))
        if (query == null) {
            Text(hint, fontSize = 15.sp, color = Palette.onSurfaceVariant, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        } else {
            androidx.compose.foundation.text.BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, color = Palette.onSurface),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun IconButtonBox(size: Int = 48, onClick: () -> Unit = {}, tint: Color = Palette.onSurface,
                  content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { content() }
}

// ---------------------------------------------------------------------------
// Hand drawn icons (kept minimal, no extended icon dependency)
// ---------------------------------------------------------------------------

@Composable
fun IconSearch(modifier: Modifier = Modifier, tint: Color = Palette.onSurface) {
    Canvas(modifier = modifier) {
        val s = size.minDimension
        val sw = s * 0.10f
        drawCircle(tint, radius = s * 0.28f, center = Offset(s * 0.42f, s * 0.42f), style = Stroke(sw))
        drawLine(tint, Offset(s * 0.63f, s * 0.63f), Offset(s * 0.86f, s * 0.86f), sw, StrokeCap.Round)
    }
}

@Composable
fun IconBack(tint: Color = Palette.onSurface) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val s = size.minDimension
        val sw = s * 0.10f
        drawLine(tint, Offset(s * 0.58f, s * 0.16f), Offset(s * 0.26f, s * 0.5f), sw, StrokeCap.Round)
        drawLine(tint, Offset(s * 0.26f, s * 0.5f), Offset(s * 0.58f, s * 0.84f), sw, StrokeCap.Round)
    }
}

@Composable
fun IconDots(tint: Color = Palette.onSurface) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val s = size.minDimension
        val r = s * 0.075f
        drawCircle(tint, r, Offset(s / 2, s * 0.16f))
        drawCircle(tint, r, Offset(s / 2, s * 0.5f))
        drawCircle(tint, r, Offset(s / 2, s * 0.84f))
    }
}

@Composable
fun IconViewType(tint: Color = Palette.onSurface) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val s = size.minDimension
        val sw = s * 0.08f
        drawRoundRect(tint, Offset(s * 0.08f, s * 0.12f), Size(s * 0.84f, s * 0.76f),
            androidx.compose.ui.geometry.CornerRadius(s * 0.12f), style = Stroke(sw))
        drawLine(tint, Offset(s * 0.08f, s * 0.34f), Offset(s * 0.92f, s * 0.34f), sw)
        drawLine(tint, Offset(s * 0.5f, s * 0.34f), Offset(s * 0.5f, s * 0.88f), sw)
    }
}

@Composable
fun IconAllMedia(tint: Color = Palette.onSurface) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val s = size.minDimension
        val p = Path().apply {
            moveTo(s * 0.14f, s * 0.24f)
            lineTo(s * 0.86f, s * 0.24f)
            lineTo(s * 0.72f, s * 0.78f)
            lineTo(s * 0.28f, s * 0.78f)
            close()
        }
        drawPath(p, tint)
        drawCircle(Color.White, s * 0.10f, Offset(s * 0.36f, s * 0.40f))
        val m = Path().apply {
            moveTo(s * 0.24f, s * 0.72f)
            lineTo(s * 0.46f, s * 0.48f)
            lineTo(s * 0.62f, s * 0.66f)
            lineTo(s * 0.72f, s * 0.56f)
            lineTo(s * 0.80f, s * 0.72f)
            close()
        }
        drawPath(m, Color.White)
    }
}

@Composable
fun IconSort(tint: Color = Palette.onSurface) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val s = size.minDimension
        val sw = s * 0.09f
        drawLine(tint, Offset(s * 0.10f, s * 0.24f), Offset(s * 0.62f, s * 0.24f), sw, StrokeCap.Round)
        drawLine(tint, Offset(s * 0.10f, s * 0.5f), Offset(s * 0.46f, s * 0.5f), sw, StrokeCap.Round)
        drawLine(tint, Offset(s * 0.10f, s * 0.76f), Offset(s * 0.30f, s * 0.76f), sw, StrokeCap.Round)
        drawLine(tint, Offset(s * 0.82f, s * 0.30f), Offset(s * 0.82f, s * 0.80f), sw, StrokeCap.Round)
    }
}

@Composable
fun IconStar(tint: Color = Palette.onSurface, filled: Boolean = false) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val s = size.minDimension
        val cx = s / 2; val cy = s * 0.53f; val r = s * 0.44f; val ri = r * 0.44f
        val path = Path()
        for (i in 0 until 10) {
            val ang = (-90.0 + i * 36.0) * Math.PI / 180.0
            val rr = if (i % 2 == 0) r else ri
            val x = (cx + rr * Math.cos(ang)).toFloat(); val y = (cy + rr * Math.sin(ang)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        if (filled) drawPath(path, tint) else drawPath(path, tint, style = Stroke(s * 0.09f, cap = StrokeCap.Round))
    }
}

@Composable
fun IconEdit(tint: Color = Palette.onSurface) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val s = size.minDimension
        drawLine(tint, Offset(s * 0.18f, s * 0.82f), Offset(s * 0.74f, s * 0.20f), s * 0.11f, StrokeCap.Round)
        drawLine(tint, Offset(s * 0.28f, s * 0.94f), Offset(s * 0.86f, s * 0.30f), s * 0.11f, StrokeCap.Round)
    }
}

@Composable
fun IconShare(tint: Color = Palette.onSurface) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val s = size.minDimension
        val sw = s * 0.09f
        drawLine(tint, Offset(s * 0.30f, s * 0.50f), Offset(s * 0.72f, s * 0.26f), sw, StrokeCap.Round)
        drawLine(tint, Offset(s * 0.30f, s * 0.50f), Offset(s * 0.72f, s * 0.74f), sw, StrokeCap.Round)
        drawCircle(tint, s * 0.13f, Offset(s * 0.24f, s * 0.50f))
        drawCircle(tint, s * 0.13f, Offset(s * 0.78f, s * 0.22f))
        drawCircle(tint, s * 0.13f, Offset(s * 0.78f, s * 0.78f))
    }
}

@Composable
fun IconTrash(tint: Color = Palette.onSurface) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val s = size.minDimension
        val sw = s * 0.09f
        drawLine(tint, Offset(s * 0.12f, s * 0.24f), Offset(s * 0.88f, s * 0.24f), sw, StrokeCap.Round)
        drawLine(tint, Offset(s * 0.38f, s * 0.14f), Offset(s * 0.62f, s * 0.14f), sw, StrokeCap.Round)
        drawRoundRect(tint, Offset(s * 0.22f, s * 0.30f), Size(s * 0.56f, s * 0.56f),
            androidx.compose.ui.geometry.CornerRadius(s * 0.10f), style = Stroke(sw))
        drawLine(tint, Offset(s * 0.40f, s * 0.44f), Offset(s * 0.40f, s * 0.72f), sw * 0.8f, StrokeCap.Round)
        drawLine(tint, Offset(s * 0.60f, s * 0.44f), Offset(s * 0.60f, s * 0.72f), sw * 0.8f, StrokeCap.Round)
    }
}

@Composable
fun IconInfo(tint: Color = Palette.onSurface) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val s = size.minDimension
        val sw = s * 0.09f
        drawCircle(tint, s * 0.44f, Offset(s / 2, s / 2), style = Stroke(sw))
        drawCircle(tint, s * 0.06f, Offset(s / 2, s * 0.29f))
        drawLine(tint, Offset(s / 2, s * 0.44f), Offset(s / 2, s * 0.72f), sw, StrokeCap.Round)
    }
}

@Composable
fun IconRotateHistory(tint: Color = Palette.onSurface) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val s = size.minDimension
        val sw = s * 0.085f
        drawArc(tint, 40f, 280f, false, Offset(s * 0.12f, s * 0.12f), Size(s * 0.76f, s * 0.76f),
            style = Stroke(sw, cap = StrokeCap.Round))
        drawCircle(tint, s * 0.055f, Offset(s * 0.5f, s * 0.5f))
        drawLine(tint, Offset(s * 0.5f, s * 0.5f), Offset(s * 0.5f, s * 0.30f), sw * 0.8f, StrokeCap.Round)
        drawLine(tint, Offset(s * 0.5f, s * 0.5f), Offset(s * 0.66f, s * 0.58f), sw * 0.8f, StrokeCap.Round)
    }
}

@Composable
fun IconCheck(tint: Color = Palette.onSurface) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val s = size.minDimension
        val sw = s * 0.11f
        drawLine(tint, Offset(s * 0.16f, s * 0.52f), Offset(s * 0.42f, s * 0.78f), sw, StrokeCap.Round)
        drawLine(tint, Offset(s * 0.42f, s * 0.78f), Offset(s * 0.86f, s * 0.24f), sw, StrokeCap.Round)
    }
}

@Composable
fun IconPlay(tint: Color = Color.White) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val s = size.minDimension
        val p = Path().apply {
            moveTo(s * 0.24f, s * 0.14f); lineTo(s * 0.86f, s * 0.5f); lineTo(s * 0.24f, s * 0.86f); close()
        }
        drawPath(p, tint)
    }
}

// ---------------------------------------------------------------------------
// Tiles
// ---------------------------------------------------------------------------

@Composable
fun AlbumTile(item: MediaItem?, label: String, count: Int, badge: String?, modifier: Modifier = Modifier,
              onClick: () -> Unit) {
    val context = LocalContext.current
    val bmp = remember(item?.uri, item?.name) { item?.let { GalleryState.bitmap(context, it) } }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(2.dp))
            .background(Color(0xFF2A2A2A))
            .clickable { onClick() }
    ) {
        if (bmp != null) {
            Image(bmp.asImageBitmap(), contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        if (badge != null) {
            Box(
                modifier = Modifier
                    .padding(6.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(0xCCFFFFFF)),
                contentAlignment = Alignment.Center,
            ) {
                when (badge) {
                    "star" -> IconStar(Palette.star, filled = true)
                    "trash" -> IconTrash(Palette.accentDark)
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color(0xCC000000)))
        )
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)) {
            Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("$count", color = Color(0xFFE6E1E5), fontSize = 13.sp)
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MediaTile(item: MediaItem, showName: Boolean, selected: Boolean, modifier: Modifier = Modifier,
              onClick: () -> Unit, onLongClick: () -> Unit) {
    val context = LocalContext.current
    val bmp = remember(item.uri, item.name) { GalleryState.bitmap(context, item) }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(2.dp))
            .background(Color(0xFF2A2A2A))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        if (bmp != null) {
            Image(bmp.asImageBitmap(), contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        if (item.favorite) {
            Box(
                modifier = Modifier.padding(6.dp).size(24.dp).clip(CircleShape).background(Color(0xCCFFFFFF)),
                contentAlignment = Alignment.Center,
            ) { IconStar(Palette.star, filled = true) }
        }
        if (item.isVideo) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xAA000000))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconPlay(Color.White)
                Spacer(Modifier.width(4.dp))
                Text(formatDuration(item.durationMs), color = Color.White, fontSize = 11.sp)
            }
        }
        if (showName) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color(0xCC000000))
                    .padding(horizontal = 5.dp, vertical = 3.dp)
            ) {
                Text(item.name, color = Color.White, fontSize = 10.sp, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
        }
        if (selected) {
            Box(modifier = Modifier.fillMaxSize().background(Palette.accent.copy(alpha = 0.45f)))
        }
    }
}

// ---------------------------------------------------------------------------
// Small building blocks
// ---------------------------------------------------------------------------

@Composable
fun SectionHeader(text: String) {
    Text(
        text,
        color = Palette.accent,
        fontSize = 13.sp,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF7F2FA))
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
fun CheckRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 15.sp, color = Palette.onSurface)
        androidx.compose.material3.Checkbox(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun RowDivider() {
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Palette.divider.copy(alpha = 0.6f)))
}
