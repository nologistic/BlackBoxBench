package com.blackboxbench.reproduction

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import kotlinx.coroutines.delay

@Composable
fun ViewerScreen(c: AppController, albumPath: String, ids: List<String>, startIndex: Int) {
    val items = ids.mapNotNull { c.repo.findById(it) }.filter { !it.deleted }
    if (items.isEmpty()) { c.pop(); return }
    val pagerState = rememberPagerState(initialPage = startIndex.coerceIn(0, items.size - 1)) { items.size }
    var chrome by remember { mutableStateOf(true) }
    var menuOpen by remember { mutableStateOf(false) }
    var directionMenu by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<String?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }
    var slideshow by remember { mutableStateOf(false) }
    var slideshowInterval by remember { mutableStateOf(5) }
    var pendingRotation by remember { mutableStateOf<Bitmap?>(null) }
    var editorBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var editMode by remember { mutableStateOf(false) }
    var zoom by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val current = items.getOrNull(pagerState.currentPage) ?: items.first()
    val file = c.repo.fileOf(current)

    LaunchedEffect(slideshow) {
        if (slideshow) {
            while (slideshow) {
                delay(slideshowInterval * 1000L)
                if (!slideshow) break
                val next = pagerState.currentPage + 1
                if (next >= items.size) { slideshow = false; toast = "幻灯片放映完毕" } else pagerState.animateScrollToPage(next)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(if (editMode) Color(0xFF1B1B1B) else Color.Black)) {
        if (chrome && !editMode && !slideshow) {
            ViewerTopBar(
                title = current.displayName,
                hasPendingSave = pendingRotation != null,
                onBack = { c.pop() },
                onSave = { dialog = "saveAs" },
                onRotate = { dialog = "rotatePending" },
                onInfo = { dialog = "properties" },
                onMore = { menuOpen = true }
            )
            MenuEntries(
                entries = listOf(
                    "重命名" to { dialog = "rename" },
                    if (current.hidden) "取消隐藏" to { c.repo.setHidden(current, false); toast = "已取消隐藏" }
                    else "隐藏" to { c.repo.setHidden(current, true); toast = "已隐藏" },
                    "复制到剪贴板" to { ClipboardHolder.copiedName.value = current.fileName; toast = "已复制到剪贴板" },
                    "复制到" to { dialog = "copy" },
                    "移动到" to { dialog = "move" },
                    "创建快捷方式" to { dialog = "shortcut" },
                    "打开方式" to { openExternally(c, file, current) },
                    "设置为" to { dialog = "setAs" },
                    "更改画面方向" to { directionMenu = true },
                    "打印" to { toast = "没有可用的打印服务" },
                    "调整大小" to { dialog = "resize" },
                    "在地图上显示" to { toast = "没有可用的地图应用" },
                    "幻灯片" to { dialog = "slideshow" },
                    "设置" to { c.push(Screen.Settings) },
                    if (current.favorite) "取消收藏" to {
                        c.repo.toggleFavorite(current); toast = "已取消收藏"
                    } else "添加到收藏" to {
                        c.repo.toggleFavorite(current); toast = "已添加到收藏"
                    },
                    "编辑" to {
                        editorBitmap = runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
                        editMode = true
                    },
                    "分享" to { shareItem(c, file, current) },
                    "删除" to { dialog = "delete" }
                ),
                expanded = menuOpen, onDismiss = { menuOpen = false },
                hasArrow = setOf("更改画面方向")
            )
            MenuEntries(
                entries = listOf(
                    "向右旋转" to { rotateItem(c, current) { pendingRotation = it }; directionMenu = false },
                    "向左旋转" to { rotateItem(c, current, 270) { pendingRotation = it }; directionMenu = false },
                    "旋转 180°" to { rotateItem(c, current, 180) { pendingRotation = it }; directionMenu = false }
                ),
                expanded = directionMenu, onDismiss = { directionMenu = false }
            )
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (editMode) {
                EditorPane(
                    item = current,
                    bitmap = editorBitmap,
                    onCancel = { editMode = false; editorBitmap = null },
                    onSave = { bitmap -> editorBitmap = bitmap; dialog = "saveAs" }
                )
            } else {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    val item = items[page]
                    val pageFile = c.repo.fileOf(item)
                    if (item.isVideo) {
                        Box(
                            modifier = Modifier.fillMaxSize().clickable { openExternally(c, pageFile, item) },
                            contentAlignment = Alignment.Center
                        ) {
                            MediaThumb(pageFile, item, Modifier.fillMaxSize(), target = 1080, contentScale = ContentScale.Fit)
                            Icon(Icons.Default.PlayArrow, contentDescription = "播放", tint = Color.White, modifier = Modifier.size(72.dp))
                        }
                    } else {
                        val bitmap = rememberFullBitmap(pageFile)
                        Box(
                            modifier = Modifier.fillMaxSize().pointerInput(item.id) {
                                detectTransformGestures { _, pan, gestureZoom, _ ->
                                    zoom = (zoom * gestureZoom).coerceIn(1f, 6f)
                                    offset = if (zoom > 1f) offset + pan else Offset.Zero
                                }
                            }.clickable {
                                if (zoom > 1f) { zoom = 1f; offset = Offset.Zero } else chrome = !chrome
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap, contentDescription = item.fileName,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize().graphicsLayer(
                                        scaleX = zoom, scaleY = zoom, translationX = offset.x, translationY = offset.y
                                    )
                                )
                            } else {
                                Text("无法打开该文件", color = Color.White)
                            }
                        }
                    }
                }
            }

            if (slideshow) {
                Text(
                    "点击退出幻灯片", color = Color(0x88FFFFFF), fontSize = 13.sp,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)
                )
            }
            ClipboardHolder.copiedName.value?.let { copied ->
                Box(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clip(RoundedCornerShape(28.dp)).background(Color(0xEE2B2B2B)).padding(8.dp)
                    ) {
                        MediaThumb(file, current, Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)))
                        Spacer(Modifier.width(8.dp))
                        Text(copied, color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }

        if (chrome && !editMode && !slideshow) {
            ViewerBottomBar(
                favorite = current.favorite,
                onFavorite = {
                    c.repo.toggleFavorite(current)
                    toast = if (current.favorite) "已添加到收藏" else "已取消收藏"
                },
                onEdit = {
                    editorBitmap = runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
                    editMode = true
                },
                onShare = { shareItem(c, file, current) },
                onDelete = { dialog = "delete" }
            )
        }
    }

    when (dialog) {
        "properties" -> PropertiesDialog(
            item = current, path = file.parent ?: "",
            onRemoveExif = { c.repo.removeExif(current); dialog = null; toast = "已移除 EXIF 数据" },
            onDismiss = { dialog = null }
        )
        "delete" -> {
            var skipTrash by remember { mutableStateOf(false) }
            var noAsk by remember { mutableStateOf(false) }
            DeleteConfirmDialog(
                count = 1, noAskAgain = noAsk, onNoAskAgainChange = { noAsk = it },
                skipTrash = skipTrash, onSkipTrashChange = { skipTrash = it },
                onConfirm = {
                    if (noAsk) c.settings.noDeleteConfirm.set(true)
                    c.repo.delete(current, skipTrash)
                    dialog = null
                    if (items.size <= 1) c.pop()
                    else c.replaceTop(Screen.Viewer(albumPath, ids, pagerState.currentPage.coerceAtMost(items.size - 2)))
                }, onDismiss = { dialog = null }
            )
        }
        "rename" -> TextFieldDialog(
            "重命名", "标题", current.fileName.substringBeforeLast('.'),
            extraLabel = "扩展名", extraInitial = current.fileName.substringAfterLast('.', "jpg"),
            onConfirm = { name, ext -> c.repo.rename(current, "$name.$ext"); dialog = null; toast = "已重命名" },
            onDismiss = { dialog = null }
        )
        "resize" -> ResizeDialog(
            current,
            onConfirm = { w, h, folder, name ->
                c.repo.resize(current, w, h, folder, name); dialog = null; toast = "已保存缩放副本"
            }, onDismiss = { dialog = null }
        )
        "copy", "move" -> TargetPickerDialog(
            albums = c.albums(),
            coverOf = { c.repo.coverOf(it, true) },
            fileOf = { c.repo.fileOf(it) },
            onPick = { target ->
                if (dialog == "copy") c.repo.copyTo(current, target) else c.repo.moveTo(current, target)
                toast = if (dialog == "copy") "已复制" else "已移动"
                dialog = null
            },
            onOtherFolders = { dialog = "browse" },
            onDismiss = { dialog = null }
        )
        "browse" -> FolderBrowserDialog(
            rootLabel = "内部存储空间", root = c.repo.root,
            onPick = { path ->
                val rel = path.removePrefix(c.repo.root.path).trimStart('/')
                c.repo.copyTo(current, rel); dialog = null; toast = "已复制到 $rel"
            },
            onCreateFolder = { parent -> File(parent, "新建文件夹").mkdirs(); toast = "已创建 新建文件夹" },
            onDismiss = { dialog = null }
        )
        "setAs" -> SetAsDialog(
            onPick = { option -> setAs(c, file, current, option); dialog = null },
            onDismiss = { dialog = null }
        )
        "shortcut" -> TextFieldDialog(
            "创建快捷方式", "名称", current.fileName,
            onConfirm = { _, _ -> dialog = null; toast = "已创建快捷方式" }, onDismiss = { dialog = null }
        )
        "slideshow" -> SlideshowDialog(
            onStart = { interval, _, _, _, _ -> slideshowInterval = interval; slideshow = true; chrome = true; dialog = null },
            onDismiss = { dialog = null }
        )
        "rotatePending" -> RadioListDialog(
            "更改画面方向", listOf("向右旋转", "向左旋转", "旋转 180°"), "",
            onSelect = { option ->
                val degrees = when (option) { "向右旋转" -> 90; "向左旋转" -> 270; else -> 180 }
                rotateItem(c, current, degrees) { pendingRotation = it }
                dialog = null
            }, onDismiss = { dialog = null }
        )
        "saveAs" -> {
            val bitmap = editorBitmap ?: pendingRotation
            if (bitmap != null) {
                SaveAsDialog(
                    defaultFolder = file.parent ?: c.repo.root.path,
                    defaultName = current.fileName.substringBeforeLast('.') + "_edited.jpg",
                    onConfirm = { folder, name ->
                        val rel = folder.removePrefix(c.repo.root.path).trimStart('/')
                        c.repo.saveCopy(current, rel.ifEmpty { current.albumPath }, name, bitmap)
                        editorBitmap = null; pendingRotation = null; editMode = false
                        dialog = null; toast = "已另存为 $name"
                    },
                    onDismiss = { dialog = null }
                )
            } else dialog = null
        }
    }

    toast?.let { ToastHost(it) { toast = null } }
}

@Composable
private fun ViewerTopBar(
    title: String,
    hasPendingSave: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onRotate: () -> Unit,
    onInfo: () -> Unit,
    onMore: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().background(Color(0xCC000000)).padding(horizontal = 4.dp, vertical = 8.dp)
    ) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Color.White) }
        Text(
            title, color = Color.White, fontSize = 19.sp, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 8.dp)
        )
        if (hasPendingSave) {
            IconButton(onClick = onSave) { Icon(Icons.Default.Check, contentDescription = "保存", tint = Color.White) }
        }
        IconButton(onClick = onRotate) { Icon(Icons.Default.Refresh, contentDescription = "旋转", tint = Color.White) }
        IconButton(onClick = onInfo) { Icon(Icons.Default.Info, contentDescription = "信息", tint = Color.White) }
        IconButton(onClick = onMore) { Icon(Icons.Default.MoreVert, contentDescription = "更多", tint = Color.White) }
    }
}

@Composable
private fun ViewerBottomBar(
    favorite: Boolean,
    onFavorite: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    // The row of actions is deliberately lifted off the bottom edge: the very bottom
    // strip of the screen is partly covered by the system gesture area.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().height(100.dp).background(Color(0xCC000000)).padding(bottom = 64.dp)
    ) {
        BarAction(Modifier.weight(1f), onFavorite) {
            Icon(if (favorite) Icons.Default.Star else Icons.Default.FavoriteBorder, contentDescription = "收藏", tint = Color.White)
        }
        BarAction(Modifier.weight(1f), onEdit) { Icon(Icons.Default.Create, contentDescription = "编辑", tint = Color.White) }
        BarAction(Modifier.weight(1f), onShare) { Icon(Icons.Default.Share, contentDescription = "分享", tint = Color.White) }
        BarAction(Modifier.weight(1f), onDelete) { Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color.White) }
    }
}

@Composable
private fun BarAction(modifier: Modifier, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = modifier.fillMaxHeight().clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

private fun rotateItem(c: AppController, item: MediaItem, degrees: Int = 90, onDone: (Bitmap) -> Unit) {
    val file = c.repo.fileOf(item)
    val bitmap = runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull() ?: return
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    onDone(rotated)
}

fun contentUriFor(c: AppController, file: File): Uri =
    androidx.core.content.FileProvider.getUriForFile(
        c.appContext, "com.blackboxbench.reproduction.fileprovider", file
    )

private fun shareItem(c: AppController, file: File, item: MediaItem) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = if (item.isVideo) "video/*" else "image/*"
        putExtra(Intent.EXTRA_STREAM, contentUriFor(c, file))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(intent, "分享")
    c.launch(chooser)
}

private fun openExternally(c: AppController, file: File, item: MediaItem) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(contentUriFor(c, file), if (item.isVideo) "video/*" else "image/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    c.launch(intent)
}

private fun setAs(c: AppController, file: File, item: MediaItem, option: String) {
    val intent = Intent(Intent.ACTION_ATTACH_DATA).apply {
        setDataAndType(contentUriFor(c, file), if (item.isVideo) "video/*" else "image/*")
        putExtra("mimeType", if (item.isVideo) "video/*" else "image/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    c.launch(intent)
}

/** In-app editor: rotate, flip and crop; saving writes a new file next to the original. */
@Composable
private fun EditorPane(
    item: MediaItem,
    bitmap: Bitmap?,
    onCancel: () -> Unit,
    onSave: (Bitmap) -> Unit
) {
    var working by remember(bitmap) { mutableStateOf(bitmap) }
    val shown = working ?: bitmap
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1B1B1B))) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            IconButton(onClick = onCancel) { Icon(Icons.Default.Clear, contentDescription = "取消", tint = Color.White) }
            Text("编辑 ${item.fileName}", color = Color.White, fontSize = 17.sp, modifier = Modifier.weight(1f))
            TextButton(onClick = { working?.let(onSave) }) { Text("保存", color = Color.White) }
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (shown != null) {
                Image(
                    bitmap = shown.asImageBitmap(), contentDescription = null,
                    contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize()
                )
            }
        }
        if (shown != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().height(96.dp).padding(bottom = 56.dp)
            ) {
                EditorAction(Modifier.weight(1f), { working = transform(shown) { m -> m.postRotate(90f) } }, "向右旋转")
                EditorAction(Modifier.weight(1f), { working = transform(shown) { m -> m.postRotate(270f) } }, "向左旋转")
                EditorAction(Modifier.weight(1f), { working = transform(shown) { m -> m.postScale(-1f, 1f) } }, "左右翻转")
                EditorAction(Modifier.weight(1f), { working = transform(shown) { m -> m.postScale(1f, -1f) } }, "上下翻转")
                EditorAction(Modifier.weight(1f), { working = centerCrop(shown) }, "裁剪 1:1")
            }
        }
    }
}

@Composable
private fun EditorAction(modifier: Modifier, onClick: () -> Unit, label: String) {
    Box(
        modifier = modifier.fillMaxHeight().clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(label, color = Color.White, fontSize = 14.sp) }
}

private fun transform(source: Bitmap, block: (Matrix) -> Unit): Bitmap {
    val matrix = Matrix()
    block(matrix)
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
}

private fun centerCrop(source: Bitmap): Bitmap {
    val size = minOf(source.width, source.height)
    val x = (source.width - size) / 2
    val y = (source.height - size) / 2
    return Bitmap.createBitmap(source, x, y, size, size)
}
