# 实验记录矩阵（Experiment Results Matrix）

> 维护规则：探索生成闭环（accepted）后登记表一；**astra xhigh** 口径测评
> 出报告后登记表二。每完成一项就更新本文件，保持与 `app_output/` 现状一致。
> 成果口径见 `docs/evaluation_contract.md` §1.1（评测模型唯一性铁律）。

更新日期：2026-09-22

## 表一 · 探索生成（✅ accepted / 🔄 进行中）

| 目标 ＼ 模型 | kimi-k3 | glm-5.3 | gpt-5.6-sol | deepseek-v4.1f |
|---|---|---|---|---|
| **minesweeper** | ✅ 92 帧 / 31 发现<br>`…80590d-39b91c` | ✅ 161 帧 / 62 发现<br>`…82881d-f8e5e4` | ✅ 133 帧 / 48 发现<br>`…64f39c-d36567` | ✅ 255 帧 / 34 发现<br>`…155124-eb8c44` |
| **tasks** | ✅ 350 帧 / 85 发现 / 70 节点<br>`…ee4c7c-1ba715`（操作员收尾）| ✅ 225 帧 / 50 发现<br>`…58da64-e17944`（原生 MCP 首航）| ✅ 184 帧 / 47 发现<br>`…490613-ed0b49` | ✅ 205 帧 / 5 发现<br>`…879f27-a24c59`（2 轮）|
| **nonogram** | ✅ 315 帧 / 72 发现<br>`…ede5ab-9bca9b` | ✅ 157 帧 / 60 发现 / 38 节点<br>`…aba4d6-5fc7e9`（2 轮）| ✅ 200 帧 / 70 发现<br>`…b4334a-cc67cf` | ✅ 224 帧 / 63 发现<br>`…68d93b-6ecd03`（4 轮）|
| **fossify_gallery** | ✅ 254 帧 / 82 发现（空库）<br>`…8570dd-b110dd` | — | ✅ 185 帧 / 81 发现（种子）<br>`…eef0b1-68ece5` | ✅ 441 帧 / 56 发现（种子）<br>`…66a402-063663` |
| **vinyl** | ✅ 221 帧 / 62 发现（种子）<br>`…db553c-53be34` | ✅ 203 帧 / 77 发现 / 60 节点<br>`…9698ad-16755e`（1 轮）| ✅ 246 帧 / 107 发现 / 99 节点<br>`…da67bc-8a7f36` | ✅ 373 帧 / 72 发现 / 64 节点<br>`…4a8a3f-88bd6c` |
| **markor** | ✅ 232 帧 / 39 发现 / 23 功能<br>`…3d3085-ddf0b0`（4 轮）| — | ✅ 256 帧 / 49 节点 / 27 边<br>`…8129d3-975842`（1 轮）| ✅ 247 帧 / 47 发现 / 36 节点<br>`…6aaa96-d930e8`（2 轮）|
| **闭环小计** | **6** | **4** | **6** | **6** |

> 产物完整 id 形如 `sess_2026MMDD_HHMMSS_<hash>`，表内截尾；产物均含 accepted
> 的 `review_summary.json` 与 APK（22/22 全量核对通过）。
> v4.1f 六目标（minesweeper 重跑 / tasks / nonogram / gallery / vinyl / markor）于
> 09-21 夜至 09-22 夜陆续闭环；kimi × markor（4 轮自审）于 09-22 上午闭环；
> glm × vinyl（zcode，1 轮）与 v4.1f × markor（dsh，2 轮，1 小时高效探索）于
> 09-22 夜闭环；glm × nonogram（zcode，2 轮，补磁盘危机损失的一单）于 09-23 下午
> 闭环——四模型 nonogram 对照就此齐整。
> v4.1f 的 tasks 探索记录稀疏（5 发现，对照 kimi 85 / sol 47）但复现功能丰富
> ——探索记录完整性与复现质量的脱钩，可对照 astra 成绩观察。
> librera（kimi）探索未闭环，待择机重跑。

## 表二 · Astra 测评（full / partial / 占位 / 失效）

| 目标 ＼ 模型 | kimi-k3 | glm-5.3 | gpt-5.6-sol | deepseek-v4.1f |
|---|---|---|---|---|
| **minesweeper** | **17** / 2 / 3 / 0 | 13 / 6 / 0 / **3** | 16 / 3 / 3 / 0 | **18** / 1 / 3 / 0 |
| **tasks** | 6 / 11 / 3 / 0 | 2 / 10 / 2 / **6** | 3 / 9 / 7 / **1** | 3 / 8 / 0 / **9** |
| **nonogram** | 10 / 7 / 0 / 0 | — | 8 / 9 / 0 / 0 | **10** / 7 / 0 / 0 |
| **fossify_gallery** | 6 / 13 / 2 / **1** | — | 0 / 6 / 15 / **1**（种子版）| 5 / 14 / 1 / 2 |
| **vinyl** | 3/15/4/0 与 4/14/4/0<br>（judge 方差，差 1 项）| — | 3 / 11 / 8 / 0 | 2 / 13 / 3 / **4** |
| **markor** | 2 / 13 / 8 / **3** | — | 1 / 12 / 12 / **1** | — |
| **完成小计** | **6 单（7 份报告）** | **2 单** | **6 单** | **5 单** |

> judge 全局唯一（`gpt-6-astra` + `xhigh`，经 codex exec，cwd=`/storage/dzj/review`）。
> minesweeper 全模型对照完成：**v4.1f 18/1/3/0 为四模型最佳**（占位 3、失效 0）。
> glm × minesweeper 的 3 项失效均为整功能缺失（Safe first tap ×2 + Fog of War）；
> glm × tasks 6 项失效、v4.1f × tasks 9 项失效——失效集中于 tasks 目标。
> sol × gallery（0/6/15/1）与 sol × markor（1/12/12/1）：占位为主的失败形态
> ——探索质量优秀但复现产物空壳率偏高，值得单独复盘。
> v4.1f × vinyl（2/13/3/4）：vinyl 目标上四家中最弱（4 项失效），失败形态与
> kimi/sol 的"低完整、零失效"不同。
> markor 目标两家同形态：kimi（2/13/8/3）与 sol（1/12/12/1）——低完整、高占位，
> 复现产物空壳率偏高。

### 待评测队列

1. v4.1f × markor、glm × vinyl、glm × nonogram —— ✅ 探索已闭环，待启动测评
2. （未来）librera 重跑后的首批测评

> 09-21 磁盘危机处置：glm × nonogram（复现中断）、kimi × librera（复现中断）两条会话
> 已按操作员指令清理作废；同日已根治中间缓存写盘路径（迁至 `/storage/dzj/tmp`，
> 见 `docs/evaluation_contract.md` §1.2）。

## 表三 · 新平台（de-policy 生效后，2026-09-27）
> **平台版本自本表起成为对照维度**：2026-09-26 15:29 CST 控制器重启并首次加载
> 去策略化代码（移除运行时前台守卫与自愈重启、去掉 retry 建议、去掉会话存亡 triage；
> 详见当日处置记录）。表一 / 表二 的全部历史成绩均出自**旧平台**，不得与本表混用。
> 数据源：`app_output/sess_20260926_*` 与 `runs/sess_20260926_*/session_summary.json`。

| 目标 | 模型（CLI） | 步 | 帧 | 探索用时 | 抖动 | 审查 | APK | handoff |
|---|---|---|---|---|---|---|---|---|
| vinyl | deepseek-v4.1f（dsh） | 222 | 410 | 27 min | 0 | ✅ 2 轮 | 10.3 MB | `sess_20260926_072941_7f9323-7ebfbf` |
| fossify_gallery | deepseek-v4.1f（dsh） | 196 | 373 | 32 min | 0 | ✅ 3 轮 | 9.6 MB | `sess_20260926_072942_213ea2-828533` |
| tasks | deepseek-v4.1f（dsh） | 410 | 673 | 51 min | 0 | ✅ 4 轮 | 9.7 MB | `sess_20260926_072942_6915bf-9bd6ce` |
| minesweeper | deepseek-v4.1f（dsh） | 113 | 189 | 19 min | 0 | ✅ 3 轮 | 9.8 MB | `sess_20260926_202603_5a84eb-e0ec87` |

| nonogram | deepseek-v4.1f（dsh） | 187 | 296 | 22 min | 0 | ✅ 3 轮（2 修订） | 9.8 MB | `sess_20260927_135453_85db84-73d1d2` |
| minesweeper（**nograph 条件**） | deepseek-v4.1f（dsh） | 93 | 160 | 17 min | 0 | ✅ 2 轮 | 9.8 MB | `sess_20260927_141055_3a88b3-eab2b6` |

> 2026-09-27 夜补记：
> · nonogram 的新平台单：187 步 / 296 帧 / 22 min，审查 3 轮 2 修订，**抖动 0、平台侧错误 0**
>   （错误 14 条全是 agent 自己的 record_* 422）；各轮帧数 [20, 17, 14] —— 验证了「预热接管后
>   按当前构建重装 APK」的修复 ✓（此前同类单首轮只有 2 帧的脚手架画面）。
> · minesweeper 的 **nograph 条件**（消融：不要求生成拓扑）首条新平台记录：93 步 / 160 帧 /
>   17 min，审查 2 轮 accept，**抖动 0、平台侧错误 0、拓扑 0KB**（消融如预期不产图）；
>   该条件与新 baseline 的唯一差异 = 移除 7 个记录类工具（record_* / resolve_hypothesis / revise），
>   平台层与 MCP 稳定性能力（预热 / 静默续接 / 状态落盘 / 审计）已同步，skill 描述已中性化
>   （不含"消融/变体/退化"字样），并用专用 profile（headless-nograph）保证条件隔离。

> 抖动 = `actions.jsonl` 的 `environment_blip` 次数（**跨平台版本可比** ✓）；新平台四单全为 0 ✓，
> 对照旧平台同目标（vinyl / fossify_gallery 各 34 ✗）。
> **astra 测评：四单均未测评 ✗** —— 判分模型 `gpt-6-astra` 受账号 Pro 过期所限（400
> `not supported when using Codex with a ChatGPT account`），续费后按 §1.1 契约补测。
> 旧平台 v4.1f 已有 5 个目标的 astra 报告（minesweeper 18/1/3/0、nonogram 10/7/0/0、
> fossify_gallery 5/14/1/2、vinyl 2/13/3/4、tasks 3/8/0/9）；markor 与 09-25 之后的重跑
> 尚未测评，同样待补。
