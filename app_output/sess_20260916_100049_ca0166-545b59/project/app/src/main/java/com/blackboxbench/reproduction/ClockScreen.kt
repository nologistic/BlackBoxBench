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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDateTime

@Composable
fun ClockScreen(
    store: ClockStore,
    now: LocalDateTime,
    onOpenSettings: () -> Unit,
    onOpenWorldClock: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val next = nextEnabledAlarm(store.alarms, now)

    Column(modifier = Modifier.fillMaxSize()) {
        Box {
            TopBar(title = "时钟", onMore = { menuOpen = true })
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("屏保", color = ClockText) }, onClick = { menuOpen = false })
                DropdownMenuItem(text = { Text("设置", color = ClockText) }, onClick = { menuOpen = false; onOpenSettings() })
                DropdownMenuItem(text = { Text("隐私权政策", color = ClockText) }, onClick = { menuOpen = false })
                DropdownMenuItem(text = { Text("发送反馈", color = ClockText) }, onClick = { menuOpen = false })
                DropdownMenuItem(text = { Text("帮助", color = ClockText) }, onClick = { menuOpen = false })
            }
        }

        Spacer(Modifier.height(10.dp))

        if (store.settings.analog) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                AnalogClock(now)
            }
        } else {
            Text(
                text = if (store.settings.showSeconds)
                    "${now.hour.two()}:${now.minute.two()}:${now.second.two()}"
                else "${now.hour.two()}:${now.minute.two()}",
                color = ClockText,
                fontSize = 76.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(start = 34.dp),
            )
        }

        Row(
            modifier = Modifier.padding(start = 34.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${now.monthValue}月${now.dayOfMonth}日周${Weekdays.dayOfWeekChar(now.dayOfWeek.value)}",
                color = ClockText,
                fontSize = 21.sp,
            )
            if (next != null) {
                Spacer(Modifier.width(14.dp))
                BellGlyph(ClockText, 20.dp)
                Spacer(Modifier.width(8.dp))
                Text(relativeLabel(next.second, now), color = ClockText, fontSize = 21.sp)
            }
        }

        // Home time is only shown when the device timezone differs from the home timezone.
        if (store.settings.autoHomeTime && store.settings.homeOffset != 8.0) {
            val home = now.plusMinutes(((store.settings.homeOffset - 8.0) * 60).toLong())
            Row(
                modifier = Modifier.padding(start = 34.dp, top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.size(14.dp).clip(RoundedCornerShape(7.dp)).background(ClockAccent))
                Spacer(Modifier.width(8.dp))
                Text(
                    "${store.settings.homeCity} ${home.hour.two()}:${home.minute.two()}",
                    color = ClockTextDim, fontSize = 18.sp,
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        store.worldCities.forEach { city ->
            WorldCityRow(city, now)
        }

        Spacer(Modifier.weight(1f))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            PlusButton(onClick = onOpenWorldClock)
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun WorldCityRow(city: WorldCity, now: LocalDateTime) {
    // Device is assumed to be at UTC+8 (GMT+8:00 北京), matching the observed target.
    val deviceOffset = 8.0
    val target = now.plusMinutes(((city.offset - deviceOffset) * 60).toLong())
    val diff = city.offset - deviceOffset
    val diffText = when {
        diff == 0.0 -> "本地时间"
        diff > 0 -> if (diff % 1.0 == 0.0) "晚 ${diff.toInt()} 小时" else "晚 ${diff} 小时"
        else -> {
            val d = -diff
            if (d % 1.0 == 0.0) "早 ${d.toInt()} 小时" else "早 $d 小时"
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(city.name, color = ClockText, fontSize = 30.sp)
            Text(
                "今天 ${target.hour.two()}:${target.minute.two()}",
                color = ClockTextDim, fontSize = 15.sp,
            )
        }
        Text(diffText, color = ClockTextDim, fontSize = 15.sp)
    }
}

@Composable
private fun AnalogClock(now: LocalDateTime) {
    val minuteAngle = (now.minute + now.second / 60f) * 6f
    val hourAngle = (now.hour % 12 + now.minute / 60f) * 30f
    val secondAngle = now.second * 6f
    Box(modifier = Modifier.size(300.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            drawCircle(color = Color(0xFFF1F1F3), radius = r * 0.92f)
            val c = Offset(size.width / 2f, size.height / 2f)
            fun hand(angleDeg: Float, len: Float, width: Float, color: Color) {
                val a = Math.toRadians((angleDeg - 90f).toDouble())
                val end = Offset(
                    c.x + (Math.cos(a) * len * r).toFloat(),
                    c.y + (Math.sin(a) * len * r).toFloat(),
                )
                drawLine(color, c, end, strokeWidth = width, cap = StrokeCap.Round)
            }
            hand(hourAngle, 0.50f, r * 0.09f, Color(0xFF5B6FA8))
            hand(minuteAngle, 0.72f, r * 0.06f, Color(0xFF3B3F46))
            hand(secondAngle, 0.80f, r * 0.02f, Color(0xFFB5708C))
            drawCircle(color = Color(0xFF8A5A6E), radius = r * 0.05f, center = Offset(
                c.x + (Math.cos(Math.toRadians((secondAngle - 90f).toDouble())) * 0.80f * r).toFloat(),
                c.y + (Math.sin(Math.toRadians((secondAngle - 90f).toDouble())) * 0.80f * r).toFloat(),
            ))
        }
    }
}
