package com.blackboxbench.reproduction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class Screen { LIST, EDIT, DETAIL, SETTINGS }

class LoopState {
    var screen by mutableStateOf(Screen.LIST)
    var detailId by mutableStateOf<String?>(null)
    var editIsNew by mutableStateOf(true)
    var editDraft by mutableStateOf<Habit?>(null)
    var reversed by mutableStateOf(false)
    var shortTap by mutableStateOf(false)
    var hideArchived by mutableStateOf(true)
    var hideCompleted by mutableStateOf(false)
    var sortMode by mutableStateOf("手动")
    var darkTheme by mutableStateOf(false)
    var selectionMode by mutableStateOf(false)
    var selected by mutableStateOf<Set<String>>(emptySet())
}

val HABIT_COLORS = listOf(
    "#E53935", "#F4511E", "#FB8C00", "#FFB300",
    "#43A047", "#7CB342", "#C0CA33", "#FDD835",
    "#00897B", "#00ACC1", "#1E88E5", "#3F51B5",
    "#E91E63", "#9C27B0", "#673AB7", "#3949AB",
    "#6D4C41", "#424242", "#757575", "#BDBDBD"
)

val PAGE_BG = Color(0xFFF2F1F6)
val BAR_BG = Color(0xFFEFEFF2)
val BAR_TEXT = Color(0xFF222222)
val BLUE = Color(0xFF1E88E5)

fun parseColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) {
    BLUE
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ReproducedApp() }
    }
}

@Composable
fun ReproducedApp() {
    val ctx = LocalContext.current
    val habits = remember { mutableStateListOf<Habit>().also { it.addAll(Repo.load(ctx)) } }
    val state = remember { LoopState() }

    fun persist() = Repo.save(ctx, habits)

    BenchmarkAppTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = PAGE_BG) {
            when (state.screen) {
                Screen.LIST -> ListScreen(
                    habits = habits,
                    state = state,
                    persist = { persist() },
                    onOpenDetail = { id ->
                        state.detailId = id
                        state.screen = Screen.DETAIL
                    },
                    onAdd = {
                        state.editIsNew = true
                        state.editDraft = null
                        state.screen = Screen.EDIT
                    },
                    onOpenSettings = { state.screen = Screen.SETTINGS }
                )
                Screen.EDIT -> EditScreen(
                    habits = habits,
                    state = state,
                    persist = { persist() },
                    onDone = { state.screen = Screen.LIST }
                )
                Screen.DETAIL -> DetailScreen(
                    habits = habits,
                    state = state,
                    persist = { persist() },
                    onBack = { state.screen = Screen.LIST }
                )
                Screen.SETTINGS -> SettingsScreen(
                    state = state,
                    onBack = { state.screen = Screen.LIST }
                )
            }
        }
    }
}
