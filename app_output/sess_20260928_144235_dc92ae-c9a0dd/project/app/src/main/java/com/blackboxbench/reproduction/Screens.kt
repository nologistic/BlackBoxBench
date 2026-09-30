package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ScreenScaffold(
    title: String,
    onBack: () -> Unit,
    actions: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Surface).statusBarsPadding().navigationBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).padding(start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIconButton(Icons.Filled.ArrowBack, "返回") { onBack() }
            Spacer(Modifier.width(14.dp))
            Text(title, fontSize = 22.sp, color = OnSurface, modifier = Modifier.weight(1f))
            actions?.invoke()
        }
        content()
    }
}

@Composable
fun SettingsScreen(model: AppModel, onBack: () -> Unit) {
    var nightMenu by remember { mutableStateOf(false) }
    var longNameMenu by remember { mutableStateOf(false) }
    var languageMenu by remember { mutableStateOf(false) }
    ScreenScaffold("设置", onBack) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            SectionHeader("界面")
            SettingsRow("语言", model.language) { languageMenu = true }
            Box {
                DropdownMenu(expanded = languageMenu, onDismissRequest = { languageMenu = false }) {
                    listOf("系统默认", "简体中文", "English").forEach {
                        SimpleDropdownItem(it) { model.language = it; languageMenu = false }
                    }
                }
            }
            SettingsRow("主题色", "应用中最常见的颜色", enabled = false)
            SettingsRow(
                "质感设计 3",
                null,
                trailing = { SwitchTrailing(model.dynamicColor, true) { model.dynamicColor = it } },
            )
            Box {
                SettingsRow("夜间模式", model.nightMode) { nightMenu = true }
                DropdownMenu(expanded = nightMenu, onDismissRequest = { nightMenu = false }) {
                    listOf("跟随系统", "关闭", "开启", "基于时间", "基于省电模式").forEach {
                        SimpleDropdownItem(it) { model.nightMode = it; nightMenu = false }
                    }
                }
            }
            SettingsRow(
                "黑色夜间模式",
                null,
                trailing = { SwitchTrailing(model.blackNight, true) { model.blackNight = it } },
            )
            SettingsRow(
                "文件列表动画",
                null,
                trailing = { SwitchTrailing(model.listAnimation, true) { model.listAnimation = it } },
            )
            Box {
                SettingsRow("显示长文件名", model.longNameMode) { longNameMenu = true }
                DropdownMenu(expanded = longNameMenu, onDismissRequest = { longNameMenu = false }) {
                    listOf("省略", "省略中间", "换行").forEach {
                        SimpleDropdownItem(it) { model.longNameMode = it; longNameMenu = false }
                    }
                }
            }

            SectionHeader("行为")
            SettingsRow("默认文件夹", model.defaultFolder)
            SettingsRow("存储空间", "根目录和内部存储")
            SettingsRow("标准文件夹", "相片、下载、电影、音乐和图片")
            SettingsRow("书签文件夹", "屏幕截图")
            SettingsRow("Root 访问模式", model.rootAccessMode)
            SettingsRow("归档文件名编码", model.archiveEncoding)
            SettingsRow("打开 Android 安装包", model.apkOpenMode)
            SettingsRow(
                "读取远程文件以显示缩略图",
                null,
                trailing = { SwitchTrailing(model.remoteThumbnails, true) { model.remoteThumbnails = it } },
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    ScreenScaffold("关于", onBack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(32.dp))
            Box(
                Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PrimaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                FolderGlyph(Modifier.size(44.dp), OnPrimaryContainer)
            }
            Spacer(Modifier.height(16.dp))
            Text("质感文件", fontSize = 22.sp, color = OnSurface)
            Text("版本 1.7.4 (39)", fontSize = 13.sp, color = OnSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            LinkRow("在 GitHub 上查看")
            LinkRow("许可证")
            LinkRow("隐私权政策")
            Spacer(Modifier.height(24.dp))
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Text("作者", fontSize = 15.sp, color = Primary, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(10.dp))
                AuthorRow("林一鸣", "界面设计与实现")
                AuthorRow("苏晚", "本地化与文档")
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun LinkRow(label: String) {
    Row(
        Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 16.sp, color = Primary)
    }
}

@Composable
private fun AuthorRow(name: String, role: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).background(SurfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(name.take(1), fontSize = 16.sp, color = Primary)
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(name, fontSize = 16.sp, color = OnSurface)
            Text(role, fontSize = 13.sp, color = OnSurfaceVariant)
        }
    }
}

@Composable
fun FtpScreen(model: AppModel, onBack: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    ScreenScaffold(
        title = "FTP 服务器",
        onBack = onBack,
        actions = {
            Box {
                AppIconButton(Icons.Filled.MoreVert, "更多") { menu = true }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    SimpleDropdownItem("设置") { menu = false }
                }
            }
        },
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            SectionHeader("状态")
            SettingsRow(
                if (model.ftpRunning) "正在运行" else "未启动",
                null,
                trailing = { SwitchTrailing(model.ftpRunning, true) { model.ftpRunning = it } },
            )
            SettingsRow("网址", "ftp://fec0:5054:ff:fe12:3456:2121/")
            SettingsRow("添加到\u201c快捷设置\u201d", null)

            SectionHeader("配置")
            SettingsRow(
                "匿名登录",
                null,
                enabled = !model.ftpRunning,
                trailing = { SwitchTrailing(true, !model.ftpRunning) { } },
            )
            SettingsRow("用户名", "admin", enabled = !model.ftpRunning)
            SettingsRow("密码", "未设置", enabled = !model.ftpRunning)
            SettingsRow("端口", "2121", enabled = !model.ftpRunning)
            SettingsRow("根文件夹", model.root.name, enabled = !model.ftpRunning)
            SettingsRow(
                "允许写入",
                null,
                enabled = !model.ftpRunning,
                trailing = { SwitchTrailing(true, !model.ftpRunning) { } },
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}
