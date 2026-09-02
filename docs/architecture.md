# BlackBoxBench 最小探索架构

## 1. 目标

系统让 Agent 在受控条件下，以像素和类人输入探索未知 App。现有托管工具是条件 A，
保存证据并形成 Functional Topology；Agent 自建工具是隔离的条件 B，不预设探索工具、
记录格式、拓扑、覆盖标准或工作流。

```text
Observation_t = Screenshot_t + Cursor_t + Budget_t
Action_t      = Coordinate-level mouse/keyboard input
```

探索结束时只作一次单向交接：立即启动与探索隔离的网站复现沙盒。托管条件携带定稿
拓扑，自建条件不要求拓扑。复现执行器属于 Controller 之外的下游域；差分评测仍属于
下一阶段。

## 2. 四个核心组件

```text
Agent (MCP / SDK)
        │
        ▼
Controller ── Session / budget / API whitelist
        │
        ├── Recorder ── frames + actions/observations JSONL
        ├── Topology ── evidence validation + graph
        └── Runtime ── screenshot in / HID out
                          │
                          ▼
                    Reference App
```

### Controller 与 Session

`benchmark/server.py` 提供 Agent 白名单接口和精简 Operator API。
`benchmark/orchestrator/session.py` 串行化单个 Session 内的观察和动作，负责预算、
坐标校验、视觉 settle、reset、finalize 和失败清理。

### Runtime

`benchmark/runtime/base.py` 的能力面只有截图、导航/reset、鼠标、键盘、滚动、健康
检查和有限标签页控制。Local Runtime 的 CDP 方法白名单是安全边界，不允许增加
任何 DOM/Runtime/Accessibility/Network/Storage 方法。

### Recorder

Recorder 保存含光标的 PNG、动作 JSONL 和观察 JSONL。系统不再生成视频或 Replay；
原始帧和轨迹就是单一审计事实源。

### Topology

Topology Store 接受 State、Feature、Data、Edge、Hypothesis 和 Revision。confirmed
声明必须引用当前 Session 中存在的 frame/step，文本必须通过实现泄漏检查。

## 3. 一次动作的数据流

```text
POST /agent/{sid}/action
  → 检查 Session、预算、动作 schema 和坐标
  → 记录 before frame
  → Runtime 发送鼠标/键盘事件
  → 等待画面稳定
  → 保存含光标的 after frame
  → 写 actions.jsonl / observations.jsonl
  → 返回 accepted + frame_id + step + tabs
```

Action 回执不解释动作结果，Agent 必须通过下一张截图自行判断。

## 4. 部署模式

### Local

- App 子进程绑定随机 loopback 端口。
- Gateway Secret 保护 App，直接访问返回空 404。
- Chromium 固定 1440×900、DPR=1。
- `--no-proxy-server` 和 resolver rules 限制网络。
- CDP 仅用于允许的截图、输入和导航能力。

### Docker

正式评测使用 reference/controller/agent 三容器。Agent 只能访问 Controller；
Reference 容器无外网，且使用 Xvfb/xdotool/scrot，不开放 CDP。

### Live target

真实网站使用持久 profile、正常公网导航和头像像素预检。入口 URL 只用于指定起点，
不作为域名/跳转白名单；对搜索原实现、直接下载页面或读取语义接口的禁令由任务 brief
软约束。Live target 只允许只读探索，没有确定性 S0；reset 只代表浏览器冷启动回入口页。

## 5. Session 生命周期

```text
create → capture S0 → running → finalize/close → closed
                              └→ runtime error → failed
```

- Budget 包含 actions、observations 和 duration。
- Local reset 会删除 Session appdata、重新播种并冷启动浏览器。
- Live reset 不删除服务端状态。
- finalize 生成最终 topology 和 session summary。
- 启动或运行失败会释放 Runtime 和 Recorder。
- **失败即归还**：observe 与 action 走同一失败路径，标记 failed 时立即停止
  Runtime，交回浏览器/模拟器进程、live profile 单例、Android target lease 与
  端口预留。Controller 也会丢弃该 Session 的 runtime 句柄。任何一处遗漏都会让
  一次瞬时抖动把目标锁死到 Controller 重启。
- **瞬时故障不等于 Agent 表现**：Runtime 层对可恢复的传输故障重试（CDP 退避重试
  约 12s 并在连接级失败后重连；adb 重试前先 wait-for-device），只有持续故障才
  上报。环境噪声不应被记录成对 Agent 的观察。
- **Controller 非正常退出**：Session 只存在于内存，被强杀后磁盘上会残留
  `status: running` 的僵尸目录，它无法再被观察、关闭或恢复。Controller 启动时
  会把这类元数据改写为 failed，帧、轨迹与拓扑作为审计日志保持原样。

## 6. 工件

```text
runs/<session_id>/
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
  crash_report.json        # 仅失败时
  runtime/app.log          # 诊断日志
  runtime/browser.log
```

`runs/` 只存证据与诊断。易失运行态（浏览器 user-data-dir 数百个缓存文件、Android
AVD clone 数 GB）建在仓库之外的 scratch（`BBB_SCRATCH_DIR`，默认
`%TEMP%/blackboxbench-scratch`，见 `benchmark/scratch.py`）：它们会话结束即无价值，
放在仓库内会让每次 teardown 删掉数千个工作区文件，删除失败还会留下数 GB 垃圾。
scratch 目录名带 owner PID，因此崩溃遗留可在下次启动前自动回收，无需登记表。

测试通过独立临时 runs 目录运行，不得污染正式 `runs/`。

## 7. 扩展边界

### Self-built-tool condition

```text
Agent framework (only generic workspace MCP)
        │ write/read/argv exec
        ▼
tool_builder (bundled: network none; public URL: prompt-governed egress)
        │ discoverable device volume; no documentation/client
        ▼
reference_raw (bundled: network none; public URL: filtered egress)
```

该条件不经过 Controller，也不导入托管 Runtime、SDK 或 discovery。Agent 初始只收到
明确起始 URL 和任务限制；该 URL 不限制后续重定向或跨域导航。workspace 为空，不含
协议、示例、starter code、辅助库或输出模板。可信侧只
维护一个像素设备文件和一个 Linux input-event FIFO，机械执行受限键鼠，并执行时长、
动作、帧预算和最小动作间隔。系统明确目标浏览器已经启动并给出浏览器 I/O 根位置，
但设备格式、输入协议和客户端不写进 Skill、MCP 返回或 Agent 镜像；系统也不告诉
Agent 应构建什么工具或采用什么探索过程。

宿主侧探索工件默认写入项目内 `runs/self_built/self_*/workspace/`；这只是结果归档位置，
不是隔离边界。Agent 容器仍仅挂载当前 workspace，严格实验还必须使用客户端工具
allowlist 或客户端容器化。最终网页写入独立的 `website_output/<handoff_id>/`。

强隔离由三个边界共同形成：Agent image 使用窄 build context；两个容器只共享设备
volume。内置目标的两个容器都断网；公网 URL 目标让可信 reference 经逐连接验证 public
IP 的 loopback proxy 出网，同时给 tool_builder 普通出网，其网络用途由 Skill 软约束，
不以入口 URL 建立网络白名单。
外部 Agent 客户端还必须使用工具 allowlist，仅保留通用 workspace MCP。具体见
`self_explorer/README.md`。

登录态按运行平台分离：宿主托管浏览器使用普通 `profile_golden`；Linux 自建容器使用
由 `scripts/self_live_login.py` 人工建立的 `docker_profile_golden`。操作员登录阶段是
独立 Compose 项目，不启动 Agent，只临时在 host loopback 提供像素/坐标界面。正式
自建探索把 Docker golden Profile 只读挂载给 `reference_raw`，启动时复制到 tmpfs；
`tool_builder`、workspace 和下游复现容器不挂载任何 Profile。

### Reproduction handoff layer

`reproduction/materials/` 提供公共只读的虚构内容、媒体、SQLite 和后端模板。
复现 Agent 读取公共素材与该条件允许交接的输入，只在本次
`website_output/<handoff_id>/` 中实现网页。托管模式提供 finalized topology，自建模式
没有必需 topology。
需要修改数据库或后端时先复制到输出目录，避免污染公共素材。

交接容器无网络、只读根文件系统、drop all capabilities，并有 CPU、内存和
进程数上限。它挂载公共素材、条件对应的白名单探索工件、本次可写输出，以及仅在
托管模式下存在的定稿 topology；不挂载
仓库、Reference App 源码、ground truth、live profile 或浏览器语义接口。两个
探索条件只共享这个中性下游域，不互相导入探索实现。

### Android vertical slice

`benchmark/android/` 把本地 APK 注册为受保护目标，并以可信 Emulator Runtime 实现
pixels-in/touch-out。通用 Session、Recorder 和 Topology 只增加平台元数据与触控动作，
网页 Runtime 和两个网页条件保持原行为。

Android 方法层位于两个互不 import 的目录：`agents/android_baseline/` 与
`agents/android_our_method/`。二者共享 `app_reproduction/` 的中立 Compose 脚手架、
虚构移动素材、断网构建器和 review emulator。输出固定为
`app_output/<handoff_id>/{project,review,artifacts}`；生成 APK 可进一步交给
独立的 `app-review` MCP/Skill。评审 Agent 通过
`app_evaluation.AppEvaluationSession` 自适应执行“观察截图—坐标操作—再次观察”，
并按人工清单产出带证据帧引用的四档功能报告；它不接触任一探索条件或目标 APK。
