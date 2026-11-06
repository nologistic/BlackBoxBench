# Recreate Anything

**一个只靠像素和坐标输入、就能让 AI Agent 复刻未知 App 的黑盒基准测试平台。**

Agent 看不到 DOM、看不到控件树、看不到日志，也拿不到 APK——它只有屏幕截图，和一只"人手"。
我们用它来回答一个问题：**当一个 Agent 只能像人一样看和操作时，它能把一个陌生的 App 复刻到什么程度？**

<p>
  <img alt="platform" src="https://img.shields.io/badge/platform-Web%20%7C%20Android-1f6feb">
  <img alt="python" src="https://img.shields.io/badge/python-3.12-3776ab">
  <img alt="judge" src="https://img.shields.io/badge/judge-gpt--6--astra-6e40c9">
  <img alt="metrics" src="https://img.shields.io/badge/metrics-13%20standardized-2ea043">
  <img alt="conditions" src="https://img.shields.io/badge/conditions-managed%20%2F%20nograph-d29922">
  <img alt="status" src="https://img.shields.io/badge/status-active%20development-orange">
</p>

[English](README.md) · **简体中文**

---

## 目录

- [它测什么](#它测什么)
- [端到端流程](#端到端流程)
- [探索条件矩阵](#探索条件矩阵)
- [评测体系](#评测体系)
- [13 项标准化指标](#13-项标准化指标)
- [最小架构](#最小架构)
- [快速开始](#快速开始)
- [仓库结构](#仓库结构)
- [不可破坏的边界](#不可破坏的边界)
- [文档索引](#文档索引)

---

## 它测什么

传统 GUI Agent 基准往往允许读取控件树、DOM、Accessibility 树，或直接调用应用 API。
这让任务变简单了，也让结论变得不可比——你无法区分"模型理解了界面"和"模型读到了结构"。

Recreate Anything 把信息通道压到最小：

```text
Unknown App ──pixels──▶  Agent  ──human-like HID──▶  Unknown App
                            │
                            ├── evidence ──▶ Functional Topology（托管条件）
                            │                     │
                            ▼                     ▼
                    Reproduction Workspace ──▶  LLM Judge  ──▶  Metrics
```

- **输入**：一张 PNG 截图（或一帧画面）
- **输出**：坐标级点击 / 拖拽 / 触控 / 键盘输入
- **禁止**：DOM、Accessibility、脚本执行、selector、URL、网络、Cookie、存储、ADB、日志、反编译
- **产出**：可安装的 APK，或可运行的静态网站；外加一份可被判分的功能拓扑

## 端到端流程

| 阶段 | 发生什么 | 产物 |
|---|---|---|
| **1. 探索** | Agent 在受控运行时里操作目标应用，受动作预算与边界约束 | 屏幕帧 + 动作轨迹（JSONL） |
| **2. 记录**（仅托管条件） | Agent 用 discovery 工具登记状态、功能、数据、边、假设 | 证据校验过的 Functional Topology |
| **3. 复现** | finalize 后自动进入隔离工作区，按拓扑重写应用 | Android：Kotlin + Compose 工程与签名 APK<br>Web：静态网站 |
| **4. 自检** | 在另一台独立设备/浏览器里像素复测，多轮修正 | `review_summary.json`（轮次、决策） |
| **5. 评测** | 独立 LLM judge 按人工功能清单逐项操作复现产物 | `evaluation_report.json`（四档评分） |
| **6. 度量** | 从工件离线计算 13 项指标 | 跨模型、跨条件的可比表格 |

关键点：**复现工作区与探索环境物理隔离**（`network=none`、只读根文件系统、capability drop），
输入只有三份只读挂载：

```text
托管 finalized topology  → /input/functional_topology.json   (read-only)
公共虚构素材              → /materials                        (read-only)
过滤后的探索工件          → /exploration                      (read-only)
复现输出                  → /workspace                        (read-write)
```

## 探索条件矩阵

两条链路 × 两种条件，共四种组合。**唯一被测变量是"有没有结构化记录工具"**。

| 平台 | 条件 | Skill | Agent 获得什么 | 记录要求 |
|---|---|---|---|---|
| Web | 基线 | `blackbox-explorer` | PNG、坐标输入、discovery MCP | 按指定 DAG 记录 |
| Web | 消融 | `web-nograph` | PNG、坐标输入（无 discovery 工具） | 自由记录或完全不记录 |
| Android | 基线 | `android-blackbox-explorer` | PNG、坐标触控、discovery MCP | 按指定 DAG 记录 |
| Android | 消融 | `baseline-nograph` | PNG、坐标触控（无 discovery 工具） | 自由记录或完全不记录 |

消融条件 = 基线 **减去 7 个 discovery 记录工具**：

```text
record_state · record_feature · record_data · record_edge
record_hypothesis · resolve · revise
```

探索纪律（预算、边界、finalize 流程）与复现链路完全一致，因此"是否有结构化记录"
成为干净的单一变量，可量化记录工具对探索质量与复现保真度的影响。

自建工具条件（Agent 完全自主决定工具与工作流）已归档于 `self_explorer/`，
不再作为活跃实验入口，代码与文档保留用于复现历史实验。

## 评测体系

评测由独立 LLM judge（`gpt-6-astra`，`xhigh` 推理）执行：它拿到人工功能清单，
像人一样操作复现产物，逐项打分。

| 档位 | 权重 | 含义 |
|---|:---:|---|
| `full` | 1.00 | 功能完整实现，含状态持久化 |
| `partial` | 0.50 | 核心可用但不完整 |
| `placeholder` | 0.25 | 界面存在但无实际功能 |
| `broken` | 0.00 | 缺失或报错 |

加权得分 **WFS = Σ(权重 × 档位计数) / 判据总数**，可直接跨目标聚合。

清单来自人工撰写，覆盖真实用户的典型使用路径：

```text
review_specs/<app_id>.json
  ├─ checklist_id / platform
  ├─ features[]   每条判据：id / name / steps / expected
  │                        / persistence / multi_user
  └─ exclusions[] 明确排除项：feature / treatment / reason / covered_by
```

当前随仓库提供 **57 份清单**：Web 28 份、Android 29 份。

## 13 项标准化指标

全部指标从已产出的工件离线计算，不需要额外 Agent 交互。

| 族 | 指标 | 数据来源 |
|---|---|---|
| **A 复现保真** | 四档分布 · WFS · persistence-WFS | `evaluation_report.json` |
| **B 探索质量** | 覆盖率 · 有效动作率 · 无效动作率 · 探索视野 | `actions.jsonl` + 覆盖率判定 |
| **C 视觉相似** | CLIP-L/14 覆盖折扣相似度 | 探索帧 × 评测帧 |
| **D 复现过程** | 修正轮数 · 幻觉率 | `review_summary.json` |
| **E 鲁棒性** | 平台归因故障数 | `crash_report.json` |
| **F 效率** | 分阶段 token · tokens-per-fidelity | CLI 用量日志 |

- **覆盖率**：用视觉 judge 被动判定探索关键帧与人工清单的交集。
- **视觉相似度**：对每条清单任务计算 CLIP 余弦相似度（探索帧 vs 复现帧），未覆盖或未复现计零。
- **幻觉率**：复现产物声称实现了、但 judge 实测无法触达的判据比例。

## 最小架构

```text
Agent framework（任意 MCP 客户端：kimi / zcode / dsh / codex / 自研）
  └─ MCP or Python SDK
       └─ Controller（benchmark.server）
            ├─ Session  预算 / 生命周期 / 动作合法性校验
            ├─ Runtime  截图进、HID 出
            │    ├─ LocalChromiumRuntime      本地开发
            │    ├─ LiveChromiumRuntime       真实网站
            │    ├─ DockerX11Runtime          正式网页评测（隔离）
            │    └─ AndroidEmulatorRuntime    Android 模拟器
            ├─ Recorder  帧 + JSONL 轨迹
            └─ Topology  证据校验后的 discoveries
                 └─ Reference App / Android APK
```

复现自检的收敛由双闸保障：

| 闸门 | 环境变量 | 默认值 | 含义 |
|---|---|:---:|---|
| 观察预算 | `BBB_REVIEW_BUDGET` | 150 | 单次自检允许的截图观察总数 |
| 修订上限 | `BBB_REPRO_REVISIONS` | 6 | 自审发现 → 修复的最大循环次数 |

## 快速开始

```bash
# 1. 环境（Python 3.12）
python3 -m venv vendor/python
vendor/python/bin/python3 -m pip install -r requirements.txt

# 2. 引导（拉取模拟器、Chrome、素材等依赖，可重复执行）
vendor/python/bin/python3 scripts/bootstrap.py

# 3. 启动控制器
vendor/python/bin/python3 -m benchmark.server --port 7800

# 4. 跑一次探索（以 Android 消融条件为例）
vendor/python/bin/python3 scripts/launch_batch.py \
    explore kimi nonogram nograph

# 5. 评测一次复现产物
vendor/python/bin/python3 scripts/launch_batch.py \
    eval nonogram sess_20260101_000000_abcdef-cdef01 nonogram.json

# 6. 计算指标
vendor/python/bin/python3 scripts/compute_metrics.py --runs runs
```

Android 目标需要先注册（本仓库随包提供 25 个目标的元数据于
`android_targets/registry.json`，APK 位于 `android_apks/`）：

```bash
vendor/python/bin/python3 scripts/android_launch_preflight.py --app ankidroid
vendor/python/bin/python3 scripts/launch_batch.py explore codex ankidroid nograph
```

常用辅助：

```bash
vendor/python/bin/python3 scripts/bench_status.py          # 当前批次状态
vendor/python/bin/python3 scripts/check_batch_health.py    # 健康检查
docker compose up -d                                       # 隔离运行时
```

## 仓库结构

```text
agents/              各条件的 agent 入口（MCP server / skill 安装器）
  android_baseline/    Android 基线（discovery 工具齐全）
  baseline_nograph/    Android 消融
  web_nograph/         Web 消融
  app_review/          复现产物的评审入口
  web_review/          Web 复现产物的评审入口
  cli_explorer/        自建工具条件（归档）
benchmark/           控制器：会话、运行时、录制、拓扑、编排
app_evaluation/      四档评分与清单加载
app_reproduction/    Android 复现工作区与构建
web_evaluation/      Web 复现产物评测
reproduction/        Web 复现工作区
docker/             隔离运行时与镜像定义
scripts/            引导、启动、诊断、指标、导出等实用脚本
review_specs/       人工功能清单（57 份）与排除项
android_apks/       目标 APK 与校验和
android_seeds/      目标应用的种子素材
android_targets/    目标注册表（25 个目标）
sample_apps/        随仓库的演示应用（含 ground truth）
schemas/            拓扑、清单、评测报告的 schema
docs/               架构、协议、平台、指标、安全模型等文档
tests/              单元测试与契约测试
```

## 不可破坏的边界

- Agent 只能经 `/agent/{sid}/...` 使用截图、预算、坐标输入与 discovery 工具。
- 禁止 DOM、Accessibility、脚本执行、selector、URL、网络、Cookie 与存储读取。
- 本地 CDP 只允许 Page 导航/截图、Input、固定 viewport 与 `Browser.close`。
- 动作回执不包含"点到了什么"这类语义结果。
- `confirmed` 的 Feature / Edge 必须引用当前 session 的真实 step / frame。
- `sample_apps/*/ground_truth.json` 不通过 HTTP 暴露，也不进入 Agent 容器。
- Android 侧 Agent 不接触 APK 文件、ADB、控件树、日志或反编译结果。

详见 [docs/security_model.md](docs/security_model.md)。

## 文档索引

| 文档 | 内容 |
|---|---|
| [architecture.md](docs/architecture.md) | 控制器、运行时、会话与工件模型 |
| [api_contract.md](docs/api_contract.md) | Agent 侧 HTTP / MCP 接口契约 |
| [topology_schema.md](docs/topology_schema.md) | Functional Topology 的数据结构 |
| [evaluation_contract.md](docs/evaluation_contract.md) | 判分口径与四档定义 |
| [benchmark_metrics.md](docs/benchmark_metrics.md) | 13 项指标的计算方式 |
| [android_platform.md](docs/android_platform.md) | 模拟器、锁、快照与网络策略 |
| [security_model.md](docs/security_model.md) | 隔离边界与攻击面 |
| [running_agent.md](docs/running_agent.md) | 接入自有 Agent |
| [adding_reference_app.md](docs/adding_reference_app.md) | 新增目标应用 |
| [dependency_requirements.md](docs/dependency_requirements.md) | 工具链与镜像依赖 |

---

<p align="center">
  <sub>Recreate Anything · 只看像素，只动手，复刻一切</sub>
</p>
