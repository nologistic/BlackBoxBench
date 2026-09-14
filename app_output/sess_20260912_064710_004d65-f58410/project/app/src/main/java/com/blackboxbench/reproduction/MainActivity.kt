package com.blackboxbench.reproduction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.WHITE
        window.navigationBarColor = android.graphics.Color.rgb(250, 245, 252)
        setContent { ReproducedApp() }
    }
}

enum class AppScreen { MAIN, EDITOR, SETTINGS, FILTER }

data class EditorDraft(
    val id: String,
    val isNew: Boolean,
    val title: String,
    val notes: String,
    val due: String,
    val priority: String,
    val listId: String,
    val tags: List<String>,
    val repeat: String,
    val subtasks: List<SubTaskItem>
)

fun TaskItem.toDraft(isNew: Boolean = false) = EditorDraft(
    id, isNew, title, notes, due, priority, listId, tags, repeat, subtasks
)

@Composable
fun ReproducedApp() {
    BenchmarkAppTheme {
        val context = androidx.compose.ui.platform.LocalContext.current
        val repository = remember { TaskRepository(context) }
        val initial = remember { repository.load() }
        var onboarded by remember { mutableStateOf(repository.isOnboarded()) }
        var lists by remember { mutableStateOf(initial.first) }
        var tasks by remember { mutableStateOf(initial.second) }
        var screen by remember { mutableStateOf(AppScreen.MAIN) }
        var editor by remember { mutableStateOf<EditorDraft?>(null) }
        var settingsPage by remember { mutableStateOf<String?>(null) }

        fun saveAll(newTasks: List<TaskItem>, newLists: List<TaskListItem> = lists) {
            tasks = newTasks
            lists = newLists
            repository.save(newLists, newTasks)
        }

        if (!onboarded) {
            OnboardingScreen {
                repository.setOnboarded(true)
                onboarded = true
            }
            return@BenchmarkAppTheme
        }

        BackHandler(screen != AppScreen.MAIN || settingsPage != null) {
            if (settingsPage != null) settingsPage = null else screen = AppScreen.MAIN
        }

        when (screen) {
            AppScreen.MAIN -> MainTasksScreen(
                lists = lists,
                tasks = tasks,
                onTasksChange = { saveAll(it) },
                onOpenTask = {
                    editor = it.toDraft()
                    screen = AppScreen.EDITOR
                },
                onAddTask = { listId ->
                    editor = EditorDraft(
                        UUID.randomUUID().toString(), true, "", "", "", "low",
                        listId ?: lists.firstOrNull()?.id.orEmpty(), emptyList(), "", emptyList()
                    )
                    screen = AppScreen.EDITOR
                },
                onOpenSettings = {
                    settingsPage = null
                    screen = AppScreen.SETTINGS
                },
                onOpenFilter = { screen = AppScreen.FILTER }
            )
            AppScreen.EDITOR -> editor?.let { draft ->
                TaskEditorScreen(
                    draft = draft,
                    lists = lists,
                    onCancel = { screen = AppScreen.MAIN },
                    onSave = { saved, maybeNewList ->
                        val updatedLists = if (maybeNewList != null && lists.none { it.id == maybeNewList.id }) lists + maybeNewList else lists
                        val updatedTasks = if (draft.isNew) tasks + saved else tasks.map { if (it.id == saved.id) saved else it }
                        saveAll(updatedTasks, updatedLists)
                        screen = AppScreen.MAIN
                    },
                    onDelete = {
                        saveAll(tasks.filterNot { it.id == draft.id })
                        screen = AppScreen.MAIN
                    }
                )
            }
            AppScreen.SETTINGS -> {
                if (settingsPage == null) {
                    SettingsHome(
                        onBack = { screen = AppScreen.MAIN },
                        onOpen = { settingsPage = it }
                    )
                } else {
                    SettingsDetail(page = settingsPage!!, onBack = { settingsPage = null })
                }
            }
            AppScreen.FILTER -> FilterEditorScreen { screen = AppScreen.MAIN }
        }
    }
}

@Composable
fun OnboardingScreen(onContinue: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color.White).padding(horizontal = 28.dp, vertical = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(0.9f))
        Box(
            modifier = Modifier.size(132.dp).background(Color(0xFF2196F3), RoundedCornerShape(34.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("✓", color = Color.White, fontSize = 86.sp, fontWeight = FontWeight.Light)
        }
        Spacer(Modifier.height(28.dp))
        Text("Tasks.org", fontSize = 30.sp, fontWeight = FontWeight.Medium)
        Text("任务 · 提醒 · 清单", color = Color(0xFF66666E), fontSize = 16.sp)
        Spacer(Modifier.weight(1.2f))
        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth().height(58.dp),
            shape = RoundedCornerShape(30.dp)
        ) { Text("添加账户", fontSize = 17.sp) }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(58.dp),
            shape = RoundedCornerShape(30.dp)
        ) { Text("继续且不使用同步", fontSize = 17.sp) }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = {}, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("导入 Tasks.org 备份", fontSize = 16.sp)
        }
    }
}
