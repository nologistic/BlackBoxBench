package com.blackboxbench.reproduction

import android.media.MediaPlayer
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class TimerInstance(
    val id: Int,
    val totalMs: Long,
) {
    var running by mutableStateOf(true)
    var endAt by mutableStateOf(System.currentTimeMillis() + totalMs)
    var pausedRemaining by mutableStateOf(totalMs)
    var finished by mutableStateOf(false)
    var notified by mutableStateOf(false)

    fun remainingMs(now: Long): Long = if (running) endAt - now else pausedRemaining
}

object TimerStore {
    val timers: SnapshotStateList<TimerInstance> = mutableStateListOf()
    private var nextId = 1

    fun create(totalMs: Long): TimerInstance {
        val t = TimerInstance(nextId++, totalMs)
        timers.add(t)
        return t
    }

    fun remove(t: TimerInstance) = timers.remove(t)
}

@Composable
fun TimerScreen() {
    val context = LocalContext.current
    var value by remember { mutableStateOf(0L) } // seconds being typed
    var adding by remember { mutableStateOf(false) }
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(200)
        }
    }

    // Completion detection + one-shot sound.
    LaunchedEffect(nowMs, TimerStore.timers.size) {
        TimerStore.timers.forEach { t ->
            if (t.running && !t.finished && t.remainingMs(nowMs) <= 0) {
                t.finished = true
            }
        }
        val anyFinished = TimerStore.timers.any { it.finished && it.running && !it.notified }
        if (anyFinished) {
            TimerStore.timers.filter { it.finished && !it.notified }.forEach { it.notified = true }
            runCatching {
                player?.release()
                player = MediaPlayer.create(context, R.raw.ringtone_cesium).also { it.start() }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(title = "定时器", onMore = {})

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            TimerStore.timers.forEach { t ->
                TimerCard(t, nowMs, onClose = { TimerStore.remove(t) }, player = player)
                Spacer(Modifier.height(12.dp))
            }

            if (TimerStore.timers.isEmpty() || adding) {
                Spacer(Modifier.height(20.dp))
                Text(
                    text = formatHms(value),
                    color = if (value > 0) ClockText else ClockTextDim,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.fillMaxWidth().padding(end = 26.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                )
                Spacer(Modifier.height(10.dp))
                TimerKeypad(
                    onDigit = { d -> value = ((value * 10) + d).coerceAtMost(99L * 3600 + 59 * 60 + 59) },
                    onDoubleZero = { value = ((value * 100)).coerceAtMost(99L * 3600 + 59 * 60 + 59) },
                    onBackspace = { value /= 10 },
                )
                Spacer(Modifier.height(24.dp))
            }
        }

        if (TimerStore.timers.isEmpty() || adding) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 8.dp),
            ) {
                if (TimerStore.timers.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF7A3B54))
                            .clickable { adding = false; value = 0 },
                        contentAlignment = Alignment.Center,
                    ) { TrashGlyph(ClockText, 26.dp) }
                }
                if (value > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(ClockAccent)
                            .clickable {
                                TimerStore.create(value * 1000L)
                                value = 0
                                adding = false
                            },
                        contentAlignment = Alignment.Center,
                    ) { PlayGlyph(Color(0xFF16233A), 38.dp) }
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            PlusButton(onClick = { adding = true; value = 0 })
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TimerCard(
    t: TimerInstance,
    nowMs: Long,
    onClose: () -> Unit,
    player: MediaPlayer?,
) {
    val remaining = t.remainingMs(nowMs)
    val finished = t.finished && remaining <= 0
    val compact = TimerStore.timers.size > 1
    val bg = if (finished) Color(0xFF6E86C0) else ClockSurface
    val onBg = if (finished) Color(0xFF16233A) else ClockText
    val secs = if (remaining >= 0) (remaining + 999) / 1000 else remaining / 1000
    val total = t.totalMs / 1000.0
    val fraction = ((remaining / 1000.0) / total).coerceIn(0.0, 1.0).toFloat()

    Card(modifier = Modifier.padding(horizontal = 24.dp), color = bg) {
        Column(modifier = Modifier.padding(0.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title(t.totalMs), color = onBg, fontSize = 26.sp, modifier = Modifier.weight(1f))
                Box(modifier = Modifier.size(34.dp).clickable { onClose() }, contentAlignment = Alignment.Center) {
                    CloseGlyph(onBg, 22.dp)
                }
            }
            if (compact) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TimerRing(fraction, finished, 170.dp, onBg) {
                        Text(secs.toString(), color = onBg, fontSize = 40.sp, fontWeight = FontWeight.Light)
                    }
                    Spacer(Modifier.width(20.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        AddMinuteButton(onBg) { extend(t, 60_000) }
                        Box(
                            modifier = Modifier.size(width = 120.dp, height = 62.dp)
                                .clip(RoundedCornerShape(31.dp))
                                .background(if (finished) Color(0xFFE7E9F2) else Color(0xFFEFC6D6))
                                .clickable { toggle(t, finished) },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (finished) StopGlyph(Color(0xFF5A2338), 26.dp)
                            else if (t.running) PauseGlyph(Color(0xFF5A2338), 26.dp)
                            else PlayGlyph(Color(0xFF5A2338), 26.dp)
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    TimerRing(fraction, finished, 300.dp, onBg) {
                        Text(secs.toString(), color = onBg, fontSize = 64.sp, fontWeight = FontWeight.Light)
                    }
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier.size(34.dp).clickable { reset(t) },
                        contentAlignment = Alignment.Center,
                    ) { ResetGlyph(onBg, 26.dp) }
                    Spacer(Modifier.height(18.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BigPill(text = "+1:00", bg = Color(0xFF3A3D45), fg = ClockText) { extend(t, 60_000) }
                        Spacer(Modifier.width(20.dp))
                        Box(
                            modifier = Modifier
                                .size(width = 150.dp, height = 78.dp)
                                .clip(RoundedCornerShape(39.dp))
                                .background(if (finished) Color(0xFFE7E9F2) else Color(0xFFEFC6D6))
                                .clickable { toggle(t, finished) },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (finished) StopGlyph(Color(0xFF5A2338), 30.dp)
                            else if (t.running) PauseGlyph(Color(0xFF5A2338), 30.dp)
                            else PlayGlyph(Color(0xFF5A2338), 30.dp)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun TimerRing(
    fraction: Float,
    finished: Boolean,
    size: androidx.compose.ui.unit.Dp,
    color: Color,
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = size.toPx() * 0.05f
            val inset = stroke / 2f
            drawArc(
                color = if (finished) Color(0xFF2E3852) else Color(0xFF3A3D45),
                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(this.size.width - stroke, this.size.height - stroke),
                style = Stroke(width = stroke),
            )
            drawArc(
                color = if (finished) Color(0xFF22304F) else ClockAccent,
                startAngle = -90f, sweepAngle = 360f * fraction, useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(this.size.width - stroke, this.size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        content()
    }
}

@Composable
private fun BigPill(text: String, bg: Color, fg: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(width = 150.dp, height = 78.dp).clip(RoundedCornerShape(39.dp)).background(bg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { Text(text, color = fg, fontSize = 22.sp) }
}

@Composable
private fun AddMinuteButton(color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(width = 120.dp, height = 50.dp).clip(RoundedCornerShape(25.dp))
            .background(Color(0xFF3A3D45)).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { Text("+1:00", color = ClockText, fontSize = 17.sp) }
}

private fun toggle(t: TimerInstance, finished: Boolean) {
    when {
        finished -> reset(t)
        t.running -> {
            t.pausedRemaining = t.remainingMs(System.currentTimeMillis())
            t.running = false
        }
        else -> {
            t.endAt = System.currentTimeMillis() + t.pausedRemaining
            t.running = true
        }
    }
}

private fun reset(t: TimerInstance) {
    t.running = false
    t.finished = false
    t.notified = false
    t.pausedRemaining = t.totalMs
    t.endAt = System.currentTimeMillis() + t.totalMs
}

private fun extend(t: TimerInstance, extraMs: Long) {
    val now = System.currentTimeMillis()
    val cur = if (t.running) t.endAt - now else t.pausedRemaining
    val next = cur + extraMs
    if (t.running) {
        t.endAt = now + next
    } else {
        t.pausedRemaining = next
    }
    t.finished = false
    t.notified = false
}

private fun title(totalMs: Long): String {
    val secs = totalMs / 1000
    val h = secs / 3600
    val m = (secs % 3600) / 60
    val s = secs % 60
    return when {
        h > 0 && m > 0 -> "${h}小时${m}分的定时器"
        h > 0 -> "${h}小时的定时器"
        m > 0 && s > 0 -> "${m}分${s}秒的定时器"
        m > 0 -> "${m}分钟的定时器"
        else -> "${s}秒的定时器"
    }
}

fun formatHms(seconds: Long): String {
    val h = (seconds / 3600).toInt()
    val m = ((seconds % 3600) / 60).toInt()
    val s = (seconds % 60).toInt()
    return "${h.two()}时 ${m.two()}分 ${s.two()}秒"
}

@Composable
private fun TimerKeypad(onDigit: (Int) -> Unit, onDoubleZero: () -> Unit, onBackspace: () -> Unit) {
    val rows = listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9))
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                row.forEach { d -> KeyCircle("$d") { onDigit(d) } }
            }
            Spacer(Modifier.height(16.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            KeyCircle("00") { onDoubleZero() }
            KeyCircle("0") { onDigit(0) }
            Box(
                modifier = Modifier.size(78.dp).clip(CircleShape).background(ClockSurfaceHigh).clickable { onBackspace() },
                contentAlignment = Alignment.Center,
            ) { BackspaceGlyph(ClockText, 28.dp) }
        }
    }
}

@Composable
private fun KeyCircle(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(78.dp).clip(CircleShape).background(ClockSurfaceHigh).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { Text(label, color = ClockText, fontSize = 26.sp) }
}
