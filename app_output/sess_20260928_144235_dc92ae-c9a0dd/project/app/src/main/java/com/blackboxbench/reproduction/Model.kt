package com.blackboxbench.reproduction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Calendar

enum class FileKind { DIR, IMAGE, AUDIO, VIDEO, DOCUMENT, ARCHIVE, APK, OTHER }

class FsNode(
    name: String,
    val kind: FileKind,
    size: Long = 0L,
    mtime: Long = 0L,
    val children: MutableList<FsNode>? = null,
) {
    var name by mutableStateOf(name)
    var size by mutableStateOf(size)
    var mtime by mutableStateOf(mtime)

    val isDir: Boolean get() = kind == FileKind.DIR
    val isArchive: Boolean get() = kind == FileKind.ARCHIVE
    val canEnter: Boolean get() = isDir || isArchive
    val hidden: Boolean get() = name.startsWith(".")

    fun child(name: String): FsNode? = children?.firstOrNull { it.name == name }

    /** Path of this node below [root], excluding the root itself. */
    fun pathFrom(root: FsNode): List<FsNode> {
        val chain = ArrayList<FsNode>()
        fun walk(n: FsNode): Boolean {
            if (n === this) return true
            for (c in n.children.orEmpty()) {
                if (walk(c)) { chain.add(0, c); return true }
            }
            return false
        }
        if (this === root) return emptyList()
        return if (walk(root)) chain else emptyList()
    }
}

fun dir(name: String, vararg kids: FsNode): FsNode =
    FsNode(name, FileKind.DIR, 0L, 0L, mutableStateListOf<FsNode>().apply { addAll(kids) })

fun archive(name: String, size: Long, mtime: Long, vararg kids: FsNode): FsNode =
    FsNode(name, FileKind.ARCHIVE, size, mtime, mutableStateListOf<FsNode>().apply { addAll(kids) })

fun file(name: String, size: Long, mtime: Long, kind: FileKind = FileKind.OTHER): FsNode =
    FsNode(name, kind, size, mtime)

private fun ts(y: Int, mo: Int, d: Int, h: Int, mi: Int): Long {
    val c = Calendar.getInstance()
    c.set(y, mo - 1, d, h, mi, 0)
    c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

/** Synthetic, entirely fictional directory tree used by the reproduction. */
object SampleTree {

    const val STORAGE_TOTAL = 10_410_000_000L
    const val STORAGE_FREE = 9_660_000_000L
    const val ROOT_TOTAL = 980_000_000L
    const val ROOT_FREE = 67_180_000L

    fun build(): FsNode {
        val documents = dir(
            "Documents",
            file("年度总结.md", 12_480L, ts(2026, 8, 30, 9, 12), FileKind.DOCUMENT),
            file("黑盒实验记录.txt", 3_841L, ts(2026, 9, 5, 21, 47), FileKind.DOCUMENT),
            file("素材清单.pdf", 262_144L, ts(2026, 7, 18, 14, 3), FileKind.DOCUMENT),
            archive(
                "会议录音.zip", 15_810_000L, ts(2026, 9, 1, 10, 20),
                file("周一例会.wav", 7_800_000L, ts(2026, 9, 1, 10, 0), FileKind.AUDIO),
                file("评审会.wav", 7_500_000L, ts(2026, 8, 28, 15, 30), FileKind.AUDIO),
                file("readme.txt", 1_024L, ts(2026, 9, 1, 10, 20), FileKind.DOCUMENT),
            ),
            dir(".draft", file("未定稿.md", 2_400L, ts(2026, 8, 28, 23, 55), FileKind.DOCUMENT)),
        )
        val pictures = dir(
            "Pictures",
            file("截图 2026-09-06.png", 482_300L, ts(2026, 9, 6, 18, 30), FileKind.IMAGE),
            file("湖边.png", 512_000L, ts(2026, 8, 12, 16, 8), FileKind.IMAGE),
            file(".nomedia", 0L, ts(2026, 8, 12, 16, 8), FileKind.OTHER),
        )
        val download = dir(
            "Download",
            file("feeder.apk", 53_000_000L, ts(2026, 8, 20, 11, 11), FileKind.APK),
            archive(
                "素材包.zip", 12_400_000L, ts(2026, 9, 4, 8, 44),
                dir("avatars"),
                file("manifest.json", 7_168L, ts(2026, 9, 4, 8, 44), FileKind.DOCUMENT),
            ),
            dir("临时"),
        )
        val trash = dir(" .trash".trim(), file("旧方案.docx", 45_056L, ts(2026, 6, 1, 10, 0), FileKind.DOCUMENT))
        return dir(
            "内部共享存储空间",
            dir("Alarms"),
            dir("Android"),
            dir("Audiobooks"),
            dir("DCIM"),
            documents,
            download,
            dir("Movies"),
            dir("Music"),
            dir("Notifications"),
            pictures,
            dir("Podcasts"),
            dir("Recordings"),
            dir("Ringtones"),
            dir("SeedFiles"),
            trash,
        )
    }

    fun bookmarks(root: FsNode): List<Bookmark> = listOf(
        Bookmark("文档", root.child("Documents")),
        Bookmark("下载", root.child("Download")),
    )
}

class Bookmark(val label: String, val target: FsNode?)

fun formatSize(bytes: Long): String {
    if (bytes < 1000L) return "$bytes B"
    val units = arrayOf("B", "kB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1000.0 && unit < units.size - 1) {
        value /= 1000.0
        unit++
    }
    return String.format("%.2f %s", value, units[unit])
}

fun formatDate(millis: Long): String {
    if (millis <= 0L) return ""
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = millis }
    val sameDay = now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
    return if (sameDay) {
        String.format("%02d:%02d", then.get(Calendar.HOUR_OF_DAY), then.get(Calendar.MINUTE))
    } else {
        "${then.get(Calendar.MONTH) + 1}月${then.get(Calendar.DAY_OF_MONTH)}日"
    }
}

fun formatFullDate(millis: Long): String {
    if (millis <= 0L) return ""
    val c = Calendar.getInstance().apply { timeInMillis = millis }
    return String.format(
        "%d年%d月%d日 %02d:%02d:%02d",
        c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH),
        c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND)
    )
}
