package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppDrawer(
    current: ViewRef,
    onSelectView: (ViewRef) -> Unit,
    onAddFilter: () -> Unit,
    onAddTag: () -> Unit,
    onAddPlace: () -> Unit,
    onAddList: () -> Unit,
    closeDrawer: () -> Unit
) {
    Store.version.value
    var filtersExpanded by remember { mutableStateOf(true) }
    var listsExpanded by remember { mutableStateOf(true) }
    ModalDrawerSheet(
        modifier = Modifier.width(340.dp),
        drawerContainerColor = AppColors.DrawerBg
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
            DrawerRow(
                icon = { Icon(Icons.Filled.DateRange, null, tint = AppColors.Subtle) },
                label = "我的任务",
                count = Store.countFor(ViewRef.MyTasks),
                selected = current is ViewRef.MyTasks
            ) { onSelectView(ViewRef.MyTasks) }

            if (Store.boolSetting("nav_filters_enabled", true)) {
                Spacer(Modifier.height(10.dp))
                DrawerCard {
                    DrawerGroupHeader(
                        icon = { FilterIcon(AppColors.Subtle) },
                        label = "过滤器",
                        expanded = filtersExpanded,
                        onToggle = { filtersExpanded = !filtersExpanded },
                        onAdd = onAddFilter
                    )
                    if (filtersExpanded) {
                        Store.filters.forEach { f ->
                            when (f.builtin) {
                                "today" -> if (Store.boolSetting("nav_filter_today", true)) {
                                    DrawerRow(
                                        icon = { Icon(Icons.Filled.DateRange, null, tint = AppColors.Subtle) },
                                        label = "今天", count = Store.countFor(ViewRef.BuiltinFilter("today")),
                                        selected = current is ViewRef.BuiltinFilter && current.kind == "today"
                                    ) { onSelectView(ViewRef.BuiltinFilter("today")) }
                                }
                                "recent" -> if (Store.boolSetting("nav_filter_recent", true)) {
                                    DrawerRow(
                                        icon = { HistoryIcon(AppColors.Subtle) },
                                        label = "最近修改过的", count = Store.countFor(ViewRef.BuiltinFilter("recent")),
                                        selected = current is ViewRef.BuiltinFilter && current.kind == "recent"
                                    ) { onSelectView(ViewRef.BuiltinFilter("recent")) }
                                }
                                else -> {
                                    val c = if (f.color != -1) Color(f.color) else AppColors.Red
                                    DrawerRow(
                                        icon = {
                                            val iv = iconByKey(f.icon)
                                            if (iv != null) Icon(iv, null, tint = c) else AlarmIcon(c)
                                        },
                                        label = f.name, count = Store.countFor(ViewRef.CustomFilter(f.id)),
                                        selected = current is ViewRef.CustomFilter && current.id == f.id
                                    ) { onSelectView(ViewRef.CustomFilter(f.id)) }
                                }
                            }
                        }
                    }
                }
            }

            if (Store.boolSetting("nav_tags_enabled", true)) {
                Spacer(Modifier.height(10.dp))
                DrawerCard {
                    DrawerGroupHeader(
                        icon = { TagIcon(AppColors.Subtle) },
                        label = "标签",
                        expanded = null,
                        onToggle = {},
                        onAdd = onAddTag
                    )
                    Store.tags.forEach { tag ->
                        val count = Store.countFor(ViewRef.TagView(tag.id))
                        val hideUnused = Store.boolSetting("nav_hide_unused_tags", false)
                        if (!hideUnused || count > 0) {
                            DrawerRow(
                                icon = { TagIcon(if (tag.color != -1) Color(tag.color) else AppColors.Blue) },
                                label = tag.name, count = count,
                                selected = current is ViewRef.TagView && current.id == tag.id
                            ) { onSelectView(ViewRef.TagView(tag.id)) }
                        }
                    }
                }
            }

            if (Store.boolSetting("nav_places_enabled", true)) {
                Spacer(Modifier.height(10.dp))
                DrawerCard {
                    DrawerGroupHeader(
                        icon = { Icon(Icons.Filled.Place, null, tint = AppColors.Subtle) },
                        label = "地点",
                        expanded = null,
                        onToggle = {},
                        onAdd = onAddPlace
                    )
                    Store.places.forEach { p ->
                        val count = Store.countFor(ViewRef.PlaceView(p.id))
                        val hideUnused = Store.boolSetting("nav_hide_unused_places", false)
                        if (!hideUnused || count > 0) {
                            DrawerRow(
                                icon = { Icon(Icons.Filled.Place, null, tint = AppColors.Subtle) },
                                label = p.name, count = count,
                                selected = current is ViewRef.PlaceView && current.id == p.id
                            ) { onSelectView(ViewRef.PlaceView(p.id)) }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            DrawerCard {
                DrawerGroupHeader(
                    icon = { CloudOffIcon(AppColors.Subtle) },
                    label = "本地清单",
                    expanded = listsExpanded,
                    onToggle = { listsExpanded = !listsExpanded },
                    onAdd = onAddList
                )
                if (listsExpanded) {
                    Store.lists.forEach { l ->
                        DrawerRow(
                            icon = { Icon(Icons.Filled.List, null, tint = AppColors.Subtle) },
                            label = l.name, count = Store.countFor(ViewRef.ListView(l.id)),
                            selected = current is ViewRef.ListView && current.id == l.id
                        ) { onSelectView(ViewRef.ListView(l.id)) }
                    }
                }
            }

            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
                FloatingActionButton(
                    onClick = closeDrawer,
                    containerColor = AppColors.DarkBlue,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.size(56.dp)
                ) { Icon(Icons.Filled.Search, null, tint = Color.White) }
            }
        }
    }
}

@Composable
private fun DrawerCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).padding(vertical = 6.dp),
        content = content
    )
}

@Composable
private fun DrawerGroupHeader(
    icon: @Composable () -> Unit,
    label: String,
    expanded: Boolean?,
    onToggle: () -> Unit,
    onAdd: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(Modifier.width(14.dp))
        Text(label, fontSize = 16.sp, modifier = Modifier.weight(1f))
        if (expanded != null) {
            Icon(
                if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                null, tint = AppColors.Subtle
            )
            Spacer(Modifier.width(8.dp))
        }
        IconButton(onClick = onAdd, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.Add, null, tint = AppColors.Subtle)
        }
    }
}

@Composable
private fun DrawerRow(
    icon: @Composable () -> Unit,
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color(0xFFE8EAF6) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(Modifier.width(14.dp))
        Text(label, fontSize = 16.sp, modifier = Modifier.weight(1f))
        if (count > 0) Text("$count", color = AppColors.Subtle, fontSize = 14.sp)
    }
}
