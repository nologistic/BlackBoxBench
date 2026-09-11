# Functional Topology — vlc

- session: `sess_20260910_173136_97aa3f`
- generated: 2026-09-10T18:20:58.617652+00:00
- coverage: 51 states · 23 features · 9 data · 1 edges (confirmed ratio 100%, 151 actions)

## Graph
```
vlc
├─ States
│  ├─ [✓] 视频主页（未授权）与通知权限说明 (1.00) `state_视频主页_未授权_与通知权限说明`
│  ├─ [✓] 视频主页（未授予媒体权限） (1.00) `state_视频主页_未授予媒体权限`
│  ├─ [✓] 权限选择面板 (1.00) `state_权限选择面板`
│  ├─ [✓] VLC 3.7 新功能介绍 (1.00) `state_vlc_3_7_新功能介绍`
│  ├─ [✓] 均衡器预设列表 (1.00) `state_均衡器预设列表`
│  ├─ [✓] 均衡器编辑器 (1.00) `state_均衡器编辑器`
│  ├─ [✓] 均衡器编辑器 (1.00) `state_均衡器编辑器_2`
│  ├─ [✓] 均衡器批量操作菜单 (1.00) `state_均衡器批量操作菜单`
│  ├─ [✓] 视频空状态（已授予常规权限） (1.00) `state_视频空状态_已授予常规权限`
│  ├─ [✓] 音频库空状态 (1.00) `state_音频库空状态`
│  ├─ [✓] 音频艺术家显示与排序设置 (1.00) `state_音频艺术家显示与排序设置`
│  ├─ [✓] 浏览主页 (1.00) `state_浏览主页`
│  ├─ [✓] 内部存储文件浏览器 (1.00) `state_内部存储文件浏览器`
│  ├─ [✓] BlackBoxBench 样例媒体目录 (1.00) `state_blackboxbench_样例媒体目录`
│  ├─ [✓] 视频播放器首次教程（全屏提示） (1.00) `state_视频播放器首次教程_全屏提示`
│  ├─ [✓] 视频文件操作菜单 (1.00) `state_视频文件操作菜单`
│  ├─ [✓] 视频文件信息页 (1.00) `state_视频文件信息页`
│  ├─ [✓] 视频库含媒体 (1.00) `state_视频库含媒体`
│  ├─ [✓] 音频库艺术家视图 (1.00) `state_音频库艺术家视图`
│  ├─ [✓] 音频轨道列表 (1.00) `state_音频轨道列表`
│  ├─ [✓] 音频播放器首次教程 (1.00) `state_音频播放器首次教程`
│  ├─ [✓] 音频轨道操作菜单 (1.00) `state_音频轨道操作菜单`
│  ├─ [✓] 添加媒体到播放列表面板 (1.00) `state_添加媒体到播放列表面板`
│  ├─ [✓] 播放列表库 (1.00) `state_播放列表库`
│  ├─ [✓] 播放列表详情 (1.00) `state_播放列表详情`
│  ├─ [✓] 播放列表操作菜单 (1.00) `state_播放列表操作菜单`
│  ├─ [✓] 更多主页 (1.00) `state_更多主页`
│  ├─ [✓] 打开网络串流页 (1.00) `state_打开网络串流页`
│  ├─ [✓] 串流更多菜单 (1.00) `state_串流更多菜单`
│  ├─ [✓] 历史记录列表 (0.50) `state_历史记录列表`
│  ├─ [✓] 关于_VLC (0.50) `state_关于_vlc`
│  ├─ [✓] 设置主页_媒体库与视频 (0.50) `state_设置主页_媒体库与视频`
│  ├─ [✓] 设置主页_历史与附加设置 (0.50) `state_设置主页_历史与附加设置`
│  ├─ [✓] 设置主页_全部分类 (0.50) `state_设置主页_全部分类`
│  ├─ [✓] 界面设置_常规与无痕 (0.50) `state_界面设置_常规与无痕`
│  ├─ [✓] 界面设置_媒体呈现 (0.50) `state_界面设置_媒体呈现`
│  ├─ [✓] 视频设置_播放与显示 (0.50) `state_视频设置_播放与显示`
│  ├─ [✓] 字幕设置_样式 (0.50) `state_字幕设置_样式`
│  ├─ [✓] 字幕设置_阴影与轮廓 (0.50) `state_字幕设置_阴影与轮廓`
│  ├─ [✓] 音频设置_播放与耳机 (0.50) `state_音频设置_播放与耳机`
│  ├─ [✓] 音频设置_回放增益与_MIDI (0.50) `state_音频设置_回放增益与_midi`
│  ├─ [✓] 投屏设置 (0.50) `state_投屏设置`
│  ├─ [✓] 家长控制密码设置 (0.50) `state_家长控制密码设置`
│  ├─ [✓] 远程访问引导_欢迎 (0.50) `state_远程访问引导_欢迎`
│  ├─ [✓] 远程访问引导_权限范围 (0.50) `state_远程访问引导_权限范围`
│  ├─ [✓] 远程访问设置 (0.50) `state_远程访问设置`
│  ├─ [✓] 远程访问服务器状态 (0.50) `state_远程访问服务器状态`
│  ├─ [✓] Android_Auto_设置 (0.50) `state_android_auto_设置`
│  ├─ [✓] 高级设置_网络与数据维护 (0.50) `state_高级设置_网络与数据维护`
│  ├─ [✓] 高级设置_性能 (0.50) `state_高级设置_性能`
│  ├─ [✓] 高级设置_开发人员 (0.50) `state_高级设置_开发人员`
├─ Features
│  ├─ [✓] 跳过引导进入受限主页 (1.00) `feature_跳过引导进入受限主页`
│  ├─ [✓] 通知权限请求 (1.00) `feature_通知权限请求`
│  ├─ [✓] 媒体文件权限分级 (1.00) `feature_媒体文件权限分级`
│  ├─ [✓] 从新功能介绍直达设置 (1.00) `feature_从新功能介绍直达设置`
│  ├─ [✓] 选择均衡器预设 (1.00) `feature_选择均衡器预设`
│  ├─ [✓] 均衡器十段编辑 (1.00) `feature_均衡器十段编辑`
│  ├─ [✓] 均衡器十段编辑 (1.00) `feature_均衡器十段编辑_2`
│  ├─ [✓] 均衡器总开关 (1.00) `feature_均衡器总开关`
│  ├─ [✓] 授予常规媒体权限 (1.00) `feature_授予常规媒体权限`
│  ├─ [✓] 媒体权限级别互斥校验 (1.00) `feature_媒体权限级别互斥校验`
│  ├─ [✓] 配置音频条目播放动作 (1.00) `feature_配置音频条目播放动作`
│  ├─ [✓] 更改音频条目默认动作 (1.00) `feature_更改音频条目默认动作`
│  ├─ [✓] 将目录加入媒体扫描 (1.00) `feature_将目录加入媒体扫描`
│  │    ─TRANSITIONS_TO→ 视频库含媒体
│  ├─ [✓] 收藏音频轨道 (1.00) `feature_收藏音频轨道`
│  ├─ [✓] 新建播放列表并添加轨道 (0.98) `feature_新建播放列表并添加轨道`
│  ├─ [✓] 串流无痕模式 (0.50) `feature_串流无痕模式`
│  ├─ [✓] 筛选当前媒体列表 (0.50) `feature_筛选当前媒体列表`
│  ├─ [✓] 完成远程访问引导 (0.50) `feature_完成远程访问引导`
│  ├─ [✓] 启用远程访问服务器 (0.50) `feature_启用远程访问服务器`
│  ├─ [✓] 应用重启后保留媒体状态 (0.50) `feature_应用重启后保留媒体状态`
│  ├─ [✓] 播放列表与收藏跨重启保留 (0.50) `feature_播放列表与收藏跨重启保留`
│  ├─ [✓] 无痕模式跨重启保留 (0.50) `feature_无痕模式跨重启保留`
│  ├─ [✓] 均衡器设置跨重启保留 (0.50) `feature_均衡器设置跨重启保留`
├─ Data
│  ├─ [✓] 均衡器预设选择 (0.95) `data_均衡器预设选择`
│  ├─ [✓] 媒体访问级别 (1.00) `data_媒体访问级别`
│  ├─ [✓] 音频库显示偏好 (1.00) `data_音频库显示偏好`
│  ├─ [✓] 媒体库扫描目录 (1.00) `data_媒体库扫描目录`
│  ├─ [✓] 收藏媒体 (1.00) `data_收藏媒体`
│  ├─ [✓] 播放列表 (0.98) `data_播放列表`
│  ├─ [✓] 无痕播放偏好 (0.50) `data_无痕播放偏好`
│  ├─ [✓] 播放历史 (0.50) `data_播放历史`
│  ├─ [✓] 远程访问服务配置 (0.50) `data_远程访问服务配置`
```

## Features
### 跳过引导进入受限主页 `feature_跳过引导进入受限主页`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 欢迎页点击“跳过”直接进入主页；媒体权限未授予时主页明确显示只能播放串流或网络媒体。
- evidence: step 1 (frame 1 → 2)

### 通知权限请求 `feature_通知权限请求`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 首次进入主页时先显示应用内用途说明；点击确定后进入系统通知许可弹窗，可允许或不允许；拒绝后仍可正常停留主页。
- evidence: step 2 (frame 3 → 4)

### 媒体文件权限分级 `feature_媒体文件权限分级`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 应用内提供无文件权限、常规媒体权限、完整访问权限三档选择；选项会影响自动扫描和字幕访问能力。
- evidence: step 4 (frame 7 → 8)

### 从新功能介绍直达设置 `feature_从新功能介绍直达设置`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- “在设置中显示”可直接打开对应功能页；均衡器链接进入预设管理界面。
- evidence: step 8 (frame 11 → 12)

### 选择均衡器预设 `feature_选择均衡器预设`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 点击预设行会立即切换选中高亮与勾选状态；Flat 可切换到 Classical。
- evidence: step 9 (frame 13 → 14)

### 均衡器十段编辑 `feature_均衡器十段编辑`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 均衡器调节入口打开可编辑面板，支持总开关、前置放大、十段频率增益、联动相邻频段以及新增/重命名/删除预设。
- evidence: step 10 (frame 15 → 16)

### 均衡器十段编辑 `feature_均衡器十段编辑_2`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 均衡器调节入口打开可编辑面板，支持总开关、前置放大、十段频率增益、联动相邻频段以及新增/重命名/删除预设。
- evidence: step 10 (frame 15 → 16)

### 均衡器总开关 `feature_均衡器总开关`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 均衡器编辑器的启用开关可由关闭灰色切换为启用橙色。
- evidence: step 11 (frame 17 → 18)

### 授予常规媒体权限 `feature_授予常规媒体权限`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 选择“只有常规媒体权限”后依次请求音乐/音频与照片/视频系统许可；允许后返回主页并自动扫描标准媒体。
- evidence: step 18 (frame 31 → 32)

### 媒体权限级别互斥校验 `feature_媒体权限级别互斥校验`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 尝试从“只有常规媒体权限”直接切换到“完整访问权限”时被阻止，界面将当前常规权限行和错误提示标红，并要求先禁用常规媒体文件权限。
- evidence: step 25 (frame 43 → 44)

### 配置音频条目播放动作 `feature_配置音频条目播放动作`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 音频显示设置中的“播放”动作可选播放、添加到播放队列、插入为下一项。
- evidence: step 30 (frame 52 → 53)

### 更改音频条目默认动作 `feature_更改音频条目默认动作`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 选择“添加到播放队列”后，显示设置中的当前动作文字立即更新。
- evidence: step 31 (frame 54 → 55)

### 将目录加入媒体扫描 `feature_将目录加入媒体扫描`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 文件信息页显示“此目录尚未扫描”时可点击“添加”；点击后该提示与按钮消失，表示目录已加入媒体库扫描范围。
- evidence: step 49 (frame 88 → 89)

### 收藏音频轨道 `feature_收藏音频轨道`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 在轨道菜单点击“添加到收藏夹”后，sample_audio.wav 行出现实心心形图标。
- evidence: step 62 (frame 112 → 113)

### 新建播放列表并添加轨道 `feature_新建播放列表并添加轨道`
- status: ClaimStatus.CONFIRMED · confidence: 0.98
- 从轨道的“添加到播放列表”面板输入 Bench，点击新建后生成播放列表并默认勾选；点击保存完成将 1 个媒体加入。
- evidence: step 65 (frame 117 → 118)

### 串流无痕模式 `feature_串流无痕模式`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 打开串流页更多菜单可切换无痕模式；勾选后再次打开菜单仍显示选中，说明设置已生效。
- evidence: step 84 (frame 149 → 150)

### 筛选当前媒体列表 `feature_筛选当前媒体列表`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 历史页搜索输入 audio 并确认后，只保留匹配的 sample_audio.wav，顶部仍可一键切换为全媒体库搜索。
- evidence: step 91 (frame 162 → 163)

### 完成远程访问引导 `feature_完成远程访问引导`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 首次进入远程访问时依次介绍跨设备控制、加密连接、一次性密码认证和内容权限控制，完成后进入远程访问设置。
- evidence: step 124 (frame 218 → 219)

### 启用远程访问服务器 `feature_启用远程访问服务器`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 勾选“启用远程访问”后服务器启动，状态页提供局域网访问链接及二维码/分享/复制操作。
- evidence: step 129 (frame 228 → 229)

### 应用重启后保留媒体状态 `feature_应用重启后保留媒体状态`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 从高级设置重启应用后直接返回主界面，不再出现引导；扫描出的音频/视频与历史记录仍可见。
- evidence: step 141 (frame 249 → 250)

### 播放列表与收藏跨重启保留 `feature_播放列表与收藏跨重启保留`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 重启后播放列表 Bench 仍含 1 条轨道，音频 sample_audio.wav 仍显示收藏心形。
- evidence: step 142 (frame 251 → 252)

### 无痕模式跨重启保留 `feature_无痕模式跨重启保留`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 应用重启后主菜单中的无痕模式仍显示勾选，与“无痕模式持续开启”设置一致。
- evidence: step 145 (frame 256 → 257)

### 均衡器设置跨重启保留 `feature_均衡器设置跨重启保留`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 重启后均衡器仍选中 Classical，编辑器总开关保持开启并保留各频段值（高频约 -7/-9 dB）。
- evidence: step 149 (frame 262 → 263)
