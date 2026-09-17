package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun DetailScreen(
    habits: MutableList<Habit>,
    state: LoopState,
    persist: () -> Unit,
    onBack: () -> Unit
) {
    val habit = habits.firstOrNull { it.id == state.detailId }
    if (habit == null) { onBack(); return }
    val color = parseColor(habit.color)
    val today = Logic.today()
    var showOverflow by remember { mutableStateOf(false) }
    var deleteAsk by remember { mutableStateOf(false) }
    var scorePeriod by remember { mutableStateOf("周") }
    var historyPeriod by remember { mutableStateOf("周") }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(
            Modifier.fillMaxWidth().background(color).statusBarsPadding().height(58.dp).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(40.dp).clickable { onBack() }, contentAlignment = Alignment.Center) {
                BackIcon(Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Text(habit.name, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(44.dp).clickable {
                state.editIsNew = false
                state.editDraft = habit.deepCopy()
                state.screen = Screen.EDIT
            }, contentAlignment = Alignment.Center) { PencilIcon(Color.White) }
            Box(Modifier.size(44.dp)) {
                Box(Modifier.fillMaxSize().clickable { showOverflow = true }, contentAlignment = Alignment.Center) {
                    DotsIcon(Color.White)
                }
                DropdownMenu(showOverflow, onDismissRequest = { showOverflow = false }) {
                    DropdownMenuItem(text = { Text(if (habit.archived) "取消存档" else "存档") }, onClick = {
                        habit.archived = !habit.archived; persist(); showOverflow = false
                    })
                    DropdownMenuItem(text = { Text("删除") }, onClick = { showOverflow = false; deleteAsk = true })
                }
            }
        }

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // question + meta
            Column(Modifier.fillMaxWidth().background(Color(0xFFF1F1F4)).padding(horizontal = 18.dp, vertical = 14.dp)) {
                if (habit.question.isNotBlank())
                    Text(habit.question, color = color, fontSize = 19.sp)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CalendarIcon(Color(0xFF757575))
                    Spacer(Modifier.width(6.dp))
                    Text(Logic.freqSummary(habit), color = Color(0xFF424242), fontSize = 15.sp)
                    if (habit.reminder != null) {
                        Spacer(Modifier.width(18.dp))
                        BellIcon(Color(0xFF757575))
                        Spacer(Modifier.width(6.dp))
                        Text(habit.reminder!!, color = Color(0xFF424242), fontSize = 15.sp)
                    }
                }
            }

            // overview
            SectionTitle("总览", color)
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                RingProgress(Logic.score(habit, today), color, 44.dp)
                Spacer(Modifier.width(18.dp))
                StatCell("${(Logic.score(habit, today) * 100).roundToInt()}%", "成绩", color)
                StatCell("+${monthDelta(habit, today)}%", "月", color)
                StatCell("+${yearDelta(habit, today)}%", "年", color)
                StatCell("${Logic.totalCount(habit)}", "总数", color)
            }

            if (habit.type == HabitType.QUANTIFIED) {
                SectionTitle("目标", color)
                GoalBars(habit, today, color)
            }

            SectionTitleWithPeriod("成绩", color, scorePeriod, { scorePeriod = it })
            ScoreChart(habit, today, color, scorePeriod)

            SectionTitleWithPeriod("历史", color, historyPeriod, { historyPeriod = it })
            HistoryChart(habit, today, color, historyPeriod)

            SectionTitle("日历", color)
            CalendarSection(habit, today, color)

            SectionTitle("最佳连续完成次数", color)
            Text("${Logic.maxStreak(habit)}", color = color, fontSize = 30.sp,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp))

            SectionTitle("频率", color)
            FrequencyBars(habit, color)
            Spacer(Modifier.height(30.dp))
        }
    }

    if (deleteAsk) {
        ConfirmDialog(
            title = "删除习惯?",
            message = "习惯将被永久地删除。此操作无法撤销。",
            confirmLabel = "完成了",
            dismissLabel = "未完成",
            onDismiss = { deleteAsk = false },
            onConfirm = {
                habits.removeAll { it.id == habit.id }
                persist(); deleteAsk = false; onBack()
            }
        )
    }
}

private fun monthDelta(h: Habit, today: LocalDate): Int {
    val now = Logic.score(h, today)
    val before = Logic.score(h, today.minusDays(30))
    return ((now - before) * 100).roundToInt().coerceAtLeast(0)
}

private fun yearDelta(h: Habit, today: LocalDate): Int {
    val now = Logic.score(h, today)
    val before = Logic.score(h, today.minusDays(365))
    return ((now - before) * 100).roundToInt().coerceAtLeast(0)
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.StatCell(value: String, label: String, color: Color) {
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 19.sp, fontWeight = FontWeight.Medium)
        Text(label, color = Color(0xFF666666), fontSize = 14.sp)
    }
}

@Composable
private fun SectionTitle(title: String, color: Color) {
    Box(Modifier.fillMaxWidth().padding(start = 18.dp, top = 16.dp, bottom = 6.dp)) {
        Text(title, color = color, fontSize = 21.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SectionTitleWithPeriod(title: String, color: Color, period: String, onPeriod: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = color, fontSize = 21.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.weight(1f))
        Box {
            Row(Modifier.clickable { open = true }.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(period, color = Color(0xFF555555), fontSize = 16.sp)
                Spacer(Modifier.width(6.dp))
                DownArrowIcon(Color(0xFF555555))
            }
            DropdownMenu(open, onDismissRequest = { open = false }) {
                (if (title == "历史") listOf("天", "周", "月") else listOf("周", "月", "年")).forEach { p ->
                    DropdownMenuItem(text = { Text(p) }, onClick = { onPeriod(p); open = false })
                }
            }
        }
    }
}

@Composable
private fun ScoreChart(habit: Habit, today: LocalDate, color: Color, period: String) {
    val points = 30
    Box(Modifier.fillMaxWidth().height(220.dp).padding(horizontal = 18.dp, vertical = 8.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val levels = listOf(1.0f, 0.8f, 0.6f, 0.4f, 0.2f)
            levels.forEach { lv ->
                val y = h * (1f - lv)
                drawLine(Color(0xFFE0E0E0), Offset(0f, y), Offset(w, y), 2f)
            }
            val xs = (0 until points).map { i ->
                val d = today.minusDays((points - 1 - i).toLong())
                w * i / (points - 1).toFloat() to (h * (1f - Logic.score(habit, d).toFloat()))
            }
            if (xs.size >= 2) {
                for (i in 0 until xs.size - 1) {
                    drawLine(color, Offset(xs[i].first, xs[i].second), Offset(xs[i + 1].first, xs[i + 1].second), 4f, StrokeCap.Round)
                }
            }
            val last = xs.last()
            drawCircle(color, 7f, Offset(last.first, last.second))
        }
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
        Spacer(Modifier.weight(1f))
        Text("${today.year}年${today.monthValue}月", color = Color(0xFF777777), fontSize = 13.sp)
    }
}

@Composable
private fun HistoryChart(habit: Habit, today: LocalDate, color: Color, period: String) {
    val sessions = when (period) {
        "天" -> 14
        "月" -> 6
        else -> 12
    }
    val values = (0 until sessions).map { i ->
        val idx = sessions - 1 - i
        when (period) {
            "天" -> (habit.checkins[Logic.key(today.minusDays(idx.toLong()))] ?: 0).toDouble()
            "月" -> Logic.totalInWindow(habit, (idx + 1) * 30, today) - Logic.totalInWindow(habit, idx * 30, today)
            else -> Logic.totalInWindow(habit, (idx + 1) * 7, today) - Logic.totalInWindow(habit, idx * 7, today)
        }
    }
    val maxV = max(1.0, values.maxOrNull() ?: 1.0)
    Box(Modifier.fillMaxWidth().height(200.dp).padding(horizontal = 18.dp, vertical = 10.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            listOf(0.0f, 0.25f, 0.5f, 0.75f, 1.0f).forEach { lv ->
                val y = h * (1f - lv)
                drawLine(Color(0xFFE0E0E0), Offset(0f, y), Offset(w, y), 2f)
            }
            val slot = w / sessions
            values.forEachIndexed { i, v ->
                val barH = (v / maxV * (h - 10f)).toFloat()
                val left = slot * i + slot * 0.3f
                drawRect(color, Offset(left, h - barH), Size(slot * 0.4f, barH))
            }
        }
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
        if (period == "天") {
            Spacer(Modifier.weight(1f))
            Text("今天", color = Color(0xFF777777), fontSize = 13.sp)
        } else {
            Spacer(Modifier.weight(1f))
            Text("${today.year}", color = Color(0xFF777777), fontSize = 13.sp)
        }
    }
}

@Composable
private fun CalendarSection(habit: Habit, today: LocalDate, color: Color) {
    val months = (0 until 3).map { today.minusMonths(it.toLong()) }.reversed()
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
        months.forEach { m ->
            Text("${Logic.MONTH_CN[m.monthValue - 1]} ${m.year}", color = Color(0xFF666666), fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
            Row(Modifier.fillMaxWidth()) {
                (0 until 7).forEach { wd ->
                    Text(Logic.WEEKDAY_CN[wd].removePrefix("周"), color = Color(0xFF999999), fontSize = 11.sp,
                        modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
            Logic.monthGrid(m).chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                    week.forEach { cell ->
                        Box(Modifier.weight(1f).padding(1.dp)) {
                            if (cell != null) {
                                val done = Logic.dayDone(habit, cell)
                                val partial = Logic.hasCheckin(habit, cell) && !done
                                val bg = when {
                                    done -> color
                                    partial -> color.copy(alpha = 0.35f)
                                    else -> Color(0xFFEDEDED)
                                }
                                Box(Modifier.fillMaxWidth().height(30.dp).clip(RoundedCornerShape(3.dp)).background(bg),
                                    contentAlignment = Alignment.Center) {
                                    Text("${cell.dayOfMonth}", color = if (done) Color.White else Color(0xFF555555), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text("编辑", color = Color(0xFF9E9E9E), fontSize = 17.sp)
        }
    }
}

@Composable
private fun GoalBars(habit: Habit, today: LocalDate, color: Color) {
    val target = habit.target
    val rows = listOf(
        "今日" to Logic.totalInWindow(habit, 1, today),
        "周" to Logic.totalInWindow(habit, 7, today),
        "月" to Logic.monthTotal(habit, today),
        "季度" to Logic.totalInWindow(habit, 90, today),
        "年" to Logic.totalInWindow(habit, 365, today)
    )
    val scales = listOf(1.0, 7.0, 30.0, 90.0, 365.0)
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
        rows.forEachIndexed { i, (label, value) ->
            val goal = target * scales[i]
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = Color(0xFF555555), fontSize = 15.sp, modifier = Modifier.width(50.dp))
                Box(Modifier.weight(1f).height(28.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFFEDEDED))) {
                    val frac = if (goal <= 0) 0f else (value / goal).toFloat().coerceIn(0f, 1f)
                    Box(Modifier.fillMaxWidth(frac).height(28.dp).clip(RoundedCornerShape(3.dp)).background(color))
                    Text(
                        "${Logic.formatNumber(value)}${if (label == "今日") "" else " / ${Logic.formatNumber(goal)}"}",
                        color = Color(0xFF555555), fontSize = 13.sp,
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FrequencyBars(habit: Habit, color: Color) {
    val today = Logic.today()
    val counts = (0 until 7).map { wd ->
        var count = 0
        var d = today.minusDays(27)
        while (!d.isAfter(today)) {
            if (d.dayOfWeek.value - 1 == wd && Logic.hasCheckin(habit, d)) count++
            d = d.plusDays(1)
        }
        count
    }
    val maxV = max(1, counts.maxOrNull() ?: 1)
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
        (0 until 7).forEach { wd ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(Logic.WEEKDAY_CN[wd], color = Color(0xFF666666), fontSize = 14.sp, modifier = Modifier.width(48.dp))
                Box(Modifier.weight(1f).height(20.dp), contentAlignment = Alignment.CenterStart) {
                    Canvas(Modifier.fillMaxWidth().height(20.dp)) {
                        val frac = counts[wd].toFloat() / maxV
                        drawRoundRect(color.copy(alpha = 0.6f), Offset(0f, 0f),
                            Size(size.width * frac, size.height), androidx.compose.ui.geometry.CornerRadius(6f, 6f))
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("4周前", color = Color(0xFF888888), fontSize = 12.sp)
            Text("今天", color = Color(0xFF888888), fontSize = 12.sp)
        }
    }
}
