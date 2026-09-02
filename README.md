# BlackBoxBench

BlackBoxBench 是一个面向 AI Agent 的黑盒 App 探索内核。Agent 只能看到屏幕
像素，并且只能发送坐标级鼠标/键盘输入。托管条件保存可审计轨迹并形成带真实证据的
Functional Topology Graph；自建条件不规定探索工具、记录格式、拓扑或工作流，用来
观察 Agent 在仅有目标与反越狱限制时的原初表现。

```text
Unknown App ──pixels──▶ Agent ──human-like HID──▶ Unknown App
                           │
                           └── evidence ──▶ Functional Topology
```

当前仓库把两种探索条件都接到同一个下游复现工作区。托管模式交接 finalized
topology；自建模式不要求 topology，只中立交接 Agent 自发留下且通过安全筛选的普通
记录或媒体。同一 Agent 随后进入断网容器，使用公共虚构素材，只在
`website_output/<handoff_id>/` 中生成网页。原站/复现站差分评测仍由后续阶段完成。

## 两种探索条件

项目现在保留两条独立入口：

| 条件 | Agent 获得什么 | Agent 自己负责什么 |
|---|---|---|
| 托管工具 | PNG 观察、坐标动作、证据记录 MCP/SDK | 规划与功能探索 |
| 自建工具 | 明确起始 URL、反越狱限制和空白隔离工作区 | 自主决定如何认真探索 |

第二种条件没有 Controller、原有 MCP/SDK、设备说明或现成截图/点击工具。内置样例时
两个容器都断网；显式公网 URL 时可信浏览器经 public-only proxy 出网，Agent 工作台
也可普通出网但由 Skill 软限制用途，不设置入口域名白名单。两者只共享硬件式设备卷；Agent 镜像的构建上下文不含本仓库源码，
workspace 启动时为空。详见 [self-built explorer](self_explorer/README.md)。

## 不可破坏的边界

- Agent 只能经 `/agent/{sid}/...` 使用截图、预算、坐标输入和 discovery 工具。
- 禁止 DOM、Accessibility、脚本执行、selector、URL、网络、Cookie 和存储读取。
- 本地 CDP 只允许 Page 导航/截图、Input、固定 viewport 和 Browser.close。
- Action 回执不包含"点到了什么"等语义结果。
- confirmed 的 Feature/Edge 必须引用当前 Session 的真实 step/frame。
- `sample_apps/*/ground_truth.json` 不通过 HTTP 暴露，也不进入 Agent 容器。
- 自建工具条件只在隔离环境底层保留像素/输入设备，不向 Agent 说明接口，且不能接触
  托管工具实现。

详见 [security model](docs/security_model.md)。

## 最小架构

```text
Agent framework
  └─ MCP or Python SDK
       └─ Controller
            ├─ Session: budget / lifecycle / action validation
            ├─ Runtime: screenshot in / HID out
            ├─ Recorder: frames + JSONL trace
            └─ Topology: evidence-validated discoveries
                 └─ Reference App
```

保留三种 Runtime：

- `LocalChromiumRuntime`：本地开发，受限 CDP，确定性 reset。
- `LiveChromiumRuntime`：真实网站，持久登录 profile、正常公网导航和视觉预检。
- `DockerX11Runtime`：正式评测隔离，容器内使用 Xvfb/xdotool/scrot，无 CDP。
- `AndroidEmulatorRuntime`：可信宿主模拟器，只向 Agent 返回 PNG 并接收坐标级触控。

### Android App 探索与 APK 复现

Android 是与网页并列的独立链路，包含托管 `android-baseline` 和增强
`android-our-method` 两个条件。它支持项目内确定性样例 APK 和操作员注册的本地 APK；
Agent 不会接触 APK 文件、ADB、控件树、日志、设备文件或反编译结果。探索完成后在固定
Kotlin + Jetpack Compose 离线工作区生成可安装 APK，并在另一台独立模拟器中进行多轮
像素复测。

环境安装、APK 注册、两个 Skill 的接入方式和输出结构见
[Android platform](docs/android_platform.md)。
完整的大文件、Android SDK、JDK、Python 包和 Docker 镜像依赖见
[依赖需求表](docs/dependency_requirements.md)；可用
`vendor/python/python.exe scripts/verify_dependencies.py --full` 做只读验收。

## 快速开始

```powershell
# 首次准备项目内 Python 和 Chromium
python scripts/bootstrap.py

# 启动 Controller
vendor/python/python.exe -m benchmark.server --port 7800
```

Controller 没有内置 Dashboard；操作员通过精简 `/api/...` 接口创建、查看、重置和
关闭运行中的 Session。

### MCP Agent 入口（推荐）

```powershell
# 一次性注册 MCP Server 和探索 Skill
vendor/python/python.exe -m agents.cli_explorer.install
```

之后在支持 MCP 的 Agent 中调用 `blackboxbench` 工具。MCP Server 可以自举
Controller 和 Session，也可以通过环境变量绑定已有 Session：

```text
BBB_CONTROLLER=http://127.0.0.1:7800
BBB_SESSION=sess_...
BBB_APP_ID=ecommerce_demo
BBB_MAX_ACTIONS=1500
BBB_MINUTES=180
```

探索目标通过 `start_session` 指定为已注册 `app_id` 或公网 URL。

### 自建探索工具入口（已归档）

自建模式已完成其实验周期，**当前已从所有 Agent（Codex / CodeBuddy / Kimi）
注销**，不再作为可安装的正式实验条件。代码与文档保留在 `self_explorer/`
作归档，需要复现历史实验时可重新安装：

```powershell
vendor/python/python.exe -m self_explorer.install --cli kimi --app ecommerce_demo
```

历史运行的产物仍在 `runs/self_built/self_*/workspace/` 与
`website_output/<handoff_id>/`。归档模式的安全约束（Docker Compose 隔离、
登录 Profile 管理、封存机制等）见 [self-built explorer](self_explorer/README.md)。

### Codex 接入

当前活跃的两种条件使用不同的用户级 MCP 名称与 Skill：

```powershell
# 托管基线（冻结，用于对照）
vendor/python/python.exe -m agents.cli_explorer.install --cli codex --app ecommerce_demo
# our-method（演进副本：素材硬闸 / 证据帧索引 / 写路径证据验收）
vendor/python/python.exe -m agents.our_method.install --cli codex --app ecommerce_demo
```

重新打开 Codex 任务后，分别使用 `$blackbox-explorer ecommerce_demo` 和
`$our-method ecommerce_demo`。两个条件可以同时注册并在不同任务中并行运行；每个
Skill 都明确禁止寻找、读取、比较或借鉴其他探索条件。若操作员另有严格客户端容器
隔离需求，再为单次正式实验只暴露一个 MCP。our-method 与基线的差异见
[our-method](agents/our_method/README.md)。

### Python SDK

```python
from blackbox_bench_sdk import Environment

env = Environment("http://127.0.0.1:7800", "sess_...")
obs = env.observe()
result = env.click(812, 431)
env.record_feature(
    "Search products",
    behavior={"postconditions": ["只显示匹配商品"]},
    evidence=[{
        "step": result.step,
        "before_frame": obs.frame_id,
        "action": "click(812,431)",
        "after_frame": result.frame_id,
    }],
)
env.finalize()
```

## 探索能力

类人输入包括：click、double click、pointer move、mouse down/up、drag、type、
key press/down/up、scroll、wait、switch tab 和 close tab。坐标系固定为
1440×900、DPR=1、左上角原点。

结构化发现包括：

- `STATE`：视觉状态。
- `FEATURE`：可触发功能及前置/后置/错误/持久化行为。
- `DATA`：可观察数据实体。
- 关系：REQUIRES、TRANSITIONS_TO、MUTATES、ENABLES、DISABLES、
  PERSISTS_TO、REVEALS、DEPENDS_ON、CONFLICTS_WITH、VALIDATES。
- Hypothesis：未验证问题及后续探针。
- Revision：update、merge、delete，用于修正探索早期误判。

## Session 工件

每次正式探索写入 `runs/<session_id>/`：

```text
session.json
frames/frame_*.png
actions.jsonl
observations.jsonl
discovery.jsonl
topology.json
hypotheses.json
functional_topology.json
functional_topology.md
coverage_report.json
session_summary.json
crash_report.json          # 仅失败时
```

`runs/live_targets/` 保存真实站点的人工登录 profile，属于敏感运营数据。清理普通
Session 时只删除 `runs/sess_*`，不能删除 `live_targets/`。

## Reference targets

- `ecommerce_demo`：种子化电商应用，支持登录、搜索、购物车、优惠券、下单、订单、
  持久化和确定性 reset。
- `douyin_web` / `bilibili_web` / `yuque_web`：人工维护登录态的真实只读目标。
- 任意公网 URL：临时 live target，自动生成 app ID 和站点 profile。

新目标接入见 [adding reference app](docs/adding_reference_app.md)。

## 目录

```text
benchmark/      Controller、Session、Runtime、Recorder、Topology
agent_sdk/      pixels-only Python SDK
agents/         通用 MCP Server、安装器和探索 Skill
self_explorer/  Agent 自建探索工具的独立工作台、原始设备契约和隔离启动器
sample_apps/    确定性 Reference App
docker/         正式隔离部署
schemas/        Topology/State/Feature/Interaction Schema
docs/           API、安全、架构、接入说明
scripts/        bootstrap、live login、最小 e2e smoke
tests/          核心行为、安全、MCP、reset、多标签和 topology 测试
runs/           运行工件（Git 忽略）
reproduction/materials/  公共只读素材、SQLite 和后端模板
website_output/          Agent 生成网页的固定目录（内容被 Git 忽略）
benchmark/android/       Android 目标注册、工具链发现与可信 Emulator Runtime
app_reproduction/        Compose 脚手架、移动素材、离线构建与 APK 复测
app_evaluation/          Android 功能清单四档评测
app_output/              Agent 生成 Android 工程、复测证据和最终 APK
```

## 探索后自动复现

两种模式的完成操作都会立即启动一个独立复现容器：

```text
managed finalized topology          -> /input/functional_topology.json (read-only)
reproduction/materials/             -> /materials (read-only)
filtered voluntary artifacts        -> /exploration (read-only)
website_output/<handoff_id>/        -> /workspace (read-write)
```

容器使用 `network=none`、只读根文件系统、资源上限和 capability drop，不挂载
仓库、Reference App 源码、ground truth 或任一探索实现。需要图片、SQLite
或后端时，必须先复制到输出目录后再修改。`/exploration/manifest.json`
列出允许复用的本次探索工件；自建模式不要求该清单中必须有任何内容，也不会把整个
Session 目录暴露给 Agent。

详细说明见 [reproduction kit](reproduction/README.md)。

## 验证

```powershell
vendor/python/python.exe -m pytest tests/ -q
vendor/python/python.exe sample_apps/ecommerce_demo/smoke_test.py
vendor/python/python.exe scripts/e2e_smoke.py
```

测试使用 pytest 独立临时目录，不再向正式 `runs/` 写测试 Session；自建模式的单元
测试 mock Docker，也不会生成 `runs/self_*`。

## 当前范围

当前负责"受控探索 → 条件对应的安全交接 → 隔离复现工作区 → 人工清单四档评测"；
其中证据轨迹与功能拓扑只属于托管探索条件。评测由 LLM judge 在 Skill 内做语义判断，
代码只做薄护栏（证据引用真实截图、四档封闭、不漏项、报告 schema 固定），见
`docs/evaluation_contract.md`。Android 侧为 `agents/app_review/` +
`app_evaluation/`；网页侧的 `web-review` Skill 尚未收编入仓库。

以下内容暂不包含：

- 专用 VLM 调用循环；Agent 规划由外部 MCP Agent 框架负责。
- 脚本化坐标 Demo。
- Dashboard、视频、历史 Session 浏览和 deterministic replay。
- 原站/复现站的**像素级**自动差分：评的是功能是否实现，按钮位置、文案、配色与
  素材差异都不构成降级理由。
- 网页条件仍不规定固定前端或后端技术栈；Android 复现固定使用 Kotlin + Compose。
