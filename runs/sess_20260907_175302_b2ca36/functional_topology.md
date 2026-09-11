# Functional Topology — google_clock

- session: `sess_20260907_175302_b2ca36`
- generated: 2026-09-07T18:25:30.529017+00:00
- coverage: 18 states · 15 features · 5 data · 26 edges (confirmed ratio 100%, 70 actions)

## Graph
```
google_clock
├─ States
│  ├─ [✓] 秒表初始 (0.99) `state_秒表初始`
│  │    ─ENABLES→ 秒表计时与计次
│  ├─ [✓] 秒表运行与计次 (0.99) `state_秒表运行与计次`
│  ├─ [✓] 就寝时间引导 (0.99) `state_就寝时间引导`
│  │    ─ENABLES→ 配置就寝和起床日程
│  ├─ [✓] 就寝时间日程设置向导 (0.99) `state_就寝时间日程设置向导`
│  ├─ [✓] 就寝时间主页 (0.99) `state_就寝时间主页`
│  ├─ [✓] 全屏助眠音效播放器 (0.99) `state_全屏助眠音效播放器`
│  ├─ [✓] 更多菜单 (0.99) `state_更多菜单`
│  ├─ [✓] 设置页 (0.99) `state_设置页`
│  ├─ [✓] 屏保 (0.99) `state_屏保`
│  ├─ [✓] 含就寝起床闹钟的闹钟列表 (0.50) `state_含就寝起床闹钟的闹钟列表`
│  ├─ [✓] 世界时钟主页 (0.50) `state_世界时钟主页`
│  │    ─ENABLES→ 搜索并添加世界时钟城市
│  ├─ [✓] 闹钟列表 (0.50) `state_闹钟列表`
│  │    ─ENABLES→ 新建闹钟
│  │    ─TRANSITIONS_TO→ 定时器数字输入
│  ├─ [✓] 定时器数字输入 (0.50) `state_定时器数字输入`
│  │    ─ENABLES→ 创建运行并处理到时计时器
│  ├─ [✓] 定时器到时提醒 (0.50) `state_定时器到时提醒`
│  ├─ [✓] 城市搜索 (0.50) `state_城市搜索`
│  ├─ [✓] 城市搜索离线提示 (0.50) `state_城市搜索离线提示`
│  ├─ [✓] 闹钟展开设置 (0.50) `state_闹钟展开设置`
│  ├─ [✓] 含自定义闹钟的闹钟列表 (0.50) `state_含自定义闹钟的闹钟列表`
├─ Features
│  ├─ [✓] 秒表计时与计次 (0.99) `feature_秒表计时与计次`
│  │    ─TRANSITIONS_TO→ 秒表运行与计次
│  │    ─MUTATES→ StopwatchSession
│  ├─ [✓] 暂停恢复和重置秒表 (0.99) `feature_暂停恢复和重置秒表`
│  │    ─MUTATES→ StopwatchSession
│  ├─ [✓] 配置就寝和起床日程 (0.99) `feature_配置就寝和起床日程`
│  │    ─MUTATES→ SleepSchedule
│  │    ─TRANSITIONS_TO→ 就寝时间主页
│  ├─ [✓] 设置就寝提醒通知 (0.99) `feature_设置就寝提醒通知`
│  │    ─MUTATES→ SleepSchedule
│  ├─ [✓] 选择并播放助眠音效 (0.99) `feature_选择并播放助眠音效`
│  │    ─REVEALS→ 全屏助眠音效播放器
│  ├─ [✓] 自定义时钟样式和秒数 (0.99) `feature_自定义时钟样式和秒数`
│  │    ─MUTATES→ AppSettings
│  ├─ [✓] 配置闹钟全局行为 (0.99) `feature_配置闹钟全局行为`
│  │    ─MUTATES→ AppSettings
│  ├─ [✓] 打开屏保 (0.99) `feature_打开屏保`
│  │    ─TRANSITIONS_TO→ 屏保
│  ├─ [✓] 就寝时间设置重启持久化 (0.50) `feature_就寝时间设置重启持久化`
│  │    ─PERSISTS_TO→ 含就寝起床闹钟的闹钟列表
│  ├─ [✓] 底部导航切换五个模块 (0.50) `feature_底部导航切换五个模块`
│  │    ─REVEALS→ 世界时钟主页
│  │    ─REVEALS→ 闹钟列表
│  │    ─REVEALS→ 定时器数字输入
│  ├─ [✓] 创建运行并处理到时计时器 (0.99) `feature_创建运行并处理到时计时器`
│  │    ─MUTATES→ Timer
│  ├─ [✓] 搜索并添加世界时钟城市 (0.50) `feature_搜索并添加世界时钟城市`
│  │    ─TRANSITIONS_TO→ 城市搜索离线提示
│  ├─ [✓] 新建闹钟 (0.50) `feature_新建闹钟`
│  │    ─MUTATES→ Alarm
│  │    ─TRANSITIONS_TO→ 闹钟展开设置
│  ├─ [✓] 编辑闹钟标签 (0.50) `feature_编辑闹钟标签`
│  │    ─MUTATES→ Alarm
│  ├─ [✓] 闹钟写入重启持久化 (1.00) `feature_闹钟写入重启持久化`
│  │    ─PERSISTS_TO→ 含自定义闹钟的闹钟列表
├─ Data
│  ├─ [✓] StopwatchSession (0.99) `data_stopwatchsession`
│  ├─ [✓] SleepSchedule (0.99) `data_sleepschedule`
│  ├─ [✓] AppSettings (0.99) `data_appsettings`
│  ├─ [✓] Timer (0.99) `data_timer`
│  ├─ [✓] Alarm (0.99) `data_alarm`
```

## Features
### 秒表计时与计次 `feature_秒表计时与计次`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 开始后持续显示秒与百分秒；点击计次会在圆环下方追加序号、单圈时间与累计时间记录。
- evidence: step 4 (frame 5 → 6)

### 暂停恢复和重置秒表 `feature_暂停恢复和重置秒表`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 运行秒表可暂停，暂停后显示播放按钮并保留计次列表；恢复后继续累计。暂停后点击重置清零时间并移除计次记录。
- evidence: step 6 (frame 9 → 10)

### 配置就寝和起床日程 `feature_配置就寝和起床日程`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 通过两步向导设置起床与就寝时间、星期及相关选项。加减按钮每次调整 15 分钟；完成后主页显示日程、睡眠时长和下次起床日。
- evidence: step 12 (frame 19 → 20)

### 设置就寝提醒通知 `feature_设置就寝提醒通知`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 点击提醒通知打开单选列表，可选就寝前 15、30、45 分钟、1 小时或关闭。探索中从 15 分钟改为 30 分钟。
- evidence: step 14 (frame 23 → 24)

### 选择并播放助眠音效 `feature_选择并播放助眠音效`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 助眠音效页提供静音、海浪、深邃的太空、沉思及 YouTube Music；选择后主页显示音效卡片。播放器支持动画、播放/暂停，并可设 10/20/30/40/50 分钟或1小时后停止。
- evidence: step 24 (frame 41 → 42)

### 自定义时钟样式和秒数 `feature_自定义时钟样式和秒数`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 设置中可在数字与指针样式间切换，并开关秒数。探索中切换为指针并开启秒数；重启后世界时钟显示指针表盘与秒点，设置已持久化。
- evidence: step 35 (frame 61 → 62)

### 配置闹钟全局行为 `feature_配置闹钟全局行为`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 闹铃时长可选 1/5/10/15/20/25 分钟或永不；延后时长可按分钟选择。探索中将闹铃时长改为5分钟、延后改为12分钟；另有音量、渐强、音量键和一周首日选项。
- evidence: step 37 (frame 63 → 64)

### 打开屏保 `feature_打开屏保`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 更多菜单的屏保进入极暗全屏时钟，显示日期及下一次闹钟；返回键退出。
- evidence: step 46 (frame 77 → 78)

### 就寝时间设置重启持久化 `feature_就寝时间设置重启持久化`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 完成就寝时间向导后，起床闹钟与时钟页下一次闹钟提示在应用重启后保持不变。
- evidence: step 49 (frame 82 → 83)

### 底部导航切换五个模块 `feature_底部导航切换五个模块`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 底部固定导航可在闹钟、时钟、定时器、秒表、就寝时间五个一级模块间切换，并高亮当前模块。
- evidence: step 10 (frame 15 → 16)

### 创建运行并处理到时计时器 `feature_创建运行并处理到时计时器`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 输入 10 秒后启动，计时到零后以高亮卡片和负秒数持续提醒，可加一分钟、停止或关闭。
- evidence: step 55 (frame 94 → 95)

### 搜索并添加世界时钟城市 `feature_搜索并添加世界时钟城市`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 点击时钟页加号进入城市搜索，输入城市名；联网时可查看更多结果，离线时显示明确限制提示。
- evidence: step 58 (frame 98 → 99)

### 新建闹钟 `feature_新建闹钟`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 点击闹钟页加号打开表盘时间选择，确认后立即新增并展开启用的闹钟卡片。
- evidence: step 65 (frame 108 → 109)

### 编辑闹钟标签 `feature_编辑闹钟标签`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 展开闹钟后点击标签，输入 Meeting 并确认，卡片标题即时更新。
- evidence: step 67 (frame 112 → 113)

### 闹钟写入重启持久化 `feature_闹钟写入重启持久化`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 新建并命名 Meeting 19:00 闹钟后重启应用，闹钟、标签和启用状态保持。
- evidence: step 70 (frame 117 → 118)
