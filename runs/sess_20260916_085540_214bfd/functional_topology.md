# Functional Topology — futoshiki

- session: `sess_20260916_085540_214bfd`
- generated: 2026-09-16T10:38:49.545060+00:00
- coverage: 9 states · 9 features · 2 data · 7 edges (confirmed ratio 100%, 77 actions)

## Graph
```
futoshiki
├─ States
│  ├─ [✓] home_menu (0.95) `state_home_menu`
│  │    ─TRANSITIONS_TO→ new_game_dialog
│  │    ─TRANSITIONS_TO→ help_page
│  │    ─TRANSITIONS_TO→ themes_page
│  ├─ [✓] help_page (0.90) `state_help_page`
│  ├─ [✓] help_core_rules (0.90) `state_help_core_rules`
│  ├─ [✓] themes_page (0.95) `state_themes_page`
│  │    ─MUTATES→ Settings
│  ├─ [✓] new_game_dialog (0.95) `state_new_game_dialog`
│  │    ─TRANSITIONS_TO→ game_board_4x4
│  ├─ [✓] game_board_4x4 (0.90) `state_game_board_4x4`
│  │    ─TRANSITIONS_TO→ paused_overlay
│  │    ─TRANSITIONS_TO→ victory_screen
│  ├─ [✓] paused_overlay (0.90) `state_paused_overlay`
│  ├─ [✓] victory_screen (0.95) `state_victory_screen`
│  ├─ [✓] game_board_5x5_medium (0.90) `state_game_board_5x5_medium`
├─ Features
│  ├─ [✓] 主题与明暗设置 (0.90) `feature_主题与明暗设置`
│  ├─ [✓] 重复数字冲突标红 (0.85) `feature_重复数字冲突标红`
│  ├─ [✓] 长按清除格子数字 (0.85) `feature_长按清除格子数字`
│  ├─ [✓] 暂停覆盖层冻结计时器 (0.90) `feature_暂停覆盖层冻结计时器`
│  ├─ [✓] 解出棋盘进入胜利页 (0.95) `feature_解出棋盘进入胜利页`
│  ├─ [✓] RESET 重置棋盘 (0.90) `feature_reset_重置棋盘`
│  ├─ [✓] 进行中的棋局不持久化 (0.80) `feature_进行中的棋局不持久化`
│  ├─ [✓] 不等式违规标红 (0.85) `feature_不等式违规标红`
│  ├─ [✓] 预填格不可编辑 (0.80) `feature_预填格不可编辑`
├─ Data
│  ├─ [✓] Settings (0.85) `data_settings`
│  ├─ [✓] Puzzle (0.85) `data_puzzle`
```

## Features
### 主题与明暗设置 `feature_主题与明暗设置`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- 主题页可切换 MONO/TINTED 配色与 AUTO/DAY/NIGHT 明暗三档（点标签或圆点切换，星形指针指示当前档）。选择立即全局生效并在重启后保留。
- evidence: step 8 (frame 15 → 16)

### 重复数字冲突标红 `feature_重复数字冲突标红`
- status: ClaimStatus.CONFIRMED · confidence: 0.85
- 填入与同行或同列重复的数字时，允许填入但把冲突的重复数字标红（含已放置的旧数字与新数字，如 (1,2)=4 与同行 (1,4)=4、同列 (4,2)=4 均变红）。
- evidence: step 21 (frame 41 → 42)

### 长按清除格子数字 `feature_长按清除格子数字`
- status: ClaimStatus.CONFIRMED · confidence: 0.85
- 长按已填写的格子可清除该格数字（恢复空白）。
- evidence: step 22 (frame 43 → 44)

### 暂停覆盖层冻结计时器 `feature_暂停覆盖层冻结计时器`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- 暂停按钮弹出 PAUSED 覆盖层，计时器冻结（等待 4 秒后仍停在 08:49），提供 NEW GAME/RESUME/HELP/MAIN MENU。
- evidence: step 23 (frame 45 → 46)

### 解出棋盘进入胜利页 `feature_解出棋盘进入胜利页`
- status: ClaimStatus.CONFIRMED · confidence: 0.95
- 正确填满棋盘（每行每列 1-4 唯一且满足所有不等式）后自动进入胜利页，显示用时并统计在暂停菜单中花费的时间。
- evidence: step 40 (frame 68 → 69)

### RESET 重置棋盘 `feature_reset_重置棋盘`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- RESET 按钮清除玩家填入的数字，恢复到题目预填状态；已用计时不重置。长按可单格清除，RESET 可整体清除。
- evidence: step 48 (frame 80 → 81)

### 进行中的棋局不持久化 `feature_进行中的棋局不持久化`
- status: ClaimStatus.CONFIRMED · confidence: 0.80
- 游戏进行中重启 App 会回到主菜单，未完成的棋局不恢复，主菜单也没有“继续游戏”入口；仅主题等设置持久化。
- evidence: step 51 (frame 84 → 85)

### 不等式违规标红 `feature_不等式违规标红`
- status: ClaimStatus.CONFIRMED · confidence: 0.85
- 填入使相邻不等式不成立的数字时（如 (6,4)=2 > (6,5)=5 实际为 2<5），相临两格数字与边框都变红提示冲突。
- evidence: step 71 (frame 118 → 119)

### 预填格不可编辑 `feature_预填格不可编辑`
- status: ClaimStatus.CONFIRMED · confidence: 0.80
- 题目预填（给定）的数字格不能选中或修改，只有空白格可以选中填数。
- evidence: step 72 (frame 120 → 121)

## Unresolved questions
- [HypothesisStatus.UNCERTAIN] 胜利页右上 SHARE 按钮点击后可能失败（离线无分享目标）并返回主菜单，或弹出系统分享面板。 (0.40) — next probe: 再次通关后在胜利页点 SHARE，立即 observe 确认是系统分享面板还是回到主菜单。
