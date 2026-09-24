package com.blackboxbench.reproduction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlin.math.floor
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(0xFF07131F.toInt()),
            navigationBarStyle = SystemBarStyle.dark(0xFF07131F.toInt())
        )
        setContent { BenchmarkAppTheme { MinesweeperApp() } }
    }
}

private val Navy = Color(0xFF07131F)
private val Panel = Color(0xFF1B2D3E)
private val PanelBorder = Color(0xFF364A5D)
private val Aqua = Color(0xFF55C8C4)
private val Muted = Color(0xFFA8B5C7)
private val Red = Color(0xFFFF4242)
private val Orange = Color(0xFFFF9418)
private val Purple = Color(0xFF5A418C)
private val LoseRed = Color(0xFF4A0909)
private val WinGreen = Color(0xFF0D4B29)

private enum class Mode { EASY, MEDIUM, HARD, CUSTOM }
private enum class Phase { READY, PLAYING, WON, LOST }

private data class Game(
    val nonce: Long,
    val mode: Mode,
    val size: Int,
    val mineCount: Int,
    val timeLimit: Int?,
    val fog: Boolean,
    val safeFirst: Boolean,
    val mines: Set<Int> = emptySet(),
    val generated: Boolean = false,
    val revealed: Set<Int> = emptySet(),
    val flagged: Set<Int> = emptySet(),
    val started: Boolean = false,
    val elapsed: Int = 0,
    val phase: Phase = Phase.READY,
    val exploded: Int? = null
)

private fun minesFor(size: Int, count: Int, excluded: Set<Int> = emptySet()): Set<Int> {
    val candidates = (0 until size * size).filterNot { it in excluded }.shuffled(Random(System.nanoTime()))
    return candidates.take(count.coerceAtMost(candidates.size)).toSet()
}

private fun freshGame(
    mode: Mode,
    customSize: Int = 16,
    customMines: Int = 40,
    customFog: Boolean = false,
    customSafe: Boolean = true
): Game {
    val size: Int
    val count: Int
    val limit: Int?
    val fog: Boolean
    val safe: Boolean
    when (mode) {
        Mode.EASY -> { size = 9; count = 10; limit = null; fog = false; safe = true }
        Mode.MEDIUM -> { size = 12; count = 30; limit = 300; fog = false; safe = true }
        Mode.HARD -> { size = 14; count = 50; limit = 180; fog = true; safe = true }
        Mode.CUSTOM -> {
            size = customSize; count = customMines; limit = null
            fog = customFog; safe = customSafe
        }
    }
    val initial = if (safe) emptySet() else minesFor(size, count)
    return Game(
        nonce = System.nanoTime(), mode = mode, size = size, mineCount = count,
        timeLimit = limit, fog = fog, safeFirst = safe,
        mines = initial, generated = !safe
    )
}

private fun neighbors(index: Int, size: Int): List<Int> {
    val row = index / size
    val col = index % size
    return buildList {
        for (dr in -1..1) for (dc in -1..1) {
            if (dr == 0 && dc == 0) continue
            val r = row + dr
            val c = col + dc
            if (r in 0 until size && c in 0 until size) add(r * size + c)
        }
    }
}

private fun adjacentMines(index: Int, game: Game): Int =
    neighbors(index, game.size).count { it in game.mines }

private fun floodReveal(start: Collection<Int>, game: Game): Set<Int> {
    val revealed = game.revealed.toMutableSet()
    val queue = ArrayDeque<Int>()
    start.filterNot { it in game.flagged || it in game.mines }.forEach(queue::addLast)
    while (queue.isNotEmpty()) {
        val index = queue.removeFirst()
        if (!revealed.add(index)) continue
        if (adjacentMines(index, game) == 0) {
            neighbors(index, game.size)
                .filterNot { it in revealed || it in game.flagged || it in game.mines }
                .forEach(queue::addLast)
        }
    }
    return revealed
}

private fun reveal(index: Int, original: Game): Game {
    if (original.phase == Phase.WON || original.phase == Phase.LOST || index in original.flagged) return original
    var game = original
    if (!game.generated) {
        val exclusion = if (game.safeFirst) setOf(index) else emptySet()
        game = game.copy(mines = minesFor(game.size, game.mineCount, exclusion), generated = true)
    }
    if (index in game.revealed) {
        val number = adjacentMines(index, game)
        val around = neighbors(index, game.size)
        if (number == 0 || around.count { it in game.flagged } != number) return game
        val candidates = around.filterNot { it in game.flagged || it in game.revealed }
        val struck = candidates.firstOrNull { it in game.mines }
        if (struck != null) {
            return game.copy(started = true, phase = Phase.LOST, exploded = struck)
        }
        val next = floodReveal(candidates, game)
        return finishIfWon(game.copy(revealed = next, started = true, phase = Phase.PLAYING))
    }
    if (index in game.mines) {
        return game.copy(started = true, phase = Phase.LOST, exploded = index)
    }
    val next = floodReveal(listOf(index), game)
    return finishIfWon(game.copy(revealed = next, started = true, phase = Phase.PLAYING))
}

private fun finishIfWon(game: Game): Game {
    if (game.revealed.size >= game.size * game.size - game.mineCount) {
        return game.copy(phase = Phase.WON, flagged = game.mines)
    }
    return game
}

private fun toggleFlag(index: Int, game: Game): Game {
    if (game.phase == Phase.WON || game.phase == Phase.LOST || index in game.revealed) return game
    val next = game.flagged.toMutableSet()
    if (!next.add(index)) next.remove(index)
    return game.copy(flagged = next, started = true, phase = Phase.PLAYING)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MinesweeperApp() {
    var customSize by remember { mutableIntStateOf(16) }
    var customMines by remember { mutableIntStateOf(40) }
    var customFog by remember { mutableStateOf(false) }
    var customSafe by remember { mutableStateOf(true) }
    var game by remember { mutableStateOf(freshGame(Mode.EASY)) }
    var showCustom by remember { mutableStateOf(false) }
    var showLicenses by remember { mutableStateOf(false) }
    var fogHidden by remember { mutableStateOf(false) }

    LaunchedEffect(game.nonce, game.started, game.phase) {
        if (!game.started || game.phase != Phase.PLAYING) return@LaunchedEffect
        while (game.phase == Phase.PLAYING) {
            delay(1000)
            if (game.phase != Phase.PLAYING) break
            val nextElapsed = game.elapsed + 1
            val limit = game.timeLimit
            if (limit != null && nextElapsed >= limit) {
                game = game.copy(elapsed = limit, phase = Phase.LOST, exploded = null)
            } else {
                game = game.copy(elapsed = nextElapsed)
            }
        }
    }

    LaunchedEffect(game.nonce, game.revealed.size, game.phase) {
        fogHidden = false
        if (game.fog && game.revealed.isNotEmpty() && game.phase == Phase.PLAYING) {
            delay(6500)
            fogHidden = true
        }
    }

    fun select(mode: Mode) {
        if (mode == Mode.CUSTOM) showCustom = true
        else game = freshGame(mode)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy)
            .padding(top = 56.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StatusRow(game)
        Spacer(Modifier.height(10.dp))
        DifficultyRow(game.mode, ::select)
        Spacer(Modifier.height(6.dp))
        OutcomeBanner(game.phase)
        Spacer(Modifier.height(105.dp))
        Board(
            game = game,
            fogHidden = fogHidden,
            onTap = { game = reveal(it, game) },
            onLongPress = { game = toggleFlag(it, game) }
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "Licenses",
            color = Color(0xFF717785),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .combinedClickable(onClick = { showLicenses = true }, onLongClick = {})
                .padding(horizontal = 24.dp, vertical = 12.dp)
        )
    }

    if (showCustom) {
        CustomDialog(
            size = customSize,
            mines = customMines,
            fog = customFog,
            safe = customSafe,
            onSize = {
                customSize = it
                customMines = customMines.coerceAtMost(maxMines(it))
            },
            onMines = { customMines = it },
            onFog = { customFog = it },
            onSafe = { customSafe = it },
            onCancel = { showCustom = false },
            onStart = {
                game = freshGame(Mode.CUSTOM, customSize, customMines, customFog, customSafe)
                showCustom = false
            }
        )
    }
    if (showLicenses) LicensesDialog { showLicenses = false }
}

@Composable
private fun StatusRow(game: Game) {
    val remaining = game.mineCount - game.flagged.size
    val shownSeconds = game.timeLimit?.let { (it - game.elapsed).coerceAtLeast(0) } ?: game.elapsed
    val mins = shownSeconds / 60
    val secs = shownSeconds % 60
    val timerColor = if (game.timeLimit != null) Orange else Red
    val face = when (game.phase) {
        Phase.READY -> "😊"
        Phase.PLAYING -> "🙂"
        Phase.WON -> "😎"
        Phase.LOST -> "😵"
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DigitalBox(remaining.toString(), Red, 75.dp)
        Box(
            modifier = Modifier.size(52.dp).background(Panel, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) { Text(face, fontSize = 29.sp) }
        DigitalBox(String.format("%02d:%02d", mins, secs), timerColor, 106.dp)
    }
}

@Composable
private fun DigitalBox(value: String, color: Color, width: Dp) {
    Box(
        modifier = Modifier
            .width(width)
            .height(40.dp)
            .background(Color(0xA80A0D14), RoundedCornerShape(7.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            value, color = color, fontSize = 27.sp, fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace, letterSpacing = 1.sp
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DifficultyRow(selected: Mode, onSelect: (Mode) -> Unit) {
    val items = listOf(
        Triple(Mode.EASY, "Easy", "10💣"),
        Triple(Mode.MEDIUM, "Medium", "⏱"),
        Triple(Mode.HARD, "Hard", "⏱👁⚡"),
        Triple(Mode.CUSTOM, "Custom", "⚙")
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items.forEach { (mode, title, sub) ->
            val active = mode == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .background(if (active) Aqua else Panel, RoundedCornerShape(10.dp))
                    .combinedClickable(onClick = { onSelect(mode) }, onLongClick = {}),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    title,
                    color = if (active) Color(0xFF07131F) else Muted,
                    fontSize = 16.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                )
                Spacer(Modifier.height(6.dp))
                Text(sub, color = if (active) Color(0xFF415760) else Color(0xFF8290A2), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun OutcomeBanner(phase: Phase) {
    val visible = phase == Phase.WON || phase == Phase.LOST
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .padding(horizontal = 16.dp)
            .background(
                if (!visible) Color.Transparent else if (phase == Phase.WON) WinGreen else Color(0xFF5A0908),
                RoundedCornerShape(9.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (visible) {
            Text(
                if (phase == Phase.WON) "🎉  You Win!  🎉" else "💥  Game Over  💥",
                color = Color(0xFFE7E7EC), fontSize = 18.sp, fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Board(
    game: Game,
    fogHidden: Boolean,
    onTap: (Int) -> Unit,
    onLongPress: (Int) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp).aspectRatio(1f)
    ) {
        val cell = maxWidth / game.size
        val font = (maxWidth.value / game.size * 0.43f).coerceIn(8f, 34f)
        Column {
            repeat(game.size) { row ->
                Row {
                    repeat(game.size) { col ->
                        val index = row * game.size + col
                        Cell(
                            modifier = Modifier.size(cell).padding(1.dp),
                            index = index,
                            game = game,
                            fogHidden = fogHidden,
                            fontSize = font,
                            onTap = onTap,
                            onLongPress = onLongPress
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Cell(
    modifier: Modifier,
    index: Int,
    game: Game,
    fogHidden: Boolean,
    fontSize: Float,
    onTap: (Int) -> Unit,
    onLongPress: (Int) -> Unit
) {
    val mine = index in game.mines
    val flagged = index in game.flagged
    val revealed = index in game.revealed
    val ended = game.phase == Phase.WON || game.phase == Phase.LOST
    val showMine = game.phase == Phase.LOST && mine
    val fogged = game.fog && fogHidden && revealed && game.phase == Phase.PLAYING

    val bg = when {
        game.exploded == index -> Color(0xFFFF2E08)
        showMine -> LoseRed
        fogged -> Purple
        revealed -> Navy
        else -> Panel
    }
    val border = when {
        game.exploded == index -> Color(0xFFFF5A20)
        revealed && !fogged -> Color(0xFF142535)
        else -> PanelBorder
    }
    val text: String
    val textColor: Color
    when {
        game.exploded == index -> { text = "✹"; textColor = Color(0xFFFFD01A) }
        showMine -> { text = "💣"; textColor = Color.White }
        flagged && !showMine -> { text = "⚑"; textColor = Color(0xFFFF603E) }
        revealed && !fogged -> {
            val number = adjacentMines(index, game)
            text = if (number == 0) "" else number.toString()
            textColor = when (number) {
                1 -> Color(0xFF69A9FF)
                2 -> Color(0xFF55D968)
                3 -> Color(0xFFFF5A67)
                4 -> Color(0xFFB889FF)
                else -> Color(0xFFFFA048)
            }
        }
        else -> { text = ""; textColor = Color.Transparent }
    }

    Box(
        modifier = modifier
            .background(bg, RoundedCornerShape(2.dp))
            .border(0.7.dp, border, RoundedCornerShape(2.dp))
            .combinedClickable(
                enabled = !ended,
                onClick = { onTap(index) },
                onLongClick = { onLongPress(index) }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (text.isNotEmpty()) {
            Text(
                text = text,
                color = textColor,
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun maxMines(size: Int): Int = floor(size * size * 0.64).toInt().coerceAtLeast(1)

@Composable
private fun CustomDialog(
    size: Int,
    mines: Int,
    fog: Boolean,
    safe: Boolean,
    onSize: (Int) -> Unit,
    onMines: (Int) -> Unit,
    onFog: (Boolean) -> Unit,
    onSafe: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onStart: () -> Unit
) {
    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .width(336.dp)
                .background(Color(0xFF172236), RoundedCornerShape(27.dp))
                .padding(horizontal = 25.dp, vertical = 25.dp)
        ) {
            Text("Custom Game", color = Color(0xFFE9EDF5), fontSize = 25.sp)
            Spacer(Modifier.height(22.dp))
            SettingHeader("Grid size", "$size × $size")
            Slider(
                value = size.toFloat(), onValueChange = { onSize(it.toInt()) },
                valueRange = 5f..20f, steps = 14,
                colors = sliderColors()
            )
            Spacer(Modifier.height(7.dp))
            SettingHeader("Mines", "$mines  (" + (mines * 100 / (size * size)) + "%)")
            Slider(
                value = mines.toFloat(), onValueChange = { onMines(it.toInt().coerceIn(1, maxMines(size))) },
                valueRange = 1f..maxMines(size).toFloat(),
                steps = (maxMines(size) - 2).coerceAtLeast(0),
                colors = sliderColors()
            )
            ToggleRow("Fog of war", fog, onFog)
            ToggleRow("Safe first tap", safe, onSafe)
            Spacer(Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onCancel) {
                    Text("Cancel", color = Muted, fontSize = 16.sp)
                }
                Spacer(Modifier.width(10.dp))
                TextButton(onClick = onStart) {
                    Text("Start", color = Aqua, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun sliderColors() = SliderDefaults.colors(
    thumbColor = Aqua,
    activeTrackColor = Aqua,
    inactiveTrackColor = Color(0xFF203149),
    activeTickColor = Color(0xFF55607E),
    inactiveTickColor = Color(0xFFAFC1FF)
)

@Composable
private fun SettingHeader(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFFDCE2EC), fontSize = 16.sp)
        Text(value, color = Aqua, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(50.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xFFDCE2EC), fontSize = 16.sp)
        Switch(
            checked = checked, onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Navy, checkedTrackColor = Aqua,
                uncheckedThumbColor = Color(0xFF97B4D9),
                uncheckedTrackColor = Color(0xFF26364A),
                uncheckedBorderColor = Color(0xFF9AA6B8)
            )
        )
    }
}

@Composable
private fun LicensesDialog(onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .width(336.dp)
                .height(610.dp)
                .background(Color(0xFF2A2B30), RoundedCornerShape(27.dp))
                .padding(horizontal = 25.dp, vertical = 25.dp)
        ) {
            Text("Licenses", color = Color(0xFFF0F0F5), fontSize = 26.sp)
            Spacer(Modifier.height(19.dp))
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
            ) {
                Text(
                    """Minesweeper is free software under the GNU
General Public License v3.0.

The libraries below are used under the
Apache License 2.0. Their attribution
and the full license text follow.

  androidx.compose (ui, ui-graphics,
  foundation, animation)
  androidx.compose.material3
  androidx.activity:activity-compose
  androidx.core:core-ktx
      Copyright (c) The Android Open
      Source Project

  Kotlin standard library
      Copyright (c) JetBrains s.r.o.


                 Apache License
                   Version 2.0,
                  January 2004

http://www.apache.org/licenses/

     TERMS AND CONDITIONS FOR USE,
     REPRODUCTION, AND DISTRIBUTION

1. Definitions.

"License" shall mean the terms and
conditions for use, reproduction, and
distribution as defined by Sections 1
through 9 of this document.

"Licensor" shall mean the copyright owner
or entity authorized by the copyright
owner that is granting the License.

"Legal Entity" shall mean the union of the
acting entity and all other entities that
control, are controlled by, or are under
common control with that entity.

2. Grant of Copyright License.

Subject to the terms and conditions of
this License, each Contributor hereby
grants You a perpetual, worldwide,
non-exclusive, no-charge, royalty-free,
irrevocable copyright license to
reproduce and prepare Derivative Works.

END OF TERMS AND CONDITIONS""",
                    color = Color(0xFFE1E1E7),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                    onClick = onClose,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFC7C9FF))
                ) { Text("Close", fontSize = 16.sp) }
            }
        }
    }
}
