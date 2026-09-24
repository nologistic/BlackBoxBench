package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun ViewerScreen(
    store: GalleryStore,
    album: String?,
    startIndex: Int,
    fromBin: Boolean,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val items = remember(store.media, store.binned, store.favorites, store.hidden, album) {
        when (album) {
            "__bin__" -> store.binItems()
            else -> if (album.isNullOrEmpty() || album == "__all__") store.allMedia() else store.itemsOf(album)
        }
    }
    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text("未找到任何项目。", color = Color.White, fontSize = 16.sp)
        }
        return
    }

    val pagerState = rememberPagerState(initialPage = startIndex.coerceIn(0, items.size - 1)) { items.size }
    var menuOpen by remember { mutableStateOf(false) }
    var rotateOpen by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf("") }
    val index = pagerState.currentPage.coerceIn(0, items.size - 1)
    val item = items[index]
    var rotation by remember(item.rel) { mutableStateOf(store.rotations[item.rel] ?: 0) }
    val favorite = store.favorites.contains(item.rel)

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
            }
            Text(
                item.name,
                color = Color.White,
                fontSize = 19.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (rotation != 0) {
                IconButton(onClick = { dialog = "saveas" }) {
                    Icon(Icons.Filled.Check, contentDescription = "确认", tint = Color.White)
                }
            }
            Box {
                IconButton(onClick = { rotateOpen = true }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "旋转", tint = Color.White)
                }
                DropdownMenu(expanded = rotateOpen, onDismissRequest = { rotateOpen = false }) {
                    DropdownMenuItem(text = { Text("向右旋转") }, onClick = {
                        rotateOpen = false
                        rotation = (rotation + 90) % 360
                        store.rotations[item.rel] = rotation
                    })
                    DropdownMenuItem(text = { Text("向左旋转") }, onClick = {
                        rotateOpen = false
                        rotation = (rotation + 270) % 360
                        store.rotations[item.rel] = rotation
                    })
                    DropdownMenuItem(text = { Text("旋转 180°") }, onClick = {
                        rotateOpen = false
                        rotation = (rotation + 180) % 360
                        store.rotations[item.rel] = rotation
                    })
                }
            }
            IconButton(onClick = { dialog = "properties" }) {
                Icon(Icons.Filled.Info, contentDescription = "信息", tint = Color.White)
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "更多", tint = Color.White)
                }
                ViewerMenu(
                    expanded = menuOpen,
                    fromBin = fromBin,
                    onDismiss = { menuOpen = false },
                    onDialog = { dialog = it },
                    onOpenSettings = onOpenSettings
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (item.video) {
                VideoSurface(
                    store = store,
                    item = item,
                    onPlay = { toast(context, "正在播放 ${item.name}") }
                )
            } else {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    val pageItem = items[page]
                    val bitmap = rememberBitmap(
                        File(store.root, pageItem.rel),
                        1080,
                        if (pageItem.rel == item.rel) rotation else (store.rotations[pageItem.rel] ?: 0)
                    )
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (bitmap != null) {
                            androidx.compose.foundation.Image(
                                bitmap = bitmap,
                                contentDescription = pageItem.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = null,
                                tint = Color(0xFFF5C542),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }
            }
        }

        if (store.bottomButtons) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!fromBin) {
                    IconButton(onClick = { store.toggleFavorite(item) }) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = "收藏",
                            tint = if (favorite) Color(0xFFFFC107) else Color.White
                        )
                    }
                }
                IconButton(onClick = { toast(context, "编辑器不可用") }) {
                    Icon(Icons.Filled.Edit, contentDescription = "编辑", tint = Color.White)
                }
                IconButton(onClick = { toast(context, "分享 ${item.name}") }) {
                    Icon(Icons.Filled.Share, contentDescription = "分享", tint = Color.White)
                }
                IconButton(onClick = { dialog = "delete" }) {
                    Icon(Icons.Filled.Delete, contentDescription = "删除", tint = Color.White)
                }
            }
        }
    }

    when (dialog) {
        "properties" -> PropertiesDialog(
            item = item,
            onRemoveExif = { dialog = "exif" },
            onDismiss = { dialog = "" }
        )

        "exif" -> ExifConfirmDialog(
            onConfirm = { toast(context, "已移除 EXIF 数据"); dialog = "" },
            onDismiss = { dialog = "properties" }
        )

        "delete" -> ConfirmDialog(
            title = "删除",
            message = "确定要将 \"${item.name}\" (${formatSize(item.size)}) 移至回收站吗？",
            extraOptions = listOf("本次会话不再询问", "跳过回收站，直接删除文件"),
            onConfirm = {
                store.moveToBin(listOf(item))
                dialog = ""
                if (items.size <= 1) onBack()
            },
            onDismiss = { dialog = "" }
        )

        "restore" -> {
            store.restore(listOf(item))
            toast(context, "已恢复 ${item.name}")
            dialog = ""
            onBack()
        }

        "rename" -> FormDialog(
            title = "重命名",
            fields = listOf("标题" to item.name.substringBeforeLast('.'), "扩展名" to item.ext),
            onConfirm = { values ->
                val ok = store.rename(item, values[0], values.getOrElse(1) { item.ext })
                toast(context, if (ok) "已重命名" else "重命名失败")
                dialog = ""
            },
            onDismiss = { dialog = "" }
        )

        "saveas" -> FormDialog(
            title = "另存为",
            fields = listOf(
                "路径" to displayPath(item.album),
                "文件名" to item.name.substringBeforeLast('.'),
                "扩展名" to item.ext
            ),
            onConfirm = { values ->
                val ok = store.saveAs(
                    item,
                    normalizePath(values[0]),
                    values[1],
                    values.getOrElse(2) { item.ext },
                    rotation
                )
                toast(context, if (ok) "已保存" else "保存失败")
                store.rotations.remove(item.rel)
                rotation = 0
                dialog = ""
            },
            onDismiss = { dialog = "" }
        )

        "hide" -> {
            store.setHidden(listOf(item), true)
            toast(context, "已隐藏 ${item.name}")
            dialog = ""
            onBack()
        }

        "copyto", "moveto" -> TargetPickerDialog(
            store = store,
            title = "选择目标",
            onConfirm = { target ->
                val count = if (dialog == "copyto") store.copyItems(listOf(item), target) else store.moveItems(listOf(item), target)
                toast(context, "已处理 $count 个项目")
                dialog = ""
            },
            onDismiss = { dialog = "" }
        )

        "clipboard" -> {
            toast(context, "已复制到剪贴板")
            dialog = ""
        }

        "openwith" -> {
            toast(context, "没有可用的其他应用")
            dialog = ""
        }

        "setas" -> {
            toast(context, "已设置为壁纸")
            dialog = ""
        }

        "print" -> {
            toast(context, "没有可用的打印服务")
            dialog = ""
        }

        "resize" -> {
            toast(context, "调整大小完成")
            dialog = ""
        }

        "map" -> {
            toast(context, "此图片没有位置信息")
            dialog = ""
        }

        "shortcut" -> {
            toast(context, "已创建快捷方式")
            dialog = ""
        }

        "slideshow" -> SlideshowDialog(
            initial = store.slideshow,
            onConfirm = { settings -> store.slideshow = settings; dialog = "" },
            onDismiss = { dialog = "" }
        )

        "orientation" -> RadioDialog(
            title = "更改画面方向",
            options = listOf("自动旋转", "横向", "纵向"),
            selected = "自动旋转",
            onConfirm = { _, _ -> dialog = "" },
            onDismiss = { dialog = "" }
        )

        else -> if (dialog.isNotEmpty()) dialog = ""
    }
}

@Composable
private fun ViewerMenu(
    expanded: Boolean,
    fromBin: Boolean,
    onDismiss: () -> Unit,
    onDialog: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    var orientationOpen by remember { mutableStateOf(false) }
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        if (fromBin) {
            DropdownMenuItem(text = { Text("恢复此文件") }, onClick = { onDismiss(); onDialog("restore") })
            DropdownMenuItem(text = { Text("彻底删除") }, onClick = { onDismiss(); onDialog("delete") })
        } else {
            DropdownMenuItem(text = { Text("重命名") }, onClick = { onDismiss(); onDialog("rename") })
            DropdownMenuItem(text = { Text("隐藏") }, onClick = { onDismiss(); onDialog("hide") })
        }
        DropdownMenuItem(text = { Text("复制到剪贴板") }, onClick = { onDismiss(); onDialog("clipboard") })
        DropdownMenuItem(text = { Text("复制到") }, onClick = { onDismiss(); onDialog("copyto") })
        DropdownMenuItem(text = { Text("移动到") }, onClick = { onDismiss(); onDialog("moveto") })
        DropdownMenuItem(text = { Text("创建快捷方式") }, onClick = { onDismiss(); onDialog("shortcut") })
        DropdownMenuItem(text = { Text("打开方式") }, onClick = { onDismiss(); onDialog("openwith") })
        DropdownMenuItem(text = { Text("设置为") }, onClick = { onDismiss(); onDialog("setas") })
        Box {
            DropdownMenuItem(
                text = { Text("更改画面方向") },
                trailingIcon = { Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp)) },
                onClick = { orientationOpen = true }
            )
            DropdownMenu(expanded = orientationOpen, onDismissRequest = { orientationOpen = false }) {
                DropdownMenuItem(text = { Text("自动旋转") }, onClick = { orientationOpen = false; onDismiss() })
                DropdownMenuItem(text = { Text("横向") }, onClick = { orientationOpen = false; onDismiss() })
                DropdownMenuItem(text = { Text("纵向") }, onClick = { orientationOpen = false; onDismiss() })
            }
        }
        DropdownMenuItem(text = { Text("打印") }, onClick = { onDismiss(); onDialog("print") })
        DropdownMenuItem(text = { Text("调整大小") }, onClick = { onDismiss(); onDialog("resize") })
        DropdownMenuItem(text = { Text("在地图上显示") }, onClick = { onDismiss(); onDialog("map") })
        DropdownMenuItem(text = { Text("幻灯片") }, onClick = { onDismiss(); onDialog("slideshow") })
        DropdownMenuItem(text = { Text("设置") }, onClick = { onDismiss(); onOpenSettings() })
    }
}

@Composable
private fun VideoSurface(store: GalleryStore, item: MediaItem, onPlay: () -> Unit) {
    var playing by remember { mutableStateOf(true) }
    var positionMs by remember { mutableStateOf(0L) }
    val duration = item.durationMs.coerceAtLeast(1000L)
    LaunchedEffect(playing) {
        while (playing) {
            delay(500)
            positionMs = (positionMs + 500) % duration
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101014))
            .clickable { playing = !playing; if (playing) onPlay() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(Color(0x33FFFFFF), androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (playing) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(2) {
                            Box(
                                modifier = Modifier
                                    .size(width = 12.dp, height = 40.dp)
                                    .background(Color.White, RoundedCornerShape(3.dp))
                            )
                        }
                    }
                } else {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = "播放",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            Text(
                item.name,
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 20.dp)
            )
            Text(
                "${formatDuration(positionMs)} / ${formatDuration(duration)}",
                color = Color(0xFFBDBDBD),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp)
                .height(4.dp)
                .background(Color(0x55FFFFFF), RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(positionMs.toFloat() / duration.toFloat())
                    .height(4.dp)
                    .background(Color(0xFFFFFFFF), RoundedCornerShape(2.dp))
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Recycle bin
// ---------------------------------------------------------------------------

@Composable
fun BinScreen(
    store: GalleryStore,
    onBack: () -> Unit,
    onOpenViewer: (Int) -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val items = store.binItems()
    var menuOpen by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().background(BarTint).padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = Color(0xFF26243B))
            }
            Text("回收站", fontSize = 19.sp, color = Color(0xFF26243B), modifier = Modifier.weight(1f))
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "更多", tint = Color(0xFF26243B))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("过滤显示的文件") }, onClick = { menuOpen = false })
                DropdownMenuItem(text = { Text("更改视图类型") }, onClick = { menuOpen = false })
                DropdownMenuItem(text = { Text("临时显示隐藏项目") }, onClick = { menuOpen = false; store.tempShowHidden = !store.tempShowHidden })
                DropdownMenuItem(text = { Text("清空回收站") }, onClick = {
                    menuOpen = false
                    store.emptyBin()
                    toast(context, "回收站已清空")
                })
                DropdownMenuItem(text = { Text("清空并禁用回收站") }, onClick = {
                    menuOpen = false
                    store.emptyBin()
                    store.recycleEnabled = false
                    store.save()
                    toast(context, "回收站已清空并禁用")
                })
                DropdownMenuItem(text = { Text("恢复所有文件") }, onClick = {
                    menuOpen = false
                    store.restore(items)
                    toast(context, "已恢复所有文件")
                })
                DropdownMenuItem(text = { Text("分组方式") }, onClick = { menuOpen = false })
                DropdownMenuItem(text = { Text("列数") }, onClick = { menuOpen = false })
                DropdownMenuItem(text = { Text("设置") }, onClick = { menuOpen = false; onOpenSettings() })
            }
        }

        if (items.isEmpty()) {
            EmptyHint("未找到任何项目。")
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(store.mediaColumns), modifier = Modifier.fillMaxSize()) {
                itemsIndexed(items) { index, item ->
                    MediaTile(
                        item = item,
                        file = File(store.root, item.rel),
                        selected = false,
                        showName = store.showFileNames,
                        onClick = { onOpenViewer(index) },
                        onLongClick = { onOpenViewer(index) }
                    )
                }
            }
        }
    }
}
