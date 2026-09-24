package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val CATEGORY_LABELS = listOf("歌曲", "专辑", "艺术家", "音乐类型", "播放列表")

private val PALETTE = listOf(
    0xFFF44336, 0xFFE91E63, 0xFF9C27B0, 0xFF673AB7, 0xFF3F51B5,
    0xFF2196F3, 0xFF03A9F4, 0xFF00BCD4, 0xFF009688, 0xFF4CAF50,
    0xFF8BC34A, 0xFFCDDC39, 0xFFFFEB3B, 0xFFFFC107, 0xFFFF9800,
    0xFFFF5722, 0xFF795548, 0xFF9E9E9E, 0xFF607D8B, 0xFF000000,
).map { it.toInt() }

@Composable
fun AppDialogs(state: AppState) {
    when (state.dialog) {
        DialogKind.DETAILS -> {
            val track = Library.byId(state.detailsTrackId ?: "")
            if (track != null) DetailsDialog(track) { state.dialog = null }
        }
        DialogKind.SLEEP_TIMER -> SleepTimerDialog(state) { state.dialog = null }
        DialogKind.CATEGORIES -> CategoriesDialog(state) { state.dialog = null }
        DialogKind.THEME -> ThemeDialog(state) { state.dialog = null }
        DialogKind.PRIMARY_COLOR -> ColorPickerDialog(
            state, state.primaryColor
        ) { c -> state.primaryColor = c; state.persist() }
        DialogKind.ACCENT_COLOR -> ColorPickerDialog(
            state, state.accentColor
        ) { c -> state.accentColor = c; state.persist() }
        DialogKind.CREATE_PLAYLIST -> NameDialog(
            title = "新建播放列表", initial = "", confirmLabel = "创建"
        ) { name ->
            val p = state.createPlaylist(name)
            state.toast("已创建播放列表 ${p.name}")
            state.dialog = null
        }
        DialogKind.SAVE_QUEUE -> NameDialog(
            title = "新建播放列表", initial = "", confirmLabel = "创建"
        ) { name ->
            val ids = state.queue.toList()
            state.createPlaylist(name, ids)
            state.toast("${ids.size} 首歌曲已加入到播放列表 $name。")
            state.dialog = null
        }
        DialogKind.RENAME_PLAYLIST -> {
            val p = state.playlist(state.openPlaylistId)
            NameDialog(
                title = "重命名播放列表", initial = p?.name ?: "", confirmLabel = "重命名"
            ) { name ->
                state.renamePlaylist(state.openPlaylistId ?: "", name)
                state.dialog = null
            }
        }
        DialogKind.ADD_TO_PLAYLIST -> AddToPlaylistDialog(state) { state.dialog = null }
        DialogKind.EQUALIZER -> EqualizerDialog(state) { state.dialog = null }
        DialogKind.INFO -> AlertDialog(
            onDismissRequest = { state.dialog = null },
            title = { Text("提示") },
            text = { Text(state.infoText) },
            confirmButton = { TextButton(onClick = { state.dialog = null }) { Text("确定") } }
        )
        else -> Unit
    }
}

// ------------------------------------------------------------------ details

@Composable
private fun DetailsDialog(track: Track, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("详情", fontSize = 20.sp) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                DetailLine("文件路径", track.path)
                DetailLine("文件大小", "%.2f MB".format(track.sizeBytes / 1024f / 1024f))
                DetailLine("格式", "Mp3")
                DetailLine("比特率", "128 kb/s")
                DetailLine("采样率", "44100 Hz")
                Spacer(Modifier.height(10.dp))
                Text("Added: Mon Sep 21 23:35:50 GMT+08:00 2026", fontSize = 13.sp)
                Text("Modified: Sun Sep 20 12:17:02 GMT+08:00 2026", fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                DetailLine("音轨", track.trackNo.toString())
                DetailLine("光碟号", track.discNo.toString())
                DetailLine("标题", track.title)
                DetailLine("艺术家", track.artist)
                DetailLine("专辑", track.album)
                DetailLine("专辑艺术家", track.albumArtist)
                DetailLine("流派", track.genre)
                DetailLine("年份", track.year.ifEmpty { "-" })
                DetailLine("长度", track.durationLabel)
                DetailLine("ReplayGain", "- -")
                DetailLine("ReplayGain peak", "- -")
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("确定") } }
    )
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.padding(vertical = 2.dp)) {
        Text("$label: ", fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 13.sp)
    }
}

// -------------------------------------------------------------- sleep timer

@Composable
private fun SleepTimerDialog(state: AppState, onClose: () -> Unit) {
    var minutes by remember { mutableStateOf(if (state.sleepTimerMinutes > 0) state.sleepTimerMinutes else 30) }
    var afterCurrent by remember { mutableStateOf(state.sleepAfterCurrent) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("睡眠定时器") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$minutes 分", fontSize = 26.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier
                        .size(220.dp)
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                val c = Offset(size.width / 2f, size.height / 2f)
                                val v = change.position
                                val dx = v.x - c.x
                                val dy = v.y - c.y
                                var deg = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                if (deg < 0) deg += 360f
                                val value = ((deg / 360f) * 120f).toInt()
                                minutes = ((value + 2) / 5) * 5
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Canvas(Modifier.size(200.dp)) {
                        val stroke = 10f
                        val radius = size.minDimension / 2f - stroke
                        drawCircle(Color(0xFFDDDDDD), radius, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
                        val sweep = (minutes / 120f) * 360f
                        drawArc(
                            color = Color(state.accentColor),
                            startAngle = -90f, sweepAngle = sweep, useCenter = false,
                            topLeft = Offset(size.width / 2f - radius, size.height / 2f - radius),
                            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        )
                    }
                    Text("$minutes", fontSize = 34.sp)
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = afterCurrent,
                        onCheckedChange = { afterCurrent = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color(state.accentColor))
                    )
                    Text("播完当前音乐", fontSize = 15.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                state.sleepTimerMinutes = minutes
                state.sleepRemainingMs = minutes * 60_000L
                state.sleepAfterCurrent = afterCurrent
                state.toast("睡眠定时器已设置：$minutes 分")
                onClose()
            }) { Text("设置") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("取消") } }
    )
}

// --------------------------------------------------------------- categories

@Composable
private fun CategoriesDialog(state: AppState, onClose: () -> Unit) {
    val order = remember { mutableStateListOf<Int>().also { it.addAll(state.categoryOrder) } }
    val enabled = remember { mutableStateListOf<Boolean>().also { it.addAll(state.categoryEnabled) } }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("媒体库类别") },
        text = {
            Column {
                order.forEachIndexed { position, category ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = enabled[category],
                            onCheckedChange = { enabled[category] = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(state.accentColor))
                        )
                        Text(CATEGORY_LABELS[category], fontSize = 16.sp, modifier = Modifier.weight(1f))
                        Column {
                            Box(
                                Modifier
                                    .size(28.dp)
                                    .clickable {
                                        if (position > 0) {
                                            val tmp = order[position]; order[position] = order[position - 1]; order[position - 1] = tmp
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) { ChevronUpGlyph(MaterialTheme.colorScheme.onSurfaceVariant, 16.dp) }
                            Box(
                                Modifier
                                    .size(28.dp)
                                    .clickable {
                                        if (position < order.size - 1) {
                                            val tmp = order[position]; order[position] = order[position + 1]; order[position + 1] = tmp
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) { ChevronDownGlyph(MaterialTheme.colorScheme.onSurfaceVariant, 16.dp) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                state.categoryOrder.clear(); state.categoryOrder.addAll(order)
                state.categoryEnabled.clear(); state.categoryEnabled.addAll(enabled)
                state.persist()
                onClose()
            }) { Text("确定") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    order.clear(); order.addAll(listOf(0, 1, 2, 3, 4))
                    enabled.clear(); enabled.addAll(listOf(true, true, true, true, true))
                }) { Text("重置") }
                TextButton(onClick = onClose) { Text("取消") }
            }
        }
    )
}

private fun Modifier.androidxRotate(): Modifier = this

// -------------------------------------------------------------------- theme

@Composable
private fun ThemeDialog(state: AppState, onClose: () -> Unit) {
    val options = listOf(
        "浅色", "暗色", "黑色（AMOLED）",
        "Follow system theme\n浅色 · 暗色",
        "Follow system theme\n浅色 · 黑色（AMOLED）"
    )
    var selected by remember { mutableStateOf(state.themeMode) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("全局主题") },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                options.forEachIndexed { index, label ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { selected = index }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == index,
                            onClick = { selected = index },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(state.accentColor))
                        )
                        Text(label, fontSize = 15.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                state.themeMode = selected
                state.persist()
                onClose()
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("取消") } }
    )
}

// -------------------------------------------------------------- color picker

@Composable
private fun ColorPickerDialog(state: AppState, current: Int, onPick: (Int) -> Unit) {
    var shades by remember { mutableStateOf<List<Int>?>(null) }
    val accent = Color(state.accentColor)
    AlertDialog(
        onDismissRequest = { onPick(current); state.dialog = null },
        title = { Text("主色调") },
        text = {
            val list = shades
            if (list == null) {
                Column {
                    PALETTE.chunked(5).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            row.forEach { argb ->
                                Box(
                                    Modifier
                                        .padding(6.dp)
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color(argb))
                                        .border(
                                            width = if (argb == current) 4.dp else 0.dp,
                                            color = if (argb == current) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { shades = shadePalette(argb) }
                                )
                            }
                        }
                    }
                }
            } else {
                Column {
                    list.chunked(5).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            row.forEach { argb ->
                                Box(
                                    Modifier
                                        .padding(6.dp)
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color(argb))
                                        .border(
                                            width = if (argb == list.first()) 4.dp else 0.dp,
                                            color = if (argb == list.first()) accent else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { onPick(argb); state.dialog = null }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (shades == null) {
                TextButton(onClick = { onPick(current); state.dialog = null }) { Text("DONE") }
            } else {
                TextButton(onClick = { onPick(shades!!.first()); state.dialog = null }) { Text("DONE") }
            }
        },
        dismissButton = {
            if (shades == null) {
                TextButton(onClick = { onPick(current); state.dialog = null }) { Text("CANCEL") }
            } else {
                TextButton(onClick = { shades = null }) { Text("BACK") }
            }
        }
    )
}

private fun shadePalette(argb: Int): List<Int> {
    val c = Color(argb)
    return listOf(
        c.lighten(0.85f), c.lighten(0.65f), c.lighten(0.45f), c.lighten(0.25f), c.lighten(0.10f),
        c, Color(c.red * 0.88f, c.green * 0.88f, c.blue * 0.88f), Color(c.red * 0.76f, c.green * 0.76f, c.blue * 0.76f),
        Color(c.red * 0.64f, c.green * 0.64f, c.blue * 0.64f), Color(c.red * 0.52f, c.green * 0.52f, c.blue * 0.52f)
    ).map { it.copy(alpha = 1f).toArgbInt() }
}

private fun Color.toArgbInt(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt()
)

// ------------------------------------------------------------ name dialogs

@Composable
private fun NameDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = { },
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onConfirm(text.trim()) }) { Text(confirmLabel) }
        }
    )
}

@Composable
private fun AddToPlaylistDialog(state: AppState, onClose: () -> Unit) {
    var creating by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    val userPlaylists = state.playlists.filter { !it.isAuto }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(if (creating) "新建播放列表" else "添加到播放列表") },
        text = {
            if (creating) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                    PlaylistPick("新建播放列表…") { creating = true }
                    listOf(state.playlist("auto_fav")).filterNotNull().forEach { p ->
                        PlaylistPick(p.name) {
                            state.addToPlaylist(p.id, state.selectedIds.toList())
                            onClose()
                        }
                    }
                    userPlaylists.forEach { p ->
                        PlaylistPick(p.name) {
                            state.addToPlaylist(p.id, state.selectedIds.toList())
                            onClose()
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (creating) {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        val p = state.createPlaylist(newName.trim(), state.selectedIds.toList())
                        state.toast("${state.selectedIds.size} 首歌曲已加入到播放列表 ${p.name}。")
                        onClose()
                    }
                }) { Text("创建") }
            } else {
                TextButton(onClick = onClose) { Text("取消") }
            }
        }
    )
}

@Composable
private fun PlaylistPick(label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) { Text(label, fontSize = 16.sp) }
}

// ------------------------------------------------------------- small popups

@Composable
fun OptionDialog(
    title: String,
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onClose: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(title) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                options.forEachIndexed { index, label ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(index); onClose() }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == index,
                            onClick = { onSelect(index); onClose() },
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                        )
                        Text(label, fontSize = 15.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("取消") } }
    )
}

@Composable
fun InfoDialog(text: String, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("提示") },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onClose) { Text("确定") } }
    )
}

@Composable
fun SurfaceCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(4.dp),
        shadowElevation = 2.dp
    ) { content() }
}
