package com.blackboxbench.reproduction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    PageScaffold("设置", onBack) {
        item { SettingsCard("♡", "捐赠", "考虑用捐赠显示您的支持！") {} }
        item {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                Column {
                    SettingsRow("⌁", "本地清单") {}
                    HorizontalDivider()
                    SettingsRow("+", "添加账号") { onNavigate("account") }
                }
            }
        }
        item { SettingsCard("◉", "外观") { onNavigate("appearance") } }
        item { SettingsCard("♟", "通知") { onNavigate("notifications") } }
        item { SettingsCard("+", "任务默认值") { onNavigate("defaults") } }
        item { SettingsCard("☷", "任务清单选项") { onNavigate("listOptions") } }
        item { SettingsCard("✎", "编辑屏幕选项") { onNavigate("editOptions") } }
        item { SettingsCard("◷", "日期和时间") {} }
        item { SettingsCard("☰", "导航抽屉") {} }
        item { SettingsCard("▣", "备份", "导出、导入和自动备份") {} }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageScaffold(title: String, onBack: () -> Unit, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    Scaffold(
        topBar = {
            Row(Modifier.statusBarsPadding().fillMaxWidth().height(74.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("‹", fontSize = 43.sp, modifier = Modifier.clickable(onClick = onBack).padding(8.dp))
                Text(title, fontSize = 30.sp, modifier = Modifier.padding(start = 10.dp))
            }
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 30.dp), content = content)
    }
}

@Composable
fun SettingsCard(icon: String, title: String, subtitle: String = "", onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clickable(onClick = onClick)
    ) { SettingsRow(icon, title, subtitle, onClick) }
}

@Composable
fun SettingsRow(icon: String, title: String, subtitle: String = "", onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 28.sp, modifier = Modifier.width(46.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 21.sp)
            if (subtitle.isNotBlank()) Text(subtitle, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .72f))
        }
        if (subtitle.isNotBlank()) Text("›", fontSize = 30.sp)
    }
}

@Composable
fun AppearanceScreen(dark: Boolean, onDarkChange: (Boolean) -> Unit, onBack: () -> Unit) {
    var markdown by remember { mutableStateOf(false) }
    var dynamic by remember { mutableStateOf(false) }
    var themeDialog by remember { mutableStateOf(false) }
    PageScaffold("外观", onBack) {
        item { SettingsCard("◐", "主题", if (dark) "暗色" else "亮色") { themeDialog = true } }
        item { CardSettingSwitch("动态", dynamic) { dynamic = it } }
        item { SettingsCard("●", "颜色") {} }
        item { SettingsCard("●", "启动器图标") {} }
        item { CardSettingSwitch("Markdown", markdown, "在任务标题和描述中启用 Markdown") { markdown = it } }
        item { SectionLabel("启动时") }
        item { CardSettingSwitch("打开上次查看的清单", true) {} }
        item { SectionLabel("本地化") }
        item { SettingsCard("文", "语言", "中文（中国）") {} }
        item { SettingsCard("↗", "贡献翻译") {} }
    }
    if (themeDialog) {
        AlertDialog(
            onDismissRequest = { themeDialog = false },
            title = { Text("主题") },
            text = {
                Column {
                    listOf("亮色", "黑色", "暗色", "壁纸", "日/夜", "系统默认").forEach { value ->
                        Row(Modifier.fillMaxWidth().clickable {
                            onDarkChange(value == "暗色" || value == "黑色")
                            themeDialog = false
                        }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = (dark && value == "暗色") || (!dark && value == "亮色"), onClick = null)
                            Text(value, fontSize = 19.sp)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    var combine by remember { mutableStateOf(true) }
    var voice by remember { mutableStateOf(false) }
    var allDay by remember { mutableStateOf(true) }
    var snooze by remember { mutableStateOf(false) }
    var quiet by remember { mutableStateOf(false) }
    PageScaffold("通知", onBack) {
        item { SettingsCard("↗", "疑难解答", "如果您在通知方面遇到问题，请点按此处") {} }
        item { SettingsCard("↗", "禁用电池优化", "电池优化可能会延迟通知") {} }
        item { SettingsCard("♩", "播放完成声音", "默认") {} }
        item { CardSettingSwitch("合并通知", combine, "将多个通知合并为一个通知") { combine = it } }
        item { CardSettingSwitch("语音提醒", voice, "Tasks 会在任务提醒时读出任务名") { voice = it } }
        item { SettingsCard("↗", "更多设置", "铃声、振动及更多") {} }
        item { SectionLabel("全天任务") }
        item { CardSettingSwitch("添加默认提醒", allDay, "为全天任务添加默认提醒") { allDay = it } }
        item { SettingsCard("◷", "提醒时间", "18:00") {} }
        item { SectionLabel("滑动延后") }
        item { CardSettingSwitch("启用", snooze) { snooze = it } }
        item { SectionLabel("静音时间") }
        item { CardSettingSwitch("启用", quiet, "静音期间不提醒") { quiet = it } }
    }
}

@Composable
fun DefaultsScreen(lists: List<TaskList>, onBack: () -> Unit) {
    var top by remember { mutableStateOf(true) }
    PageScaffold("任务默认值", onBack) {
        item { CardSettingSwitch("新任务显示在顶部", top) { top = it } }
        item { SettingsCard("☷", "默认清单", lists.firstOrNull()?.name ?: "默认清单") {} }
        item { SettingsCard("◇", "默认标签", "无") {} }
        item { SettingsCard("⚑", "默认优先级", "低") {} }
        item { SettingsCard("▣", "默认的开始日期", "无开始日期") {} }
        item { SettingsCard("◷", "默认截止日期", "无截止日期") {} }
        item { SettingsCard("□", "默认日历", "不添加到日历") {} }
        item { SectionLabel("提醒") }
        item { SettingsCard("♟", "默认提醒", "开始后 · 到期时 · 到期后 1 天") {} }
    }
}

@Composable
fun ListOptionsScreen(onBack: () -> Unit) {
    var showNotes by remember { mutableStateOf(true) }
    var fullTitle by remember { mutableStateOf(false) }
    var fullNotes by remember { mutableStateOf(false) }
    var links by remember { mutableStateOf(false) }
    PageScaffold("任务清单选项", onBack) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.padding(16.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text("字体大小                         16", fontSize = 20.sp)
                    Slider(value = .18f, onValueChange = {})
                    Text("行间距                             16", fontSize = 20.sp)
                    Slider(value = 1f, onValueChange = {})
                }
            }
        }
        item { CardSettingSwitch("显示完整的任务标题", fullTitle) { fullTitle = it } }
        item { CardSettingSwitch("显示描述", showNotes) { showNotes = it } }
        item { CardSettingSwitch("显示完整描述", fullNotes) { fullNotes = it } }
        item { CardSettingSwitch("显示链接", links, "添加网站、地址、电话号码等链接") { links = it } }
        item { SettingsCard("▤", "纸片外观", "文本和图标") {} }
    }
}

@Composable
fun EditOptionsScreen(onBack: () -> Unit) {
    var links by remember { mutableStateOf(false) }
    var saveBack by remember { mutableStateOf(false) }
    PageScaffold("编辑屏幕选项", onBack) {
        item { SettingsCard("☷", "自定义编辑屏幕", "重新安排或删除字段") {} }
        item { CardSettingSwitch("显示链接", links, "添加网站、地址、电话号码等链接") { links = it } }
        item { CardSettingSwitch("按返回键时保存任务", saveBack) { saveBack = it } }
        item { CardSettingSwitch("允许多行标题", false, "按完成保存任务") {} }
        item { CardSettingSwitch("在任务编辑中显示注释", false) {} }
        item { CardSettingSwitch("不解锁显示编辑屏", false, "允许不解锁设备的情况下使用快速设置") {} }
    }
}

@Composable
fun AccountsScreen(onBack: () -> Unit) {
    PageScaffold("添加账号", onBack) {
        item { AccountCard("✓", "Tasks.org Cloud", "包括好友和家庭共享、邮件转任务") }
        item { AccountCard("✓", "Microsoft To Do", "与您的个人 Microsoft 账号同步") }
        item { AccountCard("G", "Google Tasks", "基本服务，用您的 Google 账号进行数据同步") }
        item { AccountCard("DAV", "DAVx⁵", "使用 DAVx⁵ 应用同步您的任务") }
        item { AccountCard("DAV", "CalDAV", "基于开放的互联网标准的同步") }
        item { AccountCard("↻", "EteSync", "端到端加密的同步") }
        item { AccountCard("◎", "DecSync CC", "基于文件的同步") }
    }
}

@Composable
private fun AccountCard(icon: String, title: String, subtitle: String) {
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = Color(0xFF2196F3), modifier = Modifier.size(46.dp)) {
                Box(contentAlignment = Alignment.Center) { Text(icon, color = Color.White, fontSize = 15.sp) }
            }
            Column(Modifier.padding(start = 16.dp)) {
                Text(title, fontSize = 20.sp)
                Text(subtitle, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .72f))
            }
        }
    }
}

@Composable
fun CardSettingSwitch(title: String, value: Boolean, subtitle: String = "", onChange: (Boolean) -> Unit) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp)
    ) { SettingSwitch(title, value, subtitle, onChange) }
}

@Composable
fun SettingSwitch(title: String, value: Boolean, subtitle: String = "", onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 19.sp)
            if (subtitle.isNotBlank()) Text(subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .68f))
        }
        Switch(checked = value, onCheckedChange = onChange)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = MaterialTheme.colorScheme.primary, fontSize = 17.sp, modifier = Modifier.padding(start = 18.dp, top = 18.dp, bottom = 5.dp))
}
