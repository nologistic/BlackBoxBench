package com.blackboxbench.reproduction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsPage(model: AppModel) {
    val s = model.settings.value
    Column(Modifier.fillMaxSize()) {
        BackBar("设置", model)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 8.dp)
        ) {
            SettingsHeader("播放")
            SwitchRow(
                title = "耳机断开时暂停",
                subtitle = "拔出耳机自动暂停播放",
                checked = s.pauseOnHeadsetDisconnect,
            ) { s.pauseOnHeadsetDisconnect = it; model.saveSettings() }
            SwitchRow(
                title = "忽略短音频",
                subtitle = "曲库中忽略 10 秒以下的音频",
                checked = s.ignoreShortAudio,
            ) { s.ignoreShortAudio = it; model.saveSettings() }
            ChoiceRow(
                title = "默认播放模式",
                options = listOf("顺序播放", "随机播放", "单曲循环"),
                current = s.defaultPlayMode,
            ) { s.defaultPlayMode = it; model.saveSettings() }
            SettingsHeader("外观")
            ChoiceRow(
                title = "主题",
                options = listOf("浅色", "深色", "跟随系统"),
                current = s.theme,
            ) { s.theme = it; model.saveSettings() }
            SettingsHeader("关于")
            SimpleRow("版本", "1.0.0")
            SimpleRow("曲库统计", "${model.songs.size} 首歌曲")
        }
    }
}

@Composable
private fun SettingsHeader(text: String) {
    Text(
        text, fontSize = 13.sp, color = Color(0xFF00897B), fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, color = Color(0xFF212121))
            Spacer(Modifier.padding(2.dp))
            Text(subtitle, fontSize = 13.sp, color = Color(0xFF9E9E9E))
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ChoiceRow(title: String, options: List<String>, current: String, onPick: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 16.sp, color = Color(0xFF212121), modifier = Modifier.weight(1f))
            Text(
                if (expanded) "收起" else current,
                fontSize = 14.sp, color = Color(0xFF00897B), fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { expanded = !expanded },
            )
        }
        if (expanded) {
            Spacer(Modifier.width(8.dp))
            options.forEach { opt ->
                Text(
                    (if (opt == current) "● " else "○ ") + opt,
                    fontSize = 15.sp,
                    color = if (opt == current) Color(0xFF00897B) else Color(0xFF616161),
                    modifier = Modifier.fillMaxWidth().clickable { onPick(opt); expanded = false }
                        .padding(vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun SimpleRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(label, fontSize = 16.sp, color = Color(0xFF212121), modifier = Modifier.weight(1f))
        Text(value, fontSize = 14.sp, color = Color(0xFF757575))
    }
    HorizontalDivider(color = Color(0xFFEEEEEE))
}

@Composable
fun EqualizerPage(model: AppModel) {
    var enabled by remember { mutableStateOf(false) }
    var preset by remember { mutableStateOf("正常") }
    val bands = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")
    val gains = List(bands.size) { remember { mutableFloatStateOf(0f) } }
    Column(Modifier.fillMaxSize()) {
        BackBar("均衡器", model)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text("启用均衡器", fontSize = 16.sp, color = Color(0xFF212121))
                    Text("调整各频段增益", fontSize = 13.sp, color = Color(0xFF9E9E9E))
                }
                Switch(checked = enabled, onCheckedChange = { enabled = it })
            }
            HorizontalDivider(color = Color(0xFFEEEEEE))
            ChoiceRowInline(
                title = "预设",
                options = listOf("正常", "流行", "摇滚", "爵士", "古典"),
                current = preset, enabled = enabled,
            ) { preset = it }
            HorizontalDivider(color = Color(0xFFEEEEEE))
            bands.forEachIndexed { i, band ->
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Text(
                        "$band  ${if (gains[i].floatValue >= 0) "+" else ""}${"%.1f".format(gains[i].floatValue)} dB",
                        fontSize = 14.sp,
                        color = if (enabled) Color(0xFF212121) else Color(0xFFBDBDBD),
                    )
                    Slider(
                        value = gains[i].floatValue,
                        onValueChange = { if (enabled) gains[i].floatValue = it },
                        valueRange = -12f..12f,
                        enabled = enabled,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChoiceRowInline(
    title: String, options: List<String>, current: String,
    enabled: Boolean, onPick: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 16.sp, color = Color(0xFF212121), modifier = Modifier.weight(1f))
            Text(
                if (expanded) "收起" else current, fontSize = 14.sp,
                color = if (enabled) Color(0xFF00897B) else Color(0xFFBDBDBD),
                modifier = Modifier.clickable { if (enabled) expanded = !expanded },
            )
        }
        if (expanded) {
            options.forEach { opt ->
                Text(
                    (if (opt == current) "● " else "○ ") + opt, fontSize = 15.sp,
                    color = if (opt == current) Color(0xFF00897B) else Color(0xFF616161),
                    modifier = Modifier.fillMaxWidth().clickable { onPick(opt); expanded = false }
                        .padding(vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
fun AboutPage(model: AppModel) {
    Column(Modifier.fillMaxSize()) {
        BackBar("关于", model)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
        ) {
            VinylDisc(album = "V", sizeDp = 140, spinning = false)
            Spacer(Modifier.padding(20.dp))
            Text("Vinyl Music Player", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121))
            Spacer(Modifier.padding(6.dp))
            Text("版本 1.0.0", fontSize = 14.sp, color = Color(0xFF757575))
            Spacer(Modifier.padding(6.dp))
            Text("一款简洁的本地音乐播放器", fontSize = 14.sp, color = Color(0xFF757575))
            Spacer(Modifier.padding(6.dp))
            Text("曲库内容为虚构演示数据", fontSize = 12.sp, color = Color(0xFF9E9E9E))
        }
    }
}
