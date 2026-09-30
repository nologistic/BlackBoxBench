package com.blackboxbench.reproduction

/**
 * Nonogram puzzle model: solutions are strings of '.' (empty) and '#' (filled).
 */
object Puzzles {

    private val patterns: List<List<String>> = listOf(
        // 1 bird
        listOf(
            "..###.###",
            ".#######.",
            ".##.#####",
            "..###.##.",
            "....#....",
            "...###...",
            "..#####..",
            ".##...##.",
            "##.....##"
        ),
        // 2 heart
        listOf(
            ".##...##.",
            "#########",
            "#########",
            ".#######.",
            "..#####..",
            "...###...",
            "....#....",
            ".........",
            "........."
        ),
        // 3 cat
        listOf(
            ".#.....#.",
            ".##...##.",
            ".#######.",
            "###.#.###",
            "#########",
            ".#######.",
            ".##.#.##.",
            ".##...##.",
            ".#.....#."
        ),
        // 4 tree
        listOf(
            "....#....",
            "...###...",
            "..#####..",
            ".#######.",
            "..#####..",
            "...###...",
            "....#....",
            "....#....",
            "..#####.."
        ),
        // 5 house
        listOf(
            "....#....",
            "...###...",
            "..#####..",
            ".#######.",
            "#########",
            "##..#..##",
            "##..#..##",
            "##..#..##",
            "#########"
        ),
        // 6 umbrella
        listOf(
            "....#....",
            "..#####..",
            ".#######.",
            "#########",
            "....#....",
            "....#....",
            "....#....",
            "...#.....",
            "........."
        ),
        // 7 fish
        listOf(
            ".........",
            "...####..",
            "..######.",
            "#########",
            "#########",
            "..######.",
            "...####.#",
            ".........",
            "........."
        ),
        // 8 rocket
        listOf(
            "....#....",
            "...###...",
            "...###...",
            "..#####..",
            "..#.#.#..",
            "..#####..",
            ".###.###.",
            ".#.....#.",
            "........."
        ),
        // 9 cup
        listOf(
            ".........",
            ".#######.",
            ".#######.",
            ".#######.",
            ".#######.",
            "..#####..",
            "...###...",
            "..#####..",
            "........."
        ),
        // 10 smiley
        listOf(
            "..#####..",
            ".#######.",
            "##.###.##",
            "#########",
            "###...###",
            "##.....##",
            ".#######.",
            "..#####..",
            "........."
        ),
        // 11 diamond
        listOf(
            "....#....",
            "...###...",
            "..#####..",
            ".#######.",
            "###...###",
            "##.....##",
            ".#.....#.",
            "..#...#..",
            "...#.#..."
        ),
        // 12 mushroom
        listOf(
            "..#####..",
            ".#######.",
            "##.###.##",
            "#########",
            "#########",
            "...###...",
            "...###...",
            "...###...",
            "..#####.."
        )
    )

    fun levelCount(): Int = 24

    fun solutionFor(level: Int): List<String> {
        val p = patterns[(level - 1) % patterns.size]
        return if (level > patterns.size) p.map { it.reversed() } else p
    }

    fun sizeFor(level: Int): Int = 9

    fun randomSolution(): List<String> {
        val rows = List(9) { r ->
            val sb = StringBuilder()
            for (c in 0 until 9) {
                val centerBias = (Math.abs(4 - r) + Math.abs(4 - c)) <= 4
                val chance = if (centerBias) 0.55 else 0.18
                sb.append(if (Math.random() < chance) '#' else '.')
            }
            sb.toString()
        }
        return if (rows.sumOf { it.count { ch -> ch == '#' } } < 12) patterns[1] else rows
    }
}

data class Puzzle(val level: Int, val solution: List<String>, val size: Int) {
    val rowClues: List<List<Int>> = (solution.indices).map { clueOf(solution[it]) }
    val colClues: List<List<Int>> = (0 until size).map { c -> clueOf((0 until size).map { r -> solution[r][c] }) }
    val filledCount: Int = solution.sumOf { it.count { ch -> ch == '#' } }
}

private fun clueOf(line: List<Char>): List<Int> {
    val out = mutableListOf<Int>()
    var run = 0
    for (ch in line) {
        if (ch == '#') run++ else if (run > 0) { out.add(run); run = 0 }
    }
    if (run > 0) out.add(run)
    if (out.isEmpty()) out.add(0)
    return out
}

private fun clueOf(s: String): List<Int> = clueOf(s.toList())
