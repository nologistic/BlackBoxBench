package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
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

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopBar("", onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            TrafficCone(Modifier.size(128.dp))
            Text("VLC for Android", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(
                "VLC for Android™ 是广受欢迎的开源媒体播放器「VLC media player」的移植版本。Android™ 版本可以读取几乎所有文件以及网络串流。",
                fontSize = 16.sp, color = Color(0xFF6F6F6F), textAlign = TextAlign.Center, lineHeight = 22.sp,
                modifier = Modifier.padding(top = 14.dp, bottom = 18.dp)
            )
            Row(Modifier.fillMaxWidth().background(Color(0xFFF2F2F2), RoundedCornerShape(5.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("3.7.1", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("  2026-05-09", color = Color.Gray, fontSize = 16.sp)
            }
            AboutRow("◎", "官方网站")
            AboutRow("▣", "发送反馈")
            AboutRow("</>", "源代码")
            AboutRow("<>", "库")
            AboutRow("♣", "作者")
            Column(Modifier.fillMaxWidth().background(Color(0xFFF2F2F2), RoundedCornerShape(5.dp)).padding(16.dp)) {
                Text("Copyleft © 1996–2026 by VideoLAN", fontSize = 17.sp, fontWeight = FontWeight.Medium)
                Text("GNU General Public License v2.0", fontSize = 14.sp, color = Color.Gray)
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun AboutRow(symbol: String, title: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(symbol, fontSize = 23.sp, modifier = Modifier.width(50.dp))
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun StreamScreen(incognito: Boolean, onIncognito: () -> Unit, onBack: () -> Unit) {
    var address by remember { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        TopBar("串流", onBack) {
            Box {
                Text("⋮", fontSize = 28.sp, modifier = Modifier.clickable { menu = true }.padding(9.dp))
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Row { Text("无痕模式", fontSize = 17.sp); Spacer(Modifier.width(54.dp)); Text(if (incognito) "☑" else "☐", color = if (incognito) VlcOrange else Color.Gray) } },
                        onClick = { onIncognito(); menu = false }
                    )
                    DropdownMenuItem(text = { Text("刷新", fontSize = 17.sp) }, onClick = { message = ""; menu = false })
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = address,
                onValueChange = { address = it; message = "" },
                placeholder = { Text("请输入网络地址: 如 http://、mms://、rtsp://") },
                modifier = Modifier.weight(1f),
                minLines = 3
            )
            Box(Modifier.padding(start = 10.dp).size(48.dp).background(VlcOrange, RoundedCornerShape(24.dp)).clickable {
                message = if (address.isBlank()) "请输入有效的网络地址" else "设备处于离线状态，无法打开串流"
            }, contentAlignment = Alignment.Center) { Text("➜", color = Color.White, fontSize = 25.sp) }
        }
        if (message.isNotBlank()) Text(message, color = VlcOrange, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 20.dp))
        Text("支持 HTTP、MMS、RTSP 等网络串流地址", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(20.dp))
    }
}

@Composable
fun HistoryScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    var search by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var cleared by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        if (!search) {
            TopBar("历史", onBack) {
                Text("⌕", fontSize = 35.sp, color = Color(0xFF666666), modifier = Modifier.clickable { search = true }.padding(7.dp))
                Text("▰", fontSize = 25.sp, color = Color(0xFF666666), modifier = Modifier.clickable { confirmClear = true }.padding(9.dp))
            }
        } else {
            Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("‹", fontSize = 46.sp, color = Color.Gray, modifier = Modifier.clickable { search = false; query = "" })
                OutlinedTextField(value = query, onValueChange = { query = it }, placeholder = { Text("在当前列表中搜索") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            HorizontalDivider()
        }
        if (cleared) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("没有播放历史", color = Color.Gray, fontSize = 18.sp) }
        } else {
            if (query.isBlank() || "sample_audio.wav".contains(query, true)) {
                Row(Modifier.fillMaxWidth().clickable { onOpen("audioPlayer") }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("♫", fontSize = 46.sp, color = Color(0xFF666666), modifier = Modifier.width(82.dp))
                    Text("sample_audio.wav", fontSize = 20.sp)
                }
            }
            if (query.isBlank() || "sample_video".contains(query, true)) {
                Row(Modifier.fillMaxWidth().clickable { onOpen("videoPlayer") }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    StoryThumbnail(Modifier.size(width = 92.dp, height = 55.dp))
                    Column(Modifier.padding(start = 16.dp)) { Text("sample_video", fontSize = 20.sp); Text("0:03", color = Color.Gray) }
                }
            }
        }
    }
    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text("清除播放历史记录？") },
        text = { Text("此操作会移除历史列表中的所有媒体。") },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("取消") } },
        confirmButton = { TextButton(onClick = { cleared = true; confirmClear = false }) { Text("清除", color = VlcOrange) } }
    )
}
