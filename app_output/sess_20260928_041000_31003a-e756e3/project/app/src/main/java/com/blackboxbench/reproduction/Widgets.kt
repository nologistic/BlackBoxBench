package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun priorityColor(p: Priority): Color = when (p) {
    Priority.HIGH -> AppColors.HighPriority
    Priority.MEDIUM -> AppColors.MediumPriority
    Priority.LOW -> AppColors.LowPriority
    Priority.NONE -> AppColors.NoPriority
}

/** Rounded square checkbox tinted by priority, filled green when completed. */
@Composable
fun PriorityCheckbox(
    priority: Priority,
    checked: Boolean,
    size: Dp = 21.dp,
    onToggle: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(5.dp)
    val border = priorityColor(priority)
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .then(
                if (checked) Modifier.background(AppColors.Green)
                else Modifier.border(2.dp, border, shape)
            )
            .then(if (onToggle != null) Modifier.clickable { onToggle() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = IconCheck,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(size * 0.66f)
            )
        }
    }
}

/** The small rounded chips shown under a task title. */
@Composable
fun Chip(
    text: String,
    glyph: Glyph? = null,
    background: Color = AppColors.Chip,
    foreground: Color = AppColors.OnSurface,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (glyph != null) {
            GlyphIcon(glyph, foreground, size = 12.dp, stroke = 1.4.dp)
            Spacer(Modifier.width(3.dp))
        }
        Text(text, fontSize = 11.sp, color = foreground, maxLines = 1)
    }
}

@Composable
fun CheckboxCircle(selected: Boolean, color: Color, size: Dp = 20.dp, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(percent = 50))
            .then(
                if (selected) Modifier.background(color)
                else Modifier.border(2.dp, color, RoundedCornerShape(percent = 50))
            )
            .clickable { onClick() }
    )
}

@Composable
fun SectionDivider() {
    Box(Modifier.background(AppColors.Divider).size(width = 1.dp, height = 0.9.dp))
}

/** Light grey "enable reminders" banner shown while notifications are disabled. */
@Composable
fun ReminderBanner(onClose: () -> Unit, onSettings: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.Banner)
            .padding(start = 18.dp, end = 12.dp, top = 16.dp, bottom = 10.dp)
    ) {
        androidx.compose.foundation.layout.Column {
            Text("启用提醒", fontSize = 17.sp, color = AppColors.OnSurface)
            Spacer(Modifier.size(6.dp))
            Text("提醒在 Android 设置中被禁用", fontSize = 13.sp, color = AppColors.SecondaryText)
            Spacer(Modifier.size(14.dp))
            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "关闭",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.OnSurface,
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .clickable { onClose() }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "设置",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(AppColors.BannerButton)
                        .clickable { onSettings() }
                        .padding(horizontal = 20.dp, vertical = 9.dp)
                )
            }
        }
    }
}
