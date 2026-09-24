package com.blackboxbench.reproduction

import android.content.Context
import android.content.SharedPreferences

/** 游戏状态持久化：关卡进度、进行中棋盘、步数与模式。 */
data class GameState(
    val level: Int = 1,
    val fills: Set<Int> = emptySet(),
    val marks: Set<Int> = emptySet(),
    val moves: Int = 0,
    val crossMode: Boolean = false,
    val completed: Set<Int> = emptySet(),
    val elapsed: Int = 0,
    val won: Boolean = false
)

class GameStorage(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("nonogram_state", Context.MODE_PRIVATE)

    fun load(): GameState {
        val completed = prefs.getString("completed", "")
            ?.split(',')?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
        val level = prefs.getInt("current_level", 1).coerceIn(1, Levels.TOTAL)
        val n = Levels.sizeOf(level)
        val cells = prefs.getString("cells", "") ?: ""
        val fills = mutableSetOf<Int>()
        val marks = mutableSetOf<Int>()
        if (cells.length == n * n) {
            cells.forEachIndexed { idx, ch ->
                when (ch) {
                    '1' -> fills += idx
                    '2' -> marks += idx
                }
            }
        }
        return GameState(
            level = level,
            fills = fills,
            marks = marks,
            moves = prefs.getInt("moves", 0),
            crossMode = prefs.getBoolean("cross_mode", false),
            completed = completed
        )
    }

    fun save(state: GameState) {
        val n = Levels.sizeOf(state.level)
        val sb = StringBuilder()
        for (i in 0 until n * n) {
            sb.append(when {
                i in state.fills -> '1'
                i in state.marks -> '2'
                else -> '0'
            })
        }
        prefs.edit()
            .putString("completed", state.completed.joinToString(","))
            .putInt("current_level", state.level)
            .putString("cells", sb.toString())
            .putInt("moves", state.moves)
            .putBoolean("cross_mode", state.crossMode)
            .apply()
    }
}
