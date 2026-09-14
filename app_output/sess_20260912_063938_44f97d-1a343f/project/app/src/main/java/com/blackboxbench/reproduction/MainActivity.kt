package com.blackboxbench.reproduction

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 7)
        }
        setContent {
            val store = remember { FileStore(this) }
            BenchmarkAppTheme { MaterialFilesApp(store) }
        }
    }
}

@Composable
private fun MaterialFilesApp(store: FileStore) {
    var page by rememberSaveable { mutableStateOf("files") }
    val path = remember { mutableStateListOf<String>() }
    var drawerOpen by remember { mutableStateOf(false) }

    BackHandler(enabled = drawerOpen || page != "files" || path.isNotEmpty()) {
        when {
            drawerOpen -> drawerOpen = false
            page != "files" -> page = "files"
            path.isNotEmpty() -> path.removeAt(path.lastIndex)
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).statusBarsPadding()) {
        when (page) {
            "settings" -> SettingsScreen(
                onBack = { page = "files" },
                onStorage = { page = "storage" }
            )
            "storage" -> StorageScreen(
                onBack = { page = "settings" },
                onAddFtp = { page = "ftpAdd" }
            )
            "ftpAdd" -> FtpAddScreen(onBack = { page = "storage" })
            "ftpServer" -> FtpServerScreen(store, onBack = { page = "files" })
            "about" -> AboutScreen(onBack = { page = "files" })
            else -> FileBrowser(
                store = store,
                path = path,
                onMenu = { drawerOpen = true },
                onOpenStorage = { page = "storage" }
            )
        }

        if (drawerOpen) {
            FileDrawer(
                store = store,
                currentPath = path.toList(),
                onDismiss = { drawerOpen = false },
                onNavigate = {
                    path.clear()
                    path.addAll(it)
                    page = "files"
                    drawerOpen = false
                },
                onSettings = {
                    drawerOpen = false
                    page = "settings"
                },
                onStorage = {
                    drawerOpen = false
                    page = "storage"
                },
                onFtp = {
                    drawerOpen = false
                    page = "ftpServer"
                },
                onAbout = {
                    drawerOpen = false
                    page = "about"
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileBrowser(
    store: FileStore,
    path: MutableList<String>,
    onMenu: () -> Unit,
    onOpenStorage: () -> Unit
) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var sortOpen by remember { mutableStateOf(false) }
    var overflowOpen by remember { mutableStateOf(false) }
    var fabOpen by remember { mutableStateOf(false) }
    var createType by remember { mutableStateOf<String?>(null) }
    var renameNode by remember { mutableStateOf<FsNode?>(null) }
    var propertiesNode by remember { mutableStateOf<FsNode?>(null) }
    var compressIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var deleteIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var copyIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var copySource by remember { mutableStateOf<List<String>>(emptyList()) }
    var goToOpen by remember { mutableStateOf(false) }
    var itemMenu by remember { mutableStateOf<FsNode?>(null) }

    store.revision
    val current = store.resolve(path)
    val children = store.visibleChildren(path, query)

    LaunchedEffect(path.toList()) {
        itemMenu = null
        selectedIds = emptySet()
    }

    fun openNode(node: FsNode) {
        if (node.type == "dir" || node.type == "archive") {
            path.add(node.name)
            query = ""
        } else {
            Toast.makeText(context, "没有可用于打开此文件的应用", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            if (selectedIds.isNotEmpty()) {
                SelectionTopBar(
                    count = selectedIds.size,
                    onClose = { selectedIds = emptySet() },
                    onCopy = {
                        copyIds = selectedIds
                        copySource = path.toList()
                        selectedIds = emptySet()
                    },
                    onDelete = { deleteIds = selectedIds },
                    onCompress = { compressIds = selectedIds }
                )
            } else {
                FileTopBar(
                    title = if (searchOpen) "" else "文件",
                    summary = if (searchOpen) "" else summaryText(children),
                    searchOpen = searchOpen,
                    query = query,
                    onQuery = { query = it },
                    onMenu = onMenu,
                    onSearch = { searchOpen = true },
                    onExitSearch = {
                        searchOpen = false
                        query = ""
                    },
                    onSort = { sortOpen = true },
                    onOverflow = { overflowOpen = true },
                    sortOpen = sortOpen,
                    onDismissSort = { sortOpen = false },
                    overflowOpen = overflowOpen,
                    onDismissOverflow = { overflowOpen = false },
                    store = store,
                    onGoTo = { goToOpen = true },
                    onBookmark = {
                        store.addBookmark(path)
                        Toast.makeText(context, "已添加书签", Toast.LENGTH_SHORT).show()
                    },
                    onRefresh = {
                        store.updateSortMode(if (store.sortMode == "随机") "随机" else store.sortMode)
                        Toast.makeText(context, "已刷新", Toast.LENGTH_SHORT).show()
                    },
                    onStorage = onOpenStorage
                )
            }
        },
        floatingActionButton = {
            if (copyIds.isEmpty()) {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (fabOpen) {
                        ExtendedFloatingActionButton(
                            text = { Text("文件") },
                            icon = { Text("▤", fontSize = 22.sp) },
                            onClick = {
                                fabOpen = false
                                createType = "file"
                            }
                        )
                        ExtendedFloatingActionButton(
                            text = { Text("文件夹") },
                            icon = { Text("▰", fontSize = 22.sp) },
                            onClick = {
                                fabOpen = false
                                createType = "dir"
                            }
                        )
                    }
                    FloatingActionButton(onClick = { fabOpen = !fabOpen }) {
                        Text(if (fabOpen) "×" else "+", fontSize = 32.sp, fontWeight = FontWeight.Light)
                    }
                }
            }
        },
        bottomBar = {
            if (copyIds.isNotEmpty()) {
                CopyTargetBar(
                    count = copyIds.size,
                    onCancel = { copyIds = emptySet() },
                    onComplete = {
                        store.copyInto(path.toList(), copyIds, copySource)
                        copyIds = emptySet()
                        Toast.makeText(context, "复制完成", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            Breadcrumbs(path = path, rootLabel = "内部共享存储空间")
            if (children.isEmpty()) {
                EmptyFolder()
            } else if (store.gridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 92.dp)
                ) {
                    gridItems(children, key = { it.id }) { node ->
                        GridFileItem(
                            node = node,
                            selected = node.id in selectedIds,
                            onClick = {
                                if (selectedIds.isNotEmpty()) {
                                    selectedIds = toggleSet(selectedIds, node.id)
                                } else openNode(node)
                            },
                            onLongClick = { selectedIds = toggleSet(selectedIds, node.id) },
                            onMore = { itemMenu = node },
                            menuExpanded = itemMenu?.id == node.id,
                            onDismissMenu = { itemMenu = null },
                            onRename = { renameNode = node },
                            onDelete = { deleteIds = setOf(node.id) },
                            onCompress = { compressIds = setOf(node.id) },
                            onProperties = { propertiesNode = node },
                            onExtract = {
                                if (node.type == "archive") store.extract(path.toList(), node, true)
                            }
                        )
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 92.dp)) {
                    items(children, key = { it.id }) { node ->
                        ListFileItem(
                            node = node,
                            selected = node.id in selectedIds,
                            onClick = {
                                if (selectedIds.isNotEmpty()) selectedIds = toggleSet(selectedIds, node.id) else openNode(node)
                            },
                            onLongClick = { selectedIds = toggleSet(selectedIds, node.id) },
                            onMore = { itemMenu = node },
                            menuExpanded = itemMenu?.id == node.id,
                            onDismissMenu = { itemMenu = null },
                            onRename = { renameNode = node },
                            onDelete = { deleteIds = setOf(node.id) },
                            onCompress = { compressIds = setOf(node.id) },
                            onProperties = { propertiesNode = node },
                            onExtract = {
                                if (node.type == "archive") store.extract(path.toList(), node, true)
                            }
                        )
                    }
                }
            }
        }
    }

    if (createType != null) {
        NameDialog(
            title = if (createType == "dir") "新建文件夹" else "新建文件",
            label = "名称",
            initial = "",
            confirmText = "确定",
            onDismiss = { createType = null },
            onConfirm = {
                store.add(path.toList(), it, createType ?: "file")
                createType = null
            }
        )
    }

    renameNode?.let { node ->
        NameDialog(
            title = "重命名",
            label = "名称",
            initial = node.name,
            confirmText = "确定",
            onDismiss = { renameNode = null },
            onConfirm = {
                store.rename(path.toList(), node.id, it)
                renameNode = null
            }
        )
    }

    if (deleteIds.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { deleteIds = emptySet() },
            title = { Text("删除") },
            text = {
                val names = current.children.filter { it.id in deleteIds }.joinToString("、") { it.name }
                Text("确定要删除 $names 吗？此操作无法撤销。")
            },
            dismissButton = { TextButton(onClick = { deleteIds = emptySet() }) { Text("取消") } },
            confirmButton = {
                TextButton(onClick = {
                    store.delete(path.toList(), deleteIds)
                    selectedIds = emptySet()
                    deleteIds = emptySet()
                }) { Text("删除") }
            }
        )
    }

    if (compressIds.isNotEmpty()) {
        CompressDialog(
            initial = current.children.firstOrNull { it.id in compressIds }?.name.orEmpty(),
            onDismiss = { compressIds = emptySet() },
            onConfirm = {
                store.compress(path.toList(), compressIds, it)
                selectedIds = emptySet()
                compressIds = emptySet()
            }
        )
    }

    propertiesNode?.let { node ->
        PropertiesDialog(node = node, parentPath = store.displayPath(path), onDismiss = { propertiesNode = null })
    }

    if (goToOpen) {
        NameDialog(
            title = "转到",
            label = "路径",
            initial = store.displayPath(path),
            confirmText = "确定",
            onDismiss = { goToOpen = false },
            onConfirm = {
                val target = store.pathFromDisplay(it)
                if (store.resolve(target) != store.root || target.isEmpty()) {
                    path.clear()
                    path.addAll(target)
                    goToOpen = false
                } else {
                    Toast.makeText(context, "路径不存在", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (sortOpen) {
        SortDialog(store = store, onDismiss = { sortOpen = false })
    }

    if (overflowOpen) {
        GlobalActionsDialog(
            showHidden = store.showHidden,
            onDismiss = { overflowOpen = false },
            onUp = {
                if (path.isNotEmpty()) path.removeAt(path.lastIndex)
                overflowOpen = false
            },
            onGoTo = {
                overflowOpen = false
                goToOpen = true
            },
            onRefresh = {
                overflowOpen = false
                store.updateSortMode(if (store.sortMode == "随机") "随机" else store.sortMode)
                Toast.makeText(context, "已刷新", Toast.LENGTH_SHORT).show()
            },
            onHidden = {
                store.setHidden(!store.showHidden)
                overflowOpen = false
            },
            onBookmark = {
                store.addBookmark(path)
                overflowOpen = false
                Toast.makeText(context, "已添加书签", Toast.LENGTH_SHORT).show()
            },
            onStorage = {
                overflowOpen = false
                onOpenStorage()
            }
        )
    }

}

@Composable
private fun FileTopBar(
    title: String,
    summary: String,
    searchOpen: Boolean,
    query: String,
    onQuery: (String) -> Unit,
    onMenu: () -> Unit,
    onSearch: () -> Unit,
    onExitSearch: () -> Unit,
    onSort: () -> Unit,
    onOverflow: () -> Unit,
    sortOpen: Boolean,
    onDismissSort: () -> Unit,
    overflowOpen: Boolean,
    onDismissOverflow: () -> Unit,
    store: FileStore,
    onGoTo: () -> Unit,
    onBookmark: () -> Unit,
    onRefresh: () -> Unit,
    onStorage: () -> Unit
) {
    Surface(shadowElevation = 0.dp, color = MaterialTheme.colorScheme.surface) {
        Row(
            Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = if (searchOpen) onExitSearch else onMenu) {
                Text(if (searchOpen) "‹" else "☰", fontSize = 30.sp)
            }
            if (searchOpen) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQuery,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("搜索当前存储位置") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                )
            } else {
                Column(Modifier.weight(1f)) {
                    Text(title, fontSize = 24.sp)
                    Text(summary, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
                IconButton(onClick = onSearch) { Text("⌕", fontSize = 31.sp) }
                Box(
                    Modifier.size(48.dp).clickable { onSort() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("≡", fontSize = 30.sp)
                }
                Box(
                    Modifier.size(48.dp).clickable { onOverflow() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("⋮", fontSize = 30.sp)
                }
            }
        }
    }
}


@Composable
private fun SortDialog(store: FileStore, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("视图与排序") },
        text = {
            LazyColumn(modifier = Modifier.height(500.dp)) {
                item { SectionTitle("视图") }
                item {
                    SettingsRow("列表", if (!store.gridView) "已选择" else null, onClick = {
                        store.setGrid(false)
                        onDismiss()
                    })
                }
                item {
                    SettingsRow("网格", if (store.gridView) "已选择" else null, onClick = {
                        store.setGrid(true)
                        onDismiss()
                    })
                }
                item { HorizontalDivider() }
                item { SectionTitle("排序方式") }
                items(listOf("名称", "类型", "大小", "修改日期", "随机")) { mode ->
                    SettingsRow(mode, if (store.sortMode == mode) "已选择" else null, onClick = {
                        store.updateSortMode(mode)
                        onDismiss()
                    })
                }
                item { HorizontalDivider() }
                item {
                    Row(Modifier.fillMaxWidth().clickable { store.updateAscending(!store.ascending) }, verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(store.ascending, onCheckedChange = { store.updateAscending(it) })
                        Text("升序")
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth().clickable { store.updateFoldersFirst(!store.foldersFirst) }, verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(store.foldersFirst, onCheckedChange = { store.updateFoldersFirst(it) })
                        Text("文件夹优先")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } }
    )
}

@Composable
private fun GlobalActionsDialog(
    showHidden: Boolean,
    onDismiss: () -> Unit,
    onUp: () -> Unit,
    onGoTo: () -> Unit,
    onRefresh: () -> Unit,
    onHidden: () -> Unit,
    onBookmark: () -> Unit,
    onStorage: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("文件操作") },
        text = {
            LazyColumn(modifier = Modifier.height(480.dp)) {
                item { SettingsRow("新建窗口", onClick = onDismiss) }
                item { SettingsRow("上一级", onClick = onUp) }
                item { SettingsRow("转到", onClick = onGoTo) }
                item { SettingsRow("刷新", onClick = onRefresh) }
                item { SettingsRow("全选", onClick = onDismiss) }
                item {
                    Row(Modifier.fillMaxWidth().clickable(onClick = onHidden), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(showHidden, onCheckedChange = { onHidden() })
                        Text("显示隐藏文件")
                    }
                }
                item { SettingsRow("分享", onClick = onDismiss) }
                item { SettingsRow("复制路径", onClick = onDismiss) }
                item { SettingsRow("在终端中打开", onClick = onDismiss) }
                item { SettingsRow("添加书签", onClick = onBookmark) }
                item { SettingsRow("创建快捷方式", onClick = onDismiss) }
                item { SettingsRow("管理存储", onClick = onStorage) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

@Composable
private fun SelectionTopBar(
    count: Int,
    onClose: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onCompress: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().height(72.dp).background(MaterialTheme.colorScheme.surfaceContainer).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) { Text("×", fontSize = 28.sp) }
        Text(count.toString(), fontSize = 20.sp, modifier = Modifier.weight(1f))
        IconButton(onClick = {}) { Text("✂", fontSize = 22.sp) }
        IconButton(onClick = onCopy) { Text("▣", fontSize = 22.sp) }
        IconButton(onClick = onDelete) { Text("♲", fontSize = 25.sp) }
        Box {
            IconButton(onClick = onCompress) { Text("⋮", fontSize = 28.sp) }
        }
    }
}

@Composable
private fun CopyTargetBar(count: Int, onCancel: () -> Unit, onComplete: () -> Unit) {
    Surface(tonalElevation = 5.dp) {
        Row(
            Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) { Text("×", fontSize = 28.sp) }
            Text("复制 $count 项", modifier = Modifier.weight(1f), fontSize = 18.sp)
            TextButton(onClick = onComplete) { Text("✓", fontSize = 28.sp) }
        }
    }
}

@Composable
private fun Breadcrumbs(path: MutableList<String>, rootLabel: String) {
    LazyColumn(Modifier.fillMaxWidth().height(52.dp)) {
        item {
            Row(
                Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    rootLabel,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { path.clear() }.padding(vertical = 12.dp)
                )
                path.forEachIndexed { index, segment ->
                    Text("  ›  ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        segment,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            while (path.size > index + 1) path.removeAt(path.lastIndex)
                        }.padding(vertical = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyFolder() {
    Column(
        Modifier.fillMaxSize().padding(bottom = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        FolderGlyph(modifier = Modifier.size(96.dp), color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(20.dp))
        Text("空文件夹", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("这里还没有文件", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListFileItem(
    node: FsNode,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMore: () -> Unit,
    menuExpanded: Boolean,
    onDismissMenu: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onCompress: () -> Unit,
    onProperties: () -> Unit,
    onExtract: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth()
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 20.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FileGlyph(node, Modifier.size(46.dp))
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(node.name, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (node.type != "dir") {
                Text(formatSize(node.size) + " · " + node.mtime, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        ItemMenuButton(
            node, onMore, menuExpanded, onDismissMenu, onRename, onDelete,
            onCompress, onProperties, onExtract
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GridFileItem(
    node: FsNode,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMore: () -> Unit,
    menuExpanded: Boolean,
    onDismissMenu: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onCompress: () -> Unit,
    onProperties: () -> Unit,
    onExtract: () -> Unit
) {
    Column(
        Modifier.padding(4.dp)
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent, RoundedCornerShape(18.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(top = 18.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FileGlyph(node, Modifier.size(104.dp))
        Row(Modifier.fillMaxWidth().padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(node.name, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 16.sp)
            ItemMenuButton(
                node, onMore, menuExpanded, onDismissMenu, onRename, onDelete,
                onCompress, onProperties, onExtract
            )
        }
    }
}

@Composable
private fun ItemMenuButton(
    node: FsNode,
    onMore: () -> Unit,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onCompress: () -> Unit,
    onProperties: () -> Unit,
    onExtract: () -> Unit
) {
    Box(
        Modifier.size(48.dp).clickable { onMore() },
        contentAlignment = Alignment.Center
    ) {
        Text("⋮", fontSize = 25.sp)
        DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
            if (node.type != "dir") DropdownMenuItem(text = { Text("打开方式") }, onClick = onDismiss)
            listOf("剪切", "复制").forEach { item ->
                DropdownMenuItem(text = { Text(item) }, onClick = onDismiss)
            }
            DropdownMenuItem(text = { Text("删除") }, onClick = { onDismiss(); onDelete() })
            DropdownMenuItem(text = { Text("重命名") }, onClick = { onDismiss(); onRename() })
            DropdownMenuItem(text = { Text("压缩") }, onClick = { onDismiss(); onCompress() })
            if (node.type == "archive") {
                DropdownMenuItem(text = { Text("解压全部") }, onClick = { onDismiss(); onExtract() })
            }
            listOf("分享", "复制路径", "创建快捷方式").forEach { item ->
                DropdownMenuItem(text = { Text(item) }, onClick = onDismiss)
            }
            DropdownMenuItem(text = { Text("属性") }, onClick = { onDismiss(); onProperties() })
        }
    }
}

@Composable
private fun FileGlyph(node: FsNode, modifier: Modifier = Modifier) {
    when (node.type) {
        "dir" -> FolderGlyph(modifier, MaterialTheme.colorScheme.onSurfaceVariant)
        "archive" -> Box(modifier.background(Color(0xFFE9D8B4), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            Text("ZIP", fontWeight = FontWeight.Bold, color = Color(0xFF725B24))
        }
        else -> Box(modifier.background(fileColor(node.name), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            Text(fileExtension(node.name), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun FolderGlyph(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier) {
        val top = size.height * 0.24f
        val bodyTop = size.height * 0.34f
        drawRoundRect(color = color, topLeft = Offset(0f, bodyTop), size = Size(size.width, size.height * 0.62f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.08f))
        val tab = Path().apply {
            moveTo(size.width * 0.08f, top)
            lineTo(size.width * 0.42f, top)
            lineTo(size.width * 0.55f, bodyTop + 2f)
            lineTo(size.width * 0.08f, bodyTop + 2f)
            close()
        }
        drawPath(tab, color)
    }
}

@Composable
private fun NameDialog(
    title: String,
    label: String,
    initial: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (text.isNotBlank()) onConfirm(text) })
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        confirmButton = { TextButton(enabled = text.isNotBlank(), onClick = { onConfirm(text) }) { Text(confirmText) } }
    )
}

@Composable
private fun CompressDialog(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    var format by remember { mutableStateOf(".zip") }
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("创建压缩文件") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") }, singleLine = true)
                listOf(".zip", ".tar.xz", ".7z").forEach { option ->
                    Row(Modifier.fillMaxWidth().clickable { format = option }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = format == option, onClick = { format = option })
                        Text(option)
                    }
                }
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码（可选）") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        confirmButton = {
            TextButton(onClick = {
                val base = name.ifBlank { "压缩文件" }.substringBeforeLast('.', name.ifBlank { "压缩文件" })
                onConfirm(base + format)
            }) { Text("确定") }
        }
    )
}

@Composable
private fun PropertiesDialog(node: FsNode, parentPath: String, onDismiss: () -> Unit) {
    var tab by remember { mutableStateOf(0) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FileGlyph(node, Modifier.size(48.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(node.name, fontSize = 21.sp, modifier = Modifier.weight(1f), maxLines = 2)
                    IconButton(onClick = onDismiss) { Text("×", fontSize = 26.sp) }
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("基本", "权限", "校验和").forEachIndexed { index, label ->
                        TextButton(onClick = { tab = index }) {
                            Text(label, fontWeight = if (tab == index) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                when (tab) {
                    0 -> {
                        PropertyLine("名称", node.name)
                        PropertyLine("父路径", parentPath)
                        PropertyLine("类型", mimeType(node))
                        PropertyLine("大小", formatSize(node.size))
                        PropertyLine("修改时间", node.mtime)
                    }
                    1 -> {
                        PropertyLine("所有者", "u0_a123")
                        PropertyLine("用户组", "ext_data_rw")
                        PropertyLine("模式", "rw-rw---- (0660)")
                        PropertyLine("SELinux 上下文", "u:object_r:media_rw_data_file:s0")
                    }
                    else -> {
                        PropertyLine("CRC32", "00000000")
                        PropertyLine("MD5", "d41d8cd98f00b204e9800998ecf8427e")
                        PropertyLine("SHA-1", "da39a3ee5e6b4b0d3255bfef95601890")
                        PropertyLine("SHA-256", "e3b0c44298fc1c149afbf4c8996fb924")
                    }
                }
            }
        }
    }
}

@Composable
private fun PropertyLine(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 15.sp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileDrawer(
    store: FileStore,
    currentPath: List<String>,
    onDismiss: () -> Unit,
    onNavigate: (List<String>) -> Unit,
    onSettings: () -> Unit,
    onStorage: () -> Unit,
    onFtp: () -> Unit,
    onAbout: () -> Unit
) {
    var editBookmark by remember { mutableStateOf<Bookmark?>(null) }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.56f)).clickable(onClick = onDismiss))
        Surface(
            modifier = Modifier.fillMaxHeight().fillMaxWidth(0.80f),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            LazyColumn(contentPadding = PaddingValues(top = 18.dp, bottom = 24.dp)) {
                item { DrawerRow("▯", "Root", "67.2 MB 可用，共 0.98 GB", false) { onNavigate(emptyList()) } }
                item { DrawerRow("▣", "内部共享存储空间", "9.52 GB 可用，共 10.41 GB", currentPath.isEmpty()) { onNavigate(emptyList()) } }
                item { DrawerRow("+", "添加存储…", null, false, onStorage) }
                item { HorizontalDivider(Modifier.padding(horizontal = 28.dp, vertical = 8.dp)) }
                item { DrawerRow("▧", "DCIM", null, false) { onNavigate(listOf("DCIM")) } }
                item { DrawerRow("⇩", "下载", null, false) { onNavigate(listOf("Download")) } }
                item { DrawerRow("▤", "电影", null, false) { onNavigate(listOf("Movies")) } }
                item { DrawerRow("♪", "音乐", null, false) { onNavigate(listOf("Music")) } }
                item { DrawerRow("▧", "图片", null, false) { onNavigate(listOf("Pictures")) } }
                item { HorizontalDivider(Modifier.padding(horizontal = 28.dp, vertical = 8.dp)) }
                items(store.bookmarks, key = { it.path }) { bookmark ->
                    Row(
                        Modifier.fillMaxWidth().combinedClickable(
                            onClick = { onNavigate(store.pathFromDisplay(bookmark.path)) },
                            onLongClick = { editBookmark = bookmark }
                        ).padding(horizontal = 26.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("□", fontSize = 28.sp, modifier = Modifier.width(44.dp))
                        Text(bookmark.name, fontSize = 17.sp)
                    }
                }
                item { HorizontalDivider(Modifier.padding(horizontal = 28.dp, vertical = 8.dp)) }
                item { DrawerRow("▱", "FTP 服务器", null, false, onFtp) }
                item { DrawerRow("⚙", "设置", null, false, onSettings) }
                item { DrawerRow("?", "关于", null, false, onAbout) }
            }
        }
    }

    editBookmark?.let { bookmark ->
        var name by remember(bookmark) { mutableStateOf(bookmark.name) }
        AlertDialog(
            onDismissRequest = { editBookmark = null },
            title = { Text("书签文件夹") },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") }, singleLine = true)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = bookmark.path, onValueChange = {}, readOnly = true, label = { Text("路径") })
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { store.removeBookmark(bookmark); editBookmark = null }) { Text("移除") }
                    TextButton(onClick = { editBookmark = null }) { Text("取消") }
                }
            },
            confirmButton = {
                TextButton(onClick = { store.renameBookmark(bookmark, name); editBookmark = null }) { Text("确定") }
            }
        )
    }
}

@Composable
private fun DrawerRow(icon: String, title: String, subtitle: String?, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .padding(horizontal = 12.dp)
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent, RoundedCornerShape(28.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 27.sp, modifier = Modifier.width(46.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column {
            Text(title, fontSize = 17.sp)
            if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsScreen(onBack: () -> Unit, onStorage: () -> Unit) {
    var material3 by rememberSaveable { mutableStateOf(true) }
    var blackNight by rememberSaveable { mutableStateOf(false) }
    var animations by rememberSaveable { mutableStateOf(true) }
    var remotePreview by rememberSaveable { mutableStateOf(true) }
    var languageDialog by remember { mutableStateOf(false) }
    var language by rememberSaveable { mutableStateOf("系统默认") }
    var nightDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        SimpleTopBar("设置", onBack)
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            item { SectionTitle("界面") }
            item { SettingsRow("语言", language, onClick = { languageDialog = true }) }
            item { SettingsRow("主题颜色", "应用中最常出现的颜色", trailing = { ColorDot() }) }
            item { SwitchRow("Material Design 3", material3) { material3 = it } }
            item { SettingsRow("夜间模式", "跟随系统", onClick = { nightDialog = true }) }
            item { SwitchRow("纯黑夜间模式", blackNight) { blackNight = it } }
            item { SwitchRow("文件列表动画", animations) { animations = it } }
            item { SettingsRow("长文件名显示方式", "显示开头和结尾") }
            item { HorizontalDivider() }
            item { SectionTitle("行为") }
            item { SettingsRow("默认文件夹", "内部共享存储空间") }
            item { SettingsRow("存储空间", "Root 和内部共享存储空间", onClick = onStorage) }
            item { SettingsRow("标准文件夹", "DCIM、下载、电影、音乐和图片") }
            item { SettingsRow("收藏文件夹", "文档和下载") }
            item { SettingsRow("Root 访问模式", "自动") }
            item { SettingsRow("归档文件名编码", "UTF-8") }
            item { SettingsRow("打开 Android 安装包", "每次询问") }
            item { SwitchRow("预读远程文件以生成预览", remotePreview) { remotePreview = it } }
        }
    }

    if (languageDialog) {
        AlertDialog(
            onDismissRequest = { languageDialog = false },
            title = { Text("语言") },
            text = {
                Column {
                    listOf("系统默认", "简体中文（中国）", "Čeština (Česko)", "English (United States)").forEach { option ->
                        Row(Modifier.fillMaxWidth().clickable { language = option; languageDialog = false }, verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = language == option, onClick = { language = option; languageDialog = false })
                            Text(option)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (nightDialog) {
        AlertDialog(
            onDismissRequest = { nightDialog = false },
            title = { Text("夜间模式") },
            text = {
                Column {
                    listOf("跟随系统", "始终开启", "始终关闭").forEach { option ->
                        Row(Modifier.fillMaxWidth().clickable { nightDialog = false }, verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = option == "跟随系统", onClick = { nightDialog = false })
                            Text(option)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun StorageScreen(onBack: () -> Unit, onAddFtp: () -> Unit) {
    var addOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        SimpleTopBar("存储空间", onBack)
        StorageRow("▯", "Root", "/")
        StorageRow("▣", "内部共享存储空间", "/storage/emulated/0")
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.End) {
            FloatingActionButton(onClick = { addOpen = true }) { Text("+", fontSize = 30.sp) }
        }
    }

    if (addOpen) {
        AlertDialog(
            onDismissRequest = { addOpen = false },
            title = { Text("添加存储") },
            text = {
                Column {
                    listOf("Android/data", "Android/obb", "外部存储", "FTP 服务器", "SFTP 服务器", "SMB 服务器", "WebDAV 服务器").forEach { option ->
                        Text(
                            option,
                            fontSize = 17.sp,
                            modifier = Modifier.fillMaxWidth().clickable {
                                addOpen = false
                                if (option == "FTP 服务器") onAddFtp()
                            }.padding(vertical = 14.dp)
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun StorageRow(icon: String, title: String, path: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 28.sp, modifier = Modifier.width(52.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 19.sp)
            Text(path, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("≡", fontSize = 26.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FtpAddScreen(onBack: () -> Unit) {
    var host by rememberSaveable { mutableStateOf("") }
    var port by rememberSaveable { mutableStateOf("21") }
    var path by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var protocol by rememberSaveable { mutableStateOf("FTP") }
    var protocolOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(Modifier.fillMaxSize()) {
        SimpleTopBar("添加 FTP 服务器", onBack)
        Column(Modifier.weight(1f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(host, { host = it }, label = { Text("主机名") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(port, { port = it }, label = { Text("端口") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(path, { path = it }, label = { Text("路径") }, placeholder = { Text("可以留空") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(name, { name = it }, label = { Text("名称") }, placeholder = { Text("使用主机名") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Box {
                OutlinedTextField(
                    protocol,
                    {},
                    label = { Text("协议") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().clickable { protocolOpen = true },
                    trailingIcon = { Text("▾", modifier = Modifier.clickable { protocolOpen = true }.padding(12.dp)) }
                )
                DropdownMenu(expanded = protocolOpen, onDismissRequest = { protocolOpen = false }, modifier = Modifier.fillMaxWidth(0.88f)) {
                    listOf("FTP", "FTPS", "FTPES").forEach { option ->
                        DropdownMenuItem(text = { Text(option) }, onClick = { protocol = option; protocolOpen = false })
                    }
                }
            }
        }
        HorizontalDivider()
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { Toast.makeText(context, "服务器已添加", Toast.LENGTH_SHORT).show(); onBack() }) { Text("添加") }
            TextButton(onClick = onBack) { Text("取消") }
            TextButton(onClick = {
                Toast.makeText(context, if (host.isBlank()) "请输入主机名" else "连接成功并已添加", Toast.LENGTH_SHORT).show()
                if (host.isNotBlank()) onBack()
            }) { Text("连接并添加") }
        }
    }
}

@Composable
private fun FtpServerScreen(store: FileStore, onBack: () -> Unit) {
    var running by rememberSaveable { mutableStateOf(false) }
    var anonymous by rememberSaveable { mutableStateOf(true) }
    var username by rememberSaveable { mutableStateOf("admin") }
    var password by rememberSaveable { mutableStateOf("") }
    var usernameDialog by remember { mutableStateOf(false) }
    var passwordDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        SimpleTopBar("FTP 服务器", onBack)
        LazyColumn {
            item {
                SwitchRow(if (running) "正在运行" else "未启动", running) { running = it }
            }
            item { SettingsRow("地址", if (anonymous) "ftp://10.0.2.16:2121" else "ftp://" + username + "@10.0.2.16:2121") }
            item { SettingsRow("添加到快速设置", "在系统快捷设置中显示") }
            item { SectionTitle("登录") }
            item { SwitchRow("匿名登录", anonymous, enabled = !running) { anonymous = it } }
            item { SettingsRow("用户名", username, enabled = !anonymous && !running, onClick = { if (!anonymous && !running) usernameDialog = true }) }
            item { SettingsRow("密码", if (password.isBlank()) "未设置" else "••••••", enabled = !anonymous && !running, onClick = { if (!anonymous && !running) passwordDialog = true }) }
            item { SectionTitle("服务器") }
            item { SettingsRow("端口", "2121", enabled = !running) }
            item { SettingsRow("根文件夹", "内部共享存储空间", enabled = !running) }
            item { SwitchRow("允许写入", true, enabled = !running) {} }
        }
    }

    if (usernameDialog) NameDialog("用户名", "用户名", username, "确定", { usernameDialog = false }) {
        username = it
        usernameDialog = false
    }
    if (passwordDialog) {
        var value by remember { mutableStateOf(password) }
        AlertDialog(
            onDismissRequest = { passwordDialog = false },
            title = { Text("密码") },
            text = {
                OutlinedTextField(value, { value = it }, label = { Text("密码") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
            },
            dismissButton = { TextButton(onClick = { passwordDialog = false }) { Text("取消") } },
            confirmButton = { TextButton(onClick = { password = value; passwordDialog = false }) { Text("确定") } }
        )
    }
}

@Composable
private fun AboutScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        SimpleTopBar("关于", onBack)
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(22.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(50), color = Color(0xFF1976E9), modifier = Modifier.size(52.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text("▰", color = Color.White, fontSize = 27.sp) }
                    }
                    Spacer(Modifier.width(16.dp))
                    Text("Material Files", fontSize = 28.sp)
                }
                Spacer(Modifier.height(18.dp))
                AboutRow("ⓘ", "版本", "1.7.4 (39)")
                AboutRow("◉", "在 GitHub 上查看")
                AboutRow("▤", "许可证")
                AboutRow("▣", "隐私政策")
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(22.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("作者", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                AboutRow("♙", "Hai Zhang")
                AboutRow("◉", "在 GitHub 上关注")
                AboutRow("♥", "在 Twitter 上关注")
            }
        }
    }
}

@Composable
private fun AboutRow(icon: String, title: String, subtitle: String? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 24.sp, modifier = Modifier.width(50.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column {
            Text(title, fontSize = 17.sp)
            if (subtitle != null) Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SimpleTopBar(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Text("‹", fontSize = 38.sp) }
        Text(title, fontSize = 25.sp)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.fillMaxWidth().padding(start = 18.dp, top = 22.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String? = null,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 17.sp, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
            if (subtitle != null) Text(subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.38f))
        }
        trailing?.invoke()
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled) { onChange(!checked) }.padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 17.sp, modifier = Modifier.weight(1f), color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
private fun ColorDot() {
    Canvas(Modifier.size(42.dp)) { drawCircle(Color(0xFFB8D0F7)) }
}

@Composable
private fun MenuLabel(text: String) {
    Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
}

@Composable
private fun MenuChoice(text: String, selected: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(if (selected) "●" else "○", color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(28.dp))
        Text(text)
    }
}

@Composable
private fun CheckMenu(text: String, checked: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text(text)
    }
}

private fun toggleSet(source: Set<String>, id: String): Set<String> =
    if (id in source) source - id else source + id

private fun summaryText(children: List<FsNode>): String {
    val folders = children.count { it.type == "dir" }
    val files = children.size - folders
    return folders.toString() + " 个文件夹，" + files + " 个文件"
}

private fun fileExtension(name: String): String {
    val ext = name.substringAfterLast('.', "FILE").uppercase()
    return ext.take(4)
}

private fun fileColor(name: String): Color = when (name.substringAfterLast('.', "").lowercase()) {
    "png", "jpg", "jpeg" -> Color(0xFF4E7D55)
    "pdf" -> Color(0xFFB3261E)
    "apk" -> Color(0xFF3E7A54)
    "md", "txt" -> Color(0xFF536D8A)
    else -> Color(0xFF74777F)
}

private fun formatSize(bytes: Long): String = when {
    bytes <= 0 -> "0 B"
    bytes < 1024 -> bytes.toString() + " B"
    bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
    else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
}

private fun mimeType(node: FsNode): String = when {
    node.type == "dir" -> "文件夹"
    node.type == "archive" -> "application/zip"
    node.name.endsWith(".txt") -> "text/plain"
    node.name.endsWith(".md") -> "text/markdown"
    node.name.endsWith(".pdf") -> "application/pdf"
    node.name.endsWith(".png") -> "image/png"
    node.name.endsWith(".apk") -> "application/vnd.android.package-archive"
    else -> "application/octet-stream"
}
