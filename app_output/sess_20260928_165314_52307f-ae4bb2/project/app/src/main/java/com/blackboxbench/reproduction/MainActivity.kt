package com.blackboxbench.reproduction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ReproducedApp() }
    }
}

/** 当前主界面展示的视图 */
sealed interface MainView {
    data object Tasks : MainView
    data class ListView(val listId: String) : MainView
    data class TagView(val tag: String) : MainView
    data class FilterView(val filterId: String) : MainView
}

/** 页面路由 */
sealed interface Screen {
    data object Main : Screen
    data class Editor(val taskId: String?, val presetListId: String?) : Screen
    data object NewList : Screen
    data object NewFilter : Screen
    data object Settings : Screen
    data object Appearance : Screen
}

class AppState(private val context: android.content.Context) {
    var data by mutableStateOf(Store.load(context))
        private set
    val backStack = mutableStateListOf<Screen>(if (data.onboarded) Screen.Main as Screen else Screen.Main)
    var showWelcome by mutableStateOf(!data.onboarded)
    var view by mutableStateOf<MainView>(MainView.Tasks)
    var searchMode by mutableStateOf(false)
    var query by mutableStateOf("")
    var sortKey by mutableStateOf("my")
    var sortAscending by mutableStateOf(true)
    var drawerOpen by mutableStateOf(false)
    var lastCompleted by mutableStateOf<TaskItem?>(null)

    val screen: Screen get() = backStack.lastOrNull() ?: Screen.Main

    fun update(transform: (AppData) -> AppData) {
        data = transform(data)
        Store.save(context, data)
    }

    fun navigate(s: Screen) { backStack.add(s) }
    fun pop(): Boolean = if (backStack.size > 1) { backStack.removeAt(backStack.size - 1); true } else false

    fun finishOnboarding() = update { it.copy(onboarded = true) }

    // ---- 任务操作 ----

    fun toggleComplete(taskId: String) {
        val task = data.taskById(taskId) ?: return
        if (!task.repeat.isEmpty() && !task.isCompleted) {
            // 重复任务：完成即在本条目上顺延到下一次出现
            val next = nextOccurrence(task)
            update { d -> d.copy(tasks = d.tasks.map { if (it.id == taskId) it.copy(due = next) else it }) }
        } else if (task.isCompleted) {
            update { d -> d.copy(tasks = d.tasks.map { if (it.id == taskId) it.copy(completedAt = null) else it }) }
        } else {
            val stamp = nowIso()
            update { d -> d.copy(tasks = d.tasks.map { if (it.id == taskId) it.copy(completedAt = stamp) else it }) }
            lastCompleted = task
        }
    }

    fun undoComplete() {
        val task = lastCompleted ?: return
        update { d -> d.copy(tasks = d.tasks.map { if (it.id == task.id) it.copy(completedAt = null) else it }) }
        lastCompleted = null
    }

    fun saveTask(task: TaskItem, andNew: Boolean = false) {
        update { d ->
            val exists = d.tasks.any { it.id == task.id }
            val tasks = if (exists) d.tasks.map { if (it.id == task.id) task else it }
            else d.tasks + task.copy(createdAt = System.currentTimeMillis(), order = d.tasks.size)
            d.copy(tasks = tasks)
        }
        if (andNew) {
            backStack[backStack.size - 1] = Screen.Editor(null, (view as? MainView.ListView)?.listId)
        } else {
            pop()
        }
    }

    fun deleteTasks(ids: List<String>) = update { d -> d.copy(tasks = d.tasks.filter { !ids.contains(it.id) }) }
    fun deleteList(listId: String) {
        update { d -> d.copy(tasks = d.tasks.filter { it.listId != listId }, lists = d.lists.filter { it.id != listId }) }
        if ((view as? MainView.ListView)?.listId == listId) view = MainView.Tasks
    }
    fun renameList(listId: String, name: String) =
        update { d -> d.copy(lists = d.lists.map { if (it.id == listId) it.copy(name = name) else it }) }
}

@Composable
fun rememberAppState(context: android.content.Context): AppState = remember { AppState(context.applicationContext) }

@Composable
fun ReproducedApp() {
    BenchmarkAppTheme {
        val context = LocalContext.current
        val app = rememberAppState(context)
        val screen = app.screen

        BackHandler(enabled = app.drawerOpen) { app.drawerOpen = false }
        BackHandler(enabled = app.searchMode) { app.searchMode = false; app.query = "" }
        BackHandler(enabled = app.backStack.size > 1) { app.pop() }

        if (app.showWelcome) {
            WelcomeScreen(onContinueWithoutSync = {
                app.finishOnboarding()
                app.showWelcome = false
            })
        } else {
            when (val s = screen) {
                is Screen.Editor -> TaskEditorScreen(app, s.taskId, s.presetListId)
                is Screen.NewList -> NewListScreen(app)
                is Screen.NewFilter -> NewFilterScreen(app)
                is Screen.Settings -> SettingsScreen(app)
                is Screen.Appearance -> AppearanceScreen(app)
                is Screen.Main -> MainScreen(app)
            }
        }
    }
}
