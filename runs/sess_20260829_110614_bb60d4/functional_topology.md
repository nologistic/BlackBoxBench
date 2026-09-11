# Functional Topology — yuque_web

- session: `sess_20260829_110614_bb60d4`
- generated: 2026-08-29T12:21:30.023002+00:00
- coverage: 28 states · 35 features · 15 data · 21 edges (confirmed ratio 100%, 79 actions)

## Graph
```
yuque_web
├─ States
│  ├─ [✓] AI 写作工作台 (0.99) `state_ai`
│  ├─ [✓] AI 任务列表抽屉 (0.99) `state_ai_2`
│  ├─ [✓] 小记-卡片视图 (0.99) `state_unnamed`
│  ├─ [✓] 小记-列表编辑视图 (0.99) `state_unnamed_2`
│  ├─ [✓] 收藏页 (0.99) `state_unnamed_3`
│  ├─ [✓] 逛逛-推荐流 (0.99) `state_unnamed_4`
│  ├─ [✓] 逛逛-关注空状态 (0.99) `state_unnamed_5`
│  ├─ [✓] AI 阅读助手 (0.98) `state_ai_3`
│  ├─ [✓] 知识库首页 (0.99) `state_unnamed_6`
│  ├─ [✓] 知识库-全部文档列表 (0.99) `state_unnamed_7`
│  ├─ [✓] 文档操作抽屉 (0.99) `state_unnamed_8`
│  ├─ [✓] 文档统计详情 (0.99) `state_unnamed_9`
│  ├─ [✓] 文档历史记录 (0.99) `state_unnamed_10`
│  ├─ [✓] 文档知识网络 (0.99) `state_unnamed_11`
│  ├─ [✓] 知识库内搜索浮层 (0.99) `state_unnamed_12`
│  ├─ [✓] 高级搜索结果页 (0.99) `state_unnamed_13`
│  ├─ [✓] 开始页 (0.99) `state_unnamed_14`
│  ├─ [✓] 创建内容菜单 (0.99) `state_unnamed_15`
│  ├─ [✓] 活动视图空状态 (0.99) `state_unnamed_16`
│  ├─ [✓] 全局搜索与快捷跳转 (0.99) `state_unnamed_17`
│  ├─ [✓] 更多菜单 (0.99) `state_unnamed_18`
│  ├─ [✓] 回收站 (0.99) `state_unnamed_19`
│  ├─ [✓] 消息中心 (0.99) `state_unnamed_20`
│  ├─ [✓] 模板中心-推荐 (0.99) `state_unnamed_21`
│  ├─ [✓] 模板中心-我的空状态 (0.99) `state_unnamed_22`
│  ├─ [✓] AI 帮你写应用选择 (0.99) `state_ai_4`
│  ├─ [✓] 表格阅读页 (0.99) `state_unnamed_23`
│  ├─ [✓] 新建表格-选择知识库 (0.99) `state_unnamed_24`
├─ Features
│  ├─ [✓] AI 多类型创作入口 (0.99) `feature_ai`
│  │    ─REVEALS→ AI 任务列表管理
│  ├─ [✓] AI 模型选择 (0.99) `feature_ai_2`
│  ├─ [✓] AI 任务列表管理 (0.99) `feature_ai_3`
│  │    ─TRANSITIONS_TO→ 小记快速记录
│  ├─ [✓] 小记快速记录 (0.98) `feature_unnamed`
│  ├─ [✓] 小记视图切换与编辑 (0.99) `feature_unnamed_2`
│  ├─ [✓] 浏览与组织收藏 (0.99) `feature_unnamed_3`
│  │    ─TRANSITIONS_TO→ 查看表格
│  ├─ [✓] 按收藏类型筛选 (0.99) `feature_unnamed_4`
│  │    ─PERSISTS_TO→ 收藏筛选状态持久化
│  ├─ [✓] 社区内容流 (0.99) `feature_unnamed_5`
│  ├─ [✓] AI 文章速读 (0.97) `feature_ai_4`
│  ├─ [✓] 知识库首页总览 (0.99) `feature_unnamed_6`
│  ├─ [✓] 知识库管理菜单 (0.99) `feature_unnamed_7`
│  ├─ [✓] 目录与全部文档视图 (0.99) `feature_unnamed_8`
│  ├─ [✓] 知识库文档阅读 (0.99) `feature_unnamed_9`
│  │    ─REVEALS→ 文档操作抽屉
│  ├─ [✓] 文档操作抽屉 (0.99) `feature_unnamed_10`
│  │    ─REVEALS→ 查看与比较文档历史
│  │    ─REVEALS→ 文档知识网络
│  ├─ [✓] 查看文档统计 (0.99) `feature_unnamed_11`
│  ├─ [✓] 文档评论侧栏 (0.99) `feature_unnamed_12`
│  ├─ [✓] 查看与比较文档历史 (0.99) `feature_unnamed_13`
│  ├─ [✓] 文档知识网络 (0.99) `feature_unnamed_14`
│  ├─ [✓] 知识库内搜索与快捷跳转 (0.99) `feature_unnamed_15`
│  │    ─TRANSITIONS_TO→ 高级搜索多维筛选
│  ├─ [✓] 高级搜索多维筛选 (0.99) `feature_unnamed_16`
│  ├─ [✓] 工作台总览与全局导航 (0.99) `feature_unnamed_17`
│  │    ─ENABLES→ 工作台文档筛选
│  │    ─ENABLES→ 全局搜索与快捷跳转
│  │    ─TRANSITIONS_TO→ AI 多类型创作入口
│  │    ─TRANSITIONS_TO→ 小记快速记录
│  │    ─TRANSITIONS_TO→ 浏览与组织收藏
│  │    ─TRANSITIONS_TO→ 社区内容流
│  │    ─TRANSITIONS_TO→ 知识库首页总览
│  │    ─REVEALS→ 浏览和使用模板
│  │    ─REVEALS→ 场景化 AI 帮写
│  │    ─REVEALS→ 全局更多与帮助入口
│  │    ─REVEALS→ 消息中心分类与已读视图
│  ├─ [✓] 多类型创建入口 (0.98) `feature_unnamed_18`
│  │    ─REQUIRES→ 创建内容的归属前置条件
│  ├─ [✓] 文档活动视图切换 (0.99) `feature_unnamed_19`
│  ├─ [✓] 工作台文档筛选 (0.99) `feature_unnamed_20`
│  ├─ [✓] 全局搜索与快捷跳转 (0.99) `feature_unnamed_21`
│  ├─ [✓] 全局更多与帮助入口 (0.99) `feature_unnamed_22`
│  │    ─TRANSITIONS_TO→ 管理已删除内容
│  ├─ [✓] 管理已删除内容 (0.99) `feature_unnamed_23`
│  ├─ [✓] 账号菜单 (0.99) `feature_unnamed_24`
│  ├─ [✓] 消息中心分类与已读视图 (0.99) `feature_unnamed_25`
│  ├─ [✓] 浏览和使用模板 (0.99) `feature_unnamed_26`
│  ├─ [✓] 场景化 AI 帮写 (0.99) `feature_ai_5`
│  ├─ [✓] 查看表格 (0.99) `feature_unnamed_27`
│  ├─ [✓] 工作表选择 (0.99) `feature_unnamed_28`
│  ├─ [✓] 收藏筛选状态持久化 (0.99) `feature_unnamed_29`
│  ├─ [✓] 创建内容的归属前置条件 (0.99) `feature_unnamed_30`
│  │    ←REQUIRES─ 多类型创建入口
├─ Data
│  ├─ [✓] AI 创作任务 (0.97) `data_ai`
│  ├─ [✓] 小记 (0.98) `data_unnamed`
│  ├─ [✓] 收藏项 (0.98) `data_unnamed_2`
│  ├─ [✓] 社区文章 (0.98) `data_unnamed_3`
│  ├─ [✓] 知识库目录 (0.99) `data_unnamed_4`
│  ├─ [✓] 知识库 (0.99) `data_unnamed_5`
│  ├─ [✓] 文档 (0.99) `data_unnamed_6`
│  ├─ [✓] 文档统计 (0.99) `data_unnamed_7`
│  ├─ [✓] 文档历史记录 (0.99) `data_unnamed_8`
│  ├─ [✓] 文档引用关系 (0.99) `data_unnamed_9`
│  ├─ [✓] 工作台文档条目 (0.99) `data_unnamed_10`
│  ├─ [✓] 回收站条目 (0.99) `data_unnamed_11`
│  ├─ [✓] 通知消息 (0.99) `data_unnamed_12`
│  ├─ [✓] 模板 (0.99) `data_unnamed_13`
│  ├─ [✓] 电子表格 (0.99) `data_unnamed_14`
```

## Features
### AI 多类型创作入口 `feature_ai`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- AI 工作台提供通用对话、PPT、生图、文档、可视化页面和更多类型；用户可在统一输入框补充附件、引用对象与消息渠道后发起任务。
- evidence: step 1 (frame 1 → 2)

### AI 模型选择 `feature_ai_2`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 输入框底部可展开模型菜单，提供多个模型版本供任务执行前选择，当前模型在触发器中显示。
- evidence: step 2 (frame 3 → 4)

### AI 任务列表管理 `feature_ai_3`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 任务列表抽屉可按两类任务切换、按名称搜索并进一步筛选；无记录时显示“暂无数据”空状态。
- evidence: step 3 (frame 5 → 6)

### 小记快速记录 `feature_unnamed`
- status: ClaimStatus.CONFIRMED · confidence: 0.98
- 卡片视图提供轻量发布框，可添加图片、待办、收藏标记、附件和标签，并支持快捷键发布；未输入时发布按钮不可用。
- evidence: step 4 (frame 7 → 8)

### 小记视图切换与编辑 `feature_unnamed_2`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 点击列表视图后切换为笔记列表加富文本编辑器，可按标签和关键词定位小记，并使用标题、强调、列表等格式工具编辑内容。
- evidence: step 5 (frame 9 → 10)

### 浏览与组织收藏 `feature_unnamed_3`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 收藏页集中展示不同内容类型的收藏项，可按分组浏览、按名称搜索、按名称或收藏时间排序；侧栏提供新增分组入口。
- evidence: step 7 (frame 13 → 14)

### 按收藏类型筛选 `feature_unnamed_4`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 类型筛选支持全部收藏、文档、资源、表格、图集、知识库和知识小组；选择后筛选标签显示当前类型并更新列表。
- evidence: step 8 (frame 15 → 16)

### 社区内容流 `feature_unnamed_5`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 逛逛页可在关注流与推荐流之间切换；推荐流展示文章卡片、互动数量、原文阅读与 AI 速读入口，关注流无内容时引导前往推荐。
- evidence: step 10 (frame 19 → 20)

### AI 文章速读 `feature_ai_4`
- status: ClaimStatus.CONFIRMED · confidence: 0.97
- 文章卡片的 AI 速读入口在右侧打开阅读助手，显示结构化摘要，并允许继续追问或开启新话题。
- evidence: step 13 (frame 25 → 26)

### 知识库首页总览 `feature_unnamed_6`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 知识库首页汇总文档数量、总字数、成员、简介和分组目录；目录节点可展开并显示文档更新时间，左侧目录树支持直接导航。
- evidence: step 16 (frame 29 → 30)

### 知识库管理菜单 `feature_unnamed_7`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 知识库首页更多菜单提供重命名、编辑首页、更多设置和删除入口；危险操作以红色显示。
- evidence: step 17 (frame 31 → 32)

### 目录与全部文档视图 `feature_unnamed_8`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 左侧目录入口可在层级目录树和全部文档列表之间切换；全部文档列表展示标题和可选摘要，当前项高亮并提供行内更多菜单。
- evidence: step 18 (frame 33 → 34)

### 知识库文档阅读 `feature_unnamed_9`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 从目录或全部文档列表选择文档后，主区展示富文本正文，可包含标题、段落、图片、公式、引用等内容块；顶部保留文档工具栏，底部有字数与互动入口。
- evidence: step 20 (frame 37 → 38)

### 文档操作抽屉 `feature_unnamed_10`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 文档右上角可展开操作抽屉，集中提供阅读增强、引用关系、评审翻译、统计、版本、输出和管理功能，正文随抽屉收窄。
- evidence: step 21 (frame 39 → 40)

### 查看文档统计 `feature_unnamed_11`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 点击文档信息卡片打开统计详情，集中展示内容规模、版本、人员、时间和互动数据。
- evidence: step 22 (frame 41 → 42)

### 文档评论侧栏 `feature_unnamed_12`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 点击评论入口从右侧打开文档评论侧栏，标题显示评论总数，正文区域随侧栏收窄；无评论时内容区为空，可用顶部箭头关闭。
- evidence: step 26 (frame 47 → 48)

### 查看与比较文档历史 `feature_unnamed_13`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 历史记录页按时间展示发布记录并预览当时内容；比较下拉复用全部记录、版本、本地缓存分类和仅已发布筛选，可选另一条历史进行对比。页面另提供保存版本与恢复当前记录入口。
- evidence: step 29 (frame 51 → 52)

### 文档知识网络 `feature_unnamed_14`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 以图形方式展示当前文档与其他文档的双向引用关系；可切换被引用/引用了分类，双击节点打开文档，无关系时显示空状态。
- evidence: step 32 (frame 57 → 58)

### 知识库内搜索与快捷跳转 `feature_unnamed_15`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 知识库搜索浮层既可按关键词查找当前知识库内容，也可快速跳转首页、设置、个人知识库、工作台和近期文档；无直接命中时提供高级搜索。
- evidence: step 36 (frame 62 → 63)

### 高级搜索多维筛选 `feature_unnamed_16`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 高级搜索在独立标签页组合搜索范围、账号、知识库、内容类型、创建者和更新时间。内容类型包括全部、文档、表格、画板、数据表和话题；创建者支持搜索选择；更新时间支持不限、24 小时、7 天、30 天和半年。
- evidence: step 39 (frame 67 → 68)

### 工作台总览与全局导航 `feature_unnamed_17`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 开始页汇总创建、模板、AI 和最近文档；左侧常驻导航可在开始、AI 写作、小记、收藏、社区及各知识库间切换。
- evidence: step 46 (frame 79 → 80)

### 多类型创建入口 `feature_unnamed_18`
- status: ClaimStatus.CONFIRMED · confidence: 0.98
- 开始页的新建菜单提供文档、表格、画板、数据表和导入五种入口；具体创建前可能先要求选择归属知识库。
- evidence: step 47 (frame 81 → 82)

### 文档活动视图切换 `feature_unnamed_19`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 文档列表可在编辑过、浏览过、点赞和评论四个活动维度间切换；当前标签高亮并更新列表，无记录时显示插画和解释性空状态。
- evidence: step 50 (frame 86 → 87)

### 工作台文档筛选 `feature_unnamed_20`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 类型筛选提供所有、文档、表格、画板和数据表；选择后标签更新并过滤列表，无匹配时显示空状态。页面还提供按归属和创建者筛选。
- evidence: step 53 (frame 91 → 92)

### 全局搜索与快捷跳转 `feature_unnamed_21`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 点击左侧搜索打开浮层；空输入可快速跳转常用页面和知识库，输入关键词后可在与我相关或公开范围执行搜索，并可清空关键词恢复默认面板。
- evidence: step 55 (frame 95 → 96)

### 全局更多与帮助入口 `feature_unnamed_22`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 更多菜单集中提供已删除内容管理、客户端下载、帮助和反馈入口。
- evidence: step 59 (frame 101 → 102)

### 管理已删除内容 `feature_unnamed_23`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 回收站支持搜索和排序已删除内容，并为每项提供恢复或彻底删除入口。
- evidence: step 60 (frame 103 → 104)

### 账号菜单 `feature_unnamed_24`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 点击头像展开账号菜单，显示会员状态与购买入口，并提供我的花园、创作中心、设置、退出登录、语言切换和主题切换。
- evidence: step 61 (frame 105 → 106)

### 消息中心分类与已读视图 `feature_unnamed_25`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 通知中心按互动与系统等类型分类消息，可在未读和已读间切换，每条显示事件摘要、时间和可用动作；未读页可批量标记全部已读。
- evidence: step 62 (frame 107 → 108)

### 浏览和使用模板 `feature_unnamed_26`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 模板中心支持在推荐模板与我的模板间切换；推荐模板按场景分类并提供预览，选中后可使用模板创建内容；我的模板为空时展示创建方法和空状态。
- evidence: step 66 (frame 114 → 115)

### 场景化 AI 帮写 `feature_ai_5`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- AI 帮写入口支持直接输入创作指令，也可选择官方场景应用；界面在提交前显示当前灵感值余额和创作按钮。
- evidence: step 70 (frame 120 → 121)

### 查看表格 `feature_unnamed_27`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 表格内容在独立标签页打开，支持单元格选择、筛选、排序、格式工具、工作表切换与缩放；顶部可进入收藏、评论和辅助侧栏。
- evidence: step 73 (frame 125 → 126)

### 工作表选择 `feature_unnamed_28`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 点击底部工作表列表按钮弹出工作表菜单，当前工作表带勾选；底部同时显示当前标签。
- evidence: step 74 (frame 127 → 128)

### 收藏筛选状态持久化 `feature_unnamed_29`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 选择收藏内容类型后刷新页面，类型筛选标签和过滤后的列表仍保持，说明筛选状态随当前页面状态持久化。
- evidence: step 77 (frame 131 → 132)

### 创建内容的归属前置条件 `feature_unnamed_30`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 新建表格在实际创建前要求先选择归属知识库；关闭选择弹窗会安全取消，未产生新内容。
- evidence: step 48 (frame 83 → 84)
