# MCP Explorer

这里是 BlackBoxBench 唯一保留的 Agent 框架入口。

```text
External Agent ── MCP stdio ──▶ mcp_server.py
                                  │
                                  └─ /agent/{sid}/... ──▶ Controller
```

MCP Server 不负责模型调用或规划，探索阶段只提供受控工具：截图观察、坐标级类人输入、
Session 选择、证据记录、拓扑修订和 finalize。Agent 不能通过这些工具读取 DOM、
URL、selector、网络或本地应用实现。

finalize 成功后，Server 会立即把同一 Agent 切换到独立的复现容器。此时
`workspace_*` 操作只能写本次输出，定稿拓扑和公共素材以只读方式挂载。探索截图中的
真实账号、文档、消息等私人信息不得进入成品，实例内容必须取自公共虚构素材。
首次生成后还必须用 `start_reproduction_review` 和 `review_*` 对本地成品做像素/坐标级
黑盒复验；可在 `complete_reproduction_review(decision="revise")` 后修改并重验，默认
最多 3 轮，`accept` 后才能调用 `finish_reproduction`。

## 安装

```powershell
vendor/python/python.exe -m agents.cli_explorer.install
```

安装器默认为 Kimi Code 注册用户级 MCP 配置和 Skill；`--cli codex`
直接安装 Codex 配置，`--cli claude` 打印 Claude Code 注册命令。其他支持 stdio
MCP 的框架可直接
注册：

```text
<project>/vendor/python/python.exe -m agents.cli_explorer.mcp_server
```

## 两种运行方式

无环境变量时，Server 会检查并按需启动 Controller（多进程同时自举由跨进程
锁串行化，不会重复拉起），在首次工具调用时自动创建 Session；stdin 关闭时
自动 finalize 自己创建的 Session。

绑定现有 Session 时设置：

```text
BBB_CONTROLLER=http://127.0.0.1:7800
BBB_SESSION=sess_...
```

默认目标和预算可以通过 `BBB_APP_ID`、`BBB_MAX_ACTIONS`、`BBB_MINUTES` 设置。
也可以让 Agent 调用 `list_targets` 和 `start_session` 选择 app ID 或公网 URL。

## 并行会话

宿主 Controller（本地 runtime）天然支持多个并行 session：每个 Agent 对话
的 MCP 进程各自创建独立 session，拥有独立的 app 子进程与浏览器，互不串扰。
同一 live 目标仍同时只允许一个 session（共享登录 profile）。容器化部署
（BBB_RUNTIME=docker）共享单一 reference 浏览器，同一时刻只允许一个 session，
Controller 会以明确的 busy 错误拒绝第二个并发 session。

## 约束

外部 Agent 仍应运行在不含本仓库源码的隔离工作目录中，并禁用 shell、文件读取和
任意网络工具。MCP 只是唯一允许的 App 信息通道。正式评测应使用 Docker 部署，
避免本地 Agent 扫描 loopback 或读取绝对路径。

## 客户端容器化（严格模式）

要把客户端自带的宿主工具彻底关起来，可让 Agent 客户端运行在 Docker 容器中：

```powershell
vendor/python/python.exe -m agents.cli_explorer.agent_runtime --image <客户端镜像>
```

宿主侧 MCP 以 TCP 模式监听 `127.0.0.1`（强制 per-run token），容器**不挂载仓库、
不挂载 docker.sock**，只读挂载 MCP shim 与 Skill；客户端经
`host.docker.internal:<port>+token` 连回宿主 MCP（shim 见 `agent_runtime_assets/`）。
一次 TCP 连接即一次完整探索+复现生命周期。finalize 还会做跨会话筛查：定稿拓扑
与其他 Agent 的成品交付物做指纹比对，命中即拒绝（`benchmark/topology/crosscheck.py`）。

## 验证

`tests/test_mcp_server.py` 覆盖真实 stdio 子进程握手、图片观察、动作、discovery、
finalize、自举、目标切换、失效重绑定、中文 payload 和 finalize 后的自动复现交接。
`tests/test_agent_runtime.py` 覆盖 TCP 传输认证、容器封闭性与跨会话筛查。
