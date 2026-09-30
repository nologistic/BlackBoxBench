package com.blackboxbench.reproduction

import kotlin.random.Random

/** Difficulty choices shown in the top tab row. */
enum class Difficulty { EASY, MEDIUM, HARD, CUSTOM }

/** A difficulty definition: board size, mine count and (optional) countdown limit. */
data class Preset(
    val cols: Int,
    val rows: Int,
    val mines: Int,
    val timeLimitSeconds: Int
)

val EASY_PRESET = Preset(cols = 9, rows = 9, mines = 10, timeLimitSeconds = 0)
val MEDIUM_PRESET = Preset(cols = 16, rows = 16, mines = 30, timeLimitSeconds = 300)
val HARD_PRESET = Preset(cols = 16, rows = 16, mines = 50, timeLimitSeconds = 180)

enum class GameStatus { PLAYING, WON, LOST }

data class Cell(
    val mine: Boolean = false,
    val revealed: Boolean = false,
    val flagged: Boolean = false,
    val adjacent: Int = 0,
    val exploded: Boolean = false
)

data class GameState(
    val difficulty: Difficulty = Difficulty.EASY,
    val cols: Int = EASY_PRESET.cols,
    val rows: Int = EASY_PRESET.rows,
    val mineCount: Int = EASY_PRESET.mines,
    val timeLimitSeconds: Int = EASY_PRESET.timeLimitSeconds,
    val fog: Boolean = false,
    val safeFirstTap: Boolean = true,
    val cells: List<Cell> = List(EASY_PRESET.cols * EASY_PRESET.rows) { Cell() },
    val status: GameStatus = GameStatus.PLAYING,
    val minesPlaced: Boolean = false,
    val running: Boolean = false,
    val elapsedSeconds: Int = 0
) {
    val flagCount: Int get() = cells.count { it.flagged }
    val counter: Int get() = mineCount - flagCount
    val revealedSafe: Int get() = cells.count { it.revealed && !it.mine }
    val safeTotal: Int get() = (cols * rows - mineCount).coerceAtLeast(0)

    fun index(x: Int, y: Int): Int = y * cols + x
}

/** Maximum number of mines allowed for a square board of the given size. */
fun maxMinesFor(cols: Int): Int {
    val cells = cols * cols
    val byRatio = (cells * 0.93f).toInt()
    val bySafeZone = cells - 9
    return minOf(byRatio, bySafeZone).coerceAtLeast(1)
}

fun newGame(
    preset: Preset,
    difficulty: Difficulty,
    fog: Boolean = false,
    safeFirstTap: Boolean = true,
    mineCount: Int = preset.mines
): GameState {
    val mines = mineCount.coerceIn(1, maxMinesFor(preset.cols))
    return GameState(
        difficulty = difficulty,
        cols = preset.cols,
        rows = preset.rows,
        mineCount = mines,
        timeLimitSeconds = preset.timeLimitSeconds,
        fog = fog,
        safeFirstTap = safeFirstTap,
        cells = List(preset.cols * preset.rows) { Cell() }
    )
}

private fun neighborsOf(cols: Int, rows: Int, index: Int): List<Int> {
    val x = index % cols
    val y = index / cols
    val out = ArrayList<Int>(8)
    for (dy in -1..1) {
        for (dx in -1..1) {
            if (dx == 0 && dy == 0) continue
            val nx = x + dx
            val ny = y + dy
            if (nx in 0 until cols && ny in 0 until rows) out.add(ny * cols + nx)
        }
    }
    return out
}

/** Places mines, optionally keeping the first tapped cell (and its neighbours) clear. */
private fun placeMines(state: GameState, safeIndex: Int?): List<Cell> {
    val total = state.cols * state.rows
    val forbidden = HashSet<Int>()
    if (safeIndex != null) {
        forbidden.add(safeIndex)
        forbidden.addAll(neighborsOf(state.cols, state.rows, safeIndex))
    }
    val candidates = (0 until total).filter { it !in forbidden }
    val mineIndices = candidates.shuffled(Random.Default).take(state.mineCount).toHashSet()
    val result = ArrayList<Cell>(total)
    for (i in 0 until total) {
        val isMine = i in mineIndices
        var adj = 0
        if (!isMine) {
            for (n in neighborsOf(state.cols, state.rows, i)) {
                if (n in mineIndices) adj++
            }
        }
        result.add(Cell(mine = isMine, adjacent = adj))
    }
    return result
}

/** Reveals a cell; flood-fills empty regions; resolves win / loss. */
fun reveal(state: GameState, x: Int, y: Int): GameState {
    if (state.status != GameStatus.PLAYING) return state
    if (x !in 0 until state.cols || y !in 0 until state.rows) return state
    val index = state.index(x, y)
    if (state.cells[index].flagged || state.cells[index].revealed) return state

    var s = state
    if (!s.minesPlaced) {
        val safe = if (s.safeFirstTap) index else null
        s = s.copy(cells = placeMines(s, safe), minesPlaced = true)
    }

    if (s.cells[index].mine) {
        val cells = s.cells.mapIndexed { i, c ->
            if (c.mine) c.copy(revealed = true) else c
        }.toMutableList()
        cells[index] = cells[index].copy(exploded = true)
        return s.copy(cells = cells, status = GameStatus.LOST, running = false)
    }

    val cells = s.cells.toMutableList()
    val stack = ArrayDeque<Int>()
    stack.addLast(index)
    while (stack.isNotEmpty()) {
        val i = stack.removeLast()
        val c = cells[i]
        if (c.revealed || c.flagged || c.mine) continue
        cells[i] = c.copy(revealed = true)
        if (c.adjacent == 0) {
            for (n in neighborsOf(s.cols, s.rows, i)) {
                val nc = cells[n]
                if (!nc.revealed && !nc.flagged && !nc.mine) stack.addLast(n)
            }
        }
    }
    s = s.copy(cells = cells, running = true)

    if (s.revealedSafe >= s.safeTotal) {
        val won = s.cells.map { c -> if (c.mine) c.copy(flagged = true) else c }
        return s.copy(cells = won, status = GameStatus.WON, running = false)
    }
    return s
}

/** Long-press toggles a flag on an unrevealed cell; the counter may go negative. */
fun toggleFlag(state: GameState, x: Int, y: Int): GameState {
    if (state.status != GameStatus.PLAYING) return state
    if (x !in 0 until state.cols || y !in 0 until state.rows) return state
    val index = state.index(x, y)
    val c = state.cells[index]
    if (c.revealed) return state
    val cells = state.cells.toMutableList()
    cells[index] = c.copy(flagged = !c.flagged)
    return state.copy(cells = cells)
}

/** Advances the clock by one second and ends the game when a countdown hits zero. */
fun tick(state: GameState): GameState {
    if (state.status != GameStatus.PLAYING || !state.running) return state
    val next = state.elapsedSeconds + 1
    if (state.timeLimitSeconds > 0 && next >= state.timeLimitSeconds) {
        val cells = state.cells.map { c -> if (c.mine) c.copy(revealed = true) else c }
        return state.copy(
            cells = cells,
            status = GameStatus.LOST,
            running = false,
            elapsedSeconds = state.timeLimitSeconds
        )
    }
    return state.copy(elapsedSeconds = next)
}

fun formatTimer(state: GameState): String {
    val seconds = if (state.timeLimitSeconds > 0) {
        (state.timeLimitSeconds - state.elapsedSeconds).coerceAtLeast(0)
    } else {
        state.elapsedSeconds.coerceAtMost(99 * 60 + 59)
    }
    return "%02d:%02d".format(seconds / 60, seconds % 60)
}

fun formatCounter(value: Int): String =
    if (value >= 0) "%03d".format(value.coerceAtMost(999))
    else "-" + "%02d".format((-value).coerceAtMost(99))
