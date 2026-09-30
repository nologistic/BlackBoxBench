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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------- Browse tab

@Composable
fun BrowseTab(
    onOpenFolder: (String) -> Unit,
    onOpenInternal: () -> Unit,
) {
    val p = LocalVlcPalette.current
    Column(Modifier.fillMaxSize().verticalScrollable()) {
        SectionHeader("收藏")
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            listOf(
                Triple("Download", 1, "sample_note.txt"),
                Triple("Movies", 1, ".thumbnails"),
                Triple("Music", 1, "sample_audio.wav"),
            ).forEach { (name, count, _) ->
                FolderCard(name, count) { onOpenFolder(name) }
            }
        }
        SectionHeader("存储设备")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onOpenInternal() }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VlcIconView(VlcIcon.FOLDER, 34.dp, p.textSecondary)
            Spacer(Modifier.width(14.dp))
            Column {
                Text("内部存储", color = p.textPrimary, fontSize = 16.sp)
                Text("14 个子文件夹", color = p.textSecondary, fontSize = 13.sp)
            }
        }
        SectionHeader("本地网络")
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("本地网络无连接。", color = p.textPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
            VlcCone(44.dp, p.textSecondary)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FolderCard(name: String, count: Int, onClick: () -> Unit) {
    val p = LocalVlcPalette.current
    Column(
        modifier = Modifier
            .width(112.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(p.chip)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(12.dp),
    ) {
        VlcIconView(VlcIcon.FOLDER, 30.dp, p.textSecondary)
        Spacer(Modifier.height(10.dp))
        Text(name, color = p.textPrimary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("$count 个文件夹", color = p.textSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun Modifier.verticalScrollable(): Modifier =
    this.verticalScroll(rememberScrollState())

// ---------------------------------------------------------------- More tab

@Composable
fun MoreTab(
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenStreams: () -> Unit,
    onOpenHistory: () -> Unit,
    onPlay: (MediaItem) -> Unit,
) {
    val p = LocalVlcPalette.current
    Column(Modifier.fillMaxSize().verticalScrollable()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MoreButton("设置", VlcIcon.SETTINGS, Modifier.weight(1f), onOpenSettings)
            MoreButton("关于", VlcIcon.INFO, Modifier.weight(1f), onOpenAbout)
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(end = 16.dp)) {
            SectionHeader("串流", Modifier.weight(1f))
            IconTap(VlcIcon.ARROW_FORWARD, 20.dp, p.textPrimary, onOpenStreams)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Column(
                modifier = Modifier
                    .width(150.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(p.chip)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onOpenStreams)
                    .padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                VlcIconView(VlcIcon.ADD, 32.dp, p.accent)
                Spacer(Modifier.height(10.dp))
                Text("新建串流", color = p.textPrimary, fontSize = 14.sp)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(end = 16.dp)) {
            SectionHeader("历史", Modifier.weight(1f))
            IconTap(VlcIcon.ARROW_FORWARD, 20.dp, p.textPrimary, onOpenHistory)
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Store.history.mapNotNull { Library.byId(it) }.forEach { item ->
                Column(
                    modifier = Modifier.width(150.dp).clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onPlay(item) },
                ) {
                    Box(Modifier.fillMaxWidth().aspectRatio(1.7f).clip(RoundedCornerShape(6.dp))) {
                        MediaThumb(item, Modifier.fillMaxSize())
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(item.title, color = p.textPrimary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(item.durationLabel, color = p.textSecondary, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun MoreButton(label: String, icon: VlcIcon, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val p = LocalVlcPalette.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(p.chip)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VlcIconView(icon, 22.dp, p.accent)
        Spacer(Modifier.width(12.dp))
        Text(label, color = p.textPrimary, fontSize = 16.sp)
    }
}

// ---------------------------------------------------------------- Overlays

@Composable
fun OverflowMenu(
    onDisplaySettings: () -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
) {
    val p = LocalVlcPalette.current
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize().clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 54.dp, end = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(p.surface)
                .padding(vertical = 6.dp),
        ) {
            Text(
                "显示设置",
                color = p.textPrimary,
                fontSize = 15.sp,
                modifier = Modifier
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDisplaySettings)
                    .padding(horizontal = 22.dp, vertical = 12.dp),
            )
            Row(
                modifier = Modifier
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { Store.incognito.value = !Store.incognito.value }
                    .padding(horizontal = 22.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("无痕模式", color = p.textPrimary, fontSize = 15.sp, modifier = Modifier.width(110.dp))
                VlcCheckbox(Store.incognito.value) { Store.incognito.value = it }
            }
            Text(
                "刷新",
                color = p.textPrimary,
                fontSize = 15.sp,
                modifier = Modifier
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onRefresh)
                    .padding(horizontal = 22.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
fun DisplaySettingsSheet(onDismiss: () -> Unit) {
    val p = LocalVlcPalette.current
    BottomSheet(onDismiss, maxHeight = 620.dp) {
        SheetTitle("显示设置")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    Store.listLayout.value = !Store.listLayout.value
                    Store.persist()
                }
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VlcIconView(VlcIcon.LIST, 22.dp, p.icon)
            Spacer(Modifier.width(18.dp))
            Text("以列表形式显示", color = p.textPrimary, fontSize = 16.sp)
        }
        CheckRow("只显示收藏", checked = Store.onlyFavorites.value) {
            Store.onlyFavorites.value = it; Store.persist()
        }
        SettingRow("视频分组", "按名称分组")
        SettingRow("「播放」动作", Store.playAction.value)
        SectionHeader("排序条件")
        SortCriterion("名称", listOf("A → Z", "Z → A"), Store.sortCriteria.value == "名称") { Store.sortCriteria.value = "名称"; Store.sortAscending.value = it == 0 }
        SortCriterion("长度", listOf("时长短 → 长", "时长长 → 短"), false) { }
        SortCriterion("修改时间", listOf("最旧的优先", "最新的优先"), false) { }
        SortCriterion("轨道编号", listOf("视频较多的群组优先", "视频较少的群组优先"), false) { }
        SortCriterion("插入时间", listOf("最旧的优先", "最新的优先"), false) { }
    }
}

@Composable
private fun SortCriterion(title: String, options: List<String>, selected: Boolean, onSelect: (Int) -> Unit) {
    val p = LocalVlcPalette.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(title, color = p.textSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        options.forEachIndexed { i, option ->
            val active = selected && i == 0
            Row(
                Modifier.fillMaxWidth().clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onSelect(i) }.padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(option, color = p.textPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
                if (active) VlcIconView(VlcIcon.CHECK, 18.dp, p.accent)
            }
        }
    }
}

private val VIDEO_MENU = listOf(
    "播放" to VlcIcon.PLAY,
    "从头播放" to VlcIcon.PLAY,
    "播放全部" to VlcIcon.PLAYLIST,
    "作为音频播放" to VlcIcon.MUSIC,
    "添加到播放队列" to VlcIcon.QUEUE,
    "插入为下一项" to VlcIcon.QUEUE,
    "下载字幕" to VlcIcon.SUBTITLES,
    "信息" to VlcIcon.INFO,
    "添加到播放列表" to VlcIcon.PLAYLIST,
    "设为铃声" to VlcIcon.MUSIC,
    "添加到收藏夹" to VlcIcon.HEART,
    "删除" to VlcIcon.DELETE,
    "分享" to VlcIcon.SHARE,
    "创建启动器快捷方式" to VlcIcon.ADD,
    "添加到视频分组" to VlcIcon.PLAYLIST,
    "自动重新分组" to VlcIcon.REFRESH,
    "标记" to VlcIcon.CHECK,
    "浏览所在目录" to VlcIcon.FOLDER,
)

private val AUDIO_MENU = listOf(
    "播放全部" to VlcIcon.PLAYLIST,
    "添加到播放队列" to VlcIcon.QUEUE,
    "插入为下一项" to VlcIcon.QUEUE,
    "信息" to VlcIcon.INFO,
    "转到专辑" to VlcIcon.RING,
    "转到艺人" to VlcIcon.PERSON,
    "添加到播放列表" to VlcIcon.PLAYLIST,
    "设为铃声" to VlcIcon.MUSIC,
    "添加到收藏夹" to VlcIcon.HEART,
    "删除" to VlcIcon.DELETE,
    "分享" to VlcIcon.SHARE,
    "创建启动器快捷方式" to VlcIcon.ADD,
    "浏览所在目录" to VlcIcon.FOLDER,
)

@Composable
fun MediaContextSheet(
    item: MediaItem,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onInfo: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit,
    onBrowse: () -> Unit,
) {
    val p = LocalVlcPalette.current
    BottomSheet(onDismiss, maxHeight = 640.dp) {
        val entries = if (item.kind == MediaKind.VIDEO) VIDEO_MENU else AUDIO_MENU
        entries.forEach { (label, icon) ->
            val text = if (label == "标记") (if (Store.isWatched(item.id)) "标记为未播放" else "标记为已播放") else label
            var tint: Color? = null
            if (label == "删除") tint = Color(0xFFD32F2F)
            SheetAction(text, icon, tint) {
                when (label) {
                    "播放", "从头播放", "播放全部" -> onPlay()
                    "信息" -> onInfo()
                    "添加到播放列表" -> onAddToPlaylist()
                    "添加到收藏夹" -> onFavorite()
                    "删除" -> onDelete()
                    "浏览所在目录" -> onBrowse()
                    "标记" -> { if (Store.isWatched(item.id)) Store.watched.remove(item.id) else Store.markPlayed(item.id) }
                }
                onDismiss()
            }
        }
    }
}

@Composable
fun NewPlaylistSheet(
    item: MediaItem?,
    playlistId: String?,
    onDismiss: () -> Unit,
    onCreated: () -> Unit,
) {
    val p = LocalVlcPalette.current
    var name by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(playlistId) }
    BottomSheet(onDismiss, maxHeight = 520.dp) {
        SheetTitle("新建播放列表")
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("播放列表名称") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "新建",
                color = p.accent,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        if (name.isNotBlank()) {
                            val pl = Store.createPlaylist(name.trim())
                            selected = pl.id
                            name = ""
                            item?.let { if (!pl.itemIds.contains(it.id)) pl.itemIds.add(it.id) }
                            Store.persist()
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            )
        }
        Spacer(Modifier.height(18.dp))
        SectionHeader("添加到现有播放列表")
        Text(
            if (item != null) "1 个媒体" else "0 个媒体",
            color = p.textSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        CheckRow("替换播放列表", checked = false) { }
        if (Store.playlists.isEmpty()) {
            Text(
                "未找到播放列表。",
                color = p.textSecondary,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        } else {
            Store.playlists.forEach { pl ->
                val active = selected == pl.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { selected = pl.id }
                        .background(if (active) p.accent.copy(alpha = 0.12f) else Color.Transparent)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(38.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFFEDEDED)), contentAlignment = Alignment.Center) {
                        VlcIconView(VlcIcon.PLAYLIST, 20.dp, p.textSecondary)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(pl.name, color = p.textPrimary, fontSize = 16.sp)
                        Text("${pl.itemIds.size} 个媒体", color = p.textSecondary, fontSize = 13.sp)
                    }
                    if (active) VlcIconView(VlcIcon.CHECK, 20.dp, p.accent)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                "保存",
                color = p.accent,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        val target = selected?.let { Store.playlist(it) }
                        if (target != null && item != null && !target.itemIds.contains(item.id)) {
                            target.itemIds.add(item.id)
                            Store.persist()
                        }
                        onCreated()
                        onDismiss()
                    }
                    .padding(horizontal = 40.dp, vertical = 14.dp),
            )
        }
    }
}
