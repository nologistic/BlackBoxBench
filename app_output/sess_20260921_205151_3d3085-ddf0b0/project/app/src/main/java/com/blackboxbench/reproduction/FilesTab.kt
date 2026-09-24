package com.blackboxbench.reproduction

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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

private enum class SortKey(val label: String) { NAME("名称"), DATE("日期"), SIZE("按文件大小排序"), MIME("MIME type") }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FilesTab(onOpenFile: (String) -> Unit, onOpenSettings: () -> Unit) {
    var refresh by remember { mutableStateOf(0) }
    var files by remember { mutableStateOf(Store.listFiles()) }
    fun reload() {
        files = Store.listFiles()
        refresh++
    }

    var selectionMode by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var showNewFile by remember { mutableStateOf(false) }
    var showSort by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showStorage by remember { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }
    var selMenu by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<String?>(null) }
    var infoTarget by remember { mutableStateOf<String?>(null) }

    var sortKey by remember { mutableStateOf(SortKey.NAME) }
    var foldersFirst by remember { mutableStateOf(true) }
    var reverse by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf<String?>(null) }

    val displayed = remember(files, sortKey, reverse, query, refresh) {
        var list = files
        list = when (sortKey) {
            SortKey.NAME -> list.sortedBy { it.name.lowercase() }
            SortKey.DATE -> list.sortedByDescending { it.modified }
            SortKey.SIZE -> list.sortedByDescending { Store.sizeOf(it.name) }
            SortKey.MIME -> list.sortedBy { it.name.substringAfterLast('.', "") }
        }
        if (reverse) list = list.reversed()
        query?.let { q -> list = list.filter { it.name.contains(q, ignoreCase = true) } }
        list
    }

    if (showStorage) {
        StorageView(onBack = { showStorage = false })
        return
    }

    Scaffold(
        topBar = {
            if (selectionMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NavyBar)
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("✔", color = MarkorRed, fontSize = 22.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "(${selected.size}/${files.size})",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    TopIcon("★") {
                        selected.forEach { Store.setFavorite(it, true) }
                        reload()
                    }
                    TopIcon("✎") { if (selected.size == 1) renameTarget = selected.first() }
                    TopIcon("ⓘ") { if (selected.size == 1) infoTarget = selected.first() }
                    TopIcon("🗑") { if (selected.isNotEmpty()) deleteTarget = selected.first() }
                    Box {
                        TopIcon("⋮") { selMenu = true }
                        DropdownMenu(expanded = selMenu, onDismissRequest = { selMenu = false }) {
                            listOf("剪切板", "移动", "复制", "创建快捷方式", "分享", "清除选项").forEach { item ->
                                DropdownMenuItem(text = { Text(item) }, onClick = {
                                    selMenu = false
                                    if (item == "清除选项") {
                                        selected = emptySet()
                                        selectionMode = false
                                    }
                                })
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NavyBar)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Markor",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    TopIcon("📁") { showStorage = true }
                    TopIcon("⇅") { showSort = true }
                    TopIcon("🔍") { showSearch = true }
                    Box {
                        TopIcon("⋮") { moreMenu = true }
                        DropdownMenu(expanded = moreMenu, onDismissRequest = { moreMenu = false }) {
                            DropdownMenuItem(text = { Text("从本设备导入") }, onClick = { moreMenu = false })
                            DropdownMenuItem(text = { Text("设置") }, onClick = {
                                moreMenu = false
                                onOpenSettings()
                            })
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewFile = true },
                containerColor = MarkorRed,
                shape = RoundedCornerShape(16.dp),
            ) { Text("+", color = Color.White, fontSize = 28.sp) }
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .background(ListBg)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFDADADA))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("📁", fontSize = 18.sp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("..", fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                    Text("/storage/emulated/0/Documents", fontSize = 12.sp, color = Color.Gray)
                }
            }
            if (displayed.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("¯\\_(ツ)_/¯", fontSize = 32.sp, color = Color.Gray)
                }
            } else {
                LazyColumn {
                    items(displayed, key = { it.name }) { f ->
                        val fav = Store.isFavorite(f.name)
                        val isSelected = selected.contains(f.name)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(if (isSelected) Color(0xFFCCE0FF) else Color.Transparent)
                                .combinedClickable(
                                    onClick = {
                                        if (selectionMode) {
                                            selected = if (isSelected) selected - f.name else selected + f.name
                                            if (selected.isEmpty()) selectionMode = false
                                        } else onOpenFile(f.name)
                                    },
                                    onLongClick = {
                                        selectionMode = true
                                        selected = selected + f.name
                                    },
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                when {
                                    f.isTodo -> "☑"
                                    f.isQuickNote -> "⚡"
                                    else -> "▤"
                                },
                                fontSize = 20.sp,
                                color = if (fav) FavYellow else Color(0xFF666666),
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(f.name, fontWeight = FontWeight.Bold, color = Color(0xFF222222))
                                Text(formatTime(f.modified), fontSize = 12.sp, color = Color.Gray)
                            }
                            if (selectionMode && isSelected) {
                                Spacer(Modifier.weight(1f))
                                Text("✔", color = MarkorRed, fontSize = 18.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewFile) {
        NewFileDialog(onDismiss = { showNewFile = false }, onCreated = { name ->
            showNewFile = false
            reload()
            onOpenFile(name)
        })
    }
    if (showSort) {
        SortDialog(
            current = sortKey, foldersFirst = foldersFirst, reverse = reverse,
            onDismiss = { showSort = false },
            onApply = { k, ff, rv -> sortKey = k; foldersFirst = ff; reverse = rv; showSort = false },
        )
    }
    if (showSearch) {
        SearchDialog(
            onDismiss = { showSearch = false },
            onApply = { q -> query = q; showSearch = false },
        )
    }
    renameTarget?.let { target ->
        RenameDialog(
            original = target,
            onDismiss = { renameTarget = null },
            onRename = { newName ->
                Store.rename(target, newName)
                renameTarget = null
                selected = emptySet(); selectionMode = false
                reload()
            },
        )
    }
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除 $target 吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    (selected.ifEmpty { setOf(target) }).forEach { Store.delete(it) }
                    deleteTarget = null
                    selected = emptySet(); selectionMode = false
                    reload()
                }) { Text("确定", color = MarkorRed) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消", color = MarkorRed) }
            },
        )
    }
    infoTarget?.let { target ->
        FileInfoDialog(
            name = target,
            onDismiss = { infoTarget = null },
            onFavChanged = { reload() },
        )
    }
}

@Composable
fun TopIcon(glyph: String, onClick: () -> Unit) {
    Text(
        glyph,
        color = Color.White,
        fontSize = 20.sp,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewFileDialog(onDismiss: () -> Unit, onCreated: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(Store.fileTypes[0]) }
    var template by remember { mutableStateOf(Store.templates[0]) }
    var typeExpanded by remember { mutableStateOf(false) }
    var templateExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建文件") },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                Text("格式 {{title}}", fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
                    TextField(
                        value = type,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("类型") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                        modifier = Modifier.menuAnchor(),
                    )
                    ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        Store.fileTypes.forEach { t ->
                            DropdownMenuItem(text = { Text(t) }, onClick = { type = t; typeExpanded = false })
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                ExposedDropdownMenuBox(expanded = templateExpanded, onExpandedChange = { templateExpanded = it }) {
                    TextField(
                        value = template,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("模板") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(templateExpanded) },
                        modifier = Modifier.menuAnchor(),
                    )
                    ExposedDropdownMenu(expanded = templateExpanded, onDismissRequest = { templateExpanded = false }) {
                        Store.templates.forEach { t ->
                            DropdownMenuItem(text = { Text(t) }, onClick = { template = t; templateExpanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    val ext = when (type) {
                        "Markdown" -> ".md"
                        "纯文本" -> ".txt"
                        "todo.txt" -> ".todo.txt"
                        "Wikitext" -> ".wiki"
                        "Zim" -> ".zim"
                        "AsciiDoc" -> ".adoc"
                        "OrgMode" -> ".org"
                        else -> ".csv"
                    }
                    val fileName = if (name.contains('.')) name else name + ext
                    val title = name.substringBeforeLast('.')
                    Store.write(fileName, Store.templateContent(template, title).replace("{{title}}", title))
                    onCreated(fileName)
                }
            }) { Text("确定", color = MarkorRed) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) { Text("文件夹", color = MarkorRed) }
                TextButton(onClick = onDismiss) { Text("取消", color = MarkorRed) }
            }
        },
    )
}

@Composable
private fun SortDialog(
    current: SortKey, foldersFirst: Boolean, reverse: Boolean,
    onDismiss: () -> Unit, onApply: (SortKey, Boolean, Boolean) -> Unit,
) {
    var key by remember { mutableStateOf(current) }
    var ff by remember { mutableStateOf(foldersFirst) }
    var rv by remember { mutableStateOf(reverse) }
    var localFolder by remember { mutableStateOf(false) }
    var dotfiles by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("排序方式") },
        text = {
            Column {
                CheckRow("本地文件夹", localFolder) { localFolder = it }
                SortKey.values().forEach { k ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { key = k },
                    ) {
                        Text(k.label, modifier = Modifier.weight(1f))
                        RadioButton(
                            selected = key == k, onClick = { key = k },
                            colors = RadioButtonDefaults.colors(selectedColor = MarkorRed),
                        )
                    }
                }
                CheckRow("文件夹在前", ff) { ff = it }
                CheckRow("逆向排序", rv) { rv = it }
                CheckRow(".dotfiles", dotfiles) { dotfiles = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(key, ff, rv) }) { Text("确定", color = MarkorRed) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = MarkorRed) }
        },
    )
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) },
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Checkbox(
            checked = checked, onCheckedChange = onChange,
            colors = CheckboxDefaults.colors(checkedColor = MarkorRed),
        )
    }
}

@Composable
private fun SearchDialog(onDismiss: () -> Unit, onApply: (String?) -> Unit) {
    var q by remember { mutableStateOf("") }
    var regex by remember { mutableStateOf(false) }
    var caseSensitive by remember { mutableStateOf(false) }
    var inContent by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("搜索") },
        text = {
            Column {
                Text("在目录中递归搜索", fontSize = 13.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                TextField(value = q, onValueChange = { q = it }, label = { Text("搜索") }, singleLine = true)
                CheckRow("正则表达式搜索", regex) { regex = it }
                CheckRow("区分大小写", caseSensitive) { caseSensitive = it }
                CheckRow("在内容中搜索", inContent) { inContent = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(q.ifBlank { null }) }) { Text("确定", color = MarkorRed) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = MarkorRed) }
        },
    )
}

@Composable
private fun RenameDialog(original: String, onDismiss: () -> Unit, onRename: (String) -> Unit) {
    var name by remember { mutableStateOf(original) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重命名") },
        text = {
            TextField(value = name, onValueChange = { name = it }, singleLine = true)
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onRename(name) }) { Text("确定", color = MarkorRed) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = MarkorRed) }
        },
    )
}

@Composable
fun FileInfoDialog(name: String, onDismiss: () -> Unit, onFavChanged: () -> Unit) {
    val content = Store.read(name)
    var fav by remember { mutableStateOf(Store.isFavorite(name)) }
    var recent by remember { mutableStateOf(true) }
    val lines = content.lines().size
    val words = content.split(Regex("\\s+")).filter { it.isNotBlank() }.size
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("文档信息") },
        text = {
            Column {
                InfoRow("名称", name)
                InfoRow("位置", "/storage/emulated/0/Documents/markor/$name")
                InfoRow("修改时间", formatTimeFull(Store.file(name).lastModified()))
                InfoRow("大小", "${Store.sizeOf(name)} B")
                InfoRow("MIME", if (name.endsWith(".md")) "text/markdown" else "text/plain")
                InfoRow("统计", "$lines 行 / $words 词 / ${content.length} 字符")
                InfoRow("SHA-256", Store.sha256(name).take(24) + "…")
                Spacer(Modifier.height(8.dp))
                CheckRow("最近文件列表", recent) { recent = it }
                CheckRow("收藏", fav) {
                    fav = it
                    Store.setFavorite(name, it)
                    onFavChanged()
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("确定", color = MarkorRed) }
        },
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(Modifier.padding(vertical = 3.dp)) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Text(value, fontSize = 14.sp, color = Color(0xFF222222))
    }
}

@Composable
private fun StorageView(onBack: () -> Unit) {
    val entries = listOf(
        Triple("📁", "emulated", "2026/9/16 20:45"),
        Triple("📁", "AppData (private)", "2026/9/22 05:16"),
        Triple("📁", "AppData (external-0)", "2026/9/22 04:53"),
        Triple("🕘", "Recent", "1970/1/1 08:00"),
        Triple("♥", "Popular", "1970/1/1 08:00"),
        Triple("★", "Favourites", "1970/1/1 08:00"),
        Triple("⬇", "Download", "2026/9/22 04:52"),
        Triple("⌂", "笔记本", "2026/9/22 05:50"),
    )
    Column(Modifier.fillMaxSize().background(ListBg)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NavyBar)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "> storage",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onBack),
            )
            TopIcon("📁") { }
            TopIcon("⋮") { }
        }
        LazyColumn {
            items(entries, key = { it.second }) { (icon, name, time) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onBack)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(icon, fontSize = 20.sp, color = Color(0xFF666666))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(name, fontWeight = FontWeight.Bold, color = Color(0xFF222222))
                        Text(time, fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}
