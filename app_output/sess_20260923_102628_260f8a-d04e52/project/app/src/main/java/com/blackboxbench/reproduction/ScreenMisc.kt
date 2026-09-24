package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ------------------------------------------------------------------ onboarding

@Composable
fun OnboardingScreen(onAddAccount: () -> Unit, onSkip: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.White)) {
        Box(
            Modifier.align(Alignment.TopCenter).padding(top = 265.dp).size(152.dp)
                .clip(CircleShape).background(Color(0xFF2196F3)),
            contentAlignment = Alignment.Center
        ) { DrawIcon("check", Color.White, 78.dp, 9.dp) }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 32.dp)
                .padding(bottom = 36.dp)
        ) {
            Box(
                Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(20.dp))
                    .background(C.blueDark).clickable { onAddAccount() },
                contentAlignment = Alignment.Center
            ) { Text("添加账号", color = Color.White, fontSize = 16.sp) }
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(20.dp))
                    .border(1.dp, Color(0xFFC4C7CA), RoundedCornerShape(20.dp)).clickable { onSkip() },
                contentAlignment = Alignment.Center
            ) { Text("继续但不同步", color = C.text, fontSize = 16.sp) }
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(20.dp))
                    .border(1.dp, Color(0xFFC4C7CA), RoundedCornerShape(20.dp)).clickable { onSkip() },
                contentAlignment = Alignment.Center
            ) { Text("导入 Tasks.org 备份", color = C.text, fontSize = 16.sp) }
        }
    }
}

@Composable
fun AddAccountScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color.White)) {
        BackBar("添加账号", onBack)
        val providers = listOf(
            "Tasks.org Cloud" to "使用 Tasks.org 账号同步",
            "Microsoft To Do" to "使用 Microsoft 账号同步",
            "Google Tasks" to "使用 Google 账号同步",
            "DAVx⁵" to "通过 DAVx⁵ 同步",
            "CalDAV" to "使用 CalDAV 账号同步",
            "EteSync" to "端到端加密同步",
            "DecSync CC" to "通过 DecSync 同步",
        )
        LazyColumn(Modifier.fillMaxSize()) {
            items(providers) { (name, sub) ->
                Row(
                    Modifier.fillMaxWidth().height(76.dp).padding(horizontal = 22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(38.dp).clip(CircleShape).background(Color(0xFFEDEDF2)),
                        contentAlignment = Alignment.Center
                    ) { DrawIcon("list", C.textDim, 20.dp) }
                    Spacer(Modifier.width(18.dp))
                    Column {
                        Text(name, color = C.text, fontSize = 17.sp)
                        Spacer(Modifier.height(3.dp))
                        Text(sub, color = C.textDim, fontSize = 13.sp)
                    }
                }
                HLine()
            }
        }
    }
}

@Composable
fun BackBar(title: String, onBack: () -> Unit, trailing: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().height(84.dp).padding(start = 14.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp).clickable { onBack() }, contentAlignment = Alignment.Center) {
            DrawIcon("back", C.text, 24.dp, 2.dp)
        }
        Spacer(Modifier.width(12.dp))
        Text(title, color = C.text, fontSize = 22.sp, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

// ------------------------------------------------------------------ settings

@Composable
fun SettingsScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    Column(Modifier.fillMaxSize().background(C.screenBg)) {
        BackBar("设置", onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(4.dp))
            SettingsCard {
                Row(
                    Modifier.fillMaxWidth().height(76.dp).clickable { onOpen("donate") }
                        .padding(horizontal = 22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DrawIcon("donate", C.text, 24.dp)
                    Spacer(Modifier.width(22.dp))
                    Column(Modifier.weight(1f)) {
                        Text("捐赠", color = C.text, fontSize = 18.sp)
                        Spacer(Modifier.height(3.dp))
                        Text("考虑用捐赠显示您的支持！", color = C.textDim, fontSize = 13.sp)
                    }
                    DrawIcon("chevron_right", C.textDim, 22.dp, 1.8.dp)
                }
            }
            Spacer(Modifier.height(12.dp))
            SettingsCard {
                SettingsRow("list_slash", "本地清单") { onOpen("locallists") }
                SettingsRow("plus", "添加账号") { onOpen("accounts") }
            }
            Spacer(Modifier.height(12.dp))
            SettingsCard {
                SettingsRow("palette", "外观") { onOpen("appearance") }
                SettingsRow("bell", "通知") { onOpen("notifications") }
            }
            Spacer(Modifier.height(12.dp))
            SettingsCard {
                SettingsRow("plus", "任务默认值") { onOpen("defaults") }
                SettingsRow("list", "任务清单选项") { onOpen("listoptions") }
                SettingsRow("save", "编辑屏幕选项") { onOpen("editoptions") }
            }
            Spacer(Modifier.height(12.dp))
            SettingsCard {
                SettingsRow("clock", "日期和时间") { onOpen("datetime") }
                SettingsRow("menu", "导航抽屉") { onOpen("drawer") }
                SettingsRow("note", "备份", warning = true) { onOpen("backup") }
                SettingsRow("widget", "插件设置") { onOpen("widgets") }
                SettingsRow("wrench", "高级") { onOpen("advanced") }
            }
            Spacer(Modifier.height(12.dp))
            SettingsCard {
                SettingsRow("info", "关于") { onOpen("about") }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp)).background(Color(0xFFF7F5FB))
    ) { content() }
}

@Composable
fun SettingsRow(icon: String, label: String, warning: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(66.dp).clickable { onClick() }.padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DrawIcon(icon, C.text, 24.dp)
        Spacer(Modifier.width(22.dp))
        Text(label, color = C.text, fontSize = 18.sp, modifier = Modifier.weight(1f))
        if (warning) {
            Box(
                Modifier.size(20.dp).clip(CircleShape).border(2.dp, Color(0xFFE8A33D), CircleShape),
                contentAlignment = Alignment.Center
            ) { Text("!", color = Color(0xFFE8A33D), fontSize = 12.sp) }
        }
    }
}

@Composable
fun AppearanceScreen(onBack: () -> Unit) {
    var dynamic by remember { mutableStateOf(false) }
    var markdown by remember { mutableStateOf(false) }
    var openLast by remember { mutableStateOf(true) }
    Column(Modifier.fillMaxSize().background(C.screenBg)) {
        BackBar("外观", onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            SettingsCard {
                Column(Modifier.fillMaxWidth().height(76.dp).padding(horizontal = 22.dp, vertical = 12.dp)) {
                    Text("主题", color = C.text, fontSize = 18.sp)
                    Spacer(Modifier.height(3.dp))
                    Text("系统默认", color = C.textDim, fontSize = 14.sp)
                }
                Row(
                    Modifier.fillMaxWidth().height(66.dp).padding(horizontal = 22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("动态", color = C.text, fontSize = 18.sp, modifier = Modifier.weight(1f))
                    Switch(checked = dynamic, onCheckedChange = { dynamic = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = C.blueDark))
                }
                Row(
                    Modifier.fillMaxWidth().height(66.dp).padding(horizontal = 22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(24.dp).clip(CircleShape).background(C.blue))
                    Spacer(Modifier.width(22.dp))
                    Text("颜色", color = C.text, fontSize = 18.sp)
                }
                Row(
                    Modifier.fillMaxWidth().height(66.dp).padding(horizontal = 22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(24.dp).clip(CircleShape).background(C.blue))
                    Spacer(Modifier.width(22.dp))
                    Text("启动器图标", color = C.text, fontSize = 18.sp)
                }
                Row(
                    Modifier.fillMaxWidth().height(82.dp).padding(horizontal = 22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Markdown", color = C.text, fontSize = 18.sp)
                        Spacer(Modifier.height(3.dp))
                        Text("在任务标题和描述中启用 Markdown", color = C.textDim, fontSize = 13.sp)
                    }
                    Switch(checked = markdown, onCheckedChange = { markdown = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = C.blueDark))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("启动时", color = C.textDim, fontSize = 14.sp, modifier = Modifier.padding(start = 26.dp, bottom = 6.dp))
            SettingsCard {
                Row(
                    Modifier.fillMaxWidth().height(66.dp).padding(horizontal = 22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("打开上次查看的清单", color = C.text, fontSize = 18.sp, modifier = Modifier.weight(1f))
                    Switch(checked = openLast, onCheckedChange = { openLast = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = C.blueDark))
                }
                Column(Modifier.fillMaxWidth().height(66.dp).padding(horizontal = 22.dp, vertical = 12.dp)) {
                    Text("打开清单", color = C.textFaint, fontSize = 18.sp)
                    Text("我的任务", color = C.textFaint, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("本地化", color = C.textDim, fontSize = 14.sp, modifier = Modifier.padding(start = 26.dp, bottom = 6.dp))
            SettingsCard {
                Column(Modifier.fillMaxWidth().height(76.dp).padding(horizontal = 22.dp, vertical = 12.dp)) {
                    Text("语言", color = C.text, fontSize = 18.sp)
                    Spacer(Modifier.height(3.dp))
                    Text("中文（中国）", color = C.textDim, fontSize = 14.sp)
                }
                SettingsRow("plus", "贡献翻译") { }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun SimpleSettingsScreen(title: String, rows: List<Pair<String, String>>, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(C.screenBg)) {
        BackBar(title, onBack)
        SettingsCard {
            rows.forEach { (label, value) ->
                Row(
                    Modifier.fillMaxWidth().height(66.dp).padding(horizontal = 22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, color = C.text, fontSize = 18.sp, modifier = Modifier.weight(1f))
                    if (value.isNotEmpty()) Text(value, color = C.textDim, fontSize = 16.sp)
                }
                HLine()
            }
        }
    }
}

// ------------------------------------------------------------------ tag picker

@Composable
fun TagPickerScreen(initial: List<String>, onDone: (List<String>) -> Unit, onBack: () -> Unit) {
    val selected = remember { mutableStateListOf<String>().also { it.addAll(initial) } }
    var query by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(C.screenBg)) {
        BackBar("标签", {
            onDone(selected.toList())
            onBack()
        })
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 18.dp)
                .clip(RoundedCornerShape(8.dp)).background(Color(0xFFEDEDF2)).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DrawIcon("search", C.textDim, 20.dp)
            Spacer(Modifier.width(12.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) Text("输入标签名称", color = C.textFaint, fontSize = 16.sp)
                AppTextField(
                    value = query, onValueChange = { query = it },
                    fontSize = 16f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        if (query.isNotBlank() && !Store.allTags().contains(query)) {
            Row(
                Modifier.fillMaxWidth().height(56.dp).clickable {
                    selected.add(query); Store.addTag(query); query = ""
                }.padding(horizontal = 22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DrawIcon("plus", C.text, 22.dp, 2.dp)
                Spacer(Modifier.width(18.dp))
                Text("新建标签 “$query”", color = C.text, fontSize = 17.sp)
            }
            HLine()
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(Store.allTags()) { tag ->
                Row(
                    Modifier.fillMaxWidth().height(56.dp).clickable {
                        if (selected.contains(tag)) selected.remove(tag) else selected.add(tag)
                    }.padding(horizontal = 22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CheckBoxView(
                        checked = selected.contains(tag), size = 22.dp, color = C.blue,
                        onToggle = {
                            if (selected.contains(tag)) selected.remove(tag) else selected.add(tag)
                        }
                    )
                    Spacer(Modifier.width(20.dp))
                    Text(tag, color = C.text, fontSize = 17.sp)
                }
                HLine()
            }
        }
    }
}

// ------------------------------------------------------------------ map

@Composable
fun MapPickerScreen(onPick: (String) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color(0xFFF2EFE9))) {
        Row(
            Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 16.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(6.dp)).background(Color.White),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(16.dp))
            DrawIcon("search", C.text, 22.dp)
            Spacer(Modifier.width(16.dp))
            Text("搜索", color = C.textDim, fontSize = 17.sp)
        }
        Box(Modifier.fillMaxWidth().weight(1f)) {
            MapGrid()
            Box(
                Modifier.align(Alignment.Center).size(46.dp),
                contentAlignment = Alignment.Center
            ) { DrawIcon("place", C.red, 46.dp, 2.4.dp) }
            Box(
                Modifier.align(Alignment.BottomEnd).padding(20.dp).size(56.dp).clip(CircleShape)
                    .background(C.blue),
                contentAlignment = Alignment.Center
            ) { DrawIcon("place", Color.White, 26.dp, 2.dp) }
            Text(
                "© OpenStreetMap contributors",
                color = Color(0xFF4A4A4A), fontSize = 13.sp,
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
            )
        }
        Column(Modifier.fillMaxWidth().background(Color.White)) {
            Row(
                Modifier.fillMaxWidth().height(64.dp).clickable { onPick("已选择的位置") }
                    .padding(horizontal = 22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DrawIcon("place", C.text, 24.dp)
                Spacer(Modifier.width(20.dp))
                Text("选择此位置", color = C.text, fontSize = 17.sp)
            }
        }
    }
}

@Composable
private fun MapGrid() {
    Column(Modifier.fillMaxSize()) {
        repeat(24) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFDDD8CE)))
            Spacer(Modifier.height(39.dp))
        }
    }
}

// ------------------------------------------------------------------ list / tag editor

@Composable
fun GroupEditorScreen(
    kind: String,
    existingId: String?,
    onClose: () -> Unit,
) {
    val isList = kind == "list"
    val existing = if (isList) Store.listById(existingId ?: "") else null
    val existingFilter = Store.filters.firstOrNull { it.id == existingId }

    var name by remember { mutableStateOf(existing?.name ?: existingFilter?.name ?: "") }
    var color by remember { mutableStateOf<Long?>(existing?.color ?: existingFilter?.color) }
    var icon by remember { mutableStateOf(existing?.icon ?: "list") }
    var showColors by remember { mutableStateOf(false) }
    var showIcons by remember { mutableStateOf(false) }
    var iconQuery by remember { mutableStateOf("") }
    var showDelete by remember { mutableStateOf(false) }

    val icons = listOf(
        "list", "briefcase", "cottage", "cart", "tag", "flag", "calendar", "clock",
        "place", "note", "bell", "photo", "folder", "timer", "widget", "star",
        "wrench", "palette", "check", "play"
    )

    Column(Modifier.fillMaxSize().background(C.screenBg)) {
        BackBar(if (isList) "清单" else "标签", onClose)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(14.dp)).background(Color(0xFFF7F5FB))
                    .padding(horizontal = 22.dp, vertical = 18.dp)
            ) {
                Text(if (isList) "这是本地清单" else "这是本地标签", color = C.text, fontSize = 17.sp)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    Text("关闭", color = C.blueDark, fontSize = 15.sp,
                        modifier = Modifier.clickable { }.padding(horizontal = 12.dp))
                    Text("添加账号", color = C.blueDark, fontSize = 15.sp,
                        modifier = Modifier.clickable { }.padding(horizontal = 12.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(14.dp)).background(Color(0xFFF7F5FB))
                    .padding(horizontal = 22.dp, vertical = 14.dp)
            ) {
                Text("显示名称", color = C.textDim, fontSize = 13.sp)
                AppTextField(
                    value = name, onValueChange = { name = it },
                    fontSize = 18f,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Card2 {
                Row(
                    Modifier.fillMaxWidth().height(64.dp).clickable { showColors = !showColors }
                        .padding(horizontal = 22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (color == null) {
                        Box(
                            Modifier.size(24.dp).clip(CircleShape).border(2.dp, C.textDim, CircleShape),
                            contentAlignment = Alignment.Center
                        ) { DrawIcon("close", C.textDim, 14.dp, 2.dp) }
                    } else {
                        Box(Modifier.size(24.dp).clip(CircleShape).background(Color(color!!)))
                    }
                    Spacer(Modifier.width(22.dp))
                    Text("颜色", color = C.text, fontSize = 18.sp, modifier = Modifier.weight(1f))
                    DrawIcon("chevron_right", C.textDim, 22.dp, 1.8.dp)
                }
                if (showColors) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        modifier = Modifier.fillMaxWidth().height(240.dp).padding(horizontal = 22.dp)
                    ) {
                        items(C.palette.size) { i ->
                            Box(
                                Modifier.padding(6.dp).aspectRatio(1f).clip(CircleShape)
                                    .background(Color(C.palette[i]))
                                    .clickable { color = C.palette[i]; showColors = false },
                                contentAlignment = Alignment.Center
                            ) { }
                        }
                    }
                }
                HLine()
                Row(
                    Modifier.fillMaxWidth().height(64.dp).clickable { showIcons = !showIcons }
                        .padding(horizontal = 22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DrawIcon(icon, Color(color ?: 0xFF9E9E9E), 24.dp, 1.8.dp)
                    Spacer(Modifier.width(22.dp))
                    Text("图标", color = C.text, fontSize = 18.sp, modifier = Modifier.weight(1f))
                    DrawIcon("chevron_right", C.textDim, 22.dp, 1.8.dp)
                }
                if (showIcons) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp)) {
                        Row(
                            Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEDEDF2)).padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DrawIcon("search", C.textDim, 18.dp)
                            Spacer(Modifier.width(10.dp))
                            AppTextField(
                                value = iconQuery, onValueChange = { iconQuery = it },
                                fontSize = 16f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(6),
                            modifier = Modifier.fillMaxWidth().height(200.dp)
                        ) {
                            items(icons.size) { i ->
                                val ic = icons[i]
                                Box(
                                    Modifier.padding(6.dp).aspectRatio(1f).clip(CircleShape)
                                        .background(Color(0xFFEDEDF2))
                                        .clickable { icon = ic; showIcons = false },
                                    contentAlignment = Alignment.Center
                                ) { DrawIcon(ic, C.text, 22.dp, 1.8.dp) }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Card2 {
                SettingsRow("plus", "添加快捷方式到主屏幕") { }
                SettingsRow("widget", "添加小部件到主屏幕") { }
            }
            Spacer(Modifier.height(12.dp))
            if (kind != "filter") {
                Card2 {
                    Row(
                        Modifier.fillMaxWidth().height(64.dp)
                            .clickable {
                                if (name.isNotBlank()) {
                                    if (isList) {
                                        if (existing != null) {
                                            existing.name = name; existing.color = color ?: existing.color
                                            existing.icon = icon
                                        } else {
                                            Store.lists.add(TaskList("list_" + System.currentTimeMillis(), name, color ?: 0xFF1E88E5, icon))
                                        }
                                    } else {
                                        Store.addTag(name)
                                    }
                                    Store.save(); onClose()
                                }
                            }
                            .padding(horizontal = 22.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DrawIcon("save", if (name.isBlank()) C.textFaint else C.text, 24.dp, 1.8.dp)
                        Spacer(Modifier.width(22.dp))
                        Text("保存", color = if (name.isBlank()) C.textFaint else C.text, fontSize = 18.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            if (existing != null || (existingId != null && !isList)) {
                Card2 {
                    Row(
                        Modifier.fillMaxWidth().height(64.dp).clickable { showDelete = true }
                            .padding(horizontal = 22.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DrawIcon("trash", C.red, 24.dp, 1.8.dp)
                        Spacer(Modifier.width(22.dp))
                        Text("删除", color = C.red, fontSize = 18.sp)
                    }
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text(if (isList) "是否删除 $name?" else "是否删除 $name?", fontSize = 22.sp) },
            text = { Text(if (isList) "不会删除此清单中的任务" else "不会删除含此标签的任务", fontSize = 16.sp) },
            confirmButton = {
                TextButton(onClick = {
                    if (isList) Store.lists.removeAll { it.id == existingId }
                    else { Store.tags.remove(name); Store.tasks.forEach { it.tags.remove(name) } }
                    Store.save(); showDelete = false; onClose()
                }) { Text("删除", fontSize = 17.sp) }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("取消", fontSize = 17.sp) }
            }
        )
    }
}

@Composable
private fun Card2(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp)).background(Color(0xFFF7F5FB))
    ) { content() }
}

// ------------------------------------------------------------------ filter editor

@Composable
fun FilterEditorScreen(onClose: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var minPriority by remember { mutableStateOf(-1) }
    var showAdd by remember { mutableStateOf(false) }
    var showPriority by remember { mutableStateOf(false) }

    val count = Store.tasks.count { minPriority < 0 || it.priority >= minPriority }

    Box(Modifier.fillMaxSize().background(C.screenBg)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(
                Modifier.fillMaxWidth().height(96.dp).background(C.blueDark).padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(40.dp).clickable { onClose() }, contentAlignment = Alignment.Center) {
                    DrawIcon("back", Color.White, 24.dp, 2.dp)
                }
                Spacer(Modifier.width(10.dp))
                Text("新建过滤器", color = Color.White, fontSize = 22.sp, modifier = Modifier.weight(1f))
                DrawIcon("help", Color.White, 24.dp, 1.8.dp)
            }
            Spacer(Modifier.height(12.dp))
            Card2 {
                Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 14.dp)) {
                    Text("显示名称", color = C.textDim, fontSize = 13.sp)
                    AppTextField(
                        value = name, onValueChange = { name = it },
                        fontSize = 18f,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                }
                HLine()
                SettingsRow("palette", "颜色") { }
                SettingsRow("tag", "图标") { }
            }
            Spacer(Modifier.height(12.dp))
            Text("过滤条件", color = C.textDim, fontSize = 14.sp, modifier = Modifier.padding(start = 26.dp, bottom = 6.dp))
            Card2 {
                Row(
                    Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DrawIcon("list", C.text, 24.dp)
                    Spacer(Modifier.width(22.dp))
                    Text("我的任务", color = C.text, fontSize = 18.sp, modifier = Modifier.weight(1f))
                    Text(count.toString(), color = C.textDim, fontSize = 16.sp)
                }
                HLine()
                Row(
                    Modifier.fillMaxWidth().height(64.dp).clickable { showPriority = true }
                        .padding(horizontal = 22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.width(46.dp))
                    Text("最低的优先级", color = C.text, fontSize = 18.sp, modifier = Modifier.weight(1f))
                    Text(priorityMarks(minPriority), color = C.red, fontSize = 17.sp)
                    Spacer(Modifier.width(16.dp))
                    Text(count.toString(), color = C.textDim, fontSize = 16.sp)
                }
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(90.dp))
        }
        Box(
            Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 30.dp)
                .height(52.dp).clip(RoundedCornerShape(16.dp)).background(C.blueDark)
                .clickable { showAdd = true }.padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DrawIcon("plus", Color.White, 22.dp, 2.2.dp)
                Spacer(Modifier.width(10.dp))
                Text("添加条件", color = Color.White, fontSize = 16.sp)
            }
        }
        if (name.isNotBlank()) {
            Box(
                Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = 30.dp)
                    .height(52.dp).clip(RoundedCornerShape(16.dp)).background(C.banner)
                    .clickable {
                        Store.filters.add(
                            FilterDef(
                                "filter_" + System.currentTimeMillis(), name,
                                minPriority = minPriority
                            )
                        )
                        Store.save(); onClose()
                    }.padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) { Text("保存", color = C.text, fontSize = 16.sp) }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("添加条件", fontSize = 20.sp) },
            text = {
                Column {
                    listOf("标签…", "开始于…", "截止于…", "优先级…", "标题含…").forEach { item ->
                        Text(
                            item, color = C.text, fontSize = 17.sp,
                            modifier = Modifier.fillMaxWidth().clickable {
                                showAdd = false
                                if (item == "优先级…") showPriority = true
                            }.padding(vertical = 12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAdd = false }) { Text("取消", fontSize = 17.sp) }
            }
        )
    }

    if (showPriority) {
        AlertDialog(
            onDismissRequest = { showPriority = false },
            title = { Text("优先级", fontSize = 20.sp) },
            text = {
                Column {
                    listOf(3 to "!!!", 2 to "!!", 1 to "!", -1 to "○").forEach { (p, mark) ->
                        Row(
                            Modifier.fillMaxWidth().clickable { minPriority = p; showPriority = false }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(mark, color = C.red, fontSize = 18.sp, modifier = Modifier.weight(1f))
                            if (minPriority == p) DrawIcon("check", C.blueDark, 20.dp, 2.dp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPriority = false }) { Text("取消", fontSize = 17.sp) }
            }
        )
    }
}

private fun priorityMarks(p: Int): String = when (p) {
    3 -> "!!!"; 2 -> "!!"; 1 -> "!"; else -> "○"
}

// ------------------------------------------------------------------ gear menu

@Composable
fun GearMenu(
    expanded: Boolean,
    viewId: String,
    onDismiss: () -> Unit,
    onAppSettings: () -> Unit,
    onViewSettings: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text("应用设置", fontSize = 17.sp, color = C.text) },
            onClick = { onDismiss(); onAppSettings() }
        )
        if (viewId.startsWith("list_") || viewId.startsWith("tag_")) {
            DropdownMenuItem(
                text = {
                    Text(
                        if (viewId.startsWith("list_")) "清单设置" else "标签设置",
                        fontSize = 17.sp, color = C.text
                    )
                },
                onClick = { onDismiss(); onViewSettings() }
            )
        }
    }
}
