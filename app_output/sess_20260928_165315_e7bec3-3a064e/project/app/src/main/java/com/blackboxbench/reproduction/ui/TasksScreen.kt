package com.blackboxbench.reproduction.ui

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.statusBarsPadding
import com.blackboxbench.reproduction.BlueColor
import com.blackboxbench.reproduction.GroupBy
import com.blackboxbench.reproduction.Prefs
import com.blackboxbench.reproduction.Settings
import com.blackboxbench.reproduction.SortBy
import com.blackboxbench.reproduction.SortPrefs
import com.blackboxbench.reproduction.StateStore
import com.blackboxbench.reproduction.SubTask
import com.blackboxbench.reproduction.Task
import com.blackboxbench.reproduction.ViewRef
import com.blackboxbench.reproduction.cloneTask
import com.blackboxbench.reproduction.completeTask
import com.blackboxbench.reproduction.dayDiff
import com.blackboxbench.reproduction.dayMillis
import com.blackboxbench.reproduction.deleteTask
import com.blackboxbench.reproduction.dueLabel
import com.blackboxbench.reproduction.newId
import com.blackboxbench.reproduction.priorityColor
import com.blackboxbench.reproduction.priorityName
import com.blackboxbench.reproduction.tasksForView
import com.blackboxbench.reproduction.uncompleteTask
import com.blackboxbench.reproduction.viewTitle
import java.util.Calendar

class MainActions(
    val openSettings: () -> Unit,
    val openNotificationsSettings: () -> Unit,
    val openTagSettings: (String) -> Unit,
    val editTask: (String) -> Unit,
    val newTask: () -> Unit,
    val newTag: () -> Unit,
    val newList: () -> Unit,
    val newFilter: () -> Unit,
    val pickLocation: () -> Unit
)

private data class Group(val name: String, val order: Int, val tasks: List<Task>)

private fun sortTasks(list: List<Task>): List<Task> {
    val base = Comparator<Task> { a, b ->
        when (SortPrefs.sortBy) {
            SortBy.DUE -> compareValues(a.due ?: Long.MAX_VALUE, b.due ?: Long.MAX_VALUE)
            SortBy.PRIORITY -> compareValues(b.priority, a.priority)
            SortBy.TITLE -> compareValues(a.title, b.title)
        }
    }
    val signed = if (SortPrefs.ascending) base else Comparator { x, y -> -base.compare(x, y) }
    return list.sortedWith(signed)
}

private fun groupIndexOf(t: Task): Pair<Int, String> {
    val now = System.currentTimeMillis()
    return when (SortPrefs.groupBy) {
        GroupBy.DUE -> {
            if (t.due == null) Pair(4, "无截止日期")
            else when {
                dayDiff(t.due!!, now) < 0 -> Pair(0, "已过期")
                dayDiff(t.due!!, now) == 0 -> Pair(1, "今天截止")
                dayDiff(t.due!!, now) == 1 -> Pair(2, "明天截止")
                else -> Pair(3, "未来")
            }
        }
        GroupBy.START -> {
            if (t.start == null) Pair(4, "无开始日期")
            else when {
                dayDiff(t.start!!, now) < 0 -> Pair(0, "已开始")
                dayDiff(t.start!!, now) == 0 -> Pair(1, "今天开始")
                dayDiff(t.start!!, now) == 1 -> Pair(2, "明天开始")
                else -> Pair(3, "未来")
            }
        }
        GroupBy.PRIORITY -> when (t.priority) {
            3 -> Pair(0, "高优先级")
            2 -> Pair(1, "中优先级")
            1 -> Pair(2, "低优先级")
            else -> Pair(3, "无优先级")
        }
        GroupBy.MODIFIED -> when {
            dayDiff(now, t.modifiedAt) <= 0 -> Pair(0, "今天")
            dayDiff(now, t.modifiedAt) <= 7 -> Pair(1, "最近 7 天")
            else -> Pair(2, "更早")
        }
        GroupBy.CREATED -> when {
            dayDiff(now, t.createdAt) <= 0 -> Pair(0, "今天")
            dayDiff(now, t.createdAt) <= 7 -> Pair(1, "最近 7 天")
            else -> Pair(2, "更早")
        }
        GroupBy.LIST -> {
            val name = StateStore.lists.firstOrNull { it.id == t.listId }?.name ?: "清单"
            Pair(0, name)
        }
        GroupBy.NONE -> Pair(0, "")
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(view: MutableState<ViewRef>, actions: MainActions) {
    val context = LocalContext.current
    var drawerOpen by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var showSort by remember { mutableStateOf(false) }
    var showMic by remember { mutableStateOf(false) }
    var showOverflow by remember { mutableStateOf(false) }
    var showGearMenu by remember { mutableStateOf(false) }
    var showOffline by remember { mutableStateOf(false) }
    val selection = remember { mutableStateListOf<String>() }
    val expandedGroups = remember { mutableStateListOf<String>() }
    val expandedSubs = remember { mutableStateListOf<String>() }
    var completedGroupOpen by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var showListDialog by remember { mutableStateOf(false) }
    var showDueDialog by remember { mutableStateOf(false) }
    var showPriorityDialog by remember { mutableStateOf(false) }
    var showMultiShare by remember { mutableStateOf(false) }
    var sortVersion by remember { mutableStateOf(0) }

    BackHandlerClose(drawerOpen) { drawerOpen = false }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(drawerOpen) {
                if (!drawerOpen) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        if (down.position.x < 90f) {
                            while (true) {
                                val event = awaitPointerEvent()
                                val ch = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (ch.position.x - down.position.x > 70f) {
                                    drawerOpen = true
                                    break
                                }
                                if (!ch.pressed) break
                            }
                        }
                    }
                }
            }
    ) {
        Column(Modifier.fillMaxSize()) {
            // ---------------- Top bar ----------------
            if (selection.isNotEmpty()) {
                MultiSelectTopBar(
                    count = selection.size,
                    onClose = { selection.clear() },
                    onTags = { showTagDialog = true },
                    onList = { showListDialog = true },
                    onDue = { showDueDialog = true },
                    onPriority = { showPriorityDialog = true },
                    onMore = { showMultiShare = true }
                )
            } else if (searching) {
                SearchTopBar(
                    query = query,
                    onQuery = { query = it },
                    onClose = {
                        searching = false
                        query = ""
                    }
                )
            } else {
                NormalTopBar(
                    title = viewTitle(view.value),
                    onGear = {
                        if (view.value.type == "tag") showGearMenu = true else actions.openSettings()
                    },
                    gearMenuOpen = showGearMenu,
                    onGearDismiss = { showGearMenu = false },
                    onAppSettings = {
                        showGearMenu = false
                        actions.openSettings()
                    },
                    onTagSettings = {
                        showGearMenu = false
                        actions.openTagSettings(view.value.id)
                    }
                )
            }

            // ---------------- Banner ----------------
            val bannerIndex = Prefs.int("banner_index", 0)
            if (bannerIndex < 2) {
                ReminderBanner(
                    variant = bannerIndex,
                    onClose = { Prefs.putInt("banner_index", bannerIndex + 1) },
                    onSettings = { actions.openNotificationsSettings() }
                )
            }

            // ---------------- Task list ----------------
            val sortStamp = sortVersion
            val all = tasksForView(view.value).filter { task ->
                if (!SortPrefs.showNotStarted && task.start != null && task.start!! > System.currentTimeMillis()) false
                else if (searching && query.isNotBlank()) {
                    task.title.contains(query, true) || task.notes.contains(query, true)
                } else true
            }
            val active = all.filter { !it.completed }
            val done = all.filter { it.completed }

            if (all.isEmpty()) {
                EmptyState()
            } else {
                val lazyItems = mutableListOf<Any>()
                if (SortPrefs.groupBy == GroupBy.NONE) {
                    lazyItems.addAll(sortTasks(active))
                } else {
                    val grouped = mutableMapOf<Int, Pair<String, MutableList<Task>>>()
                    active.forEach { t ->
                        val (idx, name) = groupIndexOf(t)
                        val entry = grouped.getOrPut(idx) { Pair(name, mutableListOf()) }
                        entry.second.add(t)
                    }
                    grouped.toSortedMap().forEach { (_, entry) ->
                        lazyItems.add("header:" + entry.first)
                        if (!expandedGroups.contains(entry.first)) {
                            lazyItems.addAll(sortTasks(entry.second))
                        }
                    }
                }
                LazyColumn(Modifier.weight(1f)) {
                    items(lazyItems, key = {
                        when (it) {
                            is Task -> it.id
                            else -> it as String
                        }
                    }) { item ->
                        when (item) {
                            is String -> {
                                val name = item.removePrefix("header:")
                                GroupHeader(
                                    name = name,
                                    collapsed = expandedGroups.contains(name),
                                    onClick = {
                                        if (expandedGroups.contains(name)) expandedGroups.remove(name)
                                        else expandedGroups.add(name)
                                    }
                                )
                            }
                            is Task -> TaskRow(
                                task = item,
                                view = view.value,
                                selected = selection.contains(item.id),
                                selectionMode = selection.isNotEmpty(),
                                expanded = expandedSubs.contains(item.id),
                                onExpandToggle = {
                                    if (expandedSubs.contains(item.id)) expandedSubs.remove(item.id)
                                    else expandedSubs.add(item.id)
                                },
                                onLongPress = { selection.add(item.id) },
                                onClick = {
                                    if (selection.isNotEmpty()) {
                                        if (selection.contains(item.id)) selection.remove(item.id)
                                        else selection.add(item.id)
                                    } else actions.editTask(item.id)
                                }
                            )
                        }
                    }
                    if (done.isNotEmpty() && SortPrefs.showCompleted) {
                        item(key = "done-header") {
                            GroupHeader(
                                name = "已完成",
                                collapsed = completedGroupOpen,
                                onClick = { completedGroupOpen = !completedGroupOpen }
                            )
                        }
                        if (!completedGroupOpen) {
                            items(done.sortedByDescending { it.completedAt ?: 0L }, key = { "done-" + it.id }) { t ->
                                TaskRow(
                                    task = t,
                                    view = view.value,
                                    selected = selection.contains(t.id),
                                    selectionMode = selection.isNotEmpty(),
                                    expanded = false,
                                    onExpandToggle = {},
                                    onLongPress = { selection.add(t.id) },
                                    onClick = {
                                        if (selection.isNotEmpty()) {
                                            if (selection.contains(t.id)) selection.remove(t.id)
                                            else selection.add(t.id)
                                        } else actions.editTask(t.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ---------------- Bottom bar + FAB ----------------
            Surface(color = Color(0xFFF5F5F8)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomBarItem("≣", "菜单") { drawerOpen = true }
                    BottomBarItem("🔍", "搜索") { searching = true }
                    BottomBarItem("⇅", "排序") { showSort = true }
                    BottomBarItem("🎤", "麦克风") { showMic = true }
                    Box {
                        BottomBarItem("⋮", "更多") { showOverflow = true }
                        DropdownMenu(expanded = showOverflow, onDismissRequest = { showOverflow = false }) {
                            DropdownMenuItem(text = { Text("清除已完成项") }, onClick = {
                                showOverflow = false
                                StateStore.tasks.removeAll { it.completed }
                                StateStore.save()
                            })
                            DropdownMenuItem(text = { Text("收起子任务") }, onClick = {
                                showOverflow = false
                                expandedSubs.clear()
                            })
                            DropdownMenuItem(text = { Text("展开子任务") }, onClick = {
                                showOverflow = false
                                expandedSubs.clear()
                                StateStore.tasks.filter { it.subtasks.isNotEmpty() }.forEach { expandedSubs.add(it.id) }
                            })
                            DropdownMenuItem(text = { Text("分享") }, onClick = {
                                showOverflow = false
                                val text = all.joinToString("\n") { "· " + it.title }
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                context.startActivity(Intent.createChooser(send, "分享"))
                            })
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Surface(
                        color = BlueColor,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .size(48.dp)
                            .clickable { actions.newTask() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("+", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ---------------- Drawer ----------------
        if (drawerOpen) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0x66000000))
                    .clickable { drawerOpen = false }
            )
            Surface(
                modifier = Modifier
                    .fillMaxSize(0.85f)
                    .background(MaterialTheme.colorScheme.background),
                color = MaterialTheme.colorScheme.background
            ) {
                DrawerContent(
                    current = view.value,
                    onSelect = { ref ->
                        view.value = ref
                        drawerOpen = false
                    },
                    onNewTag = {
                        drawerOpen = false
                        actions.newTag()
                    },
                    onNewList = {
                        drawerOpen = false
                        actions.newList()
                    },
                    onNewFilter = {
                        drawerOpen = false
                        actions.newFilter()
                    },
                    onPickLocation = {
                        drawerOpen = false
                        actions.pickLocation()
                    }
                )
            }
        }
    }

    // ---------------- Dialogs / sheets ----------------
    if (showSort) {
        ModalBottomSheet(onDismissRequest = { showSort = false }) {
            SortSheet(onDone = { showSort = false }, onChanged = { sortVersion++ })
        }
    }
    if (showMic) {
        MicDialog(onDismiss = { showMic = false })
    }
    if (showTagDialog) {
        MultiTagDialog(selection.toList(), onDismiss = { showTagDialog = false }) { tagId, checked ->
            selection.forEach { id ->
                StateStore.tasks.firstOrNull { it.id == id }?.let { t ->
                    if (checked && !t.tagIds.contains(tagId)) t.tagIds.add(tagId)
                    if (!checked) t.tagIds.remove(tagId)
                    t.modifiedAt = System.currentTimeMillis()
                }
            }
            StateStore.save()
        }
    }
    if (showListDialog) {
        ListSelectDialog(onDismiss = { showListDialog = false }) { listId ->
            selection.forEach { id ->
                StateStore.tasks.firstOrNull { it.id == id }?.let {
                    it.listId = listId
                    it.modifiedAt = System.currentTimeMillis()
                }
            }
            StateStore.save()
        }
    }
    if (showDueDialog) {
        QuickDueDialog(onDismiss = { showDueDialog = false }) { due ->
            val now = System.currentTimeMillis()
            selection.forEach { id ->
                StateStore.tasks.firstOrNull { it.id == id }?.let {
                    it.due = due
                    it.dueHasTime = due != null && due % 86400000L != 0L
                    it.modifiedAt = now
                }
            }
            StateStore.save()
        }
    }
    if (showPriorityDialog) {
        PriorityPickDialog(onDismiss = { showPriorityDialog = false }) { p ->
            selection.forEach { id ->
                StateStore.tasks.firstOrNull { it.id == id }?.let {
                    it.priority = p
                    it.modifiedAt = System.currentTimeMillis()
                }
            }
            StateStore.save()
        }
    }
    if (showMultiShare) {
        AlertDialog(
            onDismissRequest = { showMultiShare = false },
            title = { Text(selection.size.toString()) },
            text = {
                Column {
                    Text("全选", Modifier
                        .fillMaxWidth()
                        .clickable {
                            selection.clear()
                            selection.addAll(tasksForView(view.value).map { it.id })
                            showMultiShare = false
                        }
                        .padding(vertical = 12.dp))
                    Text("分享", Modifier
                        .fillMaxWidth()
                        .clickable {
                            showMultiShare = false
                            val text = selection.mapNotNull { id -> StateStore.tasks.firstOrNull { it.id == id }?.title }
                                .joinToString("\n") { "· " + it }
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(send, "分享"))
                        }
                        .padding(vertical = 12.dp))
                    Text("克隆", Modifier
                        .fillMaxWidth()
                        .clickable {
                            showMultiShare = false
                            selection.mapNotNull { id -> StateStore.tasks.firstOrNull { it.id == id } }.forEach { cloneTask(it) }
                        }
                        .padding(vertical = 12.dp))
                    Text("删除", color = Color(0xFFD64545), modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showMultiShare = false
                            selection.mapNotNull { id -> StateStore.tasks.firstOrNull { it.id == id } }.forEach { deleteTask(it) }
                            selection.clear()
                        }
                        .padding(vertical = 12.dp))
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMultiShare = false }) { Text("关闭") }
            }
        )
    }
    if (showOffline) {
        OfflineAccountDialog(onDismiss = { showOffline = false })
    }
}

@Composable
private fun BackHandlerClose(enabled: Boolean, onClose: () -> Unit) {
    androidx.activity.compose.BackHandler(enabled = enabled, onBack = onClose)
}

@Composable
private fun NormalTopBar(
    title: String,
    onGear: () -> Unit,
    gearMenuOpen: Boolean,
    onGearDismiss: () -> Unit,
    onAppSettings: () -> Unit,
    onTagSettings: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = BlueColor,
            modifier = Modifier.weight(1f)
        )
        Box {
            IconButton(onClick = onGear) {
                Icon(Icons.Filled.Settings, contentDescription = "设置", tint = Color(0xFF5A5A62))
            }
            DropdownMenu(expanded = gearMenuOpen, onDismissRequest = onGearDismiss) {
                DropdownMenuItem(text = { Text("应用设置") }, onClick = onAppSettings)
                DropdownMenuItem(text = { Text("标签设置") }, onClick = onTagSettings)
            }
        }
    }
}

@Composable
private fun SearchTopBar(query: String, onQuery: (String) -> Unit, onClose: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier.weight(1f),
            placeholder = { Text("搜索") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF5A5A62)) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    Text("✕", color = BlueColor, modifier = Modifier
                        .clickable { onQuery("") }
                        .padding(8.dp))
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp)
        )
        TextButton(onClick = onClose) { Text("取消") }
    }
}

@Composable
private fun MultiSelectTopBar(
    count: Int,
    onClose: () -> Unit,
    onTags: () -> Unit,
    onList: () -> Unit,
    onDue: () -> Unit,
    onPriority: () -> Unit,
    onMore: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 4.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Text("✕", fontSize = 20.sp, color = Color(0xFF5A5A62))
        }
        Text(count.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text("🏷", fontSize = 20.sp, modifier = Modifier
            .clickable(onClick = onTags)
            .padding(10.dp))
        Text("≣", fontSize = 20.sp, modifier = Modifier
            .clickable(onClick = onList)
            .padding(10.dp))
        Text("🕒", fontSize = 20.sp, modifier = Modifier
            .clickable(onClick = onDue)
            .padding(10.dp))
        Text("🚩", fontSize = 20.sp, modifier = Modifier
            .clickable(onClick = onPriority)
            .padding(10.dp))
        Text("⋮", fontSize = 20.sp, modifier = Modifier
            .clickable(onClick = onMore)
            .padding(10.dp))
    }
}

@Composable
private fun BottomBarItem(glyph: String, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(glyph, fontSize = 20.sp, color = Color(0xFF44454A))
        Text(label, fontSize = 10.sp, color = Color(0xFF80868B))
    }
}

@Composable
private fun ReminderBanner(variant: Int, onClose: () -> Unit, onSettings: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                if (variant == 0) "启用提醒" else "在正确的时间收到通知",
                color = Color(0xFFD64545),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            if (variant == 0) {
                Text("在正确的时间收到通知", fontSize = 13.sp, color = Color(0xFF5A5A62))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onClose) { Text("关闭", color = Color(0xFF5A5A62)) }
                TextButton(onClick = onSettings) { Text("设置", color = BlueColor) }
            }
        }
    }
}

@Composable
private fun GroupHeader(name: String, collapsed: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Icon(
            if (collapsed) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskRow(
    task: Task,
    view: ViewRef,
    selected: Boolean,
    selectionMode: Boolean,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    onLongPress: () -> Unit,
    onClick: () -> Unit
) {
    val pColor = Color(priorityColor(task.priority))
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 4.dp)
    ) {
        if (task.repeat.isNotEmpty()) {
            Box(
                Modifier
                    .padding(top = 6.dp)
                    .size(28.dp)
                    .clickable { completeTask(task) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = "重复",
                    tint = Color(0xFFD64545),
                    modifier = Modifier.size(22.dp)
                )
            }
        } else {
            Checkbox(
                checked = task.completed,
                onCheckedChange = {
                    if (task.completed) uncompleteTask(task) else completeTask(task)
                },
                modifier = Modifier.padding(top = 0.dp),
                colors = CheckboxDefaults.colors(
                    checkedColor = pColor,
                    uncheckedColor = pColor,
                    checkmarkColor = Color.White
                )
            )
        }
        Column(Modifier.weight(1f).padding(top = 2.dp)) {
            Text(
                task.title,
                fontSize = Settings.fontSize.sp,
                color = if (task.completed) Color(0xFF9AA0AA) else MaterialTheme.colorScheme.onSurface,
                maxLines = if (Settings.showFullTitle) Int.MAX_VALUE else 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                )
            )
            if (Settings.showDescription && task.notes.isNotBlank()) {
                Text(
                    task.notes,
                    fontSize = 13.sp,
                    color = Color(0xFF9AA0AA),
                    maxLines = if (Settings.showFullDescription) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val subs = if (SortPrefs.subtaskAuto)
                task.subtasks.sortedBy { it.completed } else task.subtasks.toList()
            val visibleSubs = if (SortPrefs.showCompletedSubtasks) subs else subs.filter { !it.completed }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (task.subtasks.isNotEmpty()) {
                    FilterChip(
                        selected = expanded,
                        onClick = onExpandToggle,
                        label = { Text((if (expanded) "∧ " else "∨ ") + task.subtasks.count { !it.completed }) }
                    )
                }
                if (view.type != "list") {
                    val listName = StateStore.lists.firstOrNull { it.id == task.listId }?.name
                    if (listName != null) {
                        AssistChipText("≣ $listName")
                    }
                }
            }
            if (expanded && visibleSubs.isNotEmpty()) {
                visibleSubs.forEach { st ->
                    SubTaskRow(st)
                }
            }
        }
        val dueText = dueLabel(task.due, task.dueHasTime)
        if (dueText.isNotEmpty()) {
            val overdue = task.due != null && dayDiff(task.due!!, System.currentTimeMillis()) < 0 && !task.completed
            Text(
                dueText,
                fontSize = 13.sp,
                color = if (overdue) Color(0xFFD64545) else Color(0xFF5A5A62),
                modifier = Modifier.padding(top = 8.dp, start = 4.dp)
            )
        } else {
            Spacer(Modifier.width(4.dp))
        }
    }
}

@Composable
private fun AssistChipText(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun SubTaskRow(st: SubTask) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = st.completed,
            onCheckedChange = { checked ->
                st.completed = checked
                st.completedAt = if (checked) System.currentTimeMillis() else null
                StateStore.save()
            },
            modifier = Modifier.size(30.dp),
            colors = CheckboxDefaults.colors(checkmarkColor = Color.White)
        )
        Text(
            st.title,
            fontSize = 14.sp,
            color = if (st.completed) Color(0xFF9AA0AA) else MaterialTheme.colorScheme.onSurface,
            style = TextStyle(textDecoration = if (st.completed) TextDecoration.LineThrough else TextDecoration.None)
        )
    }
}

// ---------------- Sort sheet ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortSheet(onDone: () -> Unit, onChanged: () -> Unit) {
    var refresh by remember { mutableStateOf(0) }
    androidx.compose.runtime.key(refresh) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("分组", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            val ascText = if (SortPrefs.ascending) "升序" else "降序"
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.clickable {
                    SortPrefs.ascending = !SortPrefs.ascending
                    refresh++; onChanged()
                }
            ) {
                Text(
                    ascText,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        GroupBy.entries.toList().chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { g ->
                    FilterChip(
                        selected = SortPrefs.groupBy == g,
                        onClick = {
                            SortPrefs.groupBy = g
                            if (g == GroupBy.PRIORITY) SortPrefs.ascending = false
                            refresh++; onChanged()
                        },
                        label = { Text(g.label) },
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("排序", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            SortBy.entries.forEach { s ->
                FilterChip(
                    selected = SortPrefs.sortBy == s,
                    onClick = {
                        SortPrefs.sortBy = s
                        refresh++; onChanged()
                    },
                    label = { Text(s.label) },
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("子任务顺序", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            FilterChip(
                selected = !SortPrefs.subtaskAuto,
                onClick = {
                    SortPrefs.subtaskAuto = false
                    refresh++; onChanged()
                },
                label = { Text("手动") },
                modifier = Modifier.padding(end = 6.dp)
            )
            FilterChip(
                selected = SortPrefs.subtaskAuto,
                onClick = {
                    SortPrefs.subtaskAuto = true
                    refresh++; onChanged()
                },
                label = { Text("自动") }
            )
        }
        Spacer(Modifier.height(12.dp))
        SortSwitch("显示未开始", SortPrefs.showNotStarted) { SortPrefs.showNotStarted = it; refresh++; onChanged() }
        SortSwitch("显示已完成", SortPrefs.showCompleted) { SortPrefs.showCompleted = it; refresh++; onChanged() }
        SortSwitch("显示已完成子任务", SortPrefs.showCompletedSubtasks) { SortPrefs.showCompletedSubtasks = it; refresh++; onChanged() }
        SortSwitch("已完成移到底部", SortPrefs.completedAtBottom) { SortPrefs.completedAtBottom = it; refresh++; onChanged() }
        Spacer(Modifier.height(24.dp))
    }
    }
}

@Composable
private fun SortSwitch(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    var v by remember(checked) { mutableStateOf(checked) }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = v,
            onCheckedChange = {
                v = it
                onChange(it)
            },
            colors = SwitchDefaults.colors(checkedTrackColor = BlueColor)
        )
    }
}

// ---------------- Misc dialogs ----------------

@Composable
private fun MicDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Google", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF44454A))
                Spacer(Modifier.height(18.dp))
                Box(
                    Modifier
                        .size(64.dp)
                        .background(Color(0xFFD64545), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎤", fontSize = 30.sp)
                }
                Spacer(Modifier.height(18.dp))
                Text("未连接到网络", fontSize = 16.sp, color = Color(0xFF44454A))
                TextButton(onClick = { }) { Text("重试", color = BlueColor) }
                Spacer(Modifier.height(8.dp))
                Text("中文 (中国)", fontSize = 13.sp, color = Color(0xFF80868B))
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
private fun MultiTagDialog(selectedIds: List<String>, onDismiss: () -> Unit, onApply: (String, Boolean) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("标签") },
        text = {
            Column {
                StateStore.tags.forEach { tag ->
                    val checked = selectedIds.any { id ->
                        StateStore.tasks.firstOrNull { it.id == id }?.tagIds?.contains(tag.id) == true
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onApply(tag.id, !checked) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tag.name, modifier = Modifier.weight(1f))
                        if (checked) Text("✓", color = BlueColor)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成", color = BlueColor) }
        }
    )
}

@Composable
private fun ListSelectDialog(onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("清单") },
        text = {
            Column {
                StateStore.lists.forEach { l ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(l.id)
                                onDismiss()
                            }
                            .padding(vertical = 10.dp)
                    ) {
                        Text("≣ " + l.name)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun QuickDueDialog(onDismiss: () -> Unit, onPick: (Long?) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("截止日期") },
        text = {
            Column {
                val now = Calendar.getInstance()
                val today = dayMillis(now.timeInMillis)
                val options = listOf<Pair<String, Long?>>(
                    "今天" to today,
                    "明天" to today + 86400000L,
                    "下周二" to {
                        val c = Calendar.getInstance()
                        c.timeInMillis = today
                        while (c.get(Calendar.DAY_OF_WEEK) != Calendar.TUESDAY || c.timeInMillis <= today) {
                            c.add(Calendar.DAY_OF_YEAR, 1)
                        }
                        dayMillis(c.timeInMillis)
                    }(),
                    "无日期" to null
                )
                options.forEach { (label, value) ->
                    Text(
                        label,
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                onPick(value)
                                onDismiss()
                            }
                            .padding(vertical = 10.dp)
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun PriorityPickDialog(onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("优先级") },
        text = {
            Column {
                listOf(3 to "高", 2 to "中", 1 to "低", 0 to "无").forEach { (p, name) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                onPick(p)
                                onDismiss()
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(20.dp)
                                .background(Color(priorityColor(p)), CircleShape)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(name)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
