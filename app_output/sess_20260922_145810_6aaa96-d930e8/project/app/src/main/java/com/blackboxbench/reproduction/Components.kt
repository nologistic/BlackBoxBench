package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppBottomBar(selected: String, onSelect: (String) -> Unit) {
    val c = LocalMarkorColors.current
    NavigationBar(containerColor = c.navBackground, tonalElevation = 0.dp) {
        NavigationBarItem(
            selected = selected == "files",
            onClick = { onSelect("files") },
            icon = { IcoFolder(size = 24.dp, tint = tabTint(c, selected == "files"), filled = selected == "files") },
            label = { Text("文件", fontSize = 12.sp, color = tabTint(c, selected == "files")) },
            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent),
        )
        NavigationBarItem(
            selected = selected == "todo",
            onClick = { onSelect("todo") },
            icon = { IcoCheckSquare(size = 24.dp, tint = tabTint(c, selected == "todo"), filled = selected == "todo") },
            label = { Text("To-Do", fontSize = 12.sp, color = tabTint(c, selected == "todo")) },
            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent),
        )
        NavigationBarItem(
            selected = selected == "quicknote",
            onClick = { onSelect("quicknote") },
            icon = { IcoBolt(size = 24.dp, tint = tabTint(c, selected == "quicknote")) },
            label = { Text("QuickNote", fontSize = 12.sp, color = tabTint(c, selected == "quicknote")) },
            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent),
        )
        NavigationBarItem(
            selected = selected == "more",
            onClick = { onSelect("more") },
            icon = { IcoHeart(size = 24.dp, tint = tabTint(c, selected == "more"), filled = selected == "more") },
            label = { Text("更多", fontSize = 12.sp, color = tabTint(c, selected == "more")) },
            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent),
        )
    }
}

private fun tabTint(c: MarkorColors, active: Boolean): Color = if (active) c.accent else c.onToolbar

@Composable
fun SectionHeader(text: String) {
    val c = LocalMarkorColors.current
    Text(
        text = text,
        color = c.accent,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 18.dp, bottom = 8.dp),
    )
}

@Composable
fun SettingRow(
    title: String,
    subtitle: String? = null,
    icon: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    showDivider: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val c = LocalMarkorColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
                .defaultMinSize(minHeight = 64.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) { icon() }
                Spacer(Modifier.width(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = c.textPrimary, fontSize = 16.sp)
                if (subtitle != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, color = c.textSecondary, fontSize = 13.sp)
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(12.dp))
                trailing()
            }
        }
        if (showDivider) HorizontalDivider(color = c.divider)
    }
}

@Composable
fun OptionDialog(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    layout: String = "radio",
) {
    val c = LocalMarkorColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = { Text(title, color = c.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Medium) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (layout == "radio") {
                            RadioButton(
                                selected = option == selected,
                                onClick = { onSelect(option) },
                                colors = RadioButtonDefaults.colors(selectedColor = c.accent, unselectedColor = c.textSecondary),
                            )
                        } else {
                            IcoCheckSquare(size = 22.dp, tint = if (option == selected) c.accent else c.textSecondary, filled = option == selected)
                        }
                        Spacer(Modifier.width(16.dp))
                        Text(option, color = c.textPrimary, fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = c.accent, fontSize = 16.sp) }
        },
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    body: String?,
    confirmLabel: String = "确定",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = LocalMarkorColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = { Text(title, color = c.textPrimary, fontSize = 20.sp) },
        text = {
            if (body.isNullOrEmpty()) {
                Spacer(Modifier.height(4.dp))
            } else {
                Text(body, color = c.textPrimary, fontSize = 16.sp)
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel, color = c.accent, fontSize = 16.sp) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = c.accent, fontSize = 16.sp) } },
    )
}

@Composable
fun ToastOverlay(message: String?) {
    val c = LocalMarkorColors.current
    if (message == null) return
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp)
                .background(Color(0xE6323232), RoundedCornerShape(6.dp))
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(message, color = Color.White, fontSize = 14.sp)
        }
    }
}
