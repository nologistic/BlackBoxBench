package com.blackboxbench.reproduction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ReproducedApp() }
    }
}

sealed class Screen {
    object Onboarding : Screen()
    object Home : Screen()
    object Search : Screen()
    object SettingsRoot : Screen()
    object MediaFolders : Screen()
    object About : Screen()
    object Streams : Screen()
    object History : Screen()
    data class Info(val id: String) : Screen()
    data class Browser(val folder: String?) : Screen()
    data class PlaylistDetail(val id: String) : Screen()
    data class SettingsPage(val key: String) : Screen()
    data class Player(val id: String) : Screen()
    data class AudioPlayer(val id: String) : Screen()
}

@Composable
fun ReproducedApp() {
    val context = LocalContext.current
    remember { Store.init(context); true }
    val systemDark = isSystemInDarkTheme()
    val dark = when (Store.themeMode.value) {
        1 -> false
        2 -> true
        else -> systemDark
    }
    BenchmarkAppTheme(dark) {
        Surface(modifier = Modifier.fillMaxSize(), color = LocalVlcPalette.current.background) {
            val stack = remember {
                mutableStateListOf<Screen>(if (Store.onboardingDone.value) Screen.Home else Screen.Onboarding)
            }
            val homeTab = remember { mutableStateOf(0) }
            val videoSubState = remember { mutableStateOf(0) }
            val audioSubState = remember { mutableStateOf(0) }
            val push: (Screen) -> Unit = { stack.add(it) }
            val pop: () -> Unit = { if (stack.size > 1) stack.removeAt(stack.size - 1) }

            BackHandler(enabled = stack.size > 1) { pop() }

            when (val screen = stack.last()) {
                Screen.Onboarding -> OnboardingFlow(
                    onDone = { stack.clear(); stack.add(Screen.Home) },
                    onOpenFolders = { push(Screen.MediaFolders) },
                )

                Screen.Home -> HomeShell(
                    tab = homeTab.value,
                    onTab = { homeTab.value = it },
                    videoSub = videoSubState.value,
                    onVideoSub = { videoSubState.value = it },
                    audioSub = audioSubState.value,
                    onAudioSub = { audioSubState.value = it },
                    onOpenItem = { item ->
                        push(if (item.kind == MediaKind.VIDEO) Screen.Player(item.id) else Screen.AudioPlayer(item.id))
                    },
                    onOpenInfo = { push(Screen.Info(it.id)) },
                    onOpenPlaylist = { push(Screen.PlaylistDetail(it)) },
                    onOpenSettings = { push(Screen.SettingsRoot) },
                    onOpenAbout = { push(Screen.About) },
                    onOpenStreams = { push(Screen.Streams) },
                    onOpenHistory = { push(Screen.History) },
                    onOpenSearch = { push(Screen.Search) },
                )

                Screen.Search -> SearchScreen(
                    onBack = pop,
                    onOpenItem = { push(if (it.kind == MediaKind.VIDEO) Screen.Player(it.id) else Screen.AudioPlayer(it.id)) },
                    onMenu = { push(Screen.Info(it.id)) },
                )

                Screen.SettingsRoot -> SettingsScreen(
                    onBack = pop,
                    onOpen = { push(Screen.SettingsPage(it)) },
                    onOpenFolders = { push(Screen.MediaFolders) },
                )

                is Screen.SettingsPage -> SettingsPage(screen.key, pop) { push(Screen.MediaFolders) }

                Screen.MediaFolders -> MediaFoldersScreen(pop)

                Screen.About -> AboutScreen(pop)

                Screen.Streams -> StreamsScreen(pop)

                Screen.History -> HistoryScreen(
                    onBack = pop,
                    onOpenItem = { push(if (it.kind == MediaKind.VIDEO) Screen.Player(it.id) else Screen.AudioPlayer(it.id)) },
                    onClear = { Store.history.clear(); Store.persist() },
                )

                is Screen.Info -> Library.byId(screen.id)?.let { item ->
                    MediaInfoScreen(
                        item = item,
                        onBack = pop,
                        onPlay = {
                            push(if (item.kind == MediaKind.VIDEO) Screen.Player(item.id) else Screen.AudioPlayer(item.id))
                        },
                    )
                }

                is Screen.Browser -> FileBrowserScreen(
                    folder = screen.folder,
                    onBack = pop,
                    onOpenFolder = { push(Screen.Browser(it)) },
                    onOpenItem = { push(if (it.kind == MediaKind.VIDEO) Screen.Player(it.id) else Screen.AudioPlayer(it.id)) },
                    onMenu = { push(Screen.Info(it.id)) },
                )

                is Screen.PlaylistDetail -> Store.playlist(screen.id)?.let { pl ->
                    PlaylistDetailScreen(
                        pl = pl,
                        onBack = pop,
                        onPlay = { push(if (it.kind == MediaKind.VIDEO) Screen.Player(it.id) else Screen.AudioPlayer(it.id)) },
                    )
                }

                is Screen.Player -> Library.byId(screen.id)?.let { item ->
                    PlayerScreen(item = item, onExit = pop, onOpenInfo = { push(Screen.Info(item.id)) })
                }

                is Screen.AudioPlayer -> Library.byId(screen.id)?.let { item ->
                    AudioPlayerScreen(item = item, onBack = pop)
                }
            }
        }
    }
}

@Composable
fun HomeShell(
    tab: Int,
    onTab: (Int) -> Unit,
    videoSub: Int,
    onVideoSub: (Int) -> Unit,
    audioSub: Int,
    onAudioSub: (Int) -> Unit,
    onOpenItem: (MediaItem) -> Unit,
    onOpenInfo: (MediaItem) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenStreams: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSearch: () -> Unit,
) {
    val p = LocalVlcPalette.current
    var overflow by remember { mutableStateOf(false) }
    var displaySettings by remember { mutableStateOf(false) }
    var menuItem by remember { mutableStateOf<MediaItem?>(null) }
    var newPlaylistItem by remember { mutableStateOf<MediaItem?>(null) }
    var browserStack by remember { mutableStateOf<List<String>>(emptyList()) }

    BackHandler(
        enabled = browserStack.isNotEmpty() || overflow || displaySettings || menuItem != null || newPlaylistItem != null,
    ) {
        when {
            newPlaylistItem != null -> newPlaylistItem = null
            menuItem != null -> menuItem = null
            displaySettings -> displaySettings = false
            overflow -> overflow = false
            else -> browserStack = browserStack.dropLast(1)
        }
    }

    Box(Modifier.fillMaxSize().background(p.background)) {
        Column(Modifier.fillMaxSize()) {
            val actions = when (tab) {
                0, 1 -> listOf(
                    TopAction(VlcIcon.SEARCH, onOpenSearch),
                    TopAction(VlcIcon.LIST, onClick = { displaySettings = true }),
                    TopAction(VlcIcon.MORE, onClick = { overflow = true }),
                )
                3 -> listOf(
                    TopAction(VlcIcon.SEARCH, onOpenSearch),
                    TopAction(VlcIcon.LIST, onClick = { displaySettings = true }),
                    TopAction(VlcIcon.MORE, onClick = { overflow = true }),
                )
                else -> listOf(TopAction(VlcIcon.MORE, onClick = { overflow = true }))
            }
            TopBar(actions = actions)
            Box(Modifier.weight(1f)) {
                when (tab) {
                    0 -> {
                        val items = Library.videos.filter { !Store.onlyFavorites.value || Store.isFavorite(it.id) }
                        VideoTabContent(
                            listLayout = Store.listLayout.value,
                            items = items,
                            onlyFavorites = Store.onlyFavorites.value,
                            onPlay = onOpenItem,
                            onMenu = { menuItem = it },
                            onOpenPlaylistsSubTab = { onVideoSub(1) },
                            subTab = videoSub,
                            onSubTab = { onVideoSub(it) },
                        )
                    }
                    1 -> AudioTabContent(
                        subTabIndex = audioSub,
                        onSubTab = { onAudioSub(it) },
                        onPlayTrack = onOpenItem,
                        onMenu = { menuItem = it },
                        onPlayAlbum = { Library.audios.firstOrNull()?.let(onOpenItem) },
                        onMenuAlbum = { Library.audios.firstOrNull()?.let { menuItem = it } },
                    )
                    2 -> BrowseTab(
                        onOpenFolder = { browserStack = listOf(it) },
                        onOpenInternal = { browserStack = listOf("") },
                    )
                    3 -> if (Store.playlists.isEmpty()) {
                        EmptyState("未找到播放列表。")
                    } else {
                        PlaylistGrid(Store.playlists.toList()) { onOpenPlaylist(it.id) }
                    }
                    else -> MoreTab(
                        onOpenSettings = onOpenSettings,
                        onOpenAbout = onOpenAbout,
                        onOpenStreams = onOpenStreams,
                        onOpenHistory = onOpenHistory,
                        onPlay = onOpenItem,
                    )
                }
                if (tab == 0 || tab == 1 || tab == 3) {
                    Box(Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 18.dp)) {
                        Fab(if (tab == 1) VlcIcon.SHUFFLE else VlcIcon.PLAY) {
                            when (tab) {
                                1 -> Library.audios.firstOrNull()?.let(onOpenItem)
                                3 -> Store.playlists.firstOrNull()?.let { onOpenPlaylist(it.id) }
                                else -> Library.videos.firstOrNull()?.let(onOpenItem)
                            }
                        }
                    }
                }
            }
            if (NowPlaying.item.value != null && NowPlaying.item.value?.kind == MediaKind.AUDIO) {
                MiniPlayer(onOpen = { NowPlaying.item.value?.let(onOpenItem) })
            }
            VlcBottomNav(tab) { onTab(it) }
        }

        if (overflow) {
            OverflowMenu(
                onDisplaySettings = { overflow = false; displaySettings = true },
                onRefresh = { overflow = false },
                onDismiss = { overflow = false },
            )
        }
        if (displaySettings) DisplaySettingsSheet { displaySettings = false }
        menuItem?.let { item ->
            MediaContextSheet(
                item = item,
                onDismiss = { menuItem = null },
                onPlay = { onOpenItem(item) },
                onInfo = { onOpenInfo(item) },
                onAddToPlaylist = { newPlaylistItem = item },
                onFavorite = { Store.toggleFavorite(item.id) },
                onDelete = { },
                onBrowse = { browserStack = listOf(item.folder) },
            )
        }
        newPlaylistItem?.let { item ->
            NewPlaylistSheet(
                item = item,
                playlistId = Store.playlists.firstOrNull()?.id,
                onDismiss = { newPlaylistItem = null },
                onCreated = { },
            )
        }
        if (browserStack.isNotEmpty()) {
            FileBrowserScreen(
                folder = browserStack.last().ifEmpty { null },
                onBack = { browserStack = browserStack.dropLast(1) },
                onOpenFolder = { browserStack = browserStack + it },
                onOpenItem = onOpenItem,
                onMenu = { menuItem = it },
            )
        }
    }
}

@Composable
private fun MiniPlayer(onOpen: () -> Unit) {
    val p = LocalVlcPalette.current
    val item = NowPlaying.item.value ?: return
    Column {
        HLine()
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(p.surface)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onOpen,
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)).background(p.chip),
                contentAlignment = Alignment.Center,
            ) { VlcIconView(VlcIcon.MUSIC, 22.dp, p.textSecondary) }
            androidx.compose.foundation.layout.Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.fileName, color = p.textPrimary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.durationLabel, color = p.textSecondary, fontSize = 12.sp)
            }
            IconTap(if (NowPlaying.playing.value) VlcIcon.PAUSE else VlcIcon.PLAY, 24.dp, p.textPrimary) {
                NowPlaying.playing.value = !NowPlaying.playing.value
            }
        }
    }
}
