package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun EditorScreen(fileName: String, onBack: () -> Unit) {
    var text by remember { mutableStateOf(Store.read(fileName)) }
    var preview by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var showFileSettings by remember { mutableStateOf(false) }
    var showToc by remember { mutableStateOf(false) }
    var showShare by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var speedRead by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf<String?>(null) }

    val undoStack = remember { ArrayDeque<String>() }
    val redoStack = remember { ArrayDeque<String>() }

    fun save() = Store.write(fileName, text)

    fun onEdit(newValue: String) {
        undoStack.addLast(text)
        redoStack.clear()
        text = autoFormatAppend(text, newValue)
    }

    Column(Modifier.fillMaxSize().background(EditorBg)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NavyBar)
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Markor",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable {
                        save()
                        onBack()
                    }
                    .padding(horizontal = 6.dp),
            )
            Spacer(Modifier.weight(1f))
            if (!preview) {
                TopIcon("↶") {
                    if (undoStack.isNotEmpty()) {
                        redoStack.addLast(text)
                        text = undoStack.removeLast()
                    }
                }
                TopIcon("↷") {
                    if (redoStack.isNotEmpty()) {
                        undoStack.addLast(text)
                        text = redoStack.removeLast()
                    }
                }
                TopIcon("💾") { save() }
            } else {
                TopIcon("✎") { preview = false }
            }
            TopIcon(if (preview) "👁" else "👁") { preview = !preview; if (preview) save() }
            TopIcon("🔍") { searchQuery = "" }
            Box {
                TopIcon("⋮") { menu = true }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("文件设置") }, onClick = { menu = false; showFileSettings = true })
                    DropdownMenuItem(text = { Text("目录") }, onClick = { menu = false; showToc = true })
                    DropdownMenuItem(text = { Text("Speed Read") }, onClick = { menu = false; speedRead = true })
                    DropdownMenuItem(text = { Text("分享") }, onClick = { menu = false; showShare = true })
                    DropdownMenuItem(text = { Text("信息") }, onClick = { menu = false; showInfo = true })
                }
            }
        }

        Box(Modifier.weight(1f)) {
            if (preview) {
                MarkdownPreview(text)
            } else {
                BasicTextField(
                    value = text,
                    onValueChange = { onEdit(it) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Ascii,
                        autoCorrectEnabled = false,
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    textStyle = TextStyle(fontSize = 16.sp, color = Color(0xFF222222)),
                )
            }
        }

        if (!preview) {
            EditorFormatBar(text = text, onChange = { onEdit(it) })
        } else {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFECECEC))
                    .padding(vertical = 10.dp),
            ) {
                BarIcon("☰") {}
                BarIcon("🔤") {}
                BarIcon("⋯") {}
            }
        }
    }

    searchQuery?.let { q ->
        TocDialog(
            initialQuery = q,
            onDismiss = { searchQuery = null },
        )
    }
    if (showFileSettings) FileSettingsDialog(onDismiss = { showFileSettings = false })
    if (showToc) TocDialog(initialQuery = "", onDismiss = { showToc = false })
    if (showShare) ShareDialog(onDismiss = { showShare = false })
    if (showInfo) FileInfoDialog(name = fileName, onDismiss = { showInfo = false }, onFavChanged = {})
    if (speedRead) SpeedReadOverlay(text = text, onClose = { speedRead = false })
}

@Composable
fun EditorFormatBar(text: String, onChange: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFFECECEC))
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
    ) {
        BarIcon("☷") {}
        BarIcon("↻") {}
        BarIcon("🕘") {
            onChange(text + "2026-09-22 ")
        }
        BarIcon("⌨") {}
        BarIcon("☰") { onChange(text + "\n- ") }
        BarIcon("<>") { onChange(text + "`code`") }
        BarIcon("B") { onChange(text + "**bold**") }
        BarIcon("H2") { onChange(text + "\n## ") }
        BarIcon("H1") { onChange(text + "\n# ") }
        BarIcon("H3") { onChange(text + "\n### ") }
        BarIcon("aA") {}
    }
}

@Composable
fun BarIcon(glyph: String, onClick: () -> Unit) {
    Text(
        glyph,
        color = Color(0xFF333333),
        fontSize = 18.sp,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 4.dp),
    )
}

@Composable
fun MarkdownPreview(text: String) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        text.lines().forEach { line ->
            when {
                line.startsWith("# #") || line.startsWith("# ") -> Text(
                    line.removePrefix("# ").removePrefix("# "),
                    fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF222222),
                )
                line.startsWith("》") -> Row {
                    Box(Modifier.width(4.dp).height(20.dp).background(Color.LightGray))
                    Spacer(Modifier.width(8.dp))
                    Text(line.removePrefix("》").trim(), color = Color.Gray, fontSize = 15.sp)
                }
                line.startsWith("— ") || line.startsWith("- ") -> Text(
                    "• " + line.drop(2),
                    fontSize = 15.sp, color = Color(0xFF222222),
                )
                else -> Text(line, fontSize = 15.sp, color = Color(0xFF222222))
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun FileSettingsDialog(onDismiss: () -> Unit) {
    var wrap by remember { mutableStateOf(true) }
    var lineNumbers by remember { mutableStateOf(false) }
    var highlight by remember { mutableStateOf(true) }
    var autoFmt by remember { mutableStateOf(true) }
    var fontMenu by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("文件设置") },
        text = {
            Column {
                SettingsCheck("自动换行", wrap) { wrap = it }
                SettingsCheck("行号", lineNumbers) { lineNumbers = it }
                SettingsCheck("语法高亮", highlight) { highlight = it }
                SettingsCheck("自动格式", autoFmt) { autoFmt = it }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { fontMenu = true }
                        .padding(vertical = 8.dp),
                ) {
                    Text("字号", modifier = Modifier.weight(1f))
                    if (fontMenu) {
                        DropdownMenu(expanded = true, onDismissRequest = { fontMenu = false }) {
                            listOf("小", "中", "大").forEach {
                                DropdownMenuItem(text = { Text(it) }, onClick = { fontMenu = false })
                            }
                        }
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { }
                        .padding(vertical = 8.dp),
                ) { Text("格式", modifier = Modifier.weight(1f)) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("确定", color = MarkorRed) }
        },
    )
}

@Composable
private fun SettingsCheck(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) },
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Checkbox(
            checked = checked, onCheckedChange = onChange,
            colors = CheckboxDefaults.colors(checkedColor = MarkorRed),
        )
    }
}

@Composable
private fun TocDialog(initialQuery: String, onDismiss: () -> Unit) {
    var q by remember { mutableStateOf(initialQuery) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("目录") },
        text = {
            Column {
                androidx.compose.material3.TextField(
                    value = q, onValueChange = { q = it },
                    label = { Text("搜索") }, singleLine = true,
                )
                Spacer(Modifier.height(6.dp))
                Text("过滤器", fontSize = 13.sp, color = Color.Gray)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = MarkorRed) }
        },
    )
}

@Composable
private fun ShareDialog(onDismiss: () -> Unit) {
    val options = listOf(
        "URL/路径", "纯文本", "打印/PDF", "文件", "HTML",
        "HTML显示为纯文本", "图片", "Screenshot", "日历",
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("分享") },
        text = {
            LazyColumn {
                items(options, key = { it }) { opt ->
                    Text(
                        opt,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onDismiss)
                            .padding(vertical = 10.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = MarkorRed) }
        },
    )
}

@Composable
private fun SpeedReadOverlay(text: String, onClose: () -> Unit) {
    val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(words.size) {
        while (true) {
            delay(300)
            if (words.isNotEmpty()) index = (index + 1) % words.size
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF101010))
            .clickable { index = (index + 100).coerceAtMost((words.size - 1).coerceAtLeast(0)) },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "+100 WORDS CLICK / -100 LONG CLICK SWIPE DOWN",
                color = Color.Gray, fontSize = 12.sp,
                modifier = Modifier.padding(16.dp),
            )
            Spacer(Modifier.weight(1f))
            Text(
                words.getOrElse(index) { "" },
                color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onClose) { Text("取消", color = MarkorRed, fontSize = 16.sp) }
        }
    }
}
