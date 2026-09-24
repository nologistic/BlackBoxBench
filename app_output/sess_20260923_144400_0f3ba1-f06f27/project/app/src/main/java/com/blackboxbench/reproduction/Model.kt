package com.blackboxbench.reproduction

import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class SortMode(val label: String) {
    NAME("名称"),
    PATH("路径"),
    SIZE("大小"),
    COUNT("项数"),
    MODIFIED("修改日期"),
    TAKEN("拍摄日期"),
    RANDOM("随机"),
    CUSTOM("自定义");

    companion object {
        val albumModes = listOf(NAME, PATH, SIZE, COUNT, MODIFIED, TAKEN, RANDOM, CUSTOM)
        val itemModes = listOf(NAME, PATH, SIZE, MODIFIED, TAKEN, RANDOM)
    }
}

enum class GroupMode(val label: String) {
    NONE("不分组文件"),
    MODIFIED_DAY("最后修改时间（按天）"),
    MODIFIED_MONTH("最后修改时间（按月）"),
    TAKEN_DAY("拍摄日期（按天）"),
    TAKEN_MONTH("拍摄日期（按月）"),
    TYPE("文件类型"),
    EXTENSION("扩展名")
}

enum class ViewMode(val label: String) { GRID("网格"), LIST("列表") }

/** Which file kinds are listed. Mirrors the "过滤显示的文件" dialog. */
data class FileFilter(
    val images: Boolean = true,
    val videos: Boolean = true,
    val gif: Boolean = true,
    val raw: Boolean = true,
    val svg: Boolean = true,
    val portrait: Boolean = false,
) {
    fun accepts(item: MediaItem): Boolean {
        val ext = item.extension
        val kindOk = when {
            item.isVideo -> videos
            ext == "gif" -> gif
            ext == "svg" -> svg
            ext == "raw" || ext == "dng" || ext == "cr2" || ext == "nef" -> raw
            else -> images
        }
        return kindOk
    }
}

data class MediaItem(
    val relPath: String,
    val name: String,
    var sizeBytes: Long,
    var modified: Long,
    var taken: Long,
    val isVideo: Boolean,
    var favorite: Boolean = false,
    var durationMs: Long = 0L,
    var width: Int = 0,
    var height: Int = 0,
) {
    val extension: String
        get() = name.substringAfterLast('.', "").lowercase(Locale.ROOT)

    val hidden: Boolean
        get() = name.startsWith(".")

    /** Album the item lives in, "." prefixed folders are internal. */
    val album: String
        get() = relPath.substringBeforeLast('/', "")

    val displayName: String
        get() = if (hidden) name.substring(1) else name
}

data class Album(
    val name: String,
    val items: List<MediaItem>,
    val pinned: Boolean = false,
) {
    val cover: MediaItem?
        get() = items.filter { !it.isVideo }.maxByOrNull { it.modified }
            ?: items.maxByOrNull { it.modified }
            ?: items.firstOrNull()
    val count: Int get() = items.size
}

const val RECYCLE_ALBUM = ".recycle"
const val FAVORITES_ALBUM = "收藏"

object Dates {
    private val months = arrayOf(
        "一月", "二月", "三月", "四月", "五月", "六月",
        "七月", "八月", "九月", "十月", "十一月", "十二月"
    )

    /** e.g. "20 九月 2026" */
    fun dayLabel(millis: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        return "${c.get(Calendar.DAY_OF_MONTH)} ${months[c.get(Calendar.MONTH)]} ${c.get(Calendar.YEAR)}"
    }

    /** e.g. "九月 2026" */
    fun monthLabel(millis: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        return "${months[c.get(Calendar.MONTH)]} ${c.get(Calendar.YEAR)}"
    }

    /** e.g. "20 九月 2026, 12:17" */
    fun stamp(millis: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        return dayLabel(millis) + String.format(
            Locale.ROOT, ", %02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)
        )
    }

    fun dayKey(millis: Long): Long {
        val c = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return c.timeInMillis
    }

    fun monthKey(millis: Long): Long {
        val c = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return c.timeInMillis
    }
}

fun formatDuration(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.ROOT, "%02d:%02d", m, s)
}

fun formatSize(bytes: Long): String = when {
    bytes >= 1024L * 1024 * 1024 -> String.format(Locale.ROOT, "%.1f GB", bytes / 1024.0 / 1024 / 1024)
    bytes >= 1024L * 1024 -> String.format(Locale.ROOT, "%.1f MB", bytes / 1024.0 / 1024)
    bytes >= 1024 -> String.format(Locale.ROOT, "%.1f kB", bytes / 1024.0)
    else -> "$bytes B"
}

fun Date.atMillis(millis: Long): Date = Date(millis)
