# Website Reproduction Materials

复现阶段只使用两类互相分离的目录：

```text
reproduction/materials/   公共素材，只读
website_output/<handoff>/ Agent 生成网页，每次交接单独一份
```

Agent 使用该探索条件允许交接的输入和公共素材，在本次分配的
`website_output/<handoff_id>/` 中实现网页。托管探索的 `finalize` 和自建探索的
第一次 `finish_workspace` 都会立即创建该沙箱，不需要人工再启动一个任务。托管模式
交接 finalized Functional Topology；自建模式不要求或生成预设拓扑。

## 素材目录

先阅读 [materials/CATALOG.md](materials/CATALOG.md)。素材包括：

- 虚构用户、商品、订单、文章、动态、评论、私信和通知；
- 25 张 PNG、3 个 WAV 和 2 个 MP4；
- SQLite 初始数据库；
- 可选的 FastAPI 后端模板。

所有内容都是本地确定性生成的虚构数据。重新生成素材：

```powershell
vendor/python/python.exe -m reproduction.materials.build
```

## Agent 工作方式

复现容器显示以下内容：

1. `/materials/` —— 公共虚构素材，只读。
2. `/exploration/` —— 本次探索的安全白名单工件视图，只读。
3. `/workspace/` —— 映射本次 `website_output/<handoff_id>/`，可写。
4. `/input/functional_topology.json` —— 仅托管模式提供的定稿拓扑，只读；自建模式不存在。

`/exploration/manifest.json` 是工件清单。托管模式收录本次 Session 的可见 PNG 帧、
拓扑说明和覆盖报告；自建模式不规定产物，只中立收录 Agent 自发保存且后缀在安全
白名单内的文本、图片、音频或视频。它可以为空。自建探索工具源码、可执行文件和整个
工作区不会被交接。

约束只有一条关键原则：Agent 可以读取 `reproduction/materials/`，但所有新增、修改、
数据库运行态和构建产物都只能写入 `website_output/`。需要某个素材时复制过去使用，
不要原地修改公共素材。

权限由容器挂载直接限制。容器还使用 `network=none`、只读根文件系统、
non-root、capability drop、CPU/内存/进程限制；不接触 Reference App 或仓库源码。

## 输出目录

`website_output/` 是固定结果目录，已被 Git 忽略。Agent 可以自行选择技术栈和内部结构，
例如：

```text
website_output/<handoff_id>/
├── src/
├── public/
├── data/
├── backend/
├── tests/
└── README.md
```

如果需要初始数据库或后端，Agent 将 `materials/generated/library.db` 和
`materials/backend/` 复制到输出目录后再修改。这样购物车、发帖、订单等运行状态只会
改变网页结果，不会污染公共素材。

托管模式首次生成完成后，必须先启动像素级本地复验。复验浏览器只加载本次
`website_output/<handoff>/` 中的成品，只向 Agent 返回截图并接受坐标/键盘输入，不开放
DOM、selector、URL 或网络语义。Agent 可以选择 `revise`，修改后再启动下一轮；默认
最多修改 3 轮。只有一轮被 `accept` 后才能调用 `finish_reproduction`，最终摘要写入
`.blackboxbench/review_summary.json`。探索中的私人信息不得进入成品或复验摘要；所有
人物、账号、文章、消息、商品和媒体实例内容应来自公共虚构素材。

复现完成后，托管模式调用 `finish_reproduction`；自建模式第二次调用
`finish_workspace`。完成时容器会删除，交付文件保留在对应输出子目录。
