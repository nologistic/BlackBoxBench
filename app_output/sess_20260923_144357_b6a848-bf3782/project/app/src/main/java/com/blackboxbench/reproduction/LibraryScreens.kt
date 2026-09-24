package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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

data class SongActions(
    val playNext: (Track) -> Unit,
    val addToQueue: (Track) -> Unit,
    val addToPlaylist: (Track) -> Unit,
    val viewAlbum: (Track) -> Unit,
    val viewArtist: (Track) -> Unit,
    val share: (Track) -> Unit,
    val tagEditor: (Track) -> Unit,
    val details: (Track) -> Unit,
    val setRingtone: (Track) -> Unit,
    val delete: (Track) -> Unit,
    val removeFromPlaylist: ((Track) -> Unit)? = null
)

@Composable
fun MenuItem(text: String, colors: AppColors, danger: Boolean = false, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text, color = if (danger) Color(0xFFD32F2F) else colors.onSurface, fontSize = 15.sp) },
        onClick = onClick
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongListRow(
    track: Track,
    state: AppState,
    colors: AppColors,
    actions: SongActions,
    number: Int? = null,
    showDragHandle: Boolean = false,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val isCurrent = state.currentTrack?.id == track.id
    val background = if (selected) colors.accent.copy(alpha = 0.14f) else colors.surface
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(background)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showDragHandle) {
                Box(Modifier.width(32.dp), contentAlignment = Alignment.Center) {
                    DragHandleGlyph(size = 22.dp, tint = colors.secondaryText)
                }
            }
            if (number != null) {
                Box(Modifier.width(32.dp), contentAlignment = Alignment.Center) {
                    Text("$number", color = colors.secondaryText, fontSize = 14.sp)
                }
            }
            Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                if (isCurrent) VinylIcon(size = 40.dp, tint = colors.secondaryText)
                else CoverArt(track.album, size = 56.dp)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    track.title,
                    color = colors.onSurface,
                    fontSize = 17.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${track.artist} • ${track.album}",
                    color = colors.secondaryText,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                Modifier
                    .size(48.dp)
                    .clickable { menuOpen = true },
                contentAlignment = Alignment.Center
            ) {
                MoreVertGlyph(size = 22.dp, tint = colors.secondaryText)
            }
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            if (actions.removeFromPlaylist != null) {
                MenuItem("从播放列表中移除", colors) { menuOpen = false; actions.removeFromPlaylist.invoke(track) }
            }
            MenuItem("作为下一首播放", colors) { menuOpen = false; actions.playNext(track) }
            MenuItem("加入播放队列", colors) { menuOpen = false; actions.addToQueue(track) }
            MenuItem("加入播放列表…", colors) { menuOpen = false; actions.addToPlaylist(track) }
            MenuItem("查看专辑", colors) { menuOpen = false; actions.viewAlbum(track) }
            MenuItem("查看艺术家", colors) { menuOpen = false; actions.viewArtist(track) }
            MenuItem("分享", colors) { menuOpen = false; actions.share(track) }
            MenuItem("音乐标签编辑器", colors) { menuOpen = false; actions.tagEditor(track) }
            MenuItem("详情", colors) { menuOpen = false; actions.details(track) }
            MenuItem("设为铃声", colors) { menuOpen = false; actions.setRingtone(track) }
            MenuItem("从设备中删除", colors, danger = true) { menuOpen = false; actions.delete(track) }
        }
    }
}

@Composable
fun ShuffleAllHeader(count: Int, colors: AppColors, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShuffleGlyph(size = 24.dp, tint = colors.accent)
        Spacer(Modifier.width(24.dp))
        Text("随机播放所有歌曲", color = colors.accent, fontSize = 17.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SongsTab(state: AppState, colors: AppColors, actions: SongActions, onOpenPlayer: () -> Unit) {
    if (state.tracks.isEmpty()) {
        EmptyState("没有找到歌曲", colors)
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            ShuffleAllHeader(state.tracks.size, colors) {
                val shuffled = state.tracks.shuffled()
                state.shuffle = true
                state.playTrack(shuffled.first().id, shuffled.map { it.id })
                onOpenPlayer()
            }
        }
        items(state.tracks, key = { it.id }) { track ->
            SongListRow(
                track = track,
                state = state,
                colors = colors,
                actions = actions,
                onClick = { state.playTrack(track.id, state.tracks.map { it.id }) }
            )
            DividerLine(colors, indent = 88.dp)
        }
    }
}

@Composable
fun AlbumsTab(state: AppState, colors: AppColors, navigator: Navigator) {
    val albums = state.albums()
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(albums, key = { it.key }) { album ->
            GridAlbumCard(
                title = album.title,
                subtitle = "${album.artist} • ${album.tracks.size} 歌曲",
                colors = colors
            ) { navigator.push(Screen.Album(album.key)) }
        }
    }
    if (albums.isEmpty()) EmptyState("没有找到专辑", colors)
}

@Composable
fun ArtistsTab(state: AppState, colors: AppColors, navigator: Navigator) {
    val artists = state.artists()
    LazyColumn(Modifier.fillMaxSize()) {
        items(artists, key = { it.name }) { artist ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
                    .clickable { navigator.push(Screen.Artist(artist.name)) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(coverColor(artist.name)),
                    contentAlignment = Alignment.Center
                ) { ArtistGlyph(size = 28.dp, tint = Color(0xFFEEEEEE)) }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(artist.name, color = colors.onSurface, fontSize = 17.sp)
                    Text(
                        "${artist.albums.size} 专辑 • ${artist.tracks.size} 歌曲",
                        color = colors.secondaryText,
                        fontSize = 14.sp
                    )
                }
            }
            DividerLine(colors, indent = 80.dp)
        }
    }
    if (artists.isEmpty()) EmptyState("没有找到艺术家", colors)
}

@Composable
fun GenresTab(state: AppState, colors: AppColors, navigator: Navigator) {
    val genres = state.genres()
    LazyColumn(Modifier.fillMaxSize()) {
        items(genres, key = { it.name }) { genre ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
                    .clickable { navigator.push(Screen.Genre(genre.name)) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    GenreGlyph(size = 26.dp, tint = colors.secondaryText)
                }
                Spacer(Modifier.width(20.dp))
                Column(Modifier.weight(1f)) {
                    Text(genre.name, color = colors.onSurface, fontSize = 17.sp)
                    Text("${genre.tracks.size} 歌曲", color = colors.secondaryText, fontSize = 14.sp)
                }
            }
            DividerLine(colors, indent = 76.dp)
        }
    }
    if (genres.isEmpty()) EmptyState("没有找到音乐类型", colors)
}

@Composable
fun PlaylistsTab(state: AppState, colors: AppColors, navigator: Navigator, onCreatePlaylist: () -> Unit) {
    var menuFor by remember { mutableStateOf<String?>(null) }
    var renameTarget by remember { mutableStateOf<Playlist?>(null) }
    var deleteTarget by remember { mutableStateOf<Playlist?>(null) }
    var saveAsTarget by remember { mutableStateOf<Playlist?>(null) }

    val smart = state.smartPlaylists()
    val userPlaylists = state.playlists.toList()

    LazyColumn(Modifier.fillMaxSize()) {
        items(smart, key = { it.kind }) { list ->
            Box {
                PlaylistRow(
                    name = list.name,
                    subtitle = if (list.subtitle.isEmpty()) "${list.trackIds.size} 歌曲" else "${list.subtitle} • ${list.trackIds.size} 歌曲",
                    colors = colors,
                    icon = {
                        when (list.kind) {
                            "recent" -> RecentGlyph(size = 26.dp, tint = colors.secondaryText)
                            "history" -> ClockGlyph(size = 26.dp, tint = colors.secondaryText)
                            "notplayed" -> ClockGlyph(size = 26.dp, tint = colors.secondaryText)
                            else -> TrendGlyph(size = 26.dp, tint = colors.secondaryText)
                        }
                    },
                    onClick = { navigator.push(Screen.Smart(list.kind)) },
                    onOverflow = { menuFor = "smart:${list.kind}" }
                )
                DropdownMenu(expanded = menuFor == "smart:${list.kind}", onDismissRequest = { menuFor = null }) {
                    MenuItem("随机播放此播放列表", colors) {
                        menuFor = null
                        val ids = list.trackIds.shuffled()
                        if (ids.isNotEmpty()) state.playTrack(ids.first(), ids)
                    }
                    MenuItem("播放", colors) {
                        menuFor = null
                        if (list.trackIds.isNotEmpty()) state.playTrack(list.trackIds.first(), list.trackIds)
                    }
                    MenuItem("作为下一首播放", colors) { menuFor = null; state.addToQueue(list.trackIds, next = true) }
                    MenuItem("加入播放队列", colors) { menuFor = null; state.addToQueue(list.trackIds) }
                    MenuItem("设置", colors) { menuFor = null }
                    MenuItem("另存为", colors) { menuFor = null }
                }
            }
            DividerLine(colors, indent = 72.dp)
        }
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(colors.background)
            )
        }
        items(userPlaylists, key = { it.id }) { playlist ->
            Box {
                PlaylistRow(
                    name = playlist.name,
                    subtitle = "${playlist.trackIds.size} 歌曲",
                    colors = colors,
                    icon = {
                        if (playlist.favorites) HeartGlyph(size = 26.dp, tint = Color(0xFF616161))
                        else PlaylistGlyph(size = 26.dp, tint = colors.secondaryText)
                    },
                    onClick = { navigator.push(Screen.Playlist(playlist.id)) },
                    onOverflow = { menuFor = playlist.id }
                )
                DropdownMenu(expanded = menuFor == playlist.id, onDismissRequest = { menuFor = null }) {
                    MenuItem("随机播放此播放列表", colors) {
                        menuFor = null
                        val ids = playlist.trackIds.shuffled()
                        if (ids.isNotEmpty()) state.playTrack(ids.first(), ids)
                    }
                    MenuItem("播放", colors) {
                        menuFor = null
                        if (playlist.trackIds.isNotEmpty()) state.playTrack(playlist.trackIds.first(), playlist.trackIds.toList())
                    }
                    MenuItem("作为下一首播放", colors) { menuFor = null; state.addToQueue(playlist.trackIds.toList(), next = true) }
                    MenuItem("加入播放队列", colors) { menuFor = null; state.addToQueue(playlist.trackIds.toList()) }
                    MenuItem("加入播放列表…", colors) { menuFor = null }
                    MenuItem("重命名", colors) { menuFor = null; renameTarget = playlist }
                    MenuItem("另存为", colors) { menuFor = null; saveAsTarget = playlist }
                    MenuItem("删除", colors, danger = true) { menuFor = null; deleteTarget = playlist }
                }
            }
            DividerLine(colors, indent = 72.dp)
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCreatePlaylist() }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AddBoxGlyph(size = 24.dp, tint = colors.accent)
                Spacer(Modifier.width(20.dp))
                Text("新建播放列表", color = colors.accent, fontSize = 16.sp)
            }
        }
    }

    renameTarget?.let { playlist ->
        TextInputDialog(
            title = "重命名播放列表",
            label = "播放列表名称",
            initial = playlist.name,
            colors = colors,
            onDismiss = { renameTarget = null },
            onConfirm = { name ->
                state.renamePlaylist(playlist.id, name)
                renameTarget = null
            }
        )
    }
    deleteTarget?.let { playlist ->
        ConfirmDialog(
            title = "删除播放列表",
            message = "是否删除播放列表 ${playlist.name}?",
            confirmLabel = "删除",
            colors = colors,
            destructive = true,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                state.deletePlaylist(playlist.id)
                deleteTarget = null
            }
        )
    }
    saveAsTarget?.let { playlist ->
        TextInputDialog(
            title = "另存为",
            label = "播放列表名称",
            initial = "${playlist.name} 副本",
            colors = colors,
            onDismiss = { saveAsTarget = null },
            onConfirm = { name ->
                state.createPlaylist(name, playlist.trackIds.toList())
                saveAsTarget = null
            }
        )
    }
}

@Composable
fun TrackListPage(
    header: @Composable () -> Unit,
    tracks: List<Track>,
    state: AppState,
    colors: AppColors,
    actions: SongActions,
    onOpenPlayer: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize()) {
        item { header() }
        itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
            SongListRow(
                track = track,
                state = state,
                colors = colors,
                actions = actions,
                number = index + 1,
                onClick = {
                    state.playTrack(track.id, tracks.map { it.id })
                }
            )
            DividerLine(colors, indent = 88.dp)
        }
        if (tracks.isEmpty()) {
            item { EmptyState("播放列表为空", colors, Modifier.height(200.dp)) }
        }
    }
}

@Composable
fun AlbumHeader(album: AlbumGroup, colors: AppColors) {
    Column(Modifier.background(colors.surface)) {
        Spacer(Modifier.height(16.dp))
        Row(Modifier.padding(horizontal = 16.dp)) {
            CoverArt(album.title, size = 120.dp, corner = 6.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ArtistGlyph(size = 18.dp, tint = colors.secondaryText)
                    Spacer(Modifier.width(10.dp))
                    Text(album.artist, color = colors.onSurface, fontSize = 16.sp)
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MusicNoteGlyph(size = 18.dp, tint = colors.secondaryText)
                    Spacer(Modifier.width(10.dp))
                    Text("${album.tracks.size} 歌曲", color = colors.onSurface, fontSize = 16.sp)
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TimerGlyph(size = 18.dp, tint = colors.secondaryText)
                    Spacer(Modifier.width(10.dp))
                    Text(formatDuration(album.totalSeconds), color = colors.onSurface, fontSize = 16.sp)
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RecentGlyph(size = 18.dp, tint = colors.secondaryText)
                    Spacer(Modifier.width(10.dp))
                    Text(if (album.year > 0) "${album.year}" else "-", color = colors.onSurface, fontSize = 16.sp)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun ArtistHeader(artist: ArtistGroup, colors: AppColors) {
    Column(Modifier.background(colors.surface)) {
        Spacer(Modifier.height(20.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(60.dp))
                    .background(coverColor(artist.name)),
                contentAlignment = Alignment.Center
            ) { ArtistGlyph(size = 70.dp, tint = Color(0xFFEEEEEE)) }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            artist.name,
            color = colors.onSurface,
            fontSize = 22.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "${artist.albums.size} 专辑 • ${artist.tracks.size} 歌曲 • ${formatDuration(artist.totalSeconds)}",
            color = colors.secondaryText,
            fontSize = 14.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun PlaylistHeader(count: Int, totalSeconds: Int, colors: AppColors) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TimerGlyph(size = 20.dp, tint = colors.secondaryText)
        Spacer(Modifier.width(14.dp))
        Text("$count 歌曲 • ${formatDuration(totalSeconds)}", color = colors.onSurface, fontSize = 15.sp)
    }
}

@Composable
fun SearchScreen(state: AppState, colors: AppColors, navigator: Navigator, actions: SongActions, onOpenPlayer: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val albums = state.albums().filter {
        query.isNotBlank() && (it.title.contains(query, true) || it.artist.contains(query, true))
    }
    val artists = state.artists().filter { query.isNotBlank() && it.name.contains(query, true) }
    val tracks = state.tracks.filter {
        query.isNotBlank() && (it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true))
    }
    Column(Modifier.fillMaxSize().background(colors.background)) {
        SimpleTopBar(title = "搜索", colors = colors, onBack = { navigator.pop() })
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SearchFieldGlyph(size = 22.dp, tint = colors.secondaryText)
            Spacer(Modifier.width(16.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) Text("正在扫描", color = colors.secondaryText, fontSize = 16.sp)
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    textStyle = androidx.compose.ui.text.TextStyle(color = colors.onSurface, fontSize = 16.sp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotEmpty()) {
                Box(Modifier.size(32.dp).clickable { query = "" }, contentAlignment = Alignment.Center) {
                    CloseGlyph(size = 18.dp, tint = colors.secondaryText)
                }
            }
        }
        if (query.isBlank()) {
            EmptyState("输入关键词开始搜索", colors)
            return
        }
        if (tracks.isEmpty() && albums.isEmpty() && artists.isEmpty()) {
            EmptyState("没有找到结果", colors)
            return
        }
        LazyColumn(Modifier.fillMaxSize()) {
            if (tracks.isNotEmpty()) {
                item { SectionHeader("歌曲", colors) }
                items(tracks, key = { "s_${it.id}" }) { track ->
                    SongListRow(track, state, colors, actions) {
                        state.playTrack(track.id, tracks.map { it.id })
                    }
                }
            }
            if (albums.isNotEmpty()) {
                item { SectionHeader("专辑", colors) }
                items(albums, key = { "a_${it.key}" }) { album ->
                    PlaylistRow(
                        name = album.title,
                        subtitle = "${album.artist} • ${album.tracks.size} 歌曲",
                        colors = colors,
                        icon = { AlbumGlyph(size = 26.dp, tint = colors.secondaryText) },
                        onClick = { navigator.push(Screen.Album(album.key)) },
                        onOverflow = null
                    )
                }
            }
            if (artists.isNotEmpty()) {
                item { SectionHeader("艺术家", colors) }
                items(artists, key = { "r_${it.name}" }) { artist ->
                    PlaylistRow(
                        name = artist.name,
                        subtitle = "${artist.albums.size} 专辑 • ${artist.tracks.size} 歌曲",
                        colors = colors,
                        icon = { ArtistGlyph(size = 26.dp, tint = colors.secondaryText) },
                        onClick = { navigator.push(Screen.Artist(artist.name)) },
                        onOverflow = null
                    )
                }
            }
        }
    }
}

data class FolderEntry(val name: String, val isDirectory: Boolean, val sizeLabel: String, val track: Track? = null)

@Composable
fun FoldersScreen(state: AppState, colors: AppColors, navigator: Navigator, actions: SongActions) {
    var path by remember { mutableStateOf(listOf("STORAGE", "EMULATED", "0", "MUSIC")) }
    val entries = remember(path, state.tracks.size) {
        if (path.lastOrNull() == "MUSIC") {
            state.tracks.map {
                FolderEntry(it.fileName, isDirectory = false, sizeLabel = formatSize(it.sizeBytes), track = it)
            } + FolderEntry("晨间通勤.m3u", false, "633 B")
        } else {
            listOf(
                FolderEntry("Alarms", true, ""),
                FolderEntry("Android", true, ""),
                FolderEntry("Audiobooks", true, ""),
                FolderEntry("DCIM", true, ""),
                FolderEntry("Documents", true, ""),
                FolderEntry("Download", true, ""),
                FolderEntry("Movies", true, ""),
                FolderEntry("Music", true, ""),
                FolderEntry("Notifications", true, ""),
                FolderEntry("Pictures", true, ""),
                FolderEntry("Podcasts", true, ""),
                FolderEntry("Ringtones", true, "")
            )
        }
    }
    Column(Modifier.fillMaxSize().background(colors.background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.primary)
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(48.dp).clickable { navigator.pop() }, contentAlignment = Alignment.Center) {
                BackGlyph(size = 22.dp, tint = Color.White)
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val crumbs = listOf("…OT") + path
                crumbs.forEachIndexed { index, crumb ->
                    val isLast = index == crumbs.lastIndex
                    Text(
                        crumb,
                        color = if (isLast) Color.White else Color.White.copy(alpha = 0.75f),
                        fontSize = 15.sp,
                        fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier
                            .clickable {
                                if (!isLast) path = path.take(index)
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                    if (!isLast) ChevronGlyph(size = 16.dp, tint = Color.White.copy(alpha = 0.75f))
                }
            }
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                FolderGlyph(size = 22.dp, tint = Color.White)
            }
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                MoreVertGlyph(size = 22.dp, tint = Color.White)
            }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(entries, key = { it.name }) { entry ->
                if (entry.isDirectory) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.surface)
                            .clickable { path = path + entry.name }
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FolderGlyph(size = 26.dp, tint = colors.secondaryText)
                        Spacer(Modifier.width(20.dp))
                        Text(entry.name, color = colors.onSurface, fontSize = 17.sp)
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.surface)
                            .clickable {
                                entry.track?.let { track ->
                                    state.playTrack(track.id, state.tracks.map { it.id })
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (entry.name.endsWith(".m3u")) {
                            PlaylistGlyph(size = 26.dp, tint = colors.secondaryText)
                        } else {
                            MusicNoteGlyph(size = 26.dp, tint = colors.secondaryText)
                        }
                        Spacer(Modifier.width(20.dp))
                        Column(Modifier.weight(1f)) {
                            Text(entry.name, color = colors.onSurface, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(entry.sizeLabel, color = colors.secondaryText, fontSize = 14.sp)
                        }
                        MoreVertGlyph(size = 22.dp, tint = colors.secondaryText)
                    }
                }
                DividerLine(colors, indent = 62.dp)
            }
        }
    }
}
