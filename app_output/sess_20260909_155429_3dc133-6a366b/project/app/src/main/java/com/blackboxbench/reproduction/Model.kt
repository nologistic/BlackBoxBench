package com.blackboxbench.reproduction

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class Chapter(val start: Int, val title: String)
data class Episode(
    val id: String,
    val title: String,
    val published: String,
    val duration: String,
    val durationSeconds: Int,
    val description: String,
    val rawAudio: Int,
    val initialStatus: String,
    val chapters: List<Chapter>
)

enum class Page {
    HOME, QUEUE, INBOX, SUBSCRIPTIONS, EPISODES, DOWNLOADS, HISTORY,
    FAVORITES, STATISTICS, ADD, PODCAST, EPISODE, PLAYER, SETTINGS,
    UI_SETTINGS, PLAYBACK_SETTINGS, DOWNLOAD_SETTINGS, AUTO_DOWNLOAD, INFO
}

class AppModel(private val context: Context) {
    private val prefs = context.getSharedPreferences("antenna_state", Context.MODE_PRIVATE)
    val episodes: List<Episode> = loadEpisodes(context)

    var page by mutableStateOf(Page.HOME)
    var subscribed by mutableStateOf(prefs.getBoolean("subscribed", false))
    var podcastName by mutableStateOf(prefs.getString("podcast_name", "黑盒电台") ?: "黑盒电台")
    var label by mutableStateOf(prefs.getString("label", "") ?: "")
    var theme by mutableStateOf(prefs.getString("theme", "auto") ?: "auto")
    var selectedEpisodeId by mutableStateOf<String?>(null)
    var currentEpisodeId by mutableStateOf(prefs.getString("current", null))
    var isPlaying by mutableStateOf(false)
    var progressSeconds by mutableIntStateOf(prefs.getInt("progress", 0))
    var speed by mutableFloatStateOf(prefs.getFloat("speed", 1f))
    var fastForward by mutableIntStateOf(prefs.getInt("fast_forward", 30))
    var filterUnplayed by mutableStateOf(false)
    var sortNewest by mutableStateOf(true)
    var statsTab by mutableIntStateOf(0)
    var autoDownload by mutableStateOf(prefs.getBoolean("auto_download", false))
    var queueDownloadsOnly by mutableStateOf(prefs.getBoolean("queue_downloads", false))
    var downloadWhileBattery by mutableStateOf(prefs.getBoolean("battery_download", true))
    var pauseOnDisconnect by mutableStateOf(prefs.getBoolean("pause_disconnect", true))

    val queue: SnapshotStateList<String> = mutableStateListOf<String>().apply { addAll(csv("queue")) }
    var downloads by mutableStateOf(csv("downloads").toSet())
    var favorites by mutableStateOf(csv("favorites").toSet())
    var history by mutableStateOf(csv("history").toSet())
    val navigation: SnapshotStateList<Page> = mutableStateListOf<Page>().apply {
        val stored = csv("navigation").mapNotNull { runCatching { Page.valueOf(it) }.getOrNull() }
        addAll(if (stored.size >= 4) stored else listOf(Page.HOME, Page.QUEUE, Page.INBOX, Page.SUBSCRIPTIONS))
    }

    private fun csv(key: String) =
        prefs.getString(key, "")!!.split(",").filter { it.isNotBlank() }

    private fun saveCsv(key: String, values: Collection<String>) {
        prefs.edit().putString(key, values.joinToString(",")).apply()
    }

    fun subscribe() {
        subscribed = true
        episodes.firstOrNull { it.initialStatus == "downloaded" }?.let { downloads = downloads + it.id }
        prefs.edit().putBoolean("subscribed", true).apply()
        saveCsv("downloads", downloads)
        page = Page.PODCAST
    }

    fun unsubscribe() {
        subscribed = false
        queue.clear()
        downloads = emptySet()
        favorites = emptySet()
        prefs.edit().putBoolean("subscribed", false).remove("queue").remove("downloads").remove("favorites").apply()
        page = Page.SUBSCRIPTIONS
    }

    fun rename(value: String) {
        if (value.isNotBlank()) {
            podcastName = value.trim()
            prefs.edit().putString("podcast_name", podcastName).apply()
        }
    }

    fun updateLabel(value: String) {
        label = value.trim()
        prefs.edit().putString("label", label).apply()
    }

    fun toggleQueue(id: String) {
        if (queue.contains(id)) queue.remove(id) else queue.add(id)
        saveCsv("queue", queue)
    }

    fun moveQueue(id: String, delta: Int) {
        val from = queue.indexOf(id)
        if (from < 0) return
        val to = (from + delta).coerceIn(0, queue.lastIndex)
        if (from != to) {
            queue.removeAt(from)
            queue.add(to, id)
            saveCsv("queue", queue)
        }
    }

    fun toggleDownload(id: String) {
        downloads = if (id in downloads) downloads - id else downloads + id
        saveCsv("downloads", downloads)
    }

    fun toggleFavorite(id: String) {
        favorites = if (id in favorites) favorites - id else favorites + id
        saveCsv("favorites", favorites)
    }

    fun play(id: String) {
        if (currentEpisodeId != id) {
            currentEpisodeId = id
            progressSeconds = 0
        }
        isPlaying = true
        history = history + id
        prefs.edit().putString("current", id).putInt("progress", progressSeconds).apply()
        saveCsv("history", history)
    }

    fun togglePlay() {
        if (currentEpisodeId == null) episodes.firstOrNull()?.let { play(it.id) }
        else isPlaying = !isPlaying
    }

    fun seek(delta: Int) {
        val max = currentEpisode?.durationSeconds ?: 1
        progressSeconds = (progressSeconds + delta).coerceIn(0, max)
        prefs.edit().putInt("progress", progressSeconds).apply()
    }

    fun setProgress(value: Float) {
        progressSeconds = value.toInt()
        prefs.edit().putInt("progress", progressSeconds).apply()
    }

    fun tick() {
        if (!isPlaying) return
        val max = currentEpisode?.durationSeconds ?: return
        if (progressSeconds < max) {
            progressSeconds += 1
            if (progressSeconds % 5 == 0) prefs.edit().putInt("progress", progressSeconds).apply()
        } else {
            isPlaying = false
            queue.firstOrNull { it != currentEpisodeId }?.let { play(it) }
        }
    }

    fun updateTheme(value: String) {
        theme = value
        prefs.edit().putString("theme", value).apply()
    }

    fun updateSpeed(value: Float) {
        speed = value
        prefs.edit().putFloat("speed", value).apply()
    }

    fun updateFastForward(value: Int) {
        fastForward = value
        prefs.edit().putInt("fast_forward", value).apply()
    }

    fun saveNavigation() = saveCsv("navigation", navigation.map { it.name })
    val currentEpisode: Episode? get() = episodes.firstOrNull { it.id == currentEpisodeId }
    fun episode(id: String?) = episodes.firstOrNull { it.id == id }

    private fun loadEpisodes(context: Context): List<Episode> = runCatching {
        val array = JSONObject(AssetStore.readText(context, "episodes.json")).getJSONArray("episodes")
        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            val duration = item.getString("duration")
            val parts = duration.split(":")
            val seconds = (parts.getOrNull(0)?.toIntOrNull() ?: 30) * 60 +
                (parts.getOrNull(1)?.toIntOrNull() ?: 0)
            val chapterArray = item.optJSONArray("chapters")
            val chapters = if (chapterArray == null) emptyList() else
                (0 until chapterArray.length()).map { c ->
                    chapterArray.getJSONObject(c).let { Chapter(it.getInt("start"), it.getString("title")) }
                }
            val audioName = item.optString("audio_file")
            val raw = when {
                audioName.endsWith("notification.wav") -> R.raw.notification
                audioName.endsWith("success.wav") -> R.raw.success
                else -> R.raw.ambient
            }
            val description = when (index) {
                0 -> "当工具越来越聪明，可复现性反而更重要。本集讨论随机性、状态和可靠设计。"
                1 -> "一张截图能告诉我们多少？从像素线索还原产品交互的完整方法。"
                2 -> "没有网络也能安心收听：缓存、下载与同步策略的取舍。"
                else -> "黑盒电台第一季虚构节目，记录产品、工程与开源世界。"
            }
            Episode(
                id = item.getString("guid"),
                title = item.getString("title"),
                published = item.getString("published"),
                duration = duration,
                durationSeconds = seconds,
                description = description,
                rawAudio = raw,
                initialStatus = item.optString("status", "unplayed"),
                chapters = chapters
            )
        }
    }.getOrElse { emptyList() }
}

fun prettyDate(value: String): String = runCatching {
    val input = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    SimpleDateFormat("yyyy年M月d日", Locale.CHINA).format(input.parse(value)!!)
}.getOrElse { value.take(10) }

fun formatTime(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)
