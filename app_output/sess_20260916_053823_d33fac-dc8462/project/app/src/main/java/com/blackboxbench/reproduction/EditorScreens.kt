package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

val RowDivider = Color(0xFFE3E3E6)

@Composable
fun ScreenTopBar(title: String, onBack: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = MaterialTheme.colorScheme.onSurface)
        }
        Text(title, fontSize = 26.sp, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.weight(1f))
        trailing()
    }
}

@Composable
fun EditorRow(
    icon: @Composable () -> Unit,
    text: String,
    onClick: (() -> Unit)? = null,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    trailing: @Composable () -> Unit = { }
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable { onClick() } else it }
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(26.dp), contentAlignment = Alignment.Center) { icon() }
        Spacer(Modifier.width(20.dp))
        Text(text, fontSize = 21.sp, color = textColor, modifier = Modifier.weight(1f))
        trailing()
    }
    Divider(color = RowDivider, thickness = 1.dp)
}

@Composable
fun PriorityPicker(priority: Int, onPick: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        listOf(0, 1, 2, 3).forEach { p ->
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .clickable { onPick(p) },
                contentAlignment = Alignment.Center
            ) {
                val c = priorityColor(p)
                if (p == priority) {
                    Box(modifier = Modifier.size(30.dp).clip(CircleShape).background(c)) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                } else {
                    Box(modifier = Modifier.size(28.dp).clip(CircleShape).border(2.dp, c, CircleShape))
                }
            }
            Spacer(Modifier.width(26.dp))
        }
    }
}

@Composable
fun TaskEditorScreen(taskId: String, isNew: Boolean, listId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val task = Store.findTask(taskId)
    if (task == null) {
        onBack()
        return
    }
    var title by remember { mutableStateOf(task.title) }
    var notes by remember { mutableStateOf(task.notes) }
    var start by remember { mutableStateOf(task.start) }
    var due by remember { mutableStateOf(task.due) }
    var priority by remember { mutableStateOf(task.priority) }
    var repeatRule by remember { mutableStateOf(task.repeatRule) }
    var list by remember { mutableStateOf(task.listId) }
    var tags by remember { mutableStateOf(task.tags.toList()) }
    var subtasks by remember { mutableStateOf(task.subtasks.toList()) }
    var dateTarget by remember { mutableStateOf<String?>(null) }
    var repeatOpen by remember { mutableStateOf(false) }
    var listOpen by remember { mutableStateOf(false) }
    var subtaskInput by remember { mutableStateOf("") }
    var addingSubtask by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }

    fun save() {
        task.title = title
        task.notes = notes
        task.start = start
        task.due = due
        task.priority = priority
        task.repeatRule = repeatRule
        task.listId = list
        task.tags = tags.toMutableList()
        task.subtasks = subtasks.toMutableList()
        task.modifiedAt = System.currentTimeMillis()
        if (task.title.isBlank()) {
            Store.tasks.remove(task)
        }
        Store.save(context)
        onBack()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { save() }) {
                    Box(modifier = Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier.size(26.dp).clip(RoundedCornerShape(5.dp))
                                .border(2.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(5.dp))
                        )
                        Icon(Icons.Filled.Check, contentDescription = "保存", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = {
                    Store.tasks.remove(task)
                    Store.save(context)
                    onBack()
                }) { Icon(Icons.Filled.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.onSurface) }
            }

            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(30.dp).clip(RoundedCornerShape(6.dp))
                            .border(2.dp, priorityColor(priority), RoundedCornerShape(6.dp))
                    )
                    Spacer(Modifier.width(16.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (title.isEmpty()) Text("任务名称", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        BasicTextField(
                            value = title,
                            onValueChange = { title = it },
                            textStyle = TextStyle(fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Divider(color = RowDivider)

                EditorRow(
                    icon = { CalendarGlyph(MaterialTheme.colorScheme.onSurfaceVariant) },
                    text = dueDateLabel(start),
                    onClick = { dateTarget = "start" }
                )
                EditorRow(
                    icon = { ClockGlyph(MaterialTheme.colorScheme.onSurfaceVariant) },
                    text = dueDateLabel(due),
                    onClick = { dateTarget = "due" }
                )
                EditorRow(
                    icon = { RepeatGlyph(if (repeatRule == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary) },
                    text = if (repeatRule == null) "不重复" else "重复 " + repeatLabel(repeatRule!!),
                    onClick = { repeatOpen = true }
                )
                if (repeatRule != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("重复始于", fontSize = 19.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) { Text("截止日期", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface) }
                    }
                    Divider(color = RowDivider)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(26.dp), contentAlignment = Alignment.Center) {
                        FlagGlyph(MaterialTheme.colorScheme.onSurfaceVariant, 20.dp)
                    }
                    Spacer(Modifier.width(20.dp))
                    Text("优先级", fontSize = 21.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.weight(1f))
                    PriorityPicker(priority) { priority = it }
                }
                Divider(color = RowDivider)

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(26.dp), contentAlignment = Alignment.Center) {
                        ListGlyph(MaterialTheme.colorScheme.onSurfaceVariant, 20.dp)
                    }
                    Spacer(Modifier.width(20.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFFE0E0E0))
                            .clickable { listOpen = true }
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ListGlyph(Color(0xFF3C4043), 18.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(Store.listName(list), fontSize = 19.sp, color = Color(0xFF3C4043))
                        }
                    }
                }
                Divider(color = RowDivider)

                EditorRow(
                    icon = { TagGlyph(MaterialTheme.colorScheme.onSurfaceVariant, 22.dp) },
                    text = if (tags.isEmpty()) "添加标签" else tags.joinToString(" "),
                    onClick = { }
                )
                if (tags.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        tags.forEach { tag ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color(0xFFE0E0E0))
                                    .clickable { tags = tags - tag }
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TagGlyph(Color(0xFF3C4043), 16.dp)
                                Spacer(Modifier.width(6.dp))
                                Text(tag, fontSize = 17.sp, color = Color(0xFF3C4043))
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.Filled.Close, contentDescription = null, tint = Color(0xFF3C4043), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                EditorRow(
                    icon = { SubtaskGlyph(MaterialTheme.colorScheme.onSurfaceVariant) },
                    text = "添加子任务",
                    onClick = { addingSubtask = true }
                )
                if (addingSubtask) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(30.dp).clip(RoundedCornerShape(6.dp)).border(2.dp, PrimaryBlue, RoundedCornerShape(6.dp))
                        )
                        Spacer(Modifier.width(16.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (subtaskInput.isEmpty()) Text("添加子任务", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            BasicTextField(
                                value = subtaskInput,
                                onValueChange = { subtaskInput = it },
                                textStyle = TextStyle(fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        IconButton(onClick = { addingSubtask = false; subtaskInput = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                subtasks.forEach { s ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CheckBox(
                            priority = s.priority,
                            completed = !s.completedAt.isNullOrEmpty(),
                            onClick = { s.completedAt = if (s.completedAt.isNullOrEmpty()) nowStamp() else null }
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(s.title, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        IconButton(onClick = { subtasks = subtasks - s }) {
                            Icon(Icons.Filled.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                EditorRow(
                    icon = { BellGlyph(MaterialTheme.colorScheme.onSurfaceVariant) },
                    text = "",
                    onClick = { },
                    trailing = {
                        Column {
                            Text("启用提醒", fontSize = 21.sp, color = PriorityHigh)
                            Text("提醒在 Android 设置中被禁用", fontSize = 16.sp, color = PriorityHigh)
                        }
                    }
                )
                EditorRow(
                    icon = { ListGlyph(MaterialTheme.colorScheme.onSurfaceVariant) },
                    text = "",
                    onClick = { }
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(26.dp), contentAlignment = Alignment.Center) {
                        ListGlyph(MaterialTheme.colorScheme.onSurfaceVariant, 20.dp)
                    }
                    Spacer(Modifier.width(20.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (notes.isEmpty()) Text("描述", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        BasicTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            textStyle = TextStyle(fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Divider(color = RowDivider)
                EditorRow(icon = { PinGlyph(MaterialTheme.colorScheme.onSurfaceVariant) }, text = "添加位置")
                EditorRow(icon = { AttachmentGlyph(MaterialTheme.colorScheme.onSurfaceVariant) }, text = "添加附件")
                EditorRow(icon = { CalendarGlyph(MaterialTheme.colorScheme.onSurfaceVariant) }, text = "不添加到日历")
                Spacer(Modifier.height(60.dp))
            }
        }

        if (dateTarget != null) {
            DatePickerSheet(
                initial = if (dateTarget == "due") due else start,
                onDismiss = { dateTarget = null },
                onPick = { value ->
                    if (dateTarget == "due") due = value else start = value
                    dateTarget = null
                }
            )
        }
        if (repeatOpen) {
            RepeatDialog(
                current = repeatRule,
                onDismiss = { repeatOpen = false },
                onPick = { repeatRule = it; repeatOpen = false }
            )
        }
        if (listOpen) {
            ListPickerSheet(
                current = list,
                onDismiss = { listOpen = false },
                onPick = { list = it; listOpen = false }
            )
        }
        if (confirmDiscard) {
            ConfirmDialog(
                title = "确定要放弃你的修改吗？",
                message = null,
                onCancel = { confirmDiscard = false },
                onConfirm = { confirmDiscard = false; onBack() },
                confirmLabel = "放弃"
            )
        }
        if (addingSubtask && subtaskInput.isNotEmpty()) {
            // commit on close handled below via IME action placeholder
        }
    }
}

@Composable
fun CalendarGlyph(tint: Color, size: androidx.compose.ui.unit.Dp = 22.dp) {
    Box(modifier = Modifier.size(size).clip(RoundedCornerShape(5.dp)).border(2.dp, tint, RoundedCornerShape(5.dp))) {
        Box(modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter).background(tint))
    }
}

@Composable
fun BellGlyph(tint: Color, size: androidx.compose.ui.unit.Dp = 22.dp) {
    Box(
        modifier = Modifier.size(size).clip(RoundedCornerShape(50)).border(2.dp, tint, RoundedCornerShape(50)),
        contentAlignment = Alignment.Center
    ) { Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(tint)) }
}

fun dueDateLabel(value: String?): String {
    if (value.isNullOrEmpty()) return "无开始日期"
    val d = parseDateTime(value) ?: return "无开始日期"
    val h = humanDate(d.toLocalDate())
    return if (d.hour == 0 && d.minute == 0) h else "$h ${"%02d:%02d".format(d.hour, d.minute)}"
}

fun repeatLabel(rule: String): String = when {
    rule.contains("DAILY") -> "每天"
    rule.contains("WEEKLY") -> "每周"
    rule.contains("MONTHLY") -> "每月"
    rule.contains("YEARLY") -> "每年"
    else -> "自定义"
}

@Composable
fun DatePickerSheet(initial: String?, onDismiss: () -> Unit, onPick: (String?) -> Unit) {
    val today = LocalDate.now()
    var selection by remember { mutableStateOf(dueDate(initial) ?: today) }
    var time by remember { mutableStateOf(if (initial != null && initial.contains("T")) initial.substring(11, 16) else "无时间") }

    Box(modifier = Modifier.fillMaxSize().background(Color(0x88000000)).clickable { onDismiss() }) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable { }
                .padding(vertical = 12.dp)
        ) {
            Box(modifier = Modifier.align(Alignment.CenterHorizontally).width(44.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF9AA0A6)))
            Spacer(Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    DateOption("今天", today, "09:00", selection, time) { selection = today; time = "09:00" }
                    DateOption("明天", today.plusDays(1), "13:00", selection, time) { selection = today.plusDays(1); time = "13:00" }
                    DateOption("下周三", today.plusWeeks(1).with(java.time.DayOfWeek.WEDNESDAY), "17:00", selection, time) {
                        selection = today.plusWeeks(1).with(java.time.DayOfWeek.WEDNESDAY); time = "17:00"
                    }
                    Row(
                        modifier = Modifier.clickable { onPick(null) }.padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(22.dp).clip(CircleShape).border(2.dp, Color(0xFF9AA0A6), CircleShape))
                        Spacer(Modifier.width(14.dp))
                        Text("无日期", fontSize = 21.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                Column(modifier = Modifier.width(150.dp)) {
                    listOf("09:00", "13:00", "17:00", "20:00").forEach { t ->
                        Row(modifier = Modifier.clickable { time = t }.padding(vertical = 16.dp)) {
                            Text(t, fontSize = 19.sp, color = if (time == t) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    Text("挑选时间", fontSize = 19.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(vertical = 8.dp))
                    Text("无时间", fontSize = 19.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(vertical = 8.dp))
                }
            }
            Divider(color = RowDivider)
            Spacer(Modifier.height(8.dp))
            MonthGrid(selection) { selection = it }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消", fontSize = 19.sp) }
                TextButton(onClick = {
                    val s = if (time == "无时间") selection.toString() else "${selection}T$time"
                    onPick(s)
                }) { Text("确定", fontSize = 19.sp) }
            }
        }
    }
}

@Composable
fun DateOption(label: String, date: LocalDate, t: String, selection: LocalDate, time: String, onClick: () -> Unit) {
    Row(modifier = Modifier.clickable { onClick() }.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(22.dp), contentAlignment = Alignment.Center) {
            if (selection == date) Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(PrimaryBlue))
        }
        Spacer(Modifier.width(14.dp))
        Text(label, fontSize = 21.sp, color = if (selection == date) PrimaryBlue else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun MonthGrid(selection: LocalDate, onPick: (LocalDate) -> Unit) {
    val first = selection.withDayOfMonth(1)
    val len = selection.lengthOfMonth()
    val offset = (first.dayOfWeek.value % 7)
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("${selection.year}年${selection.monthValue}月", fontSize = 19.sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.weight(1f))
            Text("‹", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 16.dp))
            Text("›", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 16.dp))
        }
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach {
                Text(it, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        Spacer(Modifier.height(10.dp))
        var day = 1
        var cell = 0
        while (day <= len) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (i in 0 until 7) {
                    val idx = cell
                    Box(
                        modifier = Modifier.weight(1f).height(46.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (idx >= offset && day <= len) {
                            val d = selection.withDayOfMonth(day)
                            val selected = d == selection
                            Box(
                                modifier = Modifier.size(38.dp).clip(CircleShape)
                                    .background(if (selected) DeepBlue else Color.Transparent)
                                    .clickable { onPick(d) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "$day", fontSize = 18.sp,
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            day++
                        }
                    }
                    cell++
                }
            }
        }
    }
}

@Composable
fun RepeatDialog(current: String?, onDismiss: () -> Unit, onPick: (String?) -> Unit) {
    val options = listOf(
        "不重复" to null,
        "每天" to "FREQ=DAILY",
        "每周" to "FREQ=WEEKLY",
        "每月" to "FREQ=MONTHLY",
        "每年" to "FREQ=YEARLY",
        "自定义…" to "FREQ=DAILY"
    )
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column {
                options.forEach { (label, value) ->
                    val selected = (current == null && value == null) ||
                        (value != null && current != null && repeatLabel(current) == label)
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onPick(value) }.padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(26.dp).clip(CircleShape)
                                .border(2.dp, if (selected) PrimaryBlue else Color(0xFF9AA0A6), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(PrimaryBlue))
                        }
                        Spacer(Modifier.width(20.dp))
                        Text(label, fontSize = 21.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消", fontSize = 18.sp) } }
    )
}

@Composable
fun ListPickerSheet(current: String, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    Box(modifier = Modifier.fillMaxSize().background(Color(0x44000000)).clickable { onDismiss() }) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFF6F5FA))
                .clickable { }
                .padding(vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(16.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("搜索", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        textStyle = TextStyle(fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Divider(color = RowDivider)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ListGlyph(MaterialTheme.colorScheme.onSurface, 22.dp)
                Spacer(Modifier.width(18.dp))
                Text("本地清单", fontSize = 21.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            }
            Store.lists.filter { query.isEmpty() || it.name.contains(query, true) }.forEach { l ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (l.id == current) Color(0xFFE3E3E9) else Color.Transparent)
                        .clickable { onPick(l.id) }
                        .padding(horizontal = 24.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ListGlyph(MaterialTheme.colorScheme.onSurface, 20.dp)
                    Spacer(Modifier.width(18.dp))
                    Text(l.name, fontSize = 21.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
fun TagPickerScreen(taskId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val task = Store.findTask(taskId) ?: return
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(task.tags.toSet()) }

    fun commit() {
        task.tags = selected.toMutableList()
        task.modifiedAt = System.currentTimeMillis()
        Store.save(context)
        onBack()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { commit() }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = MaterialTheme.colorScheme.onSurface)
            }
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) Text("输入标签名称", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface)
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    textStyle = TextStyle(fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (query.isNotEmpty() && Store.allTags().none { it.equals(query, true) }) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable {
                    selected = selected + query
                    commit()
                }.padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(18.dp))
                Text("新建标签 \"$query\"", fontSize = 21.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        Store.allTags().forEach { tag ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TagGlyph(MaterialTheme.colorScheme.onSurface, 22.dp)
                Spacer(Modifier.width(18.dp))
                Text(tag, fontSize = 21.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier.size(28.dp).clip(RoundedCornerShape(4.dp))
                        .background(if (tag in selected) DeepBlue else Color.Transparent)
                        .border(2.dp, DeepBlue, RoundedCornerShape(4.dp))
                        .clickable { selected = if (tag in selected) selected - tag else selected + tag },
                    contentAlignment = Alignment.Center
                ) {
                    if (tag in selected) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    dark: Boolean,
    onDarkChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onAppearance: () -> Unit,
    onOpenList: () -> Unit,
    onNewList: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(title = "设置", onBack = onBack)
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            SettingsCard {
                SettingsRow(icon = { HeartGlyph(MaterialTheme.colorScheme.onSurface, 22.dp) }, title = "捐赠", subtitle = "考虑用捐赠显示您的支持！", onClick = { })
            }
            Spacer(Modifier.height(12.dp))
            SettingsCard {
                SettingsRow(icon = { ListGlyph(MaterialTheme.colorScheme.onSurface, 22.dp) }, title = "本地清单", onClick = onOpenList)
                SettingsRow(icon = { Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) }, title = "添加账号", onClick = { })
            }
            Spacer(Modifier.height(12.dp))
            SettingsCard {
                SettingsRow(icon = { PaletteGlyph(MaterialTheme.colorScheme.onSurface, 22.dp) }, title = "外观", onClick = onAppearance)
                SettingsRow(icon = { BellGlyph(MaterialTheme.colorScheme.onSurface, 22.dp) }, title = "通知", onClick = { })
            }
            Spacer(Modifier.height(12.dp))
            SettingsCard {
                SettingsRow(icon = { Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) }, title = "任务默认值", onClick = { })
                SettingsRow(icon = { ListGlyph(MaterialTheme.colorScheme.onSurface, 22.dp) }, title = "任务清单选项", onClick = { })
                SettingsRow(icon = { EditGlyph(MaterialTheme.colorScheme.onSurface, 22.dp) }, title = "编辑屏幕选项", onClick = { })
            }
            Spacer(Modifier.height(12.dp))
            SettingsCard {
                SettingsRow(icon = { ClockGlyph(MaterialTheme.colorScheme.onSurface, 22.dp) }, title = "日期和时间", onClick = { })
                SettingsRow(icon = { SortGlyph(MaterialTheme.colorScheme.onSurface, 22.dp) }, title = "导航抽屉", onClick = { })
                SettingsRow(icon = { ListGlyph(MaterialTheme.colorScheme.onSurface, 22.dp) }, title = "备份", onClick = { }, warn = true)
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun EditGlyph(tint: Color, size: androidx.compose.ui.unit.Dp = 22.dp) {
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.width(size * 0.7f).height(3.dp).background(tint).padding(top = 4.dp))
        Box(modifier = Modifier.width(size * 0.7f).height(size * 0.5f).border(2.dp, tint, RoundedCornerShape(3.dp)))
    }
}

@Composable
fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        content()
        Divider(color = RowDivider)
    }
}

@Composable
fun SettingsRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    warn: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 20.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(26.dp), contentAlignment = Alignment.Center) { icon() }
        Spacer(Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 21.sp, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) Text(subtitle, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (warn) Text("!", fontSize = 22.sp, color = PriorityMedium)
        else Text("›", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun AppearanceScreen(
    dark: Boolean,
    onDarkChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onNewList: () -> Unit
) {
    var themeDialog by remember { mutableStateOf(false) }
    var themeLabel by remember { mutableStateOf(if (dark) "暗色" else "系统默认") }
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(title = "外观", onBack = onBack)
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            SettingsCard {
                SettingsRow(icon = { }, title = "主题", subtitle = themeLabel, onClick = { themeDialog = true })
                SettingsRow(icon = { }, title = "动态", onClick = { })
                SettingsRow(icon = { Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(PrimaryBlue)) }, title = "颜色", onClick = { })
                SettingsRow(icon = { Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(PrimaryBlue)) }, title = "启动器图标", onClick = { })
            }
            Spacer(Modifier.height(12.dp))
            SettingsCard {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Markdown", fontSize = 21.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("在任务标题和描述中启用 Markdown", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        ConfigToggle(label = "", checked = false) { }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("启动时", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 24.dp, bottom = 8.dp))
            SettingsCard {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("打开上次查看的清单", fontSize = 21.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        ConfigToggle(label = "", checked = true) { }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("打开清单", fontSize = 19.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("我的任务", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("本地化", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 24.dp, bottom = 8.dp))
            SettingsCard {
                SettingsRow(icon = { }, title = "语言", subtitle = "中文 (中国)", onClick = { })
                SettingsRow(icon = { EditGlyph(MaterialTheme.colorScheme.onSurface, 22.dp) }, title = "贡献翻译", onClick = { })
            }
            Spacer(Modifier.height(40.dp))
        }
    }
    if (themeDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { themeDialog = false },
            title = { Text("主题", fontSize = 24.sp) },
            text = {
                Column {
                    listOf("亮色", "黑色", "暗色", "壁纸", "日/夜", "系统默认").forEach { label ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                themeLabel = label
                                onDarkChange(label == "暗色" || label == "黑色")
                                themeDialog = false
                            }.padding(vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val selected = themeLabel == label
                            Box(
                                modifier = Modifier.size(26.dp).clip(CircleShape)
                                    .border(2.dp, if (selected) DeepBlue else Color(0xFF9AA0A6), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(DeepBlue))
                            }
                            Spacer(Modifier.width(20.dp))
                            Text(label, fontSize = 21.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { themeDialog = false }) { Text("取消", fontSize = 18.sp) } }
        )
    }
}

@Composable
fun ListEditorScreen(listId: String?, onBack: () -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("新建清单") }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = MaterialTheme.colorScheme.onSurface)
            }
        }
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(BannerGray)
                    .padding(20.dp)
            ) {
                Column {
                    Text("这是本地清单", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(10.dp))
                    Text("此清单中的任务只存储在这台设备上。连接账号保护数据，在任何地方访问它。", fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onBack) { Text("关闭", fontSize = 17.sp, color = DeepBlue) }
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(24.dp)).background(DeepBlue)
                                .clickable { onBack() }.padding(horizontal = 24.dp, vertical = 10.dp)
                        ) { Text("添加账号", color = Color.White, fontSize = 17.sp) }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface).padding(20.dp)
            ) {
                Text("显示名称", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                BasicTextField(
                    value = name,
                    onValueChange = { name = it },
                    textStyle = TextStyle(fontSize = 21.sp, color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(20.dp))
            SettingsCard {
                SettingsRow(icon = { Box(modifier = Modifier.size(22.dp).clip(CircleShape).border(2.dp, Color(0xFF9AA0A6), CircleShape)) }, title = "颜色", onClick = { })
                SettingsRow(icon = { ListGlyph(MaterialTheme.colorScheme.onSurface, 22.dp) }, title = "图标", onClick = { })
            }
            Spacer(Modifier.height(20.dp))
            SettingsCard {
                SettingsRow(icon = { ListGlyph(MaterialTheme.colorScheme.onSurfaceVariant, 22.dp) }, title = "添加快捷方式到主屏幕", onClick = { })
                SettingsRow(icon = { ListGlyph(MaterialTheme.colorScheme.onSurfaceVariant, 22.dp) }, title = "添加小部件到主屏幕", onClick = { })
            }
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface)
                    .clickable {
                        Store.addList(name.ifBlank { "新建清单" })
                        Store.save(context)
                        onBack()
                    }
                    .padding(20.dp)
            ) { Text("保存", fontSize = 21.sp, color = MaterialTheme.colorScheme.onSurface) }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun FilterEditorScreen(onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var conditions by remember { mutableStateOf(listOf("我的任务" to Store.tasks.count { !it.isCompleted })) }
    var addOpen by remember { mutableStateOf(false) }
    val presets = listOf(
        "自定义…", "已过期", "仅今日", "明天", "今日以后", "任意开始日期",
        "无开始日期", "任何截止日期", "无截止日期", "无标签",
        "高优先级", "中等优先级", "低优先级"
    )
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(72.dp).background(DeepBlue).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = Color.White) }
            Spacer(Modifier.width(8.dp))
            Text("新建过滤器", fontSize = 24.sp, color = Color.White, modifier = Modifier.weight(1f))
            Text("?", fontSize = 20.sp, color = Color.White, modifier = Modifier.padding(end = 12.dp))
        }
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp)) {
                Text("显示名称", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                BasicTextField(
                    value = name, onValueChange = { name = it },
                    textStyle = TextStyle(fontSize = 21.sp, color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline))
            }
            EditorRow(icon = { Box(modifier = Modifier.size(22.dp).clip(CircleShape).border(2.dp, Color(0xFF9AA0A6), CircleShape)) }, text = "颜色")
            EditorRow(icon = { SortGlyph(MaterialTheme.colorScheme.onSurfaceVariant) }, text = "图标")
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
                Text("过滤条件", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                conditions.forEach { (label, count) ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(label, fontSize = 21.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        Text("$count", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { addOpen = true }.padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.width(18.dp))
                    Text("添加条件", fontSize = 21.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        if (addOpen) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0x44000000)).clickable { addOpen = false }) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp)
                        .clip(RoundedCornerShape(8.dp)).background(Color(0xFFF6F5FA)).clickable { }
                ) {
                    presets.forEach { p ->
                        Text(
                            p, fontSize = 21.sp, color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth().clickable {
                                if (p != "自定义…") conditions = conditions + (p to 0)
                                addOpen = false
                            }.padding(horizontal = 24.dp, vertical = 18.dp)
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { addOpen = false }) { Text("取消", fontSize = 18.sp) }
                    }
                }
            }
        }
    }
}