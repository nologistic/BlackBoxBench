package com.blackboxbench.reproduction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = Store(applicationContext)
        Player.init(store)
        setContent {
            VinylApp(store)
        }
    }
}

// 路由: 简单字符串栈
class NavStack {
    val stack = mutableStateListOf("main")
    val current: String get() = stack.last()
    fun push(r: String) { stack.add(r) }
    fun pop() { if (stack.size > 1) stack.removeAt(stack.size - 1) }
}

@Composable
fun VinylApp(store: Store) {
    val nav = remember { NavStack() }
    var onboardStage by remember { mutableStateOf(if (store.onboardingDone) 2 else 0) }
    // 0 = 引导页, 1 = 权限, 2 = 主界面

    // 播放进度 tick
    LaunchedEffect(Unit) {
        var sec = 0
        while (true) {
            delay(1000)
            Player.tick()
            sec++
            if (sec >= 60) { sec = 0; Player.tickMinute() }
        }
    }

    when (onboardStage) {
        0 -> OnboardingScreen { onboardStage = 1 }
        1 -> PermissionScreen { store.setOnboardingDone(); onboardStage = 2 }
        else -> {
            BackHandler(enabled = nav.stack.size > 1) { nav.pop() }
            Surface(Modifier.fillMaxSize()) {
                when {
                    nav.current == "main" -> MainScreen(store, nav)
                    nav.current == "nowPlaying" -> NowPlayingScreen(store, nav)
                    nav.current.startsWith("album:") -> AlbumDetailScreen(store, nav, nav.current.removePrefix("album:"))
                    nav.current.startsWith("artist:") -> ArtistDetailScreen(store, nav, nav.current.removePrefix("artist:"))
                    nav.current.startsWith("genre:") -> GenreDetailScreen(store, nav, nav.current.removePrefix("genre:"))
                    nav.current.startsWith("playlist:") -> PlaylistDetailScreen(store, nav, nav.current.removePrefix("playlist:"))
                    nav.current == "folder" -> FolderScreen(store, nav)
                    nav.current == "settings" -> SettingsScreen(store, nav)
                    nav.current == "about" -> AboutScreen(nav)
                    nav.current == "search" -> SearchScreen(store, nav)
                    nav.current.startsWith("tagEditor:") -> TagEditorScreen(store, nav, nav.current.removePrefix("tagEditor:").toInt())
                }
            }
        }
    }
}

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pages = listOf(
        Triple("欢迎使用", "Vinyl Music Player\n你的本地音乐库，黑胶般的播放体验", true),
        Triple("播放队列", "在正在播放界面上滑卡片\n即可展开播放队列", false),
        Triple("调整顺序", "拖动队列左侧的序号\n即可调整播放顺序", false)
    )
    val pager = rememberPagerState { pages.size }
    Column(Modifier.fillMaxSize().background(Indigo)) {
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { i ->
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                VinylDisc(rotation = 0f, modifier = Modifier.size(160.dp))
                Spacer(Modifier.height(40.dp))
                Text(pages[i].first, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Text(pages[i].second, color = Color(0xCCFFFFFF), fontSize = 15.sp, textAlign = TextAlign.Center)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                repeat(pages.size) { i ->
                    Box(
                        Modifier.padding(4.dp).size(8.dp)
                            .background(if (i == pager.currentPage) Pink else Color(0x66FFFFFF), CircleShape)
                    )
                }
            }
            if (pager.currentPage == pages.size - 1) {
                Button(
                    onClick = onFinish,
                    colors = ButtonDefaults.buttonColors(containerColor = Pink)
                ) { Text("开始使用") }
            }
        }
    }
}

// 模拟系统权限三连弹窗: 通知 -> 音频 -> 媒体
@Composable
fun PermissionScreen(onDone: () -> Unit) {
    val prompts = listOf(
        "允许 Vinyl Music Player 向您发送通知吗？",
        "允许 Vinyl Music Player 访问您设备上的音乐和音频吗？",
        "允许 Vinyl Music Player 访问您设备上的照片和视频吗？"
    )
    var step by remember { mutableStateOf(0) }
    Box(Modifier.fillMaxSize().background(Color(0x66000000)))
    if (step < prompts.size) {
        Dialog(onDismissRequest = {}) {
            Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFFF8F9FA)) {
                Column(Modifier.padding(24.dp)) {
                    Text(prompts[step], fontSize = 16.sp, color = Color(0xFF1B1B1F))
                    Spacer(Modifier.height(20.dp))
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                        TextButton(onClick = { step++ }) {
                            Text("允许", color = Color(0xFF0B57D0), fontWeight = FontWeight.Medium)
                        }
                        TextButton(onClick = { step++ }) {
                            Text("不允许", color = Color(0xFF0B57D0), fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    } else {
        LaunchedEffect(Unit) { onDone() }
    }
}
