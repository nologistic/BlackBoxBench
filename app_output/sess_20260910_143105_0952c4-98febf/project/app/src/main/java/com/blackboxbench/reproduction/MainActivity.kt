package com.blackboxbench.reproduction

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val Blue = Color(0xFF69A9EE)
private val Ink = Color(0xFF202124)
private val Panel = Color(0xFF2C2C2F)
private val Muted = Color(0xFFAEB1B5)

private enum class Screen { HOME, SETTINGS, HELP, PICKER, EDITOR, INFO, STACK, QR_SCAN, FOLDER }
private enum class EditorTab { LOOKS, TOOLS }
private data class EditLayer(val name: String, val value: Int)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SnapStudioApp() }
    }
}

@Composable
private fun SnapStudioApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("snapstudio", Context.MODE_PRIVATE) }
    var dark by remember { mutableStateOf(prefs.getBoolean("dark", false)) }
    var size by remember { mutableStateOf(prefs.getString("size", "不要调整大小") ?: "不要调整大小") }
    var format by remember { mutableStateOf(prefs.getString("format", "JPG 95%") ?: "JPG 95%") }
    var screen by remember { mutableStateOf(Screen.HOME) }
    var tutorial by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(EditorTab.LOOKS) }
    val edits = remember { mutableStateListOf<EditLayer>() }
    val redo = remember { mutableStateListOf<EditLayer>() }
    var tool by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var qrDialog by remember { mutableStateOf(false) }
    val scheme = if (dark) darkColorScheme(primary = Blue, background = Ink, surface = Ink)
                 else lightColorScheme(primary = Color(0xFF3977B8), background = Color.White, surface = Color.White)

    MaterialTheme(colorScheme = scheme) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (screen) {
                Screen.HOME -> HomeScreen(dark, tutorial, { tutorial = true }, { tutorial = false },
                    { screen = Screen.SETTINGS }, { screen = Screen.HELP }, { screen = Screen.PICKER })
                Screen.SETTINGS -> SettingsScreen(dark, size, format, { screen = Screen.HOME },
                    {
                        dark = it
                        prefs.edit().putBoolean("dark", it).apply()
                    },
                    {
                        size = it
                        prefs.edit().putString("size", it).apply()
                    },
                    {
                        format = it
                        prefs.edit().putString("format", it).apply()
                    })
                Screen.HELP -> HelpScreen { screen = Screen.HOME }
                Screen.PICKER -> PickerScreen({ screen = Screen.HOME }) { screen = Screen.EDITOR }
                Screen.EDITOR -> EditorScreen(tab, edits, message, { tab = it },
                    { screen = Screen.PICKER }, { screen = Screen.INFO },
                    { tool = it }, tool, { tool = null },
                    { name, value -> edits.add(EditLayer(name, value)); redo.clear(); tool = null; tab = EditorTab.LOOKS },
                    { if (edits.isNotEmpty()) redo.add(edits.removeAt(edits.lastIndex)) },
                    { if (redo.isNotEmpty()) edits.add(redo.removeAt(redo.lastIndex)) },
                    { edits.clear(); redo.clear() }, { screen = Screen.STACK },
                    { qrDialog = true }, { screen = Screen.QR_SCAN },
                    { mode ->
                        when (mode) {
                            "分享" -> Toast.makeText(context, "已准备分享图片", Toast.LENGTH_SHORT).show()
                            "导出为" -> screen = Screen.FOLDER
                            else -> message = "照片已保存"
                        }
                    })
                Screen.INFO -> InfoScreen { screen = Screen.EDITOR }
                Screen.STACK -> StackScreen(edits, { screen = Screen.EDITOR }) {
                    if (it in edits.indices) edits.removeAt(it)
                }
                Screen.QR_SCAN -> QrScanner { screen = Screen.EDITOR }
                Screen.FOLDER -> FolderScreen(format, { screen = Screen.EDITOR }) {
                    screen = Screen.EDITOR
                    message = "照片已保存"
                }
            }
            if (qrDialog) QrDialog({ qrDialog = false }) {
                Toast.makeText(context, "QR 样式已准备分享", Toast.LENGTH_SHORT).show()
            }
            if (message != null && screen == Screen.EDITOR) {
                LaunchedEffect(message) { delay(2400); message = null }
            }
        }
    }
}

@Composable
private fun HomeScreen(dark: Boolean, tutorial: Boolean, onTutorial: () -> Unit, dismissTutorial: () -> Unit,
                       settings: () -> Unit, help: () -> Unit, open: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val fg = if (dark) Muted else Color(0xFF66686C)
    Box(Modifier.fillMaxSize().background(if (dark) Ink else Color.White)) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().height(72.dp).padding(horizontal = 28.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("打开", color = fg, fontSize = 24.sp)
            Spacer(Modifier.weight(1f))
            Text("◈", color = fg.copy(alpha = .45f), fontSize = 33.sp, modifier = Modifier.size(58.dp).clickable { onTutorial() }.padding(10.dp))
            Text("ⓘ", color = fg.copy(alpha = .45f), fontSize = 31.sp, modifier = Modifier.size(58.dp).clickable { help() }.padding(10.dp))
            Text("⋮", color = fg, fontSize = 36.sp, textAlign = TextAlign.Center,
                modifier = Modifier.size(58.dp).clickable { menu = true }.padding(8.dp))
        }
        Column(Modifier.align(Alignment.Center).clickable { open() }.padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(176.dp).border(9.dp, fg.copy(alpha = .65f), CircleShape), contentAlignment = Alignment.Center) {
                Text("+", color = fg.copy(alpha = .75f), fontSize = 98.sp, fontWeight = FontWeight.Light)
            }
            Spacer(Modifier.height(34.dp))
            Text("点按任意位置即可打开照片", color = fg, fontSize = 22.sp)
        }
        if (menu) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .08f)).clickable { menu = false })
            Column(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 72.dp, end = 12.dp)
                .width(225.dp).background(if (dark) Panel else Color.White, RoundedCornerShape(3.dp))) {
                Text("设置", color = if (dark) Color.White else Ink, fontSize = 20.sp,
                    modifier = Modifier.fillMaxWidth().clickable { menu = false; settings() }.padding(horizontal = 22.dp, vertical = 18.dp))
                Text("教程", color = if (dark) Color.White else Ink, fontSize = 20.sp,
                    modifier = Modifier.fillMaxWidth().clickable { menu = false; onTutorial() }.padding(horizontal = 22.dp, vertical = 18.dp))
                Text("帮助和反馈", color = if (dark) Color.White else Ink, fontSize = 20.sp,
                    modifier = Modifier.fillMaxWidth().clickable { menu = false; help() }.padding(horizontal = 22.dp, vertical = 18.dp))
            }
        }
        if (tutorial) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .2f)).clickable { dismissTutorial() })
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(390.dp)
                .background(if (dark) Panel else Color.White).padding(28.dp)) {
                Box(Modifier.align(Alignment.CenterHorizontally).width(54.dp).height(5.dp)
                    .background(Muted, RoundedCornerShape(3.dp)))
                Spacer(Modifier.height(28.dp))
                Text("教程", fontSize = 28.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(18.dp))
                Text("通过简短教程探索专业照片编辑技巧。", color = Muted, fontSize = 18.sp)
                Spacer(Modifier.weight(1f))
                CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally), color = Blue)
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TopBar(title: String, back: () -> Unit, dark: Boolean = true) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().height(64.dp).background(if (dark) Panel else Color.White).padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text("‹", fontSize = 52.sp, color = if (dark) Color.White else Ink,
            modifier = Modifier.clickable { back() }.padding(end = 20.dp))
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.Medium, color = if (dark) Color.White else Ink)
    }
}

@Composable
private fun SettingsScreen(dark: Boolean, size: String, format: String, back: () -> Unit,
                           changeDark: (Boolean) -> Unit, changeSize: (String) -> Unit, changeFormat: (String) -> Unit) {
    var sizeDialog by remember { mutableStateOf(false) }
    var formatDialog by remember { mutableStateOf(false) }
    val bg = if (dark) Panel else Color.White
    val fg = if (dark) Color.White else Ink
    Column(Modifier.fillMaxSize().background(bg)) {
        TopBar("设置", back, dark)
        SectionTitle("外观")
        Row(Modifier.fillMaxWidth().padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("深色主题背景", color = fg, fontSize = 23.sp, modifier = Modifier.weight(1f))
            Switch(dark, changeDark, colors = SwitchDefaults.colors(checkedThumbColor = Blue))
        }
        SectionTitle("导出和分享选项")
        SettingRow("调整图片大小", size,
            "选择导出和分享的最大图片尺寸。指定大小会参照图片的长边，尺寸较小的图片不会放大。", fg) { sizeDialog = true }
        HorizontalDivider(color = Muted.copy(alpha = .25f))
        SettingRow("格式和画质", format, "选择导出或分享图片时采用的格式和质量比率。", fg) { formatDialog = true }
    }
    if (sizeDialog) ChoiceDialog("调整图片大小",
        listOf("不要调整大小","4000 像素","2000 像素","1920 像素","1366 像素","800 像素"),
        size, { sizeDialog = false }) { changeSize(it); sizeDialog = false }
    if (formatDialog) ChoiceDialog("格式和画质",
        listOf("JPG 100%","JPG 95%","JPG 80%","PNG"),
        format, { formatDialog = false }) { changeFormat(it); formatDialog = false }
}

@Composable private fun SectionTitle(text: String) {
    Text(text, color = Blue, fontSize = 19.sp, modifier = Modifier.padding(start = 22.dp, top = 24.dp, bottom = 8.dp))
}
@Composable private fun SettingRow(title: String, value: String, subtitle: String, fg: Color, click: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable { click() }.padding(22.dp)) {
        Text(title, color = fg, fontSize = 23.sp)
        Text(value, color = Muted, fontSize = 18.sp, modifier = Modifier.padding(top = 4.dp))
        Text(subtitle, color = Muted, fontSize = 16.sp, lineHeight = 24.sp, modifier = Modifier.padding(top = 18.dp))
    }
}
@Composable private fun ChoiceDialog(title: String, choices: List<String>, selected: String,
                                     dismiss: () -> Unit, choose: (String) -> Unit) {
    AlertDialog(dismiss, title = { Text(title) }, text = {
        Column { choices.forEach {
            Row(Modifier.fillMaxWidth().clickable { choose(it) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(it == selected, { choose(it) }); Text(it, fontSize = 18.sp)
            }
        }}
    }, confirmButton = { TextButton(dismiss) { Text("取消") } })
}

@Composable private fun HelpScreen(back: () -> Unit) {
    val rows = listOf("反馈" to listOf("反馈"), "帮助" to listOf("帮助中心","YouTube 频道","Facebook 页面","许可"),
        "法律信息" to listOf("服务条款","隐私权政策","Google 地图 - 附加服务条款"))
    Column(Modifier.fillMaxSize().background(Panel).verticalScroll(rememberScrollState())) {
        TopBar("帮助和反馈", back)
        rows.forEach { entry ->
            SectionTitle(entry.first)
            entry.second.forEach {
                Text(it, color = Color.White, fontSize = 22.sp, modifier = Modifier.fillMaxWidth().clickable { }.padding(22.dp))
                HorizontalDivider(color = Muted.copy(alpha = .22f))
            }
        }
        SectionTitle("应用信息")
        Text("Snap Studio\n2.22.0", color = Color.White, fontSize = 21.sp, lineHeight = 29.sp, modifier = Modifier.padding(22.dp))
    }
}

@Composable private fun PickerScreen(close: () -> Unit, choose: () -> Unit) {
    var albums by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Color(0xFFF9F7FF))) {
        Box(Modifier.height(250.dp).fillMaxWidth().background(Color.Black.copy(alpha = .94f)))
        Column(Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp))
            .background(Color(0xFFF9F7FF)).padding(20.dp)) {
            Box(Modifier.align(Alignment.CenterHorizontally).width(56.dp).height(5.dp).background(Color(0xFFD9D8E0), RoundedCornerShape(3.dp)))
            Spacer(Modifier.height(24.dp))
            Text("此应用只能访问您选择的照片", color = Ink, modifier = Modifier.align(Alignment.CenterHorizontally), fontSize = 18.sp)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("×", fontSize = 38.sp, color = Ink, modifier = Modifier.clickable { close() }.padding(8.dp))
                Spacer(Modifier.weight(1f)); TabChip("照片", !albums) { albums = false }; Spacer(Modifier.width(10.dp))
                TabChip("影集", albums) { albums = true }; Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(26.dp))
            Text(if (albums) "影集" else "最近", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(20.dp))
            if (albums) Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { albums = false }) {
                PhotoThumb(150.dp, choose); Spacer(Modifier.width(18.dp)); Text("示例相册\n4 项", color = Ink, fontSize = 20.sp)
            } else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { repeat(3) { PhotoThumb(118.dp, choose) } }
        }
    }
}
@Composable private fun TabChip(text: String, selected: Boolean, click: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(if (selected) Color(0xFFD8E2FF) else Color(0xFFEFEFF5))
        .clickable { click() }.padding(horizontal = 38.dp, vertical = 12.dp)) { Text(text, color = Ink, fontSize = 18.sp) }
}
@Composable private fun PhotoThumb(size: Dp, click: () -> Unit) {
    Image(painterResource(R.drawable.editor_sample), "示例照片", contentScale = ContentScale.Crop,
        modifier = Modifier.size(size).clickable { click() })
}

private val looks = listOf("上次修改","Portrait","Smooth","Pop","Accentuate","Faded Glow","Morning","Bright","Fine Art","Push","Structure","Silhouette")
private val tools = listOf("调整图片","突出细节","曲线","白平衡","剪裁","旋转","视角","展开","局部","画笔",
    "修复","HDR 景观","魅力光晕","色调对比度","戏剧效果","复古","粗粒胶片","怀旧","斑驳","黑白",
    "黑白电影","美颜","头部姿势","镜头模糊","晕影","双重曝光","文字","相框")

@Composable
private fun EditorScreen(tab: EditorTab, edits: List<EditLayer>, message: String?, changeTab: (EditorTab) -> Unit,
                         open: () -> Unit, info: () -> Unit, selectTool: (String) -> Unit, activeTool: String?,
                         cancelTool: () -> Unit, commit: (String, Int) -> Unit, undo: () -> Unit, redo: () -> Unit,
                         revert: () -> Unit, stack: () -> Unit, qrCreate: () -> Unit, qrScan: () -> Unit,
                         doExport: (String) -> Unit) {
    var history by remember { mutableStateOf(false) }
    var qrMenu by remember { mutableStateOf(false) }
    var export by remember { mutableStateOf(false) }
    var selectedLook by remember { mutableStateOf("") }
    if (activeTool != null) {
        ToolEditor(activeTool, cancelTool) { commit(activeTool, it) }
        return
    }
    Box(Modifier.fillMaxSize().background(Ink)) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().height(72.dp).background(Color.Black).padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("打开", color = Muted, fontSize = 23.sp, modifier = Modifier.clickable { open() })
                Spacer(Modifier.weight(1f))
                Text("◈", color = Muted, fontSize = 34.sp, textAlign = TextAlign.Center, modifier = Modifier.size(58.dp).clickable { history = true }.padding(8.dp))
                Text("ⓘ", color = Muted, fontSize = 30.sp, textAlign = TextAlign.Center, modifier = Modifier.size(58.dp).clickable { info() }.padding(8.dp))
                Text("⋮", color = Muted, fontSize = 34.sp, textAlign = TextAlign.Center, modifier = Modifier.size(58.dp).clickable { history = true }.padding(8.dp))
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                EditedPhoto(edits, Modifier.fillMaxWidth(.94f).aspectRatio(1f))
            }
            if (tab == EditorTab.LOOKS) {
                Row(Modifier.fillMaxWidth().height(188.dp).background(Panel).horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    looks.forEachIndexed { index, look ->
                        Column(Modifier.width(112.dp).clickable {
                            selectedLook = look
                            if (index > 0) commit(look, 35)
                        }.padding(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(painterResource(R.drawable.editor_sample), null, contentScale = ContentScale.Crop,
                                colorFilter = if (index % 3 == 0) null else ColorFilter.colorMatrix(ColorMatrix().apply {
                                    setToSaturation(if (index % 2 == 0) .55f else 1.25f)
                                }),
                                modifier = Modifier.size(92.dp).border(if (selectedLook == look) 3.dp else 0.dp, Blue))
                            Text(look, color = if (selectedLook == look) Blue else Color.White, fontSize = 14.sp, maxLines = 1)
                        }
                    }
                }
            } else ToolGrid(selectTool)
            Row(Modifier.fillMaxWidth().height(86.dp).background(Panel), verticalAlignment = Alignment.CenterVertically) {
                BottomTab("样式", tab == EditorTab.LOOKS, Modifier.weight(1f)) { changeTab(EditorTab.LOOKS) }
                BottomTab("工具", tab == EditorTab.TOOLS, Modifier.weight(1f)) { changeTab(EditorTab.TOOLS) }
                BottomTab("导出", false, Modifier.weight(1f)) { export = true }
            }
        }
        if (history) {
            Scrim { history = false }
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Panel)) {
                HistoryRow("↶", "撤消", edits.isNotEmpty()) { undo(); history = false }
                HistoryRow("↷", "重做", true) { redo(); history = false }
                HistoryRow("◴", "还原", edits.isNotEmpty()) { revert(); history = false }
                HorizontalDivider(color = Muted.copy(alpha = .3f))
                HistoryRow("◆", "查看修改内容", edits.isNotEmpty()) { stack(); history = false }
                HorizontalDivider(color = Muted.copy(alpha = .3f))
                HistoryRow("◫", "QR 样式…", true) { qrMenu = true; history = false }
            }
        }
        if (qrMenu) {
            Scrim { qrMenu = false }
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Panel)) {
                HistoryRow("▦", "创建 QR 样式", edits.isNotEmpty()) { qrMenu = false; qrCreate() }
                HistoryRow("▣", "扫描 QR 样式", true) { qrMenu = false; qrScan() }
            }
        }
        if (export) {
            Scrim { export = false }
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Panel)) {
                ExportRow("↗", "分享", "与他人分享图片或在其他应用中打开图片。") { export = false; doExport("分享") }
                ExportRow("▣", "保存", "为您的照片创建副本。") { export = false; doExport("保存") }
                ExportRow("JPG", "导出", "创建副本，采用设置中的大小、格式和画质。") { export = false; doExport("导出") }
                ExportRow("▰", "导出为", "在选定文件夹中创建副本。") { export = false; doExport("导出为") }
            }
        }
        if (message != null) Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color(0xFF343436))
            .padding(horizontal = 24.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, color = Color.White, fontSize = 19.sp); Spacer(Modifier.weight(1f))
            Text("查看", color = Blue, fontWeight = FontWeight.Medium, fontSize = 18.sp)
        }
    }
}
@Composable private fun Scrim(dismiss: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .35f)).clickable { dismiss() })
}
@Composable private fun EditedPhoto(edits: List<EditLayer>, modifier: Modifier) {
    val strength = edits.sumOf { it.value }.coerceIn(-100, 160)
    Box(modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        Image(painterResource(R.drawable.editor_sample), "正在编辑的示例照片", contentScale = ContentScale.Crop,
            colorFilter = if (edits.any { it.name.contains("黑白") }) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null,
            modifier = Modifier.fillMaxSize())
        if (strength > 0) Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = (strength / 700f).coerceAtMost(.18f))))
        if (strength < 0) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = (-strength / 500f).coerceAtMost(.22f))))
    }
}
@Composable private fun BottomTab(label: String, selected: Boolean, modifier: Modifier, click: () -> Unit) {
    Box(modifier.fillMaxHeight().clickable { click() }, contentAlignment = Alignment.Center) {
        Text(label, color = if (selected) Blue else Color.White, fontSize = 22.sp)
    }
}
@Composable private fun ToolGrid(select: (String) -> Unit) {
    LazyVerticalGrid(GridCells.Fixed(5), modifier = Modifier.fillMaxWidth().height(590.dp).background(Panel).padding(vertical = 12.dp)) {
        items(tools) { tool ->
            Column(Modifier.height(96.dp).clickable { select(tool) }.padding(3.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                Text(toolGlyph(tool), color = Color.White, fontSize = 29.sp, modifier = Modifier.height(40.dp))
                Text(tool, color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 2)
            }
        }
    }
}
private fun toolGlyph(name: String) = when (name) {
    "调整图片" -> "☷"; "突出细节" -> "△"; "曲线" -> "⌁"; "白平衡" -> "♢"; "剪裁" -> "⌗"
    "旋转" -> "↻"; "视角" -> "◇"; "展开" -> "⊞"; "局部" -> "◎"; "画笔" -> "╱"
    "修复" -> "✚"; "文字" -> "T"; "相框" -> "▣"; "双重曝光" -> "◫"; else -> "◉"
}
@Composable private fun HistoryRow(icon: String, title: String, enabled: Boolean, click: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(78.dp).clickable(enabled = enabled) { click() }.padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(icon, color = if (enabled) Color.White else Muted.copy(alpha = .35f), fontSize = 31.sp, modifier = Modifier.width(70.dp))
        Text(title, color = if (enabled) Color.White else Muted.copy(alpha = .35f), fontSize = 22.sp)
    }
}
@Composable private fun ExportRow(icon: String, title: String, subtitle: String, click: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { click() }.padding(horizontal = 25.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp))
        Column { Text(title, color = Color.White, fontSize = 23.sp); Text(subtitle, color = Muted, fontSize = 15.sp) }
    }
    HorizontalDivider(color = Muted.copy(alpha = .22f))
}

private fun toolParams(tool: String) = when (tool) {
    "调整图片" -> listOf("亮度","对比度","饱和度","氛围","高光","阴影","暖色调")
    "突出细节" -> listOf("结构","锐化")
    "白平衡" -> listOf("色温","着色")
    "HDR 景观" -> listOf("滤镜强度","亮度","饱和度")
    "魅力光晕" -> listOf("光晕","饱和度","暖色调")
    "色调对比度" -> listOf("高色调","中色调","低色调","保护阴影","保护高光")
    "戏剧效果" -> listOf("滤镜强度","饱和度")
    "复古" -> listOf("亮度","饱和度","样式强度","晕影强度")
    "粗粒胶片" -> listOf("粒度","样式强度")
    "怀旧" -> listOf("亮度","对比度","饱和度","样式强度","刮痕","漏光")
    "斑驳" -> listOf("样式","亮度","对比度","饱和度","纹理强度")
    "黑白" -> listOf("亮度","对比度","粒度")
    "镜头模糊" -> listOf("模糊强度","过渡","晕影强度")
    "晕影" -> listOf("外部亮度","内部亮度")
    "美颜" -> listOf("面部提亮","皮肤平滑","眼部清晰")
    else -> listOf("强度")
}

@Composable private fun ToolEditor(tool: String, cancel: () -> Unit, confirm: (Int) -> Unit) {
    var value by remember(tool) { mutableStateOf(if (tool in listOf("HDR 景观","戏剧效果","魅力光晕")) 50 else 0) }
    var param by remember(tool) { mutableStateOf(toolParams(tool).first()) }
    var menu by remember { mutableStateOf(false) }
    var preset by remember { mutableStateOf(1) }
    var textValue by remember { mutableStateOf("LAKE WALK") }
    var textDialog by remember { mutableStateOf(false) }
    val brush = remember { mutableStateListOf<Offset>() }
    Box(Modifier.fillMaxSize().background(Ink)) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.height(115.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                LinearProgressIndicator({ ((value + 100) / 200f).coerceIn(0f, 1f) },
                    Modifier.fillMaxWidth().height(4.dp), color = Blue, trackColor = Color(0xFF44464A))
                Text(param + " " + (if (value > 0) "+" else "") + value, color = Muted, fontSize = 22.sp, modifier = Modifier.padding(top = 20.dp))
            }
            Box(Modifier.weight(1f).fillMaxWidth().pointerInput(param) {
                detectHorizontalDragGestures { _, amount -> value = (value + amount / 5).roundToInt().coerceIn(-100, 100) }
            }, contentAlignment = Alignment.Center) {
                EditedPhoto(listOf(EditLayer(tool, value)), Modifier.fillMaxWidth(.96f).aspectRatio(1f))
                ToolOverlay(tool, brush) { brush.add(it) }
                if (tool == "文字") Text(textValue, color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.background(Color.Black.copy(alpha = .35f)).clickable { textDialog = true }.padding(14.dp))
            }
            if (tool in listOf("HDR 景观","魅力光晕","戏剧效果","复古","粗粒胶片","怀旧","黑白","黑白电影","相框")) {
                Row(Modifier.fillMaxWidth().height(138.dp).background(Panel).horizontalScroll(rememberScrollState()).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    (1..8).forEach { i ->
                        Column(Modifier.width(104.dp).clickable { preset = i; value = (i * 9).coerceAtMost(75) },
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(painterResource(R.drawable.editor_sample), null, contentScale = ContentScale.Crop,
                                modifier = Modifier.size(78.dp).border(if (preset == i) 3.dp else 0.dp, Blue))
                            Text(if (tool == "粗粒胶片") "A0" + i else i.toString(),
                                color = if (preset == i) Blue else Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().height(94.dp).background(Panel), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly) {
                Text("×", color = Color.White, fontSize = 48.sp, modifier = Modifier.clickable { cancel() }.padding(16.dp))
                if (tool == "白平衡") Text("AW", color = Color.White, fontSize = 20.sp, modifier = Modifier.clickable { value = 0 }.padding(16.dp))
                Text("☷", color = if (menu) Blue else Color.White, fontSize = 32.sp, modifier = Modifier.clickable { menu = !menu }.padding(16.dp))
                if (tool == "镜头模糊") Text("⊙", color = Color.White, fontSize = 34.sp,
                    modifier = Modifier.clickable { preset = if (preset == 1) 2 else 1 }.padding(16.dp))
                if (tool == "文字") Text("T", color = Color.White, fontSize = 32.sp, modifier = Modifier.clickable { textDialog = true }.padding(16.dp))
                Text("✓", color = Color.White, fontSize = 40.sp, modifier = Modifier.clickable { confirm(value) }.padding(16.dp))
            }
        }
        if (menu) Column(Modifier.align(Alignment.Center).width(330.dp).background(Color(0xDD4A4B4D)).padding(vertical = 6.dp)) {
            toolParams(tool).forEach { p ->
                Row(Modifier.fillMaxWidth().clickable { param = p; menu = false }.background(if (p == param) Blue else Color.Transparent)
                    .padding(horizontal = 24.dp, vertical = 15.dp)) {
                    Text(p, color = Color.White, fontSize = 19.sp, modifier = Modifier.weight(1f))
                    Text(if (p == param) (if (value > 0) "+" else "") + value else "0", color = Color.White, fontSize = 19.sp)
                }
            }
        }
    }
    if (textDialog) AlertDialog({ textDialog = false }, title = { Text("修改文字") },
        text = { OutlinedTextField(textValue, { textValue = it }) },
        confirmButton = { TextButton({ textDialog = false }) { Text("确定") } },
        dismissButton = { TextButton({ textDialog = false }) { Text("取消") } })
}

@Composable private fun ToolOverlay(tool: String, brush: List<Offset>, addPoint: (Offset) -> Unit) {
    when (tool) {
        "剪裁" -> Canvas(Modifier.fillMaxWidth(.82f).aspectRatio(1f).border(2.dp, Color.White)) {
            drawLine(Color.White, Offset(size.width / 3, 0f), Offset(size.width / 3, size.height), 2f)
            drawLine(Color.White, Offset(size.width * 2 / 3, 0f), Offset(size.width * 2 / 3, size.height), 2f)
            drawLine(Color.White, Offset(0f, size.height / 3), Offset(size.width, size.height / 3), 2f)
            drawLine(Color.White, Offset(0f, size.height * 2 / 3), Offset(size.width, size.height * 2 / 3), 2f)
        }
        "镜头模糊" -> Canvas(Modifier.fillMaxWidth(.72f).aspectRatio(1f)) {
            drawCircle(Color.White, size.minDimension * .22f, style = Stroke(3f))
            drawCircle(Color.White, size.minDimension * .42f, style = Stroke(3f)); drawCircle(Blue, 20f)
        }
        "晕影","局部","斑驳" -> Canvas(Modifier.fillMaxWidth(.8f).aspectRatio(1f)) {
            drawCircle(Blue, 24f); drawCircle(Color.White, 27f, style = Stroke(4f))
        }
        "曲线" -> Canvas(Modifier.fillMaxWidth(.84f).aspectRatio(1f).border(2.dp, Color.White)) {
            val path = Path().apply { moveTo(0f, size.height); cubicTo(size.width * .25f, size.height * .8f,
                size.width * .56f, size.height * .2f, size.width, 0f) }
            drawPath(path, Color.White, style = Stroke(5f)); drawCircle(Blue, 14f, Offset(size.width * .55f, size.height * .36f))
        }
        "画笔","修复" -> Canvas(Modifier.fillMaxWidth(.94f).aspectRatio(1f).pointerInput(Unit) {
            detectDragGestures { change, _ -> addPoint(change.position) }
        }) {
            if (brush.size > 1) for (i in 1 until brush.size)
                drawLine(Color.Red.copy(alpha = .65f), brush[i - 1], brush[i], 28f, cap = StrokeCap.Round)
        }
        "展开" -> Canvas(Modifier.fillMaxWidth(.94f).aspectRatio(1f).border(2.dp, Color.White)) {
            listOf(Offset(size.width / 2, 25f), Offset(size.width / 2, size.height - 25f),
                Offset(25f, size.height / 2), Offset(size.width - 25f, size.height / 2)).forEach { drawCircle(Blue, 12f, it) }
        }
    }
}

@Composable private fun InfoScreen(back: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Panel)) {
        TopBar("图片详细信息", back)
        EditedPhoto(emptyList(), Modifier.fillMaxWidth().aspectRatio(1f))
        Column(Modifier.padding(28.dp)) {
            Text("示例照片", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(18.dp)); Text("今天  ·  本地图片", color = Muted, fontSize = 18.sp)
            Spacer(Modifier.height(14.dp)); Text("正方形  ·  PNG  ·  约 11 KB", color = Muted, fontSize = 18.sp)
            Spacer(Modifier.height(24.dp)); Text("图片信息仅保存在设备上。", color = Muted, fontSize = 16.sp)
        }
    }
}
@Composable private fun StackScreen(edits: List<EditLayer>, back: () -> Unit, delete: (Int) -> Unit) {
    var selected by remember { mutableStateOf(edits.lastIndex) }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        EditedPhoto(edits, Modifier.align(Alignment.Center).fillMaxWidth(.78f).aspectRatio(1f))
        Text("‹", color = Color.White, fontSize = 52.sp, modifier = Modifier.align(Alignment.TopStart).clickable { back() }.padding(20.dp))
        Column(Modifier.align(Alignment.CenterEnd).width(190.dp).background(Panel)) {
            edits.reversed().forEachIndexed { rev, layer ->
                val index = edits.lastIndex - rev
                Row(Modifier.fillMaxWidth().background(if (selected == index) Blue else Panel).clickable { selected = index }.padding(14.dp)) {
                    Text(layer.name, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f)); Text("≡", color = Color.White)
                }
            }
            Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Text("⌫", color = Color.White, fontSize = 26.sp, modifier = Modifier.clickable { if (selected >= 0) delete(selected) })
                Text("✎", color = Color.White, fontSize = 26.sp); Text("☷", color = Color.White, fontSize = 26.sp)
            }
            Text("原图", color = Color.White, fontSize = 16.sp, modifier = Modifier.fillMaxWidth().padding(16.dp))
        }
    }
}
@Composable private fun QrDialog(dismiss: () -> Unit, share: () -> Unit) {
    AlertDialog(dismiss, title = { Column { Text("创建 QR 样式", fontSize = 27.sp); Text("与好友分享自己的样式", fontSize = 17.sp, color = Muted) } },
        text = { Box(Modifier.fillMaxWidth().aspectRatio(1f).background(Color.White), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize().padding(24.dp)) {
                val cells = 17; val cell = size.minDimension / cells
                for (y in 0 until cells) for (x in 0 until cells)
                    if ((x * y + x + y * 3) % 5 < 2 || (x < 4 && y < 4) || (x > 12 && y < 4) || (x < 4 && y > 12))
                        drawRect(Color.Black, Offset(x * cell, y * cell), androidx.compose.ui.geometry.Size(cell, cell))
                drawCircle(Color(0xFF61B35B), size.minDimension * .12f, center)
            }
        } }, confirmButton = { TextButton(share) { Text("分享") } },
        dismissButton = { TextButton(dismiss) { Text("取消") } })
}
@Composable private fun QrScanner(back: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Text("‹", color = Color.White, fontSize = 52.sp, modifier = Modifier.align(Alignment.TopStart).clickable { back() }.padding(20.dp))
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("将 QR 码对准矩形框以进行扫描", color = Color.White, fontSize = 20.sp)
            Spacer(Modifier.height(30.dp)); Box(Modifier.size(310.dp).border(3.dp, Color.White))
        }
    }
}
@Composable private fun FolderScreen(format: String, back: () -> Unit, save: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(Modifier.fillMaxWidth().height(82.dp).padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("☰", color = Color.DarkGray, fontSize = 32.sp, modifier = Modifier.clickable { back() })
            Spacer(Modifier.width(34.dp)); Text("下载", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f)); Text("▣", color = Color.DarkGray, fontSize = 31.sp)
        }
        Text("下载", color = Color(0xFF1684DC), fontSize = 20.sp, modifier = Modifier.padding(24.dp))
        Text("“下载”中的文件", color = Ink, fontSize = 20.sp, modifier = Modifier.padding(24.dp))
        Box(Modifier.padding(24.dp).width(260.dp).height(120.dp).border(1.dp, Color.LightGray), contentAlignment = Alignment.Center) {
            Text("▱  Snap Studio", color = Color(0xFF303238), fontSize = 20.sp)
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth().padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("▣", color = Color.Red, fontSize = 26.sp); Spacer(Modifier.width(20.dp))
            Text("edited." + if (format == "PNG") "png" else "jpg", color = Ink, fontSize = 20.sp, modifier = Modifier.weight(1f))
            Button(save, shape = RoundedCornerShape(28.dp)) { Text("保存", fontSize = 20.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) }
        }
    }
}
