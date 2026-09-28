package com.blackboxbench.reproduction

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun formatValue(v: Float, spec: ParamSpec): String {
    if (spec.decimals > 0) return String.format("%.${spec.decimals}f", v) + spec.suffix
    val n = v.toInt()
    val sign = if (n > 0) "+" else ""
    return "$sign$n${spec.suffix}"
}

private fun cycleParam(s: ToolSession, delta: Int): ToolSession {
    if (s.tool.params.isEmpty()) return s
    val n = s.tool.params.size
    val i = ((s.activeParam + delta) % n + n) % n
    return s.copy(activeParam = i)
}

private fun adjustValue(s: ToolSession, frac: Float): ToolSession {
    if (s.tool.params.isEmpty()) {
        return s.copy(amount = (s.amount + frac * 200f).coerceIn(-100f, 100f))
    }
    val spec = s.tool.params[s.activeParam]
    val v = s.params[spec.name] ?: spec.default
    val nv = (v + frac * (spec.max - spec.min)).coerceIn(spec.min, spec.max)
    return s.copy(params = s.params + (spec.name to nv))
}

@Composable
fun ToolScreen(state: AppState, session: ToolSession) {
    val tool = session.tool
    val preview by produceState<Bitmap?>(null, state.original, state.layers, session) {
        val orig = state.original
        if (orig == null) {
            value = null
            return@produceState
        }
        value = withContext(Dispatchers.Default) {
            val base = ImageOps.render(orig, state.layers)
            if (tool.special == "crop") base else ImageOps.applyLayer(base, sessionLayer(session))
        }
    }
    var area by remember { mutableStateOf(IntSize.Zero) }
    var textDialog by remember { mutableStateOf(false) }

    fun update(s: ToolSession) {
        state.tool = s
    }

    Column(modifier = Modifier.fillMaxSize().background(Palette.Bg)) {
        // top bar
        Box(modifier = Modifier.fillMaxWidth().height(56.dp)) {
            val activeSpec = tool.params.getOrNull(session.activeParam)
            if (activeSpec != null) {
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(activeSpec.name, fontSize = 15.sp, color = Palette.TextPrimary)
                    if (tool.params.size > 1) {
                        Spacer(Modifier.width(2.dp))
                        MiniGlyph("chevron", Palette.TextPrimary, Modifier.size(14.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        formatValue(session.params[activeSpec.name] ?: activeSpec.default, activeSpec),
                        fontSize = 15.sp,
                        color = Palette.TextPrimary
                    )
                }
            }
            Box(
                modifier = Modifier.align(Alignment.CenterEnd).size(56.dp)
                    .clickable { update(session.copy(showParamList = !session.showParamList)) },
                contentAlignment = Alignment.Center
            ) { MiniGlyph("params", Palette.TextPrimary, Modifier.size(24.dp)) }
        }

        Box(
            modifier = Modifier.fillMaxWidth().weight(1f)
                .onSizeChanged { area = it }
                .pointerInput(tool.id, session.activeParam, session.variant) {
                    detectDragGestures(
                        onDragStart = { },
                        onDrag = { change, amount ->
                            change.consume()
                            val w = if (area.width == 0) 1f else area.width.toFloat()
                            if (tool.special == "crop") {
                                val rect = session.crop
                                val d = amount.x / w
                                val left = (rect[0] + d).coerceIn(0f, 0.45f)
                                val right = (rect[2] - d).coerceIn(0.55f, 1f)
                                update(session.copy(crop = floatArrayOf(left, rect[1], right, rect[3])))
                            } else if (tool.special == "rotate") {
                                val spec = tool.params[0]
                                val v = (session.params["校正角度"] ?: 0f) + amount.x / w * 90f
                                update(session.copy(params = session.params + ("校正角度" to v.coerceIn(spec.min, spec.max))))
                            } else if (tool.special == "lensblur" || tool.special == "vignette" ||
                                (tool.special == "selective" && session.amount >= 1f)
                            ) {
                                val nx = (session.pointX + amount.x / w).coerceIn(0.05f, 0.95f)
                                val ny = (session.pointY + amount.y / (if (area.height == 0) 1f else area.height.toFloat())).coerceIn(0.05f, 0.95f)
                                update(session.copy(pointX = nx, pointY = ny))
                            } else if (abs(amount.y) > abs(amount.x)) {
                                update(cycleParam(session, if (amount.y < 0) 1 else -1))
                            } else {
                                update(adjustValue(session, amount.x / w * 0.8f))
                            }
                        }
                    )
                }
                .pointerInput(tool.id) {
                    detectTapGestures(
                        onTap = { offset ->
                            if (tool.special == "selective") {
                                val w = if (area.width == 0) 1f else area.width.toFloat()
                                val h = if (area.height == 0) 1f else area.height.toFloat()
                                update(
                                    session.copy(
                                        amount = 1f,
                                        pointX = (offset.x / w).coerceIn(0.05f, 0.95f),
                                        pointY = (offset.y / h).coerceIn(0.05f, 0.95f)
                                    )
                                )
                            }
                        },
                        onDoubleTap = { if (tool.special == "text") textDialog = true }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (preview != null) {
                Image(
                    bitmap = preview!!.asImageBitmapCompat(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(8.dp)
                )
            }
            if (tool.special == "crop") CropOverlay(session)
            if (tool.special == "rotate") GridOverlay()
            if (tool.special == "perspective") PerspectiveOverlay(session)
            if (tool.special == "lensblur") PointOverlay(session, area)
            if (tool.special == "vignette") PointOverlay(session, area)
            if (tool.special == "selective" && session.amount >= 1f) SelectivePointOverlay(session)

            // histogram
            MiniGlyph(
                "histogram", Color(0xFF8A8A8A),
                Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 6.dp).size(22.dp)
            )
            MiniGlyph(
                "star", Color(0xFF8A8A8A),
                Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 6.dp).size(22.dp)
            )

            if (session.showParamList && tool.params.isNotEmpty()) {
                ParamListPopup(session) { i -> update(session.copy(activeParam = i, showParamList = false)) }
            }
        }

        if (session.dialog != null) FaceDialog(state, session)

        if (tool.variants.isNotEmpty()) {
            VariantRow(session) { i ->
                var s = session.copy(variant = i)
                if (tool.special == "crop") s = applyAspect(s, i)
                if (tool.special == "perspective") s = s.copy(activeParam = i.coerceAtMost(tool.params.size - 1))
                update(s)
            }
        }

        // bottom action bar
        Row(
            modifier = Modifier.fillMaxWidth().height(60.dp).background(Palette.SheetBg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(60.dp).clickable { state.tool = null },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Close, contentDescription = "cancel", tint = Palette.TextPrimary) }

            Spacer(Modifier.weight(1f))
            ToolActions(session) { update(it) }
            Spacer(Modifier.weight(1f))

            Box(
                modifier = Modifier.size(60.dp).clickable {
                    state.commitLayer(sessionLayer(session))
                    state.tool = null
                    state.panelTab = null
                },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Check, contentDescription = "apply", tint = Palette.TextPrimary) }
        }
    }

    if (textDialog) {
        TextEditDialog(state, session, onDismiss = { textDialog = false }) { t ->
            update(session.copy(text = t)); textDialog = false
        }
    }
}

@Composable
fun ToolActions(session: ToolSession, update: (ToolSession) -> Unit) {
    val actions = toolActionIds(session.tool)
    Row(verticalAlignment = Alignment.CenterVertically) {
        actions.forEach { id ->
            Box(
                modifier = Modifier.size(52.dp).clickable {
                    when (id) {
                        "auto" -> {
                            val auto = session.tool.params.associate {
                                it.name to when {
                                    it.name.contains("亮度") || it.name.contains("曝光") -> 18f
                                    it.name.contains("对比度") -> 15f
                                    it.name.contains("饱和度") -> 12f
                                    it.name.contains("结构") -> 20f
                                    else -> it.default
                                }
                            }
                            update(session.copy(params = session.params + auto, amount = 12f))
                        }
                        "params" -> update(session.copy(showParamList = !session.showParamList))
                        "rot90" -> update(session.copy(rot90 = (session.rot90 + 90) % 360))
                        "flip" -> update(session.copy(flipH = !session.flipH))
                        "plus" -> update(session.copy(amount = (session.amount + 5f).coerceIn(-100f, 100f)))
                        "minus" -> update(session.copy(amount = (session.amount - 5f).coerceIn(-100f, 100f)))
                    }
                },
                contentAlignment = Alignment.Center
            ) { MiniGlyph(id, if (id == "params") Palette.Accent else Palette.TextPrimary, Modifier.size(24.dp)) }
        }
    }
}

fun toolActionIds(tool: ToolDef): List<String> = when (tool.id) {
    "tune" -> listOf("params", "auto")
    "details" -> listOf("params")
    "curves" -> listOf("channels", "eye", "presets")
    "whitebalance" -> listOf("awb", "params", "auto")
    "crop" -> listOf("rot90", "grid")
    "rotate" -> listOf("flip", "rot90")
    "perspective" -> listOf("trap", "square", "auto")
    "expand" -> listOf("fill")
    "selective" -> listOf("plus", "eye")
    "brush" -> listOf("brush", "minus", "plus", "eye")
    "healing" -> listOf("undo", "redo")
    "vignette" -> listOf("params")
    "lensblur" -> listOf("iris", "params", "blur")
    "double" -> listOf("image", "blend", "drop")
    "text" -> listOf("palette", "drop", "auto")
    "frames" -> listOf("frame")
    else -> if (tool.hasAuto) listOf("params", "auto") else listOf("params")
}

fun applyAspect(s: ToolSession, index: Int): ToolSession {
    val ratios = listOf(0f, 1f, 1f, 1.414f, 1.5f, 1.3333f, 1.7778f)
    val r = ratios.getOrElse(index) { 0f }
    if (index == 0) return s.copy(crop = floatArrayOf(0f, 0f, 1f, 1f))
    if (index == 1) return s.copy(crop = floatArrayOf(0f, 0f, 1f, 1f))
    val w = 1f
    val h = (1f / r).coerceIn(0.3f, 1f)
    val top = (1f - h) / 2f
    return s.copy(crop = floatArrayOf(0f, top, w, top + h))
}

@Composable
fun CropOverlay(session: ToolSession) {
    Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        val c = session.crop
        val l = size.width * c[0]
        val t = size.height * c[1]
        val r = size.width * c[2]
        val b = size.height * c[3]
        val w = size.width
        val h = size.height
        val shade = Color(0x77000000)
        drawRect(shade, Offset(0f, 0f), Size(w, t))
        drawRect(shade, Offset(0f, b), Size(w, h - b))
        drawRect(shade, Offset(0f, t), Size(l, b - t))
        drawRect(shade, Offset(r, t), Size(w - r, b - t))
        val sw = 3f
        drawLine(Color.White, Offset(l, t), Offset(r, t), sw)
        drawLine(Color.White, Offset(l, b), Offset(r, b), sw)
        drawLine(Color.White, Offset(l, t), Offset(l, b), sw)
        drawLine(Color.White, Offset(r, t), Offset(r, b), sw)
        for (i in 1..2) {
            val x = l + (r - l) * i / 3f
            val y = t + (b - t) * i / 3f
            drawLine(Color(0x88FFFFFF), Offset(x, t), Offset(x, b), 1f)
            drawLine(Color(0x88FFFFFF), Offset(l, y), Offset(r, y), 1f)
        }
    }
}

@Composable
fun GridOverlay() {
    Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        val w = size.width
        val h = size.height
        for (i in 1..6) {
            val x = w * i / 7f
            drawLine(Color(0x88FFFFFF), Offset(x, 0f), Offset(x, h), 1f)
        }
        for (i in 1..6) {
            val y = h * i / 7f
            drawLine(Color(0x88FFFFFF), Offset(0f, y), Offset(w, y), 1f)
        }
    }
}

@Composable
fun PerspectiveOverlay(session: ToolSession) {
    Canvas(modifier = Modifier.fillMaxSize().padding(40.dp)) {
        val w = size.width
        val h = size.height
        val p = Path().apply {
            moveTo(w * 0.18f, h * 0.12f); lineTo(w * 0.82f, h * 0.12f)
            lineTo(w * 0.95f, h * 0.88f); lineTo(w * 0.05f, h * 0.88f); close()
        }
        drawPath(p, Color(0x8833B5E5), style = Stroke(width = 4f))
        drawLine(Color(0xAA33B5E5), Offset(w * 0.5f, h * 0.05f), Offset(w * 0.5f, h * 0.95f), 3f)
        drawLine(Color(0xAA33B5E5), Offset(w * 0.03f, h * 0.5f), Offset(w * 0.97f, h * 0.5f), 3f)
    }
}

@Composable
fun PointOverlay(session: ToolSession, area: IntSize) {
    Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        val cx = size.width * session.pointX
        val cy = size.height * session.pointY
        val r = size.minDimension * 0.32f
        drawCircle(Color(0x5533B5E5), r, Offset(cx, cy), style = Stroke(width = 4f))
        drawCircle(Color(0xAA33B5E5), r * 0.55f, Offset(cx, cy), style = Stroke(width = 3f))
        drawCircle(Color(0xFF33B5E5), 14f, Offset(cx, cy))
    }
}

@Composable
fun SelectivePointOverlay(session: ToolSession) {
    Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        val cx = size.width * session.pointX
        val cy = size.height * session.pointY
        val r = size.minDimension * 0.10f
        drawCircle(Color(0x66000000), r + 4f, Offset(cx, cy))
        drawCircle(Color(0xFFFFFFFF), r, Offset(cx, cy))
        drawCircle(Color(0xFF33B5E5), r * 0.45f, Offset(cx, cy))
        drawLine(Color(0xFFFFFFFF), Offset(cx, cy - r * 2.5f), Offset(cx, cy - r * 1.35f), 5f)
        drawLine(Color(0xFFFFFFFF), Offset(cx, cy + r * 1.35f), Offset(cx, cy + r * 2.5f), 5f)
    }
}

@Composable
fun ParamListPopup(session: ToolSession, onSelect: (Int) -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.width(250.dp).background(Color.White)
        ) {
            MiniGlyph("chevronUp", Palette.TextSecondary, Modifier.align(Alignment.CenterHorizontally).size(18.dp))
            session.tool.params.forEachIndexed { i, spec ->
                val selected = i == session.activeParam
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(if (selected) Palette.Accent else Color.White)
                        .clickable { onSelect(i) }
                        .padding(horizontal = 12.dp, vertical = 11.dp)
                ) {
                    Text(spec.name, fontSize = 14.sp, color = if (selected) Color.White else Palette.TextPrimary)
                    Spacer(Modifier.weight(1f))
                    Text(
                        formatValue(session.params[spec.name] ?: spec.default, spec),
                        fontSize = 14.sp,
                        color = if (selected) Color.White else Palette.TextPrimary
                    )
                }
            }
            MiniGlyph("chevron", Palette.TextSecondary, Modifier.align(Alignment.CenterHorizontally).size(18.dp))
        }
    }
}

@Composable
fun VariantRow(session: ToolSession, onSelect: (Int) -> Unit) {
    if (session.tool.variants.isEmpty()) return
    Row(
        modifier = Modifier.fillMaxWidth().background(Palette.SheetBg)
            .horizontalScroll(rememberScrollState()).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        session.tool.variants.forEachIndexed { i, label ->
            val selected = i == session.variant
            Box(
                modifier = Modifier.padding(horizontal = 6.dp).clip(RoundedCornerShape(4.dp))
                    .background(if (selected) Palette.Accent else Color(0xFFE0E0E0))
                    .clickable { onSelect(i) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(label, fontSize = 13.sp, color = if (selected) Color.White else Palette.TextPrimary)
            }
        }
    }
}

@Composable
fun FaceDialog(state: AppState, session: ToolSession) {
    DialogCard {
        Text("此照片中找不到任何面孔。", fontSize = 15.sp, color = Palette.TextPrimary)
        Spacer(Modifier.height(20.dp))
        Row(modifier = Modifier.align(Alignment.End)) {
            Box(modifier = Modifier.clickable { state.tool = null }.padding(8.dp)) {
                Text("取消滤镜", fontSize = 14.sp, color = Palette.Accent)
            }
            Spacer(Modifier.width(12.dp))
            Box(modifier = Modifier.clickable { }.padding(8.dp)) {
                Text("再试一下", fontSize = 14.sp, color = Palette.Accent)
            }
        }
    }
}

@Composable
fun TextEditDialog(state: AppState, session: ToolSession, onDismiss: () -> Unit, onOk: (String) -> Unit) {
    var value by remember { mutableStateOf(session.text) }
    DialogCard {
        Text("编辑文字", fontSize = 18.sp, color = Palette.TextPrimary)
        Spacer(Modifier.height(16.dp))
        BasicTextField(
            value = value,
            onValueChange = { value = it },
            textStyle = TextStyle(fontSize = 16.sp, color = Palette.TextPrimary),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Palette.Accent))
        Spacer(Modifier.height(18.dp))
        Row(modifier = Modifier.align(Alignment.End)) {
            Box(modifier = Modifier.clickable { onDismiss() }.padding(8.dp)) {
                Text("取消", fontSize = 14.sp, color = Palette.Accent)
            }
            Spacer(Modifier.width(12.dp))
            Box(modifier = Modifier.clickable { onOk(value) }.padding(8.dp)) {
                Text("确定", fontSize = 14.sp, color = Palette.Accent)
            }
        }
    }
}

/** Compact monochrome glyphs for tool action bars and overlays. */
@Composable
fun MiniGlyph(kind: String, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = w * 0.09f
        val stroke = Stroke(width = sw)
        when (kind) {
            "params" -> for (i in 0..2) {
                val y = h * (0.25f + 0.25f * i)
                drawLine(tint, Offset(w * 0.1f, y), Offset(w * 0.9f, y), sw)
                drawCircle(tint, w * 0.1f, Offset(w * (0.3f + 0.2f * i), y))
            }
            "chevron", "chevronUp" -> {
                val p = Path().apply {
                    if (kind == "chevron") {
                        moveTo(w * 0.25f, h * 0.4f); lineTo(w * 0.5f, h * 0.65f); lineTo(w * 0.75f, h * 0.4f)
                    } else {
                        moveTo(w * 0.25f, h * 0.6f); lineTo(w * 0.5f, h * 0.35f); lineTo(w * 0.75f, h * 0.6f)
                    }
                }
                drawPath(p, tint, style = stroke)
            }
            "auto" -> {
                drawLine(tint, Offset(w * 0.2f, h * 0.8f), Offset(w * 0.72f, h * 0.28f), sw * 1.6f)
                drawCircle(tint, w * 0.06f, Offset(w * 0.8f, h * 0.2f))
                drawCircle(tint, w * 0.05f, Offset(w * 0.2f, h * 0.24f))
            }
            "eye" -> {
                val p = Path().apply {
                    moveTo(w * 0.1f, h * 0.5f)
                    cubicTo(w * 0.3f, h * 0.15f, w * 0.7f, h * 0.15f, w * 0.9f, h * 0.5f)
                    cubicTo(w * 0.7f, h * 0.85f, w * 0.3f, h * 0.85f, w * 0.1f, h * 0.5f)
                }
                drawPath(p, tint, style = stroke)
                drawCircle(tint, w * 0.12f, Offset(w * 0.5f, h * 0.5f))
            }
            "plus" -> {
                drawLine(tint, Offset(w * 0.5f, h * 0.2f), Offset(w * 0.5f, h * 0.8f), sw * 1.4f)
                drawLine(tint, Offset(w * 0.2f, h * 0.5f), Offset(w * 0.8f, h * 0.5f), sw * 1.4f)
            }
            "minus" -> drawLine(tint, Offset(w * 0.2f, h * 0.5f), Offset(w * 0.8f, h * 0.5f), sw * 1.4f)
            "undo", "redo" -> {
                val flip = kind == "redo"
                drawArc(tint, if (flip) 180f else 0f, 180f, false,
                    topLeft = Offset(w * 0.18f, h * 0.25f), size = Size(w * 0.64f, h * 0.5f), style = stroke)
                val p = Path().apply {
                    if (flip) { moveTo(w * 0.82f, h * 0.2f); lineTo(w * 0.62f, h * 0.3f); lineTo(w * 0.84f, h * 0.42f) }
                    else { moveTo(w * 0.18f, h * 0.2f); lineTo(w * 0.38f, h * 0.3f); lineTo(w * 0.16f, h * 0.42f) }
                    close()
                }
                drawPath(p, tint)
            }
            "rot90" -> {
                drawRect(tint, Offset(w * 0.2f, h * 0.3f), Size(w * 0.5f, h * 0.5f), style = stroke)
                drawArc(tint, 200f, 160f, false, topLeft = Offset(w * 0.4f, h * 0.05f),
                    size = Size(w * 0.55f, h * 0.4f), style = stroke)
            }
            "flip" -> {
                drawLine(tint, Offset(w * 0.5f, h * 0.12f), Offset(w * 0.5f, h * 0.88f), sw)
                val l = Path().apply { moveTo(w * 0.1f, h * 0.75f); lineTo(w * 0.4f, h * 0.5f); lineTo(w * 0.1f, h * 0.25f); close() }
                drawPath(l, tint)
                drawRect(tint, Offset(w * 0.6f, h * 0.25f), Size(w * 0.3f, h * 0.5f), style = stroke)
            }
            "grid" -> {
                for (i in 1..2) {
                    drawLine(tint, Offset(w * i / 3f, 0f), Offset(w * i / 3f, h), sw)
                    drawLine(tint, Offset(0f, h * i / 3f), Offset(w, h * i / 3f), sw)
                }
                drawRect(tint, Offset(0f, 0f), Size(w, h), style = stroke)
            }
            "fill" -> {
                val p = Path().apply {
                    moveTo(w * 0.15f, h * 0.55f); lineTo(w * 0.5f, h * 0.2f); lineTo(w * 0.85f, h * 0.55f)
                    lineTo(w * 0.5f, h * 0.85f); close()
                }
                drawPath(p, tint)
            }
            "drop" -> {
                val p = Path().apply {
                    moveTo(w * 0.5f, h * 0.12f)
                    cubicTo(w * 0.85f, h * 0.55f, w * 0.8f, h * 0.85f, w * 0.5f, h * 0.88f)
                    cubicTo(w * 0.2f, h * 0.85f, w * 0.15f, h * 0.55f, w * 0.5f, h * 0.12f)
                }
                drawPath(p, tint, style = stroke)
            }
            "palette" -> {
                drawCircle(tint, w * 0.4f, Offset(w * 0.5f, h * 0.5f), style = stroke)
                drawCircle(tint, w * 0.09f, Offset(w * 0.36f, h * 0.4f))
                drawCircle(tint, w * 0.09f, Offset(w * 0.62f, h * 0.42f))
                drawCircle(tint, w * 0.09f, Offset(w * 0.5f, h * 0.66f))
            }
            "frame" -> drawRect(tint, Offset(w * 0.12f, h * 0.12f), Size(w * 0.76f, h * 0.76f), style = Stroke(width = sw * 2.2f))
            "iris" -> {
                drawCircle(tint, w * 0.4f, Offset(w * 0.5f, h * 0.5f), style = stroke)
                drawCircle(tint, w * 0.18f, Offset(w * 0.5f, h * 0.5f))
            }
            "blur" -> {
                drawCircle(tint, w * 0.42f, Offset(w * 0.5f, h * 0.5f), style = Stroke(width = sw * 0.6f))
                drawCircle(tint, w * 0.24f, Offset(w * 0.5f, h * 0.5f), style = Stroke(width = sw * 0.6f))
            }
            "image" -> {
                drawRect(tint, Offset(w * 0.1f, h * 0.2f), Size(w * 0.8f, h * 0.6f), style = stroke)
                val p = Path().apply { moveTo(w * 0.18f, h * 0.72f); lineTo(w * 0.42f, h * 0.42f); lineTo(w * 0.62f, h * 0.62f); lineTo(w * 0.78f, h * 0.44f); lineTo(w * 0.86f, h * 0.72f); close() }
                drawPath(p, tint)
            }
            "blend" -> {
                drawCircle(tint, w * 0.26f, Offset(w * 0.38f, h * 0.5f), style = stroke)
                drawCircle(tint, w * 0.26f, Offset(w * 0.62f, h * 0.5f), style = stroke)
            }
            "trap" -> {
                val p = Path().apply { moveTo(w * 0.28f, h * 0.2f); lineTo(w * 0.72f, h * 0.2f); lineTo(w * 0.9f, h * 0.8f); lineTo(w * 0.1f, h * 0.8f); close() }
                drawPath(p, tint, style = stroke)
            }
            "square" -> drawRect(tint, Offset(w * 0.15f, h * 0.15f), Size(w * 0.7f, h * 0.7f), style = stroke)
            "brush" -> {
                drawLine(tint, Offset(w * 0.2f, h * 0.8f), Offset(w * 0.72f, h * 0.28f), sw * 2.2f)
                drawCircle(tint, w * 0.13f, Offset(w * 0.22f, h * 0.78f))
            }
            "channels" -> {
                drawCircle(tint, w * 0.4f, Offset(w * 0.5f, h * 0.5f), style = stroke)
                drawArc(tint, 90f, 180f, true, topLeft = Offset(w * 0.1f, h * 0.1f), size = Size(w * 0.8f, h * 0.8f))
            }
            "presets" -> {
                val p = Path().apply { moveTo(w * 0.15f, h * 0.8f); lineTo(w * 0.85f, h * 0.8f); lineTo(w * 0.5f, h * 0.2f); close() }
                drawPath(p, tint, style = stroke)
            }
            "awb" -> {
                val p = Path().apply { moveTo(w * 0.2f, h * 0.7f); lineTo(w * 0.5f, h * 0.25f); lineTo(w * 0.8f, h * 0.7f) }
                drawPath(p, tint, style = stroke)
                drawLine(tint, Offset(w * 0.28f, h * 0.58f), Offset(w * 0.72f, h * 0.58f), sw)
            }
            "histogram" -> for (i in 0 until 5) {
                val x = w * (0.1f + 0.2f * i)
                drawLine(tint, Offset(x, h * 0.9f), Offset(x, h * (0.3f + 0.12f * (i % 3))), sw * 1.6f)
            }
            "star" -> {
                val p = Path()
                val cx = w / 2f; val cy = h / 2f; val R = w * 0.42f; val r = w * 0.18f
                for (i in 0 until 10) {
                    val a = Math.toRadians((-90 + i * 36).toDouble())
                    val rad = if (i % 2 == 0) R else r
                    val x = cx + (rad * Math.cos(a)).toFloat()
                    val y = cy + (rad * Math.sin(a)).toFloat()
                    if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                }
                p.close()
                drawPath(p, tint, style = stroke)
            }
            else -> drawCircle(tint, w * 0.32f, Offset(w * 0.5f, h * 0.5f), style = stroke)
        }
    }
}
