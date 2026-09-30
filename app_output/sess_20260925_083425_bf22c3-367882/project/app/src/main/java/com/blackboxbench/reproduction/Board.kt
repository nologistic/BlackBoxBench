package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GameBoard(game: GameController, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Palette.BoardBackdrop)
            .pointerInput(game) {
                detectTapGestures(
                    onTap = { offset ->
                        hitCell(offset, size.width.toFloat(), size.height.toFloat(), game.size)
                            ?.let { (row, col) -> game.tap(row, col) }
                    },
                    onLongPress = { offset ->
                        hitCell(offset, size.width.toFloat(), size.height.toFloat(), game.size)
                            ?.let { (row, col) -> game.toggleFlag(row, col) }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawBoard(game, measurer)
        }
    }
}

private fun hitCell(offset: Offset, width: Float, height: Float, n: Int): Pair<Int, Int>? {
    if (width <= 0f || height <= 0f) return null
    val col = (offset.x / (width / n)).toInt()
    val row = (offset.y / (height / n)).toInt()
    if (row !in 0 until n || col !in 0 until n) return null
    return row to col
}

private fun DrawScope.drawBoard(game: GameController, measurer: TextMeasurer) {
    val n = game.size
    if (n <= 0) return
    val cw = size.width / n
    val ch = size.height / n
    val gap = (cw * 0.055f).coerceAtLeast(1f)
    val corner = CornerRadius((cw * 0.17f).coerceIn(2f, 12f))
    val fog = game.config.fog

    for (row in 0 until n) {
        for (col in 0 until n) {
            val cell = game.cellAt(row, col)
            val x = col * cw
            val y = row * ch
            val topLeft = Offset(x + gap / 2f, y + gap / 2f)
            val tile = Size(cw - gap, ch - gap)

            if (!cell.revealed) {
                drawCovered(topLeft, tile, corner)
                if (cell.flagged) drawFlag(topLeft, tile)
                continue
            }

            when {
                cell.exploded -> {
                    drawRoundRect(Palette.ExplodedTile, topLeft, tile, corner)
                    drawBurst(Offset(x + cw / 2f, y + ch / 2f), cw)
                }
                cell.mine -> {
                    drawRoundRect(Palette.Revealed, topLeft, tile, corner)
                    drawMine(Offset(x + cw / 2f, y + ch / 2f), cw)
                }
                fog && !game.numberVisible(cell) -> {
                    drawRoundRect(Palette.Fogged, topLeft, tile, corner)
                    if (cell.flagged) drawFlag(topLeft, tile)
                }
                else -> {
                    drawRoundRect(Palette.Revealed, topLeft, tile, corner)
                    if (cell.flagged) drawFlag(topLeft, tile)
                    if (cell.adjacent > 0) {
                        drawNumber(cell.adjacent, x, y, cw, ch, measurer)
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawCovered(topLeft: Offset, tile: Size, corner: CornerRadius) {
    drawRoundRect(Palette.CoverTop, topLeft, tile, corner)
    val inset = tile.width * 0.12f
    drawLine(
        color = Palette.CoverEdge,
        start = Offset(topLeft.x + inset, topLeft.y + tile.height * 0.08f),
        end = Offset(topLeft.x + tile.width - inset, topLeft.y + tile.height * 0.08f),
        strokeWidth = (tile.height * 0.07f).coerceAtLeast(1f),
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawNumber(value: Int, x: Float, y: Float, cw: Float, ch: Float, measurer: TextMeasurer) {
    val layout = measurer.measure(
        text = value.toString(),
        style = TextStyle(
            color = Palette.numberColor(value),
            fontSize = (cw * 0.62f).toSp(),
            fontWeight = FontWeight.Bold
        )
    )
    drawText(
        textLayoutResult = layout,
        topLeft = Offset(
            x + (cw - layout.size.width) / 2f,
            y + (ch - layout.size.height) / 2f
        )
    )
}

private fun DrawScope.drawFlag(topLeft: Offset, tile: Size) {
    val poleX = topLeft.x + tile.width * 0.36f
    val topY = topLeft.y + tile.height * 0.22f
    val bottomY = topLeft.y + tile.height * 0.78f
    drawLine(
        color = Palette.FlagPole,
        start = Offset(poleX, topY),
        end = Offset(poleX, bottomY),
        strokeWidth = (tile.width * 0.09f).coerceAtLeast(1.5f),
        cap = StrokeCap.Round
    )
    val path = Path().apply {
        moveTo(poleX, topY)
        lineTo(topLeft.x + tile.width * 0.72f, topY + tile.height * 0.17f)
        lineTo(poleX, topY + tile.height * 0.34f)
        close()
    }
    drawPath(path, Palette.FlagCloth)
}

private fun DrawScope.drawMine(center: Offset, cell: Float) {
    val body = cell * 0.21f
    val spoke = body * 1.7f
    for (i in 0 until 8) {
        val angle = Math.toRadians((i * 45).toDouble())
        drawLine(
            color = Palette.MineSpike,
            start = Offset(center.x + (body * 0.4f * cos(angle)).toFloat(), center.y + (body * 0.4f * sin(angle)).toFloat()),
            end = Offset(center.x + (spoke * cos(angle)).toFloat(), center.y + (spoke * sin(angle)).toFloat()),
            strokeWidth = (cell * 0.05f).coerceAtLeast(1f),
            cap = StrokeCap.Round
        )
    }
    drawCircle(Palette.MineBody, radius = body, center = center)
    drawCircle(
        color = Palette.FlagCloth,
        radius = body * 0.24f,
        center = Offset(center.x - body * 0.28f, center.y - body * 0.28f)
    )
}

private fun DrawScope.drawBurst(center: Offset, cell: Float) {
    val r = cell * 0.34f
    for (i in 0 until 8) {
        val angle = Math.toRadians((i * 45).toDouble())
        drawLine(
            color = Palette.FlagCloth,
            start = Offset(center.x + (r * 0.5f * cos(angle)).toFloat(), center.y + (r * 0.5f * sin(angle)).toFloat()),
            end = Offset(center.x + (r * 1.25f * cos(angle)).toFloat(), center.y + (r * 1.25f * sin(angle)).toFloat()),
            strokeWidth = (cell * 0.08f).coerceAtLeast(1f),
            cap = StrokeCap.Round
        )
    }
    drawCircle(Palette.FaceAmber, radius = r * 0.62f, center = center)
    drawCircle(
        color = Palette.ExplodedTile,
        radius = r * 0.3f,
        center = center,
        style = Stroke(width = (cell * 0.03f).coerceAtLeast(1f))
    )
}
