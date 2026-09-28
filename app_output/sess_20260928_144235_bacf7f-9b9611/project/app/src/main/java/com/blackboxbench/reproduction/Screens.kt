package com.blackboxbench.reproduction

import androidx.activity.compose.BackHandler
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ViewEditsScreen(state: AppState) {
    val display = displayBitmap(state)
    var selected by remember { mutableStateOf<Int?>(null) }

    BackHandler { state.screen = AppScreen.EDITOR }

    Column(modifier = Modifier.fillMaxSize().background(Palette.Bg)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(52.dp).clickable { state.screen = AppScreen.EDITOR },
                contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "back", tint = Palette.TextPrimary)
            }
            Spacer(Modifier.weight(1f))
            Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.MoreVert, contentDescription = null, tint = Palette.TextPrimary)
            }
        }
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (display != null) {
                Image(
                    bitmap = display.asImageBitmapCompat(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(8.dp)
                )
            }
            Column(
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).width(190.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                state.layers.reversed().forEachIndexed { revIdx, layer ->
                    val realIdx = state.layers.size - 1 - revIdx
                    LayerCard(
                        state = state,
                        index = realIdx,
                        name = layer.name,
                        selected = selected == realIdx,
                        onSelect = { selected = if (selected == realIdx) null else realIdx },
                        onDelete = {
                            state.layers = state.layers.toMutableList().also { it.removeAt(realIdx) }
                            selected = null
                        }
                    )
                }
                LayerCard(
                    state = state,
                    index = -1,
                    name = "原图",
                    selected = state.layers.isEmpty(),
                    onSelect = {},
                    onDelete = null
                )
            }
        }
    }
}

@Composable
fun LayerCard(state: AppState, index: Int, name: String, selected: Boolean, onSelect: () -> Unit, onDelete: (() -> Unit)?) {
    val thumb by produceState<Bitmap?>(null, state.original, state.layers, index) {
        val orig = state.original
        value = if (orig == null) null else withContext(Dispatchers.Default) {
            if (index < 0) orig else ImageOps.render(orig, state.layers.take(index + 1))
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(3.dp))
            .background(if (selected) Palette.Accent else Color.White)
            .clickable { onSelect() }.padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(2.dp))
                .background(Color(0xFFDADADA))
        ) {
            val t = thumb
            if (t != null) {
                Image(
                    bitmap = t.asImageBitmapCompat(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            name,
            fontSize = 13.sp,
            color = if (selected) Color.White else Palette.TextPrimary,
            maxLines = 1
        )
        Spacer(Modifier.weight(1f))
        if (selected && onDelete != null) {
            Box(modifier = Modifier.size(24.dp).clickable { onDelete() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Delete, contentDescription = "delete", tint = Color.White,
                    modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun InfoScreen(state: AppState) {
    BackHandler { state.screen = AppScreen.EDITOR }
    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(52.dp).clickable { state.screen = AppScreen.EDITOR },
                contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "back", tint = Palette.TextPrimary)
            }
        }
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = Palette.TextSecondary,
                    modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(20.dp))
                Column {
                    Text("九月 28, 2026", fontSize = 15.sp, color = Palette.TextPrimary)
                    Text("星期一, 22:42", fontSize = 13.sp, color = Palette.TextSecondary)
                }
            }
            Spacer(Modifier.height(28.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = Palette.TextSecondary,
                    modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(20.dp))
                Column {
                    Text(state.sourceName, fontSize = 15.sp, color = Palette.TextPrimary)
                    Text(
                        "${mp(state)} MP  ${state.sourceWidth} x ${state.sourceHeight}  ${sizeLabel(state)}",
                        fontSize = 13.sp, color = Palette.TextSecondary
                    )
                }
            }
        }
    }
}

fun mp(state: AppState): String {
    val m = state.sourceWidth.toLong() * state.sourceHeight / 1_000_000.0
    return String.format("%.1f", m)
}

fun sizeLabel(state: AppState): String {
    return String.format("%.2f KB", state.sourceBytes / 1024.0)
}

@Composable
fun SettingsScreen(state: AppState) {
    BackHandler {
        if (state.exportDialog != null) state.exportDialog = null else state.screen = AppScreen.EDITOR
    }
    val resizeLabels = listOf("不要调整大小", "4000 像素", "2000 像素", "1920 像素", "1366 像素", "800 像素")
    val formatLabels = listOf("JPG 100%", "JPG 95%", "JPG 80%", "PNG")

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(52.dp).clickable { state.screen = AppScreen.EDITOR },
                contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "back", tint = Palette.TextPrimary)
            }
            Text("设置", fontSize = 18.sp, color = Palette.TextPrimary)
        }
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(8.dp))
            Text("外观", fontSize = 13.sp, color = Palette.Accent)
            Spacer(Modifier.height(12.dp))
            Text("深色主题背景", fontSize = 16.sp, color = Palette.TextPrimary)
            Spacer(Modifier.height(24.dp))
            Text("导出和分享选项", fontSize = 13.sp, color = Palette.Accent)
            Spacer(Modifier.height(12.dp))
            Text("调整图片大小", fontSize = 16.sp, color = Palette.TextPrimary,
                modifier = Modifier.clickable { state.exportDialog = 0 })
            Text(resizeLabels[state.exportWidth], fontSize = 13.sp, color = Palette.TextSecondary)
            Spacer(Modifier.height(10.dp))
            Text(
                "选择导出和分享的最大图片尺寸。指定大小会参照图片的长边（如横向图片的宽度），且只会应用于尺寸大于所选值的图片。尺寸较小的图片不会放大。",
                fontSize = 12.sp, color = Palette.TextSecondary
            )
            Spacer(Modifier.height(24.dp))
            Text("格式和画质", fontSize = 16.sp, color = Palette.TextPrimary,
                modifier = Modifier.clickable { state.exportDialog = 1 })
            Text(formatLabels[state.exportFormat], fontSize = 13.sp, color = Palette.TextSecondary)
            Spacer(Modifier.height(10.dp))
            Text(
                "选择导出或分享图片时采用的格式和画质比率。JPG 100% 的 jpeg 图片画质最佳，但文件也会变大。",
                fontSize = 12.sp, color = Palette.TextSecondary
            )
        }
    }
    if (state.exportDialog != null) ExportOptionDialog(state, state.exportDialog!!)
    }
}
