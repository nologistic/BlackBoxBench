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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private data class IntroPage(val title: String, val body: String)

private val introPages = listOf(
    IntroPage("欢迎使用 Vinyl", "一款为本地音乐打造的简洁播放器。"),
    IntroPage("正在播放", "上滑正在播放界面内的卡片即可展开播放队列。"),
    IntroPage("播放队列", "通过拖动歌曲名前面的序列号来调整播放队列的顺序。")
)

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { introPages.size })
    val scope = rememberCoroutineScope()
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(VinylColors.OnboardingTop, VinylColors.OnboardingBottom)
                )
            )
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                IntroArt()
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) { page ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        introPages[page].title,
                        color = Color(0xFF1E1B2E),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        introPages[page].body,
                        color = Color(0xFF3A3550),
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp)
                    .height(48.dp)
                    .shadow(2.dp, RoundedCornerShape(4.dp))
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFEDE7F6))
                    .clickable { onDone() },
                contentAlignment = Alignment.Center
            ) {
                Text("GET STARTED", color = Color(0xFF3A3550), fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(20.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(introPages.size) { i ->
                    Box(
                        Modifier
                            .padding(horizontal = 5.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (i == pagerState.currentPage) Color.White else Color(0x66FFFFFF))
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
    // Swiping advances pages; tapping GET STARTED finishes the intro.
    LaunchedPageWatcher(pagerState)
}

@Composable
private fun LaunchedPageWatcher(state: androidx.compose.foundation.pager.PagerState) {
    // no-op placeholder that keeps the pager state hoisted for future use
}

@Composable
private fun IntroArt() {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.78f)
            .height(300.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val stroke = w * 0.018f
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(w * 0.02f, h * 0.02f),
                size = Size(w * 0.96f, h * 0.96f),
                cornerRadius = CornerRadius(w * 0.12f, w * 0.12f),
                style = Stroke(stroke * 1.6f)
            )
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(w * 0.10f, h * 0.10f),
                size = Size(w * 0.80f, h * 0.80f),
                cornerRadius = CornerRadius(w * 0.09f, w * 0.09f),
                style = Stroke(stroke)
            )
            drawCircle(Color(0xFFF3EEFF), radius = w * 0.14f, center = Offset(w * 0.55f, h * 0.34f))
        }
    }
}
