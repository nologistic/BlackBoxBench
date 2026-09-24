package com.blackboxbench.reproduction

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val BG = Color(0xFF131C2B)
private val TILE_CLOSED = Color(0xFF1F3040)
private val TILE_OPEN = Color(0xFF0D1622)
private val COUNTER_RED = Color(0xFFA4392F)
private val TIMER_AMBER = Color(0xFFAA6A12)
private val PILL_SELECTED = Color(0xFF33507A)
private val PILL_TEXT = Color(0xFFB9C6D6)

private val NUMBER_COLORS = listOf(
    Color(0xFF64A5F4), Color(0xFF6FCF7C), Color(0xFFE25B5B), Color(0xFFA78BFA),
    Color(0xFFD98A4A), Color(0xFF55C5C5), Color(0xFFD9E2EC), Color(0xFF9AA6B2))

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ReproducedApp() }
    }
}

@Composable
fun ReproducedApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("minesweeper", Context.MODE_PRIVATE) }
    val game = remember { GameModel().also { m -> if (!m.restore(prefs)) m.newGame("EASY") } }
    var showLicenses by remember { mutableStateOf(false) }
    var showCustom by remember { mutableStateOf(false) }
    var showEndDialog by remember { mutableStateOf(false) }
    var hasCustom by remember { mutableStateOf(prefs.getBoolean("hasCustom", false)) }

    fun persist() { game.persist(prefs) }

    // One resident ticker; no per-tick effect restarts.
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            if (game.status == Status.PLAYING && game.started) {
                game.elapsedSeconds += 1
                game.persist(prefs)
            }
        }
    }

    BackHandler(enabled = showLicenses) { showLicenses = false }

    if (showLicenses) { LicensesScreen { showLicenses = false }; return }

    Column(Modifier.fillMaxSize().background(BG)) {
        HudRow(
            counter = game.counter,
            elapsed = game.elapsedSeconds,
            status = game.status,
            onFace = { game.newGame(game.difficultyKey); persist() })
        Spacer(Modifier.height(18.dp))
        DifficultyRow(
            selected = game.difficultyKey,
            customMines = if (hasCustom) game.custom.mines else null,
            onSelect = { key ->
                if (key == "CUSTOM") showCustom = true
                else { game.newGame(key); persist() }
            })
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Board(game, onTap = { i ->
                if (game.tap(i)) { if (game.status != Status.PLAYING) showEndDialog = true; persist() }
            }, onLong = { i -> if (game.longPress(i)) persist() })
        }
        TextButton(
            onClick = { showLicenses = true },
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 14.dp)) {
            Text("Licenses", color = Color(0xFF7A8CA3))
        }
    }

    if (showCustom) {
        CustomGameDialog(
            initial = game.custom,
            onStart = { cfg ->
                game.newGame("CUSTOM", cfg)
                hasCustom = true
                prefs.edit().putBoolean("hasCustom", true).apply()
                showCustom = false
                persist()
            },
            onCancel = { showCustom = false })
    }

    if (showEndDialog && game.status != Status.PLAYING) {
        AlertDialog(
            onDismissRequest = { showEndDialog = false },
            containerColor = Color(0xFF1B2635),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFC7D2DE),
            title = { Text(if (game.status == Status.WON) "You Win!" else "You Lose!") },
            text = {
                Text(if (game.status == Status.WON)
                    "Congratulations, you found all the mines in ${game.elapsedSeconds} seconds"
                else "Oops, you hit a mine! Better luck next time.")
            },
            confirmButton = {
                Button(colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF33507A)),
                    onClick = { showEndDialog = false; game.newGame(game.difficultyKey); persist() }) {
                    Text("Play Again") }
            })
    }
}

@Composable
private fun HudRow(counter: Int, elapsed: Int, status: Status, onFace: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (counter < 0) String.format("-%03d", -counter) else String.format("%03d", counter),
            color = COUNTER_RED, fontSize = 26.sp, fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f))
        Box(
            Modifier.size(46.dp)
                .background(Color(0xFF22344A), RoundedCornerShape(23.dp))
                .tapOnly(onFace),
            contentAlignment = Alignment.Center) {
            Text(
                when (status) { Status.WON -> "😎"; Status.LOST -> "💀"; else -> "🙂" },
                fontSize = 26.sp)
        }
        Text(
            String.format("%02d:%02d", elapsed / 60, elapsed % 60),
            color = TIMER_AMBER, fontSize = 26.sp, fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace, textAlign = TextAlign.End,
            modifier = Modifier.weight(1f))
    }
}

@Composable
private fun DifficultyRow(selected: String, customMines: Int?, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        val defs = listOf(
            "EASY" to "Easy (010 💣)",
            "MEDIUM" to "Medium (040 💣)",
            "HARD" to "Hard (099 💣)",
            "CUSTOM" to (if (customMines != null) String.format("Custom (%03d 💣)", customMines) else "Custom"))
        for ((key, label) in defs) {
            val isSel = key == selected
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.fillMaxWidth().height(38.dp)
                        .background(if (isSel) PILL_SELECTED else Color.Transparent,
                            RoundedCornerShape(19.dp))
                        .tapOnly { onSelect(key) },
                    contentAlignment = Alignment.Center) {
                    Text(label, color = if (isSel) Color.White else PILL_TEXT, fontSize = 13.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1)
                }
                Spacer(Modifier.height(3.dp))
                Box(Modifier.width(52.dp).height(3.dp)
                    .background(if (isSel) Color(0xFF7FA6D9) else Color.Transparent))
            }
        }
    }
}

private fun Modifier.tapOnly(onClick: () -> Unit): Modifier =
    this.pointerInput(Unit) {
        detectTapGestures { onClick() }
    }

private fun Modifier.cellGesture(index: Int, onTap: (Int) -> Unit, onLong: (Int) -> Unit): Modifier =
    this.pointerInput(index) {
        detectTapGestures(
            onTap = { onTap(index) },
            onLongPress = { onLong(index) })
    }

@Composable
private fun Board(game: GameModel, onTap: (Int) -> Unit, onLong: (Int) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val maxWpx = with(density) { maxWidth.toPx() }
        val maxHpx = with(density) { maxHeight.toPx() }
        val tilePx = if (game.isCustom) {
            minOf(maxWpx / game.cols, 200f)
        } else {
            minOf(maxWpx, maxHpx) / maxOf(game.cols, game.rows)
        }
        val gapPx = if (game.cols > 16) 1.5f else 4f
        val tileDp = with(density) { tilePx.toDp() }
        val gapDp = with(density) { gapPx.toDp() }
        val contentH = game.rows * tilePx + (game.rows - 1) * gapPx
        val overflows = contentH > maxHpx
        val boardModifier = if (overflows)
            Modifier.align(Alignment.Center).verticalScroll(rememberScrollState())
        else
            Modifier.align(Alignment.Center)
        Column(
            boardModifier,
            verticalArrangement = Arrangement.spacedBy(gapDp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            for (r in 0 until game.rows) {
                Row(horizontalArrangement = Arrangement.spacedBy(gapDp)) {
                    for (c in 0 until game.cols) {
                        val i = r * game.cols + c
                        Cell(game, i, tileDp, onTap, onLong)
                    }
                }
            }
        }
    }
}

@Composable
private fun Cell(game: GameModel, i: Int, tile: androidx.compose.ui.unit.Dp,
                 onTap: (Int) -> Unit, onLong: (Int) -> Unit) {
    val isRevealed = i in game.revealed
    val isFlag = i in game.flags
    val isExploded = i == game.exploded
    val adj = if (isRevealed) game.adjacentMines(i) else 0
    Box(
        Modifier.size(tile)
            .background(
                when {
                    isExploded -> Color(0xFF8B2F23)
                    isRevealed -> TILE_OPEN
                    else -> TILE_CLOSED
                }, RoundedCornerShape(5.dp))
            .cellGesture(i, onTap, onLong),
        contentAlignment = Alignment.Center) {
        when {
            isExploded -> Text("💥", fontSize = (tile.value * 0.45f).sp)
            isFlag -> Text("🚩", fontSize = (tile.value * 0.4f).sp)
            isRevealed && adj > 0 -> Text(
                "$adj", color = NUMBER_COLORS[adj - 1],
                fontSize = (tile.value * 0.45f).sp, fontWeight = FontWeight.Bold)
        }
    }
}

private val LICENSES = listOf(
    "The Android Open Source Project",
    "The Android Open Source Project",
    "The Android Open Source Project",
    "Kotlin",
    "org.jetbrains.kotlin:kotlin-stdlib",
    "io.github.aakira:napier",
    "com.squareup.okio:okio",
    "com.jakewharton.timber:timber")

@Composable
private fun LicensesScreen(onBack: () -> Unit) {
    @Suppress("UNUSED_PARAMETER") val unused = onBack
    Column(Modifier.fillMaxSize().background(BG).padding(18.dp)) {
        Text("Licenses", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 12.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            for (name in LICENSES) {
                Text(name, color = PILL_TEXT, fontSize = 15.sp,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp))
            }
            Text("...and 9 more", color = Color(0xFF7A8CA3), fontSize = 13.sp,
                modifier = Modifier.padding(top = 6.dp))
        }
    }
}
