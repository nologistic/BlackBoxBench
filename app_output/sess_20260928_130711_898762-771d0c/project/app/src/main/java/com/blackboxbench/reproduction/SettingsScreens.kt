package com.blackboxbench.reproduction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsMainScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        AppTopBar("设置", onBack = onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                SearchPill(value = "", onValueChange = {}, placeholder = "搜索...")
            }
            Spacer(Modifier.height(6.dp))
            SettingRow(AppIcons.UiTheme, "用户界面", "外观、订阅、锁屏") { onOpen("settings_ui") }
            SettingRow(Icons.Filled.PlayArrow, "播放", "耳机控制、跳过间隔、队列") { onOpen("settings_playback") }
            SettingRow(AppIcons.Download, "下载", "更新间隔、移动数据、自动下载、自动删除") {
                onOpen("settings_downloads")
            }
            SettingRow(AppIcons.Cloud, "同步", "和其他设备同步") { onOpen("settings_sync") }
            SettingRow(AppIcons.Backup, "备份和恢复", "将订阅和队列转移到其他设备") {
                onOpen("settings_backup")
            }
            SettingRow(Icons.Filled.Notifications, "通知", onClick = { onOpen("settings_notifications") })
            SectionHeader("项目")
            SettingRow(AppIcons.Help, "文档和支持") {}
            SettingRow(AppIcons.Forum, "用户论坛") {}
            SettingRow(Icons.Filled.Favorite, "贡献") {}
            SettingRow(AppIcons.Bug, "报告错误") {}
        }
    }
}

@Composable
fun SettingsUiScreen(onBack: () -> Unit, model: AppModel) {
    val s = model.settings
    SettingsSection("用户界面", onBack) {
        SectionHeader("主题设置")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ThemeThumb("自动", "auto", s.themeMode, Modifier.weight(1f)) {
                model.updateSettings { it.copy(themeMode = "auto") }
            }
            ThemeThumb("浅色", "light", s.themeMode, Modifier.weight(1f)) {
                model.updateSettings { it.copy(themeMode = "light") }
            }
            ThemeThumb("深色", "dark", s.themeMode, Modifier.weight(1f)) {
                model.updateSettings { it.copy(themeMode = "dark") }
            }
        }
        SwitchRow("纯黑", "深色主题使用纯黑", s.pureBlack) {
            model.updateSettings { st -> st.copy(pureBlack = it) }
        }
        SwitchRow("动态配色", "基于背景图对应用进行着色", s.dynamicColor) {
            model.updateSettings { st -> st.copy(dynamicColor = it) }
        }
        SectionHeader("单集信息")
        SwitchRow(
            "使用单集封面",
            "启用后，如果可用，在列表中使用单集专属封面。未启用，应用将始终使用播客封面图片。",
            s.useEpisodeCover,
        ) { model.updateSettings { st -> st.copy(useEpisodeCover = it) } }
        SwitchRow(
            "显示剩余时长",
            "启用后显示单集剩余时长。未启用则显示单集总时长。",
            s.showRemainingTime,
        ) { model.updateSettings { st -> st.copy(showRemainingTime = it) } }
        SwitchRow(
            "根据播放速度调整媒体信息",
            "显示的位置和时长会根据播放速度调整",
            s.adjustBySpeed,
        ) { model.updateSettings { st -> st.copy(adjustBySpeed = it) } }
        SectionHeader("外部元素")
        SettingRow(AppIcons.Backup, "设置通知按钮", "更改播放通知的按钮") {}
        SectionHeader("行为")
        SettingRow(null, "默认页面", "启动 AntennaPod 时打开的页面", value = s.defaultPage) {}
        SwitchRow("底部导航", "一键直达核心界面，随时随地快速访问", s.bottomNavigation) {
            model.updateSettings { st -> st.copy(bottomNavigation = it) }
        }
        SettingRow(null, "自定义导航", "更改在抽屉式导航栏或底部导航中显示的项目") {}
        SwitchRow("返回按钮打开抽屉", "在默认页面上按下返回按钮将打开抽屉式导航栏", s.backOpensDrawer) {
            model.updateSettings { st -> st.copy(backOpensDrawer = it) }
        }
        SectionHeader("单集列表")
        SettingRow(null, "默认排序", "选择播客页面单集的默认排序", value = s.defaultSort) {}
        SettingRow(null, "滑动操作", "选择在列表中滑动单集时执行的操作", value = s.swipeActions) {}
        SwitchRow("首选流式播放", "在列表中显示流式播放按钮而非下载按钮", s.preferStreaming) {
            model.updateSettings { st -> st.copy(preferStreaming = it) }
        }
        SwitchRow("从下载屏幕播放", "在下载屏幕中显示播放按钮而非删除按钮", s.playFromDownloadScreen) {
            model.updateSettings { st -> st.copy(playFromDownloadScreen = it) }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ThemeThumb(
    label: String,
    mode: String,
    selectedMode: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val selected = mode == selectedMode
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color(0xFFF1F4F9))
                .then(
                    if (selected) Modifier.border(
                        2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)
                    ) else Modifier
                )
                .clickable(onClick = onClick),
        ) {
            ThemeMini(mode)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            fontSize = 14.sp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun ThemeMini(mode: String) {
    val dark = mode == "dark"
    val autoTop = Color(0xFF20303F)
    Box(
        Modifier
            .fillMaxSize()
            .padding(10.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (dark) Color(0xFF12161B) else Color.White)
    ) {
        Row(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .width(16.dp)
                    .height(24.dp)
                    .background(
                        when (mode) {
                            "auto" -> autoTop
                            "dark" -> Color(0xFF262C33)
                            else -> Color(0xFFE8F0FA)
                        }
                    )
            )
        }
        Column(Modifier.padding(start = 20.dp, top = 8.dp)) {
            Box(
                Modifier
                    .height(7.dp)
                    .fillMaxWidth(0.7f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(Modifier.height(6.dp))
            repeat(4) {
                Box(
                    Modifier
                        .height(5.dp)
                        .fillMaxWidth(if (it % 2 == 0) 0.85f else 0.6f)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (dark) Color(0xFF3A424B) else Color(0xFFCBD5E1))
                )
                Spacer(Modifier.height(5.dp))
            }
        }
    }
}

@Composable
fun SettingsPlaybackScreen(onBack: () -> Unit, model: AppModel) {
    val s = model.settings
    SettingsSection("播放", onBack) {
        SectionHeader("中断")
        SwitchRow("耳机或蓝牙断开", "耳机或蓝牙设备断开连接时暂停播放", s.headphoneDisconnect) {
            model.updateSettings { st -> st.copy(headphoneDisconnect = it) }
        }
        SectionHeader("播放控制")
        SettingRow(null, "快进跳转时间", "自定义点击快进按钮时向前跳转的秒数", value = "${s.fastForwardSec} 秒") {}
        SettingRow(null, "快退跳转时间", "自定义点击快退按钮时向后跳转的秒数", value = "${s.rewindSec} 秒") {}
        SettingRow(null, "播放速度", "自定义可用于变速播放的速度", value = "%.2f".format(s.playbackSpeed)) {}
        SectionHeader("重新分配硬件按钮")
        SettingRow(null, "前进按钮", "自定义快进按钮行为", value = s.forwardButton) {}
        SettingRow(null, "后退按钮", "自定义\"上一个\"按钮行为", value = s.rewindButton) {}
        SectionHeader("队列")
        SettingRow(null, "加入队列位置", "将单集添加到：${s.enqueueLocation}") {}
        SwitchRow("添加已下载单集到队列", "将已下载单集添加到队列", s.addDownloadedToQueue) {
            model.updateSettings { st -> st.copy(addDownloadedToQueue = it) }
        }
        SwitchRow("连续播放", "播放完成后跳转到下一队列项", s.continuousPlayback) {
            model.updateSettings { st -> st.copy(continuousPlayback = it) }
        }
        SettingRow(
            null,
            "智能标记为已播放",
            "即使单集仍有特定秒数的播放时长未完成，仍将其标记为已播放",
        ) {}
        SwitchRow("保留跳过的单集", "跳过单集时保留该单集", s.keepSkipped) {
            model.updateSettings { st -> st.copy(keepSkipped = it) }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun SettingsDownloadsScreen(onBack: () -> Unit, model: AppModel) {
    val s = model.settings
    SettingsSection("下载", onBack) {
        SettingRow(
            AppIcons.UiTheme,
            "选择数据文件夹",
            "/storage/emulated/0/Android/data/de.danoeh.antennapod/files",
        ) {}
        SectionHeader("自动化")
        SettingRow(AppIcons.Download, "刷新播客", "指定 AntennaPod 自动查找新单集的间隔", value = s.refreshInterval) {}
        SettingRow(null, "新单集操作", "为新单集采取的操作", value = s.newEpisodeAction) {}
        SettingRow(null, "自动下载", "配置单集的自动下载", value = s.autoDownload) {}
        SettingRow(null, "自动删除", "播放后或自动下载需要空间时删除单集", value = s.autoDelete) {}
        SwitchRow("删除后移出队列", "删除单集时自动将其从队列中移除", s.removeFromQueueAfterDeletion) {
            model.updateSettings { st -> st.copy(removeFromQueueAfterDeletion = it) }
        }
        SectionHeader("详细信息")
        SettingRow(null, "移动数据更新", "选择使用移动数据连接时应当允许的内容", value = s.mobileDataUpdate) {}
        SettingRow(null, "代理", "选择一个网络代理", value = s.proxy) {}
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun SettingsSyncScreen(onBack: () -> Unit) {
    SettingsSection("同步", onBack) {
        Spacer(Modifier.height(8.dp))
        SettingRow(null, "选择同步提供者", "您可以从多个服务提供者中选择，以同步您的订阅和单集播放状态。") {}
        Spacer(Modifier.height(10.dp))
        SettingRow(null, "立即同步", "同步订阅和单集状态更改") {}
        Spacer(Modifier.height(10.dp))
        SettingRow(null, "强制完整同步", "重新同步所有订阅和单集状态") {}
        Spacer(Modifier.height(10.dp))
        SettingRow(null, "登出") {}
    }
}

@Composable
fun SettingsBackupScreen(onBack: () -> Unit) {
    SettingsSection("备份和恢复", onBack) {
        Spacer(Modifier.height(8.dp))
        SettingRow(null, "数据库导出", "将数据库导出到文件") {}
        Spacer(Modifier.height(10.dp))
        SettingRow(null, "数据库导入", "从文件导入数据库") {}
        Spacer(Modifier.height(10.dp))
        SettingRow(null, "首选项导出", "将首选项导出到文件") {}
        Spacer(Modifier.height(10.dp))
        SettingRow(null, "首选项导入", "从文件导入首选项") {}
    }
}

@Composable
fun SettingsNotificationsScreen(onBack: () -> Unit, model: AppModel) {
    SettingsSection("通知", onBack) {
        SwitchRow("显示播放器通知", "在通知栏中显示播放控制", true) {}
        SwitchRow("新单集通知", "订阅的播客有新单集时通知", false) {}
        SettingRow(null, "通知按钮", "更改播放通知的按钮") {}
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsSection(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        AppTopBar(title, onBack = onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            content()
        }
    }
}
