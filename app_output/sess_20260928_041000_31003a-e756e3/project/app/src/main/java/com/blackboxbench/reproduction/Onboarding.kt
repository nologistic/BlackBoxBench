package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OnboardingScreen(onContinue: () -> Unit, onAddAccount: () -> Unit, onImport: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(96.dp))
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(AppColors.Primary)
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            GlyphIcon(Glyph.Checklist, Color.White, size = 56.dp, stroke = 3.dp)
        }
        Spacer(Modifier.height(28.dp))
        Text(
            "欢迎使用 Tasks",
            fontSize = 26.sp,
            color = AppColors.OnSurface,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "添加账号以在多台设备之间同步您的任务，或在不使用账号的情况下继续。",
            fontSize = 15.sp,
            color = AppColors.SecondaryText,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onAddAccount,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
        ) { Text("添加账号", fontSize = 15.sp, fontWeight = FontWeight.Medium) }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Primary)
        ) { Text("继续但不同步", fontSize = 15.sp, fontWeight = FontWeight.Medium) }
        Spacer(Modifier.height(4.dp))
        TextButton(
            onClick = onImport,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) { Text("导入 Tasks.org 备份", fontSize = 15.sp, color = AppColors.Primary) }
        Spacer(Modifier.height(72.dp))
    }
}

/** Simple list of sync providers, matching the 添加账号 screen. */
@Composable
fun AddAccountScreen(onBack: () -> Unit) {
    val providers = listOf(
        "Tasks.org Cloud" to "好友和家庭共享、邮件转任务",
        "Microsoft To Do" to null,
        "Google Tasks" to null,
        "DAVx⁵" to "与 DAVx⁵ 同步",
        "CalDAV" to "与 CalDAV 同步",
        "EteSync" to "端到端加密",
        "DecSync CC" to "通过 DecSync CC 同步"
    )
    Column(Modifier.fillMaxSize().background(Color.White)) {
        androidx.activity.compose.BackHandler { onBack() }
        Row0(title = "添加账号", onBack = onBack)
        providers.forEach { (name, sub) ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Text(name, fontSize = 16.sp, color = AppColors.OnSurface)
                if (sub != null) Text(sub, fontSize = 12.sp, color = AppColors.SecondaryText)
            }
            SectionDivider()
        }
    }
}

@Composable
private fun Row0(title: String, onBack: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(percent = 50))
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) { CoreIcon(IconBack, AppColors.OnSurface) }
        Text(title, fontSize = 20.sp, color = AppColors.OnSurface)
    }
}
