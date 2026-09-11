# Functional Topology — snapseed

- session: `sess_20260910_143105_0952c4`
- generated: 2026-09-10T15:50:06.181688+00:00
- coverage: 50 states · 47 features · 4 data · 24 edges (confirmed ratio 100%, 229 actions)

## Graph
```
snapseed
├─ States
│  ├─ [✓] 启动页 (1.00) `state_启动页`
│  │    ─ENABLES→ 打开启动页更多菜单
│  │    ─ENABLES→ 从系统照片选择器打开图片
│  ├─ [✓] 启动页更多菜单 (1.00) `state_启动页更多菜单`
│  │    ─REVEALS→ 设置页
│  ├─ [✓] 设置页 (1.00) `state_设置页`
│  ├─ [✓] 调整图片大小对话框 (1.00) `state_调整图片大小对话框`
│  ├─ [✓] 格式和画质对话框 (1.00) `state_格式和画质对话框`
│  ├─ [✓] 教程加载页 (1.00) `state_教程加载页`
│  ├─ [✓] 系统照片选择器-最近 (1.00) `state_系统照片选择器_最近`
│  ├─ [✓] 系统照片选择器-影集 (1.00) `state_系统照片选择器_影集`
│  ├─ [✓] 图片编辑器-样式 (1.00) `state_图片编辑器_样式`
│  │    ─ENABLES→ 套用预设样式
│  │    ─ENABLES→ 浏览工具网格
│  ├─ [✓] 图片编辑器-工具 (1.00) `state_图片编辑器_工具`
│  ├─ [✓] 调整图片工具 (1.00) `state_调整图片工具`
│  ├─ [✓] 修改内容堆栈 (1.00) `state_修改内容堆栈`
│  ├─ [✓] 编辑步骤蒙版画笔 (1.00) `state_编辑步骤蒙版画笔`
│  ├─ [✓] 操作记录菜单 (1.00) `state_操作记录菜单`
│  ├─ [✓] 图片详细信息 (1.00) `state_图片详细信息`
│  ├─ [✓] 突出细节工具 (1.00) `state_突出细节工具`
│  ├─ [✓] 曲线工具 (1.00) `state_曲线工具`
│  ├─ [✓] 白平衡工具 (1.00) `state_白平衡工具`
│  ├─ [✓] 剪裁工具 (1.00) `state_剪裁工具`
│  ├─ [✓] 旋转工具 (1.00) `state_旋转工具`
│  ├─ [✓] 视角工具 (1.00) `state_视角工具`
│  ├─ [✓] 展开工具 (1.00) `state_展开工具`
│  ├─ [✓] 局部工具 (1.00) `state_局部工具`
│  ├─ [✓] 画笔工具 (1.00) `state_画笔工具`
│  ├─ [✓] 修复工具 (1.00) `state_修复工具`
│  ├─ [✓] HDR景观工具 (1.00) `state_hdr景观工具`
│  ├─ [✓] 魅力光晕工具 (1.00) `state_魅力光晕工具`
│  ├─ [✓] 色调对比度工具 (1.00) `state_色调对比度工具`
│  ├─ [✓] 戏剧效果工具 (1.00) `state_戏剧效果工具`
│  ├─ [✓] 帮助和反馈页 (0.50) `state_帮助和反馈页`
│  ├─ [✓] 复古工具 (0.50) `state_复古工具`
│  ├─ [✓] 粗粒胶片工具 (0.50) `state_粗粒胶片工具`
│  ├─ [✓] 怀旧工具 (0.50) `state_怀旧工具`
│  ├─ [✓] 斑驳工具 (0.50) `state_斑驳工具`
│  ├─ [✓] 黑白工具 (0.50) `state_黑白工具`
│  ├─ [✓] 黑白电影工具 (0.50) `state_黑白电影工具`
│  ├─ [✓] 美颜无面孔提示 (0.50) `state_美颜无面孔提示`
│  ├─ [✓] 头部姿势无面孔提示 (0.50) `state_头部姿势无面孔提示`
│  ├─ [✓] 镜头模糊工具 (0.50) `state_镜头模糊工具`
│  ├─ [✓] 晕影工具 (0.50) `state_晕影工具`
│  ├─ [✓] 双重曝光工具 (0.50) `state_双重曝光工具`
│  ├─ [✓] 文字工具 (0.50) `state_文字工具`
│  ├─ [✓] 相框工具 (0.50) `state_相框工具`
│  ├─ [✓] 导出菜单 (0.50) `state_导出菜单`
│  ├─ [✓] 导出为文件选择器 (0.50) `state_导出为文件选择器`
│  ├─ [✓] 设置页 (0.50) `state_设置页_2`
│  ├─ [✓] QR样式菜单 (0.50) `state_qr样式菜单`
│  ├─ [✓] QR扫描相机权限 (0.50) `state_qr扫描相机权限`
│  │    ←DEPENDS_ON─ 扫描QR样式
│  │    ─TRANSITIONS_TO→ QR样式扫描器
│  ├─ [✓] QR样式扫描器 (0.50) `state_qr样式扫描器`
│  ├─ [✓] 创建QR样式对话框 (0.50) `state_创建qr样式对话框`
├─ Features
│  ├─ [✓] 打开启动页更多菜单 (1.00) `feature_打开启动页更多菜单`
│  │    ─TRANSITIONS_TO→ 启动页更多菜单
│  ├─ [✓] 切换深色主题 (1.00) `feature_切换深色主题`
│  │    ─MUTATES→ 用户设置
│  ├─ [✓] 设置导出最大图片尺寸 (1.00) `feature_设置导出最大图片尺寸`
│  │    ─MUTATES→ 用户设置
│  ├─ [✓] 设置导出格式和画质 (1.00) `feature_设置导出格式和画质`
│  │    ─MUTATES→ 用户设置
│  ├─ [✓] 打开教程 (1.00) `feature_打开教程`
│  ├─ [✓] 从系统照片选择器打开图片 (1.00) `feature_从系统照片选择器打开图片`
│  │    ─TRANSITIONS_TO→ 图片编辑器-样式
│  ├─ [✓] 套用预设样式 (1.00) `feature_套用预设样式`
│  │    ─MUTATES→ 非破坏性编辑堆栈
│  ├─ [✓] 浏览工具网格 (1.00) `feature_浏览工具网格`
│  │    ─TRANSITIONS_TO→ 图片编辑器-工具
│  ├─ [✓] 手势调整图片参数 (1.00) `feature_手势调整图片参数`
│  │    ─MUTATES→ 非破坏性编辑堆栈
│  ├─ [✓] 查看并管理编辑堆栈 (1.00) `feature_查看并管理编辑堆栈`
│  │    ─REVEALS→ 非破坏性编辑堆栈
│  ├─ [✓] 调整步骤蒙版强度并涂抹 (1.00) `feature_调整步骤蒙版强度并涂抹`
│  │    ─MUTATES→ 非破坏性编辑堆栈
│  ├─ [✓] 撤消与重做编辑 (1.00) `feature_撤消与重做编辑`
│  │    ─MUTATES→ 非破坏性编辑堆栈
│  ├─ [✓] 查看图片详细信息 (1.00) `feature_查看图片详细信息`
│  ├─ [✓] 编辑曲线控制点 (1.00) `feature_编辑曲线控制点`
│  ├─ [✓] 白平衡取色 (1.00) `feature_白平衡取色`
│  ├─ [✓] 按比例剪裁图片 (1.00) `feature_按比例剪裁图片`
│  ├─ [✓] 旋转与校直图片 (1.00) `feature_旋转与校直图片`
│  ├─ [✓] 手势变换图片视角 (1.00) `feature_手势变换图片视角`
│  ├─ [✓] 扩展画布并填充边缘 (1.00) `feature_扩展画布并填充边缘`
│  ├─ [✓] 添加并调节局部控制点 (1.00) `feature_添加并调节局部控制点`
│  ├─ [✓] 在图片上绘制局部画笔 (1.00) `feature_在图片上绘制局部画笔`
│  ├─ [✓] 涂抹修复图片区域 (1.00) `feature_涂抹修复图片区域`
│  ├─ [✓] 应用HDR景观预设并调节 (1.00) `feature_应用hdr景观预设并调节`
│  ├─ [✓] 应用魅力光晕 (1.00) `feature_应用魅力光晕`
│  ├─ [✓] 调节色调对比度 (1.00) `feature_调节色调对比度`
│  ├─ [✓] 应用戏剧效果 (1.00) `feature_应用戏剧效果`
│  ├─ [✓] 打开帮助和反馈 (0.50) `feature_打开帮助和反馈`
│  ├─ [✓] 应用复古滤镜 (0.50) `feature_应用复古滤镜`
│  ├─ [✓] 应用粗粒胶片 (0.50) `feature_应用粗粒胶片`
│  ├─ [✓] 应用怀旧滤镜 (0.50) `feature_应用怀旧滤镜`
│  ├─ [✓] 应用并定位斑驳纹理 (0.50) `feature_应用并定位斑驳纹理`
│  ├─ [✓] 黑白转换与色彩滤镜 (0.50) `feature_黑白转换与色彩滤镜`
│  ├─ [✓] 应用黑白电影风格 (0.50) `feature_应用黑白电影风格`
│  ├─ [✓] 美颜面孔检测 (0.50) `feature_美颜面孔检测`
│  ├─ [✓] 头部姿势面孔检测 (0.50) `feature_头部姿势面孔检测`
│  ├─ [✓] 创建可调焦点镜头模糊 (0.50) `feature_创建可调焦点镜头模糊`
│  ├─ [✓] 创建可定位晕影 (0.50) `feature_创建可定位晕影`
│  ├─ [✓] 创建双重曝光 (0.50) `feature_创建双重曝光`
│  ├─ [✓] 添加并样式化文字 (0.50) `feature_添加并样式化文字`
│  ├─ [✓] 添加并调节相框 (0.50) `feature_添加并调节相框`
│  ├─ [✓] 打开导出操作 (0.50) `feature_打开导出操作`
│  ├─ [✓] 按设置导出图片副本 (0.50) `feature_按设置导出图片副本`
│  │    ─MUTATES→ 导出的图片副本
│  ├─ [✓] 导出到指定文件夹 (0.50) `feature_导出到指定文件夹`
│  │    ─MUTATES→ 导出的图片副本
│  ├─ [✓] 持久保存用户设置 (0.50) `feature_持久保存用户设置`
│  ├─ [✓] 通过QR样式交换编辑配方 (0.50) `feature_通过qr样式交换编辑配方`
│  ├─ [✓] 扫描QR样式 (0.50) `feature_扫描qr样式`
│  │    ─DEPENDS_ON→ QR扫描相机权限
│  ├─ [✓] 创建QR编辑样式 (0.50) `feature_创建qr编辑样式`
│  │    ─MUTATES→ QR编辑样式
├─ Data
│  ├─ [✓] 用户设置 (1.00) `data_用户设置`
│  │    ─PERSISTS_TO→ 设置页
│  │    ─ENABLES→ 按设置导出图片副本
│  ├─ [✓] 导出的图片副本 (0.50) `data_导出的图片副本`
│  ├─ [✓] QR编辑样式 (0.50) `data_qr编辑样式`
│  ├─ [✓] 非破坏性编辑堆栈 (0.50) `data_非破坏性编辑堆栈`
│  │    ─ENABLES→ 创建QR编辑样式
```

## Features
### 打开启动页更多菜单 `feature_打开启动页更多菜单`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 点击启动页右上角三点显示设置、教程、帮助和反馈。
- evidence: step 1 (frame 1 → 2)

### 切换深色主题 `feature_切换深色主题`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 点击开关后整个设置页立即切换到深色背景，开关变为蓝色开启态。
- evidence: step 3 (frame 5 → 6)

### 设置导出最大图片尺寸 `feature_设置导出最大图片尺寸`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 调整图片大小对话框提供不调整及五档像素长边；选择后对话框关闭并在设置页显示当前值。
- evidence: step 5 (frame 9 → 10)

### 设置导出格式和画质 `feature_设置导出格式和画质`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 对话框提供JPG三档质量和PNG；选择后对话框关闭并显示当前值。
- evidence: step 7 (frame 13 → 14)

### 打开教程 `feature_打开教程`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 教程以底部上浮面板打开；离线环境下持续显示加载指示，等待后无内容。
- evidence: step 11 (frame 20 → 21)

### 从系统照片选择器打开图片 `feature_从系统照片选择器打开图片`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 启动页点击中央区域打开系统选择器，选择相册中的图片后进入编辑器。
- evidence: step 17 (frame 30 → 31)

### 套用预设样式 `feature_套用预设样式`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 样式横条可横向滚动；点选样式后即时预览并显示取消和确认，确认后返回主编辑器并保留效果。可见样式包括Portrait、Smooth、Pop、Accentuate、Faded Glow、Morning、Bright、Fine Art、Push、Structure、Silhouette。
- evidence: step 22 (frame 39 → 40)

### 浏览工具网格 `feature_浏览工具网格`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 点击工具底部导航打开包含28项编辑工具的网格。
- evidence: step 27 (frame 48 → 49)

### 手势调整图片参数 `feature_手势调整图片参数`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 在参数菜单选择一项后，左右滑动改变数值；实测对比度从0调整到+62并即时更新预览。
- evidence: step 30 (frame 54 → 55)

### 查看并管理编辑堆栈 `feature_查看并管理编辑堆栈`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 操作记录菜单可进入修改内容，查看样式展开为多个编辑步骤及后续调整；每一步可删除、局部蒙版或重新调节。
- evidence: step 34 (frame 61 → 62)

### 调整步骤蒙版强度并涂抹 `feature_调整步骤蒙版强度并涂抹`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 蒙版画笔中上下箭头改变强度（实测100降至50），可在图片上滑动涂抹并切换蒙版显示。
- evidence: step 37 (frame 67 → 68)

### 撤消与重做编辑 `feature_撤消与重做编辑`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 操作记录菜单可撤消最近一次已确认调整；撤消后重做变为可用，可恢复该调整。
- evidence: step 44 (frame 76 → 77)

### 查看图片详细信息 `feature_查看图片详细信息`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 载入图片后点击信息图标可查看日期时间、文件名、分辨率和文件大小。
- evidence: step 47 (frame 81 → 82)

### 编辑曲线控制点 `feature_编辑曲线控制点`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 点击曲线可新增控制点，拖动控制点改变曲线形状并即时更新图片。
- evidence: step 56 (frame 95 → 96)

### 白平衡取色 `feature_白平衡取色`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 点击取色器显示带红色十字的放大镜，可拖到图片区域的目标颜色位置取样。
- evidence: step 61 (frame 102 → 103)

### 按比例剪裁图片 `feature_按比例剪裁图片`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 可从横向比例列表选择预设比例，实测16:9后裁剪框即时变化；在画布拖动可进一步改变裁剪区域。
- evidence: step 67 (frame 112 → 113)

### 旋转与校直图片 `feature_旋转与校直图片`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 点击旋转按钮可作90度旋转；在图片上水平滑动可连续改变校直角度，实测显示-25.26度并实时旋转预览。
- evidence: step 72 (frame 120 → 121)

### 手势变换图片视角 `feature_手势变换图片视角`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 选择倾斜模式后按横向或纵向手势改变透视，网格与图片边界实时变形。
- evidence: step 77 (frame 127 → 128)

### 扩展画布并填充边缘 `feature_扩展画布并填充边缘`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 拖动图片边缘手柄扩大画布；实测智能填色会在新增区域延展原图纹理。
- evidence: step 81 (frame 133 → 134)

### 添加并调节局部控制点 `feature_添加并调节局部控制点`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 点击图片新增局部控制点；拖动点可重新定位。选中点时水平滑动调值，实测亮度到+88；垂直滑动切换参数，实测从亮度切到结构。
- evidence: step 85 (frame 139 → 140)

### 在图片上绘制局部画笔 `feature_在图片上绘制局部画笔`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 选择画笔类型后可在图片上拖动绘制；实测曝光0.7画笔绘制横向区域，开启蒙版显示后涂抹区域以红色覆盖呈现。
- evidence: step 92 (frame 151 → 152)

### 涂抹修复图片区域 `feature_涂抹修复图片区域`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 在图片上拖动涂抹后，应用自动从邻近区域取样修复，画面立即更新；可点击撤消恢复。
- evidence: step 98 (frame 160 → 161)

### 应用HDR景观预设并调节 `feature_应用hdr景观预设并调节`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 可选择自然、人物、精细或强HDR预设，并通过参数菜单和水平手势调整滤镜强度、亮度、饱和度。
- evidence: step 103 (frame 167 → 168)

### 应用魅力光晕 `feature_应用魅力光晕`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 可在5个预设间切换，并通过参数菜单和水平滑动调整光晕、饱和度、暖色调。
- evidence: step 107 (frame 173 → 174)

### 调节色调对比度 `feature_调节色调对比度`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 通过参数菜单选择高、中、低色调或阴影/高光保护，再水平滑动改变数值和预览。
- evidence: step 111 (frame 179 → 180)

### 应用戏剧效果 `feature_应用戏剧效果`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 可在戏剧、明亮、昏暗系列预设间切换，并调整滤镜强度和饱和度。
- evidence: step 115 (frame 185 → 186)

### 打开帮助和反馈 `feature_打开帮助和反馈`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 从启动页更多菜单打开帮助和反馈列表；反馈入口会尝试交给外部反馈处理流程。
- evidence: step 15 (frame 26 → 27)

### 应用复古滤镜 `feature_应用复古滤镜`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 选择编号复古样式，并可调亮度、饱和度、样式强度和晕影强度。
- evidence: step 118 (frame 189 → 190)

### 应用粗粒胶片 `feature_应用粗粒胶片`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 选择胶片编号样式并调节粒度与样式强度。
- evidence: step 122 (frame 195 → 196)

### 应用怀旧滤镜 `feature_应用怀旧滤镜`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 选择怀旧样式，可随机化纹理并调节色调、刮痕与漏光。
- evidence: step 126 (frame 201 → 202)

### 应用并定位斑驳纹理 `feature_应用并定位斑驳纹理`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 随机或手动选择斑驳纹理，移动纹理中心并调节样式与色调参数。
- evidence: step 130 (frame 207 → 208)

### 黑白转换与色彩滤镜 `feature_黑白转换与色彩滤镜`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 从多种黑白预设中选择，叠加彩色滤镜并调节亮度、对比度、粒度。
- evidence: step 134 (frame 213 → 214)

### 应用黑白电影风格 `feature_应用黑白电影风格`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 选择编号电影黑白风格，并调节亮度、柔化、粒度和滤镜强度。
- evidence: step 139 (frame 221 → 222)

### 美颜面孔检测 `feature_美颜面孔检测`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 进入美颜时先检测照片中的面孔；无面孔时明确提示并允许取消或重试。
- evidence: step 143 (frame 227 → 228)

### 头部姿势面孔检测 `feature_头部姿势面孔检测`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 头部姿势依赖明显面孔检测；无可用面孔时提示取消或重试。
- evidence: step 146 (frame 231 → 232)

### 创建可调焦点镜头模糊 `feature_创建可调焦点镜头模糊`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 切换圆形/线性焦点区域并通过手势调节模糊强度，另可调过渡与晕影。
- evidence: step 149 (frame 235 → 236)

### 创建可定位晕影 `feature_创建可定位晕影`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 定位晕影中心，并通过手势与参数菜单独立调整外部和内部亮度。
- evidence: step 155 (frame 244 → 245)

### 创建双重曝光 `feature_创建双重曝光`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 选择第二张图片，切换混合模式并调节叠加层不透明度。
- evidence: step 161 (frame 253 → 254)

### 添加并样式化文字 `feature_添加并样式化文字`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 为图片添加文本，选择排版样式和颜色，调节不透明度，并可倒置文字外观；不兼容字符会禁用部分样式。
- evidence: step 172 (frame 270 → 271)

### 添加并调节相框 `feature_添加并调节相框`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 从编号相框中选择样式，并通过横向手势调节相框宽度。
- evidence: step 184 (frame 289 → 290)

### 打开导出操作 `feature_打开导出操作`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 从编辑器打开分享、保存、按设置导出与指定文件夹导出四种操作。
- evidence: step 188 (frame 295 → 296)

### 按设置导出图片副本 `feature_按设置导出图片副本`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 执行“导出”后生成图片副本，并在底部提示“照片已保存”，提供“查看”操作。
- evidence: step 189 (frame 297 → 298)

### 导出到指定文件夹 `feature_导出到指定文件夹`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 打开系统文件选择器，进入目标文件夹，确认文件名后保存导出的图片。
- evidence: step 193 (frame 303 → 304)

### 持久保存用户设置 `feature_持久保存用户设置`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 应用重启后仍保留主题、最大导出尺寸与格式/画质选择。
- evidence: step 206 (frame 321 → 322)

### 通过QR样式交换编辑配方 `feature_通过qr样式交换编辑配方`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 操作记录菜单可打开 QR 样式面板；有编辑步骤时可创建配方二维码，也可扫描二维码应用配方。
- evidence: step 216 (frame 336 → 337)

### 扫描QR样式 `feature_扫描qr样式`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 请求相机权限后显示 QR 样式扫描框，可扫描并应用他人分享的编辑配方。
- evidence: step 218 (frame 340 → 341)

### 创建QR编辑样式 `feature_创建qr编辑样式`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 当编辑堆栈非空时，把当前编辑配方生成二维码，并可通过系统分享给他人。
- evidence: step 228 (frame 353 → 354)
