# IMPLEMENTATION REPORT — BlackBoxBench Phase I

日期: 2026-08-18 · 状态: **完成并通过端到端验证**

## 1. Implemented Components

| 组件 | 位置 | 说明 |
|---|---|---|
| Runtime 抽象 | `benchmark/runtime/base.py` | 仅 pixels+HID 的能力面;无 DOM/evaluate/URL 方法(物理不存在) |
| 本机 Runtime | `benchmark/runtime/local_chromium.py` | headless Chrome for Testing,CDP 白名单(截图/Input/Emulation/Browser.close);导航期瞬时 detach 与 socket reset(WinError 10054 等 OSError)自动重连重试;`--no-proxy-server` + host-resolver 锁 + 网关秘钥头 |
| 容器 Runtime | `benchmark/runtime/docker_x11.py` + `docker/runtime_rpc_server.py` | Xvfb + xdotool + scrot,容器内**无 CDP**;RPC 仅截图/输入/reset/health |
| Orchestrator | `benchmark/orchestrator/` | session 生命周期、预算(actions/duration/observations)、reset(重播种+冷启动浏览器)、app 注册表、atexit 清理 |
| Recorder | `benchmark/recorder/` | frames(含光标渲染)、actions/observations/discovery JSONL、settle 检测、phash/MSE 差分、mp4 视频(叠加步号/动作/点击准星) |
| Topology | `benchmark/topology/` | pydantic 模型 + 泄漏正则 + canonical id + evidence 校验 + 修订(update/merge/delete)+ finalize(JSON/MD/coverage) |
| Replay | `benchmark/replay/replayer.py` | deterministic replay:reset→重放(逐步 settle)→逐帧 phash 对比→report |
| Server | `benchmark/server.py` | `/agent/{sid}/...`(白名单)+ `/api/...`(operator)+ 静态 dashboard |
| Agent SDK | `agent_sdk/blackbox_bench_sdk/` | `Environment`: observe/click/type/key/scroll/drag/wait + record_state/feature/data/edge/hypothesis/resolve/revise/finalize;无任何语义读取方法 |
| Dashboard | `dashboard/static/` | Live Observer(实时帧+点击涟漪+预算+动作流)、Topology Graph(SVG 三列图,节点详情+evidence 帧对照)、Session Replay(播放器) |
| Sample App | `sample_apps/ecommerce_demo/` | "Nimbus Market": 8 商品/4 分类、搜索、登录、购物车(数量/移除/优惠券)、checkout 前置登录、下单、订单页、服务端持久化、网关保护、确定性 seed |
| Demo Agent | `demo_agent/run_demo.py` | 脚本化校准策略,97 步完成全链路探索并 finalize |
| Kimi VLM Agent | `agents/kimi_explorer/` | 自建 harness 直调 API:observe(PNG)→Kimi JSON 决策→控制台输出思考→坐标动作→evidence 自动绑定→finalize;done 自检防提前收尾;离线 mock 测试覆盖 |
| CLI 框架评测 | `agents/cli_explorer/` | **MCP stdio 服务器(零依赖手写 JSON-RPC,21 工具,自举式)+ kimi/claude/codex/opencode 四适配器**:模型在真实 Agent 框架内自主规划;工具白名单/沙箱/隔离工作区保证黑盒;install 一次后 `/skill:blackbox-explorer` 零命令启动(服务器自建 controller+会话,对话结束自动 finalize);真机冒烟通过(Kimi CLI headless observe→click(148,391)→observe 全对) |
| Docker 交付 | `docker/` + `docker-compose.yml` | 三容器拓扑,refnet/agentnet 双 internal 网络,非 root,cap_drop ALL |
| 测试 | `tests/` | **35 passed**: 安全边界 10、动作/记录 6、reset/determinism 2、topology 13、replay 1、冒烟另有 sample app 44 项 |
| 文档 | `docs/` × 7 + README + AGENTS.md | architecture/api_contract/security_model/benchmark_protocol/topology_schema/adding_reference_app/running_agent |
| Bootstrap | `scripts/bootstrap.py` | 跨平台一键:vendor 便携 Python + 固定版本 Chrome for Testing + 依赖 |

## 2. Architecture

三层安全域: **Agent ⇄ Controller ⇄ Reference Environment**。
Agent 仅经 `/agent/{sid}/` 白名单通道通信;Controller 持有唯一接触浏览器的
Runtime(本机 CDP 白名单 / 容器 RPC);Reference App 由网关头 + host-resolver 锁
保护。数据流、坐标系、工件布局详见 `docs/architecture.md`。

环境自包含: `vendor/python/`(python-build-standalone 3.12.14)、
`vendor/chromium/`(Chrome for Testing 152.0.7977.42)、ffmpeg(imageio-ffmpeg
自带)——任何机器 `python scripts/bootstrap.py` 一键重建。

## 3. Security Isolation(实测验证)

- `observe()` 响应字段 == 白名单(frame_id/timestamp/width/height/screenshot/cursor/budget)[test]
- action 回执 ⊆ {accepted, frame_id, step} [test]
- SDK 无 find_element/get_dom/get_url/get_html/get_network/evaluate/locator [test]
- CDP 客户端对 Runtime.evaluate / DOM.getDocument / Accessibility.getFullAXTree / Network.enable 抛 PermissionError [test]
- 直连 App 端口(无网关头)→ 404 空 body [test]
- ground truth 无 HTTP 路由 [test];agent 镜像不含 sample_apps [静态]
- 网关 secret 不出现在任何 session 工件 [test]
- 浏览器启动 flags: `--no-proxy-server`、host-resolver `MAP * ~NOTFOUND`、headless 无 chrome [test]
- discovery 文本命中泄漏正则(`/api/`、`.tsx`、`src/`、框架名…)→ 422 [test]
- evidence 引用不存在 frame/step → 422;confirmed 声明无 evidence → 422 [test]
- 威胁模型全表: `docs/security_model.md`(含容器/本机两种模式的强度分级)

**本机实测关键修复**: 系统代理导致 `ERR_PROXY_CONNECTION_FAILED`(浏览器流量被
引向系统代理)→ 加 `--no-proxy-server`;网关 httpx `trust_env=False`;
内部 urllib 全部走无代理 opener。**SDK / demo / smoke 测试 / docker 驱动同样
全部 `trust_env=False`**(httpx 会读 Windows 注册表代理,否则 loopback 请求被
代理拦成 502);demo 启动新增 Controller 连通性预检与中文提示。

## 4. Agent API

见 `docs/api_contract.md`。action 12 种(click/double_click/move/mouse_down/up/
drag/type_text/key_press/key_down/key_up/scroll/wait),discovery 8 端点,
全部写操作带 evidence+泄漏校验。

## 5. Functional Graph Schema

`G=(V,E)`,V=STATE+FEATURE+DATA,E=10 种边类型;confidence∈[0,1],
status∈{hypothesized,confirmed,rejected,uncertain};evidence 强制;
增量构建 + 可修订;`schemas/topology.schema.json` 机器可读定义。

## 6. Demo Result(实际运行产物)

Session `runs/sess_20260819_032900_4e2075/`:

- **97 个动作、117 帧、19 次 observe、用时 99s**
- **25 节点**(11 STATE / 11 FEATURE / 3 DATA)+ **17 边**,confirmed ratio 100%
- 2 个假设均被主动探针证实(购物车刷新持久化 F5;checkout 需登录)
- 覆盖错误路径: 超库存加购、数量为 0、无效优惠券、错误密码、空表单下单、空搜索结果
- artifacts: functional_topology.json/.md、coverage_report.json、session_summary.json、
  metrics.json、hypotheses.json、actions/observations/discovery.jsonl、
  frames/、video/session.mp4(叠加光标/点击准星/步号)
- 全部关键帧经人工目检核对(搜索空态、错误条、折扣行、登录重定向、
  订单确认 #1002、logout 回匿名首页)
- 校准中发现并修正: LAYOUT.md 关于 banner 不位移详情页/登录页的错误假设——
  以真实帧为准修正坐标(这正是黑盒探索方法论的一次实演)

Dashboard 目检截图: `docs/images/_dash_live.png`(Live Observer)、
`docs/images/_dash_topology.png`(Topology Graph)、
`docs/images/_dash_live_thoughts.png`(运行中实时画面 + 侧栏状态;注:思考流面板已按最新决策移除,运行过程固定在 Agent CLI 界面展示)。

### 实时可观察性(Phase I 增补,后调整)

- Dashboard Live 视图: 实时帧(700ms)+ 点击涟漪 + 状态/预算/Discovery 计数 + 动作流。
- **Agent 思考过程固定在 Agent CLI 自己的运行界面展示**(交互 attach 模式看 TUI,
  无头 run 模式看转发的 stdout);系统不再单建思考输出通道(thoughts.jsonl/思考面板已移除)。


## 7. Tests

```text
tests/test_security.py   10 项  (白名单/SDK 能力面/直连 404/泄漏拦截/evidence 强制/CDP 白名单…)
tests/test_actions.py     6 项  (点击改帧/输入+Enter/scroll/drag/非法坐标/预算强制/trace 工件)
tests/test_reset.py       2 项  (reset 回 S0;同轨迹重跑 phash 一致)
tests/test_topology.py   13 项  (id/校验/修订/合并/删除/假设生命周期/finalize)
tests/test_replay.py      1 项  (deterministic replay 5 步,final_match=True)
tests/test_mcp_server.py  3 项  (MCP 握手/图像返回/动作+发现+finalize)
tests/test_cli_adapters.py 6 项 (四 CLI 适配器配置/限制断言)
tests/test_vlm_agent.py   2 项  (VLM harness 离线 mock: 证据解析/JSON 纠错/finalize)
tests/fixtures/miniapp.py       确定性测试 App(计数器+echo)
合计 50 passed + sample app smoke 44 passed
```

## 8. Known Limitations

1. **本机无 Docker**: 三个镜像与 compose 仅静态验证(yaml/契约/语法/mock RPC 联调),
   未 build/up;Debian 包名按 bookworm 惯例编写,首装需实机确认
   (见 `docs/adding_reference_app.md` §9 checklist)。
2. 本机模式隔离为进程/API 级,防意外不防恶意对抗;正式评测须用容器部署。
3. Demo Agent 为脚本化策略,不演示视觉推理;真实 VLM 经同一 SDK 接入
   (`docs/running_agent.md` 有 loop 伪代码)。
4. headless Chrome 一次性 `--screenshot` 模式在本机挂起(未使用该路径;CDP 正常)。
5. F5 刷新探针产生的像素变化不可见(同页重载)——证据效力依赖服务器端状态
   在刷新后仍然呈现的帧;更严格的"刷新"语义可后续加 Ctrl+R 变体。
6. 仪表盘对 archived 会话的个别轮询会短暂显示错误横幅(自动消失),不影响数据。
7. 单浏览器并发未调优;Phase I 优先 Isolation/Correctness/Auditability。

## 9. Next Steps(Phase II 挂钩已预留)

- `ReferenceBehaviorRunner / GeneratedBehaviorRunner / DifferentialEvaluator`
  接口位: 同一 Runtime 抽象 + 同一 trajectory 格式,可做"原 App vs Agent 重建 App"
  的差分功能评测。
- 真实 VLM 探索策略研究(以 demo 轨迹为 warm-start baseline)。
- 多 seed、多 App(按 `adding_reference_app.md` 接入)、Android/Desktop Runtime。
- 探索效率分析: features_per_100_actions 等指标已入 metrics.json。
