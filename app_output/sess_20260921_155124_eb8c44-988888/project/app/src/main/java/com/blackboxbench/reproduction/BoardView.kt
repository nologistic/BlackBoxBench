package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * The play field. Cells are stretched to fill a square area, exactly like the observed app:
 * column width = area / cols and row height = area / rows.
 */
@Composable
fun BoardView(
    state: GameState,
    now: Long,
    onTap: (Int) -> Unit,
    onLongPress: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier.aspectRatio(1f)) {
        val cellW = maxWidth / state.cols
        val cellH = maxHeight / state.rows
        val glyph = with(LocalDensity.current) { (minOf(cellW, cellH) * 0.68f).toSp() }
        Column(Modifier.fillMaxSize()) {
            for (r in 0 until state.rows) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    for (c in 0 until state.cols) {
                        val index = r * state.cols + c
                        CellTile(
                            state = state,
                            index = index,
                            now = now,
                            glyph = glyph,
                            onTap = onTap,
                            onLongPress = onLongPress,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CellTile(
    state: GameState,
    index: Int,
    now: Long,
    glyph: androidx.compose.ui.unit.TextUnit,
    onTap: (Int) -> Unit,
    onLongPress: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val cell = state.cells[index]
    val fogged = state.isFogged(index, now)
    Box(
        modifier
            .padding(0.4.dp)
            .pointerInput(index, state.phase, fogged) {
                detectTapGestures(
                    onTap = { onTap(index) },
                    onLongPress = { onLongPress(index) }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            when {
                cell.exploded -> drawRoundRect(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFFFFF0A8), Color(0xFFFFA318), Color(0xFFE23B0A)),
                        center = center,
                        radius = size.minDimension * 0.75f
                    ),
                    cornerRadius = CornerRadius(size.minDimension * 0.14f)
                )
                cell.revealed && cell.mine -> drawRoundRect(
                    color = Palette.MineTile,
                    cornerRadius = CornerRadius(size.minDimension * 0.14f)
                )
                cell.revealed -> {
                    drawRoundRect(
                        color = Palette.TileFlat,
                        cornerRadius = CornerRadius(size.minDimension * 0.12f)
                    )
                    if (fogged) {
                        drawRoundRect(
                            color = Palette.Fog.copy(alpha = 0.94f),
                            cornerRadius = CornerRadius(size.minDimension * 0.12f)
                        )
                    }
                }
                else -> drawRaisedTile()
            }
        }
        when {
            cell.exploded -> Text("\uD83D\uDCA5", fontSize = glyph)
            cell.revealed && cell.mine -> Text("\uD83D\uDCA3", fontSize = glyph)
            cell.revealed && !fogged && cell.adjacent > 0 -> Text(
                text = cell.adjacent.toString(),
                color = Palette.number(cell.adjacent),
                fontSize = glyph,
                fontWeight = FontWeight.Bold
            )
            !cell.revealed && cell.flagged -> FlagGlyph(Modifier.fillMaxSize(0.68f))
        }
    }
}

private fun DrawScope.drawRaisedTile() {
    val radius = CornerRadius(size.minDimension * 0.12f)
    drawRoundRect(color = Palette.TileEdge, cornerRadius = radius)
    val inset = size.minDimension * 0.035f
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Palette.TileTop, Palette.TileBottom)),
        topLeft = Offset(inset, inset),
        size = Size(size.width - inset * 2f, size.height - inset * 2f),
        cornerRadius = CornerRadius(size.minDimension * 0.10f)
    )
}
