package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

object NowPlaying {
    val item = mutableStateOf<MediaItem?>(null)
    val playing = mutableStateOf(false)
    val position = mutableStateOf(0f)
    val subtitleTrack = mutableStateOf<String?>(null)
}

data class SubtitleCue(val start: Float, val end: Float, val text: String)

val DemoSubtitles: List<SubtitleCue> by lazy {
    val raw = listOf(
        1f to 4f to "黑盒媒体 / BlackBox Media 出品",
        5f to 9.5f to "这是一段虚构的演示视频\nThis is a fictional demo video",
        10f to 14.2f to "全片内容均可确定性重建\nEvery frame is deterministically reproducible",
        15f to 20f to "字幕支持双语与样式调整\nSubtitles support bilingual display and styling",
        21f to 25.5f to "试一试暂停——字幕会停在当前行\nTry pausing: the subtitle stays on the current line",
    )
    raw.map { (range, text) -> SubtitleCue(range.first, range.second, text) }
}

@Composable
fun VideoSurface(item: MediaItem, modifier: Modifier = Modifier) {
    val ratio = if (item.width > 0 && item.height > 0) item.width.toFloat() / item.height.toFloat() else 16f / 9f
    Box(modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxWidth().aspectRatio(ratio)) {
            TestPattern(item.pattern, Modifier.fillMaxSize())
        }
    }
}

private val TutorialSteps = listOf(
    "播放器控制按钮" to "使用这些按钮来控制播放。",
    "亮度" to "在屏幕左侧上滑或下滑以调整亮度。",
    "音量" to "在屏幕右侧上滑或下滑以调整音量。",
    "暂停" to "轻按两下可暂停。",
    "±10 秒" to "轻按两下可快进或快退 10 秒。",
    "定位" to "在屏幕上左滑或右滑以定位。",
)

@Composable
fun PlayerScreen(
    item: MediaItem,
    onExit: () -> Unit,
    onOpenInfo: () -> Unit,
) {
    val duration = item.durationSec.toFloat().coerceAtLeast(1f)
    val saved = Store.positionOf(item.id)
    var position by remember { mutableStateOf(if (saved in 0.5f..(duration - 0.4f)) saved else 0f) }
    var playing by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var hintVisible by remember { mutableStateOf(Store.tutorialPending) }
    var tutorialStep by remember { mutableStateOf(if (Store.tutorialPending) -1 else -2) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var brightness by remember { mutableStateOf(50) }
    var volume by remember { mutableStateOf(50) }
    var repeat by remember { mutableStateOf(false) }

    LaunchedEffect(item.id) {
        NowPlaying.item.value = item
        NowPlaying.playing.value = true
        playing = true
        Store.markPlayed(item.id)
    }
    val latestPosition by rememberUpdatedState(position)
    DisposableEffect(item.id) {
        onDispose {
            Store.setPosition(item.id, if (latestPosition >= duration - 0.4f) 0f else latestPosition)
            Store.flushPositions()
        }
    }
    LaunchedEffect(playing, hintVisible, tutorialStep) {
        if (!playing || hintVisible || tutorialStep >= 0) return@LaunchedEffect
        while (true) {
            delay(200)
            position += 0.2f
            NowPlaying.position.value = position
            Store.setPosition(item.id, position)
            if (position >= duration) {
                if (repeat) {
                    position = 0f
                } else {
                    Store.setPosition(item.id, 0f)
                    Store.flushPositions()
                    NowPlaying.playing.value = false
                    NowPlaying.item.value = null
                    onExit()
                    break
                }
            }
        }
    }
    LaunchedEffect(feedback) {
        if (feedback != null) {
            delay(1200)
            feedback = null
        }
    }
    LaunchedEffect(controlsVisible) {
        if (controlsVisible && !hintVisible && tutorialStep < 0) {
            delay(4000)
            controlsVisible = false
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(item.id) {
                    detectTapGestures(
                        onTap = { controlsVisible = !controlsVisible },
                        onDoubleTap = {
                            playing = !playing
                            NowPlaying.playing.value = playing
                            feedback = if (playing) "播放" else "暂停"
                        },
                    )
                }
                .pointerInput(item.id) {
                    var startX = 0f
                    var startY = 0f
                    var mode = 0
                    detectDragGestures(
                        onDragStart = { o -> startX = o.x; startY = o.y; mode = 0 },
                        onDragEnd = { feedback = null },
                        onDrag = { change, drag ->
                            if (mode == 0) {
                                mode = if (kotlin.math.abs(drag.x) > kotlin.math.abs(drag.y)) 1 else 2
                            }
                            if (mode == 1) {
                                val delta = drag.x / 8f
                                position = (position + delta).coerceIn(0f, duration)
                                NowPlaying.position.value = position
                                feedback = if (delta > 0) "${delta.toInt()} 秒 ››" else "‹‹ ${(-delta).toInt()} 秒"
                            } else {
                                val step = (-drag.y / 6f).toInt()
                                if (startX < size.width / 2f) {
                                    brightness = (brightness + step).coerceIn(0, 100)
                                    feedback = "亮度 $brightness%"
                                } else {
                                    volume = (volume + step).coerceIn(0, 100)
                                    feedback = "音量 $volume%"
                                }
                            }
                            change.consume()
                        },
                    )
                },
        ) {
            VideoSurface(item, Modifier.fillMaxSize())
        }

        // Subtitles
        val track = NowPlaying.subtitleTrack.value
        if (track != null) {
            val cue = DemoSubtitles.firstOrNull { position >= it.start && position <= it.end }
            if (cue != null) {
                Text(
                    cue.text,
                    color = Color.White,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 130.dp, start = 24.dp, end = 24.dp),
                )
            }
        }

        if (controlsVisible) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color(0x99000000))
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconTap(VlcIcon.BACK, 24.dp, Color.White, onExit)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.title, color = Color.White, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(item.path, color = Color(0xFFCCCCCC), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        if (feedback != null) {
            Box(
                Modifier.align(Alignment.Center).clip(RoundedCornerShape(10.dp)).background(Color(0xB3000000)).padding(horizontal = 20.dp, vertical = 12.dp),
            ) { Text(feedback!!, color = Color.White, fontSize = 15.sp) }
        }

        if (hintVisible) {
            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 56.dp, start = 16.dp, end = 16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xE6FFFFFF))
                    .padding(16.dp),
            ) {
                Text("目前处于全屏模式", color = Color(0xFF141414), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                Text("若要退出，请从屏幕顶部向下滑动", color = Color(0xFF333333), fontSize = 14.sp)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text(
                        "知道了",
                        color = Color(0xFFFF8800),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                hintVisible = false
                                Store.tutorialPending = false
                                tutorialStep = 0
                            }
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                    )
                }
            }
        }

        if (tutorialStep in TutorialSteps.indices) {
            val (title, body) = TutorialSteps[tutorialStep]
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 150.dp, start = 16.dp, end = 16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xE6FFFFFF))
                    .padding(16.dp),
            ) {
                Text(title, color = Color(0xFF141414), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                Text(body, color = Color(0xFF333333), fontSize = 14.sp)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text(
                        "关闭",
                        color = Color(0xFF666666),
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { tutorialStep = -2 }
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "继续",
                        color = Color(0xFFFF8800),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                tutorialStep = if (tutorialStep >= TutorialSteps.size - 1) -2 else tutorialStep + 1
                            }
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                    )
                }
            }
        }

        if (controlsVisible) {
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(2.dp).background(Color(0x55FFFFFF))) {
                    Box(Modifier.fillMaxWidth(position / duration).height(2.dp).background(Color(0xFFFF8800)))
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    IconTap(VlcIcon.QUEUE, 24.dp, Color.White, {})
                    IconTap(VlcIcon.SHUFFLE, 24.dp, Color.White, {})
                    Box(
                        Modifier.size(58.dp).clip(CircleShape).background(Color(0xFFFF8800)).clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            playing = !playing
                            NowPlaying.playing.value = playing
                        },
                        contentAlignment = Alignment.Center,
                    ) { VlcIconView(if (playing) VlcIcon.PAUSE else VlcIcon.PLAY, 28.dp, Color.White) }
                    IconTap(VlcIcon.REPEAT, 24.dp, Color.White) { repeat = !repeat }
                    IconTap(VlcIcon.MORE, 24.dp, Color.White) { showMenu = true }
                }
            }
        }

        if (showMenu) {
            PlayerMenu(
                onDismiss = { showMenu = false },
                onInfo = { showMenu = false; onOpenInfo() },
            )
        }
    }
}

@Composable
private fun PlayerMenu(onDismiss: () -> Unit, onInfo: () -> Unit) {
    var page by remember { mutableStateOf(0) }
    BottomSheet(onDismiss, maxHeight = 520.dp) {
        when (page) {
            0 -> {
                SheetTitle("播放器")
                val track = NowPlaying.subtitleTrack.value
                SheetAction(if (track == null) "字幕" else "字幕：$track", VlcIcon.SUBTITLES) { page = 1 }
                SheetAction("音频轨道", VlcIcon.MUSIC) { page = 2 }
                SheetAction("播放速度", VlcIcon.CLOCK) { page = 3 }
                SheetAction("均衡器", VlcIcon.EQUALIZER) { onDismiss() }
                SheetAction("信息", VlcIcon.INFO) { onInfo() }
            }
            1 -> {
                SheetTitle("字幕")
                SheetAction("禁用", VlcIcon.CLOSE) { NowPlaying.subtitleTrack.value = null; onDismiss() }
                SheetAction("subtitle_sample.srt", VlcIcon.SUBTITLES) {
                    NowPlaying.subtitleTrack.value = "subtitle_sample.srt"
                    onDismiss()
                }
            }
            2 -> {
                SheetTitle("音频轨道")
                SheetAction("1: 音频", VlcIcon.MUSIC) { onDismiss() }
                SheetAction("禁用", VlcIcon.CLOSE) { onDismiss() }
            }
            else -> {
                SheetTitle("播放速度")
                listOf("0.5x", "0.75x", "1.0x", "1.25x", "1.5x", "2.0x").forEach { speed ->
                    SheetAction(speed, VlcIcon.PLAY) { onDismiss() }
                }
            }
        }
    }
}

// ------------------------------------------------------------ audio player

@Composable
fun AudioPlayerScreen(item: MediaItem, onBack: () -> Unit) {
    var position by remember { mutableStateOf(0f) }
    var playing by remember { mutableStateOf(true) }
    val duration = 60f
    LaunchedEffect(item.id) {
        NowPlaying.item.value = item
        NowPlaying.playing.value = true
        Store.markPlayed(item.id)
    }
    LaunchedEffect(playing) {
        NowPlaying.playing.value = playing
        if (!playing) return@LaunchedEffect
        while (true) {
            delay(250)
            position = (position + 0.25f) % duration
            NowPlaying.position.value = position
        }
    }
    Column(Modifier.fillMaxSize().background(Color(0xFF17121C))) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconTap(VlcIcon.BACK, 24.dp, Color.White, onBack)
            Spacer(Modifier.width(10.dp))
            Text("正在播放", color = Color.White, fontSize = 17.sp, modifier = Modifier.weight(1f))
            IconTap(VlcIcon.MORE, 24.dp, Color.White, {})
        }
        Spacer(Modifier.weight(1f))
        Box(
            Modifier.fillMaxWidth().padding(horizontal = 40.dp).aspectRatio(1f).clip(RoundedCornerShape(8.dp)).background(Color(0xFF2A2333)),
            contentAlignment = Alignment.Center,
        ) { VlcIconView(VlcIcon.MUSIC, 96.dp, Color(0xFF9E8FB0)) }
        Spacer(Modifier.height(28.dp))
        Text(item.fileName, color = Color.White, fontSize = 19.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text("未知艺术家", color = Color(0xFFBBBBBB), fontSize = 14.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(2.dp).background(Color(0x55FFFFFF))) {
            Box(Modifier.fillMaxWidth(position / duration).height(2.dp).background(Color(0xFFFF8800)))
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            IconTap(VlcIcon.SHUFFLE, 24.dp, Color.White, {})
            IconTap(VlcIcon.REPEAT, 24.dp, Color.White, {})
            Box(
                Modifier.size(64.dp).clip(CircleShape).background(Color(0xFFFF8800)).clickable(
                    interactionSource = remember { MutableInteractionSource() }, indication = null,
                ) { playing = !playing },
                contentAlignment = Alignment.Center,
            ) { VlcIconView(if (playing) VlcIcon.PAUSE else VlcIcon.PLAY, 32.dp, Color.White) }
            IconTap(VlcIcon.QUEUE, 24.dp, Color.White, {})
            IconTap(VlcIcon.EQUALIZER, 24.dp, Color.White, {})
        }
        Spacer(Modifier.weight(1f))
    }
}
