package com.blackboxbench.reproduction

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PodcastDetailScreen(
    model: AppModel,
    onBack: () -> Unit,
    onEpisode: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            title = model.podcastTitle,
            onBack = onBack,
            actions = {
                AppIconButton(Icons.Filled.Refresh, "刷新", {}, modifier = Modifier.size(44.dp))
                AppIconButton(Icons.Filled.MoreVert, "更多", {}, modifier = Modifier.size(44.dp))
            },
        )
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(76.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                AppIcons.Rss,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                model.podcastTitle,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                model.podcastAuthor,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        model.podcastDescription,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { model.unsubscribe() }) { Text("退订") }
                        Button(onClick = { model.markAllPlayed() }) { Text("全部标记为已播放") }
                    }
                }
            }
            items(model.episodes, key = { it.guid }) { ep ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEpisode(ep.guid) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        if (ep.isNew) {
                            Text("新", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(ep.title, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${ep.dateLabel} · ${ep.durationLabel}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (ep.favorite) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    if (ep.downloaded) {
                        Icon(
                            AppIcons.Download,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EpisodeDetailScreen(
    model: AppModel,
    guid: String,
    onBack: () -> Unit,
) {
    val ep = model.episode(guid)
    Column(Modifier.fillMaxSize()) {
        AppTopBar(model.podcastTitle, onBack = onBack)
        if (ep == null) {
            EmptyState("单集不存在")
            return@Column
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(ep.title, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Text(
                "${ep.dateLabel} · ${ep.durationLabel}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { model.play(ep.guid) }) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("播放")
                }
                AppIconButton(
                    if (ep.favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    "收藏",
                    { model.toggleFavorite(ep.guid) },
                )
                AppIconButton(AppIcons.Download, "下载", { model.toggleDownloaded(ep.guid) })
                AppIconButton(Icons.Filled.Delete, "删除", {})
            }
            Spacer(Modifier.height(16.dp))
            Text(ep.description, fontSize = 15.sp)
            if (ep.chapters.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text("章节", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                ep.chapters.forEach { chapter ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Text(
                            "%d:%02d".format(chapter.start / 60, chapter.start % 60),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(64.dp),
                        )
                        Text(chapter.title, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
