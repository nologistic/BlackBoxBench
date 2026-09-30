package com.blackboxbench.reproduction

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

private enum class Tool { FILL, MARK }

@Composable
fun GameScreen(progress: GameProgress, nav: NavController, level: Int, randomSolution: List<String>?) {
    val puzzle = remember(level) {
        if (randomSolution != null) Puzzle(0, randomSolution, randomSolution.size)
        else Puzzle(level, Puzzles.solutionFor(level), Puzzles.sizeFor(level))
    }
    val n = puzzle.size
    var grid by remember(level) { mutableStateOf(Array(n) { IntArray(n) }) } // 0 empty, 1 filled, 2 X
    var tool by remember { mutableStateOf(Tool.FILL) }
    var paused by remember { mutableStateOf(false) }
    var won by remember { mutableStateOf(false) }

    val placedCount = grid.sumOf { row -> row.count { it == 1 } }

    fun checkWin() {
        val ok = grid.indices.all { r ->
            (0 until n).all { c -> (grid[r][c] == 1) == (puzzle.solution[r][c] == '#') }
        }
        if (ok) {
            won = true
            if (level > 0) progress.completeLevel(level)
        }
    }

    BackHandler { nav.popBackStack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .statusBarsPadding()
    ) {
        // Header: back | title | pause (large 44dp hit targets)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable { nav.popBackStack() },
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.size(26.dp)) {
                    drawBackArrow(Color.White, Offset(size.width / 2, size.height / 2), size.minDimension * 0.8f)
                }
            }
            Spacer(Modifier.weight(1f))
            Text(if (level > 0) "Level $level" else "Random Puzzle", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable { paused = true },
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.size(26.dp)) {
                    drawPause(Color.White, Offset(size.width / 2, size.height / 2), size.minDimension * 0.7f)
                }
            }
        }

        // HUD: hearts + stars + progress counter
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeartsCount(progress.hearts)
            Spacer(Modifier.width(18.dp))
            StarCount(progress.stars)
            Spacer(Modifier.weight(1f))
            Text("$placedCount/${puzzle.filledCount}", color = DimText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(CellEmpty)
        ) {
            val frac = if (puzzle.filledCount == 0) 0f else placedCount.toFloat() / puzzle.filledCount
            Box(
                modifier = Modifier
                    .fillMaxWidth(frac.coerceIn(0f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(AccentGreen)
            )
        }

        Spacer(Modifier.weight(0.5f))

        NonogramBoard(
            puzzle = puzzle,
            grid = grid,
            enabled = !paused && !won,
            onCell = { r, c ->
                when (tool) {
                    Tool.FILL -> grid[r][c] = if (grid[r][c] == 1) 0 else 1
                    Tool.MARK -> grid[r][c] = if (grid[r][c] == 2) 0 else 2
                }
                val copy = Array(n) { grid[it].copyOf() }
                grid = copy
                checkWin()
            }
        )

        Spacer(Modifier.weight(0.7f))

        // Tools
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ToolButton(selected = tool == Tool.FILL, label = "FILL", onToggle = { tool = Tool.FILL }) { color, c, s ->
                drawRoundRect(
                    color = color,
                    topLeft = Offset(c.x - s / 2, c.y - s / 2),
                    size = Size(s, s),
                    cornerRadius = CornerRadius(s * 0.2f, s * 0.2f)
                )
            }
            ToolButton(selected = tool == Tool.MARK, label = "X", onToggle = { tool = Tool.MARK }) { color, c, s ->
                drawCross(color, c, s)
            }
        }
    }

    if (paused) {
        PauseOverlay(
            onResume = { paused = false },
            onRestart = {
                grid = Array(n) { IntArray(n) }
                paused = false
            },
            onMenu = { nav.popBackStack() }
        )
    }
    if (won) {
        WinOverlay(
            stars = 3,
            onNext = {
                if (level in 1 until Puzzles.levelCount()) {
                    nav.popBackStack()
                    nav.navigate("game/${level + 1}")
                } else {
                    nav.popBackStack()
                }
            },
            onMenu = { nav.popBackStack() }
        )
    }
}

@Composable
private fun ToolButton(
    selected: Boolean,
    label: String,
    onToggle: () -> Unit,
    icon: DrawScope.(Color, Offset, Float) -> Unit
) {
    val accent = if (selected) FillYellow else DimText
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) { onToggle() }
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(PanelColor)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) FillYellow else BorderSoft,
                    shape = RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(28.dp)) {
                icon(accent, Offset(size.width / 2, size.height / 2), size.minDimension * 0.8f)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(if (selected) "$label ✓" else label, color = if (selected) FillYellow else DimText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun NonogramBoard(
    puzzle: Puzzle,
    grid: Array<IntArray>,
    enabled: Boolean,
    onCell: (Int, Int) -> Unit
) {
    val n = puzzle.size
    val cellDp = 36.dp
    val clueDp = 42.dp
    val textColor = Color.White
    val dimColor = Color(0xFF6A6A75)

    fun rowComplete(r: Int): Boolean {
        val clue = puzzle.rowClues[r]
        val runList = mutableListOf<Int>()
        var run = 0
        for (c in 0 until n) {
            if (grid[r][c] == 1) run++ else if (run > 0) { runList.add(run); run = 0 }
        }
        if (run > 0) runList.add(run)
        if (runList.isEmpty()) runList.add(0)
        return runList == clue
    }

    fun colComplete(c: Int): Boolean {
        val clue = puzzle.colClues[c]
        val runList = mutableListOf<Int>()
        var run = 0
        for (r in 0 until n) {
            if (grid[r][c] == 1) run++ else if (run > 0) { runList.add(run); run = 0 }
        }
        if (run > 0) runList.add(run)
        if (runList.isEmpty()) runList.add(0)
        return runList == clue
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        // Column clues row
        Row {
            Spacer(Modifier.size(clueDp + 2.dp))
            (0 until n).forEach { c ->
                Box(
                    modifier = Modifier.size(cellDp + 2.dp, clueDp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        puzzle.colClues[c].joinToString("\n"),
                        color = if (colComplete(c)) dimColor else textColor,
                        fontSize = 10.sp,
                        lineHeight = 11.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        // Rows: row clue + cells
        (0 until n).forEach { r ->
            Row {
                Box(
                    modifier = Modifier.size(clueDp, cellDp + 2.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        puzzle.rowClues[r].joinToString(" "),
                        color = if (rowComplete(r)) dimColor else textColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
                (0 until n).forEach { c ->
                    val state = grid[r][c]
                    Box(
                        modifier = Modifier
                            .size(cellDp + 2.dp)
                            .padding(1.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (state == 1) FillYellow else CellEmpty)
                            .clickable(enabled = enabled) { onCell(r, c) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (state == 2) {
                            Canvas(Modifier.size(16.dp)) {
                                drawCross(MarkRed, Offset(size.width / 2, size.height / 2), size.minDimension * 0.85f)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverlayScrim(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun PauseOverlay(onResume: () -> Unit, onRestart: () -> Unit, onMenu: () -> Unit) {
    OverlayScrim {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(PanelColor)
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("PAUSED", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Spacer(Modifier.height(24.dp))
            MenuButton("RESUME", Modifier, onResume)
            Spacer(Modifier.height(12.dp))
            MenuButton("RESTART LEVEL", Modifier, onRestart)
            Spacer(Modifier.height(12.dp))
            MenuButton("MAIN MENU", Modifier, onMenu)
        }
    }
}

@Composable
private fun WinOverlay(stars: Int, onNext: () -> Unit, onMenu: () -> Unit) {
    OverlayScrim {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(PanelColor)
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("LEVEL COMPLETE!", color = AccentGreen, fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Spacer(Modifier.height(16.dp))
            Row {
                (0 until stars).forEach {
                    Canvas(Modifier.size(40.dp).padding(4.dp)) {
                        drawStar(StarYellow, Offset(size.width / 2, size.height / 2), size.minDimension * 0.45f)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            MenuButton("NEXT LEVEL", Modifier, onNext)
            Spacer(Modifier.height(12.dp))
            MenuButton("MAIN MENU", Modifier, onMenu)
        }
    }
}
