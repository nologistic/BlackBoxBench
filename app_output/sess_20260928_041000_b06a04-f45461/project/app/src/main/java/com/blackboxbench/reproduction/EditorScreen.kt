package com.blackboxbench.reproduction

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@Composable
fun EditorScreen(state: GalleryState, itemId: String, onDone: () -> Unit) {
    val context = LocalContext.current
    val item = state.byId(itemId)
    if (item == null) { onDone(); return }

    val original = remember(itemId) { GalleryState.bitmap(context, item) }
    var bitmap by remember(itemId) { mutableStateOf(original) }
    var tool by remember { mutableStateOf("crop") }
    var ratio by remember { mutableStateOf("自由") }
    var menuOpen by remember { mutableStateOf(false) }
    var saveOpen by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }
    var fileName by remember { mutableStateOf(item.title + "_1") }
    var ext by remember { mutableStateOf(item.extension.ifBlank { "png" }) }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        // top bar
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButtonBox(onClick = { onDone() }) { IconBack(Palette.onSurface) }
            Text("编辑器", fontSize = 19.sp, color = Palette.onSurface,
                modifier = Modifier.weight(1f).padding(start = 8.dp))
            IconButtonBox(onClick = { saveOpen = true }) { IconCheck(Palette.onSurface) }
            IconButtonBox(onClick = {}) { IconShare(Palette.onSurface) }
            Box {
                IconButtonBox(onClick = { menuOpen = !menuOpen }) { IconDots(Palette.onSurface) }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("覆盖原文件", fontSize = 14.sp) },
                        onClick = { menuOpen = false; saveOpen = true })
                    DropdownMenuItem(text = { Text("编辑方式", fontSize = 14.sp) },
                        onClick = { menuOpen = false })
                }
            }
        }

        // canvas
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val b = bitmap
            if (b != null) {
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(b.width.toFloat() / b.height)) {
                    Image(b.asImageBitmap(), contentDescription = null,
                        contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                    if (tool == "crop") {
                        Canvas(Modifier.fillMaxSize()) {
                            val w = size.width; val h = size.height
                            val stroke = Stroke(2f)
                            val c = Palette.editorAccent
                            for (i in 1..2) {
                                drawLine(c, Offset(w * i / 3f, 0f), Offset(w * i / 3f, h), stroke.width)
                                drawLine(c, Offset(0f, h * i / 3f), Offset(w, h * i / 3f), stroke.width)
                            }
                            drawRect(Color(0x66000000), size = Size(w, h * 0.001f))
                        }
                    }
                }
            }
        }

        // aspect chips
        Row(
            Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            listOf("自由", "1:1", "4:3", "16:9", "其他").forEach { r ->
                Text(
                    r,
                    color = if (ratio == r) Palette.editorAccent else Color(0xFF7A7A7A),
                    fontSize = 15.sp,
                    fontWeight = if (ratio == r) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.clickable {
                        ratio = r
                        val b = bitmap ?: return@clickable
                        if (r != "自由") bitmap = cropToRatio(b, r)
                    }.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }

        // tool row
        Row(
            Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            EditorTool("rotate_left") { bitmap?.let { bitmap = rotate(it, -90f) } }
            EditorTool("flip") { bitmap?.let { bitmap = flipH(it) } }
            EditorTool("crop", selected = tool == "crop") { tool = "crop" }
            EditorTool("resize") { bitmap?.let { bitmap = rotate(it, 90f) } }
            EditorTool("flip_v") { bitmap?.let { bitmap = flipV(it) } }
        }

        // bottom row
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            EditorTool("add") { }
            EditorTool("rotate_right") { bitmap?.let { bitmap = rotate(it, 90f) } }
            EditorTool("wand") { bitmap?.let { bitmap = null; bitmap = it } }
        }
    }

    if (toast != null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(toast!!, color = Color.White, fontSize = 14.sp,
                modifier = Modifier.background(Color(0xCC333333), RoundedCornerShape(20.dp))
                    .padding(horizontal = 18.dp, vertical = 10.dp))
        }
    }

    if (saveOpen) {
        AlertDialog(
            onDismissRequest = { saveOpen = false },
            title = { Text("另存为") },
            text = {
                Column {
                    OutlinedTextField(value = item.albumPath + "/", onValueChange = {},
                        label = { Text("路径") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = fileName, onValueChange = { fileName = it },
                        label = { Text("文件名") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = ext, onValueChange = { ext = it },
                        label = { Text("扩展名") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val b = bitmap
                    val name = if (ext.isBlank()) fileName else "$fileName.$ext"
                    if (b != null) {
                        val out = File(context.filesDir, "media")
                        out.mkdirs()
                        val f = File(out, name)
                        f.outputStream().use { s ->
                            val fmt = if (ext.equals("jpg", true) || ext.equals("jpeg", true))
                                Bitmap.CompressFormat.JPEG else Bitmap.CompressFormat.PNG
                            b.compress(fmt, 92, s)
                        }
                        state.addItem(
                            item.copy(
                                id = item.id + "#edit" + System.currentTimeMillis(),
                                name = name,
                                uri = "file:" + f.absolutePath,
                                sizeBytes = f.length(),
                                favorite = false, hidden = false, deleted = false,
                            )
                        )
                        state.persist()
                    }
                    saveOpen = false
                    toast = "文件保存成功"
                    onDone()
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { saveOpen = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun EditorTool(kind: String, selected: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier.width(64.dp).height(56.dp).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(30.dp)) {
            val s = size.minDimension
            val c = if (selected) Palette.editorAccent else Color(0xFF5A5A5A)
            val sw = s * 0.09f
            when (kind) {
                "rotate_left", "rotate_right" -> {
                    drawArc(c, if (kind == "rotate_left") 120f else -60f, 260f, false,
                        Offset(0f, 0f), Size(s, s),
                        style = Stroke(sw, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                    drawCircle(c, s * 0.07f, Offset(s / 2, s / 2))
                }
                "crop" -> {
                    drawLine(c, Offset(s * 0.24f, s * 0.06f), Offset(s * 0.24f, s * 0.76f), sw)
                    drawLine(c, Offset(s * 0.24f, s * 0.76f), Offset(s * 0.94f, s * 0.76f), sw)
                    drawLine(c, Offset(s * 0.06f, s * 0.24f), Offset(s * 0.76f, s * 0.24f), sw)
                    drawLine(c, Offset(s * 0.76f, s * 0.24f), Offset(s * 0.76f, s * 0.94f), sw)
                }
                "flip" -> {
                    drawLine(c, Offset(s * 0.5f, s * 0.08f), Offset(s * 0.5f, s * 0.92f), sw)
                    drawLine(c, Offset(s * 0.12f, s * 0.36f), Offset(s * 0.38f, s * 0.5f), sw)
                    drawLine(c, Offset(s * 0.12f, s * 0.64f), Offset(s * 0.38f, s * 0.5f), sw)
                    drawLine(c, Offset(s * 0.88f, s * 0.36f), Offset(s * 0.62f, s * 0.5f), sw)
                    drawLine(c, Offset(s * 0.88f, s * 0.64f), Offset(s * 0.62f, s * 0.5f), sw)
                }
                "flip_v" -> {
                    drawLine(c, Offset(s * 0.08f, s * 0.5f), Offset(s * 0.92f, s * 0.5f), sw)
                    drawLine(c, Offset(s * 0.36f, s * 0.12f), Offset(s * 0.5f, s * 0.38f), sw)
                    drawLine(c, Offset(s * 0.64f, s * 0.12f), Offset(s * 0.5f, s * 0.38f), sw)
                    drawLine(c, Offset(s * 0.36f, s * 0.88f), Offset(s * 0.5f, s * 0.62f), sw)
                    drawLine(c, Offset(s * 0.64f, s * 0.88f), Offset(s * 0.5f, s * 0.62f), sw)
                }
                "resize" -> {
                    drawRect(c, Offset(s * 0.08f, s * 0.08f), Size(s * 0.84f, s * 0.84f), style = Stroke(sw))
                    drawLine(c, Offset(s * 0.4f, s * 0.6f), Offset(s * 0.62f, s * 0.38f), sw)
                    drawLine(c, Offset(s * 0.62f, s * 0.38f), Offset(s * 0.62f, s * 0.6f), sw)
                    drawLine(c, Offset(s * 0.62f, s * 0.38f), Offset(s * 0.4f, s * 0.38f), sw)
                }
                "add" -> {
                    drawRoundRect(c, Offset(s * 0.08f, s * 0.18f), Size(s * 0.84f, s * 0.64f),
                        androidx.compose.ui.geometry.CornerRadius(s * 0.1f), style = Stroke(sw))
                    drawCircle(c, s * 0.09f, Offset(s * 0.5f, s * 0.5f))
                }
                "wand" -> {
                    drawLine(c, Offset(s * 0.18f, s * 0.82f), Offset(s * 0.7f, s * 0.3f), sw)
                    drawLine(c, Offset(s * 0.66f, s * 0.12f), Offset(s * 0.9f, s * 0.36f), sw * 0.8f)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Bitmap helpers
// ---------------------------------------------------------------------------

fun rotate(src: Bitmap, degrees: Float): Bitmap {
    val m = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
}

fun flipH(src: Bitmap): Bitmap {
    val m = Matrix().apply { postScale(-1f, 1f) }
    return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
}

fun flipV(src: Bitmap): Bitmap {
    val m = Matrix().apply { postScale(1f, -1f) }
    return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
}

fun cropToRatio(src: Bitmap, ratio: String): Bitmap {
    val parts = ratio.split(":")
    val rw = parts.getOrNull(0)?.toFloatOrNull() ?: return src
    val rh = parts.getOrNull(1)?.toFloatOrNull() ?: return src
    val target = rw / rh
    val current = src.width.toFloat() / src.height
    return if (current > target) {
        val w = (src.height * target).toInt().coerceAtLeast(1)
        val x = (src.width - w) / 2
        Bitmap.createBitmap(src, x, 0, w, src.height)
    } else {
        val h = (src.width / target).toInt().coerceAtLeast(1)
        val y = (src.height - h) / 2
        Bitmap.createBitmap(src, 0, y, src.width, h)
    }
}
