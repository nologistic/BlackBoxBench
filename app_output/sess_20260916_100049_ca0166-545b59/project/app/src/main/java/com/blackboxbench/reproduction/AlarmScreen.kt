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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDateTime
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun AlarmScreen(store: ClockStore, now: LocalDateTime, onOpenSettings: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    var pickerOpen by remember { mutableStateOf(false) }
    var expandedId by remember { mutableStateOf<Int?>(null) }
    var labelAlarmId by remember { mutableStateOf<Int?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Box {
            TopBar(title = "闹钟", onMore = { menuOpen = true })
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("屏保", color = ClockText) }, onClick = { menuOpen = false })
                DropdownMenuItem(text = { Text("设置", color = ClockText) }, onClick = { menuOpen = false; onOpenSettings() })
                DropdownMenuItem(text = { Text("隐私权政策", color = ClockText) }, onClick = { menuOpen = false })
                DropdownMenuItem(text = { Text("发送反馈", color = ClockText) }, onClick = { menuOpen = false })
                DropdownMenuItem(text = { Text("帮助", color = ClockText) }, onClick = { menuOpen = false })
            }
        }

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            val wake = store.alarms.filter { it.isWake }
            val others = store.alarms.filter { !it.isWake }
            if (wake.isNotEmpty()) {
                GroupHeader("起床")
                wake.forEach { AlarmCard(store, it, expandedId == it.id, {
                    expandedId = if (expandedId == it.id) null else it.id
                }, onOpenSound = { }, onEditLabel = { labelAlarmId = it.id }) }
            }
            if (others.isNotEmpty()) {
                GroupHeader("其他", withIcon = false)
                others.forEach { AlarmCard(store, it, expandedId == it.id, {
                    expandedId = if (expandedId == it.id) null else it.id
                }, onOpenSound = { }, onEditLabel = { labelAlarmId = it.id }) }
            }
            Spacer(Modifier.height(20.dp))
        }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            PlusButton(onClick = { pickerOpen = true })
        }
        Spacer(Modifier.height(24.dp))
    }

    if (pickerOpen) {
        TimePickerDialog(
            initialHour = now.hour,
            initialMinute = now.minute,
            onDismiss = { pickerOpen = false },
            onConfirm = { h, m ->
                val added = store.addAlarm(h, m)
                expandedId = added.id
                pickerOpen = false
            },
        )
    }

    labelAlarmId?.let { id ->
        val alarm = store.alarms.firstOrNull { it.id == id }
        if (alarm != null) {
            LabelDialog(
                initial = alarm.label,
                onDismiss = { labelAlarmId = null },
                onConfirm = { text -> store.updateAlarm(alarm.copy(label = text)); labelAlarmId = null },
            )
        }
    }
}

@Composable
private fun AlarmCard(
    store: ClockStore,
    alarm: Alarm,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onOpenSound: () -> Unit,
    onEditLabel: () -> Unit,
) {
    Card(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        color = if (alarm.enabled) ClockGroupHeader else ClockSurface,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(horizontal = 22.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (expanded && alarm.label.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TagGlyph(ClockTextDim, 20.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(alarm.label, color = ClockText, fontSize = 17.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(
                        hhmm(alarm.hour, alarm.minute),
                        color = if (alarm.enabled) ClockText else ClockTextDim,
                        fontSize = 62.sp,
                        fontWeight = FontWeight.Light,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (expanded) detailSubtitle(alarm) else Weekdays.summary(alarm.days),
                        color = ClockTextDim, fontSize = 17.sp,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier.size(34.dp).clickable { onToggleExpand() },
                        contentAlignment = Alignment.Center,
                    ) { ChevronGlyph(if (alarm.enabled) ClockText else ClockTextDim, 22.dp, down = !expanded) }
                    Spacer(Modifier.height(14.dp))
                    Toggle(alarm.enabled) { store.updateAlarm(alarm.copy(enabled = it)) }
                }
            }

            if (expanded) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    for (d in 1..7) {
                        val sel = d in alarm.days
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (sel) ClockAccent else ClockSurfaceHigh)
                                .clickable {
                                    val nd = alarm.days.toMutableSet()
                                    if (sel) nd.remove(d) else nd.add(d)
                                    store.updateAlarm(alarm.copy(days = nd))
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                Weekdays.glyph(d),
                                color = if (sel) Color(0xFF16233A) else ClockTextDim,
                                fontSize = 16.sp,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (alarm.days.isNotEmpty()) {
                    ListRow(
                        icon = { PauseCircleGlyph(ClockText, 24.dp) },
                        title = "暂停闹钟",
                        trailing = { PlusGlyph(ClockTextDim, 22.dp) },
                        onClick = {},
                    )
                } else {
                    ListRow(
                        icon = { CalendarGlyph(ClockText, 24.dp) },
                        title = "预定闹钟时间",
                        trailing = { PlusGlyph(ClockTextDim, 22.dp) },
                        onClick = {},
                    )
                }
                ListRow(
                    icon = { BellGlyph(ClockText, 24.dp) },
                    title = alarm.ringtone,
                    onClick = onOpenSound,
                )
                ListRow(
                    icon = { VibrateGlyph(ClockText, 24.dp) },
                    title = "振动",
                    trailing = { CheckCircle(alarm.vibrate) { store.updateAlarm(alarm.copy(vibrate = !alarm.vibrate)) } },
                )
                ListRow(
                    icon = { TrashGlyph(ClockText, 24.dp) },
                    title = "删除",
                    onClick = { store.deleteAlarm(alarm.id) },
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

private fun detailSubtitle(alarm: Alarm): String {
    if (alarm.days.isNotEmpty()) return Weekdays.summary(alarm.days)
    val now = LocalDateTime.now()
    val next = nextTrigger(alarm, now) ?: return "仅一次"
    val days = java.time.temporal.ChronoUnit.DAYS.between(now.toLocalDate(), next.toLocalDate())
    return if (days == 0L) "今天" else "明天"
}

@Composable
private fun LabelDialog(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    DialogShell(onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("标签", color = ClockAccent, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(ClockSurfaceHigh)
                    .padding(14.dp),
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    textStyle = TextStyle(color = ClockText, fontSize = 20.sp),
                    cursorBrush = SolidColor(ClockAccent),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlineButton("取消", onClick = onDismiss)
                Spacer(Modifier.width(8.dp))
                OutlineButton("确定", onClick = { onConfirm(text) })
            }
        }
    }
}

@Composable
fun DialogShell(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable(enabled = false) { }
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(ClockSurface)
                .padding(24.dp),
        ) { content() }
    }
}

@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    var hour by remember { mutableStateOf(initialHour) }
    var minute by remember { mutableStateOf(initialMinute) }
    var editingMinute by remember { mutableStateOf(false) }
    var keypad by remember { mutableStateOf(false) }

    DialogShell(onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("选择时间", color = ClockText, fontSize = 20.sp, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(width = 130.dp, height = 96.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (!editingMinute) ClockAccentDim else ClockSurfaceHigh)
                        .clickable { editingMinute = false; keypad = true },
                    contentAlignment = Alignment.Center,
                ) { Text(hour.two(), color = ClockText, fontSize = 58.sp) }
                Text(":", color = ClockText, fontSize = 48.sp, modifier = Modifier.padding(horizontal = 10.dp))
                Box(
                    modifier = Modifier
                        .size(width = 130.dp, height = 96.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (editingMinute) ClockAccentDim else ClockSurfaceHigh)
                        .clickable { editingMinute = true; keypad = true },
                    contentAlignment = Alignment.Center,
                ) { Text(minute.two(), color = ClockText, fontSize = 58.sp) }
            }

            Spacer(Modifier.height(16.dp))

            if (keypad) {
                Text(if (editingMinute) "分钟" else "小时", color = ClockTextDim, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                NumericKeypad(
                    onDigit = { d ->
                        if (editingMinute) {
                            minute = ((minute * 10) + d).coerceAtMost(59)
                        } else {
                            hour = ((hour * 10) + d).coerceAtMost(23)
                        }
                    },
                    onBackspace = {
                        if (editingMinute) minute /= 10 else hour /= 10
                    },
                )
            } else {
                Dial(
                    editingMinute = editingMinute,
                    hour = hour,
                    minute = minute,
                    onPick = { if (editingMinute) minute = it else hour = it },
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).clickable { keypad = !keypad },
                    contentAlignment = Alignment.Center,
                ) { ClockTabIcon(ClockText, 24.dp) }
                Spacer(Modifier.weight(1f))
                OutlineButton("取消", onClick = onDismiss)
                Spacer(Modifier.width(6.dp))
                OutlineButton("确定", onClick = { onConfirm(hour, minute) })
            }
        }
    }
}

@Composable
private fun Dial(editingMinute: Boolean, hour: Int, minute: Int, onPick: (Int) -> Unit) {
    val count = if (editingMinute) 12 else 24
    val step = if (editingMinute) 5 else 1
    val selectedIndex = if (editingMinute) minute / 5 else hour
    Box(
        modifier = Modifier
            .size(300.dp)
            .pointerInput(editingMinute) {
                detectTapGestures { offset ->
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val dx = offset.x - c.x
                    val dy = offset.y - c.y
                    val deg = (atan2(dy.toDouble(), dx.toDouble()) * 180.0 / PI) + 90.0
                    val norm = ((deg % 360.0) + 360.0) % 360.0
                    val idx = ((norm / (360.0 / count)).roundToInt()) % count
                    onPick(idx * step)
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val r = size.minDimension / 2f * 0.92f
            drawCircle(Color(0xFF2A2D34), radius = r, center = c)
            drawCircle(ClockAccentDim, radius = r * 0.16f, center = c)
            val lineAngle = (-90.0 + selectedIndex * (360.0 / count)) * PI / 180.0
            drawLine(
                Color(0xFF8FA6D8),
                c,
                Offset(c.x + (cos(lineAngle) * r * 0.7).toFloat(), c.y + (sin(lineAngle) * r * 0.7).toFloat()),
                strokeWidth = 5f,
            )
        }
        for (i in 0 until count) {
            val a = (-90.0 + i * (360.0 / count)) * PI / 180.0
            val rr = 0.74f
            Box(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = (i * step).two(),
                    color = if (i == selectedIndex) Color(0xFF16233A) else ClockText,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(
                            x = (cos(a) * 111).dp,
                            y = (sin(a) * 111).dp,
                        ),
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(
                    x = (cos((-90.0 + selectedIndex * (360.0 / count)) * PI / 180.0) * 111).dp,
                    y = (sin((-90.0 + selectedIndex * (360.0 / count)) * PI / 180.0) * 111).dp,
                ),
        ) { }
    }
}

@Composable
private fun NumericKeypad(onDigit: (Int) -> Unit, onBackspace: () -> Unit) {
    val rows = listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9))
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEach { d -> KeyCircle("$d") { onDigit(d) } }
            }
            Spacer(Modifier.height(14.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            KeyCircle("00") { onDigit(0); onDigit(0) }
            KeyCircle("0") { onDigit(0) }
            Box(
                modifier = Modifier.size(72.dp).clip(CircleShape).background(ClockSurfaceHigh).clickable { onBackspace() },
                contentAlignment = Alignment.Center,
            ) { BackspaceGlyph(ClockText, 26.dp) }
        }
    }
}

@Composable
private fun KeyCircle(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(72.dp).clip(CircleShape).background(ClockSurfaceHigh).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { Text(label, color = ClockText, fontSize = 24.sp) }
}
