package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun ConverterHomeScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    Box(Modifier.fillMaxSize().background(PageBg)) {
        Column(Modifier.fillMaxSize()) {
            ScreenTitle("单位换算", onBack)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                UnitData.categories.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        row.forEach { cat ->
                            ConverterCard(cat, Modifier.weight(1f)) { onOpen(cat.id) }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConverterCard(cat: ConverterCategory, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .aspectRatio(0.92f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF4EFF8))
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CategoryIcon(cat.id, Color(0xFF7C86C9), 42)
        Spacer(Modifier.height(10.dp))
        Text(cat.name, color = Ink, fontSize = 19.sp)
    }
}

@Composable
fun CategoryIcon(id: String, color: Color, sizeDp: Int) {
    Canvas(Modifier.size(sizeDp.dp)) {
        val s = size.minDimension
        val stroke = s * 0.09f
        when (id) {
            "length" -> {
                drawLine(color, Offset(s * 0.5f, s * 0.15f), Offset(s * 0.5f, s * 0.85f), stroke, StrokeCap.Round)
                drawLine(color, Offset(s * 0.3f, s * 0.32f), Offset(s * 0.5f, s * 0.15f), stroke, StrokeCap.Round)
                drawLine(color, Offset(s * 0.7f, s * 0.32f), Offset(s * 0.5f, s * 0.15f), stroke, StrokeCap.Round)
                drawLine(color, Offset(s * 0.3f, s * 0.68f), Offset(s * 0.5f, s * 0.85f), stroke, StrokeCap.Round)
                drawLine(color, Offset(s * 0.7f, s * 0.68f), Offset(s * 0.5f, s * 0.85f), stroke, StrokeCap.Round)
            }
            "area" -> drawRect(
                color,
                topLeft = Offset(s * 0.2f, s * 0.2f),
                size = androidx.compose.ui.geometry.Size(s * 0.6f, s * 0.6f),
                style = Stroke(stroke)
            )
            "volume" -> {
                val p = Path()
                p.moveTo(s * 0.5f, s * 0.15f)
                p.cubicTo(s * 0.9f, s * 0.6f, s * 0.75f, s * 0.88f, s * 0.5f, s * 0.88f)
                p.cubicTo(s * 0.25f, s * 0.88f, s * 0.1f, s * 0.6f, s * 0.5f, s * 0.15f)
                drawPath(p, color)
            }
            "mass" -> {
                drawLine(color, Offset(s * 0.25f, s * 0.8f), Offset(s * 0.75f, s * 0.8f), stroke, StrokeCap.Round)
                drawLine(color, Offset(s * 0.35f, s * 0.8f), Offset(s * 0.5f, s * 0.3f), stroke, StrokeCap.Round)
                drawLine(color, Offset(s * 0.65f, s * 0.8f), Offset(s * 0.5f, s * 0.3f), stroke, StrokeCap.Round)
            }
            "temperature" -> {
                drawLine(color, Offset(s * 0.5f, s * 0.2f), Offset(s * 0.5f, s * 0.65f), stroke, StrokeCap.Round)
                drawCircle(color, s * 0.16f, Offset(s * 0.5f, s * 0.78f))
            }
            "time" -> {
                drawCircle(color, s * 0.36f, Offset(s * 0.5f, s * 0.5f), style = Stroke(stroke))
                drawLine(color, Offset(s * 0.5f, s * 0.5f), Offset(s * 0.5f, s * 0.3f), stroke, StrokeCap.Round)
                drawLine(color, Offset(s * 0.5f, s * 0.5f), Offset(s * 0.68f, s * 0.5f), stroke, StrokeCap.Round)
            }
            "speed" -> {
                val p = Path()
                p.moveTo(s * 0.15f, s * 0.7f)
                p.quadraticBezierTo(s * 0.5f, s * 0.05f, s * 0.85f, s * 0.7f)
                drawPath(p, color, style = Stroke(stroke))
                drawLine(color, Offset(s * 0.5f, s * 0.6f), Offset(s * 0.68f, s * 0.42f), stroke, StrokeCap.Round)
            }
            "pressure" -> {
                drawCircle(color, s * 0.36f, Offset(s * 0.5f, s * 0.5f), style = Stroke(stroke))
                drawLine(color, Offset(s * 0.5f, s * 0.5f), Offset(s * 0.62f, s * 0.36f), stroke, StrokeCap.Round)
                drawLine(color, Offset(s * 0.36f, s * 0.86f), Offset(s * 0.64f, s * 0.86f), stroke, StrokeCap.Round)
            }
            else -> {
                val p = Path()
                p.moveTo(s * 0.55f, s * 0.12f)
                p.lineTo(s * 0.3f, s * 0.55f)
                p.lineTo(s * 0.48f, s * 0.55f)
                p.lineTo(s * 0.42f, s * 0.88f)
                p.lineTo(s * 0.72f, s * 0.45f)
                p.lineTo(s * 0.52f, s * 0.45f)
                p.close()
                drawPath(p, color)
            }
        }
    }
}

@Composable
fun ConverterCategoryScreen(catId: String, prefs: Prefs, onBack: () -> Unit) {
    val cat = remember { UnitData.byId(catId) }
    var fromIndex by remember { mutableIntStateOf(prefs.converterFrom(cat.id, cat.defaultFrom)) }
    var toIndex by remember { mutableIntStateOf(prefs.converterTo(cat.id, cat.defaultTo)) }
    var input by remember { mutableStateOf("") }
    var picker by remember { mutableStateOf<Int?>(null) }

    val value = input.toDoubleOrNull() ?: 0.0
    val converted = UnitData.convert(cat, value, fromIndex, toIndex)

    Box(Modifier.fillMaxSize().background(PageBg)) {
        Column(Modifier.fillMaxSize()) {
            ScreenTitle(cat.name, onBack)

            Column(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FieldBg)
                        .padding(horizontal = 24.dp, vertical = 18.dp)
                ) {
                    UnitSelector(cat.units[fromIndex]) { picker = 0 }
                    Spacer(Modifier.height(26.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        ValuePill(Num.group(displayInput(input)), cat.units[fromIndex].symbol)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconTap(onClick = {
                        val tmp = fromIndex
                        fromIndex = toIndex
                        toIndex = tmp
                        prefs.setConverterFrom(cat.id, fromIndex)
                        prefs.setConverterTo(cat.id, toIndex)
                    }) { SwapGlyph(Color(0xFF7C86C9)) }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 10.dp)
                ) {
                    UnitSelector(cat.units[toIndex]) { picker = 1 }
                    Spacer(Modifier.height(26.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        ValuePill(convertDisplay(converted), cat.units[toIndex].symbol)
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 46.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ConverterKeyRow(listOf("7", "8", "9")) { label -> applyConverterKey(label, input) { input = it } }
                ConverterKeyRow(listOf("4", "5", "6")) { label -> applyConverterKey(label, input) { input = it } }
                ConverterKeyRow(listOf("1", "2", "3")) { label -> applyConverterKey(label, input) { input = it } }
                ConverterKeyRow(listOf("0", ".", "C")) { label -> applyConverterKey(label, input) { input = it } }
                if (cat.temperature) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(KeyDark)
                            .clickable {
                                input = if (input.startsWith("-")) input.substring(1) else "-$input"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+/-", color = Ink, fontSize = 30.sp)
                    }
                }
            }
        }
    }

    picker?.let { which ->
        UnitPickerDialog(
            units = cat.units,
            selected = if (which == 0) fromIndex else toIndex,
            onPick = { idx ->
                if (which == 0) {
                    fromIndex = idx
                    prefs.setConverterFrom(cat.id, idx)
                } else {
                    toIndex = idx
                    prefs.setConverterTo(cat.id, idx)
                }
                picker = null
            },
            onDismiss = { picker = null }
        )
    }
}

private fun applyConverterKey(k: String, input: String, update: (String) -> Unit) {
    when (k) {
        "C" -> update("")
        "." -> if (!input.contains('.')) update(if (input.isEmpty()) "." else "$input.")
        else -> {
            val next = when {
                input == "0" -> k
                else -> input + k
            }
            update(next)
        }
    }
}

@Composable
private fun ConverterKeyRow(keys: List<String>, onClick: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        keys.forEach { k ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1.45f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(KeyLight)
                    .clickable { onClick(k) },
                contentAlignment = Alignment.Center
            ) {
                Text(k, color = Ink, fontSize = 32.sp)
            }
        }
    }
}

@Composable
private fun UnitSelector(unit: UnitDef, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Text(unit.name, color = Ink, fontSize = 24.sp)
        Spacer(Modifier.width(8.dp))
        Canvas(Modifier.size(14.dp)) {
            val p = Path()
            p.moveTo(0f, size.height * 0.3f)
            p.lineTo(size.width, size.height * 0.3f)
            p.lineTo(size.width / 2f, size.height * 0.85f)
            p.close()
            drawPath(p, Color(0xFF7C86C9))
        }
    }
}

@Composable
private fun SwapGlyph(color: Color) {
    Canvas(Modifier.size(30.dp)) {
        val s = size.minDimension
        val stroke = s * 0.08f
        drawLine(color, Offset(s * 0.42f, s * 0.15f), Offset(s * 0.42f, s * 0.85f), stroke, StrokeCap.Round)
        drawLine(color, Offset(s * 0.3f, s * 0.28f), Offset(s * 0.42f, s * 0.15f), stroke, StrokeCap.Round)
        drawLine(color, Offset(s * 0.54f, s * 0.28f), Offset(s * 0.42f, s * 0.15f), stroke, StrokeCap.Round)
        drawLine(color, Offset(s * 0.6f, s * 0.15f), Offset(s * 0.6f, s * 0.85f), stroke, StrokeCap.Round)
        drawLine(color, Offset(s * 0.48f, s * 0.72f), Offset(s * 0.6f, s * 0.85f), stroke, StrokeCap.Round)
        drawLine(color, Offset(s * 0.72f, s * 0.72f), Offset(s * 0.6f, s * 0.85f), stroke, StrokeCap.Round)
    }
}

@Composable
private fun UnitPickerDialog(
    units: List<UnitDef>,
    selected: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFFF2F1FA))
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                units.forEachIndexed { index, unit ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(index) }
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioDot(selected == index)
                        Spacer(Modifier.width(20.dp))
                        Text("${unit.name} (${unit.symbol})", color = Ink, fontSize = 20.sp)
                    }
                }
            }
        }
    }
}

private fun displayInput(input: String): String {
    if (input.isEmpty()) return "0"
    val negative = input.startsWith("-")
    val body = if (negative) input.substring(1) else input
    val parts = body.split('.')
    val intPart = parts[0].ifEmpty { "0" }
    val grouped = Num.group(intPart)
    val frac = if (parts.size > 1) "." + parts[1] else ""
    return (if (negative) "-" else "") + grouped + frac
}

private fun convertDisplay(v: Double): String {
    if (!v.isFinite()) return "错误"
    val bd = java.math.BigDecimal(v)
        .round(java.math.MathContext(15, java.math.RoundingMode.HALF_UP))
        .stripTrailingZeros()
    if (bd.scale() < 0) return Num.group(bd.setScale(0).toPlainString())
    return Num.group(bd.toPlainString())
}