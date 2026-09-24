package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ------------------------------------------------------------------ palette

fun Color.lighten(f: Float): Color = Color(
    red = red + (1f - red) * f,
    green = green + (1f - green) * f,
    blue = blue + (1f - blue) * f,
    alpha = alpha
)

@Composable
fun vinylScheme(primaryArgb: Int, accentArgb: Int, dark: Boolean) = if (dark) {
    darkColorScheme(
        primary = Color(primaryArgb).lighten(0.35f),
        secondary = Color(accentArgb),
        background = Color(0xFF141414),
        surface = Color(0xFF141414),
        surfaceVariant = Color(0xFF1E1E1E),
        onBackground = Color(0xFFEDEDED),
        onSurface = Color(0xFFEDEDED),
        onSurfaceVariant = Color(0xFFB0B0B0),
        outline = Color(0xFF3A3A3A),
    )
} else {
    lightColorScheme(
        primary = Color(primaryArgb),
        secondary = Color(accentArgb),
        background = Color(0xFFFFFFFF),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFF2F2F2),
        onBackground = Color(0xFF1B1B1B),
        onSurface = Color(0xFF1B1B1B),
        onSurfaceVariant = Color(0xFF6E6E6E),
        outline = Color(0xFFDDDDDD),
    )
}

// ------------------------------------------------------------------ top bar

@Composable
fun TopBarShell(
    primary: Color,
    content: @Composable () -> Unit,
) {
    Surface(color = primary, contentColor = Color.White) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.height(24.dp))
            content()
        }
    }
}

@Composable
fun TopBarTitle(
    title: String,
    primary: Color,
    onMenu: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    onSearch: (() -> Unit)? = null,
    onOverflow: (() -> Unit)? = null,
    overflowContent: @Composable ((() -> Unit)) -> Unit = {},
    leadingCustom: (@Composable () -> Unit)? = null,
    trailingCustom: (@Composable () -> Unit)? = null,
) {
    var menuOpen by remember { mutableStateOf(false) }
    TopBarShell(primary) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingCustom != null) {
                leadingCustom()
            } else if (onBack != null) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) { BackGlyph(Color.White) }
            } else if (onMenu != null) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clickable { onMenu() },
                    contentAlignment = Alignment.Center
                ) { MenuGlyph(Color.White) }
            } else {
                Spacer(Modifier.width(12.dp))
            }
            Text(
                title,
                color = Color.White,
                fontSize = 21.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            )
            if (trailingCustom != null) trailingCustom()
            if (onSearch != null) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clickable { onSearch() },
                    contentAlignment = Alignment.Center
                ) { SearchGlyph(Color.White) }
            }
            if (onOverflow != null) {
                Box {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clickable { menuOpen = true },
                        contentAlignment = Alignment.Center
                    ) { DotsGlyph(Color.White) }
                    if (menuOpen) overflowContent { menuOpen = false }
                }
            }
        }
    }
}

@Composable
fun LibraryTabRow(
    tabs: List<String>,
    selected: Int,
    accent: Color,
    background: Color,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(background),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEachIndexed { index, label ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        label,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = if (index == selected) FontWeight.Bold else FontWeight.Normal
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .height(3.dp)
                            .width(if (index == selected) 56.dp else 0.dp)
                            .background(accent)
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------- drawer

@Composable
fun DrawerHost(
    open: Boolean,
    state: AppState,
    onClose: () -> Unit,
    onNavigate: (String) -> Unit,
    content: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        content()
        if (open) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000))
                    .clickable { onClose() }
            )
            Surface(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Column(Modifier.fillMaxHeight().verticalScroll(rememberScrollState())) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .background(Color(0xFF1E1E1E)),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        VinylGlyph(Color(0xFF9E9E9E), 92.dp)
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "Vinyl Music Player",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    DrawerItem("媒体库", MaterialTheme.colorScheme.onSurface) {
                        onNavigate("library"); onClose()
                    }
                    DrawerItem("文件夹", MaterialTheme.colorScheme.onSurface) {
                        onNavigate("folders"); onClose()
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outline)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "  播放列表",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                    )
                    state.playlists.filter { !it.isAuto }.forEach { p ->
                        DrawerItem(p.name, MaterialTheme.colorScheme.onSurface) {
                            state.openPlaylistId = p.id
                            onNavigate("playlist"); onClose()
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outline)
                    )
                    Spacer(Modifier.height(8.dp))
                    DrawerItem("设置", MaterialTheme.colorScheme.onSurface) {
                        onNavigate("settings"); onClose()
                    }
                    DrawerItem("关于", MaterialTheme.colorScheme.onSurface) {
                        onNavigate("about"); onClose()
                    }
                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
private fun DrawerItem(label: String, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = color, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

// ------------------------------------------------------------------- menus

data class MenuEntry(
    val label: String,
    val danger: Boolean = false,
    val checked: Boolean? = null,
    val submenu: List<String>? = null,
    val onSubmenuSelect: (Int) -> Unit = {},
    val onClick: () -> Unit = {},
)

@Composable
fun OverflowMenu(
    expanded: Boolean,
    entries: List<MenuEntry>,
    onDismiss: () -> Unit,
) {
    var submenu by remember { mutableStateOf<MenuEntry?>(null) }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { submenu = null; onDismiss() },
        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
    ) {
        val current = submenu
        if (current != null) {
            DropdownMenuItem(
                text = { Text("← ${current.label}", fontSize = 15.sp) },
                onClick = { submenu = null }
            )
            current.submenu!!.forEachIndexed { optionIndex, option ->
                DropdownMenuItem(
                    text = { Text(option, fontSize = 15.sp) },
                    onClick = {
                        current.onSubmenuSelect(optionIndex)
                        submenu = null
                        onDismiss()
                    }
                )
            }
        } else {
            entries.forEach { entry ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (entry.checked != null) {
                                Box(
                                    Modifier.size(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (entry.checked) CheckGlyph(MaterialTheme.colorScheme.onSurface, 18.dp)
                                }
                                Spacer(Modifier.width(10.dp))
                            }
                            Text(
                                entry.label,
                                fontSize = 15.sp,
                                color = if (entry.danger) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    onClick = {
                        if (entry.submenu != null) {
                            submenu = entry
                        } else {
                            entry.onClick()
                            onDismiss()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun OverflowButton(
    entries: () -> List<MenuEntry>,
    tint: Color,
    size: Int = 26,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        Box(
            Modifier
                .size(48.dp)
                .clickable { open = true },
            contentAlignment = Alignment.Center
        ) { DotsGlyph(tint, size.dp) }
        OverflowMenu(open, entries(), onDismiss = { open = false })
    }
}

// --------------------------------------------------------------- list items

@Composable
fun CoverArt(size: Int, seed: String, corner: Int = 0, showVinyl: Boolean = true) {
    val bg = remember(seed) {
        when (seed.hashCode().mod(4)) {
            0 -> Color(0xFF1E1E1E)
            1 -> Color(0xFF232323)
            2 -> Color(0xFF191919)
            else -> Color(0xFF2A2A2A)
        }
    }
    Box(
        Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(corner.dp))
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        if (showVinyl) VinylGlyph(Color(0xFF8A8A8A), (size * 0.78f).dp)
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TrackRow(
    track: Track,
    state: AppState,
    leading: (@Composable () -> Unit)? = null,
    index: String? = null,
    showNote: Boolean,
    selected: Boolean,
    onOverflow: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
) {
    val accent = Color(state.accentColor)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(
                when {
                    selected -> MaterialTheme.colorScheme.surfaceVariant
                    else -> Color.Transparent
                }
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (index != null) {
            Text(
                index,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(28.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        if (leading != null) {
            leading()
            Spacer(Modifier.width(12.dp))
        } else {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                if (showNote) NoteGlyph(accent, 26.dp, Modifier)
                else CoverArt(48, track.album)
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                buildString {
                    append(track.artist)
                    append(" · ")
                    append(track.album)
                },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        OverflowButton(entries = { onOverflowEntries(track, state) }, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

fun onOverflowEntries(track: Track, state: AppState): List<MenuEntry> = listOf(
    MenuEntry("作为下一首播放") { state.playNext(track.id); state.toast("已设为下一首播放") },
    MenuEntry("加入播放队列") {
        if (state.queue.isEmpty()) {
            state.playQueue(listOf(track.id), 0); state.playing = false
        } else state.addToQueue(track.id)
        state.toast("已加入播放队列")
    },
    MenuEntry("加入播放列表…") {
        state.dialog = DialogKind.ADD_TO_PLAYLIST
        state.selectedIds.clear(); state.selectedIds.add(track.id)
    },
    MenuEntry("查看专辑") {
        state.openAlbum = track.album; state.screen = Screen.ALBUM_DETAIL
    },
    MenuEntry("查看艺术家") {
        state.openArtist = track.artist; state.screen = Screen.ARTIST_DETAIL
    },
    MenuEntry("分享") { state.toast("已分享 ${track.title}") },
    MenuEntry("音乐标签编辑器") {
        state.tagEditorTrackId = track.id; state.screen = Screen.TAG_EDITOR
    },
    MenuEntry("详情") {
        state.detailsTrackId = track.id; state.dialog = DialogKind.DETAILS
    },
    MenuEntry("设为铃声") { state.toast("已设为铃声") },
    MenuEntry("从设备中删除", danger = true) { state.toast("已从设备中删除 ${track.title}") },
)

@Composable
fun SectionHeader(text: String) {
    Text(
        text,
        color = Color(0xFFC2185B),
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 6.dp)
    )
}

@Composable
fun SettingRow(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (trailing != null) trailing()
    }
}

@Composable
fun SwitchPill(checked: Boolean, accent: Color, onToggle: (Boolean) -> Unit) {
    Box(
        Modifier
            .width(52.dp)
            .height(30.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(if (checked) accent else MaterialTheme.colorScheme.outline)
            .clickable { onToggle(!checked) },
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            Modifier
                .padding(horizontal = 4.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
fun Divider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline)
    )
}

// -------------------------------------------------------------- mini player

@Composable
fun MiniPlayer(state: AppState, onExpand: () -> Unit) {
    val track = state.currentTrack ?: return
    val accent = Color(state.accentColor)
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shadowElevation = 6.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(58.dp)
                    .clickable { onExpand() },
                contentAlignment = Alignment.Center
            ) { ChevronUpGlyph(MaterialTheme.colorScheme.onSurface) }
            Text(
                track.title,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onExpand() }
            )
            Box(
                Modifier
                    .size(58.dp)
                    .clickable { state.togglePlay() },
                contentAlignment = Alignment.Center
            ) {
                if (state.playing) PauseGlyph(MaterialTheme.colorScheme.onSurface, 26.dp)
                else PlayGlyph(MaterialTheme.colorScheme.onSurface, 26.dp)
            }
        }
    }
}
