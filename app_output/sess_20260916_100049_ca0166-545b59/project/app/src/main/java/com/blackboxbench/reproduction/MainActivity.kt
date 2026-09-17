package com.blackboxbench.reproduction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.delay
import java.time.LocalDateTime

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = ClockStore(applicationContext)
        setContent {
            BenchmarkAppTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = ClockBg) {
                    Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                        ClockApp(store)
                    }
                }
            }
        }
    }
}

sealed interface Overlay {
    data object None : Overlay
    data object Settings : Overlay
    data object WorldClock : Overlay
    data object Screensaver : Overlay
    data class AlarmSound(val alarmId: Int) : Overlay
    data object AlarmRing : Overlay
}

private data class Tab(val label: String, val icon: @Composable (Color) -> Unit)

@Composable
fun ClockApp(store: ClockStore) {
    var tab by remember { mutableStateOf(1) }
    var overlay by remember { mutableStateOf<Overlay>(Overlay.None) }
    var ringing by remember { mutableStateOf(false) }

    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(500)
        }
    }

    // Lightweight alarm scheduler: fires the ringing screen when an enabled alarm matches.
    var lastFiredKey by remember { mutableStateOf("") }
    LaunchedEffect(now, store.alarms.size, store.alarms.toList()) {
        val enabled = store.alarms.filter { it.enabled }
        for (a in enabled) {
            val key = "${now.toLocalDate()}-${a.id}-${a.hour}-${a.minute}"
            if (now.hour == a.hour && now.minute == a.minute && now.second < 5 && key != lastFiredKey) {
                lastFiredKey = key
                ringing = true
                overlay = Overlay.AlarmRing
            }
        }
    }

    when (val o = overlay) {
        is Overlay.Settings -> {
            SettingsScreen(
                store = store,
                onBack = { overlay = Overlay.None },
                onOpenScreensaver = { overlay = Overlay.Screensaver },
            )
            return
        }
        is Overlay.WorldClock -> {
            WorldClockScreen(store = store, now = now, onBack = { overlay = Overlay.None })
            return
        }
        is Overlay.Screensaver -> {
            ScreensaverScreen(store = store, onExit = { overlay = Overlay.None })
            return
        }
        is Overlay.AlarmSound -> {
            AlarmSoundScreen(store = store, alarmId = o.alarmId, onBack = { overlay = Overlay.None })
            return
        }
        is Overlay.AlarmRing -> {
            AlarmRingScreen(store = store, onDismiss = { ringing = false; overlay = Overlay.None })
            return
        }
        Overlay.None -> Unit
    }

    val tabs = listOf(
        Tab("闹钟") { AlarmTabIcon(it) },
        Tab("时钟") { ClockTabIcon(it) },
        Tab("定时器") { TimerTabIcon(it) },
        Tab("秒表") { StopwatchTabIcon(it) },
        Tab("就寝时间") { BedtimeTabIcon(it) },
    )

    Column(modifier = Modifier.fillMaxSize().background(ClockBg)) {
        Box(modifier = Modifier.weight(1f)) {
            when (tab) {
                0 -> AlarmScreen(store, now, onOpenSettings = { overlay = Overlay.Settings })
                1 -> ClockScreen(
                    store, now,
                    onOpenSettings = { overlay = Overlay.Settings },
                    onOpenWorldClock = { overlay = Overlay.WorldClock },
                )
                2 -> TimerScreen()
                3 -> StopwatchScreen()
                else -> BedtimeScreen(store, now, onOpenSettings = { overlay = Overlay.Settings })
            }
        }
        BottomBar(tabs = tabs, selected = tab, onSelect = { tab = it })
    }
}

@Composable
private fun BottomBar(tabs: List<Tab>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ClockBg)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEachIndexed { index, item ->
            val active = index == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(index) }
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .width(60.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(if (active) ClockGroupHeader else Color.Transparent),
                    contentAlignment = Alignment.Center,
                ) {
                    item.icon(if (active) ClockText else ClockTextDim)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    item.label,
                    color = if (active) ClockText else ClockTextDim,
                    fontSize = 12.sp,
                    fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                )
            }
        }
    }
}
