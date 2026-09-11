package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsHome(onBack: () -> Unit, onOpen: (String) -> Unit) {
    var autoScan by remember { mutableStateOf(true) }
    var playHistory by remember { mutableStateOf(true) }
    LazyColumn(Modifier.fillMaxSize()) {
        item { TopBar("设置", onBack) { Text("⌕", fontSize = 35.sp, color = Color.Gray) } }
        item { SectionTitle("媒体库") }
        item { SettingRow("媒体库文件夹", "选择要收录于媒体库的目录") }
        item { SettingRow("自动重新扫描", "应用启动时，自动扫描设备中新增或删除的媒体文件", autoScan) { autoScan = !autoScan } }
        item { HorizontalDivider() }
        item { SectionTitle("视频") }
        item { SettingRow("背景/画中画模式", "视频播放过程中切换到其他应用程序时 VLC 的行为") }
        item { SettingRow("硬件加速", "禁用：稳定性更高\n解码：可能提升性能\n完全：可能进一步提升性能") }
        item { SettingRow("视频屏幕方向", "自动（传感器感应）") }
        item { HorizontalDivider() }
        item { SectionTitle("网络") }
        item { SettingRow("按流量计费的网络下对串流的处理", "不执行任何操作") }
        item { HorizontalDivider() }
        item { SectionTitle("权限") }
        item { SettingRow("权限", "所有权限的列表") }
        item { HorizontalDivider() }
        item { SectionTitle("历史") }
        item { SettingRow("播放历史", "在「历史记录」下保留所有播放过的媒体", playHistory) { playHistory = !playHistory } }
        item { SettingRow("视频播放队列历史", "允许储存视频播放队列内容，以便稍后继续播放", true, playHistory) {} }
        item { SettingRow("音频播放队列历史", "允许储存音频播放队列内容，以便稍后继续播放", true, playHistory) {} }
        item { HorizontalDivider() }
        item { SectionTitle("附加设置") }
        item { CategoryRow("◉", "界面") { onOpen("interface") } }
        item { CategoryRow("▤", "视频") { onOpen("videoSettings") } }
        item { CategoryRow("▣", "字幕") { onOpen("subtitleSettings") } }
        item { CategoryRow("♪", "音频") { onOpen("audioSettings") } }
        item { CategoryRow("☷", "均衡器") { onOpen("equalizer") } }
        item { CategoryRow("▱", "投屏") { onOpen("cast") } }
        item { CategoryRow("♢", "家长控制") { onOpen("parental") } }
        item { CategoryRow("♨", "远程访问") { onOpen("remote") } }
        item { CategoryRow("▣", "Android Auto") { onOpen("androidAuto") } }
        item { CategoryRow("⌕", "高级") { onOpen("advanced") } }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
fun InterfaceSettings(incognito: Boolean, onIncognito: () -> Unit, onBack: () -> Unit) {
    var tv by remember { mutableStateOf(false) }
    var headers by remember { mutableStateOf(true) }
    var missing by remember { mutableStateOf(true) }
    var persist by remember { mutableStateOf(true) }
    var watched by remember { mutableStateOf(true) }
    var thumbs by remember { mutableStateOf(true) }
    var resume by remember { mutableStateOf(true) }
    var cover by remember { mutableStateOf(true) }
    SettingsList("界面", onBack) {
        item { SettingRow("自动切换夜间模式", "跟随系统模式") }
        item { SettingRow("Android TV 界面", "将界面改为适合电视的主题", tv) { tv = !tv } }
        item { SettingRow("设置语言") }
        item { SettingRow("单行列表标题缩略", "默认") }
        item { SettingRow("显示标题", "根据排序类型按标题拆分列表", headers) { headers = !headers } }
        item { SettingRow("显示缺失的媒体", "显示远程媒体，即使它们不在线", missing) { missing = !missing } }
        item { SettingRow("睡眠计时器", "已禁用") }
        item { SettingRow("无痕模式", "", incognito) { onIncognito() } }
        item { SettingRow("无痕模式持续开启", "保持无痕模式长期开启，即使应用重启也保持开启", persist) { persist = !persist } }
        item { HorizontalDivider() }
        item { SectionTitle("视频") }
        item { SettingRow("显示视频已看过标记", "当视频播放到底时标记为已经看过", watched) { watched = !watched } }
        item { SettingRow("视频缩略图", "在列表中显示视频缩略图", thumbs) { thumbs = !thumbs } }
        item { HorizontalDivider() }
        item { SectionTitle("音频") }
        item { SettingRow("提示可使用上次的播放列表", "应用启动时，显示可继续播放上次内容的提示", resume) { resume = !resume } }
        item { SettingRow("锁屏媒体封面", "可用时，将当前媒体的专辑封面设为锁屏桌面", cover) { cover = !cover } }
        item { SettingRow("通知面板中显示定位按钮", "显示快退、快进按钮", false) {} }
    }
}

@Composable
fun VideoSettings(onBack: () -> Unit) {
    SettingsList("视频", onBack) {
        item { SettingRow("总是使用快速定位", "定位速度更快，但可能不够精确", false) {} }
        item { SettingRow("使用自定义画中画弹窗", "使用大小可调整的自定义画中画弹出窗口", false) {} }
        item { SettingRow("恢复后台播放的视频", "重新打开 VLC 时恢复后台播放的视频", false) {} }
        item { SettingRow("匹配显示器帧率", "使显示刷新率与媒体帧率相匹配", false) {} }
        item { SettingRow("首选视频分辨率", "可用的最高画质") }
        item { HorizontalDivider() }
        item { SectionTitle("外置显示器") }
        item { SettingRow("连接外部显示器时的设置", "HDMI/Chromecast") }
        item { SettingRow("克隆优先", "克隆设备屏幕，不使用遥控功能", false) {} }
    }
}

@Composable
fun SubtitleSettings(onBack: () -> Unit) {
    var auto by remember { mutableStateOf(true) }
    var bold by remember { mutableStateOf(false) }
    var background by remember { mutableStateOf(false) }
    var shadow by remember { mutableStateOf(true) }
    var outline by remember { mutableStateOf(true) }
    var opacity by remember { mutableFloatStateOf(1f) }
    SettingsList("字幕", onBack) {
        item { SettingRow("字幕预设") }
        item { SettingRow("自动载入字幕", "", auto) { auto = !auto } }
        item { SettingRow("字幕文本编码", "Default (Windows-1252)") }
        item { SettingRow("偏好的字幕语言", "无语言偏好") }
        item { HorizontalDivider() }
        item { SectionTitle("字幕字体样式") }
        item { SettingRow("字幕大小", "普通") }
        item { SettingRow("粗体字幕", "", bold) { bold = !bold } }
        item { SettingRow("颜色", "白色") }
        item { Text("不透明度", fontSize = 18.sp, modifier = Modifier.padding(horizontal = 16.dp)) }
        item { Slider(opacity, { opacity = it }, modifier = Modifier.padding(horizontal = 16.dp), colors = SliderDefaults.colors(thumbColor = VlcOrange, activeTrackColor = VlcOrange)) }
        item { HorizontalDivider() }
        item { SectionTitle("字幕背景") }
        item { SettingRow("字幕背景", "", background) { background = !background } }
        item { SectionTitle("字幕阴影") }
        item { SettingRow("字幕阴影", "", shadow) { shadow = !shadow } }
        item { SettingRow("颜色", "黑色") }
        item { SectionTitle("字幕轮廓") }
        item { SettingRow("字幕轮廓", "", outline) { outline = !outline } }
        item { SettingRow("尺寸", "普通") }
        item { SettingRow("颜色", "黑色") }
    }
}

@Composable
fun AudioSettings(onBack: () -> Unit) {
    var call by remember { mutableStateOf(true) }
    var headset by remember { mutableStateOf(true) }
    var gain by remember { mutableStateOf(false) }
    SettingsList("音频", onBack) {
        item { SettingRow("通话结束后继续播放", "否则保持暂停", call) { call = !call } }
        item { SettingRow("滑开应用时停止", "应用程序被滑开时停止播放", false) {} }
        item { SettingRow("数字音频输出（直通）", "音频数字输出已禁用", false) {} }
        item { SettingRow("偏好的音频语言", "无语言偏好") }
        item { SettingRow("继续播放音频", "总是") }
        item { HorizontalDivider() }
        item { SectionTitle("耳机") }
        item { SettingRow("启用耳机侦测", "检测耳机的插入和拔出", headset) { headset = !headset } }
        item { SettingRow("在插入耳机后继续播放", "否则暂停", false) {} }
        item { SettingRow("忽略耳机媒体按钮的动作", "适用于物理按键损坏的情况", false) {} }
        item { HorizontalDivider() }
        item { SectionTitle("回放增益") }
        item { SettingRow("启用回放增益", "为音量较小的音频流提供一致响度", gain) { gain = !gain } }
        item { SettingRow("回放增益模式", "音轨模式", null, gain) {} }
        item { SettingRow("回放前置放大", "目标电平 89 dB", null, gain) {} }
        item { SettingRow("峰值保护", "防止爆音", true, gain) {} }
        item { HorizontalDivider() }
        item { SectionTitle("高级") }
        item { SettingRow("MIDI 音色库", "选择用于播放 MIDI 音轨的 SoundFont 文件") }
    }
}

@Composable
fun EqualizerScreen(enabled: Boolean, preset: String, onEnabled: (Boolean) -> Unit, onPreset: (String) -> Unit, onBack: () -> Unit) {
    var editor by remember { mutableStateOf(false) }
    val presets = listOf("Flat", "Classical", "Club", "Dance", "Full bass", "Full bass and treble", "Full treble", "Headphones", "Large Hall", "Live", "Party", "Pop")
    Column(Modifier.fillMaxSize()) {
        TopBar("均衡器", onBack) {
            Text("☷", fontSize = 27.sp, modifier = Modifier.clickable { editor = !editor }.padding(8.dp))
            Text("⇧", fontSize = 25.sp, modifier = Modifier.padding(8.dp))
            Text("⋮", fontSize = 28.sp, modifier = Modifier.padding(8.dp))
        }
        if (!editor) {
            LazyColumn {
                presets.forEach { name ->
                    item {
                        Row(
                            Modifier.fillMaxWidth().background(if (preset == name) Color(0xFFE9E9E9) else Color.White).clickable { onPreset(name) }.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(42.dp).background(Color(0xFFC5C5C5), RoundedCornerShape(5.dp)), contentAlignment = Alignment.Center) {
                                Text(if (preset == name) "✓" else "⌁", color = Color(0xFF666666), fontSize = 25.sp)
                            }
                            Text(name, fontSize = 19.sp, modifier = Modifier.weight(1f).padding(start = 16.dp))
                            if (preset != name) Text("◉", fontSize = 25.sp, color = Color(0xFF666666))
                        }
                    }
                }
            }
        } else {
            EqualizerEditor(enabled, preset, onEnabled, onPreset, presets)
        }
    }
}

@Composable
private fun EqualizerEditor(enabled: Boolean, preset: String, onEnabled: (Boolean) -> Unit, onPreset: (String) -> Unit, presets: List<String>) {
    var menu by remember { mutableStateOf(false) }
    val labels = listOf("31Hz", "64Hz", "125Hz", "250Hz", "500Hz", "1kHz", "2kHz", "4kHz", "8kHz", "16kHz")
    val start = if (preset == "Classical") listOf(.5f,.5f,.5f,.5f,.5f,.5f,.28f,.28f,.28f,.2f) else List(10) { .5f }
    var values by remember(preset) { mutableStateOf(start) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("启用", fontSize = 20.sp, modifier = Modifier.weight(1f))
            Checkbox(enabled, { onEnabled(it) }, colors = CheckboxDefaults.colors(checkedColor = VlcOrange))
        }
        Box {
            VlcButton(preset, { menu = true }, filled = false)
            DropdownMenu(menu, onDismissRequest = { menu = false }) {
                presets.forEach { p -> DropdownMenuItem(text = { Text(p) }, onClick = { onPreset(p); menu = false }) }
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(preset, fontSize = 21.sp, fontWeight = FontWeight.Medium)
        Text("前置放大器", color = Color.Gray, modifier = Modifier.padding(top = 12.dp))
        Slider(.72f, {}, colors = SliderDefaults.colors(thumbColor = Color.Gray, activeTrackColor = Color.Gray))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEachIndexed { index, label ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(35.dp)) {
                    Text(((values[index] - .5f) * 32).toInt().toString() + "dB", fontSize = 10.sp, color = Color.Gray)
                    Slider(
                        value = values[index],
                        onValueChange = { v -> values = values.toMutableList().also { it[index] = v } },
                        modifier = Modifier.height(170.dp).width(32.dp),
                        colors = SliderDefaults.colors(thumbColor = Color.Gray, activeTrackColor = Color.Gray)
                    )
                    Text(label, fontSize = 9.sp, textAlign = TextAlign.Center)
                }
            }
        }
        SettingRow("同时影响附近频段", "", true) {}
    }
}

@Composable
fun CastSettings(onBack: () -> Unit) {
    var cast by remember { mutableStateOf(true) }
    SettingsList("投屏", onBack) {
        item { SectionTitle("投屏") }
        item { SettingRow("无线投屏", "", cast) { cast = !cast } }
        item { SettingRow("仅音频", "只投屏音频，无视频", false) {} }
        item { SettingRow("音频直通", "让 TV 全权负责音频渲染", false) {} }
        item { SettingRow("转换质量", "选择投屏到远程屏幕时的转换质量") }
    }
}

@Composable
fun ParentalControl(onBack: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        TopBar("家长控制", onBack)
        Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("首先，我们需要设置安全密码", fontSize = 18.sp, color = Color(0xFF666666))
            Text("请输入新密码", fontSize = 25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp, bottom = 30.dp))
            OutlinedTextField(
                pin, { if (it.length <= 4) pin = it.filter(Char::isDigit) },
                label = { Text("四位数字密码") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true
            )
            Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                repeat(4) { i -> Box(Modifier.size(56.dp).border(2.dp, if (i <= pin.length) VlcOrange else Color.Gray, RoundedCornerShape(5.dp)), contentAlignment = Alignment.Center) { Text(if (i < pin.length) "●" else "") } }
            }
        }
    }
}

@Composable
fun RemoteSettings(enabled: Boolean, onEnabled: (Boolean) -> Unit, onBack: () -> Unit) {
    var showStatus by remember { mutableStateOf(false) }
    if (showStatus) {
        Column(Modifier.fillMaxSize()) {
            TopBar("远程访问", { showStatus = false })
            SectionTitle("服务器状态")
            SettingRow(if (enabled) "远程访问处于活动状态" else "远程访问已停止", if (enabled) "停止" else "")
            SectionTitle("链接")
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("http://10.0.2.15:8080", fontSize = 18.sp, color = Color.Gray, modifier = Modifier.weight(1f))
                Text("▦  ↗  □", fontSize = 24.sp)
            }
        }
    } else {
        SettingsList("远程访问", onBack) {
            item { SettingRow("启用远程访问", "", enabled) { onEnabled(!enabled) } }
            item { SettingRow("服务器状态", "轻按可查看服务器状态", null, enabled) { showStatus = true } }
            item { HorizontalDivider() }
            item { SectionTitle("内容") }
            item { SettingRow("媒体库内容", "已启用: 视频 · 音频 · 播放列表 · 搜索\n已禁用: -", null, enabled) {} }
            item { SettingRow("文件浏览器", "", false, enabled) {} }
            item { SettingRow("网络浏览器", "", false, enabled) {} }
            item { SettingRow("历史", "", false, enabled) {} }
            item { SettingRow("播放控制", "", true, enabled) {} }
            item { SettingRow("分享日志文件", "", false, enabled) {} }
        }
    }
}

@Composable
fun AndroidAutoSettings(onBack: () -> Unit) {
    var titleSize by remember { mutableFloatStateOf(.9f) }
    var subtitleSize by remember { mutableFloatStateOf(.9f) }
    SettingsList("Android Auto", onBack) {
        item { SectionTitle("界面") }
        item { Text("标题文字大小", fontSize = 18.sp, modifier = Modifier.padding(horizontal = 16.dp)) }
        item { Slider(titleSize, { titleSize = it }, modifier = Modifier.padding(horizontal = 16.dp), colors = SliderDefaults.colors(thumbColor = VlcOrange, activeTrackColor = VlcOrange)) }
        item { Text("字幕文字大小", fontSize = 18.sp, modifier = Modifier.padding(horizontal = 16.dp)) }
        item { Slider(subtitleSize, { subtitleSize = it }, modifier = Modifier.padding(horizontal = 16.dp), colors = SliderDefaults.colors(thumbColor = VlcOrange, activeTrackColor = VlcOrange)) }
        item { SettingRow("队列信息", "字幕之前") }
        item { SettingRow("队列格式", "队列位置 / 队列总长") }
        item { HorizontalDivider() }
        item { SectionTitle("控制按钮") }
        item { SettingRow("使用全局播放速度", "针对所有轨道使用已经设置的全局播放速度", false) {} }
        item { SettingRow("Android Auto 播放速度", "在弹出菜单中显示速度控制按钮", false) {} }
        item { SettingRow("Android Auto 定位按钮", "在溢出菜单中显示快退与快进", false) {} }
    }
}

@Composable
fun AdvancedSettings(onBack: () -> Unit) {
    var smb by remember { mutableStateOf(true) }
    var stretch by remember { mutableStateOf(true) }
    var verbose by remember { mutableStateOf(true) }
    SettingsList("高级", onBack) {
        item { SettingRow("网络缓存值", "缓冲网络媒体流供软件解码的时间量（毫秒）\n设为 0 可禁用缓冲") }
        item { SettingRow("优先使用 SMB v1 协议", "如果浏览 SMB 服务器时出现困难，请尝试取消勾选此设置", smb) { smb = !smb } }
        item { SettingRow("HTTP 用户代理字符串", "未设置") }
        item { SettingRow("退出并重新启动应用程序") }
        item { HorizontalDivider() }
        item { SectionTitle("应用程序数据") }
        item { SettingRow("转储媒体数据库", "将数据库复制到内部存储根目录") }
        item { SettingRow("转储应用数据库", "将数据库复制到内部存储根目录") }
        item { SettingRow("清除媒体数据库", "清除数据库并从头开始") }
        item { SettingRow("清除应用数据", "清除 VLC for Android 的数据") }
        item { SettingRow("清除播放历史记录") }
        item { SettingRow("导出设置", "将设置参数导出为单独的文件") }
        item { SettingRow("恢复设置", "使用之前导出的文件恢复您的设置参数") }
        item { HorizontalDivider() }
        item { SectionTitle("性能") }
        item { SettingRow("启用音频的时间伸缩", "加速或减慢音频，但保持音调不变", stretch) { stretch = !stretch } }
        item { SettingRow("OpenGL ES2 的使用", "默认情况下，仅在需要时使用") }
        item { SettingRow("音频输出", "更改 VLC 输出音频的方式") }
        item { SettingRow("去块滤镜设置", "可能提升画质（仅供高级用户使用）") }
        item { SettingRow("跳帧", "加速解码但可能降低画质", false) {} }
        item { SettingRow("Dav1d 线程数", "未设置") }
        item { SettingRow("浏览器快速播放按钮", "在浏览器项目菜单中添加快速播放操作", false) {} }
        item { HorizontalDivider() }
        item { SectionTitle("开发人员") }
        item { SettingRow("详尽模式", "增加详尽程度 (logcat)", verbose) { verbose = !verbose } }
        item { SettingRow("调试日志") }
        item { SettingRow("安装每日构建版") }
        item { SettingRow("自定义 libVLC 选项") }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SettingsList(title: String, onBack: () -> Unit, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        item { TopBar(title, onBack) { Text("⌕", fontSize = 35.sp, color = Color.Gray) } }
        content()
    }
}
