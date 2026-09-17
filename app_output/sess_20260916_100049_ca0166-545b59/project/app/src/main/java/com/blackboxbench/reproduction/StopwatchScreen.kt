package com.blackboxbench.reproduction

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

data class Lap(val totalMs: Long, val lapMs: Long)

object StopwatchStore {
    var running by mutableStateOf(false)
    var accumulatedMs by mutableStateOf(0L)
    var startAt by mutableStateOf(0L)
    val laps = mutableStateListOf<Lap>()

    fun elapsed(now: Long): Long = if (running) accumulatedMs + (now - startAt) else accumulatedMs

    fun start(now: Long) {
        if (running) return
        startAt = now
        running = true
    }

    fun pause(now: Long) {
        if (!running) return
        accumulatedMs += now - startAt
        running = false
    }

    fun lap(now: Long) {
        if (!running) return
        val total = elapsed(now)
        val last = laps.firstOrNull()?.totalMs ?: 0L
        laps.add(0, Lap(total, total - last))
    }

    fun reset() {
        running = false
        accumulatedMs = 0
        startAt = 0
        laps.clear()
    }
}

@Composable
fun StopwatchScreen() {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(33)
        }
    }

    val elapsed = StopwatchStore.elapsed(now)

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(title = "秒表", onMore = {})

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(330.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = size.minDimension * 0.02f
                    drawCircle(
                        color = Color(0xFF3A3D45),
                        radius = size.minDimension / 2f - stroke,
                        style = Stroke(width = stroke),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val secs = elapsed / 1000
                    val mins = secs / 60
                    val remSecs = secs % 60
                    val centis = (elapsed % 1000) / 10
                    val top = if (mins > 0) "$mins:${remSecs.toString().two()}" else remSecs.toString().two()
                    Text(top, color = ClockText, fontSize = 76.sp, fontWeight = FontWeight.Light)
                    Text(centis.toString().two(), color = ClockText, fontSize = 44.sp, fontWeight = FontWeight.Light)
                }
            }
        }

        if (StopwatchStore.laps.isNotEmpty()) {
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                StopwatchStore.laps.forEachIndexed { index, lap ->
                    val number = StopwatchStore.laps.size - index
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("# $number", color = ClockTextDim, fontSize = 19.sp, modifier = Modifier.width(70.dp))
                        Text(formatLap(lap.lapMs), color = ClockText, fontSize = 19.sp, modifier = Modifier.weight(1f))
                        Text(formatLap(lap.totalMs), color = ClockText, fontSize = 19.sp)
                    }
                }
            }
        } else {
            Spacer(Modifier.weight(1f))
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (elapsed > 0) {
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(Color(0xFF7A3B54)).clickable {
                        StopwatchStore.reset()
                    },
                    contentAlignment = Alignment.Center,
                ) { ResetGlyph(ClockText, 28.dp) }
            } else {
                Spacer(Modifier.size(64.dp))
            }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(width = 150.dp, height = 78.dp)
                    .clip(RoundedCornerShape(39.dp))
                    .background(ClockAccent)
                    .clickable {
                        val t = System.currentTimeMillis()
                        if (StopwatchStore.running) StopwatchStore.pause(t) else StopwatchStore.start(t)
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (StopwatchStore.running) PauseGlyph(Color(0xFF16233A), 30.dp)
                else PlayGlyph(Color(0xFF16233A), 30.dp)
            }
            Spacer(Modifier.weight(1f))
            if (StopwatchStore.running) {
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(Color(0xFF7A3B54)).clickable {
                        StopwatchStore.lap(System.currentTimeMillis())
                    },
                    contentAlignment = Alignment.Center,
                ) { LapGlyph(ClockText, 28.dp) }
            } else {
                Spacer(Modifier.size(64.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

private fun formatLap(ms: Long): String {
    val totalSecs = ms / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    val centis = (ms % 1000) / 10
    return "$mins ${secs.toString().two()}.${centis.toString().two()}"
}
