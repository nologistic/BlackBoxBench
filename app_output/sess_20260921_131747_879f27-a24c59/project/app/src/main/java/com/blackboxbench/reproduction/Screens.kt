package com.blackboxbench.reproduction

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Badge
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.time.LocalDate

val BrandBlue = BrandNavy

@Composable
fun TasksLogo(size: Dp = 152.dp) {
    Canvas(modifier = Modifier.size(size)) {
        drawCircle(color = BrandLogoBlue)
        val w = this.size.width
        val path = Path().apply {
            moveTo(w * 0.26f, w * 0.52f)
            lineTo(w * 0.43f, w * 0.68f)
            lineTo(w * 0.76f, w * 0.34f)
        }
        drawPath(
            path = path,
            color = Color.White,
            style = Stroke(width = w * 0.095f, cap = StrokeCap.Round)
        )
    }
}

/** 冷启动引导页：Logo + 三个入口 */
@Composable
fun WelcomeScreen(
    notice: String,
    onDismissNotice: () -> Unit,
    onContinueWithoutSync: () -> Unit,
    onAddAccount: () -> Unit,
    onImportBackup: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(313.dp))
            TasksLogo(152.dp)
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onAddAccount,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandNavy,
                    contentColor = Color.White
                )
            ) { Text("添加账号") }
            Spacer(Modifier.height(18.dp))
            OutlinedButton(
                onClick = onContinueWithoutSync,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandTextDark),
                border = BorderStroke(1.dp, BrandOutline)
            ) { Text("继续但不同步") }
            Spacer(Modifier.height(18.dp))
            OutlinedButton(
                onClick = onImportBackup,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandTextDark),
                border = BorderStroke(1.dp, BrandOutline)
            ) { Text("导入 Tasks.org 备份") }
            Spacer(Modifier.height(56.dp))
        }
        if (notice.isNotEmpty()) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = onDismissNotice,
                title = { Text("提示") },
                text = { Text(notice) },
                confirmButton = { TextButton(onClick = onDismissNotice) { Text("知道了") } }
            )
        }
    }
}

/** 首页：抽屉导航 + 任务列表 + 筛选器 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenTask: (String) -> Unit,
    onNewTask: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    var view by remember { mutableStateOf<ViewKey>(ViewKey.All) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showFilter by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val message = Repo.message

    LaunchedEffect(message) {
        if (message.isNotBlank()) {
            snackbar.showSnackbar(message)
            Repo.message = ""
        }
    }

    val visible = Repo.tasksFor(view)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Tasks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "共 ${Repo.incompleteCount()} 项未完成",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text("全部任务") },
                    selected = view is ViewKey.All,
                    onClick = { view = ViewKey.All; scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("今天") },
                    selected = view is ViewKey.Today,
                    onClick = { view = ViewKey.Today; scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text(
                    "列表",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 28.dp, top = 4.dp, bottom = 4.dp)
                )
                Repo.lists.forEach { list ->
                    NavigationDrawerItem(
                        label = { Text(list.name) },
                        selected = (view as? ViewKey.ByList)?.listId == list.id,
                        badge = {
                            Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                Text("${Repo.incompleteCount(list.id)}")
                            }
                        },
                        onClick = {
                            view = ViewKey.ByList(list.id, list.name)
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text(
                    "标签",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 28.dp, top = 4.dp, bottom = 4.dp)
                )
                Repo.allTags().forEach { tag ->
                    NavigationDrawerItem(
                        label = { Text("#$tag") },
                        selected = (view as? ViewKey.ByTag)?.tag == tag,
                        onClick = {
                            view = ViewKey.ByTag(tag)
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                NavigationDrawerItem(
                    label = { Text("设置与筛选") },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() }; onOpenSettings() },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                TopAppBar(
                    title = { Text(view.title) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "菜单")
                        }
                    },
                    actions = {
                        TextButton(onClick = { showFilter = true }) { Text("筛选") }
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "更多")
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text("筛选与排序…") },
                                    onClick = { menuOpen = false; showFilter = true }
                                )
                                DropdownMenuItem(
                                    text = { Text("排序：${Repo.sortField.label}") },
                                    onClick = { menuOpen = false; Repo.cycleSort() }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (Repo.sortAsc) "方向：升序" else "方向：降序") },
                                    onClick = { menuOpen = false; Repo.setSort(Repo.sortField, !Repo.sortAsc) }
                                )
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = {
                    val listId = (view as? ViewKey.ByList)?.listId ?: Repo.lists.firstOrNull()?.id ?: ""
                    onNewTask(listId)
                }) { Icon(Icons.Filled.Add, contentDescription = "新建任务") }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (visible.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("这里还没有任务", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "点击右下角 + 新建一条",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(visible, key = { it.id }) { task ->
                            TaskRow(
                                task = task,
                                onToggle = { Repo.toggleComplete(task.id) },
                                onClick = { onOpenTask(task.id) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    if (showFilter) {
        FilterSheet(onDismiss = { showFilter = false })
    }
}

/** 单条任务：复选框 + 标题 + 元信息 */
@Composable
fun TaskRow(task: Task, onToggle: () -> Unit, onClick: () -> Unit) {
    val today = LocalDate.now()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = task.done, onCheckedChange = { onToggle() })
        Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
            Text(
                task.title,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (task.done) TextDecoration.LineThrough else null,
                color = if (task.done) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val meta = mutableListOf<String>()
            if (task.due.isNotBlank()) {
                val overdue = !task.done && isOverdue(task.due, today)
                meta.add((if (overdue) "逾期 " else "截止 ") + formatDueShort(task.due, today))
            }
            if (task.priority > PRIO_NONE) meta.add("优先级 " + priorityLabel(task.priority))
            if (task.repeat.isNotBlank()) meta.add(repeatLabel(task.repeat))
            if (task.subtasks.isNotEmpty()) {
                meta.add("子任务 ${task.subtasks.count { it.done }}/${task.subtasks.size}")
            }
            if (meta.isNotEmpty()) {
                Text(
                    meta.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (task.due.isNotBlank() && !task.done && isOverdue(task.due, today))
                        MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (task.tags.isNotEmpty()) {
                Text(
                    task.tags.joinToString(" ") { "#$it" },
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandBlue
                )
            }
            if (task.done && task.completedAt.isNotBlank()) {
                Text(
                    "完成于 ${formatStamp(task.completedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** 筛选器：隐藏已完成 / 排序字段 / 升降序 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("筛选与排序", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("隐藏已完成", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "开启后完成项从列表移除，数据仍保留",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = Repo.hideCompleted, onCheckedChange = { Repo.applyHideCompleted(it) })
            }
            Spacer(Modifier.height(20.dp))
            Text("排序方式", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SortField.values().forEach { field ->
                    FilterChip(
                        selected = Repo.sortField == field,
                        onClick = { Repo.setSort(field, Repo.sortAsc) },
                        label = { Text(field.label) }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("方向", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = Repo.sortAsc,
                    onClick = { Repo.setSort(Repo.sortField, true) },
                    label = { Text("升序") }
                )
                FilterChip(
                    selected = !Repo.sortAsc,
                    onClick = { Repo.setSort(Repo.sortField, false) },
                    label = { Text("降序") }
                )
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("完成") }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** 设置页：与筛选器一致的开关 + 数据概览 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "返回") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("隐藏已完成", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "关闭后完成项回到列表底部",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = Repo.hideCompleted, onCheckedChange = { Repo.applyHideCompleted(it) })
            }
            Spacer(Modifier.height(24.dp))
            Text("数据概览", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Text("未完成 ${Repo.incompleteCount()} 项")
            Text("已完成 ${Repo.completedCount()} 项")
            Text("列表 ${Repo.lists.size} 个 · 标签 ${Repo.allTags().size} 个")
            Spacer(Modifier.height(24.dp))
            Text(
                "示例数据均为虚构内容，仅用于复现验证。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
