package com.blackboxbench.reproduction

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private data class UndoEntry(val index: Int, val previous: CellState, val previousMoves: Int)

private enum class EditMode { FILL, CROSS }

@Composable
fun GameScreen(
    puzzle: Puzzle,
    title: String,
    boardKey: String,
    appState: AppState,
    nextLevel: Int?,
    onBack: () -> Unit,
    onWin: (Int, Long) -> Unit,
    onOpenLevel: (Int) -> Unit,
    onHome: () -> Unit
) {
    val cellCount = puzzle.rows * puzzle.cols
    val saved = remember(boardKey) { appState.board(boardKey) }

    val cells = remember(boardKey) {
        mutableStateListOf<CellState>().apply {
            repeat(cellCount) { index -> add(saved?.cells?.getOrNull(index) ?: CellState.EMPTY) }
        }
    }
    var moves by remember(boardKey) { mutableIntStateOf(saved?.moves ?: 0) }
    var elapsed by remember(boardKey) { mutableLongStateOf(saved?.elapsedMs ?: 0L) }
    var finished by remember(boardKey) { mutableStateOf(false) }
    var editMode by remember(boardKey) { mutableStateOf(EditMode.FILL) }
    val undoStack = remember(boardKey) { mutableStateListOf<UndoEntry>() }
    val haptics = LocalHapticFeedback.current

    // Running clock; also keeps the board snapshot warm so progress survives a restart.
    LaunchedEffect(boardKey, finished) {
        while (true) {
            delay(1000)
            if (finished) continue
            elapsed += 1000
            appState.saveBoard(boardKey, SavedGame(cells.toList(), moves, elapsed))
        }
    }

    fun persist() {
        appState.saveBoard(boardKey, SavedGame(cells.toList(), moves, elapsed))
    }

    fun checkWin() {
        val current = cells.indices.filter { cells[it] == CellState.FILLED }.toSet()
        if (current == puzzle.filled) {
            finished = true
            appState.clearBoard(boardKey)
            onWin(moves, elapsed)
        }
    }

    fun applyChange(index: Int, target: CellState) {
        if (finished) return
        val previous = cells[index]
        if (previous == target) return
        undoStack.add(UndoEntry(index, previous, moves))
        cells[index] = target
        if (target == CellState.FILLED) moves += 1
        if (appState.hapticFeedback) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        persist()
        checkWin()
    }

    fun handleTap(index: Int) {
        val current = cells[index]
        val target = if (appState.cycleMode) {
            when (current) {
                CellState.EMPTY -> CellState.FILLED
                CellState.FILLED -> CellState.CROSS
                CellState.CROSS -> CellState.EMPTY
            }
        } else {
            when (editMode) {
                EditMode.FILL -> when (current) {
                    CellState.EMPTY -> CellState.FILLED
                    CellState.FILLED -> CellState.EMPTY
                    CellState.CROSS -> CellState.FILLED
                }
                EditMode.CROSS -> when (current) {
                    CellState.EMPTY -> CellState.CROSS
                    CellState.CROSS -> CellState.EMPTY
                    CellState.FILLED -> CellState.CROSS
                }
            }
        }
        applyChange(index, target)
    }

    fun handleLongPress(index: Int) {
        if (!appState.longPressToCross) {
            handleTap(index)
            return
        }
        applyChange(index, CellState.CROSS)
    }

    BackHandler { onBack() }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = title, onBack = onBack, bottomPadding = 2.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Metrics.screenPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconClock(size = 18.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = formatDuration(elapsed),
                color = Palette.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.weight(1f))
            IconMoves(size = 18.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "MOVES: $moves",
                color = Palette.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 46.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolButton(
                text = "UNDO",
                onClick = {
                    val entry = undoStack.removeLastOrNull() ?: return@ToolButton
                    cells[entry.index] = entry.previous
                    moves = entry.previousMoves
                    persist()
                },
                enabled = undoStack.isNotEmpty() && !finished,
                leading = { IconUndo(size = 17.dp, color = if (undoStack.isNotEmpty() && !finished) Palette.TextPrimary else Palette.TextMuted) }
            )
            ToolButton(
                text = "HINT",
                onClick = { },
                enabled = !finished,
                leading = { IconBulb(size = 17.dp, color = if (finished) Palette.TextMuted else Palette.Amber) }
            )
            ToolButton(
                text = "RESTART",
                onClick = {
                    for (index in cells.indices) cells[index] = CellState.EMPTY
                    undoStack.clear()
                    moves = 0
                    elapsed = 0L
                    appState.clearBoard(boardKey)
                },
                enabled = !finished,
                leading = { IconRestart(size = 17.dp, color = if (finished) Palette.TextMuted else Palette.TextPrimary) }
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val available = maxWidth - 16.dp
            val cellW = available / (puzzle.cols + 0.7f * puzzle.maxRowClues)
            val cellH = maxHeight / (puzzle.rows + 0.74f * puzzle.maxColClues)
            val cell = minOf(cellW, cellH, 64.dp)
            PuzzleBoard(
                puzzle = puzzle,
                cells = cells,
                cell = cell,
                onTap = { handleTap(it) },
                onLongPress = { handleLongPress(it) },
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (!appState.cycleMode) {
            ModeBar(
                mode = editMode,
                onModeChange = { editMode = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 22.dp)
            )
        } else {
            Spacer(Modifier.height(22.dp))
        }
    }

    if (finished) {
        LevelCompletedDialog(
            moves = moves,
            elapsedMs = elapsed,
            puzzle = puzzle,
            primaryLabel = if (nextLevel != null) "NEXT LEVEL" else "PLAY AGAIN",
            onPrimary = {
                if (nextLevel != null) {
                    onOpenLevel(nextLevel)
                } else {
                    for (index in cells.indices) cells[index] = CellState.EMPTY
                    undoStack.clear()
                    moves = 0
                    elapsed = 0L
                    finished = false
                    appState.clearBoard(boardKey)
                }
            },
            onCoffee = onHome,
            onHome = onHome
        )
    }
}

@Composable
private fun ModeBar(
    mode: EditMode,
    onModeChange: (EditMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ModeButton(
            label = "FILL",
            selected = mode == EditMode.FILL,
            accent = Palette.White,
            width = 132.dp,
            onClick = { onModeChange(EditMode.FILL) },
            leading = { selected ->
                IconFilledSquare(size = 18.dp, color = if (selected) Palette.Black else Palette.White)
            }
        )
        Spacer(Modifier.width(22.dp))
        ModeButton(
            label = "CROSS (X)",
            selected = mode == EditMode.CROSS,
            accent = Palette.Red,
            width = 164.dp,
            onClick = { onModeChange(EditMode.CROSS) },
            leading = { selected ->
                IconCrossGlyph(size = 18.dp, color = if (selected) Palette.White else Palette.Red)
            }
        )
    }
}

@Composable
private fun ModeButton(
    label: String,
    selected: Boolean,
    accent: Color,
    width: Dp,
    onClick: () -> Unit,
    leading: @Composable (Boolean) -> Unit
) {
    val background = if (selected) accent else Palette.Card
    val contentColor = if (selected) {
        if (accent == Palette.White) Palette.Black else Palette.White
    } else {
        Palette.TextPrimary
    }
    Row(
        modifier = Modifier
            .width(width)
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .border(1.dp, if (selected) accent else Palette.BorderSoft, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading(selected)
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            color = contentColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun PuzzleBoard(
    puzzle: Puzzle,
    cells: List<CellState>,
    cell: Dp,
    onTap: (Int) -> Unit,
    onLongPress: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val rowClueWidth = cell * 0.7f * puzzle.maxRowClues
    val colClueHeight = cell * 0.74f * puzzle.maxColClues
    val totalWidth = rowClueWidth + cell * puzzle.cols
    val totalHeight = colClueHeight + cell * puzzle.rows

    Box(modifier = modifier.size(width = totalWidth, height = totalHeight)) {
        // Column clues: the last number of a column sits next to the grid.
        for (c in 0 until puzzle.cols) {
            Box(
                modifier = Modifier
                    .offset(x = rowClueWidth + cell * c, y = 0.dp)
                    .width(cell)
                    .height(colClueHeight)
            ) {
                Column(modifier = Modifier.align(Alignment.BottomCenter)) {
                    puzzle.colClues[c].forEach { clue ->
                        ClueText(text = "$clue", width = cell, height = cell * 0.74f)
                    }
                }
            }
        }

        // Row clues, right aligned against the grid.
        for (r in 0 until puzzle.rows) {
            Box(
                modifier = Modifier
                    .offset(x = 0.dp, y = colClueHeight + cell * r)
                    .width(rowClueWidth)
                    .height(cell)
            ) {
                Row(modifier = Modifier.align(Alignment.CenterEnd)) {
                    puzzle.rowClues[r].forEach { clue ->
                        ClueText(text = "$clue", width = cell * 0.7f, height = cell)
                    }
                }
            }
        }

        Column(modifier = Modifier.offset(x = rowClueWidth, y = colClueHeight)) {
            for (r in 0 until puzzle.rows) {
                Row {
                    for (c in 0 until puzzle.cols) {
                        val index = r * puzzle.cols + c
                        BoardCell(
                            state = cells[index],
                            size = cell,
                            onTap = { onTap(index) },
                            onLongPress = { onLongPress(index) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClueText(text: String, width: Dp, height: Dp) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Palette.TextSecondary,
            fontSize = (height.value * 0.52f).sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun BoardCell(
    state: CellState,
    size: Dp,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .padding(1.5.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(if (state == CellState.FILLED) Palette.White else Palette.CellEmpty)
            .border(
                1.dp,
                if (state == CellState.FILLED) Palette.White else Palette.CellBorder,
                RoundedCornerShape(3.dp)
            )
            .pointerInput(state) {
                detectTapGestures(
                    onTap = { onTap() },
                    onLongPress = { onLongPress() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (state == CellState.CROSS) {
            Canvas(modifier = Modifier.fillMaxSize().padding(size * 0.22f)) {
                val stroke = this.size.minDimension * 0.22f
                drawLine(
                    color = Palette.Red,
                    start = Offset(0f, 0f),
                    end = Offset(this.size.width, this.size.height),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Palette.Red,
                    start = Offset(this.size.width, 0f),
                    end = Offset(0f, this.size.height),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
