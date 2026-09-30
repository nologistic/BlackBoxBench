package com.blackboxbench.reproduction

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlin.math.cos
import kotlin.math.sin

// ---------------- Colors ----------------
object AppColors {
    val Bg = Color(0xFF0D0D0D)
    val Card = Color(0xFF161616)
    val CardBorder = Color(0xFF2B2B2B)
    val Btn = Color(0xFF1E1E1E)
    val BtnBorder = Color(0xFF333333)
    val White = Color(0xFFFFFFFF)
    val Gray = Color(0xFF9E9E9E)
    val DimGray = Color(0xFF5C5C5C)
    val Red = Color(0xFFE53935)
    val Yellow = Color(0xFFFFD54F)
    val CellBg = Color(0xFF151515)
    val CellBorder = Color(0xFF2E2E2E)
}

// ---------------- Persistence ----------------
data class InProgress(val mode: String, val cells: String, val moves: Int, val elapsed: Int)

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("nonogram_prefs", Context.MODE_PRIVATE)

    var currentLevel: Int
        get() = sp.getInt("currentLevel", 1)
        set(v) = sp.edit().putInt("currentLevel", v).apply()

    var completed: Set<Int>
        get() = sp.getStringSet("completed", emptySet())!!.mapNotNull { it.toIntOrNull() }.toSet()
        set(v) = sp.edit().putStringSet("completed", v.map { it.toString() }.toSet()).apply()

    var haptic: Boolean
        get() = sp.getBoolean("haptic", true)
        set(v) = sp.edit().putBoolean("haptic", v).apply()

    var longPressCross: Boolean
        get() = sp.getBoolean("longPressCross", true)
        set(v) = sp.edit().putBoolean("longPressCross", v).apply()

    var cycleMode: Boolean
        get() = sp.getBoolean("cycleMode", false)
        set(v) = sp.edit().putBoolean("cycleMode", v).apply()

    fun saveInProgress(p: InProgress) = sp.edit()
        .putString("ip_mode", p.mode)
        .putString("ip_cells", p.cells)
        .putInt("ip_moves", p.moves)
        .putInt("ip_elapsed", p.elapsed)
        .apply()

    fun loadInProgress(): InProgress? {
        val mode = sp.getString("ip_mode", null) ?: return null
        val cells = sp.getString("ip_cells", null) ?: return null
        return InProgress(mode, cells, sp.getInt("ip_moves", 0), sp.getInt("ip_elapsed", 0))
    }

    fun clearInProgress() = sp.edit()
        .remove("ip_mode").remove("ip_cells").remove("ip_moves").remove("ip_elapsed").apply()

    fun resetProgress() {
        sp.edit().putInt("currentLevel", 1).putStringSet("completed", emptySet()).apply()
        clearInProgress()
    }
}

// ---------------- Puzzles ----------------
val DIFFICULTIES = listOf("EASY", "MEDIUM", "HARD", "MASTER", "EXPERT")
const val CAMPAIGN_LEVELS = 11

fun diffSize(d: String): Int = when (d) {
    "EASY" -> 5
    "MEDIUM" -> 7
    "HARD" -> 8
    "MASTER" -> 10
    "EXPERT" -> 12
    else -> 5
}

fun parsePuzzle(vararg rows: String): Array<BooleanArray> =
    rows.map { r -> BooleanArray(r.length) { i -> r[i] == 'X' } }.toTypedArray()

fun generatePuzzle(size: Int, seed: Long): Array<BooleanArray> {
    val rnd = java.util.Random(seed)
    while (true) {
        val g = Array(size) { BooleanArray(size) { rnd.nextFloat() < 0.55f } }
        val rowsOk = g.all { row -> row.any { it } }
        val colsOk = (0 until size).all { c -> (0 until size).any { r -> g[r][c] } }
        if (rowsOk && colsOk) return g
    }
}

fun campaignPuzzle(level: Int): Array<BooleanArray> = when (level) {
    1 -> parsePuzzle("XX...", "XX.X.", ".XX.X", "XX.XX", "XX..X")
    2 -> parsePuzzle("XXXXX", "...XX", "XXXXX", ".X..X", "XX.XX")
    3 -> parsePuzzle("...XX", "XXX.X", ".X.X.", "X..X.", ".X.X.")
    else -> {
        val size = when (level) {
            4, 5 -> 5
            6, 7 -> 6
            8, 9 -> 7
            else -> 8
        }
        generatePuzzle(size, 4242L + level * 97L)
    }
}

fun lineClues(line: BooleanArray): List<Int> {
    val out = mutableListOf<Int>()
    var run = 0
    for (c in line) {
        if (c) run++ else if (run > 0) { out.add(run); run = 0 }
    }
    if (run > 0) out.add(run)
    if (out.isEmpty()) out.add(0)
    return out
}

fun rowClues(sol: Array<BooleanArray>): List<List<Int>> = sol.map { lineClues(it) }

fun colClues(sol: Array<BooleanArray>): List<List<Int>> {
    val n = sol.size
    return (0 until n).map { c -> lineClues(BooleanArray(n) { r -> sol[r][c] }) }
}

fun openBrowser(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: Exception) {
    }
}

// ---------------- App entry ----------------
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ReproducedApp() }
    }
}

@Composable
fun ReproducedApp() {
    MaterialTheme(colorScheme = darkColorScheme(background = AppColors.Bg, surface = AppColors.Bg)) {
        Surface(modifier = Modifier.fillMaxSize(), color = AppColors.Bg) {
            val nav = rememberNavController()
            NavHost(navController = nav, startDestination = "menu") {
                composable("menu") { MenuScreen(nav) }
                composable("levels") { LevelSelectScreen(nav) }
                composable("howto") { HowToPlayScreen(nav) }
                composable("settings") { SettingsScreen(nav) }
                composable("multiplayer") { MultiplayerScreen(nav) }
                composable("game/{mode}") { entry ->
                    GameScreen(nav, entry.arguments?.getString("mode") ?: "camp_1")
                }
            }
        }
    }
}

// ---------------- Small building blocks ----------------
@Composable
fun CanvasIcon(iconSize: Dp, content: DrawScope.() -> Unit) {
    Canvas(modifier = Modifier.size(iconSize), onDraw = content)
}

fun DrawScope.strokePath(points: List<Offset>, color: Color, width: Float) {
    for (i in 0 until points.size - 1) {
        drawLine(color, points[i], points[i + 1], strokeWidth = width, cap = StrokeCap.Round)
    }
}

@Composable
fun BackArrowIcon(color: Color = AppColors.White) {
    CanvasIcon(22.dp) {
        val w = size.width; val h = size.height; val sw = w * 0.14f
        strokePath(
            listOf(Offset(w * 0.68f, h * 0.14f), Offset(w * 0.28f, h * 0.5f), Offset(w * 0.68f, h * 0.86f)),
            color, sw
        )
    }
}

@Composable
fun StarIcon(color: Color = AppColors.Yellow, iconSize: Dp = 22.dp) {
    CanvasIcon(iconSize) {
        val cx = size.width / 2f; val cy = size.height / 2f
        val rOut = size.width * 0.48f; val rIn = rOut * 0.42f
        val path = Path()
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) rOut else rIn
            val a = Math.toRadians((-90 + i * 36).toDouble())
            val x = (cx + r * cos(a)).toFloat(); val y = (cy + r * sin(a)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path, color)
    }
}

@Composable
fun HeartIcon(color: Color = AppColors.Red, iconSize: Dp = 22.dp) {
    CanvasIcon(iconSize) {
        val w = size.width; val h = size.height
        val r = w * 0.25f
        drawCircle(color, r, Offset(w * 0.32f, h * 0.32f))
        drawCircle(color, r, Offset(w * 0.68f, h * 0.32f))
        val path = Path()
        path.moveTo(w * 0.08f, h * 0.42f)
        path.lineTo(w * 0.5f, h * 0.94f)
        path.lineTo(w * 0.92f, h * 0.42f)
        path.close()
        drawPath(path, color)
    }
}

@Composable
fun LockIcon(color: Color = AppColors.DimGray, iconSize: Dp = 26.dp) {
    CanvasIcon(iconSize) {
        val w = size.width; val h = size.height
        drawArc(
            color, 180f, 180f, false,
            topLeft = Offset(w * 0.24f, h * 0.10f), size = Size(w * 0.52f, h * 0.56f),
            style = Stroke(width = w * 0.12f)
        )
        drawRoundRect(
            color, topLeft = Offset(w * 0.14f, h * 0.42f), size = Size(w * 0.72f, h * 0.5f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.1f)
        )
        drawCircle(AppColors.Bg, w * 0.07f, Offset(w * 0.5f, h * 0.62f))
    }
}

@Composable
fun CheckIcon(color: Color = AppColors.White, iconSize: Dp = 14.dp) {
    CanvasIcon(iconSize) {
        val w = size.width; val h = size.height; val sw = w * 0.2f
        strokePath(
            listOf(Offset(w * 0.15f, h * 0.55f), Offset(w * 0.4f, h * 0.8f), Offset(w * 0.85f, h * 0.2f)),
            color, sw
        )
    }
}

@Composable
fun RefreshIcon(color: Color = AppColors.White, iconSize: Dp = 20.dp) {
    CanvasIcon(iconSize) {
        val w = size.width; val sw = w * 0.13f
        drawArc(color, 30f, 300f, false, style = Stroke(width = sw, cap = StrokeCap.Round))
        val path = Path()
        path.moveTo(w * 0.86f, w * 0.02f)
        path.lineTo(w * 0.98f, w * 0.3f)
        path.lineTo(w * 0.64f, w * 0.28f)
        path.close()
        drawPath(path, color)
    }
}

@Composable
fun BulbIcon(color: Color = AppColors.Yellow, iconSize: Dp = 18.dp) {
    CanvasIcon(iconSize) {
        val w = size.width; val h = size.height
        drawCircle(color, w * 0.26f, Offset(w * 0.5f, h * 0.38f))
        drawRect(color, topLeft = Offset(w * 0.4f, h * 0.66f), size = Size(w * 0.2f, h * 0.16f))
        drawLine(color, Offset(w * 0.42f, h * 0.9f), Offset(w * 0.58f, h * 0.9f), strokeWidth = w * 0.07f, cap = StrokeCap.Round)
    }
}

@Composable
fun ClockIcon(color: Color = AppColors.Gray, iconSize: Dp = 18.dp) {
    CanvasIcon(iconSize) {
        val w = size.width
        drawCircle(color, w * 0.42f, style = Stroke(width = w * 0.1f))
        strokePath(listOf(Offset(w * 0.5f, w * 0.5f), Offset(w * 0.5f, w * 0.24f)), color, w * 0.1f)
        strokePath(listOf(Offset(w * 0.5f, w * 0.5f), Offset(w * 0.68f, w * 0.56f)), color, w * 0.1f)
    }
}

@Composable
fun UndoIcon(color: Color = AppColors.White, iconSize: Dp = 18.dp) {
    CanvasIcon(iconSize) {
        val w = size.width
        drawArc(color, -60f, 270f, false, style = Stroke(width = w * 0.12f, cap = StrokeCap.Round))
        val path = Path()
        path.moveTo(w * 0.16f, w * 0.3f)
        path.lineTo(w * 0.02f, w * 0.04f)
        path.lineTo(w * 0.36f, w * 0.02f)
        path.close()
        drawPath(path, color)
    }
}

@Composable
fun HomeIcon(color: Color = AppColors.White, iconSize: Dp = 20.dp) {
    CanvasIcon(iconSize) {
        val w = size.width; val h = size.height
        val roof = Path()
        roof.moveTo(w * 0.5f, h * 0.06f)
        roof.lineTo(w * 0.96f, h * 0.48f)
        roof.lineTo(w * 0.8f, h * 0.48f)
        roof.lineTo(w * 0.8f, h * 0.94f)
        roof.lineTo(w * 0.2f, h * 0.94f)
        roof.lineTo(w * 0.2f, h * 0.48f)
        roof.lineTo(w * 0.04f, h * 0.48f)
        roof.close()
        drawPath(roof, color)
    }
}

@Composable
fun CoffeeIcon(color: Color = AppColors.White, iconSize: Dp = 20.dp) {
    CanvasIcon(iconSize) {
        val w = size.width; val h = size.height
        drawRoundRect(
            color, topLeft = Offset(w * 0.12f, h * 0.2f), size = Size(w * 0.6f, h * 0.52f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f),
            style = Stroke(width = w * 0.09f)
        )
        drawArc(color, -80f, 160f, false, topLeft = Offset(w * 0.62f, h * 0.28f), size = Size(w * 0.26f, h * 0.36f), style = Stroke(width = w * 0.09f))
        drawLine(color, Offset(w * 0.1f, h * 0.86f), Offset(w * 0.9f, h * 0.86f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
    }
}

@Composable
fun CopyIcon(color: Color = AppColors.White, iconSize: Dp = 18.dp) {
    CanvasIcon(iconSize) {
        val w = size.width
        val cr = androidx.compose.ui.geometry.CornerRadius(w * 0.08f)
        drawRoundRect(color, topLeft = Offset(w * 0.06f, w * 0.3f), size = Size(w * 0.58f, w * 0.64f), cornerRadius = cr, style = Stroke(width = w * 0.09f))
        drawRoundRect(color, topLeft = Offset(w * 0.36f, w * 0.06f), size = Size(w * 0.58f, w * 0.64f), cornerRadius = cr, style = Stroke(width = w * 0.09f))
    }
}

@Composable
fun PasteIcon(color: Color = AppColors.Gray, iconSize: Dp = 20.dp) {
    CanvasIcon(iconSize) {
        val w = size.width
        val cr = androidx.compose.ui.geometry.CornerRadius(w * 0.08f)
        drawRoundRect(color, topLeft = Offset(w * 0.16f, w * 0.2f), size = Size(w * 0.68f, w * 0.74f), cornerRadius = cr, style = Stroke(width = w * 0.09f))
        drawRoundRect(color, topLeft = Offset(w * 0.36f, w * 0.06f), size = Size(w * 0.28f, w * 0.18f), cornerRadius = cr, style = Stroke(width = w * 0.09f))
        drawLine(color, Offset(w * 0.32f, w * 0.48f), Offset(w * 0.68f, w * 0.48f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.32f, w * 0.66f), Offset(w * 0.68f, w * 0.66f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
    }
}

@Composable
fun WarningIcon(color: Color = AppColors.Red, iconSize: Dp = 40.dp) {
    CanvasIcon(iconSize) {
        val w = size.width; val h = size.height
        val tri = Path()
        tri.moveTo(w * 0.5f, h * 0.1f)
        tri.lineTo(w * 0.95f, h * 0.85f)
        tri.lineTo(w * 0.05f, h * 0.85f)
        tri.close()
        drawPath(tri, color, style = Stroke(width = w * 0.08f, cap = StrokeCap.Round))
        drawLine(color, Offset(w * 0.5f, h * 0.36f), Offset(w * 0.5f, h * 0.6f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
        drawCircle(color, w * 0.045f, Offset(w * 0.5f, h * 0.72f))
    }
}

@Composable
fun GridIcon(color: Color = AppColors.White, iconSize: Dp = 22.dp) {
    CanvasIcon(iconSize) {
        val w = size.width; val cw = w * 0.24f; val gap = w * 0.07f
        for (r in 0..2) for (c in 0..2) {
            drawRect(
                color, topLeft = Offset(gap + c * (cw + gap), gap + r * (cw + gap)),
                size = Size(cw, cw), style = Stroke(width = w * 0.06f)
            )
        }
    }
}

@Composable
fun ListIcon(color: Color = AppColors.White, iconSize: Dp = 22.dp) {
    CanvasIcon(iconSize) {
        val w = size.width
        for (i in 0..2) {
            val y = w * (0.2f + i * 0.3f)
            drawCircle(color, w * 0.05f, Offset(w * 0.12f, y))
            drawLine(color, Offset(w * 0.28f, y), Offset(w * 0.88f, y), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
        }
    }
}

@Composable
fun TrophyIcon(color: Color = AppColors.White, iconSize: Dp = 22.dp) {
    CanvasIcon(iconSize) {
        val w = size.width; val h = size.height
        drawRoundRect(
            color, topLeft = Offset(w * 0.26f, h * 0.08f), size = Size(w * 0.48f, h * 0.42f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.06f)
        )
        drawArc(color, -90f, 180f, false, topLeft = Offset(w * 0.06f, h * 0.12f), size = Size(w * 0.24f, h * 0.28f), style = Stroke(width = w * 0.08f))
        drawArc(color, -90f, -180f, false, topLeft = Offset(w * 0.7f, h * 0.12f), size = Size(w * 0.24f, h * 0.28f), style = Stroke(width = w * 0.08f))
        drawRect(color, topLeft = Offset(w * 0.45f, h * 0.5f), size = Size(w * 0.1f, h * 0.24f))
        drawRect(color, topLeft = Offset(w * 0.3f, h * 0.74f), size = Size(w * 0.4f, h * 0.1f))
    }
}

@Composable
fun XMarkIcon(color: Color = AppColors.Red, iconSize: Dp = 18.dp, strokeScale: Float = 0.14f) {
    CanvasIcon(iconSize) {
        val w = size.width; val sw = w * strokeScale
        drawLine(color, Offset(w * 0.15f, w * 0.15f), Offset(w * 0.85f, w * 0.85f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.85f, w * 0.15f), Offset(w * 0.15f, w * 0.85f), strokeWidth = sw, cap = StrokeCap.Round)
    }
}

@Composable
fun TapIcon(color: Color = AppColors.Gray, iconSize: Dp = 18.dp) {
    CanvasIcon(iconSize) {
        val w = size.width
        drawCircle(color, w * 0.16f, Offset(w * 0.5f, w * 0.42f))
        drawCircle(color, w * 0.3f, Offset(w * 0.5f, w * 0.42f), style = Stroke(width = w * 0.08f))
        drawLine(color, Offset(w * 0.5f, w * 0.72f), Offset(w * 0.5f, w * 0.92f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
    }
}

// ---------------- Buttons ----------------
@Composable
fun MenuButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null
) {
    val bg = if (primary) Color(0xFF000000) else AppColors.Btn
    val borderColor = if (primary) Color(0xFF1F1F1F) else AppColors.BtnBorder
    val textColor = if (enabled) AppColors.White else AppColors.DimGray
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) bg else bg.copy(alpha = 0.6f))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) {
                icon()
                Spacer(Modifier.width(10.dp))
            }
            Text(text, color = textColor, fontWeight = FontWeight.Bold, fontSize = 17.sp, letterSpacing = 1.sp)
        }
    }
}

@Composable
fun ScreenHeader(title: String, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(44.dp)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) { BackArrowIcon() }
        Text(
            title,
            modifier = Modifier.align(Alignment.Center),
            color = AppColors.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, letterSpacing = 1.5.sp
        )
    }
}

@Composable
fun CardBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(AppColors.Card)
            .border(1.dp, AppColors.CardBorder, RoundedCornerShape(24.dp))
    ) { content() }
}
