# Functional Topology — game2048

- session: `sess_20260916_085350_9806ba`
- generated: 2026-09-16T11:28:48.590271+00:00
- coverage: 7 states · 14 features · 3 data · 10 edges (confirmed ratio 100%, 124 actions)

## Graph
```
game2048
├─ States
│  ├─ [✓] Game Board (playing) (0.90) `state_game_board_playing`
│  ├─ [✓] 游戏操作列表 (0.95) `state_游戏操作列表`
│  │    ─TRANSITIONS_TO→ 棋盘尺寸选择
│  │    ─TRANSITIONS_TO→ 选择主题
│  │    ─TRANSITIONS_TO→ 历史对局列表
│  ├─ [✓] 选择主题 (0.90) `state_选择主题`
│  │    ─MUTATES→ Game Board (playing)
│  ├─ [✓] 3x3 棋盘（深色主题） (0.90) `state_3x3_棋盘_深色主题`
│  │    ─TRANSITIONS_TO→ 游戏结束覆盖层
│  ├─ [✓] 游戏结束覆盖层 (0.95) `state_游戏结束覆盖层`
│  │    ─TRANSITIONS_TO→ Game Board (playing)
│  ├─ [✓] 历史对局列表 (0.90) `state_历史对局列表`
│  ├─ [✓] 棋盘尺寸选择 (0.90) `state_棋盘尺寸选择`
│  │    ─MUTATES→ Game Board (playing)
├─ Features
│  ├─ [✓] 主题切换（系统/深色/浅色） (0.90) `feature_主题切换_系统_深色_浅色`
│  ├─ [✓] 棋盘尺寸选择（3x3 至 8x8） (0.90) `feature_棋盘尺寸选择_3x3_至_8x8`
│  ├─ [✓] 方块合并与计分 (0.90) `feature_方块合并与计分`
│  │    ─MUTATES→ 当前对局与最高分
│  ├─ [✓] 撤销与重做 (0.90) `feature_撤销与重做`
│  ├─ [✓] 书签保存当前局面 (0.80) `feature_书签保存当前局面`
│  │    ─PERSISTS_TO→ 书签与历史对局
│  ├─ [✓] 再玩一局（重开新局） (0.85) `feature_再玩一局_重开新局`
│  ├─ [✓] 游戏结束判定 (0.90) `feature_游戏结束判定`
│  ├─ [✓] 主题切换不重置棋局 (0.90) `feature_主题切换不重置棋局`
│  ├─ [✓] 历史对局列表 (0.80) `feature_历史对局列表`
│  ├─ [✓] 最高分按棋盘尺寸区分 (0.85) `feature_最高分按棋盘尺寸区分`
│  │    ─DEPENDS_ON→ 主题与棋盘尺寸设置
│  ├─ [✓] 游戏指南弹窗 (0.90) `feature_游戏指南弹窗`
│  ├─ [✓] 载入游戏打开文件选择 (0.80) `feature_载入游戏打开文件选择`
│  ├─ [✓] 刷新重开当前棋局 (0.85) `feature_刷新重开当前棋局`
│  ├─ [✓] 计分与计数面板 (0.85) `feature_计分与计数面板`
├─ Data
│  ├─ [✓] 当前对局与最高分 (0.90) `data_当前对局与最高分`
│  ├─ [✓] 书签与历史对局 (0.80) `data_书签与历史对局`
│  ├─ [✓] 主题与棋盘尺寸设置 (0.90) `data_主题与棋盘尺寸设置`
│  │    ←DEPENDS_ON─ 最高分按棋盘尺寸区分
```

## Features
### 主题切换（系统/深色/浅色） `feature_主题切换_系统_深色_浅色`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- 从游戏操作列表进入选择主题，可选系统配色、深色主题、浅色主题；选择深色主题后界面整体变为深色（背景黑、方块深灰、文字白）。
- evidence: step 12 (frame 24 → 25)

### 棋盘尺寸选择（3x3 至 8x8） `feature_棋盘尺寸选择_3x3_至_8x8`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- 从游戏操作进入棋盘尺寸设置，可选 33、44、55、66、77、88（即 3x3 至 8x8）；选择后立即开始对应尺寸的新棋局。
- evidence: step 15 (frame 30 → 31)

### 方块合并与计分 `feature_方块合并与计分`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- 相同数字方块碰撞后合并为两倍数值（2+2→4），分数按合并后数值增加（+4），最高分同步更新；每次有效滑动后空位生成一个新方块，OE 计数递增（每步 +1）。
- evidence: step 26 (frame 52 → 53)

### 撤销与重做 `feature_撤销与重做`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- 滑动后出现撤销（弯箭头）入口，点按后棋盘、分数、OE 回退到上一步之前的状态；最高分不回退。撤销后出现重做（反向弯箭头）入口，同时一个"2048"样式方块图标出现在按钮区。
- evidence: step 27 (frame 54 → 55)

### 书签保存当前局面 `feature_书签保存当前局面`
- status: ClaimStatus.CONFIRMED · confidence: 0.80
- 游戏界面书签入口点按后图标由空心变为实心，表示把当前局面加入书签。
- evidence: step 30 (frame 60 → 61)

### 再玩一局（重开新局） `feature_再玩一局_重开新局`
- status: ClaimStatus.CONFIRMED · confidence: 0.85
- 游戏操作列表中的"再玩一局"会立即开始新的同尺寸棋局：棋盘清空后出现一个"2"方块，分数归零、OE 回到 1、计时重置；最高分保持不变。
- evidence: step 34 (frame 68 → 69)

### 游戏结束判定 `feature_游戏结束判定`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- 当棋盘填满且不存在可合并的相邻同值方块时游戏结束，棋盘上显示"游戏结束／再玩一局"覆盖文字。
- evidence: step 98 (frame 143 → 144)

### 主题切换不重置棋局 `feature_主题切换不重置棋局`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- 选择主题提供系统配色、深色主题、浅色主题三项，切换后界面配色立即改变，且不重开当前棋局；最高分与棋局状态保持。
- evidence: step 109 (frame 163 → 164)

### 历史对局列表 `feature_历史对局列表`
- status: ClaimStatus.CONFIRMED · confidence: 0.80
- 游戏操作中的 Recent games 打开历史对局列表，按行显示每局的分数、最后修改时间与用时；列表可横向滚动查看更多列。
- evidence: step 112 (frame 168 → 169)

### 最高分按棋盘尺寸区分 `feature_最高分按棋盘尺寸区分`
- status: ClaimStatus.CONFIRMED · confidence: 0.85
- 最高分按棋盘尺寸分别保存：3x3 的最高分为 452，切换回 4x4 后最高分显示 0，切换尺寸会开始对应尺寸的新棋局。
- evidence: step 120 (frame 182 → 183)

### 游戏指南弹窗 `feature_游戏指南弹窗`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- 应用启动或开始新棋局时会先弹出"游戏指南"对话框，内容说明在游戏区域上下左右滑动移动方块、相同数字碰撞合并为两倍、目标是合并出尽可能大的方块、合并出 2048 是胜利的第一步；右上角 X 可关闭。
- evidence: step 23 (frame 46 → 47)

### 载入游戏打开文件选择 `feature_载入游戏打开文件选择`
- status: ClaimStatus.CONFIRMED · confidence: 0.80
- 游戏操作列表中的载入游戏会调到系统文件选择界面选取存档；列表还包含退出应用与分享游戏等条目。
- evidence: step 6 (frame 12 → 13)

### 刷新重开当前棋局 `feature_刷新重开当前棋局`
- status: ClaimStatus.CONFIRMED · confidence: 0.85
- 圆形箭头刷新入口点按后当前棋局被重开（棋盘清空并生成一个新方块、分数与 OE 归零、用时计时重置），最高分保留。
- evidence: step 21 (frame 42 → 43)

### 计分与计数面板 `feature_计分与计数面板`
- status: ClaimStatus.CONFIRMED · confidence: 0.85
- 计分面板包含：分数（当前局得分）、最高（按棋盘尺寸记录的最高分）、OE（随有效滑动递增的计数）、RETRIES（重试次数）、以及格式为 时:分:秒 的用计时计时。
- evidence: step 27 (frame 54 → 55)

## Unresolved questions
- [HypothesisStatus.UNCERTAIN] 顶部第一行左侧的"手柄"图标是 AI/模式开关：点按后图标变为"场记板（影片）"图标，工具栏简化，棋盘下方出现 AI 评估信息行（预计分数区间、搜索节点 N5、耗时 ms）；魔法棒图标会在工具栏加入一个"星形播放"按钮并显示评估信息。 (0.50) — next probe: 点按星形播放按钮观察是否自动落子；观察场记板模式下的界面元素。
