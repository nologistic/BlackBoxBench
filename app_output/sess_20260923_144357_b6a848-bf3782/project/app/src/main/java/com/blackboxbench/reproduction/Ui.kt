package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ------------------------------------------------------------------ navigation

sealed class Screen {
    object Library : Screen()
    data class Album(val key: String) : Screen()
    data class Artist(val name: String) : Screen()
    data class Genre(val name: String) : Screen()
    data class Playlist(val id: String) : Screen()
    data class Smart(val kind: String) : Screen()
    object Search : Screen()
    object Folders : Screen()
    object Settings : Screen()
    object About : Screen()
    object Player : Screen()
    data class TagEditor(val trackId: String) : Screen()
}

class Navigator {
    val stack = mutableStateListOf<Screen>(Screen.Library)
    val current: Screen get() = stack.last()
    val canGoBack: Boolean get() = stack.size > 1
    fun push(screen: Screen) {
        stack.add(screen)
    }

    fun pop() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    fun reset() {
        while (stack.size > 1) stack.removeAt(stack.lastIndex)
    }
}

// ---------------------------------------------------------------------- theme

data class AppColors(
    val primary: Color,
    val primaryDark: Color,
    val accent: Color,
    val background: Color,
    val surface: Color,
    val onSurface: Color,
    val secondaryText: Color,
    val divider: Color,
    val playerBackground: Color,
    val playerPanel: Color
)

private val primaryOptions = mapOf(
    "靛蓝" to (Color(0xFF3F51B5) to Color(0xFF303F9F)),
    "蓝色" to (Color(0xFF1976D2) to Color(0xFF0D47A1)),
    "青色" to (Color(0xFF00897B) to Color(0xFF00695C)),
    "绿色" to (Color(0xFF43A047) to Color(0xFF2E7D32)),
    "橙色" to (Color(0xFFF4511E) to Color(0xFFE64A19)),
    "紫色" to (Color(0xFF8E24AA) to Color(0xFF6A1B9A))
)

private val accentOptions = mapOf(
    "粉色" to Color(0xFFE91E63),
    "红色" to Color(0xFFE53935),
    "蓝色" to Color(0xFF1E88E5),
    "青色" to Color(0xFF00ACC1),
    "绿色" to Color(0xFF43A047),
    "橙色" to Color(0xFFFB8C00)
)

fun primaryNameFor(color: Color): String = primaryOptions.entries.firstOrNull { it.value.first == color }?.key ?: "靛蓝"

@Composable
fun rememberAppColors(state: AppState): AppColors {
    val primaryPair = primaryOptions[state.primaryColor] ?: primaryOptions["靛蓝"]!!
    val accent = accentOptions[state.accentColor] ?: accentOptions["粉色"]!!
    val dark = state.darkTheme
    return remember(state.primaryColor, state.accentColor, dark) {
        if (dark) {
            AppColors(
                primary = primaryPair.first,
                primaryDark = primaryPair.second,
                accent = accent,
                background = Color(0xFF121212),
                surface = Color(0xFF1E1E1E),
                onSurface = Color(0xFFECECEC),
                secondaryText = Color(0xFF9E9E9E),
                divider = Color(0xFF2C2C2C),
                playerBackground = Color(0xFF000000),
                playerPanel = Color(0xFF2A2A2A)
            )
        } else {
            AppColors(
                primary = primaryPair.first,
                primaryDark = primaryPair.second,
                accent = accent,
                background = Color(0xFFF5F5F5),
                surface = Color.White,
                onSurface = Color(0xFF212121),
                secondaryText = Color(0xFF757575),
                divider = Color(0xFFE0E0E0),
                playerBackground = Color(0xFF212121),
                playerPanel = Color(0xFFE0E0E0)
            )
        }
    }
}

fun coverColor(seed: String): Color {
    val palette = listOf(
        Color(0xFF37474F), Color(0xFF4E342E), Color(0xFF1A237E), Color(0xFF004D40),
        Color(0xFF3E2723), Color(0xFF263238), Color(0xFF4A148C), Color(0xFF01579B)
    )
    val index = (seed.hashCode().let { if (it < 0) -it else it }) % palette.size
    return palette[index]
}

// ------------------------------------------------------------- small widgets

@Composable
fun SectionHeader(text: String, colors: AppColors) {
    Text(
        text = text,
        color = colors.accent,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .padding(start = 16.dp, top = 20.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingRow(
    title: String,
    subtitle: String? = null,
    colors: AppColors,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .let { if (onClick != null) it.clickable { onClick() } else it }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = colors.onSurface, fontSize = 16.sp)
            if (subtitle != null) {
                Text(subtitle, color = colors.secondaryText, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
fun SwitchSettingRow(title: String, subtitle: String? = null, colors: AppColors, checked: Boolean, onChange: (Boolean) -> Unit) {
    SettingRow(title, subtitle, colors, trailing = {
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = colors.accent, checkedThumbColor = colors.accent)
        )
    }, onClick = { onChange(!checked) })
}

@Composable
fun DividerLine(colors: AppColors, indent: Dp = 16.dp) {
    Divider(color = colors.divider, thickness = 1.dp, modifier = Modifier.padding(start = indent))
}

@Composable
fun CoverArt(seed: String, size: Dp = 56.dp, corner: Dp = 4.dp, showVinyl: Boolean = true) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(coverColor(seed)),
        contentAlignment = Alignment.Center
    ) {
        if (showVinyl) VinylIcon(size = size * 0.72f, tint = Color(0xFFBDBDBD))
    }
}

@Composable
fun EmptyState(text: String, colors: AppColors, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Text(
            text,
            color = colors.secondaryText,
            fontSize = 15.sp,
            modifier = Modifier.padding(top = 48.dp)
        )
    }
}

@Composable
fun TextInputDialog(
    title: String,
    label: String,
    initial: String = "",
    confirmLabel: String = "确定",
    colors: AppColors,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = colors.onSurface, fontSize = 20.sp) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(label) },
                singleLine = false,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { if (value.isNotBlank()) onConfirm(value) }) {
                Text(confirmLabel, color = colors.accent)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = colors.accent) } },
        containerColor = colors.surface
    )
}

@Composable
fun OptionDialog(
    title: String,
    options: List<String>,
    selected: String?,
    colors: AppColors,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = colors.onSurface, fontSize = 20.sp) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            option,
                            color = if (option == selected) colors.accent else colors.onSurface,
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                        if (option == selected) {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(colors.accent)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消", color = colors.accent) } },
        containerColor = colors.surface
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    colors: AppColors,
    destructive: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = colors.onSurface, fontSize = 20.sp) },
        text = { Text(message, color = colors.onSurface, fontSize = 15.sp) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = if (destructive) Color(0xFFD32F2F) else colors.accent)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = colors.accent) } },
        containerColor = colors.surface
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongRow(
    title: String,
    subtitle: String,
    seed: String,
    colors: AppColors,
    isCurrent: Boolean = false,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    number: Int? = null,
    leading: @Composable (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
    onOverflow: (() -> Unit)?
) {
    val background = when {
        selected -> colors.accent.copy(alpha = 0.14f)
        else -> colors.surface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) { leading() }
        }
        if (number != null) {
            Box(Modifier.width(28.dp), contentAlignment = Alignment.CenterStart) {
                Text("$number", color = colors.secondaryText, fontSize = 14.sp)
            }
        }
        Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
            if (isCurrent) {
                VinylIcon(size = 40.dp, tint = colors.secondaryText)
            } else {
                CoverArt(seed, size = 56.dp)
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = colors.onSurface,
                fontSize = 17.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(subtitle, color = colors.secondaryText, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (onOverflow != null) {
            Box(
                Modifier
                    .size(48.dp)
                    .clickable { onOverflow() },
                contentAlignment = Alignment.Center
            ) {
                MoreVertGlyph(size = 22.dp, tint = colors.secondaryText)
            }
        }
    }
}

@Composable
fun PlaylistRow(
    name: String,
    subtitle: String,
    colors: AppColors,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    onOverflow: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .clickable { onClick() }
            .padding(start = 16.dp, end = 4.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { icon() }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(name, color = colors.onSurface, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = colors.secondaryText, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (onOverflow != null) {
            Box(
                Modifier
                    .size(48.dp)
                    .clickable { onOverflow() },
                contentAlignment = Alignment.Center
            ) {
                MoreVertGlyph(size = 22.dp, tint = colors.secondaryText)
            }
        }
    }
}

@Composable
fun SelectionTopBar(
    count: Int,
    colors: AppColors,
    onClose: () -> Unit,
    onUndo: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onOverflow: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.primary)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(48.dp).clickable { onClose() }, contentAlignment = Alignment.Center) {
            CloseGlyph(size = 22.dp, tint = Color.White)
        }
        Text("已选择 $count", color = Color.White, fontSize = 20.sp, modifier = Modifier.weight(1f))
        Box(Modifier.size(48.dp).clickable { onUndo() }, contentAlignment = Alignment.Center) {
            UndoGlyph(size = 22.dp, tint = Color.White)
        }
        Box(Modifier.size(48.dp).clickable { onAddToPlaylist() }, contentAlignment = Alignment.Center) {
            PlaylistGlyph(size = 22.dp, tint = Color.White)
        }
        Box(Modifier.size(48.dp).clickable { onOverflow() }, contentAlignment = Alignment.Center) {
            MoreVertGlyph(size = 22.dp, tint = Color.White)
        }
    }
}

@Composable
fun SimpleTopBar(
    title: String,
    colors: AppColors,
    onBack: (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.primary)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            Box(Modifier.size(48.dp).clickable { onBack() }, contentAlignment = Alignment.Center) {
                BackGlyph(size = 22.dp, tint = Color.White)
            }
        } else {
            Spacer(Modifier.width(12.dp))
        }
        Text(title, color = Color.White, fontSize = 20.sp, modifier = Modifier.weight(1f))
        if (actions != null) actions()
    }
}

@Composable
fun TopBarAction(onClick: () -> Unit, tint: Color = Color.White, content: @Composable () -> Unit) {
    Box(Modifier.size(48.dp).clickable { onClick() }, contentAlignment = Alignment.Center) { content() }
}

@Composable
fun CheckRow(label: String, checked: Boolean, colors: AppColors, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = colors.onSurface, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(checkedTrackColor = colors.accent, checkedThumbColor = colors.accent)
        )
    }
}

@Composable
fun GridAlbumCard(
    title: String,
    subtitle: String,
    colors: AppColors,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(coverColor(title)),
            contentAlignment = Alignment.Center
        ) {
            VinylIcon(size = 96.dp, tint = Color(0xFFCFCFCF))
        }
        Spacer(Modifier.height(8.dp))
        Text(title, color = colors.onSurface, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, color = colors.secondaryText, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
