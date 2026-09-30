package com.blackboxbench.reproduction

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AppDrawerContent(
    model: AppModel,
    onOpenFolder: (FsNode) -> Unit,
    onScreen: (Screen) -> Unit,
) {
    Column(
        Modifier
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(bottom = 12.dp)
    ) {
        Spacer(Modifier.height(40.dp))
        DrawerEntry(
            icon = { RootGlyph(Modifier.size(24.dp), OnSurfaceVariant) },
            title = "根目录",
            subtitle = "共 ${formatSize(SampleTree.ROOT_TOTAL)}，剩余 ${formatSize(SampleTree.ROOT_FREE)}",
            selected = false,
        ) { model.showRootError() }
        DrawerEntry(
            icon = { StorageGlyph(Modifier.size(24.dp), OnSurfaceVariant) },
            title = model.root.name,
            subtitle = "共 ${formatSize(SampleTree.STORAGE_TOTAL)}，剩余 ${formatSize(SampleTree.STORAGE_FREE)}",
            selected = model.current === model.root,
        ) { onOpenFolder(model.root) }
        DrawerEntry(
            icon = { Icon(Icons.Filled.Add, null, tint = OnSurfaceVariant, modifier = Modifier.size(22.dp)) },
            title = "添加存储空间…",
            subtitle = null,
            selected = false,
        ) { }

        DrawerDivider()

        StandardFolder(model, "相册", "DCIM") { CameraGlyph(Modifier.size(24.dp), OnSurfaceVariant) }
        StandardFolder(model, "下载", "Download") { DownloadGlyph(Modifier.size(24.dp), OnSurfaceVariant) }
        StandardFolder(model, "电影", "Movies") { MovieGlyph(Modifier.size(24.dp), OnSurfaceVariant) }
        StandardFolder(model, "音乐", "Music") { MusicGlyph(Modifier.size(24.dp), OnSurfaceVariant) }
        StandardFolder(model, "图片", "Pictures") { PictureGlyph(Modifier.size(24.dp), OnSurfaceVariant) }

        DrawerDivider()

        StandardFolder(model, "屏幕截图", "Pictures/Screenshots") {
            ScreenshotGlyph(Modifier.size(24.dp), OnSurfaceVariant)
        }

        for (bookmark in model.bookmarks) {
            DrawerEntry(
                icon = { FolderGlyph(Modifier.size(24.dp), OnSurfaceVariant) },
                title = bookmark.label,
                subtitle = null,
                selected = bookmark.target != null && model.current === bookmark.target,
            ) { bookmark.target?.let(onOpenFolder) }
        }

        DrawerDivider()

        DrawerEntry(
            icon = { ServerGlyph(Modifier.size(24.dp), OnSurfaceVariant) },
            title = "FTP 服务器",
            subtitle = null,
            selected = model.screen == Screen.FTP,
        ) { onScreen(Screen.FTP) }
        DrawerEntry(
            icon = { Icon(Icons.Filled.Settings, null, tint = OnSurfaceVariant, modifier = Modifier.size(22.dp)) },
            title = "设置",
            subtitle = null,
            selected = model.screen == Screen.SETTINGS,
        ) { onScreen(Screen.SETTINGS) }
        DrawerEntry(
            icon = { InfoCircleGlyph(Modifier.size(24.dp), OnSurfaceVariant) },
            title = "关于",
            subtitle = null,
            selected = model.screen == Screen.ABOUT,
        ) { onScreen(Screen.ABOUT) }
    }
}

@Composable
private fun StandardFolder(
    model: AppModel,
    label: String,
    relativePath: String,
    icon: @Composable () -> Unit,
) {
    var target: FsNode? = model.root
    for (segment in relativePath.split('/')) {
        target = target?.child(segment)
        if (target == null) break
    }
    val node = target
    DrawerEntry(
        icon = icon,
        title = label,
        subtitle = null,
        selected = node != null && model.current === node,
    ) {
        if (node != null) model.goTo(node) else model.openRelative(relativePath)
    }
}

@Composable
fun InfoCircleGlyph(modifier: Modifier = Modifier.size(24.dp), tint: androidx.compose.ui.graphics.Color = OnSurfaceVariant) {
    Glyph(modifier) {
        val w = size.width
        val h = size.height
        val sw = w * 0.08f
        drawCircle(tint, w * 0.42f, center, style = androidx.compose.ui.graphics.drawscope.Stroke(sw))
        drawLine(tint, androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.46f), androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.7f), strokeWidth = sw, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(tint, androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.3f), androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.33f), strokeWidth = sw, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
}
