package com.blackboxbench.reproduction

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

data class TaskListItem(val id: String, val name: String, val color: Long = 0xFF2196F3)
data class SubTaskItem(val id: String, val title: String, val completed: Boolean = false)
data class TaskItem(
    val id: String,
    val listId: String,
    val title: String,
    val notes: String = "",
    val due: String = "",
    val priority: String = "low",
    val tags: List<String> = emptyList(),
    val repeat: String = "",
    val completed: Boolean = false,
    val subtasks: List<SubTaskItem> = emptyList()
)

class TaskRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("tasks_clone_store", Context.MODE_PRIVATE)

    fun isOnboarded(): Boolean = prefs.getBoolean("onboarded", false)
    fun setOnboarded(value: Boolean) = prefs.edit().putBoolean("onboarded", value).apply()

    fun load(): Pair<List<TaskListItem>, List<TaskItem>> {
        val saved = prefs.getString("data", null)
        return if (saved.isNullOrBlank()) loadSeed() else parse(JSONObject(saved))
    }

    private fun loadSeed(): Pair<List<TaskListItem>, List<TaskItem>> {
        val text = context.assets.open("tasks.json").bufferedReader().use { it.readText() }
        val data = parse(JSONObject(text))
        save(data.first, data.second)
        return data
    }

    fun save(lists: List<TaskListItem>, tasks: List<TaskItem>) {
        val root = JSONObject()
        val listArray = JSONArray()
        lists.forEach { list ->
            listArray.put(JSONObject().apply {
                put("id", list.id)
                put("name", list.name)
                put("color", list.color)
            })
        }
        val taskArray = JSONArray()
        tasks.forEach { task ->
            taskArray.put(JSONObject().apply {
                put("id", task.id)
                put("list", task.listId)
                put("title", task.title)
                put("notes", task.notes)
                put("due", task.due)
                put("priority", task.priority)
                put("repeat", task.repeat)
                put("completed", task.completed)
                put("tags", JSONArray(task.tags))
                put("subtasks", JSONArray().apply {
                    task.subtasks.forEach { sub ->
                        put(JSONObject().apply {
                            put("id", sub.id)
                            put("title", sub.title)
                            put("completed", sub.completed)
                        })
                    }
                })
            })
        }
        root.put("lists", listArray)
        root.put("tasks", taskArray)
        prefs.edit().putString("data", root.toString()).apply()
    }

    private fun parse(root: JSONObject): Pair<List<TaskListItem>, List<TaskItem>> {
        val listArray = root.optJSONArray("lists") ?: JSONArray()
        val lists = buildList {
            for (i in 0 until listArray.length()) {
                val item = listArray.getJSONObject(i)
                val fallback = when (i) {
                    0 -> 0xFF2196F3
                    1 -> 0xFF7E57C2
                    else -> 0xFFC3D51F
                }
                add(TaskListItem(item.optString("id"), item.optString("name"), item.optLong("color", fallback)))
            }
        }
        val taskArray = root.optJSONArray("tasks") ?: JSONArray()
        val tasks = buildList {
            for (i in 0 until taskArray.length()) {
                val item = taskArray.getJSONObject(i)
                val tagsJson = item.optJSONArray("tags") ?: JSONArray()
                val tags = buildList { for (j in 0 until tagsJson.length()) add(tagsJson.optString(j)) }
                val subsJson = item.optJSONArray("subtasks") ?: JSONArray()
                val subs = buildList {
                    for (j in 0 until subsJson.length()) {
                        val sub = subsJson.getJSONObject(j)
                        add(SubTaskItem(
                            sub.optString("id", UUID.randomUUID().toString()),
                            sub.optString("title"),
                            sub.optBoolean("completed", sub.optString("completed_at").isNotBlank())
                        ))
                    }
                }
                add(TaskItem(
                    id = item.optString("id", UUID.randomUUID().toString()),
                    listId = item.optString("list", lists.firstOrNull()?.id ?: "default"),
                    title = item.optString("title"),
                    notes = item.optString("notes"),
                    due = item.optString("due"),
                    priority = item.optString("priority", "low"),
                    tags = tags,
                    repeat = item.optString("repeat"),
                    completed = item.optBoolean("completed", item.optString("completed_at").isNotBlank()),
                    subtasks = subs
                ))
            }
        }
        return lists to tasks
    }
}

fun nextOccurrence(task: TaskItem): TaskItem {
    if (task.repeat.isBlank()) return task.copy(completed = !task.completed)
    if (task.completed) return task.copy(completed = false)
    if (task.due.isBlank()) return task.copy(completed = false)

    fun weekdays(rule: String): Set<java.time.DayOfWeek> {
        val raw = rule.substringAfter("BYDAY=", "").substringBefore(";")
        return raw.split(",").mapNotNull {
            when (it) {
                "MO" -> java.time.DayOfWeek.MONDAY
                "TU" -> java.time.DayOfWeek.TUESDAY
                "WE" -> java.time.DayOfWeek.WEDNESDAY
                "TH" -> java.time.DayOfWeek.THURSDAY
                "FR" -> java.time.DayOfWeek.FRIDAY
                "SA" -> java.time.DayOfWeek.SATURDAY
                "SU" -> java.time.DayOfWeek.SUNDAY
                else -> null
            }
        }.toSet()
    }

    fun nextWeekday(after: LocalDate, rule: String): LocalDate {
        val allowed = weekdays(rule)
        if (allowed.isEmpty()) return after.plusWeeks(1)
        return (1L..8L).map { after.plusDays(it) }.first { allowed.contains(it.dayOfWeek) }
    }

    return try {
        val original = LocalDate.parse(task.due.take(10))
        val today = LocalDate.now()
        val next = when {
            task.repeat.contains("DAILY") ->
                if (original.isBefore(today)) today.plusDays(1) else original.plusDays(1)
            task.repeat.contains("MONTHLY") -> {
                if (!original.isBefore(today)) {
                    original.plusMonths(1)
                } else {
                    val configuredDay = task.repeat.substringAfter("BYMONTHDAY=", original.dayOfMonth.toString())
                        .substringBefore(";").toIntOrNull() ?: original.dayOfMonth
                    val thisMonth = java.time.YearMonth.from(today)
                    val candidate = thisMonth.atDay(configuredDay.coerceAtMost(thisMonth.lengthOfMonth()))
                    if (candidate.isAfter(today)) candidate else {
                        val following = thisMonth.plusMonths(1)
                        following.atDay(configuredDay.coerceAtMost(following.lengthOfMonth()))
                    }
                }
            }
            task.repeat.contains("YEARLY") ->
                if (original.isBefore(today)) today.plusYears(1) else original.plusYears(1)
            else ->
                if (original.isBefore(today)) nextWeekday(today, task.repeat)
                else nextWeekday(original, task.repeat)
        }
        val suffix = if (task.due.contains("T")) "T" + task.due.substringAfter("T") else ""
        task.copy(due = next.toString() + suffix, completed = false)
    } catch (_: Exception) {
        task.copy(completed = false)
    }
}
