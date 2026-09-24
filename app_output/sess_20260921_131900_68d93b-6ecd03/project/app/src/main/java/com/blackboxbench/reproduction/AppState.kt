package com.blackboxbench.reproduction

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

enum class CellState { EMPTY, FILLED, CROSS }

/** Board snapshot kept per puzzle so progress survives leaving the screen and app restarts. */
data class SavedGame(
    val cells: List<CellState>,
    val moves: Int,
    val elapsedMs: Long
)

/**
 * Single source of truth for the settings, campaign progress and in-flight boards.
 * Everything is written to SharedPreferences so it survives a process restart.
 */
class AppState(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("nonogram_app_state", Context.MODE_PRIVATE)

    var hapticFeedback by mutableStateOf(prefs.getBoolean(KEY_HAPTIC, true))
        private set
    var longPressToCross by mutableStateOf(prefs.getBoolean(KEY_LONG_PRESS, true))
        private set
    var cycleMode by mutableStateOf(prefs.getBoolean(KEY_CYCLE, false))
        private set

    var completedLevels by mutableStateOf(readIntSet(KEY_COMPLETED))
        private set
    var currentLevel by mutableStateOf(prefs.getInt(KEY_CURRENT_LEVEL, 1))
        private set

    var roomDifficulty by mutableStateOf(
        Difficulty.fromToken(prefs.getString(KEY_ROOM_DIFFICULTY, null) ?: "") ?: Difficulty.EASY
    )
        private set
    var roomCode by mutableStateOf(prefs.getString(KEY_ROOM_CODE, null) ?: randomRoomCode())
        private set

    private var boards: MutableMap<String, SavedGame> = readBoards()

    val highestUnlocked: Int
        get() = ((completedLevels.maxOrNull() ?: 0) + 1).coerceIn(1, Levels.count)

    fun updateHaptic(value: Boolean) {
        hapticFeedback = value
        prefs.edit().putBoolean(KEY_HAPTIC, value).apply()
    }

    fun updateLongPressToCross(value: Boolean) {
        longPressToCross = value
        prefs.edit().putBoolean(KEY_LONG_PRESS, value).apply()
    }

    fun updateCycleMode(value: Boolean) {
        cycleMode = value
        prefs.edit().putBoolean(KEY_CYCLE, value).apply()
    }

    fun updateRoomDifficulty(difficulty: Difficulty) {
        roomDifficulty = difficulty
        prefs.edit().putString(KEY_ROOM_DIFFICULTY, difficulty.name).apply()
    }

    fun updateRoomCode(code: String) {
        roomCode = code
        prefs.edit().putString(KEY_ROOM_CODE, code).apply()
    }

    fun updateCurrentLevel(level: Int) {
        currentLevel = level.coerceIn(1, Levels.count)
        prefs.edit().putInt(KEY_CURRENT_LEVEL, currentLevel).apply()
    }

    fun markCompleted(level: Int) {
        if (level in 1..Levels.count) {
            completedLevels = completedLevels + level
            writeIntSet(KEY_COMPLETED, completedLevels)
        }
    }

    fun resetProgress() {
        completedLevels = emptySet()
        writeIntSet(KEY_COMPLETED, emptySet())
        updateCurrentLevel(1)
        boards = mutableMapOf()
        prefs.edit().remove(KEY_BOARDS).apply()
    }

    fun board(key: String): SavedGame? = boards[key]

    fun saveBoard(key: String, board: SavedGame) {
        boards[key] = board
        writeBoards()
    }

    fun clearBoard(key: String) {
        boards.remove(key)
        writeBoards()
    }

    // ---------------------------------------------------------------- storage

    private fun readIntSet(key: String): Set<Int> {
        val raw = prefs.getString(key, null) ?: return emptySet()
        if (raw.isBlank()) return emptySet()
        return raw.split(',').mapNotNull { it.trim().toIntOrNull() }.toSet()
    }

    private fun writeIntSet(key: String, value: Set<Int>) {
        prefs.edit().putString(key, value.sorted().joinToString(",")).apply()
    }

    private fun readBoards(): MutableMap<String, SavedGame> {
        val raw = prefs.getString(KEY_BOARDS, null) ?: return mutableMapOf()
        return try {
            val json = JSONObject(raw)
            val result = mutableMapOf<String, SavedGame>()
            json.keys().forEach { key ->
                val obj = json.getJSONObject(key)
                val cellsJson = obj.getJSONArray("cells")
                val cells = (0 until cellsJson.length()).map { i ->
                    CellState.entries.getOrElse(cellsJson.getInt(i)) { CellState.EMPTY }
                }
                result[key] = SavedGame(cells, obj.optInt("moves", 0), obj.optLong("elapsed", 0L))
            }
            result
        } catch (_: Exception) {
            mutableMapOf()
        }
    }

    private fun writeBoards() {
        val json = JSONObject()
        boards.forEach { (key, board) ->
            val obj = JSONObject()
            obj.put("cells", JSONArray(board.cells.map { it.ordinal }))
            obj.put("moves", board.moves)
            obj.put("elapsed", board.elapsedMs)
            json.put(key, obj)
        }
        prefs.edit().putString(KEY_BOARDS, json.toString()).apply()
    }

    companion object {
        private const val KEY_HAPTIC = "haptic"
        private const val KEY_LONG_PRESS = "long_press_cross"
        private const val KEY_CYCLE = "cycle_mode"
        private const val KEY_COMPLETED = "completed_levels"
        private const val KEY_CURRENT_LEVEL = "current_level"
        private const val KEY_BOARDS = "boards"
        private const val KEY_ROOM_DIFFICULTY = "room_difficulty"
        private const val KEY_ROOM_CODE = "room_code"

        fun randomRoomCode(): String = (100000..999999).random().toString()
    }
}

/** Board key namespaces: campaign levels, random puzzles and multiplayer rooms. */
object BoardKeys {
    fun level(level: Int) = "level-$level"
    fun random(difficulty: Difficulty, seed: String) = "random-${difficulty.name}-$seed"
    fun multiplayer(code: String) = "mp-${code.uppercase()}"
}
