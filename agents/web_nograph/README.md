# MCP Explorer — web no-graph（消融条件）

web baseline（`agents/cli_explorer`）的消融变体：与 baseline 的差异**仅一处** ——
不注册任何 discovery 记录工具（`record_state` / `record_feature` / `record_data` /
`record_edge` / `record_hypothesis` / `resolve_hypothesis` / `revise`），也不做任何
记录校验；agent 自行决定是否记录、如何记录其探索发现。

```text
External Agent ── MCP stdio ──▶ mcp_server.py
                                  │
                                  └─ /agent/{sid}/... ──▶ Controller
```

- 探索工具面：`observe`、坐标级输入（click / drag / type_text / key / scroll /
  switch_tab / close_tab）与 `finalize`；不提供 DOM、URL、selector、网络或本地实现通道。
- 复现/复验链与 baseline 完全一致（`workspace_*` / `start_reproduction_review` /
  `review_*` / `complete_reproduction_review` / `finish_reproduction`）。
- `finalize` 时拓扑为空（消融如预期不产图，与 Android 的 `baseline_nograph` 口径一致），
  复现输入取自探索截图（`/exploration/`）与公共虚构素材。

安装（一次性；之后新会话直接 `/skill:web-nograph`）：

```bash
vendor/python/bin/python3 -m agents.web_nograph.install --cli kimi   # 或 codex / codebuddy / claude
vendor/python/bin/python3 -m agents.web_nograph.install --uninstall --cli kimi
```

批量探索（launch_batch 第 4 个 token = 条件）：

```bash
scripts/launch_batch.py explore kimi ecommerce_demo web-nograph
scripts/launch_batch.py explore dsh  ecommerce_demo web-nograph   # dsh 自动切 headless-web-nograph profile
```

严格隔离可改用客户端容器化：

```bash
python -m agents.web_nograph.agent_runtime --image bbb-agent-runtime
```
