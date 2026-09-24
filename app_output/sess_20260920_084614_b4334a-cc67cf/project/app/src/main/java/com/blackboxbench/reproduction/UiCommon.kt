package com.blackboxbench.reproduction

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val AppBackground = Color(0xFF101010)
val AppPanel = Color(0xFF1D1D1D)
val AppPanelDark = Color(0xFF181818)
val AppBorder = Color(0xFF3B3B3B)
val AppMuted = Color(0xFF909090)
val AppWhite = Color(0xFFF8F8F8)
val AppRed = Color(0xFFFF454B)
val AppYellow = Color(0xFFFFC400)

@Composable
fun ScreenRoot(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) { content() }
}

@Composable
fun TallText(
    text: String,
    modifier: Modifier = Modifier,
    size: Int = 18,
    color: Color = AppWhite,
    weight: FontWeight = FontWeight.ExtraBold,
    align: TextAlign = TextAlign.Start,
    spacing: Float = 0.3f
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = size.sp,
        fontWeight = weight,
        textAlign = align,
        letterSpacing = spacing.sp,
        lineHeight = (size * 1.18f).sp
    )
}

@Composable
fun BackHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            TallText("‹", size = 55, weight = FontWeight.Light, spacing = 0f)
        }
        TallText(title, modifier = Modifier.padding(start = 16.dp), size = 23)
    }
}

@Composable
fun CircleBadge(symbol: String, color: Color) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .border(1.dp, AppBorder, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        TallText(symbol, size = 25, color = color, align = TextAlign.Center, spacing = 0f)
    }
}

@Composable
fun MenuButton(
    label: String,
    onClick: () -> Unit,
    primary: Boolean = false,
    modifier: Modifier = Modifier,
    height: Dp = 54.dp,
    symbol: String? = null
) {
    val background = if (primary) Color.Black else AppPanel
    val border = if (primary) null else BorderStroke(1.dp, AppBorder)
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .shadow(3.dp, RoundedCornerShape(15.dp)),
        shape = RoundedCornerShape(15.dp),
        border = border,
        colors = ButtonDefaults.buttonColors(
            containerColor = background,
            contentColor = AppWhite,
            disabledContainerColor = AppPanel,
            disabledContentColor = AppMuted
        ),
        contentPadding = ButtonDefaults.ContentPadding
    ) {
        if (symbol != null) {
            TallText(symbol, size = 21, modifier = Modifier.padding(end = 10.dp), spacing = 0f)
        }
        TallText(label, size = 17, align = TextAlign.Center)
    }
}

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    corner: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(AppPanel, RoundedCornerShape(corner))
            .border(1.dp, AppBorder, RoundedCornerShape(corner))
    ) { content() }
}

@Composable
fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(67.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TallText(label, size = 16)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AppWhite,
                checkedTrackColor = Color(0xFF6B6B6B),
                uncheckedThumbColor = Color(0xFF8B8B8B),
                uncheckedTrackColor = Color.Transparent,
                uncheckedBorderColor = AppWhite,
                checkedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
fun SectionDivider() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFF303030))
    )
}

@Composable
fun SmallToolButton(
    symbol: String,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .height(43.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, AppBorder),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppPanel,
            contentColor = AppWhite,
            disabledContainerColor = AppPanel,
            disabledContentColor = Color(0xFF535353)
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
    ) {
        TallText(symbol, size = 20, color = if (enabled) AppWhite else Color(0xFF535353), spacing = 0f)
        TallText(label, size = 10, color = if (enabled) AppWhite else Color(0xFF535353), spacing = 0f, modifier = Modifier.padding(start = 5.dp))
    }
}
