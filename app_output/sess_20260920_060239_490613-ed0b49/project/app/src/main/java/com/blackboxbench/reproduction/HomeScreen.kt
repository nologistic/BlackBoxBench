package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TaskHome(
    tasks: List<AppTask>,
    lists: List<TaskList>,
    filters: List<FilterSpec>,
    title: String,
    scopeType: String,
    scopeId: String,
    hideCompleted: Boolean,
    onHideCompleted: (Boolean) -> Unit,
    onScope: (String, String, String) -> Unit,
    onAddTask: () -> Unit,
    onEditTask: (String) -> Unit,
    onToggleTask: (String) -> Unit,
    onToggleSubtask: (String, String) -> Unit,
    onClearCompleted: () -> Unit,
    onSettings: () -> Unit,
    onNewList: () -> Unit,
    onNewFilter: () -> Unit,
    onNewLocation: () -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutine = rememberCoroutineScope()
    var searchMode by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var sortSheet by remember { mutableStateOf(false) }
    var groupMode by remember { mutableStateOf("截止日期") }
    var showOverflow by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf(true) }
    val expanded = remember { mutableStateListOf<String>() }
    val selected = remember { mutableStateListOf<String>() }

    val scoped = tasks.filter { task ->
        val scopeMatch = when (scopeType) {
            "list" -> task.listId == scopeId
            "tag" -> task.tags.contains(scopeId)
            "filter" -> task.priority == "high"
            "today" -> task.due.startsWith("2026-09-20")
            else -> true
        }
        val searchMatch = query.isBlank() || task.title.contains(query, true) || task.notes.contains(query, true)
        scopeMatch && searchMatch && (!hideCompleted || !task.completed)
    }

    val groups: List<Pair<String, List<AppTask>>> = if (groupMode == "优先级") {
        listOf(
            "高优先级" to scoped.filter { it.priority == "high" },
            "中等优先级" to scoped.filter { it.priority == "medium" },
            "低优先级" to scoped.filter { it.priority == "low" },
            "无优先级" to scoped.filter { it.priority == "none" }
        ).filter { it.second.isNotEmpty() }
    } else {
        scoped.sortedBy { if (it.due.isBlank()) "9999" else it.due }
            .groupBy { dueLabel(it.due) }.map { it.key to it.value }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(.87f),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                DrawerContent(
                    tasks = tasks,
                    lists = lists,
                    filters = filters,
                    activeTitle = title,
                    onPick = { type, id, label ->
                        onScope(type, id, label)
                        coroutine.launch { drawerState.close() }
                    },
                    onNewFilter = { coroutine.launch { drawerState.close() }; onNewFilter() },
                    onNewList = { coroutine.launch { drawerState.close() }; onNewList() },
                    onNewLocation = { coroutine.launch { drawerState.close() }; onNewLocation() }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                when {
                    selected.isNotEmpty() -> {
                        Row(
                            Modifier.statusBarsPadding().fillMaxWidth().height(72.dp).padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { selected.clear() }) { Text("‹", fontSize = 40.sp) }
                            Text(selected.size.toString(), fontSize = 26.sp, modifier = Modifier.weight(1f))
                            listOf("◇", "☷", "◷", "⚑", "⋮").forEach { symbol ->
                                Text(symbol, fontSize = 27.sp, modifier = Modifier.padding(10.dp))
                            }
                        }
                    }
                    searchMode -> {
                        Row(
                            Modifier.statusBarsPadding().fillMaxWidth().height(72.dp).padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { searchMode = false; query = "" }) { Text("‹", fontSize = 40.sp) }
                            BasicTextField(
                                value = query,
                                onValueChange = { query = it },
                                textStyle = TextStyle(fontSize = 24.sp, color = MaterialTheme.colorScheme.onBackground),
                                singleLine = true,
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                            )
                            if (query.isNotBlank()) Text("×", fontSize = 32.sp, modifier = Modifier.clickable { query = "" }.padding(8.dp))
                            Text("⚙", fontSize = 28.sp, modifier = Modifier.clickable(onClick = onSettings).padding(8.dp))
                        }
                    }
                    else -> {
                        Row(
                            Modifier.statusBarsPadding().fillMaxWidth().height(82.dp).padding(start = 16.dp, end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(title, color = Color(0xFF2196F3), fontSize = 32.sp, modifier = Modifier.weight(1f))
                            Text("⚙", fontSize = 31.sp, modifier = Modifier.clickable(onClick = onSettings).padding(8.dp))
                        }
                    }
                }
            },
            bottomBar = {
                Row(
                    Modifier.navigationBarsPadding().fillMaxWidth().height(84.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("☰", fontSize = 31.sp, modifier = Modifier.clickable { coroutine.launch { drawerState.open() } }.padding(8.dp))
                    Text("⌕", fontSize = 34.sp, modifier = Modifier.clickable { searchMode = true }.padding(8.dp))
                    Text("↕", fontSize = 30.sp, modifier = Modifier.clickable { sortSheet = true }.padding(8.dp))
                    Text("♩", fontSize = 30.sp, modifier = Modifier.padding(8.dp))
                    Box {
                        Text("⋮", fontSize = 34.sp, modifier = Modifier.clickable { showOverflow = true }.padding(8.dp))
                        DropdownMenu(expanded = showOverflow, onDismissRequest = { showOverflow = false }) {
                            DropdownMenuItem(text = { Text("清除已完成项") }, onClick = { showOverflow = false; onClearCompleted() })
                            DropdownMenuItem(text = { Text("收起子任务") }, onClick = { expanded.clear(); showOverflow = false })
                            DropdownMenuItem(text = { Text("展开子任务") }, onClick = {
                                expanded.addAll(scoped.map { it.id }.filterNot(expanded::contains)); showOverflow = false
                            })
                            DropdownMenuItem(text = { Text("分享") }, onClick = { showOverflow = false })
                        }
                    }
                    FloatingActionButton(
                        onClick = onAddTask,
                        containerColor = Color(0xFF2196F3),
                        contentColor = Color.Black,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.size(58.dp)
                    ) { Text("+", fontSize = 36.sp, fontWeight = FontWeight.Light) }
                }
            }
        ) { padding ->
            if (scoped.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        EmptyTray()
                        Spacer(Modifier.height(22.dp))
                        Text("这里没有任务哦。", fontSize = 25.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .72f))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(bottom = 18.dp)
                ) {
                    if (notice) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(28.dp),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp).fillMaxWidth()
                            ) {
                                Column(Modifier.padding(24.dp)) {
                                    Text("在正确的时间收到通知", fontSize = 26.sp)
                                    Spacer(Modifier.height(14.dp))
                                    Text("确保你在正确的时间收到通知，在设置中授予闹钟和提醒的权限", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .78f))
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        TextButton(onClick = { notice = false }) { Text("关闭") }
                                        Button(onClick = onSettings) { Text("设置") }
                                    }
                                }
                            }
                        }
                    }
                    groups.forEach { pair ->
                        item {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(pair.first, fontSize = 18.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                Text("⌃", fontSize = 24.sp)
                            }
                        }
                        items(pair.second, key = { it.id }) { task ->
                            TaskRow(
                                task = task,
                                listName = lists.firstOrNull { it.id == task.listId }?.name ?: "默认清单",
                                expanded = expanded.contains(task.id) || task.subtasks.isNotEmpty(),
                                selected = selected.contains(task.id),
                                onClick = { onEditTask(task.id) },
                                onLongClick = { if (!selected.contains(task.id)) selected.add(task.id) },
                                onToggle = { onToggleTask(task.id) },
                                onExpand = { if (expanded.contains(task.id)) expanded.remove(task.id) else expanded.add(task.id) },
                                onToggleSub = { onToggleSubtask(task.id, it) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (sortSheet) {
        ModalBottomSheet(onDismissRequest = { sortSheet = false }) {
            Column(Modifier.padding(horizontal = 18.dp).padding(bottom = 26.dp)) {
                Row(
                    Modifier.fillMaxWidth().clickable { groupMode = if (groupMode == "截止日期") "优先级" else "截止日期" }.padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("◉", fontSize = 26.sp, modifier = Modifier.width(48.dp))
                    Column(Modifier.weight(1f)) { Text("分组", fontSize = 21.sp); Text("按" + groupMode, fontSize = 16.sp) }
                    Text("↑ 升序", fontSize = 18.sp)
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("↕", fontSize = 26.sp, modifier = Modifier.width(48.dp))
                    Column(Modifier.weight(1f)) { Text("排序", fontSize = 21.sp); Text("按截止日期", fontSize = 16.sp) }
                    Text("↑ 升序", fontSize = 18.sp)
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
                    Text("↳", fontSize = 26.sp, modifier = Modifier.width(48.dp))
                    Column { Text("子任务", fontSize = 21.sp); Text("我的顺序", fontSize = 16.sp) }
                }
                SettingSwitch("显示未开始项目", true) {}
                SettingSwitch("显示已完成任务", !hideCompleted) { onHideCompleted(!it) }
                SettingSwitch("显示已完成的子任务", true) {}
                SettingSwitch("将已完成任务移至底部", true) {}
            }
        }
    }
}

@Composable
private fun DrawerContent(
    tasks: List<AppTask>,
    lists: List<TaskList>,
    filters: List<FilterSpec>,
    activeTitle: String,
    onPick: (String, String, String) -> Unit,
    onNewFilter: () -> Unit,
    onNewList: () -> Unit,
    onNewLocation: () -> Unit
) {
    Column(Modifier.fillMaxHeight().statusBarsPadding().navigationBarsPadding().padding(8.dp)) {
        DrawerItem("▣", "我的任务", activeTitle == "我的任务") { onPick("all", "", "我的任务") }
        DrawerSection("≡", "过滤器", onAdd = onNewFilter) {
            DrawerItem("▣", "今天", activeTitle == "今天") { onPick("today", "", "今天") }
            DrawerItem("◴", "最近修改过的", activeTitle == "最近修改过的", tasks.size.toString()) { onPick("recent", "", "最近修改过的") }
            filters.forEach { filter ->
                DrawerItem("≡", filter.name, activeTitle == filter.name) { onPick("filter", filter.name, filter.name) }
            }
        }
        DrawerSection("◇", "标签", onAdd = {}) {
            tasks.flatMap { it.tags }.distinct().sorted().take(4).forEach { tag ->
                DrawerItem("◇", tag, activeTitle == tag) { onPick("tag", tag, tag) }
            }
        }
        DrawerSection("⌖", "地点", onAdd = onNewLocation) { }
        DrawerSection("⌁", "本地清单", onAdd = onNewList) {
            lists.forEach { list ->
                DrawerItem("☷", list.name, activeTitle == list.name, tasks.count { it.listId == list.id && !it.completed }.toString()) {
                    onPick("list", list.id, list.name)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            FloatingActionButton(onClick = { onPick("all", "", "我的任务") }, modifier = Modifier.size(58.dp)) { Text("⌕", fontSize = 28.sp) }
        }
    }
}

@Composable
private fun DrawerSection(icon: String, title: String, onAdd: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
    ) {
        Column(Modifier.padding(vertical = 4.dp)) {
            Row(Modifier.fillMaxWidth().height(50.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 24.sp, modifier = Modifier.width(40.dp))
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Text("⌃", fontSize = 21.sp, modifier = Modifier.padding(8.dp))
                Text("+", fontSize = 30.sp, modifier = Modifier.clickable(onClick = onAdd).padding(8.dp))
            }
            content()
        }
    }
}

@Composable
private fun DrawerItem(icon: String, label: String, selected: Boolean, count: String = "", onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(if (selected) MaterialTheme.colorScheme.onSurface.copy(alpha = .08f) else Color.Transparent)
            .clickable(onClick = onClick).height(50.dp).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 22.sp, modifier = Modifier.width(40.dp))
        Text(label, fontSize = 19.sp, modifier = Modifier.weight(1f))
        if (count.isNotBlank() && count != "0") Text(count, fontSize = 18.sp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskRow(
    task: AppTask,
    listName: String,
    expanded: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggle: () -> Unit,
    onExpand: () -> Unit,
    onToggleSub: (String) -> Unit
) {
    Column(
        Modifier.fillMaxWidth()
            .background(if (selected) MaterialTheme.colorScheme.onSurface.copy(alpha = .18f) else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            if (task.repeat.isNotBlank()) {
                Text("↔", color = if (task.priority == "high") Color(0xFFF44336) else Color(0xFF2196F3), fontSize = 30.sp, modifier = Modifier.clickable(onClick = onToggle).padding(top = 3.dp, end = 12.dp))
            } else {
                Checkbox(checked = task.completed, onCheckedChange = { onToggle() }, modifier = Modifier.size(44.dp))
            }
            Column(Modifier.weight(1f).padding(top = 5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        task.title,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (task.completed) .45f else 1f),
                        textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                        modifier = Modifier.weight(1f)
                    )
                    if (task.due.isNotBlank()) Text(dueLabel(task.due), fontSize = 15.sp)
                }
                if (task.notes.isNotBlank()) Text(task.notes, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .65f), maxLines = 1)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (task.subtasks.isNotEmpty()) SmallChip("⌃ " + task.subtasks.count { !it.completed }, onExpand)
                    SmallChip("☷ " + listName)
                    task.tags.take(2).forEach { SmallChip("◇ " + it) }
                }
            }
        }
        if (expanded && task.subtasks.isNotEmpty()) {
            task.subtasks.forEach { sub ->
                Row(Modifier.fillMaxWidth().padding(start = 42.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = sub.completed, onCheckedChange = { onToggleSub(sub.id) })
                    Text(
                        sub.title,
                        fontSize = 18.sp,
                        textDecoration = if (sub.completed) TextDecoration.LineThrough else TextDecoration.None,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (sub.completed) .45f else 1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SmallChip(text: String, onClick: () -> Unit = {}) {
    Surface(
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .10f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.padding(end = 5.dp).clickable(onClick = onClick)
    ) { Text(text, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) }
}

@Composable
private fun EmptyTray() {
    Canvas(Modifier.size(132.dp)) {
        val c = Color.Gray.copy(alpha = .62f)
        drawRect(c, topLeft = androidx.compose.ui.geometry.Offset(size.width * .14f, size.height * .18f), size = androidx.compose.ui.geometry.Size(size.width * .72f, size.height * .64f), style = Stroke(width = 14f))
        drawArc(c, 0f, 180f, false, topLeft = androidx.compose.ui.geometry.Offset(size.width * .35f, size.height * .43f), size = androidx.compose.ui.geometry.Size(size.width * .3f, size.height * .3f), style = Stroke(width = 14f))
    }
}
