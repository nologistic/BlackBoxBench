package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScaffold(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(AppColors.DrawerBg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) }
            Text(title, fontSize = 22.sp, modifier = Modifier.padding(start = 8.dp))
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), content = content)
    }
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        content = content
    )
}

@Composable
fun SettingsRow(
    label: String,
    subtitle: String = "",
    icon: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Row(
        Modifier.fillMaxWidth()
            .then(if (onClick != null && enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) { icon(); Spacer(Modifier.width(16.dp)) }
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 16.sp, color = if (enabled) Color.Unspecified else AppColors.Subtle)
            if (subtitle.isNotEmpty()) Text(subtitle, fontSize = 13.sp, color = AppColors.Subtle)
        }
        trailing?.invoke()
    }
}

@Composable
fun GroupLabel(text: String) {
    Text(text, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp, start = 4.dp))
}

// ===================== 设置主页 =====================
@Composable
fun SettingsMainScreen(onBack: () -> Unit, open: (String) -> Unit) {
    SettingsScaffold("设置", onBack) {
        SettingsCard {
            SettingsRow("捐赠", "考虑用捐赠显示您的支持！", icon = { Icon(Icons.Filled.FavoriteBorder, null, tint = AppColors.Subtle) },
                trailing = { Icon(Icons.Filled.KeyboardArrowRight, null, tint = AppColors.Subtle) }) {}
        }
        SettingsCard {
            SettingsRow("本地清单", icon = { CloudOffIcon(AppColors.Subtle) }) { open("local_account") }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("添加账号", icon = { Icon(Icons.Filled.Add, null, tint = AppColors.Subtle) }) { open("add_account") }
        }
        SettingsCard {
            SettingsRow("外观", icon = { Icon(Icons.Filled.Star, null, tint = AppColors.Subtle) }) { open("appearance") }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("通知", icon = { AlarmIcon(AppColors.Subtle) }) { open("notifications") }
        }
        SettingsCard {
            SettingsRow("任务默认值", icon = { Icon(Icons.Filled.Add, null, tint = AppColors.Subtle) }) { open("defaults") }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("任务清单选项", icon = { Icon(Icons.Filled.List, null, tint = AppColors.Subtle) }) { open("list_options") }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("编辑屏幕选项", icon = { Icon(Icons.Filled.Edit, null, tint = AppColors.Subtle) }) { open("edit_options") }
        }
        SettingsCard {
            SettingsRow("日期和时间", icon = { HistoryIcon(AppColors.Subtle) }) { open("datetime") }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("导航抽屉", icon = { Icon(Icons.Filled.Menu, null, tint = AppColors.Subtle) }) { open("drawer") }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("备份", icon = { Icon(Icons.Filled.DateRange, null, tint = AppColors.Subtle) },
                trailing = { Icon(Icons.Filled.Warning, null, tint = Color(0xFFFFA000)) }) { open("backup") }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("插件设置", icon = { GridIcon(AppColors.Subtle) }) { open("plugins") }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("高级", icon = { Icon(Icons.Filled.Build, null, tint = AppColors.Subtle) }) { open("advanced") }
        }
        SettingsCard {
            SettingsRow("关于", icon = { Icon(Icons.Filled.Info, null, tint = AppColors.Subtle) }) { open("about") }
        }
    }
}

// ===================== 外观 =====================
@Composable
fun AppearanceScreen(onBack: () -> Unit) {
    var showTheme by remember { mutableStateOf(false) }
    var showColor by remember { mutableStateOf(false) }
    val themeLabel = mapOf(
        "light" to "亮色", "black" to "黑色", "dark" to "暗色",
        "wallpaper" to "壁纸", "daynight" to "日/夜", "system" to "系统默认"
    )
    SettingsScaffold("外观", onBack) {
        SettingsCard {
            SettingsRow("主题", themeLabel[Store.setting("theme", "system")] ?: "系统默认") { showTheme = true }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("动态", trailing = {
                Switch(Store.boolSetting("dynamic_color", false), { Store.setBool("dynamic_color", it) })
            }) { Store.setBool("dynamic_color", !Store.boolSetting("dynamic_color", false)) }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("颜色", icon = {
                Box(Modifier.size(22.dp).clip(androidx.compose.foundation.shape.CircleShape)
                    .background(Store.setting("accent", "").toLongOrNull()?.let { Color(it) } ?: AppColors.Blue))
            }) { showColor = true }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("启动器图标", icon = {
                Box(Modifier.size(22.dp).clip(androidx.compose.foundation.shape.CircleShape)
                    .background(Store.setting("accent", "").toLongOrNull()?.let { Color(it) } ?: AppColors.Blue))
            }) {}
        }
        SettingsCard {
            SettingsRow("Markdown", "在任务标题和描述中启用 Markdown", trailing = {
                Switch(Store.boolSetting("markdown", false), { Store.setBool("markdown", it) })
            }) { Store.setBool("markdown", !Store.boolSetting("markdown", false)) }
        }
        GroupLabel("启动时")
        SettingsCard {
            SettingsRow("打开上次查看的清单", trailing = {
                Switch(Store.boolSetting("open_last_list", true), { Store.setBool("open_last_list", it) })
            }) { Store.setBool("open_last_list", !Store.boolSetting("open_last_list", true)) }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("打开清单", "我的任务", enabled = false)
        }
        GroupLabel("本地化")
        SettingsCard {
            SettingsRow("语言", "中文 (中国)") {}
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("贡献翻译", icon = { Icon(Icons.Filled.Edit, null, tint = AppColors.Subtle) }) {}
        }
    }
    if (showTheme) AlertDialog(onDismissRequest = { showTheme = false }, confirmButton = {}, dismissButton = {
        TextButton(onClick = { showTheme = false }) { Text("取消") }
    }, title = { Text("主题") }, text = {
        Column {
            listOf("light", "black", "dark", "wallpaper", "daynight", "system").forEach { t ->
                Row(Modifier.fillMaxWidth().clickable { Store.setSetting("theme", t); showTheme = false }.padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = Store.setting("theme", "system") == t, onClick = { Store.setSetting("theme", t); showTheme = false })
                    Spacer(Modifier.width(8.dp)); Text(themeLabel[t]!!, fontSize = 16.sp)
                }
            }
        }
    })
    if (showColor) ColorPickerDialog({ showColor = false }) { c -> Store.setSetting("accent", c.toString()) }
}

// ===================== 通知 =====================
@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    SettingsScaffold("通知", onBack) {
        SettingsCard {
            SettingsRow("疑难解答", "如果您在通知方面遇到问题，请点按此处", icon = { Icon(Icons.Filled.Edit, null, tint = AppColors.Subtle) }) {}
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("禁用电池优化", "电池优化可能会延迟通知", icon = { Icon(Icons.Filled.Edit, null, tint = AppColors.Subtle) }) {}
        }
        SettingsCard {
            SettingsRow("播放完成声音", "默认") {}
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("合并通知", "将多个通知合并为一个通知", trailing = {
                Switch(Store.boolSetting("bundle_notif", true), { Store.setBool("bundle_notif", it) })
            }) { Store.setBool("bundle_notif", !Store.boolSetting("bundle_notif", true)) }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("语音提醒", "Tasks 会在任务提醒时读出任务名", trailing = {
                Switch(Store.boolSetting("voice_notif", false), { Store.setBool("voice_notif", it) })
            }) { Store.setBool("voice_notif", !Store.boolSetting("voice_notif", false)) }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("更多设置", "铃声、振动及更多", icon = { Icon(Icons.Filled.Edit, null, tint = AppColors.Subtle) }) {}
        }
        GroupLabel("全天任务")
        SettingsCard {
            SettingsRow("添加默认提醒", "为全天任务添加默认提醒", trailing = {
                Switch(Store.boolSetting("allday_reminder", true), { Store.setBool("allday_reminder", it) })
            }) { Store.setBool("allday_reminder", !Store.boolSetting("allday_reminder", true)) }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("提醒时间", Store.setting("allday_reminder_time", "18:00")) {}
        }
    }
}

// ===================== 任务默认值 =====================
@Composable
fun TaskDefaultsScreen(onBack: () -> Unit) {
    var showListPicker by remember { mutableStateOf(false) }
    var showTagPicker by remember { mutableStateOf(false) }
    var showPriority by remember { mutableStateOf(false) }
    var showRepeat by remember { mutableStateOf(false) }
    val prioLabel = mapOf(0 to "无", 1 to "低", 2 to "中", 3 to "高")
    val defaultPrio = Store.setting("default_priority", "1").toIntOrNull() ?: 1
    SettingsScaffold("任务默认值", onBack) {
        SettingsCard {
            SettingsRow("新任务显示在顶部", icon = { Icon(Icons.Filled.KeyboardArrowUp, null, tint = AppColors.Subtle) }, trailing = {
                Switch(Store.boolSetting("new_task_top", true), { Store.setBool("new_task_top", it) })
            }) { Store.setBool("new_task_top", !Store.boolSetting("new_task_top", true)) }
        }
        SettingsCard {
            SettingsRow("默认清单", Store.listName(Store.setting("default_list", "lst_default")), icon = { Icon(Icons.Filled.List, null, tint = AppColors.Subtle) }) { showListPicker = true }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("默认标签", Store.tagById(Store.setting("default_tag", ""))?.name ?: "无", icon = { TagIcon(AppColors.Subtle) }) { showTagPicker = true }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("默认优先级", prioLabel[defaultPrio] ?: "低", icon = { FlagIcon(AppColors.Subtle) }) { showPriority = true }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("默认的开始日期", "无开始日期", icon = { Icon(Icons.Filled.DateRange, null, tint = AppColors.Subtle) }) {}
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("默认截止日期", "无截止日期", icon = { HistoryIcon(AppColors.Subtle) }) {}
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("默认日历", "不添加到日历", icon = { Icon(Icons.Filled.DateRange, null, tint = AppColors.Subtle) }) {}
        }
        GroupLabel("提醒")
        SettingsCard {
            SettingsRow("默认提醒", "开始后\n到期时\n到期后 1 天, 每1 天重复, 发生 6 次", icon = { AlarmIcon(AppColors.Subtle) }) {}
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("默认铃声 / 振动类型", "响铃一次") {}
        }
        GroupLabel("重复")
        SettingsCard {
            SettingsRow("默认重复周期", Dates.repeatLabel(Store.setting("default_repeat", "")), icon = { Icon(Icons.Filled.Refresh, null, tint = AppColors.Subtle) }) { showRepeat = true }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("重复始于", "截止日期") {}
        }
        GroupLabel("位置")
        SettingsCard {
            SettingsRow("默认位置", "无", icon = { Icon(Icons.Filled.Place, null, tint = AppColors.Subtle) }) {}
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("默认位置提醒", "无提醒") {}
        }
    }
    if (showListPicker) ListPickerDialog({ showListPicker = false }, { Store.setSetting("default_list", it); showListPicker = false })
    if (showTagPicker) TagSelectDialog({ showTagPicker = false }, { Store.setSetting("default_tag", it); showTagPicker = false })
    if (showPriority) AlertDialog(onDismissRequest = { showPriority = false }, confirmButton = {}, title = { Text("默认优先级") }, text = {
        Column {
            prioLabel.toSortedMap().forEach { (l, label) ->
                Row(Modifier.fillMaxWidth().clickable { Store.setSetting("default_priority", l.toString()); showPriority = false }.padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = defaultPrio == l, onClick = { Store.setSetting("default_priority", l.toString()); showPriority = false })
                    Spacer(Modifier.width(8.dp)); Text(label, fontSize = 16.sp)
                }
            }
        }
    })
    if (showRepeat) RepeatDialog(Store.setting("default_repeat", ""), { showRepeat = false }, { Store.setSetting("default_repeat", it); showRepeat = false })
}

// ===================== 任务清单选项 =====================
@Composable
fun TaskListOptionsScreen(onBack: () -> Unit) {
    SettingsScaffold("任务清单选项", onBack) {
        SettingsCard {
            Column(Modifier.padding(16.dp)) {
                Row {
                    Text("字体大小", Modifier.weight(1f), fontSize = 16.sp)
                    Text(Store.setting("font_size", "16"), color = AppColors.Subtle)
                }
                Slider(
                    value = Store.setting("font_size", "16").toFloatOrNull() ?: 16f,
                    onValueChange = { Store.setSetting("font_size", it.toInt().toString()) },
                    valueRange = 10f..28f
                )
            }
            HorizontalDivider()
            Column(Modifier.padding(16.dp)) {
                Row {
                    Text("行间距", Modifier.weight(1f), fontSize = 16.sp)
                    Text(Store.setting("row_spacing", "16"), color = AppColors.Subtle)
                }
                Slider(
                    value = Store.setting("row_spacing", "16").toFloatOrNull() ?: 16f,
                    onValueChange = { Store.setSetting("row_spacing", it.toInt().toString()) },
                    valueRange = 8f..32f
                )
            }
        }
        SettingsCard {
            SettingsRow("显示完整的任务标题", trailing = {
                Switch(Store.boolSetting("show_full_title", false), { Store.setBool("show_full_title", it) })
            }) { Store.setBool("show_full_title", !Store.boolSetting("show_full_title", false)) }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("显示描述", trailing = {
                Switch(Store.boolSetting("show_desc", true), { Store.setBool("show_desc", it) })
            }) { Store.setBool("show_desc", !Store.boolSetting("show_desc", true)) }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("显示完整描述", trailing = {
                Switch(Store.boolSetting("show_full_desc", false), { Store.setBool("show_full_desc", it) })
            }) { Store.setBool("show_full_desc", !Store.boolSetting("show_full_desc", false)) }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("显示链接", "添加网站，地址，电话号码等链接", trailing = {
                Switch(Store.boolSetting("show_links", false), { Store.setBool("show_links", it) })
            }) { Store.setBool("show_links", !Store.boolSetting("show_links", false)) }
        }
        SettingsCard {
            SettingsRow("清单单独排序", "记住每个清单不同的排序和分组设置", trailing = {
                Switch(Store.boolSetting("per_list_sort", false), { Store.setBool("per_list_sort", it) })
            }) { Store.setBool("per_list_sort", !Store.boolSetting("per_list_sort", false)) }
        }
        GroupLabel("纸片")
        SettingsCard {
            SettingsRow("纸片外观", "文本和图标") {}
        }
    }
}

// ===================== 编辑屏幕选项 =====================
@Composable
fun EditScreenOptionsScreen(onBack: () -> Unit) {
    SettingsScaffold("编辑屏幕选项", onBack) {
        SettingsCard {
            SettingsRow("自定义编辑屏幕", "重新安排或删除字段", trailing = {
                Icon(Icons.Filled.KeyboardArrowRight, null, tint = AppColors.Subtle)
            }) {}
        }
        SettingsCard {
            SettingsRow("显示链接", "添加网站，地址，电话号码等链接", trailing = {
                Switch(Store.boolSetting("edit_show_links", false), { Store.setBool("edit_show_links", it) })
            }) { Store.setBool("edit_show_links", !Store.boolSetting("edit_show_links", false)) }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("按返回键时保存任务", trailing = {
                Switch(Store.boolSetting("save_on_back", false), { Store.setBool("save_on_back", it) })
            }) { Store.setBool("save_on_back", !Store.boolSetting("save_on_back", false)) }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("允许多行标题", "按完成保存任务", trailing = {
                Switch(Store.boolSetting("multiline_title", false), { Store.setBool("multiline_title", it) })
            }) { Store.setBool("multiline_title", !Store.boolSetting("multiline_title", false)) }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("在任务编辑中显示注释", trailing = {
                Switch(Store.boolSetting("show_comments", false), { Store.setBool("show_comments", it) })
            }) { Store.setBool("show_comments", !Store.boolSetting("show_comments", false)) }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("不解锁显示编辑屏", "允许不解锁设备的情况下使用“快速设置”磁贴", trailing = {
                Switch(Store.boolSetting("edit_on_lockscreen", false), { Store.setBool("edit_on_lockscreen", it) })
            }) { Store.setBool("edit_on_lockscreen", !Store.boolSetting("edit_on_lockscreen", false)) }
        }
    }
}

// ===================== 日期和时间 =====================
@Composable
fun DateTimeSettingsScreen(onBack: () -> Unit) {
    SettingsScaffold("日期和时间", onBack) {
        SettingsCard {
            SettingsRow("显示完整日期", trailing = {
                Switch(Store.boolSetting("show_full_date", false), { Store.setBool("show_full_date", it) })
            }) { Store.setBool("show_full_date", !Store.boolSetting("show_full_date", false)) }
        }
        SettingsCard {
            SettingsRow("上午", "09:00") {}
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("下午", "13:00") {}
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("傍晚", "17:00") {}
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("晚上", "20:00") {}
        }
        GroupLabel("自动关闭日期时间选择器")
        SettingsCard {
            SettingsRow("任务清单", "从任务清单中选择时自动关闭", trailing = {
                Switch(Store.boolSetting("autoclose_picker_list", false), { Store.setBool("autoclose_picker_list", it) })
            }) { Store.setBool("autoclose_picker_list", !Store.boolSetting("autoclose_picker_list", false)) }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("任务编辑", "从任务编辑中选择时自动关闭", trailing = {
                Switch(Store.boolSetting("autoclose_picker_edit", false), { Store.setBool("autoclose_picker_edit", it) })
            }) { Store.setBool("autoclose_picker_edit", !Store.boolSetting("autoclose_picker_edit", false)) }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("小部件", "从小部件中选择时自动关闭", trailing = {
                Switch(Store.boolSetting("autoclose_picker_widget", false), { Store.setBool("autoclose_picker_widget", it) })
            }) { Store.setBool("autoclose_picker_widget", !Store.boolSetting("autoclose_picker_widget", false)) }
        }
    }
}

// ===================== 导航抽屉 =====================
@Composable
fun DrawerSettingsScreen(onBack: () -> Unit) {
    SettingsScaffold("导航抽屉", onBack) {
        SettingsCard {
            SettingsRow("自定义抽屉", "通过拖放重新安排菜单项", trailing = {
                Icon(Icons.Filled.KeyboardArrowRight, null, tint = AppColors.Subtle)
            }) {}
        }
        GroupLabel("过滤器")
        SettingsCard {
            SettingsRow("启用", trailing = {
                Switch(Store.boolSetting("nav_filters_enabled", true), { Store.setBool("nav_filters_enabled", it) })
            }) { Store.setBool("nav_filters_enabled", !Store.boolSetting("nav_filters_enabled", true)) }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("今天", trailing = {
                Switch(Store.boolSetting("nav_filter_today", true), { Store.setBool("nav_filter_today", it) })
            }) { Store.setBool("nav_filter_today", !Store.boolSetting("nav_filter_today", true)) }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("最近修改过的", trailing = {
                Switch(Store.boolSetting("nav_filter_recent", true), { Store.setBool("nav_filter_recent", it) })
            }) { Store.setBool("nav_filter_recent", !Store.boolSetting("nav_filter_recent", true)) }
        }
        GroupLabel("标签")
        SettingsCard {
            SettingsRow("启用", trailing = {
                Switch(Store.boolSetting("nav_tags_enabled", true), { Store.setBool("nav_tags_enabled", it) })
            }) { Store.setBool("nav_tags_enabled", !Store.boolSetting("nav_tags_enabled", true)) }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("隐藏不用的标签", trailing = {
                Switch(Store.boolSetting("nav_hide_unused_tags", false), { Store.setBool("nav_hide_unused_tags", it) })
            }) { Store.setBool("nav_hide_unused_tags", !Store.boolSetting("nav_hide_unused_tags", false)) }
        }
        GroupLabel("地点")
        SettingsCard {
            SettingsRow("启用", trailing = {
                Switch(Store.boolSetting("nav_places_enabled", true), { Store.setBool("nav_places_enabled", it) })
            }) { Store.setBool("nav_places_enabled", !Store.boolSetting("nav_places_enabled", true)) }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("隐藏不用的地点", trailing = {
                Switch(Store.boolSetting("nav_hide_unused_places", false), { Store.setBool("nav_hide_unused_places", it) })
            }) { Store.setBool("nav_hide_unused_places", !Store.boolSetting("nav_hide_unused_places", false)) }
        }
    }
}

// ===================== 备份 =====================
@Composable
fun BackupScreen(onBack: () -> Unit) {
    var backupDone by remember { mutableStateOf(false) }
    SettingsScaffold("备份", onBack) {
        SettingsCard {
            SettingsRow("文档", icon = { Icon(Icons.Filled.Edit, null, tint = AppColors.Subtle) }) {}
        }
        SettingsCard {
            Column(Modifier.padding(16.dp)) {
                Text("备份文件夹", fontSize = 16.sp)
                Text("/storage/emulated/0/Android/data/org.tasks/files/backups", fontSize = 13.sp, color = AppColors.Subtle)
                Spacer(Modifier.height(12.dp))
                Row {
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Filled.Warning, null, tint = AppColors.Red)
                }
                Text("警告：如果 Tasks 被卸载，位于 /storage/emulated/0/Android/data/org.tasks/files 的文件将被删除！请选择一个自定义位置，以防止 Android 删除您的文件。", fontSize = 14.sp)
            }
        }
        SettingsCard {
            SettingsRow("立即备份", "上次备份：${Store.setting("last_backup", "从未")}") {
                Store.setSetting("last_backup", "今天 ${java.time.LocalTime.now().withSecond(0).withNano(0)}")
                backupDone = true
            }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("导入备份") {}
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("自动备份", trailing = {
                Switch(Store.boolSetting("auto_backup", true), { Store.setBool("auto_backup", it) })
            }) { Store.setBool("auto_backup", !Store.boolSetting("auto_backup", true)) }
        }
        GroupLabel("备份到 Google Drive")
        SettingsCard {
            SettingsRow("启用", "上次备份：从未", icon = { Icon(Icons.Filled.Warning, null, tint = Color(0xFFFF6D00)) }, trailing = {
                Switch(Store.boolSetting("gdrive_backup", false), { Store.setBool("gdrive_backup", it) })
            }) { Store.setBool("gdrive_backup", !Store.boolSetting("gdrive_backup", false)) }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("账号", "无", enabled = false)
        }
        GroupLabel("Android 备份服务")
        SettingsCard {
            SettingsRow("Android 备份", "备份到 Google 云端硬盘", enabled = false)
        }
        if (backupDone) {
            Text("已创建备份", color = AppColors.Subtle, modifier = Modifier.padding(8.dp))
        }
    }
}

// ===================== 本地清单账号 =====================
@Composable
fun LocalAccountScreen(onBack: () -> Unit) {
    var name by remember { mutableStateOf("本地清单") }
    var showDelete by remember { mutableStateOf(false) }
    SettingsScaffold("本地清单", onBack) {
        SettingsCard {
            Column(Modifier.padding(16.dp)) {
                Text("显示名称", fontSize = 13.sp, color = AppColors.Subtle)
                TextField(
                    value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                )
            }
        }
        SettingsCard {
            SettingsRow("保存", icon = { SaveIcon(AppColors.Subtle) }) {}
        }
        SettingsCard {
            SettingsRow("迁移到 Tasks.org Cloud", icon = { CloudOffIcon(AppColors.Subtle) }) {}
        }
        SettingsCard {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFFFBE9E7))
                    .clickable { showDelete = true }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Delete, null, tint = AppColors.Red)
                Spacer(Modifier.width(16.dp))
                Text("删除", color = AppColors.Red)
            }
        }
    }
    if (showDelete) ConfirmDialog("确认删除？", { showDelete = false }, { showDelete = false })
}

// ===================== 高级 =====================
@Composable
fun AdvancedScreen(onBack: () -> Unit) {
    SettingsScaffold("高级", onBack) {
        SettingsCard {
            SettingsRow("Astrid 手动排序", "为“我的任务”，“今天”和标签启用 Astrid 的手动排序模式。在以后的更新中，此排序模式将被“我的顺序”代替", trailing = {
                Switch(Store.boolSetting("astrid_sort", false), { Store.setBool("astrid_sort", it) })
            }) { Store.setBool("astrid_sort", !Store.boolSetting("astrid_sort", false)) }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("附件文件夹", "/storage/emulated/0/Android/data/org.tasks/files/attachments", icon = { ClipIcon(AppColors.Subtle) }) {}
        }
        SettingsCard {
            SettingsRow("日历事件时间", "在截止时开始日历事件", icon = { Icon(Icons.Filled.DateRange, null, tint = AppColors.Subtle) }, trailing = {
                Switch(Store.boolSetting("calendar_event_time", true), { Store.setBool("calendar_event_time", it) })
            }) { Store.setBool("calendar_event_time", !Store.boolSetting("calendar_event_time", true)) }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("为已完成的任务删除日历事件") {}
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("删除所有的日历事件") {}
        }
        GroupLabel("角标")
        SettingsCard {
            SettingsRow("启用", "在 Tasks 启动图标上显示任务计数。不是所有的启动器都支持角标。", trailing = {
                Switch(Store.boolSetting("badge_enabled", false), { Store.setBool("badge_enabled", it) })
            }) { Store.setBool("badge_enabled", !Store.boolSetting("badge_enabled", false)) }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("清单", "我的任务", enabled = false)
        }
        SettingsCard {
            SettingsRow("重置偏好设置") {}
        }
    }
}

// ===================== 插件设置 =====================
@Composable
fun PluginsScreen(onBack: () -> Unit) {
    SettingsScaffold("插件设置", onBack) {
        SettingsCard {
            SettingsRow("添加快捷方式到主屏幕", icon = { HomeIcon(AppColors.Subtle) }) {}
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("添加小部件到主屏幕", icon = { GridIcon(AppColors.Subtle) }) {}
        }
    }
}

// ===================== 关于 =====================
@Composable
fun AboutScreen(onBack: () -> Unit) {
    SettingsScaffold("关于", onBack) {
        SettingsCard {
            SettingsRow("更新日志", "版本 15.10", icon = { Icon(Icons.Filled.Star, null, tint = AppColors.Subtle) }) {}
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("博客通知", "仅公告", icon = { Icon(Icons.Filled.Notifications, null, tint = AppColors.Subtle) }) {}
        }
        GroupLabel("支持")
        SettingsCard {
            SettingsRow("文档", icon = { Icon(Icons.Filled.Info, null, tint = AppColors.Subtle) }) {}
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("问题跟踪器", icon = { Icon(Icons.Filled.Warning, null, tint = AppColors.Subtle) }) {}
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("联系开发者", icon = { Icon(Icons.Filled.Email, null, tint = AppColors.Subtle) }) {}
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("发送应用程序日志", icon = { ClipIcon(AppColors.Subtle) }) {}
        }
        GroupLabel("社交")
        SettingsCard {
            SettingsRow("加入 r/tasks", icon = { Icon(Icons.Filled.Face, null, tint = AppColors.Subtle) }) {}
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow("关注 @tasks_org", icon = { Icon(Icons.Filled.Close, null, tint = AppColors.Subtle) }) {}
        }
        GroupLabel("开源")
        SettingsCard {
            SettingsRow("源代码", "Tasks 是遵循 GNU 通用公共许可证 v3.0 的自由开源软件", icon = { Icon(Icons.Filled.Star, null, tint = AppColors.Subtle) }) {}
        }
    }
}
