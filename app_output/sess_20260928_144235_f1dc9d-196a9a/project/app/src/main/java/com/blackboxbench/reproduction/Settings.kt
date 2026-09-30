package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
fun SettingsScaffold(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    val p = LocalVlcPalette.current
    Column(Modifier.fillMaxSize().background(p.background)) {
        TopBar(title = title, showLogo = false, onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) { content() }
    }
}

@Composable
fun ChoiceDialog(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    VlcDialog(onDismiss) {
        DialogTitle(title)
        Column(Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(i) }
                        .padding(horizontal = 22.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(option, color = LocalVlcPalette.current.textPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    if (i == selectedIndex) {
                        VlcIconView(VlcIcon.CHECK, 20.dp, LocalVlcPalette.current.accent)
                    }
                }
            }
        }
        Text(
            "取消",
            color = LocalVlcPalette.current.accent,
            fontSize = 15.sp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
                .padding(horizontal = 22.dp, vertical = 12.dp),
        )
    }
}

// ------------------------------------------------------------ Settings root

@Composable
fun SettingsScreen(onBack: () -> Unit, onOpen: (String) -> Unit, onOpenFolders: () -> Unit) {
    SettingsScaffold("设置", onBack) {
        SectionHeader("媒体库")
        ArrowRow("媒体库文件夹") { onOpenFolders() }
        SwitchRow("自动重新扫描", checked = Store.autoRescan.value) { Store.autoRescan.value = it }
        SectionHeader("视频")
        ArrowRow("背景/画中画模式", value = Store.backgroundMode.value) { onOpen("video") }
        ValueRow("硬件加速", Store.hardwareAccel.value) { onOpen("video") }
        ValueRow("视频屏幕方向", Store.screenOrientation.value) { onOpen("video") }
        SectionHeader("网络")
        ValueRow("按流量计费网络下对串流的处理", Store.meteredNetwork.value) { }
        SectionHeader("权限")
        ArrowRow("权限") { onOpen("permissions") }
        SectionHeader("历史")
        SwitchRow("播放历史", checked = Store.playbackHistory.value) { Store.playbackHistory.value = it }
        SwitchRow("视频播放队列历史", checked = Store.videoQueueHistory.value) { Store.videoQueueHistory.value = it }
        SwitchRow("音频播放队列历史", checked = Store.audioQueueHistory.value) { Store.audioQueueHistory.value = it }
        SectionHeader("附加设置")
        listOf(
            "界面" to "interface",
            "视频" to "video",
            "字幕" to "subtitles",
            "音频" to "audio",
            "均衡器" to "equalizer",
            "投屏" to "casting",
            "家长控制" to "parental",
            "远程访问" to "remote",
            "Android Auto" to "auto",
            "高级" to "advanced",
        ).forEach { (label, key) -> ArrowRow(label) { onOpen(key) } }
        Spacer(Modifier.height(30.dp))
    }
}

// ------------------------------------------------------------ sub pages

@Composable
fun SettingsPage(page: String, onBack: () -> Unit, onOpenFolders: () -> Unit) {
    var dialog by remember { mutableStateOf<String?>(null) }
    var restartPrompt by remember { mutableStateOf(false) }

    when (page) {
        "interface" -> SettingsScaffold("界面", onBack) {
            ArrowRow("自动切换夜间模式", value = when (Store.themeMode.value) {
                1 -> "亮色主题"; 2 -> "黑色主题"; else -> "跟随系统模式"
            }) { dialog = "theme" }
            SwitchRow("Android TV 界面", checked = false) { }
            ArrowRow("设置语言", value = "跟随系统") { dialog = "language" }
            SwitchRow("单行列表标题缩略", checked = Store.singleLineTitle.value) { Store.singleLineTitle.value = it }
            SwitchRow("显示标题", checked = Store.showHeaders.value) { Store.showHeaders.value = it }
            SwitchRow("显示缺失的媒体", checked = Store.showMissingMedia.value) { Store.showMissingMedia.value = it }
            ArrowRow("睡眠计时器", value = Store.sleepTimer.value) { dialog = "sleep" }
            SwitchRow("无痕模式", checked = Store.incognito.value) { Store.incognito.value = it }
            SwitchRow("无痕模式持续开启", checked = Store.persistIncognito.value) { Store.persistIncognito.value = it }
            SectionHeader("视频")
            SwitchRow("显示视频已看过标记", checked = Store.showWatchedVideoMarker.value) { Store.showWatchedVideoMarker.value = it }
            SwitchRow("视频缩略图", checked = Store.showVideoThumbnails.value) { Store.showVideoThumbnails.value = it }
            SwitchRow("提示可使用上次的播放列表", checked = true) { }
            SwitchRow("锁屏媒体封面", checked = true) { }
            SwitchRow("通知面板中显示定位按钮", checked = false) { }
            Spacer(Modifier.height(30.dp))
        }

        "video" -> SettingsScaffold("视频", onBack) {
            SwitchRow("总是使用快速定位", checked = Store.fastSeek.value) { Store.fastSeek.value = it }
            SwitchRow("使用自定义画中弹窗", checked = Store.customPip.value) { Store.customPip.value = it }
            SwitchRow("恢复后台播放的视频", checked = Store.resumeBackgroundVideo.value) { Store.resumeBackgroundVideo.value = it }
            SwitchRow("匹配显示器帧率", checked = Store.matchFrameRate.value) { Store.matchFrameRate.value = it }
            ArrowRow("首选视频分辨率", value = Store.preferredVideoResolution.value) { dialog = "resolution" }
            ArrowRow("外置显示器/克隆优先", value = "无") { }
            Spacer(Modifier.height(30.dp))
        }

        "subtitles" -> SettingsScaffold("字幕", onBack) {
            ArrowRow("字幕预设", value = "无") { }
            SwitchRow("自动载入字幕", checked = Store.autoLoadSubs.value) { Store.autoLoadSubs.value = it }
            ArrowRow("字幕文本编码", value = Store.subtitleEncoding.value) { dialog = "encoding" }
            ArrowRow("偏好的字幕语言", value = Store.preferredSubLanguage.value) { }
            SectionHeader("字体样式")
            ArrowRow("字幕大小", value = "普通") { dialog = "subsize" }
            SwitchRow("粗体字幕", checked = false) { }
            ArrowRow("颜色", value = "白色") { }
            SliderRow("不透明度", 1f)
            SectionHeader("字幕背景")
            SwitchRow("字幕背景", checked = false) { }
            SectionHeader("字幕阴影")
            SwitchRow("字幕阴影", checked = true) { }
            ValueRow("颜色", "黑色") { }
            SliderRow("不透明度", 1f)
            SectionHeader("字幕轮廓")
            SwitchRow("字幕轮廓", checked = true) { }
            ArrowRow("尺寸", value = "普通") { }
            ValueRow("颜色", "黑色") { }
            Spacer(Modifier.height(30.dp))
        }

        "audio" -> SettingsScaffold("音频", onBack) {
            SwitchRow("通话结束后继续播放", checked = Store.continueAfterCall.value) { Store.continueAfterCall.value = it }
            SwitchRow("滑开应用时停止", checked = Store.stopOnSwipeAway.value) { Store.stopOnSwipeAway.value = it }
            SwitchRow("数字音频输出（直通）", checked = Store.digitalAudioOut.value) { Store.digitalAudioOut.value = it }
            ArrowRow("偏好的音频语言", value = Store.preferredAudioLanguage.value) { }
            ArrowRow("继续播放音频", value = Store.continueAudioPlayback.value) { dialog = "continueaudio" }
            SectionHeader("耳机")
            SwitchRow("启用耳机侦测", checked = Store.headphoneDetection.value) { Store.headphoneDetection.value = it }
            SwitchRow("在插入耳机后继续播放", checked = Store.continueAfterHeadphones.value) { Store.continueAfterHeadphones.value = it }
            SwitchRow("忽略耳机媒体按钮的动作", checked = Store.ignoreHeadsetButtons.value) { Store.ignoreHeadsetButtons.value = it }
            SectionHeader("回放增益")
            SwitchRow("启用回放增益", checked = Store.replayGain.value) { Store.replayGain.value = it }
            Spacer(Modifier.height(30.dp))
        }

        "equalizer" -> EqualizerScreen(onBack)

        "casting" -> SettingsScaffold("投屏", onBack) {
            SwitchRow("无线投屏", checked = Store.castWireless.value) { Store.castWireless.value = it }
            SwitchRow("仅音频", checked = Store.castAudioOnly.value) { Store.castAudioOnly.value = it }
            SwitchRow("音频直通", checked = Store.castPassthrough.value) { Store.castPassthrough.value = it }
            ArrowRow("转换质量", value = "自动") { }
            Spacer(Modifier.height(30.dp))
        }

        "permissions" -> SettingsScaffold("权限", onBack) {
            SettingRow("通知权限", trailing = { VlcIconView(VlcIcon.CHECK, 22.dp, LocalVlcPalette.current.accent) })
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                VlcIconView(VlcIcon.LOCK, 46.dp, LocalVlcPalette.current.textSecondary)
                Spacer(Modifier.width(16.dp))
                Text(
                    "您将只能自动扫描标准格式的媒体文件。",
                    color = LocalVlcPalette.current.textPrimary,
                    fontSize = 15.sp,
                )
            }
            listOf("没有文件权限", "仅有常规媒体权限", "完整访问权限").forEachIndexed { i, label ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { Store.filePermission.value = i }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    VlcRadio(Store.filePermission.value == i) { Store.filePermission.value = i }
                    Spacer(Modifier.width(8.dp))
                    Text(label, color = LocalVlcPalette.current.textPrimary, fontSize = 16.sp)
                }
            }
            SwitchRow("存储设备", checked = Store.storageChecked.value) { Store.storageChecked.value = it }
            Spacer(Modifier.height(30.dp))
        }

        "parental" -> SettingsScaffold("家长控制", onBack) {
            SwitchRow("启用家长控制", checked = false) { }
            ArrowRow("设置密码", value = "") { }
            SwitchRow("过滤成人内容", checked = false) { }
            Spacer(Modifier.height(30.dp))
        }

        "remote" -> SettingsScaffold("远程访问", onBack) {
            SwitchRow("启用远程访问", checked = false) { }
            ValueRow("端口", "8080") { }
            ArrowRow("访问地址", value = "http://127.0.0.1:8080") { }
            Spacer(Modifier.height(30.dp))
        }

        "auto" -> SettingsScaffold("Android Auto", onBack) {
            SwitchRow("启用 Android Auto", checked = true) { }
            SwitchRow("自动播放", checked = false) { }
            Spacer(Modifier.height(30.dp))
        }

        "advanced" -> SettingsScaffold("高级", onBack) {
            SwitchRow("启用画面与声音同步", checked = false) { }
            ValueRow("网络缓存", "1500") { }
            SwitchRow("输出音频到外部设备", checked = false) { }
            ArrowRow("硬件解码", value = "自动") { }
            SwitchRow("保存播放位置", checked = true) { }
            Spacer(Modifier.height(30.dp))
        }
    }

    if (dialog == "theme") {
        ChoiceDialog(
            "自动切换夜间模式",
            listOf("跟随系统模式", "亮色主题", "黑色主题"),
            Store.themeMode.value,
            onSelect = {
                if (it != Store.themeMode.value) {
                    Store.themeMode.value = it
                    Store.persist()
                    dialog = null
                    restartPrompt = true
                } else dialog = null
            },
            onDismiss = { dialog = null },
        )
    }
    if (dialog == "sleep") {
        ChoiceDialog(
            "睡眠计时器",
            listOf("已禁用", "15 分钟", "30 分钟", "60 分钟", "120 分钟"),
            listOf("已禁用", "15 分钟", "30 分钟", "60 分钟", "120 分钟").indexOf(Store.sleepTimer.value).coerceAtLeast(0),
            onSelect = { i ->
                Store.sleepTimer.value = listOf("已禁用", "15 分钟", "30 分钟", "60 分钟", "120 分钟")[i]
                dialog = null
            },
            onDismiss = { dialog = null },
        )
    }
    if (dialog == "encoding") {
        ChoiceDialog(
            "字幕文本编码",
            listOf("Default (Windows-1252)", "Unicode (UTF-8)", "简体中文 (GB18030)", "自动检测"),
            listOf("Default (Windows-1252)", "Unicode (UTF-8)", "简体中文 (GB18030)", "自动检测").indexOf(Store.subtitleEncoding.value).coerceAtLeast(0),
            onSelect = { i ->
                Store.subtitleEncoding.value = listOf("Default (Windows-1252)", "Unicode (UTF-8)", "简体中文 (GB18030)", "自动检测")[i]
                dialog = null
            },
            onDismiss = { dialog = null },
        )
    }
    if (dialog == "resolution") {
        ChoiceDialog(
            "首选视频分辨率",
            listOf("可用的最高画质", "1080p", "720p", "480p"),
            listOf("可用的最高画质", "1080p", "720p", "480p").indexOf(Store.preferredVideoResolution.value).coerceAtLeast(0),
            onSelect = { i ->
                Store.preferredVideoResolution.value = listOf("可用的最高画质", "1080p", "720p", "480p")[i]
                dialog = null
            },
            onDismiss = { dialog = null },
        )
    }
    if (dialog == "subsize") {
        ChoiceDialog("字幕大小", listOf("小", "普通", "大", "特大"), 1, onSelect = { dialog = null }, onDismiss = { dialog = null })
    }
    if (dialog == "language") {
        ChoiceDialog("设置语言", listOf("跟随系统", "简体中文", "English"), 0, onSelect = { dialog = null }, onDismiss = { dialog = null })
    }
    if (dialog == "continueaudio") {
        ChoiceDialog("继续播放音频", listOf("总是", "仅当插入耳机时", "从不"), 0, onSelect = { dialog = null }, onDismiss = { dialog = null })
    }
    if (restartPrompt) {
        VlcDialog(onDismiss = { restartPrompt = false }) {
            Text(
                "重新启动 VLC",
                color = LocalVlcPalette.current.textPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp),
            )
            Text(
                "重新启动 VLC 才能应用您对主题的修改。是否现在重启？",
                color = LocalVlcPalette.current.textPrimary,
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 22.dp),
            )
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, top = 14.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
                Text(
                    "稍后",
                    color = LocalVlcPalette.current.accent,
                    fontSize = 15.sp,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { restartPrompt = false }.padding(horizontal = 14.dp, vertical = 8.dp),
                )
                Text(
                    "确定",
                    color = LocalVlcPalette.current.accent,
                    fontSize = 15.sp,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { restartPrompt = false }.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun SliderRow(label: String, value: Float) {
    val p = LocalVlcPalette.current
    var v by remember { mutableStateOf(value) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(label, color = p.textPrimary, fontSize = 14.sp)
        Slider(
            value = v,
            onValueChange = { v = it },
            colors = SliderDefaults.colors(thumbColor = p.accent, activeTrackColor = p.accent),
        )
    }
}

private val EQ_PRESETS = listOf(
    "Flat", "Classical", "Club", "Dance", "Bass", "Full bass", "Full bass and treble",
    "Full treble", "Headphones", "Large Hall", "Live", "Party", "Pop", "Reggae",
    "Rock", "Ska", "Soft", "Soft rock", "Techno",
)

@Composable
fun EqualizerScreen(onBack: () -> Unit) {
    val p = LocalVlcPalette.current
    SettingsScaffold("均衡器", onBack) {
        SwitchRow("启用均衡器", checked = Store.eqEnabled.value) { Store.eqEnabled.value = it; Store.persist() }
        LazyColumn(Modifier.fillMaxWidth().height(1400.dp)) {
            items(EQ_PRESETS) { preset ->
                val active = Store.eqPreset.value == preset
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            Store.eqPreset.value = preset
                            Store.persist()
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(width = 74.dp, height = 40.dp).clip(RoundedCornerShape(4.dp)).background(p.chip),
                    ) {
                        EqualizerCurve(preset, if (active) p.accent else p.textSecondary)
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(preset, color = if (active) p.accent else p.textPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    VlcIconView(VlcIcon.HEART, 20.dp, p.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun EqualizerCurve(name: String, color: Color) {
    androidx.compose.foundation.Canvas(Modifier.fillMaxSize().padding(6.dp)) {
        val w = size.width
        val h = size.height
        val seed = (name.hashCode() and 0xFF)
        val path = androidx.compose.ui.graphics.Path()
        val steps = 12
        for (i in 0..steps) {
            val x = w * i / steps.toFloat()
            val t = i / steps.toFloat()
            val amp = (((seed * (i + 3)) % 40) / 40f - 0.5f) * (if (i == 0 || i == steps) 0.25f else 1f)
            val y = h / 2f + amp * h * 0.44f + (0.5f - t) * h * 0.12f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.2f))
    }
}

@Composable
fun MediaFoldersScreen(onBack: () -> Unit) {
    val p = LocalVlcPalette.current
    var checked by remember { mutableStateOf(setOf("内部存储/Media", "内部存储/Music", "内部存储/Download")) }
    SettingsScaffold("媒体库文件夹", onBack) {
        Text(
            "VLC 会扫描下列文件夹中的媒体文件。",
            color = p.textSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
        listOf("内部存储/Media", "内部存储/Music", "内部存储/Download", "内部存储/Movies", "内部存储/Pictures", "内部存储/DCIM").forEach { folder ->
            CheckRow(folder, checked = checked.contains(folder)) { on ->
                checked = if (on) checked + folder else checked - folder
            }
        }
        Spacer(Modifier.height(30.dp))
    }
}
