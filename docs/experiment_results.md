# 实验记录矩阵（Experiment Results Matrix）

> 维护规则：探索生成闭环（accepted）后登记表一；**astra xhigh** 口径测评
> 出报告后登记表二。每完成一项就更新本文件，保持与 `app_output/` 现状一致。
> 成果口径见 `docs/evaluation_contract.md` §1.1（评测模型唯一性铁律）。

更新日期：2026-09-22

## 表一 · 探索生成（✅ accepted / 🔄 进行中）

| 目标 ＼ 模型 | kimi-k3 | glm-5.3 | gpt-5.6-sol | deepseek-v4.1f |
|---|---|---|---|---|
| **minesweeper** | ✅ 92 帧 / 31 发现<br>`…80590d-39b91c` | ✅ 161 帧 / 62 发现<br>`…82881d-f8e5e4` | ✅ 133 帧 / 48 发现<br>`…64f39c-d36567` | 🔄 重跑中 |
| **tasks** | ✅ 350 帧 / 85 发现 / 70 节点<br>`…ee4c7c-1ba715`（操作员收尾）| ✅ 225 帧 / 50 发现<br>`…58da64-e17944`（原生 MCP 首航）| ✅ 184 帧 / 47 发现<br>`…490613-ed0b49` | ✅ 205 帧 / 5 发现<br>`…879f27-a24c59`（2 轮）|
| **nonogram** | ✅ 315 帧 / 72 发现<br>`…ede5ab-9bca9b` | — | ✅ 200 帧 / 70 发现<br>`…b4334a-cc67cf` | ✅ 224 帧 / 63 发现<br>`…68d93b-6ecd03`（4 轮）|
| **fossify_gallery** | ✅ 254 帧 / 82 发现（空库）<br>`…8570dd-b110dd` | — | ✅ 185 帧 / 81 发现（种子）<br>`…eef0b1-68ece5` | 🔄 探索中 |
| **vinyl** | ✅ 221 帧 / 62 发现（种子）<br>`…db553c-53be34` | — | ✅ 246 帧 / 107 发现 / 99 节点<br>`…da67bc-8a7f36` | 🔄 探索中 |
| **markor** | — | — | ✅ 256 帧 / 49 节点 27 边<br>`…8129d3-975842`（1 轮）| — |
| **闭环小计** | **5** | **2** | **6** | **2**（+3 在途）|

> 产物完整 id 形如 `sess_2026MMDD_HHMMSS_<hash>`，表内截尾；产物均含 accepted
> 的 `review_summary.json` 与 APK（09-22 00:10 全量核对 15/15 通过）。
> v4.1f 的 tasks 探索记录稀疏（5 发现，对照 kimi 85 / sol 47）但复现功能丰富
> ——探索记录完整性与复现质量的脱钩，值得对照 astra 成绩观察。
> 进行中五单：v4.1f × minesweeper / fossify_gallery / vinyl（幂等化修复后代码
> 首航，凑齐五目标）；glm × nonogram（zcode，补磁盘危机损失的一单）；k3 ×
> markor（kimi，5 分钟延迟启动）。

## 表二 · Astra 测评（full / partial / 占位 / 失效）

| 目标 ＼ 模型 | kimi-k3 | glm-5.3 | gpt-5.6-sol | deepseek-v4.1f |
|---|---|---|---|---|
| **minesweeper** | **17** / 2 / 3 / 0 | 13 / 6 / 0 / **3** | 16 / 3 / 3 / 0 | ⏳ 产物重跑中 |
| **tasks** | 6 / 11 / 3 / 0 | 2 / 10 / 2 / **6** | 3 / 9 / 7 / **1** | 3 / 8 / 0 / **9** |
| **nonogram** | 10 / 7 / 0 / 0 | — | 8 / 9 / 0 / 0 | **10 / 7 / 0 / 0** |
| **fossify_gallery** | 6 / 13 / 2 / **1** | — | 0 / 6 / 15 / **1**（种子版）| 🔄 探索中 |
| **vinyl** | 3/15/4/0 与 4/14/4/0<br>（judge 方差，差 1 项）| — | 3 / 11 / 8 / 0 | 🔄 复现中 |
| **markor** | — | — | 1 / 12 / 12 / **1** | — |
| **完成小计** | **5 单（6 份报告）** | **2 单** | **6 单** | **2 单** |

> judge 全局唯一（`gpt-6-astra` + `xhigh`，经 codex exec，cwd=`/storage/dzj/review`）。
> kimi × fossify_gallery 为第四次尝试的首份有效成绩（`eval_eaa2e658a3`）；
> glm × minesweeper 的 3 项失效均为整功能缺失（Safe first tap ×2 + Fog of War），
> 失败形态与 kimi/sol（占位多、无失效）不同；glm × tasks 6 项失效。

### 待评测队列

1. deepseek-v4.1f × minesweeper / fossify_gallery / vinyl —— 🔄 复现中，闭环后入列
2. glm × nonogram、k3 × markor —— 🔄 探索中，闭环后入列

> 09-21 磁盘危机处置：glm × nonogram（复现中断）、kimi × librera（复现中断）两条会话
> 已按操作员指令清理作废，将择机重跑；同日已根治中间缓存写盘路径（迁至
> `/storage/dzj/tmp`，见 `docs/evaluation_contract.md` §1.2）。
