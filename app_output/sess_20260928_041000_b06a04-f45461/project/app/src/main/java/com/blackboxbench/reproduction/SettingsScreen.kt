package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private sealed class SettingRow {
    data class Header(val title: String) : SettingRow()
    data class Toggle(val label: String, val get: () -> Boolean, val set: (Boolean) -> Unit) : SettingRow()
    data class Sub(val label: String, val value: String? = null, val onClick: () -> Unit = {}) : SettingRow()
    data class Action(val label: String, val value: String? = null) : SettingRow()
}

@Composable
fun SettingsScreen(state: GalleryState, onBack: () -> Unit) {
    state.revision
    var autoplay by remember { mutableStateOf(false) }
    var rememberPos by remember { mutableStateOf(false) }
    var loopVideo by remember { mutableStateOf(false) }
    var horizontalPlay by remember { mutableStateOf(false) }
    var verticalVolume by remember { mutableStateOf(true) }
    var squareThumbs by remember { mutableStateOf(true) }
    var animatedGif by remember { mutableStateOf(false) }
    var horizontalScroll by remember { mutableStateOf(false) }
    var pullRefresh by remember { mutableStateOf(true) }
    var maxBrightness by remember { mutableStateOf(false) }
    var ultraHdr by remember { mutableStateOf(true) }
    var blackBg by remember { mutableStateOf(true) }
    var autoHideUi by remember { mutableStateOf(false) }
    var edgeSwitch by remember { mutableStateOf(false) }
    var keepOn by remember { mutableStateOf(true) }
    var verticalBrightness by remember { mutableStateOf(false) }
    var swipeDownExit by remember { mutableStateOf(true) }
    var showNotch by remember { mutableStateOf(true) }
    var deepZoom by remember { mutableStateOf(true) }
    var gestureRotate by remember { mutableStateOf(true) }
    var highestQuality by remember { mutableStateOf(false) }
    var twoFinger by remember { mutableStateOf(false) }
    var moreDetails by remember { mutableStateOf(false) }
    var lockApp by remember { mutableStateOf(false) }
    var lockHidden by remember { mutableStateOf(false) }
    var lockDelete by remember { mutableStateOf(false) }
    var deleteEmptyFolder by remember { mutableStateOf(false) }
    var keepDate by remember { mutableStateOf(true) }
    var noDeleteConfirm by remember { mutableStateOf(false) }
    var bottomButtons by remember { mutableStateOf(true) }
    var deleteToBin by remember { mutableStateOf(state.recycleBinEnabled) }
    var binInFolders by remember { mutableStateOf(state.showRecycleBinInFolders) }
    var binOnMainEnd by remember { mutableStateOf(false) }

    val rows = buildList {
        add(SettingRow.Header("外观"))
        add(SettingRow.Sub("自定义外观"))
        add(SettingRow.Header("常规"))
        add(SettingRow.Sub("语言", "中文"))
        add(SettingRow.Sub("更改日期和时间格式"))
        add(SettingRow.Sub("文件加载优先事项", "速度"))
        add(SettingRow.Sub("管理包含的文件夹"))
        add(SettingRow.Sub("管理排除的文件夹"))
        add(SettingRow.Toggle("显示隐藏项目", { state.showHiddenItems }) {
            state.showHiddenItems = it; state.touch(); state.persist()
        })
        add(SettingRow.Toggle("搜索所有文件，而不仅限于主屏幕显示的文件", { state.searchAllFiles }) {
            state.searchAllFiles = it; state.persist()
        })
        add(SettingRow.Header("视频"))
        add(SettingRow.Toggle("自动播放视频", { autoplay }) { autoplay = it })
        add(SettingRow.Toggle("记住视频上一次播放时的位置", { rememberPos }) { rememberPos = it })
        add(SettingRow.Toggle("循环播放视频", { loopVideo }) { loopVideo = it })
        add(SettingRow.Toggle("使用横向手势在独立屏幕上播放视频", { horizontalPlay }) { horizontalPlay = it })
        add(SettingRow.Toggle("使用纵向滑动手机控制视频音量和亮度", { verticalVolume }) { verticalVolume = it })
        add(SettingRow.Sub("点击视频时", "打开应用内播放器"))
        add(SettingRow.Header("缩略图"))
        add(SettingRow.Toggle("裁剪缩略图为正方形", { squareThumbs }) { squareThumbs = it })
        add(SettingRow.Toggle("GIF 动图的缩略图显示为动画", { animatedGif }) { animatedGif = it })
        add(SettingRow.Sub("文件缩略图样式"))
        add(SettingRow.Sub("文件夹缩略图样式", "方形"))
        add(SettingRow.Header("滚动"))
        add(SettingRow.Toggle("缩略图水平滚动", { horizontalScroll }) { horizontalScroll = it })
        add(SettingRow.Toggle("应用顶部下拉刷新", { pullRefresh }) { pullRefresh = it })
        add(SettingRow.Header("全屏显示"))
        add(SettingRow.Toggle("全屏时将屏幕调到最大亮度", { maxBrightness }) { maxBrightness = it })
        add(SettingRow.Toggle("以 HDR 模式显示 Ultra HDR 照片", { ultraHdr }) { ultraHdr = it })
        add(SettingRow.Toggle("全屏时使用黑色背景", { blackBg }) { blackBg = it })
        add(SettingRow.Toggle("全屏时自动隐藏系统界面", { autoHideUi }) { autoHideUi = it })
        add(SettingRow.Toggle("点击屏幕边缘切换文件", { edgeSwitch }) { edgeSwitch = it })
        add(SettingRow.Toggle("全屏查看照片时保持屏幕开启", { keepOn }) { keepOn = it })
        add(SettingRow.Toggle("使用纵向滑动手势控制图像亮度", { verticalBrightness }) { verticalBrightness = it })
        add(SettingRow.Toggle("使用下滑手势退出全屏", { swipeDownExit }) { swipeDownExit = it })
        add(SettingRow.Toggle("显示刘海 (如果可用)", { showNotch }) { showNotch = it })
        add(SettingRow.Sub("全屏时文件的旋转方向", "跟随系统设置"))
        add(SettingRow.Toggle("大幅度缩放图像", { deepZoom }) { deepZoom = it })
        add(SettingRow.Toggle("允许大幅度缩放图像", { deepZoom }) { deepZoom = it })
        add(SettingRow.Toggle("允许使用手势旋转图像", { gestureRotate }) { gestureRotate = it })
        add(SettingRow.Toggle("使用最高画质显示图像", { highestQuality }) { highestQuality = it })
        add(SettingRow.Toggle("两指双击后 1:1 缩放图像", { twoFinger }) { twoFinger = it })
        add(SettingRow.Header("更多详细信息"))
        add(SettingRow.Toggle("全屏时显示更多详细信息", { moreDetails }) { moreDetails = it })
        add(SettingRow.Header("安全性"))
        add(SettingRow.Toggle("用密码保护整个应用", { lockApp }) { lockApp = it })
        add(SettingRow.Toggle("用密码保护隐藏项目", { lockHidden }) { lockHidden = it })
        add(SettingRow.Toggle("用密码保护文件的删除和移动", { lockDelete }) { lockDelete = it })
        add(SettingRow.Header("文件操作"))
        add(SettingRow.Toggle("删除文件夹的所有文件后也删除该空文件夹", { deleteEmptyFolder }) { deleteEmptyFolder = it })
        add(SettingRow.Toggle("文件操作后保留原有修改日期", { keepDate }) { keepDate = it })
        add(SettingRow.Toggle("不再显示删除确认对话框", { noDeleteConfirm }) { noDeleteConfirm = it })
        add(SettingRow.Header("底部按钮"))
        add(SettingRow.Toggle("显示底部按钮", { bottomButtons }) { bottomButtons = it })
        add(SettingRow.Sub("管理底部按钮"))
        add(SettingRow.Header("回收站"))
        add(SettingRow.Toggle("将已删除项目移至回收站", { deleteToBin }) {
            deleteToBin = it; state.recycleBinEnabled = it; state.persist()
        })
        add(SettingRow.Toggle("在文件夹界面显示回收站", { binInFolders }) {
            binInFolders = it; state.showRecycleBinInFolders = it; state.persist()
        })
        add(SettingRow.Toggle("在主屏幕末尾显示回收站", { binOnMainEnd }) { binOnMainEnd = it })
        add(SettingRow.Action("清空回收站", "212.4 kB"))
        add(SettingRow.Header("迁移"))
        add(SettingRow.Action("清除缓存", "3.9 MB"))
        add(SettingRow.Action("导出收藏"))
        add(SettingRow.Action("导入收藏"))
        add(SettingRow.Action("导出设置"))
        add(SettingRow.Action("导入设置"))
    }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        TopBar {
            IconButtonBox(onClick = onBack) { IconBack() }
            Text("设置", fontSize = 19.sp, color = Palette.onSurface,
                modifier = Modifier.padding(start = 8.dp))
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            for (row in rows) {
                when (row) {
                    is SettingRow.Header -> SectionHeader(row.title)
                    is SettingRow.Toggle -> Row(
                        Modifier.fillMaxWidth().clickable { row.set(!row.get()) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(row.label, Modifier.weight(1f), fontSize = 15.sp, color = Palette.onSurface)
                        Switch(checked = row.get(), onCheckedChange = { row.set(it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = Palette.accent))
                    }
                    is SettingRow.Sub -> Row(
                        Modifier.fillMaxWidth().clickable { row.onClick() }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(row.label, Modifier.weight(1f), fontSize = 15.sp, color = Palette.onSurface)
                        if (row.value != null) Text(row.value, fontSize = 13.sp,
                            color = Palette.onSurfaceVariant)
                    }
                    is SettingRow.Action -> Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(row.label, Modifier.weight(1f), fontSize = 15.sp, color = Palette.onSurface)
                        if (row.value != null) Text(row.value, fontSize = 13.sp,
                            color = Palette.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val rows = listOf(
        "支持" to listOf("常见问题", "已知问题", "hello@fossify.org"),
        "帮助我们" to listOf("分享给好友", "贡献者", "向 Fossify 捐赠"),
        "社交网络" to listOf("GitHub", "Reddit", "Telegram"),
        "其他" to listOf("隐私政策", "第三方许可"),
    )
    Column(Modifier.fillMaxSize().background(Color.White)) {
        TopBar {
            IconButtonBox(onClick = onBack) { IconBack() }
            Text("关于", fontSize = 19.sp, color = Palette.onSurface,
                modifier = Modifier.padding(start = 8.dp))
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            for ((header, entries) in rows) {
                SectionHeader(header)
                for (e in entries) {
                    Text(e, fontSize = 15.sp, color = Palette.onSurface,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp))
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("版本 1.13.1", fontSize = 15.sp, color = Palette.onSurface,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp))
            Spacer(Modifier.height(60.dp))
        }
    }
}
