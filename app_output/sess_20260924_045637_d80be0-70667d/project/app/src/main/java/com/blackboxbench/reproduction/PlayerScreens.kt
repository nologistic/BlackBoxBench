package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------------------
// mini player
// ---------------------------------------------------------------------------

@Composable
fun MiniPlayer(app: AppState, onOpen: () -> Unit) {
    val colors = LocalViColors.current
    val song = app.current ?: return
    var dragged by remember { mutableStateOf(0f) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.card)
            .navigationBarsPadding()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragged = 0f },
                    onDragEnd = { if (dragged < -40f) onOpen() },
                    onDrag = { _, amount -> dragged += amount.y }
                )
            }
    ) {
        ViDivider(colors.divider)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onOpen() }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Ic("chevron_up", 22.dp, colors.icon)
            Spacer(Modifier.width(20.dp))
            Text(
                text = song.title,
                color = colors.text,
                fontSize = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        app.togglePlay()
                    },
                contentAlignment = Alignment.Center
            ) {
                Ic(if (app.playing) "pause" else "play", 26.dp, colors.icon)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(colors.divider)
        ) {
            val fraction = progressFraction(app)
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(3.dp)
                    .background(colors.accent)
            )
        }
    }
}

fun progressFraction(app: AppState): Float {
    val song = app.current ?: return 0f
    if (song.durationMs <= 0) return 0f
    return (app.positionMs.toFloat() / song.durationMs).coerceIn(0f, 1f)
}

// ---------------------------------------------------------------------------
// now playing
// ---------------------------------------------------------------------------

@Composable
fun NowPlayingScreen(app: AppState, onClose: () -> Unit, onOpenEqualizer: () -> Unit) {
    val colors = LocalViColors.current
    val dialogs = LocalDialogs.current
    val song = app.current ?: return
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.nowPlaying)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButtonNoRipple("close", Color.White, onClose)
            Spacer(Modifier.weight(1f))
            IconButtonNoRipple(
                if (app.isFavorite(song.id)) "heart_fill" else "heart",
                if (app.isFavorite(song.id)) colors.accent else Color.White,
                { app.toggleFavorite(song.id) }
            )
            MenuDots(Color.White) {
                listOf(
                    MenuItemSpec("清空播放队列") { app.clearQueue(); onClose() },
                    MenuItemSpec("保存播放队列") {
                        dialogs.saveAsPlaylist = "__queue__"
                    },
                    MenuItemSpec("睡眠定时器") { dialogs.sleepTimer = true },
                    MenuItemSpec("均衡器") { onOpenEqualizer() }
                )
            }
        }
        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            VinylArt(300.dp)
        }
        Column(modifier = Modifier.fillMaxWidth().background(colors.nowPlayingControls)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.Center)
                        .background(colors.divider)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFraction(app))
                            .height(3.dp)
                            .background(colors.accent)
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(mmss(app.positionMs), color = colors.text, fontSize = 15.sp)
                Spacer(Modifier.weight(1f))
                Text(song.durationText, color = colors.text, fontSize = 15.sp)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButtonNoRipple(
                    "repeat",
                    if (app.repeatMode == 0) colors.text.copy(alpha = 0.45f) else colors.accent,
                    { app.cycleRepeat() },
                    size = 30.dp
                )
                IconButtonNoRipple("prev", colors.text, { app.previous() }, size = 34.dp)
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF2F2F2))
                        .clickable { app.togglePlay() },
                    contentAlignment = Alignment.Center
                ) {
                    Ic(if (app.playing) "pause" else "play", 34.dp, Color(0xFF202020))
                }
                IconButtonNoRipple("next", colors.text, { app.next() }, size = 34.dp)
                IconButtonNoRipple(
                    "shuffle",
                    if (app.shuffle) colors.accent else colors.text.copy(alpha = 0.45f),
                    { app.toggleShuffle() },
                    size = 30.dp
                )
            }
        }
        QueueCard(app)
    }
}

@Composable
fun ColumnScope.QueueCard(app: AppState) {
    val colors = LocalViColors.current
    val dialogs = LocalDialogs.current
    val nav = LocalNav.current
    val song = app.current ?: return
    val totalMs = app.queue.mapNotNull { Library.byId(it) }.sumOf { it.durationMs }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(top = 14.dp)
            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
            .background(colors.queueCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VinylArt(38.dp)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, color = colors.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(songSubtitle(song), color = colors.subtitle, fontSize = 14.sp)
            }
            MenuDots(colors.icon) {
                queueMenuItems(app, song, dialogs, nav)
            }
        }
        Text(
            text = "即将播放 · ${mmss(totalMs)} · ${app.queueIndex + 1}/${app.queue.size}",
            color = colors.subtitle,
            fontSize = 15.sp,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 6.dp)
        )
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            itemsIndexed(app.queue) { index, id ->
                val item = Library.byId(id) ?: return@itemsIndexed
                val offset = index - app.queueIndex
                QueueRow(
                    app = app,
                    song = item,
                    offset = offset,
                    index = index,
                    onMenu = { queueMenuItems(app, item, dialogs, nav) }
                )
            }
        }
    }
}

@Composable
fun QueueRow(
    app: AppState,
    song: Song,
    offset: Int,
    index: Int,
    onMenu: () -> List<MenuItemSpec>
) {
    val colors = LocalViColors.current
    val isCurrent = offset == 0
    var dragAccum by remember { mutableStateOf(0f) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .background(if (isCurrent) colors.highlighted else Color.Transparent)
            .padding(end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(76.dp)
                .pointerInput(index) {
                    detectDragGestures(
                        onDragStart = { dragAccum = 0f },
                        onDrag = { _, amount ->
                            dragAccum += amount.y
                            if (dragAccum < -60f) {
                                app.moveInQueue(index, index - 1)
                                dragAccum = 0f
                            } else if (dragAccum > 60f) {
                                app.moveInQueue(index, index + 1)
                                dragAccum = 0f
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isCurrent) "0" else offset.toString(),
                color = if (isCurrent) colors.accent else colors.subtitle,
                fontSize = 16.sp
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = if (isCurrent) colors.accent else colors.text,
                fontSize = 17.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                songSubtitle(song),
                color = colors.subtitle,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        MenuDots(colors.icon, onMenu)
    }
}

fun queueMenuItems(
    app: AppState,
    song: Song,
    dialogs: DialogController,
    nav: Navigator
): List<MenuItemSpec> = listOf(
    MenuItemSpec("作为下一首播放") { app.playNext(song.id) },
    MenuItemSpec("从播放队列中移除") { app.removeFromQueue(song.id) },
    MenuItemSpec("加入播放列表…") { dialogs.addToPlaylist = listOf(song.id) },
    MenuItemSpec("查看专辑") { nav.push("album", song.album) },
    MenuItemSpec("查看艺术家") { nav.push("artist", song.artist) },
    MenuItemSpec("分享") { app.snackbar = "分享 ${song.title}" },
    MenuItemSpec("音乐标签编辑器") { dialogs.tagEditor = song },
    MenuItemSpec("详情") { dialogs.songDetails = song },
    MenuItemSpec("设为铃声") { dialogs.ringtone = true },
    MenuItemSpec("从设备中删除", destructive = true) { nav.push("sdcard") }
)

// ---------------------------------------------------------------------------
// dialogs
// ---------------------------------------------------------------------------

@Composable
fun SleepTimerDialog(app: AppState, onDismiss: () -> Unit) {
    val colors = LocalViColors.current
    var minutes by remember { mutableStateOf(app.sleepMinutes.toFloat()) }
    var afterTrack by remember { mutableStateOf(app.sleepAfterTrack) }
    ViDialog(onDismiss = onDismiss) {
        DialogTitle("睡眠定时器")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(180.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            val dx = change.position.x - size.width / 2f
                            val dy = change.position.y - size.height / 2f
                            var angle = Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            angle = (angle + 450f) % 360f
                            minutes = (30f + angle / 360f * 60f).coerceIn(5f, 90f)
                        }
                    }
            ) {
                val r = this.size.minDimension / 2f
                drawCircle(Color(0xFF3A3A3A), radius = r, center = center)
                for (i in 0 until 30) {
                    val a = Math.toRadians((i * 12).toDouble())
                    val inner = r * 0.78f
                    val outer = r * 0.95f
                    drawLine(
                        Color(0xFF8A8A8A),
                        Offset(center.x + inner * Math.cos(a).toFloat(), center.y + inner * Math.sin(a).toFloat()),
                        Offset(center.x + outer * Math.cos(a).toFloat(), center.y + outer * Math.sin(a).toFloat()),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }
                val sweep = (minutes - 30f) / 60f * 360f
                drawArc(
                    color = Color(0xFFF50057),
                    startAngle = -90f, sweepAngle = sweep, useCenter = false,
                    topLeft = Offset(center.x - r * 0.86f, center.y - r * 0.86f),
                    size = androidx.compose.ui.geometry.Size(r * 1.72f, r * 1.72f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 10f, cap = StrokeCap.Round)
                )
                drawCircle(Color(0xFFF50057), radius = 12f, center = center)
            }
            Text(
                "${minutes.toInt()} 分",
                color = Color.White,
                fontSize = 20.sp
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { afterTrack = !afterTrack }
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (afterTrack) colors.accent else colors.divider),
                contentAlignment = Alignment.Center
            ) {
                if (afterTrack) Ic("check", 14.dp, Color.White)
            }
            Spacer(Modifier.width(20.dp))
            Text("播完当前音乐", color = colors.text, fontSize = 17.sp)
        }
        DialogActions(
            "设置" to {
                app.sleepMinutes = minutes.toInt()
                app.sleepAfterTrack = afterTrack
                app.sleepArmed = true
                app.persistQueue()
                app.snackbar = "睡眠定时器已设置为 ${minutes.toInt()} 分"
                onDismiss()
            }
        )
    }
}

@Composable
fun SongDetailsDialog(song: Song, onDismiss: () -> Unit) {
    val colors = LocalViColors.current
    val rows = listOf(
        "文件路径" to song.path,
        "大小" to formatSize(song.sizeBytes),
        "格式" to song.format,
        "比特率" to song.bitrate,
        "采样率" to song.sampleRate,
        "Added" to song.added,
        "Modified" to song.modified,
        "音轨" to if (song.track > 0) song.track.toString() else "-",
        "光碟号" to if (song.disc > 0) song.disc.toString() else "-",
        "标题" to song.title,
        "艺术家" to song.artist.ifEmpty { "-" },
        "专辑" to song.album.ifEmpty { "-" },
        "专辑艺术家" to song.albumArtist.ifEmpty { "-" },
        "流派" to song.genre.ifEmpty { "-" },
        "年份" to song.year.ifEmpty { "-" },
        "长度" to song.durationText,
        "ReplayGain" to song.replayGain.ifEmpty { "-" }
    )
    ViDialog(onDismiss = onDismiss) {
        DialogTitle("详情")
        LazyColumn(modifier = Modifier.height(420.dp).fillMaxWidth()) {
            items(rows.size) { index ->
                val (label, value) = rows[index]
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)) {
                    Text(label, color = colors.subtitle, fontSize = 14.sp)
                    Text(value, color = colors.text, fontSize = 16.sp)
                }
            }
        }
        DialogActions("确定" to onDismiss)
    }
}

@Composable
fun TagEditorDialog(song: Song, onDismiss: () -> Unit) {
    val colors = LocalViColors.current
    var allowed by remember { mutableStateOf(false) }
    var album by remember { mutableStateOf(song.album) }
    var albumArtist by remember { mutableStateOf(song.albumArtist) }
    var genres by remember { mutableStateOf(song.genre) }
    var year by remember { mutableStateOf(song.year) }
    ViDialog(onDismiss = onDismiss) {
        if (!allowed) {
            DialogTitle("音乐标签编辑器")
            DialogText("要允许Vinyl Music Player修改这 3 个音频文件吗？")
            DialogActions("拒绝" to onDismiss, "允许" to { allowed = true })
        } else {
            DialogTitle("音乐标签编辑器")
            TagField("专辑", album) { album = it }
            TagField("专辑作者（每行一个）", albumArtist) { albumArtist = it }
            TagField("Genres (one per line)", genres) { genres = it }
            TagField("年份", year) { year = it }
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.accent)
                        .clickable { onDismiss() }
                        .padding(horizontal = 22.dp, vertical = 12.dp)
                ) {
                    Text("保存", color = Color.White, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun TagField(label: String, value: String, onChange: (String) -> Unit) {
    val colors = LocalViColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp)) {
        Text(label, color = colors.accent, fontSize = 14.sp)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = androidx.compose.ui.text.TextStyle(color = colors.text, fontSize = 17.sp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )
        ViDivider()
    }
}

@Composable
fun AddToPlaylistDialog(app: AppState, songIds: List<String>, onDismiss: () -> Unit) {
    val colors = LocalViColors.current
    val dialogs = LocalDialogs.current
    ViDialog(onDismiss = onDismiss) {
        DialogTitle("添加到播放列表")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(vertical = 4.dp)
        ) {
            LazyColumn {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDismiss()
                                dialogs.newPlaylist = songIds
                            }
                            .padding(horizontal = 24.dp, vertical = 14.dp)
                    ) {
                        Text("新建播放列表…", color = colors.text, fontSize = 17.sp)
                    }
                }
                items(app.playlists().filter { it.kind != "smart" }) { playlist ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDismiss()
                                app.addToPlaylist(playlist.name, songIds, created = false)
                            }
                            .padding(horizontal = 24.dp, vertical = 14.dp)
                    ) {
                        Text(playlist.name, color = colors.text, fontSize = 17.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun NewPlaylistDialog(app: AppState, songIds: List<String>, onDismiss: () -> Unit) {
    val colors = LocalViColors.current
    var name by remember { mutableStateOf("") }
    ViDialog(onDismiss = onDismiss) {
        DialogTitle("新建播放列表")
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = colors.text, fontSize = 18.sp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
            if (name.isEmpty()) {
                Text("播放列表名称", color = colors.subtitle, fontSize = 18.sp, modifier = Modifier.padding(vertical = 8.dp))
            }
        }
        ViDivider()
        DialogActions(
            "取消" to onDismiss,
            "创建" to {
                if (name.isNotBlank()) {
                    app.createPlaylist(name.trim(), songIds)
                }
                onDismiss()
            }
        )
    }
}

@Composable
fun RenamePlaylistDialog(app: AppState, playlistName: String, onDismiss: () -> Unit) {
    val colors = LocalViColors.current
    var name by remember { mutableStateOf(playlistName) }
    ViDialog(onDismiss = onDismiss) {
        DialogTitle("重命名播放列表")
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = colors.text, fontSize = 18.sp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
        }
        ViDivider()
        DialogActions(
            "取消" to onDismiss,
            "重命名" to {
                if (name.isNotBlank()) app.renamePlaylist(playlistName, name.trim())
                onDismiss()
            }
        )
    }
}

@Composable
fun ConfirmDeletePlaylistDialog(app: AppState, playlistName: String, onDismiss: () -> Unit) {
    ViDialog(onDismiss = onDismiss) {
        DialogTitle("删除播放列表")
        DialogText("确定要删除播放列表 $playlistName 吗？")
        DialogActions(
            "取消" to onDismiss,
            "删除" to {
                app.deletePlaylist(playlistName)
                onDismiss()
            }
        )
    }
}

@Composable
fun SaveQueueDialog(app: AppState, onDismiss: () -> Unit) {
    val colors = LocalViColors.current
    var name by remember { mutableStateOf("") }
    ViDialog(onDismiss = onDismiss) {
        DialogTitle("保存播放队列")
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = colors.text, fontSize = 18.sp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
            if (name.isEmpty()) {
                Text("播放列表名称", color = colors.subtitle, fontSize = 18.sp, modifier = Modifier.padding(vertical = 8.dp))
            }
        }
        ViDivider()
        DialogActions(
            "取消" to onDismiss,
            "保存" to {
                if (name.isNotBlank()) app.createPlaylist(name.trim(), app.queue)
                onDismiss()
            }
        )
    }
}

@Composable
fun RingtoneDialog(onDismiss: () -> Unit) {
    ViDialog(onDismiss = onDismiss) {
        DialogTitle("设置为铃声")
        DialogText("在设置铃声前请允许Vinyl音乐播放器在下一屏修改系统设置。")
        DialogActions("取消" to onDismiss, "确定" to onDismiss)
    }
}

@Composable
fun EqualizerDialog(app: AppState, onDismiss: () -> Unit) {
    val colors = LocalViColors.current
    var enabled by remember { mutableStateOf(true) }
    val bands = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")
    var levels by remember { mutableStateOf(listOf(0, 0, 0, 0, 0)) }
    ViDialog(onDismiss = onDismiss) {
        DialogTitle("均衡器")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { enabled = !enabled }
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (enabled) colors.accent else colors.divider),
                contentAlignment = Alignment.Center
            ) { if (enabled) Ic("check", 14.dp, Color.White) }
            Spacer(Modifier.width(18.dp))
            Text("启用均衡器", color = colors.text, fontSize = 17.sp)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bands.forEachIndexed { index, label ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${levels[index]}dB", color = colors.subtitle, fontSize = 12.sp)
                    Box(
                        modifier = Modifier
                            .width(30.dp)
                            .height(140.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.divider)
                            .pointerInput(index) {
                                detectDragGestures { change, _ ->
                                    val ratio = 1f - (change.position.y / size.height.toFloat())
                                    val level = ((ratio * 24f) - 12f).toInt().coerceIn(-12, 12)
                                    levels = levels.toMutableList().also { it[index] = level }
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(((levels[index] + 12) / 24f * 140).dp)
                                .align(Alignment.BottomCenter)
                                .background(colors.accent)
                        )
                    }
                    Text(label, color = colors.subtitle, fontSize = 12.sp)
                }
            }
        }
        DialogActions("确定" to onDismiss)
    }
}
