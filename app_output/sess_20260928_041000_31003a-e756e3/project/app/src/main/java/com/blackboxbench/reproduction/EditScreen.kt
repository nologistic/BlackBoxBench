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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalDateTime

@Composable
fun EditScreen(initial: Task, isNew: Boolean, onDone: () -> Unit) {
    var task by remember { mutableStateOf(initial) }
    var dateTarget by remember { mutableStateOf<String?>(null) }
    var showRepeat by remember { mutableStateOf(false) }
    var showTags by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    fun apply(t: Task) {
        task = t
        if (!isNew) Repo.updateTask(t)
    }

    fun finish() {
        if (isNew) {
            if (task.title.isNotBlank()) Repo.addTask(task)
        } else {
            Repo.updateTask(task)
        }
        onDone()
    }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        androidx.activity.compose.BackHandler { finish() }
        // ---------------------------------------------------------------- top bar
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .clickable { finish() },
                contentAlignment = Alignment.Center
            ) { GlyphIcon(Glyph.Inbox, AppColors.OnSurface, size = 22.dp) }
            Spacer(Modifier.weight(1f))
            if (!isNew) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .clickable { showDelete = true },
                    contentAlignment = Alignment.Center
                ) { CoreIcon(IconTrash, AppColors.OnSurface, 22.dp) }
            }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // title row
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PriorityCheckbox(task.priority, task.isCompleted, onToggle = {
                    apply(task.copy(completedAt = if (task.isCompleted) null else LocalDateTime.now()))
                })
                Spacer(Modifier.width(16.dp))
                BasicTextField(
                    value = task.title,
                    onValueChange = { apply(task.copy(title = it)) },
                    singleLine = false,
                    textStyle = TextStyle(fontSize = 19.sp, color = AppColors.OnSurface),
                    cursorBrush = SolidColor(AppColors.Primary),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (task.title.isEmpty()) {
                            Text("任务名称", fontSize = 19.sp, color = AppColors.SecondaryText)
                        }
                        inner()
                    }
                )
            }
            SectionDivider()

            EditRow(
                glyph = Glyph.Calendar,
                label = if (task.start == null) "无开始日期" else DateFmt.longDateTime(task.start!!),
                valueColor = if (task.start != null && task.start!!.toLocalDate().isBefore(Repo.now.toLocalDate()))
                    AppColors.HighPriority else AppColors.SecondaryText,
                onClick = { dateTarget = "start" }
            )
            SectionDivider()
            EditRow(
                glyph = Glyph.Calendar,
                label = if (task.due == null) "无截止日期" else DateFmt.longDateTime(task.due!!),
                valueColor = if (Repo.isOverdue(task)) AppColors.HighPriority else AppColors.SecondaryText,
                onClick = { dateTarget = "due" }
            )
            SectionDivider()
            EditRow(
                glyph = Glyph.Repeat,
                label = if (task.repeatRule.isEmpty()) "不重复" else RepeatRules.label(task.repeatRule),
                onClick = { showRepeat = true }
            )
            SectionDivider()
            PriorityRow(task.priority) { p -> apply(task.copy(priority = p)) }
            SectionDivider()
            EditRow(
                glyph = Glyph.List,
                label = "",
                custom = {
                    Chip(
                        Repo.listName(task.listId),
                        glyph = Glyph.List,
                        background = (Repo.list(task.listId)?.color?.let { Color(it) }
                            ?: AppColors.Chip).copy(alpha = 0.22f)
                    )
                },
                onClick = { }
            )
            SectionDivider()
            EditRow(
                glyph = Glyph.Tag,
                label = "",
                custom = {
                    if (task.tags.isEmpty()) {
                        Text("添加标签", fontSize = 15.sp, color = AppColors.SecondaryText)
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            task.tags.forEach { Chip(it, glyph = Glyph.Tag) }
                        }
                    }
                },
                onClick = { showTags = true }
            )
            SectionDivider()

            // subtasks
            task.subtasks.forEach { sub ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlyphIcon(Glyph.List, AppColors.SecondaryText, size = 18.dp)
                    Spacer(Modifier.width(18.dp))
                    PriorityCheckbox(sub.priority, sub.isCompleted, size = 18.dp, onToggle = {
                        val subs = task.subtasks.toMutableList()
                        val i = subs.indexOfFirst { it.id == sub.id }
                        subs[i] = sub.copy(completedAt = if (sub.isCompleted) null else LocalDateTime.now())
                        apply(task.copy(subtasks = subs))
                    })
                    Spacer(Modifier.width(12.dp))
                    Text(sub.title, fontSize = 15.sp, color = AppColors.OnSurface)
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        val sub = Task(
                            id = "s" + System.currentTimeMillis(),
                            title = "新子任务",
                            priority = Priority.NONE
                        )
                        apply(task.copy(subtasks = task.subtasks + sub))
                    }
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.width(36.dp))
                Text("添加子任务", fontSize = 15.sp, color = AppColors.SecondaryText)
            }
            SectionDivider()

            EditRow(glyph = Glyph.Bell, label = "启用提醒", valueColor = AppColors.HighPriority,
                onClick = { })
            Text(
                "提醒在 Android 设置中被禁用",
                fontSize = 12.sp,
                color = AppColors.HighPriority,
                modifier = Modifier.padding(start = 60.dp, bottom = 6.dp)
            )
            SectionDivider()
            EditRow(
                glyph = Glyph.Note,
                label = "",
                custom = {
                    BasicTextField(
                        value = task.notes,
                        onValueChange = { apply(task.copy(notes = it)) },
                        textStyle = TextStyle(fontSize = 15.sp, color = AppColors.OnSurface),
                        cursorBrush = SolidColor(AppColors.Primary),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (task.notes.isEmpty()) {
                                Text("描述", fontSize = 15.sp, color = AppColors.SecondaryText)
                            }
                            inner()
                        }
                    )
                },
                onClick = { }
            )
            SectionDivider()
            EditRow(glyph = Glyph.Place, label = "添加位置", onClick = { })
            SectionDivider()
            EditRow(glyph = Glyph.Paperclip, label = "添加附件", onClick = { })
            SectionDivider()
            EditRow(glyph = Glyph.CalendarOff, label = "不添加到日历", onClick = { })
            SectionDivider()
            EditRow(glyph = Glyph.Timer, label = "计时器", custom = {
                CoreIcon(IconPlay, AppColors.SecondaryText, 20.dp)
            }, onClick = { })
            SectionDivider()
            Text(
                "创建时间  ${task.createdAt.format(DateFmt.stamp)}",
                fontSize = 14.sp,
                color = AppColors.SecondaryText,
                modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 4.dp)
            )
            Text(
                "修改时间  ${task.modifiedAt.format(DateFmt.stamp)}",
                fontSize = 14.sp,
                color = AppColors.SecondaryText,
                modifier = Modifier.padding(start = 20.dp, bottom = 14.dp)
            )
            Spacer(Modifier.height(40.dp))
        }
    }

    if (dateTarget != null) {
        DateSheet(
            initial = if (dateTarget == "due") task.due else task.start,
            onDismiss = { dateTarget = null },
            onClear = {
                apply(if (dateTarget == "due") task.copy(due = null) else task.copy(start = null))
                dateTarget = null
            },
            onPick = { dt ->
                apply(if (dateTarget == "due") task.copy(due = dt) else task.copy(start = dt))
                dateTarget = null
            }
        )
    }
    if (showRepeat) {
        RepeatDialog(
            current = task.repeatRule,
            onDismiss = { showRepeat = false },
            onPick = { apply(task.copy(repeatRule = it)); showRepeat = false }
        )
    }
    if (showTags) {
        TagPicker(
            selected = task.tags,
            onDismiss = { showTags = false },
            onConfirm = { apply(task.copy(tags = it)); showTags = false }
        )
    }
    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("删除任务？") },
            text = { Text("此操作无法撤消。") },
            confirmButton = {
                TextButton(onClick = { Repo.deleteTask(task.id); showDelete = false; onDone() }) {
                    Text("删除", color = AppColors.HighPriority)
                }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun EditRow(
    glyph: Glyph,
    label: String,
    valueColor: Color = AppColors.SecondaryText,
    custom: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlyphIcon(glyph, AppColors.SecondaryText, size = 20.dp)
        Spacer(Modifier.width(18.dp))
        if (custom != null) {
            custom()
        } else {
            Text(label, fontSize = 15.sp, color = valueColor)
        }
    }
}

@Composable
private fun PriorityRow(current: Priority, onPick: (Priority) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlyphIcon(Glyph.Flag, AppColors.SecondaryText, size = 20.dp)
        Spacer(Modifier.width(18.dp))
        Text("优先级", fontSize = 15.sp, color = AppColors.OnSurface)
        Spacer(Modifier.weight(1f))
        val order = listOf(Priority.NONE, Priority.LOW, Priority.MEDIUM, Priority.HIGH)
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            order.forEach { p ->
                CheckboxCircle(
                    selected = current == p,
                    color = priorityColor(p),
                    size = 22.dp
                ) { onPick(p) }
            }
        }
        Spacer(Modifier.width(8.dp))
    }
}
