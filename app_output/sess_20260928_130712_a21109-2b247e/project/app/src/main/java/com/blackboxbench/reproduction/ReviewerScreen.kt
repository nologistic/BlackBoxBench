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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.system.measureTimeMillis

@Composable
fun ReviewerScreen(
    app: AppState,
    deck: String,
    onBack: () -> Unit,
    onEditNote: (Long) -> Unit,
    onOpenDrawer: () -> Unit = {},
    onComplete: () -> Unit = onBack
) {
    val queue = remember(deck) { mutableStateListOf<Long>().apply { addAll(app.studyQueue(deck)) } }
    var showAnswer by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var showTags by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(queue.isEmpty()) }
    var cardStart by remember { mutableStateOf(System.currentTimeMillis()) }

    if (finished || queue.isEmpty()) {
        LaunchedEffect(Unit) { onComplete() }
        return
    }

    val card = app.cardById(queue.first())
    if (card == null) {
        LaunchedEffect(queue.first()) { queue.removeAt(0) }
        return
    }
    val note = app.noteById(card.noteId)
    val newCount = queue.mapNotNull { app.cardById(it) }.count { it.state == CState.NEW }
    val learnCount = queue.mapNotNull { app.cardById(it) }.count { it.state == CState.LEARNING }
    val dueCount = queue.mapNotNull { app.cardById(it) }.count { it.state == CState.REVIEW }

    LaunchedEffect(card.id) { cardStart = System.currentTimeMillis() }

    fun advance(stays: Boolean) {
        val id = queue.removeAt(0)
        if (stays) queue.add(id)
        showAnswer = false
        if (queue.isEmpty()) finished = true
    }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Surface(color = AnkiColors.Primary, modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(44.dp).clickable { onOpenDrawer() }, contentAlignment = Alignment.Center) {
                    HamburgerIcon()
                }
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(44.dp).clickable { }, contentAlignment = Alignment.Center) {
                    Text("\u21B6", color = Color.White, fontSize = 22.sp)
                }
                Box(Modifier.size(44.dp).clickable { }, contentAlignment = Alignment.Center) {
                    Text("\u2691", color = Color.White, fontSize = 20.sp)
                }
                Box(Modifier.size(44.dp).clickable { menuOpen = true }, contentAlignment = Alignment.Center) {
                    OverflowIcon()
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(
                newCount to AnkiColors.NewCount,
                learnCount to AnkiColors.LearnCount,
                dueCount to AnkiColors.DueCount
            ).forEach { (value, color) ->
                Text(value.toString(), color = color, fontSize = 15.sp, modifier = Modifier.padding(end = 10.dp))
            }
            Spacer(Modifier.weight(1f))
            StarGlyph(filled = card.marked)
        }

        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))
            Text(
                note?.fields?.getOrNull(0).orEmpty(),
                fontSize = 18.sp,
                color = AnkiColors.TextPrimary,
                textAlign = TextAlign.Center
            )
            if (showAnswer) {
                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = AnkiColors.RowDivider)
                Spacer(Modifier.height(20.dp))
                Text(
                    note?.fields?.getOrNull(1).orEmpty(),
                    fontSize = 18.sp,
                    color = AnkiColors.TextPrimary,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (!showAnswer) {
            Surface(
                color = AnkiColors.AnswerBar,
                modifier = Modifier.fillMaxWidth().height(56.dp).clickable { showAnswer = true }
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("显示答案", color = Color.White, fontSize = 17.sp)
                }
            }
        } else {
            Row(Modifier.fillMaxWidth().height(58.dp)) {
                RatingButton("重来", app.intervalLabel(card, 0), AnkiColors.Again, Modifier.weight(1f)) {
                    val elapsed = (System.currentTimeMillis() - cardStart) / 1000.0
                    app.bumpStudy(elapsed)
                    app.bumpStudiedCards(1)
                    advance(app.answer(card.id, 0))
                }
                RatingButton("困难", app.intervalLabel(card, 1), AnkiColors.Hard, Modifier.weight(1f)) {
                    val elapsed = (System.currentTimeMillis() - cardStart) / 1000.0
                    app.bumpStudy(elapsed)
                    app.bumpStudiedCards(1)
                    advance(app.answer(card.id, 1))
                }
                RatingButton("良好", app.intervalLabel(card, 2), AnkiColors.Good, Modifier.weight(1f)) {
                    val elapsed = (System.currentTimeMillis() - cardStart) / 1000.0
                    app.bumpStudy(elapsed)
                    app.bumpStudiedCards(1)
                    advance(app.answer(card.id, 2))
                }
                RatingButton("简单", app.intervalLabel(card, 3), AnkiColors.Easy, Modifier.weight(1f)) {
                    val elapsed = (System.currentTimeMillis() - cardStart) / 1000.0
                    app.bumpStudy(elapsed)
                    app.bumpStudiedCards(1)
                    advance(app.answer(card.id, 3))
                }
            }
        }
    }

    Box(Modifier.fillMaxSize().padding(top = 6.dp, end = 8.dp), contentAlignment = Alignment.TopEnd) {
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            ReviewerMenuItem("重做") { menuOpen = false }
            ReviewerMenuItem("启用白板") { menuOpen = false }
            ReviewerMenuItem("编辑笔记") { menuOpen = false; onEditNote(card.noteId) }
            ReviewerMenuItem("编辑标签") { menuOpen = false; showTags = true }
            ReviewerMenuItem("搁置卡片") { menuOpen = false; app.setCardBuried(card.id, true); advance(false) }
            ReviewerMenuItem("暂停卡片") { menuOpen = false; app.setCardSuspended(card.id, true); advance(false) }
            ReviewerMenuItem("删除笔记") { menuOpen = false; app.deleteNote(card.noteId); advance(false) }
            ReviewerMenuItem("标记笔记") { menuOpen = false; app.setCardMarked(card.id, !card.marked) }
            ReviewerMenuItem("重新安排") { menuOpen = false }
            ReviewerMenuItem("重播媒体") { menuOpen = false }
            ReviewerMenuItem("启用语音播放") { menuOpen = false }
            ReviewerMenuItem("牌组选项") { menuOpen = false }
        }
    }

    if (showTags) {
        TagsDialog(
            app = app,
            initial = note?.tags?.joinToString(" ") ?: "",
            onDismiss = { showTags = false },
            onConfirm = { value ->
                note?.let { n ->
                    value.split(" ").filter { it.isNotBlank() }.forEach { app.tagNote(n.id, it) }
                }
                showTags = false
            }
        )
    }
}

@Composable
private fun RatingButton(label: String, interval: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .fillMaxSize()
            .background(color)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(interval, color = Color.White, fontSize = 11.sp)
            Text(label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ReviewerMenuItem(label: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, fontSize = 15.sp, color = AnkiColors.TextPrimary) },
        onClick = onClick
    )
}
