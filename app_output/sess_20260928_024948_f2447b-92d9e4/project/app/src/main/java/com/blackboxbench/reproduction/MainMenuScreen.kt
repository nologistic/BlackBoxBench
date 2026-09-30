package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

val BgColor = Color(0xFF0E0E12)
val PanelColor = Color(0xFF1D1D24)
val AccentGreen = Color(0xFF4CAF50)
val FillYellow = Color(0xFFFFD54F)
val MarkRed = Color(0xFFEF5350)
val HeartRed = Color(0xFFE53950)
val StarYellow = Color(0xFFFFC63D)
val DimText = Color(0xFF9A9AA5)
val CellEmpty = Color(0xFF232329)
val BorderSoft = Color(0xFF2C2C35)

@Composable
fun MenuButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(PanelColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
    }
}

@Composable
fun StarCount(count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(22.dp)) {
            drawStar(StarYellow, Offset(size.width / 2, size.height / 2), size.minDimension * 0.45f)
        }
        Spacer(Modifier.width(6.dp))
        Text("$count", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun HeartsCount(count: Int) {
    Canvas(Modifier.size(24.dp)) {
        drawHeart(HeartRed, Offset(size.width / 2, size.height / 2), size.minDimension * 0.9f)
    }
    Spacer(Modifier.width(6.dp))
    Text("$count", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
}

@Composable
fun LevelPill(level: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF17171D))
            .border(1.dp, AccentGreen, RoundedCornerShape(20.dp))
            .padding(horizontal = 22.dp, vertical = 8.dp)
    ) {
        Text("LEVEL $level", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
fun MainMenuScreen(progress: GameProgress, nav: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .statusBarsPadding()
    ) {
        // Top HUD: stars | level pill | hearts
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StarCount(progress.stars)
            Spacer(Modifier.weight(1f))
            LevelPill(progress.currentLevel)
            Spacer(Modifier.weight(1f))
            HeartsCount(progress.hearts)
        }

        Spacer(Modifier.height(90.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("NONOGRAM", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
            Spacer(Modifier.height(6.dp))
            Text("PICTURE LOGIC PUZZLE", color = DimText, fontSize = 14.sp, letterSpacing = 5.sp)
        }

        Spacer(Modifier.weight(0.6f))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MenuButton("PLAY") { nav.navigate("game/${progress.currentLevel}") }
            MenuButton("SELECT LEVEL") { nav.navigate("levels") }
            MenuButton("RANDOM PUZZLE") { nav.navigate("random") }
            MenuButton("MULTIPLAYER") { nav.navigate("multiplayer") }
            MenuButton("HOW TO PLAY") { nav.navigate("howto") }
            MenuButton("SETTINGS") { nav.navigate("settings") }
        }

        Spacer(Modifier.weight(1f))
    }
}
