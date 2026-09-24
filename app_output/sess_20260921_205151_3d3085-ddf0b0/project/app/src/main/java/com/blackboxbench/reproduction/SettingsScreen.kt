package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    var highlight by remember { mutableStateOf(true) }
    var launcherSpecial by remember { mutableStateOf(false) }
    var multiWindow by remember { mutableStateOf(false) }
    var experimental by remember { mutableStateOf(false) }

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
                .statusBarsPadding()
                .clickable(onClick = onBack)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("‹ ", color = Color.White, fontSize = 22.sp)
            Text("设置", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        SettingsSection("通用")
        SettingsRow("保存位置", "笔记本: Documents/markor")
        SettingsRow("QuickNote 路径", "Documents/markor/QuickNote.md")
        SettingsRow("To-Do 路径", "Documents/markor/todo.txt")
        SettingsRow("杂项", null)
        SettingsRow("搜索", null)
        SettingsCheckRow("语法高亮", highlight) { highlight = it }
        SettingsCheckRow("Launcher 特殊文件", launcherSpecial) { launcherSpecial = it }
        SettingsCheckRow("多窗口", multiWindow) { multiWindow = it }
        SettingsCheckRow("实验功能", experimental) { experimental = it }
        SettingsSection("其它")
        SettingsRow("编辑模式", null)
        SettingsRow("视图模式", null)
        SettingsRow("格式", "Markdown / 纯文本 / todo.txt / Wikitext / Zim / AsciiDoc / OrgMode / CSV")
        SettingsRow("主题", "System")
        SettingsRow("语言", "中文 (重启生效)")
    }
}

@Composable
private fun SettingsSection(title: String) {
    Text(
        title,
        color = MarkorRed,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingsRow(title: String, subtitle: String?) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(title, fontSize = 16.sp, color = Color(0xFF222222))
        subtitle?.let { Text(it, fontSize = 13.sp, color = Color(0xFF777777)) }
    }
}

@Composable
private fun SettingsCheckRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 16.sp, color = Color(0xFF222222), modifier = Modifier.weight(1f))
        Checkbox(
            checked = checked, onCheckedChange = onChange,
            colors = CheckboxDefaults.colors(checkedColor = MarkorRed),
        )
    }
}
