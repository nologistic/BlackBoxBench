package com.blackboxbench.reproduction

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

const val APP_BLUE = 0xFF4A73D8L

fun newId(): String = UUID.randomUUID().toString().replace("-", "").substring(0, 10)

inline fun JSONArray.forEachObj(action: (JSONObject) -> Unit) {
    for (i in 0 until length()) action(getJSONObject(i))
}

// ---------------- Models ----------------

class TaskList(id: String, name: String, color: Int?, icon: String?, isDefault: Boolean) {
    var id by mutableStateOf(id)
    var name by mutableStateOf(name)
    var color by mutableStateOf(color)
    var icon by mutableStateOf(icon)
    var isDefault by mutableStateOf(isDefault)

    fun toJson() = JSONObject().apply {
        put("id", id); put("name", name)
        color?.let { put("color", it) }
        icon?.let { put("icon", it) }
        put("isDefault", isDefault)
    }

    companion object {
        fun fromJson(o: JSONObject) = TaskList(
            o.getString("id"), o.getString("name"),
            if (o.has("color") && !o.isNull("color")) o.getInt("color") else null,
            if (o.has("icon") && !o.isNull("icon")) o.getString("icon") else null,
            o.optBoolean("isDefault", false)
        )
    }
}

class Tag(id: String, name: String, color: Int?, icon: String?) {
    var id by mutableStateOf(id)
    var name by mutableStateOf(name)
    var color by mutableStateOf(color)
    var icon by mutableStateOf(icon)

    fun toJson() = JSONObject().apply {
        put("id", id); put("name", name)
        color?.let { put("color", it) }
        icon?.let { put("icon", it) }
    }

    companion object {
        fun fromJson(o: JSONObject) = Tag(
            o.getString("id"), o.getString("name"),
            if (o.has("color") && !o.isNull("color")) o.getInt("color") else null,
            if (o.has("icon") && !o.isNull("icon")) o.getString("icon") else null
        )
    }
}

class SubTask(id: String, title: String, completed: Boolean, completedAt: Long?) {
    var id by mutableStateOf(id)
    var title by mutableStateOf(title)
    var completed by mutableStateOf(completed)
    var completedAt by mutableStateOf(completedAt)

    fun toJson() = JSONObject().apply {
        put("id", id); put("title", title); put("completed", completed)
        completedAt?.let { put("completedAt", it) }
    }

    companion object {
        fun fromJson(o: JSONObject) = SubTask(
            o.getString("id"), o.getString("title"), o.optBoolean("completed", false),
            if (o.has("completedAt") && !o.isNull("completedAt")) o.getLong("completedAt") else null
        )
    }
}

class Task(
    id: String, title: String, notes: String, listId: String,
    due: Long?, dueHasTime: Boolean, start: Long?, repeat: String,
    priority: Int, tagIds: List<String>, subtasks: List<SubTask>,
    completed: Boolean, completedAt: Long?, createdAt: Long, modifiedAt: Long
) {
    var id by mutableStateOf(id)
    var title by mutableStateOf(title)
    var notes by mutableStateOf(notes)
    var listId by mutableStateOf(listId)
    var due by mutableStateOf(due)
    var dueHasTime by mutableStateOf(dueHasTime)
    var start by mutableStateOf(start)
    var repeat by mutableStateOf(repeat)
    var priority by mutableStateOf(priority)
    val tagIds = mutableStateListOf<String>().apply { addAll(tagIds) }
    val subtasks = mutableStateListOf<SubTask>().apply { addAll(subtasks) }
    var completed by mutableStateOf(completed)
    var completedAt by mutableStateOf(completedAt)
    var createdAt by mutableStateOf(createdAt)
    var modifiedAt by mutableStateOf(modifiedAt)

    fun toJson() = JSONObject().apply {
        put("id", id); put("title", title); put("notes", notes); put("listId", listId)
        due?.let { put("due", it) }
        put("dueHasTime", dueHasTime)
        start?.let { put("start", it) }
        put("repeat", repeat)
        put("priority", priority)
        put("tagIds", JSONArray(tagIds))
        put("subtasks", JSONArray().apply { subtasks.forEach { put(it.toJson()) } })
        put("completed", completed)
        completedAt?.let { put("completedAt", it) }
        put("createdAt", createdAt)
        put("modifiedAt", modifiedAt)
    }

    companion object {
        fun fromJson(o: JSONObject): Task {
            val tagIds = mutableListOf<String>()
            o.optJSONArray("tagIds")?.let { arr -> for (i in 0 until arr.length()) tagIds.add(arr.getString(i)) }
            val subs = mutableListOf<SubTask>()
            o.optJSONArray("subtasks")?.forEachObj { subs.add(SubTask.fromJson(it)) }
            return Task(
                o.getString("id"), o.getString("title"), o.optString("notes", ""), o.getString("listId"),
                if (o.has("due") && !o.isNull("due")) o.getLong("due") else null,
                o.optBoolean("dueHasTime", false),
                if (o.has("start") && !o.isNull("start")) o.getLong("start") else null,
                o.optString("repeat", ""),
                o.optInt("priority", 0), tagIds, subs,
                o.optBoolean("completed", false),
                if (o.has("completedAt") && !o.isNull("completedAt")) o.getLong("completedAt") else null,
                o.optLong("createdAt", System.currentTimeMillis()),
                o.optLong("modifiedAt", System.currentTimeMillis())
            )
        }
    }
}

class CustomFilter(id: String, name: String, color: Int?, icon: String?, conditions: List<String>) {
    var id by mutableStateOf(id)
    var name by mutableStateOf(name)
    var color by mutableStateOf(color)
    var icon by mutableStateOf(icon)
    val conditions = mutableStateListOf<String>().apply { addAll(conditions) }

    fun toJson() = JSONObject().apply {
        put("id", id); put("name", name)
        color?.let { put("color", it) }
        icon?.let { put("icon", it) }
        put("conditions", JSONArray(conditions))
    }

    companion object {
        fun fromJson(o: JSONObject): CustomFilter {
            val conds = mutableListOf<String>()
            o.optJSONArray("conditions")?.let { arr -> for (i in 0 until arr.length()) conds.add(arr.getString(i)) }
            return CustomFilter(
                o.getString("id"), o.getString("name"),
                if (o.has("color") && !o.isNull("color")) o.getInt("color") else null,
                if (o.has("icon") && !o.isNull("icon")) o.getString("icon") else null,
                conds
            )
        }
    }
}

// ---------------- State store (filesDir/state.json) ----------------

object StateStore {
    val lists = mutableStateListOf<TaskList>()
    val tags = mutableStateListOf<Tag>()
    val tasks = mutableStateListOf<Task>()
    val filters = mutableStateListOf<CustomFilter>()
    var loaded by mutableStateOf(false)

    private lateinit var appContext: Context

    fun init(ctx: Context) {
        appContext = ctx.applicationContext
        load()
    }

    private fun stateFile() = File(appContext.filesDir, "state.json")

    fun load() {
        val f = stateFile()
        if (f.exists()) {
            try {
                val root = JSONObject(f.readText())
                lists.clear(); tags.clear(); tasks.clear(); filters.clear()
                root.optJSONArray("lists")?.forEachObj { lists.add(TaskList.fromJson(it)) }
                root.optJSONArray("tags")?.forEachObj { tags.add(Tag.fromJson(it)) }
                root.optJSONArray("tasks")?.forEachObj { tasks.add(Task.fromJson(it)) }
                root.optJSONArray("filters")?.forEachObj { filters.add(CustomFilter.fromJson(it)) }
            } catch (_: Exception) {
            }
        }
        loaded = true
    }

    fun save() {
        val root = JSONObject().apply {
            put("lists", JSONArray().apply { lists.forEach { put(it.toJson()) } })
            put("tags", JSONArray().apply { tags.forEach { put(it.toJson()) } })
            put("tasks", JSONArray().apply { tasks.forEach { put(it.toJson()) } })
            put("filters", JSONArray().apply { filters.forEach { put(it.toJson()) } })
        }
        try {
            stateFile().writeText(root.toString())
        } catch (_: Exception) {
        }
    }

    fun defaultList(): TaskList? = lists.firstOrNull { it.isDefault } ?: lists.firstOrNull()

    fun createEmptyDefault() {
        lists.add(TaskList(newId(), "默认清单", null, null, true))
        save()
    }

    fun importSeed(ctx: Context) {
        val root = JSONObject(AssetStore.readText(ctx, "tasks_seed.json"))
        lists.clear(); tags.clear(); tasks.clear(); filters.clear()
        root.getJSONArray("lists").forEachObj { lists.add(TaskList.fromJson(it)) }
        val palette = listOf(0xFF4A73D8, 0xFFE53935, 0xFFF9A825, 0xFF43A047, 0xFF8E24AA, 0xFF00ACC1)
        val tagIdByName = HashMap<String, String>()
        val allTasks = root.getJSONArray("tasks")
        allTasks.forEachObj { t ->
            t.optJSONArray("tags")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val name = arr.getString(i)
                    if (!tagIdByName.containsKey(name)) {
                        val id = newId()
                        tagIdByName[name] = id
                        tags.add(Tag(id, name, palette[tags.size % palette.size].toInt(), null))
                    }
                }
            }
        }
        allTasks.forEachObj { o -> tasks.add(parseSeedTask(o, tagIdByName)) }
        save()
    }

    private fun parseDate(s: String): Pair<Long?, Boolean>? {
        if (s.isBlank()) return null
        return try {
            if (s.contains("T")) {
                val d = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault()).parse(s)
                if (d != null) Pair(d.time, true) else null
            } else {
                val d = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(s)
                if (d != null) Pair(d.time, false) else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseSeedTask(o: JSONObject, tagIdByName: Map<String, String>): Task {
        val (due, dueHasTime) = parseDate(o.optString("due", "")) ?: Pair(null, false)
        val completedAt = parseDate(o.optString("completed_at", ""))?.first
        val priority = when (o.optString("priority", "")) {
            "high" -> 3; "medium" -> 2; "low" -> 1; else -> 0
        }
        val tagIds = mutableListOf<String>()
        o.optJSONArray("tags")?.let { arr -> for (i in 0 until arr.length()) tagIdByName[arr.getString(i)]?.let { tagIds.add(it) } }
        val subs = mutableListOf<SubTask>()
        o.optJSONArray("subtasks")?.forEachObj { s ->
            val ca = parseDate(s.optString("completed_at", ""))?.first
            subs.add(SubTask(s.getString("id"), s.getString("title"), ca != null, ca))
        }
        val created = (due ?: System.currentTimeMillis()) - 2L * 86400000L
        val modified = completedAt ?: (created + 86400000L)
        return Task(
            o.getString("id"), o.getString("title"), o.optString("notes", ""),
            o.getString("list"), due, dueHasTime, null, o.optString("repeat", ""),
            priority, tagIds, subs, completedAt != null, completedAt, created, modified
        )
    }
}

// ---------------- Preferences ----------------

object Prefs {
    private lateinit var sp: SharedPreferences

    fun init(ctx: Context) {
        sp = ctx.getSharedPreferences("tasks_settings", Context.MODE_PRIVATE)
    }

    fun str(k: String, d: String = ""): String = sp.getString(k, d) ?: d
    fun putStr(k: String, v: String) { sp.edit().putString(k, v).apply() }
    fun bool(k: String, d: Boolean = false): Boolean = sp.getBoolean(k, d)
    fun putBool(k: String, v: Boolean) { sp.edit().putBoolean(k, v).apply() }
    fun int(k: String, d: Int = 0): Int = sp.getInt(k, d)
    fun putInt(k: String, v: Int) { sp.edit().putInt(k, v).apply() }
}

// ---------------- Settings ----------------

object Settings {
    var theme: String
        get() = Prefs.str("theme", "system")
        set(v) = Prefs.putStr("theme", v)

    var dynamic: Boolean
        get() = Prefs.bool("dynamic")
        set(v) = Prefs.putBool("dynamic", v)

    var markdown: Boolean
        get() = Prefs.bool("markdown")
        set(v) = Prefs.putBool("markdown", v)

    var openLastView: Boolean
        get() = Prefs.bool("openLastView", true)
        set(v) = Prefs.putBool("openLastView", v)

    var fontSize: Int
        get() = Prefs.int("fontSize", 16)
        set(v) = Prefs.putInt("fontSize", v)

    var rowSpacing: Int
        get() = Prefs.int("rowSpacing", 16)
        set(v) = Prefs.putInt("rowSpacing", v)

    var showFullTitle: Boolean
        get() = Prefs.bool("showFullTitle")
        set(v) = Prefs.putBool("showFullTitle", v)

    var showDescription: Boolean
        get() = Prefs.bool("showDescription", true)
        set(v) = Prefs.putBool("showDescription", v)

    var showFullDescription: Boolean
        get() = Prefs.bool("showFullDescription")
        set(v) = Prefs.putBool("showFullDescription", v)

    var showLinks: Boolean
        get() = Prefs.bool("showLinks")
        set(v) = Prefs.putBool("showLinks", v)

    var perListSort: Boolean
        get() = Prefs.bool("perListSort")
        set(v) = Prefs.putBool("perListSort", v)

    var mergeNotifications: Boolean
        get() = Prefs.bool("mergeNotifications", true)
        set(v) = Prefs.putBool("mergeNotifications", v)

    var voiceReminders: Boolean
        get() = Prefs.bool("voiceReminders")
        set(v) = Prefs.putBool("voiceReminders", v)

    var allDayReminder: Boolean
        get() = Prefs.bool("allDayReminder", true)
        set(v) = Prefs.putBool("allDayReminder", v)

    var editorShowLinks: Boolean
        get() = Prefs.bool("editorShowLinks")
        set(v) = Prefs.putBool("editorShowLinks", v)

    var saveOnBack: Boolean
        get() = Prefs.bool("saveOnBack")
        set(v) = Prefs.putBool("saveOnBack", v)

    var multilineTitle: Boolean
        get() = Prefs.bool("multilineTitle")
        set(v) = Prefs.putBool("multilineTitle", v)

    var showComments: Boolean
        get() = Prefs.bool("showComments")
        set(v) = Prefs.putBool("showComments", v)

    var editWhenLocked: Boolean
        get() = Prefs.bool("editWhenLocked")
        set(v) = Prefs.putBool("editWhenLocked", v)

    var showFullDate: Boolean
        get() = Prefs.bool("showFullDate")
        set(v) = Prefs.putBool("showFullDate", v)

    var autoCloseList: Boolean
        get() = Prefs.bool("autoCloseList")
        set(v) = Prefs.putBool("autoCloseList", v)

    var autoCloseEdit: Boolean
        get() = Prefs.bool("autoCloseEdit")
        set(v) = Prefs.putBool("autoCloseEdit", v)

    var autoCloseWidget: Boolean
        get() = Prefs.bool("autoCloseWidget")
        set(v) = Prefs.putBool("autoCloseWidget", v)

    var drawerFiltersEnabled: Boolean
        get() = Prefs.bool("drawerFiltersEnabled", true)
        set(v) = Prefs.putBool("drawerFiltersEnabled", v)

    var drawerFilterToday: Boolean
        get() = Prefs.bool("drawerFilterToday", true)
        set(v) = Prefs.putBool("drawerFilterToday", v)

    var drawerFilterRecent: Boolean
        get() = Prefs.bool("drawerFilterRecent", true)
        set(v) = Prefs.putBool("drawerFilterRecent", v)

    var drawerTagsEnabled: Boolean
        get() = Prefs.bool("drawerTagsEnabled", true)
        set(v) = Prefs.putBool("drawerTagsEnabled", v)

    var hideUnusedTags: Boolean
        get() = Prefs.bool("hideUnusedTags")
        set(v) = Prefs.putBool("hideUnusedTags", v)

    var drawerPlacesEnabled: Boolean
        get() = Prefs.bool("drawerPlacesEnabled", true)
        set(v) = Prefs.putBool("drawerPlacesEnabled", v)

    var hideUnusedPlaces: Boolean
        get() = Prefs.bool("hideUnusedPlaces")
        set(v) = Prefs.putBool("hideUnusedPlaces", v)

    var autoBackup: Boolean
        get() = Prefs.bool("autoBackup", true)
        set(v) = Prefs.putBool("autoBackup", v)

    var gdriveBackup: Boolean
        get() = Prefs.bool("gdriveBackup")
        set(v) = Prefs.putBool("gdriveBackup", v)

    var calendarEventTime: Boolean
        get() = Prefs.bool("calendarEventTime", true)
        set(v) = Prefs.putBool("calendarEventTime", v)

    var badgesEnabled: Boolean
        get() = Prefs.bool("badgesEnabled")
        set(v) = Prefs.putBool("badgesEnabled", v)

    var defPriority: Int
        get() = Prefs.int("defPriority", 1)
        set(v) = Prefs.putInt("defPriority", v)

    var defList: String
        get() = Prefs.str("defList")
        set(v) = Prefs.putStr("defList", v)

    var defTag: String
        get() = Prefs.str("defTag")
        set(v) = Prefs.putStr("defTag", v)

    var badgeList: String
        get() = Prefs.str("badgeList", "mytasks")
        set(v) = Prefs.putStr("badgeList", v)

    var completionSound: String
        get() = Prefs.str("completionSound", "默认")
        set(v) = Prefs.putStr("completionSound", v)

    var reminderTime: String
        get() = Prefs.str("reminderTime", "18:00")
        set(v) = Prefs.putStr("reminderTime", v)

    var newTasksAtTop: Boolean
        get() = Prefs.bool("newTasksAtTop", true)
        set(v) = Prefs.putBool("newTasksAtTop", v)

    var defaultStart: String
        get() = Prefs.str("defaultStart", "无")
        set(v) = Prefs.putStr("defaultStart", v)

    var defaultDue: String
        get() = Prefs.str("defaultDue", "无")
        set(v) = Prefs.putStr("defaultDue", v)

    var defaultCalendar: String
        get() = Prefs.str("defaultCalendar", "不添加")
        set(v) = Prefs.putStr("defaultCalendar", v)

    var paperStyle: String
        get() = Prefs.str("paperStyle", "文本和图标")
        set(v) = Prefs.putStr("paperStyle", v)
}

// ---------------- Sort / grouping ----------------

enum class GroupBy(val label: String) {
    NONE("无"), DUE("按截止日期"), START("按开始日期"), PRIORITY("按优先级"),
    MODIFIED("按最后修改"), CREATED("创建时间"), LIST("清单");

    companion object {
        fun of(s: String) = entries.firstOrNull { it.name == s } ?: DUE
    }
}

enum class SortBy(val label: String) {
    DUE("截止日期"), PRIORITY("优先级"), TITLE("标题");

    companion object {
        fun of(s: String) = entries.firstOrNull { it.name == s } ?: DUE
    }
}

object SortPrefs {
    var groupBy: GroupBy
        get() = GroupBy.of(Prefs.str("groupBy", "DUE"))
        set(v) = Prefs.putStr("groupBy", v.name)

    var ascending: Boolean
        get() = Prefs.bool("ascending", true)
        set(v) = Prefs.putBool("ascending", v)

    var sortBy: SortBy
        get() = SortBy.of(Prefs.str("sortBy", "DUE"))
        set(v) = Prefs.putStr("sortBy", v.name)

    var subtaskAuto: Boolean
        get() = Prefs.bool("subtaskAuto")
        set(v) = Prefs.putBool("subtaskAuto", v)

    var showNotStarted: Boolean
        get() = Prefs.bool("showNotStarted", true)
        set(v) = Prefs.putBool("showNotStarted", v)

    var showCompleted: Boolean
        get() = Prefs.bool("showCompleted", true)
        set(v) = Prefs.putBool("showCompleted", v)

    var showCompletedSubtasks: Boolean
        get() = Prefs.bool("showCompletedSubtasks", true)
        set(v) = Prefs.putBool("showCompletedSubtasks", v)

    var completedAtBottom: Boolean
        get() = Prefs.bool("completedAtBottom", true)
        set(v) = Prefs.putBool("completedAtBottom", v)
}

// ---------------- Views ----------------

data class ViewRef(val type: String, val id: String = "") {
    fun encode(): String = if (id.isEmpty()) type else "$type:$id"

    companion object {
        val MY_TASKS = ViewRef("mytasks")
        val TODAY = ViewRef("today")
        val RECENT = ViewRef("recent")
        fun list(id: String) = ViewRef("list", id)
        fun tag(id: String) = ViewRef("tag", id)
        fun filter(id: String) = ViewRef("filter", id)

        fun parse(s: String): ViewRef {
            val idx = s.indexOf(':')
            return if (idx > 0) ViewRef(s.substring(0, idx), s.substring(idx + 1)) else ViewRef("mytasks")
        }
    }
}

fun viewTitle(view: ViewRef): String = when (view.type) {
    "mytasks" -> "我的任务"
    "today" -> "今天"
    "recent" -> "最近修改过的"
    "list" -> StateStore.lists.firstOrNull { it.id == view.id }?.name ?: "清单"
    "tag" -> StateStore.tags.firstOrNull { it.id == view.id }?.name ?: "标签"
    "filter" -> StateStore.filters.firstOrNull { it.id == view.id }?.name ?: "过滤器"
    else -> "我的任务"
}

fun tasksForView(view: ViewRef): List<Task> = when (view.type) {
    "mytasks" -> StateStore.tasks.toList()
    "today" -> StateStore.tasks.filter { !it.completed && it.due != null && dayDiff(it.due!!, System.currentTimeMillis()) <= 0 }
    "recent" -> StateStore.tasks.sortedByDescending { it.modifiedAt }
    "list" -> StateStore.tasks.filter { it.listId == view.id }
    "tag" -> StateStore.tasks.filter { it.tagIds.contains(view.id) }
    "filter" -> StateStore.tasks.filter { t -> matchesFilter(StateStore.filters.firstOrNull { it.id == view.id }, t) }
    else -> emptyList()
}

fun matchesFilter(f: CustomFilter?, t: Task): Boolean {
    if (f == null || f.conditions.isEmpty()) return false
    return f.conditions.all { matchesCond(it, t) }
}

fun matchesCond(c: String, t: Task): Boolean {
    val idx = c.indexOf('|')
    val type = if (idx >= 0) c.substring(0, idx) else c
    val arg = if (idx >= 0) c.substring(idx + 1) else ""
    val now = System.currentTimeMillis()
    return when (type) {
        "tag" -> t.tagIds.contains(arg)
        "tagname" -> StateStore.tags.any { t.tagIds.contains(it.id) && it.name.contains(arg) }
        "start" -> t.start != null && dayDiff(t.start!!, now) <= 0
        "due" -> t.due != null && dayDiff(t.due!!, now) <= 0
        "priority" -> t.priority == arg.toIntOrNull()
        "title" -> t.title.contains(arg)
        "list" -> t.listId == arg
        "repeat" -> t.repeat.isNotEmpty()
        "completed" -> t.completed
        "notstarted" -> t.start != null && t.start!! > now
        "subtasks" -> t.subtasks.isNotEmpty()
        "issubtask" -> false
        "reminder" -> false
        else -> true
    }
}

fun condLabel(c: String): String {
    val idx = c.indexOf('|')
    val type = if (idx >= 0) c.substring(0, idx) else c
    val arg = if (idx >= 0) c.substring(idx + 1) else ""
    return when (type) {
        "tag" -> "标签… " + (StateStore.tags.firstOrNull { it.id == arg }?.name ?: "")
        "tagname" -> "标签名包含… $arg"
        "start" -> "开始于…"
        "due" -> "截止于…"
        "priority" -> "优先级… ${priorityName(arg.toIntOrNull() ?: 0)}"
        "title" -> "标题含… $arg"
        "list" -> "在某清单中… " + (StateStore.lists.firstOrNull { it.id == arg }?.name ?: "")
        "repeat" -> "重复"
        "completed" -> "已完成"
        "notstarted" -> "尚未开始"
        "subtasks" -> "有子任务"
        "issubtask" -> "是子任务"
        "reminder" -> "有提醒"
        else -> c
    }
}

// ---------------- Task operations ----------------

fun completeTask(t: Task, now: Long = System.currentTimeMillis()) {
    if (t.repeat.isNotEmpty()) {
        t.due = nextRepeatDue(t.repeat, now)
        t.completed = false
        t.completedAt = null
    } else {
        t.completed = true
        t.completedAt = now
    }
    t.modifiedAt = now
    StateStore.save()
}

fun uncompleteTask(t: Task) {
    t.completed = false
    t.completedAt = null
    t.modifiedAt = System.currentTimeMillis()
    StateStore.save()
}

fun deleteTask(t: Task) {
    StateStore.tasks.remove(t)
    StateStore.save()
}

fun cloneTask(t: Task): Task {
    val copy = Task(
        newId(), t.title, t.notes, t.listId, t.due, t.dueHasTime, t.start, t.repeat,
        t.priority, t.tagIds.toList(),
        t.subtasks.map { SubTask(newId(), it.title, it.completed, it.completedAt) },
        false, null, System.currentTimeMillis(), System.currentTimeMillis()
    )
    StateStore.tasks.add(copy)
    StateStore.save()
    return copy
}

// ---------------- Date / repeat helpers ----------------

fun dayMillis(ms: Long): Long {
    val c = Calendar.getInstance().apply { timeInMillis = ms }
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

fun dayDiff(a: Long, b: Long): Int = ((dayMillis(a) - dayMillis(b)) / 86400000L).toInt()

private val hmFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
private val mdFormat = SimpleDateFormat("M月d日", Locale.getDefault())
private val fullFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

fun dueLabel(due: Long?, hasTime: Boolean): String {
    if (due == null) return ""
    val now = System.currentTimeMillis()
    val hm = if (hasTime) " " + hmFormat.format(Date(due)) else ""
    return when (dayDiff(due, now)) {
        0 -> "今天$hm"
        1 -> "明天$hm"
        else -> mdFormat.format(Date(due)) + hm
    }
}

fun fullDateLabel(ms: Long): String = fullFormat.format(Date(ms))

fun repeatLabel(r: String): String = when {
    r.isEmpty() -> "不重复"
    r == "FREQ=DAILY" -> "每天"
    r.startsWith("FREQ=WEEKLY") -> "每周"
    r.startsWith("FREQ=MONTHLY") -> "每月"
    r.startsWith("FREQ=YEARLY") -> "每年"
    else -> "自定义"
}

private fun dayConst(s: String): Int = when (s.uppercase(Locale.US)) {
    "MO" -> Calendar.MONDAY
    "TU" -> Calendar.TUESDAY
    "WE" -> Calendar.WEDNESDAY
    "TH" -> Calendar.THURSDAY
    "FR" -> Calendar.FRIDAY
    "SA" -> Calendar.SATURDAY
    "SU" -> Calendar.SUNDAY
    else -> Calendar.MONDAY
}

fun nextRepeatDue(rule: String, fromMs: Long): Long {
    val from = if (fromMs <= 0) System.currentTimeMillis() else fromMs
    val c = Calendar.getInstance().apply { timeInMillis = from }
    when {
        rule == "FREQ=DAILY" -> c.add(Calendar.DAY_OF_YEAR, 1)
        rule.startsWith("FREQ=WEEKLY") -> {
            val days = mutableSetOf<Int>()
            val idx = rule.indexOf("BYDAY=")
            if (idx >= 0) {
                rule.substring(idx + 6).split(";")[0].split(",").forEach {
                    days.add(dayConst(it.trim()))
                }
            }
            if (days.isEmpty()) {
                c.add(Calendar.WEEK_OF_YEAR, 1)
            } else {
                for (i in 1..7) {
                    c.add(Calendar.DAY_OF_YEAR, 1)
                    if (days.contains(c.get(Calendar.DAY_OF_WEEK))) break
                }
            }
        }
        rule.startsWith("FREQ=MONTHLY") -> {
            val idx = rule.indexOf("BYMONTHDAY=")
            val d = if (idx >= 0) rule.substring(idx + 11).split(";")[0].toIntOrNull() ?: 1 else 1
            c.add(Calendar.MONTH, 1)
            val max = c.getActualMaximum(Calendar.DAY_OF_MONTH)
            c.set(Calendar.DAY_OF_MONTH, minOf(d, max))
        }
        rule.startsWith("FREQ=YEARLY") -> c.add(Calendar.YEAR, 1)
        else -> c.add(Calendar.DAY_OF_YEAR, 1)
    }
    return c.timeInMillis
}

fun priorityColor(p: Int): Long = when (p) {
    3 -> 0xFFE53935L
    2 -> 0xFFF9A825L
    1 -> 0xFF4A73D8L
    else -> 0xFF9E9E9EL
}

fun priorityName(p: Int): String = when (p) {
    3 -> "高"
    2 -> "中"
    1 -> "低"
    else -> "无"
}
