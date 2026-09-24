package com.blackboxbench.reproduction

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween

/** 从 assets 装载封面位图（带缓存）。 */
@Composable
fun rememberCoverBitmap(asset: String): ImageBitmap? {
    val context = LocalContext.current
    var bitmap by remember(asset) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(asset) {
        val bmp = runCatching {
            context.assets.open(asset).use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
        bitmap = bmp?.asImageBitmap()
    }
    return bitmap
}

/** 专辑封面（素材 PNG），失败时画纯色块。 */
@Composable
fun CoverImage(album: String, size: Int, modifier: Modifier = Modifier) {
    val bmp = rememberCoverBitmap(MusicLibrary.coverFor(album))
    val m = modifier.size(size.dp).clip(RoundedCornerShape(6.dp))
    if (bmp != null) {
        Image(bitmap = bmp, contentDescription = "专辑封面", contentScale = ContentScale.Crop, modifier = m)
    } else {
        Box(m.background(Color(0xFF37474F)))
    }
}

/** 黑胶唱片：黑色圆盘 + 中心封面，播放时旋转；暂停停转。 */
@Composable
fun VinylDisc(album: String, sizeDp: Int, spinning: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "vinyl")
    val angle by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)), label = "angle"
    )
    val bmp = rememberCoverBitmap(MusicLibrary.coverFor(album))
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(sizeDp.dp)
            .graphicsLayer { rotationZ = if (spinning) angle else 0f }
            .background(Color(0xFF141414), CircleShape),
    ) {
        Box(Modifier.size((sizeDp * 0.60f).dp).background(Color(0xFF263238), CircleShape))
        if (bmp != null) {
            Image(
                bitmap = bmp, contentDescription = "封面", contentScale = ContentScale.Crop,
                modifier = Modifier.size((sizeDp * 0.36f).dp).clip(CircleShape),
            )
        } else {
            Box(
                Modifier
                    .size((sizeDp * 0.36f).dp)
                    .background(Color(0xFFF5C542), CircleShape)
            )
        }
    }
}

/** 通用歌曲行：主标题 + 副标题，可选尾部内容。 */
@Composable
fun SongRow(
    title: String,
    subtitle: String,
    playing: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title, fontSize = 17.sp,
                fontWeight = if (playing) FontWeight.Bold else FontWeight.Normal,
                color = if (playing) MaterialTheme.colorScheme.primary else Color(0xFF212121),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.size(2.dp))
            Text(
                subtitle, fontSize = 13.sp, color = Color(0xFF757575),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        trailing?.invoke()
    }
}

/** 空状态：黑胶插图 + 主/副文案。 */
@Composable
fun EmptyState(main: String, sub: String? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(top = 96.dp),
    ) {
        VinylDisc(album = "♪", sizeDp = 150, spinning = false)
        Spacer(Modifier.size(24.dp))
        Text(main, fontSize = 18.sp, color = Color(0xFF424242))
        if (sub != null) {
            Spacer(Modifier.size(8.dp))
            Text(sub, fontSize = 14.sp, color = Color(0xFF9E9E9E))
        }
    }
}
