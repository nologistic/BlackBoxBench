package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun DialogScaffold(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = MutableInteractionSource(),
                    indication = null
                ) { },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 42.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Palette.Card)
                    .padding(horizontal = 20.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content
            )
        }
    }
}

@Composable
fun DifficultyDialog(
    onPick: (Difficulty) -> Unit,
    onCancel: () -> Unit
) {
    DialogScaffold(onDismiss = onCancel) {
        Text(
            text = "CHOOSE DIFFICULTY",
            color = Palette.TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "PLAY A DYNAMICALLY GENERATED NONOGRAM PUZZLE.",
            color = Palette.TextSecondary,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.Center,
            letterSpacing = 0.6.sp
        )
        Spacer(Modifier.height(20.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Difficulty.entries.forEachIndexed { index, difficulty ->
                StackButton(
                    text = difficulty.upper,
                    onClick = { onPick(difficulty) },
                    primary = index == 0
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(Metrics.radiusLarge))
                .clickable { onCancel() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "CANCEL",
                color = Palette.TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun LevelCompletedDialog(
    moves: Int,
    elapsedMs: Long,
    puzzle: Puzzle,
    primaryLabel: String,
    onPrimary: () -> Unit,
    onCoffee: () -> Unit,
    onHome: () -> Unit
) {
    DialogScaffold(onDismiss = { }) {
        Text(
            text = "LEVEL COMPLETED!",
            color = Palette.TextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "MOVES: $moves  •  TIME: ${formatDuration(elapsedMs)}",
            color = Palette.TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Palette.CardHigh)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            PuzzleThumbnail(puzzle = puzzle, maxSide = 132.dp)
        }
        Spacer(Modifier.height(22.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StackButton(text = primaryLabel, onClick = onPrimary, primary = true, height = 52.dp)
            StackButton(
                text = "BUY ME A COFFEE",
                onClick = onCoffee,
                height = 52.dp,
                leading = { IconCup(size = 20.dp) }
            )
            StackButton(
                text = "HOME",
                onClick = onHome,
                height = 52.dp,
                leading = { IconHome(size = 20.dp) }
            )
        }
    }
}

@Composable
private fun PuzzleThumbnail(puzzle: Puzzle, maxSide: androidx.compose.ui.unit.Dp) {
    Canvas(
        modifier = Modifier.size(
            width = maxSide,
            height = maxSide * puzzle.rows / puzzle.cols
        )
    ) {
        val cell = minOf(size.width / puzzle.cols, size.height / puzzle.rows)
        val offsetX = (size.width - cell * puzzle.cols) / 2f
        val offsetY = (size.height - cell * puzzle.rows) / 2f
        for (r in 0 until puzzle.rows) {
            for (c in 0 until puzzle.cols) {
                if (puzzle.isFilled(r, c)) {
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(offsetX + c * cell, offsetY + r * cell),
                        size = Size(cell * 0.94f, cell * 0.94f),
                        cornerRadius = CornerRadius(cell * 0.08f)
                    )
                }
            }
        }
    }
}

fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
