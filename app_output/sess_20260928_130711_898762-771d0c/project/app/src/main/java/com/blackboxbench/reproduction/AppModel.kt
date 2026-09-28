package com.blackboxbench.reproduction

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

/** Mutable application state with SharedPreferences backed persistence. */
class AppModel(private val context: Context) {

    private val prefs = context.getSharedPreferences("antennapod_repro", Context.MODE_PRIVATE)

    var settings by mutableStateOf(Settings())
        private set
    var subscribed by mutableStateOf(false)
        private set
    var podcastTitle by mutableStateOf("")
        private set
    var podcastAuthor by mutableStateOf("")
        private set
    var podcastDescription by mutableStateOf("")
        private set

    val episodes = mutableStateListOf<Episode>()
    val queue = mutableStateListOf<String>()
    var currentGuid by mutableStateOf<String?>(null)
    var playing by mutableStateOf(false)
    var positionSec by mutableStateOf(0)
    var shuffle by mutableStateOf(false)

    var navItems by mutableStateOf(
        listOf("首页", "队列", "收件箱", "订阅", "单集", "下载", "播放记录", "收藏", "统计", "添加播客")
    )
        private set
    var hiddenNavItems by mutableStateOf(listOf<String>())
        private set
    var homeSections by mutableStateOf(listOf("继续收听", "最近更新", "不期而遇", "订阅列表", "管理下载"))
        private set

    init {
        load()
    }

    // ---------------------------------------------------------------- queries

    fun episode(guid: String): Episode? = episodes.firstOrNull { it.guid == guid }

    fun current(): Episode? = currentGuid?.let { episode(it) }

    val queueEpisodes: List<Episode> get() = queue.mapNotNull { episode(it) }

    val downloaded: List<Episode> get() = episodes.filter { it.downloaded }

    val favorites: List<Episode> get() = episodes.filter { it.favorite }

    val history: List<Episode> get() = episodes.filter { it.played || it.progressSec > 0 }

    val inProgress: Episode? get() = episodes.firstOrNull { !it.played && it.progressSec > 0 }

    val playedSeconds: Int get() = episodes.sumOf { if (it.played) it.durationSec else it.progressSec }

    val downloadedBytes: Long get() = downloaded.size * 28L * 1024 * 1024

    // ---------------------------------------------------------------- mutations

    fun updateSettings(block: (Settings) -> Settings) {
        settings = block(settings)
        persist()
    }

    fun setNavItems(visible: List<String>, hidden: List<String>) {
        navItems = visible
        hiddenNavItems = hidden
        persist()
    }

    fun updateHomeSections(sections: List<String>) {
        homeSections = sections
        persist()
    }

    fun subscribe() {
        val json = JSONObject(AssetStore.readText(context, "episodes.json"))
        val podcast = json.getJSONObject("podcast")
        podcastTitle = podcast.optString("title")
        podcastAuthor = podcast.optString("author")
        podcastDescription = podcast.optString("description")
        episodes.clear()
        val arr = json.getJSONArray("episodes")
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val audioPath = o.optString("audio_file")
            val audio = "audio/" + audioPath.substringAfterLast('/')
            val chapters = mutableListOf<Chapter>()
            val chapterArr = o.optJSONArray("chapters")
            if (chapterArr != null) {
                for (c in 0 until chapterArr.length()) {
                    val co = chapterArr.getJSONObject(c)
                    chapters.add(Chapter(co.optInt("start"), co.optString("title")))
                }
            }
            val status = o.optString("status")
            episodes.add(
                Episode(
                    guid = o.optString("guid"),
                    title = o.optString("title"),
                    publishedIso = o.optString("published"),
                    durationSec = parseDuration(o.optString("duration")),
                    description = o.optString("description"),
                    audio = audio,
                    chapters = chapters,
                    played = status == "played",
                    downloaded = status == "downloaded",
                    isNew = status == "new",
                    progressSec = if (status == "in_progress") o.optInt("progress_seconds") else 0,
                )
            )
        }
        subscribed = true
        persist()
    }

    fun unsubscribe() {
        subscribed = false
        episodes.clear()
        queue.clear()
        currentGuid = null
        playing = false
        persist()
    }

    fun togglePlayed(guid: String) {
        episode(guid)?.let {
            it.played = !it.played
            if (it.played) it.progressSec = 0
        }
        persist()
    }

    fun toggleFavorite(guid: String) {
        episode(guid)?.let { it.favorite = !it.favorite }
        persist()
    }

    fun toggleDownloaded(guid: String) {
        episode(guid)?.let { it.downloaded = !it.downloaded }
        persist()
    }

    fun enqueue(guid: String) {
        if (!queue.contains(guid)) queue.add(guid)
        persist()
    }

    fun dequeue(guid: String) {
        queue.remove(guid)
        persist()
    }

    fun clearQueue() {
        queue.clear()
        persist()
    }

    fun removeFromQueueAt(index: Int) {
        if (index in queue.indices) queue.removeAt(index)
        persist()
    }

    fun moveInQueue(from: Int, to: Int) {
        if (from in queue.indices && to in queue.indices) {
            val item = queue.removeAt(from)
            queue.add(to, item)
            persist()
        }
    }

    fun markAllPlayed() {
        episodes.forEach { it.played = true }
        persist()
    }

    fun play(guid: String) {
        currentGuid = guid
        positionSec = episode(guid)?.progressSec ?: 0
        playing = true
    }

    fun togglePlayPause() {
        if (currentGuid == null) {
            queue.firstOrNull()?.let { play(it) } ?: episodes.firstOrNull()?.let { play(it.guid) }
        } else {
            playing = !playing
        }
    }

    fun seekTo(sec: Int) {
        positionSec = sec.coerceAtLeast(0)
        episode(currentGuid ?: return)?.progressSec = positionSec
    }

    fun skip(delta: Int) {
        val ep = current() ?: return
        positionSec = (positionSec + delta).coerceIn(0, ep.durationSec)
        ep.progressSec = positionSec
    }

    fun onTick() {
        val ep = current() ?: return
        if (!playing) return
        positionSec += 1
        ep.progressSec = positionSec
        if (positionSec >= ep.durationSec) {
            ep.played = true
            ep.progressSec = 0
            positionSec = 0
            val next = queue.getOrNull(queue.indexOf(ep.guid) + 1)
            if (settings.continuousPlayback && next != null) {
                currentGuid = next
                positionSec = episode(next)?.progressSec ?: 0
            } else {
                playing = false
            }
            persist()
        }
    }

    // ---------------------------------------------------------------- persistence

    private fun persist() {
        val e = prefs.edit()
        e.putBoolean("subscribed", subscribed)
        e.putString("podcastTitle", podcastTitle)
        e.putString("podcastAuthor", podcastAuthor)
        e.putString("podcastDescription", podcastDescription)
        e.putString("navItems", JSONArray(navItems).toString())
        e.putString("hiddenNavItems", JSONArray(hiddenNavItems).toString())
        e.putString("homeSections", JSONArray(homeSections).toString())
        e.putString("queue", JSONArray(queue).toString())
        e.putString("currentGuid", currentGuid)
        val epArr = JSONArray()
        episodes.forEach {
            epArr.put(
                JSONObject().apply {
                    put("guid", it.guid)
                    put("played", it.played)
                    put("downloaded", it.downloaded)
                    put("favorite", it.favorite)
                    put("new", it.isNew)
                    put("progress", it.progressSec)
                }
            )
        }
        e.putString("episodes", epArr.toString())
        e.putString("settings", settingsToJson(settings).toString())
        e.apply()
    }

    private fun load() {
        subscribed = prefs.getBoolean("subscribed", false)
        podcastTitle = prefs.getString("podcastTitle", "") ?: ""
        podcastAuthor = prefs.getString("podcastAuthor", "") ?: ""
        podcastDescription = prefs.getString("podcastDescription", "") ?: ""
        prefs.getString("navItems", null)?.let { navItems = jsonList(it) }
        prefs.getString("hiddenNavItems", null)?.let { hiddenNavItems = jsonList(it) }
        prefs.getString("homeSections", null)?.let { homeSections = jsonList(it) }
        prefs.getString("settings", null)?.let { settings = settingsFromJson(JSONObject(it)) }
        currentGuid = prefs.getString("currentGuid", null)
        if (subscribed) {
            restoreEpisodes()
        }
        prefs.getString("queue", null)?.let { raw ->
            queue.clear()
            queue.addAll(jsonList(raw).filter { g -> episodes.any { it.guid == g } })
        }
    }

    private fun restoreEpisodes() {
        val json = JSONObject(AssetStore.readText(context, "episodes.json"))
        val podcast = json.getJSONObject("podcast")
        podcastTitle = podcast.optString("title")
        podcastAuthor = podcast.optString("author")
        podcastDescription = podcast.optString("description")
        val states = HashMap<String, JSONObject>()
        prefs.getString("episodes", null)?.let { raw ->
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                states[o.optString("guid")] = o
            }
        }
        episodes.clear()
        val arr = json.getJSONArray("episodes")
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val audio = "audio/" + o.optString("audio_file").substringAfterLast('/')
            val chapters = mutableListOf<Chapter>()
            o.optJSONArray("chapters")?.let { chapterArr ->
                for (c in 0 until chapterArr.length()) {
                    val co = chapterArr.getJSONObject(c)
                    chapters.add(Chapter(co.optInt("start"), co.optString("title")))
                }
            }
            val guid = o.optString("guid")
            val st = states[guid]
            episodes.add(
                Episode(
                    guid = guid,
                    title = o.optString("title"),
                    publishedIso = o.optString("published"),
                    durationSec = parseDuration(o.optString("duration")),
                    description = o.optString("description"),
                    audio = audio,
                    chapters = chapters,
                    played = st?.optBoolean("played") ?: false,
                    downloaded = st?.optBoolean("downloaded") ?: false,
                    favorite = st?.optBoolean("favorite") ?: false,
                    isNew = st?.optBoolean("new") ?: false,
                    progressSec = st?.optInt("progress") ?: 0,
                )
            )
        }
    }

    companion object {
        fun parseDuration(text: String): Int {
            val parts = text.split(":")
            return when (parts.size) {
                3 -> parts[0].toInt() * 3600 + parts[1].toInt() * 60 + parts[2].toInt()
                2 -> parts[0].toInt() * 60 + parts[1].toInt()
                1 -> parts[0].toIntOrNull() ?: 0
                else -> 0
            }
        }

        fun jsonList(raw: String): List<String> {
            val arr = JSONArray(raw)
            return (0 until arr.length()).map { arr.getString(it) }
        }

        fun settingsToJson(s: Settings): JSONObject = JSONObject().apply {
            put("themeMode", s.themeMode)
            put("pureBlack", s.pureBlack)
            put("dynamicColor", s.dynamicColor)
            put("useEpisodeCover", s.useEpisodeCover)
            put("showRemainingTime", s.showRemainingTime)
            put("adjustBySpeed", s.adjustBySpeed)
            put("notificationButtons", s.notificationButtons)
            put("defaultPage", s.defaultPage)
            put("bottomNavigation", s.bottomNavigation)
            put("backOpensDrawer", s.backOpensDrawer)
            put("defaultSort", s.defaultSort)
            put("swipeActions", s.swipeActions)
            put("preferStreaming", s.preferStreaming)
            put("playFromDownloadScreen", s.playFromDownloadScreen)
            put("headphoneDisconnect", s.headphoneDisconnect)
            put("fastForwardSec", s.fastForwardSec)
            put("rewindSec", s.rewindSec)
            put("playbackSpeed", s.playbackSpeed.toDouble())
            put("skipSilence", s.skipSilence)
            put("forwardButton", s.forwardButton)
            put("rewindButton", s.rewindButton)
            put("enqueueLocation", s.enqueueLocation)
            put("addDownloadedToQueue", s.addDownloadedToQueue)
            put("continuousPlayback", s.continuousPlayback)
            put("smartMarkPlayed", s.smartMarkPlayed)
            put("keepSkipped", s.keepSkipped)
            put("removeFromQueueAfterDeletion", s.removeFromQueueAfterDeletion)
            put("mobileDataUpdate", s.mobileDataUpdate)
            put("proxy", s.proxy)
            put("refreshInterval", s.refreshInterval)
            put("newEpisodeAction", s.newEpisodeAction)
            put("autoDownload", s.autoDownload)
            put("autoDelete", s.autoDelete)
        }

        fun settingsFromJson(o: JSONObject): Settings {
            val d = Settings()
            return Settings(
                themeMode = o.optString("themeMode", d.themeMode),
                pureBlack = o.optBoolean("pureBlack", d.pureBlack),
                dynamicColor = o.optBoolean("dynamicColor", d.dynamicColor),
                useEpisodeCover = o.optBoolean("useEpisodeCover", d.useEpisodeCover),
                showRemainingTime = o.optBoolean("showRemainingTime", d.showRemainingTime),
                adjustBySpeed = o.optBoolean("adjustBySpeed", d.adjustBySpeed),
                notificationButtons = o.optString("notificationButtons", d.notificationButtons),
                defaultPage = o.optString("defaultPage", d.defaultPage),
                bottomNavigation = o.optBoolean("bottomNavigation", d.bottomNavigation),
                backOpensDrawer = o.optBoolean("backOpensDrawer", d.backOpensDrawer),
                defaultSort = o.optString("defaultSort", d.defaultSort),
                swipeActions = o.optString("swipeActions", d.swipeActions),
                preferStreaming = o.optBoolean("preferStreaming", d.preferStreaming),
                playFromDownloadScreen = o.optBoolean("playFromDownloadScreen", d.playFromDownloadScreen),
                headphoneDisconnect = o.optBoolean("headphoneDisconnect", d.headphoneDisconnect),
                fastForwardSec = o.optInt("fastForwardSec", d.fastForwardSec),
                rewindSec = o.optInt("rewindSec", d.rewindSec),
                playbackSpeed = o.optDouble("playbackSpeed", d.playbackSpeed.toDouble()).toFloat(),
                skipSilence = o.optBoolean("skipSilence", d.skipSilence),
                forwardButton = o.optString("forwardButton", d.forwardButton),
                rewindButton = o.optString("rewindButton", d.rewindButton),
                enqueueLocation = o.optString("enqueueLocation", d.enqueueLocation),
                addDownloadedToQueue = o.optBoolean("addDownloadedToQueue", d.addDownloadedToQueue),
                continuousPlayback = o.optBoolean("continuousPlayback", d.continuousPlayback),
                smartMarkPlayed = o.optBoolean("smartMarkPlayed", d.smartMarkPlayed),
                keepSkipped = o.optBoolean("keepSkipped", d.keepSkipped),
                removeFromQueueAfterDeletion = o.optBoolean(
                    "removeFromQueueAfterDeletion", d.removeFromQueueAfterDeletion
                ),
                mobileDataUpdate = o.optString("mobileDataUpdate", d.mobileDataUpdate),
                proxy = o.optString("proxy", d.proxy),
                refreshInterval = o.optString("refreshInterval", d.refreshInterval),
                newEpisodeAction = o.optString("newEpisodeAction", d.newEpisodeAction),
                autoDownload = o.optString("autoDownload", d.autoDownload),
                autoDelete = o.optString("autoDelete", d.autoDelete),
            )
        }
    }
}
