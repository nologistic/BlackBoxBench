package com.blackboxbench.reproduction.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.blackboxbench.reproduction.BlueColor

// ---------------- Logo / icons drawn with Canvas ----------------

@Composable
fun TasksLogo(size: Int = 96, corner: Float = 24f) {
    Canvas(modifier = Modifier.size(size.dp)) {
        val r = corner.dp.toPx()
        drawRoundRect(
            color = BlueColor,
            size = this.size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r)
        )
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(w * 0.26f, h * 0.52f)
            lineTo(w * 0.42f, h * 0.68f)
            lineTo(w * 0.74f, h * 0.34f)
        }
        drawPath(path, color = Color.White, style = Stroke(width = w * 0.1f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    }
}

@Composable
fun EmptyTrayIcon(size: Int = 72) {
    Canvas(modifier = Modifier.size(size.dp)) {
        val w = this.size.width
        val h = this.size.height
        val c = Color(0xFFB9BDC7)
        val sw = w * 0.06f
        val path = Path().apply {
            moveTo(w * 0.12f, h * 0.28f)
            lineTo(w * 0.34f, h * 0.28f)
            lineTo(w * 0.42f, h * 0.42f)
            lineTo(w * 0.58f, h * 0.42f)
            lineTo(w * 0.66f, h * 0.28f)
            lineTo(w * 0.88f, h * 0.28f)
            lineTo(w * 0.8f, h * 0.78f)
            lineTo(w * 0.2f, h * 0.78f)
            close()
        }
        drawPath(path, color = c, style = Stroke(width = sw, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }
}

@Composable
fun ColorDot(color: Color?, size: Int = 24) {
    if (color == null) {
        Text("∅", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = (size * 0.8).sp)
    } else {
        Box(
            Modifier
                .size(size.dp)
                .background(color, CircleShape)
        )
    }
}

// ---------------- Empty state ----------------

@Composable
fun EmptyState(text: String = "这里没有任务哦。") {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EmptyTrayIcon()
        Spacer(Modifier.height(16.dp))
        Text(text, color = Color(0xFF9AA0AA), fontSize = 15.sp)
    }
}

// ---------------- Generic dialogs ----------------

@Composable
fun ConfirmDialog(
    title: String,
    text: String = "",
    confirmLabel: String = "确定",
    dismissLabel: String = "取消",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { if (text.isNotEmpty()) Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel, color = BlueColor) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissLabel) }
        }
    )
}

/** Offline "add account" dialog: retry loops back to the same dialog. */
@Composable
fun OfflineAccountDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("未连接到网络") },
        text = {},
        confirmButton = {
            TextButton(onClick = { }) { Text("重试", color = BlueColor) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

// ---------------- Color picker ----------------

val presetColors = listOf(
    0xFF4A73D8, 0xFFD64545, 0xFFE67E22, 0xFFF1C40F, 0xFF27AE60,
    0xFF16A085, 0xFF2980B9, 0xFF8E44AD, 0xFFD81B60, 0xFF6D4C41,
    0xFF607D8B, 0xFF2C3E50
)

@Composable
fun ColorPickerDialog(current: Int?, onSelect: (Int?) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(20.dp)) {
                Text("颜色", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Spacer(Modifier.height(12.dp))
                // color wheel placeholder
                Canvas(Modifier.fillMaxWidth().height(90.dp)) {
                    val cx = size.width / 2f
                    val r = size.height / 2f - 6
                    val colors = listOf(
                        Color(0xFFEF5350), Color(0xFFAB47BC), Color(0xFF5C6BC0),
                        Color(0xFF29B6F6), Color(0xFF26A69A), Color(0xFF9CCC65),
                        Color(0xFFFFEE58), Color(0xFFFFA726)
                    )
                    colors.forEachIndexed { i, col ->
                        val angle = i * 2f * Math.PI.toFloat() / colors.size
                        drawCircle(col, radius = 14f, center = Offset(cx + (r - 10) * kotlin.math.cos(angle), size.height / 2f + (r - 10) * kotlin.math.sin(angle)))
                    }
                    drawCircle(Color.LightGray, radius = 8f, center = Offset(cx, size.height / 2f))
                }
                Spacer(Modifier.height(12.dp))
                val rows = presetColors.chunked(6)
                rows.forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        row.forEach { c ->
                            val selected = current == c.toInt()
                            Box(
                                Modifier
                                    .size(36.dp)
                                    .background(Color(c), CircleShape)
                                    .clickable { onSelect(c.toInt()) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Text("✓", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onSelect(null); onDismiss() }) { Text("无") }
                    TextButton(onClick = onDismiss) { Text("取消") }
                }
            }
        }
    }
}

// ---------------- Icon picker ----------------

val iconGlyphs = listOf(
    "🏷", "☀", "🏠", "💼", "🛒", "❤", "⭐", "✈", "🍔", "🎵",
    "📚", "🏥", "💰", "🐱", "🌱", "🧹", "📞", "🎁", "🏃", "☕",
    "🔧", "📝", "🚀", "🌙", "⚽", "🎬", "📷", "🔑", "🛠", "🌈",
    "📌", "✔", "🔥", "💡", "🗓", "✉", "🧭", "🎓", "🍀", "⚙"
)

@Composable
fun IconPickerDialog(current: String?, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val queryState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    val query = queryState.value
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(16.dp)) {
                androidx.compose.material3.OutlinedTextField(
                    value = query,
                    onValueChange = { queryState.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("搜索") },
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                val shown = if (query.isBlank()) iconGlyphs else iconGlyphs.filter { it.contains(query) || query.contains(it) }
                val rows = shown.chunked(6)
                Column {
                    rows.forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            row.forEach { g ->
                                Box(
                                    Modifier
                                        .size(40.dp)
                                        .clickable { onSelect(g); onDismiss() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(g, fontSize = 24.sp)
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                }
            }
        }
    }
}

// ---------------- Setting row helpers ----------------

@Composable
fun SettingsRow(
    title: String,
    subtitle: String = "",
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        title,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column { content() }
    }
}
