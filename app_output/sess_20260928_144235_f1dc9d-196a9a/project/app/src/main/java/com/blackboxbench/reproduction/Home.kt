package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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

@Composable
fun Fab(icon: VlcIcon, onClick: () -> Unit) {
    val p = LocalVlcPalette.current
    Box(
        modifier = Modifier
            .size(58.dp)
            .clip(CircleShape)
            .background(p.accent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) { VlcIconView(icon, 30.dp, Color.White) }
}

@Composable
fun EmptyState(message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    val p = LocalVlcPalette.current
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        VlcCone(96.dp, p.textSecondary)
        Spacer(Modifier.height(26.dp))
        Text(message, color = p.textSecondary, fontSize = 15.sp)
        if (actionLabel != null) {
            Spacer(Modifier.height(22.dp))
            OutlinedAction(actionLabel, onAction ?: {})
        }
    }
}

@Composable
fun MediaThumb(item: MediaItem, modifier: Modifier = Modifier, corner: Int = 6) {
    Box(modifier.clip(RoundedCornerShape(corner.dp))) {
        TestPattern(item.pattern, Modifier.fillMaxSize())
        if (item.kind == MediaKind.AUDIO) {
            Box(Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
                VlcIconView(VlcIcon.MUSIC, 30.dp, LocalVlcPalette.current.textSecondary)
            }
        }
    }
}

@Composable
fun Badge(text: String) {
    if (text.isEmpty()) return
    Box(
        Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0x99000000))
            .padding(horizontal = 5.dp, vertical = 1.dp),
    ) { Text(text, color = Color.White, fontSize = 10.sp) }
}

@Composable
fun WatchedMark(show: Boolean) {
    if (!show) return
    Box(
        Modifier.size(20.dp).clip(CircleShape).background(Color(0xCC000000)),
        contentAlignment = Alignment.Center,
    ) { VlcIconView(VlcIcon.CHECK, 13.dp, Color.White) }
}

@Composable
fun UnwatchedRing() {
    Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
        VlcIconView(VlcIcon.RING, 18.dp, Color(0xCCFFFFFF))
    }
}

// ---------------------------------------------------------------- Video tab

@Composable
fun VideoTabContent(
    listLayout: Boolean,
    items: List<MediaItem>,
    onlyFavorites: Boolean,
    onPlay: (MediaItem) -> Unit,
    onMenu: (MediaItem) -> Unit,
    onOpenPlaylistsSubTab: () -> Unit,
    subTab: Int,
    onSubTab: (Int) -> Unit,
) {
    val p = LocalVlcPalette.current
    Column(Modifier.fillMaxSize()) {
        SubTabs(listOf("视频", "播放列表"), subTab, onSubTab)
        if (subTab == 1) {
            if (Store.playlists.isEmpty()) {
                EmptyState("未找到播放列表。")
            } else {
                PlaylistGrid(Store.playlists.toList()) { }
            }
            return@Column
        }
        if (items.isEmpty()) {
            EmptyState(if (onlyFavorites) "没有收藏内容。" else "未找到媒体文件，请传输一些文件到您的设备，或调整您的偏好设置。")
            return@Column
        }
        if (listLayout) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 90.dp)) {
                items(items, key = { it.id }) { item ->
                    MediaListRow(item, onPlay = { onPlay(item) }, onMenu = { onMenu(item) })
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(items, key = { it.id }) { item -> VideoGridCard(item, onPlay = { onPlay(item) }, onMenu = { onMenu(item) }) }
            }
        }
    }
}

@Composable
fun VideoGridCard(item: MediaItem, onPlay: () -> Unit, onMenu: () -> Unit) {
    val p = LocalVlcPalette.current
    Column(Modifier.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onPlay,
    )) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.95f)) {
            MediaThumb(item, Modifier.fillMaxSize())
            Box(Modifier.align(Alignment.TopStart).padding(5.dp)) { Badge(item.badge) }
            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (Store.showWatchedMarker.value) {
                    if (Store.isWatched(item.id)) WatchedMark(true) else UnwatchedRing()
                }
                IconTap(VlcIcon.MORE, 17.dp, Color.White, onMenu)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            item.title,
            color = p.textPrimary,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
        Text(item.durationLabel, color = p.textSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 2.dp))
    }
}

@Composable
fun MediaListRow(item: MediaItem, onPlay: () -> Unit, onMenu: () -> Unit, showArrow: Boolean = false) {
    val p = LocalVlcPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onPlay)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(96.dp).aspectRatio(1.7f)) {
            MediaThumb(item, Modifier.fillMaxSize())
            Box(Modifier.align(Alignment.TopStart).padding(3.dp)) { Badge(item.badge) }
            if (Store.showWatchedMarker.value && Store.isWatched(item.id)) {
                Box(Modifier.align(Alignment.TopEnd).padding(3.dp)) { WatchedMark(true) }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.title, color = p.textPrimary, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            val sub = if (item.kind == MediaKind.AUDIO) item.durationLabel else item.durationLabel + " · " + item.badge
            Text(sub, color = p.textSecondary, fontSize = 12.sp)
        }
        IconTap(VlcIcon.MORE, 20.dp, p.textSecondary, onMenu)
    }
}

// ---------------------------------------------------------------- Audio tab

@Composable
fun AudioTabContent(
    subTabIndex: Int,
    onSubTab: (Int) -> Unit,
    onPlayTrack: (MediaItem) -> Unit,
    onMenu: (MediaItem) -> Unit,
    onPlayAlbum: () -> Unit,
    onMenuAlbum: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        SubTabs(listOf("艺术家", "专辑", "轨道", "流派", "播放列表"), subTabIndex, onSubTab)
        when (subTabIndex) {
            0 -> ArtistsTab(onMenuAlbum)
            1 -> AlbumsTab(onPlayAlbum, onMenuAlbum)
            2 -> TracksTab(onPlayTrack, onMenu)
            3 -> EmptyState("未找到媒体文件，请传输一些文件到您的设备，或调整您的偏好设置。", "前往媒体库偏好设置")
            else -> if (Store.playlists.isEmpty()) EmptyState("未找到播放列表。") else PlaylistGrid(Store.playlists.toList()) { }
        }
    }
}

@Composable
private fun ArtistsTab(onMenu: () -> Unit) {
    val p = LocalVlcPalette.current
    Column(Modifier.fillMaxSize()) {
        Text("#", color = p.accent, fontSize = 14.sp, modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 6.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(58.dp).clip(CircleShape).background(Color(0xFFEDEDED)), contentAlignment = Alignment.Center) {
                VlcIconView(VlcIcon.PERSON, 30.dp, p.textSecondary)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("未知艺术家", color = p.textPrimary, fontSize = 16.sp)
                Text("1 张专辑", color = p.textSecondary, fontSize = 13.sp)
            }
            IconTap(VlcIcon.MORE, 20.dp, p.textSecondary, onMenu)
        }
    }
}

@Composable
private fun AlbumsTab(onPlay: () -> Unit, onMenu: () -> Unit) {
    val p = LocalVlcPalette.current
    Column(Modifier.fillMaxSize()) {
        Text("#", color = p.accent, fontSize = 14.sp, modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 6.dp))
        Column(Modifier.padding(horizontal = 14.dp)) {
            Box(
                Modifier.width(150.dp).aspectRatio(1f).clip(RoundedCornerShape(4.dp)).background(Color(0xFFF0F0F0)),
                contentAlignment = Alignment.Center,
            ) {
                VlcIconView(VlcIcon.RING, 72.dp, Color(0xFF9E9E9E))
                Box(Modifier.align(Alignment.BottomEnd).padding(8.dp)) {
                    Box(
                        Modifier.size(30.dp).clip(CircleShape).background(Color(0xE6FFFFFF)).clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onPlay,
                        ),
                        contentAlignment = Alignment.Center,
                    ) { VlcIconView(VlcIcon.PLAY, 18.dp, Color(0xFF333333)) }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("未知专辑", color = p.textPrimary, fontSize = 16.sp)
            Text("未知艺术家", color = p.textSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            IconTap(VlcIcon.MORE, 20.dp, p.textSecondary, onMenu)
        }
    }
}

@Composable
private fun TracksTab(onPlay: (MediaItem) -> Unit, onMenu: (MediaItem) -> Unit) {
    val p = LocalVlcPalette.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 90.dp)) {
        item {
            Text("S", color = p.accent, fontSize = 14.sp, modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 6.dp))
        }
        items(Library.audios, key = { it.id }) { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onPlay(item) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFF0F0F0)), contentAlignment = Alignment.Center) {
                    VlcIconView(VlcIcon.MUSIC, 24.dp, p.textSecondary)
                }
                Spacer(Modifier.width(14.dp))
                Text(item.fileName, color = p.textPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f))
                IconTap(VlcIcon.MORE, 20.dp, p.textSecondary) { onMenu(item) }
            }
        }
    }
}

@Composable
fun PlaylistGrid(playlists: List<Playlist>, onOpen: (Playlist) -> Unit) {
    val p = LocalVlcPalette.current
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(playlists, key = { it.id }) { pl ->
            Column {
                Box(Modifier.fillMaxWidth().aspectRatio(1.95f).clip(RoundedCornerShape(6.dp)).background(Color(0xFFEDEDED))) {
                    val first = pl.itemIds.firstOrNull()?.let { Library.byId(it) }
                    if (first != null) {
                        TestPattern(first.pattern, Modifier.fillMaxSize())
                    }
                    Box(
                        Modifier.align(Alignment.Center).size(44.dp).clip(CircleShape).background(Color(0xB3000000)),
                        contentAlignment = Alignment.Center,
                    ) { VlcIconView(VlcIcon.PLAY, 24.dp, Color.White) }
                }
                Spacer(Modifier.height(6.dp))
                Text(pl.name, color = p.textPrimary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${pl.itemIds.size} 条轨道", color = p.textSecondary, fontSize = 12.sp)
            }
        }
    }
}
