package com.blackboxbench.reproduction

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** A scanned audio file (fictional, deterministic library). */
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String,
    val genre: String,
    val durationMs: Int,
    val path: String,
    val sizeBytes: Long,
    val format: String,
    val bitrate: String,
    val sampleRate: String,
    val track: Int,
    val disc: Int,
    val year: String,
    val added: String,
    val modified: String,
    val replayGain: String
) {
    val durationText: String get() = mmss(durationMs)
}

fun mmss(ms: Int): String {
    if (ms <= 0) return "0:00"
    val total = ms / 1000
    return "${total / 60}:${(total % 60).toString().padStart(2, '0')}"
}

fun formatSize(bytes: Long): String {
    return if (bytes >= 1024L * 1024L) {
        String.format("%.2f MB", bytes / 1024.0 / 1024.0)
    } else {
        String.format("%.2f KB", bytes / 1024.0)
    }
}

object Library {
    val songs: List<Song> = listOf(
        Song(
            id = "s0", title = "sample_audio", artist = "", album = "BlackBoxBench",
            albumArtist = "", genre = "", durationMs = 0,
            path = "/storage/emulated/0/Download/BlackBoxBench/sample_audio.wav",
            sizeBytes = 30912, format = "Wav", bitrate = "352 kb/s", sampleRate = "22050 Hz",
            track = 0, disc = 0, year = "", added = "2026-09-24 05:02", modified = "2026-09-24 05:02",
            replayGain = ""
        ),
        Song(
            id = "s1", title = "Track 1", artist = "The Chromatics", album = "Neon Harbour",
            albumArtist = "The Chromatics", genre = "Synthwave", durationMs = 116000,
            path = "/storage/emulated/0/Music/01 Neon Harbour - Track 1.mp3",
            sizeBytes = 1855488, format = "MP3", bitrate = "320 kb/s", sampleRate = "44100 Hz",
            track = 1, disc = 1, year = "2024", added = "2026-09-24 05:02", modified = "2026-09-24 05:02",
            replayGain = "无"
        ),
        Song(
            id = "s2", title = "Track 2", artist = "The Chromatics", album = "Neon Harbour",
            albumArtist = "The Chromatics", genre = "Synthwave", durationMs = 160000,
            path = "/storage/emulated/0/Music/02 Neon Harbour - Track 2.mp3",
            sizeBytes = 2558525, format = "MP3", bitrate = "320 kb/s", sampleRate = "44100 Hz",
            track = 2, disc = 1, year = "2024", added = "2026-09-24 05:02", modified = "2026-09-24 05:02",
            replayGain = "无"
        ),
        Song(
            id = "s3", title = "Track 3", artist = "The Chromatics", album = "Neon Harbour",
            albumArtist = "The Chromatics", genre = "Synthwave", durationMs = 0,
            path = "/storage/emulated/0/Music/03 Neon Harbour - Track 3.mp3",
            sizeBytes = 2128896, format = "MP3", bitrate = "320 kb/s", sampleRate = "44100 Hz",
            track = 3, disc = 1, year = "2024", added = "2026-09-24 05:02", modified = "2026-09-24 05:02",
            replayGain = "无"
        )
    )

    fun byId(id: String): Song? = songs.firstOrNull { it.id == id }

    val albums: List<Album>
        get() = listOf(
            Album("Unknown Album", "", 0),
            Album("Neon Harbour", "The Chromatics", 3)
        )

    val artists: List<Artist>
        get() = listOf(Artist("The Chromatics", 1, 3))

    val genres: List<Genre>
        get() = listOf(Genre("未知音乐类型", 1), Genre("Synthwave", 3))

    fun albumSongs(album: String): List<Song> =
        songs.filter { it.album == album }.sortedBy { it.track }

    fun genreSongs(genre: String): List<Song> =
        songs.filter { if (genre == "未知音乐类型") it.genre.isEmpty() else it.genre == genre }

    fun artistSongs(artist: String): List<Song> = songs.filter { it.artist == artist }
}

data class Album(val name: String, val artist: String, val songCount: Int)
data class Artist(val name: String, val albumCount: Int, val songCount: Int)
data class Genre(val name: String, val songCount: Int)

data class Playlist(
    val id: String,
    val name: String,
    val songIds: List<String>,
    val kind: String, // smart | favorite | user
    val subtitle: String
)

data class LibraryCategory(val key: String, val label: String, val visible: Boolean)

/** Whole observable app state; persisted as JSON in shared preferences. */
class VinylState(context: Context) {
    private val prefs = context.getSharedPreferences("vinyl_reproduction", Context.MODE_PRIVATE)

    var themeMode by prefs.stringState("theme", "浅色")           // 浅色 / 暗色 / 黑色（AMOLED）
    var themeStyle by prefs.stringState("theme_style", "Classic")
    var rememberLastPage by prefs.boolState("remember_page", true)
    var blacklist by prefs.boolState("blacklist", false)
    var whitelist by prefs.boolState("whitelist", false)
    var colorizeNavBar by prefs.boolState("nav_bar", true)
    var colorizeShortcuts by prefs.boolState("shortcuts", true)
    var transparentControls by prefs.boolState("transparent", true)
    var classicNotification by prefs.boolState("notif_classic", false)
    var colorizedNotification by prefs.boolState("notif_color", false)
    var nowPlayingLook by prefs.stringState("np_look", "Card")
    var showLyrics by prefs.boolState("lyrics", true)
    var animatedIcon by prefs.boolState("animated_icon", false)
    var showTrackNumber by prefs.boolState("show_track", false)
    var defaultAction by prefs.stringState("default_action", "播放")
    var autoDownload by prefs.stringState("auto_download", "仅在 WiFi 下")
    var duckOnFocusLoss by prefs.boolState("duck", true)
    var gapless by prefs.boolState("gapless", false)
    var rememberShuffle by prefs.boolState("remember_shuffle", true)
    var replayGainMode by prefs.stringState("rg_mode", "无")
    var replayGainPreamp by prefs.stringState("rg_preamp", "不启用")
    var recentInterval by prefs.stringState("recent_interval", "本月")
    var favoritePlaylist by prefs.boolState("fav_playlist", true)
    var skippedSongs by prefs.boolState("skipped", false)
    var crashReports by prefs.boolState("crash", false)
    var syncQueueTag by prefs.boolState("sync_tag", false)
    var gridSize by prefs.intState("grid_size", 2)
    var sortMode by prefs.stringState("sort", "按首字符（正序）")
    var showFooter by prefs.boolState("footer", true)
    var colorFooter by prefs.boolState("footer_color", true)

    var tabOrder by prefs.stringListState("tabs", listOf("歌曲", "专辑", "艺术家", "音乐类型", "播放列表"))
    var hiddenTabs by prefs.stringListState("hidden_tabs", emptyList())
    var favorites by prefs.stringListState("favorites", emptyList())
    var history by prefs.stringListState("history", emptyList())
    var playCounts by prefs.stringState("play_counts", "{}")
    var queue by prefs.stringListState("queue", emptyList())
    var queueIndex by prefs.intState("queue_index", -1)
    var currentSongId by prefs.stringState("current_song", "")
    var positionMs by prefs.intState("position_ms", 0)
    var repeatMode by prefs.intState("repeat", 0)     // 0 off, 1 all, 2 one
    var shuffle by prefs.boolState("shuffle", false)
    var lastPage by prefs.stringState("last_page", "歌曲")
    var onboarded by prefs.boolState("onboarded", false)
    var sleepTimerMinutes by prefs.intState("sleep_minutes", 30)
    var sleepAfterTrack by prefs.boolState("sleep_after", false)
    var sleepArmed by prefs.boolState("sleep_armed", false)

    // user playlists: JSON [{"id":..,"name":..,"songs":[..]}]
    var userPlaylists: List<Pair<String, List<String>>> 
        get() {
            val raw = prefs.getString("user_playlists", "[]") ?: "[]"
            val out = mutableListOf<Pair<String, List<String>>>()
            try {
                val arr = JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val list = mutableListOf<String>()
                    val songs = o.optJSONArray("songs") ?: JSONArray()
                    for (j in 0 until songs.length()) list.add(songs.getString(j))
                    out.add(o.getString("name") to list)
                }
            } catch (_: Exception) {
            }
            return out
        }
        set(value) {
            val arr = JSONArray()
            value.forEach { (name, songs) ->
                val o = JSONObject()
                o.put("name", name)
                o.put("songs", JSONArray(songs))
                arr.put(o)
            }
            prefs.edit().putString("user_playlists", arr.toString()).apply()
        }

    fun addUserPlaylist(name: String, songs: List<String>) {
        val current = userPlaylists.toMutableList()
        val existing = current.indexOfFirst { it.first == name }
        if (existing >= 0) {
            val merged = (current[existing].second + songs).distinct()
            current[existing] = name to merged
        } else {
            current.add(name to songs.distinct())
        }
        userPlaylists = current
    }

    fun renamePlaylist(from: String, to: String) {
        userPlaylists = userPlaylists.map { if (it.first == from) Pair(to, it.second) else it }
    }

    fun deletePlaylist(name: String) {
        userPlaylists = userPlaylists.filterNot { it.first == name }
    }

    fun toggleFavorite(id: String) {
        favorites = if (favorites.contains(id)) favorites - id else favorites + id
    }

    fun markPlayed(id: String) {
        history = (listOf(id) + history.filterNot { it == id }).take(50)
        val counts = JSONObject(playCounts)
        counts.put(id, counts.optInt(id, 0) + 1)
        playCounts = counts.toString()
    }

    fun playCount(id: String): Int = try {
        JSONObject(playCounts).optInt(id, 0)
    } catch (_: Exception) {
        0
    }

    /** Playlist list shown on the 播放列表 tab. */
    fun playlists(): List<Playlist> {
        val result = mutableListOf<Playlist>()
        result.add(Playlist("recent", "最近添加", Library.songs.map { it.id }, "smart", "$recentInterval · ${Library.songs.size} 歌曲"))
        result.add(Playlist("history", "播放历史", history, "smart", "$recentInterval · ${history.size} 歌曲"))
        val notPlayed = Library.songs.map { it.id }.filterNot { history.contains(it) }
        result.add(Playlist("notplayed", "最近未播放过", notPlayed, "smart", "$recentInterval · ${notPlayed.size} 歌曲"))
        val mostPlayed = Library.songs.map { it.id }.sortedByDescending { playCount(it) }
        result.add(Playlist("mostplayed", "最喜爱的歌曲", mostPlayed, "smart", "${mostPlayed.size} 歌曲"))
        val favSongs = Library.songs.map { it.id }.filter { favorites.contains(it) }
        result.add(Playlist("favorites", "收藏夹", favSongs, "favorite", "${favSongs.size} 歌曲"))
        userPlaylists.forEach { (name, ids) ->
            result.add(Playlist("user:$name", name, ids.filter { Library.byId(it) != null }, "user", "${ids.size} 歌曲"))
        }
        return result
    }

    fun playlistById(id: String): Playlist? = playlists().firstOrNull { it.id == id }

    fun applyPlaylists() { /* values are computed on demand */ }
}

// ---------------------------------------------------------------------------
// small persistence helpers
// ---------------------------------------------------------------------------

private fun android.content.SharedPreferences.stringState(key: String, def: String) =
    StateProperty(
        get = { getString(key, def) ?: def },
        set = { v -> edit().putString(key, v).apply() }
    )

private fun android.content.SharedPreferences.intState(key: String, def: Int) =
    StateProperty(
        get = { getInt(key, def) },
        set = { v -> edit().putInt(key, v).apply() }
    )

private fun android.content.SharedPreferences.boolState(key: String, def: Boolean) =
    StateProperty(
        get = { getBoolean(key, def) },
        set = { v -> edit().putBoolean(key, v).apply() }
    )

private fun android.content.SharedPreferences.stringListState(key: String, def: List<String>) =
    StateProperty(
        get = {
            val raw = getString(key, null) ?: return@StateProperty def
            try {
                val arr = JSONArray(raw)
                val out = mutableListOf<String>()
                for (i in 0 until arr.length()) out.add(arr.getString(i))
                out
            } catch (_: Exception) {
                def
            }
        },
        set = { v -> edit().putString(key, JSONArray(v).toString()).apply() }
    )

class StateProperty<T>(private val get: () -> T, private val set: (T) -> Unit) {
    private var cached: T? = null
    private var loaded = false

    operator fun getValue(thisRef: Any?, property: Any?): T {
        if (!loaded) {
            cached = get()
            loaded = true
        }
        @Suppress("UNCHECKED_CAST")
        return cached as T
    }

    operator fun setValue(thisRef: Any?, property: Any?, value: T) {
        cached = value
        loaded = true
        set(value)
    }
}
