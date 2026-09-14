# Functional Topology — vinyl

- session: `sess_20260912_121955_9543c2`
- generated: 2026-09-12T12:49:37.243018+00:00
- coverage: 43 states · 28 features · 3 data · 5 edges (confirmed ratio 100%, 129 actions)

## Graph
```
vinyl
├─ States
│  ├─ [✓] 首次启动通知权限弹窗 (1.00) `state_首次启动通知权限弹窗`
│  ├─ [✓] 首次启动媒体访问权限弹窗 (1.00) `state_首次启动媒体访问权限弹窗`
│  ├─ [✓] 首次启动照片与视频权限弹窗 (1.00) `state_首次启动照片与视频权限弹窗`
│  ├─ [✓] 歌曲列表主页 (1.00) `state_歌曲列表主页`
│  ├─ [✓] 专辑网格 (1.00) `state_专辑网格`
│  ├─ [✓] 艺术家空状态 (1.00) `state_艺术家空状态`
│  ├─ [✓] 音乐类型列表 (1.00) `state_音乐类型列表`
│  ├─ [✓] 音乐类型详情 (1.00) `state_音乐类型详情`
│  ├─ [✓] 迷你播放器已载入歌曲 (1.00) `state_迷你播放器已载入歌曲`
│  ├─ [✓] 完整播放页 (1.00) `state_完整播放页`
│  ├─ [✓] 播放页更多菜单 (1.00) `state_播放页更多菜单`
│  ├─ [✓] 保存播放队列为新播放列表对话框 (1.00) `state_保存播放队列为新播放列表对话框`
│  ├─ [✓] 睡眠定时器设置对话框 (1.00) `state_睡眠定时器设置对话框`
│  ├─ [✓] 播放列表总览 (1.00) `state_播放列表总览`
│  ├─ [✓] 播放列表详情 (1.00) `state_播放列表详情`
│  ├─ [✓] 歌曲行更多菜单 (1.00) `state_歌曲行更多菜单`
│  ├─ [✓] 歌曲详情对话框 (1.00) `state_歌曲详情对话框`
│  ├─ [✓] 修改音频文件系统确认 (1.00) `state_修改音频文件系统确认`
│  ├─ [✓] 音乐标签编辑器 (1.00) `state_音乐标签编辑器`
│  ├─ [✓] 播放列表详情更多菜单 (1.00) `state_播放列表详情更多菜单`
│  ├─ [✓] 重命名播放列表对话框 (1.00) `state_重命名播放列表对话框`
│  ├─ [✓] 删除播放列表确认 (1.00) `state_删除播放列表确认`
│  ├─ [✓] 全局搜索初始状态 (1.00) `state_全局搜索初始状态`
│  ├─ [✓] 搜索命中结果 (1.00) `state_搜索命中结果`
│  ├─ [✓] 搜索无结果状态 (1.00) `state_搜索无结果状态`
│  ├─ [✓] 侧边导航抽屉 (1.00) `state_侧边导航抽屉`
│  ├─ [✓] 文件夹浏览空状态 (1.00) `state_文件夹浏览空状态`
│  ├─ [✓] 文件夹浏览目录列表 (1.00) `state_文件夹浏览目录列表`
│  ├─ [✓] 文件夹中的音频文件 (1.00) `state_文件夹中的音频文件`
│  ├─ [✓] 文件夹页更多菜单 (1.00) `state_文件夹页更多菜单`
│  ├─ [✓] 文件夹排序方式菜单 (1.00) `state_文件夹排序方式菜单`
│  ├─ [✓] 设置页：媒体库与颜色 (1.00) `state_设置页_媒体库与颜色`
│  ├─ [✓] 设置页：通知与正在播放 (1.00) `state_设置页_通知与正在播放`
│  ├─ [✓] 设置页：图片、声音与播放列表 (1.00) `state_设置页_图片_声音与播放列表`
│  ├─ [✓] 设置页：智能列表与迁移 (1.00) `state_设置页_智能列表与迁移`
│  ├─ [✓] 媒体库类别配置对话框 (1.00) `state_媒体库类别配置对话框`
│  ├─ [✓] 媒体库顶部更多菜单 (1.00) `state_媒体库顶部更多菜单`
│  ├─ [✓] 关于页上半部 (1.00) `state_关于页上半部`
│  ├─ [✓] 关于页贡献者信息 (1.00) `state_关于页贡献者信息`
│  ├─ [✓] 更新日志对话框 (1.00) `state_更新日志对话框`
│  ├─ [✓] 重新扫描媒体库确认 (0.99) `state_重新扫描媒体库确认`
│  ├─ [✓] 冷启动恢复上次页面与用户数据 (0.99) `state_冷启动恢复上次页面与用户数据`
│  ├─ [✓] 添加歌曲到播放列表选择器 (0.99) `state_添加歌曲到播放列表选择器`
├─ Features
│  ├─ [✓] 分类标签导航 (1.00) `feature_分类标签导航`
│  ├─ [✓] 按音乐类型浏览歌曲 (1.00) `feature_按音乐类型浏览歌曲`
│  ├─ [✓] 载入歌曲到播放器 (1.00) `feature_载入歌曲到播放器`
│  ├─ [✓] 展开完整播放页 (1.00) `feature_展开完整播放页`
│  ├─ [✓] 收藏当前歌曲 (1.00) `feature_收藏当前歌曲`
│  │    ─MUTATES→ 歌曲收藏状态
│  ├─ [✓] 保存当前队列 (0.90) `feature_保存当前队列`
│  ├─ [✓] 播放列表名称必填 (1.00) `feature_播放列表名称必填`
│  ├─ [✓] 创建播放列表并保存队列 (1.00) `feature_创建播放列表并保存队列`
│  │    ─MUTATES→ 播放列表
│  ├─ [✓] 设置睡眠定时器 (1.00) `feature_设置睡眠定时器`
│  ├─ [✓] 收藏写入收藏夹 (1.00) `feature_收藏写入收藏夹`
│  ├─ [✓] 查看歌曲文件与标签详情 (1.00) `feature_查看歌曲文件与标签详情`
│  ├─ [✓] 拒绝系统修改授权后仍可查看编辑表单 (1.00) `feature_拒绝系统修改授权后仍可查看编辑表单`
│  ├─ [✓] 未授权时保存标签被阻止 (0.90) `feature_未授权时保存标签被阻止`
│  ├─ [✓] 授权后打开可编辑音乐标签表单 (0.90) `feature_授权后打开可编辑音乐标签表单`
│  ├─ [✓] 保存队列结果可浏览 (1.00) `feature_保存队列结果可浏览`
│  ├─ [✓] 删除播放列表需确认 (1.00) `feature_删除播放列表需确认`
│  ├─ [✓] 按文本搜索媒体库 (1.00) `feature_按文本搜索媒体库`
│  ├─ [✓] 搜索无匹配反馈 (1.00) `feature_搜索无匹配反馈`
│  ├─ [✓] 按文件夹浏览音频 (1.00) `feature_按文件夹浏览音频`
│  ├─ [✓] 配置媒体库标签可见性 (1.00) `feature_配置媒体库标签可见性`
│  │    ─MUTATES→ 应用设置
│  ├─ [✓] 媒体库标签设置即时生效 (1.00) `feature_媒体库标签设置即时生效`
│  ├─ [✓] 新建空播放列表 (1.00) `feature_新建空播放列表`
│  ├─ [✓] 重新扫描媒体库前需确认 (0.98) `feature_重新扫描媒体库前需确认`
│  ├─ [✓] 首次启动连续权限请求 (0.99) `feature_首次启动连续权限请求`
│  ├─ [✓] 播放列表重命名入口 (0.96) `feature_播放列表重命名入口`
│  ├─ [✓] 迷你播放器跨页面保留 (0.97) `feature_迷你播放器跨页面保留`
│  ├─ [✓] 用户状态冷启动持久化 (0.99) `feature_用户状态冷启动持久化`
│  ├─ [✓] 歌曲可加入已有或新建播放列表 (0.99) `feature_歌曲可加入已有或新建播放列表`
├─ Data
│  ├─ [✓] 歌曲收藏状态 (1.00) `data_歌曲收藏状态`
│  │    ─REVEALS→ 播放列表总览
│  ├─ [✓] 播放列表 (1.00) `data_播放列表`
│  │    ─PERSISTS_TO→ 冷启动恢复上次页面与用户数据
│  ├─ [✓] 应用设置 (1.00) `data_应用设置`
```

## Features
### 分类标签导航 `feature_分类标签导航`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 歌曲、专辑、艺术家、音乐类型和播放列表可从顶部横向标签切换。
- evidence: step 4 (frame 7 → 8)

### 按音乐类型浏览歌曲 `feature_按音乐类型浏览歌曲`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 点击音乐类型条目进入详情页，并展示该类型包含的歌曲。
- evidence: step 8 (frame 15 → 16)

### 载入歌曲到播放器 `feature_载入歌曲到播放器`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 点击歌曲条目后，底部迷你播放器出现并载入该歌曲。
- evidence: step 9 (frame 17 → 18)

### 展开完整播放页 `feature_展开完整播放页`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 迷你播放器左侧上箭头打开全屏播放页，展示控制器、歌曲信息与播放队列。
- evidence: step 16 (frame 28 → 29)

### 收藏当前歌曲 `feature_收藏当前歌曲`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 播放页空心心形可切换为实心，表示当前歌曲已收藏。
- evidence: step 17 (frame 30 → 31)

### 保存当前队列 `feature_保存当前队列`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- 播放页可将当前播放队列保存为一个命名的新播放列表。
- evidence: step 19 (frame 34 → 35)

### 播放列表名称必填 `feature_播放列表名称必填`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 新建播放列表对话框留空点击创建时，对话框保持打开且未创建。
- evidence: step 20 (frame 36 → 37)

### 创建播放列表并保存队列 `feature_创建播放列表并保存队列`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 输入非空名称并点击创建后，对话框关闭，当前队列保存为新播放列表。
- evidence: step 23 (frame 42 → 43)

### 设置睡眠定时器 `feature_设置睡眠定时器`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 可选择分钟数、可选“播完当前音乐”，点击设置后关闭对话框并启用定时。
- evidence: step 27 (frame 48 → 49)

### 收藏写入收藏夹 `feature_收藏写入收藏夹`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 在播放页收藏歌曲后，播放列表页的收藏夹计数变为 1。
- evidence: step 17 (frame 30 → 31)

### 查看歌曲文件与标签详情 `feature_查看歌曲文件与标签详情`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 从歌曲行菜单打开只读详情弹窗，集中显示文件文件的文件属性和音乐标签。
- evidence: step 35 (frame 61 → 62)

### 拒绝系统修改授权后仍可查看编辑表单 `feature_拒绝系统修改授权后仍可查看编辑表单`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 在系统询问修改音频文件时选择拒绝，弹窗关闭并显示标签编辑器表单。
- evidence: step 39 (frame 67 → 68)

### 未授权时保存标签被阻止 `feature_未授权时保存标签被阻止`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- 拒绝系统修改授权后编辑标题并点击保存，编辑器保持打开且未退出。
- evidence: step 44 (frame 75 → 76)

### 授权后打开可编辑音乐标签表单 `feature_授权后打开可编辑音乐标签表单`
- status: ClaimStatus.CONFIRMED · confidence: 0.90
- 在系统修改确认中选择允许后进入音乐标签编辑器，支持对标签字段进行输入。
- evidence: step 48 (frame 81 → 82)

### 保存队列结果可浏览 `feature_保存队列结果可浏览`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 通过保存队列创建的 Road Mix 在播放列表总览出现，进入后包含当前歌曲。
- evidence: step 23 (frame 42 → 43)

### 删除播放列表需确认 `feature_删除播放列表需确认`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 选择删除播放列表时先显示命名确认对话框，不会立即删除。
- evidence: step 63 (frame 102 → 103)

### 按文本搜索媒体库 `feature_按文本搜索媒体库`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 在全局搜索输入并提交标题片段后，显示匹配的歌曲结果。
- evidence: step 67 (frame 109 → 110)

### 搜索无匹配反馈 `feature_搜索无匹配反馈`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 不存在匹配媒体时明确显示“没有找到结果”。
- evidence: step 72 (frame 116 → 117)

### 按文件夹浏览音频 `feature_按文件夹浏览音频`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 文件夹模式支持通过面包屑返回上级、进入目录，并在含媒体目录中显示音频文件。
- evidence: step 79 (frame 126 → 127)

### 配置媒体库标签可见性 `feature_配置媒体库标签可见性`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 媒体库类别对话框允许单独勾选显示的标签，并提供拖动柄调整顺序。
- evidence: step 96 (frame 157 → 158)

### 媒体库标签设置即时生效 `feature_媒体库标签设置即时生效`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 取消艺术家类别并确认后，返回媒体库时顶部仅保留歌曲、专辑、音乐类型、播放列表四个标签。
- evidence: step 97 (frame 158 → 159)

### 新建空播放列表 `feature_新建空播放列表`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 媒体库顶部更多菜单可打开命名对话框新建播放列表，输入非空名称后创建；与保存队列入口共用同类对话框。
- evidence: step 102 (frame 167 → 168)

### 重新扫描媒体库前需确认 `feature_重新扫描媒体库前需确认`
- status: ClaimStatus.CONFIRMED · confidence: 0.98
- 从侧边栏点击“重新扫描媒体库”后，先展示影响说明和取消/继续按钮，不会直接开始重建。
- evidence: step 112 (frame 183 → 184)

### 首次启动连续权限请求 `feature_首次启动连续权限请求`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 首次启动依次请求通知、音频媒体以及照片与视频访问权限，完成后进入媒体库。
- evidence: step 1 (frame 1 → 2)

### 播放列表重命名入口 `feature_播放列表重命名入口`
- status: ClaimStatus.CONFIRMED · confidence: 0.96
- 用户播放列表的更多菜单可打开带当前名称预填的重命名对话框。
- evidence: step 59 (frame 96 → 97)

### 迷你播放器跨页面保留 `feature_迷你播放器跨页面保留`
- status: ClaimStatus.CONFIRMED · confidence: 0.97
- 加载歌曲后，迷你播放器在音乐类型、播放列表、文件夹等主页面底部持续显示，并可再次展开。
- evidence: step 32 (frame 55 → 56)

### 用户状态冷启动持久化 `feature_用户状态冷启动持久化`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 自建播放列表、收藏状态、当前队列与上次媒体库页面在应用重启后保持。
- evidence: step 124 (frame 200 → 201)

### 歌曲可加入已有或新建播放列表 `feature_歌曲可加入已有或新建播放列表`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 歌曲操作菜单提供播放列表选择器，支持新建列表或选择现有收藏夹/用户列表。
- evidence: step 128 (frame 207 → 208)
