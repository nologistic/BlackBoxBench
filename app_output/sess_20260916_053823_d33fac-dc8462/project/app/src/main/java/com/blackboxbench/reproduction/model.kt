package com.blackboxbench.reproduction

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class TaskListRec(
    val id: String,
    val name: String,
    val color: String = "none",
    val icon: String = "list"
)

data class SubtaskRec(
    val id: String,
    var title: String,
    var due: String? = null,
    var priority: Int = 0,
    var completedAt: String? = null
)

data class TaskRec(
    val id: String,
    var listId: String,
    var title: String,
    var notes: String = "",
    var start: String? = null,
    var due: String? = null,
    var priority: Int = 0,
    var tags: MutableList<String> = mutableListOf(),
    var repeatRule: String? = null,
    var completedAt: String? = null,
    var subtasks: MutableList<SubtaskRec> = mutableListOf(),
    var createdAt: Long = System.currentTimeMillis(),
    var modifiedAt: Long = System.currentTimeMillis()
) {
    val isCompleted: Boolean get() = !completedAt.isNullOrEmpty()
    val pendingSubtasks: Int get() = subtasks.count { it.completedAt.isNullOrEmpty() }
}

object Store {
    private const val PREFS = "tasks_repro"
    private const val KEY = "state_json"
    private const val KEY_ONBOARDED = "onboarded"

    var revision by mutableStateOf(0)
        private set

    val lists = mutableListOf<TaskListRec>()
    val tasks = mutableListOf<TaskRec>()

    var onboarded: Boolean = false

    val filters = mutableListOf("今天", "最近修改过的")

    fun touch() {
        revision++
    }

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        onboarded = prefs.getBoolean(KEY_ONBOARDED, false)
        val raw = prefs.getString(KEY, null)
        if (raw != null) {
            runCatching { decode(raw) }.onFailure { seed(context) }
        } else {
            seed(context)
        }
        if (lists.isEmpty()) lists.add(TaskListRec("lst_default", "默认清单"))
        touch()
    }

    fun setOnboarded(context: Context) {
        onboarded = true
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ONBOARDED, true).apply()
        touch()
    }

    fun save(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY, encode()).apply()
        touch()
    }

    private fun seed(context: Context) {
        lists.clear()
        tasks.clear()
        val text = runCatching {
            context.assets.open("tasks.json").bufferedReader().use { it.readText() }
        }.getOrNull()
        if (text == null) {
            lists.add(TaskListRec("lst_default", "默认清单"))
            return
        }
        val root = JSONObject(text)
        val la = root.getJSONArray("lists")
        for (i in 0 until la.length()) {
            val o = la.getJSONObject(i)
            lists.add(TaskListRec(o.getString("id"), o.getString("name")))
        }
        val ta = root.getJSONArray("tasks")
        var order = 0L
        for (i in 0 until ta.length()) {
            val o = ta.getJSONObject(i)
            val task = TaskRec(
                id = o.getString("id"),
                listId = o.getString("list"),
                title = o.getString("title"),
                notes = o.optString("notes", ""),
                due = o.optString("due", "").ifEmpty { null },
                priority = priorityFrom(o.optString("priority", "")),
                repeatRule = o.optString("repeat", "").ifEmpty { null },
                completedAt = o.optString("completed_at", "").ifEmpty { null },
                createdAt = order,
                modifiedAt = order
            )
            order++
            o.optJSONArray("tags")?.let { arr ->
                for (j in 0 until arr.length()) task.tags.add(arr.getString(j))
            }
            o.optJSONArray("subtasks")?.let { arr ->
                for (j in 0 until arr.length()) {
                    val s = arr.getJSONObject(j)
                    task.subtasks.add(
                        SubtaskRec(
                            id = s.getString("id"),
                            title = s.getString("title"),
                            due = s.optString("due", "").ifEmpty { null },
                            priority = priorityFrom(s.optString("priority", "")),
                            completedAt = s.optString("completed_at", "").ifEmpty { null }
                        )
                    )
                }
            }
            tasks.add(task)
        }
        save(context)
    }

    fun priorityFrom(v: String): Int = when (v) {
        "high" -> 3
        "medium" -> 2
        "low" -> 1
        else -> 0
    }

    fun priorityName(p: Int): String = when (p) {
        3 -> "high"
        2 -> "medium"
        1 -> "low"
        else -> ""
    }

    fun addList(name: String): TaskListRec {
        val rec = TaskListRec("lst_" + UUID.randomUUID().toString().take(8), name)
        lists.add(rec)
        return rec
    }

    fun addTask(listId: String): TaskRec {
        val rec = TaskRec(
            id = "t_" + UUID.randomUUID().toString().take(8),
            listId = listId,
            title = ""
        )
        tasks.add(0, rec)
        return rec
    }

    fun findTask(id: String?): TaskRec? = tasks.firstOrNull { it.id == id }

    fun allTags(): List<String> {
        val set = LinkedHashSet<String>()
        tasks.forEach { it.tags.forEach { t -> set.add(t) } }
        return set.sorted()
    }

    fun listName(id: String): String = lists.firstOrNull { it.id == id }?.name ?: "默认清单"

    fun listCount(id: String): Int = tasks.count { it.listId == id && !it.isCompleted }

    fun tagCount(tag: String): Int = tasks.count { tag in it.tags && !it.isCompleted }

    fun complete(task: TaskRec) {
        if (task.repeatRule.isNullOrEmpty()) {
            task.completedAt = nowStamp()
        } else {
            advanceRepeat(task)
        }
        task.modifiedAt = System.currentTimeMillis()
    }

    fun reopen(task: TaskRec) {
        task.completedAt = null
        task.modifiedAt = System.currentTimeMillis()
    }

    private fun advanceRepeat(task: TaskRec) {
        val base = parseDateTime(task.due) ?: LocalDateTime.now()
        val next = nextOccurrence(task.repeatRule!!, base)
        task.due = format(next)
        task.completedAt = null
    }

    fun nextOccurrence(rule: String, from: LocalDateTime): LocalDateTime {
        val parts = rule.split(";").associate {
            val kv = it.split("=")
            kv[0] to (kv.getOrNull(1) ?: "")
        }
        return when (parts["FREQ"]) {
            "DAILY" -> from.plusDays(1)
            "WEEKLY" -> {
                val days = (parts["BYDAY"] ?: "").split(",").mapNotNull { dayCode(it) }
                if (days.isEmpty()) {
                    from.plusWeeks(1)
                } else {
                    var probe = from.plusDays(1)
                    while (!days.contains(probe.dayOfWeek)) probe = probe.plusDays(1)
                    probe
                }
            }
            "MONTHLY" -> {
                val day = parts["BYMONTHDAY"]?.toIntOrNull() ?: from.dayOfMonth
                var probe = from.plusMonths(1)
                val max = probe.toLocalDate().lengthOfMonth()
                probe.withDayOfMonth(day.coerceAtMost(max))
            }
            "YEARLY" -> from.plusYears(1)
            else -> from.plusDays(1)
        }
    }

    private fun dayCode(s: String) = when (s.trim()) {
        "MO" -> java.time.DayOfWeek.MONDAY
        "TU" -> java.time.DayOfWeek.TUESDAY
        "WE" -> java.time.DayOfWeek.WEDNESDAY
        "TH" -> java.time.DayOfWeek.THURSDAY
        "FR" -> java.time.DayOfWeek.FRIDAY
        "SA" -> java.time.DayOfWeek.SATURDAY
        "SU" -> java.time.DayOfWeek.SUNDAY
        else -> null
    }

    fun encode(): String {
        val root = JSONObject()
        val la = JSONArray()
        lists.forEach { l ->
            la.put(JSONObject().put("id", l.id).put("name", l.name).put("color", l.color).put("icon", l.icon))
        }
        root.put("lists", la)
        val ta = JSONArray()
        tasks.forEach { t ->
            val o = JSONObject()
            o.put("id", t.id)
            o.put("list", t.listId)
            o.put("title", t.title)
            o.put("notes", t.notes)
            o.put("start", t.start ?: "")
            o.put("due", t.due ?: "")
            o.put("priority", priorityName(t.priority))
            o.put("repeat", t.repeatRule ?: "")
            o.put("completed_at", t.completedAt ?: "")
            o.put("created_at", t.createdAt)
            o.put("modified_at", t.modifiedAt)
            o.put("tags", JSONArray(t.tags))
            val sa = JSONArray()
            t.subtasks.forEach { s ->
                sa.put(
                    JSONObject()
                        .put("id", s.id)
                        .put("title", s.title)
                        .put("due", s.due ?: "")
                        .put("priority", priorityName(s.priority))
                        .put("completed_at", s.completedAt ?: "")
                )
            }
            o.put("subtasks", sa)
            ta.put(o)
        }
        root.put("tasks", ta)
        return root.toString()
    }

    fun decode(raw: String) {
        lists.clear()
        tasks.clear()
        val root = JSONObject(raw)
        val la = root.getJSONArray("lists")
        for (i in 0 until la.length()) {
            val o = la.getJSONObject(i)
            lists.add(TaskListRec(o.getString("id"), o.getString("name"), o.optString("color", "none"), o.optString("icon", "list")))
        }
        val ta = root.getJSONArray("tasks")
        for (i in 0 until ta.length()) {
            val o = ta.getJSONObject(i)
            val t = TaskRec(
                id = o.getString("id"),
                listId = o.getString("list"),
                title = o.optString("title", ""),
                notes = o.optString("notes", ""),
                start = o.optString("start", "").ifEmpty { null },
                due = o.optString("due", "").ifEmpty { null },
                priority = priorityFrom(o.optString("priority", "")),
                repeatRule = o.optString("repeat", "").ifEmpty { null },
                completedAt = o.optString("completed_at", "").ifEmpty { null },
                createdAt = o.optLong("created_at", 0L),
                modifiedAt = o.optLong("modified_at", 0L)
            )
            o.optJSONArray("tags")?.let { arr -> for (j in 0 until arr.length()) t.tags.add(arr.getString(j)) }
            o.optJSONArray("subtasks")?.let { arr ->
                for (j in 0 until arr.length()) {
                    val s = arr.getJSONObject(j)
                    t.subtasks.add(
                        SubtaskRec(
                            s.getString("id"),
                            s.optString("title", ""),
                            s.optString("due", "").ifEmpty { null },
                            priorityFrom(s.optString("priority", "")),
                            s.optString("completed_at", "").ifEmpty { null }
                        )
                    )
                }
            }
            tasks.add(t)
        }
    }
}

// ---------- date helpers ----------

fun nowStamp(): String = LocalDateTime.now().toString().substring(0, 16)

fun parseDateTime(s: String?): LocalDateTime? {
    if (s.isNullOrEmpty()) return null
    return runCatching {
        if (s.contains("T")) LocalDateTime.parse(s) else LocalDate.parse(s).atTime(0, 0)
    }.getOrNull()
}

fun dueDate(s: String?): LocalDate? = parseDateTime(s)?.toLocalDate()

fun format(dt: LocalDateTime): String {
    val base = dt.toString()
    return if (dt.hour == 0 && dt.minute == 0) base.substring(0, 10) else base.substring(0, 16)
}

fun humanDate(d: LocalDate?): String {
    if (d == null) return ""
    val today = LocalDate.now()
    return when (d) {
        today -> "今天"
        today.plusDays(1) -> "明天"
        today.minusDays(1) -> "昨天"
        else -> "${d.monthValue}月${d.dayOfMonth}日"
    }
}

fun bucketOf(task: TaskRec): Int {
    val d = dueDate(task.due) ?: return 4
    val today = LocalDate.now()
    return when {
        d.isBefore(today) -> 0
        d == today -> 1
        d == today.plusDays(1) -> 2
        else -> 3
    }
}

val bucketTitles = listOf("已过期", "今天截止", "明天截止", "以后", "无截止日期")
