# BlackBoxBench

BlackBoxBench 是一个面向 AI Agent 的黑盒 App 探索与复现基准测试平台。Agent 只能
看到屏幕像素，并且只能发送坐标级鼠标/键盘/触控输入。平台支持 Web 与 Android 两个
独立链路，每个链路提供**托管工具**与**消融变体（nograph）**两种探索条件，探索完成后
自动进入隔离复现工作区生成可交付产物，并由独立 LLM judge 按人工功能清单做四档
评测。全流程产生的工件（`runs/`、`app_output/`、`website_output/`）均可离线计算
13 项标准化指标（WFS、覆盖率、视觉相似度、幻觉率、token 效率等），用于跨模型、
跨条件的系统性对比。

```text
Unknown App ──pixels──▶ Agent ──human-like HID──▶ Unknown App
                           │
                           ├── evidence ──▶ Functional Topology（托管条件）
                           │                    │
                           ▼                    ▼
                     Reproduction Workspace ──▶ LLM Judge ──▶ Metrics
```

## 探索条件矩阵

| 平台 | 条件 | Skill 名称 | Agent 获得什么 | 记录要求 |
|---|---|---|---|---|
| Web | 基线 | `blackbox-explorer` | PNG、坐标输入、discovery MCP | 按指定 DAG 记录 |
| Web | 消融 | `web-nograph` | PNG、坐标输入（无 discovery 工具） | 自由记录或完全不记录 |
| Android | 基线 | `android-blackbox-explorer` | PNG、坐标触控、discovery MCP | 按指定 DAG 记录 |
| Android | 消融 | `baseline-nograph` | PNG、坐标触控（无 discovery 工具） | 自由记录或完全不记录 |

消融条件与基线的唯一差异是**记录工具面**：MCP 工具面 = 基线减去 7 个 discovery
记录工具（record_state / feature / data / edge / hypothesis / resolve / revise），
探索纪律（预算、边界、finalize 流程）与复现链路完全保持一致。这使得"是否有结构化
记录"成为唯一被测变量，可用于量化记录工具对探索质量与复现保真度的影响。

自建工具条件（Agent 完全自主决定工具与工作流）已归档于 `self_explorer/`，不再作为
活跃实验入口，但代码与文档保留用于复现历史实验。

## 不可破坏的边界

- Agent 只能经 `/agent/{sid}/...` 使用截图、预算、坐标输入和 discovery 工具。
- 禁止 DOM、Accessibility、脚本执行、selector、URL、网络、Cookie 和存储读取。
- 本地 CDP 只允许 Page 导航/截图、Input、固定 viewport 和 Browser.close。
- Action 回执不包含"点到了什么"等语义结果。
- confirmed 的 Feature/Edge 必须引用当前 Session 的真实 step/frame。
- `sample_apps/*/ground_truth.json` 不通过 HTTP 暴露，也不进入 Agent 容器。
- Android 侧 Agent 不接触 APK 文件、ADB、控件树、日志或反编译结果。

详见 [security model](docs/security_model.md)。

## 最小架构

```text
Agent framework (kimi / zcode / dsh / codex / any MCP client)
  └─ MCP or Python SDK
       └─ Controller (benchmark.server)
            ├─ Session: budget / lifecycle / action validation
            ├─ Runtime: screenshot in / HID out
            │    ├─ LocalChromiumRuntime（本地开发
            │    ├─ LiveChromiumRuntime（真实网站
            │    ├─ DockerX11Runtime（正式网页评测隔离
            │    └─ AndroidEmulatorRuntime（Android 模拟器
            ├─ Recorder: frames + JSONL trace
            └─ Topology: evidence-validated discoveries
                 └─ Reference App / Android APK
```

### Android 链路

Android 探索通过 `AndroidEmulatorRuntime` 在可信宿主模拟器上运行。Agent 收到
PNG 截图并发送坐标级触控，不能接触 ADB、控件树或日志。探索完成后自动进入离线
Kotlin + Jetpack Compose 工作区，使用公共虚构素材生成可安装 APK，并在另一台
独立模拟器中进行多轮像素复测（self-review）。自检收敛由双闸保障：

- **观察预算**（`BBB_REVIEW_BUDGET`，默认 150）：单次自检允许的截图观察总数。
- **修订上限**（`BBB_REPRO_REVISIONS`，默认 6）：自审发现→修复的最大循环次数。

### 复现阶段

finalize 后自动启动隔离复现容器（`network=none`、只读根文件系统、capability
drop）：

```text
托管 finalized topology   → /input/functional_topology.json (read-only)
公共虚构素材              → /materials (read-only)
过滤后的探索工件          → /exploration (read-only)
复现输出                  → /workspace (read-write)
```

Android 侧在 `app_output/<handoff_id>/` 产出完整 Kotlin + Compose 工程与签名
APK；Web 侧在 `website_output/<handoff_id>/` 产出静态网站。

## 评测体系

探索与复现完成后，独立 LLM judge（`gpt-6-astra`）按人工功能清单
（`review_specs/<app_id>.json`）逐项操作复现产物并给出四档评分：

| 档位 | 权重 | 含义 |
|---|:---:|---|
| `full` | 1.0 | 功能完整实现，含持久化 |
| `partial` | 0.5 | 核心可用但不完整 |
| `placeholder` | 0.25 | 界面存在但无实际功能（装饰性） |
| `broken` | 0.0 | 缺失或报错 |

### 13 项标准化指标

全部指标从已产出的工件离线计算，不需要额外 Agent 交互：

| 族 | 指标 | 来源 |
|---|---|---|
| **A 复现保真** | 四档分布 / WFS / persistence-WFS | `evaluation_report.json` |
| **B 探索质量** | 覆盖率 / 有效动作率 / 无效动作率 / 探索视野 | `actions.jsonl` + coverage judge |
| **C 视觉相似** | CLIP-L/14 覆盖折扣相似度 | 探索帧 × 评测帧 |
| **D 复现过程** | 修正轮数 / 幻觉率 | `review_summary.json` |
| **E 鲁棒性** | 平台归因故障 | `crash_report.json` |
| **F 效率** | 分阶段 token / tokens-per-fidelity | CLI 日志 |

覆盖率（B1）通过 gpt-6-astra 视觉 judge 被动判定探索 keyframes 与 checklist 的
交集；视觉相似度（C1）对每个 checklist 任务计算 CLIP 余弦相似度（探索帧 vs 复现
帧），未覆盖或未复现的任务计零。一条龙指标计算：

```bash
bash scripts/full_metrics_android.sh <sid> <app> <handoff> <agent> [eval_report]
```

## 快速开始

### 环境准备

```bash
# 首次准备项目内 Python 和 Chromium
python scripts/bootstrap.py

# Android 环境（可选）
vendor/python/bin/python scripts/setup_android.py
vendor/python/bin/python scripts/android_launch_preflight.py --app <app_id> --condition nograph
```

### 启动 Controller

```bash
vendor/python/bin/python -m benchmark.server --port 7800
```

### 批量启动探索

```bash
# Android nograph 探索
vendor/python/bin/python scripts/launch_batch.py --max-parallel 4 \
  explore dsh nonogram nograph explore zcode nonogram nograph

# Web 探索
vendor/python/bin/python scripts/launch_batch.py --max-parallel 2 \
  explore dsh ecommerce_demo web-nograph
```

支持的 Agent：`dsh`（DeepSeek V4.1f）、`kimi`（K3）、`zcode`（GLM-5.3）、`codex`。
每个 Agent 独立配置 MCP 与 Skill，并发运行不互相干扰。

### 批量启动评测

```bash
vendor/python/bin/python scripts/launch_batch.py --max-parallel 1 \
  eval <target> <handoff_id> <checklist>
```

### 全指标套件

```bash
# 一条龙：探索 → 复现 → 评测 → 全指标 → results.csv
BBB_SUITE_GEN_MODEL="Opus 4.8" BBB_SUITE_EVAL_MODEL="gpt-6-astra" \
  bash scripts/run_android_suite.sh <agent> [targets...]

# 单指标
vendor/python/bin/python scripts/compute_metrics.py \
  --platform android --sid <sid> --app <app> --handoff <handoff> \
  --eval-report <path> --json metrics.json
```

## Reference targets

**Web 数据集（28 个 live 目标）**：语雀、YouTube、淘宝、知乎、小红书、微博、
豆瓣、大众点评、携程、Reddit、Quora、Notion、Trello、Todoist、Airtable、Google
Calendar、Dropbox、Google Forms、Excalidraw、diagrams.net、Spotify、Desmos
（图形/几何/3D）、Google Maps、Kleki、JS Paint、Squoosh——注册于
`benchmark/orchestrator/apps.py`，配套人工登录 profile（`runs/live_targets/`）。

**Android 数据集（26 个目标）**：Google 时钟、AnkiDroid、AntennaPod、Fossify
日历/相册/绘图/计算器、cashew、feeder、Loop Habit Tracker、Librera、Organic
Maps、Snapseed、VLC、Joplin、Tasks、Material Files、Vinyl、MJ PDF、节拍器、
Markor、Nonogram、Minesweeper、2048、Futoshiki、RTTT——注册于
`runs/android_targets/targets.json`。

每个目标配套 `review_specs/<app_id>.json` 人工功能清单（含 persistence 标志与
exclusions 排除边界），新目标接入见 [adding reference app](docs/adding_reference_app.md)。

## Session 工件

每次探索写入 `runs/<session_id>/`：

```text
session.json
frames/frame_*.png
actions.jsonl
observations.jsonl
discovery.jsonl                 # 仅托管条件
functional_topology.json        # 仅托管条件
functional_topology.md
coverage_report.json
session_summary.json
mcp_reproduction_state.json     # 复现阶段断点续传状态
crash_report.json               # 仅失败时
```

复现阶段产出 `app_output/<handoff_id>/`（Android）或 `website_output/<handoff_id>/`
（Web），包含完整工程源码、签名 APK（Android）或静态网站（Web）、审查证据与
`review/review_summary.json`。

## 目录

```text
benchmark/                Controller、Session、Runtime、Recorder、Topology
benchmark/android/        Android 目标注册、工具链发现与 Emulator Runtime
agent_sdk/                pixels-only Python SDK
agents/                   MCP Server、安装器和探索 Skill（两平台 × 两条件）
agents/app_review/        Android 四档评测 judge
agents/web_review/        Web 四档评测 judge
self_explorer/            归档：自建探索工具
sample_apps/              确定性 Reference App
docker/                   正式隔离部署（含 reference-raw、live-controller）
docker-compose*.yml       Docker Compose 部署配置
schemas/                  Topology/State/Feature/Interaction Schema
docs/                     API、安全、架构、指标定义、接入说明
scripts/                  bootstrap、探索/评测启动、指标计算、覆盖判定
scripts/run_android_suite.sh       Android 全套件 runner
scripts/coverage_judge_android.sh  codex 版被动覆盖判定
scripts/compute_metrics.py          A-F 工件指标计算（--platform web|android）
scripts/compute_visual_similarity.py CLIP-L/14 视觉相似度
scripts/extract_tokens.py           kimi/zcode token 提取
scripts/full_metrics_android.sh     一条龙指标 runner
scripts/launch_batch.py             并发探索/评测启动器
tests/                    核心行为、安全、MCP、reset、topology 测试
runs/                     运行工件（Git 忽略）
runs/android_targets/     Android APK 与目标注册
reproduction/materials/  公共只读素材、SQLite 和后端模板
app_reproduction/         Compose 脚手架、移动素材、离线构建与 APK 复测
app_evaluation/           人工清单加载校验与四档评级（两平台共享）
app_output/               Android 工程、复测证据和最终 APK
web_evaluation/           Web 四档评测会话
website_output/          Agent 生成网页
review_specs/             人工功能要求清单（web 28 / android 26）
```

## 功能排除边界（exclusions）

每个数据集目标配套人工维护的排除清单——多人协作、账户/支付、实时数据、AI 生成、
外部服务等刻意不复现的功能面（`review_specs/<app_id>.json` 的 `exclusions` 键），
由 `Checklist` 严格校验。探索侧 Agent 不在排除面上花预算，评测侧排除面不参与四档
判定，边界在探索与评测读同一份文件，口径不会分叉。

## 验证

```bash
vendor/python/bin/python -m pytest tests/ -q
vendor/python/bin/python sample_apps/ecommerce_demo/smoke_test.py
vendor/python/bin/python scripts/e2e_smoke.py
```

## 当前范围

平台负责"受控探索 → 条件对应的安全交接 → 隔离复现工作区 → 人工清单四档评测 →
标准化指标计算"的完整闭环。证据轨迹与功能拓扑只属于托管探索条件；评测由独立 LLM
judge 做 Semantic judgment，代码只做薄护栏（证据引用真实截图、四档封闭、不漏项、
报告 schema 固定）。Android 侧与 Web 侧护栏完全对齐，报告可横向比较。

暂不包含：

- 专用 VLM 调用循环（Agent 规划由外部 MCP Agent 框架负责）。
- Dashboard、视频、历史 Session 浏览和 deterministic replay。
- 原站/复现站的像素级自动差分（评的是功能是否实现）。
- 网页条件不规定固定前端或后端技术栈；Android 复现固定使用 Kotlin + Compose。
