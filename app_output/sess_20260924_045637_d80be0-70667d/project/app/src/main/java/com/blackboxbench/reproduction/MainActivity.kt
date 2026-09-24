package com.blackboxbench.reproduction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = AppState(VinylState(applicationContext))
        setContent {
            BenchmarkAppTheme(mode = app.themeMode) {
                Root(app)
            }
        }
    }
}

@Composable
fun Root(app: AppState) {
    var onboarded by remember { mutableStateOf(app.store.onboarded) }
    var permissionIndex by remember { mutableStateOf(-1) }
    if (!onboarded) {
        OnboardingScreen(onFinish = {
            onboarded = true
            app.store.onboarded = true
            permissionIndex = 0
        })
        return
    }
    if (permissionIndex in 0..2) {
        PermissionDialog(permissionIndex) {
            permissionIndex += 1
        }
        Box(modifier = Modifier.fillMaxSize().background(LocalViColors.current.background)) {
            MainShell(app)
        }
        return
    }
    MainShell(app)
}

@Composable
fun PermissionDialog(index: Int, onDone: () -> Unit) {
    val messages = listOf(
        "允许向用户发送通知" to "Vinyl Music Player 想要向您发送通知",
        "允许访问设备上的音乐和音频" to "Vinyl Music Player 想要访问设备上的音乐和音频",
        "允许访问设备上的照片和视频" to "Vinyl Music Player 想要访问设备上的照片和视频"
    )
    val (title, body) = messages[index]
    val colors = LocalViColors.current
    Dialog(onDismissRequest = { }, properties = DialogProperties()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(colors.card)
                .padding(vertical = 12.dp)
        ) {
            Text(
                title,
                color = colors.text,
                fontSize = 20.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            Text(
                body,
                color = colors.text,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(end = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onDone() }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text("不允许", color = colors.accent, fontSize = 15.sp)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onDone() }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text("允许", color = colors.accent, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val colors = LocalViColors.current
    var page by remember { mutableStateOf(0) }
    var drag by remember { mutableStateOf(0f) }
    val titles = listOf(
        "Vinyl Music Player" to "欢迎使用 Vinyl Music Player，这是一款精致且简洁的 Android 音乐播放器。",
        "播放队列" to "上滑正在播放界面内的卡片即可展开播放队列",
        "播放队列排序" to "拖动歌曲名前面的序列号即可调整播放顺序"
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFBFC7D1))
            .statusBarsPadding()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (drag < -60f && page < 2) page += 1
                        if (drag > 60f && page > 0) page -= 1
                        if (drag < -60f && page == 2) onFinish()
                        drag = 0f
                    },
                    onDrag = { _, amount -> drag += amount.x }
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(120.dp))
        VinylArt(180.dp)
        Spacer(Modifier.height(80.dp))
        Text(
            titles[page].first,
            color = Color(0xFF1B1B1F),
            fontSize = 30.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(20.dp))
        Text(
            titles[page].second,
            color = Color(0xFF33373D),
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 36.dp)
        )
        Spacer(Modifier.height(40.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFFF2F2F2))
                .clickable { onFinish() }
                .padding(horizontal = 28.dp, vertical = 14.dp)
        ) {
            Text("GET STARTED", color = Color(0xFF3B3F46), fontSize = 17.sp)
        }
        Spacer(Modifier.height(36.dp))
        Row(horizontalArrangement = Arrangement.Center) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(
                            if (page == index) Color(0xFF3B3F46) else Color(0xFF8C939C)
                        )
                )
            }
        }
    }
}

@Composable
fun MainShell(app: AppState) {
    val nav = remember { Navigator() }
    val dialogs = remember { DialogController() }
    val menu = remember { MenuController() }
    var selectedTab by remember {
        mutableStateOf(if (app.rememberLastPage) app.lastPage else "歌曲")
    }
    var searchQuery by remember { mutableStateOf("") }
    var folderPath by remember { mutableStateOf("/") }

    SideEffect {
        LocalMenuRef.show = { items, anchor -> menu.show(items, anchor) }
    }

    LaunchedEffect(app.playing, app.currentSongId) {
        while (app.playing) {
            delay(500)
            app.tick(500)
        }
    }

    CompositionLocalProvider(
        LocalNav provides nav,
        LocalDialogs provides dialogs,
        LocalApp provides app,
        LocalMenu provides menu
    ) {
        val colors = LocalViColors.current
        val route = nav.current?.first
        Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    when (route) {
                        null -> LibraryScreen(app, selectedTab) {
                            selectedTab = it
                            app.lastPage = it
                            app.persistSettings()
                        }
                        "album" -> AlbumDetailScreen(app, nav.current!!.second)
                        "artist" -> ArtistDetailScreen(app, nav.current!!.second)
                        "genre" -> GenreDetailScreen(app, nav.current!!.second)
                        "playlist" -> PlaylistDetailScreen(app, nav.current!!.second)
                        "search" -> SearchScreen(app, searchQuery) { searchQuery = it }
                        "folders" -> FoldersScreen(app, folderPath) { folderPath = it }
                        "settings" -> SettingsScreen(app)
                        "about" -> AboutScreen(app)
                        "nowplaying" -> NowPlayingScreen(app, onClose = { nav.pop() }, onOpenEqualizer = { dialogs.equalizer = true })
                        "sdcard" -> SdCardGuideScreen(onBack = { nav.pop() })
                        "intro" -> OnboardingScreen(onFinish = { nav.pop() })
                        "drawer" -> DrawerScreen(
                            app,
                            onClose = { nav.pop() },
                            onOpenSettings = { nav.push("settings") },
                            onOpenAbout = { nav.push("about") },
                            onOpenFolders = { nav.push("folders") }
                        )
                        else -> LibraryScreen(app, selectedTab) {
                            selectedTab = it
                            app.lastPage = it
                            app.persistSettings()
                        }
                    }
                }
                val showMini = route == null || route == "album" || route == "artist" ||
                    route == "genre" || route == "playlist" || route == "search" || route == "folders"
                if (showMini && app.hasQueue) {
                    MiniPlayer(app) { nav.push("nowplaying") }
                }
            }
            MenuHost(menu)
            ViSnackbarHost(app.snackbar) { app.snackbar = null }
        }

        // ---- shared dialogs -------------------------------------------------
        dialogs.sleepTimer.let { open ->
            if (open) SleepTimerDialog(app) { dialogs.sleepTimer = false }
        }
        dialogs.songDetails?.let { song ->
            SongDetailsDialog(song) { dialogs.songDetails = null }
        }
        dialogs.tagEditor?.let { song ->
            TagEditorDialog(song) { dialogs.tagEditor = null }
        }
        dialogs.addToPlaylist?.let { ids ->
            AddToPlaylistDialog(app, ids) { dialogs.addToPlaylist = null }
        }
        dialogs.newPlaylist?.let { ids ->
            NewPlaylistDialog(app, ids) { dialogs.newPlaylist = null }
        }
        dialogs.renamePlaylist?.let { name ->
            RenamePlaylistDialog(app, name) { dialogs.renamePlaylist = null }
        }
        dialogs.deletePlaylist?.let { name ->
            ConfirmDeletePlaylistDialog(app, name) { dialogs.deletePlaylist = null }
        }
        dialogs.saveAsPlaylist?.let { id ->
            SaveQueueDialog(app) { dialogs.saveAsPlaylist = null }
        }
        if (dialogs.themePicker) ThemePickerDialog(app) { dialogs.themePicker = false }
        if (dialogs.categories) LibraryCategoriesDialog(app) { dialogs.categories = false }
        if (dialogs.ringtone) RingtoneDialog { dialogs.ringtone = false }
        if (dialogs.changelog) ChangelogDialog { dialogs.changelog = false }
        if (dialogs.licenses) LicensesDialog { dialogs.licenses = false }
        if (dialogs.replayGain) ReplayGainDialog(app) { dialogs.replayGain = false }
        if (dialogs.equalizer) EqualizerDialog(app) { dialogs.equalizer = false }
        if (dialogs.blacklistInfo) {
            MessageDialog("黑名单", "添加到黑名单的文件夹不会出现在媒体库中。") { dialogs.blacklistInfo = false }
        }
        if (dialogs.whitelistInfo) {
            MessageDialog("白名单", "只显示起始目录：/storage/emulated/0/Music") { dialogs.whitelistInfo = false }
        }
        if (dialogs.exportInfo) {
            MessageDialog("Migrating", "导出或导入播放列表与设置。") { dialogs.exportInfo = false }
        }
    }
}
