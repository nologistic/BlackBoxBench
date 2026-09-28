package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TextInputDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontSize = 20.sp, color = OnSurface) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) {
                Text(confirmLabel, color = Primary, fontSize = 15.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Primary, fontSize = 15.sp)
            }
        },
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(title, fontSize = 17.sp, color = OnSurface) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("确定", color = Primary, fontSize = 15.sp) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = Primary, fontSize = 15.sp) }
        },
    )
}

@Composable
fun CompressDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    var format by remember { mutableStateOf("zip") }
    var password by remember { mutableStateOf("") }
    var reveal by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("创建压缩文件", fontSize = 20.sp, color = OnSurface) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                listOf(".zip", ".tar.xz", ".7z").forEach { option ->
                    Row(
                        Modifier.fillMaxWidth().height(40.dp).clickable { format = option },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = format == option, onClick = { format = option })
                        Text(option, fontSize = 16.sp, color = OnSurface)
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    singleLine = true,
                    label = { Text("密码（可选）", fontSize = 13.sp) },
                    visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        Box(
                            Modifier.size(40.dp).clickable { reveal = !reveal },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (reveal) EyeGlyph(Modifier.size(22.dp), OnSurfaceVariant)
                            else EyeOffGlyph(Modifier.size(22.dp), OnSurfaceVariant)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }) { Text("确定", color = Primary, fontSize = 15.sp) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = Primary, fontSize = 15.sp) }
        },
    )
}

@Composable
fun PropertiesDialog(node: FsNode, onDismiss: () -> Unit) {
    var tab by remember { mutableStateOf(0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${node.name}属性", fontSize = 20.sp, color = OnSurface) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(Modifier.fillMaxWidth()) {
                    listOf("基本", "权限").forEachIndexed { index, label ->
                        Column(
                            Modifier
                                .weight(1f)
                                .clickable { tab = index }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                label,
                                fontSize = 15.sp,
                                color = if (tab == index) Primary else OnSurfaceVariant,
                                fontWeight = if (tab == index) FontWeight.Medium else FontWeight.Normal,
                            )
                            Spacer(Modifier.height(6.dp))
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(if (tab == index) Primary else androidx.compose.ui.graphics.Color.Transparent)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (tab == 0) {
                    val visible = node.children.orEmpty().count { !it.hidden }
                    PropertyRow("名称", node.name)
                    PropertyRow("父文件夹", parentPathOf(node))
                    PropertyRow("类型", if (node.isDir) "文件夹" else mimeOf(node))
                    if (node.isDir) PropertyRow("内容", "$visible 个项目")
                    PropertyRow("大小", formatSize(nodeTotalSize(node)))
                    PropertyRow("最后修改", formatFullDate(nodeMtime(node)))
                } else {
                    PropertyRow("权限", if (node.isDir) "drwxrwx--x" else "-rw-rw----")
                    PropertyRow("所有者", "0")
                    PropertyRow("群组", "1015")
                    PropertyRow("模式", if (node.isDir) "0771" else "0660")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("确定", color = Primary, fontSize = 15.sp) }
        },
    )
}

@Composable
private fun PropertyRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(label, fontSize = 14.sp, color = OnSurfaceVariant, modifier = Modifier.width(88.dp))
        Text(value, fontSize = 14.sp, color = OnSurface, modifier = Modifier.weight(1f))
    }
}

private fun parentPathOf(node: FsNode): String = "/storage/emulated/0"

fun nodeTotalSize(node: FsNode): Long =
    if (node.size > 0L) node.size else node.children.orEmpty().sumOf { nodeTotalSize(it) }

fun nodeMtime(node: FsNode): Long =
    if (node.mtime > 0L) node.mtime
    else node.children.orEmpty().maxOfOrNull { nodeMtime(it) } ?: 0L

fun mimeOf(node: FsNode): String = when (node.kind) {
    FileKind.DIR -> "vnd.android.document/directory"
    FileKind.IMAGE -> "image/png"
    FileKind.VIDEO -> "video/mp4"
    FileKind.AUDIO -> "audio/wav"
    FileKind.ARCHIVE -> "application/zip"
    FileKind.APK -> "application/vnd.android.package-archive"
    FileKind.DOCUMENT -> "text/plain"
    FileKind.OTHER -> "application/octet-stream"
}

@Composable
fun EyeGlyph(modifier: Modifier = Modifier.size(22.dp), tint: androidx.compose.ui.graphics.Color) {
    Glyph(modifier) {
        val w = size.width
        val h = size.height
        val sw = w * 0.08f
        val p = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.1f, h * 0.5f)
            cubicTo(w * 0.3f, h * 0.2f, w * 0.7f, h * 0.2f, w * 0.9f, h * 0.5f)
            cubicTo(w * 0.7f, h * 0.8f, w * 0.3f, h * 0.8f, w * 0.1f, h * 0.5f)
            close()
        }
        drawPath(p, tint, style = androidx.compose.ui.graphics.drawscope.Stroke(sw))
        drawCircle(tint, w * 0.11f, center)
    }
}

@Composable
fun EyeOffGlyph(modifier: Modifier = Modifier.size(22.dp), tint: androidx.compose.ui.graphics.Color) {
    Glyph(modifier) {
        val w = size.width
        val h = size.height
        val sw = w * 0.08f
        val p = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.1f, h * 0.5f)
            cubicTo(w * 0.3f, h * 0.2f, w * 0.7f, h * 0.2f, w * 0.9f, h * 0.5f)
            cubicTo(w * 0.7f, h * 0.8f, w * 0.3f, h * 0.8f, w * 0.1f, h * 0.5f)
            close()
        }
        drawPath(p, tint, style = androidx.compose.ui.graphics.drawscope.Stroke(sw))
        drawCircle(tint, w * 0.11f, center)
        drawLine(tint, androidx.compose.ui.geometry.Offset(w * 0.18f, h * 0.82f), androidx.compose.ui.geometry.Offset(w * 0.82f, h * 0.18f), strokeWidth = sw, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
}
