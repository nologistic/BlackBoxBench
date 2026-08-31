# our-method (V0.2.4)

> 本包是托管模式基线（`agents/cli_explorer`）的独立演进副本。基线保持冻结用于
> 对照实验；所有改进只发生在本包。共享平台底座（`benchmark/*`、
> `reproduction/*` 的 Controller/Runtime/拓扑/沙箱）不复制、不修改。

# MCP Explorer（our-method 条件）

```text
External Agent ── MCP stdio ──▶ agents/our_method/mcp_server.py
                                  │
                                  ├─ /agent/{sid}/... ──▶ Controller（探索）
                                  └─ input_* / workspace_* / review_*（复现）
```

与基线的差异（V0.2.x 改进链）：

| 改进 | 机制 |
|---|---|
| 只读输入通道 | 新增 `input_list` / `input_read`：`/exploration/**`（探索素材）、`/materials/**`、`/input/functional_topology.json` 的唯一读取途径；`workspace_read` 仍只能读输出目录 |
| 交接索引 | finalize 自动生成 `/exploration/INDEX.md`：标注 STATE/FEATURE 证据帧，提供定向阅读清单 |
| 写前预检 | 第一次 `workspace_write` **或 `workspace_run`** 前必须已读拓扑 + 首批帧（`min(4, required)`）——所有能修改输出的入口统一拦截，堵住"用 shell/python/npm 绕过 write 预检"的路径 |
| 完成硬闸 | `finish_reproduction` 前必须经 `input_read` 实际读取拓扑 + ≥N 张帧（N = `min(BBB_OUR_MIN_FRAME_READS=12, 实际交接帧数)`）；计数绑定 handoff_id，新会话/完成自动清零 |
| 写路径证据验收 | `complete_reproduction_review(decision="accept")` 必须提供三段式 `write_flow_evidence`（before → after → persisted 观察编号）：服务端要求 before→after 含写类交互（被动 wait/move/scroll 和 F5/返回不算）、截图变化、after→persisted 含受信任的 `review_reload`、F5 或 BrowserBack+BrowserForward，且 persisted 不得退回 before 截图。这是**结构性**校验，仍不声称理解数据语义；`findings` 必须如实描述可见数据变化。无写功能目标用 `write_flow_exemption` 显式豁免，finish 只检查最终 accept 轮并与定稿拓扑全部 FEATURE 文本交叉检查。 |

复现与 review 流程同基线（像素/坐标黑盒复验、最多 3 轮 revise），差异只在
上述验收强度。

## 安装

```powershell
vendor/python/python.exe -m agents.our_method.install --cli codex
# 硬隔离（推荐用于正式对照实验）：注册 our-method 的同时移除基线 MCP
vendor/python/python.exe -m agents.our_method.install --cli codex --exclusive
# 恢复基线：python -m agents.cli_explorer.install --cli codex
```

其他 CLI（kimi/codebuddy/claude）同 `--cli` 参数。正式对照实验时，同一
Codex 任务只应暴露一个实验条件的 MCP——用 `--exclusive` 或手动
`codex mcp remove` 实现，不要依赖 Skill 提示做软隔离。

## 环境变量

| 变量 | 默认 | 说明 |
|---|---|---|
| `BBB_OUR_MIN_FRAME_READS` | 12 | 完成硬闸的帧读取下限（按实际交接帧数向下钳制） |
| `BBB_OUR_REQUIRE_ASSET_READS` | 1 | 0 = 关闭预检与完成硬闸（仅开发用） |

其余（`BBB_APP_ID` / `BBB_MAX_ACTIONS` / `BBB_MINUTES` / `BBB_CONTROLLER` /
`BBB_SESSION` / `BBB_REPRODUCTION_AUTOSTART` 等）与基线一致。

## 验证

`tests/test_our_method.py` 覆盖：input 路径白名单（合法/穿越/越权拒绝）、
`/input` 列表只暴露拓扑单文件、硬闸的 handoff 绑定与帧数钳制、写前预检、
会话重置清零、交接 INDEX 证据帧标注、写路径三段证据校验（被动动作拒绝、
显式 reload/返回探针、刷新后退回 before 拒绝）、最终 accept 轮豁免与拓扑交叉检查。
