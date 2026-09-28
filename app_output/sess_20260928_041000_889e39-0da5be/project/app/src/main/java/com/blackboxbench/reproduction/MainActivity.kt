package com.blackboxbench.reproduction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var model: AppModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        model = AppModel(applicationContext)
        model.player.restore()
        setContent {
            VinylTheme(model.settings.primary, model.settings.accent) {
                ReproducedApp(model)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (::model.isInitialized) model.player.persist()
    }
}

@Composable
fun ReproducedApp(model: AppModel) {
    if (!model.onboardingDone) {
        OnboardingScreen { model.finishOnboarding() }
        return
    }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var songMenu by remember { mutableStateOf<Song?>(null) }
    var songMenuPlaylist by remember { mutableStateOf<Long?>(null) }
    var playlistMenu by remember { mutableStateOf<Long?>(null) }
    var mainMenu by remember { mutableStateOf(false) }
    var npMenu by remember { mutableStateOf(false) }
    var addToPlaylist by remember { mutableStateOf<Song?>(null) }
    var newPlaylistRequest by remember { mutableStateOf<List<Long>?>(null) }
    var detailsFor by remember { mutableStateOf<Song?>(null) }
    var shareFor by remember { mutableStateOf<Song?>(null) }
    var ringtoneFor by remember { mutableStateOf<Song?>(null) }
    var deleteFor by remember { mutableStateOf<Song?>(null) }
    var renamePlaylist by remember { mutableStateOf<Long?>(null) }
    var deletePlaylist by remember { mutableStateOf<Long?>(null) }
    var saveAsPlaylist by remember { mutableStateOf<Long?>(null) }
    var sleepDialog by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(model.drawerOpen) {
        if (model.drawerOpen) drawerState.open() else drawerState.close()
    }
    LaunchedEffect(drawerState.currentValue) {
        model.drawerOpen = drawerState.currentValue == DrawerValue.Open
    }
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(2600)
            toast = null
        }
    }

    BackHandler(enabled = true) {
        when {
            model.nowPlayingExpanded -> model.nowPlayingExpanded = false
            model.drawerOpen -> model.drawerOpen = false
            !model.pop() -> {}
        }
    }

    Box(Modifier.fillMaxSize().background(Color.White)) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = true,
            drawerContent = {
                ModalDrawerSheet(modifier = Modifier.width(304.dp)) {
                    DrawerContent(model)
                }
            }
        ) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    when (val screen = model.current) {
                        is Screen.Library -> LibraryScaffold(
                            model = model,
                            onMainMenu = { mainMenu = true },
                            onSongMore = { songMenuPlaylist = null; songMenu = it },
                            onPlaylistMore = { playlistMenu = it }
                        )
                        is Screen.Folders -> FoldersScreen(model) { model.pop() }
                        is Screen.Settings -> Column(Modifier.fillMaxSize()) {
                            DetailTopBar("设置", { model.pop() })
                            SettingsScreen(model) { model.push(Screen.Equalizer) }
                        }
                        is Screen.About -> AboutScreen { model.pop() }
                        is Screen.Equalizer -> EqualizerScreen { model.pop() }
                        is Screen.TagEditor -> if (model.tagEditorSong != null) {
                            TagEditorScreen(model.tagEditorSong!!, { model.pop() }) { updated ->
                                model.repo.updateSong(updated)
                                model.pop()
                            }
                        } else {
                            EmptyState("没有歌曲")
                        }
                        is Screen.Search -> SearchScreen(
                            model,
                            onBack = { model.pop() },
                            onSongClick = { s, ids -> model.player.playSong(s.id, ids) },
                            onSongMore = { songMenu = it },
                            onAlbumClick = { model.push(Screen.AlbumDetail(it)) },
                            onArtistClick = { model.push(Screen.ArtistDetail(it)) }
                        )
                        is Screen.AlbumDetail -> Column(Modifier.fillMaxSize()) {
                            DetailTopBar(
                                screen.album,
                                { model.pop() },
                                actions = {
                                    BarShuffle { model.player.playQueue(model.repo.albums.first { it.title == screen.album }.songs.map { it.id }.shuffled(), 0) }
                                    BarPlay { model.player.playQueue(model.repo.albums.first { it.title == screen.album }.songs.map { it.id }, 0) }
                                }
                            )
                            AlbumDetailContent(model, screen.album, { s, ids -> model.player.playSong(s.id, ids) }, { songMenu = it })
                        }
                        is Screen.ArtistDetail -> Column(Modifier.fillMaxSize()) {
                            DetailTopBar(screen.name, { model.pop() })
                            ArtistDetailContent(model, screen.name, { model.push(Screen.AlbumDetail(it)) }, { s, ids -> model.player.playSong(s.id, ids) }, { songMenu = it })
                        }
                        is Screen.GenreDetail -> Column(Modifier.fillMaxSize()) {
                            DetailTopBar(
                                screen.name,
                                { model.pop() },
                                actions = {
                                    BarShuffle { model.player.playQueue(model.repo.genres.first { it.name == screen.name }.songs.map { it.id }.shuffled(), 0) }
                                }
                            )
                            GenreDetailContent(model, screen.name, { s, ids -> model.player.playSong(s.id, ids) }, { songMenu = it })
                        }
                        is Screen.PlaylistDetail -> {
                            val pl = model.repo.playlistById(screen.id)
                            Column(Modifier.fillMaxSize()) {
                                DetailTopBar(
                                    pl?.name ?: "播放列表",
                                    { model.pop() },
                                    actions = {
                                        BarShuffle { model.player.playQueue(pl!!.songIds.shuffled(), 0) }
                                        BarPlay { model.player.playQueue(pl!!.songIds, 0) }
                                    }
                                )
                                PlaylistDetailContent(model, screen.id, { s, ids -> model.player.playSong(s.id, ids) }, { songMenuPlaylist = screen.id; songMenu = it })
                            }
                        }
                        is Screen.SmartDetail -> {
                            val songs = model.repo.smartPlaylist(screen.kind).songs
                            Column(Modifier.fillMaxSize()) {
                                DetailTopBar(
                                    screen.kind.label,
                                    { model.pop() },
                                    actions = {
                                        BarShuffle { model.player.playQueue(songs.map { it.id }.shuffled(), 0) }
                                        BarPlay { model.player.playQueue(songs.map { it.id }, 0) }
                                    }
                                )
                                SmartDetailContent(model, screen.kind, { s, ids -> model.player.playSong(s.id, ids) }, { songMenu = it })
                            }
                        }
                    }
                }
                MiniPlayer(model) { model.nowPlayingExpanded = true }
            }
        }

        if (model.nowPlayingExpanded) {
            NowPlayingScreen(
                model = model,
                onClose = { model.nowPlayingExpanded = false },
                onMore = { npMenu = true },
                onSongMore = { songMenu = it },
                onQueueMore = { songMenu = it },
                onOpenQueueItem = { s -> model.player.playSong(s.id, model.player.queue) }
            )
        }

        // ---- popup menus ----
        AnchoredMenu(
            visible = songMenu != null,
            topOffset = 360,
            onDismiss = { songMenu = null; songMenuPlaylist = null }
        ) {
            val song = songMenu ?: return@AnchoredMenu
            if (songMenuPlaylist != null) {
                MenuText("从播放列表中移除") { model.repo.removeFromPlaylist(songMenuPlaylist!!, song.id); songMenu = null; songMenuPlaylist = null }
            }
            MenuText("作为下一首播放") { model.player.playNext(song.id); songMenu = null }
            MenuText("加入播放队列") { model.player.addToQueue(song.id); songMenu = null }
            MenuText("加入播放列表...") { addToPlaylist = song; songMenu = null }
            MenuText("查看专辑") { model.push(Screen.AlbumDetail(song.album)); songMenu = null }
            MenuText("查看艺术家") { model.push(Screen.ArtistDetail(song.artist)); songMenu = null }
            MenuText("分享") { shareFor = song; songMenu = null }
            MenuText("音乐标签编辑器") { model.tagEditorSong = song; model.push(Screen.TagEditor); songMenu = null }
            MenuText("详情") { detailsFor = song; songMenu = null }
            MenuText("设为铃声") { ringtoneFor = song; songMenu = null }
            MenuText("从设备中删除", destructive = true) { deleteFor = song; songMenu = null }
        }

        AnchoredMenu(
            visible = playlistMenu != null,
            topOffset = 200,
            onDismiss = { playlistMenu = null }
        ) {
            val id = playlistMenu ?: return@AnchoredMenu
            MenuText("作为下一首播放") { model.repo.playlistById(id)?.songIds?.firstOrNull()?.let { model.player.playNext(it) }; playlistMenu = null }
            MenuText("加入播放队列") { model.repo.playlistById(id)?.songIds?.forEach { model.player.addToQueue(it) }; playlistMenu = null }
            MenuText("加入播放列表...") { playlistMenu = null }
            MenuText("重命名") { renamePlaylist = id; playlistMenu = null }
            MenuText("另存为") { saveAsPlaylist = id; playlistMenu = null }
            MenuText("删除", destructive = true) { deletePlaylist = id; playlistMenu = null }
        }

        AnchoredMenu(
            visible = mainMenu,
            topOffset = 150,
            onDismiss = { mainMenu = false }
        ) {
            MenuText("随机播放所有歌曲") { model.player.playQueue(model.repo.songs.map { it.id }.shuffled(), 0); mainMenu = false }
            MenuText("排序方式") { mainMenu = false }
            MenuText("显示页脚") { mainMenu = false }
            MenuText("着色页脚") { mainMenu = false }
        }

        AnchoredMenu(
            visible = npMenu,
            topOffset = 150,
            onDismiss = { npMenu = false }
        ) {
            MenuText("清空播放队列") { model.player.clearQueue(); npMenu = false }
            MenuText("保存播放队列") { newPlaylistRequest = model.player.queue; npMenu = false }
            MenuText("睡眠定时器") { sleepDialog = true; npMenu = false }
            MenuText("均衡器") { model.nowPlayingExpanded = false; model.push(Screen.Equalizer); npMenu = false }
        }

        // ---- dialogs ----
        addToPlaylist?.let { song ->
            AddToPlaylistDialog(
                model,
                onNew = { addToPlaylist = null; newPlaylistRequest = listOf(song.id) },
                onPick = { id -> model.repo.addToPlaylist(id, listOf(song.id)); addToPlaylist = null },
                onDismiss = { addToPlaylist = null }
            )
        }

        newPlaylistRequest?.let { ids ->
            TextPromptDialog(
                title = "新建播放列表",
                confirmLabel = "创建",
                onConfirm = { name ->
                    if (name.isNotBlank()) {
                        model.repo.createPlaylist(name, ids)
                        toast = "${ids.size} 首歌曲已加入到播放列表 $name。"
                    }
                    newPlaylistRequest = null
                },
                onDismiss = { newPlaylistRequest = null }
            )
        }

        detailsFor?.let { SongDetailsDialog(it) { detailsFor = null } }
        shareFor?.let { ShareDialog { shareFor = null } }
        ringtoneFor?.let { ConfirmDialog("设为铃声", "在设置铃声前请允许Vinyl音乐播放器在下一屏修改系统设置。", "确定", onConfirm = { ringtoneFor = null }, onDismiss = { ringtoneFor = null }) }
        deleteFor?.let { song -> ConfirmDialog("从设备中删除", "确定要从设备中删除“${song.title}”吗？", "删除", onConfirm = { deleteFor = null }, onDismiss = { deleteFor = null }) }

        renamePlaylist?.let { id ->
            TextPromptDialog(
                title = "重命名播放列表",
                initial = model.repo.playlistById(id)?.name ?: "",
                confirmLabel = "重命名",
                onConfirm = { name -> if (name.isNotBlank()) model.repo.renamePlaylist(id, name); renamePlaylist = null },
                onDismiss = { renamePlaylist = null }
            )
        }
        saveAsPlaylist?.let { id ->
            TextPromptDialog(
                title = "另存为",
                initial = (model.repo.playlistById(id)?.name ?: "播放列表") + " 副本",
                confirmLabel = "保存",
                onConfirm = { name -> if (name.isNotBlank()) model.repo.duplicatePlaylist(id, name); saveAsPlaylist = null },
                onDismiss = { saveAsPlaylist = null }
            )
        }
        deletePlaylist?.let { id ->
            ConfirmDialog("删除播放列表", "确定要删除“${model.repo.playlistById(id)?.name ?: ""}”吗？", "删除", onConfirm = { model.repo.deletePlaylist(id); deletePlaylist = null }, onDismiss = { deletePlaylist = null })
        }
        if (sleepDialog) {
            SleepTimerDialog(
                onSet = { minutes -> model.player.setSleepTimer(minutes * 60); sleepDialog = false; toast = "睡眠定时器已设置为 $minutes 分钟" },
                onDismiss = { sleepDialog = false }
            )
        }

        toast?.let { msg ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                Surface(
                    color = Color(0xFF323232),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(bottom = 96.dp, start = 16.dp, end = 16.dp)
                ) {
                    Text(msg, color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
                }
            }
        }
    }
}

@Composable
private fun LibraryScaffold(
    model: AppModel,
    onMainMenu: () -> Unit,
    onSongMore: (Song) -> Unit,
    onPlaylistMore: (Long) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { model.drawerOpen = true }) {
                Icon(Icons.Filled.Menu, contentDescription = "菜单", tint = Color.White)
            }
            Text("Vinyl Music Player", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            IconButton(onClick = { model.push(Screen.Search) }) {
                Icon(Icons.Filled.Search, contentDescription = "搜索", tint = Color.White)
            }
            IconButton(onClick = onMainMenu) {
                Icon(Icons.Filled.MoreVert, contentDescription = "更多", tint = Color.White)
            }
        }
        val tabs = model.visibleTabs()
        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary)) {
            tabs.forEach { t ->
                val selected = t == model.tab
                Column(
                    Modifier
                        .weight(1f)
                        .clickable { model.selectTab(t) }
                        .padding(top = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        t.label,
                        color = if (selected) Color.White else Color(0xCCFFFFFF),
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(if (selected) Color.White else Color.Transparent)
                    )
                }
            }
        }
        Box(Modifier.weight(1f)) {
            LibraryContent(
                model = model,
                onSongClick = { s, ids -> model.player.playSong(s.id, ids) },
                onSongMore = onSongMore,
                onAlbumClick = { model.push(Screen.AlbumDetail(it)) },
                onArtistClick = { model.push(Screen.ArtistDetail(it)) },
                onGenreClick = { model.push(Screen.GenreDetail(it)) },
                onSmartClick = { model.push(Screen.SmartDetail(it)) },
                onPlaylistClick = { model.push(Screen.PlaylistDetail(it)) },
                onPlaylistMore = onPlaylistMore,
                onShuffleAll = { model.player.playQueue(model.repo.songs.map { it.id }.shuffled(), 0) }
            )
        }
    }
}

@Composable
private fun DrawerContent(model: AppModel) {
    val song = model.player.current
    Box(
        Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable { if (song != null) model.nowPlayingExpanded = true }
    ) {
        VinylCover(
            album = song?.album ?: "Vinyl",
            modifier = Modifier.fillMaxSize(),
            cornerRadius = 0.dp
        )
        if (song != null) {
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(song.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(song.artist, color = Color(0xCCFFFFFF), fontSize = 13.sp)
            }
        }
    }
    DrawerItem(R.drawable.ic_queue_music, "媒体库", selected = model.current is Screen.Library) { model.drawerOpen = false; model.selectTab(model.tab) }
    DrawerItem(R.drawable.ic_folder, "文件夹", selected = model.current is Screen.Folders) { model.drawerOpen = false; model.push(Screen.Folders) }
    DrawerItem(R.drawable.ic_clock, "重新扫描媒体库") { model.drawerOpen = false }
    Spacer(Modifier.height(8.dp))
    Box(Modifier.fillMaxWidth().height(1.dp).background(VinylColors.Divider))
    DrawerItem(Icons.Filled.Settings, "设置") { model.drawerOpen = false; model.push(Screen.Settings) }
    DrawerItem(Icons.Filled.Info, "关于", iconVector = true) { model.drawerOpen = false; model.push(Screen.About) }
}

@Composable
private fun DrawerItem(res: Int, label: String, selected: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(if (selected) Color(0xFFF0F0F0) else Color.Transparent)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(res), contentDescription = null, tint = if (selected) MaterialTheme.colorScheme.secondary else Color(0xFF616161))
        Spacer(Modifier.width(24.dp))
        Text(label, fontSize = 15.sp, color = Color(0xFF212121))
    }
}

@Composable
private fun DrawerItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean = false, iconVector: Boolean = true, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(if (selected) Color(0xFFF0F0F0) else Color.Transparent)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) MaterialTheme.colorScheme.secondary else Color(0xFF616161))
        Spacer(Modifier.width(24.dp))
        Text(label, fontSize = 15.sp, color = Color(0xFF212121))
    }
}

@Composable
private fun MiniPlayer(model: AppModel, onClick: () -> Unit) {
    val song = model.player.current ?: return
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5))
            .navigationBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFFF5F5F5))
                .clickable { onClick() }
                .height(56.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "展开", tint = Color(0xFF616161))
            Spacer(Modifier.width(12.dp))
            Text(song.title, color = Color(0xFF212121), fontSize = 15.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = { model.player.togglePlayPause() }) {
                if (model.player.isPlaying) {
                    Icon(painterResource(R.drawable.ic_pause), contentDescription = "暂停", tint = Color(0xFF616161))
                } else {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "播放", tint = Color(0xFF616161))
                }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(Color(0xFFE0E0E0))
        ) {
            val dur = song.durationSec.coerceAtLeast(1)
            Box(
                Modifier
                    .fillMaxWidth((model.player.positionSec / dur).coerceIn(0f, 1f))
                    .height(2.dp)
                    .background(MaterialTheme.colorScheme.secondary)
            )
        }
    }
}

@Composable
private fun BarShuffle(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(painterResource(R.drawable.ic_shuffle), contentDescription = "随机", tint = Color.White)
    }
}

@Composable
private fun BarPlay(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(Icons.Filled.PlayArrow, contentDescription = "播放", tint = Color.White)
    }
}

@Composable
private fun AnchoredMenu(visible: Boolean, topOffset: Int, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    if (!visible) return
    Popup(
        alignment = Alignment.TopEnd,
        offset = IntOffset(x = -8, y = topOffset),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Surface(color = Color.White, shape = RoundedCornerShape(2.dp), shadowElevation = 8.dp, modifier = Modifier.width(280.dp)) {
            Column(Modifier.padding(vertical = 8.dp), content = content)
        }
    }
}

@Composable
private fun MenuText(text: String, destructive: Boolean = false, onClick: () -> Unit) {
    Text(
        text,
        color = if (destructive) MaterialTheme.colorScheme.secondary else Color(0xFF212121),
        fontSize = 15.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 13.dp)
    )
}

@Composable
private fun AddToPlaylistDialog(model: AppModel, onNew: () -> Unit, onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加到播放列表", fontSize = 18.sp) },
        text = {
            Column {
                Text(
                    "新建播放列表...",
                    color = Color(0xFF212121),
                    fontSize = 15.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNew() }
                        .padding(vertical = 12.dp)
                )
                model.repo.playlists.forEach { p ->
                    Text(
                        p.name,
                        color = Color(0xFF212121),
                        fontSize = 15.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(p.id) }
                            .padding(vertical = 12.dp)
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = VinylColors.SecondaryText) } },
        containerColor = Color.White
    )
}

@Composable
private fun SongDetailsDialog(song: Song, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("详情", fontSize = 18.sp) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                DetailRow("文件路径", "/storage/emulated/0/Music/${song.artist}/${song.album}/${song.title}.flac")
                DetailRow("文件大小", "8.4 MB")
                DetailRow("格式", "Flac")
                DetailRow("比特率", "1000 kb/s")
                DetailRow("采样率", "44100 Hz")
                DetailRow("Added", "2026-09-28")
                DetailRow("Modified", "2026-09-24")
                DetailRow("音轨", song.trackNo.toString())
                DetailRow("光碟号", "0")
                DetailRow("标题", song.title)
                DetailRow("艺术家", song.artist)
                DetailRow("专辑", song.album)
                DetailRow("专辑艺术家", song.artist)
                DetailRow("流派", song.genre)
                DetailRow("年份", song.year.toString())
                DetailRow("长度", formatDuration(song.durationSec))
                DetailRow("ReplayGain", "--")
                DetailRow("ReplayGain peak", "--")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("确定", color = MaterialTheme.colorScheme.secondary) } },
        containerColor = Color.White
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.padding(vertical = 4.dp)) {
        Text("$label: ", color = Color(0xFF212121), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text(value, color = Color(0xFF424242), fontSize = 13.sp)
    }
}

@Composable
private fun ShareDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("您想分享哪些内容？", fontSize = 18.sp) },
        text = {
            Column {
                Text("音频文件", fontSize = 15.sp, color = Color(0xFF212121), modifier = Modifier.fillMaxWidth().clickable {} .padding(vertical = 12.dp))
                Text("当前正在播放 的 sample_audio 。", fontSize = 14.sp, color = VinylColors.SecondaryText)
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = VinylColors.SecondaryText) } },
        containerColor = Color.White
    )
}

@Composable
private fun SleepTimerDialog(onSet: (Int) -> Unit, onDismiss: () -> Unit) {
    var minutes by remember { mutableStateOf(30) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("睡眠定时器", fontSize = 18.sp) },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                androidx.compose.foundation.Canvas(Modifier.size(160.dp)) {
                    val stroke = 10.dp.toPx()
                    drawArc(
                        color = Color(0xFFE0E0E0),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                    drawArc(
                        color = Color(0xFFFF4081),
                        startAngle = 135f,
                        sweepAngle = 270f * (minutes / 60f).coerceIn(0f, 1f),
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                }
                Text("$minutes 分", fontSize = 26.sp, fontWeight = FontWeight.Medium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { if (minutes > 5) minutes -= 5 }) { Text("-5", color = MaterialTheme.colorScheme.secondary) }
                    TextButton(onClick = { if (minutes < 60) minutes += 5 }) { Text("+5", color = MaterialTheme.colorScheme.secondary) }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { if (minutes > 0) minutes = 0 }) {
                    Checkbox(checked = minutes == 0, onCheckedChange = { if (it) minutes = 0 })
                    Text("播完当前音乐", fontSize = 14.sp)
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSet(if (minutes == 0) 1 else minutes) }) { Text("设置", color = MaterialTheme.colorScheme.secondary) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = VinylColors.SecondaryText) } },
        containerColor = Color.White
    )
}
