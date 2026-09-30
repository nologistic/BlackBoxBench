package com.blackboxbench.reproduction

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun HowToPlayScreen(nav: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        BackBar("HOW TO PLAY") { nav.popBackStack() }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HowToStep("1", "Numbers are clues.",
                "The numbers next to each row and above each column tell you how many cells to fill in that line.")
            HowToStep("2", "Fill the cells.",
                "Select the FILL tool and tap a cell to fill it. Tap it again to clear it.")
            HowToStep("3", "Mark the blanks.",
                "Switch to the X tool to mark cells you know are empty. This helps you keep track.")
            HowToStep("4", "Complete the picture.",
                "When every row and column matches its clues, the hidden picture is revealed and the level is complete!")
            Spacer(Modifier.height(24.dp))
            MenuButton("GOT IT") { nav.popBackStack() }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun HowToStep(num: String, title: String, body: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(PanelColor)
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(AccentGreen),
            contentAlignment = Alignment.Center
        ) {
            Text(num, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(Modifier.height(4.dp))
            Text(body, color = DimText, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
fun SettingsScreen(progress: GameProgress, nav: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .statusBarsPadding()
    ) {
        BackBar("SETTINGS") { nav.popBackStack() }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingToggle("SOUND", progress.soundOn) { progress.soundOn = it }
            SettingToggle("HAPTICS", progress.hapticsOn) { progress.hapticsOn = it }
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(PanelColor)
                    .clickable {
                        progress.reset()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("RESET PROGRESS", color = MarkRed, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                "NONOGRAM v1.0 — a black-box reproduction build.",
                color = Color(0xFF5A5A64),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun SettingToggle(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PanelColor)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .width(58.dp)
                .height(30.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(if (value) AccentGreen else Color(0xFF3A3A44))
                .clickable { onChange(!value) },
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .padding(3.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

@Composable
fun MultiplayerScreen(nav: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .statusBarsPadding()
    ) {
        BackBar("MULTIPLAYER") { nav.popBackStack() }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(60.dp))
            Text("🌐", color = Color.White, fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text("MULTIPLAYER", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            Text(
                "Challenge other players in real-time nonogram duels.\n\nAn internet connection is required to play multiplayer.",
                color = DimText,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            MenuButton("BACK") { nav.popBackStack() }
        }
    }
}
