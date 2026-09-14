package com.blackboxbench.reproduction

import android.app.Activity
import android.os.Bundle
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class MainActivity : Activity() {
    private lateinit var appView: PaintAppView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(249, 249, 255)
        window.navigationBarColor = Color.rgb(249, 249, 255)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        appView = PaintAppView(this)
        setContentView(appView)
    }

    @Deprecated("Android back callback")
    override fun onBackPressed() {
        if (!appView.handleBack()) super.onBackPressed()
    }
}

private enum class ToolMode { BRUSH, FILL, ERASER, PICKER }
private enum class Page { CANVAS, SETTINGS, APPEARANCE, ABOUT, LICENSES }
private enum class Popup { NONE, OVERFLOW, COLOR, BACKGROUND_COLOR, CLEAR, SAVE, SHARE, FILE_PICKER, THEME, FONT, GUARD }

private data class CanvasSnapshot(val bitmap: Bitmap, val background: Int)

private class PaintAppView(private val host: Activity) : View(host) {
    private val d = resources.displayMetrics.density
    private val prefs = host.getSharedPreferences("paint_preferences", Context.MODE_PRIVATE)
    private val ink = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val toolbarHeight = 58f * d
    private val toolHeight = 58f * d
    private val bottomHeight = 145f * d
    private var canvasTop = toolbarHeight + toolHeight
    private var canvasBottom = 0f

    private var page = Page.CANVAS
    private var popup = Popup.NONE
    private var tool = ToolMode.BRUSH
    private var brushColor = prefs.getInt("brushColor", Color.rgb(16, 109, 31))
    private var backgroundColor = prefs.getInt("backgroundColor", Color.WHITE)
    private var brushWidthDp = prefs.getFloat("brushWidth", 13f)
    private var showBrushTools = prefs.getBoolean("showBrushTools", true)
    private var keepAwake = prefs.getBoolean("keepAwake", true)
    private var allowGestures = prefs.getBoolean("allowGestures", true)
    private var zoomBrush = prefs.getBoolean("zoomBrush", true)
    private var forcePortrait = prefs.getBoolean("forcePortrait", false)

    private lateinit var foreground: Bitmap
    private lateinit var foregroundCanvas: Canvas
    private var importedImage: Bitmap? = null
    private var savedDrawing: Bitmap? = null
    private val undoStack = ArrayDeque<CanvasSnapshot>()
    private val redoStack = ArrayDeque<CanvasSnapshot>()
    private var isDrawing = false
    private var lastX = 0f
    private var lastY = 0f
    private var selectedColor = brushColor
    private var pendingBackgroundPicker = false
    private var saveFormat = "PNG"
    private var appearanceDirty = false
    private var previewTheme = prefs.getString("theme", "跟随系统") ?: "跟随系统"
    private var previewFont = prefs.getString("font", "系统默认") ?: "系统默认"
    private var aboutOffset = 0f
    private var licenseOffset = 0f
    private var downY = 0f
    private var movedY = 0f
    private var toastMessage: String? = null
    private var toastUntil = 0L
    private var fileName = defaultFileName()

    init {
        isFocusable = true
        if (keepAwake) host.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setBackgroundColor(Color.rgb(252, 250, 253))
        ink.style = Paint.Style.STROKE
        ink.strokeCap = Paint.Cap.ROUND
        ink.strokeJoin = Paint.Join.ROUND
        fillPaint.style = Paint.Style.FILL
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeCap = Paint.Cap.ROUND
        strokePaint.strokeJoin = Paint.Join.ROUND
        textPaint.typeface = android.graphics.Typeface.create("sans", android.graphics.Typeface.NORMAL)
        dividerPaint.color = Color.rgb(225, 225, 232)
        dividerPaint.strokeWidth = max(1f, d)
    }

    private fun defaultFileName(): String =
        "Drawing_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        canvasBottom = h - if (showBrushTools) bottomHeight else 0f
        if (!::foreground.isInitialized || foreground.width != w || foreground.height != h) {
            val old = if (::foreground.isInitialized) foreground else null
            foreground = Bitmap.createBitmap(max(1, w), max(1, h), Bitmap.Config.ARGB_8888)
            foregroundCanvas = Canvas(foreground)
            if (old != null) foregroundCanvas.drawBitmap(old, null, Rect(0, 0, w, h), null)
        }
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        textPaint.typeface = android.graphics.Typeface.create(
            if (previewFont == "等宽字体") "monospace" else "sans",
            android.graphics.Typeface.NORMAL
        )
        when (page) {
            Page.CANVAS -> drawCanvasPage(c)
            Page.SETTINGS -> drawSettings(c)
            Page.APPEARANCE -> drawAppearance(c)
            Page.ABOUT -> drawAbout(c)
            Page.LICENSES -> drawLicenses(c)
        }
        drawPopup(c)
        if (toastMessage != null && System.currentTimeMillis() < toastUntil) {
            val msg = toastMessage ?: ""
            val box = RectF(width * .20f, height - 220f * d, width * .80f, height - 170f * d)
            fillPaint.color = Color.argb(225, 50, 50, 55)
            c.drawRoundRect(box, 24f * d, 24f * d, fillPaint)
            drawCentered(c, msg, box.centerX(), box.centerY() + 5f * d, 15f, Color.WHITE)
            postInvalidateDelayed(200)
        } else {
            toastMessage = null
        }
    }

    private fun drawCanvasPage(c: Canvas) {
        fillPaint.color = Color.rgb(252, 250, 253)
        c.drawRect(0f, 0f, width.toFloat(), toolbarHeight + toolHeight, fillPaint)
        c.drawLine(0f, toolbarHeight, width.toFloat(), toolbarHeight, dividerPaint)
        c.drawLine(0f, toolbarHeight + toolHeight, width.toFloat(), toolbarHeight + toolHeight, dividerPaint)

        drawTopActions(c)
        drawToolActions(c)

        canvasBottom = height - if (showBrushTools) bottomHeight else 0f
        c.save()
        c.clipRect(0f, canvasTop, width.toFloat(), canvasBottom)
        fillPaint.color = backgroundColor
        c.drawRect(0f, canvasTop, width.toFloat(), canvasBottom, fillPaint)
        importedImage?.let { drawCenterCrop(c, it, RectF(0f, canvasTop, width.toFloat(), canvasBottom)) }
        c.drawBitmap(foreground, 0f, 0f, null)
        c.restore()

        if (showBrushTools) {
            fillPaint.color = Color.rgb(252, 250, 253)
            c.drawRect(0f, canvasBottom, width.toFloat(), height.toFloat(), fillPaint)
            c.drawLine(0f, canvasBottom, width.toFloat(), canvasBottom, dividerPaint)
            val previewY = canvasBottom + 47f * d
            fillPaint.color = brushColor
            val previewR = (brushWidthDp * d / 2f).coerceIn(3f * d, 28f * d)
            c.drawCircle(width / 2f, previewY, previewR, fillPaint)
            val trackLeft = width * .26f
            val trackRight = width * .74f
            val trackY = canvasBottom + 105f * d
            strokePaint.color = Color.rgb(185, 188, 199)
            strokePaint.strokeWidth = 3f * d
            c.drawLine(trackLeft, trackY, trackRight, trackY, strokePaint)
            val fraction = ((brushWidthDp - 4f) / 56f).coerceIn(0f, 1f)
            val thumbX = trackLeft + (trackRight - trackLeft) * fraction
            strokePaint.color = Color.rgb(68, 82, 120)
            strokePaint.strokeWidth = 4f * d
            c.drawLine(trackLeft, trackY, thumbX, trackY, strokePaint)
            fillPaint.color = Color.rgb(68, 82, 120)
            c.drawCircle(thumbX, trackY, 9f * d, fillPaint)
        }
    }

    private fun drawTopActions(c: Canvas) {
        val cell = 48f * d
        val cy = toolbarHeight / 2f
        val iconColor = Color.rgb(56, 57, 64)
        for (i in 0..3) {
            val cx = width - cell * (3.5f - i)
            when (i) {
                0 -> drawSaveIcon(c, cx, cy, iconColor)
                1 -> drawTrashIcon(c, cx, cy, iconColor)
                2 -> drawShareIcon(c, cx, cy, iconColor)
                3 -> {
                    fillPaint.color = iconColor
                    c.drawCircle(cx, cy - 7f * d, 2f * d, fillPaint)
                    c.drawCircle(cx, cy, 2f * d, fillPaint)
                    c.drawCircle(cx, cy + 7f * d, 2f * d, fillPaint)
                }
            }
        }
    }

    private fun drawToolActions(c: Canvas) {
        val cell = 48f * d
        val cy = toolbarHeight + toolHeight / 2f
        val active = Color.rgb(74, 92, 142)
        val normal = Color.rgb(72, 72, 78)
        val base = width - 4f * cell

        if (undoStack.isNotEmpty()) drawUndoIcon(c, width - 6f * cell, cy, normal, false)
        if (redoStack.isNotEmpty()) drawUndoIcon(c, width - 5f * cell, cy, normal, true)

        drawFillIcon(c, base + .5f * cell, cy, if (tool == ToolMode.FILL) active else normal)
        drawEraserIcon(c, base + 1.5f * cell, cy, if (tool == ToolMode.ERASER) active else normal)
        drawPickerIcon(c, base + 2.5f * cell, cy, if (tool == ToolMode.PICKER) active else normal)
        val swatch = RectF(base + 3.15f * cell, cy - 13f * d, base + 3.85f * cell, cy + 13f * d)
        fillPaint.color = brushColor
        c.drawRoundRect(swatch, 4f * d, 4f * d, fillPaint)
        strokePaint.color = Color.rgb(80, 80, 85)
        strokePaint.strokeWidth = 1.5f * d
        c.drawRoundRect(swatch, 4f * d, 4f * d, strokePaint)
    }

    private fun drawSaveIcon(c: Canvas, x: Float, y: Float, color: Int) {
        strokePaint.color = color
        strokePaint.strokeWidth = 2f * d
        strokePaint.style = Paint.Style.STROKE
        val r = RectF(x - 10f * d, y - 11f * d, x + 10f * d, y + 11f * d)
        c.drawRect(r, strokePaint)
        c.drawRect(RectF(x - 5f * d, y - 10f * d, x + 5f * d, y - 2f * d), strokePaint)
        c.drawRect(RectF(x - 5f * d, y + 3f * d, x + 5f * d, y + 10f * d), strokePaint)
    }

    private fun drawTrashIcon(c: Canvas, x: Float, y: Float, color: Int) {
        strokePaint.color = color
        strokePaint.strokeWidth = 2f * d
        c.drawRect(RectF(x - 8f * d, y - 6f * d, x + 8f * d, y + 11f * d), strokePaint)
        c.drawLine(x - 11f * d, y - 9f * d, x + 11f * d, y - 9f * d, strokePaint)
        c.drawLine(x - 4f * d, y - 13f * d, x + 4f * d, y - 13f * d, strokePaint)
    }

    private fun drawShareIcon(c: Canvas, x: Float, y: Float, color: Int) {
        strokePaint.color = color
        strokePaint.strokeWidth = 2.2f * d
        c.drawLine(x - 8f * d, y + 5f * d, x + 7f * d, y - 7f * d, strokePaint)
        c.drawLine(x + 7f * d, y - 7f * d, x + 1f * d, y - 7f * d, strokePaint)
        c.drawLine(x + 7f * d, y - 7f * d, x + 7f * d, y - 1f * d, strokePaint)
        c.drawRect(RectF(x - 10f * d, y - 2f * d, x + 3f * d, y + 11f * d), strokePaint)
    }

    private fun drawUndoIcon(c: Canvas, x: Float, y: Float, color: Int, redo: Boolean) {
        strokePaint.color = color
        strokePaint.strokeWidth = 2.2f * d
        val dir = if (redo) -1f else 1f
        val rect = RectF(x - 10f * d, y - 9f * d, x + 10f * d, y + 9f * d)
        c.drawArc(rect, if (redo) 200f else 160f, if (redo) -235f else 235f, false, strokePaint)
        c.drawLine(x - dir * 10f * d, y, x - dir * 4f * d, y - 6f * d, strokePaint)
        c.drawLine(x - dir * 10f * d, y, x - dir * 3f * d, y + 3f * d, strokePaint)
    }

    private fun drawFillIcon(c: Canvas, x: Float, y: Float, color: Int) {
        strokePaint.color = color
        strokePaint.strokeWidth = 2.2f * d
        c.save()
        c.rotate(-42f, x, y)
        c.drawRect(RectF(x - 8f * d, y - 8f * d, x + 7f * d, y + 7f * d), strokePaint)
        c.restore()
        fillPaint.color = color
        c.drawCircle(x + 10f * d, y + 7f * d, 3f * d, fillPaint)
    }

    private fun drawEraserIcon(c: Canvas, x: Float, y: Float, color: Int) {
        strokePaint.color = color
        strokePaint.strokeWidth = 2.2f * d
        c.save()
        c.rotate(-42f, x, y)
        c.drawRoundRect(RectF(x - 10f * d, y - 7f * d, x + 10f * d, y + 7f * d), 2f * d, 2f * d, strokePaint)
        c.drawLine(x + 2f * d, y - 7f * d, x + 2f * d, y + 7f * d, strokePaint)
        c.restore()
    }

    private fun drawPickerIcon(c: Canvas, x: Float, y: Float, color: Int) {
        strokePaint.color = color
        strokePaint.strokeWidth = 3f * d
        c.drawLine(x - 8f * d, y + 9f * d, x + 7f * d, y - 8f * d, strokePaint)
        c.drawCircle(x + 7f * d, y - 8f * d, 4f * d, strokePaint)
        c.drawLine(x - 10f * d, y + 11f * d, x - 4f * d, y + 11f * d, strokePaint)
    }

    private fun drawHeader(c: Canvas, title: String) {
        fillPaint.color = Color.rgb(252, 250, 253)
        c.drawRect(0f, 0f, width.toFloat(), toolbarHeight, fillPaint)
        c.drawLine(0f, toolbarHeight, width.toFloat(), toolbarHeight, dividerPaint)
        strokePaint.color = Color.rgb(52, 53, 60)
        strokePaint.strokeWidth = 2.3f * d
        c.drawLine(28f * d, toolbarHeight / 2f, 39f * d, toolbarHeight / 2f - 10f * d, strokePaint)
        c.drawLine(28f * d, toolbarHeight / 2f, 39f * d, toolbarHeight / 2f + 10f * d, strokePaint)
        drawText(c, title, 56f * d, toolbarHeight / 2f + 7f * d, 20f, Color.rgb(37, 37, 43))
    }

    private fun drawSettings(c: Canvas) {
        c.drawColor(Color.rgb(252, 250, 253))
        drawHeader(c, "设置")
        var y = toolbarHeight
        y = drawRow(c, "自定义外观", "主题、图标颜色与字体", y, true)
        y = drawRow(c, "应用语言", "简体中文", y, true)
        y = drawToggleRow(c, "保持屏幕常亮", "绘画时防止设备进入休眠", y, keepAwake)
        y = drawToggleRow(c, "显示画笔大小工具", "在画布底部显示预览与滑块", y, showBrushTools)
        y = drawToggleRow(c, "允许手势缩放和平移", "使用手势浏览画布", y, allowGestures)
        y = drawToggleRow(c, "按缩放比例调整画笔大小", "", y, zoomBrush)
        drawToggleRow(c, "强制竖屏", "", y, forcePortrait)
    }

    private fun drawAppearance(c: Canvas) {
        c.drawColor(Color.rgb(252, 250, 253))
        drawHeader(c, "自定义外观")
        var y = toolbarHeight
        y = drawRow(c, "应用主题", previewTheme, y, true)
        y = drawRow(c, "应用图标颜色", "绿色", y, true)
        y = drawRow(c, "应用字体", previewFont, y, true)
        val card = RectF(24f * d, y + 25f * d, width - 24f * d, y + 190f * d)
        fillPaint.color = when (previewTheme) {
            "深色" -> Color.rgb(40, 40, 45)
            "深红色" -> Color.rgb(72, 28, 31)
            "黑白" -> Color.BLACK
            else -> Color.rgb(247, 249, 255)
        }
        c.drawRoundRect(card, 18f * d, 18f * d, fillPaint)
        drawText(c, "实时预览", card.left + 20f * d, card.top + 38f * d, 16f, if (previewTheme in listOf("深色", "深红色", "黑白")) Color.WHITE else Color.DKGRAY)
        fillPaint.color = Color.rgb(43, 119, 64)
        c.drawRoundRect(RectF(card.left + 20f * d, card.top + 65f * d, card.right - 20f * d, card.top + 118f * d), 12f * d, 12f * d, fillPaint)
        drawCentered(c, "✓  主色预览", card.centerX(), card.top + 98f * d, 15f, Color.WHITE)
        drawText(c, "更改会在保存后应用", card.left + 20f * d, card.bottom - 20f * d, 13f, Color.GRAY)
    }

    private fun drawAbout(c: Canvas) {
        c.drawColor(Color.rgb(252, 250, 253))
        drawHeader(c, "关于")
        c.save()
        c.clipRect(0f, toolbarHeight, width.toFloat(), height.toFloat())
        c.translate(0f, -aboutOffset)
        var y = toolbarHeight + 22f * d
        drawSection(c, "支持", y)
        y += 36f * d
        y = drawRow(c, "已知问题", "查看常见问题与解决办法", y, true, 65f)
        y = drawRow(c, "通过电子邮件联系我们", "", y, true, 65f)
        drawSection(c, "帮助我们", y + 18f * d)
        y += 54f * d
        y = drawRow(c, "分享应用", "", y, true, 60f)
        y = drawRow(c, "贡献者", "", y, true, 60f)
        y = drawRow(c, "捐赠", "", y, true, 60f)
        drawSection(c, "社交", y + 18f * d)
        y += 54f * d
        y = drawRow(c, "GitHub", "", y, true, 58f)
        y = drawRow(c, "Reddit", "", y, true, 58f)
        y = drawRow(c, "Telegram", "", y, true, 58f)
        drawSection(c, "其他", y + 18f * d)
        y += 54f * d
        y = drawRow(c, "隐私政策", "", y, true, 58f)
        y = drawRow(c, "第三方许可", "", y, true, 58f)
        drawCentered(c, "版本 1.4.0", width / 2f, y + 42f * d, 13f, Color.GRAY)
        c.restore()
    }

    private fun drawLicenses(c: Canvas) {
        c.drawColor(Color.rgb(252, 250, 253))
        drawHeader(c, "第三方许可")
        c.save()
        c.clipRect(0f, toolbarHeight, width.toFloat(), height.toFloat())
        var y = toolbarHeight + 36f * d - licenseOffset
        val blocks = listOf(
            "开放源代码许可" to "本应用包含由社区维护的开放源代码组件。以下许可文本随分发版本一同提供。",
            "Apache License 2.0" to "Copyright © respective contributors\nLicensed under the Apache License, Version 2.0. Software is provided on an AS IS basis, without warranties or conditions of any kind.",
            "MIT License" to "Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files.",
            "BSD License" to "Redistribution and use in source and binary forms, with or without modification, are permitted provided that the copyright notice is retained.",
            "图片素材" to "演示图片仅用于本地导入和导出流程测试，不包含个人信息。"
        )
        repeat(2) {
            for ((title, body) in blocks) {
                drawText(c, title, 22f * d, y, 17f, Color.rgb(45, 46, 53))
                y += 30f * d
                for (line in wrapText(body, 43)) {
                    drawText(c, line, 22f * d, y, 13f, Color.rgb(85, 85, 92))
                    y += 22f * d
                }
                y += 25f * d
            }
        }
        c.restore()
    }

    private fun drawSection(c: Canvas, label: String, y: Float) {
        drawText(c, label, 22f * d, y + 18f * d, 13f, Color.rgb(74, 92, 142))
    }

    private fun drawRow(c: Canvas, title: String, subtitle: String, top: Float, arrow: Boolean, heightDp: Float = 76f): Float {
        val h = heightDp * d
        drawText(c, title, 22f * d, top + if (subtitle.isEmpty()) 38f * d else 29f * d, 16f, Color.rgb(41, 41, 47))
        if (subtitle.isNotEmpty()) drawText(c, subtitle, 22f * d, top + 52f * d, 13f, Color.rgb(105, 105, 113))
        if (arrow) {
            strokePaint.color = Color.rgb(125, 125, 133)
            strokePaint.strokeWidth = 1.8f * d
            c.drawLine(width - 30f * d, top + h / 2f - 6f * d, width - 24f * d, top + h / 2f, strokePaint)
            c.drawLine(width - 24f * d, top + h / 2f, width - 30f * d, top + h / 2f + 6f * d, strokePaint)
        }
        c.drawLine(22f * d, top + h, width.toFloat(), top + h, dividerPaint)
        return top + h
    }

    private fun drawToggleRow(c: Canvas, title: String, subtitle: String, top: Float, on: Boolean): Float {
        val h = 76f * d
        drawText(c, title, 22f * d, top + if (subtitle.isEmpty()) 42f * d else 28f * d, 15.5f, Color.rgb(41, 41, 47))
        if (subtitle.isNotEmpty()) drawText(c, subtitle, 22f * d, top + 51f * d, 12.5f, Color.rgb(105, 105, 113))
        val r = RectF(width - 66f * d, top + 25f * d, width - 20f * d, top + 51f * d)
        fillPaint.color = if (on) Color.rgb(75, 94, 146) else Color.rgb(205, 206, 213)
        c.drawRoundRect(r, 15f * d, 15f * d, fillPaint)
        fillPaint.color = Color.WHITE
        c.drawCircle(if (on) r.right - 13f * d else r.left + 13f * d, r.centerY(), 10f * d, fillPaint)
        c.drawLine(22f * d, top + h, width.toFloat(), top + h, dividerPaint)
        return top + h
    }

    private fun drawPopup(c: Canvas) {
        when (popup) {
            Popup.NONE -> Unit
            Popup.OVERFLOW -> drawOverflow(c)
            Popup.COLOR, Popup.BACKGROUND_COLOR -> drawColorPopup(c)
            Popup.CLEAR -> drawConfirm(c, "确定要清除画板吗？", "当前画布内容将被清除。", "清除")
            Popup.SAVE -> drawSavePopup(c)
            Popup.SHARE -> drawSharePopup(c)
            Popup.FILE_PICKER -> drawFilePicker(c)
            Popup.THEME -> drawChoicePopup(c, "应用主题", listOf("跟随系统", "浅色", "深色", "深红色", "纯白", "黑白", "自定义"), previewTheme)
            Popup.FONT -> drawChoicePopup(c, "应用字体", listOf("系统默认", "等宽字体", "选择字体文件"), previewFont)
            Popup.GUARD -> drawConfirm(c, "您尚未保存更改", "是否保存外观更改？", "保存")
        }
    }

    private fun dim(c: Canvas) {
        fillPaint.color = Color.argb(105, 0, 0, 0)
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fillPaint)
    }

    private fun drawOverflow(c: Canvas) {
        val menuW = 225f * d
        val left = width - menuW - 8f * d
        val top = 8f * d
        val row = 52f * d
        fillPaint.color = Color.WHITE
        strokePaint.color = Color.rgb(222, 222, 228)
        strokePaint.strokeWidth = d
        c.drawRoundRect(RectF(left, top, width - 8f * d, top + row * 5), 6f * d, 6f * d, fillPaint)
        c.drawRoundRect(RectF(left, top, width - 8f * d, top + row * 5), 6f * d, 6f * d, strokePaint)
        val names = listOf("打开文件", "更改背景颜色", "打印", "设置", "关于")
        names.forEachIndexed { i, name ->
            drawText(c, name, left + 22f * d, top + row * i + 33f * d, 16f, Color.rgb(40, 40, 46))
        }
    }

    private fun dialogRect(heightDp: Float = 520f): RectF {
        val w = min(width - 40f * d, 340f * d)
        val h = min(height - 80f * d, heightDp * d)
        return RectF((width - w) / 2f, (height - h) / 2f, (width + w) / 2f, (height + h) / 2f)
    }

    private fun drawColorPopup(c: Canvas) {
        dim(c)
        val r = dialogRect(520f)
        fillPaint.color = Color.WHITE
        c.drawRoundRect(r, 18f * d, 18f * d, fillPaint)
        drawText(c, if (popup == Popup.BACKGROUND_COLOR) "更改背景颜色" else "选择颜色", r.left + 22f * d, r.top + 38f * d, 20f, Color.rgb(35, 35, 40))

        val presets = listOf(Color.rgb(211,47,47), Color.rgb(25,118,210), Color.rgb(46,125,50), Color.rgb(251,192,45), Color.rgb(239,108,0))
        presets.forEachIndexed { i, col ->
            val x = r.left + (35f + i * 58f) * d
            val y = r.top + 76f * d
            fillPaint.color = col
            c.drawCircle(x, y, 17f * d, fillPaint)
            if (selectedColor == col) {
                strokePaint.color = Color.rgb(40,40,45)
                strokePaint.strokeWidth = 2.5f * d
                c.drawCircle(x, y, 21f * d, strokePaint)
            }
        }

        val square = RectF(r.left + 22f * d, r.top + 112f * d, r.right - 58f * d, r.top + 300f * d)
        fillPaint.shader = LinearGradient(square.left, square.top, square.right, square.bottom, Color.WHITE, selectedColor, Shader.TileMode.CLAMP)
        c.drawRect(square, fillPaint)
        fillPaint.shader = null
        val dark = Paint()
        dark.shader = LinearGradient(square.left, square.top, square.left, square.bottom, Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP)
        c.drawRect(square, dark)
        val hue = RectF(r.right - 43f * d, square.top, r.right - 22f * d, square.bottom)
        val hues = intArrayOf(Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED)
        val positions = floatArrayOf(0f,.17f,.34f,.5f,.67f,.84f,1f)
        fillPaint.shader = LinearGradient(hue.left, hue.top, hue.left, hue.bottom, hues, positions, Shader.TileMode.CLAMP)
        c.drawRoundRect(hue, 6f * d, 6f * d, fillPaint)
        fillPaint.shader = null

        fillPaint.color = if (popup == Popup.BACKGROUND_COLOR) backgroundColor else brushColor
        c.drawRoundRect(RectF(r.left + 22f*d, r.top + 322f*d, r.left + 72f*d, r.top + 372f*d), 7f*d, 7f*d, fillPaint)
        fillPaint.color = selectedColor
        c.drawRoundRect(RectF(r.left + 84f*d, r.top + 322f*d, r.left + 134f*d, r.top + 372f*d), 7f*d, 7f*d, fillPaint)
        drawText(c, "当前", r.left + 23f*d, r.top + 393f*d, 12f, Color.GRAY)
        drawText(c, "新颜色", r.left + 84f*d, r.top + 393f*d, 12f, Color.GRAY)
        drawText(c, "#%06X".format(selectedColor and 0xFFFFFF), r.left + 158f*d, r.top + 355f*d, 16f, Color.rgb(50,50,55))
        drawText(c, "取消", r.right - 145f*d, r.bottom - 27f*d, 15f, Color.rgb(70,88,136))
        drawText(c, "确认", r.right - 72f*d, r.bottom - 27f*d, 15f, Color.rgb(70,88,136))
    }

    private fun drawConfirm(c: Canvas, title: String, subtitle: String, positive: String) {
        dim(c)
        val r = dialogRect(205f)
        fillPaint.color = Color.WHITE
        c.drawRoundRect(r, 18f * d, 18f * d, fillPaint)
        drawText(c, title, r.left + 22f * d, r.top + 42f * d, 20f, Color.rgb(35,35,40))
        drawText(c, subtitle, r.left + 22f * d, r.top + 82f * d, 14f, Color.rgb(90,90,98))
        drawText(c, if (popup == Popup.CLEAR) "否" else "放弃", r.right - 148f*d, r.bottom - 28f*d, 15f, Color.rgb(70,88,136))
        drawText(c, positive, r.right - 70f*d, r.bottom - 28f*d, 15f, Color.rgb(70,88,136))
    }

    private fun drawSavePopup(c: Canvas) {
        dim(c)
        val r = dialogRect(340f)
        fillPaint.color = Color.WHITE
        c.drawRoundRect(r, 18f*d, 18f*d, fillPaint)
        drawText(c, "另存为", r.left + 22f*d, r.top + 42f*d, 20f, Color.rgb(35,35,40))
        drawText(c, "文件名", r.left + 22f*d, r.top + 82f*d, 12f, Color.rgb(90,90,98))
        fillPaint.color = Color.rgb(246,246,251)
        c.drawRoundRect(RectF(r.left+20f*d,r.top+94f*d,r.right-20f*d,r.top+144f*d),8f*d,8f*d,fillPaint)
        drawText(c, fileName, r.left+32f*d, r.top+126f*d, 15f, Color.rgb(45,45,50))
        val formats = listOf("PNG","SVG","JPG")
        formats.forEachIndexed { i, f ->
            val y = r.top + (178f + i*38f)*d
            strokePaint.color = Color.rgb(80,91,130)
            strokePaint.strokeWidth = 2f*d
            c.drawCircle(r.left+34f*d,y,9f*d,strokePaint)
            if(saveFormat==f){ fillPaint.color=Color.rgb(80,91,130); c.drawCircle(r.left+34f*d,y,5f*d,fillPaint)}
            drawText(c,f,r.left+55f*d,y+5f*d,15f,Color.rgb(45,45,50))
        }
        drawText(c,"取消",r.right-145f*d,r.bottom-27f*d,15f,Color.rgb(70,88,136))
        drawText(c,"确认",r.right-72f*d,r.bottom-27f*d,15f,Color.rgb(70,88,136))
    }

    private fun drawSharePopup(c: Canvas) {
        dim(c)
        val h = 300f*d
        val r = RectF(0f,height-h,width.toFloat(),height.toFloat())
        fillPaint.color=Color.WHITE
        c.drawRoundRect(r,22f*d,22f*d,fillPaint)
        drawCentered(c,"分享绘画",width/2f,r.top+42f*d,20f,Color.rgb(35,35,40))
        drawCentered(c,"选择分享方式",width/2f,r.top+76f*d,13f,Color.GRAY)
        val names=listOf("消息","邮件","保存副本")
        names.forEachIndexed { i,n ->
            val x=width*(.23f+i*.27f)
            fillPaint.color=listOf(Color.rgb(73,112,191),Color.rgb(55,142,92),Color.rgb(132,91,178))[i]
            c.drawCircle(x,r.top+145f*d,27f*d,fillPaint)
            drawCentered(c,n,x,r.top+190f*d,13f,Color.rgb(50,50,55))
        }
        fillPaint.color=Color.rgb(240,241,247)
        c.drawRoundRect(RectF(24f*d,r.bottom-62f*d,width-24f*d,r.bottom-18f*d),22f*d,22f*d,fillPaint)
        drawCentered(c,"取消",width/2f,r.bottom-34f*d,15f,Color.rgb(55,55,62))
    }

    private fun drawFilePicker(c: Canvas) {
        dim(c)
        val h = 515f*d
        val r = RectF(0f,max(0f,height-h),width.toFloat(),height.toFloat())
        fillPaint.color=Color.WHITE
        c.drawRoundRect(r,22f*d,22f*d,fillPaint)
        drawCentered(c,"选择图片",width/2f,r.top+42f*d,20f,Color.rgb(35,35,40))
        drawCentered(c,"仅显示此设备上的图片",width/2f,r.top+70f*d,12.5f,Color.GRAY)
        drawText(c,"最近",22f*d,r.top+110f*d,16f,Color.rgb(45,45,52))
        val thumbTop=r.top+132f*d
        val thumbW=(width-56f*d)/2f
        val leftRect=RectF(18f*d,thumbTop,18f*d+thumbW,thumbTop+185f*d)
        val rightRect=RectF(38f*d+thumbW,thumbTop,width-18f*d,thumbTop+185f*d)
        if(savedDrawing!=null) drawCenterCrop(c,savedDrawing!!,leftRect) else {
            fillPaint.color=Color.rgb(235,236,242);c.drawRoundRect(leftRect,8f*d,8f*d,fillPaint)
            drawCentered(c,"暂无已保存绘画",leftRect.centerX(),leftRect.centerY(),13f,Color.GRAY)
        }
        val sample=BitmapFactory.decodeResource(resources,R.drawable.lake)
        drawCenterCrop(c,sample,rightRect)
        drawCentered(c,"已保存的绘画",leftRect.centerX(),thumbTop+210f*d,13f,Color.rgb(55,55,62))
        drawCentered(c,"导入为背景",rightRect.centerX(),thumbTop+210f*d,13f,Color.rgb(55,55,62))
        fillPaint.color=Color.rgb(240,241,247)
        c.drawRoundRect(RectF(24f*d,r.bottom-62f*d,width-24f*d,r.bottom-18f*d),22f*d,22f*d,fillPaint)
        drawCentered(c,"取消",width/2f,r.bottom-34f*d,15f,Color.rgb(55,55,62))
    }

    private fun drawChoicePopup(c: Canvas, title: String, choices: List<String>, selected: String) {
        dim(c)
        val r=dialogRect(if(choices.size>4) 440f else 265f)
        fillPaint.color=Color.WHITE
        c.drawRoundRect(r,18f*d,18f*d,fillPaint)
        drawText(c,title,r.left+22f*d,r.top+42f*d,20f,Color.rgb(35,35,40))
        choices.forEachIndexed{i,name->
            val y=r.top+(80f+i*43f)*d
            strokePaint.color=Color.rgb(75,91,140);strokePaint.strokeWidth=2f*d
            c.drawCircle(r.left+34f*d,y,9f*d,strokePaint)
            if(name==selected){fillPaint.color=Color.rgb(75,91,140);c.drawCircle(r.left+34f*d,y,5f*d,fillPaint)}
            drawText(c,name,r.left+56f*d,y+5f*d,15f,Color.rgb(45,45,51))
        }
        drawText(c,"取消",r.right-72f*d,r.bottom-25f*d,15f,Color.rgb(70,88,136))
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.actionMasked == MotionEvent.ACTION_DOWN) {
            downY = e.y
            movedY = e.y
        }
        if (popup != Popup.NONE) {
            handlePopupTouch(e)
            return true
        }
        when (page) {
            Page.CANVAS -> handleCanvasTouch(e)
            Page.SETTINGS -> handleSettingsTouch(e)
            Page.APPEARANCE -> handleAppearanceTouch(e)
            Page.ABOUT -> handleAboutTouch(e)
            Page.LICENSES -> handleLicenseTouch(e)
        }
        return true
    }

    private fun handleCanvasTouch(e: MotionEvent) {
        val x=e.x;val y=e.y
        if(e.actionMasked==MotionEvent.ACTION_UP && y<toolbarHeight){
            val cell=48f*d
            when{
                x>width-cell -> popup=Popup.OVERFLOW
                x>width-2*cell -> popup=Popup.SHARE
                x>width-3*cell -> popup=Popup.CLEAR
                x>width-4*cell -> {fileName=defaultFileName();popup=Popup.SAVE}
            }
            invalidate();return
        }
        if(e.actionMasked==MotionEvent.ACTION_UP && y in toolbarHeight..canvasTop){
            val cell=48f*d
            when{
                x>width-cell -> {selectedColor=brushColor;popup=Popup.COLOR}
                x>width-2*cell -> tool=ToolMode.PICKER
                x>width-3*cell -> tool=ToolMode.ERASER
                x>width-4*cell -> tool=ToolMode.FILL
                x>width-5*cell && redoStack.isNotEmpty() -> redo()
                x>width-6*cell && undoStack.isNotEmpty() -> undo()
                else -> tool=ToolMode.BRUSH
            }
            invalidate();return
        }
        if(showBrushTools && y>canvasBottom){
            if(e.actionMasked==MotionEvent.ACTION_DOWN || e.actionMasked==MotionEvent.ACTION_MOVE){
                val left=width*.26f;val right=width*.74f
                brushWidthDp=(4f+56f*((x-left)/(right-left))).coerceIn(4f,60f)
                prefs.edit().putFloat("brushWidth",brushWidthDp).apply()
                invalidate()
            }
            return
        }
        if(y<canvasTop || y>canvasBottom)return
        when(tool){
            ToolMode.BRUSH,ToolMode.ERASER->{
                when(e.actionMasked){
                    MotionEvent.ACTION_DOWN->{
                        pushUndo();isDrawing=true;lastX=x;lastY=y;drawSegment(x,y,x,y);invalidate()
                    }
                    MotionEvent.ACTION_MOVE->if(isDrawing){drawSegment(lastX,lastY,x,y);lastX=x;lastY=y;invalidate()}
                    MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL->{if(isDrawing){drawSegment(lastX,lastY,x,y);isDrawing=false;invalidate()}}
                }
            }
            ToolMode.FILL->if(e.actionMasked==MotionEvent.ACTION_DOWN){
                pushUndo()
                foregroundCanvas.save()
                foregroundCanvas.clipRect(0f,canvasTop,width.toFloat(),canvasBottom)
                foregroundCanvas.drawColor(brushColor)
                foregroundCanvas.restore()
                invalidate()
            }
            ToolMode.PICKER->if(e.actionMasked==MotionEvent.ACTION_DOWN){
                val px=x.toInt().coerceIn(0,foreground.width-1)
                val py=y.toInt().coerceIn(0,foreground.height-1)
                val picked=foreground.getPixel(px,py)
                brushColor=if(Color.alpha(picked)>20)picked else sampleBaseColor(x,y)
                selectedColor=brushColor
                prefs.edit().putInt("brushColor",brushColor).apply()
                invalidate()
            }
        }
    }

    private fun drawSegment(x1:Float,y1:Float,x2:Float,y2:Float){
        ink.strokeWidth=brushWidthDp*d
        ink.color=brushColor
        ink.xfermode=if(tool==ToolMode.ERASER) PorterDuffXfermode(PorterDuff.Mode.CLEAR) else null
        foregroundCanvas.drawLine(x1,y1,x2,y2,ink)
        ink.xfermode=null
    }

    private fun sampleBaseColor(x:Float,y:Float):Int{
        val img=importedImage ?: return backgroundColor
        val rect=RectF(0f,canvasTop,width.toFloat(),canvasBottom)
        val scale=max(rect.width()/img.width,rect.height()/img.height)
        val dw=img.width*scale;val dh=img.height*scale
        val left=rect.centerX()-dw/2f;val top=rect.centerY()-dh/2f
        val ix=((x-left)/scale).toInt().coerceIn(0,img.width-1)
        val iy=((y-top)/scale).toInt().coerceIn(0,img.height-1)
        return img.getPixel(ix,iy)
    }

    private fun pushUndo(){
        undoStack.addLast(CanvasSnapshot(foreground.copy(Bitmap.Config.ARGB_8888,true),backgroundColor))
        while(undoStack.size>24)undoStack.removeFirst()
        redoStack.clear()
    }
    private fun undo(){
        if(undoStack.isEmpty())return
        redoStack.addLast(CanvasSnapshot(foreground.copy(Bitmap.Config.ARGB_8888,true),backgroundColor))
        val s=undoStack.removeLast();foreground=s.bitmap;foregroundCanvas=Canvas(foreground);backgroundColor=s.background
        invalidate()
    }
    private fun redo(){
        if(redoStack.isEmpty())return
        undoStack.addLast(CanvasSnapshot(foreground.copy(Bitmap.Config.ARGB_8888,true),backgroundColor))
        val s=redoStack.removeLast();foreground=s.bitmap;foregroundCanvas=Canvas(foreground);backgroundColor=s.background
        invalidate()
    }

    private fun handleSettingsTouch(e:MotionEvent){
        if(e.actionMasked!=MotionEvent.ACTION_UP)return
        val y=e.y
        if(y<toolbarHeight){page=Page.CANVAS;invalidate();return}
        val index=((y-toolbarHeight)/(76f*d)).toInt()
        when(index){
            0->{page=Page.APPEARANCE;previewTheme=prefs.getString("theme","跟随系统")?:"跟随系统";previewFont=prefs.getString("font","系统默认")?:"系统默认";appearanceDirty=false}
            1->showToast("应用语言：简体中文")
            2->{keepAwake=!keepAwake;prefs.edit().putBoolean("keepAwake",keepAwake).apply();if(keepAwake)host.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)else host.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)}
            3->{showBrushTools=!showBrushTools;prefs.edit().putBoolean("showBrushTools",showBrushTools).apply();canvasBottom=height-if(showBrushTools)bottomHeight else 0f}
            4->{allowGestures=!allowGestures;prefs.edit().putBoolean("allowGestures",allowGestures).apply()}
            5->{zoomBrush=!zoomBrush;prefs.edit().putBoolean("zoomBrush",zoomBrush).apply()}
            6->{forcePortrait=!forcePortrait;prefs.edit().putBoolean("forcePortrait",forcePortrait).apply()}
        }
        invalidate()
    }

    private fun handleAppearanceTouch(e:MotionEvent){
        if(e.actionMasked!=MotionEvent.ACTION_UP)return
        val y=e.y
        if(y<toolbarHeight){if(appearanceDirty)popup=Popup.GUARD else page=Page.SETTINGS;invalidate();return}
        val idx=((y-toolbarHeight)/(76f*d)).toInt()
        when(idx){0->popup=Popup.THEME;1->showToast("应用图标颜色：绿色");2->popup=Popup.FONT}
        invalidate()
    }

    private fun handleAboutTouch(e:MotionEvent){
        when(e.actionMasked){
            MotionEvent.ACTION_MOVE->{
                val delta=movedY-e.y
                aboutOffset=(aboutOffset+delta).coerceIn(0f,360f*d)
                movedY=e.y
                invalidate()
            }
            MotionEvent.ACTION_UP->{
                if(abs(e.y-downY)<15f*d){
                    if(e.y<toolbarHeight){page=Page.CANVAS;invalidate();return}
                    val contentY=e.y+aboutOffset
                    val licenseTop=toolbarHeight+(22+36+65*2+54+60*3+54+58*3+54+58)*d
                    if(contentY>licenseTop && contentY<licenseTop+70f*d){page=Page.LICENSES;licenseOffset=0f}
                    else showToast("此项目将在系统应用中打开")
                }
                invalidate()
            }
        }
    }

    private fun handleLicenseTouch(e:MotionEvent){
        when(e.actionMasked){
            MotionEvent.ACTION_MOVE->{val delta=movedY-e.y;licenseOffset=(licenseOffset+delta).coerceIn(0f,950f*d);movedY=e.y;invalidate()}
            MotionEvent.ACTION_UP->{if(abs(e.y-downY)<15f*d&&e.y<toolbarHeight){page=Page.ABOUT;invalidate()}}
        }
    }

    private fun handlePopupTouch(e:MotionEvent){
        if(e.actionMasked!=MotionEvent.ACTION_UP)return
        val x=e.x;val y=e.y
        when(popup){
            Popup.OVERFLOW->{
                val menuW=225f*d;val left=width-menuW-8f*d;val row=52f*d;val top=8f*d
                if(x<left||y<top||y>top+row*5){popup=Popup.NONE}
                else when(((y-top)/row).toInt()){
                    0->popup=Popup.FILE_PICKER
                    1->{selectedColor=backgroundColor;popup=Popup.BACKGROUND_COLOR}
                    2->{popup=Popup.NONE;showToast("打印任务已准备")}
                    3->{popup=Popup.NONE;page=Page.SETTINGS}
                    4->{popup=Popup.NONE;page=Page.ABOUT;aboutOffset=0f}
                }
            }
            Popup.COLOR,Popup.BACKGROUND_COLOR->handleColorTouch(x,y)
            Popup.CLEAR->handleConfirmTouch(x,y,true)
            Popup.GUARD->handleConfirmTouch(x,y,false)
            Popup.SAVE->handleSaveTouch(x,y)
            Popup.SHARE->{if(y>height-85f*d||y<height-300f*d){popup=Popup.NONE}else showToast("分享副本已准备")}
            Popup.FILE_PICKER->handlePickerTouch(x,y)
            Popup.THEME->handleChoiceTouch(x,y,true)
            Popup.FONT->handleChoiceTouch(x,y,false)
            Popup.NONE->Unit
        }
        invalidate()
    }

    private fun handleColorTouch(x:Float,y:Float){
        val r=dialogRect(520f)
        if(x<r.left||x>r.right||y<r.top||y>r.bottom){popup=Popup.NONE;return}
        val presets=listOf(Color.rgb(211,47,47),Color.rgb(25,118,210),Color.rgb(46,125,50),Color.rgb(251,192,45),Color.rgb(239,108,0))
        if(y in (r.top+50f*d)..(r.top+101f*d)){
            val idx=((x-(r.left+6f*d))/(58f*d)).toInt().coerceIn(0,4);selectedColor=presets[idx];return
        }
        val square=RectF(r.left+22f*d,r.top+112f*d,r.right-58f*d,r.top+300f*d)
        if(square.contains(x,y)){
            val sx=((x-square.left)/square.width()).coerceIn(0f,1f)
            val sy=((y-square.top)/square.height()).coerceIn(0f,1f)
            val base=selectedColor
            val rr=((255*(1-sx)+Color.red(base)*sx)*(1-sy)).toInt()
            val gg=((255*(1-sx)+Color.green(base)*sx)*(1-sy)).toInt()
            val bb=((255*(1-sx)+Color.blue(base)*sx)*(1-sy)).toInt()
            selectedColor=Color.rgb(rr,gg,bb);return
        }
        if(y>r.bottom-70f*d){
            if(x>r.right-105f*d){
                if(popup==Popup.BACKGROUND_COLOR){
                    pushUndo();backgroundColor=selectedColor;prefs.edit().putInt("backgroundColor",backgroundColor).apply()
                }else{
                    brushColor=selectedColor;prefs.edit().putInt("brushColor",brushColor).apply()
                }
            }
            popup=Popup.NONE
        }
    }

    private fun handleConfirmTouch(x:Float,y:Float,clear:Boolean){
        val r=dialogRect(205f)
        if(y<r.bottom-75f*d)return
        if(x>r.right-110f*d){
            if(clear){
                pushUndo();foreground.eraseColor(Color.TRANSPARENT);importedImage=null;showToast("画板已清除")
            }else{
                prefs.edit().putString("theme",previewTheme).putString("font",previewFont).apply();appearanceDirty=false;page=Page.SETTINGS
            }
        }else if(!clear){
            previewTheme=prefs.getString("theme","跟随系统")?:"跟随系统";previewFont=prefs.getString("font","系统默认")?:"系统默认";appearanceDirty=false;page=Page.SETTINGS
        }
        popup=Popup.NONE
    }

    private fun handleSaveTouch(x:Float,y:Float){
        val r=dialogRect(340f)
        if(x<r.left||x>r.right||y<r.top||y>r.bottom){popup=Popup.NONE;return}
        for(i in 0..2){
            val yy=r.top+(178f+i*38f)*d
            if(abs(y-yy)<19f*d){saveFormat=listOf("PNG","SVG","JPG")[i];return}
        }
        if(y>r.bottom-70f*d){
            if(x>r.right-110f*d)exportDrawing()
            popup=Popup.NONE
        }
    }

    private fun exportDrawing(){
        val result=renderDrawingBitmap()
        savedDrawing=result.copy(Bitmap.Config.ARGB_8888,false)
        try{
            val ext=if(saveFormat=="JPG")"jpg" else "png"
            val out=File(host.filesDir,"$fileName.$ext")
            FileOutputStream(out).use{stream->
                result.compress(if(saveFormat=="JPG")Bitmap.CompressFormat.JPEG else Bitmap.CompressFormat.PNG,95,stream)
            }
            showToast("已保存到应用文件夹")
        }catch(_:Exception){showToast("保存失败")}
    }

    private fun handlePickerTouch(x:Float,y:Float){
        val top=max(0f,height-515f*d)
        if(y>height-85f*d){popup=Popup.NONE;return}
        val thumbTop=top+132f*d
        if(y in thumbTop..(thumbTop+225f*d)){
            val half=width/2f
            if(x<half&&savedDrawing!=null){
                importedImage=savedDrawing!!.copy(Bitmap.Config.ARGB_8888,false)
                foreground.eraseColor(Color.TRANSPARENT)
                undoStack.clear();redoStack.clear()
                popup=Popup.NONE;showToast("已打开保存的绘画")
            }else if(x>=half){
                importedImage=BitmapFactory.decodeResource(resources,R.drawable.lake)
                foreground.eraseColor(Color.TRANSPARENT)
                undoStack.clear();redoStack.clear()
                popup=Popup.NONE;showToast("图片已导入为背景")
            }
        }
    }

    private fun handleChoiceTouch(x:Float,y:Float,theme:Boolean){
        val choices=if(theme)listOf("跟随系统","浅色","深色","深红色","纯白","黑白","自定义")else listOf("系统默认","等宽字体","选择字体文件")
        val r=dialogRect(if(theme)440f else 265f)
        if(x<r.left||x>r.right||y<r.top||y>r.bottom){popup=Popup.NONE;return}
        val idx=((y-(r.top+58f*d))/(43f*d)).toInt()
        if(idx in choices.indices){
            if(theme)previewTheme=choices[idx] else previewFont=choices[idx]
            appearanceDirty=true
            popup=Popup.NONE
        }else if(y>r.bottom-60f*d)popup=Popup.NONE
    }

    fun handleBack():Boolean{
        if(popup!=Popup.NONE){popup=Popup.NONE;invalidate();return true}
        when(page){
            Page.CANVAS->return false
            Page.SETTINGS,Page.ABOUT->{page=Page.CANVAS}
            Page.LICENSES->{page=Page.ABOUT}
            Page.APPEARANCE->{if(appearanceDirty)popup=Popup.GUARD else page=Page.SETTINGS}
        }
        invalidate();return true
    }

    private fun renderDrawingBitmap():Bitmap{
        val h=max(1,(canvasBottom-canvasTop).toInt())
        val b=Bitmap.createBitmap(width,h,Bitmap.Config.ARGB_8888)
        val cc=Canvas(b)
        cc.drawColor(backgroundColor)
        importedImage?.let{drawCenterCrop(cc,it,RectF(0f,0f,width.toFloat(),h.toFloat()))}
        cc.drawBitmap(foreground,Rect(0,canvasTop.toInt(),width,canvasBottom.toInt()),Rect(0,0,width,h),null)
        return b
    }

    private fun drawCenterCrop(c:Canvas,b:Bitmap,dst:RectF){
        val scale=max(dst.width()/b.width,dst.height()/b.height)
        val sw=dst.width()/scale;val sh=dst.height()/scale
        val src=Rect(((b.width-sw)/2f).toInt(),((b.height-sh)/2f).toInt(),((b.width+sw)/2f).toInt(),((b.height+sh)/2f).toInt())
        c.drawBitmap(b,src,dst,fillPaint)
    }

    private fun showToast(msg:String){
        toastMessage=msg;toastUntil=System.currentTimeMillis()+1800;invalidate()
    }

    private fun drawText(c:Canvas,s:String,x:Float,y:Float,sp:Float,color:Int){
        textPaint.color=color;textPaint.textSize=sp*d
        c.drawText(s,x,y,textPaint)
    }
    private fun drawCentered(c:Canvas,s:String,x:Float,y:Float,sp:Float,color:Int){
        textPaint.color=color;textPaint.textSize=sp*d
        c.drawText(s,x-textPaint.measureText(s)/2f,y-(textPaint.ascent()+textPaint.descent())/2f,textPaint)
    }
    private fun wrapText(s:String,maxChars:Int):List<String>{
        val result=mutableListOf<String>()
        for(paragraph in s.split("\n")){
            var start=0
            while(start<paragraph.length){val end=min(paragraph.length,start+maxChars);result.add(paragraph.substring(start,end));start=end}
        }
        return result
    }
}
