package com.blackboxbench.reproduction

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Single source of truth for the reproduced Tasks app. State is held in Compose
 * snapshot collections so the UI recomposes on every write, and every write is
 * mirrored into SharedPreferences so that data survives a restart.
 */
object Repo {

    private const val PREFS = "tasks_repro"
    private const val KEY = "state"

    private lateinit var prefs: android.content.SharedPreferences

    val lists = mutableStateListOf<TaskList>()
    val tasks = mutableStateListOf<Task>()

    var onboarded by mutableStateOf(false)
    var showCompleted by mutableStateOf(true)
    var showCompletedSubtasks by mutableStateOf(true)
    var showUnstarted by mutableStateOf(true)
    var moveCompletedToBottom by mutableStateOf(true)
    var grouping by mutableStateOf(Grouping.DUE)
    var sortMode by mutableStateOf(SortMode.DUE)
    var defaultPriority by mutableStateOf(Priority.NONE)

    /** Reference "now" used for the relative date buckets. */
    var now: LocalDateTime = LocalDateTime.now()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY, null)
        if (saved != null) {
            runCatching { restore(JSONObject(saved)) }
        }
        if (tasks.isEmpty() && lists.isEmpty()) seed(context)
    }

    private fun seed(context: Context) {
        val raw = context.assets.open("tasks.json").bufferedReader().use { it.readText() }
        val root = JSONObject(raw)
        val listArray = root.getJSONArray("lists")
        for (i in 0 until listArray.length()) {
            val o = listArray.getJSONObject(i)
            lists.add(TaskList(o.getString("id"), o.getString("name")))
        }
        val taskArray = root.getJSONArray("tasks")
        for (i in 0 until taskArray.length()) {
            tasks.add(taskFromJson(taskArray.getJSONObject(i)))
        }
    }

    private fun taskFromJson(o: JSONObject): Task {
        val subs = mutableListOf<Task>()
        val sa = o.optJSONArray("subtasks")
        if (sa != null) for (i in 0 until sa.length()) subs.add(taskFromJson(sa.getJSONObject(i)))
        val tags = mutableListOf<String>()
        val ta = o.optJSONArray("tags")
        if (ta != null) for (i in 0 until ta.length()) tags.add(ta.getString(i))
        return Task(
            id = o.getString("id"),
            title = o.optString("title", ""),
            notes = o.optString("notes", ""),
            listId = o.optString("list", "").ifEmpty { null },
            due = parseDateTime(o.optString("due", "")),
            priority = Priority.from(o.optString("priority", "none")),
            tags = tags,
            repeatRule = o.optString("repeat", ""),
            completedAt = parseDateTime(o.optString("completed_at", "")),
            subtasks = subs,
            createdAt = parseDateTime(o.optString("created_at", "")) ?: now,
            modifiedAt = parseDateTime(o.optString("modified_at", "")) ?: now
        )
    }

    private fun parseDateTime(s: String): LocalDateTime? {
        if (s.isBlank()) return null
        return runCatching {
            if (s.length <= 10) LocalDate.parse(s).atStartOfDay() else LocalDateTime.parse(s)
        }.getOrNull()
    }

    // ---------------------------------------------------------------- persistence

    fun persist() {
        if (!::prefs.isInitialized) return
        prefs.edit().putString(KEY, snapshot().toString()).apply()
    }

    private fun snapshot(): JSONObject {
        val root = JSONObject()
        root.put("onboarded", onboarded)
        root.put("showCompleted", showCompleted)
        root.put("showCompletedSubtasks", showCompletedSubtasks)
        root.put("moveCompletedToBottom", moveCompletedToBottom)
        root.put("grouping", grouping.name)
        root.put("sort", sortMode.name)
        root.put("defaultPriority", defaultPriority.key)
        val la = JSONArray()
        lists.forEach { l ->
            la.put(JSONObject().put("id", l.id).put("name", l.name).put("color", l.color ?: JSONObject.NULL))
        }
        root.put("lists", la)
        val ta = JSONArray()
        tasks.forEach { ta.put(taskToJson(it)) }
        root.put("tasks", ta)
        return root
    }

    private fun taskToJson(t: Task): JSONObject {
        val o = JSONObject()
        o.put("id", t.id)
        o.put("title", t.title)
        o.put("notes", t.notes)
        o.put("list", t.listId ?: "")
        o.put("due", t.due?.toString() ?: "")
        o.put("start", t.start?.toString() ?: "")
        o.put("priority", t.priority.key)
        o.put("tags", JSONArray(t.tags))
        o.put("repeat", t.repeatRule)
        o.put("completed_at", t.completedAt?.toString() ?: "")
        o.put("created_at", t.createdAt.toString())
        o.put("modified_at", t.modifiedAt.toString())
        val sa = JSONArray()
        t.subtasks.forEach { sa.put(taskToJson(it)) }
        o.put("subtasks", sa)
        return o
    }

    private fun restore(root: JSONObject) {
        onboarded = root.optBoolean("onboarded", false)
        showCompleted = root.optBoolean("showCompleted", true)
        showCompletedSubtasks = root.optBoolean("showCompletedSubtasks", true)
        moveCompletedToBottom = root.optBoolean("moveCompletedToBottom", true)
        grouping = runCatching { Grouping.valueOf(root.optString("grouping", "DUE")) }.getOrDefault(Grouping.DUE)
        sortMode = runCatching { SortMode.valueOf(root.optString("sort", "DUE")) }.getOrDefault(SortMode.DUE)
        defaultPriority = Priority.from(root.optString("defaultPriority", "none"))
        val la = root.optJSONArray("lists") ?: JSONArray()
        for (i in 0 until la.length()) {
            val o = la.getJSONObject(i)
            val color = if (o.isNull("color")) null else o.optLong("color")
            lists.add(TaskList(o.getString("id"), o.getString("name"), color))
        }
        val ta = root.optJSONArray("tasks") ?: JSONArray()
        for (i in 0 until ta.length()) tasks.add(taskFromJson(ta.getJSONObject(i)))
    }

    // ---------------------------------------------------------------- mutations

    fun completeOnboarding() {
        onboarded = true
        persist()
    }

    fun listName(id: String?): String =
        lists.firstOrNull { it.id == id }?.name ?: "默认清单"

    fun list(id: String?): TaskList? = lists.firstOrNull { it.id == id }

    fun allTags(): List<String> {
        val set = LinkedHashSet<String>()
        fun walk(t: Task) { set.addAll(t.tags); t.subtasks.forEach(::walk) }
        tasks.forEach(::walk)
        return set.sorted()
    }

    fun findTask(id: String): Task? {
        tasks.firstOrNull { it.id == id }?.let { return it }
        tasks.forEach { p -> p.subtasks.firstOrNull { it.id == id }?.let { return it } }
        return null
    }

    fun updateTask(updated: Task) {
        val stamped = updated.copy(modifiedAt = LocalDateTime.now())
        val idx = tasks.indexOfFirst { it.id == stamped.id }
        if (idx >= 0) {
            tasks[idx] = stamped
        } else {
            for (p in tasks.indices) {
                val parent = tasks[p]
                val si = parent.subtasks.indexOfFirst { it.id == stamped.id }
                if (si >= 0) {
                    val subs = parent.subtasks.toMutableList()
                    subs[si] = stamped
                    tasks[p] = parent.copy(subtasks = subs)
                    break
                }
            }
        }
        persist()
    }

    fun addTask(task: Task) {
        tasks.add(task)
        persist()
    }

    fun deleteTask(id: String) {
        tasks.removeAll { it.id == id }
        for (p in tasks.indices) {
            val parent = tasks[p]
            if (parent.subtasks.any { it.id == id }) {
                tasks[p] = parent.copy(subtasks = parent.subtasks.filterNot { it.id == id })
            }
        }
        persist()
    }

    /**
     * Toggles completion. Completing a repeating task regenerates the next
     * occurrence on the same entry (the due date is advanced, the rule stays).
     */
    fun toggleComplete(task: Task, completed: Boolean) {
        var next = task
        if (completed) {
            next = next.copy(completedAt = LocalDateTime.now())
            if (next.repeatRule.isNotEmpty()) {
                val base = next.due ?: now
                val nextDate = RepeatRules.next(next.repeatRule, base)
                if (nextDate != null) {
                    next = next.copy(
                        due = nextDate,
                        completedAt = null,
                        // subtasks of a repeating task reset for the new instance
                        subtasks = next.subtasks.map { it.copy(completedAt = null) }
                    )
                }
            }
        } else {
            next = next.copy(completedAt = null)
        }
        updateTask(next)
    }

    fun newTask(listId: String?): Task = Task(
        id = "t" + System.currentTimeMillis(),
        title = "",
        listId = listId ?: lists.firstOrNull()?.id,
        priority = defaultPriority,
        createdAt = LocalDateTime.now(),
        modifiedAt = LocalDateTime.now()
    )

    fun addList(name: String) {
        if (name.isBlank()) return
        lists.add(TaskList("lst_" + System.currentTimeMillis(), name))
        persist()
    }

    fun addTag(name: String) {
        if (name.isBlank()) return
        persist()
    }

    // ---------------------------------------------------------------- queries

    /** All top level tasks visible for the given navigation target. */
    fun visibleTasks(nav: Nav): List<Task> {
        val filtered = when (nav) {
            is Nav.MyTasks -> tasks
            is Nav.List -> tasks.filter { it.listId == nav.id }
            is Nav.Tag -> tasks.filter { it.tags.contains(nav.name) }
            is Nav.Today -> tasks.filter {
                it.due != null && !it.due.toLocalDate().isAfter(now.toLocalDate())
            }
            is Nav.Recent -> tasks.sortedByDescending { it.modifiedAt }
        }
        return filtered.filter { showCompleted || !it.isCompleted }
    }

    fun isOverdue(t: Task): Boolean =
        !t.isCompleted && t.due != null && t.due.toLocalDate().isBefore(now.toLocalDate())

    fun bucket(t: Task): String {
        val due = t.due ?: return "无截止日期"
        val today = now.toLocalDate()
        val d = due.toLocalDate()
        return when {
            d.isBefore(today) -> "已过期"
            d.isEqual(today) -> "今天"
            d.isEqual(today.plusDays(1)) -> "明天"
            d.isBefore(today.plusDays(7)) -> "本周"
            d.isBefore(today.plusDays(14)) -> "下周"
            else -> "以后"
        }
    }

    fun daysUntil(d: LocalDateTime): Long =
        ChronoUnit.DAYS.between(now.toLocalDate(), d.toLocalDate())
}
