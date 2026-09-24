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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EditorRow(
    p: Palette,
    icon: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(46.dp), contentAlignment = Alignment.CenterStart) { icon() }
        Box(Modifier.weight(1f)) { content() }
    }
}

@Composable
fun EditScreenScaffold(
    p: Palette,
    router: Router,
    editing: Task?,
    onSave: () -> Unit,
    onDelete: (() -> Unit)?,
    content: @Composable () -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(p.surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(46.dp).clickable { onSave() }, contentAlignment = Alignment.Center) { SaveIcon(p.textPrimary) }
            Spacer(Modifier.weight(1f))
            if (editing != null && onDelete != null) {
                Box(Modifier.size(46.dp).clickable { confirmDelete = true }, contentAlignment = Alignment.Center) { TrashGlyph(p.textPrimary) }
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            content()
            Spacer(Modifier.height(40.dp))
        }
    }
    if (confirmDelete) {
        ConfirmDialog(p, "确认删除?", onConfirm = { confirmDelete = false; onDelete?.invoke() }, onDismiss = { confirmDelete = false })
    }
}

@Composable
fun TrashGlyph(color: Color) {
    androidx.compose.foundation.Canvas(Modifier.size(24.dp)) {
        val w = size.width; val h = size.height
        val cap = androidx.compose.ui.graphics.StrokeCap.Round
        drawLine(color, androidx.compose.ui.geometry.Offset(w * 0.14f, h * 0.24f), androidx.compose.ui.geometry.Offset(w * 0.86f, h * 0.24f), strokeWidth = w * 0.08f, cap = cap)
        drawLine(color, androidx.compose.ui.geometry.Offset(w * 0.38f, h * 0.24f), androidx.compose.ui.geometry.Offset(w * 0.40f, h * 0.13f), strokeWidth = w * 0.08f, cap = cap)
        drawLine(color, androidx.compose.ui.geometry.Offset(w * 0.62f, h * 0.24f), androidx.compose.ui.geometry.Offset(w * 0.60f, h * 0.13f), strokeWidth = w * 0.08f, cap = cap)
        drawRoundRect(
            color = color, topLeft = androidx.compose.ui.geometry.Offset(w * 0.22f, h * 0.30f),
            size = androidx.compose.ui.geometry.Size(w * 0.56f, h * 0.56f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.06f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.08f)
        )
    }
}

@Composable
fun LinesGlyph(color: Color) {
    androidx.compose.foundation.Canvas(Modifier.size(22.dp)) {
        val w = size.width; val h = size.height
        val cap = androidx.compose.ui.graphics.StrokeCap.Round
        drawLine(color, androidx.compose.ui.geometry.Offset(w * 0.12f, h * 0.3f), androidx.compose.ui.geometry.Offset(w * 0.88f, h * 0.3f), strokeWidth = w * 0.10f, cap = cap)
        drawLine(color, androidx.compose.ui.geometry.Offset(w * 0.12f, h * 0.7f), androidx.compose.ui.geometry.Offset(w * 0.62f, h * 0.7f), strokeWidth = w * 0.10f, cap = cap)
    }
}

@Composable
fun BellGlyph(color: Color) {
    androidx.compose.foundation.Canvas(Modifier.size(22.dp)) {
        val w = size.width; val h = size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.20f, h * 0.72f)
            lineTo(w * 0.26f, h * 0.56f)
            lineTo(w * 0.26f, h * 0.44f)
            cubicTo(w * 0.26f, h * 0.20f, w * 0.74f, h * 0.20f, w * 0.74f, h * 0.44f)
            lineTo(w * 0.74f, h * 0.56f)
            lineTo(w * 0.80f, h * 0.72f)
            close()
        }
        drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.09f))
        drawCircle(color, w * 0.08f, androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.84f))
    }
}

@Composable
fun TaskEditScreen(p: Palette, router: Router, taskId: String?, initialListId: String?, onOpenTagPicker: (String) -> Unit) {
    Store.revision
    val existing = Store.taskById(taskId)
    val task = remember(taskId) {
        existing ?: Task(
            listId = initialListId ?: Store.settings.defaultListId.ifBlank { Store.lists.firstOrNull()?.id ?: "" },
            priority = Store.settings.defaultPriority
        )
    }
    var title by remember { mutableStateOf(task.title) }
    var notes by remember { mutableStateOf(task.notes) }
    var priority by remember { mutableStateOf(task.priority) }
    var startAt by remember { mutableStateOf(task.startAt) }
    var dueAt by remember { mutableStateOf(task.dueAt) }
    var hasDueTime by remember { mutableStateOf(task.hasDueTime) }
    var repeatRule by remember { mutableStateOf(task.repeatRule) }
    var repeatFromDue by remember { mutableStateOf(task.repeatFromDue) }
    var listId by remember { mutableStateOf(task.listId) }
    var reminder by remember { mutableStateOf(task.reminder) }
    var location by remember { mutableStateOf(task.location) }
    var subtaskInput by remember { mutableStateOf(false) }
    var subtaskText by remember { mutableStateOf("") }
    var notesEditing by remember { mutableStateOf(false) }
    var dateForStart by remember { mutableStateOf<Boolean?>(null) }
    var repeatDialog by remember { mutableStateOf(false) }
    var listDialog by remember { mutableStateOf(false) }
    var priorityDialog by remember { mutableStateOf(false) }

    fun commit() {
        if (title.isBlank()) { router.pop(); return }
        val t = existing ?: task
        t.title = title
        t.notes = notes
        t.priority = priority
        t.startAt = startAt
        t.dueAt = dueAt
        t.hasDueTime = hasDueTime
        t.repeatRule = repeatRule
        t.repeatFromDue = repeatFromDue
        t.listId = listId
        t.reminder = reminder
        t.location = location
        if (existing == null) Store.addTask(t) else Store.touch(t)
        router.pop()
    }

    EditScreenScaffold(
        p, router, existing,
        onSave = { commit() },
        onDelete = if (existing != null) {
            { Store.removeTasks(listOf(existing.id)); router.pop() }
        } else null
    ) {
        Divider(p)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleCheck(
                checked = false, color = priorityColor(priority), size = 30.dp,
                dashed = startAt != null && startAt!! > System.currentTimeMillis(),
                onClick = { }
            )
            Spacer(Modifier.width(14.dp))
            Box(Modifier.weight(1f)) {
                if (title.isEmpty()) Text("任务名称", color = p.textSecondary, fontSize = 18.sp)
                BasicTextField(
                    value = title,
                    onValueChange = { if (Store.settings.multiLineTitle || !it.contains("\n")) title = it },
                    textStyle = TextStyle(color = p.textPrimary, fontSize = 18.sp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Divider(p)
        EditorRow(p, { CalendarStartIcon(p.textPrimary, 22.dp) }) {
            Text(
                startAt?.let { startLabel(it) } ?: "无开始日期",
                color = if (startAt != null) p.textPrimary else p.textSecondary,
                fontSize = 17.sp, modifier = Modifier.fillMaxWidth().clickable { dateForStart = true }
            )
        }
        Divider(p)
        EditorRow(p, { ClockIcon(if (dueAt != null) Color(0xFFE8710A) else p.textPrimary, 22.dp) }) {
            Text(
                dueAt?.let { dueLabel(it, hasDueTime, Store.settings.showFullDate) } ?: "无截止日期",
                color = if (dueAt != null) Color(0xFFE8710A) else p.textSecondary,
                fontSize = 17.sp, modifier = Modifier.fillMaxWidth().clickable { dateForStart = false }
            )
        }
        Divider(p)
        EditorRow(p, { RepeatArrowIcon(p.textPrimary, 22.dp) }) {
            Column {
                Text(
                    if (repeatRule == null) "不重复" else "重复 ${repeatName(repeatRule)}",
                    color = p.textPrimary, fontSize = 17.sp,
                    modifier = Modifier.fillMaxWidth().clickable { repeatDialog = true }
                )
                if (repeatRule != null) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("重复始于", color = p.textPrimary, fontSize = 17.sp)
                        Spacer(Modifier.width(10.dp))
                        Surface(
                            color = Color.Transparent, shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, p.textSecondary),
                            modifier = Modifier.clickable { repeatFromDue = !repeatFromDue }
                        ) {
                            Text(
                                if (repeatFromDue) "截止日期" else "完成日期",
                                color = p.textPrimary, fontSize = 16.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }
        Divider(p)
        EditorRow(p, { FlagIcon(p.textPrimary, 22.dp) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("优先级", color = p.textPrimary, fontSize = 17.sp)
                Spacer(Modifier.weight(1f))
                for (pr in listOf(PRIO_NONE, PRIO_LOW, PRIO_MED, PRIO_HIGH)) {
                    Box(
                        modifier = Modifier.size(42.dp).clickable { priority = pr },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier.size(if (priority == pr) 26.dp else 22.dp).clip(CircleShape)
                                .background(if (priority == pr) priorityColor(pr) else Color.Transparent)
                                .then(
                                    if (priority == pr) Modifier
                                    else Modifier.background(Color.Transparent)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.Canvas(Modifier.size(24.dp)) {
                                drawCircle(
                                    color = priorityColor(pr),
                                    radius = size.width * 0.42f,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = size.width * 0.11f)
                                )
                            }
                        }
                    }
                }
            }
        }
        Divider(p)
        EditorRow(p, { ListGlyphSmall(p.textPrimary) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val lc = chipColor(p, Store.listById(listId)?.color ?: 0xFF3F51B5)
                Surface(color = lc, shape = RoundedCornerShape(6.dp), modifier = Modifier.clickable { listDialog = true }) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        ListGlyphSmall(Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text(Store.listName(listId), color = Color.White, fontSize = 15.sp)
                    }
                }
            }
        }
        Divider(p)
        EditorRow(p, { TagIcon(p.textPrimary, 22.dp) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (task.tagIds.isEmpty()) {
                    Text(
                        "添加标签", color = p.textSecondary, fontSize = 17.sp,
                        modifier = Modifier.fillMaxWidth().clickable { onOpenTagPicker(task.id) }
                    )
                } else {
                    task.tagIds.forEach { id ->
                        Store.tagById(id)?.let { tag ->
                            val tc = chipColor(p, tag.color)
                            Surface(color = p.chipBg, shape = RoundedCornerShape(6.dp), modifier = Modifier.padding(end = 6.dp)) {
                                Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    TagIcon(tc, 15.dp)
                                    Spacer(Modifier.width(5.dp))
                                    Text(tag.name, color = p.textPrimary, fontSize = 15.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Box(Modifier.size(18.dp).clickable { task.tagIds.remove(id); Store.touch(task) }, contentAlignment = Alignment.Center) {
                                        CloseGlyph(p.textSecondary, 14.dp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Divider(p)
        EditorRow(p, { SubtaskIcon(p.textPrimary, 22.dp) }) {
            Column(Modifier.fillMaxWidth()) {
                task.subtasks.forEach { s ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircleCheck(s.done, priorityColor(task.priority), 26.dp, onClick = {
                            if (s.done) Store.uncompleteSubtask(task, s) else Store.completeSubtask(task, s)
                        })
                        Spacer(Modifier.width(10.dp))
                        Text(
                            s.title, color = if (s.done) p.textSecondary else p.textPrimary, fontSize = 17.sp,
                            textDecoration = if (s.done) TextDecoration.LineThrough else null,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (subtaskInput) {
                    BasicTextField(
                        value = subtaskText,
                        onValueChange = { subtaskText = it },
                        textStyle = TextStyle(color = p.textPrimary, fontSize = 17.sp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                    Row {
                        Text(
                            "完成", color = p.primary, fontSize = 15.sp,
                            modifier = Modifier.clickable {
                                if (subtaskText.isNotBlank()) {
                                    task.subtasks.add(SubTask(title = subtaskText))
                                    Store.touch(task)
                                }
                                subtaskText = ""; subtaskInput = false
                            }.padding(8.dp)
                        )
                    }
                } else {
                    Text(
                        "添加子任务", color = p.textSecondary, fontSize = 17.sp,
                        modifier = Modifier.clickable { subtaskInput = true }.padding(vertical = 8.dp)
                    )
                }
            }
        }
        Divider(p)
        EditorRow(p, { BellGlyph(if (reminder) p.textPrimary else Color(0xFFC0392B)) }) {
            Column(Modifier.fillMaxWidth().clickable { reminder = !reminder }) {
                if (reminder) {
                    Text("提醒", color = p.textPrimary, fontSize = 17.sp)
                    Text("截止时间提醒", color = p.textSecondary, fontSize = 14.sp)
                } else {
                    Text("启用提醒", color = Color(0xFFC0392B), fontSize = 17.sp)
                    Text("提醒在 Android 设置中被禁用", color = Color(0xFFC0392B), fontSize = 13.sp)
                }
            }
        }
        Divider(p)
        EditorRow(p, { LinesGlyph(p.textPrimary) }) {
            if (notesEditing) {
                BasicTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    textStyle = TextStyle(color = p.textPrimary, fontSize = 17.sp),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    notes.ifBlank { "描述" },
                    color = if (notes.isBlank()) p.textSecondary else p.textPrimary,
                    fontSize = 17.sp,
                    modifier = Modifier.fillMaxWidth().clickable { notesEditing = true }
                )
            }
        }
        Divider(p)
        EditorRow(p, { PlaceGlyph(p.textPrimary, 22.dp) }) {
            Text(
                location ?: "添加位置",
                color = if (location == null) p.textSecondary else p.textPrimary,
                fontSize = 17.sp,
                modifier = Modifier.fillMaxWidth().clickable { location = "未选择位置" }
            )
        }
        Divider(p)
        EditorRow(p, { AttachmentIcon(p.textPrimary, 22.dp) }) {
            Text(
                if (task.attachments == 0) "添加附件" else "${task.attachments} 个附件",
                color = if (task.attachments == 0) p.textSecondary else p.textPrimary,
                fontSize = 17.sp,
                modifier = Modifier.fillMaxWidth().clickable { task.attachments += 1; Store.touch(task) }
            )
        }
        Divider(p)
    }

    dateForStart?.let { isStart ->
        DatePickerSheet(
            p, forStart = isStart, initial = if (isStart) startAt else dueAt,
            onDismiss = { dateForStart = null },
            onPick = { ms, hasTime ->
                if (isStart) startAt = ms else { dueAt = ms; hasDueTime = hasTime }
                dateForStart = null
            }
        )
    }
    if (repeatDialog) {
        OptionDialog(
            p, "重复",
            listOf("不重复", "每天", "每周", "每月", "每年", "自定义…"),
            onPick = { v ->
                repeatRule = when (v) {
                    "每天" -> "daily"
                    "每周" -> "weekly"
                    "每月" -> "monthly"
                    "每年" -> "yearly"
                    "自定义…" -> "daily"
                    else -> null
                }
                repeatDialog = false
            },
            onDismiss = { repeatDialog = false }
        )
    }
    if (listDialog) {
        OptionDialog(p, "清单", Store.lists.map { it.name }, onPick = { name ->
            listId = Store.lists.firstOrNull { it.name == name }?.id ?: listId
            listDialog = false
        }, onDismiss = { listDialog = false })
    }
    if (priorityDialog) {
        OptionDialog(p, "优先级", listOf("无", "低", "中", "高"), onPick = { v ->
            priority = when (v) { "高" -> PRIO_HIGH; "中" -> PRIO_MED; "低" -> PRIO_LOW; else -> PRIO_NONE }
            priorityDialog = false
        }, onDismiss = { priorityDialog = false })
    }
}

@Composable
fun CloseGlyph(color: Color, size: androidx.compose.ui.unit.Dp) {
    androidx.compose.foundation.Canvas(Modifier.size(size)) {
        val w = this.size.width; val h = this.size.height
        val cap = androidx.compose.ui.graphics.StrokeCap.Round
        drawLine(color, androidx.compose.ui.geometry.Offset(w * 0.22f, h * 0.22f), androidx.compose.ui.geometry.Offset(w * 0.78f, h * 0.78f), strokeWidth = w * 0.14f, cap = cap)
        drawLine(color, androidx.compose.ui.geometry.Offset(w * 0.78f, h * 0.22f), androidx.compose.ui.geometry.Offset(w * 0.22f, h * 0.78f), strokeWidth = w * 0.14f, cap = cap)
    }
}

// ------------------------------------------------------------------ tag picker

@Composable
fun TagPickerScreen(p: Palette, router: Router, taskId: String) {
    Store.revision
    var query by remember { mutableStateOf("") }
    val task = Store.taskById(taskId)
    Column(Modifier.fillMaxSize().background(p.surface)) {
        TopBar(p, "标签", onBack = { router.pop() })
        Divider(p)
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) Text("输入标签名称", color = p.textSecondary, fontSize = 17.sp)
                BasicTextField(
                    value = query, onValueChange = { query = it },
                    textStyle = TextStyle(color = p.textPrimary, fontSize = 17.sp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotEmpty()) {
                Text("新建标签 \"$query\"", color = p.primary, fontSize = 16.sp, modifier = Modifier.clickable {
                    val t = Store.addTag(query, COLOR_PALETTE.random())
                    task?.tagIds?.add(t.id)
                    task?.let { Store.touch(it) }
                    query = ""
                })
            }
        }
        Divider(p)
        for (tag in Store.tags) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable {
                    if (task != null) {
                        if (task.tagIds.contains(tag.id)) task.tagIds.remove(tag.id) else task.tagIds.add(tag.id)
                        Store.touch(task)
                    }
                }.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                    if (task?.tagIds?.contains(tag.id) == true) CheckGlyph(p.primary, 20.dp)
                }
                Spacer(Modifier.width(14.dp))
                TagIcon(chipColor(p, tag.color), 20.dp)
                Spacer(Modifier.width(12.dp))
                Text(tag.name, color = p.textPrimary, fontSize = 17.sp)
            }
            Divider(p)
        }
    }
}

// ------------------------------------------------------------------ list / tag / filter editors

@Composable
fun ListEditScreen(p: Palette, router: Router, listId: String?) {
    val existing = Store.listById(listId)
    var name by remember { mutableStateOf(existing?.name ?: "新建清单") }
    var color by remember { mutableStateOf(existing?.color ?: 0xFF3F51B5) }
    var icon by remember { mutableStateOf(existing?.icon ?: "shopping") }
    var colorDialog by remember { mutableStateOf(false) }
    var iconDialog by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var dirty by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(if (p.dark) p.background else Color(0xFFEDEDF2))) {
        TopBar(p, if (existing == null) "新建清单" else "清单设置", onBack = { router.pop() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(8.dp))
            SettingsCard(p) {
                Column(Modifier.padding(16.dp)) {
                    Text("显示名称", color = p.textSecondary, fontSize = 14.sp)
                    BasicTextField(
                        value = name, onValueChange = { name = it; dirty = true },
                        textStyle = TextStyle(color = p.textPrimary, fontSize = 18.sp),
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            SettingsCard(p) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { colorDialog = true }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(28.dp).clip(CircleShape).background(Color(color)))
                    Spacer(Modifier.width(16.dp))
                    Text("颜色", color = p.textPrimary, fontSize = 17.sp, modifier = Modifier.weight(1f))
                    CloseGlyph(p.textSecondary, 18.dp)
                }
                Divider(p)
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { iconDialog = true }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconGlyph(icon, p.textPrimary)
                    Spacer(Modifier.width(16.dp))
                    Text("图标", color = p.textPrimary, fontSize = 17.sp, modifier = Modifier.weight(1f))
                    ChevronRight(p.textSecondary, 18.dp)
                }
            }
            Spacer(Modifier.height(10.dp))
            SettingsCard(p) {
                Row(Modifier.fillMaxWidth().clickable { }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    HomeGlyph(p.textPrimary, 22.dp)
                    Spacer(Modifier.width(16.dp))
                    Text("添加快捷方式到主屏幕", color = p.textPrimary, fontSize = 17.sp)
                }
                Divider(p)
                Row(Modifier.fillMaxWidth().clickable { }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    GridGlyph(p.textPrimary, 22.dp)
                    Spacer(Modifier.width(16.dp))
                    Text("添加小部件到主屏幕", color = p.textPrimary, fontSize = 17.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Surface(
                color = if (dirty || existing == null) p.surface else (if (p.dark) Color(0xFF23242A) else Color(0xFFE4E4E9)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp).clickable(enabled = dirty || existing == null) {
                    if (existing == null) Store.addList(name, color, icon)
                    else { existing.name = name; existing.color = color; existing.icon = icon; Store.save() }
                    router.pop()
                }
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    SaveIcon(if (dirty || existing == null) p.textPrimary else p.textSecondary)
                    Spacer(Modifier.width(16.dp))
                    Text("保存", color = if (dirty || existing == null) p.textPrimary else p.textSecondary, fontSize = 17.sp)
                }
            }
            if (existing != null) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    color = if (p.dark) Color(0xFF3A2224) else Color(0xFFF3D9DC),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp).clickable { confirmDelete = true }
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        TrashGlyph(Color(0xFFC0392B))
                        Spacer(Modifier.width(16.dp))
                        Text("删除", color = Color(0xFFC0392B), fontSize = 17.sp)
                    }
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }
    if (colorDialog) ColorDialog(p, color, { color = it; dirty = true; colorDialog = false }, { colorDialog = false })
    if (iconDialog) IconDialog(p, icon, { icon = it; dirty = true; iconDialog = false }, { iconDialog = false })
    if (confirmDelete) {
        ConfirmDialog(p, "确认删除?", onConfirm = {
            confirmDelete = false
            existing?.let { l -> Store.lists.remove(l); Store.save() }
            router.pop()
        }, onDismiss = { confirmDelete = false })
    }
}

@Composable
fun TagEditScreen(p: Palette, router: Router, tagId: String?) {
    val existing = Store.tagById(tagId)
    var name by remember { mutableStateOf(existing?.name ?: "新建标签") }
    var color by remember { mutableStateOf(existing?.color ?: 0xFF5E35B1) }
    var icon by remember { mutableStateOf(existing?.icon ?: "tag") }
    var colorDialog by remember { mutableStateOf(false) }
    var iconDialog by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(if (p.dark) p.background else Color(0xFFEDEDF2))) {
        TopBar(p, if (existing == null) "新建标签" else "标签设置", onBack = { router.pop() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(8.dp))
            SettingsCard(p) {
                Column(Modifier.padding(16.dp)) {
                    Text("显示名称", color = p.textSecondary, fontSize = 14.sp)
                    BasicTextField(
                        value = name, onValueChange = { name = it },
                        textStyle = TextStyle(color = p.textPrimary, fontSize = 18.sp),
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            SettingsCard(p) {
                Row(Modifier.fillMaxWidth().clickable { colorDialog = true }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(28.dp).clip(CircleShape).background(Color(color)))
                    Spacer(Modifier.width(16.dp))
                    Text("颜色", color = p.textPrimary, fontSize = 17.sp, modifier = Modifier.weight(1f))
                    CloseGlyph(p.textSecondary, 18.dp)
                }
                Divider(p)
                Row(Modifier.fillMaxWidth().clickable { iconDialog = true }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    TagIcon(p.textPrimary, 22.dp)
                    Spacer(Modifier.width(16.dp))
                    Text("图标", color = p.textPrimary, fontSize = 17.sp, modifier = Modifier.weight(1f))
                    ChevronRight(p.textSecondary, 18.dp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Surface(
                color = p.surface, shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp).clickable {
                    if (existing == null) Store.addTag(name, color, icon)
                    else { existing.name = name; existing.color = color; existing.icon = icon; Store.save() }
                    router.pop()
                }
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    SaveIcon(p.textPrimary)
                    Spacer(Modifier.width(16.dp))
                    Text("保存", color = p.textPrimary, fontSize = 17.sp)
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }
    if (colorDialog) ColorDialog(p, color, { color = it; colorDialog = false }, { colorDialog = false })
    if (iconDialog) IconDialog(p, icon, { icon = it; iconDialog = false }, { iconDialog = false })
}

// ------------------------------------------------------------------ filter creation

@Composable
fun FilterPresetsScreen(p: Palette, router: Router) {
    val presets = listOf(
        "自定义…", "已过期", "仅今日", "明天", "今日以后",
        "任意开始日期", "无开始日期", "任何截止日期", "无截止日期",
        "无标签", "高优先级", "中等优先级", "低优先级"
    )
    Column(Modifier.fillMaxSize().background(if (p.dark) p.background else Color(0xFFEDEDF2))) {
        TopBar(p, "过滤器", onBack = { router.pop() })
        SettingsCard(p) {
            presets.forEach { o ->
                Text(
                    o, color = p.textPrimary, fontSize = 17.sp,
                    modifier = Modifier.fillMaxWidth().clickable { router.push(Nav.FilterEdit(if (o == "自定义…") null else o)) }
                        .padding(horizontal = 18.dp, vertical = 15.dp)
                )
                Divider(p)
            }
        }
    }
}

@Composable
fun FilterEditScreen(p: Palette, router: Router, preset: String?) {
    Store.revision
    val filter = remember {
        val f = TaskFilter()
        f.conditions.add(FilterCondition("all", ""))
        when (preset) {
            "已过期" -> f.conditions.add(FilterCondition("overdue", ""))
            "仅今日" -> f.conditions.add(FilterCondition("today", ""))
            "明天" -> f.conditions.add(FilterCondition("tomorrow", ""))
            "今日以后" -> f.conditions.add(FilterCondition("future", ""))
            "任意开始日期" -> f.conditions.add(FilterCondition("start", ""))
            "无开始日期" -> f.conditions.add(FilterCondition("start", ""))
            "任何截止日期" -> f.conditions.add(FilterCondition("due", ""))
            "无截止日期" -> f.conditions.add(FilterCondition("noDue", ""))
            "无标签" -> f.conditions.add(FilterCondition("noTag", ""))
            "高优先级" -> f.conditions.add(FilterCondition("priority", "high"))
            "中等优先级" -> f.conditions.add(FilterCondition("priority", "med"))
            "低优先级" -> f.conditions.add(FilterCondition("priority", "low"))
        }
        f
    }
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(0xFF3F51B5) }
    var icon by remember { mutableStateOf("filter") }
    var addCondition by remember { mutableStateOf(false) }
    var colorDialog by remember { mutableStateOf(false) }
    var iconDialog by remember { mutableStateOf(false) }
    var pendingCondition by remember { mutableStateOf<String?>(null) }
    val conditionTypes = listOf(
        "标签…", "标签名包含…", "开始于…", "截止于…", "优先级…", "标题含…", "在某清单中…",
        "重复", "已完成", "尚未开始", "有子任务", "是子任务", "有提醒"
    )

    Column(Modifier.fillMaxSize().background(p.surface)) {
        Row(Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).clickable {
                if (name.isNotBlank()) { filter.name = name; Store.addFilter(filter) }
                router.pop()
            }, contentAlignment = Alignment.Center) { FilterIcon(p.textPrimary) }
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) { HelpGlyph(p.textSecondary) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Divider(p)
            Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                Text("显示名称", color = p.textSecondary, fontSize = 14.sp)
                BasicTextField(
                    value = name, onValueChange = { name = it },
                    textStyle = TextStyle(color = p.textPrimary, fontSize = 18.sp),
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                )
            }
            Divider(p)
            EditorRow(p, { NoEntryGlyph(p.textSecondary, 22.dp) }) {
                Text("颜色", color = p.textPrimary, fontSize = 17.sp, modifier = Modifier.fillMaxWidth().clickable { colorDialog = true })
            }
            Divider(p)
            EditorRow(p, { FilterIcon(p.textPrimary, 22.dp) }) {
                Text("图标", color = p.textPrimary, fontSize = 17.sp, modifier = Modifier.fillMaxWidth().clickable { iconDialog = true })
            }
            Divider(p)
            Spacer(Modifier.height(14.dp))
            Text("过滤条件", color = p.textSecondary, fontSize = 15.sp, modifier = Modifier.padding(start = 20.dp, bottom = 6.dp))
            filter.conditions.forEach { c ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("+", color = p.textPrimary, fontSize = 20.sp, modifier = Modifier.width(28.dp))
                    Text(conditionLabel(c), color = p.textPrimary, fontSize = 17.sp, modifier = Modifier.weight(1f))
                    Text(
                        "${Store.tasks.count { t -> conditionMatches(c, t) }}",
                        color = p.textSecondary, fontSize = 15.sp
                    )
                }
            }
            Spacer(Modifier.height(90.dp))
        }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
        Surface(
            color = Color(0xFF44506B), shape = RoundedCornerShape(24.dp),
            modifier = Modifier.padding(end = 18.dp, bottom = 34.dp).clickable { addCondition = true }
        ) {
            Row(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                PlusGlyph(Color.White)
                Spacer(Modifier.width(8.dp))
                Text("添加条件", color = Color.White, fontSize = 16.sp)
            }
        }
    }
    if (addCondition) {
        OptionDialog(p, "优先级…", conditionTypes, onPick = { t ->
            addCondition = false
            when (t) {
                "优先级…" -> pendingCondition = "priority"
                "在某清单中…" -> pendingCondition = "list"
                "标签…" -> pendingCondition = "tag"
                "标题含…" -> pendingCondition = "title"
                "重复" -> filter.conditions.add(FilterCondition("repeating", ""))
                "已完成" -> filter.conditions.add(FilterCondition("completed", ""))
                "尚未开始" -> filter.conditions.add(FilterCondition("notStarted", ""))
                "有子任务" -> filter.conditions.add(FilterCondition("hasSubtasks", ""))
                "是子任务" -> filter.conditions.add(FilterCondition("isSubtask", ""))
                "有提醒" -> filter.conditions.add(FilterCondition("hasReminder", ""))
                else -> filter.conditions.add(FilterCondition("due", ""))
            }
        }, onDismiss = { addCondition = false })
    }
    pendingCondition?.let { kind ->
        val options = when (kind) {
            "priority" -> listOf("!!!", "!!", "!", "o")
            "list" -> Store.lists.map { it.name }
            "tag" -> Store.tags.map { it.name }
            else -> listOf("")
        }
        if (kind == "title") {
            Dialog2(p, onDismiss = { pendingCondition = null }) {
                TitleInputDialog(p,
                    onDone = { v -> filter.conditions.add(FilterCondition("title", v)); pendingCondition = null },
                    onCancel = { pendingCondition = null })
            }
        } else {
            OptionDialog(p, if (kind == "priority") "优先级…" else null, options, onPick = { v ->
                when (kind) {
                    "priority" -> filter.conditions.add(
                        FilterCondition(
                            "priority",
                            when (v) { "!!!" -> "high"; "!!" -> "med"; "!" -> "low"; else -> "none" }
                        )
                    )
                    "list" -> filter.conditions.add(FilterCondition("list", Store.lists.firstOrNull { it.name == v }?.id ?: ""))
                    "tag" -> filter.conditions.add(FilterCondition("tag", Store.tags.firstOrNull { it.name == v }?.id ?: ""))
                }
                pendingCondition = null
            }, onDismiss = { pendingCondition = null })
        }
    }
    if (colorDialog) ColorDialog(p, color, { color = it; filter.color = it; colorDialog = false }, { colorDialog = false })
    if (iconDialog) IconDialog(p, icon, { icon = it; filter.icon = it; iconDialog = false }, { iconDialog = false })
}

@Composable
fun Dialog2(p: Palette, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) { content() }
}

@Composable
fun TitleInputDialog(p: Palette, onDone: (String) -> Unit, onCancel: () -> Unit) {
    var v by remember { mutableStateOf("") }
    Surface(color = p.surface, shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text("标题含…", color = p.textPrimary, fontSize = 18.sp)
            BasicTextField(
                value = v, onValueChange = { v = it },
                textStyle = TextStyle(color = p.textPrimary, fontSize = 17.sp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text("取消", color = p.primary, fontSize = 16.sp, modifier = Modifier.clickable { onCancel() }.padding(8.dp))
                Text("确定", color = p.primary, fontSize = 16.sp, modifier = Modifier.clickable { onDone(v) }.padding(8.dp))
            }
        }
    }
}

@Composable
fun HelpGlyph(color: Color) {
    androidx.compose.foundation.Canvas(Modifier.size(26.dp)) {
        val w = size.width; val h = size.height
        drawCircle(color, w * 0.42f, androidx.compose.ui.geometry.Offset(w / 2, h / 2), style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.08f))
        drawCircle(color, w * 0.055f, androidx.compose.ui.geometry.Offset(w / 2, h * 0.72f))
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.36f, h * 0.36f)
            cubicTo(w * 0.40f, h * 0.22f, w * 0.64f, h * 0.22f, w * 0.64f, h * 0.38f)
            cubicTo(w * 0.64f, h * 0.50f, w * 0.50f, h * 0.48f, w * 0.50f, h * 0.60f)
        }
        drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.08f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    }
}

@Composable
fun SettingsCard(p: Palette, content: @Composable () -> Unit) {
    Surface(
        color = p.surface, shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)
    ) { Column(Modifier.fillMaxWidth()) { content() } }
}
