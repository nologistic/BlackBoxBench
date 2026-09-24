package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(g: GalleryState) {
    var dialog by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().background(Color.White)) {
        TopPill(hint = "设置", showBack = true, onBack = { g.pop() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            SettingsSection("外观")
            ClickRow("自定义外观") { g.message("使用默认外观") }

            SettingsSection("常规")
            ClickRow("语言", g.text("language", "中文")) { }
            ClickRow("更改日期和时间格式", g.text("dateTimeFormat", "默认")) { }
            ClickRow("文件加载优先事项", g.text("fileLoadingPriority", "速度")) { }
            ClickRow("管理包含的文件夹") { dialog = "included" }
            ClickRow("管理排除的文件夹") { dialog = "excluded" }
            SwitchRow("显示隐藏项目", g.bool("showHidden", false)) { g.setBool("showHidden", it) }
            SwitchRow(
                "搜索所有文件，而不仅限于主屏幕显示的文件夹",
                g.bool("searchAllFiles", false)
            ) { g.setBool("searchAllFiles", it) }

            SettingsSection("视频")
            SwitchRow("自动播放视频", g.bool("autoplayVideos", false)) { g.setBool("autoplayVideos", it) }
            SwitchRow(
                "记住视频上一次播放时的位置",
                g.bool("rememberVideoPosition", true)
            ) { g.setBool("rememberVideoPosition", it) }
            SwitchRow(
                "使用横向手势在独立屏幕上播放视频",
                g.bool("videoHorizontal", false)
            ) { g.setBool("videoHorizontal", it) }
            SwitchRow(
                "使用纵向滑动手势控制视频音量和亮度",
                g.bool("videoVertical", true)
            ) { g.setBool("videoVertical", it) }
            ClickRow("点按视频时", g.text("videoTap", "打开默认播放器应用")) { }

            SettingsSection("缩略图")
            SwitchRow("裁剪缩略图为正方形", g.bool("cropThumbnails", true)) { g.setBool("cropThumbnails", it) }
            SwitchRow("GIF 动图的缩略图显示为动画", g.bool("animateGif", false)) { g.setBool("animateGif", it) }
            ClickRow("文件缩略图样式", g.text("fileThumbStyle", "方形")) { }
            ClickRow("文件夹缩略图样式", g.text("folderThumbStyle", "方形")) { }

            SettingsSection("滚动")
            SwitchRow("缩略图水平滚动", g.bool("horizontalScroll", false)) { g.setBool("horizontalScroll", it) }
            SwitchRow("应用顶部下拉刷新", g.bool("pullToRefresh", true)) { g.setBool("pullToRefresh", it) }

            SettingsSection("全屏显示")
            SwitchRow("全屏时将屏幕调到最大亮度", g.bool("maxBrightness", false)) { g.setBool("maxBrightness", it) }
            SwitchRow("以 HDR 模式显示 Ultra HDR 照片", g.bool("ultraHdr", true)) { g.setBool("ultraHdr", it) }
            SwitchRow("全屏时使用黑色背景", g.bool("fullscreenBlack", true)) { g.setBool("fullscreenBlack", it) }
            SwitchRow("全屏时自动隐藏系统界面", g.bool("hideSystemUi", false)) { g.setBool("hideSystemUi", it) }
            SwitchRow("点击屏幕边缘切换文件", g.bool("tapEdges", false)) { g.setBool("tapEdges", it) }
            SwitchRow("全屏查看照片时保持屏幕开启", g.bool("keepScreenOn", true)) { g.setBool("keepScreenOn", it) }
            SwitchRow("使用纵向滑动手势控制图像亮度", g.bool("imageBrightness", false)) { g.setBool("imageBrightness", it) }
            SwitchRow("使用下滑手势退出全屏", g.bool("swipeDownExit", true)) { g.setBool("swipeDownExit", it) }
            SwitchRow("显示刘海（如果可用）", g.bool("showNotch", true)) { g.setBool("showNotch", it) }
            ClickRow("全屏时文件的旋转方向", g.text("rotationDirection", "跟随系统设置")) { }

            SettingsSection("大幅度缩放图像")
            SwitchRow("允许大幅度缩放图像", g.bool("deepZoom", true)) { g.setBool("deepZoom", it) }
            SwitchRow("允许使用手势旋转图像", g.bool("gestureRotate", true)) { g.setBool("gestureRotate", it) }
            SwitchRow("使用最高画质显示图像", g.bool("highestQuality", false)) { g.setBool("highestQuality", it) }
            SwitchRow("两指双击后 1:1 缩放图像", g.bool("doubleTapZoom", false)) { g.setBool("doubleTapZoom", it) }

            SettingsSection("更多详细信息")
            SwitchRow("全屏时显示更多详细信息", g.bool("moreDetails", false)) { g.setBool("moreDetails", it) }

            SettingsSection("安全性")
            SwitchRow("用密码保护整个应用", g.bool("lockApp", false)) { value ->
                if (value) dialog = "password_app" else g.setBool("lockApp", false)
            }
            SwitchRow("用密码保护隐藏项目", g.bool("lockHidden", false)) { value ->
                if (value) dialog = "password_hidden" else g.setBool("lockHidden", false)
            }
            SwitchRow("用密码保护文件的删除和移动", g.bool("lockDelete", false)) { value ->
                if (value) dialog = "password_delete" else g.setBool("lockDelete", false)
            }

            SettingsSection("文件操作")
            SwitchRow("删除文件夹的所有文件后也删除该空文件夹", g.bool("deleteEmptyFolder", false)) {
                g.setBool("deleteEmptyFolder", it)
            }
            SwitchRow("文件操作后保留原有修改日期", g.bool("keepModifiedDate", true)) {
                g.setBool("keepModifiedDate", it)
            }
            SwitchRow("不再显示删除确认对话框", g.bool("skipDeleteConfirm", false)) {
                g.setBool("skipDeleteConfirm", it)
            }

            SettingsSection("底部按钮")
            SwitchRow("显示底部按钮", g.bool("showBottomButtons", true)) { g.setBool("showBottomButtons", it) }
            ClickRow("管理底部按钮") { dialog = "bottom_buttons" }

            SettingsSection("回收站")
            SwitchRow("将已删除项目移至回收站", g.bool("moveToRecycleBin", true)) {
                g.setBool("moveToRecycleBin", it)
            }
            SwitchRow("在文件夹界面显示回收站", g.bool("showRecycleInFolders", true)) {
                g.setBool("showRecycleInFolders", it)
            }
            SwitchRow("在主屏幕末尾显示回收站", g.bool("showRecycleAtEnd", false)) {
                g.setBool("showRecycleAtEnd", it)
            }
            ClickRow("清空回收站", formatSize(g.library.trashSize())) {
                g.library.emptyTrash()
                g.refresh()
                g.message("回收站已清空")
            }

            SettingsSection("迁移")
            ClickRow("清除缓存", formatSize(g.library.cacheSize())) {
                g.library.clearCache()
                g.refresh()
                g.message("缓存已清除")
            }
            ClickRow("导出收藏") { g.message("已导出收藏") }
            ClickRow("导入收藏") { g.message("已导入收藏") }
            ClickRow("导出设置") { g.message("已导出设置") }
            ClickRow("导入设置") { g.message("已导入设置") }
            Spacer(Modifier.height(40.dp))
        }
    }

    when (dialog) {
        "password_app" -> PasswordSetupDialog(g, "lockApp", onDismiss = { dialog = null })
        "password_hidden" -> PasswordSetupDialog(g, "lockHidden", onDismiss = { dialog = null })
        "password_delete" -> PasswordSetupDialog(g, "lockDelete", onDismiss = { dialog = null })
        "bottom_buttons" -> BottomButtonsDialog(g, onDismiss = { dialog = null })
        "included" -> ManageFoldersDialog(g, "包含", "includedFolders", onDismiss = { dialog = null })
        "excluded" -> ManageFoldersDialog(g, "排除", "excludedFolders", onDismiss = { dialog = null })
    }
}

@Composable
private fun BottomButtonsDialog(g: GalleryState, onDismiss: () -> Unit) {
    val all = listOf(
        "收藏/取消收藏", "编辑", "分享", "删除", "旋转", "属性",
        "更改画面方向", "幻灯片", "在地图上显示", "隐藏/取消隐藏文件", "重命名"
    )
    val saved = g.list("bottomButtons").ifEmpty { listOf("收藏/取消收藏", "编辑", "分享", "删除") }
    val state = remember { all.map { saved.contains(it) }.toMutableList() }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogSurface,
        text = {
            Column(Modifier.height(520.dp).verticalScroll(rememberScrollState())) {
                all.forEachIndexed { index, label ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { state[index] = !state[index] }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = state[index],
                            onCheckedChange = { state[index] = it },
                            colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = Primary),
                        )
                        Text(label, fontSize = 17.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                g.setList("bottomButtons", all.filterIndexed { index, _ -> state[index] })
                onDismiss()
            }) { Text("确定", color = Primary, fontSize = 17.sp) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = Primary, fontSize = 17.sp) } },
    )
}

@Composable
private fun ManageFoldersDialog(g: GalleryState, label: String, key: String, onDismiss: () -> Unit) {
    val folders = g.library.folderTree().drop(1).map { it.first }
    val saved = g.list(key)
    val state = remember { folders.map { saved.contains(it) }.toMutableList() }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogSurface,
        title = { Text("管理${label}的文件夹", color = Primary, fontSize = 20.sp) },
        text = {
            Column(Modifier.height(460.dp).verticalScroll(rememberScrollState())) {
                folders.forEachIndexed { index, name ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { state[index] = !state[index] }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = state[index],
                            onCheckedChange = { state[index] = it },
                            colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = Primary),
                        )
                        Text(name, fontSize = 17.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                g.setList(key, folders.filterIndexed { index, _ -> state[index] })
                onDismiss()
            }) { Text("确定", color = Primary, fontSize = 17.sp) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = Primary, fontSize = 17.sp) } },
    )
}

@Composable
fun AboutScreen(g: GalleryState) {
    Column(Modifier.fillMaxSize().background(Color.White)) {
        TopPill(hint = "关于", showBack = true, onBack = { g.pop() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            SettingsSection("支持")
            ClickRow("常见问题") { }
            ClickRow("已知问题") { }
            ClickRow("hello@fossify.org") { }
            HorizontalDivider(color = DividerGrey)
            SettingsSection("帮助我们")
            ClickRow("分享给好友") { g.message("已分享") }
            ClickRow("贡献者") { }
            ClickRow("向 Fossify 捐赠") { }
            HorizontalDivider(color = DividerGrey)
            SettingsSection("社交网络")
            ClickRow("GitHub") { }
            ClickRow("Reddit") { }
            ClickRow("Telegram") { }
            HorizontalDivider(color = DividerGrey)
            SettingsSection("其他")
            ClickRow("隐私政策") { }
            ClickRow("第三方许可") { }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun PasswordSetupDialog(g: GalleryState, key: String, onDismiss: () -> Unit) {
    var tab by remember { mutableStateOf(0) }
    var stage by remember { mutableStateOf(0) }
    var pin by remember { mutableStateOf("") }
    var firstPin by remember { mutableStateOf("") }
    var pattern by remember { mutableStateOf<List<Int>>(emptyList()) }
    var firstPattern by remember { mutableStateOf<List<Int>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    fun finish(value: String) {
        g.setText("password", value)
        g.setBool(key, true)
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogSurface,
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { tab = 0; stage = 0; error = null }
                    ) {
                        Text("图案", fontSize = 17.sp, color = if (tab == 0) Primary else HintGrey)
                        Box(
                            Modifier
                                .padding(top = 6.dp)
                                .width(78.dp)
                                .height(3.dp)
                                .background(if (tab == 0) Primary else Color.Transparent)
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { tab = 1; stage = 0; error = null }
                    ) {
                        Text("PIN 码", fontSize = 17.sp, color = if (tab == 1) Primary else HintGrey)
                        Box(
                            Modifier
                                .padding(top = 6.dp)
                                .width(78.dp)
                                .height(3.dp)
                                .background(if (tab == 1) Primary else Color.Transparent)
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                Glyph(size = 34.dp, color = PrimaryDark, kind = "lock")
                Spacer(Modifier.height(8.dp))
                if (tab == 1) {
                    Text(if (stage == 0) "输入 PIN 码" else "再次输入 PIN 码", fontSize = 17.sp)
                    Spacer(Modifier.height(10.dp))
                    PinPad(pin) { digit ->
                        when {
                            digit == "C" -> pin = ""
                            digit == "OK" -> when {
                                pin.length < 4 -> error = "PIN 码至少需要 4 位"
                                stage == 0 -> {
                                    firstPin = pin
                                    pin = ""
                                    stage = 1
                                    error = null
                                }
                                firstPin == pin -> finish(pin)
                                else -> {
                                    error = "两次输入的 PIN 码不一致"
                                    pin = ""
                                    stage = 0
                                }
                            }
                            pin.length < 8 -> {
                                pin += digit
                                error = null
                            }
                        }
                    }
                } else {
                    Text(if (stage == 0) "绘制解锁图案" else "再次绘制解锁图案", fontSize = 17.sp)
                    Spacer(Modifier.height(10.dp))
                    PatternPad(pattern) { points ->
                        when {
                            points.size < 4 -> {
                                error = "至少需要连接 4 个点"
                                pattern = emptyList()
                            }
                            stage == 0 -> {
                                firstPattern = points
                                pattern = emptyList()
                                stage = 1
                                error = null
                            }
                            firstPattern == points -> finish(points.joinToString(","))
                            else -> {
                                error = "两次绘制的图案不一致"
                                pattern = emptyList()
                                stage = 0
                            }
                        }
                    }
                }
                if (error != null) {
                    Text(error!!, color = Color(0xFFB3261E), fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消", color = Primary, fontSize = 17.sp) } },
    )
}

@Composable
private fun PinPad(value: String, onKey: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            if (value.isEmpty()) "—" else "•".repeat(value.length),
            fontSize = 20.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("C", "0", "OK"))
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { key ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clickable { onKey(key) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(key, fontSize = 20.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PatternPad(selected: List<Int>, onDone: (List<Int>) -> Unit) {
    var current by remember { mutableStateOf(selected) }
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        current = emptyList()
                        dotAt(offset, size.width.toFloat(), size.height.toFloat())?.let { current = listOf(it) }
                    },
                    onDragEnd = { onDone(current) },
                    onDrag = { change, _ ->
                        dotAt(change.position, size.width.toFloat(), size.height.toFloat())?.let { dot ->
                            if (!current.contains(dot)) current = current + dot
                        }
                    },
                )
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val radius = w * 0.045f
            fun centerOf(index: Int): Offset {
                val row = index / 3
                val col = index % 3
                return Offset(w * (0.2f + col * 0.3f), h * (0.2f + row * 0.3f))
            }
            for (i in 0 until 9) {
                drawCircle(PrimaryDark, radius = radius, center = centerOf(i))
            }
            for (i in 0 until current.size - 1) {
                drawLine(Primary, centerOf(current[i]), centerOf(current[i + 1]), strokeWidth = w * 0.02f)
            }
            current.forEach {
                drawCircle(Primary, radius = radius * 1.8f, center = centerOf(it), alpha = 0.35f)
            }
        }
    }
}

private fun dotAt(offset: Offset, w: Float, h: Float): Int? {
    for (i in 0 until 9) {
        val row = i / 3
        val col = i % 3
        val cx = w * (0.2f + col * 0.3f)
        val cy = h * (0.2f + row * 0.3f)
        if (kotlin.math.abs(offset.x - cx) < w * 0.12f && kotlin.math.abs(offset.y - cy) < h * 0.12f) return i
    }
    return null
}

@Composable
fun LockScreen(g: GalleryState, onUnlock: () -> Unit) {
    val stored = g.text("password", "")
    val isPattern = stored.contains(",")
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(120.dp))
        Glyph(size = 44.dp, color = PrimaryDark, kind = "lock")
        Spacer(Modifier.height(16.dp))
        if (isPattern) {
            Text("绘制解锁图案", fontSize = 18.sp)
            Spacer(Modifier.height(16.dp))
            Box(Modifier.width(300.dp)) {
                PatternPad(emptyList()) { points ->
                    if (points.joinToString(",") == stored) onUnlock() else error = "图案错误"
                }
            }
        } else {
            Text("输入 PIN 码", fontSize = 18.sp)
            Spacer(Modifier.height(16.dp))
            Box(Modifier.width(280.dp)) {
                PinPad(pin) { key ->
                    when (key) {
                        "C" -> pin = ""
                        "OK" -> if (pin == stored) onUnlock() else {
                            error = "PIN 码错误"
                            pin = ""
                        }
                        else -> if (pin.length < 8) pin += key
                    }
                }
            }
        }
        if (error != null) {
            Text(error!!, color = Color(0xFFB3261E), fontSize = 15.sp, modifier = Modifier.padding(top = 12.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("画廊已被密码保护", fontSize = 15.sp, color = HintGrey, textAlign = TextAlign.Center)
    }
}
