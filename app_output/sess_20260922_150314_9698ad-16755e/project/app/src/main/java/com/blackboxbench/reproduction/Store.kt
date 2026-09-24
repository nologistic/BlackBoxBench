package com.blackboxbench.reproduction

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** 播放列表 / 标签编辑 / 设置 / 引导完成标记的持久化（SharedPreferences + JSON）。 */
object Store {
    private const val PREFS = "vinyl_store"
    private const val KEY_ONBOARDED = "onboarded"
    private const val KEY_PLAYLISTS = "playlists"
    private const val KEY_TAG_OVERRIDES = "tag_overrides"
    private const val KEY_SETTINGS = "settings"
    private const val KEY_FAVORITES = "favorites"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isOnboarded(context: Context): Boolean = prefs(context).getBoolean(KEY_ONBOARDED, false)
    fun setOnboarded(context: Context, done: Boolean) =
        prefs(context).edit().putBoolean(KEY_ONBOARDED, done).apply()

    fun loadPlaylists(context: Context): MutableList<Playlist> {
        val raw = prefs(context).getString(KEY_PLAYLISTS, null) ?: return mutableListOf()
        val arr = JSONArray(raw)
        val out = mutableListOf<Playlist>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val ids = o.getJSONArray("songIds")
            val list = mutableListOf<Long>()
            for (j in 0 until ids.length()) list.add(ids.getLong(j))
            out.add(Playlist(o.getLong("id"), o.getString("name"), list))
        }
        return out
    }

    fun savePlaylists(context: Context, playlists: List<Playlist>) {
        val arr = JSONArray()
        playlists.forEach { p ->
            val o = JSONObject()
            o.put("id", p.id)
            o.put("name", p.name)
            o.put("songIds", JSONArray(p.songIds))
            arr.put(o)
        }
        prefs(context).edit().putString(KEY_PLAYLISTS, arr.toString()).apply()
    }

    /** 标签编辑结果：songId -> 覆盖字段。 */
    fun loadTagOverrides(context: Context): MutableMap<Long, JSONObject> {
        val raw = prefs(context).getString(KEY_TAG_OVERRIDES, null) ?: return mutableMapOf()
        val o = JSONObject(raw)
        val map = mutableMapOf<Long, JSONObject>()
        o.keys().forEach { k -> map[k.toLong()] = o.getJSONObject(k) }
        return map
    }

    fun saveTagOverrides(context: Context, map: Map<Long, JSONObject>) {
        val o = JSONObject()
        map.forEach { (k, v) -> o.put(k.toString(), v) }
        prefs(context).edit().putString(KEY_TAG_OVERRIDES, o.toString()).apply()
    }

    fun loadFavorites(context: Context): MutableSet<Long> {
        val raw = prefs(context).getString(KEY_FAVORITES, null) ?: return mutableSetOf()
        val arr = JSONArray(raw)
        val set = mutableSetOf<Long>()
        for (i in 0 until arr.length()) set.add(arr.getLong(i))
        return set
    }

    fun saveFavorites(context: Context, set: Set<Long>) {
        prefs(context).edit().putString(KEY_FAVORITES, JSONArray(set).toString()).apply()
    }

    fun loadSettings(context: Context): AppSettings {
        val raw = prefs(context).getString(KEY_SETTINGS, null) ?: return AppSettings()
        val o = JSONObject(raw)
        return AppSettings(
            pauseOnHeadsetDisconnect = o.optBoolean("pauseOnHeadsetDisconnect", true),
            ignoreShortAudio = o.optBoolean("ignoreShortAudio", false),
            defaultPlayMode = o.optString("defaultPlayMode", "顺序播放"),
            theme = o.optString("theme", "浅色"),
        )
    }

    fun saveSettings(context: Context, s: AppSettings) {
        val o = JSONObject()
        o.put("pauseOnHeadsetDisconnect", s.pauseOnHeadsetDisconnect)
        o.put("ignoreShortAudio", s.ignoreShortAudio)
        o.put("defaultPlayMode", s.defaultPlayMode)
        o.put("theme", s.theme)
        prefs(context).edit().putString(KEY_SETTINGS, o.toString()).apply()
    }
}
