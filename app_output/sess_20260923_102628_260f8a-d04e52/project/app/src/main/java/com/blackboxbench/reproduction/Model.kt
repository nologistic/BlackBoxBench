package com.blackboxbench.reproduction

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// ---------------------------------------------------------------- date helpers

object Dates {
    val zone: ZoneId get() = ZoneId.systemDefault()

    fun today(): LocalDate = LocalDate.now(zone)

    fun toMillis(dt: LocalDateTime): Long = dt.atZone(zone).toInstant().toEpochMilli()

    fun fromMillis(millis: Long): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), zone)

    private val weekdays = arrayOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")

    fun weekday(d: LocalDate): String = weekdays[d.dayOfWeek.value - 1]

    /** e.g. 今天 / 明天 / 9月30日星期三 */
    fun dayLabel(d: LocalDate): String {
        val today = today()
        return when (d) {
            today -> "今天"
            today.plusDays(1) -> "明天"
            today.minusDays(1) -> "昨天"
            else -> "${d.monthValue}月${d.dayOfMonth}日${weekday(d)}"
        }
    }

    fun timeLabel(dt: LocalDateTime): String =
        "%02d:%02d".format(dt.hour, dt.minute)

    fun dateTimeLabel(dt: LocalDateTime): String =
        if (dt.hour == 0 && dt.minute == 0) dayLabel(dt.toLocalDate())
        else "${dayLabel(dt.toLocalDate())} ${timeLabel(dt)}"

    fun groupLabel(due: LocalDateTime?): String {
        if (due == null) return "无截止日期"
        val d = due.toLocalDate()
        return when {
            d.isBefore(today()) -> "${dayLabel(d)}截止（已过期）"
            else -> "${dayLabel(d)}截止"
        }
    }

    fun monthTitle(d: LocalDate): String = "${d.year}年${d.monthValue}月"
}

// ---------------------------------------------------------------- model

class Subtask(
    val id: String,
    title: String,
    completed: Boolean = false,
    priority: Int = 0,
    dueMillis: Long? = null,
) {
    var title by mutableStateOf(title)
    var completed by mutableStateOf(completed)
    var priority by mutableStateOf(priority)
    var dueMillis by mutableStateOf(dueMillis)

    fun dup(newId: String = id): Subtask = Subtask(newId, title, completed, priority, dueMillis)
}

const val START_NONE = 0
const val START_ABS = 1
const val START_DUE_MINUS = 2

const val REPEAT_NONE = ""
const val REPEAT_DAILY = "DAILY"
const val REPEAT_WEEKLY = "WEEKLY"
const val REPEAT_MONTHLY = "MONTHLY"
const val REPEAT_YEARLY = "YEARLY"

class TaskItem(
    val id: String,
    listId: String,
    title: String,
    notes: String = "",
    dueMillis: Long? = null,
    startKind: Int = START_NONE,
    startMillis: Long? = null,
    startOffsetDays: Int = 0,
    repeat: String = REPEAT_NONE,
    repeatFromDue: Boolean = false,
    priority: Int = 0,
    completedMillis: Long? = null,
    timerSeconds: Long = 0L,
    location: String? = null,
    order: Int = 0,
    modifiedAt: Long = System.currentTimeMillis(),
) {
    var listId by mutableStateOf(listId)
    var title by mutableStateOf(title)
    var notes by mutableStateOf(notes)
    var dueMillis by mutableStateOf(dueMillis)
    var startKind by mutableStateOf(startKind)
    var startMillis by mutableStateOf(startMillis)
    var startOffsetDays by mutableStateOf(startOffsetDays)
    var repeat by mutableStateOf(repeat)
    var repeatFromDue by mutableStateOf(repeatFromDue)
    var priority by mutableStateOf(priority)
    var completedMillis by mutableStateOf(completedMillis)
    var timerSeconds by mutableStateOf(timerSeconds)
    var location by mutableStateOf(location)
    var order by mutableStateOf(order)
    var modifiedAt by mutableStateOf(modifiedAt)
    val tags = mutableStateListOf<String>()
    val subtasks = mutableStateListOf<Subtask>()

    val completed: Boolean get() = completedMillis != null

    fun duplicate(newId: String, newTitle: String): TaskItem {
        val c = TaskItem(
            newId, listId, newTitle, notes, dueMillis, startKind, startMillis,
            startOffsetDays, repeat, repeatFromDue, priority, completedMillis,
            timerSeconds, location, order, System.currentTimeMillis()
        )
        c.tags.addAll(tags)
        subtasks.forEach { c.subtasks.add(it.dup(it.id + "_c")) }
        return c
    }

    fun due(): LocalDateTime? = dueMillis?.let { Dates.fromMillis(it) }

    fun start(): LocalDateTime? = when (startKind) {
        START_ABS -> startMillis?.let { Dates.fromMillis(it) }
        START_DUE_MINUS -> due()?.minusDays(startOffsetDays.toLong())
        else -> null
    }

    fun repeatLabel(): String = when (repeat) {
        REPEAT_DAILY -> "每天"
        REPEAT_WEEKLY -> "每周"
        REPEAT_MONTHLY -> "每月"
        REPEAT_YEARLY -> "每年"
        else -> "不重复"
    }

    fun dueSummary(): String {
        val d = due() ?: return "无截止日期"
        return Dates.dateTimeLabel(d)
    }

    fun startSummary(): String = when (startKind) {
        START_ABS -> start()?.let { Dates.dayLabel(it.toLocalDate()) } ?: "无开始日期"
        START_DUE_MINUS -> if (startOffsetDays >= 7) "截止日期前一周" else "截止日期前一天"
        else -> "无开始日期"
    }

    /** Advance a repeating task to its next occurrence, based on the repeat rule. */
    fun advanceRepeat() {
        val base = if (repeatFromDue) due() else LocalDateTime.now()
        val next: LocalDateTime? = when (repeat) {
            REPEAT_DAILY -> (base ?: LocalDateTime.now()).plusDays(1)
            REPEAT_WEEKLY -> (base ?: LocalDateTime.now()).plusWeeks(1)
            REPEAT_MONTHLY -> (base ?: LocalDateTime.now()).plusMonths(1)
            REPEAT_YEARLY -> (base ?: LocalDateTime.now()).plusYears(1)
            else -> null
        }
        if (next != null) {
            dueMillis = Dates.toMillis(next)
            startMillis = null
            startKind = START_NONE
            modifiedAt = System.currentTimeMillis()
        }
    }
}

class TaskList(
    val id: String,
    name: String,
    color: Long = 0xFF1E88E5,
    icon: String = "list",
) {
    var name by mutableStateOf(name)
    var color by mutableStateOf(color)
    var icon by mutableStateOf(icon)
}

class FilterDef(
    val id: String,
    name: String,
    color: Long = 0xFF1E88E5,
    icon: String = "filter",
    minPriority: Int = -1, // -1 = any
    builtin: Boolean = false,
) {
    var name by mutableStateOf(name)
    var color by mutableStateOf(color)
    var icon by mutableStateOf(icon)
    var minPriority by mutableStateOf(minPriority)
    var builtin by mutableStateOf(builtin)
}

// ---------------------------------------------------------------- store

object Store {
    val lists = mutableStateListOf<TaskList>()
    val tasks = mutableStateListOf<TaskItem>()
    val filters = mutableStateListOf<FilterDef>()
    val tags = mutableStateListOf<String>()

    var onboarded by mutableStateOf(false)
    var lastViewId by mutableStateOf("mytasks")
    var currentViewId by mutableStateOf("mytasks")
    var showCompleted by mutableStateOf(true)
    var groupBy by mutableStateOf("due")
    var sortBy by mutableStateOf("due")
    var sortAscending by mutableStateOf(true)
    var remindersBannerDismissed by mutableStateOf(false)
    var filterSeq by mutableStateOf(0)
    var listSeq by mutableStateOf(0)
    var taskSeq by mutableStateOf(0)

    private const val PREFS = "tasks_repro"
    private const val KEY = "state"
    private const val SCHEMA = 2

    private lateinit var appContext: Context

    private fun clearAll() {
        lists.clear(); tasks.clear(); filters.clear(); tags.clear()
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        if (raw != null) {
            val usable = runCatching {
                if (JSONObject(raw).optInt("schema", 0) != SCHEMA) error("unsupported schema")
                clearAll()
                load(raw)
                val taskIds = tasks.map { it.id }
                val listIds = lists.map { it.id }
                lists.isNotEmpty() && taskIds.distinct().size == taskIds.size &&
                    listIds.distinct().size == listIds.size
            }.getOrDefault(false)
            if (usable) return
        }
        clearAll()
        onboarded = false
        lastViewId = "mytasks"
        currentViewId = "mytasks"
        remindersBannerDismissed = false
        seed()
        save()
    }

    fun save() {
        if (!::appContext.isInitialized) return
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, dump()).apply()
    }

    fun resetToSeed() {
        lists.clear(); tasks.clear(); filters.clear(); tags.clear()
        onboarded = false
        seed()
        save()
    }

    // ------------------------------------------------------------ queries

    fun listById(id: String): TaskList? = lists.firstOrNull { it.id == id }

    fun tasksFor(viewId: String): List<TaskItem> = when {
        viewId == "mytasks" -> tasks.toList()
        viewId == "filter_today" -> tasks.filter { t ->
            val d = t.due()?.toLocalDate()
            d != null && !d.isAfter(Dates.today())
        }
        viewId == "filter_recent" -> tasks.sortedByDescending { it.modifiedAt }
        viewId.startsWith("filter_") -> {
            val f = filters.firstOrNull { it.id == viewId }
            if (f == null) emptyList() else tasks.filter { t ->
                f.minPriority < 0 || t.priority >= f.minPriority
            }
        }
        viewId.startsWith("tag_") -> {
            val tag = viewId.removePrefix("tag_")
            tasks.filter { it.tags.contains(tag) }
        }
        viewId.startsWith("list_") -> tasks.filter { it.listId == viewId }
        else -> tasks.toList()
    }

    fun countsFor(viewId: String): Int = tasksFor(viewId).count { !it.completed }

    fun viewTitle(viewId: String): String = when {
        viewId == "mytasks" -> "我的任务"
        viewId == "filter_today" -> "今天"
        viewId == "filter_recent" -> "最近修改过的"
        viewId.startsWith("filter_") -> filters.firstOrNull { it.id == viewId }?.name ?: "过滤器"
        viewId.startsWith("tag_") -> viewId.removePrefix("tag_")
        viewId.startsWith("list_") -> listById(viewId)?.name ?: "清单"
        else -> "任务"
    }

    fun viewColor(viewId: String): Long = when {
        viewId.startsWith("list_") -> listById(viewId)?.color ?: 0xFF1E88E5
        else -> 0xFF1E88E5
    }

    fun allTags(): List<String> = tags.toList()

    fun addTag(name: String): String {
        if (!tags.contains(name)) tags.add(name)
        save()
        return name
    }

    fun nextTaskId(): String = "t" + (System.currentTimeMillis() % 100000) + "_" + (taskSeq++)

    // ------------------------------------------------------------ seed

    private fun seed() {
        val work = TaskList("list_work", "工作", 0xFF2E7D32, "briefcase")
        val life = TaskList("list_life", "生活", 0xFF1E88E5, "cottage")
        val shop = TaskList("list_shopping", "购物", 0xFFF9A825, "shopping_cart")
        lists.addAll(listOf(work, life, shop))

        filters.add(FilterDef("filter_today", "今天", builtin = true))
        filters.add(FilterDef("filter_recent", "最近修改过的", builtin = true))

        val now = LocalDateTime.now()
        val today = Dates.today()

        fun at(days: Long, hour: Int, minute: Int = 0): Long =
            Dates.toMillis(today.plusDays(days).atTime(hour, minute))

        fun day(days: Long): Long = Dates.toMillis(today.plusDays(days).atStartOfDay())

        var order = 0
        fun task(
            id: String, list: String, title: String, notes: String = "",
            due: Long? = null, priority: Int = 0, tags: List<String> = emptyList(),
            repeat: String = REPEAT_NONE, repeatFromDue: Boolean = false,
            completed: Boolean = false, subtasks: List<Subtask> = emptyList(),
        ): TaskItem {
            val t = TaskItem(
                id = id, listId = list, title = title, notes = notes, dueMillis = due,
                priority = priority, repeat = repeat, repeatFromDue = repeatFromDue,
                completedMillis = if (completed) Dates.toMillis(now.minusDays(1)) else null,
                order = order++, modifiedAt = System.currentTimeMillis() - order * 60_000L
            )
            t.tags.addAll(tags)
            t.subtasks.addAll(subtasks)
            tags.forEach { if (!this.tags.contains(it)) this.tags.add(it) }
            tasks.add(t)
            return t
        }

        task(
            "t001", work.id, "写季度 OKR 草稿", "先列三个方向", at(0, 18), 3, listOf("规划"),
            subtasks = listOf(
                Subtask("t001a", "收集上季度数据", true, 2, day(0)),
                Subtask("t001b", "对齐主管预期", false, 3, day(0)),
            )
        )
        task("t002", work.id, "每日站会纪要", due = at(0, 10), priority = 2,
            tags = listOf("会议"), repeat = REPEAT_DAILY)
        task("t003", work.id, "提交报销单", "出租车票别忘贴", at(3, 0), 1, listOf("财务"))
        task("t004", work.id, "评审复现工程 PR", due = at(2, 0), priority = 3,
            tags = listOf("评审"), completed = true)
        task("t005", life.id, "给绿植浇水", due = at(0, 0), priority = 1,
            tags = listOf("家务"), repeat = REPEAT_WEEKLY)
        task("t006", life.id, "预约牙医复诊", due = at(10, 11), priority = 2, tags = listOf("健康"))
        task("t007", life.id, "读书会分享准备", due = at(2, 20), priority = 2,
            tags = listOf("阅读"), repeat = REPEAT_WEEKLY)
        task("t008", life.id, "整理旧照片", due = null, priority = 1, tags = listOf("家务", "周末"))
        task("t009", life.id, "月度复盘日记", due = day(7), priority = 1,
            tags = listOf("复盘"), repeat = REPEAT_MONTHLY)
        task("t010", life.id, "给家里打电话", due = at(4, 20), priority = 2, repeat = REPEAT_WEEKLY)
        task("t011", shop.id, "买猫粮", "上次那款鸡肉味的", day(1), 3, listOf("宠物"))
        task(
            "t012", shop.id, "补充办公用品", due = day(5), priority = 1, tags = listOf("文具"),
            subtasks = listOf(
                Subtask("t012a", "中性笔 ×2", false, 1, null),
                Subtask("t012b", "A4 纸一包", true, 1, null),
            )
        )
        task("t013", shop.id, "换季衣物收纳袋", due = null, priority = 1, tags = listOf("家居"))
        task("t014", life.id, "缴物业费", due = day(6), priority = 3,
            tags = listOf("财务"), repeat = REPEAT_MONTHLY)
        task("t015", work.id, "更新依赖版本", due = day(4), priority = 2, tags = listOf("维护"))
        task("t016", work.id, "整理会议纪要模板", due = null, priority = 1,
            tags = listOf("模板"), completed = true)
        task("t017", life.id, "周末大扫除", due = at(3, 9), priority = 2,
            tags = listOf("家务", "周末"), repeat = REPEAT_WEEKLY)
        task("t018", shop.id, "生日礼物（母亲）", "10 月 2 日前到手", day(12), 3, listOf("家人"))
    }

    // ------------------------------------------------------------ json

    private fun dump(): String {
        val root = JSONObject()
        root.put("schema", SCHEMA)
        root.put("onboarded", onboarded)
        root.put("lastViewId", lastViewId)
        root.put("showCompleted", showCompleted)
        root.put("groupBy", groupBy)
        root.put("sortBy", sortBy)
        root.put("sortAscending", sortAscending)
        root.put("remindersBannerDismissed", remindersBannerDismissed)
        root.put("filterSeq", filterSeq)
        root.put("listSeq", listSeq)
        root.put("taskSeq", taskSeq)

        val ls = JSONArray()
        lists.forEach {
            ls.put(JSONObject().apply {
                put("id", it.id); put("name", it.name); put("color", it.color); put("icon", it.icon)
            })
        }
        root.put("lists", ls)

        val fs = JSONArray()
        filters.forEach {
            fs.put(JSONObject().apply {
                put("id", it.id); put("name", it.name); put("color", it.color)
                put("icon", it.icon); put("minPriority", it.minPriority); put("builtin", it.builtin)
            })
        }
        root.put("filters", fs)

        val ts = JSONArray()
        tasks.forEach { t ->
            ts.put(JSONObject().apply {
                put("id", t.id); put("listId", t.listId); put("title", t.title)
                put("notes", t.notes)
                put("dueMillis", t.dueMillis ?: JSONObject.NULL)
                put("startKind", t.startKind)
                put("startMillis", t.startMillis ?: JSONObject.NULL)
                put("startOffsetDays", t.startOffsetDays)
                put("repeat", t.repeat); put("repeatFromDue", t.repeatFromDue)
                put("priority", t.priority)
                put("completedMillis", t.completedMillis ?: JSONObject.NULL)
                put("timerSeconds", t.timerSeconds)
                put("location", t.location ?: JSONObject.NULL)
                put("order", t.order); put("modifiedAt", t.modifiedAt)
                put("tags", JSONArray(t.tags))
                val ss = JSONArray()
                t.subtasks.forEach { s ->
                    ss.put(JSONObject().apply {
                        put("id", s.id); put("title", s.title); put("completed", s.completed)
                        put("priority", s.priority)
                        put("dueMillis", s.dueMillis ?: JSONObject.NULL)
                    })
                }
                put("subtasks", ss)
            })
        }
        root.put("tasks", ts)
        root.put("tags", JSONArray(tags.toList()))
        return root.toString()
    }

    private fun load(raw: String) {
        val root = JSONObject(raw)
        onboarded = root.optBoolean("onboarded", false)
        lastViewId = root.optString("lastViewId", "mytasks")
        showCompleted = root.optBoolean("showCompleted", true)
        groupBy = root.optString("groupBy", "due")
        sortBy = root.optString("sortBy", "due")
        sortAscending = root.optBoolean("sortAscending", true)
        remindersBannerDismissed = root.optBoolean("remindersBannerDismissed", false)
        filterSeq = root.optInt("filterSeq", 0)
        listSeq = root.optInt("listSeq", 0)
        taskSeq = root.optInt("taskSeq", 0)

        val ls = root.optJSONArray("lists") ?: JSONArray()
        for (i in 0 until ls.length()) {
            val o = ls.getJSONObject(i)
            lists.add(
                TaskList(
                    o.getString("id"), o.getString("name"),
                    o.optLong("color", 0xFF1E88E5), o.optString("icon", "list")
                )
            )
        }
        val fs = root.optJSONArray("filters") ?: JSONArray()
        for (i in 0 until fs.length()) {
            val o = fs.getJSONObject(i)
            filters.add(
                FilterDef(
                    o.getString("id"), o.getString("name"), o.optLong("color", 0xFF1E88E5),
                    o.optString("icon", "filter"), o.optInt("minPriority", -1),
                    o.optBoolean("builtin", false)
                )
            )
        }
        val ts = root.optJSONArray("tasks") ?: JSONArray()
        for (i in 0 until ts.length()) {
            val o = ts.getJSONObject(i)
            val t = TaskItem(
                id = o.getString("id"),
                listId = o.getString("listId"),
                title = o.getString("title"),
                notes = o.optString("notes", ""),
                dueMillis = if (o.isNull("dueMillis")) null else o.getLong("dueMillis"),
                startKind = o.optInt("startKind", START_NONE),
                startMillis = if (o.isNull("startMillis")) null else o.getLong("startMillis"),
                startOffsetDays = o.optInt("startOffsetDays", 0),
                repeat = o.optString("repeat", REPEAT_NONE),
                repeatFromDue = o.optBoolean("repeatFromDue", false),
                priority = o.optInt("priority", 0),
                completedMillis = if (o.isNull("completedMillis")) null else o.getLong("completedMillis"),
                timerSeconds = o.optLong("timerSeconds", 0L),
                location = if (o.isNull("location")) null else o.getString("location"),
                order = o.optInt("order", i),
                modifiedAt = o.optLong("modifiedAt", System.currentTimeMillis()),
            )
            val tg = o.optJSONArray("tags") ?: JSONArray()
            for (j in 0 until tg.length()) t.tags.add(tg.getString(j))
            val ss = o.optJSONArray("subtasks") ?: JSONArray()
            for (j in 0 until ss.length()) {
                val s = ss.getJSONObject(j)
                t.subtasks.add(
                    Subtask(
                        s.getString("id"), s.getString("title"),
                        s.optBoolean("completed", false), s.optInt("priority", 0),
                        if (s.isNull("dueMillis")) null else s.getLong("dueMillis")
                    )
                )
            }
            tasks.add(t)
        }
        val tg = root.optJSONArray("tags") ?: JSONArray()
        for (i in 0 until tg.length()) tags.add(tg.getString(i))
    }
}

/** Formats an epoch-millis date for compact display in the editor rows. */
fun Long.asDayTime(): String = Dates.dateTimeLabel(Dates.fromMillis(this))

object TimeFmt {
    val hhmm: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
}
