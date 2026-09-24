package com.blackboxbench.reproduction

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val BgColor = Color(0xFF1A1A2E)
val PanelColor = Color(0xFF232342)
val CellEmpty = Color(0xFF34345C)
val CellFilled = Color(0xFF0A0A14)
val GridLine = Color(0xFF44446E)
val MarkRed = Color(0xFFEF5350)
val AccentAmber = Color(0xFFFFC107)
val TextWhite = Color(0xFFEDEDF5)
val TextDim = Color(0xFF9E9EB8)
val GreenOk = Color(0xFF66BB6A)
val BlueCurrent = Color(0xFF4D8DF0)

fun formatTime(totalSeconds: Int): String =
    "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)

@Composable
fun PillButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    onClick: () -> Unit = {}
) {
    val borderAlpha = if (enabled) 1f else 0.35f
    Box(
        modifier = modifier
            .height(52.dp)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = when {
                    !enabled -> Color(0xFF5A5A78)
                    selected -> AccentAmber
                    else -> Color(0xFF6A6A9A)
                }.copy(alpha = borderAlpha),
                shape = RoundedCornerShape(26.dp)
            )
            .background(
                color = when {
                    selected -> Color(0xFF3A3320)
                    else -> PanelColor.copy(alpha = if (enabled) 1f else 0.5f)
                },
                shape = RoundedCornerShape(26.dp)
            )
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (!enabled) TextDim else if (selected) AccentAmber else TextWhite,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun GameScreen(
    state: GameState,
    message: String?,
    onCellTap: (Int) -> Unit,
    onBack: () -> Unit,
    onHint: () -> Unit,
    onRestart: () -> Unit,
    onModeSelect: (Boolean) -> Unit,
    onNextLevel: () -> Unit
) {
    val n = Levels.sizeOf(state.level)
    val cell = if (n == 5) 44.dp else 28.dp
    val clueSide = if (n == 5) 40.dp else 36.dp
    val clueTop = if (n == 5) 30.dp else 26.dp
    val clueFont = if (n == 5) 16.sp else 12.sp
    val rowClues = Levels.rowClues(state.level)
    val colClues = Levels.colClues(state.level)

    Box(modifier = Modifier.fillMaxSize().background(BgColor)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶栏：返回箭头 + LEVEL N
            Row(
                modifier = Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(48.dp).clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) { Text("←", color = TextWhite, fontSize = 26.sp) }
                Spacer(Modifier.weight(1f))
                Text("LEVEL ${state.level}", color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(48.dp))
            }
            // 统计行：计时器 + MOVES
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⏱ ${formatTime(state.elapsed)}", color = TextDim, fontSize = 15.sp)
                Spacer(Modifier.weight(1f))
                Text("👆 MOVES: ${state.moves}", color = TextDim, fontSize = 15.sp)
            }
            Spacer(Modifier.height(14.dp))
            // 功能按钮排：UNDO / HINT / RESTART
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PillButton("UNDO", enabled = false, modifier = Modifier.width(142.dp))
                Spacer(Modifier.width(12.dp))
                PillButton("HINT", modifier = Modifier.width(142.dp), onClick = onHint)
                Spacer(Modifier.width(12.dp))
                PillButton("RESTART", modifier = Modifier.width(142.dp), onClick = onRestart)
            }
            // 红色错误横幅
            message?.let {
                Spacer(Modifier.height(10.dp))
                Text(
                    it,
                    color = MarkRed,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.weight(1f))
            // 谜题网格
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // 列提示
                Row {
                    Spacer(Modifier.size(clueSide))
                    (0 until n).forEach { c ->
                        Box(modifier = Modifier.size(cell, clueTop), contentAlignment = Alignment.BottomCenter) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                colClues[c].forEach { num ->
                                    Text("$num", color = TextDim, fontSize = clueFont, lineHeight = clueFont)
                                }
                            }
                        }
                    }
                }
                // 行提示 + 格子
                Row {
                    Column(verticalArrangement = Arrangement.Top) {
                        (0 until n).forEach { r ->
                            Box(modifier = Modifier.size(clueSide, cell), contentAlignment = Alignment.CenterEnd) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    rowClues[r].forEach { num ->
                                        Text("$num ", color = TextDim, fontSize = clueFont)
                                    }
                                }
                            }
                        }
                    }
                    Column {
                        (0 until n).forEach { r ->
                            Row {
                                (0 until n).forEach { c ->
                                    val idx = r * n + c
                                    val filled = idx in state.fills
                                    val marked = idx in state.marks
                                    Box(
                                        modifier = Modifier
                                            .size(cell)
                                            .background(if (filled) CellFilled else CellEmpty)
                                            .border(0.5.dp, GridLine)
                                            .clickable(enabled = !state.won) { onCellTap(idx) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (marked) {
                                            Text(
                                                "✕",
                                                color = MarkRed,
                                                fontSize = if (n == 5) 22.sp else 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            // 底部模式切换
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 26.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                PillButton(
                    "FILL",
                    selected = !state.crossMode,
                    modifier = Modifier.width(170.dp)
                ) { onModeSelect(false) }
                Spacer(Modifier.width(18.dp))
                PillButton(
                    "CROSS",
                    selected = state.crossMode,
                    modifier = Modifier.width(170.dp)
                ) { onModeSelect(true) }
            }
        }
    }
}

@Composable
fun WinDialog(state: GameState, onNextLevel: () -> Unit, lastLevel: Boolean) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xCC000000))) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .width(320.dp)
                .background(PanelColor, RoundedCornerShape(18.dp))
                .padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Congratulations!", color = AccentAmber, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text("Level ${state.level} complete!", color = TextWhite, fontSize = 18.sp)
            Spacer(Modifier.height(14.dp))
            Text("Time: ${formatTime(state.elapsed)}", color = TextDim, fontSize = 15.sp)
            Text("Moves: ${state.moves}", color = TextDim, fontSize = 15.sp)
            Spacer(Modifier.height(20.dp))
            PillButton(
                if (lastLevel) "LEVEL SELECT" else "NEXT LEVEL",
                selected = true,
                modifier = Modifier.fillMaxWidth()
            ) { onNextLevel() }
        }
    }
}

@Composable
fun RestartDialog(onCancel: () -> Unit, onConfirm: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xCC000000))) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .width(320.dp)
                .background(PanelColor, RoundedCornerShape(18.dp))
                .padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Restart?", color = TextWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(
                "Are you sure you want to restart?",
                color = TextDim,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(22.dp))
            Row {
                PillButton("CANCEL", modifier = Modifier.width(130.dp)) { onCancel() }
                Spacer(Modifier.width(14.dp))
                PillButton("RESTART", selected = true, modifier = Modifier.width(130.dp)) { onConfirm() }
            }
        }
    }
}
