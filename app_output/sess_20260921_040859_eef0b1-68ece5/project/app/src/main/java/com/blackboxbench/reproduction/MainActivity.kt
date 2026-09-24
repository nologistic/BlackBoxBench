package com.blackboxbench.reproduction

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.*


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BenchmarkAppTheme { GalleryApp() } }
    }
}

enum class Page { HOME, ALBUM, VIEWER, SETTINGS, APPEARANCE, TRASH, FAVORITES, ABOUT, SEARCH_FILES, LOCKED, EDITOR }

@Composable
fun GalleryApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("gallery_state", Context.MODE_PRIVATE) }
    var page by remember { mutableStateOf(Page.HOME) }
    var previous by remember { mutableStateOf(Page.HOME) }
    var album by remember { mutableStateOf("Camera") }
    var imageIndex by remember { mutableIntStateOf(0) }
    var columns by remember { mutableIntStateOf(prefs.getInt("columns", 2)) }
    var favorite by remember { mutableStateOf(prefs.getBoolean("favorite", false)) }
    var trashCount by remember { mutableIntStateOf(prefs.getInt("trash", 0)) }
    var locked by remember { mutableStateOf(prefs.getBoolean("locked", false)) }
    var unlocked by remember { mutableStateOf(false) }
    var savedPin by remember { mutableStateOf(prefs.getString("pin", "1234") ?: "1234") }
    var selection by remember { mutableStateOf(false) }
    var setupLock by remember { mutableStateOf(false) }
    var sortDialog by remember { mutableStateOf(false) }
    var columnDialog by remember { mutableStateOf(false) }
    var folderDialog by remember { mutableStateOf(false) }
    var slideshowDialog by remember { mutableStateOf(false) }
    var infoDialog by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }
    var folderMenu by remember { mutableStateOf(false) }
    var homeMenu by remember { mutableStateOf(false) }
    var viewerMenu by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    fun open(target: Page) { previous = page; page = target; selection = false }
    fun saveColumns(v: Int) { columns = v; prefs.edit().putInt("columns", v).apply() }
    fun setFav(v: Boolean) { favorite = v; prefs.edit().putBoolean("favorite", v).apply() }
    fun setTrash(v: Int) { trashCount = v; prefs.edit().putInt("trash", v).apply() }
    fun setLocked(v: Boolean) { locked = v; prefs.edit().putBoolean("locked", v).apply() }

    BackHandler(page != Page.HOME || selection) {
        if (selection) selection = false
        else when (page) {
            Page.VIEWER -> page = if (album == "收藏") Page.FAVORITES else Page.ALBUM
            Page.APPEARANCE -> page = Page.SETTINGS
            Page.EDITOR -> page = Page.VIEWER
            else -> page = Page.HOME
        }
    }

    when (page) {
        Page.HOME -> HomeScreen(
            columns = columns, favorite = favorite, locked = locked,
            selection = selection, onSelect = { selection = it },
            onOpenAlbum = { name ->
                album = name
                if (name == "收藏") open(Page.FAVORITES)
                else if (name == "BlackBoxBench" && locked && !unlocked) open(Page.LOCKED)
                else open(Page.ALBUM)
            },
            onCamera = { message = "相机不可用" },
            onSort = { sortDialog = true }, onColumns = { columnDialog = true },
            onNewFolder = { folderDialog = true }, onSettings = { open(Page.SETTINGS) },
            onTrash = { open(Page.TRASH) }, onAbout = { open(Page.ABOUT) },
            onSlideshow = { slideshowDialog = true }, onFolderMenu = { folderMenu = true },
            onMenuExpanded = { homeMenu = it }, menuExpanded = homeMenu,
            onSearchFiles = { open(Page.SEARCH_FILES) }
        )
        Page.ALBUM, Page.FAVORITES -> AlbumScreen(
            title = if (page == Page.FAVORITES) "收藏" else album,
            count = if (page == Page.FAVORITES) if (favorite) 1 else 0 else if (album == "Camera") 30 else 2,
            columns = columns, selection = selection, onSelect = { selection = it },
            onBack = { page = Page.HOME },
            onOpen = { imageIndex = it; open(Page.VIEWER) },
            onSort = { sortDialog = true }, onColumns = { columnDialog = true },
            onSlideshow = { slideshowDialog = true },
            onDelete = { deleteDialog = true }
        )
        Page.VIEWER -> ViewerScreen(
            index = imageIndex, total = if (album == "BlackBoxBench") 2 else 30,
            favorite = favorite, onFavorite = { setFav(!favorite) },
            onBack = { page = if (album == "收藏") Page.FAVORITES else Page.ALBUM },
            onNext = { delta -> imageIndex = (imageIndex + delta).coerceIn(0, if (album == "BlackBoxBench") 1 else 29) },
            onDelete = { deleteDialog = true }, onInfo = { infoDialog = true },
            onEdit = { open(Page.EDITOR) }, onSlideshow = { slideshowDialog = true },
            menuExpanded = viewerMenu, onMenuExpanded = { viewerMenu = it }
        )
        Page.EDITOR -> EditorScreen(imageIndex, { page = Page.VIEWER }) {
            message = "已将副本保存到 Camera"; page = Page.VIEWER
        }
        Page.SETTINGS -> SettingsScreen(
            onBack = { page = Page.HOME }, onAppearance = { open(Page.APPEARANCE) },
            locked = locked, onLock = { setupLock = true },
            onTrash = { open(Page.TRASH) }, onAbout = { open(Page.ABOUT) }
        )
        Page.APPEARANCE -> AppearanceScreen { page = Page.SETTINGS }
        Page.TRASH -> TrashScreen(
            count = trashCount, selection = selection, onSelect = { selection = it },
            onBack = { page = Page.HOME },
            onRestore = { setTrash(0); selection = false; message = "已恢复" },
            onDelete = { setTrash(0); selection = false; message = "已永久删除" }
        )
        Page.ABOUT -> AboutScreen { page = Page.HOME }
        Page.SEARCH_FILES -> FileSearchScreen { page = Page.HOME }
        Page.LOCKED -> PinScreen(
            title = "输入 PIN 码", correctPin = savedPin,
            onSuccess = { unlocked = true; page = Page.ALBUM },
            onBack = { page = Page.HOME }
        )
    }

    if (sortDialog) SortDialog { sortDialog = false }
    if (columnDialog) ColumnDialog(columns, { saveColumns(it); columnDialog = false }, { columnDialog = false })
    if (folderDialog) NewFolderDialog({ folderDialog = false }) { name -> folderDialog = false; message = "已创建文件夹「" + name + "」" }
    if (slideshowDialog) SlideshowDialog({ slideshowDialog = false }) { slideshowDialog = false; message = "幻灯片放映已开始" }
    if (infoDialog) InfoDialog(imageIndex) { infoDialog = false }
    if (deleteDialog) DeleteDialog({ deleteDialog = false }) {
        deleteDialog = false; setTrash(trashCount + 1); selection = false
        if (page == Page.VIEWER) page = Page.ALBUM
        message = "已移至回收站"
    }
    if (folderMenu) FolderActionsDialog(locked, { folderMenu = false }) { action ->
        folderMenu = false
        when (action) {
            "lock" -> setupLock = true
            "unlock" -> { setLocked(false); unlocked = false; message = "文件夹已解锁" }
            "hide" -> message = "文件夹已隐藏"
        }
    }
    if (setupLock) LockSetupDialog(
        onDismiss = { setupLock = false },
        onComplete = { pin -> savedPin = pin; prefs.edit().putString("pin", pin).apply(); setLocked(true); unlocked = false; setupLock = false; selection = false; message = "文件夹已锁定" }
    )
    message?.let { ToastNotice(it) { message = null } }
}
