# Functional Topology — material_files

- session: `sess_20260912_063938_44f97d`
- generated: 2026-09-12T07:18:26.655026+00:00
- coverage: 55 states · 52 features · 3 data · 11 edges (confirmed ratio 100%, 119 actions)

## Graph
```
material_files
├─ States
│  ├─ [✓] 通知权限请求 (0.99) `state_通知权限请求`
│  ├─ [✓] 内部共享存储根目录 (0.99) `state_内部共享存储根目录`
│  ├─ [✓] 导航抽屉 (0.99) `state_导航抽屉`
│  ├─ [✓] 设置页顶部 (0.99) `state_设置页顶部`
│  ├─ [✓] 搜索结果 (0.99) `state_搜索结果`
│  ├─ [✓] 视图与排序菜单 (0.99) `state_视图与排序菜单`
│  ├─ [✓] 网格文件视图 (0.99) `state_网格文件视图`
│  ├─ [✓] 文件页全局更多菜单 (0.99) `state_文件页全局更多菜单`
│  ├─ [✓] 转到路径对话框 (0.99) `state_转到路径对话框`
│  ├─ [✓] 转到无效路径错误 (0.99) `state_转到无效路径错误`
│  ├─ [✓] 新增操作展开 (0.99) `state_新增操作展开`
│  ├─ [✓] 新建文件夹对话框 (0.99) `state_新建文件夹对话框`
│  ├─ [✓] 创建文件夹后列表 (0.99) `state_创建文件夹后列表`
│  ├─ [✓] 空文件夹 (0.99) `state_空文件夹`
│  ├─ [✓] 新建文件对话框 (0.99) `state_新建文件对话框`
│  ├─ [✓] 创建文件后列表 (0.98) `state_创建文件后列表`
│  ├─ [✓] 文件项目菜单 (0.99) `state_文件项目菜单`
│  ├─ [✓] 文件属性基本信息 (0.99) `state_文件属性基本信息`
│  ├─ [✓] 文件属性权限 (0.99) `state_文件属性权限`
│  ├─ [✓] 文件属性校验和 (0.99) `state_文件属性校验和`
│  ├─ [✓] 重命名对话框 (0.99) `state_重命名对话框`
│  ├─ [✓] 重命名完成 (0.99) `state_重命名完成`
│  ├─ [✓] 创建压缩文件对话框 (0.99) `state_创建压缩文件对话框`
│  ├─ [✓] 压缩完成 (0.99) `state_压缩完成`
│  ├─ [✓] 浏览压缩包内容 (0.99) `state_浏览压缩包内容`
│  ├─ [✓] 压缩包内条目菜单 (0.99) `state_压缩包内条目菜单`
│  ├─ [✓] 提取目标选择模式 (0.99) `state_提取目标选择模式`
│  ├─ [✓] 提取同名冲突对话框 (0.99) `state_提取同名冲突对话框`
│  ├─ [✓] 提取冲突自定义名称 (0.99) `state_提取冲突自定义名称`
│  ├─ [✓] 提取后文件夹 (0.99) `state_提取后文件夹`
│  ├─ [✓] 单项选择模式 (0.99) `state_单项选择模式`
│  ├─ [✓] 多项选择模式 (0.99) `state_多项选择模式`
│  ├─ [✓] 多选更多菜单 (0.99) `state_多选更多菜单`
│  ├─ [✓] 复制目标选择模式 (0.99) `state_复制目标选择模式`
│  ├─ [✓] 批量复制完成 (0.99) `state_批量复制完成`
│  ├─ [✓] 删除确认 (0.99) `state_删除确认`
│  ├─ [✓] 删除完成 (0.99) `state_删除完成`
│  ├─ [✓] 导航抽屉含自定义书签 (0.99) `state_导航抽屉含自定义书签`
│  ├─ [✓] 编辑书签对话框 (0.99) `state_编辑书签对话框`
│  ├─ [✓] 书签重命名完成 (0.99) `state_书签重命名完成`
│  ├─ [✓] 书签移除完成 (0.99) `state_书签移除完成`
│  ├─ [✓] FTP 服务器设置（未启动） (0.99) `state_ftp_服务器设置_未启动`
│  ├─ [✓] FTP 服务器运行中 (0.99) `state_ftp_服务器运行中`
│  ├─ [✓] FTP 账户登录配置 (0.99) `state_ftp_账户登录配置`
│  ├─ [✓] FTP 用户名对话框 (0.99) `state_ftp_用户名对话框`
│  ├─ [✓] FTP 密码对话框 (0.99) `state_ftp_密码对话框`
│  ├─ [✓] FTP 根文件夹选择器 (0.99) `state_ftp_根文件夹选择器`
│  ├─ [✓] 捷克语设置页顶部 (0.50) `state_捷克语设置页顶部`
│  ├─ [✓] 设置页行为选项 (0.50) `state_设置页行为选项`
│  ├─ [✓] 存储空间管理 (0.50) `state_存储空间管理`
│  ├─ [✓] 新增存储类型菜单 (0.50) `state_新增存储类型菜单`
│  ├─ [✓] 新增FTP服务器配置 (0.50) `state_新增ftp服务器配置`
│  ├─ [✓] FTP协议选择 (0.50) `state_ftp协议选择`
│  ├─ [✓] 关于页面 (0.50) `state_关于页面`
│  ├─ [✓] 重启后持久化文件页 (0.50) `state_重启后持久化文件页`
├─ Features
│  ├─ [✓] 允许通知权限 (0.99) `feature_允许通知权限`
│  ├─ [✓] 打开导航抽屉 (0.99) `feature_打开导航抽屉`
│  ├─ [✓] 进入设置 (0.99) `feature_进入设置`
│  ├─ [✓] 搜索当前存储位置 (0.99) `feature_搜索当前存储位置`
│  ├─ [✓] 配置视图与排序 (0.99) `feature_配置视图与排序`
│  ├─ [✓] 切换网格视图 (0.99) `feature_切换网格视图`
│  ├─ [✓] 文件页全局操作 (0.99) `feature_文件页全局操作`
│  ├─ [✓] 按路径转到 (0.99) `feature_按路径转到`
│  ├─ [✓] 转到路径校验 (0.99) `feature_转到路径校验`
│  ├─ [✓] 展开新建操作 (0.99) `feature_展开新建操作`
│  ├─ [✓] 创建文件夹入口 (0.99) `feature_创建文件夹入口`
│  ├─ [✓] 新建文件夹 (0.99) `feature_新建文件夹`
│  │    ─MUTATES→ 文件系统条目
│  ├─ [✓] 进入文件夹 (0.99) `feature_进入文件夹`
│  ├─ [✓] 创建空文件入口 (0.99) `feature_创建空文件入口`
│  ├─ [✓] 新建空文件 (0.99) `feature_新建空文件`
│  │    ─MUTATES→ 文件系统条目
│  ├─ [✓] 文件项目操作菜单 (0.99) `feature_文件项目操作菜单`
│  ├─ [✓] 查看文件属性 (0.99) `feature_查看文件属性`
│  ├─ [✓] 查看权限与校验和 (0.99) `feature_查看权限与校验和`
│  ├─ [✓] 打开重命名 (0.99) `feature_打开重命名`
│  ├─ [✓] 重命名文件 (0.99) `feature_重命名文件`
│  │    ─MUTATES→ 文件系统条目
│  ├─ [✓] 配置压缩文件 (0.99) `feature_配置压缩文件`
│  ├─ [✓] 创建 ZIP 压缩包 (0.99) `feature_创建_zip_压缩包`
│  │    ─MUTATES→ 文件系统条目
│  ├─ [✓] 打开并浏览压缩包 (0.99) `feature_打开并浏览压缩包`
│  ├─ [✓] 压缩包条目操作 (0.99) `feature_压缩包条目操作`
│  ├─ [✓] 开始提取压缩包条目 (0.99) `feature_开始提取压缩包条目`
│  ├─ [✓] 提取时处理同名冲突 (0.99) `feature_提取时处理同名冲突`
│  ├─ [✓] 使用新名称完成提取 (0.99) `feature_使用新名称完成提取`
│  │    ─MUTATES→ 文件系统条目
│  ├─ [✓] 长按选择文件 (0.99) `feature_长按选择文件`
│  ├─ [✓] 多选文件 (0.99) `feature_多选文件`
│  ├─ [✓] 多选批量操作 (0.99) `feature_多选批量操作`
│  ├─ [✓] 开始批量复制 (0.99) `feature_开始批量复制`
│  ├─ [✓] 批量复制到目标目录 (0.99) `feature_批量复制到目标目录`
│  │    ─MUTATES→ 文件系统条目
│  ├─ [✓] 删除前确认 (0.99) `feature_删除前确认`
│  ├─ [✓] 删除文件 (0.99) `feature_删除文件`
│  │    ─MUTATES→ 文件系统条目
│  ├─ [✓] 添加当前文件夹书签 (0.99) `feature_添加当前文件夹书签`
│  │    ─MUTATES→ 书签
│  ├─ [✓] 编辑或移除书签 (0.99) `feature_编辑或移除书签`
│  ├─ [✓] 重命名书签 (0.99) `feature_重命名书签`
│  │    ─MUTATES→ 书签
│  ├─ [✓] 移除书签 (0.99) `feature_移除书签`
│  │    ─MUTATES→ 书签
│  ├─ [✓] 配置 FTP 服务器 (0.99) `feature_配置_ftp_服务器`
│  ├─ [✓] 启动 FTP 服务器 (0.99) `feature_启动_ftp_服务器`
│  ├─ [✓] 停止 FTP 服务器 (0.99) `feature_停止_ftp_服务器`
│  ├─ [✓] 切换 FTP 登录方式 (0.99) `feature_切换_ftp_登录方式`
│  ├─ [✓] 编辑 FTP 用户名 (0.99) `feature_编辑_ftp_用户名`
│  ├─ [✓] 编辑 FTP 密码 (0.99) `feature_编辑_ftp_密码`
│  ├─ [✓] 选择 FTP 根文件夹 (0.99) `feature_选择_ftp_根文件夹`
│  ├─ [✓] 应用内语言切换并持久化 (0.50) `feature_应用内语言切换并持久化`
│  ├─ [✓] 管理存储空间 (0.50) `feature_管理存储空间`
│  ├─ [✓] 新增本地或网络存储 (0.50) `feature_新增本地或网络存储`
│  ├─ [✓] 配置FTP网络存储 (0.50) `feature_配置ftp网络存储`
│  ├─ [✓] 选择FTP安全协议 (0.50) `feature_选择ftp安全协议`
│  ├─ [✓] 查看应用信息 (0.50) `feature_查看应用信息`
│  ├─ [✓] 重启后恢复持久状态 (0.50) `feature_重启后恢复持久状态`
│  │    ─PERSISTS_TO→ 应用偏好设置
├─ Data
│  ├─ [✓] 文件系统条目 (0.99) `data_文件系统条目`
│  ├─ [✓] 书签 (0.99) `data_书签`
│  ├─ [✓] 应用偏好设置 (0.50) `data_应用偏好设置`
```

## Features
### 允许通知权限 `feature_允许通知权限`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 点按系统权限弹窗“允许”后弹窗关闭并显示根目录文件列表。
- evidence: step 2 (frame 3 → 4)

### 打开导航抽屉 `feature_打开导航抽屉`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 点按左上角菜单按钮显示侧边导航抽屉。
- evidence: step 3 (frame 5 → 6)

### 进入设置 `feature_进入设置`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 在导航抽屉点按“设置”进入设置页。
- evidence: step 4 (frame 7 → 8)

### 搜索当前存储位置 `feature_搜索当前存储位置`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 点按搜索按钮打开内嵌搜索框，输入关键词并确认后，仅显示名称匹配的项目。
- evidence: step 8 (frame 11 → 12)

### 配置视图与排序 `feature_配置视图与排序`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 点按排序按钮可选择列表或网格布局及多种排序规则，并设置排序方向、文件夹优先和作用范围。
- evidence: step 15 (frame 24 → 25)

### 切换网格视图 `feature_切换网格视图`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 在视图与排序菜单选择网格后，根目录切换为两列卡片式文件夹网格。
- evidence: step 16 (frame 26 → 27)

### 文件页全局操作 `feature_文件页全局操作`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 点按右上角更多菜单可执行导航、刷新、选择、隐藏文件显示、分享路径、书签和快捷方式等操作。
- evidence: step 17 (frame 28 → 29)

### 按路径转到 `feature_按路径转到`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 全局菜单“转到”打开可输入绝对路径的对话框。
- evidence: step 18 (frame 30 → 31)

### 转到路径校验 `feature_转到路径校验`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 转到对话框会校验路径；提交无效路径时以内联红色错误提示拒绝导航。
- evidence: step 20 (frame 33 → 34)

### 展开新建操作 `feature_展开新建操作`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 点按右下角新增按钮展开创建文件和文件夹的快捷操作。
- evidence: step 22 (frame 37 → 38)

### 创建文件夹入口 `feature_创建文件夹入口`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 从新增操作选择“文件夹”打开命名对话框。
- evidence: step 23 (frame 39 → 40)

### 新建文件夹 `feature_新建文件夹`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 输入有效名称并确认后在当前目录创建文件夹，列表计数与内容立即更新。
- evidence: step 26 (frame 45 → 46)

### 进入文件夹 `feature_进入文件夹`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 点按文件夹卡片进入该目录，顶部以面包屑显示层级并呈现目录内容或空状态。
- evidence: step 27 (frame 47 → 48)

### 创建空文件入口 `feature_创建空文件入口`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 从新增操作选择“文件”打开文件命名对话框。
- evidence: step 29 (frame 51 → 52)

### 新建空文件 `feature_新建空文件`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 输入文件名并确认后在当前目录创建空文件，列表计数与内容立即更新。
- evidence: step 32 (frame 55 → 56)

### 文件项目操作菜单 `feature_文件项目操作菜单`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 点按文件条目的更多按钮显示针对该文件的操作菜单。
- evidence: step 33 (frame 57 → 58)

### 查看文件属性 `feature_查看文件属性`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 从文件菜单选择属性打开多标签属性对话框。
- evidence: step 34 (frame 59 → 60)

### 查看权限与校验和 `feature_查看权限与校验和`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 属性对话框可切换到权限和校验和标签，分别查看安全元数据与多种摘要值。
- evidence: step 35 (frame 61 → 62)

### 打开重命名 `feature_打开重命名`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 文件项目菜单的重命名操作打开预填当前名称的命名对话框。
- evidence: step 39 (frame 67 → 68)

### 重命名文件 `feature_重命名文件`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 在重命名对话框输入新名称并确认后，当前文件条目的名称立即更新。
- evidence: step 42 (frame 71 → 72)

### 配置压缩文件 `feature_配置压缩文件`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 文件菜单的压缩操作提供压缩包名称、三种格式和可选密码。
- evidence: step 44 (frame 74 → 75)

### 创建 ZIP 压缩包 `feature_创建_zip_压缩包`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 确认默认 .zip 压缩设置后，在当前文件夹生成包含所选文件的压缩包。
- evidence: step 47 (frame 78 → 79)

### 打开并浏览压缩包 `feature_打开并浏览压缩包`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 点按 ZIP 压缩包进入其内部内容视图，可查看打包条目并通过面包屑导航。
- evidence: step 48 (frame 80 → 81)

### 压缩包条目操作 `feature_压缩包条目操作`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- ZIP 内部条目可打开、提取、分享、复制路径、创建快捷方式或查看属性。
- evidence: step 49 (frame 82 → 83)

### 开始提取压缩包条目 `feature_开始提取压缩包条目`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 选择压缩包内条目的“提取”进入目标目录选择模式。
- evidence: step 50 (frame 84 → 85)

### 提取时处理同名冲突 `feature_提取时处理同名冲突`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 在提取目标目录确认后，如发现同名文件，则提供跳过、替换、改用新名称及批量应用选项。
- evidence: step 51 (frame 86 → 87)

### 使用新名称完成提取 `feature_使用新名称完成提取`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 在同名冲突中指定新名称并选择替换后，将压缩包条目提取为额外文件且保留原文件。
- evidence: step 56 (frame 94 → 95)

### 长按选择文件 `feature_长按选择文件`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 长按文件卡片进入多选模式并显示批量操作工具栏。
- evidence: step 57 (frame 96 → 97)

### 多选文件 `feature_多选文件`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 选择模式下继续点按条目可切换其选中状态，工具栏计数同步更新。
- evidence: step 58 (frame 98 → 99)

### 多选批量操作 `feature_多选批量操作`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 多选状态可批量剪切、复制、删除，并通过更多菜单批量压缩、分享或全选。
- evidence: step 59 (frame 100 → 101)

### 开始批量复制 `feature_开始批量复制`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 选择两个文件并点按复制后进入目标目录选择模式，同时保留复制数量。
- evidence: step 61 (frame 103 → 104)

### 批量复制到目标目录 `feature_批量复制到目标目录`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 在复制目标选择模式导航到根目录并确认后，两个选中文件被复制到目标位置，源目录内容保留。
- evidence: step 63 (frame 107 → 108)

### 删除前确认 `feature_删除前确认`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 删除选中文件前必须在确认对话框中确认目标。
- evidence: step 66 (frame 113 → 114)

### 删除文件 `feature_删除文件`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 在删除确认对话框点按确定后移除文件并更新列表计数。
- evidence: step 67 (frame 115 → 116)

### 添加当前文件夹书签 `feature_添加当前文件夹书签`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 全局菜单“添加书签”将当前路径加入导航抽屉，自定义书签显示文件夹名称且当前路径高亮。
- evidence: step 69 (frame 118 → 119)

### 编辑或移除书签 `feature_编辑或移除书签`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 长按自定义书签可打开编辑对话框，支持修改名称/路径或移除。
- evidence: step 74 (frame 126 → 127)

### 重命名书签 `feature_重命名书签`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 在书签编辑对话框修改名称并确认后，导航抽屉立即显示新名称。
- evidence: step 77 (frame 130 → 131)

### 移除书签 `feature_移除书签`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 在书签编辑对话框点按移除后，自定义书签立即从导航抽屉删除。
- evidence: step 79 (frame 133 → 134)

### 配置 FTP 服务器 `feature_配置_ftp_服务器`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 导航抽屉的 FTP 服务器页可启动/停止服务，并配置登录方式、凭据、端口、根文件夹和写权限。
- evidence: step 80 (frame 135 → 136)

### 启动 FTP 服务器 `feature_启动_ftp_服务器`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 开启状态开关后 FTP 服务进入正在运行状态，并锁定配置直到停止。
- evidence: step 81 (frame 137 → 138)

### 停止 FTP 服务器 `feature_停止_ftp_服务器`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 关闭运行开关后状态恢复未启动，服务图标消失且配置重新可编辑。
- evidence: step 83 (frame 140 → 141)

### 切换 FTP 登录方式 `feature_切换_ftp_登录方式`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 关闭匿名登录后启用用户名/密码字段，并在显示的 FTP 地址中包含用户名。
- evidence: step 84 (frame 142 → 143)

### 编辑 FTP 用户名 `feature_编辑_ftp_用户名`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 非匿名登录时可点按用户名打开编辑对话框。
- evidence: step 85 (frame 144 → 145)

### 编辑 FTP 密码 `feature_编辑_ftp_密码`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- 非匿名登录时可点按密码打开密码设置对话框。
- evidence: step 87 (frame 147 → 148)

### 选择 FTP 根文件夹 `feature_选择_ftp_根文件夹`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- FTP 根文件夹设置复用文件浏览器的文件夹选择模式，可导航并确认所选位置。
- evidence: step 89 (frame 150 → 151)

### 应用内语言切换并持久化 `feature_应用内语言切换并持久化`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 应用语言可以更改；当前已从中文切换为捷克语，返回文件页并重新进入设置后仍保持 Čeština (Česko)。
- evidence: step 98 (frame 164 → 165)

### 管理存储空间 `feature_管理存储空间`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 从设置进入存储空间页，可查看并排序现有 Root 与内部共享存储位置。
- evidence: step 108 (frame 178 → 179)

### 新增本地或网络存储 `feature_新增本地或网络存储`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 在存储空间管理页点击新增，可选择受限 Android 目录、外部存储或多种网络协议服务器。
- evidence: step 109 (frame 180 → 181)

### 配置FTP网络存储 `feature_配置ftp网络存储`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 选择 FTP 服务器后可填写连接参数，并选择仅添加或连接并添加。
- evidence: step 110 (frame 182 → 183)

### 选择FTP安全协议 `feature_选择ftp安全协议`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- FTP 网络存储配置允许在 FTP、FTPS 与 FTPES 之间选择。
- evidence: step 111 (frame 184 → 185)

### 查看应用信息 `feature_查看应用信息`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 从导航抽屉进入关于页面，可查看版本、许可证、隐私与作者信息。
- evidence: step 117 (frame 193 → 194)

### 重启后恢复持久状态 `feature_重启后恢复持久状态`
- status: ClaimStatus.CONFIRMED · confidence: 0.50
- 重启应用后，创建的文件夹与文件、网格显示方式和所选界面语言仍保持。
- evidence: step 119 (frame 196 → 197)
