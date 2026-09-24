package com.blackboxbench.reproduction

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Calendar
import java.util.Locale
import java.util.UUID

// ---------------------------------------------------------------- priority

const val PRIO_NONE = 0
const val PRIO_LOW = 1
const val PRIO_MED = 2
const val PRIO_HIGH = 3

fun priorityColor(p: Int): Color = when (p) {
    PRIO_HIGH -> Color(0xFFE53935)
    PRIO_MED -> Color(0xFFFBC02D)
    PRIO_LOW -> Color(0xFF2196F3)
    else -> Color(0xFF9E9E9E)
}

fun priorityName(p: Int): String = when (p) {
    PRIO_HIGH -> "高优先级"
    PRIO_MED -> "中优先级"
    PRIO_LOW -> "低优先级"
    else -> "无优先级"
}

// ---------------------------------------------------------------- model

class SubTask(
    var id: String = UUID.randomUUID().toString(),
    var title: String = "",
    var done: Boolean = false,
    var completedAt: Long? = null
)

class Task(
    var id: String = UUID.randomUUID().toString(),
    var title: String = "",
    var notes: String = "",
    var priority: Int = PRIO_LOW,
    var startAt: Long? = null,
    var dueAt: Long? = null,
    var hasDueTime: Boolean = false,
    var repeatRule: String? = null,          // null | daily | weekly | monthly | yearly
    var repeatFromDue: Boolean = true,
    var listId: String = "",
    val tagIds: MutableList<String> = mutableListOf(),
    val subtasks: MutableList<SubTask> = mutableListOf(),
    var reminder: Boolean = false,
    var location: String? = null,
    var attachments: Int = 0,
    var completed: Boolean = false,
    var completedAt: Long? = null,
    var createdAt: Long = System.currentTimeMillis(),
    var modifiedAt: Long = System.currentTimeMillis()
) {
    fun copyTask(): Task {
        val t = Task(
            title = title, notes = notes, priority = priority, startAt = startAt,
            dueAt = dueAt, hasDueTime = hasDueTime, repeatRule = repeatRule,
            repeatFromDue = repeatFromDue, listId = listId, reminder = reminder,
            location = location, attachments = attachments
        )
        t.tagIds.addAll(tagIds)
        subtasks.forEach { t.subtasks.add(SubTask(title = it.title, done = it.done, completedAt = it.completedAt)) }
        return t
    }

    val openSubtaskCount: Int get() = subtasks.count { !it.done }
    val isRepeating: Boolean get() = repeatRule != null
}

class TaskList(
    var id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var color: Long = 0xFF3F51B5,
    var icon: String = "shopping"
)

class Tag(
    var id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var color: Long = 0xFF3F51B5,
    var icon: String = "tag"
)

class FilterCondition(
    var type: String = "priority",
    var value: String = ""
)

class TaskFilter(
    var id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var color: Long = 0xFF3F51B5,
    var icon: String = "filter",
    val conditions: MutableList<FilterCondition> = mutableListOf()
)

class AppSettings {
    var theme: String = "系统默认"
    var dynamicColor: Boolean = true
    var markdown: Boolean = true
    var openLastList: Boolean = true
    var showUnstarted: Boolean = true
    var showCompleted: Boolean = true
    var showCompletedSubtasks: Boolean = true
    var moveCompletedToBottom: Boolean = true
    var groupBy: String = "无"
    var sortBy: String = "按截止日期"
    var subtaskOrder: String = "我的顺序"
    var completedSort: String = "按完成时间"
    var defaultPriority: Int = PRIO_LOW
    var defaultListId: String = ""
    var mergeNotifications: Boolean = true
    var voiceReminders: Boolean = false
    var allDayDefaultReminder: Boolean = true
    var reminderTime: String = "18:00"
    var playSound: Boolean = true
    var autoBackup: Boolean = true
    var backupDrive: Boolean = false
    var androidBackup: Boolean = true
    var lastBackup: String = "未备份"
    var showFullDate: Boolean = false
    var autoClosePicker: Boolean = false
    var customEditScreen: Boolean = false
    var editShowLinks: Boolean = true
    var saveOnBack: Boolean = true
    var multiLineTitle: Boolean = true
    var showNotesInEdit: Boolean = true
    var unlockToEdit: Boolean = false
    var drawerFilters: Boolean = true
    var drawerTags: Boolean = true
    var drawerPlaces: Boolean = true
    var drawerLists: Boolean = true
    var lastViewKey: String = "my"
}

// ---------------------------------------------------------------- store

object Store {
    private lateinit var dir: File
    val tasks = mutableStateListOf<Task>()
    val lists = mutableStateListOf<TaskList>()
    val tags = mutableStateListOf<Tag>()
    val filters = mutableStateListOf<TaskFilter>()
    val settings = AppSettings()
    var loaded by mutableStateOf(false)
    var seeding by mutableStateOf(true)
    var justSeeded by mutableStateOf(false)

    /** Bumped on every persisted mutation so Compose re-reads the plain (non-observable) model. */
    var revision by mutableStateOf(0)

    private val file: File get() = File(dir, "tasks_state.json")

    fun init(context: Context) {
        if (loaded) return
        dir = context.filesDir
        if (!load()) {
            seedFromAssets(context)
            save()
            justSeeded = true
        }
        loaded = true
        seeding = false
    }

    // ------------------------------------------------------------ seed

    private fun seedFromAssets(context: Context) {
        lists.clear(); tags.clear(); tasks.clear(); filters.clear()
        val txt = try {
            context.assets.open("tasks.json").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            ""
        }
        if (txt.isBlank()) {
            lists.add(TaskList(id = "lst_default", name = "默认清单", color = 0xFF3F51B5))
            settings.defaultListId = "lst_default"
            return
        }
        val root = JSONObject(txt)
        val listArr = root.optJSONArray("lists") ?: JSONArray()
        for (i in 0 until listArr.length()) {
            val o = listArr.getJSONObject(i)
            lists.add(TaskList(id = o.getString("id"), name = o.getString("name"), color = listColorFor(i), icon = listIconFor(i)))
        }
        settings.defaultListId = lists.firstOrNull()?.id ?: ""
        val tagArrNames = mutableSetOf<String>()
        val taskArr = root.optJSONArray("tasks") ?: JSONArray()
        for (i in 0 until taskArr.length()) {
            val o = taskArr.getJSONObject(i)
            val t = Task(
                id = o.optString("id", UUID.randomUUID().toString()),
                title = o.optString("title"),
                notes = o.optString("notes", ""),
                priority = when (o.optString("priority", "low")) {
                    "high" -> PRIO_HIGH
                    "medium" -> PRIO_MED
                    "low" -> PRIO_LOW
                    else -> PRIO_NONE
                },
                listId = o.optString("list", lists.firstOrNull()?.id ?: "")
            )
            parseDateTime(o.optString("due", ""))?.let { t.dueAt = it; t.hasDueTime = o.optString("due").contains("T") }
            val rep = o.optString("repeat", "")
            t.repeatRule = when {
                rep.startsWith("FREQ=DAILY") -> "daily"
                rep.startsWith("FREQ=WEEKLY") -> "weekly"
                rep.startsWith("FREQ=MONTHLY") -> "monthly"
                rep.startsWith("FREQ=YEARLY") -> "yearly"
                else -> null
            }
            val completedAt = o.optString("completed_at", "")
            if (completedAt.isNotBlank()) { t.completed = true; t.completedAt = parseDateTime(completedAt) }
            val stArr = o.optJSONArray("subtasks") ?: JSONArray()
            for (j in 0 until stArr.length()) {
                val s = stArr.getJSONObject(j)
                val sub = SubTask(
                    id = s.optString("id", UUID.randomUUID().toString()),
                    title = s.optString("title"),
                    done = s.optString("completed_at", "").isNotBlank()
                )
                sub.completedAt = parseDateTime(s.optString("completed_at", ""))
                t.subtasks.add(sub)
            }
            val tagArr = o.optJSONArray("tags") ?: JSONArray()
            for (j in 0 until tagArr.length()) {
                val name = tagArr.getString(j)
                tagArrNames.add(name)
            }
            t.createdAt += i * 1000L
            t.modifiedAt = t.createdAt
            tasks.add(t)
            // remember raw tag names on a side map
            rawTagNames[t.id] = (0 until tagArr.length()).map { tagArr.getString(it) }
        }
        tagArrNames.sorted().forEachIndexed { idx, name ->
            tags.add(Tag(id = "tag_" + name, name = name, color = tagColorFor(idx)))
        }
        tasks.forEach { t ->
            rawTagNames[t.id]?.forEach { n -> t.tagIds.add("tag_" + n) }
        }
        rawTagNames.clear()
    }

    private val rawTagNames = mutableMapOf<String, List<String>>()

    private fun listIconFor(i: Int): String = when (i % 3) {
        0 -> "list"
        1 -> "home"
        else -> "shopping"
    }

    private fun listColorFor(i: Int): Long = when (i % 6) {
        0 -> 0xFF2E7D32
        1 -> 0xFF1565C0
        2 -> 0xFFEF6C00
        3 -> 0xFF6A1B9A
        4 -> 0xFF00838F
        else -> 0xFFAD1457
    }

    private fun tagColorFor(i: Int): Long = when (i % 6) {
        0 -> 0xFF5E35B1
        1 -> 0xFF00897B
        2 -> 0xFFD81B60
        3 -> 0xFF3949AB
        4 -> 0xFF6D4C41
        else -> 0xFF00838F
    }

    private fun parseDateTime(s: String): Long? {
        if (s.isBlank()) return null
        return try {
            val fmt = if (s.contains("T")) "yyyy-MM-dd'T'HH:mm" else "yyyy-MM-dd"
            val simple = java.text.SimpleDateFormat(fmt, Locale.US)
            simple.isLenient = true
            simple.parse(s)?.time
        } catch (e: Exception) {
            null
        }
    }

    // ------------------------------------------------------------ persistence

    fun save() {
        revision++
        if (!::dir.isInitialized) return
        try {
            val root = JSONObject()
            root.put("settings", settingsToJson())
            val la = JSONArray()
            lists.forEach { l ->
                la.put(JSONObject().put("id", l.id).put("name", l.name).put("color", l.color).put("icon", l.icon))
            }
            root.put("lists", la)
            val ta = JSONArray()
            tags.forEach { t ->
                ta.put(JSONObject().put("id", t.id).put("name", t.name).put("color", t.color).put("icon", t.icon))
            }
            root.put("tags", ta)
            val fa = JSONArray()
            filters.forEach { f ->
                val ca = JSONArray()
                f.conditions.forEach { c -> ca.put(JSONObject().put("type", c.type).put("value", c.value)) }
                fa.put(JSONObject().put("id", f.id).put("name", f.name).put("color", f.color).put("icon", f.icon).put("conditions", ca))
            }
            root.put("filters", fa)
            val tka = JSONArray()
            tasks.forEach { t ->
                val o = JSONObject()
                o.put("id", t.id); o.put("title", t.title); o.put("notes", t.notes)
                o.put("priority", t.priority); o.put("listId", t.listId)
                o.put("startAt", t.startAt ?: -1); o.put("dueAt", t.dueAt ?: -1)
                o.put("hasDueTime", t.hasDueTime); o.put("repeatRule", t.repeatRule ?: "")
                o.put("repeatFromDue", t.repeatFromDue); o.put("reminder", t.reminder)
                o.put("location", t.location ?: ""); o.put("attachments", t.attachments)
                o.put("completed", t.completed); o.put("completedAt", t.completedAt ?: -1)
                o.put("createdAt", t.createdAt); o.put("modifiedAt", t.modifiedAt)
                o.put("tagIds", JSONArray(t.tagIds.toList()))
                val sa = JSONArray()
                t.subtasks.forEach { s ->
                    sa.put(JSONObject().put("id", s.id).put("title", s.title).put("done", s.done).put("completedAt", s.completedAt ?: -1))
                }
                o.put("subtasks", sa)
                tka.put(o)
            }
            root.put("tasks", tka)
            file.writeText(root.toString())
        } catch (e: Exception) {
            // keep running even if persistence fails
        }
    }

    private fun settingsToJson(): JSONObject {
        val s = settings
        return JSONObject()
            .put("theme", s.theme).put("dynamicColor", s.dynamicColor).put("markdown", s.markdown)
            .put("openLastList", s.openLastList).put("showUnstarted", s.showUnstarted)
            .put("showCompleted", s.showCompleted).put("showCompletedSubtasks", s.showCompletedSubtasks)
            .put("moveCompletedToBottom", s.moveCompletedToBottom).put("groupBy", s.groupBy)
            .put("sortBy", s.sortBy).put("subtaskOrder", s.subtaskOrder).put("completedSort", s.completedSort)
            .put("defaultPriority", s.defaultPriority).put("defaultListId", s.defaultListId)
            .put("mergeNotifications", s.mergeNotifications).put("voiceReminders", s.voiceReminders)
            .put("allDayDefaultReminder", s.allDayDefaultReminder).put("reminderTime", s.reminderTime)
            .put("playSound", s.playSound).put("autoBackup", s.autoBackup).put("backupDrive", s.backupDrive)
            .put("androidBackup", s.androidBackup).put("lastBackup", s.lastBackup)
            .put("showFullDate", s.showFullDate).put("autoClosePicker", s.autoClosePicker)
            .put("customEditScreen", s.customEditScreen).put("editShowLinks", s.editShowLinks)
            .put("saveOnBack", s.saveOnBack).put("multiLineTitle", s.multiLineTitle)
            .put("showNotesInEdit", s.showNotesInEdit).put("unlockToEdit", s.unlockToEdit)
            .put("drawerFilters", s.drawerFilters).put("drawerTags", s.drawerTags)
            .put("drawerPlaces", s.drawerPlaces).put("drawerLists", s.drawerLists)
            .put("lastViewKey", s.lastViewKey)
    }

    private fun load(): Boolean {
        if (!file.exists()) return false
        return try {
            val root = JSONObject(file.readText())
            root.optJSONObject("settings")?.let { o ->
                val s = settings
                s.theme = o.optString("theme", s.theme)
                s.dynamicColor = o.optBoolean("dynamicColor", s.dynamicColor)
                s.markdown = o.optBoolean("markdown", s.markdown)
                s.openLastList = o.optBoolean("openLastList", s.openLastList)
                s.showUnstarted = o.optBoolean("showUnstarted", s.showUnstarted)
                s.showCompleted = o.optBoolean("showCompleted", s.showCompleted)
                s.showCompletedSubtasks = o.optBoolean("showCompletedSubtasks", s.showCompletedSubtasks)
                s.moveCompletedToBottom = o.optBoolean("moveCompletedToBottom", s.moveCompletedToBottom)
                s.groupBy = o.optString("groupBy", s.groupBy)
                s.sortBy = o.optString("sortBy", s.sortBy)
                s.subtaskOrder = o.optString("subtaskOrder", s.subtaskOrder)
                s.completedSort = o.optString("completedSort", s.completedSort)
                s.defaultPriority = o.optInt("defaultPriority", s.defaultPriority)
                s.defaultListId = o.optString("defaultListId", s.defaultListId)
                s.mergeNotifications = o.optBoolean("mergeNotifications", s.mergeNotifications)
                s.voiceReminders = o.optBoolean("voiceReminders", s.voiceReminders)
                s.allDayDefaultReminder = o.optBoolean("allDayDefaultReminder", s.allDayDefaultReminder)
                s.reminderTime = o.optString("reminderTime", s.reminderTime)
                s.playSound = o.optBoolean("playSound", s.playSound)
                s.autoBackup = o.optBoolean("autoBackup", s.autoBackup)
                s.backupDrive = o.optBoolean("backupDrive", s.backupDrive)
                s.androidBackup = o.optBoolean("androidBackup", s.androidBackup)
                s.lastBackup = o.optString("lastBackup", s.lastBackup)
                s.showFullDate = o.optBoolean("showFullDate", s.showFullDate)
                s.autoClosePicker = o.optBoolean("autoClosePicker", s.autoClosePicker)
                s.customEditScreen = o.optBoolean("customEditScreen", s.customEditScreen)
                s.editShowLinks = o.optBoolean("editShowLinks", s.editShowLinks)
                s.saveOnBack = o.optBoolean("saveOnBack", s.saveOnBack)
                s.multiLineTitle = o.optBoolean("multiLineTitle", s.multiLineTitle)
                s.showNotesInEdit = o.optBoolean("showNotesInEdit", s.showNotesInEdit)
                s.unlockToEdit = o.optBoolean("unlockToEdit", s.unlockToEdit)
                s.drawerFilters = o.optBoolean("drawerFilters", s.drawerFilters)
                s.drawerTags = o.optBoolean("drawerTags", s.drawerTags)
                s.drawerPlaces = o.optBoolean("drawerPlaces", s.drawerPlaces)
                s.drawerLists = o.optBoolean("drawerLists", s.drawerLists)
                s.lastViewKey = o.optString("lastViewKey", s.lastViewKey)
            }
            val la = root.optJSONArray("lists") ?: JSONArray()
            for (i in 0 until la.length()) {
                val o = la.getJSONObject(i)
                lists.add(TaskList(o.getString("id"), o.getString("name"), o.getLong("color"), o.optString("icon", "shopping")))
            }
            val ta = root.optJSONArray("tags") ?: JSONArray()
            for (i in 0 until ta.length()) {
                val o = ta.getJSONObject(i)
                tags.add(Tag(o.getString("id"), o.getString("name"), o.getLong("color"), o.optString("icon", "tag")))
            }
            val fa = root.optJSONArray("filters") ?: JSONArray()
            for (i in 0 until fa.length()) {
                val o = fa.getJSONObject(i)
                val f = TaskFilter(o.getString("id"), o.getString("name"), o.getLong("color"), o.optString("icon", "filter"))
                val ca = o.optJSONArray("conditions") ?: JSONArray()
                for (j in 0 until ca.length()) {
                    val c = ca.getJSONObject(j)
                    f.conditions.add(FilterCondition(c.optString("type"), c.optString("value")))
                }
                filters.add(f)
            }
            val tka = root.optJSONArray("tasks") ?: JSONArray()
            for (i in 0 until tka.length()) {
                val o = tka.getJSONObject(i)
                val t = Task(
                    id = o.getString("id"),
                    title = o.optString("title"),
                    notes = o.optString("notes", ""),
                    priority = o.optInt("priority", PRIO_LOW),
                    listId = o.optString("listId", ""),
                    hasDueTime = o.optBoolean("hasDueTime", false),
                    repeatFromDue = o.optBoolean("repeatFromDue", true),
                    reminder = o.optBoolean("reminder", false),
                    attachments = o.optInt("attachments", 0),
                    completed = o.optBoolean("completed", false)
                )
                val rep = o.optString("repeatRule", "")
                t.repeatRule = if (rep.isBlank()) null else rep
                val sa = o.optLong("startAt", -1L); if (sa >= 0) t.startAt = sa
                val da = o.optLong("dueAt", -1L); if (da >= 0) t.dueAt = da
                val ca = o.optLong("completedAt", -1L); if (ca >= 0) t.completedAt = ca
                t.location = o.optString("location", "").ifBlank { null }
                t.createdAt = o.optLong("createdAt", System.currentTimeMillis())
                t.modifiedAt = o.optLong("modifiedAt", t.createdAt)
                val tagArr = o.optJSONArray("tagIds") ?: JSONArray()
                for (j in 0 until tagArr.length()) t.tagIds.add(tagArr.getString(j))
                val stArr = o.optJSONArray("subtasks") ?: JSONArray()
                for (j in 0 until stArr.length()) {
                    val s = stArr.getJSONObject(j)
                    val sub = SubTask(s.optString("id"), s.optString("title"), s.optBoolean("done", false))
                    val sca = s.optLong("completedAt", -1L); if (sca >= 0) sub.completedAt = sca
                    t.subtasks.add(sub)
                }
                tasks.add(t)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    // ------------------------------------------------------------ mutations

    fun taskById(id: String?): Task? = tasks.firstOrNull { it.id == id }
    fun listById(id: String?): TaskList? = lists.firstOrNull { it.id == id }
    fun tagById(id: String?): Tag? = tags.firstOrNull { it.id == id }
    fun filterById(id: String?): TaskFilter? = filters.firstOrNull { it.id == id }
    fun listName(id: String): String = listById(id)?.name ?: ""
    fun listColor(id: String): Color = Color(listById(id)?.color ?: 0xFF3F51B5)

    fun addTask(t: Task) { tasks.add(t); save() }
    fun removeTasks(ids: Collection<String>) { tasks.removeAll { ids.contains(it.id) }; save() }
    fun touch(t: Task) { t.modifiedAt = System.currentTimeMillis(); revision++; save() }

    fun addList(name: String, color: Long, icon: String = "shopping"): TaskList {
        val l = TaskList(name = name, color = color, icon = icon)
        lists.add(l); save(); return l
    }

    fun addTag(name: String, color: Long, icon: String = "tag"): Tag {
        val t = Tag(name = name, color = color, icon = icon)
        tags.add(t); save(); return t
    }

    fun addFilter(f: TaskFilter) { filters.add(f); save() }

    /** Completing a task: repeating tasks roll forward to the next occurrence. */
    fun completeTask(t: Task) {
        if (t.isRepeating) {
            advance(t, System.currentTimeMillis())
            t.subtasks.forEach { it.done = false; it.completedAt = null }
            t.completed = false
            t.completedAt = System.currentTimeMillis()
        } else {
            t.completed = true
            t.completedAt = System.currentTimeMillis()
        }
        touch(t)
    }

    fun uncompleteTask(t: Task) {
        t.completed = false
        t.completedAt = null
        touch(t)
    }

    fun completeSubtask(t: Task, s: SubTask) {
        s.done = true
        s.completedAt = System.currentTimeMillis()
        touch(t)
    }

    fun uncompleteSubtask(t: Task, s: SubTask) {
        s.done = false
        s.completedAt = null
        touch(t)
    }

    private fun advance(t: Task, from: Long) {
        val base = if (t.repeatFromDue && t.dueAt != null) t.dueAt!! else from
        val cal = Calendar.getInstance().apply { timeInMillis = base }
        when (t.repeatRule) {
            "daily" -> cal.add(Calendar.DAY_OF_YEAR, 1)
            "weekly" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            "monthly" -> cal.add(Calendar.MONTH, 1)
            "yearly" -> cal.add(Calendar.YEAR, 1)
            else -> cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        var next = cal.timeInMillis
        if (next <= System.currentTimeMillis()) {
            while (next <= System.currentTimeMillis()) {
                val c2 = Calendar.getInstance().apply { timeInMillis = next }
                when (t.repeatRule) {
                    "daily" -> c2.add(Calendar.DAY_OF_YEAR, 1)
                    "weekly" -> c2.add(Calendar.WEEK_OF_YEAR, 1)
                    "monthly" -> c2.add(Calendar.MONTH, 1)
                    "yearly" -> c2.add(Calendar.YEAR, 1)
                    else -> c2.add(Calendar.DAY_OF_YEAR, 1)
                }
                next = c2.timeInMillis
            }
        }
        t.dueAt = next
        t.startAt = t.startAt?.let { st ->
            val delta = (t.dueAt ?: next) - base
            st + delta
        }
    }

    fun duplicate(t: Task) {
        val c = t.copyTask()
        c.createdAt = System.currentTimeMillis()
        c.modifiedAt = c.createdAt
        tasks.add(c)
        save()
    }

    fun reset() {
        file.delete()
        tasks.clear(); lists.clear(); tags.clear(); filters.clear()
        loaded = false
    }
}

// ---------------------------------------------------------------- date helpers

fun calOf(ms: Long): Calendar = Calendar.getInstance().apply { timeInMillis = ms }

fun startOfDay(ms: Long): Long = calOf(ms).apply {
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

fun isSameDay(a: Long, b: Long): Boolean = startOfDay(a) == startOfDay(b)

fun dayDiff(from: Long, to: Long): Int {
    val d = (startOfDay(to) - startOfDay(from)) / 86400000L
    return d.toInt()
}

fun timeText(ms: Long): String = java.text.SimpleDateFormat("HH:mm", Locale.US).format(java.util.Date(ms))

fun weekdayText(ms: Long): String {
    val names = arrayOf("星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六")
    return names[calOf(ms).get(Calendar.DAY_OF_WEEK) - 1]
}

/** Due-date label: today shows only the time, tomorrow is prefixed, later dates use month/day. */
fun dueLabel(ms: Long, hasTime: Boolean, full: Boolean): String {
    val now = System.currentTimeMillis()
    val d = dayDiff(now, ms)
    val date = when {
        d == 0 -> if (full) "今天" else ""
        d == 1 -> "明天"
        d == -1 -> "昨天"
        d in 2..6 -> weekdayText(ms)
        else -> "${calOf(ms).get(Calendar.MONTH) + 1}月${calOf(ms).get(Calendar.DAY_OF_MONTH)}日"
    }
    val t = if (hasTime) timeText(ms) else ""
    return listOf(date, t).filter { it.isNotBlank() }.joinToString(" ")
}

fun startLabel(ms: Long): String {
    val now = System.currentTimeMillis()
    val d = dayDiff(now, ms)
    return when {
        d == 0 -> "今天"
        d == 1 -> "明天"
        d == -1 -> "昨天"
        d in 2..6 -> weekdayText(ms)
        else -> "${calOf(ms).get(Calendar.MONTH) + 1}月${calOf(ms).get(Calendar.DAY_OF_MONTH)}日"
    }
}

fun dateTimeLabel(ms: Long): String =
    "${calOf(ms).get(Calendar.MONTH) + 1}月${calOf(ms).get(Calendar.DAY_OF_MONTH)}日 ${timeText(ms)}"

fun repeatName(rule: String?): String = when (rule) {
    "daily" -> "每天"
    "weekly" -> "每周"
    "monthly" -> "每月"
    "yearly" -> "每年"
    else -> ""
}
