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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

private const val FAVORITES_PATH = "favorites"

@Composable
fun AlbumsScreen(c: AppController) {
    c.settings.revision.value
    var query by remember { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    var selectionMenu by remember { mutableStateOf(false) }
    var selection by remember { mutableStateOf(setOf<String>()) }
    var dialog by remember { mutableStateOf<String?>(null) }
    var submenu by remember { mutableStateOf<String?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }

    val selectionMode = selection.isNotEmpty()
    val favoriteItems = c.favorites()
    val albums = c.albums().filter { query.isBlank() || it.name.contains(query, true) }
    val hiddenTemporarily = c.temporaryHiddenReveal

    Column(modifier = Modifier.fillMaxSize().background(GalleryBackground)) {
        if (selectionMode) {
            SelectionBar(
                count = selection.size,
                onBack = { selection = emptySet() },
                onDelete = { dialog = "deleteAlbums" },
                onShare = { toast = "已分享 ${selection.size} 个文件夹" },
                onInfo = { dialog = "albumProperties" },
                onMenu = { selectionMenu = true },
                menu = {
                    MenuEntries(
                        entries = listOf(
                            "通过拖动重新排列文件夹" to { toast = "长按拖动可重新排列文件夹" },
                            "重命名" to { dialog = "renameAlbum" },
                            "隐藏文件夹" to {
                                selection.forEach { path ->
                                    c.repo.albumItems(path, true).forEach { c.repo.setHidden(it, true) }
                                }
                                selection = emptySet(); c.toast("已隐藏文件夹")
                            },
                            "复制到" to { dialog = "copyAlbum" },
                            "移动到" to { dialog = "moveAlbum" },
                            "创建快捷方式" to { dialog = "shortcut" },
                            "排除" to {
                                val current = c.settings.excludeFolders.get()
                                val added = (current.split(",").filter { it.isNotBlank() } + selection).distinct().joinToString(",")
                                c.settings.excludeFolders.set(added)
                                selection = emptySet(); c.toast("已排除所选文件夹")
                            },
                            "锁定文件夹" to {
                                c.settings.protectHidden.set(true)
                                selection = emptySet(); c.toast("已锁定文件夹（需密码查看隐藏项目）")
                            },
                            "更换封面图片" to { submenu = "cover" },
                            "全选" to { selection = albums.map { it.path }.toSet() + FAVORITES_PATH }
                        ),
                        expanded = selectionMenu,
                        onDismiss = { selectionMenu = false },
                        hasArrow = setOf("更换封面图片")
                    )
                }
            )
        } else {
            SearchBarHeader(
                text = query, onTextChange = { query = it },
                onCamera = { toast = "相机不可用" },
                onToggleTimeline = { c.push(Screen.Timeline) },
                onMenu = { menuOpen = true },
                menu = {
                    MenuEntries(
                        entries = listOf(
                            "排序方式" to { dialog = "sort" },
                            "过滤显示的文件" to { dialog = "filter" },
                            "更改视图类型" to { dialog = "viewType" },
                            "临时显示隐藏项目" to {
                                c.temporaryHiddenReveal = !hiddenTemporarily
                                toast = if (hiddenTemporarily) "已隐藏隐藏项目" else "已临时显示隐藏项目"
                            },
                            "临时显示已排除项目" to { toast = "已临时显示已排除项目" },
                            "新建文件夹" to { dialog = "newFolder" },
                            "列数" to { dialog = "columns" },
                            "设置" to { c.push(Screen.Settings) },
                            "关于" to { c.push(Screen.About) }
                        ),
                        expanded = menuOpen, onDismiss = { menuOpen = false }
                    )
                }
            )
        }

        val useList = c.settings.viewType.get() == "列表"
        if (useList) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (favoriteItems.isNotEmpty() || selectionMode) {
                    item {
                        FolderListRow(
                            album = Album(FAVORITES_PATH, "收藏"),
                            cover = favoriteItems.firstOrNull(),
                            count = favoriteItems.size,
                            favorite = true,
                            file = favoriteItems.firstOrNull()?.let { c.repo.fileOf(it) },
                            selected = selection.contains(FAVORITES_PATH),
                            selectionMode = selectionMode,
                            onClick = { handleAlbumClick(c, FAVORITES_PATH, selectionMode) { selection = selection + it } },
                            onLongClick = { selection = selection + FAVORITES_PATH }
                        )
                    }
                }
                items(albums, key = { it.path }) { album ->
                    val items = c.repo.albumItems(album.path, c.showHiddenNow())
                    FolderListRow(
                        album = album,
                        cover = c.repo.coverOf(album, c.showHiddenNow()),
                        count = items.size,
                        favorite = false,
                        file = c.repo.coverOf(album, c.showHiddenNow())?.let { c.repo.fileOf(it) },
                        selected = selection.contains(album.path),
                        selectionMode = selectionMode,
                        onClick = { handleAlbumClick(c, album.path, selectionMode) { selection = selection + it } },
                        onLongClick = { selection = selection + album.path }
                    )
                }
            }
        } else {
            val columns = c.settings.columns.get().coerceIn(1, 15)
            LazyVerticalGrid(columns = GridCells.Fixed(columns), modifier = Modifier.fillMaxSize()) {
                if (favoriteItems.isNotEmpty() || selectionMode) {
                    item {
                        FolderTile(
                            album = Album(FAVORITES_PATH, "收藏"),
                            cover = favoriteItems.firstOrNull(),
                            count = favoriteItems.size,
                            selected = selection.contains(FAVORITES_PATH),
                            selectionMode = selectionMode,
                            favorite = true,
                            file = favoriteItems.firstOrNull()?.let { c.repo.fileOf(it) },
                            onClick = { handleAlbumClick(c, FAVORITES_PATH, selectionMode) { selection = selection + it } },
                            onLongClick = { selection = selection + FAVORITES_PATH }
                        )
                    }
                }
                items(albums, key = { it.path }) { album ->
                    FolderTile(
                        album = album,
                        cover = c.repo.coverOf(album, c.showHiddenNow()),
                        count = c.repo.albumItems(album.path, c.showHiddenNow()).size,
                        selected = selection.contains(album.path),
                        selectionMode = selectionMode,
                        favorite = false,
                        file = c.repo.coverOf(album, c.showHiddenNow())?.let { c.repo.fileOf(it) },
                        onClick = { handleAlbumClick(c, album.path, selectionMode) { selection = selection + it } },
                        onLongClick = { selection = selection + album.path }
                    )
                }
            }
        }
    }

    when (dialog) {
        "sort" -> SortDialog(c) { dialog = null }
        "filter" -> FilterFilesDialog(
            images = c.settings.filterImages.get(), videos = c.settings.filterVideos.get(),
            gif = c.settings.filterGif.get(), raw = c.settings.filterRaw.get(),
            svg = c.settings.filterSvg.get(), portrait = c.settings.filterPortrait.get(),
            onConfirm = { i, v, g, r, s, p ->
                c.settings.filterImages.set(i); c.settings.filterVideos.set(v); c.settings.filterGif.set(g)
                c.settings.filterRaw.set(r); c.settings.filterSvg.set(s); c.settings.filterPortrait.set(p)
                dialog = null
            }, onDismiss = { dialog = null }
        )
        "viewType" -> ViewTypeDialog(
            viewType = c.settings.viewType.get(), groupByFolder = c.settings.groupByFolder.get(),
            onConfirm = { type, grouped ->
                c.settings.viewType.set(type); c.settings.groupByFolder.set(grouped); dialog = null
            }, onDismiss = { dialog = null }
        )
        "columns" -> ColumnsDialog(c.settings.columns.get(), onSelect = { c.settings.columns.set(it); dialog = null }, onDismiss = { dialog = null })
        "newFolder" -> TextFieldDialog(
            title = "新建文件夹", label = "标题", initial = "",
            onConfirm = { name, _ ->
                if (name.isNotBlank()) {
                    val path = "DCIM/$name"
                    c.repo.createAlbum(path)
                    File(c.repo.root, path).mkdirs()
                    c.toast("已创建文件夹 $name")
                }
                dialog = null
            }, onDismiss = { dialog = null }
        )
        "renameAlbum" -> {
            val path = selection.firstOrNull()
            val album = c.albums().firstOrNull { it.path == path }
            TextFieldDialog(
                title = "重命名", label = "标题", initial = album?.name ?: "",
                onConfirm = { name, _ ->
                    if (name.isNotBlank() && album != null) {
                        val target = File(c.repo.root, path!!.substringBeforeLast('/') + "/" + name)
                        File(c.repo.root, path!!).renameTo(target)
                        c.repo.items.filter { it.albumPath == path }.forEach { it.albumPath = target.path.removePrefix(c.repo.root.path + "/") }
                        val stored = c.repo.albums.firstOrNull { it.path == path }
                        stored?.let {
                            c.repo.albums.remove(it)
                            c.repo.albums.add(Album(target.path.removePrefix(c.repo.root.path + "/"), name))
                        }
                        c.repo.save()
                    }
                    selection = emptySet(); dialog = null; c.toast("已重命名")
                }, onDismiss = { dialog = null }
            )
        }
        "albumProperties" -> MessageDialog(
            title = "属性",
            text = "已选择 ${selection.size} 个文件夹",
            onDismiss = { dialog = null }
        )
        "shortcut" -> TextFieldDialog(
            title = "创建快捷方式", label = "名称", initial = selection.firstOrNull()?.substringAfterLast('/') ?: "",
            onConfirm = { name, _ -> selection = emptySet(); dialog = null; c.toast("已创建快捷方式 $name") },
            onDismiss = { dialog = null }
        )
        "copyAlbum", "moveAlbum" -> TargetPickerDialog(
            albums = c.albums(),
            coverOf = { c.repo.coverOf(it, true) },
            fileOf = { c.repo.fileOf(it) },
            onPick = { target ->
                selection.forEach { path ->
                    c.repo.albumItems(path, true).forEach { item ->
                        if (dialog == "copyAlbum") c.repo.copyTo(item, target) else c.repo.moveTo(item, target)
                    }
                }
                selection = emptySet(); dialog = null; c.toast(if (dialog == "copyAlbum") "已复制" else "已移动")
            },
            onOtherFolders = { dialog = "browse" },
            onDismiss = { dialog = null }
        )
        "browse" -> FolderBrowserDialog(
            rootLabel = "内部存储空间", root = c.repo.root,
            onPick = { path ->
                val rel = path.removePrefix(c.repo.root.path).trimStart('/')
                selection.forEach { source -> c.repo.albumItems(source, true).forEach { c.repo.copyTo(it, rel) } }
                selection = emptySet(); dialog = null; c.toast("已复制到 $rel")
            },
            onCreateFolder = { parent ->
                val dir = File(parent, "新建文件夹")
                dir.mkdirs(); c.toast("已创建 新建文件夹")
            },
            onDismiss = { dialog = null }
        )
        "deleteAlbums" -> DeleteConfirmDialog(
            count = selection.size, noAskAgain = false, onNoAskAgainChange = {},
            skipTrash = false, onSkipTrashChange = {},
            onConfirm = {
                selection.forEach { path -> c.repo.albumItems(path, true).forEach { c.repo.delete(it, false) } }
                selection = emptySet(); dialog = null; c.toast("已删除")
            }, onDismiss = { dialog = null }
        )
    }

    if (submenu == "cover") {
        val path = selection.firstOrNull()
        RadioListDialog(
            title = "更换封面图片",
            options = listOf("更换封面图片", "选择照片", "使用默认值"),
            selected = "更换封面图片",
            onSelect = { option ->
                when (option) {
                    "选择照片" -> { submenu = null; dialog = "pickCover" }
                    "使用默认值" -> {
                        c.albums().firstOrNull { it.path == path }?.let { c.repo.setCover(it, null) }
                        submenu = null; selection = emptySet(); c.toast("已恢复默认封面")
                    }
                }
            },
            onDismiss = { submenu = null }
        )
    }

    if (dialog == "pickCover") {
        val path = selection.firstOrNull()
        val items = c.repo.albumItems(path ?: "", true)
        AlertDialogGridPicker(
            title = "选择照片",
            items = items,
            fileOf = { c.repo.fileOf(it) },
            onPick = { item ->
                c.albums().firstOrNull { it.path == path }?.let { c.repo.setCover(it, item) }
                dialog = null; selection = emptySet(); c.toast("已更换封面")
            },
            onDismiss = { dialog = null }
        )
    }

    toast?.let { ToastHost(it) { toast = null } }
}

private fun handleAlbumClick(c: AppController, path: String, selectionMode: Boolean, select: (String) -> Unit) {
    if (selectionMode) select(path) else c.push(Screen.Album(path))
}

@Composable
fun SortDialog(c: AppController, onDismiss: () -> Unit) {
    var pending by remember { mutableStateOf<String?>(null) }
    val options = listOf("名称", "路径", "大小", "修改日期", "拍摄日期", "随机")
    if (pending == null) {
        RadioListDialog(
            title = "排序方式",
            options = options + if (c.settings.sortAscending.get()) "降序" else "升序",
            selected = c.settings.sortBy.get(),
            onSelect = { option ->
                if (option == "降序" || option == "升序") c.settings.sortAscending.set(option == "升序")
                else { c.settings.sortBy.set(option); onDismiss() }
            },
            onDismiss = onDismiss
        )
    }
}

@Composable
fun AlertDialogGridPicker(
    title: String,
    items: List<MediaItem>,
    fileOf: (MediaItem) -> File,
    onPick: (MediaItem) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = GalleryPrimary, fontSize = 20.sp) },
        text = {
            LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.height(420.dp)) {
                items(items) { item ->
                    Box(modifier = Modifier.padding(2.dp).clickable { onPick(item) }) {
                        MediaThumb(fileOf(item), item, Modifier.fillMaxWidth().height(110.dp))
                    }
                }
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun ToastHost(text: String, onDone: () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(text) {
        kotlinx.coroutines.delay(1800)
        onDone()
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Box(
            modifier = Modifier.padding(bottom = 120.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xEE3A3A3A))
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) { Text(text, color = Color.White, fontSize = 15.sp) }
    }
}

/** All media timeline, grouped by the day the file was taken. */
@Composable
fun TimelineScreen(c: AppController) {
    c.settings.revision.value
    var menuOpen by remember { mutableStateOf(false) }
    var selection by remember { mutableStateOf(setOf<String>()) }
    var dialog by remember { mutableStateOf<String?>(null) }
    var selectionMenu by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }

    val all = c.filterItems(c.allItems())
    val grouped = all.groupBy { formatDate(it.takenMs) }
    val selectionMode = selection.isNotEmpty()

    Column(modifier = Modifier.fillMaxSize().background(GalleryBackground)) {
        if (selectionMode) {
            SelectionBar(
                count = selection.size,
                onBack = { selection = emptySet() },
                onDelete = { dialog = "delete" },
                onShare = { toast = "已分享 ${selection.size} 个项目" },
                onInfo = { dialog = "properties" },
                onMenu = { selectionMenu = true },
                menu = { SelectionMenu(c, selection, { selection = emptySet() }, { dialog = it }, selectionMenu) { selectionMenu = false } }
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().background(GalleryBackground).padding(4.dp)
            ) {
                IconButton(onClick = { c.pop() }) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
                Text("在全部媒体中搜索", color = GallerySecondaryText, fontSize = 17.sp, modifier = Modifier.weight(1f))
                Box {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, contentDescription = "更多") }
                    MenuEntries(
                        entries = listOf(
                            "排序方式" to { dialog = "sort" },
                            "过滤显示的文件" to { dialog = "filter" },
                            "更改视图类型" to { dialog = "viewType" },
                            "设置" to { c.push(Screen.Settings) }
                        ), expanded = menuOpen, onDismiss = { menuOpen = false }
                    )
                }
            }
        }

        val columns = c.settings.columns.get().coerceIn(1, 15)
        LazyVerticalGrid(columns = GridCells.Fixed(columns), modifier = Modifier.fillMaxSize()) {
            grouped.forEach { (day, items) ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        day, fontSize = 15.sp, color = GalleryText,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
                items(items, key = { it.id }) { item ->
                    val index = all.indexOf(item)
                    MediaTile(
                        item = item,
                        file = c.repo.fileOf(item),
                        selected = selection.contains(item.id),
                        selectionMode = selectionMode,
                        showName = c.settings.showFilenames.get(),
                        onClick = {
                            if (selectionMode) selection = toggle(selection, item.id)
                            else c.push(Screen.Viewer("", all.map { it.id }, index))
                        },
                        onLongClick = { selection = selection + item.id }
                    )
                }
            }
        }
    }

    when (dialog) {
        "sort" -> SortDialog(c) { dialog = null }
        "filter" -> FilterFilesDialog(
            c.settings.filterImages.get(), c.settings.filterVideos.get(), c.settings.filterGif.get(),
            c.settings.filterRaw.get(), c.settings.filterSvg.get(), c.settings.filterPortrait.get(),
            onConfirm = { i, v, g, r, s, p ->
                c.settings.filterImages.set(i); c.settings.filterVideos.set(v); c.settings.filterGif.set(g)
                c.settings.filterRaw.set(r); c.settings.filterSvg.set(s); c.settings.filterPortrait.set(p); dialog = null
            }, onDismiss = { dialog = null }
        )
        "viewType" -> ViewTypeDialog(
            c.settings.viewType.get(), c.settings.groupByFolder.get(),
            onConfirm = { type, grouped2 -> c.settings.viewType.set(type); c.settings.groupByFolder.set(grouped2); dialog = null },
            onDismiss = { dialog = null }
        )
        "delete" -> {
            var skipTrash by remember { mutableStateOf(false) }
            var noAsk by remember { mutableStateOf(false) }
            DeleteConfirmDialog(
                count = selection.size, noAskAgain = noAsk, onNoAskAgainChange = { noAsk = it },
                skipTrash = skipTrash, onSkipTrashChange = { skipTrash = it },
                onConfirm = {
                    val list = all.filter { selection.contains(it.id) }
                    c.repo.deleteMany(list, skipTrash)
                    if (noAsk) c.settings.noDeleteConfirm.set(true)
                    selection = emptySet(); dialog = null; toast = "已删除到回收站"
                }, onDismiss = { dialog = null }
            )
        }
        "properties" -> MessageDialog("属性", "已选择 ${selection.size} 个项目", onDismiss = { dialog = null })
    }

    toast?.let { ToastHost(it) { toast = null } }
}

private fun toggle(set: Set<String>, id: String): Set<String> =
    if (set.contains(id)) set - id else set + id

@Composable
private fun SelectionMenu(
    c: AppController,
    selection: Set<String>,
    clear: () -> Unit,
    openDialog: (String) -> Unit,
    expanded: Boolean,
    onDismiss: () -> Unit
) {
    MenuEntries(
        entries = listOf(
            "旋转" to { openDialog("rotate") },
            "属性" to { openDialog("properties") },
            "重命名" to { openDialog("rename") },
            "隐藏" to {
                c.repo.items.filter { selection.contains(it.id) }.forEach { c.repo.setHidden(it, true) }
                clear(); c.toast("已隐藏")
            },
            "复制到" to { openDialog("copy") },
            "移动到" to { openDialog("move") },
            "创建快捷方式" to { openDialog("shortcut") },
            "打开方式" to { c.toast("没有可用的应用") },
            "设置为" to { openDialog("setAs") },
            "调整大小" to { openDialog("resize") },
            "编辑" to { openDialog("edit") },
            "添加到收藏" to {
                c.repo.items.filter { selection.contains(it.id) }.forEach { if (!it.favorite) c.repo.toggleFavorite(it) }
                clear(); c.toast("已添加到收藏")
            },
            "修复拍摄日期" to { c.toast("没有可修复的拍摄日期") },
            "全选" to { }
        ),
        expanded = expanded, onDismiss = onDismiss, hasArrow = setOf("旋转")
    )
}

/** Media grid of a single album or of the favorites bucket. */
@Composable
fun AlbumGridScreen(c: AppController, albumPath: String) {
    c.settings.revision.value
    val isFavorites = albumPath == FAVORITES_PATH
    var menuOpen by remember { mutableStateOf(false) }
    var selectionMenu by remember { mutableStateOf(false) }
    var selection by remember { mutableStateOf(setOf<String>()) }
    var dialog by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var toast by remember { mutableStateOf<String?>(null) }

    val all = if (isFavorites) c.favorites() else c.itemsOfAlbum(albumPath)
    val items = all.filter { query.isBlank() || it.fileName.contains(query, true) }
    val selectionMode = selection.isNotEmpty()
    val album = c.repo.albums.firstOrNull { it.path == albumPath }
    val title = if (isFavorites) "收藏" else c.repo.albumName(albumPath)

    Column(modifier = Modifier.fillMaxSize().background(GalleryBackground)) {
        if (selectionMode) {
            SelectionBar(
                count = selection.size,
                onBack = { selection = emptySet() },
                onDelete = { dialog = "delete" },
                onShare = { toast = "已分享 ${selection.size} 个项目" },
                onInfo = { dialog = "properties" },
                onMenu = { selectionMenu = true },
                menu = { SelectionMenu(c, selection, { selection = emptySet() }, { dialog = it }, selectionMenu) { selectionMenu = false } }
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().background(GalleryBackground).padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                IconButton(onClick = { c.pop() }) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(23.dp))
                        .background(GalleryTopBar).padding(horizontal = 14.dp)
                ) {
                    if (query.isEmpty()) Text("在 $title 中搜索", color = GallerySecondaryText, fontSize = 16.sp)
                    androidx.compose.foundation.text.BasicTextField(
                        value = query, onValueChange = { query = it },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = GalleryText),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                IconButton(onClick = { c.settings.showFilenames.set(!c.settings.showFilenames.get()) }) {
                    Icon(Icons.Default.List, contentDescription = "文件名")
                }
                IconButton(onClick = { dialog = "sort" }) { Icon(Icons.Default.List, contentDescription = "排序") }
                Box {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, contentDescription = "更多") }
                    MenuEntries(
                        entries = listOf(
                            "过滤显示的文件" to { dialog = "filter" },
                            "更改视图类型" to { dialog = "viewType" },
                            "临时显示隐藏项目" to {
                                c.temporaryHiddenReveal = !c.temporaryHiddenReveal
                                toast = if (c.temporaryHiddenReveal) "已临时显示隐藏项目" else "已隐藏隐藏项目"
                            },
                            "打开回收站" to { c.push(Screen.Trash) },
                            "分组方式" to { dialog = "group" },
                            "设置为默认文件夹" to {
                                c.settings.defaultFolder.set(albumPath); toast = "已设置为默认文件夹"
                            },
                            "新建文件夹" to { dialog = "newFolder" },
                            "列数" to { dialog = "columns" },
                            "幻灯片" to { dialog = "slideshow" },
                            "设置" to { c.push(Screen.Settings) }
                        ),
                        expanded = menuOpen, onDismiss = { menuOpen = false }
                    )
                }
            }
        }

        val columns = c.settings.columns.get().coerceIn(1, 15)
        LazyVerticalGrid(columns = GridCells.Fixed(columns), modifier = Modifier.fillMaxSize()) {
            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                MediaTile(
                    item = item,
                    file = c.repo.fileOf(item),
                    selected = selection.contains(item.id),
                    selectionMode = selectionMode,
                    showName = c.settings.showFilenames.get(),
                    onClick = {
                        if (selectionMode) selection = toggle(selection, item.id)
                        else c.push(Screen.Viewer(albumPath, items.map { it.id }, index))
                    },
                    onLongClick = { selection = selection + item.id }
                )
            }
        }
    }

    when (dialog) {
        "sort" -> SortDialog(c) { dialog = null }
        "columns" -> ColumnsDialog(c.settings.columns.get(), { c.settings.columns.set(it); dialog = null }, { dialog = null })
        "group" -> RadioListDialog(
            "分组方式", listOf("无", "按日期", "按文件夹"), c.settings.groupBy.get(),
            onSelect = { c.settings.groupBy.set(it); dialog = null }, onDismiss = { dialog = null }
        )
        "filter" -> FilterFilesDialog(
            c.settings.filterImages.get(), c.settings.filterVideos.get(), c.settings.filterGif.get(),
            c.settings.filterRaw.get(), c.settings.filterSvg.get(), c.settings.filterPortrait.get(),
            onConfirm = { i, v, g, r, s, p ->
                c.settings.filterImages.set(i); c.settings.filterVideos.set(v); c.settings.filterGif.set(g)
                c.settings.filterRaw.set(r); c.settings.filterSvg.set(s); c.settings.filterPortrait.set(p); dialog = null
            }, onDismiss = { dialog = null }
        )
        "viewType" -> ViewTypeDialog(
            c.settings.viewType.get(), c.settings.groupByFolder.get(),
            onConfirm = { type, grouped -> c.settings.viewType.set(type); c.settings.groupByFolder.set(grouped); dialog = null },
            onDismiss = { dialog = null }
        )
        "newFolder" -> TextFieldDialog(
            "新建文件夹", "标题", "",
            onConfirm = { name, _ ->
                if (name.isNotBlank()) { c.repo.createAlbum("$albumPath/$name"); File(c.repo.root, "$albumPath/$name").mkdirs(); toast = "已创建文件夹 $name" }
                dialog = null
            }, onDismiss = { dialog = null }
        )
        "slideshow" -> SlideshowDialog(
            onStart = { _, _, _, _, _ -> dialog = null; c.push(Screen.Viewer(albumPath, items.map { it.id }, 0)); toast = "幻灯片放映完毕" },
            onDismiss = { dialog = null }
        )
        "delete" -> {
            var skipTrash by remember { mutableStateOf(false) }
            var noAsk by remember { mutableStateOf(false) }
            DeleteConfirmDialog(
                count = selection.size, noAskAgain = noAsk, onNoAskAgainChange = { noAsk = it },
                skipTrash = skipTrash, onSkipTrashChange = { skipTrash = it },
                onConfirm = {
                    val list = all.filter { selection.contains(it.id) }
                    c.repo.deleteMany(list, skipTrash)
                    if (noAsk) c.settings.noDeleteConfirm.set(true)
                    selection = emptySet(); dialog = null; toast = "已删除到回收站"
                }, onDismiss = { dialog = null }
            )
        }
        "properties" -> {
            val item = all.firstOrNull { selection.contains(it.id) }
            if (item != null) PropertiesDialog(
                item, c.repo.fileOf(item).parent ?: "",
                onRemoveExif = { c.repo.removeExif(item); dialog = null; toast = "已移除 EXIF 数据" },
                onDismiss = { dialog = null }
            )
        }
        "rename" -> {
            val item = all.firstOrNull { selection.contains(it.id) }
            if (item != null) TextFieldDialog(
                "重命名", "标题", item.fileName.substringBeforeLast('.'),
                extraLabel = "扩展名", extraInitial = item.fileName.substringAfterLast('.', "jpg"),
                onConfirm = { name, ext ->
                    c.repo.rename(item, "$name.$ext")
                    selection = emptySet(); dialog = null; toast = "已重命名"
                }, onDismiss = { dialog = null }
            )
        }
        "resize" -> {
            val item = all.firstOrNull { selection.contains(it.id) }
            if (item != null) ResizeDialog(item, { w, h, folder, name ->
                c.repo.resize(item, w, h, folder, name)
                selection = emptySet(); dialog = null; toast = "已保存缩放副本"
            }, { dialog = null })
        }
        "setAs" -> SetAsDialog(onPick = { selection = emptySet(); dialog = null; toast = "已设置" }, onDismiss = { dialog = null })
        "shortcut" -> TextFieldDialog(
            "创建快捷方式", "名称", all.firstOrNull { selection.contains(it.id) }?.fileName ?: "",
            onConfirm = { _, _ -> selection = emptySet(); dialog = null; toast = "已创建快捷方式" }, onDismiss = { dialog = null }
        )
        "rotate" -> RadioListDialog(
            "旋转", listOf("向右旋转", "向左旋转", "旋转 180°"), "",
            onSelect = { option ->
                val degrees = when (option) { "向右旋转" -> 90; "向左旋转" -> 270; else -> 180 }
                all.filter { selection.contains(it.id) }.forEach { c.repo.rotate(it, degrees) }
                selection = emptySet(); dialog = null; toast = "已旋转"
            }, onDismiss = { dialog = null }
        )
        "edit" -> {
            val index = all.indexOfFirst { selection.contains(it.id) }
            if (index >= 0) { selection = emptySet(); c.push(Screen.Viewer(albumPath, all.map { it.id }, index)) }
            dialog = null
        }
        "copy", "move" -> TargetPickerDialog(
            albums = c.albums(),
            coverOf = { c.repo.coverOf(it, true) },
            fileOf = { c.repo.fileOf(it) },
            onPick = { target ->
                val list = all.filter { selection.contains(it.id) }
                if (dialog == "copy") c.repo.copyMany(list, target) else c.repo.moveMany(list, target)
                selection = emptySet(); dialog = null; toast = if (dialog == "copy") "已复制" else "已移动"
            },
            onOtherFolders = { dialog = "browse" },
            onDismiss = { dialog = null }
        )
        "browse" -> FolderBrowserDialog(
            rootLabel = "内部存储空间", root = c.repo.root,
            onPick = { path ->
                val rel = path.removePrefix(c.repo.root.path).trimStart('/')
                val list = all.filter { selection.contains(it.id) }
                c.repo.copyMany(list, rel)
                selection = emptySet(); dialog = null; toast = "已复制到 $rel"
            },
            onCreateFolder = { parent -> File(parent, "新建文件夹").mkdirs(); toast = "已创建 新建文件夹" },
            onDismiss = { dialog = null }
        )
    }

    if (album != null && !isFavorites) { /* album state is kept in the repository */ }
    toast?.let { ToastHost(it) { toast = null } }
}
