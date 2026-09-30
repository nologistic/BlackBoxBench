package com.blackboxbench.reproduction

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

sealed interface FileDialog {
    data object NewFolder : FileDialog
    data object NewFile : FileDialog
    data class Rename(val node: FsNode) : FileDialog
    data class Delete(val nodes: List<FsNode>) : FileDialog
    data class Compress(val nodes: List<FsNode>) : FileDialog
    data class Properties(val node: FsNode) : FileDialog
    data object GoTo : FileDialog
    data class OpenWith(val node: FsNode) : FileDialog
}

@Composable
fun FilesScreen(model: AppModel) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var dialog by remember { mutableStateOf<FileDialog?>(null) }
    var sortMenu by remember { mutableStateOf(false) }
    var overflowMenu by remember { mutableStateOf(false) }
    var selectionMenu by remember { mutableStateOf(false) }

    val canGoBack = model.selection.isNotEmpty() || model.searchActive ||
        model.path.size > 1 || model.errorText != null
    BackHandler(enabled = canGoBack) {
        when {
            model.selection.isNotEmpty() -> model.clearSelection()
            model.searchActive -> { model.searchActive = false; model.query = "" }
            else -> model.goUp()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Surface,
                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                modifier = Modifier.width(300.dp),
            ) {
                AppDrawerContent(
                    model = model,
                    onOpenFolder = { node ->
                        model.goTo(node)
                        scope.launch { drawerState.close() }
                    },
                    onScreen = { screen ->
                        model.screen = screen
                        scope.launch { drawerState.close() }
                    },
                )
            }
        },
    ) {
        Box(Modifier.fillMaxSize().background(Surface)) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                FilesAppBar(
                    model = model,
                    onNavigationClick = { scope.launch { drawerState.open() } },
                    onSortClick = { sortMenu = true },
                    onOverflowClick = { overflowMenu = true },
                    onSelectionMenuClick = { selectionMenu = true },
                    onDeleteSelection = {
                        if (model.selection.isNotEmpty()) dialog = FileDialog.Delete(model.selection.toList())
                    },
                )
                SortMenu(model, sortMenu) { sortMenu = false }
                FolderOverflowMenu(
                    model = model,
                    expanded = overflowMenu,
                    onClose = { overflowMenu = false },
                    onDialog = { dialog = it },
                )
                SelectionMenu(
                    model = model,
                    expanded = selectionMenu,
                    onClose = { selectionMenu = false },
                    onDialog = { dialog = it },
                )

                val items = model.items()
                Box(Modifier.fillMaxSize()) {
                    when {
                        model.errorText != null -> ErrorState(model.errorText!!)
                        items.isEmpty() && !model.searchActive -> EmptyState()
                        model.grid -> FileGrid(model, items, onDialog = { dialog = it })
                        else -> FileListColumn(model, items, onDialog = { dialog = it })
                    }
                    FabArea(model, onDialog = { dialog = it })
                    ClipboardBar(model, Modifier.align(Alignment.BottomCenter))
                }
            }
        }
    }

    FileDialogHost(model, dialog) { dialog = null }
}

@Composable
private fun FileListColumn(
    model: AppModel,
    items: List<FsNode>,
    onDialog: (FileDialog) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(items, key = { System.identityHashCode(it) }) { node ->
            FileRowItem(
                node = node,
                selected = model.selection.contains(node),
                onClick = {
                    if (model.selection.isNotEmpty()) {
                        model.toggleSelection(node)
                    } else if (node.canEnter) {
                        model.enter(node)
                    } else {
                        onDialog(FileDialog.OpenWith(node))
                    }
                },
                onLongClick = { model.toggleSelection(node) },
                onAction = { action -> handleItemAction(model, node, action, onDialog) },
            )
        }
    }
}

@Composable
private fun FileGrid(
    model: AppModel,
    items: List<FsNode>,
    onDialog: (FileDialog) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items, key = { System.identityHashCode(it) }) { node ->
            GridCard(
                node = node,
                selected = model.selection.contains(node),
                onClick = {
                    if (model.selection.isNotEmpty()) model.toggleSelection(node)
                    else if (node.canEnter) model.enter(node)
                    else onDialog(FileDialog.OpenWith(node))
                },
                onLongClick = { model.toggleSelection(node) },
                onAction = { action -> handleItemAction(model, node, action, onDialog) },
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun GridCard(
    node: FsNode,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onAction: (ItemAction) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Column(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) PrimaryContainer.copy(alpha = 0.6f) else SurfaceContainer.copy(alpha = 0.5f))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(6.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(110.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (node.isDir) {
                FolderGlyph(Modifier.size(64.dp), OnSurfaceVariant)
            } else {
                FileTypeIcon(node, Modifier.size(if (node.kind == FileKind.IMAGE || node.kind == FileKind.VIDEO) 96.dp else 56.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                when (node.kind) {
                    FileKind.DIR -> FolderGlyph(Modifier.size(18.dp), OnSurfaceVariant)
                    FileKind.AUDIO -> AudioGlyph(Modifier.size(16.dp), Color(0xFF7B4EA8))
                    FileKind.IMAGE -> ImageThumb(Modifier.size(18.dp))
                    FileKind.VIDEO -> VideoThumb(Modifier.size(18.dp))
                    FileKind.ARCHIVE -> ZipGlyph(Modifier.size(18.dp))
                    FileKind.APK -> ApkGlyph(Modifier.size(16.dp))
                    else -> DocGlyph(Modifier.size(16.dp), Color(0xFF3A6EA5))
                }
            }
            Spacer(Modifier.width(6.dp))
            Text(
                node.name,
                fontSize = 13.sp,
                color = OnSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Box {
                Box(
                    Modifier.size(32.dp).clickable { menuOpen = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.MoreVert, null, tint = OnSurfaceVariant, modifier = Modifier.size(18.dp))
                }
                ItemMenuContent(node, menuOpen, onClose = { menuOpen = false }) { menuOpen = false; onAction(it) }
            }
        }
    }
}

private fun handleItemAction(
    model: AppModel,
    node: FsNode,
    action: ItemAction,
    onDialog: (FileDialog) -> Unit,
) {
    when (action) {
        ItemAction.OPEN_WITH -> onDialog(FileDialog.OpenWith(node))
        ItemAction.CUT -> { model.selection.clear(); model.selection.add(node); model.copySelection(ClipMode.CUT) }
        ItemAction.COPY -> { model.selection.clear(); model.selection.add(node); model.copySelection(ClipMode.COPY) }
        ItemAction.DELETE -> onDialog(FileDialog.Delete(listOf(node)))
        ItemAction.RENAME -> onDialog(FileDialog.Rename(node))
        ItemAction.COMPRESS -> onDialog(FileDialog.Compress(listOf(node)))
        ItemAction.EXTRACT, ItemAction.EXTRACT_ALL -> {
            if (node.isArchive) {
                model.enter(node)
            }
        }
        ItemAction.SHARE -> Unit
        ItemAction.COPY_PATH -> Unit
        ItemAction.BOOKMARK -> model.addBookmark(node)
        ItemAction.SHORTCUT -> Unit
        ItemAction.PROPERTIES -> onDialog(FileDialog.Properties(node))
        ItemAction.OPEN -> Unit
    }
}

@Composable
private fun FilesAppBar(
    model: AppModel,
    onNavigationClick: () -> Unit,
    onSortClick: () -> Unit,
    onOverflowClick: () -> Unit,
    onSelectionMenuClick: () -> Unit,
    onDeleteSelection: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().background(Surface)) {
        Spacer(Modifier.height(14.dp))
        if (model.searchActive) {
            Row(
                Modifier.fillMaxWidth().height(64.dp).padding(start = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIconButton(Icons.Filled.ArrowBack, "返回") {
                    model.searchActive = false
                    model.query = ""
                }
                val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
                Box(Modifier.weight(1f)) {
                    if (model.query.isEmpty()) {
                        Text("搜索…", fontSize = 18.sp, color = OnSurfaceVariant)
                    }
                    BasicTextField(
                        value = model.query,
                        onValueChange = { model.query = it },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp, color = OnSurface),
                        cursorBrush = SolidColor(Primary),
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    )
                }
                if (model.query.isNotEmpty()) {
                    AppIconButton(Icons.Filled.Clear, "清除") { model.query = "" }
                }
                AppIconButton(Icons.Filled.MoreVert, "更多") { }
            }
        } else if (model.selection.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().height(64.dp).padding(start = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIconButton(Icons.Filled.Close, "取消选择") { model.clearSelection() }
                Text(
                    "${model.selection.size}",
                    fontSize = 22.sp,
                    color = OnSurface,
                    modifier = Modifier.weight(1f),
                )
                GlyphIconButton("剪切", { model.copySelection(ClipMode.CUT) }) {
                    CutGlyph(Modifier.size(22.dp), OnSurfaceVariant)
                }
                GlyphIconButton("复制", { model.copySelection(ClipMode.COPY) }) {
                    CopyGlyph(Modifier.size(22.dp), OnSurfaceVariant)
                }
                GlyphIconButton("删除", { onDeleteSelection() }) {
                    DeleteTrashGlyph(Modifier.size(22.dp), OnSurfaceVariant)
                }
                AppIconButton(Icons.Filled.MoreVert, "更多") { onSelectionMenuClick() }
            }
        } else {
            Row(
                Modifier.fillMaxWidth().height(64.dp).padding(start = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (model.path.size > 1) {
                    AppIconButton(Icons.Filled.ArrowBack, "返回") { model.goUp() }
                } else {
                    AppIconButton(Icons.Filled.Menu, "菜单") { onNavigationClick() }
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    "文件",
                    fontSize = 22.sp,
                    color = OnSurface,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                AppIconButton(Icons.Filled.Search, "搜索") {
                    model.searchActive = true
                    model.query = ""
                }
                GlyphIconButton("排序", { onSortClick() }) {
                    SortGlyph(Modifier.size(24.dp), OnSurfaceVariant)
                }
                AppIconButton(Icons.Filled.MoreVert, "更多") { onOverflowClick() }
            }
        }

        val subtitle = if (model.errorText != null) "错误" else model.subtitle()
        val subtitleColor = if (model.errorText != null) MaterialError else OnSurfaceVariant
        Box(Modifier.fillMaxWidth().height(20.dp)) {
            if (!model.searchActive) {
                Text(
                    subtitle,
                    fontSize = 13.sp,
                    color = subtitleColor,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 66.dp),
                )
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(start = if (model.path.size == 1 && model.breadcrumbExtra == null) 66.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val path = model.path
            val visible = if (path.size > 3) path.subList(path.size - 3, path.size) else path
            val truncated = path.size > 3
            visible.forEachIndexed { index, node ->
                if (index > 0 || truncated) {
                    Box(Modifier.padding(horizontal = 4.dp)) { ChevronGlyph(Modifier.size(14.dp), OnSurfaceVariant) }
                }
                Text(
                    node.name,
                    fontSize = 14.sp,
                    color = OnSurface,
                    fontWeight = if (index == visible.size - 1 && model.breadcrumbExtra == null) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clickable { model.goTo(node) }
                        .padding(horizontal = 2.dp),
                )
            }
            model.breadcrumbExtra?.let { extra ->
                Box(Modifier.padding(horizontal = 4.dp)) { ChevronGlyph(Modifier.size(14.dp), OnSurfaceVariant) }
                Text(
                    extra,
                    fontSize = 14.sp,
                    color = OnSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private val MaterialError = ErrorColor

@Composable
private fun SortMenu(model: AppModel, expanded: Boolean, onClose: () -> Unit) {
    DropdownMenu(expanded = expanded, onDismissRequest = onClose) {
        SortOption("列表", model.grid.not()) { model.grid = false }
        SortOption("网格", model.grid) { model.grid = true }
        Spacer(Modifier.height(4.dp))
        SortOption("名称", model.sortBy == SortBy.NAME) { model.sortBy = SortBy.NAME }
        SortOption("类型", model.sortBy == SortBy.TYPE) { model.sortBy = SortBy.TYPE }
        SortOption("大小", model.sortBy == SortBy.SIZE) { model.sortBy = SortBy.SIZE }
        SortOption("最后修改", model.sortBy == SortBy.MTIME) { model.sortBy = SortBy.MTIME }
        Spacer(Modifier.height(4.dp))
        CheckOption("升序", model.ascending) { model.ascending = it }
        CheckOption("文件夹优先", model.foldersFirst) { model.foldersFirst = it }
        Spacer(Modifier.height(4.dp))
        CheckOption("仅用于此文件夹", model.onlyThisFolder) { model.onlyThisFolder = it }
    }
}

@Composable
private fun SortOption(label: String, selected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, fontSize = 15.sp) },
        trailingIcon = { RadioButton(selected = selected, onClick = onClick) },
        onClick = onClick,
    )
}

@Composable
private fun CheckOption(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    DropdownMenuItem(
        text = { Text(label, fontSize = 15.sp) },
        trailingIcon = { Checkbox(checked = checked, onCheckedChange = onChange) },
        onClick = { onChange(!checked) },
    )
}

@Composable
private fun FolderOverflowMenu(
    model: AppModel,
    expanded: Boolean,
    onClose: () -> Unit,
    onDialog: (FileDialog) -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onClose) {
        SimpleDropdownItem("新建窗口") { onClose() }
        SimpleDropdownItem("向上") { model.goUp(); onClose() }
        SimpleDropdownItem("转到…") { onDialog(FileDialog.GoTo); onClose() }
        SimpleDropdownItem("刷新") { onClose() }
        SimpleDropdownItem("全选") { model.selectAll(); onClose() }
        DropdownMenuItem(
            text = { Text("显示隐藏文件", fontSize = 15.sp) },
            trailingIcon = { Checkbox(checked = model.showHidden, onCheckedChange = { model.showHidden = it }) },
            onClick = { model.showHidden = !model.showHidden },
        )
        SimpleDropdownItem("分享") { onClose() }
        SimpleDropdownItem("复制路径") { onClose() }
        SimpleDropdownItem("在终端中打开") { onClose() }
        SimpleDropdownItem("添加书签") { model.addBookmark(model.current); onClose() }
        SimpleDropdownItem("创建快捷方式") { onClose() }
    }
}

@Composable
private fun SelectionMenu(
    model: AppModel,
    expanded: Boolean,
    onClose: () -> Unit,
    onDialog: (FileDialog) -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onClose) {
        SimpleDropdownItem("压缩") { onDialog(FileDialog.Compress(model.selection.toList())); onClose() }
        SimpleDropdownItem("分享") { onClose() }
        SimpleDropdownItem("全选") { model.selectAll(); onClose() }
    }
}

@Composable
private fun ClipboardBar(model: AppModel, modifier: Modifier = Modifier) {
    val clip = model.clip ?: return
    Row(
        modifier
            .fillMaxWidth()
            .background(SurfaceContainer)
            .height(64.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIconButton(Icons.Filled.Close, "清除") { model.clearClip() }
        Text(
            (if (clip.mode == ClipMode.COPY) "复制 " else "剪切 ") + clip.nodes.size,
            fontSize = 17.sp,
            color = OnSurface,
            modifier = Modifier.weight(1f),
        )
        GlyphIconButton("粘贴", { model.paste() }) {
            PasteGlyph(Modifier.size(22.dp), OnSurfaceVariant)
        }
    }
}

@Composable
private fun FabArea(model: AppModel, onDialog: (FileDialog) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(bottom = if (model.clip != null) 64.dp else 0.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Bottom,
    ) {
        if (expanded) {
            MiniFab("文件") { expanded = false; onDialog(FileDialog.NewFile) }
            Spacer(Modifier.height(12.dp))
            MiniFab("文件夹") { expanded = false; onDialog(FileDialog.NewFolder) }
            Spacer(Modifier.height(12.dp))
        }
        Box(
            Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(FabNavy)
                .clickable { expanded = !expanded },
            contentAlignment = Alignment.Center,
        ) {
            if (expanded) {
                Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(26.dp))
            } else {
                Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }
    }
}

val FabNavy = Color(0xFF2C3A55)

@Composable
private fun MiniFab(label: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            fontSize = 14.sp,
            color = Color.White,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(FabNavy)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
        Spacer(Modifier.width(12.dp))
        Box(
            Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(FabNavy)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun FileDialogHost(model: AppModel, dialog: FileDialog?, onClose: () -> Unit) {
    when (dialog) {
        null -> Unit
        FileDialog.NewFolder -> TextInputDialog(
            title = "新建文件夹",
            initial = "",
            confirmLabel = "确定",
            onDismiss = onClose,
        ) { name -> model.createFolder(name); onClose() }
        FileDialog.NewFile -> TextInputDialog(
            title = "新建文件",
            initial = "",
            confirmLabel = "确定",
            onDismiss = onClose,
        ) { name -> model.createFile(name); onClose() }
        is FileDialog.Rename -> TextInputDialog(
            title = "重命名",
            initial = dialog.node.name,
            confirmLabel = "确定",
            onDismiss = onClose,
        ) { name -> model.rename(dialog.node, name); onClose() }
        is FileDialog.Delete -> ConfirmDialog(
            title = if (dialog.nodes.size == 1)
                "删除" + (if (dialog.nodes[0].isDir) "文件夹" else "文件") + "\"${dialog.nodes[0].name}\"" +
                    (if (dialog.nodes[0].isDir) "和它的内容?" else "?")
            else "删除 ${dialog.nodes.size} 个项目?",
            onDismiss = onClose,
        ) { model.delete(dialog.nodes); onClose() }
        is FileDialog.Compress -> CompressDialog(
            initial = if (dialog.nodes.size == 1) dialog.nodes[0].name.substringBeforeLast('.')
            else model.current.name,
            onDismiss = onClose,
        ) { name -> model.compress(dialog.nodes, name); onClose() }
        is FileDialog.Properties -> PropertiesDialog(dialog.node, onClose)
        FileDialog.GoTo -> TextInputDialog(
            title = "转到",
            initial = "/storage/emulated/0/" + model.path.drop(1).joinToString("/") { it.name },
            confirmLabel = "确定",
            onDismiss = onClose,
        ) { onClose() }
        is FileDialog.OpenWith -> OpenWithDialog(dialog.node, onClose)
    }
}

@Composable
private fun OpenWithDialog(node: FsNode, onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("打开方式", fontSize = 20.sp) },
        text = {
            Column {
                Row(
                    Modifier.fillMaxWidth().height(56.dp).clickable(onClick = onDismiss),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                        FolderGlyph(Modifier.size(32.dp), Primary)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("质感文件", fontSize = 16.sp)
                        Text("另存为", fontSize = 13.sp, color = OnSurfaceVariant)
                    }
                }
                Row(
                    Modifier.fillMaxWidth().height(56.dp).clickable(onClick = onDismiss),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                        if (node.kind == FileKind.IMAGE) CameraGlyph(Modifier.size(28.dp), OnSurfaceVariant)
                        else DocGlyph(Modifier.size(28.dp), Color(0xFF3A6EA5))
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(if (node.kind == FileKind.IMAGE) "相册" else "文本编辑器", fontSize = 16.sp)
                }
            }
        },
        confirmButton = {
            Text(
                "仅此一次",
                color = Primary,
                fontSize = 15.sp,
                modifier = Modifier.clickable(onClick = onDismiss).padding(8.dp),
            )
        },
        dismissButton = {
            Text(
                "始终",
                color = OnSurfaceVariant,
                fontSize = 15.sp,
                modifier = Modifier.padding(8.dp),
            )
        },
    )
}
