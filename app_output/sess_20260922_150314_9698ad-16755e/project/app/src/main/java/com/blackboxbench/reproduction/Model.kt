package com.blackboxbench.reproduction

import android.content.Context

/** 一首曲目。标签可被用户编辑，编辑结果持久保存并参与所有聚合视图。 */
data class Song(
    val id: Long,
    var title: String,
    var artist: String,
    var album: String,
    var trackNo: Int,
    val duration: Int, // 秒
    var year: Int,
    var genre: String,
)

data class Playlist(
    val id: Long,
    var name: String,
    val songIds: MutableList<Long> = mutableListOf(),
)

data class AppSettings(
    var pauseOnHeadsetDisconnect: Boolean = true,
    var ignoreShortAudio: Boolean = false,
    var defaultPlayMode: String = "顺序播放",
    var theme: String = "浅色",
)

/** 从 assets/music_library.json 装载虚构曲库；同一 album 名自动聚合。 */
object MusicLibrary {

    fun load(context: Context): MutableList<Song> {
        val text = context.assets.open("music_library.json").bufferedReader().use { it.readText() }
        val root = org.json.JSONObject(text)
        val songs = mutableListOf<Song>()
        val artists = root.getJSONArray("artists")
        for (i in 0 until artists.length()) {
            val artist = artists.getJSONObject(i)
            val artistName = artist.getString("name")
            val albums = artist.getJSONArray("albums")
            for (j in 0 until albums.length()) {
                val album = albums.getJSONObject(j)
                val albumTitle = album.getString("title")
                val year = album.getInt("year")
                val genre = album.getString("genre")
                val tracks = album.getJSONArray("tracks")
                for (k in 0 until tracks.length()) {
                    val t = tracks.getJSONObject(k)
                    songs.add(
                        Song(
                            id = songs.size.toLong(),
                            title = t.getString("title"),
                            artist = artistName,
                            album = albumTitle,
                            trackNo = t.getInt("no"),
                            duration = t.getInt("duration"),
                            year = year,
                            genre = genre,
                        )
                    )
                }
            }
        }
        return songs
    }

    /** 专辑 -> 封面图（素材包共 5 张封面，6 张专辑轮转映射）。 */
    fun coverFor(album: String): String {
        val covers = listOf("morning", "walk", "room", "baking", "notifications")
        return "covers/${covers[kotlin.math.abs(album.hashCode()) % covers.size]}.png"
    }

    /** 曲目 -> 音频素材（3 个 WAV 按曲目 id 轮转映射）。 */
    fun audioFor(songId: Long): String {
        val wavs = listOf("ambient", "notification", "success")
        return "audio/${wavs[(songId % wavs.size).toInt()]}.wav"
    }

    fun albumsOf(songs: List<Song>): List<Pair<String, List<Song>>> =
        songs.groupBy { it.album }
            .map { (album, list) -> album to list.sortedBy { it.trackNo } }
            .sortedBy { (album, _) -> album }

    fun artistsOf(songs: List<Song>): List<Pair<String, List<Song>>> =
        songs.groupBy { it.artist }
            .map { (artist, list) -> artist to list.sortedBy { it.album } }
            .sortedBy { (artist, _) -> artist }

    fun genresOf(songs: List<Song>): List<Pair<String, List<Song>>> =
        songs.groupBy { it.genre }
            .map { (genre, list) -> genre to list.sortedBy { it.title } }
            .sortedBy { (genre, _) -> genre }
}

fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}
