package com.blackboxbench.reproduction

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

enum class Status { PLAYING, WON, LOST }

data class DifficultyConfig(val cols: Int, val rows: Int, val mines: Int)

object Difficulties {
    val EASY = DifficultyConfig(9, 9, 10)
    val MEDIUM = DifficultyConfig(12, 12, 40)
    val HARD = DifficultyConfig(14, 14, 99)
}

/**
 * Game state the UI renders is backed by Compose snapshot state so that any
 * mutation recomposes the observers (HUD counters, board cells, dialogs).
 */
class GameModel {
    var difficultyKey: String by mutableStateOf("EASY")   // EASY|MEDIUM|HARD|CUSTOM
    var custom: DifficultyConfig by mutableStateOf(DifficultyConfig(9, 9, 10))
    var cols: Int by mutableStateOf(9)
    var rows: Int by mutableStateOf(9)
    var mineCount: Int by mutableStateOf(10)
    var mines = HashSet<Int>()
    var revealed: Set<Int> by mutableStateOf(emptySet())
    var flags: Set<Int> by mutableStateOf(emptySet())
    var exploded: Int by mutableStateOf(-1)
    var status: Status by mutableStateOf(Status.PLAYING)
    var started: Boolean by mutableStateOf(false)
    var elapsedSeconds: Int by mutableStateOf(0)

    val config: DifficultyConfig
        get() = when (difficultyKey) {
            "MEDIUM" -> Difficulties.MEDIUM
            "HARD" -> Difficulties.HARD
            "CUSTOM" -> custom
            else -> Difficulties.EASY
        }

    fun newGame(key: String, customCfg: DifficultyConfig? = null) {
        difficultyKey = key
        if (customCfg != null) custom = customCfg
        val cfg = config
        cols = cfg.cols; rows = cfg.rows; mineCount = cfg.mines
        mines.clear(); revealed = emptySet(); flags = emptySet()
        exploded = -1; status = Status.PLAYING; started = false; elapsedSeconds = 0
    }

    private fun idx(c: Int, r: Int) = r * cols + c

    fun neighbors(i: Int): List<Int> {
        val c = i % cols; val r = i / cols
        val out = ArrayList<Int>(8)
        for (dr in -1..1) for (dc in -1..1) {
            if (dr == 0 && dc == 0) continue
            val nr = r + dr; val nc = c + dc
            if (nr in 0 until rows && nc in 0 until cols) out.add(idx(nc, nr))
        }
        return out
    }

    fun adjacentMines(i: Int): Int = neighbors(i).count { it in mines }

    private fun placeMines(first: Int) {
        val total = cols * rows
        val excluded = HashSet<Int>()
        excluded.add(first)
        if (total - 9 >= mineCount) excluded.addAll(neighbors(first))
        val pool = (0 until total).filter { it !in excluded }.toMutableList()
        val n = minOf(mineCount, pool.size)
        for (k in 0 until n) {
            val j = k + Random.nextInt(pool.size - k)
            val tmp = pool[k]; pool[k] = pool[j]; pool[j] = tmp
            mines.add(pool[k])
        }
    }

    private fun floodReveal(start: Int) {
        val newly = LinkedHashSet<Int>()
        val stack = ArrayDeque<Int>()
        stack.addLast(start)
        while (stack.isNotEmpty()) {
            val i = stack.removeLast()
            if (i in revealed || i in newly || i in flags || i in mines) continue
            newly.add(i)
            if (adjacentMines(i) == 0) {
                for (n in neighbors(i)) if (n !in revealed && n !in newly) stack.addLast(n)
            }
        }
        revealed = revealed + newly
    }

    /** Returns true when the action mutated the board. */
    fun tap(i: Int): Boolean {
        if (status != Status.PLAYING) return false
        if (i in flags) return false
        if (i in revealed && adjacentMines(i) > 0) {
            // Chord on a number: open every unflagged neighbour. No
            // flag-count check - matches the observed app behaviour.
            val toOpen = neighbors(i).filter { it !in flags }
            for (n in toOpen) {
                if (n in mines) {
                    exploded = n; status = Status.LOST; return true
                }
            }
            var changed = false
            for (n in toOpen) if (n !in revealed) { floodReveal(n); changed = true }
            if (changed) checkWin()
            return changed
        }
        if (i in revealed) return false
        if (!started) {
            started = true
            placeMines(i)
        }
        if (i in mines) {
            exploded = i; status = Status.LOST
            return true
        }
        floodReveal(i)
        checkWin()
        return true
    }

    fun longPress(i: Int): Boolean {
        if (status != Status.PLAYING || i in revealed) return false
        flags = if (i in flags) flags - i else flags + i
        return true
    }

    private fun checkWin() {
        if (revealed.size >= cols * rows - mineCount) status = Status.WON
    }

    val flagCount: Int get() = flags.size
    val counter: Int get() = mineCount - flags.size
    val isCustom: Boolean get() = difficultyKey == "CUSTOM"

    fun persist(prefs: android.content.SharedPreferences) {
        val o = JSONObject()
        o.put("k", difficultyKey); o.put("cw", custom.cols); o.put("ch", custom.rows); o.put("cm", custom.mines)
        o.put("st", status.name); o.put("run", started); o.put("el", elapsedSeconds)
        o.put("mi", JSONArray(mines)); o.put("rv", JSONArray(revealed)); o.put("fl", JSONArray(flags)); o.put("ex", exploded)
        prefs.edit().putString("game", o.toString()).apply()
    }

    fun restore(prefs: android.content.SharedPreferences): Boolean {
        val s = prefs.getString("game", null) ?: return false
        return try {
            val o = JSONObject(s)
            difficultyKey = o.optString("k", "EASY")
            custom = DifficultyConfig(o.optInt("cw", 9), o.optInt("ch", 9), o.optInt("cm", 10))
            val cfg = config
            cols = cfg.cols; rows = cfg.rows; mineCount = cfg.mines
            status = try { Status.valueOf(o.optString("st", "PLAYING")) } catch (_: Exception) { Status.PLAYING }
            started = o.optBoolean("run", false)
            elapsedSeconds = o.optInt("el", 0)
            mines.clear()
            val rv = HashSet<Int>(); val fl = HashSet<Int>()
            fun arr(k: String) = o.optJSONArray(k)
            arr("mi")?.let { for (i in 0 until it.length()) mines.add(it.getInt(i)) }
            arr("rv")?.let { for (i in 0 until it.length()) rv.add(it.getInt(i)) }
            arr("fl")?.let { for (i in 0 until it.length()) fl.add(it.getInt(i)) }
            revealed = rv; flags = fl
            exploded = o.optInt("ex", -1)
            true
        } catch (_: Exception) { false }
    }
}
