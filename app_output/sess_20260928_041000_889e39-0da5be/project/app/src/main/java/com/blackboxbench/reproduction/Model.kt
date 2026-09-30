package com.blackboxbench.reproduction

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val trackNo: Int,
    val durationSec: Int,
    val year: Int,
    val genre: String
)

data class Album(
    val title: String,
    val artist: String,
    val year: Int,
    val genre: String,
    val songs: List<Song>
) {
    val durationSec: Int get() = songs.sumOf { it.durationSec }
}

data class Artist(val name: String, val albums: List<Album>)

data class Genre(val name: String, val songs: List<Song>)

enum class SmartKind(val label: String) {
    RECENTLY_ADDED("最近添加"),
    PLAY_HISTORY("播放历史"),
    RECENTLY_NOT_PLAYED("最近未播放过"),
    MOST_PLAYED("最喜爱的歌曲")
}

data class SmartPlaylist(val kind: SmartKind, val songs: List<Song>)

class UserPlaylist(val id: Long, val name: String, val songIds: MutableList<Long>)

fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}

class MusicRepository(private val context: Context) {

    val prefs = context.getSharedPreferences("vinyl_reproduction", Context.MODE_PRIVATE)

    var dataVersion by mutableIntStateOf(0)
        private set

    val songs: MutableList<Song> = loadLibrary().toMutableList()
    var albums: List<Album> = emptyList()
        private set
    var artists: List<Artist> = emptyList()
        private set
    var genres: List<Genre> = emptyList()
        private set

    val favorites = linkedSetOf<Long>()
    val playlists = mutableListOf<UserPlaylist>()
    val playCounts = mutableMapOf<Long, Int>()
    val lastPlayedAt = mutableMapOf<Long, Long>()
    val addedAt = mutableMapOf<Long, Long>()
    var nextPlaylistId = 1L

    init {
        songs.forEachIndexed { i, s -> addedAt[s.id] = (i + 1).toLong() }
        recomputeAggregates()
        loadUserState()
    }

    fun recomputeAggregates() {
        albums = songs.groupBy { it.album }
            .map { (title, list) ->
                val first = list.first()
                Album(title, first.artist, first.year, first.genre, list.sortedBy { it.trackNo })
            }
            .sortedBy { it.title }
        artists = songs.groupBy { it.artist }
            .map { (name, list) ->
                Artist(name, list.groupBy { it.album }.map { (t, l) ->
                    val f = l.first()
                    Album(t, f.artist, f.year, f.genre, l.sortedBy { it.trackNo })
                }.sortedBy { it.title })
            }
            .sortedBy { it.name }
        genres = songs.groupBy { it.genre }
            .map { (name, list) -> Genre(name, list.sortedBy { it.title }) }
            .sortedBy { it.name }
    }

    fun updateSong(updated: Song) {
        val idx = songs.indexOfFirst { it.id == updated.id }
        if (idx < 0) return
        songs[idx] = updated
        recomputeAggregates()
        notifyChanged()
    }

    fun notifyChanged() {
        dataVersion++
    }

    fun songById(id: Long): Song? = songs.firstOrNull { it.id == id }

    fun songsByIds(ids: List<Long>): List<Song> = ids.mapNotNull { songById(it) }

    fun playlistById(id: Long): UserPlaylist? = playlists.firstOrNull { it.id == id }

    fun smartPlaylist(kind: SmartKind): SmartPlaylist = when (kind) {
        SmartKind.RECENTLY_ADDED -> SmartPlaylist(kind, songs.sortedByDescending { addedAt[it.id] ?: 0L })
        SmartKind.PLAY_HISTORY -> SmartPlaylist(
            kind,
            lastPlayedAt.entries.sortedByDescending { it.value }.mapNotNull { songById(it.key) }
        )
        SmartKind.RECENTLY_NOT_PLAYED -> SmartPlaylist(
            kind,
            songs.filter { (playCounts[it.id] ?: 0) == 0 }
        )
        SmartKind.MOST_PLAYED -> SmartPlaylist(
            kind,
            playCounts.entries.filter { it.value > 0 }
                .sortedByDescending { it.value }.mapNotNull { songById(it.key) }
        )
    }

    val favoriteSongs: List<Song> get() = songsByIds(favorites.toList())

    fun toggleFavorite(id: Long) {
        if (!favorites.remove(id)) favorites.add(id)
        persistFavorites()
        notifyChanged()
    }

    fun isFavorite(id: Long): Boolean = favorites.contains(id)

    fun recordPlay(id: Long) {
        playCounts[id] = (playCounts[id] ?: 0) + 1
        lastPlayedAt[id] = System.currentTimeMillis()
        persistPlayStats()
        notifyChanged()
    }

    fun createPlaylist(name: String, songIds: List<Long> = emptyList()): UserPlaylist {
        val p = UserPlaylist(nextPlaylistId++, name, songIds.toMutableList())
        playlists.add(p)
        persistPlaylists()
        notifyChanged()
        return p
    }

    fun addToPlaylist(playlistId: Long, songIds: List<Long>) {
        val p = playlistById(playlistId) ?: return
        songIds.forEach { if (!p.songIds.contains(it)) p.songIds.add(it) }
        persistPlaylists()
        notifyChanged()
    }

    fun removeFromPlaylist(playlistId: Long, songId: Long) {
        playlistById(playlistId)?.songIds?.remove(songId)
        persistPlaylists()
        notifyChanged()
    }

    fun renamePlaylist(playlistId: Long, name: String) {
        val p = playlistById(playlistId) ?: return
        val idx = playlists.indexOf(p)
        playlists[idx] = UserPlaylist(p.id, name, p.songIds)
        persistPlaylists()
        notifyChanged()
    }

    fun deletePlaylist(playlistId: Long) {
        playlists.removeAll { it.id == playlistId }
        persistPlaylists()
        notifyChanged()
    }

    fun duplicatePlaylist(playlistId: Long, name: String) {
        val p = playlistById(playlistId) ?: return
        createPlaylist(name, p.songIds.toList())
    }

    // ---- persistence -------------------------------------------------

    private fun loadLibrary(): List<Song> {
        val text = context.assets.open("music_library.json").bufferedReader().use { it.readText() }
        val root = JSONObject(text)
        val artists = root.getJSONArray("artists")
        val out = ArrayList<Song>()
        var id = 1L
        for (i in 0 until artists.length()) {
            val a = artists.getJSONObject(i)
            val artistName = a.getString("name")
            val albums = a.getJSONArray("albums")
            for (j in 0 until albums.length()) {
                val al = albums.getJSONObject(j)
                val albumTitle = al.getString("title")
                val year = al.optInt("year", 0)
                val genre = al.optString("genre", "")
                val tracks = al.getJSONArray("tracks")
                for (k in 0 until tracks.length()) {
                    val t = tracks.getJSONObject(k)
                    out.add(
                        Song(
                            id = id++,
                            title = t.getString("title"),
                            artist = artistName,
                            album = albumTitle,
                            trackNo = t.optInt("no", k + 1),
                            durationSec = t.optInt("duration", 0),
                            year = year,
                            genre = genre
                        )
                    )
                }
            }
        }
        return out
    }

    private fun loadUserState() {
        prefs.getString("favorites", "")?.split(",")?.forEach {
            it.toLongOrNull()?.let { v -> favorites.add(v) }
        }
        prefs.getString("play_counts", "")?.split(";")?.forEach {
            val parts = it.split(":")
            if (parts.size == 2) {
                parts[0].toLongOrNull()?.let { id -> parts[1].toIntOrNull()?.let { c -> playCounts[id] = c } }
            }
        }
        prefs.getString("last_played", "")?.split(";")?.forEach {
            val parts = it.split(":")
            if (parts.size == 2) {
                parts[0].toLongOrNull()?.let { id -> parts[1].toLongOrNull()?.let { t -> lastPlayedAt[id] = t } }
            }
        }
        val pl = prefs.getString("playlists", null)
        if (pl.isNullOrEmpty()) {
            seedPlaylists()
        } else {
            try {
                val arr = JSONArray(pl)
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val ids = o.optJSONArray("songs") ?: JSONArray()
                    val list = ArrayList<Long>()
                    for (j in 0 until ids.length()) list.add(ids.getLong(j))
                    playlists.add(UserPlaylist(o.getLong("id"), o.getString("name"), list))
                    nextPlaylistId = maxOf(nextPlaylistId, o.getLong("id") + 1)
                }
            } catch (_: Exception) {
                seedPlaylists()
            }
        }
    }

    private fun seedPlaylists() {
        val byTitle = songs.associateBy { it.title }
        val morning = listOf("起飞检查单", "跑道灯", "早春序曲", "开机音", "夜间巡航", "橡树下的信")
        val night = listOf("载波", "快门开启", "星轨", "静默频道", "隐藏关卡")
        playlists.add(UserPlaylist(nextPlaylistId++, "晨间通勤", morning.mapNotNull { byTitle[it]?.id }.toMutableList()))
        playlists.add(UserPlaylist(nextPlaylistId++, "深夜编码", night.mapNotNull { byTitle[it]?.id }.toMutableList()))
        persistPlaylists()
    }

    private fun persistFavorites() {
        prefs.edit().putString("favorites", favorites.joinToString(",")).apply()
    }

    private fun persistPlayStats() {
        prefs.edit()
            .putString("play_counts", playCounts.entries.joinToString(";") { "${it.key}:${it.value}" })
            .putString("last_played", lastPlayedAt.entries.joinToString(";") { "${it.key}:${it.value}" })
            .apply()
    }

    private fun persistPlaylists() {
        val arr = JSONArray()
        playlists.forEach { p ->
            val o = JSONObject()
            o.put("id", p.id)
            o.put("name", p.name)
            o.put("songs", JSONArray(p.songIds))
            arr.put(o)
        }
        prefs.edit().putString("playlists", arr.toString()).apply()
    }
}
