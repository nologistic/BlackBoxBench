@file:OptIn(ExperimentalFoundationApi::class)

package com.blackboxbench.reproduction

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Random

// ---------------------------------------------------------------------------
// Palette
// ---------------------------------------------------------------------------

data class Palette(
    val accent: Color,
    val background: Color,
    val text: Color,
    val subText: Color,
    val cellBg: Color,
    val cellText: Color,
    val filledBg: Color,
    val filledText: Color,
    val outline: Color,
    val artBase: Color,
    val tinted: Boolean
)

fun paletteFor(dark: Boolean, style: String): Palette {
    val tinted = style == "TINTED"
    return if (dark) {
        Palette(
            accent = Color(0xFFF0453C),
            background = Color(0xFF000000),
            text = Color(0xFFF2F0EC),
            subText = Color(0xFF6E6E6E),
            cellBg = Color(0xFF1C1C1E),
            cellText = Color(0xFFF2F0EC),
            filledBg = Color(0xFFF2F0EC),
            filledText = Color(0xFF000000),
            outline = Color(0xFFF2F0EC),
            artBase = if (tinted) Color(0xFFF0453C) else Color(0xFFEDEBE7),
            tinted = tinted
        )
    } else {
        Palette(
            accent = Color(0xFFF0453C),
            background = Color(0xFFF0EDE7),
            text = Color(0xFF141414),
            subText = Color(0xFFB4AFA7),
            cellBg = Color(0xFFE9E5DE),
            cellText = Color(0xFF141414),
            filledBg = Color(0xFF111111),
            filledText = Color(0xFFF0EDE7),
            outline = Color(0xFF141414),
            artBase = if (tinted) Color(0xFFF0453C) else Color(0xFF2A2A2A),
            tinted = tinted
        )
    }
}

// ---------------------------------------------------------------------------
// Persistent settings
// ---------------------------------------------------------------------------

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("futoshiki_prefs", Context.MODE_PRIVATE)
    var style: String
        get() = prefs.getString("style", "MONO") ?: "MONO"
        set(v) = prefs.edit().putString("style", v).apply()
    var mode: String
        get() = prefs.getString("mode", "AUTO") ?: "AUTO"
        set(v) = prefs.edit().putString("mode", v).apply()
    var themeName: String
        get() = prefs.getString("theme", "FIRE") ?: "FIRE"
        set(v) = prefs.edit().putString("theme", v).apply()
    var size: Int
        get() = prefs.getInt("size", 4)
        set(v) = prefs.edit().putInt("size", v).apply()
    var difficulty: String
        get() = prefs.getString("difficulty", "EASY") ?: "EASY"
        set(v) = prefs.edit().putString("difficulty", v).apply()
}

// ---------------------------------------------------------------------------
// Game session state
// ---------------------------------------------------------------------------

class GameState(val puzzle: Puzzle, val difficulty: Difficulty) {
    var grid by mutableStateOf(Array(puzzle.size) { puzzle.givens[it].clone() })
    var selected by mutableStateOf(-1)
    var paused by mutableStateOf(false)
    var solved by mutableStateOf(false)
    var solvedElapsed = 0L
    var pauseTotal = 0L
    private val startAt = System.currentTimeMillis()
    private var pausedAccum = 0L
    private var pauseStart = 0L

    fun elapsed(now: Long): Long {
        val base = if (paused) pauseStart else now
        return (base - startAt - pausedAccum).coerceAtLeast(0)
    }

    fun pause(now: Long) {
        if (!paused) {
            paused = true
            pauseStart = now
        }
    }

    fun resume(now: Long) {
        if (paused) {
            pausedAccum += now - pauseStart
            paused = false
        }
    }

    fun finish(now: Long) {
        solvedElapsed = elapsed(now)
        pauseTotal = pausedAccum
        solved = true
    }
}

fun formatTime(millis: Long): String {
    val total = (millis / 1000).toInt()
    return "%02d:%02d".format(total / 60, total % 60)
}

// ---------------------------------------------------------------------------
// Root
// ---------------------------------------------------------------------------

@Composable
fun FutoshikiApp() {
    val context = LocalContext.current
    val store = remember { SettingsStore(context) }
    var style by remember { mutableStateOf(store.style) }
    var mode by remember { mutableStateOf(store.mode) }
    var size by remember { mutableStateOf(store.size) }
    var difficulty by remember { mutableStateOf(store.difficulty) }

    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val dark = when (mode) {
        "DAY" -> false
        "NIGHT" -> true
        else -> systemDark
    }
    val palette = paletteFor(dark, style)

    var screen by remember { mutableStateOf("home") }
    var showNewGame by remember { mutableStateOf(false) }
    var game by remember { mutableStateOf<GameState?>(null) }
    var helpFromPause by remember { mutableStateOf(false) }
    var shareNotice by remember { mutableStateOf(false) }

    LaunchedEffect(shareNotice) {
        if (shareNotice) {
            delay(1600)
            shareNotice = false
        }
    }

    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(250)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(palette.background)) {
        when (screen) {
            "themes" -> ThemesScreen(
                palette = palette,
                style = style,
                mode = mode,
                onStyle = { style = it; store.style = it },
                onMode = { mode = it; store.mode = it },
                onBack = { screen = "home" }
            )
            "help" -> HelpScreen(
                palette = palette,
                onBack = {
                    if (helpFromPause) {
                        screen = "game"
                        game?.paused = true
                    } else {
                        screen = "home"
                    }
                    helpFromPause = false
                }
            )
            "game" -> {
                val g = game
                if (g == null) {
                    HomeScreen(
                        palette = palette,
                        onNewGame = { showNewGame = true },
                        onHelp = { helpFromPause = false; screen = "help" },
                        onThemes = { screen = "themes" }
                    )
                } else {
                    GameScreen(
                        game = g,
                        now = now,
                        palette = palette,
                        onPause = { g.pause(System.currentTimeMillis()) },
                        onResume = { g.resume(System.currentTimeMillis()) },
                        onReset = {
                            g.grid = Array(g.puzzle.size) { g.puzzle.givens[it].clone() }
                            g.selected = -1
                        },
                        onSelect = { r, c ->
                            val idx = r * g.puzzle.size + c
                            if (g.puzzle.givens[r][c] == 0) g.selected = idx
                        },
                        onDigit = { d ->
                            val idx = g.selected
                            if (idx >= 0) {
                                val r = idx / g.puzzle.size
                                val c = idx % g.puzzle.size
                                if (g.puzzle.givens[r][c] == 0) {
                                    val ng = Array(g.puzzle.size) { g.grid[it].clone() }
                                    ng[r][c] = d
                                    g.grid = ng
                                    if (Futoshiki.isSolved(ng, g.puzzle)) {
                                        g.finish(System.currentTimeMillis())
                                        screen = "win"
                                    }
                                }
                            }
                        },
                        onLongClear = { r, c ->
                            if (g.puzzle.givens[r][c] == 0) {
                                val ng = Array(g.puzzle.size) { g.grid[it].clone() }
                                ng[r][c] = 0
                                g.grid = ng
                            }
                        },
                        onNewGame = { showNewGame = true },
                        onHelp = { helpFromPause = true; screen = "help" },
                        onMainMenu = { game = null; screen = "home" }
                    )
                    if (g.paused) {
                        PauseOverlay(
                            palette = palette,
                            elapsed = g.elapsed(now),
                            onResume = { g.resume(System.currentTimeMillis()) },
                            onNewGame = { showNewGame = true },
                            onHelp = { helpFromPause = true; screen = "help" },
                            onMainMenu = { game = null; screen = "home" }
                        )
                    }
                }
            }
            "win" -> {
                val g = game
                if (g == null) {
                    HomeScreen(
                        palette = palette,
                        onNewGame = { showNewGame = true },
                        onHelp = { helpFromPause = false; screen = "help" },
                        onThemes = { screen = "themes" }
                    )
                } else {
                    WinScreen(
                        palette = palette,
                        size = g.puzzle.size,
                        difficulty = g.difficulty,
                        elapsed = g.solvedElapsed,
                        pauseTotal = g.pauseTotal,
                        shareNotice = shareNotice,
                        onShare = { share(context, g); shareNotice = true },
                        onNewGame = { showNewGame = true }
                    )
                }
            }
            else -> HomeScreen(
                palette = palette,
                onNewGame = { showNewGame = true },
                onHelp = { helpFromPause = false; screen = "help" },
                onThemes = { screen = "themes" }
            )
        }

        if (showNewGame) {
            NewGameDialog(
                palette = palette,
                size = size,
                difficulty = difficulty,
                onSize = { size = it; store.size = it },
                onDifficulty = { difficulty = it; store.difficulty = it },
                onStart = {
                    val d = Difficulty.valueOf(difficulty)
                    val rng = Random(System.currentTimeMillis())
                    val p = Futoshiki.generate(size, d, rng)
                    game = GameState(p, d)
                    showNewGame = false
                    screen = "game"
                },
                onDismiss = { showNewGame = false }
            )
        }
    }
}

private fun share(context: Context, game: GameState) {
    val d = game.difficulty
    val text = "I solved Futoshiki ${game.puzzle.size}x${game.puzzle.size} ${d.label} in ${formatTime(game.solvedElapsed)}!"
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Futoshiki", text))
}

// ---------------------------------------------------------------------------
// Reusable widgets
// ---------------------------------------------------------------------------

@Composable
fun PixelText(
    text: String,
    color: Color,
    size: Int = 16,
    weight: FontWeight = FontWeight.Bold,
    letterSpacing: Float = 0.18f,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Center
) {
    Text(
        text = text,
        color = color,
        fontFamily = FontFamily.Monospace,
        fontWeight = weight,
        fontSize = size.sp,
        letterSpacing = letterSpacing.sp,
        textAlign = textAlign,
        modifier = modifier
    )
}

@Composable
fun OutlinedButton(label: String, palette: Palette, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(2.dp, palette.outline, RoundedCornerShape(20.dp))
            .background(Color.Transparent)
            .combinedClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        PixelText(label, palette.text, 16)
    }
}

@Composable
fun FilledButton(label: String, palette: Palette, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(palette.filledBg)
            .combinedClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        PixelText(label, palette.filledText, 16)
    }
}

@Composable
fun PauseIcon(size: Dp = 26.dp) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(size * 0.28f).height(size).clip(RoundedCornerShape(size * 0.12f)).background(Color.White))
        Spacer(modifier = Modifier.width(size * 0.22f))
        Box(modifier = Modifier.width(size * 0.28f).height(size).clip(RoundedCornerShape(size * 0.12f)).background(Color.White))
    }
}

// ---------------------------------------------------------------------------
// Scene art
// ---------------------------------------------------------------------------

@Composable
fun SceneBand(
    palette: Palette,
    showDragon: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawScene(palette, showDragon)
        }
        content()
    }
}

private fun DrawScope.drawScene(palette: Palette, showDragon: Boolean) {
    val w = size.width
    val h = size.height
    val base = palette.artBase
    val ground = h * 0.86f

    // sky wash
    drawRect(color = base.copy(alpha = if (palette.tinted) 0.06f else 0.04f), size = Size(w, ground))

    // distant mountains
    val mPath = Path().apply {
        moveTo(0f, ground)
        lineTo(0f, ground * 0.72f)
        lineTo(w * 0.16f, ground * 0.5f)
        lineTo(w * 0.3f, ground * 0.72f)
        lineTo(w * 0.44f, ground * 0.46f)
        lineTo(w * 0.62f, ground * 0.74f)
        lineTo(w * 0.76f, ground * 0.54f)
        lineTo(w * 0.9f, ground * 0.74f)
        lineTo(w, ground * 0.6f)
        lineTo(w, ground)
        close()
    }
    drawPath(mPath, color = base.copy(alpha = 0.22f))

    // clouds
    fun cloud(cx: Float, cy: Float, s: Float) {
        val a = base.copy(alpha = 0.30f)
        drawCircle(a, s, Offset(cx, cy))
        drawCircle(a, s * 0.75f, Offset(cx + s * 0.8f, cy + s * 0.12f))
        drawCircle(a, s * 0.62f, Offset(cx - s * 0.85f, cy + s * 0.16f))
        drawRect(a, topLeft = Offset(cx - s * 0.9f, cy), size = Size(s * 1.8f, s * 0.5f))
    }
    cloud(w * 0.14f, h * 0.24f, h * 0.09f)
    cloud(w * 0.62f, h * 0.38f, h * 0.11f)
    cloud(w * 0.85f, h * 0.2f, h * 0.07f)

    // ground
    drawRect(color = base.copy(alpha = 0.35f), topLeft = Offset(0f, ground), size = Size(w, h - ground))

    // tree (right)
    val treeCx = w * 0.86f
    val treeBase = ground
    drawRect(
        color = base.copy(alpha = 0.85f),
        topLeft = Offset(treeCx - w * 0.012f, treeBase - h * 0.42f),
        size = Size(w * 0.024f, h * 0.42f)
    )
    val canopy = base.copy(alpha = 0.85f)
    drawCircle(canopy, h * 0.15f, Offset(treeCx, treeBase - h * 0.5f))
    drawCircle(canopy, h * 0.1f, Offset(treeCx - w * 0.05f, treeBase - h * 0.44f))
    drawCircle(canopy, h * 0.1f, Offset(treeCx + w * 0.05f, treeBase - h * 0.45f))

    // torii gate (center)
    val toriiCx = w * 0.5f
    val gateBottom = ground
    val postH = h * 0.5f
    val postW = w * 0.016f
    val postColor = base.copy(alpha = 0.95f)
    drawRect(postColor, topLeft = Offset(toriiCx - w * 0.11f, gateBottom - postH), size = Size(postW, postH))
    drawRect(postColor, topLeft = Offset(toriiCx + w * 0.094f, gateBottom - postH), size = Size(postW, postH))
    // top beam (slightly curved)
    drawRect(postColor, topLeft = Offset(toriiCx - w * 0.17f, gateBottom - postH), size = Size(w * 0.34f, h * 0.035f))
    drawRect(postColor, topLeft = Offset(toriiCx - w * 0.14f, gateBottom - postH + h * 0.06f), size = Size(w * 0.28f, h * 0.018f))

    // samurai runner silhouette
    val sx = w * 0.5f
    val sy = ground - h * 0.02f
    drawCircle(base.copy(alpha = 0.95f), h * 0.022f, Offset(sx, sy - h * 0.09f))
    drawRect(
        base.copy(alpha = 0.95f),
        topLeft = Offset(sx - w * 0.01f, sy - h * 0.075f),
        size = Size(w * 0.02f, h * 0.055f)
    )

    if (showDragon) {
        val dc = base.copy(alpha = 0.9f)
        val dx = w * 0.16f
        val dy = h * 0.5f
        val body = Path().apply {
            moveTo(dx - w * 0.14f, dy)
            cubicTo(dx - w * 0.02f, dy - h * 0.22f, dx + w * 0.16f, dy + h * 0.05f, dx + w * 0.24f, dy - h * 0.02f)
            lineTo(dx + w * 0.24f, dy + h * 0.06f)
            cubicTo(dx + w * 0.1f, dy + h * 0.14f, dx - w * 0.04f, dy + h * 0.06f, dx - w * 0.14f, dy + h * 0.1f)
            close()
        }
        drawPath(body, dc)
        // wing
        val wing = Path().apply {
            moveTo(dx + w * 0.02f, dy - h * 0.06f)
            lineTo(dx + w * 0.2f, dy - h * 0.26f)
            lineTo(dx + w * 0.22f, dy - h * 0.04f)
            close()
        }
        drawPath(wing, dc.copy(alpha = 0.7f))
    }
}

// ---------------------------------------------------------------------------
// Home
// ---------------------------------------------------------------------------

@Composable
fun HomeScreen(palette: Palette, onNewGame: () -> Unit, onHelp: () -> Unit, onThemes: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        SceneBand(palette = palette, showDragon = false, modifier = Modifier.height(200.dp))
        Spacer(modifier = Modifier.height(24.dp))
        PixelText(
            "FUTOSHIKI",
            palette.text,
            size = 38,
            letterSpacing = 0.06f,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(40.dp))
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            FilledButton("NEW GAME", palette, onNewGame)
            OutlinedButton("HELP", palette, onHelp)
            OutlinedButton("THEMES", palette, onThemes)
        }
        Spacer(modifier = Modifier.weight(1f))
        PixelText(
            "Made with love by @HeX",
            palette.subText,
            size = 12,
            weight = FontWeight.Normal,
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// Themes
// ---------------------------------------------------------------------------

@Composable
fun ThemesScreen(
    palette: Palette,
    style: String,
    mode: String,
    onStyle: (String) -> Unit,
    onMode: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Spacer(modifier = Modifier.height(44.dp))
        PixelText("T H E M E S", palette.text, size = 15, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(28.dp))
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(palette.cellBg),
            horizontalArrangement = Arrangement.Center
        ) {
            SegmentedOption("MONO", style == "MONO", palette, Modifier.weight(1f)) { onStyle("MONO") }
            SegmentedOption("TINTED", style == "TINTED", palette, Modifier.weight(1f)) { onStyle("TINTED") }
        }
        Spacer(modifier = Modifier.weight(1f))
        PixelText(
            "\u706b",
            if (style == "TINTED") palette.accent else palette.text,
            size = 150,
            letterSpacing = 0f,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            PixelText("\u25c0", palette.text, size = 18, modifier = Modifier.width(48.dp))
            Spacer(modifier = Modifier.width(16.dp))
            PixelText("F I R E", palette.text, size = 16, modifier = Modifier.width(140.dp))
            Spacer(modifier = Modifier.width(16.dp))
            PixelText("\u25b6", palette.text, size = 18, modifier = Modifier.width(48.dp))
        }
        Spacer(modifier = Modifier.weight(1f))
        var localMode by remember { mutableStateOf(mode) }
        Box(modifier = Modifier.fillMaxWidth().height(28.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(palette.subText)
                    .align(Alignment.Center)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ModeStop(localMode == "AUTO", palette) { localMode = "AUTO"; onMode("AUTO") }
                ModeStop(localMode == "DAY", palette) { localMode = "DAY"; onMode("DAY") }
                ModeStop(localMode == "NIGHT", palette) { localMode = "NIGHT"; onMode("NIGHT") }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ModeLabel("AUTO", localMode == "AUTO", palette) { localMode = "AUTO"; onMode("AUTO") }
            ModeLabel("DAY", localMode == "DAY", palette) { localMode = "DAY"; onMode("DAY") }
            ModeLabel("NIGHT", localMode == "NIGHT", palette) { localMode = "NIGHT"; onMode("NIGHT") }
        }
        Spacer(modifier = Modifier.height(40.dp))
        FilledButton("BACK", palette.copy(filledBg = palette.accent, filledText = Color.White), onBack)
        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun SegmentedOption(
    label: String,
    selected: Boolean,
    palette: Palette,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .padding(4.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (selected) palette.accent else Color.Transparent)
            .combinedClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        PixelText(label, if (selected) Color.White else palette.subText, 13)
    }
}

@Composable
private fun ModeLabel(label: String, selected: Boolean, palette: Palette, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier.combinedClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        PixelText(label, if (selected) palette.accent else palette.subText, 12)
    }
}

@Composable
private fun ModeStop(selected: Boolean, palette: Palette, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(28.dp).combinedClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            PixelText("\u2726", palette.text, size = 20, letterSpacing = 0f)
        } else {
            Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(palette.subText))
        }
    }
}

// ---------------------------------------------------------------------------
// Help
// ---------------------------------------------------------------------------

@Composable
fun HelpScreen(palette: Palette, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        val pager = rememberPagerState(pageCount = { 2 })
        Box(modifier = Modifier.weight(1f)) {
            VerticalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
                if (page == 0) HelpPageOne(palette) else HelpPageTwo(palette)
            }
        }
        Box(modifier = Modifier.padding(horizontal = 28.dp, vertical = 28.dp)) {
            FilledButton("BACK", palette, onBack)
        }
    }
}

@Composable
private fun HelpPageOne(palette: Palette) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Box(
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(2.dp, palette.outline, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PixelText("How to Play", palette.text, 20)
                Spacer(modifier = Modifier.height(14.dp))
                PixelText("\u3075\u3068\u6307\u63ee", palette.accent, 30, letterSpacing = 0.1f)
                Spacer(modifier = Modifier.height(8.dp))
                PixelText("(Japanese For \"inequality\")", palette.subText, 12, weight = FontWeight.Normal)
                Spacer(modifier = Modifier.height(24.dp))
                RuleBlock(
                    palette,
                    "Futoshiki is a logic puzzle on a square grid. Fill the board so every row and column contains each number exactly once."
                )
                Spacer(modifier = Modifier.height(16.dp))
                RuleBlock(
                    palette,
                    "Select a cell, choose a digit. Complete the puzzle when all cells are filled without breaking any rules."
                )
                Spacer(modifier = Modifier.height(20.dp))
                PixelText("\u2191 Swipe up For rules", palette.subText, 12, weight = FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun RuleBlock(palette: Palette, text: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.width(4.dp).height(52.dp).background(palette.accent.copy(alpha = 0.5f)))
        Spacer(modifier = Modifier.width(12.dp))
        PixelText(
            text,
            palette.text,
            13,
            weight = FontWeight.Normal,
            letterSpacing = 0.02f,
            textAlign = TextAlign.Start,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun HelpPageTwo(palette: Palette) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Box(
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(2.dp, palette.outline, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PixelText("Core Rules", palette.text, 20)
                Spacer(modifier = Modifier.height(24.dp))
                CoreRule(palette, "01", "Unique Rows & Columns", "Every row and column contains each number exactly once, like Sudoku.")
                Spacer(modifier = Modifier.height(16.dp))
                CoreRule(palette, "02", "Inequality Signs", "If A < B, then A must contain a smaller number than B.")
                Spacer(modifier = Modifier.height(16.dp))
                CoreRule(palette, "03", "Pre-Filled Cells", "Respect any digits already placed on the board at the start.")
                Spacer(modifier = Modifier.height(20.dp))
                PixelText("\u2193 Swipe down For how to play", palette.subText, 12, weight = FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun CoreRule(palette: Palette, index: String, title: String, body: String) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(palette.cellBg).padding(12.dp)) {
        Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(palette.accent.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            PixelText(index, palette.accent, 12)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            PixelText(title, palette.text, 14, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(6.dp))
            PixelText(body, palette.text, 12, weight = FontWeight.Normal, letterSpacing = 0.02f, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
        }
    }
}

// ---------------------------------------------------------------------------
// New game dialog
// ---------------------------------------------------------------------------

@Composable
fun NewGameDialog(
    palette: Palette,
    size: Int,
    difficulty: String,
    onSize: (Int) -> Unit,
    onDifficulty: (String) -> Unit,
    onStart: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(2.dp, palette.outline, RoundedCornerShape(24.dp))
                .background(palette.background)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PixelText("GRID SIZE", palette.text, 13)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(palette.cellBg),
                horizontalArrangement = Arrangement.Center
            ) {
                for (s in listOf(4, 5, 6)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (size == s) palette.accent else Color.Transparent)
                            .combinedClickable { onSize(s) },
                        contentAlignment = Alignment.Center
                    ) {
                        PixelText("${s}x${s}", if (size == s) Color.White else palette.text, 14)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            PixelText("DIFFICULTY", palette.text, 13)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                for (d in Difficulty.values()) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.15f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (difficulty == d.name) palette.accent else palette.cellBg)
                                .combinedClickable { onDifficulty(d.name) },
                            contentAlignment = Alignment.Center
                        ) {
                            PixelText(
                                d.kanji,
                                if (difficulty == d.name) Color.White else palette.text,
                                34,
                                letterSpacing = 0f
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        PixelText(d.label, palette.subText, 11, weight = FontWeight.Normal)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            FilledButton("START", palette, onStart)
        }
    }
}

// ---------------------------------------------------------------------------
// Game
// ---------------------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GameScreen(
    game: GameState,
    now: Long,
    palette: Palette,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
    onSelect: (Int, Int) -> Unit,
    onDigit: (Int) -> Unit,
    onLongClear: (Int, Int) -> Unit,
    onNewGame: () -> Unit,
    onHelp: () -> Unit,
    onMainMenu: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) { drawScene(palette, showDragon = true) }
            PixelText(
                formatTime(game.elapsed(now)),
                palette.text,
                size = 22,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 44.dp, end = 24.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Board(
            game = game,
            palette = palette,
            onSelect = onSelect,
            onLongClear = onLongClear,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.weight(1f))
        Keypad(game = game, palette = palette, onDigit = onDigit)
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp, 64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(palette.accent)
                    .combinedClickable(onClick = onPause),
                contentAlignment = Alignment.Center
            ) {
                PauseIcon()
            }
            Spacer(modifier = Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(palette.cellBg)
                    .combinedClickable(onClick = onReset),
                contentAlignment = Alignment.Center
            ) {
                PixelText("RESET", palette.text, 16)
            }
        }
        Spacer(modifier = Modifier.height(28.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Board(
    game: GameState,
    palette: Palette,
    onSelect: (Int, Int) -> Unit,
    onLongClear: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val size = game.puzzle.size
    val bad = Futoshiki.conflicts(game.grid, game.puzzle)
    BoxWithConstraints(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val gapF = 0.5f
        val cellDp: Dp = maxWidth / (size + (size - 1) * gapF)
        val ineqDp: Dp = cellDp * gapF
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            for (r in 0 until size) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    for (c in 0 until size) {
                        val value = game.grid[r][c]
                        val given = game.puzzle.givens[r][c] != 0
                        val selected = game.selected == r * size + c
                        Box(
                            modifier = Modifier
                                .size(cellDp)
                                .clip(RoundedCornerShape(cellDp * 0.28f))
                                .background(
                                    if (selected) palette.accent.copy(alpha = 0.18f) else palette.cellBg
                                )
                                .border(
                                    width = if (selected || bad[r][c]) 2.dp else 0.dp,
                                    color = when {
                                        bad[r][c] -> palette.accent
                                        selected -> palette.accent
                                        else -> Color.Transparent
                                    },
                                    shape = RoundedCornerShape(cellDp * 0.28f)
                                )
                                .combinedClickable(
                                    onClick = { onSelect(r, c) },
                                    onLongClick = { onLongClear(r, c) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (value != 0) {
                                PixelText(
                                    "$value",
                                    if (bad[r][c]) palette.accent else palette.cellText,
                                    size = (cellDp.value * 0.56f).toInt().coerceAtLeast(14),
                                    letterSpacing = 0f
                                )
                            }
                        }
                        if (c < size - 1) {
                            Box(modifier = Modifier.width(ineqDp), contentAlignment = Alignment.Center) {
                                val s = game.puzzle.hIneq[r][c]
                                if (s != 0) {
                                    PixelText(
                                        if (s == 1) "<" else ">",
                                        if (palette.tinted) palette.accent else palette.text,
                                        size = (ineqDp.value * 0.7f).toInt().coerceAtLeast(12),
                                        letterSpacing = 0f
                                    )
                                }
                            }
                        }
                    }
                }
                if (r < size - 1) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        for (c in 0 until size) {
                            Box(modifier = Modifier.width(cellDp).height(ineqDp), contentAlignment = Alignment.Center) {
                                val s = game.puzzle.vIneq[r][c]
                                if (s != 0) {
                                    PixelText(
                                        if (s == 1) "\u2227" else "\u2228",
                                        if (palette.tinted) palette.accent else palette.text,
                                        size = (ineqDp.value * 0.7f).toInt().coerceAtLeast(12),
                                        letterSpacing = 0f
                                    )
                                }
                            }
                            if (c < size - 1) Spacer(modifier = Modifier.width(ineqDp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Keypad(game: GameState, palette: Palette, onDigit: (Int) -> Unit) {
    val size = game.puzzle.size
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (d in 1..size) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(palette.cellBg)
                    .combinedClickable { onDigit(d) },
                contentAlignment = Alignment.Center
            ) {
                PixelText("$d", palette.cellText, 22, letterSpacing = 0f)
            }
        }
    }
}

@Composable
fun PauseOverlay(
    palette: Palette,
    elapsed: Long,
    onResume: () -> Unit,
    onNewGame: () -> Unit,
    onHelp: () -> Unit,
    onMainMenu: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(palette.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) { drawScene(palette, showDragon = true) }
                PixelText(
                    formatTime(elapsed),
                    palette.text,
                    size = 22,
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 44.dp, end = 24.dp)
                )
                PixelText(
                    "P A U S E D",
                    palette.text,
                    size = 18,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 116.dp)
                )
            }
            Spacer(modifier = Modifier.height(28.dp))
            PixelText("FUTOSHIKI", palette.text, 34, letterSpacing = 0.06f, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(36.dp))
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                FilledButton("NEW GAME", palette, onNewGame)
                OutlinedButton("RESUME", palette, onResume)
                OutlinedButton("HELP", palette, onHelp)
                OutlinedButton("MAIN MENU", palette, onMainMenu)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Win
// ---------------------------------------------------------------------------

@Composable
fun WinScreen(
    palette: Palette,
    size: Int,
    difficulty: Difficulty,
    elapsed: Long,
    pauseTotal: Long,
    shareNotice: Boolean,
    onShare: () -> Unit,
    onNewGame: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) { drawScene(palette, showDragon = false) }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 44.dp, end = 24.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(palette.filledBg)
                    .combinedClickable(onClick = onShare)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                PixelText("SHARE", palette.filledText, 14)
            }
            if (shareNotice) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 104.dp, end = 24.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(palette.cellBg)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    PixelText("Result copied", palette.cellText, 11)
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Pill("Grid ${size}x${size}", palette)
            Spacer(modifier = Modifier.width(12.dp))
            Pill("Mode ${difficulty.label}", palette)
        }
        Spacer(modifier = Modifier.weight(1f))
        PixelText("\u52dd", palette.subText.copy(alpha = 0.55f), size = 170, letterSpacing = 0f, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.weight(1f))
        PixelText("Y O U   W O N", palette.accent, 22, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(20.dp))
        PixelText("SOLVED IN", palette.subText, 13, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(10.dp))
        PixelText(formatTime(elapsed), palette.text, 34, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(10.dp))
        PixelText(
            "although you spent ${formatTime(pauseTotal)} in pause menu",
            palette.accent,
            12,
            weight = FontWeight.Normal,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.weight(1f))
        Box(modifier = Modifier.padding(horizontal = 28.dp, vertical = 28.dp)) {
            FilledButton("NEW GAME", palette, onNewGame)
        }
    }
}

@Composable
private fun Pill(label: String, palette: Palette) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(palette.cellBg)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        PixelText(label, palette.cellText, 14)
    }
}
