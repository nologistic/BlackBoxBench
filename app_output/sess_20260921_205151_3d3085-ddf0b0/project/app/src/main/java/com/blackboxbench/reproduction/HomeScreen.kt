package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(onOpenFile: (String) -> Unit, onOpenSettings: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NavyBar)
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                BottomTab("📁", "文件", tab == 0) { tab = 0 }
                BottomTab("☑", "To-Do", tab == 1) { tab = 1 }
                BottomTab("⚡", "QuickNote", tab == 2) { tab = 2 }
                BottomTab("♥", "更多", tab == 3) { tab = 3 }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                0 -> FilesTab(onOpenFile = onOpenFile, onOpenSettings = onOpenSettings)
                1 -> TodoTab()
                2 -> QuickNoteTab()
                3 -> MoreTab(onOpenSettings = onOpenSettings)
            }
        }
    }
}

@Composable
private fun BottomTab(icon: String, label: String, active: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(icon, fontSize = 20.sp, color = if (active) MarkorRed else Color.White)
        Text(
            label,
            fontSize = 12.sp,
            color = if (active) MarkorRed else Color.White,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
