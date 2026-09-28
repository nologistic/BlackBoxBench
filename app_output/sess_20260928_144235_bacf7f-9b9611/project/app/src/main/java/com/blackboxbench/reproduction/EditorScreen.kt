package com.blackboxbench.reproduction

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

fun sessionLayer(s: ToolSession): EditLayer = EditLayer(
    toolId = s.tool.id,
    name = s.tool.name,
    params = s.params,
    variant = s.variant,
    variantName = s.tool.variants.getOrNull(s.variant) ?: "",
    crop = s.crop,
    rot90 = s.rot90,
    flipH = s.flipH,
    textValue = s.text,
    pointX = s.pointX,
    pointY = s.pointY,
    pointR = s.pointR,
    amount = s.amount
)

@Composable
fun displayBitmap(state: AppState): Bitmap? {
    val bmp by produceState<Bitmap?>(null, state.original, state.layers, state.stylePreview) {
        val orig = state.original
        if (orig == null) {
            value = null
            return@produceState
        }
        value = withContext(Dispatchers.Default) {
            var b = ImageOps.render(orig, state.layers)
            state.stylePreview?.let { b = ImageOps.applyLayer(b, state.styleLayer(it)) }
            b
        }
    }
    return bmp
}

@Composable
fun EditorScreen(state: AppState) {
    val display = displayBitmap(state)

    BackHandler(enabled = true) {
        when {
            state.tool != null -> state.tool = null
            state.pickerOpen -> state.pickerOpen = false
            state.styleNameDialog -> state.styleNameDialog = false
            state.shareOpen -> state.shareOpen = false
            state.exportAsOpen -> state.exportAsOpen = false
            state.stackMenuOpen -> state.stackMenuOpen = false
            state.qrMenuOpen -> state.qrMenuOpen = false
            state.overflowOpen -> state.overflowOpen = false
            state.exportDialog != null -> state.exportDialog = null
            state.confirmKind != null -> state.confirmKind = null
            state.stylePreview != null -> {
                state.stylePreview = null
                state.stylePreviewIndex = null
            }
            state.panelTab != null -> state.panelTab = null
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Palette.Bg)) {
        EditorTopBar(state)
        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            if (state.photoOpen) ImageArea(state, display) else HomePrompt(state)
        }
        if (state.photoOpen) {
            when {
                state.stylePreview != null -> StyleConfirmBar(state)
                state.panelTab == 0 -> BottomPanel { StyleStrip(state) }
                state.panelTab == 1 -> BottomPanel { ToolGrid(state) }
                state.panelTab == 2 -> BottomPanel { ExportList(state) }
            }
            if (state.stylePreview == null) TabBar(state)
        }
    }

    if (state.pickerOpen) PhotoPickerSheet(state)
    if (state.shareOpen) ShareSheet(state, display)
    if (state.exportAsOpen) ExportAsSheet(state)
    if (state.exportDialog != null) ExportOptionDialog(state, state.exportDialog!!)
    if (state.styleNameDialog) StyleNameDialog(state)
    if (state.confirmKind != null) ConfirmDialog(state, state.confirmKind!!)
    if (state.overflowOpen) OverflowSheet(state)
    if (state.stackMenuOpen) StackMenuSheet(state)
    if (state.qrMenuOpen) QrSheet(state)
    state.tool?.let { ToolScreen(state, it) }
    state.snackbar?.let { Snackbar(state, it) }
}

@Composable
fun EditorTopBar(state: AppState) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "打开",
            color = Palette.TextPrimary,
            fontSize = 17.sp,
            modifier = Modifier.clickable {
                if (state.photoOpen) state.confirmKind = "open" else state.pickerOpen = true
            }.padding(horizontal = 10.dp, vertical = 8.dp)
        )
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier.size(44.dp).clickable {
                if (state.photoOpen) state.stackMenuOpen = true
            },
            contentAlignment = Alignment.Center
        ) {
            StackGlyph(state.layers.size, Palette.TextPrimary)
            if (state.layers.isNotEmpty()) {
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 4.dp)
                        .size(16.dp).clip(RoundedCornerShape(8.dp)).background(Palette.TextPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${state.layers.size}", color = Color.White, fontSize = 10.sp)
                }
            }
        }
        Box(
            modifier = Modifier.size(44.dp).clickable {
                if (state.photoOpen) state.screen = AppScreen.INFO
            },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Info, contentDescription = "info", tint = Palette.TextPrimary)
        }
        Box(
            modifier = Modifier.size(44.dp).clickable { state.overflowOpen = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.MoreVert, contentDescription = "more", tint = Palette.TextPrimary)
        }
    }
}

@Composable
fun HomePrompt(state: AppState) {
    Column(
        modifier = Modifier.fillMaxSize().clickable { state.pickerOpen = true },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.AddCircle,
            contentDescription = "open",
            tint = Color(0xFF9AA0A6),
            modifier = Modifier.size(150.dp)
        )
        Spacer(Modifier.height(20.dp))
        Text("点按任意位置即可打开照片", color = Palette.TextSecondary, fontSize = 18.sp)
    }
}

@Composable
fun ImageArea(state: AppState, display: Bitmap?) {
    Box(modifier = Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.Center) {
        if (display != null) {
            Image(
                bitmap = display.asImageBitmapCompat(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

fun Bitmap.asImageBitmapCompat() = this.asImageBitmap()

@Composable
fun BottomPanel(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().background(Palette.SheetBg)) { content() }
}

@Composable
fun TabBar(state: AppState) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).background(Palette.SheetBg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf("样式", "工具", "导出").forEachIndexed { i, label ->
            val selected = state.panelTab == i
            Box(
                modifier = Modifier.weight(1f).fillMaxSize()
                    .clickable { state.panelTab = i },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (selected) Palette.Accent else Palette.TextSecondary,
                    fontSize = 16.sp,
                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun StyleStrip(state: AppState) {
    val base = state.original
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        LazyRow(
            modifier = Modifier.fillMaxWidth().height(86.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                StyleCell(
                    label = "上次修改",
                    bitmap = base,
                    filter = ColorMath.filter(state.lastStyle ?: StyleCatalog.styles[3]),
                    selected = state.stylePreviewIndex == 0
                ) {
                    state.stylePreview = state.lastStyle ?: StyleCatalog.styles[3]
                    state.stylePreviewIndex = 0
                }
            }
            items(StyleCatalog.styles.size - 1) { idx ->
                val real = idx + 1
                val style = StyleCatalog.styles[real]
                StyleCell(
                    label = style.name,
                    bitmap = base,
                    filter = ColorMath.filter(style),
                    selected = state.stylePreviewIndex == real
                ) {
                    state.stylePreview = style
                    state.stylePreviewIndex = real
                }
            }
            items(state.customStyles.size) { idx ->
                val (name, style) = state.customStyles[idx]
                StyleCell(
                    label = name,
                    bitmap = base,
                    filter = ColorMath.filter(style),
                    selected = false
                ) {
                    state.stylePreview = style
                    state.stylePreviewIndex = 100 + idx
                }
            }
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE0E0E0)).clickable { state.styleNameDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "add style", tint = Color(0xFF8A8A8A))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun StyleCell(label: String, bitmap: Bitmap?, filter: ColorFilter, selected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFDADADA)).clickable { onClick() }
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmapCompat(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    colorFilter = filter,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (selected) {
                Box(modifier = Modifier.fillMaxSize().background(Color(0x22000000)))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            fontSize = 11.sp,
            color = if (selected) Palette.Accent else Palette.TextPrimary,
            maxLines = 1
        )
    }
}

@Composable
fun StyleConfirmBar(state: AppState) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).background(Palette.SheetBg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(56.dp).clickable {
                state.stylePreview = null
                state.stylePreviewIndex = null
                state.panelTab = null
            },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Close, contentDescription = "cancel", tint = Palette.TextPrimary)
        }
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier.size(56.dp).clickable {
                state.stylePreview?.let { s ->
                    state.commitLayer(state.styleLayer(s))
                    state.lastStyle = s
                }
                state.stylePreview = null
                state.stylePreviewIndex = null
            },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Check, contentDescription = "apply", tint = Palette.TextPrimary)
        }
    }
}

@Composable
fun ToolGrid(state: AppState) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        modifier = Modifier.fillMaxWidth().height(372.dp).padding(horizontal = 6.dp, vertical = 6.dp),
        userScrollEnabled = false
    ) {
        items(ToolCatalog.tools) { tool ->
            Column(
                modifier = Modifier.height(60.dp).clickable { state.openTool(tool) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ToolGlyph(tool.id, Palette.TextPrimary)
                Spacer(Modifier.height(4.dp))
                Text(tool.name, fontSize = 10.sp, color = Palette.TextPrimary, maxLines = 1)
            }
        }
    }
}

fun AppState.openTool(tool: ToolDef) {
    val params = tool.params.associate { it.name to it.default }
    val amount = when (tool.id) {
        "double" -> 0.6f
        "brush" -> 10f
        "expand" -> 12f
        else -> 0f
    }
    val dialog = when (tool.id) {
        "face" -> "face"
        "headpose" -> "headpose"
        else -> null
    }
    this.tool = ToolSession(tool = tool, params = params, amount = amount, dialog = dialog)
}

@Composable
fun ExportList(state: AppState) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ExportRow(Icons.Filled.Share, "分享", "与他人分享图片或在其他应用中打开图片") { state.shareOpen = true }
        ExportRow(Icons.Filled.KeyboardArrowDown, "保存", "为您的照片创建副本") {
            state.snackbar = "照片已保存"
        }
        ExportRow(Icons.Filled.Send, "导出", "为您的照片创建副本。您可以在设置菜单中更改大小、格式和画质。") {
            state.snackbar = "照片已保存"
        }
        ExportRow(Icons.Filled.List, "导出为", "在选定文件夹中创建副本") { state.exportAsOpen = true }
    }
}

@Composable
fun ExportRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = Palette.TextPrimary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(20.dp))
        Column {
            Text(title, fontSize = 16.sp, color = Palette.TextPrimary)
            Text(subtitle, fontSize = 12.sp, color = Palette.TextSecondary)
        }
    }
}

@Composable
fun Snackbar(state: AppState, message: String) {
    LaunchedEffect(message) {
        delay(3000)
        state.snackbar = null
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp)
                .padding(bottom = 52.dp)
                .clip(RoundedCornerShape(4.dp)).background(Color(0xFF323232))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(message, color = Color.White, fontSize = 14.sp)
            Spacer(Modifier.weight(1f))
            Text("查看", color = Color(0xFF8AB4F8), fontSize = 14.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { state.snackbar = null })
        }
    }
}
