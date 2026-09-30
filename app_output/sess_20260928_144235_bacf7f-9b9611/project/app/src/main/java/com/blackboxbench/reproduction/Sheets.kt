package com.blackboxbench.reproduction

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Scrim(onClick: () -> Unit = {}, content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Palette.Scrim).clickable { onClick() }) { content() }
}

@Composable
fun DialogCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0x99000000)), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.fillMaxWidth(0.88f).clip(RoundedCornerShape(6.dp))
                .background(Color.White).padding(horizontal = 22.dp, vertical = 20.dp)
        ) {
            content()
        }
    }
}

@Composable
fun PhotoPickerSheet(state: AppState) {
    var inAlbum by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxSize().background(Palette.Scrim)) {
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.68f)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(Color(0xFFF2F2F2))
        ) {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier.align(Alignment.CenterHorizontally).size(width = 32.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp)).background(Color(0xFFBDBDBD))
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "此应用只能访问您选择的照片",
                fontSize = 12.sp,
                color = Palette.TextSecondary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(10.dp))
            if (!inAlbum) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier.align(Alignment.CenterStart).size(48.dp)
                            .clickable { state.pickerOpen = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "close", tint = Palette.TextPrimary)
                    }
                    Row(
                        modifier = Modifier.align(Alignment.Center).clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFE3E3E3)).padding(4.dp)
                    ) {
                        PickerTab("照片", state.pickerAlbum.not()) { state.pickerAlbum = false }
                        PickerTab("影集", state.pickerAlbum) { state.pickerAlbum = true }
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (!state.pickerAlbum) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text("最近", fontSize = 13.sp, color = Palette.TextPrimary)
                        Spacer(Modifier.height(8.dp))
                        LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.fillMaxWidth()) {
                            items(state.photoAssets) { asset ->
                                Box(
                                    modifier = Modifier.padding(2.dp).fillMaxWidth().aspectRatio(1f)
                                        .clip(RoundedCornerShape(2.dp)).background(Color(0xFFCFCFCF))
                                        .clickable {
                                            state.pickerOpen = false
                                            state.openPhoto(asset)
                                        }
                                ) {
                                    state.thumbnail(asset)?.let { bmp ->
                                        Image(
                                            bitmap = bmp.asImageBitmapCompat(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { inAlbum = true }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(84.dp).clip(RoundedCornerShape(2.dp))
                                    .background(Color(0xFFCFCFCF))
                            ) {
                                state.thumbnail(state.photoAssets.first())?.let { bmp ->
                                    Image(
                                        bitmap = bmp.asImageBitmapCompat(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Text("下载内容", fontSize = 14.sp, color = Palette.TextPrimary)
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(48.dp).clickable { inAlbum = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "back", tint = Palette.TextPrimary)
                    }
                    Text("下载内容", fontSize = 17.sp, color = Palette.TextPrimary)
                }
                Spacer(Modifier.height(8.dp))
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text("星期四", fontSize = 13.sp, color = Palette.TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.fillMaxWidth()) {
                        items(state.photoAssets) { asset ->
                            Box(
                                modifier = Modifier.padding(2.dp).fillMaxWidth().aspectRatio(1f)
                                    .clip(RoundedCornerShape(2.dp)).background(Color(0xFFCFCFCF))
                                    .clickable {
                                        state.pickerOpen = false
                                        state.openPhoto(asset)
                                    }
                            ) {
                                state.thumbnail(asset)?.let { bmp ->
                                    Image(
                                        bitmap = bmp.asImageBitmapCompat(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PickerTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(16.dp))
            .background(if (selected) Color.White else Color.Transparent)
            .clickable { onClick() }.padding(horizontal = 22.dp, vertical = 8.dp)
    ) {
        Text(label, fontSize = 14.sp, color = if (selected) Palette.Accent else Palette.TextPrimary)
    }
}

@Composable
fun ShareSheet(state: AppState, display: Bitmap?) {
    Box(modifier = Modifier.fillMaxSize().background(Palette.Scrim).clickable { state.shareOpen = false }) {
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.72f)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)).background(Color.White)
        ) {
            Spacer(Modifier.height(12.dp))
            Text("分享图片", fontSize = 15.sp, color = Palette.TextPrimary,
                modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(12.dp))
            Box(modifier = Modifier.align(Alignment.CenterHorizontally).size(210.dp)) {
                if (display != null) {
                    Image(bitmap = display.asImageBitmapCompat(), contentDescription = null,
                        contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("没有任何推荐的分享对象", fontSize = 13.sp, color = Palette.TextSecondary,
                modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("快速分享", "信息", "Gmail Chat", "相册", "云端硬盘").forEach {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(26.dp))
                                .background(Color(0xFFE8EEF8)), contentAlignment = Alignment.Center
                        ) { Icon(Icons.Filled.Share, contentDescription = it, tint = Palette.Accent) }
                        Spacer(Modifier.height(6.dp))
                        Text(it, fontSize = 11.sp, color = Palette.TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun ExportAsSheet(state: AppState) {
    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(48.dp).clickable { state.exportAsOpen = false },
                contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "back", tint = Palette.TextPrimary)
            }
            Text("下载", fontSize = 18.sp, color = Palette.TextPrimary)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Filled.List, contentDescription = null, tint = Palette.TextPrimary,
                modifier = Modifier.padding(end = 16.dp))
        }
        Column(modifier = Modifier.padding(16.dp)) {
            Text("下载", fontSize = 13.sp, color = Palette.Accent)
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("“下载”中的文件", fontSize = 13.sp, color = Palette.TextSecondary)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Filled.List, contentDescription = null, tint = Palette.TextSecondary,
                    modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFF1F1F1))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.List, contentDescription = null, tint = Palette.TextSecondary)
                Spacer(Modifier.width(16.dp))
                Text("BlackBoxBench", fontSize = 14.sp, color = Palette.TextPrimary)
            }
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFD93025))
            Spacer(Modifier.width(12.dp))
            Text(fileName(state), fontSize = 14.sp, color = Palette.TextPrimary)
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Palette.Accent)
                    .clickable {
                        state.exportAsOpen = false
                        state.snackbar = "照片已保存"
                    }.padding(horizontal = 20.dp, vertical = 8.dp)
            ) { Text("保存", color = Color.White, fontSize = 14.sp) }
        }
    }
}

fun fileName(state: AppState): String {
    val base = state.photoAsset.substringAfterLast('/').substringBeforeLast('.')
    return "${base}_edited.jpeg"
}

@Composable
fun ExportOptionDialog(state: AppState, kind: Int) {
    val resizeOptions = listOf("不要调整大小", "4000 像素", "2000 像素", "1920 像素", "1366 像素", "800 像素")
    val formatOptions = listOf("JPG 100%", "JPG 95%", "JPG 80%", "PNG")
    val options = if (kind == 0) resizeOptions else formatOptions
    val selected = if (kind == 0) state.exportWidth else state.exportFormat
    DialogCard {
        Text(if (kind == 0) "调整图片大小" else "格式和画质", fontSize = 18.sp, color = Palette.TextPrimary)
        Spacer(Modifier.height(16.dp))
        options.forEachIndexed { i, label ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable {
                    if (kind == 0) state.exportWidth = i else state.exportFormat = i
                    state.exportDialog = null
                }.padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioDot(i == selected)
                Spacer(Modifier.width(18.dp))
                Text(label, fontSize = 15.sp, color = Palette.TextPrimary)
            }
        }
        Spacer(Modifier.height(10.dp))
        Box(modifier = Modifier.align(Alignment.End).clickable { state.exportDialog = null }
            .padding(6.dp)) {
            Text("取消", fontSize = 14.sp, color = Palette.Accent)
        }
    }
}

@Composable
fun RadioDot(selected: Boolean) {
    Box(
        modifier = Modifier.size(20.dp).clip(RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.size(18.dp).clip(RoundedCornerShape(9.dp))
                .background(if (selected) Palette.Accent else Color.Transparent)
        )
        if (selected) {
            Box(modifier = Modifier.size(7.dp).clip(RoundedCornerShape(4.dp)).background(Color.White))
        }
    }
}

@Composable
fun StyleNameDialog(state: AppState) {
    DialogCard {
        Text("保存此样式", fontSize = 18.sp, color = Palette.TextPrimary)
        Spacer(Modifier.height(14.dp))
        Text(
            "此样式将保存到“我的样式”。您可以将这些修改内容应用到任何其他图片。",
            fontSize = 13.sp, color = Palette.TextSecondary
        )
        Spacer(Modifier.height(20.dp))
        Box(modifier = Modifier.fillMaxWidth().height(36.dp)) {
            BasicTextField(
                value = state.styleName,
                onValueChange = { state.styleName = it },
                singleLine = true,
                textStyle = TextStyle(fontSize = 16.sp, color = Palette.TextPrimary),
                modifier = Modifier.fillMaxWidth().align(Alignment.CenterStart)
            )
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Palette.Accent)
                .align(Alignment.BottomStart))
        }
        Spacer(Modifier.height(4.dp))
        Text("我的样式名称", fontSize = 11.sp, color = Palette.TextSecondary)
        Spacer(Modifier.height(18.dp))
        Row(modifier = Modifier.align(Alignment.End)) {
            Box(modifier = Modifier.clickable {
                state.styleNameDialog = false
                state.styleName = ""
            }.padding(8.dp)) { Text("取消", fontSize = 14.sp, color = Palette.Accent) }
            Spacer(Modifier.width(16.dp))
            Box(modifier = Modifier.clickable(enabled = state.styleName.isNotBlank()) {
                val s = state.stylePreview ?: StyleCatalog.styles[3]
                state.customStyles = state.customStyles + (state.styleName to s)
                state.styleNameDialog = false
                state.styleName = ""
            }.padding(8.dp)) {
                Text("保存", fontSize = 14.sp,
                    color = if (state.styleName.isNotBlank()) Palette.Accent else Color(0xFFBDBDBD))
            }
        }
    }
}

@Composable
fun ConfirmDialog(state: AppState, kind: String) {
    DialogCard {
        if (kind == "open") {
            Text("打开照片", fontSize = 18.sp, color = Palette.TextPrimary)
            Spacer(Modifier.height(14.dp))
            Text("所有更改都将丢失。点按“打开”可打开新照片。", fontSize = 13.sp, color = Palette.TextSecondary)
        } else {
            Text("所有更改都将丢失。点按“还原”即可将图片还原。", fontSize = 14.sp, color = Palette.TextPrimary)
        }
        Spacer(Modifier.height(18.dp))
        Row(modifier = Modifier.align(Alignment.End)) {
            Box(modifier = Modifier.clickable { state.confirmKind = null }.padding(8.dp)) {
                Text("取消", fontSize = 14.sp, color = Palette.Accent)
            }
            Spacer(Modifier.width(12.dp))
            Box(modifier = Modifier.clickable {
                if (kind == "open") {
                    state.confirmKind = null
                    state.pickerOpen = true
                } else {
                    state.confirmKind = null
                    state.revert()
                    state.panelTab = null
                }
            }.padding(8.dp)) {
                Text(if (kind == "open") "打开" else "还原", fontSize = 14.sp, color = Palette.Accent)
            }
        }
    }
}

@Composable
fun OverflowSheet(state: AppState) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0x44000000)).clickable { state.overflowOpen = false }) {
        Column(
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 56.dp, end = 8.dp)
                .clip(RoundedCornerShape(4.dp)).background(Color.White).padding(vertical = 8.dp)
        ) {
            listOf("设置", "教程", "帮助和反馈").forEach { label ->
                Box(
                    modifier = Modifier.clickable {
                        state.overflowOpen = false
                        if (label == "设置") state.screen = AppScreen.SETTINGS
                    }.padding(horizontal = 24.dp, vertical = 12.dp)
                ) { Text(label, fontSize = 15.sp, color = Palette.TextPrimary) }
            }
        }
    }
}

@Composable
fun StackMenuSheet(state: AppState) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0x55000000)).clickable { state.stackMenuOpen = false }) {
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Color.White).padding(vertical = 6.dp)
        ) {
            StackMenuItem("撤消", state.layers.isNotEmpty()) {
                state.undo(); state.stackMenuOpen = false
            }
            StackMenuItem("重做", state.redoStack.isNotEmpty()) {
                state.redo(); state.stackMenuOpen = false
            }
            StackMenuItem("还原", state.layers.isNotEmpty()) {
                state.stackMenuOpen = false
                state.confirmKind = "revert"
            }
            StackMenuItem("查看修改内容", true) {
                state.stackMenuOpen = false
                state.screen = AppScreen.VIEW_EDITS
            }
            StackMenuItem("QR 样式...", true) {
                state.stackMenuOpen = false
                state.qrMenuOpen = true
            }
        }
    }
}

@Composable
fun StackMenuItem(label: String, enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (label.startsWith("重做")) Icons.Filled.Refresh else if (label.startsWith("还原")) Icons.Filled.Refresh else Icons.Filled.List,
            contentDescription = null,
            tint = if (enabled) Palette.TextPrimary else Color(0xFFBDBDBD),
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(24.dp))
        Text(label, fontSize = 16.sp, color = if (enabled) Palette.TextPrimary else Color(0xFFBDBDBD))
    }
}

@Composable
fun QrSheet(state: AppState) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0x44000000)).clickable { state.qrMenuOpen = false }) {
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Color.White).padding(vertical = 6.dp)
        ) {
            StackMenuItem("创建 QR 样式", state.layers.isNotEmpty()) { state.qrMenuOpen = false }
            StackMenuItem("扫描 QR 样式", true) { state.qrMenuOpen = false }
        }
    }
}
