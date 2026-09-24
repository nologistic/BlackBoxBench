package com.blackboxbench.reproduction

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@Composable
fun RadioListDialog(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = GalleryPrimary, fontSize = 20.sp) },
        text = {
            Column(modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState())) {
                options.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(option) }.padding(vertical = 6.dp)
                    ) {
                        RadioButton(selected = option == selected, onClick = { onSelect(option) })
                        Text(option, fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun ColumnsDialog(current: Int, onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("列数", color = GalleryPrimary, fontSize = 20.sp) },
        text = {
            Column(modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState())) {
                (1..15).forEach { count ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(count) }.padding(vertical = 4.dp)
                    ) {
                        RadioButton(selected = count == current, onClick = { onSelect(count) })
                        Text("$count 列", fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun ViewTypeDialog(
    viewType: String,
    groupByFolder: Boolean,
    onConfirm: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var type by remember { mutableStateOf(viewType) }
    var grouped by remember { mutableStateOf(groupByFolder) }
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column {
                listOf("网格", "列表").forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { type = option }.padding(vertical = 6.dp)
                    ) {
                        RadioButton(selected = type == option, onClick = { type = option })
                        Text(option, fontSize = 16.sp)
                    }
                }
                HorizontalDivider(color = GalleryDivider)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { grouped = !grouped }.padding(vertical = 6.dp)
                ) {
                    Checkbox(checked = grouped, onCheckedChange = { grouped = it })
                    Text("按照实际文件夹分组", fontSize = 16.sp)
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(type, grouped) }) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun FilterFilesDialog(
    images: Boolean, videos: Boolean, gif: Boolean, raw: Boolean, svg: Boolean, portrait: Boolean,
    onConfirm: (Boolean, Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var i by remember { mutableStateOf(images) }
    var v by remember { mutableStateOf(videos) }
    var g by remember { mutableStateOf(gif) }
    var r by remember { mutableStateOf(raw) }
    var s by remember { mutableStateOf(svg) }
    var p by remember { mutableStateOf(portrait) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("过滤显示的文件", color = GalleryPrimary, fontSize = 20.sp) },
        text = {
            Column {
                listOf(
                    "图片" to (i to { value: Boolean -> i = value }),
                    "视频" to (v to { value: Boolean -> v = value }),
                    "GIF" to (g to { value: Boolean -> g = value }),
                    "RAW 图像" to (r to { value: Boolean -> r = value }),
                    "SVG" to (s to { value: Boolean -> s = value }),
                    "竖向" to (p to { value: Boolean -> p = value })
                ).forEach { (label, pair) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { pair.second(!pair.first) }.padding(vertical = 4.dp)
                    ) {
                        Checkbox(checked = pair.first, onCheckedChange = pair.second)
                        Text(label, fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(i, v, g, r, s, p) }) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun TextFieldDialog(
    title: String,
    label: String,
    initial: String,
    confirmLabel: String = "确定",
    extraLabel: String? = null,
    extraInitial: String = "",
    numeric: Boolean = false,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    var extra by remember { mutableStateOf(extraInitial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = GalleryPrimary, fontSize = 20.sp) },
        text = {
            Column {
                OutlinedTextField(
                    value = text, onValueChange = { text = it }, label = { Text(label) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = if (numeric) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default
                )
                if (extraLabel != null) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = extra, onValueChange = { extra = it }, label = { Text(extraLabel) },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(text, extra) }) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun PropertiesDialog(item: MediaItem, path: String, onRemoveExif: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("属性", color = GalleryPrimary, fontSize = 20.sp) },
        text = {
            Column {
                PropertyRow("名称", item.fileName)
                PropertyRow("路径", path)
                PropertyRow("大小", formatSize(item.sizeBytes))
                if (!item.isVideo) PropertyRow("分辨率", "${item.width} x ${item.height} (${"%.1f".format(item.width * item.height / 1_000_000.0)}MP)")
                PropertyRow("修改日期", formatDateTime(item.modifiedMs))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("确定") } },
        dismissButton = { TextButton(onClick = onRemoveExif) { Text("移除 EXIF 数据") } }
    )
}

@Composable
private fun PropertyRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(label, color = GallerySecondaryText, fontSize = 13.sp)
        Text(value, fontSize = 16.sp, color = GalleryText)
    }
}

@Composable
fun DeleteConfirmDialog(
    count: Int,
    noAskAgain: Boolean,
    onNoAskAgainChange: (Boolean) -> Unit,
    skipTrash: Boolean,
    onSkipTrashChange: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column {
                Text(if (count > 1) "要删除 $count 个项目吗？" else "要删除该项目吗？", fontSize = 16.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = noAskAgain, onCheckedChange = onNoAskAgainChange)
                    Text("本次会话不再询问", fontSize = 15.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = skipTrash, onCheckedChange = onSkipTrashChange)
                    Text("跳过回收站，直接删除文件", fontSize = 15.sp)
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("是") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("否") } }
    )
}

/** "选择目标" dialog: album tiles with a search field and an "other folders" escape hatch. */
@Composable
fun TargetPickerDialog(
    albums: List<Album>,
    coverOf: (Album) -> MediaItem?,
    fileOf: (MediaItem) -> File,
    onPick: (String) -> Unit,
    onOtherFolders: () -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择目标", color = GalleryPrimary, fontSize = 20.sp) },
        text = {
            Column {
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    placeholder = { Text("搜索文件夹") }, singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Check, contentDescription = null, tint = Color.Transparent) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                val shown = albums.filter { query.isBlank() || it.name.contains(query, true) }
                LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.heightIn(max = 460.dp)) {
                    items(shown) { album ->
                        Column(
                            modifier = Modifier.padding(4.dp).clickable { onPick(album.path) }
                        ) {
                            val cover = coverOf(album)
                            Box(
                                modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(2.dp))
                                    .background(Color(0xFF2A2A2A))
                            ) {
                                if (cover != null) MediaThumb(fileOf(cover), cover, Modifier.fillMaxWidth().height(110.dp))
                                Text(
                                    album.name, color = Color.White, fontSize = 13.sp,
                                    modifier = Modifier.align(Alignment.BottomStart).padding(4.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onOtherFolders) { Text("其他文件夹") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

/** Directory browser used by "other folders"; tapping a row enters it, OK picks the current dir. */
@Composable
fun FolderBrowserDialog(
    rootLabel: String,
    root: File,
    onPick: (String) -> Unit,
    onCreateFolder: (File) -> Unit,
    onDismiss: () -> Unit
) {
    var current by remember { mutableStateOf(root) }
    val crumbs = remember(current) {
        val list = mutableListOf<File>()
        var node: File? = current
        while (node != null && node.path.startsWith(root.path)) { list.add(0, node); node = node.parentFile }
        list
    }
    val children = remember(current) {
        (current.listFiles() ?: emptyArray()).sortedBy { it.name.lowercase() }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择文件夹", color = GalleryPrimary, fontSize = 20.sp) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                    crumbs.forEachIndexed { index, file ->
                        if (index > 0) Text(" › ", color = GalleryPrimary, fontSize = 14.sp)
                        Text(
                            if (index == 0) rootLabel else file.name,
                            color = GalleryPrimary, fontSize = 14.sp,
                            modifier = Modifier.clickable { current = file }
                        )
                    }
                }
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(children) { child ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable {
                                if (child.isDirectory) current = child else onPick(child.parentFile?.path ?: current.path)
                            }.padding(vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(4.dp))
                                    .background(if (child.isDirectory) Color(0xFF9AA6D6) else Color(0xFFB7CFA8))
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(child.name, fontSize = 15.sp)
                                Text(if (child.isDirectory) "文件夹" else formatSize(child.length()), fontSize = 12.sp, color = GallerySecondaryText)
                            }
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Surface(
                        color = GalleryPrimary, shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(40.dp).clickable { onCreateFolder(current) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, contentDescription = "新建文件夹", tint = Color.White)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onPick(current.path) }) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun SlideshowDialog(onStart: (Int, Boolean, Boolean, Boolean, Boolean) -> Unit, onDismiss: () -> Unit) {
    var interval by remember { mutableStateOf("5") }
    var animation by remember { mutableStateOf("滑动") }
    var includeVideos by remember { mutableStateOf(false) }
    var includeGif by remember { mutableStateOf(false) }
    var random by remember { mutableStateOf(false) }
    var reverse by remember { mutableStateOf(false) }
    var loop by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("间隔", fontSize = 16.sp, modifier = Modifier.weight(1f))
                    OutlinedTextField(
                        value = interval, onValueChange = { interval = it.filter { c -> c.isDigit() } },
                        label = { Text("秒") }, singleLine = true, modifier = Modifier.width(140.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable {
                        animation = if (animation == "滑动") "淡入淡出" else "滑动"
                    }
                ) {
                    Text("动画", fontSize = 16.sp, modifier = Modifier.weight(1f))
                    Text(animation, fontSize = 16.sp, color = GallerySecondaryText)
                }
                CheckRow("包含视频", includeVideos) { includeVideos = it }
                CheckRow("包含GIF", includeGif) { includeGif = it }
                CheckRow("随机顺序", random) { random = it }
                CheckRow("倒序播放", reverse) { reverse = it }
                CheckRow("循环播放幻灯片", loop) { loop = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onStart(interval.toIntOrNull() ?: 5, includeVideos, includeGif, random, loop) }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun CheckRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 4.dp)
    ) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label, fontSize = 16.sp)
    }
}

@Composable
fun SetAsDialog(onPick: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置为", color = GalleryPrimary, fontSize = 20.sp) },
        text = {
            Column {
                listOf("主屏幕壁纸", "锁屏壁纸", "主屏幕和锁屏", "联系人头像").forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { onPick(option) }.padding(vertical = 10.dp)
                    ) { Text(option, fontSize = 16.sp) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun ResizeDialog(item: MediaItem, onConfirm: (Int, Int, String, String) -> Unit, onDismiss: () -> Unit) {
    var width by remember { mutableStateOf(item.width.toString()) }
    var height by remember { mutableStateOf(item.height.toString()) }
    var folder by remember { mutableStateOf(item.albumPath + "/") }
    var name by remember { mutableStateOf(item.fileName.substringBeforeLast('.')) }
    var ext by remember { mutableStateOf(item.fileName.substringAfterLast('.', "jpg")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = width, onValueChange = { width = it.filter { c -> c.isDigit() } }, label = { Text("宽度") }, singleLine = true, modifier = Modifier.weight(1f))
                    Text("  :  ", fontSize = 20.sp)
                    OutlinedTextField(value = height, onValueChange = { height = it.filter { c -> c.isDigit() } }, label = { Text("高度") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = folder, onValueChange = { folder = it }, label = { Text("文件夹") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("文件名") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = ext, onValueChange = { ext = it }, label = { Text("扩展名") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(width.toIntOrNull() ?: item.width, height.toIntOrNull() ?: item.height, folder.trimEnd('/'), "$name.$ext")
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun SaveAsDialog(defaultFolder: String, defaultName: String, onConfirm: (String, String) -> Unit, onDismiss: () -> Unit) {
    var folder by remember { mutableStateOf(defaultFolder) }
    var name by remember { mutableStateOf(defaultName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column {
                OutlinedTextField(value = folder, onValueChange = { folder = it }, label = { Text("路径") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("文件名") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(folder, name) }) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun MessageDialog(title: String, text: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { if (title.isNotEmpty()) Text(title, color = GalleryPrimary, fontSize = 20.sp) },
        text = { Text(text, fontSize = 16.sp) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("确定") } }
    )
}

@Composable
fun AlbumPickerRow(name: String, count: Int, cover: MediaItem?, file: File?, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp)
    ) {
        if (cover != null && file != null) {
            MediaThumb(file, cover, Modifier.size(56.dp).clip(RoundedCornerShape(4.dp)))
        } else {
            Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF9AA6D6)))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(name, fontSize = 16.sp, fontWeight = FontWeight.Normal)
            Text("$count 项", fontSize = 13.sp, color = GallerySecondaryText)
        }
    }
}

@Composable
fun AdminPlaceholder() {
    Spacer(Modifier.height(0.dp))
}
