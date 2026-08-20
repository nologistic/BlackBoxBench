# 安全模型 (Security Model)

> 目标: Reference App 必须是真正的黑盒。本文件列出威胁模型与对应缓解措施,
> 并标注每条措施在 **容器模式 (C)** 与 **本机模式 (L)** 下的强度:
> ✅ 完全防御 / ◐ 缓解(防意外不防主动对抗) / 由测试 `tests/test_security.py` 验证的标记 [T]。

## 1. 威胁模型

### T1 源码泄漏 (Source leakage)
Agent 读取 App 源文件、Git 仓库、包元数据。
- **缓解**: App 源码只存在于 reference 侧(容器内 / 本机子进程工作目录);
  Agent 通道无任何文件读取接口;Agent SDK 无文件系统 API。[T]
- C: ✅ (文件系统 namespace 隔离) · L: ◐ (同主机,约定隔离;对抗性场景必须用容器)

### T2 DOM / AXTree 泄漏
Agent 通过 CDP、Playwright、`page.content()`、浏览器扩展拿到 DOM 或无障碍树。
- **缓解**: Runtime 驱动代码中**不存在** `Runtime.evaluate`、`DOM.*`、
  `Accessibility.*` 的调用能力面(白名单方法: 截图/输入/导航/复位)[T];
  CDP 端口随机、仅 loopback、不出现在任何 agent 可见工件中[T];
  SDK 无 `get_dom()/get_url()/find_element()` 等方法 [T];
  浏览器 `--remote-allow-origins` 仅 runtime 使用,headless 无 DevTools UI。
- C: ✅ · L: ◐ (同主机下恶意 Agent 可扫端口;容器模式为正式评测配置)

### T3 网络泄漏
Agent 直接请求 App 后端 API、抓包、访问互联网搜源码。
- **缓解**: App 只接受带 `X-BBB-Gateway` 内部头的请求,直连 404 [T];
  浏览器 `--host-resolver-rules` 仅放行 `reference-app.internal`,其余域名
  解析失败 [T];App 响应经网关 scrub (`Server` 头等)。
- C: ✅ (`internal: true` 网络,无默认路由出网) · L: ◐+ (浏览器侧已锁;
  Agent 进程本身联网需部署纪律,正式评测跑 agent-container)

### T4 DevTools / 浏览器逃逸
Agent 用 Ctrl+Shift+I、地址栏、设置页、`file://`、扩展、下载逃逸出 App。
- **缓解**: headless 无浏览器 chrome(无地址栏/无菜单); kiosk 模式 (C);
  `--disable-extensions --disable-component-update --no-first-run`;
  无下载触发面(server-rendered 无下载链接); 快捷键在无 chrome 时无效。
- C: ✅ · L: ✅ (headless 等价)

### T5 截图侧信道 (Metadata leakage via pixels)
截图中出现 `localhost:3000`、仓库路径、框架 debug overlay、报错堆栈、GitHub 链接。
- **缓解**: App 使用虚构品牌;生产模式渲染,无 debug 工具条;
  错误页为通用 404/500;浏览器以内部域名访问,且 headless 无地址栏可显示 URL。[T: 人工抽查 + determinism 测试锚定首屏]

### T6 容器逃逸 / 宿主访问
- C: 非 root 运行、`--cap-drop=ALL`、只读挂载必要目录、无 docker socket 挂载。
- L: 不适用(进程模型),文档化声明本机模式仅供开发。

### T7 Ground truth 泄漏
Agent 读取 `ground_truth.json` 影响探索策略。
- **缓解**: 该文件仅在仓库 `sample_apps/<app>/` 下,**无 HTTP 路由暴露** [T: 断言 404];
  不进入 agent 容器镜像;仅评测器(未来)读取。

### T8 会话间污染
前一 Session 的 DB/cookie/缓存影响后一个。
- **缓解**: `reset` = 删除并重建 seed 数据 + 整体重启浏览器(全新 user-data-dir)
  + 全新 App 子进程。[T: determinism 测试]

### T9 训练污染侧信道
App 若为知名开源项目,Agent 可能从训练记忆"背出"功能而非探索。
- **缓解**: Sample App 为本项目自写、虚构品牌;正式 Benchmark 应使用
  未公开或改头换面的 App;`adding_reference_app.md` 将此列为入选要求。

### T10 Action 回执泄漏语义
`{"clicked_element": "checkout_button"}` 之类的回执把语义告诉 Agent。
- **缓解**: action 响应固定为 `{"accepted": bool, "frame_id?": int}` [T: schema 白名单测试]。

## 2. 信息边界总表

| 数据 | Agent | Benchmark internal |
|---|---|---|
| 截图 PNG + 光标坐标 | ✅ | ✅ |
| action OS 回执 (accepted) | ✅ | ✅ |
| 预算余量 | ✅ | ✅ |
| 自己写的 discovery/topology | ✅ (读写) | ✅ |
| 真实 URL / 端口 | ✗ | ✅ |
| DOM / AXTree / 网络请求 | ✗ | ✅ (仅 runtime 内部,不落盘) |
| DB / seed 文件 | ✗ | ✅ |
| ground_truth.json | ✗ | ✅ (仅评测器) |
| 视觉差分 / loop 检测遥测 | ✗ | ✅ |
| 会话最终 metrics | ✗ (运行中) | ✅ (结束后可披露给研究者) |

## 3. Live Target(真实网站)的例外与降级保证

`douyin_web` 等 `kind="live"` 的被测对象是**真实线上网站**,与本地 seeded app
有本质区别。它用于"对真实软件做功能探索"的场景,**不构成 benchmark 级隔离**,
评测结论(尤其 determinism / coverage 类指标)不可与本地 app 直接比较。

- **T3 网络**: 本地 app 锁死仅解析内部域名;live target 降级为**域名白名单**
  —— `--host-resolver-rules` 只 `EXCLUDE` 目标站点自身域名
  (douyin.com/douyinpic.com/douyinvod.com 等),其余一律 `~NOTFOUND`。
  互联网出网是 live target 的固有前提。
- **T4 浏览器逃逸**: 本地 app 用 headless 无 chrome;live target 必须 headed
  (无头会被目标站点风控拦截)。缓解: CDP Input 只达渲染视口,地址栏/菜单
  不可达;action schema 无修饰键(Ctrl+L / Ctrl+Shift+I 不可表达)、无右键;
  **F12 在 Session 层拒绝**(`_LIVE_BLOCKED_KEYS`);配合域名白名单,逃逸面
  收敛到"在目标站点内导航"。
- **T8 reset**: 本地 app 删数据目录重建 S0;live target **无 S0** —— reset 仅
  冷启动浏览器回到入口 URL,服务端状态不可控,会话间可能污染(点赞/历史等)。
  manager 强制同一 live target 同时只允许一个 running 会话。
- **T9 训练污染**: 本地 app 虚构品牌自写;live target 是知名站点,模型训练
  记忆可"背出"功能——探索链路验证有效,能力评估结论需打折。
- **登录态**: 人工维护。`scripts/live_login.py --capture` 捕获 golden
  profile + 头像参考图;每次建会话跑**像素级 precheck**(头像模板滑窗匹配,
  阈值 0.15,实测间隔 0.01/0.93),失败先自愈(还原 golden)再报错拒绝启动。
  **凭据安全**: profile 内含登录 cookie,仅存于 `runs/live_targets/<app>/`,
  不暴露任何 HTTP 路由,不得外传。

不变的底线: 即使对 live target,Agent 通道仍是像素 + HID——CDP 白名单、
action 回执白名单、discovery evidence 校验全部原样适用。

## 4. 结论

- 正式 Benchmark 必须使用 **容器部署**(compose 中 agent 位于仅通 controller 的网络)。
- 本机模式用于开发与 CI;其黑盒保证在"非主动对抗"前提下成立,
  所有可机械验证的边界均由 `tests/test_security.py` 覆盖。
