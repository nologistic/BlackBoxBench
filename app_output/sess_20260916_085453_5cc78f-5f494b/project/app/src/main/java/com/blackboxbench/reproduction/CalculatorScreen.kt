package com.blackboxbench.reproduction

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun CalculatorScreen(
    prefs: Prefs,
    onOpenConverter: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit
) {
    var revision by remember { mutableIntStateOf(0) }
    val historyList = remember { mutableStateListOf<HistoryEntry>().apply { addAll(prefs.history) } }
    val engine = remember {
        CalcEngine { expr, res ->
            prefs.addHistory(expr, res)
            historyList.clear()
            historyList.addAll(prefs.history)
        }
    }
    var showHistory by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    fun act(block: () -> Unit) {
        block()
        revision++
    }

    Box(Modifier.fillMaxSize().background(PageBg)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconTap(onClick = { showHistory = true }) { HistoryGlyph(Ink) }
                Spacer(Modifier.width(8.dp))
                IconTap(onClick = onOpenConverter) { ConverterGlyph(Ink) }
                Spacer(Modifier.width(8.dp))
                Box {
                    IconTap(onClick = { showMenu = true }) { OverflowGlyph(Ink) }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("设置", color = Ink, fontSize = 18.sp) },
                            onClick = { showMenu = false; onOpenSettings() }
                        )
                        DropdownMenuItem(
                            text = { Text("关于", color = Ink, fontSize = 18.sp) },
                            onClick = { showMenu = false; onOpenAbout() }
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            revision
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.End
            ) {
                if (engine.exprLine.isNotEmpty()) {
                    Text(
                        engine.exprLine,
                        color = InkSoft,
                        fontSize = 28.sp,
                        textAlign = TextAlign.End
                    )
                    Spacer(Modifier.height(8.dp))
                }
                val text = engine.display()
                Text(
                    text,
                    color = Ink,
                    fontSize = when {
                        text.length > 14 -> 34.sp
                        text.length > 10 -> 44.sp
                        else -> 62.sp
                    },
                    textAlign = TextAlign.End,
                    maxLines = 1
                )
            }

            Spacer(Modifier.weight(0.35f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CalcRow(
                    keys = listOf(
                        Key("%", false),
                        Key("^", false),
                        Key("\u221a", false),
                        Key("\u00f7", false)
                    ),
                    dark = true,
                    onKey = { k ->
                        act {
                            when (k) {
                                "%" -> engine.inputPercent()
                                "^" -> engine.inputOp("^")
                                "\u221a" -> engine.inputSqrt()
                                "\u00f7" -> engine.inputOp("\u00f7")
                            }
                        }
                    }
                )
                CalcRow(
                    keys = listOf(Key("7", true), Key("8", true), Key("9", true), Key("\u00d7", false)),
                    onKey = { k ->
                        act { if (k == "\u00d7") engine.inputOp("\u00d7") else engine.inputDigit(k) }
                    }
                )
                CalcRow(
                    keys = listOf(Key("4", true), Key("5", true), Key("6", true), Key("\u2212", false)),
                    onKey = { k ->
                        act { if (k == "\u2212") engine.inputOp("-") else engine.inputDigit(k) }
                    }
                )
                CalcRow(
                    keys = listOf(Key("1", true), Key("2", true), Key("3", true), Key("+", false)),
                    onKey = { k ->
                        act { if (k == "+") engine.inputOp("+") else engine.inputDigit(k) }
                    }
                )
                CalcRow(
                    keys = listOf(Key("0", true), Key(".", false), Key("C", false), Key("=", false)),
                    onKey = { k ->
                        act {
                            when (k) {
                                "0" -> engine.inputDigit("0")
                                "." -> engine.inputDot()
                                "C" -> engine.backspace()
                                "=" -> engine.equals()
                            }
                        }
                    },
                    onLongKey = { k ->
                        act {
                            when (k) {
                                "C" -> engine.clearAll()
                            }
                        }
                    }
                )
            }
        }
    }

    if (showHistory) {
        HistoryDialog(
            entries = historyList.toList(),
            onClear = {
                prefs.clearHistory()
                historyList.clear()
            },
            onPick = { entry ->
                act { engine.loadResult(entry.result) }
                showHistory = false
            },
            onDismiss = { showHistory = false }
        )
    }
}

data class Key(val label: String, val digit: Boolean)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalcRow(
    keys: List<Key>,
    dark: Boolean = false,
    onKey: (String) -> Unit,
    onLongKey: (String) -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        keys.forEach { key ->
            val bg = if (dark || !key.digit) KeyDark else KeyLight
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1.18f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(bg)
                    .combinedClickable(
                        onClick = { onKey(key.label) },
                        onLongClick = { onLongKey(key.label) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(key.label, color = Ink, fontSize = 34.sp, fontWeight = FontWeight.Normal)
            }
        }
    }
}

@Composable
fun HistoryDialog(
    entries: List<HistoryEntry>,
    onClear: () -> Unit,
    onPick: (HistoryEntry) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFFF2F1FA))
                .padding(20.dp)
        ) {
            Column(Modifier.fillMaxWidth()) {
                Text("历史记录", color = Color(0xFF5A6B8A), fontSize = 26.sp)
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (entries.isEmpty()) {
                        Text("暂无历史记录", color = InkSoft, fontSize = 16.sp)
                    }
                    entries.forEach { entry ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(entry) }
                                .padding(vertical = 10.dp)
                        ) {
                            Text(entry.expression, color = InkSoft, fontSize = 20.sp)
                            Text(entry.result, color = Ink, fontSize = 24.sp)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "清除",
                        color = Color(0xFF415F91),
                        fontSize = 20.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onClear() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                    Text(
                        "确定",
                        color = Color(0xFF415F91),
                        fontSize = 20.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onDismiss() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}