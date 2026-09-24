package com.blackboxbench.reproduction

import kotlin.random.Random

/** Which difficulty tab / configuration produced the current board. */
enum class Screen { EASY, MEDIUM, HARD, CUSTOM }

enum class Phase { READY, PLAYING, WON, LOST }

/** Seconds a revealed cell stays readable before the fog swallows it again. */
const val FOG_MS = 7000L

/** The board always keeps at least this many mine free cells (observed mine cap). */
const val MINE_FREE_MARGIN = 9

fun maxMinesFor(cells: Int): Int = (cells - MINE_FREE_MARGIN).coerceAtLeast(1)

data class GameConfig(
    val screen: Screen,
    val cols: Int,
    val rows: Int,
    val mines: Int,
    val fog: Boolean,
    val safeFirstTap: Boolean,
    /** null = count up from zero, otherwise count down from this many seconds. */
    val countdownSeconds: Int?
) {
    val cells: Int get() = cols * rows
}

data class CellState(
    val mine: Boolean = false,
    val adjacent: Int = 0,
    val revealed: Boolean = false,
    val flagged: Boolean = false,
    val revealedAt: Long = 0L,
    val exploded: Boolean = false
)

data class GameState(
    val config: GameConfig,
    val cells: List<CellState>,
    val minesPlaced: Boolean = false,
    val phase: Phase = Phase.READY,
    val startedAt: Long = 0L,
    val endedAt: Long = 0L,
    val elapsedMs: Long = 0L,
    val explodedIndex: Int = -1,
    val flagCount: Int = 0
) {
    val cols: Int get() = config.cols
    val rows: Int get() = config.rows

    /** Mines that are still unaccounted for; may go negative when over flagging. */
    val remainingMines: Int get() = config.mines - flagCount

    fun neighbors(index: Int): List<Int> {
        val r = index / cols
        val c = index % cols
        val out = ArrayList<Int>(8)
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val nr = r + dr
                val nc = c + dc
                if (nr in 0 until rows && nc in 0 until cols) out.add(nr * cols + nc)
            }
        }
        return out
    }

    /** With fog of war a revealed cell turns back into an unreadable purple tile. */
    fun isFogged(index: Int, now: Long): Boolean {
        if (!config.fog) return false
        if (phase != Phase.PLAYING) return false
        val cell = cells[index]
        return cell.revealed && !cell.mine && now - cell.revealedAt > FOG_MS
    }
}

fun newGame(config: GameConfig): GameState {
    val mines = config.mines.coerceIn(1, maxMinesFor(config.cells))
    return GameState(config = config.copy(mines = mines), cells = List(config.cells) { CellState() })
}

private fun withMines(state: GameState, safeIndex: Int, random: Random): List<CellState> {
    val cells = state.cells.toMutableList()
    val forbidden = HashSet<Int>()
    if (state.config.safeFirstTap) {
        forbidden.add(safeIndex)
        forbidden.addAll(state.neighbors(safeIndex))
    }
    val candidates = cells.indices.filter { it !in forbidden }
    val mined = candidates.shuffled(random).take(state.config.mines).toSet()
    for (i in cells.indices) {
        cells[i] = cells[i].copy(mine = i in mined)
    }
    for (i in cells.indices) {
        val adjacent = state.neighbors(i).count { cells[it].mine }
        cells[i] = cells[i].copy(adjacent = adjacent)
    }
    return cells
}

private fun revealCell(state: GameState, start: Int, now: Long): GameState {
    val cells = state.cells.toMutableList()
    val pending = ArrayDeque<Int>()
    pending.addLast(start)
    var exploded = state.explodedIndex
    var phase = state.phase
    while (pending.isNotEmpty()) {
        val i = pending.removeFirst()
        val cell = cells[i]
        if (cell.revealed || cell.flagged) continue
        if (cell.mine) {
            cells[i] = cell.copy(revealed = true, revealedAt = now, exploded = true)
            exploded = i
            phase = Phase.LOST
            continue
        }
        cells[i] = cell.copy(revealed = true, revealedAt = now)
        if (cell.adjacent == 0) {
            for (n in state.neighbors(i)) {
                if (!cells[n].revealed && !cells[n].mine) pending.addLast(n)
            }
        }
    }
    return state.copy(cells = cells, explodedIndex = exploded, phase = phase)
}

private fun elapsedOf(state: GameState, now: Long): Long =
    if (state.startedAt == 0L) 0L else (now - state.startedAt).coerceAtLeast(0L)

private fun settle(state: GameState, now: Long): GameState {
    if (state.phase == Phase.LOST) {
        val cells = state.cells.map { if (it.mine) it.copy(revealed = true, revealedAt = now) else it }
        return state.copy(cells = cells, endedAt = now, elapsedMs = elapsedOf(state, now))
    }
    if (state.phase == Phase.PLAYING) {
        val cleared = state.cells.indices.all { state.cells[it].mine || state.cells[it].revealed }
        if (cleared) {
            val cells = state.cells.map { if (it.mine) it.copy(flagged = true) else it }
            return state.copy(
                cells = cells,
                phase = Phase.WON,
                endedAt = now,
                elapsedMs = elapsedOf(state, now),
                flagCount = state.config.mines
            )
        }
    }
    return state
}

fun tapCell(state: GameState, index: Int, now: Long, random: Random = Random.Default): GameState {
    if (index !in state.cells.indices) return state
    if (state.phase == Phase.WON || state.phase == Phase.LOST) return state
    if (state.isFogged(index, now)) return state
    val cell = state.cells[index]
    if (cell.flagged) return state
    if (cell.revealed) {
        // Chord: a revealed number whose surrounding flags match opens the rest.
        if (cell.adjacent == 0) return state
        val nbrs = state.neighbors(index)
        if (nbrs.count { state.cells[it].flagged } != cell.adjacent) return state
        var next = state
        for (n in nbrs) {
            if (!next.cells[n].revealed && !next.cells[n].flagged) next = revealCell(next, n, now)
        }
        return settle(next, now)
    }
    var next = state
    if (!next.minesPlaced) {
        next = next.copy(cells = withMines(next, index, random), minesPlaced = true)
    }
    if (next.phase == Phase.READY) {
        next = next.copy(phase = Phase.PLAYING, startedAt = now)
    }
    next = revealCell(next, index, now)
    return settle(next, now)
}

fun longPressCell(state: GameState, index: Int, now: Long): GameState {
    if (index !in state.cells.indices) return state
    if (state.phase == Phase.WON || state.phase == Phase.LOST) return state
    val cell = state.cells[index]
    if (cell.revealed) return state
    if (state.isFogged(index, now)) return state
    val cells = state.cells.toMutableList()
    val flagged = !cell.flagged
    cells[index] = cell.copy(flagged = flagged)
    return state.copy(cells = cells, flagCount = state.flagCount + if (flagged) 1 else -1)
}

/** Runs the clock: the countdown difficulties lose the game when time runs out. */
fun tick(state: GameState, now: Long): GameState {
    if (state.phase != Phase.PLAYING) return state
    val limit = state.config.countdownSeconds ?: return state
    val elapsed = elapsedOf(state, now)
    if (elapsed >= limit * 1000L) {
        val cells = state.cells.map { if (it.mine) it.copy(revealed = true, revealedAt = now) else it }
        return state.copy(cells = cells, phase = Phase.LOST, endedAt = now, elapsedMs = limit * 1000L)
    }
    return state
}

/** Seconds shown on the LED display (counts up, or down for the timed difficulties). */
fun displaySeconds(state: GameState, now: Long): Int {
    val frozen = state.phase == Phase.WON || state.phase == Phase.LOST
    val elapsedSec = when {
        frozen -> state.elapsedMs / 1000L
        state.startedAt == 0L -> 0L
        else -> (now - state.startedAt) / 1000L
    }
    val limit = state.config.countdownSeconds
    return if (limit == null) elapsedSec.coerceAtMost(999L).toInt()
    else (limit - elapsedSec).coerceAtLeast(0L).toInt()
}
