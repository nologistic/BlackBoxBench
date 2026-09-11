package com.blackboxbench.reproduction

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val AppBackground = Color(0xFFF9F7FF)
private val DrawerBackground = Color(0xFFF9F7FF)
private val FieldBackground = Color(0xFFEAE8EF)
private val CardBackground = Color(0xFFE7E5EC)
private val Ink = Color(0xFF1C1B20)
private val Muted = Color(0xFF77757E)
private val Outline = Color(0xFF777780)
private val Accent = Color(0xFF4F6595)
private val ErrorRed = Color(0xFFC52218)

private enum class Page { LIST, ADD, EDIT }

private data class FeedRecord(
    val url: String,
    val title: String,
    val tag: String,
    val fullArticle: Boolean,
    val summary: Boolean,
    val thumbnails: Boolean,
    val notifications: Boolean,
    val skipDuplicates: Boolean,
    val uniqueId: Boolean,
    val opening: Int
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(249, 247, 255)
        window.navigationBarColor = android.graphics.Color.rgb(249, 247, 255)
        setContent {
            MaterialTheme(
                colorScheme = MaterialTheme.colorScheme.copy(
                    primary = Accent,
                    surface = AppBackground,
                    background = AppBackground,
                    onSurface = Ink,
                    onBackground = Ink
                )
            ) {
                FeederApp(this)
            }
        }
    }
}

@Composable
private fun FeederApp(context: Context) {
    var savedFeed by remember { mutableStateOf(loadFeed(context)) }
    var page by remember { mutableStateOf(Page.LIST) }
    var selectedScope by remember {
        mutableStateOf(savedFeed?.title?.takeIf { it.isNotBlank() } ?: "所有订阅源")
    }
    var searchMode by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var addUrl by remember { mutableStateOf("") }
    var lookupAttempted by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(savedFeed ?: blankFeed("")) }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    fun showScope(name: String) {
        selectedScope = name
        searchMode = false
        query = ""
        scope.launch { drawerState.close() }
    }

    fun openAdd() {
        addUrl = ""
        lookupAttempted = false
        page = Page.ADD
    }

    BackHandler(enabled = searchMode || page != Page.LIST) {
        when {
            searchMode -> {
                searchMode = false
                query = ""
            }
            page == Page.EDIT -> page = Page.ADD
            page == Page.ADD -> page = Page.LIST
            else -> Unit
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        when (page) {
            Page.LIST -> {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = true,
                    drawerContent = {
                        DrawerPanel(
                            savedFeed = savedFeed,
                            onClose = { scope.launch { drawerState.close() } },
                            onSelect = ::showScope
                        )
                    }
                ) {
                    ArticleListScreen(
                        title = selectedScope,
                        searchMode = searchMode,
                        query = query,
                        onQueryChange = { query = it },
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onSearch = { searchMode = true },
                        onExitSearch = {
                            searchMode = false
                            query = ""
                        },
                        onOpenAnother = { scope.launch { drawerState.open() } },
                        onAdd = ::openAdd
                    )
                }
            }

            Page.ADD -> AddFeedScreen(
                url = addUrl,
                lookupAttempted = lookupAttempted,
                onUrlChange = {
                    addUrl = it
                    lookupAttempted = false
                },
                onBack = { page = Page.LIST },
                onSearch = {
                    if (addUrl.isNotBlank()) {
                        addUrl = normalizeUrl(addUrl)
                        lookupAttempted = true
                    }
                },
                onStillAdd = {
                    draft = blankFeed(normalizeUrl(addUrl))
                    page = Page.EDIT
                }
            )

            Page.EDIT -> FeedEditorScreen(
                initial = draft,
                onBack = { page = Page.ADD },
                onCancel = { page = Page.ADD },
                onConfirm = { record ->
                    val clean = record.copy(
                        url = normalizeUrl(record.url),
                        title = record.title.ifBlank { normalizeUrl(record.url) }
                    )
                    saveFeed(context, clean)
                    savedFeed = clean
                    selectedScope = clean.title
                    draft = clean
                    page = Page.LIST
                }
            )
        }
    }
}

@Composable
private fun ArticleListScreen(
    title: String,
    searchMode: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onOpenDrawer: () -> Unit,
    onSearch: () -> Unit,
    onExitSearch: () -> Unit,
    onOpenAnother: () -> Unit,
    onAdd: () -> Unit
) {
    val showFilter = title != "已保存文章"
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        if (searchMode) {
            SearchToolbar(query, onQueryChange, onExitSearch)
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlyphButton(Glyph.Menu, "打开导航", onOpenDrawer)
                Text(
                    text = title,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    color = Ink,
                    fontSize = 23.sp,
                    maxLines = 1
                )
                GlyphButton(Glyph.Search, "搜索", onSearch)
                if (showFilter) GlyphButton(Glyph.Filter, "筛选", {})
                GlyphButton(Glyph.More, "更多", {})
            }
        }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.padding(bottom = 64.dp, start = 20.dp, end = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "没有可阅读的内容。您是否想\n要…",
                    color = Ink,
                    fontSize = 24.sp,
                    lineHeight = 32.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(25.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinkText("打开", onOpenAnother)
                    Text(" 另一个订阅源？", color = Ink, fontSize = 22.sp)
                }
                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinkText("添加", onAdd)
                    Text(" 更多订阅源？", color = Ink, fontSize = 22.sp)
                }
            }
        }
    }
}

@Composable
private fun SearchToolbar(
    query: String,
    onQueryChange: (String) -> Unit,
    onExit: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(72.dp)
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .background(FieldBackground, RoundedCornerShape(28.dp))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlyphView(Glyph.Search, Modifier.size(42.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            textStyle = TextStyle(color = Ink, fontSize = 19.sp),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text("搜索", color = Muted, fontSize = 19.sp)
                }
                inner()
            }
        )
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable {
                    focus.clearFocus()
                    keyboard?.hide()
                    onExit()
                },
            contentAlignment = Alignment.Center
        ) {
            Text("×", color = Ink, fontSize = 30.sp, fontWeight = FontWeight.Light)
        }
    }
}

@Composable
private fun DrawerPanel(
    savedFeed: FeedRecord?,
    onClose: () -> Unit,
    onSelect: (String) -> Unit
) {
    var expanded by remember(savedFeed?.tag) { mutableStateOf(true) }
    ModalDrawerSheet(
        modifier = Modifier
            .width(350.dp)
            .fillMaxHeight(),
        drawerContainerColor = DrawerBackground,
        drawerShape = RoundedCornerShape(topEnd = 0.dp, bottomEnd = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlyphButton(Glyph.Menu, "关闭导航", onClose)
            }
            DrawerRow(
                text = "所有订阅源",
                icon = null,
                indent = 44.dp,
                onClick = { onSelect("所有订阅源") }
            )
            DrawerRow(
                text = "已保存文章",
                icon = Glyph.Star,
                indent = 0.dp,
                onClick = { onSelect("已保存文章") }
            )
            Divider(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                color = Color(0xFFCAC8D0)
            )
            if (savedFeed != null && savedFeed.tag.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable { expanded = !expanded },
                        contentAlignment = Alignment.Center
                    ) {
                        GlyphView(if (expanded) Glyph.ChevronUp else Glyph.ChevronDown, Modifier.size(30.dp))
                    }
                    Text(
                        savedFeed.tag,
                        color = Ink,
                        fontSize = 18.sp,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelect(savedFeed.tag) }
                            .padding(vertical = 12.dp)
                    )
                }
                if (expanded) {
                    DrawerRow(
                        text = savedFeed.title,
                        icon = null,
                        indent = 44.dp,
                        onClick = { onSelect(savedFeed.title) }
                    )
                }
            } else if (savedFeed != null) {
                DrawerRow(
                    text = savedFeed.title,
                    icon = null,
                    indent = 44.dp,
                    onClick = { onSelect(savedFeed.title) }
                )
            }
            DrawerRow(
                text = "Feeder News",
                icon = null,
                indent = 44.dp,
                onClick = { onSelect("Feeder News") }
            )
        }
    }
}

@Composable
private fun DrawerRow(
    text: String,
    icon: Glyph?,
    indent: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onClick)
            .padding(start = indent, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            GlyphView(icon, Modifier.size(42.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, color = Ink, fontSize = 18.sp, maxLines = 1)
    }
}

@Composable
private fun AddFeedScreen(
    url: String,
    lookupAttempted: Boolean,
    onUrlChange: (String) -> Unit,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onStillAdd: () -> Unit
) {
    val host = hostPart(url)
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        SimpleTopBar("添加订阅源", onBack)
        TextField(
            value = url,
            onValueChange = onUrlChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            label = { Text("订阅 URL", fontSize = 17.sp) },
            singleLine = true,
            textStyle = TextStyle(fontSize = 18.sp, color = Ink),
            colors = feederTextFieldColors()
        )
        OutlinedButton(
            enabled = url.isNotBlank(),
            onClick = {
                focus.clearFocus()
                keyboard?.hide()
                onSearch()
            },
            modifier = Modifier.align(Alignment.CenterHorizontally),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Ink,
                disabledContentColor = Color(0xFFB9B7C0)
            )
        ) {
            Text("搜索", fontSize = 17.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp))
        }
        if (lookupAttempted) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(CardBackground, RoundedCornerShape(16.dp))
                    .padding(10.dp)
            ) {
                Text("无法下载", color = ErrorRed, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))
                Text(normalizeUrl(url), color = Ink, fontSize = 16.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Unable to resolve host \"$host\": No address associated with hostname",
                    color = Ink,
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onStillAdd,
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink)
                ) {
                    Text("仍要添加", fontSize = 16.sp, modifier = Modifier.padding(horizontal = 10.dp))
                }
            }
        }
    }
}

@Composable
private fun FeedEditorScreen(
    initial: FeedRecord,
    onBack: () -> Unit,
    onCancel: () -> Unit,
    onConfirm: (FeedRecord) -> Unit
) {
    var record by remember(initial) { mutableStateOf(initial) }
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        SimpleTopBar("编辑订阅源", onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = 16.dp)
        ) {
            TextField(
                value = record.url,
                onValueChange = { record = record.copy(url = it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                label = { Text("URL") },
                singleLine = true,
                textStyle = TextStyle(fontSize = 18.sp, color = Ink),
                colors = feederTextFieldColors()
            )
            OutlinedTextField(
                value = record.title,
                onValueChange = { record = record.copy(title = it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                label = { Text("标题", fontSize = 16.sp) },
                singleLine = true,
                textStyle = TextStyle(fontSize = 18.sp, color = Ink),
                colors = feederOutlinedFieldColors()
            )
            OutlinedTextField(
                value = record.tag,
                onValueChange = { record = record.copy(tag = it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp),
                label = { Text("标签", fontSize = 17.sp) },
                singleLine = true,
                textStyle = TextStyle(fontSize = 18.sp, color = Ink),
                colors = feederOutlinedFieldColors()
            )
            Divider(color = Color(0xFFCAC8D0))
            SettingSwitch(
                title = "默认获取完整文章",
                checked = record.fullArticle,
                onChecked = { record = record.copy(fullArticle = it) }
            )
            SettingSwitch(
                title = "打开时生成摘要",
                checked = record.summary,
                onChecked = { record = record.copy(summary = it) }
            )
            SettingSwitch(
                title = "从文章元数据增强缩略图",
                description = "尝试改进此订阅源中缺失或质量较低的缩略图",
                checked = record.thumbnails,
                onChecked = { record = record.copy(thumbnails = it) }
            )
            SettingSwitch(
                title = "新内容通知",
                checked = record.notifications,
                onChecked = { record = record.copy(notifications = it) }
            )
            SettingSwitch(
                title = "跳过重复的文章",
                description = "忽略链接或标题和现存文章相同的新文章",
                checked = record.skipDuplicates,
                onChecked = { record = record.copy(skipDuplicates = it) }
            )
            SettingSwitch(
                title = "生成附加的唯一 ID",
                description = "只对坏 ID 源启用",
                checked = record.uniqueId,
                onChecked = { record = record.copy(uniqueId = it) }
            )
            Divider(color = Color(0xFFCAC8D0))
            Text(
                "内容默认打开方式",
                color = Accent,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 22.dp, bottom = 10.dp)
            )
            val choices = listOf("使用应用默认值", "阅读器", "自定义标签页", "默认浏览器")
            choices.forEachIndexed { index, label ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clickable { record = record.copy(opening = index) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, color = Ink, fontSize = 18.sp, modifier = Modifier.weight(1f))
                    RadioButton(
                        selected = record.opening == index,
                        onClick = { record = record.copy(opening = index) },
                        colors = RadioButtonDefaults.colors(selectedColor = Accent, unselectedColor = Color(0xFF4D4C54))
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 18.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    "取消",
                    color = Accent,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clickable(onClick = onCancel)
                        .padding(14.dp)
                )
                Text(
                    "确定",
                    color = Accent,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clickable { onConfirm(record) }
                        .padding(14.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    description: String? = null,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChecked(!checked) }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = Ink, fontSize = 18.sp, lineHeight = 23.sp)
            if (description != null) {
                Spacer(Modifier.height(4.dp))
                Text(description, color = Ink, fontSize = 15.sp, lineHeight = 20.sp)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Accent,
                uncheckedThumbColor = Color(0xFF86848E),
                uncheckedTrackColor = FieldBackground,
                uncheckedBorderColor = Color(0xFF86848E)
            )
        )
    }
}

@Composable
private fun SimpleTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlyphButton(Glyph.Back, "返回", onBack)
        Text(title, color = Ink, fontSize = 23.sp, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun LinkText(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        color = Accent,
        fontSize = 22.sp,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun feederTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = FieldBackground,
    unfocusedContainerColor = FieldBackground,
    focusedTextColor = Ink,
    unfocusedTextColor = Ink,
    focusedIndicatorColor = Color(0xFF4D4C54),
    unfocusedIndicatorColor = Color(0xFF4D4C54),
    focusedLabelColor = Muted,
    unfocusedLabelColor = Muted,
    cursorColor = Accent
)

@Composable
private fun feederOutlinedFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedTextColor = Ink,
    unfocusedTextColor = Ink,
    focusedIndicatorColor = Outline,
    unfocusedIndicatorColor = Outline,
    focusedLabelColor = Muted,
    unfocusedLabelColor = Muted,
    cursorColor = Accent
)

private enum class Glyph { Menu, Search, Filter, More, Back, Star, ChevronUp, ChevronDown }

@Composable
private fun GlyphButton(glyph: Glyph, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        GlyphView(glyph, Modifier.size(34.dp))
    }
}

@Composable
private fun GlyphView(glyph: Glyph, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val color = Ink
        when (glyph) {
            Glyph.Menu -> {
                val stroke = w * 0.085f
                listOf(0.28f, 0.5f, 0.72f).forEach { y ->
                    drawLine(color, Offset(w * 0.16f, h * y), Offset(w * 0.84f, h * y), stroke, StrokeCap.Square)
                }
            }
            Glyph.Search -> {
                val stroke = w * 0.075f
                drawCircle(color, w * 0.25f, Offset(w * 0.43f, h * 0.42f), style = Stroke(stroke))
                drawLine(color, Offset(w * 0.62f, h * 0.61f), Offset(w * 0.84f, h * 0.83f), stroke, StrokeCap.Square)
            }
            Glyph.Filter -> {
                val stroke = w * 0.075f
                drawLine(color, Offset(w * 0.15f, h * 0.29f), Offset(w * 0.85f, h * 0.29f), stroke)
                drawLine(color, Offset(w * 0.30f, h * 0.50f), Offset(w * 0.70f, h * 0.50f), stroke)
                drawLine(color, Offset(w * 0.43f, h * 0.71f), Offset(w * 0.57f, h * 0.71f), stroke)
            }
            Glyph.More -> {
                listOf(0.28f, 0.5f, 0.72f).forEach { y -> drawCircle(color, w * 0.075f, Offset(w * 0.5f, h * y)) }
            }
            Glyph.Back -> {
                val stroke = w * 0.075f
                drawLine(color, Offset(w * 0.78f, h * 0.5f), Offset(w * 0.23f, h * 0.5f), stroke, StrokeCap.Square)
                drawLine(color, Offset(w * 0.23f, h * 0.5f), Offset(w * 0.48f, h * 0.27f), stroke, StrokeCap.Square)
                drawLine(color, Offset(w * 0.23f, h * 0.5f), Offset(w * 0.48f, h * 0.73f), stroke, StrokeCap.Square)
            }
            Glyph.Star -> {
                val path = Path()
                val cx = w / 2f
                val cy = h / 2f
                for (i in 0 until 10) {
                    val radius = if (i % 2 == 0) w * 0.42f else w * 0.18f
                    val angle = -PI / 2 + i * PI / 5
                    val x = cx + (cos(angle) * radius).toFloat()
                    val y = cy + (sin(angle) * radius).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                drawPath(path, color)
            }
            Glyph.ChevronUp, Glyph.ChevronDown -> {
                val stroke = w * 0.11f
                if (glyph == Glyph.ChevronUp) {
                    drawLine(color, Offset(w * 0.22f, h * 0.62f), Offset(w * 0.5f, h * 0.35f), stroke, StrokeCap.Square)
                    drawLine(color, Offset(w * 0.5f, h * 0.35f), Offset(w * 0.78f, h * 0.62f), stroke, StrokeCap.Square)
                } else {
                    drawLine(color, Offset(w * 0.22f, h * 0.38f), Offset(w * 0.5f, h * 0.65f), stroke, StrokeCap.Square)
                    drawLine(color, Offset(w * 0.5f, h * 0.65f), Offset(w * 0.78f, h * 0.38f), stroke, StrokeCap.Square)
                }
            }
        }
    }
}

private fun blankFeed(url: String): FeedRecord {
    val normalized = normalizeUrl(url)
    return FeedRecord(
        url = normalized,
        title = normalized,
        tag = "",
        fullArticle = false,
        summary = false,
        thumbnails = false,
        notifications = false,
        skipDuplicates = false,
        uniqueId = false,
        opening = 0
    )
}

private fun normalizeUrl(value: String): String {
    val clean = value.trim()
    if (clean.isBlank()) return clean
    return if (clean.startsWith("http://") || clean.startsWith("https://")) clean else "http://$clean"
}

private fun hostPart(value: String): String {
    return normalizeUrl(value)
        .removePrefix("http://")
        .removePrefix("https://")
        .substringBefore("/")
        .ifBlank { value }
}

private fun saveFeed(context: Context, feed: FeedRecord) {
    context.getSharedPreferences("feeder_reproduction", Context.MODE_PRIVATE)
        .edit()
        .putString("feed_url", feed.url)
        .putString("feed_title", feed.title)
        .putString("feed_tag", feed.tag)
        .putBoolean("feed_full", feed.fullArticle)
        .putBoolean("feed_summary", feed.summary)
        .putBoolean("feed_thumbs", feed.thumbnails)
        .putBoolean("feed_notifications", feed.notifications)
        .putBoolean("feed_skip_duplicates", feed.skipDuplicates)
        .putBoolean("feed_unique_id", feed.uniqueId)
        .putInt("feed_opening", feed.opening)
        .apply()
}

private fun loadFeed(context: Context): FeedRecord? {
    val prefs = context.getSharedPreferences("feeder_reproduction", Context.MODE_PRIVATE)
    if (!prefs.contains("feed_title")) return null
    return FeedRecord(
        url = prefs.getString("feed_url", "") ?: "",
        title = prefs.getString("feed_title", "") ?: "",
        tag = prefs.getString("feed_tag", "") ?: "",
        fullArticle = prefs.getBoolean("feed_full", false),
        summary = prefs.getBoolean("feed_summary", false),
        thumbnails = prefs.getBoolean("feed_thumbs", false),
        notifications = prefs.getBoolean("feed_notifications", false),
        skipDuplicates = prefs.getBoolean("feed_skip_duplicates", false),
        uniqueId = prefs.getBoolean("feed_unique_id", false),
        opening = prefs.getInt("feed_opening", 0)
    )
}
