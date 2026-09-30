package com.blackboxbench.reproduction

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import org.json.JSONArray
import org.json.JSONObject

enum class MediaKind { VIDEO, AUDIO }

enum class PatternKind { COLOR_BARS, SAMPLE_LOOP, AUDIO_WAVE }

data class MediaItem(
    val id: String,
    val title: String,
    val fileName: String,
    val folder: String,
    val durationLabel: String,
    val durationSec: Int,
    val badge: String,
    val width: Int,
    val height: Int,
    val sizeLabel: String,
    val videoBitrate: String,
    val videoCodec: String,
    val frameRate: String,
    val audioBitrate: String,
    val audioCodec: String,
    val channels: String,
    val sampleRate: String,
    val kind: MediaKind,
    val pattern: PatternKind,
    val album: String = "未知专辑",
    val artist: String = "未知艺术家",
) {
    val path: String get() = "内部存储 › $folder › $fileName"
}

data class Playlist(val id: String, val name: String, val itemIds: MutableList<String>)

data class FolderEntry(val name: String, val childCount: Int? = null)

/** Static media library mirroring what the explorer observed on the device. */
object Library {
    val videos: List<MediaItem> = listOf(
        MediaItem(
            id = "v1", title = "clip_1_1280x720", fileName = "clip_1_1280x720.mp4", folder = "Media",
            durationLabel = "0:03", durationSec = 3, badge = "720p", width = 1280, height = 720,
            sizeLabel = "1,42 MB", videoBitrate = "3.7 Mb/s",
            videoCodec = "H264 - MPEG-4 AVC (part 10)", frameRate = "24.000",
            audioBitrate = "69.6 KB/s", audioCodec = "MPEG AAC Audio", channels = "2 声道",
            sampleRate = "44100 Hz", kind = MediaKind.VIDEO, pattern = PatternKind.COLOR_BARS,
        ),
        MediaItem(
            id = "v2", title = "clip_2_1920x1080", fileName = "clip_2_1920x1080.mp4", folder = "Media",
            durationLabel = "0:03", durationSec = 3, badge = "1080p", width = 1920, height = 1080,
            sizeLabel = "2,74 MB", videoBitrate = "5.6 Mb/s",
            videoCodec = "H264 - MPEG-4 AVC (part 10)", frameRate = "24.000",
            audioBitrate = "69.6 KB/s", audioCodec = "MPEG AAC Audio", channels = "2 声道",
            sampleRate = "44100 Hz", kind = MediaKind.VIDEO, pattern = PatternKind.COLOR_BARS,
        ),
        MediaItem(
            id = "v3", title = "clip_3_720x1280", fileName = "clip_3_720x1280.mp4", folder = "Media",
            durationLabel = "0:04", durationSec = 4, badge = "720p", width = 720, height = 1280,
            sizeLabel = "1,58 MB", videoBitrate = "3.1 Mb/s",
            videoCodec = "H264 - MPEG-4 AVC (part 10)", frameRate = "24.000",
            audioBitrate = "69.6 KB/s", audioCodec = "MPEG AAC Audio", channels = "2 声道",
            sampleRate = "44100 Hz", kind = MediaKind.VIDEO, pattern = PatternKind.COLOR_BARS,
        ),
        MediaItem(
            id = "v4", title = "sample_video", fileName = "sample_video.mp4", folder = "Media",
            durationLabel = "0:03", durationSec = 3, badge = "SD", width = 640, height = 480,
            sizeLabel = "0,28 MB", videoBitrate = "0.7 Mb/s",
            videoCodec = "H264 - MPEG-4 AVC (part 10)", frameRate = "25.000",
            audioBitrate = "64.0 KB/s", audioCodec = "MPEG AAC Audio", channels = "2 声道",
            sampleRate = "44100 Hz", kind = MediaKind.VIDEO, pattern = PatternKind.SAMPLE_LOOP,
        ),
    )

    val audios: List<MediaItem> = listOf(
        MediaItem(
            id = "a1", title = "sample_audio", fileName = "sample_audio.wav", folder = "Music",
            durationLabel = "0:00", durationSec = 0, badge = "", width = 0, height = 0,
            sizeLabel = "30,2 KB", videoBitrate = "", videoCodec = "",
            frameRate = "", audioBitrate = "705.6 KB/s", audioCodec = "PCM S16 LE",
            channels = "1 声道", sampleRate = "44100 Hz",
            kind = MediaKind.AUDIO, pattern = PatternKind.AUDIO_WAVE,
        ),
    )

    val all: List<MediaItem> = videos + audios

    fun byId(id: String): MediaItem? = all.firstOrNull { it.id == id }

    // Folder tree mirroring the observed device storage layout.
    val rootFolders: List<FolderEntry> = listOf(
        FolderEntry("Alarms", null),
        FolderEntry("Android", 3),
        FolderEntry("Audiobooks", null),
        FolderEntry("DCIM", null),
        FolderEntry("Documents", null),
        FolderEntry("Download", 1),
        FolderEntry("Media", 3),
        FolderEntry("Movies", 1),
        FolderEntry("Music", 1),
        FolderEntry("Notifications", null),
        FolderEntry("Pictures", 1),
        FolderEntry("Podcasts", null),
        FolderEntry("Recordings", null),
        FolderEntry("Ringtones", null),
    )

    fun folderFiles(name: String): List<String> = when (name) {
        "Media" -> listOf("clip_1_1280x720.mp4", "clip_2_1920x1080.mp4", "clip_3_720x1280.mp4")
        "Movies" -> listOf("sample_video.mp4")
        "Music" -> audios.map { it.fileName }
        "Download" -> listOf("sample_note.txt")
        "Pictures" -> listOf("sample_photo.png")
        else -> emptyList()
    }
}

/** Mutable, persisted application state. */
object Store {
    private const val PREFS = "vlc_reproduction"

    lateinit var appContext: Context
        private set

    val onboardingDone = mutableStateOf(false)
    val themeMode = mutableStateOf(0) // 0 follow system, 1 light, 2 dark
    val incognito = mutableStateOf(false)
    val persistIncognito = mutableStateOf(true)
    val autoRescan = mutableStateOf(true)
    val showMissingMedia = mutableStateOf(true)
    val showHeaders = mutableStateOf(true)
    val singleLineTitle = mutableStateOf(false)
    val autoLoadSubs = mutableStateOf(true)
    val subtitleEncoding = mutableStateOf("Default (Windows-1252)")
    val preferredSubLanguage = mutableStateOf("无语言偏好")
    val videoGrouping = mutableStateOf("按名称分组")
    val playAction = mutableStateOf("播放")
    val sortCriteria = mutableStateOf("名称")
    val sortAscending = mutableStateOf(true)
    val onlyFavorites = mutableStateOf(false)
    val listLayout = mutableStateOf(false)
    val showWatchedMarker = mutableStateOf(true)
    val showVideoThumbnails = mutableStateOf(true)
    val backgroundMode = mutableStateOf("无")
    val hardwareAccel = mutableStateOf("解码")
    val screenOrientation = mutableStateOf("自动（传感器感应）")
    val meteredNetwork = mutableStateOf("不执行任何操作")
    val fastSeek = mutableStateOf(false)
    val customPip = mutableStateOf(false)
    val resumeBackgroundVideo = mutableStateOf(false)
    val matchFrameRate = mutableStateOf(false)
    val preferredVideoResolution = mutableStateOf("可用的最高画质")
    val continueAfterCall = mutableStateOf(true)
    val stopOnSwipeAway = mutableStateOf(false)
    val digitalAudioOut = mutableStateOf(false)
    val preferredAudioLanguage = mutableStateOf("无语言偏好")
    val continueAudioPlayback = mutableStateOf("总是")
    val headphoneDetection = mutableStateOf(true)
    val continueAfterHeadphones = mutableStateOf(false)
    val ignoreHeadsetButtons = mutableStateOf(false)
    val replayGain = mutableStateOf(false)
    val castWireless = mutableStateOf(true)
    val castAudioOnly = mutableStateOf(false)
    val castPassthrough = mutableStateOf(false)
    val playbackHistory = mutableStateOf(true)
    val videoQueueHistory = mutableStateOf(true)
    val audioQueueHistory = mutableStateOf(true)
    val eqEnabled = mutableStateOf(false)
    val eqPreset = mutableStateOf("Flat")
    val sleepTimer = mutableStateOf("已禁用")
    val showWatchedVideoMarker = mutableStateOf(true)
    val notificationPermission = mutableStateOf(true)
    val filePermission = mutableStateOf(1) // 0 none, 1 regular media, 2 full
    val storageChecked = mutableStateOf(true)
    val localNetwork = mutableStateOf(false)

    /** First-run player hint + gesture tutorial still pending. */
    var tutorialPending: Boolean = true

    private val positionCache = mutableMapOf<String, Float>()
    private var positionsLoaded = false

    private fun ensurePositions(p: android.content.SharedPreferences) {
        if (positionsLoaded) return
        positionsLoaded = true
        p.all.forEach { (k, v) -> if (k.startsWith("pos_") && v is Float) positionCache[k.removePrefix("pos_")] = v }
    }

    /** Last playback position (seconds) remembered for a media item. */
    fun positionOf(id: String): Float {
        if (!::appContext.isInitialized) return 0f
        val p = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        ensurePositions(p)
        return positionCache[id] ?: 0f
    }

    fun setPosition(id: String, value: Float) {
        positionCache[id] = value
    }

    fun flushPositions() {
        if (!::appContext.isInitialized) return
        val p = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val e = p.edit()
        positionCache.forEach { (k, v) -> e.putFloat("pos_$k", v) }
        e.apply()
    }

    val watched = mutableStateListOf<String>()
    val favorites = mutableStateListOf<String>()
    val playlists = mutableStateListOf<Playlist>()
    val history = mutableStateListOf<String>()

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        val p = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        onboardingDone.value = p.getBoolean("onboarding", false)
        themeMode.value = p.getInt("themeMode", 0)
        eqPreset.value = p.getString("eqPreset", "Flat") ?: "Flat"
        eqEnabled.value = p.getBoolean("eqEnabled", false)
        incognito.value = p.getBoolean("incognito", false)
        listLayout.value = p.getBoolean("listLayout", false)
        onlyFavorites.value = p.getBoolean("onlyFavorites", false)
        showWatchedMarker.value = p.getBoolean("showWatched", true)
        playAction.value = p.getString("playAction", "播放") ?: "播放"
        sortCriteria.value = p.getString("sortCriteria", "名称") ?: "名称"
        watched.addAll(p.getStringSet("watched", emptySet()) ?: emptySet())
        favorites.addAll(p.getStringSet("favorites", emptySet()) ?: emptySet())
        history.addAll(readStringList(p, "history"))
        readPlaylists(p)
    }

    private fun readStringList(p: android.content.SharedPreferences, key: String): List<String> {
        val raw = p.getString(key, null) ?: return emptyList()
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { arr.getString(it) }
    }

    private fun readPlaylists(p: android.content.SharedPreferences) {
        val raw = p.getString("playlists", null) ?: return
        val arr = JSONArray(raw)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val ids = o.getJSONArray("items")
            val list = mutableListOf<String>()
            for (j in 0 until ids.length()) list.add(ids.getString(j))
            playlists.add(Playlist(o.getString("id"), o.getString("name"), list))
        }
    }

    fun persist() {
        if (!::appContext.isInitialized) return
        val p = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val pl = JSONArray()
        playlists.forEach { playlist ->
            val o = JSONObject()
            o.put("id", playlist.id)
            o.put("name", playlist.name)
            o.put("items", JSONArray(playlist.itemIds.toList()))
            pl.put(o)
        }
        p.edit()
            .putBoolean("onboarding", onboardingDone.value)
            .putInt("themeMode", themeMode.value)
            .putString("eqPreset", eqPreset.value)
            .putBoolean("eqEnabled", eqEnabled.value)
            .putBoolean("incognito", incognito.value)
            .putBoolean("listLayout", listLayout.value)
            .putBoolean("onlyFavorites", onlyFavorites.value)
            .putBoolean("showWatched", showWatchedMarker.value)
            .putString("playAction", playAction.value)
            .putString("sortCriteria", sortCriteria.value)
            .putStringSet("watched", watched.toSet())
            .putStringSet("favorites", favorites.toSet())
            .putString("history", JSONArray(history.toList()).toString())
            .putString("playlists", pl.toString())
            .apply()
    }

    fun toggleFavorite(id: String) {
        if (favorites.contains(id)) favorites.remove(id) else favorites.add(id)
        persist()
    }

    fun markPlayed(id: String) {
        if (!watched.contains(id)) watched.add(id)
        history.remove(id)
        history.add(0, id)
        while (history.size > 24) history.removeAt(history.size - 1)
        persist()
    }

    fun createPlaylist(name: String): Playlist {
        val pl = Playlist("pl" + System.currentTimeMillis(), name, mutableListOf())
        playlists.add(pl)
        persist()
        return pl
    }

    fun playlist(id: String): Playlist? = playlists.firstOrNull { it.id == id }

    fun isWatched(id: String) = watched.contains(id)
    fun isFavorite(id: String) = favorites.contains(id)
}
