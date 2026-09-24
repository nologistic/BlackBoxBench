package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(store: GalleryStore, onBack: () -> Unit, onOpenAbout: () -> Unit) {
    val context = LocalContext.current
    var dialog by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().background(BarTint).padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = Color(0xFF26243B))
            }
            Text("设置", fontSize = 19.sp, color = Color(0xFF26243B), fontWeight = FontWeight.SemiBold)
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            SectionLabel("外观")
            SettingRow(title = "自定义外观", subtitle = "跟随系统主题")

            SectionLabel("常规")
            SettingRow(title = "语言", subtitle = store.language, onClick = { dialog = "language" })
            SettingRow(title = "更改日期和时间格式", onClick = { toast(context, "已使用系统日期格式") })
            SettingRow(title = "文件加载优先事项", subtitle = store.fileLoadingPriority, onClick = { dialog = "priority" })
            SettingRow(title = "管理包含的文件夹", onClick = { toast(context, "包含全部文件夹") })
            SettingRow(title = "管理排除的文件夹", onClick = { toast(context, "没有排除的文件夹") })
            SettingRow(
                title = "显示隐藏项目",
                trailing = {
                    Switch(checked = store.showHidden, onCheckedChange = { store.showHidden = it; store.save() })
                }
            )
            SettingRow(
                title = "搜索所有文件",
                trailing = {
                    Switch(checked = store.searchAllFiles, onCheckedChange = { store.searchAllFiles = it; store.save() })
                }
            )

            SectionLabel("视频")
            SettingRow(title = "自动播放视频", trailing = {
                Switch(checked = store.autoplayVideo, onCheckedChange = { store.autoplayVideo = it; store.save() })
            })
            SettingRow(title = "记住位置", trailing = {
                Switch(checked = store.rememberPosition, onCheckedChange = { store.rememberPosition = it; store.save() })
            })
            SettingRow(title = "循环播放视频", trailing = {
                Switch(checked = store.loopVideo, onCheckedChange = { store.loopVideo = it; store.save() })
            })
            SettingRow(title = "横向手势", trailing = {
                Switch(checked = true, onCheckedChange = { })
            })
            SettingRow(title = "纵向滑动控制音量亮度", trailing = {
                Switch(checked = store.volumeBrightnessGesture, onCheckedChange = { store.volumeBrightnessGesture = it; store.save() })
            })
            SettingRow(title = "点按视频时", subtitle = store.videoTapAction, onClick = { dialog = "videotap" })

            SectionLabel("缩略图")
            SettingRow(title = "裁剪缩略图为正方形", trailing = {
                Switch(checked = store.cropSquare, onCheckedChange = { store.cropSquare = it; store.save() })
            })
            SettingRow(title = "GIF 动画", trailing = {
                Switch(checked = store.animateGif, onCheckedChange = { store.animateGif = it; store.save() })
            })
            SettingRow(title = "文件缩略图样式", subtitle = store.thumbStyle, onClick = { dialog = "thumbstyle" })
            SettingRow(title = "文件夹缩略图样式", subtitle = store.folderThumbStyle, onClick = { dialog = "folderstyle" })

            SectionLabel("滚动")
            SettingRow(title = "缩略图水平滚动", trailing = {
                Switch(checked = store.horizontalThumbScroll, onCheckedChange = { store.horizontalThumbScroll = it; store.save() })
            })
            SettingRow(title = "应用顶部下拉刷新", trailing = {
                Switch(checked = store.pullToRefresh, onCheckedChange = { store.pullToRefresh = it; store.save() })
            })

            SectionLabel("全屏显示")
            SettingRow(title = "全屏时使用黑色背景", trailing = {
                Switch(checked = store.blackFullscreen, onCheckedChange = { store.blackFullscreen = it; store.save() })
            })
            SettingRow(title = "以 HDR 模式显示", trailing = {
                Switch(checked = store.hdrDisplay, onCheckedChange = { store.hdrDisplay = it; store.save() })
            })
            SettingRow(title = "使用下滑手势退出全屏", trailing = {
                Switch(checked = store.swipeDownExit, onCheckedChange = { store.swipeDownExit = it; store.save() })
            })
            SettingRow(title = "显示刘海", trailing = {
                Switch(checked = store.showNotch, onCheckedChange = { store.showNotch = it; store.save() })
            })
            SettingRow(title = "旋转方向", subtitle = "跟随系统设置", onClick = { dialog = "rotation" })

            SectionLabel("大幅度缩放图像")
            SettingRow(title = "允许大幅度缩放", trailing = {
                Switch(checked = store.allowDeepZoom, onCheckedChange = { store.allowDeepZoom = it; store.save() })
            })
            SettingRow(title = "手势旋转", trailing = {
                Switch(checked = store.gestureRotation, onCheckedChange = { store.gestureRotation = it; store.save() })
            })
            SettingRow(title = "最高画质", trailing = {
                Switch(checked = store.maxQuality, onCheckedChange = { store.maxQuality = it; store.save() })
            })
            SettingRow(title = "两指双击 1:1", trailing = {
                Switch(checked = store.doubleTapZoom, onCheckedChange = { store.doubleTapZoom = it; store.save() })
            })

            SectionLabel("更多详细信息")
            SettingRow(title = "显示文件详情", subtitle = "名称、路径、大小、分辨率、修改日期")

            SectionLabel("安全性")
            SettingRow(title = "用密码保护整个应用", trailing = {
                Switch(checked = store.appLock, onCheckedChange = { store.appLock = it; store.save() })
            })
            SettingRow(title = "隐藏项目", trailing = {
                Switch(checked = store.lockHidden, onCheckedChange = { store.lockHidden = it; store.save() })
            })
            SettingRow(title = "文件的删除和移动", trailing = {
                Switch(checked = store.lockDelete, onCheckedChange = { store.lockDelete = it; store.save() })
            })

            SectionLabel("文件操作")
            SettingRow(title = "删除空文件夹", trailing = {
                Switch(checked = store.deleteEmptyFolder, onCheckedChange = { store.deleteEmptyFolder = it; store.save() })
            })
            SettingRow(title = "保留修改日期", trailing = {
                Switch(checked = store.keepModifiedDate, onCheckedChange = { store.keepModifiedDate = it; store.save() })
            })
            SettingRow(title = "不再显示删除确认", trailing = {
                Switch(checked = store.skipDeleteConfirm, onCheckedChange = { store.skipDeleteConfirm = it; store.save() })
            })

            SectionLabel("底部按钮")
            SettingRow(title = "显示底部按钮", trailing = {
                Switch(checked = store.bottomButtons, onCheckedChange = { store.bottomButtons = it; store.save() })
            })
            SettingRow(title = "管理底部按钮", subtitle = "收藏、编辑、分享、删除", onClick = { toast(context, "底部按钮已重置") })

            SectionLabel("回收站")
            SettingRow(title = "将已删除项目移至回收站", trailing = {
                Switch(checked = store.recycleEnabled, onCheckedChange = { store.recycleEnabled = it; store.save() })
            })
            SettingRow(title = "在文件夹界面显示回收站", trailing = {
                Switch(checked = store.binInFolders, onCheckedChange = { store.binInFolders = it; store.save() })
            })
            SettingRow(title = "在主屏幕末尾显示回收站", trailing = {
                Switch(checked = store.binAtEnd, onCheckedChange = { store.binAtEnd = it; store.save() })
            })
            SettingRow(
                title = "清空回收站",
                subtitle = if (store.binItems().isEmpty()) "0 B" else formatSize(store.binItems().sumOf { it.size }),
                onClick = {
                    store.emptyBin()
                    toast(context, "回收站已清空")
                }
            )

            SectionLabel("迁移")
            SettingRow(title = "清除缓存", subtitle = store.cacheSize, onClick = {
                store.clearCache()
                toast(context, "缓存已清除")
            })
            SettingRow(title = "导出收藏", onClick = { toast(context, "已导出收藏") })
            SettingRow(title = "导入收藏", onClick = { toast(context, "已导入收藏") })
            SettingRow(title = "导出设置", onClick = { toast(context, "已导出设置") })
            SettingRow(title = "导入设置", onClick = { toast(context, "已导入设置") })

            SettingRow(title = "关于", onClick = onOpenAbout)
        }
    }

    when (dialog) {
        "language" -> RadioDialog(
            title = "语言",
            options = listOf("中文", "English", "跟随系统"),
            selected = store.language,
            onConfirm = { value, _ -> store.language = value; store.save(); dialog = "" },
            onDismiss = { dialog = "" }
        )

        "priority" -> RadioDialog(
            title = "文件加载优先事项",
            options = listOf("速度", "质量"),
            selected = store.fileLoadingPriority,
            onConfirm = { value, _ -> store.fileLoadingPriority = value; store.save(); dialog = "" },
            onDismiss = { dialog = "" }
        )

        "videotap" -> RadioDialog(
            title = "点按视频时",
            options = listOf("打开应用内播放器", "使用其他应用打开"),
            selected = store.videoTapAction,
            onConfirm = { value, _ -> store.videoTapAction = value; store.save(); dialog = "" },
            onDismiss = { dialog = "" }
        )

        "thumbstyle" -> RadioDialog(
            title = "文件缩略图样式",
            options = listOf("方形", "圆角", "圆形"),
            selected = store.thumbStyle,
            onConfirm = { value, _ -> store.thumbStyle = value; store.save(); dialog = "" },
            onDismiss = { dialog = "" }
        )

        "folderstyle" -> RadioDialog(
            title = "文件夹缩略图样式",
            options = listOf("方形", "圆角", "圆形"),
            selected = store.folderThumbStyle,
            onConfirm = { value, _ -> store.folderThumbStyle = value; store.save(); dialog = "" },
            onDismiss = { dialog = "" }
        )

        "rotation" -> RadioDialog(
            title = "旋转方向",
            options = listOf("跟随系统设置", "横向", "纵向"),
            selected = "跟随系统设置",
            onConfirm = { _, _ -> dialog = "" },
            onDismiss = { dialog = "" }
        )

        else -> if (dialog.isNotEmpty()) dialog = ""
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().background(BarTint).padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = Color(0xFF26243B))
            }
            Text("关于", fontSize = 19.sp, color = Color(0xFF26243B))
        }
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            SectionLabel("支持")
            SettingRow(title = "常见问题", trailing = { Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(20.dp)) }, onClick = { toast(context, "常见问题") })
            SettingRow(title = "已知问题", trailing = { Icon(Icons.Filled.List, contentDescription = null, modifier = Modifier.size(20.dp)) }, onClick = { toast(context, "已知问题") })
            SettingRow(title = "hello@fossify.org", trailing = { Icon(Icons.Filled.Email, contentDescription = null, modifier = Modifier.size(20.dp)) }, onClick = { toast(context, "hello@fossify.org") })

            SectionLabel("帮助我们")
            SettingRow(title = "分享给好友", trailing = { Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(20.dp)) }, onClick = { toast(context, "分享给好友") })
            SettingRow(title = "贡献者", trailing = { Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(20.dp)) }, onClick = { toast(context, "贡献者") })
            SettingRow(title = "向 Fossify 捐赠", trailing = { Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(20.dp)) }, onClick = { toast(context, "感谢支持") })

            SectionLabel("社交网络")
            SettingRow(title = "GitHub", onClick = { toast(context, "GitHub") })
            SettingRow(title = "Reddit", onClick = { toast(context, "Reddit") })
            SettingRow(title = "Telegram", onClick = { toast(context, "Telegram") })

            SectionLabel("其他")
            SettingRow(title = "隐私政策", trailing = { Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(20.dp)) }, onClick = { toast(context, "隐私政策") })
            SettingRow(title = "第三方许可", trailing = { Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(20.dp)) }, onClick = { toast(context, "第三方许可") })
            SettingRow(title = "版本", subtitle = "1.0")
        }
    }
}
