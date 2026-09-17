package com.blackboxbench.reproduction

import android.content.Context
import org.json.JSONArray

data class HistoryEntry(val expression: String, val result: String)

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("fossify_calc_repro", Context.MODE_PRIVATE)

    var history: MutableList<HistoryEntry>
        get() {
            val raw = sp.getString("history", null) ?: return mutableListOf()
            return try {
                val arr = JSONArray(raw)
                val out = mutableListOf<HistoryEntry>()
                for (i in 0 until arr.length()) {
                    val pair = arr.getJSONArray(i)
                    out.add(HistoryEntry(pair.getString(0), pair.getString(1)))
                }
                out
            } catch (e: Exception) {
                mutableListOf()
            }
        }
        set(value) {
            val arr = JSONArray()
            value.take(200).forEach {
                val pair = JSONArray()
                pair.put(it.expression)
                pair.put(it.result)
                arr.put(pair)
            }
            sp.edit().putString("history", arr.toString()).apply()
        }

    fun addHistory(expression: String, result: String) {
        val list = history
        list.add(0, HistoryEntry(expression, result))
        history = list
    }

    fun clearHistory() {
        sp.edit().remove("history").apply()
    }

    var vibrate: Boolean
        get() = sp.getBoolean("vibrate", true)
        set(v) = sp.edit().putBoolean("vibrate", v).apply()

    var keepAwake: Boolean
        get() = sp.getBoolean("keep_awake", true)
        set(v) = sp.edit().putBoolean("keep_awake", v).apply()

    var language: String
        get() = sp.getString("language", "中文") ?: "中文"
        set(v) = sp.edit().putString("language", v).apply()

    var theme: String
        get() = sp.getString("theme", "系统默认") ?: "系统默认"
        set(v) = sp.edit().putString("theme", v).apply()

    var font: String
        get() = sp.getString("font", "系统默认") ?: "系统默认"
        set(v) = sp.edit().putString("font", v).apply()

    var iconColor: Int
        get() = sp.getInt("icon_color", 0xFF106D1F.toInt())
        set(v) = sp.edit().putInt("icon_color", v).apply()

    var widgetColor: Int
        get() = sp.getInt("widget_color", 0xFF106D1F.toInt())
        set(v) = sp.edit().putInt("widget_color", v).apply()

    fun converterFrom(catId: String, fallback: Int): Int =
        sp.getInt("conv_from_$catId", fallback)

    fun setConverterFrom(catId: String, index: Int) =
        sp.edit().putInt("conv_from_$catId", index).apply()

    fun converterTo(catId: String, fallback: Int): Int =
        sp.getInt("conv_to_$catId", fallback)

    fun setConverterTo(catId: String, index: Int) =
        sp.edit().putInt("conv_to_$catId", index).apply()
}