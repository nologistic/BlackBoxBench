package com.blackboxbench.reproduction

import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val Accent = Color(0xFF00897B)

/** 三页引导：欢迎 -> 权限说明 -> 完成，进入主界面。 */
@Composable
fun Onboarding(onFinished: () -> Unit) {
    val pager = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        ) {
            VinylDisc(
                album = listOf("V", "♪", "✓")[page],
                sizeDp = 180,
                spinning = page == 1,
            )
            Spacer(Modifier.height(40.dp))
            Text(
                text = listOf(
                    "Vinyl Music Player",
                    "关于你的权限",
                    "一切就绪",
                )[page],
                fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = listOf(
                    "一款简洁的本地音乐播放器。\n浏览艺术家、专辑与播放列表，\n让黑胶重新转动起来。",
                    "需要访问音频文件以读取本地曲库，\n以及录音权限用于歌曲识别。\n你可以随时在系统设置中更改。",
                    "曲库已准备就绪。\n开始聆听你的音乐吧。",
                )[page],
                fontSize = 15.sp, color = Color(0xFF616161), textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(48.dp))
            Button(
                onClick = {
                    if (page < 2) scope.launch { pager.animateScrollToPage(page + 1) } else onFinished()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text(
                    listOf("开始使用", "允许", "进入音乐库")[page],
                    fontSize = 16.sp, color = Color.White,
                )
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(bottom = 48.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) { i ->
                Box(
                    Modifier
                        .size(if (pager.currentPage == i) 10.dp else 8.dp, )
                        .background(
                            if (pager.currentPage == i) Accent else Color(0xFFBDBDBD),
                            CircleShape,
                        )
                )
            }
        }
    }
}
