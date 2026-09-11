# Functional Topology — loop_habit_tracker

- session: `sess_20260908_174007_687164`
- generated: 2026-09-08T18:20:15.990315+00:00
- coverage: 28 states · 22 features · 4 data · 8 edges (confirmed ratio 100%, 93 actions)

## Graph
```
loop_habit_tracker
├─ States
│  ├─ [✓] Onboarding_欢迎页 (1.00) `state_onboarding_欢迎页`
│  │    ─TRANSITIONS_TO→ Onboarding_养成新习惯页
│  ├─ [✓] Onboarding_养成新习惯页 (1.00) `state_onboarding_养成新习惯页`
│  │    ─TRANSITIONS_TO→ Onboarding_记录进步页
│  ├─ [✓] Onboarding_记录进步页 (1.00) `state_onboarding_记录进步页`
│  │    ─TRANSITIONS_TO→ 习惯列表_空状态
│  ├─ [✓] 习惯列表_空状态 (1.00) `state_习惯列表_空状态`
│  │    ─TRANSITIONS_TO→ 新建习惯_类型选择
│  ├─ [✓] 新建习惯_类型选择 (1.00) `state_新建习惯_类型选择`
│  ├─ [✓] 新建习惯_频率选择 (1.00) `state_新建习惯_频率选择`
│  ├─ [✓] 新建习惯_提醒时间选择 (1.00) `state_新建习惯_提醒时间选择`
│  ├─ [✓] 新建习惯_提醒星期选择 (1.00) `state_新建习惯_提醒星期选择`
│  ├─ [✓] 新建习惯_颜色选择 (1.00) `state_新建习惯_颜色选择`
│  ├─ [✓] 新建二元习惯_表单 (1.00) `state_新建二元习惯_表单`
│  │    ─TRANSITIONS_TO→ 通知权限请求
│  ├─ [✓] 通知权限请求 (1.00) `state_通知权限请求`
│  ├─ [✓] 习惯列表_有习惯 (1.00) `state_习惯列表_有习惯`
│  │    ─TRANSITIONS_TO→ 习惯详情_统计空数据
│  ├─ [✓] 习惯详情_统计空数据 (1.00) `state_习惯详情_统计空数据`
│  ├─ [✓] 二元习惯_打卡对话框 (1.00) `state_二元习惯_打卡对话框`
│  │    ─MUTATES→ 习惯列表_有习惯
│  ├─ [✓] 习惯详情_统计有数据 (1.00) `state_习惯详情_统计有数据`
│  ├─ [✓] 编辑习惯_表单 (1.00) `state_编辑习惯_表单`
│  ├─ [✓] 新建量化习惯_表单与目标类型 (1.00) `state_新建量化习惯_表单与目标类型`
│  ├─ [✓] 量化习惯_数值打卡对话框 (1.00) `state_量化习惯_数值打卡对话框`
│  ├─ [✓] 习惯列表_筛选菜单 (1.00) `state_习惯列表_筛选菜单`
│  ├─ [✓] 习惯列表_排序菜单 (1.00) `state_习惯列表_排序菜单`
│  ├─ [✓] 主界面_更多菜单 (1.00) `state_主界面_更多菜单`
│  ├─ [✓] 设置_界面选项上部 (1.00) `state_设置_界面选项上部`
│  ├─ [✓] 设置_提醒数据库与排障 (1.00) `state_设置_提醒数据库与排障`
│  ├─ [✓] 设置_链接底部 (1.00) `state_设置_链接底部`
│  ├─ [✓] 关于应用 (1.00) `state_关于应用`
│  ├─ [✓] 习惯列表_深色主题 (1.00) `state_习惯列表_深色主题`
│  ├─ [✓] 习惯列表_多选操作模式 (1.00) `state_习惯列表_多选操作模式`
│  ├─ [✓] 删除习惯_确认 (1.00) `state_删除习惯_确认`
├─ Features
│  ├─ [✓] 引导页前进 (1.00) `feature_引导页前进`
│  ├─ [✓] 完成首次启动引导 (1.00) `feature_完成首次启动引导`
│  ├─ [✓] 选择习惯类型 (1.00) `feature_选择习惯类型`
│  ├─ [✓] 新建二元习惯必填校验 (1.00) `feature_新建二元习惯必填校验`
│  ├─ [✓] 配置习惯颜色与提醒 (1.00) `feature_配置习惯颜色与提醒`
│  ├─ [✓] 保存新建二元习惯 (1.00) `feature_保存新建二元习惯`
│  ├─ [✓] 查看习惯详情与统计 (1.00) `feature_查看习惯详情与统计`
│  ├─ [✓] 二元习惯每日打卡 (1.00) `feature_二元习惯每日打卡`
│  ├─ [✓] 打卡驱动统计更新 (1.00) `feature_打卡驱动统计更新`
│  ├─ [✓] 切换成绩统计周期 (1.00) `feature_切换成绩统计周期`
│  ├─ [✓] 编辑并保存习惯 (1.00) `feature_编辑并保存习惯`
│  ├─ [✓] 新建量化习惯必填校验 (1.00) `feature_新建量化习惯必填校验`
│  ├─ [✓] 保存新建量化习惯 (1.00) `feature_保存新建量化习惯`
│  ├─ [✓] 量化习惯每日数值记录 (1.00) `feature_量化习惯每日数值记录`
│  ├─ [✓] 筛选与排序习惯列表 (1.00) `feature_筛选与排序习惯列表`
│  ├─ [✓] 切换深色主题 (1.00) `feature_切换深色主题`
│  ├─ [✓] 批量选择与存档习惯 (1.00) `feature_批量选择与存档习惯`
│  ├─ [✓] 习惯、打卡与主题持久化 (1.00) `feature_习惯_打卡与主题持久化`
│  ├─ [✓] 横向浏览历史日期 (1.00) `feature_横向浏览历史日期`
│  ├─ [✓] 应用设置与数据管理 (1.00) `feature_应用设置与数据管理`
│  ├─ [✓] 查看关于应用信息 (1.00) `feature_查看关于应用信息`
│  ├─ [✓] 取消存档与删除保护 (1.00) `feature_取消存档与删除保护`
├─ Data
│  ├─ [✓] OnboardingCompletion (0.80) `data_onboardingcompletion`
│  ├─ [✓] Habit (1.00) `data_habit`
│  │    ─PERSISTS_TO→ 习惯列表_深色主题
│  ├─ [✓] CheckIn (1.00) `data_checkin`
│  ├─ [✓] AppPreferences (1.00) `data_apppreferences`
```

## Features
### 引导页前进 `feature_引导页前进`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 轻触欢迎页右下箭头进入第 2 页，页面颜色、标题、插画和分页指示同步变化。
- evidence: step 4 (frame 6 → 7)

### 完成首次启动引导 `feature_完成首次启动引导`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 在第 3 页轻触右下勾号进入习惯列表空状态。
- evidence: step 6 (frame 10 → 11)

### 选择习惯类型 `feature_选择习惯类型`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 点击工具栏新增按钮显示两种新建入口：二元完成/未完成习惯与可量化习惯。
- evidence: step 7 (frame 12 → 13)

### 新建二元习惯必填校验 `feature_新建二元习惯必填校验`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 未填写标题直接保存时，页面保留并在标题框内显示红色感叹号；问题和备注未显示必填错误。
- evidence: step 9 (frame 16 → 17)

### 配置习惯颜色与提醒 `feature_配置习惯颜色与提醒`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 可从 20 色调色板选择主题色；设置提醒时间后可选择一周中的任意天，周末取消时摘要显示“工作日”。
- evidence: step 18 (frame 31 → 32)

### 保存新建二元习惯 `feature_保存新建二元习惯`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 填写标题等字段后点击保存，返回习惯列表并新增对应颜色的习惯行；若启用了提醒且尚未授权，会随后请求系统通知权限。
- evidence: step 27 (frame 44 → 45)

### 查看习惯详情与统计 `feature_查看习惯详情与统计`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 点击列表中的习惯标题进入详情页，可查看配置、备注、总览指标、成绩折线图和历史。
- evidence: step 29 (frame 48 → 49)

### 二元习惯每日打卡 `feature_二元习惯每日打卡`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 点击日期单元格后从对话框选择绿色勾号，列表中该日由灰色 X 变为习惯色勾号，左侧圆环出现对应进度段。
- evidence: step 34 (frame 58 → 59)

### 打卡驱动统计更新 `feature_打卡驱动统计更新`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 每日打卡写入后，习惯详情的总览、趋势图和历史柱状图即时反映记录；一次完成使总数变为 1，并显示 5% 及月年 +5%。
- evidence: step 35 (frame 60 → 61)

### 切换成绩统计周期 `feature_切换成绩统计周期`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 成绩图的下拉提供天、周、月、季度、年五种聚合周期；选择后标题摘要和图表粒度更新。
- evidence: step 30 (frame 50 → 51)

### 编辑并保存习惯 `feature_编辑并保存习惯`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 详情页铅笔打开预填编辑表单；修改备注并保存后返回详情，原打卡统计保持且备注显示新文本。
- evidence: step 37 (frame 64 → 65)

### 新建量化习惯必填校验 `feature_新建量化习惯必填校验`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 空表单保存时标题与目标字段显示红色感叹号；问题、单位、备注不标必填。
- evidence: step 45 (frame 76 → 77)

### 保存新建量化习惯 `feature_保存新建量化习惯`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 填写标题、单位、目标与其他配置后保存，列表新增对应颜色的量化习惯行；每日单元格以“0 pages”等数值与单位显示，而不是勾叉。
- evidence: step 59 (frame 94 → 95)

### 量化习惯每日数值记录 `feature_量化习惯每日数值记录`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 点击日期单元格可输入带负号或小数的数值并保存；保存 24 后列表当前日从“0 pages”更新为橙色“24 pages”，表示达到“至少 20”的目标。
- evidence: step 60 (frame 96 → 97)

### 筛选与排序习惯列表 `feature_筛选与排序习惯列表`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 列表可切换是否隐藏已存档/已完成习惯，并支持手动、名称、颜色、分数、状态排序。
- evidence: step 63 (frame 101 → 102)

### 切换深色主题 `feature_切换深色主题`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 主界面更多菜单的“深色主题”复选项可即时切换整个列表到深色配色。
- evidence: step 73 (frame 119 → 120)

### 批量选择与存档习惯 `feature_批量选择与存档习惯`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 长按习惯进入多选模式，更多菜单提供“存档”和“删除”；对 Read Pages 选择存档后，由于默认隐藏已存档，列表立即只剩 Morning Stretch。
- evidence: step 75 (frame 122 → 123)

### 习惯、打卡与主题持久化 `feature_习惯_打卡与主题持久化`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 重启 App 后不再显示 onboarding，直接回到深色习惯列表；两个习惯仍存在，Morning Stretch 当前日仍为绿色勾，Read Pages 当前日仍为 24 pages，深色主题也保留。
- evidence: step 83 (frame 136 → 137)

### 横向浏览历史日期 `feature_横向浏览历史日期`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 在日期栏横向左滑可从当前附近的 9 月 8–4 日切换到更早的 9 月 5–1 日，两个习惯的对应历史单元格同步滚动；可反向滑回。
- evidence: step 92 (frame 148 → 149)

### 应用设置与数据管理 `feature_应用设置与数据管理`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 设置页集中管理短按打卡、换日时间、跳过天数、缺失数据问号、日期顺序、深色纯黑、动画、周起始日、持久通知与自定义通知；并提供完整备份/CSV 导出、数据导入、错误报告和数据库修复。
- evidence: step 67 (frame 108 → 109)

### 查看关于应用信息 `feature_查看关于应用信息`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 关于页展示 Loop 图标、版本 2.3.1、商店/反馈/翻译/GitHub/隐私链接和开发者名单。
- evidence: step 70 (frame 114 → 115)

### 取消存档与删除保护 `feature_取消存档与删除保护`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 已存档习惯可在取消“隐藏已存档”后以灰化样式显示，并可在多选更多菜单选择“取消存档”；删除操作会先显示永久且不可撤销的确认对话框。
- evidence: step 78 (frame 128 → 129)
