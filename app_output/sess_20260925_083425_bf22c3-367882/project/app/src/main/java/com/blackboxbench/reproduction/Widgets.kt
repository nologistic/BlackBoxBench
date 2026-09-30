package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/** Bit masks for the seven segments of a character, a..g. */
private val SEGMENTS: Map<Char, Set<Char>> = mapOf(
    '0' to setOf('a', 'b', 'c', 'd', 'e', 'f'),
    '1' to setOf('b', 'c'),
    '2' to setOf('a', 'b', 'g', 'e', 'd'),
    '3' to setOf('a', 'b', 'g', 'c', 'd'),
    '4' to setOf('f', 'g', 'b', 'c'),
    '5' to setOf('a', 'f', 'g', 'c', 'd'),
    '6' to setOf('a', 'f', 'g', 'e', 'c', 'd'),
    '7' to setOf('a', 'b', 'c'),
    '8' to setOf('a', 'b', 'c', 'd', 'e', 'f', 'g'),
    '9' to setOf('a', 'b', 'c', 'd', 'f', 'g'),
    '-' to setOf('g')
)

/** Red LED style readout with a dark recessed panel. */
@Composable
fun SegmentPanel(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Palette.DisplayPanel)
            .border(1.dp, Palette.DisplayEdge, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 9.dp)) {
            drawSevenSegment(text, Palette.DisplayDigit)
        }
    }
}

private fun DrawScope.drawSevenSegment(text: String, color: Color) {
    if (text.isEmpty()) return
    val charCount = text.length
    // Digits are narrower than the colon separator.
    val weights = text.map { if (it == ':') 0.45f else 1f }
    val totalWeight = weights.sum()
    val gap = size.width * 0.06f
    val usable = size.width - gap * (charCount - 1)
    var x = 0f
    for ((i, ch) in text.withIndex()) {
        val w = usable * (weights[i] / totalWeight)
        if (ch == ':') {
            val r = (size.height * 0.07f).coerceAtLeast(1.5f)
            drawCircle(color, radius = r, center = Offset(x + w / 2f, size.height * 0.35f))
            drawCircle(color, radius = r, center = Offset(x + w / 2f, size.height * 0.65f))
        } else {
            drawDigit(ch, Offset(x, 0f), Size(w, size.height), color)
        }
        x += w + gap
    }
}

private fun DrawScope.drawDigit(ch: Char, topLeft: Offset, box: Size, color: Color) {
    val on = SEGMENTS[ch] ?: return
    val t = (box.height * 0.14f).coerceAtLeast(2f)
    val left = topLeft.x
    val top = topLeft.y
    val right = topLeft.x + box.width
    val bottom = topLeft.y + box.height
    val midY = top + box.height / 2f
    val inset = t * 0.6f

    fun seg(name: Char, start: Offset, end: Offset) {
        if (name in on) drawLine(color, start, end, strokeWidth = t, cap = StrokeCap.Round)
    }

    seg('a', Offset(left + inset, top + t / 2f), Offset(right - inset, top + t / 2f))
    seg('g', Offset(left + inset, midY), Offset(right - inset, midY))
    seg('d', Offset(left + inset, bottom - t / 2f), Offset(right - inset, bottom - t / 2f))
    seg('f', Offset(left + t / 2f, top + inset), Offset(left + t / 2f, midY - inset))
    seg('b', Offset(right - t / 2f, top + inset), Offset(right - t / 2f, midY - inset))
    seg('e', Offset(left + t / 2f, midY + inset), Offset(left + t / 2f, bottom - inset))
    seg('c', Offset(right - t / 2f, midY + inset), Offset(right - t / 2f, bottom - inset))
}

/** Rounded square face button used to restart the game. */
@Composable
fun FaceButton(status: GameStatus, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Palette.TabIdle)
            .clickableNoRipple(onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(40.dp)) {
            val r = size.minDimension / 2f
            val c = Offset(size.width / 2f, size.height / 2f)
            drawCircle(Palette.FaceAmber, radius = r, center = c)
            drawFace(status, c, r)
        }
    }
}

private fun DrawScope.drawFace(status: GameStatus, center: Offset, radius: Float) {
    val ink = Palette.FaceInk
    val eyeY = center.y - radius * 0.25f
    val eyeDx = radius * 0.36f
    val eyeR = radius * 0.11f
    when (status) {
        GameStatus.LOST -> {
            val d = radius * 0.16f
            for (sx in listOf(-1f, 1f)) {
                val ex = center.x + sx * eyeDx
                drawLine(ink, Offset(ex - d, eyeY - d), Offset(ex + d, eyeY + d), strokeWidth = radius * 0.1f, cap = StrokeCap.Round)
                drawLine(ink, Offset(ex + d, eyeY - d), Offset(ex - d, eyeY + d), strokeWidth = radius * 0.1f, cap = StrokeCap.Round)
            }
            drawArc(
                color = ink,
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(center.x - radius * 0.42f, center.y + radius * 0.16f),
                size = Size(radius * 0.84f, radius * 0.62f),
                style = Stroke(width = radius * 0.11f, cap = StrokeCap.Round)
            )
        }
        GameStatus.WON -> {
            // Cool face: sunglasses and a grin.
            drawRoundRect(
                color = ink,
                topLeft = Offset(center.x - radius * 0.62f, eyeY - radius * 0.2f),
                size = Size(radius * 1.24f, radius * 0.36f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius * 0.1f)
            )
            drawArc(
                color = ink,
                startAngle = 10f,
                sweepAngle = 160f,
                useCenter = false,
                topLeft = Offset(center.x - radius * 0.42f, center.y + radius * 0.1f),
                size = Size(radius * 0.84f, radius * 0.66f),
                style = Stroke(width = radius * 0.11f, cap = StrokeCap.Round)
            )
        }
        else -> {
            drawCircle(ink, radius = eyeR, center = Offset(center.x - eyeDx, eyeY))
            drawCircle(ink, radius = eyeR, center = Offset(center.x + eyeDx, eyeY))
            drawArc(
                color = ink,
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(center.x - radius * 0.45f, center.y + radius * 0.08f),
                size = Size(radius * 0.9f, radius * 0.7f),
                style = Stroke(width = radius * 0.11f, cap = StrokeCap.Round)
            )
        }
    }
}

/** One difficulty tab: title plus a small glyph / mine count underneath. */
@Composable
fun DifficultyTab(
    difficulty: Difficulty,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = modifier
            .height(58.dp)
            .clip(shape)
            .background(if (selected) Palette.TabSelected else Palette.TabIdle)
            .clickableNoRipple(onClick)
            .padding(vertical = 7.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = difficulty.title,
            color = if (selected) Palette.TabTextSelected else Palette.TabTextIdle,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (difficulty == Difficulty.CUSTOM) {
                GearGlyph(color = if (selected) Palette.TabTextSelected else Palette.TabTextIdle)
            } else {
                Text(
                    text = presetConfig(difficulty).mines.toString(),
                    color = if (selected) Palette.TabTextSelected else Palette.TabTextIdle,
                    fontSize = 11.sp
                )
                Spacer(Modifier.width(3.dp))
                MineGlyph(color = if (selected) Palette.TabTextSelected else Palette.TabTextIdle, size = 11.dp)
            }
        }
    }
}

@Composable
fun MineGlyph(color: Color, size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val c = Offset(this.size.width / 2f, this.size.height / 2f)
        val r = this.size.minDimension * 0.3f
        drawCircle(color, radius = r, center = c)
        val spoke = r * 1.7f
        for (i in 0 until 8) {
            val angle = Math.toRadians((i * 45).toDouble())
            drawLine(
                color,
                Offset(c.x + (r * 0.5f * cos(angle)).toFloat(), c.y + (r * 0.5f * sin(angle)).toFloat()),
                Offset(c.x + (spoke * cos(angle)).toFloat(), c.y + (spoke * sin(angle)).toFloat()),
                strokeWidth = this.size.minDimension * 0.11f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun GearGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(11.dp)) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val outer = size.minDimension * 0.48f
        val inner = size.minDimension * 0.18f
        drawCircle(color, radius = outer * 0.72f, center = c, style = Stroke(width = size.minDimension * 0.16f))
        for (i in 0 until 6) {
            val angle = Math.toRadians((i * 60).toDouble())
            drawLine(
                color,
                Offset(c.x + (outer * 0.6f * cos(angle)).toFloat(), c.y + (outer * 0.6f * sin(angle)).toFloat()),
                Offset(c.x + (outer * cos(angle)).toFloat(), c.y + (outer * sin(angle)).toFloat()),
                strokeWidth = size.minDimension * 0.2f,
                cap = StrokeCap.Round
            )
        }
        drawCircle(Palette.TabIdle, radius = inner, center = c)
    }
}

/** Small no-ripple clickable helper so the flat look of the target is preserved. */
@Composable
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    return this.clickable(interactionSource = source, indication = null, onClick = onClick)
}

@Composable
fun StatusBanner(status: GameStatus, modifier: Modifier = Modifier) {
    if (status != GameStatus.WON && status != GameStatus.LOST) return
    val won = status == GameStatus.WON
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (won) Palette.BannerWon else Palette.BannerLost),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (won) "\uD83C\uDF89 You Win! \uD83C\uDF89" else "\uD83D\uDCA5 Game Over \uD83D\uDCA5",
            color = if (won) Palette.BannerWonText else Palette.BannerLostText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun CircleButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(CircleShape).clickableNoRipple(onClick).fillMaxHeight())
}
