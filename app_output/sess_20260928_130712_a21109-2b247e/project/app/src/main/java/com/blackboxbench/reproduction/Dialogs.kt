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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

@Composable
fun CreateDeckDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(12.dp),
        title = { Text("创建牌组", fontSize = 20.sp, color = AnkiColors.TextPrimary) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("名称:") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name) }) {
                Text("创建", color = AnkiColors.Primary, fontSize = 15.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = AnkiColors.Primary, fontSize = 15.sp) }
        }
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirm: String,
    dismiss: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(12.dp),
        title = { Text(title, fontSize = 19.sp, color = AnkiColors.TextPrimary) },
        text = { Text(body, fontSize = 15.sp, color = AnkiColors.TextPrimary) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirm, color = AnkiColors.Primary, fontSize = 15.sp) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismiss, color = AnkiColors.Primary, fontSize = 15.sp) }
        }
    )
}

@Composable
fun CustomStudyDialog(onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val options = listOf(
        "提升今日新卡片上限",
        "提升今日复习卡片上限",
        "复习忘记的卡片",
        "提前复习",
        "预览新卡片",
        "按卡片状态或标签学习"
    )
    Surface(color = Color(0x66000000), modifier = Modifier.fillMaxSize().clickable { onDismiss() }) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Surface(color = Color(0xFFE8F1F8), shape = RoundedCornerShape(6.dp), shadowElevation = 10.dp) {
                Column(Modifier.padding(vertical = 10.dp)) {
                    options.forEach { label ->
                        Text(
                            label,
                            fontSize = 15.sp,
                            color = AnkiColors.TextPrimary,
                            modifier = Modifier
                                .width(280.dp)
                                .clickable { onPick(label) }
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExportDialog(onDismiss: () -> Unit, onExport: () -> Unit) {
    var format by remember { mutableStateOf("Anki 集合文件 (.colpkg)") }
    var media by remember { mutableStateOf(true) }
    var legacy by remember { mutableStateOf(false) }
    var formatOpen by remember { mutableStateOf(false) }
    Surface(color = Color(0x66000000), modifier = Modifier.fillMaxSize().clickable { onDismiss() }) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Surface(color = Color(0xFFE8F1F8), shape = RoundedCornerShape(8.dp), shadowElevation = 10.dp) {
                Column(Modifier.padding(20.dp).width(300.dp)) {
                    Text("导出格式", fontSize = 17.sp, color = AnkiColors.TextPrimary)
                    Spacer(Modifier.height(10.dp))
                    Box {
                        Row(
                            Modifier.fillMaxWidth().clickable { formatOpen = true },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(format, fontSize = 15.sp, color = AnkiColors.TextPrimary, modifier = Modifier.weight(1f))
                            Text("\u25BE", fontSize = 14.sp, color = AnkiColors.TextSecondary)
                        }
                        DropdownMenu(expanded = formatOpen, onDismissRequest = { formatOpen = false }) {
                            listOf("Anki 集合文件 (.colpkg)", "Anki 牌组包 (.apkg)", "文本文件 (.txt)").forEach {
                                DropdownMenuItem(text = { Text(it, fontSize = 15.sp) }, onClick = { format = it; formatOpen = false })
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("包含：", fontSize = 15.sp, color = AnkiColors.TextPrimary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = media, onCheckedChange = { media = it })
                        Text("包含媒体文件", fontSize = 15.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = legacy, onCheckedChange = { legacy = it })
                        Text("支持较旧的 Anki 版本（较慢/导出文件较大）", fontSize = 14.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text("取消", color = AnkiColors.Primary) }
                        TextButton(onClick = onExport) { Text("导出", color = AnkiColors.Primary) }
                    }
                }
            }
        }
    }
}

@Composable
fun FilteredDeckScreen(app: AppState, onBack: () -> Unit, onCreated: () -> Unit) {
    val now = Calendar.getInstance()
    val defaultName = "Filtered Deck %02d:%02d".format(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
    var name by remember { mutableStateOf(defaultName) }
    var search by remember { mutableStateOf("deck:默认牌组 is:due") }
    var limit by remember { mutableStateOf("100") }
    var order by remember { mutableStateOf("乱序") }
    var orderOpen by remember { mutableStateOf(false) }
    var second by remember { mutableStateOf(false) }
    var reschedule by remember { mutableStateOf(true) }
    var createEmpty by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(44.dp).clickable { onBack() }, contentAlignment = Alignment.Center) {
                    Text("\u2715", fontSize = 20.sp, color = AnkiColors.TextPrimary)
                }
                Text("Filtered Deck", fontSize = 18.sp, color = AnkiColors.TextPrimary, modifier = Modifier.weight(1f))
                Surface(color = AnkiColors.Primary, shape = RoundedCornerShape(20.dp)) {
                    Text(
                        "创建筛选牌组",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clickable {
                                app.addFilteredDeck(name, search, limit.toIntOrNull() ?: 100, order)
                                onCreated()
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                Box(Modifier.size(44.dp).clickable { }, contentAlignment = Alignment.Center) {
                    Text("?", fontSize = 16.sp, color = AnkiColors.TextSecondary)
                }
            }
        }
        HorizontalDivider(color = AnkiColors.RowDivider)
        Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
            Text("名称", fontSize = 15.sp, color = AnkiColors.TextPrimary)
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(18.dp))
            Text("筛选", fontSize = 20.sp, color = AnkiColors.TextPrimary, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = search, onValueChange = { search = it },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(14.dp))
            Text("上限", fontSize = 14.sp, color = AnkiColors.TextPrimary)
            OutlinedTextField(
                value = limit, onValueChange = { limit = it },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(18.dp))
            Text("张卡片，选择方式", fontSize = 20.sp, color = AnkiColors.TextPrimary, fontWeight = FontWeight.Medium)
            Box {
                Row(
                    Modifier.fillMaxWidth().clickable { orderOpen = true }.padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(order, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    Text("\u25BE", fontSize = 14.sp, color = AnkiColors.TextSecondary)
                }
                DropdownMenu(expanded = orderOpen, onDismissRequest = { orderOpen = false }) {
                    listOf("乱序", "最大间隔", "最小间隔", "最旧优先", "最新优先").forEach {
                        DropdownMenuItem(text = { Text(it) }, onClick = { order = it; orderOpen = false })
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("启用第 2 筛选器", fontSize = 20.sp, color = AnkiColors.TextPrimary, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Switch(checked = second, onCheckedChange = { second = it }, colors = SwitchDefaults.colors(checkedTrackColor = AnkiColors.Primary))
            }
            Spacer(Modifier.height(14.dp))
            Text("选项", fontSize = 20.sp, color = AnkiColors.TextPrimary, fontWeight = FontWeight.Medium)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("基于此牌组的回答情况，对卡片重新排程。", fontSize = 15.sp, modifier = Modifier.weight(1f))
                Switch(checked = reschedule, onCheckedChange = { reschedule = it }, colors = SwitchDefaults.colors(checkedTrackColor = AnkiColors.Primary))
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("创建/更新此牌组，即使其为空。", fontSize = 15.sp, modifier = Modifier.weight(1f))
                Switch(checked = createEmpty, onCheckedChange = { createEmpty = it }, colors = SwitchDefaults.colors(checkedTrackColor = AnkiColors.Primary))
            }
            Spacer(Modifier.height(40.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("显示所有已被排除的卡片", color = AnkiColors.Primary, fontSize = 15.sp)
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}
