package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------------------
// settings
// ---------------------------------------------------------------------------

@Composable
fun SettingsScreen(app: AppState) {
    val colors = LocalViColors.current
    val dialogs = LocalDialogs.current
    val nav = LocalNav.current
    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        ViTopBar(title = "设置", onBack = { nav.pop() })
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item { SectionHeader("媒体库") }
            item {
                PlainRow("媒体库类别", "配置媒体库类别可见性和顺序。") { dialogs.categories = true }
            }
            item {
                SwitchRow("记住最后显示页面", "启动时回到上次打开的视图", app.rememberLastPage) {
                    app.rememberLastPage = it; app.persistSettings()
                }
            }
            item {
                PlainRow("黑名单", "黑名单中文件夹将在媒体库中隐藏。") { dialogs.blacklistInfo = true }
            }
            item {
                SwitchRow("白名单", "只显示起始目录: /storage/emulated/0/Music", app.whitelist) {
                    app.whitelist = it; app.persistSettings()
                }
            }
            item { ViDivider() }
            item { SectionHeader("颜色") }
            item { PlainRow("全局主题", app.themeMode) { dialogs.themePicker = true } }
            item {
                PlainRow("Theme style", app.themeStyle) {
                    app.themeStyle = when (app.themeStyle) {
                        "Classic" -> "Full"
                        "Full" -> "Blur"
                        else -> "Classic"
                    }
                    app.persistSettings()
                }
            }
            item {
                PlainRow("主色调", "主题主色调，默认为靛蓝色。", trailing = { ColorDot(Color(0xFF3F51B5)) })
            }
            item {
                PlainRow("强调色", "主题强调色，默认为粉色。", trailing = { ColorDot(Color(0xFFF50057)) })
            }
            item {
                SwitchRow("着色导航栏", "用主色调着色导航栏。", app.colorizeNavBar) {
                    app.colorizeNavBar = it; app.persistSettings()
                }
            }
            item {
                SwitchRow("着色应用快捷方式", "用主色调着色应用快捷方式。", app.colorizeShortcuts) {
                    app.colorizeShortcuts = it; app.persistSettings()
                }
            }
            item {
                SwitchRow("控件背景透明", "使正在播放界面的控件背景透明。", app.transparentControls) {
                    app.transparentControls = it; app.persistSettings()
                }
            }
            item { ViDivider() }
            item { SectionHeader("通知") }
            item {
                SwitchRow("经典通知样式", null, app.classicNotification) {
                    app.classicNotification = it; app.persistSettings()
                }
            }
            item {
                SwitchRow("启用通知背景着色", null, app.colorizedNotification) {
                    app.colorizedNotification = it; app.persistSettings()
                }
            }
            item { ViDivider() }
            item { SectionHeader("正在播放界面") }
            item {
                PlainRow("外观", app.nowPlayingLook) {
                    app.nowPlayingLook = when (app.nowPlayingLook) {
                        "Card" -> "Blur"
                        "Blur" -> "Full"
                        else -> "Card"
                    }
                    app.persistSettings()
                }
            }
            item {
                SwitchRow("显示同步歌词", null, app.showLyrics) { app.showLyrics = it; app.persistSettings() }
            }
            item {
                SwitchRow("动画显示正在播放歌曲图标", null, app.animatedIcon) {
                    app.animatedIcon = it; app.persistSettings()
                }
            }
            item {
                SwitchRow("显示歌曲音轨", null, app.showTrackNumber) {
                    app.showTrackNumber = it; app.persistSettings()
                }
            }
            item {
                PlainRow("Default action…", app.defaultAction) {
                    app.defaultAction = if (app.defaultAction == "播放") "添加到队列" else "播放"
                    app.persistSettings()
                }
            }
            item { ViDivider() }
            item { SectionHeader("图片") }
            item {
                PlainRow("自动下载元数据", app.autoDownload) {
                    app.autoDownload = when (app.autoDownload) {
                        "从不" -> "仅在 WiFi 下"
                        "仅在 WiFi 下" -> "总是"
                        else -> "从不"
                    }
                    app.persistSettings()
                }
            }
            item { ViDivider() }
            item { SectionHeader("声音") }
            item {
                SwitchRow("音频焦点丢失时降低音量", null, app.duckOnFocusLoss) {
                    app.duckOnFocusLoss = it; app.persistSettings()
                }
            }
            item { SwitchRow("无缝播放", null, app.gapless) { app.gapless = it; app.persistSettings() } }
            item {
                SwitchRow("记住随机播放状态", null, app.rememberShuffle) {
                    app.rememberShuffle = it; app.persistSettings()
                }
            }
            item { PlainRow("均衡器", null) { dialogs.equalizer = true } }
            item { PlainRow("ReplayGain源模式", app.replayGainMode) { dialogs.replayGain = true } }
            item {
                PlainRow("ReplayGain预放大", app.replayGainPreamp) {
                    app.replayGainPreamp = if (app.replayGainPreamp == "不启用") "启用" else "不启用"
                    app.persistSettings()
                }
            }
            item { ViDivider() }
            item { SectionHeader("播放列表") }
            item {
                PlainRow("最近添加/最近播放/最近未播放列表间隔", app.recentInterval) {
                    app.recentInterval = when (app.recentInterval) {
                        "本周" -> "本月"
                        "本月" -> "全部"
                        else -> "本周"
                    }
                    app.persistSettings()
                }
            }
            item {
                SwitchRow("最喜爱的歌曲", "维护一个最多播放列表", app.favoritePlaylist) {
                    app.favoritePlaylist = it; app.persistSettings()
                }
            }
            item { SwitchRow("跳过的歌曲", null, app.skippedSongs) { app.skippedSongs = it; app.persistSettings() } }
            item { ViDivider() }
            item { SectionHeader("Migrating") }
            item { PlainRow("Export…", null) { dialogs.exportInfo = true } }
            item { PlainRow("Import…", null) { dialogs.exportInfo = true } }
            item { ViDivider() }
            item { SectionHeader("Development") }
            item {
                SwitchRow("Collect crash reports", null, app.crashReports) {
                    app.crashReports = it; app.persistSettings()
                }
            }
            item { SectionHeader("Experimental") }
            item {
                SwitchRow("Sync queue with media tag updates", null, app.syncQueueTag) {
                    app.syncQueueTag = it; app.persistSettings()
                }
            }
        }
    }
}

@Composable
fun ThemePickerDialog(app: AppState, onDismiss: () -> Unit) {
    val options = listOf(
        "浅色", "暗色", "黑色（AMOLED）",
        "Follow system theme\n浅色 · 暗色",
        "Follow system theme\n浅色 · 黑色（AMOLED）",
        "Follow system theme\n浅色 · 暗色",
        "Follow system theme\n浅色 · 黑色（AMOLED）"
    )
    ViDialog(onDismiss = onDismiss) {
        DialogTitle("全局主题")
        LazyColumn(modifier = Modifier.height(430.dp)) {
            items(options) { option ->
                DialogRadioRow(
                    text = option,
                    selected = app.themeMode == option
                ) {
                    app.themeMode = option.substringBefore("\n")
                    app.persistSettings()
                    onDismiss()
                }
            }
        }
    }
}

@Composable
fun LibraryCategoriesDialog(app: AppState, onDismiss: () -> Unit) {
    val colors = LocalViColors.current
    val items = remember {
        mutableStateListOf(
            LibraryCategory("songs", "歌曲", true),
            LibraryCategory("albums", "专辑", true),
            LibraryCategory("artists", "艺术家", true),
            LibraryCategory("genres", "音乐类型", true),
            LibraryCategory("playlists", "播放列表", true)
        )
    }
    ViDialog(onDismiss = onDismiss) {
        DialogTitle("媒体库类别")
        Box(modifier = Modifier.height(330.dp).fillMaxWidth()) {
            LazyColumn {
                itemsIndexed(items) { index, category ->
                    var drag by remember { mutableStateOf(0f) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (category.visible) colors.accent else colors.divider),
                            contentAlignment = Alignment.Center
                        ) {
                            if (category.visible) Ic("check", 14.dp, Color.White)
                        }
                        Spacer(Modifier.width(20.dp))
                        Text(
                            category.label,
                            color = colors.text,
                            fontSize = 17.sp,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    items[index] = category.copy(visible = !category.visible)
                                }
                        )
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .pointerInput(index) {
                                    detectDragGestures(
                                        onDragEnd = { drag = 0f },
                                        onDrag = { _, amount ->
                                            drag += amount.y
                                            if (drag < -40f && index > 0) {
                                                val moved = items.removeAt(index)
                                                items.add(index - 1, moved)
                                                drag = 0f
                                            } else if (drag > 40f && index < items.lastIndex) {
                                                val moved = items.removeAt(index)
                                                items.add(index + 1, moved)
                                                drag = 0f
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Ic("drag", 22.dp, colors.subtitle)
                        }
                    }
                }
            }
        }
        ViDivider()
        DialogActions(
            "重置" to {
                items.clear()
                items.addAll(
                    listOf(
                        LibraryCategory("songs", "歌曲", true),
                        LibraryCategory("albums", "专辑", true),
                        LibraryCategory("artists", "艺术家", true),
                        LibraryCategory("genres", "音乐类型", true),
                        LibraryCategory("playlists", "播放列表", true)
                    )
                )
            },
            "取消" to onDismiss,
            "确定" to {
                val order = items.filter { it.visible }.map {
                    when (it.key) {
                        "songs" -> "歌曲"
                        "albums" -> "专辑"
                        "artists" -> "艺术家"
                        "genres" -> "音乐类型"
                        else -> "播放列表"
                    }
                }
                app.tabs = order
                app.hiddenTabs = items.filterNot { it.visible }.map {
                    when (it.key) {
                        "songs" -> "歌曲"
                        "albums" -> "专辑"
                        "artists" -> "艺术家"
                        "genres" -> "音乐类型"
                        else -> "播放列表"
                    }
                }
                app.persistSettings()
                onDismiss()
            }
        )
    }
}

@Composable
fun ReplayGainDialog(app: AppState, onDismiss: () -> Unit) {
    val options = listOf("无", "音轨", "专辑")
    ViDialog(onDismiss = onDismiss) {
        DialogTitle("ReplayGain源模式")
        options.forEach { option ->
            DialogRadioRow(option, app.replayGainMode == option) {
                app.replayGainMode = option
                app.persistSettings()
                onDismiss()
            }
        }
    }
}

@Composable
fun MessageDialog(title: String, message: String, onDismiss: () -> Unit) {
    ViDialog(onDismiss = onDismiss) {
        DialogTitle(title)
        DialogText(message)
        DialogActions("确定" to onDismiss)
    }
}

// ---------------------------------------------------------------------------
// about
// ---------------------------------------------------------------------------

@Composable
fun AboutScreen(app: AppState) {
    val colors = LocalViColors.current
    val dialogs = LocalDialogs.current
    val nav = LocalNav.current
    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        ViTopBar(title = "关于", onBack = { nav.pop() })
        LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.card)
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        VinylArt(56.dp)
                        Spacer(Modifier.width(20.dp))
                        Text("Vinyl Music Player", color = colors.text, fontSize = 22.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    AboutRow("info", "版本", "1.11.0")
                    AboutRow("timer", "更新日志", null) { dialogs.changelog = true }
                    AboutRow("check", "介绍页", null) { nav.push("intro") }
                    AboutRow("share", "创建 GitHub 分支", null) { app.snackbar = "创建 GitHub 分支" }
                    AboutRow("info", "许可信息", null) { dialogs.licenses = true }
                }
            }
            item {
                Spacer(Modifier.height(14.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.card)
                        .padding(16.dp)
                ) {
                    Text("支持开发者", color = colors.accent, fontSize = 15.sp)
                    Spacer(Modifier.height(10.dp))
                    AboutRow("share", "提交 bug", "提交 bug 或申请添加新功能") { app.snackbar = "提交 bug" }
                    AboutRow("star", "评分", "如果喜欢 Vinyl Music Player 请在 Play Store 中留下您的好评。") {
                        app.snackbar = "评分"
                    }
                }
            }
            item {
                Spacer(Modifier.height(14.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.card)
                        .padding(16.dp)
                ) {
                    Text("Maintainers", color = colors.subtitle, fontSize = 15.sp)
                    listOf("AdrienPoupa", "Octoton", "soncaokim").forEach {
                        Row(modifier = Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            PersonArt(40.dp)
                            Spacer(Modifier.width(16.dp))
                            Text(it, color = colors.text, fontSize = 17.sp)
                        }
                    }
                    Text("Contributors", color = colors.subtitle, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun AboutRow(icon: String, title: String, subtitle: String?, onClick: (() -> Unit)? = null) {
    val colors = LocalViColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Ic(icon, 26.dp, colors.icon)
        Spacer(Modifier.width(24.dp))
        Column {
            Text(title, color = colors.text, fontSize = 17.sp)
            if (subtitle != null) Text(subtitle, color = colors.subtitle, fontSize = 14.sp)
        }
    }
}

@Composable
fun ChangelogDialog(onDismiss: () -> Unit) {
    val colors = LocalViColors.current
    ViDialog(onDismiss = onDismiss) {
        DialogTitle("[1.11.0] - 2024-08-18")
        LazyColumn(modifier = Modifier.height(400.dp)) {
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text("Features", color = colors.accent, fontSize = 16.sp)
                    Text("• 新增引导页介绍播放队列手势", color = colors.text, fontSize = 15.sp)
                    Text("• 支持拖动序号调整播放队列顺序", color = colors.text, fontSize = 15.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Fixes", color = colors.accent, fontSize = 16.sp)
                    Text("• 修复暗色主题下歌词对比度", color = colors.text, fontSize = 15.sp)
                    Text("• 修复文件夹视图面包屑返回", color = colors.text, fontSize = 15.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Other Changes", color = colors.accent, fontSize = 16.sp)
                    Text("• 更新许可信息", color = colors.text, fontSize = 15.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("New Contributors", color = colors.accent, fontSize = 16.sp)
                    Text("• 感谢所有贡献者", color = colors.text, fontSize = 15.sp)
                }
            }
        }
        DialogActions("确定" to onDismiss)
    }
}

@Composable
fun LicensesDialog(onDismiss: () -> Unit) {
    val colors = LocalViColors.current
    ViDialog(onDismiss = onDismiss) {
        DialogTitle("Licenses")
        LazyColumn(modifier = Modifier.height(420.dp)) {
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text("Vinyl Music Player", color = colors.text, fontSize = 18.sp)
                    Text("• GNU General Public License v3", color = colors.text, fontSize = 15.sp)
                    Spacer(Modifier.height(14.dp))
                    Text("Eleven", color = colors.text, fontSize = 18.sp)
                    Text("Copyright The CyanogenMod Project", color = colors.subtitle, fontSize = 14.sp)
                    Text("• Apache License 2.0", color = colors.text, fontSize = 15.sp)
                    Spacer(Modifier.height(14.dp))
                    Text("StackBlur", color = colors.text, fontSize = 18.sp)
                    Text("Copyright Enrique López Mañas", color = colors.subtitle, fontSize = 14.sp)
                    Text("• Apache License 2.0", color = colors.text, fontSize = 15.sp)
                    Spacer(Modifier.height(14.dp))
                    Text("The Android Open Source Project", color = colors.text, fontSize = 18.sp)
                    Text("Copyright The Android Open Source Project", color = colors.subtitle, fontSize = 14.sp)
                    Text("• Apache License 2.0", color = colors.text, fontSize = 15.sp)
                }
            }
        }
        DialogActions("确定" to onDismiss)
    }
}

// ---------------------------------------------------------------------------
// drawer + storage permission guide
// ---------------------------------------------------------------------------

@Composable
fun DrawerScreen(app: AppState, onClose: () -> Unit, onOpenSettings: () -> Unit, onOpenAbout: () -> Unit, onOpenFolders: () -> Unit) {
    val colors = LocalViColors.current
    val song = app.current
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x99000000))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClose() }
        )
        Column(
            modifier = Modifier
                .width(280.dp)
                .fillMaxHeight()
                .background(colors.card)
                .navigationBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF232323)).padding(16.dp)) {
                if (song != null) {
                    VinylArt(120.dp)
                    Spacer(Modifier.height(12.dp))
                    Text(song.title, color = Color.White, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(songSubtitle(song), color = Color(0xFFBDBDBD), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                } else {
                    VinylArt(120.dp)
                    Spacer(Modifier.height(12.dp))
                    Text("Vinyl Music Player", color = Color.White, fontSize = 18.sp)
                }
            }
            DrawerItem("album", "媒体库", highlighted = true) { onClose() }
            DrawerItem("folder", "文件夹", highlighted = false) { onClose(); onOpenFolders() }
            ViDivider()
            DrawerItem("refresh", "重新扫描媒体库", highlighted = false) {
                app.snackbar = "正在扫描媒体库…"
                onClose()
            }
            ViDivider()
            DrawerItem("gear", "设置", highlighted = false) { onClose(); onOpenSettings() }
            DrawerItem("info", "关于", highlighted = false) { onClose(); onOpenAbout() }
        }
    }
}

@Composable
fun DrawerItem(icon: String, label: String, highlighted: Boolean, onClick: () -> Unit) {
    val colors = LocalViColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(if (highlighted) colors.highlighted else Color.Transparent)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Ic(icon, 26.dp, if (highlighted) colors.accent else colors.icon)
        Spacer(Modifier.width(28.dp))
        Text(
            label,
            color = if (highlighted) colors.accent else colors.text,
            fontSize = 17.sp,
            fontWeight = if (highlighted) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
fun SdCardGuideScreen(onBack: () -> Unit) {
    val colors = LocalViColors.current
    var page by remember { mutableStateOf(0) }
    var drag by remember { mutableStateOf(0f) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF6C7BD1))
            .statusBarsPadding()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (drag < -60f) page = 1
                        if (drag > 60f) page = 0
                        drag = 0f
                    },
                    onDrag = { _, amount -> drag += amount.x }
                )
            }
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButtonNoRipple("back", Color.White, onBack)
        }
        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White)
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (page == 0) "Recent" else "Open from",
                    color = Color(0xFF1565C0),
                    fontSize = 20.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(Color(0xFFF0F0F0)),
                    contentAlignment = Alignment.Center
                ) {
                    if (page == 0) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Ic("folder", 60.dp, Color(0xFFBDBDBD))
                            Spacer(Modifier.height(10.dp))
                            Text("No items", color = Color(0xFF616161), fontSize = 15.sp)
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                            Text("Recent", color = Color(0xFF616161), fontSize = 15.sp)
                            Spacer(Modifier.height(10.dp))
                            Text("Android SDK built for x86", color = Color(0xFF212121), fontSize = 15.sp)
                            Text("686 MB free", color = Color(0xFF757575), fontSize = 13.sp)
                            Spacer(Modifier.height(10.dp))
                            Text("SDCARD", color = Color(0xFF212121), fontSize = 15.sp)
                            Text("148 MB free", color = Color(0xFF757575), fontSize = 13.sp)
                        }
                    }
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (page == 0) {
                Text("Vinyl Music Player需要SD卡权限", color = Color.White, fontSize = 20.sp)
                Spacer(Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text("打开导航抽屉", color = Color.White, fontSize = 17.sp)
                }
            } else {
                Text("在导航抽屉中选择你的SD卡", color = Color.White, fontSize = 20.sp)
                Spacer(Modifier.height(14.dp))
                Text(
                    "You need to select your SD card directory that contains your music (cannot be the root directory)",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 30.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.Center) {
                repeat(2) { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (page == index) Color.White else Color.White.copy(alpha = 0.45f))
                    )
                }
            }
        }
    }
}
