package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun VideoPlayerScreen(initialProgress: Float, onProgress: (Float) -> Unit, onBack: () -> Unit) {
    var playing by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(initialProgress.coerceIn(0f, .98f)) }
    var subtitles by remember { mutableStateOf(true) }
    var speed by remember { mutableStateOf(1f) }
    var gestureHint by remember { mutableStateOf("") }
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }
    var startX by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(playing) {
        while (playing) {
            delay(180)
            progress += .06f * speed
            if (progress >= 1f) { progress = 0f; playing = false }
            onProgress(progress)
        }
    }
    LaunchedEffect(gestureHint) {
        if (gestureHint.isNotEmpty()) { delay(900); gestureHint = "" }
    }

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("‹", color = Color.White, fontSize = 48.sp, modifier = Modifier.clickable(onClick = onBack))
            Text("sample_video", color = Color.White, fontSize = 20.sp, modifier = Modifier.padding(start = 12.dp))
            Spacer(Modifier.weight(1f))
            Text("⋮", color = Color.White, fontSize = 29.sp)
        }
        Box(
            Modifier.fillMaxWidth().weight(1f).clickable { playing = !playing },
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color(0xFF6C5872))) {
                Text("STORY LOOP", color = Color.White, fontSize = 15.sp, modifier = Modifier.padding(16.dp))
                Box(Modifier.align(Alignment.CenterStart).padding(start = 22.dp).size(width = 86.dp, height = 58.dp).background(Color(0xFF8DB8AA), RoundedCornerShape(14.dp)))
                Text("Synthetic local fixture", color = Color.White, fontSize = 12.sp, modifier = Modifier.align(Alignment.BottomStart).padding(16.dp))
                Box(Modifier.align(Alignment.Center).size(70.dp).background(Color.Black.copy(alpha = .42f), RoundedCornerShape(35.dp)).clickable { playing = !playing }, contentAlignment = Alignment.Center) {
                    Text(if (playing) "Ⅱ" else "▶", color = Color.White, fontSize = 32.sp)
                }
                if (subtitles) Text("示例字幕 · Sample subtitle", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp).background(Color.Black.copy(alpha = .65f), RoundedCornerShape(3.dp)).padding(horizontal = 8.dp, vertical = 3.dp))
            }
            if (gestureHint.isNotEmpty()) Text(gestureHint, color = Color.White, fontSize = 20.sp, modifier = Modifier.background(Color.Black.copy(alpha = .72f), RoundedCornerShape(8.dp)).padding(horizontal = 18.dp, vertical = 12.dp))
        }
        Column(Modifier.fillMaxWidth().background(Color(0xFF111111)).padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatVideoTime(progress), color = Color.White, fontSize = 13.sp)
                Slider(
                    value = progress,
                    onValueChange = { progress = it; onProgress(it) },
                    modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                    colors = SliderDefaults.colors(thumbColor = VlcOrange, activeTrackColor = VlcOrange)
                )
                Text("0:03", color = Color.White, fontSize = 13.sp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                Text(if (subtitles) "字幕开" else "字幕关", color = Color.White, fontSize = 15.sp, modifier = Modifier.clickable { subtitles = !subtitles }.padding(10.dp))
                Text("↶10", color = Color.White, fontSize = 18.sp, modifier = Modifier.clickable { progress = (progress - .25f).coerceAtLeast(0f); onProgress(progress) }.padding(10.dp))
                Text(if (playing) "Ⅱ" else "▶", color = Color.White, fontSize = 31.sp, modifier = Modifier.clickable { playing = !playing }.padding(10.dp))
                Text("10↷", color = Color.White, fontSize = 18.sp, modifier = Modifier.clickable { progress = (progress + .25f).coerceAtMost(1f); onProgress(progress) }.padding(10.dp))
                Text(speedText(speed), color = Color.White, fontSize = 16.sp, modifier = Modifier.clickable { speed = nextSpeed(speed) }.padding(10.dp))
            }
        }
    }
}

private fun formatVideoTime(progress: Float): String {
    val sec = (progress * 3).toInt().coerceIn(0, 3)
    return "0:0" + sec
}

private fun nextSpeed(current: Float): Float = when (current) {
    .5f -> 1f
    1f -> 1.5f
    1.5f -> 2f
    else -> .5f
}

private fun speedText(speed: Float): String = if (speed == 1f) "1×" else speed.toString() + "×"

@Composable
fun AudioPlayerScreen(favorite: Boolean, onFavorite: () -> Unit, onBack: () -> Unit) {
    var playing by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var shuffle by remember { mutableStateOf(false) }
    var repeat by remember { mutableStateOf(0) }
    var speed by remember { mutableStateOf(1f) }

    LaunchedEffect(playing) {
        while (playing) {
            delay(250)
            progress += .0125f * speed
            if (progress >= 1f) {
                progress = if (repeat > 0) 0f else 1f
                if (repeat == 0) playing = false
            }
        }
    }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        TopBar("正在播放", onBack) {
            Text("⋮", fontSize = 28.sp, color = Color(0xFF666666), modifier = Modifier.padding(8.dp))
        }
        Column(Modifier.fillMaxSize().padding(horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(46.dp))
            Box(Modifier.size(260.dp).background(Color(0xFFF2F2F2), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Text("♫", fontSize = 120.sp, color = Color(0xFF666666))
            }
            Spacer(Modifier.height(28.dp))
            Text("sample_audio.wav", fontSize = 24.sp, fontWeight = FontWeight.Medium)
            Text("Unknown Artist", color = Color.Gray, fontSize = 16.sp)
            Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (favorite) "♥" else "♡", fontSize = 29.sp, color = if (favorite) VlcOrange else Color(0xFF444444), modifier = Modifier.clickable { onFavorite() })
                Text(speedText(speed), fontSize = 17.sp, color = VlcOrange, modifier = Modifier.clickable { speed = nextSpeed(speed) }.padding(6.dp))
            }
            Slider(value = progress, onValueChange = { progress = it }, colors = SliderDefaults.colors(thumbColor = VlcOrange, activeTrackColor = VlcOrange))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatAudioTime(progress), color = Color.Gray, fontSize = 13.sp)
                Text("0:20", color = Color.Gray, fontSize = 13.sp)
            }
            Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                Text("⤨", fontSize = 25.sp, color = if (shuffle) VlcOrange else Color(0xFF555555), modifier = Modifier.clickable { shuffle = !shuffle }.padding(10.dp))
                Text("|◀", fontSize = 25.sp, modifier = Modifier.clickable { progress = 0f }.padding(10.dp))
                Box(Modifier.size(66.dp).background(VlcOrange, RoundedCornerShape(33.dp)).clickable { playing = !playing }, contentAlignment = Alignment.Center) {
                    Text(if (playing) "Ⅱ" else "▶", color = Color.White, fontSize = 30.sp)
                }
                Text("▶|", fontSize = 25.sp, modifier = Modifier.clickable { progress = 0f }.padding(10.dp))
                Text(if (repeat == 2) "↻¹" else "↻", fontSize = 25.sp, color = if (repeat > 0) VlcOrange else Color(0xFF555555), modifier = Modifier.clickable { repeat = (repeat + 1) % 3 }.padding(10.dp))
            }
            Spacer(Modifier.height(40.dp))
            Text("当前播放列表", color = VlcOrange, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("♫", fontSize = 32.sp, color = Color.Gray, modifier = Modifier.width(46.dp))
                Column(Modifier.weight(1f)) { Text("sample_audio.wav", fontSize = 17.sp); Text("正在播放", color = VlcOrange, fontSize = 13.sp) }
                Text("≡", color = Color.Gray, fontSize = 24.sp)
            }
        }
    }
}

private fun formatAudioTime(progress: Float): String {
    val sec = (progress * 20).toInt().coerceIn(0, 20)
    return "0:" + sec.toString().padStart(2, '0')
}
