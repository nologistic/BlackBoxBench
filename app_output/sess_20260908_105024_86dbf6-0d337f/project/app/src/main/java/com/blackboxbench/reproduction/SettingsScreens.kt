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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
private fun SettingsHeader(title: String, onBack: () -> Unit, showCheck: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().height(78.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onBack, modifier = Modifier.size(52.dp)) {
            Text("‹", fontSize = 48.sp, fontWeight = FontWeight.Light, color = MaterialTheme.colorScheme.onBackground)
        }
        Text(title, fontSize = 28.sp, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground)
        if (showCheck) {
            TextButton(onClick = onBack, modifier = Modifier.size(52.dp)) {
                Text("✓", fontSize = 28.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String? = null,
    endText: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    height: Int = 68
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(title, fontSize = 20.sp, color = MaterialTheme.colorScheme.onBackground)
            if (subtitle != null) {
                Text(subtitle, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (endText != null) {
            Text(endText, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        if (trailing != null) trailing()
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit, onAppearance: () -> Unit, onWidget: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("calculator_state", 0) }
    var vibration by rememberSaveable { mutableStateOf(preferences.getBoolean("vibration", true)) }
    var keepAwake by rememberSaveable { mutableStateOf(preferences.getBoolean("keep_awake", true)) }
    var message by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
    ) {
        SettingsHeader("设置", onBack)
        SectionTitle("外观")
        SettingsRow("自定义外观", onClick = onAppearance)
        SettingsRow("自定义微件颜色", onClick = onWidget)
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .45f))
        SectionTitle("常规")
        SettingsRow("购买 Fossify Thank You", onClick = { message = "感谢你的支持！" })
        SettingsRow("语言", subtitle = "中文", onClick = { message = "当前语言：中文" })
        SettingsRow(
            title = "按下按钮时振动",
            trailing = {
                Switch(
                    checked = vibration,
                    onCheckedChange = {
                        vibration = it
                        preferences.edit().putBoolean("vibration", it).apply()
                    }
                )
            },
            height = 64
        )
        SettingsRow(
            title = "应用在前台运行时，阻止设备自动休眠",
            trailing = {
                Switch(
                    checked = keepAwake,
                    onCheckedChange = {
                        keepAwake = it
                        preferences.edit().putBoolean("keep_awake", it).apply()
                    }
                )
            },
            height = 72
        )
    }

    if (message.isNotBlank()) {
        AlertDialog(
            onDismissRequest = { message = "" },
            text = { Text(message, fontSize = 19.sp) },
            confirmButton = { TextButton(onClick = { message = "" }) { Text("确定") } }
        )
    }
}

@Composable
fun AppearanceScreen(
    selectedTheme: String,
    onThemeSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    var showThemes by rememberSaveable { mutableStateOf(false) }
    var showFonts by rememberSaveable { mutableStateOf(false) }
    val dark = MaterialTheme.colorScheme.background.red < .2f

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
    ) {
        SettingsHeader("自定义外观", onBack, showCheck = true)
        SectionTitle("主题和颜色")
        SettingsRow("应用主题", endText = selectedTheme, onClick = { showThemes = true })
        SettingsRow(
            title = "应用图标颜色",
            onClick = {},
            trailing = {
                Surface(modifier = Modifier.size(38.dp), shape = CircleShape, color = Color(0xFF087326)) {}
            }
        )
        if (dark) {
            SettingsRow(
                title = "文本颜色",
                trailing = { Surface(Modifier.size(38.dp), CircleShape, Color(0xFFF1F1F1)) {} }
            )
            SettingsRow(
                title = "背景颜色",
                trailing = { Surface(Modifier.size(38.dp), CircleShape, Color(0xFF151515)) {} }
            )
            SettingsRow(
                title = "主要颜色",
                trailing = { Surface(Modifier.size(38.dp), CircleShape, Color(0xFF087326)) {} }
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .45f))
        SectionTitle("字体")
        SettingsRow("应用字体", endText = "系统默认", onClick = { showFonts = true })
    }

    if (showThemes) {
        val choices = listOf("系统默认", "浅色主题", "深色主题", "深红色", "白色", "黑白两色", "自定义")
        AlertDialog(
            onDismissRequest = { showThemes = false },
            text = {
                Column {
                    choices.forEach { choice ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onThemeSelected(choice)
                                    showThemes = false
                                }
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedTheme == choice,
                                onClick = {
                                    onThemeSelected(choice)
                                    showThemes = false
                                }
                            )
                            Text(choice, fontSize = 19.sp)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showFonts) {
        AlertDialog(
            onDismissRequest = { showFonts = false },
            title = { Text("应用字体") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = true, onClick = { showFonts = false })
                    Text("系统默认", fontSize = 19.sp)
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun WidgetPreview() {
    Surface(
        modifier = Modifier.fillMaxWidth().height(500.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color.Black.copy(alpha = .78f)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("15,937×5", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End, fontSize = 20.sp, color = Color(0xFF087A25))
            Text("79,685", modifier = Modifier.fillMaxWidth().padding(top = 46.dp), textAlign = TextAlign.End, fontSize = 43.sp, color = Color(0xFF087A25))
            Spacer(Modifier.height(56.dp))
            val rows = listOf(
                listOf("%", "^", "√", "AC", "÷"),
                listOf("7", "8", "9", "", "×"),
                listOf("4", "5", "6", "", "−"),
                listOf("1", "2", "3", "", "+"),
                listOf("0", ".", "C", "", "=")
            )
            rows.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    row.forEach { key ->
                        Text(
                            key,
                            color = Color(0xFF087A25),
                            fontSize = 25.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WidgetColorScreen(onBack: () -> Unit) {
    var opacity by rememberSaveable { mutableFloatStateOf(.65f) }
    var green by rememberSaveable { mutableStateOf(true) }
    val wallpaper = Brush.linearGradient(
        listOf(Color(0xFFE9EEF8), Color(0xFF233047), Color(0xFF070A13))
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(wallpaper)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        SettingsHeader("微件颜色", onBack)
        WidgetPreview()
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    modifier = Modifier.size(50.dp).clickable { green = false },
                    shape = CircleShape,
                    color = Color.Black,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)
                ) {}
                Surface(
                    modifier = Modifier.size(50.dp).clickable { green = true },
                    shape = CircleShape,
                    color = Color(0xFF087A25),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)
                ) {}
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Slider(value = opacity, onValueChange = { opacity = it })
                Spacer(Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth().height(54.dp).clickable(onClick = onBack),
                    shape = RoundedCornerShape(28.dp),
                    color = if (green) Color(0xFF087A25) else Color.Black
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("确定", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutRow(icon: String, title: String, subtitle: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 24.sp, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.width(38.dp))
        Column {
            Text(title, fontSize = 19.sp, color = MaterialTheme.colorScheme.onBackground)
            if (subtitle != null) Text(subtitle, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    var selected by rememberSaveable { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
    ) {
        SettingsHeader("关于", onBack)
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            AboutRow("?", "常见问题") { selected = "常见问题" }
            AboutRow("⚙", "已知问题") { selected = "已知问题" }
            AboutRow("?", "hello@fossify.org") { selected = "hello@fossify.org" }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .45f))
            SectionTitle("帮助我们")
            AboutRow("⌯", "分享给好友") { selected = "已准备分享计算器" }
            AboutRow("♟", "贡献者") { selected = "感谢所有 Fossify 贡献者" }
            AboutRow("♡", "向 Fossify 捐赠") { selected = "感谢你的支持！" }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .45f))
            SectionTitle("社交网络")
            AboutRow("●", "GitHub") { selected = "GitHub" }
            AboutRow("●", "Reddit") { selected = "Reddit" }
            AboutRow("●", "Telegram") { selected = "Telegram" }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .45f))
            SectionTitle("其他")
            AboutRow("@", "隐私政策") { selected = "隐私政策" }
            AboutRow("▤", "第三方许可") { selected = "第三方许可" }
            AboutRow("ⓘ", "版本 1.4.0", "org.fossify.math") { selected = "Fossify Calculator 1.4.0" }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (selected.isNotBlank()) {
        AlertDialog(
            onDismissRequest = { selected = "" },
            title = { Text(selected) },
            text = {
                Text(
                    when (selected) {
                        "隐私政策" -> "计算数据与历史记录仅保存在此设备上。"
                        "第三方许可" -> "本应用使用 Android 与 Jetpack Compose 开源组件。"
                        "常见问题" -> "长按 C 可清空全部内容；短按 C 删除最后一位。"
                        "已知问题" -> "目前没有已知的严重问题。"
                        else -> selected
                    }
                )
            },
            confirmButton = { TextButton(onClick = { selected = "" }) { Text("确定") } }
        )
    }
}
