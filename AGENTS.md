# AGENTS.md — BlackBoxBench

## 项目定位

这是黑盒 App 探索内核和下游网站复现素材层。探索有"托管探索工具"和"Agent 自建
探索工具"两个并列实验条件；二者都只能通过像素和坐标级类人输入探索目标。托管模式
产出 finalized Functional Topology；自建模式不规定拓扑、报告、证据格式、目录结构、
覆盖指标或探索工作流，用来观察 Agent 的原初表现。复现 Agent 读取各条件允许交接的
输入与公共虚构素材，只在固定输出目录实现网页。

## 当前实验状态（2026-09）

- 自建模式已完成实验周期并**从所有 Agent（Codex/CodeBuddy/Kimi）注销**；
  代码归档于 `self_explorer/`，历史产物在 `runs/self_built/`。本文件中与自建
  模式相关的约束继续适用于归档代码与其复现实验。
- 当前活跃的**探索条件为四个**：网页 **baseline**（`agents/cli_explorer`，
  冻结作对照）、网页 **our-method**（`agents/our_method`，基线的独立演进副本；
  改进含只读 `input_read`/`input_list` 素材通道、交接证据帧索引、写前预检、
  完成硬闸与写路径证据验收，详见 `agents/our_method/README.md`）、Android
  **baseline**（`agents/android_baseline`）与 Android **our-method**
  （`agents/android_our_method`）。四者共享 `benchmark/*` 平台底座（Android 两
  条件另共享 `benchmark/android`、Recorder、Topology 与 `app_reproduction`），
  但不得互相 import 方法层实现；任一 our-method 的改进不得反向修改对应 baseline。
- 另有**两个评测条件**：`web-review`（`agents/web_review/` + `web_evaluation/`，
  已收编；像素+类人输入+交接源码只读通道 `read_source`，与 app-review 护栏
  完全对齐）与 `app-review`（`agents/app_review/` + `app_evaluation/`）。
  语义判定由 LLM judge 在 Skill 内完成，代码只做薄护栏（证据引用真实截图、四档
  封闭、不漏项、报告 schema 固定），契约见 `docs/evaluation_contract.md`。
- 正式对照实验中每个 Agent 任务只应暴露一个条件的 MCP（our-method 安装器
  提供 `--exclusive` 互斥注册）；评测任务只启用 `app-review`。
- 正式数据集与自写样例必须区分：**web 28 目标数据集**（语雀、YouTube、淘宝、知乎、
  小红书、微博、豆瓣、大众点评、携程、Reddit、Quora、Notion、Trello、Todoist、
  Airtable、Google Calendar、Dropbox、Google Forms、Excalidraw、diagrams.net、
  Spotify、Desmos×3、Google Maps、Kleki、JS Paint、Squoosh，注册于
  `benchmark/orchestrator/apps.py`，共 848 条清单要求）与 **Android 26 目标数据集**
  （`google_clock` + 25 个应用，注册于 `runs/android_targets/targets.json`）是
  数据集；`ecommerce_demo` 与 `android_commerce_demo` 只用于冒烟测试。
- Android 数据集三件套已对齐 26 目标：目标注册（包名/启动 Activity 由持久模拟器
  `bbb_manual_show` 的系统 resolver 解析）、`app_reproduction/materials/apps/`
  每目标素材包、`review_specs/<app_id>.json` 逐条评测清单（598 条要求，
  features 含标题/流程/验收标准三段式）。
- **功能排除边界（exclusions，2026-09-15 上线）**：web 28 目标各配套排除清单
  （28 组共 438 条：多人协作、账户/支付、实时数据、AI 生成、外部服务等刻意
  不复现的功能面），嵌在 `review_specs/<app_id>.json` 的 `exclusions` 键，
  `Checklist` 严格校验（非数组/条目非对象/缺 feature 直接报错——损坏条目等于
  静默扩大评分范围，必须失败）。边界在四个回执点自动下发：探索侧
  `start_session` + `finalize`（blackboxbench），评测侧 `start_evaluation` +
  `evaluation_status`（web-review）；探索 Agent 不测不记不复现，评测 Agent
  不评分（产物缺失排除面不算缺陷）。两侧读同一份清单文件，定义唯一。
  源数据维护在数据集主表（url.xlsx）的"功能排除"工作表。
- **数据边界**：2026-09-07 环境改版（复现沙盒网络策略改为"开放 + Skill 纪律"、
  per-app 素材包上线）之前的全部 Android 端探索/生成/评审产物已归档于 `cache/`，
  与主线隔离；改版前后的样本不可混入同一数据集比较。

## 不可违反的约束

- 托管模式只有 `benchmark/runtime/` 可以接触浏览器。自建探索会话只有可信侧
  `docker/raw_device_bridge.py` 可以接触 X11；它不得进入 Agent 镜像，也不得增加
  URL、DOM、网络、selector、探索工具或发现语义。独立的操作员登录 Compose 可让
  `docker/self_login_ui.py` 临时接触 X11，但不得同时启动 Agent，只能发布像素/坐标
  操作且必须绑定 host loopback。
- CDP 白名单仅允许 Page 导航/历史/截图、Input、固定 viewport 和 Browser.close；
  禁止 Runtime.evaluate、DOM、Accessibility、Network、Storage 等语义通道。
- `/agent/{sid}/` 响应保持白名单字段，Action 回执不得包含语义结果。
- 托管模式的 discovery 必须通过真实 frame/step evidence 校验和 LEAK_PATTERN 检查。
- `sample_apps/*/ground_truth.json` 不得通过 HTTP 暴露或进入任何 Docker 镜像
  （`.dockerignore` 已排除全部 build context）。
- 自建工具模式的内置样例条件中两个容器保持 `network_mode: none`。显式公网 URL
  条件允许 `reference_raw` 与 `tool_builder` 普通出网：浏览器仍经 loopback public-only
  proxy，Agent 的网络使用靠 Skill 软约束，只可取得与目标无关的通用工具依赖，不得
  直接请求、搜索或下载目标内容。两容器不共享网络命名空间，只共享硬件式设备卷；
  `tool_builder` build context 必须保持为 `self_explorer/agent_image/`，禁止改成仓库根。
- 自建工具 Agent 只能获得通用 workspace 操作；workspace 必须为空，禁止放入协议、
  示例、starter code、辅助库或输出模板。不得要求或暗示其构建拓扑、报告、证据包、
  特定探索工具、覆盖计划或固定输出结构。Skill/MCP 可以明确目标浏览器已经启动并给出
  浏览器 I/O 根位置，但不得说明设备文件格式、输入协议、客户端实现或使用步骤。禁止复用或挂载
  `benchmark/`、`agent_sdk/`、`agents/cli_explorer/`、Controller API 或 Runtime RPC。
- 自建会话默认保存在项目内 `runs/self_built/self_*/`（`BBB_SELF_RUNS_ROOT` 可覆盖）；
  Agent 容器只能挂载本次 `workspace`。由于项目内存储不构成宿主文件系统隔离，正式
  实验必须禁用客户端宿主文件/shell 工具或使用客户端容器化严格模式。
- 自建公网登录态必须由 `scripts/self_live_login.py` 在 Linux 可信浏览器中建立；
  `docker_profile_golden` 只能只读挂载给 `reference_raw` 并复制到会话 tmpfs，禁止挂载
  给 `tool_builder`、Agent 客户端、workspace、证据交接或复现容器。登录维护与同站点
  探索不得并发。
- 自建会话期间 `sample_apps/*/ground_truth.json` 由 `self_explorer/hardening.py`
  事务性封存（`.seal/`），会话结束自动恢复；崩溃遗留由 `launcher recover` 或下次
  启动自动找回，禁止手工删除 `.seal/`。`BBB_SELF_SEAL=0` 仅供开发。封存为跨进程
  租约：多个自建会话可并行共享同一封存，恢复只发生在最后一个存活持有者释放后。
- 并行实验：托管宿主模式（本地 runtime）支持同时多个 session（每个 session 独立
  app 子进程与浏览器）；自建模式每个 Agent 一个 MCP 进程、独立 Compose 项目；
  复现素材重建与沙箱镜像构建由跨进程文件锁（`reproduction/filelock.py`）串行化。
  docker 托管模式（BBB_RUNTIME=docker）共享单一 reference 浏览器，同一时刻只允许
  一个 session。Android 多 Agent 同目标并行依赖：target lease（explore 可多持有、
  login 互斥）、全局端口命名空间、每会话独立 AVD clone、只读共享 APK、
  `app_output/<handoff_id>` 与 `app_output/evaluations/<run_id>` 的独占创建
  （碰撞必须报错而非覆盖）、构建镜像文件锁、启动前内存准入。
- 自建 `finish_workspace` 只执行安全与完整性指纹检测（ground truth/被测 App 源码/
  设备桥/托管实现/跨会话工件 + 探索期 Agent 自写文本 LEAK 扫描）。常规第三方依赖根
  （如 `.deps`、`.venv`、`node_modules`）不参与内容扫描且不得进入复现交接，避免把
  上游包源码误判为 Agent 泄漏。不得以缺少拓扑、报告、
  证据、最少状态/功能或固定目录为由拒绝交接；安全检测失败仍必须拒绝。工具调用写入
  `run_dir/audit.jsonl`（Agent 不可达）。
- 自建模式必须由任务显式提供 `target_url`；它只是起始入口，不是 host/URL 白名单。
  可信浏览器必须允许目标流程的多级重定向、跨域资源和类人导航。URL 不是语义数据
  通道；公网启动器与可信浏览器代理仍拒绝凭据、非常用端口及任何非 global 解析地址。
  `tool_builder` 的普通出网不采用目标 URL 白名单，直接请求、搜索或下载目标的禁令
  写入 Skill 并作为实验合规性要求审计。
- 托管 finalize 必须通过跨会话筛查（`benchmark/topology/crosscheck.py`：定稿拓扑
  不得逐字复现其他 session 成品或 `website_output/` 交付物），命中返回 409 且
  会话保持可修正。
- 严格模式客户端容器化（`self_explorer/agent_runtime.py`、
  `agents/cli_explorer/agent_runtime.py`）：容器不得挂载仓库或 docker.sock，只读
  挂载 shim 与 Skill；MCP TCP 模式必须绑定 127.0.0.1 并强制 per-run token；
  一次连接即一次完整生命周期。两条件的 TCP/shim 实现保持独立，不得共享。
- 所有 loopback HTTP 必须禁用系统代理；公网 live target 浏览器/VLM 可按需使用代理。
- 复现 Agent 不得接触 Reference App 源码、ground truth、live profile 或原站语义数据。
- 复现交接的探索素材必须是新建白名单视图：托管模式只包含可见帧和定稿
  报告；自建模式中立收集 Agent 自发保留的安全文本/媒体，不依赖路径或命名。禁止直接挂载
  整个 `runs/` 或自建 workspace，禁止交接自建工具源码或可执行文件。
- `reproduction/materials/` 是公共只读素材源；Agent 只能写 `website_output/`。
- 数据库或后端需要修改时，必须先复制到 `website_output/`，禁止原地运行写操作。

## 核心目录

- `benchmark/orchestrator/`：Session、预算、生命周期、目标注册和 live precheck。
- `benchmark/runtime/`：Local、Live、Docker pixels+HID Runtime。
- `benchmark/scratch.py`：仓库外易失运行态（浏览器 profile、AVD clone）与按 owner
  PID 回收；`pid_alive` 是全项目"owner 已死则回收"规则的共同基础。
- `benchmark/recorder/`：截图、光标、动作和观察轨迹。
- `benchmark/topology/`：模型、证据校验、增量图和 finalize。
- `agents/cli_explorer/`：通用 MCP Server、安装器和探索 Skill。
- `self_explorer/`：Agent 自建工具模式的空白工作台、生命周期、MCP 和隔离镜像。
- `agent_sdk/`：Python pixels-only SDK。
- `sample_apps/ecommerce_demo/`：确定性参考应用。
- `reproduction/materials/`：合成内容、媒体、SQLite 和可复用微型后端。
- `website_output/`：复现 Agent 唯一允许写入的网页结果目录。
- `benchmark/android/`：Android APK 目标注册、可信 Emulator Runtime 与工具链发现。
- `agents/android_baseline/`：Android 托管基线 MCP、安装器与 Skill。
- `agents/android_our_method/`：Android our-method 独立 MCP、门禁与 Skill。
- `app_reproduction/`：固定 Compose 脚手架、移动素材、离线 APK 构建和像素复测。
- `app_evaluation/`：安装后 Android 功能清单四档评测。
- `web_evaluation/`：网页交接产物的四档评测会话（像素+类人输入+源码只读通道）。
- `agents/web_review/`：网页评审 MCP、安装器与 Skill。
- `app_output/`：Android 复现 Agent 唯一输出目录；`project/` 可写，`review/` 与
  `artifacts/` 由可信侧管理。`evaluations/` 存放 app-review 清单评测产物。
- `app_reproduction/materials/apps/`：20 个 per-app 复现素材包
  （CATALOG.md + SUPPLEMENT.md + 虚构实体素材）。
- `review_specs/`：人工功能要求清单（web/android 各数据集，`Checklist` 校验；
  app_id 与目标注册一一对应）。
- `cache/`：2026-09-07 环境改版前的 Android 端历史产物归档（4 个复现交接、
  4 份 checklist 评测、13 个探索会话），只读隔离，不参与任何聚合比较。

## Android 条件

- Android Agent 只允许截图、tap、long_press、swipe、type_text、返回、确认、等待和
  重启 App；ADB、UIAutomator、Accessibility、selector、logcat、dumpsys、APK 和设备
  文件只能存在于可信 Runtime，任何结果不得回传 Agent。
- 网页 baseline/our-method 与 Android baseline/our-method 是四个独立条件。Android
  两个条件可以共享 `benchmark/android`、Recorder、Topology 与 `app_reproduction`，
  但不得互相 import 方法层实现；Android 改进不得反向修改网页条件行为。
- 用户 APK 存入 `runs/android_targets/`，默认断网。公网模式缺少受控 proxy 与 network
  guard 时必须拒绝，不得回退到不受限网络。
- 每个探索或复测会话必须使用独立 AVD clone、ADB serial、端口和目录；Agent 不得离开
  目标 App，权限控制器和受控文件选择器是唯一允许的临时系统界面。
- Android 端口预留必须来自唯一的全局命名空间
  （`runs/android_targets/port_locks/`，见 `benchmark/android/runtime.py`
  的 `port_locks_dir()`）。禁止再按 `work_dir` 相对推导锁目录：不同会话类型的
  层级不同会把预留切成互不可见的命名空间，两个会话各自"占用" emulator-5554，
  随后 `adb -s` 指向已死或错误设备（表现为 screencap exit -1）。预留还必须探测
  console/adb 端口对，孤儿 qemu 仍在应答时不得发放该 serial。
- Android 模拟器启动前必须做内存准入（`BBB_ANDROID_MIN_FREE_MB`，默认 5120）。
  宿主内存不足时 adb 不会干净失败，而是随机丢单条命令；必须明确拒绝而非放行。
- 每个会话的 AVD clone 数 GB、浏览器 profile 数百文件，都是易失运行态，必须建在
  **仓库之外**的 scratch（`BBB_SCRATCH_DIR`，默认 `%TEMP%/blackboxbench-scratch`，
  见 `benchmark/scratch.py`）。`runs/` 只存证据（帧、轨迹、拓扑）与诊断日志。
  scratch 目录名带 owner PID，崩溃遗留在下次启动前自动回收，无需登记表。唯一例外
  是 Android login 维护：其 clone 保留在 `runs/android_targets/login_work/` 固定
  路径，供操作员取出 golden profile。曾因把它们建在会话目录内，每次 teardown 要
  删数千个工作区文件，并遗留 7.8GB。
- 会话进入 `failed` 必须立即归还它持有的一切：模拟器/浏览器进程、target lease、
  端口预留。observe 与 action 两条路径都要走同一失败处理，否则一次瞬时设备抖动
  会把目标锁死到 Controller 重启。Controller 被强杀后残留的 `status: running`
  僵尸元数据必须在下次启动时改写为 failed（帧/轨迹/拓扑保持原样）。
- 瞬时传输故障必须重试而非上报：CDP 退避重试约 12s 并在连接级失败后重连，adb
  重试前先 wait-for-device。重试仍失败时按**设备/浏览器是否仍健康**分级：健康 →
  可恢复（HTTP 409，只损失这一步，会话存活）；不健康 → 会话 failed 并归还资源。
  环境噪声不得被记录成对 Agent 的观察。
- **探测语义**：`_device_responsive()` 的探测与被探测对象共享同一宿主内存压力，
  探测自身超时/OSError 必须保守读作**可恢复**（探测沉默 ≠ 设备死）；只有 adb 明确
  回答非 device 状态且 qemu 进程仍存活才是终局。探测沉默误判终局曾在 2026-09-01
  双开时丢掉 4 个会话（探测行为由回归测试守护）。`am start` 抖动的重试窗口同样
  按内存压力实测加宽（5 次、平方退避）。
- **失败消息绝不泄漏可信侧机制**。adb 原始错误含工具路径、命令行与 emulator
  serial，CDP 错误含方法名与 ws URL；它们只能进本地 trace。回传 Agent 的必须是
  中性句子（`DeviceError` / `_agent_safe_message`）。泄漏等于告诉 Agent ADB/CDP
  存在及其用途，违反 pixels-only 边界。
- Android 内存准入必须在跨进程锁内完成"检查 + 预留"，并为**启动中**的模拟器扣除
  预算（`BBB_ANDROID_EMULATOR_MB`，默认 4608，按 2026-09-01 双开实测 ~4.1GB/qemu
  定标；此前 3072 的估值会让并发启动共同超配）。否则两个并发启动会各自认为内存
  足够而共同超配，正是 adb 随机丢命令的诱因。
- `type_text` 只能注入 ASCII。设备 KeyCharacterMap 无 CJK 映射，`input text` 对
  CJK 必然失败（任何引号或转义都无效）；非 ASCII 必须作为可恢复的无效动作拒绝
  （会话存活），并引导 Agent 用屏幕键盘点按。
- Android 交接必须使用脱敏白名单副本。目标 APK、profile、原 App 私有数据、真实凭据
  与 protected strings/regions 不得进入 `/exploration`、`/input`、复现项目或 APK。
- Android 构建容器固定 Kotlin + Jetpack Compose、离线依赖（`gradle --offline`）；
  隔离来自只读根、cap-drop 与 no-new-privileges。**网络开放但受 Skill 纪律约束**
  （2026-09-07 起的基准环境）：优先使用 `/materials/app` 与公共素材，仅当补充材料
  确实缺少必要公开资料（格式规范、API 文档）才联网查询，绝不上传探索截图、敏感
  内容、工程文件或会话数据。只能挂载本次 `project/`、脱敏交接、公共虚构素材与
  per-app 素材包。危险权限、私人值、接受后改写或缺少可安装 APK 都必须阻止完成。
- **Per-app 素材包**：`app_reproduction/materials/apps/<app_id>/` 在目标有包时以
  只读 `/materials/app` 挂载，含 `CATALOG.md`（实体素材清单）与 `SUPPLEMENT.md`
  （非实体领域信息：格式规范、接口概念、行为规则），实体素材必须虚构且确定性。
  包目录树纳入复现期间防篡改指纹（finish 时重算比对）。**白名单必须同步**：
  `agents/android_our_method/mcp_server.py` 的 `_material_roots()` 需动态纳入
  当前会话的 `app_materials_dir`（曾漏改导致 `input_read /materials/app` 被拒，
  回归测试 `test_android_ours_input_read_accepts_per_app_materials` 守护）；
  baseline 无 input 工具、走 `workspace_run` 容器内直读，不经宿主白名单。

## 常用命令

```powershell
vendor/python/python.exe -m benchmark.server --port 7800
vendor/python/python.exe -m pytest tests/ -q
vendor/python/python.exe scripts/e2e_smoke.py
vendor/python/python.exe -m agents.cli_explorer.install
vendor/python/python.exe -m self_explorer.install
vendor/python/python.exe -m reproduction.materials.build
vendor/python/python.exe scripts/setup_android.py --preflight
vendor/python/python.exe scripts/build_android_reproduction_image.py --accept-licenses
vendor/python/python.exe scripts/build_android_sample.py
vendor/python/python.exe -m agents.android_baseline.install --cli codex
vendor/python/python.exe -m agents.android_our_method.install --cli codex
vendor/python/python.exe -m agents.app_review.install --cli codex
vendor/python/python.exe -m agents.web_review.install --cli codex
vendor/python/python.exe scripts/android_launch_preflight.py --app google_clock --condition our-method --close
vendor/python/python.exe scripts/android_parallel_check.py --app google_clock
vendor/python/python.exe scripts/android_locks.py --reclaim
vendor/python/python.exe scripts/android_target.py list
vendor/python/python.exe scripts/android_target_smoke.py --app <app_id>
```

## Session 清理

测试必须使用 pytest 临时目录，不得写正式 `runs/`。跑 pytest 必须带
`--basetemp=.test-tmp/<name>`，否则 IDE 的批量删除保护会中断清理。自建工具测试必须
mock Docker，不能实际创建 `runs/self_*`。清理 `runs/` 前先查询
`GET /api/sessions` 并确认没有 running Session，再停止相关 Controller/MCP/Chrome
进程。只清理 `runs/sess_*`；`runs/live_targets/` 是敏感登录资料，必须保留。
Android 会话还要跑 `scripts/android_locks.py --reclaim` 回收 lease 与端口预留，
必要时加 `--kill-emulators` 清掉孤儿 qemu。`cache/` 是改版前的历史归档，
**只读、永不清理**，不属于任何清理脚本的范围。

PowerShell 会吞 `$` 变量并破坏中文，复杂脚本请写成文件再执行。

## Live target

- 登录态通过 `scripts/live_login.py` 人工维护。
- 创建 Session 时执行头像像素预检，失败则拒绝启动。
- 只允许只读探索，不能点赞、评论、关注、购买或修改设置。
- 公网 live target 不使用入口 URL/域名白名单；允许正常重定向和跨域资源加载，
  仅通过 brief/Skill 软限制不得搜索、下载或直接请求目标原始实现。像素/HID 与
  只读行为边界保持不变。
- live target 无确定性 S0；reset 仅冷启动浏览器回入口。

## 修改原则

- 优先保持探索内核小而明确，不重新引入专用模型 harness、展示层或重复 runner。
- **失败归属必须先判性质**（判据与已判定实例见 `docs/evaluation_contract.md` §11）：
  Agent 做了合法的事却拿不到结果 = 平台缺陷，必须修并重跑受影响会话；平台正常服务
  而 Agent 选择不做或做得不好 = 能力/方法差异，**不得修**，如实计入结果。检验方式是
  "同平台下别的条件/会话是否做到了"。判定必须基于 `discovery.jsonl` 的拒绝记录、
  `observations.jsonl` 的 `settle_reason`、`actions.jsonl` 的 `environment_degrading`
  与 crash report，不得凭印象或从截图反推。
- 诊断信息只写本地 trace（Agent 不可达），不得因为"便于分析"而扩大 Agent 可见通道。
- 新功能必须说明是否扩大 Agent 可见通道；两个探索条件不得互相导入或共享实现。
- 修改 Runtime、Agent API、evidence 或网络边界后必须运行对应安全测试和完整测试。
- 新 Reference App 按 `docs/adding_reference_app.md` 接入。
- 素材必须是虚构、自包含、可离线重建的，不引入真实用户信息或不明版权内容。
- 素材包（`materials/apps/*/SUPPLEMENT.md`）描述**目标应用应有的领域行为规范**
  （如"定时器归零进入完成态"），对全部条件平等开放；它是公平信息源，不是答案。
  被测条件未实现某功能属于测量结果，**不得**通过改素材、skill 或提示去引导
  Agent 修复——那是被测能力的一部分（与"失败归属先判性质"同一原则）。
- 公共素材和初始数据库不得被 Agent 原地修改；所有生成结果写入 `website_output/`。
