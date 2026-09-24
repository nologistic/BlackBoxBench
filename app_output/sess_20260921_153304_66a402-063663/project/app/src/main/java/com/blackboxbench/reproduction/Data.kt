package com.blackboxbench.reproduction

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

enum class MediaKind { IMAGE, VIDEO, GIF, RAW, SVG }

/** One media entry of the private library. */
data class MediaItem(
    val id: String,
    var albumPath: String,
    var fileName: String,
    val kind: MediaKind,
    var sizeBytes: Long,
    var modifiedMs: Long,
    var takenMs: Long,
    var favorite: Boolean = false,
    var hidden: Boolean = false,
    var deleted: Boolean = false,
    var deletedMs: Long = 0L,
    var width: Int = 0,
    var height: Int = 0,
    var originalAlbum: String = ""
) {
    val isVideo: Boolean get() = kind == MediaKind.VIDEO
    val displayName: String get() = fileName
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("album", albumPath); put("name", fileName); put("kind", kind.name)
        put("size", sizeBytes); put("modified", modifiedMs); put("taken", takenMs)
        put("favorite", favorite); put("hidden", hidden); put("deleted", deleted)
        put("deletedMs", deletedMs); put("width", width); put("height", height)
        put("originalAlbum", originalAlbum)
    }

    companion object {
        fun fromJson(o: JSONObject) = MediaItem(
            id = o.getString("id"),
            albumPath = o.getString("album"),
            fileName = o.getString("name"),
            kind = runCatching { MediaKind.valueOf(o.getString("kind")) }.getOrDefault(MediaKind.IMAGE),
            sizeBytes = o.optLong("size"),
            modifiedMs = o.optLong("modified"),
            takenMs = o.optLong("taken"),
            favorite = o.optBoolean("favorite"),
            hidden = o.optBoolean("hidden"),
            deleted = o.optBoolean("deleted"),
            deletedMs = o.optLong("deletedMs"),
            width = o.optInt("width"),
            height = o.optInt("height"),
            originalAlbum = o.optString("originalAlbum")
        )
    }
}

/** A folder that is shown on the main screen. */
data class Album(
    val path: String,
    val name: String,
    var coverId: String? = null,
    var empty: Boolean = false
) {
    fun toJson() = JSONObject().apply {
        put("path", path); put("name", name); put("cover", coverId ?: ""); put("empty", empty)
    }

    companion object {
        fun fromJson(o: JSONObject) = Album(
            o.getString("path"), o.getString("name"),
            o.optString("cover").ifEmpty { null }, o.optBoolean("empty")
        )
    }
}

/**
 * Private media library: the sandboxed stand-in for the device media store.
 * Media is copied out of the APK assets on first launch and afterwards treated
 * as the on-device content of the gallery.
 */
class MediaRepository(private val context: Context) {

    val root: File = File(context.filesDir, "gallery")
    private val trashDir: File get() = File(root, ".trash")
    private val dbFile: File get() = File(context.filesDir, "library.json")

    val items = mutableListOf<MediaItem>()
    val albums = mutableListOf<Album>()
    var trashDisabled: Boolean = false

    fun fileOf(item: MediaItem): File =
        if (item.deleted) File(trashDir, item.id + "_" + item.fileName)
        else File(File(root, item.albumPath), item.fileName)

    fun load() {
        if (!dbFile.exists()) return
        runCatching {
            val json = JSONObject(dbFile.readText())
            items.clear()
            val arr = json.optJSONArray("items") ?: JSONArray()
            for (i in 0 until arr.length()) items.add(MediaItem.fromJson(arr.getJSONObject(i)))
            albums.clear()
            val al = json.optJSONArray("albums") ?: JSONArray()
            for (i in 0 until al.length()) albums.add(Album.fromJson(al.getJSONObject(i)))
            trashDisabled = json.optBoolean("trashDisabled", false)
        }
    }

    fun save() {
        val json = JSONObject().apply {
            put("items", JSONArray().apply { items.forEach { put(it.toJson()) } })
            put("albums", JSONArray().apply { albums.forEach { put(it.toJson()) } })
            put("trashDisabled", trashDisabled)
        }
        dbFile.writeText(json.toString())
    }

    /** Copies the bundled public-domain media into the private library once. */
    fun seedIfNeeded() {
        if (dbFile.exists()) { load(); return }
        root.mkdirs()
        val seed = runCatching { AssetStore.readArray(context, "media_seed.json") }.getOrNull()
        if (seed != null) {
            for (i in 0 until seed.length()) {
                val entry = seed.getJSONObject(i)
                val assetPath = entry.getString("asset")
                val album = entry.getString("album")
                val name = entry.getString("name")
                val dir = File(root, album)
                dir.mkdirs()
                val out = File(dir, name)
                runCatching {
                    context.assets.open(assetPath).use { input ->
                        out.outputStream().use { output -> input.copyTo(output) }
                    }
                }
                if (!out.exists()) continue
                val kind = kindOf(name)
                val size = out.length()
                val (w, h) = dimensionsOf(out, kind)
                val now = System.currentTimeMillis()
                val modified = entry.optLong("modified", now)
                items.add(
                    MediaItem(
                        id = UUID.randomUUID().toString(),
                        albumPath = album,
                        fileName = name,
                        kind = kind,
                        sizeBytes = size,
                        modifiedMs = modified,
                        takenMs = modified,
                        width = w, height = h
                    )
                )
            }
        }
        for (path in listOf("DCIM/Camera", "Pictures/Covers", "Pictures/Avatars", "Download", "Movies")) {
            if (albums.none { it.path == path }) albums.add(Album(path, path.substringAfterLast('/')))
        }
        save()
    }

    fun kindOf(name: String): MediaKind {
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "mp4", "mkv", "webm", "3gp", "mov" -> MediaKind.VIDEO
            "gif" -> MediaKind.GIF
            "svg" -> MediaKind.SVG
            "dng", "raw", "cr2", "nef", "arw" -> MediaKind.RAW
            else -> MediaKind.IMAGE
        }
    }

    private fun dimensionsOf(file: File, kind: MediaKind): Pair<Int, Int> {
        if (kind == MediaKind.VIDEO) {
            val mmr = MediaMetadataRetriever()
            return runCatching {
                mmr.setDataSource(file.absolutePath)
                val w = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                val h = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                w to h
            }.getOrDefault(0 to 0).also { runCatching { mmr.release() } }
        }
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, opts)
        return opts.outWidth to opts.outHeight
    }

    // ---------------------------------------------------------------- queries

    fun visibleItems(showHidden: Boolean): List<MediaItem> =
        items.filter { !it.deleted && (showHidden || !it.hidden) }

    fun trashedItems(): List<MediaItem> = items.filter { it.deleted }

    fun albumItems(albumPath: String, showHidden: Boolean): List<MediaItem> =
        visibleItems(showHidden).filter { it.albumPath == albumPath }

    fun favorites(): List<MediaItem> = visibleItems(true).filter { it.favorite }

    /** Albums that currently have visible media, plus manually created empty albums. */
    fun visibleAlbums(showHidden: Boolean): List<Album> {
        val result = mutableListOf<Album>()
        val byPath = LinkedHashMap<String, MutableList<MediaItem>>()
        for (item in visibleItems(showHidden)) {
            byPath.getOrPut(item.albumPath) { mutableListOf() }.add(item)
        }
        for ((path, list) in byPath) {
            val album = albums.firstOrNull { it.path == path } ?: Album(path, path.substringAfterLast('/'))
            result.add(album)
        }
        for (album in albums) {
            if (album.empty && byPath.containsKey(album.path).not()) result.add(album)
        }
        return result
    }

    fun countOf(album: Album, showHidden: Boolean): List<MediaItem> = albumItems(album.path, showHidden)

    fun coverOf(album: Album, showHidden: Boolean): MediaItem? {
        album.coverId?.let { id -> items.firstOrNull { it.id == id && !it.deleted }?.let { return it } }
        return albumItems(album.path, showHidden).firstOrNull()
    }

    fun findById(id: String?): MediaItem? = items.firstOrNull { it.id == id }

    fun albumName(path: String): String = path.substringAfterLast('/')

    // ------------------------------------------------------------- mutations

    fun toggleFavorite(item: MediaItem) { item.favorite = !item.favorite; save() }

    fun rename(item: MediaItem, newName: String): Boolean {
        val file = fileOf(item)
        val target = File(file.parentFile, newName)
        if (target.exists()) return false
        if (file.exists() && !file.renameTo(target)) return false
        item.fileName = newName
        save(); return true
    }

    fun setHidden(item: MediaItem, hide: Boolean) {
        val file = fileOf(item)
        val plain = item.fileName.removePrefix(".")
        val newName = if (hide) ".$plain" else plain
        val target = File(file.parentFile, newName)
        if (file.exists()) file.renameTo(target)
        item.fileName = newName
        item.hidden = hide
        save()
    }

    fun delete(item: MediaItem, skipTrash: Boolean) {
        val file = fileOf(item)
        if (skipTrash || trashDisabled) {
            file.delete()
            items.remove(item)
        } else {
            trashDir.mkdirs()
            val target = File(trashDir, item.id + "_" + item.fileName)
            file.renameTo(target)
            item.originalAlbum = item.albumPath
            item.deleted = true
            item.deletedMs = System.currentTimeMillis()
        }
        save()
    }

    fun deleteMany(list: List<MediaItem>, skipTrash: Boolean) { list.toList().forEach { delete(it, skipTrash) } }

    fun restore(item: MediaItem) {
        val file = fileOf(item)
        val dir = File(root, item.albumPath); dir.mkdirs()
        val target = File(dir, item.fileName)
        file.renameTo(target)
        item.deleted = false
        item.deletedMs = 0
        save()
    }

    fun deletePermanently(item: MediaItem) {
        fileOf(item).delete()
        items.remove(item)
        save()
    }

    fun emptyTrash() {
        trashedItems().toList().forEach { deletePermanently(it) }
    }

    fun restoreAll() { trashedItems().toList().forEach { restore(it) } }

    fun copyTo(item: MediaItem, albumPath: String): Boolean {
        val dir = File(root, albumPath); dir.mkdirs()
        var name = item.fileName
        var target = File(dir, name)
        var index = 1
        while (target.exists()) {
            val base = item.fileName.substringBeforeLast('.')
            val ext = item.fileName.substringAfterLast('.', "")
            name = if (ext.isEmpty()) "$base ($index)" else "$base ($index).$ext"
            target = File(dir, name)
            index++
        }
        return runCatching {
            fileOf(item).copyTo(target, overwrite = false)
            items.add(
                item.copy(
                    id = UUID.randomUUID().toString(), albumPath = albumPath, fileName = name,
                    favorite = false, hidden = false, deleted = false
                )
            )
            save(); true
        }.getOrDefault(false)
    }

    fun moveTo(item: MediaItem, albumPath: String): Boolean {
        val dir = File(root, albumPath); dir.mkdirs()
        val target = File(dir, item.fileName)
        return runCatching {
            fileOf(item).renameTo(target)
            item.albumPath = albumPath
            save(); true
        }.getOrDefault(false)
    }

    fun moveMany(list: List<MediaItem>, albumPath: String) { list.toList().forEach { moveTo(it, albumPath) } }
    fun copyMany(list: List<MediaItem>, albumPath: String) { list.toList().forEach { copyTo(it, albumPath) } }

    fun createAlbum(albumPath: String) {
        if (albums.none { it.path == albumPath }) albums.add(Album(albumPath, albumPath.substringAfterLast('/'), empty = true))
        save()
    }

    fun setCover(album: Album, item: MediaItem?) { album.coverId = item?.id; save() }

    /** Rotates the actual bitmap by 90 degree steps and rewrites the file. */
    fun rotate(item: MediaItem, degrees: Int): Boolean {
        if (item.isVideo) return false
        val file = fileOf(item)
        return runCatching {
            val src = BitmapFactory.decodeFile(file.absolutePath) ?: return false
            val matrix = android.graphics.Matrix().apply { postRotate(degrees.toFloat()) }
            val out = android.graphics.Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
            file.outputStream().use { out.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, it) }
            if (item.fileName.endsWith(".png", true)) {
                val jpg = item.fileName.substringBeforeLast('.') + ".jpg"
                val renamed = File(file.parentFile, jpg)
                file.renameTo(renamed)
                item.fileName = jpg
            }
            item.width = out.width; item.height = out.height
            item.sizeBytes = fileOf(item).length()
            item.modifiedMs = System.currentTimeMillis()
            save(); true
        }.getOrDefault(false)
    }

    /** Saves a rotated/scaled copy next to the original ("save as"). */
    fun saveCopy(item: MediaItem, albumPath: String, fileName: String, bitmap: android.graphics.Bitmap): Boolean {
        val dir = File(root, albumPath); dir.mkdirs()
        val target = File(dir, fileName)
        return runCatching {
            val ext = fileName.substringAfterLast('.', "jpg").lowercase()
            val format = if (ext == "png") android.graphics.Bitmap.CompressFormat.PNG else android.graphics.Bitmap.CompressFormat.JPEG
            target.outputStream().use { bitmap.compress(format, 95, it) }
            items.add(
                MediaItem(
                    id = UUID.randomUUID().toString(), albumPath = albumPath, fileName = fileName,
                    kind = kindOf(fileName), sizeBytes = target.length(),
                    modifiedMs = System.currentTimeMillis(), takenMs = System.currentTimeMillis(),
                    width = bitmap.width, height = bitmap.height
                )
            )
            save(); true
        }.getOrDefault(false)
    }

    fun resize(item: MediaItem, width: Int, height: Int, albumPath: String, fileName: String): Boolean {
        if (item.isVideo) return false
        val src = BitmapFactory.decodeFile(fileOf(item).absolutePath) ?: return false
        val scaled = android.graphics.Bitmap.createScaledBitmap(src, width.coerceAtLeast(1), height.coerceAtLeast(1), true)
        return saveCopy(item, albumPath, fileName, scaled)
    }

    fun removeExif(item: MediaItem): Boolean {
        if (item.isVideo) return false
        return runCatching {
            val src = BitmapFactory.decodeFile(fileOf(item).absolutePath) ?: return false
            val file = fileOf(item)
            file.outputStream().use { src.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, it) }
            item.sizeBytes = file.length()
            save(); true
        }.getOrDefault(false)
    }

    fun totalTrashSize(): Long = trashedItems().sumOf { it.sizeBytes }

    fun cacheSize(): Long = root.walkTopDown().filter { it.isFile }.sumOf { it.length() }
}
