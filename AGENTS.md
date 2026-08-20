# AGENTS.md — BlackBoxBench

## 项目性质
黑盒 App 理解能力 Benchmark 基础设施。Agent 只能通过像素截图 + 坐标级 HID 输入
探索 Reference App,产出带证据的 Functional Topology Graph。

## 不可违反的核心约束
- `benchmark/runtime/` 是唯一允许接触浏览器的代码;CDP 白名单仅
  Page.navigate/getNavigationHistory/navigateToHistoryEntry/captureScreenshot/
  close + Input.* + Emulation.setDeviceMetricsOverride + Browser.close。
  **禁止**添加 Runtime.evaluate / DOM.* / Accessibility.* / Network.* 等任何
  语义通道。多标签页跟随(`_follow_latest`,target=_blank 跳转向新内容页)
  只经 /json/list 发现目标,不向 agent 暴露任何信息。
- `/agent/{sid}/` 路由响应保持白名单字段(screenshot/cursor/brief/budget/
  tabs{count,active}/accepted/frame_id/step);action 回执不得包含语义结果。
- discovery 写入必须通过 evidence 校验(真实 frame/step)与泄漏正则
  (`benchmark/topology/models.py::LEAK_PATTERN`)。
- `sample_apps/*/ground_truth.json` 永远不得通过 HTTP 暴露或进入 agent 镜像。

## 常用命令(Windows;Linux/macOS 换 vendor/python/bin/python3)
```bash
vendor/python/python.exe -m benchmark.server --port 7800   # controller+dashboard
vendor/python/python.exe -m demo_agent.run_demo            # demo 探索(需先起 server)
vendor/python/python.exe -m pytest tests/ -q               # 测试(约 2-3 分钟)
vendor/python/python.exe scripts/e2e_smoke.py              # 最小链路冒烟
```

## 结构
`benchmark/`(orchestrator·runtime·recorder·topology·replay·server) ·
`agent_sdk/` · `agents/kimi_explorer/`(VLM 探索 harness)·
`agents/cli_explorer/`(MCP 服务器 + CLI 框架适配,推荐评测入口)·
`demo_agent/`(脚本化校准)· `dashboard/static/` · `sample_apps/ecommerce_demo/` ·
`schemas/` · `docs/` · `docker/` · `tests/` · `runs/<session_id>/`(artifacts)

## 约定
- 坐标系: 屏幕像素,原点左上;viewport 1440×900, dpr=1, 写入 session.json。
- 帧一律渲染光标叠加后存储(单一事实源)。
- Sample app 是服务端渲染 + 固定几何 CSS;改布局必须同步
  `sample_apps/ecommerce_demo/LAYOUT.md` 并重新校准 `demo_agent/run_demo.py` 坐标。
- 新 reference app: 按 `docs/adding_reference_app.md`,在
  `benchmark/orchestrator/apps.py` 注册。
- **本机装有系统代理**: 所有内部 HTTP 调用必须 `trust_env=False`(httpx)或
  无代理 opener(urllib);浏览器启动必须带 `--no-proxy-server`。
  否则 loopback 流量会被代理拦截(502 / ERR_PROXY_CONNECTION_FAILED)。
  **例外**: 调用公网 VLM API(`agents/kimi_explorer/client.py`)保持
  `trust_env=True` —— 出网可能需要系统代理。
- Demo 会话范例: `runs/sess_20260819_074732_dd0526/`(完整 artifacts + 视频 + replay)。
- VLM 探索 Agent: `agents/kimi_explorer/`,key 经 `BBB_VLM_API_KEY` 注入,
  禁止写入任何文件或日志。
- CLI 框架评测: `agents/cli_explorer/run.py --cli kimi|claude|codex|opencode`;
  MCP 服务器零依赖手写(stdio JSON-RPC);**不要 pip install mcp**——
  它会升级 starlette/pydantic 打破 pin;kimi 的项目级 mcp.json 在 headless
  下不可用(信任提示),必须走用户级注入并自动还原。
- 零命令入口: `agents/cli_explorer/install` 一次性注册用户级 mcp.json(不带
  session env)+ skill;MCP 服务器在无 env 时自举(起 controller+建会话),
  stdin 关闭时自动 finalize。修改 `mcp_server.py` 后回归
  `tests/test_mcp_server.py`(含自举用例)。
- **kimi 会跨对话复用 MCP 进程**(长驻 stdio server): 升级 mcp_server 代码后,
  旧对话里跑的仍是旧代码 + 旧会话绑定——必须新开对话(或按 PID 杀旧
  mcp_server 进程)。甄别方法: `list_targets` 返回的
  `mcp_server.version/started_at`。会话 404/410 时 MCP 会自动释放死绑定并在
  错误文本里指引 agent 重调 start_session。
- 探索目标在 skill 调用时指定: `/skill:blackbox-explorer [app_id|URL]`
  (底层是 MCP 的 start_session/list_targets 工具;一个对话绑定一个目标)。
  URL 临时目标无需注册——`POST /api/sessions {"live_url": ...}` 自动派生
  app_id(live_<host>)、域名白名单(host+bare 域)与按站点持久的 profile;
  需要登录的站点先 `scripts/live_login.py --url <URL> --capture`。
  站点资源若在第三方 CDN 域(如 douyinpic.com)会渲染残缺——此时按
  `docs/adding_reference_app.md` §10 注册 curated allowed_hosts。
- 清理 `runs/` 前先查 `/api/sessions`(或 dashboard)确认没有 running 会话——
  误删运行中会话的 frames/ 会让它下一帧写盘失败直接 failed;残留进程锁文件
  时先按 PID 杀进程再删(Chrome 对 live profile 有单例锁,残留 chrome 会让
  下个会话 "browser exited early: 已在现有浏览器会话中打开")。
- Live target `douyin_web` / `bilibili_web`(真实网站,`kind="live"`,非 benchmark
  级隔离,无 S0/determinism): 登录态人工维护——
  `vendor/python/python.exe scripts/live_login.py --app <id>|--url <URL> --capture`
  (headed 手动登录→Enter→存 golden profile + 头像参考图到
  `runs/live_targets/<app_id>/`,敏感勿外传)。建会话跑像素 precheck
  (`avatar_logged_in`,头像滑窗模板匹配,区域表在 `prechecks._AVATAR_ZONES`),
  失败先还原 golden 再 503 拒绝。**golden 必须取自最近一次成功登录后的
  profile**——旧备份里的会话 token 可能已被服务端轮换失效(踩过)。headed 下
  CDP Input 够不着地址栏,action schema 无修饰键/右键,F12 已在 Session 层封堵;
  外网经 `--host-resolver-rules` 白名单收敛到目标站点域名(curated 在
  `apps.py::_DOUYIN_HOSTS/_BILIBILI_HOSTS`)。新站点头像区域需校准:
  capture 存全屏截图→定位头像→登记 _AVATAR_ZONES→裁剪 header_ref.png。
  详见 `docs/security_model.md` §3 与 `docs/adding_reference_app.md` §10。
