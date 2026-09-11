@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.blackboxbench.reproduction

import android.app.Activity
import android.content.Context
import android.graphics.Color as AndroidColor
import android.view.View
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private enum class GalleryPage {
    HOME, FOLDER, FAVORITES, PHOTO, VIDEO, SETTINGS, APPEARANCE, ABOUT, TRASH
}

private data class MediaEntry(
    val id: String,
    val name: String,
    val isVideo: Boolean
)

private val photoEntry = MediaEntry("photo", "sample_photo.png", false)
private val videoEntry = MediaEntry("video", "sample_video.mp4", true)

@Composable
fun GalleryApp() {
    val context = LocalContext.current
    val view = LocalView.current
    val prefs = remember { context.getSharedPreferences("gallery_state", Context.MODE_PRIVATE) }
    var page by remember { mutableStateOf(GalleryPage.HOME) }
    var previousPage by remember { mutableStateOf(GalleryPage.FOLDER) }
    var favoritePhoto by remember { mutableStateOf(prefs.getBoolean("favorite_photo", false)) }
    var favoriteVideo by remember { mutableStateOf(prefs.getBoolean("favorite_video", false)) }
    var deletedPhoto by remember { mutableStateOf(prefs.getBoolean("deleted_photo", false)) }
    var deletedVideo by remember { mutableStateOf(prefs.getBoolean("deleted_video", false)) }
    var showFileNames by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showResizeDialog by remember { mutableStateOf(false) }

    fun setFavorite(id: String, value: Boolean) {
        if (id == "photo") {
            favoritePhoto = value
            prefs.edit().putBoolean("favorite_photo", value).apply()
        } else {
            favoriteVideo = value
            prefs.edit().putBoolean("favorite_video", value).apply()
        }
    }

    fun setDeleted(id: String, value: Boolean) {
        if (id == "photo") {
            deletedPhoto = value
            if (value) setFavorite("photo", false)
            prefs.edit().putBoolean("deleted_photo", value).apply()
        } else {
            deletedVideo = value
            if (value) setFavorite("video", false)
            prefs.edit().putBoolean("deleted_video", value).apply()
        }
    }

    fun isFavorite(entry: MediaEntry): Boolean =
        if (entry.id == "photo") favoritePhoto else favoriteVideo

    fun availableMedia(): List<MediaEntry> = listOf(videoEntry, photoEntry).filter {
        if (it.id == "photo") !deletedPhoto else !deletedVideo
    }

    fun favoriteMedia(): List<MediaEntry> = availableMedia().filter(::isFavorite)

    fun openMedia(entry: MediaEntry, origin: GalleryPage) {
        previousPage = origin
        page = if (entry.isVideo) GalleryPage.VIDEO else GalleryPage.PHOTO
    }

    BackHandler(enabled = page != GalleryPage.HOME) {
        page = when (page) {
            GalleryPage.FOLDER, GalleryPage.FAVORITES, GalleryPage.SETTINGS,
            GalleryPage.ABOUT, GalleryPage.TRASH -> GalleryPage.HOME
            GalleryPage.APPEARANCE -> GalleryPage.SETTINGS
            GalleryPage.PHOTO, GalleryPage.VIDEO -> previousPage
            GalleryPage.HOME -> GalleryPage.HOME
        }
    }

    val viewerPage = page == GalleryPage.PHOTO || page == GalleryPage.VIDEO
    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = if (viewerPage) AndroidColor.BLACK else AndroidColor.rgb(255, 250, 255)
        window.navigationBarColor = if (viewerPage) AndroidColor.BLACK else AndroidColor.rgb(255, 250, 255)
        window.decorView.systemUiVisibility = if (viewerPage) 0 else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    }

    Surface(
        modifier = if (viewerPage) Modifier.fillMaxSize() else Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
        color = if (viewerPage) Color.Black else MaterialTheme.colorScheme.background
    ) {
        when (page) {
            GalleryPage.HOME -> HomeScreen(
                folderCount = availableMedia().size,
                favoriteCount = favoriteMedia().size,
                onOpenFolder = { page = GalleryPage.FOLDER },
                onOpenFavorites = { page = GalleryPage.FAVORITES },
                onOpenMedia = { openMedia(it, GalleryPage.FOLDER) },
                onSettings = { page = GalleryPage.SETTINGS },
                onAbout = { page = GalleryPage.ABOUT },
                onTrash = { page = GalleryPage.TRASH }
            )
            GalleryPage.FOLDER -> MediaGridScreen(
                title = "BlackBoxBench",
                entries = availableMedia(),
                favorite = ::isFavorite,
                showFileNames = showFileNames,
                onToggleNames = { showFileNames = !showFileNames },
                onBack = { page = GalleryPage.HOME },
                onOpen = { openMedia(it, GalleryPage.FOLDER) },
                onFavorite = { entry -> setFavorite(entry.id, !isFavorite(entry)) },
                onDelete = { entry -> setDeleted(entry.id, true) },
                onSettings = { page = GalleryPage.SETTINGS },
                onTrash = { page = GalleryPage.TRASH }
            )
            GalleryPage.FAVORITES -> MediaGridScreen(
                title = "收藏",
                entries = favoriteMedia(),
                favorite = ::isFavorite,
                showFileNames = showFileNames,
                onToggleNames = { showFileNames = !showFileNames },
                onBack = { page = GalleryPage.HOME },
                onOpen = { openMedia(it, GalleryPage.FAVORITES) },
                onFavorite = { entry -> setFavorite(entry.id, !isFavorite(entry)) },
                onDelete = { entry -> setDeleted(entry.id, true) },
                onSettings = { page = GalleryPage.SETTINGS },
                onTrash = { page = GalleryPage.TRASH }
            )
            GalleryPage.PHOTO -> ViewerScreen(
                entry = photoEntry,
                favorite = favoritePhoto,
                onFavorite = { setFavorite("photo", !favoritePhoto) },
                onBack = { page = previousPage },
                onSwitch = {
                    if (!deletedVideo) page = GalleryPage.VIDEO
                },
                onEditOrInfo = { showPermissionDialog = true },
                onResize = { showResizeDialog = true },
                onDelete = {
                    setDeleted("photo", true)
                    page = previousPage
                }
            )
            GalleryPage.VIDEO -> ViewerScreen(
                entry = videoEntry,
                favorite = favoriteVideo,
                onFavorite = { setFavorite("video", !favoriteVideo) },
                onBack = { page = previousPage },
                onSwitch = {
                    if (!deletedPhoto) page = GalleryPage.PHOTO
                },
                onEditOrInfo = { showPermissionDialog = true },
                onResize = { showResizeDialog = true },
                onDelete = {
                    setDeleted("video", true)
                    page = previousPage
                }
            )
            GalleryPage.SETTINGS -> SettingsScreen(
                onBack = { page = GalleryPage.HOME },
                onAppearance = { page = GalleryPage.APPEARANCE }
            )
            GalleryPage.APPEARANCE -> AppearanceScreen(onBack = { page = GalleryPage.SETTINGS })
            GalleryPage.ABOUT -> AboutScreen(onBack = { page = GalleryPage.HOME })
            GalleryPage.TRASH -> TrashScreen(
                deletedPhoto = deletedPhoto,
                deletedVideo = deletedVideo,
                onRestore = { setDeleted(it.id, false) },
                onDeleteForever = { entry ->
                    setDeleted(entry.id, false)
                    Toast.makeText(context, "已从演示回收站中清除", Toast.LENGTH_SHORT).show()
                },
                onBack = { page = GalleryPage.HOME }
            )
        }
    }

    if (showPermissionDialog) {
        AllFilesDialog(onDismiss = { showPermissionDialog = false })
    }
    if (showResizeDialog) {
        ResizeDialog(onDismiss = { showResizeDialog = false })
    }
}

@Composable
private fun HomeScreen(
    folderCount: Int,
    favoriteCount: Int,
    onOpenFolder: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenMedia: (MediaEntry) -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    onTrash: () -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var fileSearch by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var filterDialog by remember { mutableStateOf(false) }
    var newFolderDialog by remember { mutableStateOf(false) }
    var columnsDialog by remember { mutableStateOf(false) }
    var viewDialog by remember { mutableStateOf(false) }
    var sortDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        SearchPill(
            value = query,
            placeholder = if (fileSearch) "搜索文件" else "搜索文件夹",
            onValueChange = { query = it },
            left = "⌕",
            actions = listOf("▣", "▤"),
            onAction = { index ->
                if (index == 0) Toast.makeText(context, "相机在离线演示中不可用", Toast.LENGTH_SHORT).show()
                else fileSearch = !fileSearch
            },
            menuOpen = menu,
            onMenu = { menu = true },
            onDismissMenu = { menu = false },
            menuContent = {
                MenuLine("排序方式") { menu = false; sortDialog = true }
                MenuLine("过滤显示的文件") { menu = false; filterDialog = true }
                MenuLine("更改视图类型") { menu = false; viewDialog = true }
                MenuLine("临时显示隐藏项目") { menu = false; toast(context, "正在显示隐藏项目") }
                MenuLine("临时显示已排除项目") { menu = false; toast(context, "正在显示已排除项目") }
                MenuLine("新建文件夹") { menu = false; newFolderDialog = true }
                MenuLine("列数") { menu = false; columnsDialog = true }
                MenuLine("设置") { menu = false; onSettings() }
                MenuLine("关于") { menu = false; onAbout() }
            }
        )

        if (query.isNotBlank() && !fileSearch) {
            Text(
                "切换文件搜索范围到所有可见的文件夹",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(horizontal = 28.dp, vertical = 12.dp)
                    .clickable { fileSearch = true },
                fontSize = 17.sp
            )
        }

        if (fileSearch) {
            val results = listOf(videoEntry, photoEntry).filter {
                query.isBlank() || it.name.contains(query, ignoreCase = true)
            }
            Text(
                "25 八月 2026",
                modifier = Modifier.padding(start = 18.dp, top = 14.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 17.sp
            )
            LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize()) {
                items(results) { entry ->
                    MediaTile(
                        entry = entry,
                        showName = true,
                        favorite = false,
                        selected = false,
                        onClick = { onOpenMedia(entry) },
                        onLongClick = {}
                    )
                }
            }
        } else {
            val albums = buildList {
                if (favoriteCount > 0 && "收藏".contains(query, true)) add(Triple("收藏", favoriteCount, true))
                if (folderCount > 0 && "BlackBoxBench".contains(query, true)) add(Triple("BlackBoxBench", folderCount, false))
            }
            LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize()) {
                items(albums) { album ->
                    AlbumTile(
                        name = album.first,
                        count = album.second,
                        pinned = album.third,
                        onClick = if (album.third) onOpenFavorites else onOpenFolder
                    )
                }
            }
        }
    }

    if (filterDialog) FilterDialog(onDismiss = { filterDialog = false })
    if (sortDialog) SortDialog(onDismiss = { sortDialog = false })
    if (newFolderDialog) FolderPickerDialog(onDismiss = { newFolderDialog = false })
    if (columnsDialog) SimpleChoiceDialog(
        title = "列数",
        choices = listOf("1", "2", "3", "4", "5"),
        selected = "2",
        onDismiss = { columnsDialog = false }
    )
    if (viewDialog) SimpleChoiceDialog(
        title = "视图类型",
        choices = listOf("网格视图", "列表视图", "紧凑网格"),
        selected = "网格视图",
        onDismiss = { viewDialog = false }
    )
}

@Composable
private fun SearchPill(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    left: String,
    actions: List<String>,
    onAction: (Int) -> Unit,
    menuOpen: Boolean,
    onMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    menuContent: @Composable ColumnScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(34.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(left, fontSize = 38.sp, color = Color(0xFF303035))
        Box(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            if (value.isEmpty()) {
                Text(placeholder, color = Color(0xFF8B8D98), fontSize = 24.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 23.sp, color = Color(0xFF303035)),
                modifier = Modifier.fillMaxWidth()
            )
        }
        actions.forEachIndexed { index, glyph ->
            GlyphButton(glyph, onClick = { onAction(index) }, size = 30)
        }
        Box {
            GlyphButton("⋮", onClick = onMenu, size = 32)
            DropdownMenu(expanded = menuOpen, onDismissRequest = onDismissMenu) {
                menuContent()
            }
        }
    }
}

@Composable
private fun ColumnScope.MenuLine(text: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text, fontSize = 21.sp, color = Color(0xFF0F2B5B)) },
        onClick = onClick,
        modifier = Modifier.widthIn(min = 260.dp)
    )
}

@Composable
private fun AlbumTile(name: String, count: Int, pinned: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .border(0.5.dp, Color.White)
            .combinedClickable(onClick = onClick, onLongClick = onClick)
    ) {
        StoryArt()
        Box(
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(116.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE5000000))))
        )
        if (pinned) {
            Text(
                "●",
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .background(Color(0x77000000), CircleShape)
                    .padding(8.dp)
            )
        }
        Column(
            Modifier.align(Alignment.BottomStart).padding(10.dp)
        ) {
            Text(name, color = Color.White, fontSize = 20.sp, maxLines = 1)
            Text(count.toString(), color = Color.White, fontSize = 20.sp)
        }
    }
}

@Composable
private fun MediaGridScreen(
    title: String,
    entries: List<MediaEntry>,
    favorite: (MediaEntry) -> Boolean,
    showFileNames: Boolean,
    onToggleNames: () -> Unit,
    onBack: () -> Unit,
    onOpen: (MediaEntry) -> Unit,
    onFavorite: (MediaEntry) -> Unit,
    onDelete: (MediaEntry) -> Unit,
    onSettings: () -> Unit,
    onTrash: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<MediaEntry?>(null) }
    var selectionMenu by remember { mutableStateOf(false) }
    var sortDialog by remember { mutableStateOf(false) }
    var filterDialog by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        if (selected != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .background(Color(0xFF8197CF))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlyphButton("‹", { selected = null }, 40)
                Text("1 / " + entries.size, fontSize = 27.sp, modifier = Modifier.weight(1f))
                GlyphButton("▱", { deleteDialog = true }, 30)
                GlyphButton("↗", { }, 32)
                Box {
                    GlyphButton("⋮", { selectionMenu = true }, 34)
                    DropdownMenu(expanded = selectionMenu, onDismissRequest = { selectionMenu = false }) {
                        MenuLine("旋转   ›") { selectionMenu = false }
                        MenuLine("属性") { selectionMenu = false }
                        MenuLine("重命名") { selectionMenu = false }
                        MenuLine("复制到") { selectionMenu = false }
                        MenuLine("移动到") { selectionMenu = false }
                        MenuLine("创建快捷方式") { selectionMenu = false }
                        MenuLine("打开方式") { selectionMenu = false }
                        MenuLine("设置为") { selectionMenu = false }
                        MenuLine("调整大小") { selectionMenu = false }
                        MenuLine("编辑") { selectionMenu = false }
                        MenuLine(if (selected?.let(favorite) == true) "从收藏中移除" else "添加到收藏") {
                            selected?.let(onFavorite)
                            selectionMenu = false
                        }
                        MenuLine("修复拍摄日期") { selectionMenu = false }
                        MenuLine("全选") { selectionMenu = false }
                    }
                }
            }
        } else {
            SearchPill(
                value = query,
                placeholder = "在 " + title + " 中搜索",
                onValueChange = { query = it },
                left = "‹",
                actions = listOf(if (showFileNames) "▦" else "▤", "≡"),
                onAction = { index ->
                    if (index == 0) onToggleNames() else sortDialog = true
                },
                menuOpen = menu,
                onMenu = { menu = true },
                onDismissMenu = { menu = false },
                menuContent = {
                    MenuLine("过滤显示的文件") { menu = false; filterDialog = true }
                    MenuLine("更改视图类型") { menu = false }
                    MenuLine("临时显示隐藏项目") { menu = false }
                    MenuLine("打开回收站") { menu = false; onTrash() }
                    MenuLine("分组方式") { menu = false }
                    MenuLine("设置为默认文件夹") { menu = false }
                    MenuLine("列数") { menu = false }
                    MenuLine("幻灯片") { menu = false }
                    MenuLine("设置") { menu = false; onSettings() }
                }
            )
        }

        val shown = entries.filter { it.name.contains(query, true) }
        if (shown.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().padding(top = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("没有找到符合条件的媒体文件。", fontSize = 20.sp)
                Text("更改过滤条件", color = MaterialTheme.colorScheme.primary, fontSize = 19.sp)
            }
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize()) {
                items(shown, key = { it.id }) { entry ->
                    MediaTile(
                        entry = entry,
                        showName = showFileNames,
                        favorite = favorite(entry),
                        selected = selected?.id == entry.id,
                        onClick = {
                            if (selected != null) selected = if (selected?.id == entry.id) null else entry
                            else onOpen(entry)
                        },
                        onLongClick = { selected = entry }
                    )
                }
            }
        }
    }

    if (sortDialog) SortDialog(onDismiss = { sortDialog = false })
    if (filterDialog) FilterDialog(onDismiss = { filterDialog = false })
    if (deleteDialog && selected != null) {
        AlertDialog(
            onDismissRequest = { deleteDialog = false },
            title = { Text("移到回收站？") },
            text = { Text("此项目将在回收站中保留 30 天。") },
            confirmButton = {
                TextButton(onClick = {
                    selected?.let(onDelete)
                    selected = null
                    deleteDialog = false
                }) { Text("移到回收站") }
            },
            dismissButton = { TextButton(onClick = { deleteDialog = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun MediaTile(
    entry: MediaEntry,
    showName: Boolean,
    favorite: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .border(0.5.dp, Color.White)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        if (entry.isVideo) StoryArt() else LakeArt()
        if (entry.isVideo) {
            Row(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0x99000000))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("▶", color = Color.White, fontSize = 15.sp)
                Spacer(Modifier.width(5.dp))
                Text("00:03", color = Color.White, fontSize = 17.sp)
            }
        }
        if (showName) {
            Text(
                entry.name,
                color = Color.White,
                fontSize = 15.sp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color(0x99000000))
                    .padding(7.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (favorite) {
            Text(
                "★",
                color = Color.White,
                fontSize = 25.sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .background(Color(0x88000000), CircleShape)
                    .padding(8.dp)
            )
        }
        if (selected) {
            Text(
                "✓",
                color = Color(0xFF203A70),
                fontSize = 28.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .background(Color(0xFF8FA5DC), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
private fun LakeArt() {
    Box(Modifier.fillMaxSize().background(Color(0xFF607B60))) {
        Canvas(Modifier.fillMaxSize().padding(18.dp)) {
            val widths = listOf(8f, 6f, 5f)
            val insets = listOf(0f, size.minDimension * .08f, size.minDimension * .18f)
            insets.forEachIndexed { index, inset ->
                drawRoundRect(
                    color = Color(0xFFDDE8E2),
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f),
                    style = Stroke(width = widths[index])
                )
            }
            drawCircle(
                color = Color(0xFFDCE8E3),
                radius = size.minDimension * .12f,
                center = androidx.compose.ui.geometry.Offset(size.width * .68f, size.height * .30f)
            )
        }
        Text(
            "LAKE WALK",
            color = Color(0xFF18202D),
            fontSize = 25.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
                .background(Color(0xEFFFFFFF), RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp)
        )
    }
}

@Composable
private fun StoryArt() {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF6B5370), Color(0xFF34283A))))
    ) {
        Text("STORY LOOP", color = Color.White, fontSize = 22.sp, modifier = Modifier.padding(18.dp))
        Box(
            Modifier
                .size(72.dp)
                .align(Alignment.CenterStart)
                .padding(start = 18.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF92B4AE))
        )
        Canvas(Modifier.size(72.dp).align(Alignment.CenterEnd).padding(end = 18.dp)) {
            drawCircle(Color.White, radius = size.minDimension * .35f, style = Stroke(width = 8f))
        }
        Text(
            "Synthetic local fixture",
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.BottomStart).padding(18.dp)
        )
    }
}

@Composable
private fun ViewerScreen(
    entry: MediaEntry,
    favorite: Boolean,
    onFavorite: () -> Unit,
    onBack: () -> Unit,
    onSwitch: () -> Unit,
    onEditOrInfo: () -> Unit,
    onResize: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var chrome by remember(entry.id) { mutableStateOf(true) }
    var menu by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }
    var playing by remember(entry.id) { mutableStateOf(false) }
    var elapsed by remember(entry.id) { mutableStateOf(0) }
    var dragTotal by remember { mutableStateOf(0f) }

    LaunchedEffect(playing, entry.id) {
        while (playing && entry.isVideo) {
            delay(1000)
            elapsed = (elapsed + 1).coerceAtMost(3)
            if (elapsed >= 3) playing = false
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(entry.id) {
                detectHorizontalDragGestures(
                    onDragStart = { dragTotal = 0f },
                    onHorizontalDrag = { _, amount -> dragTotal += amount },
                    onDragEnd = {
                        if (kotlin.math.abs(dragTotal) > 70f) onSwitch()
                    }
                )
            }
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .align(Alignment.Center)
                .clickable { chrome = !chrome }
        ) {
            if (entry.isVideo) StoryArt() else LakeArt()
            if (entry.isVideo && !playing) {
                Text(
                    "▶",
                    color = Color.White,
                    fontSize = 48.sp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color(0x88000000), CircleShape)
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                        .clickable { elapsed = 0; playing = true }
                )
            }
        }

        if (chrome) {
            Row(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(Color.Black.copy(alpha = .88f))
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlyphButton("‹", onBack, 42, Color.White)
                Text(
                    entry.name,
                    color = Color.White,
                    fontSize = 25.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!entry.isVideo) GlyphButton("↻", { toast(context, "已旋转 90°") }, 32, Color.White)
                GlyphButton("ⓘ", onEditOrInfo, 30, Color.White)
                if (entry.isVideo) GlyphButton("⌕", onEditOrInfo, 31, Color.White)
                Box {
                    GlyphButton("⋮", { menu = true }, 34, Color.White)
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        MenuLine("重命名") { menu = false }
                        MenuLine("复制到剪贴板") { menu = false; toast(context, "已复制文件名") }
                        MenuLine("复制到") { menu = false }
                        MenuLine("移动到") { menu = false }
                        MenuLine("创建快捷方式") { menu = false }
                        MenuLine("打开方式") { menu = false }
                        MenuLine("设置为") { menu = false }
                        MenuLine("更改画面方向   ›") { menu = false }
                        MenuLine("打印") { menu = false }
                        MenuLine("调整大小") { menu = false; onResize() }
                        MenuLine("在地图上显示") { menu = false }
                        MenuLine("幻灯片") { menu = false }
                        MenuLine("设置") { menu = false }
                    }
                }
            }

            if (entry.isVideo) {
                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 136.dp)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = .72f))
                        .padding(horizontal = 22.dp, vertical = 12.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("1x", color = Color.White, fontSize = 18.sp)
                        Text(
                            if (playing) "❚❚" else "▶",
                            color = Color.White,
                            fontSize = 33.sp,
                            modifier = Modifier.clickable {
                                if (elapsed >= 3) elapsed = 0
                                playing = !playing
                            }
                        )
                        Text("⌁", color = Color.White, fontSize = 27.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("00:0" + elapsed, color = Color.White, fontSize = 17.sp)
                        LinearProgressIndicator(
                            progress = { elapsed / 3f },
                            modifier = Modifier.weight(1f).padding(horizontal = 14.dp),
                            color = Color(0xFF8EA7E8),
                            trackColor = Color(0xFF44444A)
                        )
                        Text("00:03", color = Color.White, fontSize = 17.sp)
                    }
                }
            }

            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(136.dp)
                    .background(Color.Black.copy(alpha = .9f))
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlyphButton(if (favorite) "★" else "☆", onFavorite, 34, Color.White)
                GlyphButton("✎", onEditOrInfo, 34, Color.White)
                GlyphButton("↗", { toast(context, "已准备分享此媒体") }, 35, Color.White)
                GlyphButton("▱", { deleteDialog = true }, 34, Color.White)
            }
        }
    }

    if (deleteDialog) {
        AlertDialog(
            onDismissRequest = { deleteDialog = false },
            title = { Text("移到回收站？") },
            text = { Text("此项目将在回收站中保留 30 天。") },
            confirmButton = {
                TextButton(onClick = { deleteDialog = false; onDelete() }) { Text("移到回收站") }
            },
            dismissButton = { TextButton(onClick = { deleteDialog = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun GlyphButton(
    glyph: String,
    onClick: () -> Unit,
    size: Int,
    color: Color = Color(0xFF303035)
) {
    Text(
        glyph,
        color = color,
        fontSize = size.sp,
        modifier = Modifier
            .size(54.dp)
            .clickable(onClick = onClick)
            .wrapContentSize(Alignment.Center)
    )
}

@Composable
private fun SortDialog(onDismiss: () -> Unit) {
    var sort by remember { mutableStateOf("修改日期") }
    var direction by remember { mutableStateOf("降序") }
    var currentOnly by remember { mutableStateOf(false) }
    val methods = listOf("名称", "路径", "大小", "修改日期", "拍摄日期", "随机")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("排序方式", color = MaterialTheme.colorScheme.primary, fontSize = 30.sp) },
        text = {
            Column {
                methods.forEach { method ->
                    ChoiceRow(method, sort == method) { sort = method }
                }
                Divider()
                ChoiceRow("升序", direction == "升序") { direction = "升序" }
                ChoiceRow("降序", direction == "降序") { direction = "降序" }
                Divider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = currentOnly, onCheckedChange = { currentOnly = it })
                    Text("仅应用于此文件夹", fontSize = 18.sp)
                }
                Text("请注意：分组和排序是两种相互独立的文件组织方式", fontSize = 14.sp)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun ChoiceRow(text: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onSelect).padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(text, fontSize = 19.sp)
    }
}

@Composable
private fun FilterDialog(onDismiss: () -> Unit) {
    var image by remember { mutableStateOf(true) }
    var video by remember { mutableStateOf(true) }
    var gif by remember { mutableStateOf(true) }
    var raw by remember { mutableStateOf(true) }
    var svg by remember { mutableStateOf(true) }
    var portrait by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("过滤显示的文件", color = MaterialTheme.colorScheme.primary, fontSize = 29.sp) },
        text = {
            Column {
                CheckLine("图片", image) { image = it }
                CheckLine("视频", video) { video = it }
                CheckLine("GIF", gif) { gif = it }
                CheckLine("RAW 图像", raw) { raw = it }
                CheckLine("SVG", svg) { svg = it }
                CheckLine("竖向", portrait) { portrait = it }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun CheckLine(text: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChecked(!checked) }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onChecked)
        Text(text, fontSize = 19.sp)
    }
}

@Composable
private fun FolderPickerDialog(onDismiss: () -> Unit) {
    val folders = listOf(
        "Alarms" to 0, "Android" to 3, "Audiobooks" to 0, "DCIM" to 0,
        "Documents" to 0, "Download" to 1, "Movies" to 0, "Music" to 0,
        "Notifications" to 0
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择文件夹", color = MaterialTheme.colorScheme.primary, fontSize = 29.sp) },
        text = {
            Column {
                OutlinedButton(onClick = {}) { Text("内部存储空间", fontSize = 19.sp) }
                LazyColumn(Modifier.height(480.dp)) {
                    items(folders) { item ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("■", color = Color(0xFF98AAD7), fontSize = 38.sp)
                            Spacer(Modifier.width(18.dp))
                            Column {
                                Text(item.first, fontSize = 20.sp)
                                Text(item.second.toString() + " 项", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun SimpleChoiceDialog(
    title: String,
    choices: List<String>,
    selected: String,
    onDismiss: () -> Unit
) {
    var choice by remember { mutableStateOf(selected) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                choices.forEach { item ->
                    ChoiceRow(item, item == choice) { choice = item }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun AllFilesDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Text(
                "请授权本应用访问您的所有文件，没有此权限它可能无法正常工作。\n\n" +
                    "如果您不想授予本应用该权限，也可以前往设备的“设置”-“应用”-“特殊权限”-“媒体管理应用”，在该页面允许本应用管理媒体文件。",
                fontSize = 20.sp,
                lineHeight = 31.sp,
                color = Color(0xFF09295A)
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("所有文件") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("仅媒体文件") } }
    )
}

@Composable
private fun ResizeDialog(onDismiss: () -> Unit) {
    var width by remember { mutableStateOf("900") }
    var height by remember { mutableStateOf("900") }
    var name by remember { mutableStateOf("sample_photo") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("调整大小", color = MaterialTheme.colorScheme.primary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(width, { width = it }, label = { Text("宽度") }, singleLine = true)
                OutlinedTextField(height, { height = it }, label = { Text("高度") }, singleLine = true)
                OutlinedTextField(
                    "内部存储空间/Download/BlackBoxBench/",
                    {},
                    label = { Text("文件夹") },
                    singleLine = true
                )
                OutlinedTextField(name, { name = it }, label = { Text("文件名") }, singleLine = true)
                OutlinedTextField("png", {}, label = { Text("扩展名") }, singleLine = true)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun SettingsScreen(onBack: () -> Unit, onAppearance: () -> Unit) {
    var pullRefresh by remember { mutableStateOf(true) }
    var ultraHdr by remember { mutableStateOf(true) }
    var blackBackground by remember { mutableStateOf(true) }
    var keepScreen by remember { mutableStateOf(true) }
    var downExit by remember { mutableStateOf(true) }
    var notch by remember { mutableStateOf(true) }
    var largeZoom by remember { mutableStateOf(true) }
    var gestureRotate by remember { mutableStateOf(true) }
    var preserveDate by remember { mutableStateOf(true) }
    var bottomButtons by remember { mutableStateOf(true) }
    var recycleBin by remember { mutableStateOf(true) }

    Column(Modifier.fillMaxSize()) {
        PlainTopBar("设置", onBack)
        LazyColumn(Modifier.fillMaxSize()) {
            item { Category("常规") }
            item { SettingLink("自定义外观", onAppearance) }
            item { SettingValue("应用语言", "中文") }
            item { SettingValue("日期和时间格式", "系统默认") }
            item { SettingLink("管理包含的文件夹") {} }
            item { SettingLink("管理排除的文件夹") {} }
            item { SettingSwitch("显示隐藏的文件", false) {} }
            item { SettingSwitch("搜索所有文件，而不仅是媒体", false) {} }

            item { Category("视频") }
            item { SettingSwitch("自动播放视频", false) {} }
            item { SettingSwitch("记住视频播放位置", false) {} }
            item { SettingSwitch("循环播放视频", false) {} }
            item { SettingSwitch("横向手势控制视频", false) {} }
            item { SettingSwitch("使用纵向滑动手势控制音量和亮度", true) {} }
            item { SettingValue("点击视频时", "打开默认播放器") }

            item { Category("缩略图") }
            item { SettingSwitch("将缩略图裁剪为正方形", true) {} }
            item { SettingSwitch("GIF 缩略图播放动画", false) {} }
            item { SettingValue("文件缩略图样式", "圆角") }
            item { SettingValue("文件夹缩略图样式", "方形") }
            item { SettingSwitch("水平滚动缩略图", false) {} }
            item { SettingSwitch("应用顶部下拉刷新", pullRefresh) { pullRefresh = it } }

            item { Category("全屏显示") }
            item { SettingSwitch("全屏时将屏幕调到最大亮度", false) {} }
            item { SettingSwitch("以 HDR 模式显示 Ultra HDR 照片", ultraHdr) { ultraHdr = it } }
            item { SettingSwitch("全屏时使用黑色背景", blackBackground) { blackBackground = it } }
            item { SettingSwitch("全屏时自动隐藏系统界面", false) {} }
            item { SettingSwitch("点击屏幕边缘切换文件", false) {} }
            item { SettingSwitch("全屏查看照片时保持屏幕开启", keepScreen) { keepScreen = it } }
            item { SettingSwitch("使用纵向滑动手势控制图像亮度", false) {} }
            item { SettingSwitch("使用下滑手势退出全屏", downExit) { downExit = it } }
            item { SettingSwitch("显示刘海（如果可用）", notch) { notch = it } }
            item { SettingValue("全屏时文件的旋转方向", "跟随系统设置") }

            item { Category("缩放") }
            item { SettingSwitch("允许大幅缩放", largeZoom) { largeZoom = it } }
            item { SettingSwitch("使用手势旋转图像", gestureRotate) { gestureRotate = it } }
            item { SettingSwitch("使用最高图像质量", false) {} }
            item { SettingSwitch("双指双击切换到 1:1", false) {} }
            item { SettingSwitch("在全屏中显示更多详情", false) {} }

            item { Category("安全") }
            item { SettingSwitch("密码保护整个应用", false) {} }
            item { SettingSwitch("密码保护已排除的文件夹", false) {} }
            item { SettingSwitch("密码保护删除和移动", false) {} }

            item { Category("文件操作") }
            item { SettingSwitch("内容移走后删除空文件夹", false) {} }
            item { SettingSwitch("保留文件修改日期", preserveDate) { preserveDate = it } }
            item { SettingSwitch("跳过删除确认", false) {} }
            item { SettingSwitch("显示底部操作按钮", bottomButtons) { bottomButtons = it } }
            item { SettingLink("管理底部操作按钮") {} }

            item { Category("回收站") }
            item { SettingSwitch("将删除的项目移至回收站", recycleBin) { recycleBin = it } }
            item { SettingSwitch("在文件夹界面显示回收站", true) {} }
            item { SettingSwitch("在首页末尾显示回收站", false) {} }
            item { SettingValue("清空回收站", "0 B") }

            item { Category("迁移") }
            item { SettingValue("清除缓存", "391.5 kB") }
            item { SettingLink("导出收藏") {} }
            item { SettingLink("导入收藏") {} }
            item { SettingLink("导出设置") {} }
            item { SettingLink("导入设置") {} }
            item { Spacer(Modifier.height(28.dp)) }
        }
    }
}

@Composable
private fun PlainTopBar(title: String, onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(82.dp)
            .background(Color(0xFFE8ECFC))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlyphButton("‹", onBack, 43)
        Text(title, fontSize = 29.sp, color = Color(0xFF202127))
    }
}

@Composable
private fun Category(text: String) {
    Text(
        text,
        color = Color(0xFF7A94D1),
        fontSize = 19.sp,
        modifier = Modifier.fillMaxWidth().padding(start = 18.dp, top = 28.dp, bottom = 10.dp)
    )
}

@Composable
private fun SettingLink(title: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 17.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 19.sp, modifier = Modifier.weight(1f))
        Text("›", fontSize = 26.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingValue(title: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 17.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 19.sp, modifier = Modifier.weight(1f))
        Text(value, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingSwitch(title: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChecked(!checked) }.padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 18.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun AppearanceScreen(onBack: () -> Unit) {
    var theme by remember { mutableStateOf("系统默认") }
    var font by remember { mutableStateOf("系统默认") }
    var themeDialog by remember { mutableStateOf(false) }
    var colorDialog by remember { mutableStateOf(false) }
    var fontDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        PlainTopBar("自定义外观", onBack)
        Category("主题和颜色")
        Row(
            Modifier.fillMaxWidth().clickable { themeDialog = true }.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("应用主题", fontSize = 19.sp, modifier = Modifier.weight(1f))
            Text(theme, fontSize = 18.sp)
        }
        Row(
            Modifier.fillMaxWidth().clickable { colorDialog = true }.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("应用图标颜色", fontSize = 19.sp, modifier = Modifier.weight(1f))
            Box(Modifier.size(44.dp).background(Color(0xFF106D1F), CircleShape))
        }
        Divider()
        Category("字体")
        Row(
            Modifier.fillMaxWidth().clickable { fontDialog = true }.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("应用字体", fontSize = 19.sp, modifier = Modifier.weight(1f))
            Text(font, fontSize = 18.sp)
        }
    }

    if (themeDialog) {
        ChoiceStateDialog(
            title = "应用主题",
            choices = listOf("系统默认", "浅色主题", "深色主题", "深红色", "白色", "黑白两色", "自定义"),
            selected = theme,
            onSelect = { theme = it },
            onDismiss = { themeDialog = false }
        )
    }
    if (fontDialog) {
        ChoiceStateDialog(
            title = "应用字体",
            choices = listOf("系统默认", "等宽字体", "选择字体文件…"),
            selected = font,
            onSelect = { font = it },
            onDismiss = { fontDialog = false }
        )
    }
    if (colorDialog) {
        val colors = listOf(
            Color(0xFFB3261E), Color(0xFF7D3C98), Color(0xFF3154A5),
            Color(0xFF0288A8), Color(0xFF106D1F), Color(0xFFF0B400),
            Color(0xFFE06B24), Color(0xFF70463A), Color.Gray, Color.Black
        )
        AlertDialog(
            onDismissRequest = { colorDialog = false },
            title = { Text("应用图标颜色") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(100.dp).background(Color(0xFF106D1F), RoundedCornerShape(22.dp)),
                        contentAlignment = Alignment.Center
                    ) { Text("▣", color = Color.White, fontSize = 54.sp) }
                    Spacer(Modifier.height(18.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        colors.take(5).forEach { color ->
                            Box(Modifier.size(34.dp).background(color, CircleShape))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        colors.drop(5).forEach { color ->
                            Box(Modifier.size(34.dp).background(color, CircleShape))
                        }
                    }
                    Text("#106D1F", modifier = Modifier.padding(top = 18.dp), fontSize = 19.sp)
                }
            },
            confirmButton = { TextButton(onClick = { colorDialog = false }) { Text("确定") } },
            dismissButton = { TextButton(onClick = { colorDialog = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun ChoiceStateDialog(
    title: String,
    choices: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                choices.forEach { choice ->
                    ChoiceRow(choice, choice == selected) {
                        onSelect(choice)
                        onDismiss()
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun AboutScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        PlainTopBar("关于", onBack)
        LazyColumn(Modifier.fillMaxSize()) {
            item { AboutLine("⚙", "常见问题") }
            item { AboutLine("⚠", "已知问题") }
            item { AboutLine("?", "hello@fossify.org") }
            item { Divider() }
            item { Category("帮助我们") }
            item { AboutLine("↗", "分享给好友") }
            item { AboutLine("♣", "贡献者") }
            item { AboutLine("♡", "向 Fossify 捐赠") }
            item { Divider() }
            item { Category("社交网络") }
            item { AboutLine("●", "GitHub") }
            item { AboutLine("●", "Reddit") }
            item { AboutLine("●", "Telegram") }
            item { Divider() }
            item { Category("其他") }
            item { AboutLine("@", "隐私政策") }
            item { AboutLine("▤", "第三方许可") }
            item {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("ⓘ", fontSize = 28.sp, modifier = Modifier.width(54.dp))
                    Column {
                        Text("版本 1.13.1", fontSize = 20.sp)
                        Text("org.fossify.gallery", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutLine(icon: String, text: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 17.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 27.sp, modifier = Modifier.width(54.dp))
        Text(text, fontSize = 20.sp)
    }
}

@Composable
private fun TrashScreen(
    deletedPhoto: Boolean,
    deletedVideo: Boolean,
    onRestore: (MediaEntry) -> Unit,
    onDeleteForever: (MediaEntry) -> Unit,
    onBack: () -> Unit
) {
    val deleted = buildList {
        if (deletedVideo) add(videoEntry)
        if (deletedPhoto) add(photoEntry)
    }
    Column(Modifier.fillMaxSize()) {
        PlainTopBar("回收站", onBack)
        if (deleted.isEmpty()) {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("回收站为空", fontSize = 25.sp)
                Text("删除的项目会在此保留 30 天", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Text("项目将在 30 天后自动清除", modifier = Modifier.padding(18.dp), color = MaterialTheme.colorScheme.primary)
            LazyColumn {
                items(deleted) { entry ->
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(92.dp)) {
                            if (entry.isVideo) StoryArt() else LakeArt()
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(entry.name, fontSize = 18.sp)
                            Text("剩余 30 天", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { onRestore(entry) }) { Text("恢复") }
                        TextButton(onClick = { onDeleteForever(entry) }) { Text("删除") }
                    }
                }
            }
        }
    }
}

private fun toast(context: Context, text: String) {
    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
}
