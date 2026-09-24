@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TrashScreen(c: AppController) {
    c.settings.revision.value
    val items = c.repo.trashedItems()
    var menuOpen by remember { mutableStateOf(false) }
    var selection by remember { mutableStateOf(setOf<String>()) }
    var preview by remember { mutableStateOf<MediaItem?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }
    var confirmEmpty by remember { mutableStateOf(false) }
    val selectionMode = selection.isNotEmpty()

    Column(modifier = Modifier.fillMaxSize().background(GalleryBackground)) {
        if (selectionMode) {
            SelectionBar(
                count = selection.size,
                onBack = { selection = emptySet() },
                onDelete = {
                    items.filter { selection.contains(it.id) }.forEach { c.repo.deletePermanently(it) }
                    selection = emptySet(); toast = "已永久删除"
                },
                onShare = { toast = "已分享 ${selection.size} 个项目" },
                onInfo = { toast = "已选择 ${selection.size} 个项目" },
                onMenu = { selection = items.map { it.id }.toSet() },
                menu = { }
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().background(GalleryTopBar).padding(4.dp)) {
                IconButton(onClick = { c.pop() }) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
                Text("回收站", fontSize = 20.sp, modifier = Modifier.weight(1f).padding(start = 8.dp))
                Box {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, contentDescription = "更多") }
                    MenuEntries(
                        entries = listOf(
                            "清空回收站" to { confirmEmpty = true },
                            "清空并禁用回收站" to {
                                c.repo.emptyTrash()
                                c.settings.trashEnabled.set(false)
                                c.repo.trashDisabled = true
                                toast = "回收站已清空并禁用"
                            },
                            "恢复所有文件" to { c.repo.restoreAll(); toast = "已恢复所有文件" }
                        ),
                        expanded = menuOpen, onDismiss = { menuOpen = false }
                    )
                }
            }
        }

        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("未找到任何项目。", color = GallerySecondaryText, fontSize = 16.sp)
            }
        } else {
            val columns = c.settings.columns.get().coerceIn(1, 15)
            LazyVerticalGrid(columns = GridCells.Fixed(columns), modifier = Modifier.fillMaxSize()) {
                items(items, key = { it.id }) { item ->
                    Box(
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f).background(Color(0xFF202020))
                            .combinedClickable(
                                onClick = { if (selectionMode) selection = if (selection.contains(item.id)) selection - item.id else selection + item.id else preview = item },
                                onLongClick = { selection = selection + item.id }
                            )
                    ) {
                        MediaThumb(c.repo.fileOf(item), item, Modifier.fillMaxSize())
                        Text(
                            item.fileName, color = Color.White, fontSize = 13.sp, maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.align(Alignment.BottomStart).padding(6.dp)
                        )
                        if (selectionMode) {
                            Box(
                                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(26.dp).clip(CircleShape)
                                    .background(if (selection.contains(item.id)) GalleryPrimary else Color(0x66000000)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selection.contains(item.id)) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    preview?.let { item ->
        var previewMenu by remember { mutableStateOf(false) }
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            MediaThumb(c.repo.fileOf(item), item, Modifier.fillMaxSize(), target = 1400, contentScale = ContentScale.Fit)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().background(Color(0xCC000000)).padding(4.dp)
            ) {
                IconButton(onClick = { preview = null }) { Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Color.White) }
                Text(item.fileName, color = Color.White, fontSize = 18.sp, modifier = Modifier.weight(1f))
                Box {
                    IconButton(onClick = { previewMenu = true }) { Icon(Icons.Default.MoreVert, contentDescription = "更多", tint = Color.White) }
                    MenuEntries(
                        entries = listOf(
                            "恢复此文件" to { c.repo.restore(item); preview = null; toast = "已恢复" },
                            "删除" to { c.repo.deletePermanently(item); preview = null; toast = "已永久删除" }
                        ),
                        expanded = previewMenu, onDismiss = { previewMenu = false }
                    )
                }
            }
        }
    }

    if (confirmEmpty) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmEmpty = false },
            text = { Text("要清空回收站吗？", fontSize = 16.sp) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { c.repo.emptyTrash(); confirmEmpty = false; toast = "回收站已清空" }) { Text("是") }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmEmpty = false }) { Text("否") } }
        )
    }

    toast?.let { ToastHost(it) { toast = null } }
}
