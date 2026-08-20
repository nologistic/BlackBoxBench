# API 契约 (Controller HTTP API)

> 所有实现必须严格遵循本文件。Base URL 默认 `http://127.0.0.1:7800`。
> 响应除注明外均为 `application/json`。错误统一为
> `{"detail": "<error_code>", "message": "..."}` + 合适的状态码。

## 0. 术语

- `sid`: session_id,形如 `sess_20260818_123000_ab12cd`
- `frame_id`: 整数,会话内单调递增,对应 `frames/frame_000127.png`
- `step`: 整数,每个被接受的 action 使 step+1; observe 不增加 step

## 1. Agent 通道 (`/agent/{sid}/...`)

**这是 Agent SDK 唯一允许访问的路由族。** 响应字段为白名单,
任何 URL/DOM/元素语义字段的存在都是 P0 级 bug。

### 1.1 `POST /agent/{sid}/observe`

请求体: `{}` (无参数)

响应 200:

```json
{
  "frame_id": 127,
  "timestamp": "2026-08-18T12:00:00.000Z",
  "width": 1440,
  "height": 900,
  "screenshot_png_b64": "iVBOR...",
  "cursor": {"x": 812, "y": 431},
  "budget": {
    "actions_remaining": 372,
    "seconds_remaining": 1412,
    "observations_remaining": 900
  },
  "brief": "这是一个电商购物应用。你拥有一个测试账号: alice / alice123 …",
  "tabs": {"count": 2, "active": 1}
}
```

`brief` 是 operator 编写的**任务简报**(如测试账号)——属于任务输入,
不含任何实现细节;由 App 注册表声明(`orchestrator/apps.py::AppSpec.brief`)。

`tabs` 是**窗口管理级**信息:标签页数量与当前激活索引(按打开顺序,0 起)。
不含 URL/标题等任何语义;无标签概念的 runtime 返回 null。

错误: 404 会话不存在 / 410 会话已结束 / 403 预算耗尽(`budget_exhausted`)。

### 1.2 `POST /agent/{sid}/action`

请求体 (`Action`):

```json
{"type": "click", "x": 812, "y": 431}
```

| type | 参数 |
|---|---|
| `click` | x, y |
| `double_click` | x, y |
| `move_pointer` | x, y |
| `mouse_down` | x, y |
| `mouse_up` | x, y |
| `drag` | x1, y1, x2, y2, duration_ms (默认 500) |
| `type_text` | text |
| `key_press` | key (如 `"Enter"`,`"Tab"`,`"Escape"`,`"Backspace"`,`"F5"`,`"BrowserBack"`,`"BrowserForward"`,`"ArrowDown"`,`"a"` 等) |
| `key_down` | key |
| `key_up` | key |
| `scroll` | dx, dy (像素, dy>0 向下) |
| `wait` | ms |
| `switch_tab` | tab_index (0 起,按打开顺序;点击打开的新页会自动跟随,用它可以切回) |
| `close_tab` | — (关闭当前标签页并回到上一个;最后一个不可关) |

响应 200: `{"accepted": true, "frame_id": 128, "step": 42, "tabs": {"count": 2, "active": 1}}`
(新 frame 在画面稳定检测后产生; `wait`/`move_pointer` 也会产生 after-frame;
`step` 为本会话已接受动作计数,用于 evidence 引用)

响应 202 风格不存在;失败即非 200:

- 400 `invalid_action` / `invalid_coordinates` / `invalid_key` /
  `invalid_tab_index` / `cannot_close_last_tab`
- 403 `budget_exhausted`
- 410 `session_closed`
- 503 `runtime_unavailable` (浏览器崩溃等;会话标记 failed)

**绝不返回**被点击元素、语义结果、URL 变化等。

### 1.3 Discovery 工具集

所有写入均做 schema 校验 + evidence 校验 + 泄漏检查(§4)。

#### `POST /agent/{sid}/discovery/state`

```json
{
  "name": "Cart with items",
  "description": "...",
  "visual_evidence": [231, 240],
  "entry_conditions": ["至少一次成功的 add_to_cart"],
  "observed_elements": ["商品行", "数量输入框", "删除按钮"],
  "confidence": 0.92
}
```
→ `{"state_id": "state_cart_with_items"}` (id 由系统规范化生成)

#### `POST /agent/{sid}/discovery/feature`

```json
{
  "name": "Add product to cart",
  "description": "...",
  "preconditions": ["state_product_detail"],
  "trigger": {"type": "click", "target_description": "Add to Cart 按钮"},
  "inputs": [],
  "postconditions": ["购物车计数增加", "商品出现在购物车"],
  "persistent_effects": ["刷新后购物车仍保留该商品"],
  "constraints": [],
  "error_cases": [],
  "evidence": [{"step": 147, "before_frame": 146, "action": "click(811,432)", "after_frame": 147}],
  "confidence": 0.95
}
```
→ `{"feature_id": "feature_add_product_to_cart"}`

#### `POST /agent/{sid}/discovery/data`

```json
{"name": "Cart", "description": "服务器端持久化的购物车", "confidence": 0.8}
```
→ `{"data_id": "data_cart"}`

#### `POST /agent/{sid}/discovery/edge`

```json
{
  "source": "feature_checkout",
  "target": "state_authenticated",
  "type": "REQUIRES",
  "evidence": [{"step": 200, "before_frame": 199, "action": "click(...)", "after_frame": 200}],
  "confidence": 0.9,
  "status": "confirmed"
}
```
→ `{"edge_id": "edge_0007"}`

edge.type 枚举: `REQUIRES TRANSITIONS_TO MUTATES ENABLES DISABLES PERSISTS_TO REVEALS DEPENDS_ON CONFLICTS_WITH VALIDATES`
status 枚举: `hypothesized confirmed rejected uncertain` (默认 confirmed)

#### `POST /agent/{sid}/discovery/hypothesis`

```json
{
  "statement": "购物车可能在刷新后仍然保留",
  "evidence": [],
  "confidence": 0.48,
  "next_probe": "加入一件商品后按 F5 刷新"
}
```
→ `{"hypothesis_id": "hyp_0003"}` (初始 status=unverified)

#### `POST /agent/{sid}/discovery/hypothesis/{hid}/resolve`

```json
{"status": "confirmed", "evidence": [{"step": 300, "before_frame": 299, "action": "key_press(F5)", "after_frame": 301}], "note": "刷新后商品仍在"}
```

#### `PATCH /agent/{sid}/discovery/{kind}/{node_id}`

kind ∈ `state|feature|data`。修订接口:

```json
{"op": "update", "fields": {"description": "...", "confidence": 0.7}}
{"op": "merge", "into": "feature_add_to_cart"}
{"op": "delete", "reason": "..."}
```

#### `POST /agent/{sid}/finalize`

结束会话,生成全部最终产物。→ `{"topology_path": "...", "summary": {...}}`

## 2. Evidence 校验规则 (Controller 强制)

- `visual_evidence` 中每个 frame_id 必须 ≤ 当前最大 frame_id。
- evidence 条目中的 `step` 必须存在于 actions.jsonl 且 `accepted=true`;
  `before_frame`/`after_frame` 必须存在。
- 文本字段命中泄漏正则 → 422 `leak_detected`:
  `(?i)(/api/|\.tsx?\b|\.jsx\b|localhost|127\.0\.0\.1|\bSELECT\b|\bINSERT\b|React|Vue|Angular|DOM|selector|xpath|css\s*selector|sourcemap|graphql)`

## 3. 操作员/Dashboard API (`/api/...`)

不对 Agent 暴露(语义上属于 operator;本机模式同端口,容器模式仅 controller 可达)。

| 路由 | 说明 |
|---|---|
| `GET /` | Dashboard SPA (静态) |
| `POST /api/sessions` | `{app_id, seed?, budget?{max_actions,max_duration_s,max_observations}}` → `{session_id}` |
| `GET /api/sessions` | 会话列表 |
| `GET /api/sessions/{sid}` | 状态: step/elapsed/budget 余量/counts{states,features,edges,hypotheses,unresolved}/last_action/current_frame/status |
| `POST /api/sessions/{sid}/reset` | 环境回到 S0(重播种 DB+重启浏览器),trace 记录 reset 事件 |
| `POST /api/sessions/{sid}/close` | 终止并收尾(视频、metrics) |
| `POST /api/sessions/{sid}/replay?mode=visual\|deterministic` | deterministic: reset 后重放 action 序列,对比末帧,产出 replay 报告 |
| `GET /api/sessions/{sid}/frames/{frame_id}.png` | 原始帧 PNG |
| `GET /api/sessions/{sid}/live.png` | 最新帧 + 叠加(光标/最近点击标记),Dashboard 轮询用 |
| `GET /api/sessions/{sid}/trace` | actions+observations 合并 JSON 数组 |
| `GET /api/sessions/{sid}/topology` | 当前图 JSON |
| `GET /api/sessions/{sid}/hypotheses` | 假设列表 |
| `GET /api/sessions/{sid}/metrics` | 指标 JSON |
| `GET /api/apps` | 可用 reference app 列表(id/描述) |

**不存在**任何暴露 ground truth、App 内部状态、URL、DOM 的路由。

## 4. Sample App 内部网关约定 (reference 侧)

- App 进程仅监听 `127.0.0.1`,且必须拒绝不含 `X-BBB-Gateway: <secret>` 的请求 (404)。
- Controller 内置网关: `http://reference-app.internal:<gw_port>` → 转发至 app 端口并注入头;
  响应删除 `Server`/`X-Powered-By` 头。
- 浏览器以 `--host-resolver-rules="MAP reference-app.internal 127.0.0.1, MAP * ~NOTFOUND"` 启动。
- secret 存于 Controller 内存,不落盘到 agent 可读位置。

## 5. Session 状态机

```text
created → running ⇄ (budget_exhausted | agent_finalized | operator_closed) → closed
        → failed (runtime crash; 写 crash_report.json; 可 reset 重试)
```

## 6. 预算语义

- `max_actions`: 每次 accepted action 扣 1 (含 wait)。
- `max_duration_s`: 从 session 进入 running 起算。
- `max_observations`: 每次 observe 扣 1。
- 任一耗尽 → 后续 observe/action 返回 403 `budget_exhausted`;
  discovery 写入与 finalize 仍允许(让 Agent 能收尾)。
