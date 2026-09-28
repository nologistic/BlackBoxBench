package com.blackboxbench.reproduction

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale

data class AlbumDef(val name: String, val path: String)

data class MediaItem(
    val id: String,
    val album: String,
    val albumPath: String,
    val name: String,
    val uri: String,
    val isVideo: Boolean,
    val sizeBytes: Long,
    val durationMs: Long,
    val dateLabel: String,
    val favorite: Boolean = false,
    val hidden: Boolean = false,
    val deleted: Boolean = false,
) {
    val isFile: Boolean get() = uri.startsWith("file:")
    val path: String get() = if (isFile) uri.removePrefix("file:") else uri
    val extension: String get() = name.substringAfterLast('.', "")
    val title: String get() = name.substringBeforeLast('.', name)
}

fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f kB", kb)
    return String.format(Locale.US, "%.1f MB", kb / 1024.0)
}

fun formatDuration(ms: Long): String {
    val total = (ms / 1000).toInt()
    return String.format(Locale.US, "%02d:%02d", total / 60, total % 60)
}

/** Central in-memory model of the gallery, mirroring the observed app behaviour. */
class GalleryState(val context: Context) {

    val albums = mutableStateListOf<AlbumDef>()
    val items = mutableStateListOf<MediaItem>()

    // ---- persistent user settings -------------------------------------------
    var showHiddenItems by mutableStateOf(false)
    var showRecycleBinInFolders by mutableStateOf(true)
    var searchAllFiles by mutableStateOf(false)
    var albumColumns by mutableStateOf(2)
    var mediaColumns by mutableStateOf(3)
    var albumSortKey by mutableStateOf("修改日期")
    var albumSortAscending by mutableStateOf(false)
    var mediaSortKey by mutableStateOf("名称")
    var mediaSortAscending by mutableStateOf(true)
    var groupBy by mutableStateOf("不分组文件")
    var recycleBinEnabled by mutableStateOf(true)
    var showFilenames by mutableStateOf(false)

    // ---- transient UI state --------------------------------------------------
    var tempShowHidden by mutableStateOf(false)
    var tempShowExcluded by mutableStateOf(false)
    var allMediaMode by mutableStateOf(false)

    private var tick by mutableIntStateOf(0)
    fun touch() { tick++ }
    val revision: Int get() = tick

    // ---- loading -------------------------------------------------------------
    init {
        if (!load()) seedFromAssets()
    }

    private fun seedFromAssets() {
        val root = JSONObject(AssetStore.readText(context, "library.json"))
        val arr = root.getJSONArray("albums")
        for (i in 0 until arr.length()) {
            val a = arr.getJSONObject(i)
            val name = a.getString("name")
            val path = a.getString("path")
            albums.add(AlbumDef(name, path))
            val its = a.getJSONArray("items")
            for (j in 0 until its.length()) {
                val o = its.getJSONObject(j)
                val fileName = o.getString("name")
                items.add(
                    MediaItem(
                        id = "$name/$fileName",
                        album = name,
                        albumPath = path,
                        name = fileName,
                        uri = o.getString("file"),
                        isVideo = o.optBoolean("video", false),
                        sizeBytes = o.optLong("size", 0),
                        durationMs = o.optLong("duration", 0),
                        dateLabel = o.optString("date", ""),
                    )
                )
            }
        }
    }

    // ---- queries -------------------------------------------------------------
    fun visibleItems(album: String): List<MediaItem> {
        val showHidden = showHiddenItems || tempShowHidden
        var list = items.filter { it.album == album && !it.deleted && (showHidden || !it.hidden) }
        list = applySort(list, if (album == FAVORITES) mediaSortKey else mediaSortKey, mediaSortAscending)
        return list
    }

    fun favoriteItems(): List<MediaItem> {
        val showHidden = showHiddenItems || tempShowHidden
        return items.filter { it.favorite && !it.deleted && (showHidden || !it.hidden) }
    }

    fun recycledItems(): List<MediaItem> = items.filter { it.deleted }

    fun allMediaItems(): List<MediaItem> {
        val showHidden = showHiddenItems || tempShowHidden
        return applySort(items.filter { !it.deleted && (showHidden || !it.hidden) }, "拍摄日期", true)
    }

    fun albumCount(name: String): Int = items.count { it.album == name && !it.deleted }

    fun albumCover(name: String): MediaItem? =
        items.firstOrNull { it.album == name && !it.deleted }
            ?: items.firstOrNull { it.album == name }

    fun byId(id: String): MediaItem? = items.firstOrNull { it.id == id }

    private fun applySort(list: List<MediaItem>, key: String, asc: Boolean): List<MediaItem> {
        val sorted = when (key) {
            "名称" -> list.sortedBy { it.name.lowercase() }
            "路径" -> list.sortedBy { it.path.lowercase() }
            "大小" -> list.sortedBy { it.sizeBytes }
            "拍摄日期", "修改日期" -> list.sortedBy { it.dateLabel }
            "随机" -> list.shuffled()
            else -> list
        }
        return if (asc) sorted else sorted.reversed()
    }

    // ---- mutations -----------------------------------------------------------
    fun delete(ids: Collection<String>) {
        for (id in ids) replace(id) { it.copy(deleted = true) }
    }

    fun deletePermanently(ids: Collection<String>) {
        for (id in ids) {
            val it = byId(id) ?: continue
            if (it.isFile) runCatching { File(it.path).delete() }
        }
        items.removeAll { it.id in ids }
    }

    fun restore(ids: Collection<String>) {
        for (id in ids) replace(id) { it.copy(deleted = false) }
    }

    fun setFavorite(ids: Collection<String>, value: Boolean) {
        for (id in ids) replace(id) { it.copy(favorite = value) }
    }

    fun toggleFavorite(id: String) {
        replace(id) { it.copy(favorite = !it.favorite) }
    }

    fun setHidden(ids: Collection<String>, value: Boolean) {
        for (id in ids) replace(id) { it.copy(hidden = value) }
    }

    fun rename(id: String, newName: String) {
        replace(id) { it.copy(name = newName) }
    }

    fun addItem(item: MediaItem) {
        items.add(item)
    }

    private inline fun replace(id: String, transform: (MediaItem) -> MediaItem) {
        val idx = items.indexOfFirst { it.id == id }
        if (idx >= 0) { items[idx] = transform(items[idx]); touch() }
    }

    // ---- persistence ---------------------------------------------------------
    private fun prefs() = context.getSharedPreferences("gallery_state", Context.MODE_PRIVATE)

    fun persist() {
        val root = JSONObject()
        val its = JSONArray()
        for (it in items) {
            its.put(JSONObject().apply {
                put("id", it.id); put("album", it.album); put("albumPath", it.albumPath)
                put("name", it.name); put("uri", it.uri); put("video", it.isVideo)
                put("size", it.sizeBytes); put("duration", it.durationMs); put("date", it.dateLabel)
                put("favorite", it.favorite); put("hidden", it.hidden); put("deleted", it.deleted)
            })
        }
        root.put("items", its)
        val als = JSONArray()
        for (a in albums) als.put(JSONObject().apply { put("name", a.name); put("path", a.path) })
        root.put("albums", als)
        root.put("showHiddenItems", showHiddenItems)
        root.put("searchAllFiles", searchAllFiles)
        root.put("recycleBinEnabled", recycleBinEnabled)
        prefs().edit().putString("state", root.toString()).apply()
    }

    private fun load(): Boolean {
        val raw = prefs().getString("state", null) ?: return false
        return runCatching {
            val root = JSONObject(raw)
            val als = root.optJSONArray("albums") ?: return false
            for (i in 0 until als.length()) {
                val a = als.getJSONObject(i)
                albums.add(AlbumDef(a.getString("name"), a.getString("path")))
            }
            val its = root.getJSONArray("items")
            for (i in 0 until its.length()) {
                val o = its.getJSONObject(i)
                items.add(
                    MediaItem(
                        id = o.getString("id"),
                        album = o.getString("album"),
                        albumPath = o.getString("albumPath"),
                        name = o.getString("name"),
                        uri = o.getString("uri"),
                        isVideo = o.optBoolean("video", false),
                        sizeBytes = o.optLong("size", 0),
                        durationMs = o.optLong("duration", 0),
                        dateLabel = o.optString("date", ""),
                        favorite = o.optBoolean("favorite", false),
                        hidden = o.optBoolean("hidden", false),
                        deleted = o.optBoolean("deleted", false),
                    )
                )
            }
            showHiddenItems = root.optBoolean("showHiddenItems", false)
            searchAllFiles = root.optBoolean("searchAllFiles", false)
            recycleBinEnabled = root.optBoolean("recycleBinEnabled", true)
            albums.isNotEmpty()
        }.getOrDefault(false)
    }

    fun reset() {
        prefs().edit().clear().apply()
        albums.clear(); items.clear()
        seedFromAssets()
        touch()
    }

    companion object {
        const val FAVORITES = "收藏"
        const val RECYCLE = "回收站"

        private val cache = LruCache<String, Bitmap>(32 * 1024 * 1024)

        fun bitmap(context: Context, item: MediaItem): Bitmap? {
            val key = item.uri
            cache.get(key)?.let { return it }
            val bmp = runCatching {
                if (item.isFile) BitmapFactory.decodeFile(item.path)
                else context.assets.open(item.path).use { BitmapFactory.decodeStream(it) }
            }.getOrNull() ?: return null
            cache.put(key, bmp)
            return bmp
        }

        fun bitmapOf(context: Context, uri: String): Bitmap? = runCatching {
            if (uri.startsWith("file:")) BitmapFactory.decodeFile(uri.removePrefix("file:"))
            else context.assets.open(uri).use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
    }
}
