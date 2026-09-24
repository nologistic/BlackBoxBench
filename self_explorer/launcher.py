"""Operator lifecycle for the isolated self-built-tool environment.

The launcher is intentionally outside the Agent container. It creates one
host artifact directory, starts a uniquely named Compose project, and mounts
only that directory into the untrusted workshop.

Run directories live under the project's dedicated runs/self_built directory
by default (see self_explorer/hardening.py::resolve_runs_root). Only the exact
per-session workspace is mounted into the Agent container. Legacy sessions in
the former external root and directly under <repo>/runs remain resolvable.
"""
from __future__ import annotations

import argparse
import ipaddress
import json
import os
import re
import secrets
import shutil
import subprocess
import sys
import time
from dataclasses import dataclass
from pathlib import Path
from urllib.parse import SplitResult, urlsplit, urlunsplit

from . import hardening

PROJECT_ROOT = Path(__file__).resolve().parent.parent
RUNS_ROOT = hardening.resolve_runs_root()
LEGACY_RUNS_ROOT = PROJECT_ROOT / "runs"
if os.name == "nt":
    _former_base = os.environ.get("LOCALAPPDATA") or str(
        Path.home() / "AppData" / "Local")
    FORMER_DEFAULT_RUNS_ROOT = Path(_former_base) / "blackboxbench-self"
else:
    FORMER_DEFAULT_RUNS_ROOT = Path.home() / ".blackboxbench-self"
COMPOSE_FILE = PROJECT_ROOT / "docker-compose.self-explorer.yml"
URL_COMPOSE_FILE = PROJECT_ROOT / "docker-compose.self-explorer.url.yml"
PROFILE_COMPOSE_FILE = PROJECT_ROOT / "docker-compose.self-explorer.profile.yml"
LIVE_STATE_ROOT = Path(os.environ.get(
    "BBB_RUNS_DIR", PROJECT_ROOT / "runs")) / "live_targets"
SESSION_RE = re.compile(r"^self_[0-9]{8}_[0-9]{6}_[a-f0-9]{8}$")
APP_RE = re.compile(r"^[a-zA-Z0-9_-]{1,64}$")
DEMO_TARGET_URL = "http://reference-app.internal:8200/"
EXTERNAL_APP_ID = "external_url"
MAX_TARGET_URL = 2048
_BLOCKED_EXTERNAL_NAMES = {
    "localhost", "localhost.localdomain", "host.docker.internal",
    "gateway.docker.internal", "metadata.google.internal",
}


def _run_roots() -> list[Path]:
    """Candidate run roots: the configured root first, then the legacy
    in-repo root so old sessions keep resolving."""
    return list(dict.fromkeys([
        Path(RUNS_ROOT), LEGACY_RUNS_ROOT, FORMER_DEFAULT_RUNS_ROOT]))


def _docker_command() -> str:
    """Resolve Docker even when Codex predates a per-user Desktop install."""
    configured = os.environ.get("BBB_DOCKER_BIN", "").strip()
    if configured:
        path = Path(configured)
        if path.is_file():
            return str(path)
        raise FileNotFoundError("BBB_DOCKER_BIN does not name a file")
    discovered = shutil.which("docker.exe" if os.name == "nt" else "docker")
    if discovered:
        return discovered
    if os.name == "nt":
        local = os.environ.get("LOCALAPPDATA", "")
        candidate = Path(local) / "Programs" / "DockerDesktop" / "resources" / "bin" / "docker.exe"
        if local and candidate.is_file():
            return str(candidate)
    raise FileNotFoundError("Docker CLI is not installed")


def _parse_windows_proxy(value: str) -> dict[str, str]:
    """Translate WinINet's scalar or protocol-map proxy syntax for BuildKit."""
    value = value.strip()
    if not value:
        return {}
    mapping: dict[str, str] = {}
    if ";" in value or "=" in value:
        for item in value.split(";"):
            key, separator, endpoint = item.partition("=")
            if separator and key.lower() in ("http", "https") and endpoint:
                mapping[key.lower()] = endpoint
    else:
        mapping["http"] = mapping["https"] = value

    def url(endpoint: str) -> str:
        return endpoint if "://" in endpoint else "http://" + endpoint

    http = mapping.get("http") or mapping.get("https")
    https = mapping.get("https") or mapping.get("http")
    result = {}
    if http:
        result["HTTP_PROXY"] = url(http)
    if https:
        result["HTTPS_PROXY"] = url(https)
    if result:
        result["NO_PROXY"] = "localhost,127.0.0.1,::1"
    return result


def _windows_proxy_environment() -> dict[str, str]:
    if os.name != "nt":
        return {}
    try:
        import winreg
        with winreg.OpenKey(
                winreg.HKEY_CURRENT_USER,
                r"Software\Microsoft\Windows\CurrentVersion\Internet Settings") as key:
            enabled, _ = winreg.QueryValueEx(key, "ProxyEnable")
            server, _ = winreg.QueryValueEx(key, "ProxyServer")
        return _parse_windows_proxy(server) if enabled else {}
    except (OSError, ValueError):
        return {}


@dataclass(frozen=True)
class Environment:
    session_id: str
    project_name: str
    app_id: str
    run_dir: Path
    workspace: Path
    target_url: str = DEMO_TARGET_URL
    target_mode: str = "bundled"

    @property
    def metadata_path(self) -> Path:
        return self.run_dir / "session.json"

    @property
    def is_external(self) -> bool:
        return self.target_mode == "external"


def normalize_target_url(value: str | None) -> tuple[str, str]:
    """Validate a task URL before it reaches the trusted browser container.

    This is the first SSRF boundary.  The trusted proxy performs the decisive
    DNS/IP check for every request as well, which also closes DNS rebinding and
    redirect-to-private-address cases.
    """
    raw = DEMO_TARGET_URL if value is None else str(value).strip()
    if not raw or len(raw) > MAX_TARGET_URL:
        raise ValueError("target_url must be a non-empty URL of at most 2048 characters")
    if any(ord(char) < 0x20 or char.isspace() or char == "\\" for char in raw):
        raise ValueError("target_url contains forbidden whitespace or control characters")
    try:
        parsed = urlsplit(raw)
        port = parsed.port
    except ValueError as exc:
        raise ValueError("target_url is malformed") from exc
    if parsed.scheme.lower() not in {"http", "https"} or not parsed.hostname:
        raise ValueError("target_url must use http or https and include a hostname")
    if parsed.username is not None or parsed.password is not None:
        raise ValueError("target_url must not contain credentials")

    hostname = parsed.hostname.rstrip(".").lower()
    if hostname == "reference-app.internal":
        if parsed.scheme.lower() != "http" or port not in (None, 8200):
            raise ValueError("the bundled target must use http on port 8200")
        netloc = "reference-app.internal:8200"
        normalized = urlunsplit(SplitResult(
            "http", netloc, parsed.path or "/", parsed.query, parsed.fragment))
        return normalized, "bundled"

    if port not in (None, 80, 443):
        raise ValueError("external target_url ports are limited to 80 and 443")
    if (hostname in _BLOCKED_EXTERNAL_NAMES or hostname.endswith(".localhost")
            or hostname.endswith(".local") or hostname.endswith(".internal")):
        raise ValueError("target_url must name a public website")
    try:
        address = ipaddress.ip_address(hostname)
    except ValueError:
        try:
            hostname = hostname.encode("idna").decode("ascii")
        except UnicodeError as exc:
            raise ValueError("target_url hostname is invalid") from exc
    else:
        if not address.is_global:
            raise ValueError("target_url IP address must be globally routable")
        hostname = f"[{hostname}]" if address.version == 6 else hostname

    default_port = 80 if parsed.scheme.lower() == "http" else 443
    netloc = hostname if port in (None, default_port) else f"{hostname}:{port}"
    normalized = urlunsplit(SplitResult(
        parsed.scheme.lower(), netloc, parsed.path or "/", parsed.query,
        parsed.fragment))
    return normalized, "external"


def external_profile_id(target_url: str) -> str:
    """Stable per-host id shared by Docker login capture and exploration.

    This duplicates only the neutral host-to-directory naming rule; it does
    not import or expose the managed explorer implementation.
    """
    normalized, mode = normalize_target_url(target_url)
    if mode != "external":
        raise ValueError("Docker login profiles apply only to public targets")
    host = urlsplit(normalized).hostname or ""
    slug = re.sub(r"[^a-z0-9]+", "_", host.lower()).strip("_")
    return f"live_{slug}"


def docker_profile_state_dir(target_url: str) -> Path:
    return LIVE_STATE_ROOT / external_profile_id(target_url)


def docker_profile_golden_dir(target_url: str) -> Path:
    return docker_profile_state_dir(target_url) / "docker_profile_golden"


def _profile_seed(env: Environment) -> Path | None:
    if not env.is_external:
        return None
    candidate = docker_profile_golden_dir(env.target_url)
    # Chromium always writes Local State. Requiring it avoids accidentally
    # mounting a merely-created empty directory as a claimed login profile.
    if not (candidate / "Local State").is_file():
        return None
    resolved = candidate.resolve()
    if LIVE_STATE_ROOT.resolve() not in resolved.parents:
        raise ValueError("login profile escapes live target state root")
    return resolved


def _new_environment(app_id: str, target_url: str | None = None) -> Environment:
    if not APP_RE.fullmatch(app_id):
        raise ValueError("invalid app_id")
    normalized_url, target_mode = normalize_target_url(target_url)
    effective_app_id = app_id if target_mode == "bundled" else EXTERNAL_APP_ID
    if target_mode == "bundled":
        app_entry = PROJECT_ROOT / "sample_apps" / app_id / "app.py"
        if not app_entry.is_file():
            raise ValueError(f"unknown bundled app id: {app_id}")
    stamp = time.strftime("%Y%m%d_%H%M%S")
    session_id = f"self_{stamp}_{secrets.token_hex(4)}"
    run_dir = RUNS_ROOT / session_id
    workspace = run_dir / "workspace"
    workspace.mkdir(parents=True, exist_ok=False)
    return Environment(session_id, f"bbb-{session_id.replace('_', '-')}",
                       effective_app_id, run_dir, workspace,
                       normalized_url, target_mode)


def _load_environment(session_id: str) -> Environment:
    if not SESSION_RE.fullmatch(session_id):
        raise ValueError("invalid self-built session id")
    run_dir = None
    for root in _run_roots():
        candidate = (root / session_id)
        if (candidate / "session.json").is_file():
            run_dir = candidate.resolve()
            break
    if run_dir is None:
        raise ValueError(f"unknown self-built session: {session_id}")
    if run_dir.parent not in {root.resolve() for root in _run_roots()}:
        raise ValueError("session escapes runs directory")
    metadata = json.loads((run_dir / "session.json").read_text(encoding="utf-8"))
    if metadata.get("mode") != "self-built-tools":
        raise ValueError("not a self-built-tool session")
    target_url = metadata.get("target_url", DEMO_TARGET_URL)
    target_mode = metadata.get("target_mode", "bundled")
    return Environment(session_id, metadata["compose_project"],
                       metadata["app_id"], run_dir, run_dir / "workspace",
                       target_url, target_mode)


def _compose_env(env: Environment) -> dict[str, str]:
    values = dict(os.environ)
    docker_dir = str(Path(_docker_command()).parent)
    current_path = values.get("PATH", "")
    path_entries = current_path.split(os.pathsep) if current_path else []
    if docker_dir.lower() not in {entry.lower() for entry in path_entries}:
        values["PATH"] = docker_dir + (os.pathsep + current_path if current_path else "")
    for key, value in _windows_proxy_environment().items():
        values.setdefault(key, value)
    values.update({
        "BBB_SELF_WORKSPACE": str(env.workspace.resolve()),
        "BBB_APP_ID": env.app_id,
        "BBB_TARGET_URL": env.target_url,
        "BBB_TARGET_MODE": env.target_mode,
    })
    profile_seed = _profile_seed(env)
    if profile_seed is not None:
        values["BBB_SELF_PROFILE_SEED"] = str(profile_seed)
    return values


def _compose(env: Environment, *args: str, check: bool = True,
             timeout: int = 600) -> subprocess.CompletedProcess[str]:
    compose_files = ["-f", str(COMPOSE_FILE)]
    if env.is_external:
        compose_files += ["-f", str(URL_COMPOSE_FILE)]
    if _profile_seed(env) is not None:
        compose_files += ["-f", str(PROFILE_COMPOSE_FILE)]
    return subprocess.run(
        [_docker_command(), "compose", *compose_files,
         "-p", env.project_name, *args],
        cwd=PROJECT_ROOT, env=_compose_env(env), text=True,
        encoding="utf-8", errors="replace",
        stdout=subprocess.PIPE, stderr=subprocess.PIPE,
        timeout=timeout, check=check,
        creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0),
    )


def _log(message: str) -> None:
    """Stage progress for the operator: docker compose output is captured,
    so without these lines a start() looks frozen for tens of seconds."""
    try:
        print(f"[self-explorer] {message}", file=sys.stderr, flush=True)
    except (OSError, UnicodeError):
        # Logging is best-effort.  A closed/undecodable client stderr must not
        # prevent Compose cleanup or turn a successful start into an orphan.
        pass


def _run_compose(env: Environment, *args: str, check: bool = True,
                timeout: int = 600) -> subprocess.CompletedProcess[str]:
    """_compose with stage logging and a failure-diagnosis tail."""
    what = " ".join(args[:2])
    _log(f"docker compose {what} … (this can take a while on first build)")
    started = time.monotonic()
    try:
        result = _compose(env, *args, check=False, timeout=timeout)
    except subprocess.TimeoutExpired:
        _log(f"docker compose {what} TIMED OUT after {timeout}s")
        raise
    _log(f"docker compose {what} finished "
         f"({time.monotonic() - started:.0f}s, exit {result.returncode})")
    if result.returncode:
        tail = (result.stderr.strip() or result.stdout.strip())
        if tail:
            tail = "\n".join(tail.splitlines()[-8:])
            _log(f"stderr tail:\n{tail}")
        if check:
            raise subprocess.CalledProcessError(result.returncode,
                                                result.args, result.stdout,
                                                result.stderr)
    return result


def _write_metadata(env: Environment, status: str, **extra: object) -> None:
    current: dict[str, object] = {}
    if env.metadata_path.exists():
        current = json.loads(env.metadata_path.read_text(encoding="utf-8"))
    current.update({
        "session_id": env.session_id,
        "mode": "self-built-tools",
        "app_id": env.app_id,
        "target_url": env.target_url,
        "target_mode": env.target_mode,
        "compose_project": env.project_name,
        "viewport": {"width": 1440, "height": 900, "dpr": 1},
        "status": status,
        **extra,
    })
    env.metadata_path.write_text(json.dumps(current, indent=2), encoding="utf-8")


def start(app_id: str = "ecommerce_demo", target_url: str | None = None) -> Environment:
    env = _new_environment(app_id, target_url)
    _log(f"session {env.session_id}: workspace at {env.workspace}")
    _write_metadata(env, "starting", created_at=time.strftime("%Y-%m-%dT%H:%M:%S%z"))
    try:
        # No --build: compose uses the existing blackboxbench/* images when
        # present (offline-friendly; --build would probe the registry for
        # base-image metadata and fail on machines without Docker Hub
        # access). Rebuild manually with `docker compose build` after image
        # sources change.
        _run_compose(env, "up", "-d")
    except Exception:
        try:
            _run_compose(env, "down", "--volumes", "--remove-orphans",
                         check=False, timeout=180)
        except Exception:
            pass
        # This environment never became usable, so it is not an experiment
        # artifact. Remove our freshly-created exact run directory instead of
        # accumulating failed/test sessions under runs/.
        shutil.rmtree(env.run_dir, ignore_errors=True)
        raise
    _write_metadata(env, "running", runs_root=str(env.run_dir.parent),
                    docker_login_profile=bool(_profile_seed(env)))
    _log(f"environment running (containers starting in background)")
    return env


def stop(session_id: str) -> Environment:
    env = _load_environment(session_id)
    result = _run_compose(env, "down", "--volumes", "--remove-orphans",
                          check=False, timeout=180)
    status = "finished" if result.returncode == 0 else "stop_failed"
    _write_metadata(env, status, finished_at=time.strftime("%Y-%m-%dT%H:%M:%S%z"))
    if result.returncode:
        raise RuntimeError(result.stderr.strip() or "docker compose down failed")
    return env


def status(session_id: str) -> tuple[Environment, str]:
    env = _load_environment(session_id)
    result = _compose(env, "ps", "--format", "json", "--no-trunc",
                      check=False, timeout=30)
    return env, result.stdout.strip()


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)
    p_start = sub.add_parser("start")
    p_start.add_argument("--app", default="ecommerce_demo")
    p_start.add_argument("--url", default=None,
                         help="explicit http(s) target URL; defaults to the bundled demo")
    p_stop = sub.add_parser("stop")
    p_stop.add_argument("session_id")
    p_status = sub.add_parser("status")
    p_status.add_argument("session_id")
    sub.add_parser("recover")
    args = parser.parse_args()

    if args.command == "start":
        env = start(args.app, args.url)
        print(json.dumps({"session_id": env.session_id,
                          "workspace": str(env.workspace),
                          "target_url": env.target_url,
                          "target_mode": env.target_mode}, ensure_ascii=False))
    elif args.command == "stop":
        env = stop(args.session_id)
        print(json.dumps({"session_id": env.session_id, "status": "finished"}))
    elif args.command == "recover":
        restored = hardening.SealManager(lambda: RUNS_ROOT).recover()
        print(json.dumps({"restored": [str(p) for p in restored]},
                         ensure_ascii=False))
    else:
        env, detail = status(args.session_id)
        print(json.dumps({"session_id": env.session_id,
                          "compose": detail}, ensure_ascii=False))


if __name__ == "__main__":
    main()
