package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ------------------------------------------------------------ library host

@Composable
fun LibraryHost(state: AppState, onExpandPlayer: () -> Unit) {
    val accent = Color(state.accentColor)
    val primary = Color(state.primaryColor)
    val tabs = state.categoryOrder.filter { state.categoryEnabled[it] }.map { CATEGORY_LABELS[it] }
    val visibleCategories = state.categoryOrder.filter { state.categoryEnabled[it] }
    val selectedTab = visibleCategories.indexOf(state.currentCategory()).coerceAtLeast(0)

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
            if (state.multiSelect) {
                MultiSelectTopBar(state, visibleCategories)
            } else {
                TopBarTitle(
                    title = "Vinyl Music Player",
                    primary = primary,
                    onMenu = { state.drawerOpen = true },
                    onSearch = { state.searchActive = true; state.searchQuery = "" },
                    onOverflow = { },
                    overflowContent = { dismiss -> LibraryOverflow(state, dismiss) }
                )
            }
            LibraryTabRow(tabs, selectedTab, accent, primary) { index ->
                state.screen = categoryScreen(visibleCategories[index])
                state.lastLibraryScreen = state.screen
                state.persist()
            }
            Box(Modifier.weight(1f)) {
                when (state.currentCategory()) {
                    0 -> SongsTab(state)
                    1 -> AlbumsTab(state)
                    2 -> ArtistsTab(state)
                    3 -> GenresTab(state)
                    else -> PlaylistsTab(state)
                }
            }
            MiniPlayer(state) { onExpandPlayer() }
        }
    }
    if (state.searchActive) SearchScreen(state)
}

private fun AppState.currentCategory(): Int {
    val order = categoryOrder.filter { categoryEnabled[it] }
    val index = when (screen) {
        Screen.LIBRARY_ALBUMS -> 1
        Screen.LIBRARY_ARTISTS -> 2
        Screen.LIBRARY_GENRES -> 3
        Screen.LIBRARY_PLAYLISTS -> 4
        else -> 0
    }
    return if (order.contains(index)) index else (order.firstOrNull() ?: 0)
}

private fun categoryScreen(category: Int): Screen = when (category) {
    1 -> Screen.LIBRARY_ALBUMS
    2 -> Screen.LIBRARY_ARTISTS
    3 -> Screen.LIBRARY_GENRES
    4 -> Screen.LIBRARY_PLAYLISTS
    else -> Screen.LIBRARY_SONGS
}

@Composable
private fun MultiSelectTopBar(state: AppState, visibleCategories: List<Int>) {
    val primary = Color(state.primaryColor)
    TopBarShell(primary) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clickable { state.multiSelect = false; state.selectedIds.clear() },
                contentAlignment = Alignment.Center
            ) { CloseGlyph(Color.White) }
            Text(
                "已选择 ${state.selectedIds.size}",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            )
            Box(
                Modifier
                    .size(48.dp)
                    .clickable {
                        state.selectedIds.clear()
                        state.selectedIds.addAll(state.sortedSongs().map { it.id })
                    },
                contentAlignment = Alignment.Center
            ) { SelectAllGlyph(Color.White) }
            Box(
                Modifier
                    .size(48.dp)
                    .clickable { state.dialog = DialogKind.ADD_TO_PLAYLIST },
                contentAlignment = Alignment.Center
            ) { PlusGlyph(Color.White) }
            OverflowButton(entries = {
                listOf(
                    MenuEntry("加入播放列表…") { state.dialog = DialogKind.ADD_TO_PLAYLIST },
                    MenuEntry("全部选择") {
                        state.selectedIds.clear()
                        state.selectedIds.addAll(state.sortedSongs().map { it.id })
                    },
                    MenuEntry("从设备中删除", danger = true) {
                        state.toast("已从设备中删除 ${state.selectedIds.size} 首歌曲")
                        state.multiSelect = false
                        state.selectedIds.clear()
                    },
                )
            }, tint = Color.White)
        }
    }
    Box(Modifier.fillMaxWidth().height(0.dp))
}

@Composable
private fun LibraryOverflow(state: AppState, dismiss: () -> Unit) {
    val songs = state.currentCategory() == 0
    val entries = mutableListOf(
        MenuEntry("随机播放所有歌曲") {
            state.playQueue(state.sortedSongs().map { it.id }, 0); state.toast("随机播放所有歌曲")
        },
    )
    if (songs) {
        entries.add(MenuEntry("网格尺寸", submenu = GRID_SIZES, onSubmenuSelect = { index ->
            state.gridSize = index; state.persist()
        }))
        entries.add(MenuEntry("排序方式", submenu = SORT_MODES, onSubmenuSelect = { index ->
            state.sortMode = index; state.persist()
        }))
        entries.add(MenuEntry("显示页脚", checked = state.showFooter) {
            state.showFooter = !state.showFooter; state.persist()
        })
        entries.add(MenuEntry("着色页脚", checked = state.tintFooter) {
            state.tintFooter = !state.tintFooter; state.persist()
        })
    } else {
        entries.add(MenuEntry("网格尺寸", submenu = GRID_SIZES, onSubmenuSelect = { index ->
            state.gridSize = index; state.persist()
        }))
    }
    // The menu itself is rendered by the caller through OverflowMenu.
    OverflowMenuHost(entries, dismiss)
}

@Composable
private fun OverflowMenuHost(entries: List<MenuEntry>, dismiss: () -> Unit) {
    OverflowMenu(expanded = true, entries = entries, onDismiss = dismiss)
}

// ------------------------------------------------------------------- songs

@Composable
fun SongsTab(state: AppState) {
    val accent = Color(state.accentColor)
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clickable {
                    state.playQueue(state.sortedSongs().map { it.id }, 0)
                    state.toast("随机播放所有歌曲")
                }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShuffleGlyph(accent, 24.dp)
            Spacer(Modifier.width(16.dp))
            Text("随机播放所有歌曲", color = accent, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(state.sortedSongs(), key = { it.id }) { track ->
                TrackRow(
                    track = track,
                    state = state,
                    showNote = state.currentTrack?.id == track.id,
                    selected = state.selectedIds.contains(track.id) || state.currentTrack?.id == track.id,
                    onOverflow = { },
                    onClick = {
                        if (state.multiSelect) {
                            if (state.selectedIds.contains(track.id)) state.selectedIds.remove(track.id)
                            else state.selectedIds.add(track.id)
                        } else {
                            state.playTrack(track.id, state.sortedSongs().map { it.id })
                        }
                    },
                    onLongClick = {
                        state.multiSelect = true
                        state.selectedIds.clear()
                        state.selectedIds.add(track.id)
                    }
                )
            }
            if (state.showFooter) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(if (state.tintFooter) accent else Color.Transparent),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            "${state.songs.size} 首歌曲",
                            color = if (state.tintFooter) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ albums

@Composable
fun AlbumsTab(state: AppState) {
    val albums = state.albums()
    LazyVerticalGrid(
        columns = GridCells.Fixed(when (state.gridSize) {
            0 -> 1
            1 -> 2
            2 -> 3
            else -> 4
        }),
        modifier = Modifier.fillMaxSize()
    ) {
        items(albums, key = { it.first }) { (album, tracks) ->
            Column(
                Modifier
                    .padding(2.dp)
                    .clickable { state.openAlbum = album; state.screen = Screen.ALBUM_DETAIL }
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .background(Color(0xFF1E1E1E)),
                    contentAlignment = Alignment.Center
                ) { VinylGlyph(Color(0xFF8A8A8A), 110.dp) }
                Column(Modifier.padding(10.dp)) {
                    Text(album, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                    Spacer(Modifier.height(2.dp))
                    val subtitle = if (tracks.isEmpty()) "0 歌曲"
                    else "${tracks.first().artist} · ${tracks.size} 歌曲"
                    Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
        }
    }
}

// ----------------------------------------------------------------- artists

@Composable
fun ArtistsTab(state: AppState) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(state.artists(), key = { it.first }) { (artist, tracks) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clickable { state.openArtist = artist; state.screen = Screen.ARTIST_DETAIL }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2A2A2A)),
                    contentAlignment = Alignment.Center
                ) { NoteGlyph(Color(0xFFBDBDBD), 24.dp) }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(artist, fontSize = 16.sp)
                    Text("${tracks.size} 歌曲", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ------------------------------------------------------------------ genres

@Composable
fun GenresTab(state: AppState) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(state.genres(), key = { it.first }) { (genre, tracks) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clickable { state.openGenre = genre; state.screen = Screen.GENRE_DETAIL }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NoteGlyph(MaterialTheme.colorScheme.onSurfaceVariant, 24.dp)
                Spacer(Modifier.width(16.dp))
                Text(genre, fontSize = 16.sp)
            }
        }
    }
}

// --------------------------------------------------------------- playlists

@Composable
fun PlaylistsTab(state: AppState) {
    val accent = Color(state.accentColor)
    LazyColumn(Modifier.fillMaxSize()) {
        items(state.playlists, key = { it.id }) { playlist ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clickable {
                        state.openPlaylistId = playlist.id
                        state.screen = Screen.PLAYLIST_DETAIL
                    }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                    when (playlist.auto) {
                        AutoKind.RECENT_ADDED -> PlusGlyph(MaterialTheme.colorScheme.onSurfaceVariant, 24.dp)
                        AutoKind.HISTORY -> ClockGlyph(MaterialTheme.colorScheme.onSurfaceVariant, 24.dp)
                        AutoKind.NOT_PLAYED -> ClockGlyph(accent, 24.dp)
                        AutoKind.MOST_PLAYED -> TrendGlyph(MaterialTheme.colorScheme.onSurfaceVariant, 24.dp)
                        AutoKind.FAVORITES -> HeartGlyph(MaterialTheme.colorScheme.onSurfaceVariant, 24.dp)
                        null -> QueueGlyph(MaterialTheme.colorScheme.onSurfaceVariant, 24.dp)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(playlist.name, fontSize = 16.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        playlistSubtitle(playlist, state),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OverflowButton(entries = {
                    listOf(
                        MenuEntry("播放") {
                            state.playQueue(playlist.trackIds.toList(), 0)
                        },
                        MenuEntry("重命名") {
                            if (!playlist.isAuto) {
                                state.openPlaylistId = playlist.id
                                state.dialog = DialogKind.RENAME_PLAYLIST
                            }
                        },
                        MenuEntry("删除", danger = true) {
                            if (!playlist.isAuto) state.deletePlaylist(playlist.id)
                        },
                    )
                }, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

fun playlistSubtitle(playlist: Playlist, state: AppState): String {
    val prefix = when (playlist.auto) {
        AutoKind.RECENT_ADDED, AutoKind.HISTORY, AutoKind.NOT_PLAYED -> "本月 · "
        else -> ""
    }
    return "$prefix${playlist.trackIds.size} 歌曲"
}

// ----------------------------------------------------------------- folders

@Composable
fun FoldersView(state: AppState) {
    var scanned by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("/storage/emulated/0/Music", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(Modifier.fillMaxSize()) {
            itemsIndexed(state.songs, key = { _, t -> t.id }) { _, track ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clickable { state.playTrack(track.id, state.songs.map { it.id }) }
                        .padding(start = 16.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(track.fileName, fontSize = 15.sp, maxLines = 1)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "%.1f MB".format(track.sizeBytes / 1024f / 1024f),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OverflowButton(entries = {
                        onOverflowEntries(track, state) + MenuEntry("扫描") {
                            scanned = true
                            state.toast("已扫描 ${track.fileName}")
                        }
                    }, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ------------------------------------------------------------------ search

@Composable
fun SearchScreen(state: AppState) {
    val primary = Color(state.primaryColor)
    val query = state.searchQuery
    val albumHits = state.albums().filter { it.first.contains(query, true) }
    val trackHits = state.songs.filter { it.title.contains(query, true) }
    val artistHits = state.artists().filter { it.first.contains(query, true) }
    val totalHits = albumHits.size + trackHits.size + artistHits.size
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBarShell(primary) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clickable { state.searchActive = false },
                    contentAlignment = Alignment.Center
                ) { BackGlyph(Color.White) }
                Box(
                    Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x33FFFFFF))
                        .clickable { }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (query.isEmpty()) Text("搜索曲库", color = Color(0xCCFFFFFF), fontSize = 15.sp)
                    else Text(query, color = Color.White, fontSize = 15.sp)
                }
                Box(
                    Modifier
                        .size(48.dp)
                        .clickable { state.searchQuery = "" },
                    contentAlignment = Alignment.Center
                ) { CloseGlyph(Color.White) }
            }
        }
        if (totalHits == 0) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("没有找到结果", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                if (albumHits.isNotEmpty()) {
                    item { SectionHeader("专辑") }
                    items(albumHits) { (album, tracks) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                                .clickable { state.openAlbum = album; state.screen = Screen.ALBUM_DETAIL; state.searchActive = false }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CoverArt(48, album)
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(album, fontSize = 16.sp)
                                Text(
                                    "${tracks.first().artist} · ${tracks.size} 歌曲",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                if (artistHits.isNotEmpty()) {
                    item { SectionHeader("艺术家") }
                    items(artistHits) { (artist, tracks) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clickable { state.openArtist = artist; state.screen = Screen.ARTIST_DETAIL; state.searchActive = false }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) { Text(artist, fontSize = 16.sp) }
                    }
                }
                if (trackHits.isNotEmpty()) {
                    item { SectionHeader("歌曲") }
                    items(trackHits) { track ->
                        TrackRow(
                            track = track,
                            state = state,
                            showNote = state.currentTrack?.id == track.id,
                            selected = false,
                            onOverflow = { },
                            onClick = { state.playTrack(track.id, trackHits.map { it.id }) }
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- details

@Composable
fun AlbumDetailScreen(state: AppState) {
    val album = state.openAlbum ?: return
    val tracks = state.songs.filter { it.album == album }
    val accent = Color(state.accentColor)
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBarTitle(
            title = album,
            primary = Color(state.primaryColor),
            onBack = { state.screen = state.lastLibraryScreen },
            trailingCustom = {
                Box(
                    Modifier
                        .size(48.dp)
                        .clickable { state.playQueue(tracks.map { it.id }.shuffled(), 0) },
                    contentAlignment = Alignment.Center
                ) { ShuffleGlyph(Color.White, 24.dp) }
            },
            onOverflow = { },
            overflowContent = { dismiss ->
                OverflowMenu(
                    expanded = true,
                    entries = listOf(
                        MenuEntry("百科") { state.toast("正在打开百科…") },
                        MenuEntry("作为下一首播放") { tracks.firstOrNull()?.let { state.playNext(it.id) } },
                        MenuEntry("加入播放队列") { tracks.forEach { state.addToQueue(it.id) } },
                        MenuEntry("加入播放列表…") {
                            state.selectedIds.clear()
                            state.selectedIds.addAll(tracks.map { it.id })
                            state.dialog = DialogKind.ADD_TO_PLAYLIST
                        },
                        MenuEntry("查看艺术家") {
                            state.openArtist = tracks.firstOrNull()?.artist
                            state.screen = Screen.ARTIST_DETAIL
                        },
                        MenuEntry("音乐标签编辑器") {
                            state.tagEditorTrackId = tracks.firstOrNull()?.id
                            state.screen = Screen.TAG_EDITOR
                        },
                        MenuEntry("从设备中删除", danger = true) { state.toast("已从设备中删除专辑") },
                    ),
                    onDismiss = dismiss
                )
            }
        )
        Row(Modifier.fillMaxWidth().padding(16.dp)) {
            Box(
                Modifier
                    .size(150.dp)
                    .background(Color(0xFF1E1E1E)),
                contentAlignment = Alignment.Center
            ) { VinylGlyph(Color(0xFF8A8A8A), 100.dp) }
            Spacer(Modifier.width(16.dp))
            Column {
                MetaLine("person", tracks.firstOrNull()?.artist ?: "")
                MetaLine("note", "${tracks.size} 歌曲")
                MetaLine("clock", formatTotal(tracks.sumOf { it.seconds }))
                MetaLine("calendar", tracks.firstOrNull()?.year?.ifEmpty { "-" } ?: "-")
            }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            itemsIndexed(tracks, key = { _, t -> t.id }) { index, track ->
                TrackRow(
                    track = track,
                    state = state,
                    index = (index + 1).toString(),
                    showNote = state.currentTrack?.id == track.id,
                    selected = state.currentTrack?.id == track.id,
                    onOverflow = { },
                    onClick = { state.playTrack(track.id, tracks.map { it.id }) }
                )
            }
        }
    }
}

@Composable
private fun MetaLine(kind: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (kind) {
            "person" -> NoteGlyph(MaterialTheme.colorScheme.onSurfaceVariant, 20.dp)
            "note" -> NoteGlyph(MaterialTheme.colorScheme.onSurfaceVariant, 20.dp)
            "clock" -> ClockGlyph(MaterialTheme.colorScheme.onSurfaceVariant, 20.dp)
            else -> ClockGlyph(MaterialTheme.colorScheme.onSurfaceVariant, 20.dp)
        }
        Spacer(Modifier.width(12.dp))
        Text(value, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun ArtistDetailScreen(state: AppState) {
    val artist = state.openArtist ?: return
    val tracks = state.songs.filter { it.artist == artist }
    val albums = tracks.groupBy { it.album }.toList()
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBarTitle(
            title = artist,
            primary = Color(state.primaryColor),
            onBack = { state.screen = state.lastLibraryScreen },
            trailingCustom = {
                Box(
                    Modifier
                        .size(48.dp)
                        .clickable { state.playQueue(tracks.map { it.id }.shuffled(), 0) },
                    contentAlignment = Alignment.Center
                ) { ShuffleGlyph(Color.White, 24.dp) }
            }
        )
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2A2A2A)),
                        contentAlignment = Alignment.Center
                    ) { NoteGlyph(Color(0xFFBDBDBD), 40.dp) }
                    Spacer(Modifier.width(20.dp))
                    Column {
                        Text("${albums.size} 专辑 · ${tracks.size} 歌曲", fontSize = 14.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(formatTotal(tracks.sumOf { it.seconds }), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            items(albums) { (album, albumTracks) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clickable { state.openAlbum = album; state.screen = Screen.ALBUM_DETAIL }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CoverArt(48, album)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(album, fontSize = 16.sp)
                        Text("${albumTracks.size} 歌曲", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item { SectionHeader("歌曲") }
            items(tracks, key = { it.id }) { track ->
                TrackRow(
                    track = track,
                    state = state,
                    showNote = state.currentTrack?.id == track.id,
                    selected = false,
                    onOverflow = { },
                    onClick = { state.playTrack(track.id, tracks.map { it.id }) }
                )
            }
        }
    }
}

@Composable
fun GenreDetailScreen(state: AppState) {
    val genre = state.openGenre ?: return
    val tracks = state.songs.filter { it.genre == genre }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBarTitle(
            title = genre,
            primary = Color(state.primaryColor),
            onBack = { state.screen = state.lastLibraryScreen },
            trailingCustom = {
                Box(
                    Modifier
                        .size(48.dp)
                        .clickable { state.playQueue(tracks.map { it.id }.shuffled(), 0) },
                    contentAlignment = Alignment.Center
                ) { ShuffleGlyph(Color.White, 24.dp) }
            }
        )
        LazyColumn(Modifier.fillMaxSize()) {
            items(tracks, key = { it.id }) { track ->
                TrackRow(
                    track = track,
                    state = state,
                    showNote = state.currentTrack?.id == track.id,
                    selected = false,
                    onOverflow = { },
                    onClick = { state.playTrack(track.id, tracks.map { it.id }) }
                )
            }
        }
    }
}

@Composable
fun PlaylistDetailScreen(state: AppState) {
    val playlist = state.playlist(state.openPlaylistId) ?: return
    val tracks = Library.byIds(playlist.trackIds.toList())
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBarTitle(
            title = playlist.name,
            primary = Color(state.primaryColor),
            onBack = { state.screen = Screen.LIBRARY_PLAYLISTS },
            trailingCustom = {
                Row {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clickable { state.playQueue(tracks.map { it.id }.shuffled(), 0) },
                        contentAlignment = Alignment.Center
                    ) { ShuffleGlyph(Color.White, 24.dp) }
                    Box(
                        Modifier
                            .size(48.dp)
                            .clickable { state.playQueue(tracks.map { it.id }, 0) },
                        contentAlignment = Alignment.Center
                    ) { PlayGlyph(Color.White, 24.dp) }
                }
            },
            onOverflow = { },
            overflowContent = { dismiss ->
                OverflowMenu(
                    expanded = true,
                    entries = listOf(
                        MenuEntry("作为下一首播放") { tracks.firstOrNull()?.let { state.playNext(it.id) } },
                        MenuEntry("加入播放队列") { tracks.forEach { state.addToQueue(it.id) } },
                        MenuEntry("加入播放列表…") {
                            state.selectedIds.clear()
                            state.selectedIds.addAll(tracks.map { it.id })
                            state.dialog = DialogKind.ADD_TO_PLAYLIST
                        },
                        MenuEntry("重命名") {
                            if (!playlist.isAuto) state.dialog = DialogKind.RENAME_PLAYLIST
                            else state.toast("自动播放列表不可重命名")
                        },
                        MenuEntry("另存为") {
                            state.selectedIds.clear()
                            state.selectedIds.addAll(tracks.map { it.id })
                            state.dialog = DialogKind.SAVE_QUEUE
                        },
                        MenuEntry("删除", danger = true) {
                            if (!playlist.isAuto) {
                                state.deletePlaylist(playlist.id)
                                state.screen = Screen.LIBRARY_PLAYLISTS
                            } else state.toast("自动播放列表不可删除")
                        },
                    ),
                    onDismiss = dismiss
                )
            }
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ClockGlyph(MaterialTheme.colorScheme.onSurfaceVariant, 22.dp)
            Spacer(Modifier.width(12.dp))
            Text(
                "${tracks.size} 歌曲 · ${formatTotal(tracks.sumOf { it.seconds })}",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(tracks, key = { it.id }) { track ->
                TrackRow(
                    track = track,
                    state = state,
                    showNote = state.currentTrack?.id == track.id,
                    selected = false,
                    onOverflow = { },
                    onClick = { state.playTrack(track.id, tracks.map { it.id }) }
                )
            }
        }
    }
}
