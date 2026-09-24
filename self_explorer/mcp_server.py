"""Generic workspace MCP for the isolated self-built-tool mode.

This server exposes coding-workspace operations, not browser exploration
operations. Programs always execute inside the isolated tool_builder
container. It is networkless for bundled targets and has ordinary egress for
explicit public-URL tasks; the Skill, not a URL allowlist, constrains that
egress. The reference app and managed BlackBoxBench implementation are not
mounted there.

Host-side defense in depth (self_explorer/hardening.py): while a session is
active the benchmark-private answer files are sealed away on the host, every
tool call is appended to run_dir/audit.jsonl outside the agent-visible
workspace, and finish_workspace refuses handover when workspace text matches
benchmark-private fingerprints (ground truth, reference app source, the
trusted device bridge, the managed implementation or other sessions'
artifacts).
"""
from __future__ import annotations

import argparse
import base64
import hmac
import io
import json
import os
import socket
import stat
import subprocess
import sys
import threading
import time
from pathlib import Path, PurePosixPath

from reproduction.workspace import ReproductionWorkspace
from . import hardening
from . import launcher

PROTOCOL_VERSION = "2025-06-18"
SERVER_NAME = "blackboxbench-self-built"
SERVER_VERSION = "2.6.0"
MAX_READ = 2 * 1024 * 1024
MAX_OUTPUT = 96 * 1024
PROJECT_ROOT = Path(__file__).resolve().parent.parent
REPRODUCTION_ARTIFACT_SUFFIXES = {
    ".png", ".jpg", ".jpeg", ".webp", ".gif", ".bmp",
    ".ppm", ".pgm", ".pbm", ".wav", ".mp3", ".ogg", ".mp4", ".webm",
    ".md", ".txt", ".json", ".jsonl", ".csv", ".tsv", ".yaml", ".yml",
}

_environment: launcher.Environment | None = None
_reproduction: ReproductionWorkspace | None = None
_seal: hardening.SealManager | None = None
_reproduction_app_id: str = ""


def _log(message: str) -> None:
    # Diagnostics must never become a control-flow dependency.  In particular,
    # some MCP clients stop consuming stderr after a decoding error; writing to
    # that closed pipe must not abort a workspace operation or its rollback.
    try:
        print(f"[self-built-workspace] {message}", file=sys.stderr, flush=True)
    except (OSError, UnicodeError):
        pass


def _ok(*content: dict) -> dict:
    return {"content": list(content), "isError": False}


def _err(message: str) -> dict:
    return {"content": [{"type": "text", "text": message}], "isError": True}


def _categorize_workspace_error(exc: BaseException) -> str:
    """Coarse failure category safe to surface on the Agent wire.

    Maps internal exceptions to short, infrastructure-free labels that let
    the operator (and the Agent) know what kind of thing failed without
    leaking Compose commands, host paths or seal contents.
    """
    name = type(exc).__name__
    msg = str(exc).lower()
    if isinstance(exc, TimeoutError):
        return "lock contention"
    if isinstance(exc, subprocess.CalledProcessError) or name == "CalledProcessError":
        return "compose command failed"
    if isinstance(exc, subprocess.TimeoutExpired):
        return "compose timeout"
    if isinstance(exc, FileNotFoundError) or "no such file" in msg:
        return "missing resource"
    if isinstance(exc, PermissionError):
        return "permission denied"
    if isinstance(exc, FileExistsError):
        return "resource already exists"
    return ""


def _text(value: object) -> dict:
    if isinstance(value, str):
        body = value
    else:
        body = json.dumps(value, ensure_ascii=False)
    return {"type": "text", "text": body}


def _require_environment() -> launcher.Environment:
    if _environment is None:
        raise ValueError("call begin_workspace first with an explicit target_url")
    return _environment


def _ensure_environment(target_url: str) -> launcher.Environment:
    global _environment
    normalized_url, target_mode = launcher.normalize_target_url(target_url)
    if _environment is not None:
        if _environment.target_url != normalized_url:
            raise ValueError("this workspace is already bound to a different target_url")
        return _environment
    if _environment is None:
        configured_app = os.environ.get("BBB_SELF_APP_ID", "ecommerce_demo")
        app_id = configured_app if target_mode == "bundled" else launcher.EXTERNAL_APP_ID
        # Seal before any untrusted container is started.  Besides closing the
        # brief start-before-seal window, this ordering makes begin_workspace a
        # transaction: a seal failure cannot leave an untracked Compose project
        # behind, and a launcher failure releases only this process's lease.
        _seal_begin(app_id)
        try:
            environment = launcher.start(configured_app, normalized_url)
        except Exception:
            _seal_end()
            raise
        _environment = environment
        _log(f"isolated environment started: {environment.session_id}")
    return _environment


def _audit(tool: str, ok: bool, **info: object) -> None:
    """Append an operator-side audit entry outside the agent-visible workspace."""
    env = _environment
    if env is None:
        return
    try:
        entry = {"ts": time.strftime("%Y-%m-%dT%H:%M:%S%z"),
                 "tool": tool, "ok": bool(ok), **info}
        with (env.run_dir / "audit.jsonl").open("a", encoding="utf-8") as handle:
            handle.write(json.dumps(entry, ensure_ascii=False) + "\n")
    except OSError:
        pass


def _audit_call(tool: str, args: object, result: dict) -> None:
    info: dict[str, object] = {}
    if isinstance(args, dict):
        if "path" in args:
            info["path"] = str(args.get("path"))
        if isinstance(args.get("argv"), list):
            info["argv"] = [str(item) for item in args["argv"]][:16]
    _audit(str(tool or "?"), not result.get("isError", False), **info)


def _seal_begin(app_id: str) -> None:
    """Seal benchmark-private answer files for the whole conversation
    (exploration AND reproduction), so neither phase can read them."""
    global _seal
    if _seal is not None:
        return
    if not hardening.seal_enabled():
        _log("ground-truth sealing disabled via BBB_SELF_SEAL=0")
        return
    try:
        _seal = hardening.SealManager(lambda: launcher.RUNS_ROOT)
        _seal.acquire("mcp-conversation",
                      hardening.seal_targets(PROJECT_ROOT, app_id))
    except Exception as exc:
        _seal = None
        # Fail closed: refuse to run an unsealable session.
        _log(f"sealing failed ({type(exc).__name__}); aborting session")
        raise


def _seal_end() -> None:
    global _seal
    if _seal is None:
        return
    try:
        _seal.release("mcp-conversation")
    finally:
        _seal = None


def _screen(workspace, phase: str, app_id: str,
            current_session: str | None) -> dict:
    """Integrity-screen a workspace; fails closed on screening errors."""
    try:
        fingerprints = hardening.collect_fingerprints(
            PROJECT_ROOT, app_id, runs_root=launcher.RUNS_ROOT,
            current_session=current_session)
        return hardening.screen_workspace(workspace, fingerprints, phase=phase)
    except Exception as exc:
        _log(f"integrity screening error: {type(exc).__name__}: {exc}")
        return {"blocked": [{"category": "screen_error", "file": "",
                             "hits": 0, "structural": False}],
                "warnings": [], "leaks": []}


def _integrity_message(report: dict) -> str:
    categories = sorted({item.get("category", "?") for item in report["blocked"]})
    files = sorted({item.get("file", "") for item in report["blocked"]
                    if item.get("file")})
    detail = f"; files: {', '.join(files[:5])}" if files else ""
    return ("integrity check failed: workspace content matches benchmark-private "
            f"material ({', '.join(categories)}){detail}. Remove content you did "
            "not produce yourself through exploration and retry.")


def _relative(value: object, *, allow_root: bool = True) -> PurePosixPath:
    raw = str(value or ".").replace("\\", "/")
    path = PurePosixPath(raw)
    if path.is_absolute() or ".." in path.parts:
        raise ValueError("path must stay inside the workspace")
    cleaned = PurePosixPath(*[p for p in path.parts if p not in ("", ".")])
    if not cleaned.parts and not allow_root:
        raise ValueError("a file path is required")
    return cleaned


def _host_path(relative: PurePosixPath, *, for_write: bool = False) -> Path:
    env = _require_environment()
    current = env.workspace
    parts = list(relative.parts)
    check_parts = parts[:-1] if for_write else parts
    for part in check_parts:
        current = current / part
        if current.exists() and stat.S_ISLNK(current.lstat().st_mode):
            raise ValueError("symbolic links are not accepted by workspace I/O")
    result = env.workspace.joinpath(*parts)
    if result.exists() and stat.S_ISLNK(result.lstat().st_mode):
        raise ValueError("symbolic links are not accepted by workspace I/O")
    return result


def _container_workdir(value: object) -> str:
    relative = _relative(value)
    return "/workspace" + ("/" + relative.as_posix() if relative.parts else "")


def _run_in_container(argv: list[str], cwd: str, timeout: int) -> subprocess.CompletedProcess[str]:
    env = _require_environment()
    if not argv or not all(isinstance(item, str) and item for item in argv):
        raise ValueError("argv must be a non-empty string array")
    return launcher._compose(
        env, "exec", "-T", "--workdir", cwd, "tool_builder", *argv,
        check=False, timeout=timeout,
    )


def _t_begin(args: dict) -> dict:
    if _reproduction is not None:
        return _err("a reproduction stage is already active")
    target_url = args.get("target_url")
    if not isinstance(target_url, str) or not target_url.strip():
        return _err("target_url is required")
    env = _ensure_environment(target_url)
    return _ok(_text({"ready": True, "target_url": env.target_url,
                      "browser_io_root": "/device"}))


def _t_list(args: dict) -> dict:
    if _reproduction is not None:
        return _ok(_text(_reproduction.list_files(args.get("path", "."))))
    relative = _relative(args.get("path", "."))
    path = _host_path(relative)
    if not path.is_dir():
        return _err("not a directory")
    entries = []
    for child in sorted(path.iterdir(), key=lambda p: p.name)[:500]:
        mode = child.lstat().st_mode
        kind = "symlink" if stat.S_ISLNK(mode) else "directory" if child.is_dir() else "file"
        entries.append({"name": child.name, "kind": kind,
                        "size": child.stat().st_size if kind == "file" else None})
    return _ok(_text({"path": relative.as_posix() or ".", "entries": entries}))


def _t_read(args: dict) -> dict:
    if _reproduction is not None:
        return _ok(*_reproduction.read_file(args.get("path")))
    relative = _relative(args.get("path"), allow_root=False)
    path = _host_path(relative)
    if not path.is_file():
        return _err("not a file")
    size = path.stat().st_size
    if size > MAX_READ:
        return _err(f"file exceeds {MAX_READ} bytes")
    suffix = path.suffix.lower()
    if suffix in (".png", ".jpg", ".jpeg"):
        mime = "image/png" if suffix == ".png" else "image/jpeg"
        return _ok({"type": "image", "data": base64.b64encode(path.read_bytes()).decode("ascii"),
                    "mimeType": mime},
                   _text({"path": relative.as_posix(), "bytes": size}))
    try:
        content = path.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        return _err("binary files can only be viewed when encoded as PNG or JPEG")
    return _ok(_text({"path": relative.as_posix(), "content": content}))


def _t_write(args: dict) -> dict:
    if _reproduction is not None:
        return _ok(_text(_reproduction.write_file(
            args.get("path"), args.get("content"))))
    relative = _relative(args.get("path"), allow_root=False)
    path = _host_path(relative, for_write=True)
    content = args.get("content")
    if not isinstance(content, str):
        return _err("content must be text")
    if len(content.encode("utf-8")) > MAX_READ:
        return _err(f"content exceeds {MAX_READ} bytes")
    path.parent.mkdir(parents=True, exist_ok=True)
    # Recheck after creating parents so a pre-existing link cannot be used.
    path = _host_path(relative, for_write=True)
    path.write_text(content, encoding="utf-8")
    return _ok(_text({"written": relative.as_posix(),
                      "bytes": len(content.encode('utf-8'))}))


def _t_run(args: dict) -> dict:
    if _reproduction is not None:
        return _ok(_text(_reproduction.run_program(
            args.get("argv"), args.get("cwd", "."),
            args.get("timeout_seconds", 30))))
    argv = args.get("argv")
    if not isinstance(argv, list):
        return _err("argv must be an array")
    timeout = int(args.get("timeout_seconds", 30))
    if not 1 <= timeout <= 120:
        return _err("timeout_seconds must be between 1 and 120")
    result = _run_in_container(argv, _container_workdir(args.get("cwd", ".")), timeout)
    stdout = result.stdout[-MAX_OUTPUT:]
    stderr = result.stderr[-MAX_OUTPUT:]
    return _ok(_text({"exit_code": result.returncode, "stdout": stdout,
                      "stderr": stderr, "truncated":
                      len(result.stdout) > MAX_OUTPUT or len(result.stderr) > MAX_OUTPUT}))


def _self_exploration_files(env: launcher.Environment) -> dict[str, Path]:
    """Select neutral, non-executable Agent artifacts without imposing a layout."""
    root = env.workspace.resolve()
    files: dict[str, Path] = {}
    for path in env.workspace.rglob("*"):
        if path.is_symlink() or not path.is_file():
            continue
        try:
            resolved = path.resolve()
            resolved.relative_to(root)
            relative = path.relative_to(env.workspace)
        except (OSError, ValueError):
            continue
        if hardening.is_dependency_path(relative):
            continue
        if path.suffix.lower() in REPRODUCTION_ARTIFACT_SUFFIXES:
            files[f"agent_artifacts/{relative.as_posix()}"] = resolved
    return files


def _t_finish(_args: dict) -> dict:
    global _environment, _reproduction, _reproduction_app_id
    if _reproduction is not None:
        report = _screen(_reproduction.output_dir, "reproduction",
                         _reproduction_app_id or "ecommerce_demo",
                         getattr(_reproduction, "source_id", None))
        if report["blocked"]:
            _audit("integrity_scan", ok=False, phase="reproduction",
                   blocked=report["blocked"], warnings=report["warnings"])
            return _err(_integrity_message(report))
        _audit("integrity_scan", ok=True, phase="reproduction",
               warnings=report["warnings"])
        result = _reproduction.finish()
        _reproduction = None
        _seal_end()
        return _ok(_text(result))
    env = _require_environment()
    _reproduction_app_id = env.app_id
    report = _screen(env.workspace, "exploration", env.app_id, env.session_id)
    if report["blocked"]:
        _audit("integrity_scan", ok=False, phase="exploration",
               blocked=report["blocked"], warnings=report["warnings"],
               leaks=report["leaks"])
        return _err(_integrity_message(report))
    _audit("integrity_scan", ok=True, phase="exploration",
           warnings=report["warnings"], leaks=report["leaks"])
    exploration_files = _self_exploration_files(env)
    launcher.stop(env.session_id)
    _reproduction = ReproductionWorkspace.start(
        source_mode="self-built-tools", source_id=env.session_id,
        topology_path=None,
        exploration_files=exploration_files)
    _environment = None
    return _ok(_text({"exploration_finished": True,
                      "artifacts_retained": True,
                      **_reproduction.started_payload()}))


_TOOLS = {
    "begin_workspace": ({
        "name": "begin_workspace",
        "description": "Start the isolated workspace for an explicit target URL.",
        "inputSchema": {"type": "object", "properties": {
            "target_url": {"type": "string"}},
            "required": ["target_url"], "additionalProperties": False},
    }, _t_begin),
    "list_files": ({
        "name": "list_files", "description": "List files below the isolated workspace only.",
        "inputSchema": {"type": "object", "properties": {
            "path": {"type": "string"}}},
    }, _t_list),
    "read_file": ({
        "name": "read_file", "description": "Read a file below the isolated workspace.",
        "inputSchema": {"type": "object", "properties": {
            "path": {"type": "string"}}, "required": ["path"]},
    }, _t_read),
    "write_file": ({
        "name": "write_file", "description": "Write a UTF-8 file below the isolated workspace.",
        "inputSchema": {"type": "object", "properties": {
            "path": {"type": "string"}, "content": {"type": "string"}},
            "required": ["path", "content"]},
    }, _t_write),
    "run_program": ({
        "name": "run_program",
        "description": "Run an argv-form program inside the isolated workspace container.",
        "inputSchema": {"type": "object", "properties": {
            "argv": {"type": "array", "items": {"type": "string"}},
            "cwd": {"type": "string"},
            "timeout_seconds": {"type": "integer", "minimum": 1, "maximum": 120}},
            "required": ["argv"]},
    }, _t_run),
    "finish_workspace": ({
        "name": "finish_workspace",
        "description": ("Finish the current stage. Successful exploration "
                        "transitions directly to isolated reproduction."),
        "inputSchema": {"type": "object", "properties": {}},
    }, _t_finish),
}


def _handle(message: dict) -> dict | None:
    mid = message.get("id")
    method = message.get("method")
    if method == "initialize":
        return {"jsonrpc": "2.0", "id": mid, "result": {
            "protocolVersion": message.get("params", {}).get("protocolVersion", PROTOCOL_VERSION),
            "capabilities": {"tools": {}},
            "serverInfo": {"name": SERVER_NAME, "version": SERVER_VERSION}}}
    if method in ("notifications/initialized", "notifications/cancelled"):
        return None
    if method == "ping":
        return {"jsonrpc": "2.0", "id": mid, "result": {}}
    if method == "tools/list":
        return {"jsonrpc": "2.0", "id": mid, "result": {
            "tools": [schema for schema, _ in _TOOLS.values()]}}
    if method == "tools/call":
        params = message.get("params", {})
        entry = _TOOLS.get(params.get("name", ""))
        if entry is None:
            return {"jsonrpc": "2.0", "id": mid, "error": {
                "code": -32602, "message": "unknown workspace operation"}}
        try:
            result = entry[1](dict(params.get("arguments", {}) or {}))
        except ValueError as exc:
            result = _err(f"workspace operation failed: {exc}")
        except (OSError, subprocess.SubprocessError) as exc:
            # OS/subprocess exception strings can include trusted host paths or
            # the Compose command. Keep that operator detail off the Agent wire,
            # but log the real cause to stderr so the operator can diagnose.
            _log(f"workspace operation failed ({type(exc).__name__}): {exc}")
            category = _categorize_workspace_error(exc)
            result = _err("workspace operation failed" + (
                f" ({category})" if category else ""))
        except Exception as exc:
            _log(f"unexpected error: {type(exc).__name__}: {exc}")
            category = _categorize_workspace_error(exc)
            result = _err("workspace operation failed" + (
                f" ({category})" if category else ""))
        _audit_call(params.get("name", ""), params.get("arguments") or {},
                    result)
        return {"jsonrpc": "2.0", "id": mid, "result": result}
    if mid is None:
        return None
    return {"jsonrpc": "2.0", "id": mid, "error": {
        "code": -32601, "message": "method not found"}}


def _shutdown() -> None:
    global _environment, _reproduction
    if _reproduction is not None:
        try:
            _reproduction.close()
        except Exception as exc:
            _log(f"reproduction cleanup failed: {type(exc).__name__}")
        finally:
            _reproduction = None
    _seal_end()
    if _environment is None:
        return
    try:
        launcher.stop(_environment.session_id)
        _log(f"environment stopped: {_environment.session_id}")
    except Exception as exc:
        _log(f"environment cleanup failed: {type(exc).__name__}")
    finally:
        _environment = None


def _reset_conversation() -> None:
    """A new client conversation starts from a clean lifecycle state."""
    global _reproduction_app_id
    if _reproduction is not None or _environment is not None:
        try:
            _shutdown()
        except Exception:
            pass
    _reproduction_app_id = ""


def _authorized(message: object, token: str) -> bool:
    if not isinstance(message, dict):
        return False
    params = message.get("params")
    if message.get("method") != "auth" or not isinstance(params, dict):
        return False
    supplied = params.get("token")
    return isinstance(supplied, str) and hmac.compare_digest(supplied, token)


def _serve_stream(lines, send, token: str | None = None) -> None:
    """Serve one line-delimited JSON-RPC conversation (stdio or TCP)."""
    authenticated = token is None
    try:
        for raw in lines:
            raw = raw.strip()
            if not raw:
                continue
            try:
                message = json.loads(raw)
            except json.JSONDecodeError:
                continue
            if not authenticated:
                if _authorized(message, token):
                    authenticated = True
                    send({"jsonrpc": "2.0", "id": message.get("id"),
                          "result": {"ok": True}})
                else:
                    send({"jsonrpc": "2.0", "id": message.get("id"),
                          "error": {"code": -32000,
                                    "message": "unauthorized"}})
                    return
                continue
            response = _handle(message)
            if response is not None:
                send(response)
    finally:
        _shutdown()


def serve_tcp(bind_host: str, port: int, token: str) -> None:
    """TCP transport for containerized agent clients.

    One conversation at a time; every connection resets the lifecycle (a new
    client connection equals a fresh stdio process). The server should bind
    127.0.0.1 so only this machine and Docker Desktop's container proxy can
    reach it; the token is mandatory defense in depth.

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
                message = json.loads(first.decode("utf-8", errors="replace"))
            except json.JSONDecodeError:
                message = None
            if not _authorized(message, token):
                self._reply({"jsonrpc": "2.0",
                             "id": message.get("id") if isinstance(message, dict) else None,
                             "error": {"code": -32000,
                                       "message": "unauthorized"}})
                return False
            self._reply({"jsonrpc": "2.0",
                         "id": message.get("id"),
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
    # Codex's stdio MCP reader requires UTF-8 on every pipe.  Windows Python
    # otherwise uses the active console code page for stderr even though the
    # JSON-RPC streams below are explicitly UTF-8.
    try:
        sys.stderr.reconfigure(encoding="utf-8", errors="backslashreplace")
    except (AttributeError, OSError):
        pass
    parser = argparse.ArgumentParser(
        description="Self-built workspace MCP (stdio, or TCP for containerized clients)")
    parser.add_argument("--tcp", metavar="[HOST:]PORT", default=None,
                        help="serve newline-delimited JSON-RPC over TCP instead of stdio")
    parser.add_argument("--token", default=os.environ.get("BBB_SELF_MCP_TOKEN", ""),
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
