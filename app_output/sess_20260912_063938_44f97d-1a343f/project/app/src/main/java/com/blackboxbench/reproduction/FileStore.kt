package com.blackboxbench.reproduction

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class FsNode(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    val type: String,
    var size: Long = 0,
    var mtime: String = "2026-09-12 07:00",
    val hidden: Boolean = false,
    val children: MutableList<FsNode> = mutableListOf()
) {
    fun deepCopy(newName: String = name): FsNode = FsNode(
        name = newName,
        type = type,
        size = size,
        mtime = mtime,
        hidden = hidden,
        children = children.map { it.deepCopy() }.toMutableList()
    )
}

data class Bookmark(val name: String, val path: String)

class FileStore(context: Context) {
    private val preferences = context.getSharedPreferences("material_files_state", Context.MODE_PRIVATE)

    var revision by mutableIntStateOf(0)
        private set

    val root: FsNode = loadTree()
    val bookmarks = mutableStateListOf<Bookmark>().apply { addAll(loadBookmarks()) }

    var gridView by mutableStateOf(preferences.getBoolean("grid", false))
        private set
    var showHidden by mutableStateOf(preferences.getBoolean("hidden", false))
        private set
    var ascending by mutableStateOf(preferences.getBoolean("ascending", true))
        private set
    var foldersFirst by mutableStateOf(preferences.getBoolean("folders_first", true))
        private set
    var sortMode by mutableStateOf(preferences.getString("sort_mode", "名称") ?: "名称")
        private set

    fun setGrid(value: Boolean) {
        gridView = value
        preferences.edit().putBoolean("grid", value).apply()
    }

    fun setHidden(value: Boolean) {
        showHidden = value
        preferences.edit().putBoolean("hidden", value).apply()
    }

    fun updateAscending(value: Boolean) {
        ascending = value
        preferences.edit().putBoolean("ascending", value).apply()
    }

    fun updateFoldersFirst(value: Boolean) {
        foldersFirst = value
        preferences.edit().putBoolean("folders_first", value).apply()
    }

    fun updateSortMode(value: String) {
        sortMode = value
        preferences.edit().putString("sort_mode", value).apply()
    }

    fun resolve(path: List<String>): FsNode {
        var node = root
        path.forEach { segment ->
            node = node.children.firstOrNull { it.name == segment } ?: node
        }
        return node
    }

    fun visibleChildren(path: List<String>, query: String = ""): List<FsNode> {
        revision
        val list = resolve(path).children
            .filter { showHidden || !it.hidden }
            .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
        val comparator = when (sortMode) {
            "类型" -> compareBy<FsNode> { it.type }.thenBy { it.name.lowercase() }
            "大小" -> compareBy<FsNode> { it.size }
            "修改日期" -> compareBy<FsNode> { it.mtime }
            "随机" -> compareBy<FsNode> { (it.id.hashCode() + revision) % 997 }
            else -> compareBy<FsNode> { it.name.lowercase() }
        }
        val sorted = if (ascending) list.sortedWith(comparator) else list.sortedWith(comparator.reversed())
        return if (foldersFirst) sorted.sortedBy { if (it.type == "dir") 0 else 1 } else sorted
    }

    fun add(path: List<String>, name: String, type: String = "file") {
        if (name.isBlank()) return
        val parent = resolve(path)
        var finalName = name.trim()
        var index = 1
        while (parent.children.any { it.name == finalName }) {
            val dot = name.lastIndexOf('.')
            finalName = if (dot > 0) "${name.substring(0, dot)} ($index)${name.substring(dot)}" else "$name ($index)"
            index++
        }
        parent.children.add(FsNode(name = finalName, type = type))
        changed()
    }

    fun rename(path: List<String>, nodeId: String, newName: String) {
        if (newName.isBlank()) return
        resolve(path).children.firstOrNull { it.id == nodeId }?.name = newName.trim()
        changed()
    }

    fun delete(path: List<String>, ids: Set<String>) {
        resolve(path).children.removeAll { it.id in ids }
        changed()
    }

    fun compress(path: List<String>, ids: Set<String>, archiveName: String) {
        val parent = resolve(path)
        val chosen = parent.children.filter { it.id in ids }
        if (chosen.isEmpty()) return
        val name = archiveName.trim().ifBlank { chosen.first().name + ".zip" }.let {
            if (it.endsWith(".zip") || it.endsWith(".7z") || it.endsWith(".tar.xz")) it else "$it.zip"
        }
        parent.children.add(FsNode(name = name, type = "archive", size = chosen.sumOf { it.size }, children = chosen.map { it.deepCopy() }.toMutableList()))
        changed()
    }

    fun extract(path: List<String>, archive: FsNode, allIntoFolder: Boolean = false) {
        val parent = resolve(path)
        if (allIntoFolder) {
            val folderName = archive.name.substringBeforeLast('.')
            parent.children.add(FsNode(name = uniqueName(parent, folderName), type = "dir", children = archive.children.map { it.deepCopy() }.toMutableList()))
        } else {
            archive.children.forEach { child ->
                parent.children.add(child.deepCopy(uniqueName(parent, child.name)))
            }
        }
        changed()
    }

    fun copyInto(path: List<String>, ids: Set<String>, sourcePath: List<String>) {
        val source = resolve(sourcePath)
        val target = resolve(path)
        source.children.filter { it.id in ids }.forEach { node ->
            target.children.add(node.deepCopy(uniqueName(target, node.name)))
        }
        changed()
    }

    fun addBookmark(path: List<String>) {
        val bookmarkPath = displayPath(path)
        if (bookmarks.none { it.path == bookmarkPath }) {
            val name = path.lastOrNull() ?: "内部存储"
            bookmarks.add(Bookmark(name, bookmarkPath))
            saveBookmarks()
        }
    }

    fun removeBookmark(bookmark: Bookmark) {
        bookmarks.remove(bookmark)
        saveBookmarks()
    }

    fun renameBookmark(bookmark: Bookmark, newName: String) {
        val index = bookmarks.indexOf(bookmark)
        if (index >= 0 && newName.isNotBlank()) {
            bookmarks[index] = bookmark.copy(name = newName.trim())
            saveBookmarks()
        }
    }

    fun pathFromDisplay(path: String): List<String> =
        path.removePrefix("/内部存储").trim('/').takeIf { it.isNotBlank() }?.split('/') ?: emptyList()

    fun displayPath(path: List<String>): String =
        if (path.isEmpty()) "/内部存储" else "/内部存储/" + path.joinToString("/")

    private fun uniqueName(parent: FsNode, desired: String): String {
        if (parent.children.none { it.name == desired }) return desired
        var index = 1
        val dot = desired.lastIndexOf('.')
        while (true) {
            val candidate = if (dot > 0) "${desired.substring(0, dot)} ($index)${desired.substring(dot)}" else "$desired ($index)"
            if (parent.children.none { it.name == candidate }) return candidate
            index++
        }
    }

    private fun changed() {
        revision++
        preferences.edit().putString("tree", nodeToJson(root).toString()).apply()
    }

    private fun saveBookmarks() {
        val array = JSONArray()
        bookmarks.forEach { array.put(JSONObject().put("name", it.name).put("path", it.path)) }
        preferences.edit().putString("bookmarks", array.toString()).apply()
    }

    private fun loadBookmarks(): List<Bookmark> {
        val raw = preferences.getString("bookmarks", null) ?: return listOf(
            Bookmark("文档", "/内部存储/Documents"),
            Bookmark("下载", "/内部存储/Download")
        )
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                Bookmark(item.getString("name"), item.getString("path"))
            }
        }.getOrDefault(emptyList())
    }

    private fun loadTree(): FsNode {
        val stored = preferences.getString("tree", null)
        if (stored != null) runCatching { return nodeFromJson(JSONObject(stored)) }
        return defaultTree()
    }

    private fun defaultTree(): FsNode = FsNode(name = "内部共享存储空间", type = "dir", children = mutableListOf(
        FsNode(name = "Alarms", type = "dir"),
        FsNode(name = "Android", type = "dir"),
        FsNode(name = "Audiobooks", type = "dir"),
        FsNode(name = "DCIM", type = "dir"),
        FsNode(name = "Documents", type = "dir", children = mutableListOf(
            FsNode(name = "年度总结.md", type = "file", size = 12480, mtime = "2026-08-30 09:12"),
            FsNode(name = "黑盒实验记录.txt", type = "file", size = 3841, mtime = "2026-09-05 21:47"),
            FsNode(name = "素材清单.pdf", type = "file", size = 262144, mtime = "2026-07-18 14:03"),
            FsNode(name = "会议录音.zip", type = "archive", size = 15810000, mtime = "2026-09-01 10:20", children = mutableListOf(
                FsNode(name = "周一例会.wav", type = "file", size = 7800000),
                FsNode(name = "评审会.wav", type = "file", size = 7500000),
                FsNode(name = "readme.txt", type = "file", size = 1024)
            )),
            FsNode(name = ".draft", type = "dir", hidden = true, children = mutableListOf(
                FsNode(name = "未定稿.md", type = "file", size = 2400, mtime = "2026-08-28 23:55")
            ))
        )),
        FsNode(name = "Download", type = "dir", children = mutableListOf(
            FsNode(name = "feeder.apk", type = "file", size = 53000000, mtime = "2026-08-20 11:11"),
            FsNode(name = "素材包.zip", type = "archive", size = 12400000, mtime = "2026-09-04 08:44", children = mutableListOf(
                FsNode(name = "avatars", type = "dir"),
                FsNode(name = "manifest.json", type = "file", size = 7168)
            )),
            FsNode(name = "临时", type = "dir")
        )),
        FsNode(name = "Movies", type = "dir"),
        FsNode(name = "Music", type = "dir"),
        FsNode(name = "Notifications", type = "dir"),
        FsNode(name = "Pictures", type = "dir", children = mutableListOf(
            FsNode(name = "截图 2026-09-06.png", type = "file", size = 482300, mtime = "2026-09-06 18:30"),
            FsNode(name = "湖边.png", type = "file", size = 512000, mtime = "2026-08-12 16:08"),
            FsNode(name = ".nomedia", type = "file", hidden = true, size = 0, mtime = "2026-08-12 16:08")
        )),
        FsNode(name = "Podcasts", type = "dir"),
        FsNode(name = "Ringtones", type = "dir"),
        FsNode(name = "Screenshots", type = "dir"),
        FsNode(name = ".trash", type = "dir", hidden = true, children = mutableListOf(
            FsNode(name = "旧方案.docx", type = "file", size = 45056, mtime = "2026-06-01 10:00")
        ))
    ))

    private fun nodeToJson(node: FsNode): JSONObject = JSONObject()
        .put("id", node.id)
        .put("name", node.name)
        .put("type", node.type)
        .put("size", node.size)
        .put("mtime", node.mtime)
        .put("hidden", node.hidden)
        .put("children", JSONArray().also { array -> node.children.forEach { array.put(nodeToJson(it)) } })

    private fun nodeFromJson(json: JSONObject): FsNode {
        val children = mutableListOf<FsNode>()
        val array = json.optJSONArray("children") ?: JSONArray()
        for (index in 0 until array.length()) children.add(nodeFromJson(array.getJSONObject(index)))
        return FsNode(
            id = json.optString("id", UUID.randomUUID().toString()),
            name = json.getString("name"),
            type = json.getString("type"),
            size = json.optLong("size"),
            mtime = json.optString("mtime", "2026-09-12 07:00"),
            hidden = json.optBoolean("hidden"),
            children = children
        )
    }
}
