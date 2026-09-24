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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingRow(
    p: Palette,
    icon: @Composable () -> Unit,
    label: String,
    value: String? = null,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) { icon() }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = p.textPrimary, fontSize = 17.sp)
            if (value != null && value.isNotBlank()) Text(value, color = p.textSecondary, fontSize = 13.sp)
        }
        if (showChevron) ChevronRight(p.textSecondary, 18.dp)
    }
}

@Composable
fun SettingSwitch(p: Palette, icon: @Composable () -> Unit, label: String, sub: String?, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) { icon() }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = p.textPrimary, fontSize = 17.sp)
            if (sub != null) Text(sub, color = p.textSecondary, fontSize = 13.sp)
        }
        Switch(checked = value, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF2E6DB4)))
    }
}

@Composable
fun SettingsScaffold(p: Palette, title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().background(if (p.dark) p.background else Color(0xFFEDEDF2))) {
        TopBar(p, title, onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(6.dp))
            content()
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
fun SettingsScreen(p: Palette, router: Router, page: String) {
    Store.revision
    val s = Store.settings
    val back = { router.pop() }
    when (page) {
        "root" -> SettingsScaffold(p, "设置", back) {
            SettingsCard(p) {
                SettingRow(p, { DonateGlyph(p.textPrimary) }, "捐赠", "考虑用捐赠显示您的支持！", showChevron = true) { }
            }
            Spacer(Modifier.height(10.dp))
            SettingsCard(p) {
                SettingRow(p, { ListGlyphSmall(p.textPrimary) }, "本地清单") { }
                Divider(p)
                SettingRow(p, { PlusGlyph(p.textPrimary) }, "添加账号") { router.push(Nav.Accounts) }
            }
            Spacer(Modifier.height(10.dp))
            SettingsCard(p) {
                SettingRow(p, { PaletteIcon(p.textPrimary) }, "外观") { router.push(Nav.Settings("appearance")) }
                Divider(p)
                SettingRow(p, { BellGlyph(p.textPrimary) }, "通知") { router.push(Nav.Settings("notifications")) }
            }
            Spacer(Modifier.height(10.dp))
            SettingsCard(p) {
                SettingRow(p, { PlusGlyph(p.textPrimary) }, "任务默认值") { router.push(Nav.Settings("defaults")) }
                Divider(p)
                SettingRow(p, { ListGlyphSmall(p.textPrimary) }, "任务清单选项") { router.push(Nav.Settings("listoptions")) }
                Divider(p)
                SettingRow(p, { LinesGlyph(p.textPrimary) }, "编辑屏幕选项") { router.push(Nav.Settings("editoptions")) }
            }
            Spacer(Modifier.height(10.dp))
            SettingsCard(p) {
                SettingRow(p, { ClockIcon(p.textPrimary, 22.dp) }, "日期和时间") { router.push(Nav.Settings("datetime")) }
                Divider(p)
                SettingRow(p, { MenuGlyph(p.textPrimary) }, "导航抽屉") { router.push(Nav.Settings("drawer")) }
                Divider(p)
                SettingRow(p, { DonateGlyph(p.textPrimary) }, "备份", s.lastBackup, showChevron = true) { router.push(Nav.Settings("backup")) }
            }
            Spacer(Modifier.height(10.dp))
            SettingsCard(p) {
                SettingRow(p, { TagIcon(p.textPrimary, 22.dp) }, "插件设置", showChevron = true) { router.push(Nav.Settings("plugins")) }
            }
        }
        "appearance" -> SettingsScaffold(p, "外观", back) {
            var themeDialog by remember { mutableStateOf(false) }
            SettingsCard(p) {
                SettingRow(p, { PaletteIcon(p.textPrimary) }, "主题", s.theme, showChevron = true) { themeDialog = true }
                Divider(p)
                SettingSwitch(p, { PaletteIcon(p.textPrimary) }, "动态", "使用系统颜色", s.dynamicColor) { s.dynamicColor = it; Store.save() }
                Divider(p)
                SettingRow(p, { PaletteIcon(p.textPrimary) }, "颜色", showChevron = true) { }
                Divider(p)
                SettingRow(p, { GridGlyph(p.textPrimary) }, "启动器图标", showChevron = true) { }
                Divider(p)
                SettingSwitch(p, { LinesGlyph(p.textPrimary) }, "Markdown", "在任务描述中渲染 Markdown", s.markdown) { s.markdown = it; Store.save() }
            }
            Spacer(Modifier.height(10.dp))
            SettingsCard(p) {
                SettingSwitch(p, { HomeGlyph(p.textPrimary) }, "启动时", "打开上次查看的清单", s.openLastList) { s.openLastList = it; Store.save() }
            }
            Spacer(Modifier.height(10.dp))
            SettingsCard(p) {
                SettingRow(p, { TagIcon(p.textPrimary, 22.dp) }, "语言", "中文(中国)", showChevron = true) { }
                Divider(p)
                SettingRow(p, { DonateGlyph(p.textPrimary) }, "贡献翻译", showChevron = true) { }
            }
            if (themeDialog) {
                OptionDialog(p, "主题", listOf("亮色", "黑色", "暗色", "壁纸", "日/夜", "系统默认"), {
                    s.theme = it; Store.save(); themeDialog = false
                }, { themeDialog = false })
            }
        }
        "notifications" -> SettingsScaffold(p, "通知", back) {
            SettingsCard(p) {
                SettingRow(p, { HelpGlyph(p.textPrimary) }, "疑难解答", "如果您在通知方面遇到问题，请点按此处", showChevron = true) { }
                Divider(p)
                SettingRow(p, { HelpGlyph(p.textPrimary) }, "禁用电池优化", "电池优化可能会延迟通知", showChevron = true) { }
            }
            Spacer(Modifier.height(10.dp))
            SettingsCard(p) {
                SettingRow(p, { BellGlyph(p.textPrimary) }, "播放完成声音", "默认") { }
                Divider(p)
                SettingSwitch(p, { BellGlyph(p.textPrimary) }, "合并通知", "将多个通知合并为一个通知", s.mergeNotifications) { s.mergeNotifications = it; Store.save() }
                Divider(p)
                SettingSwitch(p, { BellGlyph(p.textPrimary) }, "语音提醒", "Tasks 会在任务提醒时读出任务名", s.voiceReminders) { s.voiceReminders = it; Store.save() }
            }
            Spacer(Modifier.height(10.dp))
            SettingsCard(p) {
                SettingRow(p, { BellGlyph(p.textPrimary) }, "更多设置", "铃声、振动及更多", showChevron = true) { }
            }
            Spacer(Modifier.height(14.dp))
            Text("全天任务", color = p.textSecondary, fontSize = 15.sp, modifier = Modifier.padding(start = 20.dp, bottom = 6.dp))
            SettingsCard(p) {
                SettingSwitch(p, { ClockIcon(p.textPrimary, 22.dp) }, "添加默认提醒", "为全天任务添加默认提醒", s.allDayDefaultReminder) { s.allDayDefaultReminder = it; Store.save() }
                Divider(p)
                SettingRow(p, { ClockIcon(p.textPrimary, 22.dp) }, "提醒时间", s.reminderTime) { }
            }
        }
        "defaults" -> SettingsScaffold(p, "任务默认值", back) {
            var prioDialog by remember { mutableStateOf(false) }
            var listDialog by remember { mutableStateOf(false) }
            SettingsCard(p) {
                SettingRow(p, { FlagIcon(p.textPrimary, 22.dp) }, "默认优先级", priorityName(s.defaultPriority), showChevron = true) { prioDialog = true }
                Divider(p)
                SettingRow(p, { ListGlyphSmall(p.textPrimary) }, "默认清单", Store.listName(s.defaultListId), showChevron = true) { listDialog = true }
                Divider(p)
                SettingRow(p, { TagIcon(p.textPrimary, 22.dp) }, "默认标签", showChevron = true) { }
                Divider(p)
                SettingRow(p, { ClockIcon(p.textPrimary, 22.dp) }, "默认截止日期", showChevron = true) { }
            }
            Spacer(Modifier.height(10.dp))
            SettingsCard(p) {
                SettingRow(p, { ClockIcon(p.textPrimary, 22.dp) }, "默认开始日期", showChevron = true) { }
                Divider(p)
                SettingRow(p, { ClockIcon(p.textPrimary, 22.dp) }, "默认提醒", showChevron = true) { }
            }
            if (prioDialog) {
                OptionDialog(p, "默认优先级", listOf("无", "低", "中", "高"), {
                    s.defaultPriority = when (it) { "高" -> PRIO_HIGH; "中" -> PRIO_MED; "低" -> PRIO_LOW; else -> PRIO_NONE }
                    Store.save(); prioDialog = false
                }, { prioDialog = false })
            }
            if (listDialog) {
                OptionDialog(p, "默认清单", Store.lists.map { it.name }, {
                    s.defaultListId = Store.lists.firstOrNull { l -> l.name == it }?.id ?: s.defaultListId
                    Store.save(); listDialog = false
                }, { listDialog = false })
            }
        }
        "listoptions" -> SettingsScaffold(p, "任务清单选项", back) {
            SettingsCard(p) {
                SettingSwitch(p, { LinesGlyph(p.textPrimary) }, "显示任务描述", null, true) { }
                Divider(p)
                SettingSwitch(p, { TagIcon(p.textPrimary, 22.dp) }, "显示标签", null, true) { }
                Divider(p)
                SettingSwitch(p, { ListGlyphSmall(p.textPrimary) }, "显示清单名称", null, true) { }
            }
        }
        "editoptions" -> SettingsScaffold(p, "编辑屏幕选项", back) {
            SettingsCard(p) {
                SettingSwitch(p, { LinesGlyph(p.textPrimary) }, "自定义编辑屏幕", null, s.customEditScreen) { s.customEditScreen = it; Store.save() }
            }
            Spacer(Modifier.height(10.dp))
            SettingsCard(p) {
                SettingSwitch(p, { DonateGlyph(p.textPrimary) }, "显示链接", null, s.editShowLinks) { s.editShowLinks = it; Store.save() }
                Divider(p)
                SettingSwitch(p, { SaveIcon(p.textPrimary) }, "按返回键时保存任务", null, s.saveOnBack) { s.saveOnBack = it; Store.save() }
                Divider(p)
                SettingSwitch(p, { LinesGlyph(p.textPrimary) }, "允许多行标题", null, s.multiLineTitle) { s.multiLineTitle = it; Store.save() }
                Divider(p)
                SettingSwitch(p, { LinesGlyph(p.textPrimary) }, "在任务编辑中显示注释", null, s.showNotesInEdit) { s.showNotesInEdit = it; Store.save() }
                Divider(p)
                SettingSwitch(p, { BellGlyph(p.textPrimary) }, "不解锁显示编辑屏", null, s.unlockToEdit) { s.unlockToEdit = it; Store.save() }
            }
        }
        "datetime" -> SettingsScaffold(p, "日期和时间", back) {
            SettingsCard(p) {
                SettingSwitch(p, { ClockIcon(p.textPrimary, 22.dp) }, "显示完整日期", "始终显示日期而不仅是今天/明天", s.showFullDate) { s.showFullDate = it; Store.save() }
            }
            Spacer(Modifier.height(10.dp))
            SettingsCard(p) {
                SettingRow(p, { ClockIcon(p.textPrimary, 22.dp) }, "上午", "09:00") { }
                Divider(p)
                SettingRow(p, { ClockIcon(p.textPrimary, 22.dp) }, "下午", "13:00") { }
                Divider(p)
                SettingRow(p, { ClockIcon(p.textPrimary, 22.dp) }, "傍晚", "17:00") { }
                Divider(p)
                SettingRow(p, { ClockIcon(p.textPrimary, 22.dp) }, "晚上", "20:00") { }
            }
            Spacer(Modifier.height(10.dp))
            SettingsCard(p) {
                SettingSwitch(p, { ClockIcon(p.textPrimary, 22.dp) }, "自动关闭日期时间选择器", null, s.autoClosePicker) { s.autoClosePicker = it; Store.save() }
            }
        }
        "drawer" -> SettingsScaffold(p, "导航抽屉", back) {
            SettingsCard(p) {
                SettingSwitch(p, { FilterIcon(p.textPrimary, 22.dp) }, "显示过滤器", null, s.drawerFilters) { s.drawerFilters = it; Store.save() }
                Divider(p)
                SettingSwitch(p, { TagIcon(p.textPrimary, 22.dp) }, "显示标签", null, s.drawerTags) { s.drawerTags = it; Store.save() }
                Divider(p)
                SettingSwitch(p, { PlaceGlyph(p.textPrimary, 22.dp) }, "显示地点", null, s.drawerPlaces) { s.drawerPlaces = it; Store.save() }
                Divider(p)
                SettingSwitch(p, { ListGlyphSmall(p.textPrimary) }, "显示本地清单", null, s.drawerLists) { s.drawerLists = it; Store.save() }
            }
        }
        "backup" -> SettingsScaffold(p, "备份", back) {
            SettingsCard(p) {
                SettingRow(p, { DonateGlyph(p.textPrimary) }, "备份文件夹", "未设置备份文件夹，自动备份已停用", showChevron = true) { }
            }
            Spacer(Modifier.height(10.dp))
            SettingsCard(p) {
                SettingRow(p, { SaveIcon(p.textPrimary) }, "立即备份", s.lastBackup) {
                    s.lastBackup = "上次备份 ${timeText(System.currentTimeMillis())}"; Store.save()
                }
                Divider(p)
                SettingRow(p, { SaveIcon(p.textPrimary) }, "导入备份", showChevron = true) { }
            }
            Spacer(Modifier.height(10.dp))
            SettingsCard(p) {
                SettingSwitch(p, { SaveIcon(p.textPrimary) }, "自动备份", "每天自动备份数据", s.autoBackup) { s.autoBackup = it; Store.save() }
                Divider(p)
                SettingSwitch(p, { SaveIcon(p.textPrimary) }, "备份到 Google Drive", "启用", s.backupDrive) { s.backupDrive = it; Store.save() }
                Divider(p)
                SettingSwitch(p, { SaveIcon(p.textPrimary) }, "Android 备份服务", null, s.androidBackup) { s.androidBackup = it; Store.save() }
            }
        }
        "plugins" -> SettingsScaffold(p, "插件设置", back) {
            SettingsCard(p) {
                Column(Modifier.padding(16.dp)) {
                    Text("没有可用的插件", color = p.textPrimary, fontSize = 17.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("安装支持 Tasks 插件的应用后，可在此启用。", color = p.textSecondary, fontSize = 14.sp)
                }
            }
        }
    }
}

// ------------------------------------------------------------------ search

@Composable
fun SearchScreen(p: Palette, router: Router, from: String) {
    Store.revision
    var query by remember { mutableStateOf("") }
    val results = if (query.isBlank()) emptyList() else Store.tasks.filter {
        it.title.contains(query, true) || it.notes.contains(query, true)
    }
    Column(Modifier.fillMaxSize().background(p.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(44.dp).clickable { router.pop() }, contentAlignment = Alignment.Center) { ChevronLeftGlyph(p.textPrimary, 22.dp) }
            Surface(color = p.surface, shape = RoundedCornerShape(4.dp), modifier = Modifier.weight(1f)) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    SearchGlyph(p.textSecondary, 22.dp)
                    Spacer(Modifier.width(12.dp))
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) Text("搜索", color = p.textSecondary, fontSize = 17.sp)
                        BasicTextField(
                            value = query, onValueChange = { query = it },
                            textStyle = TextStyle(color = p.textPrimary, fontSize = 17.sp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (query.isNotEmpty()) {
                        Box(Modifier.size(24.dp).clickable { query = "" }, contentAlignment = Alignment.Center) { CloseGlyph(p.textSecondary, 18.dp) }
                    }
                }
            }
            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { SettingsGlyph(p.textPrimary) }
        }
        if (query.isNotBlank() && results.isEmpty()) {
            EmptyState(p)
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                results.forEach { t ->
                    TaskRow(
                        task = t, p = p, viewKey = "search", selected = false, selectionMode = false,
                        onOpen = { router.push(Nav.Edit(t.id, null)) },
                        onToggle = { if (t.completed) Store.uncompleteTask(t) else Store.completeTask(t) },
                        onLongPress = { },
                        onToggleSubtask = { s -> if (s.done) Store.uncompleteSubtask(t, s) else Store.completeSubtask(t, s) },
                        onSelect = { }
                    )
                }
            }
        }
    }
}
