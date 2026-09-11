# Functional Topology — fossify_gallery

- session: `sess_20260909_034535_e4d491`
- generated: 2026-09-09T04:26:13.976239+00:00
- coverage: 37 states · 33 features · 2 data · 4 edges (confirmed ratio 100%, 84 actions)

## Graph
```
fossify_gallery
├─ States
│  ├─ [✓] 媒体文件夹空状态 (1.00) `state_媒体文件夹空状态`
│  ├─ [✓] 文件夹搜索无结果 (1.00) `state_文件夹搜索无结果`
│  ├─ [✓] 所有可见文件媒体网格 (1.00) `state_所有可见文件媒体网格`
│  ├─ [✓] 图片查看器 (1.00) `state_图片查看器`
│  ├─ [✓] 图片旋转菜单 (1.00) `state_图片旋转菜单`
│  ├─ [✓] 旋转后另存为对话框 (1.00) `state_旋转后另存为对话框`
│  ├─ [✓] 媒体文件夹列表 (1.00) `state_媒体文件夹列表`
│  ├─ [✓] 主界面更多菜单 (1.00) `state_主界面更多菜单`
│  ├─ [✓] 设置页顶部 (1.00) `state_设置页顶部`
│  ├─ [✓] 设置页视频与缩略图 (1.00) `state_设置页视频与缩略图`
│  ├─ [✓] 设置页全屏显示 (1.00) `state_设置页全屏显示`
│  ├─ [✓] 设置页图像手势与安全 (1.00) `state_设置页图像手势与安全`
│  ├─ [✓] 设置页底部按钮回收站与迁移 (1.00) `state_设置页底部按钮回收站与迁移`
│  ├─ [✓] 设置页迁移底部 (1.00) `state_设置页迁移底部`
│  ├─ [✓] 关于页 (1.00) `state_关于页`
│  ├─ [✓] 文件夹排序对话框 (1.00) `state_文件夹排序对话框`
│  ├─ [✓] 媒体类型过滤对话框 (1.00) `state_媒体类型过滤对话框`
│  ├─ [✓] 文件夹视图类型对话框 (1.00) `state_文件夹视图类型对话框`
│  ├─ [✓] 文件夹列表视图 (1.00) `state_文件夹列表视图`
│  ├─ [✓] 文件夹内容网格 (1.00) `state_文件夹内容网格`
│  ├─ [✓] 文件夹内容更多菜单 (1.00) `state_文件夹内容更多菜单`
│  ├─ [✓] 回收站空状态 (1.00) `state_回收站空状态`
│  ├─ [✓] 文件夹媒体排序对话框 (1.00) `state_文件夹媒体排序对话框`
│  ├─ [✓] 文件夹媒体分组对话框 (1.00) `state_文件夹媒体分组对话框`
│  ├─ [✓] 幻灯片配置对话框 (1.00) `state_幻灯片配置对话框`
│  ├─ [✓] 视频查看器与播放控件 (1.00) `state_视频查看器与播放控件`
│  ├─ [✓] 收藏虚拟文件夹 (1.00) `state_收藏虚拟文件夹`
│  ├─ [✓] 收藏内容网格 (1.00) `state_收藏内容网格`
│  ├─ [✓] 媒体多选模式 (1.00) `state_媒体多选模式`
│  ├─ [✓] 媒体选择更多菜单 (1.00) `state_媒体选择更多菜单`
│  ├─ [✓] 重命名媒体对话框 (1.00) `state_重命名媒体对话框`
│  ├─ [✓] 调整图片大小对话框 (1.00) `state_调整图片大小对话框`
│  ├─ [✓] 移动到回收站确认 (1.00) `state_移动到回收站确认`
│  ├─ [✓] 回收站项目选择模式 (1.00) `state_回收站项目选择模式`
│  ├─ [✓] 回收站选择更多菜单 (1.00) `state_回收站选择更多菜单`
│  ├─ [✓] state_图片查看器更多菜单 (1.00) `state_图片查看器更多菜单`
│  ├─ [✓] state_隐藏图片查看器 (1.00) `state_隐藏图片查看器`
├─ Features
│  ├─ [✓] 文件夹搜索 (1.00) `feature_文件夹搜索`
│  ├─ [✓] 切换到所有可见文件搜索 (1.00) `feature_切换到所有可见文件搜索`
│  ├─ [✓] 打开图片查看器 (1.00) `feature_打开图片查看器`
│  ├─ [✓] 图片旋转选项 (1.00) `feature_图片旋转选项`
│  ├─ [✓] 旋转后另存为 (1.00) `feature_旋转后另存为`
│  ├─ [✓] 主界面更多操作 (1.00) `feature_主界面更多操作`
│  ├─ [✓] 打开设置 (1.00) `feature_打开设置`
│  ├─ [✓] 打开关于页 (1.00) `feature_打开关于页`
│  ├─ [✓] 配置文件夹排序 (1.00) `feature_配置文件夹排序`
│  ├─ [✓] 过滤媒体类型 (1.00) `feature_过滤媒体类型`
│  ├─ [✓] 切换文件夹视图 (1.00) `feature_切换文件夹视图`
│  ├─ [✓] 应用列表视图 (1.00) `feature_应用列表视图`
│  ├─ [✓] 浏览文件夹内容 (1.00) `feature_浏览文件夹内容`
│  ├─ [✓] 文件夹内容更多操作 (1.00) `feature_文件夹内容更多操作`
│  ├─ [✓] 打开回收站 (1.00) `feature_打开回收站`
│  ├─ [✓] 配置文件夹媒体排序 (1.00) `feature_配置文件夹媒体排序`
│  ├─ [✓] 配置媒体分组 (1.00) `feature_配置媒体分组`
│  ├─ [✓] 配置幻灯片 (1.00) `feature_配置幻灯片`
│  ├─ [✓] 视频内置播放 (0.95) `feature_视频内置播放`
│  ├─ [✓] 收藏媒体 (1.00) `feature_收藏媒体`
│  │    ─MUTATES→ 收藏状态
│  ├─ [✓] 收藏持久化与聚合 (1.00) `feature_收藏持久化与聚合`
│  ├─ [✓] 浏览收藏内容 (1.00) `feature_浏览收藏内容`
│  ├─ [✓] 长按选择媒体 (1.00) `feature_长按选择媒体`
│  ├─ [✓] 媒体批量更多操作 (1.00) `feature_媒体批量更多操作`
│  ├─ [✓] 重命名媒体 (1.00) `feature_重命名媒体`
│  ├─ [✓] 调整图片大小 (1.00) `feature_调整图片大小`
│  ├─ [✓] 删除前确认 (1.00) `feature_删除前确认`
│  ├─ [✓] 移入回收站 (1.00) `feature_移入回收站`
│  │    ─MUTATES→ 回收站项目
│  ├─ [✓] 恢复回收站媒体 (1.00) `feature_恢复回收站媒体`
│  │    ─MUTATES→ 回收站项目
│  ├─ [✓] 恢复保留收藏状态 (1.00) `feature_恢复保留收藏状态`
│  ├─ [✓] feature_图片查看器更多操作 (1.00) `feature_图片查看器更多操作`
│  ├─ [✓] feature_隐藏媒体 (1.00) `feature_隐藏媒体`
│  ├─ [✓] feature_取消隐藏媒体 (1.00) `feature_取消隐藏媒体`
├─ Data
│  ├─ [✓] 收藏状态 (1.00) `data_收藏状态`
│  │    ─PERSISTS_TO→ 收藏虚拟文件夹
│  ├─ [✓] 回收站项目 (1.00) `data_回收站项目`
```

## Features
### 文件夹搜索 `feature_文件夹搜索`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 输入查询后即时筛选；当前目录范围无结果时显示空结果并允许扩展搜索范围。
- evidence: step 2 (frame 3 → 4)

### 切换到所有可见文件搜索 `feature_切换到所有可见文件搜索`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 从文件夹搜索的空结果链接切换为所有可见媒体的文件级搜索视图，并展示全局媒体。
- evidence: step 3 (frame 5 → 6)

### 打开图片查看器 `feature_打开图片查看器`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 点击图片缩略图进入沉浸式查看器并显示文件名及操作栏。
- evidence: step 4 (frame 7 → 8)

### 图片旋转选项 `feature_图片旋转选项`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 顶部旋转菜单提供三种旋转方向。
- evidence: step 5 (frame 9 → 10)

### 旋转后另存为 `feature_旋转后另存为`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 选择旋转方向后图片立即预览为旋转效果并出现勾选；点击勾选打开另存为对话框。
- evidence: step 6 (frame 11 → 12)

### 主界面更多操作 `feature_主界面更多操作`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 更多菜单汇集排序、过滤、视图、隐藏/排除项目、新建文件夹、列数、设置和关于入口。
- evidence: step 14 (frame 27 → 28)

### 打开设置 `feature_打开设置`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 从主界面更多菜单进入分组设置页。
- evidence: step 15 (frame 29 → 30)

### 打开关于页 `feature_打开关于页`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 主界面更多菜单可打开关于页查看支持、社区与法律链接。
- evidence: step 23 (frame 45 → 46)

### 配置文件夹排序 `feature_配置文件夹排序`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 可选择八类排序键与升降序并确认应用。
- evidence: step 27 (frame 52 → 53)

### 过滤媒体类型 `feature_过滤媒体类型`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 可按图片、视频、GIF、RAW、SVG 与竖向条件控制媒体显示。
- evidence: step 30 (frame 57 → 58)

### 切换文件夹视图 `feature_切换文件夹视图`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 支持网格/列表两种视图及实际文件夹分组选项。
- evidence: step 33 (frame 62 → 63)

### 应用列表视图 `feature_应用列表视图`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 确认列表视图后主界面立即改为缩略图、名称、数量和路径的行式布局。
- evidence: step 35 (frame 65 → 66)

### 浏览文件夹内容 `feature_浏览文件夹内容`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 点击文件夹条目进入该文件夹的媒体网格。
- evidence: step 36 (frame 67 → 68)

### 文件夹内容更多操作 `feature_文件夹内容更多操作`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 文件夹内提供过滤、视图、隐藏项目、回收站、分组、默认文件夹、新建、列数、幻灯片和设置入口。
- evidence: step 37 (frame 69 → 70)

### 打开回收站 `feature_打开回收站`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 可从文件夹内容菜单进入回收站查看已删除项目；空时显示明确空状态。
- evidence: step 38 (frame 71 → 72)

### 配置文件夹媒体排序 `feature_配置文件夹媒体排序`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 文件夹内可设置排序键、方向及是否仅应用于当前文件夹。
- evidence: step 40 (frame 75 → 76)

### 配置媒体分组 `feature_配置媒体分组`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 文件夹媒体可独立于排序按日期、类型或扩展名分组，并控制顺序和组标题计数。
- evidence: step 43 (frame 80 → 81)

### 配置幻灯片 `feature_配置幻灯片`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 幻灯片支持间隔、过渡动画、媒体类型、顺序与循环设置。
- evidence: step 46 (frame 85 → 86)

### 视频内置播放 `feature_视频内置播放`
- status: ClaimStatus.CONFIRMED · confidence: 0.95
- 点击视频中央播放按钮后在查看器内播放，进度条与时间更新，结束时停在结尾并恢复播放按钮。
- evidence: step 48 (frame 89 → 90)

### 收藏媒体 `feature_收藏媒体`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 点击查看器底部星标可在收藏/取消收藏间切换；收藏后星标由轮廓变为实心。
- evidence: step 53 (frame 98 → 99)

### 收藏持久化与聚合 `feature_收藏持久化与聚合`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 收藏状态跨应用重启保留，并在主界面生成收藏虚拟文件夹聚合收藏媒体。
- evidence: step 54 (frame 100 → 101)

### 浏览收藏内容 `feature_浏览收藏内容`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 点击收藏虚拟文件夹进入收藏媒体网格。
- evidence: step 55 (frame 102 → 103)

### 长按选择媒体 `feature_长按选择媒体`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 长按媒体进入多选模式并显示选择计数与批量操作。
- evidence: step 59 (frame 110 → 111)

### 媒体批量更多操作 `feature_媒体批量更多操作`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 选中媒体后可访问属性、重命名、隐藏、复制/移动、快捷方式、打开方式、设置为、缩放、编辑、收藏和日期修复等操作。
- evidence: step 60 (frame 112 → 113)

### 重命名媒体 `feature_重命名媒体`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 单选媒体后可分别修改文件标题与扩展名。
- evidence: step 65 (frame 121 → 122)

### 调整图片大小 `feature_调整图片大小`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 可设置输出宽高、路径、文件名和格式后生成缩放版本。
- evidence: step 68 (frame 126 → 127)

### 删除前确认 `feature_删除前确认`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 删除操作默认先询问是否移至回收站，并提供本会话免询问和永久删除选项。
- evidence: step 70 (frame 130 → 131)

### 移入回收站 `feature_移入回收站`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 确认后媒体从当前文件夹消失并进入回收站。
- evidence: step 71 (frame 132 → 133)

### 恢复回收站媒体 `feature_恢复回收站媒体`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 选择回收站项目后点击“恢复所选文件”，项目从回收站移除并返回原文件夹。
- evidence: step 78 (frame 145 → 146)

### 恢复保留收藏状态 `feature_恢复保留收藏状态`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 从回收站恢复媒体后，它重新出现在原文件夹，收藏星标仍保留。
- evidence: step 79 (frame 147 → 148)

### feature_图片查看器更多操作 `feature_图片查看器更多操作`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 从图片查看器打开包含文件管理、显示、安全、编辑及幻灯片入口的更多操作菜单。
- evidence: step 81 (frame 150 → 151)

### feature_隐藏媒体 `feature_隐藏媒体`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 在图片查看器更多菜单点击隐藏后，文件名立即变为点开头的隐藏文件名。
- evidence: step 82 (frame 152 → 153)

### feature_取消隐藏媒体 `feature_取消隐藏媒体`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 隐藏媒体的更多菜单将入口改为取消隐藏，点击后恢复原文件名与收藏状态。
- evidence: step 84 (frame 156 → 157)
