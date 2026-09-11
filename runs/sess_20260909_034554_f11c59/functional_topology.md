# Functional Topology — loop_habit_tracker

- session: `sess_20260909_034554_f11c59`
- generated: 2026-09-09T04:38:45.656673+00:00
- coverage: 28 states · 33 features · 5 data · 9 edges (confirmed ratio 100%, 150 actions)

## Graph
```
loop_habit_tracker
├─ States
│  ├─ [✓] 欢迎引导第 1 页 (0.99) `state_欢迎引导第_1_页`
│  │    ─TRANSITIONS_TO→ 欢迎引导第 2 页
│  ├─ [✓] 欢迎引导第 2 页 (0.99) `state_欢迎引导第_2_页`
│  │    ─TRANSITIONS_TO→ 欢迎引导第 3 页
│  ├─ [✓] 欢迎引导第 3 页 (0.99) `state_欢迎引导第_3_页`
│  │    ─TRANSITIONS_TO→ 习惯列表空状态
│  ├─ [✓] 习惯列表空状态 (0.99) `state_习惯列表空状态`
│  │    ─TRANSITIONS_TO→ 新建习惯类型选择
│  ├─ [✓] 新建习惯类型选择 (0.99) `state_新建习惯类型选择`
│  │    ─TRANSITIONS_TO→ 新建完成与否习惯表单
│  │    ─TRANSITIONS_TO→ 新建可量化习惯表单
│  ├─ [✓] 新建完成与否习惯表单 (0.99) `state_新建完成与否习惯表单`
│  │    ─MUTATES→ 含习惯的列表
│  ├─ [✓] 习惯颜色选择器 (0.99) `state_习惯颜色选择器`
│  ├─ [✓] 频率设置弹窗 (0.99) `state_频率设置弹窗`
│  ├─ [✓] 提醒时间选择器 (0.99) `state_提醒时间选择器`
│  ├─ [✓] 提醒星期选择器 (0.99) `state_提醒星期选择器`
│  ├─ [✓] 通知权限请求 (0.99) `state_通知权限请求`
│  ├─ [✓] 含习惯的列表 (0.99) `state_含习惯的列表`
│  ├─ [✓] 完成与否打卡确认 (0.98) `state_完成与否打卡确认`
│  │    ─MUTATES→ 每日完成记录
│  ├─ [✓] 完成与否习惯详情统计 (0.99) `state_完成与否习惯详情统计`
│  ├─ [✓] 新建可量化习惯表单 (0.99) `state_新建可量化习惯表单`
│  ├─ [✓] 量化数值打卡弹窗 (0.99) `state_量化数值打卡弹窗`
│  │    ─MUTATES→ 每日量化记录
│  ├─ [✓] 可量化习惯详情统计 (0.99) `state_可量化习惯详情统计`
│  ├─ [✓] 列表筛选菜单 (0.99) `state_列表筛选菜单`
│  ├─ [✓] 主菜单 (0.99) `state_主菜单`
│  ├─ [✓] 设置页上半部 (0.99) `state_设置页上半部`
│  ├─ [✓] 设置页数据与提醒 (0.99) `state_设置页数据与提醒`
│  ├─ [✓] 关于应用页 (0.99) `state_关于应用页`
│  ├─ [✓] 编辑习惯表单 (0.99) `state_编辑习惯表单`
│  ├─ [✓] 习惯多选模式 (0.99) `state_习惯多选模式`
│  ├─ [✓] 删除习惯确认 (0.99) `state_删除习惯确认`
│  ├─ [✓] state_一周第一天选择器 (0.99) `state_一周第一天选择器`
│  ├─ [✓] state_微件不透明度选择器 (0.99) `state_微件不透明度选择器`
│  ├─ [✓] state_习惯日历与连续记录 (0.99) `state_习惯日历与连续记录`
├─ Features
│  ├─ [✓] 首次启动三页引导 (0.99) `feature_首次启动三页引导`
│  ├─ [✓] 新建习惯必填校验 (0.99) `feature_新建习惯必填校验`
│  ├─ [✓] 提醒触发通知权限 (0.99) `feature_提醒触发通知权限`
│  ├─ [✓] 创建完成与否习惯 (0.99) `feature_创建完成与否习惯`
│  ├─ [✓] 完成与否日期打卡 (0.99) `feature_完成与否日期打卡`
│  ├─ [✓] 习惯统计详情 (0.99) `feature_习惯统计详情`
│  ├─ [✓] 统计周期切换 (0.99) `feature_统计周期切换`
│  ├─ [✓] 详情更多菜单 (0.99) `feature_详情更多菜单`
│  ├─ [✓] 导出习惯 (0.90) `feature_导出习惯`
│  ├─ [✓] 量化目标类型 (0.99) `feature_量化目标类型`
│  ├─ [✓] 量化习惯必填校验 (0.99) `feature_量化习惯必填校验`
│  ├─ [✓] 创建可量化习惯 (0.99) `feature_创建可量化习惯`
│  ├─ [✓] 量化日期打卡 (0.99) `feature_量化日期打卡`
│  ├─ [✓] 量化目标进度与统计 (0.99) `feature_量化目标进度与统计`
│  ├─ [✓] 隐藏已完成习惯 (0.99) `feature_隐藏已完成习惯`
│  ├─ [✓] 习惯排序 (0.99) `feature_习惯排序`
│  ├─ [✓] 深色主题 (0.99) `feature_深色主题`
│  ├─ [✓] 编辑习惯 (0.99) `feature_编辑习惯`
│  ├─ [✓] 批量选择与操作 (0.99) `feature_批量选择与操作`
│  ├─ [✓] 习惯存档 (0.99) `feature_习惯存档`
│  ├─ [✓] 永久删除习惯 (0.99) `feature_永久删除习惯`
│  ├─ [✓] 历史日期横向浏览 (0.99) `feature_历史日期横向浏览`
│  ├─ [✓] 默认长按直接打卡 (0.99) `feature_默认长按直接打卡`
│  ├─ [✓] 长按完成状态切换 (0.99) `feature_长按完成状态切换`
│  ├─ [✓] 短按切换设置 (0.99) `feature_短按切换设置`
│  ├─ [✓] 自定义一周起始日 (0.99) `feature_自定义一周起始日`
│  ├─ [✓] 微件不透明度 (0.99) `feature_微件不透明度`
│  ├─ [✓] 界面与打卡高级设置 (0.98) `feature_界面与打卡高级设置`
│  ├─ [✓] 提醒高级设置 (0.98) `feature_提醒高级设置`
│  ├─ [✓] 数据备份导入导出 (0.98) `feature_数据备份导入导出`
│  ├─ [✓] 日历热力图与连续记录 (0.99) `feature_日历热力图与连续记录`
│  ├─ [✓] 日历批量编辑历史 (0.99) `feature_日历批量编辑历史`
│  ├─ [✓] 本地持久化 (0.99) `feature_本地持久化`
├─ Data
│  ├─ [✓] 完成与否习惯 (0.98) `data_完成与否习惯`
│  ├─ [✓] 每日完成记录 (0.99) `data_每日完成记录`
│  ├─ [✓] 可量化习惯 (0.99) `data_可量化习惯`
│  ├─ [✓] 每日量化记录 (0.99) `data_每日量化记录`
│  ├─ [✓] 应用偏好设置 (0.98) `data_应用偏好设置`
```

## Features
### 首次启动三页引导 `feature_首次启动三页引导`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 首次启动依次展示欢迎、打卡说明和长期进度说明三页；前两页用右下箭头继续，第三页用勾选进入习惯列表。
- evidence: step 3 (frame 5 → 6)

### 新建习惯必填校验 `feature_新建习惯必填校验`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 在完成与否习惯表单不填写内容直接保存时，页面保持不变，并在习惯标题输入框右侧显示红色感叹号；问题、频率、提醒和备注未显示错误。
- evidence: step 8 (frame 15 → 16)

### 提醒触发通知权限 `feature_提醒触发通知权限`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 首次保存带提醒的习惯后请求系统通知权限；用户可选择允许或不允许。
- evidence: step 39 (frame 62 → 63)

### 创建完成与否习惯 `feature_创建完成与否习惯`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 用户可填写标题、问题、主题颜色、灵活频率、提醒时间与星期、备注并保存；保存后以带多日完成标记的行出现在主列表。
- evidence: step 39 (frame 62 → 63)

### 完成与否日期打卡 `feature_完成与否日期打卡`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 点击习惯行某日的叉号会打开勾/叉选择；选择勾后该日期标记立即变为习惯主题色勾，可用于当前或历史日期。
- evidence: step 41 (frame 66 → 67)

### 习惯统计详情 `feature_习惯统计详情`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 点击习惯标题进入详情，可查看问题、频率、提醒、备注、总体成绩百分比、月年变化、总完成数，以及带周期选择的成绩和历史图表。
- evidence: step 47 (frame 75 → 76)

### 统计周期切换 `feature_统计周期切换`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 成绩图表的周期下拉提供天、周、月、季度、年五种聚合视图。
- evidence: step 48 (frame 77 → 78)

### 详情更多菜单 `feature_详情更多菜单`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 习惯详情顶栏更多菜单包含导出与删除两项。
- evidence: step 52 (frame 84 → 85)

### 导出习惯 `feature_导出习惯`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- 在习惯详情选择导出后，界面返回主习惯列表，未出现可见成功提示或分享目标。
- evidence: step 53 (frame 86 → 87)

### 量化目标类型 `feature_量化目标类型`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 量化习惯的目标类型可选“至少”或“至多”，用于定义数值目标的达成方向。
- evidence: step 56 (frame 91 → 92)

### 量化习惯必填校验 `feature_量化习惯必填校验`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 空量化表单保存后仅标题和目标数值显示红色感叹号；问题、单位、频率、目标类型、提醒和备注未显示错误。
- evidence: step 58 (frame 94 → 95)

### 创建可量化习惯 `feature_创建可量化习惯`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 可创建带标题、问题、单位、数值目标、频率、目标方向、提醒和备注的量化习惯；保存后列表按日期显示数值与单位。
- evidence: step 74 (frame 112 → 113)

### 量化日期打卡 `feature_量化日期打卡`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 点击量化习惯某日的数值可输入整数、小数或负数样式的数字；保存后该日期格以习惯主题色显示数值和单位。
- evidence: step 75 (frame 114 → 115)

### 量化目标进度与统计 `feature_量化目标进度与统计`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 量化习惯详情将输入值对照目标方向计算达成率，并展示今日、周、月、季度、年累计/剩余目标、成绩百分比和历史数值图；至少/至多方向用箭头图标表示。
- evidence: step 78 (frame 119 → 120)

### 隐藏已完成习惯 `feature_隐藏已完成习惯`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 启用“隐藏已完成”后，已达到本期目标的 Read 习惯立即从列表消失；未达到或量化中的 Water 仍显示。
- evidence: step 81 (frame 124 → 125)

### 习惯排序 `feature_习惯排序`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 排序子菜单提供手动、按名称、按颜色、按分数、按状态五种方式；当前手动排序项旁显示向上箭头表示排序方向。
- evidence: step 83 (frame 127 → 128)

### 深色主题 `feature_深色主题`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 勾选主菜单的深色主题后，主列表背景、日期栏与习惯卡即时切换为黑/深灰配色，彩色标题与打卡标记保留。
- evidence: step 88 (frame 135 → 136)

### 编辑习惯 `feature_编辑习惯`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 详情页铅笔打开预填编辑表单；修改标题并保存后详情标题即时更新，原有 5.5 cups 记录和统计保持不变。
- evidence: step 98 (frame 153 → 154)

### 批量选择与操作 `feature_批量选择与操作`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 长按进入多选后可点选多个习惯；顶部显示已选数量，并可批量编辑、换色、存档或删除。
- evidence: step 104 (frame 161 → 162)

### 习惯存档 `feature_习惯存档`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 在多选模式更多菜单选择存档后，所选 Water Goal 从默认列表消失；筛选菜单默认隐藏已存档。
- evidence: step 110 (frame 169 → 170)

### 永久删除习惯 `feature_永久删除习惯`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 删除前弹窗明确提示永久删除且不可撤消；确认后临时习惯立即从列表消失。
- evidence: step 124 (frame 188 → 189)

### 历史日期横向浏览 `feature_历史日期横向浏览`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 主列表日期条与每个习惯的日期单元同步横向滚动，可浏览更早日期并查看或编辑对应状态。
- evidence: step 126 (frame 192 → 193)

### 默认长按直接打卡 `feature_默认长按直接打卡`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 在未启用“短按切换”时，长按完成与否习惯的日期单元会直接将该日标记为完成，不出现备注弹窗；短按则打开带完成/未完成选择的弹窗。
- evidence: step 127 (frame 194 → 195)

### 长按完成状态切换 `feature_长按完成状态切换`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 对完成与否习惯的同一日期单元反复长按，可在完成标记与未完成状态之间直接切换。
- evidence: step 127 (frame 194 → 195)

### 短按切换设置 `feature_短按切换设置`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 开启设置中的“短按切换”后，单击完成与否习惯的日期单元会直接更新完成状态，不再先打开备注弹窗。
- evidence: step 132 (frame 204 → 205)

### 自定义一周起始日 `feature_自定义一周起始日`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 用户可将星期六至星期五中的任意一天设为一周的第一天；默认摘要显示星期一。
- evidence: step 138 (frame 213 → 214)

### 微件不透明度 `feature_微件不透明度`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 主屏幕小部件的不透明度可在100%、80%、60%、40%、20%和0%六档之间选择，默认100%。
- evidence: step 140 (frame 216 → 217)

### 界面与打卡高级设置 `feature_界面与打卡高级设置`
- status: ClaimStatus.CONFIRMED · confidence: 0.98
- 设置页提供短按切换、跨午夜三小时、跳过天数、缺失数据问号、日期逆序、纯黑深色主题、禁用庆祝动画、微件不透明度和一周起始日等选项。
- evidence: step 131 (frame 202 → 203)

### 提醒高级设置 `feature_提醒高级设置`
- status: ClaimStatus.CONFIRMED · confidence: 0.98
- 设置页支持持久通知，以及自定义声音、振动和指示灯等通知行为。
- evidence: step 137 (frame 211 → 212)

### 数据备份导入导出 `feature_数据备份导入导出`
- status: ClaimStatus.CONFIRMED · confidence: 0.98
- 设置页支持可重新导入的完整备份、CSV导出，以及从本应用备份或Tickmate、HabitBull、Rewire文件导入。
- evidence: step 137 (frame 211 → 212)

### 日历热力图与连续记录 `feature_日历热力图与连续记录`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 完成与否习惯详情提供跨月日历热力图、最佳连续完成次数及日期范围、以及按星期和月份的频率分布。
- evidence: step 144 (frame 222 → 223)

### 日历批量编辑历史 `feature_日历批量编辑历史`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 点击详情日历下的“编辑”会打开放大的历史日历；直接点击日期格可补录或切换该日状态，统计与连续记录随之更新。
- evidence: step 145 (frame 224 → 225)

### 本地持久化 `feature_本地持久化`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 强制重启应用后不再重复显示引导；已创建习惯、标题编辑、打卡值、深色主题和短按切换设置均保留。
- evidence: step 149 (frame 230 → 231)
