package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ListEditor(onBack: () -> Unit, onSave: (String, Long) -> Unit) {
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(0xFF2196F3) }
    Scaffold(topBar = { SimpleBackBar("", onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(28.dp)) {
                Column(Modifier.padding(24.dp)) {
                    Text("这是本地清单", fontSize = 28.sp)
                    Spacer(Modifier.height(18.dp))
                    Text("此清单中的任务只存储在这台设备上。连接账号保护数据，在任何地方访问它。", fontSize = 16.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = {}) { Text("关闭") }
                        Button(onClick = {}) { Text("添加账号") }
                    }
                }
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("显示名称") },
                placeholder = { Text("新建清单") },
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
            )
            SettingsCard("●", "颜色") { color = if (color == 0xFF2196F3) 0xFF43A047 else 0xFF2196F3 }
            SettingsCard("☷", "图标") {}
            Button(
                onClick = { if (name.isNotBlank()) onSave(name.trim(), color) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp).height(56.dp)
            ) { Text("▣   保存", fontSize = 20.sp) }
        }
    }
}

@Composable
fun FilterEditor(count: Int, onBack: () -> Unit, onSave: (FilterSpec) -> Unit) {
    var name by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("我的任务") }
    var menu by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            Row(
                Modifier.statusBarsPadding().fillMaxWidth().height(72.dp).background(Color(0xFF9EC5F5)).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("▣", fontSize = 31.sp, modifier = Modifier.clickable {
                    if (name.isNotBlank()) onSave(FilterSpec(name.trim(), if (condition.contains("优先级")) "high" else "all"))
                }.padding(10.dp))
                Text("新建过滤器", fontSize = 28.sp, modifier = Modifier.weight(1f))
                Text("?", fontSize = 28.sp)
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                textStyle = TextStyle(fontSize = 24.sp, color = MaterialTheme.colorScheme.onBackground),
                decorationBox = { inner ->
                    Column {
                        Text("显示名称", fontSize = 15.sp, color = Color.Gray)
                        Spacer(Modifier.height(10.dp))
                        inner()
                        HorizontalDivider()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            )
            SettingsRow("◉", "颜色") {}
            SettingsRow("≡", "图标") {}
            Text("过滤条件", fontSize = 19.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 14.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(22.dp)) {
                    Text("我的任务", fontSize = 24.sp)
                    Text("2", modifier = Modifier.align(Alignment.End), color = Color.Gray)
                    if (condition != "我的任务") {
                        Text("+   " + condition, fontSize = 20.sp, modifier = Modifier.padding(top = 22.dp))
                        Text(count.toString(), modifier = Modifier.align(Alignment.End), color = Color.Gray)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Box(Modifier.align(Alignment.End)) {
                ExtendedFloatingActionButton(onClick = { menu = true }, text = { Text("添加条件") }, icon = { Text("+", fontSize = 30.sp) })
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    listOf("标签…", "标签名包含…", "开始于…", "截止于…", "优先级…", "标题含…", "在某清单中…", "重复", "已完成", "尚未开始", "有子任务", "是子任务", "有提醒").forEach { item ->
                        DropdownMenuItem(text = { Text(item) }, onClick = {
                            condition = if (item == "优先级…") "最低的优先级!!!" else item
                            menu = false
                        })
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
fun LocationPicker(onBack: () -> Unit, onSelect: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xFF21312F))) {
        Canvas(Modifier.fillMaxSize()) {
            val line = Color.White.copy(alpha = .05f)
            var x = 0f
            while (x < size.width) {
                drawLine(line, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, size.height), 2f)
                x += 48f
            }
            var y = 0f
            while (y < size.height) {
                drawLine(line, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), 2f)
                y += 48f
            }
        }
        OutlinedTextField(
            value = "",
            onValueChange = {},
            placeholder = { Text("搜索") },
            leadingIcon = { Text("⌕", fontSize = 28.sp) },
            modifier = Modifier.statusBarsPadding().padding(10.dp).fillMaxWidth().background(MaterialTheme.colorScheme.surface)
        )
        Text("‹", color = Color.Transparent, modifier = Modifier.clickable(onClick = onBack))
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("●", color = Color.Red, fontSize = 62.sp)
            Text("▼", color = Color.Red, fontSize = 38.sp, modifier = Modifier.offset(y = (-24).dp))
        }
        FloatingActionButton(
            onClick = {},
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 18.dp, bottom = 78.dp)
        ) { Text("◎", fontSize = 30.sp) }
        Row(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().fillMaxWidth().height(72.dp)
                .background(MaterialTheme.colorScheme.surface).clickable(onClick = onSelect).padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("⌖", fontSize = 30.sp, modifier = Modifier.width(50.dp))
            Text("选择此位置", fontSize = 20.sp)
        }
    }
}

@Composable
fun SimpleBackBar(title: String, onBack: () -> Unit) {
    Row(Modifier.statusBarsPadding().fillMaxWidth().height(70.dp).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("‹", fontSize = 43.sp, modifier = Modifier.clickable(onClick = onBack).padding(8.dp))
        Text(title, fontSize = 29.sp, modifier = Modifier.padding(start = 8.dp))
    }
}
