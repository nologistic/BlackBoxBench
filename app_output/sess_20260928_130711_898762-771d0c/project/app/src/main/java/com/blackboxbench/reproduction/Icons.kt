package com.blackboxbench.reproduction

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private fun filled(name: String, block: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).path(fill = SolidColor(Color.Black)) { block() }.build()

private fun stroked(name: String, width: Float = 1.9f, path: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = width,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ) { path() }.build()

object AppIcons {

    /** playlist_play */
    val Queue: ImageVector by lazy {
        filled(
            "queue",
            {
                moveTo(3f, 6f); horizontalLineTo(13f); verticalLineTo(8f); horizontalLineTo(3f); close()
                moveTo(3f, 10f); horizontalLineTo(13f); verticalLineTo(12f); horizontalLineTo(3f); close()
                moveTo(3f, 14f); horizontalLineTo(9f); verticalLineTo(16f); horizontalLineTo(3f); close()
                moveTo(15f, 13f); verticalLineTo(21f); lineTo(22f, 17f); close()
            }
        )
    }

    /** inbox (outlined) */
    val Inbox: ImageVector by lazy {
        filled(
            "inbox",
            {
                moveTo(19f, 3f)
                horizontalLineTo(5f)
                curveTo(3.9f, 3f, 3f, 3.9f, 3f, 5f)
                verticalLineTo(19f)
                curveTo(3f, 20.1f, 3.9f, 21f, 5f, 21f)
                horizontalLineTo(19f)
                curveTo(20.1f, 21f, 21f, 20.1f, 21f, 19f)
                verticalLineTo(5f)
                curveTo(21f, 3.9f, 20.1f, 3f, 19f, 3f)
                close()
                moveTo(19f, 15f)
                horizontalLineTo(15f)
                curveTo(15f, 16.66f, 13.65f, 18f, 12f, 18f)
                curveTo(10.35f, 18f, 9f, 16.66f, 9f, 15f)
                horizontalLineTo(5f)
                verticalLineTo(5f)
                horizontalLineTo(19f)
                close()
            }
        )
    }

    /** grid_view */
    val Grid: ImageVector by lazy {
        filled(
            "grid",
            {
                moveTo(3f, 3f); horizontalLineTo(11f); verticalLineTo(11f); horizontalLineTo(3f); close()
                moveTo(13f, 3f); horizontalLineTo(21f); verticalLineTo(11f); horizontalLineTo(13f); close()
                moveTo(3f, 13f); horizontalLineTo(11f); verticalLineTo(21f); horizontalLineTo(3f); close()
                moveTo(13f, 13f); horizontalLineTo(21f); verticalLineTo(21f); horizontalLineTo(13f); close()
            }
        )
    }

    /** rss_feed */
    val Rss: ImageVector by lazy {
        filled(
            "rss",
            {
                moveTo(6.18f, 15.64f)
                curveTo(7.38f, 15.64f, 8.36f, 16.62f, 8.36f, 17.82f)
                curveTo(8.36f, 19f, 7.38f, 20f, 6.18f, 20f)
                curveTo(5f, 20f, 4f, 19f, 4f, 17.82f)
                curveTo(4f, 16.62f, 5f, 15.64f, 6.18f, 15.64f)
                close()
                moveTo(4f, 4.44f)
                curveTo(12.59f, 4.44f, 19.56f, 11.41f, 19.56f, 20f)
                horizontalLineTo(16.73f)
                curveTo(16.73f, 12.97f, 11.03f, 7.27f, 4f, 7.27f)
                close()
                moveTo(4f, 10.1f)
                curveTo(9.46f, 10.1f, 13.9f, 14.54f, 13.9f, 20f)
                horizontalLineTo(11.07f)
                curveTo(11.07f, 16.1f, 7.9f, 12.93f, 4f, 12.93f)
                close()
            }
        )
    }

    /** folder (outlined) */
    val Folder: ImageVector by lazy {
        filled(
            "folder",
            {
                moveTo(20f, 6f)
                horizontalLineTo(12f)
                lineTo(10f, 4f)
                horizontalLineTo(4f)
                curveTo(2.9f, 4f, 2.01f, 4.9f, 2.01f, 6f)
                lineTo(2f, 18f)
                curveTo(2f, 19.1f, 2.9f, 20f, 4f, 20f)
                horizontalLineTo(20f)
                curveTo(21.1f, 20f, 22f, 19.1f, 22f, 18f)
                verticalLineTo(8f)
                curveTo(22f, 6.9f, 21.1f, 6f, 20f, 6f)
                close()
                moveTo(20f, 18f)
                horizontalLineTo(4f)
                verticalLineTo(8f)
                horizontalLineTo(20f)
                close()
            }
        )
    }

    /** file_download (outlined) */
    val Download: ImageVector by lazy {
        stroked(
            "download",
            path = {
                moveTo(12f, 3.5f); verticalLineTo(15f)
                moveTo(7f, 10.2f); lineTo(12f, 15.2f); lineTo(17f, 10.2f)
                moveTo(4.5f, 19.5f); horizontalLineTo(19.5f)
            }
        )
    }

    /** history */
    val History: ImageVector by lazy {
        filled(
            "history",
            {
                moveTo(13f, 3f)
                curveTo(8.03f, 3f, 4f, 7.03f, 4f, 12f)
                horizontalLineTo(1f)
                lineTo(4.89f, 15.89f)
                lineTo(4.96f, 16.03f)
                lineTo(9f, 12f)
                horizontalLineTo(6f)
                curveTo(6f, 8.13f, 9.13f, 5f, 13f, 5f)
                curveTo(16.87f, 5f, 20f, 8.13f, 20f, 12f)
                curveTo(20f, 15.87f, 16.87f, 19f, 13f, 19f)
                curveTo(11.07f, 19f, 9.32f, 18.21f, 8.06f, 16.94f)
                lineTo(6.64f, 18.36f)
                curveTo(8.27f, 19.99f, 10.51f, 21f, 13f, 21f)
                curveTo(17.97f, 21f, 22f, 16.97f, 22f, 12f)
                curveTo(22f, 7.03f, 17.97f, 3f, 13f, 3f)
                close()
                moveTo(12f, 8f)
                verticalLineTo(13f)
                lineTo(16.28f, 15.54f)
                lineTo(17f, 14.33f)
                lineTo(13.5f, 12.25f)
                verticalLineTo(8f)
                close()
            }
        )
    }

    /** bar_chart */
    val BarChart: ImageVector by lazy {
        filled(
            "bar_chart",
            {
                moveTo(5f, 9.2f); horizontalLineTo(8f); verticalLineTo(19f); horizontalLineTo(5f); close()
                moveTo(10.6f, 5f); horizontalLineTo(13.4f); verticalLineTo(19f); horizontalLineTo(10.6f); close()
                moveTo(16.2f, 13f); horizontalLineTo(19f); verticalLineTo(19f); horizontalLineTo(16.2f); close()
            }
        )
    }

    /** smartphone */
    val UiTheme: ImageVector by lazy {
        filled(
            "ui_theme",
            {
                moveTo(17f, 1.01f)
                lineTo(7f, 1f)
                curveTo(5.9f, 1f, 5f, 1.9f, 5f, 3f)
                verticalLineTo(21f)
                curveTo(5f, 22.1f, 5.9f, 23f, 7f, 23f)
                horizontalLineTo(10f)
                curveTo(11.1f, 23f, 12f, 22.1f, 12f, 21f)
                verticalLineTo(3f)
                curveTo(12f, 1.9f, 11.1f, 1.01f, 10f, 1.01f)
                close()
                moveTo(17f, 19f)
                horizontalLineTo(7f)
                verticalLineTo(5f)
                horizontalLineTo(17f)
                close()
            }
        )
    }

    /** cloud (outlined) */
    val Cloud: ImageVector by lazy {
        filled(
            "cloud",
            {
                moveTo(19.35f, 10.04f)
                curveTo(18.67f, 6.59f, 15.64f, 4f, 12f, 4f)
                curveTo(9.11f, 4f, 6.6f, 5.64f, 5.35f, 8.04f)
                curveTo(2.34f, 8.36f, 0f, 10.91f, 0f, 14f)
                curveTo(0f, 17.31f, 2.69f, 20f, 6f, 20f)
                horizontalLineTo(19f)
                curveTo(21.76f, 20f, 24f, 17.76f, 24f, 15f)
                curveTo(24f, 12.36f, 21.95f, 10.22f, 19.35f, 10.04f)
                close()
                moveTo(19f, 18f)
                horizontalLineTo(6f)
                curveTo(3.79f, 18f, 2f, 16.21f, 2f, 14f)
                curveTo(2f, 11.95f, 3.53f, 10.24f, 5.56f, 10.03f)
                lineTo(6.63f, 9.92f)
                lineTo(7.13f, 8.97f)
                curveTo(8.08f, 7.14f, 9.94f, 6f, 12f, 6f)
                curveTo(14.62f, 6f, 16.88f, 7.86f, 17.39f, 10.43f)
                lineTo(17.69f, 11.93f)
                lineTo(19.22f, 12.04f)
                curveTo(20.78f, 12.15f, 22f, 13.45f, 22f, 15f)
                curveTo(22f, 16.65f, 20.65f, 18f, 19f, 18f)
                close()
            }
        )
    }

    /** save (outlined) */
    val Backup: ImageVector by lazy {
        filled(
            "backup",
            {
                moveTo(17f, 3f)
                horizontalLineTo(5f)
                curveTo(3.9f, 3f, 3f, 3.9f, 3f, 5f)
                verticalLineTo(19f)
                curveTo(3f, 20.1f, 3.9f, 21f, 5f, 21f)
                horizontalLineTo(19f)
                curveTo(20.1f, 21f, 21f, 20.1f, 21f, 19f)
                verticalLineTo(7f)
                lineTo(17f, 3f)
                close()
                moveTo(19f, 19f)
                horizontalLineTo(5f)
                verticalLineTo(5f)
                horizontalLineTo(16.17f)
                lineTo(19f, 7.83f)
                close()
                moveTo(12f, 12f)
                curveTo(10.34f, 12f, 9f, 13.34f, 9f, 15f)
                curveTo(9f, 16.66f, 10.34f, 18f, 12f, 18f)
                curveTo(13.66f, 18f, 15f, 16.66f, 15f, 15f)
                curveTo(15f, 13.34f, 13.66f, 12f, 12f, 12f)
                close()
                moveTo(6f, 6f)
                horizontalLineTo(15f)
                verticalLineTo(10f)
                horizontalLineTo(6f)
                close()
            }
        )
    }

    /** bug_report (outlined) */
    val Bug: ImageVector by lazy {
        filled(
            "bug",
            {
                moveTo(20f, 8f)
                horizontalLineTo(17.19f)
                curveTo(16.76f, 7.16f, 16.12f, 6.47f, 15.37f, 6.04f)
                lineTo(17f, 4.41f)
                lineTo(15.59f, 3f)
                lineTo(13.42f, 5.17f)
                curveTo(12.96f, 5.06f, 12.49f, 5f, 12f, 5f)
                curveTo(11.51f, 5f, 11.04f, 5.06f, 10.59f, 5.17f)
                lineTo(8.41f, 3f)
                lineTo(7f, 4.41f)
                lineTo(8.62f, 6.04f)
                curveTo(7.87f, 6.47f, 7.23f, 7.16f, 6.81f, 8f)
                horizontalLineTo(4f)
                verticalLineTo(10f)
                horizontalLineTo(6.09f)
                curveTo(6.04f, 10.33f, 6f, 10.66f, 6f, 11f)
                verticalLineTo(12f)
                horizontalLineTo(4f)
                verticalLineTo(14f)
                horizontalLineTo(6f)
                verticalLineTo(15f)
                curveTo(6f, 15.34f, 6.04f, 15.67f, 6.09f, 16f)
                horizontalLineTo(4f)
                verticalLineTo(18f)
                horizontalLineTo(6.81f)
                curveTo(7.84f, 19.81f, 9.78f, 21f, 12f, 21f)
                curveTo(14.22f, 21f, 16.16f, 19.81f, 17.19f, 18f)
                horizontalLineTo(20f)
                verticalLineTo(16f)
                horizontalLineTo(17.91f)
                curveTo(17.96f, 15.67f, 18f, 15.34f, 18f, 15f)
                verticalLineTo(14f)
                horizontalLineTo(20f)
                verticalLineTo(12f)
                horizontalLineTo(18f)
                verticalLineTo(11f)
                curveTo(18f, 10.66f, 17.96f, 10.33f, 17.91f, 10f)
                horizontalLineTo(20f)
                close()
                moveTo(16f, 15f)
                curveTo(16f, 17.21f, 14.21f, 19f, 12f, 19f)
                curveTo(9.79f, 19f, 8f, 17.21f, 8f, 15f)
                verticalLineTo(11f)
                curveTo(8f, 8.79f, 9.79f, 7f, 12f, 7f)
                curveTo(14.21f, 7f, 16f, 8.79f, 16f, 11f)
                close()
                moveTo(10f, 13f)
                horizontalLineTo(14f)
                verticalLineTo(15f)
                horizontalLineTo(10f)
                close()
                moveTo(10f, 9f)
                horizontalLineTo(14f)
                verticalLineTo(11f)
                horizontalLineTo(10f)
                close()
            }
        )
    }

    /** forum (outlined) */
    val Forum: ImageVector by lazy {
        filled(
            "forum",
            {
                moveTo(15f, 4f)
                verticalLineTo(11f)
                horizontalLineTo(5.17f)
                lineTo(4f, 12.17f)
                verticalLineTo(4f)
                horizontalLineTo(15f)
                moveTo(16f, 2f)
                horizontalLineTo(3f)
                curveTo(2.45f, 2f, 2f, 2.45f, 2f, 3f)
                verticalLineTo(17f)
                lineTo(6f, 13f)
                horizontalLineTo(16f)
                curveTo(16.55f, 13f, 17f, 12.55f, 17f, 12f)
                verticalLineTo(3f)
                curveTo(17f, 2.45f, 16.55f, 2f, 16f, 2f)
                close()
                moveTo(21f, 6f)
                horizontalLineTo(19f)
                verticalLineTo(15f)
                horizontalLineTo(6f)
                verticalLineTo(17f)
                curveTo(6f, 17.55f, 6.45f, 18f, 7f, 18f)
                horizontalLineTo(18f)
                lineTo(22f, 22f)
                verticalLineTo(7f)
                curveTo(22f, 6.45f, 21.55f, 6f, 21f, 6f)
                close()
            }
        )
    }

    /** help_outline */
    val Help: ImageVector by lazy {
        filled(
            "help",
            {
                moveTo(11f, 18f)
                horizontalLineTo(13f)
                verticalLineTo(16f)
                horizontalLineTo(11f)
                close()
                moveTo(12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
                curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
                curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
                close()
                moveTo(12f, 20f)
                curveTo(7.59f, 20f, 4f, 16.41f, 4f, 12f)
                curveTo(4f, 7.59f, 7.59f, 4f, 12f, 4f)
                curveTo(16.41f, 4f, 20f, 7.59f, 20f, 12f)
                curveTo(20f, 16.41f, 16.41f, 20f, 12f, 20f)
                close()
                moveTo(12f, 6f)
                curveTo(9.79f, 6f, 8f, 7.79f, 8f, 10f)
                horizontalLineTo(10f)
                curveTo(10f, 8.9f, 10.9f, 8f, 12f, 8f)
                curveTo(13.1f, 8f, 14f, 8.9f, 14f, 10f)
                curveTo(14f, 12f, 11f, 11.75f, 11f, 15f)
                horizontalLineTo(13f)
                curveTo(13f, 12.75f, 16f, 12.5f, 16f, 10f)
                curveTo(16f, 7.79f, 14.21f, 6f, 12f, 6f)
                close()
            }
        )
    }

    /** sort arrows */
    val Sort: ImageVector by lazy {
        stroked(
            "sort",
            path = {
                moveTo(7f, 4f); verticalLineTo(20f)
                moveTo(3.5f, 7.5f); lineTo(7f, 4f); lineTo(10.5f, 7.5f)
                moveTo(17f, 20f); verticalLineTo(4f)
                moveTo(13.5f, 16.5f); lineTo(17f, 20f); lineTo(20.5f, 16.5f)
            }
        )
    }

    /** filter_alt (outlined) */
    val Filter: ImageVector by lazy {
        filled(
            "filter",
            {
                moveTo(7f, 6f)
                horizontalLineTo(17f)
                lineTo(11.99f, 12.3f)
                close()
                moveTo(4.25f, 5.61f)
                curveTo(6.27f, 8.2f, 10f, 13f, 10f, 13f)
                verticalLineTo(19f)
                curveTo(10f, 19.55f, 10.45f, 20f, 11f, 20f)
                horizontalLineTo(13f)
                curveTo(13.55f, 20f, 14f, 19.55f, 14f, 19f)
                verticalLineTo(13f)
                curveTo(14f, 13f, 17.72f, 8.2f, 19.74f, 5.61f)
                curveTo(20.25f, 4.95f, 19.78f, 4f, 18.95f, 4f)
                horizontalLineTo(5.04f)
                curveTo(4.21f, 4f, 3.74f, 4.95f, 4.25f, 5.61f)
                close()
            }
        )
    }

    /** pause */
    val Pause: ImageVector by lazy {
        filled(
            "pause",
            {
                moveTo(6f, 19f); horizontalLineTo(10f); verticalLineTo(5f); horizontalLineTo(6f); close()
                moveTo(14f, 5f); verticalLineTo(19f); horizontalLineTo(18f); verticalLineTo(5f); close()
            }
        )
    }

    /** skip next / forward 30 */
    val Forward: ImageVector by lazy {
        filled(
            "forward",
            {
                moveTo(4f, 18f); verticalLineTo(6f); lineTo(14f, 12f); close()
                moveTo(14f, 6f); horizontalLineTo(16f); verticalLineTo(18f); horizontalLineTo(14f); close()
            }
        )
    }

    val Rewind: ImageVector by lazy {
        filled(
            "rewind",
            {
                moveTo(20f, 6f); verticalLineTo(18f); lineTo(10f, 12f); close()
                moveTo(10f, 6f); horizontalLineTo(8f); verticalLineTo(18f); horizontalLineTo(10f); close()
            }
        )
    }

    /** drag handle */
    val DragHandle: ImageVector by lazy {
        filled(
            "drag",
            {
                moveTo(4f, 8f); horizontalLineTo(20f); verticalLineTo(9.6f); horizontalLineTo(4f); close()
                moveTo(4f, 11.2f); horizontalLineTo(20f); verticalLineTo(12.8f); horizontalLineTo(4f); close()
                moveTo(4f, 14.4f); horizontalLineTo(20f); verticalLineTo(16f); horizontalLineTo(4f); close()
            }
        )
    }

    /** smartphone / paint for UI */
    val Palette: ImageVector by lazy {
        filled(
            "palette",
            {
                moveTo(12f, 3f)
                curveTo(7.03f, 3f, 3f, 7.03f, 3f, 12f)
                curveTo(3f, 16.97f, 7.03f, 21f, 12f, 21f)
                curveTo(12.83f, 21f, 13.5f, 20.33f, 13.5f, 19.5f)
                curveTo(13.5f, 19.11f, 13.35f, 18.76f, 13.11f, 18.5f)
                curveTo(12.88f, 18.24f, 12.74f, 17.9f, 12.74f, 17.53f)
                curveTo(12.74f, 16.7f, 13.41f, 16.03f, 14.24f, 16.03f)
                horizontalLineTo(16f)
                curveTo(18.76f, 16.03f, 21f, 13.79f, 21f, 11.03f)
                curveTo(21f, 6.6f, 16.97f, 3f, 12f, 3f)
                close()
                moveTo(6.5f, 11.5f)
                curveTo(5.67f, 11.5f, 5f, 10.83f, 5f, 10f)
                curveTo(5f, 9.17f, 5.67f, 8.5f, 6.5f, 8.5f)
                curveTo(7.33f, 8.5f, 8f, 9.17f, 8f, 10f)
                curveTo(8f, 10.83f, 7.33f, 11.5f, 6.5f, 11.5f)
                close()
                moveTo(9.5f, 8f)
                curveTo(8.67f, 8f, 8f, 7.33f, 8f, 6.5f)
                curveTo(8f, 5.67f, 8.67f, 5f, 9.5f, 5f)
                curveTo(10.33f, 5f, 11f, 5.67f, 11f, 6.5f)
                curveTo(11f, 7.33f, 10.33f, 8f, 9.5f, 8f)
                close()
                moveTo(14.5f, 8f)
                curveTo(13.67f, 8f, 13f, 7.33f, 13f, 6.5f)
                curveTo(13f, 5.67f, 13.67f, 5f, 14.5f, 5f)
                curveTo(15.33f, 5f, 16f, 5.67f, 16f, 6.5f)
                curveTo(16f, 7.33f, 15.33f, 8f, 14.5f, 8f)
                close()
                moveTo(17.5f, 11.5f)
                curveTo(16.67f, 11.5f, 16f, 10.83f, 16f, 10f)
                curveTo(16f, 9.17f, 16.67f, 8.5f, 17.5f, 8.5f)
                curveTo(18.33f, 8.5f, 19f, 9.17f, 19f, 10f)
                curveTo(19f, 10.83f, 18.33f, 11.5f, 17.5f, 11.5f)
                close()
            }
        )
    }

    /** queue music for more menu "episodes" */
    val Episodes: ImageVector by lazy {
        filled(
            "episodes",
            {
                moveTo(3f, 6f); horizontalLineTo(13f); verticalLineTo(8f); horizontalLineTo(3f); close()
                moveTo(3f, 10f); horizontalLineTo(13f); verticalLineTo(12f); horizontalLineTo(3f); close()
                moveTo(3f, 14f); horizontalLineTo(9f); verticalLineTo(16f); horizontalLineTo(3f); close()
                moveTo(15f, 13f); verticalLineTo(21f); lineTo(22f, 17f); close()
            }
        )
    }
}
