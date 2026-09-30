package com.blackboxbench.reproduction

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Top row choices; separate from the engine [Preset] type to avoid name clashes. */
private enum class Tab(val title: String, val subtitle: String) {
    EASY("Easy", "10 💣"),
    MEDIUM("Medium", "⏱️"),
    HARD("Hard", "⏱️👁️⚡"),
    CUSTOM("Custom", "⚙️")
}

private fun Tab.difficulty(): Difficulty = when (this) {
    Tab.EASY -> Difficulty.EASY
    Tab.MEDIUM -> Difficulty.MEDIUM
    Tab.HARD -> Difficulty.HARD
    Tab.CUSTOM -> Difficulty.CUSTOM
}

@Composable
fun MinesweeperScreen() {
    var tab by remember { mutableStateOf(Tab.EASY) }
    var preset by remember { mutableStateOf(EASY_PRESET) }
    var fog by remember { mutableStateOf(false) }
    var safeFirstTap by remember { mutableStateOf(true) }
    var state by remember { mutableStateOf(newGame(EASY_PRESET, Difficulty.EASY)) }
    var showCustom by remember { mutableStateOf(false) }
    var showLicenses by remember { mutableStateOf(false) }

    var customSize by remember { mutableIntStateOf(16) }
    var customMines by remember { mutableIntStateOf(40) }
    var customFog by remember { mutableStateOf(false) }
    var customSafe by remember { mutableStateOf(true) }

    fun startGame(target: Preset, difficulty: Difficulty, withFog: Boolean, withSafe: Boolean) {
        preset = target
        fog = withFog
        safeFirstTap = withSafe
        state = newGame(target, difficulty, fog = withFog, safeFirstTap = withSafe)
    }

    val latest = rememberUpdatedState(state)
    LaunchedEffect(state.running, state.status) {
        if (state.running && state.status == GameStatus.PLAYING) {
            while (true) {
                delay(1000)
                val current = latest.value
                if (!current.running || current.status != GameStatus.PLAYING) break
                state = tick(current)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(GameColors.background)) {
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SevenSegmentDisplay(
                text = formatCounter(state.counter),
                modifier = Modifier.width(68.dp).height(40.dp)
            )
            Spacer(Modifier.weight(1f))
            FaceButton(
                emoji = faceEmoji(state.status),
                size = 52.dp,
                modifier = Modifier
                    .clickable { startGame(preset, tab.difficulty(), fog, safeFirstTap) }
            )
            Spacer(Modifier.weight(1f))
            SevenSegmentDisplay(
                text = formatTimer(state),
                modifier = Modifier.width(68.dp).height(40.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Tab.entries.forEach { entry ->
                DifficultyTab(
                    tab = entry,
                    selected = tab == entry,
                    modifier = Modifier.weight(1f)
                ) {
                    if (entry == Tab.CUSTOM) {
                        showCustom = true
                    } else {
                        tab = entry
                        val target = when (entry) {
                            Tab.EASY -> EASY_PRESET
                            Tab.MEDIUM -> MEDIUM_PRESET
                            else -> HARD_PRESET
                        }
                        startGame(target, entry.difficulty(), withFog = false, withSafe = true)
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
            when (state.status) {
                GameStatus.WON -> StatusBanner("🎉 You Win! 🎉", GameColors.winBanner, GameColors.winText)
                GameStatus.LOST -> StatusBanner("💥 Game Over 💥", GameColors.loseBanner, GameColors.loseText)
                else -> Unit
            }
        }
        BoardArea(
            state = state,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp),
            onReveal = { x, y -> state = reveal(state, x, y) },
            onFlag = { x, y -> state = toggleFlag(state, x, y) }
        )
        Box(Modifier.fillMaxWidth().height(46.dp), contentAlignment = Alignment.Center) {
            Text(
                text = "Licenses",
                color = GameColors.licenseText,
                fontSize = 14.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { showLicenses = true }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }

    if (showCustom) {
        CustomGameDialog(
            size = customSize,
            mines = customMines,
            fog = customFog,
            safeFirstTap = customSafe,
            onSizeChange = { newSize ->
                customSize = newSize
                val cap = maxMinesFor(newSize)
                if (customMines > cap) customMines = cap
                if (customMines < 1) customMines = 1
            },
            onMinesChange = { customMines = it },
            onFogChange = { customFog = it },
            onSafeChange = { customSafe = it },
            onCancel = { showCustom = false },
            onStart = {
                tab = Tab.CUSTOM
                startGame(
                    Preset(
                        cols = customSize,
                        rows = customSize,
                        mines = customMines,
                        timeLimitSeconds = 0
                    ),
                    Difficulty.CUSTOM,
                    withFog = customFog,
                    withSafe = customSafe
                )
                showCustom = false
            }
        )
    }
    if (showLicenses) {
        LicensesDialog(onClose = { showLicenses = false })
    }
}

private fun faceEmoji(status: GameStatus): String = when (status) {
    GameStatus.WON -> "😎"
    GameStatus.LOST -> "😵"
    else -> "😊"
}

@Composable
private fun DifficultyTab(
    tab: Tab,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) GameColors.tabSelected else GameColors.tabIdle)
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = tab.title,
            color = if (selected) GameColors.tabTitleSelected else GameColors.tabTitleIdle,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(1.dp))
        Text(
            text = tab.subtitle,
            color = if (selected) GameColors.tabSubSelected else GameColors.tabSubIdle,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun StatusBanner(text: String, background: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun BoardArea(
    state: GameState,
    modifier: Modifier,
    onReveal: (Int, Int) -> Unit,
    onFlag: (Int, Int) -> Unit
) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val cellWidth = maxWidth / state.cols
        val cellHeight = maxHeight / state.rows
        val cellSize = minOf(cellWidth, cellHeight)
        Column {
            for (y in 0 until state.rows) {
                Row {
                    for (x in 0 until state.cols) {
                        GameCell(
                            cellSize = cellSize,
                            cell = state.cells[state.index(x, y)],
                            status = state.status,
                            fog = state.fog,
                            onReveal = { onReveal(x, y) },
                            onFlag = { onFlag(x, y) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GameCell(
    cellSize: Dp,
    cell: Cell,
    status: GameStatus,
    fog: Boolean,
    onReveal: () -> Unit,
    onFlag: () -> Unit
) {
    val lost = status == GameStatus.LOST
    val showMine = lost && cell.mine
    val density = LocalDensity.current
    val fontSize = with(density) { (cellSize.toPx() * 0.56f).toSp() }

    val background = when {
        cell.exploded && lost -> GameColors.cellExploded
        showMine -> GameColors.cellMine
        cell.revealed -> if (fog && cell.adjacent == 0) GameColors.cellFog else GameColors.cellRevealed
        else -> null
    }

    val base = Modifier
        .size(cellSize)
        .padding(cellSize * 0.035f)
        .clip(RoundedCornerShape(cellSize * 0.10f))
        .then(
            if (background != null) {
                Modifier.background(background)
            } else {
                Modifier.background(Brush.verticalGradient(listOf(GameColors.cellHiddenEdge, GameColors.cellHidden)))
            }
        )

    Box(
        modifier = if (showMine) base else base.combinedClickable(onClick = onReveal, onLongClick = onFlag),
        contentAlignment = Alignment.Center
    ) {
        when {
            showMine -> Text("💣", fontSize = fontSize)
            cell.flagged -> FlagIcon(cellSize * 0.8f)
            cell.revealed && cell.adjacent > 0 -> Text(
                text = cell.adjacent.toString(),
                color = GameColors.numberColors[(cell.adjacent - 1).coerceIn(0, 7)],
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            else -> Unit
        }
    }
}
