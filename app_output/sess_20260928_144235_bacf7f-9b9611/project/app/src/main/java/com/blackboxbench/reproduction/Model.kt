package com.blackboxbench.reproduction

/** A single adjustable parameter of a tool. */
data class ParamSpec(
    val name: String,
    val min: Float,
    val max: Float,
    val default: Float,
    val decimals: Int = 0,
    val suffix: String = ""
)

/** A tool definition (name, parameters, filter variants, special interaction). */
data class ToolDef(
    val id: String,
    val name: String,
    val params: List<ParamSpec> = emptyList(),
    val variants: List<String> = emptyList(),
    val hasAuto: Boolean = false,
    val special: String? = null
)

/** A committed (or previewed) edit layer on the non-destructive stack. */
data class EditLayer(
    val toolId: String,
    val name: String,
    val params: Map<String, Float> = emptyMap(),
    val variant: Int = 0,
    val variantName: String = "",
    val crop: FloatArray? = null,
    val rot90: Int = 0,
    val flipH: Boolean = false,
    val textValue: String = "",
    val pointX: Float = 0.5f,
    val pointY: Float = 0.5f,
    val pointR: Float = 0.35f,
    val amount: Float = 0f
) {
    fun value(name: String): Float = params[name] ?: 0f
}

/** A style / "Look" preset. */
data class StyleDef(
    val name: String,
    val sat: Float = 1f,
    val contrast: Float = 1f,
    val bright: Float = 0f,
    val warm: Float = 0f,
    val sepia: Float = 0f,
    val glow: Float = 0f,
    val vignette: Float = 0f,
    val mono: Boolean = false
)

object ToolCatalog {

    val tools: List<ToolDef> = listOf(
        ToolDef(
            "tune", "调整图片",
            listOf(
                ParamSpec("亮度", -100f, 100f, 0f),
                ParamSpec("对比度", -100f, 100f, 0f),
                ParamSpec("饱和度", -100f, 100f, 0f),
                ParamSpec("氛围", -100f, 100f, 0f),
                ParamSpec("高光", -100f, 100f, 0f),
                ParamSpec("阴影", -100f, 100f, 0f),
                ParamSpec("暖色调", -100f, 100f, 0f)
            ),
            hasAuto = true
        ),
        ToolDef(
            "details", "突出细节",
            listOf(ParamSpec("结构", -100f, 100f, 0f), ParamSpec("锐化", -100f, 100f, 0f))
        ),
        ToolDef("curves", "曲线", special = "curves"),
        ToolDef(
            "whitebalance", "白平衡",
            listOf(ParamSpec("色温", -100f, 100f, 0f), ParamSpec("着色", -100f, 100f, 0f)),
            hasAuto = true
        ),
        ToolDef(
            "crop", "剪裁", special = "crop",
            variants = listOf("自由", "原图", "正方形", "DIN", "3:2", "4:3", "16:9")
        ),
        ToolDef(
            "rotate", "旋转",
            listOf(ParamSpec("校正角度", -45f, 45f, 0f, decimals = 2, suffix = "°")),
            special = "rotate"
        ),
        ToolDef(
            "perspective", "视角",
            listOf(
                ParamSpec("倾斜", -100f, 100f, 0f),
                ParamSpec("旋转", -100f, 100f, 0f),
                ParamSpec("缩放", -100f, 100f, 0f)
            ),
            variants = listOf("倾斜", "旋转", "缩放", "自由"), hasAuto = true, special = "perspective"
        ),
        ToolDef(
            "expand", "展开",
            listOf(ParamSpec("扩展", 0f, 40f, 12f)),
            variants = listOf("智能填色", "白色", "黑色"), special = "expand"
        ),
        ToolDef(
            "selective", "局部",
            listOf(
                ParamSpec("亮度", -100f, 100f, 0f),
                ParamSpec("对比度", -100f, 100f, 0f),
                ParamSpec("饱和度", -100f, 100f, 0f),
                ParamSpec("结构", -100f, 100f, 0f)
            ),
            special = "selective"
        ),
        ToolDef("brush", "画笔", special = "brush", variants = listOf("加光减光", "曝光", "色温", "饱和度")),
        ToolDef("healing", "修复", special = "healing"),
        ToolDef(
            "hdr", "HDR景观",
            listOf(
                ParamSpec("滤镜强度", 0f, 100f, 50f),
                ParamSpec("亮度", -100f, 100f, 0f),
                ParamSpec("饱和度", -100f, 100f, 0f)
            ),
            variants = listOf("自然", "人物", "精细", "强"), hasAuto = true
        ),
        ToolDef(
            "glamour", "魅力光晕",
            listOf(
                ParamSpec("光晕", 0f, 100f, 27f),
                ParamSpec("饱和度", -100f, 100f, 0f),
                ParamSpec("暖色调", -100f, 100f, 0f)
            ),
            variants = listOf("1", "2", "3", "4", "5"), hasAuto = true
        ),
        ToolDef(
            "tonal", "色调对比度",
            listOf(
                ParamSpec("高色调", -100f, 100f, 30f),
                ParamSpec("中色调", -100f, 100f, 0f),
                ParamSpec("低色调", -100f, 100f, 0f),
                ParamSpec("保护", 0f, 100f, 0f)
            )
        ),
        ToolDef(
            "drama", "戏剧效果",
            listOf(ParamSpec("滤镜强度", 0f, 100f, 90f), ParamSpec("饱和度", -100f, 100f, 0f)),
            variants = listOf("戏剧1", "戏剧2", "明亮1", "明亮2", "昏暗1", "昏暗2"), hasAuto = true
        ),
        ToolDef(
            "vintage", "复古",
            listOf(
                ParamSpec("亮度", -100f, 100f, 0f),
                ParamSpec("饱和度", -100f, 100f, 0f),
                ParamSpec("对比度", -100f, 100f, 0f),
                ParamSpec("晕影强度", 0f, 100f, 0f),
                ParamSpec("颗粒", 0f, 100f, 0f)
            ),
            variants = listOf("1", "2", "3", "4", "5", "6"), hasAuto = true, special = "vintage"
        ),
        ToolDef(
            "grainy", "粗粒胶片",
            listOf(ParamSpec("粒度", 0f, 100f, 25f), ParamSpec("风格强度", 0f, 100f, 50f)),
            variants = listOf("A01", "A02", "A03", "A04", "B01", "B02", "B03", "C01", "C02", "C03"),
            hasAuto = true, special = "grainy"
        ),
        ToolDef(
            "retrolux", "怀旧",
            listOf(
                ParamSpec("亮度", -100f, 100f, 0f),
                ParamSpec("饱和度", -100f, 100f, 0f),
                ParamSpec("对比度", -100f, 100f, 0f),
                ParamSpec("样式强度", 0f, 100f, 50f)
            ),
            variants = listOf("1", "2", "3", "4", "5", "6"), hasAuto = true, special = "retrolux"
        ),
        ToolDef(
            "grunge", "斑驳",
            listOf(
                ParamSpec("样式", 0f, 2000f, 1304f),
                ParamSpec("纹理", 0f, 100f, 50f),
                ParamSpec("亮度", -100f, 100f, 0f),
                ParamSpec("饱和度", -100f, 100f, 0f)
            ),
            variants = listOf("1", "2", "3", "4", "5", "6"), special = "grunge"
        ),
        ToolDef(
            "bw", "黑白",
            listOf(
                ParamSpec("亮度", -100f, 100f, 0f),
                ParamSpec("对比度", -100f, 100f, 0f),
                ParamSpec("颗粒", 0f, 100f, 0f)
            ),
            variants = listOf("中性", "对比", "明亮", "昏暗", "胶片", "暗化天空"), hasAuto = true,
            special = "bw"
        ),
        ToolDef(
            "bwfilm", "黑白电影",
            listOf(
                ParamSpec("亮度", -100f, 100f, 0f),
                ParamSpec("对比度", -100f, 100f, 0f),
                ParamSpec("颗粒", 0f, 100f, 0f)
            ),
            variants = listOf("S01", "S02", "S03", "S04", "C01", "C02", "C03"), hasAuto = true,
            special = "bw"
        ),
        ToolDef(
            "face", "美颜",
            listOf(
                ParamSpec("面部提亮", 0f, 100f, 30f),
                ParamSpec("肤色", -100f, 100f, 0f),
                ParamSpec("眼部清晰", 0f, 100f, 0f)
            ),
            special = "face", hasAuto = true
        ),
        ToolDef("headpose", "头部姿势", special = "headpose"),
        ToolDef(
            "lensblur", "镜头模糊",
            listOf(
                ParamSpec("模糊强度", 0f, 100f, 30f),
                ParamSpec("过渡", 0f, 100f, 50f),
                ParamSpec("晕影强度", 0f, 100f, 0f)
            ),
            special = "lensblur"
        ),
        ToolDef(
            "vignette", "晕影",
            listOf(ParamSpec("外部亮度", -100f, 100f, -50f), ParamSpec("内部亮度", -100f, 100f, 0f)),
            special = "vignette"
        ),
        ToolDef("double", "双重曝光", special = "double"),
        ToolDef("text", "文字", special = "text", variants = listOf("N4", "N5", "N6", "N7", "N8", "M1", "M2", "M3")),
        ToolDef(
            "frames", "相框",
            listOf(ParamSpec("相框宽度", -100f, 100f, -10f)),
            variants = listOf("1", "2", "3", "4", "5", "6"), special = "frames"
        )
    )

    fun byId(id: String): ToolDef = tools.first { it.id == id }
}

object StyleCatalog {
    val styles: List<StyleDef> = listOf(
        StyleDef("上次修改"),
        StyleDef("Portrait", sat = 1.05f, bright = 4f, warm = 0.06f, glow = 0.10f),
        StyleDef("Smooth", sat = 0.95f, contrast = 0.92f, bright = 6f, glow = 0.12f),
        StyleDef("Pop", sat = 1.45f, contrast = 1.18f, bright = 2f),
        StyleDef("Accentuate", sat = 1.25f, contrast = 1.25f),
        StyleDef("Faded Glow", sat = 0.90f, contrast = 0.85f, bright = 10f, glow = 0.25f),
        StyleDef("Morning", sat = 1.15f, bright = 12f, warm = 0.08f),
        StyleDef("Bright", sat = 1.05f, bright = 20f),
        StyleDef("Fine Art", sat = 0.75f, contrast = 1.20f),
        StyleDef("Push", sat = 1.30f, contrast = 1.35f),
        StyleDef("Structure", sat = 1.10f, contrast = 1.30f, bright = -4f),
        StyleDef("Silhouette", sat = 0.80f, bright = -25f, contrast = 1.40f)
    )
}
