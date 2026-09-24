package com.blackboxbench.reproduction

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderGridScreen(app: AppState, toast: (String) -> Unit) {
    val repo = app.repo
    var search by remember { mutableStateOf("") }
    var selection by remember { mutableStateOf(setOf<String>()) }
    var mainMenu by remember { mutableStateOf(false) }
    var selMenu by remember { mutableStateOf(false) }

    var showSort by remember { mutableStateOf(false) }
    var showFilter by remember { mutableStateOf(false) }
    var showViewType by remember { mutableStateOf(false) }
    var showColumns by remember { mutableStateOf(false) }
    var showNewFolder by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showCopyMove by remember { mutableStateOf<Pair<String, Boolean>?>(null) } // title to isCopy
    var showExclude by remember { mutableStateOf(false) }
    var showLock by remember { mutableStateOf(false) }
    var showCover by remember { mutableStateOf(false) }

    val showHidden = app.showHiddenTemp.value || repo.showHiddenSetting.value
    val showExcluded = app.showExcludedTemp.value
    val allMedia = app.allMediaView.value

    val folders = remember(
        repo.items.size, repo.version.value, repo.favorites.value, repo.pinnedFolders.value,
        repo.excludedFolders.value, repo.hiddenFolders.value, repo.hiddenMedia.value,
        repo.extraFolders.value, repo.covers.value, showHidden, showExcluded,
        repo.sortMode.value, repo.sortAsc.value, search, allMedia
    ) {
        if (allMedia) emptyList() else {
            var list = repo.folderNames(showExcluded, showHidden)
            if (search.isNotBlank()) list = list.filter { it.name.contains(search, true) }
            list = sortFolders(list, repo)
            val pinned = repo.pinnedFolders.value
            list.sortedByDescending { pinned.contains(it.name) }
        }
    }

    val favItems = repo.favoriteItems()
    val delItems = repo.deletedItems()
    val virtualFolders = remember(favItems.size, delItems.size, search, allMedia) {
        if (allMedia) emptyList() else buildList {
            if (favItems.isNotEmpty()) add(FolderInfo("收藏", favItems.size, favItems.last().id, true))
            if (delItems.isNotEmpty()) add(FolderInfo("回收站", delItems.size, delItems.last().id, true))
        }.filter { search.isBlank() || it.name.contains(search, true) }
    }

    val allMediaItems = remember(
        repo.items.size, repo.version.value, repo.favorites.value, repo.hiddenMedia.value, repo.hiddenFolders.value,
        repo.filters.value, showHidden, search, allMedia, repo.sortMode.value, repo.sortAsc.value
    ) {
        if (!allMedia) emptyList() else {
            var list = repo.visibleMedia(showHidden)
            if (search.isNotBlank()) list = list.filter { it.name.contains(search, true) }
            sortedMedia(list, repo.sortMode.value, repo.sortAsc.value)
        }
    }

    Column(Modifier.fillMaxSize()) {
        // top bar
        if (selection.isEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(if (allMedia) "搜索文件" else "搜索文件夹") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
                IconButton(onClick = { toast("未找到可用的相机应用") }) {
                    CameraGlyph()
                }
                IconButton(onClick = { app.allMediaView.value = !app.allMediaView.value }) {
                    Icon(if (allMedia) Icons.Default.List else Icons.Default.List, "切换视图")
                }
                Box {
                    IconButton(onClick = { mainMenu = true }) {
                        Icon(Icons.Default.MoreVert, "菜单")
                    }
                    DropdownMenu(expanded = mainMenu, onDismissRequest = { mainMenu = false }) {
                        DropdownMenuItem(text = { Text("排序方式") }, onClick = { mainMenu = false; showSort = true })
                        DropdownMenuItem(text = { Text("过滤显示的文件") }, onClick = { mainMenu = false; showFilter = true })
                        DropdownMenuItem(text = { Text("更改视图类型") }, onClick = { mainMenu = false; showViewType = true })
                        DropdownMenuItem(
                            text = { Text(if (app.showHiddenTemp.value) "✓ 临时显示隐藏项目" else "临时显示隐藏项目") },
                            onClick = { mainMenu = false; app.showHiddenTemp.value = !app.showHiddenTemp.value })
                        DropdownMenuItem(
                            text = { Text(if (app.showExcludedTemp.value) "✓ 临时显示已排除项目" else "临时显示已排除项目") },
                            onClick = { mainMenu = false; app.showExcludedTemp.value = !app.showExcludedTemp.value })
                        DropdownMenuItem(text = { Text("新建文件夹") }, onClick = { mainMenu = false; showNewFolder = true })
                        DropdownMenuItem(text = { Text("列数") }, onClick = { mainMenu = false; showColumns = true })
                        DropdownMenuItem(text = { Text("设置") }, onClick = { mainMenu = false; app.navigate(Screen.Settings) })
                        DropdownMenuItem(text = { Text("关于") }, onClick = { mainMenu = false; app.navigate(Screen.About) })
                    }
                }
            }
            if (search.isNotBlank() && !allMedia) {
                Text(
                    "切换文件搜索范围到所有可见的文件夹",
                    color = Color(0xFF8C9FD0),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).clickable {
                        app.allMediaView.value = true
                    }
                )
            }
        } else {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selection = emptySet() }) {
                    Text("✕", fontSize = 18.sp)
                }
                Text(
                    "${selection.size}/${folders.size + virtualFolders.size}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showDelete = true }) {
                    Icon(Icons.Default.Delete, "删除")
                }
                IconButton(onClick = { showInfo = true }) {
                    Icon(Icons.Default.Info, "信息")
                }
                IconButton(onClick = {
                    selection.forEach { repo.togglePin(it) }
                    selection = emptySet()
                }) {
                    PinBadge()
                }
                Box {
                    IconButton(onClick = { selMenu = true }) {
                        Icon(Icons.Default.MoreVert, "更多")
                    }
                    DropdownMenu(expanded = selMenu, onDismissRequest = { selMenu = false }) {
                        if (selection.all { repo.pinnedFolders.value.contains(it) }) {
                            DropdownMenuItem(text = { Text("取消靠前固定文件夹") }, onClick = {
                                selection.forEach { repo.togglePin(it) }
                                selection = emptySet(); selMenu = false
                            })
                        }
                        DropdownMenuItem(text = { Text("拖动以重新排列") }, onClick = {
                            selMenu = false; toast("长按并拖动以重新排列")
                        })
                        DropdownMenuItem(text = { Text("重命名") }, onClick = { selMenu = false; showRename = true })
                        DropdownMenuItem(text = { Text("隐藏文件夹") }, onClick = {
                            selection.forEach { repo.hideFolder(it) }
                            selection = emptySet(); selMenu = false
                            toast("文件夹已隐藏")
                        })
                        DropdownMenuItem(text = { Text("复制到") }, onClick = { selMenu = false; showCopyMove = "复制到" to true })
                        DropdownMenuItem(text = { Text("移动到") }, onClick = { selMenu = false; showCopyMove = "移动到" to false })
                        DropdownMenuItem(text = { Text("创建快捷方式") }, onClick = {
                            selMenu = false; toast("无法创建快捷方式")
                        })
                        DropdownMenuItem(text = { Text("排除") }, onClick = { selMenu = false; showExclude = true })
                        DropdownMenuItem(text = { Text("锁定文件夹") }, onClick = { selMenu = false; showLock = true })
                        DropdownMenuItem(text = { Text("更换封面图片") }, onClick = { selMenu = false; showCover = true })
                        DropdownMenuItem(text = { Text("全选") }, onClick = {
                            selection = (folders.map { it.name } + virtualFolders.map { it.name }).toSet()
                            selMenu = false
                        })
                    }
                }
            }
        }

        // content
        if (allMedia) {
            if (allMediaItems.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("没有找到媒体文件。", color = Color.Gray)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(repo.columnsMain.value),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                var lastDay = ""
                allMediaItems.forEach { item ->
                    val day = formatDate(item.modified)
                    if (day != lastDay) {
                        lastDay = day
                        item(key = "h_$day", span = { GridItemSpan(repo.columnsMain.value) }) {
                            Text(
                                day,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                    item(key = item.id) {
                        val idx = allMediaItems.indexOf(item)
                        Box(
                            Modifier.aspectRatio(1f).combinedClickable(
                                onClick = {
                                    app.navigate(Screen.Viewer(ALL_MEDIA, idx))
                                },
                                onLongClick = {}
                            )
                        ) {
                            ThumbImage(repo, item, Modifier.fillMaxSize())
                            if (item.isVideo) {
                                VideoDurationBadge(item.durationSec, Modifier.align(Alignment.BottomEnd).padding(4.dp))
                            }
                            if (repo.favorites.value.contains(item.id)) {
                                StarBadge(Modifier.align(Alignment.TopEnd).padding(4.dp))
                            }
                        }
                    }
                }
                }
            }
        } else if (folders.isEmpty() && virtualFolders.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (search.isBlank()) "没有找到媒体文件。\n请手动添加包含媒体文件的文件夹。" else "未找到任何项目。",
                    color = Color.Gray
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(repo.columnsMain.value),
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(virtualFolders, key = { "v_${it.name}" }) { folder ->
                    FolderCard(
                        repo = repo,
                        folder = folder,
                        selected = selection.contains(folder.name),
                        pinned = false,
                        onClick = {
                            if (selection.isNotEmpty()) {
                                selection = if (selection.contains(folder.name)) selection - folder.name else selection + folder.name
                            } else app.navigate(Screen.FolderMedia(folder.name))
                        },
                        onLongClick = {
                            selection = if (selection.contains(folder.name)) selection - folder.name else selection + folder.name
                        }
                    )
                }
                items(folders, key = { it.name }) { folder ->
                    FolderCard(
                        repo = repo,
                        folder = folder,
                        selected = selection.contains(folder.name),
                        pinned = repo.pinnedFolders.value.contains(folder.name),
                        onClick = {
                            if (selection.isNotEmpty()) {
                                selection = if (selection.contains(folder.name)) selection - folder.name else selection + folder.name
                            } else app.navigate(Screen.FolderMedia(folder.name))
                        },
                        onLongClick = {
                            selection = if (selection.contains(folder.name)) selection - folder.name else selection + folder.name
                        }
                    )
                }
            }
        }
    }

    // dialogs
    if (showSort) SortDialog(repo.sortMode.value, repo.sortAsc.value, { showSort = false }) { m, a ->
        repo.setSort(m, a); showSort = false
    }
    if (showFilter) FilterDialog(repo.filters.value, { showFilter = false }) { repo.setFilters(it); showFilter = false }
    if (showViewType) ViewTypeDialog { showViewType = false }
    if (showColumns) ColumnsDialog(repo.columnsMain.value, { showColumns = false }) { repo.setColumnsMain(it); showColumns = false }
    if (showNewFolder) NewFolderDialog({ showNewFolder = false }) {
        repo.createFolder(it); showNewFolder = false; toast("文件夹已创建")
    }
    if (showDelete) {
        val count = selection.sumOf { name -> repo.mediaInFolder(name, true).size }
        DeleteConfirmDialog(
            text = "确定要将 ${selection.size} 个文件夹（共 $count 个项目）移至回收站吗？",
            onDismiss = { showDelete = false },
            onConfirm = { skip ->
                val ids = selection.flatMap { name -> repo.mediaInFolder(name, true).map { it.id } }
                repo.deleteMedia(ids.toSet(), skip)
                selection = emptySet(); showDelete = false
            }
        )
    }
    if (showInfo) {
        val lines = selection.map { name ->
            val media = repo.mediaInFolder(name, true)
            val size = media.sumOf { it.size }
            "$name\n路径：/storage/emulated/0/$name\n项目数：${media.size}\n大小：${formatSize(size)}"
        }
        InfoDialog("信息", lines.joinToString("\n\n")) { showInfo = false }
    }
    if (showRename && selection.size == 1) {
        val old = selection.first()
        TextInputDialog("重命名", old, { showRename = false }) { new ->
            if (new.isNotBlank() && new != old) repo.renameFolder(old, new)
            selection = emptySet(); showRename = false
        }
    } else if (showRename) {
        toast("一次只能重命名一个文件夹"); showRename = false
    }
    showCopyMove?.let { (title, isCopy) ->
        CopyMoveDialog(
            title = title,
            folders = repo.folderNames(true, true).map { it.name },
            onDismiss = { showCopyMove = null },
            onPick = { target ->
                val ids = selection.flatMap { name -> repo.mediaInFolder(name, true).map { it.id } }
                if (isCopy) {
                    val n = repo.copyMedia(ids.toSet(), target)
                    toast("已复制 $n 个项目")
                } else {
                    repo.moveMedia(ids.toSet(), target)
                    toast("已移动 ${ids.size} 个项目")
                }
                selection = emptySet(); showCopyMove = null
            }
        )
    }
    if (showExclude && selection.isNotEmpty()) {
        ExcludeDialog(selection.first(), { showExclude = false }) {
            selection.forEach { repo.excludeFolder(it) }
            selection = emptySet(); showExclude = false
            toast("文件夹已排除")
        }
    }
    if (showLock) LockFolderDialog { showLock = false }
    if (showCover && selection.isNotEmpty()) {
        val folder = selection.first()
        ChangeCoverDialog(
            media = repo.mediaInFolder(folder, true),
            onDismiss = { showCover = false },
            onPick = { id -> repo.setCover(folder, id); selection = emptySet(); showCover = false },
            onDefault = { repo.setCover(folder, null); selection = emptySet(); showCover = false }
        )
    }
}

fun sortFolders(list: List<FolderInfo>, repo: GalleryRepository): List<FolderInfo> {
    val sorted = when (repo.sortMode.value) {
        "名称" -> list.sortedBy { it.name }
        "路径" -> list.sortedBy { "/storage/emulated/0/${it.name}" }
        "大小" -> list.sortedBy { f -> repo.mediaInFolder(f.name, true).sumOf { it.size } }
        "项数" -> list.sortedBy { it.count }
        "修改日期", "拍摄日期" -> list.sortedBy { f ->
            repo.mediaInFolder(f.name, true).maxOfOrNull { it.modified } ?: 0L
        }
        "随机" -> list.shuffled()
        else -> list.sortedBy { it.name }
    }
    return if (repo.sortAsc.value) sorted else sorted.reversed()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderCard(
    repo: GalleryRepository,
    folder: FolderInfo,
    selected: Boolean,
    pinned: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val coverItem = folder.coverId?.let { id -> repo.items.firstOrNull { it.id == id } }
    Box(
        Modifier
            .aspectRatio(1f)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        CoverWithGradient(repo, coverItem, Modifier.fillMaxSize())
        Column(
            Modifier.align(Alignment.BottomStart).padding(10.dp)
        ) {
            Text(
                folder.name,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text("${folder.count}", color = Color(0xCCFFFFFF), fontSize = 13.sp)
        }
        if (pinned) PinBadge(Modifier.align(Alignment.TopEnd).padding(6.dp))
        if (selected) {
            Box(Modifier.fillMaxSize().clickable {}.padding(0.dp)) {}
            CheckBadge(Modifier.align(Alignment.TopStart).padding(6.dp))
            Box(Modifier.fillMaxSize().combinedClickable(onClick = onClick, onLongClick = onLongClick))
        }
    }
}
