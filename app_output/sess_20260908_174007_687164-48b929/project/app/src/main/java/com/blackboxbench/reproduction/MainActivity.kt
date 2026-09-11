package com.blackboxbench.reproduction

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.max

private val LoopBlue = Color(0xFF3F51B5)
private val LoopBlueDark = Color(0xFF27336D)
private val LoopOrange = Color(0xFFFF8A3D)
private val LoopPurple = Color(0xFF9858C7)
private val Palette = listOf(
    "#D32F2F", "#E64A19", "#F57C00", "#FBC02D", "#AFB42B",
    "#689F38", "#388E3C", "#00897B", "#0097A7", "#0288D1",
    "#1976D2", "#3F51B5", "#5E35B1", "#7B1FA2", "#C2185B",
    "#795548", "#757575", "#546E7A", "#26A69A", "#FF7043"
)

enum class HabitKind { BOOLEAN, QUANTIFIED }

data class Habit(
    val id: String,
    val name: String,
    val question: String,
    val kind: HabitKind,
    val unit: String = "",
    val target: Float = 1f,
    val atLeast: Boolean = true,
    val colorHex: String = "#3F51B5",
    val frequency: String = "每天",
    val reminder: String = "",
    val note: String = "",
    val archived: Boolean = false,
    val checkins: Map<String, Float> = emptyMap()
)

private fun Habit.color(): Color = Color(android.graphics.Color.parseColor(colorHex))
private fun Float.pretty(): String = if (this % 1f == 0f) toInt().toString() else String.format("%.1f", this)
private fun Habit.done(date: LocalDate): Boolean {
    val value = checkins[date.toString()] ?: 0f
    return if (kind == HabitKind.BOOLEAN) value > 0f else if (atLeast) value >= target else value <= target && value > 0f
}

class HabitRepository(context: Context) {
    private val prefs = context.getSharedPreferences("loop_prefs", Context.MODE_PRIVATE)
    val habits: SnapshotStateList<Habit> = mutableStateListOf()

    var onboarded: Boolean
        get() = prefs.getBoolean("onboarded", false)
        set(value) { prefs.edit().putBoolean("onboarded", value).apply() }

    var darkTheme: Boolean
        get() = prefs.getBoolean("dark_theme", false)
        set(value) { prefs.edit().putBoolean("dark_theme", value).apply() }

    init { habits.addAll(loadHabits()) }

    private fun loadHabits(): List<Habit> {
        val text = prefs.getString("habits", null) ?: return emptyList()
        return try {
            val array = JSONArray(text)
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    val checks = mutableMapOf<String, Float>()
                    val c = o.optJSONObject("checkins") ?: JSONObject()
                    val keys = c.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        checks[key] = c.optDouble(key, 0.0).toFloat()
                    }
                    add(
                        Habit(
                            id = o.getString("id"),
                            name = o.getString("name"),
                            question = o.optString("question"),
                            kind = if (o.optString("kind") == "QUANTIFIED") HabitKind.QUANTIFIED else HabitKind.BOOLEAN,
                            unit = o.optString("unit"),
                            target = o.optDouble("target", 1.0).toFloat(),
                            atLeast = o.optBoolean("atLeast", true),
                            colorHex = o.optString("colorHex", "#3F51B5"),
                            frequency = o.optString("frequency", "每天"),
                            reminder = o.optString("reminder"),
                            note = o.optString("note"),
                            archived = o.optBoolean("archived"),
                            checkins = checks
                        )
                    )
                }
            }
        } catch (_: Exception) { emptyList() }
    }

    fun save() {
        val array = JSONArray()
        habits.forEach { h ->
            val checks = JSONObject()
            h.checkins.forEach { (key, value) -> checks.put(key, value.toDouble()) }
            array.put(JSONObject().apply {
                put("id", h.id); put("name", h.name); put("question", h.question)
                put("kind", h.kind.name); put("unit", h.unit); put("target", h.target.toDouble())
                put("atLeast", h.atLeast); put("colorHex", h.colorHex); put("frequency", h.frequency)
                put("reminder", h.reminder); put("note", h.note); put("archived", h.archived)
                put("checkins", checks)
            })
        }
        prefs.edit().putString("habits", array.toString()).commit()
    }

    fun upsert(habit: Habit) {
        val i = habits.indexOfFirst { it.id == habit.id }
        if (i >= 0) habits[i] = habit else habits.add(habit)
        save()
    }

    fun setCheckin(id: String, date: LocalDate, value: Float) {
        val i = habits.indexOfFirst { it.id == id }
        if (i < 0) return
        val h = habits[i]
        val m = h.checkins.toMutableMap()
        if (value <= 0f) m.remove(date.toString()) else m[date.toString()] = value
        habits[i] = h.copy(checkins = m)
        save()
    }

    fun archive(id: String, archived: Boolean) {
        val i = habits.indexOfFirst { it.id == id }
        if (i >= 0) {
            habits[i] = habits[i].copy(archived = archived)
            save()
        }
    }

    fun delete(id: String) {
        habits.removeAll { it.id == id }
        save()
    }
}

enum class Page { MAIN, FORM, DETAIL, SETTINGS, ABOUT }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ReproducedApp() }
    }
}

@Composable
fun ReproducedApp() {
    val context = LocalContext.current
    val repo = remember { HabitRepository(context) }
    var onboarded by remember { mutableStateOf(repo.onboarded) }
    var dark by remember { mutableStateOf(repo.darkTheme) }

    SideEffect {
        (context as? Activity)?.window?.apply {
            statusBarColor = if (dark) 0xFF111111.toInt() else 0xFF303F9F.toInt()
            navigationBarColor = if (dark) 0xFF111111.toInt() else 0xFFF5F5F5.toInt()
        }
    }

    val scheme = if (dark) {
        darkColorScheme(primary = Color(0xFF9FA8DA), secondary = Color(0xFF80CBC4), surface = Color(0xFF202124), background = Color(0xFF121212))
    } else {
        lightColorScheme(primary = LoopBlue, secondary = Color(0xFF00897B), surface = Color.White, background = Color(0xFFF5F5F5))
    }

    MaterialTheme(colorScheme = scheme) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (!onboarded) {
                Onboarding {
                    repo.onboarded = true
                    onboarded = true
                }
            } else {
                LoopApp(repo = repo, dark = dark, onDarkChanged = {
                    dark = it
                    repo.darkTheme = it
                })
            }
        }
    }
}

@Composable
private fun Onboarding(onFinished: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val backgrounds = listOf(LoopBlue, LoopOrange, LoopPurple)
    val titles = listOf("欢迎", "养成一些新习惯", "记录你的进步")
    val descriptions = listOf(
        "Loop 习惯记录能帮你养成和保持好习惯。",
        "每天回答简单的问题，逐步建立更健康、更规律的生活方式。",
        "详细图表展示长期以来习惯养成情况"
    )
    Column(
        Modifier.fillMaxSize().background(backgrounds[page]).systemBarsPadding().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(56.dp))
        Text(titles[page], color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.Normal, textAlign = TextAlign.Center)
        Spacer(Modifier.height(54.dp))
        OnboardingIllustration(page)
        Spacer(Modifier.height(55.dp))
        Text(descriptions[page], color = Color.White, fontSize = 19.sp, lineHeight = 28.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth().padding(bottom = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { page = 0 }, modifier = Modifier.size(52.dp)) {
                Text("↺", color = Color.White, fontSize = 27.sp)
            }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center) {
                repeat(3) { i ->
                    Box(
                        Modifier.padding(5.dp).size(if (i == page) 11.dp else 8.dp)
                            .clip(CircleShape).background(if (i == page) Color.White else Color.White.copy(alpha = .45f))
                    )
                }
            }
            TextButton(
                onClick = { if (page < 2) page++ else onFinished() },
                modifier = Modifier.size(52.dp)
            ) { Text(if (page < 2) "›" else "✓", color = Color.White, fontSize = 31.sp) }
        }
    }
}

@Composable
private fun OnboardingIllustration(page: Int) {
    Card(
        modifier = Modifier.size(width = 270.dp, height = 285.dp),
        colors = CardDefaults.cardColors(containerColor = if (page == 0) Color.Transparent else Color.White),
        elevation = CardDefaults.cardElevation(if (page == 0) 0.dp else 8.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Canvas(Modifier.fillMaxSize().padding(18.dp)) {
            if (page == 0) {
                drawCircle(Color.White, radius = size.minDimension * .38f, center = center, style = Stroke(width = 18f))
                drawArc(Color.White, 25f, 285f, false, style = Stroke(width = 20f, cap = StrokeCap.Round))
                val r = size.minDimension * .35f
                drawLine(Color.White, Offset(center.x + r * .65f, center.y - r * .45f), Offset(center.x + r * 1.02f, center.y - r * .2f), 18f, StrokeCap.Round)
            } else if (page == 1) {
                val green = Color(0xFF69C98B)
                repeat(3) { i ->
                    val y = 55f + i * 115f
                    drawCircle(green.copy(alpha = .25f), 38f, Offset(48f, y))
                    drawCircle(green, 24f, Offset(48f, y), style = Stroke(7f))
                    drawLine(green, Offset(36f, y), Offset(46f, y + 10f), 7f, StrokeCap.Round)
                    drawLine(green, Offset(46f, y + 10f), Offset(64f, y - 12f), 7f, StrokeCap.Round)
                    drawLine(Color(0xFFB0B0B0), Offset(105f, y - 10f), Offset(size.width - 16f, y - 10f), 10f, StrokeCap.Round)
                    drawLine(Color(0xFFE0E0E0), Offset(105f, y + 16f), Offset(size.width - 75f, y + 16f), 8f, StrokeCap.Round)
                }
            } else {
                val yellow = Color(0xFFFFC107)
                repeat(5) { i ->
                    val x = 30f + i * (size.width - 60f) / 4f
                    drawLine(Color(0xFFE8E8E8), Offset(x, 20f), Offset(x, size.height - 25f), 3f)
                }
                repeat(4) { i ->
                    val y = 30f + i * (size.height - 60f) / 3f
                    drawLine(Color(0xFFE8E8E8), Offset(20f, y), Offset(size.width - 20f, y), 3f)
                }
                val path = Path().apply {
                    moveTo(25f, size.height - 45f)
                    lineTo(size.width * .25f, size.height * .63f)
                    lineTo(size.width * .43f, size.height * .70f)
                    lineTo(size.width * .63f, size.height * .35f)
                    lineTo(size.width - 25f, size.height * .18f)
                }
                drawPath(path, yellow, style = Stroke(11f, cap = StrokeCap.Round))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoopApp(repo: HabitRepository, dark: Boolean, onDarkChanged: (Boolean) -> Unit) {
    var page by remember { mutableStateOf(Page.MAIN) }
    var formKind by remember { mutableStateOf(HabitKind.BOOLEAN) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var editHabit by remember { mutableStateOf<Habit?>(null) }

    when (page) {
        Page.MAIN -> MainScreen(
            repo = repo,
            dark = dark,
            onDarkChanged = onDarkChanged,
            onCreate = {
                formKind = it
                editHabit = null
                page = Page.FORM
            },
            onOpen = {
                selectedId = it
                page = Page.DETAIL
            },
            onSettings = { page = Page.SETTINGS },
            onAbout = { page = Page.ABOUT }
        )
        Page.FORM -> HabitFormScreen(
            initial = editHabit,
            kind = editHabit?.kind ?: formKind,
            onBack = { page = if (editHabit != null) Page.DETAIL else Page.MAIN },
            onSave = {
                repo.upsert(it)
                selectedId = it.id
                page = if (editHabit != null) Page.DETAIL else Page.MAIN
            }
        )
        Page.DETAIL -> {
            val habit = repo.habits.firstOrNull { it.id == selectedId }
            if (habit == null) page = Page.MAIN else DetailScreen(
                habit = habit,
                repo = repo,
                onBack = { page = Page.MAIN },
                onEdit = {
                    editHabit = habit
                    page = Page.FORM
                },
                onDeleted = {
                    repo.delete(habit.id)
                    page = Page.MAIN
                }
            )
        }
        Page.SETTINGS -> SettingsScreen(dark, onDarkChanged, onBack = { page = Page.MAIN }, onAbout = { page = Page.ABOUT })
        Page.ABOUT -> AboutScreen { page = Page.MAIN }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun MainScreen(
    repo: HabitRepository,
    dark: Boolean,
    onDarkChanged: (Boolean) -> Unit,
    onCreate: (HabitKind) -> Unit,
    onOpen: (String) -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit
) {
    val context = LocalContext.current
    var addDialog by remember { mutableStateOf(false) }
    var filterDialog by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var selectedMenu by remember { mutableStateOf(false) }
    var deleteConfirm by remember { mutableStateOf(false) }
    var hideArchived by remember { mutableStateOf(true) }
    var hideCompleted by remember { mutableStateOf(false) }
    var sort by remember { mutableStateOf("手动") }
    var numericHabit by remember { mutableStateOf<Habit?>(null) }
    var numericDate by remember { mutableStateOf(LocalDate.now()) }
    var binaryHabit by remember { mutableStateOf<Habit?>(null) }
    var binaryDate by remember { mutableStateOf(LocalDate.now()) }
    val today = LocalDate.now()
    val dates = remember(today) { (0..9).map { today.minusDays(it.toLong()) } }
    val displayed = repo.habits
        .filter { !hideArchived || !it.archived }
        .filter { !hideCompleted || !it.done(today) }
        .let { list ->
            when (sort) {
                "名称" -> list.sortedBy { it.name.lowercase() }
                "颜色" -> list.sortedBy { it.colorHex }
                "得分" -> list.sortedByDescending { score(it) }
                "状态" -> list.sortedByDescending { it.done(today) }
                else -> list
            }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = if (dark) Color(0xFF1B1B1B) else LoopBlue, titleContentColor = Color.White),
                title = {
                    if (selectedId == null) Text("习惯", fontSize = 22.sp)
                    else Text("已选择 1 项", fontSize = 20.sp)
                },
                navigationIcon = {
                    if (selectedId != null) TextButton(onClick = { selectedId = null }) { Text("‹", color = Color.White, fontSize = 34.sp) }
                },
                actions = {
                    if (selectedId == null) {
                        IconButton(onClick = { addDialog = true }, modifier = Modifier.size(48.dp)) { Text("＋", color = Color.White, fontSize = 28.sp) }
                        IconButton(onClick = { filterDialog = true }, modifier = Modifier.size(48.dp)) { Text("▽", color = Color.White, fontSize = 25.sp) }
                        Box {
                            IconButton(onClick = { menu = true }, modifier = Modifier.size(48.dp)) { Text("⋮", color = Color.White, fontSize = 27.sp) }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(
                                    text = { Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(dark, onCheckedChange = null)
                                        Text("深色主题")
                                    }},
                                    onClick = { onDarkChanged(!dark); menu = false }
                                )
                                DropdownMenuItem(text = { Text("设置") }, onClick = { menu = false; onSettings() })
                                DropdownMenuItem(text = { Text("帮助与常见问题") }, onClick = {
                                    menu = false
                                    Toast.makeText(context, "帮助页面", Toast.LENGTH_SHORT).show()
                                })
                                DropdownMenuItem(text = { Text("关于") }, onClick = { menu = false; onAbout() })
                            }
                        }
                    } else {
                        TextButton(onClick = {
                            repo.habits.firstOrNull { it.id == selectedId }?.let { onOpen(it.id) }
                        }) { Text("✎", color = Color.White, fontSize = 23.sp) }
                        Box {
                            TextButton(onClick = { selectedMenu = true }) { Text("⋮", color = Color.White, fontSize = 27.sp) }
                            DropdownMenu(expanded = selectedMenu, onDismissRequest = { selectedMenu = false }) {
                                val chosen = repo.habits.firstOrNull { it.id == selectedId }
                                DropdownMenuItem(text = { Text(if (chosen?.archived == true) "取消存档" else "存档") }, onClick = {
                                    chosen?.let { repo.archive(it.id, !it.archived) }
                                    selectedMenu = false; selectedId = null
                                })
                                DropdownMenuItem(text = { Text("删除") }, onClick = { selectedMenu = false; deleteConfirm = true })
                            }
                        }
                    }
                    Spacer(Modifier.width(24.dp))
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            DateHeader(dates)
            if (displayed.isEmpty()) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("☆", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 86.sp, fontWeight = FontWeight.ExtraLight)
                    Spacer(Modifier.height(18.dp))
                    Text("你还没有任何习惯", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 17.sp)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(displayed, key = { it.id }) { habit ->
                        HabitRow(
                            habit = habit,
                            dates = dates.take(5),
                            selected = selectedId == habit.id,
                            onLongPress = { selectedId = habit.id },
                            onTitle = { onOpen(habit.id) },
                            onDate = { d ->
                                if (habit.kind == HabitKind.BOOLEAN) {
                                    binaryHabit = habit; binaryDate = d
                                } else {
                                    numericHabit = habit; numericDate = d
                                }
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }

    if (addDialog) {
        Dialog(onDismissRequest = { addDialog = false }) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surface)) {
                HabitTypeCard(
                    title = "完成与否",
                    examples = "你今天早起了吗？\n你锻炼了吗？\n你下棋了吗？",
                    accent = Color(0xFF52A86C)
                ) { addDialog = false; onCreate(HabitKind.BOOLEAN) }
                HorizontalDivider()
                HabitTypeCard(
                    title = "可量化的",
                    examples = "今天你跑了几公里？\n你读了几页书？",
                    accent = LoopOrange
                ) { addDialog = false; onCreate(HabitKind.QUANTIFIED) }
            }
        }
    }

    if (filterDialog) {
        AlertDialog(
            onDismissRequest = { filterDialog = false },
            title = { Text("筛选习惯") },
            text = {
                Column {
                    CheckRow("隐藏已归档", hideArchived) { hideArchived = it }
                    CheckRow("隐藏已完成", hideCompleted) { hideCompleted = it }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Text("排序方式", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                    listOf("手动", "名称", "颜色", "得分", "状态").forEach { option ->
                        Row(
                            Modifier.fillMaxWidth().clickable { sort = option }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(sort == option, onClick = { sort = option })
                            Text(option + if (option == "手动" && sort == option) "  ↑" else "")
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { filterDialog = false }) { Text("完成") } }
        )
    }

    binaryHabit?.let { habit ->
        BinaryCheckDialog(habit, binaryDate, onDismiss = { binaryHabit = null }) { value ->
            repo.setCheckin(habit.id, binaryDate, value)
            binaryHabit = null
        }
    }
    numericHabit?.let { habit ->
        NumericCheckDialog(habit, numericDate, onDismiss = { numericHabit = null }) { value ->
            repo.setCheckin(habit.id, numericDate, value)
            numericHabit = null
        }
    }
    if (deleteConfirm) {
        AlertDialog(
            onDismissRequest = { deleteConfirm = false },
            title = { Text("删除习惯?") },
            text = { Text("习惯将被永久地删除。此操作无法撤消。") },
            dismissButton = { TextButton(onClick = { deleteConfirm = false }) { Text("取消") } },
            confirmButton = { TextButton(onClick = {
                selectedId?.let(repo::delete)
                selectedId = null; deleteConfirm = false
            }) { Text("删除") } }
        )
    }
}

@Composable
private fun HabitTypeCard(title: String, examples: String, accent: Color, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(22.dp)) {
        Box(Modifier.size(58.dp).clip(CircleShape).background(accent.copy(alpha = .14f)), contentAlignment = Alignment.Center) {
            Text(if (title == "完成与否") "✓" else "123", color = accent, fontSize = if (title == "完成与否") 28.sp else 17.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(18.dp))
        Column {
            Text(title, fontSize = 21.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(7.dp))
            Text(examples, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 21.sp)
        }
    }
}

@Composable
private fun DateHeader(dates: List<LocalDate>) {
    val scroll = rememberScrollState()
    val formatter = DateTimeFormatter.ofPattern("M/d")
    Row(
        Modifier.fillMaxWidth().height(70.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f))
            .horizontalScroll(scroll),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(118.dp))
        dates.forEachIndexed { index, date ->
            Column(Modifier.width(64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (index == 0) "今天" else listOf("一","二","三","四","五","六","日")[date.dayOfWeek.value - 1], fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatter.format(date), fontSize = 15.sp, fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HabitRow(
    habit: Habit,
    dates: List<LocalDate>,
    selected: Boolean,
    onLongPress: () -> Unit,
    onTitle: () -> Unit,
    onDate: (LocalDate) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().height(102.dp)
            .background(if (habit.archived) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .7f) else MaterialTheme.colorScheme.surface)
            .then(if (selected) Modifier.border(2.dp, habit.color()) else Modifier)
            .combinedClickable(onClick = onTitle, onLongClick = onLongPress),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(118.dp).fillMaxHeight().padding(start = 8.dp), contentAlignment = Alignment.CenterStart) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    progress = { score(habit) / 100f },
                    modifier = Modifier.size(34.dp),
                    color = habit.color(),
                    trackColor = habit.color().copy(alpha = .18f),
                    strokeWidth = 4.dp
                )
                Spacer(Modifier.width(5.dp))
                Column(Modifier.width(70.dp)) {
                    Text(habit.name, color = if (habit.archived) MaterialTheme.colorScheme.onSurfaceVariant else habit.color(), fontSize = 15.sp, maxLines = 2, fontWeight = FontWeight.Medium)
                    Text(habit.frequency, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
        }
        dates.forEach { date ->
            val value = habit.checkins[date.toString()] ?: 0f
            Box(
                Modifier.width(64.dp).fillMaxHeight().combinedClickable(onClick = { onDate(date) }, onLongClick = { onDate(date) }),
                contentAlignment = Alignment.Center
            ) {
                if (habit.kind == HabitKind.BOOLEAN) {
                    Text(if (value > 0f) "✓" else "×", color = if (value > 0f) habit.color() else MaterialTheme.colorScheme.outline, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(value.pretty(), color = if (habit.done(date)) habit.color() else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Text(habit.unit, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

private fun score(habit: Habit): Int {
    var score = 0.0
    val today = LocalDate.now()
    for (i in 29 downTo 0) {
        val complete = habit.done(today.minusDays(i.toLong()))
        score = .95 * score + .05 * if (complete) 100.0 else 0.0
    }
    return score.toInt().coerceIn(0, 100)
}

@Composable
private fun CheckRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!value) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(value, onCheckedChange = onChange)
        Text(label)
    }
}

@Composable
private fun BinaryCheckDialog(habit: Habit, date: LocalDate, onDismiss: () -> Unit, onValue: (Float) -> Unit) {
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(date.format(DateTimeFormatter.ofPattern("M月d日"))) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                OutlinedTextField(note, { note = it }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(22.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    FilledIconButton(onClick = { onValue(1f) }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = habit.color()), modifier = Modifier.size(64.dp)) {
                        Text("✓", color = Color.White, fontSize = 30.sp)
                    }
                    FilledIconButton(onClick = { onValue(0f) }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF9E9E9E)), modifier = Modifier.size(64.dp)) {
                        Text("×", color = Color.White, fontSize = 30.sp)
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun NumericCheckDialog(habit: Habit, date: LocalDate, onDismiss: () -> Unit, onValue: (Float) -> Unit) {
    var text by remember { mutableStateOf((habit.checkins[date.toString()] ?: 0f).pretty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(date.format(DateTimeFormatter.ofPattern("M月d日"))) },
        text = {
            Column {
                OutlinedTextField("", {}, label = { Text("备注") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(habit.unit.ifBlank { "数值" }) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        confirmButton = { TextButton(onClick = { onValue(text.toFloatOrNull() ?: 0f) }) { Text("保存") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HabitFormScreen(initial: Habit?, kind: HabitKind, onBack: () -> Unit, onSave: (Habit) -> Unit) {
    var title by remember(initial?.id) { mutableStateOf(initial?.name ?: "") }
    var question by remember(initial?.id) { mutableStateOf(initial?.question ?: "") }
    var unit by remember(initial?.id) { mutableStateOf(initial?.unit ?: "") }
    var target by remember(initial?.id) { mutableStateOf(initial?.target?.pretty() ?: "") }
    var atLeast by remember(initial?.id) { mutableStateOf(initial?.atLeast ?: true) }
    var colorHex by remember(initial?.id) { mutableStateOf(initial?.colorHex ?: "#3F51B5") }
    var frequency by remember(initial?.id) { mutableStateOf(initial?.frequency ?: "每天") }
    var reminder by remember(initial?.id) { mutableStateOf(initial?.reminder ?: "") }
    var note by remember(initial?.id) { mutableStateOf(initial?.note ?: "") }
    var showErrors by remember { mutableStateOf(false) }
    var frequencyDialog by remember { mutableStateOf(false) }
    var colorDialog by remember { mutableStateOf(false) }
    var reminderDialog by remember { mutableStateOf(false) }
    val invalidTarget = kind == HabitKind.QUANTIFIED && (target.toFloatOrNull() == null)

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LoopBlue, titleContentColor = Color.White),
                title = { Text(if (initial == null) "创建习惯" else "编辑习惯") },
                navigationIcon = { TextButton(onClick = onBack) { Text("‹", color = Color.White, fontSize = 34.sp) } },
                actions = { TextButton(onClick = {
                    showErrors = true
                    if (title.isNotBlank() && !invalidTarget) {
                        onSave(
                            Habit(
                                id = initial?.id ?: UUID.randomUUID().toString(),
                                name = title.trim(),
                                question = question.trim(),
                                kind = kind,
                                unit = unit.trim(),
                                target = if (kind == HabitKind.QUANTIFIED) target.toFloatOrNull() ?: 0f else 1f,
                                atLeast = atLeast,
                                colorHex = colorHex,
                                frequency = frequency,
                                reminder = reminder,
                                note = note.trim(),
                                archived = initial?.archived ?: false,
                                checkins = initial?.checkins ?: emptyMap()
                            )
                        )
                    }
                }) { Text("保存", color = Color.White, fontSize = 16.sp) } }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(bottom = 30.dp)
        ) {
            FormField("名称", title, { title = it }, showErrors && title.isBlank(), "例如：晨间拉伸")
            FormField("问题", question, { question = it }, false, if (kind == HabitKind.BOOLEAN) "你今天完成了吗？" else "你今天完成了多少？")
            if (kind == HabitKind.QUANTIFIED) {
                FormField("单位", unit, { unit = it }, false, "页、分钟、公里…")
                FormField("目标", target, { target = it }, showErrors && invalidTarget, "20", KeyboardType.Decimal)
                SettingValueRow("目标类型", if (atLeast) "至少" else "至多") { atLeast = !atLeast }
            }
            SettingValueRow(
                label = "颜色",
                value = "",
                trailing = {
                    Box(Modifier.size(30.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(colorHex))))
                },
                onClick = { colorDialog = true }
            )
            SettingValueRow("频率", frequency) { frequencyDialog = true }
            SettingValueRow("提醒", reminder.ifBlank { "无" }) { reminderDialog = true }
            FormField("备注（可选）", note, { note = it }, false, "")
            Spacer(Modifier.height(40.dp))
        }
    }

    if (colorDialog) {
        AlertDialog(
            onDismissRequest = { colorDialog = false },
            title = { Text("选择颜色") },
            text = {
                Column {
                    Palette.chunked(5).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            row.forEach { hex ->
                                Box(
                                    Modifier.padding(6.dp).size(42.dp).clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(hex)))
                                        .clickable { colorHex = hex; colorDialog = false },
                                    contentAlignment = Alignment.Center
                                ) { if (hex == colorHex) Text("✓", color = Color.White, fontSize = 20.sp) }
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (frequencyDialog) {
        var pending by remember { mutableStateOf(frequency) }
        val choices = listOf("每天", "每 3 天", "每周 3 次", "每月 10 次", "每 3 天 14 次")
        AlertDialog(
            onDismissRequest = { frequencyDialog = false },
            title = { Text("频率") },
            text = {
                Column {
                    choices.forEach { item ->
                        Row(Modifier.fillMaxWidth().clickable { pending = item }, verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(pending == item, onClick = { pending = item })
                            Text(item)
                        }
                    }
                }
            },
            dismissButton = { TextButton(onClick = { frequencyDialog = false }) { Text("取消") } },
            confirmButton = { TextButton(onClick = { frequency = pending; frequencyDialog = false }) { Text("保存") } }
        )
    }

    if (reminderDialog) {
        var time by remember { mutableStateOf(reminder.ifBlank { "08:00" }) }
        var workdays by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { reminderDialog = false },
            title = { Text("提醒") },
            text = {
                Column {
                    OutlinedTextField(time, { time = it }, label = { Text("时间") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    CheckRow("仅工作日", workdays) { workdays = it }
                    Text("提醒会在习惯尚未完成时显示。", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            },
            dismissButton = { TextButton(onClick = { reminder = ""; reminderDialog = false }) { Text("清除") } },
            confirmButton = { TextButton(onClick = {
                reminder = time + if (workdays) "  工作日" else "  每天"
                reminderDialog = false
            }) { Text("完成") } }
        )
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    isError: Boolean,
    placeholder: String,
    keyboard: KeyboardType = KeyboardType.Text
) {
    TextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        isError = isError,
        supportingText = if (isError) ({ Text("此项为必填项") }) else null,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            errorContainerColor = Color.Transparent
        ),
        singleLine = label != "备注（可选）"
    )
}

@Composable
private fun SettingValueRow(label: String, value: String, trailing: @Composable (() -> Unit)? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(65.dp).clickable(onClick = onClick).padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 16.sp)
        if (trailing != null) trailing() else Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 25.sp)
    }
    HorizontalDivider(Modifier.padding(start = 18.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailScreen(habit: Habit, repo: HabitRepository, onBack: () -> Unit, onEdit: () -> Unit, onDeleted: () -> Unit) {
    var period by remember { mutableStateOf("月") }
    var periodMenu by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var deleteConfirm by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val total = habit.checkins.values.count { if (habit.kind == HabitKind.BOOLEAN) it > 0 else if (habit.atLeast) it >= habit.target else it in 0.0001f..habit.target }
    val monthDays = max(today.dayOfMonth, 1)
    val monthDone = (1..today.dayOfMonth).count { d -> habit.done(today.withDayOfMonth(d)) }
    val pct = ((monthDone * 100f) / monthDays).toInt()

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = habit.color(), titleContentColor = Color.White),
                title = { Text(habit.name) },
                navigationIcon = { TextButton(onClick = onBack) { Text("‹", color = Color.White, fontSize = 34.sp) } },
                actions = {
                    TextButton(onClick = onEdit) { Text("✎", color = Color.White, fontSize = 24.sp) }
                    Box {
                        TextButton(onClick = { menu = true }) { Text("⋮", color = Color.White, fontSize = 27.sp) }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text("导出") }, onClick = { menu = false })
                            DropdownMenuItem(text = { Text("删除") }, onClick = { menu = false; deleteConfirm = true })
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item {
                Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(20.dp)) {
                    if (habit.question.isNotBlank()) Text(habit.question, fontSize = 19.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(15.dp))
                    InfoLine("◷", habit.frequency)
                    if (habit.reminder.isNotBlank()) InfoLine("♢", habit.reminder)
                    if (habit.note.isNotBlank()) InfoLine("≡", habit.note)
                }
                Spacer(Modifier.height(10.dp))
                Text("概览", modifier = Modifier.padding(18.dp, 12.dp), color = habit.color(), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Row(
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { pct / 100f },
                            modifier = Modifier.size(94.dp),
                            color = habit.color(),
                            trackColor = habit.color().copy(alpha = .15f),
                            strokeWidth = 9.dp
                        )
                        Text("$pct%", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(32.dp))
                    StatColumn("本月", "+$pct%")
                    Spacer(Modifier.weight(1f))
                    StatColumn("今年", "+$pct%")
                    Spacer(Modifier.weight(1f))
                    StatColumn("总计", total.toString())
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("成绩", color = habit.color(), fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Box {
                        TextButton(onClick = { periodMenu = true }) { Text(period + "  ▾") }
                        DropdownMenu(expanded = periodMenu, onDismissRequest = { periodMenu = false }) {
                            listOf("日","周","月","季度","年").forEach { p ->
                                DropdownMenuItem(text = { Text(p) }, onClick = { period = p; periodMenu = false })
                            }
                        }
                    }
                }
                Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    PerformanceChart(habit)
                }
                Spacer(Modifier.height(12.dp))
                Text("历史", modifier = Modifier.padding(18.dp, 12.dp), color = habit.color(), fontWeight = FontWeight.Medium)
                Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    HistoryBars(habit)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (deleteConfirm) {
        AlertDialog(
            onDismissRequest = { deleteConfirm = false },
            title = { Text("删除习惯?") },
            text = { Text("习惯将被永久地删除。此操作无法撤消。") },
            dismissButton = { TextButton(onClick = { deleteConfirm = false }) { Text("取消") } },
            confirmButton = { TextButton(onClick = onDeleted) { Text("删除") } }
        )
    }
}

@Composable
private fun InfoLine(symbol: String, value: String) {
    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(symbol, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 18.sp, modifier = Modifier.width(32.dp))
        Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 19.sp, fontWeight = FontWeight.Medium)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}

@Composable
private fun PerformanceChart(habit: Habit) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Canvas(Modifier.fillMaxWidth().height(170.dp)) {
            repeat(4) { i ->
                val y = i * size.height / 3f
                drawLine(Color.Gray.copy(alpha = .25f), Offset(0f, y), Offset(size.width, y), 2f)
            }
            val path = Path()
            repeat(14) { i ->
                val date = LocalDate.now().minusDays((13 - i).toLong())
                val raw = habit.checkins[date.toString()] ?: 0f
                val v = if (habit.kind == HabitKind.BOOLEAN) if (raw > 0f) 1f else 0f else (raw / habit.target.coerceAtLeast(1f)).coerceIn(0f, 1f)
                val x = i * size.width / 13f
                val y = size.height - v * size.height * .82f - 8f
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                drawCircle(habit.color(), 6f, Offset(x, y))
            }
            drawPath(path, habit.color(), style = Stroke(5f, cap = StrokeCap.Round))
        }
        Text("过去 14 天", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun HistoryBars(habit: Habit) {
    Row(Modifier.fillMaxWidth().height(150.dp).padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        repeat(14) { i ->
            val date = LocalDate.now().minusDays((13 - i).toLong())
            val raw = habit.checkins[date.toString()] ?: 0f
            val ratio = if (habit.kind == HabitKind.BOOLEAN) if (raw > 0f) 1f else .06f else (raw / habit.target.coerceAtLeast(1f)).coerceIn(.06f, 1f)
            Box(Modifier.width(10.dp).fillMaxHeight(ratio).background(if (ratio > .06f) habit.color() else habit.color().copy(alpha = .16f), RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(dark: Boolean, onDarkChanged: (Boolean) -> Unit, onBack: () -> Unit, onAbout: () -> Unit) {
    var shortTap by remember { mutableStateOf(false) }
    var extendedDay by remember { mutableStateOf(false) }
    var skipDays by remember { mutableStateOf(false) }
    var questionMark by remember { mutableStateOf(false) }
    var reverseDates by remember { mutableStateOf(false) }
    var pureBlack by remember { mutableStateOf(false) }
    var noAnimations by remember { mutableStateOf(false) }
    var persistentReminder by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = if (dark) Color(0xFF1B1B1B) else LoopBlue, titleContentColor = Color.White),
                title = { Text("设置") },
                navigationIcon = { TextButton(onClick = onBack) { Text("‹", color = Color.White, fontSize = 34.sp) } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            SectionTitle("界面")
            SwitchSetting("短按切换", "轻触一次即可标记习惯", shortTap) { shortTap = it }
            SwitchSetting("一天延长至午夜后几小时", "新的一天从凌晨 3 点开始", extendedDay) { extendedDay = it }
            SwitchSetting("启用跳过天数功能", "", skipDays) { skipDays = it }
            SwitchSetting("缺失数据处显示问号", "", questionMark) { questionMark = it }
            SwitchSetting("反转日期", "", reverseDates) { reverseDates = it }
            SwitchSetting("深色主题", "", dark) { onDarkChanged(it) }
            SwitchSetting("在深色主题中使用纯黑色", "", pureBlack) { pureBlack = it }
            SwitchSetting("禁用动画", "", noAnimations) { noAnimations = it }
            SettingValueRow("小部件透明度", "100%") {}
            SettingValueRow("一周的第一天", "星期一") {}
            SectionTitle("提醒")
            SwitchSetting("常驻通知", "未完成时保持提醒", persistentReminder) { persistentReminder = it }
            SettingValueRow("自定义通知", "") { Toast.makeText(context, "自定义通知", Toast.LENGTH_SHORT).show() }
            SectionTitle("数据库")
            ActionRow("导出完整备份", "将数据保存为 Loop 备份") { Toast.makeText(context, "备份已准备", Toast.LENGTH_SHORT).show() }
            ActionRow("导出 CSV", "导出所有习惯与记录") { Toast.makeText(context, "CSV 已准备", Toast.LENGTH_SHORT).show() }
            ActionRow("导入数据", "支持 Loop、Tickmate、HabitBull 与 Rewire") { Toast.makeText(context, "选择要导入的文件", Toast.LENGTH_SHORT).show() }
            SectionTitle("故障排除")
            ActionRow("生成错误报告", "") { Toast.makeText(context, "未发现错误", Toast.LENGTH_SHORT).show() }
            ActionRow("修复数据库", "") { Toast.makeText(context, "数据库状态良好", Toast.LENGTH_SHORT).show() }
            SectionTitle("链接")
            ActionRow("帮助与常见问题", "") { Toast.makeText(context, "帮助与常见问题", Toast.LENGTH_SHORT).show() }
            ActionRow("在 Google Play 上评分", "") {}
            ActionRow("关于", "") { onAbout() }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 18.dp, top = 24.dp, bottom = 8.dp))
}

@Composable
private fun SwitchSetting(title: String, subtitle: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!value) }.padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            if (subtitle.isNotBlank()) Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Switch(value, onCheckedChange = onChange)
    }
}

@Composable
private fun ActionRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 15.dp)) {
        Column {
            Text(title, fontSize = 16.sp)
            if (subtitle.isNotBlank()) Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LoopBlue, titleContentColor = Color.White),
                title = { Text("关于") },
                navigationIcon = { TextButton(onClick = onBack) { Text("‹", color = Color.White, fontSize = 34.sp) } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(45.dp))
            Box(Modifier.size(118.dp).clip(CircleShape).background(LoopBlue), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(76.dp)) {
                    drawCircle(Color.White, radius = 29f, center = center, style = Stroke(8f))
                    drawArc(Color.White, 25f, 285f, false, style = Stroke(9f, cap = StrokeCap.Round))
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("Loop 习惯记录", fontSize = 25.sp, fontWeight = FontWeight.Medium)
            Text("版本 2.3.1", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            Spacer(Modifier.height(28.dp))
            listOf("在 Google Play 上评分", "发送反馈", "帮助翻译 Loop", "在 GitHub 上查看源代码", "隐私政策", "开发者").forEach {
                Text(it, color = MaterialTheme.colorScheme.primary, fontSize = 17.sp, modifier = Modifier.fillMaxWidth().clickable { }.padding(horizontal = 28.dp, vertical = 17.dp))
                HorizontalDivider()
            }
        }
    }
}
