package com.blackboxbench.reproduction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp

@Composable
fun Glyph(
    modifier: Modifier = Modifier,
    draw: DrawScope.() -> Unit,
) {
    Canvas(modifier) { draw() }
}

private fun DrawScope.line(
    x1: Float, y1: Float, x2: Float, y2: Float, color: Color, width: Float,
) = drawLine(color, Offset(x1, y1), Offset(x2, y2), strokeWidth = width, cap = StrokeCap.Round)

@Composable
fun DeleteTrashGlyph(modifier: Modifier = Modifier.size(24.dp), tint: Color = OnSurfaceVariant) {
    Glyph(modifier) {
        val w = size.width
        val h = size.height
        val sw = w * 0.08f
        line(w * 0.14f, h * 0.26f, w * 0.86f, h * 0.26f, tint, sw)
        line(w * 0.4f, h * 0.26f, w * 0.4f, h * 0.14f, tint, sw)
        line(w * 0.6f, h * 0.26f, w * 0.6f, h * 0.14f, tint, sw)
        line(w * 0.4f, h * 0.14f, w * 0.6f, h * 0.14f, tint, sw)
        val p = Path().apply {
            moveTo(w * 0.24f, h * 0.36f)
            lineTo(w * 0.3f, h * 0.88f)
            lineTo(w * 0.7f, h * 0.88f)
            lineTo(w * 0.76f, h * 0.36f)
        }
        drawPath(p, tint, style = Stroke(sw, join = StrokeJoin.Round, cap = StrokeCap.Round))
    }
}

@Composable
fun FolderGlyph(modifier: Modifier = Modifier.size(40.dp), tint: Color = OnSurfaceVariant) {
    Glyph(modifier) {
        val w = size.width
        val h = size.height
        val sw = w * 0.085f
        val p = Path().apply {
            moveTo(w * 0.13f, h * 0.79f)
            lineTo(w * 0.13f, h * 0.24f)
            lineTo(w * 0.40f, h * 0.24f)
            lineTo(w * 0.49f, h * 0.35f)
            lineTo(w * 0.87f, h * 0.35f)
            lineTo(w * 0.87f, h * 0.79f)
            close()
        }
        drawPath(p, tint, style = Stroke(sw, join = StrokeJoin.Round, cap = StrokeCap.Round))
    }
}

@Composable
fun SortGlyph(modifier: Modifier = Modifier.size(24.dp), tint: Color = OnSurfaceVariant) {
    Glyph(modifier) {
        val w = size.width
        val h = size.height
        val sw = w * 0.09f
        line(w * 0.14f, h * 0.25f, w * 0.86f, h * 0.25f, tint, sw)
        line(w * 0.14f, h * 0.5f, w * 0.62f, h * 0.5f, tint, sw)
        line(w * 0.14f, h * 0.75f, w * 0.38f, h * 0.75f, tint, sw)
    }
}

@Composable
fun GridGlyph(modifier: Modifier = Modifier.size(24.dp), tint: Color = OnSurfaceVariant) {
    Glyph(modifier) {
        val w = size.width
        val h = size.height
        val r = w * 0.08f
        val s = w * 0.34f
        drawRoundRect(tint, Offset(w * 0.12f, h * 0.12f), Size(s, s), CornerRadius(r, r))
        drawRoundRect(tint, Offset(w * 0.54f, h * 0.12f), Size(s, s), CornerRadius(r, r))
        drawRoundRect(tint, Offset(w * 0.12f, h * 0.54f), Size(s, s), CornerRadius(r, r))
        drawRoundRect(tint, Offset(w * 0.54f, h * 0.54f), Size(s, s), CornerRadius(r, r))
    }
}

@Composable
fun CutGlyph(modifier: Modifier = Modifier.size(24.dp), tint: Color = OnSurfaceVariant) {
    Glyph(modifier) {
        val w = size.width
        val h = size.height
        val sw = w * 0.085f
        line(w * 0.25f, h * 0.12f, w * 0.68f, h * 0.68f, tint, sw)
        line(w * 0.75f, h * 0.12f, w * 0.32f, h * 0.68f, tint, sw)
        drawCircle(tint, w * 0.11f, Offset(w * 0.28f, h * 0.83f), style = Stroke(sw))
        drawCircle(tint, w * 0.11f, Offset(w * 0.72f, h * 0.83f), style = Stroke(sw))
    }
}

@Composable
fun CopyGlyph(modifier: Modifier = Modifier.size(24.dp), tint: Color = OnSurfaceVariant) {
    Glyph(modifier) {
        val w = size.width
        val h = size.height
        val sw = w * 0.085f
        drawRoundRect(
            tint, Offset(w * 0.14f, h * 0.14f), Size(w * 0.52f, h * 0.52f),
            CornerRadius(w * 0.08f, w * 0.08f), style = Stroke(sw)
        )
        drawRoundRect(
            tint, Offset(w * 0.34f, h * 0.34f), Size(w * 0.52f, h * 0.52f),
            CornerRadius(w * 0.08f, w * 0.08f), style = Stroke(sw)
        )
    }
}

@Composable
fun PasteGlyph(modifier: Modifier = Modifier.size(24.dp), tint: Color = OnSurfaceVariant) {
    Glyph(modifier) {
        val w = size.width
        val h = size.height
        val sw = w * 0.085f
        drawRoundRect(
            tint, Offset(w * 0.18f, h * 0.18f), Size(w * 0.64f, h * 0.7f),
            CornerRadius(w * 0.09f, w * 0.09f), style = Stroke(sw)
        )
        drawRoundRect(tint, Offset(w * 0.36f, h * 0.1f), Size(w * 0.28f, h * 0.16f), CornerRadius(w * 0.04f, w * 0.04f))
    }
}

@Composable
fun DocGlyph(modifier: Modifier = Modifier.size(34.dp), tint: Color = Color(0xFF3A6EA5)) {
    Glyph(modifier) {
        val w = size.width
        val h = size.height
        val sw = w * 0.09f
        val p = Path().apply {
            moveTo(w * 0.2f, h * 0.1f)
            lineTo(w * 0.58f, h * 0.1f)
            lineTo(w * 0.8f, h * 0.32f)
            lineTo(w * 0.8f, h * 0.9f)
            lineTo(w * 0.2f, h * 0.9f)
            close()
        }
        drawPath(p, tint, style = Stroke(sw, join = StrokeJoin.Round))
        line(w * 0.58f, h * 0.1f, w * 0.58f, h * 0.32f, tint, sw)
        line(w * 0.58f, h * 0.32f, w * 0.8f, h * 0.32f, tint, sw)
        line(w * 0.32f, h * 0.52f, w * 0.68f, h * 0.52f, tint, sw * 0.85f)
        line(w * 0.32f, h * 0.68f, w * 0.68f, h * 0.68f, tint, sw * 0.85f)
    }
}

@Composable
fun GenericFileGlyph(modifier: Modifier = Modifier.size(34.dp), tint: Color = OnSurfaceVariant) {
    Glyph(modifier) {
        val w = size.width
        val h = size.height
        val sw = w * 0.09f
        val p = Path().apply {
            moveTo(w * 0.2f, h * 0.1f)
            lineTo(w * 0.58f, h * 0.1f)
            lineTo(w * 0.8f, h * 0.32f)
            lineTo(w * 0.8f, h * 0.9f)
            lineTo(w * 0.2f, h * 0.9f)
            close()
        }
        drawPath(p, tint, style = Stroke(sw, join = StrokeJoin.Round))
        line(w * 0.58f, h * 0.1f, w * 0.58f, h * 0.32f, tint, sw)
        line(w * 0.58f, h * 0.32f, w * 0.8f, h * 0.32f, tint, sw)
    }
}

@Composable
fun AudioGlyph(modifier: Modifier = Modifier.size(34.dp), tint: Color = Color(0xFF7B4EA8)) {
    Glyph(modifier) {
        val w = size.width
        val h = size.height
        val sw = w * 0.085f
        drawCircle(tint, w * 0.21f, Offset(w * 0.27f, h * 0.74f))
        line(w * 0.46f, h * 0.74f, w * 0.46f, h * 0.2f, tint, sw)
        val flag = Path().apply {
            moveTo(w * 0.46f, h * 0.2f)
            cubicTo(w * 0.72f, h * 0.26f, w * 0.78f, h * 0.36f, w * 0.74f, h * 0.54f)
            cubicTo(w * 0.72f, h * 0.4f, w * 0.62f, h * 0.34f, w * 0.46f, h * 0.33f)
            close()
        }
        drawPath(flag, tint)
    }
}

@Composable
fun ApkGlyph(modifier: Modifier = Modifier.size(40.dp)) {
    Glyph(modifier) {
        val w = size.width
        val h = size.height
        drawRoundRect(Color(0xFF2E7D32), Offset(w * 0.1f, h * 0.1f), Size(w * 0.8f, h * 0.8f), CornerRadius(w * 0.22f, w * 0.22f))
        val ic = Color(0xFFB9F6CA)
        drawCircle(ic, w * 0.09f, Offset(w * 0.36f, h * 0.42f))
        drawCircle(ic, w * 0.09f, Offset(w * 0.64f, h * 0.42f))
        line(w * 0.26f, h * 0.24f, w * 0.36f, h * 0.16f, ic, w * 0.06f)
        line(w * 0.74f, h * 0.24f, w * 0.64f, h * 0.16f, ic, w * 0.06f)
        drawRoundRect(ic, Offset(w * 0.24f, h * 0.56f), Size(w * 0.52f, h * 0.2f), CornerRadius(w * 0.08f, w * 0.08f))
    }
}

@Composable
fun ZipGlyph(modifier: Modifier = Modifier.size(48.dp)) {
    Glyph(modifier) {
        val w = size.width
        val h = size.height
        drawRoundRect(Color(0xFFF1F1F4), Offset(0f, 0f), Size(w, h), CornerRadius(w * 0.16f, w * 0.16f))
        drawRoundRect(Color(0xFF5F6368), Offset(w * 0.2f, h * 0.12f), Size(w * 0.24f, h * 0.76f), CornerRadius(w * 0.04f, w * 0.04f))
        var y = h * 0.17f
        val tooth = w * 0.1f
        while (y < h * 0.8f) {
            drawRoundRect(
                Color(0xFFF1F1F4),
                Offset(w * 0.2f + (w * 0.24f - tooth) / 2f, y),
                Size(tooth, tooth * 0.7f),
                CornerRadius(tooth * 0.2f, tooth * 0.2f)
            )
            y += tooth * 1.5f
        }
        drawRoundRect(Color(0xFF8A8D93), Offset(w * 0.52f, h * 0.16f), Size(w * 0.3f, h * 0.14f), CornerRadius(w * 0.03f, w * 0.03f))
        drawRoundRect(Color(0xFFB9BCC2), Offset(w * 0.52f, h * 0.42f), Size(w * 0.3f, h * 0.14f), CornerRadius(w * 0.03f, w * 0.03f))
        drawRoundRect(Color(0xFF8A8D93), Offset(w * 0.52f, h * 0.68f), Size(w * 0.3f, h * 0.14f), CornerRadius(w * 0.03f, w * 0.03f))
    }
}

@Composable
fun ImageThumb(modifier: Modifier = Modifier.size(48.dp)) {
    Glyph(modifier) {
        val w = size.width
        val h = size.height
        val radius = w * 0.16f
        val path = Path().apply {
            addRoundRect(androidx.compose.ui.geometry.RoundRect(Rect(0f, 0f, w, h), CornerRadius(radius, radius)))
        }
        clipPath(path) {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF6E8F5E), Color(0xFF3F5B36))), size = Size(w, h))
            val stripe = Color(0x22FFFFFF)
            var x = w * 0.1f
            while (x < w) {
                drawRect(stripe, Offset(x, 0f), Size(w * 0.05f, h))
                x += w * 0.16f
            }
            drawCircle(Color(0xFFE8E0CC), w * 0.2f, Offset(w * 0.66f, h * 0.3f))
        }
    }
}

@Composable
fun VideoThumb(modifier: Modifier = Modifier.size(48.dp)) {
    Glyph(modifier) {
        val w = size.width
        val h = size.height
        drawRoundRect(Color(0xFF4A3B57), Offset(0f, 0f), Size(w, h), CornerRadius(w * 0.16f, w * 0.16f))
        drawRoundRect(Color(0xFF8FBDB2), Offset(w * 0.14f, h * 0.2f), Size(w * 0.3f, h * 0.24f), CornerRadius(w * 0.05f, w * 0.05f))
        drawCircle(Color.White, w * 0.16f, Offset(w * 0.72f, h * 0.32f), style = Stroke(w * 0.045f))
        drawRoundRect(Color(0x33FFFFFF), Offset(w * 0.14f, h * 0.72f), Size(w * 0.44f, h * 0.05f), CornerRadius(w * 0.02f, w * 0.02f))
    }
}

@Composable
fun ChevronGlyph(modifier: Modifier = Modifier.size(16.dp), tint: Color = OnSurfaceVariant) {
    Glyph(modifier) {
        val w = size.width
        val h = size.height
        val sw = w * 0.11f
        line(w * 0.36f, h * 0.22f, w * 0.66f, h * 0.5f, tint, sw)
        line(w * 0.66f, h * 0.5f, w * 0.36f, h * 0.78f, tint, sw)
    }
}

@Composable
fun CameraGlyph(modifier: Modifier = Modifier.size(24.dp), tint: Color = OnSurfaceVariant) {
    Glyph(modifier) {
        val w = size.width
        val h = size.height
        val sw = w * 0.075f
        drawRoundRect(tint, Offset(w * 0.08f, h * 0.26f), Size(w * 0.84f, h * 0.56f), CornerRadius(w * 0.1f, w * 0.1f), style = Stroke(sw))
        drawRoundRect(tint, Offset(w * 0.34f, h * 0.14f), Size(w * 0.32f, h * 0.14f), CornerRadius(w * 0.05f, w * 0.05f), style = Stroke(sw))
        drawCircle(tint, w * 0.16f, Offset(w * 0.5f, h * 0.55f), style = Stroke(sw))
    }
}

@Composable
fun DownloadGlyph(modifier: Modifier = Modifier.size(24.dp), tint: Color = OnSurfaceVariant) {
    Glyph(modifier) {
        val w = size.width
        val h = size.height
        val sw = w * 0.08f
        line(w * 0.5f, h * 0.14f, w * 0.5f, h * 0.6f, tint, sw)
        line(w * 0.3f, h * 0.42f, w * 0.5f, h * 0.62f, tint, sw)
        line(w * 0.7f, h * 0.42f, w * 0.5f, h * 0.62f, tint, sw)
        line(w * 0.2f, h * 0.82f, w * 0.8f, h * 0.82f, tint, sw)
    }
}

@Composable
fun MovieGlyph(modifier: Modifier = Modifier.size(24.dp), tint: Color = OnSurfaceVariant) {
    Glyph(modifier) {
        val w = size.width
        val h = size.height
        val sw = w * 0.075f
        drawRoundRect(tint, Offset(w * 0.1f, h * 0.28f), Size(w * 0.8f, h * 0.52f), CornerRadius(w * 0.08f, w * 0.08f), style = Stroke(sw))
        line(w * 0.1f, h * 0.46f, w * 0.9f, h * 0.46f, tint, sw)
        line(w * 0.28f, h * 0.28f, w * 0.4f, h * 0.14f, tint, sw)
        line(w * 0.52f, h * 0.28f, w * 0.64f, h * 0.14f, tint, sw)
    }
}

@Composable
fun MusicGlyph(modifier: Modifier = Modifier.size(24.dp), tint: Color = OnSurfaceVariant) {
    Glyph(modifier) {
        val w = size.width
        val h = size.height
        val sw = w * 0.075f
        line(w * 0.36f, h * 0.78f, w * 0.36f, h * 0.22f, tint, sw)
        line(w * 0.36f, h * 0.22f, w * 0.74f, h * 0.14f, tint, sw)
        line(w * 0.74f, h * 0.14f, w * 0.74f, h * 0.66f, tint, sw)
        drawCircle(tint, w * 0.13f, Offset(w * 0.23f, h * 0.78f))
        drawCircle(tint, w * 0.13f, Offset(w * 0.61f, h * 0.66f))
    }
}

@Composable
fun PictureGlyph(modifier: Modifier = Modifier.size(24.dp), tint: Color = OnSurfaceVariant) {
    Glyph(modifier) {
        val w = size.width
        val h = size.height
        val sw = w * 0.075f
        drawRoundRect(tint, Offset(w * 0.1f, h * 0.16f), Size(w * 0.8f, h * 0.68f), CornerRadius(w * 0.09f, w * 0.09f), style = Stroke(sw))
        drawCircle(tint, w * 0.09f, Offset(w * 0.35f, h * 0.38f), style = Stroke(sw))
        val p = Path().apply {
            moveTo(w * 0.16f, h * 0.78f)
            lineTo(w * 0.44f, h * 0.5f)
            lineTo(w * 0.6f, h * 0.66f)
            lineTo(w * 0.72f, h * 0.54f)
            lineTo(w * 0.86f, h * 0.78f)
        }
        drawPath(p, tint, style = Stroke(sw, join = StrokeJoin.Round))
    }
}

@Composable
fun StorageGlyph(modifier: Modifier = Modifier.size(24.dp), tint: Color = OnSurfaceVariant) {
    Glyph(modifier) {
        val w = size.width
        val h = size.height
        val sw = w * 0.075f
        val p = Path().apply {
            moveTo(w * 0.18f, h * 0.12f)
            lineTo(w * 0.66f, h * 0.12f)
            lineTo(w * 0.82f, h * 0.3f)
            lineTo(w * 0.82f, h * 0.88f)
            lineTo(w * 0.18f, h * 0.88f)
            close()
        }
        drawPath(p, tint, style = Stroke(sw, join = StrokeJoin.Round))
        line(w * 0.3f, h * 0.52f, w * 0.7f, h * 0.52f, tint, sw)
    }
}

@Composable
fun RootGlyph(modifier: Modifier = Modifier.size(24.dp), tint: Color = OnSurfaceVariant) {
    Glyph(modifier) {
        val w = size.width
        val h = size.height
        val sw = w * 0.075f
        drawRoundRect(tint, Offset(w * 0.24f, h * 0.1f), Size(w * 0.52f, h * 0.8f), CornerRadius(w * 0.12f, w * 0.12f), style = Stroke(sw))
        drawCircle(tint, w * 0.05f, Offset(w * 0.5f, h * 0.78f))
    }
}

@Composable
fun ServerGlyph(modifier: Modifier = Modifier.size(24.dp), tint: Color = OnSurfaceVariant) {
    Glyph(modifier) {
        val w = size.width
        val h = size.height
        val sw = w * 0.075f
        drawRoundRect(tint, Offset(w * 0.12f, h * 0.16f), Size(w * 0.76f, h * 0.28f), CornerRadius(w * 0.07f, w * 0.07f), style = Stroke(sw))
        drawRoundRect(tint, Offset(w * 0.12f, h * 0.56f), Size(w * 0.76f, h * 0.28f), CornerRadius(w * 0.07f, w * 0.07f), style = Stroke(sw))
        drawCircle(tint, w * 0.04f, Offset(w * 0.26f, h * 0.3f))
        drawCircle(tint, w * 0.04f, Offset(w * 0.26f, h * 0.7f))
    }
}

@Composable
fun ScreenshotGlyph(modifier: Modifier = Modifier.size(24.dp), tint: Color = OnSurfaceVariant) {
    Glyph(modifier) {
        val w = size.width
        val h = size.height
        val sw = w * 0.075f
        drawRoundRect(tint, Offset(w * 0.1f, h * 0.2f), Size(w * 0.8f, h * 0.62f), CornerRadius(w * 0.09f, w * 0.09f), style = Stroke(sw))
        line(w * 0.2f, h * 0.9f, w * 0.4f, h * 0.9f, tint, sw)
        line(w * 0.6f, h * 0.9f, w * 0.8f, h * 0.9f, tint, sw)
    }
}

@Composable
fun ArchiveGlyph(modifier: Modifier = Modifier.size(24.dp), tint: Color = OnSurfaceVariant) {
    Glyph(modifier) {
        val w = size.width
        val h = size.height
        val sw = w * 0.075f
        drawRoundRect(tint, Offset(w * 0.12f, h * 0.16f), Size(w * 0.76f, h * 0.68f), CornerRadius(w * 0.09f, w * 0.09f), style = Stroke(sw))
        drawRoundRect(tint, Offset(w * 0.42f, h * 0.16f), Size(w * 0.16f, h * 0.4f), CornerRadius(w * 0.03f, w * 0.03f))
    }
}

@Composable
fun FileTypeIcon(node: FsNode, modifier: Modifier = Modifier.size(48.dp)) {
    when (node.kind) {
        FileKind.DIR -> Box(modifier, contentAlignment = Alignment.Center) {
            FolderGlyph(Modifier.size(40.dp), OnSurfaceVariant)
        }
        FileKind.ARCHIVE -> ZipGlyph(modifier)
        FileKind.IMAGE -> ImageThumb(modifier)
        FileKind.VIDEO -> VideoThumb(modifier)
        FileKind.AUDIO -> Box(modifier, contentAlignment = Alignment.Center) {
            AudioGlyph(Modifier.size(34.dp), Color(0xFF7B4EA8))
        }
        FileKind.DOCUMENT -> Box(modifier, contentAlignment = Alignment.Center) {
            DocGlyph(Modifier.size(34.dp), Color(0xFF3A6EA5))
        }
        FileKind.APK -> Box(modifier, contentAlignment = Alignment.Center) {
            ApkGlyph(Modifier.size(40.dp))
        }
        FileKind.OTHER -> Box(modifier, contentAlignment = Alignment.Center) {
            GenericFileGlyph(Modifier.size(34.dp), OnSurfaceVariant)
        }
    }
}
