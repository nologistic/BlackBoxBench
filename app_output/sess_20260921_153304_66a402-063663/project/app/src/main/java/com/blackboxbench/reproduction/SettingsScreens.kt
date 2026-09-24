package com.blackboxbench.reproduction

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@Composable
fun SettingsRow(
    title: String,
    subtitle: String? = null,
    switch: Boolean? = null,
    onSwitch: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().background(GallerySurface)
            .clickable(enabled = onClick != null || switch != null) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 17.sp, color = GalleryText)
            if (subtitle != null) Text(subtitle, fontSize = 14.sp, color = GallerySecondaryText)
        }
        if (switch != null && onSwitch != null) {
            Switch(checked = switch, onCheckedChange = onSwitch)
        }
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text, fontSize = 14.sp, color = GalleryPrimary,
        modifier = Modifier.fillMaxWidth().background(GalleryBackground).padding(start = 16.dp, top = 18.dp, bottom = 6.dp)
    )
}

@Composable
private fun DetailRow(title: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().background(GallerySurface)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(title, fontSize = 17.sp, color = GalleryText, modifier = Modifier.weight(1f))
        Text(value, fontSize = 15.sp, color = GallerySecondaryText)
    }
}

@Composable
fun SettingsScreen(c: AppController) {
    c.settings.revision.value
    var dialog by remember { mutableStateOf<String?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }
    val s = c.settings

    Column(modifier = Modifier.fillMaxSize().background(GalleryBackground)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().background(GalleryTopBar).padding(4.dp)) {
            IconButton(onClick = { c.pop() }) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
            Text("设置", fontSize = 20.sp, color = GalleryText, modifier = Modifier.padding(start = 8.dp))
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item { SectionHeader("外观") }
            item { DetailRow("自定义外观", "") { c.push(Screen.Appearance) } }

            item { SectionHeader("常规") }
            item { DetailRow("语言", s.language.get()) { dialog = "language" } }
            item { DetailRow("更改日期和时间格式", "") { dialog = "dateFormat" } }
            item { DetailRow("文件加载优先事项", s.fileLoadingPriority.get()) { dialog = "priority" } }
            item { DetailRow("管理包含的文件夹", "") { c.push(Screen.ManageFolders(false)) } }
            item { DetailRow("管理排除的文件夹", "") { c.push(Screen.ManageFolders(true)) } }
            item { SettingsRow("显示隐藏项目", switch = s.showHidden.get(), onSwitch = { s.showHidden.set(it) }) }
            item {
                SettingsRow(
                    "搜索所有文件，而不仅限于主屏幕显示的文件夹",
                    switch = s.searchAllFiles.get(), onSwitch = { s.searchAllFiles.set(it) }
                )
            }

            item { SectionHeader("视频") }
            item { SettingsRow("自动播放视频", switch = s.autoplayVideos.get(), onSwitch = { s.autoplayVideos.set(it) }) }
            item { SettingsRow("记住视频上一次播放时的位置", switch = s.rememberVideoPosition.get(), onSwitch = { s.rememberVideoPosition.set(it) }) }
            item { SettingsRow("点按视频时", subtitle = "打开默认播放器应用", switch = s.openVideoWithSystemPlayer.get(), onSwitch = { s.openVideoWithSystemPlayer.set(it) }) }
            item { SettingsRow("使用横向手势在独立屏幕上播放视频", switch = s.videoHorizontalGesture.get(), onSwitch = { s.videoHorizontalGesture.set(it) }) }
            item { SettingsRow("使用纵向滑动手势控制视频音量和亮度", switch = s.videoVerticalGesture.get(), onSwitch = { s.videoVerticalGesture.set(it) }) }

            item { SectionHeader("缩略图") }
            item { SettingsRow("裁剪缩略图为正方形", switch = s.cropSquare.get(), onSwitch = { s.cropSquare.set(it) }) }
            item { SettingsRow("GIF 动图的缩略图显示为动画", switch = s.animateGif.get(), onSwitch = { s.animateGif.set(it) }) }
            item { DetailRow("文件缩略图样式", s.fileThumbnailStyle.get()) { dialog = "fileThumbStyle" } }
            item { DetailRow("文件夹缩略图样式", s.folderThumbnailStyle.get()) { dialog = "folderThumbStyle" } }

            item { SectionHeader("滚动") }
            item { SettingsRow("缩略图水平滚动", switch = s.horizontalScroll.get(), onSwitch = { s.horizontalScroll.set(it) }) }
            item { SettingsRow("应用顶部下拉刷新", switch = s.pullToRefresh.get(), onSwitch = { s.pullToRefresh.set(it) }) }

            item { SectionHeader("全屏显示") }
            item { SettingsRow("以 HDR 模式显示 Ultra HDR 照片", switch = s.hdr.get(), onSwitch = { s.hdr.set(it) }) }
            item { SettingsRow("全屏时使用黑色背景", switch = s.blackBackground.get(), onSwitch = { s.blackBackground.set(it) }) }
            item { SettingsRow("全屏时自动隐藏系统界面", switch = s.hideSystemUi.get(), onSwitch = { s.hideSystemUi.set(it) }) }
            item { SettingsRow("点击屏幕边缘切换文件", switch = s.edgeTapSwitch.get(), onSwitch = { s.edgeTapSwitch.set(it) }) }
            item { SettingsRow("全屏查看照片时保持屏幕开启", switch = s.keepScreenOn.get(), onSwitch = { s.keepScreenOn.set(it) }) }
            item { SettingsRow("使用纵向滑动手势控制图像亮度", switch = s.brightnessGesture.get(), onSwitch = { s.brightnessGesture.set(it) }) }
            item { SettingsRow("使用下滑手势退出全屏", switch = s.swipeDownExit.get(), onSwitch = { s.swipeDownExit.set(it) }) }
            item { SettingsRow("显示刘海 (如果可用)", switch = s.showNotch.get(), onSwitch = { s.showNotch.set(it) }) }
            item { DetailRow("全屏时文件的旋转方向", "跟随系统设置") { } }

            item { SectionHeader("大幅度缩放图像") }
            item { SettingsRow("允许大幅度缩放图像", switch = s.allowDeepZoom.get(), onSwitch = { s.allowDeepZoom.set(it) }) }
            item { SettingsRow("允许使用手势旋转图像", switch = s.allowRotateGesture.get(), onSwitch = { s.allowRotateGesture.set(it) }) }
            item { SettingsRow("双击后 1:1 缩放图像", switch = s.doubleTapZoom1to1.get(), onSwitch = { s.doubleTapZoom1to1.set(it) }) }

            item { SectionHeader("更多详细信息") }
            item { SettingsRow("全屏时显示更多详细信息", switch = s.showMoreDetails.get(), onSwitch = { s.showMoreDetails.set(it) }) }

            item { SectionHeader("安全性") }
            item {
                SettingsRow("用密码保护整个应用", switch = s.protectApp.get(), onSwitch = { on ->
                    if (on) dialog = "setPasswordApp" else dialog = "verifyDisableApp"
                })
            }
            item {
                SettingsRow("用密码保护隐藏项目", switch = s.protectHidden.get(), onSwitch = { on ->
                    if (on) dialog = "setPasswordHidden" else { s.protectHidden.set(false) }
                })
            }
            item {
                SettingsRow("用密码保护文件的删除和移动", switch = s.protectDeleteMove.get(), onSwitch = { on ->
                    if (on) dialog = "setPasswordDelete" else { s.protectDeleteMove.set(false) }
                })
            }

            item { SectionHeader("文件操作") }
            item { SettingsRow("删除文件夹的所有文件后也删除该空文件夹", switch = s.deleteEmptyFolder.get(), onSwitch = { s.deleteEmptyFolder.set(it) }) }
            item { SettingsRow("文件操作后保留原有修改日期", switch = s.keepModifiedDate.get(), onSwitch = { s.keepModifiedDate.set(it) }) }
            item { SettingsRow("不再显示删除确认对话框", switch = s.noDeleteConfirm.get(), onSwitch = { s.noDeleteConfirm.set(it) }) }

            item { SectionHeader("底部按钮") }
            item { SettingsRow("显示底部按钮", switch = s.showBottomButtons.get(), onSwitch = { s.showBottomButtons.set(it) }) }
            item { DetailRow("管理底部按钮", s.bottomButtons.get()) { dialog = "bottomButtons" } }

            item { SectionHeader("回收站") }
            item { SettingsRow("将已删除项目移至回收站", switch = s.trashEnabled.get(), onSwitch = { s.trashEnabled.set(it); c.repo.trashDisabled = !it }) }
            item { SettingsRow("在文件夹界面显示回收站", switch = s.showRecycleBinInFolders.get(), onSwitch = { s.showRecycleBinInFolders.set(it) }) }
            item { SettingsRow("在主屏幕末尾显示回收站", switch = s.showRecycleBinAtEnd.get(), onSwitch = { s.showRecycleBinAtEnd.set(it) }) }
            item { DetailRow("清空回收站", formatSize(c.repo.totalTrashSize())) { dialog = "emptyTrash" } }

            item { SectionHeader("迁移") }
            item { DetailRow("清除缓存", formatSize(c.repo.cacheSize() / 8)) { dialog = "clearCache" } }
            item { DetailRow("导出收藏", "") { toast = "已导出 ${c.favorites().size} 个收藏" } }
            item { DetailRow("导入收藏", "") { toast = "没有可导入的收藏" } }
            item { DetailRow("导出设置", "") { dialog = "exportSettings" } }
            item { DetailRow("导入设置", "") { dialog = "importSettings" } }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    when (dialog) {
        "language" -> RadioListDialog("语言", listOf("中文", "English", "Deutsch", "Français"), s.language.get(),
            onSelect = { s.language.set(it); dialog = null }, onDismiss = { dialog = null })
        "dateFormat" -> RadioListDialog("更改日期和时间格式", listOf("自动", "24 小时", "12 小时"), s.dateFormat.get(),
            onSelect = { s.dateFormat.set(it); dialog = null }, onDismiss = { dialog = null })
        "priority" -> RadioListDialog("文件加载优先事项", listOf("速度", "质量"), s.fileLoadingPriority.get(),
            onSelect = { s.fileLoadingPriority.set(it); dialog = null }, onDismiss = { dialog = null })
        "fileThumbStyle" -> RadioListDialog("文件缩略图样式", listOf("方形", "圆角", "圆形"), s.fileThumbnailStyle.get(),
            onSelect = { s.fileThumbnailStyle.set(it); dialog = null }, onDismiss = { dialog = null })
        "folderThumbStyle" -> RadioListDialog("文件夹缩略图样式", listOf("方形", "圆角", "圆形"), s.folderThumbnailStyle.get(),
            onSelect = { s.folderThumbnailStyle.set(it); dialog = null }, onDismiss = { dialog = null })
        "bottomButtons" -> RadioListDialog(
            "管理底部按钮",
            listOf("收藏", "编辑", "分享", "删除", "全部显示"),
            s.bottomButtons.get(),
            onSelect = {
                s.bottomButtons.set(if (it == "全部显示") "favorite,edit,share,delete" else it)
                dialog = null
            }, onDismiss = { dialog = null }
        )
        "emptyTrash" -> AlertDialog(
            onDismissRequest = { dialog = null },
            text = { Text("要清空回收站吗？", fontSize = 16.sp) },
            confirmButton = { TextButton(onClick = { c.repo.emptyTrash(); dialog = null; toast = "回收站已清空" }) { Text("是") } },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("否") } }
        )
        "clearCache" -> MessageDialog("清除缓存", "缓存已清除", onDismiss = { dialog = null })
        "exportSettings" -> {
            val file = File(c.repo.root.parentFile, "gallery_settings.txt")
            s.saveTo(file)
            MessageDialog("导出设置", "已导出到 ${file.name}", onDismiss = { dialog = null })
        }
        "importSettings" -> {
            val file = File(c.repo.root.parentFile, "gallery_settings.txt")
            if (file.exists()) { s.loadFrom(file.readText()); toast = "已导入设置" } else toast = "没有可导入的设置"
            dialog = null
        }
        "setPasswordApp", "setPasswordHidden", "setPasswordDelete" -> PasswordSetupDialog(
            onCancel = { dialog = null },
            onDone = { type, value ->
                s.setPassword(type, value)
                when (dialog) {
                    "setPasswordApp" -> s.protectApp.set(true)
                    "setPasswordHidden" -> s.protectHidden.set(true)
                    else -> s.protectDeleteMove.set(true)
                }
                dialog = null
                toast = "密码设置成功。如果您不慎遗忘了密码，请重新安装本应用。"
            }
        )
        "verifyDisableApp" -> PasswordPromptDialog(
            settings = s, onCancel = { dialog = null },
            onSuccess = { s.protectApp.set(false); s.clearPassword(); dialog = null; toast = "已关闭密码保护" }
        )
    }

    toast?.let { ToastHost(it) { toast = null } }
}

@Composable
fun PasswordSetupDialog(onCancel: () -> Unit, onDone: (String, String) -> Unit) {
    var tab by remember { mutableStateOf("图案") }
    var first by remember { mutableStateOf("") }
    var buffer by remember { mutableStateOf("") }
    var confirmStage by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    fun accept(value: String) {
        if (!confirmStage) { first = value; buffer = ""; confirmStage = true; message = "请再输入一次 PIN 码" }
        else if (value == first) onDone(if (tab == "PIN 码") "pin" else "pattern", value)
        else { buffer = ""; first = ""; confirmStage = false; message = "两次输入不一致，请重试" }
    }

    AlertDialog(
        onDismissRequest = onCancel,
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("图案", "PIN 码").forEach { option ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f).clickable { tab = option; buffer = ""; first = ""; confirmStage = false }
                        ) {
                            Text(option, fontSize = 16.sp, color = if (tab == option) GalleryText else GallerySecondaryText)
                            Spacer(Modifier.height(4.dp))
                            Box(
                                modifier = Modifier.width(90.dp).height(3.dp)
                                    .background(if (tab == option) GalleryPrimary else Color.Transparent)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Icon(Icons.Default.Lock, contentDescription = null, tint = GalleryPrimary, modifier = Modifier.size(34.dp))
                Spacer(Modifier.height(6.dp))
                Text(
                    message ?: if (tab == "PIN 码") "输入 PIN 码" else "绘制解锁图案",
                    fontSize = 17.sp, color = GalleryText
                )
                Spacer(Modifier.height(10.dp))
                if (tab == "PIN 码") {
                    Text("* ".repeat(buffer.length).trim().ifEmpty { " " }, fontSize = 18.sp, color = GalleryText)
                    Spacer(Modifier.height(10.dp))
                    PinPad(
                        onDigit = { digit ->
                            if (buffer.length < 8) buffer += digit
                        },
                        onClear = { buffer = "" },
                        onConfirm = { if (buffer.isNotEmpty()) accept(buffer) }
                    )
                } else {
                    PatternGrid(onDot = { index ->
                        val value = (buffer + index).take(9)
                        buffer = value
                    })
                    Spacer(Modifier.height(10.dp))
                    TextButton(onClick = { if (buffer.isNotEmpty()) accept(buffer) }) { Text("确定") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onCancel) { Text("取消") } }
    )
}

@Composable
fun PinPad(onDigit: (String) -> Unit, onClear: () -> Unit, onConfirm: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9")).forEach { row ->
            Row {
                row.forEach { digit ->
                    Box(
                        modifier = Modifier.size(84.dp).clickable { onDigit(digit) },
                        contentAlignment = Alignment.Center
                    ) { Text(digit, fontSize = 26.sp, color = GalleryText) }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(84.dp).clickable { onClear() }, contentAlignment = Alignment.Center) {
                Text("C", fontSize = 24.sp, color = GalleryText)
            }
            Box(modifier = Modifier.size(84.dp).clickable { onDigit("0") }, contentAlignment = Alignment.Center) {
                Text("0", fontSize = 26.sp, color = GalleryText)
            }
            Box(modifier = Modifier.size(84.dp).clickable { onConfirm() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Check, contentDescription = "确定", tint = GalleryText)
            }
        }
    }
}

@Composable
fun PatternGrid(onDot: (Int) -> Unit) {
    Column {
        for (row in 0 until 3) {
            Row {
                for (col in 0 until 3) {
                    Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier.size(16.dp).clip(CircleShape).background(GalleryPrimary)
                                .clickable { onDot(row * 3 + col) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PasswordPromptDialog(settings: SettingsStore, onCancel: () -> Unit, onSuccess: () -> Unit) {
    var buffer by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCancel,
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = GalleryPrimary, modifier = Modifier.size(34.dp))
                Spacer(Modifier.height(6.dp))
                Text("输入 PIN 码", fontSize = 17.sp)
                Spacer(Modifier.height(6.dp))
                Text("* ".repeat(buffer.length).trim().ifEmpty { " " }, fontSize = 18.sp)
                PinPad(
                    onDigit = { if (buffer.length < 8) buffer += it },
                    onClear = { buffer = "" },
                    onConfirm = { if (settings.passwordValue.get() == buffer) onSuccess() else buffer = "" }
                )
            }
        },
        confirmButton = { TextButton(onClick = onCancel) { Text("取消") } }
    )
}

@Composable
fun LockScreen(settings: SettingsStore, onUnlocked: () -> Unit) {
    var buffer by remember { mutableStateOf("") }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().background(Color.White)
    ) {
        Spacer(Modifier.height(120.dp))
        Icon(Icons.Default.Lock, contentDescription = null, tint = GalleryPrimary, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(10.dp))
        Text("输入 PIN 码", fontSize = 19.sp, color = GalleryText)
        Spacer(Modifier.height(8.dp))
        Text("* ".repeat(buffer.length).trim().ifEmpty { " " }, fontSize = 20.sp)
        Spacer(Modifier.height(60.dp))
        PinPad(
            onDigit = { if (buffer.length < 8) buffer += it },
            onClear = { buffer = "" },
            onConfirm = { if (settings.passwordValue.get() == buffer) onUnlocked() else buffer = "" }
        )
    }
}

@Composable
fun AppearanceScreen(c: AppController) {
    c.settings.revision.value
    var dialog by remember { mutableStateOf<String?>(null) }
    var warning by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize().background(GalleryBackground)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().background(GalleryTopBar).padding(4.dp)) {
            IconButton(onClick = { c.pop() }) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
            Text("自定义外观", fontSize = 20.sp, modifier = Modifier.padding(start = 8.dp))
        }
        LazyColumn {
            item { SectionHeader("主题和颜色") }
            item { DetailRow("应用主题", c.settings.theme.get()) { dialog = "theme" } }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().background(GallerySurface).clickable { warning = true }.padding(16.dp)
                ) {
                    Text("应用图标颜色", fontSize = 17.sp, modifier = Modifier.weight(1f))
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFF106D1F)))
                }
            }
            item { SectionHeader("字体") }
            item { DetailRow("应用字体", c.settings.fontSize.get()) { dialog = "font" } }
        }
    }
    when (dialog) {
        "theme" -> RadioListDialog("应用主题", listOf("系统默认", "浅色", "深色", "黑色"), c.settings.theme.get(),
            onSelect = { c.settings.theme.set(it); dialog = null }, onDismiss = { dialog = null })
        "font" -> RadioListDialog("应用字体", listOf("系统默认", "衬线", "等宽"), c.settings.fontSize.get(),
            onSelect = { c.settings.fontSize.set(it); dialog = null }, onDismiss = { dialog = null })
    }
    if (warning) {
        MessageDialog(
            "",
            "注意：某些启动器无法正确处理自定义的应用图标。如图标消失，请尝试通过 Google Play 商店或者微件启动对应的应用。启动后请还原为默认的绿色图标色 #106D1F。在某些情形下你可能要重新安装应用。",
            onDismiss = { warning = false }
        )
    }
}

@Composable
fun AboutScreen(c: AppController) {
    var toast by remember { mutableStateOf<String?>(null) }
    Column(modifier = Modifier.fillMaxSize().background(GalleryBackground)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().background(GalleryTopBar).padding(4.dp)) {
            IconButton(onClick = { c.pop() }) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
            Text("关于", fontSize = 20.sp, modifier = Modifier.padding(start = 8.dp))
        }
        LazyColumn {
            item { SectionHeader("支持") }
            item { DetailRow("常见问题", "") { toast = "常见问题" } }
            item { DetailRow("已知问题", "") { toast = "已知问题" } }
            item { DetailRow("hello@fossify.org", "") { toast = "hello@fossify.org" } }
            item { SectionHeader("帮助我们") }
            item { DetailRow("分享给好友", "") { toast = "分享给好友" } }
            item { DetailRow("贡献者", "") { toast = "贡献者" } }
            item { DetailRow("向 Fossify 捐赠", "") { toast = "感谢支持" } }
            item { SectionHeader("社交网络") }
            item { DetailRow("GitHub", "") { toast = "GitHub" } }
            item { DetailRow("Reddit", "") { toast = "Reddit" } }
            item { DetailRow("Telegram", "") { toast = "Telegram" } }
            item { SectionHeader("其他") }
            item { DetailRow("隐私政策", "") { toast = "隐私政策" } }
            item { DetailRow("第三方许可", "") { toast = "第三方许可" } }
            item { DetailRow("版本 1.13.1", "org.fossify.gallery") { } }
        }
    }
    toast?.let { ToastHost(it) { toast = null } }
}

@Composable
fun ManageFoldersScreen(c: AppController, excluded: Boolean) {
    c.settings.revision.value
    val key = if (excluded) c.settings.excludeFolders else c.settings.includeFolders
    val selected = key.get().split(",").filter { it.isNotBlank() }.toMutableList()
    val folders = remember { c.repo.albums.map { it.path } + listOf("DCIM", "Pictures", "Download", "Movies") }
    Column(modifier = Modifier.fillMaxSize().background(GalleryBackground)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().background(GalleryTopBar).padding(4.dp)) {
            IconButton(onClick = { c.pop() }) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
            Text(if (excluded) "管理排除的文件夹" else "管理包含的文件夹", fontSize = 20.sp, modifier = Modifier.padding(start = 8.dp))
        }
        LazyColumn {
            items(folders.distinct()) { folder ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().background(GallerySurface).padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = selected.contains(folder),
                        onCheckedChange = { checked ->
                            if (checked) selected.add(folder) else selected.remove(folder)
                            key.set(selected.joinToString(","))
                        }
                    )
                    Text(folder, fontSize = 17.sp)
                }
            }
        }
    }
}
