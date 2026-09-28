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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    var showAbout by remember { mutableStateOf(false) }
    if (showAbout) {
        AboutScreen(onBack = { showAbout = false })
        return
    }
    Column(Modifier.fillMaxSize().background(AppColors.SurfaceTint)) {
        androidx.activity.compose.BackHandler { onBack() }
        Row(
            Modifier.fillMaxWidth().padding(top = 40.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(48.dp).clickable { onBack() },
                contentAlignment = Alignment.Center
            ) { CoreIcon(IconBack, AppColors.OnSurface, 22.dp) }
            Text("设置", fontSize = 20.sp, color = AppColors.OnSurface)
        }
        Column(Modifier.verticalScroll(rememberScrollState())) {
            SettingsCard {
                SettingsRow(Glyph.Flag, "捐赠", subtitle = "考虑用捐赠显示您的支持！")
            }
            SettingsCard {
                SettingsRow(Glyph.Checklist, "本地清单")
                SettingsRow(IconAdd, "添加账号", glyph = null)
            }
            SettingsCard {
                SettingsRow(Glyph.List, "外观")
                SettingsRow(Glyph.Bell, "通知")
            }
            SettingsCard {
                SettingsRow(IconAdd, "任务默认值", glyph = null)
                SettingsRow(Glyph.List, "任务清单选项")
                SettingsRow(Glyph.Note, "编辑屏幕选项")
            }
            SettingsCard {
                SettingsRow(Glyph.Calendar, "日期和时间")
                SettingsRow(Glyph.List, "导航抽屉")
            }
            SettingsCard {
                SettingsRow(Glyph.Note, "备份")
                SettingsRow(Glyph.Checklist, "插件设置")
                SettingsRow(Glyph.Funnel, "高级")
            }
            SettingsCard {
                SettingsRow(Glyph.Inbox, "关于") { showAbout = true }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
    ) { content() }
}

@Composable
private fun SettingsRow(
    glyph: Glyph,
    title: String,
    subtitle: String? = null,
    glyph2: Glyph? = glyph,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit = {}
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) CoreIcon(icon, AppColors.OnSurface, 20.dp)
        else GlyphIcon(glyph, AppColors.OnSurface, size = 20.dp)
        Spacer(Modifier.width(20.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, color = AppColors.OnSurface)
            if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = AppColors.SecondaryText)
        }
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    glyph: Glyph? = null,
    subtitle: String? = null,
    onClick: () -> Unit = {}
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoreIcon(icon, AppColors.OnSurface, 20.dp)
        Spacer(Modifier.width(20.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, color = AppColors.OnSurface)
            if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = AppColors.SecondaryText)
        }
    }
}

@Composable
private fun AboutScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(AppColors.SurfaceTint)) {
        androidx.activity.compose.BackHandler { onBack() }
        Row(
            Modifier.fillMaxWidth().padding(top = 40.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(48.dp).clickable { onBack() },
                contentAlignment = Alignment.Center
            ) { CoreIcon(IconBack, AppColors.OnSurface, 22.dp) }
            Text("关于", fontSize = 20.sp, color = AppColors.OnSurface)
        }
        SettingsCard {
            SettingsRow(Glyph.History, "更新日志", subtitle = "版本 15.10")
            SettingsRow(Glyph.Bell, "博客通知", subtitle = "仅公告")
        }
        Text(
            "支持", fontSize = 13.sp, color = AppColors.SecondaryText,
            modifier = Modifier.padding(start = 24.dp, top = 14.dp, bottom = 4.dp)
        )
        SettingsCard {
            SettingsRow(Glyph.Inbox, "文档")
            SettingsRow(Glyph.Funnel, "问题跟踪器")
            SettingsRow(Glyph.Note, "联系开发者")
            SettingsRow(Glyph.Paperclip, "发送应用程序日志")
        }
        Text(
            "开源", fontSize = 13.sp, color = AppColors.SecondaryText,
            modifier = Modifier.padding(start = 24.dp, top = 14.dp, bottom = 4.dp)
        )
        SettingsCard {
            SettingsRow(Glyph.Tag, "源代码", subtitle = "Tasks 是遵循 GNU 通用公共许可证 v3.0 的自由开源软件")
        }
        Spacer(Modifier.height(32.dp))
    }
}
