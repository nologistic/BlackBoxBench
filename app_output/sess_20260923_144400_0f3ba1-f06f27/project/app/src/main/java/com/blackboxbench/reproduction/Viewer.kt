package com.blackboxbench.reproduction

import android.graphics.Bitmap
import android.graphics.Matrix
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch

@Composable
fun ViewerScreen(g: GalleryState, relPath: String, album: String, isRecycle: Boolean) {
    var menuOpen by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<String?>(null) }
    val items = g.albumItems(album, isRecycle)
    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize().background(Color.Black))
        return
    }
    val startIndex = items.indexOfFirst { it.relPath == relPath }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = startIndex) { items.size }
    val scope = rememberCoroutineScope()
    val current = items.getOrNull(pagerState.currentPage) ?: items.first()

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTap(kind = "back", tint = Color.White) { g.pop() }
            Text(
                current.displayName,
                color = Color.White,
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconTap(kind = "rotate", tint = Color.White) {
                g.message("已旋转 90°")
            }
            IconTap(kind = "info", tint = Color.White) { dialog = "info" }
            Box {
                IconTap(kind = "dots", tint = Color.White) { menuOpen = true }
                SimpleMenu(menuOpen, { menuOpen = false }) {
                    if (isRecycle) {
                        MenuEntry("复制到剪贴板") { menuOpen = false; g.message("已复制到剪贴板") }
                        MenuEntry("复制到") { menuOpen = false; dialog = "copy" }
                        MenuEntry("移动到") { menuOpen = false; dialog = "move" }
                        MenuEntry("创建快捷方式") { menuOpen = false; g.message("无法创建快捷方式") }
                        MenuEntry("打开方式") { menuOpen = false; g.message("没有可用的应用") }
                        MenuEntry("设置为") { menuOpen = false; dialog = "setas" }
                        MenuEntry("恢复此文件") {
                            menuOpen = false
                            g.library.restore(current)
                            g.refresh()
                            g.pop()
                        }
                        MenuEntry("更改画面方向") { menuOpen = false; g.message("已更改画面方向") }
                        MenuEntry("打印") { menuOpen = false; g.message("没有可用的打印服务") }
                        MenuEntry("调整大小") { menuOpen = false; dialog = "resize" }
                        MenuEntry("在地图上显示") { menuOpen = false; g.message("此图片没有位置信息") }
                        MenuEntry("幻灯片") { menuOpen = false }
                        MenuEntry("设置") { menuOpen = false; g.push(Screen.Settings) }
                    } else {
                        MenuEntry("重命名") { menuOpen = false; dialog = "rename" }
                        MenuEntry(if (current.hidden) "取消隐藏" else "隐藏") {
                            menuOpen = false
                            g.library.setHidden(current, !current.hidden)
                            g.refresh()
                        }
                        MenuEntry("复制到剪贴板") { menuOpen = false; g.message("已复制到剪贴板") }
                        MenuEntry("复制到") { menuOpen = false; dialog = "copy" }
                        MenuEntry("移动到") { menuOpen = false; dialog = "move" }
                        MenuEntry("创建快捷方式") { menuOpen = false; g.message("无法创建快捷方式") }
                        MenuEntry("打开方式") { menuOpen = false; g.message("没有可用的应用") }
                        MenuEntry("设置为") { menuOpen = false; dialog = "setas" }
                        MenuEntry("更改画面方向") { menuOpen = false; g.message("已更改画面方向") }
                        MenuEntry("打印") { menuOpen = false; g.message("没有可用的打印服务") }
                        MenuEntry("调整大小") { menuOpen = false; dialog = "resize" }
                        MenuEntry("在地图上显示") { menuOpen = false; g.message("此图片没有位置信息") }
                        MenuEntry("幻灯片") { menuOpen = false }
                        MenuEntry("设置") { menuOpen = false; g.push(Screen.Settings) }
                    }
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) { page ->
            val item = items[page]
            val bitmap = remember(item.relPath, item.modified) {
                val file = g.library.fileOf(item)
                Thumbs.full(file, item.isVideo)
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = item.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            if (!isRecycle) {
                IconTap(
                    kind = if (current.favorite) "star_filled" else "star_outline",
                    tint = Color.White,
                    size = 26.dp,
                ) {
                    g.library.setFavorite(current, !current.favorite)
                    g.refresh()
                }
            }
            IconTap(kind = "pencil", tint = Color.White, size = 26.dp) {
                g.push(Screen.Editor(current.relPath, album))
            }
            IconTap(kind = "share", tint = Color.White, size = 26.dp) {
                g.message("已分享 ${current.displayName}")
            }
            IconTap(kind = "trash", tint = Color.White, size = 26.dp) {
                g.selection = setOf(current.relPath)
                dialog = "delete"
            }
        }
    }

    when (dialog) {
        "info" -> PropertiesDialog(g, current, onDismiss = { dialog = null })
        "rename" -> {
            g.selection = setOf(current.relPath)
            RenameDialog(g, album, items, onDismiss = { dialog = null })
        }
        "resize" -> {
            g.selection = setOf(current.relPath)
            ResizeDialog(g, items, onDismiss = { dialog = null })
        }
        "setas" -> SetAsDialog(g, onDismiss = { dialog = null })
        "copy" -> FolderPickerDialog(g, "复制到", onDismiss = { dialog = null }) { target ->
            g.library.copyItems(listOf(current), target)
            g.refresh()
            dialog = null
            g.message("已复制到 $target")
        }
        "move" -> FolderPickerDialog(g, "移动到", onDismiss = { dialog = null }) { target ->
            g.library.moveItems(listOf(current), target)
            g.refresh()
            dialog = null
            g.pop()
        }
        "delete" -> DeleteDialog(g, items, isRecycle, onDismiss = {
            dialog = null
            if (items.size <= 1) g.pop()
        })
    }
}

@Composable
fun VideoScreen(g: GalleryState, relPath: String, album: String) {
    val item = g.library.itemByPath(relPath)
    var menuOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTap(kind = "back", tint = Color.White) { g.pop() }
            Text(
                item?.displayName ?: "",
                color = Color.White,
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconTap(kind = "info", tint = Color.White) { }
            Box {
                IconTap(kind = "dots", tint = Color.White) { menuOpen = true }
                SimpleMenu(menuOpen, { menuOpen = false }) {
                    MenuEntry("重命名") { menuOpen = false }
                    MenuEntry("隐藏") { menuOpen = false }
                    MenuEntry("复制到") { menuOpen = false }
                    MenuEntry("移动到") { menuOpen = false }
                    MenuEntry("打开方式") { menuOpen = false; g.message("没有可用的应用") }
                    MenuEntry("设置为") { menuOpen = false }
                    MenuEntry("更改画面方向") { menuOpen = false }
                    MenuEntry("打印") { menuOpen = false }
                    MenuEntry("调整大小") { menuOpen = false }
                    MenuEntry("在地图上显示") { menuOpen = false }
                    MenuEntry("幻灯片") { menuOpen = false }
                    MenuEntry("设置") { menuOpen = false; g.push(Screen.Settings) }
                }
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            if (item != null) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        VideoView(context).apply {
                            setVideoPath(g.library.fileOf(item).absolutePath)
                            val controller = MediaController(context)
                            controller.setAnchorView(this)
                            setMediaController(controller)
                            setOnPreparedListener { player ->
                                player.isLooping = true
                                start()
                            }
                        }
                    }
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            IconTap(kind = "star_outline", tint = Color.White, size = 26.dp) { }
            IconTap(kind = "share", tint = Color.White, size = 26.dp) { g.message("已分享视频") }
            IconTap(kind = "trash", tint = Color.White, size = 26.dp) {
                item?.let {
                    g.library.moveToTrash(it)
                    g.refresh()
                    g.pop()
                }
            }
        }
    }
}

@Composable
fun EditorScreen(g: GalleryState, relPath: String, album: String) {
    val item = g.library.itemByPath(relPath)
    if (item == null) {
        Box(Modifier.fillMaxSize().background(Color.Black))
        return
    }
    val original = remember(relPath) {
        val file = g.library.fileOf(item)
        Thumbs.full(file, false)
    }
    var rotation by remember { mutableStateOf(0f) }
    var flipX by remember { mutableStateOf(false) }
    var flipY by remember { mutableStateOf(false) }
    var cropRatio by remember { mutableStateOf<String?>(null) }
    var showCropMenu by remember { mutableStateOf(false) }

    fun render(): Bitmap? {
        val source = original ?: return null
        var bitmap = source
        if (rotation != 0f || flipX || flipY) {
            val matrix = Matrix().apply {
                postRotate(rotation)
                postScale(if (flipX) -1f else 1f, if (flipY) -1f else 1f)
            }
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
        cropRatio?.let { ratio ->
            val parts = ratio.split(":")
            val rw = parts[0].toFloat()
            val rh = parts[1].toFloat()
            val target = rw / rh
            val current = bitmap.width.toFloat() / bitmap.height.toFloat()
            bitmap = if (current > target) {
                val w = (bitmap.height * target).toInt()
                Bitmap.createBitmap(bitmap, (bitmap.width - w) / 2, 0, w, bitmap.height)
            } else {
                val h = (bitmap.width / target).toInt()
                Bitmap.createBitmap(bitmap, 0, (bitmap.height - h) / 2, bitmap.width, h)
            }
        }
        return bitmap
    }

    val preview = remember(rotation, flipX, flipY, cropRatio, original) { render() }

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTap(kind = "back", tint = Color.White) { g.pop() }
            Text("编辑", color = Color.White, fontSize = 18.sp, modifier = Modifier.weight(1f))
            Text(
                "保存",
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier
                    .clickable {
                        val bitmap = preview
                        if (bitmap != null) {
                            val created = g.library.writeCopy(item, bitmap)
                            g.refresh()
                            g.pop()
                            g.message(if (created != null) "已保存 ${created.displayName}" else "保存失败")
                        }
                    }
                    .padding(horizontal = 16.dp)
            )
        }
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            if (preview != null) {
                Image(
                    bitmap = preview.asImageBitmap(),
                    contentDescription = "edit",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(72.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            EditorAction("crop", "裁剪") { showCropMenu = true }
            EditorAction("rotate", "旋转") { rotation += 90f }
            EditorAction("flip", "翻转") { flipX = !flipX }
            EditorAction("flip", "上下") { flipY = !flipY }
        }
        Box {
            SimpleMenu(showCropMenu, { showCropMenu = false }) {
                MenuEntry("自由") { cropRatio = null; showCropMenu = false }
                MenuEntry("1:1") { cropRatio = "1:1"; showCropMenu = false }
                MenuEntry("16:9") { cropRatio = "16:9"; showCropMenu = false }
                MenuEntry("9:16") { cropRatio = "9:16"; showCropMenu = false }
                MenuEntry("4:3") { cropRatio = "4:3"; showCropMenu = false }
            }
        }
    }
}

@Composable
private fun EditorAction(kind: String, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp),
    ) {
        Glyph(size = 26.dp, color = Color.White, kind = kind)
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color.White, fontSize = 13.sp)
    }
}
