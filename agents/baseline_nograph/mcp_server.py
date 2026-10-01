"""Android baseline MCP: pixels, coordinate touch and finalized topology.

Zero external dependencies: newline-delimited JSON-RPC 2.0 over stdio
(MCP stdio transport).

Two operation modes:
- bound mode: BBB_SESSION (+BBB_CONTROLLER) env set → serves that session
- self-bootstrap mode (no env): on first use, finds or starts the controller,
  creates a fresh session (BBB_ANDROID_APP_ID, default android_commerce_demo), and finalizes
  it when the stdio channel closes (agent conversation ended). This is what
  makes "new chat → /skill:baseline-nograph" work with zero terminal commands.
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
# 最近一次绑定的会话 id：绑定被释放（410/死亡）后仍保留，供 finalize 回退取材。
_last_session = _session
_reproduction: AppReproductionWorkspace | None = None
_review: AndroidReproductionReview | None = None
# Self-review observation budget (2026-09-30: a zcode run verified 710 frames
# in round 1 without ever converging to a decision; the historical
# distribution across 26 accepted runs is 18–98 observations with median 36,
# so 100 covers every normal case and only stops runaway verification).
_review_obs_used = 0
_REVIEW_BUDGET = int(os.environ.get("BBB_REVIEW_BUDGET", "150"))

# Android session creation boots a fresh AVD clone, pins the guest
# locale (which restarts the framework) and installs the APK, so the
# first call legitimately takes minutes. A web-sized timeout would
# abandon a session that the controller is still building.
_http = httpx.Client(timeout=httpx.Timeout(600.0, connect=10.0),
                     trust_env=False)


def _log(msg: str) -> None:
    """stderr for the client console + append-only file for post-mortems.

    The stdio channel keeps no transcript: a failure inside a tool call can
    outlive the client's view, so the reason must survive in a file. Logging
    must never break a tool call.
    """
    print(f"[android-blackboxbench-mcp] {msg}", file=sys.stderr, flush=True)
    try:
        path = Path(os.environ.get("BBB_MCP_LOG")
                    or (benchmark_config.RUNS_DIR / "mcp_server.log"))
        with open(path, "a", encoding="utf-8") as fh:
            fh.write(f"{time.strftime('%Y-%m-%d %H:%M:%S')} "
                     f"[pid {os.getpid()}] [android-blackboxbench-mcp] {msg}\n")
    except OSError:
        pass


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
            return None, (f"HTTP {r.status_code}: {msg} — the session is gone and the binding has been released. "
                          f"Recovery: call start_session(app_id as before) "
                          f"to rebuild the session and continue exploring.")
        if r.status_code == 409:
            # A recoverable environment blip. The device is still healthy, so
            # this is not a fault to report or work around — the step simply did
            # not land. Say so plainly, without inviting a strategy change.
            return None, (f"{msg}. This step did not land; the device is still healthy — "
                          f"just retry the same action to carry on.")
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
    BBB_IMAGE_JPEG_QUALITY tunes quality, BBB_IMAGE_JPEG_MAX_EDGE (default
    2000) downscales to the client's image ceiling so clients pass the JPEG
    through untouched instead of re-encoding it (opencode re-encodes frames
    over 2000px, inflating them back to PNG). Any failure falls back to the
    original payload: compression must never break an exploration.
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
            max_edge = int(os.environ.get("BBB_IMAGE_JPEG_MAX_EDGE", "2000"))
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


def _t_finalize(args: dict) -> dict:
    global _reproduction
    if _reproduction is not None:
        return _ok([_text({"exploration_finished": True,
                           **_reproduction.started_payload(),
                           "review": _review_payload()})])
    # 重连后的新进程直接复用状态文件里的工作区（无参 = 本会话/唯一候选）：
    # 否则新进程没有 _last_session，会误报"没有可定稿的探索会话"而让 agent
    # 以为要重建探索会话（那会抢走审查的目标租约）。与 baseline 同步 ✓
    if not str(args.get("source_session") or "").strip() and _try_reattach():
        return _ok([_text({"exploration_finished": True,
                           **_reproduction.started_payload(),
                           "review": _review_payload()})])
    # 显式材料源（2026-09-21 增）：探索中途重建过会话时，交接材料必须取自
    # 探索主会话而非当前续接会话——后者往往只有零星几帧（v4.1f nonogram
    # 曾因此产出 2 帧的贫瘠 handoff）。材料源会话必须已定稿（有 topology）。
    requested = str(args.get("source_session") or "").strip()
    # 绑定已释放（finalize 半失败后会话 closed、后续调用 410）时，自动回退到
    # 最近会话的材料——若其已定稿，直接完成交接，agent 无需重建会话。
    if (not requested and not _session and _last_session
            and (benchmark_config.RUNS_DIR / _last_session /
                 "functional_topology.json").is_file()):
        requested = _last_session
        _log(f"finalize falling back to released session {requested} "
             "(topology present)")
    if requested and requested != _session:
        run_dir_req = benchmark_config.RUNS_DIR / requested
        if not run_dir_req.is_dir():
            return _err(f"source_session directory does not exist: {requested}")
        topo_req = run_dir_req / "functional_topology.json"
        if not topo_req.is_file():
            return _err("source_session has no functional_topology.json (the exploration is not finalized), "
                        "so it cannot serve as the handoff material source; finalize the exploration on that session first.")
        if _session:
            # 先结束当前（续接）会话，保持 finalize = 结束当前探索的语义
            _d, _err_text = _post("/finalize", {})
            if _err_text:
                return _err(_err_text)
        source_id = requested
        topology = topo_req
        d = {"topology_path": f"runs/{source_id}/functional_topology.json",
             "source_session": source_id}
    else:
        if not _session:
            # finalize means "finish the exploration I am bound to". Letting it
            # fall through to _post would lazily create a fresh session on the
            # default app — the path by which a dead Google Clock session once
            # handed off a blank commerce-demo topology: the agent believes it
            # is completing its own work while the MCP silently binds a
            # zero-exploration session, possibly on a different target. Refuse
            # and point at the recovery path instead.
            return _err("no exploration session to finalize: the original session is gone or none has started, and finalize will not "
                        "implicitly create one. Call start_session(app_id as before) to rebuild it, "
                        "finish the exploration and then finalize; if the main exploration session is already finalized, use "
                        "finalize(source_session=<main exploration session id>) to hand over its material directly.")
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
    _save_repro_state()
    _start_prewarm()
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
# recorded-evidence channel (/exploration, /input/functional_topology.json)
# belongs to the baseline condition.
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

    This ablation condition exposes /materials only: no discovery tools are
    registered, so there is no recorded exploration evidence (no /exploration
    bundle, no finalized topology) to read back.
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


def _t_workspace_patch(args: dict) -> dict:
    if _review is not None and _review.active:
        raise ValueError(
            "complete the active reproduction review before modifying output")
    return _ok([_text(_require_reproduction().patch_file(
        args.get("path"), args.get("old_text"), args.get("new_text")))])


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
        configured = int(os.environ.get("BBB_REPRO_REVISIONS", "6"))
    except ValueError:
        configured = 3
    return max(0, min(6, configured))


def _require_review() -> AndroidReproductionReview:
    if _review is None:
        raise ValueError("start_reproduction_review must be called first")
    return _review


def _t_start_reproduction_review(args: dict) -> dict:
    global _review
    workspace = _require_reproduction()
    if _review is None:
        _await_prewarm()   # 等后台把设备起好；绝不在预热中途再起一台（会同进程抢租约）
        if _prewarm_still_booting():
            raise RuntimeError("the review device is not ready yet (background prewarm unfinished).")
        _review = AndroidReproductionReview(
            workspace, max_revisions=_configured_review_revisions(),
            require_write_evidence=False)
        _adopt_prewarm(_review)
    png, meta = _review.start_round()
    _log(f"review start_round returned {len(png)} bytes")
    _save_repro_state()
    _log("review state saved")
    return _ok([
        _image_item(base64.b64encode(png).decode("ascii")),
        _text(meta),
    ])


def _t_review_observe(_args: dict) -> dict:
    global _review_obs_used
    _review_obs_used += 1
    if _review_obs_used > _REVIEW_BUDGET:
        raise ValueError(
            f"review observation budget exhausted ({_REVIEW_BUDGET} observations; "
            "override with BBB_REVIEW_BUDGET). You have gathered sufficient "
            "evidence — call complete_reproduction_review(decision=..., "
            "checked_flows=..., findings=...) NOW. Do not request more "
            "screenshots.")
    png, meta = _require_review().observe()
    if isinstance(meta, dict):
        meta = {**meta, "review_progress": {
            "observations_used": _review_obs_used, "budget": _REVIEW_BUDGET}}
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
    _clear_repro_state()  # 2026-10-02: finish 后清理状态文件，防止积累（30 个残留导致 reattach 唯一候选失效）
    return _ok([_text(result)])


def _t_list_targets(_args: dict) -> dict:
    """List sample and operator-registered local Android targets."""
    try:
        _ensure_controller()
        r = _http.get(f"{_controller}/api/apps?platform=android", timeout=10)
        r.raise_for_status()
        apps = r.json()
    except Exception as e:
        return _err(f"failed to fetch the target list: {e}")
    return _ok([_text({
        "default_target": _app_id,
        "registered": apps,
        "current_session": _session or None,
        "mcp_server": {"version": SERVER_VERSION,
                       "started_at": SERVER_STARTED_AT},
        "adhoc": "register a user-provided local APK with scripts/android_target.py register.",
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
    global _session, _own_session, _bound_target, _last_session
    _log(f"releasing binding to {_session} ({reason})")
    if _session:
        _last_session = _session   # 供 finalize 回退取材（若该会话已定稿）
    _session = ""
    _own_session = False
    _bound_target = ""


def _t_start_session(args: dict) -> dict:
    """Explicitly choose the exploration target for this conversation."""
    global _session, _own_session, _bound_target
    app_id = (args.get("app_id") or "").strip()
    if not app_id:
        return _err("a registered Android app_id is required")
    try:
        get_android_target(app_id)
    except KeyError:
        return _err(f"unknown Android app_id: {app_id}")
    key = f"app:{app_id}"
    resume_from = (args.get("resume_from", "") or "").strip()
    if _session:
        if _bound_session_running():
            if _bound_target == key:
                return _ok([_text({"session_id": _session, "target": key,
                                   "note": "already the current target; just continue exploring"})])
            return _err(f"this conversation is already bound to session {_session} ({_bound_target}). "
                        f"To change targets, finalize it first or open a new conversation.")
        # bound session died or was closed externally — release and rebind
        # instead of bricking the conversation (finalize is impossible then)
        _release_binding("session no longer running")

    # Resume a previously closed session (e.g. after provider quota
    # exhaustion): reopen it on the controller with its topology intact.
    if resume_from:
        try:
            _ensure_controller()
        except Exception as e:
            return _err(f"controller failed to start: {e}")
        try:
            r = _http.post(f"{_controller}/api/sessions/{resume_from}/reopen")
        except Exception as e:
            return _err(f"controller unreachable: {e}")
        if r.status_code == 200:
            body = r.json()
            _session = body["session_id"]
            _own_session = True
            _bound_target = f"app:{body.get('app_id', app_id)}"
            _log(f"session reopened: {_session} (app={_bound_target})")
            return _ok([_text({"session_id": _session, "target": _bound_target,
                               "note": "previous exploration session restored; topology and recorded findings intact — just continue exploring"})])
        try:
            msg = r.json().get("detail", r.text[:300])
        except Exception:
            msg = r.text[:300]
        return _err(f"session restore failed HTTP {r.status_code}: {msg}")
    try:
        _ensure_controller()
    except Exception as e:
        return _err(f"controller failed to start: {e}")
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
        return _err(f"session creation failed HTTP {r.status_code}: {msg}")
    body = r.json()
    _session = body["session_id"]
    _own_session = True
    _bound_target = key
    _log(f"session created: {_session} (target={key})")
    return _ok([_text({"session_id": _session,
                       "app_id": body.get("app_id"),
                       "brief": body.get("brief", "")})])


_ACTION_PROPS = {
    "x": {"type": "integer", "description": "pixel x-coordinate on the current screenshot"},
    "y": {"type": "integer", "description": "pixel y-coordinate on the current screenshot"},
    "x1": {"type": "integer"}, "y1": {"type": "integer"},
    "x2": {"type": "integer"}, "y2": {"type": "integer"},
    "duration_ms": {"type": "integer"},
    "text": {"type": "string"},
    "key": {"type": "string", "description": "Enter/Tab/Escape/Backspace/F5/BrowserBack/BrowserForward/ArrowDown/single char"},
    "dx": {"type": "integer"}, "dy": {"type": "integer"},
    "ms": {"type": "integer"},
    "tab_index": {"type": "integer",
                  "description": "tab index (0-based, in opening order; the reply's tabs field has count/active)"},
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
           "description": "Get the Android app's current visible screenshot and budget. This is the only observation channel; no view hierarchy, selector, ADB, logs, or network data.",
           "inputSchema": {"type": "object", "properties": {}}}, _t_observe)

_register({"name": "list_targets",
           "description": "List explorable local Android APK targets and the default target.",
           "inputSchema": {"type": "object", "properties": {}}}, _t_list_targets)

_register({"name": "start_session",
           "description": "Select a registered Android app_id and start an isolated emulator session; one conversation binds one target.",
           "inputSchema": {"type": "object", "properties": {
               "app_id": {"type": "string", "description": "registered Android target id"},
               "resume_from": {"type": "string", "description": "session ID to resume (continue after a quota interruption)"}},
               "required": ["app_id"]}},
          _t_start_session)

def _make_action_handler(name: str):
    def handler(args: dict) -> dict:
        return _t_action({**args, "type": name})
    return handler


for _n, _req, _d in [
    ("tap", ["x", "y"], "tap the pixel coordinates on the current screenshot"),
    ("long_press", ["x", "y"], "long-press coordinates; optional duration_ms"),
    ("swipe", ["x1", "y1", "x2", "y2"], "swipe from the start point to the end point; optional duration_ms"),
    ("type_text", ["text"], "type one line of text into the currently focused input field"),
    ("press_back", [], "press the Android back key"),
    ("press_enter", [], "press the Android enter key"),
    ("restart_app", [], "restart the target app to verify cold start or persisted state"),
    ("wait", ["ms"], "wait milliseconds"),
]:
    _register(_action_tool(_n, _req, _d), _make_action_handler(_n))

# baseline-nograph（消融条件下的"无图"变体）：探索记录完全自由。
# 本条件**不注册**任何 discovery 记录工具（record_state / record_feature /
# record_data / record_edge / record_hypothesis / resolve_hypothesis /
# revise）——agent 只持有探索接口（observe / action / finalize / 复现工具
# 链），自行决定是否记录、如何记录其探索发现；系统不做任何记录校验。
# 与 baseline 的差异仅此一处；探索纪律（像素黑盒、坐标触控）与复现流程
# 保持一致。


_register({"name": "finalize",
           "description": "Finish the real Android exploration, finalize the functional topology, and immediately start the isolated APK reproduction stage. "
                          "If the session was rebuilt mid-exploration, you must point source_session at the main exploration session, "
                          "or the handoff material will be taken from the sparse continuation session.",
           "inputSchema": {"type": "object", "properties": {
               "source_session": {
                   "type": "string",
                   "description": "main exploration session id (sess_..., must be finalized). Only needed when the currently bound "
                                  "session is not the main exploration session."}}}}, _t_finalize)

_register({"name": "input_list",
           "description": "List public materials under /materials (only after finalize) "
                          "(common/mobile/app, including the target-specific supplement pack).",
           "inputSchema": {"type": "object", "properties": {
               "path": {"type": "string",
                        "description": "e.g. /materials, /materials/app"}}}},
          _t_input_list)

_register({"name": "input_read",
           "description": "Read files from /materials (only after finalize) "
                          "(PNG returns an image, everything else returns text). "
                          "The path must stay under /materials/.",
           "inputSchema": {"type": "object", "properties": {
               "path": {"type": "string",
                        "description": "e.g. /materials/app/CATALOG.md, "
                                       "/materials/app/data.json"}},
               "required": ["path"]}},
          _t_input_read)

_register({"name": "workspace_list",
           "description": "List files in the reproduction output workspace (only after finalize).",
           "inputSchema": {"type": "object", "properties": {
               "path": {"type": "string"}}}}, _t_workspace_list)

_register({"name": "workspace_read",
           "description": "Read text or PNG/JPEG from the reproduction output (only after finalize).",
           "inputSchema": {"type": "object", "properties": {
               "path": {"type": "string"}}, "required": ["path"]}},
          _t_workspace_read)

_register({"name": "workspace_write",
           "description": "Write into the reproduction output workspace (only after finalize).",
           "inputSchema": {"type": "object", "properties": {
               "path": {"type": "string"}, "content": {"type": "string"}},
               "required": ["path", "content"]}}, _t_workspace_write)

_register({"name": "workspace_patch",
           "description": "Precise search-and-replace edit of an existing file in the reproduction output"
                          " (prefer it for small code changes over a full workspace_write)."
                          "old_text must match the file content exactly and be unique in the file, or it errors;"
                          "an empty new_text deletes that fragment.",
           "inputSchema": {"type": "object", "properties": {
               "path": {"type": "string"},
               "old_text": {"type": "string",
                            "description": "the text to replace (include enough context to make it unique)"},
               "new_text": {"type": "string",
                            "description": "the replacement text; an empty string deletes it"}},
               "required": ["path", "old_text", "new_text"]}},
          _t_workspace_patch)

_register({"name": "workspace_run",
           "description": "Run programs in the offline Android reproduction sandbox (only after finalize); build with gradle --offline assembleDebug.",
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
           "description": "Build the APK offline, install it onto an isolated review emulator, and start the pixel-level re-check.",
           "inputSchema": {"type": "object", "properties": {}}},
          _t_start_reproduction_review)

_register({"name": "review_observe",
           "description": "Get the reproduction APK's current visible screenshot; no view hierarchy, ADB, logs, or network semantics.",
           "inputSchema": {"type": "object", "properties": {}}},
          _t_review_observe)

for _n, _req, _d in [
    ("tap", ["x", "y"], "tap reproduction-app coordinates"),
    ("long_press", ["x", "y"], "long-press reproduction-app coordinates"),
    ("swipe", ["x1", "y1", "x2", "y2"], "swipe in the reproduction app"),
    ("type_text", ["text"], "type text into the focused element of the reproduction app"),
    ("press_back", [], "press the back key"),
    ("press_enter", [], "press the enter key"),
    ("restart_app", [], "restart the reproduction app to verify persistence"),
    ("wait", ["ms"], "wait for the reproduction app to update"),
]:
    _schema = _action_tool(f"review_{_n}", _req, _d)
    _register(_schema, _make_review_action_handler(_n))

_register({"name": "complete_reproduction_review",
           "description": "End the current review round. Choose revise when problems are found (fix, then re-review); choose accept only when the core flows work and no private information is present.",
           "inputSchema": {"type": "object", "properties": {
               "decision": {"type": "string", "enum": ["accept", "revise"]},
               "checked_flows": {"type": "array", "items": {"type": "string"},
                                 "minItems": 1},
               "findings": {"type": "array", "items": {"type": "string"},
                            "minItems": 1}},
               "required": ["decision", "checked_flows", "findings"]}},
          _t_complete_reproduction_review)

_register({"name": "finish_reproduction",
           "description": "Freeze the Android project and the installable APK in app_output (only after the pixel-level review was accepted).",
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
        _observe_tool_outcome(name, result)
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


# ============================================================================
# 与 baseline MCP 同步的【平台交互与稳定性】能力（2026-09-27 移植 ✓）
# 移植范围仅限：状态落盘/静默续接、预热（预构建+预启动+接管）、审计（错误分类与记账）。
# 本条件与 baseline 的唯一差异仍是【不提供结构化记录工具】——本次移植不涉及记录面。
# ============================================================================

_REPRO_STATE_NAME = "mcp_reproduction_state.json"

_RECORD_TOOL_PREFIX = "record_"

_prewarm: dict[str, object] = {}
_prewarm_lock = threading.Lock()
_prewarm_started = False
_prewarm_thread: threading.Thread | None = None

_TOOL_ERRORS: dict[str, object] = {
    "total": 0, "record": 0, "by_class": {}, "record_by_class": {},
    "by_tool": {}, "first_ts": None, "last_ts": None, "session": "",
}


def _state_path(session_id: str) -> Path:
    return benchmark_config.RUNS_DIR / (session_id or "_unbound") / _REPRO_STATE_NAME


def _state_paths() -> list[Path]:
    runs = benchmark_config.RUNS_DIR
    paths: list[Path] = []
    if _session:
        # 已知自己的会话：只认它，绝不跨会话兜底。
        # 否则无参 finalize 的 re-attach 可能认领【别的单】的工作区 —— 两个 agent
        # 同写一个复现工程并互相打断编译（2026-09-25 实测：tasks 的 agent 写进
        # gallery 的工程，出现 Unresolved reference 'GalleryApp'）。
        own = runs / _session / _REPRO_STATE_NAME
        if own.is_file():
            paths.append(own)
        return paths
    try:
        others = sorted((p for p in runs.glob(f"*/{_REPRO_STATE_NAME}")),
                        key=lambda p: p.stat().st_mtime, reverse=True)
    except OSError:
        others = []
    # 未绑定会话（重连的新进程）：只有候选唯一时才允许兜底（单 run 场景）；
    # 多单并行时绝不猜，让 agent 显式传 source_session。
    if len(others) > 1:
        _log(f"multiple reproduction states present ({len(others)}); "
             "no cross-session fallback; pass source_session explicitly")
        return []
    return others
    return paths


def _load_repro_state(source_id: str = "") -> dict | None:
    for path in _state_paths():
        try:
            state = json.loads(path.read_text(encoding="utf-8"))
        except Exception:
            continue
        if source_id and state.get("source_id") != source_id:
            continue
        return state
    return None


def _save_repro_state() -> None:
    """落盘生成阶段状态（含审查进度）；任何失败都吞掉，绝不影响工具行为。"""
    if _reproduction is None:
        return
    try:
        sid = _reproduction.source_id or _session
        state = _reproduction.to_state()
        state["review"] = {
            "active": bool(_review is not None and _review.active),
            "accepted": bool(_review is not None and _review.accepted),
            "revisions": int(getattr(_review, "revision_count", 0) or 0),
            "rounds": list(getattr(_review, "rounds", []) or []),
            "last_revise_hash": str(getattr(_review, "_last_revise_hash", "") or ""),
        }
        state["updated_at"] = time.strftime("%Y-%m-%dT%H:%M:%S%z")
        path = benchmark_config.RUNS_DIR / sid / _REPRO_STATE_NAME
        path.parent.mkdir(parents=True, exist_ok=True)
        tmp = path.with_name(path.name + ".tmp")
        tmp.write_text(json.dumps(state, ensure_ascii=False, indent=2),
                       encoding="utf-8")
        tmp.replace(path)
    except Exception as exc:
        _log(f"reproduction state save failed: {type(exc).__name__}: {exc}")


def _clear_repro_state(source_id: str = "") -> None:
    """交接完成（finish）后删除状态文件，避免之后误续接一个已销毁的沙箱。"""
    sid = source_id or _session
    if not sid:
        return
    path = benchmark_config.RUNS_DIR / sid / _REPRO_STATE_NAME
    try:
        path.unlink(missing_ok=True)
        path.with_name(path.name + ".tmp").unlink(missing_ok=True)
    except Exception:
        pass


def _try_reattach(hint: str = "") -> bool:
    """客户端重连后静默续接生成阶段（True = 已恢复，调用方无需报错）。

    hint = 显式 source_session 时只认那一单的状态（多单并行时防止认领别人
    的工作区：2026-09-25 实测 tasks 的 agent 写进 gallery 的工程）。
    """
    global _reproduction, _review, _session
    if _reproduction is not None:
        return True
    state = _load_repro_state(source_id=hint or (_session or ""))
    if not state:
        return False
    try:
        workspace = AppReproductionWorkspace.resume_from_state(state)
    except Exception as exc:
        _log(f"reproduction re-attach unavailable: {type(exc).__name__}: {exc}")
        return False
    _reproduction = workspace
    _finished = False   # 能 resume 就说明这一单还没走完
    if not _session:
        _session = workspace.source_id
    info = state.get("review") or {}
    try:
        if info.get("active") or info.get("accepted"):
            _review = AndroidReproductionReview(
                workspace, max_revisions=_configured_review_revisions(),
                require_write_evidence=False)
            _review.revision_count = int(info.get("revisions") or 0)
            _review.rounds = list(info.get("rounds") or [])
            _review._last_revise_hash = str(info.get("last_revise_hash") or "")
            if info.get("accepted"):
                _review.accepted = True
                # 必须连验收指纹一起恢复：review.ensure_accepted() 会拿
                # self._accepted_hash 与当前 project_fingerprint() 比对，空值会让
                # finish_reproduction 永远报 "Android project changed after
                # acceptance"（2026-09-25 在 ad546b 上实测，重连后必现）。
                _review._accepted_hash = str(
                    state.get("accepted_project_hash") or "")
            else:
                _review.resume_active_round()
    except Exception as exc:
        _log(f"review re-attach failed: {type(exc).__name__}: {exc}")
        _review = None
    _log(f"reproduction re-attached silently: handoff={workspace.handoff_id}")
    if _review is None:
        _start_prewarm()   # 新进程同样把审查设备提前热起来
    return True


def _start_prewarm() -> None:
    """后台预热（幂等、对模型完全无感、任何失败只写日志）。"""
    global _prewarm_started, _prewarm_thread
    if os.environ.get("BBB_REPRODUCTION_PREWARM", "1") == "0":
        return
    with _prewarm_lock:
        if _prewarm_started or _reproduction is None or _review is not None:
            return
        _prewarm_started = True
        _prewarm["pending"] = True
        workspace = _reproduction

    def _work() -> None:
        try:
            try:
                build = workspace.build_apk()
                with _prewarm_lock:
                    _prewarm["build"] = build
                    _prewarm["fingerprint"] = workspace.project_fingerprint()
                _log("prewarm: build ready")
            except Exception as exc:
                _log(f"prewarm build failed: {type(exc).__name__}: {exc}")
            try:
                review = AndroidReproductionReview(
                    workspace, max_revisions=_configured_review_revisions(),
                    require_write_evidence=False)
                number = len(review.rounds) + 1
                directory = (workspace.review_dir / f"round_{number:02d}"
                             / "runtime")
                runtime = None
                # 重连时可能残留着上一代 MCP 进程的 boot：租约/端口争用会让
                # 第一次失败（10s mutex 超时之类），退避后重试一次即可。
                for attempt in (1, 2):
                    try:
                        runtime = review.runtime_factory(review._spec(),
                                                         directory)
                        runtime.start()
                        break
                    except BaseException as exc:
                        import traceback as _tb
                        _log(f"prewarm runtime attempt {attempt} failed: "
                             + _tb.format_exc())
                        try:
                            runtime.stop()          # type: ignore[union-attr]
                        except Exception:
                            pass
                        runtime = None
                        if attempt == 1:
                            time.sleep(15)
                if runtime is None:
                    raise RuntimeError("prewarm runtime failed twice")
                with _prewarm_lock:
                    _prewarm["runtime"] = runtime
                _log("prewarm: review runtime ready")
            except BaseException:   # BaseException: 别让任何异常静默消失
                import traceback as _tb
                try:
                    _log("prewarm runtime failed: " + _tb.format_exc())
                except Exception:
                    pass
        finally:
            with _prewarm_lock:
                _prewarm["pending"] = False

    _prewarm_thread = threading.Thread(target=_work, name="repro-prewarm",
                                       daemon=True)
    _prewarm_thread.start()


def _await_prewarm(timeout: float = 180.0) -> None:
    """等后台预热把设备备好（有界）。

    必须等：预热线程正在起审查设备时，主线程若再起一台，两次 boot 会在同一
    进程内抢目标租约 —— `_pid_mutex` 把"自己的 pid"判为活锁，谁也过不去，
    直到 10s 超时报 `Android target lease mutex timed out`（2026-09-25 实测）。
    所以这里只等、不重起；等不到就让调用方走"稍后重试"的软失败。
    """
    th = _prewarm_thread
    if th is None:
        return
    th.join(max(0.0, timeout))


def _prewarm_still_booting() -> bool:
    with _prewarm_lock:
        return bool(_prewarm.get("pending")) and _prewarm.get("runtime") is None


def _adopt_prewarm(review: "AndroidReproductionReview") -> None:
    """把预热产物交给 review 对象；没有预热就正常走，不影响正确性。"""
    with _prewarm_lock:
        runtime = _prewarm.pop("runtime", None)
        build = _prewarm.pop("build", None)
        fingerprint = _prewarm.pop("fingerprint", "")
    if runtime is not None:
        review.warm_runtime = runtime
        _log("prewarm adopted: review runtime")
    if build is not None:
        review.warm_build = build
        review.warm_fingerprint = str(fingerprint or "")
        _log("prewarm adopted: build result")


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


def _http_status_of(text: str) -> int | None:
    i = text.find("HTTP ")
    if i >= 0:
        digits = text[i + 5:i + 8]
        if digits.isdigit():
            return int(digits)
    return None


def _tool_error_text(result: object) -> str:
    """从工具结果里提取错误文本；不是错误则返回 ""。"""
    if not isinstance(result, dict):
        return ""
    flagged = bool(result.get("isError"))
    texts = [item.get("text") for item in (result.get("content") or [])
             if isinstance(item, dict) and item.get("type") == "text"
             and isinstance(item.get("text"), str)]
    for text in texts:
        stripped = text.lstrip()
        if (stripped.startswith("Error") or "HTTP 4" in text or "HTTP 5" in text
                or "unreachable" in text or "tool crashed" in text
                or "bootstrap failed" in text or "workspace operation failed" in text):
            return text
    if flagged and texts:
        return texts[0]        # 标了 isError 但文本不显眼，也算
    return ""


def _classify_tool_error(text: str) -> str:
    if "evidence_invalid" in text:
        return "evidence"
    if ("Field required" in text or "missing" in text or "extra_forbidden" in text
            or "type_error" in text):
        return "schema"
    if ("HTTP 5" in text or "unreachable" in text or "bootstrap failed" in text
            or "timed out" in text or "timeout" in text or "refused" in text
            or "workspace operation failed" in text
            # 平台繁忙抖动（控制器 409 {'state': 'busy'}）与各类
            # 'did not complete this operation'：只用于审计口径，不改写
            # 交回模型的文本（2026-09-26 起平台不再自述处置建议）。
            or "did not complete this operation" in text
            or "'state': 'busy'" in text or '"state": "busy"' in text):
        return "environment"
    if "HTTP 4" in text:
        return "other_4xx"
    return "other"


def _note_tool_error(tool: str, text: str) -> None:
    now = time.strftime("%Y-%m-%dT%H:%M:%S%z")
    cls = _classify_tool_error(text)
    is_record = tool.startswith(_RECORD_TOOL_PREFIX)
    _TOOL_ERRORS["total"] = int(_TOOL_ERRORS["total"]) + 1
    if is_record:
        _TOOL_ERRORS["record"] = int(_TOOL_ERRORS["record"]) + 1
    by_class = _TOOL_ERRORS["by_class"]
    by_class[cls] = by_class.get(cls, 0) + 1
    if is_record:
        rb = _TOOL_ERRORS["record_by_class"]
        rb[cls] = rb.get(cls, 0) + 1
    by_tool = _TOOL_ERRORS["by_tool"]
    by_tool[tool] = by_tool.get(tool, 0) + 1
    if not _TOOL_ERRORS["first_ts"]:
        _TOOL_ERRORS["first_ts"] = now
    _TOOL_ERRORS["last_ts"] = now
    sid = _session or "_unbound"
    _TOOL_ERRORS["session"] = sid
    run_dir = benchmark_config.RUNS_DIR / sid
    run_dir.mkdir(parents=True, exist_ok=True)
    with (run_dir / "mcp_tool_errors.jsonl").open("a", encoding="utf-8") as fh:
        fh.write(json.dumps({
            "ts": now, "session_id": sid, "tool": tool,
            "record_tool": is_record, "class": cls,
            "http_status": _http_status_of(text), "message": text[:240],
        }, ensure_ascii=False) + "\n")
    (run_dir / "mcp_tool_error_summary.json").write_text(json.dumps({
        "session_id": sid, "updated_at": now,
        "total_errors": _TOOL_ERRORS["total"],
        "record_tool_errors": _TOOL_ERRORS["record"],
        "by_class": _TOOL_ERRORS["by_class"],
        "record_by_class": _TOOL_ERRORS["record_by_class"],
        "by_tool": _TOOL_ERRORS["by_tool"],
        "first_ts": _TOOL_ERRORS["first_ts"],
        "last_ts": _TOOL_ERRORS["last_ts"],
    }, ensure_ascii=False, indent=2), encoding="utf-8")
    _log(f"tool error [{cls}] {tool}: {text[:120]}")


def _observe_tool_outcome(tool: str, result: object) -> None:
    """tools/call 收尾调用；对工具调用零影响（异常全部吞掉）。

    只做审计记录：错误计数/分类写进 mcp_tool_errors.jsonl。**不改写交回模型的
    任何文本** —— 平台不替 Agent 解释、不指路（2026-09-26 移除原装饰层）。
    """
    try:
        text = _tool_error_text(result)
        if not text:
            return
        _note_tool_error(tool, text)
    except Exception:
        pass


if __name__ == "__main__":
    main()
