package com.blackboxbench.reproduction

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** One media entry inside the synthetic on-device storage. */
data class MediaItem(
    val rel: String,
    val name: String,
    val album: String,
    val size: Long,
    val modified: Long,
    val width: Int,
    val height: Int,
    val video: Boolean,
    val durationMs: Long
) {
    val ext: String get() = name.substringAfterLast('.', "").lowercase()
    val displayPath: String get() = "/storage/emulated/0/$rel"
    val isGif: Boolean get() = ext == "gif"
}

data class SlideshowSettings(
    val intervalSeconds: Int = 5,
    val animation: String = "滑动",
    val includeVideos: Boolean = false,
    val includeGif: Boolean = false,
    val randomOrder: Boolean = false,
    val reverseOrder: Boolean = false,
    val loop: Boolean = false
)

/**
 * Offline gallery store: the bundled synthetic media is copied into the app private
 * storage on first launch, then every album / favourite / recycle-bin operation works on
 * real files. Item level flags (favourite, hidden, recycle bin) are persisted.
 */
class GalleryStore(val context: Context) {
    val root = File(context.filesDir, "storage")
    private val prefs = context.getSharedPreferences("gallery_state", Context.MODE_PRIVATE)

    var media by mutableStateOf<List<MediaItem>>(emptyList())
        private set

    /** Bumped on every rescan so composables depending on the album tree recompose. */
    var revision by mutableStateOf(0)
        private set

    var favorites by mutableStateOf<Set<String>>(emptySet())
    var hidden by mutableStateOf<Set<String>>(emptySet())
    var binned by mutableStateOf<Map<String, Long>>(emptyMap())

    // ---- settings -------------------------------------------------------------
    var language by mutableStateOf("中文")
    var showHidden by mutableStateOf(false)
    var tempShowHidden by mutableStateOf(false)
    var searchAllFiles by mutableStateOf(false)
    var fileLoadingPriority by mutableStateOf("速度")
    var autoplayVideo by mutableStateOf(true)
    var rememberPosition by mutableStateOf(true)
    var loopVideo by mutableStateOf(false)
    var volumeBrightnessGesture by mutableStateOf(true)
    var videoTapAction by mutableStateOf("打开应用内播放器")
    var cropSquare by mutableStateOf(true)
    var animateGif by mutableStateOf(false)
    var thumbStyle by mutableStateOf("方形")
    var folderThumbStyle by mutableStateOf("方形")
    var horizontalThumbScroll by mutableStateOf(false)
    var pullToRefresh by mutableStateOf(true)
    var blackFullscreen by mutableStateOf(true)
    var hdrDisplay by mutableStateOf(true)
    var swipeDownExit by mutableStateOf(true)
    var showNotch by mutableStateOf(true)
    var allowDeepZoom by mutableStateOf(true)
    var gestureRotation by mutableStateOf(true)
    var maxQuality by mutableStateOf(false)
    var doubleTapZoom by mutableStateOf(false)
    var appLock by mutableStateOf(false)
    var lockHidden by mutableStateOf(false)
    var lockDelete by mutableStateOf(false)
    var deleteEmptyFolder by mutableStateOf(false)
    var keepModifiedDate by mutableStateOf(true)
    var skipDeleteConfirm by mutableStateOf(false)
    var bottomButtons by mutableStateOf(true)
    var recycleEnabled by mutableStateOf(true)
    var binInFolders by mutableStateOf(true)
    var binAtEnd by mutableStateOf(false)
    var cacheSize by mutableStateOf("663.9 kB")

    // ---- browsing state -------------------------------------------------------
    var sortKey by mutableStateOf("修改日期")
    var sortAscending by mutableStateOf(false)
    var groupBy by mutableStateOf("无")
    var folderColumns by mutableStateOf(2)
    var mediaColumns by mutableStateOf(3)
    var viewType by mutableStateOf("网格")
    var showFileNames by mutableStateOf(false)
    var filterImages by mutableStateOf(true)
    var filterVideos by mutableStateOf(true)
    var filterGif by mutableStateOf(true)
    var filterRaw by mutableStateOf(true)
    var filterSvg by mutableStateOf(true)
    var filterPortrait by mutableStateOf(false)
    var defaultFolder by mutableStateOf("DCIM/Camera")
    var slideshow by mutableStateOf(SlideshowSettings())

    init {
        load()
        seedIfNeeded()
        rescan()
    }

    // ---- storage --------------------------------------------------------------
    private fun seedIfNeeded() {
        val marker = File(root, ".seeded")
        if (marker.exists()) return
        copyAssetDir("albums", root)
        File(root, "Pictures/Empty").mkdirs()
        runCatching { marker.createNewFile() }
    }

    private fun copyAssetDir(assetPath: String, target: File) {
        val children = context.assets.list(assetPath) ?: return
        if (children.isEmpty()) {
            target.parentFile?.mkdirs()
            runCatching {
                context.assets.open(assetPath).use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
            }
            return
        }
        target.mkdirs()
        for (child in children) {
            copyAssetDir("$assetPath/$child", File(target, child))
        }
    }

    fun rescan() {
        val found = ArrayList<MediaItem>()
        val stack = ArrayDeque<File>()
        if (root.exists()) stack.add(root)
        while (stack.isNotEmpty()) {
            val dir = stack.removeLast()
            val children = dir.listFiles() ?: continue
            for (file in children) {
                if (file.isDirectory) {
                    stack.add(file)
                } else if (!file.name.startsWith(".")) {
                    val ext = file.name.substringAfterLast('.', "").lowercase()
                    if (ext in PHOTO_EXT || ext in VIDEO_EXT) {
                        found.add(describe(file))
                    }
                }
            }
        }
        media = found
        revision += 1
    }

    private fun describe(file: File): MediaItem {
        val rel = file.absolutePath.removePrefix(root.absolutePath + "/")
        val album = rel.substringBeforeLast('/', "")
        val ext = file.name.substringAfterLast('.', "").lowercase()
        var width = 0
        var height = 0
        var video = ext in VIDEO_EXT
        var duration = 0L
        if (video) {
            runCatching {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(file.absolutePath)
                width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    ?.toIntOrNull() ?: 0
                height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    ?.toIntOrNull() ?: 0
                duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
                retriever.release()
            }
        } else {
            runCatching {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, options)
                width = options.outWidth
                height = options.outHeight
            }
        }
        return MediaItem(
            rel = rel,
            name = file.name,
            album = album,
            size = file.length(),
            modified = file.lastModified(),
            width = width,
            height = height,
            video = video,
            durationMs = duration
        )
    }

    // ---- queries --------------------------------------------------------------
    fun albumNames(): List<String> {
        @Suppress("UNUSED_EXPRESSION") revision
        val result = LinkedHashSet<String>()
        media.filter { it.album.isNotEmpty() }.forEach { result.add(it.album) }
        val stack = ArrayDeque<Pair<File, String>>()
        if (root.exists()) stack.add(root to "")
        while (stack.isNotEmpty()) {
            val (dir, rel) = stack.removeLast()
            val children = dir.listFiles() ?: continue
            val subdirs = children.filter { it.isDirectory && !it.name.startsWith(".") }
            val files = children.filter { !it.isDirectory && !it.name.startsWith(".") }
            if (rel.isNotEmpty() && subdirs.isEmpty() && files.isEmpty()) {
                result.add(rel)
            }
            subdirs.forEach { child ->
                stack.add(child to (if (rel.isEmpty()) child.name else "$rel/${child.name}"))
            }
        }
        return result.sorted()
    }

    fun itemsOf(album: String): List<MediaItem> =
        sorted(media.filter { it.album == album && visible(it) })

    fun visible(item: MediaItem): Boolean {
        if (binned.containsKey(item.rel)) return false
        if (hidden.contains(item.rel) && !(showHidden || tempShowHidden)) return false
        if (item.video && !filterVideos) return false
        if (!item.video && !filterImages) return false
        if (item.isGif && !filterGif) return false
        if (item.ext == "svg" && !filterSvg) return false
        if ((item.ext == "dng" || item.ext == "raw") && !filterRaw) return false
        if (filterPortrait && item.width in 1..item.height) return false
        return true
    }

    fun sorted(list: List<MediaItem>): List<MediaItem> {
        val comparator: Comparator<MediaItem> = when (sortKey) {
            "名称" -> compareBy { it.name.lowercase() }
            "路径" -> compareBy { it.rel.lowercase() }
            "大小" -> compareBy { it.size }
            "项数" -> compareBy { it.album }
            "拍摄日期" -> compareBy { it.modified }
            "随机" -> Comparator { a, b -> a.rel.hashCode().compareTo(b.rel.hashCode()) }
            else -> compareBy { it.modified }
        }
        val result = list.sortedWith(comparator)
        return if (sortAscending) result else result.reversed()
    }

    fun favoriteItems(): List<MediaItem> = sorted(media.filter { favorites.contains(it.rel) && visible(it) })

    fun binItems(): List<MediaItem> {
        val stamp = binned
        return media.filter { stamp.containsKey(it.rel) }.sortedByDescending { stamp[it.rel] ?: 0L }
    }

    fun albumCover(album: String): MediaItem? = sorted(media.filter { it.album == album && visible(it) }).firstOrNull()

    fun allMedia(): List<MediaItem> = sorted(media.filter { visible(it) })

    fun find(rel: String): MediaItem? = media.firstOrNull { it.rel == rel }

    fun searchItems(query: String): List<MediaItem> {
        if (query.isBlank()) return emptyList()
        val needle = query.lowercase()
        return sorted(media.filter { visible(it) && (it.name.lowercase().contains(needle) || it.album.lowercase().contains(needle)) })
    }

    fun searchAlbums(query: String): List<String> {
        if (query.isBlank()) return albumNames()
        val needle = query.lowercase()
        return albumNames().filter { it.lowercase().contains(needle) }
    }

    // ---- mutations ------------------------------------------------------------
    fun toggleFavorite(item: MediaItem) {
        favorites = if (favorites.contains(item.rel)) favorites - item.rel else favorites + item.rel
        save()
    }

    fun setFavorite(items: List<MediaItem>, value: Boolean) {
        val set = favorites.toMutableSet()
        items.forEach { if (value) set.add(it.rel) else set.remove(it.rel) }
        favorites = set
        save()
    }

    fun setHidden(items: List<MediaItem>, value: Boolean) {
        val set = hidden.toMutableSet()
        items.forEach { if (value) set.add(it.rel) else set.remove(it.rel) }
        hidden = set
        save()
    }

    fun moveToBin(items: List<MediaItem>) {
        if (!recycleEnabled) {
            items.forEach { fileOf(it)?.delete() }
            rescan()
            return
        }
        val map = binned.toMutableMap()
        val now = System.currentTimeMillis()
        items.forEach { map[it.rel] = now }
        binned = map
        save()
    }

    fun restore(items: List<MediaItem>) {
        val map = binned.toMutableMap()
        items.forEach { map.remove(it.rel) }
        binned = map
        save()
    }

    fun emptyBin() {
        binned = emptyMap()
        save()
    }

    fun deleteForever(items: List<MediaItem>) {
        items.forEach { fileOf(it)?.delete() }
        val map = binned.toMutableMap()
        items.forEach { map.remove(it.rel) }
        binned = map
        rescan()
        save()
    }

    fun fileOf(item: MediaItem): File? = media.firstOrNull { it.rel == item.rel }?.let { File(root, it.rel) }

    fun rename(item: MediaItem, title: String, extension: String): Boolean {
        val file = File(root, item.rel) ?: return false
        if (!file.exists()) return false
        val target = File(file.parentFile, "$title.$extension")
        val ok = runCatching { file.renameTo(target) }.getOrDefault(false)
        if (ok) {
            val moved = item.rel
            if (favorites.contains(moved)) {
                val folder = moved.substringBeforeLast('/', "")
                val newRel = if (folder.isEmpty()) target.name else "$folder/${target.name}"
                favorites = favorites - moved + newRel
            }
            rescan()
            save()
        }
        return ok
    }

    fun createFolder(path: String, title: String): Boolean {
        val dir = File(root, path.trim('/') + "/" + title)
        return runCatching { dir.mkdirs() }.getOrDefault(false)
    }

    fun copyItems(items: List<MediaItem>, targetAlbum: String): Int {
        var count = 0
        items.forEach { item ->
            val source = File(root, item.rel)
            if (!source.exists()) return@forEach
            val dir = File(root, targetAlbum)
            dir.mkdirs()
            var target = File(dir, item.name)
            var index = 1
            while (target.exists()) {
                val base = item.name.substringBeforeLast('.')
                val ext = item.name.substringAfterLast('.', "")
                target = File(dir, "${base}_$index.$ext")
                index++
            }
            if (runCatching { source.copyTo(target) }.isSuccess) count++
        }
        rescan()
        return count
    }

    fun moveItems(items: List<MediaItem>, targetAlbum: String): Int {
        var count = 0
        items.forEach { item ->
            val source = File(root, item.rel)
            if (!source.exists()) return@forEach
            val dir = File(root, targetAlbum)
            dir.mkdirs()
            val target = File(dir, item.name)
            if (runCatching { source.copyTo(target, overwrite = true) }.isSuccess) {
                source.delete()
                count++
            }
        }
        rescan()
        return count
    }

    /** Rotation is stored in memory only; confirming it writes a rotated copy (另存为). */
    val rotations = HashMap<String, Int>()

    fun saveAs(item: MediaItem, path: String, name: String, extension: String, degrees: Int): Boolean {
        val source = File(root, item.rel)
        if (!source.exists()) return false
        val dir = File(root, path.trim('/'))
        dir.mkdirs()
        val target = File(dir, "$name.$extension")
        val ok = if (degrees % 360 == 0) {
            runCatching { source.copyTo(target, overwrite = true) }.isSuccess
        } else {
            runCatching {
                val bitmap = BitmapFactory.decodeFile(source.absolutePath) ?: return false
                val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
                val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                val format = if (extension.lowercase() in setOf("jpg", "jpeg")) {
                    Bitmap.CompressFormat.JPEG
                } else {
                    Bitmap.CompressFormat.PNG
                }
                target.outputStream().use { output -> rotated.compress(format, 95, output) }
                true
            }.getOrDefault(false)
        }
        if (ok) rescan()
        return ok
    }

    fun clearCache() {
        cacheSize = "0 B"
    }

    // ---- persistence ----------------------------------------------------------
    private fun load() {
        val raw = prefs.getString("state", null) ?: return
        runCatching {
            val json = JSONObject(raw)
            favorites = json.optJSONArray("favorites").toStringSet()
            hidden = json.optJSONArray("hidden").toStringSet()
            val bin = LinkedHashMap<String, Long>()
            val binJson = json.optJSONObject("binned")
            binJson?.keys()?.forEach { key -> bin[key] = binJson.optLong(key) }
            binned = bin
            val s = json.optJSONObject("settings") ?: JSONObject()
            language = s.optString("language", language)
            showHidden = s.optBoolean("showHidden", showHidden)
            searchAllFiles = s.optBoolean("searchAllFiles", searchAllFiles)
            autoplayVideo = s.optBoolean("autoplayVideo", autoplayVideo)
            loopVideo = s.optBoolean("loopVideo", loopVideo)
            cropSquare = s.optBoolean("cropSquare", cropSquare)
            animateGif = s.optBoolean("animateGif", animateGif)
            pullToRefresh = s.optBoolean("pullToRefresh", pullToRefresh)
            blackFullscreen = s.optBoolean("blackFullscreen", blackFullscreen)
            hdrDisplay = s.optBoolean("hdrDisplay", hdrDisplay)
            swipeDownExit = s.optBoolean("swipeDownExit", swipeDownExit)
            showNotch = s.optBoolean("showNotch", showNotch)
            allowDeepZoom = s.optBoolean("allowDeepZoom", allowDeepZoom)
            gestureRotation = s.optBoolean("gestureRotation", gestureRotation)
            appLock = s.optBoolean("appLock", appLock)
            recycleEnabled = s.optBoolean("recycleEnabled", recycleEnabled)
            binInFolders = s.optBoolean("binInFolders", binInFolders)
            binAtEnd = s.optBoolean("binAtEnd", binAtEnd)
            bottomButtons = s.optBoolean("bottomButtons", bottomButtons)
            keepModifiedDate = s.optBoolean("keepModifiedDate", keepModifiedDate)
            deleteEmptyFolder = s.optBoolean("deleteEmptyFolder", deleteEmptyFolder)
            sortKey = s.optString("sortKey", sortKey)
            sortAscending = s.optBoolean("sortAscending", sortAscending)
            folderColumns = s.optInt("folderColumns", folderColumns)
            mediaColumns = s.optInt("mediaColumns", mediaColumns)
            showFileNames = s.optBoolean("showFileNames", showFileNames)
            viewType = s.optString("viewType", viewType)
            filterImages = s.optBoolean("filterImages", filterImages)
            filterVideos = s.optBoolean("filterVideos", filterVideos)
            filterGif = s.optBoolean("filterGif", filterGif)
            filterRaw = s.optBoolean("filterRaw", filterRaw)
            filterSvg = s.optBoolean("filterSvg", filterSvg)
            filterPortrait = s.optBoolean("filterPortrait", filterPortrait)
        }
    }

    fun save() {
        val json = JSONObject()
        json.put("favorites", JSONArray(favorites.toList()))
        json.put("hidden", JSONArray(hidden.toList()))
        json.put("binned", JSONObject(binned as Map<*, *>))
        val s = JSONObject()
        s.put("language", language)
        s.put("showHidden", showHidden)
        s.put("searchAllFiles", searchAllFiles)
        s.put("autoplayVideo", autoplayVideo)
        s.put("loopVideo", loopVideo)
        s.put("cropSquare", cropSquare)
        s.put("animateGif", animateGif)
        s.put("pullToRefresh", pullToRefresh)
        s.put("blackFullscreen", blackFullscreen)
        s.put("hdrDisplay", hdrDisplay)
        s.put("swipeDownExit", swipeDownExit)
        s.put("showNotch", showNotch)
        s.put("allowDeepZoom", allowDeepZoom)
        s.put("gestureRotation", gestureRotation)
        s.put("appLock", appLock)
        s.put("recycleEnabled", recycleEnabled)
        s.put("binInFolders", binInFolders)
        s.put("binAtEnd", binAtEnd)
        s.put("bottomButtons", bottomButtons)
        s.put("keepModifiedDate", keepModifiedDate)
        s.put("deleteEmptyFolder", deleteEmptyFolder)
        s.put("sortKey", sortKey)
        s.put("sortAscending", sortAscending)
        s.put("folderColumns", folderColumns)
        s.put("mediaColumns", mediaColumns)
        s.put("showFileNames", showFileNames)
        s.put("viewType", viewType)
        s.put("filterImages", filterImages)
        s.put("filterVideos", filterVideos)
        s.put("filterGif", filterGif)
        s.put("filterRaw", filterRaw)
        s.put("filterSvg", filterSvg)
        s.put("filterPortrait", filterPortrait)
        json.put("settings", s)
        prefs.edit().putString("state", json.toString()).apply()
    }

    fun resetAll() {
        prefs.edit().clear().apply()
        favorites = emptySet()
        hidden = emptySet()
        binned = emptyMap()
    }

    companion object {
        val PHOTO_EXT = setOf("png", "jpg", "jpeg", "webp", "bmp", "gif", "svg", "dng")
        val VIDEO_EXT = setOf("mp4", "mkv", "webm", "3gp", "avi", "mov")
    }
}

private fun JSONArray?.toStringSet(): Set<String> {
    if (this == null) return emptySet()
    val set = LinkedHashSet<String>()
    for (index in 0 until length()) {
        optString(index)?.takeIf { it.isNotEmpty() }?.let { set.add(it) }
    }
    return set
}

// ---- formatting helpers --------------------------------------------------------

private val MONTHS = listOf("一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月")

fun formatSize(bytes: Long): String = when {
    bytes >= 1024L * 1024L * 1024L -> String.format(Locale.US, "%.1f GB", bytes / 1024.0 / 1024.0 / 1024.0)
    bytes >= 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes / 1024.0 / 1024.0)
    bytes >= 1024L -> String.format(Locale.US, "%.1f kB", bytes / 1024.0)
    else -> "$bytes B"
}

fun formatDate(millis: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(millis))

fun formatDayTitle(millis: Long): String {
    val calendar = Calendar.getInstance().apply { timeInMillis = millis }
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val month = MONTHS[calendar.get(Calendar.MONTH)]
    return "$day $month ${calendar.get(Calendar.YEAR)}"
}

fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
