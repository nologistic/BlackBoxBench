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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NowPlayingPage(model: AppModel) {
    val player = model.player
    val song = player.current()
    if (song == null) {
        Column(Modifier.fillMaxSize()) {
            BackBar("正在播放", model)
            EmptyState(main = "没有正在播放的歌曲", sub = "从曲库中选择一首开始播放")
        }
        return
    }
    var menuOpen by remember { mutableStateOf(false) }
    var showTagEditor by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }
    var showAddToPlaylist by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Color(0xFFEFF2F1))) {
        // 顶栏：返回 + 菜单
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            IconButton(onClick = { model.pop() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color(0xFF37474F))
            }
            Spacer(Modifier.weight(1f))
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "菜单", tint = Color(0xFF37474F))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("编辑音乐信息") },
                        leadingIcon = { Icon(Icons.Filled.Edit, null) },
                        onClick = { menuOpen = false; showTagEditor = true },
                    )
                    DropdownMenuItem(
                        text = { Text("详情") },
                        leadingIcon = { Icon(Icons.Filled.Info, null) },
                        onClick = { menuOpen = false; showDetails = true },
                    )
                    DropdownMenuItem(
                        text = { Text("分享") },
                        leadingIcon = { Icon(Icons.Filled.Share, null) },
                        onClick = { menuOpen = false },
                    )
                    DropdownMenuItem(
                        text = { Text("从曲库删除") },
                        leadingIcon = { Icon(Icons.Filled.Delete, null) },
                        onClick = { menuOpen = false },
                    )
                    DropdownMenuItem(
                        text = { Text("加入播放列表") },
                        leadingIcon = { Icon(Icons.Filled.Add, null) },
                        onClick = { menuOpen = false; showAddToPlaylist = true },
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
        ) {
            Spacer(Modifier.height(12.dp))
            VinylDisc(album = song.album, sizeDp = 280, spinning = player.playing.value)
            Spacer(Modifier.height(28.dp))
            Text(
                song.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121),
                textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(song.artist, fontSize = 15.sp, color = Color(0xFF757575))
            Spacer(Modifier.height(20.dp))

            // 进度条
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            ) {
                Text(
                    formatTime(player.positionSec.intValue), fontSize = 12.sp, color = Color(0xFF9E9E9E),
                    modifier = Modifier.width(44.dp),
                )
                Slider(
                    value = player.positionSec.intValue.toFloat(),
                    onValueChange = { player.seekTo(it.toInt()) },
                    valueRange = 0f..song.duration.toFloat(),
                    modifier = Modifier.weight(1f).height(20.dp),
                )
                Text(
                    formatTime(song.duration), fontSize = 12.sp, color = Color(0xFF9E9E9E),
                    textAlign = TextAlign.End, modifier = Modifier.width(44.dp),
                )
            }

            Spacer(Modifier.height(8.dp))
            // 主控制行：随机 / 上一首 / 播放暂停 / 下一首 / 循环
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                IconButton(onClick = { player.toggleShuffle() }, modifier = Modifier.size(56.dp)) {
                    Icon(
                        painterResource(R.drawable.ic_shuffle), contentDescription = "随机播放",
                        tint = if (player.shuffle.value) MaterialTheme.colorScheme.primary else Color(0xFF90A4AE),
                    )
                }
                IconButton(onClick = { player.previous() }, modifier = Modifier.size(64.dp)) {
                    Icon(painterResource(R.drawable.ic_skip_previous), contentDescription = "上一首", tint = Color(0xFF37474F))
                }
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(76.dp),
                ) {
                    IconButton(onClick = { player.togglePlayPause() }, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            painterResource(
                                if (player.playing.value) R.drawable.ic_pause else R.drawable.ic_play_arrow
                            ),
                            contentDescription = "播放或暂停",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }
                IconButton(onClick = { player.next() }, modifier = Modifier.size(64.dp)) {
                    Icon(painterResource(R.drawable.ic_skip_next), contentDescription = "下一首", tint = Color(0xFF37474F))
                }
                IconButton(onClick = { player.cycleRepeat() }, modifier = Modifier.size(56.dp)) {
                    Icon(
                        painterResource(R.drawable.ic_repeat), contentDescription = "循环模式",
                        tint = if (player.repeatMode.intValue > 0) MaterialTheme.colorScheme.primary else Color(0xFF90A4AE),
                    )
                }
            }
            if (player.repeatMode.intValue == 2) {
                Text("单曲循环", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            } else if (player.repeatMode.intValue == 1) {
                Text("全部循环", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(12.dp))
            // 底部功能行：收藏 / 均衡器 / 队列
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                IconButton(onClick = { model.toggleFavorite(song.id) }, modifier = Modifier.size(48.dp)) {
                    Icon(
                        if (model.favorites.value.contains(song.id)) Icons.Filled.Favorite
                        else Icons.Filled.FavoriteBorder,
                        contentDescription = "收藏",
                        tint = if (model.favorites.value.contains(song.id)) Color(0xFFE53935) else Color(0xFF90A4AE),
                    )
                }
                IconButton(onClick = { model.push(Screen.Equalizer) }, modifier = Modifier.size(48.dp)) {
                    Icon(painterResource(R.drawable.ic_equalizer), contentDescription = "均衡器", tint = Color(0xFF90A4AE))
                }
                IconButton(onClick = { showQueue = true }, modifier = Modifier.size(48.dp)) {
                    Icon(painterResource(R.drawable.ic_queue_music), contentDescription = "播放队列", tint = Color(0xFF90A4AE))
                }
            }
        }
    }

    if (showTagEditor) {
        TagEditorDialog(model, song) { showTagEditor = false }
    }
    if (showDetails) {
        SongDetailsDialog(song) { showDetails = false }
    }
    if (showAddToPlaylist) {
        AddToPlaylistDialog(model, song.id) { showAddToPlaylist = false }
    }
    if (showQueue) {
        QueuePanel(model) { showQueue = false }
    }
}

@Composable
fun TagEditorDialog(model: AppModel, song: Song, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(song.title) }
    var artist by remember { mutableStateOf(song.artist) }
    var album by remember { mutableStateOf(song.album) }
    var year by remember { mutableStateOf(song.year.toString()) }
    var genre by remember { mutableStateOf(song.genre) }
    var track by remember { mutableStateOf(song.trackNo.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑音乐信息") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                LabeledField("标题", title) { title = it }
                LabeledField("艺术家", artist) { artist = it }
                LabeledField("专辑", album) { album = it }
                LabeledField("年份", year) { year = it }
                LabeledField("流派", genre) { genre = it }
                LabeledField("音轨号", track) { track = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                model.saveTag(
                    song.id, title, artist, album,
                    year.toIntOrNull() ?: song.year,
                    genre, track.toIntOrNull() ?: song.trackNo,
                )
                onDismiss()
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun LabeledField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text(label) }, singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
}

@Composable
private fun SongDetailsDialog(song: Song, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("歌曲详情") },
        text = {
            Column {
                DetailLine("标题", song.title)
                DetailLine("艺术家", song.artist)
                DetailLine("专辑", song.album)
                DetailLine("音轨", "#${song.trackNo}")
                DetailLine("年份", song.year.toString())
                DetailLine("流派", song.genre)
                DetailLine("时长", formatTime(song.duration))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("确定") } },
    )
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, fontSize = 14.sp, color = Color(0xFF9E9E9E), modifier = Modifier.width(64.dp))
        Text(value, fontSize = 14.sp, color = Color(0xFF212121))
    }
}

@Composable
fun AddToPlaylistDialog(model: AppModel, songId: Long, onDismiss: () -> Unit) {
    var showCreate by remember { mutableStateOf(false) }
    if (showCreate) {
        NewPlaylistDialog(
            onDismiss = { showCreate = false },
            onConfirm = { name ->
                showCreate = false
                val id = model.createPlaylist(name)
                model.addToPlaylist(id, listOf(songId))
                onDismiss()
            },
        )
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("加入播放列表") },
        text = {
            Column {
                val playlists = model.playlists.value.sortedBy { it.name }
                if (playlists.isEmpty()) {
                    Text("还没有播放列表", color = Color(0xFF757575))
                }
                playlists.forEach { p ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable {
                            model.addToPlaylist(p.id, listOf(songId))
                            onDismiss()
                        }.padding(vertical = 10.dp),
                    ) {
                        Icon(painterResource(R.drawable.ic_queue_music), null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text(p.name, fontSize = 16.sp, color = Color(0xFF212121))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { showCreate = true }) { Text("新建播放列表") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
fun NewPlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    title: String = "新建播放列表",
    initial: String = "",
    confirmLabel: String = "新建",
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("名称") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }) {
                Text(confirmLabel, color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun QueuePanel(model: AppModel, onDismiss: () -> Unit) {
    val player = model.player
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("播放队列 · ${player.queue.size} 首") },
        text = {
            LazyColumn(Modifier.height(360.dp)) {
                items(player.queue.size) { i ->
                    val s = player.queue[i]
                    val current = i == player.queueIndex.intValue
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable {
                            player.queueIndex.intValue = i
                            player.seekTo(0)
                            player.resume()
                        }.padding(vertical = 8.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                s.title, fontSize = 15.sp,
                                color = if (current) MaterialTheme.colorScheme.primary else Color(0xFF212121),
                                fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                            )
                            Text(s.artist, fontSize = 12.sp, color = Color(0xFF9E9E9E))
                        }
                        if (current) {
                            Icon(painterResource(R.drawable.ic_play_arrow), "正在播放", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}
