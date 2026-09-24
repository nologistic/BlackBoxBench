package com.blackboxbench.reproduction

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

enum class RepeatMode { OFF, ALL, ONE }

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String,
    val trackNo: Int,
    val discNo: Int,
    val durationSec: Int,
    val year: Int,
    val genre: String,
    val fileName: String,
    val sizeBytes: Long,
    val addedAt: Long,
    val modifiedAt: Long,
    val lyrics: String = ""
) {
    val durationLabel: String get() = formatDuration(durationSec)
    val path: String get() = "/storage/emulated/0/Music/$artist - $album/$fileName"
}

fun formatDuration(seconds: Int): String {
    val s = if (seconds < 0) 0 else seconds
    return String.format(Locale.US, "%d:%02d", s / 60, s % 60)
}

fun formatTotal(seconds: Int): String {
    val s = if (seconds < 0) 0 else seconds
    val h = s / 3600
    val m = (s % 3600) / 60
    return if (h > 0) String.format(Locale.US, "%d 小时 %d 分钟", h, m)
    else String.format(Locale.US, "%d 分钟", m)
}

fun formatSize(bytes: Long): String = String.format(Locale.US, "%.1f MB", bytes / 1048576.0)

fun formatDate(millis: Long): String {
    val cal = java.util.Calendar.getInstance()
    cal.timeInMillis = millis
    return String.format(
        Locale.US,
        "%d年%d月%d日 %02d:%02d",
        cal.get(java.util.Calendar.YEAR),
        cal.get(java.util.Calendar.MONTH) + 1,
        cal.get(java.util.Calendar.DAY_OF_MONTH),
        cal.get(java.util.Calendar.HOUR_OF_DAY),
        cal.get(java.util.Calendar.MINUTE)
    )
}

class Playlist(
    val id: String,
    var name: String,
    val trackIds: MutableList<String>,
    val favorites: Boolean = false,
    val builtin: Boolean = false
)

data class AlbumGroup(
    val key: String,
    val title: String,
    val artist: String,
    val year: Int,
    val genre: String,
    val tracks: List<Track>
) {
    val totalSeconds: Int get() = tracks.sumOf { it.durationSec }
}

data class ArtistGroup(val name: String, val albums: List<AlbumGroup>, val tracks: List<Track>) {
    val totalSeconds: Int get() = tracks.sumOf { it.durationSec }
}

data class GenreGroup(val name: String, val tracks: List<Track>) {
    val totalSeconds: Int get() = tracks.sumOf { it.durationSec }
}

data class SmartPlaylist(val kind: String, val name: String, val trackIds: List<String>, val subtitle: String)

class AppState(private val context: Context) {

    private val prefs = context.getSharedPreferences("vinyl_player_state", Context.MODE_PRIVATE)

    val tracks = mutableStateListOf<Track>()
    val playlists = mutableStateListOf<Playlist>()

    var favorites by mutableStateOf<Set<String>>(emptySet())
    var playCounts by mutableStateOf<Map<String, Int>>(emptyMap())
    var lastPlayed by mutableStateOf<Map<String, Long>>(emptyMap())

    var queue by mutableStateOf<List<String>>(emptyList())
    var queueIndex by mutableStateOf(0)
    var shuffle by mutableStateOf(false)
    var shuffledOrder by mutableStateOf<List<Int>>(emptyList())
    var repeatMode by mutableStateOf(RepeatMode.OFF)
    var playing by mutableStateOf(false)
    var positionSec by mutableStateOf(0)
    var sleepTimerMinutes by mutableStateOf(0)
    var sleepAfterCurrent by mutableStateOf(false)

    var onboarded by mutableStateOf(false)
    var lastTab by mutableStateOf(0)
    var tabVisibility by mutableStateOf(listOf("歌曲", "专辑", "艺术家", "音乐类型", "播放列表"))
    var libraryTabs by mutableStateOf(listOf("歌曲", "专辑", "艺术家", "音乐类型", "播放列表"))

    // Settings
    var rememberLastPage by mutableStateOf(true)
    var darkTheme by mutableStateOf(false)
    var themeStyle by mutableStateOf("Classic")
    var primaryColor by mutableStateOf("靛蓝")
    var accentColor by mutableStateOf("粉色")
    var tintNavBar by mutableStateOf(true)
    var tintShortcuts by mutableStateOf(true)
    var transparentControls by mutableStateOf(true)
    var classicNotification by mutableStateOf(false)
    var tintNotification by mutableStateOf(false)
    var nowPlayingAppearance by mutableStateOf("Card")
    var showSyncedLyrics by mutableStateOf(true)
    var animatePlayingIcon by mutableStateOf(false)
    var showTrackNumber by mutableStateOf(false)
    var defaultAction by mutableStateOf("播放")
    var autoDownload by mutableStateOf("仅在 WiFi 下")
    var duckOnFocusLoss by mutableStateOf(true)
    var gapless by mutableStateOf(false)
    var rememberShuffle by mutableStateOf(true)
    var replayGainMode by mutableStateOf("无")
    var replayGainPreamp by mutableStateOf("不启用")
    var recentInterval by mutableStateOf("本月")
    var historyInterval by mutableStateOf("本月")
    var notPlayedInterval by mutableStateOf("本月")
    var favoriteSongsEnabled by mutableStateOf(true)
    var skippedSongs by mutableStateOf(false)
    var crashReports by mutableStateOf(false)
    var syncQueueWithTags by mutableStateOf(false)
    var whiteListEnabled by mutableStateOf(false)
    var blacklist by mutableStateOf(
        listOf("/storage/emulated/0/Alarms", "/storage/emulated/0/Notifications", "/storage/emulated/0/Ringtones")
    )
    var coloredFooter by mutableStateOf(true)
    var introPage by mutableStateOf(0)
    var scanning by mutableStateOf(false)
    var lastPrimaryTab by mutableStateOf(0)

    val currentTrack: Track?
        get() = queue.getOrNull(queueIndex)?.let { id -> tracks.firstOrNull { it.id == id } }

    val queueTracks: List<Track>
        get() = queue.mapNotNull { id -> tracks.firstOrNull { it.id == id } }

    fun remainingSeconds(): Int {
        val list = queueTracks
        if (list.isEmpty()) return 0
        var total = 0
        for (i in queueIndex until list.size) {
            total += if (i == queueIndex) (list[i].durationSec - positionSec).coerceAtLeast(0) else list[i].durationSec
        }
        return total
    }

    // ---------------------------------------------------------------- library

    fun load() {
        if (tracks.isNotEmpty()) return
        val raw = AssetStore.readText(context, "music_library.json")
        val root = JSONObject(raw)
        val artists = root.getJSONArray("artists")
        var index = 0
        for (a in 0 until artists.length()) {
            val artistObj = artists.getJSONObject(a)
            val artistName = artistObj.getString("name")
            val albums = artistObj.getJSONArray("albums")
            for (b in 0 until albums.length()) {
                val albumObj = albums.getJSONObject(b)
                val albumTitle = albumObj.getString("title")
                val year = albumObj.optInt("year", 0)
                val genre = albumObj.optString("genre", "未知音乐类型")
                val albumArtist = albumObj.optString("albumArtist", artistName)
                val albumTracks = albumObj.getJSONArray("tracks")
                for (t in 0 until albumTracks.length()) {
                    val tr = albumTracks.getJSONObject(t)
                    val no = tr.optInt("no", t + 1)
                    val title = tr.getString("title")
                    val duration = tr.optInt("duration", 180)
                    val fileName = String.format(Locale.US, "%02d %s.mp3", no, title)
                    val size = (duration * 40000L) + (no * 1024L) + 200000L
                    tracks.add(
                        Track(
                            id = "t${index + 1}",
                            title = title,
                            artist = artistName,
                            album = albumTitle,
                            albumArtist = albumArtist,
                            trackNo = no,
                            discNo = 1,
                            durationSec = duration,
                            year = year,
                            genre = genre,
                            fileName = fileName,
                            sizeBytes = size,
                            addedAt = System.currentTimeMillis() - index * 4L * 86_400_000L,
                            modifiedAt = 1_760_000_000_000L - index * 43_200_000L
                        )
                    )
                    index++
                }
            }
        }
        val m3u = AssetStore.readText(context, "playlists.m3u")
        val parsed = parseM3u(m3u)
        for ((name, titles) in parsed) {
            val ids = titles.mapNotNull { title -> tracks.firstOrNull { it.title == title }?.id }
            playlists.add(Playlist(id = "p_${playlists.size + 1}", name = name, trackIds = ids.toMutableList()))
        }
        if (playlists.none { it.favorites }) {
            playlists.add(0, Playlist(id = "favorites", name = "收藏夹", trackIds = mutableListOf(), favorites = true))
        }
        restore()
    }

    private fun parseM3u(content: String): List<Pair<String, List<String>>> {
        val result = mutableListOf<Pair<String, List<String>>>()
        var currentName: String? = null
        val currentTitles = mutableListOf<String>()
        for (line in content.lines()) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("# 播放列表") || trimmed.startsWith("#播放列表") -> {
                    if (currentName != null) {
                        result.add(currentName!! to currentTitles.toList())
                        currentTitles.clear()
                    }
                    currentName = trimmed.substringAfter("：").substringAfter(":").trim().ifEmpty { "播放列表" }
                }
                trimmed.isEmpty() || trimmed.startsWith("#EXTM3U") || trimmed.startsWith("#EXTINF") -> {}
                trimmed.startsWith("#") -> {}
                else -> currentTitles.add(trimmed)
            }
        }
        if (currentName != null) result.add(currentName!! to currentTitles.toList())
        return result
    }

    fun albums(): List<AlbumGroup> =
        tracks.groupBy { it.album }
            .map { (album, list) ->
                AlbumGroup(
                    key = album,
                    title = album,
                    artist = list.first().albumArtist.ifEmpty { list.first().artist },
                    year = list.first().year,
                    genre = list.first().genre,
                    tracks = list.sortedBy { it.trackNo }
                )
            }
            .sortedBy { it.title }

    fun artists(): List<ArtistGroup> =
        tracks.groupBy { it.artist }
            .map { (artist, list) ->
                val albums = list.groupBy { it.album }.map { (album, albumTracks) ->
                    AlbumGroup(
                        key = album,
                        title = album,
                        artist = artist,
                        year = albumTracks.first().year,
                        genre = albumTracks.first().genre,
                        tracks = albumTracks.sortedBy { it.trackNo }
                    )
                }.sortedBy { it.year }
                ArtistGroup(artist, albums, list.sortedBy { it.title })
            }
            .sortedBy { it.name }

    fun genres(): List<GenreGroup> =
        tracks.groupBy { it.genre.ifEmpty { "未知音乐类型" } }
            .map { (genre, list) -> GenreGroup(genre, list.sortedBy { it.title }) }
            .sortedBy { it.name }

    fun smartPlaylists(): List<SmartPlaylist> {
        val now = System.currentTimeMillis()
        val monthAgo = now - 30L * 86_400_000L
        val recent = tracks.filter { it.addedAt >= monthAgo }.sortedByDescending { it.addedAt }.map { it.id }
        val history = tracks.filter { (lastPlayed[it.id] ?: 0L) >= monthAgo }
            .sortedByDescending { lastPlayed[it.id] ?: 0L }.map { it.id }
        val notPlayed = tracks.filter { (lastPlayed[it.id] ?: 0L) < monthAgo }.sortedByDescending { it.addedAt }.map { it.id }
        val result = mutableListOf<SmartPlaylist>()
        result.add(SmartPlaylist("recent", "最近添加", recent, recentInterval))
        result.add(SmartPlaylist("history", "播放历史", history, historyInterval))
        result.add(SmartPlaylist("notplayed", "最近未播放过", notPlayed, notPlayedInterval))
        if (favoriteSongsEnabled) {
            val mostPlayed = tracks.filter { (playCounts[it.id] ?: 0) > 0 }
                .sortedByDescending { playCounts[it.id] ?: 0 }.map { it.id }
            result.add(SmartPlaylist("favorite", "最喜爱的歌曲", mostPlayed, ""))
        }
        return result
    }

    fun tracksOf(ids: List<String>): List<Track> = ids.mapNotNull { id -> tracks.firstOrNull { it.id == id } }

    // ------------------------------------------------------------- playback

    fun playTrack(id: String, newQueue: List<String>? = null) {
        if (newQueue != null && newQueue.isNotEmpty()) {
            queue = newQueue
            queueIndex = newQueue.indexOf(id).coerceAtLeast(0)
            if (shuffle) rebuildShuffle()
        } else {
            val idx = queue.indexOf(id)
            queueIndex = if (idx >= 0) idx else 0
        }
        positionSec = 0
        playing = true
        playCounts = playCounts + (id to ((playCounts[id] ?: 0) + 1))
        lastPlayed = lastPlayed + (id to System.currentTimeMillis())
        PlayerBus.play(context)
        persist()
    }

    fun togglePlay() {
        if (currentTrack == null) {
            val first = tracks.firstOrNull() ?: return
            playTrack(first.id, tracks.map { it.id })
            return
        }
        playing = !playing
        if (playing) PlayerBus.play(context) else PlayerBus.pause()
    }

    fun next(manual: Boolean = true) {
        val list = queueTracks
        if (list.isEmpty()) return
        if (manual && repeatMode == RepeatMode.ONE) {
            positionSec = 0
            PlayerBus.play(context)
            return
        }
        queueIndex = nextIndex(manual)
        positionSec = 0
        playing = true
        PlayerBus.play(context)
        val t = currentTrack
        if (t != null) {
            playCounts = playCounts + (t.id to ((playCounts[t.id] ?: 0) + 1))
            lastPlayed = lastPlayed + (t.id to System.currentTimeMillis())
        }
        persist()
    }

    private fun nextIndex(manual: Boolean): Int {
        val size = queue.size
        if (size == 0) return 0
        return if (shuffle) {
            val order = shuffledOrder.ifEmpty { (0 until size).toList() }
            val pos = order.indexOf(queueIndex)
            val nextPos = pos + 1
            if (nextPos >= order.size) {
                if (repeatMode == RepeatMode.ALL || manual) order.first() else queueIndex
            } else order[nextPos]
        } else {
            if (queueIndex + 1 >= size) {
                if (repeatMode == RepeatMode.ALL || manual) 0 else queueIndex
            } else queueIndex + 1
        }
    }

    fun previous() {
        if (positionSec > 4) {
            positionSec = 0
            PlayerBus.play(context)
            return
        }
        val size = queue.size
        if (size == 0) return
        queueIndex = if (queueIndex - 1 < 0) size - 1 else queueIndex - 1
        positionSec = 0
        playing = true
        PlayerBus.play(context)
        persist()
    }

    fun seekTo(seconds: Int) {
        positionSec = seconds.coerceIn(0, (currentTrack?.durationSec ?: 0))
    }

    fun cycleRepeat() {
        repeatMode = when (repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        persist()
    }

    fun toggleShuffle() {
        shuffle = !shuffle
        if (shuffle) rebuildShuffle() else shuffledOrder = emptyList()
        persist()
    }

    private fun rebuildShuffle() {
        shuffledOrder = queue.indices.shuffled()
    }

    fun tick() {
        if (!playing) return
        val track = currentTrack ?: return
        positionSec += 1
        if (positionSec >= track.durationSec) {
            if (repeatMode == RepeatMode.ONE) {
                positionSec = 0
            } else {
                val isLast = queueIndex >= queue.size - 1
                if (isLast && repeatMode == RepeatMode.OFF && !shuffle) {
                    positionSec = track.durationSec
                    playing = false
                    PlayerBus.pause()
                    if (sleepAfterCurrent) {
                        sleepAfterCurrent = false
                        sleepTimerMinutes = 0
                    }
                } else {
                    next(manual = false)
                }
            }
        }
        if (sleepTimerMinutes > 0) {
            sleepTimerTicks++
            if (sleepTimerTicks >= 60) {
                sleepTimerTicks = 0
                sleepTimerMinutes -= 1
                if (sleepTimerMinutes == 0) {
                    playing = false
                    PlayerBus.pause()
                    if (sleepAfterCurrent) sleepAfterCurrent = false
                }
            }
        }
    }

    private var sleepTimerTicks = 0

    fun startSleepTimer(minutes: Int, afterCurrent: Boolean) {
        sleepAfterCurrent = afterCurrent
        if (!afterCurrent) {
            sleepTimerMinutes = minutes
            sleepTimerTicks = 0
        }
    }

    fun jumpTo(index: Int) {
        if (index !in queue.indices) return
        queueIndex = index
        positionSec = 0
        playing = true
        PlayerBus.play(context)
        val t = currentTrack
        if (t != null) {
            playCounts = playCounts + (t.id to ((playCounts[t.id] ?: 0) + 1))
            lastPlayed = lastPlayed + (t.id to System.currentTimeMillis())
        }
        persist()
    }

    fun clearQueue() {
        queue = emptyList()
        queueIndex = 0
        playing = false
        positionSec = 0
        PlayerBus.pause()
        persist()
    }

    fun addToQueue(ids: List<String>, next: Boolean = false) {
        if (queue.isEmpty()) {
            queue = ids
            queueIndex = 0
        } else if (next) {
            val head = queue.take(queueIndex + 1)
            val tail = queue.drop(queueIndex + 1)
            queue = head + ids + tail
        } else {
            queue = queue + ids
        }
        persist()
    }

    fun removeFromQueue(index: Int) {
        if (index !in queue.indices) return
        queue = queue.toMutableList().also { it.removeAt(index) }
        if (queueIndex >= queue.size) queueIndex = (queue.size - 1).coerceAtLeast(0)
        persist()
    }

    // ------------------------------------------------------------ playlists

    fun createPlaylist(name: String, ids: List<String> = emptyList()): Playlist {
        val playlist = Playlist(id = "p_${System.currentTimeMillis()}", name = name, trackIds = ids.toMutableList())
        playlists.add(playlist)
        persist()
        return playlist
    }

    fun addToPlaylist(playlistId: String, ids: List<String>) {
        val playlist = playlists.firstOrNull { it.id == playlistId } ?: return
        for (id in ids) if (!playlist.trackIds.contains(id)) playlist.trackIds.add(id)
        playlists.toList()
        persist()
    }

    fun removeFromPlaylist(playlistId: String, ids: List<String>) {
        val playlist = playlists.firstOrNull { it.id == playlistId } ?: return
        playlist.trackIds.removeAll(ids)
        persist()
    }

    fun renamePlaylist(playlistId: String, name: String) {
        playlists.firstOrNull { it.id == playlistId }?.name = name
        persist()
    }

    fun deletePlaylist(playlistId: String) {
        playlists.removeAll { it.id == playlistId }
        persist()
    }

    fun toggleFavorite(id: String) {
        favorites = if (favorites.contains(id)) favorites - id else favorites + id
        val fav = playlists.firstOrNull { it.favorites }
        if (fav != null) {
            if (favorites.contains(id)) {
                if (!fav.trackIds.contains(id)) fav.trackIds.add(id)
            } else fav.trackIds.remove(id)
        }
        persist()
    }

    fun isFavorite(id: String): Boolean = favorites.contains(id)

    fun favoritesPlaylist(): Playlist? = playlists.firstOrNull { it.favorites }

    // -------------------------------------------------------------- editing

    fun updateTags(id: String, title: String, album: String, artist: String, genre: String, year: Int, trackNo: Int, discNo: Int, lyrics: String) {
        val index = tracks.indexOfFirst { it.id == id }
        if (index < 0) return
        val old = tracks[index]
        tracks[index] = old.copy(
            title = title.ifBlank { old.title },
            album = album.ifBlank { old.album },
            artist = artist.lineSequence().firstOrNull()?.trim().orEmpty().ifBlank { old.artist },
            albumArtist = artist.lineSequence().firstOrNull()?.trim().orEmpty().ifBlank { old.albumArtist },
            genre = genre.lineSequence().firstOrNull()?.trim().orEmpty().ifBlank { old.genre },
            year = if (year > 0) year else old.year,
            trackNo = if (trackNo > 0) trackNo else old.trackNo,
            discNo = if (discNo > 0) discNo else old.discNo,
            lyrics = lyrics
        )
        persist()
    }

    fun deleteTracks(ids: List<String>) {
        tracks.removeAll { ids.contains(it.id) }
        playlists.forEach { it.trackIds.removeAll(ids) }
        queue = queue.filterNot { ids.contains(it) }
        if (queueIndex >= queue.size) queueIndex = (queue.size - 1).coerceAtLeast(0)
        persist()
    }

    // ----------------------------------------------------------- persistence

    fun persist() {
        val obj = JSONObject()
        obj.put("onboarded", onboarded)
        obj.put("favorites", JSONArray(favorites.toList()))
        val counts = JSONObject()
        playCounts.forEach { (k, v) -> counts.put(k, v) }
        obj.put("playCounts", counts)
        val played = JSONObject()
        lastPlayed.forEach { (k, v) -> played.put(k, v) }
        obj.put("lastPlayed", played)
        obj.put("queue", JSONArray(queue))
        obj.put("queueIndex", queueIndex)
        obj.put("repeat", repeatMode.name)
        obj.put("shuffle", shuffle)
        obj.put("lastTab", lastTab)
        obj.put("tabVisibility", JSONArray(tabVisibility))
        val savedPlaylists = JSONArray()
        playlists.forEach { p ->
            val o = JSONObject()
            o.put("id", p.id)
            o.put("name", p.name)
            o.put("favorites", p.favorites)
            o.put("tracks", JSONArray(p.trackIds))
            savedPlaylists.put(o)
        }
        obj.put("playlists", savedPlaylists)
        obj.put("darkTheme", darkTheme)
        obj.put("themeStyle", themeStyle)
        obj.put("primaryColor", primaryColor)
        obj.put("accentColor", accentColor)
        obj.put("tintNavBar", tintNavBar)
        obj.put("tintShortcuts", tintShortcuts)
        obj.put("transparentControls", transparentControls)
        obj.put("classicNotification", classicNotification)
        obj.put("tintNotification", tintNotification)
        obj.put("nowPlayingAppearance", nowPlayingAppearance)
        obj.put("showSyncedLyrics", showSyncedLyrics)
        obj.put("animatePlayingIcon", animatePlayingIcon)
        obj.put("showTrackNumber", showTrackNumber)
        obj.put("defaultAction", defaultAction)
        obj.put("autoDownload", autoDownload)
        obj.put("duckOnFocusLoss", duckOnFocusLoss)
        obj.put("gapless", gapless)
        obj.put("rememberShuffle", rememberShuffle)
        obj.put("replayGainMode", replayGainMode)
        obj.put("replayGainPreamp", replayGainPreamp)
        obj.put("recentInterval", recentInterval)
        obj.put("historyInterval", historyInterval)
        obj.put("notPlayedInterval", notPlayedInterval)
        obj.put("favoriteSongsEnabled", favoriteSongsEnabled)
        obj.put("skippedSongs", skippedSongs)
        obj.put("crashReports", crashReports)
        obj.put("syncQueueWithTags", syncQueueWithTags)
        obj.put("whiteListEnabled", whiteListEnabled)
        obj.put("blacklist", JSONArray(blacklist))
        obj.put("rememberLastPage", rememberLastPage)
        obj.put("coloredFooter", coloredFooter)
        prefs.edit().putString("state", obj.toString()).apply()
    }

    private fun restore() {
        val raw = prefs.getString("state", null) ?: run {
            persist()
            return
        }
        try {
            val obj = JSONObject(raw)
            onboarded = obj.optBoolean("onboarded", false)
            val favs = obj.optJSONArray("favorites")
            if (favs != null) favorites = (0 until favs.length()).map { favs.getString(it) }.toSet()
            val counts = obj.optJSONObject("playCounts")
            if (counts != null) {
                val map = mutableMapOf<String, Int>()
                counts.keys().forEach { key -> map[key] = counts.getInt(key) }
                playCounts = map
            }
            val played = obj.optJSONObject("lastPlayed")
            if (played != null) {
                val map = mutableMapOf<String, Long>()
                played.keys().forEach { key -> map[key] = played.getLong(key) }
                lastPlayed = map
            }
            val q = obj.optJSONArray("queue")
            if (q != null) queue = (0 until q.length()).map { q.getString(it) }
            queueIndex = obj.optInt("queueIndex", 0)
            repeatMode = runCatching { RepeatMode.valueOf(obj.optString("repeat", "OFF")) }.getOrDefault(RepeatMode.OFF)
            shuffle = obj.optBoolean("shuffle", false)
            lastTab = obj.optInt("lastTab", 0)
            val vis = obj.optJSONArray("tabVisibility")
            if (vis != null) tabVisibility = (0 until vis.length()).map { vis.getString(it) }
            val savedPlaylists = obj.optJSONArray("playlists")
            if (savedPlaylists != null) {
                val restored = mutableListOf<Playlist>()
                for (i in 0 until savedPlaylists.length()) {
                    val o = savedPlaylists.getJSONObject(i)
                    val ids = o.optJSONArray("tracks")
                    val list = mutableListOf<String>()
                    if (ids != null) for (j in 0 until ids.length()) list.add(ids.getString(j))
                    restored.add(
                        Playlist(
                            id = o.optString("id", "p_$i"),
                            name = o.optString("name", "播放列表"),
                            trackIds = list,
                            favorites = o.optBoolean("favorites", false)
                        )
                    )
                }
                if (restored.isNotEmpty()) {
                    playlists.clear()
                    playlists.addAll(restored)
                }
            }
            darkTheme = obj.optBoolean("darkTheme", false)
            themeStyle = obj.optString("themeStyle", "Classic")
            primaryColor = obj.optString("primaryColor", "靛蓝")
            accentColor = obj.optString("accentColor", "粉色")
            tintNavBar = obj.optBoolean("tintNavBar", true)
            tintShortcuts = obj.optBoolean("tintShortcuts", true)
            transparentControls = obj.optBoolean("transparentControls", true)
            classicNotification = obj.optBoolean("classicNotification", false)
            tintNotification = obj.optBoolean("tintNotification", false)
            nowPlayingAppearance = obj.optString("nowPlayingAppearance", "Card")
            showSyncedLyrics = obj.optBoolean("showSyncedLyrics", true)
            animatePlayingIcon = obj.optBoolean("animatePlayingIcon", false)
            showTrackNumber = obj.optBoolean("showTrackNumber", false)
            defaultAction = obj.optString("defaultAction", "播放")
            autoDownload = obj.optString("autoDownload", "仅在 WiFi 下")
            duckOnFocusLoss = obj.optBoolean("duckOnFocusLoss", true)
            gapless = obj.optBoolean("gapless", false)
            rememberShuffle = obj.optBoolean("rememberShuffle", true)
            replayGainMode = obj.optString("replayGainMode", "无")
            replayGainPreamp = obj.optString("replayGainPreamp", "不启用")
            recentInterval = obj.optString("recentInterval", "本月")
            historyInterval = obj.optString("historyInterval", "本月")
            notPlayedInterval = obj.optString("notPlayedInterval", "本月")
            favoriteSongsEnabled = obj.optBoolean("favoriteSongsEnabled", true)
            skippedSongs = obj.optBoolean("skippedSongs", false)
            crashReports = obj.optBoolean("crashReports", false)
            syncQueueWithTags = obj.optBoolean("syncQueueWithTags", false)
            whiteListEnabled = obj.optBoolean("whiteListEnabled", false)
            val bl = obj.optJSONArray("blacklist")
            if (bl != null) blacklist = (0 until bl.length()).map { bl.getString(it) }
            rememberLastPage = obj.optBoolean("rememberLastPage", true)
            coloredFooter = obj.optBoolean("coloredFooter", true)
        } catch (_: Exception) {
            persist()
        }
    }
}

/** Minimal audio engine: loops the bundled sample so playback keeps sounding while a track runs. */
object PlayerBus {
    private var player: android.media.MediaPlayer? = null

    fun play(context: Context) {
        try {
            if (player == null) {
                val afd = context.assets.openFd("ambient.wav")
                player = android.media.MediaPlayer().apply {
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    isLooping = true
                    prepare()
                }
            }
            player?.start()
        } catch (_: Exception) {
        }
    }

    fun pause() {
        try {
            player?.pause()
        } catch (_: Exception) {
        }
    }

    fun stop() {
        try {
            player?.stop()
            player?.release()
        } catch (_: Exception) {
        }
        player = null
    }
}
