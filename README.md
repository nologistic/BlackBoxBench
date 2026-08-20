# BlackBoxBench

**评估 AI Agent 黑盒理解 App 能力的虚拟交互环境与功能拓扑发现系统。**

一个多模态 AI Agent 在**完全无法访问**目标 App 的源码、DOM、Accessibility Tree、
内部 API、数据库、网络请求、文件系统的前提下,仅通过与人类一致的**视觉 GUI 交互**
(截图 + 鼠标/键盘),自主探索一个运行中的 App,发现其功能、状态依赖与行为关系,
产出可审计的 **Functional Topology Graph**(功能拓扑图)。

```text
Unknown App ──pixels──▶ Vision Agent ──HID actions──▶ Unknown App
                              │
                              ▼
                      Discovery Memory ──▶ Functional Topology Graph
```

> 这是 Benchmark 基础设施(Phase I)。后续阶段将让 Agent 根据自己发现的拓扑图
> 从零重建 App,并做差分功能评测。

## 核心约束(不可 trade-off)

- **Pixels-only**: `observe()` 只返回 PNG 截图 + 光标坐标 + 预算。永远不含
  DOM/selector/URL/元素语义(见 `docs/security_model.md`)。
- **人类式输入**: click/type/scroll/key/drag 均为坐标级 OS 输入事件,
  回执只有 `accepted`,没有"点到了什么"。
- **Evidence > Eloquence**: 每个 State/Feature/Edge 必须引用真实存在的
  frame/step,Controller 机械校验,无证据即拒绝。
- **可审计**: 完整 trajectory(frames + actions.jsonl + observations.jsonl)、
  visual + deterministic replay、session 视频。
- **可重复**: seeded 数据、reset 到 S0、固定 viewport(1440×900, dpr=1)。

## 系统架构

```text
┌──────────────────────────────┐        ┌────────────────────────┐
│  Agent (SDK / VLM / demo)    │        │  Dashboard (operator)  │
│  observe/click/type/record_* │        │  live view / topology  │
└──────────┬───────────────────┘        └───────────┬────────────┘
           │ /agent/{sid}/... (pixels+HID only)     │ /api/...
┌──────────▼───────────────────────────────────────▼────────────┐
│                  Controller (FastAPI)                          │
│  orchestrator · recorder · topology store · replay · metrics   │
└──────────┬─────────────────────────────────────────────────────┘
           │ Runtime interface (base.Runtime)
┌──────────▼───────────────┐   ┌───────────────────────────────┐
│ LocalChromiumRuntime     │   │ DockerX11Runtime (容器模式)     │
│ headless Chrome, CDP:    │   │ Xvfb + xdotool + scrot,       │
│ 仅截图+输入(白名单)       │   │ RPC only, no CDP at all       │
└──────────┬───────────────┘   └───────────────┬───────────────┘
           ▼                                   ▼
   Reference App (网关秘钥头保护, host-resolver 锁定, 无系统代理)
```

详见 `docs/architecture.md`。

## Quick Start

### 一键环境(推荐)

```bash
python scripts/bootstrap.py    # 任意 Python 3.10+;下载便携 Python + Chrome, 装依赖
```

脚本把一切都装进项目内: `vendor/python/`、`vendor/chromium/`,不依赖系统 Python/浏览器。

### 启动 Controller(含 Dashboard)

```bash
vendor/python/python.exe -m benchmark.server --port 7800      # Windows
vendor/python/bin/python3  -m benchmark.server --port 7800    # Linux/macOS
```

打开 Dashboard: <http://127.0.0.1:7800/>

### 跑 Demo 探索 Agent

```bash
vendor/python/python.exe -m demo_agent.run_demo --controller http://127.0.0.1:7800
```

### 跑 Kimi VLM 探索 Agent(真实模型)

```bash
$env:BBB_VLM_API_KEY="sk-..."     # Kimi Code Console 的 API Key
vendor/python/python.exe -m agents.kimi_explorer.run
```

模型逐步看截图、自主决策动作、实时把思考流式展示在 Dashboard。
细节见 `agents/kimi_explorer/README.md`。

### 在真实 Agent 框架中评测(推荐)

通过 MCP 工具服务器把 GUI 工具注入 Kimi Code / Claude Code / Codex / OpenCode,
由框架自己的规划循环驱动探索。三种用法:

```bash
# 0. 一次性安装(注册用户级 MCP + 安装探索 skill)
vendor/python/python.exe -m agents.cli_explorer.install

# A. 零命令模式: 新开 kimi 对话,输入
#    /skill:blackbox-explorer                        # 默认目标(install --app 设定)
#    /skill:blackbox-explorer douyin_web             # 指定已注册目标
#    /skill:blackbox-explorer https://example.com    # 任意网址(临时目标)
#    (Controller/会话由 MCP 服务器自举,对话结束自动 finalize)

# B. 手动接管(attach 监督,指定会话/参数)
vendor/python/python.exe -m agents.cli_explorer.attach --cli kimi

# C. 无头批量
vendor/python/python.exe -m agents.cli_explorer.run --cli kimi      # 或 claude/codex/opencode
#    可加 --app-id douyin_web 或 --url https://example.com
```

细节与各框架的黑盒约束实现见 `agents/cli_explorer/README.md`。

结束后 artifacts 在 `runs/<session_id>/`:

```text
functional_topology.json / .md   # 最终功能拓扑图
frames/  actions.jsonl  observations.jsonl  discovery.jsonl
topology.json  hypotheses.json  metrics.json  session_summary.json
video/session.mp4                # 叠加光标/点击标记/步号
```

**实时观察**: Dashboard 的 Live Observer 视图实时显示 Agent 画面
(含光标与点击涟漪)、动作流、预算、Discovery 计数;Agent 的思考过程
显示在其 CLI 自己的运行界面中(固定为 CLI 内展示方式)。

### Docker 部署(完整隔离,有 Docker 的机器)

```bash
docker compose up --build
```

三容器: `reference`(internal 网络,无外网)、`controller`、`agent`(仅可达 controller)。
见 `docs/architecture.md` §4 与 `docker/`。**注意**: 本仓库开发机无 Docker,
镜像未实机构建,首次使用请先读 `docs/adding_reference_app.md` §9 checklist。

## Agent SDK

```python
from blackbox_bench_sdk import Environment

env = Environment("http://127.0.0.1:7800", session_id="sess_...")
obs = env.observe()                    # PNG bytes + cursor + budget
obs.screenshot_png                     # 喂给你的 VLM
r = env.click(812, 431)                # ActionResult(accepted, frame_id, step)
env.type_text("camera")
env.key_press("Enter")
env.scroll(0, 600)

fid = env.record_feature(              # 结构化发现(带 evidence 校验)
    name="Search products",
    behavior={"postconditions": ["列表只剩匹配商品"]},
    evidence=[{"step": r.step, "before_frame": 10,
               "action": "click(984,32)", "after_frame": r.frame_id}],
    confidence=0.9)
env.record_edge(fid, "state_results", "TRANSITIONS_TO", evidence=[...])
env.finalize()
```

SDK 里**不存在** `find_element()/get_dom()/get_url()/get_html()/get_network()`。
写 Agent 的完整指南: `docs/running_agent.md`。

## 测试

```bash
vendor/python/python.exe -m pytest tests/ -q
```

覆盖: observe/action 白名单、SDK 能力面、直连后端 404、ground truth 不暴露、
泄漏正则拦截、evidence 校验、预算强制、reset 回 S0、deterministic replay、
topology 修订/合并/删除。

## 目录

```text
benchmark/      controller(orchestrator/runtime/recorder/topology/replay)
agent_sdk/      blackbox_bench_sdk (pixels-only)
dashboard/      静态 SPA(live/topology/replay 三视图)+ mock_server.py
sample_apps/    ecommerce_demo (Nimbus Market) + ground_truth.json(benchmark-private)
runs/live_targets/  live target 运营状态(douyin_web 的登录 profile,敏感勿外传)
schemas/        JSON Schema: interaction/feature/state/topology
docs/           architecture · security_model · benchmark_protocol · api_contract
                topology_schema · adding_reference_app · running_agent
docker/         Dockerfile×3 · runtime_rpc_server · entrypoint · internal_gateway
scripts/        bootstrap.py · e2e_smoke.py · live_login.py(live target 登录维护)
tests/          pytest(安全/动作/reset/determinism/topology/replay)
runs/           每会话 artifacts
```

## Known Limitations

- 本机模式(无 Docker)的隔离是进程/API 级的:防意外泄漏,不防恶意对抗;
  正式评测请用容器部署(见 `docs/security_model.md` 强度表)。
- Live target(`douyin_web`,真实网站)无 S0 reset / determinism,登录态需
  人工维护(`scripts/live_login.py --capture`);隔离降级详见
  `docs/security_model.md` §3,接入流程见 `docs/adding_reference_app.md` §10。
- Demo Agent 是**脚本化校准策略**(坐标来自 LAYOUT.md),用于验证基础设施;
  它不是视觉推理能力的演示。真实 VLM Agent 复用同一 SDK。
- headless Chrome 一次性 `--screenshot` 模式在本机挂起(未使用);CDP 路径正常。
- 视频编码依赖 imageio-ffmpeg 自带二进制;失败时 replay 仍可用 Dashboard 播放器。
- 当前仅 Web;Android/Desktop 通过 `Runtime` 抽象扩展。
