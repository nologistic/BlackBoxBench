package com.blackboxbench.reproduction

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.random.Random

/** Immutable description of a nonogram puzzle. */
class Puzzle(
    val rows: Int,
    val cols: Int,
    val solution: List<BooleanArray>,
    val given: List<Pair<Int, Int>> = emptyList()
) {
    val rowClues: List<List<Int>> = (0 until rows).map { r ->
        runs((0 until cols).map { c -> solution[r][c] })
    }
    val colClues: List<List<Int>> = (0 until cols).map { c ->
        runs((0 until rows).map { r -> solution[r][c] })
    }

    /** 0 = empty, 1 = filled, 2 = crossed */
    val initialMarks: IntArray = IntArray(rows * cols).also { m ->
        given.forEach { (r, c) -> m[r * cols + c] = 1 }
    }

    fun solutionAt(index: Int): Boolean {
        val r = index / cols
        val c = index % cols
        return solution[r][c]
    }

    companion object {
        fun runs(line: List<Boolean>): List<Int> {
            val out = ArrayList<Int>()
            var count = 0
            for (b in line) {
                if (b) count++ else if (count > 0) {
                    out.add(count); count = 0
                }
            }
            if (count > 0) out.add(count)
            return out
        }
    }
}

/** The ten built-in levels. Levels 1-3 were verified against the original app. */
object Levels {
    private val patterns: List<List<String>> = listOf(
        // 1
        listOf("11000", "11010", "01101", "11011", "11001"),
        // 2
        listOf("11111", "00011", "11111", "01010", "11011"),
        // 3
        listOf("00011", "11101", "01010", "10010", "01010"),
        // 4 - row clues [1,1] [2,1] [1,1,1] [1] [3]
        listOf("10100", "11010", "10101", "00100", "01110"),
        // 5
        listOf("11111", "10001", "10101", "10001", "11111"),
        // 6
        listOf("00100", "01110", "11011", "01110", "00100"),
        // 7
        listOf("11011", "11011", "00000", "11011", "11011"),
        // 8
        listOf("10101", "11111", "10101", "11111", "10101"),
        // 9
        listOf("01110", "10001", "10001", "10001", "01110"),
        // 10
        listOf("11011", "01010", "11011", "01010", "11011")
    )

    val puzzles: List<Puzzle> = patterns.map { p ->
        Puzzle(
            rows = p.size,
            cols = p[0].length,
            solution = p.map { row -> BooleanArray(row.length) { row[it] == '1' } }
        )
    }

    val count: Int get() = puzzles.size
}

class Difficulty(val key: String, val label: String, val rows: Int, val cols: Int)

val DIFFICULTIES = listOf(
    Difficulty("EASY", "EASY", 5, 5),
    Difficulty("MEDIUM", "MEDIUM", 7, 8),
    Difficulty("HARD", "HARD", 8, 9),
    Difficulty("MASTER", "MASTER", 12, 12),
    Difficulty("EXPERT", "EXPERT", 15, 15)
)

fun difficultyFor(key: String): Difficulty = DIFFICULTIES.firstOrNull { it.key == key } ?: DIFFICULTIES[0]

/** Deterministically generates a mirror-symmetric nonogram from a seed. */
fun generatePuzzle(rows: Int, cols: Int, seed: Long): Puzzle {
    val rnd = Random(seed)
    val sol = List(rows) { BooleanArray(cols) }
    for (r in 0 until rows) {
        for (c in 0 until (cols + 1) / 2) {
            val v = rnd.nextInt(100) < 58
            sol[r][c] = v
            sol[r][cols - 1 - c] = v
        }
    }
    if (sol.all { row -> row.none { it } }) {
        sol[0][0] = true
        sol[0][cols - 1] = true
    }
    val filled = ArrayList<Pair<Int, Int>>()
    for (r in 0 until rows) for (c in 0 until cols) if (sol[r][c]) filled.add(r to c)
    val given = if (filled.isEmpty()) emptyList() else listOf(filled[rnd.nextInt(filled.size)])
    return Puzzle(rows, cols, sol, given)
}

sealed interface Screen {
    data object Menu : Screen
    data object LevelSelect : Screen
    data object HowToPlay : Screen
    data object Settings : Screen
    data object Multiplayer : Screen
    data class Play(
        val puzzle: Puzzle,
        val title: String,
        val levelIndex: Int?
    ) : Screen
}

/** Small persisted application state. */
class AppState(private val prefs: SharedPreferences) {
    var screen by mutableStateOf<Screen>(Screen.Menu)
    var maxUnlocked by mutableIntStateOf(prefs.getInt("maxUnlocked", 1))
    var completed by mutableStateOf(readCompleted())
    var haptic by mutableStateOf(prefs.getBoolean("haptic", true))
    var longPressCross by mutableStateOf(prefs.getBoolean("longPressCross", true))
    var cycleMode by mutableStateOf(prefs.getBoolean("cycleMode", false))

    private fun readCompleted(): Set<Int> =
        prefs.getStringSet("completed", emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.toSet()

    fun updateHaptic(value: Boolean) {
        haptic = value
        prefs.edit().putBoolean("haptic", value).apply()
    }

    fun updateLongPressCross(value: Boolean) {
        longPressCross = value
        prefs.edit().putBoolean("longPressCross", value).apply()
    }

    fun updateCycleMode(value: Boolean) {
        cycleMode = value
        prefs.edit().putBoolean("cycleMode", value).apply()
    }

    fun completeLevel(level: Int) {
        completed = completed + level
        if (level + 1 > maxUnlocked) maxUnlocked = minOf(level + 1, Levels.count)
        prefs.edit()
            .putStringSet("completed", completed.map { it.toString() }.toSet())
            .putInt("maxUnlocked", maxUnlocked)
            .apply()
    }

    fun resetProgress() {
        completed = emptySet()
        maxUnlocked = 1
        prefs.edit().putStringSet("completed", emptySet()).putInt("maxUnlocked", 1).apply()
    }
}
