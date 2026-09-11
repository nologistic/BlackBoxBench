package com.blackboxbench.reproduction

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = 0xFF18191F.toInt()
        window.navigationBarColor = 0xFF18191F.toInt()
        setContent { ReproducedApp() }
    }
}

data class AlarmEntry(
    val id: String,
    val hour: Int,
    val minute: Int,
    val label: String,
    val enabled: Boolean = true,
    val days: Set<Int> = emptySet(),
    val expanded: Boolean = false
)

private fun loadAlarms(prefs: SharedPreferences): List<AlarmEntry> {
    val raw = prefs.getString("custom_alarms", "") ?: ""
    if (raw.isBlank()) return emptyList()
    return raw.split(";;").mapNotNull { item ->
        val p = item.split("|")
        if (p.size < 6) null else {
            val days = p[5].split(",").mapNotNull { it.toIntOrNull() }.toSet()
            AlarmEntry(p[0], p[1].toIntOrNull() ?: 7, p[2].toIntOrNull() ?: 0, p[3], p[4] == "1", days)
        }
    }
}

private fun saveAlarms(prefs: SharedPreferences, alarms: List<AlarmEntry>) {
    val raw = alarms.joinToString(";;") { alarm ->
        listOf(
            alarm.id,
            alarm.hour.toString(),
            alarm.minute.toString(),
            alarm.label.replace("|", " "),
            if (alarm.enabled) "1" else "0",
            alarm.days.joinToString(",")
        ).joinToString("|")
    }
    prefs.edit().putString("custom_alarms", raw).apply()
}

@Composable
fun ReproducedApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("clock_prefs", Context.MODE_PRIVATE) }
    val alarms = remember { mutableStateListOf<AlarmEntry>().apply { addAll(loadAlarms(prefs)) } }
    var selectedTab by rememberSaveable { mutableIntStateOf(1) }
    var page by rememberSaveable { mutableStateOf("main") }
    var timerInput by rememberSaveable { mutableStateOf("") }
    var timerEnd by rememberSaveable { mutableLongStateOf(0L) }
    var timerPaused by rememberSaveable { mutableLongStateOf(0L) }
    var stopwatchRunning by rememberSaveable { mutableStateOf(false) }
    var stopwatchBase by rememberSaveable { mutableLongStateOf(0L) }
    var stopwatchStarted by rememberSaveable { mutableLongStateOf(0L) }
    val laps = remember { mutableStateListOf<Long>() }

    BenchmarkAppTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = AppBackground) {
            when (page) {
                "settings" -> SettingsScreen(
                    prefs = prefs,
                    onBack = { page = "main" },
                    onScreensaver = { page = "screensaver" }
                )
                "screensaver" -> ScreenSaver(onExit = { page = "main" })
                "search" -> CitySearchScreen(onBack = { page = "main" })
                else -> {
                    Scaffold(
                        containerColor = AppBackground,
                        bottomBar = {
                            BottomNavigation(selectedTab) { selectedTab = it }
                        }
                    ) { inset ->
                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(inset)
                        ) {
                            when (selectedTab) {
                                0 -> AlarmScreen(
                                    alarms = alarms,
                                    bedtimeConfigured = prefs.getBoolean("bedtime_configured", false),
                                    onAdd = {
                                        alarms.add(it)
                                        saveAlarms(prefs, alarms)
                                    },
                                    onUpdate = { index, alarm ->
                                        alarms[index] = alarm
                                        saveAlarms(prefs, alarms)
                                    },
                                    onDelete = { index ->
                                        alarms.removeAt(index)
                                        saveAlarms(prefs, alarms)
                                    },
                                    onSettings = { page = "settings" },
                                    onScreensaver = { page = "screensaver" }
                                )
                                1 -> ClockScreen(
                                    prefs = prefs,
                                    onSearch = { page = "search" },
                                    onSettings = { page = "settings" },
                                    onScreensaver = { page = "screensaver" }
                                )
                                2 -> TimerScreen(
                                    input = timerInput,
                                    endTime = timerEnd,
                                    pausedRemaining = timerPaused,
                                    onInput = { timerInput = it },
                                    onEndTime = { timerEnd = it },
                                    onPausedRemaining = { timerPaused = it },
                                    onSettings = { page = "settings" },
                                    onScreensaver = { page = "screensaver" }
                                )
                                3 -> StopwatchScreen(
                                    running = stopwatchRunning,
                                    baseElapsed = stopwatchBase,
                                    startedAt = stopwatchStarted,
                                    laps = laps,
                                    onRunning = { stopwatchRunning = it },
                                    onBase = { stopwatchBase = it },
                                    onStarted = { stopwatchStarted = it },
                                    onSettings = { page = "settings" },
                                    onScreensaver = { page = "screensaver" }
                                )
                                else -> BedtimeScreen(
                                    prefs = prefs,
                                    onSettings = { page = "settings" },
                                    onScreensaver = { page = "screensaver" }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomNavigation(selected: Int, onSelect: (Int) -> Unit) {
    val labels = listOf("闹钟", "时钟", "定时器", "秒表", "就寝时间")
    val icons = listOf("◴", "◷", "◇", "◎", "▬")
    NavigationBar(
        modifier = Modifier
            .height(112.dp)
            .navigationBarsPadding(),
        containerColor = NavBackground,
        tonalElevation = 0.dp
    ) {
        labels.forEachIndexed { index, label ->
            NavigationBarItem(
                selected = selected == index,
                onClick = { onSelect(index) },
                icon = {
                    Text(
                        icons[index],
                        fontSize = 29.sp,
                        color = if (selected == index) TextPrimary else TextSecondary
                    )
                },
                label = {
                    Text(
                        label,
                        fontSize = 13.sp,
                        fontWeight = if (selected == index) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = TextPrimary,
                    selectedTextColor = TextPrimary,
                    indicatorColor = Color(0xFF444A60),
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                )
            )
        }
    }
}

@Composable
private fun Header(
    title: String,
    onSettings: () -> Unit,
    onScreensaver: () -> Unit,
    showMenu: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 34.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        if (showMenu) {
            Box {
                Text(
                    "⋮",
                    fontSize = 40.sp,
                    color = TextSecondary,
                    modifier = Modifier
                        .padding(8.dp)
                        .clickable { expanded = true }
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(Color(0xFF42434B))
                ) {
                    DropdownMenuItem(
                        text = { Text("屏保", color = TextPrimary) },
                        onClick = { expanded = false; onScreensaver() }
                    )
                    DropdownMenuItem(
                        text = { Text("设置", color = TextPrimary) },
                        onClick = { expanded = false; onSettings() }
                    )
                    DropdownMenuItem(text = { Text("隐私权政策", color = TextPrimary) }, onClick = { expanded = false })
                    DropdownMenuItem(text = { Text("发送反馈", color = TextPrimary) }, onClick = { expanded = false })
                    DropdownMenuItem(text = { Text("帮助", color = TextPrimary) }, onClick = { expanded = false })
                }
            }
        }
    }
}

@Composable
private fun AddButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(PrimaryBlue)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text("+", fontSize = 56.sp, fontWeight = FontWeight.Light, color = DeepBlue)
    }
}

@Composable
private fun AlarmScreen(
    alarms: List<AlarmEntry>,
    bedtimeConfigured: Boolean,
    onAdd: (AlarmEntry) -> Unit,
    onUpdate: (Int, AlarmEntry) -> Unit,
    onDelete: (Int) -> Unit,
    onSettings: () -> Unit,
    onScreensaver: () -> Unit
) {
    var addDialog by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Header("闹钟", onSettings, onScreensaver)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 18.dp, end = 18.dp, top = 6.dp, bottom = 128.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (bedtimeConfigured) {
                    item {
                        Text("☀  起床", fontSize = 22.sp, color = TextPrimary, modifier = Modifier.padding(10.dp))
                        StaticAlarmCard("07:15", "每天", true)
                    }
                }
                item { Text("其他", fontSize = 22.sp, color = TextPrimary, modifier = Modifier.padding(10.dp)) }
                item { StaticAlarmCard("08:30", "周一、周二、周三、周四、周五", false) }
                item { StaticAlarmCard("09:00", "周六、周日", false) }
                itemsIndexed(alarms, key = { _, alarm -> alarm.id }) { index, alarm ->
                    CustomAlarmCard(
                        alarm = alarm,
                        onChange = { onUpdate(index, it) },
                        onDelete = { onDelete(index) }
                    )
                }
            }
        }
        AddButton(
            onClick = { addDialog = true },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp)
        )
    }
    if (addDialog) {
        AddAlarmDialog(
            onDismiss = { addDialog = false },
            onConfirm = { h, m ->
                onAdd(
                    AlarmEntry(
                        id = System.currentTimeMillis().toString(),
                        hour = h,
                        minute = m,
                        label = "",
                        enabled = true,
                        expanded = true
                    )
                )
                addDialog = false
            }
        )
    }
}

@Composable
private fun StaticAlarmCard(time: String, summary: String, enabled: Boolean) {
    var on by remember { mutableStateOf(enabled) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(time, fontSize = 50.sp, color = if (on) TextPrimary else Color(0xFF929292), fontWeight = FontWeight.Light)
                Spacer(Modifier.height(10.dp))
                Text(summary, fontSize = 18.sp, color = if (on) TextPrimary else Color(0xFF929292))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("⌄", fontSize = 30.sp, color = TextSecondary)
                Spacer(Modifier.height(54.dp))
                Switch(
                    checked = on,
                    onCheckedChange = { on = it },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = Color(0xFF38558F),
                        checkedThumbColor = Color(0xFFD6DFFF)
                    )
                )
            }
        }
    }
}

@Composable
private fun CustomAlarmCard(
    alarm: AlarmEntry,
    onChange: (AlarmEntry) -> Unit,
    onDelete: () -> Unit
) {
    var editLabel by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    Modifier
                        .weight(1f)
                        .clickable { editLabel = true }
                ) {
                    if (alarm.label.isNotBlank()) Text(alarm.label, fontSize = 18.sp, color = TextPrimary)
                    else Text("◇  添加标签", fontSize = 18.sp, color = TextSecondary)
                    Text(
                        two(alarm.hour) + ":" + two(alarm.minute),
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Light,
                        color = if (alarm.enabled) TextPrimary else Color(0xFF929292)
                    )
                    Text(daySummary(alarm.days), fontSize = 18.sp, color = TextPrimary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        if (alarm.expanded) "⌃" else "⌄",
                        fontSize = 30.sp,
                        color = TextSecondary,
                        modifier = Modifier.clickable { onChange(alarm.copy(expanded = !alarm.expanded)) }
                    )
                    Spacer(Modifier.height(34.dp))
                    Switch(
                        checked = alarm.enabled,
                        onCheckedChange = { onChange(alarm.copy(enabled = it)) },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = Color(0xFF38558F),
                            checkedThumbColor = Color(0xFFD6DFFF)
                        )
                    )
                }
            }
            if (alarm.expanded) {
                Spacer(Modifier.height(18.dp))
                DayChips(alarm.days) { day ->
                    val next = alarm.days.toMutableSet()
                    if (!next.add(day)) next.remove(day)
                    onChange(alarm.copy(days = next))
                }
                Spacer(Modifier.height(22.dp))
                SettingLine("▣", "预定闹钟时间", "+")
                SettingLine("♬", "默认铃声 (Cesium)", "")
                var vibrate by remember { mutableStateOf(true) }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("▥", fontSize = 24.sp, color = TextSecondary, modifier = Modifier.width(44.dp))
                    Text("振动", fontSize = 19.sp, modifier = Modifier.weight(1f))
                    Switch(checked = vibrate, onCheckedChange = { vibrate = it })
                }
                TextButton(onClick = { onChange(alarm.copy(enabled = false)) }) {
                    Text("关闭", fontSize = 18.sp, color = PrimaryBlue)
                }
                TextButton(onClick = onDelete) {
                    Text("▣   删除", fontSize = 18.sp, color = TextPrimary)
                }
            }
        }
    }
    if (editLabel) {
        var text by remember(alarm.label) { mutableStateOf(alarm.label) }
        AlertDialog(
            onDismissRequest = { editLabel = false },
            title = { Text("标签") },
            text = {
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { onChange(alarm.copy(label = text)); editLabel = false }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { editLabel = false }) { Text("取消") } },
            containerColor = Color(0xFF30323D)
        )
    }
}

@Composable
private fun AddAlarmDialog(onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit) {
    val now = remember { ZonedDateTime.now() }
    var hour by remember { mutableIntStateOf((now.hour + 1) % 24) }
    var minute by remember { mutableIntStateOf(0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择时间", color = TextPrimary) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TimeSegment(two(hour), true) { hour = (hour + 1) % 24 }
                    Text(":", fontSize = 48.sp, color = TextPrimary)
                    TimeSegment(two(minute), false) { minute = (minute + 5) % 60 }
                }
                Spacer(Modifier.height(20.dp))
                Box(
                    Modifier
                        .size(250.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF51515A)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val center = this.center
                        val radius = size.minDimension * .39f
                        repeat(12) { i ->
                            val angle = (i * 30f - 90f) * (PI / 180f)
                            val p = Offset(
                                center.x + cos(angle).toFloat() * radius,
                                center.y + sin(angle).toFloat() * radius
                            )
                            drawCircle(
                                if (i == hour % 12) PrimaryBlue else Color.Transparent,
                                radius = if (i == hour % 12) 23.dp.toPx() else 0f,
                                center = p
                            )
                        }
                        val angle = ((hour % 12) * 30f)
                        rotate(angle, pivot = center) {
                            drawLine(PrimaryBlue, center, Offset(center.x, center.y - radius), 3.dp.toPx())
                        }
                    }
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceAround) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Text("11"); Text("00"); Text("1")
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("9"); Text("3")
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Text("7"); Text("6"); Text("5")
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(hour, minute) }) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        containerColor = Color(0xFF30323D)
    )
}

@Composable
private fun TimeSegment(value: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(width = 104.dp, height = 82.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color(0xFF38558F) else Color(0xFF50515A))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(value, fontSize = 46.sp, color = TextPrimary)
    }
}

@Composable
private fun DayChips(days: Set<Int>, onClick: (Int) -> Unit) {
    val names = listOf("一", "二", "三", "四", "五", "六", "日")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        names.forEachIndexed { index, name ->
            val selected = days.contains(index + 1)
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (selected) PrimaryBlue else Color.Transparent)
                    .border(1.dp, Outline, CircleShape)
                    .clickable { onClick(index + 1) },
                contentAlignment = Alignment.Center
            ) {
                Text(name, color = if (selected) DeepBlue else TextSecondary, fontSize = 16.sp)
            }
        }
    }
}

private fun daySummary(days: Set<Int>): String {
    if (days.isEmpty()) return "今天"
    if (days.size == 7) return "每天"
    if (days == setOf(1, 2, 3, 4, 5)) return "周一至周五"
    if (days == setOf(6, 7)) return "周六、周日"
    val names = listOf("", "周一", "周二", "周三", "周四", "周五", "周六", "周日")
    return days.sorted().joinToString("、") { names[it] }
}

@Composable
private fun SettingLine(icon: String, title: String, trailing: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(58.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 22.sp, color = TextSecondary, modifier = Modifier.width(44.dp))
        Text(title, fontSize = 19.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        Text(trailing, fontSize = 30.sp, color = TextSecondary)
    }
}

@Composable
private fun ClockScreen(
    prefs: SharedPreferences,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onScreensaver: () -> Unit
) {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(500)
        }
    }
    val now = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), ZoneId.systemDefault())
    val analog = prefs.getBoolean("analog_clock", false)
    val showSeconds = prefs.getBoolean("show_seconds", false)
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Header("时钟", onSettings, onScreensaver)
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(45.dp))
                if (analog) {
                    AnalogClock(now, Modifier.size(270.dp))
                } else {
                    Text(
                        now.format(DateTimeFormatter.ofPattern(if (showSeconds) "HH:mm:ss" else "HH:mm")),
                        fontSize = if (showSeconds) 72.sp else 92.sp,
                        fontWeight = FontWeight.Light,
                        color = TextPrimary
                    )
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    now.format(DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA)) +
                        if (prefs.getBoolean("bedtime_configured", false)) "  ◴ 周二07:15" else "",
                    fontSize = 21.sp,
                    color = TextPrimary
                )
            }
        }
        AddButton(
            onClick = onSearch,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp)
        )
    }
}

@Composable
private fun AnalogClock(now: ZonedDateTime, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val r = size.minDimension / 2f
        drawCircle(Color(0xFFF0F1FF), r * .93f, center)
        val minute = now.minute + now.second / 60f
        val hour = (now.hour % 12) + minute / 60f
        rotate(hour * 30f, center) {
            drawLine(Color(0xFF45495B), center, Offset(center.x, center.y - r * .40f), 18.dp.toPx(), StrokeCap.Round)
        }
        rotate(minute * 6f, center) {
            drawLine(Color(0xFF6E83BD), center, Offset(center.x, center.y - r * .63f), 16.dp.toPx(), StrokeCap.Round)
        }
        val secA = now.second * 6f * PI / 180f
        drawCircle(
            Color(0xFF9B7695),
            r * .10f,
            Offset(
                center.x + sin(secA).toFloat() * r * .72f,
                center.y - cos(secA).toFloat() * r * .72f
            )
        )
    }
}

@Composable
private fun CitySearchScreen(onBack: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    Column(
        Modifier
            .fillMaxSize()
            .background(AppBackground)
            .statusBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(84.dp)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("‹", fontSize = 54.sp, color = TextPrimary, modifier = Modifier.clickable(onClick = onBack))
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
                textStyle = TextStyle(color = TextPrimary, fontSize = 24.sp),
                singleLine = true,
                decorationBox = { inner ->
                    if (query.isEmpty()) Text("搜索城市", fontSize = 24.sp, color = TextSecondary)
                    inner()
                }
            )
            if (query.isNotEmpty()) Text("×", fontSize = 42.sp, modifier = Modifier.clickable { query = "" })
        }
        HorizontalDivider(color = TextSecondary)
        if (query.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(top = 170.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("⌕", fontSize = 94.sp, color = Color(0xFF9B9B9B))
                Text("搜索城市", fontSize = 24.sp, color = Color(0xFF9B9B9B))
            }
        } else {
            Column(Modifier.padding(48.dp)) {
                Text("◒", fontSize = 42.sp, color = TextPrimary)
                Spacer(Modifier.height(28.dp))
                Text("联网即可查看更多城市", fontSize = 30.sp, color = TextPrimary)
                Spacer(Modifier.height(20.dp))
                Text(
                    "要查看所有适用的城市，请连接到互联网。否则您只能看到有限的城市列表。",
                    fontSize = 20.sp,
                    lineHeight = 31.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

private fun timerSeconds(input: String): Long {
    if (input.isBlank()) return 0L
    val padded = input.takeLast(6).padStart(6, '0')
    val h = padded.substring(0, 2).toLongOrNull() ?: 0L
    val m = padded.substring(2, 4).toLongOrNull() ?: 0L
    val s = padded.substring(4, 6).toLongOrNull() ?: 0L
    return h * 3600 + m * 60 + s
}

private fun timerDisplay(input: String): Triple<String, String, String> {
    val p = input.takeLast(6).padStart(6, '0')
    return Triple(p.substring(0, 2), p.substring(2, 4), p.substring(4, 6))
}

@Composable
private fun TimerScreen(
    input: String,
    endTime: Long,
    pausedRemaining: Long,
    onInput: (String) -> Unit,
    onEndTime: (Long) -> Unit,
    onPausedRemaining: (Long) -> Unit,
    onSettings: () -> Unit,
    onScreensaver: () -> Unit
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(endTime) {
        while (endTime > 0) {
            now = System.currentTimeMillis()
            delay(250)
        }
    }
    val active = endTime > 0 || pausedRemaining > 0
    val remaining = if (endTime > 0) ((endTime - now) / 1000L) else pausedRemaining / 1000L
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Header("定时器", onSettings, onScreensaver)
            if (!active) {
                TimerKeypad(input, onInput)
            } else {
                TimerCard(
                    original = timerSeconds(input),
                    remaining = remaining,
                    running = endTime > 0,
                    onToggle = {
                        if (endTime > 0) {
                            onPausedRemaining((endTime - System.currentTimeMillis()).coerceAtLeast(1L))
                            onEndTime(0L)
                        } else {
                            onEndTime(System.currentTimeMillis() + pausedRemaining)
                            onPausedRemaining(0L)
                        }
                    },
                    onAddMinute = {
                        if (endTime > 0) onEndTime(endTime + 60000L)
                        else onPausedRemaining(pausedRemaining + 60000L)
                    },
                    onStop = {
                        onEndTime(0L)
                        onPausedRemaining(0L)
                    }
                )
            }
        }
        if (!active && timerSeconds(input) > 0) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 18.dp)
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue)
                    .clickable {
                        onEndTime(System.currentTimeMillis() + timerSeconds(input) * 1000L)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("▶", fontSize = 34.sp, color = DeepBlue)
            }
        } else if (active) {
            AddButton(
                onClick = {
                    onEndTime(0L)
                    onPausedRemaining(0L)
                    onInput("")
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 18.dp)
            )
        }
    }
}

@Composable
private fun TimerKeypad(input: String, onInput: (String) -> Unit) {
    val (h, m, s) = timerDisplay(input)
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 46.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(54.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            TimeDigits(h, "时", input.length > 4)
            TimeDigits(m, "分", input.length in 3..4)
            TimeDigits(s, "秒", input.isNotEmpty())
        }
        Spacer(Modifier.height(48.dp))
        val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("00", "0", "⌫"))
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { key ->
                    Box(
                        Modifier
                            .padding(5.dp)
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(if (key == "⌫") Color(0xFF4A5062) else CardBackground)
                            .clickable {
                                when (key) {
                                    "⌫" -> onInput(input.dropLast(1))
                                    else -> if (input.length < 6) onInput((input + key).takeLast(6))
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(key, fontSize = 35.sp, color = TextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeDigits(value: String, suffix: String, active: Boolean) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(value, fontSize = 50.sp, fontWeight = FontWeight.Light, color = if (active) PrimaryBlue else Color(0xFF939393))
        Text(suffix, fontSize = 23.sp, color = if (active) PrimaryBlue else Color(0xFF939393), modifier = Modifier.padding(bottom = 8.dp))
        Spacer(Modifier.width(6.dp))
    }
}

@Composable
private fun TimerCard(
    original: Long,
    remaining: Long,
    running: Boolean,
    onToggle: () -> Unit,
    onAddMinute: () -> Unit,
    onStop: () -> Unit
) {
    val expired = remaining < 0
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = if (expired) Color(0xFF8499D0) else PrimaryBlue)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(formatTimerName(original), fontSize = 28.sp, color = DeepBlue, modifier = Modifier.weight(1f))
                Text("×", fontSize = 34.sp, color = DeepBlue, modifier = Modifier.clickable(onClick = onStop))
            }
            Spacer(Modifier.height(26.dp))
            Box(Modifier.size(300.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(Color(0xFF414655), style = Stroke(10.dp.toPx()))
                    val frac = if (original <= 0) 0f else (remaining.coerceAtLeast(0).toFloat() / original.toFloat()).coerceIn(0f, 1f)
                    drawArc(
                        DeepBlue,
                        -90f,
                        frac * 360f,
                        false,
                        style = Stroke(10.dp.toPx(), cap = StrokeCap.Round),
                        size = Size(size.width, size.height)
                    )
                }
                Text(
                    if (expired) remaining.toString() else formatDuration(remaining),
                    fontSize = if (expired) 74.sp else 58.sp,
                    color = DeepBlue,
                    fontWeight = FontWeight.Light
                )
            }
            Spacer(Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onAddMinute,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF464B60), contentColor = TextPrimary),
                    modifier = Modifier.weight(1f).height(70.dp)
                ) { Text("+1:00", fontSize = 22.sp) }
                Button(
                    onClick = if (expired) onStop else onToggle,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDCE4FF), contentColor = Color(0xFF5A264F)),
                    modifier = Modifier.weight(1f).height(70.dp)
                ) { Text(if (expired) "■" else if (running) "Ⅱ" else "▶", fontSize = 26.sp) }
            }
        }
    }
}

private fun formatTimerName(seconds: Long): String {
    return when {
        seconds < 60 -> seconds.toString() + "秒的定时器"
        seconds % 60 == 0L -> (seconds / 60).toString() + "分钟的定时器"
        else -> formatDuration(seconds) + " 的定时器"
    }
}

private fun formatDuration(seconds: Long): String {
    val s = seconds.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) two(h.toInt()) + ":" + two(m.toInt()) + ":" + two(sec.toInt())
    else two(m.toInt()) + ":" + two(sec.toInt())
}

@Composable
private fun StopwatchScreen(
    running: Boolean,
    baseElapsed: Long,
    startedAt: Long,
    laps: MutableList<Long>,
    onRunning: (Boolean) -> Unit,
    onBase: (Long) -> Unit,
    onStarted: (Long) -> Unit,
    onSettings: () -> Unit,
    onScreensaver: () -> Unit
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(running) {
        while (running) {
            now = System.currentTimeMillis()
            delay(20)
        }
    }
    val elapsed = baseElapsed + if (running) now - startedAt else 0L
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Header("秒表", onSettings, onScreensaver)
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(80.dp))
                Box(Modifier.size(310.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawCircle(Color(0xFF4C4D57), style = Stroke(10.dp.toPx()))
                        if (elapsed > 0) {
                            drawArc(
                                Color(0xFF839DE0),
                                -90f,
                                ((elapsed % 60000L).toFloat() / 60000f) * 360f,
                                false,
                                style = Stroke(10.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text((elapsed / 1000L).toString(), fontSize = 72.sp, color = TextPrimary, fontWeight = FontWeight.Light)
                        Text(two(((elapsed % 1000L) / 10L).toInt()), fontSize = 48.sp, color = TextPrimary)
                    }
                }
                if (laps.isNotEmpty()) {
                    Spacer(Modifier.height(22.dp))
                    laps.takeLast(4).reversed().forEachIndexed { index, lap ->
                        Text(
                            "# " + (laps.size - index) + "   " + formatStopwatch(lap) + "      " + formatStopwatch(lap),
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
        if (!running && elapsed == 0L) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 18.dp)
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue)
                    .clickable {
                        onStarted(System.currentTimeMillis())
                        onRunning(true)
                    },
                contentAlignment = Alignment.Center
            ) { Text("▶", fontSize = 34.sp, color = DeepBlue) }
        } else {
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 38.dp, vertical = 22.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundAction("↻", Plum) {
                    onRunning(false)
                    onBase(0L)
                    laps.clear()
                }
                Box(
                    Modifier
                        .size(width = 132.dp, height = 80.dp)
                        .clip(RoundedCornerShape(34.dp))
                        .background(PrimaryBlue)
                        .clickable {
                            if (running) {
                                onBase(elapsed)
                                onRunning(false)
                            } else {
                                onStarted(System.currentTimeMillis())
                                onRunning(true)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) { Text(if (running) "Ⅱ" else "▶", fontSize = 34.sp, color = DeepBlue) }
                RoundAction("◴", Plum) {
                    if (elapsed > 0) laps.add(elapsed)
                }
            }
        }
    }
}

@Composable
private fun RoundAction(text: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, fontSize = 30.sp, color = TextPrimary) }
}

private fun formatStopwatch(ms: Long): String {
    val sec = ms / 1000L
    return (sec / 60L).toString() + " " + two((sec % 60L).toInt()) + "." + two(((ms % 1000L) / 10L).toInt())
}

@Composable
private fun BedtimeScreen(
    prefs: SharedPreferences,
    onSettings: () -> Unit,
    onScreensaver: () -> Unit
) {
    var configured by remember { mutableStateOf(prefs.getBoolean("bedtime_configured", false)) }
    var wizard by rememberSaveable { mutableIntStateOf(0) }
    var wakeMinutes by rememberSaveable { mutableIntStateOf(7 * 60) }
    var bedMinutes by rememberSaveable { mutableIntStateOf(23 * 60) }
    var player by rememberSaveable { mutableStateOf(false) }

    if (player) {
        SleepPlayer(onClose = { player = false })
        return
    }
    Column(Modifier.fillMaxSize()) {
        Header("就寝时间", onSettings, onScreensaver)
        when {
            !configured && wizard == 0 -> BedtimeIntro { wizard = 1 }
            !configured -> BedtimeWizard(
                step = wizard,
                wakeMinutes = wakeMinutes,
                bedMinutes = bedMinutes,
                onWake = { wakeMinutes = it },
                onBed = { bedMinutes = it },
                onNext = {
                    if (wizard == 1) wizard = 2 else {
                        prefs.edit()
                            .putBoolean("bedtime_configured", true)
                            .putInt("wake_minutes", wakeMinutes)
                            .putInt("bed_minutes", bedMinutes)
                            .apply()
                        configured = true
                        wizard = 0
                    }
                }
            )
            else -> BedtimeHome(
                wakeMinutes = prefs.getInt("wake_minutes", 7 * 60 + 15),
                bedMinutes = prefs.getInt("bed_minutes", 23 * 60),
                onPlay = { player = true }
            )
        }
    }
}

@Composable
private fun BedtimeIntro(onStart: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(36.dp))
        Text("设置规律的就寝时间，改善睡眠质量", fontSize = 25.sp, textAlign = TextAlign.Center, lineHeight = 31.sp)
        Spacer(Modifier.height(38.dp))
        Text(
            "选择规律的就寝时间、放下设备并聆听舒缓的音效",
            fontSize = 18.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 27.sp
        )
        Spacer(Modifier.height(28.dp))
        Box(
            Modifier
                .size(width = 280.dp, height = 195.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Color(0xFFFFE5A7), 28.dp.toPx(), Offset(size.width * .78f, size.height * .2f))
                drawRoundRect(
                    Color(0xFF202126),
                    topLeft = Offset(size.width * .2f, size.height * .58f),
                    size = Size(size.width * .62f, size.height * .26f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx())
                )
                drawLine(Color(0xFF9698A0), Offset(size.width * .17f, size.height * .84f), Offset(size.width * .86f, size.height * .84f), 5.dp.toPx())
            }
            Text("✦       ☾", fontSize = 38.sp, color = TextPrimary)
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onStart,
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, contentColor = DeepBlue),
            modifier = Modifier
                .padding(bottom = 28.dp)
                .height(64.dp)
        ) { Text("开始使用", fontSize = 20.sp, modifier = Modifier.padding(horizontal = 20.dp)) }
    }
}

@Composable
private fun BedtimeWizard(
    step: Int,
    wakeMinutes: Int,
    bedMinutes: Int,
    onWake: (Int) -> Unit,
    onBed: (Int) -> Unit,
    onNext: () -> Unit
) {
    val value = if (step == 1) wakeMinutes else bedMinutes
    val title = if (step == 1) "设置起床时间" else "设置就寝时间，让设备按时静音"
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(if (step == 1) "◴" else "▰", fontSize = 38.sp)
        Spacer(Modifier.height(22.dp))
        Text(title, fontSize = 30.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(30.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(30.dp)
        ) {
            Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RoundAction("−", Color(0xFF4E505A)) {
                        if (step == 1) onWake((wakeMinutes - 15 + 1440) % 1440)
                        else onBed((bedMinutes - 15 + 1440) % 1440)
                    }
                    Text(
                        timeOfDay(value),
                        fontSize = 55.sp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 18.dp),
                        textAlign = TextAlign.Center
                    )
                    RoundAction("+", Color(0xFF4E505A)) {
                        if (step == 1) onWake((wakeMinutes + 15) % 1440)
                        else onBed((bedMinutes + 15) % 1440)
                    }
                }
                Spacer(Modifier.height(20.dp))
                if (step == 2) Text("8 小时 15 分钟", fontSize = 21.sp, color = TextSecondary)
                Spacer(Modifier.height(24.dp))
                DayChips((1..7).toSet()) {}
                Spacer(Modifier.height(22.dp))
                HorizontalDivider(color = AppBackground, thickness = 8.dp)
                SettingLine("♧", if (step == 1) "日出闹钟" else "提醒通知", if (step == 1) "" else "就寝时间前 15 分钟")
                SettingLine("☾", if (step == 1) "闹钟声音与振动" else "就寝模式", "")
            }
        }
        Spacer(Modifier.weight(1f))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onNext, shape = RoundedCornerShape(26.dp)) { Text("跳过") }
            Button(
                onClick = onNext,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, contentColor = DeepBlue)
            ) { Text(if (step == 1) "下一步" else "完成", modifier = Modifier.padding(horizontal = 18.dp)) }
        }
    }
}

@Composable
private fun BedtimeHome(wakeMinutes: Int, bedMinutes: Int, onPlay: () -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(30.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(28.dp)) {
                    Text("◴   时间表", fontSize = 22.sp)
                    Spacer(Modifier.height(28.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("就寝时间", color = TextSecondary)
                            Text(timeOfDay(bedMinutes), fontSize = 48.sp, fontWeight = FontWeight.Light)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("起床时间", color = TextSecondary)
                            Text(timeOfDay(wakeMinutes), fontSize = 48.sp, fontWeight = FontWeight.Light)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Text("8 小时 15 分钟 • 下次闹钟时间：星期二", fontSize = 18.sp, color = TextSecondary)
                }
            }
        }
        item {
            BedCard(
                title = "查看近期就寝时间内的活动数据",
                body = "跟踪设备使用时间，查看估算的睡眠时长。",
                action = "不用了"
            )
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(30.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onPlay)
            ) {
                Column(Modifier.padding(28.dp)) {
                    Text("♫   聆听助眠音效", fontSize = 22.sp)
                    Spacer(Modifier.height(20.dp))
                    Text("您可以播放舒缓的音乐，帮助自己安然入睡。", fontSize = 18.sp, color = TextSecondary)
                    Spacer(Modifier.height(22.dp))
                    Button(
                        onClick = onPlay,
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, contentColor = DeepBlue)
                    ) { Text("选择音效") }
                }
            }
        }
    }
}

@Composable
private fun BedCard(title: String, body: String, action: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(30.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(28.dp)) {
            Text("▥   " + title, fontSize = 22.sp)
            Spacer(Modifier.height(18.dp))
            Text(body, fontSize = 18.sp, color = TextSecondary, lineHeight = 28.sp)
            TextButton(onClick = {}, modifier = Modifier.align(Alignment.End)) { Text(action) }
        }
    }
}

@Composable
private fun SleepPlayer(onClose: () -> Unit) {
    var playing by remember { mutableStateOf(false) }
    var stopIn by remember { mutableStateOf("30 分钟") }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stars = listOf(.08f to .12f, .22f to .20f, .72f to .11f, .84f to .34f, .16f to .55f, .63f to .48f, .92f to .74f, .31f to .82f, .70f to .89f)
            stars.forEachIndexed { i, p ->
                drawCircle(Color(0xFFBCC8EF), if (i % 2 == 0) 2.2f else 1.2f, Offset(size.width * p.first, size.height * p.second))
            }
        }
        Text("×", fontSize = 54.sp, color = TextPrimary, modifier = Modifier.align(Alignment.TopStart).padding(28.dp).clickable(onClick = onClose))
        Text("深邃的太空", fontSize = 34.sp, color = TextPrimary, modifier = Modifier.align(Alignment.TopCenter).padding(top = 150.dp))
        Box(
            Modifier
                .size(86.dp)
                .clip(CircleShape)
                .background(PrimaryBlue)
                .clickable { playing = !playing },
            contentAlignment = Alignment.Center
        ) { Text(if (playing) "Ⅱ" else "▶", fontSize = 30.sp, color = DeepBlue) }
        var expanded by remember { mutableStateOf(false) }
        Box(Modifier.align(Alignment.Center).padding(top = 260.dp)) {
            Button(
                onClick = { expanded = true },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryContainer, contentColor = TextPrimary)
            ) { Text("◴  在" + stopIn + "后停止  ▾") }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                listOf("10 分钟", "20 分钟", "30 分钟", "40 分钟", "50 分钟", "1 小时").forEach {
                    DropdownMenuItem(text = { Text(it) }, onClick = { stopIn = it; expanded = false })
                }
            }
        }
        Text("选择其他音效", fontSize = 20.sp, color = PrimaryBlue, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 160.dp))
    }
}

@Composable
private fun SettingsScreen(
    prefs: SharedPreferences,
    onBack: () -> Unit,
    onScreensaver: () -> Unit
) {
    var analog by remember { mutableStateOf(prefs.getBoolean("analog_clock", false)) }
    var seconds by remember { mutableStateOf(prefs.getBoolean("show_seconds", false)) }
    var home by remember { mutableStateOf(true) }
    var styleMenu by remember { mutableStateOf(false) }
    var durationDialog by remember { mutableStateOf(false) }
    var snoozeDialog by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .background(AppBackground)
            .statusBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(88.dp)
                .padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("‹", fontSize = 56.sp, modifier = Modifier.clickable(onClick = onBack))
            Spacer(Modifier.weight(1f))
            Text("⋮", fontSize = 38.sp)
        }
        Text("设置", fontSize = 48.sp, modifier = Modifier.padding(horizontal = 28.dp, vertical = 28.dp))
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
        ) {
            SectionLabel("时钟")
            Box {
                SettingsRow("样式", if (analog) "指针" else "数字") { styleMenu = true }
                DropdownMenu(expanded = styleMenu, onDismissRequest = { styleMenu = false }) {
                    DropdownMenuItem(text = { Text("数字") }, onClick = {
                        analog = false
                        prefs.edit().putBoolean("analog_clock", false).apply()
                        styleMenu = false
                    })
                    DropdownMenuItem(text = { Text("指针") }, onClick = {
                        analog = true
                        prefs.edit().putBoolean("analog_clock", true).apply()
                        styleMenu = false
                    })
                }
            }
            SwitchRow("显示含秒数的时间", seconds) {
                seconds = it
                prefs.edit().putBoolean("show_seconds", it).apply()
            }
            SwitchRow("自动显示家所在地点时间", home) { home = it }
            SettingsRow("家所在时区", "")
            SettingsRow("更改日期和时间", "")
            HorizontalDivider(Modifier.padding(vertical = 14.dp), color = Outline)
            SectionLabel("闹钟")
            SettingsRow("闹铃时长", prefs.getInt("alarm_duration", 10).toString() + " 分钟") { durationDialog = true }
            SettingsRow("延后时长", prefs.getInt("snooze", 10).toString() + " 分钟") { snoozeDialog = true }
            Text("闹钟音量", fontSize = 20.sp, modifier = Modifier.padding(top = 16.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("◴", fontSize = 28.sp)
                Box(Modifier.padding(20.dp).height(4.dp).weight(1f).background(PrimaryBlue))
                Box(Modifier.size(18.dp).clip(CircleShape).background(PrimaryBlue))
            }
            SwitchRow("音量逐渐增大", true) {}
            SettingsRow("音量按钮", "控制音量")
            SettingsRow("一周起始日", "星期一")
            HorizontalDivider(Modifier.padding(vertical = 14.dp), color = Outline)
            SectionLabel("定时器")
            SettingsRow("定时器提示音", "默认提示音")
            SwitchRow("音量逐渐增大", true) {}
            SwitchRow("定时器振动", true) {}
            HorizontalDivider(Modifier.padding(vertical = 14.dp), color = Outline)
            SectionLabel("屏保")
            SettingsRow("样式", "数字")
            SwitchRow("夜间模式", true) {}
            Button(onClick = onScreensaver, modifier = Modifier.padding(vertical = 24.dp)) { Text("预览屏保") }
            Spacer(Modifier.height(60.dp))
        }
    }
    if (durationDialog) {
        ChoiceDialog(
            title = "闹铃时长",
            choices = listOf("1 分钟", "5 分钟", "10 分钟", "15 分钟", "20 分钟", "25 分钟", "永不"),
            selected = prefs.getInt("alarm_duration", 10).toString() + " 分钟",
            onDismiss = { durationDialog = false },
            onPick = {
                val v = it.substringBefore(" ").toIntOrNull() ?: 0
                prefs.edit().putInt("alarm_duration", v).apply()
                durationDialog = false
            }
        )
    }
    if (snoozeDialog) {
        ChoiceDialog(
            title = "延后时长",
            choices = (5..15).map { it.toString() + " 分钟" },
            selected = prefs.getInt("snooze", 10).toString() + " 分钟",
            onDismiss = { snoozeDialog = false },
            onPick = {
                prefs.edit().putInt("snooze", it.substringBefore(" ").toInt()).apply()
                snoozeDialog = false
            }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 18.sp, color = PrimaryBlue, modifier = Modifier.padding(vertical = 18.dp))
}

@Composable
private fun SettingsRow(title: String, subtitle: String, onClick: () -> Unit = {}) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp)
    ) {
        Text(title, fontSize = 21.sp, color = TextPrimary)
        if (subtitle.isNotEmpty()) Text(subtitle, fontSize = 17.sp, color = TextSecondary, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 21.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = Color(0xFF38558F),
                checkedThumbColor = Color(0xFFD6DFFF)
            )
        )
    }
}

@Composable
private fun ChoiceDialog(
    title: String,
    choices: List<String>,
    selected: String,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                choices.forEach { choice ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onPick(choice) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = choice == selected, onClick = { onPick(choice) })
                        Text(choice, fontSize = 20.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        containerColor = Color(0xFF50515A)
    )
}

@Composable
private fun ScreenSaver(onExit: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val z = ZonedDateTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.systemDefault())
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(onClick = onExit)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(Modifier.padding(start = 28.dp, top = 54.dp)) {
            Text(z.format(DateTimeFormatter.ofPattern("HH:mm")), fontSize = 88.sp, color = Color(0xFF262626), fontWeight = FontWeight.Light)
            Text(z.format(DateTimeFormatter.ofPattern("M月d日EEEE", Locale.CHINA)) + "  ◴ 周二07:15", fontSize = 19.sp, color = Color(0xFF252525))
        }
    }
}

private fun timeOfDay(minutes: Int): String = two((minutes / 60) % 24) + ":" + two(minutes % 60)
private fun two(value: Int): String = value.toString().padStart(2, '0')
