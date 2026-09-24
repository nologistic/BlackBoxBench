package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay

object Palette {
    val Background = Color(0xFF0A1017)
    val Panel = Color(0xFF1B2734)
    val Accent = Color(0xFF16E0B0)
    val TabTextIdle = Color(0xFF93A1B0)
    val TabTextSelected = Color(0xFF0B1620)
    val TileTop = Color(0xFF364A60)
    val TileBottom = Color(0xFF223244)
    val TileBorder = Color(0xFF131E29)
    val TileFlagTop = Color(0xFF415669)
    val TileFlagBottom = Color(0xFF2A3C4F)
    val Revealed = Color(0xFF0D1721)
    val RevealedBorder = Color(0xFF19242F)
    val Faded = Color(0xFF5A4A8C)
    val FadedBorder = Color(0xFF3F3463)
    val MineTile = Color(0xFF4A1414)
    val MineBorder = Color(0xFF661C1C)
    val Exploded = Color(0xFFD32F2F)
    val LedRed = Color(0xFFFF1B3C)
    val LedAmber = Color(0xFFFFA000)
    val LoseBanner = Color(0xFF4A1414)
    val WinBanner = Color(0xFF14401A)
    val Dialog = Color(0xFF232E3C)
    val TextPrimary = Color(0xFFE6ECF2)
    val TextMuted = Color(0xFF8C99A8)
    val SwitchTrackOff = Color(0xFF3A4756)
    val SwitchThumbOff = Color(0xFF9AA7B4)
}

val NUMBER_COLORS = listOf(
    Color(0xFF4C8DF6),
    Color(0xFF43A047),
    Color(0xFFE53935),
    Color(0xFFAB47BC),
    Color(0xFFF57C00),
    Color(0xFF26A69A),
    Color(0xFFECEFF1),
    Color(0xFF90A4AE),
)

private enum class OpenDialog { NONE, CUSTOM, LICENSES }

@Composable
fun MinesweeperScreen() {
    var difficulty by remember { mutableStateOf(Difficulty.EASY) }
    var custom by remember { mutableStateOf(CustomSettings()) }
    var board by remember { mutableStateOf(createBoard(Difficulty.EASY, custom)) }
    var dialog by remember { mutableStateOf(OpenDialog.NONE) }

    LaunchedEffect(board) {
        while (true) {
            delay(1_000L)
            board.tick()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Background)
    ) {
        Spacer(Modifier.height(50.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LedDisplay(
                text = "%03d".format(board.remainingMines),
                color = Palette.LedRed,
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .padding(horizontal = 4.dp)
            )
            FaceButton(
                state = when (board.status) {
                    GameStatus.WON -> FaceState.WON
                    GameStatus.LOST -> FaceState.LOST
                    else -> FaceState.NORMAL
                },
                onClick = { board = createBoard(difficulty, custom) },
                modifier = Modifier.size(46.dp)
            )
            LedDisplay(
                text = clockText(board),
                color = if (board.timeLimitSeconds == null) Palette.LedRed else Palette.LedAmber,
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .padding(horizontal = 4.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Difficulty.entries.forEach { entry ->
                DifficultyTab(
                    difficulty = entry,
                    selected = entry == difficulty && entry != Difficulty.CUSTOM ||
                        (entry == Difficulty.CUSTOM && difficulty == Difficulty.CUSTOM),
                    mines = if (entry == Difficulty.CUSTOM) custom.mines else entry.mines,
                    modifier = Modifier
                        .weight(1f)
                        .height(66.dp),
                    onClick = {
                        if (entry == Difficulty.CUSTOM) {
                            dialog = OpenDialog.CUSTOM
                        } else {
                            difficulty = entry
                            board = createBoard(entry, custom)
                        }
                    }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        if (board.status == GameStatus.WON || board.status == GameStatus.LOST) {
            Banner(won = board.status == GameStatus.WON)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            BoardView(
                board = board,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .aspectRatio(1f / 1.08f)
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Licenses",
                color = Palette.TextMuted,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { dialog = OpenDialog.LICENSES }
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }

    when (dialog) {
        OpenDialog.CUSTOM -> CustomGameDialog(
            initial = custom,
            onCancel = { dialog = OpenDialog.NONE },
            onStart = { settings ->
                custom = settings
                difficulty = Difficulty.CUSTOM
                board = createBoard(Difficulty.CUSTOM, settings)
                dialog = OpenDialog.NONE
            }
        )

        OpenDialog.LICENSES -> LicensesDialog(onClose = { dialog = OpenDialog.NONE })
        OpenDialog.NONE -> Unit
    }
}

private fun clockText(board: MinesweeperBoard): String {
    val limit = board.timeLimitSeconds
    val seconds = if (limit == null) board.seconds else (limit - board.seconds).coerceAtLeast(0)
    return "%02d:%02d".format(seconds / 60, seconds % 60)
}

@Composable
private fun DifficultyTab(
    difficulty: Difficulty,
    selected: Boolean,
    mines: Int,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val textColor = if (selected) Palette.TabTextSelected else Palette.TabTextIdle
    Column(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Palette.Accent else Palette.Panel)
            .clickable { onClick() }
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = difficulty.title,
            color = textColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(5.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            when (difficulty) {
                Difficulty.EASY -> {
                    Text("$mines", color = textColor, fontSize = 11.sp)
                    Spacer(Modifier.width(3.dp))
                    Canvas(Modifier.size(13.dp)) { miniIcon(MiniIcon.BOMB, textColor) }
                }

                Difficulty.MEDIUM -> {
                    Canvas(Modifier.size(13.dp)) { miniIcon(MiniIcon.CLOCK, textColor) }
                }

                Difficulty.HARD -> {
                    Canvas(Modifier.size(13.dp)) { miniIcon(MiniIcon.CLOCK, textColor) }
                    Spacer(Modifier.width(4.dp))
                    Canvas(Modifier.size(13.dp)) { miniIcon(MiniIcon.BOMB, textColor) }
                    Spacer(Modifier.width(4.dp))
                    Canvas(Modifier.size(13.dp)) { miniIcon(MiniIcon.BOLT, Color(0xFFFFC107)) }
                }

                Difficulty.CUSTOM -> {
                    Canvas(Modifier.size(14.dp)) { miniIcon(MiniIcon.GEAR, textColor) }
                }
            }
        }
    }
}

@Composable
private fun Banner(won: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .height(30.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(if (won) Palette.WinBanner else Palette.LoseBanner),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Canvas(Modifier.size(15.dp)) { burstGlyph(Color(0xFFFFEB3B)) }
        Text(
            text = if (won) "You Win!" else "Game Over",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Canvas(Modifier.size(15.dp)) { burstGlyph(Color(0xFFFFEB3B)) }
    }
}

@Composable
private fun BoardView(board: MinesweeperBoard, modifier: Modifier) {
    @Suppress("UNUSED_EXPRESSION")
    board.revision
    val now = System.currentTimeMillis()
    Box(
        modifier.pointerInput(board) {
            detectTapGestures(
                onTap = { offset ->
                    cellAt(board, offset, size)?.let { (r, c) -> board.tap(r, c) }
                },
                onLongPress = { offset ->
                    cellAt(board, offset, size)?.let { (r, c) -> board.longPress(r, c) }
                }
            )
        }
    ) {
        Column(Modifier.fillMaxSize()) {
            for (r in 0 until board.rows) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    for (c in 0 until board.cols) {
                        CellView(
                            board = board,
                            row = r,
                            col = c,
                            now = now,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

/** Maps a board-relative touch position to a (row, column) pair. */
private fun cellAt(board: MinesweeperBoard, offset: Offset, size: IntSize): Pair<Int, Int>? {
    if (size.width <= 0 || size.height <= 0) return null
    val column = (offset.x / (size.width.toFloat() / board.cols)).toInt()
    val row = (offset.y / (size.height.toFloat() / board.rows)).toInt()
    if (row !in 0 until board.rows || column !in 0 until board.cols) return null
    return row to column
}

@Composable
private fun CellView(
    board: MinesweeperBoard,
    row: Int,
    col: Int,
    now: Long,
    modifier: Modifier,
) {
    val cell = board.cell(row, col)
    val faded = board.isDigitFaded(cell, now)
    BoxWithConstraints(modifier.padding(0.5.dp)) {
        val density = LocalDensity.current
        val side = minOf(maxWidth, maxHeight)
        val digitSize = with(density) { (side.toPx() * 0.55f).toSp() }
        val glyphSize = side * 0.72f
        val shape = RoundedCornerShape(side * 0.07f)
        when {
            cell.exploded -> Box(
                Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(Palette.Exploded)
                    .border(1.dp, Color(0xFFB71C1C), shape)
            ) {
                Canvas(Modifier.fillMaxSize()) { burstGlyph(Color(0xFFFFF176)) }
            }

            cell.revealed && cell.mine -> Box(
                Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(Palette.MineTile)
                    .border(1.dp, Palette.MineBorder, shape)
            ) {
                Canvas(Modifier.size(glyphSize).align(Alignment.Center)) { bombGlyph() }
            }

            cell.revealed -> Box(
                Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(if (faded) Palette.Faded else Palette.Revealed)
                    .border(1.dp, if (faded) Palette.FadedBorder else Palette.RevealedBorder, shape),
                contentAlignment = Alignment.Center
            ) {
                if (cell.adjacent > 0 && !faded) {
                    Text(
                        text = cell.adjacent.toString(),
                        color = NUMBER_COLORS[(cell.adjacent - 1).coerceIn(0, NUMBER_COLORS.lastIndex)],
                        fontSize = digitSize,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            else -> Box(
                Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                if (cell.flagged) Palette.TileFlagTop else Palette.TileTop,
                                if (cell.flagged) Palette.TileFlagBottom else Palette.TileBottom
                            )
                        )
                    )
                    .border(1.dp, Palette.TileBorder, shape),
                contentAlignment = Alignment.Center
            ) {
                if (cell.flagged) {
                    Canvas(Modifier.size(glyphSize)) { flagGlyph() }
                }
            }
        }
    }
}

@Composable
private fun CustomGameDialog(
    initial: CustomSettings,
    onCancel: () -> Unit,
    onStart: (CustomSettings) -> Unit,
) {
    var size by remember { mutableStateOf(initial.size) }
    var mines by remember { mutableStateOf(initial.mines.coerceIn(1, maxMinesFor(initial.size))) }
    var fog by remember { mutableStateOf(initial.fogOfWar) }
    var safe by remember { mutableStateOf(initial.safeFirstTap) }

    Dialog(onDismissRequest = onCancel) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Palette.Dialog)
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Text("Custom Game", color = Palette.TextPrimary, fontSize = 22.sp)
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Grid size", color = Palette.TextPrimary, fontSize = 15.sp)
                Spacer(Modifier.weight(1f))
                Text("$size × $size", color = Palette.Accent, fontSize = 15.sp)
            }
            Slider(
                value = size.toFloat(),
                onValueChange = {
                    val next = it.roundToInt().coerceIn(5, 20)
                    size = next
                    mines = mines.coerceIn(1, maxMinesFor(next))
                },
                valueRange = 5f..20f,
                steps = 14,
                colors = sliderColors()
            )
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Mines", color = Palette.TextPrimary, fontSize = 15.sp)
                Spacer(Modifier.weight(1f))
                Text("$mines (${minePercent(mines, size)}%)", color = Palette.Accent, fontSize = 15.sp)
            }
            Slider(
                value = mines.toFloat(),
                onValueChange = { mines = it.roundToInt().coerceIn(1, maxMinesFor(size)) },
                valueRange = 1f..maxMinesFor(size).toFloat(),
                colors = sliderColors()
            )
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Fog of war", color = Palette.TextPrimary, fontSize = 15.sp)
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = fog,
                    onCheckedChange = { fog = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF0B1620),
                        checkedTrackColor = Palette.Accent,
                        uncheckedThumbColor = Palette.SwitchThumbOff,
                        uncheckedTrackColor = Palette.SwitchTrackOff,
                        uncheckedBorderColor = Palette.SwitchTrackOff
                    )
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Safe first tap", color = Palette.TextPrimary, fontSize = 15.sp)
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = safe,
                    onCheckedChange = { safe = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF0B1620),
                        checkedTrackColor = Palette.Accent,
                        uncheckedThumbColor = Palette.SwitchThumbOff,
                        uncheckedTrackColor = Palette.SwitchTrackOff,
                        uncheckedBorderColor = Palette.SwitchTrackOff
                    )
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onCancel) {
                    Text("Cancel", color = Palette.Accent, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = {
                    onStart(CustomSettings(size, mines.coerceIn(1, maxMinesFor(size)), fog, safe))
                }) {
                    Text("Start", color = Palette.Accent, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun sliderColors() = SliderDefaults.colors(
    thumbColor = Palette.Accent,
    activeTrackColor = Palette.Accent,
    activeTickColor = Palette.Background,
    inactiveTrackColor = Color(0xFF2F3C4B),
    inactiveTickColor = Color(0xFF5C6A79)
)

private fun Float.roundToInt(): Int = Math.round(this)

private val LICENSE_TEXT = """
Minesweeper
Copyright (C) 2024

This program is free software: you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free Software
Foundation, either version 3 of the License, or (at your option) any later
version.

This program is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program. If not, see <https://www.gnu.org/licenses/>.

--------------------------------------------------------------------

This application uses the following open source libraries:

androidx.compose.ui:ui          - Apache License 2.0
androidx.compose.ui:ui-graphics - Apache License 2.0
androidx.compose.foundation     - Apache License 2.0
androidx.compose.animation      - Apache License 2.0
androidx.compose.material3      - Apache License 2.0
androidx.activity:activity-compose - Apache License 2.0
androidx.core:core-ktx          - Apache License 2.0
Kotlin standard library         - Apache License 2.0

--------------------------------------------------------------------

Apache License
Version 2.0, January 2004
http://www.apache.org/licenses/

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.
""".trimIndent()

@Composable
private fun LicensesDialog(onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Palette.Dialog)
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Text("Licenses", color = Palette.TextPrimary, fontSize = 22.sp)
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 430.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = LICENSE_TEXT,
                    color = Color(0xFFC4CEDA),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Start
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onClose) {
                    Text("Close", color = Palette.Accent, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
