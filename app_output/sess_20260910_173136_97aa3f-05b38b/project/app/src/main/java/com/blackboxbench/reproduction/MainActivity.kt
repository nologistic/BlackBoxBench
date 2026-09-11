package com.blackboxbench.reproduction

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ReproducedApp() }
    }
}

@Composable
fun ReproducedApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("vlc_state", Context.MODE_PRIVATE) }
    var onboarded by remember { mutableStateOf(prefs.getBoolean("onboarded", false)) }
    var screen by remember { mutableStateOf("main") }
    var tab by remember { mutableStateOf("video") }
    var favorite by remember { mutableStateOf(prefs.getBoolean("favorite", false)) }
    var playlistName by remember { mutableStateOf(prefs.getString("playlist", "") ?: "") }
    var incognito by remember { mutableStateOf(prefs.getBoolean("incognito", false)) }
    var equalizerEnabled by remember { mutableStateOf(prefs.getBoolean("eq_enabled", false)) }
    var equalizerPreset by remember { mutableStateOf(prefs.getString("eq_preset", "Flat") ?: "Flat") }
    var remoteEnabled by remember { mutableStateOf(prefs.getBoolean("remote", false)) }
    var videoProgress by remember { mutableStateOf(prefs.getFloat("video_progress", 0f)) }

    fun persistBoolean(key: String, value: Boolean) { prefs.edit().putBoolean(key, value).apply() }
    fun open(next: String) { screen = next }
    fun home(nextTab: String = tab) { tab = nextTab; screen = "main" }

    BenchmarkAppTheme {
        Surface(Modifier.fillMaxSize(), color = Color.White) {
            if (!onboarded) {
                OnboardingScreen {
                    prefs.edit().putBoolean("onboarded", true).apply()
                    onboarded = true
                }
            } else {
                BackHandler(enabled = screen != "main") { screen = "main" }
                when (screen) {
                    "main" -> MainScaffold(
                        tab = tab,
                        onTab = { tab = it },
                        onOpen = ::open,
                        favorite = favorite,
                        onFavorite = {
                            favorite = !favorite
                            persistBoolean("favorite", favorite)
                        },
                        playlistName = playlistName,
                        onPlaylistCreated = {
                            playlistName = it
                            prefs.edit().putString("playlist", it).apply()
                        },
                        incognito = incognito,
                        onIncognito = {
                            incognito = !incognito
                            persistBoolean("incognito", incognito)
                        }
                    )
                    "videoPlayer" -> VideoPlayerScreen(
                        initialProgress = videoProgress,
                        onProgress = { videoProgress = it; prefs.edit().putFloat("video_progress", it).apply() },
                        onBack = { home("video") }
                    )
                    "audioPlayer" -> AudioPlayerScreen(
                        favorite = favorite,
                        onFavorite = {
                            favorite = !favorite
                            persistBoolean("favorite", favorite)
                        },
                        onBack = { home("audio") }
                    )
                    "settings" -> SettingsHome(onBack = { home("more") }, onOpen = ::open)
                    "about" -> AboutScreen(onBack = { home("more") })
                    "stream" -> StreamScreen(
                        incognito = incognito,
                        onIncognito = {
                            incognito = !incognito
                            persistBoolean("incognito", incognito)
                        },
                        onBack = { home("more") }
                    )
                    "history" -> HistoryScreen(onBack = { home("more") }, onOpen = ::open)
                    "interface" -> InterfaceSettings(
                        incognito = incognito,
                        onIncognito = {
                            incognito = !incognito
                            persistBoolean("incognito", incognito)
                        },
                        onBack = { screen = "settings" }
                    )
                    "videoSettings" -> VideoSettings(onBack = { screen = "settings" })
                    "subtitleSettings" -> SubtitleSettings(onBack = { screen = "settings" })
                    "audioSettings" -> AudioSettings(onBack = { screen = "settings" })
                    "equalizer" -> EqualizerScreen(
                        enabled = equalizerEnabled,
                        preset = equalizerPreset,
                        onEnabled = {
                            equalizerEnabled = it
                            prefs.edit().putBoolean("eq_enabled", it).apply()
                        },
                        onPreset = {
                            equalizerPreset = it
                            prefs.edit().putString("eq_preset", it).apply()
                        },
                        onBack = { screen = "settings" }
                    )
                    "cast" -> CastSettings(onBack = { screen = "settings" })
                    "parental" -> ParentalControl(onBack = { screen = "settings" })
                    "remote" -> RemoteSettings(
                        enabled = remoteEnabled,
                        onEnabled = {
                            remoteEnabled = it
                            prefs.edit().putBoolean("remote", it).apply()
                        },
                        onBack = { screen = "settings" }
                    )
                    "androidAuto" -> AndroidAutoSettings(onBack = { screen = "settings" })
                    "advanced" -> AdvancedSettings(onBack = { screen = "settings" })
                    else -> screen = "main"
                }
            }
        }
    }
}

@Composable
private fun OnboardingScreen(onFinish: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().background(Color(0xFFF6F6F6))) {
        Spacer(Modifier.height(54.dp))
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(28.dp)) {
                if (page == 0) {
                    TrafficCone(Modifier.size(150.dp))
                    Spacer(Modifier.height(34.dp))
                    Text("欢迎使用 VLC!", color = VlcOrange, fontSize = 31.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(14.dp))
                    Text("自由开源的多媒体播放器", fontSize = 19.sp, color = Color(0xFF666666))
                    Spacer(Modifier.height(38.dp))
                    Text("VLC 可以播放几乎所有音视频文件与网络串流。", fontSize = 16.sp, color = Color(0xFF757575), lineHeight = 24.sp)
                } else {
                    Text("媒体访问权限", color = VlcOrange, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(18.dp))
                    Text("请选择 VLC 可以访问的媒体。您可以稍后在设置中更改。", fontSize = 17.sp, lineHeight = 25.sp, color = Color(0xFF666666))
                    Spacer(Modifier.height(28.dp))
                    PermissionChoice("不允许", "仍可打开单个文件和网络串流")
                    PermissionChoice("仅媒体文件", "访问设备上的音乐、音频、照片与视频")
                    PermissionChoice("所有文件", "浏览存储空间中的所有媒体文件")
                }
            }
        }
        HorizontalDivider()
        Row(
            Modifier.fillMaxWidth().height(76.dp).padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            ActionText("跳过", onFinish)
            Spacer(Modifier.width(12.dp))
            VlcButton(if (page == 0) "下一项" else "完成", onClick = { if (page == 0) page = 1 else onFinish() })
        }
    }
}

@Composable
private fun PermissionChoice(title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(22.dp).background(Color.White, CircleShape).border(2.dp, VlcOrange, CircleShape))
        Column(Modifier.padding(start = 12.dp)) {
            Text(title, fontSize = 18.sp)
            Text(subtitle, fontSize = 14.sp, color = Color(0xFF777777))
        }
    }
}
