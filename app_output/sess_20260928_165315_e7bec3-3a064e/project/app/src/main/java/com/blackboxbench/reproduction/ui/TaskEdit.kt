package com.blackboxbench.reproduction.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blackboxbench.reproduction.BlueColor
import com.blackboxbench.reproduction.Settings
import com.blackboxbench.reproduction.StateStore
import com.blackboxbench.reproduction.SubTask
import com.blackboxbench.reproduction.Task
import com.blackboxbench.reproduction.dayMillis
import com.blackboxbench.reproduction.deleteTask
import com.blackboxbench.reproduction.dueLabel
import com.blackboxbench.reproduction.fullDateLabel
import com.blackboxbench.reproduction.newId
import com.blackboxbench.reproduction.priorityColor
import com.blackboxbench.reproduction.repeatLabel
import java.util.Calendar

private data class SubDraft(val id: String, val title: String, val completed: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(
    taskId: String?,
    initialListId: String?,
    initialTagId: String? = null,
    onBack: (saved: Boolean) -> Unit,
    onPickLocation: () -> Unit,
    onPickTags: (List<String>, (List<String>) -> Unit) -> Unit
) {
    val existing = taskId?.let { id -> StateStore.tasks.firstOrNull { it.id == id } }

    var title by remember { mutableStateOf(existing?.title ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var listId by remember { mutableStateOf(existing?.listId ?: initialListId ?: StateStore.lists.firstOrNull { it.id == Settings.defList }?.id ?: StateStore.defaultList()?.id ?: "") }
    var due by remember { mutableStateOf(existing?.due) }
    var dueHasTime by remember { mutableStateOf(existing?.dueHasTime ?: false) }
    var start by remember { mutableStateOf(existing?.start) }
    var repeat by remember { mutableStateOf(existing?.repeat ?: "") }
    var priority by remember { mutableIntStateOf(existing?.priority ?: Settings.defPriority) }
    val tagIds = remember {
        mutableStateListOf<String>().apply {
            if (existing != null) addAll(existing.tagIds) else initialTagId?.let { add(it) }
        }
    }
    val subDrafts = remember {
        mutableStateListOf<SubDraft>().apply {
            existing?.let { t -> addAll(t.subtasks.map { SubDraft(it.id, it.title, it.completed) }) }
        }
    }
    var dirty by remember { mutableStateOf(false) }
    var showDiscard by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var datePanelFor by remember { mutableStateOf("") } // "due" or "start"
    var showRepeatDialog by remember { mutableStateOf(false) }
    var showListDialog by remember { mutableStateOf(false) }
    var showAttachmentDialog by remember { mutableStateOf(false) }
    var calendarOn by remember { mutableStateOf(false) }
    var timerOn by remember { mutableStateOf(false) }

    fun markDirty() { dirty = true }

    androidx.activity.compose.BackHandler {
        if (dirty) showDiscard = true else onBack(false)
    }

    fun save(): Boolean {
        if (title.isBlank()) return false
        val now = System.currentTimeMillis()
        if (existing != null) {
            existing.title = title
            existing.notes = notes
            existing.listId = listId
            existing.due = due
            existing.dueHasTime = dueHasTime
            existing.start = start
            existing.repeat = repeat
            existing.priority = priority
            existing.tagIds.clear()
            existing.tagIds.addAll(tagIds)
            // sync subtasks by id
            existing.subtasks.removeAll { st -> subDrafts.none { it.id == st.id } }
            subDrafts.forEach { d ->
                val st = existing.subtasks.firstOrNull { it.id == d.id }
                if (st == null) existing.subtasks.add(SubTask(d.id, d.title, d.completed, null))
                else {
                    st.title = d.title
                    st.completed = d.completed
                }
            }
            existing.modifiedAt = now
        } else {
            val subs = subDrafts.filter { it.title.isNotBlank() }
                .map { SubTask(it.id, it.title, it.completed, null) }
            StateStore.tasks.add(
                Task(
                    newId(), title, notes, listId, due, dueHasTime, start, repeat,
                    priority, tagIds.toList(), subs, false, null, now, now
                )
            )
        }
        StateStore.save()
        return true
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // top bar
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (save()) onBack(true) else if (dirty) showDiscard = true else onBack(false)
            }) {
                Text("✓", fontSize = 22.sp, color = BlueColor, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.weight(1f))
            if (existing != null) {
                IconButton(onClick = { showDelete = true }) {
                    Text("🗑", fontSize = 20.sp)
                }
            }
        }

        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            // 1. 任务名称
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (repeat.isNotEmpty()) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = "重复",
                        tint = Color(0xFFD64545),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                } else {
                    Box(
                        Modifier
                            .padding(end = 8.dp)
                            .size(22.dp)
                            .border(2.dp, Color(priorityColor(priority)), CircleShape)
                    )
                }
                TextField(
                    value = title,
                    onValueChange = { title = it; markDirty() },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("任务名称") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }

            // 2. 开始日期
            EditRow("🗓", start?.let { dueLabel(it, false) } ?: "无开始日期") {
                datePanelFor = "start"
            }
            // 3. 截止日期
            EditRow("🕒", due?.let { dueLabel(it, dueHasTime) } ?: "无截止日期") {
                datePanelFor = "due"
            }
            // 4. 重复
            if (repeat.isEmpty()) {
                EditRow("🔁", "不重复") {
                    showRepeatDialog = true
                }
            } else {
                EditRow("🔁", "重复  ${repeatLabel(repeat)}") {
                    showRepeatDialog = true
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { datePanelFor = "due" }
                        .padding(start = 44.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("重复始于", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            due?.let { dueLabel(it, dueHasTime) } ?: "无截止日期",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // 5. 优先级
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(0 to "无", 1 to "低", 2 to "中", 3 to "高").forEach { (p, name) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(end = 18.dp)
                            .clickable { priority = p; markDirty() }
                    ) {
                        Box(
                            Modifier
                                .size(26.dp)
                                .background(Color(priorityColor(p)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (priority == p) {
                                Text("✓", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // 6. 清单
            val listName = StateStore.lists.firstOrNull { it.id == listId }?.name ?: "默认清单"
            EditRow("≣", listName) {
                showListDialog = true
            }

            // 7. 标签
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        onPickTags(tagIds.toList()) { newIds ->
                            tagIds.clear()
                            tagIds.addAll(newIds)
                            markDirty()
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🏷", fontSize = 16.sp)
                Spacer(Modifier.width(16.dp))
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (tagIds.isEmpty()) {
                        Text("无标签", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        tagIds.forEach { tid ->
                            val tag = StateStore.tags.firstOrNull { it.id == tid }
                            if (tag != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(tag.name, fontSize = 13.sp)
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            "⨉",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.clickable {
                                                tagIds.remove(tid)
                                                markDirty()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 8. 子任务
            subDrafts.forEachIndexed { index, draft ->
                Row(
                    Modifier.fillMaxWidth().padding(start = 44.dp, end = 12.dp, top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(20.dp)
                            .border(2.dp, Color(0xFF9E9E9E), CircleShape)
                            .clickable {
                                subDrafts[index] = draft.copy(completed = !draft.completed)
                                markDirty()
                            }
                    )
                    Spacer(Modifier.width(10.dp))
                    TextField(
                        value = draft.title,
                        onValueChange = { subDrafts[index] = draft.copy(title = it); markDirty() },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("子任务") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = TextStyle(
                            textDecoration = if (draft.completed) TextDecoration.LineThrough else TextDecoration.None
                        )
                    )
                    Text(
                        "✕",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clickable {
                                subDrafts.removeAt(index)
                                markDirty()
                            }
                            .padding(8.dp)
                    )
                }
            }
            EditRow("⊕", "添加子任务") {
                subDrafts.add(SubDraft(newId(), "", false))
                markDirty()
            }

            // 9. 提醒
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text("🔔", fontSize = 16.sp)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("启用提醒", fontSize = 15.sp, color = Color(0xFFD64545))
                    Text(
                        "提醒在 Android 设置中被禁用",
                        fontSize = 12.sp,
                        color = Color(0xFFD64545)
                    )
                }
            }

            // 10. 描述
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("📝", fontSize = 16.sp, modifier = Modifier.padding(top = 12.dp))
                Spacer(Modifier.width(16.dp))
                TextField(
                    value = notes,
                    onValueChange = { notes = it; markDirty() },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("描述") },
                    minLines = 2,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }

            // 11. 位置
            EditRow("📍", "添加位置") {
                onPickLocation()
            }
            // 12. 附件
            EditRow("📎", "添加附件") {
                showAttachmentDialog = true
            }
            // 13. 日历
            EditRow("📅", if (calendarOn) "添加到日历" else "不添加到日历") {
                calendarOn = !calendarOn
                markDirty()
            }
            // 14. 计时器
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { timerOn = !timerOn; markDirty() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⏱", fontSize = 16.sp)
                Spacer(Modifier.width(16.dp))
                Text("计时器", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Text(if (timerOn) "⏸" else "▶", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            // 15. 创建时间
            if (existing != null) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text("🕐", fontSize = 16.sp)
                    Spacer(Modifier.width(16.dp))
                    Text(
                        "创建时间  " + fullDateLabel(existing.createdAt),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // ---------- dialogs ----------
    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            title = { Text("确定要放弃你的修改吗？") },
            confirmButton = {
                TextButton(onClick = { showDiscard = false }) { Text("继续编辑", color = BlueColor) }
            },
            dismissButton = {
                TextButton(onClick = { onBack(false) }) { Text("放弃") }
            }
        )
    }
    if (showDelete && existing != null) {
        ConfirmDialog(
            title = "确认删除？",
            onConfirm = {
                deleteTask(existing)
                onBack(false)
            },
            onDismiss = { showDelete = false }
        )
    }
    if (showRepeatDialog) {
        RepeatDialog(current = repeat, onPick = { repeat = it; markDirty() }, onDismiss = { showRepeatDialog = false })
    }
    if (showListDialog) {
        AlertDialog(
            onDismissRequest = { showListDialog = false },
            title = { Text("清单") },
            text = {
                Column {
                    StateStore.lists.forEach { l ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    listId = l.id
                                    markDirty()
                                    showListDialog = false
                                }
                                .padding(vertical = 10.dp)
                        ) {
                            Text("≣ " + l.name)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showListDialog = false }) { Text("取消") }
            }
        )
    }
    if (showAttachmentDialog) {
        AlertDialog(
            onDismissRequest = { showAttachmentDialog = false },
            title = { Text("添加附件") },
            text = {
                Column {
                    listOf("拍张照片", "录制一条便笺", "从相册选一张", "从存储中挑选").forEach { opt ->
                        Text(
                            opt,
                            Modifier
                                .fillMaxWidth()
                                .clickable { showAttachmentDialog = false }
                                .padding(vertical = 10.dp)
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAttachmentDialog = false }) { Text("取消") }
            }
        )
    }
    if (datePanelFor.isNotEmpty()) {
        val isDue = datePanelFor == "due"
        ModalBottomSheet(onDismissRequest = { datePanelFor = "" }) {
            DateTimePanel(
                initialDate = if (isDue) due else start,
                initialHasTime = if (isDue) dueHasTime else false,
                onCancel = { datePanelFor = "" },
                onConfirm = { dateMs, hasTime ->
                    if (isDue) {
                        due = dateMs
                        dueHasTime = hasTime
                    } else {
                        start = dateMs
                    }
                    markDirty()
                    datePanelFor = ""
                }
            )
        }
    }
}

@Composable
private fun EditRow(icon: String, text: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 16.sp)
        Spacer(Modifier.width(16.dp))
        Text(text, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun RepeatDialog(current: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val options = listOf(
        "" to "不重复",
        "FREQ=DAILY" to "每天",
        "FREQ=WEEKLY" to "每周",
        "FREQ=MONTHLY" to "每月",
        "FREQ=YEARLY" to "每年",
        "FREQ=WEEKLY;BYDAY=MO,WE,FR" to "自定义…"
    )
    var selected by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重复") },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { selected = value }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected == value, onClick = { selected = value })
                        Spacer(Modifier.width(8.dp))
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onPick(selected)
                onDismiss()
            }) { Text("确定", color = BlueColor) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

// ---------------- Date & time panel ----------------

@Composable
fun DateTimePanel(
    initialDate: Long?,
    initialHasTime: Boolean,
    onCancel: () -> Unit,
    onConfirm: (Long?, Boolean) -> Unit
) {
    var dateSel by remember { mutableStateOf(initialDate?.let { dayMillis(it) }) }
    var timeSel by remember { mutableIntStateOf(if (initialHasTime && initialDate != null) minutesOfDay(initialDate) else -1) }
    var monthOffset by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth()) {
            // left column: dates
            Column(Modifier.weight(1f)) {
                QuickDateItem("🗓", "今天", presetTime = 9 * 60) { d, t -> dateSel = d; timeSel = t }
                QuickDateItem("☀", "明天", presetTime = 13 * 60) { d, t -> dateSel = d; timeSel = t }
                QuickDateItem("💼", "下周二", presetTime = 9 * 60) { d, t -> dateSel = d; timeSel = t }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { dateSel = null; timeSel = -1 }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⊘", fontSize = 16.sp)
                    Spacer(Modifier.width(10.dp))
                    Text("无日期", fontSize = 15.sp)
                }
            }
            // right column: times
            Column(Modifier.weight(1f)) {
                listOf("☕" to "09:00", "☀" to "13:00", "🌇" to "17:00", "🌙" to "20:00").forEach { (ic, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { timeSel = minutesFromLabel(label) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(ic, fontSize = 16.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(label, fontSize = 15.sp, color = if (timeSel == minutesFromLabel(label)) BlueColor else MaterialTheme.colorScheme.onSurface)
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🕐", fontSize = 16.sp)
                    Spacer(Modifier.width(10.dp))
                    Text("挑选时间", fontSize = 15.sp)
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { timeSel = -1 }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⊘", fontSize = 16.sp)
                    Spacer(Modifier.width(10.dp))
                    Text("无时间", fontSize = 15.sp)
                }
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        // calendar header
        val base = Calendar.getInstance()
        base.add(Calendar.MONTH, monthOffset)
        val year = base.get(Calendar.YEAR)
        val month = base.get(Calendar.MONTH)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                "${year}年${month + 1}月 ▾",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { monthOffset-- }) {
                Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "上个月")
            }
            IconButton(onClick = { monthOffset++ }) {
                Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "下个月")
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth()) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { d ->
                Text(
                    d,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        val cal = Calendar.getInstance()
        cal.set(year, month, 1)
        val firstDayWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Monday = 0
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val todayStart = dayMillis(System.currentTimeMillis())
        var cell = 0
        var day = 1 - firstDayWeek
        while (day <= maxDay) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    Box(
                        Modifier.weight(1f).height(38.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day in 1..maxDay) {
                            val thisDay = Calendar.getInstance().apply {
                                set(year, month, day)
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                            val isToday = thisDay == todayStart
                            val isSel = dateSel == thisDay
                            val label = day.toString()
                            Box(
                                Modifier
                                    .size(32.dp)
                                    .background(
                                        if (isSel) BlueColor else Color.Transparent,
                                        CircleShape
                                    )
                                    .then(
                                        if (isToday && !isSel) Modifier.border(1.dp, BlueColor, CircleShape)
                                        else Modifier
                                    )
                                    .clickable {
                                        dateSel = thisDay
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    fontSize = 14.sp,
                                    color = when {
                                        isSel -> Color.White
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                    day++
                    cell++
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("⌨", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onCancel) { Text("取消") }
            TextButton(onClick = {
                val result = if (dateSel == null) {
                    Pair(null, false)
                } else {
                    val calOut = Calendar.getInstance().apply { timeInMillis = dateSel!! }
                    if (timeSel >= 0) {
                        calOut.set(Calendar.HOUR_OF_DAY, timeSel / 60)
                        calOut.set(Calendar.MINUTE, timeSel % 60)
                        Pair(calOut.timeInMillis, true)
                    } else {
                        calOut.set(Calendar.HOUR_OF_DAY, 0)
                        calOut.set(Calendar.MINUTE, 0)
                        calOut.set(Calendar.SECOND, 0)
                        calOut.set(Calendar.MILLISECOND, 0)
                        Pair(calOut.timeInMillis, false)
                    }
                }
                onConfirm(result.first, result.second)
            }) { Text("确定", color = BlueColor) }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun QuickDateItem(
    icon: String,
    label: String,
    presetTime: Int,
    onPick: (Long, Int) -> Unit
) {
    val today = dayMillis(System.currentTimeMillis())
    val dateMs = when (label) {
        "今天" -> today
        "明天" -> today + 86400000L
        else -> {
            val c = java.util.Calendar.getInstance()
            c.timeInMillis = today + 86400000L
            while (c.get(java.util.Calendar.DAY_OF_WEEK) != java.util.Calendar.TUESDAY) {
                c.add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
            dayMillis(c.timeInMillis)
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onPick(dateMs, presetTime) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 16.sp)
        Spacer(Modifier.width(10.dp))
        Text(label, fontSize = 15.sp)
    }
}

private fun minutesOfDay(ms: Long): Int {
    val c = Calendar.getInstance().apply { timeInMillis = ms }
    return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
}

private fun minutesFromLabel(label: String): Int {
    val parts = label.split(":")
    return parts[0].toInt() * 60 + parts[1].toInt()
}
