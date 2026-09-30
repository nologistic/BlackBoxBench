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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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

val LIST_COLORS = listOf(
    0xFFE53935, 0xFFFB8C00, 0xFFFBC02D, 0xFF43A047,
    0xFF009688, 0xFF1E88E5, 0xFF5E35B1, 0xFFD81B60,
)

/** 新建列表：名称 + 颜色 + 图标 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewListScreen(app: AppState) {
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(0xFF1E88E5) }
    var icon by remember { mutableStateOf("list") }
    val icons = listOf("list", "work", "home", "shop", "star", "heart", "book", "car")

    fun save() {
        if (name.isBlank()) return
        val id = "lst_${System.currentTimeMillis()}"
        app.update { d ->
            d.copy(lists = d.lists + TaskList(id, name.trim(), color, icon, System.currentTimeMillis()))
        }
        app.view = MainView.ListView(app.data.lists.last().id)
        app.pop()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("新建列表") },
                navigationIcon = { IconButton(onClick = { app.pop() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                actions = {
                    IconButton(onClick = { save() }) { Icon(Icons.Default.Check, "保存") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(color), titleContentColor = Color.White,
                    navigationIconContentColor = Color.White, actionIconContentColor = Color.White,
                ),
            )
        },
        containerColor = Color.White,
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
            Text("颜色", fontSize = 13.sp, color = Color(0xFF9E9E9E))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LIST_COLORS.forEach { c ->
                    Box(
                        Modifier
                            .size(36.dp)
                            .background(Color(c), CircleShape)
                            .border(
                                width = if (color == c) 3.dp else 0.dp,
                                color = if (color == c) Color(0xFF212121) else Color.Transparent,
                                shape = CircleShape,
                            )
                            .clickable { color = c },
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("图标", fontSize = 13.sp, color = Color(0xFF9E9E9E))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                icons.forEach { ic ->
                    val label = mapOf(
                        "list" to "☰", "work" to "💼", "home" to "🏠", "shop" to "🛒",
                        "star" to "★", "heart" to "♥", "book" to "📖", "car" to "🚗",
                    )[ic] ?: "☰"
                    Box(
                        Modifier
                            .size(40.dp)
                            .background(if (icon == ic) Color(color).copy(alpha = 0.2f) else Color(0xFFF5F5F5), CircleShape)
                            .clickable { icon = ic },
                        contentAlignment = Alignment.Center,
                    ) { Text(label, fontSize = 18.sp) }
                }
            }
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = { save() },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ACCENT),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text("保存") }
        }
    }
}

/** 新建过滤器：今天到期 / 高优先级 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewFilterScreen(app: AppState) {
    var name by remember { mutableStateOf("") }
    var today by remember { mutableStateOf(false) }
    var highPriority by remember { mutableStateOf(false) }

    fun save() {
        if (name.isBlank()) return
        val id = "flt_${System.currentTimeMillis()}"
        app.update { d ->
            d.copy(filters = d.filters + FilterDef(id, name.trim(), today, highPriority))
        }
        app.view = MainView.FilterView(id)
        app.pop()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("新建过滤器") },
                navigationIcon = { IconButton(onClick = { app.pop() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                actions = { IconButton(onClick = { save() }) { Icon(Icons.Default.Check, "保存") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF5C6BC0), titleContentColor = Color.White, navigationIconContentColor = Color.White, actionIconContentColor = Color.White),
            )
        },
        containerColor = Color.White,
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("今天到期", modifier = Modifier.weight(1f), fontSize = 15.sp)
                Switch(checked = today, onCheckedChange = { today = it })
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("高优先级", modifier = Modifier.weight(1f), fontSize = 15.sp)
                Switch(checked = highPriority, onCheckedChange = { highPriority = it })
            }
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = { save() },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ACCENT),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text("保存") }
        }
    }
}

/** 设置主页 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(app: AppState) {
    val groups = listOf(
        "外观" to "主题、主题色、语言",
        "小组件" to "桌面小组件设置",
        "通知" to "提醒、振动、声音",
        "任务默认设置" to "新任务的默认截止日期与优先级",
        "同步" to "当前无同步账户",
        "关于 Tasks" to "版本与开源许可",
    )
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = { IconButton(onClick = { app.pop() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ACCENT, titleContentColor = Color.White, navigationIconContentColor = Color.White),
            )
        },
        containerColor = Color(0xFFFAFAFA),
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            groups.forEach { (title, subtitle) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .clickable { if (title == "外观") app.navigate(Screen.Appearance) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(title, fontSize = 16.sp, color = Color(0xFF212121))
                        Text(subtitle, fontSize = 12.sp, color = Color(0xFF9E9E9E))
                    }
                    if (title == "同步") Text("未配置", fontSize = 13.sp, color = Color(0xFF9E9E9E))
                }
            }
        }
    }
}

/** 外观子页 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(app: AppState) {
    var themeDialog by remember { mutableStateOf(false) }
    var theme by remember { mutableStateOf("系统默认") }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("外观") },
                navigationIcon = { IconButton(onClick = { app.pop() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ACCENT, titleContentColor = Color.White, navigationIconContentColor = Color.White),
            )
        },
        containerColor = Color.White,
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            SettingsRow("主题", theme) { themeDialog = true }
            SettingsRow("主题色", "蓝绿色") { }
            SettingsRow("语言", "跟随系统") { }
            SettingsRow("启动屏幕", "上次的视图") { }
        }
    }
    if (themeDialog) {
        AlertDialog(
            onDismissRequest = { themeDialog = false },
            title = { Text("主题") },
            text = {
                Column {
                    listOf("系统默认", "浅色", "深色", "纯黑").forEach { t ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { theme = t; themeDialog = false },
                        ) {
                            androidx.compose.material3.RadioButton(selected = theme == t, onClick = { theme = t; themeDialog = false })
                            Text(t)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { themeDialog = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun SettingsRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Text(label, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Text(value, fontSize = 14.sp, color = Color(0xFF757575))
    }
}
