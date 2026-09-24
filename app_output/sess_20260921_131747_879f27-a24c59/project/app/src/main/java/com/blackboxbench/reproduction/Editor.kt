package com.blackboxbench.reproduction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onDeleted: () -> Unit
) {
    val task = Repo.find(taskId)
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("任务详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "返回") }
                },
                actions = {
                    IconButton(onClick = { onEdit(taskId) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "编辑")
                    }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "删除")
                    }
                }
            )
        }
    ) { padding ->
        if (task == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("任务不存在")
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = task.done, onCheckedChange = { Repo.toggleComplete(task.id) })
                Text(
                    task.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (task.done) TextDecoration.LineThrough else null,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(12.dp))
            DetailLine("列表", Repo.listName(task.listId))
            DetailLine("截止", if (task.due.isBlank()) "无日期" else formatDue(task.due))
            DetailLine("优先级", priorityLabel(task.priority))
            DetailLine("重复", repeatLabel(task.repeat))
            if (task.tags.isNotEmpty()) DetailLine("标签", task.tags.joinToString(" ") { "#$it" })
            if (task.completedAt.isNotBlank()) DetailLine("完成于", formatStamp(task.completedAt))
            if (task.lastCompletedAt.isNotBlank()) DetailLine("上次完成", formatStamp(task.lastCompletedAt))
            if (task.notes.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text("备注", style = MaterialTheme.typography.titleSmall)
                Text(task.notes, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(20.dp))
            Text("子任务", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            if (task.subtasks.isEmpty()) {
                Text(
                    "无子任务",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                task.subtasks.forEach { sub ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = sub.done,
                            onCheckedChange = { Repo.toggleSubtask(task.id, sub.id) }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                sub.title,
                                textDecoration = if (sub.done) TextDecoration.LineThrough else null
                            )
                            val meta = mutableListOf<String>()
                            if (sub.due.isNotBlank()) meta.add(formatDue(sub.due))
                            if (sub.priority > PRIO_NONE) meta.add("优先级 " + priorityLabel(sub.priority))
                            if (meta.isNotEmpty()) {
                                Text(
                                    meta.joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { Repo.toggleComplete(task.id) }) {
                    Text(if (task.done) "恢复" else "标记完成")
                }
                OutlinedButton(onClick = { onEdit(taskId) }) { Text("编辑") }
                OutlinedButton(onClick = { Repo.move(task.id, -1) }) { Text("上移") }
                OutlinedButton(onClick = { Repo.move(task.id, 1) }) { Text("下移") }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除任务") },
            text = { Text("删除后「${task?.title ?: ""}」不再出现在任何筛选与统计中。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    task?.let { Repo.delete(it.id) }
                    onDeleted()
                }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp)
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

/** 新建 / 编辑表单 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(
    taskId: String?,
    defaultListId: String,
    onDone: () -> Unit
) {
    val existing = taskId?.let { Repo.find(it) }
    val today = LocalDate.now()

    var title by remember { mutableStateOf(existing?.title ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var listId by remember {
        mutableStateOf(existing?.listId ?: defaultListId.ifBlank { Repo.lists.firstOrNull()?.id ?: "" })
    }
    var due by remember { mutableStateOf(existing?.due ?: "") }
    var priority by remember { mutableStateOf(existing?.priority ?: PRIO_NONE) }
    var tags by remember { mutableStateOf(existing?.tags?.toList() ?: emptyList()) }
    var rule by remember { mutableStateOf(existing?.repeat ?: "") }
    var subs by remember {
        mutableStateOf<List<Pair<String, String>>>(
            existing?.subtasks?.map { it.id to it.title } ?: emptyList()
        )
    }
    var newTag by remember { mutableStateOf("") }
    var newSub by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var listMenu by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }

    fun applyDate(date: LocalDate) {
        // 若原来带时间，改日期时保留时间
        due = if (dueHasTime(due)) {
            val t = parseDue(due)!!
            date.atTime(t.hour, t.minute).format(DATETIME_FMT)
        } else date.format(DATE_FMT)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "新建任务" else "编辑任务") },
                navigationIcon = {
                    IconButton(onClick = onDone) { Icon(Icons.Filled.ArrowBack, contentDescription = "返回") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it; error = "" },
                label = { Text("标题（必填）") },
                isError = error.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )
            if (error.isNotBlank()) {
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("备注") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Text("列表", style = MaterialTheme.typography.titleSmall)
            Box {
                OutlinedButton(onClick = { listMenu = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(Repo.listName(listId))
                }
                DropdownMenu(expanded = listMenu, onDismissRequest = { listMenu = false }) {
                    Repo.lists.forEach { l ->
                        DropdownMenuItem(
                            text = { Text(l.name) },
                            onClick = { listId = l.id; listMenu = false }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("截止日期", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickDate.values().forEach { quick ->
                    val value = quickDue(quick, today)
                    FilterChip(
                        selected = due == value,
                        onClick = { due = value },
                        label = { Text(quick.label) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { showPicker = true }) { Text("选择日期") }
                Spacer(Modifier.width(12.dp))
                Text(
                    if (due.isBlank()) "当前：无日期" else "当前：${formatDue(due)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(16.dp))
            Text("优先级", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(PRIO_NONE, PRIO_LOW, PRIO_MED, PRIO_HIGH).forEach { p ->
                    FilterChip(
                        selected = priority == p,
                        onClick = { priority = p },
                        label = { Text(priorityLabel(p)) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("重复", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val options = listOf("" to "不重复", "FREQ=DAILY" to "每天", "WEEKLY" to "每周", "MONTHLY" to "每月")
                options.forEach { (key, label) ->
                    val selected = when (key) {
                        "" -> rule.isBlank()
                        "FREQ=DAILY" -> rule.contains("FREQ=DAILY")
                        "WEEKLY" -> rule.contains("FREQ=WEEKLY")
                        else -> rule.contains("FREQ=MONTHLY")
                    }
                    FilterChip(
                        selected = selected,
                        onClick = {
                            rule = when (key) {
                                "" -> ""
                                "FREQ=DAILY" -> "FREQ=DAILY"
                                "WEEKLY" -> {
                                    val existingDay = Regex("BYDAY=([A-Z,]+)").find(rule)?.value
                                    val d = parseDue(due)?.dayOfWeek ?: DayOfWeek.MONDAY
                                    existingDay ?: "FREQ=WEEKLY;BYDAY=" + dayCode(d)
                                }
                                else -> {
                                    val existingDay = Regex("BYMONTHDAY=\\d+").find(rule)?.value
                                    val d = parseDue(due)?.dayOfMonth ?: 1
                                    existingDay ?: "FREQ=MONTHLY;BYMONTHDAY=$d"
                                }
                            }
                        },
                        label = { Text(label) }
                    )
                }
            }
            if (rule.isNotBlank()) {
                Text(
                    "规则：" + repeatLabel(rule),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(16.dp))
            Text("标签", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            val known = (Repo.allTags() + tags).distinct()
            if (known.isNotEmpty()) {
                Column {
                    known.chunked(4).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { tag ->
                                FilterChip(
                                    selected = tags.contains(tag),
                                    onClick = {
                                        tags = if (tags.contains(tag)) tags - tag else tags + tag
                                    },
                                    label = { Text("#$tag") }
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newTag,
                    onValueChange = { newTag = it },
                    label = { Text("新标签") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    val t = newTag.trim()
                    if (t.isNotEmpty() && !tags.contains(t)) tags = tags + t
                    newTag = ""
                }) { Text("添加") }
            }

            Spacer(Modifier.height(16.dp))
            Text("子任务", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            subs.forEachIndexed { index, pair ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = pair.second,
                        onValueChange = { v ->
                            subs = subs.toMutableList().also { it[index] = pair.first to v }
                        },
                        label = { Text("子任务 ${index + 1}") },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { subs = subs.filterIndexed { i, _ -> i != index } }) {
                        Icon(Icons.Filled.Delete, contentDescription = "移除子任务")
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newSub,
                    onValueChange = { newSub = it },
                    label = { Text("新增子任务") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = {
                    val t = newSub.trim()
                    if (t.isNotEmpty()) subs = subs + ("s_" + System.currentTimeMillis() to t)
                    newSub = ""
                }) { Icon(Icons.Filled.Add, contentDescription = "添加子任务") }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    val name = title.trim()
                    if (name.isEmpty()) {
                        error = "标题不能为空"
                        return@Button
                    }
                    val target = existing ?: Repo.newTask(listId, Repo.nextOrder())
                    target.title = name
                    target.notes = notes.trim()
                    target.listId = listId
                    target.due = due
                    target.priority = priority
                    target.tags = tags.toMutableList()
                    target.repeat = rule
                    target.subtasks = subs.filter { it.second.isNotBlank() }
                        .map { (id, text) ->
                            existing?.subtasks?.find { it.id == id }?.also { it.title = text }
                                ?: Subtask(id = id, title = text)
                        }
                        .toMutableList()
                    Repo.upsert(target)
                    Repo.message = if (existing == null) "已创建「$name」" else "已保存「$name」"
                    onDone()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("保存") }
            Spacer(Modifier.height(40.dp))
        }
    }

    if (showPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = parseDue(due)?.toLocalDate()
                ?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        applyDate(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showPicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("取消") } }
        ) { DatePicker(state = state) }
    }
}

private fun dayCode(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "MO"
    DayOfWeek.TUESDAY -> "TU"
    DayOfWeek.WEDNESDAY -> "WE"
    DayOfWeek.THURSDAY -> "TH"
    DayOfWeek.FRIDAY -> "FR"
    DayOfWeek.SATURDAY -> "SA"
    DayOfWeek.SUNDAY -> "SU"
}
