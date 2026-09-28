package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AddNoteScreen(
    app: AppState,
    noteId: Long?,
    initialDeck: String,
    onBack: () -> Unit
) {
    val existing = noteId?.let { app.noteById(it) }
    var noteType by remember { mutableStateOf(existing?.type ?: "问答题") }
    var deck by remember { mutableStateOf(existing?.deck?.ifBlank { null } ?: initialDeck.ifBlank { "默认牌组" }) }
    val fields = remember {
        mutableStateListOf<String>().apply {
            val nt = app.noteTypeByName(existing?.type ?: "问答题")
            val names = nt?.fields ?: listOf("正面", "背面")
            names.forEachIndexed { i, _ -> add(existing?.fields?.getOrNull(i) ?: "") }
        }
    }
    var tags by remember { mutableStateOf(existing?.tags?.joinToString(" ") ?: "") }
    var typeOpen by remember { mutableStateOf(false) }
    var deckOpen by remember { mutableStateOf(false) }
    var showTags by remember { mutableStateOf(false) }

    val templates = (app.noteTypeByName(noteType)?.templates ?: listOf("卡片 1"))

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Surface(color = AnkiColors.Primary, modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(44.dp).clickable { onBack() }, contentAlignment = Alignment.Center) {
                    Text("\u2190", color = Color.White, fontSize = 22.sp)
                }
                Text(
                    if (existing == null) "添加" else "编辑笔记",
                    color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Box(Modifier.size(44.dp).clickable {
                    val names = app.noteTypeByName(noteType)?.fields ?: listOf("正面", "背面")
                    val values = names.mapIndexed { i, _ -> fields.getOrNull(i) ?: "" }
                    val tagList = tags.split(" ", ",").map { it.trim() }.filter { it.isNotEmpty() }
                    if (existing == null) {
                        app.addNote(noteType, deck, values, tagList)
                        fields.indices.forEach { fields[it] = "" }
                        tags = ""
                    } else {
                        app.updateNote(existing.id, values, tagList, deck)
                        onBack()
                    }
                }, contentAlignment = Alignment.Center) {
                    Text("\u2713", color = Color.White, fontSize = 22.sp)
                }
                Box(Modifier.size(44.dp).clickable { }, contentAlignment = Alignment.Center) {
                    EyeGlyph(Color.White, Modifier.size(22.dp))
                }
                Box(Modifier.size(44.dp).clickable { }, contentAlignment = Alignment.Center) {
                    OverflowIcon(modifier = Modifier.size(18.dp))
                }
            }
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            // Note type row
            Row(
                Modifier.fillMaxWidth().clickable { typeOpen = true }.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("笔记模板:", fontSize = 13.sp, color = AnkiColors.TextSecondary)
                Spacer(Modifier.width(10.dp))
                Text(noteType, fontSize = 15.sp, color = AnkiColors.TextPrimary, modifier = Modifier.weight(1f))
                Text("\u25BE", fontSize = 13.sp, color = AnkiColors.TextSecondary)
            }
            Box {
                DropdownMenu(expanded = typeOpen, onDismissRequest = { typeOpen = false }) {
                    app.noteTypes.forEach { nt ->
                        DropdownMenuItem(
                            text = { Text(nt.name, fontSize = 15.sp) },
                            onClick = {
                                noteType = nt.name
                                val names = nt.fields
                                while (fields.size < names.size) fields.add("")
                                typeOpen = false
                            }
                        )
                    }
                }
            }
            // Deck row
            Row(
                Modifier.fillMaxWidth().clickable { deckOpen = true }.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("牌组:", fontSize = 13.sp, color = AnkiColors.TextSecondary)
                Spacer(Modifier.width(10.dp))
                Text(deck, fontSize = 15.sp, color = AnkiColors.TextPrimary, modifier = Modifier.weight(1f))
                Text("\u25BE", fontSize = 13.sp, color = AnkiColors.TextSecondary)
            }
            Box {
                DropdownMenu(expanded = deckOpen, onDismissRequest = { deckOpen = false }) {
                    app.decks.filter { !it.filtered }.forEach { d ->
                        DropdownMenuItem(
                            text = { Text(d.name, fontSize = 15.sp) },
                            onClick = { deck = d.name; deckOpen = false }
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            val fieldNames = app.noteTypeByName(noteType)?.fields ?: listOf("正面", "背面")
            fieldNames.forEachIndexed { index, fieldName ->
                FieldEditor(
                    label = fieldName,
                    value = fields.getOrNull(index) ?: "",
                    onValue = { if (index < fields.size) fields[index] = it }
                )
            }

            Spacer(Modifier.height(16.dp))
            GreyRow("标签:", tags) { showTags = true }
            Spacer(Modifier.height(4.dp))
            GreyRow("卡片:", templates.joinToString("  ")) { }

            Spacer(Modifier.height(16.dp))
            FormattingToolbar(
                onInsert = { snippet ->
                    val current = fields.firstOrNull().orEmpty()
                    if (fields.isNotEmpty()) fields[0] = current + snippet
                }
            )
            Spacer(Modifier.height(120.dp))
        }
    }

    if (showTags) {
        TagsDialog(
            app = app,
            initial = tags,
            onDismiss = { showTags = false },
            onConfirm = { value -> tags = value; showTags = false }
        )
    }
}

@Composable
private fun FieldEditor(label: String, value: String, onValue: (String) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
            Text(label, fontSize = 13.sp, color = if (focused) AnkiColors.Primary else AnkiColors.TextSecondary)
            Spacer(Modifier.weight(1f))
            Text("\u26F2", fontSize = 15.sp, color = AnkiColors.TextSecondary)
            Spacer(Modifier.width(18.dp))
            EyeGlyph(AnkiColors.TextSecondary, Modifier.size(18.dp))
            Spacer(Modifier.width(18.dp))
            Text("\u2303", fontSize = 15.sp, color = AnkiColors.TextSecondary)
        }
        Box(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            if (value.isEmpty()) {
                Text("", fontSize = 16.sp)
            }
            BasicTextField(
                value = value,
                onValueChange = onValue,
                textStyle = TextStyle(fontSize = 16.sp, color = AnkiColors.TextPrimary),
                cursorBrush = SolidColor(AnkiColors.Primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .onFocusChanged { focused = it.isFocused }
            )
        }
        HorizontalDivider(color = if (focused) AnkiColors.Primary else AnkiColors.RowDivider, thickness = 1.dp)
    }
}

@Composable
private fun GreyRow(label: String, value: String, onClick: () -> Unit) {
    Surface(
        color = AnkiColors.FieldBg,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(46.dp)
            .clickable { onClick() }
    ) {
        Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 15.sp, color = AnkiColors.TextPrimary)
            Spacer(Modifier.width(8.dp))
            Text(value, fontSize = 15.sp, color = AnkiColors.TextPrimary)
        }
    }
}

@Composable
private fun FormattingToolbar(onInsert: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("B" to "<b></b>", "I" to "<i></i>", "U" to "<u></u>", "T" to "<s></s>", "Tt" to "", "\u03A3" to "", "+" to "").forEach { (label, snippet) ->
                Box(
                    Modifier.size(44.dp).clickable { onInsert(snippet) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, fontSize = 18.sp, color = AnkiColors.TextPrimary, fontWeight = if (label == "B") FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
        HorizontalDivider(color = AnkiColors.RowDivider)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("\u25A6", "\u263A", "GIF", "\u2399", "\u2699", "\u25D0", "\u266B").forEach { label ->
                Box(Modifier.size(46.dp).clickable { }, contentAlignment = Alignment.Center) {
                    Text(label, fontSize = 17.sp, color = AnkiColors.TextSecondary)
                }
            }
        }
    }
}

@Composable
fun TagsDialog(
    app: AppState,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selected by remember {
        mutableStateOf(initial.split(" ").filter { it.isNotBlank() }.toMutableSet())
    }
    var showAdd by remember { mutableStateOf(false) }
    var newTag by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(12.dp),
        title = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("标签", fontSize = 19.sp, modifier = Modifier.weight(1f))
                Text("+", fontSize = 22.sp, color = AnkiColors.TextSecondary, modifier = Modifier.clickable { showAdd = true })
            }
        },
        text = {
            Column {
                val all = app.allTags()
                if (all.isEmpty()) {
                    Text("你还没有添加任何标签。", fontSize = 15.sp, color = AnkiColors.TextPrimary)
                } else {
                    all.forEach { tag ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(tag, fontSize = 15.sp, modifier = Modifier.weight(1f))
                            Checkbox(
                                checked = tag in selected,
                                onCheckedChange = { c -> if (c) selected.add(tag) else selected.remove(tag) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected.joinToString(" ")) }) {
                Text("确认", color = AnkiColors.Primary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = AnkiColors.Primary) } }
    )

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(12.dp),
            title = { Text("添加标签", fontSize = 19.sp) },
            text = {
                OutlinedTextField(
                    value = newTag,
                    onValueChange = { newTag = it },
                    label = { Text("名称:") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newTag.isNotBlank()) {
                        selected.add(newTag.trim())
                        newTag = ""
                    }
                    showAdd = false
                }) { Text("添加", color = AnkiColors.Primary) }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("取消", color = AnkiColors.Primary) } }
        )
    }
}
