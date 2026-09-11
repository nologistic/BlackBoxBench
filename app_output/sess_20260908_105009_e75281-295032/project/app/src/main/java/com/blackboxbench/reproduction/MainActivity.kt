package com.blackboxbench.reproduction

import android.app.Activity
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ReproducedApp() }
    }
}

private const val SCREEN_CALCULATOR = "calculator"
private const val SCREEN_UNITS = "units"
private const val SCREEN_CONVERTER = "converter"
private const val SCREEN_SETTINGS = "settings"
private const val SCREEN_APPEARANCE = "appearance"
private const val SCREEN_ABOUT = "about"

private data class HistoryItem(val expression: String, val result: String)
private data class UnitItem(val name: String, val symbol: String, val factor: Double)
private data class UnitCategory(
    val id: String,
    val name: String,
    val glyph: String,
    val units: List<UnitItem>,
    val defaultFrom: String,
    val defaultTo: String,
    val temperature: Boolean = false
)

private val unitCategories = listOf(
    UnitCategory(
        "length", "长度", "↔",
        listOf(
            UnitItem("千米", "km", 1000.0), UnitItem("米", "m", 1.0),
            UnitItem("厘米", "cm", .01), UnitItem("毫米", "mm", .001),
            UnitItem("微米", "μm", 1e-6), UnitItem("纳米", "nm", 1e-9),
            UnitItem("埃", "Å", 1e-10), UnitItem("英里", "mi", 1609.344),
            UnitItem("码", "yd", .9144), UnitItem("英尺", "ft", .3048),
            UnitItem("英寸", "in", .0254), UnitItem("英寻", "fathom", 1.8288),
            UnitItem("海里", "NM", 1852.0), UnitItem("天文单位", "au", 149597870700.0),
            UnitItem("秒差距", "pc", 3.085677581e16), UnitItem("光年", "ly", 9.460730472e15)
        ), "km", "m"
    ),
    UnitCategory(
        "area", "面积", "▧",
        listOf(
            UnitItem("平方千米", "km²", 1e6), UnitItem("平方米", "m²", 1.0),
            UnitItem("平方厘米", "cm²", 1e-4), UnitItem("平方毫米", "mm²", 1e-6),
            UnitItem("平方英里", "sq mi", 2589988.110336), UnitItem("平方码", "sq yd", .83612736),
            UnitItem("平方英尺", "sq ft", .09290304), UnitItem("平方英寸", "sq in", .00064516),
            UnitItem("英亩", "ac", 4046.8564224), UnitItem("公顷", "ha", 10000.0)
        ), "km²", "m²"
    ),
    UnitCategory(
        "volume", "体积", "⬡",
        listOf(
            UnitItem("升", "L", 1.0), UnitItem("立方米", "m³", 1000.0),
            UnitItem("毫升", "mL", .001), UnitItem("立方厘米", "cm³", .001),
            UnitItem("立方毫米", "mm³", 1e-6), UnitItem("加仑（美）", "gal US", 3.785411784),
            UnitItem("夸脱（美）", "qt US", .946352946), UnitItem("品脱（美）", "pt US", .473176473),
            UnitItem("杯（美）", "cup US", .2365882365), UnitItem("液盎司（美）", "fl oz US", .0295735295625),
            UnitItem("汤匙", "tbsp", .01478676478125), UnitItem("茶匙", "tsp", .00492892159375),
            UnitItem("加仑（英）", "gal UK", 4.54609), UnitItem("品脱（英）", "pt UK", .56826125)
        ), "L", "m³"
    ),
    UnitCategory(
        "mass", "质量", "⚖",
        listOf(
            UnitItem("磅", "lb", .45359237), UnitItem("千克", "kg", 1.0),
            UnitItem("克", "g", .001), UnitItem("毫克", "mg", 1e-6),
            UnitItem("微克", "μg", 1e-9), UnitItem("吨", "t", 1000.0),
            UnitItem("盎司", "oz", .028349523125), UnitItem("格令", "gr", .00006479891),
            UnitItem("打兰", "dr", .0017718451953125), UnitItem("英石", "st", 6.35029318),
            UnitItem("长吨", "long ton", 1016.0469088), UnitItem("短吨", "short ton", 907.18474),
            UnitItem("千吨", "kt", 1e6), UnitItem("克拉", "ct", .0002)
        ), "lb", "kg"
    ),
    UnitCategory(
        "temperature", "温度", "℃",
        listOf(
            UnitItem("摄氏度", "°C", 1.0), UnitItem("华氏度", "°F", 1.0),
            UnitItem("兰氏度", "°R", 1.0), UnitItem("开尔文", "K", 1.0)
        ), "°C", "K", true
    ),
    UnitCategory(
        "time", "时间", "◷",
        listOf(
            UnitItem("小时", "h", 3600.0), UnitItem("秒", "s", 1.0),
            UnitItem("分钟", "min", 60.0), UnitItem("毫秒", "ms", .001),
            UnitItem("天", "d", 86400.0), UnitItem("周", "wk", 604800.0),
            UnitItem("年", "y", 31557600.0)
        ), "h", "s"
    ),
    UnitCategory(
        "speed", "速度", "➤",
        listOf(
            UnitItem("千米每小时", "km/h", .2777777777777778), UnitItem("英里每小时", "mph", .44704),
            UnitItem("米每秒", "m/s", 1.0), UnitItem("千米每秒", "km/s", 1000.0),
            UnitItem("节", "kn", .5144444444444445), UnitItem("英尺每秒", "ft/s", .3048),
            UnitItem("马赫", "Ma", 340.29), UnitItem("光速", "c", 299792458.0)
        ), "km/h", "mph"
    ),
    UnitCategory(
        "pressure", "压强", "◉",
        listOf(
            UnitItem("巴", "bar", 100.0), UnitItem("磅每平方英寸", "psi", 6.894757293168),
            UnitItem("帕斯卡", "Pa", .001), UnitItem("千帕", "kPa", 1.0),
            UnitItem("兆帕", "MPa", 1000.0), UnitItem("毫巴", "mbar", .1),
            UnitItem("标准大气压", "atm", 101.325), UnitItem("托", "Torr", .133322368421),
            UnitItem("毫米汞柱", "mmHg", .133322387415), UnitItem("英寸汞柱", "inHg", 3.386389)
        ), "bar", "psi"
    ),
    UnitCategory(
        "energy", "能量", "⚡",
        listOf(
            UnitItem("千卡", "kcal", 4184.0), UnitItem("千焦", "kJ", 1000.0),
            UnitItem("焦耳", "J", 1.0), UnitItem("兆焦", "MJ", 1e6),
            UnitItem("吉焦", "GJ", 1e9), UnitItem("卡路里", "cal", 4.184),
            UnitItem("瓦时", "Wh", 3600.0), UnitItem("千瓦时", "kWh", 3.6e6),
            UnitItem("兆瓦时", "MWh", 3.6e9), UnitItem("电子伏", "eV", 1.602176634e-19),
            UnitItem("英热单位", "BTU", 1055.05585262), UnitItem("撒姆", "thm", 105505585.262),
            UnitItem("英尺磅", "ft·lbf", 1.3558179483314), UnitItem("尔格", "erg", 1e-7)
        ), "kcal", "kJ"
    )
)

@Composable
fun ReproducedApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("calculator_state", 0) }
    var screen by rememberSaveable { mutableStateOf(SCREEN_CALCULATOR) }
    var activeCategoryId by rememberSaveable { mutableStateOf("length") }
    var savedTheme by remember { mutableStateOf(prefs.getString("theme", "light") ?: "light") }
    var pendingTheme by remember { mutableStateOf(savedTheme) }
    var history by remember { mutableStateOf(loadHistory(prefs)) }

    val previewTheme = if (screen == SCREEN_APPEARANCE) pendingTheme else savedTheme
    val isDark = previewTheme in setOf("dark", "dark_red", "black", "mono")

    BenchmarkAppTheme(darkTheme = isDark) {
        ApplySystemBarStyle(isDark)
        Surface(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(), color = MaterialTheme.colorScheme.background) {
            BackHandler(enabled = screen != SCREEN_CALCULATOR) {
                when (screen) {
                    SCREEN_CONVERTER -> screen = SCREEN_UNITS
                    SCREEN_APPEARANCE -> {
                        pendingTheme = savedTheme
                        screen = SCREEN_SETTINGS
                    }
                    else -> screen = SCREEN_CALCULATOR
                }
            }

            when (screen) {
                SCREEN_CALCULATOR -> CalculatorScreen(
                    history = history,
                    onHistoryChanged = {
                        history = it
                        saveHistory(prefs, it)
                    },
                    openUnits = { screen = SCREEN_UNITS },
                    openSettings = { screen = SCREEN_SETTINGS },
                    openAbout = { screen = SCREEN_ABOUT }
                )
                SCREEN_UNITS -> UnitCategoriesScreen(
                    onBack = { screen = SCREEN_CALCULATOR },
                    onSelect = {
                        activeCategoryId = it
                        screen = SCREEN_CONVERTER
                    }
                )
                SCREEN_CONVERTER -> ConverterScreen(
                    category = unitCategories.first { it.id == activeCategoryId },
                    prefs = prefs,
                    onBack = { screen = SCREEN_UNITS }
                )
                SCREEN_SETTINGS -> SettingsScreen(
                    prefs = prefs,
                    onBack = { screen = SCREEN_CALCULATOR },
                    openAppearance = {
                        pendingTheme = savedTheme
                        screen = SCREEN_APPEARANCE
                    }
                )
                SCREEN_APPEARANCE -> AppearanceScreen(
                    selected = pendingTheme,
                    onSelected = { pendingTheme = it },
                    onBack = {
                        pendingTheme = savedTheme
                        screen = SCREEN_SETTINGS
                    },
                    onSave = {
                        savedTheme = pendingTheme
                        prefs.edit().putString("theme", savedTheme).apply()
                        screen = SCREEN_SETTINGS
                    }
                )
                SCREEN_ABOUT -> AboutScreen(onBack = { screen = SCREEN_CALCULATOR })
            }
        }
    }
}

@Composable
private fun ApplySystemBarStyle(dark: Boolean) {
    val view = LocalView.current
    val surface = if (dark) 0xFF0F1110.toInt() else 0xFFFFFFFF.toInt()
    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = surface
        window.navigationBarColor = surface
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val lightMask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            window.insetsController?.setSystemBarsAppearance(if (dark) 0 else lightMask, lightMask)
        } else {
            @Suppress("DEPRECATION")
            view.systemUiVisibility = if (dark) 0 else
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
    }
}

@Composable
private fun AppToolbar(
    title: String = "",
    onBack: (() -> Unit)? = null,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            ToolbarIcon("‹", onBack, 40.sp)
        } else {
            Spacer(Modifier.width(48.dp))
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (action != null && onAction != null) {
            ToolbarIcon(action, onAction, 29.sp)
        } else {
            Spacer(Modifier.width(48.dp))
        }
    }
}

@Composable
private fun ToolbarIcon(label: String, onClick: () -> Unit, fontSize: androidx.compose.ui.unit.TextUnit = 28.sp) {
    Box(
        modifier = Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = fontSize, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalculatorScreen(
    history: List<HistoryItem>,
    onHistoryChanged: (List<HistoryItem>) -> Unit,
    openUnits: () -> Unit,
    openSettings: () -> Unit,
    openAbout: () -> Unit
) {
    var expression by rememberSaveable { mutableStateOf("") }
    var result by rememberSaveable { mutableStateOf("0") }
    var finished by rememberSaveable { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    fun clearAll() {
        expression = ""
        result = "0"
        finished = false
    }

    fun deleteOne() {
        if (finished) {
            clearAll()
        } else if (expression.isNotEmpty()) {
            expression = expression.dropLast(1)
            if (expression.isEmpty()) result = "0"
        }
    }

    fun appendDigit(value: String) {
        if (finished) {
            expression = if (value == ".") "0." else value
            result = "0"
            finished = false
            return
        }
        if (value == ".") {
            val currentNumber = expression.takeLastWhile { it.isDigit() || it == '.' }
            if (currentNumber.contains('.')) return
            expression += if (currentNumber.isEmpty()) "0." else "."
        } else {
            expression += value
        }
    }

    fun appendOperator(op: String) {
        if (finished) {
            expression = result + op
            finished = false
            return
        }
        if (op == "%") {
            if (expression.isNotEmpty() && !expression.endsWith("%")) expression += "%"
            return
        }
        if (op == "√") {
            expression += if (expression.isEmpty()) "1√" else "√"
            return
        }
        if (expression.isEmpty()) {
            if (op == "-") expression = "-"
            return
        }
        if (expression.last() in listOf('+', '-', '×', '÷', '^', '√')) {
            expression = expression.dropLast(1) + op
        } else {
            expression += op
        }
    }

    fun calculate() {
        val evaluated = evaluateExpression(expression) ?: return
        val shown = formatNumber(evaluated)
        result = shown
        finished = true
        val item = HistoryItem(expression, shown)
        onHistoryChanged(listOf(item) + history.filterNot { it == item })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolbarIcon("↶", { if (history.isNotEmpty()) showHistory = true }, 27.sp)
            ToolbarIcon("⇄", openUnits, 27.sp)
            Box(
                modifier = Modifier.size(58.dp).clip(CircleShape).clickable { showMenu = true },
                contentAlignment = Alignment.Center
            ) {
                Text("⋮", fontSize = 30.sp, color = MaterialTheme.colorScheme.onBackground)
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("设置", fontSize = 18.sp) },
                        onClick = {
                            showMenu = false
                            openSettings()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("关于", fontSize = 18.sp) },
                        onClick = {
                            showMenu = false
                            openAbout()
                        }
                    )
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Bottom
        ) {
            if (expression.isNotEmpty()) {
                Text(
                    expression,
                    fontSize = if (finished) 28.sp else 42.sp,
                    lineHeight = 48.sp,
                    color = if (finished) MaterialTheme.colorScheme.onSurface.copy(alpha = .66f)
                    else MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.End
                )
                Spacer(Modifier.height(10.dp))
            }
            Text(
                result,
                fontSize = 56.sp,
                lineHeight = 62.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.End
            )
        }

        val rows = listOf(
            listOf("%", "^", "√", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "-"),
            listOf("1", "2", "3", "+"),
            listOf("0", ".", "C", "=")
        )
        Column(
            modifier = Modifier.fillMaxWidth().height(448.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    row.forEach { key ->
                        val special = key in setOf("%", "^", "√", "÷", "×", "-", "+")
                        val equals = key == "="
                        val clear = key == "C"
                        CalcKey(
                            label = key,
                            special = special,
                            equals = equals,
                            clear = clear,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onClick = {
                                when {
                                    key.first().isDigit() || key == "." -> appendDigit(key)
                                    key == "C" -> deleteOne()
                                    key == "=" -> calculate()
                                    else -> appendOperator(key)
                                }
                            },
                            onLongClick = { if (key == "C") clearAll() }
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        }
        Box(
            modifier = Modifier.align(Alignment.TopEnd)
                .width(78.dp)
                .height(64.dp)
                .clickable { showMenu = true }
        )
    }

    if (showHistory) {
        AlertDialog(
            onDismissRequest = { showHistory = false },
            title = { Text("历史记录", fontSize = 24.sp) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp)) {
                    items(history) { item ->
                        Column(
                            modifier = Modifier.fillMaxWidth().clickable {
                                expression = item.expression
                                result = item.result
                                finished = true
                                showHistory = false
                            }.padding(vertical = 12.dp)
                        ) {
                            Text(item.expression, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f))
                            Text(item.result, fontSize = 28.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
                        }
                        HorizontalDivider()
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onHistoryChanged(emptyList())
                    showHistory = false
                }) { Text("清除", fontSize = 17.sp) }
            },
            confirmButton = {
                TextButton(onClick = { showHistory = false }) { Text("确定", fontSize = 17.sp) }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalcKey(
    label: String,
    special: Boolean,
    equals: Boolean,
    clear: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val bg = when {
        equals -> MaterialTheme.colorScheme.primary
        special -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val fg = when {
        equals -> MaterialTheme.colorScheme.onPrimary
        clear -> Color(0xFFD43B3B)
        special -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(25.dp))
            .background(bg)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = if (label == "√") 29.sp else 27.sp, color = fg, fontWeight = FontWeight.Normal)
    }
}

@Composable
private fun UnitCategoriesScreen(onBack: () -> Unit, onSelect: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        AppToolbar(title = "单位转换", onBack = onBack)
        Text(
            "选择转换类型",
            modifier = Modifier.padding(start = 24.dp, top = 18.dp, bottom = 16.dp),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .62f)
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            unitCategories.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(146.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { category ->
                        Column(
                            modifier = Modifier.weight(1f).fillMaxHeight()
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onSelect(category.id) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier.size(56.dp).clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(category.glyph, fontSize = 27.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.height(14.dp))
                            Text(category.name, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConverterScreen(category: UnitCategory, prefs: SharedPreferences, onBack: () -> Unit) {
    val savedFrom = prefs.getString("from_" + category.id, category.defaultFrom) ?: category.defaultFrom
    val savedTo = prefs.getString("to_" + category.id, category.defaultTo) ?: category.defaultTo
    var fromSymbol by remember(category.id) { mutableStateOf(savedFrom) }
    var toSymbol by remember(category.id) { mutableStateOf(savedTo) }
    var input by rememberSaveable(category.id) { mutableStateOf(prefs.getString("input_" + category.id, "0") ?: "0") }
    var choosingFrom by remember { mutableStateOf<Boolean?>(null) }

    val from = category.units.firstOrNull { it.symbol == fromSymbol } ?: category.units.first()
    val to = category.units.firstOrNull { it.symbol == toSymbol } ?: category.units.last()
    val numeric = input.toDoubleOrNull() ?: 0.0
    val converted = convertValue(numeric, from, to, category.temperature)

    fun persist() {
        prefs.edit()
            .putString("from_" + category.id, fromSymbol)
            .putString("to_" + category.id, toSymbol)
            .putString("input_" + category.id, input)
            .apply()
    }

    fun append(value: String) {
        input = when {
            value == "." && input.contains(".") -> input
            value == "." -> input + "."
            input == "0" -> value
            input == "-0" -> "-" + value
            else -> input + value
        }
        persist()
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        AppToolbar(title = category.name, onBack = onBack)

        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                input,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 48.sp,
                lineHeight = 54.sp,
                textAlign = TextAlign.End,
                color = MaterialTheme.colorScheme.onBackground
            )
            UnitSelector(from, modifier = Modifier.fillMaxWidth()) { choosingFrom = true }
            Spacer(Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                ToolbarIcon("⇅", {
                    val oldFrom = fromSymbol
                    fromSymbol = toSymbol
                    toSymbol = oldFrom
                    persist()
                }, 28.sp)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                formatNumber(converted),
                modifier = Modifier.fillMaxWidth(),
                fontSize = 48.sp,
                lineHeight = 54.sp,
                textAlign = TextAlign.End,
                color = MaterialTheme.colorScheme.onBackground
            )
            UnitSelector(to, modifier = Modifier.fillMaxWidth()) { choosingFrom = false }
        }

        val digitRows = listOf(listOf("7", "8", "9"), listOf("4", "5", "6"), listOf("1", "2", "3"), listOf("0", ".", "C"))
        Column(
            modifier = Modifier.fillMaxWidth().height(if (category.temperature) 382.dp else 314.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            digitRows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    row.forEach { key ->
                        CalcKey(
                            label = key,
                            special = false,
                            equals = false,
                            clear = key == "C",
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onClick = {
                                if (key == "C") {
                                    input = "0"
                                    persist()
                                } else append(key)
                            },
                            onLongClick = {
                                if (key == "C") {
                                    input = "0"
                                    persist()
                                }
                            }
                        )
                    }
                }
            }
            if (category.temperature) {
                CalcKey(
                    label = "+/−",
                    special = true,
                    equals = false,
                    clear = false,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    onClick = {
                        input = if (input.startsWith("-")) input.drop(1) else "-" + input
                        persist()
                    },
                    onLongClick = {}
                )
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    val selectFrom = choosingFrom
    if (selectFrom != null) {
        val selected = if (selectFrom) fromSymbol else toSymbol
        AlertDialog(
            onDismissRequest = { choosingFrom = null },
            title = { Text(if (selectFrom) "选择起始单位" else "选择目标单位") },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
                    items(category.units) { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                if (selectFrom) fromSymbol = item.symbol else toSymbol = item.symbol
                                persist()
                                choosingFrom = null
                            }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = item.symbol == selected, onClick = null)
                            Text(item.name + " (" + item.symbol + ")", fontSize = 17.sp)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { choosingFrom = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun UnitSelector(item: UnitItem, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier.height(58.dp).clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant).clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(item.name, modifier = Modifier.weight(1f), fontSize = 18.sp)
        Text(item.symbol, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text("⌄", fontSize = 22.sp)
    }
}

@Composable
private fun SettingsScreen(
    prefs: SharedPreferences,
    onBack: () -> Unit,
    openAppearance: () -> Unit
) {
    var vibration by remember { mutableStateOf(prefs.getBoolean("vibration", true)) }
    var keepAwake by remember { mutableStateOf(prefs.getBoolean("keep_awake", true)) }
    var widgetDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        AppToolbar(title = "设置", onBack = onBack)
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            SettingsNavRow("◐", "自定义外观", "主题、颜色和界面样式", openAppearance)
            SettingsNavRow("▦", "自定义微件颜色", "设置主屏幕微件颜色") { widgetDialog = true }

            Text(
                "支持我们",
                modifier = Modifier.padding(start = 24.dp, top = 22.dp, bottom = 8.dp),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(18.dp)
            ) {
                Text("Buy Fossify Thank You", fontSize = 19.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.height(5.dp))
                Text("支持独立、无广告的开源应用", fontSize = 15.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .75f))
            }

            Text(
                "通用",
                modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            SettingsValueRow("文", "语言", "中文")
            SettingsSwitchRow("〰", "按键振动", vibration) {
                vibration = it
                prefs.edit().putBoolean("vibration", it).apply()
            }
            SettingsSwitchRow("☼", "防止设备休眠", keepAwake) {
                keepAwake = it
                prefs.edit().putBoolean("keep_awake", it).apply()
            }
        }
    }

    if (widgetDialog) {
        AlertDialog(
            onDismissRequest = { widgetDialog = false },
            title = { Text("自定义微件颜色") },
            text = {
                Column {
                    Text("选择微件强调色", fontSize = 16.sp)
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                        listOf(Color(0xFF006C47), Color(0xFF2455A4), Color(0xFF8B4A67), Color(0xFFD06A22)).forEach {
                            Box(Modifier.size(46.dp).clip(CircleShape).background(it))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { widgetDialog = false }) { Text("确定") } }
        )
    }
}

@Composable
private fun SettingsNavRow(icon: String, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 22.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, modifier = Modifier.width(42.dp), fontSize = 25.sp, color = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 18.sp)
            Text(subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
        }
        Text("›", fontSize = 28.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .45f))
    }
}

@Composable
private fun SettingsValueRow(icon: String, title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, modifier = Modifier.width(42.dp), fontSize = 23.sp, color = MaterialTheme.colorScheme.primary)
        Text(title, modifier = Modifier.weight(1f), fontSize = 18.sp)
        Text(value, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f))
    }
}

@Composable
private fun SettingsSwitchRow(icon: String, title: String, checked: Boolean, onChanged: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onChanged(!checked) }.padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, modifier = Modifier.width(42.dp), fontSize = 23.sp, color = MaterialTheme.colorScheme.primary)
        Text(title, modifier = Modifier.weight(1f), fontSize = 18.sp)
        Switch(checked = checked, onCheckedChange = onChanged)
    }
}

private data class ThemeChoice(val id: String, val label: String, val swatch: Color)

@Composable
private fun AppearanceScreen(
    selected: String,
    onSelected: (String) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    val choices = listOf(
        ThemeChoice("system", "系统默认", Color(0xFF8A938E)),
        ThemeChoice("light", "浅色", Color(0xFFF4F5F3)),
        ThemeChoice("dark", "深色", Color(0xFF242625)),
        ThemeChoice("dark_red", "深红色", Color(0xFF5E2525)),
        ThemeChoice("white", "白色", Color.White),
        ThemeChoice("mono", "黑白", Color.Black),
        ThemeChoice("custom", "自定义", Color(0xFF006C47))
    )
    Column(modifier = Modifier.fillMaxSize()) {
        AppToolbar(title = "自定义外观", onBack = onBack, action = "✓", onAction = onSave)
        Text(
            "主题",
            modifier = Modifier.padding(start = 24.dp, top = 18.dp, bottom = 8.dp),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
        choices.forEach { choice ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onSelected(choice.id) }
                    .padding(horizontal = 18.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = selected == choice.id, onClick = { onSelected(choice.id) })
                Box(
                    modifier = Modifier.size(34.dp).clip(CircleShape).background(choice.swatch)
                )
                Text(choice.label, modifier = Modifier.padding(start = 18.dp), fontSize = 18.sp)
            }
        }
        Text(
            "选择后可立即预览。点击右上角对勾保存。",
            modifier = Modifier.padding(24.dp),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f)
        )
    }
}

@Composable
private fun AboutScreen(onBack: () -> Unit) {
    var message by remember { mutableStateOf<String?>(null) }
    val groups = listOf(
        "支持" to listOf("常见问题", "已知问题", "hello@fossify.org"),
        "参与" to listOf("帮助翻译", "分享应用", "贡献者", "捐赠"),
        "社区" to listOf("GitHub", "Reddit", "Telegram"),
        "法律" to listOf("隐私政策", "第三方许可")
    )
    Column(modifier = Modifier.fillMaxSize()) {
        AppToolbar(title = "关于", onBack = onBack)
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            groups.forEach { group ->
                Text(
                    group.first,
                    modifier = Modifier.padding(start = 24.dp, top = 18.dp, bottom = 5.dp),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                group.second.forEach { label ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { message = label }
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, modifier = Modifier.weight(1f), fontSize = 18.sp)
                        Text("›", fontSize = 25.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .45f))
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp))
            Text("版本 1.4.0", modifier = Modifier.fillMaxWidth().padding(20.dp), textAlign = TextAlign.Center, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .62f))
        }
    }
    if (message != null) {
        AlertDialog(
            onDismissRequest = { message = null },
            title = { Text(message ?: "") },
            text = { Text("此复现版本保留了该入口与信息结构。") },
            confirmButton = { TextButton(onClick = { message = null }) { Text("确定") } }
        )
    }
}

private fun loadHistory(prefs: SharedPreferences): List<HistoryItem> {
    val raw = prefs.getString("history", "") ?: ""
    if (raw.isBlank()) return emptyList()
    return raw.split('\u001D').mapNotNull { record ->
        val parts = record.split('\u001E', limit = 2)
        if (parts.size == 2) HistoryItem(parts[0], parts[1]) else null
    }
}

private fun saveHistory(prefs: SharedPreferences, history: List<HistoryItem>) {
    val raw = history.take(50).joinToString("\u001D") { it.expression + "\u001E" + it.result }
    prefs.edit().putString("history", raw).apply()
}

private fun convertValue(value: Double, from: UnitItem, to: UnitItem, temperature: Boolean): Double {
    if (!temperature) return value * from.factor / to.factor
    val celsius = when (from.symbol) {
        "°F" -> (value - 32.0) * 5.0 / 9.0
        "K" -> value - 273.15
        "°R" -> (value - 491.67) * 5.0 / 9.0
        else -> value
    }
    return when (to.symbol) {
        "°F" -> celsius * 9.0 / 5.0 + 32.0
        "K" -> celsius + 273.15
        "°R" -> (celsius + 273.15) * 9.0 / 5.0
        else -> celsius
    }
}

private fun formatNumber(value: Double): String {
    if (!value.isFinite()) return ""
    if (abs(value) < 1e-12) return "0"
    val rounded = kotlin.math.round(value)
    if (abs(value - rounded) < 1e-10 && abs(rounded) < 1e15) return rounded.toLong().toString()
    val symbols = DecimalFormatSymbols(Locale.US)
    return DecimalFormat("0.############", symbols).format(value)
}

private fun evaluateExpression(raw: String): Double? {
    val source = raw.filterNot { it.isWhitespace() }
    if (source.isEmpty()) return null
    if (source.endsWith("%")) {
        var split = -1
        for (index in 1 until source.length - 1) {
            if (source[index] == '+' || source[index] == '-') split = index
        }
        if (split >= 0) {
            val left = evaluateCore(source.substring(0, split)) ?: return null
            val percent = evaluateCore(source.substring(split + 1, source.length - 1)) ?: return null
            val value = if (source[split] == '+') left + left * percent / 100.0
            else left - left * percent / 100.0
            return value.takeIf { it.isFinite() }
        }
        val hasBinary = source.drop(1).any { it in listOf('×', '÷', '*', '/', '^') }
        if (!hasBinary) return null
    }
    return evaluateCore(source)
}

private fun evaluateCore(raw: String): Double? {
    return try {
        val parser = ExpressionParser(raw.replace('×', '*').replace('÷', '/').replace('−', '-'))
        val value = parser.parse()
        value.takeIf { parser.finished() && it.isFinite() }
    } catch (_: Exception) {
        null
    }
}

private class ExpressionParser(private val text: String) {
    private var position = 0

    fun finished(): Boolean = position == text.length

    fun parse(): Double {
        val value = parseExpression()
        if (!finished()) throw IllegalArgumentException("Unexpected token")
        return value
    }

    private fun parseExpression(): Double {
        var value = parseTerm()
        while (position < text.length) {
            when (text[position]) {
                '+' -> {
                    position++
                    value += parseTerm()
                }
                '-' -> {
                    position++
                    value -= parseTerm()
                }
                else -> return value
            }
        }
        return value
    }

    private fun parseTerm(): Double {
        var value = parsePower()
        while (position < text.length) {
            when (text[position]) {
                '*' -> {
                    position++
                    value *= parsePower()
                }
                '/' -> {
                    position++
                    val divisor = parsePower()
                    if (divisor == 0.0) throw ArithmeticException("division by zero")
                    value /= divisor
                }
                '√' -> {
                    position++
                    val operand = parsePower()
                    if (operand < 0) throw ArithmeticException("negative root")
                    value *= sqrt(operand)
                }
                else -> return value
            }
        }
        return value
    }

    private fun parsePower(): Double {
        var value = parseUnary()
        if (position < text.length && text[position] == '^') {
            position++
            value = value.pow(parsePower())
        }
        return value
    }

    private fun parseUnary(): Double {
        if (position < text.length && text[position] == '+') {
            position++
            return parseUnary()
        }
        if (position < text.length && text[position] == '-') {
            position++
            return -parseUnary()
        }
        if (position < text.length && text[position] == '√') {
            position++
            val operand = parseUnary()
            if (operand < 0) throw ArithmeticException("negative root")
            return sqrt(operand)
        }
        var value = parseNumber()
        if (position < text.length && text[position] == '%') {
            position++
            value /= 100.0
        }
        return value
    }

    private fun parseNumber(): Double {
        val start = position
        var dots = 0
        while (position < text.length && (text[position].isDigit() || text[position] == '.')) {
            if (text[position] == '.') dots++
            if (dots > 1) throw NumberFormatException("multiple decimal points")
            position++
        }
        if (start == position) throw NumberFormatException("number expected")
        return text.substring(start, position).toDouble()
    }
}
