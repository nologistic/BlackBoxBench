package com.blackboxbench.reproduction

import android.graphics.Bitmap
import android.graphics.Matrix
import android.widget.VideoView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ViewerScreen(app: AppState, folder: String, startIndex: Int, slideshow: Boolean, toast: (String) -> Unit) {
    val repo = app.repo
    val showHidden = app.showHiddenTemp.value || repo.showHiddenSetting.value
    val media = remember(repo.items.size, repo.version.value, repo.favorites.value, folder) {
        sortedMedia(repo.mediaInFolder(folder, showHidden), repo.sortMode.value, repo.sortAsc.value)
    }
    if (media.isEmpty()) {
        LaunchedEffect(Unit) { app.pop() }
        return
    }
    val pagerState = rememberPagerState(initialPage = startIndex.coerceIn(0, media.size - 1)) { media.size }
    var menu by remember { mutableStateOf(false) }
    var rotateMenu by remember { mutableStateOf(false) }
    var showProperties by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showCopyMove by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var showResize by remember { mutableStateOf(false) }
    var rotation by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(pagerState.currentPage) { rotation = 0f }

    if (slideshow) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(3000)
                val next = (pagerState.currentPage + 1) % media.size
                pagerState.animateScrollToPage(next)
            }
        }
    }

    val item = media[pagerState.currentPage.coerceIn(0, media.size - 1)]
    val isFav = repo.favorites.value.contains(item.id)

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val pageItem = media[page]
            if (pageItem.isVideo) {
                VideoPage(repo, pageItem)
            } else {
                val bmp = remember(pageItem.id) { repo.fullBitmap(pageItem) }
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = pageItem.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationZ = if (page == pagerState.currentPage) rotation else 0f },
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }

        // top bar
        Row(
            Modifier.fillMaxWidth().align(Alignment.TopCenter)
                .background(Color(0x66000000))
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { app.pop() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = Color.White)
            }
            Text(
                item.name,
                color = Color.White,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (rotation != 0f && !item.isVideo) {
                IconButton(onClick = {
                    val bmp = repo.fullBitmap(item)
                    if (bmp != null) {
                        val m = Matrix().apply { postRotate(rotation) }
                        val out = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
                        repo.saveEditedCopy(item, out, "_rotated")
                        toast("已保存")
                        rotation = 0f
                    }
                }) {
                    Text("✓", color = Color.White, fontSize = 20.sp)
                }
            }
            if (!item.isVideo) {
                Box {
                    IconButton(onClick = { rotateMenu = true }) {
                        Icon(Icons.Default.Refresh, "旋转", tint = Color.White)
                    }
                    DropdownMenu(expanded = rotateMenu, onDismissRequest = { rotateMenu = false }) {
                        DropdownMenuItem(text = { Text("向右旋转") }, onClick = { rotation = (rotation + 90f) % 360f; rotateMenu = false })
                        DropdownMenuItem(text = { Text("向左旋转") }, onClick = { rotation = (rotation - 90f + 360f) % 360f; rotateMenu = false })
                        DropdownMenuItem(text = { Text("旋转 180°") }, onClick = { rotation = (rotation + 180f) % 360f; rotateMenu = false })
                    }
                }
            }
            IconButton(onClick = { showProperties = true }) {
                Icon(Icons.Default.Info, "属性", tint = Color.White)
            }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Default.MoreVert, "菜单", tint = Color.White)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("属性") }, onClick = { menu = false; showProperties = true })
                    DropdownMenuItem(text = { Text("重命名") }, onClick = { menu = false; showRename = true })
                    DropdownMenuItem(text = { Text("隐藏") }, onClick = {
                        repo.hideMedia(listOf(item.id)); menu = false; app.pop(); toast("已隐藏")
                    })
                    DropdownMenuItem(text = { Text("复制到") }, onClick = { menu = false; showCopyMove = "复制到" to true })
                    DropdownMenuItem(text = { Text("移动到") }, onClick = { menu = false; showCopyMove = "移动到" to false })
                    DropdownMenuItem(text = { Text("打开方式") }, onClick = { menu = false; toast("未找到可打开的应用") })
                    DropdownMenuItem(text = { Text("设置为") }, onClick = { menu = false; toast("未找到可设置的应用") })
                    if (!item.isVideo) {
                        DropdownMenuItem(text = { Text("调整大小") }, onClick = { menu = false; showResize = true })
                        DropdownMenuItem(text = { Text("编辑") }, onClick = { menu = false; app.navigate(Screen.Editor(item.id)) })
                    }
                    DropdownMenuItem(
                        text = { Text(if (isFav) "取消收藏" else "添加到收藏") },
                        onClick = { repo.toggleFavorite(item.id); menu = false })
                    DropdownMenuItem(text = { Text("修复拍摄日期") }, onClick = { menu = false; toast("已尝试修复拍摄日期") })
                    DropdownMenuItem(text = { Text("幻灯片") }, onClick = { menu = false; toast("幻灯片放映中") })
                    DropdownMenuItem(text = { Text("设置") }, onClick = { menu = false; app.navigate(Screen.Settings) })
                }
            }
        }

        // bottom bar (photos only; videos have their own controls)
        if (!item.isVideo) {
        Row(
            Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                .background(Color(0x66000000))
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { repo.toggleFavorite(item.id) }) {
                StarBadge(filled = isFav)
            }
            IconButton(onClick = {
                if (item.isVideo) toast("暂不支持编辑视频") else app.navigate(Screen.Editor(item.id))
            }) {
                Icon(Icons.Default.Edit, "编辑", tint = Color.White)
            }
            IconButton(onClick = { toast("未找到可分享到的应用") }) {
                Icon(Icons.Default.Share, "分享", tint = Color.White)
            }
            IconButton(onClick = { showDelete = true }) {
                Icon(Icons.Default.Delete, "删除", tint = Color.White)
            }
        }
        }
    }

    if (showProperties) PropertiesDialog(repo, item) { showProperties = false }
    if (showRename) RenameMediaDialog(item, { showRename = false }) { newName ->
        repo.renameMedia(item, newName); showRename = false
    }
    if (showDelete) DeleteConfirmDialog(
        text = "确定要将 ${item.name}（${formatSize(item.size)}）移至回收站吗？",
        onDismiss = { showDelete = false },
        onConfirm = { skip ->
            repo.deleteMedia(setOf(item.id), skip)
            showDelete = false
            app.pop()
        }
    )
    showCopyMove?.let { (title, isCopy) ->
        CopyMoveDialog(
            title = title,
            folders = repo.folderNames(true, true).map { it.name },
            onDismiss = { showCopyMove = null },
            onPick = { target ->
                if (isCopy) {
                    repo.copyMedia(setOf(item.id), target)
                    toast("已复制 1 个项目")
                } else {
                    repo.moveMedia(setOf(item.id), target)
                    toast("已移动 1 个项目")
                    app.pop()
                }
                showCopyMove = null
            }
        )
    }
    if (showResize) ResizeDialog(item, { showResize = false }) { w, h ->
        repo.resizeMedia(item, w, h); showResize = false; toast("已调整大小")
    }
}

@Composable
fun VideoPage(repo: GalleryRepository, item: MediaItem) {
    var playing by remember { mutableStateOf(false) }
    var muted by remember { mutableStateOf(false) }
    var position by remember { mutableIntStateOf(0) }
    var speedLabel by remember { mutableStateOf("1x") }
    val videoView = remember { mutableStateOf<VideoView?>(null) }
    val mediaPlayer = remember { mutableStateOf<android.media.MediaPlayer?>(null) }

    LaunchedEffect(playing) {
        while (playing) {
            videoView.value?.let { position = it.currentPosition }
            delay(200)
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    setVideoPath(repo.fileFor(item).absolutePath)
                    setOnPreparedListener { mp ->
                        mp.setVolume(if (muted) 0f else 1f, if (muted) 0f else 1f)
                        mediaPlayer.value = mp
                    }
                    setOnCompletionListener { playing = false }
                    videoView.value = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        if (!playing) {
            Box(
                Modifier.align(Alignment.Center).size(64.dp)
                    .background(Color(0x88000000), androidx.compose.foundation.shape.CircleShape)
                    .clickable {
                        videoView.value?.start()
                        playing = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, "播放", tint = Color.White, modifier = Modifier.size(40.dp))
            }
        }
        Column(
            Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                .background(Color(0x66000000))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatDuration(position / 1000), color = Color.White, fontSize = 12.sp)
                Slider(
                    value = position.toFloat(),
                    onValueChange = { v ->
                        position = v.toInt()
                        videoView.value?.seekTo(position)
                    },
                    valueRange = 0f..(item.durationSec * 1000).coerceAtLeast(1).toFloat(),
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )
                Text(formatDuration(item.durationSec), color = Color.White, fontSize = 12.sp)
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    speedLabel,
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable {
                        speedLabel = when (speedLabel) {
                            "1x" -> "1.5x"; "1.5x" -> "2x"; else -> "1x"
                        }
                    }.padding(4.dp)
                )
                Text(
                    if (muted) "取消静音" else "静音",
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable {
                        muted = !muted
                        val v = if (muted) 0f else 1f
                        mediaPlayer.value?.setVolume(v, v)
                    }.padding(4.dp)
                )
            }
        }
    }
}
