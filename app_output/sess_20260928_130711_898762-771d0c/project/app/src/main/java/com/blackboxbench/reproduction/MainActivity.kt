package com.blackboxbench.reproduction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        setContent {
            val model = remember { AppModel(applicationContext) }
            BenchmarkAppTheme(
                themeMode = model.settings.themeMode,
                pureBlack = model.settings.pureBlack,
            ) {
                AppRoot(model)
            }
        }
    }
}

@Composable
fun AppRoot(model: AppModel) {
    var route by remember { mutableStateOf("home") }
    val stack = remember { mutableStateListOf<String>() }
    fun navigate(target: String) {
        if (target != route) {
            stack.add(route)
            route = target
        }
    }

    fun goBack() {
        if (stack.isNotEmpty()) route = stack.removeAt(stack.lastIndex)
    }

    var moreMenu by remember { mutableStateOf(false) }
    var showRss by remember { mutableStateOf(false) }
    var showHomeConfig by remember { mutableStateOf(false) }
    var showCustomizeNav by remember { mutableStateOf(false) }
    var showQueueSort by remember { mutableStateOf(false) }
    var showFilter by remember { mutableStateOf(false) }
    var queueLocked by remember { mutableStateOf(false) }
    var homeOverflow by remember { mutableStateOf(false) }
    var queueOverflow by remember { mutableStateOf(false) }
    var episodesOverflow by remember { mutableStateOf(false) }
    var downloadsOverflow by remember { mutableStateOf(false) }
    var historyOverflow by remember { mutableStateOf(false) }
    var statsTab by remember { mutableIntStateOf(0) }
    var addQuery by remember { mutableStateOf("") }
    var episodeGuid by remember { mutableStateOf("") }

    BackHandler(enabled = stack.isNotEmpty()) { goBack() }

    LaunchedEffect(model.playing) {
        while (model.playing) {
            delay(1000)
            model.onTick()
        }
    }

    val mainRoutes = setOf(
        "home", "queue", "inbox", "subscriptions", "episodes", "downloads",
        "history", "favorites", "statistics", "addpodcast", "podcast", "episode",
    )
    val bottomVisible = route in mainRoutes
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val selected = when (route) {
        "home" -> 0
        "queue" -> 1
        "inbox" -> 2
        "subscriptions", "addpodcast", "podcast", "episode" -> 3
        else -> 4
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                Column(Modifier.fillMaxSize()) {
                    Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                    Box(Modifier.weight(1f)) {
                        ScreenContent(
                            route = route,
                            model = model,
                            statsTab = statsTab,
                            onStatsTab = { statsTab = it },
                            addQuery = addQuery,
                            onAddQuery = { addQuery = it },
                            episodeGuid = episodeGuid,
                            onEpisodeGuid = { episodeGuid = it },
                            onRss = { showRss = true },
                            onFilter = { showFilter = true },
                            onNavigate = ::navigate,
                            onBack = ::goBack,
                            onOverflow = { target ->
                                when (target) {
                                    "home" -> homeOverflow = true
                                    "queue" -> queueOverflow = true
                                    "episodes" -> episodesOverflow = true
                                    "downloads" -> downloadsOverflow = true
                                    "history" -> historyOverflow = true
                                }
                            },
                        )
                    }
                }
                Box(Modifier.align(Alignment.TopEnd)) {
                    OverflowMenu(
                        expanded = homeOverflow,
                        onDismiss = { homeOverflow = false },
                        items = listOf("刷新" to Icons.Filled.Refresh, "配置主屏幕" to null),
                        onItem = { index -> if (index == 1) showHomeConfig = true },
                    )
                    QueueOverflowMenu(
                        expanded = queueOverflow,
                        onDismiss = { queueOverflow = false },
                        locked = queueLocked,
                        onSort = { showQueueSort = true },
                        onClear = { model.clearQueue() },
                        onLock = { queueLocked = !queueLocked },
                    )
                    OverflowMenu(
                        expanded = episodesOverflow,
                        onDismiss = { episodesOverflow = false },
                        items = listOf("排序" to AppIcons.Sort, "刷新" to Icons.Filled.Refresh),
                        onItem = { },
                    )
                    OverflowMenu(
                        expanded = downloadsOverflow,
                        onDismiss = { downloadsOverflow = false },
                        items = listOf(
                            "删除已播放的" to Icons.Filled.Delete,
                            "排序" to AppIcons.Sort,
                            "刷新" to Icons.Filled.Refresh,
                        ),
                        onItem = { },
                    )
                    OverflowMenu(
                        expanded = historyOverflow,
                        onDismiss = { historyOverflow = false },
                        items = listOf("排序" to AppIcons.Sort, "刷新" to Icons.Filled.Refresh),
                        onItem = { },
                    )
                }
            }
            if (bottomVisible && model.settings.bottomNavigation) {
                NowPlayingBar(model) { navigate("episode") }
                BottomNav(selected = selected, onSelect = { index ->
                    when (index) {
                        0 -> navigate("home")
                        1 -> navigate("queue")
                        2 -> navigate("inbox")
                        3 -> navigate("subscriptions")
                        4 -> moreMenu = true
                    }
                })
            }
        }

        if (moreMenu) {
            MoreMenuOverlay(
                bottomPadding = (64 + navBottom.value).toInt(),
                onDismiss = { moreMenu = false },
                onSelect = { item ->
                    moreMenu = false
                    when (item.route) {
                        "customize_nav" -> showCustomizeNav = true
                        null -> {}
                        else -> navigate(item.route)
                    }
                },
            )
        }
        if (showHomeConfig) HomeConfigureDialog(model) { showHomeConfig = false }
        if (showCustomizeNav) CustomizeNavigationDialog(model) { showCustomizeNav = false }
        if (showQueueSort) QueueSortSheet { showQueueSort = false }
        if (showFilter) FilterSheet { showFilter = false }
        if (showRss) {
            RssDialog(
                initial = "",
                onDismiss = { showRss = false },
                onConfirm = { url ->
                    val trimmed = url.trim()
                    val valid = (trimmed.startsWith("http://") || trimmed.startsWith("https://")) &&
                        !trimmed.contains(' ') && trimmed.substringAfter("://").contains('.')
                    if (valid) {
                        model.subscribe()
                        showRss = false
                        navigate("subscriptions")
                    }
                    valid
                },
            )
        }
    }
}

@Composable
private fun ScreenContent(
    route: String,
    model: AppModel,
    statsTab: Int,
    onStatsTab: (Int) -> Unit,
    addQuery: String,
    onAddQuery: (String) -> Unit,
    episodeGuid: String,
    onEpisodeGuid: (String) -> Unit,
    onRss: () -> Unit,
    onFilter: () -> Unit,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    onOverflow: (String) -> Unit,
) {
    when (route) {
        "home" -> HomeScreen(
            model,
            onSearch = { onNavigate("search") },
            onOverflow = { onOverflow("home") },
            onOpenPodcast = { onNavigate("podcast") },
        )
        "queue" -> QueueScreen(model, onSearch = { onNavigate("search") }, onOverflow = { onOverflow("queue") })
        "inbox" -> InboxScreen()
        "subscriptions" -> SubscriptionsScreen(
            model = model,
            onSearch = { onNavigate("search") },
            onOverflow = { onNavigate("search") },
            onAdd = { onNavigate("addpodcast") },
            onOpenPodcast = { onNavigate("podcast") },
        )
        "episodes" -> EpisodesScreen(
            model,
            onSearch = { onNavigate("search") },
            onFilter = onFilter,
            onOverflow = { onOverflow("episodes") },
        )
        "downloads" -> DownloadsScreen(
            model,
            onSearch = { onNavigate("search") },
            onOverflow = { onOverflow("downloads") },
        )
        "history" -> HistoryScreen(
            model,
            onSearch = { onNavigate("search") },
            onOverflow = { onOverflow("history") },
        )
        "favorites" -> FavoritesScreen(model, onSearch = { onNavigate("search") }, onOverflow = {})
        "statistics" -> StatisticsScreen(model, statsTab, onStatsTab, onOverflow = {})
        "addpodcast" -> AddPodcastScreen(
            onBack = onBack,
            query = addQuery,
            onQuery = onAddQuery,
            onSearchSubmit = { onNavigate("search") },
            onRss = onRss,
            onLocalFolder = { model.subscribe() },
            onApple = { onNavigate("search_apple") },
            onFyyd = { onNavigate("search_fyyd") },
            onIndex = { onNavigate("search_index") },
            onOpml = { model.subscribe() },
        )
        "search" -> GlobalSearchScreen(onBack = onBack, showProviderHint = true)
        "search_apple" -> ProviderSearchScreen("Apple", onBack = onBack)
        "search_fyyd" -> ProviderSearchScreen("fyyd", onBack = onBack)
        "search_index" -> ProviderSearchScreen("Podcast Index", onBack = onBack)
        "settings" -> SettingsMainScreen(onBack = onBack) { onNavigate(it) }
        "settings_ui" -> SettingsUiScreen(onBack = onBack, model = model)
        "settings_playback" -> SettingsPlaybackScreen(onBack = onBack, model = model)
        "settings_downloads" -> SettingsDownloadsScreen(onBack = onBack, model = model)
        "settings_sync" -> SettingsSyncScreen(onBack = onBack)
        "settings_backup" -> SettingsBackupScreen(onBack = onBack)
        "settings_notifications" -> SettingsNotificationsScreen(onBack = onBack, model = model)
        "podcast" -> PodcastDetailScreen(model, onBack = onBack) { guid ->
            onEpisodeGuid(guid)
            onNavigate("episode")
        }
        "episode" -> EpisodeDetailScreen(model, episodeGuid, onBack = onBack)
        else -> HomeScreen(model, onSearch = {}, onOverflow = {})
    }
}

@Composable
private fun QueueOverflowMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    locked: Boolean,
    onSort: () -> Unit,
    onClear: () -> Unit,
    onLock: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text("排序", fontSize = 16.sp) },
            leadingIcon = { Icon(AppIcons.Sort, contentDescription = null, modifier = Modifier.size(22.dp)) },
            onClick = { onDismiss(); onSort() },
        )
        DropdownMenuItem(
            text = { Text("清除队列", fontSize = 16.sp) },
            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(22.dp)) },
            onClick = { onDismiss(); onClear() },
        )
        DropdownMenuItem(
            text = { Text("刷新", fontSize = 16.sp) },
            leadingIcon = { Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(22.dp)) },
            onClick = { onDismiss() },
        )
        DropdownMenuItem(
            text = { Text("锁定队列", fontSize = 16.sp) },
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(22.dp)) },
            trailingIcon = { Checkbox(checked = locked, onCheckedChange = null) },
            onClick = { onDismiss(); onLock() },
        )
    }
}

@Composable
private fun BottomNav(selected: Int, onSelect: (Int) -> Unit) {
    val items: List<Pair<String, ImageVector>> = listOf(
        "首页" to Icons.Filled.Home,
        "队列" to AppIcons.Queue,
        "收件箱" to AppIcons.Inbox,
        "订阅" to AppIcons.Grid,
        "更多" to Icons.Filled.MoreVert,
    )
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(Modifier.fillMaxWidth().height(64.dp)) {
            items.forEachIndexed { index, (label, icon) ->
                val isSelected = selected == index
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onSelect(index) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        icon,
                        contentDescription = label,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(25.dp),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun NowPlayingBar(model: AppModel, onOpen: () -> Unit) {
    val ep = model.current() ?: return
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (model.playing) AppIcons.Pause else Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(28.dp)
                    .clickable { model.togglePlayPause() },
            )
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text(ep.title, fontSize = 14.sp, maxLines = 1)
                Text(
                    "${formatDuration(model.positionSec)} / ${ep.durationLabel}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

fun formatDuration(sec: Int): String = "%d:%02d".format(sec / 60, sec % 60)
