# Benchmark Protocol

## 1. 标准 Session 流程

```text
1. create_session(app_id, seed, budget)
2. launch_reference_environment   # App 子进程/容器 + 浏览器, 注入 seed
3. reset_reference_app            # 确认到达 canonical S0
4. agent exploration              # observe/action + incremental discovery
5. finalize_discovery             # Agent 主动收尾, 或预算耗尽后由 operator 触发
6. save artifacts                 # topology/json+md, coverage, summary, metrics, video
7. terminate session
```

CLI:

```bash
# 启动 controller (含 dashboard)
python -m benchmark.server --port 7800

# 创建会话 (operator)
curl -X POST localhost:7800/api/sessions -d '{"app_id":"ecommerce_demo"}'

# Demo Agent 跑完整探索
python -m demo_agent.run_demo --session <sid>

# 重放
curl -X POST "localhost:7800/api/sessions/<sid>/replay?mode=deterministic"
```

## 2. Phase I 结束条件

- `budget_exhausted` (actions / duration / observations 任一)
- Agent 调用 `finalize`
- operator close
- 环境致命错误 → `failed`,写 `crash_report.json`,可 reset 重开

## 3. Reset 语义

`POST /api/sessions/{sid}/reset`:

1. 关闭浏览器进程(整个 user-data-dir 丢弃)
2. 关闭 App 子进程;删除其数据文件;重新以 seed 初始化
3. 重新启动 App + 浏览器,导航至首页
4. trace 追加 `{"event": "reset"}`; frame_id 继续递增(不复位,保证证据链可追溯)
5. Agent 已写入的 discovery 数据**保留**(Agent 自己的记忆,由 Agent 决定是否修正)

## 4. Replay

### Visual Replay
Dashboard 内置播放器:按时间轴播放 frames + action 叠加;另可导出
`video/session.mp4`(光标、点击涟漪、步号、动作文本烧录)。

### Deterministic Replay
`POST .../replay?mode=deterministic`:

1. reset 环境至 S0
2. 按 actions.jsonl 顺序重放全部 action(节流:忽略 wait,间隔上限 250ms)
3. 逐帧对比(感知哈希汉明距 + 均方差),产出 `replay_{n}/report.json`:
   每步 `match: bool`、`diff_score`;末帧必须 match
4. 允许动画/光标闪烁造成的微小差异(阈值见 `benchmark/config.py`)

## 5. Exploration Metrics (benchmark-internal)

`metrics.json` 至少包含:

```text
actions_used, actions_by_type, observations_used, elapsed_s,
states/features/data/edges/hypotheses 计数,
hypotheses_confirmed / rejected / uncertain / unverified,
unique_screens(phash 去重), repeated_action_runs(连续重复动作段),
state_revisits(内部 phash 聚类回访计数),
features_per_100_actions
```

## 6. Demo Agent 验收脚本对应关系

| 验收 | 由什么验证 |
|---|---|
| AT1 observe 无 DOM | tests/test_security.py::test_observe_whitelist |
| AT2 click 生效 | tests/test_actions.py::test_click_changes_frame |
| AT3 trace 工件 | tests/test_recorder.py |
| AT4 Dashboard 实时 | 手动 + e2e 冒烟 |
| AT5 reset 一致性 | tests/test_reset.py |
| AT6 黑盒边界 | tests/test_security.py 全组 |
| AT7/8 发现与证据 | tests/test_topology.py + demo 产物 |
| AT9 topology 可视化 | e2e 冒烟 |
| AT10 replay | tests/test_replay.py |
