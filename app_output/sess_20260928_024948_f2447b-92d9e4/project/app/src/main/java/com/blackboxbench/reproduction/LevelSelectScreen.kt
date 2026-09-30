package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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

@Composable
fun BackBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(26.dp)) {
                drawBackArrow(Color.White, Offset(size.width / 2, size.height / 2), size.minDimension * 0.8f)
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
    }
}

@Composable
fun LevelSelectScreen(progress: GameProgress, nav: NavController) {
    val levels = (1..Puzzles.levelCount()).toList()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .statusBarsPadding()
    ) {
        BackBar("SELECT LEVEL") { nav.popBackStack() }
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(levels) { level ->
                LevelTile(
                    level = level,
                    unlocked = level <= progress.unlocked,
                    onClick = { nav.navigate("game/$level") }
                )
            }
        }
    }
}

@Composable
fun LevelTile(level: Int, unlocked: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (unlocked) PanelColor else Color(0xFF16161B))
            .clickable(enabled = unlocked) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (unlocked) {
            Text("$level", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Canvas(Modifier.size(24.dp)) {
                    drawLock(DimText, Offset(size.width / 2, size.height / 2), size.minDimension * 0.8f)
                }
                Spacer(Modifier.height(4.dp))
                Text("$level", color = DimText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
