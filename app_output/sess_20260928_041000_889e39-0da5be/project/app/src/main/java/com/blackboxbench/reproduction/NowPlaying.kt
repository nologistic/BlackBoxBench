package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NowPlayingScreen(
    model: AppModel,
    onClose: () -> Unit,
    onMore: () -> Unit,
    onSongMore: (Song) -> Unit,
    onQueueMore: (Song) -> Unit,
    onOpenQueueItem: (Song) -> Unit
) {
    val player = model.player
    val song = player.current
    if (song == null) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xFF303030)),
            contentAlignment = Alignment.Center
        ) { Text("没有正在播放的歌曲", color = Color.White) }
        return
    }
    val bg = albumColorDark(song.album)
    val duration = song.durationSec.coerceAtLeast(1)
    Column(
        Modifier
            .fillMaxSize()
            .background(bg)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "关闭", tint = Color.White)
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { model.repo.toggleFavorite(song.id) }) {
                Icon(
                    if (model.repo.isFavorite(song.id)) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "收藏",
                    tint = Color.White
                )
            }
            IconButton(onClick = onMore) {
                Icon(Icons.Filled.MoreVert, contentDescription = "更多", tint = Color.White)
            }
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            VinylCover(
                album = song.album,
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .aspectRatio(1f)
                    .padding(top = 4.dp),
                cornerRadius = 2.dp
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(formatDuration(player.positionSec.toInt()), color = Color.White, fontSize = 12.sp)
            Spacer(Modifier.width(10.dp))
            ThinSeekBar(
                progress = (player.positionSec / duration).coerceIn(0f, 1f),
                modifier = Modifier
                    .weight(1f)
                    .height(20.dp),
                onSeek = { player.seekTo(it) }
            )
            Spacer(Modifier.width(10.dp))
            Text(formatDuration(duration), color = Color.White, fontSize = 12.sp)
        }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { player.toggleShuffle() }) {
                Icon(
                    painterResource(R.drawable.ic_shuffle),
                    contentDescription = "随机",
                    tint = if (player.shuffle) Color.White else Color(0x99FFFFFF)
                )
            }
            IconButton(onClick = { player.previous() }) {
                Icon(painterResource(R.drawable.ic_skip_previous), contentDescription = "上一首", tint = Color.White)
            }
            Box(
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { player.togglePlayPause() },
                contentAlignment = Alignment.Center
            ) {
                if (player.isPlaying) {
                    Icon(painterResource(R.drawable.ic_pause), contentDescription = "暂停", tint = bg)
                } else {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "播放", tint = bg, modifier = Modifier.size(40.dp))
                }
            }
            IconButton(onClick = { player.next() }) {
                Icon(painterResource(R.drawable.ic_skip_next), contentDescription = "下一首", tint = Color.White)
            }
            IconButton(onClick = { player.cycleRepeat() }) {
                val res = if (player.repeatMode == 2) R.drawable.ic_repeat_one else R.drawable.ic_repeat
                Icon(
                    painterResource(res),
                    contentDescription = "循环",
                    tint = if (player.repeatMode != 0) Color.White else Color(0x99FFFFFF)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Surface(
            color = Color(0xFFF3F3F3),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                VinylCover(song.album, Modifier.size(48.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(song.title, fontSize = 16.sp, color = Color(0xFF212121), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(song.artist, fontSize = 13.sp, color = VinylColors.SecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = { onSongMore(song) }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "更多", tint = Color(0xFF616161))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Surface(
            color = Color(0xFFEFEFEF),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            Column {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    val total = player.queue.sumOf { model.repo.songById(it)?.durationSec ?: 0 }
                    Text(
                        "即将播放 · ${formatDuration(total)} · ${player.index + 1}/${player.queue.size}",
                        color = VinylColors.SecondaryText,
                        fontSize = 13.sp
                    )
                }
                player.queue.forEachIndexed { i, id ->
                    val s = model.repo.songById(id) ?: return@forEachIndexed
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(if (id == song.id) Color(0xFFE0E0E0) else Color.Transparent)
                            .clickable { onOpenQueueItem(s) }
                            .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("$i", color = VinylColors.SecondaryText, fontSize = 13.sp, modifier = Modifier.width(24.dp))
                        VinylCover(s.album, Modifier.size(44.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.title, fontSize = 15.sp, color = Color(0xFF212121), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(s.artist, fontSize = 12.sp, color = VinylColors.SecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { onQueueMore(s) }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "更多", tint = Color(0xFF616161))
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ThinSeekBar(progress: Float, modifier: Modifier = Modifier, onSeek: (Float) -> Unit) {
    BoxWithConstraints(modifier) {
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(widthPx) {
                    detectTapGestures { offset -> onSeek((offset.x / widthPx).coerceIn(0f, 1f)) }
                }
                .pointerInput(widthPx) {
                    detectHorizontalDragGestures { change, _ ->
                        onSeek((change.position.x / widthPx).coerceIn(0f, 1f))
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color(0x55FFFFFF))
            )
            Box(
                Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(2.dp)
                    .background(Color.White)
            )
            Box(
                Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(20.dp)
            ) {
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .width(3.dp)
                        .height(16.dp)
                        .background(Color.White)
                )
            }
        }
    }
}
