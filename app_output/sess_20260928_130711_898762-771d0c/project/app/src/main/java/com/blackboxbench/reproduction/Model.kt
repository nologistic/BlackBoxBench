package com.blackboxbench.reproduction

/** One optional episode chapter. */
data class Chapter(val start: Int, val title: String)

/** A podcast episode together with its mutable playback state. */
data class Episode(
    val guid: String,
    val title: String,
    val publishedIso: String,
    val durationSec: Int,
    val description: String,
    val audio: String,
    val chapters: List<Chapter> = emptyList(),
    var played: Boolean = false,
    var downloaded: Boolean = false,
    var favorite: Boolean = false,
    var isNew: Boolean = false,
    var progressSec: Int = 0,
) {
    val durationLabel: String
        get() {
            val m = durationSec / 60
            val s = durationSec % 60
            return "$m:%02d".format(s)
        }

    val dateLabel: String get() = publishedIso.take(10)
}

/** All persisted user preferences observed in the settings screens. */
data class Settings(
    val themeMode: String = "auto",
    val pureBlack: Boolean = false,
    val dynamicColor: Boolean = false,
    val useEpisodeCover: Boolean = true,
    val showRemainingTime: Boolean = false,
    val adjustBySpeed: Boolean = false,
    val notificationButtons: String = "默认",
    val defaultPage: String = "首页",
    val bottomNavigation: Boolean = true,
    val backOpensDrawer: Boolean = false,
    val defaultSort: String = "按日期",
    val swipeActions: String = "无",
    val preferStreaming: Boolean = false,
    val playFromDownloadScreen: Boolean = false,
    val headphoneDisconnect: Boolean = true,
    val fastForwardSec: Int = 30,
    val rewindSec: Int = 10,
    val playbackSpeed: Float = 1.0f,
    val skipSilence: Boolean = false,
    val forwardButton: String = "跳过 30 秒",
    val rewindButton: String = "跳过 10 秒",
    val enqueueLocation: String = "末尾",
    val addDownloadedToQueue: Boolean = true,
    val continuousPlayback: Boolean = true,
    val smartMarkPlayed: Boolean = false,
    val keepSkipped: Boolean = true,
    val removeFromQueueAfterDeletion: Boolean = false,
    val mobileDataUpdate: String = "仅订阅",
    val proxy: String = "无",
    val refreshInterval: String = "每 12 小时",
    val newEpisodeAction: String = "添加到收件箱",
    val autoDownload: String = "禁用",
    val autoDelete: String = "禁用",
)
