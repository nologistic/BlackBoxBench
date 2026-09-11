package com.blackboxbench.reproduction

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

data class AlarmItem(
    val id: Long,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean,
    val days: Set<Int>,
    val label: String,
    val vibrate: Boolean = true
)

data class TimerItem(
    val id: Long,
    val initialSeconds: Int,
    val remaining: Int,
    val running: Boolean,
    val label: String
)

private val AppBg = Color(0xFF1B1B22)
private val NavBg = Color(0xFF23242E)
private val CardBg = Color(0xFF24252F)
private val SelectedPill = Color(0xFF464A5C)
private val Primary = Color(0xFFAEC6FF)
private val PrimaryDark = Color(0xFF16335E)
private val Pink = Color(0xFFE2B6DC)
private val Muted = Color(0xFFAAA7AF)
private val WeekNames = listOf("一", "二", "三", "四", "五", "六", "日")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(27, 27, 34)
        window.navigationBarColor = android.graphics.Color.rgb(35, 36, 46)
        setContent { ReproducedApp() }
    }
}

private fun defaultAlarms() = listOf(
    AlarmItem(830, 8, 30, false, setOf(0, 1, 2, 3, 4), ""),
    AlarmItem(900, 9, 0, false, setOf(5, 6), "")
)

private fun loadAlarms(context: Context): List<AlarmItem> {
    val prefs = context.getSharedPreferences("clock_state", Context.MODE_PRIVATE)
    val raw = prefs.getString("alarms", null) ?: return defaultAlarms()
    return try {
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                val daysArray = o.optJSONArray("days") ?: JSONArray()
                val days = mutableSetOf<Int>()
                for (j in 0 until daysArray.length()) days.add(daysArray.getInt(j))
                add(
                    AlarmItem(
                        o.getLong("id"),
                        o.getInt("hour"),
                        o.getInt("minute"),
                        o.optBoolean("enabled"),
                        days,
                        o.optString("label"),
                        o.optBoolean("vibrate", true)
                    )
                )
            }
        }
    } catch (_: Exception) {
        defaultAlarms()
    }
}

private fun saveAlarms(context: Context, alarms: List<AlarmItem>) {
    val array = JSONArray()
    alarms.forEach { alarm ->
        val days = JSONArray()
        alarm.days.sorted().forEach { days.put(it) }
        array.put(
            JSONObject()
                .put("id", alarm.id)
                .put("hour", alarm.hour)
                .put("minute", alarm.minute)
                .put("enabled", alarm.enabled)
                .put("days", days)
                .put("label", alarm.label)
                .put("vibrate", alarm.vibrate)
        )
    }
    context.getSharedPreferences("clock_state", Context.MODE_PRIVATE)
        .edit().putString("alarms", array.toString()).apply()
}

@Composable
fun ReproducedApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("clock_state", Context.MODE_PRIVATE) }
    var selectedTab by remember { mutableIntStateOf(prefs.getInt("tab", 1)) }
    var subScreen by remember { mutableStateOf("main") }
    var clockStyle by remember { mutableStateOf(prefs.getString("style", "digital") ?: "digital") }
    var showSeconds by remember { mutableStateOf(prefs.getBoolean("seconds", false)) }
    val alarms = remember { mutableStateListOf<AlarmItem>().also { it.addAll(loadAlarms(context)) } }
    val timers = remember { mutableStateListOf<TimerItem>() }
    var stopwatchRunning by remember { mutableStateOf(false) }
    var stopwatchElapsed by remember { mutableLongStateOf(0L) }
    var stopwatchAnchor by remember { mutableLongStateOf(0L) }
    val laps = remember { mutableStateListOf<Long>() }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            for (i in timers.indices) {
                val timer = timers[i]
                if (timer.running) timers[i] = timer.copy(remaining = timer.remaining - 1)
            }
        }
    }

    LaunchedEffect(stopwatchRunning) {
        while (stopwatchRunning) {
            stopwatchElapsed = SystemClock.elapsedRealtime() - stopwatchAnchor
            delay(20)
        }
    }

    BackHandler(enabled = subScreen != "main") { subScreen = "main" }

    BenchmarkAppTheme {
        when (subScreen) {
            "settings" -> SettingsScreen(
                clockStyle = clockStyle,
                showSeconds = showSeconds,
                onStyle = {
                    clockStyle = it
                    prefs.edit().putString("style", it).apply()
                },
                onSeconds = {
                    showSeconds = it
                    prefs.edit().putBoolean("seconds", it).apply()
                },
                onBack = { subScreen = "main" }
            )
            "city" -> CitySearchScreen(onBack = { subScreen = "main" })
            "screensaver" -> ScreenSaver(onBack = { subScreen = "main" }, showSeconds = showSeconds)
            else -> Scaffold(
                containerColor = AppBg,
                bottomBar = {
                    ClockBottomBar(
                        selected = selectedTab,
                        onSelect = {
                            selectedTab = it
                            prefs.edit().putInt("tab", it).apply()
                        }
                    )
                }
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    when (selectedTab) {
                        0 -> AlarmScreen(alarms)
                        1 -> ClockScreen(
                            clockStyle = clockStyle,
                            showSeconds = showSeconds,
                            onAddCity = { subScreen = "city" },
                            onSettings = { subScreen = "settings" },
                            onScreenSaver = { subScreen = "screensaver" }
                        )
                        2 -> TimerScreen(timers)
                        3 -> StopwatchScreen(
                            running = stopwatchRunning,
                            elapsed = stopwatchElapsed,
                            laps = laps,
                            onStartPause = {
                                if (stopwatchRunning) {
                                    stopwatchRunning = false
                                } else {
                                    stopwatchAnchor = SystemClock.elapsedRealtime() - stopwatchElapsed
                                    stopwatchRunning = true
                                }
                            },
                            onReset = {
                                stopwatchRunning = false
                                stopwatchElapsed = 0L
                                laps.clear()
                            },
                            onLap = { if (stopwatchRunning) laps.add(stopwatchElapsed) }
                        )
                        else -> BedtimeScreen()
                    }
                }
            }
        }
    }
}

@Composable
private fun ClockBottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val labels = listOf("闹钟", "时钟", "定时器", "秒表", "就寝时间")
    val glyphs = listOf("◴", "◷", "⌛︎", "⏱︎", "▰")
    Surface(color = NavBg) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().height(92.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            labels.forEachIndexed { index, label ->
                Column(
                    Modifier.weight(1f).clickable { onSelect(index) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .background(if (selected == index) SelectedPill else Color.Transparent, RoundedCornerShape(28.dp))
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                    ) {
                        Text(glyphs[index], fontSize = 28.sp, color = if (selected == index) Color.White else Muted)
                    }
                    Text(
                        label,
                        fontSize = 13.sp,
                        fontWeight = if (selected == index) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected == index) Color.White else Muted
                    )
                }
            }
        }
    }
}

@Composable
private fun ScreenHeader(title: String, menu: @Composable (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(start = 24.dp, end = 12.dp, top = 24.dp, bottom = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 28.sp, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.weight(1f))
        if (menu != null) menu()
    }
}

@Composable
private fun OverflowMenu(onSettings: () -> Unit, onScreenSaver: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(
            onClick = { expanded = true },
            modifier = Modifier.size(width = 80.dp, height = 64.dp)
        ) {
            Text("⋮", fontSize = 34.sp, color = Muted)
        }
        if (expanded) {
            Popup(
                alignment = Alignment.TopEnd,
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true)
            ) {
                Surface(
                    modifier = Modifier
                        .padding(top = 98.dp, end = 16.dp)
                        .width(230.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF303038),
                    tonalElevation = 8.dp
                ) {
                    Column(Modifier.padding(vertical = 8.dp)) {
                        MenuRow("屏保") { expanded = false; onScreenSaver() }
                        MenuRow("设置") { expanded = false; onSettings() }
                        MenuRow("隐私权政策") { expanded = false }
                        MenuRow("发送反馈") { expanded = false }
                        MenuRow("帮助") { expanded = false }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuRow(label: String, onClick: () -> Unit) {
    Text(
        label,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 18.dp),
        fontSize = 18.sp,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun ClockScreen(
    clockStyle: String,
    showSeconds: Boolean,
    onAddCity: () -> Unit,
    onSettings: () -> Unit,
    onScreenSaver: () -> Unit
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var menuOpen by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val timePattern = if (showSeconds) "HH:mm:ss" else "HH:mm"
    val time = SimpleDateFormat(timePattern, Locale.getDefault()).format(Date(now))
    val date = SimpleDateFormat("M月d日EEE", Locale.SIMPLIFIED_CHINESE).format(Date(now))
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader("时钟") {
                Box(
                    modifier = Modifier.size(width = 80.dp, height = 64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⋮", fontSize = 34.sp, color = Muted)
                }
            }
            if (clockStyle == "analog") {
                AnalogClock(now, Modifier.align(Alignment.CenterHorizontally).padding(top = 36.dp).size(210.dp))
                Text(date, Modifier.align(Alignment.CenterHorizontally).padding(top = 18.dp), fontSize = 20.sp)
            } else {
                Text(
                    time,
                    Modifier.padding(start = 26.dp, top = 36.dp),
                    fontSize = if (showSeconds) 62.sp else 72.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-3).sp
                )
                Text(date, Modifier.padding(start = 28.dp, top = 18.dp), fontSize = 20.sp)
            }
            Spacer(Modifier.weight(1f))
            FloatingActionButton(
                onClick = onAddCity,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 18.dp).size(96.dp),
                shape = CircleShape,
                containerColor = Primary
            ) {
                Text("+", fontSize = 46.sp, color = PrimaryDark, fontWeight = FontWeight.Light)
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .width(190.dp)
                .height(220.dp)
                .clickable { menuOpen = true }
        )

        if (menuOpen) {
            Popup(
                alignment = Alignment.TopEnd,
                onDismissRequest = { menuOpen = false },
                properties = PopupProperties(focusable = true)
            ) {
                Surface(
                    modifier = Modifier
                        .padding(top = 98.dp, end = 16.dp)
                        .width(230.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF303038),
                    tonalElevation = 8.dp
                ) {
                    Column(Modifier.padding(vertical = 8.dp)) {
                        MenuRow("屏保") { menuOpen = false; onScreenSaver() }
                        MenuRow("设置") { menuOpen = false; onSettings() }
                        MenuRow("隐私权政策") { menuOpen = false }
                        MenuRow("发送反馈") { menuOpen = false }
                        MenuRow("帮助") { menuOpen = false }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalogClock(nowMillis: Long, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawCircle(Color(0xFFF0F1FF))
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = nowMillis }
        val minutes = cal.get(java.util.Calendar.MINUTE)
        val hours = cal.get(java.util.Calendar.HOUR)
        val minuteAngle = Math.toRadians((minutes * 6.0) - 90.0)
        val hourAngle = Math.toRadians(((hours + minutes / 60.0) * 30.0) - 90.0)
        val center = Offset(size.width / 2, size.height / 2)
        drawLine(
            Color(0xFF6076AF),
            center,
            Offset(center.x + cos(minuteAngle).toFloat() * size.width * .30f, center.y + sin(minuteAngle).toFloat() * size.width * .30f),
            strokeWidth = 18.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            Color(0xFF35394A),
            center,
            Offset(center.x + cos(hourAngle).toFloat() * size.width * .22f, center.y + sin(hourAngle).toFloat() * size.width * .22f),
            strokeWidth = 18.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawCircle(Color(0xFF6076AF), 8.dp.toPx(), center)
    }
}

@Composable
private fun CitySearchScreen(onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(AppBg).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().height(80.dp).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("‹", Modifier.clickable(onClick = onBack).padding(10.dp), fontSize = 42.sp)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("搜索城市") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            if (query.isNotEmpty()) Text("×", Modifier.clickable { query = "" }.padding(14.dp), fontSize = 34.sp)
        }
        HorizontalDivider(color = Color(0xFF777984))
        Column(Modifier.padding(start = 56.dp, end = 36.dp, top = 34.dp)) {
            Text("⌁", fontSize = 42.sp, color = Color.White)
            Text("联网即可查看更多城市", fontSize = 25.sp, modifier = Modifier.padding(top = 18.dp))
            Text(
                "要查看所有适用的城市，请连接到互联网。否则您只能看到有限的城市列表。",
                fontSize = 18.sp,
                color = Muted,
                modifier = Modifier.padding(top = 20.dp),
                lineHeight = 28.sp
            )
        }
    }
}

@Composable
private fun SettingsScreen(
    clockStyle: String,
    showSeconds: Boolean,
    onStyle: (String) -> Unit,
    onSeconds: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    var styleMenu by remember { mutableStateOf(false) }
    var homeTime by remember { mutableStateOf(true) }
    var volume by remember { mutableStateOf(.82f) }
    Column(Modifier.fillMaxSize().background(AppBg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("‹", Modifier.clickable(onClick = onBack).padding(8.dp), fontSize = 42.sp)
            Spacer(Modifier.weight(1f))
            Text("⋮", fontSize = 32.sp, color = Muted)
        }
        Text("设置", Modifier.padding(start = 24.dp, top = 42.dp, bottom = 34.dp), fontSize = 44.sp)
        LazyColumn(Modifier.fillMaxSize()) {
            item { SectionLabel("时钟") }
            item {
                Box {
                    SettingRow("样式", if (clockStyle == "analog") "指针" else "数字") { styleMenu = true }
                    DropdownMenu(expanded = styleMenu, onDismissRequest = { styleMenu = false }) {
                        DropdownMenuItem(text = { Text("数字") }, onClick = { onStyle("digital"); styleMenu = false })
                        DropdownMenuItem(text = { Text("指针") }, onClick = { onStyle("analog"); styleMenu = false })
                    }
                }
            }
            item { ToggleRow("显示含秒数的时间", null, showSeconds, onSeconds) }
            item { ToggleRow("自动显示家所在地时间", "在其他时区旅行时显示家中时间", homeTime) { homeTime = it } }
            item { SettingRow("家所在时区", null) {} }
            item { SettingRow("更改日期和时间", null) {} }
            item { HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Color(0xFF3A3B45)) }
            item { SectionLabel("闹钟") }
            item { SettingRow("闹铃时长", "10 分钟") {} }
            item { SettingRow("延后时长", "10 分钟") {} }
            item {
                Column(Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                    Text("闹钟音量", fontSize = 19.sp)
                    Slider(value = volume, onValueChange = { volume = it }, modifier = Modifier.fillMaxWidth())
                }
            }
            item { ToggleRow("逐渐调高音量", null, true) {} }
            item { ToggleRow("音量按钮", "控制音量", false) {} }
            item { SectionLabel("定时器") }
            item { SettingRow("定时器声音", "默认铃声") {} }
            item { ToggleRow("定时器振动", null, true) {} }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = Primary, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
}

@Composable
private fun SettingRow(title: String, subtitle: String?, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 15.dp)
    ) {
        Text(title, fontSize = 19.sp)
        if (subtitle != null) Text(subtitle, fontSize = 16.sp, color = Muted, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 19.sp)
            if (subtitle != null) Text(subtitle, fontSize = 15.sp, color = Muted, modifier = Modifier.padding(top = 4.dp))
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ScreenSaver(onBack: () -> Unit, showSeconds: Boolean) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val pattern = if (showSeconds) "HH:mm:ss" else "HH:mm"
    Box(
        Modifier.fillMaxSize().background(Color.Black).clickable(onClick = onBack),
        contentAlignment = Alignment.Center
    ) {
        Text(
            SimpleDateFormat(pattern, Locale.getDefault()).format(Date(now)),
            fontSize = if (showSeconds) 62.sp else 86.sp,
            fontWeight = FontWeight.Light,
            color = Color(0xFFB7B7B7)
        )
    }
}

private fun repeatSummary(days: Set<Int>): String {
    if (days.isEmpty()) return "今天"
    if (days == setOf(0, 1, 2, 3, 4)) return "周一、周二、周三、周四、周五"
    if (days == setOf(5, 6)) return "周六、周日"
    return days.sorted().joinToString("、") { "周" + WeekNames[it] }
}

@Composable
private fun AlarmScreen(alarms: SnapshotStateList<AlarmItem>) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf<Long?>(null) }
    var addDialog by remember { mutableStateOf(false) }
    var editAlarm by remember { mutableStateOf<AlarmItem?>(null) }
    var labelAlarm by remember { mutableStateOf<AlarmItem?>(null) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("闹钟") {
            OverflowMenu(onSettings = {}, onScreenSaver = {})
        }
        LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            itemsIndexed(alarms, key = { _, item -> item.id }) { index, alarm ->
                AlarmCard(
                    alarm = alarm,
                    expanded = expanded == alarm.id,
                    onExpand = { expanded = if (expanded == alarm.id) null else alarm.id },
                    onToggle = {
                        alarms[index] = alarm.copy(enabled = it)
                        saveAlarms(context, alarms)
                    },
                    onEditTime = { editAlarm = alarm },
                    onLabel = { labelAlarm = alarm },
                    onDay = { day ->
                        val newDays = alarm.days.toMutableSet()
                        if (!newDays.add(day)) newDays.remove(day)
                        alarms[index] = alarm.copy(days = newDays)
                        saveAlarms(context, alarms)
                    },
                    onVibrate = {
                        alarms[index] = alarm.copy(vibrate = it)
                        saveAlarms(context, alarms)
                    },
                    onDelete = {
                        alarms.removeAt(index)
                        saveAlarms(context, alarms)
                    }
                )
                Spacer(Modifier.height(10.dp))
            }
        }
        FloatingActionButton(
            onClick = { addDialog = true },
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 18.dp).size(96.dp),
            shape = CircleShape,
            containerColor = Primary
        ) { Text("+", fontSize = 46.sp, color = PrimaryDark) }
    }

    if (addDialog) {
        TimeEntryDialog(
            title = "选择时间",
            initialHour = ((java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) + 1) % 24),
            initialMinute = 0,
            onDismiss = { addDialog = false },
            onConfirm = { hour, minute ->
                val alarm = AlarmItem(System.currentTimeMillis(), hour, minute, true, emptySet(), "")
                alarms.add(alarm)
                expanded = alarm.id
                saveAlarms(context, alarms)
                addDialog = false
            }
        )
    }
    editAlarm?.let { alarm ->
        TimeEntryDialog(
            title = "选择时间",
            initialHour = alarm.hour,
            initialMinute = alarm.minute,
            onDismiss = { editAlarm = null },
            onConfirm = { hour, minute ->
                val index = alarms.indexOfFirst { it.id == alarm.id }
                if (index >= 0) {
                    alarms[index] = alarm.copy(hour = hour, minute = minute, enabled = true)
                    saveAlarms(context, alarms)
                }
                editAlarm = null
            }
        )
    }
    labelAlarm?.let { alarm ->
        TextEntryDialog(
            title = "标签",
            initial = alarm.label,
            onDismiss = { labelAlarm = null },
            onConfirm = { label ->
                val index = alarms.indexOfFirst { it.id == alarm.id }
                if (index >= 0) {
                    alarms[index] = alarm.copy(label = label)
                    saveAlarms(context, alarms)
                }
                labelAlarm = null
            }
        )
    }
}

@Composable
private fun AlarmCard(
    alarm: AlarmItem,
    expanded: Boolean,
    onExpand: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onEditTime: () -> Unit,
    onLabel: () -> Unit,
    onDay: (Int) -> Unit,
    onVibrate: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val activeColor = if (alarm.enabled) Color.White else Muted
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            if (expanded) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (alarm.label.isBlank()) "▱  添加标签" else "▱  " + alarm.label, Modifier.clickable(onClick = onLabel), fontSize = 17.sp, color = Muted)
                    Spacer(Modifier.weight(1f))
                    Text("⌃", Modifier.clickable(onClick = onExpand).padding(8.dp), fontSize = 26.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    String.format(Locale.getDefault(), "%02d:%02d", alarm.hour, alarm.minute),
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Light,
                    color = activeColor,
                    modifier = Modifier.clickable(onClick = onEditTime)
                )
                Spacer(Modifier.weight(1f))
                if (!expanded) Text("⌄", Modifier.clickable(onClick = onExpand).padding(8.dp), fontSize = 28.sp, color = Muted)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(repeatSummary(alarm.days), fontSize = 17.sp, color = activeColor, modifier = Modifier.weight(1f))
                Switch(checked = alarm.enabled, onCheckedChange = onToggle)
            }
            if (expanded) {
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    WeekNames.forEachIndexed { index, name ->
                        Box(
                            Modifier
                                .size(34.dp)
                                .background(if (index in alarm.days) Primary else Color.Transparent, CircleShape)
                                .border(1.dp, if (index in alarm.days) Primary else Muted, CircleShape)
                                .clickable { onDay(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(name, fontSize = 17.sp, color = if (index in alarm.days) PrimaryDark else Muted)
                        }
                    }
                }
                AlarmOptionRow("◉", "暂停闹钟", "+") {}
                AlarmOptionRow("♬", "默认铃声 (Cesium)", "") {}
                Row(
                    Modifier.fillMaxWidth().clickable { onVibrate(!alarm.vibrate) }.padding(vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("▣", fontSize = 23.sp, modifier = Modifier.width(42.dp))
                    Text("振动", fontSize = 17.sp, modifier = Modifier.weight(1f))
                    Checkbox(checked = alarm.vibrate, onCheckedChange = onVibrate)
                }
                Text("删除", Modifier.clickable(onClick = onDelete).padding(vertical = 14.dp), fontSize = 17.sp)
            }
        }
    }
}

@Composable
private fun AlarmOptionRow(icon: String, text: String, trailing: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 22.sp, modifier = Modifier.width(42.dp))
        Text(text, fontSize = 17.sp, modifier = Modifier.weight(1f))
        if (trailing.isNotEmpty()) Text(trailing, fontSize = 26.sp, color = Muted)
    }
}

@Composable
private fun TimeEntryDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var hour by remember { mutableStateOf(String.format(Locale.US, "%02d", initialHour)) }
    var minute by remember { mutableStateOf(String.format(Locale.US, "%02d", initialMinute)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = hour,
                    onValueChange = { hour = it.filter(Char::isDigit).take(2) },
                    label = { Text("小时") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                Text(":", fontSize = 34.sp, modifier = Modifier.padding(horizontal = 8.dp))
                OutlinedTextField(
                    value = minute,
                    onValueChange = { minute = it.filter(Char::isDigit).take(2) },
                    label = { Text("分钟") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val h = hour.toIntOrNull()?.coerceIn(0, 23) ?: 0
                val m = minute.toIntOrNull()?.coerceIn(0, 59) ?: 0
                onConfirm(h, m)
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun TextEntryDialog(title: String, initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(title) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

private fun inputToSeconds(input: String): Int {
    val padded = input.padStart(6, '0')
    val h = padded.take(2).toIntOrNull() ?: 0
    val m = padded.substring(2, 4).toIntOrNull() ?: 0
    val s = padded.takeLast(2).toIntOrNull() ?: 0
    return h * 3600 + m * 60 + s
}

private fun secondsDisplay(total: Int): String {
    val abs = kotlin.math.abs(total)
    val h = abs / 3600
    val m = (abs % 3600) / 60
    val s = abs % 60
    val value = if (h > 0) String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
    else if (m > 0) String.format(Locale.US, "%02d:%02d", m, s)
    else s.toString()
    return if (total < 0) "-" + value else value
}

private fun durationName(seconds: Int): String {
    return when {
        seconds % 3600 == 0 -> (seconds / 3600).toString() + "小时的定时器"
        seconds % 60 == 0 -> (seconds / 60).toString() + "分钟的定时器"
        else -> seconds.toString() + "秒的定时器"
    }
}

@Composable
private fun TimerScreen(timers: SnapshotStateList<TimerItem>) {
    var input by remember { mutableStateOf("") }
    var editTimer by remember { mutableStateOf<TimerItem?>(null) }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("定时器") { OverflowMenu(onSettings = {}, onScreenSaver = {}) }
        if (timers.isEmpty()) {
            TimerInput(
                input = input,
                onDigit = { digit ->
                    if (digit == "⌫") input = input.dropLast(1)
                    else if (input.length < 6) input += digit
                },
                onStart = {
                    val seconds = inputToSeconds(input)
                    if (seconds > 0) {
                        timers.add(TimerItem(System.currentTimeMillis(), seconds, seconds, true, durationName(seconds)))
                        input = ""
                    }
                }
            )
        } else {
            LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                itemsIndexed(timers, key = { _, item -> item.id }) { index, timer ->
                    TimerCard(
                        timer = timer,
                        onToggle = { timers[index] = timer.copy(running = !timer.running) },
                        onAddMinute = { timers[index] = timer.copy(remaining = timer.remaining + 60, running = true) },
                        onReset = { timers[index] = timer.copy(remaining = timer.initialSeconds, running = false) },
                        onRemove = { timers.removeAt(index) },
                        onLabel = { editTimer = timer }
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
            FloatingActionButton(
                onClick = { timers.clear() },
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 18.dp).size(96.dp),
                shape = CircleShape,
                containerColor = Primary
            ) { Text("+", fontSize = 46.sp, color = PrimaryDark) }
        }
    }
    editTimer?.let { timer ->
        TextEntryDialog(
            title = "标签",
            initial = timer.label,
            onDismiss = { editTimer = null },
            onConfirm = { label ->
                val index = timers.indexOfFirst { it.id == timer.id }
                if (index >= 0) timers[index] = timer.copy(label = label.ifBlank { durationName(timer.initialSeconds) })
                editTimer = null
            }
        )
    }
}

@Composable
private fun TimerInput(input: String, onDigit: (String) -> Unit, onStart: () -> Unit) {
    val padded = input.padStart(6, '0')
    val hh = padded.take(2)
    val mm = padded.substring(2, 4)
    val ss = padded.takeLast(2)
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.padding(top = 38.dp), verticalAlignment = Alignment.Bottom) {
            TimePart(hh, "时", input.length > 4)
            TimePart(mm, "分", input.length in 3..4)
            TimePart(ss, "秒", input.isNotEmpty() && input.length <= 2)
        }
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "00", "0", "⌫")
        Column(Modifier.padding(top = 48.dp)) {
            for (row in 0..3) {
                Row {
                    for (col in 0..2) {
                        val key = keys[row * 3 + col]
                        Box(
                            Modifier
                                .padding(4.dp)
                                .size(104.dp)
                                .background(if (key == "⌫") SelectedPill else Color(0xFF22232C), CircleShape)
                                .clickable { onDigit(key) },
                            contentAlignment = Alignment.Center
                        ) { Text(key, fontSize = if (key == "⌫") 28.sp else 34.sp) }
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        if (inputToSeconds(input) > 0) {
            FloatingActionButton(
                onClick = onStart,
                modifier = Modifier.padding(bottom = 18.dp).size(96.dp),
                shape = CircleShape,
                containerColor = Primary
            ) { Text("▶", color = PrimaryDark, fontSize = 30.sp) }
        }
    }
}

@Composable
private fun TimePart(value: String, suffix: String, active: Boolean) {
    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(horizontal = 4.dp)) {
        Text(value, fontSize = 52.sp, fontWeight = FontWeight.Light, color = if (active) Primary else Muted)
        Text(suffix, fontSize = 19.sp, color = if (active) Primary else Muted, modifier = Modifier.padding(bottom = 8.dp))
    }
}

@Composable
private fun TimerCard(
    timer: TimerItem,
    onToggle: () -> Unit,
    onAddMinute: () -> Unit,
    onReset: () -> Unit,
    onRemove: () -> Unit,
    onLabel: () -> Unit
) {
    val overtime = timer.remaining < 0
    Card(
        colors = CardDefaults.cardColors(containerColor = if (overtime) Color(0xFF8EA7E4) else CardBg),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(timer.label, Modifier.clickable(onClick = onLabel), fontSize = 25.sp, color = if (overtime) PrimaryDark else Color.White)
                Spacer(Modifier.weight(1f))
                Text("×", Modifier.clickable(onClick = onRemove).padding(6.dp), fontSize = 32.sp)
            }
            Box(
                Modifier.padding(vertical = 18.dp).fillMaxWidth().aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.fillMaxSize().padding(8.dp)) {
                    drawCircle(
                        if (overtime) Color(0xFF414654) else Color(0xFF535563),
                        style = Stroke(width = 10.dp.toPx())
                    )
                    if (!overtime) {
                        val progress = (timer.remaining.coerceAtLeast(0).toFloat() / timer.initialSeconds.coerceAtLeast(1))
                        drawArc(
                            Primary,
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(secondsDisplay(timer.remaining), fontSize = 72.sp, color = if (overtime) PrimaryDark else Color.White)
                    if (!timer.running && timer.remaining != timer.initialSeconds) {
                        Text("↻", Modifier.clickable(onClick = onReset).padding(8.dp), fontSize = 34.sp, color = Primary)
                    }
                }
            }
            Row {
                if (timer.remaining != timer.initialSeconds) {
                    Button(
                        onClick = onAddMinute,
                        colors = ButtonDefaults.buttonColors(containerColor = SelectedPill),
                        modifier = Modifier.weight(1f).height(72.dp)
                    ) { Text("+1:00", fontSize = 24.sp) }
                    Spacer(Modifier.width(10.dp))
                }
                Button(
                    onClick = onToggle,
                    colors = ButtonDefaults.buttonColors(containerColor = Pink, contentColor = Color(0xFF4B1D43)),
                    modifier = Modifier.weight(1f).height(72.dp)
                ) { Text(if (timer.running) "Ⅱ" else "▶", fontSize = 30.sp) }
            }
        }
    }
}

@Composable
private fun StopwatchScreen(
    running: Boolean,
    elapsed: Long,
    laps: SnapshotStateList<Long>,
    onStartPause: () -> Unit,
    onReset: () -> Unit,
    onLap: () -> Unit
) {
    val totalHundredths = elapsed / 10
    val minutes = totalHundredths / 6000
    val seconds = (totalHundredths / 100) % 60
    val hundredths = totalHundredths % 100
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("秒表") { OverflowMenu(onSettings = {}, onScreenSaver = {}) }
        Box(
            Modifier.align(Alignment.CenterHorizontally).padding(top = 108.dp).size(320.dp).border(10.dp, Color(0xFF4E4F59), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(String.format(Locale.US, "%02d:%02d", minutes, seconds), fontSize = 62.sp, fontWeight = FontWeight.Light)
                Text(String.format(Locale.US, "%02d", hundredths), fontSize = 38.sp, fontWeight = FontWeight.Light)
            }
        }
        if (laps.isNotEmpty()) {
            LazyColumn(Modifier.weight(1f).padding(horizontal = 64.dp, vertical = 16.dp)) {
                itemsIndexed(laps.reversed()) { index, lap ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                        Text("圈 " + (laps.size - index), color = Muted)
                        Spacer(Modifier.weight(1f))
                        Text(String.format(Locale.US, "%02d:%02d.%02d", lap / 60000, (lap / 1000) % 60, (lap / 10) % 100))
                    }
                }
            }
        } else Spacer(Modifier.weight(1f))
        if (elapsed == 0L && !running) {
            FloatingActionButton(
                onClick = onStartPause,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 18.dp).size(96.dp),
                shape = CircleShape,
                containerColor = Primary
            ) { Text("▶", color = PrimaryDark, fontSize = 30.sp) }
        } else {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 44.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RoundSmallButton("↻", onReset)
                Button(
                    onClick = onStartPause,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = PrimaryDark),
                    modifier = Modifier.width(154.dp).height(92.dp),
                    shape = RoundedCornerShape(34.dp)
                ) { Text(if (running) "Ⅱ" else "▶", fontSize = 30.sp) }
                RoundSmallButton("⏱", onLap, enabled = running)
            }
        }
    }
}

@Composable
private fun RoundSmallButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Box(
        Modifier.size(58.dp).background(if (enabled) Color(0xFF623F60) else Color(0xFF3C303C), CircleShape).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, fontSize = 25.sp, color = if (enabled) Color.White else Muted) }
}

@Composable
private fun BedtimeScreen() {
    var scheduleEnabled by remember { mutableStateOf(false) }
    var reminder by remember { mutableStateOf(true) }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("就寝时间") { OverflowMenu(onSettings = {}, onScreenSaver = {}) }
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            item {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    modifier = Modifier.fillMaxWidth().padding(top = 22.dp)
                ) {
                    Column(Modifier.padding(26.dp)) {
                        Text("☾", fontSize = 48.sp, color = Primary)
                        Text("设置规律的就寝时间", fontSize = 27.sp, modifier = Modifier.padding(top = 12.dp))
                        Text("创建睡眠时间表，并在该休息时收到提醒。", color = Muted, fontSize = 17.sp, lineHeight = 25.sp, modifier = Modifier.padding(top = 12.dp))
                        Button(
                            onClick = { scheduleEnabled = true },
                            modifier = Modifier.padding(top = 22.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = PrimaryDark)
                        ) { Text(if (scheduleEnabled) "已设置" else "开始使用") }
                    }
                }
            }
            if (scheduleEnabled) {
                item { SectionLabel("时间表") }
                item { SettingRow("就寝时间", "23:00") {} }
                item { SettingRow("起床时间", "07:00") {} }
                item { ToggleRow("就寝时间提醒", "提前 15 分钟", reminder) { reminder = it } }
                item { ToggleRow("日出闹钟", "闹钟响起前逐渐调亮屏幕", true) {} }
            } else {
                item {
                    Text(
                        "保持规律的睡眠时间有助于改善睡眠质量。",
                        Modifier.fillMaxWidth().padding(top = 28.dp),
                        textAlign = TextAlign.Center,
                        color = Muted,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
