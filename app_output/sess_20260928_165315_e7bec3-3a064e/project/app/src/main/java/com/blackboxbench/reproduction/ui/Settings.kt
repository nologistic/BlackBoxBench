package com.blackboxbench.reproduction.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blackboxbench.reproduction.BlueColor
import com.blackboxbench.reproduction.Prefs
import com.blackboxbench.reproduction.Settings
import com.blackboxbench.reproduction.StateStore
import com.blackboxbench.reproduction.ThemeState
import com.blackboxbench.reproduction.priorityName

private val themeOptions = listOf("亮色", "黑色", "暗色", "壁纸", "日", "夜", "系统默认")
private val themeKeys = listOf("light", "black", "dark", "wallpaper", "day", "night", "system")

fun themeName(key: String): String = themeKeys.indexOf(key).let { if (it >= 0) themeOptions[it] else "系统默认" }

@Composable
fun SettingsScreen(onBack: () -> Unit, startPage: String = "root") {
    var page by remember { mutableStateOf(startPage) }

    androidx.activity.compose.BackHandler {
        if (page != "root") page = "root" else onBack()
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        SimpleTopBar(
            title = when (page) {
                "root" -> "设置"
                "appearance" -> "外观"
                "notifications" -> "通知"
                "defaults" -> "任务默认值"
                "listOptions" -> "任务清单选项"
                "editorOptions" -> "编辑屏幕选项"
                "dateTime" -> "日期和时间"
                "drawer" -> "导航抽屉"
                "backup" -> "备份"
                "advanced" -> "高级"
                "about" -> "关于"
                else -> "设置"
            },
            onBack = {
                if (page != "root") page = "root" else onBack()
            }
        )
        when (page) {
            "root" -> SettingsRoot(onNavigate = { page = it })
            "appearance" -> AppearancePage()
            "notifications" -> NotificationsPage()
            "defaults" -> DefaultsPage()
            "listOptions" -> ListOptionsPage()
            "editorOptions" -> EditorOptionsPage()
            "dateTime" -> DateTimePage()
            "drawer" -> DrawerSettingsPage()
            "backup" -> BackupPage()
            "advanced" -> AdvancedPage(onReset = {
                Prefs.str("dummy") // no-op; reset handled in dialog
            })
            "about" -> AboutPage()
        }
    }
}

@Composable
private fun SettingsRoot(onNavigate: (String) -> Unit) {
    var showOffline by remember { mutableStateOf(false) }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        SettingsCard {
            SettingsRow("❤  捐赠", subtitle = "考虑用捐赠显示您的支持！", onClick = {})
        }
        SettingsCard {
            SettingsRow("≣  本地清单", onClick = {})
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SettingsRow("👤  添加账号", onClick = { showOffline = true })
        }
        SettingsCard {
            SettingsRow("🎨  外观", onClick = { onNavigate("appearance") })
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SettingsRow("🔔  通知", onClick = { onNavigate("notifications") })
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SettingsRow("✓  任务默认值", onClick = { onNavigate("defaults") })
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SettingsRow("📋  任务清单选项", onClick = { onNavigate("listOptions") })
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SettingsRow("📝  编辑屏幕选项", onClick = { onNavigate("editorOptions") })
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SettingsRow("🗓  日期和时间", onClick = { onNavigate("dateTime") })
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SettingsRow("☰  导航抽屉", onClick = { onNavigate("drawer") })
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SettingsRow("💾  备份", trailing = { Text("⚠", color = Color(0xFFF9A825)) }, onClick = { onNavigate("backup") })
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SettingsRow("🧩  插件设置", onClick = {})
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SettingsRow("⚙  高级", onClick = { onNavigate("advanced") })
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SettingsRow("ℹ  关于", onClick = { onNavigate("about") })
        }
    }
    if (showOffline) {
        OfflineAccountDialog(onDismiss = { showOffline = false })
    }
}

// ---------------- switches ----------------

@Composable
private fun SwitchRow(title: String, subtitle: String = "", checked: Boolean, onChange: (Boolean) -> Unit) {
    var v by remember(checked) { mutableStateOf(checked) }
    SettingsRow(
        title = title,
        subtitle = subtitle,
        trailing = {
            Switch(
                checked = v,
                onCheckedChange = {
                    v = it
                    onChange(it)
                },
                colors = SwitchDefaults.colors(checkedTrackColor = BlueColor)
            )
        }
    )
}

// ---------------- appearance ----------------

@Composable
private fun AppearancePage() {
    var showTheme by remember { mutableStateOf(false) }
    Column(Modifier.verticalScroll(rememberScrollState())) {
        SettingsCard {
            SettingsRow("主题", subtitle = themeName(Settings.theme), onClick = { showTheme = true })
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SwitchRow("动态", checked = Settings.dynamic) { Settings.dynamic = it }
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SettingsRow("颜色", subtitle = "蓝色", onClick = {})
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SettingsRow("启动器图标", subtitle = "默认", onClick = {})
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SwitchRow("Markdown", checked = Settings.markdown) { Settings.markdown = it }
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SwitchRow("启动时打开上次查看的清单", checked = Settings.openLastView) { Settings.openLastView = it }
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SettingsRow("语言", subtitle = "中文 (中国)", onClick = {})
        }
    }
    if (showTheme) {
        var selected by remember { mutableStateOf(Settings.theme) }
        AlertDialog(
            onDismissRequest = { showTheme = false },
            title = { Text("主题") },
            text = {
                Column {
                    themeOptions.forEachIndexed { i, label ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { selected = themeKeys[i] }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selected == themeKeys[i], onClick = { selected = themeKeys[i] })
                            Spacer(Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    Settings.theme = selected
                    ThemeState.current = selected
                    showTheme = false
                }) { Text("确定", color = BlueColor) }
            },
            dismissButton = {
                TextButton(onClick = { showTheme = false }) { Text("取消") }
            }
        )
    }
}

// ---------------- notifications ----------------

@Composable
private fun NotificationsPage() {
    var showSound by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var showOffline by remember { mutableStateOf(false) }
    Column(Modifier.verticalScroll(rememberScrollState())) {
        SettingsCard {
            SettingsRow("疑难解答", trailing = { Text("🔗", fontSize = 14.sp) }, onClick = { showOffline = true })
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SettingsRow("禁用电池优化", onClick = {})
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SettingsRow("播放完成声音", subtitle = Settings.completionSound, onClick = { showSound = true })
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SwitchRow("合并通知", subtitle = "将多个通知合并为一个通知", checked = Settings.mergeNotifications) {
                Settings.mergeNotifications = it
            }
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SwitchRow("语音提醒", subtitle = "Tasks 会在任务提醒时读出任务名", checked = Settings.voiceReminders) {
                Settings.voiceReminders = it
            }
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SettingsRow("更多设置", onClick = {})
        }
        SectionHeader("「全天任务」")
        SettingsCard {
            SwitchRow("添加默认提醒", checked = Settings.allDayReminder) { Settings.allDayReminder = it }
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SettingsRow("提醒时间", subtitle = Settings.reminderTime, onClick = { showTime = true })
        }
    }
    if (showSound) {
        var sel by remember { mutableStateOf(Settings.completionSound) }
        AlertDialog(
            onDismissRequest = { showSound = false },
            title = { Text("播放完成声音") },
            text = {
                Column {
                    listOf("默认", "无", "提示音 1", "提示音 2").forEach { opt ->
                        Row(
                            Modifier.fillMaxWidth().clickable { sel = opt }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = sel == opt, onClick = { sel = opt })
                            Spacer(Modifier.width(8.dp))
                            Text(opt)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    Settings.completionSound = sel
                    showSound = false
                }) { Text("确定", color = BlueColor) }
            },
            dismissButton = { TextButton(onClick = { showSound = false }) { Text("取消") } }
        )
    }
    if (showTime) {
        var sel by remember { mutableStateOf(Settings.reminderTime) }
        AlertDialog(
            onDismissRequest = { showTime = false },
            title = { Text("提醒时间") },
            text = {
                Column {
                    listOf("09:00", "12:00", "18:00", "20:00", "21:00").forEach { opt ->
                        Row(
                            Modifier.fillMaxWidth().clickable { sel = opt }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = sel == opt, onClick = { sel = opt })
                            Spacer(Modifier.width(8.dp))
                            Text(opt)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    Settings.reminderTime = sel
                    showTime = false
                }) { Text("确定", color = BlueColor) }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("取消") } }
        )
    }
    if (showOffline) {
        OfflineAccountDialog(onDismiss = { showOffline = false })
    }
}

// ---------------- task defaults ----------------

@Composable
private fun DefaultsPage() {
    var showPriority by remember { mutableStateOf(false) }
    var showList by remember { mutableStateOf(false) }
    Column(Modifier.verticalScroll(rememberScrollState())) {
        SettingsCard {
            SwitchRow("新任务显示在顶部", checked = Settings.newTasksAtTop) { Settings.newTasksAtTop = it }
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SettingsRow(
                "默认清单",
                subtitle = StateStore.lists.firstOrNull { it.id == Settings.defList }?.name ?: "默认清单",
                onClick = { showList = true }
            )
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SettingsRow("默认标签", subtitle = "无", onClick = {})
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SettingsRow("默认优先级", subtitle = priorityName(Settings.defPriority), onClick = { showPriority = true })
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SettingsRow("默认开始日期", subtitle = Settings.defaultStart, onClick = {})
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SettingsRow("默认截止日期", subtitle = Settings.defaultDue, onClick = {})
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SettingsRow("默认日历", subtitle = Settings.defaultCalendar, onClick = {})
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SettingsRow(
                "默认提醒",
                subtitle = "开始后、到期时、到期后1天（每1天重复6次）",
                onClick = {}
            )
        }
    }
    if (showPriority) {
        var sel by remember { mutableStateOf(Settings.defPriority) }
        AlertDialog(
            onDismissRequest = { showPriority = false },
            title = { Text("默认优先级") },
            text = {
                Column {
                    listOf(1, 2, 3, 0).forEach { p ->
                        Row(
                            Modifier.fillMaxWidth().clickable { sel = p }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = sel == p, onClick = { sel = p })
                            Spacer(Modifier.width(8.dp))
                            Text(priorityName(p))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    Settings.defPriority = sel
                    showPriority = false
                }) { Text("确定", color = BlueColor) }
            },
            dismissButton = { TextButton(onClick = { showPriority = false }) { Text("取消") } }
        )
    }
    if (showList) {
        AlertDialog(
            onDismissRequest = { showList = false },
            title = { Text("默认清单") },
            text = {
                Column {
                    StateStore.lists.forEach { l ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                Settings.defList = l.id
                                showList = false
                            }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = Settings.defList == l.id, onClick = { Settings.defList = l.id })
                            Spacer(Modifier.width(8.dp))
                            Text(l.name)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showList = false }) { Text("取消") } }
        )
    }
}

// ---------------- list options ----------------

@Composable
private fun ListOptionsPage() {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        SettingsCard {
            SliderRow("字体大小", Settings.fontSize.toFloat(), 12f..22f) { Settings.fontSize = it.toInt() }
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SliderRow("行间距", Settings.rowSpacing.toFloat(), 8f..24f) { Settings.rowSpacing = it.toInt() }
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SwitchRow("显示完整的任务标题", checked = Settings.showFullTitle) { Settings.showFullTitle = it }
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SwitchRow("显示描述", checked = Settings.showDescription) { Settings.showDescription = it }
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SwitchRow("显示完整描述", checked = Settings.showFullDescription) { Settings.showFullDescription = it }
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SwitchRow("显示链接", subtitle = "添加网站，地址，电话号码等链接", checked = Settings.showLinks) {
                Settings.showLinks = it
            }
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SwitchRow("清单单独排序", subtitle = "记住每个清单不同的排序和分组设置", checked = Settings.perListSort) {
                Settings.perListSort = it
            }
        }
        SectionHeader("「纸片」")
        SettingsCard {
            SettingsRow("纸片外观", subtitle = Settings.paperStyle, onClick = {})
        }
    }
}

@Composable
private fun SliderRow(title: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    var v by remember(value) { mutableFloatStateOf(value) }
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row {
            Text(title, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text(v.toInt().toString(), fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(value = v, onValueChange = { v = it }, valueRange = range, onValueChangeFinished = { onChange(v) })
    }
}

// ---------------- editor options ----------------

@Composable
private fun EditorOptionsPage() {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        SettingsCard {
            SettingsRow("自定义编辑屏幕", subtitle = "重新安排或删除字段", onClick = {})
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SwitchRow("显示链接", checked = Settings.editorShowLinks) { Settings.editorShowLinks = it }
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SwitchRow("按返回键时保存任务", checked = Settings.saveOnBack) { Settings.saveOnBack = it }
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SwitchRow("允许多行标题", checked = Settings.multilineTitle) { Settings.multilineTitle = it }
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SwitchRow("在任务编辑中显示注释", checked = Settings.showComments) { Settings.showComments = it }
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SwitchRow(
                "不解锁显示编辑屏",
                subtitle = "允许不解锁设备的情况下使用\"快速设置\"磁贴",
                checked = Settings.editWhenLocked
            ) { Settings.editWhenLocked = it }
        }
    }
}

// ---------------- date & time ----------------

@Composable
private fun DateTimePage() {
    var editTimeKey by remember { mutableStateOf("") }
    Column(Modifier.verticalScroll(rememberScrollState())) {
        SettingsCard {
            SwitchRow("显示完整日期", checked = Settings.showFullDate) { Settings.showFullDate = it }
        }
        SettingsCard {
            QuickTimeRow("上午", "qt1") { editTimeKey = "qt1" }
            HorizontalDivider(Modifier.padding(start = 16.dp))
            QuickTimeRow("下午", "qt2") { editTimeKey = "qt2" }
            HorizontalDivider(Modifier.padding(start = 16.dp))
            QuickTimeRow("傍晚", "qt3") { editTimeKey = "qt3" }
            HorizontalDivider(Modifier.padding(start = 16.dp))
            QuickTimeRow("晚上", "qt4") { editTimeKey = "qt4" }
        }
        SectionHeader("「自动关闭日期时间选择器 ⓘ」")
        SettingsCard {
            SwitchRow("任务清单", subtitle = "从任务清单中选择时自动关闭", checked = Settings.autoCloseList) {
                Settings.autoCloseList = it
            }
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SwitchRow("任务编辑", checked = Settings.autoCloseEdit) { Settings.autoCloseEdit = it }
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SwitchRow("小部件", checked = Settings.autoCloseWidget) { Settings.autoCloseWidget = it }
        }
    }
    if (editTimeKey.isNotEmpty()) {
        val defaults = mapOf("qt1" to "09:00", "qt2" to "13:00", "qt3" to "17:00", "qt4" to "20:00")
        var sel by remember { mutableStateOf(Prefs.str(editTimeKey, defaults[editTimeKey] ?: "09:00")) }
        AlertDialog(
            onDismissRequest = { editTimeKey = "" },
            title = { Text("时间") },
            text = {
                Column {
                    listOf("08:00", "09:00", "12:00", "13:00", "17:00", "18:00", "20:00").forEach { opt ->
                        Row(
                            Modifier.fillMaxWidth().clickable { sel = opt }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = sel == opt, onClick = { sel = opt })
                            Spacer(Modifier.width(8.dp))
                            Text(opt)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    Prefs.putStr(editTimeKey, sel)
                    editTimeKey = ""
                }) { Text("确定", color = BlueColor) }
            },
            dismissButton = { TextButton(onClick = { editTimeKey = "" }) { Text("取消") } }
        )
    }
}

@Composable
private fun QuickTimeRow(label: String, key: String, onClick: () -> Unit) {
    val defaults = mapOf("qt1" to "09:00", "qt2" to "13:00", "qt3" to "17:00", "qt4" to "20:00")
    SettingsRow("$label  " + Prefs.str(key, defaults[key] ?: "09:00"), onClick = onClick)
}

// ---------------- drawer settings ----------------

@Composable
private fun DrawerSettingsPage() {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        SettingsCard {
            SettingsRow("自定义抽屉", subtitle = "通过拖放重新安排菜单项", onClick = {})
        }
        SectionHeader("「过滤器」")
        SettingsCard {
            SwitchRow("启用", checked = Settings.drawerFiltersEnabled) { Settings.drawerFiltersEnabled = it }
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SwitchRow("今天", checked = Settings.drawerFilterToday) { Settings.drawerFilterToday = it }
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SwitchRow("最近修改过的", checked = Settings.drawerFilterRecent) { Settings.drawerFilterRecent = it }
        }
        SectionHeader("「标签」")
        SettingsCard {
            SwitchRow("启用", checked = Settings.drawerTagsEnabled) { Settings.drawerTagsEnabled = it }
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SwitchRow("隐藏不用的标签", checked = Settings.hideUnusedTags) { Settings.hideUnusedTags = it }
        }
        SectionHeader("「地点」")
        SettingsCard {
            SwitchRow("启用", checked = Settings.drawerPlacesEnabled) { Settings.drawerPlacesEnabled = it }
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SwitchRow("隐藏不用的地点", checked = Settings.hideUnusedPlaces) { Settings.hideUnusedPlaces = it }
        }
    }
}

// ---------------- backup ----------------

@Composable
private fun BackupPage() {
    var lastBackup by remember { mutableStateOf("9月29日 00:53") }
    Column(Modifier.verticalScroll(rememberScrollState())) {
        SettingsCard {
            SettingsRow(
                "备份文件夹",
                subtitle = "/storage/emulated/0/Android/data/org.tasks/files/backups\n卸载前请备份，卸载会删除这些文件。",
                onClick = {}
            )
        }
        SettingsCard {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { lastBackup = "9月29日 01:32" },
                    colors = ButtonDefaults.buttonColors(containerColor = BlueColor)
                ) {
                    Text("立即备份")
                }
                Spacer(Modifier.width(12.dp))
                Text("上次备份：$lastBackup", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SwitchRow("自动备份", checked = Settings.autoBackup) { Settings.autoBackup = it }
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SwitchRow("Google Drive", checked = Settings.gdriveBackup) { Settings.gdriveBackup = it }
        }
    }
}

// ---------------- advanced ----------------

@Composable
private fun AdvancedPage(onReset: () -> Unit) {
    var showReset by remember { mutableStateOf(false) }
    var showBadgeList by remember { mutableStateOf(false) }
    Column(Modifier.verticalScroll(rememberScrollState())) {
        SettingsCard {
            SwitchRow(
                "Astrid 手动排序",
                subtitle = "为\"我的任务\"，\"今天\"和标签启用 Astrid 的手动排序模式。在以后的更新中，此排序模式将被\"我的顺序\"代替",
                checked = false
            ) {}
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SettingsRow(
                "附件文件夹",
                subtitle = "/storage/emulated/0/Android/data/org.tasks/files/attachments",
                onClick = {}
            )
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SwitchRow("日历事件时间", subtitle = "在截止时开始日历事件", checked = Settings.calendarEventTime) {
                Settings.calendarEventTime = it
            }
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SettingsRow("为已完成的任务删除日历事件", onClick = {})
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SettingsRow("删除所有的日历事件", onClick = {})
        }
        SectionHeader("「角标」")
        SettingsCard {
            SwitchRow(
                "启用",
                subtitle = "在 Tasks 启动图标上显示任务计数。不是所有的启动器都支持角标。",
                checked = Settings.badgesEnabled
            ) { Settings.badgesEnabled = it }
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SettingsRow("清单", subtitle = "我的任务", onClick = { showBadgeList = true })
        }
        SettingsCard {
            SettingsRow("重置偏好设置", onClick = { showReset = true })
        }
    }
    if (showReset) {
        ConfirmDialog(
            title = "重置偏好设置？",
            text = "所有设置将恢复默认值。",
            onConfirm = {
                showReset = false
                onReset()
            },
            onDismiss = { showReset = false }
        )
    }
    if (showBadgeList) {
        AlertDialog(
            onDismissRequest = { showBadgeList = false },
            title = { Text("清单") },
            text = {
                Column {
                    listOf("我的任务", "今天").forEach { opt ->
                        Row(
                            Modifier.fillMaxWidth().clickable { showBadgeList = false }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = true, onClick = { })
                            Spacer(Modifier.width(8.dp))
                            Text(opt)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showBadgeList = false }) { Text("取消") } }
        )
    }
}

// ---------------- about ----------------

@Composable
private fun AboutPage() {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        TasksLogo(size = 84)
        Spacer(Modifier.height(12.dp))
        Text("Tasks", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("版本 15.10", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        SettingsCard {
            SettingsRow("开源许可", onClick = {})
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SettingsRow("隐私政策", onClick = {})
            HorizontalDivider(Modifier.padding(start = 16.dp))
            SettingsRow("问题反馈", onClick = {})
        }
    }
}
