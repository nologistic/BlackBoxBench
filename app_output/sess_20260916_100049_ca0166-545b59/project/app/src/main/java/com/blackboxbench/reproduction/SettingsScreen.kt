package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDateTime

@Composable
fun SettingsScreen(store: ClockStore, onBack: () -> Unit, onOpenScreensaver: () -> Unit) {
    var styleMenu by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf(false) }
    val s = store.settings

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(title = "设置", onBack = onBack, onMore = {})
        if (hint) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(18.dp)).background(Color(0xFF2E5B3A)).padding(16.dp),
            ) { Text("您可在这里找到隐私权政策", color = ClockText, fontSize = 16.sp) }
        }
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            GroupLabel("时钟")
            Box {
                ListRow(
                    icon = null,
                    title = "样式",
                    subtitle = if (s.analog) "指针" else "数字",
                    trailing = { ChevronGlyph(ClockTextDim, 22.dp) },
                    onClick = { styleMenu = true },
                )
                DropdownMenu(expanded = styleMenu, onDismissRequest = { styleMenu = false }) {
                    DropdownMenuItem(text = { Text("数字", color = ClockText) }, onClick = {
                        store.updateSettings { it.copy(analog = false) }; styleMenu = false
                    })
                    DropdownMenuItem(text = { Text("指针", color = ClockText) }, onClick = {
                        store.updateSettings { it.copy(analog = true) }; styleMenu = false
                    })
                }
            }
            ListRow(
                icon = null,
                title = "显示含秒数的时间",
                trailing = { Toggle(s.showSeconds) { v -> store.updateSettings { it.copy(showSeconds = v) } } },
            )
            ListRow(
                icon = null,
                title = "自动显示家所在地时间",
                subtitle = "在其他时区旅行时显示家中时间",
                trailing = { Toggle(s.autoHomeTime) { v -> store.updateSettings { it.copy(autoHomeTime = v) } } },
            )
            ListRow(icon = null, title = "家所在地时区", subtitle = "(GMT+8:00) 北京", onClick = {})
            ListRow(icon = null, title = "更改日期和时间", onClick = { hint = true })
            Divider()
            GroupLabel("闹钟")
            ListRow(icon = null, title = "闹钟时长", subtitle = "${s.alarmDurationMin} 分钟", onClick = {})
            ListRow(icon = null, title = "延后时长", subtitle = "${s.snoozeMin} 分钟", onClick = {})
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("闹钟音量", color = ClockText, fontSize = 18.sp)
                    Slider(
                        value = s.alarmVolume.toFloat(),
                        onValueChange = { v -> store.updateSettings { it.copy(alarmVolume = v.toInt()) } },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = ClockAccent,
                            activeTrackColor = ClockAccent,
                            inactiveTrackColor = Color(0xFF4A4E57),
                        ),
                    )
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(
        text,
        color = ClockTextDim,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 26.dp, top = 22.dp, bottom = 6.dp),
    )
}

@Composable
fun ScreensaverScreen(store: ClockStore, onExit: () -> Unit) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1000)
        }
    }
    var showCard by remember { mutableStateOf(true) }
    var dragged by remember { mutableStateOf(0f) }
    val next = nextEnabledAlarm(store.alarms, now)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 0) {
                        dragged += dragAmount
                        if (dragged > 260f) onExit()
                    }
                }
            },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                "${now.hour.two()}:${now.minute.two()}",
                color = Color(0xFFB9BCC2), fontSize = 84.sp, fontWeight = FontWeight.Light,
                modifier = Modifier.padding(end = 40.dp),
            )
            Text(
                "${now.monthValue}月${now.dayOfMonth}日周${Weekdays.dayOfWeekChar(now.dayOfWeek.value)}" +
                    (next?.let { "  " + relativeLabel(it.second, now) } ?: ""),
                color = Color(0xFF9BA1AA), fontSize = 22.sp,
                modifier = Modifier.padding(end = 40.dp),
            )
        }

        if (showCard) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 24.dp, vertical = 60.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFF2F2F4))
                    .padding(24.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        PauseCircleGlyph(Color(0xFF4A4E57), 30.dp)
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("目前处于全屏模式", color = Color(0xFF202124), fontSize = 26.sp)
                    Spacer(Modifier.height(10.dp))
                    Text("若要退出，请从屏幕顶部向下滑动", color = Color(0xFF5F6368), fontSize = 17.sp)
                    Spacer(Modifier.height(18.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color(0xFF3B4A6B))
                                .clickable { showCard = false }
                                .padding(horizontal = 26.dp, vertical = 12.dp),
                        ) { Text("知道了", color = Color.White, fontSize = 17.sp) }
                    }
                }
            }
        }
    }
}