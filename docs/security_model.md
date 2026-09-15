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

| 数据 | 探索 Agent | Benchmark internal |
|---|---|---|
| 截图 PNG + 光标坐标 | ✅ | ✅ |
| action OS 回执 (accepted) | ✅ | ✅ |
| 预算余量 | ✅ | ✅ |
| 自己写的 discovery/topology | ✅ (读写) | ✅ |
| 真实 URL / 端口 | ✗ | ✅ |
| DOM / AXTree / 网络请求 | ✗ | ✅ (仅 runtime 内部,不落盘) |
| DB / seed 文件 | ✗ | ✅ |
| ground_truth.json | ✗ | ✅ (仅评测器) |
| 视觉 settle 差分 | ✗ | ✅ |

## 3. Live Target(真实网站)的例外与降级保证

`yuque_web` 等 `kind="live"` 的被测对象是**真实线上网站**,与本地 seeded app
有本质区别。它用于"对真实软件做功能探索"的场景,**不构成 benchmark 级隔离**,
评测结论(尤其 determinism / coverage 类指标)不可与本地 app 直接比较。

- **T3 网络**: 本地 app 仍锁死为只能解析内部域名。live target 为支持登录、
  多级重定向、站点 CDN 和跨域资源而保留正常公网访问；入口 URL 只是起点，
  **不是**域名或导航白名单。Agent 不得访问搜索引擎、代码托管站或网页归档来查找
  原始实现，也不得直接请求/下载目标页面、源码或接口响应；这些是任务 brief 的
  软约束，因此 live target 适用于守规 Agent 实验，不抵抗主动恶意 Agent。
- **T4 浏览器逃逸**: 本地 app 用 headless 无 chrome;live target 必须 headed
  (无头会被目标站点风控拦截)。缓解: CDP Input 只达渲染视口,地址栏/菜单
  不可达;action schema 无修饰键(Ctrl+L / Ctrl+Shift+I 不可表达)、无右键;
  **F12 在 Session 层拒绝**(`_LIVE_BLOCKED_KEYS`)。链接点击可能发生正常跨域跳转；
  是否仍围绕目标进行探索由任务 brief 与审计约束，而不是 DNS 拦截。
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

## 4. 自建工具探索条件

自建工具模式不复用上表中的 Agent API。它采用更低层、但同样是 pixels/HID-only 的
设备边界：

- **S1 托管实现泄漏**：`tool_builder` 的 build context 仅包含
  `self_explorer/agent_image/`；仓库、Controller、SDK、托管 MCP 与 Runtime RPC 不在
  Agent filesystem 中。reference 与 Agent 不共享 filesystem namespace。
- **S2 语义泄漏**：可信侧只产生 P6 像素设备文件并消费 Linux input-event FIFO；除任务
  明确给出的起始 URL 外，没有请求/响应、动作回执、元素、预算或目标内部信息。起始
  URL 不构成导航白名单，可信浏览器可随目标流程重定向或跨域。Agent 只获知目标浏览器
  已启动以及浏览器 I/O 根位置，不获得设备格式说明、输入协议、客户端、工具建议或
  探索工作流。URL 仅用于标识目标，不回传
  页面语义。
- **S3 网络/源码搜索**：内置 App 条件的两个容器都为 `network_mode: none`。显式公网
  URL 条件让 reference 与 tool_builder 普通出网；Chromium 强制经过绑定 loopback 的
  public-only proxy，启动器/代理继续阻止浏览器访问非 public 目标。tool_builder 不设
  URL 白名单，其网络仅可用于通用自建工具依赖；不得直接请求、搜索或下载目标内容的
  规则由 Skill 软约束，因此该条件用于守规 Agent 实验，不声称抵抗主动恶意 Agent。
- **S4 人类操作约束**：桥限制坐标、按键集合、动作/帧/时长预算及 HID 最小间隔；
  DevTools/浏览器 chrome 快捷键不可表达。
- **S5 登录凭据**：宿主浏览器 Profile 不跨操作系统复制。操作员通过独立
  `docker-compose.self-login.yml` 在 Linux Chromium 中建立 Docker-native golden
  Profile；该阶段不启动 Agent，只把像素/坐标界面发布到随机 host loopback 端口。
  探索阶段 golden Profile 仅只读挂载到 `reference_raw` 并复制进 tmpfs，
  `tool_builder`、Agent workspace、证据交接与复现容器均不可达。此设计保护 Profile
  文件边界，但 Agent 仍能通过可见网页使用登录会话，因此只读行为约束仍然必须执行。
- **S5 宿主逃逸**：MCP 文件路径限定在本次 workspace，命令以 argv 形式固定在
  `tool_builder` 执行；不挂 Docker socket，root filesystem 只读、non-root、drop all
  capabilities、no-new-privileges。
- **S6 无结构交接**：`begin_workspace` 缺少明确 `target_url` 时拒绝启动；首次
  `finish_workspace` 只执行越狱与完整性检测，不要求图像、报告、拓扑、证据、最少
  状态/功能或固定目录。常规第三方依赖根不做内容指纹扫描且不进入交接，防止上游包
  源码造成误报；Agent 自写内容仍扫描。是否探索充分属于本实验要观察的 Agent 原初行为。

仍有一个必须由运行方满足的前提：Claude Code、Codex、Kimi Code 等客户端可能自带
宿主 shell/文件/搜索工具。正式实验必须使用客户端 tool allowlist，只开启
`blackboxbench-self-built`。仅靠 Skill 提示 Agent 不使用这些工具不构成安全边界；不
支持 allowlist 的客户端只能用于开发试跑，不能用于严格对比。

## 5. 复现交接边界

复现是探索结束后的单向下游阶段，不反向扩大任一探索条件的观察通道：

- 容器只挂载公共虚构素材（只读）、本次探索的白名单工件视图（只读）、
  `website_output/<handoff_id>/`（可写），以及仅托管模式提供的 finalized topology。
- 工件包是新建的筛选视图并带独立 manifest，不直接挂载整个 `runs/` 或自建工作区；
  路径逃逸和符号链接会被拒绝。
- 不挂载仓库、Reference App 源码、ground truth、live profile、浏览器语义
  通道或另一探索条件的实现。
- `network=none`、只读根文件系统、non-root、drop all capabilities、
  no-new-privileges，并限制 CPU、内存和进程数。
- 需要运行或修改数据库/后端时必须先复制到可写工作区；交付时拒绝
  符号链接，防止绕过输出边界。

该边界由 `tests/test_reproduction.py` 验证，并通过真实 Docker 交接检查素材哈希不变。

## 5.1 Android 垂直链路

- 目标 APK、package/activity、golden AVD 和 protected values 仅存在于可信注册区与
  `AndroidEmulatorRuntime`；Agent 只得到中立 brief、PNG 和坐标动作回执。
- ADB 仅允许可信 Runtime 内部的安装/启动/截图/input/reset/前台围栏操作，不建立 Agent
  API。UIAutomator、Accessibility、selector、logcat、设备文件和反编译通道不存在。
- 每次任务克隆独立 AVD 并分配独立 serial/端口。探索 lease 可并行，登录维护使用排他
  lease，禁止与同目标探索重叠。
- 样例、生成 APK 与默认外部目标使用模拟器内 IPv4/IPv6 `OUTPUT DROP`，飞行模式只是
  叠加措施而不是硬边界。public 模式必须同时配置独立 network guard 和 public-only
  proxy；缺少任一项即拒绝启动。
- Android 交接总是复制到新 bundle，文字按 protected strings 替换，配置区域的截图被
  模糊；JSON topology 也从脱敏副本挂载。目标 APK、profile、原 App 私有数据均不交接。
- APK 构建容器只读挂载脱敏交接与虚构素材，只写 `app_output/.../project`，无网络。完成
  前检查危险权限、私人值、APK 存在性、像素复测和接受后工程指纹。

## 6. 结论

- 正式 Benchmark 必须使用 **容器部署**(compose 中 agent 位于仅通 controller 的网络)。
- 本机模式用于开发与 CI;其黑盒保证在"非主动对抗"前提下成立,
  所有可机械验证的边界均由 `tests/test_security.py` 覆盖。
- 自建工具条件的严格运行必须使用 Docker 隔离和客户端工具 allowlist；其静态边界由
  `tests/test_self_explorer.py` 覆盖。
