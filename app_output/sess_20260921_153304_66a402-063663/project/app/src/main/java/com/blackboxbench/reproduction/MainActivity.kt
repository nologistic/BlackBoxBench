package com.blackboxbench.reproduction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val controller = AppController(applicationContext)
        controller.repo.seedIfNeeded()
        controller.repo.trashDisabled = !controller.settings.trashEnabled.get()
        setContent { ReproducedApp(controller) }
    }
}

@Composable
fun ReproducedApp(controller: AppController) {
    BenchmarkAppTheme {
        val isViewer = controller.current is Screen.Viewer
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = if (isViewer) androidx.compose.ui.graphics.Color.Black else GalleryBackground
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val locked = controller.settings.protectApp.get() &&
                    controller.settings.anyPasswordSet() &&
                    !controller.unlocked
                Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                    if (locked) {
                        LockScreen(controller.settings) { controller.unlocked = true }
                    } else {
                        MainShell(controller)
                    }
                }
                controller.message?.let { ToastHost(it) { controller.message = null } }
            }
        }
    }
}

@Composable
fun MainShell(c: AppController) {
    c.settings.revision.value
    BackHandler(enabled = c.stack.size > 1) { c.pop() }
    val screen = c.current
    remember(screen) { screen }
    when (screen) {
        Screen.Albums -> AlbumsScreen(c)
        Screen.Timeline -> TimelineScreen(c)
        is Screen.Album -> AlbumGridScreen(c, screen.path)
        is Screen.Viewer -> ViewerScreen(c, screen.albumPath, screen.ids, screen.index)
        Screen.Trash -> TrashScreen(c)
        Screen.Settings -> SettingsScreen(c)
        Screen.Appearance -> AppearanceScreen(c)
        Screen.About -> AboutScreen(c)
        is Screen.ManageFolders -> ManageFoldersScreen(c, screen.excluded)
        Screen.Slideshow -> AlbumsScreen(c)
    }
}
