"""our-method MCP stdio server (V0 baseline fork of BlackBoxBench managed mode — exposes the benchmark agent channel as MCP
tools so agent frameworks (Kimi Code, Claude Code, Codex, OpenCode…) can drive
the GUI exploration with their own planning loop.

Zero external dependencies: newline-delimited JSON-RPC 2.0 over stdio
(MCP stdio transport).

Two operation modes:
- bound mode: BBB_SESSION (+BBB_CONTROLLER) env set → serves that session
- self-bootstrap mode (no env): on first use, finds or starts the controller,
  creates a fresh session (BBB_APP_ID, default ecommerce_demo), and finalizes
  it when the stdio channel closes (agent conversation ended). This is what
  makes "new chat → /skill:our-method" work with zero terminal commands.
"""
from __future__ import annotations

import argparse
import base64
import hmac
import io
import json
import os
import re
import socket
import subprocess
import sys
import threading
import time
import traceback
from pathlib import Path, PurePosixPath

import httpx
from agents.our_method.reproduction_review import ManagedReproductionReview
from benchmark import config as benchmark_config
from benchmark.orchestrator.controller_process import (
    ensure_local_controller, start_local_controller)
from reproduction.workspace import (DockerUnavailableError, MATERIALS_ROOT,
                                    ReproductionWorkspace)

PROTOCOL_VERSION = "2025-06-18"
SERVER_NAME = "our-method"
SERVER_VERSION = "0.2.4"
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
# Last session this conversation finalized: the retry anchor for a failed
# reproduction handoff, kept independent of _session (whose binding may be
# released by the 404/410 path after the session closes).
_finalized_source = ""
# True when finalize succeeded but the reproduction workspace failed to
# start. In that state auto-creating a session (default target) would strand
# the conversation on the wrong app and block every switch-back, so the
# server keeps the retry-finalize path open instead.
_finalize_degraded = False
_reproduction: ReproductionWorkspace | None = None
_review: ManagedReproductionReview | None = None

_http = httpx.Client(timeout=120.0, trust_env=False)


def _log(msg: str) -> None:
    """stderr for the client console + append-only file for post-mortems.

    The stdio channel keeps no transcript: a handoff that fails inside a long
    tool call can outlive the client's request timeout, so the failure must
    survive in a file. Logging must never break a tool call.
    """
    print(f"[our-method-mcp] {msg}", file=sys.stderr, flush=True)
    try:
        path = Path(os.environ.get("BBB_MCP_LOG")
                    or (benchmark_config.RUNS_DIR / "mcp_server.log"))
        with open(path, "a", encoding="utf-8") as fh:
            fh.write(f"{time.strftime('%Y-%m-%d %H:%M:%S')} "
                     f"[pid {os.getpid()}] [our-method-mcp] {msg}\n")
    except OSError:
        pass


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
    if _finalize_degraded:
        # Finalized; only the reproduction handoff failed. Auto-creating a
        # session here would silently bind the conversation to the default
        # target and make recovery impossible (start_session refuses to
        # switch a live binding). Keep the retry-finalize path instead.
        raise RuntimeError(
            f"探索已定稿（session {_finalized_source}）但复现工作区未启动。"
            f"请调用 finalize 重试复现启动；如需重新探索，请新开一个对话。")
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
    if _own_session and _session and not _finalized_source:
        # _finalized_source set → the session was already finalized (or a
        # handoff waits to be retried); a second finalize call is just 410.
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
            if _finalize_degraded:
                # The close is expected (finalize already succeeded); the
                # correct recovery is retrying the handoff, not rebuilding
                # an exploration session.
                return None, (f"HTTP {r.status_code}: {msg} —— 探索已定稿"
                              f"（session {_finalized_source}）但复现工作区未启动。"
                              f"请调用 finalize 重试复现启动；不要重建探索会话。")
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
    if data_b64.startswith("data:"):
        # defensive: strip a data-URI wrapper if one ever leaks in
        data_b64 = data_b64.split(",", 1)[-1]
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
    except Exception as exc:
        # never break an exploration, but keep the reason for post-mortems
        _log(f"image re-encode failed, sending the original "
             f"{mime}: {type(exc).__name__}: {exc}")
        return {"type": "image", "data": data_b64, "mimeType": mime}


# ------------------------------------------------------------------ tools

def _t_observe(_args: dict) -> dict:
    d, err = _post("/observe", {})
    if err:
        return _err(err)
    meta = {k: d[k] for k in ("frame_id", "timestamp", "width", "height",
                              "cursor", "budget", "brief", "tabs") if k in d}
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
    # V0.2 handoff index: mark feature-evidence frames so the reproducing
    # agent has a guided reading list instead of an undifferentiated pile.
    index_path = run_dir / "reproduction_index.md"
    try:
        index_path.write_text(_build_handoff_index(run_dir, files),
                              encoding="utf-8")
        files["INDEX.md"] = index_path
    except OSError:
        pass
    return files


def _build_handoff_index(run_dir: Path, files: dict[str, Path]) -> str:
    """Summarize the handoff bundle and flag frames cited by feature steps."""
    evidence: set[str] = set()
    states = features = edges = 0
    try:
        data = json.loads(
            (run_dir / "functional_topology.json").read_text(encoding="utf-8"))
        nodes = data.get("nodes", [])
        states = sum(1 for n in nodes if n.get("type") == "STATE")
        features = sum(1 for n in nodes if n.get("type") == "FEATURE")
        edges = len(data.get("edges", []))
        for node in nodes:
            # STATE.visual_evidence is a list of frame ids.
            for fid in (node.get("visual_evidence") or []):
                if fid is not None:
                    evidence.add("screenshots/frame_%06d.png" % int(fid))
            # FEATURE.evidence items carry before_frame/after_frame pairs.
            for ev in (node.get("evidence") or []):
                if not isinstance(ev, dict):
                    continue
                for key in ("before_frame", "after_frame"):
                    fid = ev.get(key)
                    if fid is not None:
                        evidence.add("screenshots/frame_%06d.png" % int(fid))
    except (OSError, ValueError, TypeError):
        pass
    shots = sorted(p for p in files if p.startswith("screenshots/"))
    cited = [p for p in shots if p in evidence]
    lines = [
        "# Handoff Index (our-method V0.2)",
        "",
        "- Screenshots: %d under `/exploration/screenshots/`" % len(shots),
        "- Topology: %d states / %d features / %d edges" % (
            states, features, edges),
        "- Topology docs: `/exploration/functional_topology.md`, "
        "`/input/functional_topology.json`",
        "- Materials: `/materials` (public synthetic assets, copy don't link)",
        "",
        "## Read before generating (enforced by finish_reproduction)",
        "1. Read `/exploration/functional_topology.md` (or the JSON).",
        "2. Read at least %d screenshots via input_read — evidence frames "
        "below first." % _configured_min_frame_reads(),
        "3. Build from what the frames show, not from prior knowledge of the "
        "target product.",
        "",
        "## Evidence frames cited by feature steps (%d)" % len(cited),
    ]
    if cited:
        lines += ["- `%s`" % p for p in cited]
    else:
        lines += ["- (topology cites no frames; sample the list below evenly)"]
    rest = [p for p in shots if p not in evidence]
    if rest:
        lines += ["", "## Remaining screenshots (%d)" % len(rest)]
        lines += ["- `%s`" % p for p in rest]
    return "\n".join(lines) + "\n"


# V0.2.1 asset-utilization gate: the reproducing agent must actually read the
# exploration evidence it was handed (baseline audits showed 0/106 frames
# and 0 topology reads during reproduction — workspace_read cannot reach
# /exploration at all, so dedicated read-only input tools below provide the
# channel).  Counters are bound to the current handoff_id and reset on
# finalize / finish / new conversation, so a long-lived MCP process never
# inherits a previous experiment's reads.
_asset_reads: dict = {"handoff": None, "frames": set(), "topology": set(),
                      "available_frames": 0}

_INPUT_MAX_READ = 2 * 1024 * 1024


def _reset_asset_reads(handoff_id: object = None,
                       available_frames: object = None) -> None:
    _asset_reads["handoff"] = handoff_id
    _asset_reads["frames"].clear()
    _asset_reads["topology"].clear()
    _asset_reads["available_frames"] = int(available_frames or 0)


def _note_asset_read(path_value: object) -> None:
    p = str(path_value or "")
    if p.startswith("/exploration/screenshots/") and p.endswith(".png"):
        _asset_reads["frames"].add(p)
    if "functional_topology" in p:
        _asset_reads["topology"].add(p)


def _asset_reads_current() -> bool:
    current = getattr(_reproduction, "handoff_id", None)
    return current is not None and _asset_reads["handoff"] == current


def _configured_min_frame_reads() -> int:
    try:
        return max(0, int(os.environ.get("BBB_OUR_MIN_FRAME_READS", "12")))
    except ValueError:
        return 12


def _required_frame_reads() -> int:
    """Configured minimum, clamped by the frames actually handed over."""
    if _asset_reads_current():
        return min(_configured_min_frame_reads(),
                   int(_asset_reads["available_frames"]))
    return _configured_min_frame_reads()


def _asset_gate_enabled() -> bool:
    return os.environ.get("BBB_OUR_REQUIRE_ASSET_READS", "1") != "0"


def _asset_gate_error() -> str | None:
    if not _asset_gate_enabled():
        return None
    if not _asset_reads_current():
        frames_read = 0
        topology_read = False
    else:
        frames_read = len(_asset_reads["frames"])
        topology_read = bool(_asset_reads["topology"])
    need = _required_frame_reads()
    missing = []
    if frames_read < need:
        missing.append(
            "exploration frames read %d/%d — input_read more of "
            "/exploration/screenshots/frame_*.png (see /exploration/INDEX.md "
            "for the evidence-frame reading list)" % (frames_read, need))
    if not topology_read:
        missing.append(
            "functional topology not read — input_read "
            "/exploration/functional_topology.md or "
            "/input/functional_topology.json")
    if not missing:
        return None
    return "asset utilization gate: " + "; ".join(missing)


def _prewrite_gate_error() -> str | None:
    """Material must be digested BEFORE any output mutation starts.

    Enforced by both workspace_write and workspace_run: the topology plus a
    first batch of frames must already be read, so the agent cannot generate
    blindly and only skim the evidence afterwards to satisfy completion.
    """
    if not _asset_gate_enabled():
        return None
    if not _asset_reads_current():
        return ("pre-write gate: read /exploration/INDEX.md and the topology "
                "via input_read before writing any output")
    need = min(4, _required_frame_reads())
    missing = []
    if not _asset_reads["topology"]:
        missing.append("read the topology first (input_read "
                       "/exploration/functional_topology.md)")
    if len(_asset_reads["frames"]) < need:
        missing.append(
            "read at least %d exploration frame(s) first (input_read "
            "/exploration/screenshots/frame_*.png, evidence frames listed in "
            "/exploration/INDEX.md)" % need)
    if not missing:
        return None
    return "pre-write gate: " + "; ".join(missing)


# V0.2.4: an exemption may only be used when the finalized topology itself
# shows no write-style features; a claim contradicted by the exploration is
# rejected at finish time.
_WRITE_FEATURE_HINT = re.compile(
    r"save|create|edit|new|write|publish|delete|favorite|remove|submit|"
    r"保存|创建|新建|编辑|发布|删除|收藏|移除|提交|写入|登录|注册|上传", re.IGNORECASE)


def _topology_has_write_features() -> bool:
    topology = getattr(_reproduction, "topology_path", None)
    if not topology:
        return False
    try:
        data = json.loads(Path(topology).read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return False
    def text_values(value: object):
        if isinstance(value, str):
            yield value
        elif isinstance(value, dict):
            for child in value.values():
                yield from text_values(child)
        elif isinstance(value, list):
            for child in value:
                yield from text_values(child)

    for node in data.get("nodes", []):
        if not isinstance(node, dict) or node.get("type") != "FEATURE":
            continue
        text = " ".join(text_values(node))
        if _WRITE_FEATURE_HINT.search(text):
            return True
    return False


def _exemption_conflict_error(review_summary: object) -> str | None:
    if not isinstance(review_summary, dict):
        return None
    rounds = review_summary.get("rounds") or []
    accepted_round = next((
        item for item in reversed(rounds)
        if isinstance(item, dict) and item.get("decision") == "accept"
    ), None)
    exempted = bool(accepted_round and
                    accepted_round.get("write_flow_exemption"))
    if exempted and _topology_has_write_features():
        return ("write_flow_exemption conflicts with the finalized topology: "
                "the exploration recorded write-style features; verify one "
                "write flow with write_flow_evidence instead of exempting")
    return None


def _resolve_input_host(path_value: object, rep: ReproductionWorkspace) -> Path:
    """Map a whitelisted sandbox input path to its host-side file.

    Allowed roots: /exploration (handoff bundle), /materials (public
    synthetic assets), /input/functional_topology.json (finalized topology).
    Everything else — including /workspace output and any traversal — is
    rejected, so output-directory path rules stay untouched.
    """
    raw = str(path_value or "").replace("\\", "/")
    candidate = PurePosixPath(raw)
    if candidate.is_absolute() and ".." not in candidate.parts:
        parts = candidate.parts[1:]
        if parts[:1] == ("exploration",) and len(parts) >= 2:
            root = rep.artifact_dir
            if root is None:
                raise ValueError("/exploration is not available")
            host = root.joinpath(*parts[1:])
            host.resolve().relative_to(Path(root).resolve())
            return host
        if parts[:1] == ("materials",) and len(parts) >= 2:
            host = Path(MATERIALS_ROOT).resolve().joinpath(*parts[1:])
            host.resolve().relative_to(Path(MATERIALS_ROOT).resolve())
            return host
        if parts == ("input", "functional_topology.json"):
            if rep.topology_path is None:
                raise ValueError("/input/functional_topology.json is not available")
            return Path(rep.topology_path)
    raise ValueError(
        "input path must be under /exploration/, /materials/, or "
        "/input/functional_topology.json")


def _t_input_list(args: dict) -> dict:
    rep = _require_reproduction()
    raw = str(args.get("path", "/exploration")).replace("\\", "/")
    # list only whole whitelisted roots or directories below them
    if raw.rstrip("/") in ("/exploration", "/materials", "/input"):
        if raw.rstrip("/") == "/input":
            # V0.2.2: never list the session directory that hosts the
            # topology file — expose exactly the whitelisted file itself.
            entries = []
            if rep.topology_path is not None:
                entries.append({"name": "functional_topology.json",
                                "dir": False,
                                "bytes": Path(rep.topology_path).stat().st_size})
            return _ok([_text({"path": raw, "entries": entries})])
        roots = {
            "/exploration": rep.artifact_dir,
            "/materials": Path(MATERIALS_ROOT),
        }
        host_root = roots[raw.rstrip("/")]
        if host_root is None or not host_root.is_dir():
            return _ok([_text({"path": raw, "entries": []})])
        entries = []
        for child in sorted(host_root.iterdir()):
            entries.append({"name": child.name, "dir": child.is_dir(),
                            "bytes": child.stat().st_size if child.is_file() else None})
        return _ok([_text({"path": raw, "entries": entries})])
    # directory listing below /exploration or /materials
    target = _resolve_input_host(raw if "/" in raw[1:] else "/" + raw, rep)
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
    _note_asset_read(args.get("path"))
    suffix = host.suffix.lower()
    if suffix in (".png", ".jpg", ".jpeg"):
        mime = "image/png" if suffix == ".png" else "image/jpeg"
        return _ok([
            _image_item(base64.b64encode(host.read_bytes()).decode("ascii"), mime),
            _text({"path": str(args.get("path")), "bytes": size}),
        ])
    content = host.read_text(encoding="utf-8")
    return _ok([_text(content)])


def _t_finalize(_args: dict) -> dict:
    global _reproduction, _finalized_source, _finalize_degraded
    if _reproduction is not None:
        return _ok([_text({"exploration_finished": True,
                           **_reproduction.started_payload(),
                           "review": _review_payload()})])
    # Retry anchor first: after a successful finalize the session is closed,
    # and a later request may already have released the binding, so the
    # finalized id must not depend on _session staying alive.
    source_id = _finalized_source or _session
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
    if source_id:
        _finalized_source = source_id
    if os.environ.get("BBB_REPRODUCTION_AUTOSTART", "1") == "0":
        return _ok([_text({**d, "exploration_finished": True,
                           "reproduction_skipped": True})])
    try:
        run_dir = benchmark_config.RUNS_DIR / source_id
        handoff_files = _managed_exploration_files(run_dir)
        _reproduction = ReproductionWorkspace.start(
            source_mode="managed-tools", source_id=source_id,
            topology_path=topology,
            exploration_files=handoff_files)
        available = sum(1 for name in handoff_files
                        if name.startswith("screenshots/"))
        _reset_asset_reads(_reproduction.handoff_id, available)
    except DockerUnavailableError as exc:
        _finalize_degraded = True
        _log(f"reproduction handoff unavailable: {exc}\n{traceback.format_exc()}")
        return _err(str(exc))
    except Exception as exc:
        _finalize_degraded = True
        _log(f"reproduction handoff failed: {type(exc).__name__}: {exc}\n"
             f"{traceback.format_exc()}")
        return _err("exploration finalized but reproduction workspace failed to start; retry finalize")
    _finalize_degraded = False
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
    prewrite = _prewrite_gate_error()
    if prewrite is not None:
        raise ValueError(prewrite)
    return _ok([_text(_require_reproduction().write_file(
        args.get("path"), args.get("content")))])


def _t_workspace_run(args: dict) -> dict:
    if _review is not None and _review.active:
        raise ValueError(
            "complete the active reproduction review before running build commands")
    prewrite = _prewrite_gate_error()
    if prewrite is not None:
        raise ValueError(prewrite)
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
        findings=args.get("findings"),
        write_flow_exemption=args.get("write_flow_exemption"),
        write_flow_evidence=args.get("write_flow_evidence")))])


def _t_finish_reproduction(_args: dict) -> dict:
    global _reproduction, _review
    workspace = _require_reproduction()
    review = _require_review()
    review.ensure_accepted()
    gate_error = _asset_gate_error()
    if gate_error is not None:
        raise ValueError(gate_error)
    summary = review.summary()
    exemption_error = _exemption_conflict_error(summary)
    if exemption_error is not None:
        raise ValueError(exemption_error)
    result = workspace.finish(review_summary=summary)
    review.close()
    _review = None
    _reproduction = None
    _reset_asset_reads()
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
    if _finalize_degraded:
        # Exploration is done; the only valid continuation is retrying the
        # reproduction handoff (a fresh session would strand this
        # conversation on a second exploration of the same evidence).
        return _err(
            f"探索已定稿（session {_finalized_source}）但复现工作区未启动。"
            f"请调用 finalize 重试复现启动；如需重新探索，请新开一个对话。")
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
           "description": "选择本次探索目标并开始会话: app_id(已注册,如 yuque_web/ecommerce_demo) 或 url(任意网址,如 https://example.com)。一个对话绑定一个目标。",
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

_register({"name": "input_list",
           "description": "仅在 finalize 后列出只读交接输入（/exploration 探索素材、"
                          "/materials 公共素材、/input 拓扑）。这是查看探索截图与拓扑的唯一通道。",
           "inputSchema": {"type": "object", "properties": {
               "path": {"type": "string",
                        "description": "如 /exploration、/exploration/screenshots、/materials"}}}},
          _t_input_list)

_register({"name": "input_read",
           "description": "仅在 finalize 后读取只读交接输入中的文件：探索截图 PNG"
                          "（返回图像）、拓扑文档、素材文本。"
                          "路径必须在 /exploration/、/materials/ 或 /input/functional_topology.json 之下。",
           "inputSchema": {"type": "object", "properties": {
               "path": {"type": "string",
                        "description": "如 /exploration/screenshots/frame_000001.png、"
                                       "/exploration/INDEX.md、/exploration/functional_topology.md"}},
               "required": ["path"]}},
          _t_input_read)


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

_register({"name": "review_reload",
           "description": "以受信任的 F5 类人按键刷新复现网页，并记录为持久化复验动作。",
           "inputSchema": {"type": "object", "properties": {}}},
          _make_review_action_handler("reload"))

_register({"name": "review_close_tab",
           "description": "关闭复验浏览器的当前标签页并回到上一个。",
           "inputSchema": {"type": "object", "properties": {}}},
          _make_review_action_handler("close_tab"))

_register({"name": "complete_reproduction_review",
           "description": "结束当前复验轮次。发现问题选 revise，修改后再复验；核心流程正常且无私人信息时才选 accept。"
                          "accept 时必须提供 write_flow_evidence：对一条写路径（新建/编辑/保存类），"
                          "操作前、操作后和显式刷新后的三次 review_observe 编号。"
                          "服务器会校验写入区间含写类交互、截图变化、刷新动作存在，且刷新后未退回 before 状态。"
                          "目标确实没有任何写功能时，改用 write_flow_exemption 说明理由。",
           "inputSchema": {"type": "object", "properties": {
               "decision": {"type": "string", "enum": ["accept", "revise"]},
               "checked_flows": {"type": "array", "items": {"type": "string"},
                                 "minItems": 1},
               "findings": {"type": "array", "items": {"type": "string"},
                            "minItems": 1},
               "write_flow_evidence": {
                   "type": "array",
                   "description": "accept 必填（无豁免时）：[{flow, before_observation, "
                                  "after_observation, persisted_observation}]。三段式：操作前 "
                                  "review_observe；执行写操作；操作后 review_observe（须与操作前 "
                                   "不同且之间有写类交互）；调用 review_reload（推荐）或 "
                                   "review_key_press F5，或 BrowserBack+BrowserForward 后再 "
                                   "review_observe 引为 persisted。"
                                  "观察编号来自 review_observe 回执的 observations 字段。",
                   "items": {"type": "object", "properties": {
                       "flow": {"type": "string"},
                       "before_observation": {"type": "integer"},
                       "after_observation": {"type": "integer"},
                       "persisted_observation": {"type": "integer"}},
                       "required": ["flow", "before_observation",
                                    "after_observation",
                                    "persisted_observation"]}},
               "write_flow_exemption": {
                   "type": "string",
                   "description": "仅当目标没有任何写路径流程时，在 accept 时提供不可为空的理由"}},
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
    global _finalized_source, _finalize_degraded
    if _review is not None or _reproduction is not None or _own_session:
        try:
            _shutdown()
        except Exception:
            pass
    _session = os.environ.get("BBB_SESSION", "")
    _own_session = False
    _bound_target = ""
    _finalized_source = ""
    _finalize_degraded = False
    _reset_asset_reads()


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
