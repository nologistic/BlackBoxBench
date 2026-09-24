package com.blackboxbench.reproduction

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

const val ALL_MEDIA = "__all__"

class MediaItem(
    val id: String,
    var name: String,
    var folder: String,
    val isVideo: Boolean,
    var size: Long,
    var modified: Long,
    var width: Int,
    var height: Int,
    var durationSec: Int,
    var deleted: Boolean = false,
    var deletedFrom: String? = null
) {
    val extension: String get() = name.substringAfterLast('.', "")
    val title: String get() = name.substringBeforeLast('.')
}

data class FolderInfo(
    val name: String,
    val count: Int,
    val coverId: String?,
    val isVirtual: Boolean = false
)

class GalleryRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("gallery_prefs", Context.MODE_PRIVATE)
    val rootDir: File = File(context.filesDir, "media")

    val items: SnapshotStateList<MediaItem> = mutableStateListOf()
    var version = mutableStateOf(0)

    var favorites = mutableStateOf(loadStringSet("favorites"))
    var pinnedFolders = mutableStateOf(loadStringSet("pinned"))
    var excludedFolders = mutableStateOf(loadStringSet("excluded"))
    var hiddenFolders = mutableStateOf(loadStringSet("hiddenFolders"))
    var hiddenMedia = mutableStateOf(loadStringSet("hiddenMedia"))
    var extraFolders = mutableStateOf(loadStringSet("extraFolders"))
    var defaultFolder = mutableStateOf(prefs.getString("defaultFolder", null))
    var sortMode = mutableStateOf(prefs.getString("sortMode", "修改日期") ?: "修改日期")
    var sortAsc = mutableStateOf(prefs.getBoolean("sortAsc", false))
    var columnsMain = mutableStateOf(prefs.getInt("columnsMain", 2))
    var columnsFolder = mutableStateOf(prefs.getInt("columnsFolder", 2))
    var showHiddenSetting = mutableStateOf(prefs.getBoolean("showHidden", false))
    var groupMode = mutableStateOf(prefs.getString("groupMode", "不分组文件") ?: "不分组文件")
    var groupAsc = mutableStateOf(prefs.getBoolean("groupAsc", true))
    var useRecycleBin = mutableStateOf(prefs.getBoolean("useRecycleBin", true))
    var askDeleteConfirm = mutableStateOf(prefs.getBoolean("askDeleteConfirm", true))
    var filters = mutableStateOf(loadStringSet("filters").ifEmpty { setOf("图片", "视频", "GIF", "RAW图像", "SVG") })
    private var deletedMap = mutableStateOf(loadDeletedMap())
    var covers = mutableStateOf(loadCoverMap())

    private val fileMap = HashMap<String, File>()
    private val thumbCache = HashMap<String, Bitmap>()

    fun fileFor(item: MediaItem): File = fileMap[item.id] ?: File(rootDir, item.id)

    fun init() {
        if (!rootDir.exists()) {
            rootDir.mkdirs()
            copyAssetTree("media", rootDir)
            staggerModified()
        }
        if (items.isEmpty()) scanDisk()
    }

    private fun copyAssetTree(assetPath: String, dest: File) {
        val children = context.assets.list(assetPath) ?: return
        if (children.isEmpty()) {
            dest.parentFile?.mkdirs()
            context.assets.open(assetPath).use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
        } else {
            dest.mkdirs()
            for (child in children) copyAssetTree("$assetPath/$child", File(dest, child))
        }
    }

    private fun staggerModified() {
        val now = System.currentTimeMillis()
        var offset = 0L
        rootDir.listFiles()?.sortedBy { it.name }?.forEach { dir ->
            dir.listFiles()?.sortedBy { it.name }?.forEach { f ->
                f.setLastModified(now - offset)
                offset += 86_400_000L / 3
            }
        }
    }

    private fun scanDisk() {
        val meta = mutableMapOf<String, JSONObject>()
        val metaFile = File(rootDir, ".meta.json")
        if (metaFile.exists()) {
            val obj = JSONObject(metaFile.readText())
            obj.keys().forEach { meta[it] = obj.getJSONObject(it) }
        }
        items.clear()
        fileMap.clear()
        rootDir.listFiles()?.filter { it.isDirectory }?.forEach { dir ->
            dir.listFiles()?.filter { it.isFile }?.forEach { f ->
                val id = "${dir.name}/${f.name}"
                val ext = f.extension.lowercase()
                val isVideo = ext == "mp4"
                val item = MediaItem(
                    id = id,
                    name = meta[id]?.optString("name") ?: f.name,
                    folder = meta[id]?.optString("folder") ?: dir.name,
                    isVideo = isVideo,
                    size = f.length(),
                    modified = f.lastModified(),
                    width = 0, height = 0, durationSec = 0
                )
                if (isVideo) {
                    try {
                        val r = MediaMetadataRetriever()
                        r.setDataSource(f.absolutePath)
                        item.durationSec =
                            (r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                                ?: 0L).div(1000).toInt()
                        item.width =
                            r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                        item.height =
                            r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                        r.release()
                    } catch (_: Exception) {
                    }
                } else {
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(f.absolutePath, opts)
                    item.width = opts.outWidth
                    item.height = opts.outHeight
                }
                val m = meta[id]
                if (m != null && m.optBoolean("deleted")) {
                    item.deleted = true
                    item.deletedFrom = m.optString("deletedFrom", dir.name)
                    deletedMap.value = deletedMap.value + (id to (item.deletedFrom ?: dir.name))
                }
                if (m != null && m.optBoolean("custom")) {
                    // runtime-created file; already on disk
                }
                fileMap[id] = f
                items.add(item)
            }
        }
    }

    fun thumb(item: MediaItem): Bitmap? {
        thumbCache[item.id]?.let { return it }
        val f = fileFor(item)
        val bmp = if (item.isVideo) {
            try {
                val r = MediaMetadataRetriever()
                r.setDataSource(f.absolutePath)
                val frame = r.getFrameAtTime(0)
                r.release()
                frame
            } catch (_: Exception) {
                null
            }
        } else {
            val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
            BitmapFactory.decodeFile(f.absolutePath, opts)
        }
        if (bmp != null) thumbCache[item.id] = bmp
        return bmp
    }

    fun fullBitmap(item: MediaItem): Bitmap? {
        if (item.isVideo) return thumb(item)
        return BitmapFactory.decodeFile(fileFor(item).absolutePath)
    }

    // ---------- queries ----------

    fun activeItems(): List<MediaItem> = items.filter { !it.deleted }

    fun visibleMedia(showHidden: Boolean): List<MediaItem> {
        val f = filters.value
        return activeItems().filter { item ->
            val inHiddenFolder = hiddenFolders.value.contains(item.folder)
            val isHidden = hiddenMedia.value.contains(item.id) || inHiddenFolder
            val typeOk = when {
                item.isVideo -> f.contains("视频")
                item.extension.lowercase() == "gif" -> f.contains("GIF")
                else -> f.contains("图片")
            }
            (showHidden || !isHidden) && typeOk
        }
    }

    fun folderNames(showExcluded: Boolean, showHidden: Boolean): List<FolderInfo> {
        val grouped = activeItems()
            .filter { !hiddenMedia.value.contains(it.id) || showHidden }
            .groupBy { it.folder }
        val names = (grouped.keys + extraFolders.value).toMutableSet()
        val result = mutableListOf<FolderInfo>()
        for (name in names) {
            if (!showExcluded && excludedFolders.value.contains(name)) continue
            if (!showHidden && hiddenFolders.value.contains(name)) continue
            val media = grouped[name].orEmpty().filter { !hiddenMedia.value.contains(it.id) || showHidden }
            val cover = covers.value[name]?.let { cid -> media.firstOrNull { it.id == cid } }
                ?: media.firstOrNull()
            result.add(FolderInfo(name, media.size, cover?.id))
        }
        return result
    }

    fun favoriteItems(): List<MediaItem> = activeItems().filter { favorites.value.contains(it.id) }
    fun deletedItems(): List<MediaItem> = items.filter { it.deleted }

    fun mediaInFolder(folder: String, showHidden: Boolean): List<MediaItem> {
        val base = when (folder) {
            "收藏" -> favoriteItems()
            "回收站" -> deletedItems()
            ALL_MEDIA -> visibleMedia(showHidden)
            else -> visibleMedia(showHidden).filter { it.folder == folder }
        }
        return base
    }

    // ---------- mutations ----------

    fun toggleFavorite(id: String) {
        favorites.value = if (favorites.value.contains(id)) favorites.value - id else favorites.value + id
        saveStringSet("favorites", favorites.value)
    }

    fun togglePin(folder: String) {
        pinnedFolders.value =
            if (pinnedFolders.value.contains(folder)) pinnedFolders.value - folder else pinnedFolders.value + folder
        saveStringSet("pinned", pinnedFolders.value)
    }

    fun excludeFolder(folder: String) {
        excludedFolders.value = excludedFolders.value + folder
        saveStringSet("excluded", excludedFolders.value)
    }

    fun removeExclusion(folder: String) {
        excludedFolders.value = excludedFolders.value - folder
        saveStringSet("excluded", excludedFolders.value)
    }

    fun hideFolder(folder: String) {
        hiddenFolders.value = hiddenFolders.value + folder
        saveStringSet("hiddenFolders", hiddenFolders.value)
    }

    fun unhideFolder(folder: String) {
        hiddenFolders.value = hiddenFolders.value - folder
        saveStringSet("hiddenFolders", hiddenFolders.value)
    }

    fun hideMedia(ids: Collection<String>) {
        hiddenMedia.value = hiddenMedia.value + ids
        saveStringSet("hiddenMedia", hiddenMedia.value)
    }

    fun unhideMedia(ids: Collection<String>) {
        hiddenMedia.value = hiddenMedia.value - ids.toSet()
        saveStringSet("hiddenMedia", hiddenMedia.value)
    }

    fun setDefaultFolder(folder: String?) {
        defaultFolder.value = folder
        prefs.edit().putString("defaultFolder", folder).apply()
    }

    fun setSort(mode: String, asc: Boolean) {
        sortMode.value = mode
        sortAsc.value = asc
        prefs.edit().putString("sortMode", mode).putBoolean("sortAsc", asc).apply()
    }

    fun setGroup(mode: String, asc: Boolean) {
        groupMode.value = mode
        groupAsc.value = asc
        prefs.edit().putString("groupMode", mode).putBoolean("groupAsc", asc).apply()
    }

    fun setColumnsMain(n: Int) {
        columnsMain.value = n; prefs.edit().putInt("columnsMain", n).apply()
    }

    fun setColumnsFolder(n: Int) {
        columnsFolder.value = n; prefs.edit().putInt("columnsFolder", n).apply()
    }

    fun setFilters(f: Set<String>) {
        filters.value = f; saveStringSet("filters", f)
    }

    fun setCover(folder: String, mediaId: String?) {
        covers.value = if (mediaId == null) covers.value - folder else covers.value + (folder to mediaId)
        val obj = JSONObject()
        covers.value.forEach { (k, v) -> obj.put(k, v) }
        prefs.edit().putString("covers", obj.toString()).apply()
    }

    fun createFolder(name: String) {
        extraFolders.value = extraFolders.value + name
        saveStringSet("extraFolders", extraFolders.value)
        File(rootDir, name).mkdirs()
    }

    fun renameFolder(old: String, new: String) {
        items.forEach { if (it.folder == old) it.folder = new }
        if (extraFolders.value.contains(old)) {
            extraFolders.value = extraFolders.value - old + new
            saveStringSet("extraFolders", extraFolders.value)
        }
        if (pinnedFolders.value.contains(old)) {
            pinnedFolders.value = pinnedFolders.value - old + new
            saveStringSet("pinned", pinnedFolders.value)
        }
        if (defaultFolder.value == old) setDefaultFolder(new)
        persistMeta()
    }

    fun renameMedia(item: MediaItem, newName: String) {
        item.name = newName
        persistMeta()
        items.indexOf(item).let { if (it >= 0) items[it] = items[it] }
        version.value++
    }

    fun moveMedia(ids: Collection<String>, targetFolder: String) {
        if (!extraFolders.value.contains(targetFolder) &&
            items.none { it.folder == targetFolder }
        ) {
            createFolder(targetFolder)
        }
        items.forEach { if (ids.contains(it.id)) it.folder = targetFolder }
        persistMeta()
        refresh()
    }

    fun copyMedia(ids: Collection<String>, targetFolder: String): Int {
        var done = 0
        val sources = items.filter { ids.contains(it.id) }
        for (src in sources) {
            try {
                val dir = File(rootDir, targetFolder)
                dir.mkdirs()
                var destName = src.name
                var dest = File(dir, destName)
                var i = 1
                while (dest.exists()) {
                    destName = "${src.title}_$i.${src.extension}"
                    dest = File(dir, destName)
                    i++
                }
                fileFor(src).copyTo(dest)
                val copy = MediaItem(
                    id = "$targetFolder/$destName",
                    name = destName,
                    folder = targetFolder,
                    isVideo = src.isVideo,
                    size = dest.length(),
                    modified = System.currentTimeMillis(),
                    width = src.width, height = src.height, durationSec = src.durationSec
                )
                fileMap[copy.id] = dest
                items.add(copy)
                markCustom(copy.id)
                done++
            } catch (_: Exception) {
            }
        }
        refresh()
        return done
    }

    fun deleteMedia(ids: Collection<String>, skipRecycleBin: Boolean) {
        items.forEach { item ->
            if (ids.contains(item.id)) {
                if (skipRecycleBin || !useRecycleBin.value || item.deleted) {
                    item.deleted = false
                    item.deletedFrom = null
                    deletedMap.value = deletedMap.value - item.id
                    try {
                        fileFor(item).delete()
                    } catch (_: Exception) {
                    }
                    thumbCache.remove(item.id)
                    removeMeta(item.id)
                } else {
                    item.deleted = true
                    item.deletedFrom = item.folder
                    deletedMap.value = deletedMap.value + (item.id to item.folder)
                    persistMeta()
                }
            }
        }
        saveDeletedMap()
        items.removeAll { it.id in ids && !it.deleted && !File(rootDir, it.id).exists() && deletedMap.value[it.id] == null && it.deletedFrom == null }
        refresh()
    }

    fun restoreMedia(ids: Collection<String>) {
        items.forEach { item ->
            if (ids.contains(item.id) && item.deleted) {
                item.deleted = false
                item.folder = item.deletedFrom ?: item.folder
                item.deletedFrom = null
                deletedMap.value = deletedMap.value - item.id
            }
        }
        saveDeletedMap()
        persistMeta()
        refresh()
    }

    fun emptyRecycleBin() {
        val ids = deletedItems().map { it.id }
        deleteMedia(ids.toSet(), skipRecycleBin = true)
        items.removeAll { ids.contains(it.id) }
        refresh()
    }

    fun saveEditedCopy(source: MediaItem, bitmap: Bitmap, suffix: String) {
        try {
            val dir = File(rootDir, source.folder)
            var destName = "${source.title}$suffix.png"
            var dest = File(dir, destName)
            var i = 1
            while (dest.exists()) {
                destName = "${source.title}${suffix}_$i.png"
                dest = File(dir, destName); i++
            }
            dest.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val copy = MediaItem(
                id = "${source.folder}/$destName", name = destName, folder = source.folder,
                isVideo = false, size = dest.length(), modified = System.currentTimeMillis(),
                width = bitmap.width, height = bitmap.height, durationSec = 0
            )
            fileMap[copy.id] = dest
            items.add(copy)
            markCustom(copy.id)
            refresh()
        } catch (_: Exception) {
        }
    }

    fun rotateMedia(item: MediaItem, degrees: Float) {
        if (item.isVideo) return
        val bmp = fullBitmap(item) ?: return
        val m = android.graphics.Matrix().apply { postRotate(degrees) }
        val out = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        fileFor(item).outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
        thumbCache.remove(item.id)
        item.width = out.width
        item.height = out.height
        item.size = fileFor(item).length()
        item.modified = System.currentTimeMillis()
        persistMeta()
        refresh()
    }

    fun resizeMedia(item: MediaItem, w: Int, h: Int) {
        if (item.isVideo || w <= 0 || h <= 0) return
        val bmp = fullBitmap(item) ?: return
        val out = Bitmap.createScaledBitmap(bmp, w, h, true)
        saveEditedCopy(item, out, "_resized")
    }

    fun prefsShowHidden(v: Boolean) {
        prefs.edit().putBoolean("showHidden", v).apply()
    }

    fun prefsAskConfirm(v: Boolean) {
        prefs.edit().putBoolean("askDeleteConfirm", v).apply()
    }

    fun prefsUseRecycleBin(v: Boolean) {
        prefs.edit().putBoolean("useRecycleBin", v).apply()
    }

    fun refresh() {
        val snapshot = items.toList()
        items.clear()
        items.addAll(snapshot)
        version.value++
    }

    // ---------- persistence helpers ----------

    private fun loadStringSet(key: String): Set<String> {
        val raw = prefs.getString(key, null) ?: return emptySet()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getString(it) }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    private fun saveStringSet(key: String, set: Set<String>) {
        prefs.edit().putString(key, JSONArray(set.toList()).toString()).apply()
    }

    private fun loadDeletedMap(): Map<String, String> {
        val raw = prefs.getString("deletedMap", null) ?: return emptyMap()
        return try {
            val obj = JSONObject(raw)
            obj.keys().asSequence().map { it to obj.getString(it) }.toMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun saveDeletedMap() {
        val obj = JSONObject()
        deletedMap.value.forEach { (k, v) -> obj.put(k, v) }
        prefs.edit().putString("deletedMap", obj.toString()).apply()
    }

    private fun loadCoverMap(): Map<String, String> {
        val raw = prefs.getString("covers", null) ?: return emptyMap()
        return try {
            val obj = JSONObject(raw)
            obj.keys().asSequence().map { it to obj.getString(it) }.toMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun metaFile(): File = File(rootDir, ".meta.json").apply { parentFile?.mkdirs() }

    private fun persistMeta() {
        try {
            val obj = if (metaFile().exists()) JSONObject(metaFile().readText()) else JSONObject()
            items.forEach { item ->
                val m = obj.optJSONObject(item.id) ?: JSONObject().also { obj.put(item.id, it) }
                m.put("name", item.name)
                m.put("folder", item.folder)
                m.put("deleted", item.deleted)
                if (item.deletedFrom != null) m.put("deletedFrom", item.deletedFrom)
            }
            metaFile().writeText(obj.toString())
        } catch (_: Exception) {
        }
    }

    private fun markCustom(id: String) {
        try {
            val obj = if (metaFile().exists()) JSONObject(metaFile().readText()) else JSONObject()
            val item = items.firstOrNull { it.id == id } ?: return
            val m = JSONObject()
            m.put("name", item.name)
            m.put("folder", item.folder)
            m.put("deleted", false)
            m.put("custom", true)
            obj.put(id, m)
            metaFile().writeText(obj.toString())
        } catch (_: Exception) {
        }
    }

    private fun removeMeta(id: String) {
        try {
            if (!metaFile().exists()) return
            val obj = JSONObject(metaFile().readText())
            obj.remove(id)
            metaFile().writeText(obj.toString())
        } catch (_: Exception) {
        }
    }
}

fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f kB", kb)
    return String.format("%.1f MB", kb / 1024.0)
}

fun formatDuration(sec: Int): String = String.format("%02d:%02d", sec / 60, sec % 60)

fun formatDate(ts: Long): String {
    val sdf = java.text.SimpleDateFormat("yyyy年M月d日", java.util.Locale.CHINA)
    return sdf.format(java.util.Date(ts))
}

fun formatDateTime(ts: Long): String {
    val sdf = java.text.SimpleDateFormat("yyyy年M月d日 HH:mm", java.util.Locale.CHINA)
    return sdf.format(java.util.Date(ts))
}
