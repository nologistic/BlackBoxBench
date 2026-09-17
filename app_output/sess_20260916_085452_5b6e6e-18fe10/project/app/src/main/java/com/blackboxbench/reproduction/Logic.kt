package com.blackboxbench.reproduction

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.abs

object Logic {
    val WEEKDAY_CN = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    val MONTH_CN = listOf("一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月")

    fun today(): LocalDate = LocalDate.now()

    fun weekdayCn(d: LocalDate): String = WEEKDAY_CN[d.dayOfWeek.value - 1]

    fun key(d: LocalDate): String = d.toString()

    fun dayDone(h: Habit, d: LocalDate): Boolean {
        val v = h.checkins[key(d)] ?: 0
        if (v <= 0) return false
        return when (h.type) {
            HabitType.BOOLEAN -> true
            HabitType.QUANTIFIED -> if (h.targetAtLeast) v >= h.target else v <= h.target
        }
    }

    fun hasCheckin(h: Habit, d: LocalDate): Boolean = (h.checkins[key(d)] ?: 0) > 0

    fun freqSummary(h: Habit): String = when (h.freqKind) {
        FreqKind.DAILY -> "每天"
        FreqKind.EVERY_N_DAYS -> "每 ${h.freqN} 天"
        FreqKind.WEEKLY_COUNT -> "每周 ${h.freqN} 次"
        FreqKind.MONTHLY_COUNT -> "每月 ${h.freqN} 次"
        FreqKind.EVERY_N_DAYS_TIMES -> "每 ${h.freqN} 天 ${h.freqM} 次"
        FreqKind.WEEKLY_DAYS -> h.reminderDays.sorted().joinToString("") { WEEKDAY_CN[it].removePrefix("周") }
            .let { if (it.isEmpty()) "每周" else "每周$it" }
    }

    fun score(h: Habit, today: LocalDate): Double {
        if (h.checkins.isEmpty()) return 0.0
        val start = h.checkins.keys.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.minOrNull()
            ?: return 0.0
        var s = 0.0
        val lambda = 0.05
        var d = start
        while (!d.isAfter(today)) {
            val done = dayDone(h, d)
            s = (1 - lambda) * s + lambda * (if (done) 1.0 else 0.0)
            d = d.plusDays(1)
        }
        return s
    }

    fun currentStreak(h: Habit, today: LocalDate): Int {
        if (h.freqKind == FreqKind.WEEKLY_COUNT || h.freqKind == FreqKind.MONTHLY_COUNT) {
            return weeklyStreak(h, today)
        }
        var count = 0
        var d = today
        if (!dayDone(h, d)) d = d.minusDays(1)
        while (dayDone(h, d)) {
            count++
            d = d.minusDays(1)
        }
        return count
    }

    private fun weekKeyOf(d: LocalDate): String {
        val wf = WeekFields.ISO
        return "%d-%02d".format(d.get(wf.weekBasedYear()), d.get(wf.weekOfWeekBasedYear()))
    }

    private fun weekCount(h: Habit, wk: String): Int =
        h.checkins.entries.count { runCatching { weekKeyOf(LocalDate.parse(it.key)) == wk }.getOrDefault(false) }

    private fun prevWeekKey(wk: String): String {
        val parts = wk.split("-")
        val year = parts[0].toInt()
        val week = parts[1].toInt()
        return if (week > 1) {
            "%d-%02d".format(year, week - 1)
        } else {
            val dec28 = LocalDate.of(year - 1, 12, 28)
            weekKeyOf(dec28)
        }
    }

    private fun weeklyStreak(h: Habit, today: LocalDate): Int {
        var count = 0
        var wk = weekKeyOf(today)
        val need = if (h.freqKind == FreqKind.MONTHLY_COUNT) 1 else h.freqN
        if (weekCount(h, wk) < need) wk = prevWeekKey(wk)
        while (weekCount(h, wk) >= need) {
            count++
            wk = prevWeekKey(wk)
        }
        return count
    }

    fun maxStreak(h: Habit): Int {
        val dates = h.checkins.keys.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.sorted()
        if (dates.isEmpty()) return 0
        if (h.freqKind == FreqKind.WEEKLY_COUNT || h.freqKind == FreqKind.MONTHLY_COUNT) {
            val weeks = dates.map { weekKeyOf(it) }.distinct().sorted()
            var best = 0
            var run = 0
            var last: String? = null
            for (w in weeks) {
                run = if (last != null && prevWeekKey(last) == w) run + 1 else 1
                if (run > best) best = run
                last = w
            }
            return best
        }
        var best = 0
        var run = 0
        var last: LocalDate? = null
        for (d in dates) {
            run = if (last != null && last.plusDays(1) == d) run + 1 else 1
            if (run > best) best = run
            last = d
        }
        return best
    }

    fun totalCount(h: Habit): Int = h.checkins.values.sum()

    /** Percentage of days in the given window that were completed, 0..1. */
    fun completionRate(h: Habit, days: Int, today: LocalDate): Double {
        var done = 0
        var total = 0
        var d = today.minusDays((days - 1).toLong())
        while (!d.isAfter(today)) {
            total++
            if (dayDone(h, d)) done++
            d = d.plusDays(1)
        }
        return if (total == 0) 0.0 else done.toDouble() / total
    }

    /** Sum of recorded values in the window (quantified) or completed days (boolean). */
    fun totalInWindow(h: Habit, days: Int, today: LocalDate): Double {
        var sum = 0.0
        var d = today.minusDays((days - 1).toLong())
        while (!d.isAfter(today)) {
            sum += (h.checkins[key(d)] ?: 0)
            d = d.plusDays(1)
        }
        return sum
    }

    fun monthTotal(h: Habit, today: LocalDate): Double {
        var sum = 0.0
        var d = today.withDayOfMonth(1)
        while (!d.isAfter(today)) {
            sum += (h.checkins[key(d)] ?: 0)
            d = d.plusDays(1)
        }
        return sum
    }

    fun formatNumber(v: Double): String =
        if (abs(v - v.toLong()) < 1e-6) v.toLong().toString() else v.toString()

    fun dateStrip(today: LocalDate, count: Int = 5, reversed: Boolean = false): List<LocalDate> {
        val list = (0 until count).map { today.minusDays(it.toLong()) }
        return if (reversed) list.reversed() else list
    }

    fun monthGrid(date: LocalDate): List<LocalDate?> {
        val first = date.withDayOfMonth(1)
        val offset = first.dayOfWeek.value - 1
        val cells = mutableListOf<LocalDate?>()
        repeat(offset) { cells.add(null) }
        var d = first
        while (d.monthValue == date.monthValue) {
            cells.add(d)
            d = d.plusDays(1)
        }
        while (cells.size % 7 != 0) cells.add(null)
        return cells
    }

    private val HM = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    fun formatTime(hhmm: String?): String = hhmm ?: "关闭"
}
