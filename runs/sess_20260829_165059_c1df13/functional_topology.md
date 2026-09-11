# Functional Topology — yuque_web

- session: `sess_20260829_165059_c1df13`
- generated: 2026-08-29T17:16:14.898262+00:00
- coverage: 52 states · 28 features · 5 data · 52 edges (confirmed ratio 100%, 97 actions)

## Graph
```
yuque_web
├─ States
│  ├─ [✓] 开始页 (1.00) `state_unnamed`
│  │    ─TRANSITIONS_TO→ 开始页·我点赞的
│  │    ─REVEALS→ Global quick search
│  │    ─REVEALS→ Account menu
│  │    ─REVEALS→ More menu
│  │    ─REVEALS→ Message center unread
│  │    ─REVEALS→ Create content menu
│  │    ─REVEALS→ Template center recommended
│  ├─ [✓] 开始页·我点赞的 (1.00) `state_unnamed_2`
│  │    ─TRANSITIONS_TO→ 开始页·我评论过空状态
│  ├─ [✓] 开始页·我评论过空状态 (1.00) `state_unnamed_3`
│  │    ─REVEALS→ 开始页·类型筛选菜单
│  ├─ [✓] 开始页·类型筛选菜单 (1.00) `state_unnamed_4`
│  │    ─TRANSITIONS_TO→ 开始页·表格类型空结果
│  ├─ [✓] 开始页·表格类型空结果 (1.00) `state_unnamed_5`
│  │    ─TRANSITIONS_TO→ 逛逛·推荐
│  ├─ [✓] 逛逛·推荐 (1.00) `state_unnamed_6`
│  │    ─TRANSITIONS_TO→ 逛逛·关注空状态
│  ├─ [✓] 逛逛·关注空状态 (1.00) `state_unnamed_7`
│  │    ─TRANSITIONS_TO→ 收藏列表
│  ├─ [✓] 收藏列表 (1.00) `state_unnamed_8`
│  │    ─REVEALS→ 收藏搜索输入态
│  │    ─REVEALS→ 收藏类型筛选菜单
│  ├─ [✓] 收藏搜索输入态 (1.00) `state_unnamed_9`
│  │    ─TRANSITIONS_TO→ 收藏搜索空结果
│  ├─ [✓] 收藏搜索空结果 (1.00) `state_unnamed_10`
│  ├─ [✓] 收藏类型筛选菜单 (1.00) `state_unnamed_11`
│  │    ─TRANSITIONS_TO→ 收藏·资源类型空状态
│  ├─ [✓] 收藏·资源类型空状态 (1.00) `state_unnamed_12`
│  │    ─TRANSITIONS_TO→ 小记阅读与管理页
│  ├─ [✓] 小记阅读与管理页 (1.00) `state_unnamed_13`
│  │    ─TRANSITIONS_TO→ 小记搜索空结果
│  │    ─TRANSITIONS_TO→ 小记卡片视图
│  ├─ [✓] 小记搜索空结果 (1.00) `state_unnamed_14`
│  ├─ [✓] 小记卡片视图 (1.00) `state_unnamed_15`
│  │    ─TRANSITIONS_TO→ AI 写作首页
│  ├─ [✓] AI 写作首页 (1.00) `state_ai`
│  │    ─REVEALS→ AI 写作任务列表抽屉
│  ├─ [✓] AI 写作任务列表抽屉 (1.00) `state_ai_2`
│  │    ─TRANSITIONS_TO→ 知识库首页
│  ├─ [✓] 知识库首页 (1.00) `state_unnamed_16`
│  │    ─REVEALS→ 知识库文档视图与排序菜单
│  │    ─REVEALS→ 知识库搜索浮层
│  ├─ [✓] 知识库文档视图与排序菜单 (1.00) `state_unnamed_17`
│  ├─ [✓] 知识库搜索浮层 (1.00) `state_unnamed_18`
│  │    ─TRANSITIONS_TO→ 全局搜索结果空状态
│  │    ─TRANSITIONS_TO→ Search positive results
│  ├─ [✓] 全局搜索结果空状态 (1.00) `state_unnamed_19`
│  ├─ [✓] Search positive results (1.00) `state_search_positive_results`
│  │    ─TRANSITIONS_TO→ Document read
│  ├─ [✓] Document read (1.00) `state_document_read`
│  │    ─REVEALS→ Document comment panel
│  ├─ [✓] Document comment panel (1.00) `state_document_comment_panel`
│  │    ─TRANSITIONS_TO→ Document tools panel
│  ├─ [✓] Document tools panel (1.00) `state_document_tools_panel`
│  │    ─REVEALS→ Document statistics modal
│  │    ─TRANSITIONS_TO→ Document history
│  │    ─TRANSITIONS_TO→ Document presentation mode
│  │    ─REVEALS→ Document knowledge graph
│  │    ─REVEALS→ Document review setup
│  ├─ [✓] Document statistics modal (1.00) `state_document_statistics_modal`
│  ├─ [✓] Document history (1.00) `state_document_history`
│  │    ─REVEALS→ History compare menu
│  ├─ [✓] History compare menu (1.00) `state_history_compare_menu`
│  ├─ [✓] Document knowledge graph (1.00) `state_document_knowledge_graph`
│  ├─ [✓] Document review setup (1.00) `state_document_review_setup`
│  ├─ [✓] Document presentation mode (0.90) `state_document_presentation_mode`
│  ├─ [✓] Global quick search (1.00) `state_global_quick_search`
│  │    ─TRANSITIONS_TO→ Global search suggestions
│  ├─ [✓] Global search suggestions (1.00) `state_global_search_suggestions`
│  │    ─TRANSITIONS_TO→ Document read
│  ├─ [✓] Account menu (1.00) `state_account_menu`
│  │    ─TRANSITIONS_TO→ Personal garden profile
│  │    ─TRANSITIONS_TO→ Creator analytics
│  ├─ [✓] Personal garden profile (1.00) `state_personal_garden_profile`
│  │    ─REVEALS→ Personal garden activity
│  ├─ [✓] Personal garden activity (1.00) `state_personal_garden_activity`
│  ├─ [✓] More menu (1.00) `state_more_menu`
│  │    ─TRANSITIONS_TO→ Recycle bin
│  ├─ [✓] Recycle bin (1.00) `state_recycle_bin`
│  │    ─TRANSITIONS_TO→ Recycle bin search empty
│  ├─ [✓] Recycle bin search empty (1.00) `state_recycle_bin_search_empty`
│  ├─ [✓] Message center unread (1.00) `state_message_center_unread`
│  │    ─TRANSITIONS_TO→ Message center read
│  ├─ [✓] Message center read (1.00) `state_message_center_read`
│  │    ─TRANSITIONS_TO→ Message center pending
│  ├─ [✓] Message center pending (1.00) `state_message_center_pending`
│  ├─ [✓] Creator analytics (1.00) `state_creator_analytics`
│  │    ─REVEALS→ Creator analytics range menu
│  ├─ [✓] Creator analytics range menu (1.00) `state_creator_analytics_range_menu`
│  │    ─TRANSITIONS_TO→ Creator analytics 30 days
│  ├─ [✓] Creator analytics 30 days (1.00) `state_creator_analytics_30_days`
│  │    ─TRANSITIONS_TO→ Personal info settings
│  ├─ [✓] Personal info settings (1.00) `state_personal_info_settings`
│  │    ─TRANSITIONS_TO→ General preferences
│  ├─ [✓] General preferences (1.00) `state_general_preferences`
│  │    ─TRANSITIONS_TO→ Navigation notification settings
│  ├─ [✓] Navigation notification settings (1.00) `state_navigation_notification_settings`
│  ├─ [✓] Create content menu (1.00) `state_create_content_menu`
│  ├─ [✓] Template center recommended (1.00) `state_template_center_recommended`
│  │    ─TRANSITIONS_TO→ Template preview switched
│  ├─ [✓] Template preview switched (1.00) `state_template_preview_switched`
│  │    ─TRANSITIONS_TO→ My templates empty
│  ├─ [✓] My templates empty (1.00) `state_my_templates_empty`
├─ Features
│  ├─ [✓] 开始页文档活动视图切换 (0.98) `feature_unnamed`
│  ├─ [✓] 开始页文档类型筛选 (1.00) `feature_unnamed_2`
│  ├─ [✓] 逛逛关注与推荐切换 (1.00) `feature_unnamed_3`
│  ├─ [✓] 收藏内容搜索 (1.00) `feature_unnamed_4`
│  ├─ [✓] 收藏类型筛选 (1.00) `feature_unnamed_5`
│  ├─ [✓] 小记搜索 (1.00) `feature_unnamed_6`
│  ├─ [✓] 小记视图模式切换 (1.00) `feature_unnamed_7`
│  ├─ [✓] AI 多模式任务入口 (0.90) `feature_ai`
│  ├─ [✓] AI 任务历史抽屉 (1.00) `feature_ai_2`
│  ├─ [✓] 知识库文档视图与排序 (0.95) `feature_unnamed_8`
│  ├─ [✓] 知识库范围搜索与结果筛选 (1.00) `feature_unnamed_9`
│  ├─ [✓] 搜索结果打开文档 (1.00) `feature_unnamed_10`
│  ├─ [✓] 文档评论面板 (1.00) `feature_unnamed_11`
│  ├─ [✓] 文档工具与管理面板 (1.00) `feature_unnamed_12`
│  ├─ [✓] 文档统计信息 (1.00) `feature_unnamed_13`
│  ├─ [✓] 文档历史版本浏览与对比 (1.00) `feature_unnamed_14`
│  ├─ [✓] 文档知识网络 (1.00) `feature_unnamed_15`
│  ├─ [✓] 文档评审准备 (1.00) `feature_unnamed_16`
│  ├─ [✓] 文档翻译切换 (0.95) `feature_unnamed_17`
│  ├─ [✓] 文档演示模式 (0.90) `feature_unnamed_18`
│  ├─ [✓] 全局快捷搜索与跳转 (1.00) `feature_unnamed_19`
│  ├─ [✓] 个人数字花园查看 (1.00) `feature_unnamed_20`
│  ├─ [✓] 回收站查看与搜索 (1.00) `feature_unnamed_21`
│  ├─ [✓] 消息中心分类与已读切换 (1.00) `feature_unnamed_22`
│  ├─ [✓] 创作数据时间范围筛选 (1.00) `feature_unnamed_23`
│  ├─ [✓] 账户与界面偏好配置 (1.00) `feature_unnamed_24`
│  ├─ [✓] 多类型内容创建入口 (1.00) `feature_unnamed_25`
│  ├─ [✓] 模板浏览与预览 (1.00) `feature_unnamed_26`
├─ Data
│  ├─ [✓] 文档 (0.90) `data_unnamed`
│  ├─ [✓] 知识库 (0.90) `data_unnamed_2`
│  ├─ [✓] Notification preferences (0.95) `data_notification_preferences`
│  ├─ [✓] AI task (0.90) `data_ai_task`
│  ├─ [✓] Document history version (1.00) `data_document_history_version`
```

## Features
### 开始页文档活动视图切换 `feature_unnamed`
- status: ClaimStatus.CONFIRMED · confidence: 0.98
- 开始页点击活动分类标签后，列表切换为该分类中的文档记录。
- **Preconditions**:
  - 位于开始页
- **Postconditions**:
  - 所选标签高亮
  - 文档列表内容刷新为相应活动记录
- evidence: step 1 (frame 1 → 2)

### 开始页文档类型筛选 `feature_unnamed_2`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 活动列表可按文档、表格、画板或数据表类型筛选；无结果显示空状态。
- **Preconditions**:
  - 位于开始页
- **Postconditions**:
  - 筛选标签显示选中类型
  - 列表仅保留匹配类型
  - 无匹配项时显示暂无内容
- evidence: step 5 (frame 9 → 10)

### 逛逛关注与推荐切换 `feature_unnamed_3`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 逛逛可在关注流和推荐流之间切换；关注无更新时显示空状态及推荐引导。
- **Preconditions**:
  - 位于逛逛
- **Postconditions**:
  - 选中标签高亮
  - 主内容切换为相应信息流或空状态
- evidence: step 7 (frame 13 → 14)

### 收藏内容搜索 `feature_unnamed_4`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 收藏页右上角搜索可按关键词筛选；输入后需按 Enter 提交，零匹配时显示空结果。
- **Preconditions**:
  - 位于收藏页
- **Postconditions**:
  - 列表按关键词过滤
  - 无匹配时显示搜索结果为空
  - 输入框保留关键词并提供清除按钮
- evidence: step 15 (frame 26 → 27)

### 收藏类型筛选 `feature_unnamed_5`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 收藏列表可按文档、资源、表格、图集、知识库或知识小组类型筛选；无匹配项显示快速收藏空状态。
- **Preconditions**:
  - 位于收藏页
- **Postconditions**:
  - 右上角显示所选类型
  - 列表仅保留对应类型
  - 无内容时显示空状态
- evidence: step 19 (frame 33 → 34)

### 小记搜索 `feature_unnamed_6`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 小记列表支持关键词搜索；输入后按 Enter 提交，零匹配时显示未找到相关小记。
- **Preconditions**:
  - 位于小记页
- **Postconditions**:
  - 列表按关键词过滤
  - 无匹配时显示搜索空状态
  - 内容工具栏进入禁用态
- evidence: step 24 (frame 42 → 43)

### 小记视图模式切换 `feature_unnamed_7`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 小记可在列表+编辑器模式与卡片流模式之间切换；卡片模式直接展示发布卡和历史小记。
- **Preconditions**:
  - 位于小记
- **Postconditions**:
  - 页面布局在三栏编辑模式与双栏卡片模式间切换
  - 卡片模式展示发布输入区与历史卡片
- evidence: step 27 (frame 46 → 47)

### AI 多模式任务入口 `feature_ai`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- AI 工作区可选择 PPT、生图、文档、可视化页面等任务模式，并提供通用对话输入及模型选择；本次只观察入口未提交任务。
- **Preconditions**:
  - 位于 AI 写作首页
- **Postconditions**:
  - 进入相应任务准备状态
- evidence: step 29 (frame 50 → 51)

### AI 任务历史抽屉 `feature_ai_2`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 任务列表按钮展开历史任务抽屉，可切换任务类别、搜索和排序，空记录显示暂无数据。
- **Preconditions**:
  - 位于 AI 写作首页
- **Postconditions**:
  - 左侧展开任务抽屉
  - 主工作区被部分覆盖
- evidence: step 31 (frame 54 → 55)

### 知识库文档视图与排序 `feature_unnamed_8`
- status: ClaimStatus.CONFIRMED · confidence: 0.95
- 知识库侧栏可切换目录或全部文档视图，并按创建时间、更新时间或标题排序。
- **Preconditions**:
  - 位于知识库
- **Postconditions**:
  - 勾选状态更新
  - 侧栏文档列表按所选规则显示
- evidence: step 34 (frame 60 → 61)

### 知识库范围搜索与结果筛选 `feature_unnamed_9`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 知识库侧栏搜索在浮层中限定当前账号与知识库；按 Enter 会在新标签页打开搜索结果，可切换范围并按内容、创建者和更新时间筛选。
- **Preconditions**:
  - 位于知识库
- **Postconditions**:
  - 新标签页打开搜索结果
  - 结果页保留关键词和知识库范围
  - 无匹配时显示零结果空状态
- evidence: step 38 (frame 66 → 67)

### 搜索结果打开文档 `feature_unnamed_10`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 有匹配结果时，点击结果会在新的浏览器标签页打开对应知识库文档阅读页。
- **Preconditions**:
  - 搜索结果页有匹配项
- **Postconditions**:
  - 新标签页打开与所点标题一致的文档
  - 保留原搜索结果标签
- evidence: step 43 (frame 74 → 75)

### 文档评论面板 `feature_unnamed_11`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 文档阅读页可打开划词评论面板查看评论数量和讨论。
- **Preconditions**:
  - 位于文档阅读态
- **Postconditions**:
  - 右侧展开划词评论面板
  - 主正文宽度缩窄
- evidence: step 44 (frame 76 → 77)

### 文档工具与管理面板 `feature_unnamed_12`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 阅读页工具面板汇集演示、知识网络、评审、翻译、文档信息、阅读布局、历史版本、打印/导出/复制/移动等入口；删除为危险入口，本次未触发。
- **Preconditions**:
  - 位于文档阅读态
- **Postconditions**:
  - 右侧展开文档工具与管理面板
- evidence: step 47 (frame 81 → 82)

### 文档统计信息 `feature_unnamed_13`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 文档信息入口打开统计详情，展示版本、时间、人员和阅读互动指标。
- **Preconditions**:
  - 文档工具面板已打开
- **Postconditions**:
  - 统计详情弹窗出现
  - 页面背景变暗
- evidence: step 48 (frame 83 → 84)

### 文档历史版本浏览与对比 `feature_unnamed_14`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 可浏览历史发布记录、选择任一版本预览，并从对比下拉选择另一历史记录；保存或恢复会修改数据，本次未触发。
- **Preconditions**:
  - 位于文档工具面板
- **Postconditions**:
  - 进入历史记录页
  - 选择条目后预览对应时间版本
  - 对比下拉可选择另一版本
- evidence: step 50 (frame 86 → 87)

### 文档知识网络 `feature_unnamed_15`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 阅读页可打开当前文档的引用关系图，并在被引用和引用了两个方向间切换。
- **Preconditions**:
  - 位于文档阅读态
- **Postconditions**:
  - 打开知识网络模态
  - 显示当前文档中心节点与双向引用关系
- evidence: step 56 (frame 98 → 99)

### 文档评审准备 `feature_unnamed_16`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 文档可配置评审名称、说明、评审人与抄送人并发起评审；因任务只读约束，本次仅验证表单和必填禁用态，未发起。
- **Preconditions**:
  - 位于文档阅读态
- **Postconditions**:
  - 右侧打开评审表单
  - 未指定必填评审人时发起按钮禁用
- evidence: step 60 (frame 105 → 106)

### 文档翻译切换 `feature_unnamed_17`
- status: ClaimStatus.CONFIRMED · confidence: 0.95
- 工具面板翻译按钮可切换翻译显示与查看原文；点击后按钮变为查看原文。
- **Preconditions**:
  - 位于文档阅读态
  - 工具面板已打开
- **Postconditions**:
  - 翻译按钮切换为查看原文
  - 再次点击可回到原文
- evidence: step 59 (frame 103 → 104)

### 文档演示模式 `feature_unnamed_18`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- 文档可进入近全屏演示阅读布局；本次观察到标题页后界面自动回到阅读态。
- **Preconditions**:
  - 位于文档阅读态
- **Postconditions**:
  - 短暂显示近全屏文档演示布局
  - 随后返回常规阅读态
- evidence: step 54 (frame 94 → 95)

### 全局快捷搜索与跳转 `feature_unnamed_19`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 全局搜索既可选择搜索范围进入结果页，也可从浏览过的匹配项直接在当前标签打开文档；支持键盘上下选择与 Enter 跳转。
- **Preconditions**:
  - 位于全局工作台
- **Postconditions**:
  - 范围入口打开相应搜索结果
  - 匹配文档直接打开阅读页
- evidence: step 69 (frame 119 → 120)

### 个人数字花园查看 `feature_unnamed_20`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 账号菜单可在新标签页打开个人数字花园，查看公开资料、关注统计、知识库展示、创作热力图和更新动态；本次未修改公开状态或内容。
- **Preconditions**:
  - 已登录
  - 头像菜单已打开
- **Postconditions**:
  - 新标签页打开个人花园
  - 可滚动查看创作指数与更新动态
- evidence: step 72 (frame 124 → 125)

### 回收站查看与搜索 `feature_unnamed_21`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 回收站可查看删除项并按关键词搜索；每项提供恢复与彻底删除，但受只读约束本次未触发写操作。
- **Preconditions**:
  - 位于回收站
- **Postconditions**:
  - 删除项列表按关键词过滤
  - 无匹配时显示空结果
- evidence: step 80 (frame 137 → 138)

### 消息中心分类与已读切换 `feature_unnamed_22`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 消息中心可在未读和已读间切换，并按关注、点赞、提及评论、待处理、系统和其他消息分类；全部已读会修改消息状态，本次未操作。
- **Preconditions**:
  - 消息中心已打开
- **Postconditions**:
  - 顶部状态或左侧分类高亮
  - 列表刷新为相应消息集合
- evidence: step 83 (frame 142 → 143)

### 创作数据时间范围筛选 `feature_unnamed_23`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 创作中心可按近一年、近30天或历史范围更新数据总览与个人创作指标。
- **Preconditions**:
  - 位于创作中心数据统计
- **Postconditions**:
  - 时间范围标签更新
  - 统计卡片同步刷新
  - 不可用指标显示占位符
- evidence: step 88 (frame 150 → 151)

### 账户与界面偏好配置 `feature_unnamed_24`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 设置中心提供个人资料、主题/语言、文档样式、导航提醒和消息渠道配置；本次仅查看，未修改或保存任何设置。
- **Preconditions**:
  - 已登录并进入设置中心
- **Postconditions**:
  - 主区切换为相应设置表单
  - 已有配置以输入值、开关、单选或下拉展示
- evidence: step 90 (frame 154 → 155)

### 多类型内容创建入口 `feature_unnamed_25`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 开始页提供文档、表格、画板、数据表和导入的内容创建入口；选择后会创建或导入内容，本次仅验证菜单，未执行。
- **Preconditions**:
  - 位于开始页
- **Postconditions**:
  - 展开多类型创建菜单
- evidence: step 94 (frame 162 → 163)

### 模板浏览与预览 `feature_unnamed_26`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 模板中心可在推荐模板中按类别选择并实时预览，也可切换到我的模板；使用模板会创建内容，本次仅浏览未使用。
- **Preconditions**:
  - 位于开始页
- **Postconditions**:
  - 遮罩层打开模板中心
  - 选中条目后预览与高亮更新
  - 我的模板无内容时显示空状态
- evidence: step 95 (frame 164 → 165)
