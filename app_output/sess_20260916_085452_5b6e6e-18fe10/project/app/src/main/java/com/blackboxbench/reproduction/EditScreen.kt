package com.blackboxbench.reproduction

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat

@Composable
fun EditScreen(
    habits: MutableList<Habit>,
    state: LoopState,
    persist: () -> Unit,
    onDone: () -> Unit
) {
    val ctx = LocalContext.current
    val draft = state.editDraft
    if (draft == null) {
        androidx.compose.runtime.LaunchedEffect(Unit) { onDone() }
        return
    }
    val color = parseColor(draft.color)
    var name by remember { mutableStateOf(draft.name) }
    var question by remember { mutableStateOf(draft.question) }
    var unit by remember { mutableStateOf(draft.unit) }
    var target by remember { mutableStateOf(if (draft.target > 0) Logic.formatNumber(draft.target) else "") }
    var notes by remember { mutableStateOf(draft.notes) }
    var freqKind by remember { mutableStateOf(draft.freqKind) }
    var freqN by remember { mutableStateOf(draft.freqN) }
    var freqM by remember { mutableStateOf(draft.freqM) }
    var reminder by remember { mutableStateOf(draft.reminder) }
    var reminderDays by remember { mutableStateOf(draft.reminderDays.toList()) }
    var atLeast by remember { mutableStateOf(draft.targetAtLeast) }
    var showColor by remember { mutableStateOf(false) }
    var showFreq by remember { mutableStateOf(false) }
    var showDays by remember { mutableStateOf(false) }
    var showTargetType by remember { mutableStateOf(false) }
    var invalid by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    fun summaryOf(kind: FreqKind, n: Int, m: Int): String = when (kind) {
        FreqKind.DAILY -> "每天"
        FreqKind.EVERY_N_DAYS -> "每 $n 天"
        FreqKind.WEEKLY_COUNT -> "每周 $n 次"
        FreqKind.MONTHLY_COUNT -> "每月 $n 次"
        FreqKind.EVERY_N_DAYS_TIMES -> "每 $n 天 $m 次"
        FreqKind.WEEKLY_DAYS -> "每周"
    }

    fun doSave() {
        if (name.isBlank()) { invalid = true; return }
        draft.name = name.trim()
        draft.question = question.trim()
        draft.unit = unit.trim()
        draft.target = target.toDoubleOrNull() ?: 0.0
        draft.notes = notes.trim()
        draft.freqKind = freqKind
        draft.freqN = freqN
        draft.freqM = freqM
        draft.reminder = reminder
        draft.reminderDays = reminderDays.toMutableList()
        draft.targetAtLeast = atLeast
        if (state.editIsNew) habits.add(draft)
        persist()
        if (reminder != null && ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        onDone()
    }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        // top bar
        Row(
            Modifier.fillMaxWidth().background(color).statusBarsPadding().height(58.dp).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(40.dp).clickable { onDone() }, contentAlignment = Alignment.Center) {
                BackIcon(Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                if (state.editIsNew) "新建习惯" else "编辑习惯",
                color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.border(1.dp, Color.White, RoundedCornerShape(4.dp))
                    .clickable { doSave() }
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text("保存", color = Color.White, fontSize = 17.sp)
            }
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; invalid = false },
                        label = { Text("习惯标题") },
                        placeholder = { Text(if (draft.type == HabitType.QUANTIFIED) "例如：跑步" else "例如：锻炼") },
                        isError = invalid,
                        trailingIcon = {
                            if (invalid) {
                                Box(Modifier.size(30.dp).padding(2.dp), contentAlignment = Alignment.Center) {
                                    Text("!", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                                        modifier = Modifier.background(Color(0xFFE53935), RoundedCornerShape(15.dp))
                                            .size(24.dp).padding(top = 2.dp))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier.width(110.dp).height(56.dp)
                        .border(1.dp, Color(0xFF9E9E9E), RoundedCornerShape(6.dp))
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { showColor = true },
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.size(34.dp).clip(RoundedCornerShape(4.dp)).background(color))
                    Text("颜色", color = Color(0xFF888888), fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.TopStart).padding(start = 8.dp, top = 2.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                label = { Text("问题") },
                placeholder = { Text(if (draft.type == HabitType.QUANTIFIED) "例如：今天你跑了几公里？" else "例如：你今天锻炼了吗？") },
                modifier = Modifier.fillMaxWidth()
            )
            if (draft.type == HabitType.QUANTIFIED) {
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("单位") },
                    placeholder = { Text("例如：公里") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Box(Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = target,
                            onValueChange = { target = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("目标") },
                            placeholder = { Text("例如：15") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    SelectorField(
                        label = "频率",
                        value = summaryOf(freqKind, freqN, freqM),
                        modifier = Modifier.weight(1f)
                    ) { showFreq = true }
                }
                Spacer(Modifier.height(14.dp))
                SelectorField(
                    label = "目标类型",
                    value = if (atLeast) "至少" else "至多",
                    modifier = Modifier.fillMaxWidth()
                ) { showTargetType = true }
            } else {
                Spacer(Modifier.height(14.dp))
                SelectorField(
                    label = "频率",
                    value = summaryOf(freqKind, freqN, freqM),
                    modifier = Modifier.fillMaxWidth()
                ) { showFreq = true }
            }
            Spacer(Modifier.height(14.dp))
            SelectorField(
                label = "提醒",
                value = reminder ?: "关闭",
                modifier = Modifier.fillMaxWidth()
            ) {
                val initH: Int
                val initM: Int
                if (reminder != null) {
                    initH = reminder!!.substringBefore(":").toIntOrNull() ?: 8
                    initM = reminder!!.substringAfter(":").toIntOrNull() ?: 0
                } else { initH = 8; initM = 0 }
                TimePickerDialog(ctx, { _, h, m ->
                    reminder = "%02d:%02d".format(h, m)
                }, initH, initM, true).show()
            }
            if (reminder != null) {
                Spacer(Modifier.height(14.dp))
                SelectorField(
                    label = "重复",
                    value = if (reminderDays.size == 7) "每天" else reminderDays.sorted().joinToString("") { Logic.WEEKDAY_CN[it].removePrefix("周") },
                    modifier = Modifier.fillMaxWidth()
                ) { showDays = true }
            }
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("备注") },
                placeholder = { Text("(选填)") },
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(40.dp))
        }
    }

    if (showColor) {
        ColorPickerDialog(current = draft.color, onDismiss = { showColor = false }, onPick = {
            draft.color = it; showColor = false
        })
        // force recomposition of the swatch by toggling a key
    }
    if (showFreq) {
        FrequencyDialog(
            current = freqKind, n = freqN, m = freqM,
            onDismiss = { showFreq = false },
            onApply = { k, n, m -> freqKind = k; freqN = n; freqM = m; showFreq = false }
        )
    }
    if (showDays) {
        DayChooserDialog(
            selected = reminderDays,
            onDismiss = { showDays = false },
            onApply = { reminderDays = it; showDays = false }
        )
    }
    if (showTargetType) {
        Dialog(onDismissRequest = { showTargetType = false }) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(Color.White).padding(vertical = 10.dp)
            ) {
                listOf(true to "至少", false to "至多").forEach { (v, label) ->
                    Text(label, color = Color(0xFF222222), fontSize = 20.sp,
                        modifier = Modifier.fillMaxWidth().clickable { atLeast = v; showTargetType = false }
                            .padding(horizontal = 28.dp, vertical = 14.dp))
                }
            }
        }
    }
}

@Composable
fun SelectorField(label: String, value: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { DownArrowIcon(Color(0xFF888888)) },
            modifier = Modifier.fillMaxWidth()
        )
        Box(Modifier.matchParentSize().clickable { onClick() })
    }
}

@Composable
fun FrequencyDialog(
    current: FreqKind,
    n: Int,
    m: Int,
    onDismiss: () -> Unit,
    onApply: (FreqKind, Int, Int) -> Unit
) {
    var sel by remember { mutableStateOf(current) }
    var nn by remember { mutableStateOf(n.toString()) }
    var mm by remember { mutableStateOf(m.toString()) }
    var mt by remember { mutableStateOf("10") }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(Color.White)
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            FreqRow("每天", sel == FreqKind.DAILY) { sel = FreqKind.DAILY }
            FreqRowNum("每", nn, { nn = it }, "天", sel == FreqKind.EVERY_N_DAYS) {
                sel = FreqKind.EVERY_N_DAYS
            }
            FreqRowNum("每周", nn, { nn = it }, "次", sel == FreqKind.WEEKLY_COUNT) {
                sel = FreqKind.WEEKLY_COUNT
            }
            FreqRowNum("每月", mt, { mt = it }, "次", sel == FreqKind.MONTHLY_COUNT) {
                sel = FreqKind.MONTHLY_COUNT
            }
            FreqRowNumNum("每", nn, { nn = it }, "天", mm, { mm = it }, "次", sel == FreqKind.EVERY_N_DAYS_TIMES) {
                sel = FreqKind.EVERY_N_DAYS_TIMES
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text("保存", color = Color(0xFF3F51B5), fontSize = 17.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable {
                        val nVal = nn.toIntOrNull() ?: 1
                        val mVal = mm.toIntOrNull() ?: 1
                        val moVal = mt.toIntOrNull() ?: 1
                        when (sel) {
                            FreqKind.DAILY -> onApply(FreqKind.DAILY, nVal, mVal)
                            FreqKind.EVERY_N_DAYS -> onApply(FreqKind.EVERY_N_DAYS, nVal, mVal)
                            FreqKind.WEEKLY_COUNT -> onApply(FreqKind.WEEKLY_COUNT, nVal, mVal)
                            FreqKind.MONTHLY_COUNT -> onApply(FreqKind.MONTHLY_COUNT, moVal, mVal)
                            FreqKind.EVERY_N_DAYS_TIMES -> onApply(FreqKind.EVERY_N_DAYS_TIMES, nVal, mVal)
                            FreqKind.WEEKLY_DAYS -> onApply(FreqKind.DAILY, nVal, mVal)
                        }
                    }.padding(8.dp))
            }
        }
    }
}

@Composable
private fun FreqRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onSelect() }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected, onClick = onSelect)
        Text(label, fontSize = 19.sp, color = Color(0xFF222222))
    }
}

@Composable
private fun FreqRowNum(prefix: String, value: String, onValue: (String) -> Unit, suffix: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onSelect() }.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected, onClick = onSelect)
        Text(prefix, fontSize = 19.sp, color = Color(0xFF222222))
        Spacer(Modifier.width(8.dp))
        NumberBox(value, onValue)
        Spacer(Modifier.width(8.dp))
        Text(suffix, fontSize = 19.sp, color = Color(0xFF222222))
    }
}

@Composable
private fun FreqRowNumNum(prefix: String, v1: String, onV1: (String) -> Unit, mid: String, v2: String, onV2: (String) -> Unit, suffix: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onSelect() }.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected, onClick = onSelect)
        Text(prefix, fontSize = 19.sp, color = Color(0xFF222222))
        Spacer(Modifier.width(6.dp))
        NumberBox(v1, onV1)
        Spacer(Modifier.width(6.dp))
        Text(mid, fontSize = 19.sp, color = Color(0xFF222222))
        Spacer(Modifier.width(6.dp))
        NumberBox(v2, onV2)
        Spacer(Modifier.width(6.dp))
        Text(suffix, fontSize = 19.sp, color = Color(0xFF222222))
    }
}

@Composable
private fun NumberBox(value: String, onChange: (String) -> Unit) {
    Box(
        Modifier.width(84.dp).height(54.dp)
            .border(1.dp, Color(0xFF9E9E9E), RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = { onChange(it.filter { c -> c.isDigit() }) },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF222222), fontSize = 19.sp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
        )
    }
}

@Composable
fun DayChooserDialog(selected: List<Int>, onDismiss: () -> Unit, onApply: (List<Int>) -> Unit) {
    val order = listOf(5, 6, 0, 1, 2, 3, 4)
    val chosen = remember { mutableStateOf(selected.toSet()) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(Color.White)
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Text("选择天数", color = Color(0xFF222222), fontSize = 22.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(12.dp))
            order.forEach { idx ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(chosen.value.contains(idx), onCheckedChange = {
                        chosen.value = if (it) chosen.value + idx else chosen.value - idx
                    })
                    Text(Logic.WEEKDAY_CN[idx], fontSize = 18.sp, color = Color(0xFF222222))
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text("取消", color = Color(0xFF555555), fontSize = 17.sp,
                    modifier = Modifier.clickable { onDismiss() }.padding(10.dp))
                Spacer(Modifier.width(10.dp))
                Text("确定", color = Color(0xFF3F51B5), fontSize = 17.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable {
                        onApply(chosen.value.sorted())
                    }.padding(10.dp))
            }
        }
    }
}
