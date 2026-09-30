package com.blackboxbench.reproduction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.min
import kotlin.random.Random

enum class Difficulty(val title: String) { EASY("Easy"), MEDIUM("Medium"), HARD("Hard"), CUSTOM("Custom") }

enum class GameStatus { READY, RUNNING, WON, LOST }

/** A single board tile. Immutable; the controller swaps in new instances. */
data class Cell(
    val mine: Boolean = false,
    val revealed: Boolean = false,
    val flagged: Boolean = false,
    val adjacent: Int = 0,
    val revealedAt: Long = 0L,
    val exploded: Boolean = false
)

/** Everything needed to (re)start a game. */
data class GameConfig(
    val difficulty: Difficulty,
    val size: Int,
    val mines: Int,
    val fog: Boolean,
    val safeFirstTap: Boolean,
    /** Countdown length in seconds, or null to count up. */
    val timeLimit: Int?
)

fun presetConfig(difficulty: Difficulty): GameConfig = when (difficulty) {
    Difficulty.EASY -> GameConfig(Difficulty.EASY, 10, 10, fog = false, safeFirstTap = true, timeLimit = null)
    Difficulty.MEDIUM -> GameConfig(Difficulty.MEDIUM, 16, 30, fog = false, safeFirstTap = true, timeLimit = 300)
    Difficulty.HARD -> GameConfig(Difficulty.HARD, 20, 50, fog = true, safeFirstTap = false, timeLimit = 180)
    Difficulty.CUSTOM -> GameConfig(Difficulty.CUSTOM, 16, 40, fog = false, safeFirstTap = true, timeLimit = null)
}

/** How long a freshly cleared tile keeps showing its number when fog of war is on. */
const val FOG_REVEAL_MILLIS = 15_000L

/**
 * Holds the whole Minesweeper session: board, flag count, clock and status.
 * State lives in Compose snapshot state so the UI observes every change.
 */
class GameController(val config: GameConfig, seed: Long? = null) {

    private val random: Random = if (seed == null) Random.Default else Random(seed)

    var cells by mutableStateOf(List(config.size * config.size) { Cell() })
        private set

    var status by mutableStateOf(GameStatus.READY)
        private set

    var flagsPlaced by mutableStateOf(0)
        private set

    /** Seconds since the first reveal (count-up) or seconds elapsed of the countdown. */
    var elapsed by mutableStateOf(0)
        private set

    var lastExploded by mutableStateOf(-1)
        private set

    var minesPlaced by mutableStateOf(false)
        private set

    var nowMillis by mutableStateOf(System.currentTimeMillis())
        private set

    val size: Int get() = config.size
    val remainingMines: Int get() = config.mines - flagsPlaced

    /** Seconds left for a countdown game (never below zero). */
    val secondsLeft: Int
        get() {
            val limit = config.timeLimit ?: return elapsed
            return (limit - elapsed).coerceAtLeast(0)
        }

    init {
        if (!config.safeFirstTap) placeMines(-1, -1)
    }

    fun clockTick(now: Long) {
        nowMillis = now
    }

    /** Advance the game clock by one second; a finished countdown loses the game. */
    fun advanceSecond() {
        if (status != GameStatus.RUNNING) return
        elapsed += 1
        val limit = config.timeLimit
        if (limit != null && elapsed >= limit) {
            lose(-1)
        }
    }

    fun cellAt(row: Int, col: Int): Cell = cells[row * size + col]

    private fun index(row: Int, col: Int) = row * size + col

    private fun neighbours(row: Int, col: Int): List<Int> {
        val out = ArrayList<Int>(8)
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val r = row + dr
                val c = col + dc
                if (r in 0 until size && c in 0 until size) out.add(index(r, c))
            }
        }
        return out
    }

    private fun placeMines(safeRow: Int, safeCol: Int) {
        val total = size * size
        val blocked = HashSet<Int>()
        if (safeRow >= 0) {
            for (dr in -1..1) for (dc in -1..1) {
                val r = safeRow + dr
                val c = safeCol + dc
                if (r in 0 until size && c in 0 until size) blocked.add(index(r, c))
            }
        }
        val pool = ArrayList<Int>(total)
        for (i in 0 until total) if (i !in blocked) pool.add(i)
        pool.shuffle(random)
        val board = cells.toMutableList()
        val count = min(config.mines, pool.size)
        for (i in 0 until count) board[pool[i]] = board[pool[i]].copy(mine = true)
        for (r in 0 until size) {
            for (c in 0 until size) {
                val i = index(r, c)
                if (board[i].mine) continue
                var adjacent = 0
                for (n in neighbours(r, c)) if (board[n].mine) adjacent++
                board[i] = board[i].copy(adjacent = adjacent)
            }
        }
        cells = board
        minesPlaced = true
    }

    /** Primary tap: reveal a covered tile, or chord an already revealed number. */
    fun tap(row: Int, col: Int) {
        if (status == GameStatus.WON || status == GameStatus.LOST) return
        if (row !in 0 until size || col !in 0 until size) return
        val i = index(row, col)
        val cell = cells[i]
        if (cell.revealed) {
            chord(row, col)
            return
        }
        if (cell.flagged) return
        if (!minesPlaced) placeMines(row, col)
        if (status == GameStatus.READY) status = GameStatus.RUNNING
        if (cells[i].mine) {
            lose(i)
            return
        }
        floodReveal(i)
        checkWin()
    }

    /** Long press toggles a flag on a covered tile. */
    fun toggleFlag(row: Int, col: Int) {
        if (status == GameStatus.WON || status == GameStatus.LOST) return
        if (row !in 0 until size || col !in 0 until size) return
        val i = index(row, col)
        val cell = cells[i]
        if (cell.revealed) return
        val board = cells.toMutableList()
        board[i] = cell.copy(flagged = !cell.flagged)
        cells = board
        flagsPlaced += if (board[i].flagged) 1 else -1
    }

    private fun chord(row: Int, col: Int) {
        val i = index(row, col)
        val cell = cells[i]
        if (!cell.revealed || cell.adjacent == 0) return
        val around = neighbours(row, col)
        if (around.count { cells[it].flagged } != cell.adjacent) return
        for (n in around) {
            val other = cells[n]
            if (other.flagged || other.revealed) continue
            if (other.mine) {
                lose(n)
                return
            }
            floodReveal(n)
        }
        checkWin()
    }

    private fun floodReveal(start: Int) {
        val board = cells.toMutableList()
        val now = System.currentTimeMillis()
        val queue = ArrayDeque<Int>()
        if (!board[start].revealed && !board[start].flagged) {
            board[start] = board[start].copy(revealed = true, revealedAt = now)
            queue.add(start)
        }
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (board[current].adjacent != 0) continue
            val r = current / size
            val c = current % size
            for (n in neighbours(r, c)) {
                val other = board[n]
                if (other.revealed || other.flagged || other.mine) continue
                board[n] = other.copy(revealed = true, revealedAt = now)
                queue.add(n)
            }
        }
        cells = board
    }

    private fun checkWin() {
        val allSafeRevealed = cells.all { it.mine || it.revealed }
        if (!allSafeRevealed) return
        val board = cells.toMutableList()
        for (i in board.indices) {
            if (board[i].mine && !board[i].flagged) board[i] = board[i].copy(flagged = true)
        }
        cells = board
        flagsPlaced = config.mines
        status = GameStatus.WON
    }

    private fun lose(explodedIndex: Int) {
        val board = cells.toMutableList()
        for (i in board.indices) {
            if (board[i].mine) board[i] = board[i].copy(revealed = true)
        }
        if (explodedIndex >= 0) board[explodedIndex] = board[explodedIndex].copy(exploded = true)
        cells = board
        lastExploded = explodedIndex
        status = GameStatus.LOST
    }

    /** True while a fogged tile should still show its number. */
    fun numberVisible(cell: Cell): Boolean =
        !config.fog || (nowMillis - cell.revealedAt) < FOG_REVEAL_MILLIS
}
