package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay

fun formatTime(sec: Int): String = String.format("%02d:%02d", sec / 60, sec % 60)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GameScreen(nav: NavHostController, mode: String) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }

    val isCampaign = mode.startsWith("camp_")
    val isRandom = mode.startsWith("rand_")
    val level = if (isCampaign) mode.removePrefix("camp_").toIntOrNull() ?: 1 else 0
    val diff = when {
        isCampaign -> "EASY"
        isRandom -> mode.split("_").getOrNull(1) ?: "EASY"
        else -> mode.removePrefix("multi_").substringBefore("-")
    }
    val seed = when {
        isCampaign -> 0L
        isRandom -> mode.split("_").getOrNull(2)?.toLongOrNull() ?: 0L
        else -> mode.removePrefix("multi_").substringAfter("-").toLongOrNull() ?: 0L
    }
    val solution = remember(mode) {
        if (isCampaign) campaignPuzzle(level) else generatePuzzle(diffSize(diff), seed)
    }
    val n = solution.size
    val rClues = remember(solution) { rowClues(solution) }
    val cClues = remember(solution) { colClues(solution) }

    val restored = remember {
        prefs.loadInProgress()?.takeIf { it.mode == mode && it.cells.length == n * n }
    }
    val cells = remember {
        mutableStateListOf<Int>().apply {
            if (restored != null) restored.cells.forEach { add(it - '0') }
            else repeat(n * n) { add(0) }
        }
    }
    val moves = remember { mutableIntStateOf(restored?.moves ?: 0) }
    val elapsed = remember { mutableIntStateOf(restored?.elapsed ?: 0) }
    val undoStack = remember { mutableStateListOf<Triple<Int, Int, Int>>() }
    val completed = remember { mutableStateOf(false) }
    val hintCell = remember { mutableIntStateOf(-1) }
    val inputMode = remember { mutableIntStateOf(0) }
    val cycleMode = remember { prefs.cycleMode }
    val longPressCross = remember { prefs.longPressCross }

    fun persist() {
        prefs.saveInProgress(
            InProgress(mode, cells.joinToString("") { it.toString() }, moves.intValue, elapsed.intValue)
        )
    }

    fun checkWin(): Boolean {
        for (r in 0 until n) for (c in 0 until n) {
            if (solution[r][c] && cells[r * n + c] != 1) return false
        }
        return true
    }

    fun onWin() {
        completed.value = true
        prefs.clearInProgress()
        if (isCampaign) {
            prefs.completed = prefs.completed + level
            if (level >= prefs.currentLevel && level < CAMPAIGN_LEVELS) prefs.currentLevel = level + 1
        }
    }

    fun applyCell(idx: Int, ns: Int) {
        val prev = cells[idx]
        if (ns == prev) return
        undoStack.add(Triple(idx, prev, moves.intValue))
        cells[idx] = ns
        persist()
        if (checkWin()) onWin()
    }

    fun tapCell(idx: Int) {
        if (completed.value) return
        val prev = cells[idx]
        val ns: Int
        if (cycleMode) {
            ns = (prev + 1) % 3
            if (ns == 1) moves.intValue++
        } else if (inputMode.intValue == 0) {
            ns = if (prev == 1) 0 else 1
            if (ns == 1) moves.intValue++
        } else {
            ns = if (prev == 2) 0 else 2
        }
        applyCell(idx, ns)
    }

    fun crossCell(idx: Int) {
        if (completed.value || cycleMode) return
        val prev = cells[idx]
        applyCell(idx, if (prev == 2) 0 else 2)
    }

    fun undo() {
        val t = undoStack.removeLastOrNull() ?: return
        cells[t.first] = t.second
        moves.intValue = t.third
        completed.value = false
        persist()
    }

    fun restart() {
        for (i in 0 until n * n) cells[i] = 0
        moves.intValue = 0
        elapsed.intValue = 0
        undoStack.clear()
        completed.value = false
        persist()
    }

    LaunchedEffect(completed.value) {
        while (!completed.value) {
            delay(1000)
            elapsed.intValue++
            persist()
        }
    }
    LaunchedEffect(hintCell.intValue) {
        if (hintCell.intValue >= 0) {
            delay(1500)
            hintCell.intValue = -1
        }
    }

    val clueFont = if (n <= 8) 13.sp else 10.sp
    val leftW = if (n <= 8) 64.dp else 52.dp
    val topH = if (n <= 8) 76.dp else 60.dp

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Box(
                modifier = Modifier.align(Alignment.CenterStart).size(44.dp).clickable { nav.popBackStack() },
                contentAlignment = Alignment.Center
            ) { BackArrowIcon() }
            if (isCampaign) {
                Text(
                    "LEVEL $level",
                    modifier = Modifier.align(Alignment.Center),
                    color = AppColors.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, letterSpacing = 2.sp
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ClockIcon()
            Spacer(Modifier.width(6.dp))
            Text(formatTime(elapsed.intValue), color = AppColors.Gray, fontSize = 14.sp)
            Spacer(Modifier.width(20.dp))
            Text(
                "MOVES: ${moves.intValue}",
                color = AppColors.Gray, fontSize = 14.sp, letterSpacing = 1.sp
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val undoEnabled = undoStack.isNotEmpty()
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.Btn)
                    .border(1.dp, AppColors.BtnBorder, RoundedCornerShape(12.dp))
                    .clickable(enabled = undoEnabled) { undo() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UndoIcon(color = if (undoEnabled) AppColors.White else AppColors.DimGray)
                    Spacer(Modifier.width(6.dp))
                    Text("UNDO", color = if (undoEnabled) AppColors.White else AppColors.DimGray, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.sp)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.Btn)
                    .border(1.dp, AppColors.BtnBorder, RoundedCornerShape(12.dp))
                    .clickable {
                        for (i in 0 until n * n) {
                            val r = i / n; val c = i % n
                            if (solution[r][c] && cells[i] != 1) { hintCell.intValue = i; break }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BulbIcon()
                    Spacer(Modifier.width(6.dp))
                    Text("HINT", color = AppColors.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.sp)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.Btn)
                    .border(1.dp, AppColors.BtnBorder, RoundedCornerShape(12.dp))
                    .clickable { restart() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RefreshIcon(iconSize = 16.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("RESTART", color = AppColors.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.sp)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth().height(topH)) {
            Spacer(Modifier.width(leftW))
            cClues.forEach { clues ->
                Column(
                    modifier = Modifier.weight(1f).fillMaxSize(),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    clues.forEach { num ->
                        Text("$num", color = AppColors.Gray, fontSize = clueFont, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            for (r in 0 until n) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier.width(leftW),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rClues[r].forEach { num ->
                            Text(
                                "$num",
                                modifier = Modifier.padding(horizontal = 3.dp),
                                color = AppColors.Gray, fontSize = clueFont, fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    for (c in 0 until n) {
                        val idx = r * n + c
                        val state = cells[idx]
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(if (n <= 8) 2.dp else 1.dp)
                                .clip(RoundedCornerShape(if (n <= 8) 5.dp else 3.dp))
                                .background(AppColors.CellBg)
                                .border(
                                    if (hintCell.intValue == idx) 2.dp else 1.dp,
                                    if (hintCell.intValue == idx) AppColors.Yellow else AppColors.CellBorder,
                                    RoundedCornerShape(if (n <= 8) 5.dp else 3.dp)
                                )
                                .combinedClickable(
                                    onClick = { tapCell(idx) },
                                    onLongClick = { if (longPressCross) crossCell(idx) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            when (state) {
                                1 -> Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(if (n <= 8) 3.dp else 2.dp)
                                        .clip(RoundedCornerShape(if (n <= 8) 3.dp else 2.dp))
                                        .background(AppColors.White)
                                )
                                2 -> Canvas(modifier = Modifier.fillMaxSize().padding(if (n <= 8) 4.dp else 3.dp)) {
                                    val sw = size.width * 0.16f
                                    drawLine(AppColors.Red, Offset.Zero, Offset(size.width, size.height), strokeWidth = sw, cap = StrokeCap.Round)
                                    drawLine(AppColors.Red, Offset(size.width, 0f), Offset(0f, size.height), strokeWidth = sw, cap = StrokeCap.Round)
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        if (!cycleMode) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val fillActive = inputMode.intValue == 0
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (fillActive) AppColors.White else AppColors.Btn)
                    .border(1.dp, if (fillActive) AppColors.White else AppColors.BtnBorder, RoundedCornerShape(14.dp))
                    .clickable { inputMode.intValue = 0 },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (fillActive) Color.Black else AppColors.White)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "FILL",
                        color = if (fillActive) Color.Black else AppColors.White,
                        fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 1.sp
                    )
                }
            }
            val crossActive = inputMode.intValue == 1
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (crossActive) AppColors.Red else AppColors.Btn)
                    .border(1.dp, if (crossActive) AppColors.Red else AppColors.BtnBorder, RoundedCornerShape(14.dp))
                    .clickable { inputMode.intValue = 1 },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    XMarkIcon(color = AppColors.White, iconSize = 14.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "CROSS (X)",
                        color = AppColors.White,
                        fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 1.sp
                    )
                }
            }
        }
        }
        Spacer(Modifier.height(20.dp))
    }

    if (completed.value) {
        Dialog(onDismissRequest = {}) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(AppColors.Card)
                    .border(1.dp, AppColors.CardBorder, RoundedCornerShape(24.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "LEVEL COMPLETED!",
                    color = AppColors.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, letterSpacing = 2.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "MOVES: ${moves.intValue}  •  TIME: ${formatTime(elapsed.intValue)}",
                    color = AppColors.Gray, fontSize = 13.sp, letterSpacing = 1.sp
                )
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF101010))
                        .padding(10.dp)
                ) {
                    Column {
                        for (r in 0 until n) {
                            Row {
                                for (c in 0 until n) {
                                    Box(
                                        modifier = Modifier
                                            .size(if (n <= 8) 16.dp else 11.dp)
                                            .padding(1.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(if (solution[r][c]) AppColors.White else Color(0xFF1A1A1A))
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                if (isCampaign) {
                    if (level < CAMPAIGN_LEVELS) {
                        MenuButton("NEXT LEVEL", primary = true, onClick = {
                            nav.navigate("game/camp_${level + 1}") { popUpTo("menu") }
                        })
                        Spacer(Modifier.height(10.dp))
                    }
                } else {
                    MenuButton("PLAY AGAIN", primary = true, onClick = {
                        if (isRandom) {
                            nav.navigate("game/rand_${diff}_${kotlin.random.Random.nextLong(1000000L)}") { popUpTo("menu") }
                        } else {
                            nav.navigate("game/multi_${diff}-${String.format("%06d", seed.toInt())}") { popUpTo("menu") }
                        }
                    })
                    Spacer(Modifier.height(10.dp))
                }
                MenuButton("BUY ME A COFFEE", onClick = {
                    openBrowser(context, "https://www.buymeacoffee.com")
                }, icon = { CoffeeIcon() })
                Spacer(Modifier.height(10.dp))
                MenuButton("HOME", onClick = {
                    if (isCampaign) nav.navigate("levels") { popUpTo("menu") }
                    else nav.popBackStack("menu", inclusive = false)
                }, icon = { HomeIcon() })
            }
        }
    }
}
