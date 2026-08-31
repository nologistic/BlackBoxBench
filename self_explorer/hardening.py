"""Host-side hardening for the self-built-tool exploration condition.

The container boundary (docker-compose.self-explorer.yml) already keeps the
Agent away from the reference app.  This module closes the remaining HOST-side
channels that an agent client's own built-in tools (shell, file access,
search) could abuse:

1. Scoped storage — self-built runs live under the project's dedicated
   runs/self_built directory by default (BBB_SELF_RUNS_ROOT can override it).
   The Agent container still mounts only one session's workspace. Host-side
   clients require a tool allowlist or full client containerization because
   project-local storage is not a host-filesystem security boundary.
2. Answer sealing — sample_apps/*/ground_truth.json files are moved into a
   private, transactional seal directory while a session is active
   (BBB_SELF_SEAL=0 disables; seals are restored on release and orphans left
   by crashed processes are recovered automatically). The seal is a
   CROSS-PROCESS lease: several self-built sessions may run concurrently,
   the files return only when the last live holder is gone.
3. Integrity screening — before a workspace may finish, agent-written text is
   fingerprint-matched against benchmark-private sources (ground truth, the
   reference app source, the trusted device bridge, the managed
   implementation, other sessions' artifacts) and screened for
   implementation-leak patterns.
4. Audit support — tool usage is appended to run_dir/audit.jsonl, which lives
   outside the MCP-visible workspace.

This module deliberately imports nothing from benchmark/ or agents/: the two
exploration conditions must not share implementation (AGENTS.md).
"""
from __future__ import annotations

import json
import os
import re
import secrets
import shutil
import sys
import time
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent

# Mirrors benchmark/topology/models.py::LEAK_PATTERN.  Duplicated on purpose:
# importing the managed condition would itself be a boundary violation.
LEAK_PATTERN = re.compile(
    r"(/api/|\.tsx?\b|\.jsx\b|localhost|127\.0\.0\.1|0\.0\.0\.0|"
    r"\bSELECT\b|\bINSERT\b|\bUPDATE\s+\w+\s+SET\b|\bDELETE\s+FROM\b|"
    r"React|Vue\.js|\bVue\b|Angular|Svelte|Next\.js|Django|Flask|Rails|Laravel|"
    r"\bDOM\b|querySelector|xpath|css\s*selector|sourcemap|source\s*map|graphql|"
    r"src/[A-Za-z]|node_modules|webpack|vite)",
    re.IGNORECASE,
)


def _log(message: str) -> None:
    try:
        print(f"[self-hardening] {message}", file=sys.stderr, flush=True)
    except (OSError, UnicodeError):
        # Sealing and restoration must not depend on a diagnostic pipe.
        pass


# ------------------------------------------------------------------ runs root

def resolve_runs_root() -> Path:
    """Project-local self-built result root, with an explicit override."""
    override = os.environ.get("BBB_SELF_RUNS_ROOT", "").strip()
    if override:
        return Path(override).expanduser()
    return Path(__file__).resolve().parent.parent / "runs" / "self_built"


# ------------------------------------------------------------------ locking

class _FileLock:
    """Minimal cross-process advisory lock (byte-range lock on a lock file).

    Kept deliberately self-contained: this module shares no implementation
    with the managed condition (AGENTS.md). Windows uses msvcrt.locking,
    POSIX uses fcntl.flock; both release automatically when the owning
    process dies, which is what makes holder liveness detectable.
    """

    def __init__(self, path: Path, timeout: float = 60.0,
                 poll: float = 0.05) -> None:
        self._path = Path(path)
        self._timeout = timeout
        self._poll = poll
        self._fd: int | None = None

    def acquire(self) -> None:
        self._path.parent.mkdir(parents=True, exist_ok=True)
        fd = os.open(str(self._path), os.O_RDWR | os.O_CREAT, 0o600)
        deadline = time.monotonic() + self._timeout
        while True:
            os.lseek(fd, 0, os.SEEK_SET)
            if _try_lock_fd(fd):
                self._fd = fd
                return
            if time.monotonic() >= deadline:
                os.close(fd)
                raise TimeoutError(
                    f"could not acquire lock {self._path} "
                    f"within {self._timeout:.0f}s")
            time.sleep(self._poll)

    def release(self) -> None:
        if self._fd is None:
            return
        fd, self._fd = self._fd, None
        try:
            os.lseek(fd, 0, os.SEEK_SET)
            _unlock_fd(fd)
        finally:
            os.close(fd)

    def __enter__(self) -> "_FileLock":
        self.acquire()
        return self

    def __exit__(self, *_exc) -> None:
        self.release()


def _try_lock_fd(fd: int) -> bool:
    """Non-blocking exclusive lock of byte 0; True when acquired."""
    if os.name == "nt":
        import msvcrt
        try:
            msvcrt.locking(fd, msvcrt.LK_NBLCK, 1)
            return True
        except OSError:
            return False
    import fcntl
    try:
        fcntl.flock(fd, fcntl.LOCK_EX | fcntl.LOCK_NB)
        return True
    except OSError:
        return False


def _unlock_fd(fd: int) -> None:
    if os.name == "nt":
        import msvcrt
        try:
            msvcrt.locking(fd, msvcrt.LK_UNLCK, 1)
        except OSError:
            pass
    else:
        import fcntl
        try:
            fcntl.flock(fd, fcntl.LOCK_UN)
        except OSError:
            pass


class _InstanceLock:
    """Liveness token for one SealManager instance (one host process).

    The byte lock is held for the whole session lifetime; the OS releases it
    when the process dies, so any other process can cheaply distinguish a
    live holder from a crashed one by trying a non-blocking lock.
    """

    def __init__(self, path: Path) -> None:
        self._path = path
        self._fd: int | None = None

    @property
    def held(self) -> bool:
        return self._fd is not None

    def acquire(self) -> None:
        if self._fd is not None:
            return
        self._path.parent.mkdir(parents=True, exist_ok=True)
        fd = os.open(str(self._path), os.O_RDWR | os.O_CREAT, 0o600)
        if not _try_lock_fd(fd):  # instance ids are unique; never contended
            os.close(fd)
            raise RuntimeError(f"instance lock contention: {self._path}")
        self._fd = fd

    def release(self) -> None:
        if self._fd is None:
            return
        fd, self._fd = self._fd, None
        _unlock_fd(fd)
        os.close(fd)
        try:
            os.unlink(str(self._path))
        except OSError:
            pass


def _instance_alive(base: Path, instance: str) -> bool:
    """True while the process owning this instance lock still exists."""
    path = base / "holders" / f"{instance}.lock"
    if not path.is_file():
        return False
    try:
        fd = os.open(str(path), os.O_RDWR)
    except OSError:
        return False
    try:
        os.lseek(fd, 0, os.SEEK_SET)
        acquired = _try_lock_fd(fd)
        if acquired:
            _unlock_fd(fd)
        return not acquired
    finally:
        os.close(fd)


# ------------------------------------------------------------------ sealing

def seal_enabled() -> bool:
    return os.environ.get("BBB_SELF_SEAL", "1") != "0"


def seal_targets(project_root: Path, app_id: str | None = None) -> list[Path]:
    """Benchmark-private host files hidden while a self-built session runs."""
    root = Path(project_root)
    targets: list[Path] = sorted((root / "sample_apps").glob("*/ground_truth.json"))
    if app_id and os.environ.get("BBB_SELF_SEAL_APP_SOURCE", "0") == "1":
        app_dir = root / "sample_apps" / app_id
        targets.extend(
            p for p in sorted(app_dir.rglob("*"))
            if p.is_file() and p.suffix.lower() in (".py", ".html"))
        targets = list(dict.fromkeys(targets))
    return targets


def _same_bytes(a: Path, b: Path) -> bool:
    try:
        return a.read_bytes() == b.read_bytes()
    except OSError:
        return False


class SealManager:
    """Cross-process, transactional sealing of benchmark-private host files.

    ``acquire`` moves each file into ``<runs_root>/.seal/active/`` and
    registers this manager instance as a lease holder; ``release`` drops the
    lease, restoring the files only when the LAST live holder is gone.  The
    holder registry is a set of per-instance lock files (``holders/<id>.lock``)
    whose byte locks vanish with the owning process, so a crashed holder is
    detectable without trusting PIDs.  ``recover()`` restores seals left by
    dead holders only — a concurrent session from a live process never has
    its seal pulled out from under it.  The manifest records original paths,
    so restoration never guesses.

    Layout under <runs_root>/.seal/:
      .lock                  global metadata mutex (held only briefly)
      holders/<id>.lock      one live-lock per manager instance
      active/                sealed files + manifest.json
      seal-<ts>-<hex>/       legacy (pre-multi-session) seal dirs, restored
                             as orphans when encountered
    """

    MANIFEST_VERSION = 2

    def __init__(self, root_fn):
        # root_fn is a callable so tests can repoint the runs root lazily.
        self._root_fn = root_fn
        self._instance = f"{os.getpid()}-{secrets.token_hex(4)}"
        self._lock: _InstanceLock | None = None
        self._holders: set[str] = set()   # holder names registered by us

    # ------------------------------------------------------------ public api

    @property
    def _base(self) -> Path:
        return Path(self._root_fn()) / ".seal"

    @property
    def _active(self) -> Path:
        return self._base / "active"

    def acquire(self, holder: str, files) -> None:
        if not seal_enabled():
            return
        base = self._base
        base.mkdir(parents=True, exist_ok=True)
        if self._lock is None:
            self._lock = _InstanceLock(base / "holders" / f"{self._instance}.lock")
            self._lock.acquire()
        with _FileLock(base / ".lock"):
            self._recover_locked()
            manifest = self._read_manifest()
            if manifest is not None and manifest.get("files"):
                self._add_holder_locked(manifest, holder, files)
                return
            files = [Path(f) for f in files or []]
            present = [f for f in files if f.is_file()]
            if not present:
                if files:
                    _log("warning: no sealable files found at the expected paths")
                return
            active = self._active
            active.mkdir(parents=True, exist_ok=True)
            entries: list[dict] = [{"origin": str(origin),
                                    "sealed": f"{index:04d}{origin.name}"}
                                   for index, origin in enumerate(present)]
            # Write the plan BEFORE moving anything: a crash mid-acquire then
            # leaves a state="sealing" manifest that recovery can undo.
            self._write_manifest({"version": self.MANIFEST_VERSION,
                                  "state": "sealing",
                                  "holders": [self._holder_entry(holder)],
                                  "files": entries})
            try:
                for entry in entries:
                    shutil.move(entry["origin"],
                                str(active / entry["sealed"]))
            except Exception:
                for entry in entries:
                    try:
                        sealed = active / entry["sealed"]
                        if sealed.is_file() and not Path(entry["origin"]).exists():
                            shutil.move(str(sealed), entry["origin"])
                    except Exception:
                        pass
                self._write_manifest({"version": self.MANIFEST_VERSION,
                                      "state": "sealing",
                                      "holders": [], "files": entries})
                self._restore_dir(active)
                raise
            self._write_manifest({"version": self.MANIFEST_VERSION,
                                  "state": "active",
                                  "holders": [self._holder_entry(holder)],
                                  "files": entries})
            self._holders.add(holder)
            _log(f"sealed {len(entries)} benchmark-private file(s) for {holder!r}")

    def release(self, holder: str) -> None:
        if self._lock is None or holder not in self._holders:
            return
        base = self._base
        with _FileLock(base / ".lock"):
            manifest = self._read_manifest()
            if manifest is None:
                self._forget(holder)
                return
            holders = [h for h in manifest.get("holders", [])
                       if not (h.get("name") == holder
                               and h.get("instance") == self._instance)]
            if holders:
                manifest["holders"] = holders
                self._write_manifest(manifest)
                self._forget(holder)
                return
            restored = self._restore_dir(self._active)
            self._forget(holder)
        if restored:
            _log(f"restored {len(restored)} sealed file(s)")

    def recover(self) -> list[Path]:
        """Restore seals whose holders are all dead (operator escape hatch)."""
        base = self._base
        if not base.is_dir():
            return []
        with _FileLock(base / ".lock"):
            return self._recover_locked()

    def sealed_origins(self) -> list[Path]:
        manifest = self._read_manifest()
        if manifest is None:
            return []
        return [Path(e["origin"]) for e in manifest.get("files", [])]

    # ------------------------------------------------------------ internals

    def _holder_entry(self, holder: str) -> dict:
        return {"name": holder, "instance": self._instance,
                "pid": os.getpid(),
                "acquired_at": time.strftime("%Y-%m-%dT%H:%M:%S%z")}

    def _forget(self, holder: str) -> None:
        self._holders.discard(holder)
        if not self._holders and self._lock is not None:
            self._lock.release()
            self._lock = None

    def _read_manifest(self) -> dict | None:
        path = self._active / "manifest.json"
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            return None
        return data if isinstance(data, dict) else None

    def _write_manifest(self, data: dict) -> None:
        active = self._active
        active.mkdir(parents=True, exist_ok=True)
        tmp = active / f"manifest.json.{secrets.token_hex(4)}.tmp"
        tmp.write_text(json.dumps(data, indent=2), encoding="utf-8")
        os.replace(str(tmp), str(active / "manifest.json"))

    def _add_holder_locked(self, manifest: dict, holder: str, files) -> None:
        """Join the existing active seal (or extend it with new files)."""
        entries = manifest.setdefault("files", [])
        known = {e.get("origin") for e in entries}
        index = len(entries)
        active = self._active
        for origin in files or []:
            origin = Path(origin)
            if str(origin) in known or not origin.is_file():
                continue
            sealed_name = f"{index:04d}{origin.name}"
            shutil.move(str(origin), str(active / sealed_name))
            entries.append({"origin": str(origin), "sealed": sealed_name})
            known.add(str(origin))
            index += 1
        holders = manifest.setdefault("holders", [])
        if not any(h.get("name") == holder and h.get("instance") == self._instance
                   for h in holders):
            holders.append(self._holder_entry(holder))
        manifest["state"] = "active"
        self._write_manifest(manifest)
        self._holders.add(holder)

    def _recover_locked(self) -> list[Path]:
        """Drop dead holders and restore orphaned/legacy seal directories.

        Caller must hold the global .lock.  Live holders from other processes
        are left completely untouched.
        """
        base = self._base
        if not base.is_dir():
            return []
        restored: list[Path] = []
        # 1. garbage-collect lock files of dead instances
        holders_dir = base / "holders"
        if holders_dir.is_dir():
            for child in sorted(holders_dir.iterdir()):
                if child.is_file() and child.name.endswith(".lock") \
                        and child.stem != self._instance \
                        and not _instance_alive(base, child.stem):
                    try:
                        child.unlink()
                    except OSError:
                        pass
        # 2. legacy pre-multi-session seal dirs → orphans
        for child in sorted(base.iterdir()):
            if child.is_dir() and child.name.startswith("seal-") \
                    and (child / "manifest.json").is_file():
                restored.extend(self._restore_dir(child))
        # 3. the active seal
        manifest = self._read_manifest()
        if manifest is not None:
            state = manifest.get("state")
            if state == "sealing":
                # crashed mid-acquire: undo the partial move
                restored.extend(self._restore_dir(self._active))
            else:
                holders = manifest.get("holders", [])
                live = [h for h in holders
                        if h.get("instance") == self._instance
                        or _instance_alive(base, h.get("instance", ""))]
                if not live:
                    restored.extend(self._restore_dir(self._active))
                elif len(live) != len(holders):
                    manifest["holders"] = live
                    self._write_manifest(manifest)
        return restored

    def _restore_dir(self, active: Path) -> list[Path]:
        restored: list[Path] = []
        manifest_path = active / "manifest.json"
        try:
            data = json.loads(manifest_path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            data = {}
        for entry in data.get("files", []):
            origin = Path(entry["origin"])
            sealed = active / entry["sealed"]
            if not sealed.is_file():
                continue
            try:
                if origin.exists():
                    if _same_bytes(origin, sealed):
                        sealed.unlink()
                    else:
                        backup = origin.with_name(
                            f"{origin.name}.sealed-{int(time.time())}")
                        shutil.move(str(sealed), str(backup))
                        _log(f"conflict while restoring {origin.name}; "
                             f"sealed copy kept at {backup}")
                else:
                    origin.parent.mkdir(parents=True, exist_ok=True)
                    shutil.move(str(sealed), str(origin))
                    restored.append(origin)
            except OSError as exc:
                _log(f"restore failed for {origin}: {exc}")
        shutil.rmtree(active, ignore_errors=True)
        return restored


# ------------------------------------------------------------------ integrity

TEXT_SUFFIXES = {".txt", ".md", ".json", ".jsonl", ".csv", ".py", ".html",
                 ".htm", ".js", ".mjs", ".ts", ".css", ".sh", ".bash",
                 ".yaml", ".yml", ".toml", ".ini", ".cfg", ".log", ".xml",
                 ".svg"}
DEPENDENCY_ROOTS = frozenset({
    ".deps", ".venv", "venv", "node_modules", "site-packages",
    "dist-packages",
})
MAX_SCAN_FILES = 512
MAX_SCAN_BYTES = 2 * 1024 * 1024
MAX_FINGERPRINTS = 4000     # per category, keeps the scan bounded
MAX_CROSS_FILES = 400

MIN_LINE_LEN = {
    "ground_truth": 24,
    "app_source": 28,
    "device_protocol": 28,
    "managed_impl": 40,
    "cross_session": 48,
}
BLOCK_MIN_HITS = {
    "ground_truth": 3,
    "app_source": 2,
    "device_protocol": 1,
    "managed_impl": 2,
    "cross_session": 3,
}
# Verbatim presence of these strings is only explicable by reading the file.
GROUND_TRUTH_MARKERS = ("reference-ground-truth", "benchmark-private")
# Boilerplate lines that appear in almost any Python file — never fingerprints.
STOP_LINES = {
    "from __future__ import annotations",
    'if __name__ == "__main__":',
    "#!/usr/bin/env python3",
    "#!/usr/bin/env python",
    "import argparse",
    "import json",
    "import os",
    "import sys",
    "import time",
    "from pathlib import path",
}
PHASE_CATEGORIES = {
    # exploration: full screening, including implementation-leak patterns.
    "exploration": ("ground_truth", "app_source", "device_protocol",
                    "managed_impl", "cross_session"),
    # reproduction: the rebuilt site legitimately mirrors app behavior, so
    # app_source and cross_session are excluded to avoid false positives.
    "reproduction": ("ground_truth", "device_protocol", "managed_impl"),
}


def is_dependency_path(relative: Path | str) -> bool:
    """Whether a workspace-relative path is inside a conventional dependency root.

    These trees contain upstream code and licenses rather than Agent-authored
    observations. Scanning them creates broad fingerprint false positives and
    can consume the bounded scan before first-party files are reached.
    """
    parts = Path(relative).parts
    return bool(parts and parts[0].lower() in DEPENDENCY_ROOTS)


def _normalize(text: str) -> str:
    return re.sub(r"\s+", " ", text).strip().lower()


def _fingerprints_from(paths, min_len: int) -> set[str]:
    out: set[str] = set()
    for path in paths:
        try:
            content = Path(path).read_text(encoding="utf-8", errors="ignore")
        except OSError:
            continue
        for line in content.splitlines():
            normalized = _normalize(line)
            if len(normalized) >= min_len and normalized not in STOP_LINES:
                out.add(normalized)
    if len(out) > MAX_FINGERPRINTS:
        out = set(sorted(out)[:MAX_FINGERPRINTS])
    return out


def _device_protocol_files(project_root: Path) -> list[Path]:
    docker = project_root / "docker"
    names = ("raw_device_bridge.py", "runtime_rpc_server.py",
             "internal_gateway.py", "public_web_proxy.py",
             "reference_raw_entrypoint.sh",
             "reference_entrypoint.sh")
    return [docker / name for name in names]


def _managed_files(project_root: Path) -> list[Path]:
    files: list[Path] = []
    for package in ("benchmark", "agents", "agent_sdk"):
        files.extend(sorted((project_root / package).rglob("*.py")))
    return files


def _cross_session_files(project_root: Path, runs_root, current_session) -> list[Path]:
    roots = [project_root / "runs", project_root / "website_output"]
    if runs_root:
        roots.append(Path(runs_root))
    files: list[Path] = []
    seen: set[str] = set()
    for root in roots:
        key = str(root)
        if key in seen:
            continue
        seen.add(key)
        if not root.is_dir():
            continue
        for child in sorted(root.iterdir()):
            if not child.is_dir():
                continue
            if current_session and child.name == current_session:
                continue
            for path in child.rglob("*"):
                if len(files) >= MAX_CROSS_FILES:
                    return files
                if path.is_file() and path.suffix.lower() in (
                        ".json", ".md", ".txt", ".jsonl", ".csv"):
                    files.append(path)
    return files


def collect_fingerprints(project_root: Path, app_id: str,
                         runs_root=None, current_session: str | None = None) -> dict:
    """Benchmark-private content the agent must never reproduce verbatim."""
    root = Path(project_root)
    app_dir = root / "sample_apps" / app_id
    app_source = [
        p for p in sorted(app_dir.rglob("*"))
        if p.is_file() and p.suffix.lower() in (".py", ".html")
        and p.name != "ground_truth.json"
    ]
    return {
        "ground_truth": _fingerprints_from(
            sorted((root / "sample_apps").glob("*/ground_truth.json")),
            MIN_LINE_LEN["ground_truth"]),
        "app_source": _fingerprints_from(
            app_source, MIN_LINE_LEN["app_source"]),
        "device_protocol": _fingerprints_from(
            _device_protocol_files(root), MIN_LINE_LEN["device_protocol"]),
        "managed_impl": _fingerprints_from(
            _managed_files(root), MIN_LINE_LEN["managed_impl"]),
        "cross_session": _fingerprints_from(
            _cross_session_files(root, runs_root, current_session),
            MIN_LINE_LEN["cross_session"]),
    }


def screen_workspace(workspace, fingerprints: dict,
                     phase: str = "exploration") -> dict:
    """Match agent-written text against benchmark-private fingerprints.

    Returns {"blocked": [...], "warnings": [...], "leaks": [...]}.  Blocked
    entries carry the category and file name only — never matched content,
    because the report may travel back to the agent.
    """
    result: dict = {"blocked": [], "warnings": [], "leaks": []}
    workspace = Path(workspace)
    if not workspace.is_dir():
        return result
    categories = PHASE_CATEGORIES.get(phase, PHASE_CATEGORIES["exploration"])

    texts: list[tuple[Path, str, str]] = []   # (path, rel, normalized)
    raws: dict[str, str] = {}
    count = 0
    for path in sorted(workspace.rglob("*")):
        if count >= MAX_SCAN_FILES:
            break
        if path.is_symlink() or not path.is_file():
            continue
        relative = path.relative_to(workspace)
        if is_dependency_path(relative):
            continue
        if path.suffix.lower() not in TEXT_SUFFIXES:
            continue
        if path.stat().st_size > MAX_SCAN_BYTES:
            continue
        try:
            raw = path.read_text(encoding="utf-8", errors="ignore")
        except OSError:
            continue
        rel = relative.as_posix()
        texts.append((path, rel, _normalize(raw)))
        raws[rel] = raw
        count += 1

    for _path, rel, flat in texts:
        structural_gt = (
            "ground_truth" in categories
            and any(marker in flat for marker in GROUND_TRUTH_MARKERS))
        for category in categories:
            fps = fingerprints.get(category) or ()
            hits = 0
            for fp in fps:
                if fp and fp in flat:
                    hits += 1
                    if hits >= 8:
                        break
            structural = structural_gt if category == "ground_truth" else False
            if not hits and not structural:
                continue
            entry = {"category": category, "file": rel,
                     "hits": hits, "structural": structural}
            if structural or hits >= BLOCK_MIN_HITS[category]:
                result["blocked"].append(entry)
            else:
                result["warnings"].append(entry)

    if phase == "exploration":
        for rel, raw in raws.items():
            if LEAK_PATTERN.search(raw):
                result["blocked"].append(
                    {"category": "implementation_leak", "file": rel,
                     "hits": 1, "structural": False})
                result["leaks"].append(rel)
    return result
