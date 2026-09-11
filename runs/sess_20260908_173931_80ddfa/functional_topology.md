# Functional Topology — fossify_gallery

- session: `sess_20260908_173931_80ddfa`
- generated: 2026-09-08T18:33:46.410730+00:00
- coverage: 29 states · 20 features · 1 data · 7 edges (confirmed ratio 100%, 159 actions)

## Graph
```
fossify_gallery
├─ States
│  ├─ [✓] 相册文件夹首页 (0.98) `state_相册文件夹首页`
│  │    ─TRANSITIONS_TO→ 文件夹媒体网格
│  ├─ [✓] 文件夹媒体网格 (0.99) `state_文件夹媒体网格`
│  │    ─TRANSITIONS_TO→ 图片全屏查看器
│  │    ─ENABLES→ 媒体多选操作模式
│  ├─ [✓] 图片全屏查看器 (0.99) `state_图片全屏查看器`
│  │    ─TRANSITIONS_TO→ 视频全屏查看器
│  ├─ [✓] 视频全屏查看器 (0.99) `state_视频全屏查看器`
│  ├─ [✓] 含收藏的相册首页 (0.99) `state_含收藏的相册首页`
│  │    ─TRANSITIONS_TO→ 收藏媒体网格
│  ├─ [✓] 收藏媒体网格 (0.99) `state_收藏媒体网格`
│  ├─ [✓] 排序方式对话框 (0.99) `state_排序方式对话框`
│  ├─ [✓] 媒体网格更多菜单 (0.99) `state_媒体网格更多菜单`
│  ├─ [✓] 文件类型过滤对话框 (0.99) `state_文件类型过滤对话框`
│  ├─ [✓] 文件夹搜索状态 (0.99) `state_文件夹搜索状态`
│  ├─ [✓] 跨文件夹文件搜索 (0.99) `state_跨文件夹文件搜索`
│  ├─ [✓] 相册首页更多菜单 (0.99) `state_相册首页更多菜单`
│  ├─ [✓] 关于页面 (0.99) `state_关于页面`
│  ├─ [✓] 设置页面顶部 (0.99) `state_设置页面顶部`
│  ├─ [✓] 设置页面视频与缩略图 (0.99) `state_设置页面视频与缩略图`
│  ├─ [✓] 设置页面全屏显示 (0.99) `state_设置页面全屏显示`
│  ├─ [✓] 设置页面缩放与安全 (0.99) `state_设置页面缩放与安全`
│  ├─ [✓] 设置页面文件操作与回收站 (0.99) `state_设置页面文件操作与回收站`
│  ├─ [✓] 设置页面迁移 (0.99) `state_设置页面迁移`
│  ├─ [✓] 自定义外观页面 (0.99) `state_自定义外观页面`
│  ├─ [✓] 应用主题选择对话框 (0.99) `state_应用主题选择对话框`
│  ├─ [✓] 应用图标颜色选择器 (0.99) `state_应用图标颜色选择器`
│  ├─ [✓] 应用字体选择对话框 (0.99) `state_应用字体选择对话框`
│  ├─ [✓] 新建文件夹位置选择器 (0.99) `state_新建文件夹位置选择器`
│  ├─ [✓] 图片查看器更多菜单 (0.99) `state_图片查看器更多菜单`
│  ├─ [✓] 调整图片大小对话框 (0.99) `state_调整图片大小对话框`
│  ├─ [✓] 图片画面方向子菜单 (0.99) `state_图片画面方向子菜单`
│  ├─ [✓] 媒体多选操作模式 (0.99) `state_媒体多选操作模式`
│  ├─ [✓] 媒体多选更多菜单 (0.99) `state_媒体多选更多菜单`
├─ Features
│  ├─ [✓] 进入文件夹浏览媒体 (0.99) `feature_进入文件夹浏览媒体`
│  ├─ [✓] 打开图片全屏查看 (0.99) `feature_打开图片全屏查看`
│  ├─ [✓] 全屏媒体间左右滑动 (0.98) `feature_全屏媒体间左右滑动`
│  ├─ [✓] 视频播放与控制层 (0.99) `feature_视频播放与控制层`
│  ├─ [✓] 切换沉浸式控件 (0.98) `feature_切换沉浸式控件`
│  ├─ [✓] 收藏媒体 (0.99) `feature_收藏媒体`
│  │    ─MUTATES→ 收藏状态
│  ├─ [✓] 收藏状态重启持久化 (0.99) `feature_收藏状态重启持久化`
│  ├─ [✓] 浏览收藏集合 (0.99) `feature_浏览收藏集合`
│  ├─ [✓] 切换缩略图文件名 (0.97) `feature_切换缩略图文件名`
│  ├─ [✓] 设置媒体排序 (0.99) `feature_设置媒体排序`
│  ├─ [✓] 过滤显示的媒体类型 (0.99) `feature_过滤显示的媒体类型`
│  ├─ [✓] 实时搜索文件夹 (0.99) `feature_实时搜索文件夹`
│  ├─ [✓] 跨可见文件夹搜索媒体文件 (0.99) `feature_跨可见文件夹搜索媒体文件`
│  ├─ [✓] 自定义应用主题 (0.99) `feature_自定义应用主题`
│  ├─ [✓] 自定义应用图标颜色 (0.99) `feature_自定义应用图标颜色`
│  ├─ [✓] 选择新文件夹创建位置 (0.99) `feature_选择新文件夹创建位置`
│  ├─ [✓] 编辑图片需更高文件权限 (0.99) `feature_编辑图片需更高文件权限`
│  ├─ [✓] 调整图片尺寸并另存 (0.99) `feature_调整图片尺寸并另存`
│  ├─ [✓] 查看媒体信息需更高文件权限 (0.99) `feature_查看媒体信息需更高文件权限`
│  ├─ [✓] 长按进入媒体多选 (0.99) `feature_长按进入媒体多选`
├─ Data
│  ├─ [✓] 收藏状态 (0.99) `data_收藏状态`
│  │    ─PERSISTS_TO→ 含收藏的相册首页
```

## Features
### 进入文件夹浏览媒体 `feature_进入文件夹浏览媒体`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 从首页点击文件夹封面进入文件夹媒体网格。
- **Preconditions**:
  - 首页显示 BlackBoxBench 文件夹
- evidence: step 21 (frame 25 → 26)

### 打开图片全屏查看 `feature_打开图片全屏查看`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 从文件夹两列网格点击图片，进入全屏查看器并提供图片操作。
- **Preconditions**:
  - 文件夹媒体网格可见
- evidence: step 25 (frame 31 → 32)

### 全屏媒体间左右滑动 `feature_全屏媒体间左右滑动`
- status: ClaimStatus.CONFIRMED · confidence: 0.98
- 在全屏查看器横向滑动切换同一文件夹内相邻媒体。
- **Preconditions**:
  - 图片全屏查看器处于沉浸模式
- evidence: step 39 (frame 52 → 53)

### 视频播放与控制层 `feature_视频播放与控制层`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 点击视频中央播放，画面推进；再次点击显示或隐藏播放控制层。
- **Preconditions**:
  - 视频全屏查看器暂停
- evidence: step 41 (frame 55 → 56)

### 切换沉浸式控件 `feature_切换沉浸式控件`
- status: ClaimStatus.CONFIRMED · confidence: 0.98
- 点击媒体主体隐藏顶部与底部操作栏进入全屏沉浸模式，再点击可显示控制层。
- **Preconditions**:
  - 图片全屏查看器操作栏可见
- evidence: step 35 (frame 45 → 46)

### 收藏媒体 `feature_收藏媒体`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 点击全屏查看器底部星标，将当前媒体加入或移出收藏。
- **Preconditions**:
  - 视频全屏查看器控制层可见且星标为空心
- evidence: step 44 (frame 60 → 61)

### 收藏状态重启持久化 `feature_收藏状态重启持久化`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 收藏视频后重启应用，首页新增“收藏”虚拟文件夹并显示数量 1，证明收藏状态持久化。
- **Preconditions**:
  - sample_video.mp4 已收藏，底部星标实心
- evidence: step 46 (frame 63 → 64)

### 浏览收藏集合 `feature_浏览收藏集合`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 点击首页收藏虚拟文件夹，只显示已收藏媒体，并以缩略图星标标识。
- **Preconditions**:
  - 首页存在收藏文件夹，数量 1
- evidence: step 51 (frame 70 → 71)

### 切换缩略图文件名 `feature_切换缩略图文件名`
- status: ClaimStatus.CONFIRMED · confidence: 0.97
- 点击网格顶部布局图标切换媒体缩略图是否叠加文件名。
- **Preconditions**:
  - 收藏媒体网格可见
- evidence: step 53 (frame 73 → 74)

### 设置媒体排序 `feature_设置媒体排序`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 媒体网格可按名称、路径、大小、修改日期、拍摄日期或随机排序，并选升序/降序；可限制到当前文件夹。
- **Preconditions**:
  - 媒体网格可见
- evidence: step 55 (frame 77 → 78)

### 过滤显示的媒体类型 `feature_过滤显示的媒体类型`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 通过复选框限制网格显示的媒体类型与竖向条件。
- **Preconditions**:
  - 媒体网格更多菜单打开
- evidence: step 61 (frame 86 → 87)

### 实时搜索文件夹 `feature_实时搜索文件夹`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 首页搜索框按输入文本实时筛选文件夹，并可切换为跨可见文件夹的文件搜索。
- **Preconditions**:
  - 相册文件夹首页可见
- evidence: step 65 (frame 92 → 93)

### 跨可见文件夹搜索媒体文件 `feature_跨可见文件夹搜索媒体文件`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 搜索模式可从文件夹名称切换到所有可见文件夹内的媒体文件，结果按日期分组。
- **Preconditions**:
  - 文件夹搜索状态可见
- evidence: step 66 (frame 94 → 95)

### 自定义应用主题 `feature_自定义应用主题`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 用户可在预设主题和自定义主题间切换。
- **Preconditions**:
  - 自定义外观页面可见
- evidence: step 87 (frame 130 → 131)

### 自定义应用图标颜色 `feature_自定义应用图标颜色`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 通过预设色板修改启动器图标颜色，显示实时预览与十六进制值。
- **Preconditions**:
  - 自定义外观页面可见
- evidence: step 90 (frame 135 → 136)

### 选择新文件夹创建位置 `feature_选择新文件夹创建位置`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 新建文件夹流程先选择内部存储中的父目录。
- **Preconditions**:
  - 相册首页更多菜单可见
- evidence: step 101 (frame 150 → 151)

### 编辑图片需更高文件权限 `feature_编辑图片需更高文件权限`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 点击图片查看器底部编辑按钮时，在仅照片/视频权限条件下返回首页并弹出“请授权本应用访问您的所有文件”说明；说明可改用系统设置的媒体管理应用权限。
- **Preconditions**:
  - 图片全屏查看器，只有媒体访问权限
- evidence: step 110 (frame 163 → 164)

### 调整图片尺寸并另存 `feature_调整图片尺寸并另存`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 可输入宽高、输出目录、文件名和扩展名来调整图片大小。
- **Preconditions**:
  - 图片查看器更多菜单可见
- evidence: step 120 (frame 176 → 177)

### 查看媒体信息需更高文件权限 `feature_查看媒体信息需更高文件权限`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 点击图片查看器顶部信息按钮，在仅媒体权限条件下返回首页并弹出所有文件访问授权说明。
- **Preconditions**:
  - 图片全屏查看器，仅照片/视频权限
- evidence: step 131 (frame 192 → 193)

### 长按进入媒体多选 `feature_长按进入媒体多选`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 长按媒体缩略图选中项目并进入批量操作模式。
- **Preconditions**:
  - 文件夹媒体网格显示一个或多个媒体
- evidence: step 137 (frame 199 → 200)
