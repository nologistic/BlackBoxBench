package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private data class UnitDefinition(
    val name: String,
    val symbol: String,
    val factor: Double
)

private data class ConversionCategory(
    val id: String,
    val title: String,
    val icon: String,
    val units: List<UnitDefinition>,
    val defaultFrom: Int,
    val defaultTo: Int,
    val temperature: Boolean = false
)

private val conversionCategories = listOf(
    ConversionCategory(
        "length", "长度", "↕",
        listOf(
            UnitDefinition("千米", "km", 1000.0),
            UnitDefinition("米", "m", 1.0),
            UnitDefinition("厘米", "cm", .01),
            UnitDefinition("毫米", "mm", .001),
            UnitDefinition("微米", "μm", 1e-6),
            UnitDefinition("纳米", "nm", 1e-9),
            UnitDefinition("埃", "Å", 1e-10),
            UnitDefinition("英里", "mi", 1609.344),
            UnitDefinition("码", "yd", .9144),
            UnitDefinition("英尺", "ft", .3048),
            UnitDefinition("英寸", "in", .0254),
            UnitDefinition("英寻", "fathom", 1.8288),
            UnitDefinition("海里", "NM", 1852.0),
            UnitDefinition("天文单位", "au", 149597870700.0),
            UnitDefinition("秒差距", "pc", 3.085677581e16),
            UnitDefinition("光年", "ly", 9.460730472e15)
        ), 0, 1
    ),
    ConversionCategory(
        "area", "面积", "□",
        listOf(
            UnitDefinition("平方千米", "km²", 1e6),
            UnitDefinition("平方米", "m²", 1.0),
            UnitDefinition("平方厘米", "cm²", 1e-4),
            UnitDefinition("平方毫米", "mm²", 1e-6),
            UnitDefinition("平方英里", "sq mi", 2589988.110336),
            UnitDefinition("平方码", "sq yd", .83612736),
            UnitDefinition("平方英尺", "sq ft", .09290304),
            UnitDefinition("平方英寸", "sq in", .00064516),
            UnitDefinition("英亩", "ac", 4046.8564224),
            UnitDefinition("公顷", "ha", 10000.0)
        ), 0, 1
    ),
    ConversionCategory(
        "volume", "体积", "●",
        listOf(
            UnitDefinition("升", "L", 1.0),
            UnitDefinition("立方米", "m³", 1000.0),
            UnitDefinition("立方分米", "dm³", 1.0),
            UnitDefinition("立方厘米", "cm³", .001),
            UnitDefinition("立方毫米", "mm³", 1e-6),
            UnitDefinition("厘升", "cL", .01),
            UnitDefinition("分升", "dL", .1),
            UnitDefinition("毫升", "mL", .001),
            UnitDefinition("英亩英尺", "acre-ft", 1233481.83754752),
            UnitDefinition("立方英尺", "ft³", 28.316846592),
            UnitDefinition("立方英寸", "in³", .016387064),
            UnitDefinition("美制桶", "US bbl", 119.240471196),
            UnitDefinition("美制加仑", "US gal", 3.785411784),
            UnitDefinition("美制夸脱", "US qt", .946352946),
            UnitDefinition("美制品脱", "US pt", .473176473),
            UnitDefinition("美制杯", "US cup", .2365882365),
            UnitDefinition("美制液量盎司", "US fl oz", .0295735295625),
            UnitDefinition("英制桶", "Imp bbl", 163.65924),
            UnitDefinition("英制加仑", "Imp gal", 4.54609),
            UnitDefinition("英制夸脱", "Imp qt", 1.1365225),
            UnitDefinition("英制品脱", "Imp pt", .56826125),
            UnitDefinition("英制杯", "Imp cup", .284130625),
            UnitDefinition("英制及耳", "Imp gill", .1420653125),
            UnitDefinition("英制液量盎司", "Imp fl oz", .0284130625)
        ), 0, 1
    ),
    ConversionCategory(
        "mass", "质量", "⚖",
        listOf(
            UnitDefinition("克", "g", .001),
            UnitDefinition("千克", "kg", 1.0),
            UnitDefinition("毫克", "mg", 1e-6),
            UnitDefinition("微克", "µg", 1e-9),
            UnitDefinition("吨", "t", 1000.0),
            UnitDefinition("磅", "lb", .45359237),
            UnitDefinition("盎司", "oz", .028349523125),
            UnitDefinition("格令", "gr", .00006479891),
            UnitDefinition("打兰", "dr", .0017718451953125),
            UnitDefinition("英石", "st", 6.35029318),
            UnitDefinition("长吨", "long ton", 1016.0469088),
            UnitDefinition("短吨", "short ton", 907.18474),
            UnitDefinition("千吨", "kt", 1000000.0),
            UnitDefinition("克拉", "ct", .0002)
        ), 5, 1
    ),
    ConversionCategory(
        "temperature", "温度", "♨",
        listOf(
            UnitDefinition("摄氏度", "°C", 1.0),
            UnitDefinition("华氏度", "°F", 1.0),
            UnitDefinition("兰金度", "°R", 1.0),
            UnitDefinition("开尔文", "K", 1.0)
        ), 0, 3, true
    ),
    ConversionCategory(
        "time", "时间", "◷",
        listOf(
            UnitDefinition("小时", "h", 3600.0),
            UnitDefinition("秒", "s", 1.0),
            UnitDefinition("分钟", "min", 60.0),
            UnitDefinition("毫秒", "ms", .001),
            UnitDefinition("天", "d", 86400.0),
            UnitDefinition("周", "wk", 604800.0),
            UnitDefinition("年", "y", 31557600.0)
        ), 0, 1
    ),
    ConversionCategory(
        "speed", "速度", "◴",
        listOf(
            UnitDefinition("千米每小时", "km/h", .2777777777777778),
            UnitDefinition("英里每小时", "mph", .44704),
            UnitDefinition("米每秒", "m/s", 1.0),
            UnitDefinition("千米每秒", "km/s", 1000.0),
            UnitDefinition("节", "kn", .5144444444444445),
            UnitDefinition("英尺每秒", "ft/s", .3048),
            UnitDefinition("马赫", "Ma", 340.29),
            UnitDefinition("光速", "c", 299792458.0)
        ), 0, 1
    ),
    ConversionCategory(
        "pressure", "压强", "♙",
        listOf(
            UnitDefinition("巴", "bar", 100000.0),
            UnitDefinition("磅每平方英寸", "psi", 6894.757293168),
            UnitDefinition("帕斯卡", "Pa", 1.0),
            UnitDefinition("千帕", "kPa", 1000.0),
            UnitDefinition("兆帕", "MPa", 1000000.0),
            UnitDefinition("毫巴", "mbar", 100.0),
            UnitDefinition("标准大气压", "atm", 101325.0),
            UnitDefinition("托", "Torr", 133.3223684211),
            UnitDefinition("毫米汞柱", "mmHg", 133.322387415),
            UnitDefinition("英寸汞柱", "inHg", 3386.389)
        ), 0, 1
    ),
    ConversionCategory(
        "energy", "能量", "ϟ",
        listOf(
            UnitDefinition("千卡", "kcal", 4184.0),
            UnitDefinition("千焦", "kJ", 1000.0),
            UnitDefinition("焦耳", "J", 1.0),
            UnitDefinition("兆焦", "MJ", 1e6),
            UnitDefinition("吉焦", "GJ", 1e9),
            UnitDefinition("卡路里", "cal", 4.184),
            UnitDefinition("瓦时", "Wh", 3600.0),
            UnitDefinition("千瓦时", "kWh", 3.6e6),
            UnitDefinition("兆瓦时", "MWh", 3.6e9),
            UnitDefinition("电子伏特", "eV", 1.602176634e-19),
            UnitDefinition("英热单位", "BTU", 1055.05585262),
            UnitDefinition("撒姆", "thm", 105505585.262),
            UnitDefinition("英尺磅", "ft·lbf", 1.3558179483314),
            UnitDefinition("尔格", "erg", 1e-7)
        ), 0, 1
    )
)

@Composable
private fun PageHeader(title: String, onBack: () -> Unit, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onBack, modifier = Modifier.size(52.dp)) {
            Text("‹", fontSize = 48.sp, fontWeight = FontWeight.Light, color = MaterialTheme.colorScheme.onBackground)
        }
        Text(
            title,
            fontSize = 28.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) trailing()
    }
}

@Composable
fun ConverterCategoriesScreen(onBack: () -> Unit, onCategory: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        PageHeader("单位换算", onBack)
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            conversionCategories.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(116.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    row.forEach { category ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clickable { onCategory(category.id) },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(category.icon, fontSize = 27.sp, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(10.dp))
                                Text(category.title, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun toCelsius(value: Double, symbol: String): Double = when (symbol) {
    "°F" -> (value - 32.0) * 5.0 / 9.0
    "K" -> value - 273.15
    "°R" -> (value - 491.67) * 5.0 / 9.0
    else -> value
}

private fun fromCelsius(value: Double, symbol: String): Double = when (symbol) {
    "°F" -> value * 9.0 / 5.0 + 32.0
    "K" -> value + 273.15
    "°R" -> (value + 273.15) * 9.0 / 5.0
    else -> value
}

private fun convertValue(value: Double, from: UnitDefinition, to: UnitDefinition, temperature: Boolean): Double {
    return if (temperature) fromCelsius(toCelsius(value, from.symbol), to.symbol)
    else value * from.factor / to.factor
}

private fun formatConversion(value: Double): String {
    if (!value.isFinite()) return "0"
    val cleaned = if (kotlin.math.abs(value) < 1e-14) 0.0 else value
    val plain = BigDecimal.valueOf(cleaned).stripTrailingZeros().toPlainString()
    if (plain.length > 22) return "%.10g".format(Locale.US, cleaned)
    val pieces = plain.split(".")
    val formatter = DecimalFormat("#,###", DecimalFormatSymbols(Locale.US))
    val whole = pieces[0].toLongOrNull()?.let { formatter.format(it) } ?: pieces[0]
    return if (pieces.size == 2) whole + "." + pieces[1] else whole
}

@Composable
private fun UnitPickerDialog(
    units: List<UnitDefinition>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().height(560.dp).verticalScroll(rememberScrollState())
            ) {
                units.forEachIndexed { index, unit ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(index)
                                onDismiss()
                            }
                            .padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val outlineColor = MaterialTheme.colorScheme.onSurfaceVariant
                        val selectedColor = MaterialTheme.colorScheme.primary
                        Canvas(modifier = Modifier.size(28.dp)) {
                            drawCircle(
                                color = if (index == selected) selectedColor else outlineColor,
                                radius = size.minDimension * .39f,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                            )
                            if (index == selected) {
                                drawCircle(color = selectedColor, radius = size.minDimension * .22f)
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(unit.name + " (" + unit.symbol + ")", fontSize = 18.sp)
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun ConversionPanel(
    active: Boolean,
    unit: UnitDefinition,
    value: String,
    onUnit: () -> Unit,
    modifier: Modifier
) {
    val dark = MaterialTheme.colorScheme.background.red < .2f
    val activeColor = if (dark) Color(0xFF182019) else Color(0xFFE3E8F8)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(if (active) activeColor else MaterialTheme.colorScheme.background)
            .clickable(onClick = onUnit)
            .padding(horizontal = 30.dp, vertical = 16.dp)
    ) {
        Text(
            unit.name + "  ▾",
            fontSize = 21.sp,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.align(Alignment.TopStart)
        )
        Row(
            modifier = Modifier.align(Alignment.BottomEnd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                value,
                fontSize = 45.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1
            )
            Spacer(Modifier.width(12.dp))
            Surface(shape = RoundedCornerShape(12.dp), color = if (active) MaterialTheme.colorScheme.primaryContainer else activeColor) {
                Text(
                    unit.symbol,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun ConverterKey(
    label: String,
    modifier: Modifier,
    emphasized: Boolean,
    onClick: () -> Unit
) {
    val dark = MaterialTheme.colorScheme.background.red < .2f
    val color = if (dark) {
        if (emphasized) Color(0xFF103F1A) else Color(0xFF102117)
    } else {
        if (emphasized) Color(0xFFD7DEF2) else Color(0xFFF0EFF9)
    }
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(27.dp),
        color = color
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, fontSize = 34.sp, fontWeight = FontWeight.Light, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
fun UnitConverterScreen(categoryId: String, onBack: () -> Unit) {
    val category = conversionCategories.firstOrNull { it.id == categoryId } ?: conversionCategories.first()
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("calculator_state", 0) }
    var fromIndex by rememberSaveable(category.id) {
        mutableIntStateOf(preferences.getInt("from_" + category.id, category.defaultFrom))
    }
    var toIndex by rememberSaveable(category.id) {
        mutableIntStateOf(preferences.getInt("to_" + category.id, category.defaultTo))
    }
    var input by rememberSaveable(category.id) { mutableStateOf("0") }
    var picker by rememberSaveable { mutableStateOf("") }

    val inputValue = input.toDoubleOrNull() ?: 0.0
    val outputValue = convertValue(inputValue, category.units[fromIndex], category.units[toIndex], category.temperature)
    val output = formatConversion(outputValue)

    fun enter(value: String) {
        input = if (input == "0") value else input + value
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        PageHeader(category.title, onBack)
        ConversionPanel(
            active = true,
            unit = category.units[fromIndex],
            value = input,
            onUnit = { picker = "from" },
            modifier = Modifier.height(145.dp).then(
                Modifier
            )
        )
        Box(modifier = Modifier.fillMaxWidth().height(66.dp), contentAlignment = Alignment.Center) {
            TextButton(onClick = {
                val old = fromIndex
                fromIndex = toIndex
                toIndex = old
                preferences.edit()
                    .putInt("from_" + category.id, fromIndex)
                    .putInt("to_" + category.id, toIndex)
                    .apply()
            }) {
                Text("↕", fontSize = 34.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
        ConversionPanel(
            active = false,
            unit = category.units[toIndex],
            value = output,
            onUnit = { picker = "to" },
            modifier = Modifier.height(165.dp)
        )

        Spacer(Modifier.weight(1f))
        val keys = listOf(
            listOf("7", "8", "9"),
            listOf("4", "5", "6"),
            listOf("1", "2", "3"),
            listOf("0", ".", "C")
        )
        Column(
            modifier = Modifier.fillMaxWidth().height(333.dp).padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            keys.forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    row.forEachIndexed { columnIndex, key ->
                        ConverterKey(
                            label = key,
                            modifier = Modifier.weight(1f).fillMaxSize(),
                            emphasized = rowIndex == 3 && columnIndex > 0,
                            onClick = {
                                when (key) {
                                    "." -> if (!input.contains(".")) input += "."
                                    "C" -> input = if (input.length > 1) input.dropLast(1) else "0"
                                    else -> enter(key)
                                }
                            }
                        )
                    }
                }
            }
        }
        if (category.temperature) {
            Spacer(Modifier.height(10.dp))
            ConverterKey(
                label = "+/−",
                modifier = Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 8.dp),
                emphasized = true,
                onClick = {
                    input = if (input.startsWith("-")) input.drop(1) else if (input == "0") input else "-" + input
                }
            )
        }
        Spacer(Modifier.height(12.dp))
    }

    if (picker.isNotBlank()) {
        val selected = if (picker == "from") fromIndex else toIndex
        UnitPickerDialog(
            units = category.units,
            selected = selected,
            onSelect = { index ->
                if (picker == "from") {
                    fromIndex = index
                    preferences.edit().putInt("from_" + category.id, index).apply()
                } else {
                    toIndex = index
                    preferences.edit().putInt("to_" + category.id, index).apply()
                }
            },
            onDismiss = { picker = "" }
        )
    }
}
