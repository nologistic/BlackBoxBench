"""web-review MCP stdio server — the judge-facing surface for web handoffs.

An LLM judge (running as the `web-review` Skill inside a commercial Agent)
drives this server turn by turn. It decides which flows to probe, how deep to
go, and whether each human requirement is functionally satisfied.

This server deliberately does NOT judge. It supplies pixels, human-like
input and one read-only source channel (a platform privilege of web
handoffs, evaluation contract §8), and enforces four structural rules so
that reports stay comparable across the four exploration conditions:

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
from web_evaluation.session import WebEvaluationSession

PROTOCOL_VERSION = "2025-06-18"
SERVER_NAME = "web-review"
SERVER_VERSION = "0.1.0"

# Outputs follow benchmark.config so BBB_* overrides reach the judge too.
PROJECT_ROOT = config.PROJECT_ROOT
WEBSITE_OUTPUT_ROOT = Path(config.WEBSITE_OUTPUT_DIR).resolve()
REVIEW_SPECS_ROOT = (config.PROJECT_ROOT / "review_specs").resolve()

_session: WebEvaluationSession | None = None
# NOTE: no checklist env fallback — see _resolve_checklist. A silent env
# default is the wrong-object trap behind the 2026-09-08 mis-evaluation.
_handoff_env = os.environ.get("BBB_WEB_REVIEW_HANDOFF", "")


def _log(msg: str) -> None:
    print(f"[web-review-mcp] {msg}", file=sys.stderr, flush=True)


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


def _handoff_app_id(handoff_name: str) -> str:
    """Resolve the source app of a handoff via its session id prefix.

    Handoffs are named ``<session_id>-<token>``; the session's session.json
    under RUNS_DIR records the target app. Judges need this to pair a
    checklist with the SAME app's handoff — without it they can only guess
    (2026-09-17: all seven web evaluations mispaired at least once).
    """
    sid = handoff_name.rsplit("-", 1)[0]
    meta = config.RUNS_DIR / sid / "session.json"
    try:
        return json.loads(meta.read_text(encoding="utf-8")).get("app_id") or ""
    except (OSError, ValueError):
        return ""


def _handoff_root(handoff: Path, depth: int = 3) -> Path | None:
    """Locate the entry-page root of a handoff.

    The entry may sit at the handoff root, or nested a level or two down:
    some generators copy the docker output verbatim, yielding
    ``<handoff>/website_output/<handoff>/index.html`` (2026-09-17: the notion
    and youtube handoffs were unfindable this way and could not be judged).
    """
    if (handoff / "index.html").is_file():
        return handoff
    if depth <= 0:
        return None
    for sub in sorted(handoff.iterdir()):
        if sub.is_dir():
            found = _handoff_root(sub, depth - 1)
            if found is not None:
                return found
    return None


def _resolve_handoff(value: str) -> Path:
    """Locate the handoff under website_output. Accepts a handoff id."""
    raw = (value or _handoff_env or "").strip()
    if not raw:
        raise ValueError("需要 handoff_id（website_output 下的交接目录名）")
    resolved = _resolve_under(WEBSITE_OUTPUT_ROOT, raw, "handoff")
    if not resolved.is_dir():
        raise ValueError("交接目录不存在")
    root = _handoff_root(resolved)
    if root is None:
        raise ValueError("交接目录缺少 index.html；先完成网页复现交接")
    return root


def _resolve_checklist(value: str) -> Checklist:
    raw = (value or "").strip()
    if not raw:
        # An absent checklist must NOT fall back to an environment default:
        # a silent default grades the wrong object (see the 2026-09-08
        # android incident). The checklist is chosen explicitly and the
        # caller is shown what exists.
        available = sorted(p.stem for p in REVIEW_SPECS_ROOT.glob("*.json"))
        raise ValueError(
            "需要显式指定 checklist，例如 start_evaluation("
            "checklist=\"<清单文件>\", handoff_id=\"...\")；"
            f"可用清单: {', '.join(available)}")
    resolved = _resolve_under(REVIEW_SPECS_ROOT, raw, "checklist")
    checklist = Checklist.load(resolved)
    checklist.expect_platform("web")
    return checklist


def _require_session() -> WebEvaluationSession:
    if _session is None:
        raise ValueError("尚未开始评测；先调用 start_evaluation")
    return _session


# ---------------------------------------------------------------------- tools

def _t_list_checklists(_args: dict) -> dict:
    """Show the human checklists available for web evaluation."""
    items = []
    if REVIEW_SPECS_ROOT.is_dir():
        for path in sorted(REVIEW_SPECS_ROOT.glob("*.json")):
            try:
                checklist = Checklist.load(path)
            except Exception:
                continue
            if checklist.platform not in ("web", "any"):
                continue
            items.append({"file": path.name,
                          "checklist_id": checklist.checklist_id,
                          "platform": checklist.platform,
                          "requirements": len(checklist.features)})
    handoffs = []
    if WEBSITE_OUTPUT_ROOT.is_dir():
        for child in sorted(WEBSITE_OUTPUT_ROOT.iterdir()):
            if child.is_dir() and _handoff_root(child) is not None:
                entry = {"handoff_id": child.name}
                app_id = _handoff_app_id(child.name)
                if app_id:
                    entry["app_id"] = app_id
                handoffs.append(entry)
    return _ok([_text({
        "checklists": items,
        "available_handoffs": handoffs,
        "grades": {grade.value: {"label": GRADE_LABELS[grade],
                                 "criteria": GRADE_CRITERIA[grade]}
                   for grade in Grade},
        "mcp_server": {"name": SERVER_NAME, "version": SERVER_VERSION},
    })])


def _t_start_evaluation(args: dict) -> dict:
    """Serve the handoff on loopback and open it in the locked browser."""
    global _session
    if _session is not None and not _session.finished:
        return _err("已有进行中的评测；先 finish_evaluation 或 abort_evaluation")
    raw_handoff = str(args.get("handoff_id") or "").strip()
    checklist = _resolve_checklist(args.get("checklist", ""))
    handoff = _resolve_handoff(raw_handoff)
    session = WebEvaluationSession(handoff=handoff, checklist=checklist)
    started = session.start()
    _session = session
    png, meta = session.observe()
    started.update(meta)
    started["checklist_id"] = checklist.checklist_id
    # Report the TOP-LEVEL handoff id the caller named — the ledger pairs
    # on it. A nested entry dir (e.g. <id>/website_output/<id> or <id>/dist)
    # must not masquerade as the handoff id; it travels separately.
    started["handoff_id"] = raw_handoff
    if handoff.name != raw_handoff:
        started["handoff_entry"] = handoff.name
    # The boundary travels with the first payload: the judge must see the
    # out-of-scope surfaces before planning probes, not one status call
    # later.
    exclusions = checklist.exclusions()
    if exclusions:
        started["exclusions"] = exclusions
    return _ok([_image(png), _text(started)])


def _t_observe(_args: dict) -> dict:
    png, meta = _require_session().observe()
    return _ok([_image(png), _text(meta)])


def _make_action_handler(name: str):
    def handler(args: dict) -> dict:
        return _ok([_text(_require_session().action(name, args))])
    return handler


def _t_read_source(args: dict) -> dict:
    """Read one source file inside the handoff (text sources, read-only)."""
    return _ok([_text(_require_session().read_source(
        str(args.get("path") or "")))])


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
    exclusions = session.checklist.exclusions()
    if exclusions:
        payload["exclusions"] = exclusions
    return _ok([_text(payload)])


def _t_finish_evaluation(_args: dict) -> dict:
    global _session
    session = _require_session()
    result = session.finish()
    _session = None
    return _ok([_text(result)])


def _t_abort_evaluation(_args: dict) -> dict:
    """Tear the browser down without producing a report."""
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
    "dx": {"type": "integer", "description": "水平滚动量（负值向左）"},
    "dy": {"type": "integer", "description": "垂直滚动量（负值向上）"},
    "key": {"type": "string",
            "description": "键名：Enter / Escape / Tab / Backspace / ArrowDown / BrowserBack 等"},
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
    return {"name": name, "description": desc,
            "inputSchema": {"type": "object", "properties": properties,
                            "required": req}}


_register({"name": "list_checklists",
           "description": "列出可用的人工功能要求清单、可评审的网页交接目录和四档判定标准。",
           "inputSchema": {"type": "object", "properties": {}}},
          _t_list_checklists)

_register({"name": "start_evaluation",
           "description": "把交接目录在本机静态服务并在锁定浏览器中打开，载入人工清单；返回首屏截图。",
           "inputSchema": {"type": "object", "properties": {
               "checklist": {"type": "string",
                             "description": "review_specs 下的清单文件名"},
               "handoff_id": {"type": "string",
                              "description": "website_output 下的交接目录名"}}}},
          _t_start_evaluation)

_register({"name": "observe",
           "description": ("获取当前可见截图——动态验证的唯一依据。另有 read_source 可读交接目录内"
                           "的源码文本；两者配合，但给分必须引用观察编号。"),
           "inputSchema": {"type": "object", "properties": {}}},
          _t_observe)

for _n, _req, _d in [
    ("click", ["x", "y"], "点击当前页面坐标"),
    ("type_text", ["text"], "向当前焦点输入文本"),
    ("scroll", ["dx", "dy"], "滚动页面（dx/dy 为滚动量）"),
    ("key", ["key"], "按键：Enter / Escape / Tab / Backspace / 方向键 / BrowserBack 等"),
    ("press_enter", [], "按 Enter"),
    ("reload", [], "刷新页面——网页侧持久化探针"),
    ("reset_browser", [], "冷重启浏览器（清 cookie/localStorage）——强持久化探针"),
    ("wait", ["ms"], "等待毫秒"),
]:
    _register(_action_tool(_n, _req, _d), _make_action_handler(_n))

_register({"name": "read_source",
           "description": ("只读交接目录内的一个源码文本文件（html/js/css/json 等）。这是网页评审"
                           "相对 App 评审的额外通道：用于静态定位入口与数据结构，"
                           "不能代替像素观察。"),
           "inputSchema": {"type": "object", "properties": {
               "path": {"type": "string",
                        "description": "交接目录内的相对路径"}},
               "required": ["path"]}},
          _t_read_source)

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
           "description": "放弃本次评测并关闭浏览器，不产出报告。",
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
            result = _err(f"tool crashed: {e!r}")
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
