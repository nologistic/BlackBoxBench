package com.blackboxbench.reproduction

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import kotlin.math.roundToInt

data class Book(
    val id: String,
    val title: String,
    val author: String,
    val format: String,
    val pages: Int,
    val progress: Float,
    val tags: List<String>,
    val colors: List<Color>
)

private val BaseBooks = listOf(
    Book("bk001", "确定性生成手册", "林汐", "EPUB", 320, .42f, listOf("技术"), listOf(Color(0xFF132743), Color(0xFF3B7A93))),
    Book("bk002", "像素考古学", "陈北辰", "EPUB", 280, 1f, listOf("技术", "历史"), listOf(Color(0xFF6C3B63), Color(0xFFC77B8B))),
    Book("bk003", "黑盒白皮书（第 2 版）", "黑盒研究院", "PDF", 156, .12f, listOf("技术"), listOf(Color(0xFF202936), Color(0xFF6B7280))),
    Book("bk004", "早餐烘焙十二课", "莫莫", "EPUB", 198, .65f, listOf("生活"), listOf(Color(0xFF9A4E2B), Color(0xFFF0B35A))),
    Book("bk005", "城市漫步地图集", "乔桥", "PDF", 240, 0f, listOf("生活", "旅行"), listOf(Color(0xFF285D52), Color(0xFF76B39D))),
    Book("bk006", "信号与系统入门", "周野", "PDF", 412, .88f, listOf("教材"), listOf(Color(0xFF303C6C), Color(0xFF86A8E7))),
    Book("bk007", "夜航诗集", "阿岩", "FB2", 128, .30f, listOf("文学"), listOf(Color(0xFF191A3A), Color(0xFF55558B))),
    Book("bk008", "合成音效图鉴", "莫莫", "MOBI", 176, 0f, listOf("技术"), listOf(Color(0xFF126E82), Color(0xFF51C4D3))),
    Book("bk009", "翻页的哲学", "林汐", "EPUB", 224, .75f, listOf("文学"), listOf(Color(0xFF643A2B), Color(0xFFB88964))),
    Book("bk010", "离线生活指南", "陈北辰", "EPUB", 256, .05f, listOf("生活"), listOf(Color(0xFF346751), Color(0xFFC5D7BD))),
    Book("bk011", "书签设计史", "乔桥", "FB2", 144, 1f, listOf("历史"), listOf(Color(0xFF5B4B8A), Color(0xFFB9A7E8))),
    Book("bk012", "合集：黑盒短篇小说选", "多位作者", "EPUB", 384, .20f, listOf("文学", "合集"), listOf(Color(0xFF3C415C), Color(0xFF738598)))
)

class MainActivity : ComponentActivity() {
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = 0xFF3F51B5.toInt()
        window.navigationBarColor = 0xFFFFFFFF.toInt()
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = true
        setContent { ReproducedApp() }
    }
}

@Composable
fun ReproducedApp() {
    BenchmarkAppTheme {
        val context = LocalContext.current
        val prefs = remember { context.getSharedPreferences("reader_state", Context.MODE_PRIVATE) }
        var activeTab by rememberSaveable { mutableStateOf("书库") }
        var search by rememberSaveable { mutableStateOf("") }
        var listView by rememberSaveable { mutableStateOf(false) }
        var sortBy by rememberSaveable { mutableStateOf("时间") }
        var sortMenu by remember { mutableStateOf(false) }
        var viewMenu by remember { mutableStateOf(false) }
        var drawer by remember { mutableStateOf(false) }
        var pendingBook by remember { mutableStateOf<Book?>(null) }
        var readerBook by remember { mutableStateOf<Book?>(null) }
        var readerMode by rememberSaveable { mutableStateOf("上下翻页") }
        var createdNote by remember { mutableStateOf(prefs.getBoolean("created_note", false)) }
        var createDialog by remember { mutableStateOf(false) }
        var noteTitle by remember { mutableStateOf("阅读摘记") }
        var noteBody by remember { mutableStateOf("一条离线保存的公开测试笔记。") }
        val favorites = remember {
            mutableStateListOf<String>().apply {
                val saved = prefs.getString("favorites", "bk001").orEmpty()
                if (saved.isNotBlank()) addAll(saved.split(","))
            }
        }
        val books = remember(createdNote) {
            if (createdNote) {
                BaseBooks + Book("note", "阅读摘记", "本地文本", "TXT", 1, 0f, listOf("待读"), listOf(Color(0xFF087F23), Color(0xFF39A852)))
            } else BaseBooks
        }

        BackHandler(enabled = drawer || readerBook != null || pendingBook != null) {
            when {
                drawer -> drawer = false
                pendingBook != null -> pendingBook = null
                readerBook != null -> readerBook = null
            }
        }

        Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().height(36.dp).background(LibreraBlue).align(Alignment.TopCenter))
                Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).background(Color(0xFFF8F8F8))) {
                    if (readerBook != null) {
                ReaderScreen(
                    book = readerBook!!,
                    mode = readerMode,
                    prefs = prefs,
                    onClose = { readerBook = null },
                    onOpenLibrary = { readerBook = null; activeTab = "书库" }
                )
            } else {
                Box(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize()) {
                        when (activeTab) {
                            "书库" -> LibraryScreen(
                                books = books,
                                search = search,
                                onSearch = { search = it },
                                listView = listView,
                                sortBy = sortBy,
                                favorites = favorites,
                                onFavorite = { id ->
                                    if (favorites.contains(id)) favorites.remove(id) else favorites.add(id)
                                    prefs.edit().putString("favorites", favorites.joinToString(",")).apply()
                                },
                                onOpen = { pendingBook = it },
                                onMenu = { drawer = true },
                                sortMenu = sortMenu,
                                setSortMenu = { sortMenu = it },
                                viewMenu = viewMenu,
                                setViewMenu = { viewMenu = it },
                                onSort = { sortBy = it; sortMenu = false },
                                onView = { listView = it; viewMenu = false }
                            )
                            "文件夹" -> FolderScreen(books, onOpen = { pendingBook = it }, onCreate = { createDialog = true })
                            "最近" -> RecentScreen(books, onOpen = { pendingBook = it })
                            "收藏" -> FavoritesScreen(books.filter { favorites.contains(it.id) }, onOpen = { pendingBook = it }, onFavorite = { id ->
                                favorites.remove(id)
                                prefs.edit().putString("favorites", favorites.joinToString(",")).apply()
                            })
                            "书签" -> BookmarksScreen(books, prefs, onOpen = {
                                readerBook = it
                                readerMode = "上下翻页"
                            })
                        }
                        BottomNavigation(activeTab) { activeTab = it }
                    }
                    if (drawer) PreferencesDrawer(onClose = { drawer = false })
                    if (pendingBook != null) {
                        PageModeDialog(
                            book = pendingBook!!,
                            onDismiss = { pendingBook = null },
                            onSelect = {
                                readerMode = it
                                readerBook = pendingBook
                                pendingBook = null
                            }
                        )
                    }
                    if (createDialog) {
                        CreateTextDialog(
                            title = noteTitle,
                            body = noteBody,
                            onTitle = { noteTitle = it },
                            onBody = { noteBody = it },
                            onDismiss = { createDialog = false },
                            onSave = {
                                createdNote = true
                                prefs.edit().putBoolean("created_note", true).apply()
                                createDialog = false
                            }
                        )
                    }
                }
                }
            }
        }
    }
}
}

@Composable
private fun ColumnScope.LibraryScreen(
    books: List<Book>,
    search: String,
    onSearch: (String) -> Unit,
    listView: Boolean,
    sortBy: String,
    favorites: List<String>,
    onFavorite: (String) -> Unit,
    onOpen: (Book) -> Unit,
    onMenu: () -> Unit,
    sortMenu: Boolean,
    setSortMenu: (Boolean) -> Unit,
    viewMenu: Boolean,
    setViewMenu: (Boolean) -> Unit,
    onSort: (String) -> Unit,
    onView: (Boolean) -> Unit
) {
    val filtered = books.filter { search.isBlank() || it.title.contains(search, true) || it.author.contains(search, true) || it.id.contains(search, true) || it.format.contains(search, true) }
    val shown = when (sortBy) {
        "标题" -> filtered.sortedBy { it.title }
        "作者" -> filtered.sortedBy { it.author }
        "格式" -> filtered.sortedBy { it.format }
        "页数" -> filtered.sortedByDescending { it.pages }
        else -> filtered
    }
    Column(Modifier.fillMaxWidth().weight(1f)) {
        Row(
            Modifier.fillMaxWidth().height(58.dp).background(LibreraBlue).padding(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SquareTool("☰", onMenu, Modifier.width(42.dp))
            Surface(
                modifier = Modifier.weight(1f).height(50.dp).border(1.dp, LibreraBlue),
                color = Color.White
            ) {
                Row(Modifier.fillMaxSize().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        if (search.isBlank()) Text("搜索", color = LibreraBlue, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        BasicTextField(
                            value = search,
                            onValueChange = onSearch,
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(color = LibreraBlue, fontSize = 18.sp),
                            cursorBrush = SolidColor(LibreraTeal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Text(shown.size.toString(), color = LibreraBlue, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            }
            Text(
                sortBy + " ▲",
                color = Color.White,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(100.dp).fillMaxHeight().clickable { setSortMenu(true) }.padding(top = 13.dp)
            )
            SquareTool("⟳", {}, Modifier.width(42.dp))
            SquareTool(if (listView) "☷" else "▦", { setViewMenu(true) }, Modifier.width(42.dp))
        }
        if (sortMenu) LibrarySortDialog(onDismiss = { setSortMenu(false) }, onSort = onSort)
        if (viewMenu) LibraryViewDialog(onDismiss = { setViewMenu(false) }, onView = onView)
        Row(
            Modifier.fillMaxWidth().height(42.dp).background(PermissionGreen).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("该应用需要您的许可才能查找和显示您的所有书籍。", color = Color.White, modifier = Modifier.weight(1f), maxLines = 1)
            Text("是", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { }.padding(12.dp))
        }
        if (listView) {
            LazyColumn(Modifier.fillMaxSize().padding(4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(shown, key = { it.id }) { book ->
                    BookListItem(book, favorites.contains(book.id), { onFavorite(book.id) }, { onOpen(book) })
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize().padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(shown, key = { it.id }) { book ->
                    BookCard(book, favorites.contains(book.id), { onFavorite(book.id) }, { onOpen(book) })
                }
            }
        }
    }
}

@Composable
private fun LibrarySortDialog(onDismiss: () -> Unit, onSort: (String) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Column {
                BlueTitleBar("排序方式", listOf("×" to onDismiss))
                LazyColumn(Modifier.height(520.dp)) {
                    items(listOf("文件夹", "文件名", "文件大小", "时间", "标题", "作者", "丛书", "页数", "格式", "语言", "出版时间", "出版商", "最近")) { option ->
                        Text(option, fontSize = 19.sp, modifier = Modifier.fillMaxWidth().clickable { onSort(option) }.padding(horizontal = 22.dp, vertical = 15.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryViewDialog(onDismiss: () -> Unit, onView: (Boolean) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Column {
                BlueTitleBar("展示与分组", listOf("×" to onDismiss))
                LazyColumn(Modifier.height(520.dp)) {
                    item { Text("☷  列表", fontSize = 19.sp, modifier = Modifier.fillMaxWidth().clickable { onView(true) }.padding(18.dp)) }
                    item { Text("☰  简表", fontSize = 19.sp, modifier = Modifier.fillMaxWidth().clickable { onView(true) }.padding(18.dp)) }
                    item { Text("▦  网格", fontSize = 19.sp, modifier = Modifier.fillMaxWidth().clickable { onView(false) }.padding(18.dp)) }
                    item { Text("▥  封面", fontSize = 19.sp, modifier = Modifier.fillMaxWidth().clickable { onView(false) }.padding(18.dp)) }
                    item { HorizontalDivider() }
                    items(listOf("作者", "流派", "丛书", "关键词", "语言", "标签", "出版商", "出版时间")) { option ->
                        Text("#  " + option, fontSize = 18.sp, modifier = Modifier.fillMaxWidth().clickable(onClick = onDismiss).padding(horizontal = 20.dp, vertical = 15.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun BookCard(book: Book, favorite: Boolean, onFavorite: () -> Unit, onOpen: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().shadow(3.dp).clickable(onClick = onOpen),
        color = Color.White
    ) {
        Column {
            CoverArt(book, Modifier.fillMaxWidth().aspectRatio(.72f))
            Text(book.title, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 4.dp, top = 5.dp))
            Text(book.author, fontSize = 13.sp, color = LibreraTeal, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
            Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(book.format, fontSize = 13.sp, color = Color.Gray)
                Spacer(Modifier.weight(1f))
                if (book.progress > 0f) Text((book.progress * 100).roundToInt().toString() + "%", fontSize = 12.sp, color = Color.Gray)
                Text(if (favorite) "★" else "☆", fontSize = 25.sp, color = LibreraBlue, modifier = Modifier.clickable(onClick = onFavorite).padding(horizontal = 3.dp))
                Text("⋮", fontSize = 24.sp, color = LibreraBlue)
            }
        }
    }
}

@Composable
private fun BookListItem(book: Book, favorite: Boolean, onFavorite: () -> Unit, onOpen: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().height(122.dp).shadow(2.dp).clickable(onClick = onOpen), color = Color.White) {
        Row(Modifier.padding(4.dp)) {
            CoverArt(book, Modifier.width(82.dp).fillMaxHeight())
            Column(Modifier.weight(1f).padding(horizontal = 9.dp, vertical = 3.dp)) {
                Text(book.author, color = LibreraTeal, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
                Text(book.title, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(book.id + "." + book.format.lowercase(), color = Color.Gray, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (book.tags.isNotEmpty()) Text("#" + book.tags.joinToString(" #"), color = LibreraTeal, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text(book.format + "  " + book.pages + "页  " + (book.progress * 100).roundToInt() + "%", color = Color.Gray, fontSize = 13.sp)
                }
            }
            Text(if (favorite) "★" else "☆", fontSize = 28.sp, color = LibreraBlue, modifier = Modifier.clickable(onClick = onFavorite).padding(top = 67.dp))
            Text("⋮", fontSize = 28.sp, color = LibreraBlue, modifier = Modifier.padding(top = 67.dp))
        }
    }
}

@Composable
private fun CoverArt(book: Book, modifier: Modifier = Modifier) {
    Box(
        modifier.background(Brush.linearGradient(book.colors)).padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("LIBRERA", color = Color.White.copy(alpha = .72f), fontSize = 10.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(12.dp))
            Text(book.title, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 4, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(12.dp))
            Text("— " + book.author + " —", color = Color.White.copy(alpha = .88f), fontSize = 11.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun SquareTool(label: String, action: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        label,
        color = Color.White,
        fontSize = 25.sp,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxHeight().clickable(onClick = action).padding(top = 8.dp)
    )
}

@Composable
private fun BottomNavigation(active: String, onSelect: (String) -> Unit) {
    val tabs = listOf("书库" to "▣", "文件夹" to "▰", "最近" to "▥", "收藏" to "★", "书签" to "🔖")
    Row(Modifier.fillMaxWidth().height(76.dp).background(LibreraBlue)) {
        tabs.forEach { pair ->
            Column(
                Modifier.weight(1f).fillMaxHeight().clickable { onSelect(pair.first) }.padding(top = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(pair.second, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text(pair.first, color = Color.White, fontSize = 14.sp, fontWeight = if (active == pair.first) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun ColumnScope.FolderScreen(books: List<Book>, onOpen: (Book) -> Unit, onCreate: () -> Unit) {
    Column(Modifier.fillMaxWidth().weight(1f)) {
        BlueTitleBar("/storage/emulated/0/Download (12)", listOf("＋" to onCreate, "⇅" to {}, "▰" to {}))
        Row(Modifier.fillMaxWidth().height(42.dp).background(PermissionGreen).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("允许访问即可扫描更多书籍", color = Color.White, modifier = Modifier.weight(1f))
            Text("是", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Surface(Modifier.fillMaxWidth().padding(6.dp).height(74.dp).clickable { }, shadowElevation = 2.dp) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("📁", fontSize = 30.sp)
                Spacer(Modifier.width(14.dp))
                Column { Text("BlackBoxBench", fontSize = 19.sp); Text("12 本书", color = Color.Gray) }
                Spacer(Modifier.weight(1f))
                Text("☆", color = LibreraBlue, fontSize = 28.sp)
            }
        }
        LazyColumn(Modifier.fillMaxSize().padding(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            items(books.take(7)) { BookListItem(it, false, {}, { onOpen(it) }) }
        }
    }
}

@Composable
private fun ColumnScope.RecentScreen(books: List<Book>, onOpen: (Book) -> Unit) {
    Column(Modifier.fillMaxWidth().weight(1f)) {
        BlueTitleBar("最近 (" + books.count { it.progress > 0f } + ")", listOf("清除" to {}, "☷" to {}))
        LazyColumn(Modifier.fillMaxSize().padding(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(books.filter { it.progress > 0f }.sortedByDescending { it.progress }) {
                BookListItem(it, false, {}, { onOpen(it) })
            }
        }
    }
}

@Composable
private fun ColumnScope.FavoritesScreen(books: List<Book>, onOpen: (Book) -> Unit, onFavorite: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().weight(1f)) {
        BlueTitleBar("收藏", listOf("☷" to {}, "标签" to {}, "播放列表" to {}, "⇅" to {}, "▦" to {}))
        if (books.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("点击书库条目上的 ☆ 添加收藏", color = Color.Gray, fontSize = 18.sp)
            }
        } else {
            val grouped = books.groupBy { it.tags.firstOrNull() ?: "未分类" }
            LazyColumn(Modifier.fillMaxSize().padding(5.dp)) {
                grouped.forEach { (tag, values) ->
                    item { Text("#" + tag + " (" + values.size + ")", fontSize = 21.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp)) }
                    items(values) { BookListItem(it, true, { onFavorite(it.id) }, { onOpen(it) }) }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.BookmarksScreen(books: List<Book>, prefs: android.content.SharedPreferences, onOpen: (Book) -> Unit) {
    var refresh by remember { mutableIntStateOf(0) }
    val marks = remember(refresh) {
        books.mapNotNull { book ->
            val note = prefs.getString("bookmark_note_" + book.id, null)
            if (note != null) Triple(book, prefs.getInt("bookmark_page_" + book.id, 1), note) else null
        }
    }
    Column(Modifier.fillMaxWidth().weight(1f)) {
        BlueTitleBar("书签", listOf("⚙" to {}, "⌕" to {}, "☷" to { refresh++ }))
        if (marks.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无书签\n在阅读器中点击书签按钮即可添加", textAlign = TextAlign.Center, color = Color.Gray, fontSize = 18.sp)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(6.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                items(marks) { mark ->
                    Surface(Modifier.fillMaxWidth().height(92.dp).shadow(2.dp).clickable { onOpen(mark.first) }, color = Color.White) {
                        Row(Modifier.padding(7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.width(62.dp).fillMaxHeight()) {
                                CoverArt(mark.first, Modifier.fillMaxSize())
                                Text(mark.second.toString(), color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).background(LibreraBlue))
                            }
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(mark.first.id + "." + mark.first.format.lowercase(), fontWeight = FontWeight.Bold)
                                Text(mark.third, fontSize = 18.sp)
                            }
                            Text("×", fontSize = 32.sp, color = Color.Gray, modifier = Modifier.clickable {
                                prefs.edit().remove("bookmark_note_" + mark.first.id).remove("bookmark_page_" + mark.first.id).apply()
                                refresh++
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BlueTitleBar(title: String, actions: List<Pair<String, () -> Unit>>) {
    Row(Modifier.fillMaxWidth().height(58.dp).background(LibreraBlue), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color.White, fontSize = 20.sp, modifier = Modifier.weight(1f).padding(horizontal = 12.dp), maxLines = 1)
        actions.forEach { pair ->
            Text(pair.first, color = Color.White, fontSize = if (pair.first.length > 2) 16.sp else 26.sp, modifier = Modifier.clickable(onClick = pair.second).padding(horizontal = 9.dp, vertical = 13.dp))
        }
    }
}

@Composable
private fun PageModeDialog(book: Book, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    var rememberChoice by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(2.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text("翻页模式", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                listOf("① 上下翻页", "② 左右翻页", "Ⓜ 演奏模式").forEach { label ->
                    Text(label, fontSize = 21.sp, color = Color.Gray, modifier = Modifier.fillMaxWidth().clickable { onSelect(label.substring(2)) }.padding(vertical = 17.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = rememberChoice, onCheckedChange = { rememberChoice = it })
                    Text("记住选择", fontSize = 20.sp)
                    Spacer(Modifier.weight(1f))
                    Text("编辑", color = Color.LightGray, fontSize = 17.sp)
                }
                Text(book.title, color = Color.LightGray, fontSize = 12.sp, modifier = Modifier.align(Alignment.End))
            }
        }
    }
}

private val ChapterTitles = listOf("第一章 种子", "第二章 纹理", "第三章 名字", "第四章 验收")
private val ChapterParagraphs = listOf(
    listOf(
        "一切都是从一个数字开始的。给定相同的种子，宇宙会沿着同一条路径展开——这是确定性生成的第一信条。",
        "种子进入伪随机序列，逐行铺开文本、逐块填上颜色。没有人真正创造这些内容；它们只是被找到。",
        "本章末尾的练习：把种子换成你的生日，看看世界有什么不同。"
    ),
    listOf(
        "纹理是重复与扰动之和。完全重复显得机械，完全随机显得嘈杂；介于两者之间的，我们称之为自然。",
        "柏林噪声给出了这中间道路的经典答案：分形叠加，每一层把频率加倍、振幅减半。"
    ),
    listOf(
        "给虚构的人起名字是最微妙的环节：要普通到可信，又不能撞上真实的邻居。",
        "本手册示例人物全部来自虚构姓氏的组合，它们不属于任何真实的人。"
    ),
    listOf(
        "生成只是开始。验收清单逐条核对：内容合法、分布均匀、边界情况不崩、重启后完全一致。",
        "最后一项最重要——一致性是对确定性世界唯一的忠诚。"
    )
)

@Composable
private fun ReaderScreen(
    book: Book,
    mode: String,
    prefs: android.content.SharedPreferences,
    onClose: () -> Unit,
    onOpenLibrary: () -> Unit
) {
    var page by rememberSaveable(book.id) { mutableIntStateOf(prefs.getInt("page_" + book.id, (book.progress * book.pages).roundToInt().coerceAtLeast(1))) }
    var controls by rememberSaveable { mutableStateOf(true) }
    var night by rememberSaveable { mutableStateOf(false) }
    var activeDialog by remember { mutableStateOf<String?>(null) }
    var rsvp by remember { mutableStateOf(false) }
    var searchDialog by remember { mutableStateOf(false) }
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }

    fun changePage(delta: Int) {
        page = (page + delta).coerceIn(1, book.pages)
        prefs.edit().putInt("page_" + book.id, page).apply()
        controls = mode == "演奏模式"
    }

    BackHandler {
        when {
            activeDialog != null -> activeDialog = null
            rsvp -> rsvp = false
            searchDialog -> searchDialog = false
            else -> onClose()
        }
    }

    val paper = if (night) Color(0xFF30261F) else Color(0xFFECE4D3)
    val ink = if (night) Color(0xFFE7DCC9) else Color(0xFF28231E)
    Box(
        Modifier.fillMaxSize().background(paper)
            .pointerInput(mode, page) {
                detectHorizontalDragGestures(
                    onDragStart = { dragX = 0f },
                    onHorizontalDrag = { _, amount -> dragX += amount },
                    onDragEnd = {
                        if (dragX < -80) changePage(1)
                        if (dragX > 80) changePage(-1)
                    }
                )
            }
            .pointerInput(mode, page) {
                detectVerticalDragGestures(
                    onDragStart = { dragY = 0f },
                    onVerticalDrag = { _, amount -> dragY += amount },
                    onDragEnd = {
                        if (mode == "上下翻页") {
                            if (dragY < -80) changePage(1)
                            if (dragY > 80) changePage(-1)
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val third = size.width / 3f
                    when {
                        mode == "演奏模式" && offset.x < third -> changePage(-1)
                        mode == "演奏模式" && offset.x > third * 2 -> changePage(1)
                        else -> controls = !controls
                    }
                }
            }
    ) {
        Column(
            Modifier.fillMaxSize().padding(
                top = if (controls || mode == "演奏模式") 86.dp else 18.dp,
                bottom = if (controls && mode != "演奏模式") 250.dp else 26.dp,
                start = 22.dp,
                end = 22.dp
            )
        ) {
            if (page <= 2) {
                Box(Modifier.fillMaxWidth().weight(1f).background(Brush.verticalGradient(book.colors)).padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(book.title, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(28.dp))
                        Text(book.author, color = Color.White.copy(alpha = .9f), fontSize = 20.sp)
                        Spacer(Modifier.height(60.dp))
                        Text("LIBRERA EDITION", color = Color.White.copy(alpha = .72f), letterSpacing = 4.sp)
                    }
                }
            } else {
                val chapter = ((page - 1) * 4 / book.pages).coerceIn(0, 3)
                Text(ChapterTitles[chapter], color = ink, fontSize = 27.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 22.dp, bottom = 22.dp))
                ChapterParagraphs[chapter].forEach { para ->
                    Text(para, color = ink, fontSize = 22.sp, lineHeight = 35.sp, fontFamily = FontFamily.Serif, modifier = Modifier.padding(bottom = 18.dp))
                }
                Text("第 " + page + " 页", color = ink.copy(alpha = .55f), fontSize = 15.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 20.dp))
                Spacer(Modifier.weight(1f))
            }
        }

        if (mode == "演奏模式") {
            Row(Modifier.fillMaxWidth().height(52.dp).background(LibreraBlue).align(Alignment.TopCenter), verticalAlignment = Alignment.CenterVertically) {
                Text("演奏", color = Color.White, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 18.dp))
                Text(book.title + " — " + page + "/" + book.pages + "  100%", color = Color.White.copy(alpha = .55f), modifier = Modifier.weight(1f), maxLines = 1)
                Text("◀  🔒  📌  ×", color = Color.White.copy(alpha = .7f), fontSize = 18.sp, modifier = Modifier.padding(horizontal = 8.dp).clickable { onClose() })
            }
            Row(Modifier.fillMaxWidth().height(220.dp).align(Alignment.TopCenter).padding(top = 74.dp)) {
                Box(Modifier.weight(1f).fillMaxHeight().border(1.dp, LibreraBlue.copy(alpha = .7f)))
                Box(Modifier.weight(1f).fillMaxHeight().border(1.dp, LibreraBlue.copy(alpha = .7f)))
                Box(Modifier.weight(1f).fillMaxHeight().border(1.dp, LibreraBlue.copy(alpha = .7f)))
            }
            Text("🔖", color = LibreraBlue, fontSize = 29.sp, modifier = Modifier.align(Alignment.TopEnd).padding(top = 57.dp, end = 12.dp).border(1.dp, LibreraBlue).padding(12.dp).clickable { activeDialog = "bookmark" })
        } else if (controls) {
            ReaderTopBar(book, night, onNight = { night = !night }, onSettings = { activeDialog = "settings" }, onReplace = { activeDialog = "replace" }, onClose = onClose)
            ReaderBottomBar(
                book = book,
                page = page,
                mode = mode,
                onSearch = { searchDialog = true },
                onLibrary = onOpenLibrary,
                onGoTo = { activeDialog = "goto" },
                onBookmark = { activeDialog = "bookmark" },
                onToc = { activeDialog = "toc" },
                onTts = { activeDialog = "tts" },
                onMore = { activeDialog = "more" },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        } else {
            Row(Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("06:37", color = ink.copy(alpha = .55f))
                Spacer(Modifier.weight(1f))
                Text(book.title + " — " + page + " / " + book.pages, color = ink.copy(alpha = .55f))
                Spacer(Modifier.weight(1f))
                Text(((page.toFloat() / book.pages) * 100).roundToInt().toString() + "%", color = ink.copy(alpha = .55f))
            }
        }

        when (activeDialog) {
            "settings" -> ReaderSettingsDialog(onDismiss = { activeDialog = null })
            "replace" -> ReplaceDialog(onDismiss = { activeDialog = null })
            "goto" -> GoToDialog(book.pages, page, { changePage(it - page); activeDialog = null }, { activeDialog = null })
            "bookmark" -> BookmarkDialog(book, page, prefs, onDismiss = { activeDialog = null })
            "toc" -> TocDialog(book.pages, onSelect = { changePage(it - page); activeDialog = null }, onDismiss = { activeDialog = null })
            "tts" -> TtsDialog(onDismiss = { activeDialog = null })
            "more" -> ReaderMoreDialog(
                onDismiss = { activeDialog = null },
                onRsvp = { activeDialog = null; rsvp = true }
            )
        }
        if (rsvp) RsvpDialog(onDismiss = { rsvp = false })
        if (searchDialog) ReaderSearchDialog(onDismiss = { searchDialog = false }, onJump = { changePage((page + 2).coerceAtMost(book.pages) - page); searchDialog = false })
    }
}

@Composable
private fun ReaderTopBar(book: Book, night: Boolean, onNight: () -> Unit, onSettings: () -> Unit, onReplace: () -> Unit, onClose: () -> Unit) {
    Column(Modifier.fillMaxWidth().height(86.dp).background(LibreraBlue).padding(horizontal = 12.dp).fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(top = 8.dp)) {
                Text(book.title, color = Color.White, fontSize = 18.sp, maxLines = 1, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Text(book.id.uppercase() + "." + book.format, color = Color.White, fontSize = 13.sp, maxLines = 1, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            Text("×", color = Color.White, fontSize = 34.sp, modifier = Modifier.clickable(onClick = onClose).padding(8.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            listOf("☰" to {}, (if (night) "☀" else "☾") to onNight, "⛶" to {}, "✎" to onReplace, "⚙" to onSettings).forEach {
                Text(it.first, color = Color.White, fontSize = 24.sp, modifier = Modifier.clickable(onClick = it.second).padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
private fun ReaderBottomBar(
    book: Book,
    page: Int,
    mode: String,
    onSearch: () -> Unit,
    onLibrary: () -> Unit,
    onGoTo: () -> Unit,
    onBookmark: () -> Unit,
    onToc: () -> Unit,
    onTts: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth().height(250.dp).background(LibreraBlue).padding(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("☰ 最近", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("×", color = Color.White, fontSize = 30.sp)
        }
        Row(Modifier.height(82.dp)) {
            CoverArt(book, Modifier.width(58.dp).fillMaxHeight())
            Spacer(Modifier.width(6.dp))
            CoverArt(BaseBooks[3], Modifier.width(58.dp).fillMaxHeight())
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            ReaderAction("⌕", "搜索", onSearch)
            ReaderAction("▥", "书库", onLibrary)
            ReaderAction("▣", "页面", onGoTo)
            ReaderAction("🔖", "书签", onBookmark)
            ReaderAction("☷", "目录", onToc)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(page.toString(), color = Color.White)
            Slider(value = page.toFloat(), onValueChange = {}, valueRange = 1f..book.pages.toFloat(), modifier = Modifier.weight(1f))
            Text(book.pages.toString(), color = Color.White)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("▶", color = Color.White, fontSize = 27.sp, modifier = Modifier.clickable { }.padding(horizontal = 12.dp))
            Text("◉", color = Color.White, fontSize = 29.sp, modifier = Modifier.clickable(onClick = onTts).padding(horizontal = 12.dp))
            Spacer(Modifier.weight(1f))
            Text(mode, color = Color.White, fontSize = 18.sp)
            Spacer(Modifier.weight(1f))
            Text("⋮", color = Color.White, fontSize = 30.sp, modifier = Modifier.clickable(onClick = onMore).padding(horizontal = 12.dp))
            Text("🔒", color = Color.Yellow, fontSize = 24.sp)
        }
    }
}

@Composable
private fun ReaderAction(icon: String, label: String, action: () -> Unit) {
    Column(Modifier.clickable(onClick = action).padding(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, color = Color.White, fontSize = 25.sp)
        Text(label, color = Color.White, fontSize = 10.sp)
    }
}

@Composable
private fun ReaderSettingsDialog(onDismiss: () -> Unit) {
    var tab by remember { mutableStateOf("阅读设置") }
    var font by remember { mutableFloatStateOf(20f) }
    var bright by remember { mutableFloatStateOf(40f) }
    var volumeKeys by remember { mutableStateOf(true) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth(.92f).heightIn(max = 720.dp), color = Color.White) {
            Column {
                BlueTitleBar("阅读偏好设置", listOf("×" to onDismiss))
                Row(Modifier.fillMaxWidth()) {
                    listOf("阅读设置", "高级设置", "状态栏").forEach {
                        Text(it, color = if (tab == it) LibreraTeal else Color.Gray, fontWeight = if (tab == it) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.Center, modifier = Modifier.weight(1f).clickable { tab = it }.padding(14.dp))
                    }
                }
                HorizontalDivider()
                Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                    if (tab == "阅读设置") {
                        SettingChoice("屏幕方向", "自动")
                        SliderSetting("字体大小", font, 12f, 42f) { font = it }
                        SettingChoice("字体", "Charis SIL")
                        Text("日间 / 夜间", color = LibreraTeal, fontSize = 18.sp)
                        Row { listOf(Color(0xFFF1EBDD), Color.White, Color(0xFF2F251F), Color.Black).forEach { Box(Modifier.size(50.dp).padding(4.dp).background(it).border(1.dp, Color.Gray)) } }
                        SliderSetting("亮度", bright, 0f, 100f) { bright = it }
                        CheckLine("使用系统亮度", false)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(volumeKeys, { volumeKeys = it })
                            Text("音量键翻页")
                        }
                        CheckLine("反转按键位置", true)
                        SettingChoice("词典", "Google Translate")
                        CheckLine("蓝光过滤器", false)
                    } else if (tab == "高级设置") {
                        SettingChoice("阅读方向", "从左到右")
                        SettingChoice("点击区域", "边缘 25% / 自定义")
                        SettingChoice("双击中央", "适应页面")
                        SliderSetting("滚动速度", 70f, 0f, 100f) {}
                        SettingChoice("自动关闭", "5 分钟")
                        SettingChoice("抗锯齿", "8")
                        CheckLine("允许文字选择", true)
                        CheckLine("按字符选择亚洲文字", true)
                        CheckLine("沿左侧滑动调整亮度", false)
                        CheckLine("夜间 AMOLED 增强", false)
                        CheckLine("循环翻页", false)
                    } else {
                        SettingChoice("夜间状态栏颜色", "#888888")
                        CheckLine("显示状态栏", true)
                        CheckLine("显示书名", true)
                        CheckLine("显示进度滑块", true)
                        CheckLine("显示进度条", true)
                        CheckLine("显示章节与子章节进度", true)
                        CheckLine("显示时间", true)
                        CheckLine("显示电量", true)
                        CheckLine("滑动进度导航", true)
                        CheckLine("显示书签栏", true)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingChoice(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 17.sp)
        Text(value, color = LibreraTeal, fontSize = 17.sp)
    }
}

@Composable
private fun SliderSetting(label: String, value: Float, min: Float, max: Float, onChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(94.dp))
        Text(value.roundToInt().toString(), color = Color.Gray)
        Slider(value = value, onValueChange = onChange, valueRange = min..max, modifier = Modifier.weight(1f))
        Text("−  +", color = LibreraTeal, fontSize = 22.sp)
    }
}

@Composable
private fun CheckLine(label: String, initial: Boolean) {
    var checked by remember { mutableStateOf(initial) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked, { checked = it })
        Text(label)
    }
}

@Composable
private fun ReplaceDialog(onDismiss: () -> Unit) {
    var enabled by remember { mutableStateOf(false) }
    var source by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("替换文本") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(enabled, { enabled = it })
                    Text("启用文本替换")
                }
                OutlinedTextField(source, { source = it; enabled = true }, label = { Text("原文本") })
                OutlinedTextField(target, { target = it; enabled = true }, label = { Text("替换为") })
            }
        },
        confirmButton = { TextButton(onClick = onDismiss, enabled = enabled) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun GoToDialog(pages: Int, current: Int, onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Column {
                BlueTitleBar("前往页面", listOf("×" to onDismiss))
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("页码")
                    Text(current.toString(), color = LibreraTeal, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 18.dp))
                    Spacer(Modifier.weight(1f))
                    Text("✓", color = LibreraTeal, fontSize = 26.sp)
                }
                LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.height(360.dp).padding(8.dp)) {
                    items((1..12).toList()) { p ->
                        Box(
                            Modifier.padding(5.dp).aspectRatio(.75f).background(Color(0xFFE8E0D0)).border(if (p == current) 3.dp else 1.dp, if (p == current) LibreraBlue else Color.LightGray).clickable { onSelect(p.coerceAtMost(pages)) },
                            contentAlignment = Alignment.Center
                        ) { Text(p.toString(), color = Color.Gray, fontSize = 22.sp) }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookmarkDialog(book: Book, page: Int, prefs: android.content.SharedPreferences, onDismiss: () -> Unit) {
    var note by remember { mutableStateOf(prefs.getString("bookmark_note_" + book.id, "") ?: "") }
    var saved by remember { mutableStateOf(note.isNotBlank()) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Column {
                BlueTitleBar("书签", listOf("⌕" to {}, "×" to onDismiss))
                if (saved) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(page.toString(), color = Color.White, modifier = Modifier.background(LibreraBlue).padding(12.dp))
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(book.id + "." + book.format.lowercase(), fontWeight = FontWeight.Bold)
                            Text(note, fontSize = 18.sp)
                        }
                        Text("×", color = Color.Gray, fontSize = 28.sp, modifier = Modifier.clickable {
                            prefs.edit().remove("bookmark_note_" + book.id).remove("bookmark_page_" + book.id).apply()
                            note = ""
                            saved = false
                        })
                    }
                }
                OutlinedTextField(note, { note = it }, label = { Text("添加书签 " + page) }, placeholder = { Text("书签备注") }, modifier = Modifier.fillMaxWidth().padding(12.dp))
                Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Button(onClick = {
                        val finalNote = note.ifBlank { "第 " + page + " 页" }
                        note = finalNote
                        prefs.edit().putString("bookmark_note_" + book.id, finalNote).putInt("bookmark_page_" + book.id, page).apply()
                        saved = true
                    }) { Text("添加") }
                }
            }
        }
    }
}

@Composable
private fun TocDialog(pages: Int, onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth(.92f).heightIn(max = 650.dp), color = Color.White) {
            Column {
                BlueTitleBar("目录", listOf("⋮" to {}, "×" to onDismiss))
                ChapterTitles.forEachIndexed { index, title ->
                    val p = (1 + index * (pages / 4)).coerceAtMost(pages)
                    Row(Modifier.fillMaxWidth().clickable { onSelect(p) }.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (index == 0) "−" else "+", color = LibreraBlue, fontSize = 22.sp)
                        Text(title, fontSize = 19.sp, modifier = Modifier.weight(1f).padding(start = 12.dp))
                        Text(p.toString(), color = LibreraTeal)
                    }
                    if (index == 0) {
                        listOf("种子的约定", "可复现的路径", "练习").forEachIndexed { child, name ->
                            Row(Modifier.fillMaxWidth().clickable { onSelect((p + child + 1).coerceAtMost(pages)) }.padding(start = 52.dp, top = 11.dp, bottom = 11.dp, end = 16.dp)) {
                                Text(name, modifier = Modifier.weight(1f))
                                Text((p + child + 1).toString(), color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TtsDialog(onDismiss: () -> Unit) {
    var speed by remember { mutableFloatStateOf(100f) }
    var pitch by remember { mutableFloatStateOf(100f) }
    var volume by remember { mutableFloatStateOf(5f) }
    var playing by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth(.75f).heightIn(max = 680.dp), color = Color.White) {
            Column {
                BlueTitleBar("文字转语音", listOf("×" to onDismiss))
                Column(Modifier.verticalScroll(rememberScrollState()).padding(14.dp)) {
                    SettingChoice("语音", "Google 语音识别  中文")
                    SliderSetting("语速", speed, 40f, 220f) { speed = it }
                    SliderSetting("音调", pitch, 50f, 150f) { pitch = it }
                    SliderSetting("音量", volume, 0f, 10f) { volume = it }
                    Text("重置", color = LibreraTeal, fontSize = 18.sp, modifier = Modifier.clickable { speed = 100f; pitch = 100f; volume = 5f }.padding(vertical = 10.dp))
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Text("◀◀", color = LibreraBlue, fontSize = 28.sp)
                        Text(if (playing) "Ⅱ" else "▶", color = LibreraBlue, fontSize = 35.sp, modifier = Modifier.clickable { playing = !playing })
                        Text("▶▶", color = LibreraBlue, fontSize = 28.sp)
                        Text("×", color = LibreraBlue, fontSize = 32.sp, modifier = Modifier.clickable { playing = false })
                    }
                    HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    Text("▶ 播放音频文件", color = LibreraTeal, fontSize = 18.sp)
                    SettingChoice("停顿持续时间", "50 ms")
                    SettingChoice("停止朗读（定时器）", "240 分钟")
                    CheckLine("标点符号后短暂停顿", true)
                    OutlinedTextField(",;!?", {}, label = { Text("停顿标点") })
                    CheckLine("启用内容替换", true)
                }
            }
        }
    }
}

@Composable
private fun ReaderMoreDialog(onDismiss: () -> Unit, onRsvp: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = Color.White, modifier = Modifier.widthIn(max = 330.dp)) {
            Column {
                listOf("② 左右翻页", "Ⓜ 演奏模式", "≫ 快速阅读 (RSVP)", "打开方式", "发送文件", "# 添加标签", "加入播放列表", "文件信息").forEach { option ->
                    Text(option, fontSize = 19.sp, modifier = Modifier.fillMaxWidth().clickable {
                        if (option.contains("RSVP")) onRsvp() else onDismiss()
                    }.padding(horizontal = 22.dp, vertical = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun RsvpDialog(onDismiss: () -> Unit) {
    var speed by remember { mutableFloatStateOf(200f) }
    var started by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Column {
                BlueTitleBar("快速阅读 RSVP", listOf("×" to onDismiss))
                Column(Modifier.padding(18.dp)) {
                    SliderSetting("语速", speed, 80f, 600f) { speed = it }
                    SettingChoice("字体大小", "32")
                    SettingChoice("最小行长度", "2")
                    Box(Modifier.fillMaxWidth().height(150.dp).background(Color(0xFFF4F4F4)), contentAlignment = Alignment.Center) {
                        Text(if (started) "确定性" else "准备开始", fontSize = 40.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Text("◀", color = LibreraBlue, fontSize = 28.sp)
                        Button(onClick = { started = !started }) { Text(if (started) "重置" else "开始") }
                        Text("▶", color = LibreraBlue, fontSize = 28.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderSearchDialog(onDismiss: () -> Unit, onJump: () -> Unit) {
    var query by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Column {
                BlueTitleBar("在书中搜索", listOf("×" to onDismiss))
                OutlinedTextField(query, { query = it }, label = { Text("搜索全文") }, trailingIcon = { Text("⌕", color = LibreraBlue, fontSize = 25.sp) }, modifier = Modifier.fillMaxWidth().padding(12.dp))
                if (query.isNotBlank()) {
                    Text(query + " [2:320]", color = LibreraTeal, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 15.dp))
                    listOf("第 3 页 · 一切都是从一个数字开始的", "第 7 页 · 确定性生成的第一信条").forEach {
                        Text(it, fontSize = 17.sp, modifier = Modifier.fillMaxWidth().clickable(onClick = onJump).padding(16.dp))
                    }
                } else {
                    Text("输入关键词搜索当前书籍的全部文本", color = Color.Gray, modifier = Modifier.padding(18.dp))
                }
            }
        }
    }
}

@Composable
private fun CreateTextDialog(title: String, body: String, onTitle: (String) -> Unit, onBody: (String) -> Unit, onDismiss: () -> Unit, onSave: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新文件 (.txt)") },
        text = {
            Column {
                OutlinedTextField(title, onTitle, label = { Text("名称.txt") }, singleLine = true)
                OutlinedTextField(body, onBody, label = { Text("在此处粘贴或输入文字") }, minLines = 7, modifier = Modifier.padding(top = 12.dp))
            }
        },
        confirmButton = { Button(onClick = onSave) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun PreferencesDrawer(onClose: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .52f)).clickable(onClick = onClose)) {
        Surface(Modifier.fillMaxHeight().fillMaxWidth(.86f).clickable(enabled = false) {}, color = Color.White) {
            Column {
                BlueTitleBar("偏好", listOf("退出程序" to {}))
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(48.dp).background(LibreraBlue, RoundedCornerShape(50)), contentAlignment = Alignment.Center) { Text("D", color = Color.White, fontSize = 24.sp) }
                        Column(Modifier.weight(1f).padding(start = 14.dp)) {
                            Text("Demo", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                            Text("阅读配置 Profile", color = Color.Gray)
                        }
                        Text("⚙", color = LibreraBlue, fontSize = 28.sp)
                        Text(" 重置", color = LibreraTeal)
                    }
                    SectionHeader("书库设置")
                    SettingChoice("包含的文件夹", "/storage/emulated/0")
                    Text("＋ 添加文件夹", color = LibreraTeal, modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
                    Text("书籍格式", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
                    listOf("PDF", "DJVU", "FB2", "MOBI / AZW", "EPUB", "DOC / DOCX", "RTF", "ODT", "ZIP").chunked(3).forEach { row ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
                            row.forEach { format ->
                                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(true, {})
                                    Text(format, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    SectionHeader("通用")
                    SettingChoice("标签栏", "底部")
                    SettingChoice("屏幕", "正常")
                    SettingChoice("屏幕方向", "自动")
                    SettingChoice("语言", "系统默认")
                    SettingChoice("字体", "正常")
                    SettingChoice("主题", "明亮")
                    Row(Modifier.padding(16.dp)) {
                        listOf(LibreraBlue, Color(0xFF008F83), Color(0xFFEF6C00), Color(0xFF7B1FA2)).forEach { Box(Modifier.size(45.dp).padding(4.dp).background(it)) }
                    }
                    SettingChoice("单击", "选择模式")
                    SettingChoice("长按", "书籍菜单")
                    CheckLine("启动时打开最后一本书", false)
                    CheckLine("记住最后一次搜索", false)
                    SectionHeader("封面与标签")
                    SliderSetting("列表封面", 80f, 40f, 140f) {}
                    SliderSetting("网格封面", 129f, 80f, 200f) {}
                    SettingChoice("列数", "3")
                    CheckLine("显示封面", true)
                    CheckLine("扁平封面", true)
                    CheckLine("边框和阴影", true)
                    SectionHeader("标签栏项目")
                    listOf("书库", "文件夹", "最近", "收藏", "书签").forEach { CheckLine(it, true) }
                    listOf("网络", "偏好").forEach { CheckLine(it, false) }
                    SectionHeader("备份与迁移")
                    SettingChoice("导出设置与书签", "创建备份")
                    SettingChoice("导入备份", "选择文件")
                    SettingChoice("迁移书库路径", "重新关联")
                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, color = LibreraTeal, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.fillMaxWidth().background(Color(0xFFF0F0F0)).padding(horizontal = 18.dp, vertical = 9.dp))
}
