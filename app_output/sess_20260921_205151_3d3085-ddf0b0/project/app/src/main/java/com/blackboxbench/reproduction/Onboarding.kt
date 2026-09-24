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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class IntroPage(val title: String, val body: String)

private val pages = listOf(
    IntroPage("主界面", "在文件浏览器中管理你的笔记，长按可多选，点 + 新建文件。"),
    IntroPage("查看", "编辑与预览一键切换，支持 Markdown、todo.txt 等纯文本格式。"),
    IntroPage("分享 -> Markor", "从其他应用分享文本到 Markor，快速保存为笔记。"),
    IntroPage("To-Do", "内置 todo.txt 待办清单，支持优先级、项目与情境。"),
    IntroPage("QuickNote", "快速笔记随时记录，自动保存，无需手动操作。"),
)

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    var showPermission by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    pages[page].title,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MarkorRed,
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    pages[page].body,
                    fontSize = 16.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.padding(8.dp)) {
            repeat(pages.size) { i ->
                val active = pagerState.currentPage == i
                Box(
                    Modifier
                        .padding(4.dp)
                        .size(if (active) 10.dp else 8.dp)
                        .background(if (active) MarkorRed else Color.LightGray, CircleShape)
                )
            }
        }
        if (pagerState.currentPage == pages.lastIndex) {
            Button(
                onClick = { showPermission = true },
                colors = ButtonDefaults.buttonColors(containerColor = MarkorRed),
            ) { Text("完成", color = Color.White) }
        }
        Spacer(Modifier.height(16.dp))
    }

    if (showPermission) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("需要存储权限") },
            text = { Text("Markor 需要存储权限读写文件。不授权将无法保存笔记。") },
            confirmButton = {
                TextButton(onClick = onDone) { Text("确定", color = MarkorRed) }
            },
            dismissButton = {
                TextButton(onClick = { }) { Text("退出", color = MarkorRed) }
            },
        )
    }
}
