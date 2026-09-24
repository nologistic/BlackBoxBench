package com.blackboxbench.reproduction

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier

sealed class Screen {
    object Folders : Screen()
    data class FolderMedia(val folder: String) : Screen()
    data class Viewer(val folder: String, val startIndex: Int, val slideshow: Boolean = false) : Screen()
    data class Editor(val mediaId: String) : Screen()
    object Settings : Screen()
    object Appearance : Screen()
    object About : Screen()
    object ExcludedFolders : Screen()
    object IncludedFolders : Screen()
}

class AppState(val repo: GalleryRepository) {
    val backStack = mutableStateListOf<Screen>(Screen.Folders)
    var showHiddenTemp = mutableStateOf(false)
    var showExcludedTemp = mutableStateOf(false)
    var allMediaView = mutableStateOf(false)

    val current: Screen get() = backStack.last()

    fun navigate(screen: Screen) {
        backStack.add(screen)
    }

    fun pop(): Boolean {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.size - 1)
            return true
        }
        return false
    }

    fun reset() {
        backStack.clear()
        backStack.add(Screen.Folders)
    }
}

class MainActivity : ComponentActivity() {
    private lateinit var appState: AppState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = GalleryRepository(applicationContext)
        repo.init()
        appState = AppState(repo)
        repo.defaultFolder.value?.let { default ->
            if (repo.folderNames(true, true).any { it.name == default }) {
                appState.backStack.add(Screen.FolderMedia(default))
            }
        }
        setContent {
            BenchmarkAppTheme {
                Surface(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                    ReproducedApp(appState) {
                        Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}

@Composable
fun ReproducedApp(app: AppState, toast: (String) -> Unit) {
    BackHandler {
        if (!app.pop()) {
            // let system handle exit
        }
    }
    when (val screen = app.current) {
        is Screen.Folders -> FolderGridScreen(app, toast)
        is Screen.FolderMedia -> MediaGridScreen(app, screen.folder, toast)
        is Screen.Viewer -> ViewerScreen(app, screen.folder, screen.startIndex, screen.slideshow, toast)
        is Screen.Editor -> EditorScreen(app, screen.mediaId, toast)
        is Screen.Settings -> SettingsScreen(app, toast)
        is Screen.Appearance -> AppearanceScreen(app)
        is Screen.About -> AboutScreen(app)
        is Screen.ExcludedFolders -> ExcludedFoldersScreen(app)
        is Screen.IncludedFolders -> IncludedFoldersScreen(app)
    }
}
