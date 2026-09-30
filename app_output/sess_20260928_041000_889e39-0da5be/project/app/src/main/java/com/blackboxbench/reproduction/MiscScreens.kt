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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DetailTopBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable () -> Unit = {}
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
        }
        Text(
            title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        actions()
    }
}

@Composable
fun SettingsHeader(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.primary,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 20.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsSwitchRow(title: String, subtitle: String? = null, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, color = Color(0xFF212121))
            if (subtitle != null) Text(subtitle, fontSize = 13.sp, color = VinylColors.SecondaryText)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.secondary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFBDBDBD)
            )
        )
    }
}

@Composable
fun SettingsValueRow(title: String, subtitle: String? = null, value: String? = null, valueColor: Color = VinylColors.SecondaryText, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, color = Color(0xFF212121))
            if (subtitle != null) Text(subtitle, fontSize = 13.sp, color = VinylColors.SecondaryText)
        }
        if (value != null) Text(value, fontSize = 14.sp, color = valueColor)
    }
}

@Composable
fun SettingsColorRow(title: String, subtitle: String?, color: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, color = Color(0xFF212121))
            if (subtitle != null) Text(subtitle, fontSize = 13.sp, color = VinylColors.SecondaryText)
        }
        Box(
            Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

private val monthOptions = listOf("本周", "本月", "今年", "全部")
private val themeOptions = listOf("浅色", "深色", "跟随系统")
private val themeStyleOptions = listOf("Classic", "Modern")
private val appearanceOptions = listOf("Card", "Full", "Plain")
private val actionOptions = listOf("Play", "Play next", "Add to queue")
private val downloadOptions = listOf("从不", "仅在 WiFi 下", "始终")
private val rgSourceOptions = listOf("无", "音轨", "专辑")
private val rgPreampOptions = listOf("不启用", "启用")

@Composable
fun SettingsScreen(model: AppModel, onOpenEqualizer: () -> Unit) {
    val s = model.settings
    var dialog by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SettingsHeader("媒体库")
        SettingsValueRow("媒体库类别", "配置媒体库类别可见性和顺序。", onClick = { dialog = "categories" })
        SettingsSwitchRow("记住最后显示页面", "启动时回到上次打开的视图", s.bool("remember_last_page")) { s.setBool("remember_last_page", it) }
        SettingsValueRow("黑名单", "黑名单中文件夹将在媒体库中隐藏。", onClick = { dialog = "blacklist" })
        SettingsSwitchRow("白名单", "只显示起始目录：/storage/emulated/0/Music", s.bool("whitelist")) { s.setBool("whitelist", it) }

        SettingsHeader("颜色")
        SettingsValueRow("全局主题", "主题主色调，默认为靛蓝色。", themeOptions[s.int("global_theme")], onClick = { dialog = "theme" })
        SettingsValueRow("Theme style", "Classic", themeStyleOptions[s.int("theme_style")], onClick = { dialog = "style" })
        SettingsColorRow("主色调", "主题主色调，默认为靛蓝色。", s.primary, onClick = { dialog = "primary" })
        SettingsColorRow("强调色", "主题强调色，默认为粉色。", s.accent, onClick = { dialog = "accent" })
        SettingsSwitchRow("着色导航栏", "用主色调着色导航栏。", s.bool("colored_nav_bar")) { s.setBool("colored_nav_bar", it) }
        SettingsSwitchRow("着色应用快捷方式", "用主色调着色应用快捷方式。", s.bool("colored_shortcuts")) { s.setBool("colored_shortcuts", it) }
        SettingsSwitchRow("控件背景透明", "使控件的背景透明显示", s.bool("transparent_widget_bg")) { s.setBool("transparent_widget_bg", it) }

        SettingsHeader("通知")
        SettingsSwitchRow("经典通知样式", "使用经典通知样式", s.bool("classic_notification")) { s.setBool("classic_notification", it) }
        SettingsSwitchRow("启用通知背景着色", "显示通知的背景颜色", s.bool("colored_notification")) { s.setBool("colored_notification", it) }

        SettingsHeader("正在播放界面")
        SettingsValueRow("外观", "选择正在播放界面的布局。", appearanceOptions[s.int("now_playing_appearance")], onClick = { dialog = "appearance" })
        SettingsSwitchRow("显示同步歌词", "当前仅支持 LRC 格式同步歌词。内嵌或外置均可。", s.bool("show_lyrics")) { s.setBool("show_lyrics", it) }
        SettingsSwitchRow("动画显示正在播放歌曲图标", "正在播放的歌曲图标会随音乐律动。", s.bool("animate_now_playing_icon")) { s.setBool("animate_now_playing_icon", it) }
        SettingsSwitchRow("显示歌曲音轨", "在播放队列和提醒中显示歌曲音轨", s.bool("show_track_number")) { s.setBool("show_track_number", it) }
        SettingsValueRow("Default action on choosing/tapping a song while browsing", null, actionOptions[s.int("default_action")], onClick = { dialog = "action" })

        SettingsHeader("图片")
        SettingsValueRow("自动下载元数据", "自动下载缺失的专辑封面和艺人图片。", downloadOptions[s.int("auto_download")], onClick = { dialog = "download" })

        SettingsHeader("声音")
        SettingsSwitchRow("音频焦点丢失时降低音量", "当其他应用播放声音时降低音量。", s.bool("lower_volume_focus")) { s.setBool("lower_volume_focus", it) }
        SettingsSwitchRow("无缝播放", "在某些设备上会造成播放问题。", s.bool("gapless")) { s.setBool("gapless", it) }
        SettingsSwitchRow("记住随机播放状态", "重新启动后恢复随机播放。", s.bool("remember_shuffle")) { s.setBool("remember_shuffle", it) }
        SettingsValueRow("均衡器", null, "", onClick = onOpenEqualizer)
        SettingsValueRow("ReplayGain源模式", "选择 ReplayGain 的来源。", rgSourceOptions[s.int("rg_source")], onClick = { dialog = "rg_source" })
        SettingsValueRow("ReplayGain预放大", "调整 ReplayGain 的预放大。", rgPreampOptions[s.int("rg_preamp")], onClick = { dialog = "rg_preamp" })

        SettingsHeader("播放列表")
        SettingsValueRow("最近添加播放列表间隔", null, monthOptions[s.int("recently_added_interval")], onClick = { dialog = "m1" })
        SettingsValueRow("最近播放列表间隔", null, monthOptions[s.int("recent_interval")], onClick = { dialog = "m2" })
        SettingsValueRow("最近未播放列表间隔", null, monthOptions[s.int("recently_not_played_interval")], onClick = { dialog = "m3" })
        SettingsSwitchRow("最喜爱的歌曲", "维护一个最常播放的播放列表", s.bool("favorite_songs")) { s.setBool("favorite_songs", it) }
        SettingsSwitchRow("跳过的歌曲", "维护一个跳过歌曲列表", s.bool("skipped_songs")) { s.setBool("skipped_songs", it) }

        SettingsHeader("Migrating")
        SettingsValueRow("Export", "Export the settings as file", onClick = {})
        SettingsValueRow("Import", "Import the settings from previously exported file", onClick = {})

        SettingsHeader("Development / Experimental features")
        SettingsSwitchRow("Collect crash reports", "Help development by collecting and reporting crashes", s.bool("collect_crash")) { s.setBool("collect_crash", it) }
        SettingsSwitchRow("Sync queue with media tag updates", "Keep the queue in sync when tags change", s.bool("sync_queue_tags")) { s.setBool("sync_queue_tags", it) }
        Spacer(Modifier.height(32.dp))
    }

    when (dialog) {
        "categories" -> LibraryCategoriesDialog(model) { dialog = null }
        "theme" -> ChooserDialog("全局主题", themeOptions, s.int("global_theme"), { s.setInt("global_theme", it); dialog = null }) { dialog = null }
        "style" -> ChooserDialog("Theme style", themeStyleOptions, s.int("theme_style"), { s.setInt("theme_style", it); dialog = null }) { dialog = null }
        "appearance" -> ChooserDialog("外观", appearanceOptions, s.int("now_playing_appearance"), { s.setInt("now_playing_appearance", it); dialog = null }) { dialog = null }
        "action" -> ChooserDialog("Default action", actionOptions, s.int("default_action"), { s.setInt("default_action", it); dialog = null }) { dialog = null }
        "download" -> ChooserDialog("自动下载元数据", downloadOptions, s.int("auto_download"), { s.setInt("auto_download", it); dialog = null }) { dialog = null }
        "rg_source" -> ChooserDialog("ReplayGain源模式", rgSourceOptions, s.int("rg_source"), { s.setInt("rg_source", it); dialog = null }) { dialog = null }
        "rg_preamp" -> ChooserDialog("ReplayGain预放大", rgPreampOptions, s.int("rg_preamp"), { s.setInt("rg_preamp", it); dialog = null }) { dialog = null }
        "m1" -> ChooserDialog("最近添加播放列表间隔", monthOptions, s.int("recently_added_interval"), { s.setInt("recently_added_interval", it); dialog = null }) { dialog = null }
        "m2" -> ChooserDialog("最近播放列表间隔", monthOptions, s.int("recent_interval"), { s.setInt("recent_interval", it); dialog = null }) { dialog = null }
        "m3" -> ChooserDialog("最近未播放列表间隔", monthOptions, s.int("recently_not_played_interval"), { s.setInt("recently_not_played_interval", it); dialog = null }) { dialog = null }
        "primary" -> ColorChooserDialog("主色调", PrimaryChoices, s.int("primary_color"), { s.setInt("primary_color", it); dialog = null }) { dialog = null }
        "accent" -> ColorChooserDialog("强调色", AccentChoices, s.int("accent_color"), { s.setInt("accent_color", it); dialog = null }) { dialog = null }
        "blacklist" -> ConfirmDialog("黑名单", "黑名单中文件夹将在媒体库中隐藏。", "确定", onConfirm = { dialog = null }, onDismiss = { dialog = null })
    }
}

@Composable
private fun ColorChooserDialog(title: String, colors: List<Color>, selected: Int, onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontSize = 18.sp) },
        text = {
            Column {
                colors.chunked(5).forEach { row ->
                    Row(Modifier.padding(vertical = 6.dp)) {
                        row.forEach { c ->
                            Box(
                                Modifier
                                    .padding(6.dp)
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .clickable { onSelect(colors.indexOf(c)) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = VinylColors.SecondaryText) } },
        containerColor = Color.White
    )
}

@Composable
fun LibraryCategoriesDialog(model: AppModel, onDismiss: () -> Unit) {
    val s = model.settings
    val working = remember { s.enabledCategories.toMutableList() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("媒体库类别", fontSize = 18.sp) },
        text = {
            Column {
                LibraryTab.entries.forEachIndexed { i, tab ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { working[i] = !working[i] }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = working.getOrElse(i) { true },
                            onCheckedChange = { working[i] = it },
                            colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.secondary)
                        )
                        Text(tab.label, modifier = Modifier.weight(1f))
                        Text("⋮⋮", color = VinylColors.SecondaryText)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { s.setCategories(working); onDismiss() }) { Text("确定", color = MaterialTheme.colorScheme.secondary) } },
        dismissButton = {
            Row {
                TextButton(onClick = { for (i in working.indices) working[i] = true }) { Text("重置", color = MaterialTheme.colorScheme.secondary) }
                TextButton(onClick = onDismiss) { Text("取消", color = VinylColors.SecondaryText) }
            }
        },
        containerColor = Color.White
    )
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        DetailTopBar("关于", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painterResource(R.drawable.ic_music_note), contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("Vinyl Music Player", fontSize = 22.sp, fontWeight = FontWeight.Medium)
                Text("1.11.0", fontSize = 14.sp, color = VinylColors.SecondaryText)
            }
            AboutRow("更新日志")
            AboutRow("介绍页")
            AboutRow("创建 GitHub 分支")
            AboutRow("许可信息")
            SettingsHeader("支持开发者")
            AboutRow("提交 bug")
            AboutRow("评分")
            SettingsHeader("Maintainers")
            AboutRow("AdrienPoupa")
            AboutRow("Octoton")
            AboutRow("soncaokim")
            SettingsHeader("Contributors")
            AboutRow("Vinyl Music Player 社区")
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AboutRow(title: String) {
    Text(
        title,
        fontSize = 16.sp,
        color = Color(0xFF212121),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {}
            .padding(horizontal = 16.dp, vertical = 14.dp)
    )
}

@Composable
fun FoldersScreen(model: AppModel, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        DetailTopBar("Vinyl Music Player", onBack)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val parts = model.lastFolderPath.trim('/').split("/")
            parts.forEachIndexed { i, p ->
                Text(
                    if (i == 0) "…" + p.takeLast(2).uppercase() else p.uppercase(),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                if (i < parts.size - 1) Text("  >  ", color = VinylColors.SecondaryText, fontSize = 12.sp)
            }
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                painterResource(R.drawable.ic_folder),
                contentDescription = null,
                tint = Color(0xFFBDBDBD),
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
fun SearchScreen(
    model: AppModel,
    onBack: () -> Unit,
    onSongClick: (Song, List<Long>) -> Unit,
    onSongMore: (Song) -> Unit,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
            }
            androidx.compose.material3.TextField(
                value = model.searchQuery,
                onValueChange = { model.searchQuery = it },
                placeholder = { Text(if (model.searchQuery.isEmpty()) "正在扫描" else "搜索歌曲、专辑或艺人") },
                singleLine = true,
                colors = androidx.compose.material3.TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.weight(1f)
            )
            if (model.searchQuery.isNotEmpty()) {
                IconButton(onClick = { model.searchQuery = "" }) {
                    Icon(Icons.Filled.Clear, contentDescription = "清除", tint = Color.White)
                }
            }
        }
        val q = model.searchQuery.trim()
        val songs = if (q.isEmpty()) emptyList() else model.repo.songs.filter {
            it.title.contains(q, true) || it.artist.contains(q, true) || it.album.contains(q, true)
        }
        val albums = if (q.isEmpty()) emptyList() else model.repo.albums.filter { it.title.contains(q, true) }
        val artists = if (q.isEmpty()) emptyList() else model.repo.artists.filter { it.name.contains(q, true) }
        if (songs.isEmpty() && albums.isEmpty() && artists.isEmpty()) {
            EmptyState("没有找到结果")
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                if (songs.isNotEmpty()) {
                    item { SectionHeader("歌曲") }
                    items(songs, key = { it.id }) { song ->
                        SongRow(song, onClick = { onSongClick(song, songs.map { s -> s.id }) }, onMore = { onSongMore(song) })
                    }
                }
                if (albums.isNotEmpty()) {
                    item { SectionHeader("专辑") }
                    items(albums, key = { it.title }) { album ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onAlbumClick(album.title) }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            VinylCover(album.title, Modifier.size(48.dp), cornerRadius = 2.dp)
                            Spacer(Modifier.width(16.dp))
                            Text(album.title, fontSize = 16.sp)
                        }
                    }
                }
                if (artists.isNotEmpty()) {
                    item { SectionHeader("艺术家") }
                    items(artists, key = { it.name }) { artist ->
                        Text(
                            artist.name,
                            fontSize = 16.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onArtistClick(artist.name) }
                                .padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TagEditorScreen(song: Song, onBack: () -> Unit, onSave: (Song) -> Unit) {
    var title by remember { mutableStateOf(song.title) }
    var album by remember { mutableStateOf(song.album) }
    var artist by remember { mutableStateOf(song.artist) }
    var genre by remember { mutableStateOf(song.genre) }
    var year by remember { mutableStateOf(song.year.toString()) }
    var track by remember { mutableStateOf(song.trackNo.toString()) }
    var disc by remember { mutableStateOf("0") }
    var lyrics by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        DetailTopBar("音乐标签编辑器", onBack)
        Box(Modifier.weight(1f)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                TagField("歌曲", title) { title = it }
                TagField("专辑", album) { album = it }
                TagField("作者（每行一个）", artist) { artist = it }
                TagField("流派（每行一个）", genre) { genre = it }
                TagField("年份", year) { year = it }
                TagField("音轨", track) { track = it }
                TagField("光碟号", disc) { disc = it }
                TagField("歌词", lyrics, minLines = 4) { lyrics = it }
                Spacer(Modifier.height(88.dp))
            }
            FloatingActionButton(
                onClick = {
                    onSave(
                        song.copy(
                            title = title.ifBlank { song.title },
                            album = album.ifBlank { song.album },
                            artist = artist.ifBlank { song.artist },
                            genre = genre.ifBlank { song.genre },
                            year = year.toIntOrNull() ?: song.year,
                            trackNo = track.toIntOrNull() ?: song.trackNo
                        )
                    )
                },
                containerColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Filled.Check, contentDescription = "保存", tint = Color.White)
            }
        }
    }
}

@Composable
private fun TagField(label: String, value: String, minLines: Int = 1, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        minLines = minLines,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    )
}

@Composable
fun EqualizerScreen(onBack: () -> Unit) {
    var enabled by remember { mutableStateOf(false) }
    val bands = listOf("60 Hz", "230 Hz", "910 Hz", "4 kHz", "14 kHz")
    val levels = remember { mutableStateListOf(0f, 0f, 0f, 0f, 0f) }
    var bass by remember { mutableStateOf(0f) }
    var surround by remember { mutableStateOf(0f) }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
            }
            Text("均衡器", color = Color.White, fontSize = 20.sp, modifier = Modifier.weight(1f))
            Switch(
                checked = enabled,
                onCheckedChange = { enabled = it },
                modifier = Modifier.padding(end = 12.dp)
            )
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("FX 增强器", color = Color(0xFF212121), fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            Text("FX 增强器", color = VinylColors.SecondaryText, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
            Text("+10 dB", color = VinylColors.SecondaryText, fontSize = 12.sp)
            bands.forEachIndexed { i, band ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(band, color = Color(0xFF212121), fontSize = 14.sp, modifier = Modifier.width(64.dp))
                    Slider(
                        value = levels[i],
                        onValueChange = { levels[i] = it },
                        valueRange = -10f..10f,
                        enabled = enabled,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Text("-10 dB", color = VinylColors.SecondaryText, fontSize = 12.sp)
            Spacer(Modifier.height(16.dp))
            Text("低音增强", color = if (enabled) Color(0xFF212121) else Color(0xFF9E9E9E), fontSize = 15.sp)
            Slider(value = bass, onValueChange = { bass = it }, enabled = false)
            Text("环绕声", color = if (enabled) Color(0xFF212121) else Color(0xFF9E9E9E), fontSize = 15.sp)
            Slider(value = surround, onValueChange = { surround = it }, enabled = false)
        }
    }
}
