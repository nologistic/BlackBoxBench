package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

private data class SortState(
    val by: String = "名称",
    val foldersFirst: Boolean = true,
    val reverse: Boolean = false,
    val dotfiles: Boolean = false,
    val localFolder: Boolean = false,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrowserScreen(
    state: AppState,
    onOpenFile: (File) -> Unit,
    onSelectTab: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenImport: () -> Unit,
) {
    val c = LocalMarkorColors.current
    val store = state.store
    val dir = state.dir
    var refresh by remember { mutableStateOf(0) }
    var sort by remember { mutableStateOf(SortState()) }
    var menu by remember { mutableStateOf<String?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var showSort by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<File>?>(null) }
    var renameTarget by remember { mutableStateOf<File?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var moveDialog by remember { mutableStateOf(false) }
    var createFolderOnly by remember { mutableStateOf(false) }

    val selection = state.selection
    val selectionMode = selection.isNotEmpty()

    val entries = remember(dir, refresh, sort) {
        val raw = store.list(dir)
        val sorted = when (sort.by) {
            "日期" -> raw.sortedByDescending { it.modified }
            "按文件大小排序" -> raw.sortedByDescending { it.file.length() }
            "MIME type" -> raw.sortedBy { it.name.substringAfterLast('.', "") }
            else -> raw.sortedBy { it.name.lowercase() }
        }
        val withFolders = if (sort.foldersFirst) sorted.sortedByDescending { it.isDir } else sorted
        if (sort.reverse) withFolders.reversed() else withFolders
    }

    Column(modifier = Modifier.fillMaxSize().background(c.background)) {
        BrowserTopBar(
            title = if (store.isRoot(dir)) "Markor" else "> ${dir.name}",
            selectionMode = selectionMode,
            selectionCount = selection.size,
            total = entries.size,
            onNewFolder = { createFolderOnly = true; showCreate = true },
            onSort = { showSort = true },
            onSearch = { showSearch = true },
            onOverflow = { menu = if (menu == null) "root" else null },
            onFavourite = {
                state.toast("已标记为收藏")
                state.selection = emptySet()
            },
            onRename = { renameTarget = entries.firstOrNull { selection.contains(it.file.absolutePath) }?.file },
            onInfo = {
                val file = entries.firstOrNull { selection.contains(it.file.absolutePath) }?.file
                if (file != null) state.toast("${file.name} · ${file.length()} B")
            },
            onDelete = { confirmDelete = true },
            onSelectionOverflow = { menu = "selection" },
            onClearSelection = { state.selection = emptySet() },
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(c.surface)
                        .clickable {
                            val parent = store.parentOf(dir)
                            if (parent != null) {
                                state.dir = parent
                                state.selection = emptySet()
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (store.isRoot(dir)) {
                        IcoFolder(size = 30.dp, tint = c.textSecondary, filled = true)
                    } else {
                        IcoHome(size = 30.dp, tint = c.textSecondary)
                    }
                    Spacer(Modifier.width(20.dp))
                    Column {
                        Text("..", color = c.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                        Text(store.displayPath(store.parentOf(dir)), color = c.textSecondary, fontSize = 13.sp)
                    }
                }
                HorizontalDivider(color = c.divider)
            }
            if (entries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("¯\\_(ツ)_/¯", color = c.textSecondary, fontSize = 28.sp)
                    }
                }
            }
            items(entries, key = { it.file.absolutePath }) { entry ->
                val selected = selection.contains(entry.file.absolutePath)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (selected) c.accent.copy(alpha = 0.10f) else c.surface)
                        .combinedClickable(
                            onClick = {
                                if (selectionMode) {
                                    state.selection = if (selected) selection - entry.file.absolutePath
                                    else selection + entry.file.absolutePath
                                } else if (entry.isDir) {
                                    state.dir = entry.file
                                    state.selection = emptySet()
                                } else {
                                    onOpenFile(entry.file)
                                }
                            },
                            onLongClick = {
                                state.selection = if (selected) selection - entry.file.absolutePath
                                else selection + entry.file.absolutePath
                            },
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                        when {
                            selected -> IcoCheck(size = 26.dp, tint = c.accent)
                            entry.isDir -> IcoFolder(size = 30.dp, tint = c.textSecondary, filled = true)
                            entry.name.equals("QuickNote.md", true) -> IcoBolt(size = 28.dp, tint = c.textSecondary)
                            entry.name.endsWith(".txt", true) -> IcoCheckSquare(size = 28.dp, tint = c.textSecondary)
                            else -> IcoDoc(size = 30.dp, tint = c.textSecondary)
                        }
                    }
                    Spacer(Modifier.width(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(entry.name, color = c.textPrimary, fontSize = 19.sp)
                        Spacer(Modifier.height(2.dp))
                        Text(Store.stamp(entry.modified), color = c.textSecondary, fontSize = 14.sp)
                    }
                }
                HorizontalDivider(color = c.divider)
            }
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            FloatingActionButton(
                onClick = { createFolderOnly = false; showCreate = true },
                containerColor = c.accent,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 20.dp),
            ) { IcoAdd(size = 26.dp, tint = Color.White) }
        }

        AppBottomBar(selected = "files", onSelect = onSelectTab)
    }

    DropdownMenu(expanded = menu != null, onDismissRequest = { menu = null }) {
        if (menu == "root") {
            DropdownMenuItem(
                text = { Text("从本设备导入", color = c.textPrimary, fontSize = 16.sp) },
                leadingIcon = { IcoDoc(size = 22.dp, tint = c.textPrimary) },
                onClick = { menu = null; onOpenImport() },
            )
            DropdownMenuItem(
                text = { Text("设置", color = c.textPrimary, fontSize = 16.sp) },
                leadingIcon = { IcoGear(size = 22.dp, tint = c.textPrimary) },
                onClick = { menu = null; onOpenSettings() },
            )
        } else {
            DropdownMenuItem(
                text = { Text("剪切板", color = c.textPrimary, fontSize = 16.sp) },
                onClick = {
                    menu = null
                    val file = entries.firstOrNull { selection.contains(it.file.absolutePath) }?.file
                    if (file != null) {
                        state.copyToClipboard(file.name, store.read(file))
                        state.toast("内容已复制到剪切板")
                    }
                    state.selection = emptySet()
                },
            )
            DropdownMenuItem(
                text = { Text("移动", color = c.textPrimary, fontSize = 16.sp) },
                onClick = { menu = null; moveDialog = true },
            )
            DropdownMenuItem(
                text = { Text("复制", color = c.textPrimary, fontSize = 16.sp) },
                onClick = {
                    menu = null
                    selection.map { File(it) }.forEach { file ->
                        val copy = File(file.parentFile, file.nameWithoutExtension + " copy." + file.extension)
                        runCatching { file.copyTo(copy, overwrite = false) }
                    }
                    state.toast("已复制 ${selection.size} 项")
                    state.selection = emptySet()
                    refresh++
                },
            )
            DropdownMenuItem(
                text = { Text("创建快捷方式", color = c.textPrimary, fontSize = 16.sp) },
                onClick = {
                    menu = null
                    selection.map { File(it) }.forEach { file ->
                        store.write(File(file.parentFile, file.name + ".shortcut"), store.displayPath(file))
                    }
                    state.toast("已创建快捷方式")
                    state.selection = emptySet()
                    refresh++
                },
            )
            DropdownMenuItem(
                text = { Text("分享", color = c.textPrimary, fontSize = 16.sp) },
                onClick = {
                    menu = null
                    val file = entries.firstOrNull { selection.contains(it.file.absolutePath) }?.file
                    if (file != null) state.shareText(file, store.read(file))
                    state.selection = emptySet()
                },
            )
            DropdownMenuItem(
                text = { Text("清除选项", color = c.textPrimary, fontSize = 16.sp) },
                onClick = { menu = null; state.selection = emptySet() },
            )
        }
    }

    if (showCreate) {
        CreateDialog(
            folderOnly = createFolderOnly,
            onDismiss = { showCreate = false },
            onCreate = { name, isDir, type ->
                showCreate = false
                val fileName = if (isDir) name else name + extensionFor(type)
                val created = store.create(dir, fileName, isDir)
                if (created == null) {
                    state.toast("已存在同名项目")
                } else {
                    refresh++
                    if (!isDir) onOpenFile(created)
                }
            },
        )
    }

    if (showSort) {
        SortDialog(
            sort = sort,
            onDismiss = { showSort = false },
            onApply = { sort = it; showSort = false },
        )
    }

    if (showSearch) {
        SearchDialog(
            onDismiss = { showSearch = false },
            onSearch = { query, regex, caseSensitive, inContent ->
                showSearch = false
                val maxDepth = when (state.prefs.str("searchDepth", "不限")) {
                    "1" -> 1
                    "2" -> 2
                    "3" -> 3
                    else -> -1
                }
                val all = store.walk(store.root, 1, maxDepth).filter { !it.isDirectory }
                val pattern = runCatching {
                    Regex(if (regex) query else Regex.escape(query), if (caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE))
                }.getOrNull()
                val hits = if (pattern == null) emptyList() else all.filter { file ->
                    pattern.containsMatchIn(file.name) || (inContent && pattern.containsMatchIn(store.read(file)))
                }
                searchResults = hits
                if (hits.isEmpty()) state.toast("未找到匹配项")
            },
        )
    }

    searchResults?.let { hits ->
        AlertDialog(
            onDismissRequest = { searchResults = null },
            containerColor = c.surface,
            title = { Text("选择", color = c.textPrimary, fontSize = 20.sp) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("搜索", color = c.textSecondary, fontSize = 15.sp)
                    HorizontalDivider(color = c.divider, modifier = Modifier.padding(vertical = 6.dp))
                    if (hits.isEmpty()) Text("无结果", color = c.textSecondary, fontSize = 15.sp)
                    hits.forEach { file ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    searchResults = null
                                    state.dir = file.parentFile ?: store.root
                                    onOpenFile(file)
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IcoDoc(size = 24.dp, tint = c.textSecondary)
                            Spacer(Modifier.width(16.dp))
                            Text(file.name, color = c.textPrimary, fontSize = 17.sp)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { searchResults = null }) { Text("取消", color = c.accent, fontSize = 16.sp) } },
        )
    }

    renameTarget?.let { target ->
        RenameDialog(
            current = target.name,
            onDismiss = { renameTarget = null },
            onRename = { newName ->
                renameTarget = null
                if (store.rename(target, newName) == null) state.toast("重命名失败")
                state.selection = emptySet()
                refresh++
            },
        )
    }

    if (confirmDelete) {
        val names = selection.map { File(it).name }
        ConfirmDialog(
            title = "确认删除",
            body = names.joinToString("\n"),
            onConfirm = {
                confirmDelete = false
                names.forEach { name -> store.delete(File(dir, name)) }
                state.toast("已删除 ${names.size} 项")
                state.selection = emptySet()
                refresh++
            },
            onDismiss = { confirmDelete = false },
        )
    }

    if (moveDialog) {
        RenameDialog(
            title = "移动到文件夹",
            current = "",
            onDismiss = { moveDialog = false },
            onRename = { target ->
                moveDialog = false
                val targetDir = File(store.root, target.trim())
                targetDir.mkdirs()
                selection.map { File(it) }.forEach { file ->
                    runCatching { file.renameTo(File(targetDir, file.name)) }
                }
                state.toast("已移动 ${selection.size} 项")
                state.selection = emptySet()
                refresh++
            },
        )
    }
}

@Composable
private fun BrowserTopBar(
    title: String,
    selectionMode: Boolean,
    selectionCount: Int,
    total: Int,
    onNewFolder: () -> Unit,
    onSort: () -> Unit,
    onSearch: () -> Unit,
    onOverflow: () -> Unit,
    onFavourite: () -> Unit,
    onRename: () -> Unit,
    onInfo: () -> Unit,
    onDelete: () -> Unit,
    onSelectionOverflow: () -> Unit,
    onClearSelection: () -> Unit,
) {
    val c = LocalMarkorColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.toolbar)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            Text(title, color = c.onToolbar, fontSize = 22.sp, fontWeight = FontWeight.Medium)
            if (selectionMode) Text("($selectionCount / $total)", color = c.onToolbar, fontSize = 14.sp)
        }
        if (selectionMode) {
            TopBarIcon(onFavourite) { IcoStar(size = 22.dp, tint = c.onToolbar, filled = true) }
            TopBarIcon(onRename) { IcoTextLines(size = 22.dp, tint = c.onToolbar) }
            TopBarIcon(onInfo) { IcoInfo(size = 22.dp, tint = c.onToolbar) }
            TopBarIcon(onDelete) { IcoTrash(size = 22.dp, tint = c.onToolbar) }
            TopBarIcon(onSelectionOverflow) { IcoMore(size = 22.dp, tint = c.onToolbar) }
        } else {
            TopBarIcon(onNewFolder) { IcoFolder(size = 22.dp, tint = c.onToolbar) }
            TopBarIcon(onSort) { IcoSort(size = 22.dp, tint = c.onToolbar) }
            TopBarIcon(onSearch) { IcoSearch(size = 22.dp, tint = c.onToolbar) }
            TopBarIcon(onOverflow) { IcoMore(size = 22.dp, tint = c.onToolbar) }
        }
    }
}

@Composable
private fun TopBarIcon(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { content() }
}

private fun extensionFor(type: String): String = when (type) {
    "纯文本" -> ".txt"
    "todo.txt" -> ".txt"
    "AsciiDoc" -> ".adoc"
    "OrgMode" -> ".org"
    "Wikitext / Zim" -> ".wiki"
    else -> ".md"
}

@Composable
private fun CreateDialog(
    folderOnly: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String, Boolean, String) -> Unit,
) {
    val c = LocalMarkorColors.current
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Markdown") }
    var template by remember { mutableStateOf("空文件") }
    var typeMenu by remember { mutableStateOf(false) }
    var templateMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = { Text("名称", color = c.textSecondary, fontSize = 15.sp) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { Text(extensionFor(type), color = c.textSecondary, fontSize = 15.sp) },
                )
                Spacer(Modifier.height(14.dp))
                Text("格式", color = c.textSecondary, fontSize = 15.sp)
                Text("{{title}}", color = c.textPrimary, fontSize = 17.sp)
                HorizontalDivider(color = c.divider)
                Spacer(Modifier.height(14.dp))
                Text("文件类型", color = c.textSecondary, fontSize = 15.sp)
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { typeMenu = true }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(type, color = c.textPrimary, fontSize = 17.sp, modifier = Modifier.weight(1f))
                        IcoChevronDown(size = 20.dp, tint = c.textSecondary)
                    }
                    DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                        listOf("Markdown", "纯文本", "todo.txt", "AsciiDoc", "OrgMode", "Wikitext / Zim").forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option, color = c.textPrimary, fontSize = 16.sp) },
                                onClick = { type = option; typeMenu = false },
                            )
                        }
                    }
                }
                HorizontalDivider(color = c.divider)
                Spacer(Modifier.height(14.dp))
                Text("模板", color = c.textSecondary, fontSize = 15.sp)
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { templateMenu = true }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(template, color = c.textPrimary, fontSize = 17.sp, modifier = Modifier.weight(1f))
                        IcoChevronDown(size = 20.dp, tint = c.textSecondary)
                    }
                    DropdownMenu(expanded = templateMenu, onDismissRequest = { templateMenu = false }) {
                        listOf("空文件", "任务列表", "会议记录").forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option, color = c.textPrimary, fontSize = 16.sp) },
                                onClick = { template = option; templateMenu = false },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("- 创建文件夹时不会含有文件扩展名", color = c.textSecondary, fontSize = 13.sp)
                Text("- 文件和文件夹不会被覆盖", color = c.textSecondary, fontSize = 13.sp)
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = { onCreate(name, true, type) }) { Text("文件夹", color = c.accent, fontSize = 16.sp) }
                TextButton(onClick = { if (name.isNotBlank()) onCreate(name, false, type) }) {
                    Text("确定", color = c.accent, fontSize = 16.sp)
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = c.accent, fontSize = 16.sp) } },
    )
}

@Composable
private fun SortDialog(sort: SortState, onDismiss: () -> Unit, onApply: (SortState) -> Unit) {
    val c = LocalMarkorColors.current
    var current by remember { mutableStateOf(sort) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = { Text("排序方式", color = c.textPrimary, fontSize = 20.sp) },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { current = current.copy(localFolder = !current.localFolder) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IcoFolder(size = 22.dp, tint = c.textSecondary)
                    Spacer(Modifier.width(16.dp))
                    Text("本地文件夹", color = c.textPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    Checkbox(
                        checked = current.localFolder,
                        onCheckedChange = { current = current.copy(localFolder = it) },
                        colors = CheckboxDefaults.colors(checkedColor = c.accent),
                    )
                }
                listOf("名称", "日期", "按文件大小排序", "MIME type").forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { current = current.copy(by = option) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = current.by == option,
                            onClick = { current = current.copy(by = option) },
                            colors = RadioButtonDefaults.colors(selectedColor = c.accent),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(option, color = c.textPrimary, fontSize = 16.sp)
                    }
                }
                SwitchRow("文件夹在前", current.foldersFirst) { current = current.copy(foldersFirst = it) }
                SwitchRow("逆向排序", current.reverse) { current = current.copy(reverse = it) }
                SwitchRow(".dotfiles", current.dotfiles) { current = current.copy(dotfiles = it) }
            }
        },
        confirmButton = { TextButton(onClick = { onApply(current) }) { Text("确定", color = c.accent, fontSize = 16.sp) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = c.accent, fontSize = 16.sp) } },
    )
}

@Composable
private fun SearchDialog(
    onDismiss: () -> Unit,
    onSearch: (String, Boolean, Boolean, Boolean) -> Unit,
) {
    val c = LocalMarkorColors.current
    var query by remember { mutableStateOf("") }
    var regex by remember { mutableStateOf(false) }
    var caseSensitive by remember { mutableStateOf(false) }
    var inContent by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = {
            Column {
                Text("搜索", color = c.textPrimary, fontSize = 20.sp)
                Text("在目录中递归搜索", color = c.textSecondary, fontSize = 14.sp)
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    label = { Text("搜索") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                SwitchRow("正则表达式搜索", regex) { regex = it }
                SwitchRow("区分大小写", caseSensitive) { caseSensitive = it }
                SwitchRow("在内容中搜索", inContent) { inContent = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (query.isNotBlank()) onSearch(query, regex, caseSensitive, inContent) }) {
                Text("确定", color = c.accent, fontSize = 16.sp)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = c.accent, fontSize = 16.sp) } },
    )
}

@Composable
private fun RenameDialog(
    current: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    title: String = "重命名",
) {
    val c = LocalMarkorColors.current
    var name by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = { Text(title, color = c.textPrimary, fontSize = 20.sp) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onRename(name) }) { Text("确定", color = c.accent, fontSize = 16.sp) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = c.accent, fontSize = 16.sp) } },
    )
}

private data class DeviceFolder(val name: String, val stamp: String, val children: List<DeviceFolder> = emptyList(), val files: List<String> = emptyList())

@Composable
fun ImportScreen(state: AppState, onBack: () -> Unit) {
    val c = LocalMarkorColors.current
    val root = remember {
        DeviceFolder(
            name = "", stamp = "", children = listOf(
                DeviceFolder("Alarms", "2026/9/16 20:45"),
                DeviceFolder("Android", "2026/9/16 20:45"),
                DeviceFolder("Audiobooks", "2026/9/16 20:45"),
                DeviceFolder("DCIM", "2026/9/16 20:45"),
                DeviceFolder(
                    "Documents", "2026/9/22 23:03",
                    children = listOf(
                        DeviceFolder("notes", "2026/9/22 22:58", files = listOf("reading-list.md", "meeting-notes.md")),
                        DeviceFolder("markor", "2026/9/22 23:03"),
                    ),
                ),
                DeviceFolder("Download", "2026/9/22 23:03", files = listOf("sample-note.txt")),
                DeviceFolder("Movies", "2026/9/16 20:45"),
                DeviceFolder("Music", "2026/9/16 20:45"),
                DeviceFolder("Notifications", "2026/9/16 20:45"),
                DeviceFolder("Pictures", "2026/9/16 20:45"),
                DeviceFolder("Podcasts", "2026/9/16 20:45"),
                DeviceFolder("Recordings", "2026/9/16 20:45"),
            ),
        )
    }
    var path by remember { mutableStateOf(listOf<DeviceFolder>()) }
    val current = path.lastOrNull() ?: root
    var refresh by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().background(c.background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(c.toolbar)
                .padding(horizontal = 8.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TopBarIcon(onBack) { IcoArrowLeft(size = 22.dp, tint = c.onToolbar) }
            Text("从本设备导入", color = c.onToolbar, fontSize = 20.sp, modifier = Modifier.padding(start = 8.dp))
        }
        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(c.surface)
                        .clickable { path = path.dropLast(1) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IcoFolder(size = 30.dp, tint = c.textSecondary, filled = true)
                    Spacer(Modifier.width(20.dp))
                    Column {
                        Text("..", color = c.textPrimary, fontSize = 17.sp)
                        Text("/storage/emulated", color = c.textSecondary, fontSize = 13.sp)
                    }
                }
                HorizontalDivider(color = c.divider)
            }
            items(current.children, key = { it.name + refresh }) { folder ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(c.surface)
                        .clickable { path = path + folder }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IcoFolder(size = 30.dp, tint = c.textSecondary, filled = true)
                    Spacer(Modifier.width(20.dp))
                    Column {
                        Text(folder.name, color = c.textPrimary, fontSize = 19.sp)
                        Text(folder.stamp, color = c.textSecondary, fontSize = 14.sp)
                    }
                }
                HorizontalDivider(color = c.divider)
            }
            items(current.files, key = { it + refresh }) { name ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(c.surface)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IcoDoc(size = 28.dp, tint = c.textSecondary)
                    Spacer(Modifier.width(20.dp))
                    Text(name, color = c.textPrimary, fontSize = 19.sp)
                }
                HorizontalDivider(color = c.divider)
            }
        }
        Box(modifier = Modifier.fillMaxWidth().background(c.background)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onBack) { Text("取消", color = c.accent, fontSize = 16.sp) }
                TextButton(onClick = {
                    var copied = 0
                    current.files.forEach { name ->
                        val target = File(state.store.root, name)
                        if (!target.exists()) {
                            state.store.write(target, "从设备导入的示例内容\n")
                            copied++
                        }
                    }
                    current.children.forEach { folder ->
                        val target = File(state.store.root, folder.name)
                        if (!target.exists() && target.mkdirs()) copied++
                    }
                    state.toast(if (copied == 0) "没有可导入的内容" else "已导入 $copied 项")
                    refresh++
                    onBack()
                }) { Text("导入", color = c.accent, fontSize = 16.sp) }
            }
        }
    }
}
