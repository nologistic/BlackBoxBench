package com.blackboxbench.reproduction

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.UUID

@Composable
fun TaskEditorScreen(
    draft: EditorDraft,
    lists: List<TaskListItem>,
    onCancel: () -> Unit,
    onSave: (TaskItem, TaskListItem?) -> Unit,
    onDelete: () -> Unit
) {
    var title by remember(draft.id) { mutableStateOf(draft.title) }
    var notes by remember(draft.id) { mutableStateOf(draft.notes) }
    var due by remember(draft.id) { mutableStateOf(draft.due) }
    var priority by remember(draft.id) { mutableStateOf(draft.priority) }
    var listId by remember(draft.id) { mutableStateOf(draft.listId) }
    var tagsText by remember(draft.id) { mutableStateOf(draft.tags.joinToString(", ")) }
    var repeat by remember(draft.id) { mutableStateOf(draft.repeat) }
    var subtasks by remember(draft.id) { mutableStateOf(draft.subtasks) }
    var newSubtask by remember { mutableStateOf("") }
    var dateDialog by remember { mutableStateOf(false) }
    var repeatDialog by remember { mutableStateOf(false) }
    var listDialog by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }
    var newListName by remember { mutableStateOf("") }
    var timerRunning by remember { mutableStateOf(false) }
    var elapsed by remember { mutableIntStateOf(0) }

    LaunchedEffect(timerRunning) {
        while (timerRunning) {
            delay(1000)
            elapsed++
        }
    }

    fun save(newList: TaskListItem? = null) {
        if (title.isBlank()) return
        val task = TaskItem(
            id = draft.id,
            listId = newList?.id ?: listId,
            title = title.trim(),
            notes = notes.trim(),
            due = due,
            priority = priority,
            tags = tagsText.split(",").map { it.trim() }.filter { it.isNotBlank() }.distinct(),
            repeat = repeat,
            completed = false,
            subtasks = subtasks
        )
        onSave(task, newList)
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("▣", fontSize = 30.sp, modifier = Modifier.clickable { save() }.padding(5.dp))
            Spacer(Modifier.weight(1f))
            if (!draft.isNew) {
                Text(
                    "♜",
                    fontSize = 27.sp,
                    modifier = Modifier.clickable { deleteDialog = true }.padding(7.dp)
                )
            }
        }
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = { Text("任务名称") },
            singleLine = true,
            leadingIcon = { CheckSquare(false, priorityColor(priority)) {} },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
        )
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            EditorLine("◴", "无开始日期", muted = true) {}
            EditorLine("◷", if (due.isBlank()) "无截止日期" else formatDue(due)) {
                dateDialog = true
            }
            EditorLine("↔", if (repeat.isBlank()) "不重复" else repeatLabel(repeat)) {
                repeatDialog = true
            }
            Row(
                Modifier.fillMaxWidth().height(66.dp).padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⚑", color = Color(0xFF9C9CA3), fontSize = 22.sp, modifier = Modifier.width(54.dp))
                Text("优先级", fontSize = 17.sp, modifier = Modifier.weight(1f))
                listOf("none", "low", "medium", "high").forEach { value ->
                    Surface(
                        modifier = Modifier.padding(horizontal = 9.dp).size(25.dp).clickable { priority = value },
                        shape = RoundedCornerShape(50),
                        color = if (priority == value) priorityColor(value) else Color.Transparent,
                        border = BorderStroke(2.dp, priorityColor(value))
                    ) {}
                }
            }
            HorizontalDivider(color = Color(0xFFE5E2E7))
            EditorLine("☷", lists.firstOrNull { it.id == listId }?.name ?: "默认清单") {
                listDialog = true
            }
            OutlinedTextField(
                value = tagsText,
                onValueChange = { tagsText = it },
                label = { Text("标签（用逗号分隔）") },
                leadingIcon = { Text("▱", fontSize = 24.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("↳", fontSize = 25.sp, color = Color(0xFF9C9CA3), modifier = Modifier.width(54.dp))
                    Text("子任务", fontSize = 17.sp)
                }
                subtasks.forEach { sub ->
                    Row(
                        Modifier.fillMaxWidth().padding(start = 54.dp, top = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = sub.completed,
                            onCheckedChange = { checked ->
                                subtasks = subtasks.map {
                                    if (it.id == sub.id) it.copy(completed = checked) else it
                                }
                            }
                        )
                        Text(sub.title, modifier = Modifier.weight(1f))
                        Text(
                            "×",
                            fontSize = 24.sp,
                            modifier = Modifier.clickable {
                                subtasks = subtasks.filterNot { it.id == sub.id }
                            }.padding(8.dp)
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(start = 54.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newSubtask,
                        onValueChange = { newSubtask = it },
                        placeholder = { Text("添加子任务") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = {
                        if (newSubtask.isNotBlank()) {
                            subtasks = subtasks + SubTaskItem(
                                UUID.randomUUID().toString(),
                                newSubtask.trim()
                            )
                            newSubtask = ""
                        }
                    }) {
                        Text("添加")
                    }
                }
            }
            HorizontalDivider(color = Color(0xFFE5E2E7))
            EditorLine(
                "♧",
                "启用提醒",
                subtitle = "提醒在 Android 设置中被禁用",
                tint = Color(0xFFD32F2F)
            ) {}
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("描述") },
                leadingIcon = { Text("≡", fontSize = 23.sp) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
            EditorLine("⌖", "添加位置", muted = true) {}
            EditorLine("⌕", "添加附件", muted = true) {}
            EditorLine("▣", "不添加到日历") {}
            val elapsedLabel = if (elapsed == 0) "计时器" else "已用时 00:" + elapsed.toString().padStart(2, '0')
            EditorLine("◉", elapsedLabel) { timerRunning = !timerRunning }
            Spacer(Modifier.height(26.dp))
        }
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onCancel) { Text("取消") }
            Spacer(Modifier.width(10.dp))
            Button(onClick = { save() }, enabled = title.isNotBlank()) { Text("保存") }
        }
    }

    if (dateDialog) {
        val tomorrow = LocalDate.now().plusDays(1)
        val saturday = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
        AlertDialog(
            onDismissRequest = { dateDialog = false },
            title = { Text("选择日期和时间") },
            text = {
                Column {
                    DialogLine("今天") {
                        due = LocalDate.now().toString()
                        dateDialog = false
                    }
                    DialogLine("明天") {
                        due = tomorrow.toString() + "T13:00"
                        dateDialog = false
                    }
                    DialogLine("下周六") {
                        due = saturday.plusWeeks(1).toString()
                        dateDialog = false
                    }
                    DialogLine("无日期") {
                        due = ""
                        dateDialog = false
                    }
                    HorizontalDivider()
                    Text(
                        "快捷时间",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Row {
                        listOf("09:00", "13:00", "17:00", "20:00").forEach { value ->
                            TextButton(onClick = {
                                due = (if (due.isBlank()) tomorrow.toString() else due.take(10)) + "T" + value
                                dateDialog = false
                            }) {
                                Text(value)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { dateDialog = false }) { Text("取消") }
            }
        )
    }

    if (repeatDialog) {
        AlertDialog(
            onDismissRequest = { repeatDialog = false },
            title = { Text("重复") },
            text = {
                Column {
                    listOf(
                        "" to "不重复",
                        "FREQ=DAILY" to "每天",
                        "FREQ=WEEKLY" to "每周",
                        "FREQ=MONTHLY" to "每月",
                        "FREQ=YEARLY" to "每年",
                        "FREQ=WEEKLY;INTERVAL=2" to "自定义：每 2 周"
                    ).forEach { option ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                repeat = option.first
                                repeatDialog = false
                            }.padding(vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = repeat == option.first,
                                onClick = {
                                    repeat = option.first
                                    repeatDialog = false
                                }
                            )
                            Text(option.second, fontSize = 17.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { repeatDialog = false }) { Text("取消") }
            }
        )
    }

    if (listDialog) {
        AlertDialog(
            onDismissRequest = { listDialog = false },
            title = { Text("选择清单") },
            text = {
                Column {
                    lists.forEach { list ->
                        DialogLine(list.name) {
                            listId = list.id
                            listDialog = false
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    OutlinedTextField(
                        value = newListName,
                        onValueChange = { newListName = it },
                        label = { Text("新建本地清单") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newListName.isNotBlank()) {
                        val created = TaskListItem(
                            UUID.randomUUID().toString(),
                            newListName.trim(),
                            0xFFC3D51F
                        )
                        listId = created.id
                        listDialog = false
                        save(created)
                    }
                }) {
                    Text("创建并保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { listDialog = false }) { Text("取消") }
            }
        )
    }

    if (deleteDialog) {
        AlertDialog(
            onDismissRequest = { deleteDialog = false },
            title = { Text("删除任务？") },
            text = { Text("删除后该任务将不再参与任何筛选和统计。") },
            confirmButton = {
                TextButton(onClick = {
                    deleteDialog = false
                    onDelete()
                }) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteDialog = false }) { Text("取消") }
            }
        )
    }
}

fun repeatLabel(value: String) = when {
    value.contains("DAILY") -> "每天"
    value.contains("MONTHLY") -> "每月"
    value.contains("YEARLY") -> "每年"
    value.contains("INTERVAL=2") -> "每 2 周"
    else -> "每周"
}
