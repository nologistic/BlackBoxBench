package com.blackboxbench.reproduction

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

// ---------- 数据模型 ----------

data class Song(
    val id: Int,
    val title: String,
    val artist: String,
    val album: String,
    val track: Int,
    val duration: Int, // 秒
    val genre: String,
    val year: Int,
    val fileName: String,
    val fileSize: String,
    val bitrate: String = "320kb/s",
    val sampleRate: String = "44100Hz",
    val format: String = "Mp3",
    val greenCover: Boolean = false
)

fun fmtDuration(sec: Int): String = "%d:%02d".format(sec / 60, sec % 60)

object Library {
    val baseSongs: List<Song> = buildList {
        add(Song(0, "sample_audio", "BlackBoxBench", "", 0, 1, "", 0,
            "sample_audio.wav", "0.03MB", "352kb/s", "22050Hz", "Wav", greenCover = true))
        var id = 1
        fun album(artist: String, album: String, genre: String, year: Int,
                  durs: List<Int>, sizes: List<String>, fileBase: Int) {
            durs.forEachIndexed { i, d ->
                add(Song(id, "Track ${i + 1}", artist, album, i + 1, d, genre, year,
                    "%02d %s - Track %d.mp3".format(fileBase + i, album, i + 1), sizes[i]))
                id++
            }
        }
        album("Ana Vela", "Copper Sky", "Latin Jazz", 2023,
            listOf(124, 103, 164), listOf("1.95MB", "1.62MB", "2.58MB"), 15)
        album("Mei & The Foxes", "Paper Lanterns", "Indie Folk", 2024,
            listOf(96, 117, 105, 134), listOf("782.6KB", "1.82MB", "2.52MB", "1.16MB"), 5)
        album("Orbit Nine", "Deep Field", "Ambient", 2025,
            listOf(172, 185, 158), listOf("1.42MB", "2.31MB", "1.97MB"), 9)
        album("The Chromatics", "Neon Harbour", "Synthwave", 2026,
            listOf(101, 118, 109, 127), listOf("1.77MB", "2.44MB", "2.03MB", "2.4MB"), 1)
        album("The Wilhelms", "Kitchen Tapes", "Lo-fi", 2022,
            listOf(143, 131, 152), listOf("1.68MB", "1.54MB", "1.79MB"), 12)
    }

    val albums = listOf("Copper Sky", "Deep Field", "Kitchen Tapes", "Neon Harbour", "Paper Lanterns")
    val artists = listOf("Ana Vela", "Mei & The Foxes", "Orbit Nine", "The Chromatics", "The Wilhelms")
    val genreOrder = listOf("", "Ambient", "Indie Folk", "Latin Jazz", "Lo-fi", "Synthwave")

    fun albumOf(name: String) = baseSongs.filter { it.album == name }
    fun albumArtist(name: String) = baseSongs.firstOrNull { it.album == name }?.artist ?: ""
    fun artistOf(name: String) = baseSongs.filter { it.artist == name }
    fun genreOf(g: String) = baseSongs.filter { it.genre == g }
}

// ---------- 持久化 ----------

class Store(context: Context) {
    private val p = context.getSharedPreferences("vinyl", Context.MODE_PRIVATE)

    var onboardingDone by mutableStateOf(p.getBoolean("onboardingDone", false))
        private set
    fun setOnboardingDone() { onboardingDone = true; p.edit().putBoolean("onboardingDone", true).apply() }

    val favorites = mutableStateListOf<Int>().also {
        it.addAll(p.getStringSet("favorites", emptySet())!!.map { s -> s.toInt() })
    }
    fun toggleFavorite(id: Int) {
        if (favorites.contains(id)) favorites.remove(id) else favorites.add(id)
        p.edit().putStringSet("favorites", favorites.map { it.toString() }.toSet()).apply()
    }

    val history = mutableStateListOf<Int>().also { list ->
        p.getString("history", "")!!.split(",").filter { it.isNotEmpty() }.forEach { list.add(it.toInt()) }
    }
    val playCounts = mutableMapOf<Int, Int>().also { m ->
        p.getString("playCounts", "")!!.split(",").filter { it.contains(":") }
            .forEach { val (k, v) = it.split(":"); m[k.toInt()] = v.toInt() }
    }
    fun recordPlay(id: Int) {
        history.remove(id); history.add(0, id)
        playCounts[id] = (playCounts[id] ?: 0) + 1
        p.edit()
            .putString("history", history.joinToString(","))
            .putString("playCounts", playCounts.map { "${it.key}:${it.value}" }.joinToString(","))
            .apply()
    }

    val deleted = mutableStateListOf<Int>().also {
        it.addAll(p.getStringSet("deleted", emptySet())!!.map { s -> s.toInt() })
    }
    fun deleteSong(id: Int) {
        deleted.add(id); favorites.remove(id)
        p.edit().putStringSet("deleted", deleted.map { it.toString() }.toSet()).apply()
    }

    // 标签编辑覆盖: id -> title|artist|album|genre
    private val tagOverrides = mutableMapOf<Int, List<String>>().also { m ->
        p.getString("tags", "")!!.split(";").filter { it.contains("|") }.forEach {
            val parts = it.split("|"); m[parts[0].toInt()] = parts.drop(1)
        }
    }
    fun tagOverride(id: Int) = tagOverrides[id]
    fun saveTags(id: Int, title: String, artist: String, album: String, genre: String) {
        tagOverrides[id] = listOf(title, artist, album, genre)
        p.edit().putString("tags", tagOverrides.map { e -> (listOf(e.key.toString()) + e.value).joinToString("|") }
            .joinToString(";")).apply()
    }

    var lastTab by mutableStateOf(p.getInt("lastTab", 0))
        private set
    fun saveLastTab(t: Int) { lastTab = t; p.edit().putInt("lastTab", t).apply() }

    fun getBool(key: String, def: Boolean) = p.getBoolean(key, def)
    fun setBool(key: String, v: Boolean) { p.edit().putBoolean(key, v).apply() }
    fun getStr(key: String, def: String) = p.getString(key, def)!!
    fun setStr(key: String, v: String) { p.edit().putString(key, v).apply() }

    fun songs(): List<Song> = Library.baseSongs.filter { !deleted.contains(it.id) }.map { s ->
        val o = tagOverrides[s.id]
        if (o != null) s.copy(title = o[0], artist = o[1], album = o[2], genre = o[3]) else s
    }
}

// ---------- 播放器 ----------

object Player {
    val queue = mutableStateListOf<Int>()
    var pos by mutableStateOf(0)
    var progress by mutableStateOf(0)
    var playing by mutableStateOf(false)
    var shuffle by mutableStateOf(false)
    var repeatMode by mutableStateOf(0) // 0 关 1 全部 2 单曲
    var rotation by mutableStateOf(0f)
    var sleepMinutes by mutableStateOf(-1)
    var sleepFinishCurrent by mutableStateOf(false)

    lateinit var store: Store

    fun init(s: Store) {
        store = s
        val q = s.getStr("queue", "")
        if (q.isNotEmpty()) {
            queue.addAll(q.split(",").map { it.toInt() })
            pos = s.getStr("queuePos", "0").toInt().coerceIn(0, queue.size - 1)
            progress = s.getStr("progress", "0").toInt()
        }
        shuffle = s.getBool("shuffle", false)
        repeatMode = s.getStr("repeat", "0").toInt()
        playing = false
    }

    fun persist() {
        store.setStr("queue", queue.joinToString(","))
        store.setStr("queuePos", pos.toString())
        store.setStr("progress", progress.toString())
        store.setBool("shuffle", shuffle)
        store.setStr("repeat", repeatMode.toString())
    }

    fun songs() = queue.mapNotNull { id -> store.songs().firstOrNull { it.id == id } }
    fun current(): Song? = queue.getOrNull(pos)?.let { id -> store.songs().firstOrNull { it.id == id } }
    fun duration() = current()?.duration ?: 0

    fun playList(list: List<Song>, start: Song, shuffled: Boolean = false) {
        queue.clear()
        queue.addAll(list.map { it.id })
        if (shuffled) {
            queue.shuffle()
            queue.remove(start.id); queue.add(0, start.id)
        }
        pos = queue.indexOf(start.id).coerceAtLeast(0)
        progress = 0
        playing = true
        rotation = 0f
        store.recordPlay(start.id)
        persist()
    }

    fun playSong(song: Song) {
        val all = store.songs()
        playList(all, song)
    }

    fun toggle() { if (queue.isNotEmpty()) { playing = !playing; persist() } }

    fun next(auto: Boolean = false) {
        if (queue.isEmpty()) return
        if (auto && repeatMode == 2) { progress = 0; return }
        if (pos < queue.size - 1) {
            pos++; progress = 0
            store.recordPlay(queue[pos])
        } else if (repeatMode == 1) {
            pos = 0; progress = 0
            store.recordPlay(queue[pos])
        } else {
            playing = false; progress = 0
        }
        persist()
    }

    fun prev() {
        if (queue.isEmpty()) return
        if (progress > 3) progress = 0
        else if (pos > 0) { pos--; progress = 0; store.recordPlay(queue[pos]) }
        persist()
    }

    fun toggleShuffle() {
        shuffle = !shuffle
        val cur = current()
        if (shuffle) {
            queue.shuffle()
            cur?.let { queue.remove(it.id); queue.add(0, it.id); pos = 0 }
        }
        persist()
    }

    fun cycleRepeat() { repeatMode = (repeatMode + 1) % 3; persist() }

    fun playNext(song: Song) {
        if (queue.isEmpty()) { playList(listOf(song), song); playing = false; return }
        queue.remove(song.id)
        queue.add(pos + 1, song.id)
        persist()
    }

    fun enqueue(song: Song) {
        if (queue.isEmpty()) { playList(listOf(song), song); playing = false; return }
        queue.remove(song.id)
        queue.add(song.id)
        persist()
    }

    fun removeAt(index: Int) {
        if (index !in queue.indices) return
        if (index == pos) return
        queue.removeAt(index)
        if (index < pos) pos--
        persist()
    }

    fun move(from: Int, to: Int) {
        if (from !in queue.indices || to !in queue.indices || from == to) return
        val item = queue.removeAt(from)
        queue.add(to, item)
        pos = when {
            pos == from -> to
            from < pos && to >= pos -> pos - 1
            from > pos && to <= pos -> pos + 1
            else -> pos
        }
        persist()
    }

    fun clear() {
        queue.clear(); pos = 0; progress = 0; playing = false
        persist()
    }

    // 每秒 tick，由 UI 层驱动
    fun tick() {
        if (!playing) return
        rotation = (rotation + 8f) % 360f
        progress++
        if (sleepMinutes == 0 && !sleepFinishCurrent) {
            playing = false; sleepMinutes = -1; persist(); return
        }
        if (progress >= duration()) {
            if (sleepMinutes == 0) { playing = false; sleepMinutes = -1; progress = 0 }
            else next(auto = true)
        }
        persist()
    }

    fun tickMinute() {
        if (sleepMinutes > 0) { sleepMinutes--; persist() }
    }
}
