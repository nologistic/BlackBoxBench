package com.blackboxbench.reproduction

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

/** 4x5 colour-matrix helpers used for live style thumbnails in Compose. */
object ColorMath {

    fun mul(a: FloatArray, b: FloatArray): FloatArray {
        val r = FloatArray(20)
        for (i in 0 until 4) {
            for (j in 0 until 5) {
                var s = 0f
                for (k in 0 until 4) s += a[i * 5 + k] * b[k * 5 + j]
                if (j == 4) s += a[i * 5 + 4]
                r[i * 5 + j] = s
            }
        }
        return r
    }

    val identity = floatArrayOf(
        1f, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )

    fun sat(s: Float): FloatArray {
        val inv = 1f - s
        val r = 0.213f * inv
        val g = 0.715f * inv
        val b = 0.072f * inv
        return floatArrayOf(
            r + s, g, b, 0f, 0f,
            r, g + s, b, 0f, 0f,
            r, g, b + s, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    }

    fun contrast(c: Float): FloatArray {
        val t = (1f - c) * 127.5f
        return floatArrayOf(
            c, 0f, 0f, 0f, t,
            0f, c, 0f, 0f, t,
            0f, 0f, c, 0f, t,
            0f, 0f, 0f, 1f, 0f
        )
    }

    fun bright(o: Float): FloatArray = floatArrayOf(
        1f, 0f, 0f, 0f, o,
        0f, 1f, 0f, 0f, o,
        0f, 0f, 1f, 0f, o,
        0f, 0f, 0f, 1f, 0f
    )

    fun warm(w: Float): FloatArray = floatArrayOf(
        1f + w, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f - w, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )

    fun sepia(a: Float): FloatArray {
        val id = identity
        val sp = floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        val out = FloatArray(20)
        for (i in 0 until 20) out[i] = id[i] * (1 - a) + sp[i] * a
        return out
    }

    fun of(style: StyleDef): FloatArray {
        var m = identity
        if (style.mono) m = mul(sat(0f), m)
        else if (style.sat != 1f) m = mul(sat(style.sat), m)
        if (style.contrast != 1f) m = mul(contrast(style.contrast), m)
        if (style.warm != 0f) m = mul(warm(style.warm), m)
        if (style.bright != 0f) m = mul(bright(style.bright), m)
        if (style.sepia > 0f) m = mul(sepia(style.sepia), m)
        return m
    }

    fun filter(style: StyleDef): ColorFilter = ColorFilter.colorMatrix(ColorMatrix(of(style)))
}
