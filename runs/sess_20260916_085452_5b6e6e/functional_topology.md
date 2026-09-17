# Functional Topology — loop_habit_tracker

- session: `sess_20260916_085452_5b6e6e`
- generated: 2026-09-16T10:48:13.290111+00:00
- coverage: 9 states · 7 features · 2 data · 5 edges (confirmed ratio 100%, 105 actions)

## Graph
```
loop_habit_tracker
├─ States
│  ├─ [✓] 习惯主界面(空状态) (0.95) `state_习惯主界面_空状态`
│  │    ─TRANSITIONS_TO→ 新建习惯表单
│  ├─ [✓] 新建习惯表单 (0.95) `state_新建习惯表单`
│  │    ─ENABLES→ Habit 习惯
│  ├─ [✓] 新建习惯-提醒已设置 (0.90) `state_新建习惯_提醒已设置`
│  ├─ [✓] 习惯列表(含一条习惯) (0.95) `state_习惯列表_含一条习惯`
│  │    ─TRANSITIONS_TO→ 习惯详情页
│  ├─ [✓] 习惯详情页 (0.90) `state_习惯详情页`
│  ├─ [✓] 设置页 (0.90) `state_设置页`
│  ├─ [✓] 习惯多选模式 (0.90) `state_习惯多选模式`
│  ├─ [✓] 新建可量化习惯表单 (0.95) `state_新建可量化习惯表单`
│  ├─ [✓] 可量化习惯详情页 (0.90) `state_可量化习惯详情页`
├─ Features
│  ├─ [✓] 取消打卡并保留备注 (0.50) `feature_取消打卡并保留备注`
│  ├─ [✓] 数据持久化 (0.50) `feature_数据持久化`
│  ├─ [✓] 长按快速打卡 (0.50) `feature_长按快速打卡`
│  ├─ [✓] 习惯过滤与排序 (0.50) `feature_习惯过滤与排序`
│  ├─ [✓] 创建可量化习惯 (0.50) `feature_创建可量化习惯`
│  ├─ [✓] 可量化习惯记录数值 (0.50) `feature_可量化习惯记录数值`
│  ├─ [✓] 可量化目标进度 (0.50) `feature_可量化目标进度`
├─ Data
│  ├─ [✓] Habit 习惯 (0.50) `data_habit_习惯`
│  │    ─REQUIRES→ Checkin 打卡记录
│  ├─ [✓] Checkin 打卡记录 (0.50) `data_checkin_打卡记录`
│  │    ←REQUIRES─ Habit 习惯
│  │    ─PERSISTS_TO→ 习惯详情页
```

## Features
### 取消打卡并保留备注 `feature_取消打卡并保留备注`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 点击已打卡日期会重新打开备注对话框显示已有备注；点 ✗ 取消该日打卡但保留备注(蓝点仍在)；点 ✓ 确认打卡。
- evidence: step 36 (frame 65 → 66)

### 数据持久化 `feature_数据持久化`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 重启 App 后习惯及其每日打卡记录/备注均持久保留。
- evidence: step 37 (frame 67 → 68)

### 长按快速打卡 `feature_长按快速打卡`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 默认设置下，长按某日期格可直接切换该日打卡状态(无需弹窗)；短按则打开备注对话框。
- evidence: step 43 (frame 78 → 79)

### 习惯过滤与排序 `feature_习惯过滤与排序`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 顶部漏斗图标打开过滤菜单：隐藏已存档(默认开)、隐藏已完成、排序(手动/按名称/按颜色/按分数/按状态)。
- evidence: step 73 (frame 129 → 130)

### 创建可量化习惯 `feature_创建可量化习惯`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 新建可量化习惯表单比完成与否多出：单位、目标(数字)、目标类型(至少/至多)。保存后列表行为每日显示数值+单位(默认0)，而非对勾/X。
- evidence: step 92 (frame 163 → 164)

### 可量化习惯记录数值 `feature_可量化习惯记录数值`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 可量化习惯点击某日格弹出数值输入对话框，输入数值并保存后该日显示数值+单位(蓝色表示已打卡)。
- evidence: step 102 (frame 177 → 178)

### 可量化目标进度 `feature_可量化目标进度`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 可量化习惯详情页有"目标"进度区，按今日/周/月/季度/年显示累计值与目标，另含成绩与历史图表。
- evidence: step 105 (frame 183 → 184)
