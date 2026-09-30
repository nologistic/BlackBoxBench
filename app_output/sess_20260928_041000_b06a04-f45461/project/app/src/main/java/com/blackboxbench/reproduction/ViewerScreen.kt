package com.blackboxbench.reproduction

import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File

@Composable
fun ViewerScreen(
    state: GalleryState,
    ids: List<String>,
    startIndex: Int,
    onClose: () -> Unit,
    onEdit: (String) -> Unit,
) {
    state.revision
    val list = remember(ids, state.revision) { ids.mapNotNull { state.byId(it) } }
    var index by remember { mutableStateOf(startIndex.coerceIn(0, (list.size - 1).coerceAtLeast(0))) }
    var menuOpen by remember { mutableStateOf(false) }
    var rotateOpen by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<String?>(null) }
    var deleteConfirm by remember { mutableStateOf(false) }

    if (list.isEmpty()) { onClose(); return }
    val item = list[index.coerceIn(0, list.size - 1)]

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (item.isVideo) {
                VideoPane(item, Modifier.fillMaxSize())
            } else {
                val context = LocalContext.current
                val bmp = remember(item.uri, item.name) { GalleryState.bitmap(context, item) }
                var drag by remember { mutableStateOf(0f) }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(index, list.size) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (drag < -120 && index < list.size - 1) index++
                                    else if (drag > 120 && index > 0) index--
                                    drag = 0f
                                },
                                onHorizontalDrag = { _, delta -> drag += delta },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (bmp != null) {
                        Image(bmp.asImageBitmap(), contentDescription = null,
                            contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                    }
                }
            }

            // ---- top bar (overlaid) ----
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButtonBox(onClick = { onClose() }) { IconBack(Color.White) }
                Text(
                    item.name, color = Color.White, fontSize = 18.sp, maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                )
                Box {
                    IconButtonBox(onClick = { rotateOpen = !rotateOpen }) { IconRotateHistory(Color.White) }
                    DropdownMenu(expanded = rotateOpen, onDismissRequest = { rotateOpen = false }) {
                        listOf("向右旋转", "向左旋转", "旋转 180°").forEach {
                            DropdownMenuItem(text = { Text(it, fontSize = 14.sp) },
                                onClick = { rotateOpen = false })
                        }
                    }
                }
                IconButtonBox(onClick = { dialog = "属性" }) { IconInfo(Color.White) }
                Box {
                    IconButtonBox(onClick = { menuOpen = !menuOpen }) { IconDots(Color.White) }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        val items = listOf(
                            "重命名", if (item.hidden) "取消隐藏" else "隐藏", "复制到剪贴板", "复制到",
                            "移动到", "创建快捷方式", "打开方式", "设置为", "更改画面方向", "打印",
                            "调整大小", "在地图上显示", "幻灯片", "设置"
                        )
                        items.forEach { label ->
                            DropdownMenuItem(text = { Text(label, fontSize = 14.sp) }, onClick = {
                                menuOpen = false
                                when (label) {
                                    "隐藏" -> { state.setHidden(listOf(item.id), true); state.persist() }
                                    "取消隐藏" -> { state.setHidden(listOf(item.id), false); state.persist() }
                                    "更改画面方向" -> dialog = "方向"
                                    "属性" -> dialog = "属性"
                                    else -> dialog = label
                                }
                            })
                        }
                    }
                }
            }
        }

        // ---- bottom action bar (real layout row, not an overlay) ----
        Row(
            modifier = Modifier.fillMaxWidth().height(72.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActionSlot(enabled = true, onClick = {
                state.toggleFavorite(item.id); state.persist()
            }) { IconStar(if (item.favorite) Palette.star else Color.White, item.favorite) }
            ActionSlot(enabled = true, onClick = { onEdit(item.id) }) { IconEdit(Color.White) }
            ActionSlot(enabled = true, onClick = { }) { IconShare(Color.White) }
            ActionSlot(enabled = true, onClick = { deleteConfirm = true }) { IconTrash(Color.White) }
        }
    }

    when (dialog) {
        null -> {}
        "属性" -> PropertiesDialog(item) { dialog = null }
        "重命名" -> RenameDialog(item, onConfirm = { n ->
            state.rename(item.id, n); state.persist(); dialog = null
        }, onClose = { dialog = null })
        "调整大小" -> ResizeDialog(item) { dialog = null }
        "幻灯片" -> SlideshowDialog { dialog = null }
        "复制到", "移动到" -> CopyToDialog(state, item) { dialog = null }
        "方向" -> OrientationDialog { dialog = null }
        "设置为" -> SetAsDialog { dialog = null }
        else -> dialog = null
    }

    if (deleteConfirm) {
        var skipAsk by remember { mutableStateOf(false) }
        var skipBin by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { deleteConfirm = false },
            title = {},
            text = {
                Column {
                    Text("确定要将 \"${item.name}\" (${formatSize(item.sizeBytes)}) 移至回收站吗？",
                        fontSize = 15.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = skipAsk, onCheckedChange = { skipAsk = it })
                        Text("本次会话不再询问", fontSize = 14.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = skipBin, onCheckedChange = { skipBin = it })
                        Text("跳过回收站，直接删除文件", fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (skipBin || !state.recycleBinEnabled) state.deletePermanently(listOf(item.id))
                    else state.delete(listOf(item.id))
                    state.persist()
                    deleteConfirm = false
                    onClose()
                }) { Text("是") }
            },
            dismissButton = { TextButton(onClick = { deleteConfirm = false }) { Text("否") } },
        )
    }
}

/** A wide bottom-bar slot with a generous touch target. */
@Composable
fun ActionSlot(enabled: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .width(72.dp)
            .fillMaxHeight()
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun OrientationDialog(onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("更改画面方向") },
        text = {
            Column {
                listOf("强制竖屏", "强制横屏", "强制横屏（反向）", "使用默认画面方向").forEach {
                    Text(it, fontSize = 15.sp,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("确定") } },
        dismissButton = { TextButton(onClick = onClose) { Text("取消") } },
    )
}

@Composable
private fun SetAsDialog(onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("设置为") },
        text = {
            Column {
                listOf("相册 · 壁纸", "通讯录 · 联系人照片", "图库 · 壁纸").forEach {
                    Text(it, fontSize = 15.sp,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("确定") } },
        dismissButton = { TextButton(onClick = onClose) { Text("取消") } },
    )
}

@Composable
private fun VideoPane(item: MediaItem, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var playing by remember { mutableStateOf(false) }
    var position by remember { mutableStateOf(0) }
    var duration by remember { mutableStateOf(item.durationMs.toInt()) }
    var videoView by remember { mutableStateOf<VideoView?>(null) }

    val path = remember(item.id) { materializeVideo(context, item) }

    Box(modifier = modifier) {
        if (path != null) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setVideoPath(path)
                        setOnPreparedListener { duration = it.duration }
                        setOnCompletionListener { playing = false }
                        videoView = this
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text("无法播放视频", color = Color.White, modifier = Modifier.align(Alignment.Center))
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .padding(bottom = 12.dp, start = 16.dp, end = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("1x", color = Color.White, fontSize = 14.sp)
                Spacer(Modifier.width(20.dp))
                ActionSlot(enabled = true, onClick = {
                    videoView?.let { v ->
                        if (playing) v.pause() else v.start()
                        playing = !playing
                        position = v.currentPosition
                    }
                }) { if (playing) TwoBars(Color.White) else IconPlay(Color.White) }
                Spacer(Modifier.width(20.dp))
                ActionSlot(enabled = true, onClick = { }) { Speaker(Color.White) }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatDuration(position.toLong()), color = Color.White, fontSize = 12.sp)
                Spacer(Modifier.width(8.dp))
                LinearProgressIndicator(
                    progress = { if (duration > 0) position.toFloat() / duration else 0f },
                    modifier = Modifier.weight(1f).height(3.dp),
                    color = Color.White,
                    trackColor = Color(0x55FFFFFF),
                )
                Spacer(Modifier.width(8.dp))
                Text(formatDuration(duration.toLong()), color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun TwoBars(tint: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(20.dp)) {
        val s = size.minDimension
        drawRoundRect(tint, androidx.compose.ui.geometry.Offset(s * 0.24f, s * 0.16f),
            androidx.compose.ui.geometry.Size(s * 0.18f, s * 0.68f))
        drawRoundRect(tint, androidx.compose.ui.geometry.Offset(s * 0.58f, s * 0.16f),
            androidx.compose.ui.geometry.Size(s * 0.18f, s * 0.68f))
    }
}

@Composable
private fun Speaker(tint: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(20.dp)) {
        val s = size.minDimension
        val p = androidx.compose.ui.graphics.Path().apply {
            moveTo(s * 0.12f, s * 0.36f); lineTo(s * 0.34f, s * 0.36f); lineTo(s * 0.56f, s * 0.16f)
            lineTo(s * 0.56f, s * 0.84f); lineTo(s * 0.34f, s * 0.64f); lineTo(s * 0.12f, s * 0.64f); close()
        }
        drawPath(p, tint)
        drawArc(tint, -50f, 100f, false,
            androidx.compose.ui.geometry.Offset(s * 0.44f, s * 0.24f),
            androidx.compose.ui.geometry.Size(s * 0.44f, s * 0.52f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(s * 0.08f))
    }
}

/** Copies an asset-backed video into the app cache so VideoView can play it. */
fun materializeVideo(context: android.content.Context, item: MediaItem): String? {
    if (item.isFile) return item.path.takeIf { File(it).exists() }
    val out = File(context.cacheDir, item.name)
    if (!out.exists()) {
        runCatching {
            context.assets.open(item.path).use { input ->
                out.outputStream().use { input.copyTo(it) }
            }
        }.onFailure { return null }
    }
    return out.absolutePath
}
