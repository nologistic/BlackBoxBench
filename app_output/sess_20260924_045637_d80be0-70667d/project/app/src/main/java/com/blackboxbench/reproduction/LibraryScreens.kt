package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------------------
// shared menu builders
// ---------------------------------------------------------------------------

fun songMenuItems(
    app: AppState,
    song: Song,
    dialogs: DialogController,
    nav: Navigator,
    inQueue: Boolean = false,
    showScan: Boolean = false
): List<MenuItemSpec> {
    val items = mutableListOf<MenuItemSpec>()
    items.add(MenuItemSpec("作为下一首播放") { app.playNext(song.id) })
    if (inQueue) {
        items.add(MenuItemSpec("从播放队列中移除") { app.removeFromQueue(song.id) })
    } else {
        items.add(MenuItemSpec("加入播放队列") { app.queue = app.queue + song.id; app.persistQueue() })
    }
    items.add(MenuItemSpec("加入播放列表…") { dialogs.addToPlaylist = listOf(song.id) })
    items.add(MenuItemSpec("查看专辑") { nav.push("album", song.album) })
    items.add(MenuItemSpec("查看艺术家") { nav.push("artist", song.artist) })
    items.add(MenuItemSpec("分享") { app.snackbar = "分享 ${song.title}" })
    items.add(MenuItemSpec("音乐标签编辑器") { dialogs.tagEditor = song })
    items.add(MenuItemSpec("详情") { dialogs.songDetails = song })
    if (showScan) items.add(MenuItemSpec("扫描") { app.snackbar = "已扫描 ${song.title}" })
    items.add(MenuItemSpec("设为铃声") { dialogs.ringtone = true })
    items.add(MenuItemSpec("从设备中删除", destructive = true) { nav.push("sdcard") })
    return items
}

fun albumMenuItems(album: Album, app: AppState, dialogs: DialogController, nav: Navigator): List<MenuItemSpec> {
    val ids = Library.albumSongs(album.name).map { it.id }
    return listOf(
        MenuItemSpec("百科") { app.snackbar = "无网络连接" },
        MenuItemSpec("作为下一首播放") { ids.firstOrNull()?.let { app.playNext(it) } },
        MenuItemSpec("加入播放队列") { app.queue = app.queue + ids; app.persistQueue() },
        MenuItemSpec("加入播放列表…") { dialogs.addToPlaylist = ids },
        MenuItemSpec("查看艺术家") { nav.push("artist", album.artist) },
        MenuItemSpec("音乐标签编辑器") { Library.albumSongs(album.name).firstOrNull()?.let { dialogs.tagEditor = it } },
        MenuItemSpec("从设备中删除", destructive = true) { nav.push("sdcard") }
    )
}

fun artistMenuItems(artist: Artist, app: AppState, dialogs: DialogController): List<MenuItemSpec> {
    val ids = Library.artistSongs(artist.name).map { it.id }
    return listOf(
        MenuItemSpec("歌手简介") { app.snackbar = "无网络连接" },
        MenuItemSpec("作为下一首播放") { ids.firstOrNull()?.let { app.playNext(it) } },
        MenuItemSpec("加入播放队列") { app.queue = app.queue + ids; app.persistQueue() },
        MenuItemSpec("加入播放列表…") { dialogs.addToPlaylist = ids },
        MenuItemSpec("设置艺术家图片") { app.snackbar = "设置艺术家图片" },
        MenuItemSpec("重置艺术家图片") { app.snackbar = "重置艺术家图片" },
        MenuItemSpec("着色页脚", checked = app.colorFooter) {
            app.colorFooter = !app.colorFooter
            app.persistSettings()
        }
    )
}

fun playlistMenuItems(
    playlist: Playlist,
    app: AppState,
    dialogs: DialogController,
    nav: Navigator
): List<MenuItemSpec> {
    val items = mutableListOf<MenuItemSpec>()
    items.add(MenuItemSpec("作为下一首播放") { playlist.songIds.firstOrNull()?.let { app.playNext(it) } })
    items.add(MenuItemSpec("加入播放队列") { app.queue = app.queue + playlist.songIds; app.persistQueue() })
    items.add(MenuItemSpec("加入播放列表…") { dialogs.addToPlaylist = playlist.songIds })
    if (playlist.kind == "user") {
        items.add(MenuItemSpec("重命名") { dialogs.renamePlaylist = playlist.name })
        items.add(MenuItemSpec("另存为") { dialogs.saveAsPlaylist = playlist.id })
        items.add(MenuItemSpec("删除", destructive = true) { dialogs.deletePlaylist = playlist.name })
    } else {
        items.add(MenuItemSpec("设置") { app.snackbar = "播放列表设置" })
        items.add(MenuItemSpec("清空播放列表", destructive = true) { app.snackbar = "已清空 ${playlist.name}" })
        items.add(MenuItemSpec("另存为") { dialogs.saveAsPlaylist = playlist.id })
        items.add(MenuItemSpec("Import from playlist") { app.snackbar = "Import from playlist" })
    }
    return items
}

fun libraryOverflowItems(app: AppState): List<MenuItemSpec> = listOf(
    MenuItemSpec("随机播放所有歌曲") {
        app.shuffle = true
        Library.songs.randomOrNull()?.let { app.play(it) }
    },
    MenuItemSpec(
        "网格尺寸",
        submenu = listOf(1, 2, 3, 4).map { size ->
            MenuItemSpec(size.toString(), checked = app.gridSize == size) {
                app.gridSize = size
                app.persistSettings()
            }
        }
    ),
    MenuItemSpec(
        "排序方式",
        submenu = listOf("按首字符（正序）", "按首字符（倒序）", "Modified", "Modified - Recent first").map { mode ->
            MenuItemSpec(mode, checked = app.sortMode == mode) {
                app.sortMode = mode
                app.persistSettings()
            }
        }
    ),
    MenuItemSpec("显示页脚", checked = app.showFooter) {
        app.showFooter = !app.showFooter
        app.persistSettings()
    },
    MenuItemSpec("着色页脚", checked = app.colorFooter) {
        app.colorFooter = !app.colorFooter
        app.persistSettings()
    }
)

// ---------------------------------------------------------------------------
// library root
// ---------------------------------------------------------------------------

@Composable
fun LibraryScreen(app: AppState, selectedTab: String, onSelectTab: (String) -> Unit) {
    val colors = LocalViColors.current
    val dialogs = LocalDialogs.current
    val nav = LocalNav.current
    val tabs = app.visibleTabs()
    var selection by remember { mutableStateOf(setOf<String>()) }

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        if (selection.isEmpty()) {
            ViTopBar(
                title = "Vinyl Music Player",
                onMenu = { app.snackbar = null; nav.push("drawer") },
                onSearch = { nav.push("search") },
                onOverflow = {
                    val menu = LocalMenuRef
                    menu.show(libraryOverflowItems(app), menu.anchorFor(1023f, 200f))
                }
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.appBar)
                    .statusBarsPadding()
                    .height(64.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButtonNoRipple("close", colors.appBarText, { selection = emptySet() })
                Spacer(Modifier.width(20.dp))
                Text("已选择 ${selection.size}", color = colors.appBarText, fontSize = 21.sp, modifier = Modifier.weight(1f))
                IconButtonNoRipple("list", colors.appBarText, { selection = Library.songs.map { it.id }.toSet() })
                IconButtonNoRipple("add", colors.appBarText, { selection = emptySet() })
                MenuDots(colors.appBarText) {
                    listOf(
                        MenuItemSpec("加入播放列表…") { dialogs.addToPlaylist = selection.toList() },
                        MenuItemSpec("全部选择") { selection = Library.songs.map { it.id }.toSet() },
                        MenuItemSpec("从设备中删除", destructive = true) { nav.push("sdcard") }
                    )
                }
            }
        }
        ViTabRow(tabs, selectedTab, onSelectTab)
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                "歌曲" -> SongsTab(app, selection) { selection = it }
                "专辑" -> AlbumsTab(app)
                "艺术家" -> ArtistsTab(app)
                "音乐类型" -> GenresTab(app)
                "播放列表" -> PlaylistsTab(app)
                else -> SongsTab(app, selection) { selection = it }
            }
        }
    }
}

/** bridge so screens can hand a menu back to the root host without extra plumbing */
object LocalMenuRef {
    var show: (List<MenuItemSpec>, androidx.compose.ui.geometry.Offset) -> Unit = { _, _ -> }
    fun anchorFor(x: Float, y: Float) = androidx.compose.ui.geometry.Offset(x, y)
}

@Composable
fun ViTabRow(tabs: List<String>, selected: String, onSelect: (String) -> Unit) {
    val colors = LocalViColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(colors.appBar),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { tab ->
            val active = tab == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        onSelect(tab)
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = tab,
                    color = if (active) colors.appBarText else colors.appBarText.copy(alpha = 0.72f),
                    fontSize = 15.sp,
                    fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(if (active) colors.accent else Color.Transparent)
                )
            }
        }
    }
}

@Composable
fun SongsTab(app: AppState, selection: Set<String>, onSelectionChange: (Set<String>) -> Unit) {
    val colors = LocalViColors.current
    val dialogs = LocalDialogs.current
    val nav = LocalNav.current
    val songs = when (app.sortMode) {
        "按首字符（倒序）" -> Library.songs.sortedByDescending { it.title }
        "Modified", "Modified - Recent first" -> Library.songs.sortedByDescending { it.added }
        else -> Library.songs
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (selection.isEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            app.shuffle = true
                            Library.songs.randomOrNull()?.let { app.play(it) }
                        }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Ic("shuffle", 26.dp, colors.accent)
                    Spacer(Modifier.width(24.dp))
                    Text("随机播放所有歌曲", color = colors.accent, fontSize = 17.sp)
                }
                ViDivider()
            }
        }
        items(songs) { song ->
            val selected = selection.contains(song.id)
            SongRow(
                song = song,
                isCurrent = song.id == app.currentSongId,
                playing = app.playing,
                showTrack = app.showTrackNumber,
                onTap = {
                    if (selection.isEmpty()) {
                        app.play(song, songs)
                    } else {
                        onSelectionChange(if (selected) selection - song.id else selection + song.id)
                    }
                },
                onLongClick = { onSelectionChange(selection + song.id) },
                onMenu = {},
                menuItems = { songMenuItems(app, song, dialogs, nav) },
                selected = selected
            )
        }
    }
}

@Composable
fun AlbumsTab(app: AppState) {
    val colors = LocalViColors.current
    val dialogs = LocalDialogs.current
    val nav = LocalNav.current
    LazyVerticalGrid(
        columns = GridCells.Fixed(if (app.gridSize >= 2) 2 else 1),
        modifier = Modifier.fillMaxSize()
    ) {
        items(Library.albums) { album ->
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .clickable { nav.push("album", album.name) }
            ) {
                VinylArt(150.dp, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text(album.name, color = colors.text, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (album.artist.isEmpty()) "${album.songCount} 歌曲" else "${album.artist} · ${album.songCount} 歌曲",
                    color = colors.subtitle,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ArtistsTab(app: AppState) {
    val colors = LocalViColors.current
    val dialogs = LocalDialogs.current
    val nav = LocalNav.current
    if (app.gridSize >= 2) {
        LazyVerticalGrid(columns = GridCells.Fixed(app.gridSize), modifier = Modifier.fillMaxSize()) {
            items(Library.artists) { artist ->
                Column(modifier = Modifier.padding(4.dp)) {
                    PersonArt(150.dp, modifier = Modifier.fillMaxWidth().clickable { nav.push("artist", artist.name) })
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { nav.push("artist", artist.name) }
                                .padding(vertical = 6.dp)
                        ) {
                            Text(artist.name, color = colors.text, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${artist.albumCount} 专辑 · ${artist.songCount} 歌曲",
                                color = colors.subtitle, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                        MenuDots(colors.icon) { artistMenuItems(artist, app, dialogs) }
                    }
                }
            }
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(Library.artists) { artist ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { nav.push("artist", artist.name) }
                        .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PersonArt(46.dp)
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(artist.name, color = colors.text, fontSize = 17.sp)
                        Text("${artist.albumCount} 专辑 · ${artist.songCount} 歌曲", color = colors.subtitle, fontSize = 14.sp)
                    }
                    MenuDots(colors.icon) { artistMenuItems(artist, app, dialogs) }
                }
            }
        }
    }
}

@Composable
fun GenresTab(app: AppState) {
    val colors = LocalViColors.current
    val nav = LocalNav.current
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(Library.genres) { genre ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { nav.push("genre", genre.name) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(genre.name, color = colors.text, fontSize = 17.sp)
                    Text("${genre.songCount} 歌曲", color = colors.subtitle, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun PlaylistsTab(app: AppState) {
    val colors = LocalViColors.current
    val dialogs = LocalDialogs.current
    val nav = LocalNav.current
    val playlists = app.playlists()
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(playlists) { playlist ->
            if (playlist.kind == "favorite") {
                ViDivider()
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { nav.push("playlist", playlist.id) }
                    .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val icon = when (playlist.kind) {
                    "favorite" -> "heart_fill"
                    "user" -> "note"
                    else -> when (playlist.id) {
                        "recent" -> "add"
                        "history" -> "clock"
                        "notplayed" -> "clock"
                        else -> "sort"
                    }
                }
                Ic(icon, 30.dp, if (playlist.kind == "favorite") colors.icon else colors.icon)
                Spacer(Modifier.width(28.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(playlist.name, color = colors.text, fontSize = 17.sp)
                    Text(playlist.subtitle, color = colors.subtitle, fontSize = 14.sp)
                }
                MenuDots(colors.icon) { playlistMenuItems(playlist, app, dialogs, nav) }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// detail screens
// ---------------------------------------------------------------------------

@Composable
fun DetailTrackRow(index: Int?, song: Song, app: AppState, onTap: () -> Unit, onMenu: () -> List<MenuItemSpec>) {
    val colors = LocalViColors.current
    val isCurrent = song.id == app.currentSongId
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isCurrent) colors.highlighted else Color.Transparent)
            .clickable { onTap() }
            .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (index != null) {
            Text(
                text = "$index",
                color = colors.subtitle,
                fontSize = 16.sp,
                modifier = Modifier.width(34.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                song.title,
                color = if (isCurrent) colors.accent else colors.text,
                fontSize = 17.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${song.durationText} · ${song.artist.ifEmpty { "<unknown>" }}",
                color = colors.subtitle,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        MenuDots(colors.icon, onMenu)
    }
}

@Composable
fun DetailHeader(art: @Composable () -> Unit, lines: List<Pair<String, String>>, title: String) {
    val colors = LocalViColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(16.dp)
    ) {
        art()
        Spacer(Modifier.width(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = colors.text, fontSize = 20.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(10.dp))
            lines.forEach { (icon, text) ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                    Ic(icon, 20.dp, colors.icon)
                    Spacer(Modifier.width(16.dp))
                    Text(text, color = colors.text, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun AlbumDetailScreen(app: AppState, albumName: String) {
    val colors = LocalViColors.current
    val dialogs = LocalDialogs.current
    val nav = LocalNav.current
    val album = Library.albums.firstOrNull { it.name == albumName } ?: Album(albumName, "", 0)
    val songs = Library.albumSongs(albumName)
    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        ViTopBar(
            title = album.name,
            onBack = { nav.pop() },
            extra = {
                IconButtonNoRipple("shuffle", colors.appBarText, {
                    app.shuffle = true
                    songs.randomOrNull()?.let { app.play(it, songs) }
                })
            },
            onOverflow = {
                LocalMenuRef.show(albumMenuItems(album, app, dialogs, nav), LocalMenuRef.anchorFor(1023f, 200f))
            }
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                DetailHeader(
                    art = { VinylArt(110.dp) },
                    lines = listOf(
                        "note" to "${songs.size} 歌曲",
                        "clock" to mmss(songs.sumOf { it.durationMs }),
                        "calendar" to "-"
                    ),
                    title = album.artist.ifEmpty { album.name }
                )
            }
            itemsIndexed(songs) { index, song ->
                DetailTrackRow(index + 1, song, app, { app.play(song, songs) }) {
                    songMenuItems(app, song, dialogs, nav)
                }
            }
        }
    }
}

@Composable
fun ArtistDetailScreen(app: AppState, artistName: String) {
    val colors = LocalViColors.current
    val dialogs = LocalDialogs.current
    val nav = LocalNav.current
    val artist = Library.artists.firstOrNull { it.name == artistName } ?: Artist(artistName, 0, 0)
    val songs = Library.artistSongs(artistName)
    val albums = Library.albums.filter { it.artist == artistName }
    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        ViTopBar(
            title = artist.name,
            onBack = { nav.pop() },
            extra = {
                IconButtonNoRipple("shuffle", colors.appBarText, {
                    app.shuffle = true
                    songs.randomOrNull()?.let { app.play(it, songs) }
                })
            },
            onOverflow = {
                LocalMenuRef.show(artistMenuItems(artist, app, dialogs), LocalMenuRef.anchorFor(1023f, 200f))
            }
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                DetailHeader(
                    art = { PersonArt(110.dp) },
                    lines = listOf(
                        "album" to "${artist.albumCount} 专辑",
                        "note" to "${songs.size} 歌曲",
                        "clock" to mmss(songs.sumOf { it.durationMs })
                    ),
                    title = artist.name
                )
            }
            if (albums.isNotEmpty()) {
                items(albums) { alb ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { nav.push("album", alb.name) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VinylArt(56.dp)
                        Spacer(Modifier.width(16.dp))
                        Text(alb.name, color = colors.text, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            itemsIndexed(songs) { index, song ->
                DetailTrackRow(index + 1, song, app, { app.play(song, songs) }) {
                    songMenuItems(app, song, dialogs, nav)
                }
            }
        }
    }
}

@Composable
fun GenreDetailScreen(app: AppState, genreName: String) {
    val colors = LocalViColors.current
    val dialogs = LocalDialogs.current
    val nav = LocalNav.current
    val songs = Library.genreSongs(genreName)
    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        ViTopBar(title = genreName, onBack = { nav.pop() }, onOverflow = {
            LocalMenuRef.show(libraryOverflowItems(app), LocalMenuRef.anchorFor(1023f, 200f))
        })
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(songs) { song ->
                DetailTrackRow(null, song, app, { app.play(song, songs) }) {
                    songMenuItems(app, song, dialogs, nav)
                }
            }
        }
    }
}

@Composable
fun PlaylistDetailScreen(app: AppState, playlistId: String) {
    val colors = LocalViColors.current
    val dialogs = LocalDialogs.current
    val nav = LocalNav.current
    val playlist = app.playlist(playlistId) ?: return
    val songs = playlist.songIds.mapNotNull { Library.byId(it) }
    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        ViTopBar(
            title = playlist.name,
            onBack = { nav.pop() },
            extra = {
                IconButtonNoRipple("shuffle", colors.appBarText, {
                    app.shuffle = true
                    songs.randomOrNull()?.let { app.play(it, songs) }
                })
                IconButtonNoRipple("play", colors.appBarText, {
                    songs.firstOrNull()?.let { app.play(it, songs) }
                })
            },
            onOverflow = {
                LocalMenuRef.show(playlistMenuItems(playlist, app, dialogs, nav), LocalMenuRef.anchorFor(1023f, 200f))
            }
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon = when (playlist.kind) {
                        "favorite" -> "heart_fill"
                        "user" -> "note"
                        else -> "list"
                    }
                    Ic(icon, 40.dp, colors.icon)
                    Spacer(Modifier.width(22.dp))
                    Text(
                        "${songs.size} 歌曲 · ${mmss(songs.sumOf { it.durationMs })}",
                        color = colors.text,
                        fontSize = 17.sp
                    )
                }
            }
            itemsIndexed(songs) { index, song ->
                DetailTrackRow(index + 1, song, app, { app.play(song, songs) }) {
                    songMenuItems(app, song, dialogs, nav)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// search
// ---------------------------------------------------------------------------

@Composable
fun SearchScreen(app: AppState, query: String, onQuery: (String) -> Unit) {
    val colors = LocalViColors.current
    val dialogs = LocalDialogs.current
    val nav = LocalNav.current
    val songs = Library.songs.filter {
        it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true)
    }
    val albums = Library.albums.filter { it.name.contains(query, true) }
    val artists = Library.artists.filter { it.name.contains(query, true) }
    val empty = query.isNotEmpty() && songs.isEmpty() && albums.isEmpty() && artists.isEmpty()
    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.appBar)
                .statusBarsPadding()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButtonNoRipple("back", colors.appBarText, { nav.pop() })
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = query,
                    onValueChange = onQuery,
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = colors.appBarText, fontSize = 19.sp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (query.isEmpty()) {
                    Text("搜索", color = colors.appBarText.copy(alpha = 0.7f), fontSize = 19.sp)
                }
            }
            IconButtonNoRipple("close", colors.appBarText, { onQuery("") })
        }
        if (empty) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("没有找到结果", color = colors.subtitle, fontSize = 17.sp)
            }
            return@Column
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (songs.isNotEmpty()) {
                item { SectionHeader("歌曲") }
                items(songs) { song ->
                    DetailTrackRow(null, song, app, { app.play(song, songs) }) {
                        songMenuItems(app, song, dialogs, nav)
                    }
                }
            }
            if (albums.isNotEmpty()) {
                item { SectionHeader("专辑") }
                items(albums) { album ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { nav.push("album", album.name) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VinylArt(46.dp)
                        Spacer(Modifier.width(16.dp))
                        Text(album.name, color = colors.text, fontSize = 17.sp)
                    }
                }
            }
            if (artists.isNotEmpty()) {
                item { SectionHeader("艺术家") }
                items(artists) { artist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { nav.push("artist", artist.name) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PersonArt(46.dp)
                        Spacer(Modifier.width(16.dp))
                        Text(artist.name, color = colors.text, fontSize = 17.sp)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// folders
// ---------------------------------------------------------------------------

data class FolderEntry(val name: String, val folder: Boolean, val size: String, val path: String)

val folderTree: Map<String, List<FolderEntry>> = mapOf(
    "/" to listOf(FolderEntry("storage", true, "", "/storage")),
    "/storage" to listOf(FolderEntry("emulated", true, "", "/storage/emulated")),
    "/storage/emulated" to listOf(FolderEntry("0", true, "", "/storage/emulated/0")),
    "/storage/emulated/0" to listOf(
        FolderEntry("Download", true, "", "/storage/emulated/0/Download"),
        FolderEntry("Music", true, "", "/storage/emulated/0/Music")
    ),
    "/storage/emulated/0/Download" to listOf(
        FolderEntry("BlackBoxBench", true, "", "/storage/emulated/0/Download/BlackBoxBench")
    ),
    "/storage/emulated/0/Download/BlackBoxBench" to listOf(
        FolderEntry("sample_audio.wav", false, "30.19 KB", "/storage/emulated/0/Download/BlackBoxBench/sample_audio.wav")
    ),
    "/storage/emulated/0/Music" to listOf(
        FolderEntry("01 Neon Harbour - Track 1.mp3", false, "1.77 MB", "/storage/emulated/0/Music/01 Neon Harbour - Track 1.mp3"),
        FolderEntry("02 Neon Harbour - Track 2.mp3", false, "2.44 MB", "/storage/emulated/0/Music/02 Neon Harbour - Track 2.mp3"),
        FolderEntry("03 Neon Harbour - Track 3.mp3", false, "2.03 MB", "/storage/emulated/0/Music/03 Neon Harbour - Track 3.mp3")
    )
)

fun songForPath(path: String): Song? = Library.songs.firstOrNull { it.path == path }

@Composable
fun FoldersScreen(app: AppState, path: String, onPath: (String) -> Unit) {
    val colors = LocalViColors.current
    val dialogs = LocalDialogs.current
    val nav = LocalNav.current
    val entries = folderTree[path] ?: emptyList()
    val segments = path.trim('/').split("/").filter { it.isNotEmpty() }
    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        ViTopBar(
            title = "Vinyl Music Player",
            onMenu = { nav.pop() },
            onSearch = { nav.push("search") },
            extra = {
                IconButtonNoRipple("note", colors.appBarText, { app.snackbar = "文件夹视图" })
            },
            onOverflow = {
                LocalMenuRef.show(libraryOverflowItems(app), LocalMenuRef.anchorFor(1023f, 200f))
            }
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.appBar)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (path != "/") {
                IconButtonNoRipple("back", colors.appBarText, {
                    onPath(segments.dropLast(1).joinToString("/", prefix = "/").ifEmpty { "/" })
                })
            }
            segments.forEachIndexed { index, segment ->
                Text(
                    text = segment.uppercase(),
                    color = colors.appBarText.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    modifier = Modifier
                        .clickable {
                            onPath("/" + segments.take(index + 1).joinToString("/"))
                        }
                        .padding(horizontal = 6.dp)
                )
                if (index != segments.lastIndex) {
                    Text(">", color = colors.appBarText.copy(alpha = 0.6f), fontSize = 13.sp)
                }
            }
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(entries) { entry ->
                val song = if (!entry.folder) songForPath(entry.path) else null
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (entry.folder) onPath(entry.path)
                            else song?.let { app.play(it, Library.songs) }
                        }
                        .padding(start = 16.dp, end = 4.dp, top = 14.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Ic(if (entry.folder) "folder" else "note", 28.dp, colors.icon)
                    Spacer(Modifier.width(24.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(entry.name, color = colors.text, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (!entry.folder) {
                            Text(entry.size, color = colors.subtitle, fontSize = 14.sp)
                        }
                    }
                    MenuDots(colors.icon) {
                        if (entry.folder) {
                            listOf(
                                MenuItemSpec("作为下一首播放") { app.snackbar = "作为下一首播放" },
                                MenuItemSpec("加入播放队列") { app.snackbar = "加入播放队列" },
                                MenuItemSpec("加入播放列表…") { dialogs.addToPlaylist = Library.songs.map { it.id } },
                                MenuItemSpec("设为起始目录") { app.snackbar = "已设为起始目录" },
                                MenuItemSpec("扫描") { app.snackbar = "扫描 ${entry.name}" },
                                MenuItemSpec("从设备中删除", destructive = true) { nav.push("sdcard") }
                            )
                        } else {
                            songMenuItems(app, song ?: Library.songs[0], dialogs, nav, showScan = true)
                        }
                    }
                }
                ViDivider()
            }
        }
    }
}
