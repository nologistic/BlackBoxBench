package com.blackboxbench.reproduction

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas

private val ClosedTop = Color(0xFF5AA0E8)
private val ClosedBottom = Color(0xFF2E6FB0)
private val ClosedEdge = Color(0x66FFFFFF)
private val OpenedBg = Color(0xFF141D2B)
private val MineCellBg = Color(0xFF233042)

private val NumberColors = mapOf(
    1 to Color(0xFF64B5F6),
    2 to Color(0xFF81C784),
    3 to Color(0xFFE57373),
    4 to Color(0xFFBA68C8),
    5 to Color(0xFFFFB74D),
    6 to Color(0xFF4DD0E1),
    7 to Color(0xFFE0E0E0),
    8 to Color(0xFF9E9E9E)
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BoardGrid(
    game: MinesweeperGame,
    revision: Int,
    onCellTap: (Int, Int) -> Unit,
    onCellLongPress: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.runtime.key(revision) {}
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 3.dp)
    ) {
        val byWidth = maxWidth / game.cols
        val byHeight = maxHeight / game.rows
        val pitchDp = if (byWidth.value < byHeight.value) byWidth else byHeight
        val density = LocalDensity.current
        val pitchPx = with(density) { pitchDp.toPx() }
        val numberStyle = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = with(density) { (pitchPx * 0.40f).toSp() },
            color = Color(0xFF64B5F6)
        )
        val flagStyle = TextStyle(
            fontSize = with(density) { (pitchPx * 0.46f).toSp() }
        )
        Column {
            for (r in 0 until game.rows) {
                Row {
                    for (c in 0 until game.cols) {
                        val cell = game.board[r][c]
                        Box(
                            modifier = Modifier
                                .size(pitchDp)
                                .padding(1.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .then(
                                    when {
                                        cell.revealed && cell.isMine ->
                                            Modifier.background(MineCellBg)
                                        cell.revealed ->
                                            Modifier.background(OpenedBg)
                                        else ->
                                            Modifier
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(ClosedTop, ClosedBottom)
                                                    )
                                                )
                                                .border(0.5.dp, ClosedEdge, RoundedCornerShape(3.dp))
                                    }
                                )
                                .combinedClickable(
                                    onClick = { onCellTap(r, c) },
                                    onLongClick = { onCellLongPress(r, c) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                cell.flagged -> {
                                    androidx.compose.material3.Text(text = "\uD83D\uDEA9", style = flagStyle)
                                }
                                cell.revealed && cell.isMine -> {
                                    Canvas(modifier = Modifier.size(pitchDp)) {
                                        drawMineIcon(size.minDimension * 0.28f)
                                    }
                                }
                                cell.revealed && cell.adjacent > 0 -> {
                                    androidx.compose.material3.Text(
                                        text = cell.adjacent.toString(),
                                        style = numberStyle.copy(color = NumberColors[cell.adjacent] ?: Color.White)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawMineIcon(radius: Float) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val spike = radius * 0.55f
    for (angle in 0 until 360 step 45) {
        val rad = Math.toRadians(angle.toDouble())
        val end = Offset(
            center.x + (radius + spike) * kotlin.math.cos(rad).toFloat(),
            center.y + (radius + spike) * kotlin.math.sin(rad).toFloat()
        )
        drawLine(
            color = Color(0xFF05070A),
            start = center,
            end = end,
            strokeWidth = radius * 0.18f
        )
    }
    drawCircle(color = Color(0xFF05070A), radius = radius, center = center)
    drawCircle(color = Color(0xFF9AB0C4), radius = radius * 0.32f, center = Offset(center.x - radius * 0.3f, center.y - radius * 0.3f))
}
