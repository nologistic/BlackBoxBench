package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlin.math.min

// ---------------------------------------------------------------- palette

private val Background = Color(0xFF0B1420)
private val SurfaceDark = Color(0xFF16202C)
private val HiddenTile = Color(0xFF26323F)
private val HiddenEdgeLight = Color(0xFF36444F)
private val HiddenEdgeDark = Color(0xFF1A2430)
private val RevealedTile = Color(0xFF131C26)
private val MineTile = Color(0xFF4A1B1B)
private val MineEdge = Color(0xFF6B2525)
private val FogTile = Color(0xFF4A3B72)
private val Accent = Color(0xFF4FD8C6)
private val AccentInk = Color(0xFF06231F)
private val TabIdle = Color(0xFF1B2735)
private val WinBanner = Color(0xFF1E6B34)
private val LoseBanner = Color(0xFF7A1F1F)
private val LedPanel = Color(0xFF1B0C09)
private val LedOff = Color(0xFF3A1512)
private val LedRed = Color(0xFFFF3B30)
private val LedAmber = Color(0xFFFF9F0A)

private val NumberColors = mapOf(
    1 to Color(0xFF4C7DFF),
    2 to Color(0xFF3FBF6F),
    3 to Color(0xFFE5484D),
    4 to Color(0xFF9B6BFF),
    5 to Color(0xFFF08A3C),
    6 to Color(0xFF2BB3B3),
    7 to Color(0xFFE05FA8),
    8 to Color(0xFFB0B8C4)
)

// ---------------------------------------------------------------- seven segment LED

private val SEGMENTS: Map<Char, Set<Int>> = mapOf(
    '0' to setOf(0, 1, 2, 3, 4, 5),
    '1' to setOf(1, 2),
    '2' to setOf(0, 1, 6, 4, 3),
    '3' to setOf(0, 1, 6, 2, 3),
    '4' to setOf(5, 6, 1, 2),
    '5' to setOf(0, 5, 6, 2, 3),
    '6' to setOf(0, 5, 6, 4, 2, 3),
    '7' to setOf(0, 1, 2),
    '8' to setOf(0, 1, 2, 3, 4, 5, 6),
    '9' to setOf(0, 1, 2, 3, 5, 6),
    '-' to setOf(6),
    ' ' to emptySet()
)

/** Draws one seven segment character inside the given box. */
private fun DrawScope.drawLedChar(ch: Char, left: Float, top: Float, w: Float, h: Float, color: Color) {
    val t = w * 0.26f
    val corner = CornerRadius(t / 2f, t / 2f)
    val lit = SEGMENTS[ch] ?: emptySet()
    fun seg(index: Int, x: Float, y: Float, sw: Float, sh: Float) {
        drawRoundRect(
            color = if (lit.contains(index)) color else LedOff,
            topLeft = Offset(x, y),
            size = Size(sw, sh),
            cornerRadius = corner
        )
    }
    // 0 top, 1 top right, 2 bottom right, 3 bottom, 4 bottom left, 5 top left, 6 middle
    seg(0, left + t * 0.6f, top, w - t * 1.2f, t)
    seg(1, left + w - t, top + t * 0.6f, t, h / 2f - t * 0.9f)
    seg(2, left + w - t, top + h / 2f + t * 0.3f, t, h / 2f - t * 0.9f)
    seg(3, left + t * 0.6f, top + h - t, w - t * 1.2f, t)
    seg(4, left, top + h / 2f + t * 0.3f, t, h / 2f - t * 0.9f)
    seg(5, left, top + t * 0.6f, t, h / 2f - t * 0.9f)
    seg(6, left + t * 0.6f, top + h / 2f - t / 2f, w - t * 1.2f, t)
}

@Composable
private fun LedDisplay(text: String, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRoundRect(color = LedPanel, cornerRadius = CornerRadius(h * 0.18f, h * 0.18f))
        val padX = w * 0.08f
        val padY = h * 0.14f
        val innerW = w - padX * 2
        val innerH = h - padY * 2
        val gap = innerW * 0.06f
        val chars = text.toList()
        val charW = (innerW - gap * (chars.size - 1)) / chars.size
        chars.forEachIndexed { index, ch ->
            val x = padX + index * (charW + gap)
            if (ch == ':') {
                val dot = charW * 0.22f
                drawCircle(color, dot, Offset(x + charW / 2f, padY + innerH * 0.3f))
                drawCircle(color, dot, Offset(x + charW / 2f, padY + innerH * 0.7f))
            } else {
                drawLedChar(ch, x, padY, charW, innerH, color)
            }
        }
    }
}

// ---------------------------------------------------------------- board canvas

@Composable
private fun BoardCanvas(game: Game, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val cell: Dp = minOf(maxWidth / game.cols, maxHeight / game.rows)
        Canvas(
            modifier = Modifier
                .size(cell * game.cols, cell * game.rows)
                .pointerInput(game) {
                    detectTapGestures(
                        onTap = { pos ->
                            val c = (pos.x / size.width * game.cols).toInt().coerceIn(0, game.cols - 1)
                            val r = (pos.y / size.height * game.rows).toInt().coerceIn(0, game.rows - 1)
                            val i = game.index(c, r)
                            val now = System.currentTimeMillis()
                            if (game.isRevealed(i) && !game.isMine(i)) game.chord(i, now)
                            else game.reveal(i, now)
                        },
                        onLongPress = { pos ->
                            val c = (pos.x / size.width * game.cols).toInt().coerceIn(0, game.cols - 1)
                            val r = (pos.y / size.height * game.rows).toInt().coerceIn(0, game.rows - 1)
                            game.toggleFlag(game.index(c, r))
                        }
                    )
                }
        ) {
            val renderVersion = game.version // read so the canvas redraws on every change
            if (renderVersion < 0) return@Canvas
            val cw = size.width / game.cols
            val chh = size.height / game.rows
            val gap = cw * 0.045f
            val radius = CornerRadius(cw * 0.12f, cw * 0.12f)
            val numberStyle = TextStyle(fontWeight = FontWeight.Bold)
            val emojiCache = HashMap<String, androidx.compose.ui.text.TextLayoutResult>()

            fun emoji(glyph: String, target: Float) = emojiCache.getOrPut(glyph) {
                measurer.measure(
                    AnnotatedString(glyph),
                    style = TextStyle(fontSize = (target * 0.62f).toSp())
                )
            }

            for (r in 0 until game.rows) {
                for (c in 0 until game.cols) {
                    val i = game.index(c, r)
                    val left = c * cw
                    val top = r * chh
                    val w = cw - gap
                    val h = chh - gap
                    val tl = Offset(left + gap / 2f, top + gap / 2f)
                    val sz = Size(w, h)
                    val revealedCell = game.isRevealed(i)
                    val mineCell = game.isMine(i)
                    val flaggedCell = game.isFlagged(i)
                    val exploded = i == game.explodedIndex

                    when {
                        exploded -> {
                            drawRoundRect(color = Color(0xFF7A1D0E), topLeft = tl, size = sz, cornerRadius = radius)
                            val center = Offset(tl.x + w / 2f, tl.y + h / 2f)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFFFFF3B0), Color(0xFFFFB020), Color(0xFFE2451B)),
                                    center = center,
                                    radius = w * 0.62f
                                ),
                                radius = w * 0.62f,
                                center = center
                            )
                            val spikes = Path()
                            val outer = w * 0.62f
                            val inner = w * 0.22f
                            for (k in 0 until 16) {
                                val angle = Math.toRadians((k * 22.5).toDouble())
                                val rad = if (k % 2 == 0) outer else inner
                                val x = center.x + (rad * Math.cos(angle)).toFloat()
                                val y = center.y + (rad * Math.sin(angle)).toFloat()
                                if (k == 0) spikes.moveTo(x, y) else spikes.lineTo(x, y)
                            }
                            spikes.close()
                            drawPath(spikes, Color(0xFFFFD34D))
                        }
                        revealedCell && mineCell -> {
                            drawRoundRect(color = MineTile, topLeft = tl, size = sz, cornerRadius = radius)
                            drawRoundRect(
                                color = MineEdge,
                                topLeft = tl,
                                size = sz,
                                cornerRadius = radius,
                                style = Stroke(width = w * 0.06f)
                            )
                            val layout = emoji("\uD83D\uDCA3", w)
                            drawText(
                                layout,
                                topLeft = Offset(
                                    tl.x + (w - layout.size.width) / 2f,
                                    tl.y + (h - layout.size.height) / 2f
                                )
                            )
                        }
                        revealedCell -> {
                            drawRoundRect(color = RevealedTile, topLeft = tl, size = sz, cornerRadius = radius)
                            if (flaggedCell) {
                                val layout = emoji("\uD83D\uDEA9", w)
                                drawText(
                                    layout,
                                    topLeft = Offset(
                                        tl.x + (w - layout.size.width) / 2f,
                                        tl.y + (h - layout.size.height) / 2f
                                    )
                                )
                            } else if (!mineCell && game.adjacentOf(i) > 0) {
                                val fogged = game.isFogged(i)
                                if (fogged) {
                                    drawRoundRect(color = FogTile, topLeft = tl, size = sz, cornerRadius = radius)
                                }
                                val layout = measurer.measure(
                                    AnnotatedString(game.adjacentOf(i).toString()),
                                    style = numberStyle.copy(fontSize = (w * 0.62f).toSp())
                                )
                                drawText(
                                    layout,
                                    color = NumberColors[game.adjacentOf(i)] ?: Color.White,
                                    topLeft = Offset(
                                        tl.x + (w - layout.size.width) / 2f,
                                        tl.y + (h - layout.size.height) / 2f
                                    ),
                                    alpha = if (fogged) 0.14f else 1f
                                )
                            }
                        }
                        else -> {
                            drawRoundRect(color = HiddenTile, topLeft = tl, size = sz, cornerRadius = radius)
                            val edge = w * 0.07f
                            drawRoundRect(
                                color = HiddenEdgeLight,
                                topLeft = Offset(tl.x, tl.y),
                                size = Size(w, edge),
                                cornerRadius = CornerRadius(edge / 2f, edge / 2f)
                            )
                            drawRoundRect(
                                color = HiddenEdgeLight,
                                topLeft = Offset(tl.x, tl.y),
                                size = Size(edge, h),
                                cornerRadius = CornerRadius(edge / 2f, edge / 2f)
                            )
                            drawRoundRect(
                                color = HiddenEdgeDark,
                                topLeft = Offset(tl.x, tl.y + h - edge),
                                size = Size(w, edge),
                                cornerRadius = CornerRadius(edge / 2f, edge / 2f)
                            )
                            drawRoundRect(
                                color = HiddenEdgeDark,
                                topLeft = Offset(tl.x + w - edge, tl.y),
                                size = Size(edge, h),
                                cornerRadius = CornerRadius(edge / 2f, edge / 2f)
                            )
                            if (flaggedCell) {
                                val layout = emoji("\uD83D\uDEA9", w)
                                drawText(
                                    layout,
                                    topLeft = Offset(
                                        tl.x + (w - layout.size.width) / 2f,
                                        tl.y + (h - layout.size.height) / 2f
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- small pieces

@Composable
private fun SmileyButton(status: GameStatus, onClick: () -> Unit) {
    val face = when (status) {
        GameStatus.PLAYING -> "\uD83D\uDE42"
        GameStatus.WON -> "\uD83D\uDE0E"
        GameStatus.LOST -> "\uD83D\uDE35"
    }
    Surface(
        color = SurfaceDark,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.size(34.dp),
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(face, fontSize = 19.sp)
        }
    }
}

@Composable
private fun DifficultyTab(
    preset: Preset,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) Accent else TabIdle,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.height(34.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                preset.title,
                fontSize = 11.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) AccentInk else Color(0xFFC7D2DC)
            )
            Text(
                preset.subtitle,
                fontSize = 8.sp,
                lineHeight = 10.sp,
                color = if (selected) AccentInk else Color(0xFF8A97A5)
            )
        }
    }
}

@Composable
private fun StatusBanner(text: String, won: Boolean, modifier: Modifier = Modifier) {
    Surface(
        color = if (won) WinBanner else LoseBanner,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.fillMaxWidth().height(22.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private const val LICENSE_TEXT = """Minesweeper (reproduction build)

This application is free software: you can redistribute it and/or modify it
under the terms of the GNU General Public License as published by the Free
Software Foundation, either version 3 of the License, or (at your option) any
later version.

This application is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
details.

--------------------------------------------------------------------------
Third party components

AndroidX / Jetpack Compose - Copyright (C) The Android Open Source Project,
licensed under the Apache License, Version 2.0.

Kotlin standard library - Copyright (C) JetBrains s.r.o., licensed under the
Apache License, Version 2.0.

You may obtain a copy of the Apache License, Version 2.0 at
http://www.apache.org/licenses/LICENSE-2.0

You may obtain a copy of the GNU General Public License, version 3, at
http://www.gnu.org/licenses/gpl-3.0.html
"""

@Composable
private fun LicensesDialog(onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        Surface(color = Color(0xFF1A2431), shape = RoundedCornerShape(18.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Licenses", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(Modifier.height(12.dp))
                Text(
                    LICENSE_TEXT,
                    fontSize = 11.sp,
                    color = Color(0xFFB9C4CF),
                    modifier = Modifier
                        .height(360.dp)
                        .verticalScroll(rememberScrollState())
                )
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onClose) { Text("Close", color = Accent) }
                }
            }
        }
    }
}

@Composable
private fun CustomGameDialog(
    initial: CustomConfig,
    onCancel: () -> Unit,
    onStart: (CustomConfig) -> Unit
) {
    var draft by remember { mutableStateOf(initial.clamped()) }
    Dialog(onDismissRequest = onCancel) {
        Surface(color = Color(0xFF1A2431), shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Custom Game", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Grid size", fontSize = 13.sp, color = Color(0xFFC7D2DC))
                    Text("${draft.size} \u00D7 ${draft.size}", fontSize = 13.sp, color = Accent)
                }
                Slider(
                    value = draft.size.toFloat(),
                    onValueChange = { v ->
                        val s = v.toInt().coerceIn(CustomConfig.MIN_SIZE, CustomConfig.MAX_SIZE)
                        draft = draft.copy(size = s, mines = draft.mines.coerceIn(1, CustomConfig.maxMines(s)))
                    },
                    valueRange = CustomConfig.MIN_SIZE.toFloat()..CustomConfig.MAX_SIZE.toFloat(),
                    steps = CustomConfig.SIZE_STEPS - 1
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Mines", fontSize = 13.sp, color = Color(0xFFC7D2DC))
                    Text("${draft.mines} (${draft.minePercent()}%)", fontSize = 13.sp, color = Accent)
                }
                val maxMines = CustomConfig.maxMines(draft.size)
                Slider(
                    value = draft.mines.toFloat(),
                    onValueChange = { v -> draft = draft.copy(mines = v.toInt().coerceIn(1, maxMines)) },
                    valueRange = 1f..maxMines.toFloat(),
                    steps = (maxMines - 2).coerceAtLeast(0)
                )

                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Fog of war", fontSize = 13.sp, color = Color(0xFFC7D2DC))
                    Switch(checked = draft.fog, onCheckedChange = { draft = draft.copy(fog = it) })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Safe first tap", fontSize = 13.sp, color = Color(0xFFC7D2DC))
                    Switch(
                        checked = draft.safeFirstTap,
                        onCheckedChange = { draft = draft.copy(safeFirstTap = it) }
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onCancel) { Text("Cancel", color = Color(0xFFC7D2DC)) }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { onStart(draft.clamped()) }) { Text("Start", color = Accent) }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- screen

@Composable
fun MinesweeperApp() {
    var preset by remember { mutableStateOf(Preset.EASY) }
    var custom by remember { mutableStateOf(CustomConfig()) }
    var game by remember { mutableStateOf(newGame(Preset.EASY, custom)) }
    var showCustom by remember { mutableStateOf(false) }
    var showLicenses by remember { mutableStateOf(false) }

    // Observe the change counter in composition so the header, banner and smiley redraw.
    val version = game.version
    val counterText = remember(version) { game.counterText() }
    val timerText = remember(version) { game.timerText() }
    val timerSeconds = remember(version) { game.timerSeconds() }

    LaunchedEffect(game) {
        var last = System.currentTimeMillis()
        while (true) {
            delay(160)
            val now = System.currentTimeMillis()
            val delta = now - last
            last = now
            if (game.status == GameStatus.PLAYING && game.started) game.tick(delta)
            game.setNow(now)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Spacer(Modifier.height(5.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LedDisplay(
                text = counterText,
                color = LedRed,
                modifier = Modifier.size(50.dp, 28.dp)
            )
            SmileyButton(game.status) {
                game = newGame(preset, custom)
            }
            LedDisplay(
                text = timerText,
                color = when {
                    !game.hasLimit -> LedRed
                    timerSeconds <= 10 -> LedRed
                    else -> LedAmber
                },
                modifier = Modifier.size(50.dp, 28.dp)
            )
        }

        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Preset.entries.forEach { entry ->
                DifficultyTab(
                    preset = entry,
                    selected = preset == entry,
                    modifier = Modifier.weight(1f)
                ) {
                    preset = entry
                    if (entry == Preset.CUSTOM) {
                        showCustom = true
                    } else {
                        game = newGame(entry, custom)
                    }
                }
            }
        }

        Box(modifier = Modifier.padding(horizontal = 8.dp)) {
            if (game.status != GameStatus.PLAYING) {
                StatusBanner(
                    text = if (game.status == GameStatus.WON) "\uD83C\uDF89 You Win! \uD83C\uDF89"
                    else "\uD83D\uDCA5 Game Over \uD83D\uDCA5",
                    won = game.status == GameStatus.WON
                )
            } else {
                Spacer(Modifier.height(22.dp))
            }
        }

        BoardCanvas(
            game = game,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(start = 8.dp, end = 8.dp, top = 96.dp, bottom = 8.dp)
        )

        Spacer(Modifier.height(16.dp))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TextButton(onClick = { showLicenses = true }) {
                Text("Licenses", fontSize = 12.sp, color = Color(0xFF8A97A5))
            }
        }
        Spacer(Modifier.height(20.dp))
    }

    if (showCustom) {
        CustomGameDialog(
            initial = custom,
            onCancel = { showCustom = false },
            onStart = { cfg ->
                custom = cfg
                preset = Preset.CUSTOM
                game = newGame(Preset.CUSTOM, cfg)
                showCustom = false
            }
        )
    }

    if (showLicenses) {
        LicensesDialog(onClose = { showLicenses = false })
    }
}
