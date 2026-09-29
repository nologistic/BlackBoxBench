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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

val ACCENT = Color(0xFF009688)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(app: AppState) {
    val drawerState = rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var selection by remember { mutableStateOf(setOf<String>()) }
    var menuOpen by remember { mutableStateOf(false) }
    var sortDialog by remember { mutableStateOf(false) }
    var renameDialog by remember { mutableStateOf(false) }
    var deleteListDialog by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(1) } // 0全部 1未完成 2已完成

    LaunchedEffect(app.lastCompleted) {
        val t = app.lastCompleted ?: return@LaunchedEffect
        val r = snackbar.showSnackbar("任务已完成", actionLabel = "撤销")
        if (r == androidx.compose.material3.SnackbarResult.ActionPerformed) app.undoComplete()
        app.lastCompleted = null
    }

    val view = app.view
    val listView = view as? MainView.ListView
    val tagView = view as? MainView.TagView
    val filterView = view as? MainView.FilterView
    val list = listView?.let { app.data.listById(it.listId) }
    val barColor = when {
        list != null -> Color(list.color)
        tagView != null -> ACCENT
        filterView != null -> Color(0xFF5C6BC0)
        else -> ACCENT
    }
    val title = when {
        list != null -> list.name
        tagView != null -> "#${tagView.tag}"
        filterView != null -> app.data.filters.firstOrNull { it.id == filterView.filterId }?.name ?: "过滤器"
        else -> "任务"
    }

    val tasks = remember(app.data, view, app.query, app.sortKey, app.sortAscending, tab, app.data.hideCompleted) {
        var ts = when (view) {
            is MainView.ListView -> app.data.tasksOfList(view.listId)
            is MainView.TagView -> app.data.tasksWithTag(view.tag)
            is MainView.FilterView -> {
                val f = app.data.filters.firstOrNull { it.id == view.filterId }
                app.data.tasks.filter { t ->
                    (f == null) || ((f.today && dueBucket(t.due) in 0..1 && !t.isCompleted) || (f.highPriority && t.priority == Priority.HIGH))
                }
            }
            MainView.Tasks -> app.data.tasks
        }
        if (app.searchMode && app.query.isNotEmpty()) ts = ts.filter { it.title.contains(app.query, ignoreCase = true) }
        if (tab == 2) ts = ts.filter { it.isCompleted }
        else {
            if (tab == 1 || app.data.hideCompleted) ts = ts.filter { !it.isCompleted }
        }
        val cmp = when (app.sortKey) {
            "due" -> compareBy { dueBucket(it.due) * 100 + (parseDue(it.due)?.dayOfMonth ?: 99) }
            "priority" -> compareByDescending<TaskItem> { it.priority.ordinal }
            "title" -> compareBy { it.title }
            "created" -> compareBy { it.createdAt }
            else -> compareBy { it.order }
        }
        val ordered = ts.sortedWith(cmp)
        if (app.sortAscending) ordered else ordered.reversed()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(320.dp)) {
                DrawerContent(app, onSelect = {
                    app.view = it; app.searchMode = false; app.query = ""
                    scope.launch { drawerState.close() }
                })
            }
        },
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                if (app.searchMode) {
                    SearchBar(app)
                } else if (selection.isNotEmpty()) {
                    SelectionBar(count = selection.size,
                        onDelete = { app.deleteTasks(selection.toList()); selection = emptySet() },
                        onClose = { selection = emptySet() })
                } else {
                    TopAppBar(
                        title = { Text(title, color = Color.White) },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, "菜单", tint = Color.White)
                            }
                        },
                        actions = {
                            IconButton(onClick = { app.searchMode = true; tab = 0 }) {
                                Icon(Icons.Default.Search, "搜索", tint = Color.White)
                            }
                            if (listView != null || tagView != null || filterView != null || view == MainView.Tasks) {
                                IconButton(onClick = { menuOpen = true }) {
                                    Icon(Icons.Default.MoreVert, "更多", tint = Color.White)
                                }
                                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                    DropdownMenuItem(
                                        text = { Text("排序") },
                                        onClick = { menuOpen = false; sortDialog = true },
                                    )
                                    val hideLabel = if (app.data.hideCompleted) "显示已完成" else "隐藏已完成"
                                    DropdownMenuItem(
                                        text = { Text(hideLabel) },
                                        onClick = {
                                            menuOpen = false
                                            app.update { it.copy(hideCompleted = !it.hideCompleted) }
                                        },
                                    )
                                    if (list != null) {
                                        DropdownMenuItem(
                                            text = { Text("重命名列表") },
                                            onClick = { menuOpen = false; renameDialog = true },
                                        )
                                        DropdownMenuItem(
                                            text = { Text("删除列表") },
                                            onClick = { menuOpen = false; deleteListDialog = true },
                                        )
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = barColor),
                    )
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        app.navigate(Screen.Editor(null, (view as? MainView.ListView)?.listId))
                    },
                    containerColor = barColor,
                    contentColor = Color.White,
                ) { Icon(Icons.Default.Add, "新建任务") }
            },
            containerColor = Color(0xFFFAFAFA),
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                // 列表视图的 tabs（搜索模式下隐藏）
                if (!app.searchMode && (listView != null || tagView != null || filterView != null)) {
                    val titles = listOf("全部", "未完成", "已完成")
                    TabRow(
                        selectedTabIndex = tab,
                        containerColor = barColor,
                        contentColor = Color.White,
                    ) {
                        titles.forEachIndexed { i, label ->
                            Tab(
                                selected = tab == i,
                                onClick = { tab = i },
                                text = { Text(label, color = Color.White) },
                            )
                        }
                    }
                }

                if (tasks.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            if (app.searchMode && app.query.isNotEmpty()) "没有匹配的任务"
                            else "没有任务。点按 + 创建一个！",
                            color = Color(0xFF9E9E9E), fontSize = 15.sp,
                        )
                    }
                } else if (view == MainView.Tasks && !app.searchMode) {
                    // 主视图按日期分组
                    val grouped = tasks.groupBy { dueBucket(it.due) }
                    val headers = listOf(0 to "已过期", 1 to "今天", 2 to "明天", 3 to "本周", 4 to "更晚", 5 to "无日期")
                    LazyColumn(Modifier.fillMaxSize()) {
                        headers.filter { it.first in grouped.keys }.forEach { (bucket, label) ->
                            val items = grouped[bucket]!!
                            item(key = "h_$bucket") {
                                DateHeader(label, items.size)
                            }
                            items(items, key = { it.id }) { task ->
                                TaskRow(
                                    app = app,
                                    task = task,
                                    selected = selection.contains(task.id),
                                    selectionMode = selection.isNotEmpty(),
                                    showListDot = true,
                                    onToggle = {
                                        if (selection.isNotEmpty()) {
                                            selection = if (selection.contains(task.id)) selection - task.id else selection + task.id
                                        } else app.toggleComplete(task.id)
                                    },
                                    onLongPress = { selection = setOf(task.id) },
                                    onClick = { app.navigate(Screen.Editor(task.id, null)) },
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(tasks, key = { it.id }) { task ->
                            TaskRow(
                                app = app,
                                task = task,
                                selected = selection.contains(task.id),
                                selectionMode = selection.isNotEmpty(),
                                showListDot = view is MainView.TagView || view is MainView.FilterView,
                                onToggle = {
                                    if (selection.isNotEmpty()) {
                                        selection = if (selection.contains(task.id)) selection - task.id else selection + task.id
                                    } else app.toggleComplete(task.id)
                                },
                                onLongPress = { selection = setOf(task.id) },
                                onClick = { app.navigate(Screen.Editor(task.id, null)) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (sortDialog) {
        SortDialog(
            initialKey = app.sortKey,
            initialAscending = app.sortAscending,
            onApply = { k, asc -> app.sortKey = k; app.sortAscending = asc; sortDialog = false },
            onDismiss = { sortDialog = false },
        )
    }
    if (renameDialog && list != null) {
        RenameListDialog(list.name, onConfirm = {
            app.renameList(list.id, it); renameDialog = false
        }, onDismiss = { renameDialog = false })
    }
    if (deleteListDialog && list != null) {
        AlertDialog(
            onDismissRequest = { deleteListDialog = false },
            title = { Text("删除列表") },
            text = { Text("确定要删除列表“${list.name}”及其所有任务吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(onClick = { app.deleteList(list.id); deleteListDialog = false }) {
                    Text("删除", color = Color(0xFFE53935))
                }
            },
            dismissButton = { TextButton(onClick = { deleteListDialog = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun DateHeader(label: String, count: Int) {
    val today = LocalDate.now()
    val detail = when (label) {
        "今天" -> " ${today.monthValue}月${today.dayOfMonth}日 ${WEEKDAY_CN[today.dayOfWeek]}"
        "明天" -> " ${today.plusDays(1).monthValue}月${today.plusDays(1).dayOfMonth}日 ${WEEKDAY_CN[today.plusDays(1).dayOfWeek]}"
        else -> ""
    }
    Row(
        Modifier.fillMaxWidth().background(Color(0xFFEEEEEE)).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$label$detail", color = Color(0xFF616161), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(8.dp))
        Text("$count", color = Color(0xFF9E9E9E), fontSize = 13.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(app: AppState) {
    TopAppBar(
        title = {
            OutlinedTextField(
                value = app.query,
                onValueChange = { app.query = it },
                placeholder = { Text("搜索任务", color = Color(0xFFBDBDBD)) },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                ),
            )
        },
        navigationIcon = {
            IconButton(onClick = { app.searchMode = false; app.query = "" }) {
                Icon(Icons.Default.ArrowBack, "返回", tint = Color.White)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = ACCENT),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionBar(count: Int, onDelete: () -> Unit, onClose: () -> Unit) {
    TopAppBar(
        title = { Text("已选 $count", color = Color.White) },
        navigationIcon = {
            IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, "关闭", tint = Color.White) }
        },
        actions = {
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "删除", tint = Color.White) }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF424242)),
    )
}

/** 单条任务行 */
@Composable
fun TaskRow(
    app: AppState,
    task: TaskItem,
    selected: Boolean,
    selectionMode: Boolean,
    showListDot: Boolean,
    onToggle: () -> Unit,
    onLongPress: () -> Unit,
    onClick: () -> Unit,
) {
    val listColor = Color(app.data.listById(task.listId)?.color ?: 0xFF009688)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) Color(0xFFB2DFDB) else Color.Transparent)
            .combinedClickableCompat(onClick = onClick, onLongClick = onLongPress)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectionMode) {
            Checkbox(
                checked = selected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = ACCENT),
                modifier = Modifier.size(32.dp),
            )
        } else {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = listColor, checkmarkColor = Color.White,
                ),
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (task.priority == Priority.HIGH) {
                    Box(
                        Modifier.size(16.dp).background(Color(0xFFE53935), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Text("!", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(6.dp))
                } else if (task.priority == Priority.MEDIUM) {
                    Box(Modifier.size(10.dp).background(Color(0xFFFBC02D), CircleShape))
                    Spacer(Modifier.width(6.dp))
                } else if (task.priority == Priority.LOW) {
                    Box(Modifier.size(10.dp).background(Color(0xFF42A5F5), CircleShape))
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    task.title,
                    fontSize = 16.sp,
                    color = Color(0xFF212121),
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                )
            }
            // 副行：截止日期 / 重复 / 提醒 / 标签 / 子任务数
            val meta = buildList {
                formatDue(task.due)?.let { add(it) }
                if (task.repeat.isNotEmpty()) add(repeatLabel(task.repeat))
                if (task.reminder != null) add(formatDue(task.reminder) ?: "提醒")
                if (task.subtasks.isNotEmpty()) {
                    val done = task.subtasks.count { !it.completedAt.isNullOrEmpty() }
                    add("$done/${task.subtasks.size} 个子任务")
                }
                task.tags.take(3).forEach { add("#$it") }
            }
            if (meta.isNotEmpty()) {
                Text(meta.joinToString("  "), fontSize = 12.sp, color = Color(0xFF9E9E9E), modifier = Modifier.padding(top = 2.dp))
            }
        }
        if (showListDot) {
            Box(Modifier.size(12.dp).background(listColor, CircleShape))
        }
    }
}

/** 抽屉导航 */
@Composable
fun DrawerContent(app: AppState, onSelect: (MainView) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().background(Color.White)) {
        item {
            Column(Modifier.padding(16.dp)) {
                Text("Tasks", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121))
                Spacer(Modifier.height(2.dp))
                Text("本地账户（不同步）", fontSize = 12.sp, color = Color(0xFF9E9E9E))
            }
        }
        item {
            NavigationDrawerItem(
                label = { Text("任务") },
                selected = app.view == MainView.Tasks,
                onClick = { onSelect(MainView.Tasks) },
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
        item {
            SectionHeader("标签")
        }
        items(app.data.allTags, key = { "tag_$it" }) { tag ->
            NavigationDrawerItem(
                label = { Text("#$tag") },
                selected = app.view is MainView.TagView && (app.view as MainView.TagView).tag == tag,
                onClick = { onSelect(MainView.TagView(tag)) },
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 24.dp, top = 14.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("列表", fontSize = 13.sp, color = Color(0xFF9E9E9E), modifier = Modifier.weight(1f))
                TextButton(onClick = { app.drawerOpen = false; app.navigate(Screen.NewList) }) {
                    Icon(Icons.Default.Add, null, tint = ACCENT, modifier = Modifier.size(18.dp))
                }
            }
        }
        items(app.data.lists, key = { it.id }) { list ->
            NavigationDrawerItem(
                label = { Text(list.name) },
                icon = { Box(Modifier.size(14.dp).background(Color(list.color), CircleShape)) },
                badge = { Text("${app.data.openCount(list.id)}") },
                selected = app.view == MainView.ListView(list.id),
                onClick = { onSelect(MainView.ListView(list.id)) },
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
        item { SectionHeader("过滤器") }
        items(app.data.filters, key = { it.id }) { f ->
            NavigationDrawerItem(
                label = { Text(f.name) },
                selected = app.view == MainView.FilterView(f.id),
                onClick = { onSelect(MainView.FilterView(f.id)) },
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
        item {
            TextButton(
                onClick = { app.navigate(Screen.NewFilter) },
                modifier = Modifier.padding(start = 16.dp),
            ) { Text("+ 新建过滤器", color = ACCENT) }
        }
        item {
            Spacer(Modifier.height(16.dp))
            NavigationDrawerItem(
                label = { Text("设置") },
                icon = { Icon(Icons.Default.Settings, null, tint = Color(0xFF757575)) },
                selected = false,
                onClick = { app.drawerOpen = false; app.navigate(Screen.Settings) },
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        fontSize = 13.sp,
        color = Color(0xFF9E9E9E),
        modifier = Modifier.padding(start = 24.dp, top = 14.dp, bottom = 4.dp),
    )
}
