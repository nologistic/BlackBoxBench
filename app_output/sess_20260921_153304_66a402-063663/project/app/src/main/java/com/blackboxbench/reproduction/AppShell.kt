package com.blackboxbench.reproduction

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList

sealed interface Screen {
    data object Albums : Screen
    data object Timeline : Screen
    data class Album(val path: String) : Screen
    data class Viewer(val albumPath: String, val ids: List<String>, val index: Int) : Screen
    data object Trash : Screen
    data object Settings : Screen
    data object Appearance : Screen
    data object About : Screen
    data class ManageFolders(val excluded: Boolean) : Screen
    data object Slideshow : Screen
}

/** Holds the whole reproduction state: library, preferences and the screen stack. */
class AppController(context: Context) {
    val appContext: Context = context.applicationContext
    val repo = MediaRepository(context)
    val settings = SettingsStore(context)

    /** Starts an external activity (share / open with / set as) and reports failures. */
    fun launch(intent: android.content.Intent) {
        try {
            appContext.startActivity(intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            toast("没有可用的应用")
        }
    }

    val stack: SnapshotStateList<Screen> = mutableStateListOf<Screen>(Screen.Albums)
    var message by mutableStateOf<String?>(null)
    var unlocked by mutableStateOf(false)
    var pendingLockAction by mutableStateOf<(() -> Unit)?>(null)

    val current: Screen get() = stack.last()

    fun push(screen: Screen) { stack.add(screen) }

    fun replaceTop(screen: Screen) { stack[stack.size - 1] = screen }

    fun pop(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.size - 1)
        return true
    }

    fun popToAlbums() {
        while (stack.size > 1) stack.removeAt(stack.size - 1)
    }

    fun toast(text: String) { message = text }

    /** Runs [action] immediately or after a successful password check. */
    fun guarded(action: () -> Unit) {
        if (settings.protectDeleteMove.get() && settings.anyPasswordSet() && !unlocked) {
            pendingLockAction = action
            unlocked = false
            push(Screen.Albums)
        } else action()
    }

    fun showHiddenNow(): Boolean =
        settings.showHidden.get() || temporaryHiddenReveal

    var temporaryHiddenReveal by mutableStateOf(false)

    fun itemsOfAlbum(path: String): List<MediaItem> = sorted(repo.albumItems(path, showHiddenNow()))

    fun sorted(list: List<MediaItem>): List<MediaItem> {
        val by = settings.sortBy.get()
        val asc = settings.sortAscending.get()
        val comparator = when (by) {
            "路径" -> compareBy<MediaItem> { it.albumPath }.thenBy { it.fileName.lowercase() }
            "大小" -> compareBy { it.sizeBytes }
            "修改日期" -> compareBy { it.modifiedMs }
            "拍摄日期" -> compareBy { it.takenMs }
            "随机" -> Comparator { a, b -> a.id.hashCode().compareTo(b.id.hashCode()) }
            else -> compareBy { it.fileName.lowercase() }
        }
        return if (asc) list.sortedWith(comparator) else list.sortedWith(comparator.reversed())
    }

    fun albums(): List<Album> {
        val list = repo.visibleAlbums(showHiddenNow()).toMutableList()
        val excluded = settings.excludeFolders.get().split(",").filter { it.isNotBlank() }
        val included = settings.includeFolders.get().split(",").filter { it.isNotBlank() }
        var filtered = list.filter { album -> excluded.none { album.path.startsWith(it) || album.name == it } }
        if (included.isNotEmpty()) filtered = filtered.filter { album -> included.any { album.path.startsWith(it) || album.name == it } }
        filtered = filtered.filter { album ->
            val items = repo.albumItems(album.path, showHiddenNow())
            items.isNotEmpty() || album.empty
        }
        return when (settings.sortBy.get()) {
            "大小" -> if (settings.sortAscending.get()) filtered.sortedBy { repo.albumItems(it.path, showHiddenNow()).sumOf { m -> m.sizeBytes } }
            else filtered.sortedByDescending { repo.albumItems(it.path, showHiddenNow()).sumOf { m -> m.sizeBytes } }
            "修改日期", "拍摄日期" -> if (settings.sortAscending.get()) filtered.sortedBy { repo.albumItems(it.path, showHiddenNow()).minOfOrNull { m -> m.modifiedMs } ?: 0 }
            else filtered.sortedByDescending { repo.albumItems(it.path, showHiddenNow()).minOfOrNull { m -> m.modifiedMs } ?: 0 }
            else -> if (settings.sortAscending.get()) filtered.sortedBy { it.name.lowercase() } else filtered.sortedByDescending { it.name.lowercase() }
        }
    }

    /** All visible media, used by the "all media / timeline" screen. */
    fun allItems(): List<MediaItem> = sorted(repo.visibleItems(showHiddenNow()))

    fun favorites(): List<MediaItem> = sorted(repo.favorites())

    fun filterItems(list: List<MediaItem>): List<MediaItem> = list.filter { item ->
        when (item.kind) {
            MediaKind.VIDEO -> settings.filterVideos.get()
            MediaKind.GIF -> settings.filterGif.get()
            MediaKind.RAW -> settings.filterRaw.get()
            MediaKind.SVG -> settings.filterSvg.get()
            else -> settings.filterImages.get()
        }
    }

    fun refresh() { settings.revision.value = settings.revision.value + 1 }
}
