package com.blackboxbench.reproduction

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

@Composable
fun DrawerContent(
    lists: List<TaskListItem>,
    tasks: List<TaskItem>,
    tags: List<String>,
    selectedList: String?,
    selectedTag: String?,
    onAll: () -> Unit,
    onList: (String) -> Unit,
    onTag: (String) -> Unit,
    onAddFilter: () -> Unit
) {
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 18.dp)
    ) {
        item {
            NavigationDrawerItem(
                label = { Text("我的任务", fontSize = 19.sp) },
                icon = { Text("▣", fontSize = 24.sp) },
                badge = { Text(tasks.count { !it.completed }.toString()) },
                selected = selectedList == null && selectedTag == null,
                onClick = onAll
            )
            DrawerSection("过滤器", "+", onAddFilter)
            DrawerLine("▣", "今天", tasks.count { it.due.startsWith(LocalDate.now().toString()) }) {}
            DrawerLine("◴", "最近修改过的", tasks.size) {}
            DrawerSection("标签", "+", {})
        }
        items(tags.size) { index ->
            val tag = tags[index]
            DrawerLine("▱", tag, tasks.count { it.tags.contains(tag) }) { onTag(tag) }
        }
        item {
            DrawerSection("地点", "+", {})
            DrawerSection("本地清单", "+", {})
        }
        items(lists.size) { index ->
            val list = lists[index]
            DrawerLine("☷", list.name, tasks.count { it.listId == list.id && !it.completed }, Color(list.color)) {
                onList(list.id)
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun DrawerSection(title: String, action: String, onAction: () -> Unit) {
    Surface(
        color = Color(0xFFFEFBFF),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 9.dp)
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(action, fontSize = 29.sp, modifier = Modifier.clickable { onAction() }.padding(horizontal = 8.dp))
        }
    }
}

@Composable
fun DrawerLine(
    symbol: String,
    title: String,
    count: Int,
    tint: Color = Color(0xFF25262B),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 22.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(symbol, color = tint, fontSize = 22.sp, modifier = Modifier.width(42.dp))
        Text(title, fontSize = 17.sp, modifier = Modifier.weight(1f))
        if (count > 0) Text(count.toString(), fontSize = 16.sp)
    }
}

@Composable
fun BottomSymbol(symbol: String, onClick: () -> Unit) {
    Text(symbol, fontSize = 30.sp, modifier = Modifier.clickable { onClick() }.padding(10.dp))
}

@Composable
fun TaskRow(
    task: TaskItem,
    list: TaskListItem?,
    showListChip: Boolean,
    onToggle: () -> Unit,
    onToggleSub: (SubTaskItem) -> Unit,
    onOpen: () -> Unit,
    onLongPress: () -> Unit
) {
    val color = priorityColor(task.priority)
    Column(
        modifier = Modifier.fillMaxWidth()
            .pointerInput(task.id, task.completed) {
                detectTapGestures(onTap = { onOpen() }, onLongPress = { onLongPress() })
            }
            .padding(horizontal = 18.dp, vertical = 11.dp)
            .alpha(if (task.completed) 0.56f else 1f)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            CheckSquare(task.completed, color, onToggle)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        task.title,
                        fontSize = 18.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (task.completed) TextDecoration.LineThrough else null,
                        modifier = Modifier.weight(1f)
                    )
                    if (task.due.isNotBlank()) {
                        Text(
                            formatDue(task.due),
                            fontSize = 14.sp,
                            color = Color(0xFF565760),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                if (task.notes.isNotBlank()) {
                    Text(task.notes, color = Color(0xFF565760), fontSize = 15.sp, maxLines = 2)
                }
                if (task.tags.isNotEmpty() || task.subtasks.isNotEmpty() || task.repeat.isNotBlank() || showListChip) {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()).padding(top = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (task.subtasks.isNotEmpty()) Chip("⌃ " + task.subtasks.count { !it.completed })
                        if (task.repeat.isNotBlank()) Chip("↻")
                        if (showListChip && list != null) Chip("☷ " + list.name, Color(list.color).copy(alpha = 0.25f))
                        task.tags.forEach { Chip("▱ " + it) }
                    }
                }
            }
        }
        task.subtasks.forEach { sub ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onToggleSub(sub) }
                    .padding(start = 20.dp, top = 12.dp, bottom = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CheckSquare(sub.completed, Color(0xFF2196F3)) { onToggleSub(sub) }
                Spacer(Modifier.width(14.dp))
                Text(
                    sub.title,
                    fontSize = 17.sp,
                    textDecoration = if (sub.completed) TextDecoration.LineThrough else null
                )
            }
        }
    }
    HorizontalDivider(color = Color(0xFFF1EEF2))
}

@Composable
fun CheckSquare(checked: Boolean, color: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(24.dp).clickable { onClick() },
        shape = RoundedCornerShape(2.dp),
        color = if (checked) color else Color.Transparent,
        border = BorderStroke(2.dp, color)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (checked) Text("✓", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun Chip(text: String, color: Color = Color(0xFFE8E8EC)) {
    Surface(color = color, shape = RoundedCornerShape(9.dp), modifier = Modifier.padding(end = 5.dp)) {
        Text(text, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
    }
}

fun priorityColor(priority: String) = when (priority) {
    "high" -> Color(0xFFF44336)
    "medium" -> Color(0xFFFFB800)
    "low" -> Color(0xFF2196F3)
    else -> Color(0xFF9E9E9E)
}

fun formatDue(value: String): String {
    return try {
        val date = LocalDate.parse(value.take(10))
        val label = when (date) {
            LocalDate.now() -> "今天"
            LocalDate.now().plusDays(1) -> "明天"
            else -> if (date.isBefore(LocalDate.now())) {
                "逾期 " + date.monthValue + "/" + date.dayOfMonth
            } else {
                date.monthValue.toString() + "/" + date.dayOfMonth
            }
        }
        if (value.length > 10) label + " " + value.substring(11).take(5) else label
    } catch (_: Exception) {
        value
    }
}

@Composable
fun EditorLine(
    symbol: String,
    title: String,
    subtitle: String? = null,
    muted: Boolean = false,
    tint: Color = if (muted) Color(0xFFB0B0B5) else Color(0xFF25262B),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(symbol, color = Color(0xFFAAAAAF), fontSize = 23.sp, modifier = Modifier.width(54.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 17.sp, color = tint)
            if (subtitle != null) Text(subtitle, fontSize = 13.sp, color = tint)
        }
    }
    HorizontalDivider(color = Color(0xFFE5E2E7))
}

@Composable
fun DialogLine(text: String, onClick: () -> Unit) {
    Text(
        text,
        fontSize = 17.sp,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 11.dp, horizontal = 4.dp)
    )
}

@Composable
fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun SettingChoice(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 17.sp)
            Text(value, fontSize = 13.sp, color = Color(0xFF6A6971))
        }
        Text("›", fontSize = 26.sp)
    }
    HorizontalDivider(color = Color(0xFFF0EDF1))
}

@Composable
fun SimpleTopBar(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("‹", fontSize = 40.sp, modifier = Modifier.clickable { onBack() }.padding(horizontal = 8.dp))
        Text(title, fontSize = 25.sp, fontWeight = FontWeight.Medium)
    }
    HorizontalDivider()
}
