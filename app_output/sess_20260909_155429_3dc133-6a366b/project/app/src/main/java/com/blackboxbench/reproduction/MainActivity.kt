package com.blackboxbench.reproduction

import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val model = remember { AppModel(this) }
            val context = LocalContext.current
            var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

            DisposableEffect(model.currentEpisodeId) {
                mediaPlayer?.release()
                mediaPlayer = model.currentEpisode?.let { episode ->
                    MediaPlayer.create(context, episode.rawAudio)?.apply { isLooping = true }
                }
                onDispose {
                    mediaPlayer?.release()
                    mediaPlayer = null
                }
            }
            LaunchedEffect(model.currentEpisodeId, model.isPlaying) {
                runCatching {
                    if (model.isPlaying) mediaPlayer?.start() else mediaPlayer?.pause()
                }
            }
            LaunchedEffect(model.currentEpisodeId, model.isPlaying, model.speed) {
                while (model.isPlaying) {
                    delay((1000 / model.speed).toLong())
                    model.tick()
                }
            }

            BenchmarkAppTheme(dark = model.theme == "dark") {
                Surface(Modifier.fillMaxSize()) { AntennaPodApp(model) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AntennaPodApp(model: AppModel) {
    var showMore by remember { mutableStateOf(false) }
    var showRss by remember { mutableStateOf(false) }
    var showFilter by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showLabels by remember { mutableStateOf(false) }
    var showNavEditor by remember { mutableStateOf(false) }
    var showFastForward by remember { mutableStateOf(false) }

    val mainPages = setOf(
        Page.HOME, Page.QUEUE, Page.INBOX, Page.SUBSCRIPTIONS, Page.EPISODES,
        Page.DOWNLOADS, Page.HISTORY, Page.FAVORITES, Page.STATISTICS, Page.ADD
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (model.page in mainPages) {
                Column {
                    if (model.currentEpisode != null) MiniPlayer(model)
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                        model.navigation.take(4).forEach { item ->
                            NavigationBarItem(
                                selected = model.page == item,
                                onClick = { model.page = item },
                                icon = { Text(pageIcon(item), fontSize = 23.sp) },
                                label = { Text(pageTitle(item), maxLines = 1) }
                            )
                        }
                        Box(Modifier.weight(1f).height(80.dp)) {
                            Column(
                                Modifier.fillMaxSize().clickable { showMore = true },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("⋮", fontSize = 29.sp, fontWeight = FontWeight.Bold)
                                Text("更多", fontSize = 12.sp)
                            }
                            DropdownMenu(
                                expanded = false,
                                onDismissRequest = { showMore = false },
                                modifier = Modifier.width(250.dp)
                            ) {
                                val menu = listOf(
                                    Page.EPISODES, Page.DOWNLOADS, Page.HISTORY,
                                    Page.FAVORITES, Page.STATISTICS, Page.ADD
                                )
                                menu.filterNot { it in model.navigation.take(4) }.forEach { page ->
                                    DropdownMenuItem(
                                        text = { Text(pageTitle(page), fontSize = 18.sp) },
                                        leadingIcon = { Text(pageIcon(page), fontSize = 21.sp) },
                                        onClick = { model.page = page; showMore = false }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("自定义导航", fontSize = 18.sp) },
                                    leadingIcon = { Text("✎", fontSize = 21.sp) },
                                    onClick = { showMore = false; showNavEditor = true }
                                )
                                DropdownMenuItem(
                                    text = { Text("设置", fontSize = 18.sp) },
                                    leadingIcon = { Text("⚙", fontSize = 21.sp) },
                                    onClick = { model.page = Page.SETTINGS; showMore = false }
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (model.page) {
                Page.HOME -> HomeScreen(model)
                Page.QUEUE -> EpisodeListScreen(
                    "队列", model.queue.mapNotNull(model::episode), "☷",
                    "队列中没有单集", "下载单集，或将单集加入队列。", model,
                    queueMode = true, onFilter = { showFilter = true }
                )
                Page.INBOX -> EpisodeListScreen(
                    "收件箱", if (model.subscribed) model.episodes.take(1) else emptyList(),
                    "▣", "收件箱为空", "新单集会出现在这里。", model
                )
                Page.SUBSCRIPTIONS -> SubscriptionScreen(
                    model,
                    onAdd = { model.page = Page.ADD },
                    onLabels = { showLabels = true }
                )
                Page.EPISODES -> {
                    val list = model.episodes.filter { model.subscribed }
                        .filter { !model.filterUnplayed || it.id !in model.history }
                        .let { if (model.sortNewest) it else it.reversed() }
                    EpisodeListScreen(
                        "单集", list, "◉", "没有单集",
                        "订阅播客后，单集会显示在这里。", model,
                        onFilter = { showFilter = true },
                        onSort = { model.sortNewest = !model.sortNewest }
                    )
                }
                Page.DOWNLOADS -> EpisodeListScreen(
                    "下载", model.episodes.filter { it.id in model.downloads },
                    "↓", "没有已下载的单集", "下载的单集可以离线播放。", model
                )
                Page.HISTORY -> EpisodeListScreen(
                    "播放记录", model.episodes.filter { it.id in model.history },
                    "↶", "没有播放记录", "开始收听后，单集会显示在这里。", model
                )
                Page.FAVORITES -> EpisodeListScreen(
                    "收藏", model.episodes.filter { it.id in model.favorites },
                    "★", "没有收藏的单集", "点击单集旁的星标即可收藏。", model
                )
                Page.STATISTICS -> StatisticsScreen(model)
                Page.ADD -> AddPodcastScreen(model, onRss = { showRss = true })
                Page.PODCAST -> PodcastScreen(
                    model,
                    onBack = { model.page = Page.SUBSCRIPTIONS },
                    onFilter = { showFilter = true },
                    onRename = { showRename = true }
                )
                Page.EPISODE -> EpisodeDetailScreen(model)
                Page.PLAYER -> PlayerScreen(model)
                Page.SETTINGS -> SettingsHome(model)
                Page.UI_SETTINGS -> UiSettings(model)
                Page.PLAYBACK_SETTINGS -> PlaybackSettings(model) { showFastForward = true }
                Page.DOWNLOAD_SETTINGS -> DownloadSettings(model)
                Page.AUTO_DOWNLOAD -> AutoDownloadSettings(model)
                Page.INFO -> PodcastInfoScreen(model)
            }
        }
    }

    if (showMore) {
        MorePopup(
            model = model,
            onDismiss = { showMore = false },
            onNavigationEditor = {
                showMore = false
                showNavEditor = true
            }
        )
    }
    if (showRss) RssDialog(model) { showRss = false }
    if (showFilter) FilterSheet(model) { showFilter = false }
    if (showRename) RenameDialog(model) { showRename = false }
    if (showLabels) LabelDialog(model) { showLabels = false }
    if (showNavEditor) NavigationEditor(model) { showNavEditor = false }
    if (showFastForward) FastForwardDialog(model) { showFastForward = false }
}

fun pageTitle(page: Page) = when (page) {
    Page.HOME -> "首页"
    Page.QUEUE -> "队列"
    Page.INBOX -> "收件箱"
    Page.SUBSCRIPTIONS -> "订阅"
    Page.EPISODES -> "单集"
    Page.DOWNLOADS -> "下载"
    Page.HISTORY -> "播放记录"
    Page.FAVORITES -> "收藏"
    Page.STATISTICS -> "统计"
    Page.ADD -> "添加播客"
    else -> ""
}

fun pageIcon(page: Page) = when (page) {
    Page.HOME -> "⌂"
    Page.QUEUE -> "☷"
    Page.INBOX -> "▣"
    Page.SUBSCRIPTIONS -> "▦"
    Page.EPISODES -> "◔"
    Page.DOWNLOADS -> "⇩"
    Page.HISTORY -> "↶"
    Page.FAVORITES -> "★"
    Page.STATISTICS -> "▥"
    Page.ADD -> "+"
    else -> "•"
}

@Composable
fun MainHeader(
    title: String,
    back: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        Modifier.fillMaxWidth().height(82.dp).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (back != null) {
            IconButton(onClick = back) {
                Text("‹", fontSize = 42.sp, fontWeight = FontWeight.Light)
            }
            Spacer(Modifier.width(2.dp))
        }
        Text(title, fontSize = 30.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        actions()
    }
}

@Composable
fun AntennaLogo(modifier: Modifier = Modifier, muted: Boolean = false) {
    val blue = if (muted) MaterialTheme.colorScheme.outline else Color(0xFF078EDA)
    Canvas(modifier) {
        drawCircle(blue)
        val white = Color.White
        val c = center
        drawCircle(white, size.minDimension * .075f, c)
        drawLine(
            white,
            androidx.compose.ui.geometry.Offset(c.x, c.y + size.minDimension * .02f),
            androidx.compose.ui.geometry.Offset(c.x - size.minDimension * .14f, c.y + size.minDimension * .36f),
            size.minDimension * .065f,
            cap = StrokeCap.Round
        )
        drawLine(
            white,
            androidx.compose.ui.geometry.Offset(c.x, c.y + size.minDimension * .02f),
            androidx.compose.ui.geometry.Offset(c.x + size.minDimension * .14f, c.y + size.minDimension * .36f),
            size.minDimension * .065f,
            cap = StrokeCap.Round
        )
        listOf(.18f, .30f, .42f).forEach { radius ->
            drawArc(
                white, 205f, 130f, false,
                topLeft = androidx.compose.ui.geometry.Offset(
                    c.x - size.minDimension * radius,
                    c.y - size.minDimension * radius
                ),
                size = androidx.compose.ui.geometry.Size(
                    size.minDimension * radius * 2,
                    size.minDimension * radius * 2
                ),
                style = Stroke(size.minDimension * .038f, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun HomeScreen(model: AppModel) {
    Column(Modifier.fillMaxSize()) {
        MainHeader("首页", actions = {
            IconButton(onClick = {}) { Text("⌕", fontSize = 34.sp) }
            IconButton(onClick = {}) { Text("⋮", fontSize = 30.sp) }
        })
        if (!model.subscribed) {
            Column(
                Modifier.fillMaxSize().padding(bottom = 80.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AntennaLogo(Modifier.size(92.dp))
                Spacer(Modifier.height(24.dp))
                Text("欢迎使用 AntennaPod!", fontSize = 25.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    "您尚未订阅任何播客。请打开菜单添加播客。",
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("继续收听", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        TextButton(onClick = { model.page = Page.QUEUE }) { Text("队列 ›") }
                    }
                    val current = model.currentEpisode
                    if (current != null) {
                        ElevatedCard(
                            Modifier.fillMaxWidth().clickable { model.page = Page.PLAYER }
                        ) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                AntennaLogo(Modifier.size(64.dp))
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(current.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(model.podcastName, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                FilledIconButton(onClick = { model.togglePlay() }) {
                                    Text(if (model.isPlaying) "Ⅱ" else "▶")
                                }
                            }
                        }
                    } else {
                        Text("选择一个单集开始收听", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item { Text("最新单集", fontSize = 20.sp, fontWeight = FontWeight.SemiBold) }
                items(model.episodes.take(3), key = { it.id }) { EpisodeRow(it, model) }
            }
        }
    }
}

@Composable
fun SubscriptionScreen(model: AppModel, onAdd: () -> Unit, onLabels: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            MainHeader("订阅", actions = {
                IconButton(onClick = {}) { Text("⌕", fontSize = 34.sp) }
                IconButton(onClick = onLabels) { Text("⋮", fontSize = 30.sp) }
            })
            if (!model.subscribed) {
                EmptyState("▦", "您还没有订阅", "点击 + 添加第一个播客。")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = true,
                                    onClick = {},
                                    label = { Text("全部") }
                                )
                            }
                            if (model.label.isNotBlank()) {
                                item {
                                    FilterChip(
                                        selected = false,
                                        onClick = {},
                                        label = { Text(model.label) }
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Card(
                            Modifier.fillMaxWidth().height(155.dp)
                                .clickable { model.page = Page.PODCAST },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                Modifier.fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF70B389), Color(0xFF185D43))
                                        )
                                    )
                                    .padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AntennaLogo(Modifier.size(98.dp))
                                Spacer(Modifier.width(18.dp))
                                Column {
                                    Text(
                                        model.podcastName,
                                        fontSize = 23.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text("黑盒媒体 · 6 个单集", color = Color.White.copy(alpha = .85f))
                                    if (model.label.isNotBlank()) {
                                        Text(model.label, color = Color.White.copy(alpha = .85f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = onAdd,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
        ) { Text("+", fontSize = 30.sp) }
    }
}

@Composable
fun EmptyState(icon: String, title: String, body: String) {
    Column(
        Modifier.fillMaxSize().padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(icon, fontSize = 82.sp, color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(20.dp))
        Text(
            title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            fontSize = 17.sp
        )
    }
}

@Composable
fun EpisodeListScreen(
    title: String,
    episodes: List<Episode>,
    emptyIcon: String,
    emptyTitle: String,
    emptyBody: String,
    model: AppModel,
    queueMode: Boolean = false,
    onFilter: (() -> Unit)? = null,
    onSort: (() -> Unit)? = null
) {
    Column(Modifier.fillMaxSize()) {
        MainHeader(title, actions = {
            IconButton(onClick = {}) { Text("⌕", fontSize = 34.sp) }
            if (onFilter != null) {
                IconButton(onClick = onFilter) { Text("≡", fontSize = 24.sp) }
            }
            if (onSort != null) {
                IconButton(onClick = onSort) { Text("↕", fontSize = 24.sp) }
            }
        })
        if (episodes.isEmpty()) {
            EmptyState(emptyIcon, emptyTitle, emptyBody)
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (queueMode) {
                    item {
                        Text(
                            "剩余 ${episodes.size} 个单集 · ${episodes.sumOf { it.durationSeconds } / 60} 分钟",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
                items(episodes, key = { it.id }) { episode ->
                    EpisodeRow(episode, model, queueMode)
                }
            }
        }
    }
}

@Composable
fun EpisodeRow(episode: Episode, model: AppModel, queueMode: Boolean = false) {
    ElevatedCard(
        Modifier.fillMaxWidth().clickable {
            model.selectedEpisodeId = episode.id
            model.page = Page.EPISODE
        },
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(64.dp).clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0A8DD7), Color(0xFF164B91))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) { AntennaLogo(Modifier.size(46.dp)) }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (episode.initialStatus == "new") {
                        Text("新", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(5.dp))
                    }
                    Text(
                        episode.title,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    "${prettyDate(episode.published)} · ${episode.duration}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                    TextButton(
                        onClick = { model.toggleQueue(episode.id) },
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text(
                            if (episode.id in model.queue) "移出队列" else "＋队列",
                            fontSize = 11.sp
                        )
                    }
                    TextButton(
                        onClick = { model.toggleDownload(episode.id) },
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text(
                            if (episode.id in model.downloads) "已下载" else "下载",
                            fontSize = 11.sp
                        )
                    }
                    TextButton(
                        onClick = { model.toggleFavorite(episode.id) },
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text(if (episode.id in model.favorites) "★" else "☆", fontSize = 17.sp)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FilledIconButton(onClick = { model.play(episode.id) }) { Text("▶") }
                if (queueMode) {
                    Row {
                        IconButton(
                            onClick = { model.moveQueue(episode.id, -1) },
                            modifier = Modifier.size(30.dp)
                        ) { Text("↑") }
                        IconButton(
                            onClick = { model.moveQueue(episode.id, 1) },
                            modifier = Modifier.size(30.dp)
                        ) { Text("↓") }
                    }
                }
            }
        }
    }
}

@Composable
fun MiniPlayer(model: AppModel) {
    val episode = model.currentEpisode ?: return
    Surface(
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth().height(74.dp)
            .clickable { model.page = Page.PLAYER }
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AntennaLogo(Modifier.size(46.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    episode.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )
                LinearProgressIndicator(
                    progress = {
                        model.progressSeconds.toFloat() /
                            episode.durationSeconds.coerceAtLeast(1)
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 7.dp)
                )
            }
            IconButton(onClick = { model.seek(-15) }) { Text("↶15") }
            FilledIconButton(onClick = { model.togglePlay() }) {
                Text(if (model.isPlaying) "Ⅱ" else "▶")
            }
        }
    }
}

@Composable
fun PodcastScreen(
    model: AppModel,
    onBack: () -> Unit,
    onFilter: () -> Unit,
    onRename: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxWidth().height(260.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1D6B4D), Color(0xFF8AC09A))
                    )
                )
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.TopStart).padding(top = 8.dp)
            ) { Text("‹", color = Color.White, fontSize = 42.sp) }
            IconButton(
                onClick = onRename,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 10.dp)
            ) { Text("⋮", color = Color.White, fontSize = 30.sp) }
            Column(
                Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AntennaLogo(Modifier.size(102.dp))
                Spacer(Modifier.height(12.dp))
                Text(
                    model.podcastName,
                    color = Color.White,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("黑盒媒体", color = Color.White.copy(alpha = .85f))
            }
            Row(
                Modifier.align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.spacedBy(26.dp)
            ) {
                TextButton(onClick = { model.page = Page.INFO }) {
                    Text("ⓘ 信息", color = Color.White)
                }
                TextButton(onClick = onFilter) { Text("≡ 筛选", color = Color.White) }
                TextButton(onClick = onRename) { Text("⚙ 设置", color = Color.White) }
            }
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (model.filterUnplayed) "已筛选的" else "全部单集",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { model.sortNewest = !model.sortNewest }) {
                        Text("日期 ${if (model.sortNewest) "↓" else "↑"}")
                    }
                }
            }
            val shown = model.episodes
                .filter { !model.filterUnplayed || it.id !in model.history }
                .let { if (model.sortNewest) it else it.reversed() }
            items(shown, key = { it.id }) { EpisodeRow(it, model) }
        }
    }
}

@Composable
fun PodcastInfoScreen(model: AppModel) {
    Column(Modifier.fillMaxSize()) {
        MainHeader("播客信息", back = { model.page = Page.PODCAST })
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AntennaLogo(Modifier.size(120.dp))
            Spacer(Modifier.height(18.dp))
            Text(model.podcastName, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Text("黑盒媒体", color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(Modifier.padding(vertical = 24.dp))
            Text(
                "虚构的科技播客，用于播客阅读器复现测试。每周讨论工程、产品与开源世界。",
                fontSize = 17.sp
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "语言：中文   ·   分类：科技",
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "RSS：https://example.test/blackbox-feed.xml",
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(28.dp))
            OutlinedButton(onClick = { model.unsubscribe() }) { Text("取消订阅") }
        }
    }
}

@Composable
fun EpisodeDetailScreen(model: AppModel) {
    val episode = model.episode(model.selectedEpisodeId)
        ?: model.episodes.firstOrNull()
        ?: return
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        MainHeader("单集", back = { model.page = Page.PODCAST }, actions = {
            IconButton(onClick = { model.toggleFavorite(episode.id) }) {
                Text(
                    if (episode.id in model.favorites) "★" else "☆",
                    fontSize = 28.sp
                )
            }
        })
        Box(
            Modifier.size(170.dp).align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF078EDA), Color(0xFF253C90))
                    )
                ),
            contentAlignment = Alignment.Center
        ) { AntennaLogo(Modifier.size(120.dp)) }
        Column(Modifier.padding(22.dp)) {
            Text(episode.title, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Text(
                "${model.podcastName} · ${prettyDate(episode.published)} · ${episode.duration}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    model.play(episode.id)
                    model.page = Page.PLAYER
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("▶  播放") }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = { model.toggleQueue(episode.id) }) {
                    Text(
                        if (episode.id in model.queue) "✓ 已加入队列" else "＋ 加入队列"
                    )
                }
                TextButton(onClick = { model.toggleDownload(episode.id) }) {
                    Text(
                        if (episode.id in model.downloads) "✓ 已下载" else "⇩ 下载"
                    )
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            Text("单集简介", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(episode.description, fontSize = 17.sp, lineHeight = 26.sp)
            if (episode.chapters.isNotEmpty()) {
                Spacer(Modifier.height(22.dp))
                Text("章节", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                episode.chapters.forEach { chapter ->
                    ListItem(
                        headlineContent = { Text(chapter.title) },
                        leadingContent = { Text(formatTime(chapter.start)) },
                        modifier = Modifier.clickable {
                            model.play(episode.id)
                            model.setProgress(chapter.start.toFloat())
                            model.page = Page.PLAYER
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerScreen(model: AppModel) {
    val episode = model.currentEpisode ?: return
    var showSpeed by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MainHeader("正在播放", back = { model.page = Page.HOME }, actions = {
            IconButton(onClick = { model.toggleFavorite(episode.id) }) {
                Text(
                    if (episode.id in model.favorites) "★" else "☆",
                    fontSize = 28.sp
                )
            }
        })
        Spacer(Modifier.height(20.dp))
        Box(
            Modifier.size(260.dp).clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF0799E6), Color(0xFF263A86))
                    )
                ),
            contentAlignment = Alignment.Center
        ) { AntennaLogo(Modifier.size(184.dp)) }
        Spacer(Modifier.height(25.dp))
        Text(
            episode.title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(model.podcastName, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        Slider(
            value = model.progressSeconds.toFloat()
                .coerceIn(0f, episode.durationSeconds.toFloat()),
            onValueChange = model::setProgress,
            valueRange = 0f..episode.durationSeconds.toFloat().coerceAtLeast(1f)
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(model.progressSeconds))
            Text(episode.duration)
        }
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { model.seek(-15) },
                modifier = Modifier.size(64.dp)
            ) { Text("↶\n15", textAlign = TextAlign.Center, fontSize = 18.sp) }
            FilledIconButton(
                onClick = { model.togglePlay() },
                modifier = Modifier.size(78.dp)
            ) { Text(if (model.isPlaying) "Ⅱ" else "▶", fontSize = 30.sp) }
            IconButton(
                onClick = { model.seek(15) },
                modifier = Modifier.size(64.dp)
            ) { Text("↷\n15", textAlign = TextAlign.Center, fontSize = 18.sp) }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Box {
                TextButton(onClick = { showSpeed = true }) {
                    Text("${model.speed}×  倍速")
                }
                DropdownMenu(
                    expanded = showSpeed,
                    onDismissRequest = { showSpeed = false }
                ) {
                    listOf(.75f, 1f, 1.25f, 1.5f, 2f).forEach { value ->
                        DropdownMenuItem(
                            text = { Text("${value}×") },
                            onClick = {
                                model.updateSpeed(value)
                                showSpeed = false
                            }
                        )
                    }
                }
            }
            TextButton(onClick = { model.toggleQueue(episode.id) }) {
                Text(if (episode.id in model.queue) "队列 ✓" else "加入队列")
            }
            if (episode.chapters.isNotEmpty()) {
                TextButton(onClick = {}) {
                    Text("章节 ${episode.chapters.size}")
                }
            }
        }
    }
}
