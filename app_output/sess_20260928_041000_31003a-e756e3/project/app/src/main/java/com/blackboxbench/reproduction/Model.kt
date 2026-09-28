package com.blackboxbench.reproduction

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Priority, matching the four circles of the observed editor (无/低/中/高). */
enum class Priority(val key: String, val label: String) {
    NONE("none", "无"),
    LOW("low", "低"),
    MEDIUM("medium", "中"),
    HIGH("high", "高");

    companion object {
        fun from(key: String?): Priority = entries.firstOrNull { it.key == key } ?: NONE
    }
}

data class TaskList(
    val id: String,
    val name: String,
    val color: Long? = null
)

data class Task(
    val id: String,
    val title: String = "",
    val notes: String = "",
    val listId: String? = null,
    val start: LocalDateTime? = null,
    val due: LocalDateTime? = null,
    val priority: Priority = Priority.NONE,
    val tags: List<String> = emptyList(),
    val repeatRule: String = "",
    val completedAt: LocalDateTime? = null,
    val subtasks: List<Task> = emptyList(),
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val modifiedAt: LocalDateTime = LocalDateTime.now()
) {
    val isCompleted: Boolean get() = completedAt != null
    val hasTime: Boolean get() = due != null && (due.hour != 0 || due.minute != 0)
}

/** Grouping modes offered by the sort sheet. */
enum class Grouping(val label: String) {
    NONE("无"),
    DUE("按截止日期"),
    START("按开始日期"),
    PRIORITY("按优先级"),
    MODIFIED("按最后修改"),
    CREATED("创建时间"),
    LIST("清单")
}

enum class SortMode(val label: String) {
    DUE("按截止日期"),
    PRIORITY("按优先级"),
    TITLE("按标题"),
    MANUAL("手动")
}

object DateFmt {
    private val day = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val stamp: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun date(d: LocalDate?): String = d?.format(day) ?: ""

    /** e.g. 9月18日星期五 */
    fun longDate(d: LocalDate): String {
        val w = when (d.dayOfWeek.value) {
            1 -> "一"; 2 -> "二"; 3 -> "三"; 4 -> "四"; 5 -> "五"; 6 -> "六"; else -> "日"
        }
        return "${d.monthValue}月${d.dayOfMonth}日星期$w"
    }

    /** e.g. 9月18日星期五 11:00 */
    fun longDateTime(dt: LocalDateTime): String {
        val t = "%02d:%02d".format(dt.hour, dt.minute)
        return if (dt.hour == 0 && dt.minute == 0) longDate(dt.toLocalDate())
        else "${longDate(dt.toLocalDate())} $t"
    }

    /** Chip label, e.g. 9月18日 / 18:00 */
    fun chip(dt: LocalDateTime): String =
        if (dt.hour == 0 && dt.minute == 0) "${dt.monthValue}月${dt.dayOfMonth}日"
        else "%02d:%02d".format(dt.hour, dt.minute)

    fun time(dt: LocalDateTime): String = "%02d:%02d".format(dt.hour, dt.minute)
}

object RepeatRules {
    val options = listOf(
        "" to "不重复",
        "FREQ=DAILY" to "每天",
        "FREQ=WEEKLY" to "每周",
        "FREQ=MONTHLY" to "每月",
        "FREQ=YEARLY" to "每年"
    )

    fun label(rule: String): String = when {
        rule.isEmpty() -> "不重复"
        rule == "FREQ=DAILY" -> "每天"
        rule == "FREQ=WEEKLY" -> "每周"
        rule == "FREQ=WEEKLY;BYDAY=MO,TH" -> "每周一和周四"
        rule == "FREQ=WEEKLY;BYDAY=MO" -> "每周一"
        rule == "FREQ=WEEKLY;BYDAY=WE" -> "每周三"
        rule == "FREQ=WEEKLY;BYDAY=SA" -> "每周六"
        rule == "FREQ=WEEKLY;BYDAY=SU" -> "每周日"
        rule == "FREQ=MONTHLY" -> "每月"
        rule == "FREQ=MONTHLY;BYMONTHDAY=15" -> "每月15日"
        rule == "FREQ=MONTHLY;BYMONTHDAY=30" -> "每月30日"
        rule == "FREQ=YEARLY" -> "每年"
        else -> rule
    }

    /** Next occurrence for the supported rules, anchored on the current due date. */
    fun next(rule: String, from: LocalDateTime): LocalDateTime? = when {
        rule == "FREQ=DAILY" -> from.plusDays(1)
        rule == "FREQ=WEEKLY" -> from.plusWeeks(1)
        rule.startsWith("FREQ=WEEKLY;BYDAY=") -> {
            val days = rule.substringAfter("BYDAY=").split(",").mapNotNull { code ->
                when (code.trim()) {
                    "MO" -> 1; "TU" -> 2; "WE" -> 3; "TH" -> 4; "FR" -> 5; "SA" -> 6; "SU" -> 7
                    else -> null
                }
            }.sorted()
            var cursor = from.plusDays(1)
            var guard = 0
            while (guard++ < 14 && days.isNotEmpty() && cursor.dayOfWeek.value !in days) {
                cursor = cursor.plusDays(1)
            }
            cursor
        }
        rule.startsWith("FREQ=MONTHLY;BYMONTHDAY=") -> {
            val dom = rule.substringAfter("BYMONTHDAY=").toIntOrNull() ?: from.dayOfMonth
            var cursor = from.plusMonths(1)
            val maxDay = cursor.toLocalDate().lengthOfMonth()
            cursor.withDayOfMonth(dom.coerceAtMost(maxDay))
        }
        rule == "FREQ=MONTHLY" -> from.plusMonths(1)
        rule == "FREQ=YEARLY" -> from.plusYears(1)
        else -> null
    }
}
