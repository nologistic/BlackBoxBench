package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ScreenHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 18.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = bottomPadding)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            IconBackChevron(size = 26.dp)
        }
        Text(
            text = title,
            color = Palette.TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

/** Dark rounded surface used for every list / section container. */
@Composable
fun PanelCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Metrics.radiusLarge))
            .background(Palette.Card)
            .border(1.dp, Palette.BorderSoft, RoundedCornerShape(Metrics.radiusLarge))
    ) {
        content()
    }
}

@Composable
fun StackButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    height: Dp = Metrics.buttonHeight,
    leading: (@Composable () -> Unit)? = null
) {
    val background = if (primary) Palette.Black else Palette.Card
    val border = if (primary) Palette.Border else Palette.BorderSoft
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(Metrics.radiusLarge))
            .background(background)
            .border(1.dp, border, RoundedCornerShape(Metrics.radiusLarge))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = text,
                color = Palette.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ToolButton(
    text: String,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val textColor = if (enabled) Palette.TextPrimary else Palette.TextMuted
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(Metrics.radiusLarge))
            .background(Palette.Card)
            .border(1.dp, Palette.BorderSoft, RoundedCornerShape(Metrics.radiusLarge))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp)
        ) {
            leading()
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                color = textColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun LabelText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = Palette.TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = modifier
    )
}

@Composable
fun SelectableChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = if (selected) Palette.White else Palette.CardHigh
    val contentColor = if (selected) Palette.Black else Palette.TextPrimary
    Row(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(Metrics.radiusSmall))
            .background(background)
            .border(1.dp, if (selected) Palette.White else Palette.BorderSoft, RoundedCornerShape(Metrics.radiusSmall))
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (selected) {
            IconCheck(size = 15.dp, color = contentColor)
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text,
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun NonoSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = 54.dp, height = 32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (checked) Palette.White else Color.Transparent)
            .border(
                2.dp,
                if (checked) Palette.White else Palette.TextMuted,
                RoundedCornerShape(16.dp)
            )
            .clickable { onCheckedChange(!checked) },
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .size(22.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(if (checked) Palette.White else Palette.TextMuted)
                .border(
                    2.dp,
                    if (checked) Palette.Background else Palette.TextMuted,
                    RoundedCornerShape(11.dp)
                )
        )
    }
}
