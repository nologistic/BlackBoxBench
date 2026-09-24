package com.blackboxbench.reproduction

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

data class SubTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val completed: Boolean = false
)

data class AppTask(
    val id: String = UUID.randomUUID().toString(),
    val listId: String,
    val title: String,
    val notes: String = "",
    val due: String = "",
    val priority: String = "low",
    val tags: List<String> = emptyList(),
    val repeat: String = "",
    val completed: Boolean = false,
    val subtasks: List<SubTask> = emptyList()
)

data class TaskList(val id: String, val name: String, val color: Long = 0xFF2196F3)
data class FilterSpec(val name: String, val kind: String = "high")

class TaskStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("tasks_clone", Context.MODE_PRIVATE)

    fun loadTasks(): List<AppTask> {
        val raw = prefs.getString("tasks", null) ?: AssetStore.readText(context, "tasks.json")
        val root = JSONObject(raw)
        val source = root.optJSONArray("tasks") ?: JSONArray()
        return (0 until source.length()).map { i ->
            val o = source.getJSONObject(i)
            val children = o.optJSONArray("subtasks") ?: JSONArray()
            AppTask(
                id = o.optString("id", UUID.randomUUID().toString()),
                listId = o.optString("list", "lst_work"),
                title = o.optString("title"),
                notes = o.optString("notes"),
                due = o.optString("due"),
                priority = o.optString("priority", "low"),
                tags = o.optJSONArray("tags").toStrings(),
                repeat = o.optString("repeat"),
                completed = o.optString("completed_at").isNotBlank() || o.optBoolean("completed", false),
                subtasks = (0 until children.length()).map { j ->
                    val c = children.getJSONObject(j)
                    SubTask(
                        id = c.optString("id", UUID.randomUUID().toString()),
                        title = c.optString("title"),
                        completed = c.optString("completed_at").isNotBlank() || c.optBoolean("completed", false)
                    )
                }
            )
        }
    }

    fun saveTasks(tasks: List<AppTask>) {
        val root = JSONObject()
        val arr = JSONArray()
        tasks.forEach { t ->
            val o = JSONObject()
                .put("id", t.id)
                .put("list", t.listId)
                .put("title", t.title)
                .put("notes", t.notes)
                .put("due", t.due)
                .put("priority", t.priority)
                .put("repeat", t.repeat)
                .put("completed", t.completed)
                .put("completed_at", if (t.completed) "saved" else "")
            val tags = JSONArray()
            t.tags.forEach(tags::put)
            o.put("tags", tags)
            val children = JSONArray()
            t.subtasks.forEach { s ->
                children.put(JSONObject().put("id", s.id).put("title", s.title).put("completed", s.completed).put("completed_at", if (s.completed) "saved" else ""))
            }
            o.put("subtasks", children)
            arr.put(o)
        }
        root.put("tasks", arr)
        prefs.edit().putString("tasks", root.toString()).apply()
    }

    fun loadLists(): List<TaskList> {
        val saved = prefs.getString("lists", null)
        if (saved != null) {
            val arr = JSONArray(saved)
            return (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                TaskList(o.getString("id"), o.getString("name"), o.optLong("color", 0xFF2196F3))
            }
        }
        return listOf(
            TaskList("lst_work", "工作", 0xFF2196F3),
            TaskList("lst_life", "生活", 0xFF43A047),
            TaskList("lst_shopping", "购物", 0xFFFF9800)
        )
    }

    fun saveLists(lists: List<TaskList>) {
        val arr = JSONArray()
        lists.forEach { arr.put(JSONObject().put("id", it.id).put("name", it.name).put("color", it.color)) }
        prefs.edit().putString("lists", arr.toString()).apply()
    }

    fun loadFilters(): List<FilterSpec> {
        val raw = prefs.getString("filters", null) ?: return emptyList()
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            FilterSpec(o.getString("name"), o.optString("kind", "high"))
        }
    }

    fun saveFilters(filters: List<FilterSpec>) {
        val arr = JSONArray()
        filters.forEach { arr.put(JSONObject().put("name", it.name).put("kind", it.kind)) }
        prefs.edit().putString("filters", arr.toString()).apply()
    }

    fun isDark() = prefs.getBoolean("dark", false)
    fun saveDark(value: Boolean) = prefs.edit().putBoolean("dark", value).apply()

    fun hideCompleted() = prefs.getBoolean("hide_completed", false)
    fun saveHideCompleted(value: Boolean) = prefs.edit().putBoolean("hide_completed", value).apply()
}

private fun JSONArray?.toStrings(): List<String> {
    if (this == null) return emptyList()
    return (0 until length()).map { optString(it) }
}

fun nextDay(value: String): String {
    if (value.isBlank()) return LocalDate.now().plusDays(1).toString()
    return runCatching {
        val date = LocalDate.parse(value.take(10), DateTimeFormatter.ISO_LOCAL_DATE).plusDays(1)
        date.toString() + value.drop(10)
    }.getOrDefault(value)
}

fun dueLabel(due: String): String {
    if (due.isBlank()) return "无截止日期"
    val date = due.take(10)
    return when (date) {
        "2026-09-20" -> "今天"
        "2026-09-21" -> "明天"
        else -> date.substring(5).replace("-", "月") + "日"
    }
}
