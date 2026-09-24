package com.blackboxbench.reproduction

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

data class Subtask(
    var id: String,
    var title: String,
    var due: String = "",
    var priority: String = "none",
    var completedAt: String = ""
)

data class Task(
    var id: String,
    var listId: String,
    var title: String,
    var notes: String = "",
    var due: String = "",
    var priority: String = "none",
    var tags: MutableList<String> = mutableListOf(),
    var repeat: String = "",
    var completedAt: String = "",
    var subtasks: MutableList<Subtask> = mutableListOf(),
    var createdAt: Long = 0L
)

data class TaskList(var id: String, var name: String)

data class Prefs(
    var theme: String = "system",
    var hideCompleted: Boolean = true,
    var sort: String = "my",
    var setupDone: Boolean = false
)

data class Data(
    var lists: MutableList<TaskList> = mutableListOf(),
    var tasks: MutableList<Task> = mutableListOf(),
    var knownTags: MutableList<String> = mutableListOf(),
    var prefs: Prefs = Prefs()
) {
    fun toJson(): String {
        val root = JSONObject()
        root.put("lists", JSONArray().apply {
            for (l in lists) put(JSONObject().put("id", l.id).put("name", l.name))
        })
        root.put("tasks", JSONArray().apply {
            for (t in tasks) {
                val o = JSONObject()
                o.put("id", t.id); o.put("list", t.listId); o.put("title", t.title)
                o.put("notes", t.notes); o.put("due", t.due); o.put("priority", t.priority)
                o.put("repeat", t.repeat); o.put("completed_at", t.completedAt)
                o.put("created_at", t.createdAt)
                o.put("tags", JSONArray().apply { for (g in t.tags) put(g) })
                o.put("subtasks", JSONArray().apply {
                    for (s in t.subtasks) put(JSONObject()
                        .put("id", s.id).put("title", s.title).put("due", s.due)
                        .put("priority", s.priority).put("completed_at", s.completedAt))
                })
                put(o)
            }
        })
        root.put("known_tags", JSONArray().apply { for (g in knownTags) put(g) })
        root.put("prefs", JSONObject()
            .put("theme", prefs.theme).put("hide_completed", prefs.hideCompleted)
            .put("sort", prefs.sort).put("setup_done", prefs.setupDone))
        return root.toString()
    }

    companion object {
        fun parse(text: String): Data {
            val root = JSONObject(text)
            val d = Data()
            val lists = root.optJSONArray("lists") ?: JSONArray()
            for (i in 0 until lists.length()) {
                val o = lists.getJSONObject(i)
                d.lists.add(TaskList(o.optString("id"), o.optString("name")))
            }
            val tasks = root.optJSONArray("tasks") ?: JSONArray()
            for (i in 0 until tasks.length()) {
                val o = tasks.getJSONObject(i)
                val t = Task(
                    id = o.optString("id"),
                    listId = o.optString("list"),
                    title = o.optString("title"),
                    notes = o.optString("notes"),
                    due = o.optString("due"),
                    priority = o.optString("priority").ifBlank { "none" },
                    repeat = o.optString("repeat"),
                    completedAt = o.optString("completed_at"),
                    createdAt = o.optLong("created_at", i.toLong())
                )
                val tags = o.optJSONArray("tags")
                if (tags != null) for (j in 0 until tags.length()) t.tags.add(tags.optString(j))
                val subs = o.optJSONArray("subtasks")
                if (subs != null) for (j in 0 until subs.length()) {
                    val s = subs.getJSONObject(j)
                    t.subtasks.add(Subtask(
                        s.optString("id"), s.optString("title"), s.optString("due"),
                        s.optString("priority").ifBlank { "none" }, s.optString("completed_at")))
                }
                d.tasks.add(t)
            }
            val kt = root.optJSONArray("known_tags")
            if (kt != null) for (j in 0 until kt.length()) d.knownTags.add(kt.optString(j))
            val p = root.optJSONObject("prefs")
            if (p != null) {
                d.prefs.theme = p.optString("theme").ifBlank { "system" }
                d.prefs.hideCompleted = p.optBoolean("hide_completed", true)
                d.prefs.sort = p.optString("sort").ifBlank { "my" }
                d.prefs.setupDone = p.optBoolean("setup_done", false)
            }
            return d
        }
    }
}

enum class ToggleResult { NONE, COMPLETED, RESTORED, RECURRING }

class Store private constructor(private val appContext: Context) {

    val version = mutableStateOf(0)
    var data: Data
        private set

    init {
        val file = File(appContext.filesDir, "state.json")
        data = if (file.exists()) {
            runCatching { Data.parse(file.readText()) }.getOrNull() ?: seed()
        } else seed()
        persist()
    }

    private fun seed(): Data =
        Data.parse(appContext.assets.open("tasks.json").bufferedReader().use { it.readText() })

    private fun persist() {
        runCatching {
            File(appContext.filesDir, "state.json").writeText(data.toJson())
        }
    }

    private fun bump() {
        persist()
        version.value = version.value + 1
    }

    // ---- preferences ----
    fun completeSetup() { data.prefs.setupDone = true; bump() }
    fun setTheme(t: String) { data.prefs.theme = t; bump() }
    fun setHideCompleted(h: Boolean) { data.prefs.hideCompleted = h; bump() }
    fun setSort(s: String) { data.prefs.sort = s; bump() }

    // ---- lists ----
    fun addList(name: String): TaskList {
        val l = TaskList("lst_" + System.currentTimeMillis(), name)
        data.lists.add(l)
        bump()
        return l
    }

    // ---- tags ----
    fun allTags(): List<String> {
        val s = LinkedHashSet<String>(data.knownTags)
        data.tasks.forEach { t -> t.tags.forEach { s.add(it) } }
        return s.toList()
    }

    fun addTag(name: String) {
        val n = name.trim()
        if (n.isNotEmpty() && allTags().none { it == n } && data.knownTags.none { it == n }) {
            data.knownTags.add(n)
            bump()
        }
    }

    // ---- tasks ----
    fun task(id: String): Task? = data.tasks.firstOrNull { it.id == id }

    fun insertTask(t: Task) {
        t.id = "t_" + System.currentTimeMillis()
        t.createdAt = System.currentTimeMillis()
        data.tasks.add(0, t)
        bump()
    }

    fun updateTask(t: Task) {
        val i = data.tasks.indexOfFirst { it.id == t.id }
        if (i >= 0) data.tasks[i] = t
        bump()
    }

    fun deleteTask(id: String) {
        data.tasks.removeAll { it.id == id }
        bump()
    }

    fun deleteTasks(ids: Set<String>) {
        data.tasks.removeAll { it.id in ids }
        bump()
    }

    fun toggleTask(id: String): ToggleResult {
        val t = task(id) ?: return ToggleResult.NONE
        return if (t.completedAt.isNotEmpty()) {
            t.completedAt = ""
            bump()
            ToggleResult.RESTORED
        } else if (t.repeat.isNotEmpty()) {
            t.due = nextDue(t.due, t.repeat)
            t.completedAt = ""
            bump()
            ToggleResult.RECURRING
        } else {
            t.completedAt = LocalDateTime.now()
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            bump()
            ToggleResult.COMPLETED
        }
    }

    fun toggleSubtask(taskId: String, subId: String) {
        val s = task(taskId)?.subtasks?.firstOrNull { it.id == subId } ?: return
        s.completedAt = if (s.completedAt.isEmpty())
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) else ""
        bump()
    }

    fun addSubtask(taskId: String, title: String) {
        val t = task(taskId) ?: return
        if (title.isBlank()) return
        t.subtasks.add(Subtask("s_" + System.currentTimeMillis(), title.trim()))
        bump()
    }

    // ---- queries ----
    fun listName(id: String): String = data.lists.firstOrNull { it.id == id }?.name ?: "我的任务"
    fun listBadge(listId: String): Int =
        data.tasks.count { it.listId == listId && it.completedAt.isEmpty() }
    fun tagBadge(tag: String): Int =
        data.tasks.count { tag in it.tags && it.completedAt.isEmpty() }

    fun tasksFor(view: String): List<Task> {
        val base: List<Task> = when {
            view == "all" -> data.tasks.toList()
            view == "today" -> data.tasks.filter { dueDate(it.due)?.isEqual(LocalDate.now()) == true }
            view == "recent" -> data.tasks.sortedByDescending { it.createdAt }.take(10)
            view == "later" -> data.tasks.filter { dueDate(it.due)?.isAfter(LocalDate.now()) == true }
            view.startsWith("list:") -> {
                val id = view.removePrefix("list:")
                data.tasks.filter { it.listId == id }
            }
            view.startsWith("tag:") -> {
                val g = view.removePrefix("tag:")
                data.tasks.filter { g in it.tags }
            }
            else -> data.tasks.toList()
        }
        val visible = if (data.prefs.hideCompleted) base.filter { it.completedAt.isEmpty() } else base
        return sorted(visible)
    }

    private fun sorted(list: List<Task>): List<Task> = when (data.prefs.sort) {
        "due" -> list.sortedWith(compareBy({ it.completedAt.isNotEmpty() },
            { dueDate(it.due) ?: LocalDate.MAX }))
        "priority" -> list.sortedWith(compareBy({ it.completedAt.isNotEmpty() }, { prioRank(it.priority) }))
        "title" -> list.sortedWith(compareBy({ it.completedAt.isNotEmpty() }, { it.title }))
        else -> list.sortedBy { it.completedAt.isNotEmpty() }
    }

    companion object {
        @Volatile private var inst: Store? = null
        fun get(context: Context): Store =
            inst ?: synchronized(this) {
                inst ?: Store(context.applicationContext).also { inst = it }
            }
    }
}

// ---- date / priority helpers ----

fun dueDate(due: String): LocalDate? = when {
    due.isBlank() -> null
    due.contains("T") -> runCatching { LocalDate.parse(due.substringBefore("T")) }.getOrNull()
    else -> runCatching { LocalDate.parse(due) }.getOrNull()
}

fun dueDateTime(due: String): LocalDateTime? = when {
    due.isBlank() -> null
    due.contains("T") -> runCatching { LocalDateTime.parse(due) }.getOrNull()
        ?: dueDate(due)?.atStartOfDay()
    else -> dueDate(due)?.atStartOfDay()
}

fun hasTime(due: String): Boolean = due.contains("T")

fun formatDue(due: String): String {
    val d = dueDate(due) ?: return ""
    val today = LocalDate.now()
    var s = "${d.monthValue}月${d.dayOfMonth}日"
    if (d == today) s += " (今天)"
    else if (d == today.plusDays(1)) s += " (明天)"
    else if (d == today.minusDays(1)) s += " (昨天)"
    if (hasTime(due)) {
        val t = dueDateTime(due) ?: return s
        val h12 = when (val h = t.hour) { 0 -> 12; in 13..23 -> h - 12; else -> h }
        val ampm = if (t.hour < 12) "上午" else "下午"
        s += " ($ampm$h12:${"%02d".format(t.minute)})"
    }
    return s
}

fun todaySubtitle(): String {
    val d = LocalDate.now()
    val dow = when (d.dayOfWeek) {
        java.time.DayOfWeek.MONDAY -> "周一"; java.time.DayOfWeek.TUESDAY -> "周二"
        java.time.DayOfWeek.WEDNESDAY -> "周三"; java.time.DayOfWeek.THURSDAY -> "周四"
        java.time.DayOfWeek.FRIDAY -> "周五"; java.time.DayOfWeek.SATURDAY -> "周六"
        else -> "周日"
    }
    return "${d.monthValue}月${d.dayOfMonth}日 ($dow)"
}

fun priorityLabel(p: String): String = when (p) {
    "high" -> "高 (!!!)"; "medium" -> "中 (!!)"; "low" -> "低 (!)"; else -> "无"
}

fun priorityMarks(p: String): String = when (p) {
    "high" -> "!!!"; "medium" -> "!!"; "low" -> "!"; else -> ""
}

fun prioRank(p: String): Int = when (p) { "high" -> 0; "medium" -> 1; "low" -> 2; else -> 3 }

fun repeatLabel(r: String): String = when {
    r.contains("DAILY") -> "每天"; r.contains("WEEKLY") -> "每周"
    r.contains("MONTHLY") -> "每月"; else -> "不重复"
}

fun subtaskSummary(t: Task): String? {
    if (t.subtasks.isEmpty()) return null
    val done = t.subtasks.count { it.completedAt.isNotEmpty() }
    return "$done/${t.subtasks.size}"
}

fun nextDue(due: String, repeat: String): String {
    var dt = dueDateTime(due) ?: LocalDateTime.now().plusDays(1)
    val step: (LocalDateTime) -> LocalDateTime = when {
        repeat.contains("DAILY") -> { d -> d.plusDays(1) }
        repeat.contains("WEEKLY") -> { d -> d.plusWeeks(1) }
        repeat.contains("MONTHLY") -> { d -> d.plusMonths(1) }
        else -> { d -> d.plusDays(1) }
    }
    var guard = 0
    while (!dt.isAfter(LocalDateTime.now()) && guard < 800) { dt = step(dt); guard++ }
    return if (hasTime(due))
        dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    else dt.toLocalDate().toString()
}

fun millisForDate(d: LocalDate): Long =
    d.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

fun localDateFromMillis(millis: Long): LocalDate =
    java.time.Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
