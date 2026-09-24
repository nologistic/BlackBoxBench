package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap

/** The rounded search pill used as the top bar on every browsing screen. */
@Composable
fun TopPill(
    hint: String,
    modifier: Modifier = Modifier,
    showBack: Boolean = false,
    onBack: () -> Unit = {},
    onSearchClick: (() -> Unit)? = null,
    icons: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 6.dp)
            .height(46.dp)
            .clip(RoundedCornerShape(23.dp))
            .background(PillSurface)
            .let { if (onSearchClick != null) it.clickable { onSearchClick() } else it }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBack) {
            IconTap(kind = "back", tint = Color(0xFF3B3B3B)) { onBack() }
            Spacer(Modifier.width(6.dp))
        } else {
            Glyph(size = 22.dp, color = Color(0xFF3B3B3B), kind = "search")
            Spacer(Modifier.width(14.dp))
        }
        Text(
            text = hint,
            color = HintGrey,
            fontSize = 17.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        icons()
    }
}

@Composable
fun IconTap(
    kind: String,
    tint: Color = Color(0xFF3B3B3B),
    size: androidx.compose.ui.unit.Dp = 24.dp,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        if (kind == "back") {
            Glyph(size = size, color = tint, kind = "arrow_back")
        } else {
            Glyph(size = size, color = tint, kind = kind)
        }
    }
}

/** Solid primary top bar used while items are selected. */
@Composable
fun SelectionBar(
    count: Int,
    total: Int,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    menu: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Primary)
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTap(kind = "back", tint = Color.White) { onBack() }
        Text(
            "$count / $total",
            color = Color.White,
            fontSize = 19.sp,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        )
        IconTap(kind = "trash", tint = Color.White) { onDelete() }
        IconTap(kind = "share", tint = Color.White) { onShare() }
        Box {
            IconTap(kind = "dots", tint = Color.White) { }
            menu()
        }
    }
}

/** Bottom gradient + title used by album and media tiles. */
@Composable
fun TileCaption(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color(0x00000000), Color(0x99000000)),
                )
            )
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, end = 8.dp, bottom = 6.dp)
        ) {
            Text(title, color = Color.White, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, color = Color(0xFFEDEDED), fontSize = 13.sp, maxLines = 1)
            }
        }
    }
}

@Composable
fun MediaThumb(item: MediaItem, modifier: Modifier = Modifier, showName: Boolean = false) {
    val bitmap = remember(item.relPath, item.modified) {
        val file = LibraryHolder.library?.fileOf(item)
        if (file == null) null else Thumbs.thumbnail(file, 512, item.isVideo)
    }
    Box(modifier) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(Modifier.fillMaxSize().background(Color(0xFFDDDDDD)))
        }
        if (showName) {
            TileCaption(item.displayName)
        }
        if (item.isVideo) {
            val duration = remember(item.relPath) { durationMs(item) }
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x66000000))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Glyph(size = 12.dp, color = Color.White, kind = "play")
                Spacer(Modifier.width(4.dp))
                Text(formatDuration(duration), color = Color.White, fontSize = 11.sp)
            }
        }
        if (item.favorite) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(0x77000000)),
                contentAlignment = Alignment.Center,
            ) {
                Glyph(size = 16.dp, color = Color.White, kind = "star_filled")
            }
        }
    }
}

private fun durationMs(item: MediaItem): Long {
    return try {
        LibraryHolder.library?.durationOf(item) ?: 0L
    } catch (_: Exception) {
        0L
    }
}

/** Small helper so composables can reach the library without extra plumbing. */
object LibraryHolder {
    var library: Library? = null
}

@Composable
fun androidx.compose.foundation.layout.BoxScope.TileCheck(selected: Boolean) {
    if (selected) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x336D7ED8))
        )
        Box(
            modifier = Modifier
                .padding(6.dp)
                .size(26.dp)
                .clip(CircleShape)
                .background(Primary)
                .align(Alignment.TopEnd),
            contentAlignment = Alignment.Center,
        ) {
            Glyph(size = 16.dp, color = Color.White, kind = "check")
        }
    }
}

fun LazyGridScope.gridSection(header: String) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        Text(
            header,
            fontSize = 17.sp,
            color = Color(0xFF2B2B2B),
            modifier = Modifier.padding(start = 14.dp, top = 16.dp, bottom = 8.dp)
        )
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text,
        fontSize = 17.sp,
        color = Color(0xFF2B2B2B),
        modifier = Modifier.padding(start = 14.dp, top = 16.dp, bottom = 8.dp)
    )
}

// ------------------------------------------------------------------- dialogs

@Composable
fun RadioListDialog(
    title: String,
    options: List<String>,
    selected: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
    extra: (@Composable () -> Unit)? = null,
) {
    var choice = selected
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogSurface,
        title = { Text(title, color = Primary, fontSize = 22.sp) },
        text = {
            Column {
                options.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { choice = index }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = choice == index,
                            onClick = { choice = index },
                            colors = RadioButtonDefaults.colors(selectedColor = Primary),
                        )
                        Text(option, fontSize = 17.sp, color = Color(0xFF23232B))
                    }
                }
                if (extra != null) {
                    HorizontalDivider(Modifier.padding(vertical = 8.dp), color = DividerGrey)
                    extra()
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(choice) }) { Text("确定", color = Primary, fontSize = 17.sp) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = Primary, fontSize = 17.sp) }
        },
    )
}

@Composable
fun CheckListDialog(
    title: String,
    options: List<Pair<String, Boolean>>,
    onDismiss: () -> Unit,
    onConfirm: (List<Boolean>) -> Unit,
    extra: (@Composable () -> Unit)? = null,
) {
    val state = options.map { it.second }.toMutableList()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogSurface,
        title = { Text(title, color = Primary, fontSize = 22.sp) },
        text = {
            Column {
                options.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { state[index] = !state[index] }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = state[index],
                            onCheckedChange = { state[index] = it },
                            colors = CheckboxDefaults.colors(checkedColor = Primary),
                        )
                        Text(option.first, fontSize = 17.sp, color = Color(0xFF23232B))
                    }
                }
                if (extra != null) extra()
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.toList()) }) { Text("确定", color = Primary, fontSize = 17.sp) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = Primary, fontSize = 17.sp) }
        },
    )
}

@Composable
fun ConfirmDialog(
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    title: String? = null,
    checkboxes: List<String> = emptyList(),
    onChecked: ((List<Boolean>) -> Unit)? = null,
    confirmLabel: String = "是",
    dismissLabel: String = "否",
) {
    val state = checkboxes.map { false }.toMutableList()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogSurface,
        title = title?.let { { Text(it, color = Primary, fontSize = 22.sp) } },
        text = {
            Column {
                Text(message, fontSize = 17.sp, color = Color(0xFF23232B))
                checkboxes.forEachIndexed { index, label ->
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { state[index] = !state[index] },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = state[index],
                            onCheckedChange = { state[index] = it },
                            colors = CheckboxDefaults.colors(checkedColor = Primary),
                        )
                        Text(label, fontSize = 16.sp, color = Color(0xFF23232B))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onChecked?.invoke(state.toList())
                onConfirm()
            }) { Text(confirmLabel, color = Primary, fontSize = 17.sp) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissLabel, color = Primary, fontSize = 17.sp) }
        },
    )
}

@Composable
fun SimpleMenu(expanded: Boolean, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = DialogSurface,
    ) { content() }
}

@Composable
fun MenuEntry(text: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text, fontSize = 16.sp) },
        onClick = onClick,
    )
}

@Composable
fun SwitchRow(title: String, checked: Boolean, subtitle: String? = null, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, color = Color(0xFF23232B))
            if (subtitle != null) {
                Text(subtitle, fontSize = 14.sp, color = HintGrey)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PrimaryDark,
                checkedTrackColor = Primary,
                uncheckedThumbColor = Color(0xFFB9B9C4),
                uncheckedTrackColor = Color(0xFFD9D9E3),
                uncheckedBorderColor = Color(0xFFC6C6D2),
            ),
        )
    }
}

@Composable
fun SettingsSection(title: String) {
    Text(
        title,
        fontSize = 14.sp,
        color = Primary,
        modifier = Modifier.padding(start = 16.dp, top = 18.dp, bottom = 6.dp)
    )
}

@Composable
fun ClickRow(title: String, subtitle: String? = null, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(title, fontSize = 16.sp, color = Color(0xFF23232B))
        if (subtitle != null) {
            Text(subtitle, fontSize = 14.sp, color = HintGrey)
        }
    }
}

@Composable
fun EmptyState(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Text(
            text,
            color = Color(0xFF3B3B3B),
            fontSize = 17.sp,
            modifier = Modifier.padding(top = 26.dp)
        )
    }
}

@Composable
fun SquareBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.aspectRatio(1f)) { content() }
}
