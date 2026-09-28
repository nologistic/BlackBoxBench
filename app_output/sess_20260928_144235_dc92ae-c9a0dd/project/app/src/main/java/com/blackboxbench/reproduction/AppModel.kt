package com.blackboxbench.reproduction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class SortBy { NAME, TYPE, SIZE, MTIME }

enum class Screen { FILES, SETTINGS, ABOUT, FTP }

enum class ClipMode { COPY, CUT }

class Clip(val mode: ClipMode, val nodes: List<FsNode>)

class AppModel {

    val root: FsNode = SampleTree.build()
    val path = mutableStateListOf<FsNode>(root)
    val selection = mutableStateListOf<FsNode>()
    val bookmarks = mutableStateListOf<Bookmark>().apply { addAll(SampleTree.bookmarks(root)) }

    var screen by mutableStateOf(Screen.FILES)
    var drawerOpen by mutableStateOf(false)
    var query by mutableStateOf("")
    var searchActive by mutableStateOf(false)
    var sortBy by mutableStateOf(SortBy.NAME)
    var ascending by mutableStateOf(true)
    var foldersFirst by mutableStateOf(true)
    var grid by mutableStateOf(false)
    var showHidden by mutableStateOf(false)
    var clip by mutableStateOf<Clip?>(null)
    var onlyThisFolder by mutableStateOf(false)
    var ftpRunning by mutableStateOf(false)
    var dynamicColor by mutableStateOf(true)
    var nightMode by mutableStateOf("跟随系统")
    var blackNight by mutableStateOf(false)
    var listAnimation by mutableStateOf(true)
    var longNameMode by mutableStateOf("省略中间")
    var defaultFolder by mutableStateOf("内部共享存储空间")
    var rootAccessMode by mutableStateOf("自动")
    var archiveEncoding by mutableStateOf("UTF-8")
    var apkOpenMode by mutableStateOf("询问操作")
    var remoteThumbnails by mutableStateOf(true)
    var language by mutableStateOf("系统默认")
    var errorText by mutableStateOf<String?>(null)
    var breadcrumbExtra by mutableStateOf<String?>(null)

    val current: FsNode get() = path.last()

    fun clearError() {
        errorText = null
        breadcrumbExtra = null
    }

    /** Navigates to a virtual path such as "Pictures/Screenshots"; reports a missing segment as an error. */
    fun openRelative(relative: String) {
        path.clear()
        path.add(root)
        selection.clear()
        errorText = null
        breadcrumbExtra = null
        var node: FsNode = root
        for (segment in relative.split('/')) {
            val child = node.child(segment)
            if (child == null) {
                breadcrumbExtra = segment
                errorText =
                    "java.nio.file.NoSuchFileException: /storage/emulated/0/$relative: opendir: No such file or directory"
                return
            }
            path.add(child)
            node = child
        }
    }

    fun showRootError() {
        path.clear()
        path.add(root)
        selection.clear()
        breadcrumbExtra = "根目录"
        errorText = "RemoteFileSystemException: Root isn't available"
    }

    fun items(): List<FsNode> {
        val source = current.children.orEmpty()
        var list = source.filter { showHidden || !it.hidden }
        if (searchActive) {
            list = list.filter { !it.isDir && it.name.contains(query, ignoreCase = true) }
        }
        if (list.size > 1) {
            val byName = compareBy<FsNode> { it.name.lowercase() }
            val byType = compareBy<FsNode> { kindRank(it) }.then(byName)
            val bySize = compareBy<FsNode> { it.size }.then(byName)
            val byTime = compareBy<FsNode> { it.mtime }.then(byName)
            val base = when (sortBy) {
                SortBy.NAME -> byName
                SortBy.TYPE -> byType
                SortBy.SIZE -> bySize
                SortBy.MTIME -> byTime
            }
            val ordered = if (ascending) base else base.reversed()
            list = if (foldersFirst && !searchActive) {
                val dirs = list.filter { it.canEnter }.sortedWith(ordered)
                val files = list.filter { !it.canEnter }.sortedWith(ordered)
                dirs + files
            } else {
                list.sortedWith(ordered)
            }
        }
        return list
    }

    private fun kindRank(node: FsNode): Int = when (node.kind) {
        FileKind.DIR -> 0
        FileKind.ARCHIVE -> 1
        FileKind.IMAGE -> 2
        FileKind.VIDEO -> 3
        FileKind.AUDIO -> 4
        FileKind.DOCUMENT -> 5
        FileKind.APK -> 6
        FileKind.OTHER -> 7
    }

    fun folderCount(): Int = current.children.orEmpty().count { it.isDir && (showHidden || !it.hidden) }
    fun fileCount(): Int = current.children.orEmpty().count { !it.isDir && (showHidden || !it.hidden) }

    fun subtitle(): String {
        if (current.kind == FileKind.ARCHIVE) return "压缩文件"
        val d = folderCount()
        val f = fileCount()
        return when {
            d > 0 && f > 0 -> "$d 个文件夹，$f 个文件"
            d > 0 -> "$d 个文件夹"
            f > 0 -> "$f 个文件"
            else -> "空文件夹"
        }
    }

    // ---------------------------------------------------------------- navigation

    fun enter(node: FsNode) {
        if (!node.canEnter) return
        path.add(node)
        selection.clear()
        searchActive = false
        query = ""
        clearError()
    }

    fun goTo(target: FsNode) {
        if (target === root) {
            path.clear(); path.add(root); selection.clear(); clearError(); return
        }
        val chain = target.pathFrom(root)
        if (chain.isNotEmpty()) {
            path.clear(); path.add(root); path.addAll(chain)
        }
        selection.clear()
        clearError()
    }

    fun goUp(): Boolean {
        if (path.size > 1) {
            path.removeAt(path.size - 1)
            selection.clear()
            clearError()
            return true
        }
        if (errorText != null) {
            clearError()
            return true
        }
        return false
    }

    fun breadcrumb(): List<FsNode> = path.toList()

    // ---------------------------------------------------------------- selection

    fun toggleSelection(node: FsNode) {
        if (selection.contains(node)) selection.remove(node) else selection.add(node)
    }

    fun clearSelection() = selection.clear()

    fun selectAll() {
        selection.clear()
        selection.addAll(items())
    }

    // ---------------------------------------------------------------- write ops

    private fun uniqueName(parent: FsNode, wanted: String): String {
        if (parent.child(wanted) == null) return wanted
        val dot = wanted.lastIndexOf('.')
        val stem = if (dot > 0) wanted.substring(0, dot) else wanted
        val ext = if (dot > 0) wanted.substring(dot) else ""
        var i = 1
        while (parent.child("$stem ($i)$ext") != null) i++
        return "$stem ($i)$ext"
    }

    fun createFolder(name: String) {
        val parent = current
        if (parent.children == null) return
        val trimmed = name.trim()
        if (trimmed.isBlank() || parent.child(trimmed) != null) return
        parent.children.add(FsNode(trimmed, FileKind.DIR, 0L, System.currentTimeMillis(), mutableStateListOf()))
    }

    fun createFile(name: String) {
        val parent = current
        if (parent.children == null) return
        val trimmed = name.trim()
        if (trimmed.isBlank() || parent.child(trimmed) != null) return
        parent.children.add(FsNode(trimmed, FileKind.OTHER, 0L, System.currentTimeMillis(), null))
    }

    fun rename(node: FsNode, newName: String): Boolean {
        val trimmed = newName.trim()
        if (trimmed.isBlank() || trimmed == node.name) return false
        val parent = findParent(node) ?: return false
        if (parent.child(trimmed) != null) return false
        node.name = trimmed
        node.mtime = System.currentTimeMillis()
        return true
    }

    fun findParent(node: FsNode): FsNode? {
        fun walk(n: FsNode): FsNode? {
            for (c in n.children.orEmpty()) {
                if (c === node) return n
                walk(c)?.let { return it }
            }
            return null
        }
        return walk(root)
    }

    fun delete(nodes: List<FsNode>) {
        for (node in nodes) {
            val parent = findParent(node) ?: continue
            parent.children?.remove(node)
        }
        selection.clear()
    }

    fun compress(nodes: List<FsNode>, rawName: String) {
        val parent = current
        if (parent.children == null) return
        var name = rawName.trim().ifBlank { "archive" }
        if (!name.endsWith(".zip", ignoreCase = true)) name += ".zip"
        name = uniqueName(parent, name)
        val payload = nodes.map { shallowCopy(it) }.toMutableList()
        val total = nodes.sumOf { it.size }
        parent.children.add(FsNode(name, FileKind.ARCHIVE, total, System.currentTimeMillis(), payload))
        selection.clear()
    }

    private fun shallowCopy(node: FsNode): FsNode =
        FsNode(
            node.name, node.kind, node.size, node.mtime,
            node.children?.let { kids -> mutableStateListOf<FsNode>().apply { kids.forEach { add(shallowCopy(it)) } } },
        )

    fun extractAll(archiveNode: FsNode) {
        val parent = current
        if (parent.children == null) return
        val base = archiveNode.name.substringBeforeLast('.', archiveNode.name)
        val target = FsNode(uniqueName(parent, base), FileKind.DIR, 0L, System.currentTimeMillis(), mutableStateListOf())
        target.children!!.addAll(archiveNode.children.orEmpty().map { shallowCopy(it) })
        parent.children.add(target)
        selection.clear()
    }

    fun extractFiles(nodes: List<FsNode>) {
        val parent = current
        if (parent.children == null) return
        for (node in nodes) {
            val copy = shallowCopy(node)
            copy.name = uniqueName(parent, node.name)
            parent.children.add(copy)
        }
        selection.clear()
    }

    fun copySelection(mode: ClipMode) {
        if (selection.isEmpty()) return
        clip = Clip(mode, selection.toList())
        selection.clear()
    }

    fun paste() {
        val active = clip ?: return
        val parent = current
        if (parent.children == null) return
        for (node in active.nodes) {
            val copy = shallowCopy(node)
            copy.name = uniqueName(parent, node.name)
            parent.children.add(copy)
        }
        if (active.mode == ClipMode.CUT) {
            for (node in active.nodes) {
                findParent(node)?.children?.remove(node)
            }
        }
        clip = null
    }

    fun clearClip() {
        clip = null
    }

    fun addBookmark(node: FsNode) {
        if (bookmarks.any { it.target === node }) return
        bookmarks.add(Bookmark(node.name, node))
    }

    fun removeBookmark(bookmark: Bookmark) {
        bookmarks.remove(bookmark)
    }

    fun bookmarkFor(node: FsNode): Bookmark? = bookmarks.firstOrNull { it.target === node }
}
