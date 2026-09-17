package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.time.LocalDate

@Composable
fun RingProgress(fraction: Double, color: Color, size: androidx.compose.ui.unit.Dp = 30.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val stroke = w * 0.16f
        drawCircle(Color(0xFFE0E0E0), radius = (w - stroke) / 2f, style = Stroke(width = stroke))
        val f = fraction.coerceIn(0.0, 1.0).toFloat()
        if (f > 0.001f) {
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * f,
                useCenter = false,
                topLeft = Offset(stroke / 2f, stroke / 2f),
                size = Size(w - stroke, w - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun ListScreen(
    habits: MutableList<Habit>,
    state: LoopState,
    persist: () -> Unit,
    onOpenDetail: (String) -> Unit,
    onAdd: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val ctx = LocalContext.current
    val today = Logic.today()
    var showTypeChooser by remember { mutableStateOf(false) }
    var showFilter by remember { mutableStateOf(false) }
    var showOverflow by remember { mutableStateOf(false) }
    var colorTarget by remember { mutableStateOf<List<String>?>(null) }
    var deleteTarget by remember { mutableStateOf<List<String>?>(null) }
    var dayEdit by remember { mutableStateOf<Pair<Habit, LocalDate>?>(null) }

    val visible = remember(habits.toList(), state.hideArchived, state.hideCompleted, state.sortMode) {
        val base = habits.filter { !(state.hideArchived && it.archived) }
            .filter { !(state.hideCompleted && Logic.dayDone(it, today)) }
        when (state.sortMode) {
            "按名称" -> base.sortedBy { it.name }
            "按颜色" -> base.sortedBy { it.color }
            "按分数" -> base.sortedByDescending { Logic.score(it, today) }
            "按状态" -> base.sortedBy { !Logic.dayDone(it, today) }
            else -> base
        }
    }

    fun clearSelection() {
        state.selectionMode = false
        state.selected = emptySet()
    }

    Column(Modifier.fillMaxSize().background(PAGE_BG)) {
        // ---- top bar ----
        Row(
            Modifier.fillMaxWidth().background(BAR_BG).statusBarsPadding().height(58.dp).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state.selectionMode) {
                Box(Modifier.size(40.dp).clickable { clearSelection() }, contentAlignment = Alignment.Center) {
                    BackIcon(Color(0xFF444444))
                }
                Text("${state.selected.size}", color = BAR_TEXT, fontSize = 21.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(46.dp).clickable {
                    state.selected.firstOrNull()?.let { id ->
                        habits.firstOrNull { it.id == id }?.let { h ->
                            state.editIsNew = false
                            state.editDraft = h.deepCopy()
                            state.screen = Screen.EDIT
                        }
                    }
                }, contentAlignment = Alignment.Center) { PencilIcon(Color(0xFF444444)) }
                Box(Modifier.size(46.dp).clickable {
                    colorTarget = state.selected.toList()
                }, contentAlignment = Alignment.Center) { PaletteIcon(Color(0xFF444444)) }
                Box(Modifier.size(46.dp)) {
                    Box(Modifier.fillMaxSize().clickable { showOverflow = true }, contentAlignment = Alignment.Center) {
                        DotsIcon(Color(0xFF444444))
                    }
                    SelectionOverflowMenu(
                        expanded = showOverflow,
                        onDismiss = { showOverflow = false },
                        onArchive = {
                            showOverflow = false
                            habits.filter { state.selected.contains(it.id) }.forEach { it.archived = true }
                            persist(); clearSelection()
                        },
                        onDelete = {
                            showOverflow = false
                            deleteTarget = state.selected.toList()
                        }
                    )
                }
            } else {
                Text("习惯", color = BAR_TEXT, fontSize = 21.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(46.dp).clickable { showTypeChooser = true }, contentAlignment = Alignment.Center) {
                    PlusIcon(Color(0xFF444444))
                }
                Box(Modifier.size(46.dp)) {
                    Box(Modifier.fillMaxSize().clickable { showFilter = true }, contentAlignment = Alignment.Center) {
                        FunnelIcon(Color(0xFF444444))
                    }
                    FilterMenu(
                        expanded = showFilter,
                        onDismiss = { showFilter = false },
                        state = state
                    )
                }
                Box(Modifier.size(46.dp)) {
                    Box(Modifier.fillMaxSize().clickable { showOverflow = true }, contentAlignment = Alignment.Center) {
                        DotsIcon(Color(0xFF444444))
                    }
                    MainOverflowMenu(
                        expanded = showOverflow,
                        onDismiss = { showOverflow = false },
                        darkTheme = state.darkTheme,
                        onToggleDark = { state.darkTheme = !state.darkTheme },
                        onSettings = { showOverflow = false; onOpenSettings() }
                    )
                }
            }
        }

        // ---- date strip ----
        val dates = Logic.dateStrip(today, 5, state.reversed)
        Row(
            Modifier.fillMaxWidth().height(62.dp).background(PAGE_BG).padding(start = 132.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            dates.forEach { d ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(Logic.weekdayCn(d), color = Color(0xFF888888), fontSize = 12.sp)
                    Text("${d.dayOfMonth}", color = Color(0xFF777777), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFD8D8D8)))

        if (visible.isEmpty()) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                StarIcon(Color(0xFFB0B0B0), 150.dp)
                Spacer(Modifier.height(24.dp))
                Text("你还没有任何习惯", color = Color(0xFF9AA0A6), fontSize = 17.sp)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(visible, key = { it.id }) { habit ->
                    val selected = state.selected.contains(habit.id)
                    HabitRow(
                        habit = habit,
                        dates = dates,
                        selected = selected,
                        onOpen = {
                            if (state.selectionMode) {
                                state.selected = if (selected) state.selected - habit.id else state.selected + habit.id
                                if (state.selected.isEmpty()) state.selectionMode = false
                            } else onOpenDetail(habit.id)
                        },
                        onLongPressName = {
                            state.selectionMode = true
                            state.selected = state.selected + habit.id
                        },
                        onDayTap = { d ->
                            if (state.shortTap) {
                                quickToggle(ctx, habit, d)
                                persist()
                            } else {
                                dayEdit = habit to d
                            }
                        },
                        onDayLongPress = { d ->
                            quickToggle(ctx, habit, d)
                            persist()
                        }
                    )
                }
            }
        }
    }

    if (showTypeChooser) {
        HabitTypeChooser(
            onDismiss = { showTypeChooser = false },
            onPick = { quantified ->
                showTypeChooser = false
                state.editIsNew = true
                state.editDraft = Habit(
                    id = "hb_" + System.currentTimeMillis(),
                    type = if (quantified) HabitType.QUANTIFIED else HabitType.BOOLEAN,
                    color = HABIT_COLORS[2]
                )
                state.screen = Screen.EDIT
            }
        )
    }

    dayEdit?.let { (habit, date) ->
        DayCheckDialog(
            habit = habit,
            date = date,
            onDismiss = { dayEdit = null },
            onConfirmBoolean = { done, note ->
                if (done) habit.checkins[Logic.key(date)] = 1 else habit.checkins.remove(Logic.key(date))
                if (note.isNotBlank()) habit.dayNotes[Logic.key(date)] = note else habit.dayNotes.remove(Logic.key(date))
                persist(); dayEdit = null
            },
            onConfirmValue = { value, note ->
                habit.checkins[Logic.key(date)] = value
                if (note.isNotBlank()) habit.dayNotes[Logic.key(date)] = note
                persist(); dayEdit = null
            }
        )
    }

    colorTarget?.let { ids ->
        ColorPickerDialog(
            current = habits.firstOrNull { it.id == ids.firstOrNull() }?.color,
            onDismiss = { colorTarget = null },
            onPick = { hex ->
                habits.filter { ids.contains(it.id) }.forEach { it.color = hex }
                persist(); colorTarget = null; clearSelection()
            }
        )
    }

    deleteTarget?.let { ids ->
        ConfirmDialog(
            title = "删除习惯?",
            message = "习惯将被永久地删除。此操作无法撤销。",
            confirmLabel = "完成了",
            dismissLabel = "未完成",
            onDismiss = { deleteTarget = null },
            onConfirm = {
                habits.removeAll { ids.contains(it.id) }
                persist(); deleteTarget = null; clearSelection()
            }
        )
    }
}

private fun quickToggle(ctx: android.content.Context, habit: Habit, date: LocalDate) {
    if (habit.type == HabitType.BOOLEAN) {
        val k = Logic.key(date)
        if ((habit.checkins[k] ?: 0) > 0) habit.checkins.remove(k) else habit.checkins[k] = 1
    }
}

@Composable
fun HabitRow(
    habit: Habit,
    dates: List<LocalDate>,
    selected: Boolean,
    onOpen: () -> Unit,
    onLongPressName: () -> Unit,
    onDayTap: (LocalDate) -> Unit,
    onDayLongPress: (LocalDate) -> Unit
) {
    val color = parseColor(habit.color)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0x332196F3) else Color.White)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val fraction = Logic.score(habit, Logic.today())
        Row(
            Modifier
                .width(150.dp)
                .pointerInput(habit.id) {
                    detectTapGestures(
                        onTap = { onOpen() },
                        onLongPress = { onLongPressName() }
                    )
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            RingProgress(fraction, color, 30.dp)
            Spacer(Modifier.width(12.dp))
            Text(habit.name, color = color, fontSize = 18.sp, modifier = Modifier.width(96.dp))
        }
        dates.forEach { d ->
            DayCell(
                habit = habit,
                date = d,
                color = color,
                modifier = Modifier.weight(1f),
                onTap = { onDayTap(d) },
                onLongPress = { onDayLongPress(d) }
            )
        }
    }
}

@Composable
fun DayCell(
    habit: Habit,
    date: LocalDate,
    color: Color,
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val hasNote = !habit.dayNotes[Logic.key(date)].isNullOrBlank()
    Box(
        modifier
            .height(56.dp)
            .pointerInput(habit.id, date) {
                detectTapGestures(onTap = { onTap() }, onLongPress = { onLongPress() })
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.height(40.dp), contentAlignment = Alignment.Center) {
                when {
                    habit.type == HabitType.BOOLEAN && Logic.hasCheckin(habit, date) ->
                        CheckIcon(color, 30.dp)
                    habit.type == HabitType.BOOLEAN ->
                        CloseIcon(Color(0xFFC4C4C4), 24.dp)
                    else -> {
                        val v = habit.checkins[Logic.key(date)] ?: 0
                        val done = Logic.dayDone(habit, date)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${v}",
                                color = if (done) color else Color(0xFF9E9E9E),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(habit.unit.ifBlank { "" }, color = Color(0xFF9E9E9E), fontSize = 11.sp)
                        }
                    }
                }
            }
            if (hasNote) {
                Spacer(Modifier.height(2.dp))
                Box(Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(color))
            }
        }
    }
}

@Composable
fun HabitTypeChooser(onDismiss: () -> Unit, onPick: (Boolean) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            TypeCard("完成与否", "例如：你今天早起了吗？你锻炼了吗？你下棋了吗？") { onPick(false) }
            Spacer(Modifier.height(14.dp))
            TypeCard("可量化的", "例如：今天你跑了几公里？你读了几页书？") { onPick(true) }
        }
    }
}

@Composable
private fun TypeCard(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White)
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Text(title, color = Color(0xFF212121), fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = Color(0xFF555555), fontSize = 15.sp)
    }
}

@Composable
fun FilterMenu(expanded: Boolean, onDismiss: () -> Unit, state: LoopState) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Row(verticalAlignment = Alignment.CenterVertically) { Text("隐藏已存档"); Spacer(Modifier.weight(1f)); Checkbox(state.hideArchived, onCheckedChange = null) } },
            onClick = { state.hideArchived = !state.hideArchived }
        )
        DropdownMenuItem(
            text = { Row(verticalAlignment = Alignment.CenterVertically) { Text("隐藏已完成"); Spacer(Modifier.weight(1f)); Checkbox(state.hideCompleted, onCheckedChange = null) } },
            onClick = { state.hideCompleted = !state.hideCompleted }
        )
        var showSort by remember { mutableStateOf(false) }
        Box {
            DropdownMenuItem(
                text = { Row(verticalAlignment = Alignment.CenterVertically) { Text("排序"); Spacer(Modifier.weight(1f)); RightArrowIcon(Color(0xFF777777)) } },
                onClick = { showSort = true }
            )
            DropdownMenu(expanded = showSort, onDismissRequest = { showSort = false }) {
                listOf("手动", "按名称", "按颜色", "按分数", "按状态").forEach { mode ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (state.sortMode == mode) { UpArrowIcon(Color(0xFF333333)); Spacer(Modifier.width(8.dp)) }
                                Text(mode)
                            }
                        },
                        onClick = { state.sortMode = mode; showSort = false; onDismiss() }
                    )
                }
            }
        }
    }
}

@Composable
fun MainOverflowMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    darkTheme: Boolean,
    onToggleDark: () -> Unit,
    onSettings: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Row(verticalAlignment = Alignment.CenterVertically) { Text("深色主题"); Spacer(Modifier.weight(1f)); Checkbox(darkTheme, onCheckedChange = null) } },
            onClick = onToggleDark
        )
        DropdownMenuItem(text = { Text("设置") }, onClick = onSettings)
        DropdownMenuItem(text = { Text("帮助 & 常见问题") }, onClick = onDismiss)
        DropdownMenuItem(text = { Text("关于应用") }, onClick = onDismiss)
    }
}

@Composable
fun SelectionOverflowMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(text = { Text("存档") }, onClick = onArchive)
        DropdownMenuItem(text = { Text("删除") }, onClick = onDelete)
    }
}

@Composable
fun ColorPickerDialog(
    current: String?,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White)
                .padding(20.dp)
        ) {
            Text("选择颜色", color = Color(0xFF212121), fontSize = 20.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(18.dp))
            HABIT_COLORS.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    row.forEach { hex ->
                        val c = parseColor(hex)
                        Box(
                            Modifier
                                .size(52.dp)
                                .padding(6.dp)
                                .clip(RoundedCornerShape(26.dp))
                                .background(c)
                                .clickable { onPick(hex) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (hex == current) CheckIcon(Color.White, 26.dp)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White)
                .padding(24.dp)
        ) {
            Text(title, color = Color(0xFF212121), fontSize = 22.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(14.dp))
            Text(message, color = Color(0xFF444444), fontSize = 16.sp)
            Spacer(Modifier.height(22.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(dismissLabel, color = Color(0xFF555555), fontSize = 17.sp,
                    modifier = Modifier.clickable { onDismiss() }.padding(horizontal = 16.dp, vertical = 6.dp))
                Spacer(Modifier.width(12.dp))
                Text(confirmLabel, color = Color(0xFF3F51B5), fontSize = 17.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onConfirm() }.padding(horizontal = 16.dp, vertical = 6.dp))
            }
        }
    }
}

@Composable
fun DayCheckDialog(
    habit: Habit,
    date: LocalDate,
    onDismiss: () -> Unit,
    onConfirmBoolean: (Boolean, String) -> Unit,
    onConfirmValue: (Int, String) -> Unit
) {
    var text by remember {
        mutableStateOf(
            if (habit.type == HabitType.QUANTIFIED) {
                val v = habit.checkins[Logic.key(date)] ?: 0
                if (v > 0) v.toString() else ""
            } else (habit.dayNotes[Logic.key(date)] ?: "")
        )
    }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFFAFAFA))
        ) {
            Text("备注", color = Color(0xFF444444), fontSize = 18.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 26.dp))
            Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp)) {
                if (text.isEmpty()) Text(
                    if (habit.type == HabitType.QUANTIFIED) "0" else "备注",
                    color = Color(0xFFAAAAAA), fontSize = 17.sp
                )
                BasicTextField(
                    value = text,
                    onValueChange = { v ->
                        text = if (habit.type == HabitType.QUANTIFIED) v.filter { it.isDigit() } else v
                    },
                    singleLine = habit.type == HabitType.QUANTIFIED,
                    textStyle = TextStyle(color = Color(0xFF222222), fontSize = 17.sp),
                    cursorBrush = SolidColor(BLUE),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFDDDDDD)))
            if (habit.type == HabitType.QUANTIFIED) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f).clickable { onDismiss() }.padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center) { CloseIcon(Color(0xFF9E9E9E), 24.dp) }
                    Box(Modifier.width(1.dp).height(40.dp).background(Color(0xFFDDDDDD)))
                    Box(Modifier.weight(1f).clickable { onConfirmValue(text.toIntOrNull() ?: 0, "") }.padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center) {
                        Text("保存", color = Color(0xFF3F51B5), fontSize = 17.sp, fontWeight = FontWeight.Medium)
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth()) {
                    Box(Modifier.weight(1f).clickable { onConfirmBoolean(true, text) }.padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center) { CheckIcon(BLUE, 28.dp) }
                    Box(Modifier.width(1.dp).height(44.dp).background(Color(0xFFDDDDDD)))
                    Box(Modifier.weight(1f).clickable { onConfirmBoolean(false, text) }.padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center) { CloseIcon(Color(0xFF9E9E9E), 26.dp) }
                }
            }
        }
    }
}
