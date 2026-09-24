package com.blackboxbench.reproduction

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max

/** One audio file in the local media library. */
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String,
    val genre: String,
    val trackNo: Int,
    val discNo: Int,
    val year: String,
    val seconds: Int,
    val fileName: String,
    val sizeBytes: Long,
) {
    val path: String get() = "/storage/emulated/0/Music/$fileName"
    val durationLabel: String get() = formatTime(seconds)
}

fun formatTime(seconds: Int): String {
    val s = max(0, seconds)
    return "%d:%02d".format(s / 60, s % 60)
}

fun formatTime(ms: Long): String = formatTime((ms / 1000L).toInt())

fun formatTotal(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

enum class AutoKind(val label: String) {
    RECENT_ADDED("最近添加"),
    HISTORY("播放历史"),
    NOT_PLAYED("最近未播放过"),
    MOST_PLAYED("最喜爱的歌曲"),
    FAVORITES("收藏夹"),
}

class Playlist(val id: String, var name: String, val auto: AutoKind? = null) {
    val trackIds = mutableStateListOf<String>()
    val isAuto: Boolean get() = auto != null
}

/** The seeded local music library, mirroring the observed device contents. */
object Library {
    val tracks: List<Track> = listOf(
        Track("t1", "Track 1", "The Chromatics", "Neon Harbour", "", "Synthwave", 1, 0, "", 107, "01 Neon Harbour - Track 1.mp3", 2_411_264L),
        Track("t2", "Track 2", "The Chromatics", "Neon Harbour", "", "Synthwave", 2, 0, "", 125, "02 Neon Harbour - Track 2.mp3", 2_806_144L),
        Track("t3", "Track 3", "The Chromatics", "Neon Harbour", "", "Synthwave", 3, 0, "", 92, "03 Neon Harbour - Track 3.mp3", 2_068_480L),
        Track("t4", "Track 4", "The Chromatics", "Neon Harbour", "", "Synthwave", 4, 0, "", 157, "04 Neon Harbour - Track 4.mp3", 2_516_582L),
        Track("t5", "Track 1", "Mei & The Foxes", "Paper Lanterns", "", "Indie Folk", 1, 0, "", 50, "05 Paper Lanterns - Track 1.mp3", 1_126_400L),
        Track("t6", "Track 2", "Mei & The Foxes", "Paper Lanterns", "", "Indie Folk", 2, 0, "", 119, "06 Paper Lanterns - Track 2.mp3", 2_672_640L),
        Track("t7", "Track 3", "Mei & The Foxes", "Paper Lanterns", "", "Indie Folk", 3, 0, "", 165, "07 Paper Lanterns - Track 3.mp3", 3_706_880L),
        Track("t8", "sample_audio", "BlackBoxBench", "Unknown Album", "", "", 0, 0, "", 0, "08 sample_audio.wav", 30_912L),
    )

    fun byId(id: String): Track? = tracks.firstOrNull { it.id == id }
    fun byIds(ids: List<String>): List<Track> = ids.mapNotNull { byId(it) }
}

/** Sort modes offered by the songs tab overflow menu. */
val SORT_MODES = listOf(
    "按首字符", "按首字符（倒序）", "按艺术家", "按专辑", "按 Year", "按年份",
    "按 Added", "按添加日期", "按 Modified", "Modified - Recent first"
)

val GRID_SIZES = listOf("列表", "小", "中", "大")

class AppState(private val context: Context) {

    private val prefs = context.getSharedPreferences("vinyl_repro", Context.MODE_PRIVATE)

    // ---------------------------------------------------------------- library
    val songs get() = Library.tracks

    var sortMode by mutableStateOf(0)
    var gridSize by mutableStateOf(1)
    var showFooter by mutableStateOf(true)
    var tintFooter by mutableStateOf(true)

    val playlists = mutableStateListOf<Playlist>()

    var favoriteIds = mutableStateListOf<String>()
    var historyIds = mutableStateListOf<String>()

    // --------------------------------------------------------------- settings
    var firstLaunch by mutableStateOf(true)
    var rememberLastPage by mutableStateOf(true)
    var categoryOrder = mutableStateListOf(0, 1, 2, 3, 4)
    var categoryEnabled = mutableStateListOf(true, true, true, true, true)
    var primaryColor by mutableStateOf(0xFF3F51B5.toInt())
    var accentColor by mutableStateOf(0xFFE91E63.toInt())
    var tintNavBar by mutableStateOf(true)
    var tintShortcuts by mutableStateOf(true)
    var transparentWidgets by mutableStateOf(true)
    var themeMode by mutableStateOf(0) // 0 light, 1 dark, 2 black
    var classicNotification by mutableStateOf(false)
    var coloredNotification by mutableStateOf(false)
    var nowPlayingAppearance by mutableStateOf(0)
    var syncLyrics by mutableStateOf(true)
    var animateIcon by mutableStateOf(false)
    var showTrackNumber by mutableStateOf(false)
    var defaultTapAction by mutableStateOf(0)
    var autoDownloadMeta by mutableStateOf(1)
    var pauseOnFocusLoss by mutableStateOf(true)
    var gapless by mutableStateOf(false)
    var rememberShuffle by mutableStateOf(true)
    var replayGainMode by mutableStateOf(0)
    var replayGainPreamp by mutableStateOf(0)
    var recentAddedInterval by mutableStateOf(0)
    var recentPlayedInterval by mutableStateOf(0)
    var notPlayedInterval by mutableStateOf(0)
    var whiteListEnabled by mutableStateOf(false)

    // -------------------------------------------------------------- playback
    val queue = mutableStateListOf<String>()
    var queueIndex by mutableStateOf(-1)
    var positionMs by mutableStateOf(0L)
    var playing by mutableStateOf(false)
    var shuffle by mutableStateOf(false)
    var repeatMode by mutableStateOf(0) // 0 off, 1 all, 2 one
    var sleepTimerMinutes by mutableStateOf(0)
    var sleepAfterCurrent by mutableStateOf(false)
    var sleepRemainingMs by mutableStateOf(0L)

    val currentTrack: Track?
        get() = if (queueIndex in queue.indices) Library.byId(queue[queueIndex]) else null

    // ------------------------------------------------------------ navigation
    var screen by mutableStateOf(Screen.LIBRARY_SONGS)
    var drawerOpen by mutableStateOf(false)
    var lastLibraryScreen by mutableStateOf(Screen.LIBRARY_SONGS)
    var searchQuery by mutableStateOf("")
    var searchActive by mutableStateOf(false)
    var snackbar by mutableStateOf<String?>(null)
    var multiSelect by mutableStateOf(false)
    val selectedIds = mutableStateListOf<String>()

    // -------------------------------------------------------------- dialogs
    var dialog by mutableStateOf<DialogKind?>(null)
    var infoText by mutableStateOf("")
    var tagEditorTrackId by mutableStateOf<String?>(null)
    var detailsTrackId by mutableStateOf<String?>(null)
    var openPlaylistId by mutableStateOf<String?>(null)
    var openAlbum by mutableStateOf<String?>(null)
    var openArtist by mutableStateOf<String?>(null)
    var openGenre by mutableStateOf<String?>(null)

    init {
        seed()
        restore()
    }

    private fun seed() {
        if (playlists.isNotEmpty()) return
        fun mk(id: String, name: String, auto: AutoKind?, ids: List<String>) =
            Playlist(id, name, auto).also { it.trackIds.addAll(ids) }

        playlists.add(mk("auto_recent", "最近添加", AutoKind.RECENT_ADDED, songs.map { it.id }))
        playlists.add(mk("auto_history", "播放历史", AutoKind.HISTORY, listOf("t4", "t6", "t1")))
        playlists.add(mk("auto_notplayed", "最近未播放过", AutoKind.NOT_PLAYED, listOf("t2", "t3", "t5", "t7", "t8")))
        playlists.add(mk("auto_most", "最喜爱的歌曲", AutoKind.MOST_PLAYED, listOf("t4", "t1")))
        playlists.add(mk("auto_fav", "收藏夹", AutoKind.FAVORITES, listOf("t5")))
        favoriteIds.add("t5")
        historyIds.addAll(listOf("t4", "t6", "t1"))
    }

    // ------------------------------------------------------------ persistence
    fun persist() {
        val json = JSONObject()
        json.put("sortMode", sortMode)
        json.put("gridSize", gridSize)
        json.put("showFooter", showFooter)
        json.put("tintFooter", tintFooter)
        json.put("firstLaunch", firstLaunch)
        json.put("rememberLastPage", rememberLastPage)
        json.put("primaryColor", primaryColor)
        json.put("accentColor", accentColor)
        json.put("tintNavBar", tintNavBar)
        json.put("tintShortcuts", tintShortcuts)
        json.put("transparentWidgets", transparentWidgets)
        json.put("themeMode", themeMode)
        json.put("classicNotification", classicNotification)
        json.put("coloredNotification", coloredNotification)
        json.put("syncLyrics", syncLyrics)
        json.put("animateIcon", animateIcon)
        json.put("showTrackNumber", showTrackNumber)
        json.put("pauseOnFocusLoss", pauseOnFocusLoss)
        json.put("gapless", gapless)
        json.put("rememberShuffle", rememberShuffle)
        json.put("whiteListEnabled", whiteListEnabled)
        json.put("lastScreen", lastLibraryScreen.name)
        json.put("categoryOrder", JSONArray(categoryOrder.toList()))
        json.put("categoryEnabled", JSONArray(categoryEnabled.toList()))
        json.put("favorites", JSONArray(favoriteIds.toList()))
        json.put("history", JSONArray(historyIds.toList()))
        json.put("queue", JSONArray(queue.toList()))
        json.put("queueIndex", queueIndex)
        json.put("shuffle", shuffle)
        val pls = JSONArray()
        playlists.forEach { p ->
            pls.put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("auto", p.auto?.name ?: JSONObject.NULL)
                put("ids", JSONArray(p.trackIds.toList()))
            })
        }
        json.put("playlists", pls)
        prefs.edit().putString("state", json.toString()).apply()
    }

    private fun restore() {
        val raw = prefs.getString("state", null) ?: return
        runCatching {
            val json = JSONObject(raw)
            sortMode = json.optInt("sortMode", 0)
            gridSize = json.optInt("gridSize", 1)
            showFooter = json.optBoolean("showFooter", true)
            tintFooter = json.optBoolean("tintFooter", true)
            firstLaunch = json.optBoolean("firstLaunch", true)
            rememberLastPage = json.optBoolean("rememberLastPage", true)
            primaryColor = json.optInt("primaryColor", 0xFF3F51B5.toInt())
            accentColor = json.optInt("accentColor", 0xFFE91E63.toInt())
            tintNavBar = json.optBoolean("tintNavBar", true)
            tintShortcuts = json.optBoolean("tintShortcuts", true)
            transparentWidgets = json.optBoolean("transparentWidgets", true)
            themeMode = json.optInt("themeMode", 0)
            classicNotification = json.optBoolean("classicNotification", false)
            coloredNotification = json.optBoolean("coloredNotification", false)
            syncLyrics = json.optBoolean("syncLyrics", true)
            animateIcon = json.optBoolean("animateIcon", false)
            showTrackNumber = json.optBoolean("showTrackNumber", false)
            pauseOnFocusLoss = json.optBoolean("pauseOnFocusLoss", true)
            gapless = json.optBoolean("gapless", false)
            rememberShuffle = json.optBoolean("rememberShuffle", true)
            whiteListEnabled = json.optBoolean("whiteListEnabled", false)
            json.optJSONArray("categoryOrder")?.let { a ->
                categoryOrder.clear(); for (i in 0 until a.length()) categoryOrder.add(a.getInt(i))
            }
            json.optJSONArray("categoryEnabled")?.let { a ->
                categoryEnabled.clear(); for (i in 0 until a.length()) categoryEnabled.add(a.getBoolean(i))
            }
            json.optJSONArray("favorites")?.let { a ->
                favoriteIds.clear(); for (i in 0 until a.length()) favoriteIds.add(a.getString(i))
            }
            json.optJSONArray("history")?.let { a ->
                historyIds.clear(); for (i in 0 until a.length()) historyIds.add(a.getString(i))
            }
            json.optJSONArray("queue")?.let { a ->
                queue.clear(); for (i in 0 until a.length()) queue.add(a.getString(i))
            }
            queueIndex = json.optInt("queueIndex", -1)
            shuffle = json.optBoolean("shuffle", false)
            val last = json.optString("lastScreen", Screen.LIBRARY_SONGS.name)
            lastLibraryScreen = runCatching { Screen.valueOf(last) }.getOrDefault(Screen.LIBRARY_SONGS)
            json.optJSONArray("playlists")?.let { a ->
                playlists.clear()
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    val autoName = o.optString("auto", "")
                    val auto = if (autoName.isEmpty() || autoName == "null") null
                    else runCatching { AutoKind.valueOf(autoName) }.getOrNull()
                    val p = Playlist(o.getString("id"), o.getString("name"), auto)
                    val ids = o.optJSONArray("ids")
                    if (ids != null) for (j in 0 until ids.length()) p.trackIds.add(ids.getString(j))
                    playlists.add(p)
                }
            }
        }
    }

    // ------------------------------------------------------------- utilities
    fun toast(message: String) {
        snackbar = message
    }

    fun playlist(id: String?): Playlist? = playlists.firstOrNull { it.id == id }

    fun autoPlaylist(kind: AutoKind): Playlist? = playlists.firstOrNull { it.auto == kind }

    fun refreshAutoPlaylists() {
        autoPlaylist(AutoKind.HISTORY)?.let { p ->
            p.trackIds.clear(); p.trackIds.addAll(historyIds)
        }
        autoPlaylist(AutoKind.FAVORITES)?.let { p ->
            p.trackIds.clear(); p.trackIds.addAll(favoriteIds)
        }
    }

    fun isFavorite(id: String) = favoriteIds.contains(id)

    fun toggleFavorite(id: String) {
        if (favoriteIds.contains(id)) favoriteIds.remove(id) else favoriteIds.add(0, id)
        refreshAutoPlaylists()
        persist()
    }

    fun sortedSongs(): List<Track> {
        val base = songs.toList()
        return when (sortMode) {
            0 -> base.sortedBy { it.title.lowercase() }
            1 -> base.sortedByDescending { it.title.lowercase() }
            2 -> base.sortedBy { it.artist.lowercase() }
            3 -> base.sortedBy { it.album.lowercase() }
            4, 5 -> base.sortedBy { it.year.ifEmpty { "zzz" } }
            else -> base
        }
    }

    // -------------------------------------------------------------- playback
    fun noteHistory(id: String) {
        historyIds.remove(id)
        historyIds.add(0, id)
        refreshAutoPlaylists()
    }

    fun playQueue(ids: List<String>, startIndex: Int) {
        queue.clear()
        queue.addAll(if (shuffle) ids.shuffled() else ids)
        queueIndex = if (shuffle) 0 else startIndex.coerceIn(0, max(0, queue.size - 1))
        positionMs = 0
        playing = true
        currentTrack?.let { noteHistory(it.id) }
        persist()
    }

    fun playTrack(id: String, within: List<String>) {
        val ids = within.ifEmpty { listOf(id) }
        val index = ids.indexOf(id).coerceAtLeast(0)
        playQueue(ids, index)
    }

    fun togglePlay() {
        if (currentTrack == null) return
        playing = !playing
        persist()
    }

    fun next() {
        if (queue.isEmpty()) return
        queueIndex = if (queueIndex + 1 < queue.size) queueIndex + 1 else 0
        positionMs = 0
        currentTrack?.let { noteHistory(it.id) }
        persist()
    }

    fun previous() {
        if (queue.isEmpty()) return
        if (positionMs > 3000) {
            positionMs = 0
        } else {
            queueIndex = if (queueIndex - 1 >= 0) queueIndex - 1 else 0
            positionMs = 0
            currentTrack?.let { noteHistory(it.id) }
        }
        persist()
    }

    fun tick(deltaMs: Long) {
        if (!playing || currentTrack == null) return
        val dur = currentTrack!!.seconds * 1000L
        positionMs += deltaMs
        if (sleepTimerMinutes > 0 || sleepAfterCurrent) {
            if (sleepRemainingMs > 0) {
                sleepRemainingMs -= deltaMs
                if (sleepRemainingMs <= 0) {
                    sleepTimerMinutes = 0
                    sleepRemainingMs = 0
                    playing = false
                }
            }
        }
        if (dur > 0 && positionMs >= dur) {
            if (repeatMode == 2) {
                positionMs = 0
            } else if (sleepAfterCurrent) {
                sleepAfterCurrent = false
                playing = false
                positionMs = dur
            } else {
                next()
            }
        }
    }

    fun clearQueue() {
        queue.clear()
        queueIndex = -1
        positionMs = 0
        playing = false
        persist()
    }

    fun addToQueue(id: String) {
        queue.add(id)
        persist()
    }

    fun playNext(id: String) {
        if (queueIndex < 0) {
            queue.add(id); queueIndex = 0
        } else {
            queue.add(queueIndex + 1, id)
        }
        persist()
    }

    fun removeFromQueue(index: Int) {
        if (index !in queue.indices) return
        queue.removeAt(index)
        if (index < queueIndex) queueIndex--
        if (queue.isEmpty()) {
            queueIndex = -1
            playing = false
        } else if (queueIndex >= queue.size) {
            queueIndex = queue.size - 1
        }
        persist()
    }

    fun queueTotalSeconds(): Int = Library.byIds(queue.toList()).sumOf { it.seconds }

    // ---------------------------------------------------------- collections
    fun createPlaylist(name: String, initial: List<String> = emptyList()): Playlist {
        val p = Playlist("user_${System.currentTimeMillis()}", name)
        p.trackIds.addAll(initial)
        playlists.add(p)
        persist()
        return p
    }

    fun renamePlaylist(id: String, name: String) {
        playlist(id)?.let { it.name = name }
        persist()
    }

    fun deletePlaylist(id: String) {
        playlists.removeAll { it.id == id }
        persist()
    }

    fun addToPlaylist(id: String, ids: List<String>) {
        playlist(id)?.let { p ->
            ids.forEach { if (!p.trackIds.contains(it)) p.trackIds.add(it) }
            toast("${ids.size} 首歌曲已加入到播放列表 ${p.name}。")
        }
        persist()
    }

    fun albums(): List<Pair<String, List<Track>>> =
        songs.groupBy { it.album }.toList().sortedBy { it.first.lowercase() }

    fun artists(): List<Pair<String, List<Track>>> =
        songs.groupBy { it.artist }.toList().sortedBy { it.first.lowercase() }

    fun genres(): List<Pair<String, List<Track>>> =
        songs.filter { it.genre.isNotEmpty() }.groupBy { it.genre }.toList().sortedBy { it.first.lowercase() }
}

enum class Screen {
    LIBRARY_SONGS, LIBRARY_ALBUMS, LIBRARY_ARTISTS, LIBRARY_GENRES, LIBRARY_PLAYLISTS,
    FOLDERS, SETTINGS, ABOUT, INTRO, NOW_PLAYING, TAG_EDITOR,
    ALBUM_DETAIL, ARTIST_DETAIL, GENRE_DETAIL, PLAYLIST_DETAIL
}

enum class DialogKind {
    CREATE_PLAYLIST, RENAME_PLAYLIST, ADD_TO_PLAYLIST, SAVE_QUEUE, DETAILS, SLEEP_TIMER,
    CATEGORIES, THEME, PRIMARY_COLOR, ACCENT_COLOR, INFO, SORT, GRID, APPEARANCE,
    REPLAY_GAIN, INTERVAL, CONFIRM_DELETE, EQUALIZER
}
