package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
private fun SettingsScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    val c = LocalMarkorColors.current
    Column(modifier = Modifier.fillMaxSize().background(c.background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(c.toolbar)
                .padding(horizontal = 8.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(44.dp).clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) { IcoArrowLeft(size = 22.dp, tint = c.onToolbar) }
            Text(title, color = c.onToolbar, fontSize = 21.sp, modifier = Modifier.padding(start = 8.dp))
        }
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) { content() }
    }
}

@Composable
private fun ValueRow(
    title: String,
    value: String,
    icon: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    SettingRow(
        title = title,
        subtitle = value,
        icon = icon,
        showDivider = true,
        onClick = onClick,
    )
}

@Composable
private fun SwitchSetting(
    title: String,
    checked: Boolean,
    subtitle: String? = null,
    icon: (@Composable () -> Unit)? = null,
    onChange: (Boolean) -> Unit,
) {
    val c = LocalMarkorColors.current
    SettingRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        showDivider = true,
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onChange,
                colors = SwitchDefaults.colors(checkedTrackColor = c.accent),
            )
        },
    )
}

@Composable
fun SettingsScreen(
    state: AppState,
    onBack: () -> Unit,
    onOpenGeneral: () -> Unit,
    onOpenOther: () -> Unit,
    onOpenEdit: () -> Unit,
    onOpenView: () -> Unit,
    onOpenFormat: (String) -> Unit,
) {
    val c = LocalMarkorColors.current
    var themeDialog by remember { mutableStateOf(false) }
    var languageDialog by remember { mutableStateOf(false) }

    SettingsScaffold(title = "设置", onBack = onBack) {
        ValueRow("通用", "文件与文件夹", icon = { IcoFolder(size = 24.dp, tint = c.textSecondary, filled = true) }, onClick = onOpenGeneral)
        ValueRow("其它", "杂项设置", icon = { IcoGear(size = 24.dp, tint = c.textSecondary) }, onClick = onOpenOther)
        ValueRow("编辑模式", "编辑器设置", icon = { IcoPencil(size = 24.dp, tint = c.textSecondary) }, onClick = onOpenEdit)
        ValueRow("视图模式", "显示转换后的标记", icon = { IcoEye(size = 24.dp, tint = c.textSecondary) }, onClick = onOpenView)
        SectionHeader("格式")
        listOf("Markdown", "todo.txt", "Wikitext / Zim", "纯文本", "AsciiDoc", "OrgMode").forEach { name ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenFormat(name) }
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                    when (name) {
                        "Markdown" -> Text("M↓", color = c.textSecondary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        "todo.txt" -> IcoCheckSquare(size = 24.dp, tint = c.textSecondary)
                        "Wikitext / Zim" -> IcoPencil(size = 24.dp, tint = c.textSecondary)
                        "纯文本" -> Text("Tt", color = c.textSecondary, fontSize = 15.sp)
                        "AsciiDoc" -> IcoTextLines(size = 24.dp, tint = c.textSecondary)
                        else -> IcoStar(size = 24.dp, tint = c.textSecondary)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Text(name, color = c.textPrimary, fontSize = 17.sp)
            }
            HorizontalDivider(color = c.divider)
        }
        SectionHeader("基础设置")
        ValueRow("主题", state.prefs.str("theme", "System"), icon = { IcoPalette(size = 24.dp, tint = c.textSecondary) }) { themeDialog = true }
        SettingRow(
            title = "语言",
            subtitle = "更改语言（重启应用后生效）\n${state.prefs.str("language", "Chinese (中文, 中国)")}",
            icon = { IcoTranslate(size = 24.dp, tint = c.textSecondary) },
            showDivider = true,
            onClick = { languageDialog = true },
        )
        Spacer(Modifier.height(24.dp))
    }

    if (themeDialog) {
        OptionDialog(
            title = "主题",
            options = listOf("System", "Auto", "Auto (09:00 - 17:00)", "浅色", "深色", "黑色"),
            selected = state.prefs.str("theme", "System"),
            onSelect = {
                state.prefs.setStr("theme", it)
                themeDialog = false
                state.toast("主题已切换为 $it")
            },
            onDismiss = { themeDialog = false },
        )
    }

    if (languageDialog) {
        OptionDialog(
            title = "语言",
            options = listOf("Chinese (中文, 中国)", "English", "Deutsch", "日本語"),
            selected = state.prefs.str("language", "Chinese (中文, 中国)"),
            onSelect = {
                state.prefs.setStr("language", it)
                languageDialog = false
                state.toast("重启应用后生效")
            },
            onDismiss = { languageDialog = false },
        )
    }
}

@Composable
fun GeneralSettingsScreen(state: AppState, onBack: () -> Unit) {
    val c = LocalMarkorColors.current
    val prefs = state.prefs
    var startTabDialog by remember { mutableStateOf(false) }
    var depthDialog by remember { mutableStateOf(false) }
    var passwordDialog by remember { mutableStateOf(false) }

    SettingsScaffold(title = "通用", onBack = onBack) {
        SectionHeader("保存位置")
        ValueRow("笔记本", "Documents/markor", icon = { IcoFolder(size = 24.dp, tint = c.textSecondary, filled = true) })
        ValueRow("QuickNote", "Documents/markor/QuickNote.md", icon = { IcoBolt(size = 24.dp, tint = c.textSecondary) })
        ValueRow("To-Do", "Documents/markor/todo.txt", icon = { IcoCheckSquare(size = 24.dp, tint = c.textSecondary) })
        SectionHeader("杂项设置")
        SwitchSetting("总是使用本应用打开", prefs.bool("alwaysOpenWith", false), icon = { IcoOpenExternal(size = 24.dp, tint = c.textSecondary) }) {
            prefs.setBool("alwaysOpenWith", it)
        }
        ValueRow("应用程序启动选项卡", prefs.str("startTab", "笔记本"), icon = { IcoDoc(size = 24.dp, tint = c.textSecondary) }) { startTabDialog = true }
        ValueRow("应用起始文件夹", "笔记本", icon = { IcoFolder(size = 24.dp, tint = c.textSecondary) })
        ValueRow("文件加密密码", if (prefs.bool("hasPassword", false)) "已设置" else "未设置", icon = { IcoLock(size = 24.dp, tint = c.textSecondary) }) { passwordDialog = true }
        SectionHeader("搜索")
        ValueRow("搜索深度", prefs.str("searchDepth", "不限"), icon = { IcoSort(size = 24.dp, tint = c.textSecondary) }) { depthDialog = true }
        ValueRow("忽略名单", "未设置", icon = { IcoImage(size = 24.dp, tint = c.textSecondary) })
        SectionHeader("Features")
        SwitchSetting("语法高亮", prefs.bool("editorHighlight", true), "控制是否应突出显示输入的文本", icon = { IcoTextLines(size = 24.dp, tint = c.textSecondary) }) {
            prefs.setBool("editorHighlight", it)
        }
        SwitchSetting("Launcher (特殊文件)", prefs.bool("launcherSpecial", false), "App drawer launchers - To-Do, QuickNote", icon = { IcoBolt(size = 24.dp, tint = c.textSecondary) }) {
            prefs.setBool("launcherSpecial", it)
        }
        SwitchSetting("多窗口", prefs.bool("multiWindow", false), "始终在单独的窗口中打开文档。使用设备的\"最近\"键进行切换。", icon = { IcoOpenExternal(size = 24.dp, tint = c.textSecondary) }) {
            prefs.setBool("multiWindow", it)
        }
        SwitchSetting("实验功能", prefs.bool("experimental", false), icon = { IcoGear(size = 24.dp, tint = c.textSecondary) }) {
            prefs.setBool("experimental", it)
        }
        Spacer(Modifier.height(24.dp))
    }

    if (startTabDialog) {
        OptionDialog(
            title = "应用程序启动选项卡",
            options = listOf("笔记本", "To-Do", "QuickNote"),
            selected = prefs.str("startTab", "笔记本"),
            onSelect = { prefs.setStr("startTab", it); startTabDialog = false },
            onDismiss = { startTabDialog = false },
        )
    }
    if (depthDialog) {
        OptionDialog(
            title = "搜索深度",
            options = listOf("不限", "1", "2", "3"),
            selected = prefs.str("searchDepth", "不限"),
            onSelect = { prefs.setStr("searchDepth", it); depthDialog = false },
            onDismiss = { depthDialog = false },
        )
    }
    if (passwordDialog) {
        ConfirmDialog(
            title = "文件加密密码",
            body = if (prefs.bool("hasPassword", false)) "移除已保存的密码？" else "为笔记本设置加密密码？",
            onConfirm = {
                prefs.setBool("hasPassword", !prefs.bool("hasPassword", false))
                passwordDialog = false
                state.toast("已更新密码设置")
            },
            onDismiss = { passwordDialog = false },
        )
    }
}

@Composable
fun OtherSettingsScreen(state: AppState, onBack: () -> Unit) {
    val c = LocalMarkorColors.current
    val prefs = state.prefs
    var navDialog by remember { mutableStateOf(false) }
    var descDialog by remember { mutableStateOf(false) }
    var restoreDialog by remember { mutableStateOf(false) }

    SettingsScaffold(title = "其它", onBack = onBack) {
        SectionHeader("通用")
        ValueRow("导航栏颜色", prefs.str("navColor", "黑色"), icon = { IcoPalette(size = 24.dp, tint = c.textSecondary) }) { navDialog = true }
        SwitchSetting("保持屏幕开启", prefs.bool("keepScreenOn", false), icon = { IcoDoc(size = 24.dp, tint = c.textSecondary) }) { prefs.setBool("keepScreenOn", it) }
        SwitchSetting("禁止截屏", prefs.bool("noScreenshot", false), icon = { IcoLock(size = 24.dp, tint = c.textSecondary) }) { prefs.setBool("noScreenshot", it) }
        ValueRow("分享到 - 格式", prefs.str("shareFormat", "纯文本"), icon = { IcoShare(size = 24.dp, tint = c.textSecondary) })
        ValueRow("附件文件夹名", prefs.str("attachmentFolder", ".attachments"), icon = { IcoLink(size = 24.dp, tint = c.textSecondary) })
        ValueRow("文本片段/模板目录", "/storage/emulated/0/Documents/markor/.app/snippets", icon = { IcoFolder(size = 24.dp, tint = c.textSecondary, filled = true) })
        SwitchSetting("Chrome Custom Tabs", prefs.bool("customTabs", true), icon = { IcoOpenExternal(size = 24.dp, tint = c.textSecondary) }) { prefs.setBool("customTabs", it) }
        SectionHeader("文件浏览器")
        ValueRow("文件描述格式", prefs.str("fileDesc", "默认"), icon = { IcoInfo(size = 24.dp, tint = c.textSecondary) }) { descDialog = true }
        SectionHeader("Backup")
        ValueRow("备份设置", "导出设置到 JSON 文件", icon = { IcoSave(size = 24.dp, tint = c.textSecondary) }) {
            val target = java.io.File(state.store.root, "settings-backup.json")
            state.store.write(target, prefs.exportToJson())
            state.toast("已导出 ${target.name}")
        }
        ValueRow("还原设置", "从备份 JSON 文件导入设置。成功后，应用将重启。", icon = { IcoRefresh(size = 24.dp, tint = c.textSecondary) }) { restoreDialog = true }
        Spacer(Modifier.height(24.dp))
    }

    if (navDialog) {
        OptionDialog(
            title = "导航栏颜色",
            options = listOf("黑色", "深色", "主题色"),
            selected = prefs.str("navColor", "黑色"),
            onSelect = { prefs.setStr("navColor", it); navDialog = false },
            onDismiss = { navDialog = false },
        )
    }
    if (descDialog) {
        OptionDialog(
            title = "文件描述格式",
            options = listOf("默认", "名称", "日期", "大小"),
            selected = prefs.str("fileDesc", "默认"),
            onSelect = { prefs.setStr("fileDesc", it); descDialog = false },
            onDismiss = { descDialog = false },
        )
    }
    if (restoreDialog) {
        val backup = java.io.File(state.store.root, "settings-backup.json")
        ConfirmDialog(
            title = "还原设置",
            body = if (backup.exists()) "从 settings-backup.json 导入设置？" else "未找到备份文件 settings-backup.json",
            onConfirm = {
                restoreDialog = false
                if (backup.exists()) {
                    val count = prefs.importFromJson(state.store.read(backup))
                    state.toast("已导入 $count 项设置")
                } else {
                    state.toast("未找到备份文件")
                }
            },
            onDismiss = { restoreDialog = false },
        )
    }
}

@Composable
fun EditModeSettingsScreen(state: AppState, onBack: () -> Unit) {
    val c = LocalMarkorColors.current
    val prefs = state.prefs
    SettingsScaffold(title = "编辑模式", onBack = onBack) {
        SettingRow(
            title = "字体",
            subtitle = prefs.str("editorFont", "Roboto Regular (sans-serif-regular)"),
            icon = { Text("A", color = c.textSecondary, fontSize = 18.sp) },
            showDivider = true,
        )
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text("字号", color = c.textPrimary, fontSize = 16.sp)
            Text("调整编辑器中文字的字体大小", color = c.textSecondary, fontSize = 13.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = prefs.int("editorFontSize", 15).toFloat(),
                    onValueChange = { prefs.setInt("editorFontSize", it.toInt()) },
                    valueRange = 10f..32f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = c.accent, activeTrackColor = c.accent),
                )
                Text("${prefs.int("editorFontSize", 15)}", color = c.textPrimary, fontSize = 15.sp)
            }
        }
        HorizontalDivider(color = c.divider)
        ValueRow("基本颜色方案", prefs.str("colorScheme", "默认"), icon = { IcoPalette(size = 24.dp, tint = c.textSecondary) })
        SectionHeader("语法高亮")
        SwitchSetting("下划线十六进制颜色代码", prefs.bool("hexUnderline", false), "是否根据颜色在十六进制颜色代码下划线", icon = { IcoCode(size = 24.dp, tint = c.textSecondary) }) {
            prefs.setBool("hexUnderline", it)
        }
        SwitchSetting("停用语法检查", prefs.bool("syntaxCheck", true), "保留输入法建议的同时，移除自动纠错的下划线", icon = { IcoTextLines(size = 24.dp, tint = c.textSecondary) }) {
            prefs.setBool("syntaxCheck", it)
        }
        SwitchSetting("为代码使用等宽字体", prefs.bool("monoCode", false), "用不同的字体显示代码。启用后可能会大幅影响性能。", icon = { IcoCode(size = 24.dp, tint = c.textSecondary) }) {
            prefs.setBool("monoCode", it)
        }
        SwitchSetting("停用代码块高亮", prefs.bool("noCodeHighlight", false), icon = { IcoCode(size = 24.dp, tint = c.textSecondary) }) {
            prefs.setBool("noCodeHighlight", it)
        }
        SwitchSetting("大标题", prefs.bool("bigHeadings", false), "根据标题级别(# h1, ## h2, ### h3...) 增加标题文字大小", icon = { IcoTextLines(size = 24.dp, tint = c.textSecondary) }) {
            prefs.setBool("bigHeadings", it)
        }
        SectionHeader("杂项设置")
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text("快捷键间距", color = c.textPrimary, fontSize = 16.sp)
            Text("快捷键之间的水平间距", color = c.textSecondary, fontSize = 13.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = prefs.int("shortcutSpacing", 8).toFloat(),
                    onValueChange = { prefs.setInt("shortcutSpacing", it.toInt()) },
                    valueRange = 0f..24f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = c.accent, activeTrackColor = c.accent),
                )
                Text("${prefs.int("shortcutSpacing", 8)}", color = c.textPrimary, fontSize = 15.sp)
            }
        }
        SwitchSetting("自动换行", prefs.bool("editorWrap", true), "如果文字不适合屏幕宽度则软打断。否则启用横向滚动。", icon = { IcoEye(size = 24.dp, tint = c.textSecondary) }) {
            prefs.setBool("editorWrap", it)
        }
        SwitchSetting("居中文字", prefs.bool("centerText", false), "在屏幕中央编辑文本", icon = { IcoTextLines(size = 24.dp, tint = c.textSecondary) }) {
            prefs.setBool("centerText", it)
        }
        SwitchSetting("从文末开始", prefs.bool("startAtEnd", false), "载入文件时将光标置于文件末尾处", icon = { IcoDownload(size = 24.dp, tint = c.textSecondary) }) {
            prefs.setBool("startAtEnd", it)
        }
        SwitchSetting("使用 Tab 键缩进行", prefs.bool("tabIndent", true), icon = { IcoArrowRight(size = 24.dp, tint = c.textSecondary) }) {
            prefs.setBool("tabIndent", it)
        }
        ValueRow("无序列表标示字符", prefs.str("bulletChar", "-"), icon = { IcoListBullets(size = 24.dp, tint = c.textSecondary) })
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun ViewModeSettingsScreen(state: AppState, onBack: () -> Unit) {
    val c = LocalMarkorColors.current
    val prefs = state.prefs
    SettingsScaffold(title = "视图模式", onBack = onBack) {
        SwitchSetting("首选视图", prefs.bool("preferredView", false), "在查看模式中打开现有文件", icon = { IcoEye(size = 24.dp, tint = c.textSecondary) }) {
            prefs.setBool("preferredView", it)
        }
        SectionHeader("Inject")
        ValueRow("Inject -> head", "html: <head>CSS, JavaScript</head>", icon = { IcoAdd(size = 24.dp, tint = c.textSecondary) })
        ValueRow("Inject -> body", "", icon = { IcoAdd(size = 24.dp, tint = c.textSecondary) })
        SectionHeader("Text")
        SettingRow(
            title = "链接颜色",
            icon = { IcoLink(size = 24.dp, tint = c.textSecondary) },
            showDivider = true,
            trailing = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF2E7D32), RoundedCornerShape(2.dp)),
                )
            },
        )
        SwitchSetting("从右往左渲染", prefs.bool("rtl", false), "渲染从右往左读的语言", icon = { IcoTextLines(size = 24.dp, tint = c.textSecondary) }) {
            prefs.setBool("rtl", it)
        }
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text("字号", color = c.textPrimary, fontSize = 16.sp)
            Text("-1 => Editor", color = c.textSecondary, fontSize = 13.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = prefs.int("viewFontSize", -1).toFloat(),
                    onValueChange = { prefs.setInt("viewFontSize", it.toInt()) },
                    valueRange = -1f..32f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = c.accent, activeTrackColor = c.accent),
                )
                Text("${prefs.int("viewFontSize", -1)}", color = c.textPrimary, fontSize = 15.sp)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun FormatSettingsScreen(format: String, state: AppState, onBack: () -> Unit) {
    val c = LocalMarkorColors.current
    val prefs = state.prefs
    SettingsScaffold(title = format, onBack = onBack) {
        SectionHeader("语法高亮")
        SwitchSetting(
            "语法高亮",
            prefs.bool("format.$format.highlight", true),
            "控制 $format 文档是否突出显示标记",
            icon = { IcoTextLines(size = 24.dp, tint = c.textSecondary) },
        ) { prefs.setBool("format.$format.highlight", it) }
        SwitchSetting(
            "大标题",
            prefs.bool("format.$format.bigHeadings", false),
            "根据标题级别增加标题文字大小",
            icon = { IcoTextLines(size = 24.dp, tint = c.textSecondary) },
        ) { prefs.setBool("format.$format.bigHeadings", it) }
        SectionHeader("模板")
        ValueRow("新建文件模板", prefs.str("format.$format.template", "空文件"), icon = { IcoDoc(size = 24.dp, tint = c.textSecondary) })
        ValueRow("文件扩展名", formatExtension(format), icon = { IcoTextLines(size = 24.dp, tint = c.textSecondary) })
        Spacer(Modifier.height(24.dp))
    }
}

private fun formatExtension(format: String): String = when (format) {
    "Markdown" -> ".md"
    "todo.txt" -> ".txt"
    "Wikitext / Zim" -> ".wiki"
    "纯文本" -> ".txt"
    "AsciiDoc" -> ".adoc"
    else -> ".org"
}

@Composable
fun MoreScreen(
    state: AppState,
    onSelectTab: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val c = LocalMarkorColors.current
    Column(modifier = Modifier.fillMaxSize().background(c.background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(c.toolbar)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Markor", color = c.onToolbar, fontSize = 22.sp, fontWeight = FontWeight.Medium)
        }
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(c.surface)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFF37474F), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center,
                ) { Text("M", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Markor", color = c.textPrimary, fontSize = 19.sp)
                    Text("net.gsantner.markor", color = c.textSecondary, fontSize = 15.sp)
                    Text("Version v2.16.1 (163)", color = c.textSecondary, fontSize = 15.sp)
                }
            }
            HorizontalDivider(color = c.divider)
            SettingRow("存在疑问或问题吗?", "给出反馈，报告问题，或提出改进建议", icon = { IcoGear(size = 24.dp, tint = c.textSecondary) }, showDivider = true) {
                state.toast("已打开反馈通道")
            }
            SettingRow("设置", icon = { IcoGear(size = 24.dp, tint = c.textSecondary) }, showDivider = true, onClick = onOpenSettings)
            SettingRow("帮助 / FAQ", icon = { IcoInfo(size = 24.dp, tint = c.textSecondary) }, showDivider = true) {
                state.toast("帮助文档位于项目主页")
            }
            SettingRow("评价本应用", icon = { IcoThumb() }, showDivider = true) { state.toast("感谢支持") }
            SectionHeader("项目团队")
            SettingRow(
                "Gregor Santner (gsantner)",
                "Austrian software developer and Open Source enthusiast",
                icon = { IcoPerson(size = 24.dp, tint = c.textSecondary) },
                showDivider = true,
            )
            SectionHeader("社区")
            SettingRow("翻译", "翻译本应用", icon = { IcoTranslate(size = 24.dp, tint = c.textSecondary) }, showDivider = true) {
                state.toast("翻译在社区平台进行")
            }
            SettingRow("加入社区", icon = { IcoPeople(size = 24.dp, tint = c.textSecondary) }, showDivider = true) {
                state.toast("欢迎加入社区")
            }
            SettingRow(
                "贡献者",
                "显示贡献者信息。贡献者可自行选择是否被添加到这里。",
                icon = { IcoInfo(size = 24.dp, tint = c.textSecondary) },
                showDivider = true,
            ) { state.toast("感谢所有贡献者") }
            SectionHeader("开源协议")
            SettingRow("项目开源协议", "Apache 2.0", icon = { IcoCopyright(size = 24.dp, tint = c.textSecondary) }, showDivider = true)
            SettingRow("开源协议", icon = { IcoCopyright(size = 24.dp, tint = c.textSecondary) }, showDivider = true) {
                state.toast("Apache License 2.0")
            }
            SettingRow(
                "源代码",
                "为本项目贡献代码。我们欢迎所有人参与进来，包括新手",
                icon = { IcoCode(size = 24.dp, tint = c.textSecondary) },
                showDivider = true,
            ) { state.toast("源码托管在公开仓库") }
            Column(modifier = Modifier.fillMaxWidth().background(c.surface).padding(16.dp)) {
                Text("Copy build information", color = c.textPrimary, fontSize = 16.sp)
                listOf(
                    "Package: net.gsantner.markor",
                    "Version: v2.16.1 (163)",
                    "Flavor: Default (release)",
                    "Build date: 2026-03-19T08:23Z",
                    "ISource: Sideloaded",
                    "VCS Hash: f33eb6a8dfb210f27bfe7294430d4d39b795bd0f",
                    "VCS Msg: Markor v2.16.1 (code 163)",
                ).forEach { line ->
                    Text(line, color = c.textSecondary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        AppBottomBar(selected = "more", onSelect = onSelectTab)
    }
}

@Composable
private fun IcoThumb() {
    val c = LocalMarkorColors.current
    Text("👍", fontSize = 18.sp, color = c.textSecondary)
}

@Composable
private fun IcoCopyright(size: androidx.compose.ui.unit.Dp = 24.dp, tint: Color) {
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Text("©", color = tint, fontSize = 18.sp)
    }
}
