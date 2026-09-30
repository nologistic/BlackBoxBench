package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas

@Composable
fun VlcCone(size: Dp = 30.dp, tint: Color) {
    VlcIconView(VlcIcon.CONE, size, tint)
}

@Composable
fun HLine(modifier: Modifier = Modifier) {
    val p = LocalVlcPalette.current
    Box(modifier.fillMaxWidth().height(1.dp).background(p.divider))
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    val p = LocalVlcPalette.current
    Text(
        text = text,
        color = p.accent,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 8.dp),
    )
}

data class TopAction(val icon: VlcIcon, val onClick: () -> Unit, val tint: Color? = null)

@Composable
fun TopBar(
    title: String = "VLC",
    showLogo: Boolean = true,
    onBack: (() -> Unit)? = null,
    actions: List<TopAction> = emptyList(),
) {
    val p = LocalVlcPalette.current
    Row(
        modifier = Modifier.fillMaxWidth().height(60.dp).background(p.surface).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconTap(VlcIcon.BACK, 24.dp, p.textPrimary, onBack)
            Spacer(Modifier.width(10.dp))
        } else if (showLogo) {
            Spacer(Modifier.width(6.dp))
            VlcCone(30.dp, p.accent)
            Spacer(Modifier.width(10.dp))
        }
        Text(title, color = p.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        actions.forEach { a ->
            IconTap(a.icon, 24.dp, a.tint ?: p.textPrimary, a.onClick)
        }
    }
}

@Composable
fun IconTap(icon: VlcIcon, size: Dp = 24.dp, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(size + 14.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) { VlcIconView(icon, size, tint) }
}

val BottomNavItems = listOf(
    Triple("视频", VlcIcon.VIDEO, 0),
    Triple("音频", VlcIcon.MUSIC, 1),
    Triple("浏览", VlcIcon.FOLDER, 2),
    Triple("播放列表", VlcIcon.PLAYLIST, 3),
    Triple("更多", VlcIcon.MORE, 4),
)

@Composable
fun VlcBottomNav(selected: Int, onSelect: (Int) -> Unit) {
    val p = LocalVlcPalette.current
    Column {
        HLine()
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).background(p.surface),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomNavItems.forEach { (label, icon, index) ->
                val active = index == selected
                Column(
                    modifier = Modifier.weight(1f).fillMaxSize().clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSelect(index) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    VlcIconView(icon, 23.dp, if (active) p.accent else p.textSecondary)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        label,
                        color = if (active) p.accent else p.textSecondary,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

@Composable
fun SubTabs(tabs: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val p = LocalVlcPalette.current
    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(44.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { i, t ->
                val active = i == selectedIndex
                Column(
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSelect(i) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        t,
                        color = if (active) p.accent else p.textSecondary,
                        fontSize = 15.sp,
                        fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier.height(2.dp).width(if (active) 28.dp else 0.dp).background(p.accent),
                    )
                }
            }
        }
        HLine()
    }
}

@Composable
fun CheckSquare(checked: Boolean, onToggle: (() -> Unit)? = null, size: Dp = 22.dp) {
    val p = LocalVlcPalette.current
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(3.dp))
            .background(if (checked) p.accent else Color.Transparent)
            .border(1.5.dp, if (checked) p.accent else p.textSecondary, RoundedCornerShape(3.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onToggle?.invoke() },
        contentAlignment = Alignment.Center,
    ) {
        if (checked) VlcIconView(VlcIcon.CHECK, size * 0.72f, Color.White)
    }
}

@Composable
fun VlcSwitch(checked: Boolean, onToggle: (Boolean) -> Unit) {
    val p = LocalVlcPalette.current
    Switch(
        checked = checked,
        onCheckedChange = onToggle,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = p.accent,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = Color(0xFFBDBDBD),
            uncheckedBorderColor = Color.Transparent,
        ),
    )
}

@Composable
fun VlcCheckbox(checked: Boolean, onToggle: (Boolean) -> Unit) {
    val p = LocalVlcPalette.current
    Checkbox(
        checked = checked,
        onCheckedChange = onToggle,
        colors = CheckboxDefaults.colors(
            checkedColor = p.accent,
            uncheckedColor = p.textSecondary,
            checkmarkColor = Color.White,
        ),
    )
}

@Composable
fun VlcRadio(selected: Boolean, onSelect: () -> Unit) {
    val p = LocalVlcPalette.current
    RadioButton(
        selected = selected,
        onClick = onSelect,
        colors = RadioButtonDefaults.colors(selectedColor = p.accent, unselectedColor = p.textSecondary),
    )
}

/** Settings-style row with a title, optional subtitle and arbitrary trailing content. */
@Composable
fun SettingRow(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    titleColor: Color? = null,
    subtitleColor: Color? = null,
) {
    val p = LocalVlcPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = onClick != null,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = titleColor ?: p.textPrimary, fontSize = 16.sp)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = subtitleColor ?: p.textSecondary, fontSize = 13.sp)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
fun CheckRow(title: String, subtitle: String? = null, checked: Boolean, onToggle: (Boolean) -> Unit) {
    SettingRow(title, subtitle, onClick = { onToggle(!checked) }, trailing = { VlcCheckbox(checked, onToggle) })
}

@Composable
fun SwitchRow(title: String, subtitle: String? = null, checked: Boolean, onToggle: (Boolean) -> Unit) {
    SettingRow(title, subtitle, onClick = { onToggle(!checked) }, trailing = { VlcSwitch(checked, onToggle) })
}

@Composable
fun ArrowRow(title: String, subtitle: String? = null, value: String? = null, onClick: () -> Unit) {
    val p = LocalVlcPalette.current
    SettingRow(
        title = title,
        subtitle = subtitle ?: value,
        onClick = onClick,
        trailing = { VlcIconView(VlcIcon.ARROW_RIGHT, 22.dp, p.textSecondary) },
    )
}

@Composable
fun ValueRow(title: String, value: String, subtitle: String? = null, onClick: (() -> Unit)? = null) {
    val p = LocalVlcPalette.current
    SettingRow(
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(value, color = p.accent, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
    )
}

/** Custom bottom sheet so the reproduction controls scrim and shape exactly. */
@Composable
fun BottomSheet(onDismiss: () -> Unit, maxHeight: Dp = 560.dp, content: @Composable ColumnScope.() -> Unit) {
    val p = LocalVlcPalette.current
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0x8A000000))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                .background(p.surface)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 10.dp),
            content = content,
        )
    }
}

@Composable
fun SheetTitle(text: String) {
    val p = LocalVlcPalette.current
    Text(
        text,
        color = p.textPrimary,
        fontSize = 19.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 6.dp),
    )
}

@Composable
fun SheetAction(text: String, icon: VlcIcon? = null, tint: Color? = null, onClick: () -> Unit) {
    val p = LocalVlcPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            VlcIconView(icon, 23.dp, tint ?: p.icon)
            Spacer(Modifier.width(18.dp))
        }
        Text(text, color = tint ?: p.textPrimary, fontSize = 16.sp, modifier = Modifier.padding(start = if (icon == null) 8.dp else 0.dp))
    }
}

@Composable
fun VlcDialog(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val p = LocalVlcPalette.current
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0x8A000000))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(p.surface)
                .padding(vertical = 18.dp),
            content = content,
        )
    }
}

@Composable
fun DialogTitle(text: String) {
    val p = LocalVlcPalette.current
    Text(
        text,
        color = p.textPrimary,
        fontSize = 19.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp),
    )
}

@Composable
fun DialogMessage(text: String) {
    val p = LocalVlcPalette.current
    Text(
        text,
        color = p.textPrimary,
        fontSize = 15.sp,
        modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp),
    )
}

@Composable
fun DialogButtons(vararg labels: String) {
    val p = LocalVlcPalette.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(end = 12.dp, top = 12.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        labels.forEach { label ->
            Text(
                label,
                color = p.accent,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
fun DialogButtonList(vararg items: Pair<String, () -> Unit>) {
    val p = LocalVlcPalette.current
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        items.forEach { (label, action) ->
            Text(
                label,
                color = p.accent,
                fontSize = 15.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = action,
                    )
                    .padding(horizontal = 22.dp, vertical = 12.dp),
            )
        }
    }
}

/** Deterministic test-pattern artwork used for the synthetic video files. */
@Composable
fun TestPattern(pattern: PatternKind, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        when (pattern) {
            PatternKind.COLOR_BARS -> {
                val w = size.width
                val h = size.height
                val bars = listOf(
                    Color(0xFFB8B800), Color(0xFF00B800), Color(0xFF00B8B8),
                    Color(0xFF0000C0), Color(0xFFB800B8), Color(0xFFC00000),
                )
                val barW = w * 0.66f / bars.size
                bars.forEachIndexed { i, c ->
                    drawRect(c, topLeft = Offset(i * barW, 0f), size = Size(barW + 0.6f, h * 0.72f))
                }
                drawRect(Color(0xFF2A2A2A), topLeft = Offset(0f, h * 0.72f), size = Size(w * 0.66f, h * 0.28f))
                listOf(
                    Color(0xFF101060), Color.White, Color(0xFF601060), Color(0xFF202020),
                    Color(0xFF0A0A0A), Color(0xFF303030),
                ).forEachIndexed { i, c ->
                    val seg = w * 0.34f / 6f
                    drawRect(c, topLeft = Offset(w * 0.66f + i * seg, h * 0.72f), size = Size(seg + 0.6f, h * 0.28f))
                }
                // right column gradient block
                val steps = 18
                for (i in 0 until steps) {
                    val t = i / (steps - 1f)
                    drawRect(
                        Color(0.15f + 0.75f * t, 0.15f + 0.75f * t, 0.15f + 0.75f * t),
                        topLeft = Offset(w * 0.66f, h * 0.72f * t),
                        size = Size(w * 0.34f, h * 0.72f / steps + 0.6f),
                    )
                }
                // diagonal sweep line
                val path = Path().apply {
                    moveTo(w * 0.02f, h * 0.06f)
                    lineTo(w * 0.98f, h * 0.62f)
                }
                drawPath(path, Color.White, style = androidx.compose.ui.graphics.drawscope.Stroke(width = h * 0.02f))
            }
            PatternKind.SAMPLE_LOOP -> {
                drawRect(Color(0xFF3B2F42))
                drawRect(Color(0xFF4A3C52), topLeft = Offset(0f, size.height * 0.62f), size = Size(size.width, size.height * 0.38f))
                drawRoundRect(
                    color = Color(0xFF7C8F7A),
                    topLeft = Offset(size.width * 0.10f, size.height * 0.70f),
                    size = Size(size.width * 0.26f, size.height * 0.20f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height * 0.05f),
                )
            }
            PatternKind.AUDIO_WAVE -> {
                drawRect(Color(0xFFEFEFEF))
                val mid = size.height / 2f
                var x = 0f
                var i = 0
                while (x < size.width) {
                    val amp = size.height * (0.08f + 0.34f * ((i * 37 % 11) / 10f))
                    drawLine(Color(0xFFFF8800), Offset(x, mid - amp), Offset(x, mid + amp), strokeWidth = 2.5f)
                    x += 4.5f
                    i++
                }
            }
        }
    }
}

@Composable
fun OverlayTextLabel(text: String, color: Color, fontSize: Int = 12) {
    Text(text, color = color, fontSize = fontSize.sp, textAlign = TextAlign.Center)
}
