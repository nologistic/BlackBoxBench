package com.blackboxbench.reproduction

import java.util.Collections
import java.util.Random

enum class Difficulty(val label: String, val kanji: String) {
    EASY("EASY", "\u6613"),
    MEDIUM("MEDIUM", "\u666e"),
    HARD("HARD", "\u96e3")
}

/**
 * A generated Futoshiki puzzle.
 *
 * hIneq[r][c] describes the horizontal relation between (r,c) and (r,c+1):
 *   0 = none, 1 = left < right, -1 = left > right.
 * vIneq[r][c] describes the vertical relation between (r,c) and (r+1,c):
 *   0 = none, 1 = top < bottom, -1 = top > bottom.
 */
class Puzzle(
    val size: Int,
    val givens: Array<IntArray>,
    val solution: Array<IntArray>,
    val hIneq: Array<IntArray>,
    val vIneq: Array<IntArray>
)

object Futoshiki {

    fun generate(size: Int, difficulty: Difficulty, rng: Random): Puzzle {
        val solution = generateLatin(size, rng)

        val hCandidates = ArrayList<Int>()
        for (r in 0 until size) for (c in 0 until size - 1) hCandidates.add(r * size + c)
        val vCandidates = ArrayList<Int>()
        for (r in 0 until size - 1) for (c in 0 until size) vCandidates.add(r * size + c)
        Collections.shuffle(hCandidates, rng)
        Collections.shuffle(vCandidates, rng)

        val ineqTarget = when (difficulty) {
            Difficulty.EASY -> size
            Difficulty.MEDIUM -> (size * 3) / 2
            Difficulty.HARD -> (size * 7) / 3
        }
        val hIneq = Array(size) { IntArray(size - 1) }
        val vIneq = Array(size - 1) { IntArray(size) }
        var made = 0
        var hi = 0
        var vi = 0
        while (made < ineqTarget && (hi < hCandidates.size || vi < vCandidates.size)) {
            val useH = if (hi >= hCandidates.size) false else if (vi >= vCandidates.size) true else rng.nextBoolean()
            if (useH) {
                val k = hCandidates[hi++]
                val r = k / size
                val c = k % size
                val a = solution[r][c]
                val b = solution[r][c + 1]
                hIneq[r][c] = if (a < b) 1 else -1
            } else {
                val k = vCandidates[vi++]
                val r = k / size
                val c = k % size
                val a = solution[r][c]
                val b = solution[r + 1][c]
                vIneq[r][c] = if (a < b) 1 else -1
            }
            made++
        }

        val givens = Array(size) { IntArray(size) }
        val order = ArrayList<Int>()
        for (i in 0 until size * size) order.add(i)
        Collections.shuffle(order, rng)
        for (idx in order) {
            val r = idx / size
            val c = idx % size
            givens[r][c] = solution[r][c]
            if (countSolutions(givens, hIneq, vIneq, size, 2) == 1) break
        }

        val target = when (difficulty) {
            Difficulty.EASY -> (size * size * 55) / 100
            Difficulty.MEDIUM -> (size * size * 38) / 100
            Difficulty.HARD -> (size * size * 30) / 100
        }
        var givenCount = givens.sumOf { row -> row.count { it != 0 } }
        val remaining = ArrayList<Int>()
        for (i in 0 until size * size) if (givens[i / size][i % size] == 0) remaining.add(i)
        Collections.shuffle(remaining, rng)
        for (idx in remaining) {
            if (givenCount >= target) break
            givens[idx / size][idx % size] = solution[idx / size][idx % size]
            givenCount++
        }
        return Puzzle(size, givens, solution, hIneq, vIneq)
    }

    private fun generateLatin(size: Int, rng: Random): Array<IntArray> {
        val g = Array(size) { IntArray(size) }
        fun ok(r: Int, c: Int, v: Int): Boolean {
            for (i in 0 until size) if (g[r][i] == v || g[i][c] == v) return false
            return true
        }
        fun fill(pos: Int): Boolean {
            if (pos == size * size) return true
            val r = pos / size
            val c = pos % size
            val vals = ArrayList<Int>()
            for (v in 1..size) vals.add(v)
            Collections.shuffle(vals, rng)
            for (v in vals) {
                if (ok(r, c, v)) {
                    g[r][c] = v
                    if (fill(pos + 1)) return true
                    g[r][c] = 0
                }
            }
            return false
        }
        fill(0)
        return g
    }

    fun countSolutions(
        givens: Array<IntArray>,
        hIneq: Array<IntArray>,
        vIneq: Array<IntArray>,
        size: Int,
        limit: Int
    ): Int {
        val grid = Array(size) { givens[it].clone() }
        var count = 0
        fun ok(r: Int, c: Int, v: Int): Boolean {
            for (i in 0 until size) if (i != c && grid[r][i] == v) return false
            for (i in 0 until size) if (i != r && grid[i][c] == v) return false
            if (c > 0 && grid[r][c - 1] != 0) {
                when (hIneq[r][c - 1]) {
                    1 -> if (grid[r][c - 1] >= v) return false
                    -1 -> if (grid[r][c - 1] <= v) return false
                }
            }
            if (c < size - 1 && grid[r][c + 1] != 0) {
                when (hIneq[r][c]) {
                    1 -> if (v >= grid[r][c + 1]) return false
                    -1 -> if (v <= grid[r][c + 1]) return false
                }
            }
            if (r > 0 && grid[r - 1][c] != 0) {
                when (vIneq[r - 1][c]) {
                    1 -> if (grid[r - 1][c] >= v) return false
                    -1 -> if (grid[r - 1][c] <= v) return false
                }
            }
            if (r < size - 1 && grid[r + 1][c] != 0) {
                when (vIneq[r][c]) {
                    1 -> if (v >= grid[r + 1][c]) return false
                    -1 -> if (v <= grid[r + 1][c]) return false
                }
            }
            return true
        }
        fun bt() {
            if (count >= limit) return
            var br = -1
            var bc = -1
            loop@ for (r in 0 until size) for (c in 0 until size) if (grid[r][c] == 0) {
                br = r; bc = c; break@loop
            }
            if (br == -1) {
                count++
                return
            }
            for (v in 1..size) {
                if (ok(br, bc, v)) {
                    grid[br][bc] = v
                    bt()
                    grid[br][bc] = 0
                    if (count >= limit) return
                }
            }
        }
        bt()
        return count
    }

    fun conflicts(grid: Array<IntArray>, puzzle: Puzzle): Array<BooleanArray> {
        val size = puzzle.size
        val bad = Array(size) { BooleanArray(size) }
        for (r in 0 until size) for (c in 0 until size) {
            val v = grid[r][c]
            if (v == 0) continue
            for (i in 0 until size) {
                if (i != c && grid[r][i] == v) {
                    bad[r][c] = true; bad[r][i] = true
                }
                if (i != r && grid[i][c] == v) {
                    bad[r][c] = true; bad[i][c] = true
                }
            }
        }
        for (r in 0 until size) for (c in 0 until size - 1) {
            val a = grid[r][c]
            val b = grid[r][c + 1]
            val s = puzzle.hIneq[r][c]
            if (a != 0 && b != 0 && s != 0 && ((s == 1 && a > b) || (s == -1 && a < b))) {
                bad[r][c] = true; bad[r][c + 1] = true
            }
        }
        for (r in 0 until size - 1) for (c in 0 until size) {
            val a = grid[r][c]
            val b = grid[r + 1][c]
            val s = puzzle.vIneq[r][c]
            if (a != 0 && b != 0 && s != 0 && ((s == 1 && a > b) || (s == -1 && a < b))) {
                bad[r][c] = true; bad[r + 1][c] = true
            }
        }
        return bad
    }

    fun isSolved(grid: Array<IntArray>, puzzle: Puzzle): Boolean {
        for (r in 0 until puzzle.size) for (c in 0 until puzzle.size) if (grid[r][c] == 0) return false
        val bad = conflicts(grid, puzzle)
        for (r in 0 until puzzle.size) for (c in 0 until puzzle.size) if (bad[r][c]) return false
        return true
    }
}
