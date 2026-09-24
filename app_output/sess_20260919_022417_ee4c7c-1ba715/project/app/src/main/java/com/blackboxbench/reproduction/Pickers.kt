package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------- 颜色选择器 ----------
@Composable
fun ColorPickerDialog(onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, confirmButton = {}, dismissButton = {
        TextButton(onClick = onDismiss) { Text("取消") }
    }, title = null, text = {
        LazyVerticalGrid(columns = GridCells.Fixed(5), modifier = Modifier.heightIn(max = 480.dp)) {
            items(AppColors.palette.size) { i ->
                val c = Color(AppColors.palette[i])
                Box(
                    Modifier.padding(6.dp).size(44.dp).clip(CircleShape).background(c)
                        .clickable { onPick(AppColors.palette[i]); onDismiss() }
                )
            }
        }
    })
}

// ---------- 图标选择器（有限集） ----------
data class IconEntry(val key: String, val icon: ImageVector)

val iconChoices = listOf(
    IconEntry("check", Icons.Filled.Check), IconEntry("done_all", Icons.Filled.Done),
    IconEntry("star", Icons.Filled.Star), IconEntry("favorite", Icons.Filled.Favorite),
    IconEntry("home", Icons.Filled.Home), IconEntry("list", Icons.Filled.List),
    IconEntry("work", Icons.Filled.ShoppingCart), IconEntry("person", Icons.Filled.Person),
    IconEntry("call", Icons.Filled.Call), IconEntry("email", Icons.Filled.Email),
    IconEntry("alarm", Icons.Filled.Notifications), IconEntry("event", Icons.Filled.DateRange),
    IconEntry("place", Icons.Filled.Place), IconEntry("edit", Icons.Filled.Edit),
    IconEntry("search", Icons.Filled.Search), IconEntry("build", Icons.Filled.Build),
    IconEntry("info", Icons.Filled.Info), IconEntry("lock", Icons.Filled.Lock),
    IconEntry("send", Icons.Filled.Send), IconEntry("share", Icons.Filled.Share),
    IconEntry("refresh", Icons.Filled.Refresh), IconEntry("add", Icons.Filled.Add),
    IconEntry("face", Icons.Filled.Face), IconEntry("warning", Icons.Filled.Warning)
)

fun iconByKey(key: String): ImageVector? = iconChoices.firstOrNull { it.key == key }?.icon

@Composable
fun IconPickerDialog(onDismiss: () -> Unit, onPick: (String) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, confirmButton = {}, title = { Text("图标") }, text = {
        LazyVerticalGrid(columns = GridCells.Fixed(6), modifier = Modifier.heightIn(max = 420.dp)) {
            items(iconChoices.size) { i ->
                Icon(
                    iconChoices[i].icon, null,
                    Modifier.padding(10.dp).size(30.dp).clickable { onPick(iconChoices[i].key); onDismiss() }
                )
            }
        }
    })
}

// ---------- 优先级四圆点 ----------
@Composable
fun PriorityDots(selected: Priority, onSelect: (Priority) -> Unit, modifier: Modifier = Modifier) {
    val colors = listOf(AppColors.Gray, AppColors.Blue, AppColors.Yellow, AppColors.Red)
    val values = listOf(Priority.NONE, Priority.LOW, Priority.MEDIUM, Priority.HIGH)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
        values.forEachIndexed { i, p ->
            Box(
                Modifier.size(30.dp).clip(CircleShape)
                    .border(2.dp, colors[i], CircleShape)
                    .clickable { onSelect(p) },
                contentAlignment = Alignment.Center
            ) {
                if (p == selected) Box(Modifier.size(14.dp).clip(CircleShape).background(colors[i]))
            }
        }
    }
}

fun priorityColor(p: Priority): Color = when (p) {
    Priority.HIGH -> AppColors.Red
    Priority.MEDIUM -> AppColors.Yellow
    Priority.LOW -> AppColors.Blue
    Priority.NONE -> AppColors.Gray
}

// ---------- 重复设置 ----------
@Composable
fun RepeatDialog(current: String, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val options = listOf("" to "不重复", "FREQ=DAILY" to "每天", "FREQ=WEEKLY" to "每周", "FREQ=MONTHLY" to "每月", "FREQ=YEARLY" to "每年")
    AlertDialog(onDismissRequest = onDismiss, confirmButton = {}, title = null, text = {
        Column {
            options.forEach { (rule, label) ->
                Row(
                    Modifier.fillMaxWidth().clickable { onPick(rule); onDismiss() }.padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = current == rule || (rule.isNotEmpty() && current.startsWith(rule) && !current.contains(";")),
                        onClick = { onPick(rule); onDismiss() })
                    Spacer(Modifier.width(8.dp))
                    Text(label, fontSize = 17.sp)
                }
            }
            Row(Modifier.fillMaxWidth().clickable { onPick("FREQ=WEEKLY"); onDismiss() }.padding(vertical = 14.dp)) {
                RadioButton(selected = false, onClick = {})
                Spacer(Modifier.width(8.dp)); Text("自定义…", fontSize = 17.sp)
            }
        }
    })
}

// ---------- 日期时间选择底部面板 ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeSheet(
    initialDate: String, initialTime: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var date by remember { mutableStateOf(Dates.parseDate(initialDate)) }
    var time by remember { mutableStateOf(initialTime) }
    var month by remember { mutableStateOf((date ?: Dates.today()).withDayOfMonth(1)) }
    val today = Dates.today()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Column {
                    SheetOption("今天", date == today) { date = today }
                    SheetOption("明天", date == today.plusDays(1)) { date = today.plusDays(1) }
                    SheetOption("下周六", date == today.plusDays(7)) { date = today.plusDays(7) }
                    SheetOption("无日期", date == null) { date = null }
                }
                Spacer(Modifier.weight(1f))
                Column {
                    SheetOption("09:00", time == "09:00") { time = if (time == "09:00") "" else "09:00" }
                    SheetOption("13:00", time == "13:00") { time = if (time == "13:00") "" else "13:00" }
                    SheetOption("17:00", time == "17:00") { time = if (time == "17:00") "" else "17:00" }
                    SheetOption("20:00", time == "20:00") { time = if (time == "20:00") "" else "20:00" }
                    SheetOption("无时间", time.isEmpty()) { time = "" }
                }
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${month.year}年${month.monthValue}月", fontSize = 16.sp)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { month = month.minusMonths(1) }) { Icon(Icons.Filled.KeyboardArrowLeft, null) }
                IconButton(onClick = { month = month.plusMonths(1) }) { Icon(Icons.Filled.KeyboardArrowRight, null) }
            }
            val firstDow = month.dayOfWeek.value // 1=Mon
            val days = month.lengthOfMonth()
            val weeks = mutableListOf<MutableList<Int?>>()
            var cur = MutableList<Int?>(7) { null }
            for (d in 1..days) {
                val idx = (firstDow - 1 + d - 1) % 7
                cur[idx] = d
                if (idx == 6 || d == days) { weeks.add(cur); cur = MutableList(7) { null } }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEach {
                    Text(it, Modifier.weight(1f), color = AppColors.Subtle, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
            weeks.forEach { week ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    week.forEach { d ->
                        if (d == null) Box(Modifier.weight(1f).height(44.dp))
                        else {
                            val day = month.withDayOfMonth(d)
                            val isSel = date == day
                            val isToday = day == today
                            Box(
                                Modifier.weight(1f).height(44.dp), contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    Modifier.size(36.dp).clip(CircleShape)
                                        .background(if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .then(
                                            if (isToday && !isSel) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                            else Modifier
                                        )
                                        .clickable { date = day },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "$d",
                                        color = if (isSel) Color.White else if (isToday) MaterialTheme.colorScheme.primary else Color.Unspecified
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消") }
                TextButton(onClick = { onConfirm(date?.let { Dates.fmtDate(it) } ?: "", time) }) { Text("确定") }
            }
        }
    }
}

@Composable
private fun SheetOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = if (selected) MaterialTheme.colorScheme.primary else Color.Unspecified, fontSize = 16.sp)
    }
}

// ---------- 通用确认对话框 ----------
@Composable
fun ConfirmDialog(text: String, onCancel: () -> Unit, onOk: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        confirmButton = { TextButton(onClick = onOk) { Text("确定") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("取消") } },
        title = null,
        text = { Text(text, fontSize = 18.sp) }
    )
}

// ---------- 清单选择 ----------
@Composable
fun ListPickerDialog(onDismiss: () -> Unit, onPick: (String) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, confirmButton = {}, title = null, text = {
        Column {
            Store.lists.forEach { l ->
                Row(
                    Modifier.fillMaxWidth().clickable { onPick(l.id); onDismiss() }.padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.List, null, tint = if (l.color != -1) Color(l.color) else AppColors.Subtle)
                    Spacer(Modifier.width(12.dp))
                    Text(l.name, fontSize = 16.sp)
                }
            }
        }
    })
}
