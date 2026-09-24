package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.atan2
import kotlin.math.roundToInt

@Composable
fun NowPlayingScreen(store: Store, nav: NavStack) {
    val cur = Player.current()
    var menuOpen by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var showSaveQueue by remember { mutableStateOf(false) }
    var menuSong by remember { mutableStateOf<Pair<Song, Int>?>(null) }
    var addToPlaylistSong by remember { mutableStateOf<Song?>(null) }
    var detailsSong by remember { mutableStateOf<Song?>(null) }

    Column(Modifier.fillMaxSize().background(DarkBg)) {
        // 顶部栏
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().height(56.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { nav.pop() }) { Icon(Icons.Filled.Close, null, tint = Color.White) }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { cur?.let { store.toggleFavorite(it.id) } }) {
                val fav = cur != null && store.favorites.contains(cur.id)
                Icon(
                    if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    null, tint = Color.White
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, null, tint = Color.White) }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("清空播放队列") }, onClick = { menuOpen = false; Player.clear(); nav.pop() })
                    DropdownMenuItem(text = { Text("保存播放队列") }, onClick = { menuOpen = false; showSaveQueue = true })
                    DropdownMenuItem(text = { Text("睡眠定时器") }, onClick = { menuOpen = false; showSleepTimer = true })
                    DropdownMenuItem(text = { Text("均衡器") }, onClick = { menuOpen = false })
                }
            }
        }

        // 黑胶
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            VinylDisc(rotation = Player.rotation, modifier = Modifier.size(280.dp))
        }

        // 进度条
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(fmtDuration(Player.progress), color = Color.White, fontSize = 12.sp)
            Box(
                Modifier.weight(1f).height(24.dp).padding(horizontal = 8.dp)
                    .pointerInput(cur?.id) {
                        detectTapGestures { off ->
                            cur?.let { Player.progress = (it.duration * (off.x / size.width)).toInt(); Player.persist() }
                        }
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Box(Modifier.fillMaxWidth().height(2.dp).background(Color(0x66FFFFFF)))
                val f = if ((cur?.duration ?: 0) > 0) Player.progress.toFloat() / cur!!.duration else 0f
                Box(Modifier.fillMaxWidth(f.coerceIn(0f, 1f)).height(2.dp).background(Color.White))
            }
            Text(fmtDuration(cur?.duration ?: 0), color = Color.White, fontSize = 12.sp)
        }

        // 控制区 + 队列（浅色卡片）
        Column(
            Modifier.fillMaxWidth().background(SheetGray, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
        ) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { Player.cycleRepeat() }) {
                    Box {
                        RepeatGlyph(Modifier.size(24.dp), if (Player.repeatMode > 0) Pink else Color(0xFF616161))
                        if (Player.repeatMode == 2) {
                            Text("1", fontSize = 9.sp, color = Pink, modifier = Modifier.align(Alignment.TopEnd))
                        }
                    }
                }
                IconButton(onClick = { Player.prev() }) { SkipGlyph(Modifier.size(28.dp), Color(0xFF424242), next = false) }
                Box(
                    Modifier.size(56.dp).background(Color.White, CircleShape).clickable { Player.toggle() },
                    contentAlignment = Alignment.Center
                ) {
                    PlayGlyph(Modifier.size(30.dp), Color(0xFF212121), Player.playing)
                }
                IconButton(onClick = { Player.next() }) { SkipGlyph(Modifier.size(28.dp), Color(0xFF424242), next = true) }
                IconButton(onClick = { Player.toggleShuffle() }) {
                    ShuffleGlyph(Modifier.size(24.dp), if (Player.shuffle) Pink else Color(0xFF616161))
                }
            }

            // 当前歌曲卡
            Surface(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                shape = RoundedCornerShape(8.dp), color = Color.White, shadowElevation = 2.dp
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Canvas(Modifier.size(32.dp)) {
                        val r = size.minDimension / 2
                        drawCircle(VinylGray, r)
                        drawCircle(Color(0xFF6A6A6A), r * 0.6f, center, style = Stroke(2f))
                        drawCircle(Color(0xFF3A3A3A), r * 0.25f)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(cur?.title ?: "", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            if (cur != null && cur.album.isNotEmpty()) "${cur.artist} · ${cur.album}" else cur?.artist ?: "",
                            fontSize = 13.sp, color = Color.Gray
                        )
                    }
                    IconButton(onClick = { cur?.let { s -> menuSong = s to Player.pos } }) {
                        Icon(Icons.Filled.MoreVert, null, tint = Color.Gray)
                    }
                }
            }

            // 队列
            val qs = Player.songs()
            val total = qs.sumOf { it.duration }
            Text(
                "即将播放 · ${fmtDuration(total)} · ${Player.pos + 1}/${qs.size}",
                fontSize = 13.sp, color = Color.Gray,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            var dragFrom by remember { mutableStateOf(-1) }
            var dragOffset by remember { mutableStateOf(0f) }
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                itemsIndexed(qs, key = { i, s -> "${s.id}-$i" }) { i, song ->
                    Row(
                        Modifier.fillMaxWidth().height(60.dp)
                            .background(if (i == dragFrom) Color(0x11000000) else Color.Transparent)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "$i", fontSize = 14.sp, color = Color.Gray,
                            modifier = Modifier.width(28.dp).pointerInput(i, qs.size) {
                                detectDragGestures(
                                    onDragStart = { dragFrom = i; dragOffset = 0f },
                                    onDrag = { _, d -> dragOffset += d.y },
                                    onDragEnd = {
                                        val rowH = 60.dp.toPx()
                                        val to = (dragFrom + (dragOffset / rowH).roundToInt()).coerceIn(0, qs.size - 1)
                                        Player.move(dragFrom, to)
                                        dragFrom = -1; dragOffset = 0f
                                    },
                                    onDragCancel = { dragFrom = -1; dragOffset = 0f }
                                )
                            }
                        )
                        Column(Modifier.weight(1f)) {
                            Text(song.title, fontSize = 15.sp)
                            Text(
                                if (song.album.isNotEmpty()) "${song.artist} · ${song.album}" else song.artist,
                                fontSize = 12.sp, color = Color.Gray
                            )
                        }
                        IconButton(onClick = { menuSong = song to i }) {
                            Icon(Icons.Filled.MoreVert, null, tint = Color.Gray)
                        }
                    }
                    HorizontalDivider(color = Color(0xFFE0E0E0))
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    menuSong?.let { (song, idx) ->
        SongMenuDialog(song, store, nav, inPlaylist = true, onAction = { act ->
            when (act) {
                "removeFromPlaylist" -> Player.removeAt(idx)
                "addToPlaylist" -> addToPlaylistSong = song
                "details" -> detailsSong = song
            }
        }, onDismiss = { menuSong = null })
    }
    addToPlaylistSong?.let { song ->
        var showNew by remember { mutableStateOf(false) }
        if (showNew) NewPlaylistDialog { showNew = false }
        else AddToPlaylistDialog(song, store, onNewPlaylist = { showNew = true }) { addToPlaylistSong = null }
    }
    detailsSong?.let { SongDetailsDialog(it) { detailsSong = null } }
    if (showSaveQueue) SaveQueueDialog { showSaveQueue = false }
    if (showSleepTimer) SleepTimerDialog { showSleepTimer = false }
}

// ---------- 睡眠定时器（圆弧滑块） ----------

@Composable
fun SleepTimerDialog(onDismiss: () -> Unit) {
    var minutes by remember { mutableStateOf(if (Player.sleepMinutes > 0) Player.sleepMinutes else 30) }
    var finishCurrent by remember { mutableStateOf(Player.sleepFinishCurrent) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(8.dp), color = Color.White) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("睡眠定时器", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Box(
                    Modifier.size(200.dp).pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            val cx = size.width / 2f; val cy = size.height / 2f
                            val ang = Math.toDegrees(atan2(change.position.y - cy, change.position.x - cx).toDouble())
                            val frac = ((ang + 90 + 360) % 360) / 360.0
                            minutes = (frac * 120).roundToInt().coerceIn(0, 120)
                        }
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val r = size.minDimension / 2 - 16.dp.toPx()
                        drawCircle(Color(0xFFE0E0E0), r, center, style = Stroke(10f))
                        drawArc(
                            Pink, startAngle = -90f, sweepAngle = minutes / 120f * 360f,
                            useCenter = false, style = Stroke(10f),
                            topLeft = Offset(center.x - r, center.y - r),
                            size = androidx.compose.ui.geometry.Size(r * 2, r * 2)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$minutes", fontSize = 40.sp, fontWeight = FontWeight.Bold)
                        Text("分钟", fontSize = 13.sp, color = Color.Gray)
                    }
                }
                Row(
                    Modifier.fillMaxWidth().clickable { finishCurrent = !finishCurrent },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = finishCurrent, onCheckedChange = { finishCurrent = it },
                        colors = CheckboxDefaults.colors(checkedColor = Pink))
                    Text("播完当前音乐", fontSize = 14.sp)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消", color = Pink) }
                    TextButton(onClick = {
                        Player.sleepMinutes = minutes
                        Player.sleepFinishCurrent = finishCurrent
                        Player.persist()
                        onDismiss()
                    }) { Text("设置", color = Pink) }
                }
            }
        }
    }
}
