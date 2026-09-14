package com.blackboxbench.reproduction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTasksScreen(
    lists: List<TaskListItem>,
    tasks: List<TaskItem>,
    onTasksChange: (List<TaskItem>) -> Unit,
    onOpenTask: (TaskItem) -> Unit,
    onAddTask: (String?) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFilter: () -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedList by remember { mutableStateOf<String?>(null) }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var showCompleted by remember { mutableStateOf(true) }
    var groupByList by remember { mutableStateOf(true) }
    var reminderVisible by remember { mutableStateOf(true) }
    var sortDialog by remember { mutableStateOf(false) }
    var moreDialog by remember { mutableStateOf(false) }
    var actionTask by remember { mutableStateOf<TaskItem?>(null) }

    val listName = lists.firstOrNull { it.id == selectedList }?.name
    val title = selectedTag ?: listName ?: "我的任务"
    val accent = lists.firstOrNull { it.id == selectedList }?.let { Color(it.color) }
        ?: MaterialTheme.colorScheme.primary
    val allTags = tasks.flatMap { it.tags }.distinct().sorted()
    val visible = tasks
        .filter { selectedList == null || it.listId == selectedList }
        .filter { selectedTag == null || it.tags.contains(selectedTag) }
        .filter { query.isBlank() || it.title.contains(query, true) || it.notes.contains(query, true) }
        .filter { showCompleted || !it.completed }
        .sortedWith(compareBy<TaskItem> { it.completed }.thenBy { it.due.ifBlank { "9999" } }.thenBy { it.title })

    fun toggleTask(task: TaskItem) {
        onTasksChange(tasks.map { if (it.id == task.id) nextOccurrence(it) else it })
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.88f)) {
                DrawerContent(
                    lists = lists,
                    tasks = tasks,
                    tags = allTags,
                    selectedList = selectedList,
                    selectedTag = selectedTag,
                    onAll = {
                        selectedList = null
                        selectedTag = null
                        scope.launch { drawerState.close() }
                    },
                    onList = {
                        selectedList = it
                        selectedTag = null
                        scope.launch { drawerState.close() }
                    },
                    onTag = {
                        selectedTag = it
                        selectedList = null
                        scope.launch { drawerState.close() }
                    },
                    onAddFilter = {
                        scope.launch { drawerState.close() }
                        onOpenFilter()
                    }
                )
            }
        }
    ) {
        Scaffold(
            containerColor = Color.White,
            bottomBar = {
                Surface(color = Color(0xFFFBF5FF), tonalElevation = 1.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding().height(72.dp)
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BottomSymbol("☰") { scope.launch { drawerState.open() } }
                        BottomSymbol("⌕") { searching = true }
                        BottomSymbol("↕") { sortDialog = true }
                        BottomSymbol("●") {}
                        BottomSymbol("⋮") { moreDialog = true }
                        Spacer(Modifier.width(70.dp))
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { onAddTask(selectedList) },
                    containerColor = accent,
                    contentColor = if (selectedList == null) Color.Black else Color.White,
                    shape = RoundedCornerShape(25.dp),
                    modifier = Modifier.size(64.dp)
                ) {
                    Text("+", fontSize = 38.sp, fontWeight = FontWeight.Light)
                }
            }
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(
                        start = 16.dp, end = 14.dp, top = 22.dp, bottom = 18.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (searching) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            placeholder = { Text("搜索任务") },
                            leadingIcon = {
                                Text(
                                    "‹",
                                    fontSize = 32.sp,
                                    modifier = Modifier.clickable { searching = false; query = "" }
                                )
                            },
                            trailingIcon = {
                                if (query.isNotEmpty()) {
                                    Text("×", fontSize = 28.sp, modifier = Modifier.clickable { query = "" })
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Text(
                            title,
                            color = accent,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "⚙",
                            fontSize = 31.sp,
                            modifier = Modifier.clickable { onOpenSettings() }.padding(5.dp)
                        )
                    }
                }

                if (reminderVisible) {
                    Surface(
                        color = Color(0xFFEFF1F8),
                        shape = RoundedCornerShape(30.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Column(Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
                            Text("启用提醒", fontSize = 24.sp)
                            Spacer(Modifier.height(12.dp))
                            Text("提醒在 Android 设置中被禁用", fontSize = 16.sp, color = Color(0xFF5C5D66))
                            Spacer(Modifier.height(18.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { reminderVisible = false }) {
                                    Text("关闭", fontSize = 16.sp)
                                }
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = {},
                                    shape = RoundedCornerShape(24.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B6F9F))
                                ) {
                                    Text("设置", fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }

                if (visible.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(bottom = 120.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "✓",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Light
                        )
                        Spacer(Modifier.height(10.dp))
                        Text("这里没有任务", fontSize = 20.sp)
                    }
                } else {
                    val grouped: Map<String, List<TaskItem>> =
                        if (groupByList && selectedList == null) visible.groupBy { it.listId }
                        else mapOf("" to visible)

                    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                        grouped.forEach { entry ->
                            val groupId = entry.key
                            val groupTasks = entry.value
                            if (groupId.isNotBlank()) {
                                val group = lists.firstOrNull { it.id == groupId }
                                item(key = "header_" + groupId) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(
                                            start = 16.dp, end = 22.dp, top = 24.dp, bottom = 12.dp
                                        ),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            group?.name ?: "默认清单",
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text("⌃", fontSize = 22.sp)
                                    }
                                }
                            }
                            items(groupTasks, key = { it.id }) { task ->
                                TaskRow(
                                    task = task,
                                    list = lists.firstOrNull { it.id == task.listId },
                                    showListChip = selectedList == null && !groupByList,
                                    onToggle = { toggleTask(task) },
                                    onToggleSub = { sub ->
                                        onTasksChange(tasks.map {
                                            if (it.id == task.id) {
                                                it.copy(subtasks = it.subtasks.map { child ->
                                                    if (child.id == sub.id) child.copy(completed = !child.completed) else child
                                                })
                                            } else it
                                        })
                                    },
                                    onOpen = { onOpenTask(task) },
                                    onLongPress = { actionTask = task }
                                )
                            }
                        }
                        if (!showCompleted) {
                            val hiddenCount = tasks.count { it.completed }
                            if (hiddenCount > 0) {
                                item {
                                    TextButton(
                                        onClick = { showCompleted = true },
                                        modifier = Modifier.fillMaxWidth().padding(12.dp)
                                    ) {
                                        Text("显示 " + hiddenCount + " 个已完成任务")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (sortDialog) {
        AlertDialog(
            onDismissRequest = { sortDialog = false },
            title = { Text("分组与排序") },
            text = {
                Column {
                    SettingSwitch("按清单分组", groupByList) { groupByList = it }
                    SettingSwitch("显示已完成任务", showCompleted) { showCompleted = it }
                    SettingSwitch("已完成任务移到底部", true) {}
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Text("排序依据", color = MaterialTheme.colorScheme.primary)
                    Text("截止日期 · 升序", modifier = Modifier.padding(vertical = 12.dp), fontSize = 17.sp)
                    Text("子任务 · 我的顺序", modifier = Modifier.padding(vertical = 8.dp))
                }
            },
            confirmButton = {
                TextButton(onClick = { sortDialog = false }) { Text("完成") }
            }
        )
    }

    if (moreDialog) {
        AlertDialog(
            onDismissRequest = { moreDialog = false },
            title = { Text("任务操作") },
            text = {
                Column {
                    DialogLine("清除已完成项目") {
                        onTasksChange(tasks.filterNot { it.completed })
                        moreDialog = false
                    }
                    DialogLine("折叠子任务") { moreDialog = false }
                    DialogLine("展开子任务") { moreDialog = false }
                    DialogLine("分享") { moreDialog = false }
                }
            },
            confirmButton = {
                TextButton(onClick = { moreDialog = false }) { Text("取消") }
            }
        )
    }

    actionTask?.let { selected ->
        AlertDialog(
            onDismissRequest = { actionTask = null },
            title = { Text("已选择 1 项") },
            text = {
                Column {
                    DialogLine("修改标签") { actionTask = null }
                    DialogLine("移动到清单") { actionTask = null }
                    DialogLine("分享") { actionTask = null }
                    DialogLine("克隆") {
                        onTasksChange(
                            tasks + selected.copy(
                                id = UUID.randomUUID().toString(),
                                title = selected.title + "（副本）"
                            )
                        )
                        actionTask = null
                    }
                    DialogLine("删除") {
                        onTasksChange(tasks.filterNot { it.id == selected.id })
                        actionTask = null
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { actionTask = null }) { Text("取消") }
            }
        )
    }
}
