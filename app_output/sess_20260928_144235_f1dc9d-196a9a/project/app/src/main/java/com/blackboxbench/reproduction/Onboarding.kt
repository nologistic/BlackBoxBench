package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OnboardingFlow(onDone: () -> Unit, onOpenFolders: () -> Unit) {
    val p = LocalVlcPalette.current
    var step by remember { mutableStateOf(0) }
    var permissionChoice by remember { mutableStateOf(2) }
    var autoScan by remember { mutableStateOf(true) }
    var themeChoice by remember { mutableStateOf(0) }
    var runtimeDialog by remember { mutableStateOf(0) } // 1 audio, 2 photos

    Box(Modifier.fillMaxSize().background(p.background)) {
        Column(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (step) {
                    0 -> WelcomePage()
                    1 -> PermissionChoicePage(permissionChoice) { permissionChoice = it }
                    2 -> AutoScanPage(autoScan, { autoScan = it }, onOpenFolders)
                    3 -> NotificationPage()
                    else -> ThemePage(themeChoice) { themeChoice = it }
                }
            }
            OnboardingButtons(
                step = step,
                onSkip = {
                    Store.onboardingDone.value = true
                    Store.persist()
                    onDone()
                },
                onNext = {
                    if (step == 1 && permissionChoice > 0) {
                        runtimeDialog = 1
                    } else if (step < 4) {
                        step++
                    } else {
                        Store.themeMode.value = themeChoice
                        Store.onboardingDone.value = true
                        Store.persist()
                        onDone()
                    }
                },
            )
        }
        if (runtimeDialog == 1) {
            PermissionDialog(
                text = "要允许“VLC”访问此设备上的音乐和音频吗？",
                onAllow = { runtimeDialog = 2 },
                onDeny = { runtimeDialog = 0 },
            )
        } else if (runtimeDialog == 2) {
            PermissionDialog(
                text = "要允许“VLC”访问您的照片和视频吗？",
                onAllow = { runtimeDialog = 0; step = 2 },
                onDeny = { runtimeDialog = 0; step = 2 },
                threeOptions = true,
            )
        }
    }
}

@Composable
private fun PermissionDialog(
    text: String,
    onAllow: () -> Unit,
    onDeny: () -> Unit,
    threeOptions: Boolean = false,
) {
    VlcDialog(onDismiss = onDeny) {
        Text(
            text,
            color = LocalVlcPalette.current.textPrimary,
            fontSize = 17.sp,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
        )
        Column(Modifier.fillMaxWidth()) {
            listOf(
                "允许" to onAllow,
                "不允许" to onDeny,
            ).forEach { (label, action) ->
                Text(
                    label,
                    color = LocalVlcPalette.current.accent,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = action,
                        )
                        .padding(horizontal = 22.dp, vertical = 14.dp),
                )
            }
        }
    }
}

@Composable
private fun WelcomePage() {
    val p = LocalVlcPalette.current
    Spacer(Modifier.height(120.dp))
    VlcCone(140.dp, p.accent)
    Spacer(Modifier.height(36.dp))
    Text("VLC for Android", color = p.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(18.dp))
    Text(
        "VLC 是一款自由、开源的跨平台多媒体播放器，可播放大多数多媒体文件、" +
            "光盘、设备以及网络串流。",
        color = p.textSecondary,
        fontSize = 15.sp,
        modifier = Modifier.padding(horizontal = 36.dp),
    )
}

@Composable
private fun PermissionChoicePage(choice: Int, onChoice: (Int) -> Unit) {
    val p = LocalVlcPalette.current
    Spacer(Modifier.height(70.dp))
    Text("授予权限", color = p.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(8.dp))
    Text(
        "为了扫描设备上的媒体，VLC 需要文件访问权限",
        color = p.textSecondary,
        fontSize = 14.sp,
        modifier = Modifier.padding(horizontal = 36.dp),
    )
    Spacer(Modifier.height(40.dp))
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PermissionShield(choice == 0, { onChoice(0) }, "无")
        PermissionShield(choice == 1, { onChoice(1) }, "媒体")
        PermissionShield(choice == 2, { onChoice(2) }, "全部")
    }
    Spacer(Modifier.height(34.dp))
    val description = when (choice) {
        0 -> "您将无法自动扫描媒体文件。您将只能播放串流或网络媒体。"
        1 -> "您将只能自动扫描标准格式的媒体文件。"
        else -> "您将可以自动扫描设备上的所有媒体文件。"
    }
    Text(
        description,
        color = p.textPrimary,
        fontSize = 15.sp,
        modifier = Modifier.padding(horizontal = 36.dp),
    )
}

@Composable
private fun PermissionShield(selected: Boolean, onClick: () -> Unit, label: String) {
    val p = LocalVlcPalette.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(74.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (selected) p.accent.copy(alpha = 0.14f) else p.chip)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            VlcIconView(if (selected) VlcIcon.LOCK else VlcIcon.WARNING, 34.dp, if (selected) p.accent else p.textSecondary)
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = if (selected) p.accent else p.textSecondary, fontSize = 13.sp)
    }
}

@Composable
private fun AutoScanPage(autoScan: Boolean, onToggle: (Boolean) -> Unit, onCustomize: () -> Unit) {
    val p = LocalVlcPalette.current
    Spacer(Modifier.height(80.dp))
    Text("自动扫描媒体文件", color = p.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(44.dp))
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("让 VLC 扫描我的设备，寻找媒体内容", color = p.textPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f))
        VlcSwitch(autoScan, onToggle)
    }
    Spacer(Modifier.height(30.dp))
    OutlinedAction("自定义", onCustomize)
}

@Composable
private fun NotificationPage() {
    val p = LocalVlcPalette.current
    Spacer(Modifier.height(80.dp))
    Text("通知权限", color = p.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(24.dp))
    Text(
        "VLC 需要您的授权，方可发送通知。应用只会在为您扫描媒体文件或者自定义「画中画」视频模式时向您发送通知。",
        color = p.textPrimary,
        fontSize = 15.sp,
        modifier = Modifier.padding(horizontal = 32.dp),
    )
    Spacer(Modifier.height(14.dp))
    Text(
        "拒绝该权限不会影响 VLC 的正常运行，只是不显示通知信息而已。",
        color = p.textSecondary,
        fontSize = 14.sp,
        modifier = Modifier.padding(horizontal = 32.dp),
    )
}

@Composable
private fun ThemePage(choice: Int, onChoice: (Int) -> Unit) {
    val p = LocalVlcPalette.current
    Spacer(Modifier.height(74.dp))
    Text("就像在自己家", color = p.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(12.dp))
    Text(
        "VLC 将跟随系统设置自动切换深色模式",
        color = p.textSecondary,
        fontSize = 14.sp,
        modifier = Modifier.padding(horizontal = 32.dp),
    )
    Spacer(Modifier.height(40.dp))
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        ThemeCard(choice == 0, Color.White, "自动") { onChoice(0) }
        ThemeCard(choice == 1, Color(0xFFF7F7F7), "亮色") { onChoice(1) }
        ThemeCard(choice == 2, Color(0xFF161616), "黑色") { onChoice(2) }
    }
}

@Composable
private fun ThemeCard(selected: Boolean, bg: Color, label: String, onClick: () -> Unit) {
    val p = LocalVlcPalette.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(width = 88.dp, height = 70.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(bg)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                )
                .padding(10.dp),
        ) {
            Column {
                Box(Modifier.fillMaxWidth(0.7f).height(6.dp).background(if (bg == Color.White) Color(0xFFCCCCCC) else Color(0xFF444444)))
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth().height(6.dp).background(p.accent))
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth(0.85f).height(6.dp).background(if (bg == Color.White) Color(0xFFCCCCCC) else Color(0xFF444444)))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = if (selected) p.accent else p.textSecondary, fontSize = 13.sp)
    }
}

@Composable
private fun OnboardingButtons(step: Int, onSkip: () -> Unit, onNext: () -> Unit) {
    val p = LocalVlcPalette.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.weight(1f))
        Text(
            "跳过",
            color = p.textSecondary,
            fontSize = 16.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSkip,
                )
                .padding(horizontal = 20.dp, vertical = 14.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            if (step >= 4) "完成" else "下一项",
            color = p.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onNext,
                )
                .padding(horizontal = 20.dp, vertical = 14.dp),
        )
    }
}

@Composable
fun OutlinedAction(text: String, onClick: () -> Unit) {
    val p = LocalVlcPalette.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(p.chip)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 22.dp, vertical = 10.dp),
    ) {
        Text(text, color = p.textPrimary, fontSize = 15.sp)
    }
}
