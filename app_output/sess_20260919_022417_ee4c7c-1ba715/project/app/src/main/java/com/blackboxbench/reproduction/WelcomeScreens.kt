package com.blackboxbench.reproduction

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WelcomeScreen(
    onAddAccount: () -> Unit,
    onContinueOffline: () -> Unit,
    onImportBackup: () -> Unit
) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(24.dp)) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            LogoCheck(size = 160.dp)
        }
        Button(
            onClick = onAddAccount,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF35618E))
        ) { Text("添加账号", fontSize = 16.sp) }
        Spacer(Modifier.height(14.dp))
        OutlinedButton(
            onClick = onContinueOffline,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp)
        ) { Text("继续但不同步", fontSize = 16.sp) }
        Spacer(Modifier.height(14.dp))
        OutlinedButton(
            onClick = onImportBackup,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp)
        ) { Text("导入 Tasks.org 备份", fontSize = 16.sp) }
        Spacer(Modifier.height(24.dp))
    }
}
