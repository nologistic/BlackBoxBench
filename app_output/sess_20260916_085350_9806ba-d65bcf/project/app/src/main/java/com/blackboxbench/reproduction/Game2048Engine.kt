package com.blackboxbench.reproduction

import kotlin.random.Random

enum class Direction { LEFT, RIGHT, UP, DOWN }

class Snapshot(val grid: IntArray, val score: Int, val moves: Int) {
    fun copy(): Snapshot = Snapshot(grid.copyOf(), score, moves)

    override fun equals(other: Any?): Boolean {
        if (other !is Snapshot) return false
        return score == other.score && moves == other.moves && grid.contentEquals(other.grid)
    }

    override fun hashCode(): Int {
        var h = score
        h = h * 31 + moves
        h = h * 31 + grid.contentHashCode()
        return h
    }
}

/** Deterministic 2048 board engine. Starts with a single tile like the target app. */
class Game2048Engine(val size: Int, private val random: Random = Random.Default) {

    var grid: IntArray = IntArray(size * size)
        private set

    var score: Int = 0
        private set

    var moves: Int = 0
        private set

    val undoStack = ArrayDeque<Snapshot>()
    val redoStack = ArrayDeque<Snapshot>()

    init {
        spawnTile()
    }

    fun cell(row: Int, col: Int): Int = grid[row * size + col]

    fun hasEmpty(): Boolean = grid.any { it == 0 }

    fun emptyCount(): Int = grid.count { it == 0 }

    fun maxTile(): Int = grid.maxOrNull() ?: 0

    fun canMove(): Boolean {
        if (grid.any { it == 0 }) return true
        for (r in 0 until size) {
            for (c in 0 until size) {
                val v = cell(r, c)
                if (c + 1 < size && cell(r, c + 1) == v) return true
                if (r + 1 < size && cell(r + 1, c) == v) return true
            }
        }
        return false
    }

    fun isGameOver(): Boolean = !canMove()

    fun snapshot(): Snapshot = Snapshot(grid.copyOf(), score, moves)

    fun load(s: Snapshot) {
        grid = s.grid.copyOf()
        score = s.score
        moves = s.moves
    }

    fun reset() {
        grid = IntArray(size * size)
        score = 0
        moves = 0
        undoStack.clear()
        redoStack.clear()
        spawnTile()
    }

    private fun spawnTile() {
        val empties = ArrayList<Int>(grid.size)
        for (i in grid.indices) if (grid[i] == 0) empties.add(i)
        if (empties.isEmpty()) return
        val index = empties[random.nextInt(empties.size)]
        grid[index] = if (random.nextInt(10) == 0) 4 else 2
    }

    fun swipe(direction: Direction): Boolean {
        val previous = Snapshot(grid.copyOf(), score, moves)
        val g = grid.copyOf()
        val n = size
        var gained = 0
        for (i in 0 until n) {
            val line = IntArray(n) { j ->
                when (direction) {
                    Direction.LEFT -> g[i * n + j]
                    Direction.RIGHT -> g[i * n + (n - 1 - j)]
                    Direction.UP -> g[j * n + i]
                    Direction.DOWN -> g[(n - 1 - j) * n + i]
                }
            }
            val merged = mergeLine(line)
            gained += merged.second
            for (j in 0 until n) {
                when (direction) {
                    Direction.LEFT -> g[i * n + j] = merged.first[j]
                    Direction.RIGHT -> g[i * n + (n - 1 - j)] = merged.first[j]
                    Direction.UP -> g[j * n + i] = merged.first[j]
                    Direction.DOWN -> g[(n - 1 - j) * n + i] = merged.first[j]
                }
            }
        }
        if (g.contentEquals(grid)) return false
        grid = g
        score += gained
        moves += 1
        undoStack.addLast(previous)
        redoStack.clear()
        spawnTile()
        return true
    }

    fun undo(): Boolean {
        val previous = undoStack.removeLastOrNull() ?: return false
        redoStack.addLast(snapshot())
        load(previous)
        return true
    }

    fun redo(): Boolean {
        val next = redoStack.removeLastOrNull() ?: return false
        undoStack.addLast(snapshot())
        load(next)
        return true
    }

    companion object {
        /** Compacts and merges a single line towards index 0. Returns (line, gainedScore). */
        fun mergeLine(line: IntArray): Pair<IntArray, Int> {
            val values = ArrayList<Int>(line.size)
            for (v in line) if (v != 0) values.add(v)
            val out = ArrayList<Int>(line.size)
            var gained = 0
            var i = 0
            while (i < values.size) {
                if (i + 1 < values.size && values[i] == values[i + 1]) {
                    val merged = values[i] * 2
                    out.add(merged)
                    gained += merged
                    i += 2
                } else {
                    out.add(values[i])
                    i += 1
                }
            }
            while (out.size < line.size) out.add(0)
            return out.toIntArray() to gained
        }
    }
}
