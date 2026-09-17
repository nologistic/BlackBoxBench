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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(state: LoopState, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(
            Modifier.fillMaxWidth().background(BLUE).statusBarsPadding().height(58.dp).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(40.dp).clickable { onBack() }, contentAlignment = Alignment.Center) {
                BackIcon(Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Text("设置", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Medium)
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            SectionLabel("界面")
            ToggleRow("短按切换", "只需点击一下即可打卡，而不是长按。", state.shortTap) { state.shortTap = it }
            ToggleRow("将一天延长到午夜后的几个小时", "凌晨 3 点后再显示新的一天。如果你通常在午夜后入睡，这会很有用。重启应用后生效。", false) { }
            ToggleRow("启用跳过天数功能", "切换两次以添加跳过而不是复选标记。跳过将保持你的得分不变，且不会打破你的连续纪录", false) { }
            ToggleRow("对丢失的数据显示问号", "区分无数据和未完成习惯的日期。要输入一个习惯未完成，请切换两次。", false) { }
            ToggleRow("逆序显示日期", "在主界面以相反的顺序显示日期。", state.reversed) { state.reversed = it }
            ToggleRow("在深色主题中使用纯黑色", "以纯黑色背景代替深色主题中的灰色背景。 这可以降低 AMOLED 屏幕手机的耗电量。", false) { }
            ToggleRow("Disable animations", "Disable confetti animation after adding a checkmark.", false) { }
            PlainRow("微件不透明度", "调整主屏幕上小部件的不透明度。")
            PlainRow("一周的第一天", "星期一")

            SectionLabel("提醒")
            ToggleRow("使通知持久", "防止通知被滑掉。", false) { }
            PlainRow("自定义通知", "更改声音、振动、指示灯（呼吸灯）和其他通知设置")

            SectionLabel("数据库")
            PlainRow("导出完整备份", "生成一个包含所有数据的文件。该文件可以重新导入。")
            PlainRow("导出为 CSV", "生成可以通过电子表格软件打开的文件，如 Microsoft Excel 或 OpenOffice Calc。该文件无法重新导入。")
            PlainRow("导入数据", "支持本应用导出的完整备份文件，也支持 Tickmate、HabitBull 或 Rewire 的导出文件，请参阅常见问题以获取更多信息。")

            SectionLabel("故障排除")
            PlainRow("生成错误报告", null)
            PlainRow("修复数据库", null)

            SectionLabel("链接")
            PlainRow("帮助 & 常见问题", null)
            PlainRow("去 Play 商店评价此应用", null)
            PlainRow("关于应用", null)
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = BLUE, fontSize = 16.sp, fontWeight = FontWeight.Medium,
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 20.dp, bottom = 4.dp))
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color(0xFF222222), fontSize = 18.sp)
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(subtitle, color = Color(0xFF777777), fontSize = 14.sp)
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun PlainRow(title: String, subtitle: String?) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(title, color = Color(0xFF222222), fontSize = 18.sp)
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = Color(0xFF777777), fontSize = 14.sp)
        }
    }
}
