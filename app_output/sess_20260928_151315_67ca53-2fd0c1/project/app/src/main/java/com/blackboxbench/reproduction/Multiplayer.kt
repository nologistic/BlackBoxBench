package com.blackboxbench.reproduction

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

// ---------------- Multiplayer ----------------
@Composable
fun MultiplayerScreen(nav: NavHostController) {
    val clipboard = LocalClipboardManager.current
    var diff by remember { mutableStateOf("MEDIUM") }
    var digits by remember { mutableStateOf(String.format("%06d", kotlin.random.Random.nextInt(1000000))) }
    var joinText by remember { mutableStateOf("") }
    var joinError by remember { mutableStateOf(false) }
    val code = "$diff-$digits"
    val codeRegex = remember { Regex("^(EASY|MEDIUM|HARD|MASTER|EXPERT)-\\d{6}$") }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        ScreenHeader("MULTIPLAYER") { nav.popBackStack() }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DIFFICULTIES.take(4).forEach { d ->
                val sel = d == diff
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .background(if (sel) AppColors.White else AppColors.Btn)
                        .border(1.dp, if (sel) AppColors.White else AppColors.BtnBorder, RoundedCornerShape(19.dp))
                        .clickable { diff = d },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (sel) {
                            CheckIcon(color = Color.Black, iconSize = 10.dp)
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            d,
                            color = if (sel) Color.Black else AppColors.Gray,
                            fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        val expertSel = diff == "EXPERT"
        Box(
            modifier = Modifier
                .width(110.dp)
                .height(38.dp)
                .clip(RoundedCornerShape(19.dp))
                .background(if (expertSel) AppColors.White else AppColors.Btn)
                .border(1.dp, if (expertSel) AppColors.White else AppColors.BtnBorder, RoundedCornerShape(19.dp))
                .clickable { diff = "EXPERT" },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (expertSel) {
                    CheckIcon(color = Color.Black, iconSize = 10.dp)
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    "EXPERT",
                    color = if (expertSel) Color.Black else AppColors.Gray,
                    fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        CardBox {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("YOUR ROOM CODE", color = AppColors.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(6.dp))
                Text("SHARE THIS CODE WITH A FRIEND TO PLAY THE SAME PUZZLE.", color = AppColors.Gray, fontSize = 11.sp, letterSpacing = 0.5.sp)
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF101010))
                        .border(1.dp, AppColors.BtnBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        code,
                        modifier = Modifier.weight(1f),
                        color = AppColors.White, fontWeight = FontWeight.Bold, fontSize = 17.sp, letterSpacing = 2.sp
                    )
                    Box(modifier = Modifier.size(30.dp).clickable {
                        digits = String.format("%06d", kotlin.random.Random.nextInt(1000000))
                    }, contentAlignment = Alignment.Center) { RefreshIcon() }
                }
                Spacer(Modifier.height(14.dp))
                MenuButton("COPY CODE", onClick = { clipboard.setText(AnnotatedString(code)) }, icon = { CopyIcon() })
                Spacer(Modifier.height(10.dp))
                MenuButton("START PUZZLE", primary = true, onClick = { nav.navigate("game/multi_$code") })
            }
        }
        Spacer(Modifier.height(16.dp))
        CardBox {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("JOIN WITH CODE", color = AppColors.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(6.dp))
                Text("ENTER A FRIEND'S CODE TO PLAY THEIR PUZZLE.", color = AppColors.Gray, fontSize = 11.sp, letterSpacing = 0.5.sp)
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = joinText,
                    onValueChange = { joinText = it; joinError = false },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("E.g. EASY-123456", color = AppColors.DimGray) },
                    singleLine = true,
                    isError = joinError,
                    trailingIcon = {
                        Box(modifier = Modifier.size(36.dp).clickable {
                            val t = clipboard.getText()?.text
                            if (t != null) { joinText = t; joinError = false }
                        }, contentAlignment = Alignment.Center) { PasteIcon() }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.White,
                        unfocusedBorderColor = AppColors.BtnBorder,
                        errorBorderColor = AppColors.Red,
                        focusedTextColor = AppColors.White,
                        unfocusedTextColor = AppColors.White,
                        cursorColor = AppColors.White,
                        focusedContainerColor = Color(0xFF101010),
                        unfocusedContainerColor = Color(0xFF101010),
                        errorContainerColor = Color(0xFF101010)
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
                if (joinError) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "FORMAT: DIFFICULTY-CODE (E.G. EASY-123456)",
                        color = AppColors.Red, fontSize = 11.sp, letterSpacing = 0.5.sp
                    )
                }
                Spacer(Modifier.height(14.dp))
                MenuButton("JOIN PUZZLE", onClick = {
                    val t = joinText.trim().uppercase()
                    if (codeRegex.matches(t)) {
                        nav.navigate("game/multi_$t")
                    } else {
                        joinError = true
                    }
                })
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
