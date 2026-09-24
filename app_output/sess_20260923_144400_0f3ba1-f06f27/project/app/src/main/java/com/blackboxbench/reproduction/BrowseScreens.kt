@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.blackboxbench.reproduction

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AlbumsScreen(g: GalleryState) {
    var menuOpen by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<String?>(null) }
    val albums = g.albums()
    val sorted = remember(albums, g.revision, g.text("albumSortMode", SortMode.MODIFIED.name)) {
        sortAlbums(albums, g)
    }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        TopPill(
            hint = if (g.searchFiles) "搜索文件" else "搜索文件夹",
            onSearchClick = { g.push(Screen.Search) },
            icons = {
                IconTap(kind = "camera") { g.message("没有可用的相机应用") }
                IconTap(kind = if (g.searchFiles) "folders" else "media") {
                    g.searchFiles = !g.searchFiles
                }
                Box {
                    IconTap(kind = "dots") { menuOpen = true }
                    SimpleMenu(menuOpen, { menuOpen = false }) {
                        MenuEntry("排序方式") { menuOpen = false; dialog = "sort" }
                        MenuEntry("过滤显示的文件") { menuOpen = false; dialog = "filter" }
                        MenuEntry("更改视图类型") { menuOpen = false; dialog = "view" }
                        MenuEntry(if (g.showHiddenTemp) "停止显示隐藏项目" else "临时显示隐藏项目") {
                            menuOpen = false
                            g.showHiddenTemp = !g.showHiddenTemp
                        }
                        MenuEntry(if (g.showExcludedTemp) "停止显示已排除项目" else "临时显示已排除项目") {
                            menuOpen = false
                            g.showExcludedTemp = !g.showExcludedTemp
                        }
                        MenuEntry("新建文件夹") { menuOpen = false; dialog = "newfolder" }
                        MenuEntry("列数") { menuOpen = false; dialog = "columns" }
                        MenuEntry("设置") { menuOpen = false; g.push(Screen.Settings) }
                        MenuEntry("关于") { menuOpen = false; g.push(Screen.About) }
                    }
                }
            }
        )

        if (g.searchFiles) {
            AllMediaGrid(g)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(sorted, key = { it.name }) { album ->
                    AlbumTile(album) {
                        g.push(Screen.Album(album.name, false))
                    }
                }
            }
        }
    }

    when (dialog) {
        "sort" -> SortDialog(g, isAlbum = true, onDismiss = { dialog = null })
        "filter" -> FilterDialog(g, onDismiss = { dialog = null })
        "view" -> ViewTypeDialog(g, onDismiss = { dialog = null })
        "columns" -> ColumnsDialog(g, onDismiss = { dialog = null })
        "newfolder" -> NewFolderDialog(g, parent = "", onDismiss = { dialog = null })
    }
}

private fun sortAlbums(albums: List<Album>, g: GalleryState): List<Album> {
    val pinned = albums.filter { it.pinned }
    val rest = albums.filterNot { it.pinned }
    val mode = try {
        SortMode.valueOf(g.text("albumSortMode", SortMode.MODIFIED.name))
    } catch (_: Exception) {
        SortMode.MODIFIED
    }
    val ascending = g.bool("albumSortAsc", false)
    val comparator: Comparator<Album> = when (mode) {
        SortMode.NAME -> compareBy { it.name.lowercase() }
        SortMode.PATH -> compareBy { it.name.lowercase() }
        SortMode.SIZE -> compareBy { it.items.sumOf { item -> item.sizeBytes } }
        SortMode.COUNT -> compareBy { it.count }
        SortMode.MODIFIED, SortMode.TAKEN -> compareBy { it.items.maxOfOrNull { item -> item.modified } ?: 0L }
        SortMode.RANDOM, SortMode.CUSTOM -> compareBy { it.name.lowercase() }
    }
    val ordered = if (ascending) rest.sortedWith(comparator) else rest.sortedWith(comparator.reversed())
    return pinned + ordered
}

@Composable
private fun AlbumTile(album: Album, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onClick() }
    ) {
        val cover = album.cover
        val bitmap = remember(cover?.relPath, cover?.modified) {
            val file = cover?.let { LibraryHolder.library?.fileOf(it) }
            if (file == null) null else Thumbs.thumbnail(file, 512, cover.isVideo)
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = album.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(Modifier.fillMaxSize().background(Color(0xFFCFCFDA)))
        }
        TileCaption(album.name, album.count.toString())
        if (album.pinned) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0x99000000)),
                contentAlignment = Alignment.Center,
            ) {
                Glyph(size = 16.dp, color = Color.White, kind = "pin")
            }
        }
    }
}

@Composable
private fun AllMediaGrid(g: GalleryState) {
    val items = remember(g.revision, g.showHiddenTemp, g.searchFiles) {
        val all = g.library.scanItems()
            .filter { g.showHidden() || !it.hidden }
            .filter { g.filter().accepts(it) }
        all.sortedByDescending { it.modified }
    }
    MediaGrid(g, items, album = "", isRecycle = false)
}

@Composable
fun MediaGrid(g: GalleryState, items: List<MediaItem>, album: String, isRecycle: Boolean) {
    val columns = (g.text("columns", "3").toIntOrNull() ?: 3).coerceIn(1, 15)
    val viewMode = g.text("viewMode", ViewMode.GRID.name)
    val showNames = g.bool("showFileNames", true)
    val grouping = g.groupMode()

    if (viewMode == ViewMode.LIST.name) {
        LazyColumn(Modifier.fillMaxSize()) {
            var lastHeader: String? = null
            items.forEach { item ->
                val header = g.sectionOf(item)
                if (grouping != GroupMode.NONE && header != null && header != lastHeader) {
                    lastHeader = header
                    item { SectionHeader(header) }
                }
                item(key = item.relPath) {
                    MediaListRow(g, item, isRecycle) { }
                }
            }
        }
        return
    }

    LazyVerticalGrid(columns = GridCells.Fixed(columns), modifier = Modifier.fillMaxSize()) {
        var lastHeader: String? = null
        items.forEach { item ->
            val header = g.sectionOf(item)
            if (grouping != GroupMode.NONE && header != null && header != lastHeader) {
                lastHeader = header
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    SectionHeader(header)
                }
            }
            item(key = item.relPath) {
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .combinedClickable(
                            onClick = {
                                if (g.selection.isNotEmpty()) {
                                    g.toggleSelection(item.relPath)
                                } else if (isRecycle || !item.isVideo) {
                                    g.push(Screen.Viewer(item.relPath, album, isRecycle))
                                } else {
                                    g.push(Screen.VideoViewer(item.relPath, album))
                                }
                            },
                            onLongClick = { g.toggleSelection(item.relPath) },
                        )
                ) {
                    MediaThumb(item, Modifier.fillMaxSize(), showName = showNames)
                    TileCheck(g.selection.contains(item.relPath))
                }
            }
        }
    }
}

@Composable
fun MediaListRow(g: GalleryState, item: MediaItem, isRecycle: Boolean, trailing: @Composable () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        if (g.selection.isNotEmpty()) g.toggleSelection(item.relPath)
                        else if (isRecycle || !item.isVideo) g.push(Screen.Viewer(item.relPath, item.album, isRecycle))
                        else g.push(Screen.VideoViewer(item.relPath, item.album))
                    },
                    onLongClick = { g.toggleSelection(item.relPath) },
                )
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(52.dp)) {
                MediaThumb(item, Modifier.fillMaxSize())
            }
            Spacer(Modifier.width(16.dp))
            Text(
                item.displayName,
                fontSize = 16.sp,
                color = Color(0xFF23232B),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            trailing()
        }
        HorizontalDivider(color = DividerGrey.copy(alpha = 0.5f))
    }
}

@Composable
fun AlbumScreen(g: GalleryState, album: String, isRecycle: Boolean) {
    var menuOpen by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<String?>(null) }
    val items = g.albumItems(album, isRecycle)
    val selection = g.selection
    val albumTitle = if (isRecycle) "回收站" else album

    Column(Modifier.fillMaxSize().background(Color.White)) {
        if (selection.isEmpty()) {
            TopPill(
                hint = "在 $albumTitle 中搜索",
                showBack = true,
                onBack = { g.pop() },
                onSearchClick = { g.push(Screen.Search) },
                icons = {
                    if (g.text("viewMode", ViewMode.GRID.name) == ViewMode.GRID.name) {
                        IconTap(kind = "names") { g.setBool("showFileNames", !g.bool("showFileNames", true)) }
                        IconTap(kind = "sort") { dialog = "sort" }
                    } else {
                        IconTap(kind = "sort") { dialog = "sort" }
                        IconTap(kind = "filter") { dialog = "filter" }
                    }
                    Box {
                        IconTap(kind = "dots") { menuOpen = true }
                        SimpleMenu(menuOpen, { menuOpen = false }) {
                            if (g.text("viewMode", ViewMode.GRID.name) == ViewMode.GRID.name) {
                                MenuEntry("过滤显示的文件") { menuOpen = false; dialog = "filter" }
                            }
                            MenuEntry("更改视图类型") { menuOpen = false; dialog = "view" }
                            MenuEntry(if (g.showHiddenTemp) "停止显示隐藏项目" else "临时显示隐藏项目") {
                                menuOpen = false
                                g.showHiddenTemp = !g.showHiddenTemp
                            }
                            if (!isRecycle) {
                                MenuEntry("打开回收站") {
                                    menuOpen = false
                                    g.push(Screen.Album(RECYCLE_ALBUM, true))
                                }
                            }
                            MenuEntry("分组方式") { menuOpen = false; dialog = "group" }
                            if (!isRecycle) {
                                MenuEntry("设置为默认文件夹") {
                                    menuOpen = false
                                    g.setText("defaultFolder", album)
                                    g.message("已将 $album 设为默认文件夹")
                                }
                            }
                            MenuEntry("新建文件夹") { menuOpen = false; dialog = "newfolder" }
                            if (g.text("viewMode", ViewMode.GRID.name) == ViewMode.GRID.name) {
                                MenuEntry("列数") { menuOpen = false; dialog = "columns" }
                            }
                            MenuEntry("幻灯片") {
                                menuOpen = false
                                if (items.isNotEmpty()) g.push(Screen.Viewer(items.first().relPath, album, isRecycle))
                            }
                            MenuEntry("设置") { menuOpen = false; g.push(Screen.Settings) }
                        }
                    }
                }
            )
        } else {
            SelectionBar(
                count = selection.size,
                total = items.size,
                onBack = { g.selection = emptySet() },
                onDelete = { dialog = "delete" },
                onShare = { g.message("已分享 ${selection.size} 个项目") },
                menu = {
                    SimpleMenu(menuOpen, { menuOpen = false }) {
                        MenuEntry("旋转") { menuOpen = false; g.message("已旋转所选项目") }
                        MenuEntry("属性") { menuOpen = false; dialog = "properties" }
                        if (isRecycle) {
                            MenuEntry("恢复此文件") {
                                menuOpen = false
                                g.selectedItems(items).forEach { g.library.restore(it) }
                                g.refresh()
                                g.selection = emptySet()
                                g.message("已恢复所选项目")
                            }
                        } else {
                            MenuEntry("重命名") { menuOpen = false; dialog = "rename" }
                            MenuEntry("隐藏") { menuOpen = false; dialog = "hide" }
                        }
                        MenuEntry("复制到") { menuOpen = false; dialog = "copy" }
                        MenuEntry("移动到") { menuOpen = false; dialog = "move" }
                        MenuEntry("创建快捷方式") { menuOpen = false; g.message("无法创建快捷方式") }
                        MenuEntry("打开方式") { menuOpen = false; g.message("没有可用的应用") }
                        MenuEntry("设置为") { menuOpen = false; dialog = "setas" }
                        MenuEntry("调整大小") { menuOpen = false; dialog = "resize" }
                        MenuEntry("编辑") { menuOpen = false; selection.firstOrNull()?.let { g.push(Screen.Editor(it, album)) } }
                        MenuEntry("添加到收藏") { menuOpen = false; dialog = "favorite" }
                        MenuEntry("修复拍摄日期") { menuOpen = false; dialog = "fixdate" }
                        MenuEntry("全选") { menuOpen = false; g.selectAll(items) }
                    }
                }
            )
        }

        if (items.isEmpty()) {
            EmptyState("未找到任何项目。")
        } else {
            MediaGrid(g, items, album, isRecycle)
        }
    }

    when (dialog) {
        "sort" -> SortDialog(g, isAlbum = false, onDismiss = { dialog = null })
        "filter" -> FilterDialog(g, onDismiss = { dialog = null })
        "view" -> ViewTypeDialog(g, onDismiss = { dialog = null })
        "columns" -> ColumnsDialog(g, onDismiss = { dialog = null })
        "group" -> GroupDialog(g, onDismiss = { dialog = null })
        "rename" -> RenameDialog(g, album, items, onDismiss = { dialog = null })
        "delete" -> DeleteDialog(g, items, isRecycle, onDismiss = { dialog = null })
        "hide" -> HideDialog(g, items, onDismiss = { dialog = null })
        "copy" -> FolderPickerDialog(g, "复制到", onDismiss = { dialog = null }) { target ->
            val chosen = g.selectedItems(items)
            val count = g.library.copyItems(chosen, target)
            g.refresh()
            g.selection = emptySet()
            dialog = null
            g.message("已复制 $count 个项目")
        }
        "move" -> FolderPickerDialog(g, "移动到", onDismiss = { dialog = null }) { target ->
            val chosen = g.selectedItems(items)
            val count = g.library.moveItems(chosen, target)
            g.refresh()
            g.selection = emptySet()
            dialog = null
            g.message("已移动 $count 个项目")
        }
        "resize" -> ResizeDialog(g, items, onDismiss = { dialog = null })
        "properties" -> PropertiesDialog(g, items.firstOrNull(), onDismiss = { dialog = null })
        "setas" -> SetAsDialog(g, onDismiss = { dialog = null })
        "favorite" -> {
            g.selectedItems(items).forEach { g.library.setFavorite(it, true) }
            g.refresh()
            g.selection = emptySet()
            dialog = null
        }
        "fixdate" -> {
            g.message("拍摄日期已修复")
            dialog = null
        }
        "newfolder" -> NewFolderDialog(g, parent = album, onDismiss = { dialog = null })
    }
}

@Composable
fun SearchScreen(g: GalleryState) {
    Column(Modifier.fillMaxSize().background(Color.White)) {
        TopPill(
            hint = if (g.searchFiles) "搜索文件" else "搜索文件夹",
            showBack = true,
            onBack = { g.pop() },
            icons = {
                IconTap(kind = "names") { }
            }
        )
        SearchField(g)
        val query = g.searchQuery.trim()
        if (query.isEmpty()) {
            if (!g.searchFiles) {
                Text(
                    "切换文件搜索范围到所有可见的文件夹",
                    color = Primary,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            g.searchFiles = true
                            g.setBool("searchAllFiles", true)
                        }
                        .padding(vertical = 14.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                EmptyState("未找到任何项目。")
            } else {
                AllMediaGrid(g)
            }
        } else {
            val all = g.library.scanItems().filter { g.showHidden() || !it.hidden }
            val matches = all.filter { it.displayName.contains(query, ignoreCase = true) }
            if (!g.searchFiles) {
                val albums = g.albums().filter { it.name.contains(query, ignoreCase = true) }
                if (albums.isEmpty()) {
                    Text(
                        "切换文件搜索范围到所有可见的文件夹",
                        color = Primary,
                        fontSize = 15.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { g.searchFiles = true }
                            .padding(vertical = 14.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    EmptyState("未找到任何项目。")
                } else {
                    LazyVerticalGrid(columns = GridCells.Fixed(2)) {
                        items(albums, key = { it.name }) { album ->
                            AlbumTile(album) { g.push(Screen.Album(album.name, false)) }
                        }
                    }
                }
            } else if (matches.isEmpty()) {
                EmptyState("未找到任何项目。")
            } else {
                MediaGrid(g, matches.sortedByDescending { it.modified }, album = "", isRecycle = false)
            }
        }
    }
}

@Composable
private fun SearchField(g: GalleryState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 6.dp)
            .height(46.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(23.dp))
            .background(PillSurface)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Glyph(size = 22.dp, color = Color(0xFF3B3B3B), kind = "search")
        Spacer(Modifier.width(12.dp))
        androidx.compose.foundation.text.BasicTextField(
            value = g.searchQuery,
            onValueChange = { g.searchQuery = it },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 17.sp, color = Color(0xFF23232B)),
            modifier = Modifier.weight(1f),
        )
    }
}
