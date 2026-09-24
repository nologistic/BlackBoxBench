package com.blackboxbench.reproduction

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    store: Store,
    onEditTask: (String?) -> Unit,
    onOpenSettings: () -> Unit
) {
    store.version.value
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var view by remember { mutableStateOf("all") }
    var searchMode by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    var sortOpen by remember { mutableStateOf(false) }
    var newListOpen by remember { mutableStateOf(false) }
    var selection by remember { mutableStateOf(setOf<String>()) }
    var confirmDeleteSelection by remember { mutableStateOf(false) }

    fun titleFor(v: String): String = when {
        v == "all" -> "任务"
        v == "today" -> "今日"
        v == "recent" -> "最近"
        v == "later" -> "稍后"
        v.startsWith("list:") -> store.listName(v.removePrefix("list:"))
        v.startsWith("tag:") -> v.removePrefix("tag:")
        else -> "任务"
    }

    fun open(v: String) {
        view = v
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                Column(
                    Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "Tasks",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 20.dp, top = 28.dp, bottom = 4.dp)
                    )
                    Text(
                        "本地账户（离线）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 20.dp, bottom = 12.dp)
                    )
                    HorizontalDivider()
                    DrawerSectionLabel("我的过滤器")
                    DrawerItem("今日", store.tagBadgeToday(), view == "today") { open("today") }
                    DrawerItem("最近", store.tasksFor("recent").size, view == "recent") { open("recent") }
                    DrawerItem("稍后", store.tasksFor("later").size, view == "later") { open("later") }
                    HorizontalDivider(Modifier.padding(top = 8.dp))
                    Row(
                        Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DrawerSectionLabel("列表", Modifier.weight(1f))
                        IconButton(onClick = { newListOpen = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Add, "新建列表", modifier = Modifier.size(18.dp))
                        }
                    }
                    for (list in store.data.lists) {
                        DrawerItem(list.name, store.listBadge(list.id),
                            view == "list:" + list.id) { open("list:" + list.id) }
                    }
                    HorizontalDivider(Modifier.padding(top = 8.dp))
                    DrawerSectionLabel("标签")
                    for (tag in store.allTags()) {
                        DrawerItem(tag, store.tagBadge(tag), view == "tag:" + tag) {
                            open("tag:" + tag)
                        }
                    }
                    HorizontalDivider(Modifier.padding(top = 8.dp))
                    DrawerItem("设置", null, false, Icons.Default.Settings) { onOpenSettings() }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                when {
                    selection.isNotEmpty() -> {
                        TopAppBar(
                            title = { Text("已选择 ${selection.size} 项") },
                            navigationIcon = {
                                IconButton(onClick = { selection = emptySet() }) {
                                    Icon(Icons.Default.Close, null)
                                }
                            },
                            actions = {
                                IconButton(onClick = { confirmDeleteSelection = true }) {
                                    Icon(Icons.Default.Delete, "删除")
                                }
                            }
                        )
                    }
                    searchMode -> {
                        TopAppBar(
                            title = {
                                OutlinedTextField(
                                    value = query,
                                    onValueChange = { query = it },
                                    placeholder = { Text("搜索…") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = {
                                    searchMode = false; query = ""
                                }) { Icon(Icons.Default.Close, null) }
                            }
                        )
                    }
                    else -> {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(titleFor(view))
                                    if (view == "today") {
                                        Text(
                                            todaySubtitle(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, null)
                                }
                            },
                            actions = {
                                IconButton(onClick = { searchMode = true }) {
                                    Icon(Icons.Default.Search, "搜索")
                                }
                                IconButton(onClick = { menuOpen = true }) {
                                    Icon(Icons.Default.MoreVert, "更多")
                                }
                                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                    DropdownMenuItem(
                                        text = { Text("搜索") },
                                        leadingIcon = { Icon(Icons.Default.Search, null) },
                                        onClick = { menuOpen = false; searchMode = true }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Checkbox(
                                                    checked = !store.data.prefs.hideCompleted,
                                                    onCheckedChange = null
                                                )
                                                Text("显示已完成")
                                            }
                                        },
                                        onClick = {
                                            store.setHideCompleted(!store.data.prefs.hideCompleted)
                                            menuOpen = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("排序方式") },
                                        onClick = { menuOpen = false; sortOpen = true }
                                    )
                                }
                            }
                        )
                    }
                }
            },
            floatingActionButton = {
                if (selection.isEmpty()) {
                    FloatingActionButton(onClick = { onEditTask(null) }) {
                        Icon(Icons.Default.Add, "新建任务")
                    }
                }
            }
        ) { padding ->
            val tasks: List<Task> =
                if (searchMode && query.isNotBlank())
                    store.data.tasks.filter { it.title.contains(query, ignoreCase = true) }
                else store.tasksFor(view)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp)
            ) {
                if (tasks.isEmpty()) {
                    item { EmptyTasksPlaceholder(padding) }
                } else if (view == "all" && !searchMode) {
                    for (list in store.data.lists) {
                        val lt = tasks.filter { it.listId == list.id }
                        if (lt.isNotEmpty()) {
                            item(key = "header_" + list.id) {
                                Row(
                                    Modifier.fillMaxWidth()
                                        .padding(start = 68.dp, top = 14.dp, bottom = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        list.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        store.listBadge(list.id).toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            items(lt.size, key = { lt[it].id }) { i ->
                                TaskRow(store, lt[i], selection,
                                    onSelect = { id ->
                                        selection = if (id in selection) selection - id
                                        else selection + id
                                    },
                                    onToggle = { id -> handleToggle(store, snackbar, id) },
                                    onOpen = { id ->
                                        if (selection.isNotEmpty()) {
                                            selection = if (id in selection) selection - id
                                            else selection + id
                                        } else onEditTask(id)
                                    })
                            }
                        }
                    }
                } else {
                    items(tasks.size, key = { tasks[it].id }) { i ->
                        TaskRow(store, tasks[i], selection,
                            onSelect = { id ->
                                selection = if (id in selection) selection - id
                                else selection + id
                            },
                            onToggle = { id -> handleToggle(store, snackbar, id) },
                            onOpen = { id ->
                                if (selection.isNotEmpty()) {
                                    selection = if (id in selection) selection - id
                                    else selection + id
                                } else onEditTask(id)
                            })
                    }
                }
            }
        }
    }

    if (newListOpen) {
        TextFieldDialog(
            title = "新建列表",
            placeholder = "名称",
            confirmText = "确定",
            onDismiss = { newListOpen = false },
            onConfirm = { name ->
                if (name.isNotBlank()) store.addList(name.trim())
                newListOpen = false
            }
        )
    }

    if (sortOpen) {
        val options = listOf(
            "my" to "我的排序", "due" to "到期日期",
            "priority" to "优先级", "title" to "标题"
        )
        var chosen by remember { mutableStateOf(store.data.prefs.sort) }
        AlertDialog(
            onDismissRequest = { sortOpen = false },
            title = { Text("排序方式") },
            text = {
                Column {
                    for ((key, label) in options) {
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable { chosen = key }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = chosen == key, onClick = { chosen = key })
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { store.setSort(chosen); sortOpen = false }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { sortOpen = false }) { Text("取消") }
            }
        )
    }

    if (confirmDeleteSelection) {
        AlertDialog(
            onDismissRequest = { confirmDeleteSelection = false },
            title = { Text("删除所选任务？") },
            text = { Text("将删除 ${selection.size} 个任务") },
            confirmButton = {
                TextButton(onClick = {
                    store.deleteTasks(selection)
                    selection = emptySet()
                    confirmDeleteSelection = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteSelection = false }) { Text("取消") }
            }
        )
    }
}

private fun Store.tagBadgeToday(): Int =
    data.tasks.count {
        it.completedAt.isEmpty() && dueDate(it.due)?.isEqual(java.time.LocalDate.now()) == true
    }

private suspend fun handleToggle(store: Store, snackbar: SnackbarHostState, id: String) {
    val result = store.toggleTask(id)
    when (result) {
        ToggleResult.COMPLETED -> {
            val r = snackbar.showSnackbar("已完成 1 项任务", actionLabel = "撤销")
            if (r == SnackbarResult.ActionPerformed) store.toggleTask(id)
        }
        ToggleResult.RECURRING -> {
            val t = store.task(id)
            if (t != null) snackbar.showSnackbar("已重复：下一次出现 ${formatDue(t.due)}")
        }
        else -> Unit
    }
}

@Composable
private fun EmptyTasksPlaceholder(padding: androidx.compose.foundation.layout.PaddingValues) {
    Column(
        Modifier.fillMaxSize().padding(padding).padding(bottom = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(96.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(96.dp)
            ) {}
            Icon(
                Icons.Default.Check, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
        }
        Spacer(Modifier.height(18.dp))
        Text("恭喜，您已完成所有任务！")
    }
}

@Composable
private fun DrawerSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DrawerItem(
    text: String,
    badge: Int?,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    val bg = if (selected) MaterialTheme.colorScheme.secondaryContainer
    else androidx.compose.ui.graphics.Color.Transparent
    Surface(color = bg, onClick = onClick) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(14.dp))
            }
            Text(text, modifier = Modifier.weight(1f), fontSize = 15.sp)
            if (badge != null && badge > 0) Badge { Text(badge.toString()) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskRow(
    store: Store,
    task: Task,
    selection: Set<String>,
    onSelect: (String) -> Unit,
    onToggle: suspend (String) -> Unit,
    onOpen: (String) -> Unit
) {
    store.version.value
    val scope = rememberCoroutineScope()
    val completed = task.completedAt.isNotEmpty()
    val overdue = !completed && dueDate(task.due)?.isBefore(java.time.LocalDate.now()) == true
    val selected = task.id in selection

    Surface(
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else androidx.compose.ui.graphics.Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .combinedClickable(
                    onClick = { onOpen(task.id) },
                    onLongClick = { onSelect(task.id) }
                )
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = completed,
                onCheckedChange = { scope.launch { onToggle(task.id) } },
                modifier = Modifier.padding(end = 8.dp)
            )
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val marks = priorityMarks(task.priority)
                    if (marks.isNotEmpty()) {
                        val markColor = if (task.priority == "high") Color(0xFFCC3A3A)
                        else Color(0xFFC77F00)
                        Text(marks, color = markColor, fontWeight = FontWeight.Bold,
                            fontSize = 13.sp)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        task.title,
                        fontSize = 16.sp,
                        textDecoration = if (completed) TextDecoration.LineThrough else null,
                        color = if (completed) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
                val meta = buildList {
                    if (task.due.isNotBlank()) add(formatDue(task.due))
                    if (task.repeat.isNotBlank()) add(repeatLabel(task.repeat))
                    task.tags.forEach { add("#$it") }
                    subtaskSummary(task)?.let { add("子任务 $it") }
                }
                if (meta.isNotEmpty()) {
                    Text(
                        meta.joinToString("  ·  "),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (overdue) Color(0xFFCC3A3A)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
