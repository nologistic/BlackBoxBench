package com.blackboxbench.reproduction

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditor(
    task: AppTask?,
    lists: List<TaskList>,
    initialList: String,
    allTags: List<String>,
    onBack: () -> Unit,
    onSave: (AppTask) -> Unit,
    onDelete: (String) -> Unit
) {
    var title by remember(task?.id) { mutableStateOf(task?.title.orEmpty()) }
    var notes by remember(task?.id) { mutableStateOf(task?.notes.orEmpty()) }
    var due by remember(task?.id) { mutableStateOf(task?.due.orEmpty()) }
    var priority by remember(task?.id) { mutableStateOf(task?.priority ?: "low") }
    var repeat by remember(task?.id) { mutableStateOf(task?.repeat.orEmpty()) }
    var listId by remember(task?.id) { mutableStateOf(task?.listId ?: initialList) }
    var tags by remember(task?.id) { mutableStateOf(task?.tags ?: emptyList()) }
    var subtasks by remember(task?.id) { mutableStateOf(task?.subtasks ?: emptyList()) }
    var subText by remember { mutableStateOf("") }
    var repeatDialog by remember { mutableStateOf(false) }
    var listDialog by remember { mutableStateOf(false) }
    var tagDialog by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }

    fun buildTask() = AppTask(
        id = task?.id ?: UUID.randomUUID().toString(),
        listId = listId,
        title = title.trim(),
        notes = notes,
        due = due,
        priority = priority,
        tags = tags,
        repeat = repeat,
        completed = task?.completed ?: false,
        subtasks = subtasks
    )

    Scaffold(
        topBar = {
            Row(Modifier.statusBarsPadding().fillMaxWidth().height(72.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("▣", fontSize = 31.sp, modifier = Modifier.clickable { if (title.isNotBlank()) onSave(buildTask()) }.padding(10.dp))
                Spacer(Modifier.weight(1f))
                if (task != null) Text("♜", fontSize = 29.sp, modifier = Modifier.clickable { deleteDialog = true }.padding(10.dp))
            }
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 30.dp)) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(30.dp).border(3.dp, Color(0xFF2196F3), RoundedCornerShape(3.dp)))
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        textStyle = TextStyle(fontSize = 23.sp, color = MaterialTheme.colorScheme.onBackground),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(18.dp)
                    )
                }
                HorizontalDivider()
            }
            item { EditorRow("▣", if (due.isBlank()) "无开始日期" else "开始 " + dueLabel(due)) { due = cycleDate(due) } }
            item { EditorRow("◷", if (due.isBlank()) "无截止日期" else dueLabel(due)) { due = cycleDate(due) } }
            item { EditorRow("↔", if (repeat.isBlank()) "不重复" else "重复 " + repeat) { repeatDialog = true } }
            item {
                Row(Modifier.fillMaxWidth().height(74.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("⚑", fontSize = 25.sp, modifier = Modifier.width(48.dp))
                    Text("优先级", fontSize = 20.sp, modifier = Modifier.width(112.dp))
                    listOf("none" to Color.Gray, "low" to Color(0xFF2196F3), "medium" to Color(0xFFFFB300), "high" to Color(0xFFF44336)).forEach { pair ->
                        Box(
                            Modifier.padding(horizontal = 8.dp).size(26.dp).border(3.dp, pair.second, CircleShape)
                                .clickable { priority = pair.first }
                        ) {
                            if (priority == pair.first) Box(Modifier.fillMaxSize().padding(5.dp).border(6.dp, pair.second, CircleShape))
                        }
                    }
                }
                HorizontalDivider()
            }
            item { EditorRow("☷", lists.firstOrNull { it.id == listId }?.name ?: "默认清单") { listDialog = true } }
            item { EditorRow("◇", if (tags.isEmpty()) "添加标签" else tags.joinToString(" · ")) { tagDialog = true } }
            item {
                Column {
                    subtasks.forEachIndexed { index, sub ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = sub.completed, onCheckedChange = { checked ->
                                subtasks = subtasks.mapIndexed { i, s -> if (i == index) s.copy(completed = checked) else s }
                            })
                            Text(sub.title, modifier = Modifier.weight(1f), fontSize = 18.sp)
                            Text("×", fontSize = 25.sp, modifier = Modifier.clickable {
                                subtasks = subtasks.filterIndexed { i, _ -> i != index }
                            }.padding(10.dp))
                        }
                    }
                    Row(Modifier.fillMaxWidth().height(68.dp).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("↳", fontSize = 27.sp, modifier = Modifier.width(48.dp))
                        BasicTextField(
                            value = subText,
                            onValueChange = { subText = it },
                            textStyle = TextStyle(fontSize = 19.sp, color = MaterialTheme.colorScheme.onBackground),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Text("+", fontSize = 31.sp, modifier = Modifier.clickable {
                            if (subText.isNotBlank()) {
                                subtasks = subtasks + SubTask(title = subText.trim())
                                subText = ""
                            }
                        }.padding(10.dp))
                    }
                    HorizontalDivider()
                }
            }
            item { EditorRow("♟", "启用提醒") { } }
            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("描述") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
            }
            item { EditorRow("⌖", "添加位置") { } }
            item { EditorRow("⌕", "添加附件") { } }
            item {
                Button(
                    onClick = { if (title.isNotBlank()) onSave(buildTask()) },
                    enabled = title.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().padding(18.dp).height(52.dp)
                ) { Text(if (task == null) "创建任务" else "保存更改", fontSize = 18.sp) }
            }
        }
    }

    if (repeatDialog) {
        ChoiceDialog("重复", listOf("不重复", "每天", "每周", "每月", "每年", "自定义…"), {
            repeat = if (it == "不重复") "" else it
            repeatDialog = false
        }) { repeatDialog = false }
    }
    if (listDialog) {
        ChoiceDialog("选择清单", lists.map { it.name }, { name ->
            listId = lists.first { it.name == name }.id
            listDialog = false
        }) { listDialog = false }
    }
    if (tagDialog) {
        AlertDialog(
            onDismissRequest = { tagDialog = false },
            title = { Text("选择标签") },
            text = {
                Column {
                    (allTags + listOf("家庭", "计划")).distinct().forEach { tag ->
                        Row(Modifier.fillMaxWidth().clickable {
                            tags = if (tags.contains(tag)) tags - tag else tags + tag
                        }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = tags.contains(tag), onCheckedChange = null)
                            Text(tag)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { tagDialog = false }) { Text("完成") } }
        )
    }
    if (deleteDialog && task != null) {
        AlertDialog(
            onDismissRequest = { deleteDialog = false },
            title = { Text("删除任务？") },
            text = { Text("该任务将从所有列表和过滤器中移除。") },
            dismissButton = { TextButton(onClick = { deleteDialog = false }) { Text("取消") } },
            confirmButton = { TextButton(onClick = { onDelete(task.id) }) { Text("删除", color = Color.Red) } }
        )
    }
}

private fun cycleDate(current: String): String = when {
    current.isBlank() -> "2026-09-20"
    current.startsWith("2026-09-20") -> "2026-09-21"
    else -> ""
}

@Composable
private fun EditorRow(icon: String, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(70.dp).clickable(onClick = onClick).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 25.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .55f), modifier = Modifier.width(48.dp))
        Text(label, fontSize = 20.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (label.startsWith("无") || label.startsWith("添加")) .48f else 1f))
    }
    HorizontalDivider()
}

@Composable
private fun ChoiceDialog(title: String, values: List<String>, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                values.forEach { value ->
                    Text(value, fontSize = 20.sp, modifier = Modifier.fillMaxWidth().clickable { onPick(value) }.padding(vertical = 12.dp))
                }
            }
        },
        confirmButton = {}
    )
}
