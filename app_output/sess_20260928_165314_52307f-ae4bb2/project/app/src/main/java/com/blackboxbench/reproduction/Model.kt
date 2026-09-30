package com.blackboxbench.reproduction

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

enum class Priority(val label: String) {
    NONE("无"), LOW("低"), MEDIUM("中"), HIGH("高");

    companion object {
        fun from(name: String?): Priority = when (name) {
            "high" -> HIGH
            "medium" -> MEDIUM
            "low" -> LOW
            else -> NONE
        }
        fun fromInt(v: Int): Priority = entries.firstOrNull { it.ordinal == v } ?: NONE
    }
}

data class SubTask(
    val id: String,
    val title: String,
    val due: String? = null,
    val priority: Priority = Priority.NONE,
    val completedAt: String? = null,
)

data class TaskItem(
    val id: String,
    val listId: String,
    val title: String,
    val notes: String = "",
    val due: String? = null,
    val priority: Priority = Priority.NONE,
    val tags: List<String> = emptyList(),
    val repeat: String = "",
    val reminder: String? = null,
    val timer: String? = null,
    val completedAt: String? = null,
    val subtasks: List<SubTask> = emptyList(),
    val createdAt: Long = 0L,
    val order: Int = 0,
) {
    val isCompleted: Boolean get() = !completedAt.isNullOrEmpty()
    val hasDueDate: Boolean get() = !due.isNullOrEmpty()
    val hasTime: Boolean get() = due != null && due.contains("T")
}

data class TaskList(
    val id: String,
    val name: String,
    val color: Long = 0xFF009688,
    val icon: String = "list",
    val createdAt: Long = 0L,
)

data class FilterDef(
    val id: String,
    val name: String,
    val today: Boolean = false,
    val highPriority: Boolean = false,
)

data class AppData(
    val lists: List<TaskList> = emptyList(),
    val tasks: List<TaskItem> = emptyList(),
    val filters: List<FilterDef> = emptyList(),
    val hideCompleted: Boolean = false,
    val onboarded: Boolean = false,
) {
    fun listById(id: String): TaskList? = lists.firstOrNull { it.id == id }
    fun taskById(id: String): TaskItem? = tasks.firstOrNull { it.id == id }
    fun tasksOfList(listId: String): List<TaskItem> = tasks.filter { it.listId == listId }
    fun tasksWithTag(tag: String): List<TaskItem> = tasks.filter { it.tags.contains(tag) }
    val allTags: List<String> get() = tasks.flatMap { it.tags }.distinct().sortedBy { it }
    fun openCount(listId: String): Int = tasks.count { it.listId == listId && !it.isCompleted }
}

// ---------- 日期与格式工具 ----------

private val ISO_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val ISO_DATETIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
val WEEKDAY_CN = mapOf(
    DayOfWeek.MONDAY to "周一", DayOfWeek.TUESDAY to "周二", DayOfWeek.WEDNESDAY to "周三",
    DayOfWeek.THURSDAY to "周四", DayOfWeek.FRIDAY to "周五", DayOfWeek.SATURDAY to "周六",
    DayOfWeek.SUNDAY to "周日",
)

fun nowIso(): String = LocalDateTime.now().format(ISO_DATETIME)

fun parseDue(due: String?): LocalDateTime? = when {
    due.isNullOrEmpty() -> null
    due.contains("T") -> runCatching { LocalDateTime.parse(due, ISO_DATETIME) }.getOrNull()
    else -> runCatching { LocalDate.parse(due, ISO_DATE).atStartOfDay() }.getOrNull()
}

fun formatDue(due: String?, today: LocalDate = LocalDate.now()): String? {
    val dt = parseDue(due) ?: return null
    val date = dt.toLocalDate()
    val dayPart = when {
        date == today -> "今天"
        date == today.plusDays(1) -> "明天"
        date == today.minusDays(1) -> "昨天"
        date.year == today.year -> "${date.monthValue}月${date.dayOfMonth}日"
        else -> "${date.year}年${date.monthValue}月${date.dayOfMonth}日"
    }
    val timePart = if (due!!.contains("T")) " %02d:%02d".format(dt.hour, dt.minute) else ""
    return dayPart + timePart
}

fun formatCompletedAt(at: String?): String? {
    val dt = parseDue(at ?: return null) ?: return null
    return "${dt.monthValue}月${dt.dayOfMonth}日" + if (at.contains("T")) " %02d:%02d".format(dt.hour, dt.minute) else ""
}

fun dueBucket(due: String?, today: LocalDate = LocalDate.now()): Int {
    val dt = parseDue(due) ?: return 5 // 无日期
    val date = dt.toLocalDate()
    return when {
        date.isBefore(today) -> 0 // 已过期
        date == today -> 1
        date == today.plusDays(1) -> 2
        date.isBefore(today.plusWeeks(1)) -> 3 // 本周
        else -> 4
    }
}

/** 重复规则展示文案 */
fun repeatLabel(repeat: String): String = when {
    repeat.isEmpty() -> "不重复"
    repeat.contains("DAILY") -> "每天"
    repeat.contains("WEEKLY") -> "每周"
    repeat.contains("MONTHLY") -> "每月"
    repeat.contains("YEARLY") -> "每年"
    else -> "自定义"
}

/** 完成重复任务时计算下一次出现 */
fun nextOccurrence(task: TaskItem, now: LocalDate = LocalDate.now()): String? {
    val dt = parseDue(task.due) ?: return task.due
    val hasTime = task.due!!.contains("T")
    val time = dt.toLocalTime()
    var base = if (dt.toLocalDate().isBefore(now)) now else dt.toLocalDate()
    fun pack(d: LocalDate): String =
        if (hasTime) d.atTime(if (time == LocalTime.MIDNIGHT) LocalTime.of(8, 0) else time).format(ISO_DATETIME)
        else d.format(ISO_DATE)
    return when {
        task.repeat.contains("DAILY") -> pack(base.plusDays(1))
        task.repeat.contains("WEEKLY") -> {
            val days = Regex("BYDAY=([A-Z,]+)").find(task.repeat)?.groupValues?.get(1)
                ?.split(",")?.mapNotNull { n ->
                    when (n) {
                        "MO" -> DayOfWeek.MONDAY; "TU" -> DayOfWeek.TUESDAY; "WE" -> DayOfWeek.WEDNESDAY
                        "TH" -> DayOfWeek.THURSDAY; "FR" -> DayOfWeek.FRIDAY; "SA" -> DayOfWeek.SATURDAY
                        "SU" -> DayOfWeek.SUNDAY; else -> null
                    }
                } ?: listOf(DayOfWeek.MONDAY)
            var d = base.plusDays(1)
            while (!days.contains(d.dayOfWeek)) d = d.plusDays(1)
            pack(d)
        }
        task.repeat.contains("MONTHLY") -> {
            val md = Regex("BYMONTHDAY=(\\d+)").find(task.repeat)?.groupValues?.get(1)?.toIntOrNull()
                ?: base.dayOfMonth
            var m = base.plusMonths(1).withDayOfMonth(1)
            while (m.lengthOfMonth() < md) m = m.plusMonths(1)
            pack(m.withDayOfMonth(md))
        }
        task.repeat.contains("YEARLY") -> pack(base.plusYears(1))
        else -> task.due
    }
}

// ---------- JSON 序列化 ----------

object TaskJson {
    fun subToJson(s: SubTask): JSONObject = JSONObject()
        .put("id", s.id).put("title", s.title).put("due", s.due ?: "")
        .put("priority", s.priority.name.lowercase()).put("completed_at", s.completedAt ?: "")

    fun subFrom(o: JSONObject): SubTask = SubTask(
        id = o.optString("id"),
        title = o.optString("title"),
        due = o.optString("due").ifEmpty { null },
        priority = Priority.from(o.optString("priority")),
        completedAt = o.optString("completed_at").ifEmpty { null },
    )

    fun taskToJson(t: TaskItem): JSONObject = JSONObject()
        .put("id", t.id).put("list", t.listId).put("title", t.title).put("notes", t.notes)
        .put("due", t.due ?: "").put("priority", t.priority.name.lowercase())
        .put("tags", JSONArray(t.tags)).put("repeat", t.repeat)
        .put("reminder", t.reminder ?: "").put("timer", t.timer ?: "")
        .put("completed_at", t.completedAt ?: "")
        .put("subtasks", JSONArray(t.subtasks.map { subToJson(it) }))
        .put("created_at", t.createdAt).put("order", t.order)

    fun taskFrom(o: JSONObject): TaskItem = TaskItem(
        id = o.optString("id"),
        listId = o.optString("list"),
        title = o.optString("title"),
        notes = o.optString("notes"),
        due = o.optString("due").ifEmpty { null },
        priority = Priority.from(o.optString("priority")),
        tags = run {
            val arr = o.optJSONArray("tags") ?: JSONArray()
            (0 until arr.length()).map { arr.optString(it) }
        },
        repeat = o.optString("repeat"),
        reminder = o.optString("reminder").ifEmpty { null },
        timer = o.optString("timer").ifEmpty { null },
        completedAt = o.optString("completed_at").ifEmpty { null },
        subtasks = run {
            val arr = o.optJSONArray("subtasks") ?: JSONArray()
            (0 until arr.length()).map { subFrom(arr.getJSONObject(it)) }
        },
        createdAt = o.optLong("created_at"),
        order = o.optInt("order"),
    )

    fun toJson(d: AppData): JSONObject = JSONObject()
        .put("lists", JSONArray(d.lists.map {
            JSONObject().put("id", it.id).put("name", it.name)
                .put("color", it.color).put("icon", it.icon).put("created_at", it.createdAt)
        }))
        .put("tasks", JSONArray(d.tasks.map { taskToJson(it) }))
        .put("filters", JSONArray(d.filters.map {
            JSONObject().put("id", it.id).put("name", it.name)
                .put("today", it.today).put("highPriority", it.highPriority)
        }))
        .put("hide_completed", d.hideCompleted)
        .put("onboarded", d.onboarded)

    fun fromJson(o: JSONObject): AppData = AppData(
        lists = run {
            val arr = o.optJSONArray("lists") ?: JSONArray()
            (0 until arr.length()).map {
                val j = arr.getJSONObject(it)
                TaskList(
                    id = j.optString("id"), name = j.optString("name"),
                    color = if (j.has("color")) j.optLong("color") else 0xFF009688,
                    icon = j.optString("icon", "list"),
                    createdAt = j.optLong("created_at"),
                )
            }
        },
        tasks = run {
            val arr = o.optJSONArray("tasks") ?: JSONArray()
            (0 until arr.length()).map { taskFrom(arr.getJSONObject(it)) }
        },
        filters = run {
            val arr = o.optJSONArray("filters") ?: JSONArray()
            (0 until arr.length()).map {
                val j = arr.getJSONObject(it)
                FilterDef(j.optString("id"), j.optString("name"), j.optBoolean("today"), j.optBoolean("highPriority"))
            }
        },
        hideCompleted = o.optBoolean("hide_completed"),
        onboarded = o.optBoolean("onboarded"),
    )
}

/** 首次启动从 assets/tasks.json 生成种子数据，之后写入 SharedPreferences。 */
object Store {
    private const val PREFS = "tasks_store"
    private const val KEY = "data"

    fun load(context: Context): AppData {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(KEY, null)?.let {
            runCatching { return TaskJson.fromJson(JSONObject(it)) }
        }
        return seed(context)
    }

    fun save(context: Context, data: AppData) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, TaskJson.toJson(data).toString()).apply()
    }

    private fun seed(context: Context): AppData {
        val root = JSONObject(AssetStore.readText(context, "tasks.json"))
        val colors = mapOf("lst_work" to 0xFF1E88E5, "lst_life" to 0xFF43A047, "lst_shopping" to 0xFFE53935)
        val lists = run {
            val arr = root.getJSONArray("lists")
            (0 until arr.length()).map {
                val j = arr.getJSONObject(it)
                TaskList(j.optString("id"), j.optString("name"), colors[j.optString("id")] ?: 0xFF009688)
            }
        }
        val tasks = run {
            val arr = root.getJSONArray("tasks")
            (0 until arr.length()).mapIndexed { idx, i ->
                val j = arr.getJSONObject(i)
                TaskJson.taskFrom(j).copy(createdAt = 1_000L + idx, order = idx)
            }
        }
        return AppData(lists = lists, tasks = tasks)
    }
}
