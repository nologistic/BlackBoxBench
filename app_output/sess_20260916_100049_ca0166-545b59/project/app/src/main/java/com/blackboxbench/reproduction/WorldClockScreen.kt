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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import java.time.LocalDateTime

private fun loadCities(context: android.content.Context): List<WorldCity> {
    return runCatching {
        val text = AssetStore.readText(context, "world_clocks.json")
        val arr = JSONObject(text).getJSONArray("cities")
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            WorldCity(o.getString("display"), o.getDouble("utc_offset"))
        }
    }.getOrDefault(emptyList())
}

@Composable
fun WorldClockScreen(store: ClockStore, now: LocalDateTime, onBack: () -> Unit) {
    val context = LocalContext.current
    val cities = remember { loadCities(context) }
    var query by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }

    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    val filtered = cities.filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(40.dp).clickable { onBack() }, contentAlignment = Alignment.Center) {
                BackGlyph(ClockText, 26.dp)
            }
            Spacer(Modifier.width(6.dp))
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = TextStyle(color = ClockText, fontSize = 20.sp),
                cursorBrush = SolidColor(ClockAccent),
                modifier = Modifier.weight(1f).focusRequester(focus),
                decorationBox = { inner ->
                    Box {
                        if (query.isEmpty()) Text("搜索城市", color = ClockTextDim, fontSize = 20.sp)
                        inner()
                    }
                },
            )
            if (query.isNotEmpty()) {
                Box(modifier = Modifier.size(40.dp).clickable { query = "" }, contentAlignment = Alignment.Center) {
                    CloseGlyph(ClockText, 20.dp)
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 12.dp)) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(ClockSurfaceHigh),
                contentAlignment = Alignment.Center,
            ) { Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(ClockTextDim)) }
            Spacer(Modifier.height(18.dp))
            Text("联网即可查看更多城市", color = ClockText, fontSize = 26.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                "要查看所有适用的城市，请连接到互联网。否则您只能看到有限的城市列表。",
                color = ClockTextDim, fontSize = 16.sp,
            )
        }

        Spacer(Modifier.height(8.dp))
        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                Text("搜索城市", color = ClockTextDim, fontSize = 18.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filtered) { city ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { store.addCity(city.name, city.offset); onBack() }
                            .padding(horizontal = 26.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(city.name, color = ClockText, fontSize = 19.sp)
                        }
                        Text("添加", color = ClockAccent, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AlarmSoundScreen(store: ClockStore, alarmId: Int, onBack: () -> Unit) {
    val alarm = store.alarms.firstOrNull { it.id == alarmId } ?: return onBack()
    var musicDialog by remember { mutableStateOf(true) }
    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(title = "闹钟提示音", onBack = onBack, onMore = {})
        if (musicDialog) {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(ClockSurface).padding(20.dp)) {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("可使用音乐闹钟", color = ClockText, fontSize = 26.sp)
                        Spacer(Modifier.height(10.dp))
                        Text("选择要用来播放音乐闹钟的应用", color = ClockTextDim, fontSize = 16.sp)
                        Spacer(Modifier.height(18.dp))
                        listOf("Pandora Music", "Calm", "Spotify Music", "YouTube Music").forEach { app ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(modifier = Modifier.size(30.dp).clip(CircleShape).background(ClockSurfaceHigh))
                                Spacer(Modifier.width(16.dp))
                                Text(app, color = ClockText, fontSize = 19.sp, modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("从 Google Play 下载", color = ClockTextDim, fontSize = 16.sp, modifier = Modifier.weight(1f))
                            OutlineButton("关闭", onClick = { musicDialog = false })
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(ClockAccentDim)
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            ) { Text("提示音", color = ClockText, fontSize = 16.sp) }
            Spacer(Modifier.width(16.dp))
            Text("YouTube Music", color = ClockTextDim, fontSize = 16.sp)
        }
        Spacer(Modifier.height(18.dp))
        Text("您的提示音", color = ClockTextDim, fontSize = 15.sp, modifier = Modifier.padding(start = 26.dp))
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(ClockAccentDim),
                contentAlignment = Alignment.Center,
            ) { PlusGlyph(ClockText, 20.dp) }
            Spacer(Modifier.width(20.dp))
            Text("新增", color = ClockText, fontSize = 19.sp)
        }
        Spacer(Modifier.height(16.dp))
        Text("设备提示音", color = ClockTextDim, fontSize = 15.sp, modifier = Modifier.padding(start = 26.dp))
        Ringtones.names.forEach { name ->
            val selected = alarm.ringtone == name
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { store.updateAlarm(alarm.copy(ringtone = name)) }
                    .padding(horizontal = 26.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                    if (name == Ringtones.SILENT) CloseGlyph(ClockText, 20.dp) else BellGlyph(ClockText, 24.dp)
                }
                Spacer(Modifier.width(20.dp))
                Text(name, color = ClockText, fontSize = 19.sp, modifier = Modifier.weight(1f))
                if (selected) CheckCircle(true) {}
            }
        }
    }
}
