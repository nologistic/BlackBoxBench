package com.blackboxbench.reproduction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.blackboxbench.reproduction.ui.MainActions
import com.blackboxbench.reproduction.ui.NewFilterScreen
import com.blackboxbench.reproduction.ui.NewListScreen
import com.blackboxbench.reproduction.ui.NewTagScreen
import com.blackboxbench.reproduction.ui.LocationPickScreen
import com.blackboxbench.reproduction.ui.SettingsScreen
import com.blackboxbench.reproduction.ui.TagSelectScreen
import com.blackboxbench.reproduction.ui.TaskEditScreen
import com.blackboxbench.reproduction.ui.TasksScreen
import com.blackboxbench.reproduction.ui.WelcomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        StateStore.init(this)
        setContent { ReproducedApp() }
    }
}

private sealed interface Screen {
    data object Welcome : Screen
    data object Main : Screen
    data class EditTask(val taskId: String?, val listId: String? = null, val tagId: String? = null) : Screen
    data class NewTag(val tagId: String?) : Screen
    data object NewListS : Screen
    data object NewFilterS : Screen
    data object LocationS : Screen
    data object SettingsS : Screen
    data object NotificationsS : Screen
    data class TagSelectS(val initial: List<String>) : Screen
}

private class NavStack {
    val screens = mutableStateListOf<Screen>()

    fun push(s: Screen) {
        screens.add(s)
    }

    fun pop() {
        if (screens.size > 1) screens.removeAt(screens.lastIndex)
    }

    val current: Screen get() = screens.last()
}

@Composable
fun ReproducedApp() {
    ThemeState.current = Settings.theme
    BenchmarkAppTheme(ThemeState.current) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val nav = remember {
                NavStack().apply {
                    push(if (Prefs.str("welcome_choice").isEmpty()) Screen.Welcome else Screen.Main)
                }
            }
            val view = remember {
                mutableStateOf(
                    if (Settings.openLastView) {
                        val ref = ViewRef.parse(Prefs.str("last_view", "mytasks"))
                        val valid = when (ref.type) {
                            "list" -> StateStore.lists.any { it.id == ref.id }
                            "tag" -> StateStore.tags.any { it.id == ref.id }
                            "filter" -> StateStore.filters.any { it.id == ref.id }
                            else -> true
                        }
                        if (valid) ref else ViewRef.MY_TASKS
                    } else ViewRef.MY_TASKS
                )
            }
            val tagResult = remember { mutableStateOf<((List<String>) -> Unit)?>(null) }

            BackHandler(enabled = nav.screens.size > 1 && nav.current !is Screen.EditTask && nav.current !is Screen.SettingsS && nav.current !is Screen.NotificationsS && nav.current !is Screen.TagSelectS && nav.current !is Screen.NewTag && nav.current !is Screen.NewFilterS) {
                nav.pop()
            }

            when (val s = nav.current) {
                is Screen.Welcome -> WelcomeScreen(onDone = {
                    nav.screens.clear()
                    nav.push(Screen.Main)
                })

                Screen.Main -> {
                    val actions = MainActions(
                        openSettings = { nav.push(Screen.SettingsS) },
                        openNotificationsSettings = { nav.push(Screen.NotificationsS) },
                        openTagSettings = { id -> nav.push(Screen.NewTag(id)) },
                        editTask = { id -> nav.push(Screen.EditTask(id)) },
                        newTask = {
                            val listId = if (view.value.type == "list") view.value.id else null
                            val tagId = if (view.value.type == "tag") view.value.id else null
                            nav.push(Screen.EditTask(null, listId, tagId))
                        },
                        newTag = { nav.push(Screen.NewTag(null)) },
                        newList = { nav.push(Screen.NewListS) },
                        newFilter = { nav.push(Screen.NewFilterS) },
                        pickLocation = { nav.push(Screen.LocationS) }
                    )
                    TasksScreen(view = view, actions = actions)
                    if (Settings.openLastView) Prefs.putStr("last_view", view.value.encode())
                }

                is Screen.EditTask -> TaskEditScreen(
                    taskId = s.taskId,
                    initialListId = s.listId,
                    initialTagId = s.tagId,
                    onBack = { nav.pop() },
                    onPickLocation = { nav.push(Screen.LocationS) },
                    onPickTags = { initial, cb ->
                        tagResult.value = cb
                        nav.push(Screen.TagSelectS(initial))
                    }
                )

                is Screen.NewTag -> NewTagScreen(
                    tagId = s.tagId,
                    onBack = { nav.pop() },
                    onSaved = { id ->
                        if (s.tagId == null) {
                            nav.pop()
                            view.value = ViewRef.tag(id)
                        } else {
                            nav.pop()
                        }
                    }
                )

                Screen.NewListS -> NewListScreen(
                    onBack = { nav.pop() },
                    onSaved = { id ->
                        nav.pop()
                        view.value = ViewRef.list(id)
                    }
                )

                Screen.NewFilterS -> NewFilterScreen(
                    onBack = { nav.pop() },
                    onSaved = { id ->
                        nav.pop()
                        view.value = ViewRef.filter(id)
                    }
                )

                Screen.LocationS -> LocationPickScreen(onBack = { nav.pop() })
                Screen.SettingsS -> SettingsScreen(onBack = { nav.pop() })
                Screen.NotificationsS -> SettingsScreen(onBack = { nav.pop() }, startPage = "notifications")

                is Screen.TagSelectS -> TagSelectScreen(
                    initial = s.initial,
                    onDone = { ids ->
                        nav.pop()
                        tagResult.value?.invoke(ids)
                        tagResult.value = null
                    }
                )
            }
        }
    }
}
