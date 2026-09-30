package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ------------------------------------------------------------------ Home

@Composable
fun HomeScreen(
    model: AppModel,
    onSearch: () -> Unit,
    onOverflow: () -> Unit,
    onOpenPodcast: () -> Unit = {},
) {
    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            title = "首页",
            actions = {
                AppIconButton(Icons.Filled.Search, "搜索", onSearch, modifier = Modifier.size(44.dp))
                AppIconButton(Icons.Filled.MoreVert, "更多", onOverflow, modifier = Modifier.size(44.dp))
            },
        )
        Box(Modifier.fillMaxSize()) {
            if (model.subscribed) {
                LazyColumn(Modifier.fillMaxSize()) {
                    if (model.inProgress != null) {
                        item { SectionHeader("继续收听") }
                        item { EpisodeRow(model.inProgress!!) }
                    }
                    item { SectionHeader("最近更新") }
                    items(model.episodes.take(6), key = { it.guid }) { ep ->
                        EpisodeRow(ep)
                    }
                    item { SectionHeader("订阅列表") }
                    item {
                        PodcastCardRow(
                            title = model.podcastTitle,
                            author = model.podcastAuthor,
                            onOpen = onOpenPodcast,
                        )
                    }
                }
            } else {
                WelcomeEmpty()
            }
        }
    }
}

@Composable
private fun WelcomeEmpty() {
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(150.dp))
            AntennaPodLogo(76)
            Spacer(Modifier.height(18.dp))
            Text(
                "欢迎使用 AntennaPod！",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "您尚未订阅任何播客。请打开菜单添加播客。",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Canvas(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 56.dp, bottom = 56.dp)
                .size(96.dp)
        ) {
            val p = Path()
            p.moveTo(size.width * 0.85f, 0f)
            p.cubicTo(
                size.width * 0.1f, size.height * 0.15f,
                size.width * 1.05f, size.height * 0.65f,
                size.width * 0.62f, size.height * 0.92f,
            )
            drawPath(
                p,
                color = Color(0xFF7A8994),
                style = Stroke(width = 3.5f, cap = StrokeCap.Round),
            )
            val tip = Offset(size.width * 0.62f, size.height * 0.92f)
            val head = Path()
            head.moveTo(tip.x - 9f, tip.y)
            head.lineTo(tip.x + 1f, tip.y - 1f)
            head.lineTo(tip.x - 3f, tip.y - 11f)
            drawPath(head, color = Color(0xFF7A8994), style = Stroke(width = 3.5f, cap = StrokeCap.Round))
        }
    }
}

// ------------------------------------------------------------------ Queue

@Composable
fun QueueScreen(
    model: AppModel,
    onSearch: () -> Unit,
    onOverflow: () -> Unit,
) {
    val episodes = model.queueEpisodes
    val minutes = episodes.sumOf { it.durationSec } / 60
    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            title = "队列",
            actions = {
                AppIconButton(Icons.Filled.Search, "搜索", onSearch, modifier = Modifier.size(44.dp))
                AppIconButton(Icons.Filled.MoreVert, "更多", onOverflow, modifier = Modifier.size(44.dp))
            },
        )
        Row(Modifier.padding(start = 16.dp, top = 6.dp, bottom = 6.dp)) {
            Text(
                "剩余 ${episodes.size} 个单集 · $minutes 分钟",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(Modifier.fillMaxSize()) {
            if (episodes.isEmpty()) {
                EmptyState(
                    "队列中无单集",
                    "通过下载添加单集，或长按单集并选择\"添加到队列\"。",
                )
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(episodes, key = { it.guid }) { ep -> EpisodeRow(ep) }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Inbox

@Composable
fun InboxScreen() {
    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            title = "收件箱",
            actions = {
                AppIconButton(Icons.Filled.Search, "搜索", {}, modifier = Modifier.size(44.dp))
                AppIconButton(Icons.Filled.MoreVert, "更多", {}, modifier = Modifier.size(44.dp))
            },
        )
        EmptyState(
            "收件箱中没有单集",
            "新单集上线后，将在此处显示。随后您可以决定是否对其感兴趣。",
        )
    }
}

// ------------------------------------------------------------------ Subscriptions

@Composable
fun SubscriptionsScreen(
    model: AppModel,
    onSearch: () -> Unit,
    onOverflow: () -> Unit,
    onAdd: () -> Unit,
    onOpenPodcast: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            AppTopBar(
                title = "订阅",
                actions = {
                    AppIconButton(Icons.Filled.Search, "搜索", onSearch, modifier = Modifier.size(44.dp))
                    AppIconButton(Icons.Filled.MoreVert, "更多", onOverflow, modifier = Modifier.size(44.dp))
                },
            )
            if (model.subscribed) {
                LazyColumn(Modifier.fillMaxSize()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onOpenPodcast)
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    AppIcons.Rss,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(26.dp),
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(model.podcastTitle, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    model.podcastAuthor,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            } else {
                EmptyState("没有订阅", "点按下方的+号按钮来订阅播客。", icon = AppIcons.Grid)
            }
        }
        // FAB
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 44.dp)
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(onClick = onAdd),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = "添加播客",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

// ------------------------------------------------------------------ Episodes / Downloads / History / Favorites

@Composable
fun EpisodesScreen(model: AppModel, onSearch: () -> Unit, onFilter: () -> Unit, onOverflow: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            title = "单集",
            actions = {
                AppIconButton(Icons.Filled.Search, "搜索", onSearch, modifier = Modifier.size(44.dp))
                AppIconButton(AppIcons.Filter, "筛选", onFilter, modifier = Modifier.size(44.dp))
                AppIconButton(Icons.Filled.MoreVert, "更多", onOverflow, modifier = Modifier.size(44.dp))
            },
        )
        if (model.episodes.isEmpty()) {
            EmptyState("无单集", "添加播客时，其单集将在此处显示。")
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(model.episodes, key = { it.guid }) { ep -> EpisodeRow(ep) }
            }
        }
    }
}

@Composable
fun DownloadsScreen(model: AppModel, onSearch: () -> Unit, onOverflow: () -> Unit) {
    val items = model.downloaded
    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            title = "下载",
            actions = {
                AppIconButton(Icons.Filled.Search, "搜索", onSearch, modifier = Modifier.size(44.dp))
                AppIconButton(AppIcons.Download, "全部下载", {}, modifier = Modifier.size(44.dp))
                AppIconButton(Icons.Filled.MoreVert, "更多", onOverflow, modifier = Modifier.size(44.dp))
            },
        )
        if (items.isEmpty()) {
            EmptyState("无已下载单集", "您可在播客详情页下载单集。")
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(items, key = { it.guid }) { ep -> EpisodeRow(ep) }
            }
        }
    }
}

@Composable
fun HistoryScreen(model: AppModel, onSearch: () -> Unit, onOverflow: () -> Unit) {
    val items = model.history
    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            title = "播放记录",
            actions = {
                AppIconButton(Icons.Filled.Search, "搜索", onSearch, modifier = Modifier.size(44.dp))
                AppIconButton(Icons.Filled.MoreVert, "更多", onOverflow, modifier = Modifier.size(44.dp))
            },
        )
        if (items.isEmpty()) {
            EmptyState("无历史记录", "收听单集后，该单集将在此处显示。")
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(items, key = { it.guid }) { ep -> EpisodeRow(ep) }
            }
        }
    }
}

@Composable
fun FavoritesScreen(model: AppModel, onSearch: () -> Unit, onOverflow: () -> Unit) {
    val items = model.favorites
    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            title = "收藏",
            actions = {
                AppIconButton(Icons.Filled.Search, "搜索", onSearch, modifier = Modifier.size(44.dp))
                AppIconButton(Icons.Filled.MoreVert, "更多", onOverflow, modifier = Modifier.size(44.dp))
            },
        )
        if (items.isEmpty()) {
            EmptyState("无收藏", "轻触加星图标将单集标记为收藏。")
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(items, key = { it.guid }) { ep -> EpisodeRow(ep) }
            }
        }
    }
}

// ------------------------------------------------------------------ Statistics

@Composable
fun StatisticsScreen(
    model: AppModel,
    tab: Int,
    onTab: (Int) -> Unit,
    onOverflow: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            title = "统计",
            actions = {
                AppIconButton(AppIcons.Filter, "筛选", {}, modifier = Modifier.size(44.dp))
                AppIconButton(Icons.Filled.MoreVert, "更多", onOverflow, modifier = Modifier.size(44.dp))
            },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            listOf("订阅", "年", "下载").forEachIndexed { index, label ->
                Column(
                    modifier = Modifier
                        .clickable { onTab(index) }
                        .padding(horizontal = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        label,
                        fontSize = 15.sp,
                        fontWeight = if (tab == index) FontWeight.Medium else FontWeight.Normal,
                        color = if (tab == index) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .width(44.dp)
                            .height(3.dp)
                            .background(
                                if (tab == index) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        when (tab) {
            2 -> Gauge(
                value = formatBytes(model.downloadedBytes),
                caption = "设备上${model.downloaded.size}个单集的总大小",
            )
            else -> Gauge(
                value = "%.1f 小时".format(model.playedSeconds / 3600.0),
                caption = "2026年9月 到 2026年9月 期间播放的",
            )
        }
    }
}

@Composable
private fun Gauge(value: String, caption: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 3f
            drawArc(
                color = Color(0xFF8A8A8A),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(size.width * 0.16f, size.height * 0.10f),
                size = Size(size.width * 0.68f, size.width * 0.68f),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Column(
            modifier = Modifier.padding(top = 92.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, fontSize = 26.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(8.dp))
            Text(caption, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.2f kB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    return "%.1f GB".format(mb / 1024.0)
}

// ------------------------------------------------------------------ Rows

@Composable
fun PodcastCardRow(title: String, author: String, onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AntennaPodLogo(52)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(author, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun EpisodeRow(ep: Episode) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(ep.title, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(
                "${ep.dateLabel} · ${ep.durationLabel}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        if (ep.favorite) {
            Icon(
                Icons.Filled.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        if (ep.downloaded) {
            Icon(
                AppIcons.Download,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        if (ep.played) {
            Text("已播放", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
