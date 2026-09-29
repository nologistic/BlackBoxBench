package com.blackboxbench.reproduction

import kotlin.random.Random

enum class Difficulty(val label: String, val cols: Int, val rows: Int, val mines: Int) {
    EASY("Easy", 9, 9, 10),
    MEDIUM("Medium", 12, 12, 40),
    HARD("Hard", 12, 16, 99),
    CUSTOM("Custom", 8, 10, 15);

    companion object {
        fun fromName(name: String?): Difficulty =
            entries.firstOrNull { it.name == name } ?: EASY
    }
}

enum class GameStatus { IDLE, PLAYING, WON, LOST }

class Cell {
    var isMine: Boolean = false
    var revealed: Boolean = false
    var flagged: Boolean = false
    var adjacent: Int = 0
}

class MinesweeperGame(val difficulty: Difficulty, seed: Int = Random.nextInt()) {
    val cols: Int = difficulty.cols
    val rows: Int = difficulty.rows
    val minesTotal: Int = difficulty.mines
    val board: Array<Array<Cell>> = Array(rows) { Array(cols) { Cell() } }
    var status: GameStatus = GameStatus.IDLE
        private set
    var revealedCount: Int = 0
        private set

    private val random = Random(seed)

    val flagsPlaced: Int
        get() = board.flatten().count { it.flagged }

    val minesRemaining: Int
        get() = minesTotal - flagsPlaced

    val isOver: Boolean
        get() = status == GameStatus.WON || status == GameStatus.LOST

    fun inBounds(r: Int, c: Int): Boolean = r in 0 until rows && c in 0 until cols

    fun reveal(r: Int, c: Int) {
        if (isOver || !inBounds(r, c)) return
        val cell = board[r][c]
        if (cell.revealed || cell.flagged) return
        if (status == GameStatus.IDLE) {
            placeMines(r, c)
            status = GameStatus.PLAYING
        }
        if (cell.isMine) {
            cell.revealed = true
            revealAllMines()
            status = GameStatus.LOST
            return
        }
        floodReveal(r, c)
        checkWin()
    }

    fun toggleFlag(r: Int, c: Int) {
        if (isOver || !inBounds(r, c)) return
        val cell = board[r][c]
        if (cell.revealed) return
        cell.flagged = !cell.flagged
    }

    private fun placeMines(safeR: Int, safeC: Int) {
        val forbidden = HashSet<Pair<Int, Int>>()
        for (dr in -1..1) {
            for (dc in -1..1) {
                val rr = safeR + dr
                val cc = safeC + dc
                if (inBounds(rr, cc)) forbidden += rr to cc
            }
        }
        var placed = 0
        while (placed < minesTotal) {
            val r = random.nextInt(rows)
            val c = random.nextInt(cols)
            val key = r to c
            if (key in forbidden) continue
            if (board[r][c].isMine) continue
            board[r][c].isMine = true
            placed++
        }
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                board[r][c].adjacent = neighborMines(r, c)
            }
        }
    }

    private fun neighborMines(r: Int, c: Int): Int {
        var count = 0
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val rr = r + dr
                val cc = c + dc
                if (inBounds(rr, cc) && board[rr][cc].isMine) count++
            }
        }
        return count
    }

    private fun floodReveal(startR: Int, startC: Int) {
        val stack = ArrayDeque<Pair<Int, Int>>()
        stack.addLast(startR to startC)
        while (stack.isNotEmpty()) {
            val (r, c) = stack.removeLast()
            if (!inBounds(r, c)) continue
            val cell = board[r][c]
            if (cell.revealed || cell.flagged || cell.isMine) continue
            cell.revealed = true
            revealedCount++
            if (cell.adjacent == 0) {
                for (dr in -1..1) {
                    for (dc in -1..1) {
                        if (dr == 0 && dc == 0) continue
                        stack.addLast((r + dr) to (c + dc))
                    }
                }
            }
        }
    }

    private fun revealAllMines() {
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val cell = board[r][c]
                if (cell.isMine && !cell.flagged) cell.revealed = true
            }
        }
    }

    private fun checkWin() {
        if (revealedCount == rows * cols - minesTotal) {
            status = GameStatus.WON
        }
    }
}
