package com.blackboxbench.reproduction

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun TaskEditScreen(
    taskId: String,
    isNew: Boolean,
    onBack: () -> Unit,
    onPickTags: () -> Unit,
    onPickLocation: () -> Unit
) {
    val task = Store.tasks.firstOrNull { it.id == taskId }
    if (task == null) { onBack(); return }
    Store.version.value

    var dirty by remember { mutableStateOf(false) }
    var showDiscard by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var dateTarget by remember { mutableStateOf("") }   // start / due
    var showRepeat by remember { mutableStateOf(false) }
    var showListPicker by remember { mutableStateOf(false) }
    var addingSubtask by remember { mutableStateOf(false) }
    var subtaskText by remember { mutableStateOf("") }
    var timerRunning by remember { mutableStateOf(false) }

    fun edit(block: () -> Unit) { block(); dirty = true; Store.save() }

    LaunchedEffect(timerRunning) {
        while (timerRunning) {
            delay(1000)
            edit { task.elapsedSeconds++ }
        }
    }

    fun tryBack() {
        if (dirty && !Store.boolSetting("save_on_back", false)) {
            showDiscard = true
        } else {
            if (isNew && !dirty) Store.delete(task)
            Store.save(); onBack()
        }
    }
    BackHandler { tryBack() }

    Column(Modifier.fillMaxSize().statusBarsPadding().background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { Store.touch(task); onBack() }) { SaveIcon(Color(0xFF333333)) }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { showDeleteConfirm = true }) { Icon(Icons.Filled.Delete, null, tint = AppColors.Subtle) }
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                TaskCheckbox(task, onToggle = { edit { Store.complete(task, !task.isCompleted) } })
                Spacer(Modifier.width(12.dp))
                TextField(
                    value = task.title,
                    onValueChange = { v -> edit { task.title = v } },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("任务名称", color = AppColors.Subtle) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 17.sp,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                    )
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 4.dp))

            EditRow(icon = { Icon(Icons.Filled.DateRange, null, tint = AppColors.Subtle) }, onClick = { dateTarget = "start" }) {
                Text(
                    if (task.startDate.isEmpty()) "无开始日期" else task.startDate,
                    color = if (task.startDate.isEmpty()) AppColors.Subtle else Color.Unspecified
                )
            }
            HorizontalDivider()
            EditRow(icon = { HistoryIcon(AppColors.Subtle) }, onClick = { dateTarget = "due" }) {
                Text(
                    buildString {
                        if (task.dueDate.isEmpty()) append("无截止日期")
                        else {
                            val d = Dates.parseDate(task.dueDate)!!
                            append(
                                when {
                                    d.isEqual(Dates.today()) -> "今天"
                                    d.isEqual(Dates.today().plusDays(1)) -> "明天"
                                    else -> task.dueDate
                                }
                            )
                            if (task.dueTime.isNotEmpty()) append(" ${task.dueTime}")
                        }
                    },
                    color = if (task.dueDate.isEmpty()) AppColors.Subtle else Color.Unspecified
                )
            }
            HorizontalDivider()
            EditRow(icon = { Icon(Icons.Filled.Refresh, null, tint = if (task.isRepeating) AppColors.Red else AppColors.Subtle) }, onClick = { showRepeat = true }) {
                Column {
                    Text(
                        if (task.isRepeating) "重复 ${Dates.repeatLabel(task.repeat)}" else "不重复",
                        color = if (task.isRepeating) Color.Unspecified else AppColors.Subtle
                    )
                    if (task.isRepeating) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("重复始于", color = AppColors.Subtle, fontSize = 14.sp)
                            Spacer(Modifier.width(8.dp))
                            Surface(shape = RoundedCornerShape(6.dp), border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Subtle)) {
                                Text("截止日期", Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
            HorizontalDivider()
            EditRow(icon = { FlagIcon(AppColors.Subtle) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("优先级")
                    Spacer(Modifier.width(24.dp))
                    PriorityDots(task.priority, { p -> edit { task.priority = p } })
                }
            }
            HorizontalDivider()
            EditRow(icon = { Icon(Icons.Filled.List, null, tint = AppColors.Subtle) }, onClick = { showListPicker = true }) {
                InfoChip(Color(0xFFE8E8E8)) {
                    Icon(Icons.Filled.List, null, Modifier.size(14.dp), tint = AppColors.Subtle)
                    Text(Store.listName(task.listId), fontSize = 13.sp, color = AppColors.Subtle)
                }
            }
            HorizontalDivider()
            EditRow(icon = { TagIcon(AppColors.Subtle) }, onClick = onPickTags) {
                if (task.tagIds.isEmpty()) {
                    Text("添加标签", color = AppColors.Subtle)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        task.tagIds.forEach { tid ->
                            Store.tagById(tid)?.let { tag ->
                                val c = if (tag.color != -1) Color(tag.color) else AppColors.Blue
                                InfoChip(c) {
                                    TagIcon(Color.White, Modifier.size(12.dp))
                                    Text(tag.name, fontSize = 13.sp, color = Color.White)
                                    Icon(
                                        Icons.Filled.Close, null, Modifier.size(14.dp).clickable {
                                            edit { task.tagIds.remove(tid) }
                                        }, tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
            HorizontalDivider()
            EditRow(icon = { SubtaskIcon(AppColors.Subtle) }) {
                Column {
                    Store.subtasksOf(task.id).forEach { st ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TaskCheckbox(st, onToggle = { edit { Store.complete(st, !st.isCompleted) } })
                            Spacer(Modifier.width(10.dp))
                            Text(
                                st.title, Modifier.weight(1f),
                                textDecoration = if (st.isCompleted) TextDecoration.LineThrough else null,
                                color = if (st.isCompleted) AppColors.Subtle else Color.Unspecified
                            )
                            Icon(Icons.Filled.Close, null, Modifier.size(18.dp).clickable { edit { Store.delete(st) } }, tint = AppColors.Subtle)
                        }
                    }
                    if (addingSubtask) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(26.dp).border(2.dp, AppColors.Blue, RoundedCornerShape(4.dp)))
                            Spacer(Modifier.width(10.dp))
                            TextField(
                                value = subtaskText, onValueChange = { subtaskText = it },
                                modifier = Modifier.weight(1f), singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = {
                                    if (subtaskText.isNotBlank()) {
                                        edit {
                                            Store.addTask(Task(listId = task.listId, title = subtaskText, parentId = task.id, priority = Priority.LOW))
                                        }
                                        subtaskText = ""
                                    }
                                })
                            )
                            Icon(Icons.Filled.Close, null, Modifier.clickable { addingSubtask = false; subtaskText = "" }, tint = AppColors.Subtle)
                        }
                    }
                    Text(
                        "添加子任务", color = AppColors.Subtle,
                        modifier = Modifier.clickable { addingSubtask = true }.padding(vertical = 8.dp)
                    )
                }
            }
            HorizontalDivider()
            EditRow(icon = { AlarmIcon(AppColors.Subtle) }) {
                Column {
                    Text("启用提醒", color = AppColors.Red, fontSize = 16.sp)
                    Text("提醒在 Android 设置中被禁用", color = AppColors.Red, fontSize = 13.sp)
                }
            }
            HorizontalDivider()
            EditRow(icon = { Icon(Icons.Filled.List, null, tint = AppColors.Subtle) }) {
                TextField(
                    value = task.notes,
                    onValueChange = { v -> edit { task.notes = v } },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("描述", color = AppColors.Subtle) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }
            HorizontalDivider()
            EditRow(icon = { Icon(Icons.Filled.Place, null, tint = AppColors.Subtle) }, onClick = onPickLocation) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (task.locationId.isEmpty()) {
                        Text("添加位置", color = AppColors.Subtle)
                    } else {
                        Text(Store.placeById(task.locationId)?.name ?: "")
                        Spacer(Modifier.weight(1f))
                        BellOffIcon(AppColors.Subtle)
                    }
                }
            }
            HorizontalDivider()
            EditRow(icon = { ClipIcon(AppColors.Subtle) }) {
                Text("添加附件", color = AppColors.Subtle)
            }
            HorizontalDivider()
            EditRow(icon = { Icon(Icons.Filled.DateRange, null, tint = AppColors.Subtle) }, onClick = { edit { task.addToCalendar = !task.addToCalendar } }) {
                Text(if (task.addToCalendar) "添加到日历" else "不添加到日历", color = AppColors.Subtle)
            }
            HorizontalDivider()
            EditRow(icon = { TimerIcon(AppColors.Subtle) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (task.elapsedSeconds > 0 || timerRunning) "已耗时 %02d:%02d".format((task.elapsedSeconds) / 60, task.elapsedSeconds % 60)
                        else "计时器",
                        color = if (task.elapsedSeconds > 0 || timerRunning) Color.Unspecified else AppColors.Subtle
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = {
                        timerRunning = !timerRunning
                        if (!timerRunning) Store.save()
                    }) {
                        if (timerRunning) PauseIcon(AppColors.Subtle) else Icon(Icons.Filled.PlayArrow, null, tint = AppColors.Subtle)
                    }
                }
            }
            HorizontalDivider()
            EditRow(icon = { Icon(Icons.Filled.Info, null, tint = AppColors.Subtle) }) {
                Column {
                    Text("创建时间 ${task.createdAt.replace('T', ' ')}")
                    if (task.modifiedAt.isNotEmpty()) Text("修改时间 ${task.modifiedAt.replace('T', ' ')}")
                }
            }
            Column(
                Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(20.dp))
                    .background(AppColors.BannerBg).padding(20.dp)
            ) {
                Text("信息太多？", fontSize = 20.sp, color = Color(0xFF333333))
                Spacer(Modifier.height(10.dp))
                Text("你可以重新安排或删除字段来定制此屏幕", color = Color(0xFF555555))
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    when (dateTarget) {
        "start" -> DateTimeSheet(task.startDate, "", onDismiss = { dateTarget = "" }, onConfirm = { d, _ ->
            edit { task.startDate = d }; dateTarget = ""
        })
        "due" -> DateTimeSheet(task.dueDate, task.dueTime, onDismiss = { dateTarget = "" }, onConfirm = { d, tm ->
            edit { task.dueDate = d; task.dueTime = tm }; dateTarget = ""
        })
    }
    if (showRepeat) RepeatDialog(task.repeat, { showRepeat = false }, { r -> edit { task.repeat = r }; showRepeat = false })
    if (showListPicker) ListPickerDialog({ showListPicker = false }, { id -> edit { task.listId = id }; showListPicker = false })
    if (showDiscard) ConfirmDialog("是否放弃修改？", { showDiscard = false }, {
        if (isNew) Store.delete(task)
        Store.save(); onBack()
    })
    if (showDeleteConfirm) ConfirmDialog("确认删除？", { showDeleteConfirm = false }, {
        Store.delete(task); onBack()
    })
}

@Composable
private fun EditRow(
    icon: @Composable () -> Unit,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Row(
        Modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(36.dp), contentAlignment = Alignment.Center) { icon() }
        Column(Modifier.weight(1f), content = content)
    }
}
