package com.blackboxbench.reproduction.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blackboxbench.reproduction.BlueColor
import com.blackboxbench.reproduction.CustomFilter
import com.blackboxbench.reproduction.Prefs
import com.blackboxbench.reproduction.StateStore
import com.blackboxbench.reproduction.ViewRef
import com.blackboxbench.reproduction.condLabel
import com.blackboxbench.reproduction.matchesFilter
import com.blackboxbench.reproduction.newId
import com.blackboxbench.reproduction.priorityName

// ---------------- shared small field rows ----------------

@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 2.dp)
    )
}

@Composable
private fun PickerRow(label: String, trailing: @Composable () -> Unit, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 16.sp, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
private fun GrayPlaceholderRow(text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BottomSaveButton(enabled: Boolean, onSave: () -> Unit) {
    Button(
        onClick = onSave,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().padding(16.dp).height(48.dp),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = BlueColor,
            disabledContainerColor = Color(0xFFD6DAE4)
        )
    ) {
        Text("保存", fontSize = 16.sp)
    }
}

// ---------------- New / edit tag ----------------

@Composable
fun NewTagScreen(
    tagId: String?,
    onBack: () -> Unit,
    onSaved: (String) -> Unit
) {
    val existing = tagId?.let { id -> StateStore.tags.firstOrNull { it.id == id } }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var color by remember { mutableStateOf(existing?.color) }
    var icon by remember { mutableStateOf(existing?.icon) }
    var showColor by remember { mutableStateOf(false) }
    var showIcon by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        SimpleTopBar(title = if (existing != null) "标签设置" else "新建标签", onBack = onBack)
        Column(Modifier.verticalScroll(rememberScrollState())) {
            FieldLabel("显示名称")
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                singleLine = true
            )
            PickerRow(
                label = "颜色",
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ColorDot(color?.let { Color(it) }, 24)
                        if (color != null) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "⨉",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.clickable { color = null }.padding(4.dp)
                            )
                        }
                    }
                },
                onClick = { showColor = true }
            )
            PickerRow(
                label = "图标",
                trailing = {
                    if (icon != null) Text(icon!!, fontSize = 22.sp)
                    else Text("∅", color = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                onClick = { showIcon = true }
            )
            GrayPlaceholderRow("添加快捷方式到主屏幕")
            GrayPlaceholderRow("添加小部件到主屏幕")
            BottomSaveButton(enabled = name.isNotBlank()) {
                val id = if (existing != null) {
                    existing.name = name
                    existing.color = color
                    existing.icon = icon
                    existing.id
                } else {
                    val tid = newId()
                    StateStore.tags.add(com.blackboxbench.reproduction.Tag(tid, name, color, icon))
                    tid
                }
                StateStore.save()
                onSaved(id)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (showColor) {
        ColorPickerDialog(current = color, onSelect = { color = it }, onDismiss = { showColor = false })
    }
    if (showIcon) {
        IconPickerDialog(current = icon, onSelect = { icon = it }, onDismiss = { showIcon = false })
    }
}

// ---------------- New list ----------------

@Composable
fun NewListScreen(
    onBack: () -> Unit,
    onSaved: (String) -> Unit
) {
    var name by remember { mutableStateOf("新建清单") }
    var color by remember { mutableStateOf<Int?>(null) }
    var icon by remember { mutableStateOf<String?>(null) }
    var showColor by remember { mutableStateOf(false) }
    var showIcon by remember { mutableStateOf(false) }
    var showOffline by remember { mutableStateOf(false) }
    var bannerClosed by remember { mutableStateOf(Prefs.bool("list_banner_closed")) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        SimpleTopBar(title = "新建清单", onBack = onBack)
        Column(Modifier.verticalScroll(rememberScrollState())) {
            if (!bannerClosed) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("这是本地清单", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "此清单中的任务只存储在这台设备上。连接账号保护数据，在任何地方访问它。",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
                            TextButton(onClick = {
                                bannerClosed = true
                                Prefs.putBool("list_banner_closed", true)
                            }) { Text("关闭", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            TextButton(onClick = { showOffline = true }) { Text("添加账号", color = BlueColor) }
                        }
                    }
                }
            }
            FieldLabel("显示名称")
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                singleLine = true
            )
            PickerRow(
                label = "颜色",
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ColorDot(color?.let { Color(it) }, 24)
                        if (color != null) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "⨉",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.clickable { color = null }.padding(4.dp)
                            )
                        }
                    }
                },
                onClick = { showColor = true }
            )
            PickerRow(
                label = "图标",
                trailing = {
                    if (icon != null) Text(icon!!, fontSize = 22.sp)
                    else Text("∅", color = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                onClick = { showIcon = true }
            )
            GrayPlaceholderRow("添加快捷方式到主屏幕")
            GrayPlaceholderRow("添加小部件到主屏幕")
            BottomSaveButton(enabled = name.isNotBlank() && name != "新建清单") {
                val id = newId()
                StateStore.lists.add(com.blackboxbench.reproduction.TaskList(id, name, color, icon, false))
                StateStore.save()
                onSaved(id)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (showColor) {
        ColorPickerDialog(current = color, onSelect = { color = it }, onDismiss = { showColor = false })
    }
    if (showIcon) {
        IconPickerDialog(current = icon, onSelect = { icon = it }, onDismiss = { showIcon = false })
    }
    if (showOffline) {
        OfflineAccountDialog(onDismiss = { showOffline = false })
    }
}

// ---------------- New filter ----------------

@Composable
fun NewFilterScreen(
    onBack: () -> Unit,
    onSaved: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf<Int?>(null) }
    var icon by remember { mutableStateOf<String?>(null) }
    val conditions = remember { mutableStateListOf<String>() }
    var showColor by remember { mutableStateOf(false) }
    var showIcon by remember { mutableStateOf(false) }
    var showCondTypes by remember { mutableStateOf(false) }
    var dirty by remember { mutableStateOf(false) }
    var showDiscard by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var subDialog by remember { mutableStateOf("") }

    fun markDirty() { dirty = true }

    androidx.activity.compose.BackHandler {
        if (dirty) showDiscard = true else onBack()
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (name.isNotBlank()) {
                    val id = newId()
                    StateStore.filters.add(CustomFilter(id, name, color, icon, conditions.toList()))
                    StateStore.save()
                    onSaved(id)
                }
            }) {
                Text("✓", fontSize = 22.sp, color = if (name.isNotBlank()) BlueColor else Color(0xFFB9BDC7), fontWeight = FontWeight.Bold)
            }
            Text(
                "新建过滤器",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            IconButton(onClick = { showHelp = true }) {
                Text("?", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            }
        }

        Column(Modifier.verticalScroll(rememberScrollState())) {
            FieldLabel("显示名称")
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; markDirty() },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                singleLine = true
            )
            PickerRow(
                label = "颜色",
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ColorDot(color?.let { Color(it) }, 24)
                        if (color != null) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "⨉",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.clickable { color = null; markDirty() }.padding(4.dp)
                            )
                        }
                    }
                },
                onClick = { showColor = true }
            )
            PickerRow(
                label = "图标",
                trailing = {
                    if (icon != null) Text(icon!!, fontSize = 22.sp)
                    else Text("∅", color = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                onClick = { showIcon = true }
            )

            FieldLabel("过滤条件")
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(Modifier.padding(12.dp)) {
                    val probe = CustomFilter("probe", "", null, null, conditions.toList())
                    val count = StateStore.tasks.count { matchesFilter(probe, it) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("我的任务", fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Text(count.toString(), fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    conditions.forEach { c ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Text(
                                    condLabel(c),
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "⨉",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.clickable {
                                    conditions.remove(c)
                                    markDirty()
                                }.padding(4.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
                        TextButton(onClick = { showCondTypes = true }) {
                            Text("+ 添加条件", color = BlueColor)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showColor) {
        ColorPickerDialog(current = color, onSelect = { color = it; markDirty() }, onDismiss = { showColor = false })
    }
    if (showIcon) {
        IconPickerDialog(current = icon, onSelect = { icon = it; markDirty() }, onDismiss = { showIcon = false })
    }
    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text("过滤器") },
            text = { Text("过滤器可根据标签、日期、优先级等条件组合筛选任务。") },
            confirmButton = {
                TextButton(onClick = { showHelp = false }) { Text("确定", color = BlueColor) }
            }
        )
    }
    if (showDiscard) {
        ConfirmDialog(
            title = "是否放弃修改？",
            onConfirm = onBack,
            onDismiss = { showDiscard = false }
        )
    }
    if (showCondTypes) {
        AlertDialog(
            onDismissRequest = { showCondTypes = false },
            title = { Text("添加条件") },
            text = {
                Column {
                    listOf(
                        "标签…" to "tag",
                        "标签名包含…" to "tagname",
                        "开始于…" to "start",
                        "截止于…" to "due",
                        "优先级…" to "priority",
                        "标题含…" to "title",
                        "在某清单中…" to "list",
                        "重复" to "repeat",
                        "已完成" to "completed",
                        "尚未开始" to "notstarted",
                        "有子任务" to "subtasks",
                        "是子任务" to "issubtask",
                        "有提醒" to "reminder"
                    ).forEach { (label, key) ->
                        Text(
                            label,
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showCondTypes = false
                                    when (key) {
                                        "tag", "priority", "list", "tagname", "title" -> subDialog = key
                                        else -> {
                                            conditions.add(key)
                                            markDirty()
                                        }
                                    }
                                }
                                .padding(vertical = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCondTypes = false }) { Text("取消") }
            }
        )
    }
    if (subDialog.isNotEmpty()) {
        when (subDialog) {
            "tag" -> AlertDialog(
                onDismissRequest = { subDialog = "" },
                title = { Text("标签") },
                text = {
                    Column {
                        StateStore.tags.forEach { t ->
                            Text(
                                t.name,
                                Modifier.fillMaxWidth().clickable {
                                    conditions.add("tag|" + t.id)
                                    markDirty()
                                    subDialog = ""
                                }.padding(vertical = 10.dp)
                            )
                        }
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = { subDialog = "" }) { Text("取消") } }
            )
            "priority" -> AlertDialog(
                onDismissRequest = { subDialog = "" },
                title = { Text("优先级") },
                text = {
                    Column {
                        listOf(3, 2, 1, 0).forEach { p ->
                            Text(
                                priorityName(p),
                                Modifier.fillMaxWidth().clickable {
                                    conditions.add("priority|$p")
                                    markDirty()
                                    subDialog = ""
                                }.padding(vertical = 10.dp)
                            )
                        }
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = { subDialog = "" }) { Text("取消") } }
            )
            "list" -> AlertDialog(
                onDismissRequest = { subDialog = "" },
                title = { Text("清单") },
                text = {
                    Column {
                        StateStore.lists.forEach { l ->
                            Text(
                                l.name,
                                Modifier.fillMaxWidth().clickable {
                                    conditions.add("list|" + l.id)
                                    markDirty()
                                    subDialog = ""
                                }.padding(vertical = 10.dp)
                            )
                        }
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = { subDialog = "" }) { Text("取消") } }
            )
            "tagname" -> TextInputCondDialog(
                title = "标签名包含",
                onDone = { conditions.add("tagname|$it"); markDirty() },
                onDismiss = { subDialog = "" }
            )
            "title" -> TextInputCondDialog(
                title = "标题含",
                onDone = { conditions.add("title|$it"); markDirty() },
                onDismiss = { subDialog = "" }
            )
        }
    }
}

@Composable
private fun TextInputCondDialog(title: String, onDone: (String) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(value = value, onValueChange = { value = it }, singleLine = true)
        },
        confirmButton = {
            TextButton(onClick = { if (value.isNotBlank()) onDone(value); onDismiss() }) {
                Text("确定", color = BlueColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

// ---------------- Location picker ----------------

@Composable
fun LocationPickScreen(onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize().background(Color(0xFFF5F1E8))) {
        // grid placeholder
        Canvas(Modifier.fillMaxSize()) {
            val step = 44.dp.toPx()
            var x = 0f
            while (x < size.width) {
                drawLine(Color(0xFFE3DDCF), Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                x += step
            }
            var y = 0f
            while (y < size.height) {
                drawLine(Color(0xFFE3DDCF), Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                y += step
            }
        }
        // red pin
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Canvas(Modifier.size(48.dp)) {
                    val w = size.width
                    val r = w * 0.32f
                    drawCircle(Color(0xFFD64545), radius = r, center = Offset(w / 2f, w / 2f))
                    drawCircle(Color.White, radius = r * 0.45f, center = Offset(w / 2f, w / 2f))
                }
            }
        }
        // top search
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 4.dp
        ) {
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("🔍 搜索") },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }
        // locate FAB
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 120.dp)
                .size(48.dp),
            shape = CircleShape,
            color = BlueColor,
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("◎", color = Color.White, fontSize = 22.sp)
            }
        }
        // bottom bar
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onBack() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                ) {
                    Text("📍", fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("选择此位置", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Text(
                "© OpenStreetMap contributors",
                fontSize = 11.sp,
                color = Color(0xFF8A8A8A),
                modifier = Modifier
                    .align(Alignment.Start)
                    .background(Color(0xCCF5F1E8))
                    .padding(start = 8.dp, top = 2.dp, bottom = 2.dp)
            )
        }
    }
}

// ---------------- Tag select (from task editor) ----------------

@Composable
fun TagSelectScreen(
    initial: List<String>,
    onDone: (List<String>) -> Unit
) {
    val selected = remember { mutableStateListOf<String>().apply { addAll(initial) } }
    var query by remember { mutableStateOf("") }

    androidx.activity.compose.BackHandler {
        onDone(selected.toList())
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("输入标签名称", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(
                "完成",
                color = BlueColor,
                fontSize = 15.sp,
                modifier = Modifier.clickable { onDone(selected.toList()) }.padding(4.dp)
            )
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text("搜索") },
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        val shown = StateStore.tags.filter { query.isBlank() || it.name.contains(query, true) }
        Column(Modifier.verticalScroll(rememberScrollState())) {
            shown.forEach { tag ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (selected.contains(tag.id)) selected.remove(tag.id) else selected.add(tag.id)
                        }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(tag.name, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    if (selected.contains(tag.id)) {
                        Text("✓", color = BlueColor, fontSize = 18.sp)
                    }
                }
            }
            if (query.isNotBlank() && shown.none { it.name == query }) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            val id = newId()
                            StateStore.tags.add(com.blackboxbench.reproduction.Tag(id, query, null, null))
                            StateStore.save()
                            selected.add(id)
                        }
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text("创建 \"$query\"", color = BlueColor, fontSize = 15.sp)
                }
            }
        }
    }
}

// ---------------- simple top bar ----------------

@Composable
fun SimpleTopBar(title: String, onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Text("←", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
        }
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}
