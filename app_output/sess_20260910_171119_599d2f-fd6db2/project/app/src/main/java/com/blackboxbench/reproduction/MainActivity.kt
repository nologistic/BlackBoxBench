package com.blackboxbench.reproduction

import android.content.Context
import android.os.Bundle
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AnkiBlue = Color(0xFF079BE5)
private val DeepBlue = Color(0xFF075E7A)
private val NewBlue = Color(0xFF2196F3)
private val LearnRed = Color(0xFFF04B4B)
private val DueGreen = Color(0xFF3FAE64)
private val LightBg = Color(0xFFF5F7FA)

enum class AppPage { Welcome, Permission, Decks, DeckDetail, Study, Browser, Add, Stats, Settings }

data class FlashCard(val deck: String, val front: String, val back: String, val tags: String)
data class DeckRow(val name: String, val level: Int, val newCount: Int, val learn: Int, val due: Int, val branch: Boolean = false)

private val sampleCards = listOf(
    FlashCard("语言::日语::五十音", "あ", "a", "日语 平假名"),
    FlashCard("语言::日语::五十音", "い", "i", "日语 平假名"),
    FlashCard("语言::日语::五十音", "う", "u", "日语 平假名"),
    FlashCard("语言::日语::词汇", "おはようございます", "早上好", "日语 词汇"),
    FlashCard("语言::日语::词汇", "ありがとう", "谢谢", "日语 词汇"),
    FlashCard("语言::日语::词汇", "すみません", "对不起 / 劳驾", "日语 词汇"),
    FlashCard("科学::天文", "太阳系最大的行星是？", "木星", "天文 常识"),
    FlashCard("科学::天文", "光从太阳到地球约需多久？", "约 8 分 20 秒", "天文 常识"),
    FlashCard("科学::天文", "月球自转与公转周期有何关系？", "几乎相等（潮汐锁定）", "天文 常识"),
    FlashCard("科学::化学", "元素符号 Au 表示？", "金", "化学 元素"),
    FlashCard("科学::化学", "常温下唯一的液态金属？", "汞（Hg）", "化学 元素"),
    FlashCard("历史::近代史", "第一次月球着陆是哪一年？", "1969 年", "历史 年代")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ReproducedApp() }
    }
}

@Composable
fun ReproducedApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ankidroid_demo", Context.MODE_PRIVATE) }
    var page by rememberSaveable {
        mutableStateOf(if (prefs.getBoolean("setup_done", false)) AppPage.Decks else AppPage.Welcome)
    }
    var selectedDeck by rememberSaveable { mutableStateOf("语言") }
    var permissionGranted by rememberSaveable { mutableStateOf(false) }
    var syncDialog by rememberSaveable { mutableStateOf(false) }
    var infoDialog by rememberSaveable { mutableStateOf<String?>(null) }
    var menuExpanded by rememberSaveable { mutableStateOf(false) }
    var reviewIndex by rememberSaveable { mutableIntStateOf(prefs.getInt("review_index", 0)) }
    var reviewCount by rememberSaveable { mutableIntStateOf(prefs.getInt("review_count", 0)) }
    val addedCards = remember { mutableStateListOf<FlashCard>().apply { addAll(loadAddedCards(prefs)) } }

    fun goBack() {
        page = when (page) {
            AppPage.Permission -> AppPage.Welcome
            AppPage.DeckDetail, AppPage.Browser, AppPage.Add, AppPage.Stats, AppPage.Settings -> AppPage.Decks
            AppPage.Study -> AppPage.DeckDetail
            else -> page
        }
    }
    BackHandler(enabled = page != AppPage.Welcome && page != AppPage.Decks) { goBack() }

    BenchmarkAppTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = LightBg) {
            when (page) {
                AppPage.Welcome -> WelcomeScreen(
                    onStart = { page = AppPage.Permission },
                    onSync = { syncDialog = true }
                )
                AppPage.Permission -> PermissionScreen(
                    granted = permissionGranted,
                    onToggle = { permissionGranted = it },
                    onContinue = {
                        prefs.edit().putBoolean("setup_done", true).apply()
                        page = AppPage.Decks
                    }
                )
                AppPage.Decks -> DeckScreen(
                    menuExpanded = menuExpanded,
                    onMenu = { menuExpanded = !menuExpanded },
                    onDismissMenu = { menuExpanded = false },
                    onDeck = { selectedDeck = it; page = AppPage.DeckDetail },
                    onAdd = { page = AppPage.Add },
                    onBrowser = { page = AppPage.Browser },
                    onStats = { page = AppPage.Stats },
                    onSettings = { page = AppPage.Settings },
                    onSync = { syncDialog = true },
                    onImport = { infoDialog = "从文件导入\n支持制表符分隔的 .txt / .tsv 文件。可指定目标牌组和标签列。" },
                    onExport = { infoDialog = "导出完成\n已将 12 条示例笔记整理为可再次导入的文本集合。" }
                )
                AppPage.DeckDetail -> DeckDetailScreen(
                    deck = selectedDeck,
                    reviewed = reviewCount,
                    onBack = { page = AppPage.Decks },
                    onStudy = { reviewIndex = 0; page = AppPage.Study },
                    onBrowse = { page = AppPage.Browser }
                )
                AppPage.Study -> StudyScreen(
                    deck = selectedDeck,
                    startIndex = reviewIndex,
                    onIndexChange = {
                        reviewIndex = it
                        prefs.edit().putInt("review_index", it).apply()
                    },
                    onRated = {
                        reviewCount += 1
                        prefs.edit().putInt("review_count", reviewCount).apply()
                    },
                    onBack = { page = AppPage.DeckDetail }
                )
                AppPage.Browser -> BrowserScreen(
                    cards = sampleCards + addedCards,
                    initialDeck = selectedDeck,
                    onBack = { page = AppPage.Decks },
                    onAdd = { page = AppPage.Add },
                    onStats = { page = AppPage.Stats }
                )
                AppPage.Add -> AddCardScreen(
                    onBack = { page = AppPage.Decks },
                    onSave = {
                        addedCards.add(it); saveAddedCards(prefs, addedCards)
                        infoDialog = "卡片已添加\n新笔记已保存到“" + it.deck + "”。"
                        page = AppPage.Decks
                    }
                )
                AppPage.Stats -> StatsScreen(onBack = { page = AppPage.Decks }, onBrowser = { page = AppPage.Browser }, reviewed = reviewCount)
                AppPage.Settings -> SettingsScreen(onBack = { page = AppPage.Decks })
            }
        }

        if (syncDialog) {
            SyncDialog(onDismiss = { syncDialog = false })
        }
        infoDialog?.let { message ->
            AlertDialog(
                onDismissRequest = { infoDialog = null },
                title = { Text(message.substringBefore("\n"), fontWeight = FontWeight.Bold) },
                text = { Text(message.substringAfter("\n", "")) },
                confirmButton = { TextButton(onClick = { infoDialog = null }) { Text("确定") } }
            )
        }
    }
}

@Composable
private fun WelcomeScreen(onStart: () -> Unit, onSync: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.43f)
                .background(Brush.verticalGradient(listOf(Color(0xFF00A4E8), Color(0xFF86D4F5), Color.White))),
            contentAlignment = Alignment.BottomCenter
        ) {
            AnkiLogo(Modifier.padding(bottom = 12.dp))
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 42.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("学得更少", fontSize = 41.sp, lineHeight = 48.sp, fontWeight = FontWeight.Black, color = Color.Black)
            Box(Modifier.padding(vertical = 12.dp).width(108.dp).height(3.dp).background(Color(0xFFE4E4E4)))
            Text("记得更牢", fontSize = 41.sp, lineHeight = 48.sp, fontWeight = FontWeight.Black, color = Color.Black)
            Spacer(Modifier.height(24.dp))
            Text(
                "使用 Anki 的卡片调度系统，强化并巩固近记忆，从而节省您的学习时间",
                fontSize = 18.sp,
                lineHeight = 28.sp,
                color = Color(0xFF202124),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(30.dp))
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2398EA))
            ) { Text("开始", fontSize = 20.sp) }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onSync,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2398EA))
            ) { Text("从 AnkiWeb 同步", fontSize = 19.sp, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun AnkiLogo(modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(150.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(126.dp)) {
            drawRoundRect(
                color = Color(0xFF5F6568),
                topLeft = Offset(size.width * .12f, size.height * .08f),
                size = Size(size.width * .6f, size.height * .78f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
            )
            repeat(5) { i ->
                drawLine(
                    color = Color(0xFF818689),
                    start = Offset(size.width * .23f, size.height * (.22f + i * .12f)),
                    end = Offset(size.width * (.58f - (i % 2) * .1f), size.height * (.22f + i * .12f)),
                    strokeWidth = 7f,
                    cap = StrokeCap.Round
                )
            }
            val p = Path()
            val cx = size.width * .66f
            val cy = size.height * .58f
            val outer = size.minDimension * .29f
            val inner = outer * .45f
            for (i in 0 until 10) {
                val angle = -Math.PI / 2 + i * Math.PI / 5
                val radius = if (i % 2 == 0) outer else inner
                val x = cx + kotlin.math.cos(angle).toFloat() * radius
                val y = cy + kotlin.math.sin(angle).toFloat() * radius
                if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
            }
            p.close()
            drawPath(p, Color.White, style = Stroke(width = 14f))
            drawPath(p, Color(0xFF16A6E9))
        }
    }
}

@Composable
private fun PermissionScreen(granted: Boolean, onToggle: (Boolean) -> Unit, onContinue: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color.White).statusBarsPadding().padding(horizontal = 28.dp)
    ) {
        Spacer(Modifier.height(32.dp))
        Text("AnkiDroid 需要一些权限才能运行", fontSize = 24.sp, lineHeight = 35.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(58.dp))
        Row(verticalAlignment = Alignment.Top) {
            Box(Modifier.padding(top = 8.dp).size(28.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF6E7377)), contentAlignment = Alignment.Center) {
                Text("●", color = Color.White, fontSize = 12.sp)
            }
            Spacer(Modifier.width(25.dp))
            Column(Modifier.weight(1f)) {
                Text("所有文件访问权限", fontSize = 20.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Text("将您的集合保存在安全的地方，即使卸载应用也不会被删除", fontSize = 16.sp, lineHeight = 24.sp, color = Color(0xFF5F6368))
            }
            Switch(checked = granted, onCheckedChange = onToggle)
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onContinue,
            enabled = granted,
            modifier = Modifier.fillMaxWidth().height(59.dp).padding(bottom = 4.dp),
            shape = RoundedCornerShape(30.dp)
        ) { Text("继续", fontSize = 19.sp) }
        Spacer(Modifier.height(26.dp))
    }
}

@Composable
private fun DeckScreen(
    menuExpanded: Boolean,
    onMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onDeck: (String) -> Unit,
    onAdd: () -> Unit,
    onBrowser: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
    onSync: () -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit
) {
    var languageOpen by rememberSaveable { mutableStateOf(true) }
    var japaneseOpen by rememberSaveable { mutableStateOf(true) }
    var englishOpen by rememberSaveable { mutableStateOf(false) }
    var scienceOpen by rememberSaveable { mutableStateOf(true) }
    var historyOpen by rememberSaveable { mutableStateOf(false) }
    val rows = buildList {
        add(DeckRow("语言", 0, 8, 1, 1, true))
        if (languageOpen) {
            add(DeckRow("日语", 1, 6, 0, 1, true))
            if (japaneseOpen) {
                add(DeckRow("五十音", 2, 3, 0, 0))
                add(DeckRow("词汇", 2, 3, 0, 1))
            }
            add(DeckRow("英语", 1, 2, 1, 0, true))
            if (englishOpen) add(DeckRow("核心词汇", 2, 2, 1, 0))
        }
        add(DeckRow("科学", 0, 5, 0, 2, true))
        if (scienceOpen) {
            add(DeckRow("天文", 1, 3, 0, 2))
            add(DeckRow("化学", 1, 2, 0, 0))
        }
        add(DeckRow("历史", 0, 1, 0, 0, true))
        if (historyOpen) add(DeckRow("近代史", 1, 1, 0, 0))
    }
    Scaffold(
        topBar = {
            AppTopBar(
                title = "AnkiDroid",
                left = "☰",
                right = {
                    Text("↻", color = Color.White, fontSize = 27.sp, modifier = Modifier.clickable(onClick = onSync).padding(10.dp))
                    Box {
                        Text("⋮", color = Color.White, fontSize = 30.sp, modifier = Modifier.clickable(onClick = onMenu).padding(10.dp))
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = onDismissMenu) {
                            DropdownMenuItem(text = { Text("从文件导入") }, onClick = { onDismissMenu(); onImport() })
                            DropdownMenuItem(text = { Text("导出") }, onClick = { onDismissMenu(); onExport() })
                            DropdownMenuItem(text = { Text("设置") }, onClick = { onDismissMenu(); onSettings() })
                            DropdownMenuItem(text = { Text("同步") }, onClick = { onDismissMenu(); onSync() })
                        }
                    }
                }
            )
        },
        bottomBar = { MainBottomBar(current = AppPage.Decks, onDecks = {}, onBrowser = onBrowser, onStats = onStats) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                containerColor = AnkiBlue,
                contentColor = Color.White,
                text = { Text("添加") },
                icon = { Text("+", fontSize = 26.sp) }
            )
        },
        containerColor = LightBg
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(bottom = 90.dp)) {
            item {
                TodaySummary()
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
                    Text("牌组", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF5F6368), modifier = Modifier.weight(1f))
                    Text("新卡", color = NewBlue, fontSize = 13.sp, modifier = Modifier.width(48.dp), textAlign = TextAlign.Center)
                    Text("学习", color = LearnRed, fontSize = 13.sp, modifier = Modifier.width(48.dp), textAlign = TextAlign.Center)
                    Text("到期", color = DueGreen, fontSize = 13.sp, modifier = Modifier.width(48.dp), textAlign = TextAlign.Center)
                }
            }
            items(rows) { row ->
                DeckListRow(
                    row = row,
                    onArrow = {
                        when (row.name) {
                            "语言" -> languageOpen = !languageOpen
                            "日语" -> japaneseOpen = !japaneseOpen
                            "英语" -> englishOpen = !englishOpen
                            "科学" -> scienceOpen = !scienceOpen
                            "历史" -> historyOpen = !historyOpen
                        }
                    },
                    onClick = { onDeck(row.name) }
                )
            }
        }
    }
}

@Composable
private fun TodaySummary() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("今天", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("17 张卡片等待学习", color = Color(0xFF60656A), fontSize = 14.sp)
                }
                Text("3", color = DueGreen, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { 0.18f },
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                color = AnkiBlue,
                trackColor = Color(0xFFE4E9ED)
            )
            Spacer(Modifier.height(7.dp))
            Text("今日已完成 3 次复习", color = Color(0xFF74777A), fontSize = 13.sp)
        }
    }
}

@Composable
private fun DeckListRow(row: DeckRow, onArrow: () -> Unit, onClick: () -> Unit) {
    Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = (14 + row.level * 28).dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (row.branch) "▾" else "•",
                modifier = Modifier.size(28.dp).clickable(enabled = row.branch, onClick = onArrow),
                textAlign = TextAlign.Center,
                color = Color(0xFF697177),
                fontSize = 18.sp
            )
            Text(row.name, modifier = Modifier.weight(1f), fontSize = if (row.level == 0) 18.sp else 16.sp, fontWeight = if (row.level == 0) FontWeight.SemiBold else FontWeight.Normal)
            CountCell(row.newCount, NewBlue)
            CountCell(row.learn, LearnRed)
            CountCell(row.due, DueGreen)
        }
    }
    HorizontalDivider(color = Color(0xFFE8EBEE))
}

@Composable
private fun CountCell(value: Int, color: Color) {
    Text(if (value == 0) "—" else value.toString(), color = if (value == 0) Color(0xFFB4B8BB) else color, modifier = Modifier.width(48.dp), textAlign = TextAlign.Center, fontWeight = if (value > 0) FontWeight.Bold else FontWeight.Normal)
}

@Composable
private fun DeckDetailScreen(deck: String, reviewed: Int, onBack: () -> Unit, onStudy: () -> Unit, onBrowse: () -> Unit) {
    Scaffold(
        topBar = { AppTopBar(title = deck, left = "‹", onLeft = onBack, right = { Text("⋮", color = Color.White, fontSize = 30.sp, modifier = Modifier.padding(12.dp)) }) },
        containerColor = LightBg
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(22.dp)) {
                    Text("今日学习", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(24.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        StatNumber("新卡", if (deck == "语言") 8 else 3, NewBlue)
                        StatNumber("学习中", 1, LearnRed)
                        StatNumber("待复习", if (deck == "科学") 2 else 1, DueGreen)
                    }
                    Spacer(Modifier.height(22.dp))
                    Text("本牌组及子牌组的到期卡片", color = Color(0xFF676C70), fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(22.dp))
            Button(onClick = onStudy, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(29.dp)) {
                Text("现在学习", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onBrowse, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(26.dp)) { Text("浏览卡片") }
            Spacer(Modifier.height(22.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("进度", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("今天已复习 " + reviewed + " 次", color = Color(0xFF5F6368))
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(progress = { (reviewed.coerceAtMost(10) / 10f) }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape))
                }
            }
        }
    }
}

@Composable
private fun StatNumber(label: String, number: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(number.toString(), color = color, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color(0xFF5F6368), fontSize = 14.sp)
    }
}

@Composable
private fun StudyScreen(
    deck: String,
    startIndex: Int,
    onIndexChange: (Int) -> Unit,
    onRated: () -> Unit,
    onBack: () -> Unit
) {
    val relevant = remember(deck) {
        val exact = sampleCards.filter { it.deck.contains(deck) }
        if (exact.isEmpty()) sampleCards else exact
    }
    var index by rememberSaveable(deck) { mutableIntStateOf(startIndex % relevant.size) }
    var revealed by rememberSaveable(deck, index) { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf("") }
    var completed by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        topBar = { AppTopBar(title = deck, left = "×", onLeft = onBack, right = { Text((index + 1).toString() + " / " + relevant.size, color = Color.White, modifier = Modifier.padding(16.dp)) }) },
        containerColor = Color(0xFFF1F3F5)
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (message.isNotBlank()) {
                Text(message, color = if (message.startsWith("忘记")) LearnRed else DueGreen, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(6.dp))
            } else Spacer(Modifier.height(30.dp))
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(relevant[index].front, fontSize = 31.sp, lineHeight = 42.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
                    if (revealed) {
                        Spacer(Modifier.height(32.dp))
                        HorizontalDivider(Modifier.width(180.dp), color = Color(0xFFB8BDC1))
                        Spacer(Modifier.height(32.dp))
                        Text(relevant[index].back, fontSize = 27.sp, lineHeight = 38.sp, textAlign = TextAlign.Center, color = Color(0xFF17495C))
                        Spacer(Modifier.height(24.dp))
                        Text(relevant[index].tags, color = Color(0xFF7B8084), fontSize = 14.sp)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            if (!revealed) {
                Button(onClick = { revealed = true; message = "" }, modifier = Modifier.fillMaxWidth().height(64.dp), shape = RoundedCornerShape(12.dp)) {
                    Text("显示答案", fontSize = 21.sp)
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    RatingButton("忘记", "1分钟", Color(0xFFD94545), Modifier.weight(1f)) {
                        message = "忘记 · 将在 1 分钟后再次出现"
                        revealed = false
                        onRated()
                    }
                    RatingButton("困难", "6分钟", Color(0xFFE2812B), Modifier.weight(1f)) {
                        message = "困难 · 间隔小幅增加"
                        index = (index + 1) % relevant.size
                        onIndexChange(index)
                        revealed = false
                        completed++
                        onRated()
                    }
                    RatingButton("良好", "10分钟", Color(0xFF2F8B57), Modifier.weight(1f)) {
                        message = "良好 · 下次间隔更长"
                        index = (index + 1) % relevant.size
                        onIndexChange(index)
                        revealed = false
                        completed++
                        onRated()
                    }
                    RatingButton("简单", "4天", Color(0xFF2876B7), Modifier.weight(1f)) {
                        message = "简单 · 4 天后复习"
                        index = (index + 1) % relevant.size
                        onIndexChange(index)
                        revealed = false
                        completed++
                        onRated()
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("已完成 " + completed + " 张 · 左右滑动可切换卡片", color = Color(0xFF73787C), fontSize = 13.sp)
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun RatingButton(label: String, interval: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = modifier.height(68.dp), shape = RoundedCornerShape(9.dp), contentPadding = PaddingValues(2.dp), colors = ButtonDefaults.buttonColors(containerColor = color)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(interval, fontSize = 10.sp)
        }
    }
}

@Composable
private fun BrowserScreen(cards: List<FlashCard>, initialDeck: String, onBack: () -> Unit, onAdd: () -> Unit, onStats: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var deckOnly by rememberSaveable { mutableStateOf(false) }
    var dueOnly by rememberSaveable { mutableStateOf(false) }
    val filtered = cards.filter {
        val textMatch = query.isBlank() || it.front.contains(query, true) || it.back.contains(query, true) || it.tags.contains(query, true)
        val deckMatch = !deckOnly || it.deck.contains(initialDeck)
        textMatch && deckMatch
    }
    Scaffold(
        topBar = { AppTopBar(title = "卡片浏览器", left = "‹", onLeft = onBack, right = { Text("+", color = Color.White, fontSize = 28.sp, modifier = Modifier.clickable(onClick = onAdd).padding(14.dp)) }) },
        bottomBar = { MainBottomBar(current = AppPage.Browser, onDecks = onBack, onBrowser = {}, onStats = onStats) },
        containerColor = LightBg
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth().padding(12.dp), placeholder = { Text("搜索问题、答案或标签") }, singleLine = true, leadingIcon = { Text("⌕", fontSize = 24.sp) })
            Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterPill("当前牌组", deckOnly) { deckOnly = !deckOnly }
                FilterPill("今日到期", dueOnly) { dueOnly = !dueOnly }
                FilterPill("按到期排序", false) {}
            }
            Text("共 " + filtered.size + " 张卡片", modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), color = Color(0xFF63686C), fontSize = 13.sp)
            LazyColumn(Modifier.fillMaxSize()) {
                items(filtered) { card ->
                    CardRow(card)
                }
            }
        }
    }
}

@Composable
private fun FilterPill(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) Color(0xFFD7EEFC) else Color.White,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) AnkiBlue else Color(0xFFB8BDC1)),
        modifier = Modifier.clickable(onClick = onClick)
    ) { Text(text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 13.sp, color = if (selected) DeepBlue else Color(0xFF45494C)) }
}

@Composable
private fun CardRow(card: FlashCard) {
    Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 13.dp)) {
            Text(card.front, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 17.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(5.dp))
            Text(card.back, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color(0xFF5E6367), fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            Row {
                Text(card.deck, color = AnkiBlue, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(card.tags, color = Color(0xFF777C80), fontSize = 12.sp)
            }
        }
    }
    HorizontalDivider(color = Color(0xFFE3E6E8))
}

@Composable
private fun AddCardScreen(onBack: () -> Unit, onSave: (FlashCard) -> Unit) {
    var front by rememberSaveable { mutableStateOf("") }
    var back by rememberSaveable { mutableStateOf("") }
    var tags by rememberSaveable { mutableStateOf("") }
    val decks = listOf("语言::日语::词汇", "语言::英语::核心词汇", "科学::天文", "科学::化学", "历史::近代史")
    var deckIndex by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        topBar = { AppTopBar(title = "添加笔记", left = "×", onLeft = onBack, right = { Text("保存", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(enabled = front.isNotBlank() && back.isNotBlank()) { onSave(FlashCard(decks[deckIndex], front, back, tags.ifBlank { "自建" })) }.padding(16.dp)) }) },
        containerColor = Color.White
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("笔记类型", color = Color(0xFF64696D), fontSize = 13.sp)
            Surface(Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { }, color = Color(0xFFF1F6F9), shape = RoundedCornerShape(8.dp)) {
                Text("基本", modifier = Modifier.padding(14.dp), fontSize = 17.sp)
            }
            Spacer(Modifier.height(14.dp))
            Text("牌组", color = Color(0xFF64696D), fontSize = 13.sp)
            Surface(Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { deckIndex = (deckIndex + 1) % decks.size }, color = Color(0xFFF1F6F9), shape = RoundedCornerShape(8.dp)) {
                Row(Modifier.padding(14.dp)) {
                    Text(decks[deckIndex], modifier = Modifier.weight(1f), fontSize = 16.sp)
                    Text("▾")
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = front, onValueChange = { front = it }, modifier = Modifier.fillMaxWidth().height(150.dp), label = { Text("正面") }, placeholder = { Text("问题") })
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(value = back, onValueChange = { back = it }, modifier = Modifier.fillMaxWidth().height(150.dp), label = { Text("背面") }, placeholder = { Text("答案") })
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(value = tags, onValueChange = { tags = it }, modifier = Modifier.fillMaxWidth(), label = { Text("标签") }, placeholder = { Text("用空格分隔标签") }, singleLine = true)
            Spacer(Modifier.height(18.dp))
            Text("一条笔记会由“基本”模板生成一张正面到背面的卡片。", color = Color(0xFF777C80), fontSize = 13.sp, lineHeight = 19.sp)
            Spacer(Modifier.height(24.dp))
            Button(onClick = { onSave(FlashCard(decks[deckIndex], front, back, tags.ifBlank { "自建" })) }, enabled = front.isNotBlank() && back.isNotBlank(), modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("添加") }
        }
    }
}

@Composable
private fun StatsScreen(onBack: () -> Unit, onBrowser: () -> Unit, reviewed: Int) {
    Scaffold(
        topBar = { AppTopBar(title = "统计", left = "‹", onLeft = onBack) },
        bottomBar = { MainBottomBar(current = AppPage.Stats, onDecks = onBack, onBrowser = onBrowser, onStats = {}) },
        containerColor = LightBg
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("今天学习", (3 + reviewed).toString(), "次", Modifier.weight(1f))
                StatCard("正确率", "86", "%", Modifier.weight(1f))
                StatCard("学习时间", "7", "分钟", Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("未来 7 天到期", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text("预计需要复习的卡片", color = Color(0xFF707579), fontSize = 13.sp)
                    Spacer(Modifier.height(18.dp))
                    DueChart()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("今天","明天","周日","周一","周二","周三","周四").forEach { Text(it, fontSize = 10.sp, color = Color(0xFF74797D)) }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("今日明细", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(14.dp))
                    DetailLine("新学", 3, NewBlue)
                    DetailLine("复习", 5 + reviewed, DueGreen)
                    DetailLine("重新学习", 1, LearnRed)
                }
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, suffix: String, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 12.sp, color = Color(0xFF686D71))
            Text(value, fontSize = 29.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
            Text(suffix, fontSize = 11.sp, color = Color(0xFF85898C))
        }
    }
}

@Composable
private fun DueChart() {
    val values = listOf(3f, 6f, 2f, 8f, 5f, 9f, 4f)
    Canvas(Modifier.fillMaxWidth().height(180.dp)) {
        val max = values.max()
        val gap = size.width / values.size
        values.forEachIndexed { i, v ->
            val h = (v / max) * size.height * .82f
            drawRoundRect(
                color = if (i == 0) AnkiBlue else Color(0xFF89C7E8),
                topLeft = Offset(i * gap + gap * .17f, size.height - h),
                size = Size(gap * .62f, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(9f, 9f)
            )
        }
        drawLine(Color(0xFFD6DBDE), Offset(0f, size.height - 1), Offset(size.width, size.height - 1), 2f)
    }
}

@Composable
private fun DetailLine(label: String, count: Int, color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(11.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(10.dp))
        Text(label, modifier = Modifier.weight(1f))
        Text(count.toString() + " 张", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SettingsScreen(onBack: () -> Unit) {
    var notifications by rememberSaveable { mutableStateOf(true) }
    var gestures by rememberSaveable { mutableStateOf(true) }
    var darkCards by rememberSaveable { mutableStateOf(false) }
    var newLimit by rememberSaveable { mutableIntStateOf(20) }
    Scaffold(topBar = { AppTopBar(title = "设置", left = "‹", onLeft = onBack) }, containerColor = LightBg) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            SettingsHeader("学习")
            SettingsValueRow("每日新卡上限", newLimit.toString()) { newLimit = if (newLimit == 20) 30 else 20 }
            SettingsSwitchRow("启用复习手势", "在学习界面使用滑动操作", gestures) { gestures = it }
            SettingsHeader("通知")
            SettingsSwitchRow("每日学习提醒", "有到期卡片时提醒我", notifications) { notifications = it }
            SettingsHeader("外观")
            SettingsSwitchRow("深色卡片", "仅学习卡片使用深色背景", darkCards) { darkCards = it }
            SettingsValueRow("界面语言", "简体中文") {}
            SettingsHeader("关于")
            SettingsValueRow("AnkiDroid", "复现演示 1.0") {}
        }
    }
}

@Composable
private fun SettingsHeader(text: String) {
    Text(text, color = AnkiBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.fillMaxWidth().padding(start = 18.dp, top = 22.dp, bottom = 8.dp))
}

@Composable
private fun SettingsValueRow(title: String, value: String, onClick: () -> Unit) {
    Surface(color = Color.White, modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 18.dp)) {
            Text(title, modifier = Modifier.weight(1f), fontSize = 16.sp)
            Text(value, color = Color(0xFF6E7377))
        }
    }
    HorizontalDivider(color = Color(0xFFE5E8EA))
}

@Composable
private fun SettingsSwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp)
                Text(subtitle, color = Color(0xFF74797D), fontSize = 13.sp)
            }
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
    HorizontalDivider(color = Color(0xFFE5E8EA))
}

@Composable
private fun AppTopBar(title: String, left: String = "", onLeft: () -> Unit = {}, right: @Composable () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().background(AnkiBlue).statusBarsPadding().height(62.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (left.isNotBlank()) {
            Text(left, color = Color.White, fontSize = 30.sp, modifier = Modifier.size(52.dp).clickable(onClick = onLeft).padding(horizontal = 10.dp), textAlign = TextAlign.Center)
        } else Spacer(Modifier.width(8.dp))
        Text(title, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        right()
    }
}

@Composable
private fun MainBottomBar(current: AppPage, onDecks: () -> Unit, onBrowser: () -> Unit, onStats: () -> Unit) {
    Surface(color = Color.White, shadowElevation = 8.dp) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().height(64.dp)) {
            BottomItem("▤", "牌组", current == AppPage.Decks, Modifier.weight(1f), onDecks)
            BottomItem("⌕", "浏览", current == AppPage.Browser, Modifier.weight(1f), onBrowser)
            BottomItem("▥", "统计", current == AppPage.Stats, Modifier.weight(1f), onStats)
        }
    }
}

@Composable
private fun BottomItem(icon: String, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clickable(onClick = onClick).padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, color = if (selected) AnkiBlue else Color(0xFF6F7478), fontSize = 22.sp)
        Text(label, color = if (selected) AnkiBlue else Color(0xFF6F7478), fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun SyncDialog(onDismiss: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var attempted by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("从 AnkiWeb 同步", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (attempted) {
                    Surface(color = Color(0xFFFFF0EE), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        Text("设备当前离线，无法连接 AnkiWeb。", color = Color(0xFFAA312B), modifier = Modifier.padding(12.dp))
                    }
                }
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("AnkiWeb 用户名") }, singleLine = true)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("密码") }, singleLine = true)
                Spacer(Modifier.height(10.dp))
                Text("同步会合并本地集合与 AnkiWeb 上的卡片。", color = Color(0xFF666B6F), fontSize = 13.sp)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        confirmButton = { Button(onClick = { attempted = true }) { Text("登录并同步") } }
    )
}
