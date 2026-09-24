package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MiniPlayer(state: AppState, colors: AppColors, onOpen: () -> Unit) {
    val track = state.currentTrack
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpen() }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                ChevronGlyph(size = 20.dp, tint = colors.secondaryText, up = true)
            }
            Spacer(Modifier.width(4.dp))
            Text(
                track?.title ?: "未在播放",
                color = colors.onSurface,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Box(
                Modifier
                    .size(44.dp)
                    .clickable { state.togglePlay() },
                contentAlignment = Alignment.Center
            ) {
                if (state.playing) PauseGlyph(size = 22.dp, tint = colors.secondaryText)
                else PlayGlyph(size = 22.dp, tint = colors.secondaryText)
            }
        }
        if (track != null) {
            val progress = (state.positionSec.toFloat() / track.durationSec.coerceAtLeast(1)).coerceIn(0f, 1f)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(colors.divider)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress)
                        .height(3.dp)
                        .background(colors.accent)
                )
            }
        }
    }
}

@Composable
private fun PlayerIconButton(onClick: () -> Unit, size: Int = 40, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(size.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
fun NowPlayingScreen(state: AppState, navigator: Navigator) {
    val track = state.currentTrack
    var overflow by remember { mutableStateOf(false) }
    var sleepDialog by remember { mutableStateOf(false) }
    var saveDialog by remember { mutableStateOf(false) }
    var equalizerDialog by remember { mutableStateOf(false) }

    val dark = Color(0xFF212121)
    val panel = Color(0xFFE0E0E0)

    Column(Modifier.fillMaxSize().background(dark).statusBarsPadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayerIconButton({ navigator.pop() }) { CloseGlyph(size = 22.dp, tint = Color.White) }
            Spacer(Modifier.weight(1f))
            PlayerIconButton({
                track?.let { state.toggleFavorite(it.id) }
            }) {
                val fav = track != null && state.isFavorite(track.id)
                if (fav) HeartGlyph(size = 24.dp, tint = Color(0xFFE91E63))
                else HeartGlyph(size = 24.dp, tint = Color.White)
            }
            Box {
                PlayerIconButton({ overflow = true }) { MoreVertGlyph(size = 22.dp, tint = Color.White) }
                DropdownMenu(expanded = overflow, onDismissRequest = { overflow = false }) {
                    DropdownMenuItem(
                        text = { Text("清空播放队列", fontSize = 15.sp) },
                        onClick = { overflow = false; state.clearQueue() }
                    )
                    DropdownMenuItem(
                        text = { Text("保存播放队列", fontSize = 15.sp) },
                        onClick = { overflow = false; saveDialog = true }
                    )
                    DropdownMenuItem(
                        text = { Text("睡眠定时器", fontSize = 15.sp) },
                        onClick = { overflow = false; sleepDialog = true }
                    )
                    DropdownMenuItem(
                        text = { Text("均衡器", fontSize = 15.sp) },
                        onClick = { overflow = false; equalizerDialog = true }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .fillMaxWidth(0.78f)
                    .height(320.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(coverColor(track?.album ?: "vinyl")),
                contentAlignment = Alignment.Center
            ) {
                VinylIcon(size = 240.dp, tint = Color(0xFF9E9E9E))
            }
        }

        if (track != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(formatDuration(state.positionSec), color = Color.White, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                Text(track.durationLabel, color = Color.White, fontSize = 13.sp)
            }
            SeekBar(
                progress = (state.positionSec.toFloat() / track.durationSec.coerceAtLeast(1)).coerceIn(0f, 1f),
                onChange = { fraction -> state.seekTo((fraction * track.durationSec).toInt()) }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(panel)
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            PlayerIconButton(size = 44, onClick = { state.cycleRepeat() }) {
                val active = state.repeatMode != RepeatMode.OFF
                RepeatGlyph(
                    size = 24.dp,
                    tint = if (active) Color(0xFF212121) else Color(0xFF9E9E9E),
                    one = state.repeatMode == RepeatMode.ONE
                )
            }
            PlayerIconButton(size = 52, onClick = { state.previous() }) { SkipPrevGlyph(size = 30.dp, tint = Color(0xFF212121)) }
            Box(
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { state.togglePlay() },
                contentAlignment = Alignment.Center
            ) {
                if (state.playing) PauseGlyph(size = 30.dp, tint = Color(0xFF212121))
                else PlayGlyph(size = 30.dp, tint = Color(0xFF212121))
            }
            PlayerIconButton(size = 52, onClick = { state.next() }) { SkipNextGlyph(size = 30.dp, tint = Color(0xFF212121)) }
            PlayerIconButton(size = 44, onClick = { state.toggleShuffle() }) {
                ShuffleGlyph(size = 24.dp, tint = if (state.shuffle) Color(0xFF212121) else Color(0xFF9E9E9E))
            }
        }

        val queue = state.queueTracks
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.25f)
                .background(Color.White)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "即将播放 • ${formatDuration(state.remainingSeconds())} • ${if (queue.isEmpty()) 0 else state.queueIndex + 1}/${queue.size}",
                        color = Color(0xFF757575),
                        fontSize = 14.sp
                    )
                }
            }
            itemsIndexed(queue, key = { index, item -> "$index-${item.id}" }) { index, item ->
                val isCurrent = index == state.queueIndex
                val relative = index - state.queueIndex
                val played = relative < 0
                var menu by remember { mutableStateOf(false) }
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { state.jumpTo(index) }
                            .padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.width(36.dp), contentAlignment = Alignment.Center) {
                            if (isCurrent) {
                                VinylIcon(size = 26.dp, tint = Color(0xFF616161))
                            } else {
                                Text(
                                    "$relative",
                                    color = if (played) Color(0xFFBDBDBD) else Color(0xFF616161),
                                    fontSize = 14.sp
                                )
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                item.title,
                                color = if (played) Color(0xFF9E9E9E) else Color(0xFF212121),
                                fontSize = 16.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                item.artist,
                                color = Color(0xFF9E9E9E),
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Box(
                            Modifier
                                .size(44.dp)
                                .clickable { menu = true },
                            contentAlignment = Alignment.Center
                        ) { MoreVertGlyph(size = 20.dp, tint = Color(0xFF9E9E9E)) }
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("从播放队列中移除", fontSize = 15.sp) },
                            onClick = { menu = false; state.removeFromQueue(index) }
                        )
                        DropdownMenuItem(
                            text = { Text("加入播放列表…", fontSize = 15.sp) },
                            onClick = { menu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("查看专辑", fontSize = 15.sp) },
                            onClick = { menu = false; navigator.push(Screen.Album(item.album)) }
                        )
                        DropdownMenuItem(
                            text = { Text("查看艺术家", fontSize = 15.sp) },
                            onClick = { menu = false; navigator.push(Screen.Artist(item.artist)) }
                        )
                    }
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 44.dp)
                        .height(1.dp)
                        .background(Color(0xFFEEEEEE))
                )
            }
            if (queue.isEmpty()) {
                item {
                    Text(
                        "播放队列为空",
                        color = Color(0xFF757575),
                        fontSize = 15.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    if (sleepDialog) {
        SleepTimerDialog(
            onDismiss = { sleepDialog = false },
            onConfirm = { minutes, afterCurrent ->
                state.startSleepTimer(minutes, afterCurrent)
                sleepDialog = false
            }
        )
    }
    if (saveDialog) {
        TextInputDialog(
            title = "新建播放列表",
            label = "播放列表名称",
            confirmLabel = "创建",
            colors = rememberAppColors(state),
            onDismiss = { saveDialog = false },
            onConfirm = { name ->
                state.createPlaylist(name, state.queue)
                saveDialog = false
            }
        )
    }
    if (equalizerDialog) {
        EqualizerDialog(onDismiss = { equalizerDialog = false }, colors = rememberAppColors(state))
    }
}

@Composable
fun SeekBar(progress: Float, onChange: (Float) -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onChange((offset.x / size.width).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    onChange((change.position.x / size.width).coerceIn(0f, 1f))
                }
            }
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .align(Alignment.Center)
                .background(Color(0xFF616161))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(3.dp)
                    .background(Color.White)
            )
        }
    }
}

@Composable
fun SleepTimerDialog(onDismiss: () -> Unit, onConfirm: (Int, Boolean) -> Unit) {
    var minutes by remember { mutableStateOf(30) }
    var afterCurrent by remember { mutableStateOf(false) }
    val accent = Color(0xFFE91E63)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("睡眠定时器", fontSize = 20.sp) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val dx = change.position.x - center.x
                                val dy = change.position.y - center.y
                                var angle = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                                if (angle < 0) angle += 360f
                                minutes = ((angle / 360f) * 30f).toInt().coerceIn(1, 30)
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures { position ->
                                val center = Offset(size.width / 2f, size.height / 2f)
                                var angle = Math.toDegrees(
                                    kotlin.math.atan2(
                                        (position.y - center.y).toDouble(),
                                        (position.x - center.x).toDouble()
                                    )
                                ).toFloat() + 90f
                                if (angle < 0) angle += 360f
                                minutes = ((angle / 360f) * 30f).toInt().coerceIn(1, 30)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val stroke = 26f
                        drawArc(
                            color = Color(0xFFE0E0E0),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = Offset(stroke / 2, stroke / 2),
                            size = Size(size.width - stroke, size.height - stroke),
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = accent,
                            startAngle = -90f,
                            sweepAngle = 360f * (minutes / 30f),
                            useCenter = false,
                            topLeft = Offset(stroke / 2, stroke / 2),
                            size = Size(size.width - stroke, size.height - stroke),
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                    Text("+$minutes 分", fontSize = 26.sp, color = Color(0xFF212121))
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { afterCurrent = !afterCurrent }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (afterCurrent) accent else Color.Transparent)
                            .border(
                                width = 2.dp,
                                color = if (afterCurrent) accent else Color(0xFF9E9E9E),
                                shape = RoundedCornerShape(3.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (afterCurrent) CheckMarkGlyph()
                    }
                    Spacer(Modifier.width(16.dp))
                    Text("播完当前音乐", fontSize = 15.sp)
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(minutes, afterCurrent) }) { Text("设置", color = accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = accent) } }
    )
}

@Composable
fun EqualizerDialog(onDismiss: () -> Unit, colors: AppColors) {
    var enabled by remember { mutableStateOf(true) }
    val bands = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")
    val values = remember { bands.map { mutableStateOf(0.5f) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("均衡器", fontSize = 20.sp) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = enabled,
                        onCheckedChange = { enabled = it },
                        colors = CheckboxDefaults.colors(checkedColor = colors.accent)
                    )
                    Text("启用均衡器", fontSize = 15.sp)
                }
                bands.forEachIndexed { index, label ->
                    Text(label, fontSize = 13.sp, color = colors.secondaryText)
                    Slider(
                        value = values[index].value,
                        onValueChange = { values[index].value = it },
                        enabled = enabled,
                        colors = SliderDefaults.colors(thumbColor = colors.accent, activeTrackColor = colors.accent)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                values.forEach { it.value = 0.5f }
                onDismiss()
            }) { Text("重置", color = colors.accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = colors.accent) } },
        containerColor = colors.surface
    )
}
