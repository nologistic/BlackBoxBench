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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(state: AppState) {
    val accent = Color(state.accentColor)
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBarTitle(
            title = "设置",
            primary = Color(state.primaryColor),
            onBack = { state.screen = state.lastLibraryScreen }
        )
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            SectionHeader("媒体库")
            SettingRow(
                "媒体库类别",
                "配置媒体库类别可见性和顺序。"
            ) { state.dialog = DialogKind.CATEGORIES }
            SettingRow(
                "记住最后显示页面",
                "启动时回到上次打开的视图",
                trailing = {
                    SwitchPill(state.rememberLastPage, accent) { state.rememberLastPage = it; state.persist() }
                }
            )
            SettingRow("黑名单", "黑名单中文件夹将在媒体库中隐藏。") { state.toast("黑名单") }
            SettingRow(
                "白名单",
                "只显示起始目录：/storage/emulated/0/Music",
                trailing = {
                    SwitchPill(state.whiteListEnabled, accent) { state.whiteListEnabled = it; state.persist() }
                }
            )

            SectionHeader("颜色")
            SettingRow("全局主题", themeLabel(state.themeMode)) { state.dialog = DialogKind.THEME }
            SettingRow("Theme style", "Classic") { state.toast("Theme style") }
            SettingRow(
                "主色调",
                "主题主色调，默认为靛蓝色。",
                trailing = { ColorDot(state.primaryColor) }
            ) { state.dialog = DialogKind.PRIMARY_COLOR }
            SettingRow(
                "强调色",
                "主题强调色，默认为粉色。",
                trailing = { ColorDot(state.accentColor) }
            ) { state.dialog = DialogKind.ACCENT_COLOR }
            SettingRow(
                "着色导航栏", "用主色调着色导航栏。",
                trailing = { SwitchPill(state.tintNavBar, accent) { state.tintNavBar = it; state.persist() } }
            )
            SettingRow(
                "着色应用快捷方式", "用主色调着色应用快捷方式。",
                trailing = { SwitchPill(state.tintShortcuts, accent) { state.tintShortcuts = it; state.persist() } }
            )
            SettingRow(
                "控件背景透明", "使控件的背景透明显示",
                trailing = { SwitchPill(state.transparentWidgets, accent) { state.transparentWidgets = it; state.persist() } }
            )

            SectionHeader("通知")
            SettingRow(
                "经典通知样式", "使用经典通知样式。",
                trailing = { SwitchPill(state.classicNotification, accent) { state.classicNotification = it; state.persist() } }
            )
            SettingRow(
                "启用通知背景着色", "使用与专辑封面相匹配的颜色着色通知背景色。",
                trailing = { SwitchPill(state.coloredNotification, accent) { state.coloredNotification = it; state.persist() } }
            )

            SectionHeader("正在播放界面")
            SettingRow("外观", listOf("Card", "Full", "Plain").getOrElse(state.nowPlayingAppearance) { "Card" }) {
                state.nowPlayingAppearance = (state.nowPlayingAppearance + 1) % 3
                state.persist()
            }
            SettingRow(
                "显示同步歌词", "当前仅支持 LRC 格式同步歌词。内嵌或外置均可。",
                trailing = { SwitchPill(state.syncLyrics, accent) { state.syncLyrics = it; state.persist() } }
            )
            SettingRow(
                "动画显示正在播放歌曲图标", "在歌曲相关视图中动画显示当前歌曲图标",
                trailing = { SwitchPill(state.animateIcon, accent) { state.animateIcon = it; state.persist() } }
            )
            SettingRow(
                "显示歌曲音轨", "在播放队列和提醒中显示歌曲音轨",
                trailing = { SwitchPill(state.showTrackNumber, accent) { state.showTrackNumber = it; state.persist() } }
            )
            SettingRow("Default action on choosing/tapping a song while browsing", "播放") {
                state.defaultTapAction = (state.defaultTapAction + 1) % 2
                state.persist()
            }

            SectionHeader("图片")
            SettingRow(
                "自动下载元数据",
                listOf("从不", "仅在 WiFi 下", "总是").getOrElse(state.autoDownloadMeta) { "仅在 WiFi 下" }
            ) {
                state.autoDownloadMeta = (state.autoDownloadMeta + 1) % 3
                state.persist()
            }

            SectionHeader("声音")
            SettingRow(
                "音频焦点丢失时降低音量", "通知、导航等",
                trailing = { SwitchPill(state.pauseOnFocusLoss, accent) { state.pauseOnFocusLoss = it; state.persist() } }
            )
            SettingRow(
                "无缝播放", "在某些设备上会造成播放问题。",
                trailing = { SwitchPill(state.gapless, accent) { state.gapless = it; state.persist() } }
            )
            SettingRow(
                "记住随机播放状态", "选择新的歌曲列表时保持随机播放状态",
                trailing = { SwitchPill(state.rememberShuffle, accent) { state.rememberShuffle = it; state.persist() } }
            )
            SettingRow("均衡器") { state.dialog = DialogKind.EQUALIZER }
            SettingRow("ReplayGain源模式", listOf("无", "音轨", "专辑").getOrElse(state.replayGainMode) { "无" }) {
                state.replayGainMode = (state.replayGainMode + 1) % 3
                state.persist()
            }
            SettingRow("ReplayGain预放大", if (state.replayGainPreamp == 0) "不启用" else "启用") {
                state.replayGainPreamp = (state.replayGainPreamp + 1) % 2
                state.persist()
            }

            SectionHeader("播放列表")
            SettingRow("最近添加播放列表间隔", intervalLabel(state.recentAddedInterval)) {
                state.recentAddedInterval = (state.recentAddedInterval + 1) % 4; state.persist()
            }
            SettingRow("最近播放列表间隔", intervalLabel(state.recentPlayedInterval)) {
                state.recentPlayedInterval = (state.recentPlayedInterval + 1) % 4; state.persist()
            }
            SettingRow("最近未播放列表间隔", intervalLabel(state.notPlayedInterval)) {
                state.notPlayedInterval = (state.notPlayedInterval + 1) % 4; state.persist()
            }

            SectionHeader("Migrating")
            SettingRow("从旧版本恢复", "从 Vinyl 旧版本导入播放列表与收藏") { state.toast("没有可恢复的数据") }

            SectionHeader("Development")
            SettingRow("Experimental features", "尚未稳定的实验特性") { state.toast("暂无实验特性") }
            Spacer(Modifier.height(32.dp))
        }
    }
}

private fun themeLabel(mode: Int) = listOf("浅色", "暗色", "黑色（AMOLED）").getOrElse(mode) { "浅色" }

private fun intervalLabel(value: Int) =
    listOf("本月", "上月", "过去 6 个月", "全部").getOrElse(value) { "本月" }

@Composable
private fun ColorDot(argb: Int) {
    Box(
        Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(Color(argb))
    )
}

// -------------------------------------------------------------------- about

@Composable
fun AboutScreen(state: AppState) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBarTitle(
            title = "关于",
            primary = Color(state.primaryColor),
            onBack = { state.screen = state.lastLibraryScreen }
        )
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            SectionHeader("应用信息")
            SettingRow("Vinyl Music Player", "版本 1.11.0")
            SettingRow("更新日志", "查看此版本的更新内容") { state.toast("更新日志") }
            SettingRow("介绍页", "重新查看功能介绍") { state.screen = Screen.INTRO }
            SettingRow("创建 GitHub 分支", "为本项目创建分支") { state.toast("已复制仓库地址") }
            SettingRow("许可信息", "开源许可") { state.toast("许可信息") }

            SectionHeader("支持开发者")
            SettingRow("提交 bug", "在问题跟踪器中反馈") { state.toast("提交 bug") }
            SettingRow("评分", "在应用商店中评分") { state.toast("感谢评分") }

            SectionHeader("维护者")
            listOf("Rin Takeda", "Marta Oliveira", "Kenji Sato").forEach { name ->
                SettingRow(name, "维护者")
            }
            SectionHeader("贡献者")
            listOf("Lucia Ferreira", "Dmitri Volkov", "Amara Nwosu", "Tomás Reyes").forEach { name ->
                SettingRow(name, "贡献者")
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// -------------------------------------------------------------------- intro

@Composable
fun IntroScreen(state: AppState) {
    var page by remember { mutableStateOf(0) }
    val accent = Color(state.accentColor)
    val titles = listOf(
        "欢迎使用 Vinyl Music Player",
        "播放队列",
        "拖动调整播放顺序"
    )
    val bodies = listOf(
        "一款简洁的本地音乐播放器：浏览歌曲、专辑、艺术家与播放列表，完全离线。",
        "上滑底部的迷你播放器即可展开正在播放页与播放队列。",
        "在播放队列中按住序号把手即可拖动调整曲目顺序。"
    )
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(60.dp))
        Box(
            Modifier
                .size(220.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1E1E)),
            contentAlignment = Alignment.Center
        ) { VinylGlyph(Color(0xFF9E9E9E), 170.dp) }
        Spacer(Modifier.height(36.dp))
        Text(
            titles[page],
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(16.dp))
        Text(
            bodies[page],
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.Center) {
            repeat(3) { index ->
                Box(
                    Modifier
                        .padding(6.dp)
                        .size(if (index == page) 12.dp else 9.dp)
                        .clip(CircleShape)
                        .background(if (index == page) accent else MaterialTheme.colorScheme.outline)
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "跳过",
                color = accent,
                fontSize = 16.sp,
                modifier = Modifier.clickable { state.screen = Screen.LIBRARY_SONGS }
            )
            Text(
                if (page < 2) "下一页" else "GET STARTED",
                color = accent,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable {
                    if (page < 2) page++ else state.screen = Screen.LIBRARY_SONGS
                }
            )
        }
        Spacer(Modifier.height(40.dp))
    }
}

// --------------------------------------------------------------- tag editor

@Composable
fun TagEditorScreen(state: AppState) {
    val track = Library.byId(state.tagEditorTrackId ?: "") ?: state.currentTrack ?: return
    var song by remember { mutableStateOf(track.title) }
    var album by remember { mutableStateOf(track.album) }
    var artist by remember { mutableStateOf(track.artist) }
    var genre by remember { mutableStateOf(track.genre) }
    var year by remember { mutableStateOf(track.year) }
    var trackNo by remember { mutableStateOf(track.trackNo.toString()) }
    var discNo by remember { mutableStateOf(track.discNo.toString()) }
    var lyrics by remember { mutableStateOf("") }
    var granted by remember { mutableStateOf(false) }
    var pendingSave by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBarTitle(
            title = "音乐标签编辑器",
            primary = Color(state.primaryColor),
            onBack = { state.screen = state.lastLibraryScreen }
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Field("歌曲", song) { song = it }
            Field("专辑", album) { album = it }
            Field("作者（每行一个）", artist) { artist = it }
            Field("Genres (one per line)", genre) { genre = it }
            Field("年份", year) { year = it }
            Field("音轨", trackNo) { trackNo = it }
            Field("光碟号", discNo) { discNo = it }
            Field("歌词", lyrics) { lyrics = it }
            Spacer(Modifier.height(80.dp))
        }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
        Box(
            Modifier
                .padding(24.dp)
                .size(60.dp)
                .clip(CircleShape)
                .background(Color(state.accentColor))
                .clickable {
                    if (!granted) {
                        pendingSave = true
                    } else {
                        state.toast("标签已保存")
                        state.screen = state.lastLibraryScreen
                    }
                },
            contentAlignment = Alignment.Center
        ) { SaveGlyph(Color.White, 28.dp) }
    }
    if (pendingSave) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingSave = false },
            title = { Text("要允许Vinyl Music Player修改这个音频文件吗？") },
            text = { Text("修改后标签将写入该音频文件。") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    granted = true
                    pendingSave = false
                    state.toast("标签已保存")
                    state.screen = state.lastLibraryScreen
                }) { Text("允许") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { pendingSave = false }) { Text("拒绝") }
            }
        )
    }
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 14.dp)) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            singleLine = label != "歌词",
            modifier = Modifier.fillMaxWidth()
        )
    }
}
