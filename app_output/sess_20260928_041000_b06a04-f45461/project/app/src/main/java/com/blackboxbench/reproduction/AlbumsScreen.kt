package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class GridMode { ALBUMS, ALBUM, FAVORITES, RECYCLE, ALL_MEDIA }

@Composable
fun GridScreen(
    state: GalleryState,
    mode: GridMode,
    albumName: String,
    onOpenAlbum: (String) -> Unit,
    onOpenViewer: (List<String>, Int) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onBack: () -> Unit,
    onOpenAllMedia: () -> Unit = {},
) {
    state.revision
    var selection by remember(mode, albumName) { mutableStateOf(emptySet<String>()) }
    var menuOpen by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }

    val showNames = state.showFilenames
    val title = when (mode) {
        GridMode.ALBUMS -> "搜索文件夹"
        GridMode.ALL_MEDIA -> "搜索文件"
        else -> "在 $albumName 中搜索"
    }

    val media: List<MediaItem> = when (mode) {
        GridMode.ALBUM -> state.visibleItems(albumName)
        GridMode.FAVORITES -> state.favoriteItems()
        GridMode.RECYCLE -> state.recycledItems()
        GridMode.ALL_MEDIA -> state.allMediaItems()
        GridMode.ALBUMS -> emptyList()
    }.filter { searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) }

    Column(modifier = Modifier.fillMaxSize().background(Palette.background)) {
        if (selection.isNotEmpty()) {
            TopBar(containerColor = Palette.accent) {
                IconButtonBox(onClick = { selection = emptySet() }) { IconBack(Color.White) }
                Box(Modifier.weight(1f).padding(start = 4.dp)) {
                    Text("${selection.size} / ${media.size}", color = Color.White, fontSize = 17.sp)
                }
                IconButtonBox(onClick = { dialog = "__delete" }) { IconTrash(Color.White) }
                Spacer(Modifier.width(2.dp))
                IconButtonBox(onClick = {}) { IconShare(Color.White) }
                Box {
                    IconButtonBox(onClick = { menuOpen = !menuOpen }) { IconDots(Color.White) }
                    SelectionMenu(
                        open = menuOpen,
                        recycle = mode == GridMode.RECYCLE,
                        onDismiss = { menuOpen = false },
                        onAction = { action -> menuOpen = false; dialog = action },
                    )
                }
            }
        } else {
            TopBar {
                if (mode == GridMode.ALBUM || mode == GridMode.FAVORITES || mode == GridMode.RECYCLE) {
                    IconButtonBox(onClick = { onBack() }) { IconBack() }
                }
                Box(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                    SearchPill(
                        hint = title,
                        query = searchQuery,
                        active = searching,
                        onActivate = { searching = true },
                        onQueryChange = { searchQuery = it },
                    )
                }
                IconButtonBox(onClick = {
                    state.showFilenames = !state.showFilenames
                    state.touch()
                }) { IconViewType() }
                if (mode == GridMode.ALBUMS) {
                    IconButtonBox(onClick = { onOpenAllMedia() }) { IconAllMedia() }
                } else if (mode == GridMode.ALL_MEDIA) {
                    IconButtonBox(onClick = { onBack() }) { IconAllMedia(Palette.accent) }
                } else {
                    IconButtonBox(onClick = { dialog = "排序方式" }) { IconSort() }
                }
                Box {
                    IconButtonBox(onClick = { menuOpen = !menuOpen }) { IconDots() }
                    GridOverflowMenu(
                        open = menuOpen, mode = mode, state = state,
                        onDismiss = { menuOpen = false },
                        onDialog = { menuOpen = false; dialog = it },
                        onOpenSettings = { menuOpen = false; onOpenSettings() },
                        onOpenAbout = { menuOpen = false; onOpenAbout() },
                    )
                }
            }
        }

        when (mode) {
            GridMode.ALBUMS -> AlbumsGrid(state, onOpenAlbum)
            GridMode.ALL_MEDIA -> GroupedMediaGrid(state, media, showNames, selection,
                onToggle = { id -> selection = if (id in selection) selection - id else selection + id },
                onLong = { id -> if (selection.isEmpty()) selection = setOf(id) },
                onOpen = { index -> onOpenViewer(media.map { it.id }, index) })
            else -> MediaGrid(state, media, showNames, selection,
                onToggle = { id -> selection = if (id in selection) selection - id else selection + id },
                onLong = { id -> if (selection.isEmpty()) selection = setOf(id) },
                onOpen = { index -> onOpenViewer(media.map { it.id }, index) })
        }
    }

    when (dialog) {
        null -> {}
        "排序方式" -> SortDialog(state, mode == GridMode.ALBUMS) { dialog = null }
        "过滤显示的文件" -> FilterDialog { dialog = null }
        "分组方式" -> GroupDialog(state) { dialog = null }
        "列数" -> ColumnsDialog(state, mode) { dialog = null }
        "幻灯片" -> SlideshowDialog { dialog = null }
        "新建文件夹" -> NewFolderDialog { dialog = null }
        "打开回收站" -> { dialog = null; onOpenAlbum(GalleryState.RECYCLE) }
        "__delete" -> {
            val victims = selection.mapNotNull { state.byId(it) }
            val first = victims.firstOrNull()
            if (first == null) { dialog = null } else {
                var skipAsk by remember { mutableStateOf(false) }
                var skipBin by remember { mutableStateOf(false) }
                AlertDialog(
                    onDismissRequest = { dialog = null },
                    title = {},
                    text = {
                        Column {
                            Text(
                                "确定要将 \"${first.name}\"" +
                                    (if (victims.size > 1) " 等 ${victims.size} 个项目" else "") +
                                    " (${formatSize(first.sizeBytes)}) 移至回收站吗？",
                                fontSize = 15.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.Checkbox(checked = skipAsk,
                                    onCheckedChange = { skipAsk = it })
                                Text("本次会话不再询问", fontSize = 14.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.Checkbox(checked = skipBin,
                                    onCheckedChange = { skipBin = it })
                                Text("跳过回收站，直接删除文件", fontSize = 14.sp)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            if (skipBin || !state.recycleBinEnabled) state.deletePermanently(selection)
                            else state.delete(selection)
                            state.persist(); selection = emptySet(); dialog = null
                        }) { Text("是") }
                    },
                    dismissButton = { TextButton(onClick = { dialog = null }) { Text("否") } },
                )
            }
        }
        "属性" -> {
            val it = selection.firstOrNull()?.let { id -> state.byId(id) }
            if (it != null) PropertiesDialog(it) { dialog = null } else dialog = null
        }
        "重命名" -> {
            val it = selection.firstOrNull()?.let { id -> state.byId(id) }
            if (it != null) RenameDialog(it, onConfirm = { n ->
                state.rename(it.id, n); state.persist(); dialog = null
            }, onClose = { dialog = null }) else dialog = null
        }
        "调整大小" -> {
            val it = selection.firstOrNull()?.let { id -> state.byId(id) }
            if (it != null) ResizeDialog(it) { dialog = null } else dialog = null
        }
        "复制到", "移动到" -> {
            val it = selection.firstOrNull()?.let { id -> state.byId(id) }
            CopyToDialog(state, it) { dialog = null }
        }
        "隐藏" -> {
            state.setHidden(selection, true); state.persist(); selection = emptySet(); dialog = null
        }
        "添加到收藏" -> {
            state.setFavorite(selection, true); state.persist(); selection = emptySet(); dialog = null
        }
        "全选" -> { selection = media.map { it.id }.toSet(); dialog = null }
        "恢复所选文件" -> {
            state.restore(selection); state.persist(); selection = emptySet(); dialog = null
        }
        "旋转" -> { selection = emptySet(); dialog = null }
        "编辑" -> { selection = emptySet(); dialog = null }
        "复制到剪贴板", "创建快捷方式", "打开方式", "设置为", "修复拍摄日期" -> dialog = null
        else -> dialog = null
    }
}

@Composable
private fun SearchPill(
    hint: String,
    query: String,
    active: Boolean,
    onActivate: () -> Unit,
    onQueryChange: (String) -> Unit,
) {
    val focus = remember { FocusRequester() }
    Row(
        modifier = Modifier
            .height(40.dp)
            .fillMaxWidth()
            .background(Palette.searchPill, RoundedCornerShape(20.dp))
            .clickable { onActivate() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconSearch(Modifier.size(20.dp), Palette.onSurfaceVariant)
        Spacer(Modifier.width(10.dp))
        if (active) {
            LaunchedEffect(active) { runCatching { focus.requestFocus() } }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 15.sp, color = Palette.onSurface),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        } else {
            Text(hint, fontSize = 15.sp, color = Palette.onSurfaceVariant)
        }
    }
}

@Composable
private fun AlbumsGrid(state: GalleryState, onOpenAlbum: (String) -> Unit) {
    state.revision
    val favCount = state.favoriteItems().size
    val recycleCount = state.recycledItems().size
    data class Entry(val label: String, val count: Int, val badge: String?, val cover: MediaItem?)

    val cells = mutableListOf<Entry>()
    if (favCount > 0) cells.add(Entry(GalleryState.FAVORITES, favCount, "star", state.favoriteItems().firstOrNull()))
    if (recycleCount > 0) cells.add(Entry(GalleryState.RECYCLE, recycleCount, "trash", state.recycledItems().firstOrNull()))
    for (a in state.albums) cells.add(Entry(a.name, state.albumCount(a.name), null, state.albumCover(a.name)))

    LazyVerticalGrid(
        columns = GridCells.Fixed(state.albumColumns.coerceIn(1, 15)),
        contentPadding = PaddingValues(1.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(cells.size) { idx ->
            val e = cells[idx]
            AlbumTile(
                item = e.cover, label = e.label, count = e.count, badge = e.badge,
                modifier = Modifier.aspectRatio(1f),
            ) { onOpenAlbum(e.label) }
        }
    }
}

@Composable
private fun MediaGrid(
    state: GalleryState,
    items: List<MediaItem>,
    showNames: Boolean,
    selection: Set<String>,
    onToggle: (String) -> Unit,
    onLong: (String) -> Unit,
    onOpen: (Int) -> Unit,
) {
    if (items.isEmpty()) {
        EmptyHint()
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(state.mediaColumns.coerceIn(1, 15)),
        contentPadding = PaddingValues(1.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(items.size) { idx ->
            val item = items[idx]
            MediaTile(
                item = item, showName = showNames, selected = selection.contains(item.id),
                modifier = Modifier.aspectRatio(1f),
                onClick = { if (selection.isEmpty()) onOpen(idx) else onToggle(item.id) },
                onLongClick = { onLong(item.id) },
            )
        }
    }
}

@Composable
private fun GroupedMediaGrid(
    state: GalleryState,
    items: List<MediaItem>,
    showNames: Boolean,
    selection: Set<String>,
    onToggle: (String) -> Unit,
    onLong: (String) -> Unit,
    onOpen: (Int) -> Unit,
) {
    if (items.isEmpty()) {
        EmptyHint()
        return
    }
    // group by month label derived from the stored date text
    val groups = mutableListOf<Pair<String, MutableList<Int>>>()
    items.forEachIndexed { idx, item ->
        val label = item.dateLabel.substringAfter(", ").ifBlank { "未知日期" }
        val key = item.dateLabel.substringBefore(",").let { d ->
            val parts = d.split(" ")
            if (parts.size >= 3) "${parts[2]}年${parts[1]}" else "未知日期"
        }
        val last = groups.lastOrNull()
        if (last == null || last.first != key) groups.add(key to mutableListOf(idx))
        else last.second.add(idx)
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(state.mediaColumns.coerceIn(1, 15)),
        contentPadding = PaddingValues(1.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        for ((header, idxs) in groups) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    Modifier.fillMaxWidth().padding(start = 12.dp, top = 14.dp, bottom = 8.dp)
                ) { Text(header, fontSize = 14.sp, color = Palette.onSurface) }
            }
            items(idxs.size) { k ->
                val idx = idxs[k]
                val item = items[idx]
                MediaTile(
                    item = item, showName = showNames, selected = selection.contains(item.id),
                    modifier = Modifier.aspectRatio(1f),
                    onClick = { if (selection.isEmpty()) onOpen(idx) else onToggle(item.id) },
                    onLongClick = { onLong(item.id) },
                )
            }
        }
    }
}

@Composable
private fun EmptyHint() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Text("未找到任何项目。", color = Palette.onSurfaceVariant, fontSize = 14.sp,
            modifier = Modifier.padding(top = 24.dp))
    }
}

@Composable
private fun SelectionMenu(
    open: Boolean,
    recycle: Boolean,
    onDismiss: () -> Unit,
    onAction: (String) -> Unit,
) {
    val items = if (recycle) listOf(
        "旋转", "属性", "复制到", "创建快捷方式", "打开方式", "设置为", "调整大小",
        "编辑", "恢复所选文件", "全选"
    ) else listOf(
        "旋转", "属性", "重命名", "隐藏", "复制到", "移动到", "创建快捷方式", "打开方式",
        "设置为", "调整大小", "编辑", "添加到收藏", "修复拍摄日期", "全选"
    )
    DropdownMenu(expanded = open, onDismissRequest = onDismiss) {
        for (i in items) DropdownMenuItem(text = { Text(i, fontSize = 14.sp) }, onClick = { onAction(i) })
    }
}

@Composable
private fun GridOverflowMenu(
    open: Boolean,
    mode: GridMode,
    state: GalleryState,
    onDismiss: () -> Unit,
    onDialog: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val items = if (mode == GridMode.ALBUMS) listOf(
        "排序方式", "过滤显示的文件", "更改视图类型", "隐藏项目", "临时显示已排除项目",
        "新建文件夹", "列数", "设置", "关于"
    ) else listOf(
        "过滤显示的文件", "更改视图类型", "隐藏项目", "打开回收站", "分组方式", "设置为默认文件夹",
        "新建文件夹", "列数", "幻灯片", "设置"
    )
    DropdownMenu(expanded = open, onDismissRequest = onDismiss) {
        for (i in items) {
            val label = if (i == "隐藏项目") {
                if (state.tempShowHidden) "停止显示隐藏项目" else "临时显示隐藏项目"
            } else i
            DropdownMenuItem(text = { Text(label, fontSize = 14.sp) }, onClick = {
                when (i) {
                    "设置" -> onOpenSettings()
                    "关于" -> onOpenAbout()
                    "更改视图类型" -> { state.showFilenames = !state.showFilenames; state.touch() }
                    "隐藏项目" -> { state.tempShowHidden = !state.tempShowHidden; state.touch() }
                    "临时显示已排除项目" -> { state.tempShowExcluded = !state.tempShowExcluded }
                    else -> onDialog(i)
                }
                onDismiss()
            })
        }
    }
}

// ---------------------------------------------------------------------------
// Dialogs
// ---------------------------------------------------------------------------

@Composable
fun SortDialog(state: GalleryState, forAlbums: Boolean, onClose: () -> Unit) {
    val keys = if (forAlbums) listOf("名称", "路径", "大小", "项数", "修改日期", "拍摄日期", "随机", "自定义")
    else listOf("名称", "路径", "大小", "修改日期", "拍摄日期", "随机")
    var key by remember { mutableStateOf(if (forAlbums) state.albumSortKey else state.mediaSortKey) }
    var asc by remember { mutableStateOf(if (forAlbums) state.albumSortAscending else state.mediaSortAscending) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("排序方式") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                keys.forEach { k ->
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { key = k }) {
                        RadioButton(selected = key == k, onClick = { key = k })
                        Text(k, fontSize = 15.sp)
                    }
                }
                RowDivider()
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { asc = true }) {
                    RadioButton(selected = asc, onClick = { asc = true }); Text("升序", fontSize = 15.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { asc = false }) {
                    RadioButton(selected = !asc, onClick = { asc = false }); Text("降序", fontSize = 15.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (forAlbums) { state.albumSortKey = key; state.albumSortAscending = asc }
                else { state.mediaSortKey = key; state.mediaSortAscending = asc }
                state.touch(); onClose()
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("取消") } },
    )
}

@Composable
fun FilterDialog(onClose: () -> Unit) {
    var images by remember { mutableStateOf(true) }
    var videos by remember { mutableStateOf(true) }
    var gif by remember { mutableStateOf(true) }
    var raw by remember { mutableStateOf(true) }
    var svg by remember { mutableStateOf(true) }
    var portrait by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("过滤显示的文件") },
        text = {
            Column {
                CheckRow("图片", images) { images = it }
                CheckRow("视频", videos) { videos = it }
                CheckRow("GIF", gif) { gif = it }
                CheckRow("RAW 图像", raw) { raw = it }
                CheckRow("SVG", svg) { svg = it }
                CheckRow("竖向", portrait) { portrait = it }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("确定") } },
        dismissButton = { TextButton(onClick = onClose) { Text("取消") } },
    )
}

@Composable
fun GroupDialog(state: GalleryState, onClose: () -> Unit) {
    var key by remember { mutableStateOf(state.groupBy) }
    var asc by remember { mutableStateOf(true) }
    var count by remember { mutableStateOf(false) }
    var onlyFolder by remember { mutableStateOf(false) }
    val options = listOf(
        "不分组文件", "最后修改时间 (按天)", "最后修改时间 (按月)", "拍摄日期 (按天)",
        "拍摄日期 (按月)", "文件类型", "扩展名"
    )
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("分组方式") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { o ->
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { key = o }) {
                        RadioButton(selected = key == o, onClick = { key = o })
                        Text(o, fontSize = 15.sp)
                    }
                }
                RowDivider()
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { asc = true }) {
                    RadioButton(selected = asc, onClick = { asc = true }); Text("升序", fontSize = 15.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { asc = false }) {
                    RadioButton(selected = !asc, onClick = { asc = false }); Text("降序", fontSize = 15.sp)
                }
                CheckRow("在栏目标题上显示文件数", count) { count = it }
                CheckRow("仅应用于此文件夹", onlyFolder) { onlyFolder = it }
                Text("请注意：分组和排序是两种独立的文件组织方式", fontSize = 12.sp,
                    color = Palette.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = { state.groupBy = key; state.touch(); onClose() }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("取消") } },
    )
}

@Composable
fun ColumnsDialog(state: GalleryState, mode: GridMode, onClose: () -> Unit) {
    val current = if (mode == GridMode.ALBUMS) state.albumColumns else state.mediaColumns
    AlertDialog(
        onDismissRequest = onClose,
        title = {},
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                for (n in 1..15) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable {
                            if (mode == GridMode.ALBUMS) state.albumColumns = n else state.mediaColumns = n
                            state.touch(); onClose()
                        }) {
                        RadioButton(selected = current == n, onClick = {
                            if (mode == GridMode.ALBUMS) state.albumColumns = n else state.mediaColumns = n
                            state.touch(); onClose()
                        })
                        Text("$n 列", fontSize = 15.sp)
                    }
                }
            }
        },
        confirmButton = {},
    )
}

@Composable
fun PropertiesDialog(item: MediaItem, onClose: () -> Unit) {
    val context = LocalContext.current
    val dims = remember(item.id) {
        if (item.isVideo) null
        else runCatching {
            val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            if (item.isFile) android.graphics.BitmapFactory.decodeFile(item.path, opts)
            else context.assets.open(item.path).use {
                android.graphics.BitmapFactory.decodeStream(it, null, opts)
            }
            if (opts.outWidth > 0) opts.outWidth to opts.outHeight else null
        }.getOrNull()
    }
    val path = if (item.isFile) item.path else item.albumPath + "/" + item.name
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("属性") },
        text = {
            Column {
                PropRow("名称", item.name)
                PropRow("路径", path)
                PropRow("大小", formatSize(item.sizeBytes))
                if (dims != null) PropRow(
                    "分辨率",
                    "${dims.first} x ${dims.second} (" +
                        "%.1f".format(dims.first * dims.second / 1_000_000.0) + "MP)"
                )
                PropRow("修改日期", item.dateLabel)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    TextButton(onClick = onClose) { Text("移除 EXIF 数据") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("确定") } },
    )
}

@Composable
private fun PropRow(label: String, value: String) {
    Column(Modifier.padding(vertical = 3.dp)) {
        Text(label, fontSize = 12.sp, color = Palette.accent)
        Text(value, fontSize = 15.sp, color = Palette.onSurface)
    }
}

@Composable
fun RenameDialog(item: MediaItem, onConfirm: (String) -> Unit, onClose: () -> Unit) {
    var title by remember { mutableStateOf(item.title) }
    var ext by remember { mutableStateOf(item.extension) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("重命名") },
        text = {
            Column {
                OutlinedTextField(value = title, onValueChange = { title = it },
                    label = { Text("标题") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = ext, onValueChange = { ext = it },
                    label = { Text("扩展名") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(if (ext.isBlank()) title else "$title.$ext") }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("取消") } },
    )
}

@Composable
fun ResizeDialog(item: MediaItem, onClose: () -> Unit) {
    var w by remember { mutableStateOf("1920") }
    var h by remember { mutableStateOf("1080") }
    var name by remember { mutableStateOf(item.title) }
    var ext by remember { mutableStateOf(item.extension) }
    AlertDialog(
        onDismissRequest = onClose,
        title = {},
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = w, onValueChange = { w = it }, label = { Text("宽度") },
                        singleLine = true, modifier = Modifier.weight(1f))
                    Text(" : ", fontSize = 18.sp)
                    OutlinedTextField(value = h, onValueChange = { h = it }, label = { Text("高度") },
                        singleLine = true, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = item.albumPath + "/", onValueChange = {},
                    label = { Text("文件夹") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("文件名") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = ext, onValueChange = { ext = it },
                    label = { Text("扩展名") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("确定") } },
        dismissButton = { TextButton(onClick = onClose) { Text("取消") } },
    )
}

@Composable
fun SlideshowDialog(onClose: () -> Unit) {
    var interval by remember { mutableStateOf("5") }
    var includeVideos by remember { mutableStateOf(false) }
    var includeGif by remember { mutableStateOf(false) }
    var random by remember { mutableStateOf(false) }
    var reverse by remember { mutableStateOf(false) }
    var loop by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onClose,
        title = {},
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("间隔", Modifier.weight(1f), fontSize = 15.sp)
                    OutlinedTextField(value = interval, onValueChange = { interval = it },
                        label = { Text("秒") }, singleLine = true, modifier = Modifier.width(120.dp))
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("动画", Modifier.weight(1f), fontSize = 15.sp)
                    Text("滑动", fontSize = 15.sp, modifier = Modifier.padding(end = 12.dp))
                }
                CheckRow("包含视频", includeVideos) { includeVideos = it }
                CheckRow("包含GIF", includeGif) { includeGif = it }
                CheckRow("随机顺序", random) { random = it }
                CheckRow("倒序播放", reverse) { reverse = it }
                CheckRow("循环播放幻灯片", loop) { loop = it }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("确定") } },
        dismissButton = { TextButton(onClick = onClose) { Text("取消") } },
    )
}

@Composable
fun NewFolderDialog(onClose: () -> Unit) {
    var title by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("新建文件夹") },
        text = {
            Column {
                OutlinedTextField(value = "内部存储空间/DCIM/", onValueChange = {},
                    label = { Text("文件夹") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = title, onValueChange = { title = it },
                    label = { Text("标题") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("确定") } },
        dismissButton = { TextButton(onClick = onClose) { Text("取消") } },
    )
}

@Composable
fun CopyToDialog(state: GalleryState, item: MediaItem?, onClose: () -> Unit) {
    var query by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("选择目标") },
        text = {
            Column {
                SearchPill(hint = "搜索文件夹", query = query, active = true,
                    onActivate = {}, onQueryChange = { query = it })
                Spacer(Modifier.height(10.dp))
                Column(Modifier.height(220.dp).verticalScroll(rememberScrollState())) {
                    for (a in state.albums) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { onClose() }
                                .padding(vertical = 8.dp)) {
                            Box(Modifier.size(44.dp).background(Color(0xFFB0A8C0), RoundedCornerShape(4.dp)))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(a.name, fontSize = 15.sp)
                                Text("${state.albumCount(a.name)}", fontSize = 12.sp,
                                    color = Palette.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("确定") } },
        dismissButton = {
            Row {
                TextButton(onClick = onClose) { Text("其他文件夹") }
                TextButton(onClick = onClose) { Text("取消") }
            }
        },
    )
}
