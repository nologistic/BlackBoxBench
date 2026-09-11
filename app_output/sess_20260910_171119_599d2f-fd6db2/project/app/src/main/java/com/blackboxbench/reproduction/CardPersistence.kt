package com.blackboxbench.reproduction

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

fun loadAddedCards(prefs: SharedPreferences): List<FlashCard> {
    val result = mutableListOf<FlashCard>()
    val raw = prefs.getString("added_cards", "[]") ?: "[]"
    runCatching {
        val array = JSONArray(raw)
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            result += FlashCard(
                deck = item.optString("deck"),
                front = item.optString("front"),
                back = item.optString("back"),
                tags = item.optString("tags", "自建")
            )
        }
    }
    return result
}

fun saveAddedCards(prefs: SharedPreferences, cards: List<FlashCard>) {
    val array = JSONArray()
    cards.forEach { card ->
        array.put(JSONObject().apply {
            put("deck", card.deck)
            put("front", card.front)
            put("back", card.back)
            put("tags", card.tags)
        })
    }
    prefs.edit().putString("added_cards", array.toString()).apply()
}
