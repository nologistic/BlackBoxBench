# BlackBoxBench 系统架构

> 版本: 1.0 · 状态: Phase I 基础设施

## 1. 研究目标

本系统测量的是:

```text
Unknown Application
        ↓
Visual Exploration        (Agent 只能看到像素)
        ↓
Behavior Discovery        (Agent 只能发送人类式 GUI 输入)
        ↓
Functional Modeling
        ↓
Functional Topology Graph
```

它**不是** Screenshot-to-Code、不是 GUI 任务完成度评测、也不是浏览器自动化框架。
它测量 Agent 能否像第一次使用陌生软件的人类一样,通过主动操作、观察反馈、
尝试边界条件、分析状态变化,逆向理解一个黑盒软件系统。

## 2. 最高优先级安全边界: Pixels-Only

```text
Observation_t = Pixels_t + CursorPosition_t
```

Agent **永远不能**获得: 源代码、HTML、DOM、Accessibility Tree、CSS/JS 源、
DevTools/CDP、Playwright locator、Selenium selector、API 文档、网络请求、
WebSocket 消息、localStorage/sessionStorage/IndexedDB、Cookies、数据库、
应用日志、文件系统、包元数据、Git 仓库、sourcemap、浏览器 console、
内部事件监听、隐藏测试用例、ground-truth graph。

Agent 能获得的只有:

- 屏幕截图 (PNG, 含渲染进去的光标)
- 光标坐标
- OS 输入级别的事件回执 (`accepted: true/false`, 不含任何语义结果)
- 探索预算余量

**工程上如何守住这条边界:**

1. **Runtime 驱动收口**: 所有与浏览器的底层通信(CDP)只存在于
   `benchmark/runtime/` 内部。该模块只暴露
   `screenshot() / send_mouse() / send_key() / navigate() / reset()`,
   **不存在** `evaluate()`、`get_dom()`、`locator()` 之类的方法——不是"不调用",
   是代码里根本没有这个能力面。
2. **Agent 通道收口**: Agent 只能访问 Controller 的 `/agent/{sid}/...` 路由。
   这些路由的响应 schema 是白名单式的 (见 §7 与 `docs/api_contract.md`),
   响应中不存在 URL、元素、语义标注字段。
3. **网络收口**: 浏览器通过 `--host-resolver-rules` 被限制为只能解析
   `reference-app.internal`; App 进程只接受带内部网关头的请求;
   Docker 部署中 reference 容器位于 `internal: true` 网络,无外网路由。
4. **工件收口**: `ground_truth.json` 不以任何形式通过 HTTP 暴露;
   Agent 容器/进程内不存在该文件。

## 3. 逻辑分层

```text
┌──────────────────────────────────────────────┐
│             Benchmark Orchestrator           │
│  session 生命周期 / 预算 / reset / 日志 / 指标  │
└───────────────┬──────────────────────────────┘
                │  controlled interface (HTTP)
┌───────────────▼──────────────────────────────┐
│              Agent Controller                │
│  observe() click() type() scroll() key()     │
│  drag() wait() + discovery 工具集             │
└───────────────┬──────────────────────────────┘
                │  pixels + HID events only
┌───────────────▼──────────────────────────────┐
│          Reference App Environment           │
│  Chromium (kiosk, headless/Xvfb)             │
│        │                                     │
│        ▼                                     │
│  Reference App  (源码/DB/内部 API 对 Agent 不可达)│
└──────────────────────────────────────────────┘

┌──────────────────────────────────────────────┐
│           Observation Recorder               │
│  frames/ actions.jsonl observations.jsonl    │
│  cursor / click markers / timing / 状态转换    │
│  video (overlay) / 视觉差分 / settle 检测      │
└──────────────────────────────────────────────┘

┌──────────────────────────────────────────────┐
│        Functional Discovery Engine           │
│  states / features / data / edges            │
│  hypotheses / evidence 校验 / topology 聚合    │
└──────────────────────────────────────────────┘
```

## 4. 部署拓扑

### 4.1 容器部署 (有 Docker 的机器,完整隔离)

```text
docker-compose:
  reference-container   ← Chromium+Xvfb+xdotool+scrot+App+RuntimeRPC
                          网络: refnet (internal: true, 无外网)
  controller-container  ← FastAPI Orchestrator
                          网络: refnet + agentnet + 对宿主暴露 Dashboard 端口
  agent-container       ← Agent 进程 (demo 或用户 Agent)
                          网络: agentnet only (只能到 controller)
```

三个安全域之间只允许严格定义的通信:

- agent → controller: HTTP `/agent/{sid}/...`
- controller → reference: RuntimeRPC (screenshot/input/reset)
- agent ✗→ reference: 网络上不可达

### 4.2 本机开发模式 (无 Docker 的机器,如本仓库验证环境)

同一套代码,`LocalChromiumRuntime` 替换 `DockerX11Runtime`:

- App 以子进程运行,绑定 `127.0.0.1:<random>`,**只接受带 `X-BBB-Gateway`
  内部头的请求** (由 Controller 内置网关注入); 直接访问返回 404。
- 浏览器为项目内 vendor 的 Chrome for Testing,headless,CDP 端口随机且
  仅绑定 loopback,**只用于截图与输入注入**。
- 浏览器经 `--host-resolver-rules` 锁定: 只能解析 `reference-app.internal`,
  其余全部 `~NOTFOUND` —— 即使 Agent 尝试在页面里跳外网也会失败。
- 所有进程外依赖 (Python / Chromium / ffmpeg) 均 vendor 在项目目录内,
  `scripts/bootstrap.py` 可在任意 Windows/Linux/macOS 机器一键重建。

两种模式对 Agent 暴露的接口与黑盒保证**完全一致**; 差异仅在于 OS 级
隔离强度 (本机模式防意外泄漏,容器模式防主动对抗),见
`docs/security_model.md`。

## 5. 数据流

### 5.1 一次 action 的完整路径

```text
Agent: POST /agent/{sid}/action {"type":"click","x":812,"y":431}
  → Orchestrator: 预算检查 + 坐标校验
  → Recorder: 记录 action, 捕获 before-frame (若无)
  → Runtime: CDP Input.dispatchMouseEvent (move/press/release)
  → Controller: 虚拟光标位置更新 (812,431)
  → Settle 检测: 轮询截图直至帧差 < 阈值或超时
  → Recorder: 保存 after-frame (渲染光标叠加层), 写 observations.jsonl
  → 内部遥测: 视觉差分 (phash/均方差), loop 检测 (不进 agent 响应)
  → Agent 收到: {"accepted": true, "frame_id": 128}
```

Agent 随后 `observe()` 拿第 128 帧,自行判断点击造成了什么。

### 5.2 Discovery → Topology

Agent 在探索中随时调用结构化记忆工具 (`record_state/feature/data/edge/
hypothesis`)。Controller 做三件事:

1. **Schema 校验** (pydantic): 类型、confidence ∈ [0,1]、edge 类型枚举。
2. **Evidence 校验**: 引用的 frame_id/step 必须真实存在于本会话
   (Evidence > Eloquence,机械强制)。
3. **泄漏检查**: 文本字段命中实现细节正则 (`/api/`、`.tsx`、`localhost`、
   `SELECT `、`React`…) 一律 422 拒绝。

通过后写入 `discovery.jsonl` (append-only) 并增量更新 `topology.json`。
支持 create / update / merge / delete (Agent 早期理解可能错,必须可修正)。
`finalize` 时生成 `functional_topology.json` + `.md` + `coverage_report.json`
+ `session_summary.json`。

## 6. 坐标系统

- 统一屏幕像素坐标, 原点左上, x→右, y→下。
- 默认 viewport **1440×900**, `devicePixelRatio=1`, zoom=100%, 无 browser chrome
  (headless / kiosk), 全部写入 `session.json`。
- action 坐标与截图像素严格 1:1 (CDP 输入使用 CSS 像素, dpr=1 时等于设备像素)。

## 7. observe() 响应 (白名单 schema)

```json
{
  "frame_id": 127,
  "timestamp": "2026-08-18T12:00:00.000Z",
  "width": 1440,
  "height": 900,
  "screenshot_png_b64": "...",
  "cursor": {"x": 812, "y": 431},
  "budget": {"actions_remaining": 372, "seconds_remaining": 1412,
             "observations_remaining": 900}
}
```

不存在 `url` / `elements` / `dom` / `html` / `network` 等任何语义字段。
action 回执同理: `{"accepted": true}` —— "OS 鼠标事件已发送",仅此而已。

## 8. Session 工件布局

```text
runs/{session_id}/
  session.json            # viewport/dpr/seed/app/budget/状态/计时
  actions.jsonl           # {step, ts, action, accepted, error}
  observations.jsonl      # {step, frame_id, ts, path, cursor, diff, settle_ms}
  discovery.jsonl         # append-only discovery 操作日志
  topology.json           # 实时增量图
  hypotheses.json
  frames/frame_*.png      # 含光标叠加
  video/session.mp4       # 叠加光标/点击涟漪/步号/动作
  functional_topology.json|.md   # finalize 产物
  coverage_report.json
  session_summary.json
  metrics.json
  replay_{n}/             # deterministic replay 产物
  crash_report.json       # 异常时
```

## 9. 关键设计决策

| 决策 | 理由 |
|---|---|
| CDP 仅用于截图+输入,且封装在 runtime 内部 | 规范允许截图管线用 CDP;输入经 `Input.dispatch*`,等价于人类 HID;能力面物理上不含 DOM |
| 光标由 Controller 渲染进帧 | headless 截图不带光标;渲染后帧自包含,replay/视频/dashboard 统一 |
| Server-rendered Sample App | 行为全部经由导航/表单可见,不依赖 JS 内部状态,便于黑盒验证 |
| filesystem + JSONL 存储 | Phase I 无需数据库;工件可直接审计 |
| 轮询式 Dashboard (700ms) | 零额外依赖,稳定;WS 可作为后续优化 |
| 禁止 URL 作为 State | State 由 Agent 以视觉证据定义;同页多态 (空购物车/非空) 必须可分 |
| Evidence 机械校验 | Graph 可审计的根基;无证据的声明写入即拒绝 |

## 10. 未来扩展挂钩

- `runtime/base.py` 的 `Runtime` 抽象 → Android/Electron/Desktop 驱动
- `ReferenceBehaviorRunner` / `GeneratedBehaviorRunner` / `DifferentialEvaluator`
  接口预留 (Phase II 差分评测)
- touch/swipe/pinch 加入 action 枚举 (schema 已预留)
- 多 seed (`seed_001...`) → robustness 评测
- 输入通道可替换为 X11/xdotool (docker 模式已实现该路径)
