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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun MainScreen() {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var nav by remember { mutableStateOf<Nav>(Nav.MyTasks) }
    var editing by remember { mutableStateOf<Task?>(null) }
    var creating by remember { mutableStateOf(false) }
    var showSort by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showSettingsMenu by remember { mutableStateOf(false) }
    var showReminder by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf<String?>(null) }

    if (creating || editing != null) {
        val target = editing
        EditScreen(
            initial = target ?: Repo.newTask(defaultListFor(nav)),
            isNew = target == null,
            onDone = { editing = null; creating = false }
        )
        return
    }
    if (showSettings) {
        SettingsScreen(onBack = { showSettings = false })
        return
    }
    if (searchQuery != null) {
        SearchScreen(
            query = searchQuery!!,
            onQuery = { searchQuery = it },
            onBack = { searchQuery = null },
            onOpen = { editing = it }
        )
        return
    }

    if (drawerState.isOpen) {
        androidx.activity.compose.BackHandler { scope.launch { drawerState.close() } }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                nav = nav,
                onSelect = {
                    nav = it
                    scope.launch { drawerState.close() }
                },
                onCollapseToggle = { }
            )
        }
    ) {
        Box(Modifier.fillMaxSize().background(AppColors.Background)) {
            Column(Modifier.fillMaxSize()) {
                TopBar(
                    title = nav.title,
                    onGear = { showSettingsMenu = true }
                )
                if (showReminder) {
                    ReminderBanner(
                        onClose = { showReminder = false },
                        onSettings = { showReminder = false }
                    )
                } else {
                    Spacer(Modifier.height(6.dp))
                }
                TaskListBody(
                    nav = nav,
                    modifier = Modifier.weight(1f),
                    onOpen = { editing = it },
                    onTagClick = { nav = Nav.Tag(it) },
                    onListClick = { nav = Nav.List(it) }
                )
                BottomBar(
                    showSort = nav is Nav.MyTasks || nav is Nav.List || nav is Nav.Tag,
                    onMenu = { scope.launch { drawerState.open() } },
                    onSearch = { searchQuery = "" },
                    onSort = { showSort = true },
                    onOverflow = { showSort = true },
                    onAdd = { creating = true }
                )
            }
            if (showSettingsMenu) {
                SettingsMenu(
                    listSettings = nav is Nav.List,
                    tagSettings = nav is Nav.Tag,
                    onDismiss = { showSettingsMenu = false },
                    onAppSettings = { showSettingsMenu = false; showSettings = true },
                    onListSettings = { showSettingsMenu = false }
                )
            }
        }
    }

    if (showSort) {
        SortSheet(onDismiss = { showSort = false })
    }
}

private fun defaultListFor(nav: Nav): String? = when (nav) {
    is Nav.List -> nav.id
    else -> Repo.lists.firstOrNull()?.id
}

@Composable
private fun TopBar(title: String, onGear: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 36.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            fontSize = 24.sp,
            color = AppColors.Title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(percent = 50))
                .clickable { onGear() },
            contentAlignment = Alignment.Center
        ) { CoreIcon(IconSettings, AppColors.OnSurface, 22.dp) }
    }
}

@Composable
private fun TaskListBody(
    nav: Nav,
    modifier: Modifier,
    onOpen: (Task) -> Unit,
    onTagClick: (String) -> Unit,
    onListClick: (String) -> Unit
) {
    val tasks = Repo.visibleTasks(nav).toMutableList()
    if (Repo.sortMode == SortMode.TITLE) tasks.sortBy { it.title }
    val open = tasks.filter { !it.isCompleted }
    val done = tasks.filter { it.isCompleted }

    if (tasks.isEmpty()) {
        Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(180.dp))
                GlyphIcon(Glyph.Inbox, AppColors.Outline, size = 96.dp, stroke = 4.dp)
                Spacer(Modifier.height(18.dp))
                Text("这里没有任务哦。", fontSize = 15.sp, color = AppColors.SecondaryText)
            }
        }
        return
    }

    LazyColumn(modifier.fillMaxWidth()) {
        if (Repo.grouping == Grouping.DUE) {
            val order = listOf("已过期", "今天", "明天", "本周", "下周", "以后", "无截止日期")
            order.forEach { bucket ->
                val group = open.filter { Repo.bucket(it) == bucket }
                if (group.isNotEmpty()) {
                    item(key = "h-$bucket") { GroupHeader(bucket, group.size) }
                    group.forEach { t ->
                        item(key = t.id) {
                            TaskRowView(t, onOpen, onTagClick, onListClick)
                        }
                    }
                }
            }
        } else {
            open.forEach { t ->
                item(key = t.id) { TaskRowView(t, onOpen, onTagClick, onListClick) }
            }
        }
        if (done.isNotEmpty() && Repo.showCompleted) {
            item(key = "h-done") { GroupHeader("已完成", done.size) }
            done.forEach { t ->
                item(key = t.id) { TaskRowView(t, onOpen, onTagClick, onListClick) }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun GroupHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 14.sp, color = AppColors.SecondaryText, modifier = Modifier.weight(1f))
        Text("$count", fontSize = 14.sp, color = AppColors.SecondaryText)
    }
}

@Composable
private fun TaskRowView(
    task: Task,
    onOpen: (Task) -> Unit,
    onTagClick: (String) -> Unit,
    onListClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(task) }
            .padding(start = 20.dp, end = 20.dp, top = 9.dp, bottom = 9.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            PriorityCheckbox(task.priority, task.isCompleted, onToggle = {
                Repo.toggleComplete(task, !task.isCompleted)
            })
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    task.title.ifBlank { "任务名称" },
                    fontSize = 16.sp,
                    color = if (task.isCompleted) AppColors.SecondaryText else AppColors.OnSurface,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                )
                if (task.notes.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        task.notes,
                        fontSize = 14.sp,
                        color = AppColors.SecondaryText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                    )
                }
                ChipsRow(task, onTagClick, onListClick)
            }
        }
        task.subtasks.forEach { sub ->
            if (sub.isCompleted && !Repo.showCompletedSubtasks) return@forEach
            Row(
                modifier = Modifier.padding(start = 33.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PriorityCheckbox(sub.priority, sub.isCompleted, size = 18.dp, onToggle = {
                    Repo.toggleComplete(sub, !sub.isCompleted)
                })
                Spacer(Modifier.width(12.dp))
                Text(
                    sub.title,
                    fontSize = 15.sp,
                    color = if (sub.isCompleted) AppColors.SecondaryText else AppColors.OnSurface,
                    textDecoration = if (sub.isCompleted) TextDecoration.LineThrough else null
                )
            }
        }
    }
}

@Composable
private fun ChipsRow(task: Task, onTagClick: (String) -> Unit, onListClick: (String) -> Unit) {
    val subtaskCount = task.subtasks.size
    val hasChips = subtaskCount > 0 || task.listId != null || task.tags.isNotEmpty() ||
        (task.due != null && Repo.grouping != Grouping.DUE)
    if (!hasChips) return
    Row(
        modifier = Modifier.padding(top = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (subtaskCount > 0) {
            Chip("$subtaskCount", glyph = Glyph.Checklist)
        }
        if (Repo.grouping != Grouping.DUE && task.due != null) {
            Chip(DateFmt.chip(task.due), glyph = Glyph.Calendar,
                foreground = if (Repo.isOverdue(task)) AppColors.HighPriority else AppColors.OnSurface)
        }
        task.listId?.let { id ->
            val l = Repo.list(id)
            Chip(
                Repo.listName(id),
                glyph = Glyph.List,
                background = (l?.color?.let { Color(it) } ?: AppColors.Chip).copy(alpha = 0.22f)
            ) { onListClick(id) }
        }
        task.tags.forEach { tag ->
            Chip(tag, glyph = Glyph.Tag) { onTagClick(tag) }
        }
    }
}

@Composable
private fun BottomBar(
    showSort: Boolean,
    onMenu: () -> Unit,
    onSearch: () -> Unit,
    onSort: () -> Unit,
    onOverflow: () -> Unit,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.SurfaceTint)
            .navigationBarsPadding()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BarIcon(IconMenu, onMenu)
        BarIcon(IconSearch, onSearch)
        if (showSort) BarGlyph(Glyph.Sort, onSort)
        BarGlyph(Glyph.Mic, {})
        BarIcon(IconMore, onOverflow)
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(AppColors.Fab)
                .clickable { onAdd() },
            contentAlignment = Alignment.Center
        ) { CoreIcon(IconAdd, Color.White, 26.dp) }
        Spacer(Modifier.width(8.dp))
    }
}

@Composable
private fun BarIcon(image: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(percent = 50))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { CoreIcon(image, AppColors.OnSurface, 22.dp) }
}

@Composable
private fun BarGlyph(glyph: Glyph, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(percent = 50))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { GlyphIcon(glyph, AppColors.OnSurface, size = 22.dp) }
}

@Composable
private fun SettingsMenu(
    listSettings: Boolean,
    tagSettings: Boolean,
    onDismiss: () -> Unit,
    onAppSettings: () -> Unit,
    onListSettings: () -> Unit
) {
    Box(Modifier.fillMaxSize().clickable { onDismiss() }) {
        Column(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 60.dp, end = 8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White)
                .padding(vertical = 8.dp)
        ) {
            MenuRow("应用设置", onAppSettings)
            if (listSettings) MenuRow("清单设置", onListSettings)
            if (tagSettings) MenuRow("标签设置", onListSettings)
        }
    }
}

@Composable
private fun MenuRow(text: String, onClick: () -> Unit) {
    Text(
        text,
        fontSize = 15.sp,
        color = AppColors.OnSurface,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp)
    )
}
