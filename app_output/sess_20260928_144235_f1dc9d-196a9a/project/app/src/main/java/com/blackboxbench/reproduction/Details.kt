package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
private fun DetailRow(label: String, value: String) {
    val p = LocalVlcPalette.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp)) {
        Text(label, color = p.textSecondary, fontSize = 14.sp, modifier = Modifier.width(110.dp))
        Text(value, color = p.textPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
fun MediaInfoScreen(item: MediaItem, onBack: () -> Unit, onPlay: () -> Unit) {
    val p = LocalVlcPalette.current
    Box(Modifier.fillMaxSize().background(p.background)) {
        Column(Modifier.fillMaxSize()) {
            TopBar(title = item.fileName, showLogo = false, onBack = onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Box(Modifier.fillMaxWidth().padding(20.dp).aspectRatio(1.7f)) {
                    MediaThumb(item, Modifier.fillMaxSize(), corner = 8)
                }
                Text(item.title, color = p.textPrimary, fontSize = 19.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(6.dp))
                Text(item.path, color = p.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(18.dp))
                if (item.kind == MediaKind.VIDEO) {
                    DetailRow("长度", item.durationLabel)
                    DetailRow("文件大小", item.sizeLabel)
                    SectionHeader("视频轨")
                    DetailRow("率", item.videoBitrate)
                    DetailRow("格式", item.videoCodec)
                    DetailRow("分辨率", "${item.width}×${item.height}")
                    DetailRow("帧率", item.frameRate)
                    SectionHeader("音频轨")
                    DetailRow("率", item.audioBitrate)
                    DetailRow("格式", item.audioCodec)
                    DetailRow("声道", item.channels)
                    DetailRow("采样率", item.sampleRate)
                } else {
                    DetailRow("长度", item.durationLabel)
                    DetailRow("文件大小", item.sizeLabel)
                    SectionHeader("音频轨")
                    DetailRow("率", item.audioBitrate)
                    DetailRow("格式", item.audioCodec)
                    DetailRow("声道", item.channels)
                    DetailRow("采样率", item.sampleRate)
                }
                Spacer(Modifier.height(110.dp))
            }
        }
        Box(Modifier.align(Alignment.BottomEnd).padding(20.dp)) { Fab(VlcIcon.PLAY, onPlay) }
    }
}

@Composable
fun FileBrowserScreen(
    folder: String?,
    onBack: () -> Unit,
    onOpenFolder: (String) -> Unit,
    onOpenItem: (MediaItem) -> Unit,
    onMenu: (MediaItem) -> Unit,
) {
    val p = LocalVlcPalette.current
    val crumb = if (folder == null) "浏览器 › 内部存储" else "浏览器 › 内部存储 › $folder"
    Column(Modifier.fillMaxSize().background(p.background)) {
        TopBar(title = crumb, showLogo = false, onBack = onBack)
        if (folder == null) {
            LazyColumn(Modifier.fillMaxSize()) {
                items(Library.rootFolders) { f ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onOpenFolder(f.name) }
                            .padding(horizontal = 16.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        VlcIconView(VlcIcon.FOLDER, 28.dp, p.textSecondary)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(f.name, color = p.textPrimary, fontSize = 16.sp)
                            if (f.childCount != null) Text("${f.childCount} 个子文件夹", color = p.textSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
        } else {
            val files = Library.folderFiles(folder)
            if (files.isEmpty()) {
                EmptyState("此文件夹为空。")
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(files) { name ->
                        val item = Library.all.firstOrNull { it.fileName == name }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    if (item != null) onOpenItem(item)
                                }
                                .padding(horizontal = 16.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            VlcIconView(if (item?.kind == MediaKind.AUDIO) VlcIcon.MUSIC else VlcIcon.VIDEO, 26.dp, p.textSecondary)
                            Spacer(Modifier.width(16.dp))
                            Text(name, color = p.textPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f))
                            if (item != null) IconTap(VlcIcon.MORE, 20.dp, p.textSecondary) { onMenu(item) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistDetailScreen(pl: Playlist, onBack: () -> Unit, onPlay: (MediaItem) -> Unit) {
    val p = LocalVlcPalette.current
    val items = pl.itemIds.mapNotNull { Library.byId(it) }
    Box(Modifier.fillMaxSize().background(p.background)) {
        Column(Modifier.fillMaxSize()) {
            Box {
                Column(Modifier.fillMaxWidth().height(190.dp).background(p.accent.copy(alpha = 0.25f))) {}
                TopBar(title = "", showLogo = false, onBack = onBack)
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(96.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFFEDEDED))) {
                    items.firstOrNull()?.let { TestPattern(it.pattern, Modifier.fillMaxSize()) }
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(pl.name, color = p.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                    Text("${items.sumOf { it.durationSec }}s", color = p.textSecondary, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).clip(CircleShape).background(p.accent).clickable(
                    interactionSource = remember { MutableInteractionSource() }, indication = null,
                ) { items.firstOrNull()?.let(onPlay) }, contentAlignment = Alignment.Center) {
                    VlcIconView(VlcIcon.SHUFFLE, 26.dp, Color.White)
                }
                Spacer(Modifier.width(18.dp))
                IconTap(VlcIcon.QUEUE, 26.dp, p.textPrimary, {})
                Spacer(Modifier.width(18.dp))
                IconTap(VlcIcon.HEART, 26.dp, p.textPrimary, {})
            }
            Spacer(Modifier.height(10.dp))
            HLine()
            LazyColumn(Modifier.weight(1f)) {
                items(items, key = { it.id }) { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onPlay(item) }.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        VlcIconView(VlcIcon.DRAG, 22.dp, p.textSecondary)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.title, color = p.textPrimary, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${item.durationLabel} · ${item.badge}", color = p.textSecondary, fontSize = 12.sp)
                        }
                        IconTap(VlcIcon.MORE, 20.dp, p.textSecondary, {})
                    }
                }
            }
        }
    }
}

@Composable
fun SearchScreen(onBack: () -> Unit, onOpenItem: (MediaItem) -> Unit, onMenu: (MediaItem) -> Unit) {
    val p = LocalVlcPalette.current
    var query by remember { mutableStateOf("") }
    var global by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(p.background)) {
        TopBar(title = "", showLogo = false, onBack = onBack)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(if (global) "在所有媒体库中搜索" else "在当前列表中搜索") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(p.chip)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { global = !global }
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) { Text("在所有媒体库中搜索媒体", color = p.textPrimary, fontSize = 13.sp) }
        Spacer(Modifier.height(8.dp))
        val results = Library.all.filter { query.isNotEmpty() && it.title.contains(query, ignoreCase = true) }
        val videos = results.filter { it.kind == MediaKind.VIDEO }
        val tracks = results.filter { it.kind == MediaKind.AUDIO }
        LazyColumn(Modifier.fillMaxSize()) {
            if (videos.isNotEmpty()) {
                item { SectionHeader("视频") }
                items(videos, key = { it.id }) { item -> MediaListRow(item, { onOpenItem(item) }, { onMenu(item) }) }
            }
            if (tracks.isNotEmpty()) {
                item { SectionHeader("轨道") }
                items(tracks, key = { it.id }) { item -> MediaListRow(item, { onOpenItem(item) }, { onMenu(item) }) }
            }
            if (query.isNotEmpty() && results.isEmpty()) {
                item {
                    Text(
                        "未找到结果",
                        color = p.textSecondary,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun StreamsScreen(onBack: () -> Unit) {
    val p = LocalVlcPalette.current
    var url by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(p.background)) {
        TopBar(title = "新建串流", showLogo = false, onBack = onBack)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            placeholder = { Text("输入网络串流地址") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(p.accent)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) { VlcIconView(VlcIcon.PLAY, 28.dp, Color.White) }
    }
}

@Composable
fun HistoryScreen(onBack: () -> Unit, onOpenItem: (MediaItem) -> Unit, onClear: () -> Unit) {
    val p = LocalVlcPalette.current
    val items = Store.history.mapNotNull { Library.byId(it) }
    Column(Modifier.fillMaxSize().background(p.background)) {
        TopBar(
            title = "历史", showLogo = false, onBack = onBack,
            actions = listOf(
                TopAction(VlcIcon.SEARCH, onClick = {}),
                TopAction(VlcIcon.DELETE, onClear),
                TopAction(VlcIcon.MORE, onClick = {}),
            ),
        )
        if (items.isEmpty()) {
            EmptyState("暂无历史记录。")
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(items, key = { it.id }) { item -> MediaListRow(item, { onOpenItem(item) }, {}) }
            }
        }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val p = LocalVlcPalette.current
    Column(Modifier.fillMaxSize().background(p.background)) {
        TopBar(title = "关于", showLogo = false, onBack = onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            VlcCone(110.dp, p.accent)
            Spacer(Modifier.height(18.dp))
            Text("VLC for Android", color = p.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(14.dp))
            Text(
                "VLC media player 是一款自由、开源的跨平台多媒体播放器，可播放大多数多媒体文件、光盘、设备以及网络串流。",
                color = p.textPrimary, fontSize = 14.sp, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))
            Text("Version 3.7.1 2026-05-09", color = p.textSecondary, fontSize = 14.sp)
            Spacer(Modifier.height(26.dp))
            listOf("官方网站", "发送反馈", "源代码", "库", "作者").forEach { label ->
                Text(
                    label,
                    color = p.accent,
                    fontSize = 15.sp,
                    modifier = Modifier.fillMaxWidth().clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {}.padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(20.dp))
            Text("Copyright © 1996-2026 by VideoLAN. All rights reserved.", color = p.textSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("GNU General Public License v2.0", color = p.textSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(30.dp))
        }
    }
}
