# Functional Topology — google_clock

- session: `sess_20260907_173041_8219a8`
- generated: 2026-09-07T17:50:15.135079+00:00
- coverage: 6 states · 14 features · 3 data · 1 edges (confirmed ratio 100%, 40 actions)

## Graph
```
google_clock
├─ States
│  ├─ [✓] 世界时钟首页 (1.00) `state_世界时钟首页`
│  ├─ [✓] 设置首页 (1.00) `state_设置首页`
│  ├─ [✓] 离线城市搜索无结果 (1.00) `state_离线城市搜索无结果`
│  ├─ [✓] 闹钟列表 (1.00) `state_闹钟列表`
│  ├─ [✓] 定时器运行卡片 (1.00) `state_定时器运行卡片`
│  ├─ [✓] 秒表初始与运行 (1.00) `state_秒表初始与运行`
├─ Features
│  ├─ [✓] 切换时钟样式 (1.00) `feature_切换时钟样式`
│  ├─ [✓] 编辑闹钟标签 (1.00) `feature_编辑闹钟标签`
│  ├─ [✓] 设置闹钟重复星期 (1.00) `feature_设置闹钟重复星期`
│  ├─ [✓] 启用或停用闹钟 (1.00) `feature_启用或停用闹钟`
│  ├─ [✓] 新增闹钟 (1.00) `feature_新增闹钟`
│  │    ─MUTATES→ 闹钟条目
│  ├─ [✓] 闹钟状态持久化 (1.00) `feature_闹钟状态持久化`
│  ├─ [✓] 删除闹钟 (1.00) `feature_删除闹钟`
│  ├─ [✓] 创建并启动定时器 (1.00) `feature_创建并启动定时器`
│  ├─ [✓] 定时器加一分钟 (1.00) `feature_定时器加一分钟`
│  ├─ [✓] 暂停与继续定时器 (1.00) `feature_暂停与继续定时器`
│  ├─ [✓] 重置定时器 (1.00) `feature_重置定时器`
│  ├─ [✓] 编辑定时器标签 (1.00) `feature_编辑定时器标签`
│  ├─ [✓] 移除定时器 (1.00) `feature_移除定时器`
│  ├─ [✓] 启动秒表 (1.00) `feature_启动秒表`
├─ Data
│  ├─ [✓] 闹钟条目 (1.00) `data_闹钟条目`
│  ├─ [✓] 定时器条目 (1.00) `data_定时器条目`
│  ├─ [✓] 秒表会话 (0.90) `data_秒表会话`
```

## Features
### 切换时钟样式 `feature_切换时钟样式`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 设置中的样式可在数字与指针之间切换，选择后设置摘要立即更新。
- evidence: step 6 (frame 10 → 11)

### 编辑闹钟标签 `feature_编辑闹钟标签`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 展开闹钟卡片，点“添加标签”，输入文本并确认后，标签立即显示在卡片顶部。
- evidence: step 18 (frame 30 → 31)

### 设置闹钟重复星期 `feature_设置闹钟重复星期`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 展开的闹钟卡片以周一到周日圆形按钮控制重复日；轻触某日会即时切换选中状态并更新星期摘要。
- evidence: step 19 (frame 32 → 33)

### 启用或停用闹钟 `feature_启用或停用闹钟`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 每个闹钟卡片右侧开关可立即切换启用状态；启用时卡片时间、星期和开关从灰色变为高亮。
- evidence: step 20 (frame 34 → 35)

### 新增闹钟 `feature_新增闹钟`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 点加号打开时间选择器，可在表盘和数字输入模式间切换；确认后新增一个默认启用并展开的闹钟卡片。当天一次性闹钟显示“今天”。
- evidence: step 24 (frame 42 → 43)

### 闹钟状态持久化 `feature_闹钟状态持久化`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 重启应用后，已编辑的标签、重复星期、启用状态以及新增闹钟均保持不变。
- evidence: step 25 (frame 44 → 45)

### 删除闹钟 `feature_删除闹钟`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 展开闹钟卡片并轻触“删除”后，该条目立即从列表移除，不出现二次确认。
- evidence: step 27 (frame 48 → 49)

### 创建并启动定时器 `feature_创建并启动定时器`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 使用屏幕数字键盘输入时分秒，输入非零后出现开始按钮；启动后转为含环形进度的卡片并倒计时，结束后继续显示负数超时。
- evidence: step 30 (frame 54 → 55)

### 定时器加一分钟 `feature_定时器加一分钟`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 运行或超时中的定时器可点“+1:00”增加一分钟；超时负数会转回正数并继续运行。
- evidence: step 31 (frame 56 → 57)

### 暂停与继续定时器 `feature_暂停与继续定时器`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 运行中点暂停后剩余时间停止且按钮变为继续；再次点击可恢复。
- evidence: step 32 (frame 58 → 59)

### 重置定时器 `feature_重置定时器`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 暂停定时器后轻触中央重置图标，剩余时间恢复为原始设定值并保持待启动。
- evidence: step 33 (frame 60 → 61)

### 编辑定时器标签 `feature_编辑定时器标签`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 轻触定时器名称可打开标签输入框；输入并确认后卡片标题立即更新。
- evidence: step 36 (frame 65 → 66)

### 移除定时器 `feature_移除定时器`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 定时器卡片右上角关闭按钮会立即移除该卡片；当无条目时恢复数字输入界面。
- evidence: step 37 (frame 67 → 68)

### 启动秒表 `feature_启动秒表`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 初始秒表点开始后持续计时，界面由单个开始按钮切换为重置、暂停和圈次三个控制。
- evidence: step 39 (frame 71 → 72)
