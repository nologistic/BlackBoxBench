package com.blackboxbench.reproduction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.floor
import kotlin.random.Random

// ---------- palette ----------
private val BgColor = Color(0xFF0C1424)
private val PanelColor = Color(0xFF223040)
private val CellColor = Color(0xFF26374C)
private val CellRevealedColor = Color(0xFF111A28)
private val AccentTeal = Color(0xFF4ED0C4)
private val LedRed = Color(0xFFFF3B30)
private val LedOrange = Color(0xFFFFA726)
private val LedBoxColor = Color(0xFF170E14)
private val LoseBannerColor = Color(0xFF7F1D1D)
private val WinBannerColor = Color(0xFF1B5E20)
private val MineCellColor = Color(0xFF5B1F1F)
private val ExplodedCellColor = Color(0xFFE53935)
private val FadePurple = Color(0xFF5A4FA0)
private val FogPurple = Color(0xFF4A3F7A)
private val FogDotColor = Color(0xFF8A7FD0)
private val DialogColor = Color(0xFF1B2536)

private val numberColors = mapOf(
    1 to Color(0xFF4C8DFF),
    2 to Color(0xFF3FB950),
    3 to Color(0xFFF85149),
    4 to Color(0xFFBC8CF2),
    5 to Color(0xFFDA3633),
    6 to Color(0xFF39C5CF),
    7 to Color(0xFFE3B341),
    8 to Color(0xFFADB6C2),
)

// ---------- game model ----------
enum class Difficulty { EASY, MEDIUM, HARD, CUSTOM }

enum class Status { IDLE, PLAYING, WON, LOST }

data class Cell(
    val mine: Boolean = false,
    val revealed: Boolean = false,
    val flagged: Boolean = false,
    val adj: Int = 0,
    val revealTick: Int = -1000,
)

class Game(
    val rows: Int,
    val cols: Int,
    val mineCount: Int,
    val fog: Boolean,
    val safeFirst: Boolean,
    val timeLimitSec: Int, // 0 = count up
    val numberFade: Boolean,
) {
    var cells by mutableStateOf(List(rows * cols) { Cell() })
        private set
    var status by mutableStateOf(Status.IDLE)
        private set
    var flags by mutableIntStateOf(0)
        private set
    var tick by mutableIntStateOf(0)
        private set
    var explodedAt by mutableIntStateOf(-1)
        private set
    private var minesPlaced = false

    val remaining: Int get() = mineCount - flags
    val timeText: String
        get() {
            val v = if (timeLimitSec > 0) (timeLimitSec - tick).coerceAtLeast(0) else tick.coerceAtMost(99 * 60 + 59)
            return "%02d:%02d".format(v / 60, v % 60)
        }

    fun onTick() {
        if (status != Status.PLAYING) return
        tick++
        if (timeLimitSec > 0 && tick >= timeLimitSec) lose(-1)
    }

    private fun placeMines(safeIdx: Int) {
        val excluded = mutableSetOf<Int>()
        if (safeFirst) {
            excluded.add(safeIdx)
            neighbors(safeIdx).forEach { excluded.add(it) }
        }
        val pool = cells.indices.filter { it !in excluded }
        val chosen = pool.shuffled(Random.Default).take(mineCount.coerceAtMost(pool.size)).toSet()
        cells = cells.indices.map { i ->
            val m = chosen.contains(i)
            val a = if (m) 0 else neighbors(i).count { chosen.contains(it) }
            Cell(mine = m, adj = a)
        }
        minesPlaced = true
    }

    fun neighbors(i: Int): List<Int> {
        val r = i / cols
        val c = i % cols
        val out = ArrayList<Int>(8)
        for (dr in -1..1) for (dc in -1..1) {
            if (dr == 0 && dc == 0) continue
            val nr = r + dr
            val nc = c + dc
            if (nr in 0 until rows && nc in 0 until cols) out.add(nr * cols + nc)
        }
        return out
    }

    fun reveal(i: Int) {
        if (status == Status.WON || status == Status.LOST) return
        val cell = cells[i]
        if (cell.revealed || cell.flagged) return
        if (!minesPlaced) placeMines(i)
        if (status == Status.IDLE) status = Status.PLAYING
        if (cells[i].mine) {
            lose(i)
            return
        }
        val mutable = cells.toMutableList()
        val stack = ArrayDeque<Int>()
        stack.add(i)
        while (stack.isNotEmpty()) {
            val cur = stack.removeLast()
            val cc = mutable[cur]
            if (cc.revealed || cc.flagged || cc.mine) continue
            mutable[cur] = cc.copy(revealed = true, revealTick = tick)
            if (cc.adj == 0) neighbors(cur).forEach { if (!mutable[it].revealed) stack.add(it) }
        }
        cells = mutable
        checkWin()
    }

    fun toggleFlag(i: Int) {
        if (status == Status.WON || status == Status.LOST) return
        val cell = cells[i]
        if (cell.revealed) return
        val mutable = cells.toMutableList()
        mutable[i] = cell.copy(flagged = !cell.flagged)
        cells = mutable
        flags += if (mutable[i].flagged) 1 else -1
    }

    private fun checkWin() {
        val revealedCount = cells.count { it.revealed }
        if (revealedCount == rows * cols - mineCount) {
            status = Status.WON
            // auto-flag every mine
            cells = cells.map { if (it.mine && !it.flagged) it.copy(flagged = true) else it }
            flags = mineCount
        }
    }

    private fun lose(exploded: Int) {
        status = Status.LOST
        explodedAt = exploded
    }

    fun numberVisible(cell: Cell): Boolean {
        if (!numberFade && !fog) return true
        return tick - cell.revealTick <= 2
    }

    fun isFogged(i: Int): Boolean {
        if (!fog) return false
        val cell = cells[i]
        if (cell.revealed) return !numberVisible(cell)
        if (cells.none { it.revealed }) return false
        return neighbors(i).none { cells[it].revealed }
    }
}

// ---------- activity ----------
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MinesweeperApp() }
    }
}

@Composable
fun MinesweeperApp() {
    var difficulty by remember { mutableStateOf(Difficulty.EASY) }
    var customRows by remember { mutableIntStateOf(16) }
    var customMines by remember { mutableIntStateOf(40) }
    var customFog by remember { mutableStateOf(false) }
    var customSafeFirst by remember { mutableStateOf(true) }
    var showCustomDialog by remember { mutableStateOf(false) }
    var showLicenses by remember { mutableStateOf(false) }

    fun newGame(d: Difficulty): Game = when (d) {
        Difficulty.EASY -> Game(9, 9, 10, fog = false, safeFirst = true, timeLimitSec = 0, numberFade = false)
        Difficulty.MEDIUM -> Game(12, 12, 30, fog = false, safeFirst = true, timeLimitSec = 300, numberFade = false)
        Difficulty.HARD -> Game(14, 14, 50, fog = false, safeFirst = true, timeLimitSec = 180, numberFade = true)
        Difficulty.CUSTOM -> Game(
            customRows, customRows, customMines.coerceIn(1, floor(customRows * customRows * 0.75).toInt()),
            fog = customFog, safeFirst = customSafeFirst, timeLimitSec = 0, numberFade = false
        )
    }

    var game by remember { mutableStateOf(newGame(Difficulty.EASY)) }

    LaunchedEffect(game) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            game.onTick()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .padding(horizontal = 10.dp),
    ) {
        Spacer(Modifier.height(46.dp))
        // header: counter - smiley - timer
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LedDisplay(text = "%3d".format(game.remaining), color = LedRed)
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PanelColor)
                    .tapActions(onClick = {
                        game = newGame(difficulty)
                    }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = when (game.status) {
                        Status.WON -> "😎"
                        Status.LOST -> "😵"
                        else -> "😊"
                    },
                    fontSize = 28.sp,
                )
            }
            Spacer(Modifier.weight(1f))
            LedDisplay(
                text = game.timeText,
                color = if (game.timeLimitSec > 0) LedOrange else LedRed,
            )
        }
        Spacer(Modifier.height(14.dp))
        // difficulty row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DifficultyButton("Easy", "10 💣", difficulty == Difficulty.EASY, Modifier.weight(1f)) {
                difficulty = Difficulty.EASY
                game = newGame(Difficulty.EASY)
            }
            DifficultyButton("Medium", "⏱", difficulty == Difficulty.MEDIUM, Modifier.weight(1f)) {
                difficulty = Difficulty.MEDIUM
                game = newGame(Difficulty.MEDIUM)
            }
            DifficultyButton("Hard", "⏱ 👁 ⚡", difficulty == Difficulty.HARD, Modifier.weight(1f)) {
                difficulty = Difficulty.HARD
                game = newGame(Difficulty.HARD)
            }
            DifficultyButton("Custom", "⚙", difficulty == Difficulty.CUSTOM, Modifier.weight(1f)) {
                showCustomDialog = true
            }
        }
        // result banner
        when (game.status) {
            Status.WON -> ResultBanner("🎉  You Win!  🎉", WinBannerColor)
            Status.LOST -> ResultBanner("💥  Game Over  💥", LoseBannerColor)
            else -> Spacer(Modifier.height(0.dp))
        }
        Spacer(Modifier.weight(1f))
        Board(game)
        Spacer(Modifier.weight(1.4f))
        Text(
            text = "Licenses",
            color = Color(0xFF7A8699),
            fontSize = 13.sp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(6.dp))
                .tapActions(onClick = { showLicenses = true })
                .padding(8.dp),
        )
        Spacer(Modifier.height(20.dp))
    }

    if (showCustomDialog) {
        CustomGameDialog(
            grid = customRows,
            mines = customMines,
            fog = customFog,
            safeFirst = customSafeFirst,
            onDismiss = { showCustomDialog = false },
            onStart = { g, m, f, s ->
                customRows = g
                customMines = m
                customFog = f
                customSafeFirst = s
                difficulty = Difficulty.CUSTOM
                game = newGame(Difficulty.CUSTOM)
                showCustomDialog = false
            },
        )
    }

    if (showLicenses) {
        LicensesDialog { showLicenses = false }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.tapActions(onClick: () -> Unit, onLongClick: (() -> Unit)? = null): Modifier =
    this.combinedClickable(onClick = onClick, onLongClick = onLongClick)

@Composable
fun DifficultyButton(name: String, subtitle: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) AccentTeal else PanelColor)
            .tapActions(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                name,
                color = if (selected) Color(0xFF0C1424) else Color(0xFFAAB6C6),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            )
            Text(
                subtitle,
                color = if (selected) Color(0xFF0C1424) else Color(0xFF7A8699),
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
fun ResultBanner(text: String, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(color)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

// ---------- board ----------
@Composable
fun Board(game: Game) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        for (r in 0 until game.rows) {
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                for (c in 0 until game.cols) {
                    val i = r * game.cols + c
                    CellView(game, i, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun CellView(game: Game, i: Int, modifier: Modifier) {
    val cell = game.cells[i]
    val over = game.status == Status.LOST
    val won = game.status == Status.WON

    var bg: Color = CellColor
    var content: String? = null
    var contentColor = Color.Unspecified
    var showFogDot = false

    when {
        over && cell.mine && i == game.explodedAt -> {
            bg = ExplodedCellColor
            content = "💥"
        }
        over && cell.mine && !game.isFogged(i) -> {
            bg = MineCellColor
            content = "💣"
        }
        over && game.fog && game.isFogged(i) -> {
            bg = FogPurple
            showFogDot = true
        }
        cell.flagged -> {
            bg = CellColor
            content = "🚩"
        }
        cell.revealed -> {
            if (game.numberVisible(cell)) {
                bg = CellRevealedColor
                if (cell.adj > 0) {
                    content = cell.adj.toString()
                    contentColor = numberColors[cell.adj] ?: Color.White
                }
            } else {
                bg = FadePurple
            }
        }
        game.isFogged(i) -> {
            bg = FogPurple
            showFogDot = true
        }
        else -> bg = CellColor
    }
    if (won && cell.mine) {
        bg = CellColor
        content = "🚩"
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .tapActions(
                onClick = { game.reveal(i) },
                onLongClick = { game.toggleFlag(i) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (content != null) {
            Text(
                content,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                fontSize = if (game.cols >= 14) 13.sp else if (game.cols >= 12) 15.sp else 18.sp,
            )
        } else if (showFogDot) {
            Box(
                Modifier
                    .size(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(FogDotColor)
            )
        }
    }
}

// ---------- seven segment LED ----------
private val segmentMap = mapOf(
    '0' to "abcfed".toSet(), // a b c d e f
    '1' to setOf('b', 'c'),
    '2' to setOf('a', 'b', 'g', 'e', 'd'),
    '3' to setOf('a', 'b', 'g', 'c', 'd'),
    '4' to setOf('f', 'g', 'b', 'c'),
    '5' to setOf('a', 'f', 'g', 'c', 'd'),
    '6' to setOf('a', 'f', 'g', 'e', 'c', 'd'),
    '7' to setOf('a', 'b', 'c'),
    '8' to setOf('a', 'b', 'c', 'd', 'e', 'f', 'g'),
    '9' to setOf('a', 'b', 'c', 'd', 'f', 'g'),
    '-' to setOf('g'),
)

@Composable
fun LedDisplay(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(LedBoxColor)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            text.forEach { ch ->
                if (ch == ':') {
                    LedColon(color)
                } else {
                    LedDigit(ch, color)
                }
            }
        }
    }
}

@Composable
fun LedColon(color: Color) {
    Canvas(Modifier.size(width = 8.dp, height = 26.dp)) {
        val r = size.width * 0.22f
        drawCircle(color, r, Offset(size.width / 2, size.height * 0.32f))
        drawCircle(color, r, Offset(size.width / 2, size.height * 0.68f))
    }
}

@Composable
fun LedDigit(ch: Char, color: Color) {
    val lit = segmentMap[ch] ?: emptySet()
    Canvas(Modifier.size(width = 18.dp, height = 26.dp)) {
        val w = size.width
        val h = size.height
        val t = h * 0.075f // half thickness
        fun segPath(horizontal: Boolean, cx: Float, cy: Float, len: Float): Path {
            val p = Path()
            if (horizontal) {
                p.moveTo(cx - len / 2, cy)
                p.lineTo(cx - len / 2 + t, cy - t)
                p.lineTo(cx + len / 2 - t, cy - t)
                p.lineTo(cx + len / 2, cy)
                p.lineTo(cx + len / 2 - t, cy + t)
                p.lineTo(cx - len / 2 + t, cy + t)
                p.close()
            } else {
                p.moveTo(cx, cy - len / 2)
                p.lineTo(cx + t, cy - len / 2 + t)
                p.lineTo(cx + t, cy + len / 2 - t)
                p.lineTo(cx, cy + len / 2)
                p.lineTo(cx - t, cy + len / 2 - t)
                p.lineTo(cx - t, cy - len / 2 + t)
                p.close()
            }
            return p
        }
        val segs = mapOf(
            'a' to segPath(true, w / 2, t, w - 4 * t),
            'g' to segPath(true, w / 2, h / 2, w - 4 * t),
            'd' to segPath(true, w / 2, h - t, w - 4 * t),
            'f' to segPath(false, t, h / 4, h / 2 - 3 * t),
            'b' to segPath(false, w - t, h / 4, h / 2 - 3 * t),
            'e' to segPath(false, t, 3 * h / 4, h / 2 - 3 * t),
            'c' to segPath(false, w - t, 3 * h / 4, h / 2 - 3 * t),
        )
        segs.forEach { (name, path) ->
            drawPath(path, color.copy(alpha = if (lit.contains(name)) 1f else 0.10f))
        }
    }
}

// ---------- custom game dialog ----------
@Composable
fun CustomGameDialog(
    grid: Int,
    mines: Int,
    fog: Boolean,
    safeFirst: Boolean,
    onDismiss: () -> Unit,
    onStart: (Int, Int, Boolean, Boolean) -> Unit,
) {
    var g by remember { mutableIntStateOf(grid) }
    val maxMines = floor(g * g * 0.75).toInt().coerceAtLeast(1)
    var m by remember { mutableIntStateOf(mines) }
    if (m > maxMines) m = maxMines
    var f by remember { mutableStateOf(fog) }
    var s by remember { mutableStateOf(safeFirst) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(DialogColor)
                .padding(24.dp),
        ) {
            Text("Custom Game", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Grid size", color = Color(0xFFAAB6C6), fontSize = 15.sp)
                Spacer(Modifier.weight(1f))
                Text("$g × $g", color = AccentTeal, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            androidx.compose.material3.Slider(
                value = g.toFloat(),
                onValueChange = { g = it.toInt() },
                valueRange = 6f..20f,
                steps = 13,
                colors = androidx.compose.material3.SliderDefaults.colors(
                    thumbColor = AccentTeal,
                    activeTrackColor = AccentTeal,
                    inactiveTrackColor = Color(0xFF33415A),
                ),
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Mines", color = Color(0xFFAAB6C6), fontSize = 15.sp)
                Spacer(Modifier.weight(1f))
                Text(
                    "$m (${(m * 100 / (g * g)).toInt()}%)",
                    color = AccentTeal,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            androidx.compose.material3.Slider(
                value = m.toFloat(),
                onValueChange = { m = it.toInt().coerceIn(1, maxMines) },
                valueRange = 1f..maxMines.toFloat(),
                colors = androidx.compose.material3.SliderDefaults.colors(
                    thumbColor = AccentTeal,
                    activeTrackColor = AccentTeal,
                    inactiveTrackColor = Color(0xFF33415A),
                ),
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Fog of war", color = Color(0xFFAAB6C6), fontSize = 15.sp)
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = f,
                    onCheckedChange = { f = it },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = AccentTeal,
                        uncheckedTrackColor = Color(0xFF33415A),
                    ),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Safe first tap", color = Color(0xFFAAB6C6), fontSize = 15.sp)
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = s,
                    onCheckedChange = { s = it },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = AccentTeal,
                        uncheckedTrackColor = Color(0xFF33415A),
                    ),
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancel", color = Color(0xFF7A8699)) }
                TextButton(onClick = { onStart(g, m, f, s) }) { Text("Start", color = AccentTeal, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

// ---------- licenses ----------
private val licenseText = """
Minesweeper is free software under the GNU General Public License v3.0.
The full text ships with the source at github.com/john-athan/minesweeper.

The libraries below are used under the Apache License 2.0. Their attribution and the full license text follow.

    androidx.compose (ui, ui-graphics, foundation, animation)
    androidx.compose.material3
    androidx.activity:activity-compose
    androidx.core:core-ktx
        Copyright (c) The Android Open Source Project

    Kotlin standard library
        Copyright (c) JetBrains s.r.o.

                                 Apache License
                           Version 2.0, January 2004
                        http://www.apache.org/licenses/

   TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

   1. Definitions.

      "License" shall mean the terms and conditions for use, reproduction,
      and distribution as defined by Sections 1 through 9 of this document.

      "Licensor" shall mean the copyright owner or entity authorized by
      the copyright owner that is granting the License.

      "Legal Entity" shall mean the union of the acting entity and all
      other entities that control, are controlled by, or are under common
      control with that entity. For the purposes of this definition,
      "control" means (i) the power, direct or indirect, to cause the
      direction or management of such entity, whether by contract or
      otherwise, or (ii) ownership of fifty percent (50%) or more of the
      outstanding shares, or (iii) beneficial ownership of such entity.

      "You" (or "Your") shall mean an individual or Legal Entity
      exercising permissions granted by this License.

      "Source" form shall mean the preferred form for making modifications,
      including but not limited to software source code, documentation
      source, and configuration files.

      "Object" form shall mean any form resulting from mechanical
      transformation or translation of a Source form, including but
      not limited to compiled object code, generated documentation,
      and conversions to other media types.

      "Work" shall mean the work of authorship, whether in Source or
      Object form, made available under the License, as indicated by a
      copyright notice that is included in or attached to the work.

      "Derivative Works" shall mean any work, whether in Source or Object
      form, that is based on (or derived from) the Work and for which the
      editorial revisions, annotations, elaborations, or other modifications
      represent, as a whole, an original work of authorship.

   2. Grant of Copyright License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      copyright license to reproduce, prepare Derivative Works of,
      publicly display, publicly perform, sublicense, and distribute the
      Work and such Derivative Works in Source or Object form.

   3. Grant of Patent License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      patent license to make, have made, use, offer to sell, sell, import,
      and otherwise transfer the Work.

   4. Redistribution. You may reproduce and distribute copies of the
      Work or Derivative Works thereof in any medium, with or without
      modifications, and in Source or Object form, provided that You
      meet the conditions of the License.

   5. Submission of Contributions. Unless You explicitly state otherwise,
      any Contribution intentionally submitted for inclusion in the Work
      by You to the Licensor shall be under the terms and conditions of
      this License.

   6. Trademarks. This License does not grant permission to use the trade
      names, trademarks, service marks, or product names of the Licensor.

   7. Disclaimer of Warranty. Unless required by applicable law or
      agreed to in writing, Licensor provides the Work on an "AS IS"
      BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND.

   8. Limitation of Liability. In no event and under no legal theory
      shall any Contributor be liable for damages arising out of the use
      of the Work.

   9. Accepting Warranty or Additional Liability. While redistributing
      the Work or Derivative Works thereof, You may choose to offer
      support, warranty, indemnity, or other liability obligations.

   END OF TERMS AND CONDITIONS
""".trimIndent()

@Composable
fun LicensesDialog(onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(DialogColor)
                .padding(24.dp),
        ) {
            Text("Licenses", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(16.dp))
            Text(
                licenseText,
                color = Color(0xFFAAB6C6),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
            )
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onClose) { Text("Close", color = Color(0xFF8AB4F8)) }
            }
        }
    }
}
