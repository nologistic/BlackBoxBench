# CLI Explorer — 在真实 Agent 框架中评测模型

与 `agents/kimi_explorer/`(自建 harness 直调 API)不同,这里模型运行在
**真实 Agent 框架**(Kimi Code / Claude Code / Codex / OpenCode)内,
通过 **MCP 工具服务器**获得 GUI 交互能力,框架自己的规划/工具调用循环
驱动整个探索。这测的是"模型 × Agent 框架"组合体。

```text
Agent CLI (kimi / claude / codex / opencode, 无头或交互)
   │  MCP stdio (JSON-RPC, 21 个工具)
   ▼
agents/cli_explorer/mcp_server.py
   │  HTTP /agent/{sid}/...(白名单通道)
   ▼
Controller → Reference App(黑盒)
```

## 两种用法

### A. 零命令模式(skill 触发,自举)

一次性安装后,日常使用不需要任何终端命令:

```powershell
vendor/python/python.exe -m agents.cli_explorer.install   # 一次性
```

之后: 新开一个 kimi 对话 → 输入 `/skill:blackbox-explorer` → 探索自动开始。
原理: 用户级 `~/.kimi-code/mcp.json` 常驻注册 MCP 服务器(不带 session env);
CLI 拉起服务器时它自动检测/启动 Controller、自动创建新会话;
对话结束(stdin 关闭)时自动 finalize 并退出。`--uninstall` 可移除。
Claude Code 用 `install --cli claude` 打印对应注册指引。

### B. Attach 模式(手动启动,指定会话/参数)

你自己启动 Agent CLI 的交互界面,attach 进程负责建**指定**会话、注册 MCP、
安装探索 skill,并监督到会话结束:

```powershell
# 终端 1
vendor/python/python.exe -m benchmark.server --port 7800
# 终端 2
vendor/python/python.exe -m agents.cli_explorer.attach --cli kimi
```

按打印的指引操作:

1. 另开终端启动 CLI(示例: `kimi --agent-file "runs/<sid>/agent_workspace/explorer.md"`);
2. 在对话框输入 `/skill:blackbox-explorer`(attach 已把 skill 安装到
   `~/.kimi-code/skills/`),或直接粘贴打印的启动语;
3. 模型的思考直接显示在 CLI 自己的界面里;
   应用画面在 Dashboard 实时可见。

结束: Agent 调用 finalize,或在 attach 终端按 Ctrl+C —— 自动还原
用户级 mcp.json 并兜底 finalize。

### C. Headless 模式(无头自动跑,用于批量评测)

```powershell
vendor/python/python.exe -m agents.cli_explorer.run --cli kimi      # 或 claude/codex/opencode
```

runner 拉起 CLI 子进程并流式转发输出——Agent 的思考过程就在其 stdout/界面里。

参数(两种用法通用): `--app-id` `--session`(接入已有会话)
`--max-actions 400` `--minutes 30`;attach 另有 `--no-skill`。

CLI 的模型/账号由各 CLI 自己的登录态决定(如 Kimi Code 用 `/login`
的会员额度;Claude Code 可用 Kimi Code 端点,见官方文档
third-party-tools 页)。

## 各框架的黑盒约束实现

| CLI | MCP 注册 | 内置工具限制 | 权限模式 |
|---|---|---|---|
| kimi | 运行期临时注入用户级 `~/.kimi-code/mcp.json`(结束自动还原;项目级因 headless 信任提示不可用) | `--agent-file` 白名单 `tools: [mcp__blackboxbench__*]`(无 Bash/Read/…) | 默认(manual)— print 模式下 MCP 调用直接执行 |
| claude | `--mcp-config <ws>/mcp.json` | `--disallowedTools Bash Read Write Edit … WebFetch WebSearch …` | `--permission-mode bypassPermissions` |
| codex | `-c mcp_servers.*` 命令行覆盖(不碰用户 ~/.codex) | `--sandbox read-only` + 隔离 cwd | `approval_policy=never` |
| opencode | `<ws>/opencode.json` | `permission: {bash/read/edit/…: deny}` | run 默认非交互 |

共同兜底: 隔离空工作区作 cwd;任务 prompt/skill 明文禁止本地文件/shell/
网络;所有 App 信息只经 MCP 工具(像素截图)。

## 已知残余风险(本机模式)

拥有 shell 的 Agent 理论上可扫本机 loopback 端口(发现 CDP)或读绝对路径。
已通过工具禁用缓解;**正式评测用 Docker 部署**(agent 容器内无仓库、
无 CDP 可达性)。见 `docs/security_model.md`。

## 验证

- `tests/test_mcp_server.py`: 真实子进程握手 + observe 图像返回 + click/
  record/finalize 全链路(3 项)。
- `tests/test_cli_adapters.py`: 四个适配器的配置/命令/限制清单断言(6 项)。
- 真机冒烟: Kimi Code CLI headless 使用 MCP 工具完成 observe→click→observe,
  画面理解与坐标估计准确(session trace 记录 `click(148,391)` 命中 View 链接)。
- attach 机制实测: 会话创建/skill 安装/mcp.json 注入/finalize 检测/自动还原
  全链路通过。
