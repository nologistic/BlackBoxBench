# 评测契约（Evaluation Contract）

四个探索条件产出复现品，两个评测条件给它们打分。本文件规定**两平台共享的评测
语义**，使 web 与 app 的报告可以直接横向比较。平台差异只允许出现在动作面与运行时。

```text
4 exploration conditions                2 evaluation conditions
  web-baseline    ─┐                      web-review   (网页产物)
  web-our-method  ─┤─▶ 复现产物 ─────────▶
  app-baseline    ─┤                      app-review   (APK 产物)
  app-our-method  ─┘
```

## 1. 职责分界

评测由 **LLM judge**（以 Skill 形态运行在 Claude Code / Codex / OpenCode 等商用
Agent 中）和**薄校验层**共同完成：

| 归属 | 内容 |
|---|---|
| LLM judge（Skill） | 探查路径与深度、功能是否实现、四档给分、rationale 文本 |
| 校验层（代码） | 提供像素与坐标输入；校验证据引用、四档封闭、不漏项、报告 schema |

**校验层不判断语义**。它从不读源码，从不决定某个功能"能不能用"。它只保证 judge
一旦给分，就必须有据、说全、格式一致——这是四条件对照实验可比性的前提，而不是
对 judge 判断力的替代。

## 2. 评测立场

**评的是功能，不是代码，也不是像素还原度。**

- 按钮位置、文案、配色、布局与原目标不同，只要功能达成即判 `full`。
- 不要求交互路径一一对应；原 App 用底部导航、复现用抽屉菜单同样接受。
- 复现使用公共虚构素材，**素材内容不一致绝不作为降级理由**。
- 唯一依据：在可见屏幕上，该条要求描述的行为能否走通。

## 3. 四档词表（两平台共享）

定义于 `app_evaluation/grades.py`，web 端必须复用同一词表。

| 档位 | 中文 | 判定标准 |
|---|---|---|
| `full` | 完整 | 功能在可见行为上达成目标，含状态变化与必要的持久化 |
| `partial` | 部分 | 主要路径可用，但缺失分支、缺少校验或部分子流程不可用 |
| `placeholder` | 占位 | 界面存在但无真实行为：点击无变化、数据不落地、内容恒定 |
| `broken` | 失效 | 入口缺失、崩溃、报错或完全无法进入 |

封闭枚举。不得新增档位，不得使用"基本可用"这类自由措辞——否则跨条件统计失效。

## 4. 人工清单 schema（两平台共享）

`review_specs/*.json`，由 `app_evaluation/checklist.py` 加载校验。

```json
{
  "checklist_id": "android_commerce_demo",
  "platform": "android",
  "features": [
    {
      "id": "favorite",
      "name": "收藏与持久化",
      "steps": ["收藏商品", "重启 App"],
      "expected": "收藏状态在重启后保留",
      "persistence": true,
      "multi_user": false
    }
  ]
}
```

- `id` 小写短横线/下划线，清单内唯一；`name` 必填。
- `steps` / `expected` 是**给 judge 读的自然语言**，不构成机械断言。
- `platform` 取 `web` / `android` / `any`；加载时与运行平台校验。

两个机器可读标记，描述的是"单机黑盒能证明什么"，而非实现方式：

| 标记 | 含义 | 校验层行为 |
|---|---|---|
| `persistence` | 该功能需数据落地 | 判 `full` 时必须提交三段持久化证据 |
| `multi_user` | 涉及第二账号/外部访问者 | 必须填写 `not_verifiable_reason` |

### 4.1 排除边界 `exclusions`（可选，web 数据集）

与上述标记不同，`exclusions` 不是"怎么判"的元数据，而是**什么根本不在评分
范围**——操作员划定的排除面（多人协作、账户/支付、实时数据、AI 生成、外部
服务等刻意不复现的功能）：

```json
"exclusions": [
  {"feature": "实时协作", "treatment": "Hard Exclude",
   "reason": "需要多客户端、网络同步、presence、冲突解决。"}
]
```

- `treatment` 保留操作员原文（`Hard Exclude` / `Conditional Exclude` /
  `Default Exclude` / `Exclude` 等）；对 judge 的行为约束一致：**不探索、不评分**。
- 产物缺失排除面**不构成任何档位的降级理由**；恰好实现了也不加分。`rationale`
  无需逐条提及排除面，正常覆盖清单内条目即可。
- 校验层对 `exclusions` 采用与 `features` 相同的严格性：非数组、条目非对象、缺
  `feature` 直接报错——损坏的边界条目等于静默扩大评分范围，必须失败。
- 下发点：评测回执（`start_evaluation` / `evaluation_status`）与探索回执
  （`start_session` / `finalize`）自动携带；两侧读同一份
  `review_specs/<app_id>.json`，边界定义唯一。
- 清单内 `multi_user: true` 条目是"要验证、单机只能保守判"；`exclusions` 是
  "根本不在评分范围"——两者语义不同，不得混淆。

## 5. 证据规则

### 通用

每条判定必须引用 `evidence_observations`——**实际截取过的**观察编号。引用未捕获的
编号会被拒绝。`rationale` 必填，描述实际所见，不得写"应该""可能"。

### 持久化三段证据

`persistence: true` 的要求判 `full` 时必须提交：

```text
before_observation  → 写操作前
after_observation   → 写操作后（截图必须变化）
persisted_observation → 重启或退出重入后（不得退回 before 截图）
```

校验层做**结构**检查：区间内存在写类交互、截图确有变化、之后有重启或返回+重入
探针、persisted 不等于 before。**数据语义是否正确由 judge 判断并写入 rationale。**

若重启后变更消失，应判 `placeholder` 且不提交三段证据。

### 单机不可验证项

`multi_user: true` 的要求必须在 `not_verifiable_reason` 中区分：已确认部分与
无法确认部分，并据此给保守判定（通常 `partial`）。

## 6. 报告 schema

`schema_version: 1`，两平台字段一致，平台专属字段仅追加：

```json
{
  "schema_version": 1,
  "platform": "android",
  "run_id": "eval_…",
  "checklist_id": "…",
  "checklist_sha256": "…",
  "apk_sha256": "…",
  "counts": {"full": 6, "partial": 3, "placeholder": 1, "broken": 1},
  "requirements": [{
    "requirement_id": "…", "name": "…",
    "grade": "full", "grade_label": "完整",
    "rationale": "…",
    "evidence_observations": [8, 9, 12],
    "persistence_evidence": {"before_observation": 8, "after_observation": 9,
                             "persisted_observation": 12},
    "not_verifiable_reason": null
  }],
  "actions": [{"type": "tap"}],
  "observations": [{"number": 1, "action_count": 0, "sha256": "…", "path": "…"}]
}
```

`actions` **只记动作类型**，不记坐标与输入文本——避免私人输入进入报告。清单与
被测产物均带哈希，报告因此可追溯到确定的输入。

## 7. 隐私

探索期看到的真实账号、密码、文档、消息、订单不得进入 rationale、
`not_verifiable_reason` 或报告任何字段。评测容器/模拟器断网。

## 8. 平台实现

| | web-review | app-review |
|---|---|---|
| Skill | `agents/web_review/skill/web-review/` | `agents/app_review/skill/app-review/` |
| MCP | `agents/web_review/mcp_server.py` | `agents/app_review/mcp_server.py` |
| 会话 | `web_evaluation/session.py` | `app_evaluation/session.py` |
| 运行时 | loopback 静态服务 + 锁定像素浏览器（同一 CDP 白名单底座） | `AndroidEmulatorRuntime`（全新 AVD、断网） |
| 动作面 | click / type_text / scroll / key / reload / reset_browser / wait / read_source | tap / long_press / swipe / type_text / press_back / press_enter / restart_app / reset_app / wait |
| 被测物 | `website_output/<handoff>/`（须含 index.html） | `app_output/<handoff>/artifacts/app-debug.apk` |
| 持久化探针 | reload / reset_browser / BrowserBack 后重入 | restart_app / reset_app / press_back 后重入 |
| 报告 | `website_output/evaluations/<run_id>/evaluation_report.json` | `app_output/evaluations/<run_id>/evaluation_report.json` |

**平台差异：源码只读通道（仅 web）。** 网页交接目录天然以源码形态交付，web
judge 因此比 app judge 多一个 `read_source` 工具——只读、限交接目录内、限文本
源码、有大小上限。它是**静态侦察通道**（定位入口、路由与状态存放），不是判定
捷径：每条判定的证据仍然必须引用像素观察编号，源码与屏幕冲突时以屏幕为准。
app 产物是 APK，无源码可读，这一差异是交付形态决定的，不构成评审能力不对等。

共享 `app_evaluation/grades.py` 与 `app_evaluation/checklist.py`；两个评测条件
**不得互相 import 判定实现**（`web_evaluation` 不得 import `agents.app_review`，
反之亦然），与四个探索条件的隔离纪律一致。报告字段两平台一致；`apk_sha256`
为 android 专属、`handoff_sha256`（交接目录清单哈希）为 web 专属，其余字段
完全相同，可直接横向聚合。

## 9. 安装与使用

```powershell
vendor/python/python.exe -m agents.app_review.install --cli codex
vendor/python/python.exe -m agents.web_review.install --cli codex
# 新任务中： $app-review google_clock.json / $web-review yuque_web.json
```

正式评测时在客户端只启用 `app-review` 一个 MCP，禁用四个探索条件的 MCP，避免
judge 触及目标 App、ground truth 或探索通道。

## 11. 平台缺陷 vs Agent 能力

对照实验的结论只在一个前提下成立：**失败的归属判断正确**。把平台故障算成 Agent
能力不足，会凭空制造方法差异；把 Agent 的选择算成平台 bug 去"修"，会抹掉正要测量
的信号。两者必须用证据区分，不能凭印象。

### 判据

| | 平台/支撑不到位 | Agent 能力或方法差异 |
|---|---|---|
| 特征 | Agent 做了合法的事，却拿不到结果 | 平台正常服务，Agent 选择了不做或做得不好 |
| 检验 | 任何 Agent 在同条件下都会遇到 | 同平台下别的条件/会话做到了 |
| 处置 | **必须修**，并重跑受影响的会话 | **不得修**，如实计入结果 |

判定必须落在可复核的证据上：崩溃报告、被拒绝的调用记录、settle 原因、延迟曲线。
"看起来像"不构成判定。

### 已判定实例（google_clock，2026-08-31）

平台缺陷（已修）：

| 现象 | 判据 |
|---|---|
| `screencap` exit -1 报废 49/85 步探索 | 端口锁命名空间碎片，两会话共用 emulator-5554；与决策无关 |
| `am start` exit 1 报废 98 步探索 | force-stop 后窗口未拆完的时序问题 |
| `dumpsys` 超时终止会话 | 单次 adb 抖动被当作致命 |
| 中文 `type_text` 终止会话 | 设备 KeyCharacterMap 限制，任何 Agent 都会撞上 |
| settle 命中超时 57–63%，每场浪费 105–303s | 时钟持续重绘，"静止"条件永不满足 |
| 设备渐进劣化无记录（1.9s→4.9s，峰值 16s） | 崩溃前无任何痕迹，使渐进崩塌看似突发故障 |

Agent 能力/方法差异（**不修**）：

| 现象 | 判据 |
|---|---|
| android-baseline 定稿 0 条边（19 状态 / 10 功能） | 同平台 our-method 在语雀建了 52 条 state→state 边；web-baseline 同样 0 条。平台从未拒绝建边 |
| 转换边普遍偏少 | 这正是 our-method 相对 baseline 的改进目标，属被测量对象 |

已排除的怀疑（记录以免重复走弯路）：

- **节点 id 退化为 `state_unnamed_N`**：`_slug` 丢弃全部非 ASCII 字符。看似会阻断
  建边（无法引用状态），但语雀会话同样 47/85、95/110 退化，edges 仍达 52、48。
  故它是可用性缺陷，**不是** edges=0 的原因，也不解释条件差异。
- **31% 观察帧零视觉变化**：疑似 Agent 乱点。实测其中仅 0–3 帧紧随新动作，其余为
  同一 step 内的连续 observe。不构成任何一方的问题。

### 使判定可行的基础设施

这次判定一度卡住，因为平台只记录成功的 discovery 调用——"Agent 没尝试建边"与
"Agent 尝试了被平台拒绝"留下完全相同的痕迹。现已补齐：

- `discovery.jsonl` 记录被拒绝的调用（`accepted: false` + 原因 + 原请求），
  Agent 收到的错误不变，不构成新信息通道。
- `observations.jsonl` 记录 `settle_reason`（`still`/`animation`/`timeout`/`none`），
  可区分"目标真的慢"与"目标永不静止"。
- `actions.jsonl` 记录 `environment_degrading` 事件（持续 3 倍以上延迟漂移）。

判定失败归属前应先读这三处，而不是从截图反推。

## 12. 已废弃

`app_evaluation/evaluator.py` 与 `app_evaluation/cli.py` 的 `--plan` 批处理已废弃：
它要求 judge 在看到任何截图前声明完整坐标计划，无法自适应探查。两文件保留为带
迁移指引的垫片。
