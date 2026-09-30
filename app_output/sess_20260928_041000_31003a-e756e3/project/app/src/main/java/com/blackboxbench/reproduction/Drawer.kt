package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DrawerContent(
    nav: Nav,
    onSelect: (Nav) -> Unit,
    onCollapseToggle: () -> Unit
) {
    var filtersOpen by remember { mutableStateOf(true) }
    var tagsOpen by remember { mutableStateOf(true) }
    var listsOpen by remember { mutableStateOf(true) }

    Box(Modifier.fillMaxHeight().width(300.dp).background(Color.White)) {
        Column(Modifier.fillMaxHeight().verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(24.dp))
            DrawerItem(
                glyph = Glyph.Checklist,
                text = "我的任务",
                count = Repo.tasks.count { !it.isCompleted },
                selected = nav is Nav.MyTasks,
                onClick = { onSelect(Nav.MyTasks) }
            )
            Spacer(Modifier.height(6.dp))
            SectionHeader(
                glyph = Glyph.Funnel,
                text = "过滤器",
                open = filtersOpen,
                count = null,
                onToggle = { filtersOpen = !filtersOpen },
                onAdd = { }
            )
            if (filtersOpen) {
                DrawerItem(
                    glyph = Glyph.Calendar,
                    text = "今天",
                    count = countMatching { t -> t.due != null && !t.due.toLocalDate().isAfter(Repo.now.toLocalDate()) },
                    selected = nav is Nav.Today,
                    onClick = { onSelect(Nav.Today) }
                )
                DrawerItem(
                    glyph = Glyph.History,
                    text = "最近修改过的",
                    count = countMatching { true },
                    selected = nav is Nav.Recent,
                    onClick = { onSelect(Nav.Recent) }
                )
            }
            Spacer(Modifier.height(6.dp))
            SectionHeader(
                glyph = Glyph.Tag,
                text = "标签",
                open = tagsOpen,
                count = null,
                onToggle = { tagsOpen = !tagsOpen },
                onAdd = { }
            )
            if (tagsOpen) {
                Repo.allTags().forEach { tag ->
                    val c = countMatching { it.tags.contains(tag) }
                    DrawerItem(
                        glyph = Glyph.Tag,
                        text = tag,
                        count = c,
                        selected = nav is Nav.Tag && (nav as Nav.Tag).name == tag,
                        onClick = { onSelect(Nav.Tag(tag)) }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            SectionHeader(
                glyph = Glyph.Place,
                text = "地点",
                open = null,
                count = null,
                onToggle = { },
                onAdd = { }
            )
            Spacer(Modifier.height(6.dp))
            SectionHeader(
                glyph = Glyph.Checklist,
                text = "本地清单",
                open = listsOpen,
                count = null,
                onToggle = { listsOpen = !listsOpen },
                onAdd = { }
            )
            if (listsOpen) {
                Repo.lists.forEach { list ->
                    val c = Repo.tasks.sumOf { t ->
                        (if (!t.isCompleted && t.listId == list.id) 1 else 0) +
                            t.subtasks.count { !it.isCompleted && t.listId == list.id }
                    }
                    DrawerItem(
                        glyph = Glyph.List,
                        text = list.name,
                        count = c,
                        tint = list.color?.let { Color(it) },
                        selected = nav is Nav.List && (nav as Nav.List).id == list.id,
                        onClick = { onSelect(Nav.List(list.id)) }
                    )
                }
            }
            Spacer(Modifier.height(80.dp))
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AppColors.Primary)
                    .clickable { },
                contentAlignment = Alignment.Center
            ) { CoreIcon(IconSearch, Color.White, 24.dp) }
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AppColors.Fab)
                    .clickable { },
                contentAlignment = Alignment.Center
            ) { CoreIcon(IconAdd, Color.White, 26.dp) }
        }
    }
}

private fun countMatching(pred: (Task) -> Boolean): Int {
    var n = 0
    Repo.tasks.forEach { t ->
        if (pred(t) && !t.isCompleted) n++
        t.subtasks.forEach { s -> if (pred(t) && !s.isCompleted) n++ }
    }
    return n
}

@Composable
private fun SectionHeader(
    glyph: Glyph,
    text: String,
    open: Boolean?,
    count: Int?,
    onToggle: () -> Unit,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlyphIcon(glyph, AppColors.OnSurface, size = 20.dp)
        Spacer(Modifier.width(20.dp))
        Text(text, fontSize = 15.sp, color = AppColors.OnSurface, modifier = Modifier.weight(1f))
        if (count != null) {
            Text("$count", fontSize = 14.sp, color = AppColors.SecondaryText)
            Spacer(Modifier.width(8.dp))
        }
        if (open != null) {
            Box(Modifier.size(28.dp).clickable { onToggle() }, contentAlignment = Alignment.Center) {
                CoreIcon(if (open) IconUp else IconDown, AppColors.SecondaryText, 20.dp)
            }
        }
        Box(Modifier.size(28.dp).clickable { onAdd() }, contentAlignment = Alignment.Center) {
            CoreIcon(IconAdd, AppColors.SecondaryText, 20.dp)
        }
    }
}

@Composable
private fun DrawerItem(
    glyph: Glyph,
    text: String,
    count: Int,
    selected: Boolean,
    tint: Color? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(if (selected) Color(0xFFE3F2FD) else Color.Transparent)
            .clickable { onClick() }
            .padding(start = 32.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlyphIcon(glyph, tint ?: AppColors.OnSurface, size = 19.dp)
        Spacer(Modifier.width(20.dp))
        Text(
            text,
            fontSize = 15.sp,
            color = AppColors.OnSurface,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        if (count > 0) Text("$count", fontSize = 14.sp, color = AppColors.SecondaryText)
    }
}
