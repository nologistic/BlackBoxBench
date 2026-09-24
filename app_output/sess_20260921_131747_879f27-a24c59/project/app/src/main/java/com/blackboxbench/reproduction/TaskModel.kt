package com.blackboxbench.reproduction

import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/** 任务清单：工作 / 生活 / 购物 … */
data class TaskList(val id: String, val name: String) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
    }

    companion object {
        fun fromJson(o: JSONObject) = TaskList(o.optString("id"), o.optString("name"))
    }
}

/** 优先级：0 无 / 1 低 / 2 中 / 3 高 */
const val PRIO_NONE = 0
const val PRIO_LOW = 1
const val PRIO_MED = 2
const val PRIO_HIGH = 3

fun priorityLabel(p: Int): String = when (p) {
    PRIO_HIGH -> "高"
    PRIO_MED -> "中"
    PRIO_LOW -> "低"
    else -> "无"
}

fun parsePriority(value: Any?): Int = when (value) {
    is Int -> value
    is Number -> value.toInt()
    is String -> when (value.trim().lowercase()) {
        "high", "3" -> PRIO_HIGH
        "medium", "2" -> PRIO_MED
        "low", "1" -> PRIO_LOW
        else -> PRIO_NONE
    }
    else -> PRIO_NONE
}

/** 两级嵌套的子任务 */
class Subtask(
    val id: String,
    var title: String = "",
    var due: String = "",
    var priority: Int = PRIO_NONE,
    var completedAt: String = ""
) {
    val done: Boolean get() = completedAt.isNotBlank()

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("due", due)
        put("priority", priority)
        put("completed_at", completedAt)
    }

    companion object {
        fun fromJson(o: JSONObject) = Subtask(
            id = o.optString("id"),
            title = o.optString("title"),
            due = o.optString("due"),
            priority = parsePriority(o.opt("priority")),
            completedAt = o.optString("completed_at")
        )
    }
}

/**
 * 任务条目。due 形如 "2026-09-08T18:00"（含时间）或 "2026-09-07"（仅日期），空串表示无日期。
 * repeat 为 RFC5545 风格的简化规则，空串表示不重复。
 */
class Task(
    val id: String,
    var listId: String,
    var title: String = "",
    var notes: String = "",
    var due: String = "",
    var priority: Int = PRIO_NONE,
    var tags: MutableList<String> = mutableListOf(),
    var repeat: String = "",
    var completedAt: String = "",
    /** 重复任务上一次完成的时间戳（同一任务条目顺延到下一次出现） */
    var lastCompletedAt: String = "",
    var subtasks: MutableList<Subtask> = mutableListOf(),
    var order: Int = 0
) {
    val done: Boolean get() = completedAt.isNotBlank()

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("list", listId)
        put("title", title)
        put("notes", notes)
        put("due", due)
        put("priority", priority)
        put("tags", JSONArray(tags))
        put("repeat", repeat)
        put("completed_at", completedAt)
        put("last_completed_at", lastCompletedAt)
        put("order", order)
        put("subtasks", JSONArray().apply { subtasks.forEach { put(it.toJson()) } })
    }

    companion object {
        fun fromJson(o: JSONObject): Task {
            val subs = mutableListOf<Subtask>()
            o.optJSONArray("subtasks")?.let { arr ->
                for (i in 0 until arr.length()) subs.add(Subtask.fromJson(arr.getJSONObject(i)))
            }
            val tagList = mutableListOf<String>()
            o.optJSONArray("tags")?.let { arr ->
                for (i in 0 until arr.length()) tagList.add(arr.getString(i))
            }
            return Task(
                id = o.optString("id"),
                listId = o.optString("list", o.optString("listId")),
                title = o.optString("title"),
                notes = o.optString("notes"),
                due = o.optString("due"),
                priority = parsePriority(o.opt("priority")),
                tags = tagList,
                repeat = o.optString("repeat"),
                completedAt = o.optString("completed_at"),
                lastCompletedAt = o.optString("last_completed_at"),
                subtasks = subs,
                order = o.optInt("order", 0)
            )
        }
    }
}

// ---------------------------------------------------------------- 日期与重复

val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
val DATETIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
val STAMP_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

fun parseDue(due: String): LocalDateTime? {
    if (due.isBlank()) return null
    return try {
        if (due.contains('T')) LocalDateTime.parse(due, DATETIME_FMT)
        else LocalDate.parse(due, DATE_FMT).atStartOfDay()
    } catch (e: Exception) {
        null
    }
}

fun dueHasTime(due: String): Boolean = due.contains('T')

fun formatDue(due: String): String {
    val dt = parseDue(due) ?: return ""
    return if (dueHasTime(due)) dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    else dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
}

fun formatDueShort(due: String, today: LocalDate): String {
    val dt = parseDue(due) ?: return ""
    val d = dt.toLocalDate()
    val head = when (d) {
        today -> "今天"
        today.plusDays(1) -> "明天"
        today.minusDays(1) -> "昨天"
        else -> d.format(DateTimeFormatter.ofPattern("M月d日"))
    }
    return if (dueHasTime(due)) "$head ${dt.format(DateTimeFormatter.ofPattern("HH:mm"))}" else head
}

fun isOverdue(due: String, today: LocalDate): Boolean {
    val dt = parseDue(due) ?: return false
    return dt.isBefore(today.atStartOfDay())
}

fun stampOf(dt: LocalDateTime): String = dt.format(STAMP_FMT)

/** 把种子数据里的 ISO 完成时间（2026-09-08T11:05）统一格式化为 yyyy-MM-dd HH:mm 展示 */
fun formatStamp(raw: String): String {
    if (raw.isBlank()) return ""
    return try {
        when {
            raw.contains('T') -> LocalDateTime.parse(raw, DATETIME_FMT).format(STAMP_FMT)
            raw.length > 10 -> LocalDateTime.parse(raw, STAMP_FMT).format(STAMP_FMT)
            else -> LocalDate.parse(raw, DATE_FMT).format(STAMP_FMT)
        }
    } catch (e: Exception) {
        raw
    }
}

fun repeatLabel(rule: String): String {
    if (rule.isBlank()) return "不重复"
    val freq = when {
        rule.contains("FREQ=DAILY") -> "每天"
        rule.contains("FREQ=WEEKLY") -> "每周"
        rule.contains("FREQ=MONTHLY") -> "每月"
        else -> "重复"
    }
    val byDay = Regex("BYDAY=([A-Z,]+)").find(rule)?.groupValues?.get(1)
    val byMonthDay = Regex("BYMONTHDAY=(\\d+)").find(rule)?.groupValues?.get(1)
    return when {
        byDay != null -> freq + "（" + byDay.split(",").joinToString("、") { weekLabel(it) } + "）"
        byMonthDay != null -> "$freq ${byMonthDay} 日"
        else -> freq
    }
}

private fun weekLabel(code: String): String = when (code.trim().uppercase()) {
    "MO" -> "周一"
    "TU" -> "周二"
    "WE" -> "周三"
    "TH" -> "周四"
    "FR" -> "周五"
    "SA" -> "周六"
    "SU" -> "周日"
    else -> code
}

private fun dayOfWeek(code: String): DayOfWeek? = when (code.trim().uppercase()) {
    "MO" -> DayOfWeek.MONDAY
    "TU" -> DayOfWeek.TUESDAY
    "WE" -> DayOfWeek.WEDNESDAY
    "TH" -> DayOfWeek.THURSDAY
    "FR" -> DayOfWeek.FRIDAY
    "SA" -> DayOfWeek.SATURDAY
    "SU" -> DayOfWeek.SUNDAY
    else -> null
}

/**
 * 计算下一次出现。逾期完成时从完成日顺延，不补漏掉的实例。
 */
fun nextOccurrence(due: String, rule: String, completedOn: LocalDate): String {
    val dt = parseDue(due) ?: return due
    if (rule.isBlank()) return due
    var anchor = dt.toLocalDate()
    if (anchor.isBefore(completedOn)) anchor = completedOn
    val next: LocalDate = when {
        rule.contains("FREQ=DAILY") -> anchor.plusDays(1)
        rule.contains("FREQ=WEEKLY") -> {
            val days = Regex("BYDAY=([A-Z,]+)").find(rule)?.groupValues?.get(1)
                ?.split(",")?.mapNotNull { dayOfWeek(it) }?.toSet().orEmpty()
            if (days.isEmpty()) anchor.plusWeeks(1)
            else {
                var d = anchor.plusDays(1)
                while (d.dayOfWeek !in days) d = d.plusDays(1)
                d
            }
        }
        rule.contains("FREQ=MONTHLY") -> {
            val day = Regex("BYMONTHDAY=(\\d+)").find(rule)?.groupValues?.get(1)?.toIntOrNull()
                ?: anchor.dayOfMonth
            var y = anchor.year
            var m = anchor.monthValue + 1
            if (m > 12) {
                m = 1
                y += 1
            }
            LocalDate.of(y, m, minOf(day, YearMonth.of(y, m).lengthOfMonth()))
        }
        else -> anchor.plusDays(1)
    }
    return if (dueHasTime(due)) LocalDateTime.of(next, dt.toLocalTime()).format(DATETIME_FMT)
    else next.format(DATE_FMT)
}

/** 表单里的快捷日期选项 */
enum class QuickDate(val label: String) {
    TODAY("今天"),
    TOMORROW("明天"),
    NEXT_SUNDAY("下周日"),
    NONE("无日期")
}

fun quickDue(quick: QuickDate, today: LocalDate): String = when (quick) {
    QuickDate.TODAY -> today.format(DATE_FMT)
    QuickDate.TOMORROW -> today.plusDays(1).format(DATE_FMT)
    QuickDate.NEXT_SUNDAY -> {
        var d = today.plusDays(1)
        while (d.dayOfWeek != DayOfWeek.SUNDAY) d = d.plusDays(1)
        d.format(DATE_FMT)
    }
    QuickDate.NONE -> ""
}
