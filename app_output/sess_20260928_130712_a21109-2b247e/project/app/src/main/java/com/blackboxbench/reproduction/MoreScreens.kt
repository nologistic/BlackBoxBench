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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
private fun BlueBar(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null) {
    Surface(color = AnkiColors.Primary, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                Box(Modifier.size(44.dp).clickable { onBack() }, contentAlignment = Alignment.Center) {
                    Text("\u2190", color = Color.White, fontSize = 22.sp)
                }
            }
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                if (subtitle != null) Text(subtitle, color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun SettingsScreen(
    app: AppState,
    onBack: () -> Unit,
    onGeneral: () -> Unit,
    onNoteTypes: () -> Unit
) {
    val categories = listOf(
        Triple("常规", "语言·学习中·全局设置", "\u2699"),
        Triple("新学习屏", "屏幕·工具栏·回答按钮", "\u25A3"),
        Triple("复习", "计划任务·保持屏幕常亮", "\u21BB"),
        Triple("同步", "AnkiWeb 帐户·自动同步", "\u2601"),
        Triple("通知", "通知类型·振动·呼吸灯", "\u25B3"),
        Triple("外观", "主题·背景", "\u25D0"),
        Triple("控制", "手势操作·键盘·蓝牙", "\u2318"),
        Triple("无障碍功能", "卡片缩放·「显示答案」按钮大小", "\u263A"),
        Triple("备份", "频率·备份保留期限", "\u21E1")
    )
    var query by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(Color.White)) {
        BlueBar("设置", onBack = onBack)
        Surface(color = Color(0xFFF0F0F0), shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth().padding(16.dp).height(44.dp)) {
            Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                SearchGlyph()
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("搜索...", color = AnkiColors.TextSecondary, fontSize = 16.sp)
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 16.sp, color = AnkiColors.TextPrimary),
                        cursorBrush = SolidColor(AnkiColors.Primary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        Column(Modifier.verticalScroll(rememberScrollState())) {
            categories.filter { query.isBlank() || it.first.contains(query) }.forEach { (title, sub, glyph) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { if (title == "常规") onGeneral() }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                        Text(glyph, fontSize = 20.sp, color = AnkiColors.TextPrimary)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(title, fontSize = 17.sp, color = AnkiColors.TextPrimary)
                        Text(sub, fontSize = 13.sp, color = AnkiColors.TextSecondary)
                    }
                }
                HorizontalDivider(color = AnkiColors.RowDivider, thickness = 0.8.dp)
            }
            Row(
                Modifier.fillMaxWidth().clickable { onNoteTypes() }.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { Text("\u25A4", fontSize = 20.sp) }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("笔记类型", fontSize = 17.sp, color = AnkiColors.TextPrimary)
                    Text("管理笔记模板与卡片", fontSize = 13.sp, color = AnkiColors.TextSecondary)
                }
            }
        }
    }
}

@Composable
fun GeneralSettingsScreen(app: AppState, onBack: () -> Unit) {
    var share by remember { mutableStateOf(false) }
    var fullDrawer by remember { mutableStateOf(false) }
    var pastePng by remember { mutableStateOf(false) }
    var doubleBack by remember { mutableStateOf(false) }
    var ankiMenu by remember { mutableStateOf(true) }
    var browserMenu by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        BlueBar("常规", onBack = onBack)
        Column(Modifier.verticalScroll(rememberScrollState())) {
            SettingTap("语言", "系统语言")
            SettingTap("错误报告模式", "询问我")
            SettingSwitch("分享软件使用情况", share) { share = it }
            SettingSwitch("全屏导航抽屉", fullDrawer) { fullDrawer = it }
            SectionHeader("编辑")
            SettingSwitch("将剪贴板图像粘贴为 PNG", pastePng) { pastePng = it }
            SectionHeader("学习中")
            SettingTap("新卡片的默认牌组", "使用当前牌组")
            SettingSwitch("按两次返回键返回/退出", doubleBack) { doubleBack = it }
            SectionHeader("全局设置")
            SettingSwitch("\u201CAnki 卡片\u201D 菜单", ankiMenu) { ankiMenu = it }
            SettingSwitch("\u201C卡片浏览器\u201D 菜单", browserMenu) { browserMenu = it }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        color = AnkiColors.Primary,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 18.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingSwitch(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 16.sp, color = AnkiColors.TextPrimary, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedTrackColor = AnkiColors.Primary))
    }
}

@Composable
private fun SettingTap(title: String, value: String) {
    Row(
        Modifier.fillMaxWidth().clickable { }.padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 16.sp, color = AnkiColors.TextPrimary, modifier = Modifier.weight(1f))
        Text(value, fontSize = 14.sp, color = AnkiColors.TextSecondary)
    }
}

@Composable
fun NoteTypesScreen(app: AppState, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val counts = app.noteTypes.associate { nt -> nt.name to app.notes.count { it.type == nt.name } }
    Column(Modifier.fillMaxSize().background(Color.White)) {
        BlueBar("笔记类型", "${app.noteTypes.size} 个笔记类型可用", onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            app.noteTypes.forEach { nt ->
                Row(
                    Modifier.fillMaxWidth().clickable { onOpen(nt.name) }.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(nt.name, fontSize = 16.sp, color = AnkiColors.TextPrimary)
                        Text("${counts[nt.name] ?: 0}笔记", fontSize = 13.sp, color = AnkiColors.TextSecondary)
                    }
                    Box(Modifier.size(40.dp).clickable { onOpen(nt.name) }, contentAlignment = Alignment.Center) {
                        Text("\u270E", fontSize = 17.sp, color = AnkiColors.TextSecondary)
                    }
                    Box(Modifier.size(40.dp).clickable { }, contentAlignment = Alignment.Center) {
                        Text("\u22EE", fontSize = 18.sp, color = AnkiColors.TextSecondary)
                    }
                }
                HorizontalDivider(color = AnkiColors.RowDivider, thickness = 0.8.dp)
            }
        }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
        FloatingActionButton(
            onClick = { },
            containerColor = AnkiColors.Primary,
            contentColor = Color.White,
            shape = androidx.compose.foundation.shape.CircleShape,
            modifier = Modifier.padding(16.dp)
        ) { Text("+", fontSize = 26.sp, color = Color.White) }
    }
}

@Composable
fun CardTemplateScreen(app: AppState, noteType: String, onBack: () -> Unit) {
    var tab by remember { mutableStateOf("卡片 1") }
    var section by remember { mutableStateOf("正面") }
    val nt = app.noteTypeByName(noteType)
    val body = when (section) {
        "正面" -> "{{${nt?.fields?.firstOrNull() ?: "正面"}}}"
        "背面" -> "{{FrontSide}}\n\n<hr id=answer>\n\n{{${nt?.fields?.getOrNull(1) ?: "背面"}}}"
        else -> ".card {\n  font-family: arial;\n  font-size: 20px;\n  text-align: center;\n}"
    }
    Column(Modifier.fillMaxSize().background(Color.White)) {
        BlueBar("卡片类型", noteType, onBack = onBack)
        Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(4.dp).height(44.dp).background(AnkiColors.Primary))
            Text(tab, fontSize = 15.sp, color = AnkiColors.TextPrimary, modifier = Modifier.padding(start = 16.dp))
        }
        HorizontalDivider(color = AnkiColors.RowDivider)
        Box(Modifier.weight(1f).fillMaxWidth().background(Color(0xFFFAFAFA)).padding(12.dp)) {
            Text(body, fontSize = 14.sp, color = AnkiColors.TextPrimary)
        }
        HorizontalDivider(color = AnkiColors.RowDivider)
        Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf("正面", "背面", "样式").forEach { s ->
                Box(
                    Modifier.weight(1f).fillMaxSize().clickable { section = s },
                    contentAlignment = Alignment.Center
                ) {
                    Text(s, fontSize = 14.sp, color = if (section == s) AnkiColors.Primary else AnkiColors.TextSecondary)
                }
            }
        }
    }
}

@Composable
fun StatisticsScreen(app: AppState, onBack: () -> Unit, onOpenDrawer: () -> Unit) {
    var deckOpen by remember { mutableStateOf(false) }
    val total = app.cards.size
    val newCount = app.cards.count { it.state == CState.NEW }
    val learnCount = app.cards.count { it.state == CState.LEARNING }
    val dueCount = app.cards.count { it.state == CState.REVIEW && it.dueDay <= today() }
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Surface(color = AnkiColors.Primary, modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(44.dp).clickable { onOpenDrawer() }, contentAlignment = Alignment.Center) {
                    HamburgerIcon()
                }
                Text("全部牌组", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 8.dp).weight(1f))
                Box(Modifier.size(44.dp).clickable { deckOpen = true }, contentAlignment = Alignment.Center) {
                    Text("\u25BE", color = Color.White, fontSize = 16.sp)
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(20.dp).verticalScroll(rememberScrollState())) {
            Text("今日学习", fontSize = 20.sp, color = AnkiColors.TextPrimary, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(12.dp))
            StatRow("已学习卡片", "${app.studiedCards}")
            StatRow("复习次数", "${app.reviewedToday}")
            StatRow("正确", "${app.correctToday}")
            Spacer(Modifier.height(20.dp))
            Text("卡片状态", fontSize = 20.sp, color = AnkiColors.TextPrimary, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(12.dp))
            StatRow("新卡片", "$newCount")
            StatRow("学习中", "$learnCount")
            StatRow("待复习", "$dueCount")
            StatRow("卡片总数", "$total")
            Spacer(Modifier.height(20.dp))
            Text("未来 30 天到期", fontSize = 20.sp, color = AnkiColors.TextPrimary, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().height(120.dp), verticalAlignment = Alignment.Bottom) {
                listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).forEach { i ->
                    val h = (20 + (i * 7) % 70).dp
                    Box(
                        Modifier.weight(1f).padding(horizontal = 2.dp).height(h).background(AnkiColors.Primary)
                    )
                }
            }
        }
    }
    androidx.compose.material3.DropdownMenu(expanded = deckOpen, onDismissRequest = { deckOpen = false }) {
        app.decks.forEach { d ->
            androidx.compose.material3.DropdownMenuItem(
                text = { Text(d.name, fontSize = 15.sp) },
                onClick = { deckOpen = false }
            )
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 15.sp, color = AnkiColors.TextPrimary, modifier = Modifier.weight(1f))
        Text(value, fontSize = 15.sp, color = AnkiColors.TextPrimary, fontWeight = FontWeight.Medium)
    }
}
