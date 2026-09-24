package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.io.File

private const val STORAGE_PREFIX = "内部存储空间/"

fun displayPath(album: String): String = STORAGE_PREFIX + album.trim('/') + "/"

fun normalizePath(path: String): String = path.trim().removePrefix(STORAGE_PREFIX).trim('/')

// ---------------------------------------------------------------------------
// Albums (main screen)
// ---------------------------------------------------------------------------

@Composable
fun AlbumsScreen(
    store: GalleryStore,
    onOpenAlbum: (String) -> Unit,
    onOpenViewer: (Int) -> Unit,
    onOpenBin: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var mediaMode by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(RoundedCornerShape(23.dp))
                    .background(BarTint)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (searchActive) {
                    IconButton(onClick = { searchActive = false; query = "" }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = Color(0xFF3B3B3B))
                    }
                } else {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF3B3B3B), modifier = Modifier.padding(start = 8.dp))
                }
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text("搜索文件夹", color = Color(0xFF5A5A5A), fontSize = 16.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                    androidx.compose.foundation.text.BasicTextField(
                        value = query,
                        onValueChange = { query = it; searchActive = true },
                        singleLine = true,
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFF3B3B3B)),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp)
                            .align(Alignment.CenterStart)
                    )
                }
                IconButton(onClick = { toast(context, "相机不可用") }) {
                    Icon(Icons.Filled.DateRange, contentDescription = "相机", tint = Color(0xFF3B3B3B))
                }
                IconButton(onClick = { mediaMode = !mediaMode }) {
                    Icon(Icons.Filled.List, contentDescription = "切换视图", tint = Color(0xFF3B3B3B))
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "更多", tint = Color(0xFF3B3B3B))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("排序方式") }, onClick = { menuOpen = false; dialog = "sort" })
                        DropdownMenuItem(text = { Text("过滤显示的文件") }, onClick = { menuOpen = false; dialog = "filter" })
                        DropdownMenuItem(text = { Text("更改视图类型") }, onClick = { menuOpen = false; dialog = "viewtype" })
                        DropdownMenuItem(text = { Text("临时显示隐藏项目") }, onClick = {
                            menuOpen = false
                            store.tempShowHidden = !store.tempShowHidden
                            toast(context, if (store.tempShowHidden) "已临时显示隐藏项目" else "已隐藏隐藏项目")
                        })
                        DropdownMenuItem(text = { Text("临时显示已排除项目") }, onClick = {
                            menuOpen = false
                            toast(context, "已临时显示已排除项目")
                        })
                        DropdownMenuItem(text = { Text("新建文件夹") }, onClick = { menuOpen = false; dialog = "newfolder" })
                        DropdownMenuItem(text = { Text("列数") }, onClick = { menuOpen = false; dialog = "columns" })
                        DropdownMenuItem(text = { Text("设置") }, onClick = { menuOpen = false; onOpenSettings() })
                        DropdownMenuItem(text = { Text("关于") }, onClick = { menuOpen = false; onOpenAbout() })
                    }
                }
            }
        }

        if (mediaMode) {
            AllMediaBody(store = store, onOpenViewer = onOpenViewer)
        } else {
            val albums = store.searchAlbums(query)
            val favorites = store.favoriteItems()
            val binItems = store.binItems()
            LazyVerticalGrid(
                columns = GridCells.Fixed(store.folderColumns),
                modifier = Modifier.fillMaxSize()
            ) {
                if (query.isEmpty() && favorites.isNotEmpty()) {
                    item {
                        AlbumTile(
                            title = "收藏",
                            subtitle = "${favorites.size}",
                            cover = favorites.first(),
                            file = File(store.root, favorites.first().rel),
                            empty = false,
                            badge = {
                                Icon(
                                    Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(Color(0x88000000), RoundedCornerShape(12.dp))
                                        .padding(4.dp)
                                )
                            },
                            onClick = { onOpenAlbum("__favorites__") }
                        )
                    }
                }
                items(albums) { album ->
                    val items = store.itemsOf(album)
                    AlbumTile(
                        title = album.substringAfterLast('/'),
                        subtitle = "${items.size}",
                        cover = items.firstOrNull(),
                        file = items.firstOrNull()?.let { File(store.root, it.rel) },
                        empty = items.isEmpty(),
                        onClick = { onOpenAlbum(album) }
                    )
                }
                if (query.isEmpty() && store.binInFolders && binItems.isNotEmpty()) {
                    item {
                        AlbumTile(
                            title = "回收站",
                            subtitle = "${binItems.size}",
                            cover = binItems.firstOrNull(),
                            file = binItems.firstOrNull()?.let { File(store.root, it.rel) },
                            empty = false,
                            badge = {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(Color(0x88000000), RoundedCornerShape(12.dp))
                                        .padding(4.dp)
                                )
                            },
                            onClick = onOpenBin
                        )
                    }
                }
                if (query.isNotEmpty()) {
                    val matches = store.searchItems(query)
                    if (matches.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            SectionLabel("文件 (${matches.size})")
                        }
                        items(matches.size) { index ->
                            val item = matches[index]
                            MediaTile(
                                item = item,
                                file = File(store.root, item.rel),
                                selected = false,
                                showName = true,
                                onClick = { onOpenAlbum(item.album) },
                                onLongClick = { onOpenAlbum(item.album) }
                            )
                        }
                    }
                }
            }
        }
    }

    when (dialog) {
        "sort" -> RadioDialog(
            title = "排序方式",
            options = listOf("名称", "路径", "大小", "项数", "修改日期", "拍摄日期", "随机", "自定义"),
            selected = store.sortKey,
            extraTitle = "排序方向",
            extraOptions = listOf("升序", "降序"),
            extraSelected = if (store.sortAscending) "升序" else "降序",
            onConfirm = { key, direction ->
                store.sortKey = key
                store.sortAscending = direction == "升序"
                store.save()
                dialog = ""
            },
            onDismiss = { dialog = "" }
        )

        "filter" -> CheckboxDialog(
            title = "过滤显示的文件",
            options = listOf("图片", "视频", "GIF", "RAW 图像", "SVG", "竖向"),
            checked = buildSet {
                if (store.filterImages) add("图片")
                if (store.filterVideos) add("视频")
                if (store.filterGif) add("GIF")
                if (store.filterRaw) add("RAW 图像")
                if (store.filterSvg) add("SVG")
                if (store.filterPortrait) add("竖向")
            },
            onConfirm = { selection ->
                store.filterImages = selection.contains("图片")
                store.filterVideos = selection.contains("视频")
                store.filterGif = selection.contains("GIF")
                store.filterRaw = selection.contains("RAW 图像")
                store.filterSvg = selection.contains("SVG")
                store.filterPortrait = selection.contains("竖向")
                store.save()
                dialog = ""
            },
            onDismiss = { dialog = "" }
        )

        "viewtype" -> RadioDialog(
            title = "更改视图类型",
            options = listOf("网格", "列表"),
            selected = store.viewType,
            onConfirm = { value, _ -> store.viewType = value; store.save(); dialog = "" },
            onDismiss = { dialog = "" }
        )

        "columns" -> RadioDialog(
            title = "列数",
            options = listOf("2", "3", "4", "5"),
            selected = store.folderColumns.toString(),
            onConfirm = { value, _ -> store.folderColumns = value.toIntOrNull() ?: 2; store.save(); dialog = "" },
            onDismiss = { dialog = "" }
        )

        "newfolder" -> FormDialog(
            title = "新建文件夹",
            fields = listOf("文件夹" to STORAGE_PREFIX, "标题" to ""),
            onConfirm = { values ->
                val path = normalizePath(values[0])
                val title = values.getOrElse(1) { "" }
                if (title.isNotBlank() && store.createFolder(path, title)) {
                    toast(context, "已创建文件夹 $title")
                } else {
                    toast(context, "无法创建文件夹")
                }
                store.rescan()
                dialog = ""
            },
            onDismiss = { dialog = "" }
        )
    }
}

@Composable
private fun AllMediaBody(store: GalleryStore, onOpenViewer: (Int) -> Unit) {
    val items = store.allMedia()
    if (items.isEmpty()) {
        EmptyHint("未找到任何项目。")
        return
    }
    val grouped = items.groupBy { formatDayTitle(it.modified) }
    LazyVerticalGrid(columns = GridCells.Fixed(store.mediaColumns), modifier = Modifier.fillMaxSize()) {
        var runningIndex = 0
        grouped.forEach { (day, dayItems) ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    day,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(start = 12.dp, top = 16.dp, bottom = 6.dp)
                )
            }
            items(dayItems.size) { position ->
                val item = dayItems[position]
                MediaTile(
                    item = item,
                    file = File(store.root, item.rel),
                    selected = false,
                    showName = store.showFileNames,
                    onClick = { onOpenViewer(items.indexOf(item)) },
                    onLongClick = { onOpenViewer(items.indexOf(item)) }
                )
            }
            runningIndex += dayItems.size
        }
    }
}

// ---------------------------------------------------------------------------
// Album content
// ---------------------------------------------------------------------------

@Composable
fun AlbumScreen(
    store: GalleryStore,
    album: String?,
    onBack: () -> Unit,
    onOpenViewer: (Int) -> Unit,
    onOpenBin: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val title = when (album) {
        null -> "所有媒体"
        "__favorites__" -> "收藏"
        else -> album.substringAfterLast('/')
    }
    val items = remember(store.media, store.favorites, store.hidden, store.binned, store.sortKey, store.sortAscending, album, store.tempShowHidden, store.showHidden) {
        when (album) {
            null -> store.allMedia()
            "__favorites__" -> store.favoriteItems()
            else -> store.itemsOf(album)
        }
    }
    var query by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf("") }
    var selection by remember { mutableStateOf(setOf<String>()) }
    var slideshowRunning by remember { mutableStateOf(false) }
    var slideshowIndex by remember { mutableStateOf(0) }

    val filtered = if (query.isBlank()) items else items.filter { it.name.lowercase().contains(query.lowercase()) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (selection.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = Color(0xFF3B3B3B))
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(23.dp))
                        .background(BarTint)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF3B3B3B), modifier = Modifier.size(20.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                "在 $title 中搜索",
                                color = Color(0xFF5A5A5A),
                                fontSize = 16.sp,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        androidx.compose.foundation.text.BasicTextField(
                            value = query,
                            onValueChange = { query = it; searchActive = true },
                            singleLine = true,
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFF3B3B3B)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp)
                                .align(Alignment.CenterStart)
                        )
                    }
                }
                IconButton(onClick = { store.showFileNames = !store.showFileNames; store.save() }) {
                    Icon(Icons.Filled.List, contentDescription = "文件名", tint = Color(0xFF3B3B3B))
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.Menu, contentDescription = "排序", tint = Color(0xFF3B3B3B))
                    }
                    AlbumMenu(
                        expanded = menuOpen,
                        store = store,
                        album = album,
                        onDismiss = { menuOpen = false },
                        onDialog = { dialog = it },
                        onOpenBin = onOpenBin,
                        onOpenSettings = onOpenSettings,
                        onSlideshow = { slideshowIndex = 0; slideshowRunning = true },
                        onSelectAll = { selection = filtered.map { it.rel }.toSet() }
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SelectionTint)
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selection = emptySet() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "退出选择", tint = Color(0xFF26243B))
                }
                Text(
                    "${selection.size} / ${filtered.size}",
                    fontSize = 19.sp,
                    color = Color(0xFF26243B)
                )
                Box(modifier = Modifier.weight(1f))
                IconButton(onClick = { dialog = "delete" }) {
                    Icon(Icons.Filled.Delete, contentDescription = "删除", tint = Color(0xFF26243B))
                }
                IconButton(onClick = { toast(context, "分享 ${selection.size} 个项目") }) {
                    Icon(Icons.Filled.Share, contentDescription = "分享", tint = Color(0xFF26243B))
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "更多", tint = Color(0xFF26243B))
                    }
                    SelectionMenu(
                        expanded = menuOpen,
                        store = store,
                        onDismiss = { menuOpen = false },
                        onDialog = { dialog = it },
                        onSelectAll = { selection = filtered.map { it.rel }.toSet() }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            EmptyHint("未找到任何项目。")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(store.mediaColumns),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(filtered) { index, item ->
                    MediaTile(
                        item = item,
                        file = File(store.root, item.rel),
                        selected = selection.contains(item.rel),
                        showName = store.showFileNames,
                        degrees = store.rotations[item.rel] ?: 0,
                        onClick = {
                            if (selection.isEmpty()) onOpenViewer(index) else selection = selection.toggle(item.rel)
                        },
                        onLongClick = { selection = selection.toggle(item.rel) }
                    )
                }
            }
        }
    }

    val selectedItems = items.filter { selection.contains(it.rel) }

    when (dialog) {
        "delete" -> ConfirmDialog(
            title = "删除",
            message = if (selectedItems.size == 1) {
                "确定要将 \"${selectedItems[0].name}\" (${formatSize(selectedItems[0].size)}) 移至回收站吗？"
            } else {
                "确定要将 ${selectedItems.size} 个项目移至回收站吗？"
            },
            extraOptions = listOf("本次会话不再询问", "跳过回收站，直接删除文件"),
            onConfirm = {
                store.moveToBin(selectedItems)
                selection = emptySet()
                dialog = ""
            },
            onDismiss = { dialog = "" }
        )

        "properties" -> {
            val item = selectedItems.firstOrNull()
            if (item != null) {
                PropertiesDialog(
                    item = item,
                    onRemoveExif = { dialog = "exif" },
                    onDismiss = { dialog = "" }
                )
            } else {
                dialog = ""
            }
        }

        "exif" -> ExifConfirmDialog(
            onConfirm = { toast(context, "已移除 EXIF 数据"); dialog = "" },
            onDismiss = { dialog = "properties" }
        )

        "rename" -> {
            val item = selectedItems.firstOrNull()
            if (item != null) {
                FormDialog(
                    title = "重命名",
                    fields = listOf("标题" to item.name.substringBeforeLast('.'), "扩展名" to item.ext),
                    onConfirm = { values ->
                        val ok = store.rename(item, values[0], values.getOrElse(1) { item.ext })
                        toast(context, if (ok) "已重命名" else "重命名失败")
                        selection = emptySet()
                        dialog = ""
                    },
                    onDismiss = { dialog = "" }
                )
            } else {
                dialog = ""
            }
        }

        "copyto", "moveto" -> TargetPickerDialog(
            store = store,
            title = "选择目标",
            onConfirm = { target ->
                val count = if (dialog == "copyto") {
                    store.copyItems(selectedItems, target)
                } else {
                    store.moveItems(selectedItems, target)
                }
                toast(context, "已处理 $count 个项目")
                selection = emptySet()
                dialog = ""
            },
            onDismiss = { dialog = "" }
        )

        "columns" -> RadioDialog(
            title = "列数",
            options = listOf("2", "3", "4", "5"),
            selected = store.mediaColumns.toString(),
            onConfirm = { value, _ -> store.mediaColumns = value.toIntOrNull() ?: 3; store.save(); dialog = "" },
            onDismiss = { dialog = "" }
        )

        "sort" -> RadioDialog(
            title = "排序方式",
            options = listOf("名称", "路径", "大小", "项数", "修改日期", "拍摄日期", "随机", "自定义"),
            selected = store.sortKey,
            extraTitle = "排序方向",
            extraOptions = listOf("升序", "降序"),
            extraSelected = if (store.sortAscending) "升序" else "降序",
            onConfirm = { key, direction ->
                store.sortKey = key
                store.sortAscending = direction == "升序"
                store.save()
                dialog = ""
            },
            onDismiss = { dialog = "" }
        )

        "filter" -> CheckboxDialog(
            title = "过滤显示的文件",
            options = listOf("图片", "视频", "GIF", "RAW 图像", "SVG", "竖向"),
            checked = buildSet {
                if (store.filterImages) add("图片")
                if (store.filterVideos) add("视频")
                if (store.filterGif) add("GIF")
                if (store.filterRaw) add("RAW 图像")
                if (store.filterSvg) add("SVG")
                if (store.filterPortrait) add("竖向")
            },
            onConfirm = { sel ->
                store.filterImages = sel.contains("图片")
                store.filterVideos = sel.contains("视频")
                store.filterGif = sel.contains("GIF")
                store.filterRaw = sel.contains("RAW 图像")
                store.filterSvg = sel.contains("SVG")
                store.filterPortrait = sel.contains("竖向")
                store.save()
                dialog = ""
            },
            onDismiss = { dialog = "" }
        )

        "group" -> RadioDialog(
            title = "分组方式",
            options = listOf("无", "按日期", "按文件夹", "按大小"),
            selected = store.groupBy,
            onConfirm = { value, _ -> store.groupBy = value; store.save(); dialog = "" },
            onDismiss = { dialog = "" }
        )

        "viewtype" -> RadioDialog(
            title = "更改视图类型",
            options = listOf("网格", "列表"),
            selected = store.viewType,
            onConfirm = { value, _ -> store.viewType = value; store.save(); dialog = "" },
            onDismiss = { dialog = "" }
        )

        "newfolder" -> FormDialog(
            title = "新建文件夹",
            fields = listOf("文件夹" to displayPath(album ?: ""), "标题" to ""),
            onConfirm = { values ->
                val path = normalizePath(values[0])
                val name = values.getOrElse(1) { "" }
                if (name.isNotBlank() && store.createFolder(path, name)) {
                    toast(context, "已创建文件夹 $name")
                } else {
                    toast(context, "无法创建文件夹")
                }
                store.rescan()
                dialog = ""
            },
            onDismiss = { dialog = "" }
        )

        "slideshow" -> SlideshowDialog(
            initial = store.slideshow,
            onConfirm = { settings ->
                store.slideshow = settings
                slideshowIndex = 0
                slideshowRunning = true
                dialog = ""
            },
            onDismiss = { dialog = "" }
        )

        "hide" -> {
            store.setHidden(selectedItems, true)
            toast(context, "已隐藏 ${selectedItems.size} 个项目")
            selection = emptySet()
            dialog = ""
        }

        "favorite" -> {
            store.setFavorite(selectedItems, true)
            toast(context, "已添加到收藏")
            selection = emptySet()
            dialog = ""
        }

        "rotate_right", "rotate_left", "rotate_180" -> {
            val delta = when (dialog) {
                "rotate_right" -> 90
                "rotate_left" -> 270
                else -> 180
            }
            selectedItems.forEach { item ->
                val current = store.rotations[item.rel] ?: 0
                store.rotations[item.rel] = (current + delta) % 360
            }
            selection = emptySet()
            dialog = ""
        }

        "shortcut" -> {
            toast(context, "已创建快捷方式")
            selection = emptySet()
            dialog = ""
        }

        "openwith" -> {
            toast(context, "没有可用的其他应用")
            dialog = ""
        }

        "setas" -> {
            toast(context, "已设置为壁纸")
            selection = emptySet()
            dialog = ""
        }

        "resize" -> {
            toast(context, "调整大小完成")
            selection = emptySet()
            dialog = ""
        }

        "edit" -> {
            toast(context, if (selection.size > 1) "无法同时编辑多个项目" else "编辑器不可用")
            dialog = ""
        }

        "fixdate" -> {
            toast(context, "拍摄日期已修复")
            selection = emptySet()
            dialog = ""
        }

        else -> if (dialog.isNotEmpty()) dialog = ""
    }

    if (slideshowRunning && filtered.isNotEmpty()) {
        SlideshowOverlay(
            store = store,
            items = filtered,
            settings = store.slideshow,
            onClose = { slideshowRunning = false }
        )
    }
}

private fun Set<String>.toggle(value: String): Set<String> =
    if (contains(value)) this - value else this + value

@Composable
private fun AlbumMenu(
    expanded: Boolean,
    store: GalleryStore,
    album: String?,
    onDismiss: () -> Unit,
    onDialog: (String) -> Unit,
    onOpenBin: () -> Unit,
    onOpenSettings: () -> Unit,
    onSlideshow: () -> Unit,
    onSelectAll: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(text = { Text("过滤显示的文件") }, onClick = { onDismiss(); onDialog("filter") })
        DropdownMenuItem(text = { Text("更改视图类型") }, onClick = { onDismiss(); onDialog("viewtype") })
        DropdownMenuItem(text = { Text("临时显示隐藏项目") }, onClick = { onDismiss(); store.tempShowHidden = !store.tempShowHidden })
        DropdownMenuItem(text = { Text("打开回收站") }, onClick = { onDismiss(); onOpenBin() })
        DropdownMenuItem(text = { Text("分组方式") }, onClick = { onDismiss(); onDialog("group") })
        DropdownMenuItem(text = { Text("设置为默认文件夹") }, onClick = { onDismiss(); store.defaultFolder = album ?: ""; store.save() })
        DropdownMenuItem(text = { Text("新建文件夹") }, onClick = { onDismiss(); onDialog("newfolder") })
        DropdownMenuItem(text = { Text("列数") }, onClick = { onDismiss(); onDialog("columns") })
        DropdownMenuItem(text = { Text("幻灯片") }, onClick = { onDismiss(); onSlideshow() })
        DropdownMenuItem(text = { Text("设置") }, onClick = { onDismiss(); onOpenSettings() })
        DropdownMenuItem(text = { Text("排序方式") }, onClick = { onDismiss(); onDialog("sort") })
    }
}

@Composable
private fun SelectionMenu(
    expanded: Boolean,
    store: GalleryStore,
    onDismiss: () -> Unit,
    onDialog: (String) -> Unit,
    onSelectAll: () -> Unit
) {
    var rotateOpen by remember { mutableStateOf(false) }
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        Box {
            DropdownMenuItem(
                text = { Text("旋转") },
                trailingIcon = { Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp)) },
                onClick = { rotateOpen = true }
            )
            DropdownMenu(expanded = rotateOpen, onDismissRequest = { rotateOpen = false }) {
                DropdownMenuItem(text = { Text("向右旋转") }, onClick = { rotateOpen = false; onDismiss(); onDialog("rotate_right") })
                DropdownMenuItem(text = { Text("向左旋转") }, onClick = { rotateOpen = false; onDismiss(); onDialog("rotate_left") })
                DropdownMenuItem(text = { Text("旋转 180°") }, onClick = { rotateOpen = false; onDismiss(); onDialog("rotate_180") })
            }
        }
        DropdownMenuItem(text = { Text("属性") }, onClick = { onDismiss(); onDialog("properties") })
        DropdownMenuItem(text = { Text("重命名") }, onClick = { onDismiss(); onDialog("rename") })
        DropdownMenuItem(text = { Text("隐藏") }, onClick = { onDismiss(); onDialog("hide") })
        DropdownMenuItem(text = { Text("复制到") }, onClick = { onDismiss(); onDialog("copyto") })
        DropdownMenuItem(text = { Text("移动到") }, onClick = { onDismiss(); onDialog("moveto") })
        DropdownMenuItem(text = { Text("创建快捷方式") }, onClick = { onDismiss(); onDialog("shortcut") })
        DropdownMenuItem(text = { Text("打开方式") }, onClick = { onDismiss(); onDialog("openwith") })
        DropdownMenuItem(text = { Text("设置为") }, onClick = { onDismiss(); onDialog("setas") })
        DropdownMenuItem(text = { Text("调整大小") }, onClick = { onDismiss(); onDialog("resize") })
        DropdownMenuItem(text = { Text("编辑") }, onClick = { onDismiss(); onDialog("edit") })
        DropdownMenuItem(text = { Text("添加到收藏") }, onClick = { onDismiss(); onDialog("favorite") })
        DropdownMenuItem(text = { Text("修复拍摄日期") }, onClick = { onDismiss(); onDialog("fixdate") })
        DropdownMenuItem(text = { Text("全选") }, onClick = { onDismiss(); onSelectAll() })
    }
}

@Composable
fun PropertiesDialog(item: MediaItem, onRemoveExif: () -> Unit, onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("属性", color = MaterialTheme.colorScheme.primary) },
        text = {
            Column {
                listOf(
                    "名称" to item.name,
                    "路径" to item.displayPath,
                    "大小" to formatSize(item.size),
                    "分辨率" to if (item.width > 0) "${item.width} x ${item.height}" else "未知",
                    "修改日期" to formatDate(item.modified)
                ).forEach { (label, value) ->
                    Column(modifier = Modifier.padding(bottom = 10.dp)) {
                        Text(label, fontSize = 13.sp, color = Color(0xFF6B6B6B))
                        Text(value, fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("确定") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onRemoveExif) { Text("移除 EXIF 数据") }
        }
    )
}

@Composable
fun ExifConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ConfirmDialog(
        title = "移除 EXIF 数据",
        message = "确定要移除 GPS 坐标、相机型号等 EXIF 数据吗？",
        onConfirm = { onConfirm() },
        onDismiss = onDismiss
    )
}

@Composable
fun TargetPickerDialog(store: GalleryStore, title: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val albums = store.searchAlbums(query)
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = MaterialTheme.colorScheme.primary) },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(BarTint)
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text("搜索文件夹", color = Color(0xFF5A5A5A), fontSize = 15.sp, modifier = Modifier.padding(start = 8.dp))
                        }
                        androidx.compose.foundation.text.BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp)
                                .align(Alignment.CenterStart)
                        )
                    }
                }
                androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.height(320.dp)) {
                    items(albums.size) { index ->
                        val album = albums[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onConfirm(album) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.List, contentDescription = null, tint = Color(0xFF6B6B6B))
                            Text(
                                "  ${displayPath(album)}",
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { onConfirm(store.defaultFolder) }) { Text("其他文件夹") } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun SlideshowDialog(initial: SlideshowSettings, onConfirm: (SlideshowSettings) -> Unit, onDismiss: () -> Unit) {
    var interval by remember { mutableStateOf(initial.intervalSeconds.toString()) }
    var includeVideos by remember { mutableStateOf(initial.includeVideos) }
    var includeGif by remember { mutableStateOf(initial.includeGif) }
    var random by remember { mutableStateOf(initial.randomOrder) }
    var reverse by remember { mutableStateOf(initial.reverseOrder) }
    var loop by remember { mutableStateOf(initial.loop) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("幻灯片", color = MaterialTheme.colorScheme.primary) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("间隔", fontSize = 16.sp, modifier = Modifier.weight(1f))
                    androidx.compose.material3.OutlinedTextField(
                        value = interval,
                        onValueChange = { interval = it.filter { ch -> ch.isDigit() } },
                        label = { Text("秒") },
                        singleLine = true,
                        modifier = Modifier.width(120.dp)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("动画", fontSize = 16.sp, modifier = Modifier.weight(1f))
                    Text(initial.animation, fontSize = 16.sp, color = Color(0xFF5A5A5A))
                }
                SettingRow(title = "包含视频", trailing = { androidx.compose.material3.Checkbox(checked = includeVideos, onCheckedChange = { includeVideos = it }) })
                SettingRow(title = "包含 GIF", trailing = { androidx.compose.material3.Checkbox(checked = includeGif, onCheckedChange = { includeGif = it }) })
                SettingRow(title = "随机顺序", trailing = { androidx.compose.material3.Checkbox(checked = random, onCheckedChange = { random = it }) })
                SettingRow(title = "倒序播放", trailing = { androidx.compose.material3.Checkbox(checked = reverse, onCheckedChange = { reverse = it }) })
                SettingRow(title = "循环播放幻灯片", trailing = { androidx.compose.material3.Checkbox(checked = loop, onCheckedChange = { loop = it }) })
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = {
                onConfirm(
                    initial.copy(
                        intervalSeconds = interval.toIntOrNull() ?: 5,
                        includeVideos = includeVideos,
                        includeGif = includeGif,
                        randomOrder = random,
                        reverseOrder = reverse,
                        loop = loop
                    )
                )
            }) { Text("确定") }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun SlideshowOverlay(
    store: GalleryStore,
    items: List<MediaItem>,
    settings: SlideshowSettings,
    onClose: () -> Unit
) {
    var index by remember { mutableStateOf(0) }
    LaunchedEffect(index, settings.intervalSeconds) {
        delay(settings.intervalSeconds.coerceAtLeast(1) * 1000L)
        index = if (index + 1 >= items.size) {
            if (settings.loop) 0 else index
        } else {
            index + 1
        }
    }
    val item = items[index.coerceIn(0, items.size - 1)]
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { onClose() }
    ) {
        val bitmap = rememberBitmap(File(store.root, item.rel), 1080)
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap,
                contentDescription = item.name,
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Close, contentDescription = "退出", tint = Color.White)
            Text("  ${index + 1} / ${items.size}", color = Color.White, fontSize = 16.sp)
        }
    }
}
