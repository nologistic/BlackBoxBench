package com.blackboxbench.reproduction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlin.math.floor
import kotlin.random.Random

// ---------- palette ----------
private val BgColor = Color(0xFF0B1120)
private val PanelColor = Color(0xFF1B2740)
private val CellColor = Color(0xFF22304A)
private val CellBorder = Color(0xFF2E3D5C)
private val RevealedColor = Color(0xFF111726)
private val FogColor = Color(0xFF6D28D9)
private val AccentTeal = Color(0xFF2DD4BF)
private val LedBg = Color(0xFF05070D)
private val LedRed = Color(0xFFFF3B30)
private val LedOrange = Color(0xFFFF9F0A)
private val BannerRed = Color(0xFF7F1D1D)
private val BannerGreen = Color(0xFF14532D)
private val MineCellBg = Color(0xFF6B1B1B)
private val ExplodedBg = Color(0xFFDC2626)

private enum class Difficulty(val label: String, val icon: String) {
    EASY("Easy", "10 💣"),
    MEDIUM("Medium", "⏱"),
    HARD("Hard", "⏱👁⚡"),
    CUSTOM("Custom", "⚙")
}

private data class GameConfig(
    val rows: Int,
    val cols: Int,
    val mines: Int,
    val countdownSeconds: Int?, // null = count up
    val fogOfWar: Boolean,
    val safeFirstTap: Boolean
)

private fun configFor(d: Difficulty, custom: GameConfig?): GameConfig = when (d) {
    Difficulty.EASY -> GameConfig(9, 9, 10, null, false, true)
    Difficulty.MEDIUM -> GameConfig(12, 12, 30, 300, false, true)
    Difficulty.HARD -> GameConfig(16, 16, 50, 180, true, true)
    Difficulty.CUSTOM -> custom ?: GameConfig(16, 16, 40, null, false, true)
}

private class CellState {
    var mine by mutableStateOf(false)
    var revealed by mutableStateOf(false)
    var flagged by mutableStateOf(false)
    var exploded by mutableStateOf(false)
    var adjacent by mutableIntStateOf(0)
}

private enum class GameStatus { READY, PLAYING, WON, LOST }

private class GameState(config0: GameConfig) {
    var config = config0
        private set
    var rows by mutableIntStateOf(config0.rows)
        private set
    var cols by mutableIntStateOf(config0.cols)
        private set
    var cells: List<List<CellState>> by mutableStateOf(newCells(config0.rows, config0.cols))
        private set
    var status by mutableStateOf(GameStatus.READY)
    var minesPlaced = false
    var flagsUsed by mutableIntStateOf(0)
    var elapsedSeconds by mutableIntStateOf(0)
    var revealedSafe by mutableIntStateOf(0)

    private fun newCells(r: Int, c: Int) = List(r) { List(c) { CellState() } }

    val remainingMines: Int get() = config.mines - flagsUsed
    val running: Boolean get() = status == GameStatus.PLAYING

    fun reset(newConfig: GameConfig) {
        config = newConfig
        rows = newConfig.rows
        cols = newConfig.cols
        cells = newCells(rows, cols)
        status = GameStatus.READY
        minesPlaced = false
        flagsUsed = 0
        elapsedSeconds = 0
        revealedSafe = 0
    }

    private fun placeMines(safeR: Int, safeC: Int) {
        var placed = 0
        while (placed < config.mines) {
            val r = Random.nextInt(rows)
            val c = Random.nextInt(cols)
            if (cells[r][c].mine) continue
            if (config.safeFirstTap && r == safeR && c == safeC) continue
            cells[r][c].mine = true
            placed++
        }
        for (r in 0 until rows) for (c in 0 until cols) {
            if (cells[r][c].mine) continue
            var n = 0
            for (dr in -1..1) for (dc in -1..1) {
                val nr = r + dr; val nc = c + dc
                if (nr in 0 until rows && nc in 0 until cols && cells[nr][nc].mine) n++
            }
            cells[r][c].adjacent = n
        }
        minesPlaced = true
    }

    fun reveal(r: Int, c: Int) {
        if (status == GameStatus.WON || status == GameStatus.LOST) return
        val cell = cells[r][c]
        if (cell.flagged) return
        if (cell.revealed) {
            chord(r, c)
            return
        }
        if (!minesPlaced) {
            placeMines(r, c)
            status = GameStatus.PLAYING
        }
        if (cell.mine) {
            cell.exploded = true
            lose()
            return
        }
        floodReveal(r, c)
        checkWin()
    }

    private fun floodReveal(sr: Int, sc: Int) {
        val stack = ArrayDeque<Pair<Int, Int>>()
        stack.add(sr to sc)
        while (stack.isNotEmpty()) {
            val (r, c) = stack.removeLast()
            val cell = cells[r][c]
            if (cell.revealed || cell.flagged || cell.mine) continue
            cell.revealed = true
            revealedSafe++
            if (cell.adjacent == 0) {
                for (dr in -1..1) for (dc in -1..1) {
                    val nr = r + dr; val nc = c + dc
                    if (nr in 0 until rows && nc in 0 until cols && !cells[nr][nc].revealed) {
                        stack.add(nr to nc)
                    }
                }
            }
        }
    }

    private fun chord(r: Int, c: Int) {
        val cell = cells[r][c]
        if (cell.adjacent == 0) return
        var flags = 0
        for (dr in -1..1) for (dc in -1..1) {
            val nr = r + dr; val nc = c + dc
            if (nr in 0 until rows && nc in 0 until cols && cells[nr][nc].flagged) flags++
        }
        if (flags != cell.adjacent) return
        for (dr in -1..1) for (dc in -1..1) {
            val nr = r + dr; val nc = c + dc
            if (nr in 0 until rows && nc in 0 until cols) {
                val n = cells[nr][nc]
                if (!n.flagged && !n.revealed) {
                    if (n.mine) {
                        n.exploded = true
                        lose()
                        return
                    }
                    floodReveal(nr, nc)
                }
            }
        }
        checkWin()
    }

    fun toggleFlag(r: Int, c: Int) {
        if (status == GameStatus.WON || status == GameStatus.LOST) return
        val cell = cells[r][c]
        if (cell.revealed) return
        cell.flagged = !cell.flagged
        flagsUsed += if (cell.flagged) 1 else -1
    }

    private fun checkWin() {
        if (revealedSafe == rows * cols - config.mines) {
            status = GameStatus.WON
            for (r in 0 until rows) for (c in 0 until cols) {
                if (cells[r][c].mine && !cells[r][c].flagged) {
                    cells[r][c].flagged = true
                    flagsUsed++
                }
            }
        }
    }

    fun lose() {
        status = GameStatus.LOST
    }

    fun tick() {
        if (!running) return
        elapsedSeconds++
        val limit = config.countdownSeconds
        if (limit != null && elapsedSeconds >= limit) {
            lose()
        }
    }

    fun timerText(): String {
        val limit = config.countdownSeconds
        val v = if (limit == null) elapsedSeconds else (limit - elapsedSeconds).coerceAtLeast(0)
        return "%02d:%02d".format(v / 60, v % 60)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ReproducedApp() }
    }
}

@Composable
fun ReproducedApp() {
    MaterialTheme(colorScheme = darkColorScheme(background = BgColor, surface = BgColor)) {
        Surface(modifier = Modifier.fillMaxSize(), color = BgColor) {
            MinesweeperScreen()
        }
    }
}

@Composable
private fun MinesweeperScreen() {
    var difficulty by remember { mutableStateOf(Difficulty.EASY) }
    var customConfig by remember { mutableStateOf(GameConfig(16, 16, 40, null, false, true)) }
    val game = remember { GameState(configFor(Difficulty.EASY, null)) }
    var showCustomDialog by remember { mutableStateOf(false) }
    var showLicenses by remember { mutableStateOf(false) }

    LaunchedEffect(game.running) {
        while (game.running) {
            delay(1000)
            game.tick()
        }
    }

    fun newGame(d: Difficulty) {
        difficulty = d
        game.reset(configFor(d, customConfig))
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(48.dp))
        // top HUD
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LedDisplay(text = "%03d".format(game.remainingMines.coerceAtLeast(0)), color = LedRed)
            val face = when (game.status) {
                GameStatus.WON -> "😎"
                GameStatus.LOST -> "😵"
                else -> "😊"
            }
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(PanelColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { newGame(difficulty) },
                contentAlignment = Alignment.Center
            ) {
                Text(face, fontSize = 40.sp)
            }
            LedDisplay(
                text = game.timerText(),
                color = if (game.config.countdownSeconds == null) LedRed else LedOrange
            )
        }
        Spacer(Modifier.height(16.dp))
        // difficulty row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (d in Difficulty.entries) {
                val selected = d == difficulty
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(104.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (selected) AccentTeal else PanelColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (d == Difficulty.CUSTOM) {
                                showCustomDialog = true
                            } else {
                                newGame(d)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            d.label,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selected) Color(0xFF0B1120) else Color(0xFFAAB4C8)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            d.icon,
                            fontSize = 15.sp,
                            color = if (selected) Color(0xFF0B1120) else Color(0xFFAAB4C8)
                        )
                    }
                }
            }
        }
        // result banner
        if (game.status == GameStatus.LOST || game.status == GameStatus.WON) {
            Spacer(Modifier.height(16.dp))
            val won = game.status == GameStatus.WON
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (won) BannerGreen else BannerRed)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (won) "🎉 You Win! 🎉" else "💥 Game Over 💥",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        // board, vertically centered in remaining space
        BoxWithConstraints(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val cellSize = maxWidth / game.cols
            Column {
                for (r in 0 until game.rows) {
                    Row {
                        for (c in 0 until game.cols) {
                            CellView(
                                game = game,
                                r = r,
                                c = c,
                                modifier = Modifier.size(cellSize)
                            )
                        }
                    }
                }
            }
        }
        // licenses link
        Text(
            "Licenses",
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { showLicenses = true }
                .padding(vertical = 18.dp),
            textAlign = TextAlign.Center,
            color = Color(0xFF8A94A8),
            fontSize = 15.sp
        )
    }

    if (showCustomDialog) {
        CustomGameDialog(
            initial = customConfig,
            onCancel = {
                showCustomDialog = false
                if (difficulty == Difficulty.CUSTOM) {
                    // keep current game
                }
            },
            onStart = { cfg ->
                customConfig = cfg
                showCustomDialog = false
                newGame(Difficulty.CUSTOM)
            }
        )
    }

    if (showLicenses) {
        LicensesDialog(onClose = { showLicenses = false })
    }
}

@Composable
private fun LedDisplay(text: String, color: Color) {
    Box(
        modifier = Modifier
            .width(150.dp)
            .height(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(LedBg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = color,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
    }
}

private fun numberColor(n: Int): Color = when (n) {
    1 -> Color(0xFF5B9BFF)
    2 -> Color(0xFF3DD68C)
    3 -> Color(0xFFFF6B5E)
    4 -> Color(0xFFB48CFF)
    5 -> Color(0xFFFFB340)
    6 -> Color(0xFF4FD8E0)
    7 -> Color(0xFFFF7AB8)
    else -> Color(0xFFE5E9F0)
}

@Composable
private fun CellView(game: GameState, r: Int, c: Int, modifier: Modifier) {
    val cell = game.cells[r][c]
    val ended = game.status == GameStatus.WON || game.status == GameStatus.LOST
    val lost = game.status == GameStatus.LOST
    val showMine = lost && cell.mine

    val bg = when {
        cell.exploded -> ExplodedBg
        showMine -> MineCellBg
        cell.revealed -> if (game.config.fogOfWar) FogColor else RevealedColor
        else -> CellColor
    }

    Box(
        modifier = modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .then(
                if (!cell.revealed && !showMine) Modifier.border(1.dp, CellBorder, RoundedCornerShape(6.dp))
                else Modifier
            )
            .pointerInput(ended) {
                if (ended) return@pointerInput
                detectTapGestures(
                    onTap = { game.reveal(r, c) },
                    onLongPress = { game.toggleFlag(r, c) }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        when {
            cell.flagged -> Text("🚩", fontSize = 20.sp)
            showMine -> Text(if (cell.exploded) "💥" else "💣", fontSize = 18.sp)
            cell.revealed && game.config.fogOfWar ->
                Box(Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0x66FFFFFF)))
            cell.revealed && cell.adjacent > 0 -> Text(
                "${cell.adjacent}",
                color = numberColor(cell.adjacent),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CustomGameDialog(
    initial: GameConfig,
    onCancel: () -> Unit,
    onStart: (GameConfig) -> Unit
) {
    var gridSize by remember { mutableIntStateOf(initial.rows) }
    var mines by remember { mutableIntStateOf(initial.mines) }
    var fog by remember { mutableStateOf(initial.fogOfWar) }
    var safeFirst by remember { mutableStateOf(initial.safeFirstTap) }

    val cells = gridSize * gridSize
    val minMines = (cells / 10).coerceAtLeast(1)
    val maxMines = (cells * 85 / 100).coerceAtLeast(minMines + 1)
    if (mines > maxMines) mines = maxMines
    if (mines < minMines) mines = minMines
    val percent = floor(100.0 * mines / cells).toInt()

    Dialog(onDismissRequest = onCancel) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF1A2440))
                .padding(28.dp)
        ) {
            Text("Custom Game", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE5E9F0))
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Grid size", color = Color(0xFFAAB4C8), fontSize = 17.sp)
                Text("$gridSize × $gridSize", color = AccentTeal, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = gridSize.toFloat(),
                onValueChange = { gridSize = it.toInt() },
                valueRange = 7f..20f,
                steps = 12
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Mines", color = Color(0xFFAAB4C8), fontSize = 17.sp)
                Text("$mines ($percent%)", color = AccentTeal, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = mines.toFloat(),
                onValueChange = { mines = it.toInt().coerceIn(minMines, maxMines) },
                valueRange = minMines.toFloat()..maxMines.toFloat()
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Fog of war", color = Color(0xFFE5E9F0), fontSize = 17.sp, modifier = Modifier.weight(1f))
                Switch(
                    checked = fog,
                    onCheckedChange = { fog = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = AccentTeal)
                )
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Safe first tap", color = Color(0xFFE5E9F0), fontSize = 17.sp, modifier = Modifier.weight(1f))
                Switch(
                    checked = safeFirst,
                    onCheckedChange = { safeFirst = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = AccentTeal)
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onCancel) {
                    Text("Cancel", color = Color(0xFFAAB4C8), fontSize = 17.sp)
                }
                Spacer(Modifier.width(16.dp))
                TextButton(onClick = {
                    onStart(GameConfig(gridSize, gridSize, mines, null, fog, safeFirst))
                }) {
                    Text("Start", color = AccentTeal, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun LicensesDialog(onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF2A3242))
                .padding(28.dp)
        ) {
            Text("Licenses", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE5E9F0))
            Spacer(Modifier.height(16.dp))
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                Text(
                    "Minesweeper is free software under the GNU General Public License v3.0. " +
                        "The full text ships with the source.\n\n" +
                        "The libraries below are used under the Apache License 2.0. Their attribution " +
                        "and the full license text follow.\n\n" +
                        "  androidx.compose (ui, ui-graphics, foundation, animation)\n" +
                        "  androidx.compose.material3\n" +
                        "  androidx.activity:activity-compose\n" +
                        "  androidx.core:core-ktx\n" +
                        "      Copyright (c) The Android Open Source Project\n\n" +
                        "  Kotlin standard library\n" +
                        "      Copyright (c) JetBrains s.r.o.\n\n" +
                        "Apache License\nVersion 2.0, January 2004\nhttp://www.apache.org/licenses/\n\n" +
                        "TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION",
                    color = Color(0xFFD5DBE5),
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onClose) {
                    Text("Close", color = Color(0xFF8AB4FF), fontSize = 17.sp)
                }
            }
        }
    }
}
