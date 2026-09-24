package com.blackboxbench.reproduction

import android.content.Context
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.random.Random

private val Bg = Color(0xFF0E0E0E)
private val Card = Color(0xFF1B1B1B)
private val Btn = Color(0xFF242424)
private val BtnBorder = Color(0xFF3A3A3A)
private val CellBg = Color(0xFF232323)
private val TxtGray = Color(0xFF8E8E93)
private val Accent = Color(0xFFFFC90A)
private val Red = Color(0xFFFF453A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NonogramApp() }
    }
}

private class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("nonogram", Context.MODE_PRIVATE)
    var highestUnlocked: Int
        get() = sp.getInt("highestUnlocked", 1)
        set(v) = sp.edit().putInt("highestUnlocked", v).apply()
    var completedMask: Int
        get() = sp.getInt("completedMask", 0)
        set(v) = sp.edit().putInt("completedMask", v).apply()
    var haptic: Boolean
        get() = sp.getBoolean("haptic", true)
        set(v) = sp.edit().putBoolean("haptic", v).apply()
    var longPress: Boolean
        get() = sp.getBoolean("longPress", true)
        set(v) = sp.edit().putBoolean("longPress", v).apply()
    var cycle: Boolean
        get() = sp.getBoolean("cycle", false)
        set(v) = sp.edit().putBoolean("cycle", v).apply()

    fun saveGame(key: String, board: String, moves: Int, elapsed: Int) {
        sp.edit().putString("save_key", key).putString("save_board", board)
            .putInt("save_moves", moves).putInt("save_elapsed", elapsed).apply()
    }

    fun loadGame(key: String): Triple<String, Int, Int>? {
        if (sp.getString("save_key", null) != key) return null
        val b = sp.getString("save_board", null) ?: return null
        return Triple(b, sp.getInt("save_moves", 0), sp.getInt("save_elapsed", 0))
    }

    fun clearSavedGame() {
        sp.edit().remove("save_key").remove("save_board").remove("save_moves")
            .remove("save_elapsed").apply()
    }

    fun resetProgress() {
        highestUnlocked = 1
        completedMask = 0
        clearSavedGame()
    }
}

private object Levels {
    val rows: List<List<String>> = listOf(
        listOf("11000", "11010", "01101", "11011", "11001"),
        listOf("11111", "00011", "11111", "01010", "11011"),
        listOf("10001", "01010", "00100", "01010", "10001"),
        listOf("00100", "01110", "11111", "01110", "00100"),
        listOf("01010", "11111", "11111", "01110", "00100"),
        listOf("00100", "00100", "11111", "00100", "00100"),
        listOf("10000", "11000", "11100", "11110", "11111"),
        listOf("10101", "01010", "10101", "01010", "10101"),
        listOf("10001", "10001", "10001", "10001", "01110"),
        listOf("10001", "10001", "11111", "10001", "10001"),
        listOf("11111", "10001", "10101", "10001", "11111"),
        listOf("00100", "00010", "11111", "00010", "00100"),
        listOf("10101", "01110", "11111", "01010", "10001")
    )
    const val COUNT = 13
}

private fun lineClues(line: String): List<Int> {
    val out = ArrayList<Int>()
    var run = 0
    for (ch in line) {
        if (ch == '1') run++ else if (run > 0) { out.add(run); run = 0 }
    }
    if (run > 0) out.add(run)
    return if (out.isEmpty()) listOf(0) else out
}

private class Puzzle(val size: Int, val grid: List<String>) {
    val rowClues: List<List<Int>> = grid.map { lineClues(it) }
    val colClues: List<List<Int>> = (0 until size).map { c ->
        lineClues(grid.map { it[c] }.joinToString(""))
    }
    val maxRowClue = rowClues.maxOf { it.size }
    val maxColClue = colClues.maxOf { it.size }
    fun filled(r: Int, c: Int) = grid[r][c] == '1'
}

private fun puzzleFromSeed(seed: Long, size: Int): Puzzle {
    val rnd = Random(seed)
    val rows = (0 until size).map {
        (0 until size).map { if (rnd.nextFloat() < 0.55f) '1' else '0' }.joinToString("")
    }
    return Puzzle(size, rows)
}

private enum class Screen { MENU, SELECT, GAME, MULTI, HOWTO, SETTINGS }

private sealed class GameRequest {
    abstract val saveKey: String
    abstract val title: String
    abstract val puzzle: Puzzle

    class Level(val level: Int) : GameRequest() {
        override val saveKey get() = "L$level"
        override val title get() = "LEVEL $level"
        override val puzzle get() = Puzzle(5, Levels.rows[level - 1])
    }

    class RandomP(val difficulty: String, val size: Int, val seed: Long) : GameRequest() {
        override val saveKey get() = "R$difficulty$seed"
        override val title get() = ""
        override val puzzle get() = puzzleFromSeed(seed, size)
    }

    class Multi(val code: String, val size: Int) : GameRequest() {
        override val saveKey get() = "M$code"
        override val title get() = ""
        override val puzzle get() = puzzleFromSeed(code.hashCode().toLong(), size)
    }
}

private val DIFF_SIZES = linkedMapOf(
    "EASY" to 5, "MEDIUM" to 7, "HARD" to 8, "MASTER" to 10, "EXPERT" to 12
)

@Composable
fun NonogramApp() {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    var screen by remember { mutableStateOf(Screen.MENU) }
    var backTarget by remember { mutableStateOf(Screen.MENU) }
    var gameRequest by remember { mutableStateOf<GameRequest?>(null) }
    var progressTick by remember { mutableIntStateOf(0) }

    fun go(s: Screen) { screen = s }
    fun startGame(req: GameRequest, from: Screen) {
        gameRequest = req
        backTarget = from
        screen = Screen.GAME
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Bg) {
        when (screen) {
            Screen.MENU -> MenuScreen(
                badgeLevel = prefs.highestUnlocked.coerceAtMost(Levels.COUNT),
                tick = progressTick,
                onPlay = { startGame(GameRequest.Level(prefs.highestUnlocked.coerceAtMost(Levels.COUNT)), Screen.MENU) },
                onSelect = { go(Screen.SELECT) },
                onRandom = { req -> startGame(req, Screen.MENU) },
                onMulti = { go(Screen.MULTI) },
                onHowTo = { go(Screen.HOWTO) },
                onSettings = { go(Screen.SETTINGS) }
            )
            Screen.SELECT -> SelectScreen(
                highest = prefs.highestUnlocked,
                completedMask = prefs.completedMask,
                onBack = { go(Screen.MENU) },
                onPick = { lvl -> startGame(GameRequest.Level(lvl), Screen.SELECT) }
            )
            Screen.GAME -> gameRequest?.let { req ->
                GameScreen(
                    prefs = prefs,
                    request = req,
                    onExit = {
                        progressTick++
                        if (backTarget == Screen.SELECT) go(Screen.SELECT) else go(Screen.MENU)
                    },
                    onNextLevel = { next ->
                        progressTick++
                        startGame(next, Screen.MENU)
                    },
                    onHome = {
                        progressTick++
                        go(Screen.MENU)
                    }
                )
            }
            Screen.MULTI -> MultiScreen(
                onBack = { go(Screen.MENU) },
                onStart = { req -> startGame(req, Screen.MULTI) }
            )
            Screen.HOWTO -> HowToScreen(onBack = { go(Screen.MENU) })
            Screen.SETTINGS -> SettingsScreen(
                prefs = prefs,
                onBack = {
                    progressTick++
                    go(Screen.MENU)
                }
            )
        }
    }
}

@Composable
private fun MenuButton(label: String, primary: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (primary) Color(0xFF080808) else Btn)
            .border(1.dp, if (primary) Color(0xFF2A2A2A) else BtnBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
    }
}

@Composable
private fun MenuScreen(
    badgeLevel: Int,
    tick: Int,
    onPlay: () -> Unit,
    onSelect: () -> Unit,
    onRandom: (GameRequest.RandomP) -> Unit,
    onMulti: () -> Unit,
    onHowTo: () -> Unit,
    onSettings: () -> Unit
) {
    var showDifficulty by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize().background(Bg).padding(horizontal = 26.dp, vertical = 22.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.size(52.dp).clip(CircleShape).background(Card)
                    .border(1.dp, BtnBorder, CircleShape).clickable { },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Star, contentDescription = null, tint = Accent, modifier = Modifier.size(26.dp)) }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier.clip(RoundedCornerShape(50)).background(Card)
                    .border(1.dp, BtnBorder, RoundedCornerShape(50)).padding(horizontal = 22.dp, vertical = 10.dp)
            ) {
                Text("LEVEL $badgeLevel", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 1.sp)
            }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier.size(52.dp).clip(CircleShape).background(Card)
                    .border(1.dp, BtnBorder, CircleShape).clickable { },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Favorite, contentDescription = null, tint = Red, modifier = Modifier.size(24.dp)) }
        }
        Spacer(Modifier.height(92.dp))
        Text(
            "NONOGRAM", color = Color.White, fontSize = 52.sp, fontWeight = FontWeight.Black,
            letterSpacing = 2.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "PICTURE LOGIC PUZZLE", color = TxtGray, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
        )
        Spacer(Modifier.weight(1f))
        MenuButton("PLAY", primary = true, onClick = onPlay)
        Spacer(Modifier.height(16.dp))
        MenuButton("SELECT LEVEL", onClick = onSelect)
        Spacer(Modifier.height(16.dp))
        MenuButton("RANDOM PUZZLE", onClick = { showDifficulty = true })
        Spacer(Modifier.height(16.dp))
        MenuButton("MULTIPLAYER", onClick = onMulti)
        Spacer(Modifier.height(16.dp))
        MenuButton("HOW TO PLAY", onClick = onHowTo)
        Spacer(Modifier.height(16.dp))
        MenuButton("SETTINGS", onClick = onSettings)
        Spacer(Modifier.height(24.dp))
    }
    if (showDifficulty) {
        DifficultyDialog(
            onDismiss = { showDifficulty = false },
            onPick = { diff ->
                showDifficulty = false
                val size = DIFF_SIZES.getValue(diff)
                onRandom(GameRequest.RandomP(diff, size, Random.nextLong()))
            }
        )
    }
}

@Composable
private fun DifficultyDialog(onDismiss: () -> Unit, onPick: (String) -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xAA000000)).clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(36.dp).clip(RoundedCornerShape(24.dp))
                .background(Card).clickable { }.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            DIFF_SIZES.keys.forEach { diff ->
                Box(
                    modifier = Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(14.dp))
                        .background(Btn).border(1.dp, BtnBorder, RoundedCornerShape(14.dp))
                        .clickable { onPick(diff) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(diff, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 2.sp)
                }
            }
        }
    }
}

@Composable
private fun ScreenHeader(title: String, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(top = 10.dp)) {
        Box(
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp)
                .size(56.dp).clip(CircleShape).clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Text(
            title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp,
            modifier = Modifier.align(Alignment.Center), textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SelectScreen(highest: Int, completedMask: Int, onBack: () -> Unit, onPick: (Int) -> Unit) {
    BackHandler(onBack = onBack)
    Column(modifier = Modifier.fillMaxSize().background(Bg)) {
        ScreenHeader("SELECT LEVEL", onBack)
        Spacer(Modifier.height(28.dp))
        val cell = 86.dp
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            for (row in 0 until 4) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    for (col in 0 until 4) {
                        val idx = row * 4 + col + 1
                        if (idx > Levels.COUNT) { Spacer(Modifier.size(cell)); continue }
                        val done = (completedMask shr (idx - 1)) and 1 == 1
                        val unlocked = idx <= highest
                        Box(
                            modifier = Modifier.size(cell).clip(RoundedCornerShape(22.dp))
                                .background(if (done) Color(0xFF3A3A3C) else if (unlocked) Btn else Color(0xFF171717))
                                .border(
                                    2.dp,
                                    if (done) Color(0xFFD8D8D8) else if (unlocked) BtnBorder else Color(0xFF222222),
                                    RoundedCornerShape(22.dp)
                                )
                                .clickable(enabled = unlocked) { onPick(idx) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (unlocked) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$idx", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                    if (done) {
                                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                            } else {
                                Icon(Icons.Filled.Lock, contentDescription = null, tint = Color(0xFF6E6E73), modifier = Modifier.size(26.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GameScreen(
    prefs: Prefs,
    request: GameRequest,
    onExit: () -> Unit,
    onNextLevel: (GameRequest) -> Unit,
    onHome: () -> Unit
) {
    val puzzle = remember(request.saveKey) { request.puzzle }
    val n = puzzle.size
    val saved = remember(request.saveKey) { prefs.loadGame(request.saveKey) }
    val cells = remember(request.saveKey) {
        val init = IntArray(n * n)
        if (saved != null && saved.first.length == n * n) {
            for (i in 0 until n * n) init[i] = saved.first[i] - '0'
        }
        init.toList().toMutableStateList()
    }
    var moves by remember(request.saveKey) { mutableIntStateOf(saved?.second ?: 0) }
    var elapsed by remember(request.saveKey) { mutableIntStateOf(saved?.third ?: 0) }
    var completed by remember(request.saveKey) { mutableStateOf(false) }
    var fillMode by remember { mutableStateOf(true) }
    val undoStack = remember(request.saveKey) { ArrayDeque<Pair<Int, Int>>() }
    val view = LocalView.current

    fun persist() {
        if (completed) return
        val sb = StringBuilder(n * n)
        for (i in 0 until n * n) sb.append(cells[i])
        prefs.saveGame(request.saveKey, sb.toString(), moves, elapsed)
    }

    fun checkWin(): Boolean {
        for (r in 0 until n) for (c in 0 until n) {
            val idx = r * n + c
            if (puzzle.filled(r, c) && cells[idx] != 1) return false
            if (!puzzle.filled(r, c) && cells[idx] == 1) return false
        }
        return true
    }

    fun afterChange() {
        if (checkWin()) {
            completed = true
            prefs.clearSavedGame()
        } else persist()
    }

    fun tapCell(idx: Int) {
        if (completed) return
        if (prefs.haptic) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        val prev = cells[idx]
        val next = if (prefs.cycle) (prev + 1) % 3 else if (fillMode) (if (prev == 1) 0 else 1) else (if (prev == 2) 0 else 2)
        if (next == prev) return
        undoStack.addLast(idx to prev)
        cells[idx] = next
        moves++
        afterChange()
    }

    fun longPressCell(idx: Int) {
        if (completed) return
        if (prefs.cycle) { tapCell(idx); return }
        if (!prefs.longPress) return
        if (prefs.haptic) view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        val prev = cells[idx]
        val next = if (prev == 2) 0 else 2
        undoStack.addLast(idx to prev)
        cells[idx] = next
        moves++
        afterChange()
    }

    LaunchedEffect(request.saveKey, completed) {
        while (!completed) {
            delay(1000)
            elapsed++
            persist()
        }
    }

    BackHandler(onBack = {
        persist()
        onExit()
    })

    Column(modifier = Modifier.fillMaxSize().background(Bg)) {
        ScreenHeader(request.title, onBack = {
            persist()
            onExit()
        })
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                String.format("%02d:%02d", elapsed / 60, elapsed % 60),
                color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            Text("MOVES: $moves", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf("UNDO", "HINT", "RESTART").forEach { label ->
                Box(
                    modifier = Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(23.dp))
                        .background(Btn).border(1.dp, BtnBorder, RoundedCornerShape(23.dp))
                        .clickable {
                            when (label) {
                                "UNDO" -> if (!completed && undoStack.isNotEmpty()) {
                                    val (idx, prev) = undoStack.removeLast()
                                    cells[idx] = prev
                                    if (moves > 0) moves--
                                    persist()
                                }
                                "RESTART" -> if (!completed) {
                                    for (i in 0 until n * n) cells[i] = 0
                                    undoStack.clear()
                                    moves = 0
                                    elapsed = 0
                                    persist()
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = if (label == "HINT") Accent else Color.White,
                        fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                    )
                }
            }
        }
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val clueWUnit = 22.dp
            val clueHUnit = 22.dp
            val rowClueW = clueWUnit * puzzle.maxRowClue + 10.dp
            val colClueH = clueHUnit * puzzle.maxColClue + 8.dp
            val availW = maxWidth - 40.dp - rowClueW
            val availH = maxHeight - colClueH - 16.dp
            val cellPx = min(availW.value / n, availH.value / n).coerceAtMost(76f)
            val cell = cellPx.dp
            Column(modifier = Modifier.align(Alignment.Center)) {
                Row {
                    Spacer(Modifier.width(rowClueW))
                    for (c in 0 until n) {
                        Box(modifier = Modifier.width(cell).height(colClueH), contentAlignment = Alignment.BottomCenter) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                puzzle.colClues[c].forEach { clue ->
                                    Text("$clue", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, lineHeight = 17.sp)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                for (r in 0 until n) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.width(rowClueW).height(cell), contentAlignment = Alignment.CenterEnd) {
                            Row {
                                puzzle.rowClues[r].forEach { clue ->
                                    Text("$clue ", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(Modifier.width(6.dp))
                        for (c in 0 until n) {
                            val idx = r * n + c
                            val state = cells[idx]
                            Box(
                                modifier = Modifier.size(cell).padding(2.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (state == 1) Color.White else CellBg)
                                    .combinedClickable(
                                        onClick = { tapCell(idx) },
                                        onLongClick = { longPressCell(idx) }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (state == 2) {
                                    Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                                        val w = size.width
                                        drawLine(Red, Offset(w * 0.15f, w * 0.15f), Offset(w * 0.85f, w * 0.85f), strokeWidth = 6f)
                                        drawLine(Red, Offset(w * 0.85f, w * 0.15f), Offset(w * 0.15f, w * 0.85f), strokeWidth = 6f)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (!prefs.cycle) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.weight(1f).height(60.dp).clip(RoundedCornerShape(18.dp))
                        .background(if (fillMode) Color.White else Btn)
                        .border(1.dp, if (fillMode) Color.White else BtnBorder, RoundedCornerShape(18.dp))
                        .clickable { fillMode = true },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(18.dp).background(if (fillMode) Color.Black else Color.White))
                        Spacer(Modifier.width(10.dp))
                        Text("FILL", color = if (fillMode) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 1.sp)
                    }
                }
                Box(
                    modifier = Modifier.weight(1f).height(60.dp).clip(RoundedCornerShape(18.dp))
                        .background(if (!fillMode) Color.White else Btn)
                        .border(1.dp, if (!fillMode) Color.White else BtnBorder, RoundedCornerShape(18.dp))
                        .clickable { fillMode = false },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("✕", color = Red, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Spacer(Modifier.width(10.dp))
                        Text("CROSS (X)", color = if (!fillMode) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 1.sp)
                    }
                }
            }
        } else {
            Spacer(Modifier.height(24.dp))
        }
    }

    if (completed) {
        CompletionDialog(
            puzzle = puzzle,
            moves = moves,
            elapsed = elapsed,
            onNext = {
                if (request is GameRequest.Level) {
                    val lvl = request.level
                    val mask = prefs.completedMask or (1 shl (lvl - 1))
                    prefs.completedMask = mask
                    if (lvl >= prefs.highestUnlocked && lvl < Levels.COUNT) prefs.highestUnlocked = lvl + 1
                    prefs.clearSavedGame()
                    if (lvl < Levels.COUNT) onNextLevel(GameRequest.Level(lvl + 1)) else onHome()
                } else if (request is GameRequest.RandomP) {
                    prefs.clearSavedGame()
                    onNextLevel(GameRequest.RandomP(request.difficulty, request.size, Random.nextLong()))
                } else if (request is GameRequest.Multi) {
                    prefs.clearSavedGame()
                    onHome()
                }
            },
            onCoffee = {
                prefs.clearSavedGame()
                onHome()
            },
            onHome = {
                if (request is GameRequest.Level) {
                    val lvl = request.level
                    val mask = prefs.completedMask or (1 shl (lvl - 1))
                    prefs.completedMask = mask
                    if (lvl >= prefs.highestUnlocked && lvl < Levels.COUNT) prefs.highestUnlocked = lvl + 1
                }
                prefs.clearSavedGame()
                onHome()
            }
        )
    }
}

@Composable
private fun CompletionDialog(
    puzzle: Puzzle,
    moves: Int,
    elapsed: Int,
    onNext: () -> Unit,
    onCoffee: () -> Unit,
    onHome: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0x66000000)), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(30.dp).clip(RoundedCornerShape(28.dp))
                .background(Card).border(1.dp, BtnBorder, RoundedCornerShape(28.dp)).padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("LEVEL COMPLETED!", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                "MOVES: $moves  •  TIME: ${String.format("%02d:%02d", elapsed / 60, elapsed % 60)}",
                color = TxtGray, fontSize = 15.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(Color(0xFF242424)).padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val cellDp = (200 / puzzle.size).coerceAtLeast(12).dp
                    for (r in 0 until puzzle.size) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (c in 0 until puzzle.size) {
                                Box(
                                    modifier = Modifier.size(cellDp).clip(RoundedCornerShape(3.dp))
                                        .background(if (puzzle.filled(r, c)) Color.White else Color(0xFF2E2E2E))
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF050505)).border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(16.dp))
                    .clickable(onClick = onNext),
                contentAlignment = Alignment.Center
            ) { Text("NEXT LEVEL", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp, letterSpacing = 2.sp) }
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(16.dp))
                    .background(Btn).border(1.dp, BtnBorder, RoundedCornerShape(16.dp))
                    .clickable(onClick = onCoffee),
                contentAlignment = Alignment.Center
            ) { Text("BUY ME A COFFEE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 2.sp) }
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(16.dp))
                    .background(Btn).border(1.dp, BtnBorder, RoundedCornerShape(16.dp))
                    .clickable(onClick = onHome),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Home, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("HOME", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 2.sp)
                }
            }
        }
    }
}

@Composable
private fun MultiScreen(onBack: () -> Unit, onStart: (GameRequest.Multi) -> Unit) {
    BackHandler(onBack = onBack)
    var difficulty by remember { mutableStateOf("EASY") }
    var digits by remember { mutableStateOf("%06d".format(Random.nextInt(1000000))) }
    var joinText by remember { mutableStateOf("") }
    var joinError by remember { mutableStateOf(false) }
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val code = "$difficulty-$digits"

    Column(modifier = Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState())) {
        ScreenHeader("MULTIPLAYER", onBack)
        Spacer(Modifier.height(20.dp))
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).clip(RoundedCornerShape(22.dp))
                .background(Card).padding(20.dp)
        ) {
            Text("DIFFICULTY", color = TxtGray, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(14.dp))
            val diffs = DIFF_SIZES.keys.toList()
            diffs.chunked(3).forEach { rowDiffs ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowDiffs.forEach { d ->
                        val sel = d == difficulty
                        Box(
                            modifier = Modifier.height(48.dp).clip(RoundedCornerShape(12.dp))
                                .background(if (sel) Color.White else Btn)
                                .border(1.dp, if (sel) Color.White else BtnBorder, RoundedCornerShape(12.dp))
                                .clickable { difficulty = d }
                                .padding(horizontal = 18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (sel) {
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text(d, color = if (sel) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
        Spacer(Modifier.height(18.dp))
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).clip(RoundedCornerShape(22.dp))
                .background(Card).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("YOUR ROOM CODE", color = TxtGray, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(10.dp))
            Text(code, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.weight(1f).height(54.dp).clip(RoundedCornerShape(14.dp))
                        .background(Btn).border(1.dp, BtnBorder, RoundedCornerShape(14.dp))
                        .clickable { clipboard.setText(androidx.compose.ui.text.AnnotatedString(code)) },
                    contentAlignment = Alignment.Center
                ) { Text("COPY CODE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 2.sp) }
                Spacer(Modifier.width(14.dp))
                Box(
                    modifier = Modifier.size(44.dp).clickable { digits = "%06d".format(Random.nextInt(1000000)) },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp)) }
            }
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF050505)).border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(14.dp))
                    .clickable { onStart(GameRequest.Multi(code, DIFF_SIZES.getValue(difficulty))) },
                contentAlignment = Alignment.Center
            ) { Text("START PUZZLE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 2.sp) }
        }
        Spacer(Modifier.height(18.dp))
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).clip(RoundedCornerShape(22.dp))
                .background(Card).padding(20.dp)
        ) {
            Text("JOIN WITH CODE", color = TxtGray, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = joinText,
                onValueChange = { joinText = it; joinError = false },
                placeholder = { Text("E.G. EASY-123456", color = Color(0xFF5A5A5E), fontWeight = FontWeight.Bold) },
                singleLine = true,
                isError = joinError,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = BtnBorder,
                    unfocusedBorderColor = BtnBorder,
                    errorBorderColor = Red,
                    focusedContainerColor = Btn,
                    unfocusedContainerColor = Btn
                ),
                shape = RoundedCornerShape(14.dp)
            )
            if (joinError) {
                Spacer(Modifier.height(8.dp))
                Text("FORMAT: DIFFICULTY-CODE (E.G. EASY-123456)", color = Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(14.dp))
                    .background(Btn).border(1.dp, BtnBorder, RoundedCornerShape(14.dp))
                    .clickable {
                        val v = joinText.trim().uppercase()
                        val m = Regex("^(EASY|MEDIUM|HARD|MASTER|EXPERT)-[0-9]{6}$").matchEntire(v)
                        if (m == null) {
                            joinError = true
                        } else {
                            val diff = v.substringBefore('-')
                            onStart(GameRequest.Multi(v, DIFF_SIZES.getValue(diff)))
                        }
                    },
                contentAlignment = Alignment.Center
            ) { Text("JOIN PUZZLE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 2.sp) }
        }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun HowToScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val cards = listOf(
        "READ THE CLUES" to "THE NUMBERS ABOVE EACH COLUMN AND NEXT TO EACH ROW SHOW HOW MANY CONSECUTIVE CELLS MUST BE FILLED IN THAT LINE.",
        "FILL CELLS" to "USE FILL MODE AND TAP A CELL TO FILL IT. TAP IT AGAIN TO CLEAR IT.",
        "MARK CROSSES" to "SWITCH TO CROSS MODE, OR LONG PRESS A CELL, TO MARK CELLS THAT MUST STAY EMPTY.",
        "COMPLETE THE PICTURE" to "FILL EVERY CORRECT CELL TO REVEAL THE HIDDEN PICTURE AND FINISH THE PUZZLE."
    )
    Column(modifier = Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState())) {
        ScreenHeader("HOW TO PLAY", onBack)
        Spacer(Modifier.height(20.dp))
        cards.forEach { (title, body) ->
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(20.dp)).background(Card).padding(20.dp)
            ) {
                Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Spacer(Modifier.height(8.dp))
                Text(body, color = TxtGray, fontSize = 13.sp, fontWeight = FontWeight.Bold, lineHeight = 19.sp, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
private fun SettingsScreen(prefs: Prefs, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    var haptic by remember { mutableStateOf(prefs.haptic) }
    var longPress by remember { mutableStateOf(prefs.longPress) }
    var cycle by remember { mutableStateOf(prefs.cycle) }
    var showReset by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Bg)) {
        ScreenHeader("SETTINGS", onBack)
        Spacer(Modifier.height(20.dp))
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(22.dp)).background(Card)
        ) {
            @Composable
            fun toggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
                    Switch(
                        checked = value,
                        onCheckedChange = onChange,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = Color(0xFF4A4A4E),
                            checkedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFF2A2A2E),
                            uncheckedThumbColor = Color(0xFF8E8E93),
                            uncheckedBorderColor = BtnBorder
                        )
                    )
                }
            }
            toggleRow("HAPTIC FEEDBACK", haptic) { haptic = it; prefs.haptic = it }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF2A2A2A)))
            toggleRow("LONG PRESS TO CROSS", longPress) { longPress = it; prefs.longPress = it }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF2A2A2A)))
            toggleRow("CYCLE MODE", cycle) { cycle = it; prefs.cycle = it }
        }
        Spacer(Modifier.height(22.dp))
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(58.dp)
                .clip(RoundedCornerShape(16.dp)).background(Btn)
                .border(1.dp, BtnBorder, RoundedCornerShape(16.dp))
                .clickable { showReset = true },
            contentAlignment = Alignment.Center
        ) { Text("RESET PROGRESS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 2.sp) }
    }

    if (showReset) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xAA000000)), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(36.dp).clip(RoundedCornerShape(26.dp))
                    .background(Card).border(1.dp, BtnBorder, RoundedCornerShape(26.dp)).padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = Red, modifier = Modifier.size(44.dp))
                Spacer(Modifier.height(14.dp))
                Text("RESET PROGRESS?", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Spacer(Modifier.height(10.dp))
                Text(
                    "ARE YOU SURE YOU WANT TO RESET ALL YOUR GAME PROGRESS?\nTHIS ACTION CANNOT BE UNDONE.",
                    color = TxtGray, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 19.sp
                )
                Spacer(Modifier.height(22.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF050505)).border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(14.dp))
                        .clickable {
                            prefs.resetProgress()
                            showReset = false
                        },
                    contentAlignment = Alignment.Center
                ) { Text("RESET PROGRESS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 2.sp) }
                Spacer(Modifier.height(12.dp))
                Text(
                    "CANCEL", color = TxtGray, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 2.sp,
                    modifier = Modifier.clickable { showReset = false }
                )
            }
        }
    }
}
