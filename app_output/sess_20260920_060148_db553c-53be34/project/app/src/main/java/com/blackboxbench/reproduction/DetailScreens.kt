package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 详情页通用脚手架：返回 + 标题 + ⋮ + 内容 + 迷你播放器
@Composable
fun DetailScaffold(
    title: String, nav: NavStack,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(
            Modifier.fillMaxWidth().background(Indigo).statusBarsPadding().height(56.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { nav.pop() }) { Icon(Icons.Filled.ArrowBack, null, tint = Color.White) }
            Text(title, color = Color.White, fontSize = 20.sp, maxLines = 1, modifier = Modifier.weight(1f))
            actions()
        }
        Column(Modifier.weight(1f)) { content() }
        MiniPlayer(nav)
    }
}

// 详情页共用对话框状态
private class DetailDialogs {
    var menuSong by mutableStateOf<Song?>(null)
    var addToPlaylistSong by mutableStateOf<Song?>(null)
    var detailsSong by mutableStateOf<Song?>(null)
    var deleteSong by mutableStateOf<Song?>(null)
    var toast by mutableStateOf<String?>(null)
}

@Composable
private fun DetailDialogHost(d: DetailDialogs, store: Store, nav: NavStack, inPlaylist: Boolean = false, onRemove: (Song) -> Unit = {}) {
    d.menuSong?.let { song ->
        SongMenuDialog(song, store, nav, inPlaylist = inPlaylist, onAction = { act ->
            when {
                act == "removeFromPlaylist" -> onRemove(song)
                act == "addToPlaylist" -> d.addToPlaylistSong = song
                act == "details" -> d.detailsSong = song
                act == "delete" -> d.deleteSong = song
                act.startsWith("toast:") -> d.toast = act.removePrefix("toast:")
            }
        }, onDismiss = { d.menuSong = null })
    }
    d.addToPlaylistSong?.let { song ->
        var showNew by remember { mutableStateOf(false) }
        if (showNew) NewPlaylistDialog { showNew = false }
        else AddToPlaylistDialog(song, store, onNewPlaylist = { showNew = true }) { d.addToPlaylistSong = null }
    }
    d.detailsSong?.let { SongDetailsDialog(it) { d.detailsSong = null } }
    d.deleteSong?.let { song ->
        DeleteSongDialog(song, onConfirm = { store.deleteSong(song.id) }) { d.deleteSong = null }
    }
    d.toast?.let {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Text(it, color = Color.White, fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 96.dp).background(Color(0xDD323232)).padding(12.dp))
        }
        LaunchedEffect(it) { kotlinx.coroutines.delay(2000); d.toast = null }
    }
}

@Composable
fun SongListWithMenu(songs: List<Song>, onMenu: (Song) -> Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(songs, key = { it.id }) { song ->
            SongRow(song, modifier = Modifier.clickable { Player.playList(songs, song) }, trailing = {
                IconButton(onClick = { onMenu(song) }) { Icon(Icons.Filled.MoreVert, null, tint = Color.Gray) }
            })
            HorizontalDivider(color = Color(0xFFEEEEEE))
        }
    }
}

// ---------- 专辑详情 ----------

@Composable
fun AlbumDetailScreen(store: Store, nav: NavStack, album: String) {
    val d = remember { DetailDialogs() }
    val songs = store.songs().filter { it.album == album }.sortedBy { it.track }
    val artist = songs.firstOrNull()?.artist ?: ""
    val year = songs.firstOrNull()?.year ?: 0
    val total = songs.sumOf { it.duration }
    var toolbarMenu by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        DetailScaffold(album, nav, actions = {
            IconButton(onClick = { if (songs.isNotEmpty()) Player.playList(songs, songs.random(), shuffled = true) }) {
                ShuffleGlyph(Modifier.size(24.dp), Color.White)
            }
            Box {
                IconButton(onClick = { toolbarMenu = true }) { Icon(Icons.Filled.MoreVert, null, tint = Color.White) }
                DropdownMenu(expanded = toolbarMenu, onDismissRequest = { toolbarMenu = false }) {
                    DropdownMenuItem(text = { Text("百科") }, onClick = { toolbarMenu = false })
                    DropdownMenuItem(text = { Text("作为下一首播放") }, onClick = {
                        toolbarMenu = false; songs.forEach { Player.playNext(it) }
                    })
                    DropdownMenuItem(text = { Text("加入播放队列") }, onClick = {
                        toolbarMenu = false; songs.forEach { Player.enqueue(it) }
                    })
                    DropdownMenuItem(text = { Text("加入播放列表…") }, onClick = {
                        toolbarMenu = false; songs.forEach { if (!store.favorites.contains(it.id)) store.toggleFavorite(it.id) }
                    })
                    DropdownMenuItem(text = { Text("查看艺术家") }, onClick = {
                        toolbarMenu = false; if (Library.artists.contains(artist)) nav.push("artist:$artist")
                    })
                    DropdownMenuItem(text = { Text("音乐标签编辑器") }, onClick = {
                        toolbarMenu = false; songs.firstOrNull()?.let { nav.push("tagEditor:${it.id}") }
                    })
                    DropdownMenuItem(text = { Text("删除", color = Color(0xFFD32F2F)) }, onClick = {
                        toolbarMenu = false; songs.forEach { store.deleteSong(it.id) }; nav.pop()
                    })
                }
            }
        }) {
            LazyColumn(Modifier.fillMaxSize()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        CoverArt(songs.firstOrNull(), 180.dp)
                        Spacer(Modifier.height(16.dp))
                        Text(album, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(artist, fontSize = 15.sp, color = Color.Gray)
                        Text("$year · ${songs.size} 歌曲 · ${fmtDuration(total)}", fontSize = 13.sp, color = Color.Gray)
                    }
                }
                items(songs, key = { it.id }) { song ->
                    SongRow(song, modifier = Modifier.clickable { Player.playList(songs, song) }, leading = {
                        Text("${song.track}", fontSize = 15.sp, color = Color.Gray, modifier = Modifier.width(24.dp))
                    }, trailing = {
                        IconButton(onClick = { d.menuSong = song }) { Icon(Icons.Filled.MoreVert, null, tint = Color.Gray) }
                    })
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                }
            }
        }
        DetailDialogHost(d, store, nav)
    }
}

// ---------- 艺术家详情 ----------

@Composable
fun ArtistDetailScreen(store: Store, nav: NavStack, artist: String) {
    val d = remember { DetailDialogs() }
    val songs = store.songs().filter { it.artist == artist }
    val albums = songs.map { it.album }.distinct().filter { it.isNotEmpty() }
    var toolbarMenu by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        DetailScaffold(artist, nav, actions = {
            IconButton(onClick = { if (songs.isNotEmpty()) Player.playList(songs, songs.random(), shuffled = true) }) {
                ShuffleGlyph(Modifier.size(24.dp), Color.White)
            }
            Box {
                IconButton(onClick = { toolbarMenu = true }) { Icon(Icons.Filled.MoreVert, null, tint = Color.White) }
                DropdownMenu(expanded = toolbarMenu, onDismissRequest = { toolbarMenu = false }) {
                    DropdownMenuItem(text = { Text("歌手简介") }, onClick = { toolbarMenu = false })
                    DropdownMenuItem(text = { Text("作为下一首播放") }, onClick = {
                        toolbarMenu = false; songs.forEach { Player.playNext(it) }
                    })
                    DropdownMenuItem(text = { Text("加入播放队列") }, onClick = {
                        toolbarMenu = false; songs.forEach { Player.enqueue(it) }
                    })
                    DropdownMenuItem(text = { Text("加入播放列表…") }, onClick = {
                        toolbarMenu = false; songs.forEach { if (!store.favorites.contains(it.id)) store.toggleFavorite(it.id) }
                    })
                }
            }
        }) {
            LazyColumn(Modifier.fillMaxSize()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Canvas(Modifier.size(120.dp)) { drawCircle(Color(0xFF9E9E9E)) }
                        Spacer(Modifier.height(16.dp))
                        Text(artist, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("${albums.size} 专辑 · ${songs.size} 歌曲", fontSize = 13.sp, color = Color.Gray)
                        Spacer(Modifier.height(8.dp))
                    }
                }
                item {
                    LazyRow(Modifier.padding(horizontal = 8.dp)) {
                        items(albums) { a ->
                            Column(
                                Modifier.padding(8.dp).width(140.dp).clickable { nav.push("album:$a") }
                            ) {
                                CoverArt(songs.firstOrNull { it.album == a }, 124.dp, Modifier.fillMaxWidth())
                                Text(a, fontSize = 14.sp, maxLines = 1)
                                Text("${songs.count { it.album == a }} 歌曲", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                }
                items(songs, key = { it.id }) { song ->
                    SongRow(song, modifier = Modifier.clickable { Player.playList(songs, song) }, trailing = {
                        IconButton(onClick = { d.menuSong = song }) { Icon(Icons.Filled.MoreVert, null, tint = Color.Gray) }
                    })
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                }
            }
        }
        DetailDialogHost(d, store, nav)
    }
}

// ---------- 类型详情 ----------

@Composable
fun GenreDetailScreen(store: Store, nav: NavStack, genre: String) {
    val d = remember { DetailDialogs() }
    val g = if (genre == "未知音乐类型") "" else genre
    val songs = store.songs().filter { it.genre == g }
    DetailScaffold(genre, nav, actions = {
        IconButton(onClick = { if (songs.isNotEmpty()) Player.playList(songs, songs.random(), shuffled = true) }) {
            ShuffleGlyph(Modifier.size(24.dp), Color.White)
        }
    }) {
        Box {
            SongListWithMenu(songs) { d.menuSong = it }
            DetailDialogHost(d, store, nav)
        }
    }
}

// ---------- 播放列表详情 ----------

@Composable
fun PlaylistDetailScreen(store: Store, nav: NavStack, key: String) {
    val d = remember { DetailDialogs() }
    val all = store.songs()
    val (title, songs) = when (key) {
        "recent" -> "最近添加" to all
        "history" -> "播放历史" to store.history.mapNotNull { id -> all.firstOrNull { it.id == id } }
        "notPlayed" -> "最近未播放过" to all.filter { !store.history.contains(it.id) }
        "mostPlayed" -> "最喜爱的歌曲" to all.filter { (store.playCounts[it.id] ?: 0) > 0 }
            .sortedByDescending { store.playCounts[it.id] }
        else -> "收藏夹" to store.favorites.mapNotNull { id -> all.firstOrNull { it.id == id } }
    }
    var toolbarMenu by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var plName by remember { mutableStateOf(title) }

    Box(Modifier.fillMaxSize()) {
        DetailScaffold(plName, nav, actions = {
            IconButton(onClick = { if (songs.isNotEmpty()) Player.playList(songs, songs.random(), shuffled = true) }) {
                ShuffleGlyph(Modifier.size(24.dp), Color.White)
            }
            Box {
                IconButton(onClick = { toolbarMenu = true }) { Icon(Icons.Filled.MoreVert, null, tint = Color.White) }
                DropdownMenu(expanded = toolbarMenu, onDismissRequest = { toolbarMenu = false }) {
                    DropdownMenuItem(text = { Text("作为下一首播放") }, onClick = {
                        toolbarMenu = false; songs.forEach { Player.playNext(it) }
                    })
                    DropdownMenuItem(text = { Text("加入播放队列") }, onClick = {
                        toolbarMenu = false; songs.forEach { Player.enqueue(it) }
                    })
                    DropdownMenuItem(text = { Text("加入播放列表…") }, onClick = {
                        toolbarMenu = false; songs.forEach { if (!store.favorites.contains(it.id)) store.toggleFavorite(it.id) }
                    })
                    if (key == "favorites") {
                        DropdownMenuItem(text = { Text("重命名") }, onClick = { toolbarMenu = false; showRename = true })
                        DropdownMenuItem(text = { Text("另存为") }, onClick = { toolbarMenu = false })
                        DropdownMenuItem(text = { Text("删除", color = Color(0xFFD32F2F)) }, onClick = { toolbarMenu = false })
                    }
                }
            }
        }) {
            if (songs.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("没有歌曲", color = Color.Gray)
                }
            } else {
                SongListWithMenu(songs) { d.menuSong = it }
            }
        }
        DetailDialogHost(d, store, nav, inPlaylist = key == "favorites", onRemove = { song ->
            if (store.favorites.contains(song.id)) store.toggleFavorite(song.id)
        })
        if (showRename) RenamePlaylistDialog(plName, onRename = { plName = it }) { showRename = false }
    }
}

// ---------- 文件夹 ----------

@Composable
fun FolderScreen(store: Store, nav: NavStack) {
    val d = remember { DetailDialogs() }
    var sortMenu by remember { mutableStateOf(false) }
    var toolbarMenu by remember { mutableStateOf(false) }
    var sort by remember { mutableStateOf("Modified - Recent first") }
    val files = store.songs().filter { !it.greenCover }.let { list ->
        when (sort) {
            "按首字符（正序）" -> list.sortedBy { it.fileName }
            "按首字符（倒序）" -> list.sortedByDescending { it.fileName }
            "Modified" -> list.sortedBy { it.id }
            else -> list.sortedBy { it.fileName }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(Color.White)) {
            Row(
                Modifier.fillMaxWidth().background(Indigo).statusBarsPadding().height(56.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { nav.pop() }) { Icon(Icons.Filled.Menu, null, tint = Color.White) }
                Text("Vinyl Music Player", color = Color.White, fontSize = 20.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = { }) {
                    Canvas(Modifier.size(24.dp)) {
                        val w = size.width; val h = size.height
                        val p = androidx.compose.ui.graphics.Path().apply {
                            moveTo(w * 0.25f, h * 0.1f); lineTo(w * 0.75f, h * 0.1f)
                            lineTo(w * 0.75f, h * 0.9f); lineTo(w * 0.5f, h * 0.68f)
                            lineTo(w * 0.25f, h * 0.9f); close()
                        }
                        drawPath(p, Color.White)
                    }
                }
                Box {
                    IconButton(onClick = { toolbarMenu = true }) { Icon(Icons.Filled.MoreVert, null, tint = Color.White) }
                    DropdownMenu(expanded = toolbarMenu, onDismissRequest = { toolbarMenu = false }) {
                        DropdownMenuItem(text = { Text("扫描音乐") }, onClick = { toolbarMenu = false })
                        DropdownMenuItem(text = { Text("排序方式            ▸") }, onClick = {
                            toolbarMenu = false; sortMenu = true
                        })
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().background(IndigoDark).horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("ROOT", "STORAGE", "EMULATED", "0", "MUSIC").forEachIndexed { i, seg ->
                    Text(seg, color = Color(0xB3FFFFFF), fontSize = 13.sp)
                    if (i < 4) Text("  ›  ", color = Color(0x80FFFFFF), fontSize = 13.sp)
                }
            }
            LazyColumn(Modifier.weight(1f)) {
                items(files, key = { it.id }) { song ->
                    Row(
                        Modifier.fillMaxWidth().height(68.dp)
                            .clickable { Player.playList(files, song) }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Canvas(Modifier.size(40.dp)) {
                            val w = size.width; val h = size.height
                            val p = androidx.compose.ui.graphics.Path().apply {
                                moveTo(w * 0.15f, h * 0.05f); lineTo(w * 0.62f, h * 0.05f)
                                lineTo(w * 0.85f, h * 0.28f); lineTo(w * 0.85f, h * 0.95f)
                                lineTo(w * 0.15f, h * 0.95f); close()
                            }
                            drawPath(p, Color(0xFF757575))
                            drawCircle(Color.White, w * 0.16f, androidx.compose.ui.geometry.Offset(w * 0.4f, h * 0.62f))
                            drawRect(Color.White, topLeft = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.35f), size = androidx.compose.ui.geometry.Size(w * 0.08f, h * 0.3f))
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(song.fileName, fontSize = 15.sp, maxLines = 1)
                            Text(song.fileSize, fontSize = 13.sp, color = Color.Gray)
                        }
                        IconButton(onClick = { d.menuSong = song }) {
                            Icon(Icons.Filled.MoreVert, null, tint = Color.Gray)
                        }
                    }
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                }
            }
            MiniPlayer(nav)
        }
        // 文件夹文件菜单需要 isFile = true: 复用 SongMenuDialog 通过 DetailDialogHost 默认 false，这里单独处理
        d.menuSong?.let { song ->
            SongMenuDialog(song, store, nav, isFile = true, onAction = { act ->
                when {
                    act == "addToPlaylist" -> d.addToPlaylistSong = song
                    act == "details" -> d.detailsSong = song
                    act == "delete" -> d.deleteSong = song
                    act.startsWith("toast:") -> d.toast = act.removePrefix("toast:")
                }
                d.menuSong = null
            }, onDismiss = { d.menuSong = null })
        }
        d.addToPlaylistSong?.let { song ->
            var showNew by remember { mutableStateOf(false) }
            if (showNew) NewPlaylistDialog { showNew = false }
            else AddToPlaylistDialog(song, store, onNewPlaylist = { showNew = true }) { d.addToPlaylistSong = null }
        }
        d.detailsSong?.let { SongDetailsDialog(it) { d.detailsSong = null } }
        d.deleteSong?.let { song ->
            DeleteSongDialog(song, onConfirm = { store.deleteSong(song.id) }) { d.deleteSong = null }
        }
        if (sortMenu) {
            ListMenuDialog(
                listOf("按首字符（正序）" to null, "按首字符（倒序）" to null, "Modified" to null, "Modified - Recent first" to null),
                onItem = { i ->
                    sort = listOf("按首字符（正序）", "按首字符（倒序）", "Modified", "Modified - Recent first")[i]
                }, onDismiss = { sortMenu = false })
        }
        d.toast?.let {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                Text(it, color = Color.White, fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 96.dp).background(Color(0xDD323232)).padding(12.dp))
            }
            LaunchedEffect(it) { kotlinx.coroutines.delay(2000); d.toast = null }
        }
    }
}

// ---------- 搜索 ----------

@Composable
fun SearchScreen(store: Store, nav: NavStack) {
    var query by remember { mutableStateOf("") }
    val d = remember { DetailDialogs() }
    val songs = store.songs()
    val q = query.trim()
    val matchedSongs = if (q.isEmpty()) emptyList() else songs.filter {
        it.title.contains(q, true) || it.artist.contains(q, true) || it.album.contains(q, true)
    }
    val matchedAlbums = matchedSongs.map { it.album }.distinct().filter { it.isNotEmpty() }
    val matchedArtists = matchedSongs.map { it.artist }.distinct()

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(Color.White)) {
            Row(
                Modifier.fillMaxWidth().background(Indigo).statusBarsPadding().height(56.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { nav.pop() }) { Icon(Icons.Filled.ArrowBack, null, tint = Color.White) }
                androidx.compose.material3.TextField(
                    value = query, onValueChange = { query = it },
                    placeholder = { Text("搜索媒体库", color = Color(0x80FFFFFF)) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        cursorColor = Color.White,
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.weight(1f)
                )
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, null, tint = Color.White) }
                }
            }
            if (q.isEmpty() || (matchedSongs.isEmpty())) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("没有找到结果", color = Color.Gray, fontSize = 15.sp)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    if (matchedAlbums.isNotEmpty()) {
                        item { SearchHeader("专辑") }
                        items(matchedAlbums) { a ->
                            val list = songs.filter { it.album == a }
                            Row(
                                Modifier.fillMaxWidth().height(64.dp).clickable { nav.push("album:$a") }
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CoverArt(list.firstOrNull(), 44.dp)
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(a, fontSize = 15.sp)
                                    Text("${list.first().artist} · ${list.size} 歌曲", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                    if (matchedArtists.isNotEmpty()) {
                        item { SearchHeader("艺术家") }
                        items(matchedArtists.filter { Library.artists.contains(it) }) { a ->
                            Row(
                                Modifier.fillMaxWidth().height(56.dp).clickable { nav.push("artist:$a") }
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Canvas(Modifier.size(40.dp)) { drawCircle(Color(0xFF9E9E9E)) }
                                Spacer(Modifier.width(16.dp))
                                Text(a, fontSize = 15.sp)
                            }
                        }
                    }
                    if (matchedSongs.isNotEmpty()) {
                        item { SearchHeader("歌曲") }
                        items(matchedSongs, key = { it.id }) { song ->
                            SongRow(song, modifier = Modifier.clickable { Player.playSong(song) }, trailing = {
                                IconButton(onClick = { d.menuSong = song }) { Icon(Icons.Filled.MoreVert, null, tint = Color.Gray) }
                            })
                        }
                    }
                }
            }
        }
        DetailDialogHost(d, store, nav)
    }
}

@Composable
fun SearchHeader(text: String) {
    Text(text, fontSize = 13.sp, color = Color.Gray,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
}
