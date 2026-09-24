package com.blackboxbench.reproduction

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

val BarTint = Color(0xFFE6E1F4)
val SelectionTint = Color(0xFFB0A7DC)
val TileLabel = Color(0xFFF2F0FA)
val FolderLabel = Color(0xFFEDEAF6)

/** Decodes a bitmap from disk with down-sampling; rotation is applied for the editor flow. */
@Composable
fun rememberBitmap(file: File?, reqPx: Int = 512, degrees: Int = 0): ImageBitmap? {
    val state by produceState<ImageBitmap?>(initialValue = null, file?.absolutePath, reqPx, degrees) {
        value = withContext(Dispatchers.IO) { decode(file, reqPx, degrees) }
    }
    return state
}

private fun decode(file: File?, reqPx: Int, degrees: Int): ImageBitmap? {
    if (file == null || !file.exists()) return null
    return runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        var sample = 1
        while (bounds.outWidth / sample > reqPx * 2 && bounds.outHeight / sample > reqPx * 2) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = BitmapFactory.decodeFile(file.absolutePath, options) ?: return null
        if (degrees == 0) {
            bitmap.asImageBitmap()
        } else {
            val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).asImageBitmap()
        }
    }.getOrNull()
}

/** Grid tile used by the album grids (thumbnail, optional file name, selection overlay). */
@Composable
fun MediaTile(
    item: MediaItem,
    file: File?,
    selected: Boolean,
    showName: Boolean,
    degrees: Int = 0,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmap = rememberBitmap(file, 360, degrees)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(Color(0xFF1B1B1B))
            .tileGestures(onClick = onClick, onLongClick = onLongClick)
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (item.video) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF23252B)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color(0xFFE8E8E8),
                    modifier = Modifier.size(44.dp)
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFF5C542))
            }
        }
        if (item.video) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .background(Color(0x99000000), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        formatDuration(item.durationMs),
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }
        }
        if (showName) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color(0x99000000))
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                Text(
                    item.name,
                    color = Color.White,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x553F51B5))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(22.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(Color(0xFF3F51B5)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/** Album tile: cover image with the gradient caption used by the observed app. */
@Composable
fun AlbumTile(
    title: String,
    subtitle: String,
    cover: MediaItem?,
    file: File?,
    empty: Boolean,
    badge: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmap = rememberBitmap(file, 420)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(Color(0xFF262230))
            .clickable(onClick = onClick)
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (empty) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint = Color(0xFFF5C542),
                    modifier = Modifier.size(44.dp)
                )
            }
        }
        if (badge != null) {
            Box(modifier = Modifier.align(Alignment.TopStart).padding(6.dp)) { badge() }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color(0x99000000))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(title, color = Color.White, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = Color(0xFFDDDDDD), fontSize = 13.sp)
        }
    }
}

fun Modifier.tileGestures(onClick: () -> Unit, onLongClick: () -> Unit): Modifier = this.composed {
    Modifier.pointerInput(onClick, onLongClick) {
        detectTapGestures(onTap = { onClick() }, onLongPress = { onLongClick() })
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.primary,
        fontSize = 14.sp,
        modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 6.dp)
    )
}

@Composable
fun SettingRow(
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(subtitle, fontSize = 13.sp, color = Color(0xFF6B6B6B))
            }
        }
        if (trailing != null) trailing()
    }
}

@Composable
fun RadioDialog(
    title: String,
    options: List<String>,
    selected: String,
    extraTitle: String? = null,
    extraOptions: List<String> = emptyList(),
    extraSelected: String? = null,
    onConfirm: (String, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var choice by remember { mutableStateOf(selected) }
    var extraChoice by remember { mutableStateOf(extraSelected ?: extraOptions.firstOrNull().orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = MaterialTheme.colorScheme.primary) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { choice = option }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = choice == option, onClick = { choice = option })
                        Text(option, fontSize = 16.sp)
                    }
                }
                if (extraOptions.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    if (extraTitle != null) {
                        Text(extraTitle, fontSize = 14.sp, color = Color(0xFF6B6B6B))
                    }
                    extraOptions.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { extraChoice = option }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = extraChoice == option, onClick = { extraChoice = option })
                            Text(option, fontSize = 16.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(choice, extraChoice) }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun CheckboxDialog(
    title: String,
    options: List<String>,
    checked: Set<String>,
    onConfirm: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var state by remember { mutableStateOf(checked) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = MaterialTheme.colorScheme.primary) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                state = if (state.contains(option)) state - option else state + option
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = state.contains(option), onCheckedChange = {
                            state = if (state.contains(option)) state - option else state + option
                        })
                        Text(option, fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(state) }) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun FormDialog(
    title: String,
    fields: List<Pair<String, String>>,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val values = remember { fields.map { it.second }.toMutableStateList() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = MaterialTheme.colorScheme.primary) },
        text = {
            Column {
                fields.forEachIndexed { index, field ->
                    OutlinedTextField(
                        value = values[index],
                        onValueChange = { values[index] = it },
                        label = { Text(field.first) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(values.toList()) }) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    extraOptions: List<String> = emptyList(),
    onConfirm: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var state by remember { mutableStateOf(emptySet<String>()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = MaterialTheme.colorScheme.primary) },
        text = {
            Column {
                Text(message, fontSize = 15.sp)
                extraOptions.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                state = if (state.contains(option)) state - option else state + option
                            }
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = state.contains(option), onCheckedChange = {
                            state = if (state.contains(option)) state - option else state + option
                        })
                        Text(option, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(state) }) { Text("是") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("否") } }
    )
}

@Composable
fun InfoDialog(title: String, rows: List<Pair<String, String>>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = MaterialTheme.colorScheme.primary) },
        text = {
            Column {
                rows.forEach { (label, value) ->
                    Column(modifier = Modifier.padding(bottom = 10.dp)) {
                        Text(label, fontSize = 13.sp, color = Color(0xFF6B6B6B))
                        Text(value, fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("确定") } }
    )
}

@Composable
fun OutlinedField(value: String, label: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}

@Composable
fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Text(
            text,
            fontSize = 16.sp,
            color = Color(0xFF6B6B6B),
            modifier = Modifier.padding(top = 40.dp)
        )
    }
}

@Composable
fun NumberChip(text: String) {
    Box(
        modifier = Modifier
            .background(Color(0xCC1B1B1B), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text, color = TileLabel, fontSize = 12.sp)
    }
}

@Composable
fun ToolbarTitle(text: String, bold: Boolean = false) {
    Text(
        text,
        fontSize = 19.sp,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun SpacerWidth(width: Dp) {
    Spacer(modifier = Modifier.width(width))
}
