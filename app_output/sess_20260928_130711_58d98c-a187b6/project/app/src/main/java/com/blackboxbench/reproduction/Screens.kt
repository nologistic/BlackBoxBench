package com.blackboxbench.reproduction

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val SEL_ALL = "all"
private const val SEL_SAVED = "saved"

@Composable
fun FeederApp(state: FeederState) {
    var selection by remember { mutableStateOf(SEL_ALL) }
    var route by remember { mutableStateOf("main") }
    var param by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val toast: (String) -> Unit = { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    BackHandler(enabled = route != "main") {
        route = if (route == "text") "settings" else "main"
    }

    Box(Modifier.fillMaxSize()) {
        when (route) {
            "add" -> AddFeedScreen(
                state = state,
                onBack = { route = "main" },
                onAdded = { feed ->
                    if (feed != null) selection = feed.id
                    route = "main"
                },
            )
            "edit" -> EditFeedScreen(
                state = state,
                feedId = param,
                onBack = { route = "main" },
                onToast = toast,
            )
            "settings" -> SettingsScreen(
                state = state,
                onBack = { route = "main" },
                onTextSettings = { route = "text" },
            )
            "text" -> TextSettingsScreen(state, onBack = { route = "settings" })
            "reader" -> ReaderScreen(
                state = state,
                articleId = param,
                onBack = { route = "main" },
                onToast = toast,
            )
            else -> MainScreen(
                state = state,
                selection = selection,
                onSelect = { selection = it },
                onOpenArticle = { param = it; route = "reader" },
                onEditFeed = { param = it; route = "edit" },
                onAddFeed = { route = "add" },
                onSettings = { route = "settings" },
                onToast = toast,
            )
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }
}

// --------------------------------------------------------------------- assets

@Composable
fun AssetImage(name: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember(name) {
        if (name.isNullOrBlank()) {
            null
        } else {
            try {
                context.assets.open("images/$name").use { BitmapFactory.decodeStream(it) }
            } catch (_: Exception) {
                null
            }
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant))
    }
}

// ----------------------------------------------------------------- main list

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: FeederState,
    selection: String,
    onSelect: (String) -> Unit,
    onOpenArticle: (String) -> Unit,
    onEditFeed: (String) -> Unit,
    onAddFeed: () -> Unit,
    onSettings: () -> Unit,
    onToast: (String) -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var filterMenu by remember { mutableStateOf(false) }
    var overflowMenu by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }
    var importDialog by remember { mutableStateOf(false) }

    var filterUnread by remember { mutableStateOf(true) }
    var filterSaved by remember { mutableStateOf(false) }
    var filterRecent by remember { mutableStateOf(true) }
    var filterRead by remember { mutableStateOf(true) }

    val currentFeed = state.feeds.firstOrNull { it.id == selection }
    val title = when {
        selection == SEL_SAVED -> "已保存文章"
        currentFeed != null -> currentFeed.displayTitle
        else -> "所有订阅源"
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 28.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "所有订阅源",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onSelect(SEL_ALL)
                                scope.launch { drawerState.close() }
                            }
                            .padding(vertical = 8.dp),
                    )
                    IconButton(onClick = { scope.launch { drawerState.close() } }) {
                        Icon(Icons.Default.Menu, contentDescription = "close")
                    }
                }
                Spacer(Modifier.height(8.dp))
                NavigationDrawerItem(
                    label = { Text("★ 已保存文章") },
                    selected = selection == SEL_SAVED,
                    onClick = {
                        onSelect(SEL_SAVED)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                state.feeds.forEach { feed ->
                    NavigationDrawerItem(
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(feed.displayTitle, modifier = Modifier.weight(1f))
                                if (feed.unreadCount > 0) {
                                    Text(
                                        text = feed.unreadCount.toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        },
                        selected = selection == feed.id,
                        onClick = {
                            onSelect(feed.id)
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "menu")
                        }
                    },
                    title = {
                        if (searching) {
                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it },
                                placeholder = { Text("搜索") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            Text(title)
                        }
                    },
                    actions = {
                        if (searching) {
                            IconButton(onClick = { searching = false; query = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "clear")
                            }
                        } else {
                            IconButton(onClick = { searching = true }) {
                                Icon(Icons.Default.Search, contentDescription = "search")
                            }
                            if (selection != SEL_SAVED) {
                                Box {
                                    IconButton(onClick = { filterMenu = true }) {
                                        Icon(Icons.Default.List, contentDescription = "filter")
                                    }
                                    DropdownMenu(expanded = filterMenu, onDismissRequest = { filterMenu = false }) {
                                        FilterCheck("未读", filterUnread) { filterUnread = it }
                                        FilterCheck("已保存", filterSaved) { filterSaved = it }
                                        FilterCheck("最近读过", filterRecent) { filterRecent = it }
                                        FilterCheck("已读", filterRead) { filterRead = it }
                                    }
                                }
                            }
                            Box {
                                IconButton(onClick = { overflowMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "more")
                                }
                                OverflowMenu(
                                    state = state,
                                    selection = selection,
                                    expanded = overflowMenu,
                                    onDismiss = { overflowMenu = false },
                                    onMarkAllRead = {
                                        state.markAllRead()
                                        onToast("已将全部文章标记为已读")
                                    },
                                    onSync = { onToast("同步完成") },
                                    onAddFeed = onAddFeed,
                                    onEditFeed = { currentFeed?.let { onEditFeed(it.id) } ?: onToast("请先选择一个订阅源") },
                                    onDelete = { deleteDialog = true },
                                    onImport = { importDialog = true },
                                    onExport = {
                                        val name = "feeder-export-" + System.currentTimeMillis() + ".opml"
                                        val path = state.writeExport(name, state.exportOpml())
                                        onToast("已导出到 $path")
                                    },
                                    onImportSaved = { onToast("没有可导入的已保存文章文件") },
                                    onExportSaved = {
                                        val count = state.savedArticles().size
                                        onToast("已导出 $count 篇已保存文章")
                                    },
                                    onSettings = onSettings,
                                )
                            }
                        }
                    },
                )
            },
            floatingActionButton = {
                if (state.showFab && selection != SEL_SAVED && state.feeds.isNotEmpty()) {
                    FloatingActionButton(onClick = onAddFeed) {
                        Icon(Icons.Default.Add, contentDescription = "添加订阅源")
                    }
                }
            },
        ) { padding ->
            val base: List<Article> = when {
                selection == SEL_SAVED -> state.savedArticles()
                currentFeed != null -> currentFeed.articles
                else -> state.allArticles()
            }.toMutableList()

            var visible = base.filter { article ->
                val matchesQuery = query.isBlank() ||
                    article.title.contains(query, ignoreCase = true) ||
                    article.summary.contains(query, ignoreCase = true)
                if (!matchesQuery) return@filter false
                val savedMatch = article.saved && filterSaved
                val unreadMatch = !article.read && filterUnread
                val readMatch = article.read && (filterRead || filterRecent)
                savedMatch || unreadMatch || readMatch
            }
            visible = if (state.articleSort == "从旧到新") {
                visible.sortedBy { it.published }
            } else {
                visible.sortedByDescending { it.published }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                if (visible.isEmpty()) {
                    EmptyState(
                        modifier = Modifier.align(Alignment.Center),
                        onOpenAnother = { scope.launch { drawerState.open() } },
                        onAddMore = onAddFeed,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        items(visible, key = { it.id + it.feedId }) { article ->
                            ArticleRow(
                                state = state,
                                article = article,
                                onClick = {
                                    state.markRead(article)
                                    onOpenArticle(article.id)
                                },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    if (deleteDialog) {
        DeleteFeedsDialog(
            state = state,
            onDismiss = { deleteDialog = false },
            onConfirm = { ids ->
                state.removeFeeds(ids)
                if (selection in ids) onSelect(SEL_ALL)
                deleteDialog = false
                onToast("已删除所选订阅源")
            },
        )
    }

    if (importDialog) {
        ImportDialog(
            state = state,
            onDismiss = { importDialog = false },
            onImport = { name ->
                val text = if (name == "subscriptions.opml") {
                    state.readBundledOpml()
                } else {
                    state.readExport(name)
                }
                if (text == null) {
                    onToast("无法读取文件")
                } else {
                    val added = state.importOpmlText(text)
                    onToast("已导入 $added 个订阅源")
                }
                importDialog = false
            },
        )
    }
}

@Composable
private fun FilterCheck(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    DropdownMenuItem(
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, modifier = Modifier.weight(1f))
                if (checked) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        },
        onClick = { onCheckedChange(!checked) },
    )
}

@Composable
private fun OverflowMenu(
    state: FeederState,
    selection: String,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onMarkAllRead: () -> Unit,
    onSync: () -> Unit,
    onAddFeed: () -> Unit,
    onEditFeed: () -> Unit,
    onDelete: () -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
    onImportSaved: () -> Unit,
    onExportSaved: () -> Unit,
    onSettings: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        MenuRow(Icons.Default.Check, "全部标记为已读") { onDismiss(); onMarkAllRead() }
        MenuRow(Icons.Default.Refresh, "同步订阅源") { onDismiss(); onSync() }
        MenuRow(Icons.Default.Add, "添加订阅源") { onDismiss(); onAddFeed() }
        MenuRow(Icons.Default.Edit, "编辑订阅源") { onDismiss(); onEditFeed() }
        MenuRow(Icons.Default.Delete, "删除订阅源") { onDismiss(); onDelete() }
        HorizontalDivider()
        MenuRow(Icons.Default.Add, "从 OPML 导入订阅源") { onDismiss(); onImport() }
        MenuRow(Icons.Default.Share, "将订阅源导出为 OPML") { onDismiss(); onExport() }
        MenuRow(Icons.Default.Add, "导入已保存的文章") { onDismiss(); onImportSaved() }
        MenuRow(Icons.Default.Share, "导出已保存的文章") { onDismiss(); onExportSaved() }
        HorizontalDivider()
        MenuRow(Icons.Default.Settings, "设置") { onDismiss(); onSettings() }
    }
}

@Composable
private fun MenuRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        onClick = onClick,
    )
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier, onOpenAnother: () -> Unit, onAddMore: () -> Unit) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "没有可阅读的内容。您是否想要…",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "打开 另一个订阅源?",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable { onOpenAnother() }
                .padding(8.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "添加 更多订阅源?",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable { onAddMore() }
                .padding(8.dp),
        )
    }
}

@Composable
private fun ArticleRow(state: FeederState, article: Article, onClick: () -> Unit) {
    val feed = state.findFeed(article.feedId)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!article.read) {
                    Box(
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (article.read) FontWeight.Normal else FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = (feed?.displayTitle ?: "") + " · " + formatDate(article.published),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!state.titlesOnly) {
                val summary = FeedParser.htmlToText(article.summary)
                if (summary.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = state.maxLines,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (state.showThumbnails && article.image != null) {
            Spacer(Modifier.width(12.dp))
            AssetImage(
                name = article.image,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
        }
    }
}

private fun formatDate(millis: Long): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(millis))
}

// ------------------------------------------------------------------- add feed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFeedScreen(state: FeederState, onBack: () -> Unit, onAdded: (Feed?) -> Unit) {
    var url by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "back") }
                },
                title = { Text("添加订阅源") },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it; error = null },
                label = { Text("订阅 URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = {
                    val trimmed = url.trim()
                    if (trimmed.isBlank()) return@OutlinedButton
                    val feed = state.addFeedFromAsset(trimmed)
                    if (feed != null) {
                        onAdded(feed)
                    } else {
                        error = "unexpected end of stream on $trimmed/..."
                    }
                },
                enabled = url.isNotBlank(),
            ) {
                Text("搜索")
            }
            error?.let { message ->
                Spacer(Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = "无法下载",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(url, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(message, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = {
                            val title = url.trim().substringAfter("://").substringBefore("/").ifBlank { "新订阅源" }
                            val feed = state.addEmptyFeed(url.trim(), title)
                            onAdded(feed)
                        }) {
                            Text("仍要添加")
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ edit feed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditFeedScreen(
    state: FeederState,
    feedId: String,
    onBack: () -> Unit,
    onToast: (String) -> Unit,
) {
    val feed = state.findFeed(feedId)
    if (feed == null) {
        androidx.compose.runtime.LaunchedEffect(feedId) { onBack() }
        return
    }
    var title by remember(feedId) { mutableStateOf(feed.displayTitle) }
    var tag by remember(feedId) { mutableStateOf("") }
    var fetchFull by remember(feedId) { mutableStateOf(feed.fetchFullArticle) }
    var enhance by remember(feedId) { mutableStateOf(feed.enhanceThumbnails) }
    var notifications by remember(feedId) { mutableStateOf(feed.notifications) }
    var skipDuplicates by remember(feedId) { mutableStateOf(feed.skipDuplicates) }
    var generateId by remember(feedId) { mutableStateOf(feed.generateUniqueId) }
    var openMode by remember(feedId) { mutableStateOf(feed.openMode) }

    val openModes = listOf("使用应用默认值", "阅读器", "自定义标签页", "默认浏览器")

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "back") }
                },
                title = { Text("编辑订阅源") },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text("URL", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(feed.url, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("标题") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = tag,
                onValueChange = { tag = it },
                label = { Text("标签") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            SettingSwitch("默认获取完整文章", checked = fetchFull) { fetchFull = it }
            SettingSwitch("打开时生成摘要", checked = false) { }
            SettingSwitch("从文章元数据增强缩略图", checked = enhance) { enhance = it }
            SettingSwitch("新内容通知", checked = notifications) { notifications = it }
            SettingSwitch("跳过重复的文章", checked = skipDuplicates) { skipDuplicates = it }
            SettingSwitch("生成附加的唯一 ID", checked = generateId) { generateId = it }
            Spacer(Modifier.height(16.dp))
            Text("内容默认打开方式", style = MaterialTheme.typography.titleMedium)
            openModes.forEachIndexed { index, label ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { openMode = index }
                        .padding(vertical = 4.dp),
                ) {
                    RadioButton(selected = openMode == index, onClick = { openMode = index })
                    Text(label)
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onBack) { Text("取消") }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = {
                    feed.customTitle = title
                    feed.fetchFullArticle = fetchFull
                    feed.enhanceThumbnails = enhance
                    feed.notifications = notifications
                    feed.skipDuplicates = skipDuplicates
                    feed.generateUniqueId = generateId
                    feed.openMode = openMode
                    state.persist()
                    onToast("已保存订阅源")
                    onBack()
                }) {
                    Text("确定")
                }
            }
        }
    }
}

// --------------------------------------------------------------- delete dialog

@Composable
private fun DeleteFeedsDialog(state: FeederState, onDismiss: () -> Unit, onConfirm: (Set<String>) -> Unit) {
    val selected = remember { mutableStateListOf<String>() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除订阅源") },
        text = {
            Column {
                state.feeds.forEach { feed ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (selected.contains(feed.id)) selected.remove(feed.id) else selected.add(feed.id)
                            }
                            .padding(vertical = 4.dp),
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = selected.contains(feed.id),
                            onCheckedChange = {
                                if (it) selected.add(feed.id) else selected.remove(feed.id)
                            },
                        )
                        Text(feed.displayTitle)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected.toSet()) }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

// --------------------------------------------------------------- import dialog

@Composable
private fun ImportDialog(state: FeederState, onDismiss: () -> Unit, onImport: (String) -> Unit) {
    val files = listOf("subscriptions.opml") + state.listExportFiles()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("从 OPML 导入订阅源") },
        text = {
            Column {
                files.forEach { name ->
                    Text(
                        text = name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onImport(name) }
                            .padding(vertical = 12.dp),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
