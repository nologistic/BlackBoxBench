package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

private fun lineStartOf(text: String, cursor: Int): Int {
    val index = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0))
    return if (index < 0) 0 else index + 1
}

private fun lineEndOf(text: String, cursor: Int): Int {
    val index = text.indexOf('\n', cursor)
    return if (index < 0) text.length else index
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NoteScreen(
    state: AppState,
    file: File,
    showBottomBar: Boolean,
    selectedTab: String,
    onSelectTab: (String) -> Unit,
    onBack: () -> Unit,
    onOpenBrowser: () -> Unit,
) {
    val c = LocalMarkorColors.current
    val prefs = state.prefs
    val mode = remember(file) { state.modeOf(file) }
    val initial = remember(file) { state.store.read(file) }

    var value by remember(file) { mutableStateOf(TextFieldValue(initial)) }
    var savedText by remember(file) { mutableStateOf(initial) }
    var preview by remember(file) { mutableStateOf(prefs.bool("preferredView", false)) }
    var menuPage by remember { mutableStateOf<String?>(null) }
    var showFind by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var matchIndex by remember { mutableStateOf(0) }
    var showFileSettings by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var showPriority by remember { mutableStateOf(false) }
    var showClearDone by remember { mutableStateOf(false) }
    var showHtmlSource by remember { mutableStateOf(false) }
    var speedRead by remember { mutableStateOf(false) }

    val undoStack = remember(file) { mutableStateListOf<TextFieldValue>() }
    val redoStack = remember(file) { mutableStateListOf<TextFieldValue>() }
    val highlight = prefs.bool("editorHighlight", true)
    val fontSize = prefs.int("editorFontSize", 15)
    val lineNumbers = prefs.bool("editorLineNumbers", false)
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    val dirty = value.text != savedText
    val matches = remember(value.text, query) {
        if (query.isBlank()) emptyList()
        else Regex(Regex.escape(query), RegexOption.IGNORE_CASE).findAll(value.text).map { it.range }.toList()
    }

    fun commit(newValue: TextFieldValue, pushUndo: Boolean = true) {
        if (pushUndo) {
            undoStack.add(value)
            if (undoStack.size > 120) undoStack.removeAt(0)
            redoStack.clear()
        }
        value = newValue
    }

    fun save() {
        state.store.write(file, value.text)
        savedText = value.text
        state.toast("已保存 ${file.name}")
    }

    DisposableEffect(file) {
        onDispose { runCatching { state.store.write(file, value.text) } }
    }

    // Auto-save: the original Markor persists the note while typing.
    LaunchedEffect(file, value.text) {
        if (value.text != savedText) {
            kotlinx.coroutines.delay(600)
            runCatching { state.store.write(file, value.text) }
            savedText = value.text
        }
    }

    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    if (!preview) {
        LaunchedEffect(file) { runCatching { focusRequester.requestFocus() } }
    }

    Column(modifier = Modifier.fillMaxSize().background(c.editorBackground).imePadding()) {
        NoteTopBar(
            title = if (showFind) null else file.nameWithoutExtension,
            preview = preview,
            dirty = dirty,
            showFind = showFind,
            query = query,
            matchCount = matches.size,
            matchIndex = matchIndex,
            onQueryChange = { query = it; matchIndex = 0 },
            onCloseFind = { showFind = false; query = "" },
            onPrevMatch = { if (matches.isNotEmpty()) matchIndex = (matchIndex - 1 + matches.size) % matches.size },
            onNextMatch = { if (matches.isNotEmpty()) matchIndex = (matchIndex + 1) % matches.size },
            onUndo = { if (undoStack.isNotEmpty()) { redoStack.add(value); value = undoStack.removeAt(undoStack.lastIndex) } },
            onRedo = { if (redoStack.isNotEmpty()) { undoStack.add(value); value = redoStack.removeAt(redoStack.lastIndex) } },
            onSave = { save() },
            onPreview = { preview = !preview },
            onSearch = { showFind = !showFind },
            onOverflow = { menuPage = if (menuPage == null) "root" else null },
            onBack = { state.store.write(file, value.text); onBack() },
        )

        Box(modifier = Modifier.weight(1f)) {
            if (preview) {
                MarkdownView(text = value.text, query = if (showFind) query else "", current = matchIndex, fontSize = fontSize)
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    if (lineNumbers) {
                        Column(modifier = Modifier.padding(end = 8.dp)) {
                            value.text.split('\n').forEachIndexed { index, _ ->
                                Text(
                                    text = "${index + 1}",
                                    color = c.textSecondary,
                                    fontSize = fontSize.sp,
                                    lineHeight = (fontSize + 7).sp,
                                )
                            }
                        }
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = { new ->
                            val old = value
                            val lineBreaks = new.text.count { it == '\n' } - old.text.count { it == '\n' }
                            commit(new)
                            if (mode != NoteMode.PLAIN && lineBreaks == 1) {
                                val cursor = new.selection.end
                                if (cursor > 0 && new.text.getOrNull(cursor - 1) == '\n') {
                                    val start = lineStartOf(new.text, cursor - 1)
                                    val previous = new.text.substring(start, cursor - 1)
                                    val marker = Regex("^(\\s*)(\\d+)\\.\\s(.*)$").find(previous)
                                    if (marker != null) {
                                        val content = marker.groupValues[3]
                                        if (content.isBlank()) {
                                            val replaced = new.text.substring(0, start) + new.text.substring(cursor)
                                            value = TextFieldValue(replaced, TextRange(start))
                                        } else {
                                            val next = marker.groupValues[1] + (marker.groupValues[2].toInt() + 1) + ". "
                                            val replaced = new.text.substring(0, cursor) + next + new.text.substring(cursor)
                                            value = TextFieldValue(replaced, TextRange(cursor + next.length))
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        textStyle = TextStyle(color = c.textPrimary, fontSize = fontSize.sp, lineHeight = (fontSize + 7).sp),
                        cursorBrush = SolidColor(c.accent),
                        visualTransformation = MarkdownHighlight(c.marker, c.accent, highlight),
                    )
                }
            }
        }

        if (preview) {
            ViewerBottomBar(
                onRefresh = { value = TextFieldValue(state.store.read(file)); savedText = value.text; state.toast("已刷新") },
                onShare = { state.shareText(file, value.text) },
                onExport = {
                    val target = state.exportPng(file, value.text)
                    state.toast("已导出 ${target.name}")
                },
            )
        } else {
            FormatBar(
                mode = mode,
                onOrderedList = {
                    val text = value.text
                    val cursor = value.selection.end
                    val start = lineStartOf(text, cursor)
                    val end = lineEndOf(text, cursor)
                    val line = text.substring(start, end)
                    val indent = line.takeWhile { it.isWhitespace() }
                    val body = line.trimStart()
                    val existing = Regex("^(\\d+)\\.\\s").find(body)
                    val newBody = if (existing != null) {
                        (existing.groupValues[1].toInt() + 1).toString() + ". " + body.substring(existing.value.length)
                    } else {
                        "1. $body"
                    }
                    val replaced = text.substring(0, start) + indent + newBody + text.substring(end)
                    commit(TextFieldValue(replaced, TextRange(start + indent.length + newBody.length)))
                },
                onClock = {
                    val cursor = value.selection.end
                    val stamp = Store.audit(System.currentTimeMillis())
                    val replaced = value.text.substring(0, cursor) + stamp + value.text.substring(cursor)
                    commit(TextFieldValue(replaced, TextRange(cursor + stamp.length)))
                },
                onKeyboard = { keyboard?.hide() },
                onIndent = {
                    val text = value.text
                    val start = lineStartOf(text, value.selection.end)
                    val replaced = text.substring(0, start) + "  " + text.substring(start)
                    commit(TextFieldValue(replaced, TextRange(value.selection.end + 2)))
                },
                onCode = {
                    val selection = value.selection
                    val text = value.text
                    val selected = text.substring(selection.min, selection.max)
                    val wrapped = if (selected.isEmpty()) "``" else "`$selected`"
                    val replaced = text.substring(0, selection.min) + wrapped + text.substring(selection.max)
                    val caret = if (selected.isEmpty()) selection.min + 1 else selection.min + wrapped.length
                    commit(TextFieldValue(replaced, TextRange(caret)))
                },
                onBold = {
                    val selection = value.selection
                    val text = value.text
                    val selected = text.substring(selection.min, selection.max)
                    val wrapped = if (selected.isEmpty()) "****" else "**$selected**"
                    val replaced = text.substring(0, selection.min) + wrapped + text.substring(selection.max)
                    val caret = if (selected.isEmpty()) selection.min + 2 else selection.min + wrapped.length
                    commit(TextFieldValue(replaced, TextRange(caret)))
                },
                onHeading = { level ->
                    val text = value.text
                    val start = lineStartOf(text, value.selection.end)
                    val end = lineEndOf(text, value.selection.end)
                    val line = text.substring(start, end)
                    val indent = line.takeWhile { it.isWhitespace() }
                    val prefix = "#".repeat(level) + " "
                    val body = line.trimStart().removePrefix(prefix)
                    val newLine = indent + prefix + body
                    commit(TextFieldValue(text.substring(0, start) + newLine + text.substring(end), TextRange(start + newLine.length)))
                },
                onSortTasks = {
                    val lines = value.text.split('\n')
                    val sorted = lines.sortedWith(compareBy({ it.trimStart().length }, { it.lowercase() }))
                    commit(TextFieldValue(sorted.joinToString("\n"), TextRange(0)))
                    state.toast("任务已按字母排序")
                },
                onPriority = { showPriority = true },
                onUndoTask = { if (undoStack.isNotEmpty()) { redoStack.add(value); value = undoStack.removeAt(undoStack.lastIndex) } },
                onMarkDone = {
                    val text = value.text
                    val start = lineStartOf(text, value.selection.end)
                    val end = lineEndOf(text, value.selection.end)
                    val line = text.substring(start, end)
                    val indent = line.takeWhile { it.isWhitespace() }
                    val body = line.trimStart()
                    val doneRegex = Regex("^x\\s+\\d{4}-\\d{2}-\\d{2}\\s+")
                    val newLine = if (doneRegex.containsMatchIn(body)) {
                        indent + body.replaceFirst(doneRegex, "")
                    } else {
                        indent + "x " + Store.today() + " " + body
                    }
                    commit(TextFieldValue(text.substring(0, start) + newLine + text.substring(end), TextRange(start + newLine.length)))
                },
                onDueDate = {
                    val text = value.text
                    val end = lineEndOf(text, value.selection.end)
                    val suffix = " due:" + Store.today()
                    commit(TextFieldValue(text.substring(0, end) + suffix + text.substring(end), TextRange(end + suffix.length)))
                },
                onCase = {
                    val text = value.text
                    val start = lineStartOf(text, value.selection.end)
                    val end = lineEndOf(text, value.selection.end)
                    val line = text.substring(start, end)
                    val toggled = if (line.any { it.isUpperCase() }) line.lowercase() else line.uppercase()
                    commit(TextFieldValue(text.substring(0, start) + toggled + text.substring(end), TextRange(start + toggled.length)))
                },
                onClearDone = { showClearDone = true },
            )
        }

        if (showBottomBar && !imeVisible) {
            AppBottomBar(selected = selectedTab, onSelect = onSelectTab)
        }
    }

    DropdownMenu(expanded = menuPage != null, onDismissRequest = { menuPage = null }) {
        when (menuPage) {
            "root" -> {
                DropdownMenuItem(
                    text = { Text("文件设置", color = c.textPrimary, fontSize = 16.sp) },
                    leadingIcon = { IcoGear(size = 22.dp, tint = c.textPrimary) },
                    onClick = { menuPage = null; showFileSettings = true },
                )
                DropdownMenuItem(
                    text = { Text("分享", color = c.textPrimary, fontSize = 16.sp) },
                    leadingIcon = { IcoShare(size = 22.dp, tint = c.textPrimary) },
                    trailingIcon = { IcoChevronRight(size = 18.dp, tint = c.textSecondary) },
                    onClick = { menuPage = "share" },
                )
                DropdownMenuItem(
                    text = { Text("工具", color = c.textPrimary, fontSize = 16.sp) },
                    leadingIcon = { IcoTextLines(size = 22.dp, tint = c.textPrimary) },
                    trailingIcon = { IcoChevronRight(size = 18.dp, tint = c.textSecondary) },
                    onClick = { menuPage = "tools" },
                )
                DropdownMenuItem(
                    text = { Text("文件浏览器", color = c.textPrimary, fontSize = 16.sp) },
                    leadingIcon = { IcoFolder(size = 22.dp, tint = c.textPrimary) },
                    onClick = { menuPage = null; onOpenBrowser() },
                )
                DropdownMenuItem(
                    text = { Text("信息", color = c.textPrimary, fontSize = 16.sp) },
                    leadingIcon = { IcoInfo(size = 22.dp, tint = c.textPrimary) },
                    onClick = { menuPage = null; showInfo = true },
                )
                DropdownMenuItem(
                    text = { Text("刷新", color = c.textPrimary, fontSize = 16.sp) },
                    leadingIcon = { IcoRefresh(size = 22.dp, tint = c.textPrimary) },
                    onClick = {
                        menuPage = null
                        value = TextFieldValue(state.store.read(file))
                        savedText = value.text
                        state.toast("已刷新")
                    },
                )
            }

            "share" -> {
                DropdownMenuItem(
                    text = { Text("URL / 路径", color = c.textPrimary, fontSize = 16.sp) },
                    leadingIcon = { IcoLink(size = 22.dp, tint = c.textPrimary) },
                    onClick = {
                        menuPage = null
                        state.copyToClipboard("path", state.store.displayPath(file))
                        state.toast("路径已复制")
                    },
                )
                DropdownMenuItem(
                    text = { Text("纯文本", color = c.textPrimary, fontSize = 16.sp) },
                    leadingIcon = { IcoTextLines(size = 22.dp, tint = c.textPrimary) },
                    onClick = {
                        menuPage = null
                        state.copyToClipboard("note", value.text)
                        state.toast("文本已复制")
                    },
                )
                DropdownMenuItem(
                    text = { Text("打印/PDF", color = c.textPrimary, fontSize = 16.sp) },
                    leadingIcon = { IcoPrint(size = 22.dp, tint = c.textPrimary) },
                    onClick = {
                        menuPage = null
                        val target = state.exportHtml(file, value.text)
                        state.toast("已导出 ${target.name}")
                    },
                )
                DropdownMenuItem(
                    text = { Text("文件", color = c.textPrimary, fontSize = 16.sp) },
                    leadingIcon = { IcoDoc(size = 22.dp, tint = c.textPrimary) },
                    onClick = { menuPage = null; state.shareText(file, value.text) },
                )
                DropdownMenuItem(
                    text = { Text("HTML", color = c.textPrimary, fontSize = 16.sp) },
                    leadingIcon = { IcoCode(size = 22.dp, tint = c.textPrimary) },
                    onClick = {
                        menuPage = null
                        val target = state.exportHtml(file, value.text)
                        state.toast("已导出 ${target.name}")
                    },
                )
                DropdownMenuItem(
                    text = { Text("HTML (显示为纯文本)", color = c.textPrimary, fontSize = 16.sp) },
                    leadingIcon = { IcoCode(size = 22.dp, tint = c.textPrimary) },
                    onClick = { menuPage = null; showHtmlSource = true },
                )
                DropdownMenuItem(
                    text = { Text("图片", color = c.textPrimary, fontSize = 16.sp) },
                    leadingIcon = { IcoImage(size = 22.dp, tint = c.textPrimary) },
                    onClick = {
                        menuPage = null
                        val target = state.exportPng(file, value.text)
                        state.toast("已导出 ${target.name}")
                    },
                )
                DropdownMenuItem(
                    text = { Text("Screenshot", color = c.textPrimary, fontSize = 16.sp) },
                    leadingIcon = { IcoImage(size = 22.dp, tint = c.textPrimary) },
                    onClick = {
                        menuPage = null
                        val target = state.exportPng(file, value.text)
                        state.toast("已导出 ${target.name}")
                    },
                )
                DropdownMenuItem(
                    text = { Text("日历", color = c.textPrimary, fontSize = 16.sp) },
                    leadingIcon = { IcoCalendar(size = 22.dp, tint = c.textPrimary) },
                    onClick = {
                        menuPage = null
                        val target = state.exportIcs(file, value.text)
                        state.toast("已导出 ${target.name}")
                    },
                )
            }

            "tools" -> {
                DropdownMenuItem(
                    text = { Text("Speed Read", color = c.textPrimary, fontSize = 16.sp) },
                    leadingIcon = { IcoBolt(size = 22.dp, tint = c.textPrimary) },
                    onClick = { menuPage = null; speedRead = true },
                )
                DropdownMenuItem(
                    text = { Text("统计信息", color = c.textPrimary, fontSize = 16.sp) },
                    leadingIcon = { IcoInfo(size = 22.dp, tint = c.textPrimary) },
                    onClick = { menuPage = null; showInfo = true },
                )
            }
        }
    }

    if (showFileSettings) {
        FileSettingsDialog(
            state = state,
            onDismiss = { showFileSettings = false },
        )
    }

    if (showInfo) {
        val lines = value.text.split('\n').size
        val words = value.text.split(Regex("\\s+")).count { it.isNotBlank() }
        AlertDialog(
            onDismissRequest = { showInfo = false },
            containerColor = c.surface,
            title = { Text("信息", color = c.textPrimary, fontSize = 20.sp) },
            text = {
                Column {
                    InfoLine("名称", file.name)
                    InfoLine("路径", state.store.displayPath(file))
                    InfoLine("大小", "${file.length()} B")
                    InfoLine("修改时间", Store.audit(file.lastModified()))
                    InfoLine("行数", "$lines")
                    InfoLine("词数", "$words")
                    InfoLine("字符数", "${value.text.length}")
                }
            },
            confirmButton = { TextButton(onClick = { showInfo = false }) { Text("确定", color = c.accent, fontSize = 16.sp) } },
        )
    }

    if (showPriority) {
        AlertDialog(
            onDismissRequest = { showPriority = false },
            containerColor = c.surface,
            title = { Text("优先级", color = c.textPrimary, fontSize = 20.sp) },
            text = {
                Column {
                    listOf("无", "A", "B", "C", "D", "E").forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showPriority = false
                                    val text = value.text
                                    val start = lineStartOf(text, value.selection.end)
                                    val end = lineEndOf(text, value.selection.end)
                                    val line = text.substring(start, end)
                                    val indent = line.takeWhile { it.isWhitespace() }
                                    val body = line.trimStart()
                                    val withoutPriority = body.replaceFirst(Regex("^\\([A-Z]\\)\\s+"), "")
                                    val newLine = if (option == "无") indent + withoutPriority
                                    else "$indent($option) $withoutPriority"
                                    commit(TextFieldValue(text.substring(0, start) + newLine + text.substring(end), TextRange(start + newLine.length)))
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (option == "无") IcoTrash(size = 22.dp, tint = c.accent) else IcoStar(size = 22.dp, tint = c.textSecondary)
                            Spacer(Modifier.width(16.dp))
                            Text(option, color = c.textPrimary, fontSize = 16.sp)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showPriority = false }) { Text("取消", color = c.accent, fontSize = 16.sp) } },
        )
    }

    if (showClearDone) {
        val doneCount = value.text.split('\n').count { Regex("^\\s*x\\s+\\d{4}-\\d{2}-\\d{2}\\s+.*").matches(it) }
        ConfirmDialog(
            title = "确认删除",
            body = "删除 $doneCount 条已完成任务",
            onConfirm = {
                showClearDone = false
                val kept = value.text.split('\n').filterNot { Regex("^\\s*x\\s+\\d{4}-\\d{2}-\\d{2}\\s+.*").matches(it) }
                commit(TextFieldValue(kept.joinToString("\n"), TextRange(0)))
                state.toast("已删除 $doneCount 条")
            },
            onDismiss = { showClearDone = false },
        )
    }

    if (showHtmlSource) {
        AlertDialog(
            onDismissRequest = { showHtmlSource = false },
            containerColor = c.surface,
            title = { Text("HTML", color = c.textPrimary, fontSize = 20.sp) },
            text = {
                Text(
                    text = value.text.split('\n').joinToString("\n") { "<p>${it}</p>" },
                    color = c.textPrimary,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            },
            confirmButton = { TextButton(onClick = { showHtmlSource = false }) { Text("确定", color = c.accent, fontSize = 16.sp) } },
        )
    }

    if (speedRead) {
        SpeedReadDialog(text = value.text, onDismiss = { speedRead = false })
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    val c = LocalMarkorColors.current
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = c.textSecondary, fontSize = 14.sp, modifier = Modifier.width(84.dp))
        Text(value, color = c.textPrimary, fontSize = 14.sp)
    }
}

@Composable
private fun NoteTopBar(
    title: String?,
    preview: Boolean,
    dirty: Boolean,
    showFind: Boolean,
    query: String,
    matchCount: Int,
    matchIndex: Int,
    onQueryChange: (String) -> Unit,
    onCloseFind: () -> Unit,
    onPrevMatch: () -> Unit,
    onNextMatch: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSave: () -> Unit,
    onPreview: () -> Unit,
    onSearch: () -> Unit,
    onOverflow: () -> Unit,
    onBack: () -> Unit,
) {
    val c = LocalMarkorColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.toolbar)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showFind) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(color = c.onToolbar, fontSize = 17.sp),
                    cursorBrush = SolidColor(c.accent),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (query.isEmpty()) Text("搜索", color = c.textSecondary, fontSize = 17.sp)
            }
            Text(
                text = if (matchCount == 0) "0/0" else "${matchIndex + 1}/$matchCount",
                color = c.onToolbar,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            TopIcon(onPrevMatch) { IcoChevronUp(size = 22.dp, tint = c.onToolbar) }
            TopIcon(onNextMatch) { IcoChevronDown(size = 22.dp, tint = c.onToolbar) }
            TopIcon(onCloseFind) { IcoClose(size = 20.dp, tint = c.onToolbar) }
            TopIcon(onOverflow) { IcoMore(size = 22.dp, tint = c.onToolbar) }
            return@Row
        }

        if (preview) {
            TopIcon(onBack) { IcoArrowLeft(size = 22.dp, tint = c.onToolbar) }
        }
        Text(
            text = title ?: "",
            color = c.onToolbar,
            fontSize = 20.sp,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
        )
        if (preview) {
            TopIcon(onPreview) { IcoPencil(size = 22.dp, tint = c.onToolbar) }
        } else {
            TopIcon(onUndo) { IcoUndo(size = 22.dp, tint = if (dirty) c.onToolbar else c.textSecondary) }
            TopIcon(onRedo) { IcoRedo(size = 22.dp, tint = if (dirty) c.onToolbar else c.textSecondary) }
            TopIcon(onSave) { IcoSave(size = 22.dp, tint = if (dirty) c.onToolbar else c.textSecondary) }
            TopIcon(onPreview) { IcoEye(size = 22.dp, tint = c.onToolbar) }
        }
        TopIcon(onSearch) { IcoSearch(size = 22.dp, tint = c.onToolbar) }
        TopIcon(onOverflow) { IcoMore(size = 22.dp, tint = c.onToolbar) }
    }
}

@Composable
private fun TopIcon(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun ViewerBottomBar(onRefresh: () -> Unit, onShare: () -> Unit, onExport: () -> Unit) {
    val c = LocalMarkorColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.surface)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TopIcon(onRefresh) { IcoRefresh(size = 22.dp, tint = c.textPrimary) }
        TopIcon(onShare) { IcoOpenExternal(size = 22.dp, tint = c.textPrimary) }
        TopIcon(onExport) { IcoDownload(size = 22.dp, tint = c.textPrimary) }
    }
}

@Composable
private fun FormatBar(
    mode: NoteMode,
    onOrderedList: () -> Unit,
    onClock: () -> Unit,
    onKeyboard: () -> Unit,
    onIndent: () -> Unit,
    onCode: () -> Unit,
    onBold: () -> Unit,
    onHeading: (Int) -> Unit,
    onSortTasks: () -> Unit,
    onPriority: () -> Unit,
    onUndoTask: () -> Unit,
    onMarkDone: () -> Unit,
    onDueDate: () -> Unit,
    onCase: () -> Unit,
    onClearDone: () -> Unit,
) {
    val c = LocalMarkorColors.current
    Column {
        HorizontalDivider(color = c.divider)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(c.surface)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (mode == NoteMode.TASK) {
                BarText("A-Z", onSortTasks)
                BarIcon(onPriority) { IcoStar(size = 22.dp, tint = c.textPrimary) }
                BarIcon(onUndoTask) { IcoUndo(size = 22.dp, tint = c.textPrimary) }
                BarIcon(onMarkDone) { IcoCheckSquare(size = 22.dp, tint = c.textPrimary, filled = false) }
                BarIcon(onDueDate) { IcoClock(size = 22.dp, tint = c.textPrimary) }
                BarIcon(onKeyboard) { IcoKeyboard(size = 22.dp, tint = c.textPrimary) }
                BarText("aA", onCase)
                BarIcon(onClearDone) { IcoTrash(size = 22.dp, tint = c.textPrimary) }
            } else {
                BarIcon(onOrderedList) { IcoListBullets(size = 22.dp, tint = c.textPrimary) }
                BarIcon(onUndoTask) { IcoUndo(size = 22.dp, tint = c.textPrimary) }
                BarIcon(onClock) { IcoClock(size = 22.dp, tint = c.textPrimary) }
                BarIcon(onKeyboard) { IcoKeyboard(size = 22.dp, tint = c.textPrimary) }
                BarIcon(onIndent) { IcoIndent(size = 22.dp, tint = c.textPrimary) }
                BarIcon(onCode) { IcoCode(size = 22.dp, tint = c.textPrimary) }
                BarText("B", onBold, bold = true)
                BarText("H2", { onHeading(2) })
                BarText("H1", { onHeading(1) })
            }
        }
    }
}

@Composable
private fun BarIcon(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun BarText(label: String, onClick: () -> Unit, bold: Boolean = false) {
    val c = LocalMarkorColors.current
    Box(
        modifier = Modifier
            .size(46.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = c.textPrimary, fontSize = 17.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun FileSettingsDialog(state: AppState, onDismiss: () -> Unit) {
    val c = LocalMarkorColors.current
    val prefs = state.prefs
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = { Text("文件设置", color = c.textPrimary, fontSize = 20.sp) },
        text = {
            Column {
                SwitchRow("自动换行", prefs.bool("editorWrap", true)) { prefs.setBool("editorWrap", it) }
                SwitchRow("行号", prefs.bool("editorLineNumbers", false)) { prefs.setBool("editorLineNumbers", it) }
                SwitchRow("语法高亮", prefs.bool("editorHighlight", true)) { prefs.setBool("editorHighlight", it) }
                SwitchRow("自动格式", prefs.bool("editorAutoFormat", true)) { prefs.setBool("editorAutoFormat", it) }
                Spacer(Modifier.height(8.dp))
                Text("字号", color = c.textPrimary, fontSize = 15.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Slider(
                        value = prefs.int("editorFontSize", 15).toFloat(),
                        onValueChange = { prefs.setInt("editorFontSize", it.toInt()) },
                        valueRange = 10f..32f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = c.accent, activeTrackColor = c.accent),
                    )
                    Text("${prefs.int("editorFontSize", 15)}", color = c.textPrimary, fontSize = 15.sp)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭", color = c.accent, fontSize = 16.sp) } },
    )
}

@Composable
fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = LocalMarkorColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = c.textPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = c.accent),
        )
    }
}

@Composable
private fun MarkdownView(text: String, query: String, current: Int, fontSize: Int) {
    val c = LocalMarkorColors.current
    val blocks = remember(text) { parseMarkdown(text) }
    val matches = remember(text, query) {
        if (query.isBlank()) emptyList()
        else Regex(Regex.escape(query), RegexOption.IGNORE_CASE).findAll(text).map { it.range }.toList()
    }
    val activeRange = matches.getOrNull(current)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Heading -> {
                    Text(
                        text = styled(block.text, c, query, activeRange, text),
                        color = c.textPrimary,
                        fontSize = when (block.level) {
                            1 -> 26.sp
                            2 -> 22.sp
                            else -> 19.sp
                        },
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 10.dp, bottom = 6.dp),
                    )
                    if (block.level <= 2) HorizontalDivider(color = c.divider)
                }

                is MdBlock.Ordered -> Row(modifier = Modifier.padding(start = 24.dp, top = 3.dp, bottom = 3.dp)) {
                    Text("${block.number}.", color = c.textPrimary, fontSize = fontSize.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(styled(block.text, c, query, activeRange, text), color = c.textPrimary, fontSize = fontSize.sp)
                }

                is MdBlock.Bullet -> Row(modifier = Modifier.padding(start = 24.dp, top = 3.dp, bottom = 3.dp)) {
                    Text("•", color = c.textPrimary, fontSize = fontSize.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(styled(block.text, c, query, activeRange, text), color = c.textPrimary, fontSize = fontSize.sp)
                }

                is MdBlock.Quote -> Text(
                    text = styled(block.text, c, query, activeRange, text),
                    color = c.textSecondary,
                    fontSize = fontSize.sp,
                    modifier = Modifier.padding(start = 20.dp, top = 3.dp, bottom = 3.dp),
                )

                is MdBlock.Paragraph -> Text(
                    text = styled(block.text, c, query, activeRange, text),
                    color = c.textPrimary,
                    fontSize = fontSize.sp,
                    modifier = Modifier.padding(top = 3.dp, bottom = 3.dp),
                )

                MdBlock.Blank -> Spacer(Modifier.height(10.dp))
            }
        }
    }
}

private fun styled(
    line: String,
    c: MarkorColors,
    query: String,
    activeRange: IntRange?,
    whole: String,
): AnnotatedString {
    val base = inlineStyled(line, c.accent)
    if (query.isBlank()) return base
    val builder = AnnotatedString.Builder(base)
    Regex(Regex.escape(query), RegexOption.IGNORE_CASE).findAll(line).forEach { match ->
        builder.addStyle(SpanStyle(background = Color(0xFFFFB74D)), match.range.first, match.range.last + 1)
    }
    val offset = whole.indexOf(line)
    if (activeRange != null && offset >= 0 && activeRange.first in offset until offset + line.length) {
        val start = activeRange.first - offset
        val end = (activeRange.last + 1 - offset).coerceAtMost(line.length)
        if (start in 0..line.length && end <= line.length && start < end) {
            builder.addStyle(
                SpanStyle(background = Color(0xFFFF9800), textDecoration = TextDecoration.None),
                start,
                end,
            )
        }
    }
    return builder.toAnnotatedString()
}

@Composable
private fun SpeedReadDialog(text: String, onDismiss: () -> Unit) {
    val c = LocalMarkorColors.current
    val words = remember(text) { text.split(Regex("\\s+")).filter { it.isNotBlank() } }
    var index by remember { mutableStateOf(0) }
    var running by remember { mutableStateOf(true) }
    var wpm by remember { mutableStateOf(300) }

    LaunchedEffect(running, wpm) {
        while (running && index < words.lastIndex) {
            kotlinx.coroutines.delay((60_000L / wpm).coerceAtLeast(60L))
            index++
        }
        if (index >= words.lastIndex) running = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = { Text("Speed Read", color = c.textPrimary, fontSize = 20.sp) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = words.getOrNull(index) ?: "",
                        color = c.textPrimary,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Text("${index + 1} / ${words.size}", color = c.textSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$wpm wpm", color = c.textPrimary, fontSize = 14.sp)
                    Slider(
                        value = wpm.toFloat(),
                        onValueChange = { wpm = it.toInt() },
                        valueRange = 100f..600f,
                        modifier = Modifier.weight(1f).padding(start = 12.dp),
                        colors = SliderDefaults.colors(thumbColor = c.accent, activeTrackColor = c.accent),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { running = !running }) { Text(if (running) "暂停" else "继续", color = c.accent, fontSize = 16.sp) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("关闭", color = c.accent, fontSize = 16.sp) } },
    )
}
