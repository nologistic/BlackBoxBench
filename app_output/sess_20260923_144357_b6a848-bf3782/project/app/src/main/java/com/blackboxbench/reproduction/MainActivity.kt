package com.blackboxbench.reproduction

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.parseColor("#3F51B5")
        window.navigationBarColor = android.graphics.Color.parseColor("#F9F9FF")
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        setContent { ReproducedApp() }
    }
}

@Composable
fun ReproducedApp() {
    val context = LocalContext.current
    val state = remember { AppState(context.applicationContext) }
    LaunchedEffect(Unit) { state.load() }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            state.tick()
        }
    }
    if (!state.onboarded) {
        OnboardingScreen { state.onboarded = true; state.persist() }
    } else {
        MainShell(state)
    }
}

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pages = listOf(
        "Vinyl Music Player" to "一个快速、简洁的本地音乐播放器。",
        "你的音乐库" to "按歌曲、专辑、艺术家与音乐类型浏览本地音乐。",
        "开始使用" to "点击下面的按钮开始你的黑胶之旅。"
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        HorizontalPager(state = pagerState, modifier = Modifier.weight(3f)) { page ->
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                VinylIcon(size = 120.dp, tint = Color(0xFF3F51B5))
                Spacer(Modifier.height(24.dp))
                Text(pages[page].first, fontSize = 26.sp, color = Color(0xFF212121), fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(12.dp))
                Text(pages[page].second, fontSize = 15.sp, color = Color(0xFF757575))
            }
        }
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            repeat(pages.size) { index ->
                Box(
                    Modifier
                        .padding(horizontal = 6.dp)
                        .size(if (index == pagerState.currentPage) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(if (index == pagerState.currentPage) Color(0xFFE91E63) else Color(0xFFBDBDBD))
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = {
            if (pagerState.currentPage < pages.size - 1) {
                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            } else {
                onFinish()
            }
        }) {
            Text("GET STARTED", color = Color(0xFFE91E63), fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
fun MainShell(state: AppState) {
    val colors = rememberAppColors(state)
    val navigator = remember { Navigator() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    var detailsFor by remember { mutableStateOf<Track?>(null) }
    var deleteFor by remember { mutableStateOf<List<Track>?>(null) }
    var addToPlaylistFor by remember { mutableStateOf<List<Track>?>(null) }
    var newPlaylistFor by remember { mutableStateOf<List<Track>?>(null) }
    var ringtoneFor by remember { mutableStateOf<Track?>(null) }

    val actions = remember(state) {
        SongActions(
            playNext = { track -> state.addToQueue(listOf(track.id), next = true) },
            addToQueue = { track -> state.addToQueue(listOf(track.id)) },
            addToPlaylist = { track -> addToPlaylistFor = listOf(track) },
            viewAlbum = { track -> navigator.push(Screen.Album(track.album)) },
            viewArtist = { track -> navigator.push(Screen.Artist(track.artist)) },
            share = { track ->
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "audio/*"
                    putExtra(Intent.EXTRA_SUBJECT, track.title)
                    putExtra(Intent.EXTRA_TEXT, "${track.title} - ${track.artist}")
                }
                context.startActivity(Intent.createChooser(intent, "分享"))
            },
            tagEditor = { track -> navigator.push(Screen.TagEditor(track.id)) },
            details = { track -> detailsFor = track },
            setRingtone = { track -> ringtoneFor = track },
            delete = { track -> deleteFor = listOf(track) }
        )
    }

    BackHandler(enabled = navigator.canGoBack) { navigator.pop() }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerContent(state, colors) { target ->
                    scope.launch { drawerState.close() }
                    when (target) {
                        "library" -> navigator.reset()
                        "folders" -> navigator.push(Screen.Folders)
                        "rescan" -> state.scanning = true
                        "settings" -> navigator.push(Screen.Settings)
                        "about" -> navigator.push(Screen.About)
                    }
                }
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            containerColor = colors.background
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(colors.background)
            ) {
                Box(Modifier.weight(1f)) {
                    when (val screen = navigator.current) {
                    is Screen.Library -> {
                        Column(Modifier.fillMaxSize()) {
                            LibraryTopBar(state, colors, { scope.launch { drawerState.open() } }, { navigator.push(Screen.Search) })
                            if (state.tabVisibility.isNotEmpty()) {
                                LibraryTabRow(state, colors) { state.lastTab = it; state.persist() }
                            }
                            Box(Modifier.weight(1f)) {
                                LibraryContent(state, colors, navigator, actions) { navigator.push(Screen.Search) }
                            }
                        }
                    }
                    is Screen.Album -> {
                        val album = state.albums().firstOrNull { it.key == screen.key }
                        var menu by remember { mutableStateOf(false) }
                        SimpleTopBar(
                            title = screen.key,
                            colors = colors,
                            onBack = { navigator.pop() },
                            actions = {
                                TopBarAction({
                                    if (album != null) {
                                        val shuffled = album.tracks.shuffled()
                                        state.shuffle = true
                                        state.playTrack(shuffled.first().id, shuffled.map { it.id })
                                    }
                                }) { ShuffleGlyph(size = 22.dp, tint = Color.White) }
                                Box {
                                    TopBarAction({ menu = true }) { MoreVertGlyph(size = 22.dp, tint = Color.White) }
                                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                        MenuItem("作为下一首播放", colors) {
                                            menu = false
                                            album?.let { state.addToQueue(it.tracks.map { t -> t.id }, next = true) }
                                        }
                                        MenuItem("加入播放队列", colors) {
                                            menu = false
                                            album?.let { state.addToQueue(it.tracks.map { t -> t.id }) }
                                        }
                                        MenuItem("加入播放列表…", colors) {
                                            menu = false
                                            album?.let { addToPlaylistFor = it.tracks }
                                        }
                                        MenuItem("查看艺术家", colors) {
                                            menu = false
                                            album?.let { navigator.push(Screen.Artist(it.artist)) }
                                        }
                                        MenuItem("从设备中删除", colors, danger = true) {
                                            menu = false
                                            album?.let { deleteFor = it.tracks }
                                        }
                                    }
                                }
                            }
                        )
                        if (album != null) {
                            TrackListPage(
                                header = { AlbumHeader(album, colors) },
                                tracks = album.tracks,
                                state = state,
                                colors = colors,
                                actions = actions,
                                onOpenPlayer = { }
                            )
                        } else EmptyState("没有找到专辑", colors)
                    }
                    is Screen.Artist -> {
                        val artist = state.artists().firstOrNull { it.name == screen.name }
                        var menu by remember { mutableStateOf(false) }
                        var coloredFooter by remember { mutableStateOf(state.coloredFooter) }
                        SimpleTopBar(
                            title = screen.name,
                            colors = colors,
                            onBack = { navigator.pop() },
                            actions = {
                                TopBarAction({
                                    if (artist != null) {
                                        val shuffled = artist.tracks.shuffled()
                                        state.shuffle = true
                                        state.playTrack(shuffled.first().id, shuffled.map { it.id })
                                    }
                                }) { ShuffleGlyph(size = 22.dp, tint = Color.White) }
                                Box {
                                    TopBarAction({ menu = true }) { MoreVertGlyph(size = 22.dp, tint = Color.White) }
                                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                        MenuItem("作为下一首播放", colors) {
                                            menu = false
                                            artist?.let { state.addToQueue(it.tracks.map { t -> t.id }, next = true) }
                                        }
                                        MenuItem("加入播放队列", colors) {
                                            menu = false
                                            artist?.let { state.addToQueue(it.tracks.map { t -> t.id }) }
                                        }
                                        MenuItem("加入播放列表…", colors) {
                                            menu = false
                                            artist?.let { addToPlaylistFor = it.tracks }
                                        }
                                        MenuItem("设置艺术家图片", colors) { menu = false }
                                        MenuItem("重置艺术家图片", colors) { menu = false }
                                        MenuItem("着色页脚", colors) {
                                            menu = false
                                            coloredFooter = !coloredFooter
                                            state.coloredFooter = coloredFooter
                                            state.persist()
                                        }
                                        MenuItem("从设备中删除", colors, danger = true) {
                                            menu = false
                                            artist?.let { deleteFor = it.tracks }
                                        }
                                    }
                                }
                            }
                        )
                        if (artist != null) {
                            TrackListPage(
                                header = {
                                    Column {
                                        ArtistHeader(artist, colors)
                                        artist.albums.forEach { album ->
                                            Row(
                                                Modifier
                                                    .fillMaxWidth()
                                                    .background(colors.surface)
                                                    .clickable { navigator.push(Screen.Album(album.key)) }
                                                    .padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                CoverArt(album.title, size = 52.dp)
                                                Spacer(Modifier.width(16.dp))
                                                Column(Modifier.weight(1f)) {
                                                    Text(album.title, color = colors.onSurface, fontSize = 16.sp)
                                                    Text(
                                                        "${album.year} • ${album.tracks.size} 歌曲",
                                                        color = colors.secondaryText,
                                                        fontSize = 13.sp
                                                    )
                                                }
                                                ChevronGlyph(size = 20.dp, tint = colors.secondaryText)
                                            }
                                        }
                                        SectionHeader("歌曲", colors)
                                    }
                                },
                                tracks = artist.tracks,
                                state = state,
                                colors = colors,
                                actions = actions,
                                onOpenPlayer = { }
                            )
                        } else EmptyState("没有找到艺术家", colors)
                    }
                    is Screen.Genre -> {
                        val genre = state.genres().firstOrNull { it.name == screen.name }
                        SimpleTopBar(title = screen.name, colors = colors, onBack = { navigator.pop() })
                        if (genre != null) {
                            TrackListPage(
                                header = {
                                    Column(Modifier.background(colors.surface)) {
                                        Spacer(Modifier.height(16.dp))
                                        Text(
                                            genre.name,
                                            color = colors.onSurface,
                                            fontSize = 26.sp,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            "${genre.tracks.size} 歌曲 • ${formatDuration(genre.totalSeconds)}",
                                            color = colors.secondaryText,
                                            fontSize = 14.sp,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                        Spacer(Modifier.height(16.dp))
                                    }
                                },
                                tracks = genre.tracks,
                                state = state,
                                colors = colors,
                                actions = actions,
                                onOpenPlayer = { }
                            )
                        } else EmptyState("没有找到音乐类型", colors)
                    }
                    is Screen.Playlist -> PlaylistDetail(state, colors, navigator, actions, screen.id, snackbar)
                    is Screen.Smart -> {
                        val smart = state.smartPlaylists().firstOrNull { it.kind == screen.kind }
                        val tracks = state.tracksOf(smart?.trackIds ?: emptyList())
                        var menu by remember { mutableStateOf(false) }
                        SimpleTopBar(
                            title = smart?.name ?: "播放列表",
                            colors = colors,
                            onBack = { navigator.pop() },
                            actions = {
                                TopBarAction({
                                    if (tracks.isNotEmpty()) {
                                        state.playTrack(tracks.first().id, tracks.map { it.id })
                                    }
                                }) { PlayGlyph(size = 20.dp, tint = Color.White) }
                                Box {
                                    TopBarAction({ menu = true }) { MoreVertGlyph(size = 22.dp, tint = Color.White) }
                                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                        MenuItem("随机播放此播放列表", colors) {
                                            menu = false
                                            val shuffled = tracks.shuffled()
                                            if (shuffled.isNotEmpty()) state.playTrack(shuffled.first().id, shuffled.map { it.id })
                                        }
                                        MenuItem("加入播放队列", colors) { menu = false; state.addToQueue(tracks.map { it.id }) }
                                        MenuItem("作为下一首播放", colors) {
                                            menu = false
                                            state.addToQueue(tracks.map { it.id }, next = true)
                                        }
                                    }
                                }
                            }
                        )
                        TrackListPage(
                            header = { PlaylistHeader(tracks.size, tracks.sumOf { it.durationSec }, colors) },
                            tracks = tracks,
                            state = state,
                            colors = colors,
                            actions = actions,
                            onOpenPlayer = { }
                        )
                    }
                    is Screen.Search -> SearchScreen(state, colors, navigator, actions) { }
                    is Screen.Folders -> FoldersScreen(state, colors, navigator, actions)
                    is Screen.Settings -> SettingsScreen(state, colors, navigator)
                    is Screen.About -> AboutScreen(state, colors, navigator) {
                        state.onboarded = false
                        state.persist()
                    }
                    is Screen.TagEditor -> TagEditorScreen(state, colors, screen.trackId) { navigator.pop() }
                    is Screen.Player -> {}
                    }
                }
                if (state.currentTrack != null) {
                    MiniPlayer(state, colors) { navigator.push(Screen.Player) }
                }
            }
        }
    }

    if (navigator.current is Screen.Player) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xFF212121))
                .navigationBarsPadding()
        ) { NowPlayingScreen(state, navigator) }
    }

    detailsFor?.let { track ->
        DetailsDialog(track, colors, onDismiss = { detailsFor = null })
    }
    deleteFor?.let { list ->
        ConfirmDialog(
            title = "从设备中删除",
            message = if (list.size == 1) "是否从设备中删除 ${list.first().title}?" else "是否从设备中删除 ${list.size} 首歌曲?",
            confirmLabel = "删除",
            colors = colors,
            destructive = true,
            onDismiss = { deleteFor = null },
            onConfirm = {
                state.deleteTracks(list.map { it.id })
                deleteFor = null
                scope.launch { snackbar.showSnackbar("已删除 ${list.size} 首歌曲") }
            }
        )
    }
    addToPlaylistFor?.let { list ->
        AddToPlaylistDialog(
            state = state,
            colors = colors,
            onDismiss = { addToPlaylistFor = null },
            onPick = { playlist ->
                state.addToPlaylist(playlist.id, list.map { it.id })
                addToPlaylistFor = null
                scope.launch { snackbar.showSnackbar("${list.size} 首歌曲已加入到播放列表 ${playlist.name}。") }
            },
            onCreateNew = {
                newPlaylistFor = list
                addToPlaylistFor = null
            }
        )
    }
    newPlaylistFor?.let { list ->
        TextInputDialog(
            title = "新建播放列表",
            label = "播放列表名称",
            confirmLabel = "创建",
            colors = colors,
            onDismiss = { newPlaylistFor = null },
            onConfirm = { name ->
                val playlist = state.createPlaylist(name, list.map { it.id })
                newPlaylistFor = null
                scope.launch { snackbar.showSnackbar("${list.size} 首歌曲已加入到播放列表 ${playlist.name}。") }
            }
        )
    }
    ringtoneFor?.let { track ->
        OptionDialog(
            title = "设为铃声",
            options = listOf("电话铃声", "通知铃声", "闹钟铃声", "联系人铃声"),
            selected = null,
            colors = colors,
            onDismiss = { ringtoneFor = null },
            onSelect = { option ->
                ringtoneFor = null
                scope.launch { snackbar.showSnackbar("${track.title} 已设为$option") }
            }
        )
    }
}

@Composable
private fun DrawerContent(state: AppState, colors: AppColors, onSelect: (String) -> Unit) {
    Column(Modifier.fillMaxSize().background(colors.surface)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(Color(0xFF212121))
        ) {
            Row(Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.Bottom) {
                CoverArt(state.currentTrack?.album ?: "vinyl", size = 72.dp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        state.currentTrack?.title ?: "未在播放",
                        color = Color.White,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        state.currentTrack?.let { "${it.artist} • ${it.album}" } ?: "",
                        color = Color(0xFFBDBDBD),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        DrawerItem("媒体库", colors, selected = true) { onSelect("library") }
        Spacer(Modifier.height(8.dp))
        DrawerItem("文件夹", colors) { onSelect("folders") }
        DrawerItem("重新扫描媒体库", colors) { onSelect("rescan") }
        Spacer(Modifier.height(8.dp))
        DrawerItem("设置", colors) { onSelect("settings") }
        DrawerItem("关于", colors) { onSelect("about") }
    }
}

@Composable
private fun DrawerItem(label: String, colors: AppColors, selected: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) colors.accent.copy(alpha = 0.10f) else colors.surface)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (label) {
            "媒体库" -> LibraryGlyph(size = 24.dp, tint = if (selected) colors.accent else colors.secondaryText)
            "文件夹" -> FolderGlyph(size = 24.dp, tint = colors.secondaryText)
            "重新扫描媒体库" -> RescanGlyph(size = 24.dp, tint = colors.secondaryText)
            "设置" -> SettingsGlyph(size = 24.dp, tint = colors.secondaryText)
            else -> AboutGlyph(size = 24.dp, tint = colors.secondaryText)
        }
        Spacer(Modifier.width(24.dp))
        Text(
            label,
            color = if (selected) colors.accent else colors.onSurface,
            fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun LibraryTopBar(state: AppState, colors: AppColors, onMenu: () -> Unit, onSearch: () -> Unit) {
    var overflow by remember { mutableStateOf(false) }
    var rescan by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.primary)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TopBarAction({ onMenu() }) { MenuGlyph(size = 24.dp, tint = Color.White) }
        Text("Vinyl Music Player", color = Color.White, fontSize = 20.sp, modifier = Modifier.weight(1f))
        TopBarAction({ onSearch() })  { SearchFieldGlyph(size = 22.dp, tint = Color.White) }
        Box {
            TopBarAction({ overflow = true }) { MoreVertGlyph(size = 22.dp, tint = Color.White) }
            DropdownMenu(expanded = overflow, onDismissRequest = { overflow = false }) {
                MenuItem("随机播放所有歌曲", colors) {
                    overflow = false
                    val shuffled = state.tracks.shuffled()
                    if (shuffled.isNotEmpty()) {
                        state.shuffle = true
                        state.playTrack(shuffled.first().id, shuffled.map { it.id })
                    }
                }
                MenuItem("重新扫描媒体库", colors) { overflow = false; rescan = true }
                MenuItem("设置", colors) { overflow = false }
                MenuItem("关于", colors) { overflow = false }
            }
        }
    }
    if (rescan) {
        ConfirmDialog(
            title = "重新扫描媒体库",
            message = "This will clear and rebuild the app internal song database.\n\n" +
                "No song will be deleted during this operation, but this might clear the play history.\n\n" +
                "During a possible long duration, until the rescan is complete, you will not be able to use the app normally",
            confirmLabel = "重新扫描媒体库",
            colors = colors,
            onDismiss = { rescan = false },
            onConfirm = { rescan = false }
        )
    }
}

@Composable
private fun LibraryTabRow(state: AppState, colors: AppColors, onSelect: (Int) -> Unit) {
    val tabs = state.tabVisibility
    if (tabs.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.primary)
            .horizontalScroll(rememberScrollState())
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = state.lastTab.coerceIn(0, tabs.size - 1) == index
            Column(
                modifier = Modifier
                    .clickable { onSelect(index) }
                    .padding(horizontal = 22.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    tab,
                    color = if (selected) Color.White else Color.White.copy(alpha = 0.75f),
                    fontSize = 15.sp,
                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .width(if (selected) 56.dp else 0.dp)
                        .height(3.dp)
                        .background(if (selected) Color.White else Color.Transparent)
                )
            }
        }
    }
}

@Composable
private fun LibraryContent(
    state: AppState,
    colors: AppColors,
    navigator: Navigator,
    actions: SongActions,
    onSearch: () -> Unit
) {
    val tabs = state.tabVisibility
    val index = state.lastTab.coerceIn(0, (tabs.size - 1).coerceAtLeast(0))
    when (tabs.getOrNull(index)) {
        "歌曲" -> SongsTab(state, colors, actions) { navigator.push(Screen.Player) }
        "专辑" -> AlbumsTab(state, colors, navigator)
        "艺术家" -> ArtistsTab(state, colors, navigator)
        "音乐类型" -> GenresTab(state, colors, navigator)
        "播放列表" -> PlaylistsTab(state, colors, navigator, onCreatePlaylist = {
            state.createPlaylist("新建播放列表 ${state.playlists.size + 1}")
        })
        else -> SongsTab(state, colors, actions) { navigator.push(Screen.Player) }
    }
}

@Composable
fun PlaylistDetail(
    state: AppState,
    colors: AppColors,
    navigator: Navigator,
    actions: SongActions,
    playlistId: String,
    snackbar: SnackbarHostState
) {
    val playlist = state.playlists.firstOrNull { it.id == playlistId }
    if (playlist == null) {
        EmptyState("播放列表为空", colors)
        return
    }
    val tracks = state.tracksOf(playlist.trackIds.toList())
    var selection by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectionMode by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val actionsWithRemove = actions.copy(removeFromPlaylist = { track ->
        state.removeFromPlaylist(playlist.id, listOf(track.id))
    })

    Column(Modifier.fillMaxSize().background(colors.background)) {
        if (selectionMode) {
            SelectionTopBar(
                count = selection.size,
                colors = colors,
                onClose = { selectionMode = false; selection = emptySet() },
                onUndo = { selection = emptySet() },
                onAddToPlaylist = { },
                onOverflow = { confirmRemove = true }
            )
        } else {
            SimpleTopBar(
                title = playlist.name,
                colors = colors,
                onBack = { navigator.pop() },
                actions = {
                    TopBarAction({
                        if (tracks.isNotEmpty()) {
                            state.shuffle = true
                            val shuffled = tracks.shuffled()
                            state.playTrack(shuffled.first().id, shuffled.map { it.id })
                        }
                    }) { ShuffleGlyph(size = 22.dp, tint = Color.White) }
                    TopBarAction({
                        if (tracks.isNotEmpty()) state.playTrack(tracks.first().id, tracks.map { it.id })
                    }) { PlayGlyph(size = 20.dp, tint = Color.White) }
                    Box {
                        TopBarAction({ menu = true }) { MoreVertGlyph(size = 22.dp, tint = Color.White) }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            MenuItem("随机播放此播放列表", colors) {
                                menu = false
                                val shuffled = tracks.shuffled()
                                if (shuffled.isNotEmpty()) state.playTrack(shuffled.first().id, shuffled.map { it.id })
                            }
                            MenuItem("加入播放队列", colors) { menu = false; state.addToQueue(tracks.map { it.id }) }
                            MenuItem("重命名", colors) { menu = false }
                            MenuItem("删除", colors, danger = true) {
                                menu = false
                                state.deletePlaylist(playlist.id)
                                navigator.pop()
                            }
                        }
                    }
                }
            )
        }
        LazyColumnLike(
            header = {
                PlaylistHeader(tracks.size, tracks.sumOf { it.durationSec }, colors)
            },
            tracks = tracks,
            state = state,
            colors = colors,
            actions = actionsWithRemove,
            selection = selection,
            selectionMode = selectionMode,
            showDragHandle = true,
            onClick = { track ->
                if (selectionMode) {
                    selection = if (selection.contains(track.id)) selection - track.id else selection + track.id
                    if (selection.isEmpty()) selectionMode = false
                } else {
                    state.playTrack(track.id, tracks.map { it.id })
                }
            },
            onLongClick = { track ->
                selectionMode = true
                selection = selection + track.id
            }
        )
    }

    if (confirmRemove) {
        Box {
            DropdownMenu(expanded = true, onDismissRequest = { confirmRemove = false }) {
                MenuItem("全部选择", colors) {
                    confirmRemove = false
                    selection = tracks.map { it.id }.toSet()
                }
                MenuItem("从播放列表中移除", colors) {
                    confirmRemove = false
                    state.removeFromPlaylist(playlist.id, selection.toList())
                    scope.launch { snackbar.showSnackbar("已从播放列表中移除 ${selection.size} 首歌曲") }
                    selection = emptySet()
                    selectionMode = false
                }
                MenuItem("从设备中删除", colors, danger = true) {
                    confirmRemove = false
                    state.deleteTracks(selection.toList())
                    selection = emptySet()
                    selectionMode = false
                }
            }
        }
    }
}

@Composable
private fun LazyColumnLike(
    header: @Composable () -> Unit,
    tracks: List<Track>,
    state: AppState,
    colors: AppColors,
    actions: SongActions,
    selection: Set<String>,
    selectionMode: Boolean,
    showDragHandle: Boolean,
    onClick: (Track) -> Unit,
    onLongClick: (Track) -> Unit
) {
        androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxSize()) {
        item { header() }
        if (tracks.isEmpty()) {
            item {
                Text(
                    "播放列表为空",
                    color = colors.secondaryText,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        items(tracks, key = { it.id }) { track ->
            SongListRow(
                track = track,
                state = state,
                colors = colors,
                actions = actions,
                showDragHandle = showDragHandle,
                selected = selection.contains(track.id),
                selectionMode = selectionMode,
                onLongClick = { onLongClick(track) },
                onClick = { onClick(track) }
            )
            DividerLine(colors, indent = 96.dp)
        }
    }
}

@Composable
fun AddToPlaylistDialog(
    state: AppState,
    colors: AppColors,
    onDismiss: () -> Unit,
    onPick: (Playlist) -> Unit,
    onCreateNew: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("加入播放列表", fontSize = 20.sp, color = colors.onSurface) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onCreateNew() }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AddBoxGlyph(size = 22.dp, tint = colors.accent)
                    Spacer(Modifier.width(16.dp))
                    Text("新建播放列表…", color = colors.accent, fontSize = 16.sp)
                }
                state.playlists.forEach { playlist ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onPick(playlist) }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlaylistGlyph(size = 22.dp, tint = colors.secondaryText)
                        Spacer(Modifier.width(16.dp))
                        Text(playlist.name, color = colors.onSurface, fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消", color = colors.accent) } },
        containerColor = colors.surface
    )
}

@Composable
fun DetailsDialog(track: Track, colors: AppColors, onDismiss: () -> Unit) {
    val rows = listOf(
        "文件路径" to track.path,
        "大小" to formatSize(track.sizeBytes),
        "格式" to "MP3",
        "比特率" to "320 kb/s",
        "采样率" to "44100 Hz",
        "添加时间" to formatDate(track.addedAt),
        "修改时间" to formatDate(track.addedAt + 6L * 3600_000L),
        "音轨" to "${track.trackNo}",
        "光碟号" to "${track.discNo}",
        "标题" to track.title,
        "艺术家" to track.artist,
        "专辑" to track.album,
        "专辑艺术家" to track.albumArtist,
        "流派" to track.genre,
        "年份" to if (track.year > 0) "${track.year}" else "-",
        "长度" to track.durationLabel,
        "ReplayGain" to "-"
    )
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(track.title, fontSize = 20.sp, color = colors.onSurface) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                rows.forEach { (label, value) ->
                    Column(Modifier.padding(vertical = 6.dp)) {
                        Text(label, color = colors.secondaryText, fontSize = 13.sp)
                        Text(value, color = colors.onSurface, fontSize = 15.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("确定", color = colors.accent) } },
        containerColor = colors.surface
    )
}
