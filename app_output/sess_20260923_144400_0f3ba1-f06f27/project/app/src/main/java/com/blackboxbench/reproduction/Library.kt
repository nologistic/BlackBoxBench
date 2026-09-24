package com.blackboxbench.reproduction

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale

/** Filesystem backed media library. Everything lives under filesDir/media. */
class Library(private val context: Context) {

    val root: File = File(context.filesDir, "media")
    private val recycleDir: File = File(root, RECYCLE_ALBUM)
    private val prefs = context.getSharedPreferences("gallery", Context.MODE_PRIVATE)

    private val durationCache = HashMap<String, Long>()

    // ---------------------------------------------------------------- seeding

    fun seedIfNeeded() {
        if (prefs.getBoolean("seeded", false) && root.exists()) return
        root.mkdirs()
        recycleDir.mkdirs()
        val manifest = JSONObject(context.assets.open("media/manifest.json").bufferedReader().use { it.readText() })
        val albums = manifest.getJSONArray("albums")
        for (i in 0 until albums.length()) {
            val album = albums.getJSONObject(i)
            val name = album.getString("name")
            val dir = File(root, name).apply { mkdirs() }
            val items = album.getJSONArray("items")
            for (j in 0 until items.length()) {
                val entry = items.getJSONObject(j)
                val rel = entry.getString("file")
                val target = File(dir, File(rel).name)
                if (!target.exists()) {
                    context.assets.open("media/$rel").use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    }
                }
                target.setLastModified(entry.getLong("modified"))
            }
        }
        prefs.edit().putBoolean("seeded", true).apply()
    }

    // ------------------------------------------------------------------- scan

    /** All non recycled items found on disk. */
    fun scanItems(): List<MediaItem> {
        val out = ArrayList<MediaItem>()
        val albums = root.listFiles() ?: return out
        for (dir in albums) {
            if (!dir.isDirectory || dir.name == RECYCLE_ALBUM) continue
            val folderHidden = File(dir, ".nomedia").exists()
            for (file in dir.listFiles() ?: emptyArray()) {
                if (!file.isFile) continue
                if (file.name == ".nomedia") continue
                val item = toItem(dir.name, file)
                if (folderHidden) {
                    // items of a .nomedia folder are only listed while hidden items are shown
                    out.add(item.copy(name = "." + item.name))
                } else {
                    out.add(item)
                }
            }
        }
        return out
    }

    private fun toItem(album: String, file: File): MediaItem {
        val rel = "$album/${file.name}"
        return MediaItem(
            relPath = rel,
            name = file.name,
            sizeBytes = file.length(),
            modified = file.lastModified(),
            taken = file.lastModified(),
            isVideo = isVideo(file.name),
            favorite = favorites().contains(rel),
        )
    }

    fun itemByPath(relPath: String): MediaItem? {
        val file = File(root, relPath)
        if (!file.exists()) return null
        return toItem(relPath.substringBeforeLast('/'), file)
    }

    fun fileOf(item: MediaItem): File = File(root, item.relPath)

    private fun isVideo(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return ext in setOf("mp4", "mkv", "webm", "avi", "mov", "3gp", "m4v")
    }

    fun isGif(name: String) = name.endsWith(".gif", true)
    fun isRaw(name: String) = name.substringAfterLast('.', "").lowercase(Locale.ROOT) in setOf("raw", "dng", "cr2", "nef")
    fun isSvg(name: String) = name.endsWith(".svg", true)

    fun durationOf(item: MediaItem): Long {
        durationCache[item.relPath]?.let { return it }
        var value = 0L
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(fileOf(item).absolutePath)
            value = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            retriever.release()
        } catch (_: Exception) {
        }
        durationCache[item.relPath] = value
        return value
    }

    fun sizeOf(item: MediaItem): Pair<Int, Int> {
        if (item.isVideo) {
            return try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(fileOf(item).absolutePath)
                val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                retriever.release()
                w to h
            } catch (_: Exception) {
                0 to 0
            }
        }
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(fileOf(item).absolutePath, options)
        return options.outWidth to options.outHeight
    }

    // -------------------------------------------------------------- favorites

    fun favorites(): Set<String> = prefs.getStringSet("favorites", emptySet()) ?: emptySet()

    fun toggleFavorite(item: MediaItem): Boolean {
        val set = favorites().toMutableSet()
        val nowFavorite = if (set.contains(item.relPath)) {
            set.remove(item.relPath); false
        } else {
            set.add(item.relPath); true
        }
        prefs.edit().putStringSet("favorites", set).apply()
        return nowFavorite
    }

    fun setFavorite(item: MediaItem, value: Boolean) {
        val set = favorites().toMutableSet()
        if (value) set.add(item.relPath) else set.remove(item.relPath)
        prefs.edit().putStringSet("favorites", set).apply()
    }

    fun renameRelPath(from: String, to: String) {
        val set = favorites().toMutableSet()
        if (set.remove(from)) {
            set.add(to)
            prefs.edit().putStringSet("favorites", set).apply()
        }
    }

    // ------------------------------------------------------------- favourites album

    fun albums(showHidden: Boolean, filter: FileFilter): List<Album> {
        val items = scanItems().filter { showHidden || !it.hidden }.filter { filter.accepts(it) }
        val grouped = LinkedHashMap<String, MutableList<MediaItem>>()
        for (item in items) grouped.getOrPut(item.album) { ArrayList() }.add(item)
        val albums = grouped.map { (name, list) -> Album(name, list.sortedByDescending { it.modified }) }
        val favorites = items.filter { it.favorite }
        val result = ArrayList<Album>()
        if (favorites.isNotEmpty()) {
            result.add(Album(FAVORITES_ALBUM, favorites.sortedByDescending { it.modified }, pinned = true))
        }
        result.addAll(albums.sortedBy { it.name.lowercase(Locale.ROOT) })
        return result
    }

    fun itemsOfAlbum(album: String, showHidden: Boolean, filter: FileFilter): List<MediaItem> {
        val all = scanItems().filter { showHidden || !it.hidden }.filter { filter.accepts(it) }
        return if (album == FAVORITES_ALBUM) all.filter { it.favorite } else all.filter { it.album == album }
    }

    // -------------------------------------------------------------- recycle bin

    private fun trashIndex(): JSONObject {
        val file = File(recycleDir, "index.json")
        if (!file.exists()) return JSONObject()
        return try {
            JSONObject(file.readText())
        } catch (_: Exception) {
            JSONObject()
        }
    }

    private fun saveTrashIndex(index: JSONObject) {
        recycleDir.mkdirs()
        File(recycleDir, "index.json").writeText(index.toString())
    }

    fun trashItems(): List<MediaItem> {
        val index = trashIndex()
        val out = ArrayList<MediaItem>()
        for (file in recycleDir.listFiles() ?: emptyArray()) {
            if (!file.isFile || file.name == "index.json") continue
            val original = index.optString(file.name, "")
            val display = if (original.isNotEmpty()) original.substringAfterLast('/') else file.name
            out.add(
                MediaItem(
                    relPath = "$RECYCLE_ALBUM/${file.name}",
                    name = display,
                    sizeBytes = file.length(),
                    modified = file.lastModified(),
                    taken = file.lastModified(),
                    isVideo = isVideo(display),
                )
            )
        }
        return out.sortedByDescending { it.modified }
    }

    fun trashCount(): Int = trashItems().size

    fun trashSize(): Long = (recycleDir.listFiles() ?: emptyArray()).filter { it.isFile && it.name != "index.json" }
        .sumOf { it.length() }

    fun moveToTrash(item: MediaItem): Boolean {
        val source = fileOf(item)
        if (!source.exists()) return false
        recycleDir.mkdirs()
        val trashName = "${System.currentTimeMillis()}_${item.name}"
        val target = File(recycleDir, trashName)
        val moved = source.renameTo(target)
        if (moved) {
            val index = trashIndex()
            index.put(trashName, item.relPath)
            saveTrashIndex(index)
            if (item.favorite) {
                val set = favorites().toMutableSet()
                set.remove(item.relPath)
                prefs.edit().putStringSet("favorites", set).apply()
            }
        }
        return moved
    }

    fun deletePermanently(item: MediaItem): Boolean {
        val file = fileOf(item)
        val ok = !file.exists() || file.delete()
        if (ok && item.relPath.startsWith("$RECYCLE_ALBUM/")) {
            val index = trashIndex()
            index.remove(item.name.let { "" } + File(item.relPath).name)
            saveTrashIndex(index)
        }
        return ok
    }

    fun restore(item: MediaItem): Boolean {
        val trashName = File(item.relPath).name
        val index = trashIndex()
        val original = index.optString(trashName, "")
        if (original.isEmpty()) return false
        val source = File(recycleDir, trashName)
        val target = File(root, original)
        target.parentFile?.mkdirs()
        var destination = target
        if (target.exists()) {
            destination = File(target.parentFile, "restored_${System.currentTimeMillis()}_${target.name}")
        }
        val ok = source.renameTo(destination)
        if (ok) {
            index.remove(trashName)
            saveTrashIndex(index)
        }
        return ok
    }

    fun emptyTrash() {
        for (file in recycleDir.listFiles() ?: emptyArray()) {
            if (file.isFile && file.name != "index.json") file.delete()
        }
        saveTrashIndex(JSONObject())
    }

    // ---------------------------------------------------------------- mutators

    fun uniqueName(dir: File, base: String, ext: String): String {
        var candidate = if (ext.isEmpty()) base else "$base.$ext"
        var counter = 1
        while (File(dir, candidate).exists()) {
            candidate = if (ext.isEmpty()) "$base ($counter)" else "$base ($counter).$ext"
            counter++
        }
        return candidate
    }

    fun rename(item: MediaItem, newBase: String, newExt: String): MediaItem? {
        val file = fileOf(item)
        if (!file.exists()) return null
        val target = File(file.parentFile, if (newExt.isEmpty()) newBase else "$newBase.$newExt")
        if (target.absolutePath == file.absolutePath) return item
        val destination = if (target.exists()) File(file.parentFile, uniqueName(file.parentFile!!, newBase, newExt))
        else target
        if (!file.renameTo(destination)) return null
        val newRel = "${item.album}/${destination.name}"
        renameRelPath(item.relPath, newRel)
        return itemByPath(newRel)
    }

    fun setHidden(item: MediaItem, hidden: Boolean): MediaItem? {
        val file = fileOf(item)
        if (!file.exists()) return null
        val newName = if (hidden) {
            if (file.name.startsWith(".")) file.name else "." + file.name
        } else {
            file.name.removePrefix(".")
        }
        val target = File(file.parentFile, newName)
        if (!file.renameTo(target)) return null
        val newRel = "${item.album}/$newName"
        renameRelPath(item.relPath, newRel)
        return itemByPath(newRel)
    }

    fun copyItems(items: List<MediaItem>, targetAlbum: String): Int {
        val dir = File(root, targetAlbum).apply { mkdirs() }
        var count = 0
        for (item in items) {
            val source = fileOf(item)
            if (!source.exists()) continue
            val base = source.nameWithoutExtension
            val ext = source.extension
            val name = uniqueName(dir, base, ext)
            if (source.copyTo(File(dir, name), overwrite = false).exists()) count++
        }
        return count
    }

    fun moveItems(items: List<MediaItem>, targetAlbum: String): Int {
        val dir = File(root, targetAlbum).apply { mkdirs() }
        var count = 0
        for (item in items) {
            val source = fileOf(item)
            if (!source.exists()) continue
            val base = source.nameWithoutExtension
            val ext = source.extension
            val name = uniqueName(dir, base, ext)
            if (source.renameTo(File(dir, name))) {
                renameRelPath(item.relPath, "$targetAlbum/$name")
                count++
            }
        }
        return count
    }

    fun createFolder(parent: String, name: String): Boolean {
        val dir = if (parent.isEmpty()) File(root, name) else File(File(root, parent), name)
        if (dir.exists()) return false
        return dir.mkdirs()
    }

    fun folderTree(): List<Pair<String, Int>> {
        val out = ArrayList<Pair<String, Int>>()
        out.add("内部存储空间" to 0)
        for (dir in (root.listFiles() ?: emptyArray()).sortedBy { it.name.lowercase(Locale.ROOT) }) {
            if (!dir.isDirectory || dir.name == RECYCLE_ALBUM) continue
            val count = (dir.listFiles() ?: emptyArray()).count { it.isFile && it.name != ".nomedia" }
            out.add(dir.name to count)
        }
        return out
    }

    fun writeCopy(source: MediaItem, bitmap: Bitmap, suffix: String = "_edited"): MediaItem? {
        val file = fileOf(source)
        val dir = file.parentFile ?: return null
        val name = uniqueName(dir, file.nameWithoutExtension + suffix, "png")
        val target = File(dir, name)
        return try {
            target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            itemByPath("${source.album}/$name")
        } catch (_: Exception) {
            null
        }
    }

    fun resizeCopy(item: MediaItem, width: Int, height: Int): MediaItem? {
        val bitmap = decodeScaled(fileOf(item), width, height) ?: return null
        return writeCopy(item, bitmap, "_${width}x$height")
    }

    private fun decodeScaled(file: File, width: Int, height: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val options = BitmapFactory.Options().apply {
            inSampleSize = 1
            while (bounds.outWidth / (inSampleSize * 2) >= width && bounds.outHeight / (inSampleSize * 2) >= height) {
                inSampleSize *= 2
            }
        }
        val decoded = BitmapFactory.decodeFile(file.absolutePath, options) ?: return null
        return Bitmap.createScaledBitmap(decoded, width, height, true)
    }

    // ----------------------------------------------------------------- storage

    fun totalMediaCount(): Int = scanItems().count { !it.hidden }

    fun cacheSize(): Long = File(context.cacheDir, "thumbs").let { dir ->
        (dir.listFiles() ?: emptyArray()).sumOf { it.length() }
    }

    fun clearCache() {
        File(context.cacheDir, "thumbs").deleteRecursively()
    }

    // ---------------------------------------------------------------- settings

    fun setting(key: String, def: Boolean): Boolean = prefs.getBoolean(key, def)
    fun setSetting(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()
    fun string(key: String, def: String): String = prefs.getString(key, def) ?: def
    fun setString(key: String, value: String) = prefs.edit().putString(key, value).apply()

    fun jsonArray(key: String): JSONArray {
        val raw = prefs.getString(key, null) ?: return JSONArray()
        return try {
            JSONArray(raw)
        } catch (_: Exception) {
            JSONArray()
        }
    }

    fun setJsonArray(key: String, array: JSONArray) = prefs.edit().putString(key, array.toString()).apply()
}
