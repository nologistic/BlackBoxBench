package com.blackboxbench.reproduction

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(state: AppState, colors: AppColors, navigator: Navigator) {
    var dialog by remember { mutableStateOf<String?>(null) }
    var libraryTabsDialog by remember { mutableStateOf(false) }
    var blacklistDialog by remember { mutableStateOf(false) }
    var intervalDialog by remember { mutableStateOf<String?>(null) }
    var equalizerDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(colors.background)) {
        SimpleTopBar(title = "设置", colors = colors, onBack = { navigator.pop() })
        Column(Modifier.verticalScroll(rememberScrollState())) {
            SectionHeader("媒体库", colors)
            SettingRow("媒体库类别", colors = colors, onClick = { libraryTabsDialog = true })
            SwitchSettingRow("记住最后显示页面", "启动时打开上次查看的页面", colors, state.rememberLastPage) {
                state.rememberLastPage = it; state.persist()
            }
            SettingRow("黑名单", state.blacklist.firstOrNull() ?: "", colors = colors, onClick = { blacklistDialog = true })
            SwitchSettingRow(
                "白名单",
                if (state.whiteListEnabled) "只显示起始目录：/storage/emulated/0/Music" else "/storage/emulated/0/Music",
                colors, state.whiteListEnabled
            ) { state.whiteListEnabled = it; state.persist() }

            SectionHeader("颜色", colors)
            SettingRow("全局主题", state.ifDark(), colors = colors, onClick = { dialog = "theme" })
            SettingRow("Theme style", state.themeStyle, colors = colors, onClick = { dialog = "style" })
            ColorRow("主色调", state.primaryColor, colors) { dialog = "primary" }
            ColorRow("强调色", state.accentColor, colors) { dialog = "accent" }
            SwitchSettingRow("着色导航栏", "用主色调着色导航栏。", colors, state.tintNavBar) { state.tintNavBar = it; state.persist() }
            SwitchSettingRow("着色应用快捷方式", "用主色调着色应用快捷方式。", colors, state.tintShortcuts) { state.tintShortcuts = it; state.persist() }
            SwitchSettingRow("控件背景透明", "使控件的背景透明显示", colors, state.transparentControls) { state.transparentControls = it; state.persist() }

            SectionHeader("通知", colors)
            SwitchSettingRow("经典通知样式", "使用经典通知样式。", colors, state.classicNotification) { state.classicNotification = it; state.persist() }
            SwitchSettingRow("启用通知背景着色", "使用与专辑封面匹配的颜色着色通知背景色。", colors, state.tintNotification) { state.tintNotification = it; state.persist() }

            SectionHeader("正在播放界面", colors)
            SettingRow("外观", state.nowPlayingAppearance, colors = colors, onClick = { dialog = "appearance" })
            SwitchSettingRow(
                "显示同步歌词", "当前仅支持 LRC 格式同步歌词。内嵌或外置均可。",
                colors, state.showSyncedLyrics
            ) { state.showSyncedLyrics = it; state.persist() }
            SwitchSettingRow("动画显示正在播放歌曲图标", "在歌曲相关视图中动画显示当前歌曲图标", colors, state.animatePlayingIcon) {
                state.animatePlayingIcon = it; state.persist()
            }
            SwitchSettingRow("显示歌曲音轨", "在播放队列和提醒中显示歌曲音轨", colors, state.showTrackNumber) {
                state.showTrackNumber = it; state.persist()
            }
            SettingRow("Default action on choosing/tapping a song while browsing", state.defaultAction, colors = colors, onClick = { dialog = "default" })

            SectionHeader("图片", colors)
            SettingRow("自动下载元数据", state.autoDownload, colors = colors, onClick = { dialog = "download" })

            SectionHeader("声音", colors)
            SwitchSettingRow("音频焦点丢失时降低音量", "通知、导航等", colors, state.duckOnFocusLoss) { state.duckOnFocusLoss = it; state.persist() }
            SwitchSettingRow("无缝播放", "在某些设备上会造成播放问题。", colors, state.gapless) { state.gapless = it; state.persist() }
            SwitchSettingRow("记住随机播放状态", "选择新的歌曲列表时保持随机播放状态", colors, state.rememberShuffle) {
                state.rememberShuffle = it; state.persist()
            }
            SettingRow("均衡器", colors = colors, onClick = { equalizerDialog = true })
            SettingRow("ReplayGain源模式", state.replayGainMode, colors = colors, onClick = { dialog = "replaygain" })
            SettingRow("ReplayGain预放大", state.replayGainPreamp, colors = colors, onClick = { dialog = "preamp" })

            SectionHeader("播放列表", colors)
            SettingRow("最近添加播放列表间隔", state.recentInterval, colors = colors, onClick = { intervalDialog = "recent" })
            SettingRow("最近播放列表间隔", state.historyInterval, colors = colors, onClick = { intervalDialog = "history" })
            SettingRow("最近未播放列表间隔", state.notPlayedInterval, colors = colors, onClick = { intervalDialog = "notplayed" })
            SwitchSettingRow("最喜爱的歌曲", "维护一个最多播放列表", colors, state.favoriteSongsEnabled) {
                state.favoriteSongsEnabled = it; state.persist()
            }
            SwitchSettingRow("跳过的歌曲", "维护一个跳过播放列表", colors, state.skippedSongs) { state.skippedSongs = it; state.persist() }

            SectionHeader("Migrating", colors)
            SettingRow("Export", "导出播放列表与设置", colors = colors, onClick = { dialog = null })
            SettingRow("Import", "导入播放列表与设置", colors = colors, onClick = { dialog = null })

            SectionHeader("Development", colors)
            SwitchSettingRow("Collect crash reports", null, colors, state.crashReports) { state.crashReports = it; state.persist() }
            SectionHeader("Experimental", colors)
            SwitchSettingRow(
                "Sync queue with media tag updates",
                "标签更新后同步播放队列", colors, state.syncQueueWithTags
            ) { state.syncQueueWithTags = it; state.persist() }
            Spacer(Modifier.height(32.dp))
        }
    }

    when (dialog) {
        "theme" -> OptionDialog(
            "全局主题", listOf("浅色", "深色", "自动"), state.ifDark(), colors,
            onDismiss = { dialog = null },
            onSelect = {
                state.darkTheme = it == "深色"
                state.persist()
                dialog = null
            }
        )
        "style" -> OptionDialog(
            "Theme style", listOf("Classic", "Alternative"), state.themeStyle, colors,
            onDismiss = { dialog = null },
            onSelect = { state.themeStyle = it; state.persist(); dialog = null }
        )
        "primary" -> OptionDialog(
            "主色调", listOf("靛蓝", "蓝色", "青色", "绿色", "橙色", "紫色"), state.primaryColor, colors,
            onDismiss = { dialog = null },
            onSelect = { state.primaryColor = it; state.persist(); dialog = null }
        )
        "accent" -> OptionDialog(
            "强调色", listOf("粉色", "红色", "蓝色", "青色", "绿色", "橙色"), state.accentColor, colors,
            onDismiss = { dialog = null },
            onSelect = { state.accentColor = it; state.persist(); dialog = null }
        )
        "appearance" -> OptionDialog(
            "外观", listOf("Card", "Plain", "Blur"), state.nowPlayingAppearance, colors,
            onDismiss = { dialog = null },
            onSelect = { state.nowPlayingAppearance = it; state.persist(); dialog = null }
        )
        "default" -> OptionDialog(
            "Default action on choosing/tapping a song while browsing",
            listOf("播放", "加入播放队列", "作为下一首播放"), state.defaultAction, colors,
            onDismiss = { dialog = null },
            onSelect = { state.defaultAction = it; state.persist(); dialog = null }
        )
        "download" -> OptionDialog(
            "自动下载元数据", listOf("从不", "仅在 WiFi 下", "总是"), state.autoDownload, colors,
            onDismiss = { dialog = null },
            onSelect = { state.autoDownload = it; state.persist(); dialog = null }
        )
        "replaygain" -> OptionDialog(
            "ReplayGain源模式", listOf("无", "音轨", "专辑"), state.replayGainMode, colors,
            onDismiss = { dialog = null },
            onSelect = { state.replayGainMode = it; state.persist(); dialog = null }
        )
        "preamp" -> OptionDialog(
            "ReplayGain预放大", listOf("不启用", "启用"), state.replayGainPreamp, colors,
            onDismiss = { dialog = null },
            onSelect = { state.replayGainPreamp = it; state.persist(); dialog = null }
        )
    }

    if (libraryTabsDialog) {
        LibraryTabsDialog(state, colors, onDismiss = { libraryTabsDialog = false })
    }
    if (blacklistDialog) {
        BlacklistDialog(state, colors, onDismiss = { blacklistDialog = false })
    }
    intervalDialog?.let { kind ->
        IntervalDialog(
            title = when (kind) {
                "recent" -> "最近添加播放列表间隔"
                "history" -> "最近播放列表间隔"
                else -> "最近未播放列表间隔"
            },
            current = when (kind) {
                "recent" -> state.recentInterval
                "history" -> state.historyInterval
                else -> state.notPlayedInterval
            },
            colors = colors,
            onDismiss = { intervalDialog = null },
            onConfirm = { label ->
                when (kind) {
                    "recent" -> state.recentInterval = label
                    "history" -> state.historyInterval = label
                    else -> state.notPlayedInterval = label
                }
                state.persist()
                intervalDialog = null
            }
        )
    }
    if (equalizerDialog) EqualizerDialog(onDismiss = { equalizerDialog = false }, colors = colors)
}

private fun AppState.ifDark(): String = if (darkTheme) "深色" else "浅色"

@Composable
private fun ColorRow(title: String, value: String, colors: AppColors, onClick: () -> Unit) {
    val swatch = when (value) {
        "靛蓝" -> Color(0xFF3F51B5)
        "蓝色" -> Color(0xFF1976D2)
        "青色" -> Color(0xFF00897B)
        "绿色" -> Color(0xFF43A047)
        "橙色" -> Color(0xFFF4511E)
        "紫色" -> Color(0xFF8E24AA)
        "粉色" -> Color(0xFFE91E63)
        "红色" -> Color(0xFFE53935)
        else -> Color(0xFF9E9E9E)
    }
    SettingRow(title, colors = colors, onClick = onClick, trailing = {
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(swatch)
        )
    })
}

@Composable
fun LibraryTabsDialog(state: AppState, colors: AppColors, onDismiss: () -> Unit) {
    val all = listOf("歌曲", "专辑", "艺术家", "音乐类型", "播放列表")
    val checked = remember {
        mutableStateOf(all.map { tab -> tab to state.tabVisibility.contains(tab) }.toMap())
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("媒体库类别", fontSize = 20.sp) },
        text = {
            Column {
                all.forEach { tab ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = checked.value[tab] == true,
                            onCheckedChange = { value ->
                                checked.value = checked.value + (tab to value)
                            },
                            colors = CheckboxDefaults.colors(checkedColor = colors.accent)
                        )
                        Text(tab, fontSize = 16.sp, color = colors.onSurface, modifier = Modifier.weight(1f))
                        DragHandleGlyph(size = 22.dp, tint = colors.secondaryText)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val visible = all.filter { checked.value[it] == true }
                state.tabVisibility = visible
                state.persist()
                onDismiss()
            }) { Text("确定", color = colors.accent) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { checked.value = all.associateWith { true } }) { Text("重置", color = colors.accent) }
                TextButton(onClick = onDismiss) { Text("取消", color = colors.accent) }
            }
        },
        containerColor = colors.surface
    )
}

@Composable
fun BlacklistDialog(state: AppState, colors: AppColors, onDismiss: () -> Unit) {
    var paths by remember { mutableStateOf(state.blacklist) }
    var adding by remember { mutableStateOf(false) }
    var newPath by remember { mutableStateOf("/storage/emulated/0/") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("黑名单", fontSize = 20.sp) },
        text = {
            Column {
                paths.forEach { path ->
                    Text(path, fontSize = 15.sp, color = colors.onSurface, modifier = Modifier.padding(vertical = 8.dp))
                }
                if (adding) {
                    OutlinedTextField(
                        value = newPath,
                        onValueChange = { newPath = it },
                        label = { Text("文件夹路径") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (adding) {
                    paths = paths + newPath
                    adding = false
                } else {
                    state.blacklist = paths
                    state.persist()
                    onDismiss()
                }
            }) { Text(if (adding) "添加" else "确定", color = colors.accent) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { paths = emptyList() }) { Text("清空", color = colors.accent) }
                TextButton(onClick = { if (!adding) adding = true else onDismiss() }) {
                    Text(if (adding) "取消" else "添加", color = colors.accent)
                }
            }
        },
        containerColor = colors.surface
    )
}

@Composable
fun IntervalDialog(
    title: String,
    current: String,
    colors: AppColors,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var weeks by remember { mutableStateOf("100") }
    var months by remember { mutableStateOf("1") }
    var years by remember { mutableStateOf("2") }
    var disabled by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontSize = 20.sp) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = weeks,
                        onValueChange = { weeks = it },
                        label = { Text("周") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = months,
                        onValueChange = { months = it },
                        label = { Text("月") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = years,
                        onValueChange = { years = it },
                        label = { Text("年") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = disabled,
                        onCheckedChange = { disabled = it },
                        colors = CheckboxDefaults.colors(checkedColor = colors.accent)
                    )
                    Text("不启用", fontSize = 15.sp, color = colors.onSurface)
                }
                Text("当前：$current", fontSize = 13.sp, color = colors.secondaryText)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val label = when {
                    disabled -> "不启用"
                    months != "1" -> "$months 月"
                    weeks != "100" -> "$weeks 周"
                    else -> "$years 年份"
                }
                onConfirm(label)
            }) { Text("确定", color = colors.accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = colors.accent) } },
        containerColor = colors.surface
    )
}

@Composable
fun AboutScreen(state: AppState, colors: AppColors, navigator: Navigator, onOpenIntro: () -> Unit) {
    var dialog by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().background(colors.background)) {
        SimpleTopBar(title = "关于", colors = colors, onBack = { navigator.pop() })
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(24.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(colors.primary),
                    contentAlignment = Alignment.Center
                ) { VinylIcon(size = 64.dp, tint = Color.White) }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Vinyl Music Player",
                color = colors.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                "版本 1.11.0",
                color = colors.secondaryText,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            SettingRow("更新日志", colors = colors, onClick = { dialog = "changelog" })
            SettingRow("介绍页", colors = colors, onClick = onOpenIntro)
            SettingRow("创建 GitHub 分支", colors = colors, onClick = { dialog = "fork" })
            SettingRow("许可信息", colors = colors, onClick = { dialog = "licenses" })
            SectionHeader("支持开发者", colors)
            SettingRow("提交 bug", colors = colors, onClick = { dialog = "bug" })
            SettingRow("评分", colors = colors, onClick = { dialog = "rate" })
            SectionHeader("Maintainers", colors)
            listOf("AdrienPoupa", "Octoton", "soncaokim").forEach { name ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(colors.surface)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(coverColor(name)),
                        contentAlignment = Alignment.Center
                    ) { ArtistGlyph(size = 24.dp, tint = Color(0xFFEEEEEE)) }
                    Spacer(Modifier.width(16.dp))
                    Text(name, color = colors.onSurface, fontSize = 16.sp)
                }
            }
            SectionHeader("Contributors", colors)
            listOf("BlackBoxBench").forEach { name ->
                Text(
                    name,
                    color = colors.onSurface,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surface)
                        .padding(16.dp)
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
    when (dialog) {
        "changelog" -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("更新日志", fontSize = 20.sp) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("## 1.11.0\n- 改进播放队列\n- 修复标签编辑器问题\n\n## 1.10.0\n- 新增睡眠定时器", fontSize = 15.sp, color = colors.onSurface)
                }
            },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text("确定", color = colors.accent) } },
            containerColor = colors.surface
        )
        "fork" -> ConfirmDialog("创建 GitHub 分支", "此操作需要联网访问 GitHub。", "确定", colors, onDismiss = { dialog = null }, onConfirm = { dialog = null })
        "licenses" -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("许可信息", fontSize = 20.sp) },
            text = { Text("GNU General Public License v3.0", fontSize = 15.sp, color = colors.onSurface) },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text("确定", color = colors.accent) } },
            containerColor = colors.surface
        )
        "bug" -> ConfirmDialog("提交 bug", "此操作需要联网访问 GitHub。", "确定", colors, onDismiss = { dialog = null }, onConfirm = { dialog = null })
        "rate" -> ConfirmDialog("评分", "此操作需要访问应用商店。", "确定", colors, onDismiss = { dialog = null }, onConfirm = { dialog = null })
    }
}

@Composable
fun TagEditorScreen(state: AppState, colors: AppColors, trackId: String, onDone: () -> Unit) {
    val track = state.tracks.firstOrNull { it.id == trackId }
    if (track == null) {
        onDone()
        return
    }
    var title by remember { mutableStateOf(track.title) }
    var album by remember { mutableStateOf(track.album) }
    var artist by remember { mutableStateOf(track.artist) }
    var genre by remember { mutableStateOf(track.genre) }
    var year by remember { mutableStateOf(if (track.year > 0) track.year.toString() else "") }
    var trackNo by remember { mutableStateOf(track.trackNo.toString()) }
    var discNo by remember { mutableStateOf(track.discNo.toString()) }
    var lyrics by remember { mutableStateOf(track.lyrics) }
    var permission by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(colors.background)) {
        SimpleTopBar(title = "音乐标签编辑器", colors = colors, onBack = onDone)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            OutlinedTextField(title, { title = it }, label = { Text("歌曲") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(album, { album = it }, label = { Text("专辑") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(artist, { artist = it }, label = { Text("作者（每行一个）") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(genre, { genre = it }, label = { Text("Genres (one per line)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(year, { year = it }, label = { Text("年份") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Row {
                OutlinedTextField(trackNo, { trackNo = it }, label = { Text("音轨") }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(12.dp))
                OutlinedTextField(discNo, { discNo = it }, label = { Text("光碟号") }, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(lyrics, { lyrics = it }, label = { Text("歌词") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(24.dp))
        }
        Row(
            Modifier
                .fillMaxWidth()
                .background(colors.primary)
                .padding(16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(colors.accent)
                    .clickable { permission = true },
                contentAlignment = Alignment.Center
            ) { CheckMarkGlyph() }
        }
    }

    if (permission) {
        ConfirmDialog(
            title = "允许 Vinyl Music Player 修改这个音频文件吗？",
            message = "修改后，音乐文件的标签信息将被更新。",
            confirmLabel = "允许",
            colors = colors,
            onDismiss = { permission = false },
            onConfirm = {
                permission = false
                state.updateTags(
                    trackId,
                    title,
                    album,
                    artist,
                    genre,
                    year.toIntOrNull() ?: track.year,
                    trackNo.toIntOrNull() ?: track.trackNo,
                    discNo.toIntOrNull() ?: track.discNo,
                    lyrics
                )
                onDone()
            }
        )
    }
}

@Composable
fun CheckMarkGlyph() {
    val white = Color.White
    androidx.compose.foundation.Canvas(Modifier.size(24.dp)) {
        val w = size.minDimension * 0.12f
        drawLine(white, Offset(size.width * 0.22f, size.height * 0.52f), Offset(size.width * 0.42f, size.height * 0.72f), strokeWidth = w)
        drawLine(white, Offset(size.width * 0.42f, size.height * 0.72f), Offset(size.width * 0.78f, size.height * 0.28f), strokeWidth = w)
    }
}
