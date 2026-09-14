# Functional Topology — tasks

- session: `sess_20260912_064710_004d65`
- generated: 2026-09-12T07:14:16.787519+00:00
- coverage: 20 states · 18 features · 4 data · 15 edges (confirmed ratio 100%, 101 actions)

## Graph
```
tasks
├─ States
│  ├─ [✓] 首次启动账户选择 (1.00) `state_首次启动账户选择`
│  ├─ [✓] 系统备份文件选择器 (1.00) `state_系统备份文件选择器`
│  ├─ [✓] 我的任务空状态 (1.00) `state_我的任务空状态`
│  ├─ [✓] 日期时间选择底部面板 (1.00) `state_日期时间选择底部面板`
│  ├─ [✓] 自定义重复周期 (1.00) `state_自定义重复周期`
│  ├─ [✓] 新建本地清单 (1.00) `state_新建本地清单`
│  ├─ [✓] 含任务的我的任务列表 (1.00) `state_含任务的我的任务列表`
│  ├─ [✓] 列表分组与排序面板 (1.00) `state_列表分组与排序面板`
│  ├─ [✓] 侧边导航抽屉 (1.00) `state_侧边导航抽屉`
│  ├─ [✓] 新增过滤器模板菜单 (1.00) `state_新增过滤器模板菜单`
│  ├─ [✓] 自定义过滤器编辑 (1.00) `state_自定义过滤器编辑`
│  ├─ [✓] 自定义过滤器条件菜单 (1.00) `state_自定义过滤器条件菜单`
│  ├─ [✓] 应用设置主页 (1.00) `state_应用设置主页`
│  ├─ [✓] 外观设置 (1.00) `state_外观设置`
│  ├─ [✓] 通知设置 (1.00) `state_通知设置`
│  ├─ [✓] 任务默认值设置 (1.00) `state_任务默认值设置`
│  ├─ [✓] 备份设置 (1.00) `state_备份设置`
│  ├─ [✓] 任务清单显示选项 (1.00) `state_任务清单显示选项`
│  ├─ [✓] 编辑屏幕选项 (1.00) `state_编辑屏幕选项`
│  ├─ [✓] state_任务批量选择操作 (0.98) `state_任务批量选择操作`
├─ Features
│  ├─ [✓] 导入 Tasks.org 备份 (1.00) `feature_导入_tasks_org_备份`
│  ├─ [✓] 离线使用 (1.00) `feature_离线使用`
│  ├─ [✓] 创建并保存任务 (1.00) `feature_创建并保存任务`
│  │    ─MUTATES→ Task
│  ├─ [✓] 创建自定义本地清单 (1.00) `feature_创建自定义本地清单`
│  │    ─MUTATES→ TaskList
│  ├─ [✓] 新建并关联标签 (1.00) `feature_新建并关联标签`
│  │    ─MUTATES→ Tag
│  ├─ [✓] 任务用时计时 (1.00) `feature_任务用时计时`
│  ├─ [✓] feature_长按批量管理任务 (0.97) `feature_长按批量管理任务`
│  ├─ [✓] feature_克隆任务需确认 (0.98) `feature_克隆任务需确认`
│  ├─ [✓] feature_编辑已有任务 (0.99) `feature_编辑已有任务`
│  │    ─MUTATES→ Task
│  ├─ [✓] feature_本地数据持久化 (0.99) `feature_本地数据持久化`
│  │    ─PERSISTS_TO→ Task
│  ├─ [✓] feature_完成与撤销完成 (0.99) `feature_完成与撤销完成`
│  │    ─MUTATES→ Task
│  ├─ [✓] feature_任务搜索与空结果 (0.98) `feature_任务搜索与空结果`
│  │    ─REVEALS→ Task
│  ├─ [✓] feature_任务分组与排序 (0.99) `feature_任务分组与排序`
│  │    ─REVEALS→ Task
│  ├─ [✓] feature_创建自定义过滤器 (0.98) `feature_创建自定义过滤器`
│  │    ─MUTATES→ data_filter
│  ├─ [✓] feature_应用个性化与默认值设置 (0.97) `feature_应用个性化与默认值设置`
│  ├─ [✓] feature_本地备份与导入 (0.98) `feature_本地备份与导入`
│  │    ─PERSISTS_TO→ Task
│  ├─ [✓] feature_侧边栏按标签浏览 (0.99) `feature_侧边栏按标签浏览`
│  │    ─REVEALS→ Tag
│  ├─ [✓] feature_侧边栏按清单浏览 (0.99) `feature_侧边栏按清单浏览`
│  │    ─REVEALS→ TaskList
├─ Data
│  ├─ [✓] Task (1.00) `data_task`
│  │    ─DEPENDS_ON→ TaskList
│  │    ─DEPENDS_ON→ Tag
│  ├─ [✓] TaskList (1.00) `data_tasklist`
│  │    ←DEPENDS_ON─ Task
│  ├─ [✓] Tag (1.00) `data_tag`
│  │    ←DEPENDS_ON─ Task
│  ├─ [✓] data_filter (0.97) `data_filter`
│  │    ─REVEALS→ Task
```

## Features
### 导入 Tasks.org 备份 `feature_导入_tasks_org_备份`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 首次启动页可进入系统文件选择器选择备份文件。
- evidence: step 1 (frame 1 → 2)

### 离线使用 `feature_离线使用`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 无需添加账号即可进入本地任务列表。
- evidence: step 3 (frame 5 → 6)

### 创建并保存任务 `feature_创建并保存任务`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 新增按钮打开任务编辑页，填写后点击保存返回列表并显示任务及元数据。
- evidence: step 46 (frame 86 → 87)

### 创建自定义本地清单 `feature_创建自定义本地清单`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 清单选择器可新建本地清单，设置名称、颜色与图标并用于当前任务。
- evidence: step 28 (frame 54 → 55)

### 新建并关联标签 `feature_新建并关联标签`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 标签选择页支持搜索现有标签和按输入创建新标签，创建后自动勾选并关联任务。
- evidence: step 33 (frame 63 → 64)

### 任务用时计时 `feature_任务用时计时`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 任务编辑页可启动计时，显示累计用时和暂停按钮；暂停后保留累计时间并恢复播放按钮。
- evidence: step 43 (frame 81 → 82)

### feature_长按批量管理任务 `feature_长按批量管理任务`
- status: ClaimStatus.CONFIRMED · confidence: 0.97
- 长按任务进入选择模式，支持批量标签/清单/时间/优先级操作，并可分享、克隆或删除。
- evidence: step 90 (frame 167 → 168)

### feature_克隆任务需确认 `feature_克隆任务需确认`
- status: ClaimStatus.CONFIRMED · confidence: 0.98
- 从任务批量操作菜单选择克隆后，会先弹出确认对话框，取消不产生副本。
- evidence: step 92 (frame 171 → 172)

### feature_编辑已有任务 `feature_编辑已有任务`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 点击任务主体进入完整编辑页面，原有标题、日期、优先级、清单、标签、子任务和描述都会回填；页面顶部可保存或删除。
- evidence: step 95 (frame 176 → 177)

### feature_本地数据持久化 `feature_本地数据持久化`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 重启应用后，离线创建的任务、Garden 清单、outdoor 标签、子任务、日期、优先级与描述均保持。
- evidence: step 97 (frame 179 → 180)

### feature_完成与撤销完成 `feature_完成与撤销完成`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 点击子任务复选框可单独完成；完成父任务后整项移入已完成分组；将父任务恢复为未完成时子任务也恢复未完成。
- evidence: step 47 (frame 88 → 89)

### feature_任务搜索与空结果 `feature_任务搜索与空结果`
- status: ClaimStatus.CONFIRMED · confidence: 0.98
- 底部搜索可按标题即时筛选；匹配时显示任务，不匹配时显示空状态“这里没有任务”。
- evidence: step 53 (frame 99 → 100)

### feature_任务分组与排序 `feature_任务分组与排序`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 任务列表支持按无、截止日期、开始日期、优先级、最近修改、创建时间或清单分组，同时可配置排序、子任务与已完成任务显示方式。
- evidence: step 58 (frame 107 → 108)

### feature_创建自定义过滤器 `feature_创建自定义过滤器`
- status: ClaimStatus.CONFIRMED · confidence: 0.98
- 侧边栏过滤器区可从预设模板或自定义开始；自定义过滤器可设置名称、颜色、图标并组合多种任务条件。
- evidence: step 65 (frame 120 → 121)

### feature_应用个性化与默认值设置 `feature_应用个性化与默认值设置`
- status: ClaimStatus.CONFIRMED · confidence: 0.97
- 设置页集中提供外观/主题/语言、通知与安静时段、新任务默认值、列表显示、编辑字段编排、备份、插件和高级设置。
- evidence: step 72 (frame 133 → 134)

### feature_本地备份与导入 `feature_本地备份与导入`
- status: ClaimStatus.CONFIRMED · confidence: 0.98
- 备份设置支持立即备份、从备份导入和自动备份；首次启动也提供导入 Tasks.org 备份入口，并通过系统文件选择器选择文件。
- evidence: step 1 (frame 1 → 2)

### feature_侧边栏按标签浏览 `feature_侧边栏按标签浏览`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 从侧边栏选择标签后，标题切换为标签名，并仅显示关联该标签的任务；列表内不再重复显示该标签徽标。
- evidence: step 99 (frame 183 → 184)

### feature_侧边栏按清单浏览 `feature_侧边栏按清单浏览`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 从侧边栏选择本地清单后，页面标题和新增按钮采用清单颜色，只显示该清单任务，并省略列表分组标题。
- evidence: step 101 (frame 186 → 187)
