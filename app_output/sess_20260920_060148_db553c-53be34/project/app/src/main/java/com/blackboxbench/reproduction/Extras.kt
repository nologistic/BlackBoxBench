package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------- 设置 ----------

@Composable
fun SettingsScreen(store: Store, nav: NavStack) {
    var picker by remember { mutableStateOf<Pair<String, List<String>>?>(null) }
    var pickerKey by remember { mutableStateOf("") }

    @Composable
    fun settingSwitch(key: String, def: Boolean): Pair<Boolean, (Boolean) -> Unit> {
        var v by remember { mutableStateOf(store.getBool(key, def)) }
        return v to { nv: Boolean -> v = nv; store.setBool(key, nv) }
    }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(
            Modifier.fillMaxWidth().background(Indigo).statusBarsPadding().height(56.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { nav.pop() }) { Icon(Icons.Filled.ArrowBack, null, tint = Color.White) }
            Text("设置", color = Color.White, fontSize = 20.sp)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            SettingsGroup("媒体库")
            SettingsItem("媒体库类别", "自定义媒体库页面显示哪些类别") { pickerKey = "cats"; picker = "媒体库类别" to listOf("歌曲", "专辑", "艺术家", "音乐类型", "播放列表") }
            SettingsSwitch("记住最后显示页面", "下次打开应用时恢复上次浏览的页面", settingSwitch("rememberTab", true))
            SettingsItem("黑名单", "管理不加入媒体库的文件夹") { }
            SettingsSwitch("白名单", "只扫描白名单中的文件夹", settingSwitch("whitelist", false))

            SettingsGroup("颜色")
            SettingsItem("全局主题", "浅色") { pickerKey = "theme"; picker = "全局主题" to listOf("浅色", "深色", "跟随系统") }
            SettingsItem("Theme style", "Classic") { }
            SettingsItem("主色调", "靛蓝") { pickerKey = "primary"; picker = "主色调" to listOf("靛蓝", "红", "蓝", "青") }
            SettingsItem("强调色", "粉") { pickerKey = "accent"; picker = "强调色" to listOf("粉", "紫", "蓝") }
            SettingsSwitch("着色导航栏", "使用主色调为导航栏着色", settingSwitch("colorNav", true))
            SettingsSwitch("着色应用快捷方式", "使用主色调为应用快捷方式着色", settingSwitch("colorShortcut", true))

            SettingsGroup("通知")
            SettingsSwitch("经典样式", "使用经典的通知样式", settingSwitch("classicNotif", false))
            SettingsSwitch("背景着色", "为通知背景着色", settingSwitch("notifBg", false))

            SettingsGroup("正在播放界面")
            SettingsItem("外观", "Card") { pickerKey = "np"; picker = "外观" to listOf("Card", "Flat", "Classic") }
            SettingsSwitch("同步歌词（LRC）", "显示 .lrc 文件中的同步歌词", settingSwitch("lrc", true))
            SettingsSwitch("控件背景透明", "正在播放界面控件背景透明", settingSwitch("npTransparent", true))
            SettingsSwitch("动画图标", "播放时显示动画图标", settingSwitch("animIcon", false))
            SettingsSwitch("显示音轨", "在歌曲列表显示音轨号", settingSwitch("showTrack", false))
            SettingsItem("点歌默认动作", "播放") { pickerKey = "tapAction"; picker = "点歌默认动作" to listOf("播放", "作为下一首播放", "加入播放队列") }

            SettingsGroup("图片")
            SettingsItem("自动下载元数据", "仅在 WiFi 下") { pickerKey = "imgDl"; picker = "自动下载元数据" to listOf("仅在 WiFi 下", "总是", "从不") }

            SettingsGroup("声音")
            SettingsSwitch("音频焦点丢失时降低音量", "通知、导航等", settingSwitch("duck", true))
            SettingsSwitch("无缝播放", "在某些设备上会造成播放问题。", settingSwitch("gapless", false))
            SettingsSwitch("记住随机播放状态", "选择新的歌曲列表时保持随机播放状态", settingSwitch("rememberShuffle", true))
            SettingsItem("均衡器", "") { }
            SettingsItem("ReplayGain源模式", "无") { pickerKey = "rg"; picker = "ReplayGain源模式" to listOf("无", "音轨", "专辑") }
            SettingsItem("ReplayGain预放大", "不启用") { }

            SettingsGroup("播放列表")
            SettingsItem("最近添加播放列表间隔", "本月") { pickerKey = "i1"; picker = "最近添加播放列表间隔" to listOf("今天", "本周", "本月", "今年") }
            SettingsItem("最近播放列表间隔", "本月") { pickerKey = "i2"; picker = "最近播放列表间隔" to listOf("今天", "本周", "本月", "今年") }
            SettingsItem("最近未播放列表间隔", "本月") { pickerKey = "i3"; picker = "最近未播放列表间隔" to listOf("今天", "本周", "本月", "今年") }
            SettingsSwitch("最喜爱的歌曲", "维护一个最多播放列表", settingSwitch("mostPlayed", true))
            SettingsSwitch("跳过的歌曲", "维护一个跳过歌曲列表", settingSwitch("skipped", false))

            SettingsGroup("Migrating")
            SettingsItem("Export", "Export the settings as file") { }
            SettingsItem("Import", "Import the settings from previously exported file") { }

            SettingsGroup("Development/Experimental features")
            SettingsSwitch("Collect crash reports", "Help development by collecting and reporting crashes", settingSwitch("crash", false))
            SettingsSwitch("Sync queue with media tag updates", "Keep the playing queue in sync when tags change", settingSwitch("syncQueue", false))
            Spacer(Modifier.height(24.dp))
        }
    }

    picker?.let { (title, options) ->
        ListMenuDialog(options.map { it to null }, onItem = { i ->
            store.setStr("pref_$pickerKey", options[i])
        }, onDismiss = { picker = null })
    }
}

@Composable
fun SettingsGroup(title: String) {
    Text(title, color = Pink, fontSize = 13.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp))
    HorizontalDivider(color = Color(0xFFEEEEEE))
}

@Composable
fun SettingsItem(title: String, sub: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(title, fontSize = 16.sp)
        if (sub.isNotEmpty()) Text(sub, fontSize = 13.sp, color = Color.Gray)
    }
}

@Composable
fun SettingsSwitch(title: String, sub: String, state: Pair<Boolean, (Boolean) -> Unit>) {
    val (v, setV) = state
    Row(
        Modifier.fillMaxWidth().clickable { setV(!v) }.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            if (sub.isNotEmpty()) Text(sub, fontSize = 13.sp, color = Color.Gray)
        }
        Switch(checked = v, onCheckedChange = setV, colors = SwitchDefaults.colors(
            checkedTrackColor = Pink.copy(alpha = 0.5f), checkedThumbColor = Pink
        ))
    }
}

// ---------- 关于 ----------

@Composable
fun AboutScreen(nav: NavStack) {
    var showChangelog by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        Row(
            Modifier.fillMaxWidth().background(Indigo).statusBarsPadding().height(56.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { nav.pop() }) { Icon(Icons.Filled.ArrowBack, null, tint = Color.White) }
            Text("关于", color = Color.White, fontSize = 20.sp)
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(12.dp)) {
            Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp), color = Color.White) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        VinylDisc(0f, Modifier.size(48.dp))
                        Spacer(Modifier.width(16.dp))
                        Text("Vinyl Music Player", fontSize = 20.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(16.dp))
                    AboutItem("版本", "1.11.0") { }
                    AboutItem("更新日志", "") { showChangelog = true }
                    AboutItem("介绍页", "") { }
                    AboutItem("创建 GitHub 分支", "") { }
                    AboutItem("许可信息", "") { }
                }
            }
            Spacer(Modifier.height(12.dp))
            Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp), color = Color.White) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("支持开发者", fontSize = 13.sp, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    AboutItem("提交 bug", "提交 bug 或申请添加新功能") { }
                    AboutItem("评分", "如果喜欢 Vinyl Music Player 请在 Play Store 中留下您的好评。") { }
                }
            }
            Spacer(Modifier.height(12.dp))
            Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp), color = Color.White) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Maintainers", fontSize = 13.sp, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    listOf("AdrienPoupa", "Octoton", "soncaokim").forEach { m ->
                        Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.foundation.Canvas(Modifier.size(40.dp)) { drawCircle(Color(0xFF90A4AE)) }
                            Spacer(Modifier.width(16.dp))
                            Text(m, fontSize = 15.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Contributors", fontSize = 13.sp, color = Color.Gray)
                }
            }
        }
    }
    if (showChangelog) {
        AlertDialog(
            onDismissRequest = { showChangelog = false },
            confirmButton = { TextButton(onClick = { showChangelog = false }) { Text("确定", color = Pink) } },
            title = { Text("Changelog") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("[1.11.0] - 2024-08-18", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Features", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 13.sp)
                    Text("• Export import settings by @TheFireCircle in PR #946\n• Pref export/import by @soncaokim in PR #1051\n• Add setting to hide album details on album view by @eliehess in PR #911\n• Translating strings to german by @TheFireCircle in PR #1103\n• Update russian translation by @developersu in PR #1112", fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Fixes", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 13.sp)
                    Text("• Fix crasher - when the device just started and the storage/mediastore is not ready by @soncaokim in PR #1087\n• Allow sleep timer to reach 0, resolving issue #1054 by @mfolsom1 in PR #1102", fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Other Changes", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 13.sp)
                    Text("• Update tools by @soncaokim in PR #1085\n• Remove the obsolete material-cab library by @soncaokim in PR #1058", fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("New Contributors", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        )
    }
}

@Composable
fun AboutItem(title: String, sub: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp)) {
        Text(title, fontSize = 16.sp)
        if (sub.isNotEmpty()) Text(sub, fontSize = 13.sp, color = Color.Gray)
    }
}

// ---------- 标签编辑器 ----------

@Composable
fun TagEditorScreen(store: Store, nav: NavStack, songId: Int) {
    val song = store.songs().firstOrNull { it.id == songId }
    if (song == null) { nav.pop(); return }
    var title by remember { mutableStateOf(song.title) }
    var artist by remember { mutableStateOf(song.artist) }
    var album by remember { mutableStateOf(song.album) }
    var albumArtist by remember { mutableStateOf(song.artist) }
    var genre by remember { mutableStateOf(song.genre) }
    var year by remember { mutableStateOf(if (song.year > 0) song.year.toString() else "") }
    var track by remember { mutableStateOf(if (song.track > 0) song.track.toString() else "") }
    var lyrics by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(Color.White)) {
            Row(
                Modifier.fillMaxWidth().background(Indigo).statusBarsPadding().height(56.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { nav.pop() }) { Icon(Icons.Filled.ArrowBack, null, tint = Color.White) }
                Text("音乐标签编辑器", color = Color.White, fontSize = 20.sp)
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CoverArt(song, 140.dp)
                }
                Spacer(Modifier.height(16.dp))
                TagField("标题", title) { title = it }
                TagField("艺术家", artist) { artist = it }
                TagField("专辑", album) { album = it }
                TagField("专辑艺术家", albumArtist) { albumArtist = it }
                TagField("流派", genre) { genre = it }
                TagField("年份", year) { year = it }
                TagField("音轨号", track) { track = it }
                TagField("歌词", lyrics) { lyrics = it }
                Spacer(Modifier.height(80.dp))
            }
        }
        FloatingActionButton(
            onClick = { store.saveTags(songId, title, artist, album, genre); nav.pop() },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = Pink, shape = CircleShape
        ) { Icon(Icons.Filled.Check, null, tint = Color.White) }
    }
}

@Composable
fun TagField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text(label) }, singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}
