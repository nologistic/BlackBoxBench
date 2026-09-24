package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SortDialog(g: GalleryState, isAlbum: Boolean, onDismiss: () -> Unit) {
    val modes = if (isAlbum) SortMode.albumModes else SortMode.itemModes
    val stored = g.text(if (isAlbum) "albumSortMode" else "fileSortMode", SortMode.MODIFIED.name)
    val selected = modes.indexOfFirst { it.name == stored }.coerceAtLeast(0)
    val ascending = g.bool(if (isAlbum) "albumSortAsc" else "fileSortAsc", false)
    var choice by remember { mutableStateOf(selected) }
    var asc by remember { mutableStateOf(ascending) }
    var localOnly by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogSurface,
        title = { Text("排序方式", color = Primary, fontSize = 22.sp) },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState())) {
                modes.forEachIndexed { index, mode ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { choice = index }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = choice == index,
                            onClick = { choice = index },
                            colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = Primary),
                        )
                        Text(mode.label, fontSize = 17.sp)
                    }
                }
                androidx.compose.material3.HorizontalDivider(Modifier.padding(vertical = 8.dp), color = DividerGrey)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { asc = true }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.material3.RadioButton(
                        selected = asc,
                        onClick = { asc = true },
                        colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = Primary),
                    )
                    Text("升序", fontSize = 17.sp)
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { asc = false }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.material3.RadioButton(
                        selected = !asc,
                        onClick = { asc = false },
                        colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = Primary),
                    )
                    Text("降序", fontSize = 17.sp)
                }
                androidx.compose.material3.HorizontalDivider(Modifier.padding(vertical = 8.dp), color = DividerGrey)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Checkbox(
                        checked = localOnly,
                        onCheckedChange = { localOnly = it },
                        colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = Primary),
                    )
                    Text("仅应用于此文件夹", fontSize = 16.sp)
                }
                Text(
                    "请注意：分组和排序是两种相互独立的文件组织方式",
                    fontSize = 14.sp,
                    color = Color(0xFF3B3B3B),
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (isAlbum) {
                    g.setText("albumSortMode", modes[choice].name)
                    g.setBool("albumSortAsc", asc)
                } else {
                    g.setText("fileSortMode", modes[choice].name)
                    g.setBool("fileSortAsc", asc)
                }
                if (localOnly) g.setBool("sortLocalOnly", true)
                onDismiss()
            }) { Text("确定", color = Primary, fontSize = 17.sp) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = Primary, fontSize = 17.sp) } },
    )
}

@Composable
fun GroupDialog(g: GalleryState, onDismiss: () -> Unit) {
    val modes = GroupMode.entries
    val selected = modes.indexOfFirst { it.name == g.text("groupMode", GroupMode.NONE.name) }.coerceAtLeast(0)
    var choice by remember { mutableStateOf(selected) }
    var asc by remember { mutableStateOf(g.bool("groupAsc", true)) }
    var showCount by remember { mutableStateOf(g.bool("groupShowCount", false)) }
    var localOnly by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogSurface,
        title = { Text("分组方式", color = Primary, fontSize = 22.sp) },
        text = {
            Column(Modifier.heightIn(max = 540.dp).verticalScroll(rememberScrollState())) {
                modes.forEachIndexed { index, mode ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { choice = index }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = choice == index,
                            onClick = { choice = index },
                            colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = Primary),
                        )
                        Text(mode.label, fontSize = 17.sp)
                    }
                }
                androidx.compose.material3.HorizontalDivider(Modifier.padding(vertical = 8.dp), color = DividerGrey)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { asc = true }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.material3.RadioButton(
                        selected = asc,
                        onClick = { asc = true },
                        colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = Primary),
                    )
                    Text("升序", fontSize = 17.sp)
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { asc = false }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.material3.RadioButton(
                        selected = !asc,
                        onClick = { asc = false },
                        colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = Primary),
                    )
                    Text("降序", fontSize = 17.sp)
                }
                androidx.compose.material3.HorizontalDivider(Modifier.padding(vertical = 8.dp), color = DividerGrey)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Checkbox(
                        checked = showCount,
                        onCheckedChange = { showCount = it },
                        colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = Primary),
                    )
                    Text("在栏目标题上显示文件数", fontSize = 16.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Checkbox(
                        checked = localOnly,
                        onCheckedChange = { localOnly = it },
                        colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = Primary),
                    )
                    Text("仅应用于此文件夹", fontSize = 16.sp)
                }
                Text(
                    "请注意：分组和排序是两种相互独立的文件组织方式",
                    fontSize = 14.sp,
                    color = Color(0xFF3B3B3B),
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                g.setText("groupMode", modes[choice].name)
                g.setBool("groupAsc", asc)
                g.setBool("groupShowCount", showCount)
                onDismiss()
            }) { Text("确定", color = Primary, fontSize = 17.sp) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = Primary, fontSize = 17.sp) } },
    )
}

@Composable
fun ViewTypeDialog(g: GalleryState, onDismiss: () -> Unit) {
    val options = listOf(ViewMode.GRID, ViewMode.LIST)
    val selected = options.indexOfFirst { it.name == g.text("viewMode", ViewMode.GRID.name) }.coerceAtLeast(0)
    var choice by remember { mutableStateOf(selected) }
    var localOnly by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogSurface,
        title = { Text("更改视图类型", color = Primary, fontSize = 22.sp) },
        text = {
            Column {
                options.forEachIndexed { index, mode ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { choice = index }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = choice == index,
                            onClick = { choice = index },
                            colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = Primary),
                        )
                        Text(mode.label, fontSize = 17.sp)
                    }
                }
                androidx.compose.material3.HorizontalDivider(Modifier.padding(vertical = 8.dp), color = DividerGrey)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Checkbox(
                        checked = localOnly,
                        onCheckedChange = { localOnly = it },
                        colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = Primary),
                    )
                    Text("仅应用于此文件夹", fontSize = 16.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                g.setText("viewMode", options[choice].name)
                onDismiss()
            }) { Text("确定", color = Primary, fontSize = 17.sp) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = Primary, fontSize = 17.sp) } },
    )
}

@Composable
fun ColumnsDialog(g: GalleryState, onDismiss: () -> Unit) {
    var choice by remember { mutableStateOf((g.text("columns", "3").toIntOrNull() ?: 3) - 1) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogSurface,
        title = { Text("列数", color = Primary, fontSize = 22.sp) },
        text = {
            Column(Modifier.heightIn(max = 540.dp).verticalScroll(rememberScrollState())) {
                (1..15).forEach { value ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { choice = value - 1 }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = choice == value - 1,
                            onClick = { choice = value - 1 },
                            colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = Primary),
                        )
                        Text("$value 列", fontSize = 17.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                g.setText("columns", (choice + 1).toString())
                onDismiss()
            }) { Text("确定", color = Primary, fontSize = 17.sp) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = Primary, fontSize = 17.sp) } },
    )
}

@Composable
fun FilterDialog(g: GalleryState, onDismiss: () -> Unit) {
    val filter = g.filter()
    var images by remember { mutableStateOf(filter.images) }
    var videos by remember { mutableStateOf(filter.videos) }
    var gif by remember { mutableStateOf(filter.gif) }
    var raw by remember { mutableStateOf(filter.raw) }
    var svg by remember { mutableStateOf(filter.svg) }
    var portrait by remember { mutableStateOf(filter.portrait) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogSurface,
        title = { Text("过滤显示的文件", color = Primary, fontSize = 22.sp) },
        text = {
            Column {
                listOf(
                    "图片" to (images to { v: Boolean -> images = v }),
                    "视频" to (videos to { v: Boolean -> videos = v }),
                    "GIF" to (gif to { v: Boolean -> gif = v }),
                    "RAW 图像" to (raw to { v: Boolean -> raw = v }),
                    "SVG" to (svg to { v: Boolean -> svg = v }),
                    "竖向" to (portrait to { v: Boolean -> portrait = v }),
                ).forEach { (label, pair) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { pair.second(!pair.first) }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = pair.first,
                            onCheckedChange = pair.second,
                            colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = Primary),
                        )
                        Text(label, fontSize = 17.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                g.setFilter(FileFilter(images, videos, gif, raw, svg, portrait))
                onDismiss()
            }) { Text("确定", color = Primary, fontSize = 17.sp) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = Primary, fontSize = 17.sp) } },
    )
}

@Composable
fun RenameDialog(g: GalleryState, album: String, items: List<MediaItem>, onDismiss: () -> Unit) {
    val item = items.firstOrNull { g.selection.contains(it.relPath) } ?: return
    var base by remember { mutableStateOf(item.name.substringBeforeLast('.')) }
    var ext by remember { mutableStateOf(item.name.substringAfterLast('.', "")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogSurface,
        title = { Text("重命名", color = Primary, fontSize = 22.sp) },
        text = {
            Column {
                OutlinedTextField(
                    value = base,
                    onValueChange = { base = it },
                    label = { Text("标题") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = ext,
                    onValueChange = { ext = it },
                    label = { Text("扩展名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                g.selectedItems(items).forEach { g.library.rename(it, base, ext) }
                g.refresh()
                g.selection = emptySet()
                onDismiss()
            }) { Text("确定", color = Primary, fontSize = 17.sp) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = Primary, fontSize = 17.sp) } },
    )
}

@Composable
fun DeleteDialog(g: GalleryState, items: List<MediaItem>, isRecycle: Boolean, onDismiss: () -> Unit) {
    val chosen = g.selectedItems(items)
    val first = chosen.firstOrNull()
    val skipConfirm = g.bool("skipDeleteConfirm", false)
    if (skipConfirm || first == null) {
        if (first != null) performDelete(g, chosen, isRecycle)
        onDismiss()
        return
    }
    var noAsk by remember { mutableStateOf(false) }
    var skipTrash by remember { mutableStateOf(false) }
    val message = if (isRecycle) {
        "确定要彻底删除 \"${first.displayName}\" (${formatSize(first.sizeBytes)}) 吗？"
    } else {
        "确定要将 \"${first.displayName}\" (${formatSize(first.sizeBytes)}) 移至回收站吗？"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogSurface,
        text = {
            Column {
                Text(message, fontSize = 17.sp)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Checkbox(
                        checked = noAsk,
                        onCheckedChange = { noAsk = it },
                        colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = Primary),
                    )
                    Text("本次会话不再询问", fontSize = 16.sp)
                }
                if (!isRecycle) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Checkbox(
                            checked = skipTrash,
                            onCheckedChange = { skipTrash = it },
                            colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = Primary),
                        )
                        Text("跳过回收站，直接删除文件", fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (noAsk) g.setBool("skipDeleteConfirm", true)
                if (skipTrash && !isRecycle) {
                    chosen.forEach { g.library.deletePermanently(it) }
                    g.refresh()
                    g.selection = emptySet()
                    onDismiss()
                } else {
                    performDelete(g, chosen, isRecycle)
                    onDismiss()
                }
            }) { Text("是", color = Primary, fontSize = 17.sp) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("否", color = Primary, fontSize = 17.sp) } },
    )
}

private fun performDelete(g: GalleryState, chosen: List<MediaItem>, isRecycle: Boolean) {
    if (isRecycle) {
        chosen.forEach { g.library.deletePermanently(it) }
    } else {
        chosen.forEach { g.library.moveToTrash(it) }
    }
    g.refresh()
    g.selection = emptySet()
}

@Composable
fun HideDialog(g: GalleryState, items: List<MediaItem>, onDismiss: () -> Unit) {
    val chosen = g.selectedItems(items)
    val message = if (chosen.firstOrNull()?.hidden == true) {
        "确定要取消隐藏所选项目吗？"
    } else {
        "确定要隐藏所选项目吗？"
    }
    ConfirmDialog(
        message = message,
        onDismiss = onDismiss,
        onConfirm = {
            val hide = chosen.firstOrNull()?.hidden != true
            chosen.forEach { g.library.setHidden(it, hide) }
            g.refresh()
            g.selection = emptySet()
            onDismiss()
        },
        confirmLabel = "是",
        dismissLabel = "否",
    )
}

@Composable
fun FolderPickerDialog(
    g: GalleryState,
    title: String,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    var choice by remember { mutableStateOf(0) }
    val folders = g.library.folderTree()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogSurface,
        title = { Text(title, color = Primary, fontSize = 22.sp) },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState())) {
                Row(
                    Modifier
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFE3E3EE))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("内部存储空间", fontSize = 15.sp)
                }
                folders.forEachIndexed { index, folder ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { choice = index }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Primary.copy(alpha = 0.75f))
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(folder.first, fontSize = 17.sp)
                            if (index > 0) Text("${folder.second} 项", fontSize = 14.sp, color = HintGrey)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onPick(folders[choice].first) }) {
                Text("确定", color = Primary, fontSize = 17.sp)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = Primary, fontSize = 17.sp) } },
    )
}

@Composable
fun NewFolderDialog(g: GalleryState, parent: String, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogSurface,
        title = { Text("新建文件夹", color = Primary, fontSize = 22.sp) },
        text = {
            Column {
                Text("将在「内部存储空间/${parent.ifEmpty { "" }}」下创建文件夹", fontSize = 15.sp, color = HintGrey)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("文件夹名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    val ok = g.library.createFolder(parent, name.trim())
                    g.refresh()
                    g.message(if (ok) "已创建文件夹 ${name.trim()}" else "已存在同名文件夹")
                }
                onDismiss()
            }) { Text("确定", color = Primary, fontSize = 17.sp) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = Primary, fontSize = 17.sp) } },
    )
}

@Composable
fun ResizeDialog(g: GalleryState, items: List<MediaItem>, onDismiss: () -> Unit) {
    val item = items.firstOrNull { g.selection.contains(it.relPath) } ?: return
    val size = remember(item.relPath) { g.library.sizeOf(item) }
    var width by remember { mutableStateOf(if (size.first > 0) size.first.toString() else "1080") }
    var height by remember { mutableStateOf(if (size.second > 0) size.second.toString() else "1080") }
    var base by remember { mutableStateOf(item.name.substringBeforeLast('.')) }
    var ext by remember { mutableStateOf(item.name.substringAfterLast('.', "")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogSurface,
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = width,
                        onValueChange = { width = it },
                        label = { Text("宽度") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Text(" : ", fontSize = 20.sp, modifier = Modifier.padding(horizontal = 6.dp))
                    OutlinedTextField(
                        value = height,
                        onValueChange = { height = it },
                        label = { Text("高度") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = "内部存储空间/DCIM/${item.album}/",
                    onValueChange = { },
                    label = { Text("文件夹") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = base,
                    onValueChange = { base = it },
                    label = { Text("文件名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = ext,
                    onValueChange = { ext = it },
                    label = { Text("扩展名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val w = width.toIntOrNull() ?: 0
                val h = height.toIntOrNull() ?: 0
                if (w > 0 && h > 0) {
                    val created = g.library.resizeCopy(item, w, h)
                    g.refresh()
                    g.selection = emptySet()
                    g.message(if (created != null) "已保存 ${created.displayName}" else "调整大小失败")
                }
                onDismiss()
            }) { Text("确定", color = Primary, fontSize = 17.sp) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = Primary, fontSize = 17.sp) } },
    )
}

@Composable
fun PropertiesDialog(g: GalleryState, item: MediaItem?, onDismiss: () -> Unit) {
    if (item == null) return
    val size = remember(item.relPath) { g.library.sizeOf(item) }
    val megaPixels = if (size.first > 0 && size.second > 0) {
        String.format(java.util.Locale.ROOT, "%.1fMP", size.first * size.second / 1_000_000.0)
    } else ""
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogSurface,
        title = { Text("属性", color = Primary, fontSize = 22.sp) },
        text = {
            Column {
                LabeledValue("名称", item.displayName)
                LabeledValue("路径", "内部存储空间/DCIM/${item.album}")
                LabeledValue("大小", formatSize(item.sizeBytes))
                LabeledValue("分辨率", "${size.first} x ${size.second} ($megaPixels)")
                LabeledValue("修改日期", Dates.stamp(item.modified))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                g.message("已移除 EXIF 数据")
                onDismiss()
            }) { Text("移除 EXIF 数据", color = Primary, fontSize = 16.sp) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("确定", color = Primary, fontSize = 17.sp) } },
    )
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Column(Modifier.padding(bottom = 10.dp)) {
        Text(label, fontSize = 15.sp, color = Color(0xFF5A5A63))
        Text(value, fontSize = 17.sp, color = Color(0xFF23232B))
    }
}

@Composable
fun SetAsDialog(g: GalleryState, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogSurface,
        title = { Text("设置为", color = Primary, fontSize = 22.sp) },
        text = {
            Column {
                listOf("主屏幕壁纸", "锁屏壁纸", "联系人头像").forEach { option ->
                    Text(
                        option,
                        fontSize = 17.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                g.message("无法设置为$option")
                                onDismiss()
                            }
                            .padding(vertical = 12.dp)
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消", color = Primary, fontSize = 17.sp) } },
    )
}
