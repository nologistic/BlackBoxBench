package com.blackboxbench.reproduction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.random.Random

/** Lifecycle of a single board. */
enum class GameStatus { READY, PLAYING, WON, LOST }

/** The four entries of the difficulty selector. */
enum class Difficulty(
    val title: String,
    val cols: Int,
    val rows: Int,
    val mines: Int,
    val timeLimitSeconds: Int?,
    val fadesDigits: Boolean,
) {
    EASY("Easy", 9, 9, 10, null, false),
    MEDIUM("Medium", 13, 14, 30, 300, false),
    HARD("Hard", 16, 17, 50, 180, true),
    CUSTOM("Custom", 9, 9, 10, null, false),
}

/** Settings edited inside the "Custom Game" dialog. */
data class CustomSettings(
    val size: Int = 16,
    val mines: Int = 40,
    val fogOfWar: Boolean = false,
    val safeFirstTap: Boolean = true,
)

/** Upper bound for the mine slider: a 3x3 safe pocket always stays free. */
fun maxMinesFor(size: Int): Int = (size * size - 9).coerceAtLeast(1)

fun minePercent(mines: Int, size: Int): Int =
    (mines * 100) / (size * size)

class Cell {
    var mine = false
    var revealed = false
    var flagged = false
    var adjacent = 0
    var revealedAt = 0L
    var exploded = false
}

private const val DIGIT_FADE_MILLIS = 20_000L

/**
 * Full minesweeper model: hidden board generation, flood reveal, flagging,
 * chording, win detection and the two loss conditions (mine hit / timeout).
 */
class MinesweeperBoard(
    val cols: Int,
    val rows: Int,
    val totalMines: Int,
    val timeLimitSeconds: Int?,
    val fadeDigits: Boolean,
    val fogOfWar: Boolean,
    val safeFirstTap: Boolean,
) {
    val grid: Array<Array<Cell>> = Array(rows) { Array(cols) { Cell() } }

    var status by mutableStateOf(GameStatus.READY)
        private set
    var seconds by mutableStateOf(0)
        private set
    var flags by mutableStateOf(0)
        private set
    var started by mutableStateOf(false)
        private set

    /** Bumped on every model mutation so Compose redraws the board. */
    var revision by mutableStateOf(0)
        private set

    private var minesPlaced = false
    private var random = Random(System.nanoTime())

    val isOver: Boolean get() = status == GameStatus.WON || status == GameStatus.LOST
    val remainingMines: Int get() = (totalMines - flags).coerceAtLeast(0)

    fun cell(r: Int, c: Int): Cell = grid[r][c]

    fun isDigitFaded(cell: Cell, now: Long): Boolean =
        fadeDigits && cell.revealed && !cell.mine && cell.adjacent > 0 &&
            cell.revealedAt > 0 && now - cell.revealedAt > DIGIT_FADE_MILLIS

    private fun touch() {
        revision++
    }

    private fun recountFlags() {
        var n = 0
        for (r in 0 until rows) for (c in 0 until cols) if (grid[r][c].flagged) n++
        flags = n
    }

    /** One second of game time; drives the count-up / countdown display. */
    fun tick() {
        if (status != GameStatus.PLAYING) return
        seconds++
        val limit = timeLimitSeconds
        if (limit != null && seconds >= limit) {
            status = GameStatus.LOST
            revealAllMines()
        }
        touch()
    }

    fun tap(r: Int, c: Int) {
        if (isOver) return
        val target = grid[r][c]
        if (target.flagged) return
        if (target.revealed) {
            chord(r, c)
            return
        }
        if (!started) {
            started = true
            status = GameStatus.PLAYING
            placeMines(r, c)
        }
        if (target.mine) {
            explode(r, c)
            touch()
            return
        }
        floodReveal(r, c)
        checkWin()
        touch()
    }

    fun longPress(r: Int, c: Int) {
        if (isOver) return
        val target = grid[r][c]
        if (target.revealed) return
        target.flagged = !target.flagged
        recountFlags()
        touch()
    }

    private fun placeMines(safeRow: Int, safeCol: Int) {
        val safe = HashSet<Int>()
        if (safeFirstTap) {
            for (dr in -1..1) for (dc in -1..1) {
                val r = safeRow + dr
                val c = safeCol + dc
                if (r in 0 until rows && c in 0 until cols) safe.add(r * cols + c)
            }
        }
        val candidates = ArrayList<Int>(rows * cols)
        for (i in 0 until rows * cols) if (i !in safe) candidates.add(i)
        candidates.shuffle(random)
        val count = totalMines.coerceAtMost(candidates.size)
        for (i in 0 until count) {
            val idx = candidates[i]
            grid[idx / cols][idx % cols].mine = true
        }
        minesPlaced = true
        computeAdjacency()
    }

    private fun computeAdjacency() {
        for (r in 0 until rows) for (c in 0 until cols) {
            if (grid[r][c].mine) continue
            var n = 0
            for (dr in -1..1) for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val nr = r + dr
                val nc = c + dc
                if (nr in 0 until rows && nc in 0 until cols && grid[nr][nc].mine) n++
            }
            grid[r][c].adjacent = n
        }
    }

    private fun floodReveal(startRow: Int, startCol: Int) {
        val now = System.currentTimeMillis()
        val stack = ArrayDeque<Int>()
        stack.addLast(startRow * cols + startCol)
        while (stack.isNotEmpty()) {
            val idx = stack.removeLast()
            val r = idx / cols
            val c = idx % cols
            val cell = grid[r][c]
            if (cell.revealed || cell.flagged || cell.mine) continue
            cell.revealed = true
            cell.revealedAt = now
            if (cell.adjacent == 0) {
                for (dr in -1..1) for (dc in -1..1) {
                    if (dr == 0 && dc == 0) continue
                    val nr = r + dr
                    val nc = c + dc
                    if (nr in 0 until rows && nc in 0 until cols) {
                        val next = grid[nr][nc]
                        if (!next.revealed && !next.flagged && !next.mine) stack.addLast(nr * cols + nc)
                    }
                }
            }
        }
    }

    private fun chord(r: Int, c: Int) {
        val cell = grid[r][c]
        if (cell.adjacent == 0) return
        var flaggedNeighbours = 0
        val covered = ArrayList<Int>()
        for (dr in -1..1) for (dc in -1..1) {
            if (dr == 0 && dc == 0) continue
            val nr = r + dr
            val nc = c + dc
            if (nr !in 0 until rows || nc !in 0 until cols) continue
            val next = grid[nr][nc]
            if (next.flagged) flaggedNeighbours++ else if (!next.revealed) covered.add(nr * cols + nc)
        }
        if (flaggedNeighbours != cell.adjacent) return
        for (idx in covered) {
            val nr = idx / cols
            val nc = idx % cols
            if (grid[nr][nc].mine) {
                explode(nr, nc)
                touch()
                return
            }
            floodReveal(nr, nc)
        }
        checkWin()
        touch()
    }

    private fun explode(r: Int, c: Int) {
        grid[r][c].exploded = true
        status = GameStatus.LOST
        revealAllMines()
    }

    private fun revealAllMines() {
        for (r in 0 until rows) for (c in 0 until cols) {
            if (grid[r][c].mine) grid[r][c].revealed = true
        }
    }

    private fun checkWin() {
        for (r in 0 until rows) for (c in 0 until cols) {
            val cell = grid[r][c]
            if (!cell.mine && !cell.revealed) return
        }
        status = GameStatus.WON
        for (r in 0 until rows) for (c in 0 until cols) {
            val cell = grid[r][c]
            if (cell.mine) cell.flagged = true
        }
        recountFlags()
    }
}

fun createBoard(difficulty: Difficulty, custom: CustomSettings): MinesweeperBoard = when (difficulty) {
    Difficulty.CUSTOM -> MinesweeperBoard(
        cols = custom.size,
        rows = custom.size,
        totalMines = custom.mines.coerceIn(1, maxMinesFor(custom.size)),
        timeLimitSeconds = null,
        fadeDigits = custom.fogOfWar,
        fogOfWar = custom.fogOfWar,
        safeFirstTap = custom.safeFirstTap,
    )

    else -> MinesweeperBoard(
        cols = difficulty.cols,
        rows = difficulty.rows,
        totalMines = difficulty.mines,
        timeLimitSeconds = difficulty.timeLimitSeconds,
        fadeDigits = difficulty.fadesDigits,
        fogOfWar = false,
        safeFirstTap = true,
    )
}
