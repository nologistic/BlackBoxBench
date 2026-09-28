package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ItemAction {
    OPEN, OPEN_WITH, CUT, COPY, DELETE, RENAME, COMPRESS,
    EXTRACT, EXTRACT_ALL, SHARE, COPY_PATH, BOOKMARK, SHORTCUT, PROPERTIES
}

@Composable
fun MenuRow(
    label: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 15.sp, color = OnSurface, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun SimpleDropdownItem(label: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, fontSize = 15.sp) },
        onClick = onClick,
    )
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun FileRowItem(
    node: FsNode,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onAction: (ItemAction) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val background = if (selected) PrimaryContainer.copy(alpha = 0.55f) else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(background)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            FileTypeIcon(node)
            if (selected) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Primary.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Glyph(Modifier.size(22.dp)) {
                        val w = size.width
                        val h = size.height
                        drawLine(Color.White, androidx.compose.ui.geometry.Offset(w * 0.22f, h * 0.52f), androidx.compose.ui.geometry.Offset(w * 0.42f, h * 0.72f), strokeWidth = w * 0.12f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        drawLine(Color.White, androidx.compose.ui.geometry.Offset(w * 0.42f, h * 0.72f), androidx.compose.ui.geometry.Offset(w * 0.78f, h * 0.28f), strokeWidth = w * 0.12f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    }
                }
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                node.name,
                fontSize = 16.sp,
                color = OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!node.isDir) {
                Text(
                    "${formatDate(node.mtime)}   ${formatSize(node.size)}",
                    fontSize = 12.sp,
                    color = OnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Box {
            Box(
                Modifier
                    .size(48.dp)
                    .clickable { menuOpen = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.MoreVert, null, tint = OnSurfaceVariant, modifier = Modifier.size(22.dp))
            }
            ItemMenuContent(node, menuOpen, onClose = { menuOpen = false }) { menuOpen = false; onAction(it) }
        }
    }
}

@Composable
fun ItemMenuContent(
    node: FsNode,
    expanded: Boolean,
    onClose: () -> Unit,
    onAction: (ItemAction) -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onClose) {
        val act: (ItemAction) -> Unit = { onAction(it) }
        SimpleDropdownItem("打开方式") { act(ItemAction.OPEN_WITH) }
        SimpleDropdownItem("剪切") { act(ItemAction.CUT) }
        SimpleDropdownItem("复制") { act(ItemAction.COPY) }
        SimpleDropdownItem("删除") { act(ItemAction.DELETE) }
        SimpleDropdownItem("重命名") { act(ItemAction.RENAME) }
        if (node.isArchive) {
            SimpleDropdownItem("解压") { act(ItemAction.EXTRACT_ALL) }
        } else {
            SimpleDropdownItem("压缩") { act(ItemAction.COMPRESS) }
        }
        SimpleDropdownItem("分享") { act(ItemAction.SHARE) }
        SimpleDropdownItem("复制路径") { act(ItemAction.COPY_PATH) }
        if (node.isDir) {
            SimpleDropdownItem("添加书签") { act(ItemAction.BOOKMARK) }
        }
        SimpleDropdownItem("创建快捷方式") { act(ItemAction.SHORTCUT) }
        SimpleDropdownItem("属性") { act(ItemAction.PROPERTIES) }
    }
}

@Composable
fun DrawerEntry(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val height = if (subtitle == null) 52.dp else 60.dp
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 1.dp)
            .height(height)
            .clip(RoundedCornerShape(28.dp))
            .background(if (selected) PrimaryContainer else Color.Transparent)
            .clickable(onClick = onClick),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { icon() }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, color = OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) {
                    Text(subtitle, fontSize = 12.sp, color = OnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun DrawerDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .height(1.dp)
            .background(OutlineVariant.copy(alpha = 0.6f))
    )
}

@Composable
fun BreadcrumbBar(path: List<FsNode>, onSegment: (FsNode) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val visible = if (path.size > 3) path.subList(path.size - 3, path.size) else path
        val offset = path.size - visible.size
        visible.forEachIndexed { index, node ->
            if (index > 0 || offset > 0) {
                Box(Modifier.padding(horizontal = 4.dp)) { ChevronGlyph(Modifier.size(14.dp), OnSurfaceVariant) }
            }
            Text(
                node.name,
                fontSize = 14.sp,
                color = OnSurface,
                fontWeight = if (index == visible.size - 1) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clickable { onSegment(node) }
                    .padding(horizontal = 2.dp),
            )
        }
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text,
        fontSize = 14.sp,
        color = Primary,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 28.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
fun SettingsRow(
    title: String,
    value: String?,
    enabled: Boolean = true,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(if (value == null) 56.dp else 72.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 16.sp,
                color = if (enabled) OnSurface else OnSurfaceVariant.copy(alpha = 0.5f),
            )
            if (value != null) {
                Text(
                    value,
                    fontSize = 13.sp,
                    color = if (enabled) OnSurfaceVariant else OnSurfaceVariant.copy(alpha = 0.5f),
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun SwitchTrailing(checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    androidx.compose.material3.Switch(
        checked = checked,
        onCheckedChange = onChange,
        enabled = enabled,
    )
}

@Composable
fun AppIconButton(icon: ImageVector, contentDescription: String?, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = OnSurfaceVariant, modifier = Modifier.size(24.dp))
    }
}

@Composable
fun GlyphIconButton(contentDescription: String?, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
fun ErrorState(message: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Glyph(Modifier.size(140.dp)) {
            val w = size.width
            val sw = w * 0.055f
            drawCircle(OnSurfaceVariant, w * 0.46f, center, style = androidx.compose.ui.graphics.drawscope.Stroke(sw))
            drawRoundRect(
                OnSurfaceVariant,
                androidx.compose.ui.geometry.Offset(w * 0.455f, w * 0.22f),
                androidx.compose.ui.geometry.Size(w * 0.09f, w * 0.32f),
                androidx.compose.ui.geometry.CornerRadius(w * 0.045f, w * 0.045f),
            )
            drawRoundRect(
                OnSurfaceVariant,
                androidx.compose.ui.geometry.Offset(w * 0.455f, w * 0.62f),
                androidx.compose.ui.geometry.Size(w * 0.09f, w * 0.09f),
                androidx.compose.ui.geometry.CornerRadius(w * 0.045f, w * 0.045f),
            )
        }
        Spacer(Modifier.height(28.dp))
        Text(
            message,
            fontSize = 14.sp,
            color = OnSurfaceVariant,
            modifier = Modifier.padding(horizontal = 28.dp),
        )
    }
}

@Composable
fun EmptyState() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 110.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Glyph(Modifier.size(150.dp)) {
            val w = size.width
            val h = size.height
            val sw = w * 0.075f
            val p = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.22f, h * 0.12f)
                lineTo(w * 0.6f, h * 0.12f)
                lineTo(w * 0.8f, h * 0.32f)
                lineTo(w * 0.8f, h * 0.88f)
                lineTo(w * 0.22f, h * 0.88f)
                close()
            }
            drawPath(p, OnSurfaceVariant, style = androidx.compose.ui.graphics.drawscope.Stroke(sw, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            drawLine(OnSurfaceVariant, androidx.compose.ui.geometry.Offset(w * 0.6f, h * 0.12f), androidx.compose.ui.geometry.Offset(w * 0.6f, h * 0.32f), sw)
            drawLine(OnSurfaceVariant, androidx.compose.ui.geometry.Offset(w * 0.6f, h * 0.32f), androidx.compose.ui.geometry.Offset(w * 0.8f, h * 0.32f), sw)
        }
        Spacer(Modifier.height(24.dp))
        Text("无文件", fontSize = 15.sp, color = OnSurfaceVariant)
    }
}
