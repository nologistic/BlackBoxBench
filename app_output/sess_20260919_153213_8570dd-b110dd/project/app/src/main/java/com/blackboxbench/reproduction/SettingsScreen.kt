package com.blackboxbench.reproduction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
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
private fun SettingsTopBar(title: String, onBack: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
        }
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        trailing()
    }
    HorizontalDivider()
}

@Composable
private fun GroupHeader(title: String) {
    Text(
        title,
        color = Color(0xFF8C9FD0),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 18.dp, bottom = 4.dp)
    )
}

@Composable
private fun ClickRow(title: String, subtitle: String? = null, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(title, fontSize = 16.sp)
        if (subtitle != null) Text(subtitle, fontSize = 13.sp, color = Color.Gray)
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, subtitle: String? = null, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            if (subtitle != null) Text(subtitle, fontSize = 13.sp, color = Color.Gray)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun SettingsScreen(app: AppState, toast: (String) -> Unit) {
    val repo = app.repo
    var useRecycleBin by remember { mutableStateOf(repo.useRecycleBin.value) }
    val switchStates = remember { mutableStateOf(mutableMapOf<String, Boolean>()) }
    fun sw(key: String): Boolean = switchStates.value[key] ?: false

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SettingsTopBar("设置", { app.pop() })

        GroupHeader("外观")
        ClickRow("自定义外观", "主题、应用图标、字体") { app.navigate(Screen.Appearance) }
        SwitchRow("使用深色主题", sw("dark")) { switchStates.value["dark"] = it }

        GroupHeader("常规")
        ClickRow("语言", "中文") { toast("当前仅提供中文") }
        ClickRow("更改日期和时间格式") { toast("使用系统默认格式") }
        ClickRow("文件加载优先事项", "速度") { toast("文件加载优先事项：速度") }
        ClickRow("管理包含的文件夹") { app.navigate(Screen.IncludedFolders) }
        ClickRow("管理排除的文件夹") { app.navigate(Screen.ExcludedFolders) }
        SwitchRow("显示隐藏项目", repo.showHiddenSetting.value) {
            repo.showHiddenSetting.value = it
            repo.prefsShowHidden(it)
        }
        SwitchRow("搜索所有文件", sw("searchAll")) { switchStates.value["searchAll"] = it }

        GroupHeader("视频")
        SwitchRow("自动播放视频", sw("autoplay")) { switchStates.value["autoplay"] = it }
        SwitchRow("循环播放视频", sw("loop")) { switchStates.value["loop"] = it }
        SwitchRow("静音播放视频", sw("mute")) { switchStates.value["mute"] = it }

        GroupHeader("缩略图")
        SwitchRow("在缩略图上显示视频时长", true) {}
        SwitchRow("在缩略图上显示收藏标记", true) {}

        GroupHeader("滚动")
        SwitchRow("快速滑动时显示日期", sw("scrollDate")) { switchStates.value["scrollDate"] = it }

        GroupHeader("全屏")
        SwitchRow("以全屏方式打开媒体", sw("fullscreen")) { switchStates.value["fullscreen"] = it }

        GroupHeader("缩放")
        SwitchRow("双击缩放", true) {}
        SwitchRow("允许深度缩放", sw("deepZoom")) { switchStates.value["deepZoom"] = it }

        GroupHeader("安全性")
        ClickRow("锁定应用", "使用图案或 PIN 码") { toast("锁定应用暂不可用") }
        SwitchRow("锁定媒体可见性", sw("lockMedia")) { switchStates.value["lockMedia"] = it }

        GroupHeader("文件操作")
        SwitchRow("删除前显示确认提示", repo.askDeleteConfirm.value) {
            repo.askDeleteConfirm.value = it
            repo.prefsAskConfirm(it)
        }
        SwitchRow("显示文件扩展名", true) {}

        GroupHeader("底部按钮")
        SwitchRow("显示收藏按钮", true) {}
        SwitchRow("显示编辑按钮", true) {}
        SwitchRow("显示分享按钮", true) {}
        SwitchRow("显示删除按钮", true) {}

        GroupHeader("回收站")
        SwitchRow("移至回收站", useRecycleBin, "删除的文件会先移至回收站") {
            useRecycleBin = it
            repo.useRecycleBin.value = it
            repo.prefsUseRecycleBin(it)
        }
        val binSize = repo.deletedItems().sumOf { it.size }
        ClickRow("清空回收站", "回收站大小：${formatSize(binSize)}") {
            repo.emptyRecycleBin()
            toast("回收站已清空")
        }

        GroupHeader("迁移")
        ClickRow("清除缓存") { toast("缓存已清除") }
        ClickRow("导出收藏") { toast("收藏已导出") }
        ClickRow("导入收藏") { toast("未找到可导入的文件") }
        ClickRow("导出设置") { toast("设置已导出") }
        ClickRow("导入设置") { toast("未找到可导入的文件") }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun AppearanceScreen(app: AppState) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SettingsTopBar("自定义外观", { app.pop() })
        ClickRow("应用主题", "系统默认") {}
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("应用图标颜色", fontSize = 16.sp)
                Text("绿色", fontSize = 13.sp, color = Color.Gray)
            }
            androidx.compose.foundation.Canvas(Modifier.size(28.dp)) {
                drawCircle(Color(0xFF4CAF50))
            }
        }
        ClickRow("应用字体", "系统默认") {}
    }
}

@Composable
fun AboutScreen(app: AppState) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SettingsTopBar("关于", { app.pop() })
        GroupHeader("支持")
        ClickRow("常见问题") {}
        ClickRow("支持我们") {}
        GroupHeader("帮助我们")
        ClickRow("评价我们") {}
        ClickRow("邀请朋友") {}
        ClickRow("贡献翻译") {}
        GroupHeader("社交")
        ClickRow("关注我们") {}
        GroupHeader("其他")
        ClickRow("隐私政策") {}
        ClickRow("许可证") {}
        ClickRow("版本", "1.0") {}
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun ExcludedFoldersScreen(app: AppState) {
    val repo = app.repo
    val excluded = repo.excludedFolders.value.toList()
    Column(Modifier.fillMaxSize()) {
        SettingsTopBar("排除的文件夹", { app.pop() }) {
            IconButton(onClick = {}) {
                Icon(Icons.Default.Add, "添加")
            }
        }
        if (excluded.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(32.dp)) {
                Text(
                    "排除某个文件夹只会在本应用中排除它和它的子文件夹，其他应用仍然可以看到这些文件。\n\n" +
                        "如果要防止其他应用读取你的媒体文件，请使用隐藏功能。",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(excluded) { name ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(name, fontSize = 16.sp)
                            Text("/storage/emulated/0/$name", fontSize = 12.sp, color = Color.Gray)
                        }
                        var rowMenu by remember { mutableStateOf(false) }
                        androidx.compose.foundation.layout.Box {
                            IconButton(onClick = { rowMenu = true }) {
                                Icon(Icons.Default.MoreVert, "更多")
                            }
                            androidx.compose.material3.DropdownMenu(
                                expanded = rowMenu,
                                onDismissRequest = { rowMenu = false }
                            ) {
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text("移除") },
                                    onClick = { repo.removeExclusion(name); rowMenu = false }
                                )
                            }
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun IncludedFoldersScreen(app: AppState) {
    val repo = app.repo
    val folders = repo.folderNames(true, true).map { it.name }.sorted()
    Column(Modifier.fillMaxSize()) {
        SettingsTopBar("包含的文件夹", { app.pop() })
        if (folders.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(32.dp)) {
                Text("未找到包含媒体文件的文件夹。", color = Color.Gray, fontSize = 14.sp)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(folders) { name ->
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Text(name, fontSize = 16.sp)
                        Text("/storage/emulated/0/$name", fontSize = 12.sp, color = Color.Gray)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
