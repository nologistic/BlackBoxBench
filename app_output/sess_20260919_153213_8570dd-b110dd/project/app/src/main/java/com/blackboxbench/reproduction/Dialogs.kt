package com.blackboxbench.reproduction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------- shared sorting / grouping helpers ----------

fun sortedMedia(items: List<MediaItem>, mode: String, asc: Boolean): List<MediaItem> {
    val sorted = when (mode) {
        "名称" -> items.sortedBy { it.name }
        "路径" -> items.sortedBy { it.id }
        "大小" -> items.sortedBy { it.size }
        "修改日期" -> items.sortedBy { it.modified }
        "拍摄日期" -> items.sortedBy { it.modified }
        "扩展名" -> items.sortedBy { it.extension }
        "随机" -> items.shuffled()
        else -> items.sortedBy { it.modified }
    }
    return if (asc) sorted else sorted.reversed()
}

fun groupLabel(item: MediaItem, mode: String): String = when (mode) {
    "修改时间（按天）" -> formatDate(item.modified)
    "修改时间（按月）" -> {
        val sdf = java.text.SimpleDateFormat("yyyy年M月", java.util.Locale.CHINA)
        sdf.format(java.util.Date(item.modified))
    }
    "拍摄日期（按天）" -> formatDate(item.modified)
    "拍摄日期（按月）" -> {
        val sdf = java.text.SimpleDateFormat("yyyy年M月", java.util.Locale.CHINA)
        sdf.format(java.util.Date(item.modified))
    }
    "文件类型" -> if (item.isVideo) "视频" else "图片"
    "扩展名" -> item.extension.uppercase()
    else -> ""
}

// ---------- dialogs ----------

@Composable
fun SortDialog(current: String, asc: Boolean, onDismiss: () -> Unit, onApply: (String, Boolean) -> Unit) {
    val modes = listOf("名称", "路径", "大小", "项数", "修改日期", "拍摄日期", "随机")
    var mode by remember { mutableStateOf(if (modes.contains(current)) current else "修改日期") }
    var ascending by remember { mutableStateOf(asc) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("排序方式") },
        text = {
            Column {
                modes.forEach { m ->
                    Row(
                        Modifier.fillMaxWidth().clickable { mode = m },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = mode == m, onClick = { mode = m })
                        Text(m)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth().clickable { ascending = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = ascending, onClick = { ascending = true })
                    Text("升序")
                }
                Row(
                    Modifier.fillMaxWidth().clickable { ascending = false },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = !ascending, onClick = { ascending = false })
                    Text("降序")
                }
            }
        },
        confirmButton = { TextButton(onClick = { onApply(mode, ascending) }) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun FilterDialog(current: Set<String>, onDismiss: () -> Unit, onApply: (Set<String>) -> Unit) {
    val types = listOf("图片", "视频", "GIF", "RAW图像", "SVG", "竖向")
    var sel by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("过滤显示的文件") },
        text = {
            Column {
                types.forEach { t ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            sel = if (sel.contains(t)) sel - t else sel + t
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = sel.contains(t), onCheckedChange = {
                            sel = if (sel.contains(t)) sel - t else sel + t
                        })
                        Text(t)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onApply(sel) }) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun ViewTypeDialog(onDismiss: () -> Unit) {
    var grid by remember { mutableStateOf(true) }
    var groupByFolder by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("更改视图类型") },
        text = {
            Column {
                Row(Modifier.fillMaxWidth().clickable { grid = true }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = grid, onClick = { grid = true })
                    Text("网格")
                }
                Row(Modifier.fillMaxWidth().clickable { grid = false }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = !grid, onClick = { grid = false })
                    Text("列表")
                }
                Row(Modifier.fillMaxWidth().clickable { groupByFolder = !groupByFolder }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = groupByFolder, onCheckedChange = { groupByFolder = it })
                    Text("按实际文件夹分组")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun ColumnsDialog(current: Int, onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    var value by remember { mutableIntStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("列数") },
        text = {
            Column {
                Text("$value", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = value.toFloat(),
                    onValueChange = { value = it.toInt().coerceIn(1, 15) },
                    valueRange = 1f..15f,
                    steps = 13
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSelect(value) }) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun GroupDialog(current: String, asc: Boolean, onDismiss: () -> Unit, onApply: (String, Boolean) -> Unit) {
    val modes = listOf("不分组文件", "修改时间（按天）", "修改时间（按月）", "拍摄日期（按天）", "拍摄日期（按月）", "文件类型", "扩展名")
    var mode by remember { mutableStateOf(if (modes.contains(current)) current else "不分组文件") }
    var ascending by remember { mutableStateOf(asc) }
    var showCount by remember { mutableStateOf(false) }
    var onlyThisFolder by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("分组方式") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("请注意：分组和排序是两种相互独立的文件组织方式。", fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                modes.forEach { m ->
                    Row(Modifier.fillMaxWidth().clickable { mode = m }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = mode == m, onClick = { mode = m })
                        Text(m)
                    }
                }
                Row(Modifier.fillMaxWidth().clickable { ascending = true }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = ascending, onClick = { ascending = true })
                    Text("升序")
                }
                Row(Modifier.fillMaxWidth().clickable { ascending = false }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = !ascending, onClick = { ascending = false })
                    Text("降序")
                }
                Row(Modifier.fillMaxWidth().clickable { showCount = !showCount }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = showCount, onCheckedChange = { showCount = it })
                    Text("在栏目标题上显示文件数")
                }
                Row(Modifier.fillMaxWidth().clickable { onlyThisFolder = !onlyThisFolder }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = onlyThisFolder, onCheckedChange = { onlyThisFolder = it })
                    Text("仅应用于此文件夹")
                }
            }
        },
        confirmButton = { TextButton(onClick = { onApply(mode, ascending) }) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun TextInputDialog(title: String, initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true)
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun RenameMediaDialog(item: MediaItem, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(item.title) }
    var ext by remember { mutableStateOf(item.extension) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重命名") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("文件名") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = ext, onValueChange = { ext = it }, label = { Text("扩展名") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val full = if (ext.isBlank()) name else "$name.$ext"
                onConfirm(full)
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun NewFolderDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建文件夹") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("文件夹名称") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onCreate(name.trim()) }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun DeleteConfirmDialog(text: String, onDismiss: () -> Unit, onConfirm: (skipRecycleBin: Boolean) -> Unit) {
    var skip by remember { mutableStateOf(false) }
    var dontAsk by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除") },
        text = {
            Column {
                Text(text)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().clickable { dontAsk = !dontAsk }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = dontAsk, onCheckedChange = { dontAsk = it })
                    Text("本次会话不再询问", fontSize = 14.sp)
                }
                Row(Modifier.fillMaxWidth().clickable { skip = !skip }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = skip, onCheckedChange = { skip = it })
                    Text("跳过回收站，直接删除文件", fontSize = 14.sp)
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(skip) }) { Text("是") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("否") } }
    )
}

@Composable
fun InfoDialog(title: String, body: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body, fontSize = 14.sp) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("确定") } }
    )
}

@Composable
fun PropertiesDialog(repo: GalleryRepository, item: MediaItem, onDismiss: () -> Unit) {
    val body = buildString {
        append("名称：${item.name}\n")
        append("路径：/storage/emulated/0/${item.id}\n")
        append("大小：${formatSize(item.size)}\n")
        if (item.width > 0) append("分辨率：${item.width}x${item.height}\n")
        if (item.isVideo) append("时长：${formatDuration(item.durationSec)}\n")
        append("修改日期：${formatDateTime(item.modified)}")
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("属性") },
        text = {
            Column {
                Text(body, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Text("移除 EXIF 数据", color = androidx.compose.ui.graphics.Color(0xFF8C9FD0), fontSize = 14.sp)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("确定") } }
    )
}

@Composable
fun CopyMoveDialog(
    title: String,
    folders: List<String>,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var browsing by remember { mutableStateOf(false) }
    val shown = folders.filter { query.isBlank() || it.contains(query, true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (browsing) "选择文件夹" else title) },
        text = {
            if (browsing) {
                FolderBrowser(onPick = onPick)
            } else {
                Column {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("搜索文件夹") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(Modifier.heightIn(max = 300.dp)) {
                        items(shown) { name ->
                            Text(
                                name,
                                modifier = Modifier.fillMaxWidth().clickable { onPick(name) }.padding(vertical = 10.dp),
                                fontSize = 16.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "其他文件夹",
                        color = androidx.compose.ui.graphics.Color(0xFF8C9FD0),
                        modifier = Modifier.clickable { browsing = true }.padding(vertical = 8.dp)
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun FolderBrowser(onPick: (String) -> Unit) {
    Column {
        Text("内部存储空间", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(6.dp))
        val entries = listOf(
            "BlackBoxBench", "Products", "Covers", "Avatars", "Posts", "Clips",
            "DCIM", "Pictures", "Movies", "Download", "Documents"
        )
        LazyColumn(Modifier.heightIn(max = 280.dp)) {
            items(entries) { name ->
                Row(
                    Modifier.fillMaxWidth().clickable { onPick(name) }.padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(name, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun ExcludeDialog(folder: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    var level by remember { mutableIntStateOf(0) }
    val options = listOf(
        folder,
        "上级文件夹（包含子文件夹）",
        "内部存储空间（所有内容）"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("排除") },
        text = {
            Column {
                Text("排除的文件夹及其子文件夹将不再显示。是否也要排除上级文件夹？", fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                options.forEachIndexed { i, label ->
                    Row(Modifier.fillMaxWidth().clickable { level = i }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = level == i, onClick = { level = i })
                        Text(label, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun LockFolderDialog(onDismiss: () -> Unit) {
    var agreed by remember { mutableStateOf(false) }
    var tab by remember { mutableIntStateOf(0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (agreed) "锁定文件夹" else "免责声明") },
        text = {
            if (!agreed) {
                Text(
                    "文件夹锁定仅用于防止应用内的意外访问，并不能替代真正的加密。" +
                        "请不要将其用于保护敏感数据。继续操作即表示你理解并接受这一点。",
                    fontSize = 14.sp
                )
            } else {
                Column {
                    TabRow(selectedTabIndex = tab) {
                        Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("图案") })
                        Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("PIN码") })
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (tab == 0) "请绘制解锁图案" else "请输入 PIN 码",
                        fontSize = 14.sp
                    )
                }
            }
        },
        confirmButton = {
            if (!agreed) {
                TextButton(onClick = { agreed = true }) { Text("确定") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun ChangeCoverDialog(
    media: List<MediaItem>,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
    onDefault: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("更换封面图片") },
        text = {
            Column {
                Text("选择照片", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                LazyColumn(Modifier.heightIn(max = 260.dp)) {
                    items(media) { item ->
                        Text(
                            item.name,
                            modifier = Modifier.fillMaxWidth().clickable { onPick(item.id) }.padding(vertical = 8.dp),
                            fontSize = 15.sp
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "使用默认值",
                    color = androidx.compose.ui.graphics.Color(0xFF8C9FD0),
                    modifier = Modifier.clickable { onDefault() }.padding(vertical = 8.dp)
                )
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun ResizeDialog(item: MediaItem, onDismiss: () -> Unit, onConfirm: (width: Int, height: Int) -> Unit) {
    var w by remember { mutableStateOf(if (item.width > 0) item.width.toString() else "800") }
    var h by remember { mutableStateOf(if (item.height > 0) item.height.toString() else "600") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("调整大小") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = w,
                        onValueChange = { w = it.filter { c -> c.isDigit() } },
                        label = { Text("宽度") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Text("  x  ", fontSize = 16.sp)
                    OutlinedTextField(
                        value = h,
                        onValueChange = { h = it.filter { c -> c.isDigit() } },
                        label = { Text("高度") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text("文件夹：${item.folder}", fontSize = 13.sp)
                Text("文件名：${item.name}", fontSize = 13.sp)
                Text("扩展名：${item.extension}", fontSize = 13.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(w.toIntOrNull() ?: 0, h.toIntOrNull() ?: 0)
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
