package com.blackboxbench.reproduction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.platform.LocalContext
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ReproducedApp() }
    }
}

@Composable
fun ReproducedApp() {
    val context = LocalContext.current
    val store = remember { TaskStore(context) }
    var tasks by remember { mutableStateOf(store.loadTasks()) }
    var lists by remember { mutableStateOf(store.loadLists()) }
    var filters by remember { mutableStateOf(store.loadFilters()) }
    var dark by remember { mutableStateOf(store.isDark()) }
    var hideCompleted by remember { mutableStateOf(store.hideCompleted()) }
    var screen by rememberSaveable { mutableStateOf("main") }
    var scopeType by rememberSaveable { mutableStateOf("all") }
    var scopeId by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("我的任务") }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }

    fun saveTasks(next: List<AppTask>) {
        tasks = next
        store.saveTasks(next)
    }

    BenchmarkAppTheme(darkTheme = dark) {
        Surface(Modifier.fillMaxSize()) {
            when (screen) {
                "main" -> TaskHome(
                    tasks = tasks,
                    lists = lists,
                    filters = filters,
                    title = title,
                    scopeType = scopeType,
                    scopeId = scopeId,
                    hideCompleted = hideCompleted,
                    onHideCompleted = { hideCompleted = it; store.saveHideCompleted(it) },
                    onScope = { type, id, label -> scopeType = type; scopeId = id; title = label },
                    onAddTask = { editingId = null; screen = "editor" },
                    onEditTask = { editingId = it; screen = "editor" },
                    onToggleTask = { id ->
                        saveTasks(tasks.map { task ->
                            if (task.id != id) task
                            else if (task.repeat.isNotBlank()) task.copy(
                                due = nextDay(task.due),
                                completed = false,
                                subtasks = task.subtasks.map { it.copy(completed = false) }
                            ) else task.copy(completed = !task.completed)
                        })
                    },
                    onToggleSubtask = { taskId, subId ->
                        saveTasks(tasks.map { task ->
                            if (task.id != taskId) task else task.copy(
                                subtasks = task.subtasks.map { sub ->
                                    if (sub.id == subId) sub.copy(completed = !sub.completed) else sub
                                }
                            )
                        })
                    },
                    onClearCompleted = { saveTasks(tasks.filterNot { it.completed }) },
                    onSettings = { screen = "settings" },
                    onNewList = { screen = "listEditor" },
                    onNewFilter = { screen = "filterEditor" },
                    onNewLocation = { screen = "location" }
                )
                "editor" -> TaskEditor(
                    task = tasks.firstOrNull { it.id == editingId },
                    lists = lists,
                    initialList = if (scopeType == "list") scopeId else lists.firstOrNull()?.id.orEmpty(),
                    allTags = tasks.flatMap { it.tags }.distinct().sorted(),
                    onBack = { screen = "main" },
                    onSave = { changed ->
                        saveTasks(if (tasks.any { it.id == changed.id }) tasks.map { if (it.id == changed.id) changed else it } else listOf(changed) + tasks)
                        scopeType = "all"; scopeId = ""; title = "我的任务"; screen = "main"
                    },
                    onDelete = { id -> saveTasks(tasks.filterNot { it.id == id }); screen = "main" }
                )
                "settings" -> SettingsScreen(onBack = { screen = "main" }, onNavigate = { screen = it })
                "appearance" -> AppearanceScreen(
                    dark = dark,
                    onDarkChange = { dark = it; store.saveDark(it) },
                    onBack = { screen = "settings" }
                )
                "notifications" -> NotificationsScreen { screen = "settings" }
                "defaults" -> DefaultsScreen(lists) { screen = "settings" }
                "listOptions" -> ListOptionsScreen { screen = "settings" }
                "editOptions" -> EditOptionsScreen { screen = "settings" }
                "account" -> AccountsScreen { screen = "settings" }
                "listEditor" -> ListEditor(
                    onBack = { screen = "main" },
                    onSave = { name, color ->
                        val item = TaskList("list_" + UUID.randomUUID(), name, color)
                        lists = lists + item
                        store.saveLists(lists)
                        scopeType = "list"; scopeId = item.id; title = item.name; screen = "main"
                    }
                )
                "filterEditor" -> FilterEditor(
                    count = tasks.count { it.priority == "high" && !it.completed },
                    onBack = { screen = "main" },
                    onSave = { spec ->
                        filters = filters + spec
                        store.saveFilters(filters)
                        scopeType = "filter"; scopeId = spec.name; title = spec.name; screen = "main"
                    }
                )
                "location" -> LocationPicker(
                    onBack = { screen = "main" },
                    onSelect = { scopeType = "location"; scopeId = "0.0 0.0"; title = "0.0 0.0"; screen = "main" }
                )
            }
        }
    }
}
