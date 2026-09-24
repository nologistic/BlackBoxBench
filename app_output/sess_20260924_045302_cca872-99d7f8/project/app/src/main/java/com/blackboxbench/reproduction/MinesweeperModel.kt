package com.blackboxbench.reproduction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/** Game logic for the reproduced minesweeper. */

enum class GameStatus { PLAYING, WON, LOST }

/** The three preset difficulties plus the user defined one. */
enum class Preset(
    val title: String,
    val cols: Int,
    val rows: Int,
    val mines: Int,
    val limitSeconds: Int?,
    val fog: Boolean,
    val subtitle: String
) {
    EASY("Easy", 9, 9, 10, null, false, "10 \uD83D\uDCA3"),
    MEDIUM("Medium", 12, 14, 30, 300, false, "30 \uD83D\uDCA3"),
    HARD("Hard", 16, 16, 50, 180, true, "50 \uD83D\uDCA3"),
    CUSTOM("Custom", 16, 16, 40, null, false, "\u2699")
}

/** Settings edited in the Custom Game dialog. */
data class CustomConfig(
    val size: Int = 16,
    val mines: Int = 40,
    val fog: Boolean = false,
    val safeFirstTap: Boolean = true
) {
    companion object {
        const val MIN_SIZE = 5
        const val MAX_SIZE = 20
        const val SIZE_STEPS = MAX_SIZE - MIN_SIZE
        fun maxMines(size: Int): Int = (size * size - 9).coerceAtLeast(1)
    }

    fun clamped(): CustomConfig {
        val s = size.coerceIn(MIN_SIZE, MAX_SIZE)
        return copy(size = s, mines = mines.coerceIn(1, maxMines(s)))
    }

    fun minePercent(): Int = mines * 100 / (size * size)
}

/** Milliseconds a revealed number stays readable while fog of war is on. */
const val FOG_DELAY_MS = 7_000L

class Game(
    val cols: Int,
    val rows: Int,
    val mineCount: Int,
    val fog: Boolean,
    val safeFirstTap: Boolean,
    val limitSeconds: Int?
) {
    val cellCount = cols * rows

    private val mine = BooleanArray(cellCount)
    private val revealed = BooleanArray(cellCount)
    private val flagged = BooleanArray(cellCount)
    private val adjacent = IntArray(cellCount)
    private val revealedAt = LongArray(cellCount)

    var minesPlaced = false
        private set
    var status = GameStatus.PLAYING
        private set
    var explodedIndex = -1
        private set
    var started = false
        private set
    var elapsedMs = 0L
        private set
    var remainingMs: Long = (limitSeconds ?: 0).toLong() * 1000L
        private set
    var nowMs = 0L
        private set

    /** Bumped whenever anything visible changes. Compose state so the UI redraws. */
    var version by mutableIntStateOf(0)
        private set

    val hasLimit: Boolean get() = limitSeconds != null
    val flagsPlaced: Int get() = flagged.count { it }
    val counterValue: Int get() = mineCount - flagsPlaced
    val finished: Boolean get() = status != GameStatus.PLAYING

    fun touch() {
        version++
    }

    fun setNow(now: Long) {
        nowMs = now
        version++
    }

    fun colOf(i: Int) = i % cols
    fun rowOf(i: Int) = i / cols
    fun index(c: Int, r: Int) = r * cols + c

    fun isMine(i: Int) = mine[i]
    fun isRevealed(i: Int) = revealed[i]
    fun isFlagged(i: Int) = flagged[i]
    fun adjacentOf(i: Int) = adjacent[i]

    /** True when a revealed tile with fog of war has lost its number. */
    fun isFogged(i: Int): Boolean =
        fog && revealed[i] && !mine[i] && nowMs - revealedAt[i] > FOG_DELAY_MS

    fun neighbors(i: Int): List<Int> {
        val c = colOf(i)
        val r = rowOf(i)
        val out = ArrayList<Int>(8)
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dc == 0 && dr == 0) continue
                val nc = c + dc
                val nr = r + dr
                if (nc in 0 until cols && nr in 0 until rows) out.add(index(nc, nr))
            }
        }
        return out
    }

    private fun placeMines(safeIndex: Int) {
        val forbidden = HashSet<Int>()
        forbidden.add(safeIndex)
        if (safeFirstTap) forbidden.addAll(neighbors(safeIndex))
        val candidates = (0 until cellCount).filter { it !in forbidden }
        val shuffled = candidates.shuffled()
        var placed = 0
        for (i in shuffled) {
            if (placed >= mineCount) break
            mine[i] = true
            placed++
        }
        if (placed < mineCount) {
            // Extremely dense boards: fall back to filling the remaining cells.
            for (i in 0 until cellCount) {
                if (placed >= mineCount) break
                if (!mine[i] && i != safeIndex) {
                    mine[i] = true
                    placed++
                }
            }
        }
        for (i in 0 until cellCount) {
            adjacent[i] = if (mine[i]) 0 else neighbors(i).count { mine[it] }
        }
        minesPlaced = true
    }

    private fun floodReveal(start: Int, now: Long) {
        val stack = ArrayDeque<Int>()
        stack.addLast(start)
        while (stack.isNotEmpty()) {
            val i = stack.removeLast()
            if (revealed[i] || flagged[i] || mine[i]) continue
            revealed[i] = true
            revealedAt[i] = now
            if (adjacent[i] == 0) {
                for (n in neighbors(i)) {
                    if (!revealed[n] && !flagged[n] && !mine[n]) stack.addLast(n)
                }
            }
        }
    }

    private fun revealAllMines() {
        for (i in 0 until cellCount) if (mine[i]) revealed[i] = true
    }

    private fun checkWin() {
        val done = (0 until cellCount).all { mine[it] || revealed[it] }
        if (done) {
            status = GameStatus.WON
            for (i in 0 until cellCount) if (mine[i]) flagged[i] = true
        }
    }

    private fun lose(exploded: Int) {
        status = GameStatus.LOST
        explodedIndex = exploded
        revealAllMines()
    }

    /** First tap on the board; places mines lazily so the opening move is safe. */
    fun reveal(i: Int, now: Long) {
        if (status != GameStatus.PLAYING) return
        if (i !in 0 until cellCount) return
        if (revealed[i] || flagged[i]) return
        if (!minesPlaced) {
            placeMines(i)
            started = true
        }
        if (mine[i]) {
            lose(i)
            version++
            return
        }
        floodReveal(i, now)
        checkWin()
        version++
    }

    fun toggleFlag(i: Int) {
        if (status != GameStatus.PLAYING) return
        if (i !in 0 until cellCount) return
        if (revealed[i]) return
        flagged[i] = !flagged[i]
        version++
    }

    /** Tap on a satisfied number opens the remaining hidden neighbours. */
    fun chord(i: Int, now: Long) {
        if (status != GameStatus.PLAYING) return
        if (i !in 0 until cellCount) return
        if (!revealed[i]) return
        val count = adjacent[i]
        if (count <= 0) return
        val nb = neighbors(i)
        if (nb.count { flagged[it] } != count) return
        val targets = nb.filter { !flagged[it] && !revealed[it] }
        if (targets.isEmpty()) return
        val hit = targets.firstOrNull { mine[it] }
        if (hit != null) {
            lose(hit)
            version++
            return
        }
        targets.forEach { floodReveal(it, now) }
        checkWin()
        version++
    }

    fun tick(deltaMs: Long) {
        if (status != GameStatus.PLAYING || !started) return
        if (hasLimit) {
            remainingMs -= deltaMs
            if (remainingMs <= 0L) {
                remainingMs = 0L
                status = GameStatus.LOST
                explodedIndex = -1
                revealAllMines()
            }
        } else {
            elapsedMs += deltaMs
        }
    }

    /** Counter shown by the LED display: zero padded, negative values get a minus. */
    fun counterText(): String {
        val v = counterValue
        return if (v < 0) "-" + (-v).coerceAtMost(99).toString().padStart(2, '0')
        else v.coerceAtMost(999).toString().padStart(3, '0')
    }

    fun timerSeconds(): Int =
        if (hasLimit) ((remainingMs + 999L) / 1000L).toInt() else (elapsedMs / 1000L).toInt()

    fun timerText(): String {
        val total = timerSeconds().coerceAtLeast(0)
        return (total / 60).toString().padStart(2, '0') + ":" + (total % 60).toString().padStart(2, '0')
    }
}

fun newGame(preset: Preset, custom: CustomConfig): Game = when (preset) {
    Preset.CUSTOM -> {
        val cfg = custom.clamped()
        Game(cfg.size, cfg.size, cfg.mines, cfg.fog, cfg.safeFirstTap, null)
    }
    else -> Game(preset.cols, preset.rows, preset.mines, preset.fog, false, preset.limitSeconds)
}
