package com.blackboxbench.reproduction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ReproducedApp() }
    }
}

@Composable
fun ReproducedApp() {
    val context = LocalContext.current
    val state = remember {
        GalleryState(context.applicationContext).also { LibraryHolder.library = it.library }
    }
    BenchmarkAppTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
            BackHandler(enabled = true) {
                state.pop()
            }
            val dark = state.current is Screen.Viewer ||
                state.current is Screen.Editor ||
                state.current is Screen.VideoViewer
            Box(
                Modifier
                    .fillMaxSize()
                    .background(if (dark) Color.Black else Color.White)
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                ) {
                if (state.locked) {
                    LockScreen(state) { state.locked = false }
                } else {
                    when (val screen = state.current) {
                        is Screen.Albums -> AlbumsScreen(state)
                        is Screen.Album -> AlbumScreen(state, screen.name, screen.isRecycle)
                        is Screen.Viewer -> ViewerScreen(state, screen.relPath, screen.album, screen.isRecycle)
                        is Screen.Editor -> EditorScreen(state, screen.relPath, screen.album)
                        is Screen.VideoViewer -> VideoScreen(state, screen.relPath, screen.album)
                        is Screen.Settings -> SettingsScreen(state)
                        is Screen.About -> AboutScreen(state)
                        is Screen.Search -> SearchScreen(state)
                        is Screen.FolderPicker -> AlbumsScreen(state)
                    }
                }
                ToastHost(state)
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.ToastHost(state: GalleryState) {
    val message = state.toast ?: return
    LaunchedEffect(message) {
        delay(1800)
        state.toast = null
    }
    Text(
        message,
        color = Color.White,
        fontSize = 15.sp,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 96.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xCC222222))
            .padding(horizontal = 18.dp, vertical = 10.dp)
    )
}
