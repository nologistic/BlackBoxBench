package com.blackboxbench.reproduction

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** 全局内存仓库 + JSON 文件持久化（filesDir/tasks_state.json）。 */
object Store {
    val tasks = mutableStateListOf<Task>()
    val lists = mutableStateListOf<TaskList>()
    val tags = mutableStateListOf<Tag>()
    val places = mutableStateListOf<Place>()
    val filters = mutableStateListOf<Filter>()

    val settings = mutableStateOf(mutableMapOf<String, String>())
    val onboarded = mutableStateOf(false)
    val bannerDismissed = mutableStateOf(false)
    /** 每次写入递增，驱动依赖任务字段的界面重组 */
    val version = androidx.compose.runtime.mutableLongStateOf(0L)

    private var file: File? = null

    fun setting(key: String, def: String = ""): String = settings.value[key] ?: def
    fun boolSetting(key: String, def: Boolean): Boolean = (settings.value[key] ?: def.toString()).toBoolean()
    fun setSetting(key: String, value: String) {
        val m = LinkedHashMap(settings.value); m[key] = value
        settings.value = m; save()
    }
    fun setBool(key: String, v: Boolean) = setSetting(key, v.toString())

    fun init(context: Context) {
        file = File(context.filesDir, "tasks_state.json")
        if (file!!.exists()) {
            runCatching { loadFrom(file!!.readText()) }
            if (tasks.isEmpty() && lists.isEmpty()) seed()
        } else {
            seed()
            save()
        }
    }

    // ---------- 查询 ----------
    fun listName(id: String) = lists.firstOrNull { it.id == id }?.name ?: ""
    fun tagById(id: String) = tags.firstOrNull { it.id == id }
    fun listById(id: String) = lists.firstOrNull { it.id == id }
    fun placeById(id: String) = places.firstOrNull { it.id == id }
    fun filterById(id: String) = filters.firstOrNull { it.id == id }

    fun subtasksOf(taskId: String) = tasks.filter { it.parentId == taskId }
    fun openSubtaskCount(taskId: String) = subtasksOf(taskId).count { !it.isCompleted }

    fun matchesFilter(t: Task, f: Filter): Boolean {
        for (c in f.conditions) {
            when (c.type) {
                "tag" -> if (!t.tagIds.contains(c.param)) return false
                "no_tag" -> if (t.tagIds.isNotEmpty()) return false
                "tag_name" -> if (tags.filter { t.tagIds.contains(it.id) }.none { it.name.contains(c.param, true) }) return false
                "priority" -> if (t.priority.level < (c.param.toIntOrNull() ?: 3)) return false
                "title" -> if (!t.title.contains(c.param, true)) return false
                "list" -> if (t.listId != c.param) return false
                "repeating" -> if (!t.isRepeating) return false
                "completed" -> if (!t.isCompleted) return false
                "not_started" -> {
                    val s = Dates.parseDate(t.startDate)
                    if (s == null || !s.isAfter(Dates.today())) return false
                }
                "has_subtasks" -> if (subtasksOf(t.id).isEmpty()) return false
                "is_subtask" -> if (t.parentId.isEmpty()) return false
                "has_reminder" -> if (!hasReminder(t)) return false
                "overdue" -> if (!Dates.isOverdue(t)) return false
                "due_today" -> { val d = Dates.parseDate(t.dueDate); if (d == null || !d.isEqual(Dates.today())) return false }
                "due_tomorrow" -> { val d = Dates.parseDate(t.dueDate); if (d == null || !d.isEqual(Dates.today().plusDays(1))) return false }
                "due_after_today" -> { val d = Dates.parseDate(t.dueDate); if (d == null || !d.isAfter(Dates.today())) return false }
                "any_start" -> if (t.startDate.isEmpty()) return false
                "no_start" -> if (t.startDate.isNotEmpty()) return false
                "any_due" -> if (t.dueDate.isEmpty()) return false
                "no_due" -> if (t.dueDate.isNotEmpty()) return false
            }
        }
        return true
    }

    fun hasReminder(t: Task): Boolean = t.dueTime.isNotEmpty() || t.locationReminder

    fun tasksFor(view: ViewRef): List<Task> = when (view) {
        is ViewRef.MyTasks -> tasks.filter { it.parentId.isEmpty() }
        is ViewRef.ListView -> tasks.filter { it.listId == view.id && it.parentId.isEmpty() }
        is ViewRef.TagView -> tasks.filter { it.tagIds.contains(view.id) && it.parentId.isEmpty() }
        is ViewRef.PlaceView -> tasks.filter { it.locationId == view.id && it.parentId.isEmpty() }
        is ViewRef.BuiltinFilter -> when (view.kind) {
            "today" -> tasks.filter {
                it.parentId.isEmpty() && !it.isCompleted &&
                    Dates.parseDate(it.dueDate)?.let { d -> !d.isAfter(Dates.today()) } == true
            }
            "recent" -> tasks.toList()
            else -> emptyList()
        }
        is ViewRef.CustomFilter -> filters.firstOrNull { it.id == view.id }?.let { f ->
            tasks.filter { matchesFilter(it, f) && it.parentId.isEmpty() }
        } ?: emptyList()
    }

    fun countFor(view: ViewRef): Int = when (view) {
        is ViewRef.BuiltinFilter -> if (view.kind == "recent") tasks.count { !it.isCompleted } else tasksFor(view).count { !it.isCompleted }
        else -> tasksFor(view).count { !it.isCompleted }
    }

    // ---------- 变更 ----------
    fun touch(t: Task) { t.modifiedAt = Dates.nowStamp(); save() }

    fun addTask(t: Task): Task {
        if (t.createdAt.isEmpty()) t.createdAt = Dates.nowStamp()
        t.modifiedAt = Dates.nowStamp()
        tasks.add(t); save(); return t
    }

    fun complete(t: Task, done: Boolean) {
        if (done) {
            if (t.isRepeating && t.dueDate.isNotEmpty()) {
                t.dueDate = Dates.nextOccurrence(t.repeat, t.dueDate)
                t.completedAt = ""
            } else {
                t.completedAt = Dates.nowStamp()
            }
        } else {
            t.completedAt = ""
        }
        touch(t)
    }

    fun delete(t: Task) {
        subtasksOf(t.id).forEach { tasks.remove(it) }
        tasks.remove(t); save()
    }

    fun cloneTask(t: Task) {
        val copy = t.copy(id = UUID.randomUUID().toString(), createdAt = Dates.nowStamp(), completedAt = "")
        tasks.add(copy); save()
    }

    fun addTag(name: String, color: Int = -1, icon: String = ""): Tag {
        val t = Tag(name = name, color = color, icon = icon); tags.add(t); save(); return t
    }
    fun addList(name: String, color: Int = -1, icon: String = ""): TaskList {
        val l = TaskList(name = name, color = color, icon = icon); lists.add(l); save(); return l
    }
    fun addPlace(name: String): Place {
        val p = Place(name = name); places.add(p); save(); return p
    }
    fun addFilter(f: Filter): Filter { filters.add(f); save(); return f }

    fun deleteTag(t: Tag) { tasks.forEach { it.tagIds.remove(t.id) }; tags.remove(t); save() }
    fun deleteList(l: TaskList) {
        val fallback = lists.firstOrNull { it.id != l.id }
        tasks.filter { it.listId == l.id }.forEach { it.listId = fallback?.id ?: "" }
        lists.remove(l); save()
    }
    fun deleteFilter(f: Filter) { filters.remove(f); save() }

    fun clearCompleted(view: ViewRef) {
        tasksFor(view).filter { it.isCompleted }.toList().forEach { delete(it) }
    }

    // ---------- 持久化 ----------
    fun save() {
        val f = file ?: return
        val root = JSONObject()
        root.put("onboarded", onboarded.value)
        val st = JSONObject(); settings.value.forEach { (k, v) -> st.put(k, v) }
        root.put("settings", st)

        fun colorOf(c: Int): Any = if (c == -1) JSONObject.NULL else c

        val jLists = JSONArray()
        lists.forEach { jLists.put(JSONObject(mapOf("id" to it.id, "name" to it.name, "color" to colorOf(it.color), "icon" to it.icon))) }
        root.put("lists", jLists)

        val jTags = JSONArray()
        tags.forEach { jTags.put(JSONObject(mapOf("id" to it.id, "name" to it.name, "color" to colorOf(it.color), "icon" to it.icon))) }
        root.put("tags", jTags)

        val jPlaces = JSONArray()
        places.forEach { jPlaces.put(JSONObject(mapOf("id" to it.id, "name" to it.name, "lat" to it.lat, "lng" to it.lng))) }
        root.put("places", jPlaces)

        val jFilters = JSONArray()
        filters.forEach { fl ->
            val conds = JSONArray()
            fl.conditions.forEach { conds.put(JSONObject(mapOf("type" to it.type, "param" to it.param))) }
            jFilters.put(JSONObject(mapOf(
                "id" to fl.id, "name" to fl.name, "color" to colorOf(fl.color), "icon" to fl.icon,
                "builtin" to fl.builtin, "conditions" to conds
            )))
        }
        root.put("filters", jFilters)

        val jTasks = JSONArray()
        tasks.forEach { t ->
            jTasks.put(JSONObject(mapOf(
                "id" to t.id, "listId" to t.listId, "title" to t.title, "notes" to t.notes,
                "startDate" to t.startDate, "dueDate" to t.dueDate, "dueTime" to t.dueTime,
                "repeat" to t.repeat, "priority" to t.priority.level, "parentId" to t.parentId,
                "completedAt" to t.completedAt, "createdAt" to t.createdAt, "modifiedAt" to t.modifiedAt,
                "locationId" to t.locationId, "locationReminder" to t.locationReminder,
                "addToCalendar" to t.addToCalendar, "elapsedSeconds" to t.elapsedSeconds,
                "tagIds" to JSONArray(t.tagIds.toList())
            )))
        }
        root.put("tasks", jTasks)
        f.writeText(root.toString())
        version.value++
    }

    private fun JSONObject.str(k: String) = if (has(k) && !isNull(k)) getString(k) else ""
    private fun JSONObject.intOrNull(k: String) = if (has(k) && !isNull(k)) getInt(k) else null

    private fun loadFrom(text: String) {
        val root = JSONObject(text)
        onboarded.value = root.optBoolean("onboarded", false)
        val m = mutableMapOf<String, String>()
        root.optJSONObject("settings")?.let { st -> st.keys().forEach { k -> m[k] = st.getString(k) } }
        settings.value = m

        lists.clear(); tags.clear(); places.clear(); filters.clear(); tasks.clear()

        root.optJSONArray("lists")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                lists.add(TaskList(o.str("id"), o.str("name"), o.intOrNull("color") ?: -1, o.str("icon")))
            }
        }
        root.optJSONArray("tags")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                tags.add(Tag(o.str("id"), o.str("name"), o.intOrNull("color") ?: -1, o.str("icon")))
            }
        }
        root.optJSONArray("places")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                places.add(Place(o.str("id"), o.str("name"), o.optDouble("lat"), o.optDouble("lng")))
            }
        }
        root.optJSONArray("filters")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val conds = mutableListOf<FilterCondition>()
                o.optJSONArray("conditions")?.let { ca ->
                    for (j in 0 until ca.length()) {
                        val co = ca.getJSONObject(j); conds.add(FilterCondition(co.str("type"), co.str("param")))
                    }
                }
                filters.add(Filter(o.str("id"), o.str("name"), o.intOrNull("color") ?: -1, o.str("icon"), conds, o.str("builtin")))
            }
        }
        root.optJSONArray("tasks")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val tagIds = mutableListOf<String>()
                o.optJSONArray("tagIds")?.let { ta -> for (j in 0 until ta.length()) tagIds.add(ta.getString(j)) }
                tasks.add(Task(
                    id = o.str("id"), listId = o.str("listId"), title = o.str("title"), notes = o.str("notes"),
                    startDate = o.str("startDate"), dueDate = o.str("dueDate"), dueTime = o.str("dueTime"),
                    repeat = o.str("repeat"), priority = Priority.of(o.optInt("priority", 1)),
                    tagIds = tagIds, parentId = o.str("parentId"), completedAt = o.str("completedAt"),
                    createdAt = o.str("createdAt"), modifiedAt = o.str("modifiedAt"),
                    locationId = o.str("locationId"), locationReminder = o.optBoolean("locationReminder"),
                    addToCalendar = o.optBoolean("addToCalendar"), elapsedSeconds = o.optLong("elapsedSeconds")
                ))
            }
        }
    }

    // ---------- 种子数据（来自 /materials/app/tasks.json 的虚构内容） ----------
    private fun seed() {
        lists.clear(); tags.clear(); places.clear(); filters.clear(); tasks.clear()

        lists.add(TaskList("lst_default", "默认清单"))
        lists.add(TaskList("lst_work", "工作"))
        lists.add(TaskList("lst_life", "生活"))
        lists.add(TaskList("lst_shopping", "购物"))

        val tagIds = mutableMapOf<String, String>()
        fun tagId(name: String): String = tagIds.getOrPut(name) { addTagRaw(name) }

        filters.add(Filter(name = "今天", icon = "today", builtin = "today"))
        filters.add(Filter(name = "最近修改过的", icon = "history", builtin = "recent"))

        data class SeedSub(val title: String, val due: String, val priority: Priority, val completedAt: String)
        data class SeedTask(
            val list: String, val title: String, val notes: String = "", val due: String = "",
            val priority: Priority = Priority.LOW, val tags: List<String> = emptyList(),
            val repeat: String = "", val completedAt: String = "", val subtasks: List<SeedSub> = emptyList()
        )
        val seeds = listOf(
            SeedTask("lst_work", "写季度 OKR 草稿", "先列三个方向", "2026-09-08", Priority.HIGH, listOf("规划"),
                subtasks = listOf(
                    SeedSub("收集上季度数据", "2026-09-07", Priority.MEDIUM, "2026-09-06T16:20"),
                    SeedSub("对齐主管预期", "2026-09-07", Priority.HIGH, "")
                )),
            SeedTask("lst_work", "每日站会纪要", due = "2026-09-07T10:00", priority = Priority.MEDIUM, tags = listOf("会议"), repeat = "FREQ=DAILY"),
            SeedTask("lst_work", "提交报销单", "出租车票别忘贴", "2026-09-10", Priority.LOW, listOf("财务")),
            SeedTask("lst_work", "评审复现工程 PR", due = "2026-09-09", priority = Priority.HIGH, tags = listOf("评审"), completedAt = "2026-09-08T11:05"),
            SeedTask("lst_life", "给绿植浇水", due = "2026-09-07", priority = Priority.LOW, tags = listOf("家务"), repeat = "FREQ=WEEKLY;BYDAY=MO,TH"),
            SeedTask("lst_life", "预约牙医复诊", due = "2026-09-18T11:00", priority = Priority.MEDIUM, tags = listOf("健康")),
            SeedTask("lst_life", "读书会分享准备", due = "2026-09-09T20:00", priority = Priority.MEDIUM, tags = listOf("阅读"), repeat = "FREQ=WEEKLY;BYDAY=WE"),
            SeedTask("lst_life", "整理旧照片", priority = Priority.LOW, tags = listOf("家务", "周末")),
            SeedTask("lst_life", "月度复盘日记", due = "2026-09-30", priority = Priority.LOW, tags = listOf("复盘"), repeat = "FREQ=MONTHLY;BYMONTHDAY=30"),
            SeedTask("lst_life", "给家里打电话", due = "2026-09-13T20:00", priority = Priority.MEDIUM, repeat = "FREQ=WEEKLY;BYDAY=SU"),
            SeedTask("lst_shopping", "买猫粮", "上次那款鸡肉味的", "2026-09-08", Priority.HIGH, listOf("宠物")),
            SeedTask("lst_shopping", "补充办公用品", due = "2026-09-12", priority = Priority.LOW, tags = listOf("文具"),
                subtasks = listOf(
                    SeedSub("中性笔 ×2", "", Priority.LOW, ""),
                    SeedSub("A4 纸一包", "", Priority.LOW, "2026-09-05T09:00")
                )),
            SeedTask("lst_shopping", "换季衣物收纳袋", priority = Priority.LOW, tags = listOf("家居")),
            SeedTask("lst_life", "缴物业费", due = "2026-09-15", priority = Priority.HIGH, tags = listOf("财务"), repeat = "FREQ=MONTHLY;BYMONTHDAY=15"),
            SeedTask("lst_work", "更新依赖版本", due = "2026-09-11", priority = Priority.MEDIUM, tags = listOf("维护")),
            SeedTask("lst_work", "整理会议纪要模板", priority = Priority.LOW, tags = listOf("模板"), completedAt = "2026-09-03T15:40"),
            SeedTask("lst_life", "周末大扫除", due = "2026-09-12T09:00", priority = Priority.MEDIUM, tags = listOf("家务", "周末"), repeat = "FREQ=WEEKLY;BYDAY=SA"),
            SeedTask("lst_shopping", "生日礼物（母亲）", "10 月 2 日前到手", "2026-09-28", Priority.HIGH, listOf("家人"))
        )
        seeds.forEach { s ->
            val (dueDate, dueTime) = if (s.due.contains("T")) s.due.substringBefore("T") to s.due.substringAfter("T") else s.due to ""
            val t = Task(
                id = UUID.randomUUID().toString(), listId = s.list, title = s.title, notes = s.notes,
                dueDate = dueDate, dueTime = dueTime, repeat = s.repeat, priority = s.priority,
                tagIds = s.tags.map { tagId(it) }.toMutableList(),
                completedAt = s.completedAt, createdAt = "2026-09-01T09:00", modifiedAt = "2026-09-01T09:00"
            )
            tasks.add(t)
            s.subtasks.forEach { sub ->
                tasks.add(Task(
                    id = UUID.randomUUID().toString(), listId = s.list, title = sub.title,
                    dueDate = sub.due, priority = sub.priority, parentId = t.id,
                    completedAt = sub.completedAt, createdAt = "2026-09-01T09:00", modifiedAt = "2026-09-01T09:00"
                ))
            }
        }
        onboarded.value = false
    }

    private fun addTagRaw(name: String): String {
        val t = Tag(name = name, color = 0xFF2196F3.toInt())
        tags.add(t); return t.id
    }
}
