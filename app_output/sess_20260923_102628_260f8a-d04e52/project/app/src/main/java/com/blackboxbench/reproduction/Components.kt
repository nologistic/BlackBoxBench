package com.blackboxbench.reproduction

import android.app.Activity
import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

object C {
    val blue = Color(0xFF1E88E5)
    val blueDark = Color(0xFF2A6592)
    val green = Color(0xFF2E7D32)
    val amber = Color(0xFFF9A825)
    val red = Color(0xFFD32F2F)
    val redSoft = Color(0xFFE53935)
    val text = Color(0xFF1B1B1B)
    val textDim = Color(0xFF757575)
    val textFaint = Color(0xFF9E9E9E)
    val divider = Color(0xFFE3E3E3)
    val banner = Color(0xFFE7E9EE)
    val barBg = Color(0xFFF0EEF6)
    val screenBg = Color(0xFFFFFFFF)
    val rowSelected = Color(0xFFE0E0E0)
    val chipBg = Color(0xFFE7E7EC)
    val sheetBg = Color(0xFFF7F5FB)

    val palette = listOf(
        0xFFD32F2F, 0xFFE53935, 0xFFF4511E, 0xFFF57C00, 0xFFFB8C00,
        0xFFF9A825, 0xFFFDD835, 0xFFD4E157, 0xFF9CCC65, 0xFF7CB342,
        0xFF43A047, 0xFF2E7D32, 0xFF00897B, 0xFF26A69A, 0xFF00ACC1,
        0xFF29B6F6, 0xFF1E88E5, 0xFF3949AB, 0xFF7986CB, 0xFFB39DDB,
        0xFF8E24AA, 0xFFAB47BC, 0xFFC2185B, 0xFFEC407A, 0xFFF06292,
        0xFFBDBDBD, 0xFFA1887F, 0xFF757575, 0xFF546E7A, 0xFF000000,
    )
}

fun priorityColor(p: Int): Color = when (p) {
    3 -> C.redSoft
    2 -> Color(0xFFFBC02D)
    1 -> C.blue
    else -> Color(0xFF9E9E9E)
}

// ------------------------------------------------------------------ icons

/**
 * Hand drawn stand-ins for the iconography observed on screen. Keeps the
 * artwork self contained (no network, no external icon fonts).
 */
@Composable
fun DrawIcon(name: String, tint: Color, size: Dp = 24.dp, stroke: Dp = 1.8.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = stroke.toPx()
        val s = Stroke(width = sw, cap = StrokeCap.Round)
        when (name) {
            "menu" -> {
                for (i in 0..2) {
                    val y = h * (0.28f + 0.22f * i)
                    drawLine(tint, Offset(w * 0.14f, y), Offset(w * 0.86f, y), sw, StrokeCap.Round)
                }
            }
            "search" -> {
                drawCircle(tint, w * 0.27f, Offset(w * 0.44f, h * 0.44f), style = s)
                drawLine(tint, Offset(w * 0.63f, h * 0.63f), Offset(w * 0.86f, h * 0.86f), sw, StrokeCap.Round)
            }
            "gear" -> {
                // Cog: toothed ring drawn with a thick stroke so it reads as a
                // gear rather than a sun (rays + thin circle).
                val cx = w / 2
                val cy = h / 2
                for (i in 0 until 8) {
                    val a = Math.toRadians((i * 45).toDouble())
                    val ca = kotlin.math.cos(a).toFloat()
                    val sa = kotlin.math.sin(a).toFloat()
                    drawLine(
                        tint,
                        Offset(cx + (w * 0.26f) * ca, cy + (h * 0.26f) * sa),
                        Offset(cx + (w * 0.44f) * ca, cy + (h * 0.44f) * sa),
                        sw * 1.5f,
                        StrokeCap.Round
                    )
                }
                drawCircle(tint, w * 0.25f, Offset(cx, cy), style = Stroke(width = sw * 1.5f))
            }
            "sort" -> {
                drawLine(tint, Offset(w * 0.3f, h * 0.2f), Offset(w * 0.3f, h * 0.82f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.3f, h * 0.2f), Offset(w * 0.16f, h * 0.36f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.3f, h * 0.2f), Offset(w * 0.44f, h * 0.36f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.7f, h * 0.82f), Offset(w * 0.7f, h * 0.2f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.7f, h * 0.82f), Offset(w * 0.56f, h * 0.66f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.7f, h * 0.82f), Offset(w * 0.84f, h * 0.66f), sw, StrokeCap.Round)
            }
            "mic" -> {
                drawRoundRect(
                    tint, Offset(w * 0.36f, h * 0.12f), Size(w * 0.28f, h * 0.44f),
                    androidx.compose.ui.geometry.CornerRadius(w * 0.14f), style = s
                )
                drawArc(
                    tint, 0f, 180f, false,
                    Offset(w * 0.22f, h * 0.34f), Size(w * 0.56f, h * 0.44f), style = s
                )
                drawLine(tint, Offset(w * 0.5f, h * 0.78f), Offset(w * 0.5f, h * 0.9f), sw, StrokeCap.Round)
            }
            "more" -> {
                for (i in 0..2) drawCircle(tint, sw * 0.9f, Offset(w / 2, h * (0.25f + 0.25f * i)))
            }
            "plus" -> {
                drawLine(tint, Offset(w * 0.5f, h * 0.2f), Offset(w * 0.5f, h * 0.8f), sw * 1.3f, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.2f, h * 0.5f), Offset(w * 0.8f, h * 0.5f), sw * 1.3f, StrokeCap.Round)
            }
            "back" -> {
                drawLine(tint, Offset(w * 0.72f, h * 0.5f), Offset(w * 0.3f, h * 0.5f), sw * 1.2f, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.3f, h * 0.5f), Offset(w * 0.5f, h * 0.28f), sw * 1.2f, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.3f, h * 0.5f), Offset(w * 0.5f, h * 0.72f), sw * 1.2f, StrokeCap.Round)
            }
            "close" -> {
                drawLine(tint, Offset(w * 0.24f, h * 0.24f), Offset(w * 0.76f, h * 0.76f), sw * 1.2f, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.76f, h * 0.24f), Offset(w * 0.24f, h * 0.76f), sw * 1.2f, StrokeCap.Round)
            }
            "trash" -> {
                drawLine(tint, Offset(w * 0.2f, h * 0.28f), Offset(w * 0.8f, h * 0.28f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.4f, h * 0.28f), Offset(w * 0.42f, h * 0.16f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.6f, h * 0.28f), Offset(w * 0.58f, h * 0.16f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.42f, h * 0.16f), Offset(w * 0.58f, h * 0.16f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.26f, h * 0.28f), Offset(w * 0.31f, h * 0.86f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.74f, h * 0.28f), Offset(w * 0.69f, h * 0.86f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.31f, h * 0.86f), Offset(w * 0.69f, h * 0.86f), sw, StrokeCap.Round)
            }
            "save" -> {
                drawRoundRect(
                    tint, Offset(w * 0.18f, h * 0.18f), Size(w * 0.64f, h * 0.64f),
                    androidx.compose.ui.geometry.CornerRadius(w * 0.12f), style = s
                )
                drawLine(tint, Offset(w * 0.34f, h * 0.18f), Offset(w * 0.34f, h * 0.42f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.34f, h * 0.42f), Offset(w * 0.66f, h * 0.42f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.34f, h * 0.66f), Offset(w * 0.66f, h * 0.66f), sw, StrokeCap.Round)
            }
            "repeat" -> {
                drawArc(tint, 200f, 140f, false, Offset(w * 0.14f, h * 0.16f), Size(w * 0.72f, h * 0.68f), style = s)
                drawArc(tint, 20f, 140f, false, Offset(w * 0.14f, h * 0.16f), Size(w * 0.72f, h * 0.68f), style = s)
                drawLine(tint, Offset(w * 0.1f, h * 0.42f), Offset(w * 0.24f, h * 0.3f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.1f, h * 0.42f), Offset(w * 0.26f, h * 0.5f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.9f, h * 0.58f), Offset(w * 0.76f, h * 0.7f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.9f, h * 0.58f), Offset(w * 0.74f, h * 0.5f), sw, StrokeCap.Round)
            }
            "calendar" -> {
                drawRoundRect(
                    tint, Offset(w * 0.16f, h * 0.22f), Size(w * 0.68f, h * 0.6f),
                    androidx.compose.ui.geometry.CornerRadius(w * 0.1f), style = s
                )
                drawLine(tint, Offset(w * 0.16f, h * 0.42f), Offset(w * 0.84f, h * 0.42f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.34f, h * 0.12f), Offset(w * 0.34f, h * 0.3f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.66f, h * 0.12f), Offset(w * 0.66f, h * 0.3f), sw, StrokeCap.Round)
            }
            "calendar_off" -> {
                drawRoundRect(
                    tint, Offset(w * 0.16f, h * 0.22f), Size(w * 0.68f, h * 0.6f),
                    androidx.compose.ui.geometry.CornerRadius(w * 0.1f), style = s
                )
                drawLine(tint, Offset(w * 0.16f, h * 0.42f), Offset(w * 0.84f, h * 0.42f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.34f, h * 0.12f), Offset(w * 0.34f, h * 0.3f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.66f, h * 0.12f), Offset(w * 0.66f, h * 0.3f), sw, StrokeCap.Round)
            }
            "clock" -> {
                drawCircle(tint, w * 0.34f, Offset(w / 2, h / 2), style = s)
                drawLine(tint, Offset(w / 2, h / 2), Offset(w / 2, h * 0.28f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w / 2, h / 2), Offset(w * 0.66f, h * 0.58f), sw, StrokeCap.Round)
            }
            "timer" -> {
                drawCircle(tint, w * 0.32f, Offset(w / 2, h * 0.56f), style = s)
                drawLine(tint, Offset(w / 2, h * 0.18f), Offset(w / 2, h * 0.26f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.36f, h * 0.12f), Offset(w * 0.64f, h * 0.12f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.5f, h * 0.56f), Offset(w * 0.5f, h * 0.4f), sw * 0.9f, StrokeCap.Round)
            }
            "flag" -> {
                drawLine(tint, Offset(w * 0.26f, h * 0.14f), Offset(w * 0.26f, h * 0.88f), sw, StrokeCap.Round)
                val p = Path().apply {
                    moveTo(w * 0.26f, h * 0.18f)
                    lineTo(w * 0.82f, h * 0.3f)
                    lineTo(w * 0.26f, h * 0.5f)
                    close()
                }
                drawPath(p, tint, style = s)
            }
            "tag" -> {
                val p = Path().apply {
                    moveTo(w * 0.14f, h * 0.36f)
                    lineTo(w * 0.42f, h * 0.14f)
                    lineTo(w * 0.86f, h * 0.14f)
                    lineTo(w * 0.86f, h * 0.58f)
                    lineTo(w * 0.62f, h * 0.86f)
                    lineTo(w * 0.14f, h * 0.36f)
                    close()
                }
                drawPath(p, tint, style = s)
                drawCircle(tint, w * 0.055f, Offset(w * 0.68f, h * 0.32f))
            }
            "place" -> {
                val p = Path().apply {
                    moveTo(w * 0.5f, h * 0.9f)
                    cubicTo(w * 0.14f, h * 0.5f, w * 0.2f, h * 0.12f, w * 0.5f, h * 0.12f)
                    cubicTo(w * 0.8f, h * 0.12f, w * 0.86f, h * 0.5f, w * 0.5f, h * 0.9f)
                    close()
                }
                drawPath(p, tint, style = s)
                drawCircle(tint, w * 0.08f, Offset(w * 0.5f, h * 0.38f), style = s)
            }
            "attach" -> {
                drawArc(tint, 110f, 320f, false, Offset(w * 0.1f, h * 0.3f), Size(w * 0.5f, h * 0.5f), style = s)
                drawArc(tint, 110f, 320f, false, Offset(w * 0.3f, h * 0.16f), Size(w * 0.5f, h * 0.5f), style = s)
            }
            "bell" -> {
                val p = Path().apply {
                    moveTo(w * 0.22f, h * 0.7f)
                    lineTo(w * 0.78f, h * 0.7f)
                    lineTo(w * 0.7f, h * 0.56f)
                    lineTo(w * 0.7f, h * 0.38f)
                    cubicTo(w * 0.7f, h * 0.16f, w * 0.3f, h * 0.16f, w * 0.3f, h * 0.38f)
                    lineTo(w * 0.3f, h * 0.56f)
                    close()
                }
                drawPath(p, tint, style = s)
                drawArc(tint, 0f, 180f, false, Offset(w * 0.38f, h * 0.66f), Size(w * 0.24f, h * 0.2f), style = s)
            }
            "note" -> {
                drawLine(tint, Offset(w * 0.18f, h * 0.24f), Offset(w * 0.82f, h * 0.24f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.18f, h * 0.5f), Offset(w * 0.82f, h * 0.5f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.18f, h * 0.76f), Offset(w * 0.6f, h * 0.76f), sw, StrokeCap.Round)
            }
            "subtask" -> {
                drawLine(tint, Offset(w * 0.24f, h * 0.16f), Offset(w * 0.24f, h * 0.62f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.24f, h * 0.62f), Offset(w * 0.8f, h * 0.62f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.8f, h * 0.62f), Offset(w * 0.66f, h * 0.46f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.8f, h * 0.62f), Offset(w * 0.66f, h * 0.78f), sw, StrokeCap.Round)
            }
            "filter" -> {
                drawLine(tint, Offset(w * 0.16f, h * 0.28f), Offset(w * 0.84f, h * 0.28f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.3f, h * 0.5f), Offset(w * 0.7f, h * 0.5f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.42f, h * 0.72f), Offset(w * 0.58f, h * 0.72f), sw, StrokeCap.Round)
            }
            "list" -> {
                for (i in 0..2) {
                    drawCircle(tint, sw * 0.7f, Offset(w * 0.2f, h * (0.28f + 0.22f * i)))
                    drawLine(
                        tint, Offset(w * 0.42f, h * (0.28f + 0.22f * i)),
                        Offset(w * 0.84f, h * (0.28f + 0.22f * i)), sw, StrokeCap.Round
                    )
                }
            }
            "list_slash" -> {
                for (i in 0..2) {
                    drawCircle(tint, sw * 0.7f, Offset(w * 0.2f, h * (0.28f + 0.22f * i)))
                    drawLine(
                        tint, Offset(w * 0.42f, h * (0.28f + 0.22f * i)),
                        Offset(w * 0.84f, h * (0.28f + 0.22f * i)), sw, StrokeCap.Round
                    )
                }
                drawLine(tint, Offset(w * 0.12f, h * 0.84f), Offset(w * 0.88f, h * 0.2f), sw, StrokeCap.Round)
            }
            "check" -> {
                drawLine(tint, Offset(w * 0.2f, h * 0.54f), Offset(w * 0.42f, h * 0.76f), sw * 1.3f, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.42f, h * 0.76f), Offset(w * 0.82f, h * 0.26f), sw * 1.3f, StrokeCap.Round)
            }
            "chevron_down" -> {
                drawLine(tint, Offset(w * 0.28f, h * 0.42f), Offset(w * 0.5f, h * 0.62f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.72f, h * 0.42f), Offset(w * 0.5f, h * 0.62f), sw, StrokeCap.Round)
            }
            "chevron_up" -> {
                drawLine(tint, Offset(w * 0.28f, h * 0.58f), Offset(w * 0.5f, h * 0.38f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.72f, h * 0.58f), Offset(w * 0.5f, h * 0.38f), sw, StrokeCap.Round)
            }
            "chevron_right" -> {
                drawLine(tint, Offset(w * 0.42f, h * 0.26f), Offset(w * 0.62f, h * 0.5f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.42f, h * 0.74f), Offset(w * 0.62f, h * 0.5f), sw, StrokeCap.Round)
            }
            "chevron_left" -> {
                drawLine(tint, Offset(w * 0.58f, h * 0.26f), Offset(w * 0.38f, h * 0.5f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.58f, h * 0.74f), Offset(w * 0.38f, h * 0.5f), sw, StrokeCap.Round)
            }
            "arrow_left" -> {
                drawLine(tint, Offset(w * 0.8f, h * 0.5f), Offset(w * 0.2f, h * 0.5f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.2f, h * 0.5f), Offset(w * 0.46f, h * 0.24f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.2f, h * 0.5f), Offset(w * 0.46f, h * 0.76f), sw, StrokeCap.Round)
            }
            "arrow_up" -> {
                drawLine(tint, Offset(w * 0.5f, h * 0.82f), Offset(w * 0.5f, h * 0.2f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.5f, h * 0.2f), Offset(w * 0.26f, h * 0.44f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.5f, h * 0.2f), Offset(w * 0.74f, h * 0.44f), sw, StrokeCap.Round)
            }
            "arrow_down" -> {
                drawLine(tint, Offset(w * 0.5f, h * 0.18f), Offset(w * 0.5f, h * 0.8f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.5f, h * 0.8f), Offset(w * 0.26f, h * 0.56f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.5f, h * 0.8f), Offset(w * 0.74f, h * 0.56f), sw, StrokeCap.Round)
            }
            "play" -> {
                val p = Path().apply {
                    moveTo(w * 0.3f, h * 0.2f); lineTo(w * 0.3f, h * 0.8f)
                    lineTo(w * 0.8f, h * 0.5f); close()
                }
                drawPath(p, tint)
            }
            "pause" -> {
                drawRoundRect(
                    tint, Offset(w * 0.28f, h * 0.2f), Size(w * 0.16f, h * 0.6f),
                    androidx.compose.ui.geometry.CornerRadius(w * 0.05f)
                )
                drawRoundRect(
                    tint, Offset(w * 0.56f, h * 0.2f), Size(w * 0.16f, h * 0.6f),
                    androidx.compose.ui.geometry.CornerRadius(w * 0.05f)
                )
            }
            "inbox" -> {
                drawRoundRect(
                    tint, Offset(w * 0.14f, h * 0.2f), Size(w * 0.72f, h * 0.6f),
                    androidx.compose.ui.geometry.CornerRadius(w * 0.08f), style = Stroke(width = sw * 2.4f)
                )
                drawLine(tint, Offset(w * 0.14f, h * 0.56f), Offset(w * 0.36f, h * 0.56f), sw * 2.4f, StrokeCap.Square)
                drawLine(tint, Offset(w * 0.64f, h * 0.56f), Offset(w * 0.86f, h * 0.56f), sw * 2.4f, StrokeCap.Square)
                drawArc(
                    tint, 0f, 180f, false, Offset(w * 0.36f, h * 0.44f),
                    Size(w * 0.28f, h * 0.24f), style = Stroke(width = sw * 2.0f)
                )
            }
            "check_circle_solid" -> {
                drawCircle(tint, w * 0.44f, Offset(w / 2, h / 2))
                drawLine(Color.White, Offset(w * 0.3f, h * 0.52f), Offset(w * 0.45f, h * 0.67f), sw * 1.6f, StrokeCap.Round)
                drawLine(Color.White, Offset(w * 0.45f, h * 0.67f), Offset(w * 0.72f, h * 0.34f), sw * 1.6f, StrokeCap.Round)
            }
            "donate" -> {
                val p = Path().apply {
                    moveTo(w * 0.5f, h * 0.82f)
                    cubicTo(w * 0.08f, h * 0.5f, w * 0.2f, h * 0.14f, w * 0.5f, h * 0.34f)
                    cubicTo(w * 0.8f, h * 0.14f, w * 0.92f, h * 0.5f, w * 0.5f, h * 0.82f)
                    close()
                }
                drawPath(p, tint, style = s)
            }
            "palette" -> {
                drawCircle(tint, w * 0.34f, Offset(w / 2, h / 2), style = s)
                drawCircle(tint, w * 0.07f, Offset(w * 0.34f, h * 0.34f))
                drawCircle(tint, w * 0.07f, Offset(w * 0.66f, h * 0.36f))
                drawCircle(tint, w * 0.07f, Offset(w * 0.36f, h * 0.68f))
            }
            "widget" -> {
                for (r in 0..1) for (c in 0..1) {
                    drawRoundRect(
                        tint, Offset(w * (0.16f + 0.4f * c), h * (0.16f + 0.4f * r)),
                        Size(w * 0.28f, h * 0.28f),
                        androidx.compose.ui.geometry.CornerRadius(w * 0.05f), style = Stroke(width = sw * 1.6f)
                    )
                }
            }
            "info" -> {
                drawCircle(tint, w * 0.36f, Offset(w / 2, h / 2), style = s)
                drawCircle(tint, sw * 0.8f, Offset(w / 2, h * 0.32f))
                drawLine(tint, Offset(w / 2, h * 0.46f), Offset(w / 2, h * 0.7f), sw, StrokeCap.Round)
            }
            "help" -> {
                drawCircle(tint, w * 0.36f, Offset(w / 2, h / 2), style = s)
                drawArc(tint, 180f, 180f, false, Offset(w * 0.34f, h * 0.28f), Size(w * 0.32f, h * 0.28f), style = s)
                drawLine(tint, Offset(w * 0.5f, h * 0.56f), Offset(w * 0.5f, h * 0.64f), sw, StrokeCap.Round)
                drawCircle(tint, sw * 0.8f, Offset(w * 0.5f, h * 0.74f))
            }
            "wrench" -> {
                drawLine(tint, Offset(w * 0.3f, h * 0.74f), Offset(w * 0.68f, h * 0.3f), sw, StrokeCap.Round)
                drawArc(tint, 120f, 220f, false, Offset(w * 0.56f, h * 0.1f), Size(w * 0.34f, h * 0.34f), style = s)
                drawCircle(tint, w * 0.1f, Offset(w * 0.24f, h * 0.78f), style = s)
            }
            "briefcase" -> {
                drawRoundRect(
                    tint, Offset(w * 0.14f, h * 0.3f), Size(w * 0.72f, h * 0.48f),
                    androidx.compose.ui.geometry.CornerRadius(w * 0.08f), style = s
                )
                drawArc(tint, 180f, 180f, false, Offset(w * 0.36f, h * 0.14f), Size(w * 0.28f, h * 0.24f), style = s)
            }
            "cottage" -> {
                val p = Path().apply {
                    moveTo(w * 0.5f, h * 0.14f); lineTo(w * 0.88f, h * 0.46f)
                    lineTo(w * 0.78f, h * 0.46f); lineTo(w * 0.78f, h * 0.86f)
                    lineTo(w * 0.22f, h * 0.86f); lineTo(w * 0.22f, h * 0.46f)
                    lineTo(w * 0.12f, h * 0.46f); close()
                }
                drawPath(p, tint, style = s)
            }
            "cart" -> {
                drawLine(tint, Offset(w * 0.12f, h * 0.2f), Offset(w * 0.3f, h * 0.2f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.3f, h * 0.2f), Offset(w * 0.44f, h * 0.62f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.44f, h * 0.62f), Offset(w * 0.84f, h * 0.62f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.34f, h * 0.34f), Offset(w * 0.88f, h * 0.34f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.84f, h * 0.62f), Offset(w * 0.88f, h * 0.34f), sw, StrokeCap.Round)
                drawCircle(tint, w * 0.06f, Offset(w * 0.5f, h * 0.78f))
                drawCircle(tint, w * 0.06f, Offset(w * 0.78f, h * 0.78f))
            }
            "photo" -> {
                drawRoundRect(
                    tint, Offset(w * 0.14f, h * 0.2f), Size(w * 0.72f, h * 0.6f),
                    androidx.compose.ui.geometry.CornerRadius(w * 0.08f), style = s
                )
                drawCircle(tint, w * 0.07f, Offset(w * 0.32f, h * 0.38f))
                val p = Path().apply {
                    moveTo(w * 0.2f, h * 0.74f); lineTo(w * 0.42f, h * 0.5f)
                    lineTo(w * 0.58f, h * 0.66f); lineTo(w * 0.7f, h * 0.56f)
                    lineTo(w * 0.84f, h * 0.74f); close()
                }
                drawPath(p, tint, style = s)
            }
            "folder" -> {
                val p = Path().apply {
                    moveTo(w * 0.14f, h * 0.78f); lineTo(w * 0.14f, h * 0.24f)
                    lineTo(w * 0.42f, h * 0.24f); lineTo(w * 0.5f, h * 0.34f)
                    lineTo(w * 0.86f, h * 0.34f); lineTo(w * 0.86f, h * 0.78f); close()
                }
                drawPath(p, tint, style = s)
            }
            "gif" -> {
                drawRoundRect(
                    tint, Offset(w * 0.1f, h * 0.26f), Size(w * 0.8f, h * 0.48f),
                    androidx.compose.ui.geometry.CornerRadius(w * 0.08f), style = s
                )
                drawLine(tint, Offset(w * 0.3f, h * 0.42f), Offset(w * 0.3f, h * 0.6f), sw * 0.8f, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.46f, h * 0.42f), Offset(w * 0.46f, h * 0.6f), sw * 0.8f, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.62f, h * 0.42f), Offset(w * 0.62f, h * 0.6f), sw * 0.8f, StrokeCap.Round)
            }
            "keyboard" -> {
                drawRoundRect(
                    tint, Offset(w * 0.1f, h * 0.28f), Size(w * 0.8f, h * 0.44f),
                    androidx.compose.ui.geometry.CornerRadius(w * 0.08f), style = s
                )
                for (i in 0..3) drawCircle(tint, sw * 0.5f, Offset(w * (0.22f + 0.19f * i), h * 0.42f))
                drawLine(tint, Offset(w * 0.26f, h * 0.6f), Offset(w * 0.74f, h * 0.6f), sw * 0.8f, StrokeCap.Round)
            }
            "slider" -> {
                drawLine(tint, Offset(w * 0.14f, h * 0.5f), Offset(w * 0.86f, h * 0.5f), sw, StrokeCap.Round)
                drawCircle(tint, w * 0.12f, Offset(w * 0.36f, h * 0.5f))
            }
            "timer_play" -> {
                drawCircle(tint, w * 0.32f, Offset(w / 2, h * 0.56f), style = s)
                drawLine(tint, Offset(w * 0.5f, h * 0.18f), Offset(w * 0.5f, h * 0.26f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.36f, h * 0.12f), Offset(w * 0.64f, h * 0.12f), sw, StrokeCap.Round)
                val p = Path().apply {
                    moveTo(w * 0.42f, h * 0.44f); lineTo(w * 0.42f, h * 0.68f)
                    lineTo(w * 0.62f, h * 0.56f); close()
                }
                drawPath(p, tint)
            }
        }
    }
}

// ------------------------------------------------------------------ atoms

@Composable
fun CheckBoxView(checked: Boolean, color: Color, size: Dp = 20.dp, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(4.dp))
            .background(if (checked) color else Color.Transparent)
            .border(2.dp, color, RoundedCornerShape(4.dp))
            .clickable { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        if (checked) DrawIcon("check", Color.White, size * 0.7f, 2.dp)
    }
}

@Composable
fun Chip(
    text: String,
    bg: Color = C.chipBg,
    fg: Color = Color(0xFF3C3C3C),
    icon: String? = null,
    iconTint: Color = fg,
    trailing: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .height(26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(bg)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            DrawIcon(icon, iconTint, 16.dp, 1.6.dp)
            Spacer(Modifier.width(5.dp))
        }
        Text(text, color = fg, fontSize = 13.sp, maxLines = 1)
        if (trailing != null) {
            Spacer(Modifier.width(6.dp))
            trailing()
        }
    }
}

@Composable
fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(C.divider))
}

@Composable
fun EmptyState(text: String = "这里没有任务哦。", modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        DrawIcon("inbox", Color(0xFFBDBDBD), 120.dp, 2.4.dp)
        Spacer(Modifier.height(24.dp))
        Text(text, color = C.text, fontSize = 18.sp)
    }
}

/** Light weight replacement for a dashed/solid horizontal rule used by menus. */
@Composable
fun MenuDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFDADADA)))
}

@Composable
fun HLine(color: Color = C.divider, thickness: Dp = 1.dp) {
    Box(Modifier.fillMaxWidth().height(thickness).background(color))
}

@Composable
fun CircleSwatch(color: Color, size: Dp = 22.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color))
}

@Composable
fun DashedLine(color: Color) {
    Canvas(Modifier.fillMaxWidth().height(1.dp)) {
        drawLine(
            color, Offset(0f, size.height / 2), Offset(size.width, size.height / 2),
            1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
        )
    }
}

@Composable
fun RowLabel(
    icon: String,
    label: String,
    value: String,
    valueColor: Color = C.textDim,
    iconTint: Color = C.textDim,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp).clickable { onClick() }
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DrawIcon(icon, iconTint, 24.dp)
        Spacer(Modifier.width(22.dp))
        Text(label, color = valueColor, fontSize = 17.sp)
        Spacer(Modifier.weight(1f))
        if (value.isNotEmpty()) Text(value, color = valueColor, fontSize = 17.sp)
    }
}

@Composable
fun RectSwatch(color: Color, size: Dp = 22.dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(4.dp)).background(color))
}

@Composable
fun IconCircle(name: String, tint: Color, bg: Color, size: Dp = 34.dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(bg),
        contentAlignment = Alignment.Center
    ) { DrawIcon(name, tint, size * 0.6f, 1.6f.dp) }
}

/**
 * Deterministic in-window bottom panel: a scrim plus a rounded sheet anchored to
 * the bottom of the current screen. Avoids popup windows entirely.
 */
@Composable
fun BottomPanel(
    onDismiss: () -> Unit,
    maxHeight: Dp = 640.dp,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    // A sheet must never be covered by the soft keyboard: drop focus and close
    // the IME as soon as it appears (matches the original app, which dismisses
    // the keyboard when a picker sheet opens).
    LaunchedEffect(Unit) {
        focusManager.clearFocus(force = true)
        val activity = context as? Activity
        val token = activity?.currentFocus?.windowToken ?: return@LaunchedEffect
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(token, 0)
    }
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize().background(Color(0x73000000)).clickable { onDismiss() }
        )
        Column(
            Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(C.sheetBg)
                .heightIn(max = maxHeight)
                .verticalScroll(rememberScrollState())
                .padding(top = 18.dp)
        ) { content() }
    }
}

/**
 * Text field backed by a platform [EditText].
 *
 * Compose text fields do not receive `adb shell input text` keystrokes while a
 * soft IME is active, so every editable field in the reproduction uses the same
 * View-based widget the original app uses.
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = C.text,
    fontSize: Float = 17f,
    singleLine: Boolean = true,
    digitsOnly: Boolean = false,
) {
    val current by rememberUpdatedState(onValueChange)
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            EditText(ctx).apply {
                background = null
                setPadding(0, 0, 0, 0)
                setTextColor(textColor.toArgb())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize)
                inputType = if (digitsOnly) InputType.TYPE_CLASS_NUMBER
                else InputType.TYPE_CLASS_TEXT or
                    if (singleLine) 0 else InputType.TYPE_TEXT_FLAG_MULTI_LINE
                isSingleLine = singleLine
                setText(value)
                setSelection(text.length)
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
                    override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
                    override fun afterTextChanged(s: Editable?) {
                        current(s?.toString().orEmpty())
                    }
                })
            }
        },
        update = { field ->
            if (field.text.toString() != value) {
                field.setText(value)
                field.setSelection(value.length)
            }
        },
    )
}

/** Small anchored popup menu used for the gear / overflow affordances. */
@Composable
fun PopupCard(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().clickable { onDismiss() })
        Column(
            modifier.padding(top = 74.dp, end = 8.dp).align(Alignment.TopEnd)
                .clip(RoundedCornerShape(6.dp)).background(Color(0xFFF3F1F7))
                .padding(vertical = 6.dp)
        ) { content() }
    }
}
