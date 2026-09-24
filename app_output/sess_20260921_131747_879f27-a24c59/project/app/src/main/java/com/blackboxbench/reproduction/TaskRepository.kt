package com.blackboxbench.reproduction

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

enum class SortField(val label: String) {
    DUE("截止日期"),
    PRIORITY("优先级"),
    TITLE("标题"),
    MANUAL("手动")
}

/** 当前浏览的视图 */
sealed class ViewKey(val title: String) {
    object All : ViewKey("全部任务")
    object Today : ViewKey("今天")
    data class ByList(val listId: String, val name: String) : ViewKey(name)
    data class ByTag(val tag: String) : ViewKey("#$tag")
}

/**
 * 任务数据的唯一来源：内存状态 + SharedPreferences 持久化。
 * 首次启动用 /materials 提供的虚构样例数据播种。
 */
object Repo {

    private const val PREFS = "tasks_repro_state"
    private const val KEY = "state"
    private lateinit var prefs: SharedPreferences

    var lists by mutableStateOf<List<TaskList>>(emptyList())
        private set
    var tasks by mutableStateOf<List<Task>>(emptyList())
        private set
    var onboarded by mutableStateOf(false)
        private set
    var hideCompleted by mutableStateOf(false)
        private set
    var sortField by mutableStateOf(SortField.DUE)
        private set
    var sortAsc by mutableStateOf(true)
        private set

    /** 最近一次操作的反馈文案，供 Snackbar 展示 */
    var message by mutableStateOf("")

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY, null)
        if (raw.isNullOrBlank()) seedFromAssets(context) else load(raw)
    }

    private fun seedFromAssets(context: Context) {
        val text = runCatching {
            context.assets.open("tasks.json").bufferedReader().use { it.readText() }
        }.getOrNull()
        if (text.isNullOrBlank()) {
            lists = emptyList()
            tasks = emptyList()
        } else {
            load(text, persistAfter = false)
        }
        refresh()
        persist()
    }

    private fun load(raw: String, persistAfter: Boolean = true) {
        val root = JSONObject(raw)
        val ls = mutableListOf<TaskList>()
        root.optJSONArray("lists")?.let { arr ->
            for (i in 0 until arr.length()) ls.add(TaskList.fromJson(arr.getJSONObject(i)))
        }
        val ts = mutableListOf<Task>()
        root.optJSONArray("tasks")?.let { arr ->
            for (i in 0 until arr.length()) {
                val t = Task.fromJson(arr.getJSONObject(i))
                if (t.order == 0) t.order = i
                ts.add(t)
            }
        }
        lists = ls
        tasks = ts
        onboarded = root.optBoolean("onboarded", false)
        hideCompleted = root.optBoolean("hide_completed", false)
        sortField = runCatching { SortField.valueOf(root.optString("sort_field", "DUE")) }
            .getOrDefault(SortField.DUE)
        sortAsc = root.optBoolean("sort_asc", true)
        if (persistAfter) persist()
    }

    private fun persist() {
        if (!::prefs.isInitialized) return
        val root = JSONObject().apply {
            put("lists", JSONArray().apply { lists.forEach { put(it.toJson()) } })
            put("tasks", JSONArray().apply { tasks.forEach { put(it.toJson()) } })
            put("onboarded", onboarded)
            put("hide_completed", hideCompleted)
            put("sort_field", sortField.name)
            put("sort_asc", sortAsc)
        }
        prefs.edit().putString(KEY, root.toString()).apply()
    }

    /** 用新的列表实例替换，触发 Compose 重组 */
    private fun refresh() {
        tasks = tasks.toList()
    }

    // ------------------------------------------------------------ 视图与筛选

    fun tasksFor(view: ViewKey): List<Task> {
        val today = LocalDate.now()
        val filtered = tasks.filter { t ->
            val inView = when (view) {
                is ViewKey.All -> true
                is ViewKey.Today -> parseDue(t.due)?.toLocalDate() == today
                is ViewKey.ByList -> t.listId == view.listId
                is ViewKey.ByTag -> t.tags.contains(view.tag)
            }
            inView && (!hideCompleted || !t.done)
        }
        val sorted = when (sortField) {
            SortField.DUE -> filtered.sortedWith(
                compareBy<Task> { it.due.isBlank() }
                    .thenBy { parseDue(it.due) ?: LocalDateTime.MAX }
                    .thenBy { it.title }
            )
            SortField.PRIORITY ->
                if (sortAsc) filtered.sortedBy { it.priority } else filtered.sortedByDescending { it.priority }
            SortField.TITLE ->
                if (sortAsc) filtered.sortedBy { it.title } else filtered.sortedByDescending { it.title }
            SortField.MANUAL ->
                if (sortAsc) filtered.sortedBy { it.order } else filtered.sortedByDescending { it.order }
        }
        val ordered = if (sortField == SortField.MANUAL) sorted
        else sorted.filter { !it.done } + sorted.filter { it.done }
        return ordered
    }

    fun incompleteCount(listId: String? = null): Int =
        tasks.count { !it.done && (listId == null || it.listId == listId) }

    fun completedCount(listId: String? = null): Int =
        tasks.count { it.done && (listId == null || it.listId == listId) }

    fun allTags(): List<String> = tasks.flatMap { it.tags }.distinct().sorted()

    fun listName(id: String): String = lists.find { it.id == id }?.name ?: "未归类"

    fun find(id: String): Task? = tasks.find { it.id == id }

    // ------------------------------------------------------------ 写操作

    fun completeOnboarding() {
        onboarded = true
        persist()
    }

    fun applyHideCompleted(value: Boolean) {
        hideCompleted = value
        persist()
    }

    fun setSort(field: SortField, ascending: Boolean) {
        sortField = field
        sortAsc = ascending
        persist()
    }

    /** 依次切换排序字段 */
    fun cycleSort() {
        val values = SortField.values()
        sortField = values[(sortField.ordinal + 1) % values.size]
        persist()
    }

    /** 勾选完成 / 取消完成 */
    fun toggleComplete(taskId: String) {
        val t = find(taskId) ?: return
        val now = LocalDateTime.now()
        if (t.done) {
            t.completedAt = ""
            message = "已恢复「${t.title}」"
        } else if (t.repeat.isNotBlank()) {
            // 重复任务：当前实例记录完成时间，同一条目立即顺延到下一次出现
            val next = nextOccurrence(t.due, t.repeat, now.toLocalDate())
            t.lastCompletedAt = stampOf(now)
            t.due = next
            t.completedAt = ""
            t.subtasks.forEach { it.completedAt = "" }
            message = "已完成本次，「${t.title}」下一次 ${formatDue(next)}"
        } else {
            t.completedAt = stampOf(now)
            message = "已完成「${t.title}」"
        }
        refresh()
        persist()
    }

    fun toggleSubtask(taskId: String, subtaskId: String) {
        val t = find(taskId) ?: return
        val s = t.subtasks.find { it.id == subtaskId } ?: return
        s.completedAt = if (s.done) "" else stampOf(LocalDateTime.now())
        refresh()
        persist()
    }

    fun upsert(task: Task) {
        val idx = tasks.indexOfFirst { it.id == task.id }
        tasks = if (idx >= 0) tasks.toMutableList().also { it[idx] = task }
        else tasks + task
        persist()
    }

    fun newTask(listId: String, order: Int): Task = Task(
        id = "t_" + UUID.randomUUID().toString().take(8),
        listId = listId,
        order = order
    )

    fun nextOrder(): Int = (tasks.maxOfOrNull { it.order } ?: 0) + 1

    fun delete(taskId: String) {
        val t = find(taskId) ?: return
        tasks = tasks.filterNot { it.id == taskId }
        message = "已删除「${t.title}」"
        persist()
    }

    /** 手动排序：在当前视图内上移 / 下移 */
    fun move(taskId: String, delta: Int) {
        val ordered = tasks.sortedBy { it.order }.toMutableList()
        val idx = ordered.indexOfFirst { it.id == taskId }
        if (idx < 0) return
        val target = idx + delta
        if (target < 0 || target >= ordered.size) return
        val a = ordered[idx]
        val b = ordered[target]
        val tmp = a.order
        a.order = b.order
        b.order = tmp
        refresh()
        persist()
    }

    fun resetToSeed(context: Context) {
        seedFromAssets(context)
    }
}
