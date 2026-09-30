package com.blackboxbench.reproduction

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val HeaderHeight = 56.dp
private val RowHeight = 56.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DeckListScreen(
    app: AppState,
    onOpenDrawer: () -> Unit,
    onStudy: (String) -> Unit,
    onAdd: (Long?, String) -> Unit,
    onBrowse: () -> Unit,
    onNoteTypes: () -> Unit,
    onSettings: () -> Unit,
    incomingSnackbar: String? = null,
    onSnackbarShown: () -> Unit = {}
) {
    var overflowOpen by remember { mutableStateOf(false) }
    var fabOpen by remember { mutableStateOf(false) }
    var showCreateDeck by remember { mutableStateOf(false) }
    var showCustomStudy by remember { mutableStateOf(false) }
    var customDeck by remember { mutableStateOf<String?>(null) }
    var showFiltered by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }
    var showSyncLogin by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<String?>(null) }
    var contextDeck by remember { mutableStateOf<Deck?>(null) }
    var currentDeck by remember { mutableStateOf("默认牌组") }
    var snackbar by remember { mutableStateOf<String?>(null) }

    androidx.compose.runtime.LaunchedEffect(incomingSnackbar) {
        if (incomingSnackbar != null) {
            snackbar = incomingSnackbar
            onSnackbarShown()
        }
    }

    if (showFiltered) {
        FilteredDeckScreen(
            app = app,
            onBack = { showFiltered = false },
            onCreated = { showFiltered = false }
        )
        return
    }

    Box(Modifier.fillMaxSize().background(Color.White)) {
        Column(Modifier.fillMaxSize()) {
            DeckHeader(
                dueTotal = app.totalDue(),
                onMenu = onOpenDrawer,
                onSync = { showSyncLogin = true },
                onOverflow = { overflowOpen = true }
            )
            Box(Modifier.weight(1f)) {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(app.decks.toList()) { deck ->
                        val (n, l, d) = app.deckCounts(deck.name)
                        DeckRow(
                            deck = deck,
                            new = n,
                            learn = l,
                            due = d,
                            selected = currentDeck == deck.name,
                            onClick = {
                                currentDeck = deck.name
                                if (deck.filtered) {
                                    app.rebuildFilteredDeck(deck)
                                    onStudy(deck.name)
                                } else {
                                    val total = app.deckCounts(deck.name)
                                    if (total.first + total.second + total.third == 0) {
                                        snackbar = "此牌组为空"
                                    } else {
                                        onStudy(deck.name)
                                    }
                                }
                            },
                            onLongClick = { contextDeck = deck }
                        )
                        HorizontalDivider(color = AnkiColors.RowDivider, thickness = 0.8.dp)
                    }
                    item {
                        Spacer(Modifier.height(72.dp))
                    }
                }
                Column(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, end = 90.dp, bottom = 22.dp)
                ) {
                    Text(
                        "今天在 ${app.studySeconds.toInt()} 秒内学习了 ${app.studiedCards} 张卡片" +
                            "（平均每张卡片 ${if (app.studiedCards == 0) "0" else "%.2f".format(app.studySeconds / app.studiedCards)} 秒）",
                        color = AnkiColors.TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // FAB speed dial
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.End
        ) {
            if (fabOpen) {
                SpeedDialItem("获取共享牌组", "\u2913") { fabOpen = false; snackbar = "共享牌组需要网络连接" }
                Spacer(Modifier.height(12.dp))
                SpeedDialItem("创建筛选牌组", "\u25BD") { fabOpen = false; showFiltered = true }
                Spacer(Modifier.height(12.dp))
                SpeedDialItem("创建牌组", "\u25A4") { fabOpen = false; showCreateDeck = true }
                Spacer(Modifier.height(12.dp))
                SpeedDialItem("添加", "\u25A5") { fabOpen = false; onAdd(null, currentDeck) }
                Spacer(Modifier.height(12.dp))
            }
            FloatingActionButton(
                onClick = { fabOpen = !fabOpen },
                containerColor = AnkiColors.Primary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Text(if (fabOpen) "\u25A5" else "+", fontSize = 26.sp, color = Color.White)
            }
        }

        // Overflow menu
        Box(Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 8.dp)) {
            DropdownMenu(expanded = overflowOpen, onDismissRequest = { overflowOpen = false }) {
                OverflowRow("撤销更新笔记") { overflowOpen = false }
                OverflowRow("检查") { overflowOpen = false }
                OverflowRow("创建备份") { overflowOpen = false; snackbar = "备份已创建" }
                OverflowRow("恢复备份") { overflowOpen = false }
                OverflowRow("笔记类型") { overflowOpen = false; onNoteTypes() }
                OverflowRow("导入牌组") { overflowOpen = false }
                OverflowRow("导出") { overflowOpen = false; showExport = true }
            }
        }

        // Long press context menu
        contextDeck?.let { deck ->
            DeckContextMenu(
                app = app,
                deck = deck,
                onDismiss = { contextDeck = null },
                onAdd = { contextDeck = null; onAdd(null, deck.name) },
                onBrowse = { contextDeck = null; onBrowse() },
                onRename = { contextDeck = null },
                onSubdeck = { contextDeck = null; showCreateDeck = true },
                onOptions = { contextDeck = null; snackbar = "牌组选项" },
                onCustom = { contextDeck = null; customDeck = deck.name; showCustomStudy = true },
                onExport = { contextDeck = null; showExport = true },
                onShortcut = { contextDeck = null; snackbar = "已创建快捷方式" },
                onEditDesc = { contextDeck = null },
                onDelete = { contextDeck = null; deleteTarget = deck.name },
                onRebuild = { contextDeck = null; app.rebuildFilteredDeck(deck) },
                onEmpty = { contextDeck = null; app.emptyDeck(deck.name) }
            )
        }

        snackbar?.let { msg ->
            Surface(
                color = Color(0xFF323232),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, end = 16.dp, bottom = 84.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(msg, color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp))
                    Spacer(Modifier.weight(1f))
                    Text(
                        "添加",
                        color = AnkiColors.Primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clickable { snackbar = null; onAdd(null, currentDeck) }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }

    if (showCreateDeck) {
        CreateDeckDialog(
            onDismiss = { showCreateDeck = false },
            onCreate = { name -> app.addDeck(name); showCreateDeck = false }
        )
    }
    if (showCustomStudy) {
        CustomStudyDialog(
            onDismiss = { showCustomStudy = false; customDeck = null },
            onPick = { which ->
                showCustomStudy = false
                if (which == "按卡片状态或标签学习") {
                    snackbar = "没有卡片符合筛选条件。"
                } else {
                    snackbar = "已创建自定义学习会话"
                }
                customDeck = null
            }
        )
    }
    if (showExport) {
        ExportDialog(
            onDismiss = { showExport = false },
            onExport = { showExport = false; snackbar = "导出完成" }
        )
    }
    if (showSyncLogin) {
        ConfirmDialog(
            title = "登录到 AnkiWeb",
            body = "您必须登录到第三方账户以使用云同步服务。您可以在下一步创建一个账户。",
            confirm = "登录",
            dismiss = "取消",
            onConfirm = { showSyncLogin = false; snackbar = "无法连接到 AnkiWeb" },
            onDismiss = { showSyncLogin = false }
        )
    }
    if (showHelp) {
        HelpDialog(onDismiss = { showHelp = false })
    }
    deleteTarget?.let { name ->
        ConfirmDialog(
            title = "删除牌组",
            body = "确定要删除牌组「$name」及其所有卡片吗？",
            confirm = "删除",
            dismiss = "取消",
            onConfirm = { app.deleteDeck(name); deleteTarget = null },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
private fun DeckHeader(
    dueTotal: Int,
    onMenu: () -> Unit,
    onSync: () -> Unit,
    onOverflow: () -> Unit
) {
    Surface(color = AnkiColors.Primary, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(HeaderHeight)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clickable { onMenu() },
                contentAlignment = Alignment.Center
            ) { HamburgerIcon() }
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text("AnkiDroid", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                if (dueTotal > 0) {
                    Text("$dueTotal 张卡片待复习", color = Color.White, fontSize = 12.sp)
                }
            }
            Box(
                Modifier
                    .size(44.dp)
                    .clickable { onSync() },
                contentAlignment = Alignment.Center
            ) {
                Text("\u21BB", color = Color.White, fontSize = 22.sp)
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 4.dp)
                        .size(15.dp)
                        .background(Color(0xFFD32F2F), CircleShape),
                    contentAlignment = Alignment.Center
                ) { Text("1", color = Color.White, fontSize = 9.sp) }
            }
            Box(
                Modifier
                    .size(44.dp)
                    .clickable { onOverflow() },
                contentAlignment = Alignment.Center
            ) { OverflowIcon() }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeckRow(
    deck: Deck,
    new: Int,
    learn: Int,
    due: Int,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(RowHeight)
            .background(if (selected) AnkiColors.RowHighlight else Color.White)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 16.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            deck.name,
            color = if (deck.filtered) AnkiColors.PrimaryDark else AnkiColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = if (deck.filtered) FontWeight.Bold else FontWeight.Normal,
            fontStyle = if (deck.filtered) FontStyle.Italic else FontStyle.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        CountCell(new, AnkiColors.NewCount)
        CountCell(learn, AnkiColors.LearnCount)
        CountCell(due, AnkiColors.DueCount)
    }
}

@Composable
private fun CountCell(value: Int, color: Color) {
    Box(Modifier.width(34.dp), contentAlignment = Alignment.CenterEnd) {
        Text(
            value.toString(),
            color = if (value == 0) AnkiColors.ZeroCount else color,
            fontSize = 15.sp
        )
    }
}

@Composable
private fun SpeedDialItem(label: String, glyph: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(color = Color(0xFF616161), shape = RoundedCornerShape(4.dp)) {
            Text(label, color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
        }
        Spacer(Modifier.width(12.dp))
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = AnkiColors.Primary,
            contentColor = Color.White,
            shape = RoundedCornerShape(8.dp)
        ) { Text(glyph, color = Color.White, fontSize = 18.sp) }
    }
}

@Composable
private fun OverflowRow(label: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, fontSize = 15.sp, color = AnkiColors.TextPrimary) },
        onClick = onClick
    )
}

@Composable
private fun DeckContextMenu(
    app: AppState,
    deck: Deck,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    onBrowse: () -> Unit,
    onRename: () -> Unit,
    onSubdeck: () -> Unit,
    onOptions: () -> Unit,
    onCustom: () -> Unit,
    onExport: () -> Unit,
    onShortcut: () -> Unit,
    onEditDesc: () -> Unit,
    onDelete: () -> Unit,
    onRebuild: () -> Unit,
    onEmpty: () -> Unit
) {
    Surface(
        color = Color(0x66000000),
        modifier = Modifier
            .fillMaxSize()
            .clickable { onDismiss() }
    ) {
        Box(Modifier.fillMaxSize()) {
            Surface(
                color = Color(0xFFE8F1F8),
                shape = RoundedCornerShape(4.dp),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 110.dp, start = 24.dp)
                    .width(260.dp)
            ) {
                Column(Modifier.padding(vertical = 8.dp)) {
                    Text(
                        deck.name,
                        fontSize = 18.sp,
                        color = AnkiColors.TextPrimary,
                        modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 12.dp)
                    )
                    val items: List<Pair<String, () -> Unit>> =
                        if (deck.filtered) listOf(
                            "添加" to onAdd,
                            "浏览卡片" to onBrowse,
                            "重建" to onRebuild,
                            "清空" to onEmpty,
                            "重命名" to onRename,
                            "牌组选项" to onOptions,
                            "导出牌组" to onExport,
                            "创建快捷方式" to onShortcut,
                            "删除牌组" to onDelete
                        ) else listOf(
                            "添加" to onAdd,
                            "浏览卡片" to onBrowse,
                            "重命名" to onRename,
                            "创建子牌组" to onSubdeck,
                            "牌组选项" to onOptions,
                            "自定义学习" to onCustom,
                            "导出牌组" to onExport,
                            "创建快捷方式" to onShortcut,
                            "编辑描述" to onEditDesc,
                            "删除牌组" to onDelete
                        )
                    items.forEach { (label, action) ->
                        Text(
                            label,
                            fontSize = 15.sp,
                            color = AnkiColors.TextPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { action() }
                                .padding(start = 20.dp, top = 12.dp, bottom = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    Surface(color = Color(0x66000000), modifier = Modifier.fillMaxSize().clickable { onDismiss() }) {
        Box(Modifier.fillMaxSize()) {
            Surface(
                color = Color(0xFFE8F1F8),
                shape = RoundedCornerShape(4.dp),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 110.dp, start = 24.dp)
                    .width(260.dp)
            ) {
                Column(Modifier.padding(vertical = 8.dp)) {
                    listOf("使用 AnkiDroid", "获取帮助", "社区", "隐私").forEach { label ->
                        Text(
                            label,
                            fontSize = 15.sp,
                            color = AnkiColors.TextPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onDismiss() }
                                .padding(start = 20.dp, top = 12.dp, bottom = 12.dp)
                        )
                    }
                }
            }
        }
    }
}
