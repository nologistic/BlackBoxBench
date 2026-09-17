package com.blackboxbench.reproduction

import org.json.JSONArray
import org.json.JSONObject

enum class HabitType { BOOLEAN, QUANTIFIED }

enum class FreqKind { DAILY, EVERY_N_DAYS, WEEKLY_COUNT, MONTHLY_COUNT, EVERY_N_DAYS_TIMES, WEEKLY_DAYS }

data class Habit(
    var id: String = "",
    var name: String = "",
    var question: String = "",
    var type: HabitType = HabitType.BOOLEAN,
    var unit: String = "",
    var target: Double = 0.0,
    var targetAtLeast: Boolean = true,
    var color: String = "#3F80E0",
    var freqKind: FreqKind = FreqKind.DAILY,
    var freqN: Int = 3,
    var freqM: Int = 14,
    var reminder: String? = null,
    var reminderDays: MutableList<Int> = mutableListOf(0, 1, 2, 3, 4, 5, 6),
    var notes: String = "",
    var archived: Boolean = false,
    var checkins: MutableMap<String, Int> = linkedMapOf(),
    var dayNotes: MutableMap<String, String> = linkedMapOf()
)

private val DAY_CODES = listOf("MO", "TU", "WE", "TH", "FR", "SA", "SU")

fun habitDayCode(index: Int): String = DAY_CODES[index.coerceIn(0, 6)]

fun habitToJson(h: Habit): JSONObject {
    val o = JSONObject()
    o.put("id", h.id)
    o.put("name", h.name)
    o.put("question", h.question)
    o.put("type", if (h.type == HabitType.QUANTIFIED) "quantified" else "boolean")
    o.put("unit", h.unit)
    o.put("target", h.target)
    o.put("targetAtLeast", h.targetAtLeast)
    o.put("color", h.color)
    o.put("freqKind", h.freqKind.name)
    o.put("freqN", h.freqN)
    o.put("freqM", h.freqM)
    o.put("reminder", h.reminder ?: "")
    o.put("reminderDays", JSONArray(h.reminderDays))
    o.put("notes", h.notes)
    o.put("archived", h.archived)
    o.put("checkins", JSONObject(h.checkins as Map<*, *>))
    o.put("dayNotes", JSONObject(h.dayNotes as Map<*, *>))
    return o
}

fun habitFromJson(o: JSONObject): Habit {
    val h = Habit()
    h.id = o.optString("id")
    h.name = o.optString("name")
    h.question = o.optString("question")
    h.type = if (o.optString("type") == "quantified") HabitType.QUANTIFIED else HabitType.BOOLEAN
    h.unit = o.optString("unit")
    h.target = o.optDouble("target", 0.0)
    h.targetAtLeast = o.optBoolean("targetAtLeast", true)
    h.color = o.optString("color", "#3F80E0")
    h.freqKind = try {
        FreqKind.valueOf(o.optString("freqKind", "DAILY"))
    } catch (e: Exception) {
        FreqKind.DAILY
    }
    h.freqN = o.optInt("freqN", 3)
    h.freqM = o.optInt("freqM", 14)
    val rem = o.optString("reminder", "")
    h.reminder = if (rem.isBlank()) null else rem
    val days = o.optJSONArray("reminderDays")
    h.reminderDays = mutableListOf()
    if (days != null) for (i in 0 until days.length()) h.reminderDays.add(days.getInt(i))
    if (h.reminderDays.isEmpty()) h.reminderDays = mutableListOf(0, 1, 2, 3, 4, 5, 6)
    h.notes = o.optString("notes")
    h.archived = o.optBoolean("archived", false)
    val ci = o.optJSONObject("checkins")
    h.checkins = linkedMapOf()
    if (ci != null) {
        val it = ci.keys()
        while (it.hasNext()) {
            val k = it.next()
            h.checkins[k] = ci.optInt(k)
        }
    }
    val dn = o.optJSONObject("dayNotes")
    h.dayNotes = linkedMapOf()
    if (dn != null) {
        val it = dn.keys()
        while (it.hasNext()) {
            val k = it.next()
            h.dayNotes[k] = dn.optString(k)
        }
    }
    return h
}

fun habitsToJson(list: List<Habit>): String {
    val arr = JSONArray()
    list.forEach { arr.put(habitToJson(it)) }
    return arr.toString()
}

fun habitsFromJson(raw: String): MutableList<Habit> {
    val list = mutableListOf<Habit>()
    val arr = JSONArray(raw)
    for (i in 0 until arr.length()) list.add(habitFromJson(arr.getJSONObject(i)))
    return list
}

fun Habit.deepCopy(): Habit = habitFromJson(habitToJson(this))
