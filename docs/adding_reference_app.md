# 接入新的 Reference App

> 本文规定一个新的 reference app 入选 BlackBoxBench 必须满足的约定。
> 目标读者: app 作者与 benchmark 维护者。
> 安全动机逐条对应 `docs/security_model.md` 的威胁编号 (T1–T10)。
> 注意: 本文描述的容器链路未在编写机上 build/运行验证 (无 Docker),
> 首次在有 Docker 的机器上启用时请先跑 §9 的验收清单。

## 1. 目录约定

```text
sample_apps/<app_id>/
  __init__.py             # (或等效包结构) 使 python -m 可定位
  app.py                  # 入口: python -m sample_apps.<app_id>.app --port <N> --data-dir <D>
  seed/                   #  pristine 数据目录 (见 §3)
  ground_truth.json       #  标准答案拓扑 (见 §7) — 永不进 agent 镜像
  README.md               #  建议: 一页说明 (业务域/功能面/seed 内容)
```

- `<app_id>` 全小写 snake_case,同时是 controller `POST /api/sessions` 的 `app_id`
  与 compose 中 reference 服务的 `BBB_APP_ID`。
- 入口必须恰好接受 `--port` 与 `--data-dir` 两个参数,监听 **127.0.0.1 only**。
- 技术栈不限,但容器镜像默认只装了 FastAPI/uvicorn/pydantic/jinja2/Pillow
  (`docker/Dockerfile.reference`);引入新依赖需同步修改该 Dockerfile 的 pip 层。

## 2. 网关头约定 (T3)

- App 必须拒绝一切不带 `X-BBB-Gateway: <secret>` 的请求,返回 **404**(不是 401/403
  —— 不暴露"存在但无权"这一信息)。
- secret 从环境变量 `BBB_GATEWAY_SECRET` 读取;代码中允许保留仅开发用的默认值,
  容器部署由 compose 注入(`BBB_GATEWAY_SECRET` env)。
- App 只监听 `127.0.0.1:<port>`;对外暴露一律经由 reference 容器内的回环网关
  (`docker/internal_gateway.py`, 127.0.0.1:8200 → app,自动注入网关头并剥除
  `Server`/`X-Powered-By` 响应头)。
- 浏览器侧以 `--host-resolver-rules` 锁定只能解析 `reference-app.internal`;
  App 内部所有链接/表单/重定向都必须使用相对路径或该内部域名,**不得**出现
  任何真实外部 URL(既防逃逸,也防像素泄漏)。

## 3. Seed 要求 (T8)

- `seed/` 是 app `--data-dir` 的 pristine 母本:reference 容器启动与每次 `/reset`
  时,`docker/reference_entrypoint.sh` / `docker/runtime_rpc_server.py` 执行
  `rm -rf <live> && cp -a <seed> <live>`(默认 live 目录为容器内 `/data/live`,
  可用 `BBB_DATA_SEED`/`BBB_DATA_LIVE` 覆盖)。
- seed 内容必须**完全确定**:固定 id、固定时间戳(或干脆不含时间)、固定顺序;
  生成脚本若用随机数必须固定随机种子。
- seed 数据全部虚构(见 §5),规模以"首屏信息足够丰富但可一屏看完"为宜。

## 4. Reset 契约(对 app 实现的硬性要求)

容器模式的 `/reset` **不重启 app 进程**,只替换其数据目录并冷启动浏览器。
因此 app 必须:

- 以 `--data-dir` 为唯一事实来源;**不得**在进程内存中缓存业务数据
  (连接句柄可按请求打开/缓存失效容忍目录整体替换;SQLite 请按请求打开)。
- 不得在 `data-dir` 之外写任何可变状态(临时文件也不允许携带业务状态)。
- 会话/购物车等持久化状态必须落在 data-dir 内,使"换目录 = 回到 S0"成立。

(本机模式的 controller 会整体重启 app 子进程,无此约束;为两种模式一致,
新 app 一律按容器契约实现。)

## 5. 虚构品牌与去标识 (T5/T9)

- 品牌、公司、人名、商品、地名、域名全部虚构;不使用任何真实开源项目/知名
  网站的名称与素材(防止 Agent 从训练记忆"背出"功能)。
- 页面渲染产物中**禁止出现**: 真实 URL、端口号、`localhost`、仓库/文件路径、
  框架 debug 工具条、异常堆栈、版本页脚。错误页使用通用 404/500。
- App 应为 server-rendered(行为全部经由导航/表单可见,见 architecture §9),
  不依赖不可见的 JS 内部状态驱动核心行为。

## 6. 确定性要求

- 同一 seed 下,首屏与任一固定操作序列后的画面必须像素级稳定
  (deterministic replay 用 phash 汉明距 ≤ 6、末帧 MSE ≤ 0.001 判定,
  见 `benchmark/config.py`)。
- 禁止: 渲染 wall-clock 时间/日期、随机推荐、A/B 抖动、外部字体/图片/CDN。
  动画应短暂可 settle(settle 检测阈值: 60ms 轮询, 2000ms 超时)。
- 若确实需要时间展示(如"订单创建时间"),固定为 seed 中的逻辑时间。

## 7. ground_truth.json 编写 (T7)

- 结构与 `docs/topology_schema.md` 的 `functional_topology.json` 对齐:
  `nodes[]` (STATE/FEATURE/DATA) + `edges[]`,节点字段与
  `benchmark/topology/models.py` 一致;机器可读 schema 见
  `schemas/topology.schema.json`。
- State 由视觉证据语义定义(禁止用 URL 当 state);Feature 写可观察行为契约
  (preconditions/trigger/inputs/postconditions/persistent_effects/error_cases)。
- ground_truth 中的 `evidence` 字段可留空(它是参考答案,不引用会话帧),
  但 name/description 同样不得包含实现细节——评测器输出可能被 Agent 事后看到。
- 该文件只允许存在于仓库 `sample_apps/<app_id>/` 与 reference/controller 镜像内;
  **不得**被任何 HTTP 路由暴露,**不得**进入 agent 镜像
  (`docker/Dockerfile.agent` 不 COPY `sample_apps/`,新增 app 时不得破坏这一点)。

## 8. Docker 接入

无需新增 Dockerfile —— reference 镜像 COPY 整个 `sample_apps/`。接入步骤:

1. 按 §1 放好 `sample_apps/<app_id>/`。
2. `docker compose up --build`(reference 镜像重建后包含新 app)。
3. 以环境变量选择 app: `BBB_APP_ID=<app_id> docker compose up -d reference`
   (compose 默认 `ecommerce_demo`)。
4. Controller 的 `GET /api/apps` 从 `sample_apps/` 元数据列出可选 app;
   建会话: `POST /api/sessions {"app_id": "<app_id>"}`。

多 app 并存于同一镜像,`BBB_APP_ID` 只决定 entrypoint 启动哪一个以及从哪个
`seed/` 重建 live 数据。

## 9. 验收 checklist

接入 PR 必须逐项勾选:

- [ ] `python -m sample_apps.<app_id>.app --port 8100 --data-dir <tmp>` 本机可起,
      仅监听 127.0.0.1
- [ ] 无 `X-BBB-Gateway` 头 → 一律 404;带头 → 正常
- [ ] 首屏截图人工抽查: 无 URL/端口/路径/框架标识泄漏 (T5)
- [ ] 全部链接/表单/重定向为相对路径或 `reference-app.internal`
- [ ] `rm -rf live && cp -a seed live` 后 app 无需重启即回到 S0 (reset 契约)
- [ ] 固定操作序列两遍,末帧 phash 汉明距 ≤ 6 (determinism)
- [ ] seed 无真实个人/品牌数据;内容为虚构
- [ ] `ground_truth.json` 通过 `schemas/topology.schema.json` 校验;
      无 HTTP 路由可访问它 (T7)
- [ ] `docker compose up --build` 后 reference 容器 healthy,
      `curl http://localhost:8300/health`(容器内)返回 ok
- [ ] 跑一次完整 demo session + deterministic replay,末帧 match

## 10. Live Target(真实网站,例外通道)

上文 §1–§9 是 benchmark 级本地 app 的约定。把**真实线上网站**(如抖音网页版)
接为被测对象走另一条路——`AppSpec.kind="live"`,§2–§7 的网关/seed/reset/虚构
品牌要求整体不适用(也无法适用),降级保证见 `docs/security_model.md` §3。

### 10.0 临时目标(免注册)

探索一个**无需登录、或你愿意先手动登录一次**的网站,不必改任何代码:

- 会话 API: `POST /api/sessions {"live_url": "https://example.com/"}`。
- MCP/skill: `start_session(url=...)`;CLI: `run.py --cli kimi --url <URL>`。
- 行为: 自动派生 app_id(`live_<host_slug>`)、resolver 白名单(站点 host +
  bare 域名)与按站点持久化的 profile(`runs/live_targets/live_<host>/`)。
- 需要登录: `scripts/live_login.py --url <URL> --capture`(同一站点只需一次)。
- **已知限制**: 站点静态资源若在独立 CDN 域(非本站域名族,如
  douyinpic.com),派生白名单会拦截它们→页面渲染残缺。出现这种情况或需要
  precheck/定制 brief 时,按下文注册为正式 live target。

### 10.1 注册为正式 live target(以 douyin_web 为范例)

1. `benchmark/orchestrator/apps.py` 注册:
   `AppSpec(app_id=..., kind="live", live_url="https://.../", precheck="<key>",
   allowed_hosts=(...), seed="live", brief=...)`。brief 必须告知 agent 这是
   真实线上应用并划定只读边界(禁止发帖/评论/点赞等修改账号公开状态的操作)。
2. `allowed_hosts` 核对目标站点所需域名(主站 + 图床 + 视频 CDN),其余一律
   不解析;留空则自动派生(站点 host + bare 域名,见 §10.0 的限制)。
3. `benchmark/orchestrator/prechecks.py` 注册一个像素级 precheck(只用
   `runtime.screenshot()`,禁止语义通道),登录态/人工前置条件不满足时抛
   `PreflightError`。
4. 人工捕获登录态: `vendor/python/python.exe scripts/live_login.py --capture`
   ( headed 窗口手动登录 → Enter → 自动保存头像参考图与 golden profile 到
   `runs/live_targets/<app_id>/`)。之后 `--check` 可随时验证。
5. 建会话: `POST /api/sessions {"app_id": "<app_id>"}`;precheck 失败会先
   自愈(还原 golden profile)再拒绝启动。同一 live target 同时只允许一个
   running 会话(profile 是单例)。
6. CLI 评测: `agents/cli_explorer/run.py --cli kimi --app-id <app_id>`,或
   零命令入口新开对话 `/skill:blackbox-explorer <app_id>`(skill 内调
   `start_session` 选定目标;`install --app` 只决定缺省目标)。

live target 的已知限制(设计接受,非 bug):

- 无 S0 reset、无 determinism replay;`reset` 语义 = 冷启动浏览器回入口页。
- 登录 cookie 会过期/被风控,表现为建会话 503 并提示重新 `--capture`;
  扫码那一步永远需要人工。
- 探索 artifacts(截图/视频)包含真实账号信息,按敏感数据处理。
