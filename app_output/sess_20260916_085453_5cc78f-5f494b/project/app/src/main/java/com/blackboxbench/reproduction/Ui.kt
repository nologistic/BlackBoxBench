package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.Color as AndroidColor

val Ink = Color(0xFF1B2B4B)
val InkSoft = Color(0xFF5A6B8A)
val PageBg = Color(0xFFFBFBFE)
val FieldBg = Color(0xFFE7EAF6)
val KeyLight = Color(0xFFEEEFF7)
val KeyDark = Color(0xFFD6D9EB)

@Composable
fun HistoryGlyph(color: Color, sizeDp: Int = 26) {
    Canvas(Modifier.size(sizeDp.dp)) {
        val s = size.minDimension
        val stroke = s * 0.075f
        drawCircle(color, radius = s / 2f - stroke / 2f, style = Stroke(stroke))
        drawLine(color, Offset(s / 2f, s / 2f), Offset(s / 2f, s * 0.26f), stroke, StrokeCap.Round)
        drawLine(color, Offset(s / 2f, s / 2f), Offset(s * 0.74f, s * 0.5f), stroke, StrokeCap.Round)
    }
}

@Composable
fun ConverterGlyph(color: Color, sizeDp: Int = 26) {
    Canvas(Modifier.size(sizeDp.dp)) {
        val s = size.minDimension
        val stroke = s * 0.075f
        drawLine(
            color,
            Offset(s * 0.24f, s * 0.76f),
            Offset(s * 0.76f, s * 0.24f),
            stroke,
            StrokeCap.Round
        )
        drawLine(
            color,
            Offset(s * 0.5f, s * 0.5f),
            Offset(s * 0.78f, s * 0.5f),
            stroke,
            StrokeCap.Round
        )
        drawLine(
            color,
            Offset(s * 0.5f, s * 0.5f),
            Offset(s * 0.5f, s * 0.78f),
            stroke,
            StrokeCap.Round
        )
    }
}

@Composable
fun OverflowGlyph(color: Color, sizeDp: Int = 24) {
    Canvas(Modifier.size(sizeDp.dp)) {
        val r = size.width * 0.085f
        for (i in 0..2) {
            drawCircle(color, r, Offset(size.width / 2f, size.height * (0.22f + 0.28f * i)))
        }
    }
}

@Composable
fun BackGlyph(color: Color, sizeDp: Int = 26) {
    Canvas(Modifier.size(sizeDp.dp)) {
        val s = size.minDimension
        val stroke = s * 0.08f
        drawLine(color, Offset(s * 0.62f, s * 0.2f), Offset(s * 0.3f, s * 0.5f), stroke, StrokeCap.Round)
        drawLine(color, Offset(s * 0.3f, s * 0.5f), Offset(s * 0.62f, s * 0.8f), stroke, StrokeCap.Round)
        drawLine(color, Offset(s * 0.3f, s * 0.5f), Offset(s * 0.84f, s * 0.5f), stroke, StrokeCap.Round)
    }
}

@Composable
fun IconTap(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
fun ScreenTitle(title: String, onBack: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 8.dp, end = 16.dp, top = 6.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconTap(onClick = onBack) { BackGlyph(Ink) }
        } else {
            Spacer(Modifier.width(8.dp))
        }
        Text(title, color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Normal)
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        color = InkSoft,
        fontSize = 16.sp,
        modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 8.dp)
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
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
            Text(title, color = Ink, fontSize = 19.sp)
            if (subtitle != null) {
                Text(subtitle, color = InkSoft, fontSize = 15.sp)
            }
        }
        if (trailing != null) trailing()
    }
}

@Composable
fun ToggleSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    androidx.compose.material3.Switch(checked = checked, onCheckedChange = onChange)
}

@Composable
fun RadioDot(selected: Boolean, color: Color = Color(0xFF415F91)) {
    Canvas(Modifier.size(24.dp)) {
        val s = size.minDimension
        if (selected) {
            drawCircle(color, radius = s * 0.34f)
            drawCircle(color, radius = s * 0.46f, style = Stroke(s * 0.09f))
        } else {
            drawCircle(InkSoft, radius = s * 0.46f, style = Stroke(s * 0.075f))
        }
    }
}

@Composable
fun Divider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0x14000000))
    )
}

fun colorFromHex(hex: String): Color {
    return try {
        Color(AndroidColor.parseColor(hex))
    } catch (e: Exception) {
        Color(0xFF106D1F)
    }
}

fun toHex(color: Color): String {
    val argb = AndroidColor.argb(
        (color.alpha * 255).toInt(),
        (color.red * 255).toInt(),
        (color.green * 255).toInt(),
        (color.blue * 255).toInt()
    )
    return String.format("#%06X", 0xFFFFFF and argb)
}

@Composable
fun ValuePill(value: String, symbol: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            value,
            color = Ink,
            fontSize = if (value.length > 11) 26.sp else 40.sp,
            fontWeight = FontWeight.Normal
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFCDD2E8))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(symbol, color = Ink, fontSize = 18.sp)
        }
    }
}