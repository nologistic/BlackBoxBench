package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun ListMenuDialog(items: List<Pair<String, Color?>>, onItem: (Int) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(4.dp), color = Color.White, shadowElevation = 8.dp) {
            Column(Modifier.padding(vertical = 8.dp).widthIn(min = 260.dp).verticalScroll(rememberScrollState())) {
                items.forEachIndexed { i, (text, color) ->
                    Box(
                        Modifier.fillMaxWidth().clickable { onItem(i); onDismiss() }
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) { Text(text, fontSize = 15.sp, color = color ?: Color(0xFF212121)) }
                }
            }
        }
    }
}

// 歌曲 ⋮ 上下文菜单（歌曲/专辑详情/队列/文件夹通用）
@Composable
fun SongMenuDialog(
    song: Song, store: Store, nav: NavStack,
    inPlaylist: Boolean = false, isFile: Boolean = false,
    onAction: (String) -> Unit = {}, onDismiss: () -> Unit
) {
    val items = buildList<Pair<String, Color?>> {
        if (inPlaylist) add("从播放列表中移除" to null)
        add("作为下一首播放" to null)
        add("加入播放队列" to null)
        add("加入播放列表…" to null)
        add("查看专辑" to null)
        add("查看艺术家" to null)
        add("分享" to null)
        add("音乐标签编辑器" to null)
        add("详情" to null)
        if (isFile) add("扫描" to null)
        add("设为铃声" to null)
        add("从设备中删除" to Color(0xFFD32F2F))
    }
    ListMenuDialog(items, onItem = { i ->
        when (items[i].first) {
            "从播放列表中移除" -> onAction("removeFromPlaylist")
            "作为下一首播放" -> Player.playNext(song)
            "加入播放队列" -> Player.enqueue(song)
            "加入播放列表…" -> onAction("addToPlaylist")
            "查看专辑" -> if (song.album.isNotEmpty()) nav.push("album:${song.album}")
            "查看艺术家" -> if (Library.artists.contains(song.artist)) nav.push("artist:${song.artist}")
            "分享" -> onAction("share")
            "音乐标签编辑器" -> nav.push("tagEditor:${song.id}")
            "详情" -> onAction("details")
            "扫描" -> onAction("toast:已扫描 1 个文件")
            "设为铃声" -> onAction("toast:已将 ${song.title} 设为铃声")
            "从设备中删除" -> onAction("delete")
        }
    }, onDismiss = onDismiss)
}

@Composable
fun SongDetailsDialog(song: Song, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("确定", color = Pink) } },
        title = { Text(song.title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                val rows = buildList {
                    add("标题" to song.title)
                    add("艺术家" to song.artist)
                    if (song.album.isNotEmpty()) add("专辑" to song.album)
                    if (song.genre.isNotEmpty()) add("流派" to song.genre)
                    if (song.year > 0) add("年份" to song.year.toString())
                    if (song.track > 0) add("音轨号" to song.track.toString())
                    add("时长" to fmtDuration(song.duration))
                    add("格式" to song.format)
                    add("大小" to song.fileSize)
                    add("比特率" to song.bitrate)
                    add("采样率" to song.sampleRate)
                    add("路径" to "/storage/emulated/0/" +
                        (if (song.greenCover) "Download/BlackBoxBench/" else "Music/") + song.fileName)
                }
                rows.forEach { (k, v) ->
                    Column(Modifier.padding(vertical = 4.dp)) {
                        Text(k, fontSize = 12.sp, color = Color.Gray)
                        Text(v, fontSize = 14.sp)
                    }
                }
            }
        }
    )
}

@Composable
fun AddToPlaylistDialog(song: Song, store: Store, onNewPlaylist: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(4.dp), color = Color.White) {
            Column(Modifier.padding(top = 16.dp).widthIn(min = 280.dp)) {
                Text("加入播放列表", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp))
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth().clickable { onNewPlaylist(); onDismiss() }
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) { Text("＋  新建播放列表…", fontSize = 15.sp, color = Pink) }
                Row(
                    Modifier.fillMaxWidth().clickable {
                        if (!store.favorites.contains(song.id)) store.toggleFavorite(song.id)
                        onDismiss()
                    }.padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) { Text("♥  收藏夹", fontSize = 15.sp) }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// 新建播放列表（目标行为：创建静默失败，对话框关闭后列表不出现）
@Composable
fun NewPlaylistDialog(onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建播放列表") },
        text = {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                placeholder = { Text("播放列表名称") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("创建", color = Pink) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = Pink) } }
    )
}

@Composable
fun SaveQueueDialog(onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("保存播放队列") },
        text = {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                placeholder = { Text("播放列表名称") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("保存", color = Pink) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = Pink) } }
    )
}

@Composable
fun RenamePlaylistDialog(current: String, onRename: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重命名播放列表") },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it },
                placeholder = { Text("播放列表名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = { TextButton(onClick = { onRename(name); onDismiss() }) { Text("重命名", color = Pink) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = Pink) } }
    )
}

@Composable
fun DeleteSongDialog(song: Song, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("从设备中删除") },
        text = { Text("确定要从设备中删除 ${song.title} 吗？此操作无法撤销。") },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text("删除", color = Color(0xFFD32F2F)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = Pink) } }
    )
}

@Composable
fun RescanDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重新扫描媒体库") },
        text = {
            Text(
                "This will clear and rebuild the app internal song database.\n\n" +
                    "No song will be deleted during this operation, but this might clear the play history.\n\n" +
                    "During a possible long duration, until the rescan is complete, you will not be able to use the app normally"
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text("重新扫描媒体库", color = Pink) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = Pink) } }
    )
}
