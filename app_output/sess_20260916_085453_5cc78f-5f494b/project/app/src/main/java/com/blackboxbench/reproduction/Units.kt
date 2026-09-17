package com.blackboxbench.reproduction

data class UnitDef(val name: String, val symbol: String, val factor: Double)

class ConverterCategory(
    val id: String,
    val name: String,
    val units: List<UnitDef>,
    val defaultFrom: Int,
    val defaultTo: Int,
    val temperature: Boolean = false
)

object UnitData {
    val categories: List<ConverterCategory> = listOf(
        ConverterCategory(
            "length", "长度", listOf(
                UnitDef("千米", "km", 1000.0),
                UnitDef("米", "m", 1.0),
                UnitDef("厘米", "cm", 0.01),
                UnitDef("毫米", "mm", 0.001),
                UnitDef("微米", "\u03bcm", 1e-6),
                UnitDef("纳米", "nm", 1e-9),
                UnitDef("埃", "\u00c5", 1e-10),
                UnitDef("英里", "mi", 1609.344),
                UnitDef("码", "yd", 0.9144),
                UnitDef("英尺", "ft", 0.3048),
                UnitDef("英寸", "in", 0.0254),
                UnitDef("英寻", "fathom", 1.8288),
                UnitDef("海里", "NM", 1852.0),
                UnitDef("天文单位", "au", 1.495978707e11),
                UnitDef("秒差距", "pc", 3.0856775814913673e16)
            ), 0, 1
        ),
        ConverterCategory(
            "area", "面积", listOf(
                UnitDef("平方千米", "km\u00b2", 1e6),
                UnitDef("平方米", "m\u00b2", 1.0),
                UnitDef("平方厘米", "cm\u00b2", 1e-4),
                UnitDef("平方毫米", "mm\u00b2", 1e-6),
                UnitDef("平方英里", "sq mi", 2589988.110336),
                UnitDef("平方码", "sq yd", 0.83612736),
                UnitDef("平方英尺", "sq ft", 0.09290304),
                UnitDef("平方英寸", "sq in", 0.00064516),
                UnitDef("英亩", "ac", 4046.8564224),
                UnitDef("公顷", "ha", 10000.0)
            ), 0, 1
        ),
        ConverterCategory(
            "volume", "体积", listOf(
                UnitDef("立方米", "m\u00b3", 1.0),
                UnitDef("立方分米", "dm\u00b3", 0.001),
                UnitDef("立方厘米", "cm\u00b3", 1e-6),
                UnitDef("立方毫米", "mm\u00b3", 1e-9),
                UnitDef("升", "L", 0.001),
                UnitDef("厘升", "cL", 1e-5),
                UnitDef("分升", "dL", 1e-4),
                UnitDef("毫升", "mL", 1e-6),
                UnitDef("茶匙", "tsp", 4.92892159375e-6),
                UnitDef("汤匙", "tbsp", 1.478676478125e-5),
                UnitDef("英亩-英尺", "ac ft", 1233.48183754752),
                UnitDef("立方英尺", "ft\u00b3", 0.028316846592),
                UnitDef("立方英寸", "in\u00b3", 1.6387064e-5),
                UnitDef("桶（美制）", "fl bl (US)", 0.158987294928),
                UnitDef("加仑（美制）", "gal (US)", 0.003785411784),
                UnitDef("夸脱（美制）", "qt (US)", 0.000946352946),
                UnitDef("品脱（美制）", "pt (US fl)", 0.000473176473),
                UnitDef("法定杯（美制）", "cup (US)", 0.0002365882365),
                UnitDef("惯用杯（美制）", "cup (US)", 0.0002365882365),
                UnitDef("吉耳（美制）", "gi (US)", 0.000118294118),
                UnitDef("液量盎司（美制）", "US fl oz", 2.95735295625e-5),
                UnitDef("桶（英制）", "bl (imp)", 0.16365924),
                UnitDef("加仑（英制）", "gal (imp)", 0.00454609),
                UnitDef("夸脱（英制）", "qt (imp)", 0.0011365225),
                UnitDef("品脱（英制）", "pt (imp)", 0.00056826125),
                UnitDef("吉耳（英制）", "gi (imp)", 0.0001420653125),
                UnitDef("液量盎司（英制）", "fl oz (imp)", 2.84130625e-5)
            ), 4, 0
        ),
        ConverterCategory(
            "mass", "质量", listOf(
                UnitDef("克", "g", 0.001),
                UnitDef("千克", "kg", 1.0),
                UnitDef("毫克", "mg", 1e-6),
                UnitDef("微克", "\u03bcg", 1e-9),
                UnitDef("公吨", "t", 1000.0),
                UnitDef("磅", "lb", 0.45359237),
                UnitDef("盎司", "oz", 0.028349523125),
                UnitDef("格令", "gr", 6.479891e-5),
                UnitDef("打兰", "dr", 0.0017718451953125),
                UnitDef("英石", "st", 6.35029318),
                UnitDef("长吨", "ton", 1016.0469088),
                UnitDef("短吨", "sh tn", 907.18474),
                UnitDef("克拉", "kt", 0.0002),
                UnitDef("克拉（公制）", "ct", 0.0002),
                UnitDef("市斤", "jin", 0.5)
            ), 5, 1
        ),
        ConverterCategory(
            "temperature", "温度", listOf(
                UnitDef("摄氏度", "\u00b0C", 1.0),
                UnitDef("华氏度", "\u00b0F", 1.0),
                UnitDef("开尔文", "K", 1.0)
            ), 0, 2, temperature = true
        ),
        ConverterCategory(
            "time", "时间", listOf(
                UnitDef("小时", "h", 3600.0),
                UnitDef("分钟", "m", 60.0),
                UnitDef("秒", "s", 1.0),
                UnitDef("毫秒", "ms", 0.001),
                UnitDef("天", "d", 86400.0),
                UnitDef("周", "wk", 604800.0),
                UnitDef("年", "y", 31536000.0)
            ), 0, 2
        ),
        ConverterCategory(
            "speed", "速度", listOf(
                UnitDef("米/秒", "m/s", 1.0),
                UnitDef("千米/秒", "km/s", 1000.0),
                UnitDef("千米/时", "km/h", 0.2777777777777778),
                UnitDef("英里/时", "mph", 0.44704),
                UnitDef("节", "kn", 0.5144444444444445),
                UnitDef("英尺/秒", "ft/s", 0.3048),
                UnitDef("马赫", "Ma", 340.29),
                UnitDef("光速", "c", 299792458.0)
            ), 2, 3
        ),
        ConverterCategory(
            "pressure", "压强", listOf(
                UnitDef("帕斯卡", "Pa", 1.0),
                UnitDef("千帕", "kPa", 1000.0),
                UnitDef("兆帕", "MPa", 1e6),
                UnitDef("巴", "bar", 100000.0),
                UnitDef("豪巴", "mbar", 100.0),
                UnitDef("气压", "atm", 101325.0),
                UnitDef("磅/平方英寸", "psi", 6894.757293168),
                UnitDef("托尔", "Torr", 133.32236842105263),
                UnitDef("毫米汞柱", "mmHg", 133.322387415),
                UnitDef("英寸汞柱", "inHg", 3386.389)
            ), 3, 6
        ),
        ConverterCategory(
            "energy", "能量", listOf(
                UnitDef("焦耳", "J", 1.0),
                UnitDef("千焦", "kJ", 1000.0),
                UnitDef("兆焦", "MJ", 1e6),
                UnitDef("吉焦", "GJ", 1e9),
                UnitDef("卡路里", "cal", 4.184),
                UnitDef("千卡", "kcal", 4184.0),
                UnitDef("瓦时", "Wh", 3600.0),
                UnitDef("千瓦时", "kWh", 3600000.0),
                UnitDef("兆瓦时", "MWh", 3.6e9),
                UnitDef("电子伏特", "eV", 1.602176634e-19),
                UnitDef("英制热量单位", "BTU", 1055.05585262),
                UnitDef("热单位", "thm", 1.05505585262e8),
                UnitDef("英尺磅", "ft\u00b7lbf", 1.3558179483314004),
                UnitDef("尔格", "erg", 1e-7)
            ), 5, 1
        )
    )

    fun byId(id: String): ConverterCategory =
        categories.firstOrNull { it.id == id } ?: categories[0]

    fun convert(cat: ConverterCategory, value: Double, from: Int, to: Int): Double {
        if (cat.temperature) return convertTemperature(value, from, to)
        val f = cat.units[from].factor
        val t = cat.units[to].factor
        return value * f / t
    }

    private fun convertTemperature(value: Double, from: Int, to: Int): Double {
        val c = when (from) {
            0 -> value
            1 -> (value - 32.0) * 5.0 / 9.0
            else -> value - 273.15
        }
        return when (to) {
            0 -> c
            1 -> c * 9.0 / 5.0 + 32.0
            else -> c + 273.15
        }
    }
}