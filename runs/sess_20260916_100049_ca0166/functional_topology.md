# Functional Topology — google_clock

- session: `sess_20260916_100049_ca0166`
- generated: 2026-09-16T12:08:55.824013+00:00
- coverage: 23 states · 22 features · 4 data · 9 edges (confirmed ratio 100%, 84 actions)

## Graph
```
google_clock
├─ States
│  ├─ [✓] alarm_list (0.95) `state_alarm_list`
│  │    ─TRANSITIONS_TO→ new_alarm_detail_2130
│  │    ─REVEALS→ alarm_time_picker_dialog
│  ├─ [✓] bedtime_onboarding (0.90) `state_bedtime_onboarding`
│  │    ─TRANSITIONS_TO→ bedtime_setup_wake_alarm
│  ├─ [✓] bedtime_setup_wake_alarm (0.90) `state_bedtime_setup_wake_alarm`
│  ├─ [✓] bedtime_setup_bedtime_time (0.90) `state_bedtime_setup_bedtime_time`
│  │    ─TRANSITIONS_TO→ bedtime_configured
│  ├─ [✓] bedtime_configured (0.90) `state_bedtime_configured`
│  │    ─MUTATES→ alarm_list_with_bedtime_wake
│  ├─ [✓] calendar_permission_dialog (0.90) `state_calendar_permission_dialog`
│  ├─ [✓] bedtime_cards (0.85) `state_bedtime_cards`
│  ├─ [✓] stopwatch_idle (0.90) `state_stopwatch_idle`
│  │    ─TRANSITIONS_TO→ stopwatch_running
│  ├─ [✓] stopwatch_running (0.90) `state_stopwatch_running`
│  │    ─TRANSITIONS_TO→ stopwatch_paused
│  ├─ [✓] stopwatch_paused (0.90) `state_stopwatch_paused`
│  ├─ [✓] alarm_list_with_bedtime_wake (0.95) `state_alarm_list_with_bedtime_wake`
│  │    ─MUTATES→ clock_tab_home
│  ├─ [✓] wake_alarm_detail (0.90) `state_wake_alarm_detail`
│  ├─ [✓] alarm_time_picker_dialog (0.90) `state_alarm_time_picker_dialog`
│  ├─ [✓] new_alarm_detail_2130 (0.90) `state_new_alarm_detail_2130`
│  ├─ [✓] alarm_sound_picker (0.90) `state_alarm_sound_picker`
│  ├─ [✓] clock_tab_home (0.95) `state_clock_tab_home`
│  ├─ [✓] timer_input (0.90) `state_timer_input`
│  │    ─TRANSITIONS_TO→ timer_running
│  ├─ [✓] timer_running (0.90) `state_timer_running`
│  ├─ [✓] timer_multiple (0.90) `state_timer_multiple`
│  ├─ [✓] settings_page (0.90) `state_settings_page`
│  ├─ [✓] overflow_menu (0.90) `state_overflow_menu`
│  ├─ [✓] screensaver_prompt (0.85) `state_screensaver_prompt`
│  ├─ [✓] screensaver_fullscreen (0.85) `state_screensaver_fullscreen`
├─ Features
│  ├─ [✓] 秒表计次 (0.90) `feature_秒表计次`
│  ├─ [✓] 秒表暂停/继续 (0.90) `feature_秒表暂停_继续`
│  ├─ [✓] 秒表重置 (0.90) `feature_秒表重置`
│  ├─ [✓] 闹钟删除 (0.90) `feature_闹钟删除`
│  ├─ [✓] 闹钟启用/停用开关 (0.90) `feature_闹钟启用_停用开关`
│  ├─ [✓] 新建闹钟 (0.90) `feature_新建闹钟`
│  ├─ [✓] 闹钟标签 (0.90) `feature_闹钟标签`
│  ├─ [✓] 闹钟重复星期选择 (0.85) `feature_闹钟重复星期选择`
│  ├─ [✓] 闹钟提示音选择 (0.90) `feature_闹钟提示音选择`
│  ├─ [✓] 时钟主页显示时间与下一闹钟 (0.90) `feature_时钟主页显示时间与下一闹钟`
│  ├─ [✓] 更改日期和时间提示 (0.70) `feature_更改日期和时间提示`
│  ├─ [✓] 显示含秒数设置 (0.90) `feature_显示含秒数设置`
│  ├─ [✓] 定时器键盘输入 (0.85) `feature_定时器键盘输入`
│  ├─ [✓] 定时器暂停/继续 (0.90) `feature_定时器暂停_继续`
│  ├─ [✓] 定时器加一分钟 (0.90) `feature_定时器加一分钟`
│  ├─ [✓] 定时器完成态与超时计数 (0.85) `feature_定时器完成态与超时计数`
│  ├─ [✓] 定时器重置到完整时长 (0.85) `feature_定时器重置到完整时长`
│  ├─ [✓] 多个定时器 (0.90) `feature_多个定时器`
│  ├─ [✓] 关闭定时器 (0.85) `feature_关闭定时器`
│  ├─ [✓] 闹钟持久化 (0.90) `feature_闹钟持久化`
│  ├─ [✓] 时钟样式数字/指针切换 (0.85) `feature_时钟样式数字_指针切换`
│  ├─ [✓] 屏保全屏模式 (0.80) `feature_屏保全屏模式`
├─ Data
│  ├─ [✓] Alarm（闹钟） (0.90) `data_alarm_闹钟`
│  ├─ [✓] Stopwatch（秒表） (0.85) `data_stopwatch_秒表`
│  ├─ [✓] Timer（定时器） (0.85) `data_timer_定时器`
│  ├─ [✓] BedtimeSchedule（就寝时间表） (0.90) `data_bedtimeschedule_就寝时间表`
```

## Features
### 秒表计次 `feature_秒表计次`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- 秒表计次：运行中点击计次按钮，圆下方列出每条记录（#编号、该圈用时、累计用时），新记录置顶。
- evidence: step 15 (frame 28 → 29)

### 秒表暂停/继续 `feature_秒表暂停_继续`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- 秒表可通过中央按钮暂停与继续；暂停后计次按钮隐藏。
- evidence: step 16 (frame 30 → 31)

### 秒表重置 `feature_秒表重置`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- 秒表重置按钮清空计时与所有计次记录，直接回到初始态。
- evidence: step 17 (frame 32 → 33)

### 闹钟删除 `feature_闹钟删除`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- 闹钟详情卡片底部有"删除"，点击即删除该闹钟。
- evidence: step 22 (frame 42 → 43)

### 闹钟启用/停用开关 `feature_闹钟启用_停用开关`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- 闹钟列表每条右侧有开关可启用/停用，启用后高亮。
- evidence: step 23 (frame 44 → 45)

### 新建闹钟 `feature_新建闹钟`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- 通过加号创建闹钟：打开"选择时间"对话框（表盘/键盘两种输入），确定后创建闹钟并自动展开。
- evidence: step 24 (frame 46 → 47)

### 闹钟标签 `feature_闹钟标签`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- 可为闹钟设置文字标签，设置后显示在卡片顶部。
- evidence: step 30 (frame 57 → 58)

### 闹钟重复星期选择 `feature_闹钟重复星期选择`
- status: ClaimStatus.CONFIRMED · confidence: 0.85
- 闹钟可多选一周重复日；选定重复日后下次响铃文本更新，并额外出现"暂停闹钟"行（未选重复日时为"预定闹钟时间"）。
- evidence: step 34 (frame 65 → 66)

### 闹钟提示音选择 `feature_闹钟提示音选择`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- 闹钟可更换提示音：点击铃声行进入"闹钟提示音"页，在设备提示音列表中选择，返回后生效。
- evidence: step 36 (frame 69 → 70)

### 时钟主页显示时间与下一闹钟 `feature_时钟主页显示时间与下一闹钟`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- 时钟标签页显示当前时间与日期，并显示下一个已启用闹钟的时间。
- evidence: step 41 (frame 79 → 80)

### 更改日期和时间提示 `feature_更改日期和时间提示`
- status: ClaimStatus.CONFIRMED · confidence: 0.70
- 设置中的"更改日期和时间"点击后回到时钟标签页，并在右上角溢出菜单处弹出绿色提示"您可在这里找到隐私权政策"。
- evidence: step 44 (frame 84 → 85)

### 显示含秒数设置 `feature_显示含秒数设置`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- 设置中的"显示含秒数的时间"开关控制时钟页是否显示秒。
- evidence: step 47 (frame 88 → 89)

### 定时器键盘输入 `feature_定时器键盘输入`
- status: ClaimStatus.CONFIRMED · confidence: 0.85
- 定时器用数字键盘输入时长，数字逐位累积填充秒/分/时；时长为零时无开始按钮。
- evidence: step 50 (frame 94 → 95)

### 定时器暂停/继续 `feature_定时器暂停_继续`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- 定时器可通过粉色中央按钮暂停/继续。
- evidence: step 57 (frame 106 → 107)

### 定时器加一分钟 `feature_定时器加一分钟`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- 定时器"+1:00"按钮延长 1 分钟；在超时态使用可将其拉回正计时并继续。
- evidence: step 56 (frame 104 → 105)

### 定时器完成态与超时计数 `feature_定时器完成态与超时计数`
- status: ClaimStatus.CONFIRMED · confidence: 0.85
- 定时器倒计时归零后进入完成态：卡片高亮、数字显示负值继续累加、按钮变为停止。
- evidence: step 56 (frame 104 → 105)

### 定时器重置到完整时长 `feature_定时器重置到完整时长`
- status: ClaimStatus.CONFIRMED · confidence: 0.85
- 定时器卡片内的重置图标把倒计时恢复到完整时长。
- evidence: step 58 (frame 108 → 109)

### 多个定时器 `feature_多个定时器`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- 可添加多个定时器，并列显示各自倒计时与控件。
- evidence: step 62 (frame 115 → 116)

### 关闭定时器 `feature_关闭定时器`
- status: ClaimStatus.CONFIRMED · confidence: 0.85
- 定时器卡片右上角 X 可关闭该定时器。
- evidence: step 63 (frame 117 → 118)

### 闹钟持久化 `feature_闹钟持久化`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- 闹钟与就寝时间设置持久化：重启 App 后闹钟列表与开关状态不变。
- evidence: step 66 (frame 123 → 124)

### 时钟样式数字/指针切换 `feature_时钟样式数字_指针切换`
- status: ClaimStatus.CONFIRMED · confidence: 0.85
- 设置"样式"可切换时钟显示为数字或指针；指针模式显示模拟表盘。
- evidence: step 70 (frame 130 → 131)

### 屏保全屏模式 `feature_屏保全屏模式`
- status: ClaimStatus.CONFIRMED · confidence: 0.80
- 屏保模式：全屏深色仅显示时钟与下一闹钟，从屏幕顶部下滑退出。
- evidence: step 80 (frame 145 → 146)
