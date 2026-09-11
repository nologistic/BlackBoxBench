# Functional Topology — fossify_calculator

- session: `sess_20260908_105009_e75281`
- generated: 2026-09-08T11:19:03.711932+00:00
- coverage: 21 states · 17 features · 3 data · 4 edges (confirmed ratio 100%, 113 actions)

## Graph
```
fossify_calculator
├─ States
│  ├─ [✓] 计算器主界面·初始 (1.00) `state_计算器主界面_初始`
│  ├─ [✓] 计算器主界面·求值结果 (1.00) `state_计算器主界面_求值结果`
│  ├─ [✓] 历史记录对话框·有记录 (1.00) `state_历史记录对话框_有记录`
│  ├─ [✓] 计算器主界面·历史为空 (1.00) `state_计算器主界面_历史为空`
│  ├─ [✓] 单位换算·类别选择 (1.00) `state_单位换算_类别选择`
│  ├─ [✓] 长度换算·默认千米到米 (1.00) `state_长度换算_默认千米到米`
│  ├─ [✓] 长度单位选择对话框 (1.00) `state_长度单位选择对话框`
│  ├─ [✓] 温度换算·摄氏到开尔文 (1.00) `state_温度换算_摄氏到开尔文`
│  ├─ [✓] 温度单位选择对话框 (1.00) `state_温度单位选择对话框`
│  ├─ [✓] 面积换算与单位列表 (1.00) `state_面积换算与单位列表`
│  ├─ [✓] 体积换算与单位列表 (1.00) `state_体积换算与单位列表`
│  ├─ [✓] 质量换算与单位列表 (1.00) `state_质量换算与单位列表`
│  ├─ [✓] 时间换算与单位列表 (1.00) `state_时间换算与单位列表`
│  ├─ [✓] 速度换算与单位列表 (1.00) `state_速度换算与单位列表`
│  ├─ [✓] 压强换算与单位列表 (1.00) `state_压强换算与单位列表`
│  ├─ [✓] 能量换算与单位列表 (1.00) `state_能量换算与单位列表`
│  ├─ [✓] 更多菜单 (1.00) `state_更多菜单`
│  ├─ [✓] 设置页·浅色 (1.00) `state_设置页_浅色`
│  ├─ [✓] 自定义外观页·主题选择 (1.00) `state_自定义外观页_主题选择`
│  ├─ [✓] 深色主题外观预览 (1.00) `state_深色主题外观预览`
│  ├─ [✓] 关于页 (1.00) `state_关于页`
├─ Features
│  ├─ [✓] 基础算术表达式输入与求值 (1.00) `feature_基础算术表达式输入与求值`
│  ├─ [✓] 查看计算历史 (1.00) `feature_查看计算历史`
│  ├─ [✓] 计算历史跨重启持久化 (1.00) `feature_计算历史跨重启持久化`
│  ├─ [✓] 清除全部计算历史 (1.00) `feature_清除全部计算历史`
│  │    ─MUTATES→ 计算历史条目
│  ├─ [✓] 进入单位换算类别页 (1.00) `feature_进入单位换算类别页`
│  ├─ [✓] 长度数值即时换算 (1.00) `feature_长度数值即时换算`
│  ├─ [✓] 交换换算方向 (1.00) `feature_交换换算方向`
│  ├─ [✓] 选择换算单位 (1.00) `feature_选择换算单位`
│  ├─ [✓] 温度负数输入与换算 (1.00) `feature_温度负数输入与换算`
│  ├─ [✓] 选择并保存应用主题 (1.00) `feature_选择并保存应用主题`
│  │    ─MUTATES→ 外观与常规偏好
│  ├─ [✓] 应用主题跨重启持久化 (1.00) `feature_应用主题跨重启持久化`
│  ├─ [✓] 查看关于与版本信息 (1.00) `feature_查看关于与版本信息`
│  ├─ [✓] 百分比参与表达式 (1.00) `feature_百分比参与表达式`
│  ├─ [✓] 清除键短按退格与长按清空 (1.00) `feature_清除键短按退格与长按清空`
│  ├─ [✓] 非法除零表达式保持可编辑 (1.00) `feature_非法除零表达式保持可编辑`
│  ├─ [✓] 小数前导零规范化 (1.00) `feature_小数前导零规范化`
│  ├─ [✓] 幂与平方根运算 (1.00) `feature_幂与平方根运算`
├─ Data
│  ├─ [✓] 计算历史条目 (1.00) `data_计算历史条目`
│  │    ─PERSISTS_TO→ 历史记录对话框·有记录
│  ├─ [✓] 单位类别与单位定义 (1.00) `data_单位类别与单位定义`
│  ├─ [✓] 外观与常规偏好 (1.00) `data_外观与常规偏好`
│  │    ─PERSISTS_TO→ 深色主题外观预览
```

## Features
### 基础算术表达式输入与求值 `feature_基础算术表达式输入与求值`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 依次点击数字和运算符会在结果区组成中缀表达式；点击等号保留表达式并显示计算结果。
- **Preconditions**:
  - 计算器主界面
- evidence: step 1 (frame 1 → 2)

### 查看计算历史 `feature_查看计算历史`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 点击时钟图标弹出历史记录对话框，按表达式和结果两行展示已求值记录。
- **Preconditions**:
  - 至少完成一条合法计算
- evidence: step 5 (frame 7 → 8)

### 计算历史跨重启持久化 `feature_计算历史跨重启持久化`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 完成计算后关闭历史对话框并重启应用，主界面清空为 0，但再次打开历史仍保留先前条目。
- **Preconditions**:
  - 已有历史条目
- evidence: step 7 (frame 10 → 11)

### 清除全部计算历史 `feature_清除全部计算历史`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 历史对话框左下“清除”会立即删除全部条目并关闭对话框；历史为空时点击顶部历史图标不弹出任何界面。
- **Preconditions**:
  - 历史对话框至少包含一条记录
- evidence: step 9 (frame 14 → 15)

### 进入单位换算类别页 `feature_进入单位换算类别页`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 点击计算器顶部尺规图标进入单位换算类别选择页，共九类。
- **Preconditions**:
  - 计算器主界面
- evidence: step 11 (frame 18 → 19)

### 长度数值即时换算 `feature_长度数值即时换算`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 在源值数字键盘输入时，目标值即时更新并按千位分组显示。
- **Preconditions**:
  - 长度换算页默认千米到米
- evidence: step 13 (frame 22 → 23)

### 交换换算方向 `feature_交换换算方向`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 点击中间上下箭头交换源和目标单位，保留源数值并按新方向重新计算。
- **Preconditions**:
  - 长度换算页，源 1 km、目标 1000 m
- evidence: step 14 (frame 24 → 25)

### 选择换算单位 `feature_选择换算单位`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 点击单位名打开可滚动单选列表，选择后立即关闭并按新单位重新计算。
- **Preconditions**:
  - 长度换算页
- evidence: step 15 (frame 26 → 27)

### 温度负数输入与换算 `feature_温度负数输入与换算`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 温度换算专属 +/− 键可切换输入正负；输入 40 后切换为负数，正确得到 -40°C = 233.15K。
- **Preconditions**:
  - 温度换算页默认摄氏到开尔文
- evidence: step 20 (frame 36 → 37)

### 选择并保存应用主题 `feature_选择并保存应用主题`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 在自定义外观中选择深色主题会即时预览；点击右上勾选保存并返回设置页，整个应用切换为深色绿调。
- **Preconditions**:
  - 自定义外观页
  - 主题选择弹窗
- evidence: step 63 (frame 104 → 105)

### 应用主题跨重启持久化 `feature_应用主题跨重启持久化`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 保存深色主题后重启应用，计算器主界面仍以黑底、白字、深绿色按键显示。
- **Preconditions**:
  - 已保存深色主题
- evidence: step 65 (frame 108 → 109)

### 查看关于与版本信息 `feature_查看关于与版本信息`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 更多菜单可进入可滚动关于页，提供支持、分享、社区、政策和版本信息入口。
- **Preconditions**:
  - 计算器主界面
- evidence: step 67 (frame 112 → 113)

### 百分比参与表达式 `feature_百分比参与表达式`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 百分号不能单独把 200% 求值为 2；它可作为乘除等表达式的一部分，例如 100×20% 求值为 20。
- **Preconditions**:
  - 计算器主界面
- evidence: step 100 (frame 155 → 156)

### 清除键短按退格与长按清空 `feature_清除键短按退格与长按清空`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 短按 C 删除表达式最后一个字符；长按 C 清空整个表达式并恢复 0。
- **Preconditions**:
  - 表达式非空
- evidence: step 106 (frame 163 → 164)

### 非法除零表达式保持可编辑 `feature_非法除零表达式保持可编辑`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 输入 1÷0 后点击等号不弹出错误且不产生结果，表达式原样保留；可用 C 退格修改。
- **Preconditions**:
  - 计算器主界面
- evidence: step 105 (frame 161 → 162)

### 小数前导零规范化 `feature_小数前导零规范化`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 空表达式点击小数点会显示 0.，继续输入 5 得到 0.5。
- **Preconditions**:
  - 表达式为空
- evidence: step 108 (frame 166 → 167)

### 幂与平方根运算 `feature_幂与平方根运算`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 幂键作为中缀运算符，例如 2^3 求值为 8；平方根键在空表达式自动插入“1√”，例如 1√9 求值为 3。
- **Preconditions**:
  - 计算器主界面
- evidence: step 80 (frame 132 → 133)
