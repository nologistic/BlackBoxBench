"""Android baseline MCP: pixels, coordinate touch and finalized topology.

Zero external dependencies: newline-delimited JSON-RPC 2.0 over stdio
(MCP stdio transport).

Two operation modes:
- bound mode: BBB_SESSION (+BBB_CONTROLLER) env set → serves that session
- self-bootstrap mode (no env): on first use, finds or starts the controller,
  creates a fresh session (BBB_ANDROID_APP_ID, default android_commerce_demo), and finalizes
  it when the stdio channel closes (agent conversation ended). This is what
  makes "new chat → /skill:android-blackbox-explorer" work with zero terminal commands.
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
from pathlib import Path, PurePosixPath

import httpx
from app_reproduction.materials.build import ensure_app_materials
from app_reproduction.review import AndroidReproductionReview
from app_reproduction.workspace import (
    AppReproductionWorkspace, DockerUnavailableError)
from reproduction.materials.build import ensure_materials
from benchmark import config as benchmark_config
from benchmark.android.targets import get_android_target
from benchmark.orchestrator.controller_process import (
    ensure_local_controller, start_local_controller)

PROTOCOL_VERSION = "2025-06-18"
SERVER_NAME = "android-blackboxbench"
SERVER_VERSION = "1.0.0"
# process start stamp: kimi keeps MCP stdio processes alive across
# conversations, so a long-running conversation may run STALE code after an
# upgrade — this stamp makes that diagnosable via list_targets.
SERVER_STARTED_AT = time.strftime("%Y-%m-%d %H:%M:%S")

PROJECT_ROOT = Path(__file__).resolve().parent.parent.parent
_controller = os.environ.get("BBB_CONTROLLER", "http://127.0.0.1:7800").rstrip("/")
_session = os.environ.get("BBB_SESSION", "")
_app_id = os.environ.get("BBB_ANDROID_APP_ID", "android_commerce_demo")
_own_session = False   # we created it → we finalize on shutdown
_bound_target = ""     # normalized target key set by start_session
_reproduction: AppReproductionWorkspace | None = None
_review: AndroidReproductionReview | None = None

# Android session creation boots a fresh AVD clone, pins the guest
# locale (which restarts the framework) and installs the APK, so the
# first call legitimately takes minutes. A web-sized timeout would
# abandon a session that the controller is still building.
_http = httpx.Client(timeout=httpx.Timeout(600.0, connect=10.0),
                     trust_env=False)


def _log(msg: str) -> None:
    print(f"[android-blackboxbench-mcp] {msg}", file=sys.stderr, flush=True)


def _controller_up() -> bool:
    try:
        r = _http.get(f"{_controller}/api/apps?platform=android", timeout=3)
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
    get_android_target(_app_id)
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
            response = _http.post(
                f"{_controller}/agent/{_session}/finalize", json={}, timeout=30)
            if response.status_code == 200:
                _log(f"session {_session} finalized on exit")
            else:
                _http.post(f"{_controller}/api/sessions/{_session}/close",
                           json={}, timeout=30)
                _log(f"session {_session} closed after finalize rejection")
        except Exception as e:
            _log(f"finalize on exit failed: {e}")
            try:
                _http.post(f"{_controller}/api/sessions/{_session}/close",
                           json={}, timeout=10)
            except Exception:
                pass


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
                          f"恢复方法: 调用 start_session(app_id 同前)"
                          f"重建会话后继续探索。")
        if r.status_code == 409:
            # A recoverable environment blip. The device is still healthy, so
            # this is not a fault to report or work around — the step simply did
            # not land. Say so plainly, without inviting a strategy change.
            return None, (f"{msg}。这一步没有生效，设备仍然正常；"
                          f"直接重试同一操作即可继续。")
        return None, f"HTTP {r.status_code}: {msg}"
    return r.json(), None


def _ok(content: list[dict]) -> dict:
    return {"content": content, "isError": False}


def _err(text: str) -> dict:
    return {"content": [{"type": "text", "text": text}], "isError": True}


def _text(d: dict) -> dict:
    return {"type": "text", "text": json.dumps(d, ensure_ascii=False)}


def _image_item(data_b64: str, mime: str = "image/png") -> dict:
    """Return one MCP image block, re-encoding PNG screenshots as JPEG.

    Exploration frames are lossless PNG captures of a 1080x2400 screen
    (measured 0.5-3.8MB each) and every frame is echoed back in later turns,
    so a long run pushes the request body toward the ~50MB gateway limit.
    Re-encoding at quality 80 shrinks a frame ~8x with no visible loss for
    UI work; the original PNG stays on disk for archival and pixel review
    (only the model-facing copy changes). BBB_IMAGE_JPEG=0 disables,
    BBB_IMAGE_JPEG_QUALITY tunes quality, BBB_IMAGE_JPEG_MAX_EDGE optionally
    downscales. Any failure falls back to the original payload: compression
    must never break an exploration.
    """
    if mime != "image/png" or not data_b64 or os.environ.get("BBB_IMAGE_JPEG", "1") == "0":
        return {"type": "image", "data": data_b64, "mimeType": mime}
    try:
        import io

        from PIL import Image

        with Image.open(io.BytesIO(base64.b64decode(data_b64))) as img:
            if img.mode not in ("RGB", "L"):
                img = img.convert("RGB")
            max_edge = int(os.environ.get("BBB_IMAGE_JPEG_MAX_EDGE", "0"))
            if max_edge and max(img.size) > max_edge:
                scale = max_edge / max(img.size)
                img = img.resize((max(1, int(img.width * scale)),
                                  max(1, int(img.height * scale))),
                                 Image.LANCZOS)
            buf = io.BytesIO()
            img.save(buf, "JPEG",
                     quality=int(os.environ.get("BBB_IMAGE_JPEG_QUALITY", "80")),
                     optimize=True)
        return {"type": "image",
                "data": base64.b64encode(buf.getvalue()).decode("ascii"),
                "mimeType": "image/jpeg"}
    except Exception:
        return {"type": "image", "data": data_b64, "mimeType": mime}


# ------------------------------------------------------------------ tools

def _t_observe(_args: dict) -> dict:
    d, err = _post("/observe", {})
    if err:
        return _err(err)
    meta = {k: d[k] for k in ("frame_id", "timestamp", "width", "height",
                              "cursor", "budget", "brief", "platform",
                              "orientation", "density_dpi") if k in d}
    meta["session_id"] = _session
    return _ok([
        _image_item(d["screenshot_png_b64"]),
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
    if not _session:
        # finalize means "finish the exploration I am bound to". Letting it
        # fall through to _post would lazily create a fresh session on the
        # default app — the path by which a dead Google Clock session once
        # handed off a blank commerce-demo topology: the agent believes it
        # is completing its own work while the MCP silently binds a
        # zero-exploration session, possibly on a different target. Refuse
        # and point at the recovery path instead.
        return _err("没有可定稿的探索会话：原会话已失效或尚未开始，finalize 不会"
                    "隐式新建会话。请先调用 start_session(app_id 同前) 重建会话、"
                    "完成探索后再 finalize。")
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
        target_id = _bound_target.removeprefix("app:") or _app_id
        target = get_android_target(target_id)
        _reproduction = AppReproductionWorkspace.start(
            source_mode="android-baseline", source_id=source_id,
            topology_path=topology,
            exploration_files=_managed_exploration_files(run_dir),
            protected_strings=target.protected_strings,
            protected_regions=target.protected_regions,
            target_id=target_id)
    except DockerUnavailableError as exc:
        _log(f"reproduction handoff unavailable: {exc}")
        return _err(str(exc))
    except Exception as exc:
        _log(f"reproduction handoff failed: {type(exc).__name__}: {exc}")
        return _err("exploration finalized but reproduction workspace failed to start; retry finalize")
    return _ok([_text({**d, "exploration_finished": True,
                       **_reproduction.started_payload(),
                       "review": _review_payload()})])


def _require_reproduction() -> AppReproductionWorkspace:
    if _reproduction is None:
        raise ValueError("available only after finalize starts reproduction")
    return _reproduction


# Material read channel for the baseline reproduction stage. The per-app
# supplement packs are mounted for BOTH conditions (AppReproductionWorkspace
# mounts /materials/app by target_id, not by source_mode), but the baseline
# MCP historically offered no tool that could reach them — workspace_read is
# anchored to /workspace, so every /materials/* request was path-rejected
# (observed on the 2026-09-10 organic_maps reproduction: the agent had to
# draw the map from prior knowledge instead of reading the POI pack). This
# restores the intended fairness: materials are a shared baseline, while the
# exploration evidence (/exploration, topology) stays our-method-only — that
# difference IS the experimental condition.
_INPUT_MAX_READ = 2 * 1024 * 1024


def _material_roots() -> dict[str, Path]:
    roots = {"common": ensure_materials().resolve(),
             "mobile": ensure_app_materials().resolve()}
    # The per-app supplement pack is mounted at /materials/app when the
    # current target ships one (see AppReproductionWorkspace.start).
    if (_reproduction is not None
            and _reproduction.app_materials_dir is not None):
        roots["app"] = Path(_reproduction.app_materials_dir)
    return roots


def _resolve_input_host(path_value: object,
                        rep: AppReproductionWorkspace) -> Path:
    """Map a whitelisted sandbox materials path to its host-side file.

    Baseline may read /materials only: /exploration and the finalized
    topology are the our-method evidence channel and stay closed here.
    """
    raw = str(path_value or "").replace("\\", "/")
    candidate = PurePosixPath(raw)
    if candidate.is_absolute() and ".." not in candidate.parts:
        parts = candidate.parts[1:]
        if parts[:1] == ("materials",) and len(parts) >= 2:
            roots = _material_roots()
            root = roots.get(parts[1])
            if root is None:
                raise ValueError(
                    "materials root must be common, mobile or app")
            host = root.joinpath(*parts[2:])
            host.resolve().relative_to(root)
            return host
    raise ValueError("input path must be under /materials/")


def _t_input_list(args: dict) -> dict:
    rep = _require_reproduction()
    raw = str(args.get("path", "/materials")).replace("\\", "/")
    if raw.rstrip("/") == "/materials":
        entries = [{"name": name, "dir": True, "bytes": None}
                   for name in sorted(_material_roots())]
        return _ok([_text({"path": raw, "entries": entries})])
    target = _resolve_input_host(raw, rep)
    if not target.is_dir():
        raise ValueError("not a directory")
    entries = []
    for child in sorted(target.iterdir()):
        entries.append({"name": child.name, "dir": child.is_dir(),
                        "bytes": child.stat().st_size if child.is_file() else None})
    return _ok([_text({"path": raw, "entries": entries})])


def _t_input_read(args: dict) -> dict:
    rep = _require_reproduction()
    host = _resolve_input_host(args.get("path"), rep)
    if not host.is_file():
        raise ValueError("not a file")
    size = host.stat().st_size
    if size > _INPUT_MAX_READ:
        raise ValueError(f"file exceeds {_INPUT_MAX_READ} bytes")
    suffix = host.suffix.lower()
    if suffix in (".png", ".jpg", ".jpeg"):
        mime = "image/png" if suffix == ".png" else "image/jpeg"
        return _ok([
            _image_item(base64.b64encode(host.read_bytes()).decode("ascii"), mime),
            _text({"path": str(args.get("path")), "bytes": size}),
        ])
    content = host.read_text(encoding="utf-8")
    return _ok([_text(content)])


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


def _require_review() -> AndroidReproductionReview:
    if _review is None:
        raise ValueError("start_reproduction_review must be called first")
    return _review


def _t_start_reproduction_review(args: dict) -> dict:
    global _review
    workspace = _require_reproduction()
    if _review is None:
        _review = AndroidReproductionReview(
            workspace, max_revisions=_configured_review_revisions(),
            require_write_evidence=False)
    png, meta = _review.start_round()
    return _ok([
        _image_item(base64.b64encode(png).decode("ascii")),
        _text(meta),
    ])


def _t_review_observe(_args: dict) -> dict:
    png, meta = _require_review().observe()
    return _ok([
        _image_item(base64.b64encode(png).decode("ascii")),
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
    """List sample and operator-registered local Android targets."""
    try:
        _ensure_controller()
        r = _http.get(f"{_controller}/api/apps?platform=android", timeout=10)
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
        "adhoc": "用 scripts/android_target.py register 注册用户提供的本地 APK。",
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
    if not app_id:
        return _err("需要已注册的 Android app_id")
    try:
        get_android_target(app_id)
    except KeyError:
        return _err(f"未知 Android app_id: {app_id}")
    key = f"app:{app_id}"
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
    payload["app_id"] = app_id
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
    "x": {"type": "integer", "description": "当前截图中的像素横坐标"},
    "y": {"type": "integer", "description": "当前截图中的像素纵坐标"},
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
    properties = {k: _ACTION_PROPS[k] for k in req}
    if name in ("long_press", "swipe", "review_long_press", "review_swipe"):
        properties["duration_ms"] = _ACTION_PROPS["duration_ms"]
    return {"name": name, "description": desc,
            "inputSchema": {"type": "object",
                            "properties": properties,
                            "required": req}}


_TOOLS: dict[str, dict] = {}


def _register(schema: dict, handler) -> None:
    schema = dict(schema)
    schema["_handler"] = handler
    _TOOLS[schema["name"]] = schema


_register({"name": "observe",
           "description": "获取 Android App 当前可见截图和预算。这是唯一观察通道；没有控件树、selector、ADB、日志或网络数据。",
           "inputSchema": {"type": "object", "properties": {}}}, _t_observe)

_register({"name": "list_targets",
           "description": "列出可探索的本地 Android APK 目标及默认目标。",
           "inputSchema": {"type": "object", "properties": {}}}, _t_list_targets)

_register({"name": "start_session",
           "description": "选择已注册的 Android app_id 并启动独立模拟器会话；一个对话绑定一个目标。",
           "inputSchema": {"type": "object", "properties": {
               "app_id": {"type": "string", "description": "已注册 Android 目标 id"}},
               "required": ["app_id"]}},
          _t_start_session)

def _make_action_handler(name: str):
    def handler(args: dict) -> dict:
        return _t_action({**args, "type": name})
    return handler


for _n, _req, _d in [
    ("tap", ["x", "y"], "轻触当前截图中的像素坐标"),
    ("long_press", ["x", "y"], "长按坐标；可选 duration_ms"),
    ("swipe", ["x1", "y1", "x2", "y2"], "从起点滑动到终点；可选 duration_ms"),
    ("type_text", ["text"], "向当前已聚焦输入框输入一行文本"),
    ("press_back", [], "按 Android 返回键"),
    ("press_enter", [], "按 Android 确认键"),
    ("restart_app", [], "重启目标 App 以验证冷启动或持久化状态"),
    ("wait", ["ms"], "等待毫秒"),
]:
    _register(_action_tool(_n, _req, _d), _make_action_handler(_n))

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
           "description": "完成真实 Android 探索、定稿功能拓扑并立即启动隔离 APK 复现阶段。",
           "inputSchema": {"type": "object", "properties": {}}}, _t_finalize)

_register({"name": "input_list",
           "description": "仅在 finalize 后列出 /materials 下的公共素材"
                          "（common/mobile/app，含目标专属补充素材包）。",
           "inputSchema": {"type": "object", "properties": {
               "path": {"type": "string",
                        "description": "如 /materials、/materials/app"}}}},
          _t_input_list)

_register({"name": "input_read",
           "description": "仅在 finalize 后读取 /materials 素材中的文件"
                          "（PNG 返回图像，其余返回文本）。"
                          "路径必须在 /materials/ 之下。",
           "inputSchema": {"type": "object", "properties": {
               "path": {"type": "string",
                        "description": "如 /materials/app/CATALOG.md、"
                                       "/materials/app/data.json"}},
               "required": ["path"]}},
          _t_input_read)

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
           "description": "仅在 finalize 后于无网络 Android 复现沙箱运行程序；可用 gradle --offline assembleDebug 构建。",
           "inputSchema": {"type": "object", "properties": {
               "argv": {"type": "array", "items": {"type": "string"}},
               "cwd": {"type": "string"},
               "timeout_seconds": {"type": "integer", "minimum": 1,
                                   "maximum": 300}},
               "required": ["argv"]}}, _t_workspace_run)


def _make_review_action_handler(name: str):
    def handler(args: dict) -> dict:
        return _t_review_action({**args, "type": name})
    return handler


_register({"name": "start_reproduction_review",
           "description": "离线构建 APK，安装到独立 review 模拟器并启动像素级复验。",
           "inputSchema": {"type": "object", "properties": {}}},
          _t_start_reproduction_review)

_register({"name": "review_observe",
           "description": "获取复现 APK 当前可见截图；不提供控件树、ADB、日志或网络语义。",
           "inputSchema": {"type": "object", "properties": {}}},
          _t_review_observe)

for _n, _req, _d in [
    ("tap", ["x", "y"], "轻触复现 App 坐标"),
    ("long_press", ["x", "y"], "长按复现 App 坐标"),
    ("swipe", ["x1", "y1", "x2", "y2"], "在复现 App 中滑动"),
    ("type_text", ["text"], "向复现 App 当前焦点输入文本"),
    ("press_back", [], "按返回键"),
    ("press_enter", [], "按确认键"),
    ("restart_app", [], "重启复现 App 验证持久化"),
    ("wait", ["ms"], "等待复现 App 更新"),
]:
    _schema = _action_tool(f"review_{_n}", _req, _d)
    _register(_schema, _make_review_action_handler(_n))

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
           "description": "仅在像素级复验 accept 后冻结 Android 工程与 app_output 中的可安装 APK。",
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
