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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth

/** 任务新建/编辑页 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorScreen(app: AppState, taskId: String?, presetListId: String?) {
    val existing = taskId?.let { app.data.taskById(it) }
    val isNew = existing == null

    var title by remember { mutableStateOf(existing?.title ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var listId by remember { mutableStateOf(existing?.listId ?: presetListId ?: app.data.lists.firstOrNull()?.id ?: "") }
    var priority by remember { mutableStateOf(existing?.priority ?: Priority.NONE) }
    var due by remember { mutableStateOf(existing?.due) }
    var reminder by remember { mutableStateOf(existing?.reminder) }
    var repeat by remember { mutableStateOf(existing?.repeat ?: "") }
    var tags by remember { mutableStateOf(existing?.tags ?: emptyList()) }
    var subtasks by remember { mutableStateOf(existing?.subtasks ?: emptyList()) }
    var newSubtask by remember { mutableStateOf("") }

    var menuOpen by remember { mutableStateOf(false) }
    var listDialog by remember { mutableStateOf(false) }
    var tagDialog by remember { mutableStateOf(false) }
    var priorityDialog by remember { mutableStateOf(false) }
    var dateDialog by remember { mutableStateOf(false) }
    var reminderDialog by remember { mutableStateOf(false) }
    var repeatDialog by remember { mutableStateOf(false) }
    var timerDialog by remember { mutableStateOf(false) }

    fun buildTask(): TaskItem = (existing ?: TaskItem(
        id = "t${System.currentTimeMillis()}", listId = listId, title = title.trim(),
    )).copy(
        listId = listId, title = title.trim(), notes = notes, priority = priority,
        due = due, reminder = reminder, repeat = repeat, tags = tags, subtasks = subtasks,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "添加任务" else "编辑任务") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (title.isNotBlank()) app.saveTask(buildTask()) else app.pop()
                    }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                },
                actions = {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, "更多") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("保存并关闭") }, onClick = {
                            menuOpen = false
                            if (title.isNotBlank()) app.saveTask(buildTask()) else app.pop()
                        })
                        DropdownMenuItem(text = { Text("保存并新建") }, onClick = {
                            menuOpen = false
                            if (title.isNotBlank()) app.saveTask(buildTask(), andNew = true)
                        })
                        DropdownMenuItem(text = { Text("放弃更改") }, onClick = { menuOpen = false; app.pop() })
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(app.data.listById(listId)?.color ?: 0xFF009688),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
            )
        },
        containerColor = Color.White,
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("任务标题") },
                textStyle = TextStyle(fontSize = 19.sp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                placeholder = { Text("描述") },
                textStyle = TextStyle(fontSize = 15.sp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))

            EditorRow(
                leading = { Box(Modifier.size(14.dp).background(Color(app.data.listById(listId)?.color ?: 0xFF009688), CircleShape)) },
                label = "列表",
                value = app.data.listById(listId)?.name ?: "",
                onClick = { listDialog = true },
            )
            EditorRow(label = "标签", value = if (tags.isEmpty()) "" else tags.joinToString("、") { "#$it" }, onClick = { tagDialog = true })
            EditorRow(
                leading = { PriorityBadge(priority) },
                label = "优先级",
                value = priority.label,
                onClick = { priorityDialog = true },
            )
            EditorRow(
                leading = { Text("📅", fontSize = 16.sp) },
                label = "截止日期",
                value = formatDue(due) ?: "",
                onClick = { dateDialog = true },
            )
            EditorRow(
                leading = { Text("🔔", fontSize = 15.sp) },
                label = "提醒",
                value = when {
                    reminder == null -> ""
                    reminder == "due-30m" -> "截止日期前 30 分钟"
                    else -> formatDue(reminder) ?: ""
                },
                onClick = { reminderDialog = true },
            )
            EditorRow(
                leading = { Icon(Icons.Default.Refresh, null, tint = Color(0xFF757575), modifier = Modifier.size(18.dp)) },
                label = "重复",
                value = repeatLabel(repeat),
                onClick = { repeatDialog = true },
            )
            EditorRow(
                leading = { Text("⏱", fontSize = 15.sp) },
                label = "定时器",
                value = "",
                onClick = { timerDialog = true },
            )

            Spacer(Modifier.height(24.dp))
            Text("子任务", fontSize = 13.sp, color = Color(0xFF9E9E9E))
            Spacer(Modifier.height(6.dp))
            subtasks.forEachIndexed { idx, st ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                ) {
                    Checkbox(
                        checked = !st.completedAt.isNullOrEmpty(),
                        onCheckedChange = { checked ->
                            subtasks = subtasks.toMutableList().also {
                                it[idx] = st.copy(completedAt = if (checked) nowIso() else null)
                            }
                        },
                        colors = CheckboxDefaults.colors(checkedColor = ACCENT),
                        modifier = Modifier.size(30.dp),
                    )
                    Text(
                        st.title,
                        fontSize = 15.sp,
                        textDecoration = if (!st.completedAt.isNullOrEmpty()) TextDecoration.LineThrough else null,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { subtasks = subtasks.filterIndexed { i, _ -> i != idx } }) {
                        Icon(Icons.Default.Delete, "删除子任务", tint = Color(0xFFBDBDBD), modifier = Modifier.size(18.dp))
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newSubtask,
                    onValueChange = { newSubtask = it },
                    placeholder = { Text("添加子任务…") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = {
                        if (newSubtask.isNotBlank()) {
                            subtasks = subtasks + SubTask("s${System.currentTimeMillis()}", newSubtask.trim())
                            newSubtask = ""
                        }
                    },
                ) { Icon(Icons.Default.Check, "添加", tint = ACCENT) }
            }
            Spacer(Modifier.height(48.dp))
        }
    }

    // ---- 对话框组 ----
    if (listDialog) {
        AlertDialog(
            onDismissRequest = { listDialog = false },
            title = { Text("选择列表") },
            text = {
                Column {
                    app.data.lists.forEach { l ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { listId = l.id; listDialog = false },
                        ) {
                            Box(Modifier.size(14.dp).background(Color(l.color), CircleShape))
                            Spacer(Modifier.width(12.dp))
                            Text(l.name, modifier = Modifier.weight(1f))
                            if (l.id == listId) Text("✓", color = ACCENT)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { listDialog = false }) { Text("取消") } },
        )
    }
    if (tagDialog) {
        TagPickerDialog(
            allTags = app.data.allTags,
            selected = tags,
            onConfirm = { tags = it; tagDialog = false },
            onDismiss = { tagDialog = false },
        )
    }
    if (priorityDialog) {
        AlertDialog(
            onDismissRequest = { priorityDialog = false },
            title = { Text("选择优先级") },
            text = {
                Column {
                    Priority.entries.forEach { p ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { priority = p; priorityDialog = false },
                        ) {
                            RadioButton(selected = priority == p, onClick = { priority = p; priorityDialog = false })
                            PriorityBadge(p)
                            Spacer(Modifier.width(8.dp))
                            Text(p.label)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { priorityDialog = false }) { Text("取消") } },
        )
    }
    if (dateDialog) {
        DatePickerDialog(
            initial = due,
            onConfirm = { due = it; dateDialog = false },
            onDismiss = { dateDialog = false },
        )
    }
    if (reminderDialog) {
        ReminderDialog(
            initial = reminder,
            onConfirm = { reminder = it; reminderDialog = false },
            onClear = { reminder = null; reminderDialog = false },
            onDismiss = { reminderDialog = false },
        )
    }
    if (repeatDialog) {
        RepeatDialog(
            initial = repeat,
            onConfirm = { repeat = it; repeatDialog = false },
            onDismiss = { repeatDialog = false },
        )
    }
    if (timerDialog) {
        AlertDialog(
            onDismissRequest = { timerDialog = false },
            title = { Text("定时器") },
            text = { Text("定时器用于记录任务用时。此演示未实现计时逻辑。") },
            confirmButton = { TextButton(onClick = { timerDialog = false }) { Text("好的") } },
        )
    }
}

@Composable
private fun PriorityBadge(p: Priority) {
    when (p) {
        Priority.HIGH -> Box(
            Modifier.size(16.dp).background(Color(0xFFE53935), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text("!", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        Priority.MEDIUM -> Box(Modifier.size(12.dp).background(Color(0xFFFBC02D), CircleShape))
        Priority.LOW -> Box(Modifier.size(12.dp).background(Color(0xFF42A5F5), CircleShape))
        Priority.NONE -> Box(Modifier.size(12.dp).background(Color(0xFFBDBDBD), CircleShape))
    }
}

/** 属性行：图标 + 标签 + 右侧当前值 */
@Composable
private fun EditorRow(
    leading: (@Composable () -> Unit)? = null,
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    Surface(color = Color.Transparent, modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 14.dp),
        ) {
            leading?.invoke()
            Spacer(Modifier.width(leading?.let { 14.dp } ?: 4.dp))
            Text(label, fontSize = 15.sp, color = Color(0xFF212121), modifier = Modifier.weight(1f))
            if (value.isNotEmpty()) {
                Text(value, fontSize = 14.sp, color = Color(0xFF757575))
            } else {
                Text("＋", fontSize = 16.sp, color = ACCENT)
            }
        }
    }
}

/** 日期选择：快捷 chips（今天/明天/下周日/无日期）+ 当月日历 + 确定/取消 */
@Composable
fun DatePickerDialog(initial: String?, onConfirm: (String?) -> Unit, onDismiss: () -> Unit) {
    val today = LocalDate.now()
    var picked by remember { mutableStateOf(parseDue(initial)?.toLocalDate() ?: today) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择日期") },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("今天" to today, "明天" to today.plusDays(1), "下周日" to today.plusWeeks(1).let {
                        var d = it
                        while (d.dayOfWeek != java.time.DayOfWeek.SUNDAY) d = d.plusDays(1)
                        d
                    }).forEach { (label, date) ->
                        TextButton(onClick = { picked = date }) { Text(label, color = ACCENT) }
                    }
                    TextButton(onClick = { onConfirm(null) }) { Text("无日期", color = ACCENT) }
                }
                val month = YearMonth.from(picked)
                val days = month.lengthOfMonth()
                val firstOffset = month.atDay(1).dayOfWeek.value % 7
                Text("${month.year}年${month.monthValue}月", fontSize = 14.sp, modifier = Modifier.padding(bottom = 6.dp))
                // 星期表头
                Row(Modifier.fillMaxWidth()) {
                    listOf("日", "一", "二", "三", "四", "五", "六").forEach {
                        Text(it, fontSize = 12.sp, color = Color(0xFF9E9E9E), modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
                var d = 1
                while (d <= days) {
                    Row(Modifier.fillMaxWidth()) {
                        repeat(7) { col ->
                            val day = if (col < firstOffset || d > days) null else d++
                            Box(Modifier.weight(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                                if (day != null) {
                                    val date = month.atDay(day)
                                    val isPicked = date == picked
                                    TextButton(
                                        onClick = { picked = date },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                                        modifier = Modifier.size(36.dp),
                                    ) {
                                        Text(
                                            "$day",
                                            fontSize = 13.sp,
                                            color = if (isPicked) Color.White else Color(0xFF212121),
                                            modifier = if (isPicked) Modifier.background(ACCENT, CircleShape).padding(6.dp) else Modifier.padding(6.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(picked.toString()) }) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

/** 提醒对话框：相对截止日期 / 固定时间 */
@Composable
fun ReminderDialog(initial: String?, onConfirm: (String?) -> Unit, onClear: () -> Unit, onDismiss: () -> Unit) {
    var mode by remember { mutableStateOf(if (initial != null) 1 else 0) } // 0 相对 1 固定
    var day by remember { mutableStateOf(parseDue(initial)?.toLocalDate() ?: LocalDate.now()) }
    var hour by remember { mutableStateOf(parseDue(initial)?.hour ?: 8) }
    var minute by remember { mutableStateOf(parseDue(initial)?.minute ?: 0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("提醒") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { mode = 0 }) {
                    RadioButton(selected = mode == 0, onClick = { mode = 0 })
                    Text("相对截止日期")
                }
                if (mode == 0) {
                    Text("截止日期前 30 分钟", fontSize = 13.sp, color = Color(0xFF757575), modifier = Modifier.padding(start = 48.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { mode = 1 }) {
                    RadioButton(selected = mode == 1, onClick = { mode = 1 })
                    Text("固定时间")
                }
                if (mode == 1) {
                    Text(
                        "${day.monthValue}月${day.dayOfMonth}日  %02d:%02d".format(hour, minute),
                        fontSize = 15.sp, modifier = Modifier.padding(start = 48.dp, bottom = 8.dp),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { day = day.minusDays(1) }) { Text("前一天", color = ACCENT) }
                        TextButton(onClick = { day = day.plusDays(1) }) { Text("后一天", color = ACCENT) }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("时间", fontSize = 14.sp)
                        Spacer(Modifier.width(12.dp))
                        TextButton(onClick = { hour = (hour + 1) % 24 }) { Text("%02d".format(hour), color = ACCENT) }
                        Text(":")
                        TextButton(onClick = { minute = (minute + 15) % 60 }) { Text("%02d".format(minute), color = ACCENT) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (mode == 1) onConfirm(day.atTime(hour, minute).toString().replace(" ", "T").substring(0, 16))
                else onConfirm("due-30m")
            }) { Text("确定") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onClear) { Text("清除", color = Color(0xFFE53935)) }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        },
    )
}

/** 重复对话框 */
@Composable
fun RepeatDialog(initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var picked by remember { mutableStateOf(initial) }
    val options = listOf(
        "" to "不重复",
        "FREQ=DAILY" to "每天",
        "FREQ=DAILY;BYDAY=MO,TU,WE,TH,FR" to "每个工作日",
        "FREQ=WEEKLY;BYDAY=MO" to "每周（周一）",
        "FREQ=MONTHLY;BYMONTHDAY=15" to "每月（15 日）",
        "FREQ=YEARLY" to "每年",
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重复") },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { picked = value },
                    ) {
                        RadioButton(selected = picked == value, onClick = { picked = value })
                        Text(label)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(picked) }) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

/** 标签选择对话框：既有标签勾选 + 新建标签 */
@Composable
fun TagPickerDialog(allTags: List<String>, selected: List<String>, onConfirm: (List<String>) -> Unit, onDismiss: () -> Unit) {
    var checked by remember { mutableStateOf(selected.toMutableList()) }
    var newTag by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("标签") },
        text = {
            Column {
                if (allTags.isEmpty()) Text("没有标签", fontSize = 13.sp, color = Color(0xFF9E9E9E))
                allTags.forEach { tag ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable {
                            checked = if (checked.contains(tag)) (checked - tag).toMutableList() else (checked + tag).toMutableList()
                        },
                    ) {
                        Checkbox(
                            checked = checked.contains(tag),
                            onCheckedChange = {
                                checked = if (checked.contains(tag)) (checked - tag).toMutableList() else (checked + tag).toMutableList()
                            },
                            colors = CheckboxDefaults.colors(checkedColor = ACCENT),
                        )
                        Text("#$tag")
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newTag,
                        onValueChange = { newTag = it },
                        placeholder = { Text("新建标签…") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = {
                        if (newTag.isNotBlank() && !checked.contains(newTag.trim())) {
                            checked = (checked + newTag.trim()).toMutableList(); newTag = ""
                        }
                    }) { Icon(Icons.Default.Add, "添加", tint = ACCENT) }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(checked.toList()) }) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
