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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BrowserScreen(
    app: AppState,
    onBack: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenNote: (Long) -> Unit
) {
    var overflowOpen by remember { mutableStateOf(false) }
    var deckPickOpen by remember { mutableStateOf(false) }
    var searchMode by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var deckFilter by remember { mutableStateOf<String?>(null) }
    var showTagFilter by remember { mutableStateOf(false) }
    var sortAsc by remember { mutableStateOf(true) }

    val rows = remember(app.cards.toList(), app.notes.toList(), query, deckFilter, sortAsc) {
        app.cards.mapNotNull { c -> app.noteById(c.noteId)?.let { c to it } }
            .filter { (c, n) ->
                val textOk = query.isBlank() ||
                    n.fields.any { it.contains(query, ignoreCase = true) } ||
                    n.tags.any { it.contains(query, ignoreCase = true) }
                val deckOk = deckFilter == null || c.deck == deckFilter ||
                    c.deck.startsWith("${deckFilter}::")
                textOk && deckOk
            }
            .sortedBy { it.first.dueDay }
            .let { if (sortAsc) it else it.reversed() }
    }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Surface(color = AnkiColors.Primary, modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (searchMode) {
                    Box(Modifier.size(44.dp).clickable { searchMode = false; query = "" }, contentAlignment = Alignment.Center) {
                        Text("\u2190", color = Color.White, fontSize = 22.sp)
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 17.sp),
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                    Box(Modifier.size(44.dp).clickable { overflowOpen = true }, contentAlignment = Alignment.Center) {
                        OverflowIcon()
                    }
                } else {
                    Box(Modifier.size(44.dp).clickable { onOpenDrawer() }, contentAlignment = Alignment.Center) {
                        HamburgerIcon()
                    }
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Text(deckFilter ?: "全部牌组", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                        Text("共有 ${rows.size} 张卡片", color = Color.White, fontSize = 12.sp)
                    }
                    Box(Modifier.size(40.dp).clickable { deckPickOpen = true }, contentAlignment = Alignment.Center) {
                        Text("\u25BE", color = Color.White, fontSize = 16.sp)
                    }
                    Box(Modifier.size(44.dp).clickable { }, contentAlignment = Alignment.Center) {
                        Text("+", color = Color.White, fontSize = 24.sp)
                    }
                    Box(Modifier.size(44.dp).clickable { searchMode = true }, contentAlignment = Alignment.Center) {
                        SearchGlyph(Color.White, Modifier.size(20.dp))
                    }
                    Box(Modifier.size(44.dp).clickable { overflowOpen = true }, contentAlignment = Alignment.Center) {
                        OverflowIcon()
                    }
                }
            }
        }

        // Column header
        Surface(color = Color(0xFFF2F2F2), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("排序字段", fontSize = 13.sp, color = AnkiColors.TextPrimary, modifier = Modifier.weight(2.4f))
                Text("卡片模板", fontSize = 13.sp, color = AnkiColors.TextPrimary, modifier = Modifier.weight(1.1f))
                Text("到期", fontSize = 13.sp, color = AnkiColors.TextPrimary, modifier = Modifier.weight(1.1f))
                Text("牌组", fontSize = 13.sp, color = AnkiColors.TextPrimary, modifier = Modifier.weight(1.2f))
            }
        }
        HorizontalDivider(color = AnkiColors.RowDivider)
        LazyColumn(Modifier.weight(1f)) {
            items(rows) { (card, note) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clickable { onOpenNote(note.id) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        note.sortField,
                        fontSize = 13.sp,
                        color = AnkiColors.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(2.4f)
                    )
                    Text(card.template, fontSize = 13.sp, color = AnkiColors.TextPrimary, maxLines = 1, modifier = Modifier.weight(1.1f))
                    Text(dayLabel(card.dueDay), fontSize = 13.sp, color = AnkiColors.TextPrimary, maxLines = 1, modifier = Modifier.weight(1.1f))
                    Text(card.deck, fontSize = 13.sp, color = AnkiColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1.2f))
                }
                HorizontalDivider(color = AnkiColors.RowDivider, thickness = 0.8.dp)
            }
        }

        if (searchMode || deckFilter != null) {
            Surface(color = Color(0xFFF2F2F2), modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("共有 ${rows.size} 张卡片", fontSize = 13.sp)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "搜索全部牌组",
                        fontSize = 13.sp,
                        color = AnkiColors.Primary,
                        modifier = Modifier.clickable { deckFilter = null }
                    )
                }
            }
        }
    }

    Box(Modifier.fillMaxSize().padding(top = 6.dp, end = 8.dp), contentAlignment = Alignment.TopEnd) {
        DropdownMenu(expanded = overflowOpen, onDismissRequest = { overflowOpen = false }) {
            listOf(
                "改变排序方式",
                "筛选已标记卡片",
                "筛选已暂停卡片",
                "按标签筛选",
                "按旗标筛选",
                "撤销更新笔记",
                "预览",
                "全选",
                "选项",
                "创建筛选牌组"
            ).forEach { label ->
                DropdownMenuItem(
                    text = { Text(label, fontSize = 15.sp, color = AnkiColors.TextPrimary) },
                    onClick = {
                        when (label) {
                            "改变排序方式" -> sortAsc = !sortAsc
                            "筛选已标记卡片" -> query = "tag:marked"
                            "筛选已暂停卡片" -> query = "is:suspended"
                            "按标签筛选" -> showTagFilter = true
                        }
                        overflowOpen = false
                    }
                )
            }
        }
    }

    if (deckPickOpen) {
        AlertDialog(
            onDismissRequest = { deckPickOpen = false },
            containerColor = Color.White,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            title = { Text("选择牌组", fontSize = 19.sp) },
            text = {
                Column {
                    listOf<String?>(null).plus(app.decks.map { it.name }).forEach { name ->
                        Row(
                            Modifier.fillMaxWidth().clickable { deckFilter = name; deckPickOpen = false }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(name ?: "所有牌组", fontSize = 15.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { deckPickOpen = false }) { Text("关闭", color = AnkiColors.Primary) }
            }
        )
    }

    if (showTagFilter) {
        var tag by remember { mutableStateOf(app.allTags().firstOrNull()) }
        var scope by remember { mutableStateOf("所有卡片") }
        AlertDialog(
            onDismissRequest = { showTagFilter = false },
            containerColor = Color.White,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            title = {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("选择标签", fontSize = 19.sp, modifier = Modifier.weight(1f))
                    Text("+", fontSize = 22.sp)
                }
            },
            text = {
                Column {
                    app.allTags().forEach { t ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(t, fontSize = 15.sp, modifier = Modifier.weight(1f))
                            Checkbox(checked = tag == t, onCheckedChange = { tag = t })
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    listOf("所有卡片", "新卡牌", "待复习").forEach { s ->
                        Row(
                            Modifier.clickable { scope = s },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = scope == s, onClick = { scope = s })
                            Text(s, fontSize = 15.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (tag != null) query = "(tag:$tag)"
                    showTagFilter = false
                }) { Text("选择", color = AnkiColors.Primary) }
            },
            dismissButton = { TextButton(onClick = { showTagFilter = false }) { Text("取消", color = AnkiColors.Primary) } }
        )
    }
}
