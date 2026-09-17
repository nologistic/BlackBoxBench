package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    onMore: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Box(
                modifier = Modifier.size(36.dp).clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) { BackGlyph(ClockText, 26.dp) }
            Spacer(Modifier.width(8.dp))
        }
        Text(
            title,
            color = ClockText,
            fontSize = 30.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
        if (onMore != null) {
            Box(
                modifier = Modifier.size(36.dp).clickable { onMore() },
                contentAlignment = Alignment.Center,
            ) { MoreGlyph(ClockText, 22.dp) }
        }
    }
}

@Composable
fun PlusButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(78.dp)
            .clip(CircleShape)
            .background(ClockAccent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { PlusGlyph(Color(0xFF16233A), 34.dp) }
}

@Composable
fun Card(
    modifier: Modifier = Modifier,
    color: Color = ClockSurface,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(color),
    ) { Column(modifier = Modifier.padding(0.dp)) { content() } }
}

@Composable
fun GroupHeader(text: String, withIcon: Boolean = true) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 26.dp, top = 14.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (withIcon) {
            ClockTabIcon(ClockTextDim, 20.dp)
            Spacer(Modifier.width(12.dp))
        }
        Text(text, color = ClockText, fontSize = 17.sp)
    }
}

@Composable
fun ListRow(
    icon: @Composable (() -> Unit)?,
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 26.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) { icon() }
            Spacer(Modifier.width(20.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(title, color = ClockText, fontSize = 18.sp)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = ClockTextDim, fontSize = 15.sp)
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun Toggle(checked: Boolean, onChange: (Boolean) -> Unit) {
    val track = if (checked) ClockAccentDim else Color(0xFF4A4E57)
    Box(
        modifier = Modifier
            .size(width = 58.dp, height = 34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(track)
            .clickable { onChange(!checked) },
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .size(26.dp)
                .clip(CircleShape)
                .background(if (checked) ClockText else Color(0xFFD6D8DC)),
        )
    }
}

@Composable
fun CheckCircle(checked: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(28.dp).clip(CircleShape).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Box(
                modifier = Modifier.size(26.dp).clip(CircleShape).background(ClockAccent),
                contentAlignment = Alignment.Center,
            ) { CheckGlyph(Color(0xFF16233A), 18.dp) }
        } else {
            androidx.compose.foundation.Canvas(modifier = Modifier.size(24.dp)) {
                drawCircle(
                    color = ClockTextDim,
                    radius = size.minDimension / 2 - 3f,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f),
                )
            }
        }
    }
}

@Composable
fun PillButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(if (enabled) ClockAccent else ClockSurfaceHigh)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 30.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (enabled) Color(0xFF16233A) else ClockTextDim, fontSize = 17.sp)
    }
}

@Composable
fun OutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 26.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = ClockText, fontSize = 17.sp)
    }
}

@Composable
fun Divider() {
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF2B2E35)))
}