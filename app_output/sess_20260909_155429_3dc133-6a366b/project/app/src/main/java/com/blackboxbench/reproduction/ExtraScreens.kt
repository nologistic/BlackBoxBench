package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup

@Composable
fun AddPodcastScreen(model: AppModel, onRss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        MainHeader("添加播客")
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("搜索播客…") },
            leadingIcon = { Text("⌕", fontSize = 28.sp) },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = { query = "" }) {
                        Text("×", fontSize = 28.sp)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (query.isNotBlank()) {
                item {
                    Text(
                        "搜索结果",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                item {
                    ElevatedCard(
                        Modifier.fillMaxWidth().clickable { model.subscribe() }
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AntennaLogo(Modifier.size(68.dp))
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "黑盒电台",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("黑盒媒体 · 科技")
                            }
                            Button(onClick = { model.subscribe() }) { Text("订阅") }
                        }
                    }
                }
            } else {
                item {
                    TextButton(
                        onClick = { suggestions = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("显示建议") }
                }
                if (suggestions) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text("无法连接到播客目录", fontWeight = FontWeight.Bold)
                                Text("设备当前处于离线状态。您仍可通过 RSS 地址添加播客。")
                                TextButton(onClick = { suggestions = false }) { Text("重试") }
                            }
                        }
                    }
                }
                item {
                    Text(
                        "发现更多",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                item {
                    AddOption("◉", "通过 RSS 地址添加播客", "输入播客源地址", onRss)
                }
                item {
                    AddOption("▣", "添加本地文件夹", "把设备上的音频作为播客") {
                        model.rename("Podcasts")
                        model.subscribe()
                    }
                }
                item {
                    AddOption("♫", "Apple Podcasts", "浏览公开播客目录") {
                        suggestions = true
                    }
                }
                item {
                    AddOption("◎", "Podcast Index", "开放播客搜索") {
                        suggestions = true
                    }
                }
                item {
                    AddOption("⇧", "导入 OPML", "从其他播客应用迁移") {
                        suggestions = true
                    }
                }
                item {
                    ElevatedCard {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AntennaLogo(Modifier.size(72.dp))
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "黑盒电台",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("内置离线演示播客 · 6 个单集")
                            }
                            Button(onClick = { model.subscribe() }) { Text("添加") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddOption(
    icon: String,
    title: String,
    body: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(body) },
        leadingContent = { Text(icon, fontSize = 28.sp) },
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    )
}

@Composable
fun RssDialog(model: AppModel, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf("") }
    var invalid by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("通过 RSS 地址添加播客") },
        text = {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it; invalid = false },
                    label = { Text("RSS 地址") },
                    placeholder = { Text("https://example.com/feed.xml") },
                    isError = invalid,
                    supportingText = {
                        if (invalid) Text("您输入的 RSS 地址无效。")
                    },
                    singleLine = true
                )
                Text(
                    "离线演示会加载内置的“黑盒电台”测试源。",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (value.isBlank()) {
                    invalid = true
                } else {
                    model.subscribe()
                    onDismiss()
                }
            }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(model: AppModel, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 22.dp).padding(bottom = 28.dp)) {
            Text("筛选单集", fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            Text("播放状态", color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = model.filterUnplayed,
                    onClick = { model.filterUnplayed = true },
                    label = { Text("未播放") }
                )
                FilterChip(
                    selected = !model.filterUnplayed,
                    onClick = { model.filterUnplayed = false },
                    label = { Text("已播放") }
                )
            }
            Text("媒体", color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = false, onClick = {}, label = { Text("已下载") })
                FilterChip(selected = false, onClick = {}, label = { Text("已加入队列") })
                FilterChip(selected = false, onClick = {}, label = { Text("收藏") })
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { model.filterUnplayed = false }) { Text("重置") }
                Button(onClick = onDismiss) { Text("确定") }
            }
        }
    }
}

@Composable
fun RenameDialog(model: AppModel, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf(model.podcastName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重命名播客") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("显示名称") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = {
                model.rename(value)
                onDismiss()
            }) { Text("确定") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { value = "黑盒电台" }) { Text("重置") }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        }
    )
}

@Composable
fun LabelDialog(model: AppModel, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf(model.label) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑订阅标签") },
        text = {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("添加标签") },
                    singleLine = true
                )
                if (value.isNotBlank()) {
                    AssistChip(onClick = {}, label = { Text(value) })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                model.updateLabel(value)
                onDismiss()
            }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun NavigationEditor(model: AppModel, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("抽屉偏好设置", fontSize = 27.sp, fontWeight = FontWeight.Medium)
                Text(
                    "显示",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                model.navigation.forEachIndexed { index, page ->
                    key(page) {
                        Row(
                            Modifier.fillMaxWidth().height(56.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "⠿",
                                color = MaterialTheme.colorScheme.outline,
                                fontSize = 22.sp
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                pageTitle(page),
                                fontSize = 19.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                if (index > 0) {
                                    model.navigation.removeAt(index)
                                    model.navigation.add(index - 1, page)
                                }
                            }) { Text("↑") }
                            IconButton(onClick = {
                                if (index < model.navigation.lastIndex) {
                                    model.navigation.removeAt(index)
                                    model.navigation.add(index + 1, page)
                                }
                            }) { Text("↓") }
                        }
                    }
                }
                Text("隐藏", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = {
                        model.navigation.clear()
                        model.navigation.addAll(
                            listOf(
                                Page.HOME,
                                Page.QUEUE,
                                Page.INBOX,
                                Page.SUBSCRIPTIONS
                            )
                        )
                    }) { Text("重置") }
                    Row {
                        TextButton(onClick = onDismiss) { Text("取消") }
                        TextButton(onClick = {
                            model.saveNavigation()
                            onDismiss()
                        }) { Text("确定") }
                    }
                }
            }
        }
    }
}

@Composable
fun StatisticsScreen(model: AppModel) {
    Column(Modifier.fillMaxSize()) {
        MainHeader("统计", actions = {
            IconButton(onClick = {}) { Text("≡", fontSize = 24.sp) }
        })
        TabRow(selectedTabIndex = model.statsTab) {
            listOf("订阅", "年", "下载").forEachIndexed { index, title ->
                Tab(
                    selected = model.statsTab == index,
                    onClick = { model.statsTab = index },
                    text = { Text(title) }
                )
            }
        }
        when (model.statsTab) {
            0 -> StatGauge(
                if (model.history.isEmpty()) "0 小时"
                else "${model.history.size * 0.5} 小时",
                "本月收听时长",
                "${model.history.size} 个单集"
            )
            1 -> {
                Column(
                    Modifier.fillMaxSize().padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("每月播放时长", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(60.dp))
                    Canvas(Modifier.fillMaxWidth().height(200.dp)) {
                        val color = Color(0xFF078EDA)
                        val step = size.width / 11f
                        for (i in 0..10) {
                            val now = androidx.compose.ui.geometry.Offset(
                                step * i,
                                size.height - (i % 3) * 30f - 20f
                            )
                            drawCircle(color, 5f, now)
                            if (i > 0) {
                                val prior = androidx.compose.ui.geometry.Offset(
                                    step * (i - 1),
                                    size.height - ((i - 1) % 3) * 30f - 20f
                                )
                                drawLine(color, prior, now, 5f)
                            }
                        }
                    }
                    Text(
                        "${model.history.size * 0.5} 小时",
                        fontSize = 34.sp
                    )
                }
            }
            else -> StatGauge(
                if (model.downloads.isEmpty()) "0 B"
                else "${model.downloads.size * 32} MB",
                "设备上 ${model.downloads.size} 个单集的总大小",
                "本地存储"
            )
        }
    }
}

@Composable
fun StatGauge(value: String, subtitle: String, foot: String) {
    Column(
        Modifier.fillMaxSize().padding(26.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(55.dp))
        Box(
            Modifier.fillMaxWidth().height(250.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawArc(
                    color = Color(0xFF8A8A8A),
                    startAngle = 190f,
                    sweepAngle = 160f,
                    useCenter = false,
                    style = Stroke(12.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 65.dp)
            ) {
                Text(value, fontSize = 38.sp)
                Text(
                    subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        HorizontalDivider()
        Spacer(Modifier.height(20.dp))
        Text(foot, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SettingsHome(model: AppModel) {
    var query by remember { mutableStateOf("") }
    val rows = listOf(
        Triple("▣", "用户界面", "外观、订阅、锁屏"),
        Triple("▶", "播放", "耳机控制、跳过间隔、队列"),
        Triple("⇩", "下载", "更新间隔、移动数据、自动下载、自动删除"),
        Triple("☁", "同步", "和其他设备同步"),
        Triple("▤", "备份和恢复", "将订阅和队列转移到其他设备"),
        Triple("♟", "通知", "")
    )
    Column(Modifier.fillMaxSize()) {
        MainHeader("设置", back = { model.page = Page.HOME })
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("搜索…") },
            leadingIcon = { Text("⌕", fontSize = 28.sp) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) { Text("×") }
                }
            },
            shape = RoundedCornerShape(28.dp),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)
        )
        val filtered = rows.filter {
            query.isBlank() ||
                it.second.contains(query, true) ||
                it.third.contains(query, true)
        }
        if (query.isNotBlank() && filtered.isEmpty()) {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    "无结果",
                    modifier = Modifier.padding(top = 60.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 20.dp)
            ) {
                items(filtered) { row ->
                    ListItem(
                        headlineContent = {
                            Text(
                                row.second,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supportingContent = {
                            if (row.third.isNotBlank()) Text(row.third)
                        },
                        leadingContent = { Text(row.first, fontSize = 27.sp) },
                        modifier = Modifier.clickable {
                            model.page = when (row.second) {
                                "用户界面" -> Page.UI_SETTINGS
                                "播放" -> Page.PLAYBACK_SETTINGS
                                "下载" -> Page.DOWNLOAD_SETTINGS
                                else -> Page.SETTINGS
                            }
                        }
                    )
                }
                item { HorizontalDivider() }
                item {
                    Text(
                        "项目",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(18.dp)
                    )
                }
                items(listOf("文档和支持", "用户论坛", "贡献", "报告错误")) {
                    ListItem(
                        headlineContent = { Text(it, fontSize = 19.sp) },
                        leadingContent = { Text("◉", fontSize = 22.sp) }
                    )
                }
            }
        }
    }
}

@Composable
fun UiSettings(model: AppModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        MainHeader("用户界面", back = { model.page = Page.SETTINGS })
        Text(
            "主题设置",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 18.sp,
            modifier = Modifier.padding(18.dp)
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ThemeCard("自动", model.theme == "auto", Modifier.weight(1f)) {
                model.updateTheme("auto")
            }
            ThemeCard("浅色", model.theme == "light", Modifier.weight(1f)) {
                model.updateTheme("light")
            }
            ThemeCard("深色", model.theme == "dark", Modifier.weight(1f)) {
                model.updateTheme("dark")
            }
        }
        SettingSwitch("纯黑", "深色主题使用纯黑", false) {}
        SettingSwitch("动态配色", "基于背景图对应用进行着色", false) {}
        HorizontalDivider()
        Text(
            "单集信息",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 18.sp,
            modifier = Modifier.padding(18.dp)
        )
        SettingSwitch("使用单集封面", "如果可用，在列表中使用单集专属封面", true) {}
        SettingSwitch("显示剩余时长", "启用后显示单集剩余时长", false) {}
        SettingSwitch("根据播放速度调整媒体信息", "位置和时长根据播放速度调整", false) {}
    }
}

@Composable
fun RowScope.ThemeCard(
    title: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier.height(178.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            Modifier.fillMaxSize().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                Modifier.fillMaxWidth().height(120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (title == "深色") Color(0xFF22292D) else Color.White
                    )
            ) {
                Text(
                    "☰  Home      ⌕",
                    color = if (title == "深色") Color.White else Color.DarkGray,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Text(title, fontSize = 18.sp)
        }
    }
}

@Composable
fun SettingSwitch(
    title: String,
    body: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    var local by remember(checked) { mutableStateOf(checked) }
    ListItem(
        headlineContent = {
            Text(title, fontSize = 19.sp, fontWeight = FontWeight.Medium)
        },
        supportingContent = { Text(body) },
        trailingContent = {
            Switch(
                checked = local,
                onCheckedChange = {
                    local = it
                    onChange(it)
                }
            )
        }
    )
}

@Composable
fun SettingRow(title: String, body: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(title, fontSize = 19.sp, fontWeight = FontWeight.Medium)
        },
        supportingContent = { Text(body) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
fun PlaybackSettings(model: AppModel, onFastForward: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        MainHeader("播放", back = { model.page = Page.SETTINGS })
        Text(
            "中断",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 18.sp,
            modifier = Modifier.padding(18.dp)
        )
        SettingSwitch(
            "耳机或蓝牙断开",
            "耳机或蓝牙设备断开连接时暂停播放",
            model.pauseOnDisconnect
        ) { model.pauseOnDisconnect = it }
        HorizontalDivider()
        Text(
            "播放控制",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 18.sp,
            modifier = Modifier.padding(18.dp)
        )
        SettingRow(
            "快进跳转时间",
            "当前：${model.fastForward} 秒",
            onFastForward
        )
        SettingRow("快退跳转时间", "自定义点击快退按钮时向后跳转的秒数") {}
        SettingRow("播放速度", "自定义可用于变速播放的速度") {}
        HorizontalDivider()
        Text(
            "重新分配硬件按钮",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 18.sp,
            modifier = Modifier.padding(18.dp)
        )
        SettingRow("前进按钮", "自定义快进按钮行为") {}
        SettingRow("后退按钮", "自定义“上一个”按钮行为") {}
        HorizontalDivider()
        Text(
            "队列",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 18.sp,
            modifier = Modifier.padding(18.dp)
        )
        SettingRow("加入队列位置", "将单集添加到：末尾") {}
        SettingSwitch(
            "添加已下载单集到队列",
            "仅将已经下载的单集自动加入队列",
            model.queueDownloadsOnly
        ) { model.queueDownloadsOnly = it }
    }
}

@Composable
fun FastForwardDialog(model: AppModel, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("快进跳转时间") },
        text = {
            Column {
                listOf(5, 10, 15, 20, 30, 45, 60).forEach { seconds ->
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable {
                                model.updateFastForward(seconds)
                                onDismiss()
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = model.fastForward == seconds,
                            onClick = {
                                model.updateFastForward(seconds)
                                onDismiss()
                            }
                        )
                        Text("$seconds 秒", fontSize = 18.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun DownloadSettings(model: AppModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        MainHeader("下载", back = { model.page = Page.SETTINGS })
        SettingRow(
            "选择数据文件夹",
            "/storage/emulated/0/Android/data/com.blackboxbench.reproduction/files"
        ) {}
        HorizontalDivider()
        Text(
            "自动化",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 18.sp,
            modifier = Modifier.padding(18.dp)
        )
        SettingRow("刷新播客", "指定 AntennaPod 自动查找新单集的间隔") {}
        SettingRow("新单集操作", "为新单集采取的操作") {}
        SettingRow("自动下载", "配置单集的自动下载") {
            model.page = Page.AUTO_DOWNLOAD
        }
        SettingRow("自动删除", "播放后或需要空间时删除单集") {}
        SettingSwitch("删除后移出队列", "删除单集时自动将其从队列中移除", false) {}
        HorizontalDivider()
        Text(
            "详细信息",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 18.sp,
            modifier = Modifier.padding(18.dp)
        )
        SettingRow("移动数据更新", "选择使用移动数据连接时应当允许的内容") {}
        SettingRow("代理", "选择一个网络代理") {}
    }
}

@Composable
fun AutoDownloadSettings(model: AppModel) {
    Column(Modifier.fillMaxSize()) {
        MainHeader("自动下载", back = { model.page = Page.DOWNLOAD_SETTINGS })
        SettingSwitch(
            "自动下载",
            "自动从收件箱下载单集。可针对每个播客单独设置。",
            model.autoDownload
        ) { model.autoDownload = it }
        SettingSwitch(
            "下载队列中的单集",
            "自动下载队列中的单集",
            model.queueDownloadsOnly
        ) { model.queueDownloadsOnly = it }
        SettingRow("单集限制", "如果达到此数值，自动下载将停止") {}
        SettingSwitch(
            "未充电时下载",
            "允许设备未充电时自动下载",
            model.downloadWhileBattery
        ) { model.downloadWhileBattery = it }
    }
}


@Composable
fun MorePopup(
    model: AppModel,
    onDismiss: () -> Unit,
    onNavigationEditor: () -> Unit
) {
    Popup(
        alignment = Alignment.BottomEnd,
        offset = IntOffset(-18, -230),
        onDismissRequest = onDismiss
    ) {
        Surface(
            modifier = Modifier.width(265.dp),
            shape = RoundedCornerShape(14.dp),
            tonalElevation = 8.dp,
            shadowElevation = 10.dp
        ) {
            Column(Modifier.padding(vertical = 6.dp)) {
                val menu = listOf(
                    Page.EPISODES,
                    Page.DOWNLOADS,
                    Page.HISTORY,
                    Page.FAVORITES,
                    Page.STATISTICS,
                    Page.ADD
                ).filterNot { it in model.navigation.take(4) }
                menu.forEach { page ->
                    Row(
                        Modifier.fillMaxWidth().height(54.dp)
                            .clickable {
                                model.page = page
                                onDismiss()
                            }
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(pageIcon(page), fontSize = 22.sp)
                        Spacer(Modifier.width(20.dp))
                        Text(pageTitle(page), fontSize = 18.sp)
                    }
                }
                HorizontalDivider()
                Row(
                    Modifier.fillMaxWidth().height(54.dp)
                        .clickable(onClick = onNavigationEditor)
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("✎", fontSize = 22.sp)
                    Spacer(Modifier.width(20.dp))
                    Text("自定义导航", fontSize = 18.sp)
                }
                Row(
                    Modifier.fillMaxWidth().height(54.dp)
                        .clickable {
                            model.page = Page.SETTINGS
                            onDismiss()
                        }
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚙", fontSize = 22.sp)
                    Spacer(Modifier.width(20.dp))
                    Text("设置", fontSize = 18.sp)
                }
            }
        }
    }
}
