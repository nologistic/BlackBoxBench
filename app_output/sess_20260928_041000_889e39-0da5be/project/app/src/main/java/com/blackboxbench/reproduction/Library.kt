package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LibraryContent(
    model: AppModel,
    onSongClick: (Song, List<Long>) -> Unit,
    onSongMore: (Song) -> Unit,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onGenreClick: (String) -> Unit,
    onSmartClick: (SmartKind) -> Unit,
    onPlaylistClick: (Long) -> Unit,
    onPlaylistMore: (Long) -> Unit,
    onShuffleAll: () -> Unit
) {
    when (model.tab) {
        LibraryTab.SONGS -> SongsTab(model, onSongClick, onSongMore, onShuffleAll)
        LibraryTab.ALBUMS -> AlbumsTab(model, onAlbumClick)
        LibraryTab.ARTISTS -> ArtistsTab(model, onArtistClick)
        LibraryTab.GENRES -> GenresTab(model, onGenreClick)
        LibraryTab.PLAYLISTS -> PlaylistsTab(model, onSmartClick, onPlaylistClick, onPlaylistMore)
    }
}

@Composable
private fun ShuffleAllRow(onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painterResource(R.drawable.ic_shuffle),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text("随机播放所有歌曲", color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
    }
}

@Composable
private fun SongsTab(model: AppModel, onSongClick: (Song, List<Long>) -> Unit, onSongMore: (Song) -> Unit, onShuffleAll: () -> Unit) {
    val ids = model.repo.songs.map { it.id }
    LazyColumn(Modifier.fillMaxSize()) {
        item { ShuffleAllRow(onShuffleAll) }
        items(model.repo.songs, key = { it.id }) { song ->
            SongRow(song, onClick = { onSongClick(song, ids) }, onMore = { onSongMore(song) })
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun AlbumsTab(model: AppModel, onAlbumClick: (String) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp)
    ) {
        items(model.repo.albums, key = { it.title }) { album ->
            Column(
                Modifier
                    .padding(8.dp)
                    .clickable { onAlbumClick(album.title) }
            ) {
                VinylCover(
                    album = album.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    cornerRadius = 4.dp
                )
                Spacer(Modifier.height(8.dp))
                Text(album.title, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color(0xFF212121))
                Text(
                    "${album.songs.size} 歌曲",
                    fontSize = 13.sp,
                    color = VinylColors.SecondaryText
                )
            }
        }
    }
}

@Composable
private fun ArtistsTab(model: AppModel, onArtistClick: (String) -> Unit) {
    if (model.repo.artists.isEmpty()) {
        EmptyState("没有艺术家")
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(model.repo.artists, key = { it.name }) { artist ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onArtistClick(artist.name) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(albumColor(artist.name)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        artist.name.take(1),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(artist.name, fontSize = 16.sp, color = Color(0xFF212121))
                    Text("${artist.albums.size} 专辑", fontSize = 13.sp, color = VinylColors.SecondaryText)
                }
            }
        }
    }
}

@Composable
private fun GenresTab(model: AppModel, onGenreClick: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(model.repo.genres, key = { it.name }) { genre ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable { onGenreClick(genre.name) }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(genre.name, fontSize = 16.sp, color = Color(0xFF212121))
                Text("${genre.songs.size} 歌曲", fontSize = 13.sp, color = VinylColors.SecondaryText)
            }
        }
    }
}

private fun smartIcon(kind: SmartKind): Int = when (kind) {
    SmartKind.RECENTLY_ADDED -> R.drawable.ic_add_box
    SmartKind.PLAY_HISTORY -> R.drawable.ic_clock
    SmartKind.RECENTLY_NOT_PLAYED -> R.drawable.ic_clock
    SmartKind.MOST_PLAYED -> R.drawable.ic_trending_up
}

@Composable
private fun PlaylistsTab(
    model: AppModel,
    onSmartClick: (SmartKind) -> Unit,
    onPlaylistClick: (Long) -> Unit,
    onPlaylistMore: (Long) -> Unit
) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(SmartKind.entries) { kind ->
            val count = model.repo.smartPlaylist(kind).songs.size
            PlaylistRow(
                iconRes = smartIcon(kind),
                title = kind.label,
                subtitle = "本月 · $count 歌曲",
                onClick = { onSmartClick(kind) },
                onMore = null
            )
        }
        item {
            Divider(color = VinylColors.Divider, thickness = 1.dp)
            Spacer(Modifier.height(4.dp))
        }
        items(model.repo.playlists, key = { it.id }) { playlist ->
            PlaylistRow(
                iconRes = R.drawable.ic_queue_music,
                title = playlist.name,
                subtitle = "${playlist.songIds.size} 歌曲",
                onClick = { onPlaylistClick(playlist.id) },
                onMore = { onPlaylistMore(playlist.id) }
            )
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun PlaylistRow(
    iconRes: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    onMore: (() -> Unit)?
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painterResource(iconRes),
            contentDescription = null,
            tint = Color(0xFF616161),
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(20.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, color = Color(0xFF212121))
            Text(subtitle, fontSize = 13.sp, color = VinylColors.SecondaryText)
        }
        if (onMore != null) {
            IconButton(onClick = onMore) {
                Icon(Icons.Filled.MoreVert, contentDescription = "更多", tint = Color(0xFF616161))
            }
        }
    }
}

// ---------------------------------------------------------------- details

@Composable
fun SongList(
    songs: List<Song>,
    onSongClick: (Song, List<Long>) -> Unit,
    onSongMore: (Song) -> Unit,
    showIndex: Boolean = false
) {
    val ids = songs.map { it.id }
    LazyColumn(Modifier.fillMaxSize()) {
        items(songs, key = { it.id }) { song ->
            SongRow(
                song,
                onClick = { onSongClick(song, ids) },
                onMore = { onSongMore(song) },
                showIndex = if (showIndex) song.trackNo else null
            )
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
fun AlbumDetailContent(model: AppModel, albumTitle: String, onSongClick: (Song, List<Long>) -> Unit, onSongMore: (Song) -> Unit) {
    val album = model.repo.albums.firstOrNull { it.title == albumTitle } ?: return
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            VinylCover(album.title, Modifier.size(120.dp), cornerRadius = 4.dp)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(album.title, fontSize = 20.sp, fontWeight = FontWeight.Medium, color = Color(0xFF212121))
                Spacer(Modifier.height(6.dp))
                Text(album.artist, fontSize = 14.sp, color = VinylColors.SecondaryText)
                Text("${album.songs.size} 歌曲 · ${formatDuration(album.durationSec)}", fontSize = 13.sp, color = VinylColors.SecondaryText)
            }
        }
        SongList(album.songs, onSongClick, onSongMore, showIndex = true)
    }
}

@Composable
fun ArtistDetailContent(model: AppModel, artistName: String, onAlbumClick: (String) -> Unit, onSongClick: (Song, List<Long>) -> Unit, onSongMore: (Song) -> Unit) {
    val artist = model.repo.artists.firstOrNull { it.name == artistName } ?: return
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Column(Modifier.padding(16.dp)) {
                Box(
                    Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(albumColor(artist.name)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(artist.name.take(1), color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                Text(artist.name, fontSize = 20.sp, fontWeight = FontWeight.Medium, color = Color(0xFF212121))
                Text("${artist.albums.size} 专辑 · ${artist.albums.sumOf { it.songs.size }} 歌曲", fontSize = 13.sp, color = VinylColors.SecondaryText)
            }
        }
        items(artist.albums, key = { it.title }) { album ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onAlbumClick(album.title) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                VinylCover(album.title, Modifier.size(52.dp), cornerRadius = 2.dp)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(album.title, fontSize = 16.sp, color = Color(0xFF212121))
                    Text("${album.year} · ${album.songs.size} 歌曲", fontSize = 13.sp, color = VinylColors.SecondaryText)
                }
            }
        }
    }
}

@Composable
fun GenreDetailContent(model: AppModel, genreName: String, onSongClick: (Song, List<Long>) -> Unit, onSongMore: (Song) -> Unit) {
    val genre = model.repo.genres.firstOrNull { it.name == genreName } ?: return
    SongList(genre.songs, onSongClick, onSongMore)
}

@Composable
fun PlaylistDetailContent(model: AppModel, playlistId: Long, onSongClick: (Song, List<Long>) -> Unit, onSongMore: (Song) -> Unit) {
    val playlist = model.repo.playlistById(playlistId) ?: return
    val songs = model.repo.songsByIds(playlist.songIds)
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(painterResource(R.drawable.ic_clock), contentDescription = null, tint = VinylColors.SecondaryText)
            Spacer(Modifier.width(12.dp))
            Text(
                "${songs.size} 歌曲 · ${formatDuration(songs.sumOf { it.durationSec })}",
                color = VinylColors.SecondaryText,
                fontSize = 14.sp
            )
        }
        SongList(songs, onSongClick, onSongMore)
    }
}

@Composable
fun SmartDetailContent(model: AppModel, kind: SmartKind, onSongClick: (Song, List<Long>) -> Unit, onSongMore: (Song) -> Unit) {
    val songs = model.repo.smartPlaylist(kind).songs
    if (songs.isEmpty()) {
        EmptyState("没有歌曲")
    } else {
        SongList(songs, onSongClick, onSongMore)
    }
}
