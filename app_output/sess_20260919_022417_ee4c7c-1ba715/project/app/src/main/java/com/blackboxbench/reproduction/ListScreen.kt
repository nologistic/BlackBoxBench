package com.blackboxbench.reproduction

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// ===================== 任务勾选框 =====================
@Composable
fun TaskCheckbox(task: Task, onToggle: () -> Unit) {
    Store.version.value
    val color = if (task.isRepeating) AppColors.Blue else priorityColor(task.priority)
    Box(
        Modifier.size(26.dp).clip(RoundedCornerShape(4.dp))
            .then(
                if (task.isCompleted) Modifier.background(AppColors.Blue)
                else Modifier.border(2.dp, color, RoundedCornerShape(4.dp))
            )
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        when {
            task.isCompleted -> Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
            task.isRepeating -> Icon(Icons.Filled.Refresh, null, tint = AppColors.Blue, modifier = Modifier.size(16.dp))
        }
    }
}

// ===================== 小纸片 =====================
@Composable
fun InfoChip(bg: Color, content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).background(bg).padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        content = content
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskRow(
    task: Task,
    showList: Boolean,
    showTagChips: Boolean,
    showSubCount: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    selected: Boolean = false
) {
    Store.version.value
    val showDesc = Store.boolSetting("show_desc", true)
    val bg = if (selected) Color(0xFFE3E3E3) else Color.Transparent
    Column(
        Modifier.fillMaxWidth().background(bg)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TaskCheckbox(task, onToggle)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    task.title, fontSize = 16.sp,
                    color = if (task.isCompleted) AppColors.Subtle else Color.Unspecified,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                )
                if (showDesc && task.notes.isNotEmpty()) {
                    Text(task.notes, fontSize = 14.sp, color = AppColors.Subtle)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.padding(start = 40.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (showSubCount) {
                val n = Store.openSubtaskCount(task.id)
                if (n > 0 || Store.subtasksOf(task.id).isNotEmpty()) {
                    InfoChip(Color(0xFFE8E8E8)) {
                        Icon(Icons.Filled.KeyboardArrowUp, null, Modifier.size(14.dp), tint = AppColors.Subtle)
                        Text("$n", fontSize = 12.sp, color = AppColors.Subtle)
                    }
                }
            }
            if (task.locationId.isNotEmpty()) {
                Store.placeById(task.locationId)?.let { p ->
                    InfoChip(Color(0xFFE8E8E8)) {
                        Icon(Icons.Filled.Place, null, Modifier.size(14.dp), tint = AppColors.Subtle)
                        Text(p.name, fontSize = 12.sp, color = AppColors.Subtle)
                    }
                }
            }
            if (showList && task.listId.isNotEmpty()) {
                Store.listById(task.listId)?.let { l ->
                    InfoChip(Color(0xFFE8E8E8)) {
                        Icon(Icons.Filled.List, null, Modifier.size(14.dp), tint = AppColors.Subtle)
                        Text(l.name, fontSize = 12.sp, color = AppColors.Subtle)
                    }
                }
            }
            if (showTagChips) {
                task.tagIds.forEach { tid ->
                    Store.tagById(tid)?.let { tag ->
                        val c = if (tag.color != -1) Color(tag.color) else AppColors.Blue
                        InfoChip(c) {
                            TagIcon(Color.White, Modifier.size(12.dp))
                            Text(tag.name, fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ===================== 空状态 =====================
@Composable
fun EmptyState() {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(130.dp).border(14.dp, Color(0xFFBDBDBD), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(Modifier.padding(bottom = 18.dp).size(52.dp, 28.dp).clip(RoundedCornerShape(50)).background(Color(0xFFBDBDBD)))
        }
        Spacer(Modifier.height(28.dp))
        Text("这里没有任务哦。", fontSize = 18.sp, color = AppColors.Subtle)
    }
}

// ===================== 提醒横幅 =====================
@Composable
fun ReminderBanner(onClose: () -> Unit, onSettings: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(20.dp))
            .background(AppColors.BannerBg).padding(20.dp)
    ) {
        Text("启用提醒", fontSize = 20.sp, color = Color(0xFF333333))
        Spacer(Modifier.height(10.dp))
        Text("提醒在 Android 设置中被禁用", color = Color(0xFF555555))
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onClose) { Text("关闭") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onSettings, shape = RoundedCornerShape(20.dp)) { Text("设置") }
        }
    }
}

// ===================== 主列表屏 =====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    view: ViewRef,
    onSelectView: (ViewRef) -> Unit,
    onOpenTask: (String) -> Unit,
    onNewTask: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenEntitySettings: () -> Unit,
    onAddFilter: () -> Unit,
    onAddTag: () -> Unit,
    onAddPlace: () -> Unit,
    onAddList: () -> Unit,
    onOpenNotifications: () -> Unit
) {
    Store.version.value
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var selection by remember { mutableStateOf(setOf<String>()) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showSelectionMenu by remember { mutableStateOf(false) }
    var showTagPicker by remember { mutableStateOf(false) }
    var showListPicker by remember { mutableStateOf(false) }
    var showPriorityDialog by remember { mutableStateOf(false) }
    var showDateSheet by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val collapsedGroups = remember { mutableStateMapOf<String, Boolean>() }
    val subExpanded = remember { mutableStateMapOf<String, Boolean>() }
    val snackbar = remember { SnackbarHostState() }

    val (title, themeColor) = when (view) {
        is ViewRef.MyTasks -> "我的任务" to (Store.setting("accent", "").toLongOrNull()?.let { Color(it) } ?: AppColors.Blue)
        is ViewRef.BuiltinFilter -> (if (view.kind == "today") "今天" else "最近修改过的") to AppColors.Blue
        is ViewRef.CustomFilter -> {
            val f = Store.filterById(view.id)
            (f?.name ?: "") to (f?.color?.takeIf { it != -1 }?.let { Color(it) } ?: AppColors.Blue)
        }
        is ViewRef.TagView -> (Store.tagById(view.id)?.name ?: "") to AppColors.Blue
        is ViewRef.PlaceView -> (Store.placeById(view.id)?.name ?: "") to AppColors.Blue
        is ViewRef.ListView -> (Store.listById(view.id)?.name ?: "") to AppColors.Blue
    }

    val inSelection = selection.isNotEmpty()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                current = view,
                onSelectView = { onSelectView(it); scope.launch { drawerState.close() } },
                onAddFilter = onAddFilter, onAddTag = onAddTag, onAddPlace = onAddPlace, onAddList = onAddList,
                closeDrawer = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                BottomAppBar(containerColor = AppColors.BottomBar, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Filled.Menu, null, tint = AppColors.Subtle) }
                    IconButton(onClick = { searching = true }) { Icon(Icons.Filled.Search, null, tint = AppColors.Subtle) }
                    IconButton(onClick = { showSortSheet = true }) { SortIcon(AppColors.Subtle) }
                    IconButton(onClick = { }) { MicIcon(AppColors.Subtle) }
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) { Icon(Icons.Filled.MoreVert, null, tint = AppColors.Subtle) }
                        DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                            DropdownMenuItem(text = { Text("清除已完成项") }, onClick = {
                                Store.clearCompleted(view); showMoreMenu = false
                            })
                            DropdownMenuItem(text = { Text("收起子任务") }, onClick = {
                                Store.tasks.forEach { subExpanded[it.id] = false }; showMoreMenu = false
                            })
                            DropdownMenuItem(text = { Text("展开子任务") }, onClick = {
                                Store.tasks.forEach { subExpanded[it.id] = true }; showMoreMenu = false
                            })
                            DropdownMenuItem(text = { Text("分享") }, onClick = {
                                showMoreMenu = false; scope.launch { snackbar.showSnackbar("分享不可用") }
                            })
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    FloatingActionButton(
                        onClick = onNewTask,
                        containerColor = themeColor,
                        shape = RoundedCornerShape(16.dp)
                    ) { Icon(Icons.Filled.Add, null, tint = Color.White) }
                }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background)) {
                // 顶栏
                if (inSelection) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { selection = emptySet() }) { Icon(Icons.Filled.ArrowBack, null) }
                        Text("${selection.size}", fontSize = 20.sp, modifier = Modifier.padding(start = 8.dp))
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { showTagPicker = true }) { TagIcon(AppColors.Subtle) }
                        IconButton(onClick = { showListPicker = true }) { Icon(Icons.Filled.List, null, tint = AppColors.Subtle) }
                        IconButton(onClick = { showDateSheet = true }) { HistoryIcon(AppColors.Subtle) }
                        IconButton(onClick = { showPriorityDialog = true }) { FlagIcon(AppColors.Subtle) }
                        Box {
                            IconButton(onClick = { showSelectionMenu = true }) { Icon(Icons.Filled.MoreVert, null, tint = AppColors.Subtle) }
                            DropdownMenu(expanded = showSelectionMenu, onDismissRequest = { showSelectionMenu = false }) {
                                DropdownMenuItem(text = { Text("全选") }, onClick = {
                                    selection = Store.tasksFor(view).map { it.id }.toSet(); showSelectionMenu = false
                                })
                                DropdownMenuItem(text = { Text("分享") }, onClick = {
                                    showSelectionMenu = false; scope.launch { snackbar.showSnackbar("分享不可用") }
                                })
                                DropdownMenuItem(text = { Text("克隆") }, onClick = {
                                    selection.forEach { id -> Store.tasks.firstOrNull { it.id == id }?.let { Store.cloneTask(it) } }
                                    selection = emptySet(); showSelectionMenu = false
                                })
                                DropdownMenuItem(text = { Text("删除") }, onClick = {
                                    confirmDelete = true; showSelectionMenu = false
                                })
                            }
                        }
                    }
                } else if (searching) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { searching = false; query = "" }) { Icon(Icons.Filled.ArrowBack, null) }
                        TextField(
                            value = query, onValueChange = { query = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Icon(Icons.Filled.Search, null) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, null) }
                        }
                        IconButton(onClick = onOpenSettings) { Icon(Icons.Filled.Settings, null, tint = AppColors.Subtle) }
                    }
                } else {
                    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(title, fontSize = 26.sp, color = themeColor, modifier = Modifier.weight(1f).padding(vertical = 18.dp))
                        Box {
                            var ctxMenu by remember { mutableStateOf(false) }
                            IconButton(onClick = { ctxMenu = true }) { Icon(Icons.Filled.Settings, null, tint = AppColors.Subtle) }
                            DropdownMenu(expanded = ctxMenu, onDismissRequest = { ctxMenu = false }) {
                                DropdownMenuItem(text = { Text("应用设置") }, onClick = { ctxMenu = false; onOpenSettings() })
                                val objLabel = when (view) {
                                    is ViewRef.TagView -> "标签设置"
                                    is ViewRef.ListView -> "清单设置"
                                    is ViewRef.CustomFilter -> "过滤器设置"
                                    is ViewRef.PlaceView -> "地点设置"
                                    else -> ""
                                }
                                if (objLabel.isNotEmpty()) {
                                    DropdownMenuItem(text = { Text(objLabel) }, onClick = { ctxMenu = false; onOpenEntitySettings() })
                                }
                            }
                        }
                    }
                }

                // 提醒横幅
                if (!Store.bannerDismissed.value) {
                    ReminderBanner(
                        onClose = { Store.bannerDismissed.value = true },
                        onSettings = onOpenNotifications
                    )
                }

                // 任务列表
                val all = Store.tasksFor(view)
                val filtered = if (query.isEmpty()) all
                else all.filter { it.title.contains(query, true) || it.notes.contains(query, true) }
                val showCompleted = Store.boolSetting("show_completed", true)
                val showCompletedSubs = Store.boolSetting("show_completed_subs", true)

                if (view is ViewRef.BuiltinFilter && view.kind == "recent") {
                    val flat = filtered.sortedByDescending { it.modifiedAt }
                    if (flat.isEmpty()) EmptyState() else LazyColumn(Modifier.fillMaxSize()) {
                        items(flat.size) { i ->
                            val t = flat[i]
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.weight(1f)) {
                                    TaskRow(t, showList = false, showTagChips = true, showSubCount = t.parentId.isEmpty(),
                                        onToggle = { Store.complete(t, !t.isCompleted) },
                                        onClick = { if (inSelection) selection = toggleSel(t.id, selection) else onOpenTask(t.id) },
                                        onLongClick = { selection = selection + t.id },
                                        selected = selection.contains(t.id))
                                }
                                Text(
                                    Dates.relativeLabel(t), fontSize = 14.sp, color = AppColors.Subtle,
                                    modifier = Modifier.padding(end = 16.dp)
                                )
                            }
                        }
                    }
                } else {
                    val (open, done) = filtered.partition { !it.isCompleted }
                    val groups = open.groupBy { Dates.groupName(it) }.toSortedMap(compareBy { name ->
                        open.firstOrNull { Dates.groupName(it) == name }?.let { Dates.groupOrder(it) } ?: Long.MAX_VALUE
                    })
                    val doneTasks = if (showCompleted) done else emptyList()
                    if (open.isEmpty() && doneTasks.isEmpty()) {
                        EmptyState()
                    } else LazyColumn(Modifier.fillMaxSize()) {
                        groups.forEach { (gname, gtasks) ->
                            item(key = "g_$gname") {
                                GroupHeader(gname, collapsedGroups[gname] == true) {
                                    collapsedGroups[gname] = !(collapsedGroups[gname] == true)
                                }
                            }
                            if (collapsedGroups[gname] != true) {
                                val sorted = sortTasks(gtasks)
                                items(sorted.size) { i ->
                                    val t = sorted[i]
                                    TaskWithSubs(
                                        t, view, inSelection, selection, subExpanded,
                                        showCompletedSubs,
                                        onToggle = { Store.complete(t, !t.isCompleted) },
                                        onOpenTask = onOpenTask,
                                        onSelect = { selection = it }
                                    )
                                }
                            }
                        }
                        if (doneTasks.isNotEmpty()) {
                            item(key = "g_done") {
                                GroupHeader("已完成", collapsedGroups["已完成"] == true) {
                                    collapsedGroups["已完成"] = !(collapsedGroups["已完成"] == true)
                                }
                            }
                            if (collapsedGroups["已完成"] != true) {
                                val sortedDone = doneTasks.sortedByDescending { it.completedAt }
                                items(sortedDone.size) { i ->
                                    val t = sortedDone[i]
                                    TaskWithSubs(
                                        t, view, inSelection, selection, subExpanded,
                                        showCompletedSubs,
                                        onToggle = { Store.complete(t, false) },
                                        onOpenTask = onOpenTask,
                                        onSelect = { selection = it }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSortSheet) SortSheet { showSortSheet = false }
    if (showTagPicker) TagSelectDialog(onDismiss = { showTagPicker = false }, onPick = { tagId ->
        selection.forEach { id -> Store.tasks.firstOrNull { it.id == id }?.let { if (!it.tagIds.contains(tagId)) it.tagIds.add(tagId); Store.touch(it) } }
        selection = emptySet(); showTagPicker = false
    })
    if (showListPicker) ListPickerDialog(onDismiss = { showListPicker = false }, onPick = { listId ->
        selection.forEach { id -> Store.tasks.firstOrNull { it.id == id }?.let { it.listId = listId; Store.touch(it) } }
        selection = emptySet(); showListPicker = false
    })
    if (showPriorityDialog) AlertDialog(
        onDismissRequest = { showPriorityDialog = false }, confirmButton = {}, title = { Text("优先级…") },
        text = {
            Column {
                listOf(Priority.HIGH to "!!!", Priority.MEDIUM to "!!", Priority.LOW to "!", Priority.NONE to "o").forEach { (p, label) ->
                    Text(label, fontSize = 17.sp, modifier = Modifier.fillMaxWidth().clickable {
                        selection.forEach { id -> Store.tasks.firstOrNull { it.id == id }?.let { it.priority = p; Store.touch(it) } }
                        selection = emptySet(); showPriorityDialog = false
                    }.padding(vertical = 14.dp))
                }
            }
        }
    )
    if (showDateSheet) DateTimeSheet("", "", onDismiss = { showDateSheet = false }, onConfirm = { d, tm ->
        selection.forEach { id -> Store.tasks.firstOrNull { it.id == id }?.let { it.dueDate = d; it.dueTime = tm; Store.touch(it) } }
        selection = emptySet(); showDateSheet = false
    })
    if (confirmDelete) ConfirmDialog("确认删除？", { confirmDelete = false }, {
        selection.forEach { id -> Store.tasks.firstOrNull { it.id == id }?.let { Store.delete(it) } }
        selection = emptySet(); confirmDelete = false
    })
}

private fun toggleSel(id: String, sel: Set<String>): Set<String> = if (sel.contains(id)) sel - id else sel + id

@Composable
private fun GroupHeader(name: String, collapsed: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, color = AppColors.Subtle, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        Icon(
            if (collapsed) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
            null, tint = AppColors.Subtle
        )
    }
}

private fun sortTasks(list: List<Task>): List<Task> {
    val field = Store.setting("sort_field", "due")
    val dir = Store.setting("sort_dir", "asc")
    val comparator: Comparator<Task> = when (field) {
        "priority" -> compareByDescending { it.priority.level }
        "title" -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
        "modified" -> compareByDescending { it.modifiedAt }
        "created" -> compareByDescending { it.createdAt }
        "start" -> compareBy { it.startDate.ifEmpty { "9999" } }
        "smart" -> compareBy<Task> { it.dueDate.ifEmpty { "9999" } }.thenByDescending { it.priority.level }
        else -> compareBy<Task> { it.dueDate.ifEmpty { "9999" } }.thenBy { it.dueTime }
    }
    val sorted = list.sortedWith(comparator)
    return if (dir == "desc") sorted.reversed() else sorted
}

@Composable
private fun TaskWithSubs(
    t: Task, view: ViewRef, inSelection: Boolean, selection: Set<String>,
    subExpanded: MutableMap<String, Boolean>, showCompletedSubs: Boolean,
    onToggle: () -> Unit, onOpenTask: (String) -> Unit, onSelect: (Set<String>) -> Unit
) {
    val showList = view !is ViewRef.ListView
    val showTags = view !is ViewRef.TagView
    val expanded = subExpanded[t.id] ?: true
    Column {
        TaskRow(
            t, showList = showList, showTagChips = showTags, showSubCount = true,
            onToggle = onToggle,
            onClick = { if (inSelection) onSelect(toggleSel(t.id, selection)) else onOpenTask(t.id) },
            onLongClick = { onSelect(selection + t.id) },
            selected = selection.contains(t.id)
        )
        val subs = Store.subtasksOf(t.id).filter { showCompletedSubs || !it.isCompleted }
        if (expanded) {
            subs.forEach { st ->
                Row(Modifier.padding(start = 32.dp)) {
                    TaskRow(
                        st, showList = false, showTagChips = false, showSubCount = false,
                        onToggle = { Store.complete(st, !st.isCompleted) },
                        onClick = { if (inSelection) onSelect(toggleSel(st.id, selection)) else onOpenTask(st.id) },
                        onLongClick = { onSelect(selection + st.id) },
                        selected = selection.contains(st.id)
                    )
                }
            }
        }
    }
}

// ===================== 排序面板 =====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortSheet(onDismiss: () -> Unit) {
    var fieldMenu by remember { mutableStateOf(false) }
    val fieldLabel = mapOf(
        "due" to "按截止日期", "start" to "按开始日期", "priority" to "按优先级",
        "title" to "按标题", "modified" to "按最后修改", "smart" to "智能排序", "created" to "创建时间"
    )
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth().clickable { fieldMenu = true }.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                HistoryIcon(AppColors.Subtle)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("分组", fontSize = 16.sp); Text("按截止日期", color = AppColors.Subtle, fontSize = 13.sp)
                }
            }
            Row(Modifier.fillMaxWidth().clickable { fieldMenu = true }.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                SortIcon(AppColors.Subtle)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("排序", fontSize = 16.sp)
                    Text(fieldLabel[Store.setting("sort_field", "due")] ?: "按截止日期", color = AppColors.Subtle, fontSize = 13.sp)
                }
                TextButton(onClick = {
                    Store.setSetting("sort_dir", if (Store.setting("sort_dir", "asc") == "asc") "desc" else "asc")
                }) {
                    Text(if (Store.setting("sort_dir", "asc") == "asc") "↑ 升序" else "↓ 降序")
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                SubtaskIcon(AppColors.Subtle)
                Spacer(Modifier.width(16.dp))
                Column { Text("子任务", fontSize = 16.sp); Text("我的顺序", color = AppColors.Subtle, fontSize = 13.sp) }
            }
            HorizontalDivider()
            SettingSwitch("显示未开始项目", Store.boolSetting("show_unstarted", true)) { Store.setBool("show_unstarted", it) }
            SettingSwitch("显示已完成任务", Store.boolSetting("show_completed", true)) { Store.setBool("show_completed", it) }
            SettingSwitch("显示已完成的子任务", Store.boolSetting("show_completed_subs", true)) { Store.setBool("show_completed_subs", it) }
            SettingSwitch("将已完成任务移至底部", Store.boolSetting("completed_bottom", true)) { Store.setBool("completed_bottom", it) }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                SortIcon(AppColors.Subtle)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("已完成", fontSize = 16.sp); Text("按完成时间", color = AppColors.Subtle, fontSize = 13.sp)
                }
                Text("↓ 降序", color = AppColors.Subtle)
            }
            Spacer(Modifier.height(16.dp))
        }
        if (fieldMenu) {
            AlertDialog(onDismissRequest = { fieldMenu = false }, confirmButton = {}, title = null, text = {
                Column {
                    listOf("due", "start", "priority", "title", "modified", "smart", "created").forEach { f ->
                        Text(
                            fieldLabel[f]!!, fontSize = 17.sp,
                            color = if (Store.setting("sort_field", "due") == f) MaterialTheme.colorScheme.primary else Color.Unspecified,
                            modifier = Modifier.fillMaxWidth().clickable { Store.setSetting("sort_field", f); fieldMenu = false }.padding(vertical = 12.dp)
                        )
                    }
                    Text("我的顺序", fontSize = 17.sp, color = AppColors.Subtle, modifier = Modifier.padding(vertical = 12.dp))
                    Text("不适用于标签、过滤器或地点", fontSize = 13.sp, color = AppColors.Red)
                }
            })
        }
    }
}

@Composable
fun SettingSwitch(label: String, checked: Boolean, subtitle: String = "", onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 16.sp)
            if (subtitle.isNotEmpty()) Text(subtitle, fontSize = 13.sp, color = AppColors.Subtle)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

// ===================== 标签选择对话框 =====================
@Composable
fun TagSelectDialog(onDismiss: () -> Unit, onPick: (String) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, confirmButton = {}, title = { Text("输入标签名称") }, text = {
        Column {
            Store.tags.forEach { tag ->
                Row(
                    Modifier.fillMaxWidth().clickable { onPick(tag.id) }.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TagIcon(if (tag.color != -1) Color(tag.color) else AppColors.Blue)
                    Spacer(Modifier.width(12.dp))
                    Text(tag.name, fontSize = 16.sp)
                }
            }
        }
    })
}
