package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ------------------------------------------------------------------ More menu

data class MoreItem(val label: String, val icon: ImageVector, val route: String?)

val moreMenuItems = listOf(
    MoreItem("单集", AppIcons.Episodes, "episodes"),
    MoreItem("下载", AppIcons.Download, "downloads"),
    MoreItem("播放记录", AppIcons.History, "history"),
    MoreItem("收藏", Icons.Filled.Favorite, "favorites"),
    MoreItem("统计", AppIcons.BarChart, "statistics"),
    MoreItem("添加播客", AppIcons.Rss, "addpodcast"),
    MoreItem("自定义导航", Icons.Filled.Menu, "customize_nav"),
    MoreItem("设置", Icons.Filled.Settings, "settings"),
)

@Composable
fun MoreMenuOverlay(
    bottomPadding: Int,
    onDismiss: () -> Unit,
    onSelect: (MoreItem) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = MutableInteractionSource(),
                indication = null,
                onClick = onDismiss,
            )
    ) {
        MenuSurface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = bottomPadding.dp)
                .width(248.dp)
        ) {
            Column(Modifier.padding(vertical = 4.dp)) {
                moreMenuItems.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(item) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            item.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(22.dp))
                        Text(item.label, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ generic overflow

@Composable
fun OverflowMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    items: List<Pair<String, ImageVector?>>,
    onItem: (Int) -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        items.forEachIndexed { index, item ->
            DropdownMenuItem(
                text = { Text(item.first, fontSize = 16.sp) },
                leadingIcon = item.second?.let { icon ->
                    { Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp)) }
                },
                onClick = {
                    onDismiss()
                    onItem(index)
                },
            )
        }
    }
}

// ------------------------------------------------------------------ overlay shells

/** Full-screen scrim with a centred card; lives in the main window. */
@Composable
fun CenterOverlay(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable(
                interactionSource = MutableInteractionSource(),
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .clickable(
                    interactionSource = MutableInteractionSource(),
                    indication = null,
                    onClick = {},
                )
        ) {
            content()
        }
    }
}

/** Full-screen scrim with content pinned to the bottom edge. */
@Composable
fun BottomSheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable(
                interactionSource = MutableInteractionSource(),
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = MutableInteractionSource(),
                    indication = null,
                    onClick = {},
                ),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column {
                content()
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }
    }
}

// ------------------------------------------------------------------ home configure

@Composable
fun HomeConfigureDialog(model: AppModel, onDismiss: () -> Unit) {
    CenterOverlay(onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.padding(vertical = 20.dp)) {
                Text(
                    "配置主屏幕",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                Spacer(Modifier.height(12.dp))
                Column(
                    Modifier
                        .heightIn(max = 260.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "显示",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
                    )
                    model.homeSections.forEach { section ->
                        DragRow(section)
                    }
                    Text(
                        "隐藏",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = {
                        model.updateHomeSections(
                            listOf("继续收听", "最近更新", "不期而遇", "订阅列表", "管理下载")
                        )
                    }) { Text("重置") }
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = onDismiss) { Text("确定") }
                }
            }
        }
    }
}

@Composable
private fun DragRow(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            AppIcons.DragHandle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(label, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

// ------------------------------------------------------------------ customize navigation

@Composable
fun CustomizeNavigationDialog(model: AppModel, onDismiss: () -> Unit) {
    val all = listOf("首页", "队列", "收件箱", "订阅", "单集", "下载", "播放记录", "收藏", "统计", "添加播客")
    CenterOverlay(onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.padding(vertical = 20.dp)) {
                Text(
                    "抽屉偏好设置",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                Spacer(Modifier.height(12.dp))
                Column(
                    Modifier
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "显示",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
                    )
                    model.navItems.forEach { DragRow(it) }
                    Text(
                        "隐藏",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
                    )
                    model.hiddenNavItems.forEach { DragRow(it) }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = { model.setNavItems(all, emptyList()) }) { Text("重置") }
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = onDismiss) { Text("确定") }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ queue dialogs

@Composable
fun QueueSortSheet(onDismiss: () -> Unit) {
    BottomSheet(onDismiss) {
        Text(
            "排序",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        )
        listOf("单集标题", "播客标题", "按时长", "按日期", "随机", "智能随机播放").forEach {
            Text(
                it,
                fontSize = 16.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 24.dp, vertical = 14.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = false, onCheckedChange = {})
            Text("保持排序", fontSize = 16.sp)
        }
    }
}

@Composable
fun FilterSheet(onDismiss: () -> Unit) {
    BottomSheet(onDismiss) {
        Spacer(Modifier.height(12.dp))
        val pairs = listOf(
            listOf("已播放", "未播放"),
            listOf("已暂停", "未暂停"),
            listOf("已收藏", "未收藏"),
            listOf("包含媒体", "没有媒体"),
            listOf("已加入队列", "未加入队列"),
            listOf("已下载", "未下载"),
        )
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            pairs.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { label ->
                        FilterChip(label, Modifier.weight(1f))
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onDismiss) {
                Text("重置", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onDismiss) { Text("确认") }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun FilterChip(label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
