# Functional Topology — fossify_calculator

- session: `sess_20260908_105024_86dbf6`
- generated: 2026-09-08T11:24:01.295696+00:00
- coverage: 19 states · 19 features · 2 data · 28 edges (confirmed ratio 100%, 142 actions)

## Graph
```
fossify_calculator
├─ States
│  ├─ [✓] 基础计算器初始状态 (0.99) `state_基础计算器初始状态`
│  │    ─TRANSITIONS_TO→ 历史记录对话框
│  │    ─TRANSITIONS_TO→ 单位换算分类页
│  │    ─TRANSITIONS_TO→ 更多菜单
│  ├─ [✓] 历史记录对话框 (0.99) `state_历史记录对话框`
│  ├─ [✓] 单位换算分类页 (0.99) `state_单位换算分类页`
│  │    ─TRANSITIONS_TO→ 长度换算页
│  │    ─TRANSITIONS_TO→ 面积换算页
│  │    ─TRANSITIONS_TO→ 体积换算页
│  │    ─TRANSITIONS_TO→ 质量换算页
│  │    ─TRANSITIONS_TO→ 温度换算页
│  │    ─TRANSITIONS_TO→ 时间换算页
│  │    ─TRANSITIONS_TO→ 速度换算页
│  │    ─TRANSITIONS_TO→ 压强换算页
│  │    ─TRANSITIONS_TO→ 能量换算页
│  ├─ [✓] 长度换算页 (0.99) `state_长度换算页`
│  │    ─TRANSITIONS_TO→ 长度单位选择对话框
│  ├─ [✓] 长度单位选择对话框 (0.99) `state_长度单位选择对话框`
│  ├─ [✓] 面积换算页 (0.99) `state_面积换算页`
│  ├─ [✓] 体积换算页 (0.99) `state_体积换算页`
│  ├─ [✓] 质量换算页 (0.99) `state_质量换算页`
│  ├─ [✓] 温度换算页 (0.99) `state_温度换算页`
│  ├─ [✓] 时间换算页 (0.99) `state_时间换算页`
│  ├─ [✓] 速度换算页 (0.99) `state_速度换算页`
│  ├─ [✓] 压强换算页 (0.99) `state_压强换算页`
│  ├─ [✓] 能量换算页 (0.99) `state_能量换算页`
│  ├─ [✓] 更多菜单 (0.99) `state_更多菜单`
│  │    ─TRANSITIONS_TO→ 关于页
│  │    ─TRANSITIONS_TO→ 设置页
│  ├─ [✓] 设置页 (0.99) `state_设置页`
│  │    ─TRANSITIONS_TO→ 自定义外观页
│  │    ─TRANSITIONS_TO→ 自定义微件颜色页
│  ├─ [✓] 自定义外观页 (0.99) `state_自定义外观页`
│  ├─ [✓] 自定义微件颜色页 (0.98) `state_自定义微件颜色页`
│  ├─ [✓] 关于页 (0.99) `state_关于页`
│  ├─ [✓] 基础计算器深色主题 (0.99) `state_基础计算器深色主题`
│  │    ─TRANSITIONS_TO→ 历史记录对话框
├─ Features
│  ├─ [✓] 基础算术求值 (0.99) `feature_基础算术求值`
│  │    ─MUTATES→ 计算历史
│  ├─ [✓] 查看计算历史 (0.99) `feature_查看计算历史`
│  ├─ [✓] 计算历史跨重启保留 (0.99) `feature_计算历史跨重启保留`
│  ├─ [✓] 清除计算历史 (0.99) `feature_清除计算历史`
│  │    ─MUTATES→ 计算历史
│  ├─ [✓] 选择单位换算类别 (0.99) `feature_选择单位换算类别`
│  ├─ [✓] 实时长度换算 (0.99) `feature_实时长度换算`
│  ├─ [✓] 交换换算单位 (0.99) `feature_交换换算单位`
│  │    ─MUTATES→ 实时长度换算
│  ├─ [✓] 选择长度单位 (0.99) `feature_选择长度单位`
│  ├─ [✓] 九类单位的即时双向换算 (0.99) `feature_九类单位的即时双向换算`
│  ├─ [✓] 温度正负号切换 (0.99) `feature_温度正负号切换`
│  ├─ [✓] 保存并持久化应用主题 (0.99) `feature_保存并持久化应用主题`
│  │    ─MUTATES→ 外观与常规偏好
│  ├─ [✓] 配置计算器微件外观 (0.98) `feature_配置计算器微件外观`
│  ├─ [✓] 查看关于与支持入口 (0.99) `feature_查看关于与支持入口`
│  ├─ [✓] 幂与平方根计算 (0.99) `feature_幂与平方根计算`
│  ├─ [✓] 相对百分比计算 (0.99) `feature_相对百分比计算`
│  ├─ [✓] 逐段即时运算与运算符替换 (0.99) `feature_逐段即时运算与运算符替换`
│  │    ─MUTATES→ 计算历史
│  ├─ [✓] 清除键短按删除长按清空 (0.99) `feature_清除键短按删除长按清空`
│  ├─ [✓] 输入边界与无效表达式保护 (0.99) `feature_输入边界与无效表达式保护`
│  │    ─VALIDATES→ 计算历史
│  ├─ [✓] 从历史恢复结果 (0.99) `feature_从历史恢复结果`
│  │    ─DEPENDS_ON→ 计算历史
├─ Data
│  ├─ [✓] 计算历史 (0.90) `data_计算历史`
│  │    ─REVEALS→ 历史记录对话框
│  │    ─PERSISTS_TO→ 基础计算器初始状态
│  │    ←DEPENDS_ON─ 从历史恢复结果
│  ├─ [✓] 外观与常规偏好 (0.99) `data_外观与常规偏好`
│  │    ─PERSISTS_TO→ 基础计算器初始状态
```

## Features
### 基础算术求值 `feature_基础算术求值`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 在基础计算器中通过数字键与运算符输入表达式，按等号后同时显示表达式与结果；实测 12+7 得到 19。
- evidence: step 7 (frame 8 → 9)

### 查看计算历史 `feature_查看计算历史`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 点按历史图标打开历史记录对话框，可查看之前完成的表达式及结果；对话框提供清除和确定按钮。
- evidence: step 8 (frame 10 → 11)

### 计算历史跨重启保留 `feature_计算历史跨重启保留`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 完成计算后关闭历史对话框并重启应用，主界面恢复为 0；再次打开历史仍可看到此前的 12+7 与结果 19。
- evidence: step 8 (frame 10 → 11)

### 清除计算历史 `feature_清除计算历史`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 在历史记录对话框点按“清除”会立即删除所有历史并关闭对话框；之后点按历史图标不再弹出历史窗口。
- evidence: step 13 (frame 18 → 19)

### 选择单位换算类别 `feature_选择单位换算类别`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 从基础计算器进入单位换算分类页，可选择九种测量类别。
- evidence: step 15 (frame 22 → 23)

### 实时长度换算 `feature_实时长度换算`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 在活动输入区输入数字会立即更新另一单位的换算值；实测 1.5 千米实时显示为 1,500 米。
- evidence: step 19 (frame 28 → 29)

### 交换换算单位 `feature_交换换算单位`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 点按中央双向箭头交换上下单位并保留输入数字；实测 1.5 km / 1,500 m 交换为 1.5 m / 0.0015 km。
- evidence: step 20 (frame 30 → 31)

### 选择长度单位 `feature_选择长度单位`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 点按长度单位选择器打开可滚动单选列表，选择任一可用单位后用于换算。
- evidence: step 21 (frame 32 → 33)

### 九类单位的即时双向换算 `feature_九类单位的即时双向换算`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 长度、面积、体积、质量、温度、时间、速度、压强和能量均使用一致的双面板即时换算界面；各分类提供对应单位单选列表和中央交换按钮。
- evidence: step 25 (frame 39 → 40)

### 温度正负号切换 `feature_温度正负号切换`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 温度换算键盘底部提供 +/− 键，可切换当前输入的符号并立即重算；实测输入 40 后切换为 -40°C，结果为 233.15 K。
- evidence: step 41 (frame 63 → 64)

### 保存并持久化应用主题 `feature_保存并持久化应用主题`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 自定义外观中选择深色主题后界面即时预览深色与绿色主色；点按右上勾保存，返回主界面和重启应用后仍保持深色。
- evidence: step 67 (frame 104 → 105)

### 配置计算器微件外观 `feature_配置计算器微件外观`
- status: ClaimStatus.CONFIRMED · confidence: 0.98
- 从设置进入微件颜色页，可在桌面背景预览中调整颜色与不透明度并点按确定保存。
- evidence: step 69 (frame 108 → 109)

### 查看关于与支持入口 `feature_查看关于与支持入口`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 关于页集中提供支持、分享、社区、政策和第三方许可入口，并显示版本 1.4.0。
- evidence: step 75 (frame 114 → 115)

### 幂与平方根计算 `feature_幂与平方根计算`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 幂运算按 a^b 输入并求值；实测 2^8=256。平方根键在空表达式下形成 1√n，实测 1√9=3；数字后直接插入根号会作为隐式乘法，如 9√9=27。
- evidence: step 89 (frame 136 → 137)

### 相对百分比计算 `feature_相对百分比计算`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 百分号作为相对上一操作数的比例使用。单独 50% 按等号不会求值；实测 200+10%=220。
- evidence: step 103 (frame 155 → 156)

### 逐段即时运算与运算符替换 `feature_逐段即时运算与运算符替换`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 连续输入新运算符会先计算已有二元表达式并以结果继续，因此 2+3×4 先生成历史 2+3=5，再计算 5×4=20。若连续点按两个二元运算符，后者替换前者；实测 5+×2 变为 5×2=10。
- evidence: step 128 (frame 184 → 185)

### 清除键短按删除长按清空 `feature_清除键短按删除长按清空`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 短按 C 删除当前输入末位，实测 123 变为 12；长按 C 清空完整表达式和结果并恢复 0。
- evidence: step 140 (frame 199 → 200)

### 输入边界与无效表达式保护 `feature_输入边界与无效表达式保护`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 同一数字段第二次输入小数点会被忽略（1..2 显示 1.2）；除以零的 5÷0 按等号后不产生结果且不进入历史。
- evidence: step 121 (frame 176 → 177)

### 从历史恢复结果 `feature_从历史恢复结果`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 历史对话框按最近记录在上显示多条表达式与结果；点按某条历史项关闭对话框，并把该条结果恢复为当前输入值。
- evidence: step 142 (frame 203 → 204)
