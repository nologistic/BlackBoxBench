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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalDateTime

class Draft {
    var id by mutableStateOf<String?>(null)
    var title by mutableStateOf("")
    var notes by mutableStateOf("")
    var dueMillis by mutableStateOf<Long?>(null)
    var startKind by mutableStateOf(START_NONE)
    var startMillis by mutableStateOf<Long?>(null)
    var startOffsetDays by mutableStateOf(0)
    var repeat by mutableStateOf(REPEAT_NONE)
    var repeatFromDue by mutableStateOf(false)
    var priority by mutableStateOf(0)
    var listId by mutableStateOf(Store.lists.firstOrNull()?.id ?: "")
    var location by mutableStateOf<String?>(null)
    var timerSeconds by mutableStateOf(0L)
    var completed by mutableStateOf(false)
    var modifiedAt by mutableStateOf(System.currentTimeMillis())
    val tags = mutableStateListOf<String>()
    val subtasks = mutableStateListOf<Subtask>()

    fun due(): LocalDateTime? = dueMillis?.let { Dates.fromMillis(it) }

    fun start(): LocalDateTime? = when (startKind) {
        START_ABS -> startMillis?.let { Dates.fromMillis(it) }
        START_DUE_MINUS -> due()?.minusDays(startOffsetDays.toLong())
        else -> null
    }

    fun startSummary(): String = when (startKind) {
        START_ABS -> start()?.let { Dates.dayLabel(it.toLocalDate()) } ?: "无开始日期"
        START_DUE_MINUS -> if (startOffsetDays >= 7) "截止日期前一周" else "截止日期前一天"
        else -> "无开始日期"
    }

    fun dueSummary(): String {
        val d = due() ?: return "无截止日期"
        return Dates.dateTimeLabel(d)
    }

    fun repeatLabel(): String = when (repeat) {
        REPEAT_DAILY -> "每天"
        REPEAT_WEEKLY -> "每周"
        REPEAT_MONTHLY -> "每月"
        REPEAT_YEARLY -> "每年"
        else -> "不重复"
    }
}

fun Store.newDraft(baseViewId: String): Draft {
    val d = Draft()
    d.listId = when {
        baseViewId.startsWith("list_") -> baseViewId
        else -> lists.firstOrNull()?.id ?: ""
    }
    if (baseViewId.startsWith("tag_")) d.tags.add(baseViewId.removePrefix("tag_"))
    return d
}

fun Store.draftOf(taskId: String): Draft? {
    val t = tasks.firstOrNull { it.id == taskId } ?: return null
    val d = Draft()
    d.id = t.id
    d.title = t.title
    d.notes = t.notes
    d.dueMillis = t.dueMillis
    d.startKind = t.startKind
    d.startMillis = t.startMillis
    d.startOffsetDays = t.startOffsetDays
    d.repeat = t.repeat
    d.repeatFromDue = t.repeatFromDue
    d.priority = t.priority
    d.listId = t.listId
    d.location = t.location
    d.timerSeconds = t.timerSeconds
    d.completed = t.completed
    d.modifiedAt = t.modifiedAt
    d.tags.addAll(t.tags)
    d.subtasks.addAll(t.subtasks.map { it.dup() })
    return d
}

fun Store.commit(d: Draft) {
    if (d.title.isBlank()) return
    val existing = d.id?.let { id -> tasks.firstOrNull { it.id == id } }
    val target = existing ?: TaskItem(
        id = nextTaskId(), listId = d.listId, title = d.title,
        order = (tasks.maxOfOrNull { it.order } ?: 0) + 1
    ).also { tasks.add(it) }
    target.title = d.title
    target.notes = d.notes
    target.dueMillis = d.dueMillis
    target.startKind = d.startKind
    target.startMillis = d.startMillis
    target.startOffsetDays = d.startOffsetDays
    target.repeat = d.repeat
    target.repeatFromDue = d.repeatFromDue
    target.priority = d.priority
    target.listId = d.listId
    target.location = d.location
    target.timerSeconds = d.timerSeconds
    target.modifiedAt = System.currentTimeMillis()
    target.tags.clear(); target.tags.addAll(d.tags)
    target.subtasks.clear(); target.subtasks.addAll(d.subtasks)
    d.tags.forEach { if (!tags.contains(it)) tags.add(it) }
    if (d.completed) {
        if (target.completedMillis == null) target.completedMillis = System.currentTimeMillis()
    } else {
        target.completedMillis = null
    }
    save()
}

// ------------------------------------------------------------------ editor

@Composable
fun TaskEditorScreen(taskId: String?, baseViewId: String, onClose: () -> Unit) {
    val draft = remember(taskId, baseViewId) {
        if (taskId == null) Store.newDraft(baseViewId) else Store.draftOf(taskId) ?: Store.newDraft(baseViewId)
    }
    var revision by remember { mutableStateOf(0) }
    var sheet by remember { mutableStateOf<String?>(null) }
    var showDelete by remember { mutableStateOf(false) }
    var newSubtask by remember { mutableStateOf<String?>(null) }
    var showTimer by remember { mutableStateOf(false) }
    var fullScreen by remember { mutableStateOf<String?>(null) }
    var showCalendarPrompt by remember { mutableStateOf(false) }

    fun refresh() { revision++ }

    Box(Modifier.fillMaxSize().background(C.screenBg)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(
                Modifier.fillMaxWidth().height(84.dp).padding(start = 16.dp, end = 15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(40.dp).clickable {
                        Store.commit(draft); onClose()
                    },
                    contentAlignment = Alignment.Center
                ) { DrawIcon("save", C.text, 24.dp, 1.8.dp) }
                Spacer(Modifier.weight(1f))
                if (taskId != null) {
                    Box(
                        Modifier.size(40.dp).clickable { showDelete = true },
                        contentAlignment = Alignment.Center
                    ) { DrawIcon("trash", C.text, 24.dp, 1.8.dp) }
                }
            }
            HLine()

            // title row
            Row(
                Modifier.fillMaxWidth().heightIn(min = 68.dp).padding(start = 21.dp, end = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CheckBoxView(
                    checked = draft.completed, size = 22.dp, color = Color(0xFF9E9E9E),
                    onToggle = { draft.completed = !draft.completed; refresh() }
                )
                Spacer(Modifier.width(14.dp))
                Box(Modifier.weight(1f).padding(vertical = 14.dp)) {
                    if (draft.title.isEmpty()) {
                        Text("任务名称", color = C.textFaint, fontSize = 18.sp)
                    }
                    AppTextField(
                        value = draft.title,
                        onValueChange = { draft.title = it },
                        fontSize = 18f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            HLine()

            EditorRow("calendar", draft.startSummary(), draft.startKind != START_NONE) { sheet = "start" }
            EditorRow("clock", draft.dueSummary(), draft.dueMillis != null) { sheet = "due" }
            Column(Modifier.fillMaxWidth().clickable { sheet = "repeat" }) {
                Row(
                    Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DrawIcon("repeat", C.textDim, 24.dp)
                    Spacer(Modifier.width(22.dp))
                    Text(
                        draft.repeatLabel(),
                        color = if (draft.repeat != REPEAT_NONE) C.text else C.textDim,
                        fontSize = 17.sp
                    )
                }
                if (draft.repeat != REPEAT_NONE) {
                    Row(
                        Modifier.fillMaxWidth().height(56.dp).padding(start = 64.dp, end = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("重复始于", color = C.text, fontSize = 17.sp)
                        Spacer(Modifier.width(14.dp))
                        Box(
                            Modifier.clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFEFEFEF))
                                .clickable {
                                    draft.repeatFromDue = !draft.repeatFromDue; refresh()
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                if (draft.repeatFromDue) "截止日期" else "完成日期",
                                color = C.text, fontSize = 17.sp
                            )
                        }
                    }
                }
            }
            HLine()

            Row(
                Modifier.fillMaxWidth().height(64.dp).padding(start = 20.dp, end = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DrawIcon("flag", C.textDim, 24.dp)
                Spacer(Modifier.width(22.dp))
                Text("优先级", color = C.text, fontSize = 17.sp)
                Spacer(Modifier.weight(1f))
                listOf(0, 1, 2, 3).forEach { p ->
                    Box(
                        Modifier.size(40.dp).clickable { draft.priority = p; refresh() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (draft.priority == p) {
                            Box(
                                Modifier.size(22.dp).clip(CircleShape)
                                    .background(priorityColor(p)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(Modifier.size(8.dp).clip(CircleShape).background(Color.White))
                            }
                        } else {
                            Box(
                                Modifier.size(22.dp).clip(CircleShape)
                                    .background(Color.Transparent)
                                    .then(Modifier)
                            ) {
                                Box(
                                    Modifier.size(20.dp).clip(CircleShape)
                                        .background(Color(0xFFF2F2F2))
                                )
                            }
                        }
                    }
                }
            }
            HLine()

            // list row
            Row(
                Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 20.dp)
                    .clickable { sheet = "list" },
                verticalAlignment = Alignment.CenterVertically
            ) {
                DrawIcon("list", C.textDim, 24.dp)
                Spacer(Modifier.width(22.dp))
                val list = Store.listById(draft.listId)
                if (list != null) {
                    Chip(
                        text = list.name, bg = Color(list.color), fg = Color.White,
                        icon = list.icon, iconTint = Color.White
                    )
                }
            }
            HLine()

            // tags
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DrawIcon("tag", C.textDim, 24.dp)
                    Spacer(Modifier.width(22.dp))
                    Row(
                        Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (draft.tags.isEmpty()) {
                            Box(Modifier.clickable { fullScreen = "tags" }) {
                                Text("添加标签", color = C.textDim, fontSize = 17.sp, modifier = Modifier.padding(vertical = 8.dp))
                            }
                        } else {
                            Row(
                                Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                draft.tags.take(2).forEach { tag ->
                                    Chip(text = tag, icon = "tag", trailing = {
                                        Box(Modifier.size(18.dp).clickable {
                                            draft.tags.remove(tag); refresh()
                                        }, contentAlignment = Alignment.Center) {
                                            DrawIcon("close", Color(0xFF3C3C3C), 15.dp, 1.6.dp)
                                        }
                                    })
                                }
                                Box(Modifier.clickable { fullScreen = "tags" }) {
                                    Text("＋", color = C.textDim, fontSize = 18.sp)
                                }
                            }
                        }
                    }
                }
            }
            HLine()

            // subtasks
            Column(Modifier.fillMaxWidth()) {
                draft.subtasks.forEach { sub ->
                    Row(
                        Modifier.fillMaxWidth().height(56.dp).padding(start = 44.dp, end = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CheckBoxView(
                            checked = sub.completed, size = 20.dp,
                            color = if (sub.priority > 0) priorityColor(sub.priority) else Color(0xFF9E9E9E),
                            onToggle = { sub.completed = !sub.completed; refresh() }
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(sub.title, color = C.text, fontSize = 17.sp, modifier = Modifier.weight(1f))
                        Box(
                            Modifier.size(30.dp).clickable {
                                draft.subtasks.remove(sub); refresh()
                            },
                            contentAlignment = Alignment.Center
                        ) { DrawIcon("close", C.textDim, 18.dp) }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DrawIcon("subtask", C.textDim, 24.dp)
                    Spacer(Modifier.width(22.dp))
                    if (newSubtask == null) {
                        Box(Modifier.weight(1f).clickable { newSubtask = "" }) {
                            Text("添加子任务", color = C.textDim, fontSize = 17.sp, modifier = Modifier.padding(vertical = 10.dp))
                        }
                    } else {
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            CheckBoxView(checked = false, size = 20.dp, color = Color(0xFF9E9E9E), onToggle = { })
                            Spacer(Modifier.width(14.dp))
                            AppTextField(
                                value = newSubtask ?: "",
                                onValueChange = { newSubtask = it },
                                fontSize = 17f,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                Modifier.size(32.dp).clickable {
                                    val v = newSubtask?.trim().orEmpty()
                                    if (v.isNotEmpty()) {
                                        draft.subtasks.add(Subtask("s" + System.currentTimeMillis(), v))
                                    }
                                    newSubtask = null; refresh()
                                },
                                contentAlignment = Alignment.Center
                            ) { DrawIcon("check", C.blue, 20.dp, 2.dp) }
                            Box(
                                Modifier.size(32.dp).clickable { newSubtask = null },
                                contentAlignment = Alignment.Center
                            ) { DrawIcon("close", C.textDim, 18.dp) }
                        }
                    }
                }
            }
            HLine()

            // reminder
            Row(
                Modifier.fillMaxWidth().height(84.dp).padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DrawIcon("bell", C.red, 24.dp)
                Spacer(Modifier.width(22.dp))
                Column {
                    Text("启用提醒", color = C.red, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text("提醒在 Android 设置中被禁用", color = C.red, fontSize = 13.sp)
                }
            }
            HLine()

            // description
            Row(
                Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                DrawIcon("note", C.textDim, 24.dp)
                Spacer(Modifier.width(22.dp))
                Box(Modifier.weight(1f)) {
                    if (draft.notes.isEmpty()) {
                        Text("描述", color = C.textDim, fontSize = 17.sp)
                    }
                    AppTextField(
                        value = draft.notes, onValueChange = { draft.notes = it },
                        fontSize = 17f, singleLine = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            HLine()

            Row(
                Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 20.dp)
                    .clickable { fullScreen = "map" },
                verticalAlignment = Alignment.CenterVertically
            ) {
                DrawIcon("place", C.textDim, 24.dp)
                Spacer(Modifier.width(22.dp))
                Text(draft.location ?: "添加位置", color = if (draft.location == null) C.textDim else C.text, fontSize = 17.sp)
            }
            HLine()

            Row(
                Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 20.dp)
                    .clickable { sheet = "attach" },
                verticalAlignment = Alignment.CenterVertically
            ) {
                DrawIcon("attach", C.textDim, 24.dp)
                Spacer(Modifier.width(22.dp))
                Text("添加附件", color = C.textDim, fontSize = 17.sp)
            }
            HLine()

            Column(Modifier.fillMaxWidth().clickable { showCalendarPrompt = true }) {
                Row(
                    Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DrawIcon("calendar_off", C.textDim, 24.dp)
                    Spacer(Modifier.width(22.dp))
                    Text("不添加到日历", color = C.textDim, fontSize = 17.sp)
                }
            }
            HLine()

            Row(
                Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DrawIcon("timer", C.textDim, 24.dp)
                Spacer(Modifier.width(22.dp))
                Text("计时器", color = C.textDim, fontSize = 17.sp)
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.size(36.dp).clip(CircleShape).clickable { showTimer = !showTimer },
                    contentAlignment = Alignment.Center
                ) { DrawIcon(if (showTimer) "pause" else "play", C.text, 22.dp) }
                Spacer(Modifier.width(12.dp))
                Text(formatTimer(draft.timerSeconds + if (showTimer) 0 else 0), color = C.text, fontSize = 17.sp)
            }
            HLine()
            Spacer(Modifier.height(60.dp))
        }

        if (fullScreen != null) {
            Box(Modifier.fillMaxSize().background(C.screenBg)) {
                when (fullScreen) {
                    "tags" -> TagPickerScreen(
                        initial = draft.tags.toList(),
                        onDone = { selected ->
                            draft.tags.clear(); draft.tags.addAll(selected); fullScreen = null; refresh()
                        },
                        onBack = { fullScreen = null }
                    )
                    "map" -> MapPickerScreen(
                        onPick = { draft.location = it; fullScreen = null; refresh() },
                        onBack = { fullScreen = null }
                    )
                    else -> {}
                }
            }
        }
    }

    when (sheet) {
        "start" -> DateSheet(
            title = "开始日期", isStart = true, draft = draft,
            onDismiss = { sheet = null }, onChanged = { refresh() }
        )
        "due" -> DateSheet(
            title = "截止日期", isStart = false, draft = draft,
            onDismiss = { sheet = null }, onChanged = { refresh() }
        )
        "repeat" -> RepeatSheet(
            draft = draft, onDismiss = { sheet = null }, onChanged = { refresh() }
        )
        "list" -> ListPickerSheet(
            onDismiss = { sheet = null },
            onPick = { draft.listId = it; sheet = null; refresh() },
            onCreate = { fullScreen = "newlist"; sheet = null }
        )
        "attach" -> AttachSheet(onDismiss = { sheet = null })
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("确认删除?", fontSize = 22.sp) },
            confirmButton = {
                TextButton(onClick = {
                    draft.id?.let { id -> Store.tasks.removeAll { it.id == id } }
                    Store.save(); showDelete = false; onClose()
                }) { Text("确定", fontSize = 17.sp) }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("取消", fontSize = 17.sp) }
            }
        )
    }

    if (showCalendarPrompt) {
        AlertDialog(
            onDismissRequest = { showCalendarPrompt = false },
            title = { Text("要允许“Tasks”访问您的日历吗？", fontSize = 20.sp) },
            confirmButton = {
                TextButton(onClick = { showCalendarPrompt = false; sheet = "calendar_empty" }) {
                    Text("允许", fontSize = 17.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCalendarPrompt = false }) { Text("不允许", fontSize = 17.sp) }
            }
        )
    }

    if (sheet == "calendar_empty") {
        AlertDialog(
            onDismissRequest = { sheet = null },
            title = { Text("选择日历", fontSize = 20.sp) },
            text = { Text("没有可用的日历", fontSize = 16.sp) },
            confirmButton = { TextButton(onClick = { sheet = null }) { Text("确定", fontSize = 17.sp) } }
        )
    }
}

fun formatTimer(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "已耗时 %02d:%02d".format(m, s)
}

@Composable
fun EditorRow(icon: String, value: String, active: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(64.dp).clickable { onClick() }.padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DrawIcon(icon, if (active) C.red else C.textDim, 24.dp)
        Spacer(Modifier.width(22.dp))
        Text(
            value,
            color = if (active) C.text else C.textDim,
            fontSize = 17.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
    HLine()
}

// ------------------------------------------------------------------ date sheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSheet(title: String, isStart: Boolean, draft: Draft, onDismiss: () -> Unit, onChanged: () -> Unit) {
    var month by remember { mutableStateOf(Dates.today().withDayOfMonth(1)) }
    var picked by remember { mutableStateOf<LocalDate?>(draft.due()?.toLocalDate()) }
    var time by remember { mutableStateOf(draft.due()?.let { Dates.timeLabel(it) }) }
    var keyboardMode by remember { mutableStateOf(false) }
    var hourText by remember { mutableStateOf(draft.due()?.hour?.toString() ?: "9") }
    var minuteText by remember { mutableStateOf(draft.due()?.minute?.toString() ?: "0") }

    val presets = if (isStart)
        listOf("截止日期", "截止时间", "截止日期前一天", "截止日期前一周", "无日期")
    else listOf("今天", "明天", "下周三", "无日期")
    val times = listOf("09:00", "13:00", "17:00", "20:00", "挑选时间", "无时间")

    BottomPanel(onDismiss = onDismiss, maxHeight = 720.dp) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 18.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    presets.forEach { p ->
                        Box(
                            Modifier.fillMaxWidth().height(48.dp).clickable {
                                fun setTime(label: String?) {
                                    time = label
                                    if (label != null) {
                                        hourText = label.substring(0, 2).trimStart('0').ifEmpty { "0" }
                                        minuteText = label.substring(3, 5).trimStart('0').ifEmpty { "0" }
                                    }
                                }
                                when (p) {
                                    "今天" -> { picked = Dates.today(); setTime("09:00") }
                                    "明天" -> { picked = Dates.today().plusDays(1); setTime("13:00") }
                                    "下周三" -> { picked = Dates.today().plusWeeks(1); setTime("17:00") }
                                    "无日期" -> { picked = null; time = null }
                                    "截止日期" -> picked = draft.due()?.toLocalDate()
                                    "截止时间" -> picked = draft.due()?.toLocalDate()
                                    "截止日期前一天" -> picked = draft.due()?.toLocalDate()?.minusDays(1)
                                    "截止日期前一周" -> picked = draft.due()?.toLocalDate()?.minusDays(7)
                                }
                            },
                            contentAlignment = Alignment.CenterStart
                        ) { Text(p, color = C.text, fontSize = 16.sp) }
                    }
                }
                Column(Modifier.weight(1f)) {
                    times.forEach { t ->
                        Box(
                            Modifier.fillMaxWidth().height(48.dp).clickable {
                                if (t == "挑选时间") keyboardMode = true
                                else if (t == "无时间") time = null
                                else {
                                    time = t
                                    hourText = t.substring(0, 2).trimStart('0').ifEmpty { "0" }
                                    minuteText = t.substring(3, 5).trimStart('0').ifEmpty { "0" }
                                }
                            },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                t, fontSize = 16.sp,
                                color = if (time == t) C.blueDark else C.text
                            )
                        }
                    }
                }
            }
            HLine()
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    Dates.monthTitle(month), color = C.text, fontSize = 17.sp,
                    fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f)
                )
                Box(
                    Modifier.size(36.dp).clickable { month = month.minusMonths(1) },
                    contentAlignment = Alignment.Center
                ) { DrawIcon("chevron_left", C.text, 22.dp, 2.dp) }
                Box(
                    Modifier.size(36.dp).clickable { month = month.plusMonths(1) },
                    contentAlignment = Alignment.Center
                ) { DrawIcon("chevron_right", C.text, 22.dp, 2.dp) }
            }
            Row(Modifier.fillMaxWidth()) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEach {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(it, color = C.textDim, fontSize = 13.sp)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            CalendarGrid(month = month, selected = picked) { picked = it }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clickable { keyboardMode = !keyboardMode },
                    contentAlignment = Alignment.Center
                ) { DrawIcon("keyboard", C.text, 24.dp) }
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.height(44.dp).clickable { onDismiss() }.padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center
                ) { Text("取消", color = C.text, fontSize = 17.sp) }
                Box(
                    Modifier.height(44.dp).clickable {
                        if (isStart) {
                            when {
                                picked == null -> { draft.startKind = START_NONE; draft.startMillis = null }
                                else -> {
                                    draft.startKind = START_ABS
                                    draft.startMillis = Dates.toMillis(
                                        picked!!.atTime(
                                            hourText.toIntOrNull() ?: 9,
                                            minuteText.toIntOrNull() ?: 0
                                        )
                                    )
                                }
                            }
                        } else {
                            draft.dueMillis = picked?.let { d ->
                                if (time == null) Dates.toMillis(d.atStartOfDay())
                                else Dates.toMillis(
                                    d.atTime(hourText.toIntOrNull() ?: 9, minuteText.toIntOrNull() ?: 0)
                                )
                            }
                        }
                        onChanged(); onDismiss()
                    }.padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center
                ) { Text("确定", color = C.blueDark, fontSize = 17.sp) }
            }
            if (keyboardMode) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    KeyboardField(hourText) { hourText = it }
                    Text(" : ", fontSize = 22.sp, color = C.text)
                    KeyboardField(minuteText) { minuteText = it }
                }
            }
        }
    }
}

@Composable
private fun KeyboardField(value: String, onChange: (String) -> Unit) {
    Box(
        Modifier.size(width = 74.dp, height = 54.dp).clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFEDEDF2)),
        contentAlignment = Alignment.Center
    ) {
        AppTextField(
            value = value, onValueChange = { onChange(it.filter { c -> c.isDigit() }.take(2)) },
            fontSize = 24f, digitsOnly = true,
            modifier = Modifier.width(56.dp)
        )
    }
}

@Composable
fun CalendarGrid(month: LocalDate, selected: LocalDate?, onPick: (LocalDate) -> Unit) {
    val first = month.withDayOfMonth(1)
    val offset = (first.dayOfWeek.value + 6) % 7
    val days = first.lengthOfMonth()
    val cells = offset + days
    val rows = (cells + 6) / 7
    Column(Modifier.fillMaxWidth()) {
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth().height(46.dp)) {
                for (c in 0 until 7) {
                    val index = r * 7 + c
                    val dayNum = index - offset + 1
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (dayNum in 1..days) {
                            val date = first.withDayOfMonth(dayNum)
                            val isSelected = selected == date
                            val isToday = date == Dates.today()
                            Box(
                                Modifier.size(36.dp).clip(CircleShape)
                                    .background(if (isSelected) C.blueDark else Color.Transparent)
                                    .clickable { onPick(date) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    dayNum.toString(),
                                    color = if (isSelected) Color.White else C.text,
                                    fontSize = 15.sp
                                )
                                if (isToday && !isSelected) {
                                    Box(
                                        Modifier.size(36.dp).clip(CircleShape)
                                            .background(Color.Transparent)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ repeat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepeatSheet(draft: Draft, onDismiss: () -> Unit, onChanged: () -> Unit) {
    var custom by remember { mutableStateOf(false) }
    val options = listOf(
        REPEAT_NONE to "不重复",
        REPEAT_DAILY to "每天",
        REPEAT_WEEKLY to "每周",
        REPEAT_MONTHLY to "每月",
        REPEAT_YEARLY to "每年",
    )
    BottomPanel(onDismiss = onDismiss, maxHeight = 560.dp) {
        Column(Modifier.fillMaxWidth().padding(bottom = 30.dp)) {
            if (!custom) {
                options.forEach { (value, label) ->
                    Row(
                        Modifier.fillMaxWidth().height(58.dp).clickable {
                            draft.repeat = value; draft.repeatFromDue = draft.repeatFromDue
                            onChanged(); onDismiss()
                        }.padding(horizontal = 26.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, color = C.text, fontSize = 17.sp, modifier = Modifier.weight(1f))
                        if (draft.repeat == value) DrawIcon("check", C.blueDark, 22.dp, 2.dp)
                    }
                }
                Row(
                    Modifier.fillMaxWidth().height(58.dp).clickable { custom = true }.padding(horizontal = 26.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) { Text("自定义…", color = C.text, fontSize = 17.sp) }
            } else {
                Text(
                    "自定义重复周期", color = C.text, fontSize = 19.sp,
                    modifier = Modifier.padding(start = 26.dp, top = 10.dp, bottom = 10.dp)
                )
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("重复频率", color = C.text, fontSize = 17.sp, modifier = Modifier.weight(1f))
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFEDEDF2)),
                        contentAlignment = Alignment.Center
                    ) { Text("1", color = C.text, fontSize = 17.sp) }
                    Spacer(Modifier.width(12.dp))
                    Box(
                        Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFEDEDF2))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) { Text("周", color = C.text, fontSize = 17.sp) }
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("重复于", color = C.text, fontSize = 17.sp)
                    Spacer(Modifier.weight(1f))
                    listOf("一", "二", "三", "四", "五", "六", "日").forEach { d ->
                        Box(
                            Modifier.size(38.dp).clip(CircleShape)
                                .background(if (d == "三") C.blueDark else Color(0xFFEDEDF2))
                                .clickable { },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(d, color = if (d == "三") Color.White else C.text, fontSize = 15.sp)
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                }
                Text("结束", color = C.textDim, fontSize = 14.sp, modifier = Modifier.padding(start = 26.dp, top = 10.dp))
                listOf("永不", "于 " + Dates.dayLabel(Dates.today().plusMonths(1)), "前有 1 次发生").forEachIndexed { i, label ->
                    Row(
                        Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 26.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(22.dp).clip(CircleShape)
                                .background(if (i == 0) C.blueDark else Color(0xFFBDBDBD)),
                            contentAlignment = Alignment.Center
                        ) { Box(Modifier.size(8.dp).clip(CircleShape).background(Color.White)) }
                        Spacer(Modifier.width(16.dp))
                        Text(label, color = C.text, fontSize = 17.sp)
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        Modifier.clickable { custom = false }.padding(horizontal = 18.dp, vertical = 10.dp)
                    ) { Text("取消", color = C.text, fontSize = 17.sp) }
                    Box(
                        Modifier.clickable {
                            draft.repeat = REPEAT_WEEKLY
                            onChanged(); onDismiss()
                        }.padding(horizontal = 18.dp, vertical = 10.dp)
                    ) { Text("确定", color = C.blueDark, fontSize = 17.sp) }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ sheets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListPickerSheet(onDismiss: () -> Unit, onPick: (String) -> Unit, onCreate: () -> Unit) {
    var query by remember { mutableStateOf("") }
    BottomPanel(onDismiss = onDismiss, maxHeight = 480.dp) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(bottom = 30.dp)) {
            Row(
                Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFEDEDF2)).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DrawIcon("search", C.textDim, 20.dp)
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("搜索", color = C.textFaint, fontSize = 16.sp)
                    AppTextField(
                        value = query, onValueChange = { query = it },
                        fontSize = 16f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("本地清单", color = C.textDim, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Box(Modifier.size(36.dp).clickable { onCreate() }, contentAlignment = Alignment.Center) {
                    DrawIcon("plus", C.text, 22.dp, 2.dp)
                }
            }
            Store.lists.filter { it.name.contains(query, true) }.forEach { list ->
                Row(
                    Modifier.fillMaxWidth().height(52.dp).clickable { onPick(list.id) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DrawIcon(list.icon, Color(list.color), 22.dp, 1.8.dp)
                    Spacer(Modifier.width(18.dp))
                    Text(list.name, color = C.text, fontSize = 17.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachSheet(onDismiss: () -> Unit) {
    BottomPanel(onDismiss = onDismiss, maxHeight = 340.dp) {
        Column(Modifier.fillMaxWidth().padding(bottom = 30.dp)) {
            listOf(
                "photo" to "拍张照片",
                "mic" to "录制一条便笺",
                "folder" to "从相册选一张",
                "attach" to "从存储中挑选",
            ).forEach { (icon, label) ->
                Row(
                    Modifier.fillMaxWidth().height(58.dp).clickable { onDismiss() }.padding(horizontal = 26.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DrawIcon(icon, C.text, 22.dp, 1.8.dp)
                    Spacer(Modifier.width(20.dp))
                    Text(label, color = C.text, fontSize = 17.sp)
                }
            }
        }
    }
}
