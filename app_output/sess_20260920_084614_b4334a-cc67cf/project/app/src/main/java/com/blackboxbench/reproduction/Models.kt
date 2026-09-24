package com.blackboxbench.reproduction

import kotlin.random.Random

enum class AppPage { HOME, SELECT_LEVEL, HOW_TO, SETTINGS, MULTIPLAYER, GAME }
enum class GameOrigin { HOME, SELECT_LEVEL, MULTIPLAYER, RANDOM }

enum class Difficulty(val title: String, val gridSize: Int) {
    EASY("EASY", 5),
    MEDIUM("MEDIUM", 7),
    HARD("HARD", 8),
    MASTER("MASTER", 10),
    EXPERT("EXPERT", 12);

    companion object {
        fun fromCode(code: String): Difficulty? =
            entries.firstOrNull { code.uppercase().startsWith(it.title + "-") }
    }
}

data class Puzzle(
    val id: String,
    val title: String?,
    val size: Int,
    val solution: List<Boolean>
) {
    val rowClues: List<List<Int>> = (0 until size).map { row ->
        clues((0 until size).map { col -> solution[row * size + col] })
    }
    val columnClues: List<List<Int>> = (0 until size).map { col ->
        clues((0 until size).map { row -> solution[row * size + col] })
    }
}

private fun clues(line: List<Boolean>): List<Int> {
    val result = mutableListOf<Int>()
    var run = 0
    line.forEach { filled ->
        if (filled) run++ else if (run > 0) {
            result += run
            run = 0
        }
    }
    if (run > 0) result += run
    return if (result.isEmpty()) listOf(0) else result
}

fun levelPuzzle(level: Int): Puzzle {
    if (level == 1) {
        val rows = listOf(
            "11000",
            "11010",
            "01101",
            "11011",
            "11001"
        )
        return Puzzle("level_1", "LEVEL 1", 5, rows.flatMap { row -> row.map { it == '1' } })
    }
    if (level == 2) {
        val rows = listOf(
            "11111",
            "00011",
            "11111",
            "01010",
            "11011"
        )
        return Puzzle("level_2", "LEVEL 2", 5, rows.flatMap { row -> row.map { it == '1' } })
    }
    val size = when {
        level < 5 -> 5
        level < 8 -> 7
        else -> 8
    }
    val difficulty = Difficulty.entries.first { it.gridSize == size }
    return seededPuzzle(difficulty, level.toLong() * 7919L, "LEVEL $level", "level_$level")
}

fun seededPuzzle(
    difficulty: Difficulty,
    seed: Long,
    title: String? = null,
    id: String = "random_" + difficulty.title + "_" + seed
): Puzzle {
    val size = difficulty.gridSize
    val random = Random(seed)
    val values = MutableList(size * size) { random.nextFloat() < 0.46f }
    for (row in 0 until size) {
        if ((0 until size).none { values[row * size + it] }) {
            values[row * size + random.nextInt(size)] = true
        }
    }
    for (col in 0 until size) {
        if ((0 until size).none { values[it * size + col] }) {
            values[random.nextInt(size) * size + col] = true
        }
    }
    return Puzzle(id, title, size, values)
}

fun encodeBoard(board: List<Int>): String = board.joinToString(separator = "") { it.coerceIn(0, 2).toString() }

fun decodeBoard(value: String?, size: Int): List<Int> {
    if (value == null || value.length != size * size) return List(size * size) { 0 }
    return value.map { ch -> ch.digitToIntOrNull()?.coerceIn(0, 2) ?: 0 }
}

fun formatElapsed(totalSeconds: Int): String {
    val minutes = totalSeconds.coerceAtLeast(0) / 60
    val seconds = totalSeconds.coerceAtLeast(0) % 60
    return "%02d:%02d".format(minutes, seconds)
}
