package com.blackboxbench.reproduction

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDateTime

@Composable
fun BedtimeScreen(store: ClockStore, now: LocalDateTime, onOpenSettings: () -> Unit) {
    val step = store.bedtimeStep
    val draft = store.bedtimeDraft
    val dismissed = remember { mutableStateListOf<Int>() }

    when (step) {
        0 -> BedtimeOnboarding(onStart = { store.bedtimeStep = 1 })
        1 -> WakeStep(
            draft = draft,
            onSkip = { store.bedtimeStep = 0 },
            onNext = { store.bedtimeStep = 2 },
        )
        2 -> SleepStep(
            draft = draft,
            onSkip = { store.bedtimeStep = 0 },
            onDone = {
                store.enableBedtime(
                    store.bedtime.copy(
                        enabled = true,
                        bedHour = draft.bedHour, bedMinute = draft.bedMinute,
                        wakeHour = draft.wakeHour, wakeMinute = draft.wakeMinute,
                        days = draft.days, sunrise = draft.sunrise, sound = draft.sound,
                        vibrate = draft.vibrate, remindMinutes = draft.remindMinutes,
                        bedtimeMode = draft.bedtimeMode,
                    ),
                )
                store.bedtimeStep = 3
            },
        )
        else -> BedtimeConfigured(store, now, dismissed)
    }
}

@Composable
private fun BedtimeOnboarding(onStart: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().clickable { onStart() }) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TopBar(title = "就寝时间", onMore = {})
            Spacer(Modifier.height(24.dp))
            Text(
                "设置规律的就寝时间，改善睡眠质量",
                color = ClockText, fontSize = 30.sp, lineHeight = 44.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(22.dp))
            Text(
                "选择规律的就寝时间、放下设备并聆听舒缓的音效",
                color = ClockTextDim, fontSize = 18.sp, lineHeight = 28.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(30.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(300.dp)
                    .clip(RoundedCornerShape(24.dp)).background(ClockSurface),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(120.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFFF2B01E)),
                        contentAlignment = Alignment.Center,
                    ) { AlarmTabIcon(Color(0xFF3A2E12), 54.dp) }
                    Spacer(Modifier.height(18.dp))
                    Box(
                        modifier = Modifier.size(width = 260.dp, height = 22.dp)
                            .clip(RoundedCornerShape(11.dp)).background(Color(0xFF3A3D45)),
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(ClockAccent)
                    .clickable { onStart() },
                contentAlignment = Alignment.Center,
            ) { Text("开始使用", color = Color(0xFF16233A), fontSize = 19.sp) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun WakeStep(draft: BedtimeDraft, onSkip: () -> Unit, onNext: () -> Unit) {
    BedtimeStepShell(
        icon = { AlarmTabIcon(ClockText, 30.dp) },
        title = "设置规律的起床闹钟",
        time = hhmm(draft.wakeHour, draft.wakeMinute),
        onMinus = { draft.wakeHour = (draft.wakeHour + 23) % 24 },
        onPlus = { draft.wakeHour = (draft.wakeHour + 1) % 24 },
        days = draft.days,
        onDay = { d ->
            val s = draft.days.toMutableSet()
            if (d in s) s.remove(d) else s.add(d)
            draft.days = s
        },
        onSkip = onSkip,
        onNext = onNext,
        nextLabel = "下一步",
    ) {
        ListRow(
            icon = { if (draft.sunrise) SunGlyph(ClockText, 24.dp) else SunGlyph(ClockTextDim, 24.dp) },
            title = "日出闹钟",
            subtitle = "在闹钟响铃之前慢慢调亮屏幕",
            trailing = { Toggle(draft.sunrise) { draft.sunrise = it } },
        )
        ListRow(
            icon = { BellGlyph(ClockText, 24.dp) },
            title = "音效",
            subtitle = draft.sound,
            onClick = {},
        )
        ListRow(
            icon = { VibrateGlyph(ClockText, 24.dp) },
            title = "振动",
            trailing = { CheckCircle(draft.vibrate) { draft.vibrate = !draft.vibrate } },
        )
    }
}

@Composable
private fun SleepStep(draft: BedtimeDraft, onSkip: () -> Unit, onDone: () -> Unit) {
    val hours = ((24 + draft.wakeHour - draft.bedHour) % 24).let { if (it == 0) 8 else it }
    BedtimeStepShell(
        icon = { BedtimeTabIcon(ClockText, 30.dp) },
        title = "设置就寝时间，让设备按时静音",
        time = hhmm(draft.bedHour, draft.bedMinute),
        subtitle = "$hours 小时",
        onMinus = { draft.bedHour = (draft.bedHour + 23) % 24 },
        onPlus = { draft.bedHour = (draft.bedHour + 1) % 24 },
        days = draft.days,
        onDay = { d ->
            val s = draft.days.toMutableSet()
            if (d in s) s.remove(d) else s.add(d)
            draft.days = s
        },
        onSkip = onSkip,
        onNext = onDone,
        nextLabel = "完成",
    ) {
        ListRow(
            icon = { BellGlyph(ClockText, 24.dp) },
            title = "提醒通知",
            subtitle = "就寝时间前 ${draft.remindMinutes} 分钟",
            onClick = {},
        )
        ListRow(
            icon = { MoonGlyph(ClockText, 24.dp) },
            title = "就寝模式",
            subtitle = if (draft.bedtimeMode) "勿扰・更改屏幕" else "关闭",
            onClick = {},
        )
    }
}

@Composable
private fun BedtimeStepShell(
    icon: @Composable () -> Unit,
    title: String,
    time: String,
    subtitle: String? = null,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    days: Set<Int>,
    onDay: (Int) -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit,
    nextLabel: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.height(30.dp))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { icon() }
        Spacer(Modifier.height(16.dp))
        Text(
            title, color = ClockText, fontSize = 28.sp, lineHeight = 40.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier.size(64.dp).clip(CircleShape).background(ClockSurfaceHigh).clickable { onMinus() },
                contentAlignment = Alignment.Center,
            ) { Text("−", color = ClockText, fontSize = 34.sp) }
            Spacer(Modifier.width(24.dp))
            Text(time, color = ClockText, fontSize = 72.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.width(24.dp))
            Box(
                modifier = Modifier.size(64.dp).clip(CircleShape).background(ClockSurfaceHigh).clickable { onPlus() },
                contentAlignment = Alignment.Center,
            ) { PlusGlyph(ClockText, 30.dp) }
        }
        if (subtitle != null) {
            Spacer(Modifier.height(8.dp))
            Text(subtitle, color = ClockText, fontSize = 22.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            for (d in 1..7) {
                val sel = d in days
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (sel) ClockAccent else ClockSurfaceHigh)
                        .clickable { onDay(d) },
                    contentAlignment = Alignment.Center,
                ) { Text(Weekdays.glyph(d), color = if (sel) Color(0xFF16233A) else ClockTextDim, fontSize = 16.sp) }
            }
        }
        Spacer(Modifier.height(16.dp))
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) { content() }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlineButton("跳过", onClick = onSkip)
            Spacer(Modifier.weight(1f))
            PillButton(nextLabel, onClick = onNext)
        }
    }
}

@Composable
private fun BedtimeConfigured(store: ClockStore, now: LocalDateTime, dismissed: MutableList<Int>) {
    val b = store.bedtime
    val wake = store.alarms.firstOrNull { it.isWake }
    val nextText = wake?.let { a ->
        nextTrigger(a, now)?.let { relativeLabel(it, now) }
    } ?: "无"
    val hours = ((24 + b.wakeHour - b.bedHour) % 24).let { if (it == 0) 8 else it }

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(title = "就寝时间", onMore = {})
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Card(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(ClockSurfaceHigh),
                            contentAlignment = Alignment.Center,
                        ) { TimerTabIcon(ClockText, 20.dp) }
                        Spacer(Modifier.width(16.dp))
                        Text("时间表", color = ClockText, fontSize = 22.sp)
                    }
                    Spacer(Modifier.height(18.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("就寝时间", color = ClockTextDim, fontSize = 15.sp)
                            Text(hhmm(b.bedHour, b.bedMinute), color = ClockText, fontSize = 52.sp, fontWeight = FontWeight.Light)
                        }
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("起床时间", color = ClockTextDim, fontSize = 15.sp)
                            Text(hhmm(b.wakeHour, b.wakeMinute), color = ClockText, fontSize = 52.sp, fontWeight = FontWeight.Light)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "$hours 小时 · 下次闹钟时间：$nextText",
                        color = ClockTextDim, fontSize = 16.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                    )
                }
            }

            if (0 !in dismissed) {
                InfoCard(
                    icon = { GraphGlyph(ClockText, 24.dp) },
                    title = "查看近期就寝时间内的活动数据",
                    body = "跟踪设备使用时间，查看估算的睡眠时长。睡眠时长根据设备在黑暗的房间内保持静止不动的时间估算。",
                    primaryLabel = "继续",
                    onPrimary = { dismissed.add(0) },
                    onDismiss = { dismissed.add(0) },
                )
            }
            InfoCard(
                icon = { MoonGlyph(ClockText, 24.dp) },
                title = "聆听助眠音效",
                body = "您可以播放舒缓的音乐，帮助自己安然入睡。系统不会自动播放助眠音效。",
                primaryLabel = "选择音效",
                onPrimary = { },
                onDismiss = null,
            )
            if (1 !in dismissed) {
                InfoCard(
                    icon = { CalendarGlyph(ClockText, 24.dp) },
                    title = "查看即将进行的活动",
                    body = "请允许\"时钟\"应用访问您的日历，以确保在活动开始之前设置闹钟。",
                    primaryLabel = "继续",
                    onPrimary = { dismissed.add(1) },
                    onDismiss = { dismissed.add(1) },
                )
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun InfoCard(
    icon: @Composable () -> Unit,
    title: String,
    body: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    onDismiss: (() -> Unit)?,
) {
    Card(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(ClockSurfaceHigh),
                    contentAlignment = Alignment.Center,
                ) { icon() }
                Spacer(Modifier.width(16.dp))
                Text(title, color = ClockText, fontSize = 21.sp, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Text(body, color = ClockTextDim, fontSize = 16.sp)
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (onDismiss != null) {
                    Spacer(Modifier.weight(1f))
                    OutlineButton("不用了", onClick = onDismiss)
                }
                Spacer(Modifier.weight(1f))
                PillButton(primaryLabel, onClick = onPrimary)
            }
        }
    }
}

@Composable
fun AlarmRingScreen(store: ClockStore, onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            kotlinx.coroutines.delay(1000)
        }
    }
    var player by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    androidx.compose.runtime.DisposableEffect(Unit) {
        runCatching {
            player = android.media.MediaPlayer.create(context, R.raw.ringtone_cesium).also { it.start() }
        }
        onDispose { runCatching { player?.release() } }
    }
    val alarm = store.alarms.firstOrNull { it.enabled && it.hour == now.hour && it.minute == now.minute }
        ?: store.alarms.firstOrNull { it.enabled }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF1B2436)).padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(80.dp))
        if (alarm != null && alarm.label.isNotEmpty()) {
            Text(alarm.label, color = ClockTextDim, fontSize = 24.sp)
            Spacer(Modifier.height(12.dp))
        }
        Text(
            hhmm(now.hour, now.minute),
            color = ClockText, fontSize = 92.sp, fontWeight = FontWeight.Light,
        )
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier.size(width = 220.dp, height = 74.dp).clip(RoundedCornerShape(37.dp))
                .background(ClockAccent).clickable { onDismiss() },
            contentAlignment = Alignment.Center,
        ) { Text("停止", color = Color(0xFF16233A), fontSize = 22.sp) }
        Spacer(Modifier.height(20.dp))
        Box(
            modifier = Modifier.size(width = 220.dp, height = 74.dp).clip(RoundedCornerShape(37.dp))
                .background(ClockSurfaceHigh).clickable { onDismiss() },
            contentAlignment = Alignment.Center,
        ) { Text("稍后提醒", color = ClockText, fontSize = 22.sp) }
        Spacer(Modifier.height(60.dp))
    }
}