package com.blackboxbench.reproduction

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GalleryTheme { GalleryApp() } }
    }
}

@Composable
fun GalleryTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF5A4FCF),
            onPrimary = Color.White,
            secondary = Color(0xFF625B71),
            surface = Color(0xFFFDFBFF),
            background = Color(0xFFFDFBFF)
        ),
        content = content
    )
}

sealed interface Screen {
    data object Albums : Screen
    data class Album(val album: String) : Screen
    data object AllMedia : Screen
    data object Bin : Screen
    data class Viewer(val album: String, val index: Int, val fromBin: Boolean) : Screen
    data object Settings : Screen
    data object About : Screen
}

@Composable
fun GalleryApp() {
    val context = LocalContext.current
    val store = remember { GalleryStore(context.applicationContext) }
    val stack = remember { mutableStateListOf<Screen>(Screen.Albums) }

    fun push(screen: Screen) {
        stack.add(screen)
    }

    fun pop() {
        if (stack.size > 1) stack.removeAt(stack.size - 1)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        val screen = stack.last()
        val isViewer = screen is Screen.Viewer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isViewer) Color.Black else Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
            ) {
                when (screen) {
            is Screen.Albums -> AlbumsScreen(
                store = store,
                onOpenAlbum = { push(Screen.Album(it)) },
                onOpenViewer = { index -> push(Screen.Viewer("__all__", index, false)) },
                onOpenBin = { push(Screen.Bin) },
                onOpenSettings = { push(Screen.Settings) },
                onOpenAbout = { push(Screen.About) }
            )

            is Screen.Album -> AlbumScreen(
                store = store,
                album = screen.album,
                onBack = { pop() },
                onOpenViewer = { index -> push(Screen.Viewer(screen.album, index, false)) },
                onOpenBin = { push(Screen.Bin) },
                onOpenSettings = { push(Screen.Settings) }
            )

            is Screen.AllMedia -> AlbumScreen(
                store = store,
                album = null,
                onBack = { pop() },
                onOpenViewer = { index -> push(Screen.Viewer("", index, false)) },
                onOpenBin = { push(Screen.Bin) },
                onOpenSettings = { push(Screen.Settings) }
            )

            is Screen.Bin -> BinScreen(
                store = store,
                onBack = { pop() },
                onOpenViewer = { index -> push(Screen.Viewer("__bin__", index, true)) },
                onOpenSettings = { push(Screen.Settings) }
            )

            is Screen.Viewer -> ViewerScreen(
                store = store,
                album = screen.album,
                startIndex = screen.index,
                fromBin = screen.fromBin,
                onBack = { pop() },
                onOpenSettings = { push(Screen.Settings) }
            )

            is Screen.Settings -> SettingsScreen(
                store = store,
                onBack = { pop() },
                onOpenAbout = { push(Screen.About) }
            )

            is Screen.About -> AboutScreen(onBack = { pop() })
                }
            }
        }
    }
}

fun toast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
