# Functional Topology — antennapod

- session: `sess_20260909_155429_3dc133`
- generated: 2026-09-09T16:44:06.874295+00:00
- coverage: 40 states · 32 features · 7 data · 39 edges (confirmed ratio 100%, 103 actions)

## Graph
```
antennapod
├─ States
│  ├─ [✓] 首页·无订阅空状态 (1.00) `state_首页_无订阅空状态`
│  │    ─TRANSITIONS_TO→ 队列·空状态
│  ├─ [✓] 队列·空状态 (1.00) `state_队列_空状态`
│  │    ─TRANSITIONS_TO→ 收件箱·空状态
│  ├─ [✓] 收件箱·空状态 (1.00) `state_收件箱_空状态`
│  │    ─TRANSITIONS_TO→ 订阅·空状态
│  ├─ [✓] 订阅·空状态 (1.00) `state_订阅_空状态`
│  │    ─REVEALS→ 更多导航菜单
│  ├─ [✓] 更多导航菜单 (1.00) `state_更多导航菜单`
│  │    ─TRANSITIONS_TO→ 添加播客入口页
│  │    ─TRANSITIONS_TO→ 单集·空状态
│  │    ─TRANSITIONS_TO→ 下载·空状态
│  │    ─TRANSITIONS_TO→ 播放记录·空状态
│  │    ─TRANSITIONS_TO→ 收藏·空状态
│  │    ─TRANSITIONS_TO→ 统计·订阅视图
│  │    ─TRANSITIONS_TO→ state_自定义导航对话框
│  │    ─TRANSITIONS_TO→ state_全局设置首页
│  ├─ [✓] 添加播客入口页 (1.00) `state_添加播客入口页`
│  │    ─TRANSITIONS_TO→ 添加播客·建议加载失败
│  ├─ [✓] 添加播客·建议加载失败 (1.00) `state_添加播客_建议加载失败`
│  │    ─TRANSITIONS_TO→ 播客搜索·无结果
│  │    ─REVEALS→ 通过 RSS 地址添加播客对话框
│  ├─ [✓] 播客搜索·无结果 (1.00) `state_播客搜索_无结果`
│  ├─ [✓] 通过 RSS 地址添加播客对话框 (1.00) `state_通过_rss_地址添加播客对话框`
│  │    ─VALIDATES→ RSS 地址·空值校验错误
│  ├─ [✓] RSS 地址·空值校验错误 (1.00) `state_rss_地址_空值校验错误`
│  ├─ [✓] 系统文件夹选择器 (1.00) `state_系统文件夹选择器`
│  │    ─REVEALS→ 本地文件夹访问授权确认
│  ├─ [✓] 本地文件夹访问授权确认 (1.00) `state_本地文件夹访问授权确认`
│  │    ─TRANSITIONS_TO→ 本地文件夹播客详情·空内容
│  ├─ [✓] 本地文件夹播客详情·空内容 (1.00) `state_本地文件夹播客详情_空内容`
│  │    ─TRANSITIONS_TO→ 本地文件夹播客·信息页
│  │    ─REVEALS→ 单集筛选底部面板
│  │    ─TRANSITIONS_TO→ 播客设置·上部
│  ├─ [✓] 本地文件夹播客·信息页 (1.00) `state_本地文件夹播客_信息页`
│  ├─ [✓] 单集筛选底部面板 (1.00) `state_单集筛选底部面板`
│  ├─ [✓] 播客设置·上部 (1.00) `state_播客设置_上部`
│  ├─ [✓] 播客重命名对话框 (1.00) `state_播客重命名对话框`
│  ├─ [✓] 订阅·单个播客网格 (1.00) `state_订阅_单个播客网格`
│  │    ─TRANSITIONS_TO→ 订阅多选操作模式
│  ├─ [✓] 订阅多选操作模式 (1.00) `state_订阅多选操作模式`
│  ├─ [✓] 编辑订阅标签对话框 (1.00) `state_编辑订阅标签对话框`
│  ├─ [✓] 订阅·标签筛选网格 (1.00) `state_订阅_标签筛选网格`
│  ├─ [✓] 单集·空状态 (1.00) `state_单集_空状态`
│  ├─ [✓] 下载·空状态 (1.00) `state_下载_空状态`
│  ├─ [✓] 下载日志底部面板 (1.00) `state_下载日志底部面板`
│  ├─ [✓] 播放记录·空状态 (1.00) `state_播放记录_空状态`
│  ├─ [✓] 收藏·空状态 (1.00) `state_收藏_空状态`
│  ├─ [✓] 统计·订阅视图 (1.00) `state_统计_订阅视图`
│  ├─ [✓] 统计·年度视图 (1.00) `state_统计_年度视图`
│  ├─ [✓] 统计·下载存储视图 (1.00) `state_统计_下载存储视图`
│  ├─ [✓] state_自定义导航对话框 (0.50) `state_自定义导航对话框`
│  │    ─MUTATES→ state_主导航_订阅置顶
│  ├─ [✓] state_主导航_订阅置顶 (0.50) `state_主导航_订阅置顶`
│  ├─ [✓] state_全局设置首页 (0.50) `state_全局设置首页`
│  │    ─TRANSITIONS_TO→ state_用户界面设置_主题与单集信息
│  │    ─TRANSITIONS_TO→ state_播放设置_上部
│  │    ─TRANSITIONS_TO→ state_下载设置_上部
│  │    ─TRANSITIONS_TO→ state_设置搜索_无结果
│  ├─ [✓] state_用户界面设置_主题与单集信息 (0.50) `state_用户界面设置_主题与单集信息`
│  │    ─MUTATES→ state_用户界面设置_深色主题
│  ├─ [✓] state_用户界面设置_深色主题 (0.50) `state_用户界面设置_深色主题`
│  ├─ [✓] state_播放设置_上部 (0.50) `state_播放设置_上部`
│  │    ─REVEALS→ state_快进跳转时间选择
│  ├─ [✓] state_快进跳转时间选择 (0.50) `state_快进跳转时间选择`
│  │    ─MUTATES→ state_播放设置_上部
│  ├─ [✓] state_下载设置_上部 (0.50) `state_下载设置_上部`
│  │    ─TRANSITIONS_TO→ state_自动下载设置
│  ├─ [✓] state_自动下载设置 (0.50) `state_自动下载设置`
│  ├─ [✓] state_设置搜索_无结果 (0.50) `state_设置搜索_无结果`
│  ├─ [✓] state_首页_自定义导航持久化 (0.50) `state_首页_自定义导航持久化`
├─ Features
│  ├─ [✓] 底部主导航 (1.00) `feature_底部主导航`
│  ├─ [✓] 播客建议离线错误与重试 (1.00) `feature_播客建议离线错误与重试`
│  ├─ [✓] 播客关键词搜索 (1.00) `feature_播客关键词搜索`
│  ├─ [✓] RSS 地址输入校验 (1.00) `feature_rss_地址输入校验`
│  ├─ [✓] 添加本地媒体文件夹 (1.00) `feature_添加本地媒体文件夹`
│  ├─ [✓] 以本地文件夹创建播客 (1.00) `feature_以本地文件夹创建播客`
│  │    ─MUTATES→ 播客订阅
│  ├─ [✓] 查看播客信息 (1.00) `feature_查看播客信息`
│  ├─ [✓] 播客单集条件筛选 (1.00) `feature_播客单集条件筛选`
│  ├─ [✓] 应用单集筛选条件 (1.00) `feature_应用单集筛选条件`
│  │    ─MUTATES→ 播客单集筛选条件
│  ├─ [✓] 播客级设置入口 (1.00) `feature_播客级设置入口`
│  ├─ [✓] 重命名播客 (1.00) `feature_重命名播客`
│  ├─ [✓] 保存自定义播客名称 (1.00) `feature_保存自定义播客名称`
│  │    ─MUTATES→ 播客显示名称
│  ├─ [✓] 订阅与自定义名称持久化 (1.00) `feature_订阅与自定义名称持久化`
│  ├─ [✓] 批量管理订阅 (1.00) `feature_批量管理订阅`
│  ├─ [✓] 为订阅批量编辑标签 (1.00) `feature_为订阅批量编辑标签`
│  ├─ [✓] 创建并应用订阅标签 (1.00) `feature_创建并应用订阅标签`
│  │    ─MUTATES→ 订阅标签
│  ├─ [✓] 按标签筛选订阅 (1.00) `feature_按标签筛选订阅`
│  ├─ [✓] 单集排序 (1.00) `feature_单集排序`
│  ├─ [✓] 查看与清空下载日志 (1.00) `feature_查看与清空下载日志`
│  ├─ [✓] 切换统计维度 (1.00) `feature_切换统计维度`
│  ├─ [✓] feature_自定义主导航 (0.50) `feature_自定义主导航`
│  ├─ [✓] feature_保存导航排序 (0.50) `feature_保存导航排序`
│  ├─ [✓] feature_全局设置分类与搜索 (0.50) `feature_全局设置分类与搜索`
│  ├─ [✓] feature_用户界面偏好 (0.50) `feature_用户界面偏好`
│  ├─ [✓] feature_即时切换深色主题 (0.50) `feature_即时切换深色主题`
│  ├─ [✓] feature_播放行为设置 (0.50) `feature_播放行为设置`
│  ├─ [✓] feature_自定义快进秒数 (0.50) `feature_自定义快进秒数`
│  ├─ [✓] feature_保存快进跳转秒数 (0.50) `feature_保存快进跳转秒数`
│  ├─ [✓] feature_下载与自动化设置 (0.50) `feature_下载与自动化设置`
│  ├─ [✓] feature_自动下载细则 (0.50) `feature_自动下载细则`
│  ├─ [✓] feature_搜索设置项 (0.50) `feature_搜索设置项`
│  ├─ [✓] feature_导航顺序跨重启持久化 (0.50) `feature_导航顺序跨重启持久化`
├─ Data
│  ├─ [✓] 播客订阅 (0.90) `data_播客订阅`
│  │    ─PERSISTS_TO→ 订阅·单个播客网格
│  ├─ [✓] 播客单集筛选条件 (0.90) `data_播客单集筛选条件`
│  ├─ [✓] 播客显示名称 (1.00) `data_播客显示名称`
│  │    ─PERSISTS_TO→ 订阅·单个播客网格
│  ├─ [✓] 订阅标签 (1.00) `data_订阅标签`
│  │    ─ENABLES→ 订阅·标签筛选网格
│  ├─ [✓] data_自定义导航顺序 (0.50) `data_自定义导航顺序`
│  │    ─PERSISTS_TO→ state_首页_自定义导航持久化
│  ├─ [✓] data_主题模式 (0.50) `data_主题模式`
│  ├─ [✓] data_快进跳转秒数 (0.50) `data_快进跳转秒数`
```

## Features
### 底部主导航 `feature_底部主导航`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 点击底部五个入口在首页、队列、收件箱、订阅和更多之间切换，并突出当前入口。
- evidence: step 1 (frame 1 → 2)

### 播客建议离线错误与重试 `feature_播客建议离线错误与重试`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 设备离线时点击“显示建议”，原建议区域显示主机解析失败消息和“重试”按钮。
- evidence: step 6 (frame 11 → 12)

### 播客关键词搜索 `feature_播客关键词搜索`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 添加播客页的搜索框接受文字；提交后进入结果页，顶部可返回或清除查询。离线测试显示无结果。
- evidence: step 8 (frame 15 → 16)

### RSS 地址输入校验 `feature_rss_地址输入校验`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 空 RSS 地址无法提交；输入框以红色边框、错误图标和文案提示地址无效。
- evidence: step 12 (frame 23 → 24)

### 添加本地媒体文件夹 `feature_添加本地媒体文件夹`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 打开系统文件夹选择器，要求选择根目录下的具体子文件夹后授权。
- evidence: step 20 (frame 39 → 40)

### 以本地文件夹创建播客 `feature_以本地文件夹创建播客`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 选择并允许访问一个具体本地文件夹后，App 将其作为“本地文件夹”播客创建，并打开详情页。
- evidence: step 23 (frame 45 → 46)

### 查看播客信息 `feature_查看播客信息`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 播客详情的信息图标切换到统计、描述和来源地址视图。
- evidence: step 24 (frame 47 → 48)

### 播客单集条件筛选 `feature_播客单集条件筛选`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 播客详情的漏斗图标展开多条件底部面板；条件按互补成对呈现，底部可重置或确认。
- evidence: step 29 (frame 57 → 58)

### 应用单集筛选条件 `feature_应用单集筛选条件`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 点击一个筛选条件后该分段变蓝；确认关闭面板，详情页显示“已筛选的”标记。
- evidence: step 30 (frame 59 → 60)

### 播客级设置入口 `feature_播客级设置入口`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 每个播客详情的齿轮图标打开独立设置页，可配置显示、播放与自动化行为。
- evidence: step 32 (frame 63 → 64)

### 重命名播客 `feature_重命名播客`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 从播客设置打开重命名对话框，可重置、取消或确定新的显示名称。
- evidence: step 37 (frame 73 → 74)

### 保存自定义播客名称 `feature_保存自定义播客名称`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 在重命名对话框替换名称并确定后，返回详情显示新名称。
- evidence: step 39 (frame 77 → 78)

### 订阅与自定义名称持久化 `feature_订阅与自定义名称持久化`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 重启 App 后，订阅页仍显示先前添加的本地播客及自定义名称 Demo Podcast。
- evidence: step 42 (frame 83 → 84)

### 批量管理订阅 `feature_批量管理订阅`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 长按订阅卡片进入多选，可批量编辑标签、清理收件箱、取消订阅/归档或切换保持更新。
- evidence: step 44 (frame 87 → 88)

### 为订阅批量编辑标签 `feature_为订阅批量编辑标签`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 在订阅多选模式打开标签对话框，可添加并确认标签。
- evidence: step 47 (frame 93 → 94)

### 创建并应用订阅标签 `feature_创建并应用订阅标签`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 在标签对话框输入 Work 并点加号生成可删除标签项，确定后订阅页顶部出现 Work 过滤芯片。
- evidence: step 49 (frame 97 → 98)

### 按标签筛选订阅 `feature_按标签筛选订阅`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 点击订阅页顶部标签芯片后，勾选从“全部”移到该标签，并仅显示匹配播客。
- evidence: step 52 (frame 103 → 104)

### 单集排序 `feature_单集排序`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 排序面板可在“按时长”和“按日期”之间切换；再次点击当前条件可在升序与降序间切换，三角方向即时更新。
- evidence: step 56 (frame 111 → 112)

### 查看与清空下载日志 `feature_查看与清空下载日志`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 下载页历史图标展开日志底部面板，展示任务状态、名称、类型和相对时间，并提供清空入口。
- evidence: step 64 (frame 127 → 128)

### 切换统计维度 `feature_切换统计维度`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 统计页可切换订阅总览、年度每月播放时间和下载存储用量三个维度。
- evidence: step 72 (frame 143 → 144)

### feature_自定义主导航 `feature_自定义主导航`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- evidence: step 75 (frame 149 → 150)

### feature_保存导航排序 `feature_保存导航排序`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- evidence: step 76 (frame 151 → 152)

### feature_全局设置分类与搜索 `feature_全局设置分类与搜索`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- evidence: step 79 (frame 157 → 158)

### feature_用户界面偏好 `feature_用户界面偏好`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- evidence: step 80 (frame 159 → 160)

### feature_即时切换深色主题 `feature_即时切换深色主题`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- evidence: step 81 (frame 161 → 162)

### feature_播放行为设置 `feature_播放行为设置`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- evidence: step 87 (frame 171 → 172)

### feature_自定义快进秒数 `feature_自定义快进秒数`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- evidence: step 88 (frame 173 → 174)

### feature_保存快进跳转秒数 `feature_保存快进跳转秒数`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- evidence: step 89 (frame 175 → 176)

### feature_下载与自动化设置 `feature_下载与自动化设置`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- evidence: step 94 (frame 182 → 183)

### feature_自动下载细则 `feature_自动下载细则`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- evidence: step 95 (frame 184 → 185)

### feature_搜索设置项 `feature_搜索设置项`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- evidence: step 101 (frame 192 → 193)

### feature_导航顺序跨重启持久化 `feature_导航顺序跨重启持久化`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- evidence: step 103 (frame 195 → 196)
