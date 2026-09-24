package com.blackboxbench.reproduction

/** A nonogram: solution bitmap plus the row/column clues derived from it. */
class Puzzle(val rows: Int, val cols: Int, val filled: Set<Int>) {

    val rowClues: List<List<Int>> = (0 until rows).map { r ->
        cluesOf((0 until cols).map { c -> filled.contains(r * cols + c) })
    }

    val colClues: List<List<Int>> = (0 until cols).map { c ->
        cluesOf((0 until rows).map { r -> filled.contains(r * cols + c) })
    }

    fun isFilled(row: Int, col: Int): Boolean = filled.contains(row * cols + col)

    val maxRowClues: Int get() = rowClues.maxOf { it.size }
    val maxColClues: Int get() = colClues.maxOf { it.size }

    companion object {
        fun cluesOf(line: List<Boolean>): List<Int> {
            val out = mutableListOf<Int>()
            var run = 0
            for (cell in line) {
                if (cell) run++ else if (run > 0) { out.add(run); run = 0 }
            }
            if (run > 0) out.add(run)
            return if (out.isEmpty()) listOf(0) else out
        }

        fun fromArt(art: List<String>): Puzzle {
            val rows = art.size
            val cols = art.maxOf { it.length }
            val filled = mutableSetOf<Int>()
            art.forEachIndexed { r, line ->
                line.forEachIndexed { c, ch -> if (ch == 'X' || ch == '#') filled.add(r * cols + c) }
            }
            return Puzzle(rows, cols, filled)
        }
    }
}

enum class Difficulty(val label: String, val size: Int) {
    EASY("Easy", 5),
    MEDIUM("Medium", 6),
    HARD("Hard", 8),
    MASTER("Master", 10),
    EXPERT("Expert", 12);

    val upper: String get() = name

    companion object {
        fun fromToken(token: String): Difficulty? =
            entries.firstOrNull { it.name.equals(token.trim(), ignoreCase = true) }
    }
}

/** Static campaign levels; the first two match the ones played in the target app. */
object Levels {

    private val ART: List<List<String>> = listOf(
        // 1 - the observed opening puzzle
        listOf(
            "XX...",
            "XX.X.",
            ".XX.X",
            "XX.XX",
            "XX..X"
        ),
        // 2 - the observed second puzzle
        listOf(
            "XXXXX",
            "...XX",
            "XXXXX",
            ".X.X.",
            "XX.XX"
        ),
        // 3 - house
        listOf(
            "..X..",
            ".XXX.",
            "XXXXX",
            "X.X.X",
            "X.X.X"
        ),
        // 4 - heart
        listOf(
            ".X.X.",
            "XXXXX",
            "XXXXX",
            ".XXX.",
            "..X.."
        ),
        // 5 - cat
        listOf(
            "X....X",
            "XX..XX",
            "XXXXXX",
            "X.XX.X",
            "XXXXXX",
            ".X..X."
        ),
        // 6 - tree
        listOf(
            "..XX..",
            ".XXXX.",
            "XXXXXX",
            "..XX..",
            "..XX..",
            "XXXXXX"
        ),
        // 7 - face
        listOf(
            ".XXXXX.",
            "X.....X",
            "X.X.X.X",
            "X.....X",
            "X.X.X.X",
            "XX...XX",
            ".XXXXX."
        ),
        // 8 - sail boat
        listOf(
            "...X...",
            "...XX..",
            "...XXX.",
            "...XXXX",
            "XXXXXXX",
            ".......",
            ".XXXXX."
        ),
        // 9 - invader
        listOf(
            "..X..X..",
            "...XX...",
            "..XXXX..",
            ".XX..XX.",
            "XXXXXXXX",
            "X.XXXX.X",
            "X.X..X.X",
            "..XX.XX."
        ),
        // 10 - umbrella
        listOf(
            "...XX...",
            "..XXXX..",
            ".XXXXXX.",
            "XXXXXXXX",
            "...XX...",
            "...XX...",
            "...XX...",
            "...XX..."
        ),
        // 11 - camera
        listOf(
            ".........",
            "..XXXXX..",
            ".XXXXXXX.",
            "XX.XXX.XX",
            "XXXXXXXXX",
            "XX.XXX.XX",
            "XXXXXXXXX",
            ".XXXXXXX.",
            "........."
        ),
        // 12 - skyline
        listOf(
            "....X.....",
            "....X..X..",
            ".X..X..X..",
            ".X..X.XX..",
            "XX.XX.XX..",
            "XX.XX.XX.X",
            "XXXXXXXXXX",
            "XXXXXXXXXX",
            "XXXXXXXXXX",
            "XXXXXXXXXX"
        )
    )

    val count: Int get() = ART.size

    fun puzzle(level: Int): Puzzle {
        val index = (level - 1).coerceIn(0, ART.size - 1)
        return Puzzle.fromArt(ART[index])
    }
}

/** Deterministic art libraries used by the random and multiplayer puzzles. */
object PuzzleGenerator {

    private val LIB_5 = listOf(
        listOf("..X..", ".XXX.", "XXXXX", "X.X.X", ".X.X."),
        listOf("XX.XX", "XXXXX", ".XXX.", "..X..", "..X.."),
        listOf(".XXX.", "X...X", "X...X", "X...X", ".XXX."),
        listOf("X...X", "XX.XX", "X.X.X", "X...X", "X...X"),
        listOf("..X..", ".XXX.", "XXXXX", "..X..", ".XXX."),
        listOf("XX...", ".XX..", "..XX.", "...XX", "....X"),
        listOf("X.X.X", "XXXXX", "XXXXX", ".XXX.", "..X.."),
        listOf(".XXX.", "XX.XX", "XXXXX", ".X.X.", ".X.X.")
    )

    private val LIB_6 = listOf(
        listOf("..XX..", ".XXXX.", "XXXXXX", "X.XX.X", "XXXXXX", ".X..X."),
        listOf(".XXXX.", "X....X", "X.XX.X", "X....X", "X....X", ".XXXX."),
        listOf("XX..XX", "XXXXXX", ".XXXX.", "..XX..", "..XX..", ".XXXX."),
        listOf("...X..", "..XX..", ".XXX..", "XXXXX.", "..XX..", "XXXXXX"),
        listOf("X....X", "XX..XX", "XXXXXX", "X.XX.X", "X....X", ".XXXX."),
        listOf("XXXXXX", "X....X", "X.XX.X", "X.XX.X", "X....X", "XXXXXX"),
        listOf("..X...", ".XX...", "XXXX..", "..XX..", "..XXX.", "..XXXX"),
        listOf(".X..X.", "XXXXXX", "XXXXXX", ".XXXX.", "..XX..", ".X..X.")
    )

    private val LIB_8 = listOf(
        listOf(
            "..X..X..", "...XX...", "..XXXX..", ".XX..XX.", "XXXXXXXX",
            "X.XXXX.X", "X.X..X.X", "..XX.XX."
        ),
        listOf(
            "...XX...", "..XXXX..", ".XXXXXX.", "XXXXXXXX", "...XX...",
            "...XX...", "...XX...", "...XX..."
        ),
        listOf(
            "..XXXX..", ".X....X.", "X..XX..X", "X.XXXX.X", "X.XXXX.X",
            "X..XX..X", ".X....X.", "..XXXX.."
        ),
        listOf(
            "X......X", "XX....XX", "XXXXXXXX", "XX.XX.XX", "XXXXXXXX",
            ".XXXXXX.", "..X..X..", ".XX..XX."
        ),
        listOf(
            "....X...", "...XX...", "..XXX...", ".XXXX...", "XXXXX...",
            "...XX...", "...XX...", "XXXXXXXX"
        ),
        listOf(
            "..XXXX..", ".XXXXXX.", "XXXXXXXX", "XX.XX.XX", "XXXXXXXX",
            "X.XXXX.X", "XX.XX.XX", ".X....X."
        ),
        listOf(
            "XX....XX", "XXX..XXX", "XXXXXXXX", "XXXXXXXX", "XX.XX.XX",
            "XXXXXXXX", ".XXXXXX.", "..XXXX.."
        ),
        listOf(
            "...XX...", "..XXXX..", ".XXXXXX.", "XXXXXXXX", "XXXXXXXX",
            "...XX...", "..XXXX..", ".XXXXXX."
        )
    )

    private val LIB_10 = listOf(
        listOf(
            "....X.....", "....X..X..", ".X..X..X..", ".X..X.XX..", "XX.XX.XX..",
            "XX.XX.XX.X", "XXXXXXXXXX", "XXXXXXXXXX", "XXXXXXXXXX", "XXXXXXXXXX"
        ),
        listOf(
            "....XX....", "...XXXX...", "..XXXXXX..", ".XXXXXXXX.", "XXXXXXXXXX",
            "....XX....", "....XX....", "....XX....", "....XX....", "...XXXX..."
        ),
        listOf(
            "..XXXXXX..", ".X......X.", "X..X..X..X", "X........X", "X..XXXX..X",
            "X..XXXX..X", "X........X", "X.X....X.X", ".X......X.", "..XXXXXX.."
        ),
        listOf(
            "...XXXX...", "..XXXXXX..", ".XXXXXXXX.", "XXXXXXXXXX", "XX.XXXX.XX",
            "XXXXXXXXXX", "XX.XXXX.XX", ".XXXXXXXX.", "..XXXXXX..", "...XXXX..."
        ),
        listOf(
            "X........X", "XX......XX", "XXX....XXX", "XXXX..XXXX", "XXXXXXXXXX",
            "XXXXXXXXXX", ".XXXXXXXX.", "..XXXXXX..", "...XXXX...", "....XX...."
        ),
        listOf(
            "..XX..XX..", ".XXXXXXXX.", "XXXXXXXXXX", "XX.XXXX.XX", "XXXXXXXXXX",
            "XXXXXXXXXX", ".XXXXXXXX.", "..XXXXXX..", "...X..X...", "..XX..XX.."
        ),
        listOf(
            ".........X", "........XX", ".......XXX", "......XXXX", ".....XXXXX",
            "....XXXXXX", "...XXXXXXX", "..XXXXXXXX", ".XXXXXXXXX", "XXXXXXXXXX"
        ),
        listOf(
            "XXXX..XXXX", "XXXX..XXXX", "XXXXXXXXXX", "XXXXXXXXXX", "...XXXX...",
            "...XXXX...", "..XXXXXX..", ".XXXXXXXX.", "XXXXXXXXXX", "XXXXXXXXXX"
        )
    )

    private val LIB_12 = listOf(
        listOf(
            "....XX......", "...XXXX.....", "..XXXXXX....", ".XXXXXXXX...",
            "XXXXXXXXXX..", "XXXXXXXXXXXX", "....XX......", "....XX......",
            "....XX......", "....XX......", "....XX......", "...XXXX....."
        ),
        listOf(
            "..XX....XX..", ".XXXX..XXXX.", "XXXXXXXXXXXX", "XXXXXXXXXXXX",
            "XX.XXXXXX.XX", "XXXXXXXXXXXX", "XXXXXXXXXXXX", ".XXXXXXXXXX.",
            "..XXXXXXXX..", "...XXXXXX...", "....XXXX....", ".....XX....."
        ),
        listOf(
            "X..........X", "XX........XX", "XXX......XXX", "XXXX....XXXX",
            "XXXXX..XXXXX", "XXXXXXXXXXXX", "XXXXXXXXXXXX", ".XXXXXXXXXX.",
            "..XXXXXXXX..", "...XXXXXX...", "....XXXX....", ".....XX....."
        ),
        listOf(
            "...XXXXXX...", "..XXXXXXXX..", ".XXXXXXXXXX.", "XXXXXXXXXXXX",
            "XX.XXXXXX.XX", "XXXXXXXXXXXX", "XX.XXXXXX.XX", "XXXXXXXXXXXX",
            ".XXXXXXXXXX.", "..XXXXXXXX..", "...XXXXXX...", "....XXXX...."
        ),
        listOf(
            "XX........XX", "XXX......XXX", "XXXX....XXXX", "XXXXX..XXXXX",
            "XXXXXXXXXXXX", "XXXXXXXXXXXX", "XXXXXXXXXXXX", "XXXXXXXXXXXX",
            ".XXXXXXXXXX.", "..XXXXXXXX..", "...XXXXXX...", "....XXXX...."
        ),
        listOf(
            "....XXXX....", "...XXXXXX...", "..XXXXXXXX..", ".XXXXXXXXXX.",
            "XXXXXXXXXXXX", "XX.XXXXXX.XX", "XX.XXXXXX.XX", "XXXXXXXXXXXX",
            ".XXXXXXXXXX.", "..XXXXXXXX..", "...XX..XX...", "..XXX..XXX.."
        ),
        listOf(
            ".....XX.....", "....XXXX....", "...XXXXXX...", "..XXXXXXXX..",
            ".XXXXXXXXXX.", "XXXXXXXXXXXX", "XXXXXXXXXXXX", "XXXXXXXXXXXX",
            "....XXXX....", "....XXXX....", "....XXXX....", "...XXXXXX..."
        ),
        listOf(
            "XXXXXXXXXXXX", "X..........X", "X.XXXXXXXX.X", "X.X......X.X",
            "X.X.XXXX.X.X", "X.X.X..X.X.X", "X.X.X..X.X.X", "X.X.XXXX.X.X",
            "X.X......X.X", "X.XXXXXXXX.X", "X..........X", "XXXXXXXXXXXX"
        )
    )

    fun library(size: Int): List<List<String>> = when (size) {
        5 -> LIB_5
        6 -> LIB_6
        8 -> LIB_8
        10 -> LIB_10
        else -> LIB_12
    }

    /** Same difficulty + code always resolves to the same puzzle. */
    fun generate(difficulty: Difficulty, code: String): Puzzle {
        val lib = library(difficulty.size)
        var h = code.lowercase().hashCode() * 31 + difficulty.ordinal * 7919
        h = h xor (h ushr 15)
        val artIndex = ((h % lib.size) + lib.size) % lib.size
        val variant = (((h / lib.size) % 8) + 8) % 8
        return Puzzle.fromArt(transform(lib[artIndex], variant))
    }

    private fun transform(art: List<String>, variant: Int): List<String> {
        var grid = art
        if (variant and 1 != 0) grid = grid.map { it.reversed() }
        if (variant and 2 != 0) grid = grid.reversed()
        if (variant and 4 != 0) {
            val rows = grid.size
            val cols = grid[0].length
            grid = (0 until cols).map { c -> (0 until rows).map { r -> grid[r][c] }.joinToString("") }
        }
        return grid
    }
}
