package com.blackboxbench.reproduction

/** Difficulty presets observed in the target app. `timeLimit` is 0 for count-up boards. */
enum class Difficulty(val label: String, val size: Int, val mines: Int, val timeLimit: Int) {
    EASY("Easy", 9, 10, 0),
    MEDIUM("Medium", 12, 40, 300),
    HARD("Hard", 14, 50, 180),
    CUSTOM("Custom", 16, 40, 0)
}

enum class GameStatus { READY, PLAYING, WON, LOST }

data class CustomConfig(
    val size: Int = 16,
    val mines: Int = 40,
    val fog: Boolean = false,
    val safeFirstTap: Boolean = true
) {
    val maxMines: Int get() = if (safeFirstTap) (size * size - 9).coerceAtLeast(1) else size * size
}

data class Cell(
    val mine: Boolean = false,
    val revealed: Boolean = false,
    val flagged: Boolean = false,
    val adjacent: Int = 0,
    val exploded: Boolean = false
)

data class GameState(
    val difficulty: Difficulty = Difficulty.EASY,
    val size: Int = 9,
    val mineTotal: Int = 10,
    val timeLimit: Int = 0,
    val cells: List<Cell> = List(81) { Cell() },
    val status: GameStatus = GameStatus.READY,
    val fog: Boolean = false,
    val safeFirstTap: Boolean = true,
    val minesPlaced: Boolean = false,
    val seconds: Int = 0,
    val revealSeq: Int = 0
) {
    val flags: Int get() = cells.count { it.flagged }
    val counter: Int get() = mineTotal - flags
    fun cell(c: Int, r: Int): Cell = cells[r * size + c]
}

object GameLogic {

    fun newGame(difficulty: Difficulty, custom: CustomConfig): GameState {
        val size: Int
        val mines: Int
        val fog: Boolean
        val safe: Boolean
        val limit: Int
        when (difficulty) {
            Difficulty.EASY -> { size = 9; mines = 10; fog = false; safe = true; limit = 0 }
            Difficulty.MEDIUM -> { size = 12; mines = 40; fog = false; safe = true; limit = 300 }
            Difficulty.HARD -> { size = 14; mines = 50; fog = false; safe = true; limit = 180 }
            Difficulty.CUSTOM -> {
                size = custom.size
                mines = custom.mines.coerceIn(1, custom.maxMines)
                fog = custom.fog
                safe = custom.safeFirstTap
                limit = 0
            }
        }
        return GameState(
            difficulty = difficulty,
            size = size,
            mineTotal = mines,
            timeLimit = limit,
            cells = List(size * size) { Cell() },
            status = GameStatus.READY,
            fog = fog,
            safeFirstTap = safe,
            minesPlaced = false,
            seconds = limit
        )
    }

    private fun neighbors(size: Int, index: Int): List<Int> {
        val c = index % size
        val r = index / size
        val out = ArrayList<Int>(8)
        for (dr in -1..1) for (dc in -1..1) {
            if (dr == 0 && dc == 0) continue
            val nc = c + dc
            val nr = r + dr
            if (nc in 0 until size && nr in 0 until size) out.add(nr * size + nc)
        }
        return out
    }

    private fun placeMines(state: GameState, tapped: Int): GameState {
        val size = state.size
        val total = size * size
        val forbidden = HashSet<Int>()
        if (state.safeFirstTap) {
            forbidden.add(tapped)
            forbidden.addAll(neighbors(size, tapped))
        }
        val available = (0 until total).filter { it !in forbidden }
        val mineCount = state.mineTotal.coerceAtMost(available.size)
        val mineSet = available.shuffled().take(mineCount).toHashSet()
        val cells = state.cells.mapIndexed { i, c -> c.copy(mine = i in mineSet) }.toMutableList()
        for (i in 0 until total) {
            val adj = neighbors(size, i).count { cells[it].mine }
            cells[i] = cells[i].copy(adjacent = adj)
        }
        return state.copy(cells = cells, minesPlaced = true)
    }

    private fun floodReveal(cells: MutableList<Cell>, size: Int, start: Int) {
        val stack = ArrayDeque<Int>()
        stack.addLast(start)
        val seen = HashSet<Int>()
        while (stack.isNotEmpty()) {
            val i = stack.removeLast()
            if (!seen.add(i)) continue
            val cell = cells[i]
            if (cell.revealed || cell.flagged || cell.mine) continue
            cells[i] = cell.copy(revealed = true)
            if (cell.adjacent == 0) {
                for (n in neighbors(size, i)) {
                    if (!cells[n].revealed && !cells[n].flagged && !cells[n].mine) stack.addLast(n)
                }
            }
        }
    }

    private fun revealAllMines(cells: MutableList<Cell>, exploded: Int) {
        for (i in cells.indices) {
            val c = cells[i]
            if (c.mine) cells[i] = c.copy(revealed = true, exploded = i == exploded)
        }
    }

    private fun checkWin(state: GameState): GameState {
        val done = state.cells.all { it.mine || it.revealed }
        if (!done) return state
        val cells = state.cells.map { if (it.mine) it.copy(flagged = true) else it }.toMutableList()
        return state.copy(cells = cells, status = GameStatus.WON)
    }

    fun tap(state: GameState, index: Int): GameState {
        if (state.status == GameStatus.WON || state.status == GameStatus.LOST) return state
        if (state.cells[index].flagged) return state
        var s = if (!state.minesPlaced) placeMines(state, index) else state
        if (s.status == GameStatus.READY) s = s.copy(status = GameStatus.PLAYING, seconds = if (s.timeLimit > 0) s.timeLimit else 0)
        val cell = s.cells[index]
        val cells = s.cells.toMutableList()
        if (cell.revealed) {
            // Chording: reveal neighbours when the flag count matches the number.
            if (cell.adjacent > 0) {
                val ns = neighbors(s.size, index)
                val flagged = ns.count { cells[it].flagged }
                if (flagged == cell.adjacent) {
                    var lost = false
                    for (n in ns) {
                        val nc = cells[n]
                        if (nc.revealed || nc.flagged) continue
                        if (nc.mine) { lost = true; break }
                        floodReveal(cells, s.size, n)
                    }
                    if (lost) {
                        revealAllMines(cells, -1)
                        return s.copy(cells = cells, status = GameStatus.LOST, revealSeq = s.revealSeq + 1)
                    }
                }
            }
            return checkWin(s.copy(cells = cells, revealSeq = s.revealSeq + 1))
        }
        if (cell.mine) {
            revealAllMines(cells, index)
            return s.copy(cells = cells, status = GameStatus.LOST, revealSeq = s.revealSeq + 1)
        }
        floodReveal(cells, s.size, index)
        return checkWin(s.copy(cells = cells, revealSeq = s.revealSeq + 1))
    }

    fun longPress(state: GameState, index: Int): GameState {
        if (state.status == GameStatus.WON || state.status == GameStatus.LOST) return state
        val cell = state.cells[index]
        if (cell.revealed) return state
        val cells = state.cells.toMutableList()
        cells[index] = cell.copy(flagged = !cell.flagged)
        return state.copy(cells = cells)
    }

    /** Advances the clock by one second. */
    fun tick(state: GameState): GameState {
        if (state.status != GameStatus.PLAYING) return state
        return if (state.timeLimit > 0) {
            val next = state.seconds - 1
            if (next <= 0) {
                val cells = state.cells.toMutableList()
                revealAllMines(cells, -1)
                state.copy(cells = cells, seconds = 0, status = GameStatus.LOST)
            } else {
                state.copy(seconds = next)
            }
        } else {
            state.copy(seconds = state.seconds + 1)
        }
    }
}
