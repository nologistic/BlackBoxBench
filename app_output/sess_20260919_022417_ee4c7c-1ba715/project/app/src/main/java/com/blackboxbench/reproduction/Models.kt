package com.blackboxbench.reproduction

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

enum class Priority(val level: Int) {
    NONE(0), LOW(1), MEDIUM(2), HIGH(3);
    companion object { fun of(l: Int) = entries.firstOrNull { it.level == l } ?: NONE }
}

data class Task(
    val id: String = UUID.randomUUID().toString(),
    var listId: String = "lst_default",
    var title: String = "",
    var notes: String = "",
    var startDate: String = "",       // yyyy-MM-dd
    var dueDate: String = "",         // yyyy-MM-dd
    var dueTime: String = "",         // HH:mm, empty = 无时间
    var repeat: String = "",          // FREQ=DAILY / FREQ=WEEKLY;BYDAY=MO,TH / FREQ=MONTHLY;BYMONTHDAY=15
    var priority: Priority = Priority.LOW,
    var tagIds: MutableList<String> = mutableListOf(),
    var parentId: String = "",
    var completedAt: String = "",     // ISO date-time
    var createdAt: String = "",
    var modifiedAt: String = "",
    var locationId: String = "",
    var locationReminder: Boolean = false,
    var addToCalendar: Boolean = false,
    var elapsedSeconds: Long = 0L
) {
    val isCompleted get() = completedAt.isNotEmpty()
    val isRepeating get() = repeat.isNotEmpty()
}

data class TaskList(val id: String = UUID.randomUUID().toString(), var name: String, var color: Int = -1, var icon: String = "")

data class Tag(val id: String = UUID.randomUUID().toString(), var name: String, var color: Int = -1, var icon: String = "")

data class Place(val id: String = UUID.randomUUID().toString(), var name: String, var lat: Double = 0.0, var lng: Double = 0.0)

data class FilterCondition(val type: String, val param: String = "")

data class Filter(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var color: Int = -1,
    var icon: String = "",
    var conditions: MutableList<FilterCondition> = mutableListOf(),
    var builtin: String = ""          // "today" | "recent" | ""
)

sealed class ViewRef {
    object MyTasks : ViewRef()
    data class BuiltinFilter(val kind: String) : ViewRef()   // today / recent
    data class CustomFilter(val id: String) : ViewRef()
    data class TagView(val id: String) : ViewRef()
    data class PlaceView(val id: String) : ViewRef()
    data class ListView(val id: String) : ViewRef()
}

object Dates {
    private val dateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val dtFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")

    fun today(): LocalDate = LocalDate.now()
    fun nowStamp(): String = LocalDateTime.now().format(dtFmt)
    fun parseDate(s: String): LocalDate? = if (s.isEmpty()) null else runCatching { LocalDate.parse(s, dateFmt) }.getOrNull()
    fun fmtDate(d: LocalDate): String = d.format(dateFmt)
    fun fmtTime(h: Int, m: Int): String = String.format("%02d:%02d", h, m)
    fun isOverdue(t: Task): Boolean {
        val d = parseDate(t.dueDate) ?: return false
        return d.isBefore(today()) && !t.isCompleted
    }
    fun weekdayCn(dow: DayOfWeek): String = when (dow) {
        DayOfWeek.MONDAY -> "一"; DayOfWeek.TUESDAY -> "二"; DayOfWeek.WEDNESDAY -> "三"
        DayOfWeek.THURSDAY -> "四"; DayOfWeek.FRIDAY -> "五"; DayOfWeek.SATURDAY -> "六"
        DayOfWeek.SUNDAY -> "日"
    }
    fun weekdayCn(d: LocalDate): String = weekdayCn(d.dayOfWeek)

    /** 列表分组名 */
    fun groupName(t: Task): String {
        val d = parseDate(t.dueDate) ?: return "无截止日期"
        val today = today()
        return when {
            d.isBefore(today) -> "已过期"
            d.isEqual(today) -> "今天截止"
            d.isEqual(today.plusDays(1)) -> "明天截止"
            else -> "${d.monthValue}月${d.dayOfMonth}日星期${weekdayCn(d)}截止"
        }
    }
    fun groupOrder(t: Task): Long {
        val d = parseDate(t.dueDate) ?: return Long.MAX_VALUE
        return d.toEpochDay()
    }
    fun relativeLabel(t: Task): String {
        val d = parseDate(t.dueDate) ?: return ""
        val today = today()
        return when {
            d.isBefore(today) -> "已过期"
            d.isEqual(today) -> "今天"
            d.isEqual(today.plusDays(1)) -> "明天"
            else -> "${d.monthValue}月${d.dayOfMonth}日"
        }
    }

    private val dowLetters = mapOf(
        DayOfWeek.MONDAY to "MO", DayOfWeek.TUESDAY to "TU", DayOfWeek.WEDNESDAY to "WE",
        DayOfWeek.THURSDAY to "TH", DayOfWeek.FRIDAY to "FR", DayOfWeek.SATURDAY to "SA",
        DayOfWeek.SUNDAY to "SU"
    )

    fun repeatLabel(rule: String): String {
        if (rule.isEmpty()) return "不重复"
        val parts = rule.split(";").associate { it.substringBefore("=") to it.substringAfter("=", "") }
        return when (parts["FREQ"]) {
            "DAILY" -> "每天"
            "WEEKLY" -> {
                val days = parts["BYDAY"]?.split(",")?.mapNotNull { code ->
                    dowLetters.entries.firstOrNull { it.value == code }?.key
                } ?: emptyList()
                if (days.isEmpty()) "每周"
                else "每周 " + days.joinToString("、") { "周" + weekdayCn(it) }
            }
            "MONTHLY" -> "每月 ${parts["BYMONTHDAY"] ?: ""} 日"
            "YEARLY" -> "每年"
            else -> "自定义"
        }
    }

    /** 完成重复任务后下一次出现的日期 */
    fun nextOccurrence(rule: String, currentDue: String, completionDate: LocalDate = today()): String {
        val due = parseDate(currentDue) ?: completionDate
        val parts = rule.split(";").associate { it.substringBefore("=") to it.substringAfter("=", "") }
        val next: LocalDate = when (parts["FREQ"]) {
            "DAILY" -> {
                var n = due.plusDays(1)
                if (!n.isAfter(completionDate)) n = completionDate.plusDays(1)
                n
            }
            "WEEKLY" -> {
                val codes = parts["BYDAY"]?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
                val days = codes.mapNotNull { c -> dowLetters.entries.firstOrNull { it.value == c }?.key }
                var base = due
                if (base.isBefore(completionDate)) base = completionDate
                if (days.isEmpty()) {
                    base.plusDays(7)
                } else {
                    var n = base
                    do { n = n.plusDays(1) } while (!days.contains(n.dayOfWeek))
                    n
                }
            }
            "MONTHLY" -> {
                val day = parts["BYMONTHDAY"]?.toIntOrNull() ?: due.dayOfMonth
                var n = due.plusMonths(1).withDayOfMonth(minOf(day, due.plusMonths(1).lengthOfMonth()))
                if (!n.isAfter(completionDate)) {
                    val m = completionDate.plusMonths(1)
                    n = m.withDayOfMonth(minOf(day, m.lengthOfMonth()))
                }
                n
            }
            "YEARLY" -> due.plusYears(1)
            else -> due.plusDays(1)
        }
        return fmtDate(next)
    }
}
