# Functional Topology — live_www_yuque_com

- session: `sess_20260826_132450_cf9bf0`
- generated: 2026-08-26T13:52:32.947628+00:00
- coverage: 43 states · 41 features · 26 data · 48 edges (confirmed ratio 100%, 61 actions)

## Graph
```
live_www_yuque_com
├─ States
│  ├─ [✓] 知识库文档编辑模式 (0.99) `state_unnamed`
│  │    ─TRANSITIONS_TO→ 知识库首页
│  ├─ [✓] 知识库首页 (0.99) `state_unnamed_2`
│  │    ─TRANSITIONS_TO→ 知识库总览
│  ├─ [✓] 知识库总览 (0.99) `state_unnamed_3`
│  │    ─TRANSITIONS_TO→ 受邀协作知识库空状态
│  ├─ [✓] 受邀协作知识库空状态 (0.99) `state_unnamed_4`
│  │    ─TRANSITIONS_TO→ 个人与空间切换菜单
│  ├─ [✓] 个人与空间切换菜单 (0.99) `state_unnamed_5`
│  │    ─REVEALS→ 消息中心
│  │    ─REVEALS→ 账号菜单
│  ├─ [✓] 消息中心 (0.99) `state_unnamed_6`
│  │    ─TRANSITIONS_TO→ 消息中心已读视图
│  ├─ [✓] 消息中心已读视图 (0.99) `state_unnamed_7`
│  ├─ [✓] 账号菜单 (0.99) `state_unnamed_8`
│  │    ─TRANSITIONS_TO→ 个人花园私密主页
│  │    ─TRANSITIONS_TO→ 创作中心数据统计
│  ├─ [✓] 个人花园私密主页 (0.99) `state_unnamed_9`
│  │    ─REVEALS→ 个人花园创作指数
│  ├─ [✓] 个人花园创作指数 (0.99) `state_unnamed_10`
│  ├─ [✓] 创作中心数据统计 (0.99) `state_unnamed_11`
│  │    ─TRANSITIONS_TO→ 偏好设置—通用与文档样式
│  ├─ [✓] 偏好设置—通用与文档样式 (0.99) `state_unnamed_12`
│  │    ─TRANSITIONS_TO→ 偏好设置—导航、提醒与消息
│  ├─ [✓] 偏好设置—导航、提醒与消息 (0.99) `state_unnamed_13`
│  │    ─TRANSITIONS_TO→ 偏好设置—AI 助手
│  ├─ [✓] 偏好设置—AI 助手 (0.99) `state_ai`
│  │    ─TRANSITIONS_TO→ 安全日志
│  ├─ [✓] 安全日志 (0.99) `state_unnamed_14`
│  │    ─TRANSITIONS_TO→ 账户管理
│  ├─ [✓] 账户管理 (0.99) `state_unnamed_15`
│  │    ─TRANSITIONS_TO→ 会员信息与邀请活动
│  ├─ [✓] 会员信息与邀请活动 (0.99) `state_unnamed_16`
│  │    ─TRANSITIONS_TO→ 账单与发票管理
│  ├─ [✓] 账单与发票管理 (0.99) `state_unnamed_17`
│  │    ─TRANSITIONS_TO→ 稻谷积分中心
│  ├─ [✓] 稻谷积分中心 (0.99) `state_unnamed_18`
│  │    ─TRANSITIONS_TO→ 开始工作台
│  ├─ [✓] 开始工作台 (0.99) `state_unnamed_19`
│  │    ─REVEALS→ 全局搜索/命令面板
│  │    ─REVEALS→ 全局快速创建菜单
│  │    ─REVEALS→ 全局更多菜单
│  │    ─REVEALS→ 模板中心弹层
│  │    ─REVEALS→ AI 帮写应用选择弹层
│  │    ─TRANSITIONS_TO→ AI 写作主工作台
│  ├─ [✓] 全局搜索/命令面板 (0.99) `state_unnamed_20`
│  │    ─TRANSITIONS_TO→ 全局搜索范围选择
│  ├─ [✓] 全局搜索范围选择 (0.99) `state_unnamed_21`
│  │    ─TRANSITIONS_TO→ 全局搜索结果页
│  ├─ [✓] 全局搜索结果页 (0.99) `state_unnamed_22`
│  │    ─TRANSITIONS_TO→ 公开搜索聚合结果
│  ├─ [✓] 公开搜索聚合结果 (0.99) `state_unnamed_23`
│  ├─ [✓] 全局快速创建菜单 (0.99) `state_unnamed_24`
│  ├─ [✓] 全局更多菜单 (0.99) `state_unnamed_25`
│  │    ─TRANSITIONS_TO→ 回收站列表
│  ├─ [✓] 回收站列表 (0.99) `state_unnamed_26`
│  ├─ [✓] 模板中心弹层 (0.99) `state_unnamed_27`
│  │    ─TRANSITIONS_TO→ 个人模板空状态
│  ├─ [✓] 个人模板空状态 (0.99) `state_unnamed_28`
│  ├─ [✓] AI 帮写应用选择弹层 (0.99) `state_ai_2`
│  │    ─TRANSITIONS_TO→ AI 应用会话页
│  ├─ [✓] AI 应用会话页 (0.99) `state_ai_3`
│  ├─ [✓] AI 写作主工作台 (0.99) `state_ai_4`
│  │    ─TRANSITIONS_TO→ AI 技能市场
│  ├─ [✓] AI 技能市场 (0.99) `state_ai_5`
│  │    ─REVEALS→ AI 技能详情弹层
│  ├─ [✓] AI 技能详情弹层 (0.99) `state_ai_6`
│  │    ─TRANSITIONS_TO→ 小记卡片视图
│  ├─ [✓] 小记卡片视图 (0.99) `state_unnamed_29`
│  │    ─TRANSITIONS_TO→ 小记列表编辑视图
│  ├─ [✓] 小记列表编辑视图 (0.99) `state_unnamed_30`
│  │    ─REVEALS→ 小记筛选、排序与批量菜单
│  ├─ [✓] 小记筛选、排序与批量菜单 (0.99) `state_unnamed_31`
│  │    ─TRANSITIONS_TO→ 收藏列表
│  ├─ [✓] 收藏列表 (0.99) `state_unnamed_32`
│  │    ─REVEALS→ 收藏类型筛选
│  │    ─TRANSITIONS_TO→ 逛逛推荐信息流
│  │    ─TRANSITIONS_TO→ 电子表格阅读页
│  │    ─PERSISTS_TO→ 收藏列表
│  │    ─TRANSITIONS_TO→ 全局搜索零结果状态
│  ├─ [✓] 收藏类型筛选 (0.99) `state_unnamed_33`
│  ├─ [✓] 逛逛推荐信息流 (0.99) `state_unnamed_34`
│  ├─ [✓] 电子表格阅读页 (0.99) `state_unnamed_35`
│  │    ─TRANSITIONS_TO→ 电子表格筛选模式
│  ├─ [✓] 电子表格筛选模式 (0.99) `state_unnamed_36`
│  ├─ [✓] 全局搜索零结果状态 (0.99) `state_unnamed_37`
├─ Features
│  ├─ [✓] 知识库分层组织与文档编辑 (0.98) `feature_unnamed`
│  │    ─DEPENDS_ON→ 知识库
│  ├─ [✓] 知识库总览、搜索与布局 (0.99) `feature_unnamed_2`
│  ├─ [✓] 个人账号与团队空间切换 (0.99) `feature_unnamed_3`
│  ├─ [✓] 消息分类、未读管理与待处理入口 (0.99) `feature_unnamed_4`
│  ├─ [✓] 账号、会员、语言与主题入口 (0.99) `feature_unnamed_5`
│  ├─ [✓] 个人花园展示与公开控制 (0.99) `feature_unnamed_6`
│  ├─ [✓] 花园创作活跃度展示 (0.99) `feature_unnamed_7`
│  ├─ [✓] 创作统计与账号管理中心 (0.99) `feature_unnamed_8`
│  ├─ [✓] 跨端主题语言与默认文档样式 (0.99) `feature_unnamed_9`
│  ├─ [✓] 导航可见性与分类型通知渠道 (0.99) `feature_unnamed_10`
│  ├─ [✓] AI 写作能力与应用逐项开关 (0.99) `feature_ai`
│  ├─ [✓] 登录设备与安全记录管理 (0.99) `feature_unnamed_11`
│  ├─ [✓] 登录凭据、第三方绑定与账号删除 (0.99) `feature_unnamed_12`
│  ├─ [✓] 会员权益、配额与邀请奖励 (0.99) `feature_unnamed_13`
│  ├─ [✓] 会员账单查询与发票管理 (0.99) `feature_unnamed_14`
│  ├─ [✓] 创作互动积分与权益兑换 (0.99) `feature_unnamed_15`
│  ├─ [✓] 开始页创建入口与活动文档聚合 (0.99) `feature_unnamed_16`
│  ├─ [✓] 全局搜索与快捷导航 (0.99) `feature_unnamed_17`
│  ├─ [✓] 私域与公开内容搜索范围 (0.99) `feature_unnamed_18`
│  ├─ [✓] 全文结果、高亮与多维筛选 (0.99) `feature_unnamed_19`
│  │    ─REVEALS→ 搜索结果
│  ├─ [✓] 公开用户、知识库与内容聚合搜索 (0.99) `feature_unnamed_20`
│  ├─ [✓] 全局多类型快速创建 (0.99) `feature_unnamed_21`
│  ├─ [✓] 回收站、客户端下载、帮助与反馈入口 (0.99) `feature_unnamed_22`
│  ├─ [✓] 删除内容查找、恢复与彻底删除入口 (0.99) `feature_unnamed_23`
│  ├─ [✓] 模板分类浏览、预览与使用入口 (0.99) `feature_unnamed_24`
│  ├─ [✓] 个人模板创建与另存 (0.99) `feature_unnamed_25`
│  ├─ [✓] AI 直接指令与场景应用 (0.99) `feature_ai_2`
│  ├─ [✓] AI 应用专属对话 (0.99) `feature_ai_3`
│  ├─ [✓] 多形态 AI 内容生成工作台 (0.99) `feature_ai_4`
│  │    ─DEPENDS_ON→ AI 生成任务
│  ├─ [✓] AI 技能发现、搜索与投稿入口 (0.99) `feature_ai_5`
│  ├─ [✓] AI 技能详情、安装与试玩入口 (0.99) `feature_ai_6`
│  ├─ [✓] 小记快速记录与双视图 (0.99) `feature_unnamed_26`
│  ├─ [✓] 小记富文本编辑与标签搜索 (0.99) `feature_unnamed_27`
│  ├─ [✓] 小记分类来源状态筛选与批量操作 (0.99) `feature_unnamed_28`
│  ├─ [✓] 收藏内容聚合、分组、搜索与筛选 (0.99) `feature_unnamed_29`
│  │    ─DEPENDS_ON→ 收藏记录
│  ├─ [✓] 收藏按内容类型筛选 (0.99) `feature_unnamed_30`
│  ├─ [✓] 社区内容发现、原文阅读与花园推荐 (0.99) `feature_unnamed_31`
│  ├─ [✓] 电子表格网格浏览、筛选排序与缩放 (0.99) `feature_unnamed_32`
│  │    ─DEPENDS_ON→ 电子表格
│  ├─ [✓] 表头筛选模式切换 (0.99) `feature_unnamed_33`
│  ├─ [✓] 页面与收藏数据刷新持久化 (0.99) `feature_unnamed_34`
│  ├─ [✓] 搜索零结果反馈与恢复建议 (0.99) `feature_unnamed_35`
├─ Data
│  ├─ [✓] 知识库文档 (0.99) `data_unnamed`
│  ├─ [✓] 知识库 (0.98) `data_unnamed_2`
│  │    ←DEPENDS_ON─ 知识库分层组织与文档编辑
│  ├─ [✓] 知识库总览条目 (0.99) `data_unnamed_3`
│  ├─ [✓] 工作空间 (0.98) `data_unnamed_4`
│  ├─ [✓] 通知消息 (0.99) `data_unnamed_5`
│  ├─ [✓] 账号偏好与会员状态 (0.98) `data_unnamed_6`
│  ├─ [✓] 个人花园 (0.99) `data_unnamed_7`
│  ├─ [✓] 创作统计 (0.99) `data_unnamed_8`
│  ├─ [✓] 用户偏好 (0.99) `data_unnamed_9`
│  ├─ [✓] 安全登录记录 (0.99) `data_unnamed_10`
│  ├─ [✓] 账户绑定状态 (0.99) `data_unnamed_11`
│  ├─ [✓] 会员与配额 (0.99) `data_unnamed_12`
│  ├─ [✓] 账单记录 (0.99) `data_unnamed_13`
│  ├─ [✓] 稻谷积分记录 (0.99) `data_unnamed_14`
│  ├─ [✓] 文档活动条目 (0.98) `data_unnamed_15`
│  ├─ [✓] 搜索结果 (0.99) `data_unnamed_16`
│  ├─ [✓] 公开用户与知识库搜索结果 (0.99) `data_unnamed_17`
│  ├─ [✓] 已删除内容记录 (0.99) `data_unnamed_18`
│  ├─ [✓] 模板条目 (0.99) `data_unnamed_19`
│  ├─ [✓] AI 灵感值与应用 (0.99) `data_ai`
│  ├─ [✓] AI 生成任务 (0.99) `data_ai_2`
│  │    ←DEPENDS_ON─ 多形态 AI 内容生成工作台
│  ├─ [✓] AI 技能条目 (0.99) `data_ai_3`
│  ├─ [✓] 小记 (0.99) `data_unnamed_20`
│  ├─ [✓] 收藏记录 (0.99) `data_unnamed_21`
│  │    ←DEPENDS_ON─ 收藏内容聚合、分组、搜索与筛选
│  ├─ [✓] 社区推荐条目 (0.99) `data_unnamed_22`
│  ├─ [✓] 电子表格 (0.99) `data_unnamed_23`
│  │    ←DEPENDS_ON─ 电子表格网格浏览、筛选排序与缩放
```

## Features
### 知识库分层组织与文档编辑 `feature_unnamed`
- status: ClaimStatus.CONFIRMED · confidence: 0.98
- 知识库首页提供规模统计、所有者、简介和层级目录；侧栏支持搜索、新建、首页/全部文档切换。文档可进入完整富文本编辑模式并返回首页。本次未修改内容。
- evidence: step 1 (frame 1 → 2)

### 知识库总览、搜索与布局 `feature_unnamed_2`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 个人知识库总览聚合常用知识库和完整列表，可在个人拥有与受邀协作之间切换，搜索知识库，并切换网格/列表布局；条目显示权限和更新时间，可置顶。本次未新建或更改置顶。
- evidence: step 2 (frame 3 → 4)

### 个人账号与团队空间切换 `feature_unnamed_3`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 全局左上入口用于在个人工作区和空间之间切换；当前仅显示个人账号，并提供创建空间入口。本次未创建空间。
- evidence: step 4 (frame 7 → 8)

### 消息分类、未读管理与待处理入口 `feature_unnamed_4`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 消息中心把社交互动、评论提及、待处理、系统和其他通知分类管理，可在未读/已读间切换，并提供逐条查看/处理与全部标为已读入口。本次未标记已读或处理消息。
- evidence: step 6 (frame 10 → 11)

### 账号、会员、语言与主题入口 `feature_unnamed_5`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 头像菜单集中提供个人花园、创作中心、设置、退出登录、会员购买/状态、界面语言和明暗主题切换。本次未更改设置、语言、主题或登录状态。
- evidence: step 9 (frame 15 → 16)

### 个人花园展示与公开控制 `feature_unnamed_6`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 个人花园作为公开主页候选，汇总个人资料、关注/粉丝、介绍、公开知识库和创作活跃度；当前私密，仅自己可见，可从页面设置公开或编辑展示模块。本次未公开或添加内容。
- evidence: step 10 (frame 17 → 18)

### 花园创作活跃度展示 `feature_unnamed_7`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 个人花园用类似贡献热力图的全年创作指数展示活跃程度，并在下方呈现公开更新内容。
- evidence: step 11 (frame 19 → 20)

### 创作统计与账号管理中心 `feature_unnamed_8`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 创作中心汇总创作规模、内容结构、公开分享表现、互动者和全年活跃度，并提供个人信息、通用/文档/导航/提醒/消息/AI 偏好、安全日志、账号管理、会员和账单入口。本次仅查看统计，未更改设置。
- evidence: step 14 (frame 23 → 24)

### 跨端主题语言与默认文档样式 `feature_unnamed_9`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 用户可设置跨桌面/网页同步的主题和语言，并配置新建文档默认字号、段间距与中英文数字自动空格，配有实时预览；单篇文档可另行覆盖。本次未更改任何偏好。
- evidence: step 15 (frame 25 → 26)

### 导航可见性与分类型通知渠道 `feature_unnamed_10`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 用户可控制社区入口和导航提醒的可见性，并为关注、点赞、评论提及、待处理、系统及其他消息分别选择站内消息/邮件等通知渠道。本次未改变开关或渠道。
- evidence: step 17 (frame 29 → 30)

### AI 写作能力与应用逐项开关 `feature_ai`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 用户可分别启停选区 AI 的编辑、润色、整理、续写能力，也可逐个控制百宝箱中的官方场景应用是否可用。本次未改变开关。
- evidence: step 18 (frame 31 → 32)

### 登录设备与安全记录管理 `feature_unnamed_11`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 安全日志展示当前和历史登录设备及账号登录记录；用户可删除设备，之后该设备再次登录需身份验证。本次未删除设备。
- evidence: step 19 (frame 33 → 34)

### 登录凭据、第三方绑定与账号删除 `feature_unnamed_12`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 用户可管理手机、邮箱、密码、个人路径及多种第三方登录绑定；账号删除需要先处理名下知识库/知识小组/空间，并提示删除后的重新注册限制。本次未绑定、解绑、更改或删除账号。
- evidence: step 20 (frame 35 → 36)

### 会员权益、配额与邀请奖励 `feature_unnamed_13`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 会员中心展示会员状态和用量配额，支持兑换或购买；邀请活动通过个人邀请码邀请新用户，并可输入他人邀请码领取会员奖励，提供邀请明细与账号管理入口。本次未购买、兑换、邀请或提交邀请码。
- evidence: step 21 (frame 37 → 38)

### 会员账单查询与发票管理 `feature_unnamed_14`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 用户可按日期范围查询会员权益或交易记录，查看支付历史，并管理发票申请。本次未申请发票或执行交易操作。
- evidence: step 22 (frame 39 → 40)

### 创作互动积分与权益兑换 `feature_unnamed_15`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 平台通过创建文档/小记、内容获赞、评论和关注等行为发放稻谷积分；积分可积累并兑换权益，明细按操作与时间可查。本次未兑换或触发积分行为。
- evidence: step 23 (frame 41 → 42)

### 开始页创建入口与活动文档聚合 `feature_unnamed_16`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 开始页聚合多类型内容创建、知识库创建、模板与 AI 帮写入口，并把个人文档活动按编辑、浏览、赞赏、评论分类显示和筛选。
- evidence: step 24 (frame 43 → 44)

### 全局搜索与快捷导航 `feature_unnamed_17`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 全局搜索兼具命令面板功能，默认可快速跳转页面和知识库，支持键盘方向键选择、回车跳转，并可通过特定前缀唤起更多命令。
- evidence: step 25 (frame 45 → 46)

### 私域与公开内容搜索范围 `feature_unnamed_18`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 关键词搜索可在与当前账号相关的内容和公开平台内容两个范围之间选择，再进入结果页。
- evidence: step 26 (frame 47 → 48)

### 全文结果、高亮与多维筛选 `feature_unnamed_19`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 全局搜索结果可在私域和公开范围间切换，展示匹配数量和摘要关键词高亮，并按内容类型、创建者和更新时间筛选。
- evidence: step 27 (frame 49 → 50)

### 公开用户、知识库与内容聚合搜索 `feature_unnamed_20`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 公开搜索按用户、知识库和内容三类聚合结果，并为用户和知识库提供查看全部入口；内容列表显示大规模匹配数量和高亮摘要。
- evidence: step 28 (frame 51 → 52)

### 全局多类型快速创建 `feature_unnamed_21`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 侧栏加号集中提供文档、表格、画板、数据表、知识库、模板、AI 帮写和导入入口。本次仅查看菜单，未创建或导入。
- evidence: step 30 (frame 54 → 55)

### 回收站、客户端下载、帮助与反馈入口 `feature_unnamed_22`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 全局更多菜单汇总删除内容恢复、客户端获取、帮助中心和用户反馈入口。
- evidence: step 31 (frame 56 → 57)

### 删除内容查找、恢复与彻底删除入口 `feature_unnamed_23`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 回收站支持查看和搜索已删除内容，并按条恢复或彻底删除；本次未执行任何数据变更。
- evidence: step 32 (frame 58 → 59)

### 模板分类浏览、预览与使用入口 `feature_unnamed_24`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 模板中心支持在推荐和个人模板间切换，按场景分组选择模板，并在左侧预览完整内容；选中后可使用模板创建内容。本次未使用模板。
- evidence: step 34 (frame 61 → 62)

### 个人模板创建与另存 `feature_unnamed_25`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 个人模板可直接新建，也可从已有文档另存为模板；无个人模板时显示空状态。本次未创建。
- evidence: step 35 (frame 63 → 64)

### AI 直接指令与场景应用 `feature_ai_2`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- AI 帮写可直接输入指令创作，也可选择预设官方场景应用；界面显示灵感值余额。本次未提交生成。
- evidence: step 37 (frame 66 → 67)

### AI 应用专属对话 `feature_ai_3`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 每个场景应用提供独立对话式会话，用户可输入需求发送并返回应用列表。本次未发送消息。
- evidence: step 38 (frame 68 → 69)

### 多形态 AI 内容生成工作台 `feature_ai_4`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- AI 写作可面向 PPT、图片、文档、可视化页面和更多场景创建任务；提示可附材料、提及上下文、绑定链接渠道并选择模型。还可查看任务、热门玩法和技能市场。本次未发起生成。
- evidence: step 40 (frame 71 → 72)

### AI 技能发现、搜索与投稿入口 `feature_ai_5`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 技能市场支持按场景分类或关键词发现技能，查看作者、版本与热度，并管理个人技能或投稿新技能。本次未安装、导入或投稿。
- evidence: step 42 (frame 74 → 75)

### AI 技能详情、安装与试玩入口 `feature_ai_6`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 技能详情提供完整能力说明、安装命令、一键安装、推荐玩法、试玩和 README，并可切换相邻技能。本次未安装或试玩。
- evidence: step 44 (frame 77 → 78)

### 小记快速记录与双视图 `feature_unnamed_26`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 小记支持列表编辑和卡片快速记录两种布局；卡片模式可附图片、待办、书签、附件和标签，并用 Ctrl+Enter 发布，同时查看历史预览。本次未输入或发布。
- evidence: step 45 (frame 79 → 80)

### 小记富文本编辑与标签搜索 `feature_unnamed_27`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 列表视图支持新建、按标签筛选、搜索和富文本编辑，工具栏覆盖段落与常用文字/列表格式；小记可添加标签。本次未编辑。
- evidence: step 46 (frame 81 → 82)

### 小记分类来源状态筛选与批量操作 `feature_unnamed_28`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 小记可按内容形态、来源渠道、分享/归档状态过滤，按更新时间排序，并进入支持 Shift 选择的批量操作模式。
- evidence: step 47 (frame 83 → 84)

### 收藏内容聚合、分组、搜索与筛选 `feature_unnamed_29`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 收藏页集中管理文档、表格、知识库等已收藏内容，可按分组查看、搜索和筛选；星标表示收藏状态。本次未取消收藏或创建分组。
- evidence: step 49 (frame 86 → 87)

### 收藏按内容类型筛选 `feature_unnamed_30`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 收藏记录可按文档、资源、表格、图集、知识库和知识小组类型过滤。
- evidence: step 50 (frame 88 → 89)

### 社区内容发现、原文阅读与花园推荐 `feature_unnamed_31`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 逛逛提供关注与推荐信息流，可进入原文或 AI 速读；右栏推荐其他花园并支持刷新，另有投稿和开通花园入口。本次未点赞、关注或投稿。
- evidence: step 51 (frame 90 → 91)

### 电子表格网格浏览、筛选排序与缩放 `feature_unnamed_32`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 电子表格支持单元格网格阅读、工作表切换、筛选排序/查找等工具以及缩放和互动入口；当前打开为只读视图，本次未编辑或点赞。
- evidence: step 53 (frame 93 → 94)

### 表头筛选模式切换 `feature_unnamed_33`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 漏斗工具可切换表头筛选模式，启用后在表头显示筛选标记。
- evidence: step 54 (frame 95 → 96)

### 页面与收藏数据刷新持久化 `feature_unnamed_34`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 在收藏页执行刷新并等待后，仍停留在收藏模块，分组计数与收藏记录保持可见，说明导航页面与服务端收藏数据可在刷新后恢复。
- evidence: step 57 (frame 99 → 100)

### 搜索零结果反馈与恢复建议 `feature_unnamed_35`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 无匹配时页面明确显示 0 结果，并建议切换私域/公开搜索范围或修改关键词。
- evidence: step 59 (frame 102 → 103)
