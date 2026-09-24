package com.blackboxbench.reproduction

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ------------------------------------------------------------------ router

sealed class Nav {
    object Welcome : Nav()
    object Accounts : Nav()
    data class Main(val viewKey: String) : Nav()
    data class Edit(val taskId: String?, val listId: String?) : Nav()
    data class TagPick(val taskId: String) : Nav()
    data class ListEdit(val listId: String?) : Nav()
    data class TagEdit(val tagId: String?) : Nav()
    object FilterPresets : Nav()
    data class FilterEdit(val preset: String?) : Nav()
    data class Search(val from: String) : Nav()
    data class Settings(val page: String) : Nav()
}

class Router(start: Nav) {
    val stack = mutableStateListOfInternal()
    var current by mutableStateOf(start)
        private set

    fun push(n: Nav) {
        stack.add(current)
        current = n
    }

    fun reset(n: Nav) {
        stack.clear()
        current = n
    }

    fun pop() {
        if (stack.isNotEmpty()) {
            current = stack.removeAt(stack.size - 1)
        }
    }

    val canPop: Boolean get() = stack.isNotEmpty()
}

@Suppress("FunctionName")
private fun mutableStateListOfInternal() = androidx.compose.runtime.mutableStateListOf<Nav>()

// ------------------------------------------------------------------ shared bits

@Composable
fun TopBar(
    p: Palette,
    title: String,
    titleColor: Color = p.primary,
    onBack: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp).background(p.surface).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            Box(modifier = Modifier.size(44.dp).clickable { onBack() }, contentAlignment = Alignment.Center) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(22.dp)) {
                    val w = size.width; val h = size.height
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(w * 0.66f, h * 0.14f); lineTo(w * 0.28f, h * 0.5f); lineTo(w * 0.66f, h * 0.86f)
                    }
                    drawPath(path, p.textPrimary, style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.12f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                }
            }
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = title,
            color = titleColor,
            fontSize = 26.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        trailing()
    }
}

@Composable
fun Chip(text: String, p: Palette, bg: Color = p.chipBg, fg: Color = p.textPrimary, icon: (@Composable () -> Unit)? = null) {
    Surface(color = bg, shape = RoundedCornerShape(6.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                icon()
                Spacer(Modifier.width(4.dp))
            }
            Text(text, color = fg, fontSize = 13.sp, maxLines = 1)
        }
    }
}

@Composable
fun Divider(p: Palette) {
    Box(Modifier.fillMaxWidth().height(1.dp).background(p.divider))
}

@Composable
fun CircleCheck(
    checked: Boolean,
    color: Color,
    size: androidx.compose.ui.unit.Dp = 24.dp,
    dashed: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.size(size).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(size * 0.78f)) {
            val w = this.size.width
            val h = this.size.height
            if (checked) {
                drawRoundRect(
                    color = color, cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.22f)
                )
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.22f, h * 0.52f); lineTo(w * 0.42f, h * 0.72f); lineTo(w * 0.78f, h * 0.28f)
                }
                drawPath(path, Color.White, style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.13f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            } else if (dashed) {
                drawRoundRect(
                    color = color.copy(alpha = 0.55f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.22f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = w * 0.10f,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(w * 0.16f, w * 0.14f))
                    )
                )
            } else {
                drawRoundRect(
                    color = color,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.22f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.11f)
                )
            }
        }
    }
}

/** One task row, matching the observed list rendering. */
@Composable
fun TaskRow(
    task: Task,
    p: Palette,
    viewKey: String,
    selected: Boolean,
    selectionMode: Boolean,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onLongPress: () -> Unit,
    onToggleSubtask: (SubTask) -> Unit,
    onSelect: () -> Unit
) {
    val titleColor = when {
        task.completed -> p.textSecondary
        task.startAt != null && task.startAt!! > System.currentTimeMillis() -> p.textSecondary
        else -> p.textPrimary
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) p.primary.copy(alpha = 0.12f) else Color.Transparent)
            .pointerInput(task.id, selectionMode) {
                detectTapGestures(
                    onTap = { if (selectionMode) onSelect() else onOpen() },
                    onLongPress = { onLongPress() }
                )
            }
            .padding(start = 6.dp, end = 14.dp, top = 8.dp, bottom = 6.dp)
    ) {
        Row {
            Box(modifier = Modifier.width(46.dp).height(26.dp), contentAlignment = Alignment.Center) {
                val unstarted = task.startAt != null && task.startAt!! > System.currentTimeMillis()
                if (task.isRepeating && !task.completed) {
                    Box(modifier = Modifier.size(26.dp).clickable { onToggle() }, contentAlignment = Alignment.Center) {
                        RepeatIcon(color = priorityColor(task.priority), size = 22.dp)
                    }
                } else {
                    CircleCheck(
                        checked = task.completed, color = priorityColor(task.priority),
                        size = 26.dp, dashed = unstarted, onClick = onToggle
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = task.title,
                        color = titleColor,
                        fontSize = 17.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (task.completed) TextDecoration.LineThrough else null,
                        modifier = Modifier.weight(1f)
                    )
                    if (task.dueAt != null) {
                        val overdue = task.dueAt!! < System.currentTimeMillis() && !task.completed
                        Text(
                            text = dueLabel(task.dueAt!!, task.hasDueTime, Store.settings.showFullDate),
                            color = if (task.completed) p.textSecondary else if (overdue) Color(0xFFE8710A) else Color(0xFFE8710A),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                if (task.notes.isNotBlank()) {
                    Text(
                        text = task.notes,
                        color = if (task.completed) p.textSecondary else p.textPrimary.copy(alpha = 0.72f),
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (task.subtasks.isNotEmpty() || (!isListView(viewKey) && task.listId.isNotBlank()) ||
                    (!isTagView(viewKey) && task.tagIds.isNotEmpty())
                ) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (task.subtasks.isNotEmpty()) {
                            Chip(
                                text = "${task.openSubtaskCount}",
                                p = p,
                                icon = { ChevronUp(color = p.textSecondary, size = 14.dp) }
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        if (!isListView(viewKey) && task.listId.isNotBlank()) {
                            val lc = chipColor(p, Store.listById(task.listId)?.color ?: 0xFF3F51B5)
                            Chip(
                                text = Store.listName(task.listId), p = p,
                                icon = { ListGlyphSmall(lc) }
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        if (!isTagView(viewKey)) {
                            task.tagIds.forEach { id ->
                                Store.tagById(id)?.let { tag ->
                                    val tc = chipColor(p, tag.color)
                                    Chip(text = tag.name, p = p, icon = { TagIcon(color = tc, size = 15.dp) })
                                    Spacer(Modifier.width(6.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
        task.subtasks.forEach { s ->
            if (s.done && !Store.settings.showCompletedSubtasks) return@forEach
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 46.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleCheck(
                    checked = s.done, color = priorityColor(task.priority), size = 24.dp,
                    onClick = { onToggleSubtask(s) }
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = s.title,
                    color = if (s.done) p.textSecondary else p.textPrimary,
                    fontSize = 15.sp,
                    textDecoration = if (s.done) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ListGlyphSmall(color: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(13.dp)) {
        val w = size.width; val h = size.height
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.14f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(color, androidx.compose.ui.geometry.Offset(w * 0.10f, h * 0.22f), androidx.compose.ui.geometry.Offset(w * 0.90f, h * 0.22f), strokeWidth = w * 0.14f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(color, androidx.compose.ui.geometry.Offset(w * 0.10f, h * 0.52f), androidx.compose.ui.geometry.Offset(w * 0.90f, h * 0.52f), strokeWidth = w * 0.14f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(color, androidx.compose.ui.geometry.Offset(w * 0.10f, h * 0.82f), androidx.compose.ui.geometry.Offset(w * 0.60f, h * 0.82f), strokeWidth = w * 0.14f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
}

fun isListView(viewKey: String): Boolean = viewKey.startsWith("list:")
fun isTagView(viewKey: String): Boolean = viewKey.startsWith("tag:")
fun isFilterView(viewKey: String): Boolean = viewKey.startsWith("filter:")

fun viewTitle(viewKey: String): String = when {
    viewKey == "my" -> "我的任务"
    viewKey == "today" -> "今天"
    viewKey == "recent" -> "最近修改过的"
    viewKey.startsWith("tag:") -> Store.tagById(viewKey.removePrefix("tag:"))?.name ?: "标签"
    viewKey.startsWith("list:") -> Store.listById(viewKey.removePrefix("list:"))?.name ?: "清单"
    viewKey.startsWith("filter:") -> Store.filterById(viewKey.removePrefix("filter:"))?.name ?: "过滤器"
    else -> "我的任务"
}

fun viewColor(viewKey: String): Color = when {
    viewKey.startsWith("tag:") -> Color(Store.tagById(viewKey.removePrefix("tag:"))?.color ?: 0xFF3F51B5)
    viewKey.startsWith("list:") -> Color(Store.listById(viewKey.removePrefix("list:"))?.color ?: 0xFF3F51B5)
    viewKey.startsWith("filter:") -> Color(Store.filterById(viewKey.removePrefix("filter:"))?.color ?: 0xFF3F51B5)
    else -> Color(0xFF2E6DB4)
}

// ------------------------------------------------------------------ welcome

@Composable
fun WelcomeScreen(p: Palette, onContinue: () -> Unit, onAccounts: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(p.surface),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(72.dp))
        Box(
            modifier = Modifier.size(84.dp).clip(CircleShape).background(Color(0xFF3B78C3)),
            contentAlignment = Alignment.Center
        ) {
            CheckGlyph(color = Color.White, size = 44.dp)
        }
        Spacer(Modifier.height(18.dp))
        Text("Tasks", color = Color(0xFF3B78C3), fontSize = 40.sp)
        Spacer(Modifier.height(14.dp))
        Text(
            "一个开源、基于 Astrid 的待办事项应用",
            color = p.textPrimary, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 40.dp)
        )
        Spacer(Modifier.weight(1f))
        OutlinedWideButton("添加账号", p, onAccounts)
        Spacer(Modifier.height(14.dp))
        OutlinedWideButton("继续但不同步", p, onContinue)
        Spacer(Modifier.height(14.dp))
        OutlinedWideButton("导入 Tasks.org 备份", p) { }
        Spacer(Modifier.height(48.dp))
    }
}

@Composable
fun OutlinedWideButton(text: String, p: Palette, onClick: () -> Unit) {
    Surface(
        color = p.surface,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF3B78C3)),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(50.dp).clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, color = Color(0xFF2E5F9E), fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun AccountsScreen(p: Palette, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(p.surface)) {
        TopBar(p, "添加账号", onBack = onBack)
        val accounts = listOf(
            "Tasks.org Cloud" to "与 Tasks.org 同步",
            "Microsoft To Do" to "使用微软账号登录",
            "Google Tasks" to "使用 Google 账号登录",
            "DAVx⁵" to "通过 DAVx⁵ 同步",
            "CalDAV" to "连接到 CalDAV 服务器",
            "EteSync" to "端到端加密同步",
            "DecSync CC" to "通过 DecSync CC 同步"
        )
        LazyColumn(Modifier.fillMaxSize()) {
            items(accounts) { row ->
                Column(Modifier.fillMaxWidth().clickable { }.padding(horizontal = 20.dp, vertical = 14.dp)) {
                    Text(row.first, color = p.textPrimary, fontSize = 17.sp)
                    Text(row.second, color = p.textSecondary, fontSize = 14.sp)
                }
                Divider(p)
            }
        }
    }
}
