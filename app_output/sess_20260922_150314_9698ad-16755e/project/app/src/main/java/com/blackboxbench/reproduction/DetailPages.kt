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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BackBar(title: String, model: AppModel, actions: (@Composable () -> Unit)? = null) {
    Surface(shadowElevation = 2.dp, color = Color(0xFFF9F9FF)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            IconButton(onClick = { model.pop() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color(0xFF37474F))
            }
            Text(
                title, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color(0xFF263238),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            actions?.invoke()
        }
    }
}

@Composable
fun AlbumDetailPage(model: AppModel, album: String) {
    val songs = model.songs.filter { it.album == album }.sortedBy { it.trackNo }
    Column(Modifier.fillMaxSize()) {
        BackBar(album, model)
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CoverImage(album = album, size = 110)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(album, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121))
                        Spacer(Modifier.height(6.dp))
                        Text(songs.firstOrNull()?.artist ?: "", fontSize = 14.sp, color = Color(0xFF616161))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${songs.firstOrNull()?.year ?: ""} · ${songs.firstOrNull()?.genre ?: ""} · ${songs.size} 首歌",
                            fontSize = 13.sp, color = Color(0xFF9E9E9E),
                        )
                    }
                }
            }
            items(songs, key = { it.id }) { s ->
                SongRow(
                    title = s.title,
                    subtitle = "曲目 ${s.trackNo} · ${formatTime(s.duration)}",
                    playing = model.player.current()?.id == s.id && model.player.playing.value,
                    onClick = { model.player.playQueue(songs, songs.indexOf(s)) },
                )
            }
        }
    }
}

@Composable
fun ArtistDetailPage(model: AppModel, artist: String) {
    val songs = model.songs.filter { it.artist == artist }.sortedWith(compareBy({ it.album }, { it.trackNo }))
    Column(Modifier.fillMaxSize()) {
        BackBar(artist, model)
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Text(
                    "$artist · ${songs.distinctBy { it.album }.size} 张专辑 · ${songs.size} 首歌曲",
                    fontSize = 13.sp, color = Color(0xFF9E9E9E),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
            items(songs, key = { it.id }) { s ->
                SongRow(
                    title = s.title,
                    subtitle = "${s.album} · ${formatTime(s.duration)}",
                    playing = model.player.current()?.id == s.id && model.player.playing.value,
                    onClick = { model.player.playQueue(songs, songs.indexOf(s)) },
                )
            }
        }
    }
}

@Composable
fun GenreDetailPage(model: AppModel, genre: String) {
    val songs = model.songs.filter { it.genre == genre }.sortedBy { it.title }
    Column(Modifier.fillMaxSize()) {
        BackBar(genre, model)
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Text(
                    "$genre · ${songs.size} 首歌曲",
                    fontSize = 13.sp, color = Color(0xFF9E9E9E),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
            items(songs, key = { it.id }) { s ->
                SongRow(
                    title = s.title,
                    subtitle = "${s.artist} · ${s.album}",
                    playing = model.player.current()?.id == s.id && model.player.playing.value,
                    onClick = { model.player.playQueue(songs, songs.indexOf(s)) },
                )
            }
        }
    }
}

@Composable
fun PlaylistDetailPage(model: AppModel, playlistId: Long) {
    val playlist = model.playlists.value.firstOrNull { it.id == playlistId }
    if (playlist == null) {
        Column(Modifier.fillMaxSize()) { BackBar("播放列表", model) }
        return
    }
    val songs = playlist.songIds.mapNotNull { model.songById(it) }
    var menuOpen by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        BackBar(playlist.name, model, actions = {
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "菜单", tint = Color(0xFF37474F))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("添加歌曲") },
                        onClick = { menuOpen = false; model.push(Screen.SongPicker(playlistId)) },
                    )
                    DropdownMenuItem(
                        text = { Text("重命名") },
                        onClick = { menuOpen = false; showRename = true },
                    )
                    DropdownMenuItem(
                        text = { Text("删除播放列表") },
                        onClick = {
                            menuOpen = false
                            model.deletePlaylist(playlistId)
                            model.pop()
                        },
                    )
                }
            }
        })
        if (songs.isEmpty()) {
            EmptyState(main = "播放列表为空", sub = "通过右上角菜单添加歌曲")
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                item {
                    Text(
                        "${playlist.name} · ${songs.size} 首歌曲",
                        fontSize = 13.sp, color = Color(0xFF9E9E9E),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
                items(songs, key = { it.id }) { s ->
                    SongRow(
                        title = s.title,
                        subtitle = "${s.artist} · ${formatTime(s.duration)}",
                        playing = model.player.current()?.id == s.id && model.player.playing.value,
                        onClick = { model.player.playQueue(songs, songs.indexOf(s)) },
                    )
                }
            }
        }
    }
    if (showRename) {
        NewPlaylistDialog(
            initial = playlist.name,
            title = "重命名播放列表",
            confirmLabel = "保存",
            onDismiss = { showRename = false },
            onConfirm = { name -> showRename = false; model.renamePlaylist(playlistId, name) },
        )
    }
}

@Composable
fun SongPickerPage(model: AppModel, playlistId: Long) {
    val songs = model.songs.sortedBy { it.title }
    val playlist = model.playlists.value.firstOrNull { it.id == playlistId }
    val selected = remember { mutableStateOf(playlist?.songIds?.toSet() ?: emptySet()) }
    var query by remember { mutableStateOf("") }
    val visible = if (query.isBlank()) songs else songs.filter {
        it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true)
    }

    Column(Modifier.fillMaxSize()) {
        Surface(shadowElevation = 2.dp, color = Color(0xFFF9F9FF)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                IconButton(onClick = { model.pop() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color(0xFF37474F))
                }
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    placeholder = { Text("搜索歌曲") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = {
                    model.addToPlaylist(playlistId, selected.value.toList())
                    model.pop()
                }) { Text("完成", color = MaterialTheme.colorScheme.primary) }
            }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(visible, key = { it.id }) { s ->
                val checked = selected.value.contains(s.id)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                        .clickable {
                            selected.value = if (checked) selected.value - s.id else selected.value + s.id
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(s.title, fontSize = 16.sp, color = Color(0xFF212121))
                        Spacer(Modifier.height(2.dp))
                        Text("${s.artist} · ${s.album}", fontSize = 13.sp, color = Color(0xFF757575))
                    }
                    Icon(
                        androidx.compose.ui.res.painterResource(
                            if (checked) R.drawable.ic_check_circle else R.drawable.ic_circle_outline
                        ),
                        contentDescription = if (checked) "已选" else "未选",
                        tint = if (checked) MaterialTheme.colorScheme.primary else Color(0xFFBDBDBD),
                    )
                }
            }
        }
    }
}

@Composable
fun SearchPage(model: AppModel) {
    var query by remember { mutableStateOf("") }
    val songs = model.songs
    val results = if (query.isBlank()) emptyList() else songs.filter {
        it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true)
    }
    Column(Modifier.fillMaxSize()) {
        Surface(shadowElevation = 2.dp, color = Color(0xFFF9F9FF)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                IconButton(onClick = { model.pop() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color(0xFF37474F))
                }
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    placeholder = { Text("搜索歌曲、艺术家或专辑") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                )
            }
        }
        if (query.isBlank()) {
            EmptyState(main = "搜索音乐库", sub = "输入关键词查找歌曲、艺术家或专辑")
        } else if (results.isEmpty()) {
            EmptyState(main = "没有找到匹配项", sub = "换个关键词试试")
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                item {
                    Text(
                        "找到 ${results.size} 首",
                        fontSize = 13.sp, color = Color(0xFF9E9E9E),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
                items(results, key = { it.id }) { s ->
                    SongRow(
                        title = s.title,
                        subtitle = "${s.artist} · ${s.album}",
                        playing = model.player.current()?.id == s.id && model.player.playing.value,
                        onClick = { model.player.playQueue(results, results.indexOf(s)) },
                    )
                }
            }
        }
    }
}
