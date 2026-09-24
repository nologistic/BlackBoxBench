package com.blackboxbench.reproduction

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Fossify-like light palette used by the observed gallery. */
val GalleryBackground = Color(0xFFF5F5FA)
val GallerySurface = Color(0xFFFFFBFF)
val GalleryPrimary = Color(0xFF415F91)
val GalleryTopBar = Color(0xFFDDE2F0)
val GallerySelectionBar = Color(0xFF7C8CA8)
val GalleryText = Color(0xFF1A1C1E)
val GallerySecondaryText = Color(0xFF44474E)
val GalleryDivider = Color(0xFFE1E2EC)

@Composable
fun BenchmarkAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = GalleryPrimary,
            onPrimary = Color.White,
            background = GalleryBackground,
            onBackground = GalleryText,
            surface = GallerySurface,
            onSurface = GalleryText,
            surfaceVariant = GalleryTopBar,
            onSurfaceVariant = GallerySecondaryText
        ),
        content = content
    )
}

private val thumbCache = LruCache<String, ImageBitmap>(160)

private fun loadThumbnail(file: File, kind: MediaKind, target: Int): ImageBitmap? {
    if (!file.exists()) return null
    return runCatching {
        if (kind == MediaKind.VIDEO) {
            val mmr = MediaMetadataRetriever()
            val frame: Bitmap? = try {
                mmr.setDataSource(file.absolutePath)
                mmr.getFrameAtTime(0)
            } finally {
                runCatching { mmr.release() }
            }
            frame?.asImageBitmap()
        } else {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, opts)
            var sample = 1
            while (opts.outWidth / sample > target * 2 || opts.outHeight / sample > target * 2) sample *= 2
            val decode = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeFile(file.absolutePath, decode)?.asImageBitmap()
        }
    }.getOrNull()
}

/** Thumbnail for grid cells; falls back to a warning placeholder for undecodable files. */
@Composable
fun MediaThumb(
    file: File,
    item: MediaItem,
    modifier: Modifier = Modifier,
    target: Int = 320,
    contentScale: ContentScale = ContentScale.Crop
) {
    val key = file.absolutePath + "#" + item.modifiedMs + "#" + target
    var bitmap by remember(key) { mutableStateOf(thumbCache.get(key)) }
    LaunchedEffect(key) {
        if (bitmap == null) {
            val loaded = withContext(Dispatchers.IO) { loadThumbnail(file, item.kind, target) }
            if (loaded != null) {
                thumbCache.put(key, loaded)
                bitmap = loaded
            }
        }
    }
    val current = bitmap
    Box(modifier = modifier.background(Color(0xFF2A2A2A)), contentAlignment = Alignment.Center) {
        if (current != null) {
            Image(bitmap = current, contentDescription = item.fileName, modifier = Modifier.fillMaxSize(), contentScale = contentScale)
        } else {
            Text("⚠", color = Color(0xFFFFC107), fontSize = 34.sp, textAlign = TextAlign.Center)
        }
    }
}

/** Full resolution bitmap for the viewer / editor. */
@Composable
fun rememberFullBitmap(file: File): ImageBitmap? {
    val key = "full:" + file.absolutePath + ":" + file.length()
    var bitmap by remember(key) { mutableStateOf(thumbCache.get(key)) }
    LaunchedEffect(key) {
        if (bitmap == null) {
            val loaded = withContext(Dispatchers.IO) {
                runCatching {
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(file.absolutePath, opts)
                    var sample = 1
                    while (opts.outWidth / sample > 1600 || opts.outHeight / sample > 1600) sample *= 2
                    BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
                        ?.asImageBitmap()
                }.getOrNull()
            }
            if (loaded != null) {
                thumbCache.put(key, loaded)
                bitmap = loaded
            }
        }
    }
    return bitmap
}

fun formatSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / 1024.0 / 1024.0)
    bytes >= 1024 -> String.format("%.1f kB", bytes / 1024.0)
    else -> "$bytes B"
}

fun formatDate(ms: Long): String {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = ms }
    val months = listOf("一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月")
    return "${cal.get(java.util.Calendar.DAY_OF_MONTH)} ${months[cal.get(java.util.Calendar.MONTH)]} ${cal.get(java.util.Calendar.YEAR)}"
}

fun formatDateTime(ms: Long): String {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = ms }
    return formatDate(ms) + String.format(
        ", %02d:%02d",
        cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE)
    )
}
