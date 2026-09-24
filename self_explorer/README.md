# 自建工具探索模式

这是 BlackBoxBench 的第二种、与现有托管工具模式并列的实验条件。

| 条件 | Agent 起点 | 可见接口 | 探索工具由谁实现 |
|---|---|---|---|
| 托管工具模式 | `agents/cli_explorer/` | observe、坐标动作、发现记录 | Benchmark 提供 |
| 自建工具模式 | `self_explorer/` | 只有任务限制；隔离 workspace 为空 | Agent 现场实现 |

## “不给工具”的准确含义

完全没有物理输入输出面时，任何 Agent 都不可能看到或操作网页。因此可信侧维护
最低限度的屏幕像素设备和 Linux 输入事件设备。`begin_workspace` 与容器环境只明确
浏览器已经启动并给出 I/O 根位置，避免 Agent 把“没有浏览器可执行文件”误判为“目标
不可访问”；设备文件格式、输入协议和使用步骤仍不提供。Agent 初始还会得到明确的
起始 URL、任务目标和禁止事项，进入的是一个真正空白的 `/workspace`。系统不提供
探索工具，也不规定 Agent 应采用何种方法、记录、覆盖标准或工作流。

不提供 `DEVICE_PROTOCOL.md`、`OUTPUT_CONTRACT.md`、示例客户端、PNG 截图、
click/observe、拓扑记录函数或 starter code。设备也没有动作回执或语义错误。

MCP 的六个操作只是通用工作台：开始、列文件、读文件、写文件、运行程序、结束；
程序实际运行在隔离容器中。这些操作不接触目标浏览器；内置样例条件断网，公网 URL
条件允许普通出网，但网络用途受 Skill 约束。目标浏览器始终由可信容器运行，工作台
只通过已标识根位置下的原始 I/O 设备与它交互。

## 隔离边界

- `reference_raw`：可信侧，包含样例 App、Chromium 和原始设备桥；内置样例时
  `network_mode: none`。显式公网 URL 时该容器获得 bridge 网络，并强制让 Chromium
  经过 loopback public-only proxy；代理逐连接解析并拒绝私网、回环、保留地址和
  80/443 以外端口，防止重定向或 DNS rebinding 把浏览器带到内部服务。探索会话始终
  不暴露端口。
- `tool_builder`：不可信侧使用只读根文件系统，只挂载本次空白 `/workspace` 和硬件式
  设备卷；内置样例时断网，公网 URL 时获得普通出网，不设目标 URL/域名白名单。
- Agent 镜像的 Docker build context 是 `self_explorer/agent_image/`，不是仓库；
  现有 `benchmark/`、`agent_sdk/`、`agents/cli_explorer/` 从未进入镜像。
- 两个容器不共享网络命名空间、进程或根文件系统，只共享不带说明的设备卷。公网 URL
  模式下 Agent 网络行为由 Skill 软约束：可获取与目标无关的通用工具依赖，但不得请求、
  搜索或下载目标及其实现；这属于实验合规要求，而非防恶意 Agent 的硬安全边界。
- MCP 服务运行在容器外，但只允许访问本次 workspace，并把命令固定送进
  `tool_builder`；不会把宿主路径或仓库文件提供给 Agent。

容器隔离不能撤销 Agent 客户端自己附带的宿主机工具。正式实验必须在 Agent
框架中仅启用 `blackboxbench-self-built` MCP，禁用其 host shell、文件访问、网页
浏览、搜索、下载以及其他 MCP。若框架无法做工具 allowlist，就不能把该次运行
视为严格隔离实验。即使提示词要求 Agent 自律，也不能替代这条启动条件。

## 宿主侧防线

容器边界之外，客户端自带的宿主工具是剩余的泄漏面。自建模式在宿主侧叠加
四层防御（`self_explorer/hardening.py`）：

1. **限定存储**：自建会话默认保存在项目的 `runs/self_built/self_*/`，
   `BBB_SELF_RUNS_ROOT` 可覆盖。Docker 只挂载本次 `workspace`，不会挂载项目根、
   其他 session 或 `website_output/`。但项目内路径本身不是宿主文件系统安全边界：
   正式实验仍须禁用客户端宿主文件/shell 工具，或使用客户端容器化严格模式。
2. **答案封存**：会话期间（探索 + 复现全程），`sample_apps/*/ground_truth.json`
   被事务性移入 runs 根下私有 `.seal/` 目录，会话结束自动恢复；进程崩溃
   遗留的封存会在下次 acquire 或 `launcher recover` 时自动找回。
   `BBB_SELF_SEAL=0` 关闭；`BBB_SELF_SEAL_APP_SOURCE=1` 同时封存被测 App 源码。
   封存是**跨进程租约**：多个自建会话可以同时运行，共享同一份封存；每个
   持有者持有操作系统级文件锁（进程死亡即释放），只有最后一个存活持有者
   释放后答案文件才会恢复——并行会话不会互相解封对方的 ground truth。
3. **完整性检测**：两次 `finish_workspace` 都会把工作区全部文本与基准私有
   内容做指纹比对（ground truth、被测 App 源码、可信设备桥、托管实现、
   其他会话工件），探索期还对 Agent 自写文本执行实现泄漏扫描。`.deps`、`.venv`、
   `node_modules` 等常规第三方依赖根会被排除，避免扫描上游包源码产生误报；这些目录
   同样不会进入复现交接。命中即拒绝交接；
   不检查拓扑、报告、证据数量、最少状态/功能或目录结构。错误只回报类别与文件名，不回显内容，
   检测器崩溃时同样拒绝（fail closed）。
4. **审计**：每次工作区工具调用追加记录到 `run_dir/audit.jsonl`（位于 MCP
   可见工作区之外，Agent 不可达），操作员可事后归因。

这些是纵深防御的补充层，不是客户端工具 allowlist 的替代品：正式对比实验仍
要求客户端只启用 `blackboxbench-self-built` MCP。

## 客户端容器化（严格模式，推荐）

上面四层仍依赖客户端自律。若要把客户端自带的宿主工具也彻底关起来，可以让
**Agent 客户端整个运行在 Docker 容器里**（托管模式的 agent 容器同款思路）：

```powershell
vendor/python/python.exe -m self_explorer.agent_runtime --image <你的客户端镜像>
```

该命令会：

1. 在宿主启动 MCP 的 TCP 模式（绑定 `127.0.0.1`，本机与 Docker 代理可达、
   LAN 不可见，强制 per-run token 认证）；
2. 启动 `bbb-self-agent-*` 容器：**不挂载仓库、不挂载 docker.sock、不挂载
   workspace**——只读挂载通用 MCP shim 与任务 Skill，`--cap-drop ALL` 等加固；
   默认桥接网络供客户端访问其模型 API；
3. 打印进入容器与注册 MCP 的指引（客户端用 `python3 /opt/mcp-shim/mcp_relay.py`
   或 `bash /opt/mcp-shim/mcp_relay.sh` 作为 stdio MCP 命令，shim 经
   `host.docker.internal:<port>+token` 连回宿主 MCP）。

容器内的客户端只能看到容器文件系统——仓库、ground truth、其他 session 的
结果物理不可达。一次 TCP 连接即一次完整探索+复现生命周期；断线重连会强制
接管被代理遗留的僵尸连接（Docker Desktop 代理不总是传播容器侧断连，服务端
用"认证后驱逐旧连接 + 秒级读超时"处理）。客户端镜像示例见
`agent_runtime_assets/Dockerfile.example`（需要 bash 或 python3 任一）。

## 使用

需要 Docker Compose。仅手工启动环境：

```powershell
vendor/python/python.exe -m self_explorer.launcher start --app ecommerce_demo
```

手工启动公网目标（目标也可在 Skill 调用 `begin_workspace` 时逐次指定）：

```powershell
vendor/python/python.exe -m self_explorer.launcher start --url https://example.com/
```

### 登录后网站

宿主 `scripts/live_login.py` 使用 Windows/macOS 浏览器，Cookie 受操作系统加密，不能
可靠地复制给 Linux Docker。自建模式因此维护独立的 Docker-native golden Profile。
首次探索某个需要登录的网站前运行：

```powershell
vendor/python/python.exe scripts/self_live_login.py --url https://example.com/ --build
```

命令只启动可信 `reference_login`，不会启动 `tool_builder` 或 Agent；它临时在随机
`127.0.0.1` 端口打开一个纯像素/坐标操作员页面。人工登录并回到终端按 Enter 后，
Profile 保存为 `runs/live_targets/live_<host>/docker_profile_golden/`。以后对同一 host
调用 `begin_workspace(target_url=...)` 时，启动器会把 golden Profile **只读挂载给
`reference_raw`**，复制到该会话的 tmpfs 可写 Profile 后启动 Chromium。
`tool_builder`、Agent workspace、探索工件交接和复现容器都看不到 Cookie 或 Profile。

只有首次构建或可信镜像代码变化时需要 `--build`；后续续登/刷新 Cookie 可省略。
维护登录态时不要同时启动同一站点的探索会话。

封存异常恢复（正常情况自动完成，无需手工执行）：

```powershell
vendor/python/python.exe -m self_explorer.launcher recover
```

接入 Kimi Code：

```powershell
vendor/python/python.exe -m self_explorer.install --cli kimi --app ecommerce_demo
```

接入 Codex：

```powershell
vendor/python/python.exe -m self_explorer.install --cli codex --app ecommerce_demo
```

接入 CodeBuddy（打印设置界面用的 MCP JSON 与严格模式操作指引）：

```powershell
vendor/python/python.exe -m self_explorer.install --cli codebuddy --app ecommerce_demo
```

在新 Codex 任务中调用 `$self-built-explorer https://目标网址/`。严格实验需禁用 Codex 的宿主文件、
shell、浏览器、搜索和其他 MCP，只保留 `blackboxbench-self-built`。

新对话只启用该 MCP 后运行 `/skill:self-built-explorer`。Claude Code 可用
`--cli claude` 输出注册提示。Codex、OpenCode 等只要支持 stdio MCP 和工具
allowlist，也可把命令注册为：

```text
<python> -m self_explorer.mcp_server
```

MCP 在 `begin_workspace(target_url=...)` 时才创建环境；没有明确起始 URL 会拒绝启动。
这个参数只决定浏览器最初打开的位置，不构成 URL 或域名白名单：服务端重定向、跨域
登录、CDN、点击链接和新页面均可继续访问其他公开 HTTP(S) 地址。公网条件的工作台
也不设置 URL 白名单；Skill 通过行为约束禁止 Agent 搜索、下载或直接请求目标来绕过
可见探索。
对话关闭会自动停止容器；完成的工作区原样保留在项目内
`runs/self_built/self_*/workspace/`，不要求其中存在特定子目录或文件。
它不创建原有 Controller session，所以不会在 Dashboard 里产生测试 session。
停止时本次临时 device volume 会一并删除，不留下 Docker 会话卷。

## 并行运行多个自建会话

多个 Agent 可以同时进行自建探索与复现，互不干扰：

- **每个 Agent 一个 MCP 进程**。宿主 stdio 模式下，客户端（CodeBuddy、
Codex 等）为每个对话各自拉起一个 `self_explorer.mcp_server` 进程；
  严格模式（`self_explorer.agent_runtime`）每次调用独立端口与 token。
- 每个会话获得**独立的** `self_*` run 目录、独立 Compose 项目与独立
  的 device volume，容器互不可见。
- ground truth 封存由跨进程租约共享（见“宿主侧防线”第 2 条）：并行
  会话期间答案文件保持封存，最后一个会话结束才恢复。
- `launcher recover` 现在只回收**持有者进程已死亡**的封存，不会误恢复
  正在运行的并行会话。

安装器会为 stdio MCP 显式设置 `PYTHONUTF8=1`。这是协议兼容要求：包括
`stderr` 诊断在内的三个标准流都保持 UTF-8；即使客户端关闭诊断流，日志写入
失败也不会中断环境启动、回滚或封存恢复。

探索工作区通过安全校验后，第一次 `finish_workspace` 会停止上述两个探索容器，
并立即创建与它们分离的断网复现容器。Agent 仍只看到同一组通用工作区
操作，但此时工作区映射到 `website_output/<handoff_id>/`；公共素材以及 Agent
自发保留且后缀位于安全白名单内的普通记录/媒体以只读方式提供。自建工具源码和
可执行文件不会交接，也不存在必需拓扑输入。第二次 `finish_workspace` 结束复现并
保留交付文件。
