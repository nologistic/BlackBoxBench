package com.blackboxbench.reproduction.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blackboxbench.reproduction.BlueColor
import com.blackboxbench.reproduction.Prefs
import com.blackboxbench.reproduction.Settings
import com.blackboxbench.reproduction.StateStore
import com.blackboxbench.reproduction.ViewRef

fun incompleteCount(pred: (com.blackboxbench.reproduction.Task) -> Boolean): Int =
    StateStore.tasks.count { !it.completed && pred(it) }

@Composable
fun DrawerContent(
    current: ViewRef,
    onSelect: (ViewRef) -> Unit,
    onNewTag: () -> Unit,
    onNewList: () -> Unit,
    onNewFilter: () -> Unit,
    onPickLocation: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { onSelect(ViewRef.MY_TASKS) }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("☰", fontSize = 20.sp, color = if (current.type == "mytasks") BlueColor else Color(0xFF5A5A62))
            Spacer(Modifier.width(16.dp))
            Text(
                "我的任务",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (current.type == "mytasks") BlueColor else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                incompleteCount { true }.toString(),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider(Modifier.padding(vertical = 4.dp))

        if (Settings.drawerFiltersEnabled) {
            DrawerGroup(
                key = "filters",
                title = "过滤器",
                onAdd = onNewFilter
            ) {
                if (Settings.drawerFilterToday) {
                    DrawerChild(
                        icon = "🗓",
                        label = "今天",
                        count = incompleteCount { t -> t.due != null && com.blackboxbench.reproduction.dayDiff(t.due!!, System.currentTimeMillis()) <= 0 },
                        selected = current.type == "today",
                        onClick = { onSelect(ViewRef.TODAY) }
                    )
                }
                if (Settings.drawerFilterRecent) {
                    DrawerChild(
                        icon = "🕒",
                        label = "最近修改过的",
                        count = null,
                        selected = current.type == "recent",
                        onClick = { onSelect(ViewRef.RECENT) }
                    )
                }
                StateStore.filters.forEach { f ->
                    DrawerChild(
                        icon = f.icon ?: "⧩",
                        label = f.name,
                        count = incompleteCount { matchesFilterPublic(f, it) },
                        selected = current.type == "filter" && current.id == f.id,
                        onClick = { onSelect(ViewRef.filter(f.id)) }
                    )
                }
            }
        }

        if (Settings.drawerTagsEnabled) {
            DrawerGroup(
                key = "tags",
                title = "标签",
                onAdd = onNewTag
            ) {
                StateStore.tags.forEach { tag ->
                    DrawerChild(
                        icon = tag.icon ?: "🏷",
                        label = tag.name,
                        count = incompleteCount { it.tagIds.contains(tag.id) },
                        selected = current.type == "tag" && current.id == tag.id,
                        onClick = { onSelect(ViewRef.tag(tag.id)) }
                    )
                }
            }
        }

        if (Settings.drawerPlacesEnabled) {
            DrawerGroup(
                key = "places",
                title = "地点",
                onAdd = onPickLocation
            ) {
            }
        }

        DrawerGroup(
            key = "lists",
            title = "本地清单",
            onAdd = onNewList
        ) {
            StateStore.lists.forEach { l ->
                DrawerChild(
                    icon = l.icon ?: "≣",
                    label = l.name,
                    count = incompleteCount { it.listId == l.id },
                    selected = current.type == "list" && current.id == l.id,
                    onClick = { onSelect(ViewRef.list(l.id)) }
                )
            }
        }
    }
}

fun matchesFilterPublic(f: com.blackboxbench.reproduction.CustomFilter, t: com.blackboxbench.reproduction.Task): Boolean =
    com.blackboxbench.reproduction.matchesFilter(f, t)

@Composable
private fun DrawerGroup(
    key: String,
    title: String,
    onAdd: () -> Unit,
    children: @Composable () -> Unit
) {
    val openKey = "drawer_open_$key"
    var open = rememberDrawerOpen(openKey)
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { Prefs.putBool(openKey, !open.value) }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                "+",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clickable(onClick = onAdd)
                    .padding(horizontal = 8.dp)
            )
            Icon(
                if (open.value) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (open.value) {
            children()
        }
    }
}

@Composable
private fun rememberDrawerOpen(key: String): androidx.compose.runtime.MutableState<Boolean> {
    val state = androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(Prefs.bool(key, true))
    }
    return state
}

@Composable
private fun DrawerChild(
    icon: String,
    label: String,
    count: Int?,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 32.dp, end = 16.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 16.sp)
        Spacer(Modifier.width(16.dp))
        Text(
            label,
            fontSize = 15.sp,
            color = if (selected) BlueColor else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (count != null) {
            Text(count.toString(), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
