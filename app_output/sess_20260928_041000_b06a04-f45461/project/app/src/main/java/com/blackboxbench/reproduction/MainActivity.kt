package com.blackboxbench.reproduction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ReproducedApp() }
    }
}

sealed class Screen {
    object Albums : Screen()
    data class Album(val name: String) : Screen()
    object AllMedia : Screen()
    object Settings : Screen()
    object About : Screen()
    data class Viewer(val ids: List<String>, val index: Int) : Screen()
    data class Editor(val id: String) : Screen()
}

@Composable
fun ReproducedApp() {
    BenchmarkAppTheme {
        val context = LocalContext.current
        val state = remember { GalleryState(context.applicationContext) }
        val stack = remember { mutableStateListOf<Screen>(Screen.Albums) }

        fun push(screen: Screen) { stack.add(screen) }
        fun pop() { if (stack.size > 1) stack.removeAt(stack.size - 1) }

        BackHandler(enabled = stack.size > 1) { pop() }

        Surface(modifier = Modifier.fillMaxSize().background(Color.White)) {
            when (val top = stack.last()) {
                is Screen.Albums -> GridScreen(
                    state = state, mode = GridMode.ALBUMS, albumName = "",
                    onOpenAlbum = { name -> push(Screen.Album(name)) },
                    onOpenViewer = { ids, index -> push(Screen.Viewer(ids, index)) },
                    onOpenSettings = { push(Screen.Settings) },
                    onOpenAbout = { push(Screen.About) },
                    onBack = { pop() },
                    onOpenAllMedia = { push(Screen.AllMedia) },
                )
                is Screen.Album -> {
                    val mode = when (top.name) {
                        GalleryState.FAVORITES -> GridMode.FAVORITES
                        GalleryState.RECYCLE -> GridMode.RECYCLE
                        else -> GridMode.ALBUM
                    }
                    GridScreen(
                        state = state, mode = mode, albumName = top.name,
                        onOpenAlbum = { name -> push(Screen.Album(name)) },
                        onOpenViewer = { ids, index -> push(Screen.Viewer(ids, index)) },
                        onOpenSettings = { push(Screen.Settings) },
                        onOpenAbout = { push(Screen.About) },
                        onBack = { pop() },
                    )
                }
                is Screen.AllMedia -> GridScreen(
                    state = state, mode = GridMode.ALL_MEDIA, albumName = "",
                    onOpenAlbum = { name -> push(Screen.Album(name)) },
                    onOpenViewer = { ids, index -> push(Screen.Viewer(ids, index)) },
                    onOpenSettings = { push(Screen.Settings) },
                    onOpenAbout = { push(Screen.About) },
                    onBack = { pop() },
                )
                is Screen.Settings -> SettingsScreen(state) { pop() }
                is Screen.About -> AboutScreen { pop() }
                is Screen.Viewer -> ViewerScreen(
                    state = state, ids = top.ids, startIndex = top.index,
                    onClose = { pop() },
                    onEdit = { id -> push(Screen.Editor(id)) },
                )
                is Screen.Editor -> EditorScreen(state, top.id) { pop() }
            }
        }
    }
}
