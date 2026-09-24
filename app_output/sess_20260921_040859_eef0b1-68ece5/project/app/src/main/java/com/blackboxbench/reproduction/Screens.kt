@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.blackboxbench.reproduction

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*

val Lavender = Color(0xFFE8EAFE)
val Purple = Color(0xFF4F4D87)
val PalePurple = Color(0xFFDCE0FF)
val SoftGray = Color(0xFFF3F3F8)

val galleryAssets = listOf(
    "gallery/morning.png", "gallery/lake.png", "gallery/room.png", "gallery/cookies.png",
    "gallery/walk.png", "gallery/shelf.png", "gallery/baking.png", "gallery/mint.png",
    "gallery/notifications.png"
)

@Composable
fun AssetImage(index: Int, modifier: Modifier = Modifier, darken: Boolean = false) {
    val context = LocalContext.current
    val path = galleryAssets[index.mod(galleryAssets.size)]
    val bitmap = remember(path) {
        runCatching { context.assets.open(path).use { BitmapFactory.decodeStream(it).asImageBitmap() } }.getOrNull()
    }
    Box(modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFFD9DAE4))) {
        if (bitmap != null) Image(bitmap, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        if (darken) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .42f)))
    }
}

@Composable
fun TextIcon(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = Color(0xFF292934)) {
    Box(modifier.size(48.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text, color = tint, fontSize = 22.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun AppTopBar(title: String, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().height(72.dp).background(Lavender).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) TextIcon("‹", onBack)
        Text(title, Modifier.weight(1f).padding(start = if (onBack == null) 14.dp else 4.dp), fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF252530))
        actions()
    }
}

@Composable
fun HomeScreen(
    columns: Int, favorite: Boolean, locked: Boolean, selection: Boolean,
    onSelect: (Boolean) -> Unit, onOpenAlbum: (String) -> Unit, onCamera: () -> Unit,
    onSort: () -> Unit, onColumns: () -> Unit, onNewFolder: () -> Unit,
    onSettings: () -> Unit, onTrash: () -> Unit, onAbout: () -> Unit,
    onSlideshow: () -> Unit, onFolderMenu: () -> Unit,
    onMenuExpanded: (Boolean) -> Unit, menuExpanded: Boolean,
    onSearchFiles: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(Color(0xFFFCFCFF))) {
        if (selection) {
            AppTopBar("1") {
                TextIcon("▶", onSlideshow)
                TextIcon("⋮", onFolderMenu)
            }
        } else {
            Row(
                Modifier.fillMaxWidth().background(Lavender).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    Modifier.weight(1f).height(58.dp),
                    color = Color(0xFFF4F4FF), shape = RoundedCornerShape(30.dp)
                ) {
                    Row(Modifier.padding(horizontal = 17.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("⌕", fontSize = 26.sp, color = Purple)
                        androidx.compose.foundation.text.BasicTextField(
                            value = query, onValueChange = { query = it },
                            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 17.sp, color = Color.DarkGray),
                            decorationBox = { inner ->
                                if (query.isEmpty()) Text("搜索文件夹", color = Color(0xFF666679), fontSize = 16.sp)
                                inner()
                            }
                        )
                        TextIcon("⌁", onCamera, Modifier.size(40.dp), Purple)
                        TextIcon("▦", onColumns, Modifier.size(40.dp), Purple)
                        Box {
                            TextIcon("⋮", { onMenuExpanded(true) }, Modifier.size(40.dp), Purple)
                            DropdownMenu(menuExpanded, { onMenuExpanded(false) }) {
                                DropdownMenuItem({ Text("新建文件夹") }, { onMenuExpanded(false); onNewFolder() })
                                DropdownMenuItem({ Text("排序") }, { onMenuExpanded(false); onSort() })
                                DropdownMenuItem({ Text("选择全部") }, { onMenuExpanded(false); onSelect(true) })
                                DropdownMenuItem({ Text("回收站") }, { onMenuExpanded(false); onTrash() })
                                DropdownMenuItem({ Text("设置") }, { onMenuExpanded(false); onSettings() })
                                DropdownMenuItem({ Text("关于") }, { onMenuExpanded(false); onAbout() })
                            }
                        }
                    }
                }
            }
        }

        if (!selection) {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onSearchFiles).padding(horizontal = 20.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("在所有文件中搜索", color = Purple, fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
                Text("›", color = Purple, fontSize = 20.sp)
            }
        }

        val folders = buildList {
            if (favorite) add(Triple("收藏", 1, 0))
            add(Triple("Camera", 30, 1))
            add(Triple("BlackBoxBench", 2, 2))
        }.filter { query.isBlank() || it.first.contains(query, true) }

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(folders) { (name, count, idx) ->
                FolderCard(
                    name, count, idx, selected = selection && name == "BlackBoxBench",
                    locked = locked && name == "BlackBoxBench",
                    onClick = { if (selection) onSelect(false) else onOpenAlbum(name) },
                    onLongClick = { onSelect(true) }
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun FolderCard(name: String, count: Int, index: Int, selected: Boolean, locked: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(4.dp)
    ) {
        Box {
            AssetImage(index, Modifier.fillMaxWidth().aspectRatio(1.12f), darken = selected)
            if (selected) Surface(
                Modifier.align(Alignment.TopEnd).padding(9.dp).size(27.dp),
                shape = CircleShape, color = Purple
            ) { Box(contentAlignment = Alignment.Center) { Text("✓", color = Color.White) } }
            if (locked) Surface(
                Modifier.align(Alignment.BottomEnd).padding(9.dp),
                shape = RoundedCornerShape(12.dp), color = Color.Black.copy(alpha = .55f)
            ) { Text("▣", color = Color.White, modifier = Modifier.padding(7.dp)) }
        }
        Text(name, Modifier.padding(top = 7.dp, start = 3.dp), fontSize = 17.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        Text("$count 项", Modifier.padding(start = 3.dp, top = 2.dp), color = Color.Gray, fontSize = 14.sp)
    }
}

@Composable
fun AlbumScreen(
    title: String, count: Int, columns: Int, selection: Boolean, onSelect: (Boolean) -> Unit,
    onBack: () -> Unit, onOpen: (Int) -> Unit, onSort: () -> Unit, onColumns: () -> Unit,
    onSlideshow: () -> Unit, onDelete: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Color(0xFFFCFCFF))) {
        if (selection) AppTopBar("1", { onSelect(false) }) {
            TextIcon("▶", onSlideshow)
            TextIcon("⌫", onDelete)
        } else AppTopBar(title, onBack) {
            TextIcon("↕", onSort)
            TextIcon("▦", onColumns)
            Box {
                TextIcon("⋮", { menu = true })
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem({ Text("选择全部") }, { menu = false; onSelect(true) })
                    DropdownMenuItem({ Text("幻灯片放映") }, { menu = false; onSlideshow() })
                }
            }
        }
        if (count == 0) EmptyState(if (title == "收藏") "没有收藏的文件" else "文件夹为空")
        else LazyVerticalGrid(
            GridCells.Fixed(columns), contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp), verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            items((0 until count).toList()) { index ->
                Box(
                    Modifier.aspectRatio(1f).combinedClickable(
                        onClick = { if (selection) onSelect(false) else onOpen(index) },
                        onLongClick = { onSelect(true) }
                    )
                ) {
                    AssetImage(index, Modifier.fillMaxSize(), darken = selection && index == 0)
                    Text(
                        if (index % 4 == 0) "00:" + (12 + index).toString().padStart(2, '0') else "",
                        Modifier.align(Alignment.BottomEnd).padding(5.dp).background(Color.Black.copy(alpha = .55f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp),
                        color = Color.White, fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ViewerScreen(
    index: Int, total: Int, favorite: Boolean, onFavorite: () -> Unit, onBack: () -> Unit,
    onNext: (Int) -> Unit, onDelete: () -> Unit, onInfo: () -> Unit, onEdit: () -> Unit,
    onSlideshow: () -> Unit, menuExpanded: Boolean, onMenuExpanded: (Boolean) -> Unit
) {
    var chrome by remember { mutableStateOf(true) }
    Box(
        Modifier.fillMaxSize().background(Color.Black).pointerInput(index) {
            var distance = 0f
            detectHorizontalDragGestures(
                onDragStart = { distance = 0f },
                onHorizontalDrag = { _, d -> distance += d },
                onDragEnd = { if (distance < -70) onNext(1) else if (distance > 70) onNext(-1) }
            )
        }.clickable { chrome = !chrome }
    ) {
        AssetImage(index, Modifier.fillMaxWidth().aspectRatio(.78f).align(Alignment.Center))
        if (chrome) {
            Row(
                Modifier.fillMaxWidth().height(78.dp).background(Color.Black.copy(alpha = .72f)).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextIcon("‹", onBack, tint = Color.White)
                Column(Modifier.weight(1f)) {
                    Text("IMG_2026_PUBLIC_" + (index + 1) + ".jpg", color = Color.White, fontWeight = FontWeight.Medium, maxLines = 1)
                    Text((index + 1).toString() + " / " + total, color = Color.LightGray, fontSize = 13.sp)
                }
                TextIcon(if (favorite) "♥" else "♡", onFavorite, tint = if (favorite) Color(0xFFFFB2C8) else Color.White)
                Box {
                    TextIcon("⋮", { onMenuExpanded(true) }, tint = Color.White)
                    DropdownMenu(menuExpanded, { onMenuExpanded(false) }) {
                        DropdownMenuItem({ Text("幻灯片放映") }, { onMenuExpanded(false); onSlideshow() })
                        DropdownMenuItem({ Text("详细信息") }, { onMenuExpanded(false); onInfo() })
                        DropdownMenuItem({ Text("设为壁纸") }, { onMenuExpanded(false) })
                    }
                }
            }
            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(86.dp)
                    .background(Color.Black.copy(alpha = .76f)),
                horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically
            ) {
                ViewerAction("↗", "分享") { }
                ViewerAction("✎", "编辑", onEdit)
                ViewerAction("⌫", "删除", onDelete)
                ViewerAction("ⓘ", "信息", onInfo)
            }
        }
    }
}

@Composable
fun ViewerAction(icon: String, label: String, onClick: () -> Unit) {
    Column(Modifier.width(76.dp).clickable(onClick = onClick).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, color = Color.White, fontSize = 25.sp)
        Text(label, color = Color.White, fontSize = 12.sp)
    }
}

@Composable
fun EditorScreen(index: Int, onBack: () -> Unit, onSave: () -> Unit) {
    var rotation by remember { mutableIntStateOf(0) }
    var flip by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(Modifier.fillMaxWidth().height(72.dp), verticalAlignment = Alignment.CenterVertically) {
            TextIcon("×", onBack, tint = Color.White)
            Text("编辑", Modifier.weight(1f), color = Color.White, fontSize = 20.sp)
            TextButton(onClick = onSave) { Text("保存副本", color = Color.White) }
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            AssetImage(index, Modifier.fillMaxWidth().aspectRatio(if (rotation % 180 == 0) .8f else 1.2f))
            if (flip) Text("已水平翻转", color = Color.White, modifier = Modifier.align(Alignment.TopCenter).padding(12.dp).background(Color.Black.copy(alpha=.6f)).padding(8.dp))
        }
        Row(
            Modifier.fillMaxWidth().height(100.dp).background(Color(0xFF17171A)),
            horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically
        ) {
            ViewerAction("⌗", "裁剪") { }
            ViewerAction("↻", "旋转") { rotation += 90 }
            ViewerAction("↔", "翻转") { flip = !flip }
        }
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit, onAppearance: () -> Unit, locked: Boolean, onLock: () -> Unit, onTrash: () -> Unit, onAbout: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color(0xFFFCFCFF))) {
        AppTopBar("设置", onBack)
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Section("常规")
            SettingRow("外观", "主题、颜色和字体", onAppearance)
            SettingRow("媒体文件分组", "按日期分组显示")
            SettingRow("缩略图动画", "启用")
            Section("安全与隐私")
            SettingRow(if (locked) "更改应用锁" else "设置应用锁", if (locked) "PIN 码已启用" else "保护指定文件夹", onLock)
            SettingRow("隐藏系统媒体", "使用 .nomedia 隐藏")
            Section("管理")
            SettingRow("回收站", "已删除的项目最多保留 30 天", onTrash)
            SettingRow("包含的文件夹", "管理扫描路径")
            SettingRow("关于", "Fossify Gallery", onAbout)
        }
    }
}

@Composable
fun AppearanceScreen(onBack: () -> Unit) {
    var theme by remember { mutableStateOf("系统默认") }
    var iconColor by remember { mutableStateOf(Purple) }
    var fontSize by remember { mutableStateOf("标准") }
    Column(Modifier.fillMaxSize().background(Color(0xFFFCFCFF))) {
        AppTopBar("外观", onBack)
        Section("主题")
        listOf("系统默认", "浅色", "深色").forEach { option ->
            Row(Modifier.fillMaxWidth().clickable { theme = option }.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(theme == option, { theme = option })
                Text(option, Modifier.padding(start = 10.dp), fontSize = 17.sp)
            }
        }
        Divider()
        Text("图标颜色", Modifier.padding(20.dp), color = Purple, fontWeight = FontWeight.Medium)
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(Purple, Color(0xFF006C4C), Color(0xFF9B4056), Color(0xFF4D5F9B), Color(0xFF8B5000)).forEach { c ->
                Surface(Modifier.size(42.dp).clickable { iconColor = c }, shape = CircleShape, color = c) {
                    if (c == iconColor) Box(contentAlignment = Alignment.Center) { Text("✓", color = Color.White) }
                }
            }
        }
        Divider(Modifier.padding(top = 22.dp))
        SettingRow("字体大小", fontSize) { fontSize = if (fontSize == "标准") "大" else "标准" }
    }
}

@Composable
fun TrashScreen(count: Int, selection: Boolean, onSelect: (Boolean) -> Unit, onBack: () -> Unit, onRestore: () -> Unit, onDelete: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color(0xFFFCFCFF))) {
        if (selection) AppTopBar("1", { onSelect(false) }) {
            TextIcon("↶", onRestore)
            TextIcon("⌫", onDelete)
        } else AppTopBar("回收站", onBack) {
            if (count > 0) TextButton(onClick = onDelete) { Text("清空", color = Purple) }
        }
        if (count == 0) EmptyState("回收站为空", "已删除的项目会在这里保留 30 天")
        else {
            Text("项目将在 30 天后永久删除", Modifier.fillMaxWidth().background(PalePurple).padding(14.dp), textAlign = TextAlign.Center, color = Purple)
            Box(Modifier.width(180.dp).padding(8.dp).combinedClickable(onClick = { onSelect(!selection) }, onLongClick = { onSelect(true) })) {
                AssetImage(0, Modifier.fillMaxWidth().aspectRatio(1f), selection)
                if (selection) Text("✓", Modifier.align(Alignment.TopEnd).padding(10.dp).background(Purple, CircleShape).padding(6.dp), color = Color.White)
            }
        }
    }
}

@Composable
fun FileSearchScreen(onBack: () -> Unit) {
    var q by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(Color(0xFFFCFCFF))) {
        Row(Modifier.fillMaxWidth().height(74.dp).background(Lavender).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextIcon("‹", onBack)
            TextField(
                q, { q = it }, Modifier.weight(1f),
                placeholder = { Text("搜索所有文件") }, singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
                )
            )
            if (q.isNotEmpty()) TextIcon("×", { q = "" })
        }
        if (q.isBlank()) EmptyState("搜索媒体文件", "输入文件名以在所有文件夹中搜索")
        else LazyVerticalGrid(GridCells.Fixed(3), contentPadding = PaddingValues(4.dp)) {
            items((0..5).toList()) { AssetImage(it, Modifier.padding(2.dp).aspectRatio(1f)) }
        }
    }
}

@Composable
fun PinScreen(title: String, correctPin: String, onSuccess: () -> Unit, onBack: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Color(0xFFFCFCFF)), horizontalAlignment = Alignment.CenterHorizontally) {
        AppTopBar(title, onBack)
        Spacer(Modifier.height(84.dp))
        Surface(Modifier.size(76.dp), color = Lavender, shape = CircleShape) {
            Box(contentAlignment = Alignment.Center) { Text("▣", fontSize = 30.sp, color = Purple) }
        }
        Text(if (pin.isEmpty()) "输入 4 位 PIN 码" else "•".repeat(pin.length), Modifier.padding(24.dp), fontSize = 24.sp)
        if (error) Text("PIN 码不正确，请重试", color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(20.dp))
        (listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf("","0","⌫"))).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 65.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { n ->
                    Box(Modifier.size(78.dp).clickable(enabled = n.isNotEmpty()) {
                        if (n == "⌫") pin = pin.dropLast(1)
                        else if (pin.length < 4) {
                            pin += n
                            if (pin.length == 4) {
                                if (pin == correctPin) onSuccess() else { error = true; pin = "" }
                            }
                        }
                    }, contentAlignment = Alignment.Center) { Text(n, fontSize = 25.sp) }
                }
            }
        }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color(0xFFFCFCFF))) {
        AppTopBar("关于", onBack)
        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(Modifier.size(90.dp), shape = RoundedCornerShape(24.dp), color = Purple) {
                Box(contentAlignment = Alignment.Center) { Text("▧", color = Color.White, fontSize = 42.sp) }
            }
            Text("Fossify Gallery", Modifier.padding(top = 18.dp), fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Text("版本 1.0.0", color = Color.Gray, modifier = Modifier.padding(8.dp))
            Divider(Modifier.padding(vertical = 20.dp))
            listOf("常见问题", "隐私政策", "开源许可证", "在 GitHub 上查看").forEach { SettingRow(it, "") }
        }
    }
}

@Composable
fun Section(text: String) {
    Text(text, Modifier.fillMaxWidth().padding(start = 20.dp, top = 22.dp, bottom = 8.dp), color = Purple, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
}

@Composable
fun SettingRow(title: String, subtitle: String = "", onClick: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 17.sp)
            if (subtitle.isNotBlank()) Text(subtitle, color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(top = 3.dp))
        }
        Text("›", color = Color.Gray, fontSize = 25.sp)
    }
}

@Composable
fun EmptyState(title: String, subtitle: String = "") {
    Column(Modifier.fillMaxSize().padding(40.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("▧", fontSize = 60.sp, color = Color(0xFFB5B6C4))
        Text(title, Modifier.padding(top = 20.dp), fontSize = 20.sp, color = Color(0xFF666672), textAlign = TextAlign.Center)
        if (subtitle.isNotBlank()) Text(subtitle, Modifier.padding(top = 8.dp), color = Color.Gray, textAlign = TextAlign.Center)
    }
}
