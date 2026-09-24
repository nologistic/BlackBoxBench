package com.blackboxbench.reproduction

import android.content.Context
import org.json.JSONArray

/** Neutral helpers for JSON bundled by the reproduction Agent in assets/. */
object AssetStore {
    fun readText(context: Context, name: String): String =
        context.assets.open(name).bufferedReader().use { it.readText() }

    fun readArray(context: Context, name: String): JSONArray =
        JSONArray(readText(context, name))
}
