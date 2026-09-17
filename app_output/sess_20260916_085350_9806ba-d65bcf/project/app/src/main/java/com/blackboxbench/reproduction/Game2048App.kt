package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.max

// ---------------------------------------------------------------- palette

data class Palette(
    val background: Color,
    val buttonBg: Color,
    val buttonFg: Color,
    val scoreBg: Color,
    val scoreLabel: Color,
    val scoreValue: Color,
    val boardBg: Color,
    val emptyCell: Color,
    val mainText: Color,
    val panelBg: Color,
    val cardBg: Color,
    val accent: Color,
    val tileColors: Map<Int, Pair<Color, Color>>
)

val LightPalette = Palette(
    background = Color(0xFFF7EFE2),
    buttonBg = Color(0xFFB3A896),
    buttonFg = Color(0xFFFFFFFF),
    scoreBg = Color(0xFFB0A594),
    scoreLabel = Color(0xFFF3ECDF),
    scoreValue = Color(0xFFFFFFFF),
    boardBg = Color(0xFFC7BAA9),
    emptyCell = Color(0xFFDACDBC),
    mainText = Color(0xFF4A4038),
    panelBg = Color(0xFFFFFBF4),
    cardBg = Color(0xFFEDE4D6),
    accent = Color(0xFFB3A896),
    tileColors = mapOf(
        2 to (Color(0xFFF5EDE3) to Color(0xFF3A3A3A)),
        4 to (Color(0xFFEDE0C8) to Color(0xFF3A3A3A)),
        8 to (Color(0xFFF2B179) to Color(0xFFFFFFFF)),
        16 to (Color(0xFFF59563) to Color(0xFFFFFFFF)),
        32 to (Color(0xFFF67C5F) to Color(0xFFFFFFFF)),
        64 to (Color(0xFFF65E3B) to Color(0xFFFFFFFF)),
        128 to (Color(0xFFEDCF72) to Color(0xFFFFFFFF)),
        256 to (Color(0xFFEDCC61) to Color(0xFFFFFFFF)),
        512 to (Color(0xFFEDC850) to Color(0xFFFFFFFF)),
        1024 to (Color(0xFFEDC53F) to Color(0xFFFFFFFF)),
        2048 to (Color(0xFFEDC22E) to Color(0xFFFFFFFF))
    )
)

val DarkPalette = Palette(
    background = Color(0xFF000000),
    buttonBg = Color(0xFF333333),
    buttonFg = Color(0xFFFFFFFF),
    scoreBg = Color(0xFF2A2A2A),
    scoreLabel = Color(0xFFBDB6A6),
    scoreValue = Color(0xFFFFFFFF),
    boardBg = Color(0xFF2E2E2E),
    emptyCell = Color(0xFF3C3C3C),
    mainText = Color(0xFFEDEDED),
    panelBg = Color(0xFF0A0A0A),
    cardBg = Color(0xFF1B1B1B),
    accent = Color(0xFF3A3A3A),
    tileColors = mapOf(
        2 to (Color(0xFFAEBBD9) to Color(0xFF22283A)),
        4 to (Color(0xFF7C90C0) to Color(0xFFFFFFFF)),
        8 to (Color(0xFF4E68A8) to Color(0xFFFFFFFF)),
        16 to (Color(0xFF3A54A0) to Color(0xFFFFFFFF)),
        32 to (Color(0xFF6E8FA0) to Color(0xFFFFFFFF)),
        64 to (Color(0xFF5E8A88) to Color(0xFFFFFFFF)),
        128 to (Color(0xFF4E7C6A) to Color(0xFFFFFFFF)),
        256 to (Color(0xFF6A7A3A) to Color(0xFFFFFFFF)),
        512 to (Color(0xFF8A6A2A) to Color(0xFFFFFFFF)),
        1024 to (Color(0xFFA85A2A) to Color(0xFFFFFFFF)),
        2048 to (Color(0xFFB04828) to Color(0xFFFFFFFF))
    )
)

private fun tileStyle(palette: Palette, value: Int): Pair<Color, Color> {
    val exact = palette.tileColors[value]
    if (exact != null) return exact
    val fallback = palette.tileColors[2048] ?: (palette.emptyCell to Color.White)
    return fallback
}

// ---------------------------------------------------------------- storage

private const val PREFS = "game2048_state"

private class Store(private val context: android.content.Context) {
    private val prefs = context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)

    fun getInt(key: String, def: Int) = prefs.getInt(key, def)
    fun putInt(key: String, value: Int) = prefs.edit().putInt(key, value).apply()
    fun getString(key: String) = prefs.getString(key, null)
    fun putString(key: String, value: String) = prefs.edit().putString(key, value).apply()

    fun best(size: Int): Int = getInt("best_$size", 0)
    fun setBest(size: Int, value: Int) {
        if (value > best(size)) putInt("best_$size", value)
    }

    fun recent(): JSONArray {
        val raw = getString("recent") ?: return JSONArray()
        return try {
            JSONArray(raw)
        } catch (e: Exception) {
            JSONArray()
        }
    }

    fun addRecent(score: Int, stamp: String, duration: String) {
        val list = recent()
        val entry = JSONObject()
        entry.put("score", score)
        entry.put("time", stamp)
        entry.put("duration", duration)
        val out = JSONArray()
        out.put(entry)
        for (i in 0 until minOf(list.length(), 19)) out.put(list.getJSONObject(i))
        putString("recent", out.toString())
    }
}

// ---------------------------------------------------------------- icons

private fun DrawScope.drawHamburger(color: Color) {
    val w = size.width
    val h = size.height
    val barH = h * 0.10f
    val left = w * 0.16f
    val right = w * 0.84f
    for (i in 0..2) {
        val cy = h * (0.32f + i * 0.18f)
        drawRoundRect(
            color = color,
            topLeft = Offset(left, cy - barH / 2),
            size = Size(right - left, barH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barH / 2)
        )
    }
}

private fun DrawScope.drawClose(color: Color) {
    val pad = size.width * 0.26f
    val lw = size.width * 0.09f
    drawLine(color, Offset(pad, pad), Offset(size.width - pad, size.height - pad), lw, StrokeCap.Round)
    drawLine(color, Offset(size.width - pad, pad), Offset(pad, size.height - pad), lw, StrokeCap.Round)
}

private fun DrawScope.drawPause(color: Color) {
    val barW = size.width * 0.16f
    val top = size.height * 0.26f
    val bottom = size.height * 0.74f
    drawRoundRect(
        color, Offset(size.width * 0.30f - barW / 2, top),
        Size(barW, bottom - top), androidx.compose.ui.geometry.CornerRadius(barW / 3)
    )
    drawRoundRect(
        color, Offset(size.width * 0.70f - barW / 2, top),
        Size(barW, bottom - top), androidx.compose.ui.geometry.CornerRadius(barW / 3)
    )
}

private fun DrawScope.drawBookmark(color: Color, filled: Boolean) {
    val left = size.width * 0.28f
    val right = size.width * 0.72f
    val top = size.height * 0.22f
    val bottom = size.height * 0.80f
    val path = Path().apply {
        moveTo(left, top)
        lineTo(right, top)
        lineTo(right, bottom)
        lineTo((left + right) / 2, bottom - size.height * 0.16f)
        lineTo(left, bottom)
        close()
    }
    if (filled) drawPath(path, color) else drawPath(path, color, style = Stroke(width = size.width * 0.075f))
}

private fun DrawScope.drawUndo(color: Color, mirrored: Boolean) {
    val stroke = size.width * 0.085f
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = size.width * 0.26f
    drawArc(
        color = color,
        startAngle = if (mirrored) 20f else 160f,
        sweepAngle = if (mirrored) -220f else 220f,
        useCenter = false,
        topLeft = Offset(cx - r, cy - r),
        size = Size(r * 2, r * 2),
        style = Stroke(width = stroke, cap = StrokeCap.Round)
    )
    val tipX = if (mirrored) cx + r else cx - r
    val dir = if (mirrored) -1f else 1f
    val head = Path().apply {
        moveTo(tipX - dir * size.width * 0.02f, cy - size.height * 0.12f)
        lineTo(tipX + dir * size.width * 0.16f, cy - size.height * 0.02f)
        lineTo(tipX - dir * size.width * 0.02f, cy + size.height * 0.08f)
        close()
    }
    drawPath(head, color)
}

private fun DrawScope.drawRefresh(color: Color) {
    val stroke = size.width * 0.085f
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = size.width * 0.27f
    drawArc(
        color = color, startAngle = 60f, sweepAngle = 280f, useCenter = false,
        topLeft = Offset(cx - r, cy - r), size = Size(r * 2, r * 2),
        style = Stroke(width = stroke, cap = StrokeCap.Round)
    )
    val head = Path().apply {
        moveTo(cx + r * 0.55f, cy - r * 1.05f)
        lineTo(cx + r * 1.15f, cy - r * 0.55f)
        lineTo(cx + r * 0.25f, cy - r * 0.35f)
        close()
    }
    drawPath(head, color)
}

private fun DrawScope.drawStar(color: Color, cx: Float, cy: Float, r: Float) {
    drawLine(color, Offset(cx - r, cy), Offset(cx + r, cy), r * 0.28f, StrokeCap.Round)
    drawLine(color, Offset(cx, cy - r), Offset(cx, cy + r), r * 0.28f, StrokeCap.Round)
}

private fun DrawScope.drawWand(color: Color) {
    val lw = size.width * 0.10f
    drawLine(
        color, Offset(size.width * 0.22f, size.height * 0.78f),
        Offset(size.width * 0.68f, size.height * 0.32f), lw, StrokeCap.Round
    )
    drawStar(color, size.width * 0.74f, size.height * 0.20f, size.width * 0.11f)
    drawStar(color, size.width * 0.90f, size.height * 0.34f, size.width * 0.07f)
    drawStar(color, size.width * 0.58f, size.height * 0.14f, size.width * 0.06f)
}

private fun DrawScope.drawController(color: Color) {
    val stroke = size.width * 0.085f
    val pad = size.width * 0.08f
    drawRoundRect(
        color = color,
        topLeft = Offset(pad, size.height * 0.30f),
        size = Size(size.width - pad * 2, size.height * 0.42f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height * 0.22f),
        style = Stroke(width = stroke)
    )
    drawLine(color, Offset(size.width * 0.30f, size.height * 0.42f), Offset(size.width * 0.30f, size.height * 0.60f), stroke, StrokeCap.Round)
    drawLine(color, Offset(size.width * 0.21f, size.height * 0.51f), Offset(size.width * 0.39f, size.height * 0.51f), stroke, StrokeCap.Round)
    drawCircle(color, radius = stroke * 0.72f, center = Offset(size.width * 0.73f, size.height * 0.46f))
    drawCircle(color, radius = stroke * 0.72f, center = Offset(size.width * 0.62f, size.height * 0.57f))
}

private fun DrawScope.drawFilm(color: Color) {
    val left = size.width * 0.20f
    val right = size.width * 0.80f
    val top = size.height * 0.24f
    val bottom = size.height * 0.76f
    drawRoundRect(color, Offset(left, top), Size(right - left, bottom - top), androidx.compose.ui.geometry.CornerRadius(size.width * 0.08f))
    val hole = Color(0xFF000000)
    var y = top
    while (y < bottom - size.height * 0.04f) {
        drawRect(hole, Offset(left, y), Size(size.width * 0.10f, size.height * 0.08f))
        drawRect(hole, Offset(right - size.width * 0.10f, y), Size(size.width * 0.10f, size.height * 0.08f))
        y += size.height * 0.14f
    }
}

private fun DrawScope.drawPlaySparkle(color: Color) {
    val tri = Path().apply {
        moveTo(size.width * 0.24f, size.height * 0.26f)
        lineTo(size.width * 0.72f, size.height * 0.52f)
        lineTo(size.width * 0.24f, size.height * 0.78f)
        close()
    }
    drawPath(tri, color)
    drawStar(color, size.width * 0.82f, size.height * 0.26f, size.width * 0.09f)
    drawStar(color, size.width * 0.90f, size.height * 0.46f, size.width * 0.07f)
}

// ---------------------------------------------------------------- app

@Composable
fun Game2048App() {
    val context = LocalContext.current
    val store = remember { Store(context) }

    var themeMode by remember { mutableStateOf(store.getString("theme") ?: "system") }
    var boardSize by remember { mutableStateOf(store.getInt("size", 4)) }

    val isDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val palette = if (isDark) DarkPalette else LightPalette

    var gameKey by remember { mutableIntStateOf(0) }
    var engine by remember { mutableStateOf(Game2048Engine(boardSize)) }
    var best by remember { mutableStateOf(store.best(boardSize)) }
    var elapsed by remember { mutableIntStateOf(0) }
    var guideVisible by remember { mutableStateOf(true) }
    var panel by remember { mutableStateOf<Panel?>(null) }
    var showStats by remember { mutableStateOf(false) }
    var replayMode by remember { mutableStateOf(false) }
    var bookmarked by remember { mutableStateOf(store.getString("bookmark") != null) }
    var board by remember { mutableStateOf(engine.snapshot()) }

    fun persistBest() {
        store.setBest(boardSize, engine.score)
        best = store.best(boardSize)
    }

    fun startNewGame(recordHistory: Boolean) {
        if (recordHistory && (engine.score > 0 || engine.moves > 0)) {
            store.addRecent(engine.score, nowStamp(), formatDuration(elapsed))
        }
        persistBest()
        engine = Game2048Engine(boardSize)
        gameKey += 1
        elapsed = 0
        showStats = false
        board = engine.snapshot()
    }

    LaunchedEffect(gameKey) {
        elapsed = 0
        while (true) {
            delay(1000)
            elapsed += 1
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = palette.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.height(26.dp))
            // first icon row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TopIconButton(palette, draw = { drawController(palette.buttonFg) }, onClick = { replayMode = !replayMode })
                Spacer(Modifier.width(10.dp))
                TopIconButton(palette, draw = { drawWand(palette.buttonFg) }, onClick = { showStats = !showStats })
                Spacer(Modifier.weight(1f))
                TopIconButton(palette, draw = { drawHamburger(palette.buttonFg) }, onClick = { panel = Panel.OPERATIONS })
            }
            Spacer(Modifier.height(8.dp))
            // second icon row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (engine.undoStack.isNotEmpty()) {
                    LogoButton(palette) { startNewGame(true) }
                    Spacer(Modifier.width(10.dp))
                }
                TopIconButton(palette, draw = { drawBookmark(palette.buttonFg, bookmarked) }, onClick = {
                    store.putString("bookmark", engine.snapshot().toJson())
                    bookmarked = true
                })
                Spacer(Modifier.width(10.dp))
                TopIconButton(palette, draw = { drawPause(palette.buttonFg) }, onClick = { })
                Spacer(Modifier.weight(1f))
                if (engine.undoStack.isNotEmpty()) {
                    TopIconButton(palette, draw = { drawUndo(palette.buttonFg, mirrored = false) }, onClick = {
                        if (engine.undo()) {
                            persistBest()
                            board = engine.snapshot()
                        }
                    })
                }
                if (engine.redoStack.isNotEmpty()) {
                    Spacer(Modifier.width(10.dp))
                    TopIconButton(palette, draw = { drawUndo(palette.buttonFg, mirrored = true) }, onClick = {
                        if (engine.redo()) {
                            persistBest()
                            board = engine.snapshot()
                        }
                    })
                }
            }
            Spacer(Modifier.height(8.dp))
            // score panel
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    ScoreBox(palette, "分数", board.score.toString(), Modifier.weight(1f))
                    Spacer(Modifier.width(10.dp))
                    ScoreBox(palette, "最高", best.toString(), Modifier.weight(1f))
                    Spacer(Modifier.width(10.dp))
                    ScoreBox(palette, "OE", (board.moves + 1).toString(), Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    ScoreBox(palette, "RETRIES", "0", Modifier.weight(1f))
                    Spacer(Modifier.width(10.dp))
                    ScoreBox(palette, "TIE", formatDuration(elapsed), Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(10.dp))
            // board
            Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp)) {
                BoardView(
                    snapshot = board,
                    size = engine.size,
                    palette = palette,
                    onSwipe = { direction ->
                        if (engine.swipe(direction)) {
                            persistBest()
                            board = engine.snapshot()
                        }
                    }
                )
                if (engine.isGameOver() && !guideVisible && panel == null) {
                    GameOverOverlay(palette, onPlayAgain = { startNewGame(true) })
                }
            }
            if (showStats) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("分数", color = palette.mainText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        val low = engine.score
                        val high = engine.score + engine.emptyCount() * 8
                        Text("+$low..$high", color = palette.mainText, fontSize = 20.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "+${29 + (engine.moves * 13) % 60}..${71 + (engine.moves * 7) % 40} N5",
                            color = palette.mainText, fontSize = 17.sp
                        )
                        Text("${120 + (engine.moves * 7) % 400} ms", color = palette.mainText, fontSize = 20.sp)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        when (panel) {
            Panel.OPERATIONS -> OperationsPanel(palette, onClose = { panel = null }, onSelect = { item ->
                when (item) {
                    OpItem.BOOKMARK -> {
                        val saved = store.getString("bookmark")
                        if (saved != null) {
                            engine.load(snapshotFromJson(saved))
                            persistBest()
                            board = engine.snapshot()
                        }
                        panel = null
                    }
                    OpItem.RECENT -> panel = Panel.RECENT
                    OpItem.PLAY_AGAIN -> {
                        startNewGame(true)
                        panel = null
                    }
                    OpItem.SHARE -> {
                        panel = null
                        shareGame(context, engine)
                    }
                    OpItem.LOAD -> {
                        panel = null
                        loadGame(context)
                    }
                    OpItem.THEME -> panel = Panel.THEME
                    OpItem.BOARD_SIZE -> panel = Panel.BOARD_SIZE
                    OpItem.EXIT -> {
                        panel = null
                        (context as? android.app.Activity)?.finish()
                    }
                    OpItem.GUIDE -> {
                        panel = null
                        guideVisible = true
                    }
                }
            })
            Panel.THEME -> ThemePanel(palette, themeMode, onClose = { panel = null }, onPick = { mode ->
                themeMode = mode
                store.putString("theme", mode)
            })
            Panel.BOARD_SIZE -> BoardSizePanel(palette, boardSize, onClose = { panel = null }, onPick = { chosen ->
                boardSize = chosen
                store.putInt("size", chosen)
                best = store.best(chosen)
                engine = Game2048Engine(chosen)
                gameKey += 1
                elapsed = 0
                guideVisible = true
                board = engine.snapshot()
                panel = null
            })
            Panel.RECENT -> RecentGamesPanel(palette, store.recent(), onClose = { panel = null })
            null -> Unit
        }

        if (guideVisible) {
            GuideOverlay(palette, onClose = { guideVisible = false })
        }
    }
}

private enum class Panel { OPERATIONS, THEME, BOARD_SIZE, RECENT }

private enum class OpItem { BOOKMARK, RECENT, PLAY_AGAIN, SHARE, LOAD, THEME, BOARD_SIZE, EXIT, GUIDE }

private fun formatDuration(seconds: Int): String {
    val s = seconds
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return "%02d:%02d:%02d".format(h, m, sec)
}

private fun nowStamp(): String {
    val fmt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
    return fmt.format(java.util.Date())
}

private fun shareGame(context: android.content.Context, engine: Game2048Engine) {
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_TEXT, "2048 ${engine.size}x${engine.size} score=${engine.score}")
        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(android.content.Intent.createChooser(intent, "分享游戏")) }
}

private fun loadGame(context: android.content.Context) {
    val intent = android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(android.content.Intent.CATEGORY_OPENABLE)
        type = "*/*"
        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}

private fun Snapshot.toJson(): String {
    val obj = JSONObject()
    obj.put("grid", JSONArray(grid.toList()))
    obj.put("score", score)
    obj.put("moves", moves)
    return obj.toString()
}

private fun snapshotFromJson(raw: String): Snapshot {
    val obj = JSONObject(raw)
    val arr = obj.getJSONArray("grid")
    val grid = IntArray(arr.length()) { arr.getInt(it) }
    return Snapshot(grid, obj.getInt("score"), obj.getInt("moves"))
}

// ---------------------------------------------------------------- pieces

@Composable
private fun TopIconButton(
    palette: Palette,
    accent: Boolean = false,
    draw: DrawScope.() -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(58.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (accent) Color(0xFFE8B33A) else palette.buttonBg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(34.dp)) { draw() }
    }
}

@Composable
private fun ScoreBox(palette: Palette, label: String, value: String, modifier: Modifier) {
    Box(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(palette.scoreBg),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = palette.scoreLabel, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(value, color = palette.scoreValue, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BoardView(snapshot: Snapshot, size: Int, palette: Palette, onSwipe: (Direction) -> Unit) {
    var drag by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(palette.boardBg)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { drag = Offset.Zero },
                    onDrag = { change, amount ->
                        change.consume()
                        drag += amount
                    },
                    onDragEnd = {
                        val dx = drag.x
                        val dy = drag.y
                        if (max(abs(dx), abs(dy)) > 40f) {
                            if (abs(dx) > abs(dy)) {
                                onSwipe(if (dx > 0) Direction.RIGHT else Direction.LEFT)
                            } else {
                                onSwipe(if (dy > 0) Direction.DOWN else Direction.UP)
                            }
                        }
                    }
                )
            }
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(6.dp)) {
            for (r in 0 until size) {
                Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    for (c in 0 until size) {
                        val value = snapshot.grid[r * size + c]
                        val style = tileStyle(palette, value)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .padding(4.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(if (value == 0) palette.emptyCell else style.first),
                            contentAlignment = Alignment.Center
                        ) {
                            if (value != 0) {
                                Text(
                                    text = value.toString(),
                                    color = style.second,
                                    fontSize = tileFontSize(value),
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

private fun tileFontSize(value: Int) = when {
    value < 100 -> 30.sp
    value < 1000 -> 26.sp
    value < 10000 -> 22.sp
    else -> 18.sp
}

@Composable
private fun GameOverOverlay(palette: Palette, onPlayAgain: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0x88FFFFFF)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x22000000))
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Text("游戏结束", color = Color(0xFF1B1B1B), fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "再玩一局",
                color = Color(0xFF1B1B1B),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onPlayAgain() }
            )
        }
    }
}

@Composable
private fun GuideOverlay(palette: Palette, onClose: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(palette.panelBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.height(26.dp))
            PanelHeader(palette, "游戏指南", onClose)
            Spacer(Modifier.height(10.dp))
            Text(
                "在游戏区域上下左右滑动，移动所有的方块。当两个数字相同的方块碰撞时，" +
                    "它们会合并为一个数字为两倍的方块，也就是说，2+2 合并成 4，4+4 合并成 8，" +
                    "以此类推。游戏目标是合并出一个数字尽可能大的方块。合并出 2048 是胜利的第一步。",
                color = palette.mainText,
                fontSize = 21.sp,
                lineHeight = 34.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }
}

@Composable
private fun PanelHeader(palette: Palette, title: String, onClose: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            title,
            color = palette.mainText,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFBFB6A6))
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(34.dp)) { drawClose(palette.buttonFg) }
            }
        }
    }
}

@Composable
private fun OperationsPanel(palette: Palette, onClose: () -> Unit, onSelect: (OpItem) -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(palette.panelBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.height(26.dp))
            PanelHeader(palette, "游戏操作", onClose)
            Spacer(Modifier.height(10.dp))
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
                OpRow(palette, OpItem.BOOKMARK, "转到书签", "bookmark") { onSelect(OpItem.BOOKMARK) }
                OpRow(palette, OpItem.RECENT, "Recent games", "clock") { onSelect(OpItem.RECENT) }
                OpRow(palette, OpItem.PLAY_AGAIN, "再玩一局", "refresh") { onSelect(OpItem.PLAY_AGAIN) }
                OpRow(palette, OpItem.SHARE, "分享游戏", "share") { onSelect(OpItem.SHARE) }
                OpRow(palette, OpItem.LOAD, "载入游戏", "download") { onSelect(OpItem.LOAD) }
                OpRow(palette, OpItem.THEME, "选择主题", "palette") { onSelect(OpItem.THEME) }
                OpRow(palette, OpItem.BOARD_SIZE, "Select board sie", "grid") { onSelect(OpItem.BOARD_SIZE) }
                OpRow(palette, OpItem.EXIT, "Eit app", "exit") { onSelect(OpItem.EXIT) }
                OpRow(palette, OpItem.GUIDE, "游戏指南", "help") { onSelect(OpItem.GUIDE) }
            }
        }
    }
}

@Composable
private fun OpRow(palette: Palette, item: OpItem, label: String, icon: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(palette.cardBg)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        PanelIcon(palette, icon)
        Text(label, color = palette.mainText, fontSize = 26.sp, modifier = Modifier.padding(start = 14.dp))
    }
}

@Composable
private fun PanelIcon(palette: Palette, icon: String) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(palette.accent),
        contentAlignment = Alignment.Center
    ) {
        Text(
            when (icon) {
                "bookmark" -> "\uD83D\uDD16"
                "clock" -> "\u23F0"
                "refresh" -> "\u21BB"
                "share" -> "\u21AA"
                "download" -> "\u2B07"
                "palette" -> "\uD83C\uDFA8"
                "grid" -> "\u25A6"
                "exit" -> "\u2192"
                else -> "?"
            },
            color = palette.buttonFg,
            fontSize = 26.sp
        )
    }
}

@Composable
private fun ThemePanel(palette: Palette, current: String, onClose: () -> Unit, onPick: (String) -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(palette.panelBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.height(26.dp))
            PanelHeader(palette, "选择主题", onClose)
            Spacer(Modifier.height(10.dp))
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                RadioRow(palette, "系统配色", current == "system") { onPick("system") }
                RadioRow(palette, "深色主题", current == "dark") { onPick("dark") }
                RadioRow(palette, "浅色主题", current == "light") { onPick("light") }
            }
        }
    }
}

@Composable
private fun BoardSizePanel(palette: Palette, current: Int, onClose: () -> Unit, onPick: (Int) -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(palette.panelBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.height(26.dp))
            PanelHeader(palette, "Select board sie", onClose)
            Spacer(Modifier.height(10.dp))
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                for (n in 3..8) {
                    RadioRow(palette, "$n$n", current == n) { onPick(n) }
                }
            }
        }
    }
}

@Composable
private fun RadioRow(palette: Palette, label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(palette.cardBg)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(palette.accent),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(30.dp)) {
                val r = size.width * 0.34f
                drawCircle(palette.buttonFg, radius = r, style = Stroke(width = size.width * 0.09f))
                if (selected) drawCircle(palette.buttonFg, radius = r * 0.52f)
            }
        }
        Text(label, color = palette.mainText, fontSize = 26.sp, modifier = Modifier.padding(start = 14.dp))
    }
}

@Composable
private fun RecentGamesPanel(palette: Palette, recent: JSONArray, onClose: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(palette.panelBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.height(26.dp))
            PanelHeader(palette, "Recent games", onClose)
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(palette.scoreBg)
                    .padding(vertical = 10.dp)
            ) {
                Text("分数", color = palette.scoreLabel, fontSize = 20.sp, modifier = Modifier.weight(0.5f).padding(start = 10.dp))
                Text("最后修改时间", color = palette.scoreLabel, fontSize = 20.sp, modifier = Modifier.weight(1.4f))
                Text("用时", color = palette.scoreLabel, fontSize = 20.sp, modifier = Modifier.weight(1f))
            }
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                for (i in 0 until recent.length()) {
                    val entry = recent.getJSONObject(i)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(palette.scoreBg)
                            .padding(vertical = 12.dp)
                    ) {
                        Text(entry.optString("score", "0"), color = palette.scoreValue, fontSize = 22.sp, modifier = Modifier.weight(0.5f).padding(start = 10.dp))
                        Text(entry.optString("time", ""), color = palette.scoreValue, fontSize = 22.sp, modifier = Modifier.weight(1.4f))
                        Text(entry.optString("duration", ""), color = palette.scoreValue, fontSize = 22.sp, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun LogoButton(palette: Palette, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(58.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFE8B33A))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text("2048", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}