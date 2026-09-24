package com.blackboxbench.reproduction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(applicationContext)
        setContent { ReproducedApp() }
    }
}

private fun viewToString(v: ViewRef): String = when (v) {
    is ViewRef.MyTasks -> "my"
    is ViewRef.BuiltinFilter -> v.kind
    is ViewRef.CustomFilter -> "filter:${v.id}"
    is ViewRef.TagView -> "tag:${v.id}"
    is ViewRef.PlaceView -> "place:${v.id}"
    is ViewRef.ListView -> "list:${v.id}"
}

private fun viewFromString(s: String): ViewRef = when {
    s == "my" -> ViewRef.MyTasks
    s == "today" -> ViewRef.BuiltinFilter("today")
    s == "recent" -> ViewRef.BuiltinFilter("recent")
    s.startsWith("filter:") -> ViewRef.CustomFilter(s.removePrefix("filter:"))
    s.startsWith("tag:") -> ViewRef.TagView(s.removePrefix("tag:"))
    s.startsWith("place:") -> ViewRef.PlaceView(s.removePrefix("place:"))
    s.startsWith("list:") -> ViewRef.ListView(s.removePrefix("list:"))
    else -> ViewRef.MyTasks
}

@Composable
fun ReproducedApp() {
    BenchmarkAppTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            var screen by remember {
                mutableStateOf(if (Store.onboarded.value) "list" else "welcome")
            }
            var currentView by remember {
                mutableStateOf<ViewRef>(
                    if (Store.boolSetting("open_last_list", true))
                        viewFromString(Store.setting("last_view", "my"))
                    else ViewRef.MyTasks
                )
            }
            var editTaskId by remember { mutableStateOf("") }
            var editIsNew by remember { mutableStateOf(false) }

            fun selectView(v: ViewRef) {
                currentView = v
                Store.setSetting("last_view", viewToString(v))
                screen = "list"
            }

            fun newTask() {
                val t = Task(
                    listId = when (val cv = currentView) {
                        is ViewRef.ListView -> cv.id
                        else -> Store.setting("default_list", "lst_default")
                    },
                    priority = Priority.of(Store.setting("default_priority", "1").toIntOrNull() ?: 1),
                    dueDate = if ((currentView as? ViewRef.BuiltinFilter)?.kind == "today")
                        Dates.fmtDate(Dates.today()) else "",
                    repeat = Store.setting("default_repeat", "")
                )
                (currentView as? ViewRef.TagView)?.let { t.tagIds.add(it.id) }
                    ?: Store.setting("default_tag", "").takeIf { it.isNotEmpty() }?.let { t.tagIds.add(it) }
                (currentView as? ViewRef.PlaceView)?.let { t.locationId = it.id }
                Store.addTask(t)
                editTaskId = t.id; editIsNew = true
                screen = "edit_task"
            }

            when (screen) {
                "welcome" -> WelcomeScreen(
                    onAddAccount = { screen = "add_account" },
                    onContinueOffline = { Store.onboarded.value = true; Store.save(); screen = "list" },
                    onImportBackup = { Store.onboarded.value = true; Store.save(); screen = "list" }
                )
                "list" -> ListScreen(
                    view = currentView,
                    onSelectView = { selectView(it) },
                    onOpenTask = { id -> editTaskId = id; editIsNew = false; screen = "edit_task" },
                    onNewTask = { newTask() },
                    onOpenSettings = { screen = "settings" },
                    onOpenEntitySettings = {
                        when (val v = currentView) {
                            is ViewRef.TagView -> screen = "tag_editor:${v.id}"
                            is ViewRef.ListView -> screen = "list_editor:${v.id}"
                            is ViewRef.CustomFilter -> screen = "filter_editor:${v.id}"
                            is ViewRef.PlaceView -> screen = "place_editor:${v.id}"
                            else -> screen = "settings"
                        }
                    },
                    onAddFilter = { screen = "filter_editor:new" },
                    onAddTag = { screen = "tag_editor:new" },
                    onAddPlace = { screen = "place_picker:create" },
                    onAddList = { screen = "list_editor:new" },
                    onOpenNotifications = { screen = "settings:notifications" }
                )
                "edit_task" -> TaskEditScreen(
                    taskId = editTaskId,
                    isNew = editIsNew,
                    onBack = { screen = "list" },
                    onPickTags = { screen = "task_tags" },
                    onPickLocation = { screen = "place_picker:attach" }
                )
                "task_tags" -> TaskTagPickerScreen(taskId = editTaskId, onBack = { screen = "edit_task" })
                "settings" -> SettingsMainScreen(
                    onBack = { screen = "list" },
                    open = { key ->
                        screen = when (key) {
                            "local_account" -> "settings:local_account"
                            "add_account" -> "add_account"
                            else -> "settings:$key"
                        }
                    }
                )
                "settings:appearance" -> AppearanceScreen(onBack = { screen = "settings" })
                "settings:notifications" -> NotificationsScreen(onBack = { screen = "settings" })
                "settings:defaults" -> TaskDefaultsScreen(onBack = { screen = "settings" })
                "settings:list_options" -> TaskListOptionsScreen(onBack = { screen = "settings" })
                "settings:edit_options" -> EditScreenOptionsScreen(onBack = { screen = "settings" })
                "settings:datetime" -> DateTimeSettingsScreen(onBack = { screen = "settings" })
                "settings:drawer" -> DrawerSettingsScreen(onBack = { screen = "settings" })
                "settings:backup" -> BackupScreen(onBack = { screen = "settings" })
                "settings:plugins" -> PluginsScreen(onBack = { screen = "settings" })
                "settings:advanced" -> AdvancedScreen(onBack = { screen = "settings" })
                "settings:about" -> AboutScreen(onBack = { screen = "settings" })
                "settings:local_account" -> LocalAccountScreen(onBack = { screen = "settings" })
                "add_account" -> AddAccountScreen(
                    onBack = { screen = if (Store.onboarded.value) "settings" else "welcome" },
                    onCalDav = { screen = "caldav" }
                )
                "caldav" -> CalDavScreen(onBack = { screen = "add_account" })
                else -> when {
                    screen == "tag_editor:new" -> EntityEditorScreen(
                        title = "新建标签", name = "新建标签", color = -1, icon = "",
                        showLocalBanner = false, canDelete = false,
                        onBack = { screen = "list" },
                        onSave = { n, c, i -> Store.addTag(n, c, i) },
                        onDelete = {},
                        onAddAccount = { screen = "add_account" }
                    )
                    screen.startsWith("tag_editor:") -> {
                        val id = screen.removePrefix("tag_editor:")
                        val tag = Store.tagById(id)
                        if (tag == null) screen = "list" else EntityEditorScreen(
                            title = "编辑标签", name = tag.name, color = tag.color, icon = tag.icon,
                            showLocalBanner = false, canDelete = true,
                            onBack = { screen = "list" },
                            onSave = { n, c, i -> tag.name = n; tag.color = c; tag.icon = i; Store.save() },
                            onDelete = { Store.deleteTag(tag) },
                            onAddAccount = { screen = "add_account" }
                        )
                    }
                    screen == "list_editor:new" -> EntityEditorScreen(
                        title = "新建清单", name = "新建清单", color = -1, icon = "",
                        showLocalBanner = true, canDelete = false,
                        onBack = { screen = "list" },
                        onSave = { n, c, i -> Store.addList(n, c, i) },
                        onDelete = {},
                        onAddAccount = { screen = "add_account" }
                    )
                    screen.startsWith("list_editor:") -> {
                        val id = screen.removePrefix("list_editor:")
                        val l = Store.listById(id)
                        if (l == null) screen = "list" else EntityEditorScreen(
                            title = "编辑清单", name = l.name, color = l.color, icon = l.icon,
                            showLocalBanner = true, canDelete = true,
                            onBack = { screen = "list" },
                            onSave = { n, c, i -> l.name = n; l.color = c; l.icon = i; Store.save() },
                            onDelete = { Store.deleteList(l) },
                            onAddAccount = { screen = "add_account" }
                        )
                    }
                    screen == "filter_editor:new" -> FilterEditorScreen(filterId = null, onBack = { screen = "list" })
                    screen.startsWith("filter_editor:") -> FilterEditorScreen(
                        filterId = screen.removePrefix("filter_editor:"),
                        onBack = { screen = "list" }
                    )
                    screen.startsWith("place_editor:") -> {
                        val id = screen.removePrefix("place_editor:")
                        val p = Store.placeById(id)
                        if (p == null) screen = "list" else EntityEditorScreen(
                            title = "编辑地点", name = p.name, color = -1, icon = "",
                            showLocalBanner = false, canDelete = true,
                            onBack = { screen = "list" },
                            onSave = { n, _, _ -> p.name = n; Store.save() },
                            onDelete = { Store.places.remove(p); Store.save() },
                            onAddAccount = { screen = "add_account" }
                        )
                    }
                    screen == "place_picker:create" -> PlacePickerScreen(
                        showExisting = false,
                        onPick = { id -> selectView(ViewRef.PlaceView(id)) },
                        onBack = { screen = "list" }
                    )
                    screen == "place_picker:attach" -> PlacePickerScreen(
                        showExisting = true,
                        onPick = { id ->
                            Store.tasks.firstOrNull { it.id == editTaskId }?.let { t ->
                                t.locationId = id; Store.touch(t)
                            }
                            screen = "edit_task"
                        },
                        onBack = { screen = "edit_task" }
                    )
                    else -> screen = "list"
                }
            }
        }
    }
}
