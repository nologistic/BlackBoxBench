package com.blackboxbench.reproduction

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private val Bg = Color(0xFFF9F9FF)
private val MenuBg = Color(0xFFF0F2FF)
private val SearchBg = Color(0xFFE5ECFA)
private val Navy = Color(0xFF0D2B5B)
private val PrimaryBlue = Color(0xFF415F91)
private val Purple = Color(0xFF6D5296)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Bg.toArgbCompat()
        window.navigationBarColor = Bg.toArgbCompat()
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        setContent { GalleryTheme { GalleryApp() } }
    }
}

private fun Color.toArgbCompat(): Int =
    android.graphics.Color.argb((alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())

@Composable
private fun GalleryTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = PrimaryBlue, onPrimary = Color.White, background = Bg,
            onBackground = Color(0xFF1A1B20), surface = Bg, onSurface = Color(0xFF1A1B20),
            surfaceVariant = SearchBg, onSurfaceVariant = Navy,
            secondaryContainer = Color(0xFFD9E2F8), onSecondaryContainer = Navy
        ),
        typography = Typography(
            bodyLarge = androidx.compose.ui.text.TextStyle(fontSize = 17.sp),
            titleLarge = androidx.compose.ui.text.TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Normal)
        ),
        content = content
    )
}

private enum class Screen { HOME, SEARCH, FOLDER, FAVORITES, IMAGE, VIDEO, SETTINGS, ABOUT, TRASH }

@Composable
private fun GalleryApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("gallery_state", 0) }
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    var previousScreen by rememberSaveable { mutableStateOf(Screen.FOLDER) }
    var favorite by remember { mutableStateOf(prefs.getBoolean("favorite", false)) }
    var trashed by remember { mutableStateOf(prefs.getBoolean("trashed", false)) }
    var hidden by remember { mutableStateOf(prefs.getBoolean("hidden", false)) }
    var listMode by remember { mutableStateOf(prefs.getBoolean("listMode", false)) }

    fun save() {
        prefs.edit().putBoolean("favorite", favorite).putBoolean("trashed", trashed)
            .putBoolean("hidden", hidden).putBoolean("listMode", listMode).apply()
    }

    val viewer = screen == Screen.IMAGE || screen == Screen.VIDEO
    val activity = context as? Activity
    SideEffect {
        activity?.window?.let { w ->
            w.statusBarColor = (if (viewer) Color.Black else Bg).toArgbCompat()
            w.navigationBarColor = (if (viewer) Color.Black else Bg).toArgbCompat()
            @Suppress("DEPRECATION")
            w.decorView.systemUiVisibility = if (viewer) 0 else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }

    Surface(Modifier.fillMaxSize(), color = if (viewer) Color.Black else Bg) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Bottom))) {
        when (screen) {
            Screen.HOME -> HomeScreen(
                favorite = favorite && !trashed && !hidden,
                count = 2 - (if (trashed) 1 else 0) - (if (hidden) 1 else 0),
                listMode = listMode,
                onListMode = { listMode = it; save() },
                onSearch = { screen = Screen.SEARCH },
                onFolder = { screen = Screen.FOLDER },
                onFavorites = { screen = Screen.FAVORITES },
                onSettings = { screen = Screen.SETTINGS },
                onAbout = { screen = Screen.ABOUT },
                onTrash = { screen = Screen.TRASH }
            )
            Screen.SEARCH -> SearchScreen(
                imageVisible = !trashed && !hidden,
                onBack = { screen = Screen.HOME },
                onImage = { previousScreen = Screen.SEARCH; screen = Screen.IMAGE },
                onVideo = { previousScreen = Screen.SEARCH; screen = Screen.VIDEO }
            )
            Screen.FOLDER -> FolderScreen(
                imageVisible = !trashed && !hidden, favorite = favorite,
                onBack = { screen = Screen.HOME },
                onImage = { previousScreen = Screen.FOLDER; screen = Screen.IMAGE },
                onVideo = { previousScreen = Screen.FOLDER; screen = Screen.VIDEO },
                onTrash = { screen = Screen.TRASH }, onSettings = { screen = Screen.SETTINGS }
            )
            Screen.FAVORITES -> FavoritesScreen(
                showImage = favorite && !trashed && !hidden,
                onBack = { screen = Screen.HOME },
                onImage = { previousScreen = Screen.FAVORITES; screen = Screen.IMAGE }
            )
            Screen.IMAGE -> ImageViewer(
                favorite = favorite, hidden = hidden,
                onBack = { save(); screen = previousScreen },
                onFavorite = { favorite = !favorite; save() },
                onHidden = { hidden = !hidden; save() },
                onDelete = { trashed = true; save(); screen = Screen.FOLDER },
                onSettings = { screen = Screen.SETTINGS }
            )
            Screen.VIDEO -> VideoViewer(onBack = { screen = previousScreen }, onSettings = { screen = Screen.SETTINGS })
            Screen.SETTINGS -> SettingsScreen(onBack = { screen = Screen.HOME })
            Screen.ABOUT -> AboutScreen(onBack = { screen = Screen.HOME })
            Screen.TRASH -> TrashScreen(trashed, favorite, { screen = Screen.FOLDER }) { trashed = false; save() }
        }
        }
    }
}

@Composable
private fun IconText(text: String, onClick: () -> Unit, tint: Color = Navy, size: Int = 28) {
    Box(Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text, color = tint, fontSize = size.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SimpleTopBar(title: String, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().height(68.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) IconText("‹", onBack, size = 42)
        Text(title, Modifier.weight(1f).padding(start = if (onBack == null) 16.dp else 4.dp),
            fontSize = 22.sp, color = Navy, maxLines = 1, overflow = TextOverflow.Ellipsis)
        actions()
    }
}

@Composable
private fun SearchBar(onSearch: () -> Unit, onCamera: () -> Unit, onAllMedia: () -> Unit, onMore: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(12.dp).height(64.dp).clip(RoundedCornerShape(34.dp))
        .background(SearchBg).clickable(onClick = onSearch).padding(start = 18.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text("⌕", fontSize = 31.sp, color = Navy)
        Text("搜索文件夹", Modifier.weight(1f).padding(start = 12.dp), color = Navy, fontSize = 18.sp)
        IconText("▣", onCamera, size = 22); IconText("▧", onAllMedia, size = 24); IconText("⋮", onMore, size = 29)
    }
}

@Composable
private fun AppMenu(expanded: Boolean, onDismiss: () -> Unit, entries: List<String>, onEntry: (String) -> Unit) {
    if (!expanded) return
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            Modifier.fillMaxSize().padding(top = 72.dp, end = 12.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Surface(
                modifier = Modifier.width(310.dp).heightIn(max = 690.dp),
                shape = RoundedCornerShape(18.dp),
                color = MenuBg,
                tonalElevation = 4.dp,
                shadowElevation = 6.dp
            ) {
                LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(entries) { name ->
                        Row(
                            Modifier.fillMaxWidth().height(50.dp).clickable {
                                onDismiss()
                                onEntry(name)
                            }.padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(name, color = Navy, fontSize = 17.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    favorite: Boolean, count: Int, listMode: Boolean, onListMode: (Boolean) -> Unit,
    onSearch: () -> Unit, onFolder: () -> Unit, onFavorites: () -> Unit,
    onSettings: () -> Unit, onAbout: () -> Unit, onTrash: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize()) {
        Box {
            SearchBar(onSearch, { dialog = "相机" }, onSearch, { menu = true })
            Box(Modifier.align(Alignment.TopEnd).padding(top = 58.dp, end = 8.dp)) {
                AppMenu(menu, { menu = false }, listOf(
                    "排序方式", "过滤显示的文件", "更改视图类型", "临时显示隐藏项目",
                    "临时显示已排除项目", "新建文件夹", "列数", "设置", "关于"
                )) {
                    when (it) {
                        "排序方式" -> dialog = "排序"; "过滤显示的文件" -> dialog = "过滤"
                        "更改视图类型" -> dialog = "视图"; "新建文件夹" -> dialog = "新建"
                        "设置" -> onSettings(); "关于" -> onAbout()
                    }
                }
            }
        }
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)) {
            if (favorite) item { if (listMode) AlbumListRow("收藏", "favorites/", 1, true, onFavorites) else AlbumGridCard("收藏", 1, true, onFavorites) }
            item { if (listMode) AlbumListRow("BlackBoxBench", "/storage/emulated/0/Download/", count, false, onFolder) else AlbumGridCard("BlackBoxBench", count, false, onFolder) }
            item { TextButton(onClick = onTrash, modifier = Modifier.fillMaxWidth()) { Text("回收站", color = Navy) } }
        }
    }
    when (dialog) {
        "排序" -> SortDialog { dialog = null }; "过滤" -> FilterDialog { dialog = null }
        "视图" -> ViewDialog(listMode, { onListMode(it) }) { dialog = null }
        "新建" -> TextEntryDialog("新建文件夹", "文件夹名称", "") { dialog = null }
        "相机" -> InfoDialog("相机", "此复现环境中没有可用的系统相机。") { dialog = null }
    }
}

@Composable
private fun AlbumGridCard(name: String, count: Int, pinned: Boolean, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick).background(Color(0xFFE7EAF3)).padding(bottom = 12.dp)) {
        Box(Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))) {
            if (pinned) Image(painterResource(R.drawable.sample_photo), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Row(Modifier.fillMaxSize()) {
                VideoThumb(Modifier.weight(1f).fillMaxHeight(), true)
                Image(painterResource(R.drawable.sample_photo), null, Modifier.weight(1f).fillMaxHeight(), contentScale = ContentScale.Crop)
            }
            if (pinned) Text("★", Modifier.align(Alignment.TopEnd).padding(12.dp), color = Color.White, fontSize = 27.sp)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(name, fontSize = 20.sp, color = Navy); Text("$count", fontSize = 15.sp, color = Color(0xFF5C6070)) }
            if (pinned) Text("📌", fontSize = 20.sp)
        }
    }
}

@Composable
private fun AlbumListRow(name: String, path: String, count: Int, pinned: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(112.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).background(Color(0xFFE9ECF5)).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(96.dp).clip(RoundedCornerShape(12.dp))) {
            if (pinned) Image(painterResource(R.drawable.sample_photo), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else VideoThumb(Modifier.fillMaxSize(), false)
        }
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Text(name, color = Navy, fontSize = 19.sp); if (pinned) Text("  📌", fontSize = 15.sp) }
            Text("$count", color = Color.Gray, fontSize = 15.sp)
            Text(path, color = Color.Gray, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun VideoThumb(modifier: Modifier = Modifier, showDuration: Boolean = true) {
    Box(modifier.background(Purple), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(58.dp)) {
            drawCircle(Color(0x44FFFFFF))
            val p = Path().apply { moveTo(size.width*.4f,size.height*.28f); lineTo(size.width*.75f,size.height*.5f); lineTo(size.width*.4f,size.height*.72f); close() }
            drawPath(p, Color.White)
        }
        Text("STORY", Modifier.align(Alignment.BottomStart).padding(10.dp), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        if (showDuration) Text("0:03", Modifier.align(Alignment.BottomEnd).padding(8.dp), color = Color.White, fontSize = 13.sp)
    }
}

@Composable
private fun SearchScreen(imageVisible: Boolean, onBack: () -> Unit, onImage: () -> Unit, onVideo: () -> Unit) {
    var query by remember { mutableStateOf("") }; var global by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(12.dp).height(58.dp).clip(RoundedCornerShape(32.dp)).background(SearchBg), verticalAlignment = Alignment.CenterVertically) {
            IconText("‹", onBack, size = 42)
            TextField(query, { query = it }, Modifier.weight(1f), placeholder = { Text(if (global) "搜索所有可见文件" else "搜索文件夹") }, singleLine = true,
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent))
            if (query.isNotEmpty()) IconText("×", { query = "" }, size = 28)
        }
        if (!global && query.isNotBlank()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("未找到任何项目。", color = Color.Gray, fontSize = 17.sp)
                TextButton({ global = true }) { Text("切换文件搜索范围到所有可见的文件夹", textAlign = TextAlign.Center) }
            }
        } else if (global) {
            Text("今天", Modifier.padding(horizontal = 16.dp, vertical = 12.dp), color = Navy, fontSize = 19.sp)
            MediaGrid(imageVisible, false, onImage, onVideo, {}, {})
        } else EmptyMessage("输入名称以搜索文件夹")
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderScreen(
    imageVisible: Boolean, favorite: Boolean, onBack: () -> Unit, onImage: () -> Unit,
    onVideo: () -> Unit, onTrash: () -> Unit, onSettings: () -> Unit
) {
    var more by remember { mutableStateOf(false) }; var sort by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<String?>(null) }; var dialog by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize()) {
        if (selected != null) Row(Modifier.fillMaxWidth().height(68.dp).background(Color(0xFFDCE6FA)).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconText("×", { selected = null })
            Text("1 / \${if (imageVisible) 2 else 1}", Modifier.weight(1f), color = Navy, fontSize = 20.sp)
            IconText("⌫", { if (selected == "image") dialog = "删除" }); IconText("↗", { dialog = "分享" })
            Box {
                IconText("⋮", { more = true })
                Box(Modifier.align(Alignment.TopEnd).padding(top = 42.dp)) {
                    AppMenu(more, { more = false }, listOf("旋转","属性","重命名","隐藏","复制到","移动到","创建快捷方式","打开方式","设置为","调整大小","编辑",if(favorite)"取消收藏" else "收藏","修复拍摄日期","全选")) {
                        dialog = when(it) { "重命名"->"重命名"; "调整大小"->"调整"; "属性"->"属性"; else->dialog }
                    }
                }
            }
        } else Row(Modifier.fillMaxWidth().padding(12.dp).height(58.dp).clip(RoundedCornerShape(32.dp)).background(SearchBg), verticalAlignment = Alignment.CenterVertically) {
            IconText("‹", onBack, size = 42); Text("在 BlackBoxBench 中搜索", Modifier.weight(1f), color = Navy, fontSize = 18.sp, maxLines = 1)
            IconText("▦", { dialog = "视图" }, size = 23); IconText("⇅", { sort = true }, size = 24)
            Box {
                IconText("⋮", { more = true })
                Box(Modifier.align(Alignment.TopEnd).padding(top = 44.dp)) {
                    AppMenu(more, { more = false }, listOf("过滤显示的文件","更改视图类型","临时显示隐藏项目","打开回收站","分组方式","设置为默认文件夹","新建文件夹","列数","幻灯片","设置")) {
                        when(it) { "过滤显示的文件"->dialog="过滤";"更改视图类型"->dialog="视图";"打开回收站"->onTrash();"分组方式"->dialog="分组";"新建文件夹"->dialog="新建";"幻灯片"->dialog="幻灯片";"设置"->onSettings() }
                    }
                }
            }
        }
        Text("今天", Modifier.padding(start = 16.dp, top = 8.dp, bottom = 12.dp), color = Navy, fontSize = 19.sp)
        MediaGrid(imageVisible, favorite, onImage, onVideo, { selected = "image" }, { selected = "video" })
    }
    if (sort) SortDialog { sort = false }
    when(dialog) {
        "过滤"->FilterDialog{dialog=null};"视图"->ViewDialog(false,{}){dialog=null};"分组"->GroupDialog{dialog=null}
        "幻灯片"->SlideshowDialog{dialog=null};"新建"->TextEntryDialog("新建文件夹","文件夹名称",""){dialog=null}
        "重命名"->TextEntryDialog("重命名","文件名","sample_photo"){dialog=null};"调整"->ResizeDialog{dialog=null}
        "属性"->InfoDialog("属性","sample_photo.png\n900 × 900\n10.7 kB\n内部存储空间/Download/BlackBoxBench/"){dialog=null}
        "分享"->InfoDialog("分享","可通过系统分享功能发送所选媒体。"){dialog=null}
        "删除"->InfoDialog("删除","请在图片查看器中删除以移动到回收站。"){dialog=null}
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaGrid(imageVisible:Boolean,favorite:Boolean,onImage:()->Unit,onVideo:()->Unit,onLongImage:()->Unit,onLongVideo:()->Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal=8.dp), horizontalArrangement=Arrangement.spacedBy(4.dp)) {
        Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(3.dp)).combinedClickable(onClick=onVideo,onLongClick=onLongVideo)) { VideoThumb(Modifier.fillMaxSize(),true) }
        if(imageVisible) Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(3.dp)).combinedClickable(onClick=onImage,onLongClick=onLongImage)) {
            Image(painterResource(R.drawable.sample_photo),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
            if(favorite) Text("★",Modifier.align(Alignment.TopEnd).padding(8.dp),color=Color.White,fontSize=26.sp)
        } else Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun FavoritesScreen(showImage:Boolean,onBack:()->Unit,onImage:()->Unit) {
    Column(Modifier.fillMaxSize()) {
        SimpleTopBar("收藏",onBack){IconText("⇅",{});IconText("⋮",{})}
        if(showImage) Row(Modifier.fillMaxWidth().padding(8.dp)) {
            Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(3.dp)).clickable(onClick=onImage)) {
                Image(painterResource(R.drawable.sample_photo),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                Text("★",Modifier.align(Alignment.TopEnd).padding(8.dp),color=Color.White,fontSize=27.sp)
            }; Spacer(Modifier.weight(1f))
        } else EmptyMessage("没有收藏的媒体。")
    }
}

@Composable
private fun ImageViewer(favorite:Boolean,hidden:Boolean,onBack:()->Unit,onFavorite:()->Unit,onHidden:()->Unit,onDelete:()->Unit,onSettings:()->Unit) {
    var menu by remember{mutableStateOf(false)};var rotate by remember{mutableStateOf(false)};var dialog by remember{mutableStateOf<String?>(null)}
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(Modifier.fillMaxWidth().height(72.dp).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically) {
            IconText("‹",onBack,Color.White,42)
            Text((if(hidden)"." else "")+"sample_photo.png",Modifier.weight(1f),color=Color.White,fontSize=21.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
            Box { IconText("⟳",{rotate=true},Color.White,29); Box(Modifier.align(Alignment.TopEnd).padding(top=42.dp)){AppMenu(rotate,{rotate=false},listOf("向右旋转","向左旋转","旋转180°")){dialog="另存为"}} }
            IconText("ⓘ",{dialog="信息"},Color.White,27)
            Box {
                IconText("⋮",{menu=true},Color.White,29)
                Box(Modifier.align(Alignment.TopEnd).padding(top=44.dp)) {
                    AppMenu(menu,{menu=false},listOf("重命名",if(hidden)"取消隐藏" else "隐藏","复制到剪贴板","复制到","移动到","创建快捷方式","打开方式","设置为","更改画面方向","打印","调整大小","在地图上显示","幻灯片","设置")) {
                        when(it){"隐藏","取消隐藏"->onHidden();"重命名"->dialog="重命名";"调整大小"->dialog="调整";"设置"->onSettings();"幻灯片"->dialog="幻灯片"}
                    }
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center){Image(painterResource(R.drawable.sample_photo),null,Modifier.fillMaxWidth(),contentScale=ContentScale.FillWidth)}
        Row(Modifier.fillMaxWidth().height(88.dp).padding(horizontal=42.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
            IconText(if(favorite)"★" else "☆",onFavorite,Color.White,32);IconText("✎",{dialog="编辑"},Color.White,29)
            IconText("↗",{dialog="分享"},Color.White,31);IconText("⌫",{dialog="删除确认"},Color.White,29)
        }
    }
    when(dialog) {
        "另存为"->SaveAsDialog{dialog=null};"信息"->InfoDialog("文件信息","sample_photo.png\n900 × 900\nPNG · 10.7 kB"){dialog=null}
        "重命名"->TextEntryDialog("重命名","文件名","sample_photo"){dialog=null};"调整"->ResizeDialog{dialog=null}
        "幻灯片"->SlideshowDialog{dialog=null};"编辑"->EditorDialog{dialog=null}
        "分享"->InfoDialog("分享","可通过系统分享功能发送这张图片。"){dialog=null}
        "删除确认"->DeleteDialog(onDelete,{dialog=null})
    }
}

@Composable
private fun VideoViewer(onBack:()->Unit,onSettings:()->Unit) {
    var playing by remember{mutableStateOf(false)};var pos by remember{mutableIntStateOf(0)};var menu by remember{mutableStateOf(false)}
    LaunchedEffect(playing){if(playing){repeat(3){kotlinx.coroutines.delay(850);pos=it+1};playing=false}}
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(Modifier.fillMaxWidth().height(72.dp).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically) {
            IconText("‹",onBack,Color.White,42);Text("sample_video.mp4",Modifier.weight(1f),color=Color.White,fontSize=21.sp)
            IconText("ⓘ",{},Color.White,27);IconText("◉",{},Color.White,24)
            Box{IconText("⋮",{menu=true},Color.White,29);Box(Modifier.align(Alignment.TopEnd).padding(top=44.dp)){AppMenu(menu,{menu=false},listOf("重命名","隐藏","打开方式","设置为","属性","设置")){if(it=="设置")onSettings()}}}
        }
        Box(Modifier.weight(1f).fillMaxWidth().background(Purple),contentAlignment=Alignment.Center) {
            Canvas(Modifier.size(104.dp).clip(CircleShape).clickable{playing=!playing}) {
                drawCircle(Color(0x55000000))
                if(!playing){val p=Path().apply{moveTo(size.width*.42f,size.height*.3f);lineTo(size.width*.75f,size.height*.5f);lineTo(size.width*.42f,size.height*.7f);close()};drawPath(p,Color.White)}
                else{drawRect(Color.White,Offset(size.width*.38f,size.height*.32f),androidx.compose.ui.geometry.Size(size.width*.08f,size.height*.36f));drawRect(Color.White,Offset(size.width*.56f,size.height*.32f),androidx.compose.ui.geometry.Size(size.width*.08f,size.height*.36f))}
            }
            Text("STORY",Modifier.align(Alignment.BottomStart).padding(22.dp),color=Color.White,fontWeight=FontWeight.Bold,fontSize=28.sp)
        }
        Column(Modifier.fillMaxWidth().height(146.dp).background(Color.Black).padding(horizontal=16.dp)) {
            Slider(pos.toFloat(),{pos=it.toInt()},valueRange=0f..3f,colors=SliderDefaults.colors(thumbColor=Color.White,activeTrackColor=Color.White))
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text("1x",color=Color.White,modifier=Modifier.width(58.dp));IconText(if(playing)"Ⅱ" else "▶",{playing=!playing},Color.White,24)
                Text("🔇",color=Color.White,modifier=Modifier.width(52.dp),textAlign=TextAlign.Center);Spacer(Modifier.weight(1f));Text("0:0$pos / 0:03",color=Color.White)
            }
        }
        Row(Modifier.fillMaxWidth().height(82.dp).padding(horizontal=42.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
            IconText("☆",{},Color.White,31);IconText("✎",{},Color.White,29);IconText("↗",{},Color.White,31);IconText("⌫",{},Color.White,29)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrashScreen(trashed:Boolean,favorite:Boolean,onBack:()->Unit,onRestore:()->Unit) {
    var selected by remember{mutableStateOf(false)};var menu by remember{mutableStateOf(false)};var confirmDelete by remember{mutableStateOf(false)}
    Column(Modifier.fillMaxSize()) {
        if(selected) Row(Modifier.fillMaxWidth().height(68.dp).background(Color(0xFFDCE6FA)).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically) {
            IconText("×",{selected=false});Text("1 / 1",Modifier.weight(1f),color=Navy,fontSize=20.sp);IconText("⌫",{confirmDelete=true});IconText("↗",{})
            Box{IconText("⋮",{menu=true});Box(Modifier.align(Alignment.TopEnd).padding(top=44.dp)){AppMenu(menu,{menu=false},listOf("旋转","属性","复制到","创建快捷方式","打开方式","设置为","调整大小","编辑","恢复所选文件","全选")){if(it=="恢复所选文件"){onRestore();selected=false}}}}
        } else SimpleTopBar("回收站",onBack){IconText("⋮",{})}
        if(!trashed)EmptyMessage("回收站为空") else {
            Text("将在 30 天后自动删除",Modifier.padding(16.dp),color=Color.Gray)
            Row(Modifier.fillMaxWidth().padding(8.dp)) {
                Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(3.dp)).combinedClickable(onClick={},onLongClick={selected=true})) {
                    Image(painterResource(R.drawable.sample_photo),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                    if(favorite)Text("★",Modifier.align(Alignment.TopEnd).padding(8.dp),color=Color.White,fontSize=27.sp)
                    if(selected)Box(Modifier.align(Alignment.TopStart).padding(8.dp).size(28.dp).clip(CircleShape).background(PrimaryBlue),contentAlignment=Alignment.Center){Text("✓",color=Color.White)}
                };Spacer(Modifier.weight(1f))
            }
        }
    }
    if(confirmDelete)AlertDialog({confirmDelete=false},title={Text("永久删除")},text={Text("确定要永久删除所选文件吗？此操作无法撤销。")},
        confirmButton={TextButton({confirmDelete=false}){Text("删除")}},dismissButton={TextButton({confirmDelete=false}){Text("取消")}})
}

@Composable
private fun SettingsScreen(onBack:()->Unit) {
    var switches by remember{mutableStateOf(mapOf("显示隐藏项目" to false,"搜索所有文件" to false,"自动播放视频" to false,"循环播放视频" to false,
        "竖向手势调节音量和亮度" to true,"裁剪缩略图为正方形" to true,"下拉刷新" to true,"HDR / Ultra HDR" to true,
        "黑色背景" to true,"屏幕常亮" to true,"下滑退出" to true,"显示刘海区域" to true,"手势旋转图片" to true,
        "保留修改日期" to true,"显示底部按钮" to true,"删除的文件移到回收站" to true,"文件夹界面显示回收站" to true))}
    Column(Modifier.fillMaxSize()) {
        SimpleTopBar("设置",onBack)
        LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=24.dp)) {
            item{SettingSection("外观")};item{SettingLink("自定义外观","颜色、主题和字体")};item{SettingSection("常规")}
            item{SettingLink("语言","中文")};item{SettingLink("更改日期和时间格式","")};item{SettingLink("文件加载优先级","速度")}
            item{SettingLink("管理包含的文件夹","")};item{SettingLink("管理排除的文件夹","")}
            item{ToggleSetting("显示隐藏项目",switches){switches=switches+("显示隐藏项目" to it)}}
            item{ToggleSetting("搜索所有文件，而不仅是主文件夹",switches,"搜索所有文件"){switches=switches+("搜索所有文件" to it)}}
            item{SettingSection("视频")};item{ToggleSetting("自动播放视频",switches){switches=switches+("自动播放视频" to it)}}
            item{ToggleSetting("循环播放视频",switches){switches=switches+("循环播放视频" to it)}}
            item{ToggleSetting("独立播放时使用横向手势",switches){switches=switches+("独立播放时使用横向手势" to it)}}
            item{ToggleSetting("竖向手势调节音量和亮度",switches){switches=switches+("竖向手势调节音量和亮度" to it)}}
            item{SettingLink("点击视频时","使用默认播放器应用")};item{SettingSection("缩略图")}
            item{ToggleSetting("裁剪缩略图为正方形",switches){switches=switches+("裁剪缩略图为正方形" to it)}}
            item{ToggleSetting("动画 GIF 缩略图",switches){switches=switches+("动画 GIF 缩略图" to it)}}
            item{SettingLink("文件缩略图样式","")};item{SettingLink("文件夹缩略图样式","方形")}
            item{ToggleSetting("横向滚动缩略图",switches){switches=switches+("横向滚动缩略图" to it)}}
            item{ToggleSetting("下拉刷新",switches){switches=switches+("下拉刷新" to it)}}
            item{SettingSection("全屏查看器")};item{ToggleSetting("适应屏幕最大宽度",switches){switches=switches+("适应屏幕最大宽度" to it)}}
            item{ToggleSetting("HDR / Ultra HDR",switches){switches=switches+("HDR / Ultra HDR" to it)}}
            item{ToggleSetting("黑色背景",switches){switches=switches+("黑色背景" to it)}}
            item{ToggleSetting("自动隐藏系统界面",switches){switches=switches+("自动隐藏系统界面" to it)}}
            item{ToggleSetting("点击屏幕边缘切换文件",switches){switches=switches+("点击屏幕边缘切换文件" to it)}}
            item{ToggleSetting("屏幕常亮",switches){switches=switches+("屏幕常亮" to it)}}
            item{ToggleSetting("竖向手势调节图片亮度",switches){switches=switches+("竖向手势调节图片亮度" to it)}}
            item{ToggleSetting("下滑退出",switches){switches=switches+("下滑退出" to it)}}
            item{ToggleSetting("显示刘海区域",switches){switches=switches+("显示刘海区域" to it)}};item{SettingLink("屏幕旋转","跟随系统")}
            item{SettingSection("图片与手势")};item{ToggleSetting("手势旋转图片",switches){switches=switches+("手势旋转图片" to it)}}
            item{ToggleSetting("使用最高图片质量",switches){switches=switches+("使用最高图片质量" to it)}}
            item{ToggleSetting("双指双击显示 1:1",switches){switches=switches+("双指双击显示 1:1" to it)}}
            item{ToggleSetting("全屏显示更多详细信息",switches){switches=switches+("全屏显示更多详细信息" to it)}}
            item{SettingSection("安全")};item{ToggleSetting("使用密码保护整个应用",switches){switches=switches+("使用密码保护整个应用" to it)}}
            item{ToggleSetting("使用密码保护隐藏项目",switches){switches=switches+("使用密码保护隐藏项目" to it)}}
            item{ToggleSetting("使用密码保护删除或移动",switches){switches=switches+("使用密码保护删除或移动" to it)}}
            item{ToggleSetting("自动删除空文件夹",switches){switches=switches+("自动删除空文件夹" to it)}}
            item{ToggleSetting("保留修改日期",switches){switches=switches+("保留修改日期" to it)}}
            item{ToggleSetting("不显示删除确认",switches){switches=switches+("不显示删除确认" to it)}}
            item{ToggleSetting("显示底部按钮",switches){switches=switches+("显示底部按钮" to it)}};item{SettingLink("管理底部按钮","")}
            item{SettingSection("回收站")};item{ToggleSetting("删除的文件移到回收站",switches){switches=switches+("删除的文件移到回收站" to it)}}
            item{ToggleSetting("文件夹界面显示回收站",switches){switches=switches+("文件夹界面显示回收站" to it)}}
            item{ToggleSetting("主页末尾显示回收站",switches){switches=switches+("主页末尾显示回收站" to it)}}
            item{SettingLink("清空回收站","0 B")};item{SettingSection("维护与迁移")};item{SettingLink("清除缓存","605.4 kB")}
            item{SettingLink("导出收藏","")};item{SettingLink("导入收藏","")};item{SettingLink("导出设置","")};item{SettingLink("导入设置","")}
        }
    }
}

@Composable private fun SettingSection(text:String){Text(text,Modifier.fillMaxWidth().padding(start=20.dp,top=24.dp,bottom=8.dp),color=PrimaryBlue,fontSize=15.sp,fontWeight=FontWeight.Medium)}
@Composable private fun SettingLink(title:String,subtitle:String){Row(Modifier.fillMaxWidth().clickable{}.padding(horizontal=20.dp,vertical=13.dp),verticalAlignment=Alignment.CenterVertically){
    Column(Modifier.weight(1f)){Text(title,fontSize=17.sp,color=Color(0xFF222329));if(subtitle.isNotBlank())Text(subtitle,fontSize=14.sp,color=Color.Gray)};Text("›",fontSize=28.sp,color=Color.Gray)}}
@Composable private fun ToggleSetting(title:String,values:Map<String,Boolean>,key:String=title,onChange:(Boolean)->Unit){Row(Modifier.fillMaxWidth().clickable{onChange(!(values[key]?:false))}.padding(horizontal=20.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically){
    Text(title,Modifier.weight(1f).padding(end=10.dp),fontSize=17.sp,color=Color(0xFF222329));Switch(values[key]?:false,onChange)}}

@Composable
private fun AboutScreen(onBack:()->Unit){Column(Modifier.fillMaxSize()){SimpleTopBar("关于",onBack);LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=28.dp)){
    item{AboutHeader()};item{SettingSection("支持")};items(listOf("常见问题","已知问题","hello@fossify.org")){SettingLink(it,"")}
    item{SettingSection("参与")};items(listOf("分享给好友","贡献者","向 Fossify 捐赠")){SettingLink(it,"")}
    item{SettingSection("社区")};items(listOf("GitHub","Reddit","Telegram")){SettingLink(it,"")}
    item{SettingSection("法律")};items(listOf("隐私政策","第三方许可")){SettingLink(it,"")}
    item{Text("版本 1.13.1",Modifier.fillMaxWidth().padding(24.dp),textAlign=TextAlign.Center,color=Color.Gray)}
}}}
@Composable private fun AboutHeader(){Column(Modifier.fillMaxWidth().padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally){
    Box(Modifier.size(92.dp).clip(RoundedCornerShape(22.dp)).background(PrimaryBlue),contentAlignment=Alignment.Center){Text("▧",color=Color.White,fontSize=54.sp)}
    Text("Fossify Gallery",Modifier.padding(top=14.dp),fontSize=24.sp,color=Navy);Text("简洁、私密、开源的图库",color=Color.Gray,fontSize=15.sp)}}
@Composable private fun EmptyMessage(text:String){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text(text,color=Color.Gray,fontSize=18.sp,textAlign=TextAlign.Center)}}
@Composable private fun DialogOption(label:String,selected:Boolean,onClick:()->Unit){Row(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(vertical=5.dp),verticalAlignment=Alignment.CenterVertically){RadioButton(selected,onClick);Text(label,fontSize=16.sp)}}

@Composable
private fun SortDialog(onDismiss:()->Unit){var selected by remember{mutableStateOf("修改日期")};var desc by remember{mutableStateOf(true)}
    AlertDialog(onDismiss,title={Text("排序方式")},text={Column{listOf("名称","路径","大小","项目数量","修改日期","拍摄日期","随机","自定义").forEach{DialogOption(it,selected==it){selected=it}};HorizontalDivider();DialogOption("升序",!desc){desc=false};DialogOption("降序",desc){desc=true}}},
        confirmButton={TextButton(onDismiss){Text("确定")}},dismissButton={TextButton(onDismiss){Text("取消")}})}
@Composable
private fun FilterDialog(onDismiss:()->Unit){var checks by remember{mutableStateOf(mapOf("图片" to true,"视频" to true,"GIF" to true,"RAW 图像" to true,"SVG" to true,"竖向视频" to false))}
    AlertDialog(onDismiss,title={Text("过滤显示的文件")},text={Column{checks.forEach{(k,v)->Row(Modifier.fillMaxWidth().clickable{checks=checks+(k to !v)},verticalAlignment=Alignment.CenterVertically){Checkbox(v,{checks=checks+(k to it)});Text(k)}}}},
        confirmButton={TextButton(onDismiss){Text("确定")}},dismissButton={TextButton(onDismiss){Text("取消")}})}
@Composable
private fun ViewDialog(isList:Boolean,onMode:(Boolean)->Unit,onDismiss:()->Unit){var selected by remember{mutableStateOf(if(isList)"列表" else "网格")};var group by remember{mutableStateOf(false)}
    AlertDialog(onDismiss,title={Text("更改视图类型")},text={Column{DialogOption("网格",selected=="网格"){selected="网格"};DialogOption("列表",selected=="列表"){selected="列表"};Row(Modifier.fillMaxWidth().clickable{group=!group},verticalAlignment=Alignment.CenterVertically){Checkbox(group,{group=it});Text("按照实际文件夹分组")}}},
        confirmButton={TextButton({onMode(selected=="列表");onDismiss()}){Text("确定")}},dismissButton={TextButton(onDismiss){Text("取消")}})}
@Composable
private fun GroupDialog(onDismiss:()->Unit){var selected by remember{mutableStateOf("不分组")}
    AlertDialog(onDismiss,title={Text("分组方式")},text={Column{listOf("不分组","按修改日期（日）","按修改日期（月）","按拍摄日期（日）","按拍摄日期（月）","文件类型","扩展名").forEach{DialogOption(it,selected==it){selected=it}};Row(verticalAlignment=Alignment.CenterVertically){Checkbox(false,{});Text("在分区标题显示文件数量")}}},
        confirmButton={TextButton(onDismiss){Text("确定")}},dismissButton={TextButton(onDismiss){Text("取消")}})}
@Composable
private fun SlideshowDialog(onDismiss:()->Unit){AlertDialog(onDismiss,title={Text("幻灯片")},text={Column(verticalArrangement=Arrangement.spacedBy(9.dp)){
    OutlinedTextField("5",{},label={Text("间隔（秒）")},singleLine=true);Text("动画");DialogOption("滑动",true){}
    listOf("包含视频","包含 GIF","随机顺序","反向顺序","循环播放").forEach{Row(verticalAlignment=Alignment.CenterVertically){Checkbox(false,{});Text(it)}}}},
    confirmButton={TextButton(onDismiss){Text("开始")}},dismissButton={TextButton(onDismiss){Text("取消")}})}
@Composable
private fun TextEntryDialog(title:String,label:String,initial:String,onDismiss:()->Unit){var value by remember{mutableStateOf(initial)}
    AlertDialog(onDismiss,title={Text(title)},text={Column{OutlinedTextField(value,{value=it},label={Text(label)},singleLine=true);if(title=="重命名")Text(".png",color=Color.Gray,modifier=Modifier.padding(top=7.dp))}},
        confirmButton={TextButton(onDismiss){Text("确定")}},dismissButton={TextButton(onDismiss){Text("取消")}})}
@Composable
private fun ResizeDialog(onDismiss:()->Unit){AlertDialog(onDismiss,title={Text("调整图片大小")},text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField("900",{},Modifier.weight(1f),label={Text("宽度")});OutlinedTextField("900",{},Modifier.weight(1f),label={Text("高度")})}
    Text("🔗  保持宽高比",color=Navy);Text("内部存储空间/Download/BlackBoxBench/",color=Color.Gray);OutlinedTextField("sample_photo",{},label={Text("文件名")});Text("png",color=Color.Gray)}},
    confirmButton={TextButton(onDismiss){Text("确定")}},dismissButton={TextButton(onDismiss){Text("取消")}})}
@Composable
private fun SaveAsDialog(onDismiss:()->Unit){AlertDialog(onDismiss,title={Text("另存为")},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){Text("内部存储空间/Download/BlackBoxBench/",color=Color.Gray);OutlinedTextField("sample_photo",{},label={Text("文件名")});Text("png",color=Color.Gray)}},
    confirmButton={TextButton(onDismiss){Text("确定")}},dismissButton={TextButton(onDismiss){Text("取消")}})}
@Composable
private fun DeleteDialog(onDelete:()->Unit,onDismiss:()->Unit){var noAsk by remember{mutableStateOf(false)};var skip by remember{mutableStateOf(false)}
    AlertDialog(onDismiss,title={Text("移动到回收站")},text={Column{Text("确定要将 “sample_photo.png” (10.7 kB) 移至回收站吗？");Row(verticalAlignment=Alignment.CenterVertically){Checkbox(noAsk,{noAsk=it});Text("本次会话不再询问")};Row(verticalAlignment=Alignment.CenterVertically){Checkbox(skip,{skip=it});Text("跳过回收站，直接删除文件")}}},
        confirmButton={TextButton(onDelete){Text("是")}},dismissButton={TextButton(onDismiss){Text("否")}})}
@Composable
private fun EditorDialog(onDismiss:()->Unit){AlertDialog(onDismiss,title={Text("编辑图片")},text={Column(horizontalAlignment=Alignment.CenterHorizontally){
    Image(painterResource(R.drawable.sample_photo),null,Modifier.fillMaxWidth().aspectRatio(1f));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){Text("裁剪");Text("旋转 90°");Text("翻转")};Text("保存会生成新文件，原图保留。",Modifier.padding(top=16.dp),color=Color.Gray)}},
    confirmButton={TextButton(onDismiss){Text("另存为副本")}},dismissButton={TextButton(onDismiss){Text("取消")}})}
@Composable
private fun InfoDialog(title:String,message:String,onDismiss:()->Unit){AlertDialog(onDismiss,title={Text(title)},text={Text(message)},confirmButton={TextButton(onDismiss){Text("确定")}})}
