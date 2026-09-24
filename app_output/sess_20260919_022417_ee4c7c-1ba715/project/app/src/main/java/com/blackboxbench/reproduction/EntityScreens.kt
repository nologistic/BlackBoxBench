package com.blackboxbench.reproduction

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import java.util.UUID

// ===================== 任务标签选择页 =====================
@Composable
fun TaskTagPickerScreen(taskId: String, onBack: () -> Unit) {
    Store.version.value
    val task = Store.tasks.firstOrNull { it.id == taskId } ?: run { onBack(); return }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) }
            Text("输入标签名称", fontSize = 20.sp, modifier = Modifier.padding(start = 8.dp))
        }
        Store.tags.forEach { tag ->
            Row(
                Modifier.fillMaxWidth().clickable {
                    if (task.tagIds.contains(tag.id)) task.tagIds.remove(tag.id)
                    else task.tagIds.add(tag.id)
                    Store.touch(task)
                }.padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TagIcon(if (tag.color != -1) Color(tag.color) else AppColors.Blue)
                Spacer(Modifier.width(14.dp))
                Text(tag.name, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Box(
                    Modifier.size(24.dp).border(2.dp, AppColors.Subtle, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (task.tagIds.contains(tag.id)) {
                        Box(Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)).background(AppColors.Blue)) {
                            Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

// ===================== 通用条目编辑器骨架（标签/清单/地点） =====================
@Composable
fun EntityEditorScreen(
    title: String,
    name: String,
    color: Int,
    icon: String,
    showLocalBanner: Boolean,
    canDelete: Boolean,
    onBack: () -> Unit,
    onSave: (String, Int, String) -> Unit,
    onDelete: () -> Unit,
    onAddAccount: () -> Unit
) {
    var nameField by remember {
        mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(
            name,
            selection = androidx.compose.ui.text.TextRange(0, name.length)
        ))
    }
    var colorV by remember { mutableStateOf(color) }
    var iconV by remember { mutableStateOf(icon) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    var showDiscard by remember { mutableStateOf(false) }
    var showBanner by remember { mutableStateOf(true) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val dirty = nameField.text != name || colorV != color || iconV != icon

    BackHandler { if (dirty) showDiscard = true else onBack() }

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row {
            IconButton(onClick = { if (dirty) showDiscard = true else onBack() }) { Icon(Icons.Filled.ArrowBack, null) }
        }
        if (showLocalBanner && showBanner) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                    .background(AppColors.BannerBg).padding(20.dp)
            ) {
                Text("这是本地清单", fontSize = 20.sp, color = Color(0xFF333333))
                Spacer(Modifier.height(10.dp))
                Text("此清单中的任务只存储在这台设备上。连接账号保护数据，在任何地方访问它。", color = Color(0xFF555555))
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { showBanner = false }) { Text("关闭") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onAddAccount, shape = RoundedCornerShape(20.dp)) { Text("添加账号") }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(16.dp)) {
                Text("显示名称", fontSize = 13.sp, color = AppColors.Subtle)
                TextField(
                    value = nameField, onValueChange = { nameField = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column {
                Row(
                    Modifier.fillMaxWidth().clickable { showColorPicker = true }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (colorV != -1) Box(Modifier.size(26.dp).clip(CircleShape).background(Color(colorV)))
                    else Text("⊘", fontSize = 22.sp, color = AppColors.Subtle)
                    Spacer(Modifier.width(16.dp))
                    Text("颜色", Modifier.weight(1f))
                    if (colorV != -1) Icon(Icons.Filled.Close, null, Modifier.clickable { colorV = -1 }, tint = AppColors.Subtle)
                    else Icon(Icons.Filled.KeyboardArrowRight, null, tint = AppColors.Subtle)
                }
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                Row(
                    Modifier.fillMaxWidth().clickable { showIconPicker = true }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val iv = iconByKey(iconV)
                    if (iv != null) Icon(iv, null) else TagIcon(AppColors.Subtle)
                    Spacer(Modifier.width(16.dp))
                    Text("图标", Modifier.weight(1f))
                    Icon(Icons.Filled.KeyboardArrowRight, null, tint = AppColors.Subtle)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    HomeIcon(AppColors.Subtle); Spacer(Modifier.width(16.dp))
                    Text("添加快捷方式到主屏幕", color = AppColors.Subtle)
                }
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    GridIcon(AppColors.Subtle); Spacer(Modifier.width(16.dp))
                    Text("添加小部件到主屏幕", color = AppColors.Subtle)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.clickable(enabled = nameField.text.isNotBlank()) { onSave(nameField.text, colorV, iconV); onBack() }
        ) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                SaveIcon(if (nameField.text.isBlank()) AppColors.Subtle else Color(0xFF333333))
                Spacer(Modifier.width(16.dp))
                Text("保存", color = if (nameField.text.isBlank()) AppColors.Subtle else Color.Unspecified)
            }
        }
        if (canDelete) {
            Spacer(Modifier.height(12.dp))
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFBE9E7)),
                modifier = Modifier.clickable { showDeleteConfirm = true }
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Delete, null, tint = AppColors.Red)
                    Spacer(Modifier.width(16.dp))
                    Text("删除", color = AppColors.Red)
                }
            }
        }
    }

    if (showColorPicker) ColorPickerDialog({ showColorPicker = false }) { colorV = it }
    if (showIconPicker) IconPickerDialog({ showIconPicker = false }) { iconV = it }
    if (showDiscard) ConfirmDialog("是否放弃修改？", { showDiscard = false }, { onBack() })
    if (showDeleteConfirm) ConfirmDialog("确认删除？", { showDeleteConfirm = false }, { onDelete(); onBack() })
}

// ===================== 过滤器编辑器 =====================
@Composable
fun FilterEditorScreen(filterId: String?, onBack: () -> Unit) {
    Store.version.value
    val existing = filterId?.let { id -> Store.filters.firstOrNull { it.id == id } }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var color by remember { mutableStateOf(existing?.color ?: -1) }
    var icon by remember { mutableStateOf(existing?.icon ?: "") }
    var conditions by remember {
        mutableStateOf<MutableList<FilterCondition>>(existing?.conditions?.toMutableList() ?: mutableListOf())
    }
    var showColorPicker by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    var showCondMenu by remember { mutableStateOf(false) }
    var showPriorityMenu by remember { mutableStateOf(false) }
    var showListMenu by remember { mutableStateOf(false) }
    var showTagMenu by remember { mutableStateOf(false) }
    var showPresetMenu by remember { mutableStateOf(filterId == null) }
    var showDiscard by remember { mutableStateOf(false) }
    val dirty = name != (existing?.name ?: "") || color != (existing?.color ?: -1) ||
        icon != (existing?.icon ?: "") || conditions != (existing?.conditions?.toMutableList() ?: mutableListOf<FilterCondition>())

    fun save() {
        val f = existing ?: Filter(id = UUID.randomUUID().toString(), name = name)
        f.name = name; f.color = color; f.icon = icon; f.conditions = conditions
        if (existing == null) Store.addFilter(f) else Store.save()
        onBack()
    }
    BackHandler { if (dirty) showDiscard = true else onBack() }

    val barColor = if (color != -1) Color(color) else Color(0xFF35618E)
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().background(barColor).statusBarsPadding().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { if (name.isNotBlank()) save() else onBack() }) { SaveIcon(Color.White) }
            Text(if (existing == null) "新建过滤器" else "编辑过滤器", color = Color.White, fontSize = 20.sp, modifier = Modifier.padding(start = 8.dp))
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {}) { Icon(Icons.Filled.Info, null, tint = Color.White) }
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text("显示名称", fontSize = 13.sp, color = AppColors.Subtle)
            TextField(
                value = name, onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
            Row(
                Modifier.fillMaxWidth().clickable { showColorPicker = true }.padding(vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (color != -1) Box(Modifier.size(26.dp).clip(CircleShape).background(Color(color)))
                else Text("⊘", fontSize = 22.sp, color = AppColors.Subtle)
                Spacer(Modifier.width(16.dp))
                Text("颜色", Modifier.weight(1f))
                if (color != -1) Icon(Icons.Filled.Close, null, Modifier.clickable { color = -1 }, tint = AppColors.Subtle)
            }
            Row(
                Modifier.fillMaxWidth().clickable { showIconPicker = true }.padding(vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val iv = iconByKey(icon)
                if (iv != null) Icon(iv, null) else FilterIcon(AppColors.Subtle)
                Spacer(Modifier.width(16.dp))
                Text("图标")
            }
            Spacer(Modifier.height(12.dp))
            Text("过滤条件", fontSize = 13.sp, color = AppColors.Subtle)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                Spacer(Modifier.width(40.dp))
                Text("我的任务", Modifier.weight(1f), fontSize = 16.sp)
                Text("${Store.countFor(ViewRef.MyTasks)}", color = AppColors.Subtle)
            }
            conditions.forEachIndexed { idx, cond ->
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Add, null, tint = AppColors.Subtle)
                    Spacer(Modifier.width(16.dp))
                    Text(condLabel(cond), Modifier.weight(1f), fontSize = 16.sp)
                    Text("${Store.tasks.count { t -> Store.matchesFilter(t, Filter(name = "", conditions = mutableListOf(cond))) }}", color = AppColors.Subtle)
                    Icon(Icons.Filled.Close, null, Modifier.clickable { conditions = conditions.toMutableList().also { it.removeAt(idx) } }, tint = AppColors.Subtle)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = { showCondMenu = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF455A64)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("添加条件")
                }
            }
        }
    }

    if (showPresetMenu) {
        AlertDialog(onDismissRequest = { showPresetMenu = false }, confirmButton = {}, dismissButton = {
            TextButton(onClick = { showPresetMenu = false }) { Text("取消") }
        }, title = null, text = {
            Column {
                listOf(
                    "自定义…" to "",
                    "已过期" to "overdue", "仅今日" to "due_today", "明天" to "due_tomorrow",
                    "今日以后" to "due_after_today", "任意开始日期" to "any_start", "无开始日期" to "no_start",
                    "任何截止日期" to "any_due", "无截止日期" to "no_due", "无标签" to "no_tag",
                    "高优先级" to "priority:3", "中等优先级" to "priority:2", "低优先级" to "priority:1"
                ).forEach { (label, code) ->
                    Text(label, fontSize = 17.sp, modifier = Modifier.fillMaxWidth().clickable {
                        if (code.isNotEmpty()) {
                            val parts = code.split(":")
                            val t = parts[0]; val p = if (parts.size > 1) parts[1] else ""
                            conditions = mutableListOf(FilterCondition(t, p))
                        }
                        showPresetMenu = false
                    }.padding(vertical = 14.dp))
                }
            }
        })
    }
    if (showCondMenu) {
        AlertDialog(onDismissRequest = { showCondMenu = false }, confirmButton = {}, title = null, text = {
            Column {
                val items = listOf(
                    "标签…" to "tag", "标签名包含…" to "tag_name", "开始于…" to "any_start",
                    "截止于…" to "any_due", "优先级…" to "priority", "标题含…" to "title",
                    "在某清单中…" to "list", "重复" to "repeating", "已完成" to "completed",
                    "尚未开始" to "not_started", "有子任务" to "has_subtasks",
                    "是子任务" to "is_subtask", "有提醒" to "has_reminder"
                )
                items.forEach { (label, type) ->
                    Text(label, fontSize = 16.sp, modifier = Modifier.fillMaxWidth().clickable {
                        showCondMenu = false
                        when (type) {
                            "priority" -> showPriorityMenu = true
                            "list" -> showListMenu = true
                            "tag" -> showTagMenu = true
                            else -> conditions = (conditions + FilterCondition(type)).toMutableList()
                        }
                    }.padding(vertical = 12.dp))
                }
            }
        })
    }
    if (showPriorityMenu) {
        AlertDialog(onDismissRequest = { showPriorityMenu = false }, confirmButton = {}, title = { Text("优先级…") }, text = {
            Column {
                listOf("!!!" to "3", "!!" to "2", "!" to "1", "o" to "0").forEach { (label, p) ->
                    Text(label, fontSize = 17.sp, modifier = Modifier.fillMaxWidth().clickable {
                        conditions = (conditions + FilterCondition("priority", p)).toMutableList(); showPriorityMenu = false
                    }.padding(vertical = 14.dp))
                }
            }
        })
    }
    if (showListMenu) ListPickerDialog({ showListMenu = false }, { id ->
        conditions = (conditions + FilterCondition("list", id)).toMutableList(); showListMenu = false
    })
    if (showTagMenu) TagSelectDialog({ showTagMenu = false }, { id ->
        conditions = (conditions + FilterCondition("tag", id)).toMutableList(); showTagMenu = false
    })
    if (showColorPicker) ColorPickerDialog({ showColorPicker = false }) { color = it }
    if (showIconPicker) IconPickerDialog({ showIconPicker = false }) { icon = it }
    if (showDiscard) ConfirmDialog("是否放弃修改？", { showDiscard = false }, { onBack() })
}

private fun condLabel(c: FilterCondition): String = when (c.type) {
    "tag" -> "标签：${Store.tagById(c.param)?.name ?: ""}"
    "tag_name" -> "标签名包含"
    "priority" -> {
        val n = c.param.toIntOrNull() ?: 0
        "最低的优先级" + (if (n <= 0) "o" else "!".repeat(n))
    }
    "title" -> "标题含"
    "list" -> "在清单：${Store.listName(c.param)}"
    "repeating" -> "重复"
    "completed" -> "已完成"
    "not_started" -> "尚未开始"
    "has_subtasks" -> "有子任务"
    "is_subtask" -> "是子任务"
    "has_reminder" -> "有提醒"
    "overdue" -> "已过期"
    "due_today" -> "仅今日"
    "due_tomorrow" -> "明天"
    "due_after_today" -> "今日以后"
    "any_start" -> "任意开始日期"
    "no_start" -> "无开始日期"
    "any_due" -> "任何截止日期"
    "no_due" -> "无截止日期"
    "no_tag" -> "无标签"
    else -> c.type
}

// ===================== 地点选择页（地图） =====================
@Composable
fun PlacePickerScreen(
    showExisting: Boolean,
    onPick: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth().background(Color(0xFFE8E4E6))) {
            Column(Modifier.fillMaxSize()) {
                repeat(24) {
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        repeat(8) { Box(Modifier.weight(1f).fillMaxHeight().border(0.5.dp, Color(0xFFD8D4D6))) }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(14.dp).clip(RoundedCornerShape(12.dp))
                    .background(Color.White).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Search, null, tint = AppColors.Subtle)
                Spacer(Modifier.width(12.dp))
                Text("搜索", color = AppColors.Subtle, fontSize = 16.sp)
            }
            Column(
                Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier.size(34.dp).clip(CircleShape).background(AppColors.Red)
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) { Box(Modifier.size(12.dp).clip(CircleShape).background(Color(0xFF7F0000))) }
                Box(Modifier.width(3.dp).height(16.dp).background(AppColors.Red))
            }
            FloatingActionButton(
                onClick = {}, containerColor = AppColors.Blue, shape = CircleShape,
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
            ) { TargetIcon(Color.White) }
            Text(
                "© OpenStreetMap contributors",
                Modifier.align(Alignment.BottomStart).padding(6.dp),
                fontSize = 12.sp, color = Color(0xFF555555)
            )
        }
        Column(Modifier.fillMaxWidth().background(Color.White).padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth().clickable {
                    val p = Store.addPlace("0.0 0.0")
                    onPick(p.id)
                }.padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Place, null, tint = AppColors.Subtle)
                Spacer(Modifier.width(16.dp))
                Text("选择此位置", fontSize = 16.sp)
            }
            if (showExisting && Store.places.isNotEmpty()) {
                Text("或选择一个位置", color = AppColors.Subtle, fontSize = 14.sp)
                Store.places.forEach { p ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onPick(p.id) }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(p.name, Modifier.weight(1f), fontSize = 16.sp)
                        Icon(Icons.Filled.Edit, null, tint = AppColors.Subtle)
                    }
                }
            }
        }
    }
}

// ===================== 添加账号 =====================
@Composable
fun AddAccountScreen(onBack: () -> Unit, onCalDav: () -> Unit) {
    data class Provider(val name: String, val desc: String, val color: Color, val badge: String)
    val providers = listOf(
        Provider("Tasks.org Cloud", "包括好友和家庭共享、邮件转任务", AppColors.Blue, "✓"),
        Provider("Microsoft To Do", "与您的个人 Microsoft 账号同步", Color(0xFF2B6CB0), "✓"),
        Provider("Google Tasks", "基本服务，用您的 Google 账号进行数据同步", Color(0xFFEA4335), "G"),
        Provider("DAVx⁵", "使用 DAVx⁵ 应用同步您的任务", Color(0xFF8BC34A), "D"),
        Provider("CalDAV", "基于开放的互联网标准的同步", Color(0xFF424242), "DAV"),
        Provider("EteSync", "端到端加密的同步", Color(0xFFFBC02D), "E"),
        Provider("DecSync CC", "基于文件的同步", Color(0xFF0277BD), "D")
    )
    Column(Modifier.fillMaxSize().background(AppColors.DrawerBg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) }
            Text("添加账号", fontSize = 22.sp, modifier = Modifier.padding(start = 8.dp))
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
            providers.forEach { p ->
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp)
                        .clickable { if (p.name == "CalDAV") onCalDav() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(44.dp).clip(CircleShape).background(p.color),
                            contentAlignment = Alignment.Center
                        ) { Text(p.badge, color = Color.White, fontSize = 16.sp) }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(p.name, fontSize = 17.sp)
                            Text(p.desc, fontSize = 13.sp, color = AppColors.Subtle)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalDavScreen(onBack: () -> Unit) {
    var url by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var storeMeta by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(AppColors.DrawerBg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) }
            Text("添加账号", fontSize = 20.sp, modifier = Modifier.padding(start = 8.dp))
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp)) {
                    Text("网址", fontSize = 13.sp, color = AppColors.Subtle)
                    TextField(
                        value = url, onValueChange = { url = it }, modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://example.com/dav") }, singleLine = true,
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp)) {
                    Text("用户名", fontSize = 13.sp, color = AppColors.Subtle)
                    TextField(
                        value = user, onValueChange = { user = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp)) {
                    Text("密码", fontSize = 13.sp, color = AppColors.Subtle)
                    TextField(
                        value = pass, onValueChange = { pass = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp)) {
                    Text("服务器类型", fontSize = 13.sp, color = AppColors.Subtle)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("未知", Modifier.weight(1f), fontSize = 16.sp)
                        Icon(Icons.Filled.ArrowDropDown, null)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("在此存储标签元数据", fontSize = 16.sp)
                        Text("存储此账户的标签名、颜色、图标和展示顺序以便跨设备同步", fontSize = 13.sp, color = AppColors.Subtle)
                    }
                    Switch(checked = storeMeta, onCheckedChange = { storeMeta = it })
                }
            }
            Spacer(Modifier.height(10.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.clickable(enabled = url.isNotBlank() && user.isNotBlank()) { }
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.ExitToApp, null, tint = if (url.isBlank() || user.isBlank()) AppColors.Subtle else Color.Unspecified)
                    Spacer(Modifier.width(12.dp))
                    Text("登录", color = if (url.isBlank() || user.isBlank()) AppColors.Subtle else Color.Unspecified)
                }
            }
        }
    }
}
