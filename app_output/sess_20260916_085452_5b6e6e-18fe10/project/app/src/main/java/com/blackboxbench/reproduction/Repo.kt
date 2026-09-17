package com.blackboxbench.reproduction

import android.content.Context
import org.json.JSONObject

/** Persists the habit list as a JSON string in SharedPreferences. */
object Repo {
    private const val PREF = "loop_repro"
    private const val KEY = "habits"
    private var cached: MutableList<Habit>? = null

    fun load(context: Context): MutableList<Habit> {
        cached?.let { return it }
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val raw = sp.getString(KEY, null)
        val list = if (raw == null) {
            val seeded = seedFromAssets(context)
            sp.edit().putString(KEY, habitsToJson(seeded)).apply()
            seeded
        } else {
            try {
                habitsFromJson(raw)
            } catch (e: Exception) {
                seedFromAssets(context)
            }
        }
        cached = list
        return list
    }

    fun save(context: Context, list: List<Habit>) {
        cached = list as MutableList<Habit>
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString(KEY, habitsToJson(list)).apply()
    }

    private fun seedFromAssets(context: Context): MutableList<Habit> {
        val list = mutableListOf<Habit>()
        try {
            val raw = AssetStore.readText(context, "habits.json")
            val root = JSONObject(raw)
            val arr = root.optJSONArray("habits") ?: return defaultSeed()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val h = Habit()
                h.id = o.optString("id", "hb_$i")
                h.name = o.optString("name")
                h.question = ""
                h.type = if (o.optString("type") == "quantified") HabitType.QUANTIFIED else HabitType.BOOLEAN
                h.unit = o.optString("unit")
                h.target = o.optDouble("target", 0.0)
                h.targetAtLeast = true
                h.color = o.optString("color", "#3F80E0")
                val freq = o.optJSONObject("frequency") ?: JSONObject()
                when (freq.optString("kind")) {
                    "weekly_count" -> {
                        h.freqKind = FreqKind.WEEKLY_COUNT
                        h.freqN = freq.optInt("times", 3)
                    }
                    "weekly_days" -> {
                        h.freqKind = FreqKind.WEEKLY_DAYS
                        val days = freq.optJSONArray("days")
                        val codes = mutableListOf<Int>()
                        if (days != null) for (j in 0 until days.length()) {
                            val idx = listOf("MO", "TU", "WE", "TH", "FR", "SA", "SU").indexOf(days.getString(j))
                            if (idx >= 0) codes.add(idx)
                        }
                        if (codes.isNotEmpty()) h.reminderDays = codes
                    }
                    "monthly_count" -> {
                        h.freqKind = FreqKind.MONTHLY_COUNT
                        h.freqN = freq.optInt("times", 10)
                    }
                    "every_n_days" -> {
                        h.freqKind = FreqKind.EVERY_N_DAYS
                        h.freqN = freq.optInt("days", 3)
                    }
                    else -> h.freqKind = FreqKind.DAILY
                }
                val rem = o.optString("reminder", "")
                h.reminder = if (rem.isBlank()) null else rem
                h.reminderDays = mutableListOf(0, 1, 2, 3, 4, 5, 6)
                h.archived = o.optBoolean("archived", false)
                val ci = o.optJSONObject("checkins")
                if (ci != null) {
                    val it = ci.keys()
                    while (it.hasNext()) {
                        val k = it.next()
                        h.checkins[k] = ci.optInt(k)
                    }
                }
                list.add(h)
            }
        } catch (e: Exception) {
            return defaultSeed()
        }
        return if (list.isEmpty()) defaultSeed() else list
    }

    private fun defaultSeed(): MutableList<Habit> = mutableListOf()
}
