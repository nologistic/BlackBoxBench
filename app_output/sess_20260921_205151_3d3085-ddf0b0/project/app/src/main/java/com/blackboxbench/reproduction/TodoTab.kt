package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private object TodoTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val highlighted = buildTodo(text.text)
        return TransformedText(highlighted, OffsetMapping.Identity)
    }
}

private fun buildTodo(text: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    text.split("\n").forEachIndexed { i, line ->
        builder.append(highlightTodoLine(line))
        if (i < text.split("\n").lastIndex) builder.append("\n")
    }
    return builder.toAnnotatedString()
}

@Composable
fun TodoTab() {
    var text by remember { mutableStateOf(Store.read("todo.txt")) }
    var browseDialog by remember { mutableStateOf(false) }

    val undoStack = remember { ArrayDeque<String>() }
    val redoStack = remember { ArrayDeque<String>() }

    fun save(newText: String) {
        undoStack.addLast(text)
        redoStack.clear()
        text = newText
        Store.write("todo.txt", newText)
    }

    Column(Modifier.fillMaxSize().background(EditorBg)) {
        EditorTopBar(
            onSave = { Store.write("todo.txt", text) },
            onUndo = {
                if (undoStack.isNotEmpty()) {
                    redoStack.addLast(text)
                    text = undoStack.removeLast()
                    Store.write("todo.txt", text)
                }
            },
            onRedo = {
                if (redoStack.isNotEmpty()) {
                    undoStack.addLast(text)
                    text = redoStack.removeLast()
                    Store.write("todo.txt", text)
                }
            },
        )
        BasicTextField(
            value = text,
            onValueChange = { save(autoFormatAppend(text, it)) },
            visualTransformation = TodoTransformation,
            keyboardOptions = KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Ascii,
                autoCorrectEnabled = false,
            ),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            textStyle = TextStyle(fontSize = 16.sp, color = Color(0xFF222222)),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFFECECEC))
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
        ) {
            BarIcon("AZ") {
                val sorted = text.lines().sorted()
                save(sorted.joinToString("\n"))
            }
            BarIcon("☆") {}
            BarIcon("↻") { text = Store.read("todo.txt") }
            BarIcon("☑") { browseDialog = true }
            BarIcon("🕘") { save(text + "2026-09-22 ") }
            BarIcon("⌨") {}
            BarIcon("aA") {}
            BarIcon("🗑") {
                save(text.lines().filter { !isDoneLine(it) }.joinToString("\n"))
            }
            BarIcon("☰") {}
        }
    }

    if (browseDialog) {
        BrowseTodosDialog(text = text, onDismiss = { browseDialog = false })
    }
}

@Composable
fun EditorTopBar(onSave: () -> Unit, onUndo: () -> Unit = {}, onRedo: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NavyBar)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(
            "Markor",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        Spacer(Modifier.weight(1f))
        TopIcon("↶") { onUndo() }
        TopIcon("↷") { onRedo() }
        TopIcon("💾") { onSave() }
        TopIcon("👁") {}
        TopIcon("🔍") {}
        TopIcon("⋮") {}
    }
}

@Composable
private fun BrowseTodosDialog(text: String, onDismiss: () -> Unit) {
    var subDialog by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("浏览待办事项") },
        text = {
            Column {
                BrowseRow("★", "优先级") { subDialog = "优先级" }
                BrowseRow("📅", "截止日期") { subDialog = "截止日期" }
                BrowseRow("➕", "项目") { subDialog = "项目" }
                BrowseRow("@", "情境") { subDialog = "情境" }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = MarkorRed) }
        },
    )
    subDialog?.let { kind ->
        TodoFilterDialog(kind = kind, text = text, onDismiss = { subDialog = null })
    }
}

@Composable
private fun BrowseRow(icon: String, label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Text(icon, fontSize = 18.sp)
        Spacer(Modifier.padding(horizontal = 8.dp))
        Text(label, fontSize = 16.sp)
    }
}

@Composable
private fun TodoFilterDialog(kind: String, text: String, onDismiss: () -> Unit) {
    val options: List<Pair<String, Int>> = when (kind) {
        "项目" -> todoProjects(text).map { it to Regex("\\${it}").findAll(text).count() }
        "情境" -> todoContexts(text).map { it to Regex(it).findAll(text).count() }
        "优先级" -> todoPriorities(text).map { it to Regex("^\\$it", RegexOption.MULTILINE).findAll(text).count() }
        else -> listOf(
            "将来到期" to 0, "今日到期" to 0, "到期时间" to 0,
            "-" to text.lines().count { it.isNotBlank() },
        )
    }
    val checked = remember { mutableStateOf(setOf<String>()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (kind == "截止日期") "按截止时间浏览" else "按${kind}浏览") },
        text = {
            LazyColumn {
                items(options, key = { it.first }) { (label, count) ->
                    val isChecked = checked.value.contains(label)
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                checked.value = if (isChecked) checked.value - label else checked.value + label
                            }
                            .padding(vertical = 8.dp),
                    ) {
                        Text("$label ($count)", modifier = Modifier.weight(1f))
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = {
                                checked.value = if (isChecked) checked.value - label else checked.value + label
                            },
                            colors = CheckboxDefaults.colors(checkedColor = MarkorRed),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onDismiss) { Text("取消", color = MarkorRed) }
                TextButton(onClick = onDismiss) { Text("确定 (${checked.value.size})", color = MarkorRed) }
            }
        },
    )
}
