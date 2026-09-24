package com.blackboxbench.reproduction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    val state = remember { AppState(context) }
    val systemDark = isSystemInDarkTheme()
    val dark = state.themeMode == 1 || state.themeMode == 2 || (state.themeMode >= 3 && systemDark)

    LaunchedEffect(Unit) {
        while (true) {
            delay(250)
            state.tick(250)
        }
    }

    MaterialTheme(colorScheme = vinylScheme(state.primaryColor, state.accentColor, dark)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize()) {
                if (state.firstLaunch) {
                    OnboardingScreen(state)
                } else {
                    when (state.screen) {
                        Screen.SETTINGS -> SettingsScreen(state)
                        Screen.ABOUT -> AboutScreen(state)
                        Screen.INTRO -> IntroScreen(state)
                        Screen.NOW_PLAYING -> NowPlayingScreen(state)
                        Screen.TAG_EDITOR -> TagEditorScreen(state)
                        Screen.ALBUM_DETAIL -> AlbumDetailScreen(state)
                        Screen.ARTIST_DETAIL -> ArtistDetailScreen(state)
                        Screen.GENRE_DETAIL -> GenreDetailScreen(state)
                        Screen.PLAYLIST_DETAIL -> PlaylistDetailScreen(state)
                        Screen.FOLDERS -> FoldersHost(state)
                        else -> LibraryHost(state) { state.screen = Screen.NOW_PLAYING }
                    }
                }
                state.snackbar?.let { message ->
                    Snackbar(message) { state.snackbar = null }
                }
                AppDialogs(state)
            }
        }
    }
}

@Composable
private fun FoldersHost(state: AppState) {
    DrawerHost(
        open = state.drawerOpen,
        state = state,
        onClose = { state.drawerOpen = false },
        onNavigate = { route ->
            when (route) {
                "library" -> state.screen = state.lastLibraryScreen
                "folders" -> state.screen = Screen.FOLDERS
                "settings" -> state.screen = Screen.SETTINGS
                "about" -> state.screen = Screen.ABOUT
                "playlist" -> state.screen = Screen.PLAYLIST_DETAIL
            }
        }
    ) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            TopBarTitle(
                title = "文件夹",
                primary = Color(state.primaryColor),
                onMenu = { state.drawerOpen = true },
                onSearch = { state.searchActive = true; state.searchQuery = "" },
                onOverflow = { },
                overflowContent = { dismiss ->
                    OverflowMenu(
                        expanded = true,
                        entries = listOf(
                            MenuEntry("随机播放所有歌曲") {
                                state.playQueue(state.songs.map { it.id }, 0)
                            },
                            MenuEntry("网格尺寸", submenu = GRID_SIZES, onSubmenuSelect = { index ->
                                state.gridSize = index
                            }),
                        ),
                        onDismiss = dismiss
                    )
                }
            )
            Box(Modifier.weight(1f)) { FoldersView(state) }
            MiniPlayer(state) { state.screen = Screen.NOW_PLAYING }
        }
    }
    if (state.searchActive) SearchScreen(state)
}

@Composable
private fun Snackbar(message: String, onDismiss: () -> Unit) {
    LaunchedEffect(message) {
        delay(2200)
        onDismiss()
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Surface(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 90.dp),
            color = Color(0xFF323232),
            shape = RoundedCornerShape(4.dp),
            shadowElevation = 6.dp
        ) {
            Text(
                message,
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            )
        }
    }
}

@Composable
private fun OnboardingScreen(state: AppState) {
    var page by remember { mutableStateOf(0) }
    var permissionStep by remember { mutableStateOf(-1) }
    val accent = Color(state.accentColor)
    val permissions = listOf(
        "要允许Vinyl Music Player发送通知吗？",
        "要允许Vinyl Music Player访问此设备上的音乐和音频吗？",
        "要允许Vinyl Music Player访问此设备上的照片和视频吗？"
    )
    val titles = listOf("欢迎使用 Vinyl Music Player", "播放队列", "拖动调整播放顺序")
    val bodies = listOf(
        "一款简洁的本地音乐播放器：浏览歌曲、专辑、艺术家与播放列表，完全离线。",
        "上滑底部的迷你播放器即可展开正在播放页与播放队列。",
        "在播放队列中按住序号把手即可拖动调整曲目顺序。"
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(70.dp))
        Box(
            Modifier
                .size(220.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1E1E)),
            contentAlignment = Alignment.Center
        ) { VinylGlyph(Color(0xFF9E9E9E), 170.dp) }
        Spacer(Modifier.height(40.dp))
        Text(titles[page], fontSize = 23.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(16.dp))
        Text(
            bodies[page],
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.Center) {
            repeat(3) { index ->
                Box(
                    Modifier
                        .padding(6.dp)
                        .size(if (index == page) 12.dp else 9.dp)
                        .clip(CircleShape)
                        .background(if (index == page) accent else MaterialTheme.colorScheme.outline)
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("跳过", color = accent, fontSize = 16.sp, modifier = Modifier.clickable {
                permissionStep = 0
            })
            Text(
                if (page < 2) "下一页" else "GET STARTED",
                color = accent, fontSize = 16.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable {
                    if (page < 2) page++ else permissionStep = 0
                }
            )
        }
        Spacer(Modifier.height(48.dp))
    }

    if (permissionStep in permissions.indices) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(permissions[permissionStep], fontSize = 17.sp) },
            text = { Text("此权限用于本地音乐播放。") },
            confirmButton = {
                TextButton(onClick = {
                    if (permissionStep == permissions.lastIndex) {
                        permissionStep = -1
                        state.firstLaunch = false
                        state.screen = Screen.LIBRARY_SONGS
                        state.lastLibraryScreen = Screen.LIBRARY_SONGS
                        state.persist()
                    } else permissionStep++
                }) { Text("允许") }
            },
            dismissButton = {
                TextButton(onClick = {
                    if (permissionStep == permissions.lastIndex) {
                        permissionStep = -1
                        state.firstLaunch = false
                        state.screen = Screen.LIBRARY_SONGS
                        state.persist()
                    } else permissionStep++
                }) { Text("拒绝") }
            }
        )
    }
}
