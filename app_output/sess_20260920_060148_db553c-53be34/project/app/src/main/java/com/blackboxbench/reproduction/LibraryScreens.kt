package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// ---------- 主界面 ----------

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(store: Store, nav: NavStack) {
    var tab by remember { mutableStateOf(if (store.getBool("rememberTab", true)) store.lastTab else 0) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var overflowOpen by remember { mutableStateOf(false) }
    var showNewPlaylist by remember { mutableStateOf(false) }
    var showRescan by remember { mutableStateOf(false) }
    var menuSong by remember { mutableStateOf<Song?>(null) }
    var addToPlaylistSong by remember { mutableStateOf<Song?>(null) }
    var detailsSong by remember { mutableStateOf<Song?>(null) }
    var deleteSongTarget by remember { mutableStateOf<Song?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }
    val selected = remember { mutableStateListOf<Int>() }
    var tabMenuSongAction by remember { mutableStateOf(0) }

    val songs = store.songs()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = Color.White, modifier = Modifier.width(300.dp)) {
                DrawerContent(store, nav, onRescan = { showRescan = true },
                    close = { scope.launch { drawerState.close() } })
            }
        }
    ) {
        Column(Modifier.fillMaxSize().background(Color.White)) {
            if (selected.isEmpty()) {
                // 普通工具栏
                Column(Modifier.background(Indigo).statusBarsPadding()) {
                    Row(
                        Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, null, tint = Color.White)
                        }
                        Text("Vinyl Music Player", color = Color.White, fontSize = 20.sp,
                            modifier = Modifier.weight(1f))
                        IconButton(onClick = { nav.push("search") }) {
                            Icon(Icons.Filled.Search, null, tint = Color.White)
                        }
                        Box {
                            IconButton(onClick = { overflowOpen = true }) {
                                Icon(Icons.Filled.MoreVert, null, tint = Color.White)
                            }
                            DropdownMenu(expanded = overflowOpen, onDismissRequest = { overflowOpen = false }) {
                                DropdownMenuItem(text = { Text("随机播放所有歌曲") }, onClick = {
                                    overflowOpen = false
                                    val s = store.songs()
                                    if (s.isNotEmpty()) Player.playList(s, s.random(), shuffled = true)
                                })
                                if (tab == 4) {
                                    DropdownMenuItem(text = { Text("新建播放列表") }, onClick = {
                                        overflowOpen = false; showNewPlaylist = true
                                    })
                                }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().height(48.dp)) {
                        listOf("歌曲", "专辑", "艺术家", "音乐类型", "播放列表").forEachIndexed { i, t ->
                            Column(
                                Modifier.weight(1f).fillMaxHeight().clickable { tab = i; store.saveLastTab(i) },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    Text(t, fontSize = 14.sp, maxLines = 1,
                                        color = if (tab == i) Color.White else Color(0xB3FFFFFF))
                                }
                                Box(Modifier.fillMaxWidth().height(3.dp).background(if (tab == i) Pink else Color.Transparent))
                            }
                        }
                    }
                }
            } else {
                // 多选 CAB
                Row(
                    Modifier.fillMaxWidth().background(Indigo).statusBarsPadding().height(56.dp).padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selected.clear() }) { Icon(Icons.Filled.Close, null, tint = Color.White) }
                    Text("已选择 ${selected.size}", color = Color.White, fontSize = 20.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = { }) { Icon(Icons.Filled.Refresh, null, tint = Color.White) }
                    IconButton(onClick = {
                        selected.forEach { id -> store.songs().firstOrNull { it.id == id }?.let { if (!store.favorites.contains(id)) store.toggleFavorite(id) } }
                        selected.clear()
                    }) { Icon(Icons.Filled.Add, null, tint = Color.White) }
                    Box {
                        var cabMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { cabMenu = true }) { Icon(Icons.Filled.MoreVert, null, tint = Color.White) }
                        DropdownMenu(expanded = cabMenu, onDismissRequest = { cabMenu = false }) {
                            DropdownMenuItem(text = { Text("加入播放列表…") }, onClick = {
                                cabMenu = false
                                selected.forEach { id -> if (!store.favorites.contains(id)) store.toggleFavorite(id) }
                                selected.clear()
                            })
                            DropdownMenuItem(text = { Text("全部选择") }, onClick = {
                                cabMenu = false; selected.clear(); selected.addAll(songs.map { it.id })
                            })
                            DropdownMenuItem(text = { Text("从设备中删除", color = Color(0xFFD32F2F)) }, onClick = {
                                cabMenu = false; selected.forEach { store.deleteSong(it) }; selected.clear()
                            })
                        }
                    }
                }
                Spacer(Modifier.height(48.dp))
            }

            Box(Modifier.weight(1f)) {
                when (tab) {
                    0 -> SongsTab(songs, selected, onMenu = { menuSong = it }, currentId = Player.current()?.id)
                    1 -> AlbumsTab(store, nav)
                    2 -> ArtistsTab(store, nav)
                    3 -> GenresTab(store, nav)
                    4 -> PlaylistsTab(store, nav)
                }
            }
            MiniPlayer(nav)
            toast?.let {
                Text(it, color = Color.White, fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth().background(Color(0xDD323232)).padding(12.dp))
                LaunchedEffect(it) { kotlinx.coroutines.delay(2000); toast = null }
            }
        }
    }

    menuSong?.let { song ->
        SongMenuDialog(song, store, nav, onAction = { act ->
            when {
                act == "addToPlaylist" -> addToPlaylistSong = song
                act == "details" -> detailsSong = song
                act == "delete" -> deleteSongTarget = song
                act.startsWith("toast:") -> toast = act.removePrefix("toast:")
            }
        }, onDismiss = { menuSong = null })
    }
    addToPlaylistSong?.let { song ->
        AddToPlaylistDialog(song, store, onNewPlaylist = { showNewPlaylist = true }) { addToPlaylistSong = null }
    }
    detailsSong?.let { SongDetailsDialog(it) { detailsSong = null } }
    deleteSongTarget?.let { song ->
        DeleteSongDialog(song, onConfirm = { store.deleteSong(song.id) }) { deleteSongTarget = null }
    }
    if (showNewPlaylist) NewPlaylistDialog { showNewPlaylist = false }
    if (showRescan) RescanDialog(onConfirm = { }) { showRescan = false }
}

// ---------- 抽屉 ----------

@Composable
fun DrawerContent(store: Store, nav: NavStack, onRescan: () -> Unit, close: () -> Unit) {
    val cur = Player.current()
    Column(Modifier.fillMaxHeight()) {
        Column(Modifier.fillMaxWidth().background(DarkBg).padding(24.dp)) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                VinylDisc(rotation = 0f, modifier = Modifier.size(140.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text(cur?.title ?: "Vinyl Music Player", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                if (cur != null && cur.album.isNotEmpty()) "${cur.artist} · ${cur.album}" else cur?.artist ?: "",
                color = Color(0xB3FFFFFF), fontSize = 14.sp
            )
        }
        Spacer(Modifier.height(8.dp))
        DrawerItem("媒体库", Pink, selected = true) { close(); nav.stack.clear(); nav.stack.add("main") }
        DrawerItem("文件夹", Color.Gray) { close(); nav.push("folder") }
        DrawerItem("重新扫描媒体库", Color.Gray) { close(); onRescan() }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        DrawerItem("设置", Color.Gray) { close(); nav.push("settings") }
        DrawerItem("关于", Color.Gray) { close(); nav.push("about") }
    }
}

@Composable
fun DrawerItem(text: String, tint: Color, selected: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .background(if (selected) Color(0x14000000) else Color.Transparent)
            .clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(Modifier.size(24.dp)) {
            val w = size.width; val h = size.height
            when (text) {
                "媒体库" -> { drawRect(tint, size = Size(w * 0.8f, h * 0.8f), topLeft = Offset(w * 0.1f, h * 0.1f)); drawCircle(Color.White, w * 0.18f, center) }
                "文件夹" -> { drawRect(tint, topLeft = Offset(w * 0.05f, h * 0.35f), size = Size(w * 0.9f, h * 0.55f)); drawRect(tint, topLeft = Offset(w * 0.05f, h * 0.2f), size = Size(w * 0.4f, h * 0.18f)) }
                "重新扫描媒体库" -> { drawCircle(tint, w * 0.38f, center, style = androidx.compose.ui.graphics.drawscope.Stroke(w * 0.1f)); val p = androidx.compose.ui.graphics.Path().apply { moveTo(w * 0.7f, h * 0.05f); lineTo(w * 0.95f, h * 0.35f); lineTo(w * 0.6f, h * 0.4f); close() }; drawPath(p, tint) }
                "设置" -> { drawCircle(tint, w * 0.32f, center); drawCircle(Color.White, w * 0.14f, center) }
                else -> { drawCircle(tint, w * 0.4f, center) }
            }
        }
        Spacer(Modifier.width(28.dp))
        Text(text, fontSize = 15.sp, color = if (selected) Pink else Color(0xDE000000),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

// ---------- 迷你播放器 ----------

@Composable
fun MiniPlayer(nav: NavStack) {
    val cur = Player.current() ?: return
    Column(
        Modifier.fillMaxWidth().background(Color.White).clickable { nav.push("nowPlaying") }
    ) {
        Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.push("nowPlaying") }) { Icon(Icons.Filled.KeyboardArrowUp, null, tint = Color.Gray) }
            Text(cur.title, fontSize = 16.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            IconButton(onClick = { Player.toggle() }) {
                PlayGlyph(Modifier.size(28.dp), Color(0xFF424242), Player.playing)
            }
        }
        Box(Modifier.fillMaxWidth().height(3.dp).background(Color(0xFFE0E0E0))) {
            val f = if (cur.duration > 0) Player.progress.toFloat() / cur.duration else 0f
            Box(Modifier.fillMaxWidth(f.coerceIn(0f, 1f)).height(3.dp).background(Pink))
        }
    }
}

// ---------- 标签页 ----------

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongsTab(songs: List<Song>, selected: MutableList<Int>, onMenu: (Song) -> Unit, currentId: Int?) {
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Row(
                Modifier.fillMaxWidth().height(52.dp).clickable {
                    if (songs.isNotEmpty()) Player.playList(songs, songs.random(), shuffled = true)
                }.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShuffleGlyph(Modifier.size(24.dp), Pink)
                Spacer(Modifier.width(24.dp))
                Text("随机播放所有歌曲", color = Pink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
            HorizontalDivider()
        }
        items(songs, key = { it.id }) { song ->
            val isSel = selected.contains(song.id)
            Row(
                Modifier.fillMaxWidth()
                    .background(if (isSel) Color(0x22000000) else Color.Transparent)
                    .combinedClickable(
                        onClick = {
                            if (selected.isNotEmpty()) {
                                if (isSel) selected.remove(song.id) else selected.add(song.id)
                            } else Player.playSong(song)
                        },
                        onLongClick = { if (selected.isEmpty()) selected.add(song.id) }
                    )
            ) {
                SongRow(song, titleBold = song.id == currentId, trailing = {
                    IconButton(onClick = { onMenu(song) }) { Icon(Icons.Filled.MoreVert, null, tint = Color.Gray) }
                })
            }
            HorizontalDivider(color = Color(0xFFEEEEEE))
        }
    }
}

@Composable
fun AlbumsTab(store: Store, nav: NavStack) {
    val songs = store.songs()
    val albums = Library.albums.map { a -> a to songs.filter { it.album == a } }.filter { it.second.isNotEmpty() }
    LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize()) {
        gridItems(albums, key = { it.first }) { (name, list) ->
            Column(
                Modifier.padding(12.dp).clickable { nav.push("album:$name") }
            ) {
                CoverArt(list.firstOrNull(), 160.dp, Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text(name, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                Text(list.firstOrNull()?.artist ?: "", fontSize = 13.sp, color = Color.Gray, maxLines = 1)
            }
        }
        item {
            Column(Modifier.padding(12.dp)) {
                CoverArt(null, 160.dp, Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text("Unknown Album", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text("0 歌曲", fontSize = 13.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun ArtistsTab(store: Store, nav: NavStack) {
    val songs = store.songs()
    LazyColumn(Modifier.fillMaxSize()) {
        items(Library.artists.filter { a -> songs.any { it.artist == a } }) { artist ->
            val list = songs.filter { it.artist == artist }
            val albumCount = list.map { it.album }.distinct().size
            Row(
                Modifier.fillMaxWidth().height(72.dp).clickable { nav.push("artist:$artist") }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Canvas(Modifier.size(48.dp)) { drawCircle(Color(0xFF9E9E9E)) }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(artist, fontSize = 16.sp)
                    Text("$albumCount 专辑 · ${list.size} 歌曲", fontSize = 13.sp, color = Color.Gray)
                }
            }
            HorizontalDivider(color = Color(0xFFEEEEEE))
        }
    }
}

@Composable
fun GenresTab(store: Store, nav: NavStack) {
    val songs = store.songs()
    LazyColumn(Modifier.fillMaxSize()) {
        items(Library.genreOrder) { g ->
            val count = songs.count { it.genre == g }
            if (count == 0 && g.isNotEmpty()) return@items
            Column(
                Modifier.fillMaxWidth().clickable { nav.push("genre:${g.ifEmpty { "未知音乐类型" }}") }
                    .padding(horizontal = 16.dp, vertical = 18.dp)
            ) {
                Text(if (g.isEmpty()) "未知音乐类型" else g, fontSize = 16.sp)
                Text("$count 歌曲", fontSize = 13.sp, color = Color.Gray)
            }
            HorizontalDivider()
        }
    }
}

@Composable
fun PlaylistsTab(store: Store, nav: NavStack) {
    val songs = store.songs()
    val historyCount = store.history.count { id -> songs.any { it.id == id } }
    val notPlayed = songs.size - historyCount
    val mostPlayed = store.playCounts.filter { it.value > 0 }.keys.count { id -> songs.any { it.id == id } }
    val favCount = store.favorites.count { id -> songs.any { it.id == id } }
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            PlaylistRow("最近添加", "本月 · ${songs.size} 歌曲", 0) { nav.push("playlist:recent") }
            PlaylistRow("播放历史", "本月 · $historyCount 歌曲", 1) { nav.push("playlist:history") }
            PlaylistRow("最近未播放过", "本月 · $notPlayed 歌曲", 2) { nav.push("playlist:notPlayed") }
            PlaylistRow("最喜爱的歌曲", "$mostPlayed 歌曲", 3) { nav.push("playlist:mostPlayed") }
            HorizontalDivider(Modifier.padding(vertical = 8.dp), thickness = 1.dp)
            PlaylistRow("收藏夹", "$favCount 歌曲", 4) { nav.push("playlist:favorites") }
        }
    }
}

@Composable
fun PlaylistRow(name: String, sub: String, icon: Int, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(72.dp).clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(Modifier.size(28.dp)) {
            val w = size.width; val h = size.height; val c = Color(0xFF757575)
            when (icon) {
                0 -> { // 最近添加: 列表+加号
                    drawRect(c, topLeft = Offset(w * 0.1f, h * 0.15f), size = Size(w * 0.62f, h * 0.7f))
                    drawLine(Color.White, Offset(w * 0.26f, h * 0.32f), Offset(w * 0.56f, h * 0.32f), strokeWidth = 3f)
                    drawLine(Color.White, Offset(w * 0.26f, h * 0.48f), Offset(w * 0.56f, h * 0.48f), strokeWidth = 3f)
                    drawCircle(c, w * 0.2f, Offset(w * 0.75f, h * 0.75f))
                    drawLine(Color.White, Offset(w * 0.63f, h * 0.75f), Offset(w * 0.87f, h * 0.75f), strokeWidth = 3f)
                    drawLine(Color.White, Offset(w * 0.75f, h * 0.63f), Offset(w * 0.75f, h * 0.87f), strokeWidth = 3f)
                }
                1, 2 -> { // 时钟
                    drawCircle(c, w * 0.44f, center, style = androidx.compose.ui.graphics.drawscope.Stroke(w * 0.09f))
                    drawLine(c, center, Offset(w * 0.5f, h * 0.28f), strokeWidth = w * 0.09f)
                    drawLine(c, center, Offset(w * 0.68f, h * 0.55f), strokeWidth = w * 0.09f)
                    if (icon == 2) drawCircle(c, w * 0.44f, center)
                }
                3 -> { // 折线上升
                    drawLine(c, Offset(w * 0.1f, h * 0.8f), Offset(w * 0.4f, h * 0.5f), strokeWidth = w * 0.09f)
                    drawLine(c, Offset(w * 0.4f, h * 0.5f), Offset(w * 0.6f, h * 0.65f), strokeWidth = w * 0.09f)
                    drawLine(c, Offset(w * 0.6f, h * 0.65f), Offset(w * 0.85f, h * 0.3f), strokeWidth = w * 0.09f)
                    drawLine(c, Offset(w * 0.85f, h * 0.3f), Offset(w * 0.85f, h * 0.5f), strokeWidth = w * 0.09f)
                    drawLine(c, Offset(w * 0.85f, h * 0.3f), Offset(w * 0.65f, h * 0.3f), strokeWidth = w * 0.09f)
                }
                else -> { // 心形
                    val p = androidx.compose.ui.graphics.Path().apply {
                        moveTo(w * 0.5f, h * 0.85f)
                        cubicTo(w * 0.1f, h * 0.55f, w * 0.05f, h * 0.25f, w * 0.25f, h * 0.14f)
                        cubicTo(w * 0.4f, h * 0.06f, w * 0.5f, h * 0.2f, w * 0.5f, h * 0.3f)
                        cubicTo(w * 0.5f, h * 0.2f, w * 0.6f, h * 0.06f, w * 0.75f, h * 0.14f)
                        cubicTo(w * 0.95f, h * 0.25f, w * 0.9f, h * 0.55f, w * 0.5f, h * 0.85f)
                        close()
                    }
                    drawPath(p, c)
                }
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(name, fontSize = 16.sp)
            Text(sub, fontSize = 13.sp, color = Color.Gray)
        }
        Icon(Icons.Filled.MoreVert, null, tint = Color.Gray)
    }
}
