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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

private val tileGradient = Brush.verticalGradient(
    colors = listOf(Color.Transparent, Color(0x66000000), Color(0xCC000000))
)

/** Camera outline drawn by hand: the core icon set has no camera glyph. */
@Composable
fun CameraGlyph(tint: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val stroke = h * 0.09f
        drawRoundRect(
            color = tint, topLeft = androidx.compose.ui.geometry.Offset(w * 0.08f, h * 0.26f),
            size = androidx.compose.ui.geometry.Size(w * 0.84f, h * 0.56f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.14f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
        )
        drawRoundRect(
            color = tint, topLeft = androidx.compose.ui.geometry.Offset(w * 0.34f, h * 0.12f),
            size = androidx.compose.ui.geometry.Size(w * 0.28f, h * 0.16f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.05f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
        )
        drawCircle(
            color = tint, radius = h * 0.15f,
            center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.54f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
        )
    }
}

@Composable
fun SearchBarHeader(
    text: String,
    onTextChange: (String) -> Unit,
    onCamera: () -> Unit,
    onToggleTimeline: () -> Unit,
    onMenu: () -> Unit,
    menu: @Composable () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().background(GalleryBackground).padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f).height(52.dp)
                .clip(RoundedCornerShape(26.dp)).background(GalleryTopBar).padding(horizontal = 14.dp)
        ) {
            Icon(Icons.Default.Search, contentDescription = "搜索", tint = GallerySecondaryText)
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (text.isEmpty()) Text("搜索文件夹", color = GallerySecondaryText, fontSize = 17.sp)
                androidx.compose.foundation.text.BasicTextField(
                    value = text, onValueChange = onTextChange,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 17.sp, color = GalleryText),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        IconButton(onClick = onCamera) { CameraGlyph(tint = GalleryText) }
        IconButton(onClick = onToggleTimeline) { Icon(Icons.Default.List, contentDescription = "全部媒体", tint = GalleryText) }
        Box {
            IconButton(onClick = onMenu) { Icon(Icons.Default.MoreVert, contentDescription = "更多", tint = GalleryText) }
            menu()
        }
    }
}

@Composable
fun SelectionBar(
    count: Int,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onInfo: () -> Unit,
    onMenu: () -> Unit,
    menu: @Composable () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().background(GallerySelectionBar).padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Color.White) }
        Text("$count", color = Color.White, fontSize = 20.sp, modifier = Modifier.padding(start = 12.dp))
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color.White) }
        IconButton(onClick = onShare) { Icon(Icons.Default.Share, contentDescription = "分享", tint = Color.White) }
        IconButton(onClick = onInfo) { Icon(Icons.Default.Info, contentDescription = "信息", tint = Color.White) }
        Box {
            IconButton(onClick = onMenu) { Icon(Icons.Default.MoreVert, contentDescription = "更多", tint = Color.White) }
            menu()
        }
    }
}

@Composable
fun MenuEntries(entries: List<Pair<String, () -> Unit>>, expanded: Boolean, onDismiss: () -> Unit, hasArrow: Set<String> = emptySet()) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, containerColor = Color(0xFFF2F0F7)) {
        entries.forEach { (label, action) ->
            DropdownMenuItem(
                text = { Text(label, fontSize = 17.sp) },
                onClick = { onDismiss(); action() },
                trailingIcon = if (hasArrow.contains(label)) {
                    { Text("▸", fontSize = 16.sp) }
                } else null
            )
        }
    }
}

@Composable
fun MediaTile(
    item: MediaItem,
    file: File,
    selected: Boolean,
    selectionMode: Boolean,
    showName: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f).background(Color(0xFF202020))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        MediaThumb(file, item, Modifier.fillMaxSize())
        if (item.isVideo) {
            Icon(
                Icons.Default.PlayArrow, contentDescription = null, tint = Color.White,
                modifier = Modifier.align(Alignment.Center).size(48.dp)
            )
        }
        Box(modifier = Modifier.fillMaxSize().background(tileGradient))
        if (showName) {
            Text(
                item.fileName, color = Color.White, fontSize = 13.sp, maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 6.dp, end = 6.dp, bottom = 6.dp)
            )
        }
        if (item.favorite) {
            Box(
                modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp).size(28.dp)
                    .clip(CircleShape).background(Color(0x99000000)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Star, contentDescription = "收藏", tint = Color.White, modifier = Modifier.size(18.dp)) }
        }
        if (selectionMode) {
            Box(
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(26.dp).clip(CircleShape)
                    .background(if (selected) GalleryPrimary else Color(0x66000000)),
                contentAlignment = Alignment.Center
            ) {
                if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun FolderTile(
    album: Album,
    cover: MediaItem?,
    count: Int,
    selected: Boolean,
    selectionMode: Boolean,
    favorite: Boolean,
    file: File?,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f).background(Color(0xFF202020))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        if (cover != null && file != null) {
            MediaThumb(file, cover, Modifier.fillMaxSize())
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF101010)), contentAlignment = Alignment.Center) {
                Text("⚠", color = Color(0xFFFFC107), fontSize = 34.sp)
            }
        }
        Box(modifier = Modifier.fillMaxSize().background(tileGradient))
        Text(
            album.name, color = Color.White, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 8.dp, end = 8.dp, bottom = 22.dp)
        )
        Text(
            "$count", color = Color.White, fontSize = 15.sp,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = 4.dp)
        )
        if (favorite) {
            Box(
                modifier = Modifier.align(Alignment.TopStart).padding(6.dp).size(30.dp).clip(CircleShape).background(Color(0x99000000)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Star, contentDescription = "收藏", tint = Color.White, modifier = Modifier.size(18.dp)) }
        }
        if (selectionMode) {
            Box(
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(26.dp).clip(CircleShape)
                    .background(if (selected) GalleryPrimary else Color(0x66000000)),
                contentAlignment = Alignment.Center
            ) { if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp)) }
        }
    }
}

@Composable
fun FolderListRow(
    album: Album,
    cover: MediaItem?,
    count: Int,
    favorite: Boolean,
    file: File?,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().background(GalleryBackground)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Box(modifier = Modifier.size(84.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF9AA6D6))) {
            if (cover != null && file != null) MediaThumb(file, cover, Modifier.fillMaxSize())
            if (selectionMode) {
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(22.dp).clip(CircleShape)
                        .background(if (selected) GalleryPrimary else Color(0x66000000)),
                    contentAlignment = Alignment.Center
                ) { if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp)) }
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(album.name, fontSize = 17.sp, color = GalleryText)
                Spacer(Modifier.width(10.dp))
                Text("$count", fontSize = 14.sp, color = GallerySecondaryText)
            }
            Text(
                if (favorite) "favorites/" else "/storage/emulated/0/${album.path}/",
                fontSize = 13.sp, color = GallerySecondaryText
            )
        }
        if (favorite) Icon(Icons.Default.Star, contentDescription = "收藏", tint = GalleryText, modifier = Modifier.size(18.dp))
    }
}
