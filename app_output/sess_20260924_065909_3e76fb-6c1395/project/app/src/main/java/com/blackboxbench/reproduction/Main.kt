package com.blackboxbench.reproduction

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ------------------------------------------------------------------ filtering

fun tasksForView(viewKey: String): List<Task> {
    val now = System.currentTimeMillis()
    val base = when {
        viewKey == "my" -> Store.tasks.toList()
        viewKey == "today" -> Store.tasks.filter { it.dueAt != null && isSameDay(it.dueAt!!, now) }
        viewKey == "recent" -> Store.tasks.toList()
        viewKey.startsWith("list:") -> Store.tasks.filter { it.listId == viewKey.removePrefix("list:") }
        viewKey.startsWith("tag:") -> Store.tasks.filter { it.tagIds.contains(viewKey.removePrefix("tag:")) }
        viewKey.startsWith("filter:") -> {
            val f = Store.filterById(viewKey.removePrefix("filter:"))
            if (f == null) Store.tasks.toList() else Store.tasks.filter { t -> f.conditions.all { c -> conditionMatches(c, t) } }
        }
        else -> Store.tasks.toList()
    }
    var list = base.filter { t ->
        (Store.settings.showCompleted || !t.completed) &&
            (Store.settings.showUnstarted || t.startAt == null || t.startAt!! <= now)
    }
    if (viewKey == "recent") {
        list = list.sortedByDescending { it.modifiedAt }
    } else {
        list = when (Store.settings.sortBy) {
            "按优先级" -> list.sortedWith(compareByDescending<Task> { it.priority }.thenBy { it.dueAt ?: Long.MAX_VALUE })
            "按标题" -> list.sortedBy { it.title }
            "按最后修改" -> list.sortedByDescending { it.modifiedAt }
            "创建时间" -> list.sortedBy { it.createdAt }
            "清单" -> list.sortedBy { Store.listName(it.listId) }
            else -> list.sortedWith(compareBy<Task> { it.dueAt ?: Long.MAX_VALUE }.thenByDescending { it.priority })
        }
    }
    if (Store.settings.moveCompletedToBottom) {
        list = list.sortedBy { it.completed }
    }
    return list
}

fun conditionMatches(c: FilterCondition, t: Task): Boolean {
    val now = System.currentTimeMillis()
    return when (c.type) {
        "all" -> true
        "priority" -> when (c.value) {
            "high" -> t.priority == PRIO_HIGH
            "med" -> t.priority == PRIO_MED
            "low" -> t.priority == PRIO_LOW
            else -> t.priority == PRIO_NONE
        }
        "list" -> t.listId == c.value
        "tag" -> t.tagIds.contains(c.value)
        "title" -> t.title.contains(c.value, ignoreCase = true)
        "completed" -> t.completed
        "notStarted" -> (t.startAt ?: 0L) > now
        "hasSubtasks" -> t.subtasks.isNotEmpty()
        "isSubtask" -> false
        "hasReminder" -> t.reminder
        "repeating" -> t.isRepeating
        "due" -> t.dueAt != null
        "start" -> t.startAt != null
        "noTag" -> t.tagIds.isEmpty()
        "overdue" -> t.dueAt != null && t.dueAt!! < now
        "today" -> t.dueAt != null && isSameDay(t.dueAt!!, now)
        "tomorrow" -> t.dueAt != null && dayDiff(now, t.dueAt!!) == 1
        "future" -> t.dueAt != null && t.dueAt!! > now
        "noDue" -> t.dueAt == null
        else -> true
    }
}

fun conditionLabel(c: FilterCondition): String = when (c.type) {
    "all" -> "我的任务"
    "priority" -> when (c.value) {
        "high" -> "最低的优先级!!!"
        "med" -> "最低的优先级!!"
        "low" -> "最低的优先级!"
        else -> "最低的优先级o"
    }
    "list" -> "在清单「${Store.listName(c.value)}」中"
    "tag" -> "标签「${Store.tagById(c.value)?.name ?: ""}」"
    "title" -> "标题含「${c.value}」"
    "completed" -> "已完成"
    "notStarted" -> "尚未开始"
    "hasSubtasks" -> "有子任务"
    "isSubtask" -> "是子任务"
    "hasReminder" -> "有提醒"
    "repeating" -> "重复"
    "due" -> "有截止日期"
    "start" -> "有开始日期"
    "noTag" -> "无标签"
    "overdue" -> "已过期"
    "today" -> "今天"
    "tomorrow" -> "明天"
    "future" -> "今天以后"
    "noDue" -> "无截止日期"
    else -> c.type
}

fun groupOf(t: Task, groupBy: String): String? {
    val now = System.currentTimeMillis()
    return when (groupBy) {
        "按优先级" -> priorityName(t.priority)
        "按截止日期" -> when {
            t.dueAt == null -> "无截止日期"
            t.dueAt!! < startOfDay(now) -> "已过期"
            dayDiff(now, t.dueAt!!) == 0 -> "今天"
            dayDiff(now, t.dueAt!!) == 1 -> "明天"
            else -> "以后"
        }
        "按开始日期" -> when {
            t.startAt == null -> "无开始日期"
            isSameDay(t.startAt!!, now) -> "今天"
            dayDiff(now, t.startAt!!) == 1 -> "明天"
            else -> "以后"
        }
        "按最后修改", "创建时间" -> {
            val ts = if (groupBy == "创建时间") t.createdAt else t.modifiedAt
            when {
                isSameDay(ts, now) -> "今天"
                dayDiff(now, ts) == -1 -> "昨天"
                else -> "更早"
            }
        }
        "清单" -> Store.listName(t.listId)
        else -> null
    }
}

fun groupOrder(name: String): Int = when (name) {
    "已过期" -> 0
    "今天" -> 1
    "明天" -> 2
    "高优先级" -> 3
    "中优先级" -> 4
    "低优先级" -> 5
    "无优先级" -> 6
    "以后" -> 7
    "昨天" -> 8
    "更早" -> 9
    else -> 10
}

// ------------------------------------------------------------------ main screen

@Composable
fun MainScreen(p: Palette, router: Router, viewKey: String, onViewChange: (String) -> Unit) {
    Store.revision
    var drawerOpen by remember { mutableStateOf(false) }
    var sortOpen by remember { mutableStateOf(false) }
    var overflowOpen by remember { mutableStateOf(false) }
    var cloneConfirm by remember { mutableStateOf(false) }
    var deleteConfirm by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    val selected = remember { androidx.compose.runtime.mutableStateListOf<String>() }
    var snack by remember { mutableStateOf<Pair<String, (() -> Unit)?>?>(null) }
    var bannerStep by remember { mutableStateOf(if (Store.justSeeded) 1 else 0) }
    val settings = Store.settings

    LaunchedEffect(snack) {
        if (snack != null) {
            kotlinx.coroutines.delay(3500)
            snack = null
        }
    }

    val all = tasksForView(viewKey)
    val grouped: List<Pair<String?, List<Task>>> = if (settings.groupBy == "无") {
        listOf(null to all)
    } else {
        all.groupBy { groupOf(it, settings.groupBy) ?: "" }
            .toList()
            .sortedBy { groupOrder(it.first) }
    }

    BackHandler(enabled = true) {
        when {
            selectionMode -> { selectionMode = false; selected.clear() }
            drawerOpen -> drawerOpen = false
            sortOpen -> sortOpen = false
            router.canPop -> router.pop()
        }
    }

    Box(Modifier.fillMaxSize().background(p.background)) {
        Column(Modifier.fillMaxSize()) {
            if (selectionMode) {
                SelectionBar(p, selected.size, onBack = { selectionMode = false; selected.clear() }, onMore = { overflowOpen = true })
            } else {
                TopBar(p, viewTitle(viewKey), titleColor = viewColor(viewKey), trailing = {
                    Box(
                        modifier = Modifier.size(44.dp).clickable { router.push(Nav.Settings("root")) },
                        contentAlignment = Alignment.Center
                    ) { SettingsGlyph(p.textPrimary) }
                })
            }
            if (bannerStep == 1) {
                BannerCard(
                    p,
                    title = "启用提醒",
                    body = "提醒在 Android 设置中被禁用",
                    onClose = { bannerStep = 2 }
                )
            } else if (bannerStep == 2) {
                BannerCard(
                    p,
                    title = "在正确的时间收到通知",
                    body = "允许 Tasks 在后台运行以按时提醒",
                    onClose = { bannerStep = 0 }
                )
            }
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                grouped.forEach { (header, list) ->
                    if (header != null) {
                        item(key = "h_" + header) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(header, color = p.textSecondary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                ChevronUp(color = p.textSecondary, size = 16.dp)
                            }
                        }
                    }
                    if (list.isEmpty() && header == null) {
                        item(key = "empty") { EmptyState(p) }
                    }
                    items(list, key = { it.id }) { task ->
                        TaskRow(
                            task = task, p = p, viewKey = viewKey,
                            selected = selected.contains(task.id),
                            selectionMode = selectionMode,
                            onOpen = { if (selectionMode) toggleSel(selected, task.id) else router.push(Nav.Edit(task.id, null)) },
                            onToggle = {
                                if (task.completed) {
                                    Store.uncompleteTask(task)
                                } else {
                                    Store.completeTask(task)
                                    snack = "任务已完成" to {
                                        Store.uncompleteTask(task)
                                        snack = null
                                    }
                                }
                            },
                            onLongPress = {
                                if (!selectionMode) { selectionMode = true; selected.clear(); selected.add(task.id) }
                            },
                            onToggleSubtask = { s ->
                                if (s.done) Store.uncompleteSubtask(task, s) else {
                                    Store.completeSubtask(task, s)
                                    snack = "任务已完成" to {
                                        Store.uncompleteSubtask(task, s)
                                        snack = null
                                    }
                                }
                            },
                            onSelect = { toggleSel(selected, task.id) }
                        )
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
            BottomBar(
                p = p,
                fabColor = if (isListView(viewKey)) viewColor(viewKey) else Color(0xFF2196F3),
                onMenu = { drawerOpen = true },
                onSearch = { router.push(Nav.Search(viewKey)) },
                onSort = { sortOpen = true },
                onMic = { },
                onMore = { overflowOpen = true },
                onFab = { router.push(Nav.Edit(null, if (isListView(viewKey)) viewKey.removePrefix("list:") else null)) }
            )
        }

        if (drawerOpen) {
            DrawerOverlay(p, viewKey, onDismiss = { drawerOpen = false }, onPick = { key ->
                drawerOpen = false
                onViewChange(key)
            }, onSearch = { drawerOpen = false; router.push(Nav.Search(viewKey)) },
                onNewFilter = { drawerOpen = false; router.push(Nav.FilterPresets) },
                onNewTag = { drawerOpen = false; router.push(Nav.TagEdit(null)) },
                onNewList = { drawerOpen = false; router.push(Nav.ListEdit(null)) })
        }
        if (sortOpen) {
            SortSheet(p, onDismiss = { sortOpen = false })
        }
        if (overflowOpen) {
            if (selectionMode) {
                ListMenuDialog(
                    p,
                    onDismiss = { overflowOpen = false },
                    items = listOf("全选", "分享", "克隆", "删除"),
                    onPick = { label ->
                        overflowOpen = false
                        when (label) {
                            "全选" -> { selected.clear(); selected.addAll(all.map { it.id }) }
                            "分享" -> snack = "没有可分享的内容" to null
                            "克隆" -> cloneConfirm = true
                            "删除" -> deleteConfirm = true
                        }
                    }
                )
            } else {
                ListMenuDialog(
                    p,
                    onDismiss = { overflowOpen = false },
                    items = listOf("清除已完成项", "收起子任务", "展开子任务", "分享"),
                    onPick = { label ->
                        overflowOpen = false
                        when (label) {
                            "清除已完成项" -> {
                                val ids = Store.tasks.filter { it.completed }.map { it.id }
                                if (ids.isNotEmpty()) Store.removeTasks(ids)
                            }
                            "分享" -> snack = "没有可分享的内容" to null
                        }
                    }
                )
            }
        }
        if (cloneConfirm) {
            ConfirmDialog(
                p, "复制已选的任务?",
                onConfirm = {
                    cloneConfirm = false
                    selected.toList().forEach { id -> Store.taskById(id)?.let { Store.duplicate(it) } }
                    snack = "已复制 ${selected.size} 个任务" to null
                    selectionMode = false
                    selected.clear()
                },
                onDismiss = { cloneConfirm = false }
            )
        }
        if (deleteConfirm) {
            ConfirmDialog(
                p, "删除已选的任务?",
                onConfirm = {
                    deleteConfirm = false
                    Store.removeTasks(selected.toList())
                    selectionMode = false
                    selected.clear()
                },
                onDismiss = { deleteConfirm = false }
            )
        }
        if (!sortOpen && !drawerOpen && !overflowOpen) {
            snack?.let { (text, action) ->
                Snack(p, text, action)
            }
        }
    }
}

fun toggleSel(selected: MutableList<String>, id: String) {
    if (selected.contains(id)) selected.remove(id) else selected.add(id)
}

@Composable
fun SettingsGlyph(color: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(24.dp)) {
        val w = size.width; val h = size.height
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.10f)
        drawCircle(color, w * 0.20f, androidx.compose.ui.geometry.Offset(w / 2, h / 2), style = stroke)
        drawCircle(color, w * 0.40f, androidx.compose.ui.geometry.Offset(w / 2, h / 2), style = stroke)
        for (i in 0 until 8) {
            val a = Math.toRadians((i * 45).toDouble())
            val cx = w / 2 + (w * 0.44f) * Math.cos(a).toFloat()
            val cy = h / 2 + (h * 0.44f) * Math.sin(a).toFloat()
            drawCircle(color, w * 0.07f, androidx.compose.ui.geometry.Offset(cx, cy))
        }
    }
}

@Composable
fun EmptyState(p: Palette) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 150.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(120.dp).clip(RoundedCornerShape(10.dp))
                .background(if (p.dark) Color(0xFF2A2B31) else Color(0xFFBDBDBD)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.size(96.dp).clip(RoundedCornerShape(6.dp)).background(p.background)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text("这里没有任务哦。", color = p.textSecondary, fontSize = 18.sp)
    }
}

@Composable
fun BannerCard(p: Palette, title: String, body: String, onClose: () -> Unit) {
    Surface(
        color = p.surface, shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = Color(0xFFC0392B), fontSize = 16.sp)
                Text(body, color = Color(0xFFC0392B), fontSize = 13.sp)
            }
            Text("关闭", color = p.primary, fontSize = 15.sp, modifier = Modifier.clickable { onClose() })
        }
    }
}

@Composable
fun Snack(p: Palette, text: String, action: (() -> Unit)?) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Surface(
            color = Color(0xFF303030), shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 90.dp)
        ) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
                if (action != null) {
                    Text(
                        "撤消", color = Color(0xFF80B4F0), fontSize = 15.sp,
                        modifier = Modifier.clickable { action() }.padding(start = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SelectionBar(p: Palette, count: Int, onBack: () -> Unit, onMore: () -> Unit) {
    Surface(color = p.surface, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(44.dp).clickable { onBack() }, contentAlignment = Alignment.Center) {
                androidx.compose.foundation.Canvas(Modifier.size(22.dp)) {
                    val w = size.width; val h = size.height
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(w * 0.70f, h * 0.12f); lineTo(w * 0.22f, h * 0.5f); lineTo(w * 0.70f, h * 0.88f)
                    }
                    drawPath(path, p.textPrimary, style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.12f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                }
            }
            Text("$count", color = p.textPrimary, fontSize = 20.sp, modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { TagIcon(color = p.textPrimary, size = 22.dp) }
                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { ListGlyphSmall(p.textPrimary) }
                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { ClockIcon(color = p.textPrimary, size = 22.dp) }
                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { FlagIcon(color = p.textPrimary, size = 22.dp) }
                Box(Modifier.size(44.dp).clickable { onMore() }, contentAlignment = Alignment.Center) { DotsGlyph(p.textPrimary, 22.dp) }
            }
        }
    }
}

@Composable
fun BottomBar(
    p: Palette,
    fabColor: Color,
    onMenu: () -> Unit,
    onSearch: () -> Unit,
    onSort: () -> Unit,
    onMic: () -> Unit,
    onMore: () -> Unit,
    onFab: () -> Unit
) {
    Surface(color = p.barBg, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(70.dp).padding(start = 10.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(44.dp).clickable { onMenu() }, contentAlignment = Alignment.Center) { MenuGlyph(p.textPrimary) }
            Box(Modifier.size(44.dp).clickable { onSearch() }, contentAlignment = Alignment.Center) { SearchGlyph(p.textPrimary, 23.dp) }
            Box(Modifier.size(44.dp).clickable { onSort() }, contentAlignment = Alignment.Center) { SortIcon(p.textPrimary, 23.dp) }
            Box(Modifier.size(44.dp).clickable { onMic() }, contentAlignment = Alignment.Center) { MicIcon(p.textPrimary, 23.dp) }
            Box(Modifier.size(44.dp).clickable { onMore() }, contentAlignment = Alignment.Center) { DotsGlyph(p.textPrimary, 23.dp) }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape).background(fabColor).clickable { onFab() },
                contentAlignment = Alignment.Center
            ) { PlusGlyph(Color.White) }
        }
    }
}

@Composable
fun MenuGlyph(color: Color) {
    androidx.compose.foundation.Canvas(Modifier.size(24.dp)) {
        val w = size.width; val h = size.height
        val cap = androidx.compose.ui.graphics.StrokeCap.Round
        drawLine(color, androidx.compose.ui.geometry.Offset(w * 0.12f, h * 0.24f), androidx.compose.ui.geometry.Offset(w * 0.88f, h * 0.24f), strokeWidth = w * 0.10f, cap = cap)
        drawLine(color, androidx.compose.ui.geometry.Offset(w * 0.12f, h * 0.50f), androidx.compose.ui.geometry.Offset(w * 0.88f, h * 0.50f), strokeWidth = w * 0.10f, cap = cap)
        drawLine(color, androidx.compose.ui.geometry.Offset(w * 0.12f, h * 0.76f), androidx.compose.ui.geometry.Offset(w * 0.88f, h * 0.76f), strokeWidth = w * 0.10f, cap = cap)
    }
}

@Composable
fun PlusGlyph(color: Color) {
    androidx.compose.foundation.Canvas(Modifier.size(26.dp)) {
        val w = size.width; val h = size.height
        val cap = androidx.compose.ui.graphics.StrokeCap.Round
        drawLine(color, androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.18f), androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.82f), strokeWidth = w * 0.11f, cap = cap)
        drawLine(color, androidx.compose.ui.geometry.Offset(w * 0.18f, h * 0.5f), androidx.compose.ui.geometry.Offset(w * 0.82f, h * 0.5f), strokeWidth = w * 0.11f, cap = cap)
    }
}
