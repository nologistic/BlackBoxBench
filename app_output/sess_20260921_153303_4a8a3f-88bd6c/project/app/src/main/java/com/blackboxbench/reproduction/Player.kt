package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NowPlayingScreen(state: AppState) {
    val track = state.currentTrack
    val accent = Color(state.accentColor)
    val primary = Color(state.primaryColor)
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
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
                        .clickable { state.screen = state.lastLibraryScreen },
                    contentAlignment = Alignment.Center
                ) { CloseGlyph(Color.White) }
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .size(48.dp)
                        .clickable { track?.let { state.toggleFavorite(it.id) } },
                    contentAlignment = Alignment.Center
                ) {
                    HeartGlyph(Color.White, 24.dp, filled = track != null && state.isFavorite(track.id))
                }
                OverflowButton(entries = {
                    listOf(
                        MenuEntry("清空播放队列") {
                            state.clearQueue(); state.screen = state.lastLibraryScreen
                            state.toast("播放队列已清空")
                        },
                        MenuEntry("保存播放队列") { state.dialog = DialogKind.SAVE_QUEUE },
                        MenuEntry("睡眠定时器") { state.dialog = DialogKind.SLEEP_TIMER },
                        MenuEntry("均衡器") { state.dialog = DialogKind.EQUALIZER },
                    )
                }, tint = Color.White)
            }
        }
        Box(Modifier.fillMaxWidth().height(210.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(190.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF1E1E1E)),
                contentAlignment = Alignment.Center
            ) { VinylGlyph(Color(0xFF9E9E9E), 150.dp) }
        }
        val duration = (track?.seconds ?: 0) * 1000L
        var barWidth by remember { mutableStateOf(1) }
        val density = LocalDensity.current
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .onSizeChanged { barWidth = it.width }
                    .pointerInput(duration) {
                        detectTapGestures { offset ->
                            if (duration > 0 && barWidth > 0) {
                                val fraction = (offset.x / barWidth).coerceIn(0f, 1f)
                                state.positionMs = (duration * fraction).toLong()
                            }
                        }
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color(0x33000000))
                )
                Box(
                    Modifier
                        .fillMaxWidth(if (duration > 0) (state.positionMs.toFloat() / duration).coerceIn(0f, 1f) else 0f)
                        .height(4.dp)
                        .background(accent)
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(state.positionMs), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatTime(duration), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                Modifier.size(48.dp).clickable { state.repeatMode = (state.repeatMode + 1) % 3 },
                contentAlignment = Alignment.Center
            ) {
                RepeatGlyph(
                    if (state.repeatMode > 0) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    26.dp
                )
            }
            Box(
                Modifier.size(56.dp).clickable { state.previous() },
                contentAlignment = Alignment.Center
            ) { PrevGlyph(MaterialTheme.colorScheme.onSurface, 30.dp) }
            Surface(
                modifier = Modifier
                    .size(72.dp)
                    .clickable { state.togglePlay() },
                shape = RoundedCornerShape(36.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (state.playing) PauseGlyph(MaterialTheme.colorScheme.onSurface, 30.dp)
                    else PlayGlyph(MaterialTheme.colorScheme.onSurface, 30.dp)
                }
            }
            Box(
                Modifier.size(56.dp).clickable { state.next() },
                contentAlignment = Alignment.Center
            ) { NextGlyph(MaterialTheme.colorScheme.onSurface, 30.dp) }
            Box(
                Modifier.size(48.dp).clickable { state.shuffle = !state.shuffle },
                contentAlignment = Alignment.Center
            ) {
                ShuffleGlyph(
                    if (state.shuffle) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    26.dp
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        QueueList(state)
    }
}

@Composable
private fun QueueList(state: AppState) {
    val current = state.currentTrack
    val queueItems = state.queue.toList().withIndex()
        .filter { it.index != state.queueIndex }
        .map { it.index to it.value }
    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        if (current != null) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(6.dp),
                    shadowElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    QueueRow(
                        state = state,
                        track = current,
                        label = null,
                        isCurrent = true,
                        dimmed = false,
                        onOverflow = { }
                    )
                }
            }
        }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "即将播放 · ${formatTotal(state.queueTotalSeconds())} · " +
                        "${(state.queueIndex + 1).coerceAtLeast(0)}/${state.queue.size}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        itemsIndexed(queueItems, key = { index, pair -> "${pair.first}-$index" }) { position, pair ->
            val index = pair.first
            val track = Library.byId(pair.second) ?: return@itemsIndexed
            val isCurrent = index == state.queueIndex
            val label = when {
                isCurrent -> null
                else -> (index - state.queueIndex).toString()
            }
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)
            ) {
                QueueRow(
                    state = state,
                    track = track,
                    label = label,
                    isCurrent = isCurrent,
                    dimmed = index < state.queueIndex,
                    onOverflow = {
                        state.removeFromQueue(index)
                        state.toast("已从播放队列中移除")
                    }
                )
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun QueueRow(
    state: AppState,
    track: Track,
    label: String?,
    isCurrent: Boolean,
    dimmed: Boolean,
    onOverflow: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(start = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (label != null) {
            Text(
                label,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(30.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        } else {
            Spacer(Modifier.width(30.dp))
        }
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            if (isCurrent) NoteGlyph(Color(state.accentColor), 26.dp)
            else CoverArt(44, track.album)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                fontSize = 16.sp,
                color = if (dimmed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                "${track.artist} · ${track.album}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        OverflowButton(entries = {
            listOf(
                MenuEntry("作为下一首播放") { state.playNext(track.id) },
                MenuEntry("从播放队列中移除") {
                    val index = state.queue.indexOf(track.id)
                    if (index >= 0) state.removeFromQueue(index)
                    state.toast("已从播放队列中移除")
                },
                MenuEntry("加入播放列表…") {
                    state.selectedIds.clear(); state.selectedIds.add(track.id)
                    state.dialog = DialogKind.ADD_TO_PLAYLIST
                },
                MenuEntry("查看专辑") { state.openAlbum = track.album; state.screen = Screen.ALBUM_DETAIL },
                MenuEntry("查看艺术家") { state.openArtist = track.artist; state.screen = Screen.ARTIST_DETAIL },
                MenuEntry("分享") { state.toast("已分享 ${track.title}") },
                MenuEntry("音乐标签编辑器") {
                    state.tagEditorTrackId = track.id; state.screen = Screen.TAG_EDITOR
                },
                MenuEntry("详情") { state.detailsTrackId = track.id; state.dialog = DialogKind.DETAILS },
                MenuEntry("设为铃声") { state.toast("已设为铃声") },
                MenuEntry("从设备中删除", danger = true) { state.toast("已从设备中删除 ${track.title}") },
            )
        }, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun EqualizerDialog(state: AppState, onClose: () -> Unit) {
    val bands = remember { mutableStateListOf(0f, 0f, 0f, 0f, 0f) }
    var enabled by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("均衡器") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SwitchPill(enabled, Color(state.accentColor)) { enabled = it }
                    Spacer(Modifier.width(12.dp))
                    Text(if (enabled) "已启用" else "已关闭", fontSize = 15.sp)
                }
                Spacer(Modifier.height(8.dp))
                listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz").forEachIndexed { index, label ->
                    Column {
                        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Slider(
                            value = bands[index],
                            onValueChange = { bands[index] = it },
                            valueRange = -15f..15f,
                            enabled = enabled
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("确定") } }
    )
}
