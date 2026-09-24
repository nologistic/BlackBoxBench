package com.blackboxbench.reproduction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(store: Store, taskId: String, onBack: () -> Unit) {
    store.version.value
    val isNew = taskId == "-1"
    val existing = if (isNew) null else store.task(taskId)

    var title by remember { mutableStateOf(existing?.title ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var listId by remember { mutableStateOf(existing?.listId ?: store.data.lists.first().id) }
    var tags by remember { mutableStateOf(existing?.tags?.toList() ?: emptyList()) }
    var priority by remember { mutableStateOf(existing?.priority ?: "none") }
    var due by remember { mutableStateOf(existing?.due ?: "") }
    var repeat by remember { mutableStateOf(existing?.repeat ?: "") }
    val subtasks = remember { existing?.subtasks ?: mutableListOf() }

    var menuOpen by remember { mutableStateOf(false) }
    var listOpen by remember { mutableStateOf(false) }
    var tagOpen by remember { mutableStateOf(false) }
    var prioOpen by remember { mutableStateOf(false) }
    var dateOpen by remember { mutableStateOf(false) }
    var timeOpen by remember { mutableStateOf(false) }
    var pendingDate by remember { mutableStateOf("") }
    var repeatOpen by remember { mutableStateOf(false) }
    var deleteOpen by remember { mutableStateOf(false) }
    var subInput by remember { mutableStateOf("") }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun save() {
        if (title.isBlank()) {
            scope.launch { snackbar.showSnackbar("标题不能为空") }
            return
        }
        if (isNew) {
            store.insertTask(Task(
                id = "", listId = listId, title = title.trim(), notes = notes.trim(),
                due = due, priority = priority, tags = tags.toMutableList(),
                repeat = repeat, subtasks = subtasks
            ))
        } else {
            val t = existing!!.copy()
            t.title = title.trim(); t.notes = notes.trim(); t.listId = listId
            t.due = due; t.priority = priority; t.tags = tags.toMutableList()
            t.repeat = repeat; t.subtasks = subtasks
            store.updateTask(t)
        }
        onBack()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "新任务" else "任务") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.Close, "关闭") }
                },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, "更多")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("复制任务") },
                            onClick = {
                                menuOpen = false
                                store.insertTask(Task(
                                    id = "",
                                    listId = listId,
                                    title = (title.ifBlank { "任务" } + " (副本)").trim(),
                                    notes = notes.trim(),
                                    due = due,
                                    priority = priority,
                                    tags = tags.toMutableList(),
                                    repeat = repeat
                                ))
                                onBack()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("删除") },
                            leadingIcon = { Icon(Icons.Default.Delete, null) },
                            onClick = {
                                menuOpen = false
                                if (isNew) onBack() else deleteOpen = true
                            }
                        )
                    }
                    IconButton(onClick = { save() }) {
                        Icon(Icons.Default.Check, "保存")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("新任务") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                placeholder = { Text("描述") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            LabeledRow(Icons.Default.List, "列表", store.listName(listId)) { listOpen = true }
            LabeledRow(Icons.Default.Star, "标签",
                if (tags.isEmpty()) "" else tags.joinToString(", ")) { tagOpen = true }
            LabeledRow(Icons.Default.Warning, "优先级", priorityLabel(priority)) { prioOpen = true }
            LabeledRow(Icons.Default.DateRange, "到期日期",
                formatDue(due).ifBlank { "" }) { dateOpen = true }
            LabeledRow(Icons.Default.Refresh, "重复", repeatLabel(repeat)) { repeatOpen = true }

            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                QuickDateChip("今天", Modifier.weight(1f)) {
                    due = LocalDate.now().toString()
                }
                QuickDateChip("明天", Modifier.weight(1f)) {
                    due = LocalDate.now().plusDays(1).toString()
                }
                QuickDateChip("下周日", Modifier.weight(1f)) {
                    var d = LocalDate.now().plusDays(1)
                    while (d.dayOfWeek != java.time.DayOfWeek.SUNDAY) d = d.plusDays(1)
                    due = d.toString()
                }
                QuickDateChip("无日期", Modifier.weight(1f)) { due = "" }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.KeyboardArrowRight, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(14.dp))
                Text("子任务", fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(8.dp))
                if (subtasks.isNotEmpty()) {
                    Text(
                        "${subtasks.count { it.completedAt.isNotEmpty() }}/${subtasks.size}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            for (s in subtasks) {
                val done = s.completedAt.isNotEmpty()
                Row(
                    Modifier.fillMaxWidth().padding(start = 30.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = done, onCheckedChange = {
                        if (!isNew) store.toggleSubtask(taskId, s.id)
                        else s.completedAt = if (done) "" else LocalDateTime.now()
                            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    })
                    Text(
                        s.title,
                        textDecoration = if (done) TextDecoration.LineThrough else null,
                        color = if (done) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(start = 30.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = subInput,
                    onValueChange = { subInput = it },
                    placeholder = { Text("新任务") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                IconButton(onClick = {
                    if (subInput.isNotBlank()) {
                        if (!isNew) store.addSubtask(taskId, subInput)
                        else subtasks.add(Subtask("s_tmp_" + System.currentTimeMillis(),
                            subInput.trim()))
                        subInput = ""
                    }
                }) { Icon(Icons.Default.Add, "添加子任务") }
            }

            Spacer(Modifier.height(8.dp))
            LabeledRow(Icons.Default.Notifications, "提醒", "无") { }
            LabeledRow(Icons.Default.LocationOn, "位置", "无") { }
            Spacer(Modifier.height(40.dp))
        }
    }

    if (listOpen) {
        val lists = store.data.lists
        AlertDialog(
            onDismissRequest = { listOpen = false },
            title = { Text("列表") },
            text = {
                Column {
                    for (l in lists) {
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable { listId = l.id; listOpen = false }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = listId == l.id, onClick = {
                                listId = l.id; listOpen = false
                            })
                            Text(l.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { listOpen = false }) { Text("取消") }
            }
        )
    }

    if (tagOpen) {
        TagPickerDialog(
            store = store,
            selected = tags,
            onDismiss = { tagOpen = false },
            onConfirm = { picked -> tags = picked; tagOpen = false }
        )
    }

    if (prioOpen) {
        val options = listOf(
            "none" to "无", "low" to "低 (!)", "medium" to "中 (!!)", "high" to "高 (!!!)"
        )
        AlertDialog(
            onDismissRequest = { prioOpen = false },
            title = { Text("优先级") },
            text = {
                Column {
                    for ((key, label) in options) {
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable { priority = key; prioOpen = false }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = priority == key, onClick = {
                                priority = key; prioOpen = false
                            })
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { prioOpen = false }) { Text("取消") }
            }
        )
    }

    if (dateOpen) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (dueDate(due) ?: LocalDate.now())
                .let { millisForDate(it) }
        )
        DatePickerDialog(
            onDismissRequest = { dateOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = state.selectedDateMillis
                    val picked = if (millis != null) localDateFromMillis(millis)
                    else LocalDate.now()
                    pendingDate = picked.toString()
                    dateOpen = false
                    if (hasTime(due)) {
                        val t = dueDateTime(due)!!
                        due = pendingDate + "T" +
                            "%02d:%02d".format(t.hour, t.minute)
                    } else {
                        timeOpen = true
                    }
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { dateOpen = false }) { Text("取消") }
            }
        ) { DatePicker(state) }
    }

    if (timeOpen) {
        val init = dueDateTime(due) ?: LocalDateTime.now().withSecond(0).withNano(0)
        val state = rememberTimePickerState(
            initialHour = init.hour, initialMinute = init.minute, is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { timeOpen = false },
            title = { Text("选择时间") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TimePicker(state = state)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(onClick = {
                            due = pendingDate; timeOpen = false
                        }) { Text("仅日期") }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    due = pendingDate + "T" + "%02d:%02d".format(state.hour, state.minute)
                    timeOpen = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { timeOpen = false }) { Text("取消") }
            }
        )
    }

    if (repeatOpen) {
        val options = listOf(
            "" to "不重复", "FREQ=DAILY" to "每天",
            "FREQ=WEEKLY" to "每周", "FREQ=MONTHLY" to "每月"
        )
        AlertDialog(
            onDismissRequest = { repeatOpen = false },
            title = { Text("重复") },
            text = {
                Column {
                    for ((key, label) in options) {
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable { repeat = key; repeatOpen = false }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = repeat == key, onClick = {
                                repeat = key; repeatOpen = false
                            })
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { repeatOpen = false }) { Text("取消") }
            }
        )
    }

    if (deleteOpen && existing != null) {
        AlertDialog(
            onDismissRequest = { deleteOpen = false },
            title = { Text("是否删除此任务？") },
            text = { Text("此操作无法撤销") },
            confirmButton = {
                TextButton(onClick = {
                    store.deleteTask(existing.id)
                    deleteOpen = false
                    onBack()
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { deleteOpen = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun QuickDateChip(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = modifier) { Text(text) }
}

@Composable
private fun LabeledRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(14.dp))
        Text(label, Modifier.width(88.dp))
        Text(
            value.ifBlank { "添加" },
            color = if (value.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp
        )
    }
}

@Composable
fun TagPickerDialog(
    store: Store,
    selected: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    store.version.value
    val chosen = remember { mutableStateOf(selected.toMutableSet()) }
    var newTagOpen by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("标签") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                val tags = store.allTags()
                if (tags.isEmpty()) {
                    Text("暂无标签", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                for (tag in tags) {
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable {
                                if (tag in chosen.value) chosen.value.remove(tag)
                                else chosen.value.add(tag)
                                chosen.value = chosen.value.toMutableSet()
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = tag in chosen.value,
                            onCheckedChange = {
                                if (tag in chosen.value) chosen.value.remove(tag)
                                else chosen.value.add(tag)
                                chosen.value = chosen.value.toMutableSet()
                            }
                        )
                        Text(tag)
                    }
                }
                TextButton(onClick = { newTagOpen = true }) {
                    Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("新建标签")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(chosen.value.toList()) }) {
                Icon(Icons.Default.Check, null, Modifier.size(16.dp)); Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
    if (newTagOpen) {
        TextFieldDialog(
            title = "新建标签",
            placeholder = "名称",
            confirmText = "确定",
            onDismiss = { newTagOpen = false },
            onConfirm = { name ->
                store.addTag(name.trim())
                if (name.isNotBlank()) chosen.value.add(name.trim())
                newTagOpen = false
            }
        )
    }
}

@Composable
fun TextFieldDialog(
    title: String,
    placeholder: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(placeholder) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
