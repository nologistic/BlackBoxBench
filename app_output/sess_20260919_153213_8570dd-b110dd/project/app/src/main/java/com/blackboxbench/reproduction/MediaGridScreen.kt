package com.blackboxbench.reproduction

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaGridScreen(app: AppState, folder: String, toast: (String) -> Unit) {
    val repo = app.repo
    var search by remember { mutableStateOf("") }
    var selection by remember { mutableStateOf(setOf<String>()) }
    var menu by remember { mutableStateOf(false) }
    var selMenu by remember { mutableStateOf(false) }
    var rotateSub by remember { mutableStateOf(false) }

    var showSort by remember { mutableStateOf(false) }
    var showFilter by remember { mutableStateOf(false) }
    var showViewType by remember { mutableStateOf(false) }
    var showColumns by remember { mutableStateOf(false) }
    var showGroup by remember { mutableStateOf(false) }
    var showNewFolder by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showProperties by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showCopyMove by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var showResize by remember { mutableStateOf(false) }

    val isRecycleBin = folder == "回收站"
    val showHidden = app.showHiddenTemp.value || repo.showHiddenSetting.value
    val isDefault = repo.defaultFolder.value == folder

    val media = remember(
        repo.items.size, repo.version.value, repo.favorites.value, repo.hiddenMedia.value, repo.hiddenFolders.value,
        repo.filters.value, showHidden, search, folder, repo.sortMode.value, repo.sortAsc.value
    ) {
        var list = repo.mediaInFolder(folder, showHidden)
        if (search.isNotBlank()) list = list.filter { it.name.contains(search, true) }
        sortedMedia(list, repo.sortMode.value, repo.sortAsc.value)
    }

    Column(Modifier.fillMaxSize()) {
        if (selection.isEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { app.pop() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                }
                TextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("在 $folder 中搜索") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
                IconButton(onClick = { showViewType = true }) {
                    Text("▦", fontSize = 20.sp)
                }
                IconButton(onClick = { showSort = true }) {
                    Icon(Icons.Default.Refresh, "排序")
                }
                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Default.MoreVert, "菜单")
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("过滤显示的文件") }, onClick = { menu = false; showFilter = true })
                        DropdownMenuItem(text = { Text("更改视图类型") }, onClick = { menu = false; showViewType = true })
                        DropdownMenuItem(
                            text = { Text(if (app.showHiddenTemp.value) "✓ 临时显示隐藏项目" else "临时显示隐藏项目") },
                            onClick = { menu = false; app.showHiddenTemp.value = !app.showHiddenTemp.value })
                        DropdownMenuItem(text = { Text("打开回收站") }, onClick = {
                            menu = false; app.navigate(Screen.FolderMedia("回收站"))
                        })
                        DropdownMenuItem(text = { Text("分组方式") }, onClick = { menu = false; showGroup = true })
                        DropdownMenuItem(
                            text = { Text(if (isDefault) "取消设为默认文件夹" else "设置为默认文件夹") },
                            onClick = {
                                menu = false
                                repo.setDefaultFolder(if (isDefault) null else folder)
                                toast(if (isDefault) "已取消默认文件夹" else "已设为默认文件夹")
                            })
                        DropdownMenuItem(text = { Text("新建文件夹") }, onClick = { menu = false; showNewFolder = true })
                        DropdownMenuItem(text = { Text("列数") }, onClick = { menu = false; showColumns = true })
                        DropdownMenuItem(text = { Text("幻灯片") }, onClick = {
                            menu = false
                            if (media.isNotEmpty()) app.navigate(Screen.Viewer(folder, 0, slideshow = true))
                        })
                        DropdownMenuItem(text = { Text("设置") }, onClick = { menu = false; app.navigate(Screen.Settings) })
                    }
                }
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
                    "${selection.size}/${media.size}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    if (repo.askDeleteConfirm.value) showDelete = true
                    else {
                        repo.deleteMedia(selection, false)
                        selection = emptySet()
                    }
                }) {
                    Icon(Icons.Default.Delete, "删除")
                }
                IconButton(onClick = { toast("未找到可分享到的应用") }) {
                    Icon(Icons.Default.Share, "分享")
                }
                Box {
                    IconButton(onClick = { selMenu = true }) {
                        Icon(Icons.Default.MoreVert, "更多")
                    }
                    DropdownMenu(expanded = selMenu, onDismissRequest = { selMenu = false }) {
                        if (isRecycleBin) {
                            DropdownMenuItem(text = { Text("恢复所选文件") }, onClick = {
                                repo.restoreMedia(selection)
                                selection = emptySet(); selMenu = false
                                toast("已恢复")
                            })
                        }
                        Box {
                            DropdownMenuItem(text = { Text("旋转 ▸") }, onClick = { rotateSub = true })
                            DropdownMenu(expanded = rotateSub, onDismissRequest = { rotateSub = false }) {
                                DropdownMenuItem(text = { Text("向右旋转") }, onClick = {
                                    selection.forEach { id -> repo.items.firstOrNull { it.id == id }?.let { repo.rotateMedia(it, 90f) } }
                                    rotateSub = false; selMenu = false
                                })
                                DropdownMenuItem(text = { Text("向左旋转") }, onClick = {
                                    selection.forEach { id -> repo.items.firstOrNull { it.id == id }?.let { repo.rotateMedia(it, -90f) } }
                                    rotateSub = false; selMenu = false
                                })
                                DropdownMenuItem(text = { Text("旋转 180°") }, onClick = {
                                    selection.forEach { id -> repo.items.firstOrNull { it.id == id }?.let { repo.rotateMedia(it, 180f) } }
                                    rotateSub = false; selMenu = false
                                })
                            }
                        }
                        DropdownMenuItem(text = { Text("属性") }, onClick = { selMenu = false; showProperties = true })
                        DropdownMenuItem(text = { Text("重命名") }, onClick = { selMenu = false; showRename = true })
                        DropdownMenuItem(text = { Text("隐藏") }, onClick = {
                            repo.hideMedia(selection)
                            selection = emptySet(); selMenu = false
                            toast("已隐藏")
                        })
                        if (selection.all { repo.hiddenMedia.value.contains(it) }) {
                            DropdownMenuItem(text = { Text("取消隐藏") }, onClick = {
                                repo.unhideMedia(selection)
                                selection = emptySet(); selMenu = false
                            })
                        }
                        DropdownMenuItem(text = { Text("复制到") }, onClick = { selMenu = false; showCopyMove = "复制到" to true })
                        DropdownMenuItem(text = { Text("移动到") }, onClick = { selMenu = false; showCopyMove = "移动到" to false })
                        DropdownMenuItem(text = { Text("创建快捷方式") }, onClick = { selMenu = false; toast("无法创建快捷方式") })
                        DropdownMenuItem(text = { Text("打开方式") }, onClick = { selMenu = false; toast("未找到可打开的应用") })
                        DropdownMenuItem(text = { Text("设置为") }, onClick = { selMenu = false; toast("未找到可设置的应用") })
                        DropdownMenuItem(text = { Text("调整大小") }, onClick = { selMenu = false; showResize = true })
                        DropdownMenuItem(text = { Text("编辑") }, onClick = {
                            selMenu = false
                            val first = selection.firstOrNull()
                            if (first != null) app.navigate(Screen.Editor(first))
                        })
                        val allFav = selection.all { repo.favorites.value.contains(it) }
                        DropdownMenuItem(
                            text = { Text(if (allFav && selection.isNotEmpty()) "取消收藏" else "添加到收藏") },
                            onClick = {
                                selection.forEach { repo.toggleFavorite(it) }
                                selection = emptySet(); selMenu = false
                            })
                        DropdownMenuItem(text = { Text("修复拍摄日期") }, onClick = {
                            selMenu = false; toast("已尝试修复拍摄日期")
                        })
                        DropdownMenuItem(text = { Text("全选") }, onClick = {
                            selection = media.map { it.id }.toSet(); selMenu = false
                        })
                    }
                }
            }
        }

        // content
        if (media.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("未找到任何项目。", color = Color.Gray)
            }
        } else {
            val cols = repo.columnsFolder.value
            val grouped = repo.groupMode.value != "不分组文件" && !isRecycleBin
            LazyVerticalGrid(
                columns = GridCells.Fixed(cols),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (grouped) {
                    val groups = media.groupBy { groupLabel(it, repo.groupMode.value) }
                    val ordered = if (repo.groupAsc.value) groups.toSortedMap() else groups.toSortedMap(reverseOrder())
                    ordered.forEach { (label, list) ->
                        item(key = "g_$label", span = { GridItemSpan(cols) }) {
                            Text(
                                label,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                        items(list, key = { it.id }) { item ->
                            MediaCell(
                                repo, item, selection.contains(item.id),
                                onClick = {
                                    if (selection.isNotEmpty()) {
                                        selection = if (selection.contains(item.id)) selection - item.id else selection + item.id
                                    } else {
                                        app.navigate(Screen.Viewer(folder, media.indexOf(item)))
                                    }
                                },
                                onLongClick = {
                                    selection = if (selection.contains(item.id)) selection - item.id else selection + item.id
                                }
                            )
                        }
                    }
                } else {
                    items(media, key = { it.id }) { item ->
                        MediaCell(
                            repo, item, selection.contains(item.id),
                            onClick = {
                                if (selection.isNotEmpty()) {
                                    selection = if (selection.contains(item.id)) selection - item.id else selection + item.id
                                } else {
                                    app.navigate(Screen.Viewer(folder, media.indexOf(item)))
                                }
                            },
                            onLongClick = {
                                selection = if (selection.contains(item.id)) selection - item.id else selection + item.id
                            }
                        )
                    }
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
    if (showColumns) ColumnsDialog(repo.columnsFolder.value, { showColumns = false }) { repo.setColumnsFolder(it); showColumns = false }
    if (showGroup) GroupDialog(repo.groupMode.value, repo.groupAsc.value, { showGroup = false }) { m, a ->
        repo.setGroup(m, a); showGroup = false
    }
    if (showNewFolder) NewFolderDialog({ showNewFolder = false }) {
        repo.createFolder(it); showNewFolder = false; toast("文件夹已创建")
    }
    if (showDelete) {
        val sel = repo.items.filter { selection.contains(it.id) }
        val size = sel.sumOf { it.size }
        DeleteConfirmDialog(
            text = if (isRecycleBin) "确定要永久删除 ${sel.size} 个项目吗？此操作无法撤销。"
            else "确定要将 ${sel.size} 个项目（${formatSize(size)}）移至回收站吗？",
            onDismiss = { showDelete = false },
            onConfirm = { skip ->
                repo.deleteMedia(selection, skip || isRecycleBin)
                selection = emptySet(); showDelete = false
            }
        )
    }
    if (showProperties) {
        val sel = repo.items.filter { selection.contains(it.id) }
        if (sel.size == 1) {
            PropertiesDialog(repo, sel.first()) { showProperties = false }
        } else {
            InfoDialog(
                "属性",
                "项目数：${sel.size}\n总大小：${formatSize(sel.sumOf { it.size })}"
            ) { showProperties = false }
        }
    }
    if (showRename && selection.size == 1) {
        val item = repo.items.firstOrNull { it.id == selection.first() }
        if (item != null) {
            RenameMediaDialog(item, { showRename = false }) { newName ->
                repo.renameMedia(item, newName)
                selection = emptySet(); showRename = false
            }
        } else showRename = false
    } else if (showRename) {
        toast("一次只能重命名一个文件"); showRename = false
    }
    showCopyMove?.let { (title, isCopy) ->
        CopyMoveDialog(
            title = title,
            folders = repo.folderNames(true, true).map { it.name },
            onDismiss = { showCopyMove = null },
            onPick = { target ->
                if (isCopy) {
                    val n = repo.copyMedia(selection, target)
                    toast("已复制 $n 个项目")
                } else {
                    repo.moveMedia(selection, target)
                    toast("已移动 ${selection.size} 个项目")
                }
                selection = emptySet(); showCopyMove = null
            }
        )
    }
    if (showResize && selection.size == 1) {
        val item = repo.items.firstOrNull { it.id == selection.first() }
        if (item != null) {
            ResizeDialog(item, { showResize = false }) { w, h ->
                repo.resizeMedia(item, w, h)
                selection = emptySet(); showResize = false
                toast("已调整大小")
            }
        } else showResize = false
    } else if (showResize) {
        toast("一次只能调整一个文件"); showResize = false
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaCell(
    repo: GalleryRepository,
    item: MediaItem,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        Modifier
            .aspectRatio(1f)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        ThumbImage(repo, item, Modifier.fillMaxSize())
        if (item.isVideo) {
            VideoDurationBadge(item.durationSec, Modifier.align(Alignment.BottomEnd).padding(4.dp))
        }
        if (repo.favorites.value.contains(item.id)) {
            StarBadge(Modifier.align(Alignment.TopEnd).padding(4.dp))
        }
        if (selected) {
            CheckBadge(Modifier.align(Alignment.TopStart).padding(4.dp))
        }
    }
}
