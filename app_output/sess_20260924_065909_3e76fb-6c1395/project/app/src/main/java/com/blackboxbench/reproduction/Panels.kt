package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.util.Calendar

// ------------------------------------------------------------------ generic dialogs

@Composable
fun OptionDialog(
    p: Palette,
    title: String?,
    options: List<String>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = p.surface, shape = RoundedCornerShape(6.dp)) {
            Column(Modifier.padding(vertical = 8.dp)) {
                if (title != null) {
                    Text(
                        title, color = p.textPrimary, fontSize = 22.sp,
                        modifier = Modifier.padding(start = 22.dp, top = 12.dp, bottom = 6.dp)
                    )
                }
                options.forEach { o ->
                    Text(
                        o, color = p.textPrimary, fontSize = 16.sp,
                        modifier = Modifier.fillMaxWidth().clickable { onPick(o) }
                            .padding(horizontal = 22.dp, vertical = 15.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ConfirmDialog(p: Palette, text: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = if (p.dark) Color(0xFF2A2B31) else Color(0xFFEDEDF2), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(22.dp)) {
                Text(text, color = p.textPrimary, fontSize = 19.sp, modifier = Modifier.padding(bottom = 18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text(
                        "取消", color = Color(0xFF3B78C3), fontSize = 16.sp,
                        modifier = Modifier.clickable { onDismiss() }.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                    Text(
                        "确定", color = Color(0xFF3B78C3), fontSize = 16.sp,
                        modifier = Modifier.clickable { onConfirm() }.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ListMenuDialog(p: Palette, items: List<String>, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = p.surface, shape = RoundedCornerShape(4.dp)) {
            Column(Modifier.padding(vertical = 6.dp)) {
                items.forEach { i ->
                    Text(
                        i, color = p.textPrimary, fontSize = 16.sp,
                        modifier = Modifier.fillMaxWidth().clickable { onPick(i) }
                            .padding(horizontal = 20.dp, vertical = 13.dp)
                    )
                }
            }
        }
    }
}

/** Bottom sheet replacement: scrim + panel anchored to the bottom. */
@Composable
fun BottomPanel(p: Palette, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(p.scrim).clickable { onDismiss() })
        Surface(
            color = if (p.dark) Color(0xFF1F2025) else Color(0xFFF5F4F9),
            shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
        ) {
            Column(Modifier.fillMaxWidth().padding(top = 6.dp).navigationBarsPadding()) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(Modifier.width(46.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(p.textSecondary.copy(alpha = 0.4f)))
                }
                Spacer(Modifier.height(10.dp))
                content()
            }
        }
    }
}

// ------------------------------------------------------------------ drawer

@Composable
fun DrawerOverlay(
    p: Palette,
    viewKey: String,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
    onSearch: () -> Unit,
    onNewFilter: () -> Unit,
    onNewTag: () -> Unit,
    onNewList: () -> Unit
) {
    Store.revision
    val todayCount = tasksForView("today").count { !it.completed }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(p.scrim).clickable { onDismiss() })
        Column(
            modifier = Modifier.width(215.dp).fillMaxHeight()
                .clip(RoundedCornerShape(topEnd = 14.dp, bottomEnd = 14.dp))
                .background(if (p.dark) Color(0xFF1B1C21) else Color(0xFFEDEDF2))
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp)
        ) {
            DrawerCard(p) {
                DrawerRow(p, "我的任务", viewKey == "my", 0xFF3F78C0, Store.tasks.count { !it.completed },
                    icon = { HomeGlyph(p.textPrimary, 18.dp) }) { onPick("my") }
            }
            Spacer(Modifier.height(10.dp))
            DrawerCard(p) {
                DrawerHeader(p, "过滤器", onAdd = onNewFilter)
                DrawerRow(p, "今天", viewKey == "today", 0xFF3F78C0, todayCount,
                    icon = { CalendarStartIcon(p.textPrimary, 18.dp) }) { onPick("today") }
                DrawerRow(p, "最近修改过的", viewKey == "recent", 0xFF3F78C0, Store.tasks.size,
                    icon = { ClockIcon(p.textPrimary, 18.dp) }) { onPick("recent") }
                Store.filters.forEach { f ->
                    DrawerRow(p, f.name, viewKey == "filter:" + f.id, f.color,
                        Store.tasks.count { t -> f.conditions.all { c -> conditionMatches(c, t) } },
                        icon = { FilterIcon(chipColor(p, f.color), 18.dp) }) { onPick("filter:" + f.id) }
                }
            }
            if (Store.settings.drawerTags) {
                Spacer(Modifier.height(10.dp))
                DrawerCard(p) {
                    DrawerHeader(p, "标签", onAdd = onNewTag)
                    Store.tags.forEach { t ->
                        DrawerRow(p, t.name, viewKey == "tag:" + t.id, t.color,
                            Store.tasks.count { it.tagIds.contains(t.id) && !it.completed },
                            icon = { TagIcon(chipColor(p, t.color), 18.dp) }) { onPick("tag:" + t.id) }
                    }
                }
            }
            if (Store.settings.drawerPlaces) {
                Spacer(Modifier.height(10.dp))
                DrawerCard(p) {
                    DrawerHeader(p, "地点", onAdd = { })
                }
            }
            if (Store.settings.drawerLists) {
                Spacer(Modifier.height(10.dp))
                DrawerCard(p) {
                    DrawerHeader(p, "本地清单", onAdd = onNewList)
                    Store.lists.forEach { l ->
                        DrawerRow(p, l.name, viewKey == "list:" + l.id, l.color,
                            Store.tasks.count { it.listId == l.id && !it.completed },
                            icon = { IconGlyph(l.icon, chipColor(p, l.color)) }) { onPick("list:" + l.id) }
                    }
                }
            }
            Spacer(Modifier.height(60.dp))
        }
        Box(
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 26.dp, bottom = 34.dp)
                .size(58.dp).clip(CircleShape).background(Color(0xFF3B6EA5)).clickable { onSearch() },
            contentAlignment = Alignment.Center
        ) { SearchGlyph(Color.White, 28.dp) }
    }
}

@Composable
fun DrawerCard(p: Palette, content: @Composable () -> Unit) {
    Surface(
        color = p.surface, shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)
    ) { Column(Modifier.padding(vertical = 4.dp)) { content() } }
}

@Composable
fun DrawerHeader(p: Palette, title: String, onAdd: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterIcon(p.textPrimary, 18.dp)
        Spacer(Modifier.width(8.dp))
        Text(title, color = p.textPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
        ChevronUp(p.textPrimary, 16.dp)
        Box(Modifier.size(30.dp).clickable { onAdd() }, contentAlignment = Alignment.Center) { PlusGlyph(p.textPrimary) }
    }
}

@Composable
fun DrawerRow(
    p: Palette,
    title: String,
    selectedRow: Boolean,
    colorLong: Long,
    count: Int,
    icon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(if (selectedRow) p.chipBg else Color.Transparent)
            .clickable { onClick() }
            .padding(start = 14.dp, end = 8.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            if (icon != null) icon() else ListGlyphSmall(chipColor(p, colorLong))
        }
        Spacer(Modifier.width(10.dp))
        Text(title, color = p.textPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text("$count", color = p.textSecondary, fontSize = 13.sp)
    }
}

// ------------------------------------------------------------------ sort / display sheet

@Composable
fun SortSheet(p: Palette, onDismiss: () -> Unit) {
    Store.revision
    val s = Store.settings
    var groupDialog by remember { mutableStateOf(false) }
    var sortDialog by remember { mutableStateOf(false) }
    var subtaskDialog by remember { mutableStateOf(false) }
    var completedDialog by remember { mutableStateOf(false) }
    BottomPanel(p, onDismiss) {
        Column(Modifier.fillMaxWidth().padding(bottom = 22.dp)) {
            SheetRow(p, "分组", s.groupBy) { groupDialog = true }
            SheetRow(p, "排序", s.sortBy) { sortDialog = true }
            SheetRow(p, "子任务", s.subtaskOrder) { subtaskDialog = true }
            SheetToggle(p, "显示未开始项目", s.showUnstarted) { s.showUnstarted = it; Store.save() }
            SheetToggle(p, "显示已完成任务", s.showCompleted) { s.showCompleted = it; Store.save() }
            SheetToggle(p, "显示已完成的子任务", s.showCompletedSubtasks) { s.showCompletedSubtasks = it; Store.save() }
            SheetToggle(p, "将已完成任务移至底部", s.moveCompletedToBottom) { s.moveCompletedToBottom = it; Store.save() }
            SheetRow(p, "已完成", s.completedSort) { completedDialog = true }
        }
    }
    if (groupDialog) {
        OptionDialog(p, "分组", listOf("无", "按截止日期", "按开始日期", "按优先级", "按最后修改", "创建时间", "清单"), {
            s.groupBy = it; Store.save(); groupDialog = false
        }, { groupDialog = false })
    }
    if (sortDialog) {
        OptionDialog(p, "排序", listOf("按截止日期", "按优先级", "按标题", "按最后修改", "创建时间", "清单"), {
            s.sortBy = it; Store.save(); sortDialog = false
        }, { sortDialog = false })
    }
    if (subtaskDialog) {
        OptionDialog(p, "子任务", listOf("我的顺序", "按截止日期", "按优先级"), {
            s.subtaskOrder = it; Store.save(); subtaskDialog = false
        }, { subtaskDialog = false })
    }
    if (completedDialog) {
        OptionDialog(p, "已完成", listOf("按完成时间", "按截止日期", "按优先级"), {
            s.completedSort = it; Store.save(); completedDialog = false
        }, { completedDialog = false })
    }
}

@Composable
fun SheetRow(p: Palette, label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 22.dp, vertical = 17.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = p.textPrimary, fontSize = 17.sp, modifier = Modifier.weight(1f))
        Text(value, color = p.primary, fontSize = 16.sp)
    }
}

@Composable
fun SheetToggle(p: Palette, label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = p.textPrimary, fontSize = 17.sp, modifier = Modifier.weight(1f))
        Switch(checked = value, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF2E6DB4)))
    }
}

// ------------------------------------------------------------------ date picker sheet

@Composable
fun DatePickerSheet(
    p: Palette,
    forStart: Boolean,
    initial: Long?,
    onDismiss: () -> Unit,
    onPick: (Long?, Boolean) -> Unit
) {
    val today = startOfDay(System.currentTimeMillis())
    var shown by remember { mutableStateOf(calOf(initial ?: today)) }
    var selected by remember { mutableStateOf(initial?.let { startOfDay(it) }) }
    var hour by remember { mutableStateOf(initial?.let { calOf(it).get(Calendar.HOUR_OF_DAY) } ?: 9) }
    var minute by remember { mutableStateOf(initial?.let { calOf(it).get(Calendar.MINUTE) } ?: 0) }
    val quick = if (forStart) listOf("截止日期", "截止时间", "截止日期前一天", "截止日期前一周", "无日期")
    else listOf("今天", "明天", "下周四", "无日期")
    val times = listOf("09:00", "13:00", "17:00", "20:00")

    BottomPanel(p, onDismiss) {
        Column(Modifier.fillMaxWidth().padding(bottom = 18.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                Column(Modifier.weight(1f)) {
                    quick.forEach { q ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                when (q) {
                                    "今天" -> selected = today
                                    "明天" -> selected = today + 86400000L
                                    "下周四" -> selected = today + 7 * 86400000L
                                    "无日期" -> { onPick(null, false); onDismiss() }
                                    "截止日期" -> selected = today
                                    "截止日期前一天" -> selected = today - 86400000L
                                    "截止日期前一周" -> selected = today - 7 * 86400000L
                                }
                            }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val ic = when (q) {
                                "截止时间" -> 1
                                "无日期" -> 2
                                else -> 0
                            }
                            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                                when (ic) {
                                    1 -> ClockIcon(p.textPrimary, 22.dp)
                                    2 -> NoEntryGlyph(p.textSecondary, 22.dp)
                                    else -> CalendarStartIcon(p.textPrimary, 22.dp)
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(q, color = p.textPrimary, fontSize = 16.sp)
                        }
                    }
                }
                Column(Modifier.weight(1f)) {
                    times.forEach { t ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                hour = t.substring(0, 2).toInt(); minute = t.substring(3).toInt()
                                if (selected == null) selected = today
                            }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { TimeGlyph(p.textPrimary, 22.dp, t) }
                            Spacer(Modifier.width(12.dp))
                            Text(t, color = p.textPrimary, fontSize = 16.sp)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { ClockIcon(p.textPrimary, 22.dp) }
                        Spacer(Modifier.width(12.dp))
                        Text("挑选时间", color = p.textPrimary, fontSize = 16.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            onPick(selected, false); onDismiss()
                        }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { NoEntryGlyph(p.textSecondary, 22.dp) }
                        Spacer(Modifier.width(12.dp))
                        Text("无时间", color = p.textPrimary, fontSize = 16.sp)
                    }
                }
            }
            MonthCalendar(p, shown, selected, onMonth = { shown = it }, onDay = { selected = it })
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) { GridGlyph(p.textPrimary, 22.dp) }
                Spacer(Modifier.weight(1f))
                Text("取消", color = p.primary, fontSize = 17.sp, modifier = Modifier.clickable { onDismiss() }.padding(horizontal = 16.dp))
                Text(
                    "确定", color = p.primary, fontSize = 17.sp,
                    modifier = Modifier.clickable {
                        val base = selected ?: today
                        val c = calOf(base)
                        c.set(Calendar.HOUR_OF_DAY, hour); c.set(Calendar.MINUTE, minute)
                        onPick(c.timeInMillis, !forStart)
                        onDismiss()
                    }.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
fun NoEntryGlyph(color: Color, size: androidx.compose.ui.unit.Dp) {
    androidx.compose.foundation.Canvas(Modifier.size(size)) {
        val w = this.size.width; val h = this.size.height
        drawCircle(color, w * 0.38f, androidx.compose.ui.geometry.Offset(w / 2, h / 2), style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.09f))
        drawLine(color, androidx.compose.ui.geometry.Offset(w * 0.24f, h * 0.76f), androidx.compose.ui.geometry.Offset(w * 0.76f, h * 0.24f), strokeWidth = w * 0.09f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
}

@Composable
fun TimeGlyph(color: Color, size: androidx.compose.ui.unit.Dp, label: String) {
    androidx.compose.foundation.Canvas(Modifier.size(size)) {
        val w = this.size.width; val h = this.size.height
        drawCircle(color, w * 0.40f, androidx.compose.ui.geometry.Offset(w / 2, h / 2), style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.09f))
        drawLine(color, androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.5f), androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.28f), strokeWidth = w * 0.08f)
        drawLine(color, androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.5f), androidx.compose.ui.geometry.Offset(w * 0.68f, h * 0.5f), strokeWidth = w * 0.08f)
    }
}

@Composable
fun MonthCalendar(p: Palette, shown: Calendar, selected: Long?, onMonth: (Calendar) -> Unit, onDay: (Long) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${shown.get(Calendar.YEAR)}年${shown.get(Calendar.MONTH) + 1}月", color = p.textPrimary, fontSize = 17.sp)
            Spacer(Modifier.width(6.dp))
            ChevronDownGlyph(p.textPrimary, 16.dp)
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(34.dp).clickable {
                val c = calOf(shown.timeInMillis); c.add(Calendar.MONTH, -1); onMonth(c)
            }, contentAlignment = Alignment.Center) { ChevronLeftGlyph(p.textPrimary, 20.dp) }
            Box(Modifier.size(34.dp).clickable {
                val c = calOf(shown.timeInMillis); c.add(Calendar.MONTH, 1); onMonth(c)
            }, contentAlignment = Alignment.Center) { ChevronRight(p.textPrimary, 20.dp) }
        }
        Row(Modifier.fillMaxWidth()) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { d ->
                Text(d, color = p.textSecondary, fontSize = 14.sp, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        val first = calOf(shown.timeInMillis).apply { set(Calendar.DAY_OF_MONTH, 1) }
        val offset = (first.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
        val days = first.getActualMaximum(Calendar.DAY_OF_MONTH)
        val cells = offset + days
        val rows = (cells + 6) / 7
        Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            for (r in 0 until rows) {
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    for (c in 0 until 7) {
                        val index = r * 7 + c
                        val day = index - offset + 1
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            if (day in 1..days) {
                                val ms = calOf(first.timeInMillis).apply { set(Calendar.DAY_OF_MONTH, day) }.timeInMillis
                                val isSel = selected != null && isSameDay(selected, ms)
                                Box(
                                    modifier = Modifier.size(38.dp).clip(CircleShape)
                                        .background(if (isSel) Color.Transparent else Color.Transparent)
                                        .clickable { onDay(ms) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSel) {
                                        Box(Modifier.size(36.dp).clip(CircleShape).background(p.primary.copy(alpha = 0.18f)))
                                    }
                                    Text("$day", color = p.textPrimary, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChevronDownGlyph(color: Color, size: androidx.compose.ui.unit.Dp) {
    androidx.compose.foundation.Canvas(Modifier.size(size)) {
        val w = this.size.width; val h = this.size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.18f, h * 0.34f); lineTo(w * 0.5f, h * 0.68f); lineTo(w * 0.82f, h * 0.34f)
        }
        drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.13f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    }
}

@Composable
fun ChevronLeftGlyph(color: Color, size: androidx.compose.ui.unit.Dp) {
    androidx.compose.foundation.Canvas(Modifier.size(size)) {
        val w = this.size.width; val h = this.size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.66f, h * 0.16f); lineTo(w * 0.30f, h * 0.5f); lineTo(w * 0.66f, h * 0.84f)
        }
        drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.13f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    }
}

// ------------------------------------------------------------------ colour & icon pickers

val COLOR_PALETTE = listOf(
    0xFFE53935, 0xFFD81B60, 0xFF8E24AA, 0xFF5E35B1, 0xFF3949AB,
    0xFF1E88E5, 0xFF039BE5, 0xFF00897B, 0xFF43A047, 0xFF7CB342,
    0xFFF4511E, 0xFFFB8C00, 0xFFFDD835, 0xFF6D4C41, 0xFF546E7A
)

@Composable
fun ColorDialog(p: Palette, current: Long, onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = p.surface, shape = RoundedCornerShape(8.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("颜色", color = p.textPrimary, fontSize = 18.sp, modifier = Modifier.padding(bottom = 12.dp))
                for (r in 0 until 3) {
                    Row(Modifier.padding(vertical = 6.dp)) {
                        for (c in 0 until 5) {
                            val col = COLOR_PALETTE[r * 5 + c]
                            Box(
                                modifier = Modifier.padding(horizontal = 6.dp).size(40.dp).clip(CircleShape)
                                    .background(Color(col)).clickable { onPick(col) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (col == current) CheckGlyph(Color.White, 22.dp)
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.End) {
                    Text("自定义", color = p.primary, fontSize = 16.sp, modifier = Modifier.clickable { onDismiss() }.padding(8.dp))
                }
            }
        }
    }
}

val ICON_CHOICES = listOf("shopping", "list", "home", "tag", "flag", "clock", "place", "palette")

@Composable
fun IconDialog(p: Palette, current: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = p.surface, shape = RoundedCornerShape(8.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("图标", color = p.textPrimary, fontSize = 18.sp, modifier = Modifier.padding(bottom = 12.dp))
                Text("搜索", color = p.textSecondary, fontSize = 15.sp, modifier = Modifier.padding(bottom = 10.dp))
                Text("Action", color = p.textSecondary, fontSize = 14.sp, modifier = Modifier.padding(bottom = 6.dp))
                for (r in 0 until 2) {
                    Row(Modifier.padding(vertical = 6.dp)) {
                        for (c in 0 until 4) {
                            val idx = r * 4 + c
                            val name = ICON_CHOICES[idx]
                            Box(
                                modifier = Modifier.padding(horizontal = 6.dp).size(44.dp).clip(CircleShape)
                                    .background(if (name == current) p.primary.copy(alpha = 0.2f) else Color.Transparent)
                                    .clickable { onPick(name) },
                                contentAlignment = Alignment.Center
                            ) { IconGlyph(name, p.textPrimary) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CartGlyph(color: Color, size: androidx.compose.ui.unit.Dp = 22.dp) {
    androidx.compose.foundation.Canvas(Modifier.size(size)) {
        val w = this.size.width; val h = this.size.height
        val cap = androidx.compose.ui.graphics.StrokeCap.Round
        drawLine(color, androidx.compose.ui.geometry.Offset(w * 0.10f, h * 0.20f), androidx.compose.ui.geometry.Offset(w * 0.30f, h * 0.20f), strokeWidth = w * 0.10f, cap = cap)
        drawLine(color, androidx.compose.ui.geometry.Offset(w * 0.30f, h * 0.20f), androidx.compose.ui.geometry.Offset(w * 0.46f, h * 0.62f), strokeWidth = w * 0.09f, cap = cap)
        drawLine(color, androidx.compose.ui.geometry.Offset(w * 0.46f, h * 0.62f), androidx.compose.ui.geometry.Offset(w * 0.86f, h * 0.62f), strokeWidth = w * 0.09f, cap = cap)
        drawLine(color, androidx.compose.ui.geometry.Offset(w * 0.36f, h * 0.34f), androidx.compose.ui.geometry.Offset(w * 0.82f, h * 0.34f), strokeWidth = w * 0.09f, cap = cap)
        drawCircle(color, w * 0.075f, androidx.compose.ui.geometry.Offset(w * 0.50f, h * 0.82f))
        drawCircle(color, w * 0.075f, androidx.compose.ui.geometry.Offset(w * 0.78f, h * 0.82f))
    }
}

@Composable
fun IconGlyph(name: String, color: Color) {
    when (name) {
        "list" -> ListGlyphSmall(color)
        "shopping" -> CartGlyph(color)
        "home" -> HomeGlyph(color, 22.dp)
        "tag" -> TagIcon(color, 22.dp)
        "flag" -> FlagIcon(color, 22.dp)
        "clock" -> ClockIcon(color, 22.dp)
        "place" -> PlaceGlyph(color, 22.dp)
        "palette" -> PaletteIcon(color, 22.dp)
        else -> FilterIcon(color, 22.dp)
    }
}
