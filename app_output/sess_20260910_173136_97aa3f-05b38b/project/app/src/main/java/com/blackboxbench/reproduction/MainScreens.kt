package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MainScaffold(
    tab: String,
    onTab: (String) -> Unit,
    onOpen: (String) -> Unit,
    favorite: Boolean,
    onFavorite: () -> Unit,
    playlistName: String,
    onPlaylistCreated: (String) -> Unit,
    incognito: Boolean,
    onIncognito: () -> Unit
) {
    var overflow by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().height(82.dp).padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VlcBrand()
            Spacer(Modifier.weight(1f))
            Text("⌕", fontSize = 36.sp, color = Color(0xFF666666), modifier = Modifier.clickable { search = true }.padding(8.dp))
            Text("⋮", fontSize = 32.sp, color = Color(0xFF666666), modifier = Modifier.clickable { overflow = true }.padding(8.dp))
            DropdownMenu(expanded = overflow, onDismissRequest = { overflow = false }) {
                DropdownMenuItem(
                    text = { Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("无痕模式", fontSize = 17.sp)
                        Spacer(Modifier.width(40.dp))
                        Text(if (incognito) "☑" else "☐", color = if (incognito) VlcOrange else Color.Gray, fontSize = 22.sp)
                    } },
                    onClick = { onIncognito(); overflow = false }
                )
                DropdownMenuItem(text = { Text("刷新", fontSize = 17.sp) }, onClick = { overflow = false })
            }
        }
        Box(Modifier.weight(1f)) {
            when (tab) {
                "video" -> VideoLibrary(onOpen)
                "audio" -> AudioLibrary(onOpen, favorite, onFavorite, onPlaylistCreated)
                "browse" -> BrowseLibrary(onOpen)
                "playlist" -> PlaylistLibrary(playlistName, onPlaylistCreated, onOpen)
                else -> MoreLibrary(onOpen)
            }
        }
        BottomNav(tab, onTab)
    }
    if (search) {
        AlertDialog(
            onDismissRequest = { search = false },
            title = { Text("在当前列表中搜索") },
            text = {
                Column {
                    var query by remember { mutableStateOf("") }
                    OutlinedTextField(value = query, onValueChange = { query = it }, placeholder = { Text("输入媒体标题") }, singleLine = true)
                    Spacer(Modifier.height(16.dp))
                    if (query.isBlank() || "sample_audio.wav".contains(query, true)) Text("♪  sample_audio.wav", fontSize = 17.sp)
                    if (query.isBlank() || "sample_video".contains(query, true)) Text("▣  sample_video   0:03", fontSize = 17.sp, modifier = Modifier.padding(top = 12.dp))
                }
            },
            confirmButton = { TextButton(onClick = { search = false }) { Text("完成", color = VlcOrange) } }
        )
    }
}

@Composable
private fun VideoLibrary(onOpen: (String) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("视频", color = VlcOrange, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text("▦", fontSize = 25.sp, color = Color(0xFF666666))
        }
        HorizontalDivider()
        Row(
            Modifier.fillMaxWidth().clickable { onOpen("videoPlayer") }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StoryThumbnail(Modifier.size(width = 145.dp, height = 84.dp))
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text("sample_video", fontSize = 19.sp)
                Text("0:03", color = Color(0xFF777777), fontSize = 15.sp)
            }
            Text("⋮", fontSize = 26.sp, color = Color(0xFF666666))
        }
    }
}

@Composable
fun StoryThumbnail(modifier: Modifier) {
    Box(modifier.background(Color(0xFF6C5872), RoundedCornerShape(2.dp))) {
        Text("STORY LOOP", color = Color.White, fontSize = 9.sp, modifier = Modifier.padding(6.dp))
        Box(Modifier.align(Alignment.CenterStart).padding(start = 8.dp).size(width = 40.dp, height = 28.dp).background(Color(0xFF8DB8AA), RoundedCornerShape(8.dp)))
        Box(Modifier.align(Alignment.TopEnd).padding(8.dp).size(17.dp).border(2.dp, Color.White, RoundedCornerShape(50)))
        Text("Synthetic local fixture", color = Color.White.copy(alpha = .8f), fontSize = 7.sp, modifier = Modifier.align(Alignment.BottomStart).padding(6.dp))
    }
}

@Composable
private fun AudioLibrary(onOpen: (String) -> Unit, favorite: Boolean, onFavorite: () -> Unit, onPlaylistCreated: (String) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    var addDialog by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("艺术家", "专辑", "轨道", "流派", "播放列表").forEach {
                Text(it, color = if (it == "轨道") VlcOrange else Color(0xFF646464), fontSize = 16.sp, modifier = Modifier.padding(vertical = 12.dp))
            }
        }
        Box(Modifier.fillMaxWidth().height(3.dp).background(Color.White)) {
            Box(Modifier.align(Alignment.Center).width(48.dp).fillMaxHeight().background(VlcOrange, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
        }
        HorizontalDivider()
        Text("S", color = VlcOrange, fontSize = 22.sp, modifier = Modifier.padding(start = 18.dp, top = 12.dp))
        Row(Modifier.fillMaxWidth().clickable { onOpen("audioPlayer") }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("♫", color = Color(0xFF666666), fontSize = 43.sp, modifier = Modifier.width(62.dp))
            Text("sample_audio.wav", fontSize = 20.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (favorite) "♥" else "♡", fontSize = 25.sp, modifier = Modifier.clickable { onFavorite() }.padding(10.dp))
            Box {
                Text("⋮", fontSize = 28.sp, color = Color(0xFF666666), modifier = Modifier.clickable { menu = true }.padding(8.dp))
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    listOf("播放全部", "添加到播放队列", "接下来播放", "信息", "转到专辑", "转到艺术家").forEach {
                        DropdownMenuItem(text = { Text(it) }, onClick = { menu = false })
                    }
                    DropdownMenuItem(text = { Text("添加到播放列表") }, onClick = { menu = false; addDialog = true })
                    DropdownMenuItem(text = { Text(if (favorite) "取消收藏" else "收藏") }, onClick = { menu = false; onFavorite() })
                    listOf("设为铃声", "删除", "分享", "浏览目录").forEach {
                        DropdownMenuItem(text = { Text(it) }, onClick = { menu = false })
                    }
                }
            }
        }
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier.align(Alignment.BottomEnd).padding(18.dp).size(62.dp).background(VlcOrange, RoundedCornerShape(31.dp)).clickable { onOpen("audioPlayer") },
                contentAlignment = Alignment.Center
            ) { Text("⤨", color = Color.White, fontSize = 28.sp) }
        }
    }
    if (addDialog) PlaylistNameDialog(initial = "Bench", onDismiss = { addDialog = false }, onConfirm = { onPlaylistCreated(it); addDialog = false })
}

@Composable
private fun BrowseLibrary(onOpen: (String) -> Unit) {
    var folder by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        Text("浏览", color = VlcOrange, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(18.dp))
        HorizontalDivider()
        if (folder.isBlank()) {
            SectionTitle("收藏")
            listOf("Download", "Movies", "Music", "Podcasts").forEach { FolderRow(it) {} }
            SectionTitle("存储")
            FolderRow("内部存储") { folder = "internal" }
            SectionTitle("本地网络")
            Text("设备处于离线状态", color = Color(0xFF888888), modifier = Modifier.padding(horizontal = 18.dp))
        } else if (folder == "internal") {
            Row(Modifier.clickable { folder = "" }.padding(16.dp)) { Text("‹  内部存储", color = VlcOrange, fontSize = 20.sp) }
            FolderRow("BlackBoxBench") { folder = "bench" }
            FolderRow("Download") {}
            FolderRow("Movies") {}
        } else {
            Row(Modifier.clickable { folder = "internal" }.padding(16.dp)) { Text("‹  BlackBoxBench", color = VlcOrange, fontSize = 20.sp) }
            MediaFileRow("sample_audio.wav", "30.91 kB", "♫") { onOpen("audioPlayer") }
            MediaFileRow("sample_video.mp4", "21.59 kB", "▣") { onOpen("videoPlayer") }
        }
    }
}

@Composable
private fun FolderRow(name: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("□", fontSize = 27.sp, color = Color(0xFF666666), modifier = Modifier.width(48.dp))
        Text(name, fontSize = 18.sp)
        Spacer(Modifier.weight(1f))
        Text("›", fontSize = 27.sp, color = Color.Gray)
    }
}

@Composable
private fun MediaFileRow(name: String, size: String, symbol: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(symbol, fontSize = 32.sp, color = Color(0xFF666666), modifier = Modifier.width(54.dp))
        Column(Modifier.weight(1f)) { Text(name, fontSize = 18.sp); Text(size, fontSize = 14.sp, color = Color.Gray) }
        Text("⋮", fontSize = 26.sp)
    }
}

@Composable
private fun PlaylistLibrary(playlistName: String, onPlaylistCreated: (String) -> Unit, onOpen: (String) -> Unit) {
    var dialog by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("播放列表", color = VlcOrange, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text("＋", fontSize = 28.sp, color = VlcOrange, modifier = Modifier.clickable { dialog = true })
        }
        HorizontalDivider()
        if (playlistName.isBlank()) {
            Column(Modifier.fillMaxWidth().padding(top = 90.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("≋♪", fontSize = 70.sp, color = Color(0xFF777777))
                Text("尚无播放列表", fontSize = 20.sp, modifier = Modifier.padding(top = 10.dp))
                Text("创建播放列表来整理您喜爱的媒体", color = Color.Gray, fontSize = 15.sp)
                VlcButton("新建播放列表", onClick = { dialog = true }, modifier = Modifier.padding(top = 22.dp))
            }
        } else {
            Text(playlistName.take(1).uppercase(), color = VlcOrange, fontSize = 22.sp, modifier = Modifier.padding(18.dp))
            Column(Modifier.padding(horizontal = 18.dp).clickable { onOpen("audioPlayer") }) {
                Box(Modifier.size(182.dp).background(Color.White).border(1.dp, Color(0xFFE1E1E1), RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                    Text("≋♪", fontSize = 64.sp, color = Color(0xFF666666))
                    Box(Modifier.align(Alignment.BottomEnd).padding(10.dp).size(38.dp).border(1.dp, Color.LightGray, RoundedCornerShape(19.dp)), contentAlignment = Alignment.Center) { Text("▶", color = Color(0xFF666666)) }
                }
                Text(playlistName, fontSize = 19.sp, modifier = Modifier.padding(top = 12.dp))
                Text("1 条轨道 · 0:00", color = Color.Gray, fontSize = 14.sp)
            }
        }
    }
    if (dialog) PlaylistNameDialog(initial = "Bench", onDismiss = { dialog = false }, onConfirm = { onPlaylistCreated(it); dialog = false })
}

@Composable
private fun PlaylistNameDialog(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建播放列表") },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("播放列表名称") }, singleLine = true) },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        confirmButton = { TextButton(onClick = { onConfirm(name.ifBlank { "Bench" }) }) { Text("创建", color = VlcOrange) } }
    )
}

@Composable
private fun MoreLibrary(onOpen: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VlcButton("⚙  设置", { onOpen("settings") }, modifier = Modifier.weight(1f), filled = false)
            Spacer(Modifier.width(16.dp))
            VlcButton("ⓘ  关于", { onOpen("about") }, modifier = Modifier.weight(1f), filled = false)
        }
        Row(Modifier.fillMaxWidth().clickable { onOpen("stream") }.padding(top = 30.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("串流", color = VlcOrange, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f)); Text("›", fontSize = 35.sp)
        }
        Box(Modifier.size(width = 160.dp, height = 118.dp).border(1.dp, Color(0xFFDADADA), RoundedCornerShape(8.dp)).clickable { onOpen("stream") }, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("＋", color = VlcOrange, fontSize = 50.sp); Text("新建串流", fontSize = 18.sp) }
        }
        Row(Modifier.fillMaxWidth().clickable { onOpen("history") }.padding(top = 30.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("历史", color = VlcOrange, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f)); Text("›", fontSize = 35.sp)
        }
        Row {
            Box(Modifier.size(width = 145.dp, height = 136.dp).border(1.dp, Color(0xFFDADADA), RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("♫", fontSize = 58.sp, color = Color(0xFF666666)); Text("sample_audio.w…", maxLines = 1, fontSize = 15.sp) }
            }
            Spacer(Modifier.width(10.dp))
            Column {
                StoryThumbnail(Modifier.size(width = 145.dp, height = 94.dp))
                Text("sample_video", fontSize = 16.sp)
                Text("0:03", color = Color.Gray, fontSize = 14.sp)
            }
        }
    }
}
