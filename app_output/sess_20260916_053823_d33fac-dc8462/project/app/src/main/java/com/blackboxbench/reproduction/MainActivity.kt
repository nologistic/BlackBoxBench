package com.blackboxbench.reproduction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

sealed interface Route {
    data object MyTasks : Route
    data object Today : Route
    data object Recent : Route
    data class Tag(val name: String) : Route
    data class List(val id: String) : Route
    data object Settings : Route
    data object Appearance : Route
    data class TaskEditor(val taskId: String, val isNew: Boolean, val listId: String) : Route
    data class TagPicker(val taskId: String) : Route
    data class ListEditor(val listId: String?) : Route
    data object FilterEditor : Route
}

val BottomBarColor = Color(0xFFEDEAF3)
val DrawerBg = Color(0xFFF1F1F5)
val CardBg = Color(0xFFFFFFFF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(applicationContext)
        setContent { AppRoot() }
    }
}

@Composable
fun AppRoot() {
    var dark by remember { mutableStateOf(false) }
    var onboarded by remember { mutableStateOf(Store.onboarded) }
    BenchmarkAppTheme(dark = dark) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (!onboarded) {
                OnboardingScreen(
                    onContinue = { onboarded = true },
                    onImport = { onboarded = true },
                    onAccount = { onboarded = true }
                )
            } else {
                MainShell(dark = dark, onDarkChange = { dark = it })
            }
        }
    }
}

@Composable
fun OnboardingScreen(onContinue: () -> Unit, onImport: () -> Unit, onAccount: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier.size(150.dp).clip(CircleShape).background(FabBlue),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(90.dp))
        }
        Spacer(Modifier.weight(1f))
        PillButton(text = "添加账号", filled = true) {
            Store.setOnboarded(context)
            onAccount()
        }
        Spacer(Modifier.height(16.dp))
        PillButton(text = "继续但不同步", filled = false) {
            Store.setOnboarded(context)
            onContinue()
        }
        Spacer(Modifier.height(16.dp))
        PillButton(text = "导入 Tasks.org 备份", filled = false) {
            Store.setOnboarded(context)
            onImport()
        }
        Spacer(Modifier.height(48.dp))
    }
}

@Composable
fun PillButton(text: String, filled: Boolean, onClick: () -> Unit) {
    val bg = if (filled) DeepBlue else Color.Transparent
    val fg = if (filled) Color.White else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(bg)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(30.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = fg, fontSize = 19.sp)
    }
}

data class SortConfig(
    val groupBy: String = "按截止日期",
    val sortBy: String = "按截止日期",
    val groupAsc: Boolean = true,
    val sortAsc: Boolean = true,
    val completedBy: String = "按完成时间",
    val completedAsc: Boolean = false
)

@Composable
fun MainShell(dark: Boolean, onDarkChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    val backStack = remember { mutableStateListOf<Route>(Route.MyTasks) }
    val current = backStack.last()
    val push: (Route) -> Unit = { backStack.add(it) }
    val pop: () -> Unit = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) }

    when (current) {
        is Route.TaskEditor -> TaskEditorScreen(
            taskId = current.taskId,
            isNew = current.isNew,
            listId = current.listId,
            onBack = pop
        )
        is Route.TagPicker -> TagPickerScreen(taskId = current.taskId, onBack = pop)
        is Route.Settings -> SettingsScreen(
            dark = dark,
            onDarkChange = onDarkChange,
            onBack = pop,
            onAppearance = { push(Route.Appearance) },
            onOpenList = { push(Route.List(Store.lists.firstOrNull()?.id ?: "lst_default")) },
            onNewList = { push(Route.ListEditor(null)) }
        )
        is Route.Appearance -> AppearanceScreen(
            dark = dark,
            onDarkChange = onDarkChange,
            onBack = pop,
            onNewList = { }
        )
        is Route.ListEditor -> ListEditorScreen(listId = current.listId, onBack = pop)
        is Route.FilterEditor -> FilterEditorScreen(onBack = pop)
        else -> ListScaffold(
            route = current,
            push = push,
            pop = pop
        )
    }
}

@Composable
fun ListScaffold(route: Route, push: (Route) -> Unit, pop: () -> Unit) {
    val context = LocalContext.current
    val revision = Store.revision
    var drawerOpen by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var sortOpen by remember { mutableStateOf(false) }
    var moreOpen by remember { mutableStateOf(false) }
    var selectionOpen by remember { mutableStateOf(false) }
    var overflowOpen by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var config by remember { mutableStateOf(SortConfig()) }
    var showCompleted by remember { mutableStateOf(false) }
    var collapsedSections by remember { mutableStateOf(setOf<String>()) }
    var bannerVisible by remember { mutableStateOf(true) }
    var dialog by remember { mutableStateOf<String?>(null) }
    var subtaskCollapsed by remember { mutableStateOf(false) }
    var shareVisible by remember { mutableStateOf(false) }

    val title = when (route) {
        is Route.MyTasks -> "我的任务"
        is Route.Today -> "今天"
        is Route.Recent -> "最近修改过的"
        is Route.Tag -> route.name
        is Route.List -> Store.listName(route.id)
        else -> "我的任务"
    }

    val all = Store.tasks
    var visible = when (route) {
        is Route.MyTasks -> all
        is Route.Today -> {
            val today = java.time.LocalDate.now()
            all.filter { t ->
                val d = dueDate(t.due)
                (d != null && !d.isAfter(today)) || (t.isCompleted && d == today)
            }
        }
        is Route.Recent -> all.sortedByDescending { it.modifiedAt }
        is Route.Tag -> all.filter { route.name in it.tags }
        is Route.List -> all.filter { it.listId == route.id }
        else -> all
    }
    if (searching && query.isNotEmpty()) {
        visible = all.filter { it.title.contains(query, true) || it.notes.contains(query, true) }
    }

    val pending = visible.filter { !it.isCompleted }
    val completed = visible.filter { it.isCompleted }.let { list ->
        if (config.completedAsc) list.sortedBy { it.completedAt } else list.sortedByDescending { it.completedAt }
    }

    fun sortedBucket(bucket: Int): List<TaskRec> {
        val list = pending.filter { bucketOf(it) == bucket }.toMutableList()
        val cmp = when (config.sortBy) {
            "按优先级" -> compareBy<TaskRec> { it.priority }
            "按标题" -> compareBy<TaskRec> { it.title }
            else -> compareBy<TaskRec> { it.due ?: "zzzz" }
        }
        val s = if (config.sortAsc) list.sortedWith(cmp) else list.sortedWith(cmp.reversed())
        return s
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (drawerOpen) {
                DrawerContent(
                    route = route,
                    onSelect = { r -> drawerOpen = false; if (r == null) { } else push(r) },
                    onCloseDrawer = { drawerOpen = false }
                )
            }
            Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                TopBar(
                    title = title,
                    searching = searching,
                    query = query,
                    onQuery = { query = it },
                    selectionOpen = selectionOpen,
                    selectedCount = selected.size,
                    onBack = { if (searching) { searching = false; query = "" } else pop() },
                    onGear = { push(Route.Settings) },
                    onSelectionOverflow = { overflowOpen = true }
                )
                Box(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (!searching && bannerVisible && route is Route.MyTasks) {
                            ReminderBanner(onClose = { bannerVisible = false })
                        }
                        val hasAny = pending.isNotEmpty() || completed.isNotEmpty()
                        if (!hasAny) {
                            EmptyState()
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                if (route is Route.Recent || (searching && query.isNotEmpty())) {
                                    items(pending, key = { it.id }) { t ->
                                        TaskBlock(
                                            task = t,
                                            selectionOpen = selectionOpen,
                                            selected = t.id in selected,
                                            subtaskCollapsed = subtaskCollapsed,
                                            onOpen = { push(Route.TaskEditor(t.id, false, t.listId)) },
                                            onToggle = {
                                                if (t.isCompleted) Store.reopen(t) else Store.complete(t)
                                                Store.save(context)
                                            },
                                            onLong = { selectionOpen = true; selected = selected + t.id },
                                            onSelectToggle = {
                                                selected = if (t.id in selected) selected - t.id else selected + t.id
                                            }
                                        )
                                    }
                                    if (completed.isNotEmpty()) {
                                        items(completed, key = { it.id }) { t ->
                                            TaskBlock(
                                                task = t,
                                                selectionOpen = selectionOpen,
                                                selected = t.id in selected,
                                                subtaskCollapsed = subtaskCollapsed,
                                                onOpen = { push(Route.TaskEditor(t.id, false, t.listId)) },
                                                onToggle = { Store.reopen(t); Store.save(context) },
                                                onLong = { selectionOpen = true; selected = selected + t.id },
                                                onSelectToggle = {
                                                    selected = if (t.id in selected) selected - t.id else selected + t.id
                                                }
                                            )
                                        }
                                    }
                                } else {
                                    bucketTitles.forEachIndexed { idx, name ->
                                        val list = sortedBucket(idx)
                                        if (list.isEmpty()) return@forEachIndexed
                                        item(key = "h_$idx") {
                                            SectionHeader(
                                                title = name,
                                                expanded = name !in collapsedSections,
                                                onToggle = {
                                                    collapsedSections = if (name in collapsedSections)
                                                        collapsedSections - name else collapsedSections + name
                                                }
                                            )
                                        }
                                        if (name !in collapsedSections) {
                                            items(list, key = { it.id }) { t ->
                                                TaskBlock(
                                                    task = t,
                                                    selectionOpen = selectionOpen,
                                                    selected = t.id in selected,
                                                    subtaskCollapsed = subtaskCollapsed,
                                                    onOpen = { push(Route.TaskEditor(t.id, false, t.listId)) },
                                                    onToggle = {
                                                        if (t.isCompleted) Store.reopen(t) else Store.complete(t)
                                                        Store.save(context)
                                                    },
                                                    onLong = { selectionOpen = true; selected = selected + t.id },
                                                    onSelectToggle = {
                                                        selected = if (t.id in selected) selected - t.id else selected + t.id
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    if (completed.isNotEmpty() && showCompleted) {
                                        item(key = "h_done") {
                                            SectionHeader(
                                                title = "已完成",
                                                expanded = showCompleted,
                                                onToggle = { showCompleted = !showCompleted }
                                            )
                                        }
                                        if (showCompleted) {
                                            items(completed, key = { it.id }) { t ->
                                                TaskBlock(
                                                    task = t,
                                                    selectionOpen = selectionOpen,
                                                    selected = t.id in selected,
                                                    subtaskCollapsed = subtaskCollapsed,
                                                    onOpen = { push(Route.TaskEditor(t.id, false, t.listId)) },
                                                    onToggle = { Store.reopen(t); Store.save(context) },
                                                    onLong = { selectionOpen = true; selected = selected + t.id },
                                                    onSelectToggle = {
                                                        selected = if (t.id in selected) selected - t.id else selected + t.id
                                                    }
                                                )
                                            }
                                        }
                                    } else if (completed.isNotEmpty()) {
                                        item(key = "h_done_collapsed") {
                                            SectionHeader(title = "已完成", expanded = false, onToggle = { showCompleted = true })
                                        }
                                    }
                                }
                                item { Spacer(Modifier.height(96.dp)) }
                            }
                        }
                    }
                }
                BottomBar(
                    onMenu = { drawerOpen = true },
                    onSearch = { searching = true },
                    onSort = { sortOpen = true },
                    onMic = { dialog = "mic" },
                    onMore = { moreOpen = true },
                    onAdd = {
                        val listId = when (route) {
                            is Route.List -> route.id
                            else -> Store.lists.firstOrNull()?.id ?: "lst_default"
                        }
                        val t = Store.addTask(listId)
                        Store.save(context)
                        push(Route.TaskEditor(t.id, true, listId))
                    }
                )
            }
        }

        if (sortOpen) {
            SortPanel(
                config = config,
                onChange = { config = it; Store.touch() },
                onDismiss = { sortOpen = false }
            )
        }
        if (moreOpen) {
            MoreMenu(
                onDismiss = { moreOpen = false },
                onClearCompleted = { moreOpen = false; dialog = "clear" },
                onCollapseSub = { moreOpen = false; subtaskCollapsed = true },
                onExpandSub = { moreOpen = false; subtaskCollapsed = false },
                onShare = { moreOpen = false; shareVisible = true }
            )
        }
        if (selectionOpen && overflowOpen) {
            SelectionOverflow(
                onDismiss = { overflowOpen = false },
                onSelectAll = { selected = pending.map { it.id }.toSet(); overflowOpen = false },
                onShare = { overflowOpen = false; shareVisible = true },
                onClone = { dialog = "clone"; overflowOpen = false },
                onDelete = { dialog = "delete"; overflowOpen = false }
            )
        }
        if (dialog == "clear") {
            ConfirmDialog(
                title = "是否清除完成的任务？",
                message = "${completed.size} 个任务 将会删除。此操作无法撤销！",
                onCancel = { dialog = null },
                onConfirm = {
                    Store.tasks.removeAll { it.isCompleted }
                    Store.save(context)
                    dialog = null
                }
            )
        }
        if (dialog == "delete") {
            ConfirmDialog(
                title = "删除已选的任务？",
                message = null,
                onCancel = { dialog = null },
                onConfirm = {
                    Store.tasks.removeAll { it.id in selected }
                    Store.save(context)
                    selected = setOf()
                    selectionOpen = false
                    dialog = null
                }
            )
        }
        if (dialog == "clone") {
            ConfirmDialog(
                title = "复制已选的任务？",
                message = null,
                onCancel = { dialog = null },
                onConfirm = {
                    val copies = Store.tasks.filter { it.id in selected }.map { it.copy(id = "t_" + System.nanoTime()) }
                    copies.forEach { c ->
                        c.subtasks = c.subtasks.map { it.copy(id = "s_" + System.nanoTime()) }.toMutableList()
                    }
                    Store.tasks.addAll(0, copies)
                    Store.save(context)
                    selected = setOf()
                    selectionOpen = false
                    dialog = null
                }
            )
        }
        if (dialog == "mic") {
            ConfirmDialog(title = "语音输入", message = "未检测到语音输入设备", onCancel = { dialog = null }, onConfirm = { dialog = null }, confirmLabel = "好")
        }
        if (shareVisible) {
            ConfirmDialog(title = "分享", message = "已准备好分享内容", onCancel = { shareVisible = false }, onConfirm = { shareVisible = false }, confirmLabel = "好")
        }
    }
}

@Composable
fun TopBar(
    title: String,
    searching: Boolean,
    query: String,
    onQuery: (String) -> Unit,
    selectionOpen: Boolean,
    selectedCount: Int,
    onBack: () -> Unit,
    onGear: () -> Unit,
    onSelectionOverflow: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (searching || selectionOpen) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = MaterialTheme.colorScheme.onSurface)
            }
        }
        if (searching) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) Text("搜索", color = Color(0xFF9AA0A6), fontSize = 20.sp)
                BasicTextField(
                    value = query,
                    onValueChange = onQuery,
                    textStyle = TextStyle(fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQuery("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "清除", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        } else if (selectionOpen) {
            Text("$selectedCount", fontSize = 26.sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { }) { TagGlyph(MaterialTheme.colorScheme.onSurface) }
            IconButton(onClick = { }) { ListGlyph(MaterialTheme.colorScheme.onSurface) }
            IconButton(onClick = { }) { ClockGlyph(MaterialTheme.colorScheme.onSurface) }
            IconButton(onClick = { }) { FlagGlyph(MaterialTheme.colorScheme.onSurface) }
            IconButton(onClick = onSelectionOverflow) {
                Icon(Icons.Filled.MoreVert, contentDescription = "更多", tint = MaterialTheme.colorScheme.onSurface)
            }
        } else {
            Text(title, fontSize = 30.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onGear) {
                Icon(Icons.Filled.Settings, contentDescription = "设置", tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun ReminderBanner(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(BannerGray)
            .padding(20.dp)
    ) {
        Column {
            Text("启用提醒", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(10.dp))
            Text("提醒在 Android 设置中被禁用", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onClose) { Text("关闭", fontSize = 17.sp, color = DeepBlue) }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(DeepBlue)
                        .clickable { onClose() }
                        .padding(horizontal = 28.dp, vertical = 10.dp)
                ) { Text("设置", color = Color.White, fontSize = 17.sp) }
            }
        }
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(130.dp).clip(RoundedCornerShape(28.dp)).border(8.dp, Color(0xFFBDBDBD), RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().height(34.dp).align(Alignment.BottomCenter)
                    .background(Color(0xFFBDBDBD))
            )
        }
        Spacer(Modifier.height(24.dp))
        Text("这里没有任务哦。", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(200.dp))
    }
}

@Composable
fun SectionHeader(title: String, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onToggle() }.padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 19.sp, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.weight(1f))
        Text(if (expanded) "⌃" else "⌄", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun TaskBlock(
    task: TaskRec,
    selectionOpen: Boolean,
    selected: Boolean,
    subtaskCollapsed: Boolean,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onLong: () -> Unit,
    onSelectToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) Color(0xFFE0E0E0) else Color.Transparent)
    ) {
        TaskRow(
            title = task.title,
            priority = task.priority,
            completed = task.isCompleted,
            tags = task.tags,
            badge = if (task.subtasks.isNotEmpty()) "⌃ ${task.subtasks.size}" else null,
            selectionOpen = selectionOpen,
            selected = selected,
            onClick = { if (selectionOpen) onSelectToggle() else onOpen() },
            onToggle = { if (selectionOpen) onSelectToggle() else onToggle() },
            onLong = onLong
        )
        if (!subtaskCollapsed) {
            task.subtasks.forEach { s ->
                Column(modifier = Modifier.padding(start = 56.dp)) {
                    TaskRow(
                        title = s.title,
                        priority = s.priority,
                        completed = !s.completedAt.isNullOrEmpty(),
                        tags = emptyList(),
                        badge = null,
                        selectionOpen = false,
                        selected = false,
                        onClick = { },
                        onToggle = {
                            s.completedAt = if (s.completedAt.isNullOrEmpty()) nowStamp() else null
                        },
                        onLong = { }
                    )
                }
            }
        }
    }
}

@Composable
fun TaskRow(
    title: String,
    priority: Int,
    completed: Boolean,
    tags: List<String>,
    badge: String?,
    selectionOpen: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onLong: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .pointerInput(selectionOpen) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLong() }
                )
            },
        verticalAlignment = Alignment.Top
    ) {
        CheckBox(priority = priority, completed = completed, onClick = onToggle)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title.ifEmpty { "无标题" },
                fontSize = 21.sp,
                color = if (completed) Color(0xFF9AA0A6) else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (completed) TextDecoration.LineThrough else null
            )
            if (badge != null && !completed) {
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE0E0E0))
                        .padding(horizontal = 10.dp, vertical = 2.dp)
                ) { Text(badge, fontSize = 15.sp, color = Color(0xFF5F6368)) }
            }
            if (tags.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tags.forEach { tag ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFE8E8E8))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TagGlyph(Color(0xFF5F6368), 16.dp)
                            Spacer(Modifier.width(5.dp))
                            Text(tag, fontSize = 15.sp, color = Color(0xFF3C4043))
                        }
                    }
                }
            }
        }
        if (badge == null && tags.isEmpty()) {
            Text(humanDate(dueDate(null)), color = Color.Transparent, fontSize = 1.sp)
        }
    }
}

@Composable
fun CheckBox(priority: Int, completed: Boolean, onClick: () -> Unit) {
    val color = priorityColor(priority)
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(6.dp))
            .border(2.dp, color, RoundedCornerShape(6.dp))
            .background(if (completed) color else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (completed) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun BottomBar(
    onMenu: () -> Unit,
    onSearch: () -> Unit,
    onSort: () -> Unit,
    onMic: () -> Unit,
    onMore: () -> Unit,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(76.dp).background(BottomBarColor).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val tint = MaterialTheme.colorScheme.onSurface
        IconButton(onClick = onMenu) { Icon(Icons.Filled.Menu, contentDescription = "菜单", tint = tint) }
        IconButton(onClick = onSearch) { Icon(Icons.Filled.Search, contentDescription = "搜索", tint = tint) }
        IconButton(onClick = onSort) { SortGlyph(tint) }
        IconButton(onClick = onMic) { MicGlyph(tint) }
        IconButton(onClick = onMore) { Icon(Icons.Filled.MoreVert, contentDescription = "更多", tint = tint) }
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(FabBlue)
                .clickable { onAdd() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Add, contentDescription = "新建", tint = Color.White, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
fun DrawerContent(route: Route, onSelect: (Route?) -> Unit, onCloseDrawer: () -> Unit) {
    val revision = Store.revision
    Column(
        modifier = Modifier
            .width(300.dp)
            .fillMaxSize()
            .background(DrawerBg)
            .padding(10.dp)
    ) {
        DrawerRow(
            icon = { ListGlyph(MaterialTheme.colorScheme.onSurface, 22.dp) },
            label = "我的任务",
            count = Store.tasks.count { !it.isCompleted },
            selected = route is Route.MyTasks,
            onClick = { onSelect(Route.MyTasks) }
        )
        Spacer(Modifier.height(10.dp))
        DrawerCard {
            DrawerRow(
                icon = { SortGlyph(MaterialTheme.colorScheme.onSurface, 22.dp) },
                label = "过滤器",
                count = null,
                selected = false,
                onClick = { },
                trailing = "⌃",
                onAdd = { }
            )
            DrawerRow(
                icon = { ClockGlyph(MaterialTheme.colorScheme.onSurface) },
                label = "今天",
                count = null,
                selected = route is Route.Today,
                onClick = { onSelect(Route.Today) }
            )
            DrawerRow(
                icon = { ClockGlyph(MaterialTheme.colorScheme.onSurface) },
                label = "最近修改过的",
                count = Store.tasks.size,
                selected = route is Route.Recent,
                onClick = { onSelect(Route.Recent) }
            )
        }
        Spacer(Modifier.height(10.dp))
        DrawerCard {
            DrawerRow(
                icon = { TagGlyph(MaterialTheme.colorScheme.onSurface, 22.dp) },
                label = "标签",
                count = null,
                selected = false,
                onClick = { },
                trailing = "⌃",
                onAdd = { }
            )
            Store.allTags().forEach { tag ->
                DrawerRow(
                    icon = { TagGlyph(MaterialTheme.colorScheme.onSurface, 20.dp) },
                    label = tag,
                    count = Store.tagCount(tag),
                    selected = route is Route.Tag && route.name == tag,
                    onClick = { onSelect(Route.Tag(tag)) }
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        DrawerCard {
            DrawerRow(
                icon = { PinGlyph(MaterialTheme.colorScheme.onSurface) },
                label = "地点",
                count = null,
                selected = false,
                onClick = { },
                onAdd = { }
            )
        }
        Spacer(Modifier.height(10.dp))
        DrawerCard {
            DrawerRow(
                icon = { ListGlyph(MaterialTheme.colorScheme.onSurface, 22.dp) },
                label = "本地清单",
                count = null,
                selected = false,
                onClick = { },
                trailing = "⌃",
                onAdd = { }
            )
            Store.lists.forEach { l ->
                DrawerRow(
                    icon = { ListGlyph(MaterialTheme.colorScheme.onSurface, 20.dp) },
                    label = l.name,
                    count = Store.listCount(l.id),
                    selected = route is Route.List && route.id == l.id,
                    onClick = { onSelect(Route.List(l.id)) }
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier.align(Alignment.End).size(60.dp).clip(RoundedCornerShape(18.dp)).background(DeepBlue).clickable { },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = Color.White)
        }
    }
}

@Composable
fun DrawerCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardBg)
            .padding(vertical = 6.dp)
    ) { content() }
}

@Composable
fun DrawerRow(
    icon: @Composable () -> Unit,
    label: String,
    count: Int?,
    selected: Boolean,
    onClick: () -> Unit,
    trailing: String? = null,
    onAdd: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) Color(0xFFE3E3E9) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) { icon() }
        Spacer(Modifier.width(16.dp))
        Text(label, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        if (count != null) Text("$count", fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (trailing != null) Text(trailing, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 18.dp))
        if (onAdd != null) {
            Box(modifier = Modifier.padding(start = 18.dp).size(24.dp).clickable { onAdd() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Add, contentDescription = "添加", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
fun SortPanel(
    config: SortConfig,
    onChange: (SortConfig) -> Unit,
    onDismiss: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0x88000000)).clickable { onDismiss() }) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Color(0xFFF3F2F7))
                .clickable { }
                .padding(vertical = 12.dp)
        ) {
            Box(modifier = Modifier.align(Alignment.CenterHorizontally).width(40.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF9AA0A6)))
            Spacer(Modifier.height(16.dp))
            PanelRow(label = "分组", value = config.groupBy, arrow = if (config.groupAsc) "↑ 升序" else "↓ 降序",
                onClick = { onChange(config.copy(groupBy = nextOf(config.groupBy, listOf("按截止日期", "按优先级", "按标题")), groupAsc = if (config.groupBy == "按截止日期") !config.groupAsc else config.groupAsc)) })
            PanelRow(label = "排序", value = config.sortBy, arrow = if (config.sortAsc) "↑ 升序" else "↓ 降序",
                onClick = { onChange(config.copy(sortBy = nextOf(config.sortBy, listOf("按截止日期", "按优先级", "按标题")), sortAsc = !config.sortAsc)) })
            PanelRow(label = "子任务", value = "我的顺序", arrow = null, onClick = { })
            Spacer(Modifier.height(8.dp))
            ConfigToggle(label = "显示未开始项目", checked = true) { }
            ConfigToggle(label = "显示已完成任务", checked = false) { }
            ConfigToggle(label = "显示已完成的子任务", checked = true) { }
            ConfigToggle(label = "将已完成任务移至底部", checked = true) { }
            PanelRow(label = "已完成", value = config.completedBy, arrow = if (config.completedAsc) "↑ 升序" else "↓ 降序",
                onClick = { onChange(config.copy(completedAsc = !config.completedAsc)) })
        }
    }
}

private fun nextOf(cur: String, options: List<String>): String {
    val i = options.indexOf(cur)
    return options[(i + 1) % options.size]
}

@Composable
fun PanelRow(label: String, value: String, arrow: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 21.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(value, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (arrow != null) Text(arrow, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun ConfigToggle(label: String, checked: Boolean, onChange: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onChange() }.padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 19.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .width(52.dp)
                .height(30.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(if (checked) DeepBlue else Color(0xFFC7C7CC))
                .padding(3.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color.White))
        }
    }
}

@Composable
fun MoreMenu(
    onDismiss: () -> Unit,
    onClearCompleted: () -> Unit,
    onCollapseSub: () -> Unit,
    onExpandSub: () -> Unit,
    onShare: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().clickable { onDismiss() }) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 40.dp, bottom = 200.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFF6F5FA))
                .width(180.dp)
                .clickable { }
        ) {
            MenuItem("清除已完成项", onClearCompleted)
            MenuItem("收起子任务", onCollapseSub)
            MenuItem("展开子任务", onExpandSub)
            MenuItem("分享", onShare)
        }
    }
}

@Composable
fun SelectionOverflow(
    onDismiss: () -> Unit,
    onSelectAll: () -> Unit,
    onShare: () -> Unit,
    onClone: () -> Unit,
    onDelete: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().clickable { onDismiss() }) {
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 72.dp, end = 12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFF6F5FA))
                .width(180.dp)
                .clickable { }
        ) {
            MenuItem("全选", onSelectAll)
            MenuItem("分享", onShare)
            MenuItem("克隆", onClone)
            MenuItem("删除", onDelete)
        }
    }
}

@Composable
fun MenuItem(label: String, onClick: () -> Unit) {
    Text(
        label,
        fontSize = 19.sp,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 18.dp, vertical = 16.dp)
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String?,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    confirmLabel: String = "确定"
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title, fontSize = 22.sp) },
        text = { if (message != null) Text(message, fontSize = 17.sp) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel, fontSize = 18.sp) } },
        dismissButton = { TextButton(onClick = onCancel) { Text("取消", fontSize = 18.sp) } }
    )
}
