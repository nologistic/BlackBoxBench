package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSheet(
    initial: LocalDateTime?,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onPick: (LocalDateTime) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var month by remember { mutableStateOf(YearMonth.from(initial?.toLocalDate() ?: Repo.now.toLocalDate())) }
    var day by remember { mutableStateOf(initial?.toLocalDate() ?: Repo.now.toLocalDate()) }
    var time by remember { mutableStateOf(initial?.let { DateFmt.time(it) }) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    QuickRow(if (day == Repo.now.toLocalDate()) "截止日期" else DateFmt.longDate(day), false) {
                        time = null
                    }
                    QuickRow(if (time == null) "截止时间" else time!!, false) { time = "09:00" }
                    QuickRow("截止日期前一天", false) { day = day.minusDays(1); time = null }
                    QuickRow("截止日期前一周", false) { day = day.minusDays(7); time = null }
                    QuickRow("无日期", false) { onClear() }
                }
                Column(Modifier.weight(1f)) {
                    listOf("09:00", "13:00", "17:00", "20:00").forEach { t ->
                        QuickRow(t, time == t) { time = t }
                    }
                    QuickRow("挑选时间", false) { time = "12:00" }
                    QuickRow("无时间", time == null) { time = null }
                }
            }
            Spacer(Modifier.height(8.dp))
            CalendarView(month, day, onMonth = { month = it }, onDay = { day = it })
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    GlyphIcon(Glyph.Calendar, AppColors.SecondaryText, size = 20.dp)
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("取消") }
                TextButton(onClick = {
                    val base = day
                    val dt = if (time == null) base.atStartOfDay()
                    else base.atTime(time!!.substring(0, 2).toInt(), time!!.substring(3, 5).toInt())
                    onPick(dt)
                }) { Text("确定") }
            }
        }
    }
}

@Composable
private fun QuickRow(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) Color(0xFFE3F2FD) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, fontSize = 15.sp, color = if (selected) AppColors.Primary else AppColors.OnSurface)
    }
}

@Composable
private fun CalendarView(
    month: YearMonth,
    selected: LocalDate,
    onMonth: (YearMonth) -> Unit,
    onDay: (LocalDate) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            Text("${month.year}年${month.monthValue}月", fontSize = 16.sp, color = AppColors.OnSurface)
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.size(36.dp).clickable { onMonth(month.minusMonths(1)) },
                contentAlignment = Alignment.Center
            ) { CoreIcon(IconUp, AppColors.SecondaryText, 18.dp) }
            Box(
                Modifier.size(36.dp).clickable { onMonth(month.plusMonths(1)) },
                contentAlignment = Alignment.Center
            ) { CoreIcon(IconDown, AppColors.SecondaryText, 18.dp) }
        }
        Row(Modifier.fillMaxWidth()) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { d ->
                Text(
                    d, fontSize = 13.sp, color = AppColors.SecondaryText,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        val first = month.atDay(1)
        val offset = first.dayOfWeek.value - 1
        val length = month.lengthOfMonth()
        var d = 1
        val cells = offset + length
        val rows = (cells + 6) / 7
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val idx = r * 7 + c
                    val dayNum = idx - offset + 1
                    if (dayNum in 1..length) {
                        val date = month.atDay(dayNum)
                        val isSel = date == selected
                        val isToday = date == Repo.now.toLocalDate()
                        Box(
                            Modifier
                                .weight(1f)
                                .height(42.dp)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(percent = 50))
                                .background(if (isSel) AppColors.Primary else Color.Transparent)
                                .clickable { onDay(date) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "$dayNum",
                                fontSize = 14.sp,
                                color = when {
                                    isSel -> Color.White
                                    isToday -> AppColors.Primary
                                    else -> AppColors.OnSurface
                                }
                            )
                        }
                    } else {
                        Spacer(Modifier.weight(1f).height(42.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun RepeatDialog(current: String, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val options = RepeatRules.options + ("CUSTOM" to "自定义…")
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        text = {
            Column {
                options.forEach { (rule, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (rule == "CUSTOM") onPick("FREQ=WEEKLY;BYDAY=MO,TH") else onPick(rule)
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = current == rule, onClick = {
                            if (rule == "CUSTOM") onPick("FREQ=WEEKLY;BYDAY=MO,TH") else onPick(rule)
                        })
                        Text(label, fontSize = 16.sp, color = AppColors.OnSurface)
                    }
                }
            }
        }
    )
}

@Composable
fun TagPicker(selected: List<String>, onDismiss: () -> Unit, onConfirm: (List<String>) -> Unit) {
    var query by remember { mutableStateOf("") }
    var chosen by remember { mutableStateOf(selected.toSet()) }
    val all = (Repo.allTags() + chosen).distinct().sorted()
    val filtered = all.filter { query.isBlank() || it.contains(query, ignoreCase = true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(chosen.toList()) }) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        title = {
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                textStyle = TextStyle(fontSize = 17.sp, color = AppColors.OnSurface),
                cursorBrush = SolidColor(AppColors.Primary),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (query.isEmpty()) Text("输入标签名称", fontSize = 17.sp, color = AppColors.SecondaryText)
                    inner()
                }
            )
        },
        text = {
            Column {
                if (query.isNotBlank() && all.none { it.equals(query, true) }) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { chosen = chosen + query; query = "" }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("+ 新建标签 \"$query\"", fontSize = 15.sp, color = AppColors.OnSurface)
                    }
                }
                filtered.forEach { tag ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                chosen = if (chosen.contains(tag)) chosen - tag else chosen + tag
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CheckboxCircle(selected = chosen.contains(tag), color = AppColors.Primary, size = 22.dp) {
                            chosen = if (chosen.contains(tag)) chosen - tag else chosen + tag
                        }
                        Spacer(Modifier.width(14.dp))
                        Text(tag, fontSize = 15.sp, color = AppColors.OnSurface)
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var groupDialog by remember { mutableStateOf(false) }
    var sortDialog by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            SheetRow(Glyph.List, "分组  ${Repo.grouping.label}") { groupDialog = true }
            SheetRow(Glyph.Sort, "排序  ${Repo.sortMode.label}") { sortDialog = true }
            SheetRow(Glyph.Checklist, "子任务  我的顺序") { }
            ToggleRow("显示未开始项目", Repo.showUnstarted) { Repo.showUnstarted = it; Repo.persist() }
            ToggleRow("显示已完成任务", Repo.showCompleted) { Repo.showCompleted = it; Repo.persist() }
            ToggleRow("显示已完成的子任务", Repo.showCompletedSubtasks) {
                Repo.showCompletedSubtasks = it; Repo.persist()
            }
            ToggleRow("将已完成任务移至底部", Repo.moveCompletedToBottom) {
                Repo.moveCompletedToBottom = it; Repo.persist()
            }
            SheetRow(Glyph.Sort, "已完成  按完成时间  ↓") { }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (groupDialog) {
        ChoiceDialog("分组", Grouping.entries.map { it to it.label }, Repo.grouping) {
            Repo.grouping = it; Repo.persist(); groupDialog = false
        }
    }
    if (sortDialog) {
        ChoiceDialog("排序", SortMode.entries.map { it to it.label }, Repo.sortMode) {
            Repo.sortMode = it; Repo.persist(); sortDialog = false
        }
    }
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    current: T,
    onPick: (T) -> Unit
) {
    AlertDialog(
        onDismissRequest = { onPick(current) },
        confirmButton = { TextButton(onClick = { onPick(current) }) { Text("取消") } },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onPick(value) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = value == current, onClick = { onPick(value) })
                        Text(label, fontSize = 16.sp, color = AppColors.OnSurface)
                    }
                }
            }
        }
    )
}

@Composable
private fun SheetRow(glyph: Glyph, text: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlyphIcon(glyph, AppColors.OnSurface, size = 20.dp)
        Spacer(Modifier.width(18.dp))
        Text(text, fontSize = 15.sp, color = AppColors.OnSurface)
    }
}

@Composable
private fun ToggleRow(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, fontSize = 15.sp, color = AppColors.OnSurface, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
