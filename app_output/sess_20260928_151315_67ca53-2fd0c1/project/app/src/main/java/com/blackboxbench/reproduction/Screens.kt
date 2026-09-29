package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

// ---------------- Menu ----------------
@Composable
fun MenuScreen(nav: NavHostController) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    val backEntry by nav.currentBackStackEntryAsState()
    var level by remember { mutableIntStateOf(prefs.currentLevel) }
    var showDifficulty by remember { mutableStateOf(false) }
    LaunchedEffect(backEntry) { level = prefs.currentLevel }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(28.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AppColors.Btn)
                    .border(1.dp, AppColors.BtnBorder, CircleShape)
                    .clickable { openBrowser(context, "https://play.google.com/store/apps") },
                contentAlignment = Alignment.Center
            ) { StarIcon() }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, AppColors.BtnBorder, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "LEVEL $level",
                    color = AppColors.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 2.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AppColors.Btn)
                    .border(1.dp, AppColors.BtnBorder, CircleShape)
                    .clickable { openBrowser(context, "https://www.buymeacoffee.com") },
                contentAlignment = Alignment.Center
            ) { HeartIcon() }
        }
        Spacer(Modifier.height(36.dp))
        Text(
            "NONOGRAM",
            color = AppColors.White, fontWeight = FontWeight.ExtraBold, fontSize = 38.sp, letterSpacing = 5.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "PICTURE LOGIC PUZZLE",
            color = AppColors.Gray, fontSize = 13.sp, letterSpacing = 3.sp
        )
        Spacer(Modifier.height(32.dp))
        MenuButton("PLAY", primary = true, onClick = {
            val ip = prefs.loadInProgress()
            if (ip != null && ip.mode.startsWith("camp")) {
                nav.navigate("game/${ip.mode}")
            } else {
                nav.navigate("game/camp_${prefs.currentLevel}")
            }
        })
        Spacer(Modifier.height(14.dp))
        MenuButton("SELECT LEVEL", onClick = { nav.navigate("levels") })
        Spacer(Modifier.height(14.dp))
        MenuButton("RANDOM PUZZLE", onClick = { showDifficulty = true })
        Spacer(Modifier.height(14.dp))
        MenuButton("MULTIPLAYER", onClick = { nav.navigate("multiplayer") })
        Spacer(Modifier.height(14.dp))
        MenuButton("HOW TO PLAY", onClick = { nav.navigate("howto") })
        Spacer(Modifier.height(14.dp))
        MenuButton("SETTINGS", onClick = { nav.navigate("settings") })
    }

    if (showDifficulty) {
        Dialog(onDismissRequest = { showDifficulty = false }) {
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
                    "CHOOSE DIFFICULTY",
                    color = AppColors.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, letterSpacing = 2.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "PLAY A DYNAMICALLY GENERATED NONOGRAM PUZZLE.",
                    color = AppColors.Gray, fontSize = 12.sp, textAlign = TextAlign.Center, letterSpacing = 1.sp
                )
                Spacer(Modifier.height(20.dp))
                DIFFICULTIES.forEachIndexed { i, d ->
                    if (i > 0) Spacer(Modifier.height(10.dp))
                    MenuButton(d, primary = (d == "EASY"), onClick = {
                        showDifficulty = false
                        nav.navigate("game/rand_${d}_${kotlin.random.Random.nextLong(1000000L)}")
                    })
                }
                Spacer(Modifier.height(10.dp))
                MenuButton("CANCEL", onClick = { showDifficulty = false })
            }
        }
    }
}

// ---------------- Level select ----------------
@Composable
fun LevelSelectScreen(nav: NavHostController) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    val backEntry by nav.currentBackStackEntryAsState()
    var current by remember { mutableIntStateOf(prefs.currentLevel) }
    var done by remember { mutableStateOf(prefs.completed) }
    LaunchedEffect(backEntry) { current = prefs.currentLevel; done = prefs.completed }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        ScreenHeader("SELECT LEVEL") { nav.popBackStack() }
        Spacer(Modifier.height(24.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(CAMPAIGN_LEVELS) { i ->
                val lv = i + 1
                val isDone = done.contains(lv)
                val isOpen = lv <= current
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isOpen) AppColors.Btn else Color(0xFF141414))
                        .border(
                            1.5.dp,
                            if (isDone) AppColors.White else AppColors.BtnBorder,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable(enabled = isOpen) { nav.navigate("game/camp_$lv") },
                    contentAlignment = Alignment.Center
                ) {
                    if (isOpen) {
                        Text(
                            "$lv",
                            color = if (isDone) AppColors.White else AppColors.Gray,
                            fontWeight = FontWeight.Bold, fontSize = 22.sp
                        )
                        if (isDone) {
                            Box(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)) {
                                CheckIcon(iconSize = 12.dp)
                            }
                        }
                    } else {
                        LockIcon()
                    }
                }
            }
        }
    }
}

// ---------------- How to play ----------------
@Composable
fun HowToPlayScreen(nav: NavHostController) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        ScreenHeader("HOW TO PLAY") { nav.popBackStack() }
        Spacer(Modifier.height(24.dp))
        val items = listOf(
            Triple("1. REVEAL THE PICTURE", "Fill cells to uncover a hidden pixel picture. Every puzzle has exactly one solution.", "grid"),
            Triple("2. READ THE CLUES", "Numbers on each row and column tell you how many consecutive cells to fill.", "list"),
            Triple("3. MARK BLANK SPACES", "Use X to mark cells you know must stay empty. Crosses do not count as moves.", "x"),
            Triple("4. COMPLETE THE GRID", "Fill every solution cell to complete the puzzle and win the level.", "trophy")
        )
        items.forEachIndexed { i, (title, desc, icon) ->
            if (i > 0) Spacer(Modifier.height(14.dp))
            CardBox {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(AppColors.Btn)
                            .border(1.dp, AppColors.BtnBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        when (icon) {
                            "grid" -> GridIcon()
                            "list" -> ListIcon()
                            "x" -> XMarkIcon(iconSize = 20.dp)
                            else -> TrophyIcon()
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(title, color = AppColors.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 1.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(desc, color = AppColors.Gray, fontSize = 13.sp, lineHeight = 18.sp)
                    }
                }
            }
        }
    }
}

// ---------------- Settings ----------------
@Composable
fun SettingsScreen(nav: NavHostController) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    var haptic by remember { mutableStateOf(prefs.haptic) }
    var longPress by remember { mutableStateOf(prefs.longPressCross) }
    var cycle by remember { mutableStateOf(prefs.cycleMode) }
    var showReset by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        ScreenHeader("SETTINGS") { nav.popBackStack() }
        Spacer(Modifier.height(24.dp))
        CardBox {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                @Composable
                fun toggleRow(label: String, desc: String, value: Boolean, onChange: (Boolean) -> Unit) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(label, color = AppColors.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 0.5.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(desc, color = AppColors.Gray, fontSize = 12.sp)
                        }
                        Switch(
                            checked = value,
                            onCheckedChange = onChange,
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = AppColors.White,
                                checkedThumbColor = Color.Black,
                                uncheckedTrackColor = AppColors.Btn,
                                uncheckedThumbColor = AppColors.Gray
                            )
                        )
                    }
                }
                toggleRow("HAPTIC FEEDBACK", "Vibrate on cell actions", haptic) {
                    haptic = it; prefs.haptic = it
                }
                toggleRow("LONG PRESS TO CROSS", "Long press a cell to mark it with an X", longPress) {
                    longPress = it; prefs.longPressCross = it
                }
                toggleRow("CYCLE MODE", "Tap a cell to cycle empty, fill, cross", cycle) {
                    cycle = it; prefs.cycleMode = it
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        MenuButton("RESET PROGRESS", onClick = { showReset = true })
    }

    if (showReset) {
        Dialog(onDismissRequest = { showReset = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(AppColors.Card)
                    .border(1.dp, AppColors.CardBorder, RoundedCornerShape(24.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                WarningIcon()
                Spacer(Modifier.height(16.dp))
                Text(
                    "RESET PROGRESS?",
                    color = AppColors.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, letterSpacing = 2.sp
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "ARE YOU SURE YOU WANT TO RESET ALL PROGRESS? THIS CANNOT BE UNDONE.",
                    color = AppColors.Gray, fontSize = 12.sp, textAlign = TextAlign.Center, letterSpacing = 1.sp
                )
                Spacer(Modifier.height(22.dp))
                MenuButton("RESET PROGRESS", primary = true, onClick = {
                    prefs.resetProgress()
                    showReset = false
                })
                Spacer(Modifier.height(10.dp))
                MenuButton("CANCEL", onClick = { showReset = false })
            }
        }
    }
}
