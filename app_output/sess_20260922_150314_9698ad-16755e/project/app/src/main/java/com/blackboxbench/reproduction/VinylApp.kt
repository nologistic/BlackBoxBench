package com.blackboxbench.reproduction

import android.content.Context
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 主界面底部五个入口（顺序与探索结果一致）。 */
enum class MainTab(val label: String, val icon: Int) {
    SONGS("歌曲", R.drawable.ic_music_note),
    ALBUMS("专辑", R.drawable.ic_album),
    ARTISTS("艺术家", R.drawable.ic_person),
    GENRES("音乐类型", R.drawable.ic_grid),
    PLAYLISTS("播放列表", R.drawable.ic_queue_music),
}

/** 简单页面栈导航。 */
sealed class Screen {
    data object Main : Screen()
    data class AlbumDetail(val album: String) : Screen()
    data class ArtistDetail(val artist: String) : Screen()
    data class GenreDetail(val genre: String) : Screen()
    data class PlaylistDetail(val playlistId: Long) : Screen()
    data object Search : Screen()
    data object Settings : Screen()
    data object Equalizer : Screen()
    data object About : Screen()
    data object NowPlaying : Screen()
    data class SongPicker(val playlistId: Long) : Screen()
}

class AppModel(context: Context) {
    val baseLibrary: MutableList<Song> = MusicLibrary.load(context)
    val playlists = mutableStateOf(Store.loadPlaylists(context))
    val favorites = mutableStateOf(Store.loadFavorites(context))
    val settings = mutableStateOf(Store.loadSettings(context))
    val stack = mutableStateOf(listOf<Screen>(Screen.Main))
    val tab = mutableStateOf(MainTab.SONGS)
    val libraryVersion = androidx.compose.runtime.mutableIntStateOf(0)
    val player: PlayerEngine = PlayerEngine(context, baseLibrary)
    private val ctx = context
    private val tagOverrides = Store.loadTagOverrides(context)

    fun bumpLibrary() { libraryVersion.intValue = libraryVersion.intValue + 1 }

    /** 应用标签编辑覆盖后的曲库快照；读取 libraryVersion 以便保存后刷新聚合视图。 */
    val songs: List<Song>
        get() {
            libraryVersion.intValue
            return baseLibrary.map { s ->
            val o = tagOverrides[s.id]
            if (o == null) s.copy() else s.copy(
                title = o.optString("title", s.title),
                artist = o.optString("artist", s.artist),
                album = o.optString("album", s.album),
                year = o.optInt("year", s.year),
                genre = o.optString("genre", s.genre),
                trackNo = o.optInt("trackNo", s.trackNo),
            )
            }
        }

    fun songById(id: Long): Song? = songs.firstOrNull { it.id == id }

    fun push(s: Screen) { stack.value = stack.value + s }
    fun pop() { if (stack.value.size > 1) stack.value = stack.value.dropLast(1) }

    fun saveTag(songId: Long, title: String, artist: String, album: String, year: Int, genre: String, trackNo: Int) {
        val o = org.json.JSONObject()
        o.put("title", title); o.put("artist", artist); o.put("album", album)
        o.put("year", year); o.put("genre", genre); o.put("trackNo", trackNo)
        tagOverrides[songId] = o
        Store.saveTagOverrides(ctx, tagOverrides)
        bumpLibrary()
    }

    fun createPlaylist(name: String): Long {
        val id = (playlists.value.maxOfOrNull { it.id } ?: 0L) + 1L
        playlists.value = (playlists.value + Playlist(id, name)).toMutableList()
        Store.savePlaylists(ctx, playlists.value)
        return id
    }

    fun renamePlaylist(id: Long, name: String) {
        playlists.value.firstOrNull { it.id == id }?.name = name
        playlists.value = playlists.value.toList().toMutableList()
        Store.savePlaylists(ctx, playlists.value)
    }

    fun deletePlaylist(id: Long) {
        playlists.value = playlists.value.filterNot { it.id == id }.toMutableList()
        Store.savePlaylists(ctx, playlists.value)
    }

    fun addToPlaylist(id: Long, songIds: List<Long>) {
        playlists.value.firstOrNull { it.id == id }?.let { p ->
            songIds.forEach { if (!p.songIds.contains(it)) p.songIds.add(it) }
        }
        playlists.value = playlists.value.toList().toMutableList()
        Store.savePlaylists(ctx, playlists.value)
    }

    fun removeFromPlaylist(id: Long, songId: Long) {
        playlists.value.firstOrNull { it.id == id }?.songIds?.remove(songId)
        playlists.value = playlists.value.toList().toMutableList()
        Store.savePlaylists(ctx, playlists.value)
    }

    fun toggleFavorite(songId: Long) {
        if (favorites.value.contains(songId)) favorites.value.remove(songId)
        else favorites.value.add(songId)
        favorites.value = favorites.value.toMutableSet()
        Store.saveFavorites(ctx, favorites.value)
    }

    fun saveSettings() = Store.saveSettings(ctx, settings.value)
}

@Composable
fun VinylApp(model: AppModel, onboarding: Boolean, finishOnboarding: () -> Unit) {
    if (onboarding) { Onboarding(onFinished = finishOnboarding); return }
    val screen = model.stack.value.last()
    MaterialTheme(colorScheme = VinylColors()) {
        when (screen) {
            is Screen.Main -> MainScaffold(model)
            is Screen.AlbumDetail -> AlbumDetailPage(model, screen.album)
            is Screen.ArtistDetail -> ArtistDetailPage(model, screen.artist)
            is Screen.GenreDetail -> GenreDetailPage(model, screen.genre)
            is Screen.PlaylistDetail -> PlaylistDetailPage(model, screen.playlistId)
            is Screen.Search -> SearchPage(model)
            is Screen.Settings -> SettingsPage(model)
            is Screen.Equalizer -> EqualizerPage(model)
            is Screen.About -> AboutPage(model)
            is Screen.NowPlaying -> NowPlayingPage(model)
            is Screen.SongPicker -> SongPickerPage(model, screen.playlistId)
        }
    }
}

@Composable
fun AppTopBar(title: String, model: AppModel, showHamburger: Boolean = true, showActions: Boolean = true) {
    var menuOpen by remember { mutableStateOf(false) }
    var mainMenuOpen by remember { mutableStateOf(false) }
    Surface(shadowElevation = 2.dp, color = Color(0xFFF9F9FF)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
        ) {
            if (showHamburger) {
                IconButton(onClick = { mainMenuOpen = true }) {
                    Icon(Icons.Filled.Menu, contentDescription = "菜单", tint = Color(0xFF37474F))
                }
                DropdownMenu(expanded = mainMenuOpen, onDismissRequest = { mainMenuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("设置") },
                        leadingIcon = { Icon(Icons.Filled.Settings, null) },
                        onClick = { mainMenuOpen = false; model.push(Screen.Settings) },
                    )
                    DropdownMenuItem(
                        text = { Text("均衡器") },
                        leadingIcon = {
                            Icon(painterResource(R.drawable.ic_equalizer), null)
                        },
                        onClick = { mainMenuOpen = false; model.push(Screen.Equalizer) },
                    )
                    DropdownMenuItem(
                        text = { Text("关于") },
                        leadingIcon = { Icon(Icons.Filled.Info, null) },
                        onClick = { mainMenuOpen = false; model.push(Screen.About) },
                    )
                    DropdownMenuItem(
                        text = { Text("退出") },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_pause), null) },
                        onClick = { mainMenuOpen = false },
                    )
                }
            }
            Text(
                title, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color(0xFF263238),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = if (showHamburger) 4.dp else 16.dp),
            )
            if (showActions) {
                IconButton(onClick = { model.push(Screen.Search) }) {
                    Icon(Icons.Filled.Search, contentDescription = "搜索", tint = Color(0xFF37474F))
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "更多", tint = Color(0xFF37474F))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("设置") },
                            leadingIcon = { Icon(Icons.Filled.Settings, null) },
                            onClick = { menuOpen = false; model.push(Screen.Settings) },
                        )
                        DropdownMenuItem(
                            text = { Text("均衡器") },
                            leadingIcon = {
                                Icon(painterResource(R.drawable.ic_equalizer), null)
                            },
                            onClick = { menuOpen = false; model.push(Screen.Equalizer) },
                        )
                        DropdownMenuItem(
                            text = { Text("关于") },
                            leadingIcon = { Icon(Icons.Filled.Info, null) },
                            onClick = { menuOpen = false; model.push(Screen.About) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainScaffold(model: AppModel) {
    Scaffold(
        topBar = { AppTopBar("Vinyl Music Player", model) },
        containerColor = Color(0xFFFAFAFA),
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            Box(Modifier.weight(1f)) {
                when (model.tab.value) {
                    MainTab.SONGS -> SongsTab(model)
                    MainTab.ALBUMS -> AlbumsTab(model)
                    MainTab.ARTISTS -> ArtistsTab(model)
                    MainTab.GENRES -> GenresTab(model)
                    MainTab.PLAYLISTS -> PlaylistsTab(model)
                }
            }
            MiniPlayer(model)
            BottomTabs(model)
        }
    }
}

@Composable
private fun BottomTabs(model: AppModel) {
    Surface(color = Color(0xFFF9F9FF), shadowElevation = 8.dp) {
        Row(Modifier.fillMaxWidth().height(64.dp)) {
            MainTab.entries.forEach { t ->
                val active = model.tab.value == t
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f).fillMaxSize()
                        .clickable { model.tab.value = t },
                ) {
                    Icon(
                        painterResource(t.icon), contentDescription = t.label,
                        tint = if (active) MaterialTheme.colorScheme.primary else Color(0xFF90A4AE),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        t.label, fontSize = 11.sp,
                        color = if (active) MaterialTheme.colorScheme.primary else Color(0xFF90A4AE),
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
fun SongsTab(model: AppModel) {
    val songs = model.songs.sortedBy { it.title }
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Text(
                "全部歌曲 · ${songs.size} 首",
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

@Composable
fun AlbumsTab(model: AppModel) {
    val albums = MusicLibrary.albumsOf(model.songs)
    LazyColumn(Modifier.fillMaxSize()) {
        items(albums, key = { it.first }) { (album, list) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { model.push(Screen.AlbumDetail(album)) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                CoverImage(album = album, size = 64)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(album, fontSize = 17.sp, color = Color(0xFF212121), fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${list.first().artist} · ${list.size} 首 · ${list.first().year}",
                        fontSize = 13.sp, color = Color(0xFF757575),
                    )
                }
            }
        }
    }
}

@Composable
fun ArtistsTab(model: AppModel) {
    val artists = MusicLibrary.artistsOf(model.songs)
    LazyColumn(Modifier.fillMaxSize()) {
        items(artists, key = { it.first }) { (artist, list) ->
            SongRow(
                title = artist,
                subtitle = "${list.distinctBy { it.album }.size} 张专辑 · ${list.size} 首歌曲",
                onClick = { model.push(Screen.ArtistDetail(artist)) },
            )
        }
    }
}

@Composable
fun GenresTab(model: AppModel) {
    val genres = MusicLibrary.genresOf(model.songs)
    LazyColumn(Modifier.fillMaxSize()) {
        items(genres, key = { it.first }) { (genre, list) ->
            SongRow(
                title = genre,
                subtitle = "${list.size} 首歌曲",
                onClick = { model.push(Screen.GenreDetail(genre)) },
            )
        }
    }
}

@Composable
fun PlaylistsTab(model: AppModel) {
    val playlists = model.playlists.value.sortedBy { it.name }
    var showCreate by remember { mutableStateOf(false) }
    if (playlists.isEmpty()) {
        Column(Modifier.fillMaxSize()) {
            EmptyState(main = "没有播放列表", sub = "创建一个播放列表，收藏你喜欢的歌")
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = { showCreate = true },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) { Text("新建播放列表") }
        }
        if (showCreate) {
            NewPlaylistDialog(
                onDismiss = { showCreate = false },
                onConfirm = { name ->
                    showCreate = false
                    val id = model.createPlaylist(name)
                    model.push(Screen.PlaylistDetail(id))
                },
            )
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(playlists, key = { it.id }) { p ->
            val count = p.songIds.size
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { model.push(Screen.PlaylistDetail(p.id)) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Icon(
                    painterResource(R.drawable.ic_queue_music), null,
                    tint = Color(0xFF00897B), modifier = Modifier.size(40.dp),
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(p.name, fontSize = 17.sp, color = Color(0xFF212121))
                    Spacer(Modifier.height(2.dp))
                    Text("$count 首歌曲", fontSize = 13.sp, color = Color(0xFF757575))
                }
            }
        }
    }
}

@Composable
fun MiniPlayer(model: AppModel) {
    val song = model.player.current() ?: return
    Surface(shadowElevation = 8.dp, color = Color(0xFFECEFF1)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(64.dp)
                .clickable { model.push(Screen.NowPlaying) }.padding(horizontal = 12.dp),
        ) {
            VinylDisc(album = song.album, sizeDp = 44, spinning = model.player.playing.value)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    song.title, fontSize = 15.sp, color = Color(0xFF212121),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    song.artist, fontSize = 12.sp, color = Color(0xFF757575),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = { model.player.togglePlayPause() }) {
                Icon(
                    painterResource(
                        if (model.player.playing.value) R.drawable.ic_pause else R.drawable.ic_play_arrow
                    ),
                    contentDescription = "播放或暂停",
                    tint = Color(0xFF00897B),
                )
            }
        }
    }
}
