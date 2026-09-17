package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun SettingsScreen(prefs: Prefs, onBack: () -> Unit, onOpenAppearance: () -> Unit) {
    var vibrate by remember { mutableStateOf(prefs.vibrate) }
    var keepAwake by remember { mutableStateOf(prefs.keepAwake) }
    var language by remember { mutableStateOf(prefs.language) }
    var showLanguage by remember { mutableStateOf(false) }
    var showWidgetColor by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(PageBg).verticalScroll(rememberScrollState())) {
        ScreenTitle("设置", onBack)
        SectionLabel("外观")
        SettingRow("自定义外观", onClick = onOpenAppearance)
        SettingRow("自定义微件颜色", onClick = { showWidgetColor = true })
        Spacer(Modifier.height(8.dp))
        Divider()
        SectionLabel("常规")
        SettingRow("购买 Fossify Thank You", onClick = {})
        SettingRow("语言", subtitle = language, onClick = { showLanguage = true })
        SettingRow("按下按钮时振动", trailing = {
            ToggleSwitch(vibrate) { vibrate = it; prefs.vibrate = it }
        })
        SettingRow("应用在前台运行时，阻止设备自动休眠", trailing = {
            ToggleSwitch(keepAwake) { keepAwake = it; prefs.keepAwake = it }
        })
        Spacer(Modifier.height(24.dp))
    }

    if (showLanguage) {
        SimpleChoiceDialog(
            title = "语言",
            options = listOf("中文", "English"),
            selected = language,
            onPick = { language = it; prefs.language = it; showLanguage = false },
            onDismiss = { showLanguage = false }
        )
    }
    if (showWidgetColor) {
        ColorPickerDialog(
            initial = prefs.widgetColor,
            onConfirm = { prefs.widgetColor = it; showWidgetColor = false },
            onDismiss = { showWidgetColor = false }
        )
    }
}

@Composable
fun AppearanceScreen(prefs: Prefs, onBack: () -> Unit) {
    var theme by remember { mutableStateOf(prefs.theme) }
    var font by remember { mutableStateOf(prefs.font) }
    var iconColor by remember { mutableStateOf(prefs.iconColor) }
    var showTheme by remember { mutableStateOf(false) }
    var showFont by remember { mutableStateOf(false) }
    var showColor by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(PageBg).verticalScroll(rememberScrollState())) {
        ScreenTitle("自定义外观", onBack)
        SectionLabel("主题和颜色")
        SettingRow("应用主题", subtitle = theme, onClick = { showTheme = true })
        SettingRow("应用图标颜色", trailing = {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(iconColor))
            )
        }, onClick = { showColor = true })
        Spacer(Modifier.height(8.dp))
        Divider()
        SectionLabel("字体")
        SettingRow("应用字体", subtitle = font, onClick = { showFont = true })
        Spacer(Modifier.height(24.dp))
    }

    if (showTheme) {
        SimpleChoiceDialog(
            title = "应用主题",
            options = listOf("系统默认", "浅色", "深色"),
            selected = theme,
            onPick = { theme = it; prefs.theme = it; showTheme = false },
            onDismiss = { showTheme = false }
        )
    }
    if (showFont) {
        SimpleChoiceDialog(
            title = "应用字体",
            options = listOf("系统默认", "衬线", "等宽"),
            selected = font,
            onPick = { font = it; prefs.font = it; showFont = false },
            onDismiss = { showFont = false }
        )
    }
    if (showColor) {
        ColorPickerDialog(
            initial = iconColor,
            onConfirm = { iconColor = it; prefs.iconColor = it; showColor = false },
            onDismiss = { showColor = false }
        )
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(PageBg).verticalScroll(rememberScrollState())) {
        ScreenTitle("关于", onBack)
        SectionLabel("支持")
        SettingRow("常见问题")
        SettingRow("已知问题")
        SettingRow("hello@fossify.org")
        Spacer(Modifier.height(8.dp))
        Divider()
        SectionLabel("帮助我们")
        SettingRow("分享给好友")
        SettingRow("贡献者")
        SettingRow("向 Fossify 捐赠")
        Spacer(Modifier.height(8.dp))
        Divider()
        SectionLabel("社交网络")
        SettingRow("GitHub")
        SettingRow("Reddit")
        SettingRow("Telegram")
        Spacer(Modifier.height(8.dp))
        Divider()
        SectionLabel("其他")
        SettingRow("隐私政策")
        SettingRow("第三方许可")
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun SimpleChoiceDialog(
    title: String,
    options: List<String>,
    selected: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFFF2F1FA))
                .padding(vertical = 18.dp)
        ) {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    title,
                    color = InkSoft,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(Modifier.height(12.dp))
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    options.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(option) }
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioDot(option == selected)
                            Spacer(Modifier.width(20.dp))
                            Text(option, color = Ink, fontSize = 20.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ColorPickerDialog(
    initial: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val initialColor = Color(initial)
    var hue by remember { mutableFloatStateOf(initialColor.hsvHue) }
    var sat by remember { mutableFloatStateOf(if (initialColor.hsvSaturation == 0f) 1f else initialColor.hsvSaturation) }
    var value by remember { mutableFloatStateOf(if (initialColor.hsvValue == 0f) 1f else initialColor.hsvValue) }
    val current = Color.hsv(hue, sat, value)

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFFF2F1FA))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(current),
                    contentAlignment = Alignment.Center
                ) {
                    Text("=", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                hue = (offset.x / size.width * 360f).coerceIn(0f, 359.9f)
                            }
                        }
                ) {
                    Canvas(Modifier.fillMaxWidth().height(40.dp)) {
                        val colors = (0..36).map { i ->
                            Color.hsv(i / 36f * 359.9f, 1f, 1f)
                        }
                        drawRect(
                            brush = Brush.horizontalGradient(colors = colors)
                        )
                        val x = hue / 359.9f * size.width
                        drawRect(
                            color = Color(0xFF38802A),
                            topLeft = Offset(x - 6f, -6f),
                            size = androidx.compose.ui.geometry.Size(12f, size.height + 12f)
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text(toHex(current), color = Ink, fontSize = 20.sp)
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "取消",
                        color = Color(0xFF415F91),
                        fontSize = 20.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onDismiss() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                    Text(
                        "确定",
                        color = Color(0xFF415F91),
                        fontSize = 20.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onConfirm(android.graphics.Color.argb((current.alpha * 255).toInt(), (current.red * 255).toInt(), (current.green * 255).toInt(), (current.blue * 255).toInt())) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

private fun Color.toArgbInt(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(),
    (red * 255).toInt(),
    (green * 255).toInt(),
    (blue * 255).toInt()
)

private val Color.hsvHue: Float
    get() {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(toArgbInt(), hsv)
        return hsv[0]
    }

private val Color.hsvSaturation: Float
    get() {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(toArgbInt(), hsv)
        return hsv[1]
    }

private val Color.hsvValue: Float
    get() {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(toArgbInt(), hsv)
        return hsv[2]
    }