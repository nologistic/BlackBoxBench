package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsHome(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val sections = listOf(
        Triple("本地清单", "管理清单的名称、颜色和图标", "lists"),
        Triple("添加账户", "使用 CalDAV 或 Tasks.org 同步", "account"),
        Triple("外观", "主题、颜色、语言与启动画面", "appearance"),
        Triple("通知", "提醒、声音、稍后提醒与安静时段", "notifications"),
        Triple("任务默认值", "清单、标签、优先级和日期", "defaults"),
        Triple("任务清单选项", "字体、间距、描述与排序", "tasklist"),
        Triple("编辑屏幕选项", "重新排列和隐藏编辑字段", "editor"),
        Triple("日期和时间", "日期格式和快捷时间", "datetime"),
        Triple("导航抽屉", "显示或隐藏抽屉项目", "drawer"),
        Triple("备份", "立即备份、导入与自动备份", "backup"),
        Triple("插件设置", "日历与其他集成", "plugins"),
        Triple("高级", "实验与故障排除选项", "advanced"),
        Triple("关于", "Tasks.org", "about")
    )
    Column(Modifier.fillMaxSize().background(Color.White)) {
        SimpleTopBar("设置", onBack)
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Surface(
                    color = Color(0xFFFFF1C4),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text("支持 Tasks.org", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "自由开源的任务管理器",
                            color = Color(0xFF5F5B4F),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        TextButton(onClick = {}) { Text("捐赠") }
                    }
                }
            }
            items(sections) { item ->
                Row(
                    Modifier.fillMaxWidth().clickable { onOpen(item.third) }
                        .padding(horizontal = 20.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "◈",
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(48.dp)
                    )
                    Column(Modifier.weight(1f)) {
                        Text(item.first, fontSize = 18.sp)
                        Text(item.second, fontSize = 13.sp, color = Color(0xFF6A6971))
                    }
                    Text("›", fontSize = 28.sp)
                }
                HorizontalDivider(color = Color(0xFFF0EDF1))
            }
        }
    }
}

@Composable
fun SettingsDetail(page: String, onBack: () -> Unit) {
    val title = when (page) {
        "appearance" -> "外观"
        "notifications" -> "通知"
        "defaults" -> "任务默认值"
        "tasklist" -> "任务清单选项"
        "editor" -> "编辑屏幕选项"
        "backup" -> "备份"
        "lists" -> "本地清单"
        "account" -> "添加账户"
        "datetime" -> "日期和时间"
        "drawer" -> "导航抽屉"
        "plugins" -> "插件设置"
        "advanced" -> "高级"
        "about" -> "关于"
        else -> page
    }
    var enabledOne by remember(page) { mutableStateOf(true) }
    var enabledTwo by remember(page) { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        SimpleTopBar(title, onBack)
        Column(Modifier.verticalScroll(rememberScrollState())) {
            when (page) {
                "appearance" -> {
                    SettingChoice("主题", "系统默认")
                    SettingSwitch("动态颜色", enabledTwo) { enabledTwo = it }
                    SettingChoice("颜色", "蓝色")
                    SettingChoice("启动器图标", "默认")
                    SettingSwitch("Markdown", enabledTwo) { enabledTwo = it }
                    SettingSwitch("启动时打开上次查看的清单", enabledOne) { enabledOne = it }
                    SettingChoice("语言", "中文（中国）")
                    SettingChoice("帮助翻译", "在 Weblate 上贡献")
                }
                "notifications" -> {
                    SettingChoice("常见问题", "通知疑难解答")
                    SettingChoice("完成提示音", "默认")
                    SettingSwitch("合并通知", enabledOne) { enabledOne = it }
                    SettingSwitch("语音提醒", enabledTwo) { enabledTwo = it }
                    SettingSwitch("全天任务添加默认提醒", enabledOne) { enabledOne = it }
                    SettingChoice("提醒时间", "18:00")
                    SettingSwitch("启用稍后提醒", enabledTwo) { enabledTwo = it }
                    SettingChoice("延迟时长", "15 分钟")
                    SettingSwitch("启用安静时段", enabledTwo) { enabledTwo = it }
                    SettingChoice("开始", "22:00")
                    SettingChoice("结束", "10:00")
                }
                "defaults" -> {
                    SettingSwitch("新任务显示在顶部", enabledOne) { enabledOne = it }
                    SettingChoice("默认清单", "工作")
                    SettingChoice("默认标签", "无")
                    SettingChoice("优先级", "低")
                    SettingChoice("开始日期", "无")
                    SettingChoice("截止日期", "无")
                    SettingChoice("日历", "不添加")
                    SettingChoice("提醒", "截止时")
                }
                "tasklist" -> {
                    SettingChoice("字体大小", "16")
                    SettingChoice("行间距", "16")
                    SettingSwitch("显示完整任务标题", enabledTwo) { enabledTwo = it }
                    SettingSwitch("显示描述", enabledOne) { enabledOne = it }
                    SettingSwitch("显示完整描述", enabledTwo) { enabledTwo = it }
                    SettingSwitch("显示链接", enabledTwo) { enabledTwo = it }
                    SettingSwitch("每个清单单独排序", enabledTwo) { enabledTwo = it }
                    SettingChoice("卡片外观", "文本和图标")
                }
                "editor" -> {
                    Text(
                        "自定义编辑屏幕",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(20.dp)
                    )
                    SettingChoice("字段顺序", "拖动以重新排列")
                    SettingSwitch("显示链接", enabledTwo) { enabledTwo = it }
                    SettingSwitch("返回时保存任务", enabledTwo) { enabledTwo = it }
                    SettingSwitch("允许多行标题", enabledTwo) { enabledTwo = it }
                    SettingSwitch("在编辑器中显示评论", enabledTwo) { enabledTwo = it }
                    SettingSwitch("快速设置磁贴无需解锁", enabledTwo) { enabledTwo = it }
                }
                "backup" -> {
                    Surface(
                        color = Color(0xFFFFF2C7),
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Text(
                            "请定期备份。本地数据可能在卸载应用时被移除。",
                            modifier = Modifier.padding(18.dp)
                        )
                    }
                    SettingChoice("备份文件夹", "Documents / Tasks")
                    SettingChoice("立即备份", "上次备份：尚未备份")
                    SettingChoice("导入备份", "选择 Tasks.org 备份文件")
                    SettingSwitch("自动备份", enabledOne) { enabledOne = it }
                    SettingSwitch("Google Drive", enabledTwo) { enabledTwo = it }
                    SettingChoice("Android 备份服务", "已启用")
                }
                "lists" -> {
                    SettingChoice("工作", "6 个任务")
                    SettingChoice("生活", "8 个任务")
                    SettingChoice("购物", "4 个任务")
                    Button(onClick = {}, modifier = Modifier.padding(20.dp)) {
                        Text("新建本地清单")
                    }
                }
                "account" -> {
                    SettingChoice("Google Tasks", "添加账户")
                    SettingChoice("DAVx⁵", "添加 CalDAV 账户")
                    SettingChoice("EteSync", "添加加密账户")
                    Text(
                        "离线使用时，所有任务只保存在此设备。",
                        modifier = Modifier.padding(20.dp),
                        color = Color(0xFF6A6971)
                    )
                }
                "about" -> {
                    Text("Tasks.org", fontSize = 30.sp, modifier = Modifier.padding(20.dp))
                    Text(
                        "自由开源的任务管理器\n复现演示版本 1.0",
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
                else -> {
                    SettingChoice("默认选项", "系统设置")
                    SettingSwitch("启用", enabledOne) { enabledOne = it }
                }
            }
        }
    }
}

@Composable
fun FilterEditorScreen(onBack: () -> Unit) {
    var name by remember { mutableStateOf("新过滤器") }
    var highOnly by remember { mutableStateOf(false) }
    var dueOnly by remember { mutableStateOf(false) }
    var hideCompleted by remember { mutableStateOf(true) }
    var addDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "▣",
                fontSize = 29.sp,
                modifier = Modifier.clickable { onBack() }.padding(7.dp)
            )
            Text(
                "自定义过滤器",
                fontSize = 24.sp,
                modifier = Modifier.padding(start = 12.dp).weight(1f)
            )
            Text("?", fontSize = 24.sp)
        }
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("显示名称") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )
        SettingChoice("颜色", "蓝色")
        SettingChoice("图标", "过滤器")
        Text(
            "条件",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )
        SettingSwitch("仅高优先级", highOnly) { highOnly = it }
        SettingSwitch("有截止日期", dueOnly) { dueOnly = it }
        SettingSwitch("隐藏已完成", hideCompleted) { hideCompleted = it }
        Spacer(Modifier.weight(1f))
        Row(
            Modifier.fillMaxWidth().padding(18.dp),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton(onClick = { addDialog = true }) { Text("+ 添加条件") }
            Spacer(Modifier.width(10.dp))
            Button(onClick = onBack) { Text("保存") }
        }
    }

    if (addDialog) {
        AlertDialog(
            onDismissRequest = { addDialog = false },
            title = { Text("添加条件") },
            text = {
                Column {
                    listOf(
                        "标签", "标签名称包含", "开始日期", "截止日期", "优先级", "标题包含",
                        "特定清单", "重复", "已完成", "未开始", "有子任务", "是子任务", "有提醒"
                    ).forEach {
                        DialogLine(it) { addDialog = false }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { addDialog = false }) { Text("取消") }
            }
        )
    }
}
