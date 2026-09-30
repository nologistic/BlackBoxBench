package com.blackboxbench.reproduction

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** Non-destructive bitmap pipeline implementing the observed tool behaviour. */
object ImageOps {

    private var noiseTile: Bitmap? = null

    fun fit(src: Bitmap, maxDim: Int = 720): Bitmap {
        val m = max(src.width, src.height)
        if (m <= maxDim) return src
        val s = maxDim.toFloat() / m
        return Bitmap.createScaledBitmap(
            src,
            (src.width * s).toInt().coerceAtLeast(1),
            (src.height * s).toInt().coerceAtLeast(1),
            true
        )
    }

    fun copy(src: Bitmap): Bitmap = src.copy(Bitmap.Config.ARGB_8888, true)

    fun render(base: Bitmap, layers: List<EditLayer>): Bitmap {
        var bmp = copy(base)
        for (layer in layers) bmp = applyLayer(bmp, layer)
        return bmp
    }

    // ---------------------------------------------------------------- presets

    private data class Preset(
        val sat: Float = 1f,
        val contrast: Float = 1f,
        val bright: Float = 0f,
        val warm: Float = 0f,
        val sepia: Float = 0f,
        val mono: Boolean = false,
        val grain: Float = 0f,
        val glow: Float = 0f,
        val vignette: Float = 0f
    )

    private fun presetFor(toolId: String, variant: Int): Preset = when (toolId) {
        "hdr" -> listOf(
            Preset(1.10f, 1.15f, 2f), Preset(1.05f, 1.10f, 6f, 0.05f),
            Preset(1.20f, 1.22f, -3f), Preset(1.35f, 1.30f, -6f)
        ).getOrElse(variant) { Preset() }
        "glamour" -> listOf(
            Preset(1.05f, 0.95f, 8f, glow = 0.35f), Preset(1.10f, 0.92f, 12f, glow = 0.45f, warm = 0.05f),
            Preset(1.00f, 0.98f, 6f, glow = 0.30f), Preset(1.15f, 0.90f, 14f, glow = 0.50f, warm = 0.08f),
            Preset(0.95f, 0.95f, 10f, glow = 0.40f)
        ).getOrElse(variant) { Preset() }
        "drama" -> listOf(
            Preset(0.95f, 1.50f, -10f), Preset(0.85f, 1.60f, -15f), Preset(1.00f, 1.30f, 10f),
            Preset(1.00f, 1.35f, 15f), Preset(0.90f, 1.50f, -25f), Preset(0.85f, 1.55f, -30f)
        ).getOrElse(variant) { Preset() }
        "vintage" -> listOf(
            Preset(0.85f, 1.05f, 6f, 0.06f, 0.12f), Preset(0.80f, 1.00f, 4f, 0.10f, 0.20f),
            Preset(0.90f, 1.10f, 2f, 0.04f, 0.08f), Preset(0.75f, 1.05f, 8f, 0.12f, 0.25f, vignette = 0.5f),
            Preset(0.85f, 1.00f, 6f, 0.08f, 0.15f, vignette = 0.4f), Preset(0.95f, 1.15f, -4f, 0.05f, 0.10f)
        ).getOrElse(variant) { Preset() }
        "grainy" -> {
            val mono = variant < 4
            val base = listOf(1.05f, 1.15f, 0.95f, 1.25f, 1.00f, 1.10f, 1.20f, 0.90f, 1.30f, 1.05f)
            Preset(sat = if (mono) 0f else 0.6f, contrast = base.getOrElse(variant) { 1f }, grain = 0.6f, mono = mono)
        }
        "retrolux" -> listOf(
            Preset(0.90f, 1.10f, 4f, 0.12f, 0.20f), Preset(0.85f, 1.15f, 2f, 0.15f, 0.30f),
            Preset(0.95f, 1.05f, 6f, 0.10f, 0.15f), Preset(0.80f, 1.20f, -2f, 0.18f, 0.35f),
            Preset(0.90f, 1.00f, 8f, 0.08f, 0.12f), Preset(1.00f, 1.10f, 0f, 0.14f, 0.22f)
        ).getOrElse(variant) { Preset() }
        "grunge" -> listOf(
            Preset(0.80f, 1.35f, -6f, sepia = 0.10f, grain = 0.5f), Preset(0.70f, 1.40f, -10f, sepia = 0.20f, grain = 0.7f),
            Preset(0.90f, 1.25f, 0f, sepia = 0.05f, grain = 0.4f), Preset(0.60f, 1.45f, -8f, sepia = 0.25f, grain = 0.8f),
            Preset(0.85f, 1.30f, 2f, sepia = 0.12f, grain = 0.5f), Preset(1.00f, 1.20f, -4f, sepia = 0.08f, grain = 0.6f)
        ).getOrElse(variant) { Preset() }
        "bw" -> listOf(
            Preset(0f, 1.00f, 0f, mono = true), Preset(0f, 1.30f, -2f, mono = true),
            Preset(0f, 1.05f, 15f, mono = true), Preset(0f, 1.05f, -15f, mono = true),
            Preset(0f, 1.15f, 0f, mono = true, grain = 0.5f), Preset(0f, 1.25f, -8f, mono = true)
        ).getOrElse(variant) { Preset(0f, 1f, 0f, mono = true) }
        "bwfilm" -> listOf(
            Preset(0f, 1.30f, -4f, mono = true, grain = 0.7f), Preset(0f, 1.45f, -8f, mono = true, grain = 0.8f),
            Preset(0f, 1.15f, 6f, mono = true, grain = 0.5f), Preset(0f, 1.20f, -12f, mono = true, grain = 0.6f),
            Preset(0f, 1.35f, -6f, mono = true, grain = 0.9f), Preset(0f, 1.25f, 4f, mono = true, grain = 0.55f),
            Preset(0f, 1.50f, -10f, mono = true, grain = 1.0f)
        ).getOrElse(variant) { Preset(0f, 1.2f, 0f, mono = true, grain = 0.6f) }
        else -> Preset()
    }

    // ------------------------------------------------------------- color math

    private fun contrastMatrix(f: Float): ColorMatrix {
        val t = (1f - f) * 127.5f
        return ColorMatrix(
            floatArrayOf(
                f, 0f, 0f, 0f, t,
                0f, f, 0f, 0f, t,
                0f, 0f, f, 0f, t,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    private fun offsetMatrix(o: Float): ColorMatrix = ColorMatrix(
        floatArrayOf(
            1f, 0f, 0f, 0f, o,
            0f, 1f, 0f, 0f, o,
            0f, 0f, 1f, 0f, o,
            0f, 0f, 0f, 1f, 0f
        )
    )

    private fun channelScale(r: Float, g: Float, b: Float): ColorMatrix = ColorMatrix(
        floatArrayOf(
            r, 0f, 0f, 0f, 0f,
            0f, g, 0f, 0f, 0f,
            0f, 0f, b, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )

    private fun sepiaMatrix(amount: Float): ColorMatrix {
        val a = amount.coerceIn(0f, 1f)
        val id = floatArrayOf(1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)
        val sp = floatArrayOf(0.393f, 0.769f, 0.189f, 0f, 0f, 0.349f, 0.686f, 0.168f, 0f, 0f, 0.272f, 0.534f, 0.131f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)
        val out = FloatArray(20)
        for (i in 0 until 20) out[i] = id[i] * (1 - a) + sp[i] * a
        return ColorMatrix(out)
    }

    private fun applyMatrix(src: Bitmap, cm: ColorMatrix): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return out
    }

    // --------------------------------------------------------------- pipeline

    fun applyLayer(src: Bitmap, layer: EditLayer): Bitmap {
        if (layer.toolId == "style") return colorLayer(src, layer)
        val tool = ToolCatalog.byId(layer.toolId)
        return when (tool.special) {
            "crop" -> cropLayer(src, layer)
            "rotate" -> rotateLayer(src, layer)
            "perspective" -> perspectiveLayer(src, layer)
            "expand" -> expandLayer(src, layer)
            "vignette" -> vignetteLayer(src, layer)
            "lensblur" -> lensBlurLayer(src, layer)
            "text" -> textLayer(src, layer)
            "frames" -> frameLayer(src, layer)
            "double" -> doubleLayer(src, layer)
            else -> colorLayer(src, layer)
        }
    }

    private fun colorLayer(src: Bitmap, layer: EditLayer): Bitmap {
        val id = layer.toolId
        val preset = presetFor(id, layer.variant)
        var sat = preset.sat
        var contrast = preset.contrast
        var offset = preset.bright
        var warm = preset.warm
        var sepia = preset.sepia
        var grain = preset.grain
        var glow = preset.glow
        var vignette = preset.vignette

        fun v(n: String) = layer.value(n)

        when (id) {
            "tune" -> {
                sat *= 1f + (v("饱和度") + v("氛围") * 0.5f) / 100f
                contrast *= 1f + v("对比度") / 100f + abs(v("氛围")) / 400f
                offset += v("亮度") * 1.2f + v("高光") * 0.5f + v("阴影") * 0.5f + v("氛围") * 0.2f
                warm += v("暖色调") / 250f
            }
            "details" -> {
                contrast *= 1f + v("结构") / 200f + v("锐化") / 200f
                sat *= 1f + v("结构") / 300f
                offset += v("结构") * 0.15f
            }
            "whitebalance" -> {
                warm += v("色温") / 250f
                sat *= 1f - v("着色") / 500f
            }
            "curves" -> {
                contrast *= 1.28f
                offset += 4f
            }
            "tonal" -> {
                contrast *= 1f + (v("高色调") + v("低色调")) / 300f
                offset += v("高色调") * 0.4f + v("低色调") * 0.2f + v("中色调") * 0.3f
                sat *= 1f + v("中色调") / 400f
            }
            "hdr" -> {
                val k = (v("滤镜强度") / 100f).coerceIn(0f, 1f)
                sat = 1f + (sat - 1f) * k * 1.6f
                contrast = 1f + (contrast - 1f) * k * 1.6f
                offset += v("亮度") * 1.2f + (preset.bright * k)
                sat *= 1f + v("饱和度") / 150f
            }
            "glamour" -> {
                val k = (v("光晕") / 100f).coerceIn(0f, 1f)
                glow = max(glow, k * 0.9f)
                offset += v("光晕") * 0.25f
                sat *= 1f + v("饱和度") / 150f
                warm += v("暖色调") / 250f
            }
            "drama" -> {
                val k = (v("滤镜强度") / 100f).coerceIn(0f, 1f)
                sat = 1f + (sat - 1f) * k
                contrast = 1f + (contrast - 1f) * k
                offset += v("饱和度") * 0.3f
                sat *= 1f + v("饱和度") / 150f
            }
            "vintage" -> {
                val k = (v("样式强度") / 100f).coerceIn(0f, 1f) + 0.4f
                offset += v("亮度") * 1.2f
                sat *= 1f + v("饱和度") / 120f
                contrast *= 1f + v("对比度") / 150f
                vignette = max(vignette, v("晕影强度") / 100f)
                grain = max(grain, v("颗粒") / 100f)
                sepia *= (0.6f + k * 0.4f)
            }
            "grainy" -> {
                grain = max(grain, v("粒度") / 100f)
                val k = (v("风格强度") / 100f).coerceIn(0f, 1f) * 1.5f
                contrast = 1f + (contrast - 1f) * k
                if (preset.mono) sat = 0f
            }
            "retrolux" -> {
                val k = (v("样式强度") / 100f).coerceIn(0f, 1f) + 0.4f
                offset += v("亮度") * 1.2f
                sat *= 1f + v("饱和度") / 120f
                contrast *= 1f + v("对比度") / 150f
                sepia *= (0.5f + k * 0.5f)
            }
            "grunge" -> {
                contrast *= 1f + v("纹理") / 250f
                offset += v("亮度") * 1.2f
                sat *= 1f + v("饱和度") / 120f
                grain = max(grain, v("纹理") / 150f)
            }
            "bw", "bwfilm" -> {
                sat = 0f
                contrast *= 1f + v("对比度") / 100f
                offset += v("亮度") * 1.2f
                grain = max(grain, v("颗粒") / 100f)
            }
            "face" -> {
                offset += v("面部提亮") * 0.4f
                sat *= 1f + v("肤色") / 200f
                contrast *= 1f + v("眼部清晰") / 400f
            }
            "brush" -> {
                val type = layer.variant
                val value = layer.amount
                when (type) {
                    0 -> offset += value * 1.5f
                    1 -> offset += value * 1.5f
                    2 -> warm += value / 250f
                    3 -> sat *= 1f + value / 100f
                }
            }
            "selective" -> {
                offset += v("亮度") * 1.2f
                contrast *= 1f + v("对比度") / 100f
                sat *= 1f + v("饱和度") / 100f
            }
            "healing" -> { /* no visual change */ }
            "style" -> {
                sat *= layer.params["sat"] ?: 1f
                contrast *= layer.params["contrast"] ?: 1f
                offset += layer.params["bright"] ?: 0f
                warm += layer.params["warm"] ?: 0f
                sepia = layer.params["sepia"] ?: 0f
                glow = layer.params["glow"] ?: 0f
                vignette = layer.params["vignette"] ?: 0f
            }
        }

        var cm = ColorMatrix()
        if (abs(sat - 1f) > 0.001f || preset.mono) {
            cm.postConcat(ColorMatrix().apply { setSaturation(sat) })
        }
        if (abs(contrast - 1f) > 0.001f) cm.postConcat(contrastMatrix(contrast))
        if (abs(offset) > 0.01f) cm.postConcat(offsetMatrix(offset))
        if (abs(warm) > 0.001f) cm.postConcat(channelScale(1f + warm, 1f, 1f - warm))
        if (sepia > 0.001f) cm.postConcat(sepiaMatrix(sepia))

        var out = applyMatrix(src, cm)
        if (glow > 0.01f) out = blend(out, blur(out, 24), glow.coerceIn(0f, 0.9f))
        if (grain > 0.01f) out = addGrain(out, grain)
        if (vignette > 0.01f) out = vignetteLayer(out, EditLayer("vignette", "晕影", mapOf("外部亮度" to -100f * vignette)))
        return out
    }

    // ---------------------------------------------------------------- geometry

    private fun cropLayer(src: Bitmap, layer: EditLayer): Bitmap {
        val c = layer.crop ?: return src
        val l = (c[0] * src.width).toInt().coerceIn(0, src.width - 1)
        val t = (c[1] * src.height).toInt().coerceIn(0, src.height - 1)
        val r = (c[2] * src.width).toInt().coerceIn(l + 1, src.width)
        val b = (c[3] * src.height).toInt().coerceIn(t + 1, src.height)
        if (l == 0 && t == 0 && r == src.width && b == src.height) return src
        return Bitmap.createBitmap(src, l, t, r - l, b - t)
    }

    private fun rotateLayer(src: Bitmap, layer: EditLayer): Bitmap {
        var bmp = src
        if (layer.rot90 % 360 != 0) {
            val m = Matrix()
            m.postRotate(layer.rot90.toFloat())
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        }
        val angle = layer.value("校正角度")
        if (abs(angle) > 0.01f) {
            val m = Matrix()
            m.postRotate(angle)
            val w = bmp.width
            val h = bmp.height
            val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)
            canvas.drawColor(Color.BLACK)
            val scale = 1f / (abs(sin(Math.toRadians(angle.toDouble()))).toFloat() + 1f)
            m.postScale(scale, scale)
            m.postTranslate(w / 2f, h / 2f)
            m.preTranslate(-w / 2f, -h / 2f)
            canvas.drawBitmap(bmp, m, Paint(Paint.FILTER_BITMAP_FLAG))
            bmp = out
        }
        if (layer.flipH) {
            val m = Matrix()
            m.postScale(-1f, 1f)
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        }
        return bmp
    }

    private fun perspectiveLayer(src: Bitmap, layer: EditLayer): Bitmap {
        val tilt = layer.value("倾斜")
        val rot = layer.value("旋转")
        val zoom = layer.value("缩放")
        if (abs(tilt) < 0.01f && abs(rot) < 0.01f && abs(zoom) < 0.01f) return src
        val w = src.width
        val h = src.height
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val m = Matrix()
        val s = 1f + zoom / 200f
        m.postScale(s, s)
        m.postSkew(-tilt / 400f, 0f)
        m.postRotate(rot / 6f)
        m.postTranslate(w / 2f, h / 2f)
        m.preTranslate(-w / 2f, -h / 2f)
        canvas.drawBitmap(src, m, Paint(Paint.FILTER_BITMAP_FLAG))
        return out
    }

    private fun expandLayer(src: Bitmap, layer: EditLayer): Bitmap {
        val pct = layer.value("扩展").coerceIn(0f, 40f) / 100f
        if (pct <= 0.001f) return src
        val padX = (src.width * pct).toInt()
        val padY = (src.height * pct).toInt()
        val out = Bitmap.createBitmap(src.width + padX * 2, src.height + padY * 2, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val fill = when (layer.variant) {
            1 -> Color.WHITE
            2 -> Color.BLACK
            else -> averageColor(src)
        }
        canvas.drawColor(fill)
        canvas.drawBitmap(src, padX.toFloat(), padY.toFloat(), Paint(Paint.FILTER_BITMAP_FLAG))
        return out
    }

    private fun averageColor(src: Bitmap): Int {
        var r = 0L; var g = 0L; var b = 0L; var n = 0L
        val stepX = max(1, src.width / 32)
        val stepY = max(1, src.height / 32)
        var y = 0
        while (y < src.height) {
            var x = 0
            while (x < src.width) {
                val c = src.getPixel(x, y)
                r += Color.red(c); g += Color.green(c); b += Color.blue(c); n++
                x += stepX
            }
            y += stepY
        }
        if (n == 0L) return Color.GRAY
        return Color.rgb((r / n).toInt(), (g / n).toInt(), (b / n).toInt())
    }

    // ------------------------------------------------------------------ extra

    fun vignetteLayer(src: Bitmap, layer: EditLayer): Bitmap {
        val outer = layer.value("外部亮度")
        val inner = layer.value("内部亮度")
        var bmp = src
        if (abs(inner) > 0.5f) {
            val cm = ColorMatrix()
            cm.postConcat(offsetMatrix(inner * 1.2f))
            bmp = applyMatrix(bmp, cm)
        }
        if (abs(outer) < 0.5f) return bmp
        val out = copy(bmp)
        val canvas = Canvas(out)
        val cx = out.width / 2f
        val cy = out.height / 2f
        val maxR = hypot(cx, cy)
        val target = if (outer < 0) Color.BLACK else Color.WHITE
        val alpha = (abs(outer) / 100f * 235).toInt().coerceIn(0, 255)
        val shader = RadialGradient(
            cx, cy, maxR,
            intArrayOf(
                Color.argb(0, Color.red(target), Color.green(target), Color.blue(target)),
                Color.argb(0, Color.red(target), Color.green(target), Color.blue(target)),
                Color.argb(alpha, Color.red(target), Color.green(target), Color.blue(target))
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        paint.shader = shader
        canvas.drawRect(0f, 0f, out.width.toFloat(), out.height.toFloat(), paint)
        return out
    }

    private fun lensBlurLayer(src: Bitmap, layer: EditLayer): Bitmap {
        val strength = layer.value("模糊强度").coerceIn(0f, 100f)
        if (strength < 0.5f) return src
        val radius = (strength / 100f * 45f).toInt().coerceAtLeast(1)
        val blurred = blur(src, radius)
        val transition = (layer.value("过渡").coerceIn(0f, 100f) / 100f * 0.5f + 0.05f)
        val w = src.width
        val h = src.height
        val sharpPx = IntArray(w * h)
        val blurPx = IntArray(w * h)
        src.getPixels(sharpPx, 0, w, 0, 0, w, h)
        blurred.getPixels(blurPx, 0, w, 0, 0, w, h)
        val cx = w / 2f
        val cy = h / 2f
        val baseR = min(w, h) * 0.30f
        val t0 = baseR
        val t1 = baseR * (1f + transition)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val i = y * w + x
                val d = hypot(x - cx, y - cy)
                val t = ((d - t0) / (t1 - t0)).coerceIn(0f, 1f)
                if (t <= 0f) continue
                val a = sharpPx[i]
                val b = blurPx[i]
                val ia = (1 - t)
                val r = (Color.red(a) * ia + Color.red(b) * t).toInt()
                val g = (Color.green(a) * ia + Color.green(b) * t).toInt()
                val bl = (Color.blue(a) * ia + Color.blue(b) * t).toInt()
                sharpPx[i] = Color.argb(Color.alpha(a), r, g, bl)
            }
        }
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(sharpPx, 0, w, 0, 0, w, h)
        val vig = layer.value("晕影强度")
        return if (vig > 0.5f) vignetteLayer(out, EditLayer("vignette", "晕影", mapOf("外部亮度" to -vig))) else out
    }

    private fun textLayer(src: Bitmap, layer: EditLayer): Bitmap {
        val out = copy(src)
        val canvas = Canvas(out)
        val text = layer.textValue.ifBlank { "点按两次即可更改文本" }
        val size = out.width * 0.11f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.WHITE
        paint.textSize = size
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        paint.setShadowLayer(size * 0.12f, 0f, size * 0.06f, Color.argb(160, 0, 0, 0))
        val cx = out.width / 2f
        val cy = out.height * 0.9f
        canvas.drawText(text, cx, cy, paint)
        return out
    }

    private fun frameLayer(src: Bitmap, layer: EditLayer): Bitmap {
        val w = abs(layer.value("相框宽度")).coerceIn(0f, 100f) / 100f
        if (w <= 0.001f) return src
        return addFrame(src, layer.variant, w)
    }

    fun addFrame(src: Bitmap, variant: Int, amount: Float): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val border = (min(src.width, src.height) * amount * 0.28f)
        val inner = RectF(border, border, src.width - border, src.height - border)
        val frameColor = when (variant % 3) {
            0 -> Color.WHITE
            1 -> Color.BLACK
            else -> Color.rgb(240, 235, 225)
        }
        canvas.drawColor(frameColor)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        if (variant >= 3) paint.setShadowLayer(border * 0.35f, 0f, border * 0.15f, Color.argb(140, 0, 0, 0))
        canvas.drawBitmap(src, null, inner, paint)
        return out
    }

    private fun doubleLayer(src: Bitmap, layer: EditLayer): Bitmap {
        val out = copy(src)
        val canvas = Canvas(out)
        val overlay = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val oc = Canvas(overlay)
        val shader = LinearGradient(
            0f, 0f, src.width.toFloat(), src.height.toFloat(),
            Color.argb(220, 255, 210, 120), Color.argb(200, 40, 90, 200),
            Shader.TileMode.CLAMP
        )
        val op = Paint()
        op.shader = shader
        oc.drawRect(0f, 0f, src.width.toFloat(), src.height.toFloat(), op)
        val p = Paint(Paint.FILTER_BITMAP_FLAG)
        p.alpha = (layer.amount.coerceIn(0f, 1f) * 255).toInt()
        p.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        canvas.drawBitmap(overlay, 0f, 0f, p)
        return out
    }

    // ------------------------------------------------------------------ utils

    fun blur(src: Bitmap, radius: Int): Bitmap {
        if (radius < 1) return src
        val scale = max(2, radius / 2)
        val sw = max(1, src.width / scale)
        val sh = max(1, src.height / scale)
        val small = Bitmap.createScaledBitmap(src, sw, sh, true)
        return Bitmap.createScaledBitmap(small, src.width, src.height, true)
    }

    fun blend(a: Bitmap, b: Bitmap, t: Float): Bitmap {
        val out = copy(a)
        val canvas = Canvas(out)
        val p = Paint(Paint.FILTER_BITMAP_FLAG)
        p.alpha = (t.coerceIn(0f, 1f) * 255).toInt()
        canvas.drawBitmap(b, 0f, 0f, p)
        return out
    }

    private fun noiseBitmap(): Bitmap {
        noiseTile?.let { return it }
        val n = 160
        val bmp = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888)
        val rnd = Random(7)
        val px = IntArray(n * n)
        for (i in px.indices) {
            val v = rnd.nextInt(256)
            px[i] = Color.argb(60, v, v, v)
        }
        bmp.setPixels(px, 0, n, 0, 0, n, n)
        noiseTile = bmp
        return bmp
    }

    fun addGrain(src: Bitmap, amount: Float): Bitmap {
        val out = copy(src)
        val canvas = Canvas(out)
        val p = Paint()
        p.shader = BitmapShader(noiseBitmap(), Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        p.alpha = (amount.coerceIn(0f, 1f) * 90).toInt()
        canvas.drawRect(0f, 0f, out.width.toFloat(), out.height.toFloat(), p)
        return out
    }
}
