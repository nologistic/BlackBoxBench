# BlackBoxBench 依赖需求表

本页列出复现网页与 Android 两条实验链路所需的外部依赖。大文件、登录资料、运行
截图和生成结果都不进入 Git；Git 只保存锁文件、构建脚本和验证方法。

## 最小宿主环境

| 依赖 | 锁定/最低要求 | 典型磁盘占用 | 本项目位置或来源 | 用途 | 验证方式 |
|---|---:|---:|---|---|---|
| Windows | Windows 11，x64，Hyper-V/WHPX 可用 | — | 操作系统 | Android Emulator 硬件加速 | `scripts/android_runtime_smoke.py` |
| Python | 3.12；包版本见 `requirements/runtime.lock.txt` | 当前便携目录约 297 MB | `vendor/python/`，也可使用独立 venv | Controller、MCP、测试、构建脚本 | `scripts/verify_dependencies.py` |
| JDK | 17；当前验证 Temurin 17.0.20.1+1 | 当前约 318 MB | `vendor/jdk17/` 或 `BBB_JAVA_HOME` | Android SDK 工具与宿主脚本 | `scripts/setup_android.py --preflight` |
| Docker Desktop | 可工作的 Linux Engine；当前验证 29.7.2 | 程序本体另计 | Docker Desktop 官方安装 | 网页/Android 生成沙箱和严格 Agent 容器 | `docker version` |
| Git | 2.x | 很小 | Git for Windows | 获取源码和版本记录 | `git status` |

## Android 大文件

精确版本记录在 `requirements/android-toolchain.lock.json`，安装脚本会在下载后再次
校验实际 revision，版本不符会拒绝把环境报告为 ready。

| 组件 | 锁定版本 | 当前总占用/估算 | 默认缓存位置 | 获取与重建 |
|---|---:|---:|---|---|
| Android command-line tools | 22.0，下载 ZIP 有 SHA-256 | 包含在 SDK 总量中 | `vendor/android/sdk/cmdline-tools/` | `scripts/setup_android.py --accept-licenses` |
| Platform Tools | 37.0.1 | 包含在 SDK 总量中 | `vendor/android/sdk/platform-tools/` | 同上 |
| Emulator | 37.1.11 | 包含在 SDK 总量中 | `vendor/android/sdk/emulator/` | 同上 |
| Android 35 platform | revision 2 | 包含在 SDK 总量中 | `vendor/android/sdk/platforms/android-35/` | 同上 |
| Build Tools | 35.0.0 | 包含在 SDK 总量中 | `vendor/android/sdk/build-tools/35.0.0/` | 同上 |
| Google APIs x86_64 system image | Android 35 revision 9 | SDK + AVD 当前合计约 5.28 GB | `vendor/android/sdk/system-images/` | 同上 |
| BBB_Base AVD | Pixel 6 / x86_64 / google_apis | 首次运行后会增长 | `vendor/android/avd/` | 安装脚本确定性创建 |
| Android Commerce Demo APK | 由源码构建，约 10 MB | 构建目录可删除 | `sample_apps/android_commerce_demo/**/build/` | `scripts/build_android_sample.py` |

只需网页实验时可以不安装 Android SDK、JDK 与 Android 构建镜像。Android 探索需要
宿主 SDK/AVD；Android APK 生成还需要下一节的 Gradle 构建镜像。

## Docker 镜像

外部基础镜像的不可变 digest 见 `requirements/container-images.lock.json`，所有正式
Dockerfile 的 `FROM` 也使用相同 digest。下表的项目镜像是本地可重建产物，不提交镜像
tar；体积会随 Docker 存储驱动略有变化。

| 镜像 | 当前约占用 | 必需场景 | 构建命令 |
|---|---:|---|---|
| `blackboxbench/android-reproduction-workbench:1` | 5.23 GB | Android APK 生成 | `scripts/build_android_reproduction_image.py --accept-licenses` |
| `blackboxbench/reproduction-workbench:latest` | 309 MB | 网页生成 | 首次复现时由平台构建，或按 `reproduction/agent_image/Dockerfile` 构建 |
| `blackboxbench/reference-raw:latest` | 1.56 GB | 已归档的 self-built 实验/操作员登录 | `docker compose -f docker-compose.self-explorer.url.yml build` |
| `blackboxbench/self-tool-builder:latest` | 516 MB | 已归档的 self-built 实验 | 同上 |
| `gradle:8.10.2-jdk17` 基础镜像 | 1.15 GB | Android 构建镜像的基础层 | 构建脚本按 digest 自动拉取 |
| 严格 Agent Node 镜像 | 约 329 MB 起 | 客户端容器化实验 | 各条件 `agent_runtime_assets/Dockerfile.example` |

示例 Agent 镜像默认锁定 CodeBuddy CLI 2.138.0；切换 Codex、Claude Code 或其他
客户端时必须在 Dockerfile 中写入明确版本，并把版本记入实验元数据，不能使用隐含的
`latest`。

Docker 镜像层会共享，因此表中大小不能直接相加。若需要离线搬迁，在已验证机器上用
`docker save` 导出所需镜像，并同时保存本仓库与两个 JSON 锁文件；导入后再次运行依赖
验证脚本。镜像 tar 往往数 GB，必须作为外部制品管理，不能提交到 Git。

## 可重建素材与不可迁移数据

| 内容 | 当前约占用 | Git 策略 | 说明 |
|---|---:|---|---|
| 公共网页素材 | 小于 1 MB | 生成物忽略，生成器提交 | `python -m reproduction.materials.build` 可离线重建 |
| 公共移动素材 | 约 1.4 MB | 生成物忽略，生成器提交 | 由 `app_reproduction/materials/build.py` 重建 |
| `runs/live_targets/` | 当前约 1.40 GB | 永不提交、不得复制给 Agent | 含真实登录 Profile，只能由操作员在目标机器维护 |
| `runs/android_targets/` | 随 APK/Profile 增长 | 永不提交 | 用户 APK、golden snapshot 与网络守卫租约 |
| `website_output/`、`app_output/` | 随实验增长 | 只保留 `.gitkeep` | 每次 Agent 的独立生成结果 |

登录 Profile 不是普通依赖，也不应为了“复现环境”传给其他人。其他机器需要由各自的
操作员重新登录并建立 golden profile。

## 推荐复现顺序

1. 克隆仓库，建立 Python 3.12 环境并安装 `requirements/runtime.lock.txt`。
2. 启动 Docker Desktop，确认 `docker version` 同时显示 Client 与 Server。
3. 只做网页实验时，构建所需网页镜像并执行完整测试。
4. 做 Android 实验时，准备 JDK 17，运行 `scripts/setup_android.py --accept-licenses`。
5. 运行 `scripts/build_android_reproduction_image.py --accept-licenses` 和
   `scripts/build_android_sample.py`。
6. 运行 `scripts/verify_dependencies.py --full`、`pytest tests/ -q`，再安装所需的单一
   实验条件 Skill/MCP。

`scripts/verify_dependencies.py` 是只读检查，不下载、不构建也不修改运行数据。
