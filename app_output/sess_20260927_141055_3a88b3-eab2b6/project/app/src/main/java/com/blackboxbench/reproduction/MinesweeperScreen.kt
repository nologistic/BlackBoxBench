package com.blackboxbench.reproduction

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val Bg = Color(0xFF090F1B)
private val Panel = Color(0xFF1B2B3B)
private val Unrevealed = Color(0xFF0C1421)
private val UnrevealedEdge = Color(0xFF05090F)
private val Revealed = Color(0xFF22313F)
private val FogRevealed = Color(0xFF332F57)
private val Teal = Color(0xFF4ECDC4)
private val TealDark = Color(0xFF06251F)
private val LabelLight = Color(0xFFE6EDF3)
private val LabelDim = Color(0xFF9AA7B4)
private val SegRed = Color(0xFFF03030)
private val SegOff = Color(0xFF3A0A0A)
private val FlagColor = Color(0xFFF06030)
private val FlagPole = Color(0xFF8A5A2B)
private val MineCell = Color(0xFF5A1414)
private val Exploded = Color(0xFFFF4A22)
private val MineDark = Color(0xFF120607)
private val DialogBg = Color(0xFF15202F)
private val WinBg = Color(0xFF00300F)
private val WinText = Color(0xFF3FD07F)
private val LoseBg = Color(0xFF3C0908)
private val LoseText = Color(0xFFFF7A4D)

private fun numberColor(n: Int): Color = when (n) {
    1 -> Color(0xFF5AA0F0)
    2 -> Color(0xFF50C070)
    3 -> Color(0xFFF05050)
    4 -> Color(0xFFA06BE0)
    5 -> Color(0xFFE5A03C)
    6 -> Color(0xFF35C4C4)
    7 -> Color(0xFFD0D0D0)
    else -> Color(0xFF9AA0A6)
}

@Composable
fun MinesweeperApp() {
    var custom by remember { mutableStateOf(CustomConfig()) }
    var state by remember { mutableStateOf(GameLogic.newGame(Difficulty.EASY, custom)) }
    var showCustom by remember { mutableStateOf(false) }
    var showLicenses by remember { mutableStateOf(false) }
    var fogVisible by remember { mutableStateOf(false) }

    LaunchedEffect(state.status) {
        while (state.status == GameStatus.PLAYING) {
            delay(1000)
            state = GameLogic.tick(state)
        }
    }
    LaunchedEffect(state.revealSeq) {
        if (state.fog && state.revealSeq > 0) {
            fogVisible = true
            delay(5000)
            fogVisible = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Bg)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)
        ) {
            Spacer(Modifier.height(28.dp))
            TopBar(
                counter = state.counter,
                seconds = state.seconds,
                status = state.status,
                onReset = { state = GameLogic.newGame(state.difficulty, custom) }
            )
            Spacer(Modifier.height(18.dp))
            DifficultyRow(
                selected = state.difficulty,
                onSelect = { d ->
                    if (d == Difficulty.CUSTOM) {
                        showCustom = true
                    } else {
                        state = GameLogic.newGame(d, custom)
                    }
                }
            )
            Spacer(Modifier.height(12.dp))
            Banner(state.status)
            Spacer(Modifier.weight(0.4f))
            Board(
                state = state,
                showNumbers = !state.fog || fogVisible,
                onTap = { state = GameLogic.tap(state, it) },
                onLongPress = { state = GameLogic.longPress(state, it) }
            )
            Spacer(Modifier.weight(0.6f))
            Text(
                text = "Licenses",
                color = LabelDim,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clickable { showLicenses = true }.padding(8.dp)
            )
            Spacer(Modifier.height(12.dp))
        }
    }

    if (showCustom) {
        CustomDialog(
            initial = custom,
            onCancel = { showCustom = false },
            onStart = { cfg ->
                custom = cfg
                state = GameLogic.newGame(Difficulty.CUSTOM, cfg)
                showCustom = false
            }
        )
    }
    if (showLicenses) {
        LicensesDialog(onClose = { showLicenses = false })
    }
}

@Composable
private fun TopBar(counter: Int, seconds: Int, status: GameStatus, onReset: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SegBox {
            val text = formatCounter(counter)
            SegText(text, digitW = 17.dp, digitH = 30.dp, on = SegRed, off = SegOff, dimLeadingZeros = true)
        }
        SmileyButton(status, onReset)
        SegBox {
            SegText(formatTime(seconds), digitW = 17.dp, digitH = 30.dp, on = SegRed, off = SegOff, dimLeadingZeros = true)
        }
    }
}

@Composable
private fun SegBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF050608))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) { content() }
}

private fun formatCounter(value: Int): String {
    val neg = value < 0
    val digits = kotlin.math.abs(value).toString()
    val body = if (neg) "-$digits" else digits
    return body.padStart(3, '0').takeLast(4)
}

private fun formatTime(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    val m = s / 60
    val sec = s % 60
    return "%02d:%02d".format(m, sec)
}

@Composable
private fun SmileyButton(status: GameStatus, onReset: () -> Unit) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF141E2C))
            .clickable { onReset() },
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(38.dp)) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val r = w * 0.42f
            drawCircle(Color(0xFFF2C230), radius = r, center = Offset(cx, cy))
            val eyeY = cy - r * 0.22f
            when (status) {
                GameStatus.LOST -> {
                    val dx = r * 0.32f
                    val dy = r * 0.18f
                    val s = r * 0.14f
                    val eyeColor = Color(0xFF2B1A05)
                    for (ex in listOf(cx - dx, cx + dx)) {
                        drawLine(eyeColor, Offset(ex - s, eyeY - s), Offset(ex + s, eyeY + s), strokeWidth = 3f)
                        drawLine(eyeColor, Offset(ex - s, eyeY + s), Offset(ex + s, eyeY - s), strokeWidth = 3f)
                    }
                    drawCircle(Color(0xFF2B1A05), radius = s * 1.1f, center = Offset(cx, cy + r * 0.42f))
                    drawCircle(Color(0xFFE05A50), radius = s * 0.7f, center = Offset(cx, cy + r * 0.5f))
                }
                else -> {
                    val eyeColor = Color(0xFF2B1A05)
                    val ex = r * 0.3f
                    drawCircle(eyeColor, radius = r * 0.09f, center = Offset(cx - ex, eyeY))
                    drawCircle(eyeColor, radius = r * 0.09f, center = Offset(cx + ex, eyeY))
                    drawArc(
                        color = eyeColor,
                        startAngle = 20f,
                        sweepAngle = 140f,
                        useCenter = false,
                        topLeft = Offset(cx - r * 0.45f, cy - r * 0.28f),
                        size = androidx.compose.ui.geometry.Size(r * 0.9f, r * 0.9f),
                        style = Stroke(width = 3f)
                    )
                    drawCircle(Color(0x55E0746A), radius = r * 0.14f, center = Offset(cx - r * 0.55f, cy + r * 0.18f))
                    drawCircle(Color(0x55E0746A), radius = r * 0.14f, center = Offset(cx + r * 0.55f, cy + r * 0.18f))
                }
            }
        }
    }
}

@Composable
private fun DifficultyRow(selected: Difficulty, onSelect: (Difficulty) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Difficulty.entries.forEach { d ->
            val active = d == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(68.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (active) Teal else Panel)
                    .clickable { onSelect(d) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = d.label,
                        color = if (active) TealDark else LabelLight,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = subtitleFor(d),
                        color = if (active) TealDark else LabelDim,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

private fun subtitleFor(d: Difficulty): String = when (d) {
    Difficulty.EASY -> "10 \uD83D\uDCA3"
    Difficulty.MEDIUM -> "\u23F1"
    Difficulty.HARD -> "\u23F1 \uD83D\uDCA3 \u26A1"
    Difficulty.CUSTOM -> "\u2699"
}

@Composable
private fun Banner(status: GameStatus) {
    val (bg, fg, text) = when (status) {
        GameStatus.WON -> Triple(WinBg, WinText, "\uD83C\uDF89 You Win! \uD83C\uDF89")
        GameStatus.LOST -> Triple(LoseBg, LoseText, "\uD83D\uDCA5 Game Over \uD83D\uDCA5")
        else -> Triple(Color.Transparent, Color.Transparent, "")
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        if (text.isNotEmpty()) {
            Text(text = text, color = fg, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Board(
    state: GameState,
    showNumbers: Boolean,
    onTap: (Int) -> Unit,
    onLongPress: (Int) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cell: Dp = maxWidth / state.size
        Column {
            for (r in 0 until state.size) {
                Row {
                    for (c in 0 until state.size) {
                        val index = r * state.size + c
                        BoardCell(
                            cell = state.cells[index],
                            fog = state.fog,
                            showNumbers = showNumbers,
                            size = cell,
                            onClick = { onTap(index) },
                            onLongClick = { onLongPress(index) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BoardCell(
    cell: Cell,
    fog: Boolean,
    showNumbers: Boolean,
    size: Dp,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val bg = when {
        cell.exploded -> Exploded
        cell.mine && cell.revealed -> MineCell
        cell.revealed -> if (fog) FogRevealed else Revealed
        else -> Unrevealed
    }
    Box(
        modifier = Modifier
            .size(size)
            .padding(1.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(bg)
            .border(1.dp, UnrevealedEdge, RoundedCornerShape(2.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center
    ) {
        if (cell.flagged && !cell.revealed) {
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) { drawFlag() }
        } else if (cell.mine && cell.revealed) {
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) { drawMine() }
        } else if (cell.revealed && cell.adjacent > 0 && showNumbers) {
            Text(
                text = cell.adjacent.toString(),
                color = numberColor(cell.adjacent),
                fontSize = (size.value * 0.6f).sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun DrawScope.drawFlag() {
    val w = size.width
    val cx = size.width / 2f
    val top = size.height * 0.18f
    val bottom = size.height * 0.82f
    drawLine(FlagPole, Offset(cx, top), Offset(cx, bottom), strokeWidth = w * 0.07f)
    val p = Path().apply {
        moveTo(cx, top)
        lineTo(cx + w * 0.34f, top + size.height * 0.14f)
        lineTo(cx, top + size.height * 0.28f)
        close()
    }
    drawPath(p, FlagColor)
}

private fun DrawScope.drawMine() {
    val w = size.width
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = w * 0.24f
    drawCircle(MineDark, radius = r, center = Offset(cx, cy))
    for (i in 0 until 8) {
        val a = Math.toRadians((i * 45).toDouble())
        val sx = cx + (r * 1.05f * kotlin.math.cos(a)).toFloat()
        val sy = cy + (r * 1.05f * kotlin.math.sin(a)).toFloat()
        val ex = cx + (r * 1.6f * kotlin.math.cos(a)).toFloat()
        val ey = cy + (r * 1.6f * kotlin.math.sin(a)).toFloat()
        drawLine(MineDark, Offset(sx, sy), Offset(ex, ey), strokeWidth = w * 0.06f)
    }
    drawCircle(Color(0x55FFFFFF), radius = r * 0.3f, center = Offset(cx - r * 0.3f, cy - r * 0.3f))
}

@Composable
private fun CustomDialog(
    initial: CustomConfig,
    onCancel: () -> Unit,
    onStart: (CustomConfig) -> Unit
) {
    var size by remember { mutableStateOf(initial.size) }
    var mines by remember { mutableStateOf(initial.mines) }
    var fog by remember { mutableStateOf(initial.fog) }
    var safe by remember { mutableStateOf(initial.safeFirstTap) }

    val maxMines = if (safe) (size * size - 9).coerceAtLeast(1) else size * size
    val clampedMines = mines.coerceIn(1, maxMines)

    Dialog(onDismissRequest = onCancel) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(DialogBg)
                .padding(22.dp)
        ) {
            Column {
                Text("Custom Game", color = LabelLight, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(20.dp))
                LabeledValue("Grid size", "$size \u00D7 $size")
                Slider(
                    value = size.toFloat(),
                    onValueChange = { size = it.roundToInt() },
                    valueRange = 5f..20f,
                    steps = 14,
                    colors = sliderColors()
                )
                Spacer(Modifier.height(8.dp))
                LabeledValue("Mines", "$clampedMines (${clampedMines * 100 / (size * size)}%)")
                Slider(
                    value = clampedMines.toFloat(),
                    onValueChange = { mines = it.roundToInt() },
                    valueRange = 1f..maxMines.toFloat(),
                    steps = (maxMines - 2).coerceAtLeast(0),
                    colors = sliderColors()
                )
                Spacer(Modifier.height(8.dp))
                ToggleRow("Fog of war", fog) { fog = it }
                Spacer(Modifier.height(4.dp))
                ToggleRow("Safe first tap", safe) { safe = it }
                Spacer(Modifier.height(18.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text(
                        "Cancel",
                        color = LabelLight,
                        fontSize = 16.sp,
                        modifier = Modifier.clickable { onCancel() }.padding(12.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Start",
                        color = Teal,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable {
                                onStart(
                                    CustomConfig(
                                        size = size,
                                        mines = clampedMines,
                                        fog = fog,
                                        safeFirstTap = safe
                                    )
                                )
                            }
                            .padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = LabelDim, fontSize = 15.sp)
        Text(value, color = LabelLight, fontSize = 15.sp)
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = LabelLight, fontSize = 15.sp)
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Teal,
                uncheckedThumbColor = Color(0xFF8A97A4),
                uncheckedTrackColor = Color(0xFF2A3646)
            )
        )
    }
}

@Composable
private fun sliderColors() = SliderDefaults.colors(
    thumbColor = Teal,
    activeTrackColor = Teal,
    inactiveTrackColor = Color(0xFF2A3646),
    activeTickColor = Color.Transparent,
    inactiveTickColor = Color(0xFF4A5A6A)
)

@Composable
private fun LicensesDialog(onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(DialogBg)
                .padding(22.dp)
        ) {
            Column {
                Text("Licenses", color = LabelLight, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp))
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Minesweeper is free software distributed under the GNU General " +
                            "Public License v3.0. The full license text is included with the " +
                            "source distribution.",
                        color = LabelDim,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "The following libraries are used under the Apache License 2.0. " +
                            "Their attribution and the full license text follow.",
                        color = LabelDim,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    val libs = listOf(
                        "androidx.compose (ui, ui-graphics, foundation, animation)",
                        "androidx.compose.material3",
                        "androidx.activity:activity-compose",
                        "androidx.core:core-ktx",
                        "Copyright (c) The Android Open Source Project",
                        "Kotlin standard library, Copyright (c) JetBrains s.r.o."
                    )
                    libs.forEach {
                        Text("\u2022 $it", color = LabelDim, fontSize = 14.sp)
                        Spacer(Modifier.height(4.dp))
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("Apache License, Version 2.0, January 2004", color = LabelLight, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("http://www.apache.org/licenses/", color = LabelDim, fontSize = 14.sp)
                    Spacer(Modifier.height(10.dp))
                    Text("TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION", color = LabelLight, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    repeat(6) {
                        Text(
                            "Licensed under the Apache License, Version 2.0 (the \"License\"); " +
                                "you may not use this file except in compliance with the License. " +
                                "You may obtain a copy of the License at the address above. " +
                                "Unless required by applicable law or agreed to in writing, software " +
                                "distributed under the License is distributed on an \"AS IS\" BASIS, " +
                                "WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.",
                            color = LabelDim,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text(
                        "Close",
                        color = Teal,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onClose() }.padding(12.dp)
                    )
                }
            }
        }
    }
}
