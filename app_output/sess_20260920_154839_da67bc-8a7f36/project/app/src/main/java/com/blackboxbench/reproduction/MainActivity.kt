
package com.blackboxbench.reproduction

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.json.JSONObject
import kotlin.math.abs

private val VinylBlue = Color(0xFF3F51B5)
private val VinylPink = Color(0xFFFF006E)
private val Pale = Color(0xFFF5F5F5)
private val coverColors = listOf(
    Color(0xFF3949AB), Color(0xFF00897B), Color(0xFFE65100),
    Color(0xFF6A1B9A), Color(0xFF546E7A), Color(0xFFC62828)
)

data class Song(
    val id: Int,
    val title: String,
    val artist: String,
    val album: String,
    val no: Int,
    val duration: Int,
    val year: Int,
    val genre: String
)

private fun loadSongs(context: Context): List<Song> = runCatching {
    val root = JSONObject(AssetStore.readText(context, "music_library.json"))
    val result = mutableListOf<Song>()
    var id = 0
    val artists = root.getJSONArray("artists")
    for (a in 0 until artists.length()) {
        val artist = artists.getJSONObject(a)
        val name = artist.getString("name")
        val albums = artist.getJSONArray("albums")
        for (b in 0 until albums.length()) {
            val album = albums.getJSONObject(b)
            val tracks = album.getJSONArray("tracks")
            for (t in 0 until tracks.length()) {
                val track = tracks.getJSONObject(t)
                result += Song(
                    id++, track.getString("title"), name, album.getString("title"),
                    track.getInt("no"), track.getInt("duration"),
                    album.getInt("year"), album.getString("genre")
                )
            }
        }
    }
    result
}.getOrElse {
    listOf(Song(0, "起飞检查单", "夜航电台", "北纬四十", 1, 214, 2024, "合成器流行"))
}

private fun timeText(seconds: Int) = "%d:%02d".format(seconds / 60, seconds % 60)
private fun totalText(songs: List<Song>) = timeText(songs.sumOf { it.duration })

class MainActivity : ComponentActivity() {
    private var mediaPlayer: MediaPlayer? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaPlayer = MediaPlayer.create(this, R.raw.ambient).apply { isLooping = true }
        setContent { ReproducedApp(mediaPlayer) }
    }
    override fun onDestroy() {
        mediaPlayer?.release()
        super.onDestroy()
    }
}

@Composable
fun ReproducedApp(mediaPlayer: MediaPlayer? = null) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("vinyl_state", Context.MODE_PRIVATE) }
    var introDone by remember { mutableStateOf(prefs.getBoolean("intro_done", false)) }
    BenchmarkAppTheme {
        if (!introDone) {
            Onboarding {
                prefs.edit().putBoolean("intro_done", true).apply()
                introDone = true
            }
        } else VinylApp(loadSongs(context), prefs, mediaPlayer)
    }
}

@Composable
private fun Onboarding(onDone: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    var drag by remember { mutableFloatStateOf(0f) }
    val titles = listOf("Vinyl Music Player", "播放队列", "播放队列")
    val subtitles = listOf(
        "欢迎使用一个轻巧、专注于本地音乐的播放器。",
        "在播放器中向上滑动歌曲卡片，即可展开当前播放队列。",
        "通过拖动歌曲名前面的序列号来调整播放队列的顺序。"
    )
    val colors = listOf(Color(0xFF90CAF9), Color(0xFF7E57C2), VinylBlue)
    Column(
        Modifier.fillMaxSize().background(colors[page]).statusBarsPadding().pointerInput(page) {
            detectHorizontalDragGestures(
                onDragStart = { drag = 0f },
                onDragEnd = {
                    if (drag < -120 && page < 2) page++
                    if (drag > 120 && page > 0) page--
                }
            ) { _, amount -> drag += amount }
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(90.dp))
        OnboardingPicture(page)
        Spacer(Modifier.height(38.dp))
        Text(titles[page], color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        Text(
            subtitles[page], color = Color.White.copy(alpha = .88f), fontSize = 18.sp,
            modifier = Modifier.padding(horizontal = 36.dp), lineHeight = 28.sp
        )
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onDone,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = VinylBlue),
            shape = RoundedCornerShape(3.dp)
        ) { Text("GET STARTED", fontSize = 18.sp) }
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(3) { index ->
                Box(
                    Modifier.size(11.dp).clip(CircleShape)
                        .background(if (index == page) Color.White else VinylBlue.copy(alpha = .45f))
                        .clickable { page = index }
                )
            }
        }
        Spacer(Modifier.height(38.dp))
    }
}

@Composable
private fun OnboardingPicture(page: Int) {
    Box(
        Modifier.size(width = 300.dp, height = 360.dp).background(Color.White.copy(alpha = .14f)),
        contentAlignment = Alignment.Center
    ) {
        when (page) {
            0 -> VinylDisc(220.dp, Color(0xFFEF476F), 0f)
            1 -> Column(Modifier.fillMaxWidth(.82f).background(Color.White)) {
                repeat(4) { i ->
                    Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}", color = Color.DarkGray)
                        Spacer(Modifier.width(16.dp))
                        Text(listOf("起飞检查单", "跑道灯", "云层之上", "夜间巡航")[i])
                    }
                    HorizontalDivider(color = Color.LightGray)
                }
            }
            else -> Column(Modifier.fillMaxWidth(.82f).background(Color.White)) {
                repeat(6) { i ->
                    Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}", color = Color.DarkGray)
                        Spacer(Modifier.width(22.dp))
                        HorizontalDivider(Modifier.weight(1f), color = Color(0xFFE5E5E5))
                    }
                }
            }
        }
    }
}

@Composable
private fun VinylApp(
    songs: List<Song>,
    prefs: android.content.SharedPreferences,
    mediaPlayer: MediaPlayer?
) {
    val tabs = listOf("歌曲", "专辑", "艺术家", "音乐类型", "播放列表")
    var tab by remember { mutableIntStateOf(prefs.getInt("tab", 0).coerceIn(0, 4)) }
    var screen by remember { mutableStateOf(prefs.getString("screen", "library") ?: "library") }
    var selectedKey by remember { mutableStateOf("") }
    var drawerOpen by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var topMenu by remember { mutableStateOf(false) }
    var songMenu by remember { mutableStateOf<Song?>(null) }
    var playlistChooser by remember { mutableStateOf<Song?>(null) }
    var createPlaylist by remember { mutableStateOf(false) }
    var playlistName by remember { mutableStateOf("") }
    var current by remember {
        mutableStateOf(prefs.getInt("current", -1).takeIf { it in songs.indices }?.let { songs[it] })
    }
    var queue by remember { mutableStateOf(if (current == null) emptyList() else listOf(current!!)) }
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(prefs.getInt("progress", 0)) }
    var repeatMode by remember { mutableIntStateOf(prefs.getInt("repeat", 0)) }
    var shuffle by remember { mutableStateOf(prefs.getBoolean("shuffle", false)) }
    var favorites by remember {
        mutableStateOf(prefs.getString("favorites", "")!!.split(",").mapNotNull { it.toIntOrNull() }.toSet())
    }
    val defaultLists = remember(songs) {
        mapOf(
            "晨间通勤" to songs.filter { it.title in setOf("起飞检查单", "跑道灯", "早春序曲", "开机音", "夜间巡航", "橡树下的信") },
            "深夜编码" to songs.filter { it.title in setOf("载波", "快门开启", "星轨", "静默频道", "隐藏关卡") }
        )
    }
    var customLists by remember {
        val names = prefs.getString("playlist_names", "")!!.split("|").filter { it.isNotBlank() }
        mutableStateOf(names.associateWith { name ->
            prefs.getString("playlist_$name", "")!!.split(",")
                .mapNotNull { id -> id.toIntOrNull()?.let { n -> songs.find { it.id == n } } }
        })
    }

    fun saveFavorites(set: Set<Int>) {
        favorites = set
        prefs.edit().putString("favorites", set.joinToString(",")).apply()
    }
    fun saveLists(value: Map<String, List<Song>>) {
        customLists = value
        val editor = prefs.edit().putString("playlist_names", value.keys.joinToString("|"))
        value.forEach { (name, list) -> editor.putString("playlist_$name", list.joinToString(",") { it.id.toString() }) }
        editor.apply()
    }
    fun playList(list: List<Song>, selected: Song = list.first()) {
        queue = if (shuffle) list.shuffled() else list
        current = selected
        progress = 0
        isPlaying = true
        prefs.edit().putInt("current", selected.id).putInt("progress", 0).apply()
        runCatching { mediaPlayer?.seekTo(0); mediaPlayer?.start() }
    }
    fun togglePlay() {
        if (current == null && songs.isNotEmpty()) playList(songs, songs.first())
        else {
            isPlaying = !isPlaying
            runCatching { if (isPlaying) mediaPlayer?.start() else mediaPlayer?.pause() }
        }
    }
    fun next(delta: Int) {
        if (queue.isEmpty()) return
        val index = queue.indexOfFirst { it.id == current?.id }.coerceAtLeast(0)
        var newIndex = index + delta
        if (newIndex !in queue.indices) newIndex = if (repeatMode == 1) (newIndex + queue.size) % queue.size else index
        current = queue[newIndex]
        progress = 0
        prefs.edit().putInt("current", current!!.id).putInt("progress", 0).apply()
    }

    LaunchedEffect(isPlaying, current?.id, progress) {
        if (isPlaying && current != null) {
            delay(1000)
            if (progress + 1 >= current!!.duration) {
                if (repeatMode == 2) progress = 0 else next(1)
            } else progress++
            prefs.edit().putInt("progress", progress).apply()
        }
    }

    BackHandler {
        when {
            drawerOpen -> drawerOpen = false
            screen != "library" -> screen = "library"
            searching -> { searching = false; query = "" }
        }
    }

    LaunchedEffect(screen) {
        if (screen in setOf("library", "folders", "settings", "about")) {
            prefs.edit().putString("screen", screen).apply()
        }
    }

    Box(
        Modifier.fillMaxSize()
            .background(if (screen == "player") Color(0xFF1E1E1E) else VinylBlue)
            .statusBarsPadding()
    ) {
        when (screen) {
            "player" -> PlayerPage(
                current, queue, isPlaying, progress, repeatMode, shuffle, favorites,
                onClose = { screen = "library" },
                onToggle = { togglePlay() },
                onNext = { next(1) },
                onPrevious = { next(-1) },
                onSeek = { progress = it },
                onRepeat = { repeatMode = (repeatMode + 1) % 3; prefs.edit().putInt("repeat", repeatMode).apply() },
                onShuffle = { shuffle = !shuffle; queue = queue.shuffled(); prefs.edit().putBoolean("shuffle", shuffle).apply() },
                onFavorite = { current?.let { s -> saveFavorites(if (s.id in favorites) favorites - s.id else favorites + s.id) } },
                onSongMenu = { songMenu = it }
            )
            "album" -> CollectionDetail(
                title = selectedKey,
                subtitle = songs.firstOrNull { it.album == selectedKey }?.artist.orEmpty(),
                songs = songs.filter { it.album == selectedKey }.sortedBy { it.no },
                onBack = { screen = "library" }, onPlay = { list, s -> playList(list, s) },
                onMenu = { songMenu = it }
            )
            "artist" -> CollectionDetail(
                title = selectedKey, subtitle = "${songs.count { it.artist == selectedKey }} 首歌曲",
                songs = songs.filter { it.artist == selectedKey }.sortedWith(compareBy({ it.album }, { it.no })),
                onBack = { screen = "library" }, onPlay = { list, s -> playList(list, s) },
                onMenu = { songMenu = it }
            )
            "genre" -> CollectionDetail(
                title = selectedKey, subtitle = "${songs.count { it.genre == selectedKey }} 首歌曲",
                songs = songs.filter { it.genre == selectedKey }.sortedBy { it.title },
                onBack = { screen = "library" }, onPlay = { list, s -> playList(list, s) },
                onMenu = { songMenu = it }
            )
            "playlist" -> {
                val list = when (selectedKey) {
                    "收藏夹" -> songs.filter { it.id in favorites }
                    else -> customLists[selectedKey] ?: defaultLists[selectedKey].orEmpty()
                }
                PlaylistDetail(selectedKey, list, onBack = { screen = "library" },
                    onPlay = { playList(list, it) }, onMenu = { songMenu = it })
            }
            "folders" -> FolderPage(songs, current, isPlaying, onBack = { screen = "library" },
                onPlay = { playList(songs, it) }, onMini = { screen = "player" }, onMenu = { songMenu = it })
            "settings" -> SettingsPage(onBack = { screen = "library" })
            "about" -> AboutPage(onBack = { screen = "library" })
            else -> LibraryPage(
                songs, tabs, tab, query, searching, topMenu, current, isPlaying,
                favorites, defaultLists + customLists,
                onTab = { tab = it; prefs.edit().putInt("tab", it).apply() },
                onDrawer = { drawerOpen = true },
                onSearch = { searching = !searching; if (!searching) query = "" },
                onQuery = { query = it },
                onTopMenu = { topMenu = it },
                onPlay = { list, song -> playList(list, song) },
                onSongMenu = { songMenu = it },
                onOpenPlayer = { screen = "player" },
                onToggle = { togglePlay() },
                onAlbum = { selectedKey = it; screen = "album" },
                onArtist = { selectedKey = it; screen = "artist" },
                onGenre = { selectedKey = it; screen = "genre" },
                onPlaylist = { selectedKey = it; screen = "playlist" },
                onCreatePlaylist = { createPlaylist = true }
            )
        }

        if (drawerOpen) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .55f)).clickable { drawerOpen = false })
            NavigationDrawer(
                current, onClose = { drawerOpen = false },
                onLibrary = { drawerOpen = false; screen = "library" },
                onFolders = { drawerOpen = false; screen = "folders" },
                onSettings = { drawerOpen = false; screen = "settings" },
                onAbout = { drawerOpen = false; screen = "about" }
            )
        }
    }

    songMenu?.let { song ->
        SongActionsDialog(song,
            onDismiss = { songMenu = null },
            onPlayNext = {
                queue = listOfNotNull(current, song) + queue.filter { it.id != current?.id && it.id != song.id }
                songMenu = null
            },
            onAddQueue = { queue = queue + song; songMenu = null },
            onPlaylist = { songMenu = null; playlistChooser = song },
            onAlbum = { selectedKey = song.album; screen = "album"; songMenu = null },
            onArtist = { selectedKey = song.artist; screen = "artist"; songMenu = null },
            onFavorite = { saveFavorites(if (song.id in favorites) favorites - song.id else favorites + song.id); songMenu = null }
        )
    }

    playlistChooser?.let { song ->
        PlaylistChooserDialog(
            names = listOf("收藏夹") + defaultLists.keys + customLists.keys,
            onDismiss = { playlistChooser = null },
            onNew = { playlistChooser = null; createPlaylist = true },
            onChoose = { name ->
                if (name == "收藏夹") saveFavorites(favorites + song.id)
                else {
                    val old = customLists[name] ?: defaultLists[name].orEmpty()
                    saveLists(customLists + (name to (old + song).distinctBy { it.id }))
                }
                playlistChooser = null
            }
        )
    }

    if (createPlaylist) {
        AlertDialog(
            onDismissRequest = { createPlaylist = false },
            title = { Text("创建播放列表") },
            text = { OutlinedTextField(value = playlistName, onValueChange = { playlistName = it }, label = { Text("名称") }, singleLine = true) },
            dismissButton = { TextButton(onClick = { createPlaylist = false }) { Text("取消") } },
            confirmButton = {
                TextButton(
                    enabled = playlistName.isNotBlank(),
                    onClick = {
                        saveLists(customLists + (playlistName.trim() to emptyList()))
                        playlistName = ""
                        createPlaylist = false
                    }
                ) { Text("创建") }
            }
        )
    }
}

@Composable
private fun LibraryPage(
    songs: List<Song>, tabs: List<String>, tab: Int, query: String, searching: Boolean,
    topMenu: Boolean, current: Song?, isPlaying: Boolean, favorites: Set<Int>,
    playlists: Map<String, List<Song>>, onTab: (Int) -> Unit, onDrawer: () -> Unit,
    onSearch: () -> Unit, onQuery: (String) -> Unit, onTopMenu: (Boolean) -> Unit,
    onPlay: (List<Song>, Song) -> Unit, onSongMenu: (Song) -> Unit,
    onOpenPlayer: () -> Unit, onToggle: () -> Unit, onAlbum: (String) -> Unit,
    onArtist: (String) -> Unit, onGenre: (String) -> Unit, onPlaylist: (String) -> Unit,
    onCreatePlaylist: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(Modifier.fillMaxWidth().height(70.dp).background(VinylBlue).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("☰", color = Color.White, fontSize = 30.sp, modifier = Modifier.clickable { onDrawer() }.padding(8.dp))
            Spacer(Modifier.width(14.dp))
            if (searching) {
                TextField(
                    value = query, onValueChange = onQuery, placeholder = { Text("搜索歌曲、艺术家或专辑") },
                    singleLine = true, modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        cursorColor = Color.White, focusedIndicatorColor = Color.White, unfocusedIndicatorColor = Color.White.copy(alpha = .5f)
                    )
                )
            } else Text("Vinyl Music Player", color = Color.White, fontSize = 24.sp, modifier = Modifier.weight(1f))
            Text(if (searching) "✕" else "⌕", color = Color.White, fontSize = 30.sp, modifier = Modifier.clickable { onSearch() }.padding(8.dp))
            Box {
                Text("⋮", color = Color.White, fontSize = 30.sp, modifier = Modifier.clickable { onTopMenu(true) }.padding(8.dp))
                DropdownMenu(expanded = topMenu, onDismissRequest = { onTopMenu(false) }) {
                    DropdownMenuItem(text = { Text("随机播放所有歌曲") }, onClick = {
                        onTopMenu(false); songs.randomOrNull()?.let { onPlay(songs.shuffled(), it) }
                    })
                    DropdownMenuItem(text = { Text("网格大小  ›") }, onClick = { onTopMenu(false) })
                    DropdownMenuItem(text = { Text("排序方式  ›") }, onClick = { onTopMenu(false) })
                    DropdownMenuItem(text = { Text("显示页脚   ✓") }, onClick = { onTopMenu(false) })
                    DropdownMenuItem(text = { Text("为页脚着色   ✓") }, onClick = { onTopMenu(false) })
                }
            }
        }
        Row(Modifier.fillMaxWidth().height(56.dp).background(VinylBlue)) {
            tabs.forEachIndexed { index, label ->
                Column(
                    Modifier.weight(1f).fillMaxHeight().clickable { onTab(index) },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom
                ) {
                    Text(label, color = Color.White, fontSize = if (label.length > 3) 14.sp else 16.sp)
                    Spacer(Modifier.height(13.dp))
                    Box(Modifier.fillMaxWidth().height(3.dp).background(if (tab == index) VinylPink else Color.Transparent))
                }
            }
        }

        val filtered = if (query.isBlank()) songs else songs.filter {
            it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true)
        }
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> SongList(filtered, songs, current, favorites, onPlay, onSongMenu)
                1 -> AlbumList(songs, onAlbum)
                2 -> ArtistList(songs, onArtist)
                3 -> GenreList(songs, onGenre)
                else -> PlaylistList(songs, favorites, playlists, onPlaylist, onCreatePlaylist)
            }
            if (filtered.isEmpty() && tab == 0) {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("♫", fontSize = 58.sp, color = Color.LightGray)
                    Text("没有结果", color = Color.Gray)
                }
            }
        }
        current?.let { MiniPlayer(it, isPlaying, onOpenPlayer, onToggle) }
    }
}

@Composable
private fun SongList(
    shown: List<Song>, playQueue: List<Song>, current: Song?, favorites: Set<Int>,
    onPlay: (List<Song>, Song) -> Unit, onMenu: (Song) -> Unit
) {
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Row(
                Modifier.fillMaxWidth().clickable { shown.randomOrNull()?.let { onPlay(shown.shuffled(), it) } }.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⤨", color = Color.Gray, fontSize = 25.sp)
                Spacer(Modifier.width(18.dp))
                Text("随机播放全部", fontWeight = FontWeight.Medium, fontSize = 18.sp)
            }
            HorizontalDivider()
        }
        items(shown, key = { it.id }) { song ->
            SongRow(song, current?.id == song.id, song.id in favorites, onClick = { onPlay(playQueue, song) }, onMenu = { onMenu(song) })
        }
    }
}

@Composable
private fun SongRow(
    song: Song, active: Boolean, favorite: Boolean, onClick: () -> Unit,
    onMenu: () -> Unit, number: String? = null
) {
    Row(Modifier.fillMaxWidth().height(78.dp).clickable { onClick() }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        if (number != null) Text(number, color = if (active) VinylPink else Color.Gray, modifier = Modifier.width(34.dp))
        else {
            AlbumCover(song.album, 48.dp)
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(song.title, fontSize = 17.sp, color = if (active) VinylPink else Color(0xFF202020), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (favorite) Text("  ♥", color = VinylPink, fontSize = 13.sp)
            }
            Spacer(Modifier.height(3.dp))
            Text("${song.artist}  •  ${song.album}", color = Color.Gray, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(timeText(song.duration), color = Color.Gray, fontSize = 13.sp)
        Text("⋮", fontSize = 28.sp, color = Color.Gray, modifier = Modifier.clickable { onMenu() }.padding(start = 12.dp, top = 12.dp, bottom = 12.dp))
    }
    HorizontalDivider(color = Color(0xFFEEEEEE))
}

@Composable
private fun AlbumList(songs: List<Song>, onAlbum: (String) -> Unit) {
    val albums = songs.groupBy { it.album }.toList()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp)) {
        items(albums.chunked(2)) { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { (album, tracks) ->
                    Column(Modifier.weight(1f).padding(7.dp).clickable { onAlbum(album) }) {
                        AlbumCover(album, 160.dp, Modifier.fillMaxWidth().aspectRatio(1f))
                        Spacer(Modifier.height(8.dp))
                        Text(album, fontSize = 17.sp, maxLines = 1)
                        Text("${tracks.first().artist}  •  ${tracks.size} 首", color = Color.Gray, fontSize = 14.sp)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ArtistList(songs: List<Song>, onArtist: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(songs.groupBy { it.artist }.toList()) { (artist, tracks) ->
            Row(Modifier.fillMaxWidth().height(88.dp).clickable { onArtist(artist) }.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(58.dp).clip(CircleShape).background(coverColors[abs(artist.hashCode()) % coverColors.size]),
                    contentAlignment = Alignment.Center
                ) { Text(artist.take(1), color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Text(artist, fontSize = 19.sp)
                    Text("${tracks.map { it.album }.distinct().size} 张专辑  •  ${tracks.size} 首歌曲", color = Color.Gray)
                }
                Text("⋮", fontSize = 28.sp, color = Color.Gray)
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun GenreList(songs: List<Song>, onGenre: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(songs.groupBy { it.genre }.toList()) { (genre, tracks) ->
            Row(Modifier.fillMaxWidth().height(76.dp).clickable { onGenre(genre) }.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("♫", color = Color.Gray, fontSize = 28.sp)
                Spacer(Modifier.width(20.dp))
                Column(Modifier.weight(1f)) { Text(genre, fontSize = 19.sp); Text("${tracks.size} 首歌曲", color = Color.Gray) }
                Text("⋮", fontSize = 28.sp, color = Color.Gray)
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun PlaylistList(
    songs: List<Song>, favorites: Set<Int>, playlists: Map<String, List<Song>>,
    onPlaylist: (String) -> Unit, onCreate: () -> Unit
) {
    val smart = listOf(
        Triple("最近添加", "本月 • ${songs.size} 歌曲", "⊞"),
        Triple("播放历史", "本月 • 4 歌曲", "◷"),
        Triple("最近未播放过", "本月 • ${songs.size - 4} 歌曲", "◔"),
        Triple("最喜爱的歌曲", "${favorites.size} 歌曲", "↗")
    )
    LazyColumn(Modifier.fillMaxSize()) {
        items(smart) { (name, detail, icon) -> PlaylistRow(icon, name, detail) {} }
        item { HorizontalDivider(Modifier.height(8.dp), color = Color(0xFFF1F1F1)) }
        item { PlaylistRow("♥", "收藏夹", "${favorites.size} 歌曲") { onPlaylist("收藏夹") } }
        items(playlists.toList()) { (name, list) -> PlaylistRow("≡♫", name, "${list.size} 歌曲") { onPlaylist(name) } }
        item {
            Row(Modifier.fillMaxWidth().clickable { onCreate() }.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("＋", color = VinylPink, fontSize = 30.sp)
                Spacer(Modifier.width(18.dp))
                Text("创建播放列表", color = VinylPink, fontSize = 17.sp)
            }
        }
    }
}

@Composable
private fun PlaylistRow(icon: String, title: String, detail: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(82.dp).clickable { onClick() }.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, color = Color.Gray, fontSize = 28.sp, modifier = Modifier.width(58.dp))
        Column(Modifier.weight(1f)) { Text(title, fontSize = 19.sp); Text(detail, color = Color.Gray) }
        Text("⋮", color = Color.Gray, fontSize = 28.sp)
    }
}

@Composable
private fun MiniPlayer(song: Song, playing: Boolean, onOpen: () -> Unit, onToggle: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color.White)) {
        LinearProgressIndicator(progress = { .22f }, modifier = Modifier.fillMaxWidth().height(3.dp), color = VinylPink, trackColor = Color(0xFFE6E6E6))
        Row(Modifier.fillMaxWidth().height(68.dp).clickable { onOpen() }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("⌃", color = Color.Gray, fontSize = 23.sp)
            Spacer(Modifier.width(26.dp))
            Text(song.title, fontSize = 18.sp, modifier = Modifier.weight(1f))
            Text(if (playing) "Ⅱ" else "▶", fontSize = 28.sp, color = Color.Gray, modifier = Modifier.clickable { onToggle() }.padding(10.dp))
        }
    }
}

@Composable
private fun PlayerPage(
    current: Song?, queue: List<Song>, playing: Boolean, progress: Int, repeatMode: Int,
    shuffle: Boolean, favorites: Set<Int>, onClose: () -> Unit, onToggle: () -> Unit,
    onNext: () -> Unit, onPrevious: () -> Unit, onSeek: (Int) -> Unit,
    onRepeat: () -> Unit, onShuffle: () -> Unit, onFavorite: () -> Unit,
    onSongMenu: (Song) -> Unit
) {
    var angle by remember { mutableFloatStateOf(0f) }
    var menu by remember { mutableStateOf(false) }
    var sleepDialog by remember { mutableStateOf(false) }
    var saveQueue by remember { mutableStateOf(false) }
    LaunchedEffect(playing) { while (playing) { angle = (angle + 1.4f) % 360f; delay(16) } }
    Column(Modifier.fillMaxSize().background(Color(0xFFE8E8E8))) {
        Box(Modifier.fillMaxWidth().height(450.dp).background(Color(0xFF1E1E1E))) {
            Text("✕", color = Color.White, fontSize = 34.sp, modifier = Modifier.align(Alignment.TopStart).clickable { onClose() }.padding(20.dp))
            Text(if (current?.id in favorites) "♥" else "♡", color = Color.White, fontSize = 38.sp, modifier = Modifier.align(Alignment.TopEnd).clickable { onFavorite() }.padding(end = 70.dp, top = 16.dp))
            Box(Modifier.align(Alignment.TopEnd)) {
                Text("⋮", color = Color.White, fontSize = 34.sp, modifier = Modifier.clickable { menu = true }.padding(18.dp))
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("清空播放队列") }, onClick = { menu = false })
                    DropdownMenuItem(text = { Text("保存播放队列") }, onClick = { menu = false; saveQueue = true })
                    DropdownMenuItem(text = { Text("睡眠定时器") }, onClick = { menu = false; sleepDialog = true })
                    DropdownMenuItem(text = { Text("均衡器") }, onClick = { menu = false })
                }
            }
            VinylDisc(260.dp, coverColors[abs((current?.album ?: "").hashCode()) % coverColors.size], angle, Modifier.align(Alignment.Center))
        }
        val duration = current?.duration ?: 1
        Slider(
            value = progress.coerceIn(0, duration).toFloat(), onValueChange = { onSeek(it.toInt()) },
            valueRange = 0f..duration.toFloat(), modifier = Modifier.fillMaxWidth().offset(y = (-14).dp),
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color(0xFFBDBDBD))
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
            Text(timeText(progress), color = Color.Gray)
            Spacer(Modifier.weight(1f))
            Text(timeText(duration), color = Color.Gray)
        }
        Row(
            Modifier.fillMaxWidth().height(106.dp).padding(horizontal = 28.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(if (repeatMode == 2) "↻¹" else "↻", color = if (repeatMode > 0) VinylPink else Color.Gray, fontSize = 30.sp, modifier = Modifier.clickable { onRepeat() })
            Text("Ⅰ◀", color = Color.Gray, fontSize = 28.sp, modifier = Modifier.clickable { onPrevious() })
            FloatingActionButton(onClick = onToggle, containerColor = Color.White, contentColor = Color.Black, modifier = Modifier.size(76.dp)) {
                Text(if (playing) "Ⅱ" else "▶", fontSize = 30.sp)
            }
            Text("▶Ⅰ", color = Color.Gray, fontSize = 28.sp, modifier = Modifier.clickable { onNext() })
            Text("⤨", color = if (shuffle) VinylPink else Color.Gray, fontSize = 30.sp, modifier = Modifier.clickable { onShuffle() })
        }
        current?.let { song ->
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(3.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(Modifier.fillMaxWidth().height(88.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    AlbumCover(song.album, 48.dp)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(song.title, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text("${song.artist}  •  ${song.album}", color = Color.Gray)
                    }
                    Text("⋮", color = Color.Gray, fontSize = 28.sp, modifier = Modifier.clickable { onSongMenu(song) }.padding(8.dp))
                }
                HorizontalDivider()
                Text("即将播放  •  ${timeText(progress)}  •  ${queue.indexOfFirst { it.id == song.id } + 1}/${queue.size}", color = Color.Gray, modifier = Modifier.padding(16.dp))
                LazyColumn(Modifier.heightIn(max = 300.dp)) {
                    itemsIndexed(queue) { index, item ->
                        SongRow(item, item.id == song.id, item.id in favorites, onClick = {}, onMenu = { onSongMenu(item) }, number = index.toString())
                    }
                }
            }
        }
    }
    if (sleepDialog) {
        AlertDialog(
            onDismissRequest = { sleepDialog = false }, title = { Text("睡眠定时器") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("30", fontSize = 64.sp, color = VinylBlue)
                    Text("分钟", color = Color.Gray)
                    Spacer(Modifier.height(12.dp))
                    Text("☑ 播放完当前歌曲", fontSize = 16.sp)
                }
            },
            dismissButton = { TextButton(onClick = { sleepDialog = false }) { Text("取消") } },
            confirmButton = { TextButton(onClick = { sleepDialog = false }) { Text("设置") } }
        )
    }
    if (saveQueue) {
        AlertDialog(
            onDismissRequest = { saveQueue = false }, title = { Text("保存播放队列") },
            text = { Text("将当前 ${queue.size} 首歌曲保存到新的播放列表。") },
            confirmButton = { TextButton(onClick = { saveQueue = false }) { Text("确定") } },
            dismissButton = { TextButton(onClick = { saveQueue = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun VinylDisc(size: androidx.compose.ui.unit.Dp, accent: Color, angle: Float, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size).graphicsLayer(rotationZ = angle)) {
        val radius = this.size.minDimension / 2
        drawCircle(Color(0xFF3B3D46), radius)
        for (i in 1..5) drawCircle(Color(0xFF555863), radius * (1f - i * .11f), style = Stroke(width = 3f))
        drawCircle(accent, radius * .27f)
        drawCircle(Color(0xFFEEEEEE), radius * .06f)
        drawLine(Color(0xFF777A84), center, androidx.compose.ui.geometry.Offset(center.x, center.y - radius * .85f), 3f)
    }
}

@Composable
private fun AlbumCover(album: String, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier.size(size)) {
    val color = coverColors[abs(album.hashCode()) % coverColors.size]
    Box(modifier.clip(RoundedCornerShape(2.dp)).background(color), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(8.dp)) {
            drawCircle(Color.Black.copy(alpha = .34f), this.size.minDimension / 2)
            drawCircle(Color.White.copy(alpha = .17f), this.size.minDimension * .30f, style = Stroke(3f))
            drawCircle(Color.White.copy(alpha = .85f), this.size.minDimension * .05f)
        }
        Text(album.take(1), color = Color.White.copy(alpha = .88f), fontWeight = FontWeight.Bold, fontSize = (size.value / 4).sp)
    }
}

@Composable
private fun CollectionDetail(
    title: String, subtitle: String, songs: List<Song>, onBack: () -> Unit,
    onPlay: (List<Song>, Song) -> Unit, onMenu: (Song) -> Unit
) {
    Column(Modifier.fillMaxSize().background(Color.White)) {
        DetailToolbar(title, onBack)
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            AlbumCover(title, 112.dp)
            Spacer(Modifier.width(20.dp))
            Column {
                Text(title, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Color.Gray, fontSize = 17.sp)
                Spacer(Modifier.height(8.dp))
                Text("${songs.size} 首歌曲  •  ${totalText(songs)}", color = Color.Gray)
            }
        }
        Button(
            onClick = { songs.firstOrNull()?.let { onPlay(songs.shuffled(), it) } },
            modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = VinylBlue)
        ) { Text("⤨  随机播放") }
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f)) {
            itemsIndexed(songs) { index, song ->
                SongRow(song, false, false, onClick = { onPlay(songs, song) }, onMenu = { onMenu(song) }, number = (index + 1).toString())
            }
        }
    }
}

@Composable
private fun PlaylistDetail(
    title: String, songs: List<Song>, onBack: () -> Unit, onPlay: (Song) -> Unit, onMenu: (Song) -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(Modifier.fillMaxWidth().height(70.dp).background(VinylBlue).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("‹", color = Color.White, fontSize = 42.sp, modifier = Modifier.clickable { onBack() }.padding(8.dp))
            Text(title, color = Color.White, fontSize = 24.sp, modifier = Modifier.weight(1f))
            Text("⤨", color = Color.White, fontSize = 28.sp, modifier = Modifier.clickable { songs.randomOrNull()?.let(onPlay) }.padding(8.dp))
            Box {
                Text("⋮", color = Color.White, fontSize = 30.sp, modifier = Modifier.clickable { menu = true }.padding(8.dp))
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    listOf("作为下一首播放", "加入播放队列", "加入播放列表", "重命名", "另存为", "删除")
                        .forEach { label -> DropdownMenuItem(text = { Text(label) }, onClick = { menu = false }) }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            AlbumCover(title, 92.dp)
            Spacer(Modifier.width(18.dp))
            Column { Text("${songs.size} 首歌曲", fontSize = 19.sp); Text(totalText(songs), color = Color.Gray) }
        }
        if (songs.isEmpty()) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("♫", fontSize = 60.sp, color = Color.LightGray)
                Text("这个播放列表还是空的", color = Color.Gray)
            }
        } else LazyColumn {
            itemsIndexed(songs) { index, song ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("≡", color = Color.Gray, fontSize = 25.sp, modifier = Modifier.padding(start = 14.dp))
                    Box(Modifier.weight(1f)) {
                        SongRow(song, false, false, onClick = { onPlay(song) }, onMenu = { onMenu(song) }, number = (index + 1).toString())
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailToolbar(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(70.dp).background(VinylBlue).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("‹", color = Color.White, fontSize = 42.sp, modifier = Modifier.clickable { onBack() }.padding(8.dp))
        Text(title, color = Color.White, fontSize = 24.sp, modifier = Modifier.weight(1f), maxLines = 1)
        Text("⋮", color = Color.White, fontSize = 30.sp, modifier = Modifier.padding(8.dp))
    }
}

@Composable
private fun FolderPage(
    songs: List<Song>, current: Song?, playing: Boolean, onBack: () -> Unit,
    onPlay: (Song) -> Unit, onMini: () -> Unit, onMenu: (Song) -> Unit
) {
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(Modifier.fillMaxWidth().height(70.dp).background(VinylBlue).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("☰", color = Color.White, fontSize = 30.sp, modifier = Modifier.clickable { onBack() }.padding(8.dp))
            Spacer(Modifier.width(18.dp))
            Text("Vinyl Music Player", color = Color.White, fontSize = 24.sp, modifier = Modifier.weight(1f))
            Text("♫", color = Color.White, fontSize = 26.sp)
            Text("⋮", color = Color.White, fontSize = 30.sp, modifier = Modifier.padding(8.dp))
        }
        Row(Modifier.fillMaxWidth().height(60.dp).background(VinylBlue).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("ROOT  ›  STORAGE  ›  EMULATED  ›  0  ›  MUSIC", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        LazyColumn(Modifier.weight(1f)) {
            itemsIndexed(songs) { index, song ->
                Row(Modifier.fillMaxWidth().height(82.dp).clickable { onPlay(song) }.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("▰♫", fontSize = 32.sp, color = Color.Gray)
                    Spacer(Modifier.width(18.dp))
                    Column(Modifier.weight(1f)) {
                        Text("%02d %s - %s.mp3".format(index + 1, song.album, song.title), fontSize = 17.sp, maxLines = 1)
                        Text("${(song.duration * 12 / 1000f).coerceAtLeast(.7f).let { "%.2f MB".format(it) }}", color = Color.Gray)
                    }
                    Text("⋮", color = Color.Gray, fontSize = 28.sp, modifier = Modifier.clickable { onMenu(song) }.padding(8.dp))
                }
                HorizontalDivider()
            }
        }
        current?.let { MiniPlayer(it, playing, onMini) {} }
    }
}

@Composable
private fun SettingsPage(onBack: () -> Unit) {
    var mediaDialog by remember { mutableStateOf(false) }
    var themeDialog by remember { mutableStateOf(false) }
    var styleDialog by remember { mutableStateOf(false) }
    var colorDialog by remember { mutableStateOf(false) }
    var appearanceDialog by remember { mutableStateOf(false) }
    var defaultActionDialog by remember { mutableStateOf(false) }
    var metadataDialog by remember { mutableStateOf(false) }
    var replayDialog by remember { mutableStateOf(false) }
    var checked by remember {
        mutableStateOf(setOf("记住上次页面", "为导航栏和快捷方式着色", "控制界面背景透明", "同步歌词", "音频焦点丢失时降低音量", "记住随机播放模式", "自动更新最喜爱的歌曲"))
    }
    fun toggle(name: String) { checked = if (name in checked) checked - name else checked + name }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        DetailToolbar("设置", onBack)
        LazyColumn(Modifier.fillMaxSize()) {
            item { Section("媒体库") }
            item { SettingRow("媒体分类", "歌曲、专辑、艺术家、音乐类型、播放列表") { mediaDialog = true } }
            item { ToggleRow("记住上次页面", "启动时恢复上次位置", "记住上次页面" in checked) { toggle("记住上次页面") } }
            item { SettingRow("黑名单", "排除指定文件夹") {} }
            item { ToggleRow("白名单", "仅扫描指定路径", false) {} }
            item { Section("颜色与样式") }
            item { SettingRow("全局主题", "浅色") { themeDialog = true } }
            item { SettingRow("主题样式", "经典") { styleDialog = true } }
            item { SettingRow("主要颜色", "靛蓝") { colorDialog = true } }
            item { SettingRow("强调色", "粉色") { colorDialog = true } }
            item { ToggleRow("为导航栏和快捷方式着色", null, "为导航栏和快捷方式着色" in checked) { toggle("为导航栏和快捷方式着色") } }
            item { ToggleRow("控制界面背景透明", null, "控制界面背景透明" in checked) { toggle("控制界面背景透明") } }
            item { Section("正在播放") }
            item { SettingRow("正在播放界面外观", "卡片") { appearanceDialog = true } }
            item { ToggleRow("同步歌词", null, "同步歌词" in checked) { toggle("同步歌词") } }
            item { ToggleRow("动画图标", null, false) {} }
            item { SettingRow("默认歌曲点击操作", "播放") { defaultActionDialog = true } }
            item { SettingRow("自动下载图像和元数据", "仅 Wi-Fi") { metadataDialog = true } }
            item { Section("声音") }
            item { ToggleRow("音频焦点丢失时降低音量", null, "音频焦点丢失时降低音量" in checked) { toggle("音频焦点丢失时降低音量") } }
            item { ToggleRow("无缝播放", null, false) {} }
            item { ToggleRow("记住随机播放模式", null, "记住随机播放模式" in checked) { toggle("记住随机播放模式") } }
            item { SettingRow("均衡器", "系统均衡器") {} }
            item { SettingRow("ReplayGain 来源", "无") { replayDialog = true } }
            item { Section("智能播放列表") }
            item { SettingRow("最近添加间隔", "1 个月") {} }
            item { SettingRow("最近未播放间隔", "1 个月") {} }
            item { ToggleRow("自动更新最喜爱的歌曲", null, "自动更新最喜爱的歌曲" in checked) { toggle("自动更新最喜爱的歌曲") } }
            item { ToggleRow("包含跳过的歌曲", null, false) {} }
            item { Section("备份") }
            item { SettingRow("导出设置", "保存应用配置") {} }
            item { SettingRow("导入设置", "从文件恢复") {} }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }

    if (mediaDialog) ChoiceDialog("媒体分类", listOf("☑ 歌曲", "☑ 专辑", "☑ 艺术家", "☑ 音乐类型", "☑ 播放列表")) { mediaDialog = false }
    if (themeDialog) ChoiceDialog("全局主题", listOf("◉ 浅色", "○ 深色", "○ 黑色 AMOLED", "○ 跟随系统主题")) { themeDialog = false }
    if (styleDialog) ChoiceDialog("主题样式", listOf("◉ 经典", "○ 圆角")) { styleDialog = false }
    if (colorDialog) ColorPickerDialog { colorDialog = false }
    if (appearanceDialog) ChoiceDialog("正在播放界面外观", listOf("▣ 卡片", "□ 平面")) { appearanceDialog = false }
    if (defaultActionDialog) ChoiceDialog("默认歌曲点击操作", listOf("○ 总是询问", "◉ 播放", "○ 作为下一首播放", "○ 加入播放队列")) { defaultActionDialog = false }
    if (metadataDialog) ChoiceDialog("自动下载图像和元数据", listOf("○ 总是", "◉ 仅 Wi-Fi", "○ 从不")) { metadataDialog = false }
    if (replayDialog) ChoiceDialog("ReplayGain 来源", listOf("◉ 无", "○ 曲目", "○ 专辑")) { replayDialog = false }
}

@Composable
private fun Section(title: String) {
    Text(title, color = VinylPink, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(start = 18.dp, top = 20.dp, bottom = 7.dp))
}

@Composable
private fun SettingRow(title: String, subtitle: String?, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text(title, fontSize = 17.sp)
        subtitle?.let { Text(it, color = Color.Gray, fontSize = 14.sp) }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String?, checked: Boolean, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onToggle() }.padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 17.sp)
            subtitle?.let { Text(it, color = Color.Gray, fontSize = 14.sp) }
        }
        Switch(checked = checked, onCheckedChange = { onToggle() }, colors = SwitchDefaults.colors(checkedThumbColor = VinylPink, checkedTrackColor = VinylPink.copy(alpha = .45f)))
    }
}

@Composable
private fun ChoiceDialog(title: String, choices: List<String>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text(title) },
        text = { Column { choices.forEach { Text(it, fontSize = 17.sp, modifier = Modifier.fillMaxWidth().clickable { onDismiss() }.padding(vertical = 12.dp)) } } },
        confirmButton = { TextButton(onClick = onDismiss) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun ColorPickerDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("选择颜色") },
        text = {
            Column {
                coverColors.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        row.forEach { color -> Box(Modifier.padding(10.dp).size(48.dp).clip(CircleShape).background(color)) }
                    }
                }
                Text("自定义", color = VinylPink, modifier = Modifier.padding(top = 16.dp))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun AboutPage(onBack: () -> Unit) {
    var changelog by remember { mutableStateOf(false) }
    var intro by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Pale)) {
        DetailToolbar("关于", onBack)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp)) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(3.dp)) {
                    Row(Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                        VinylDisc(72.dp, Color(0xFFEF476F), 0f)
                        Spacer(Modifier.width(24.dp))
                        Text("Vinyl Music Player", fontSize = 28.sp)
                    }
                    AboutRow("ⓘ", "版本", "1.11.0") {}
                    AboutRow("◴", "更新日志", null) { changelog = true }
                    AboutRow("☑", "介绍页", null) { intro = true }
                    AboutRow("◉", "创建 GitHub 分支", null) {}
                    AboutRow("▤", "许可信息", null) {}
                }
            }
            item { Spacer(Modifier.height(14.dp)) }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(3.dp)) {
                    Text("支持开发者", color = Color.Gray, modifier = Modifier.padding(20.dp))
                    AboutRow("♣", "提交 bug", "提交 bug 或申请添加新功能") {}
                    AboutRow("★", "评分", "如果喜欢 Vinyl Music Player，请留下好评。") {}
                }
            }
            item {
                Card(Modifier.padding(top = 14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Text("Maintainers", fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(20.dp))
                    Text("AdrienPoupa          Octoton\n\nsoncaokim", fontSize = 18.sp, modifier = Modifier.padding(20.dp))
                    HorizontalDivider()
                    Text("Contributors", fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(20.dp))
                }
            }
        }
    }
    if (changelog) {
        AlertDialog(
            onDismissRequest = { changelog = false }, title = { Text("版本 1.11.0") },
            text = { Text("2024-08-18\n\n新功能\n• 改进播放队列\n• 更新界面\n\n修复\n• 修复若干稳定性问题\n\n其他\n• 新贡献者") },
            confirmButton = { TextButton(onClick = { changelog = false }) { Text("确定") } }
        )
    }
    if (intro) {
        AlertDialog(
            onDismissRequest = { intro = false }, title = { Text("介绍页") },
            text = { Text("欢迎使用 Vinyl Music Player。\n\n播放队列支持上滑展开和拖动排序。") },
            confirmButton = { TextButton(onClick = { intro = false }) { Text("确定") } }
        )
    }
}

@Composable
private fun AboutRow(icon: String, title: String, subtitle: String?, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 22.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 26.sp, color = Color.Gray, modifier = Modifier.width(58.dp))
        Column { Text(title, fontSize = 19.sp); subtitle?.let { Text(it, color = Color.Gray) } }
    }
}

@Composable
private fun NavigationDrawer(
    current: Song?, onClose: () -> Unit, onLibrary: () -> Unit,
    onFolders: () -> Unit, onSettings: () -> Unit, onAbout: () -> Unit
) {
    Column(Modifier.fillMaxHeight().fillMaxWidth(.68f).background(Color.White)) {
        Box(Modifier.fillMaxWidth().height(290.dp).background(Color(0xFF171717))) {
            VinylDisc(160.dp, Color(0xFFEF476F), 0f, Modifier.align(Alignment.Center).offset(y = (-28).dp))
            Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                Text(current?.title ?: "Vinyl Music Player", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(current?.let { "${it.artist}  •  ${it.album}" } ?: "本地音乐播放器", color = Color.LightGray)
            }
        }
        DrawerRow("▣", "媒体库", onLibrary)
        DrawerRow("▰", "文件夹", onFolders)
        HorizontalDivider(Modifier.height(8.dp), color = Color(0xFFF1F1F1))
        DrawerRow("▤", "重新扫描媒体库") { onClose() }
        DrawerRow("⚙", "设置", onSettings)
        DrawerRow("?", "关于", onAbout)
    }
}

@Composable
private fun DrawerRow(icon: String, title: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(66.dp).clickable { onClick() }.padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 27.sp, color = Color.Gray, modifier = Modifier.width(58.dp))
        Text(title, fontSize = 19.sp)
    }
}

@Composable
private fun SongActionsDialog(
    song: Song, onDismiss: () -> Unit, onPlayNext: () -> Unit, onAddQueue: () -> Unit,
    onPlaylist: () -> Unit, onAlbum: () -> Unit, onArtist: () -> Unit, onFavorite: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text(song.title) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                listOf(
                    "作为下一首播放" to onPlayNext,
                    "加入播放队列" to onAddQueue,
                    "加入播放列表…" to onPlaylist,
                    "查看专辑" to onAlbum,
                    "查看艺术家" to onArtist,
                    "收藏 / 取消收藏" to onFavorite,
                    "分享" to onDismiss,
                    "音乐标签编辑器" to onDismiss,
                    "详情" to onDismiss,
                    "设为铃声" to onDismiss,
                    "从设备中删除" to onDismiss
                ).forEach { (label, action) ->
                    Text(
                        label, color = if (label == "从设备中删除") Color(0xFFD32F2F) else Color.Unspecified,
                        fontSize = 17.sp, modifier = Modifier.fillMaxWidth().clickable { action() }.padding(vertical = 8.dp)
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

@Composable
private fun PlaylistChooserDialog(
    names: List<String>, onDismiss: () -> Unit, onNew: () -> Unit, onChoose: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("加入播放列表") },
        text = {
            Column {
                Text("＋  新建播放列表", color = VinylPink, modifier = Modifier.fillMaxWidth().clickable { onNew() }.padding(vertical = 12.dp))
                names.distinct().forEach { name ->
                    Text(name, fontSize = 18.sp, modifier = Modifier.fillMaxWidth().clickable { onChoose(name) }.padding(vertical = 12.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
