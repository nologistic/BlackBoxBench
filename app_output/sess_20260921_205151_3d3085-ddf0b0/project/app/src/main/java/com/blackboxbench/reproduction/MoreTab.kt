package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MoreTab(onOpenSettings: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(ListBg)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NavyBar)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text("Markor", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFFE0E0E0))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("M", fontSize = 40.sp, color = NavyBar, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Markor", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF222222))
                Text("net.gsantner.markor", color = Color(0xFF555555))
                Text("Version v2.16.1 (163)", color = Color(0xFF555555))
            }
        }
        AboutRow("🐞", "存在疑问或问题吗？", "给出反馈，报告问题，或提出改进建议") {}
        AboutRow("⚙️", "设置", null, onOpenSettings)
        AboutRow("❓", "帮助 / FAQ", null) {}
        AboutRow("👍", "评价本应用", null) {}
        AboutSection("项目团队")
        AboutRow("👤", "Gregor Santner (gsantner)", "Austrian software developer and Open Source enthusiast") {}
        AboutSection("社区")
        AboutRow("文A", "翻译", "翻译本应用") {}
        AboutRow("👥", "加入社区", null) {}
        AboutRow("ℹ️", "贡献者", "显示贡献者信息。贡献者可自行选择是否被添加到这里。") {}
        AboutSection("开源协议")
        AboutRow("©", "项目开源协议", "Apache 2.0") {}
        AboutRow("©", "开源协议", null) {}
        AboutRow("<>", "源代码", "为本项目贡献代码。我们欢迎所有人参与进来，包括新手") {}
    }
}

@Composable
private fun AboutSection(title: String) {
    Text(
        title,
        color = MarkorRed,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun AboutRow(icon: String, title: String, subtitle: String?, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(icon, fontSize = 20.sp, modifier = Modifier.width(36.dp))
        Column {
            Text(title, fontWeight = FontWeight.Medium, color = Color(0xFF222222), fontSize = 16.sp)
            subtitle?.let { Text(it, color = Color(0xFF777777), fontSize = 13.sp) }
        }
    }
}
