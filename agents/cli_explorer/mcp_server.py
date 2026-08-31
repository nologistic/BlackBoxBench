"""BlackBoxBench MCP stdio server — exposes the benchmark agent channel as MCP
tools so agent frameworks (Kimi Code, Claude Code, Codex, OpenCode…) can drive
the GUI exploration with their own planning loop.

Zero external dependencies: newline-delimited JSON-RPC 2.0 over stdio
(MCP stdio transport).

Two operation modes:
- bound mode: BBB_SESSION (+BBB_CONTROLLER) env set → serves that session
- self-bootstrap mode (no env): on first use, finds or starts the controller,
  creates a fresh session (BBB_APP_ID, default ecommerce_demo), and finalizes
  it when the stdio channel closes (agent conversation ended). This is what
  makes "new chat → /skill:blackbox-explorer" work with zero terminal commands.
"""
from __future__ import annotations

import argparse
import base64
import hmac
import io
import json
import os
import socket
import subprocess
import sys
import threading
import time
from pathlib import Path

import httpx
from agents.cli_explorer.reproduction_review import ManagedReproductionReview
from benchmark import config as benchmark_config
from benchmark.orchestrator.controller_process import (
    ensure_local_controller, start_local_controller)
from reproduction.workspace import DockerUnavailableError, ReproductionWorkspace

PROTOCOL_VERSION = "2025-06-18"
SERVER_NAME = "blackboxbench"
SERVER_VERSION = "1.6.0"
# process start stamp: kimi keeps MCP stdio processes alive across
# conversations, so a long-running conversation may run STALE code after an
# upgrade — this stamp makes that diagnosable via list_targets.
SERVER_STARTED_AT = time.strftime("%Y-%m-%d %H:%M:%S")

PROJECT_ROOT = Path(__file__).resolve().parent.parent.parent
_controller = os.environ.get("BBB_CONTROLLER", "http://127.0.0.1:7800").rstrip("/")
_session = os.environ.get("BBB_SESSION", "")
_app_id = os.environ.get("BBB_APP_ID", "ecommerce_demo")
_own_session = False   # we created it → we finalize on shutdown
_bound_target = ""     # normalized target key set by start_session
_reproduction: ReproductionWorkspace | None = None
_review: ManagedReproductionReview | None = None

_http = httpx.Client(timeout=120.0, trust_env=False)


def _log(msg: str) -> None:
    print(f"[blackboxbench-mcp] {msg}", file=sys.stderr, flush=True)


def _controller_up() -> bool:
    try:
        r = _http.get(f"{_controller}/api/apps", timeout=3)
        return r.status_code == 200 and isinstance(r.json(), list)
    except Exception:
        return False


def _start_controller() -> None:
    start_local_controller(_controller, _controller_up)


def _ensure_controller() -> None:
    ensure_local_controller(_controller, _controller_up, announce=_log)


def _budget_payload() -> dict:
    return {"max_actions": int(os.environ.get("BBB_MAX_ACTIONS", "1500")),
            "max_duration_s": int(os.environ.get("BBB_MINUTES", "180")) * 60,
            "max_observations": 2000}


def _ensure_session() -> None:
    """Lazily bind to a benchmark session (creating everything if needed)."""
    global _session, _own_session, _bound_target
    if _session:
        return
    _ensure_controller()
    r = _http.post(f"{_controller}/api/sessions", json={
        "app_id": _app_id, "budget": _budget_payload()})
    r.raise_for_status()
    _session = r.json()["session_id"]
    _own_session = True
    _bound_target = f"app:{_app_id}"
    _log(f"session created: {_session} (app={_app_id})")


def _shutdown() -> None:
    global _reproduction, _review
    if _review is not None:
        try:
            _review.close()
        except Exception as e:
            _log(f"reproduction review cleanup failed: {type(e).__name__}")
        finally:
            _review = None
    if _reproduction is not None:
        try:
            _reproduction.close()
        except Exception as e:
            _log(f"reproduction cleanup failed: {type(e).__name__}")
        finally:
            _reproduction = None
    if _own_session and _session:
        try:
            _http.post(f"{_controller}/agent/{_session}/finalize", json={},
                       timeout=30)
            _log(f"session {_session} finalized on exit")
        except Exception as e:
            _log(f"finalize on exit failed: {e}")


def _post(path: str, payload: dict) -> tuple[dict | None, str | None]:
    """POST to the agent channel; returns (json, error_text)."""
    try:
        _ensure_session()
    except Exception as e:
        return None, f"bootstrap failed: {e}"
    try:
        r = _http.post(f"{_controller}/agent/{_session}{path}", json=payload)
    except Exception as e:
        return None, f"controller unreachable: {e}"
    if r.status_code != 200:
        try:
            d = r.json()
            msg = d.get("detail", r.text[:200])
            if isinstance(d.get("message"), str):
                msg += ": " + d["message"]
        except Exception:
            msg = r.text[:200]
        if r.status_code in (404, 410):
            # session gone (controller restarted / failed / closed): the
            # binding is dead weight — release it NOW and tell the agent the
            # one-step recovery, or it flails (wait/list_targets loops).
            _release_binding(f"{r.status_code} from agent channel")
            return None, (f"HTTP {r.status_code}: {msg} —— 会话已失效且绑定已释放。"
                          f"恢复方法: 调用 start_session(app_id 或 url 同前)"
                          f"重建会话后继续探索。")
        return None, f"HTTP {r.status_code}: {msg}"
    return r.json(), None


def _ok(content: list[dict]) -> dict:
    return {"content": content, "isError": False}


def _err(text: str) -> dict:
    return {"content": [{"type": "text", "text": text}], "isError": True}


def _text(d: dict) -> dict:
    return {"type": "text", "text": json.dumps(d, ensure_ascii=False)}


# ------------------------------------------------------------------ tools

def _t_observe(_args: dict) -> dict:
    d, err = _post("/observe", {})
    if err:
        return _err(err)
    meta = {k: d[k] for k in ("frame_id", "timestamp", "width", "height",
                              "cursor", "budget", "brief", "tabs") if k in d}
    meta["session_id"] = _session
    return _ok([
        {"type": "image", "data": d["screenshot_png_b64"],
         "mimeType": "image/png"},
        _text(meta),
    ])


def _t_action(args: dict) -> dict:
    atype = args.pop("type", None)
    if not atype:
        return _err("missing action type")
    d, err = _post("/action", {"type": atype,
                               **{k: v for k, v in args.items() if v is not None}})
    if err:
        return _err(err)
    return _ok([_text(d)])


def _t_discovery(path: str):
    def call(args: dict) -> dict:
        d, err = _post(path, args)
        if err:
            return _err(err)
        return _ok([_text(d)])
    return call


def _managed_exploration_files(run_dir: Path) -> dict[str, Path]:
    """Allowlist only Agent-visible visual evidence and finalized summaries."""
    files: dict[str, Path] = {}
    if run_dir.is_symlink():
        return files
    root = run_dir.resolve()
    frames = run_dir / "frames"
    if frames.is_dir() and not frames.is_symlink():
        for path in sorted(frames.glob("frame_*.png")):
            if path.is_file() and not path.is_symlink():
                resolved = path.resolve()
                try:
                    resolved.relative_to(root)
                except ValueError:
                    continue
                files[f"screenshots/{path.name}"] = resolved
    for name in ("functional_topology.md", "coverage_report.json"):
        path = run_dir / name
        if path.is_file() and not path.is_symlink():
            resolved = path.resolve()
            try:
                resolved.relative_to(root)
            except ValueError:
                continue
            files[name] = resolved
    return files


def _t_finalize(_args: dict) -> dict:
    global _reproduction
    if _reproduction is not None:
        return _ok([_text({"exploration_finished": True,
                           **_reproduction.started_payload(),
                           "review": _review_payload()})])
    source_id = _session
    existing = (benchmark_config.RUNS_DIR / source_id /
                "functional_topology.json") if source_id else None
    if existing is None or not existing.is_file():
        d, err = _post("/finalize", {})
        if err:
            return _err(err)
        source_id = _session
        topology = benchmark_config.RUNS_DIR / source_id / "functional_topology.json"
    else:
        d = {"topology_path": f"runs/{source_id}/functional_topology.json"}
        topology = existing
    if os.environ.get("BBB_REPRODUCTION_AUTOSTART", "1") == "0":
        return _ok([_text({**d, "exploration_finished": True,
                           "reproduction_skipped": True})])
    try:
        run_dir = benchmark_config.RUNS_DIR / source_id
        _reproduction = ReproductionWorkspace.start(
            source_mode="managed-tools", source_id=source_id,
            topology_path=topology,
            exploration_files=_managed_exploration_files(run_dir))
    except DockerUnavailableError as exc:
        _log(f"reproduction handoff unavailable: {exc}")
        return _err(str(exc))
    except Exception as exc:
        _log(f"reproduction handoff failed: {type(exc).__name__}: {exc}")
        return _err("exploration finalized but reproduction workspace failed to start; retry finalize")
    return _ok([_text({**d, "exploration_finished": True,
                       **_reproduction.started_payload(),
                       "review": _review_payload()})])


def _require_reproduction() -> ReproductionWorkspace:
    if _reproduction is None:
        raise ValueError("available only after finalize starts reproduction")
    return _reproduction


def _t_workspace_list(args: dict) -> dict:
    return _ok([_text(_require_reproduction().list_files(args.get("path", ".")))])


def _t_workspace_read(args: dict) -> dict:
    return _ok(_require_reproduction().read_file(args.get("path")))


def _t_workspace_write(args: dict) -> dict:
    if _review is not None and _review.active:
        raise ValueError(
            "complete the active reproduction review before modifying output")
    return _ok([_text(_require_reproduction().write_file(
        args.get("path"), args.get("content")))])


def _t_workspace_run(args: dict) -> dict:
    if _review is not None and _review.active:
        raise ValueError(
            "complete the active reproduction review before running build commands")
    return _ok([_text(_require_reproduction().run_program(
        args.get("argv"), args.get("cwd", "."),
        args.get("timeout_seconds", 30)))])


def _review_payload() -> dict:
    if _review is not None:
        return _review.status_payload()
    configured = _configured_review_revisions()
    return {"review_required": True, "review_active": False,
            "review_accepted": False, "review_rounds": 0,
            "revision_count": 0, "max_revisions": configured,
            "start_tool": "start_reproduction_review"}


def _configured_review_revisions() -> int:
    try:
        configured = int(os.environ.get("BBB_REPRO_REVISIONS", "3"))
    except ValueError:
        configured = 3
    return max(0, min(5, configured))


def _require_review() -> ManagedReproductionReview:
    if _review is None:
        raise ValueError("start_reproduction_review must be called first")
    return _review


def _t_start_reproduction_review(args: dict) -> dict:
    global _review
    workspace = _require_reproduction()
    if _review is None:
        _review = ManagedReproductionReview(
            workspace.output_dir, workspace.handoff_id,
            max_revisions=_configured_review_revisions())
    png, meta = _review.start_round(args.get("entry_path", "."))
    return _ok([
        {"type": "image", "data": base64.b64encode(png).decode("ascii"),
         "mimeType": "image/png"},
        _text(meta),
    ])


def _t_review_observe(_args: dict) -> dict:
    png, meta = _require_review().observe()
    return _ok([
        {"type": "image", "data": base64.b64encode(png).decode("ascii"),
         "mimeType": "image/png"},
        _text(meta),
    ])


def _t_review_action(args: dict) -> dict:
    action_type = args.pop("type", None)
    if not action_type:
        raise ValueError("missing review action type")
    return _ok([_text(_require_review().action(action_type, args))])


def _t_complete_reproduction_review(args: dict) -> dict:
    return _ok([_text(_require_review().complete_round(
        decision=args.get("decision"),
        checked_flows=args.get("checked_flows"),
        findings=args.get("findings")))])


def _t_finish_reproduction(_args: dict) -> dict:
    global _reproduction, _review
    workspace = _require_reproduction()
    review = _require_review()
    review.ensure_accepted()
    result = workspace.finish(review_summary=review.summary())
    review.close()
    _review = None
    _reproduction = None
    return _ok([_text(result)])


def _t_list_targets(_args: dict) -> dict:
    """List registered targets + how to aim at an unregistered site."""
    try:
        _ensure_controller()
        r = _http.get(f"{_controller}/api/apps", timeout=10)
        r.raise_for_status()
        apps = r.json()
    except Exception as e:
        return _err(f"无法获取目标列表: {e}")
    return _ok([_text({
        "default_target": _app_id,
        "registered": apps,
        "current_session": _session or None,
        "mcp_server": {"version": SERVER_VERSION,
                       "started_at": SERVER_STARTED_AT},
        "adhoc": "start_session 也接受 url 参数(如 https://example.com) "
                 "直接探索未注册的网站;该站点的浏览器登录态会按站点持久化。",
    })])


def _bound_session_running() -> bool:
    """True iff the currently bound session still accepts work."""
    if not _session:
        return False
    try:
        r = _http.get(f"{_controller}/api/sessions/{_session}", timeout=10)
        return r.status_code == 200 and r.json().get("status") == "running"
    except Exception:
        return False


def _release_binding(reason: str) -> None:
    global _session, _own_session, _bound_target
    _log(f"releasing binding to {_session} ({reason})")
    _session = ""
    _own_session = False
    _bound_target = ""


def _t_start_session(args: dict) -> dict:
    """Explicitly choose the exploration target for this conversation."""
    global _session, _own_session, _bound_target
    app_id = (args.get("app_id") or "").strip()
    url = (args.get("url") or "").strip()
    if not app_id and not url:
        return _err("需要 app_id 或 url 之一")
    key = f"app:{app_id}" if app_id else f"url:{url}"
    if _session:
        if _bound_session_running():
            if _bound_target == key:
                return _ok([_text({"session_id": _session, "target": key,
                                   "note": "已是当前目标,继续探索即可"})])
            return _err(f"当前对话已绑定会话 {_session}({_bound_target})。"
                        f"更换目标请先 finalize 结束它,或新开一个对话。")
        # bound session died or was closed externally — release and rebind
        # instead of bricking the conversation (finalize is impossible then)
        _release_binding("session no longer running")
    try:
        _ensure_controller()
    except Exception as e:
        return _err(f"controller 启动失败: {e}")
    payload = {"budget": _budget_payload()}
    if app_id:
        payload["app_id"] = app_id
    else:
        payload["live_url"] = url
    try:
        r = _http.post(f"{_controller}/api/sessions", json=payload)
    except Exception as e:
        return _err(f"controller unreachable: {e}")
    if r.status_code != 200:
        try:
            msg = r.json().get("detail", r.text[:300])
        except Exception:
            msg = r.text[:300]
        # precheck 失败(如抖音未登录)的指引会随这里带给用户
        return _err(f"会话创建失败 HTTP {r.status_code}: {msg}")
    body = r.json()
    _session = body["session_id"]
    _own_session = True
    _bound_target = key
    _log(f"session created: {_session} (target={key})")
    return _ok([_text({"session_id": _session,
                       "app_id": body.get("app_id"),
                       "brief": body.get("brief", "")})])


_ACTION_PROPS = {
    "x": {"type": "integer", "description": "屏幕像素坐标(0-1439)"},
    "y": {"type": "integer", "description": "屏幕像素坐标(0-899)"},
    "x1": {"type": "integer"}, "y1": {"type": "integer"},
    "x2": {"type": "integer"}, "y2": {"type": "integer"},
    "duration_ms": {"type": "integer"},
    "text": {"type": "string"},
    "key": {"type": "string", "description": "Enter/Tab/Escape/Backspace/F5/BrowserBack/BrowserForward/ArrowDown/单字符"},
    "dx": {"type": "integer"}, "dy": {"type": "integer"},
    "ms": {"type": "integer"},
    "tab_index": {"type": "integer",
                  "description": "标签页索引(0起,按打开顺序;回执 tabs 字段有 count/active)"},
}


def _action_tool(name: str, req: list[str], desc: str) -> dict:
    return {"name": name, "description": desc,
            "inputSchema": {"type": "object",
                            "properties": {k: _ACTION_PROPS[k] for k in req},
                            "required": req}}


_TOOLS: dict[str, dict] = {}


def _register(schema: dict, handler) -> None:
    schema = dict(schema)
    schema["_handler"] = handler
    _TOOLS[schema["name"]] = schema


_register({"name": "observe",
           "description": "获取当前屏幕截图(PNG 图像)+光标坐标+预算。这是你看应用的唯一窗口——没有 DOM/URL。",
           "inputSchema": {"type": "object", "properties": {}}}, _t_observe)

_register({"name": "list_targets",
           "description": "列出可探索的目标(已注册 app)及默认目标;探索开始前可先查看。",
           "inputSchema": {"type": "object", "properties": {}}}, _t_list_targets)

_register({"name": "start_session",
           "description": "选择本次探索目标并开始会话: app_id(已注册,如 douyin_web/ecommerce_demo) 或 url(任意网址,如 https://example.com)。一个对话绑定一个目标。",
           "inputSchema": {"type": "object", "properties": {
               "app_id": {"type": "string", "description": "已注册目标 id"},
               "url": {"type": "string", "description": "任意网站 URL(临时目标)"}}}},
          _t_start_session)

def _make_action_handler(name: str):
    def handler(args: dict) -> dict:
        return _t_action({**args, "type": name})
    return handler


for _n, _req, _d in [
    ("click", ["x", "y"], "左键点击屏幕坐标"),
    ("double_click", ["x", "y"], "双击"),
    ("move_pointer", ["x", "y"], "移动光标(悬停)"),
    ("mouse_down", ["x", "y"], "按下左键"),
    ("mouse_up", ["x", "y"], "抬起左键"),
    ("drag", ["x1", "y1", "x2", "y2"], "拖拽(可选 duration_ms)"),
    ("type_text", ["text"], "键盘输入文本(先 click 输入框获得焦点)"),
    ("key_press", ["key"], "按一下键: Enter/Tab/Escape/Backspace/F5/BrowserBack(返回上一页)/BrowserForward/方向键/单字符"),
    ("key_down", ["key"], "按住键"),
    ("key_up", ["key"], "松开键"),
    ("scroll", ["dx", "dy"], "滚动,dy>0 向下"),
    ("wait", ["ms"], "等待毫秒"),
    ("switch_tab", ["tab_index"],
     "切换到指定标签页(点击可能已自动跟随到新页;用它可以切回之前的页)"),
]:
    _register(_action_tool(_n, _req, _d), _make_action_handler(_n))

_register({"name": "close_tab",
           "description": "关闭当前标签页并回到上一个(最后一个不可关;用于关闭点击新开的页面)",
           "inputSchema": {"type": "object", "properties": {}}},
          _make_action_handler("close_tab"))

_register({"name": "record_state",
           "description": "记录发现的 UI 状态。visual_evidence 为当前 frame_id 列表。",
           "inputSchema": {"type": "object", "properties": {
               "name": {"type": "string"}, "description": {"type": "string"},
               "visual_evidence": {"type": "array", "items": {"type": "integer"}},
               "entry_conditions": {"type": "array", "items": {"type": "string"}},
               "observed_elements": {"type": "array", "items": {"type": "string"}},
               "confidence": {"type": "number"}}, "required": ["name"]}},
          _t_discovery("/discovery/state"))

_register({"name": "record_feature",
           "description": "记录发现的功能(可观察行为契约)。evidence 引用真实 step/帧。",
           "inputSchema": {"type": "object", "properties": {
               "name": {"type": "string"}, "description": {"type": "string"},
               "behavior": {"type": "object"},
               "evidence": {"type": "array", "items": {"type": "object"}},
               "confidence": {"type": "number"},
               "status": {"type": "string"}}, "required": ["name"]}},
          _t_discovery("/discovery/feature"))

_register({"name": "record_data",
           "description": "记录持久化数据实体(如 Cart/Order/Session)。",
           "inputSchema": {"type": "object", "properties": {
               "name": {"type": "string"}, "description": {"type": "string"},
               "confidence": {"type": "number"}}, "required": ["name"]}},
          _t_discovery("/discovery/data"))

_register({"name": "record_edge",
           "description": "记录节点间关系。type: REQUIRES|TRANSITIONS_TO|MUTATES|ENABLES|DISABLES|PERSISTS_TO|REVEALS|DEPENDS_ON|CONFLICTS_WITH|VALIDATES",
           "inputSchema": {"type": "object", "properties": {
               "source": {"type": "string"}, "target": {"type": "string"},
               "type": {"type": "string"},
               "evidence": {"type": "array", "items": {"type": "object"}},
               "confidence": {"type": "number"},
               "status": {"type": "string"},
               "note": {"type": "string"}},
               "required": ["source", "target", "type"]}},
          _t_discovery("/discovery/edge"))

_register({"name": "record_hypothesis",
           "description": "登记未验证的猜测与下一步探针。",
           "inputSchema": {"type": "object", "properties": {
               "statement": {"type": "string"},
               "next_probe": {"type": "string"},
               "confidence": {"type": "number"}},
               "required": ["statement"]}},
          _t_discovery("/discovery/hypothesis"))

_register({"name": "resolve_hypothesis",
           "description": "用新证据裁决假设: confirmed|rejected|uncertain。",
           "inputSchema": {"type": "object", "properties": {
               "hypothesis_id": {"type": "string"},
               "status": {"type": "string"},
               "note": {"type": "string"},
               "evidence": {"type": "array", "items": {"type": "object"}}},
               "required": ["hypothesis_id", "status"]}},
          lambda a: _t_discovery(f"/discovery/hypothesis/{a.pop('hypothesis_id')}/resolve")(a))

_register({"name": "revise",
           "description": "修订已记录的节点: op=update|merge|delete。",
           "inputSchema": {"type": "object", "properties": {
               "target_kind": {"type": "string"},
               "id": {"type": "string"}, "op": {"type": "string"},
               "fields": {"type": "object"}, "into": {"type": "string"},
               "reason": {"type": "string"}},
               "required": ["target_kind", "id", "op"]}},
          lambda a: _revise(a))


def _revise(a: dict) -> dict:
    target_kind, nid = a.get("target_kind"), a.get("id")
    body = {k: v for k, v in a.items() if k in ("op", "fields", "into", "reason")}
    try:
        _ensure_session()
        r = _http.patch(f"{_controller}/agent/{_session}/discovery/{target_kind}/{nid}",
                        json=body)
    except Exception as e:
        return _err(f"controller unreachable: {e}")
    if r.status_code != 200:
        return _err(f"HTTP {r.status_code}: {r.text[:200]}")
    return _ok([_text(r.json())])


_register({"name": "finalize",
           "description": "探索完成:生成最终功能拓扑图、结束目标会话并立即启动隔离复现阶段。",
           "inputSchema": {"type": "object", "properties": {}}}, _t_finalize)

_register({"name": "workspace_list",
           "description": "仅在 finalize 后列出复现输出工作区中的文件。",
           "inputSchema": {"type": "object", "properties": {
               "path": {"type": "string"}}}}, _t_workspace_list)

_register({"name": "workspace_read",
           "description": "仅在 finalize 后读取复现输出中的文本或 PNG/JPEG。",
           "inputSchema": {"type": "object", "properties": {
               "path": {"type": "string"}}, "required": ["path"]}},
          _t_workspace_read)

_register({"name": "workspace_write",
           "description": "仅在 finalize 后写入复现输出工作区。",
           "inputSchema": {"type": "object", "properties": {
               "path": {"type": "string"}, "content": {"type": "string"}},
               "required": ["path", "content"]}}, _t_workspace_write)

_register({"name": "workspace_run",
           "description": "仅在 finalize 后于无网络复现沙箱中运行 argv 形式程序。",
           "inputSchema": {"type": "object", "properties": {
               "argv": {"type": "array", "items": {"type": "string"}},
               "cwd": {"type": "string"},
               "timeout_seconds": {"type": "integer", "minimum": 1,
                                   "maximum": 120}},
               "required": ["argv"]}}, _t_workspace_run)


def _make_review_action_handler(name: str):
    def handler(args: dict) -> dict:
        return _t_review_action({**args, "type": name})
    return handler


_register({"name": "start_reproduction_review",
           "description": "启动对本次 Agent 生成网页的像素级黑盒复验；entry_path 必须是 website_output 本次结果内含 index.html 的目录。",
           "inputSchema": {"type": "object", "properties": {
               "entry_path": {"type": "string", "description": "相对输出根目录，默认 ."}}}},
          _t_start_reproduction_review)

_register({"name": "review_observe",
           "description": "获取复现网页当前可见截图；不提供 DOM、selector、URL 或网络语义。",
           "inputSchema": {"type": "object", "properties": {}}},
          _t_review_observe)

for _n, _req, _d in [
    ("click", ["x", "y"], "点击复现网页坐标"),
    ("double_click", ["x", "y"], "双击复现网页坐标"),
    ("move_pointer", ["x", "y"], "在复现网页中移动光标"),
    ("mouse_down", ["x", "y"], "在复现网页中按下左键"),
    ("mouse_up", ["x", "y"], "在复现网页中抬起左键"),
    ("drag", ["x1", "y1", "x2", "y2"], "在复现网页中拖拽"),
    ("type_text", ["text"], "向复现网页当前焦点键入文本"),
    ("key_press", ["key"], "在复现网页中按键"),
    ("key_down", ["key"], "在复现网页中按住键"),
    ("key_up", ["key"], "在复现网页中松开键"),
    ("scroll", ["dx", "dy"], "滚动复现网页"),
    ("wait", ["ms"], "等待复现网页更新"),
    ("switch_tab", ["tab_index"], "切换复现网页标签页"),
]:
    _schema = _action_tool(f"review_{_n}", _req, _d)
    _register(_schema, _make_review_action_handler(_n))

_register({"name": "review_close_tab",
           "description": "关闭复验浏览器的当前标签页并回到上一个。",
           "inputSchema": {"type": "object", "properties": {}}},
          _make_review_action_handler("close_tab"))

_register({"name": "complete_reproduction_review",
           "description": "结束当前复验轮次。发现问题选 revise，修改后再复验；核心流程正常且无私人信息时才选 accept。",
           "inputSchema": {"type": "object", "properties": {
               "decision": {"type": "string", "enum": ["accept", "revise"]},
               "checked_flows": {"type": "array", "items": {"type": "string"},
                                 "minItems": 1},
               "findings": {"type": "array", "items": {"type": "string"},
                            "minItems": 1}},
               "required": ["decision", "checked_flows", "findings"]}},
          _t_complete_reproduction_review)

_register({"name": "finish_reproduction",
           "description": "仅在像素级复验已 accept 后完成网页复现并保留 website_output 中的结果。",
           "inputSchema": {"type": "object", "properties": {}}},
          _t_finish_reproduction)


# ------------------------------------------------------------------ JSON-RPC

def _handle(msg: dict) -> dict | None:
    mid = msg.get("id")
    method = msg.get("method")
    if method == "initialize":
        return {"jsonrpc": "2.0", "id": mid, "result": {
            "protocolVersion": msg.get("params", {}).get(
                "protocolVersion", PROTOCOL_VERSION),
            "capabilities": {"tools": {}},
            "serverInfo": {"name": SERVER_NAME, "version": SERVER_VERSION}}}
    if method == "notifications/initialized" or method == "notifications/cancelled":
        return None
    if method == "ping":
        return {"jsonrpc": "2.0", "id": mid, "result": {}}
    if method == "tools/list":
        tools = [{k: v for k, v in s.items() if not k.startswith("_")}
                 for s in _TOOLS.values()]
        return {"jsonrpc": "2.0", "id": mid, "result": {"tools": tools}}
    if method == "tools/call":
        params = msg.get("params", {})
        name = params.get("name", "")
        args = params.get("arguments", {}) or {}
        tool = _TOOLS.get(name)
        if not tool:
            return {"jsonrpc": "2.0", "id": mid, "error": {
                "code": -32602, "message": f"unknown tool: {name}"}}
        try:
            result = tool["_handler"](dict(args))
        except ValueError as e:
            result = _err(str(e))
        except (OSError, subprocess.SubprocessError):
            result = _err("workspace operation failed")
        except Exception as e:
            result = _err(f"tool crashed: {e!r}")
        return {"jsonrpc": "2.0", "id": mid, "result": result}
    if mid is None:
        return None
    return {"jsonrpc": "2.0", "id": mid,
            "error": {"code": -32601, "message": f"method not found: {method}"}}


def _reset_conversation() -> None:
    """A new client conversation starts from a clean binding state.

    In bound mode (BBB_SESSION env) the external session is kept across
    conversations; in self-bootstrap mode each conversation gets its own
    session and the previous one is finalized on disconnect.
    """
    global _session, _own_session, _bound_target
    if _review is not None or _reproduction is not None or _own_session:
        try:
            _shutdown()
        except Exception:
            pass
    _session = os.environ.get("BBB_SESSION", "")
    _own_session = False
    _bound_target = ""


def _authorized(msg: object, token: str) -> bool:
    if not isinstance(msg, dict):
        return False
    params = msg.get("params")
    if msg.get("method") != "auth" or not isinstance(params, dict):
        return False
    supplied = params.get("token")
    return isinstance(supplied, str) and hmac.compare_digest(supplied, token)


def _serve_stream(lines, send, token: str | None = None) -> None:
    """Serve one line-delimited JSON-RPC conversation (stdio or TCP)."""
    _reset_conversation()
    authenticated = token is None
    try:
        for raw in lines:
            raw = raw.strip()
            if not raw:
                continue
            try:
                msg = json.loads(raw)
            except Exception:
                continue
            if not authenticated:
                if _authorized(msg, token):
                    authenticated = True
                    send({"jsonrpc": "2.0", "id": msg.get("id"),
                          "result": {"ok": True}})
                else:
                    send({"jsonrpc": "2.0", "id": msg.get("id"),
                          "error": {"code": -32000, "message": "unauthorized"}})
                    return
                continue
            resp = _handle(msg)
            if resp is not None:
                send(resp)
    finally:
        _shutdown()  # stream closed → the agent conversation ended


def serve_tcp(bind_host: str, port: int, token: str) -> None:
    """TCP transport for containerized agent clients (strict runs).

    One conversation at a time; bind 127.0.0.1 so only this machine and
    Docker Desktop's container proxy can reach the endpoint. The token is
    mandatory defense in depth.

    Docker Desktop's proxy does not always propagate a container-side
    disconnect, which would leave an application-level zombie connection
    holding the conversation slot forever (TCP keepalives cannot detect it:
    the proxy ACKs on its own behalf). A newly connected client that proves
    the token therefore EVICTS the incumbent connection: with a per-run
    token there is exactly one legitimate client, so the incumbent is
    almost certainly a zombie from a killed `docker exec` / crashed client.
    """
    import socketserver

    max_message = 16 * 1024 * 1024

    class Conversation:
        def __init__(self) -> None:
            self.lock = threading.Lock()
            self.current: "Handler | None" = None

    conversation = Conversation()

    class Handler(socketserver.StreamRequestHandler):
        def setup(self) -> None:
            super().setup()
            self._evicted = False
            # Wake blocked reads every second so eviction (see evict()) can
            # take effect: neither shutdown() nor close() reliably unblocks a
            # recv() sitting in another thread on Windows.
            self.connection.settimeout(1.0)

        def handle(self) -> None:
            if not self._authenticate():
                return
            if not self._acquire_slot():
                self._reply({"jsonrpc": "2.0", "id": None,
                             "error": {"code": -32000, "message": "busy"}})
                return
            conversation.current = self
            try:
                def send(payload: dict) -> None:
                    self.wfile.write(
                        json.dumps(payload, ensure_ascii=False).encode("utf-8")
                        + b"\n")
                    self.wfile.flush()

                def lines():
                    while True:
                        try:
                            chunk = self.rfile.readline(max_message + 1)
                        except TimeoutError:
                            if self._evicted:
                                return
                            continue
                        if not chunk:
                            return
                        if len(chunk) > max_message:
                            self._reply({"jsonrpc": "2.0", "id": None,
                                         "error": {"code": -32000,
                                                   "message": "message too large"}})
                            return
                        yield chunk.decode("utf-8", errors="replace")

                _serve_stream(lines(), send, token=None)
            except OSError:
                pass
            finally:
                conversation.current = None
                conversation.lock.release()

        def _authenticate(self) -> bool:
            self.connection.settimeout(60)
            try:
                first = self.rfile.readline(max_message + 1)
            except OSError:
                return False
            finally:
                self.connection.settimeout(1.0)
            try:
                msg = json.loads(first.decode("utf-8", errors="replace"))
            except Exception:
                msg = None
            if not _authorized(msg, token):
                self._reply({"jsonrpc": "2.0",
                             "id": msg.get("id") if isinstance(msg, dict) else None,
                             "error": {"code": -32000,
                                       "message": "unauthorized"}})
                return False
            self._reply({"jsonrpc": "2.0", "id": msg.get("id"),
                         "result": {"ok": True}})
            return True

        def _acquire_slot(self) -> bool:
            for _ in range(3):
                if conversation.lock.acquire(timeout=1):
                    return True
                holder = conversation.current
                if holder is not None:
                    _log("evicting stale MCP connection")
                    holder.evict()
            return conversation.lock.acquire(timeout=3)

        def evict(self) -> None:
            # The periodic read timeout in lines() makes the holder exit on
            # this flag; shutdown accelerates it where the platform allows.
            self._evicted = True
            try:
                self.connection.shutdown(socket.SHUT_RDWR)
            except OSError:
                pass

        def _reply(self, payload: dict) -> None:
            try:
                self.wfile.write(
                    json.dumps(payload, ensure_ascii=False).encode("utf-8")
                    + b"\n")
                self.wfile.flush()
            except OSError:
                pass

    class Server(socketserver.ThreadingTCPServer):
        allow_reuse_address = True
        daemon_threads = True

        def get_request(self):
            # Keepalives reap connections whose peer died at the TCP level
            # (e.g. a hard container kill); proxy-level zombies are handled
            # by the eviction above.
            sock, addr = super().get_request()
            sock.setsockopt(socket.SOL_SOCKET, socket.SO_KEEPALIVE, 1)
            if os.name == "nt":
                sock.ioctl(socket.SIO_KEEPALIVE_VALS, (1, 30_000, 5_000))
            else:
                sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_KEEPIDLE, 30)
                sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_KEEPINTVL, 5)
                sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_KEEPCNT, 3)
            return sock, addr

    with Server((bind_host, port), Handler) as server:
        _log(f"TCP MCP listening on {bind_host}:{port} (token auth required)")
        try:
            server.serve_forever()
        except KeyboardInterrupt:
            pass


def main() -> None:
    parser = argparse.ArgumentParser(
        description="BlackBoxBench MCP (stdio, or TCP for containerized clients)")
    parser.add_argument("--tcp", metavar="[HOST:]PORT", default=None,
                        help="serve newline-delimited JSON-RPC over TCP instead of stdio")
    parser.add_argument("--token", default=os.environ.get("BBB_MCP_TOKEN", ""),
                        help="shared secret required by every TCP connection")
    args = parser.parse_args()

    if args.tcp:
        bind, _, port_text = args.tcp.rpartition(":")
        bind = bind or "127.0.0.1"
        token = args.token.strip()
        if len(token) < 16:
            raise SystemExit("TCP mode requires a --token of at least 16 characters")
        serve_tcp(bind, int(port_text), token)
        return

    # JSON-RPC over stdio is UTF-8 by spec; on a zh-CN Windows host the locale
    # default (GBK) would corrupt/drop non-ASCII payloads (Chinese discovery
    # records, search text). Pin both channels to UTF-8.
    sys.stdin = io.TextIOWrapper(sys.stdin.buffer, encoding="utf-8")
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")

    def send(payload: dict) -> None:
        sys.stdout.write(json.dumps(payload, ensure_ascii=False) + "\n")
        sys.stdout.flush()

    try:
        _serve_stream(sys.stdin, send)
    except OSError:
        pass


if __name__ == "__main__":
    main()
