package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay

@Composable
fun SortDialog(onDismiss: () -> Unit) {
    var sort by remember { mutableStateOf("名称") }
    var ascending by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("排序依据") },
        text = {
            Column {
                listOf("名称", "路径", "大小", "最后修改", "拍摄日期").forEach { item ->
                    Row(
                        Modifier.fillMaxWidth().clickable { sort = item }.padding(vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(sort == item, { sort = item })
                        Text(item, Modifier.padding(start = 8.dp), fontSize = 16.sp)
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(ascending, { ascending = it })
                    Text("升序")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun ColumnDialog(current: Int, choose: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("每行列数") },
        text = {
            Column {
                (1..5).forEach { n ->
                    Row(
                        Modifier.fillMaxWidth().clickable { choose(n) }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(n == current, { choose(n) })
                        Text(n.toString() + " 列", Modifier.padding(start = 10.dp))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun NewFolderDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建文件夹") },
        text = {
            Column {
                Text("文件夹会创建在应用的图库目录中", color = Color.Gray, fontSize = 13.sp)
                OutlinedTextField(value, { value = it }, Modifier.fillMaxWidth().padding(top = 14.dp), label = { Text("文件夹名称") }, singleLine = true)
            }
        },
        confirmButton = { TextButton(onClick = { if (value.isNotBlank()) onCreate(value.trim()) }, enabled = value.isNotBlank()) { Text("创建") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun SlideshowDialog(onDismiss: () -> Unit, onStart: () -> Unit) {
    var interval by remember { mutableFloatStateOf(5f) }
    var loop by remember { mutableStateOf(true) }
    var random by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("幻灯片放映") },
        text = {
            Column {
                Text("切换间隔：" + interval.toInt() + " 秒", fontSize = 16.sp)
                Slider(interval, { interval = it }, valueRange = 2f..15f, steps = 12)
                ToggleLine("循环播放", loop) { loop = it }
                ToggleLine("随机顺序", random) { random = it }
            }
        },
        confirmButton = { TextButton(onClick = onStart) { Text("开始") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun ToggleLine(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked, onChecked)
    }
}

@Composable
fun DeleteDialog(onDismiss: () -> Unit, onDelete: () -> Unit) {
    var noAsk by remember { mutableStateOf(false) }
    var permanent by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (permanent) "永久删除？" else "移至回收站？") },
        text = {
            Column {
                Text(if (permanent) "该操作无法撤销。" else "所选项目将保留 30 天，之后永久删除。")
                Row(Modifier.fillMaxWidth().clickable { permanent = !permanent }.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(permanent, { permanent = it })
                    Text("直接永久删除")
                }
                Row(Modifier.fillMaxWidth().clickable { noAsk = !noAsk }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(noAsk, { noAsk = it })
                    Text("不再询问")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDelete) { Text("删除", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun InfoDialog(index: Int, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("详细信息") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoLine("名称", "IMG_2026_PUBLIC_" + (index + 1) + ".jpg")
                InfoLine("路径", "/Pictures/Camera/")
                InfoLine("分辨率", "1920 × 1080")
                InfoLine("大小", (1.4 + index / 10.0).toString().take(3) + " MB")
                InfoLine("修改时间", "2026年9月21日 12:30")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

@Composable
fun InfoLine(label: String, value: String) {
    Column {
        Text(label, color = Color.Gray, fontSize = 12.sp)
        Text(value, fontSize = 16.sp)
    }
}

@Composable
fun FolderActionsDialog(locked: Boolean, onDismiss: () -> Unit, onAction: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("文件夹操作") },
        text = {
            Column {
                DialogAction("▶  幻灯片放映") { onDismiss() }
                DialogAction("✎  重命名") { onDismiss() }
                DialogAction("▣  " + if (locked) "解锁文件夹" else "锁定文件夹") { onAction(if (locked) "unlock" else "lock") }
                DialogAction("◉  隐藏文件夹") { onAction("hide") }
                DialogAction("⌫  删除文件夹") { onDismiss() }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun DialogAction(text: String, onClick: () -> Unit) {
    Text(text, Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp), fontSize = 17.sp)
}

@Composable
fun LockSetupDialog(onDismiss: () -> Unit, onComplete: (String) -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var usePattern by remember { mutableStateOf(false) }
    val valid = pin.length >= 4 && pin == confirm
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(when (step) {
                0 -> "锁定文件夹"
                1 -> "选择锁定方式"
                else -> if (usePattern) "设置图案" else "设置 PIN 码"
            })
        },
        text = {
            when (step) {
                0 -> Column {
                    Text("此功能只在应用内提供保护。")
                    Text("它不会对系统中的文件进行加密，其他拥有存储访问权限的应用仍可能看到这些文件。", Modifier.padding(top = 12.dp), color = Color.Gray)
                }
                1 -> Column {
                    Row(Modifier.fillMaxWidth().clickable { usePattern = true; step = 2 }.padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("⌁", fontSize = 25.sp, color = Purple)
                        Column(Modifier.padding(start = 16.dp)) { Text("图案", fontSize = 17.sp); Text("连接至少 4 个点", color = Color.Gray, fontSize = 13.sp) }
                    }
                    Row(Modifier.fillMaxWidth().clickable { usePattern = false; step = 2 }.padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("●", fontSize = 25.sp, color = Purple)
                        Column(Modifier.padding(start = 16.dp)) { Text("PIN 码", fontSize = 17.sp); Text("使用 4 位或更多数字", color = Color.Gray, fontSize = 13.sp) }
                    }
                }
                else -> if (usePattern) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("绘制解锁图案")
                        Text("●   ●   ●\n\n●   ●   ●\n\n●   ●   ●", Modifier.padding(30.dp), fontSize = 25.sp)
                        Text("为简化设置，将使用默认图案", color = Color.Gray, fontSize = 13.sp)
                    }
                } else {
                    Column {
                        OutlinedTextField(
                            pin, { if (it.length <= 8 && it.all(Char::isDigit)) pin = it },
                            Modifier.fillMaxWidth(), label = { Text("输入 PIN 码") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true
                        )
                        OutlinedTextField(
                            confirm, { if (it.length <= 8 && it.all(Char::isDigit)) confirm = it },
                            Modifier.fillMaxWidth().padding(top = 12.dp), label = { Text("再次输入 PIN 码") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true,
                            isError = confirm.isNotEmpty() && pin != confirm
                        )
                        if (confirm.isNotEmpty() && pin != confirm) Text("两次输入的 PIN 码不一致", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            when (step) {
                0 -> TextButton(onClick = { step = 1 }) { Text("继续") }
                1 -> {}
                else -> TextButton(
                    onClick = { onComplete(if (usePattern) "1234" else pin) },
                    enabled = usePattern || valid
                ) { Text("完成") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun ToastNotice(text: String, onDone: () -> Unit) {
    LaunchedEffect(text) { delay(2200); onDone() }
    Box(Modifier.fillMaxSize().padding(bottom = 90.dp), contentAlignment = Alignment.BottomCenter) {
        Surface(color = Color(0xFF303038), shape = RoundedCornerShape(24.dp), shadowElevation = 5.dp) {
            Text(text, Modifier.padding(horizontal = 22.dp, vertical = 12.dp), color = Color.White)
        }
    }
}
