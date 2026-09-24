package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ------------------------------------------------------------------ list screen

@Composable
fun TaskListScreen(
    viewId: String,
    onOpenDrawer: () -> Unit,
    onOpenEditor: (String) -> Unit,
    onNewTask: () -> Unit,
    onAppSettings: () -> Unit,
    onViewSettings: (String) -> Unit,
    onBack: (() -> Unit)? = null,
) {
    var revision by remember { mutableStateOf(0) }
    var showGearMenu by remember { mutableStateOf(false) }
    var searchMode by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var showSort by remember { mutableStateOf(false) }
    var showOverflow by remember { mutableStateOf(false) }
    var collapsed by remember { mutableStateOf(setOf<String>()) }
    var selection by remember { mutableStateOf(setOf<String>()) }
    var selectMode by remember { mutableStateOf(false) }
    var showSelectMenu by remember { mutableStateOf(false) }

    val title = Store.viewTitle(viewId)
    val color = Color(Store.viewColor(viewId))
    val all = Store.tasksFor(viewId)
    val filtered = if (searchMode && query.isNotBlank())
        all.filter { it.title.contains(query, true) || it.notes.contains(query, true) }
    else all
    val visible = if (Store.showCompleted) filtered else filtered.filter { !it.completed }

    fun toggle(id: String) {
        val t = Store.tasks.firstOrNull { it.id == id } ?: return
        if (!t.completed) {
            t.completedMillis = System.currentTimeMillis()
            if (t.repeat != REPEAT_NONE) t.advanceRepeat()
            Store.save()
        } else {
            t.completedMillis = null
            Store.save()
        }
        revision++
    }

    Box(Modifier.fillMaxSize().background(C.screenBg)) {
        Column(Modifier.fillMaxSize()) {
            if (selectMode) {
                SelectTopBar(
                    count = selection.size,
                    onBack = { selectMode = false; selection = emptySet() },
                    onOverflow = { showSelectMenu = true },
                    onAction = { }
                )
            } else {
                Row(
                    Modifier.fillMaxWidth().height(84.dp).padding(start = 16.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBack != null) {
                        Box(Modifier.size(38.dp).clickable { onBack() }, contentAlignment = Alignment.Center) {
                            DrawIcon("back", C.text, 22.dp, 2.dp)
                        }
                    }
                    Text(
                        title, color = color, fontSize = 24.sp, fontWeight = FontWeight.Medium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                    )
                    Box(Modifier.size(42.dp).clickable { showGearMenu = true }, contentAlignment = Alignment.Center) {
                        DrawIcon("gear", C.text, 24.dp, 1.8.dp)
                    }
                }
            }

            if (searchMode) {
                Row(
                    Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DrawIcon("search", C.textDim, 22.dp)
                    Spacer(Modifier.width(12.dp))
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) Text("搜索", color = C.textFaint, fontSize = 17.sp)
                        AppTextField(
                            value = query, onValueChange = { query = it },
                            fontSize = 17f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (query.isNotEmpty()) {
                        Box(Modifier.size(34.dp).clickable { query = "" }, contentAlignment = Alignment.Center) {
                            DrawIcon("close", C.textDim, 20.dp)
                        }
                    }
                    Box(Modifier.size(34.dp).clickable { }, contentAlignment = Alignment.Center) {
                        DrawIcon("gear", C.textDim, 22.dp)
                    }
                }
                HLine()
            }

            if (!Store.remindersBannerDismissed && !searchMode) {
                ReminderBanner(
                    onDismiss = { Store.remindersBannerDismissed = true; Store.save(); revision++ },
                    onSettings = { }
                )
            }

            Box(Modifier.weight(1f)) {
                if (visible.isEmpty()) {
                    EmptyState()
                } else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        val groups = groupTasks(visible)
                        groups.forEach { (label, tasks) ->
                            item {
                                val isCollapsed = collapsed.contains(label)
                                Row(
                                    Modifier.fillMaxWidth().height(48.dp)
                                        .clickable {
                                            collapsed = if (isCollapsed) collapsed - label else collapsed + label
                                        }
                                        .padding(start = 16.dp, end = 20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        label, color = C.text, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    DrawIcon(
                                        if (isCollapsed) "chevron_down" else "chevron_up",
                                        C.text, 20.dp, 1.6.dp
                                    )
                                }
                            }
                            if (!collapsed.contains(label)) {
                                items(tasks) { task ->
                                    TaskRow(
                                        task = task,
                                        selected = selection.contains(task.id),
                                        onClick = {
                                            if (selectMode) {
                                                selection = if (selection.contains(task.id))
                                                    selection - task.id else selection + task.id
                                            } else onOpenEditor(task.id)
                                        },
                                        onLongClick = {
                                            selectMode = true
                                            selection = selection + task.id
                                        },
                                        onToggle = { toggle(task.id) },
                                        onToggleSubtask = { sub ->
                                            sub.completed = !sub.completed
                                            Store.save(); revision++
                                        }
                                    )
                                }
                            }
                        }
                        item { Spacer(Modifier.height(20.dp)) }
                    }
                }
            }

            BottomBar(
                onDrawer = onOpenDrawer,
                onSearch = { searchMode = !searchMode },
                onSort = { showSort = true },
                onOverflow = { showOverflow = true },
                fabColor = color,
                onFab = onNewTask
            )
        }

        if (showGearMenu) {
            PopupCard(onDismiss = { showGearMenu = false }) {
                PopupItem("应用设置") { showGearMenu = false; onAppSettings() }
                if (viewId.startsWith("list_") || viewId.startsWith("tag_")) {
                    PopupItem(if (viewId.startsWith("list_")) "清单设置" else "标签设置") {
                        showGearMenu = false; onViewSettings(viewId)
                    }
                }
            }
        }

        if (showSort) {
            SortPanel(onDismiss = { showSort = false }, onChanged = { revision++ })
        }
        if (showOverflow) {
            OverflowPanel(
                onDismiss = { showOverflow = false },
                onClearCompleted = {
                    Store.tasks.removeAll { it.completed }
                    Store.save(); revision++; showOverflow = false
                }
            )
        }
        if (showSelectMenu) {
            SelectMenuPanel(
                onDismiss = { showSelectMenu = false },
                onSelectAll = { selection = visible.map { it.id }.toSet(); showSelectMenu = false },
                onClone = {
                    selection.forEach { id ->
                        val t = Store.tasks.firstOrNull { it.id == id } ?: return@forEach
                        val copy = t.duplicate(Store.nextTaskId(), t.title + " (副本)")
                        Store.tasks.add(copy)
                    }
                    Store.save(); revision++; showSelectMenu = false
                    selectMode = false; selection = emptySet()
                },
                onDelete = {
                    Store.tasks.removeAll { selection.contains(it.id) }
                    Store.save(); revision++; showSelectMenu = false
                    selectMode = false; selection = emptySet()
                }
            )
        }
    }
}

private fun groupTasks(list: List<TaskItem>): List<Pair<String, List<TaskItem>>> {
    val sorted = when (Store.sortBy) {
        "priority" -> list.sortedByDescending { it.priority }
        "title" -> list.sortedBy { it.title }
        else -> list.sortedWith(compareBy({ it.dueMillis ?: Long.MAX_VALUE }, { it.order }))
    }
    if (Store.groupBy == "none") return listOf("" to sorted)
    val completed = sorted.filter { it.completed }
    val open = sorted.filter { !it.completed }
    val groups = LinkedHashMap<String, MutableList<TaskItem>>()
    open.forEach { t ->
        val key = Dates.groupLabel(t.due())
        groups.getOrPut(key) { mutableListOf() }.add(t)
    }
    if (completed.isNotEmpty()) groups["已完成"] = completed.toMutableList()
    return groups.map { it.key to it.value.toList() }
}

@Composable
fun ReminderBanner(onDismiss: () -> Unit, onSettings: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(16.dp)
            .clip(RoundedCornerShape(16.dp)).background(C.banner)
            .padding(start = 26.dp, end = 20.dp, top = 24.dp, bottom = 18.dp)
    ) {
        Text("启用提醒", color = C.text, fontSize = 21.sp)
        Spacer(Modifier.height(14.dp))
        Text("提醒在 Android 设置中被禁用", color = C.text, fontSize = 14.sp)
        Spacer(Modifier.height(20.dp))
        Row(
            Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "关闭", color = C.blueDark, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { onDismiss() }.padding(horizontal = 14.dp, vertical = 8.dp)
            )
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier.clip(RoundedCornerShape(18.dp)).background(C.blueDark)
                    .clickable { onSettings() }.padding(horizontal = 22.dp, vertical = 9.dp)
            ) { Text("设置", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium) }
        }
    }
}

@Composable
fun TaskRow(
    task: TaskItem,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggle: () -> Unit,
    onToggleSubtask: (Subtask) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth()
            .background(if (selected) C.rowSelected else Color.Transparent)
            .pointerInput(task.id) {
                detectTapGestures(onTap = { onClick() }, onLongPress = { onLongClick() })
            }
            .padding(start = 21.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                if (task.repeat != REPEAT_NONE) {
                    Box(Modifier.size(24.dp).clickable { onToggle() }, contentAlignment = Alignment.Center) {
                        DrawIcon("repeat", C.redSoft, 20.dp, 1.8.dp)
                    }
                } else {
                    CheckBoxView(
                        checked = task.completed,
                        color = if (task.priority > 0) priorityColor(task.priority) else Color(0xFF9E9E9E),
                        size = 20.dp, onToggle = onToggle
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        task.title,
                        color = if (task.completed) C.textFaint else C.text,
                        fontSize = 17.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = androidx.compose.ui.text.TextStyle(
                            textDecoration = if (task.completed) androidx.compose.ui.text.style.TextDecoration.LineThrough
                            else null
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    val due = task.due()
                    if (due != null && !(due.hour == 0 && due.minute == 0)) {
                        Spacer(Modifier.width(8.dp))
                        Text(Dates.timeLabel(due), color = C.textDim, fontSize = 14.sp)
                    }
                }
                if (task.notes.isNotBlank()) {
                    Text(
                        task.notes, color = C.textDim, fontSize = 15.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                val hasChips = task.subtasks.isNotEmpty() || task.tags.isNotEmpty() || Store.viewIdNeedsListChip(task)
                if (hasChips) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (task.subtasks.isNotEmpty()) {
                            Chip(
                                text = task.subtasks.size.toString(),
                                icon = if (expanded) "chevron_up" else "chevron_down",
                                onClick = { expanded = !expanded }
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        if (Store.viewIdNeedsListChip(task)) {
                            val list = Store.listById(task.listId)
                            if (list != null) {
                                Chip(
                                    text = list.name, bg = Color(list.color), fg = Color.White,
                                    icon = list.icon, iconTint = Color.White
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                        }
                        task.tags.forEach { tag ->
                            Chip(text = tag, icon = "tag")
                            Spacer(Modifier.width(8.dp))
                        }
                    }
                }
                if (expanded) {
                    task.subtasks.forEach { sub ->
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Spacer(Modifier.width(20.dp))
                            CheckBoxView(
                                checked = sub.completed, size = 18.dp,
                                color = if (sub.priority > 0) priorityColor(sub.priority) else Color(0xFF9E9E9E),
                                onToggle = { onToggleSubtask(sub) }
                            )
                            Spacer(Modifier.width(14.dp))
                            Text(
                                sub.title,
                                color = if (sub.completed) C.textFaint else C.text,
                                fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                style = androidx.compose.ui.text.TextStyle(
                                    textDecoration = if (sub.completed) androidx.compose.ui.text.style.TextDecoration.LineThrough
                                    else null
                                )
                            )
                        }
                    }
                }
            }
        }
    }
    HLine()
}

private fun Store.viewIdNeedsListChip(task: TaskItem): Boolean =
    currentViewId == "mytasks" || currentViewId.startsWith("tag_") ||
        currentViewId.startsWith("filter_")

// ------------------------------------------------------------------ bars

private val barCenters = listOf(31.dp, 83.dp, 138.dp, 193.dp, 227.dp)

@Composable
fun BottomBar(
    onDrawer: () -> Unit,
    onSearch: () -> Unit,
    onSort: () -> Unit,
    onOverflow: () -> Unit,
    fabColor: Color,
    onFab: () -> Unit,
    fabIcon: String = "plus",
) {
    val actions = listOf<() -> Unit>(onDrawer, onSearch, onSort, { }, onOverflow)
    val names = listOf("menu", "search", "sort", "mic", "more")
    Box(Modifier.fillMaxWidth().height(64.dp).background(C.barBg)) {
        names.forEachIndexed { i, name ->
            Box(
                Modifier.offset(x = barCenters[i] - 22.dp).size(44.dp)
                    .align(Alignment.CenterStart)
                    .clickable { actions[i]() },
                contentAlignment = Alignment.Center
            ) { DrawIcon(name, C.text, 24.dp, 1.8.dp) }
        }
        Box(
            Modifier.align(Alignment.CenterEnd).padding(end = 15.dp).size(56.dp)
                .clip(RoundedCornerShape(18.dp)).background(fabColor)
                .clickable { onFab() },
            contentAlignment = Alignment.Center
        ) { DrawIcon(fabIcon, Color.White, 26.dp, 2.4.dp) }
    }
}

@Composable
fun SelectTopBar(count: Int, onBack: () -> Unit, onOverflow: () -> Unit, onAction: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(84.dp).background(Color(0xFFEDEDF2)).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(44.dp).clickable { onBack() }, contentAlignment = Alignment.Center) {
            DrawIcon("back", C.text, 22.dp, 2.dp)
        }
        Text(count.toString(), color = C.text, fontSize = 20.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.weight(1f))
        listOf("tag", "list", "clock", "flag").forEach {
            Box(Modifier.size(44.dp).clickable { onAction(it) }, contentAlignment = Alignment.Center) {
                DrawIcon(it, C.text, 22.dp, 1.8.dp)
            }
        }
        Box(Modifier.size(44.dp).clickable { onOverflow() }, contentAlignment = Alignment.Center) {
            DrawIcon("more", C.text, 22.dp)
        }
    }
}

// ------------------------------------------------------------------ drawer

@Composable
fun AppDrawer(
    currentView: String,
    onSelect: (String) -> Unit,
    onAddFilter: () -> Unit,
    onAddTag: () -> Unit,
    onAddList: () -> Unit,
    onSettings: () -> Unit,
    onSearch: () -> Unit,
) {
    Column(
        Modifier.fillMaxHeight().width(348.dp).background(Color(0xFFF3F1F7))
            .padding(top = 8.dp, start = 8.dp, end = 8.dp)
    ) {
        DrawerCard {
            DrawerRow(
                icon = "list", label = "我的任务", count = Store.countsFor("mytasks"),
                selected = currentView == "mytasks", onClick = { onSelect("mytasks") }
            )
        }
        Spacer(Modifier.height(6.dp))
        DrawerCard {
            DrawerHeader("过滤器", "filter", onAdd = onAddFilter, onToggle = { })
            DrawerRow(
                icon = "calendar", label = "今天", count = Store.countsFor("filter_today"),
                selected = currentView == "filter_today", onClick = { onSelect("filter_today") }, indent = 26.dp
            )
            DrawerRow(
                icon = "clock", label = "最近修改过的", count = Store.countsFor("filter_recent"),
                selected = currentView == "filter_recent", onClick = { onSelect("filter_recent") }, indent = 26.dp
            )
            Store.filters.filter { !it.builtin }.forEach { f ->
                DrawerRow(
                    icon = "filter", label = f.name, count = Store.countsFor(f.id),
                    selected = currentView == f.id, onClick = { onSelect(f.id) }, indent = 26.dp
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        DrawerCard {
            DrawerHeader("标签", "tag", onAdd = onAddTag, onToggle = { })
            Store.allTags().forEach { tag ->
                DrawerRow(
                    icon = "tag", label = tag, count = Store.countsFor("tag_$tag"),
                    selected = currentView == "tag_$tag", onClick = { onSelect("tag_$tag") }, indent = 26.dp
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        DrawerCard {
            DrawerHeader("地点", "place", onAdd = { }, onToggle = null)
        }
        Spacer(Modifier.height(6.dp))
        DrawerCard {
            DrawerHeader("本地清单", "cottage", onAdd = onAddList, onToggle = { })
            Store.lists.forEach { list ->
                DrawerRow(
                    icon = list.icon, label = list.name, count = Store.countsFor(list.id),
                    iconTint = Color(list.color), selected = currentView == list.id,
                    onClick = { onSelect(list.id) }, indent = 26.dp
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Box(
                Modifier.padding(end = 18.dp, bottom = 18.dp).size(52.dp).clip(CircleShape)
                    .background(C.blueDark).clickable { onSearch() },
                contentAlignment = Alignment.Center
            ) { DrawIcon("search", Color.White, 26.dp, 2.dp) }
        }
    }
}

@Composable
private fun DrawerCard(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFFCFAFF))
    ) { content() }
}

@Composable
private fun DrawerHeader(
    title: String,
    icon: String,
    onAdd: () -> Unit,
    onToggle: (() -> Unit)?,
) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).padding(start = 16.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DrawIcon(icon, C.text, 22.dp, 1.8.dp)
        Spacer(Modifier.width(22.dp))
        Text(title, color = C.text, fontSize = 18.sp, modifier = Modifier.weight(1f))
        if (onToggle != null) {
            Box(Modifier.size(36.dp).clickable { onToggle() }, contentAlignment = Alignment.Center) {
                DrawIcon("chevron_up", C.text, 20.dp, 1.8.dp)
            }
        }
        Box(Modifier.size(36.dp).clickable { onAdd() }, contentAlignment = Alignment.Center) {
            DrawIcon("plus", C.text, 22.dp, 2.dp)
        }
    }
}

@Composable
private fun DrawerRow(
    icon: String,
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    iconTint: Color = C.text,
    indent: androidx.compose.ui.unit.Dp = 16.dp,
) {
    Row(
        Modifier.fillMaxWidth().height(50.dp)
            .background(if (selected) Color(0xFFDCDCE2) else Color.Transparent)
            .clickable { onClick() }
            .padding(start = indent, end = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DrawIcon(icon, iconTint, 22.dp, 1.8.dp)
        Spacer(Modifier.width(20.dp))
        Text(label, color = C.text, fontSize = 17.sp, modifier = Modifier.weight(1f))
        if (count > 0) Text(count.toString(), color = C.text, fontSize = 16.sp)
    }
}

// ------------------------------------------------------------------ panels

@Composable
fun SortPanel(onDismiss: () -> Unit, onChanged: () -> Unit) {
    BottomPanel(onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(bottom = 30.dp)) {
            Text("分组", color = C.textDim, fontSize = 14.sp)
            Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(Store.groupByLabel(), color = C.text, fontSize = 17.sp, modifier = Modifier.weight(1f))
                DrawIcon("arrow_up", C.text, 20.dp, 1.8.dp)
            }
            HLine()
            Spacer(Modifier.height(12.dp))
            Text("排序", color = C.textDim, fontSize = 14.sp)
            Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(Store.sortByLabel(), color = C.text, fontSize = 17.sp, modifier = Modifier.weight(1f))
                DrawIcon("arrow_up", C.text, 20.dp, 1.8.dp)
            }
            HLine()
            Spacer(Modifier.height(12.dp))
            Text("子任务", color = C.textDim, fontSize = 14.sp)
            Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("我的顺序", color = C.text, fontSize = 17.sp)
            }
            HLine()
            Spacer(Modifier.height(8.dp))
            ToggleRow("显示未开始项目", true) { }
            ToggleRow("显示已完成任务", Store.showCompleted) { Store.showCompleted = it; Store.save(); onChanged() }
            ToggleRow("显示已完成的子任务", true) { }
            ToggleRow("将已完成任务移至底部", true) { }
            Spacer(Modifier.height(8.dp))
            Text("已完成", color = C.textDim, fontSize = 14.sp)
            Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("按完成时间", color = C.text, fontSize = 17.sp, modifier = Modifier.weight(1f))
                DrawIcon("arrow_down", C.text, 20.dp, 1.8.dp)
            }
            HLine()
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth().height(52.dp).clickable { onDismiss() },
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
            ) { Text("取消", color = C.text, fontSize = 17.sp) }
        }
    }
}

private fun Store.groupByLabel(): String = when (groupBy) {
    "none" -> "无"
    "priority" -> "按优先级"
    else -> "按截止日期"
}

private fun Store.sortByLabel(): String = when (sortBy) {
    "priority" -> "按优先级"
    "title" -> "按标题"
    else -> "按截止日期"
}

@Composable
fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = C.text, fontSize = 17.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = C.blueDark)
        )
    }
}

@Composable
fun OverflowPanel(onDismiss: () -> Unit, onClearCompleted: () -> Unit) {
    BottomPanel(onDismiss = onDismiss, maxHeight = 320.dp) {
        Column(Modifier.fillMaxWidth().padding(bottom = 30.dp)) {
            SheetItem("清除已完成项", onClick = onClearCompleted)
            SheetItem("收起子任务") { onDismiss() }
            SheetItem("展开子任务") { onDismiss() }
            SheetItem("分享") { onDismiss() }
        }
    }
}

@Composable
fun SelectMenuPanel(
    onDismiss: () -> Unit,
    onSelectAll: () -> Unit,
    onClone: () -> Unit,
    onDelete: () -> Unit,
) {
    BottomPanel(onDismiss = onDismiss, maxHeight = 320.dp) {
        Column(Modifier.fillMaxWidth().padding(bottom = 30.dp)) {
            SheetItem("全选", onClick = onSelectAll)
            SheetItem("分享") { onDismiss() }
            SheetItem("克隆", onClick = onClone)
            SheetItem("删除", danger = true, onClick = onDelete)
        }
    }
}

@Composable
fun SheetItem(label: String, danger: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(58.dp).clickable { onClick() }.padding(horizontal = 26.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = if (danger) C.red else C.text, fontSize = 17.sp)
    }
}

@Composable
fun PopupItem(label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(48.dp).clickable { onClick() }
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) { Text(label, color = C.text, fontSize = 17.sp) }
}
