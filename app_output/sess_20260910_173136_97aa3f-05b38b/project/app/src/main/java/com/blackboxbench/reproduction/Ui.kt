package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ActionText(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        label,
        color = VlcOrange,
        fontSize = 17.sp,
        modifier = modifier.clickable(onClick = onClick).padding(12.dp)
    )
}

@Composable
fun TopBar(title: String, onBack: (() -> Unit)? = null, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().height(64.dp).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            Text("‹", fontSize = 46.sp, color = Color(0xFF666666), modifier = Modifier.clickable(onClick = onBack).padding(end = 12.dp))
        }
        Text(title, color = VlcOrange, fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        trailing?.invoke()
    }
    HorizontalDivider(color = Color(0xFFE3E3E3))
}

@Composable
fun VlcBrand(modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(42.dp)) {
            val grey = Color(0xFF676767)
            drawLine(grey, start = center.copy(x = size.width * .18f, y = size.height * .48f), end = center.copy(x = size.width * .82f, y = size.height * .48f), strokeWidth = 4f)
            drawArc(grey, 20f, 140f, false, topLeft = center.copy(x = size.width * .08f, y = size.height * .46f), size = Size(size.width * .36f, size.height * .30f), style = Stroke(4f))
            drawArc(grey, 20f, 140f, false, topLeft = center.copy(x = size.width * .56f, y = size.height * .46f), size = Size(size.width * .36f, size.height * .30f), style = Stroke(4f))
            drawLine(grey, start = center.copy(x = size.width * .32f, y = size.height * .26f), end = center.copy(x = size.width * .68f, y = size.height * .26f), strokeWidth = 7f, cap = StrokeCap.Round)
            drawLine(grey, start = center.copy(x = size.width * .39f, y = size.height * .13f), end = center.copy(x = size.width * .61f, y = size.height * .13f), strokeWidth = 6f, cap = StrokeCap.Round)
        }
        Text("VLC", fontSize = 25.sp, color = Color(0xFF676767), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun TrafficCone(modifier: Modifier = Modifier) {
    Canvas(modifier.size(132.dp)) {
        val p = Path().apply {
            moveTo(size.width * .50f, size.height * .08f)
            lineTo(size.width * .29f, size.height * .74f)
            lineTo(size.width * .71f, size.height * .74f)
            close()
        }
        drawPath(p, VlcOrange)
        drawRoundRect(VlcOrange, topLeft = center.copy(x = size.width * .16f, y = size.height * .72f), size = Size(size.width * .68f, size.height * .18f))
        drawLine(Color.White, center.copy(x = size.width * .39f, y = size.height * .38f), center.copy(x = size.width * .61f, y = size.height * .38f), strokeWidth = 16f, cap = StrokeCap.Round)
        drawLine(Color.White, center.copy(x = size.width * .32f, y = size.height * .59f), center.copy(x = size.width * .68f, y = size.height * .59f), strokeWidth = 18f, cap = StrokeCap.Round)
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, color = VlcOrange, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp))
}

@Composable
fun SettingRow(
    title: String,
    subtitle: String = "",
    checked: Boolean? = null,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 18.sp, color = if (enabled) VlcDark else Color(0xFFB8B8B8))
            if (subtitle.isNotBlank()) Text(subtitle, fontSize = 14.sp, color = if (enabled) Color(0xFF737373) else Color(0xFFC5C5C5), lineHeight = 20.sp)
        }
        if (checked != null) Checkbox(
            checked = checked,
            onCheckedChange = { onClick() },
            enabled = enabled,
            colors = CheckboxDefaults.colors(checkedColor = VlcOrange)
        )
    }
}

@Composable
fun CategoryRow(symbol: String, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(symbol, fontSize = 25.sp, color = Color(0xFF666666), modifier = Modifier.width(52.dp))
        Text(title, fontSize = 19.sp)
    }
}

@Composable
fun VlcButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, filled: Boolean = true) {
    Box(
        modifier = modifier
            .background(if (filled) VlcOrange else Color.White, RoundedCornerShape(4.dp))
            .then(if (!filled) Modifier.border(1.dp, Color(0xFFD7D7D7), RoundedCornerShape(4.dp)) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (filled) Color.White else VlcOrange, fontSize = 17.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun BottomNav(current: String, onSelect: (String) -> Unit) {
    val items = listOf(
        Triple("video", "▤", "视频"),
        Triple("audio", "♪", "音频"),
        Triple("browse", "□", "浏览"),
        Triple("playlist", "≋", "播放列表"),
        Triple("more", "•••", "更多")
    )
    Column {
        HorizontalDivider(color = Color(0xFFE2E2E2))
        Row(Modifier.fillMaxWidth().height(74.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            items.forEach { item ->
                val active = current == item.first
                Column(
                    modifier = Modifier.weight(1f).clickable { onSelect(item.first) }.padding(top = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(item.second, fontSize = 25.sp, color = if (active) VlcOrange else Color(0xFF606060), fontWeight = FontWeight.Bold)
                    Text(item.third, fontSize = 13.sp, color = if (active) VlcOrange else Color(0xFF606060))
                }
            }
        }
    }
}
