package com.blackboxbench.reproduction

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray

sealed class Screen {
    data object Albums : Screen()
    data class Album(val name: String, val isRecycle: Boolean = false) : Screen()
    data class Viewer(val relPath: String, val album: String, val isRecycle: Boolean = false) : Screen()
    data class Editor(val relPath: String, val album: String) : Screen()
    data class VideoViewer(val relPath: String, val album: String) : Screen()
    data object Settings : Screen()
    data object About : Screen()
    data object Search : Screen()
    data object FolderPicker : Screen()
}

class GalleryState(val context: Context) {
    val library = Library(context)

    var revision by mutableStateOf(0)
        private set

    val stack = mutableStateListOf<Screen>(Screen.Albums)
    val current: Screen get() = stack.last()

    // transient ui state
    var selection by mutableStateOf<Set<String>>(emptySet())
    var searchQuery by mutableStateOf("")
    var searchFiles by mutableStateOf(false)
    var showHiddenTemp by mutableStateOf(false)
    var showExcludedTemp by mutableStateOf(false)
    var locked by mutableStateOf(false)
    var toast by mutableStateOf<String?>(null)

    init {
        library.seedIfNeeded()
        locked = library.setting("lockApp", false)
    }

    fun refresh() {
        revision++
    }

    fun push(screen: Screen) {
        selection = emptySet()
        stack.add(screen)
    }

    fun replace(screen: Screen) {
        if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
        stack.add(screen)
    }

    fun pop(): Boolean {
        if (selection.isNotEmpty()) {
            selection = emptySet()
            return true
        }
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        return true
    }

    fun popToAlbums() {
        while (stack.size > 1) stack.removeAt(stack.lastIndex)
        selection = emptySet()
    }

    // ------------------------------------------------------------- settings io

    fun bool(key: String, def: Boolean): Boolean {
        revision
        return library.setting(key, def)
    }

    fun setBool(key: String, value: Boolean) {
        library.setSetting(key, value)
        if (key == "lockApp" && !value) locked = false
        revision++
    }

    fun text(key: String, def: String): String {
        revision
        return library.string(key, def)
    }

    fun setText(key: String, value: String) {
        library.setString(key, value)
        revision++
    }

    fun list(key: String): List<String> {
        revision
        val array = library.jsonArray(key)
        return (0 until array.length()).map { array.optString(it) }
    }

    fun setList(key: String, values: List<String>) {
        val array = JSONArray()
        values.forEach { array.put(it) }
        library.setJsonArray(key, array)
        revision++
    }

    // -------------------------------------------------------------- filtering

    fun filter(): FileFilter {
        revision
        return FileFilter(
            images = bool("filterImages", true),
            videos = bool("filterVideos", true),
            gif = bool("filterGif", true),
            raw = bool("filterRaw", true),
            svg = bool("filterSvg", true),
            portrait = bool("filterPortrait", false),
        )
    }

    fun setFilter(filter: FileFilter) {
        setBool("filterImages", filter.images)
        setBool("filterVideos", filter.videos)
        setBool("filterGif", filter.gif)
        setBool("filterRaw", filter.raw)
        setBool("filterSvg", filter.svg)
        setBool("filterPortrait", filter.portrait)
        // setBool already bumps revision, keep a single extra bump for safety
        revision++
    }

    fun showHidden(): Boolean = showHiddenTemp || bool("showHidden", false)

    fun albums(): List<Album> = library.albums(showHidden(), filter())

    fun albumItems(album: String, isRecycle: Boolean): List<MediaItem> {
        revision
        val raw = if (isRecycle) library.trashItems()
        else library.itemsOfAlbum(album, showHidden(), filter())
        val sorted = sortItems(raw, bool("sort$album", false))
        return sorted
    }

    private fun sortItems(items: List<MediaItem>, dummy: Boolean): List<MediaItem> {
        val mode = text("fileSortMode", SortMode.MODIFIED.name)
        val ascending = bool("fileSortAsc", false)
        val comparator: Comparator<MediaItem> = when (SortMode.valueOf(mode)) {
            SortMode.NAME -> compareBy { it.displayName.lowercase() }
            SortMode.PATH -> compareBy { it.relPath.lowercase() }
            SortMode.SIZE -> compareBy { it.sizeBytes }
            SortMode.MODIFIED -> compareBy { it.modified }
            SortMode.TAKEN -> compareBy { it.taken }
            SortMode.RANDOM, SortMode.COUNT, SortMode.CUSTOM -> compareBy { it.modified }
        }
        val result = if (ascending) items.sortedWith(comparator) else items.sortedWith(comparator.reversed())
        return result
    }

    fun groupMode(): GroupMode {
        val stored = text("groupMode", GroupMode.NONE.name)
        return try {
            GroupMode.valueOf(stored)
        } catch (_: Exception) {
            GroupMode.NONE
        }
    }

    fun sectionOf(item: MediaItem): String? = when (groupMode()) {
        GroupMode.NONE -> null
        GroupMode.MODIFIED_DAY -> Dates.dayLabel(item.modified)
        GroupMode.MODIFIED_MONTH -> Dates.monthLabel(item.modified)
        GroupMode.TAKEN_DAY -> Dates.dayLabel(item.taken)
        GroupMode.TAKEN_MONTH -> Dates.monthLabel(item.taken)
        GroupMode.TYPE -> if (item.isVideo) "视频" else "图片"
        GroupMode.EXTENSION -> item.extension.uppercase()
    }

    // ------------------------------------------------------------- selection

    fun toggleSelection(relPath: String) {
        selection = if (selection.contains(relPath)) selection - relPath else selection + relPath
    }

    fun selectAll(items: List<MediaItem>) {
        selection = items.map { it.relPath }.toSet()
    }

    fun selectedItems(all: List<MediaItem>): List<MediaItem> = all.filter { selection.contains(it.relPath) }

    fun newerThan(relPath: String, album: String): MediaItem? = library.itemByPath(relPath)

    fun message(text: String) {
        toast = text
    }
}
