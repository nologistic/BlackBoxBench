# Functional Topology — librera

- session: `sess_20260911_054708_415ebc`
- generated: 2026-09-11T06:38:35.926899+00:00
- coverage: 20 states · 19 features · 3 data · 3 edges (confirmed ratio 100%, 136 actions)

## Graph
```
librera
├─ States
│  ├─ [✓] 书库网格首页 (1.00) `state_书库网格首页`
│  │    ─TRANSITIONS_TO→ state_演奏阅读模式
│  ├─ [✓] 首次打开书籍的翻页模式选择 (1.00) `state_首次打开书籍的翻页模式选择`
│  ├─ [✓] 阅读器控制层（封面页） (1.00) `state_阅读器控制层_封面页`
│  ├─ [✓] 阅读偏好设置 (1.00) `state_阅读偏好设置`
│  ├─ [✓] 高级阅读设置 (1.00) `state_高级阅读设置`
│  ├─ [✓] 阅读状态栏设置 (1.00) `state_阅读状态栏设置`
│  ├─ [✓] 替换文本规则面板 (0.95) `state_替换文本规则面板`
│  ├─ [✓] 书内搜索面板 (1.00) `state_书内搜索面板`
│  ├─ [✓] 阅读器内书库面板 (1.00) `state_阅读器内书库面板`
│  ├─ [✓] 文件夹浏览页 (1.00) `state_文件夹浏览页`
│  ├─ [✓] 收藏列表 (1.00) `state_收藏列表`
│  ├─ [✓] 条目更多操作 (1.00) `state_条目更多操作`
│  ├─ [✓] 书签面板与添加对话框 (1.00) `state_书签面板与添加对话框`
│  ├─ [✓] 目录导航面板 (1.00) `state_目录导航面板`
│  ├─ [✓] 快速阅读RSVP (1.00) `state_快速阅读rsvp`
│  ├─ [✓] 全局偏好抽屉 (1.00) `state_全局偏好抽屉`
│  ├─ [✓] state_书库列表视图 (0.99) `state_书库列表视图`
│  ├─ [✓] state_书签总览 (0.99) `state_书签总览`
│  ├─ [✓] state_文字转语音设置 (0.99) `state_文字转语音设置`
│  ├─ [✓] state_演奏阅读模式 (0.99) `state_演奏阅读模式`
├─ Features
│  ├─ [✓] 选择翻页模式并进入阅读 (1.00) `feature_选择翻页模式并进入阅读`
│  ├─ [✓] 夜间阅读模式 (1.00) `feature_夜间阅读模式`
│  ├─ [✓] 可配置阅读外观与交互 (1.00) `feature_可配置阅读外观与交互`
│  ├─ [✓] 配置文本替换 (0.95) `feature_配置文本替换`
│  ├─ [✓] 书内全文搜索 (1.00) `feature_书内全文搜索`
│  ├─ [✓] 阅读中切换其他书籍 (1.00) `feature_阅读中切换其他书籍`
│  ├─ [✓] 新建TXT文本 (1.00) `feature_新建txt文本`
│  │    ─PERSISTS_TO→ feature_本地数据重启持久化
│  ├─ [✓] 收藏与标签管理 (1.00) `feature_收藏与标签管理`
│  ├─ [✓] 创建带备注书签 (1.00) `feature_创建带备注书签`
│  │    ─PERSISTS_TO→ state_书签总览
│  ├─ [✓] 按目录和缩略图跳页 (1.00) `feature_按目录和缩略图跳页`
│  ├─ [✓] 快速阅读播放 (1.00) `feature_快速阅读播放`
│  ├─ [✓] 全局配置与书库定制 (1.00) `feature_全局配置与书库定制`
│  ├─ [✓] feature_书库搜索与排序 (0.95) `feature_书库搜索与排序`
│  ├─ [✓] feature_书库展示与分组模式 (0.99) `feature_书库展示与分组模式`
│  ├─ [✓] feature_跨书籍书签汇总 (0.99) `feature_跨书籍书签汇总`
│  ├─ [✓] feature_本地数据重启持久化 (0.99) `feature_本地数据重启持久化`
│  ├─ [✓] feature_左右翻页模式 (0.99) `feature_左右翻页模式`
│  ├─ [✓] feature_文字转语音控制 (0.99) `feature_文字转语音控制`
│  ├─ [✓] feature_演奏模式触控翻页 (0.99) `feature_演奏模式触控翻页`
├─ Data
│  ├─ [✓] 本地TXT文档 (1.00) `data_本地txt文档`
│  ├─ [✓] 收藏与标签 (1.00) `data_收藏与标签`
│  ├─ [✓] 书签 (1.00) `data_书签`
```

## Features
### 选择翻页模式并进入阅读 `feature_选择翻页模式并进入阅读`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 首次打开书籍要求选择翻页方向；选择“上下翻页”后进入封面阅读页并显示阅读控制层。
- evidence: step 8 (frame 8 → 9)

### 夜间阅读模式 `feature_夜间阅读模式`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 阅读控制层的月亮按钮可将浅色纸张背景与深色正文切换为深棕纸张背景与浅色正文，图标随之变为太阳。
- evidence: step 17 (frame 23 → 24)

### 可配置阅读外观与交互 `feature_可配置阅读外观与交互`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 阅读器通过齿轮打开多页偏好面板，可调整字体、配色、亮度、翻页方式、高级交互和状态栏内容。
- evidence: step 18 (frame 25 → 26)

### 配置文本替换 `feature_配置文本替换`
- status: ClaimStatus.CONFIRMED · confidence: 0.95
- 阅读器提供文本替换规则面板；点击添加后自动启用并出现一对规则输入框。
- evidence: step 24 (frame 36 → 37)

### 书内全文搜索 `feature_书内全文搜索`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 输入关键词并点击搜索后，应用按总页数扫描全文，实时显示搜索进度并列出命中页码。
- evidence: step 32 (frame 48 → 49)

### 阅读中切换其他书籍 `feature_阅读中切换其他书籍`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 阅读面板内可直接浏览书库或文件夹，无需退出阅读器。
- evidence: step 42 (frame 65 → 66)

### 新建TXT文本 `feature_新建txt文本`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 文件夹页的新增菜单可新建.txt文件；编辑器要求名称与正文，保存后目录数量增加并生成带封面、时间、格式与大小的文件卡。
- evidence: step 47 (frame 74 → 75)

### 收藏与标签管理 `feature_收藏与标签管理`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 星标条目会进入收藏页；可创建标签并在条目更多菜单中勾选后应用。
- evidence: step 59 (frame 92 → 93)

### 创建带备注书签 `feature_创建带备注书签`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 可在当前页创建带说明的书签，保存后立即出现在书签面板。
- evidence: step 83 (frame 131 → 132)

### 按目录和缩略图跳页 `feature_按目录和缩略图跳页`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 可展开卷目录并点章节跳到起始页，也可打开前往页面缩略图网格并点选页码。
- evidence: step 92 (frame 146 → 147)

### 快速阅读播放 `feature_快速阅读播放`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- RSVP模式按设定速度在面板中央逐词播放当前正文，可重置或关闭。
- evidence: step 99 (frame 158 → 159)

### 全局配置与书库定制 `feature_全局配置与书库定制`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 用户可选择参与书库扫描的文件格式和目录，定制主题、交互与封面网格，并显示/隐藏及重排底部标签；还提供配置导入、导出与迁移。
- evidence: step 107 (frame 171 → 172)

### feature_书库搜索与排序 `feature_书库搜索与排序`
- status: ClaimStatus.CONFIRMED · confidence: 0.95
- 书库支持标题搜索过滤，并可从文件夹、文件名、文件大小、时间、标题、作者、丛书、丛书编号、页数、格式、语言、出版时间、出版商、最近等字段切换排序；选择标题后卡片顺序立即重排。
- evidence: step 118 (frame 190 → 191)

### feature_书库展示与分组模式 `feature_书库展示与分组模式`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 书库可切换列表、简表、网格、封面四种展示方式，也可按作者、流派、丛书、关键词、语言、标签、出版商、出版时间分组。列表模式显示更完整的文件元数据和阅读进度。
- evidence: step 119 (frame 192 → 193)

### feature_跨书籍书签汇总 `feature_跨书籍书签汇总`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 在阅读器中创建的带页码与备注书签会出现在主导航书签页，可按源文件识别、点击回到对应位置、搜索或删除。
- evidence: step 121 (frame 196 → 197)

### feature_本地数据重启持久化 `feature_本地数据重启持久化`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 重启应用后，新建的 TXT 文档、收藏状态、标签分组和带备注书签仍保留；显示方式与排序恢复为默认的网格/时间。
- evidence: step 122 (frame 198 → 199)

### feature_左右翻页模式 `feature_左右翻页模式`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 首次打开书籍可选择左右翻页。读者界面显示模式标签“左右翻页”，向左滑动后由第1页进入第2页，控制层自动隐藏，仅保留底部时间、章节页码和进度。
- evidence: step 127 (frame 207 → 208)

### feature_文字转语音控制 `feature_文字转语音控制`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 阅读器提供文字转语音控制，可选择语音/语言，调整语速、音调与音量，按段前后移动或播放/停止，并配置停顿、定时停止与标点停顿。
- evidence: step 130 (frame 213 → 214)

### feature_演奏模式触控翻页 `feature_演奏模式触控翻页`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 演奏模式为免手势阅读提供大号分区热区。点击右侧虚线区域后页码由1/4进入2/4，固定顶栏和书签热区保持可见。
- evidence: step 135 (frame 222 → 223)
