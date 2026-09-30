package com.blackboxbench.reproduction

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class AppScreen { EDITOR, VIEW_EDITS, INFO, SETTINGS }

/** A mutable editing session for one tool before it is committed to the stack. */
data class ToolSession(
    val tool: ToolDef,
    val params: Map<String, Float> = emptyMap(),
    val variant: Int = 0,
    val amount: Float = 0f,
    val activeParam: Int = 0,
    val showParamList: Boolean = false,
    val crop: FloatArray = floatArrayOf(0f, 0f, 1f, 1f),
    val rot90: Int = 0,
    val flipH: Boolean = false,
    val text: String = "点按两次即可更改文本",
    val pointX: Float = 0.5f,
    val pointY: Float = 0.5f,
    val pointR: Float = 0.35f,
    val dialog: String? = null
)

class AppState(private val context: Context) {

    val photoAssets: List<String> = (1..8).map { "photos/photo_%02d.png".format(it) }

    // ---- document ----
    var screen by mutableStateOf(AppScreen.EDITOR)
    var photoOpen by mutableStateOf(false)
    var photoAsset by mutableStateOf("")
    var original by mutableStateOf<Bitmap?>(null)
    var photoWidth by mutableStateOf(0)
    var photoHeight by mutableStateOf(0)
    var sourceName by mutableStateOf("")
    var sourceWidth by mutableStateOf(0)
    var sourceHeight by mutableStateOf(0)
    var sourceBytes by mutableStateOf(0L)

    // ---- stack ----
    var layers by mutableStateOf<List<EditLayer>>(emptyList())
    var redoStack by mutableStateOf<List<EditLayer>>(emptyList())

    // ---- ui ----
    var panelTab by mutableStateOf<Int?>(null)
    var pickerOpen by mutableStateOf(false)
    var pickerAlbum by mutableStateOf(false)
    var stylePreview by mutableStateOf<StyleDef?>(null)
    var stylePreviewIndex by mutableStateOf<Int?>(null)
    var tool by mutableStateOf<ToolSession?>(null)
    var snackbar by mutableStateOf<String?>(null)
    var lastStyle by mutableStateOf<StyleDef?>(null)
    var customStyles by mutableStateOf<List<Pair<String, StyleDef>>>(emptyList())
    var styleNameDialog by mutableStateOf(false)
    var styleName by mutableStateOf("")
    var shareOpen by mutableStateOf(false)
    var exportAsOpen by mutableStateOf(false)
    var exportWidth by mutableStateOf(0)
    var exportFormat by mutableStateOf(1)
    var showHistogram by mutableStateOf(false)
    var stackMenuOpen by mutableStateOf(false)
    var qrMenuOpen by mutableStateOf(false)
    var overflowOpen by mutableStateOf(false)
    var exportDialog by mutableStateOf<Int?>(null)
    var selectedLayer by mutableStateOf<Int?>(null)
    var confirmKind by mutableStateOf<String?>(null)

    private val thumbs = HashMap<String, Bitmap?>()

    fun thumbnail(asset: String): Bitmap? {
        if (thumbs.containsKey(asset)) return thumbs[asset]
        val bmp = try {
            context.assets.open(asset).use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            null
        }
        thumbs[asset] = bmp
        return bmp
    }

    fun openPhoto(asset: String) {
        val bytes = try {
            context.assets.open(asset).use { it.readBytes() }
        } catch (e: Exception) {
            ByteArray(0)
        }
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val fitted = ImageOps.fit(bmp)
        original = fitted
        photoAsset = asset
        photoWidth = fitted.width
        photoHeight = fitted.height
        sourceName = asset.substringAfterLast('/')
        sourceWidth = bmp.width
        sourceHeight = bmp.height
        sourceBytes = bytes.size.toLong()
        layers = emptyList()
        redoStack = emptyList()
        stylePreview = null
        stylePreviewIndex = null
        tool = null
        panelTab = 0
        photoOpen = true
        screen = AppScreen.EDITOR
    }

    fun commitLayer(layer: EditLayer) {
        layers = layers + layer
        redoStack = emptyList()
        panelTab = null
    }

    fun styleLayer(style: StyleDef): EditLayer = EditLayer(
        toolId = "style",
        name = style.name,
        params = mapOf(
            "sat" to style.sat,
            "contrast" to style.contrast,
            "bright" to style.bright,
            "warm" to style.warm,
            "sepia" to style.sepia,
            "glow" to style.glow,
            "vignette" to style.vignette
        )
    )

    /** Effective list of layers including a live style preview. */
    fun effectiveLayers(): List<EditLayer> {
        val base = layers
        val s = stylePreview ?: return base
        return base + styleLayer(s)
    }

    fun undo(): Boolean {
        if (layers.isEmpty()) return false
        redoStack = redoStack + layers.last()
        layers = layers.dropLast(1)
        return true
    }

    fun redo(): Boolean {
        if (redoStack.isEmpty()) return false
        layers = layers + redoStack.last()
        redoStack = redoStack.dropLast(1)
        return true
    }

    fun revert() {
        layers = emptyList()
        redoStack = emptyList()
        stylePreview = null
        stylePreviewIndex = null
    }
}
