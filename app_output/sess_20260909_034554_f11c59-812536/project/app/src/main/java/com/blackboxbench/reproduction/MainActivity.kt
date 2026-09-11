@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.blackboxbench.reproduction

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale
import kotlin.math.max

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ReproducedApp() }
    }
}

enum class HabitKind { BOOLEAN, NUMBER }
enum class AppScreen { MAIN, FORM, DETAIL, SETTINGS, ABOUT, HELP }

data class Habit(
    val id: Long = System.currentTimeMillis(),
    val kind: HabitKind = HabitKind.BOOLEAN,
    val title: String = "",
    val question: String = "",
    val unit: String = "",
    val target: Double = 1.0,
    val atMost: Boolean = false,
    val colorIndex: Int = 0,
    val frequency: String = "每天",
    val reminder: String = "关闭",
    val notes: String = "",
    val archived: Boolean = false,
    val entries: Map<String, Double> = emptyMap()
)

private val habitColors = listOf(
    Color(0xFFEF5350), Color(0xFFE57373), Color(0xFFEC407A), Color(0xFFAB47BC), Color(0xFF7E57C2),
    Color(0xFF5C6BC0), Color(0xFF42A5F5), Color(0xFF29B6F6), Color(0xFF26C6DA), Color(0xFF26A69A),
    Color(0xFF66BB6A), Color(0xFF9CCC65), Color(0xFFD4E157), Color(0xFFFFCA28), Color(0xFFFFA726),
    Color(0xFFFF7043), Color(0xFF8D6E63), Color(0xFF78909C), Color(0xFF9E9E9E), Color(0xFF5E35B1)
)

private fun SharedPreferences.loadHabits(): List<Habit> {
    val raw = getString("habits", "[]") ?: "[]"
    return try {
        val array = JSONArray(raw)
        (0 until array.length()).map { index ->
            val obj = array.getJSONObject(index)
            val values = mutableMapOf<String, Double>()
            val ent = obj.optJSONObject("entries") ?: JSONObject()
            val keys = ent.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                values[key] = ent.optDouble(key, 0.0)
            }
            Habit(
                id = obj.optLong("id", System.currentTimeMillis() + index),
                kind = if (obj.optString("kind") == "NUMBER") HabitKind.NUMBER else HabitKind.BOOLEAN,
                title = obj.optString("title"),
                question = obj.optString("question"),
                unit = obj.optString("unit"),
                target = obj.optDouble("target", 1.0),
                atMost = obj.optBoolean("atMost", false),
                colorIndex = obj.optInt("colorIndex", 0),
                frequency = obj.optString("frequency", "每天"),
                reminder = obj.optString("reminder", "关闭"),
                notes = obj.optString("notes"),
                archived = obj.optBoolean("archived", false),
                entries = values
            )
        }
    } catch (_: Exception) { emptyList() }
}

private fun SharedPreferences.saveHabits(habits: List<Habit>) {
    val array = JSONArray()
    habits.forEach { habit ->
        val entries = JSONObject()
        habit.entries.forEach { (key, value) -> entries.put(key, value) }
        array.put(JSONObject().apply {
            put("id", habit.id)
            put("kind", habit.kind.name)
            put("title", habit.title)
            put("question", habit.question)
            put("unit", habit.unit)
            put("target", habit.target)
            put("atMost", habit.atMost)
            put("colorIndex", habit.colorIndex)
            put("frequency", habit.frequency)
            put("reminder", habit.reminder)
            put("notes", habit.notes)
            put("archived", habit.archived)
            put("entries", entries)
        })
    }
    edit().putString("habits", array.toString()).apply()
}

@Composable
fun ReproducedApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("loop", Context.MODE_PRIVATE) }
    var onboardingDone by remember { mutableStateOf(prefs.getBoolean("onboarding_done", false)) }
    var dark by remember { mutableStateOf(prefs.getBoolean("dark", false)) }

    SideEffect {
        (context as? ComponentActivity)?.window?.apply {
            statusBarColor = android.graphics.Color.rgb(13, 13, 13)
            navigationBarColor = if (dark) android.graphics.Color.rgb(32, 32, 32) else android.graphics.Color.rgb(245, 245, 245)
        }
    }

    BenchmarkAppTheme(darkTheme = dark) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (!onboardingDone) {
                Onboarding {
                    prefs.edit().putBoolean("onboarding_done", true).apply()
                    onboardingDone = true
                }
            } else {
                HabitApplication(
                    prefs = prefs,
                    dark = dark,
                    onDarkChange = {
                        dark = it
                        prefs.edit().putBoolean("dark", it).apply()
                    }
                )
            }
        }
    }
}

@Composable
private fun Onboarding(onDone: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val backgrounds = listOf(Color(0xFF3F51B5), Color(0xFFEF6C00), Color(0xFF7E57C2))
    val titles = listOf("欢迎", "养成一些新习惯", "记录你的进步")
    val descriptions = listOf(
        "Loop 习惯记录能帮你养成和保持好习惯。",
        "每当你完成一项习惯后，就在应用上做一个标记。",
        "利用详细的图表和统计数据，了解你的习惯是如何随时间改进的。"
    )
    Column(
        Modifier.fillMaxSize().background(backgrounds[page]).statusBarsPadding().navigationBarsPadding().padding(28.dp)
    ) {
        Text(titles[page], color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(top = 30.dp))
        Spacer(Modifier.height(84.dp))
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            when (page) {
                0 -> LoopMark(Color.White, 188.dp)
                1 -> ExampleChecks()
                else -> ExampleGraph()
            }
        }
        Text(descriptions[page], color = Color.White, fontSize = 19.sp, lineHeight = 28.sp)
        Spacer(Modifier.height(46.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { if (page > 0) page-- else page = 0 }) {
                Text(if (page == 0) "↶" else "←", color = Color.White, fontSize = 30.sp)
            }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                repeat(3) { i ->
                    Box(Modifier.size(if (i == page) 11.dp else 8.dp).clip(CircleShape).background(if (i == page) Color.White else Color.White.copy(alpha = .42f)))
                }
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { if (page < 2) page++ else onDone() }) {
                Text(if (page < 2) "→" else "✓", color = Color.White, fontSize = 31.sp)
            }
        }
    }
}

@Composable
private fun LoopMark(color: Color, size: androidx.compose.ui.unit.Dp) {
    Canvas(Modifier.size(size)) {
        drawArc(color.copy(alpha = .25f), -90f, 360f, false, style = Stroke(20.dp.toPx(), cap = StrokeCap.Round))
        drawArc(color, -80f, 278f, false, style = Stroke(20.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(color, 10.dp.toPx(), center = center.copy(x = center.x + size.toPx() * .42f))
    }
}

@Composable
private fun ExampleChecks() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Meditate", color = Color.White, fontSize = 27.sp)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            listOf("✓","✓","×","✓","✓","✓","×").forEach {
                Text(it, color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ExampleGraph() {
    Canvas(Modifier.fillMaxWidth(.86f).height(230.dp)) {
        val pts = listOf(.82f,.72f,.78f,.56f,.62f,.36f,.18f)
        for (i in 0 until pts.lastIndex) {
            drawLine(
                Color(0xFFFFEB3B),
                start = androidx.compose.ui.geometry.Offset(size.width * i / 6f, size.height * pts[i]),
                end = androidx.compose.ui.geometry.Offset(size.width * (i + 1) / 6f, size.height * pts[i + 1]),
                strokeWidth = 7.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HabitApplication(prefs: SharedPreferences, dark: Boolean, onDarkChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    val habits = remember { mutableStateListOf<Habit>().apply { addAll(prefs.loadHabits()) } }
    var screen by remember { mutableStateOf(AppScreen.MAIN) }
    var activeId by remember { mutableLongStateOf(-1L) }
    var formKind by remember { mutableStateOf(HabitKind.BOOLEAN) }
    var editHabit by remember { mutableStateOf<Habit?>(null) }
    var shortPress by remember { mutableStateOf(prefs.getBoolean("short_press", false)) }

    fun persist() = prefs.saveHabits(habits)
    fun replace(updated: Habit) {
        val index = habits.indexOfFirst { it.id == updated.id }
        if (index >= 0) habits[index] = updated else habits.add(updated)
        persist()
    }

    when (screen) {
        AppScreen.MAIN -> MainScreen(
            habits = habits,
            dark = dark,
            shortPress = shortPress,
            onDarkChange = onDarkChange,
            onAdd = { kind ->
                formKind = kind
                editHabit = null
                screen = AppScreen.FORM
            },
            onOpen = { activeId = it; screen = AppScreen.DETAIL },
            onEntry = { id, date, value ->
                habits.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { index ->
                    habits[index] = habits[index].copy(entries = habits[index].entries + (date to value))
                    persist()
                }
            },
            onArchive = { ids, archived ->
                ids.forEach { id ->
                    val i = habits.indexOfFirst { it.id == id }
                    if (i >= 0) habits[i] = habits[i].copy(archived = archived)
                }
                persist()
            },
            onDelete = { ids -> habits.removeAll { it.id in ids }; persist() },
            onSettings = { screen = AppScreen.SETTINGS },
            onAbout = { screen = AppScreen.ABOUT },
            onHelp = { screen = AppScreen.HELP }
        )
        AppScreen.FORM -> HabitForm(
            kind = editHabit?.kind ?: formKind,
            existing = editHabit,
            onBack = { screen = if (editHabit == null) AppScreen.MAIN else AppScreen.DETAIL },
            onSave = { newHabit ->
                replace(newHabit)
                activeId = newHabit.id
                if (newHabit.reminder != "关闭" && Build.VERSION.SDK_INT >= 33 &&
                    context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    (context as? ComponentActivity)?.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 7)
                }
                screen = AppScreen.MAIN
            }
        )
        AppScreen.DETAIL -> {
            val habit = habits.firstOrNull { it.id == activeId }
            if (habit == null) {
                LaunchedEffect(Unit) { screen = AppScreen.MAIN }
            } else {
                HabitDetail(
                    habit = habit,
                    onBack = { screen = AppScreen.MAIN },
                    onEdit = { editHabit = habit; screen = AppScreen.FORM },
                    onDelete = { habits.removeAll { it.id == habit.id }; persist(); screen = AppScreen.MAIN },
                    onEntry = { date, value -> replace(habit.copy(entries = habit.entries + (date to value))) }
                )
            }
        }
        AppScreen.SETTINGS -> SettingsScreen(
            shortPress = shortPress,
            onShortPress = {
                shortPress = it
                prefs.edit().putBoolean("short_press", it).apply()
            },
            onBack = { screen = AppScreen.MAIN }
        )
        AppScreen.ABOUT -> AboutScreen(onBack = { screen = AppScreen.MAIN })
        AppScreen.HELP -> HelpScreen(onBack = { screen = AppScreen.MAIN })
    }
}

@Composable
private fun AppTopBar(
    title: String,
    color: Color,
    onBack: (() -> Unit)? = null,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {}
) {
    Row(
        Modifier.fillMaxWidth().height(78.dp).background(color).padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp), modifier = Modifier.width(48.dp)) {
                Text("←", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Light)
            }
        }
        Text(title, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        actions()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MainScreen(
    habits: List<Habit>,
    dark: Boolean,
    shortPress: Boolean,
    onDarkChange: (Boolean) -> Unit,
    onAdd: (HabitKind) -> Unit,
    onOpen: (Long) -> Unit,
    onEntry: (Long, String, Double) -> Unit,
    onArchive: (Set<Long>, Boolean) -> Unit,
    onDelete: (Set<Long>) -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    onHelp: () -> Unit
) {
    var addDialog by remember { mutableStateOf(false) }
    var filterMenu by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }
    var hideArchived by remember { mutableStateOf(true) }
    var hideCompleted by remember { mutableStateOf(false) }
    var sort by remember { mutableStateOf("手动") }
    var dayOffset by remember { mutableIntStateOf(0) }
    var entryTarget by remember { mutableStateOf<Pair<Habit, LocalDate>?>(null) }
    val selected = remember { mutableStateListOf<Long>() }
    var selectionMenu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val today = LocalDate.now()
    val dates = (0..4).map { today.minusDays((dayOffset * 5L) + it) }
    val currentKey = today.toString()
    val visible = habits.filter {
        (!hideArchived || !it.archived) &&
        (!hideCompleted || !(it.kind == HabitKind.BOOLEAN && it.entries[currentKey] == 1.0))
    }.let { list ->
        when (sort) {
            "按名称" -> list.sortedBy { it.title.lowercase() }
            "按颜色" -> list.sortedBy { it.colorIndex }
            "按分数" -> list.sortedByDescending { it.entries.values.sum() }
            "按状态" -> list.sortedBy { it.archived }
            else -> list
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        AppTopBar(
            title = if (selected.isEmpty()) "习惯" else selected.size.toString(),
            color = if (dark) Color(0xFF0D0D0D) else Color(0xFF3F51B5),
            onBack = if (selected.isNotEmpty()) ({ selected.clear() }) else null
        ) {
            if (selected.isEmpty()) {
                TextButton(onClick = { addDialog = true }) { Text("+", color = Color.White, fontSize = 37.sp) }
                TextButton(onClick = { filterMenu = true }) { Text("≡", color = Color.White, fontSize = 31.sp) }
                TextButton(onClick = { moreMenu = true }) { Text("⋮", color=Color.White,fontSize=31.sp) }
            } else {
                TextButton(onClick={}){Text("✎",color=Color.White,fontSize=25.sp)}
                TextButton(onClick={}){Text("●",color=Color.White,fontSize=22.sp)}
                Box {
                    TextButton(onClick={selectionMenu=true}){Text("⋮",color=Color.White,fontSize=30.sp)}
                    DropdownMenu(expanded=selectionMenu,onDismissRequest={selectionMenu=false}) {
                        val allArchived=selected.all{id->habits.firstOrNull{it.id==id}?.archived==true}
                        DropdownMenuItem(text={Text(if(allArchived)"取消存档" else "存档")},onClick={
                            onArchive(selected.toSet(),!allArchived);selected.clear();selectionMenu=false
                        })
                        DropdownMenuItem(text={Text("删除")},onClick={selectionMenu=false;confirmDelete=true})
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().height(62.dp).background(MaterialTheme.colorScheme.surfaceVariant)
                .pointerInput(Unit) {
                    var drag=0f
                    detectHorizontalDragGestures(
                        onHorizontalDrag={_,amount->drag+=amount},
                        onDragEnd={
                            if(drag < -80) dayOffset++
                            if(drag > 80) dayOffset=max(0,dayOffset-1)
                            drag=0f
                        }
                    )
                },
            verticalAlignment=Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(170.dp))
            dates.forEach { date ->
                Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally) {
                    Text(date.dayOfWeek.getDisplayName(DateTextStyle.SHORT,Locale.CHINA).replace("星期","周"),fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onSurface.copy(.65f))
                    Text(date.dayOfMonth.toString(),fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onSurface.copy(.65f))
                }
            }
        }

        if (visible.isEmpty()) {
            Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                Text("★",fontSize=68.sp,color=MaterialTheme.colorScheme.onBackground.copy(.28f))
                Text("你还没有任何习惯",fontSize=18.sp,color=MaterialTheme.colorScheme.onBackground.copy(.54f))
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(2.dp)) {
                items(visible,key={it.id}) { habit ->
                    HabitRow(
                        habit=habit,
                        dates=dates,
                        selected=habit.id in selected,
                        shortPress=shortPress,
                        onOpen={onOpen(habit.id)},
                        onLongClick={if(habit.id in selected) selected.remove(habit.id) else selected.add(habit.id)},
                        onDirect={date,value->onEntry(habit.id,date.toString(),value)},
                        onDialog={date->entryTarget=habit to date}
                    )
                }
            }
        }
    }

    if(filterMenu) {
        AlertDialog(
            onDismissRequest={filterMenu=false},
            title={Text("筛选习惯")},
            text={
                Column {
                    Row(
                        Modifier.fillMaxWidth().combinedClickable(onClick={hideArchived=!hideArchived},onLongClick={}).padding(vertical=7.dp),
                        verticalAlignment=Alignment.CenterVertically
                    ) {
                        Checkbox(hideArchived,{hideArchived=it})
                        Text("隐藏已存档",fontSize=18.sp)
                    }
                    Row(
                        Modifier.fillMaxWidth().combinedClickable(onClick={hideCompleted=!hideCompleted},onLongClick={}).padding(vertical=7.dp),
                        verticalAlignment=Alignment.CenterVertically
                    ) {
                        Checkbox(hideCompleted,{hideCompleted=it})
                        Text("隐藏已完成",fontSize=18.sp)
                    }
                    TextButton(onClick={filterMenu=false;sortMenu=true},modifier=Modifier.fillMaxWidth()) {
                        Text("排序    ›",fontSize=18.sp)
                    }
                }
            },
            confirmButton={TextButton(onClick={filterMenu=false}){Text("完成")}}
        )
    }

    if(sortMenu) {
        AlertDialog(
            onDismissRequest={sortMenu=false},
            title={Text("排序")},
            text={
                Column {
                    listOf("手动","按名称","按颜色","按分数","按状态").forEach { option ->
                        Row(
                            Modifier.fillMaxWidth().combinedClickable(onClick={sort=option;sortMenu=false},onLongClick={}).padding(vertical=6.dp),
                            verticalAlignment=Alignment.CenterVertically
                        ) {
                            RadioButton(selected=sort==option,onClick={sort=option;sortMenu=false})
                            Text(option,fontSize=18.sp)
                        }
                    }
                }
            },
            confirmButton={TextButton(onClick={sortMenu=false}){Text("取消")}}
        )
    }

    if(moreMenu) {
        AlertDialog(
            onDismissRequest={moreMenu=false},
            title={Text("菜单")},
            text={
                Column {
                    Row(
                        Modifier.fillMaxWidth().combinedClickable(onClick={onDarkChange(!dark)},onLongClick={}).padding(vertical=4.dp),
                        verticalAlignment=Alignment.CenterVertically
                    ) {
                        Text("深色主题",fontSize=18.sp,modifier=Modifier.weight(1f))
                        Checkbox(dark,{onDarkChange(it)})
                    }
                    TextButton(onClick={moreMenu=false;onSettings()},modifier=Modifier.fillMaxWidth()){Text("设置",fontSize=18.sp)}
                    TextButton(onClick={moreMenu=false;onHelp()},modifier=Modifier.fillMaxWidth()){Text("帮助 & 常见问题",fontSize=18.sp)}
                    TextButton(onClick={moreMenu=false;onAbout()},modifier=Modifier.fillMaxWidth()){Text("关于应用",fontSize=18.sp)}
                }
            },
            confirmButton={TextButton(onClick={moreMenu=false}){Text("关闭")}}
        )
    }

    if(addDialog) {
        AlertDialog(
            onDismissRequest={addDialog=false},
            title={Text("新建习惯")},
            text={
                Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    HabitTypeCard("✓","完成与否","比如：你今天锻炼了吗？") { addDialog=false;onAdd(HabitKind.BOOLEAN) }
                    HabitTypeCard("12","可量化的","比如：今天走了多少公里？") { addDialog=false;onAdd(HabitKind.NUMBER) }
                }
            },
            confirmButton={}
        )
    }

    entryTarget?.let { pair ->
        EntryDialog(
            habit=pair.first,
            date=pair.second,
            onDismiss={entryTarget=null},
            onSave={value->onEntry(pair.first.id,pair.second.toString(),value);entryTarget=null}
        )
    }

    if(confirmDelete) {
        AlertDialog(
            onDismissRequest={confirmDelete=false},
            title={Text("删除习惯?")},
            text={Text("永久删除选中的习惯及其全部记录。此操作无法撤消。")},
            dismissButton={TextButton(onClick={confirmDelete=false}){Text("未完成")}},
            confirmButton={TextButton(onClick={onDelete(selected.toSet());selected.clear();confirmDelete=false}){Text("完成了")}}
        )
    }
}

@Composable
private fun HabitTypeCard(icon:String,title:String,subtitle:String,onClick:()->Unit) {
    Card(onClick=onClick,colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant),modifier=Modifier.fillMaxWidth()) {
        Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically) {
            Text(icon,fontSize=32.sp,color=MaterialTheme.colorScheme.primary,modifier=Modifier.width(56.dp))
            Column { Text(title,fontSize=20.sp);Text(subtitle,fontSize=14.sp,color=MaterialTheme.colorScheme.onSurface.copy(.65f)) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HabitRow(
    habit: Habit,
    dates: List<LocalDate>,
    selected: Boolean,
    shortPress: Boolean,
    onOpen:()->Unit,
    onLongClick:()->Unit,
    onDirect:(LocalDate,Double)->Unit,
    onDialog:(LocalDate)->Unit
) {
    val color=habitColors[habit.colorIndex.coerceIn(habitColors.indices)]
    val trackColor=MaterialTheme.colorScheme.onSurface.copy(.2f)
    Row(
        Modifier.fillMaxWidth().height(84.dp)
            .background(if(selected) color.copy(.22f) else MaterialTheme.colorScheme.surface)
            .then(if(selected)Modifier.border(2.dp,color) else Modifier),
        verticalAlignment=Alignment.CenterVertically
    ) {
        Row(
            Modifier.width(170.dp).fillMaxHeight().combinedClickable(onClick=onOpen,onLongClick=onLongClick).padding(horizontal=18.dp),
            verticalAlignment=Alignment.CenterVertically
        ) {
            Canvas(Modifier.size(30.dp)) {
                drawCircle(trackColor,style=Stroke(6.dp.toPx()))
                val done=habit.entries.values.count{it>0}
                drawArc(color,-90f,(done.coerceAtMost(12))*30f,false,style=Stroke(6.dp.toPx(),cap=StrokeCap.Round))
            }
            Spacer(Modifier.width(12.dp))
            Text(habit.title,color=if(habit.archived)MaterialTheme.colorScheme.onSurface.copy(.45f) else color,fontSize=20.sp,maxLines=2)
        }
        dates.forEach { date ->
            val key=date.toString()
            val value=habit.entries[key]
            Box(
                Modifier.weight(1f).fillMaxHeight().combinedClickable(
                    onClick={
                        if(habit.kind==HabitKind.BOOLEAN && shortPress) onDirect(date,if(value==1.0)-1.0 else 1.0)
                        else onDialog(date)
                    },
                    onLongClick={
                        if(habit.kind==HabitKind.BOOLEAN) onDirect(date,if(value==1.0)-1.0 else 1.0)
                        else onDialog(date)
                    }
                ),
                contentAlignment=Alignment.Center
            ) {
                if(habit.kind==HabitKind.BOOLEAN) {
                    Text(if(value==1.0)"✓" else "×",fontSize=27.sp,fontWeight=FontWeight.Bold,color=if(value==1.0)color else MaterialTheme.colorScheme.onSurface.copy(.18f))
                } else {
                    Column(horizontalAlignment=Alignment.CenterHorizontally) {
                        Text(formatNumber(value?:0.0),fontSize=18.sp,fontWeight=FontWeight.Bold,color=if(value!=null)color else MaterialTheme.colorScheme.onSurface.copy(.22f))
                        Text(habit.unit,fontSize=13.sp,color=if(value!=null)color else MaterialTheme.colorScheme.onSurface.copy(.22f))
                    }
                }
            }
        }
    }
}

private fun formatNumber(value:Double):String = if(value%1.0==0.0)value.toInt().toString() else String.format(Locale.US,"%.1f",value)

@Composable
private fun EntryDialog(habit:Habit,date:LocalDate,onDismiss:()->Unit,onSave:(Double)->Unit) {
    var number by remember(habit.id,date) { mutableStateOf(formatNumber(habit.entries[date.toString()]?:0.0)) }
    if(habit.kind==HabitKind.BOOLEAN) {
        AlertDialog(
            onDismissRequest=onDismiss,
            title={Text("备注")},
            text={Column { Text(date.toString(),color=MaterialTheme.colorScheme.onSurface.copy(.55f));Spacer(Modifier.height(34.dp)) }},
            dismissButton={TextButton(onClick={onSave(-1.0)}){Text("×",fontSize=34.sp,color=Color(0xFFEF5350))}},
            confirmButton={TextButton(onClick={onSave(1.0)}){Text("✓",fontSize=34.sp,color=Color(0xFF66BB6A))}}
        )
    } else {
        AlertDialog(
            onDismissRequest=onDismiss,
            title={Text("备注")},
            text={
                Column {
                    Text(date.toString(),color=MaterialTheme.colorScheme.onSurface.copy(.55f))
                    OutlinedTextField(
                        value=number,onValueChange={number=it},
                        label={Text(habit.unit.ifBlank{"数值"})},
                        keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),
                        singleLine=true
                    )
                }
            },
            dismissButton={TextButton(onClick=onDismiss){Text("取消")}},
            confirmButton={TextButton(onClick={onSave(number.replace(",",".").toDoubleOrNull()?:0.0)}){Text("保存")}}
        )
    }
}


@Composable
private fun HabitForm(kind:HabitKind,existing:Habit?,onBack:()->Unit,onSave:(Habit)->Unit) {
    var title by remember(existing?.id,kind){mutableStateOf(existing?.title?:"")}
    var question by remember(existing?.id,kind){mutableStateOf(existing?.question?:"")}
    var unit by remember(existing?.id,kind){mutableStateOf(existing?.unit?:"")}
    var target by remember(existing?.id,kind){mutableStateOf(if(existing==null && kind==HabitKind.NUMBER)"" else formatNumber(existing?.target?:1.0))}
    var atMost by remember(existing?.id,kind){mutableStateOf(existing?.atMost?:false)}
    var colorIndex by remember(existing?.id,kind){mutableIntStateOf(existing?.colorIndex ?: if(kind==HabitKind.NUMBER)6 else 0)}
    var frequency by remember(existing?.id,kind){mutableStateOf(existing?.frequency?:"每天")}
    var reminder by remember(existing?.id,kind){mutableStateOf(existing?.reminder?:"关闭")}
    var notes by remember(existing?.id,kind){mutableStateOf(existing?.notes?:"")}
    var colorDialog by remember{mutableStateOf(false)}
    var frequencyDialog by remember{mutableStateOf(false)}
    var reminderDialog by remember{mutableStateOf(false)}
    var targetMenu by remember{mutableStateOf(false)}
    var triedSave by remember{mutableStateOf(false)}
    val color=habitColors[colorIndex]
    val titleError=triedSave && title.isBlank()
    val targetError=triedSave && kind==HabitKind.NUMBER && target.toDoubleOrNull()==null

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        AppTopBar(
            title=if(existing==null)"新建习惯" else "编辑习惯",
            color=color,
            onBack=onBack
        ) {
            TextButton(onClick={
                triedSave=true
                if(!titleError && title.isNotBlank() && (kind==HabitKind.BOOLEAN || target.toDoubleOrNull()!=null)) {
                    onSave(Habit(
                        id=existing?.id?:System.currentTimeMillis(),
                        kind=kind,title=title.trim(),question=question,unit=unit,
                        target=target.toDoubleOrNull()?:1.0,atMost=atMost,colorIndex=colorIndex,
                        frequency=frequency,reminder=reminder,notes=notes,
                        archived=existing?.archived?:false,entries=existing?.entries?:emptyMap()
                    ))
                }
            }) { Text("保存",color=Color.White,fontSize=17.sp,fontWeight=FontWeight.Bold) }
        }
        LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(11.dp)) {
            item {
                OutlinedTextField(
                    value=title,onValueChange={title=it},modifier=Modifier.fillMaxWidth(),
                    label={Text("名称")},singleLine=true,isError=titleError,
                    supportingText={if(titleError) Text("请输入习惯名称")},
                    trailingIcon={if(titleError) Text("!",color=MaterialTheme.colorScheme.error,fontWeight=FontWeight.Bold)}
                )
            }
            item {
                Row(
                    Modifier.fillMaxWidth().combinedClickable(onClick={colorDialog=true},onLongClick={}).padding(vertical=8.dp),
                    verticalAlignment=Alignment.CenterVertically
                ) {
                    Text("颜色",modifier=Modifier.weight(1f),fontSize=17.sp)
                    Box(Modifier.size(34.dp).clip(CircleShape).background(color))
                }
            }
            item {
                OutlinedTextField(value=question,onValueChange={question=it},modifier=Modifier.fillMaxWidth(),label={Text("问题")},singleLine=true)
            }
            if(kind==HabitKind.NUMBER) {
                item {
                    OutlinedTextField(value=unit,onValueChange={unit=it},modifier=Modifier.fillMaxWidth(),label={Text("单位")},singleLine=true)
                }
                item {
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Box {
                            TextButton(onClick={targetMenu=true}) { Text(if(atMost)"至多 ▾" else "至少 ▾",color=color) }
                            DropdownMenu(expanded=targetMenu,onDismissRequest={targetMenu=false}) {
                                DropdownMenuItem(text={Text("至少")},onClick={atMost=false;targetMenu=false})
                                DropdownMenuItem(text={Text("至多")},onClick={atMost=true;targetMenu=false})
                            }
                        }
                        OutlinedTextField(
                            value=target,onValueChange={target=it},modifier=Modifier.weight(1f),
                            label={Text("目标")},isError=targetError,singleLine=true,
                            keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),
                            trailingIcon={if(targetError) Text("!",color=MaterialTheme.colorScheme.error,fontWeight=FontWeight.Bold)}
                        )
                    }
                }
            }
            item { Divider() }
            item {
                SelectRow("频率",frequency,color){frequencyDialog=true}
            }
            item {
                SelectRow("提醒",reminder,color){reminderDialog=true}
            }
            item { Divider() }
            item {
                OutlinedTextField(
                    value=notes,onValueChange={notes=it},modifier=Modifier.fillMaxWidth().height(132.dp),
                    label={Text("备注（可选）")}
                )
            }
        }
    }

    if(colorDialog) {
        AlertDialog(
            onDismissRequest={colorDialog=false},title={Text("选择颜色")},
            text={
                Column(verticalArrangement=Arrangement.spacedBy(13.dp)) {
                    for(row in 0 until 5) {
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly) {
                            for(col in 0 until 4) {
                                val i=row*4+col
                                Box(
                                    Modifier.size(43.dp).clip(CircleShape).background(habitColors[i])
                                        .combinedClickable(onClick={colorIndex=i;colorDialog=false},onLongClick={}),
                                    contentAlignment=Alignment.Center
                                ) { if(i==colorIndex)Text("✓",color=Color.White,fontSize=24.sp,fontWeight=FontWeight.Bold) }
                            }
                        }
                    }
                }
            },confirmButton={}
        )
    }

    if(frequencyDialog) {
        ChoiceDialog(
            title="频率",
            options=listOf("每天","每 3 天","每周 3 次","每月 10 次","每 14 天 3 次"),
            selected=frequency,
            onDismiss={frequencyDialog=false},
            onSelect={frequency=it;frequencyDialog=false}
        )
    }

    if(reminderDialog) {
        ChoiceDialog(
            title="提醒时间",
            options=listOf("关闭","08:00 每天","09:30 工作日","20:00 每天"),
            selected=reminder,
            onDismiss={reminderDialog=false},
            onSelect={reminder=it;reminderDialog=false}
        )
    }
}

@Composable
private fun SelectRow(label:String,value:String,color:Color,onClick:()->Unit) {
    Row(
        Modifier.fillMaxWidth().combinedClickable(onClick=onClick,onLongClick={}).padding(vertical=16.dp),
        verticalAlignment=Alignment.CenterVertically
    ) {
        Text(label,fontSize=17.sp,modifier=Modifier.weight(1f))
        Text(value,color=color,fontSize=17.sp)
        Spacer(Modifier.width(10.dp))
        Text("›",fontSize=28.sp,color=MaterialTheme.colorScheme.onSurface.copy(.45f))
    }
}

@Composable
private fun ChoiceDialog(title:String,options:List<String>,selected:String,onDismiss:()->Unit,onSelect:(String)->Unit) {
    AlertDialog(
        onDismissRequest=onDismiss,title={Text(title)},
        text={
            Column {
                options.forEach { option ->
                    Row(
                        Modifier.fillMaxWidth().combinedClickable(onClick={onSelect(option)},onLongClick={}).padding(vertical=5.dp),
                        verticalAlignment=Alignment.CenterVertically
                    ) {
                        RadioButton(selected=selected==option,onClick={onSelect(option)})
                        Text(option,fontSize=17.sp)
                    }
                }
            }
        },
        confirmButton={TextButton(onClick=onDismiss){Text("取消")}}
    )
}

@Composable
private fun HabitDetail(
    habit:Habit,
    onBack:()->Unit,
    onEdit:()->Unit,
    onDelete:()->Unit,
    onEntry:(String,Double)->Unit
) {
    val color=habitColors[habit.colorIndex]
    var moreMenu by remember{mutableStateOf(false)}
    var confirmDelete by remember{mutableStateOf(false)}
    var achievementPeriod by remember{mutableStateOf(if(habit.kind==HabitKind.BOOLEAN)"月" else "月")}
    var historyPeriod by remember{mutableStateOf(if(habit.kind==HabitKind.BOOLEAN)"周" else "日")}
    var achievementMenu by remember{mutableStateOf(false)}
    var historyMenu by remember{mutableStateOf(false)}
    var editCalendar by remember{mutableStateOf(false)}
    val positives=habit.entries.values.count{it>0}
    val total=habit.entries.values.filter{it>0}.sum()
    val score=(positives*1.5).toInt().coerceAtMost(100)

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        AppTopBar(habit.title,color,onBack) {
            TextButton(onClick=onEdit){Text("✎",color=Color.White,fontSize=28.sp)}
            Box {
                TextButton(onClick={moreMenu=true}){Text("⋮",color=Color.White,fontSize=31.sp)}
                DropdownMenu(expanded=moreMenu,onDismissRequest={moreMenu=false}) {
                    DropdownMenuItem(text={Text("导出")},onClick={moreMenu=false})
                    DropdownMenuItem(text={Text("删除")},onClick={moreMenu=false;confirmDelete=true})
                }
            }
        }
        LazyColumn(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(2.dp)) {
            item {
                Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(20.dp)) {
                    Text(habit.question,color=color,fontSize=21.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement=Arrangement.spacedBy(24.dp)) {
                        Text("▣  "+habit.frequency,color=MaterialTheme.colorScheme.onSurface.copy(.62f),fontSize=17.sp)
                        Text("♟  "+habit.reminder,color=MaterialTheme.colorScheme.onSurface.copy(.62f),fontSize=17.sp)
                    }
                }
            }
            if(habit.notes.isNotBlank()) {
                item { Text(habit.notes,modifier=Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(20.dp),fontSize=18.sp) }
            }
            if(habit.kind==HabitKind.BOOLEAN) {
                item { BooleanOverview(color,score,positives) }
            } else {
                item { NumberGoals(habit,color,total) }
            }
            item {
                ChartCard(
                    title="成绩",color=color,period=achievementPeriod,
                    menuOpen=achievementMenu,onMenuChange={achievementMenu=it},
                    options=listOf("日","周","月","季度","年"),
                    onPeriod={achievementPeriod=it;achievementMenu=false},
                    values=listOf(0f,.1f,.12f,.07f,max(.12f,score/100f))
                )
            }
            item {
                HistoryCard(
                    color=color,period=historyPeriod,menuOpen=historyMenu,onMenuChange={historyMenu=it},
                    options=listOf("日","周","月","季度","年"),onPeriod={historyPeriod=it;historyMenu=false},
                    habit=habit
                )
            }
            if(habit.kind==HabitKind.BOOLEAN) {
                item {
                    CalendarCard(habit,color,onEdit={editCalendar=true})
                }
                item { StreakCard(habit,color) }
                item { FrequencyCard(color,habit) }
            }
            item { Spacer(Modifier.height(28.dp)) }
        }
    }

    if(confirmDelete) {
        AlertDialog(
            onDismissRequest={confirmDelete=false},title={Text("删除习惯?")},
            text={Text("永久删除这个习惯及其全部记录。此操作无法撤消。")},
            dismissButton={TextButton(onClick={confirmDelete=false}){Text("未完成")}},
            confirmButton={TextButton(onClick=onDelete){Text("完成了")}}
        )
    }

    if(editCalendar) {
        AlertDialog(
            onDismissRequest={editCalendar=false},
            text={CalendarGrid(habit=habit,color=color,editable=true,onDate={date->
                val old=habit.entries[date.toString()]
                onEntry(date.toString(),if(old==1.0)-1.0 else 1.0)
            })},
            confirmButton={TextButton(onClick={editCalendar=false}){Text("完成")}}
        )
    }
}

@Composable
private fun BooleanOverview(color:Color,score:Int,total:Int) {
    val trackColor=MaterialTheme.colorScheme.onSurface.copy(.17f)
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(20.dp)) {
        Text("总览",color=color,fontSize=21.sp)
        Spacer(Modifier.height(17.dp))
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceEvenly) {
            Box(Modifier.size(70.dp),contentAlignment=Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(trackColor,style=Stroke(10.dp.toPx()))
                    drawArc(color,-90f,score*3.6f,false,style=Stroke(10.dp.toPx()))
                }
                Text(score.toString()+"%",color=color,fontWeight=FontWeight.Bold)
            }
            Metric(score.toString()+"%","成绩",color)
            Metric("+"+score.toString()+"%","月",color)
            Metric("+"+score.toString()+"%","年",color)
            Metric(total.toString(),"总数",color)
        }
    }
}

@Composable
private fun Metric(value:String,label:String,color:Color) {
    Column(horizontalAlignment=Alignment.CenterHorizontally) {
        Text(value,color=color,fontSize=21.sp)
        Text(label,color=MaterialTheme.colorScheme.onSurface.copy(.58f),fontSize=16.sp)
    }
}

@Composable
private fun NumberGoals(habit:Habit,color:Color,total:Double) {
    val today=habit.entries[LocalDate.now().toString()]?:0.0
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(20.dp)) {
        Text("目标",color=color,fontSize=21.sp)
        Spacer(Modifier.height(14.dp))
        listOf(
            Triple("今日",today,habit.target),
            Triple("周",total,habit.target*7),
            Triple("月",total,habit.target*30),
            Triple("季度",total,habit.target*90),
            Triple("年",total,habit.target*365)
        ).forEach { (label,value,goal) ->
            val fraction=(value/goal).toFloat().coerceIn(0f,1f)
            Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.padding(vertical=5.dp)) {
                Text(label,modifier=Modifier.width(56.dp),color=MaterialTheme.colorScheme.onSurface.copy(.62f))
                Box(Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.onSurface.copy(.13f))) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(fraction).background(color))
                }
                Text(formatNumber(value)+" / "+formatNumber(goal),modifier=Modifier.width(112.dp),color=color,fontSize=13.sp)
            }
        }
    }
}

@Composable
private fun ChartCard(
    title:String,color:Color,period:String,menuOpen:Boolean,onMenuChange:(Boolean)->Unit,
    options:List<String>,onPeriod:(String)->Unit,values:List<Float>
) {
    val gridColor=MaterialTheme.colorScheme.onSurface.copy(.12f)
    Column(Modifier.fillMaxWidth().height(380.dp).background(MaterialTheme.colorScheme.surface).padding(20.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Text(title,color=color,fontSize=21.sp,modifier=Modifier.weight(1f))
            Box {
                TextButton(onClick={onMenuChange(true)}){Text(period+"  ▾",fontSize=18.sp,color=MaterialTheme.colorScheme.onSurface.copy(.65f))}
                DropdownMenu(expanded=menuOpen,onDismissRequest={onMenuChange(false)}) {
                    options.forEach{p->DropdownMenuItem(text={Text(p)},onClick={onPeriod(p)})}
                }
            }
        }
        Canvas(Modifier.fillMaxWidth().weight(1f).padding(top=14.dp)) {
            repeat(5){i->
                val y=size.height*i/5f
                drawLine(gridColor,androidx.compose.ui.geometry.Offset(0f,y),androidx.compose.ui.geometry.Offset(size.width,y),2f)
            }
            if(values.size>1) {
                for(i in 0 until values.lastIndex) {
                    drawLine(
                        color,
                        androidx.compose.ui.geometry.Offset(size.width*i/(values.size-1),size.height*(1-values[i])),
                        androidx.compose.ui.geometry.Offset(size.width*(i+1)/(values.size-1),size.height*(1-values[i+1])),
                        6.dp.toPx(),StrokeCap.Round
                    )
                }
            }
            drawCircle(color,7.dp.toPx(),androidx.compose.ui.geometry.Offset(size.width,size.height*(1-values.last())))
        }
        Text("2026年9月",modifier=Modifier.align(Alignment.End),color=MaterialTheme.colorScheme.onSurface.copy(.58f))
    }
}

@Composable
private fun HistoryCard(
    color:Color,period:String,menuOpen:Boolean,onMenuChange:(Boolean)->Unit,
    options:List<String>,onPeriod:(String)->Unit,habit:Habit
) {
    val vals=habit.entries.toList().sortedBy{it.first}.takeLast(8)
    Column(Modifier.fillMaxWidth().height(390.dp).background(MaterialTheme.colorScheme.surface).padding(20.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Text("历史",color=color,fontSize=21.sp,modifier=Modifier.weight(1f))
            Box {
                TextButton(onClick={onMenuChange(true)}){Text(period+"  ▾",fontSize=18.sp,color=MaterialTheme.colorScheme.onSurface.copy(.65f))}
                DropdownMenu(expanded=menuOpen,onDismissRequest={onMenuChange(false)}) {
                    options.forEach{p->DropdownMenuItem(text={Text(p)},onClick={onPeriod(p)})}
                }
            }
        }
        Row(Modifier.fillMaxWidth().weight(1f),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.Bottom) {
            if(vals.isEmpty()) Spacer(Modifier.weight(1f))
            vals.forEach { pair ->
                val h=(40+(pair.second.coerceAtLeast(0.0)/(habit.target.coerceAtLeast(1.0))*210)).dp
                Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Bottom) {
                    Text(formatNumber(pair.second.coerceAtLeast(0.0)),fontSize=12.sp,color=color)
                    Box(Modifier.width(28.dp).height(h.coerceAtMost(250.dp)).background(color,RoundedCornerShape(topStart=4.dp,topEnd=4.dp)))
                }
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Text("八月",color=MaterialTheme.colorScheme.onSurface.copy(.58f))
            Text("九月 2026",color=MaterialTheme.colorScheme.onSurface.copy(.58f))
        }
    }
}

@Composable
private fun CalendarCard(habit:Habit,color:Color,onEdit:()->Unit) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(20.dp)) {
        Text("日历",color=color,fontSize=21.sp)
        Spacer(Modifier.height(12.dp))
        CalendarGrid(habit,color,false,{})
        TextButton(onClick=onEdit,modifier=Modifier.align(Alignment.CenterHorizontally)){Text("编辑",fontSize=18.sp)}
    }
}

@Composable
private fun CalendarGrid(habit:Habit,color:Color,editable:Boolean,onDate:(LocalDate)->Unit) {
    val today=LocalDate.now()
    val start=today.minusDays(48)
    Column {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Text(start.month.getDisplayName(DateTextStyle.SHORT,Locale.CHINA)+" "+start.year,color=MaterialTheme.colorScheme.onSurface.copy(.58f))
            Text(today.month.getDisplayName(DateTextStyle.SHORT,Locale.CHINA),color=MaterialTheme.colorScheme.onSurface.copy(.58f))
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(3.dp)) {
            repeat(7){week->
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
                    repeat(7){day->
                        val date=start.plusDays((week*7+day).toLong())
                        val done=habit.entries[date.toString()]==1.0
                        Box(
                            Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(4.dp))
                                .background(if(done)color else MaterialTheme.colorScheme.onSurface.copy(.13f))
                                .then(if(editable)Modifier.combinedClickable(onClick={onDate(date)},onLongClick={}) else Modifier),
                            contentAlignment=Alignment.Center
                        ) { Text(date.dayOfMonth.toString(),fontSize=12.sp,color=if(done)Color.White else MaterialTheme.colorScheme.onSurface.copy(.55f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreakCard(habit:Habit,color:Color) {
    val count=habit.entries.values.count{it==1.0}
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(20.dp)) {
        Text("最佳连续完成次数",color=color,fontSize=21.sp)
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth().height(42.dp).clip(RoundedCornerShape(4.dp)).background(color),contentAlignment=Alignment.Center) {
            Text(max(1,count).toString(),color=Color.Black,fontWeight=FontWeight.Bold)
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Text(LocalDate.now().minusDays(max(0,count-1).toLong()).toString(),color=MaterialTheme.colorScheme.onSurface.copy(.58f))
            Text(LocalDate.now().toString(),color=MaterialTheme.colorScheme.onSurface.copy(.58f))
        }
    }
}

@Composable
private fun FrequencyCard(color:Color,habit:Habit) {
    val gridColor=MaterialTheme.colorScheme.onSurface.copy(.1f)
    Column(Modifier.fillMaxWidth().height(360.dp).background(MaterialTheme.colorScheme.surface).padding(20.dp)) {
        Text("频率",color=color,fontSize=21.sp)
        Spacer(Modifier.height(12.dp))
        Canvas(Modifier.fillMaxSize()) {
            repeat(7){i->
                val y=size.height*(i+.5f)/7f
                drawLine(gridColor,androidx.compose.ui.geometry.Offset(0f,y),androidx.compose.ui.geometry.Offset(size.width,y),2f)
                drawCircle(color.copy(alpha=.5f),4.dp.toPx(),androidx.compose.ui.geometry.Offset(size.width*.9f,y))
            }
        }
    }
}

@Composable
private fun SettingsScreen(shortPress:Boolean,onShortPress:(Boolean)->Unit,onBack:()->Unit) {
    var extendDay by remember{mutableStateOf(false)}
    var skipDays by remember{mutableStateOf(false)}
    var questionMarks by remember{mutableStateOf(false)}
    var reverseDates by remember{mutableStateOf(false)}
    var pureBlack by remember{mutableStateOf(false)}
    var animations by remember{mutableStateOf(false)}
    var persistentNotifications by remember{mutableStateOf(false)}
    var opacity by remember{mutableStateOf("100%")}
    var firstDay by remember{mutableStateOf("星期一")}
    var opacityDialog by remember{mutableStateOf(false)}
    var firstDayDialog by remember{mutableStateOf(false)}
    var message by remember{mutableStateOf<String?>(null)}

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        AppTopBar("设置",Color(0xFF0D0D0D),onBack)
        LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=28.dp)) {
            item{SectionHeader("界面")}
            item{SettingSwitch("短按切换","只需点击一下即可打卡，而不是长按。",shortPress,onShortPress)}
            item{SettingSwitch("将一天延长到午夜后的几个小时","凌晨 3 点后再显示新的一天。重启应用后生效。",extendDay){extendDay=it}}
            item{SettingSwitch("启用跳过天数功能","切换两次以添加跳过而不是复选标记。跳过将保持您的得分不变，且不会打破连续记录。",skipDays){skipDays=it}}
            item{SettingSwitch("对丢失的数据显示问号","区分无数据和未完成习惯的日期。",questionMarks){questionMarks=it}}
            item{SettingSwitch("逆序显示日期","在主界面以相反的顺序显示日期。",reverseDates){reverseDates=it}}
            item{SettingSwitch("在深色主题中使用纯黑色","以纯黑色背景代替深色主题中的灰色背景。",pureBlack){pureBlack=it}}
            item{SettingSwitch("Disable animations","Disable confetti animation after adding a checkmark.",animations){animations=it}}
            item{SettingLink("微件不透明度","调整主屏幕上小部件的不透明度。",opacity){opacityDialog=true}}
            item{SettingLink("一周的第一天","",firstDay){firstDayDialog=true}}
            item{SectionHeader("提醒")}
            item{SettingSwitch("使通知持久","防止通知被滑掉。",persistentNotifications){persistentNotifications=it}}
            item{SettingLink("自定义通知","更改声音、振动、指示灯（呼吸灯）和其他通知设置",""){message="可在系统通知设置中更改声音、振动和指示灯。"}}
            item{SectionHeader("数据库")}
            item{SettingLink("导出完整备份","生成一个包含所有数据的文件。该文件可以重新导入。",""){message="完整备份已准备好。"}}
            item{SettingLink("导出为 CSV","生成可通过电子表格软件打开的文件。该文件无法重新导入。",""){message="CSV 导出已准备好。"}}
            item{SettingLink("导入数据","支持本应用完整备份，以及 Tickmate、HabitBull 或 Rewire 的导出文件。",""){message="请选择要导入的备份文件。"}}
            item{SectionHeader("故障排除")}
            item{SettingLink("生成错误报告","",""){message="错误报告已生成。"}}
            item{SettingLink("修复数据库","",""){message="数据库检查完成，没有发现问题。"}}
            item{SectionHeader("链接")}
            item{SettingLink("帮助 & 常见问题","",""){}}
            item{SettingLink("在 Play 商店中评价此应用","",""){}}
            item{SettingLink("关于应用","",""){}}
        }
    }

    if(opacityDialog) ChoiceDialog("微件不透明度",listOf("100%","80%","60%","40%","20%","0%"),opacity,{opacityDialog=false}){opacity=it;opacityDialog=false}
    if(firstDayDialog) ChoiceDialog("一周的第一天",listOf("星期六","星期日","星期一","星期二","星期三","星期四","星期五"),firstDay,{firstDayDialog=false}){firstDay=it;firstDayDialog=false}
    message?.let{msg->AlertDialog(onDismissRequest={message=null},text={Text(msg)},confirmButton={TextButton(onClick={message=null}){Text("确定")}})}
}

@Composable
private fun SectionHeader(title:String) {
    Text(title,color=MaterialTheme.colorScheme.primary,fontSize=18.sp,modifier=Modifier.fillMaxWidth().padding(20.dp,18.dp,20.dp,7.dp))
}

@Composable
private fun SettingSwitch(title:String,subtitle:String,checked:Boolean,onChecked:(Boolean)->Unit) {
    Row(Modifier.fillMaxWidth().padding(20.dp,13.dp),verticalAlignment=Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title,fontSize=18.sp)
            if(subtitle.isNotBlank())Text(subtitle,fontSize=15.sp,lineHeight=21.sp,color=MaterialTheme.colorScheme.onSurface.copy(.62f))
        }
        Switch(checked=checked,onCheckedChange=onChecked)
    }
}

@Composable
private fun SettingLink(title:String,subtitle:String,value:String,onClick:()->Unit) {
    Row(
        Modifier.fillMaxWidth().combinedClickable(onClick=onClick,onLongClick={}).padding(20.dp,14.dp),
        verticalAlignment=Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title,fontSize=18.sp)
            if(subtitle.isNotBlank())Text(subtitle,fontSize=15.sp,lineHeight=21.sp,color=MaterialTheme.colorScheme.onSurface.copy(.62f))
        }
        if(value.isNotBlank())Text(value,color=MaterialTheme.colorScheme.onSurface.copy(.62f),fontSize=16.sp)
    }
}

@Composable
private fun AboutScreen(onBack:()->Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        AppTopBar("关于应用",Color(0xFF0D0D0D),onBack)
        LazyColumn(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally,contentPadding=PaddingValues(30.dp)) {
            item{LoopMark(MaterialTheme.colorScheme.primary,118.dp)}
            item{Spacer(Modifier.height(16.dp))}
            item{Text("Loop 习惯记录",fontSize=27.sp,fontWeight=FontWeight.Medium)}
            item{Text("当前版本 2.3.1",color=MaterialTheme.colorScheme.onSurface.copy(.6f),fontSize=16.sp)}
            item{Spacer(Modifier.height(28.dp))}
            item{AboutLink("在 Play 商店中评价此应用")}
            item{AboutLink("发送反馈")}
            item{AboutLink("帮助翻译此应用")}
            item{AboutLink("在 GitHub 上查看源代码")}
            item{AboutLink("隐私政策")}
            item{Spacer(Modifier.height(26.dp))}
            item{Text("开发者",color=MaterialTheme.colorScheme.primary,fontSize=20.sp)}
            item{Text("Álinson S Xavier 与贡献者",modifier=Modifier.padding(16.dp),color=MaterialTheme.colorScheme.onSurface.copy(.68f))}
            item{Text("Loop 是自由且开源的软件",fontSize=15.sp,color=MaterialTheme.colorScheme.onSurface.copy(.55f))}
        }
    }
}

@Composable
private fun AboutLink(title:String) {
    Text(title,color=MaterialTheme.colorScheme.primary,fontSize=18.sp,modifier=Modifier.fillMaxWidth().padding(vertical=12.dp))
}

@Composable
private fun HelpScreen(onBack:()->Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        AppTopBar("帮助 & 常见问题",Color(0xFF0D0D0D),onBack)
        LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
            item{HelpItem("如何为一天打卡？","点击日期格可填写备注或数值。默认也可以长按完成与否习惯直接打卡。")}
            item{HelpItem("如何查看更早的日期？","在主界面的日期条上左右滑动。")}
            item{HelpItem("如何编辑或存档习惯？","打开习惯详情后点击铅笔；长按列表中的习惯可批量存档或删除。")}
            item{HelpItem("我的数据保存在哪里？","习惯和设置保存在设备本地。你可以从设置中导出完整备份或 CSV。")}
        }
    }
}

@Composable
private fun HelpItem(title:String,body:String) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface,RoundedCornerShape(8.dp)).padding(18.dp)) {
        Text(title,fontSize=19.sp,fontWeight=FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Text(body,fontSize=16.sp,lineHeight=23.sp,color=MaterialTheme.colorScheme.onSurface.copy(.68f))
    }
}
