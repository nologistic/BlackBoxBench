"""app-review MCP stdio server — the judge-facing surface for Android APKs.

An LLM judge (running as the `app-review` Skill inside a commercial Agent)
drives this server turn by turn. It decides which flows to probe, how deep to
go, and whether each human requirement is functionally satisfied.

This server deliberately does NOT judge. It supplies pixels and coordinate
input, and enforces four structural rules so that reports stay comparable
across the four exploration conditions:

  1. every grade must cite observations that were actually captured
  2. grades come from a closed four-value set
  3. no requirement may be left ungraded when finishing
  4. the report shape and hashes are pinned

Zero external dependencies: newline-delimited JSON-RPC 2.0 over stdio.
"""
from __future__ import annotations

import argparse
import base64
import hmac
import io
import json
import os
import socket
import sys
import threading
from pathlib import Path

from benchmark import config
from app_evaluation.checklist import Checklist
from app_evaluation.grades import GRADE_CRITERIA, GRADE_LABELS, Grade
from app_evaluation.session import AppEvaluationSession

PROTOCOL_VERSION = "2025-06-18"
SERVER_NAME = "app-review"
SERVER_VERSION = "0.1.0"

# Outputs follow benchmark.config so BBB_* overrides (multi-runs-dir
# deployments) reach the judge too, not just the exploration side.
PROJECT_ROOT = config.PROJECT_ROOT
APP_OUTPUT_ROOT = Path(config.APP_OUTPUT_DIR).resolve()
RUNS_ROOT = Path(config.RUNS_DIR).resolve()
REVIEW_SPECS_ROOT = (config.PROJECT_ROOT / "review_specs").resolve()

_session: AppEvaluationSession | None = None
# NOTE: no checklist env fallback — see _resolve_checklist. A silent env
# default is the wrong-object trap behind the 2026-09-08 mis-evaluation.
_apk_env = os.environ.get("BBB_APP_REVIEW_APK", "")


def _log(msg: str) -> None:
    print(f"[app-review-mcp] {msg}", file=sys.stderr, flush=True)


def _ok(content: list[dict]) -> dict:
    return {"content": content, "isError": False}


def _err(text: str) -> dict:
    return {"content": [{"type": "text", "text": text}], "isError": True}


def _text(d: dict) -> dict:
    return {"type": "text", "text": json.dumps(d, ensure_ascii=False)}


def _image(png: bytes) -> dict:
    return {"type": "image",
            "data": base64.b64encode(png).decode("ascii"),
            "mimeType": "image/png"}


# ------------------------------------------------------------------ resolution

def _resolve_under(root: Path, value: str, what: str) -> Path:
    """Resolve a caller-supplied path and keep it inside `root`."""
    candidate = Path(value)
    if not candidate.is_absolute():
        candidate = root / candidate
    resolved = candidate.resolve()
    if resolved != root and root not in resolved.parents:
        raise ValueError(f"{what} must stay inside {root.name}")
    if resolved.is_symlink():
        raise ValueError(f"{what} must not be a symlink")
    return resolved


def _resolve_apk(value: str) -> Path:
    """Locate the APK under app_output. Accepts a handoff id for convenience."""
    raw = (value or _apk_env or "").strip()
    if not raw:
        raise ValueError("需要 apk 路径或 handoff_id")
    direct = Path(raw)
    if direct.suffix.lower() != ".apk":
        # treat as a handoff id: app_output/<handoff>/artifacts/app-debug.apk
        candidate = APP_OUTPUT_ROOT / raw / "artifacts" / "app-debug.apk"
    else:
        candidate = direct
    resolved = _resolve_under(APP_OUTPUT_ROOT, str(candidate), "apk")
    if not resolved.is_file():
        raise ValueError("找不到可安装 APK；先完成复现并生成 artifacts/app-debug.apk")
    return resolved


def _resolve_checklist(value: str) -> Checklist:
    raw = (value or "").strip()
    if not raw:
        # An absent checklist used to silently fall back to the environment
        # default, which on 2026-09-08 evaluated a clock reproduction
        # against the commerce-demo list: every item graded broken because
        # the judge was looking for a shopping app. With a 20-app dataset a
        # silent default is a wrong-object trap, so the checklist must be
        # chosen explicitly and the caller is shown what exists.
        available = sorted(p.stem for p in REVIEW_SPECS_ROOT.glob("*.json"))
        raise ValueError(
            "需要显式指定 checklist，例如 start_evaluation("
            "checklist=\"google_clock\", handoff_id=\"...\")；"
            f"可用清单: {', '.join(available)}")
    resolved = _resolve_under(REVIEW_SPECS_ROOT, raw, "checklist")
    checklist = Checklist.load(resolved)
    checklist.expect_platform("android")
    return checklist


def _infer_checklist_from_handoff(handoff: str) -> str:
    """Derive the checklist name from a handoff's exploration session.

    A handoff id is ``<session_id>-<hex>``; that session's session.json
    records the app it explored, and ``review_specs/<app_id>.json`` is the
    per-app checklist (the 20-target dataset keeps them one-to-one). This
    closes the gap where a judge task named only the handoff and could not
    know which checklist to pass. Returns "" when nothing certain derives.
    """
    raw = (handoff or "").strip()
    if not raw or raw.lower().endswith(".apk"):
        return ""
    if any(c not in "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNO"
           "PQRSTUVWXYZ0123456789_-" for c in raw):
        return ""
    session_id = raw.rsplit("-", 1)[0] if "-" in raw else raw
    meta = RUNS_ROOT / session_id / "session.json"
    try:
        data = json.loads(meta.read_text(encoding="utf-8"))
        app_id = str(data.get("app_id") or "").strip()
    except (OSError, ValueError):
        return ""
    if not app_id or any(c not in "abcdefghijklmnopqrstuvwxyz0123456789_-"
                         for c in app_id):
        return ""
    if (REVIEW_SPECS_ROOT / f"{app_id}.json").is_file():
        return app_id
    return ""


def _handoff_app_id(handoff_name: str) -> str:
    """Resolve the source app of a handoff via its session id prefix.

    Handoffs are named ``<session_id>-<token>``; the session's session.json
    under runs/ records the target app. Judges need this to pair a
    checklist with the SAME app's handoff - without it they can only guess.
    """
    sid = handoff_name.rsplit("-", 1)[0]
    meta = RUNS_ROOT / sid / "session.json"
    try:
        return json.loads(meta.read_text(encoding="utf-8")).get("app_id") or ""
    except (OSError, ValueError):
        return ""


def _require_session() -> AppEvaluationSession:
    if _session is None:
        raise ValueError("尚未开始评测；先调用 start_evaluation")
    return _session


# ---------------------------------------------------------------------- tools

def _t_list_checklists(_args: dict) -> dict:
    """Show the human checklists available for Android evaluation."""
    items = []
    if REVIEW_SPECS_ROOT.is_dir():
        for path in sorted(REVIEW_SPECS_ROOT.glob("*.json")):
            try:
                checklist = Checklist.load(path)
            except Exception:
                continue
            if checklist.platform not in ("android", "any"):
                continue
            items.append({"file": path.name,
                          "checklist_id": checklist.checklist_id,
                          "platform": checklist.platform,
                          "requirements": len(checklist.features)})
    handoffs = []
    if APP_OUTPUT_ROOT.is_dir():
        for child in sorted(APP_OUTPUT_ROOT.iterdir()):
            if (child / "artifacts" / "app-debug.apk").is_file():
                entry = {"handoff_id": child.name}
                app_id = _handoff_app_id(child.name)
                if app_id:
                    entry["app_id"] = app_id
                handoffs.append(entry)
    return _ok([_text({
        "checklists": items,
        "installable_handoffs": handoffs,
        "grades": {grade.value: {"label": GRADE_LABELS[grade],
                                 "criteria": GRADE_CRITERIA[grade]}
                   for grade in Grade},
        "mcp_server": {"name": SERVER_NAME, "version": SERVER_VERSION},
    })])


def _t_start_evaluation(args: dict) -> dict:
    """Install the APK in a fresh offline emulator and load the checklist."""
    global _session
    if _session is not None and not _session.finished:
        return _err("已有进行中的评测；先 finish_evaluation 或 abort_evaluation")
    checklist_arg = (args.get("checklist", "") or "").strip()
    apk_arg = (args.get("apk", "") or args.get("handoff_id", "")).strip()
    inferred = ""
    if not checklist_arg:
        # The judge task may name only the handoff; derive the checklist
        # from the exploration session that produced it (one-to-one with
        # the 20-target dataset). Still explicit-first: a passed checklist
        # always wins, and an underivable one fails loudly.
        inferred = _infer_checklist_from_handoff(apk_arg)
    checklist = _resolve_checklist(
        checklist_arg or (f"{inferred}.json" if inferred else ""))
    apk = _resolve_apk(apk_arg)
    session = AppEvaluationSession(
        apk=apk, checklist=checklist,
        package_name=str(args.get("package_name")
                         or "com.blackboxbench.reproduction"),
        launch_activity=str(args.get("launch_activity") or ".MainActivity"))
    started = session.start()
    _session = session
    png, meta = session.observe()
    started.update(meta)
    started["checklist_id"] = checklist.checklist_id
    started["checklist_inferred_from_handoff"] = bool(inferred) \
        and not checklist_arg
    started["apk_name"] = apk.name
    return _ok([_image(png), _text(started)])


def _t_observe(_args: dict) -> dict:
    png, meta = _require_session().observe()
    return _ok([_image(png), _text(meta)])


def _make_action_handler(name: str):
    def handler(args: dict) -> dict:
        return _ok([_text(_require_session().action(name, args))])
    return handler


def _t_record_result(args: dict) -> dict:
    """Grade one requirement. The judge owns the semantic decision."""
    session = _require_session()
    evidence = args.get("evidence_observations") or []
    if not isinstance(evidence, list):
        raise ValueError("evidence_observations 必须是观察编号数组")
    persistence = args.get("persistence_evidence")
    if persistence is not None and not isinstance(persistence, dict):
        raise ValueError("persistence_evidence 必须是对象")
    return _ok([_text(session.record_result(
        requirement_id=str(args.get("requirement_id") or ""),
        grade=str(args.get("grade") or ""),
        rationale=str(args.get("rationale") or ""),
        evidence_observations=[int(n) for n in evidence],
        persistence_evidence=persistence,
        not_verifiable_reason=args.get("not_verifiable_reason")))])


def _t_evaluation_status(_args: dict) -> dict:
    session = _require_session()
    payload = session.status_payload()
    payload["requirements"] = session.checklist.requirements()
    return _ok([_text(payload)])


def _t_finish_evaluation(_args: dict) -> dict:
    global _session
    session = _require_session()
    result = session.finish()
    _session = None
    return _ok([_text(result)])


def _t_abort_evaluation(_args: dict) -> dict:
    """Tear the emulator down without producing a report."""
    global _session
    if _session is None:
        return _ok([_text({"aborted": False, "note": "没有进行中的评测"})])
    _session.close()
    _session = None
    return _ok([_text({"aborted": True})])


# ------------------------------------------------------------------- registry

_ACTION_PROPS = {
    "x": {"type": "integer", "description": "当前截图中的像素横坐标"},
    "y": {"type": "integer", "description": "当前截图中的像素纵坐标"},
    "x1": {"type": "integer"}, "y1": {"type": "integer"},
    "x2": {"type": "integer"}, "y2": {"type": "integer"},
    "duration_ms": {"type": "integer"},
    "text": {"type": "string"},
    "ms": {"type": "integer"},
}

_TOOLS: dict[str, dict] = {}


def _register(schema: dict, handler) -> None:
    schema = dict(schema)
    schema["_handler"] = handler
    _TOOLS[schema["name"]] = schema


def _action_tool(name: str, req: list[str], desc: str) -> dict:
    properties = {k: _ACTION_PROPS[k] for k in req}
    if name in ("long_press", "swipe"):
        properties["duration_ms"] = _ACTION_PROPS["duration_ms"]
    return {"name": name, "description": desc,
            "inputSchema": {"type": "object", "properties": properties,
                            "required": req}}


_register({"name": "list_checklists",
           "description": "列出可用的人工功能要求清单、可安装的复现产物和四档判定标准。",
           "inputSchema": {"type": "object", "properties": {}}},
          _t_list_checklists)

_register({"name": "start_evaluation",
           "description": "在全新模拟器（隔离网络：仅经白名单代理放行，与探索环境一致）安装待测 APK 并载入人工清单；返回首屏截图。",
           "inputSchema": {"type": "object", "properties": {
               "checklist": {"type": "string",
                             "description": "review_specs 下的清单文件名"},
               "handoff_id": {"type": "string",
                              "description": "app_output 下的交接目录名"},
               "apk": {"type": "string",
                       "description": "可选：app_output 内的 APK 相对路径"},
               "package_name": {"type": "string"},
               "launch_activity": {"type": "string"}}}},
          _t_start_evaluation)

_register({"name": "observe",
           "description": "获取待测 App 当前可见截图。这是唯一观察通道；没有控件树、selector、ADB、日志或源码。",
           "inputSchema": {"type": "object", "properties": {}}},
          _t_observe)

for _n, _req, _d in [
    ("tap", ["x", "y"], "轻触待测 App 坐标"),
    ("long_press", ["x", "y"], "长按待测 App 坐标；可选 duration_ms"),
    ("swipe", ["x1", "y1", "x2", "y2"], "在待测 App 中滑动；可选 duration_ms"),
    ("type_text", ["text"], "向待测 App 当前焦点输入文本"),
    ("press_back", [], "按 Android 返回键"),
    ("press_enter", [], "按 Android 确认键"),
    ("restart_app", [], "重启待测 App，用于验证持久化"),
    ("reset_app", [], "清空待测 App 数据后重启，用于验证初始态"),
    ("wait", ["ms"], "等待毫秒"),
]:
    _register(_action_tool(_n, _req, _d), _make_action_handler(_n))

_register({"name": "record_result",
           "description": ("为一条人工要求给出四档判定。grade 取 full|partial|"
                           "placeholder|broken；必须引用已保存的观察编号，并如实说明"
                           "看到的可见行为。判定标准由你把握，服务端只校验证据结构。"),
           "inputSchema": {"type": "object", "properties": {
               "requirement_id": {"type": "string"},
               "grade": {"type": "string",
                         "enum": [g.value for g in Grade]},
               "rationale": {"type": "string",
                             "description": "依据可见行为的判定理由"},
               "evidence_observations": {
                   "type": "array", "items": {"type": "integer"},
                   "description": "支撑该判定的观察编号"},
               "persistence_evidence": {
                   "type": "object",
                   "description": ("持久化三段证据：before/after/persisted 观察编号；"
                                   "persistence 类要求判 full 时必填"),
                   "properties": {
                       "before_observation": {"type": "integer"},
                       "after_observation": {"type": "integer"},
                       "persisted_observation": {"type": "integer"}}},
               "not_verifiable_reason": {
                   "type": "string",
                   "description": "multi_user 等单机黑盒无法完整验证时的说明"}},
               "required": ["requirement_id", "grade", "rationale",
                            "evidence_observations"]}},
          _t_record_result)

_register({"name": "evaluation_status",
           "description": "查看清单全文、已评与未评要求。",
           "inputSchema": {"type": "object", "properties": {}}},
          _t_evaluation_status)

_register({"name": "finish_evaluation",
           "description": "全部要求评完后冻结四档报告；仍有未评要求时会被拒绝。",
           "inputSchema": {"type": "object", "properties": {}}},
          _t_finish_evaluation)

_register({"name": "abort_evaluation",
           "description": "放弃本次评测并关闭模拟器，不产出报告。",
           "inputSchema": {"type": "object", "properties": {}}},
          _t_abort_evaluation)


# ------------------------------------------------------------------ transport

def _handle(msg: dict) -> dict | None:
    method = msg.get("method", "")
    mid = msg.get("id")
    if method == "initialize":
        return {"jsonrpc": "2.0", "id": mid, "result": {
            "protocolVersion": PROTOCOL_VERSION,
            "capabilities": {"tools": {}},
            "serverInfo": {"name": SERVER_NAME, "version": SERVER_VERSION}}}
    if method in ("notifications/initialized", "initialized"):
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
        except FileNotFoundError as e:
            result = _err(str(e))
        except OSError:
            result = _err("evaluation operation failed")
        except Exception as e:
            # DeviceError is worded agent-safe upstream (no host paths, no
            # serials); anything else must not leak internals to the judge.
            if getattr(e, "agent_safe", False):
                result = _err(str(e))
            else:
                _log(f"tool {name} crashed: {e!r}")
                result = _err("evaluation tool failed internally")
        return {"jsonrpc": "2.0", "id": mid, "result": result}
    if mid is None:
        return None
    return {"jsonrpc": "2.0", "id": mid,
            "error": {"code": -32601, "message": f"method not found: {method}"}}


def _shutdown() -> None:
    global _session
    if _session is not None:
        try:
            _session.close()
        except Exception:
            pass
        _session = None


def _reset_conversation() -> None:
    _shutdown()


def _authorized(msg: object, token: str) -> bool:
    if not isinstance(msg, dict):
        return False
    params = msg.get("params")
    if msg.get("method") != "auth" or not isinstance(params, dict):
        return False
    supplied = params.get("token")
    return isinstance(supplied, str) and hmac.compare_digest(supplied, token)


def _serve_stream(lines, send, token: str | None = None) -> None:
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
            response = _handle(msg)
            if response is not None:
                send(response)
    finally:
        _shutdown()


def _serve_stdio() -> None:
    # MCP payloads are UTF-8 JSON. On Windows the console default (GBK) would
    # corrupt or drop non-ASCII payloads (Chinese requirement names, rationale
    # text). Pin both channels to UTF-8.
    sys.stdin = io.TextIOWrapper(sys.stdin.buffer, encoding="utf-8")
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")

    def send(payload: dict) -> None:
        sys.stdout.write(json.dumps(payload, ensure_ascii=False) + "\n")
        sys.stdout.flush()
    _serve_stream(sys.stdin, send)


def _peek_auth(conn: socket.socket) -> bytes | None:
    """Read the first line of a NEW connection (its auth message).

    The MCP is strict request/response: the client waits for the auth
    reply before sending anything else, so there is no buffered remainder
    to preserve. Bytes after the first newline would be a protocol
    violation and are dropped.
    """
    try:
        conn.settimeout(60)
        buf = b""
        while b"\n" not in buf and len(buf) < (1 << 20):
            chunk = conn.recv(65536)
            if not chunk:
                break
            buf += chunk
    except OSError:
        return None
    finally:
        conn.settimeout(None)
    if not buf:
        return None
    return buf.split(b"\n", 1)[0] + b"\n"


def _serve_tcp(host: str, port: int, token: str) -> None:
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind((host, port))
    server.listen(1)
    _log(f"listening on {host}:{server.getsockname()[1]}")
    lock = threading.Lock()
    holder: dict = {"conn": None}
    try:
        while True:
            conn, _addr = server.accept()
            # Zombie eviction (aligned with the explorer MCP): a container
            # proxy can swallow the client-side disconnect, and the dead
            # connection then holds the single slot forever. A NEW client
            # that proves the token takes over — the stale connection is
            # shut down (its reader unblocks and releases the lock). An
            # unproven connection never evicts a live session.
            if holder["conn"] is not None:
                first = _peek_auth(conn)
                ok = False
                if first is not None:
                    try:
                        ok = _authorized(
                            json.loads(first.decode("utf-8", "replace")),
                            token)
                    except ValueError:
                        ok = False
                if not ok:
                    try:
                        conn.close()
                    except OSError:
                        pass
                    continue
            else:
                first = None
            prev = holder["conn"]
            if prev is not None:
                try:
                    prev.shutdown(socket.SHUT_RDWR)
                except OSError:
                    pass
            holder["conn"] = conn
            if not lock.acquire(timeout=30):
                try:
                    conn.close()
                except OSError:
                    pass
                if holder["conn"] is conn:
                    holder["conn"] = None
                continue
            try:
                stream = conn.makefile("rwb")

                def send(payload: dict) -> None:
                    stream.write(
                        (json.dumps(payload, ensure_ascii=False) + "\n").encode("utf-8"))
                    stream.flush()

                def lines():
                    if first is not None:
                        yield first.decode("utf-8", errors="ignore")
                    for raw in stream:
                        yield raw.decode("utf-8", errors="ignore")

                _serve_stream(lines(), send, token)
            except Exception as e:
                _log(f"connection ended: {e!r}")
            finally:
                if holder["conn"] is conn:
                    holder["conn"] = None
                lock.release()
                try:
                    conn.close()
                except Exception:
                    pass
    finally:
        server.close()


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--transport", choices=["stdio", "tcp"],
                        default="stdio")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=0)
    parser.add_argument("--token", default="")
    args = parser.parse_args()
    if args.transport == "tcp":
        if len(args.token) < 16:
            raise SystemExit("TCP mode requires a --token of at least 16 characters")
        if args.host != "127.0.0.1":
            raise SystemExit("TCP mode must bind loopback")
        _serve_tcp(args.host, args.port, args.token)
    else:
        _serve_stdio()


if __name__ == "__main__":
    main()
