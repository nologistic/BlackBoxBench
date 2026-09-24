# Functional Topology Graph Schema

## 1. 形式定义

`G = (V, E)`,其中 `V = States ∪ Features ∪ PersistentDataEntities`。

## 2. 节点类型

### STATE
UI 状态。**State ≠ Page ≠ URL**。同一页面可有多个状态(购物车空/非空/优惠券无效…)。
禁止以 URL 作为 state 定义;state 必须由视觉证据锚定。

```json
{
  "id": "state_cart_nonempty",
  "type": "STATE",
  "name": "Cart with items",
  "description": "...",
  "visual_evidence": [231],
  "entry_conditions": [],
  "observed_elements": [],
  "confidence": 0.92,
  "status": "confirmed",
  "created_step": 231,
  "updated_step": 240
}
```

### FEATURE
**可观察行为契约**,不是 UI 元素。"Add to Cart 按钮"是元素;
"加入商品会使购物车状态变更"才是 Feature。一个 Feature 可跨多页/多元素/多输入。

```json
{
  "id": "feature_add_to_cart",
  "type": "FEATURE",
  "name": "Add product to cart",
  "description": "...",
  "behavior": {
    "preconditions": ["state_product_detail"],
    "trigger": {"type": "click", "target_description": "Add to Cart 按钮"},
    "inputs": [{"name": "quantity", "kind": "number", "constraints": "1-99"}],
    "postconditions": ["cart count +1"],
    "persistent_effects": ["刷新后仍保留"],
    "constraints": [],
    "error_cases": ["数量为 0 时提示错误且不修改购物车"]
  },
  "evidence": [{"step": 147, "before_frame": 146, "action": "click(811,432)", "after_frame": 147}],
  "confidence": 0.95,
  "status": "confirmed"
}
```

### DATA
持久化数据实体(User session / Cart / Product / Order / Profile)。

```json
{"id": "data_cart", "type": "DATA", "name": "Cart", "description": "...", "confidence": 0.8}
```

## 3. 边类型

| type | 含义 | 例 |
|---|---|---|
| REQUIRES | 前置条件 | Checkout REQUIRES Authenticated |
| TRANSITIONS_TO | 状态迁移 | Login 成功 → Authenticated Home |
| MUTATES | 修改数据 | Add to Cart MUTATES Cart |
| ENABLES | 使能 | 填写地址 ENABLES 下单 |
| DISABLES | 禁用/互斥 | 空购物车 DISABLES Checkout |
| PERSISTS_TO | 持久化 | Cart PERSISTS_TO Refresh |
| REVEALS | 揭示新 UI | 点击账户 REVEALS 账户菜单 |
| DEPENDS_ON | 一般依赖 | |
| CONFLICTS_WITH | 冲突 | |
| VALIDATES | 校验关系 | Coupon 校验 VALIDATES 订单金额 |

边:

```json
{
  "id": "edge_0007",
  "source": "feature_checkout",
  "target": "state_authenticated",
  "type": "REQUIRES",
  "evidence": [{"step": 200, "before_frame": 199, "action": "click(...)", "after_frame": 200}],
  "confidence": 0.9,
  "status": "confirmed"
}
```

`status ∈ {hypothesized, confirmed, rejected, uncertain}` —— 未验证的猜测
必须保留为 `hypothesized` + 低 confidence,**不得**作为事实输出。

## 4. 顶层文件 `functional_topology.json`

```json
{
  "app_id": "ecommerce_demo",
  "graph_version": "1.0",
  "session_id": "sess_...",
  "generated_at": "...",
  "nodes": [ ...STATE/FEATURE/DATA... ],
  "edges": [ ... ],
  "unresolved_questions": [
    {"hypothesis_id": "hyp_0003", "statement": "...", "status": "uncertain", "confidence": 0.4}
  ],
  "coverage_summary": {
    "states": 18, "features": 11, "data_entities": 4, "edges": 24,
    "confirmed_ratio": 0.83, "actions_used": 312
  }
}
```

机器可读 schema 见 `schemas/topology.schema.json`。

## 5. 规则

1. **Evidence 强制**: 每个 node/edge 至少一条引用真实 frame/step 的 evidence
   (hypothesized 状态可空,但必须在 `unresolved_questions` 中登记)。
2. **Confidence**: 连续 [0,1]。
3. **无实现细节**: 文本字段不得出现源码路径/API 路由/框架名(Controller 机械拦截)。
4. **增量构建**: Explore → Update → Probe → Update → finalize,
   禁止"最后凭记忆一次性生成"。
5. **可修订**: update / merge / delete;全部改动留痕于 discovery.jsonl。
6. Canonical ID: `state_|feature_|data_` + slug(name);merge 由 Agent 显式发起,
   系统不做自动合并(防止替 Agent 做决定)。
