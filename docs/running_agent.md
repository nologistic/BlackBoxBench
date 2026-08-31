# 编写并运行一个 Agent

> 目标读者: 要在 BlackBoxBench 上评测自己 Agent 的研究者。
> 契约的唯一权威来源是 `docs/api_contract.md`;本文是上手指南。
> 注意: 容器链路未在编写机上运行验证 (无 Docker);文中客户端示例仅依据
> API 契约静态编写。

本文描述的是“托管探索工具”条件。若要评测 Agent 现场实现自己的截图、输入、证据
和分析工具，使用独立的 [自建工具条件](../self_explorer/README.md)；两种条件不能在
同一次 Agent 运行中同时启用。

## 1. 运行拓扑

Agent 永远只和 **controller** 通信:

```text
你的 Agent ──HTTP──> controller:7800  /agent/{sid}/...   (唯一允许的通道)
                          │
                          └── RuntimeRPC ──> reference 容器 (网络上对你不可达)
```

- 本机开发: controller 起在 `127.0.0.1:7800`。
- 容器评测: `docker compose up --build` 后,把你的 agent 打进
  `docker/Dockerfile.agent`(或用 `docker compose run --rm agent python your_agent.py`),
  agent 容器位于 `agentnet` (internal),除 controller 外**无任何网络可达性**。
- 会话由操作员通道创建:
  `POST /api/sessions {"app_id": "ecommerce_demo", "budget": {...}}` → `{"session_id": "sess_..."}`。

## 2. 快速上手 (最小客户端)

`agent_sdk/blackbox_bench_sdk` 是对下述路由的薄封装;在 SDK 就绪前,
下面这个 40 行的 httpx 客户端覆盖了全部协议,可直接使用 (仅依赖 httpx):

```python
import httpx

class BlackBoxClient:
    """BlackBoxBench agent-channel client (docs/api_contract.md §1)."""

    def __init__(self, controller: str, sid: str):
        self._c = httpx.Client(base_url=f"{controller}/agent/{sid}", timeout=60)

    def _post(self, path, payload=None):
        r = self._c.post(path, json=payload or {})
        r.raise_for_status()          # 403=budget_exhausted 410=session_closed ...
        return r.json()

    # ---- perception / action (§1.1, §1.2)
    def observe(self):                 return self._post("/observe")
    def action(self, **a):             return self._post("/action", a)
    # ---- discovery (§1.3)
    def record_state(self, **s):       return self._post("/discovery/state", s)
    def record_feature(self, **f):     return self._post("/discovery/feature", f)
    def record_data(self, **d):        return self._post("/discovery/data", d)
    def record_edge(self, **e):        return self._post("/discovery/edge", e)
    def record_hypothesis(self, **h):  return self._post("/discovery/hypothesis", h)
    def resolve_hypothesis(self, hid, **r):
        return self._post(f"/discovery/hypothesis/{hid}/resolve", r)
    def revise(self, kind, node_id, **rev):
        return self._c.request("PATCH", f"/discovery/{kind}/{node_id}", json=rev).json()
    # ---- finish
    def finalize(self):                return self._post("/finalize")
```

## 3. 工具列表 (Agent 可见的全部能力)

### 感知

| 调用 | 返回 | 说明 |
|---|---|---|
| `observe()` | `frame_id, width, height, screenshot_png_b64, cursor{x,y}, budget{...}` | 白名单 schema;**没有** url/elements/dom 等字段 |

### 操作 (`action(type=..., ...)`,成功返回 `{"accepted": true, "frame_id"}`)

| type | 参数 | 备注 |
|---|---|---|
| `click` / `double_click` | x, y | 坐标为屏幕像素 (1440×900, 原点左上) |
| `move_pointer` | x, y | 只动光标 |
| `mouse_down` / `mouse_up` | x, y | 组合成自定义按压 |
| `drag` | x1, y1, x2, y2, duration_ms=500 | |
| `type_text` | text | 输入到**当前焦点**;先点击获得焦点,无元素定位 |
| `key_press` / `key_down` / `key_up` | key | `"Enter" "Tab" "Escape" "Backspace" "F5" "ArrowDown" "a"` … |
| `scroll` | dx, dy | 像素,dy>0 向下 |
| `wait` | ms | 也会产生 after-frame |

action 回执只有 `accepted` —— "OS 事件已发送",不含任何语义结果 (T10)。
点击发生了什么,必须 `observe()` 自己看。

### 结构化记忆 (discovery,§1.3)

`record_state / record_feature / record_data / record_edge / record_hypothesis /
resolve_hypothesis / revise(update|merge|delete)`。要点:

- **Evidence 强制**: node/edge 至少一条引用真实 `step`+`frame_id` 的 evidence;
  伪造引用会被 controller 机械拒绝 (Evidence > Eloquence)。
- **泄漏检查**: 文本字段命中实现细节正则 (`/api/`、`React`、`localhost`、
  `DOM`、`SELECT` …,完整清单见 `benchmark/topology/models.py: LEAK_PATTERN`)
  一律 422。描述**可观察行为**,不是实现。
- **可修订**: 早期理解错了就 update/merge/delete,全部留痕。
- **Hypothesis**: 不确定的猜测登记为 hypothesis + `next_probe`,验证后 resolve;
  未验证的猜测不得写成 confirmed。

### 预算 (§6)

`actions_remaining / seconds_remaining / observations_remaining`,随 observe 响应
返回;任一耗尽 → observe/action 返回 403 `budget_exhausted`,**但 discovery 写入与
finalize 仍可用** —— 留预算收尾,或耗尽后把记忆整理完再 finalize。

### 结束

`finalize()` → 生成 `functional_topology.json|.md`、`coverage_report.json`、
`session_summary.json`。预算耗尽后 operator 也可替你触发。

## 4. 用真实 VLM 组探索 loop (伪代码)

```python
client = BlackBoxClient("http://controller:7800", sid)
memory = TopologyMemory()          # 你自己维护的增量图 (也实时写入 discovery)

while True:
    obs = client.observe()
    if obs is None:                # 403 budget_exhausted
        break
    png = b64decode(obs["screenshot_png_b64"])

    decision = vlm.chat(images=[png], prompt=f"""
        你在逆向理解一个黑盒 Web 应用。只能看截图,只能发送人类输入。
        已知拓扑记忆: {memory.summary()}
        未验证假设: {memory.open_hypotheses()}
        预算: {obs["budget"]}
        系统策略: 广度优先枚举可见 affordance; 对表单尝试合法/非法/边界输入;
        对"可能持久化"的功能用 F5 刷新验证; 同页多态 (空/非空) 视为不同 state。
        输出下一个动作 (click/type/key/scroll/wait + 坐标) 或 finalize。""")

    if decision.type == "finalize":
        break
    receipt = client.action(**decision.action)     # {"accepted": true, "frame_id"}
    if not receipt["accepted"]:
        continue

    after = client.observe()                       # 自己判断造成了什么
    finding = vlm.compare(before=png, after=b64decode(after["screenshot_png_b64"]),
                          action=decision.action)
    # 增量写入结构化记忆 —— 禁止"最后凭记忆一次性生成"
    for rec in finding.to_discovery_records(step_hint=receipt["frame_id"]):
        client.__getattribute__(f"record_{rec.kind}")(**rec.payload)
    memory.apply(finding)

client.finalize()
```

工程建议:

- 每 N 步或每发现新 state 就把假设队列里最便宜的一条 probe 掉。
- 在 Agent 自己的探索记忆中对近期画面去重，避免原地打转；研究者也可以从最终
  轨迹离线统计重复动作和状态重访。
- 坐标来自对截图的目测;点偏了不会有任何报错 —— 用 observe 对比验证,
  这是本 benchmark 的核心考点。

## 5. 禁忌清单 (违反任一即破坏评测有效性)

- ✗ 尝试访问 DOM / AXTree / CDP / Playwright / 浏览器 console —— Runtime 与
  RPC 两侧都**不存在**该能力面 (T2),不必试,试了也没有。
- ✗ 尝试读取当前 URL / 地址栏 / 网络请求 / cookies / localStorage。
- ✗ 直接请求 app 后端或任何非 controller 地址 —— 容器网络层面不可达 (T3);
  本机模式下直连 app 也只得到 404。
- ✗ 读取文件系统、`ground_truth.json`、会话目录、日志 —— SDK 无文件 API,
  agent 容器内根本没有这些文件 (T1/T7)。
- ✗ 用 `key_press` 组合快捷键开 DevTools/视图源 —— kiosk 无 chrome,
  快捷键无效 (T4),且会计入你的 action 预算。
- ✗ 在 discovery 文本中写实现细节 (路由/框架/选择器/SQL) —— 422 拒绝 (§2 泄漏检查)。
- ✗ 凭训练记忆"认出" app 直接默写拓扑 —— evidence 校验要求每条声明引用
  真实 frame/step;虚构证据会被机械拒绝。正式 benchmark 使用未公开 App。
