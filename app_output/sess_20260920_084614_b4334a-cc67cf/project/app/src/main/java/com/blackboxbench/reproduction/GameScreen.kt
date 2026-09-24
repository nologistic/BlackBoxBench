package com.blackboxbench.reproduction

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

@Composable
fun GameScreen(
    puzzle: Puzzle,
    initialBoard: List<Int>,
    initialElapsed: Int,
    cycleMode: Boolean,
    longPressToCross: Boolean,
    hapticEnabled: Boolean,
    onPersist: (List<Int>, Int) -> Unit,
    onBack: () -> Unit,
    onNextLevel: () -> Unit,
    onHome: () -> Unit
) {
    var board by remember(puzzle.id) { mutableStateOf(initialBoard) }
    var elapsed by remember(puzzle.id) { mutableIntStateOf(initialElapsed) }
    val undo = remember(puzzle.id) { mutableStateListOf<List<Int>>() }
    var selectedMode by remember(puzzle.id) { mutableIntStateOf(1) }
    var completed by remember(puzzle.id) { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(puzzle.id, completed) {
        while (!completed) {
            delay(1000)
            elapsed += 1
            onPersist(board, elapsed)
        }
    }

    fun isSolved(values: List<Int>): Boolean =
        values.indices.all { index -> (values[index] == 1) == puzzle.solution[index] }

    fun applyCell(index: Int, forced: Int? = null) {
        val old = board
        val next = old.toMutableList()
        next[index] = forced ?: if (cycleMode) {
            when (old[index]) {
                0 -> 1
                1 -> 2
                else -> 0
            }
        } else {
            if (old[index] == selectedMode) 0 else selectedMode
        }
        if (next != old) {
            undo += old
            board = next
            if (hapticEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onPersist(next, elapsed)
            if (isSolved(next)) completed = true
        }
    }

    ScreenRoot {
        Column(Modifier.fillMaxSize()) {
            GameTop(
                title = puzzle.title,
                elapsed = elapsed,
                moves = board.count { it != 0 },
                canUndo = undo.isNotEmpty(),
                onBack = {
                    onPersist(board, elapsed)
                    onBack()
                },
                onUndo = {
                    if (undo.isNotEmpty()) {
                        board = undo.removeAt(undo.lastIndex)
                        onPersist(board, elapsed)
                    }
                },
                onHint = { },
                onRestart = {
                    if (board.any { it != 0 } && hapticEnabled) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    undo.clear()
                    board = List(puzzle.size * puzzle.size) { 0 }
                    elapsed = 0
                    onPersist(board, elapsed)
                }
            )
            Spacer(Modifier.weight(1f))
            PuzzleGrid(
                puzzle = puzzle,
                board = board,
                onTap = { applyCell(it) },
                onLongPress = {
                    if (longPressToCross) applyCell(it, if (board[it] == 2) 0 else 2)
                    else applyCell(it)
                }
            )
            Spacer(Modifier.weight(1f))
            if (!cycleMode) {
                ModeSelector(selectedMode = selectedMode, onMode = { selectedMode = it })
                Spacer(Modifier.height(8.dp))
            }
        }
        if (completed) {
            CompletionDialog(
                puzzle = puzzle,
                elapsed = elapsed,
                moves = board.count { it != 0 },
                onNext = {
                    onPersist(board, elapsed)
                    onNextLevel()
                },
                onSupport = {
                    onPersist(board, elapsed)
                    onHome()
                },
                onHome = {
                    onPersist(board, elapsed)
                    onHome()
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GameTop(
    title: String?,
    elapsed: Int,
    moves: Int,
    canUndo: Boolean,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onHint: () -> Unit,
    onRestart: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .combinedClickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) { TallText("‹", size = 55, weight = androidx.compose.ui.text.font.FontWeight.Light, spacing = 0f) }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                if (title != null) TallText(title, size = 23, align = TextAlign.Center)
            }
            Spacer(Modifier.width(44.dp))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 25.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TallText("◴  " + formatElapsed(elapsed), size = 15, color = AppMuted)
            TallText("♙  MOVES: $moves", size = 15, color = AppMuted)
        }
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 50.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Box(Modifier.weight(1f)) { SmallToolButton("↶", "UNDO", canUndo, onUndo) }
            Box(Modifier.weight(1f)) { SmallToolButton("♧", "HINT", true, onHint) }
            Box(Modifier.weight(1f)) { SmallToolButton("↻", "RESTART", true, onRestart) }
        }
    }
}

private fun gridCellSize(size: Int): Dp = when {
    size <= 5 -> 53.dp
    size <= 7 -> 38.dp
    size <= 8 -> 33.dp
    size <= 10 -> 26.dp
    else -> 21.dp
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PuzzleGrid(
    puzzle: Puzzle,
    board: List<Int>,
    onTap: (Int) -> Unit,
    onLongPress: (Int) -> Unit
) {
    val size = puzzle.size
    val cell = gridCellSize(size)
    val clueWidth = if (size <= 5) 64.dp else 72.dp
    val clueTextSize = when {
        size <= 5 -> 16
        size <= 8 -> 12
        else -> 9
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom) {
            Spacer(Modifier.width(clueWidth).height(74.dp))
            puzzle.columnClues.forEach { clue ->
                Box(
                    modifier = Modifier
                        .width(cell)
                        .height(74.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    TallText(
                        clue.joinToString("\n"),
                        size = clueTextSize,
                        color = AppMuted,
                        align = TextAlign.Center,
                        spacing = 0f,
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                }
            }
        }
        puzzle.rowClues.forEachIndexed { row, clue ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(clueWidth)
                        .height(cell),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    TallText(
                        clue.joinToString(" "),
                        size = clueTextSize,
                        align = TextAlign.End,
                        spacing = 0f,
                        modifier = Modifier.padding(end = 9.dp)
                    )
                }
                repeat(size) { col ->
                    val index = row * size + col
                    val state = board.getOrElse(index) { 0 }
                    Box(
                        modifier = Modifier
                            .size(cell)
                            .padding(1.dp)
                            .background(if (state == 1) AppWhite else AppPanel, RoundedCornerShape(3.dp))
                            .border(1.dp, AppBorder, RoundedCornerShape(3.dp))
                            .combinedClickable(
                                onClick = { onTap(index) },
                                onLongClick = { onLongPress(index) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state == 2) TallText("×", size = (clueTextSize + 16), color = AppRed, spacing = 0f, align = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeSelector(selectedMode: Int, onMode: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 54.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        ModeButton(
            modifier = Modifier.weight(1f),
            label = "FILL",
            symbol = "■",
            selected = selectedMode == 1,
            selectedColor = AppWhite,
            selectedText = Color.Black,
            onClick = { onMode(1) }
        )
        ModeButton(
            modifier = Modifier.weight(1f),
            label = "CROSS [X]",
            symbol = "×",
            selected = selectedMode == 2,
            selectedColor = AppRed,
            selectedText = AppWhite,
            onClick = { onMode(2) }
        )
    }
}

@Composable
private fun ModeButton(
    modifier: Modifier,
    label: String,
    symbol: String,
    selected: Boolean,
    selectedColor: Color,
    selectedText: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(58.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) AppWhite else AppBorder),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) selectedColor else AppPanel,
            contentColor = if (selected) selectedText else AppWhite
        )
    ) {
        TallText(symbol, size = 24, color = if (selected) selectedText else if (symbol == "×") AppRed else AppWhite, spacing = 0f)
        TallText(label, size = 15, color = if (selected) selectedText else AppWhite, modifier = Modifier.padding(start = 10.dp))
    }
}

@Composable
private fun CompletionDialog(
    puzzle: Puzzle,
    elapsed: Int,
    moves: Int,
    onNext: () -> Unit,
    onSupport: () -> Unit,
    onHome: () -> Unit
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Panel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 42.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TallText("LEVEL COMPLETED!", size = 27, align = TextAlign.Center)
                TallText(
                    "MOVES: $moves  •  TIME: " + formatElapsed(elapsed),
                    size = 14,
                    color = AppMuted,
                    align = TextAlign.Center
                )
                Spacer(Modifier.height(18.dp))
                SolutionPreview(puzzle)
                Spacer(Modifier.height(20.dp))
                MenuButton("NEXT LEVEL", onNext, primary = true, height = 58.dp)
                Spacer(Modifier.height(10.dp))
                MenuButton("BUY ME A COFFEE", onSupport, symbol = "▱", height = 55.dp)
                Spacer(Modifier.height(10.dp))
                MenuButton("HOME", onHome, symbol = "⌂", height = 55.dp)
            }
        }
    }
}

@Composable
private fun SolutionPreview(puzzle: Puzzle) {
    val size = puzzle.size
    val cell = when {
        size <= 5 -> 25.dp
        size <= 8 -> 17.dp
        else -> 13.dp
    }
    Box(
        modifier = Modifier
            .background(AppPanel, RoundedCornerShape(16.dp))
            .border(1.dp, AppBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column {
            repeat(size) { row ->
                Row {
                    repeat(size) { col ->
                        Box(
                            modifier = Modifier
                                .size(cell)
                                .padding(1.dp)
                                .background(if (puzzle.solution[row * size + col]) AppWhite else AppPanel, RoundedCornerShape(2.dp))
                        )
                    }
                }
            }
        }
    }
}
