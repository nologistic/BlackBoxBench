package com.blackboxbench.reproduction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Observable application state with write-through persistence. */
class AppState(val store: VinylState) {

    var themeMode by mutableStateOf(store.themeMode)
    var themeStyle by mutableStateOf(store.themeStyle)
    var rememberLastPage by mutableStateOf(store.rememberLastPage)
    var blacklist by mutableStateOf(store.blacklist)
    var whitelist by mutableStateOf(store.whitelist)
    var colorizeNavBar by mutableStateOf(store.colorizeNavBar)
    var colorizeShortcuts by mutableStateOf(store.colorizeShortcuts)
    var transparentControls by mutableStateOf(store.transparentControls)
    var classicNotification by mutableStateOf(store.classicNotification)
    var colorizedNotification by mutableStateOf(store.colorizedNotification)
    var nowPlayingLook by mutableStateOf(store.nowPlayingLook)
    var showLyrics by mutableStateOf(store.showLyrics)
    var animatedIcon by mutableStateOf(store.animatedIcon)
    var showTrackNumber by mutableStateOf(store.showTrackNumber)
    var defaultAction by mutableStateOf(store.defaultAction)
    var autoDownload by mutableStateOf(store.autoDownload)
    var duckOnFocusLoss by mutableStateOf(store.duckOnFocusLoss)
    var gapless by mutableStateOf(store.gapless)
    var rememberShuffle by mutableStateOf(store.rememberShuffle)
    var replayGainMode by mutableStateOf(store.replayGainMode)
    var replayGainPreamp by mutableStateOf(store.replayGainPreamp)
    var recentInterval by mutableStateOf(store.recentInterval)
    var favoritePlaylist by mutableStateOf(store.favoritePlaylist)
    var skippedSongs by mutableStateOf(store.skippedSongs)
    var crashReports by mutableStateOf(store.crashReports)
    var syncQueueTag by mutableStateOf(store.syncQueueTag)
    var gridSize by mutableStateOf(store.gridSize)
    var sortMode by mutableStateOf(store.sortMode)
    var showFooter by mutableStateOf(store.showFooter)
    var colorFooter by mutableStateOf(store.colorFooter)

    var tabs by mutableStateOf(store.tabOrder)
    var hiddenTabs by mutableStateOf(store.hiddenTabs)

    val favorites = mutableStateListOf<String>().also { it.addAll(store.favorites) }
    val history = mutableStateListOf<String>().also { it.addAll(store.history) }
    val userPlaylists = mutableStateListOf<Pair<String, List<String>>>().also {
        it.addAll(store.userPlaylists)
    }

    var queue by mutableStateOf(store.queue)
    var queueIndex by mutableStateOf(store.queueIndex)
    var currentSongId by mutableStateOf(store.currentSongId)
    var positionMs by mutableStateOf(store.positionMs)
    var playing by mutableStateOf(false)
    var repeatMode by mutableStateOf(store.repeatMode)
    var shuffle by mutableStateOf(store.shuffle)
    var sleepMinutes by mutableStateOf(store.sleepTimerMinutes)
    var sleepAfterTrack by mutableStateOf(store.sleepAfterTrack)
    var sleepArmed by mutableStateOf(store.sleepArmed)
    var lastPage by mutableStateOf(store.lastPage)
    var snackbar by mutableStateOf<String?>(null)

    val current: Song? get() = currentSongId.takeIf { it.isNotEmpty() }?.let { Library.byId(it) }
    val hasQueue: Boolean get() = queue.isNotEmpty() && current != null

    // --- persistence helpers -------------------------------------------------

    fun persistQueue() {
        store.queue = queue
        store.queueIndex = queueIndex
        store.currentSongId = currentSongId
        store.positionMs = positionMs
        store.repeatMode = repeatMode
        store.shuffle = shuffle
        store.sleepTimerMinutes = sleepMinutes
        store.sleepAfterTrack = sleepAfterTrack
        store.sleepArmed = sleepArmed
    }

    fun persistSettings() {
        store.themeMode = themeMode
        store.themeStyle = themeStyle
        store.rememberLastPage = rememberLastPage
        store.blacklist = blacklist
        store.whitelist = whitelist
        store.colorizeNavBar = colorizeNavBar
        store.colorizeShortcuts = colorizeShortcuts
        store.transparentControls = transparentControls
        store.classicNotification = classicNotification
        store.colorizedNotification = colorizedNotification
        store.nowPlayingLook = nowPlayingLook
        store.showLyrics = showLyrics
        store.animatedIcon = animatedIcon
        store.showTrackNumber = showTrackNumber
        store.defaultAction = defaultAction
        store.autoDownload = autoDownload
        store.duckOnFocusLoss = duckOnFocusLoss
        store.gapless = gapless
        store.rememberShuffle = rememberShuffle
        store.replayGainMode = replayGainMode
        store.replayGainPreamp = replayGainPreamp
        store.recentInterval = recentInterval
        store.favoritePlaylist = favoritePlaylist
        store.skippedSongs = skippedSongs
        store.crashReports = crashReports
        store.syncQueueTag = syncQueueTag
        store.gridSize = gridSize
        store.sortMode = sortMode
        store.showFooter = showFooter
        store.colorFooter = colorFooter
        store.tabOrder = tabs
        store.hiddenTabs = hiddenTabs
        store.favorites = favorites.toList()
        store.history = history.toList()
        store.userPlaylists = userPlaylists.toList()
        store.lastPage = lastPage
    }

    fun update(block: () -> Unit) {
        block()
        persistSettings()
    }

    // --- playback ------------------------------------------------------------

    fun play(song: Song, list: List<Song> = Library.songs) {
        queue = list.map { it.id }
        queueIndex = queue.indexOf(song.id).coerceAtLeast(0)
        currentSongId = song.id
        positionMs = 0
        playing = true
        markPlayed(song.id)
        persistQueue()
    }

    fun playFirst(song: Song, list: List<Song>) = play(song, list)

    fun togglePlay() {
        if (currentSongId.isEmpty()) return
        playing = !playing
    }

    fun markPlayed(id: String) {
        history.remove(id)
        history.add(0, id)
        store.markPlayed(id)
    }

    fun next(auto: Boolean = false) {
        if (queue.isEmpty()) return
        if (repeatMode == 2 && auto) {
            positionMs = 0
            persistQueue()
            return
        }
        val nextIndex = queueIndex + 1
        if (nextIndex >= queue.size) {
            if (repeatMode == 1 || !auto) {
                queueIndex = 0
            } else {
                queueIndex = queue.size - 1
                playing = false
                positionMs = 0
                persistQueue()
                return
            }
        } else {
            queueIndex = nextIndex
        }
        currentSongId = queue[queueIndex]
        positionMs = 0
        playing = true
        currentSongId.takeIf { it.isNotEmpty() }?.let { markPlayed(it) }
        persistQueue()
    }

    fun previous() {
        if (queue.isEmpty()) return
        if (positionMs > 3000) {
            positionMs = 0
            persistQueue()
            return
        }
        queueIndex = if (queueIndex - 1 < 0) queue.size - 1 else queueIndex - 1
        currentSongId = queue[queueIndex]
        positionMs = 0
        playing = true
        persistQueue()
    }

    fun tick(deltaMs: Int) {
        val song = current ?: return
        if (song.durationMs <= 0) {
            positionMs = 0
            return
        }
        positionMs += deltaMs
        if (positionMs >= song.durationMs) {
            next(auto = true)
        }
    }

    fun cycleRepeat() {
        repeatMode = (repeatMode + 1) % 3
        persistQueue()
    }

    fun toggleShuffle() {
        if (!shuffle) {
            val currentId = currentSongId
            val rest = queue.filterNot { it == currentId }.shuffled()
            queue = if (currentId.isEmpty()) rest else listOf(currentId) + rest
            queueIndex = 0
        } else {
            queue = Library.songs.map { it.id }
            queueIndex = queue.indexOf(currentSongId).coerceAtLeast(0)
        }
        shuffle = !shuffle
        persistQueue()
    }

    fun clearQueue() {
        queue = emptyList()
        queueIndex = -1
        currentSongId = ""
        positionMs = 0
        playing = false
        persistQueue()
    }

    fun playNext(songId: String) {
        val list = queue.toMutableList()
        list.remove(songId)
        val insertAt = (queueIndex + 1).coerceIn(0, list.size)
        list.add(insertAt, songId)
        val currentId = currentSongId
        queue = list
        queueIndex = list.indexOf(currentId).coerceAtLeast(0)
        persistQueue()
    }

    fun removeFromQueue(songId: String) {
        val wasCurrent = songId == currentSongId
        val list = queue.toMutableList()
        val removedIndex = list.indexOf(songId)
        if (removedIndex < 0) return
        list.removeAt(removedIndex)
        queue = list
        when {
            list.isEmpty() -> {
                queueIndex = -1
                currentSongId = ""
                playing = false
                positionMs = 0
            }
            wasCurrent -> {
                val newIndex = removedIndex.coerceAtMost(list.size - 1)
                queueIndex = newIndex
                currentSongId = list[newIndex]
                positionMs = 0
            }
            removedIndex < queueIndex -> queueIndex -= 1
        }
        persistQueue()
    }

    fun moveInQueue(from: Int, to: Int) {
        if (from == to || from !in queue.indices || to !in queue.indices) return
        val currentId = currentSongId
        val list = queue.toMutableList()
        val item = list.removeAt(from)
        list.add(to, item)
        queue = list
        queueIndex = list.indexOf(currentId).coerceAtLeast(0)
        persistQueue()
    }

    fun toggleFavorite(id: String) {
        if (favorites.contains(id)) favorites.remove(id) else favorites.add(id)
        store.favorites = favorites.toList()
    }

    fun isFavorite(id: String): Boolean = favorites.contains(id)

    // --- playlists -----------------------------------------------------------

    fun playlists(): List<Playlist> {
        val result = mutableListOf<Playlist>()
        result.add(
            Playlist(
                "recent", "最近添加", Library.songs.map { it.id }, "smart",
                "$recentInterval · ${Library.songs.size} 歌曲"
            )
        )
        result.add(
            Playlist(
                "history", "播放历史", history.toList(), "smart",
                "$recentInterval · ${history.size} 歌曲"
            )
        )
        val notPlayed = Library.songs.map { it.id }.filterNot { history.contains(it) }
        result.add(
            Playlist(
                "notplayed", "最近未播放过", notPlayed, "smart",
                "$recentInterval · ${notPlayed.size} 歌曲"
            )
        )
        val mostPlayed = Library.songs.map { it.id }.sortedByDescending { store.playCount(it) }
        result.add(
            Playlist(
                "mostplayed", "最喜爱的歌曲", mostPlayed, "smart",
                "${mostPlayed.size} 歌曲"
            )
        )
        val favSongs = Library.songs.map { it.id }.filter { favorites.contains(it) }
        result.add(Playlist("favorites", "收藏夹", favSongs, "favorite", "${favSongs.size} 歌曲"))
        userPlaylists.forEach { (name, ids) ->
            result.add(
                Playlist(
                    "user:$name", name, ids.filter { Library.byId(it) != null }, "user",
                    "${ids.size} 歌曲"
                )
            )
        }
        return result
    }

    fun playlist(id: String): Playlist? = playlists().firstOrNull { it.id == id }

    fun addToPlaylist(name: String, songIds: List<String>, created: Boolean) {
        val existing = userPlaylists.indexOfFirst { it.first == name }
        if (existing >= 0) {
            val merged = (userPlaylists[existing].second + songIds).distinct()
            userPlaylists[existing] = name to merged
        } else {
            userPlaylists.add(name to songIds.distinct())
        }
        store.userPlaylists = userPlaylists.toList()
        snackbar = "${songIds.size} 首歌曲已加入到播放列表 $name。"
    }

    fun createPlaylist(name: String, songIds: List<String>) {
        userPlaylists.add(name to songIds.distinct())
        store.userPlaylists = userPlaylists.toList()
        snackbar = "${songIds.size} 首歌曲已加入到播放列表 $name。"
    }

    fun renamePlaylist(from: String, to: String) {
        val index = userPlaylists.indexOfFirst { it.first == from }
        if (index >= 0) {
            userPlaylists[index] = to to userPlaylists[index].second
            store.userPlaylists = userPlaylists.toList()
        }
    }

    fun deletePlaylist(name: String) {
        userPlaylists.removeAll { it.first == name }
        store.userPlaylists = userPlaylists.toList()
    }

    fun visibleTabs(): List<String> = tabs.filterNot { hiddenTabs.contains(it) }
}
