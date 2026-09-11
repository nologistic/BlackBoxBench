package com.blackboxbench.reproduction

import android.content.SharedPreferences
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.math.BigDecimal
import kotlin.math.pow
import kotlin.math.sqrt

private data class HistoryItem(val expression: String, val result: String)

private fun formatNumber(value: Double): String {
    if (!value.isFinite()) return ""
    val cleaned = if (kotlin.math.abs(value) < 1e-12) 0.0 else value
    return BigDecimal.valueOf(cleaned).stripTrailingZeros().toPlainString()
}

private fun readHistory(preferences: SharedPreferences): List<HistoryItem> {
    val text = preferences.getString("history", "") ?: ""
    return text.lineSequence().mapNotNull { line ->
        val pieces = line.split('\t', limit = 2)
        if (pieces.size == 2) HistoryItem(pieces[0], pieces[1]) else null
    }.toList()
}

private fun saveHistory(preferences: SharedPreferences, history: List<HistoryItem>) {
    val value = history.take(40).joinToString("\n") { it.expression + "\t" + it.result }
    preferences.edit().putString("history", value).apply()
}

private fun calculate(left: Double, right: Double, operator: String): Double? {
    val value = when (operator) {
        "+" -> left + right
        "−" -> left - right
        "×" -> left * right
        "÷" -> if (right == 0.0) return null else left / right
        "^" -> left.pow(right)
        "√" -> if (right < 0.0) return null else left * sqrt(right)
        else -> return null
    }
    return if (value.isFinite()) value else null
}

@Composable
private fun HistoryIcon(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onBackground
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * .32f
        drawCircle(color, radius, center, style = Stroke(width = size.minDimension * .07f))
        drawLine(color, center, Offset(center.x, center.y - radius * .58f), strokeWidth = size.minDimension * .07f, cap = StrokeCap.Round)
        drawLine(color, center, Offset(center.x + radius * .5f, center.y + radius * .34f), strokeWidth = size.minDimension * .07f, cap = StrokeCap.Round)
    }
}

@Composable
private fun ConverterIcon(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onBackground
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawLine(color, Offset(w * .22f, h * .22f), Offset(w * .22f, h * .78f), strokeWidth = w * .07f)
        drawLine(color, Offset(w * .22f, h * .78f), Offset(w * .82f, h * .78f), strokeWidth = w * .07f)
        for (i in 0..3) {
            val x = w * (.31f + i * .14f)
            drawLine(color, Offset(x, h * .65f), Offset(x, if (i % 2 == 0) h * .48f else h * .56f), strokeWidth = w * .045f)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalculatorButton(
    label: String,
    modifier: Modifier,
    emphasized: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val dark = MaterialTheme.colorScheme.background.red < .2f
    val color = if (dark) {
        if (emphasized) Color(0xFF103F1A) else Color(0xFF102117)
    } else {
        if (emphasized) Color(0xFFD7DEF2) else Color(0xFFF0EFF9)
    }
    val clickModifier = if (onLongClick != null) {
        modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
    } else {
        modifier.combinedClickable(onClick = onClick)
    }
    Surface(
        modifier = clickModifier,
        shape = RoundedCornerShape(26.dp),
        color = color
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = if (label == "√") 39.sp else 34.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Light
            )
        }
    }
}

@Composable
fun CalculatorScreen(
    onOpenConverters: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("calculator_state", 0) }
    val history = remember { mutableStateListOf<HistoryItem>().apply { addAll(readHistory(preferences)) } }

    var current by rememberSaveable { mutableStateOf("0") }
    var expression by rememberSaveable { mutableStateOf("") }
    var leftValue by rememberSaveable { mutableStateOf<Double?>(null) }
    var pendingOperator by rememberSaveable { mutableStateOf<String?>(null) }
    var percentText by rememberSaveable { mutableStateOf<String?>(null) }
    var justEvaluated by rememberSaveable { mutableStateOf(false) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var showMenu by rememberSaveable { mutableStateOf(false) }

    fun clearAll() {
        current = "0"
        expression = ""
        leftValue = null
        pendingOperator = null
        percentText = null
        justEvaluated = false
    }

    fun addHistory(itemExpression: String, value: Double) {
        val answer = formatNumber(value)
        history.add(0, HistoryItem(itemExpression, answer))
        while (history.size > 40) history.removeAt(history.lastIndex)
        saveHistory(preferences, history)
    }

    fun enterDigit(digit: String) {
        if (justEvaluated) {
            clearAll()
            current = ""
        }
        percentText = null
        current = if (current == "0" || current.isBlank()) digit else current + digit
        if (pendingOperator != null && leftValue != null) {
            expression = formatNumber(leftValue!!) + pendingOperator + current
        }
    }

    fun enterDecimal() {
        if (justEvaluated) {
            clearAll()
            current = ""
        }
        if (!current.contains(".")) {
            current = if (current.isBlank()) "." else current + "."
        }
        if (pendingOperator != null && leftValue != null) {
            expression = formatNumber(leftValue!!) + pendingOperator + current
        }
    }

    fun pressOperator(newOperator: String) {
        val numeric = current.toDoubleOrNull()
        if (pendingOperator == null || leftValue == null) {
            val first = if (newOperator == "√" && (numeric == null || current == "0")) 1.0 else (numeric ?: return)
            leftValue = first
            pendingOperator = newOperator
            current = ""
            expression = formatNumber(first) + newOperator
            justEvaluated = false
            percentText = null
            return
        }

        if (numeric != null && current.isNotBlank()) {
            val oldExpression = formatNumber(leftValue!!) + pendingOperator + (percentText ?: current)
            val value = calculate(leftValue!!, numeric, pendingOperator!!) ?: return
            addHistory(oldExpression, value)
            leftValue = value
            current = ""
            pendingOperator = newOperator
            expression = formatNumber(value) + newOperator
        } else {
            pendingOperator = newOperator
            expression = formatNumber(leftValue!!) + newOperator
        }
        percentText = null
        justEvaluated = false
    }

    fun pressPercent() {
        val right = current.toDoubleOrNull() ?: return
        val op = pendingOperator
        val left = leftValue
        if (op != null && left != null) {
            percentText = current + "%"
            val adjusted = if (op == "+" || op == "−") left * right / 100.0 else right / 100.0
            current = formatNumber(adjusted)
            expression = formatNumber(left) + op + percentText
        } else {
            expression = current + "%"
        }
    }

    fun pressEquals() {
        val left = leftValue ?: return
        val op = pendingOperator ?: return
        val right = current.toDoubleOrNull() ?: return
        val fullExpression = formatNumber(left) + op + (percentText ?: current)
        val answer = calculate(left, right, op)
        expression = fullExpression
        if (answer != null) {
            addHistory(fullExpression, answer)
            current = formatNumber(answer)
            leftValue = answer
            pendingOperator = null
            percentText = null
            justEvaluated = true
        }
    }

    fun shortClear() {
        if (current.isNotBlank() && current != "0") {
            current = current.dropLast(1).ifBlank { "0" }
            if (pendingOperator != null && leftValue != null) {
                expression = formatNumber(leftValue!!) + pendingOperator +
                    if (current == "0") "" else current
            } else if (justEvaluated) {
                expression = ""
                justEvaluated = false
            }
        } else {
            clearAll()
        }
        percentText = null
    }

    val shownValue = when {
        current.isNotBlank() -> current
        leftValue != null -> formatNumber(leftValue!!)
        else -> "0"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(68.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { if (history.isNotEmpty()) showHistory = true },
                modifier = Modifier.size(52.dp)
            ) { HistoryIcon(Modifier.size(34.dp)) }
            TextButton(onClick = onOpenConverters, modifier = Modifier.size(52.dp)) {
                ConverterIcon(Modifier.size(34.dp))
            }
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clickable { showMenu = true },
                contentAlignment = Alignment.Center
            ) {
                Text("⋮", fontSize = 34.sp, color = MaterialTheme.colorScheme.onBackground)
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("设置", fontSize = 19.sp) },
                        onClick = {
                            showMenu = false
                            onOpenSettings()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("关于", fontSize = 19.sp) },
                        onClick = {
                            showMenu = false
                            onOpenAbout()
                        }
                    )
                }
            }
            Spacer(Modifier.width(2.dp))
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 9.dp)) {
            Column(
                modifier = Modifier.align(Alignment.CenterEnd),
                horizontalAlignment = Alignment.End
            ) {
                if (expression.isNotBlank()) {
                    Text(
                        expression,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = .84f),
                        textAlign = TextAlign.End
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    shownValue,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    textAlign = TextAlign.End,
                    maxLines = 1
                )
            }
        }

        val rows = listOf(
            listOf("%", "^", "√", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "−"),
            listOf("1", "2", "3", "+"),
            listOf("0", ".", "C", "=")
        )
        Column(
            modifier = Modifier.fillMaxWidth().height(510.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            rows.forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    row.forEachIndexed { columnIndex, key ->
                        val emphasized = rowIndex == 0 || columnIndex == 3 ||
                            (rowIndex == 4 && (columnIndex == 1 || columnIndex == 2))
                        CalculatorButton(
                            label = key,
                            modifier = Modifier.weight(1f).fillMaxSize(),
                            emphasized = emphasized,
                            onClick = {
                                when (key) {
                                    in "0".."9" -> enterDigit(key)
                                    "." -> enterDecimal()
                                    "%" -> pressPercent()
                                    "^", "√", "÷", "×", "−", "+" -> pressOperator(key)
                                    "C" -> shortClear()
                                    "=" -> pressEquals()
                                }
                            },
                            onLongClick = if (key == "C") ({ clearAll() }) else null
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    if (showHistory) {
        AlertDialog(
            onDismissRequest = { showHistory = false },
            title = {
                Text(
                    "历史记录",
                    fontSize = 29.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().height((history.size * 64).coerceIn(64, 360).dp)) {
                    items(history) { item ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = {
                                    expression = item.expression
                                    current = item.result
                                    leftValue = item.result.toDoubleOrNull()
                                    pendingOperator = null
                                    justEvaluated = true
                                    showHistory = false
                                })
                                .padding(vertical = 9.dp)
                        ) {
                            Text(item.expression, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(item.result, fontSize = 21.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistory = false }) { Text("确定", fontSize = 18.sp) }
            },
            dismissButton = {
                TextButton(onClick = {
                    history.clear()
                    saveHistory(preferences, history)
                    showHistory = false
                }) { Text("清除", fontSize = 18.sp) }
            }
        )
    }
}
