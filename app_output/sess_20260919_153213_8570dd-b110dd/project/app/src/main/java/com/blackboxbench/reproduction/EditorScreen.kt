package com.blackboxbench.reproduction

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EditorScreen(app: AppState, mediaId: String, toast: (String) -> Unit) {
    val repo = app.repo
    val item = repo.items.firstOrNull { it.id == mediaId }
    if (item == null) {
        app.pop()
        return
    }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var loaded by remember { mutableStateOf(false) }
    if (!loaded) {
        bitmap = repo.fullBitmap(item)
        loaded = true
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { app.pop() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = Color.White)
                }
                Text("编辑", color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    bitmap?.let {
                        repo.saveEditedCopy(item, it, "_edited")
                        toast("已保存副本")
                        app.pop()
                    }
                }) {
                    Text("保存", color = Color.White)
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                EditorButton("旋转 90°") {
                    bitmap = bitmap?.let { transform(it, Matrix().apply { postRotate(90f) }) }
                }
                EditorButton("左右翻转") {
                    bitmap = bitmap?.let { transform(it, Matrix().apply { preScale(-1f, 1f) }) }
                }
                EditorButton("上下翻转") {
                    bitmap = bitmap?.let { transform(it, Matrix().apply { preScale(1f, -1f) }) }
                }
                EditorButton("裁剪 1:1") {
                    bitmap = bitmap?.let { src ->
                        val side = minOf(src.width, src.height)
                        Bitmap.createBitmap(src, (src.width - side) / 2, (src.height - side) / 2, side, side)
                    }
                }
            }
        }
    }
}

private fun transform(src: Bitmap, m: Matrix): Bitmap =
    Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)

@Composable
private fun EditorButton(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(label, color = Color.White, fontSize = 13.sp)
    }
}
