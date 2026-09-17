package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// Palette
// ---------------------------------------------------------------------------
private val BgTop = Color(0xFF0E1A26)
private val BgBottom = Color(0xFF091017)
private val Panel = Color(0xFF16222D)
private val Covered = Color(0xFF22303F)
private val CoveredBorder = Color(0xFF334453)
private val Revealed = Color(0xFF0D1620)
private val FogRevealed = Color(0xFF40356B)
private val Teal = Color(0xFF3ED9C4)
private val TealDark = Color(0xFF08231F)
private val DigitRed = Color(0xFFFF3B30)
private val DigitAmber = Color(0xFFFFB020)
private val PanelRed = Color(0xFF2C0A0B)
private val MineCell = Color(0xFF5A1414)
private val BoomCell = Color(0xFFB4241A)
private val FlagColor = Color(0xFFF0633A)
private val Muted = Color(0xFF8FA0AE)

private fun numberColor(n: Int): Color = when (n) {
    1 -> Color(0xFF3B82F6)
    2 -> Color(0xFF22C55E)
    3 -> Color(0xFFEF4444)
    4 -> Color(0xFFA855F7)
    5 -> Color(0xFFF59E0B)
    6 -> Color(0xFF06B6D4)
    7 -> Color(0xFFE5E7EB)
    else -> Color(0xFF9CA3AF)
}

// ---------------------------------------------------------------------------
// Model
// ---------------------------------------------------------------------------
private enum class Status { READY, PLAYING, WON, LOST }

private class Cell {
    var mine by mutableStateOf(false)
    var revealed by mutableStateOf(false)
    var flagged by mutableStateOf(false)
    var adjacent by mutableStateOf(0)
}

private class Board(val rows: Int, val cols: Int, val mines: Int, val safeFirstTap: Boolean) {
    val grid: Array<Array<Cell>> = Array(rows) { Array(cols) { Cell() } }
    var status = Status.READY
    private var placed = false
    var revealedCount = 0
    var flagCount = 0
    var explodedRow = -1
    var explodedCol = -1

    private fun neighbors(r: Int, c: Int): List<Pair<Int, Int>> {
        val out = ArrayList<Pair<Int, Int>>(8)
        for (dr in -1..1) for (dc in -1..1) {
            if (dr == 0 && dc == 0) continue
            val nr = r + dr
            val nc = c + dc
            if (nr in 0 until rows && nc in 0 until cols) out.add(nr to nc)
        }
        return out
    }

    private fun place(excludeR: Int, excludeC: Int) {
        val candidates = ArrayList<Pair<Int, Int>>(rows * cols)
        for (r in 0 until rows) for (c in 0 until cols) {
            if (safeFirstTap && r == excludeR && c == excludeC) continue
            candidates.add(r to c)
        }
        candidates.shuffle()
        var remaining = mines.coerceAtMost(candidates.size)
        var i = 0
        while (i < candidates.size && remaining > 0) {
            val (r, c) = candidates[i]
            grid[r][c].mine = true
            remaining--
            i++
        }
        for (r in 0 until rows) for (c in 0 until cols) {
            grid[r][c].adjacent = neighbors(r, c).count { grid[it.first][it.second].mine }
        }
        placed = true
    }

    private fun checkWin() {
        if (revealedCount == rows * cols - mines) {
            status = Status.WON
            for (r in 0 until rows) for (c in 0 until cols) {
                if (grid[r][c].mine) grid[r][c].flagged = true
            }
            flagCount = mines
        }
    }

    private fun lose(row: Int, col: Int, boom: Boolean) {
        grid[row][col].revealed = true
        if (boom) {
            explodedRow = row
            explodedCol = col
        }
        status = Status.LOST
        for (r in 0 until rows) for (c in 0 until cols) {
            if (grid[r][c].mine) grid[r][c].revealed = true
        }
    }

    fun timeout() {
        if (status != Status.READY && status != Status.PLAYING) return
        status = Status.LOST
        for (r in 0 until rows) for (c in 0 until cols) {
            if (grid[r][c].mine) grid[r][c].revealed = true
        }
    }

    private fun flood(sr: Int, sc: Int) {
        val stack = ArrayDeque<Pair<Int, Int>>()
        stack.addLast(sr to sc)
        while (stack.isNotEmpty()) {
            val (r, c) = stack.removeLast()
            val cell = grid[r][c]
            if (cell.revealed || cell.flagged || cell.mine) continue
            cell.revealed = true
            revealedCount++
            if (cell.adjacent == 0) {
                for ((nr, nc) in neighbors(r, c)) {
                    val n = grid[nr][nc]
                    if (!n.revealed && !n.flagged && !n.mine) stack.addLast(nr to nc)
                }
            }
        }
    }

    fun reveal(r: Int, c: Int) {
        if (status == Status.WON || status == Status.LOST) return
        val cell = grid[r][c]
        if (cell.revealed || cell.flagged) return
        if (!placed) place(r, c)
        status = Status.PLAYING
        if (cell.mine) {
            lose(r, c, true)
            return
        }
        flood(r, c)
        checkWin()
    }

    fun toggleFlag(r: Int, c: Int) {
        if (status == Status.WON || status == Status.LOST) return
        val cell = grid[r][c]
        if (cell.revealed) return
        cell.flagged = !cell.flagged
        flagCount += if (cell.flagged) 1 else -1
    }

    fun chord(r: Int, c: Int) {
        if (status == Status.WON || status == Status.LOST) return
        val cell = grid[r][c]
        if (!cell.revealed || cell.adjacent == 0) return
        val ns = neighbors(r, c)
        if (ns.count { grid[it.first][it.second].flagged } != cell.adjacent) return
        for ((nr, nc) in ns) {
            val n = grid[nr][nc]
            if (!n.flagged && !n.revealed) {
                if (n.mine) {
                    lose(nr, nc, true)
                    return
                }
                flood(nr, nc)
            }
        }
        checkWin()
    }
}

private data class GameConfig(
    val name: String,
    val rows: Int,
    val cols: Int,
    val mines: Int,
    val fog: Boolean,
    val safeFirstTap: Boolean,
    val timeLimit: Int
)

private fun easyConfig() = GameConfig("Easy", 9, 9, 10, false, true, 0)
private fun mediumConfig() = GameConfig("Medium", 12, 12, 30, false, true, 300)
private fun hardConfig() = GameConfig("Hard", 14, 14, 50, true, true, 180)

// ---------------------------------------------------------------------------
// Root screen
// ---------------------------------------------------------------------------
@Composable
fun MinesweeperApp() {
    var config by remember { mutableStateOf(easyConfig()) }
    var board by remember { mutableStateOf(Board(config.rows, config.cols, config.mines, config.safeFirstTap)) }
    var version by remember { mutableIntStateOf(0) }
    var elapsed by remember { mutableIntStateOf(0) }
    var showCustom by remember { mutableStateOf(false) }
    var showLicenses by remember { mutableStateOf(false) }

    var customRows by remember { mutableIntStateOf(16) }
    var customPercent by remember { mutableStateOf(15.5f) }
    var customFog by remember { mutableStateOf(false) }
    var customSafe by remember { mutableStateOf(true) }

    fun startNew(cfg: GameConfig) {
        config = cfg
        board = Board(cfg.rows, cfg.cols, cfg.mines, cfg.safeFirstTap)
        elapsed = 0
        version++
    }

    LaunchedEffect(board.status) {
        if (board.status == Status.PLAYING) {
            while (true) {
                delay(1000)
                if (board.status != Status.PLAYING) break
                elapsed++
                version++
                if (config.timeLimit > 0 && elapsed >= config.timeLimit) {
                    board.timeout()
                    version++
                    break
                }
            }
        }
    }

    val timerSeconds = if (config.timeLimit > 0) (config.timeLimit - elapsed).coerceAtLeast(0) else elapsed
    val counter = config.mines - board.flagCount

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.height(28.dp))
            TopBar(counter = counter, timerSeconds = timerSeconds, countdown = config.timeLimit > 0, status = board.status) {
                startNew(config)
            }
            Spacer(Modifier.height(10.dp))
            DifficultyTabs(
                selected = config.name,
                onPreset = { name ->
                    when (name) {
                        "Easy" -> startNew(easyConfig())
                        "Medium" -> startNew(mediumConfig())
                        "Hard" -> startNew(hardConfig())
                    }
                },
                onCustom = { showCustom = true }
            )
            Spacer(Modifier.height(12.dp))
            if (board.status == Status.WON || board.status == Status.LOST) {
                Banner(won = board.status == Status.WON)
                Spacer(Modifier.height(12.dp))
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                GameGrid(
                    board = board,
                    fog = config.fog,
                    version = version,
                    onReveal = { r, c ->
                        if (board.grid[r][c].revealed) board.chord(r, c) else board.reveal(r, c)
                        version++
                    },
                    onFlag = { r, c ->
                        board.toggleFlag(r, c)
                        version++
                    }
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Licenses",
                    color = Muted,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .pointerInput(Unit) { detectTapGestures { showLicenses = true } }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }

    if (showCustom) {
        CustomDialog(
            rows = customRows,
            percent = customPercent,
            fog = customFog,
            safe = customSafe,
            onRows = { customRows = it },
            onPercent = { customPercent = it },
            onFog = { customFog = it },
            onSafe = { customSafe = it },
            onCancel = { showCustom = false },
            onStart = {
                val cells = customRows * customRows
                val mines = (cells * customPercent / 100f).roundToInt().coerceIn(1, cells - 1)
                showCustom = false
                startNew(GameConfig("Custom", customRows, customRows, mines, customFog, customSafe, 0))
            }
        )
    }

    if (showLicenses) {
        LicensesDialog(onClose = { showLicenses = false })
    }
}

// ---------------------------------------------------------------------------
// Top bar
// ---------------------------------------------------------------------------
@Composable
private fun TopBar(counter: Int, timerSeconds: Int, countdown: Boolean, status: Status, onReset: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DigitalPanel(
            text = formatCounter(counter),
            color = DigitRed,
            background = PanelRed,
            slots = 3
        )
        FaceButton(status = status, onClick = onReset)
        DigitalPanel(
            text = formatClock(timerSeconds),
            color = if (countdown) DigitAmber else DigitRed,
            background = PanelRed,
            slots = 5
        )
    }
}

@Composable
private fun DigitalPanel(text: String, color: Color, background: Color, slots: Int) {
    val digitWidth = 24.dp
    val digitHeight = 40.dp
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .width(digitWidth * slots + 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        text.forEach { ch ->
            when (ch) {
                ':' -> Colon(color, digitWidth * 0.45f, digitHeight)
                else -> SevenSeg(ch, color, digitWidth, digitHeight)
            }
        }
    }
}

@Composable
private fun SevenSeg(ch: Char, color: Color, width: Dp, height: Dp) {
    Canvas(modifier = Modifier.size(width, height)) {
        val w = size.width
        val h = size.height
        val t = w * 0.16f
        val p = t / 2f
        val mid = h / 2f
        val segs: Set<Char> = when (ch) {
            '0' -> "abcdef".toSet()
            '1' -> "bc".toSet()
            '2' -> "abdeg".toSet()
            '3' -> "abcdg".toSet()
            '4' -> "bcfg".toSet()
            '5' -> "acdfg".toSet()
            '6' -> "acdefg".toSet()
            '7' -> "abc".toSet()
            '8' -> "abcdefg".toSet()
            '9' -> "abcdfg".toSet()
            '-' -> "g".toSet()
            else -> emptySet()
        }
        fun seg(on: Boolean, x1: Float, y1: Float, x2: Float, y2: Float) {
            if (!on) return
            drawLine(color, Offset(x1, y1), Offset(x2, y2), strokeWidth = t, cap = StrokeCap.Round)
        }
        seg('a' in segs, p, p, w - p, p)
        seg('f' in segs, p, p, p, mid)
        seg('b' in segs, w - p, p, w - p, mid)
        seg('g' in segs, p, mid, w - p, mid)
        seg('e' in segs, p, mid, p, h - p)
        seg('c' in segs, w - p, mid, w - p, h - p)
        seg('d' in segs, p, h - p, w - p, h - p)
    }
}

@Composable
private fun Colon(color: Color, width: Dp, height: Dp) {
    Canvas(modifier = Modifier.size(width, height)) {
        val r = size.width * 0.28f
        drawCircle(color, radius = r, center = Offset(size.width / 2f, size.height * 0.34f))
        drawCircle(color, radius = r, center = Offset(size.width / 2f, size.height * 0.66f))
    }
}

private fun formatCounter(value: Int): String =
    if (value < 0) "-" + (-value).toString().padStart(2, '0') else value.toString().padStart(3, '0')

private fun formatClock(seconds: Int): String {
    val s = seconds.coerceIn(0, 99 * 60 + 59)
    return "%02d:%02d".format(s / 60, s % 60)
}

// ---------------------------------------------------------------------------
// Face button
// ---------------------------------------------------------------------------
@Composable
private fun FaceButton(status: Status, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Panel)
            .pointerInput(Unit) { detectTapGestures { onClick() } },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(44.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = size.minDimension / 2f
            drawCircle(color = Color(0xFFFFD54F), radius = r, center = Offset(cx, cy))
            drawCircle(color = Color(0xFFE0A800), radius = r, center = Offset(cx, cy), style = Stroke(width = r * 0.08f))
            val eyeR = r * 0.11f
            val eyeY = cy - r * 0.18f
            val eyeDx = r * 0.36f
            when (status) {
                Status.LOST -> {
                    val d = r * 0.14f
                    for (ex in listOf(cx - eyeDx, cx + eyeDx)) {
                        drawLine(Color(0xFF1A1A1A), Offset(ex - d, eyeY - d), Offset(ex + d, eyeY + d), strokeWidth = r * 0.09f, cap = StrokeCap.Round)
                        drawLine(Color(0xFF1A1A1A), Offset(ex - d, eyeY + d), Offset(ex + d, eyeY - d), strokeWidth = r * 0.09f, cap = StrokeCap.Round)
                    }
                    drawCircle(Color(0xFF1A1A1A), radius = r * 0.16f, center = Offset(cx, cy + r * 0.34f), style = Stroke(width = r * 0.09f))
                }
                Status.WON -> {
                    val lensW = r * 0.5f
                    val lensH = r * 0.32f
                    drawRoundRect(
                        color = Color(0xFF1A1A1A),
                        topLeft = Offset(cx - eyeDx - lensW / 2f, eyeY - lensH / 2f),
                        size = androidx.compose.ui.geometry.Size(lensW, lensH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(lensH / 2f)
                    )
                    drawRoundRect(
                        color = Color(0xFF1A1A1A),
                        topLeft = Offset(cx + eyeDx - lensW / 2f, eyeY - lensH / 2f),
                        size = androidx.compose.ui.geometry.Size(lensW, lensH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(lensH / 2f)
                    )
                    drawLine(Color(0xFF1A1A1A), Offset(cx - eyeDx + lensW / 2f, eyeY), Offset(cx + eyeDx - lensW / 2f, eyeY), strokeWidth = r * 0.07f)
                    drawArc(
                        color = Color(0xFF1A1A1A),
                        startAngle = 20f,
                        sweepAngle = 140f,
                        useCenter = false,
                        topLeft = Offset(cx - r * 0.42f, cy + r * 0.02f),
                        size = androidx.compose.ui.geometry.Size(r * 0.84f, r * 0.6f),
                        style = Stroke(width = r * 0.09f, cap = StrokeCap.Round)
                    )
                }
                else -> {
                    drawCircle(Color(0xFF1A1A1A), radius = eyeR, center = Offset(cx - eyeDx, eyeY))
                    drawCircle(Color(0xFF1A1A1A), radius = eyeR, center = Offset(cx + eyeDx, eyeY))
                    drawArc(
                        color = Color(0xFF1A1A1A),
                        startAngle = 20f,
                        sweepAngle = 140f,
                        useCenter = false,
                        topLeft = Offset(cx - r * 0.42f, cy + r * 0.02f),
                        size = androidx.compose.ui.geometry.Size(r * 0.84f, r * 0.6f),
                        style = Stroke(width = r * 0.09f, cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Difficulty tabs
// ---------------------------------------------------------------------------
@Composable
private fun DifficultyTabs(selected: String, onPreset: (String) -> Unit, onCustom: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TabCard(
            title = "Easy",
            selected = selected == "Easy",
            modifier = Modifier.weight(1f),
            onClick = { onPreset("Easy") },
            subtitle = { TabSubtitle(listOf(MiniIconType.MINE), "10") }
        )
        TabCard(
            title = "Medium",
            selected = selected == "Medium",
            modifier = Modifier.weight(1f),
            onClick = { onPreset("Medium") },
            subtitle = { TabSubtitle(listOf(MiniIconType.CLOCK), null) }
        )
        TabCard(
            title = "Hard",
            selected = selected == "Hard",
            modifier = Modifier.weight(1f),
            onClick = { onPreset("Hard") },
            subtitle = { TabSubtitle(listOf(MiniIconType.CLOCK, MiniIconType.EYE, MiniIconType.BOLT), null) }
        )
        TabCard(
            title = "Custom",
            selected = selected == "Custom",
            modifier = Modifier.weight(1f),
            onClick = onCustom,
            subtitle = { TabSubtitle(listOf(MiniIconType.GEAR), null) }
        )
    }
}

@Composable
private fun TabCard(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    subtitle: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) Teal else Panel)
            .pointerInput(Unit) { detectTapGestures { onClick() } },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = if (selected) TealDark else Color(0xFFC7D2DC),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        subtitle()
    }
}

@Composable
private fun TabSubtitle(icons: List<MiniIconType>, leadingText: String?) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (leadingText != null) {
            Text(leadingText, color = Muted, fontSize = 12.sp)
        }
        icons.forEach { icon ->
            MiniIcon(icon, size = 13.dp, color = Muted)
        }
    }
}

private enum class MiniIconType { MINE, CLOCK, EYE, BOLT, GEAR }

@Composable
private fun MiniIcon(type: MiniIconType, size: Dp, color: Color) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = min(w, h) / 2f
        val stroke = r * 0.22f
        when (type) {
            MiniIconType.MINE -> {
                drawCircle(color, radius = r * 0.62f, center = Offset(cx, cy))
                for (i in 0 until 8) {
                    val a = Math.PI * i / 4.0
                    drawLine(
                        color,
                        Offset(cx + (r * 0.62f * kotlin.math.cos(a)).toFloat(), cy + (r * 0.62f * kotlin.math.sin(a)).toFloat()),
                        Offset(cx + (r * kotlin.math.cos(a)).toFloat(), cy + (r * kotlin.math.sin(a)).toFloat()),
                        strokeWidth = stroke * 0.6f,
                        cap = StrokeCap.Round
                    )
                }
            }
            MiniIconType.CLOCK -> {
                drawCircle(color, radius = r * 0.8f, center = Offset(cx, cy), style = Stroke(width = stroke))
                drawLine(color, Offset(cx, cy), Offset(cx, cy - r * 0.45f), strokeWidth = stroke, cap = StrokeCap.Round)
                drawLine(color, Offset(cx, cy), Offset(cx + r * 0.4f, cy), strokeWidth = stroke, cap = StrokeCap.Round)
            }
            MiniIconType.EYE -> {
                drawOval(color, topLeft = Offset(0f, cy - r * 0.5f), size = androidx.compose.ui.geometry.Size(w, r), style = Stroke(width = stroke))
                drawCircle(color, radius = r * 0.26f, center = Offset(cx, cy))
            }
            MiniIconType.BOLT -> {
                val path = Path().apply {
                    moveTo(cx + r * 0.35f, 0f)
                    lineTo(cx - r * 0.45f, cy + r * 0.1f)
                    lineTo(cx + r * 0.05f, cy + r * 0.1f)
                    lineTo(cx - r * 0.3f, h)
                    lineTo(cx + r * 0.55f, cy - r * 0.15f)
                    lineTo(cx + r * 0.02f, cy - r * 0.15f)
                    close()
                }
                drawPath(path, color)
            }
            MiniIconType.GEAR -> {
                drawCircle(color, radius = r * 0.55f, center = Offset(cx, cy), style = Stroke(width = stroke))
                for (i in 0 until 8) {
                    val a = Math.PI * i / 4.0
                    drawLine(
                        color,
                        Offset(cx + (r * 0.7f * kotlin.math.cos(a)).toFloat(), cy + (r * 0.7f * kotlin.math.sin(a)).toFloat()),
                        Offset(cx + (r * kotlin.math.cos(a)).toFloat(), cy + (r * kotlin.math.sin(a)).toFloat()),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Status banner
// ---------------------------------------------------------------------------
@Composable
private fun Banner(won: Boolean) {
    val bg = if (won) Color(0xFF0E5A2A) else Color(0xFF7A1414)
    val fg = if (won) Color(0xFF8EF0A6) else Color(0xFFFF8A8A)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Burst(color = fg, size = 20.dp)
        Spacer(Modifier.width(12.dp))
        Text(
            text = if (won) "You Win!" else "Game Over",
            color = fg,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(12.dp))
        Burst(color = fg, size = 20.dp)
    }
}

@Composable
private fun Burst(color: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val rOuter = min(this.size.width, this.size.height) / 2f
        val rInner = rOuter * 0.42f
        val path = Path()
        for (i in 0 until 12) {
            val a = Math.PI * i / 6.0
            val x = cx + (rOuter * kotlin.math.cos(a)).toFloat()
            val y = cy + (rOuter * kotlin.math.sin(a)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            val a2 = Math.PI * (i + 0.5) / 6.0
            path.lineTo(
                cx + (rInner * kotlin.math.cos(a2)).toFloat(),
                cy + (rInner * kotlin.math.sin(a2)).toFloat()
            )
        }
        path.close()
        drawPath(path, color)
    }
}

// ---------------------------------------------------------------------------
// Game grid
// ---------------------------------------------------------------------------
@Composable
private fun GameGrid(
    board: Board,
    fog: Boolean,
    version: Int,
    onReveal: (Int, Int) -> Unit,
    onFlag: (Int, Int) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp)) {
        val cell = min(
            maxWidth.value / board.cols,
            maxHeight.value / board.rows
        ).dp
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            for (r in 0 until board.rows) {
                Row {
                    for (c in 0 until board.cols) {
                        val cellState = board.grid[r][c]
                        GameCell(
                            cell = cellState,
                            fog = fog,
                            size = cell,
                            version = version,
                            exploded = board.explodedRow == r && board.explodedCol == c,
                            onTap = { onReveal(r, c) },
                            onLong = { onFlag(r, c) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GameCell(
    cell: Cell,
    fog: Boolean,
    size: Dp,
    version: Int,
    exploded: Boolean,
    onTap: () -> Unit,
    onLong: () -> Unit
) {
    val shape = RoundedCornerShape(4.dp)
    val revealed = cell.revealed
    val flagged = cell.flagged
    val adjacent = cell.adjacent
    val mine = cell.mine
    val bg = when {
        revealed && mine && exploded -> BoomCell
        revealed && mine -> MineCell
        revealed && fog -> FogRevealed
        revealed -> Revealed
        else -> Covered
    }
    Box(
        modifier = Modifier
            .size(size)
            .padding(1.dp)
            .clip(shape)
            .background(bg)
            .then(
                if (!revealed) Modifier.border(1.dp, CoveredBorder, shape) else Modifier
            )
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onTap() }, onLongPress = { onLong() })
            },
        contentAlignment = Alignment.Center
    ) {
        when {
            revealed && mine -> MineSprite(size, exploded)
            revealed && !fog && adjacent > 0 -> Text(
                text = adjacent.toString(),
                color = numberColor(adjacent),
                fontSize = (size.value * 0.52f).sp,
                fontWeight = FontWeight.Bold
            )
            flagged -> FlagSprite(size)
        }
    }
}

@Composable
private fun MineSprite(size: Dp, exploded: Boolean) {
    Canvas(modifier = Modifier.size(size * 0.7f)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val r = min(this.size.width, this.size.height) * 0.3f
        if (exploded) {
            val burst = Path()
            val rOuter = min(this.size.width, this.size.height) / 2f
            val rInner = rOuter * 0.4f
            for (i in 0 until 12) {
                val a = Math.PI * i / 6.0
                val x = cx + (rOuter * kotlin.math.cos(a)).toFloat()
                val y = cy + (rOuter * kotlin.math.sin(a)).toFloat()
                if (i == 0) burst.moveTo(x, y) else burst.lineTo(x, y)
                val a2 = Math.PI * (i + 0.5) / 6.0
                burst.lineTo(
                    cx + (rInner * kotlin.math.cos(a2)).toFloat(),
                    cy + (rInner * kotlin.math.sin(a2)).toFloat()
                )
            }
            burst.close()
            drawPath(burst, Color(0xFFFFD64A))
        } else {
            for (i in 0 until 8) {
                val a = Math.PI * i / 4.0
                drawLine(
                    Color(0xFF6B7280),
                    Offset(cx + (r * 1.05f * kotlin.math.cos(a)).toFloat(), cy + (r * 1.05f * kotlin.math.sin(a)).toFloat()),
                    Offset(cx + (r * 1.55f * kotlin.math.cos(a)).toFloat(), cy + (r * 1.55f * kotlin.math.sin(a)).toFloat()),
                    strokeWidth = r * 0.28f,
                    cap = StrokeCap.Round
                )
            }
            drawCircle(Color(0xFF374151), radius = r * 1.25f, center = Offset(cx, cy))
            drawCircle(Color(0xFF9CA3AF), radius = r * 0.4f, center = Offset(cx - r * 0.4f, cy - r * 0.4f))
        }
    }
}

@Composable
private fun FlagSprite(size: Dp) {
    Canvas(modifier = Modifier.size(size * 0.7f)) {
        val w = this.size.width
        val h = this.size.height
        drawLine(
            Color(0xFFB9C2CC),
            Offset(w * 0.3f, h * 0.2f),
            Offset(w * 0.3f, h * 0.85f),
            strokeWidth = w * 0.09f,
            cap = StrokeCap.Round
        )
        val path = Path().apply {
            moveTo(w * 0.34f, h * 0.2f)
            lineTo(w * 0.82f, h * 0.36f)
            lineTo(w * 0.34f, h * 0.52f)
            close()
        }
        drawPath(path, FlagColor)
    }
}

// ---------------------------------------------------------------------------
// Dialogs
// ---------------------------------------------------------------------------
@Composable
private fun CustomDialog(
    rows: Int,
    percent: Float,
    fog: Boolean,
    safe: Boolean,
    onRows: (Int) -> Unit,
    onPercent: (Float) -> Unit,
    onFog: (Boolean) -> Unit,
    onSafe: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onStart: () -> Unit
) {
    val cells = rows * rows
    val mines = (cells * percent / 100f).roundToInt().coerceIn(1, cells - 1)
    val shownPercent = mines * 100 / cells
    Dialog(onDismissRequest = onCancel) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xFF1B2833))
                .padding(24.dp)
        ) {
            Text("Custom Game", color = Color(0xFFE6EDF3), fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(20.dp))
            SliderRow(label = "Grid size", valueText = "$rows × $rows") {
                Slider(
                    value = rows.toFloat(),
                    onValueChange = { onRows(it.roundToInt().coerceIn(5, 20)) },
                    valueRange = 5f..20f,
                    steps = 14
                )
            }
            Spacer(Modifier.height(8.dp))
            SliderRow(label = "Mines", valueText = "$mines ($shownPercent%)") {
                Slider(
                    value = percent,
                    onValueChange = { onPercent(it.coerceIn(1f, 93f)) },
                    valueRange = 1f..93f
                )
            }
            Spacer(Modifier.height(16.dp))
            ToggleRow("Fog of war", fog, onFog)
            Spacer(Modifier.height(8.dp))
            ToggleRow("Safe first tap", safe, onSafe)
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel) {
                    Text("Cancel", color = Color(0xFFB9C2CC), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onStart) {
                    Text("Start", color = Teal, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SliderRow(label: String, valueText: String, slider: @Composable () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color(0xFFD6DEE6), fontSize = 16.sp)
            Text(valueText, color = Teal, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        slider()
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color(0xFFD6DEE6), fontSize = 16.sp)
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF06231F),
                checkedTrackColor = Teal,
                uncheckedThumbColor = Color(0xFF6B7683),
                uncheckedTrackColor = Color(0xFF2A3843)
            )
        )
    }
}

@Composable
private fun LicensesDialog(onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF232323))
                .padding(24.dp)
        ) {
            Text("Licenses", color = Color(0xFFF0F0F0), fontSize = 26.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "This game is free software released under the GNU General Public License v3.0.\n\n" +
                    "Third-party libraries are used under the Apache License 2.0.\n\n" +
                    "Apache License\nVersion 2.0, January 2004\nhttp://www.apache.org/licenses/\n\n" +
                    "TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION",
                color = Color(0xFFCFCFCF),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onClose) {
                    Text("Close", color = Color(0xFF8AB4F8), fontSize = 16.sp)
                }
            }
        }
    }
}