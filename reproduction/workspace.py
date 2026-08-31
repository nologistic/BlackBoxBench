"""Isolated handoff from finalized exploration to website reproduction.

The reproduction sandbox is a downstream domain shared by both experimental
conditions.  Managed exploration supplies its finalized topology; self-built
exploration deliberately has no required topology or report.  Both receive
public synthetic materials and a filtered view of any safe exploration
artifacts.  The sandbox never imports either exploration implementation and
never mounts the repository or Reference App into the Agent container.
"""
from __future__ import annotations

import base64
import json
import os
import re
import secrets
import shutil
import stat
import subprocess
import tempfile
from dataclasses import dataclass
from pathlib import Path, PurePosixPath
from typing import Mapping

from reproduction.materials.build import ensure_materials
from reproduction.filelock import lock_for

PROJECT_ROOT = Path(__file__).resolve().parent.parent
MATERIALS_ROOT = PROJECT_ROOT / "reproduction" / "materials"
OUTPUT_ROOT = PROJECT_ROOT / "website_output"
IMAGE_CONTEXT = PROJECT_ROOT / "reproduction" / "agent_image"
IMAGE_NAME = "blackboxbench/reproduction-workbench:latest"
SAFE_ID = re.compile(r"^[A-Za-z0-9_.-]{1,96}$")
MAX_READ = 2 * 1024 * 1024
MAX_OUTPUT = 96 * 1024
MAX_ARTIFACT_FILES = 10_000
DOCKER_HEALTH_TIMEOUT = 12


class DockerUnavailableError(RuntimeError):
    """Safe, user-facing failure raised before a reproduction handoff starts."""


def _docker_command() -> str:
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
        candidate = (Path(local) / "Programs" / "DockerDesktop" /
                     "resources" / "bin" / "docker.exe")
        if local and candidate.is_file():
            return str(candidate)
    raise FileNotFoundError("Docker CLI is not installed")


def _docker_env() -> dict[str, str]:
    values = dict(os.environ)
    docker_dir = str(Path(_docker_command()).parent)
    current = values.get("PATH", "")
    entries = current.split(os.pathsep) if current else []
    if docker_dir.lower() not in {entry.lower() for entry in entries}:
        values["PATH"] = docker_dir + (os.pathsep + current if current else "")
    for key, value in _windows_proxy_environment().items():
        values.setdefault(key, value)
    return values


def _parse_windows_proxy(value: str) -> dict[str, str]:
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


def _docker(*args: str, check: bool = True,
            timeout: int = 600) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        [_docker_command(), *args], cwd=PROJECT_ROOT, env=_docker_env(),
        text=True, encoding="utf-8", errors="replace",
        stdout=subprocess.PIPE, stderr=subprocess.PIPE,
        timeout=timeout, check=check,
        creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0),
    )


def ensure_docker_available(timeout: int = DOCKER_HEALTH_TIMEOUT) -> str:
    """Fail quickly when Docker Desktop is open but its Linux engine is dead.

    Docker Desktop can leave its Windows UI/backend processes alive while the
    WSL engine no longer answers. A normal build then spends roughly a minute
    retrying, and cleanup incurs another daemon timeout. Probe the server before
    creating handoff directories so the failure is fast and leaves no partial
    output behind.
    """
    try:
        result = _docker("version", "--format", "{{.Server.Version}}",
                         timeout=timeout)
    except subprocess.TimeoutExpired as exc:
        raise DockerUnavailableError(
            f"Docker engine did not respond within {timeout}s; restart Docker "
            "Desktop and wait until the Linux engine is running") from exc
    except subprocess.CalledProcessError as exc:
        detail = (exc.stderr or exc.stdout or "").strip().splitlines()
        suffix = f": {detail[-1][:300]}" if detail else ""
        raise DockerUnavailableError(
            "Docker engine is unavailable" + suffix) from exc
    version = result.stdout.strip()
    if not version:
        raise DockerUnavailableError(
            "Docker CLI returned no server version; restart Docker Desktop "
            "and wait until the Linux engine is running")
    return version


def _relative(value: object, *, allow_root: bool = True) -> PurePosixPath:
    raw = str(value or ".").replace("\\", "/")
    path = PurePosixPath(raw)
    if path.is_absolute() or ".." in path.parts:
        raise ValueError("path must stay inside the reproduction output")
    cleaned = PurePosixPath(*[p for p in path.parts if p not in ("", ".")])
    if not cleaned.parts and not allow_root:
        raise ValueError("a file path is required")
    return cleaned


def _artifact_bundle(handoff_id: str, source_mode: str,
                     files: Mapping[str, Path] | None) -> Path:
    """Create a curated, immutable-input view without mounting a whole run.

    Hard links avoid duplicating thousands of managed PNG frames.  The bundle
    itself is mounted read-only, so the downstream container cannot mutate the
    original evidence through those links.  Copying is the portable fallback.
    """
    root = Path(tempfile.mkdtemp(prefix=f"bbb-repro-{handoff_id}-"))
    items = []
    try:
        entries = sorted((files or {}).items())
        if len(entries) > MAX_ARTIFACT_FILES:
            raise ValueError("too many exploration artifacts")
        for target_name, source_value in entries:
            relative = _relative(target_name, allow_root=False)
            if relative == PurePosixPath("manifest.json"):
                raise ValueError("manifest.json is reserved")
            source = Path(source_value)
            if not source.exists() or not source.is_file():
                raise ValueError("an exploration artifact is not a regular file")
            if stat.S_ISLNK(source.lstat().st_mode):
                raise ValueError("exploration artifacts must not contain symlinks")
            destination = root.joinpath(*relative.parts)
            destination.parent.mkdir(parents=True, exist_ok=True)
            if destination.exists():
                raise ValueError("duplicate exploration artifact path")
            try:
                os.link(source, destination)
            except OSError:
                shutil.copy2(source, destination)
            items.append({"path": relative.as_posix(),
                          "bytes": destination.stat().st_size})
        (root / "manifest.json").write_text(json.dumps({
            "source_mode": source_mode,
            "handoff_id": handoff_id,
            "read_only": True,
            "items": items,
        }, ensure_ascii=False, indent=2), encoding="utf-8")
        return root
    except Exception:
        _remove_artifact_bundle(root, handoff_id)
        raise


def _remove_artifact_bundle(path: Path, handoff_id: str) -> None:
    """Remove only a bundle created by this module in the system temp root."""
    resolved = path.resolve()
    temp_root = Path(tempfile.gettempdir()).resolve()
    prefix = f"bbb-repro-{handoff_id}-"
    if resolved.parent != temp_root or not resolved.name.startswith(prefix):
        raise ValueError("refusing to remove an unrecognized artifact bundle")
    shutil.rmtree(resolved, ignore_errors=True)


@dataclass
class ReproductionWorkspace:
    handoff_id: str
    source_mode: str
    source_id: str
    topology_path: Path | None
    output_dir: Path
    container_name: str
    artifact_dir: Path | None = None
    running: bool = True

    @classmethod
    def start(cls, *, source_mode: str, source_id: str,
              topology_path: Path | None,
              exploration_files: Mapping[str, Path] | None = None,
              ) -> "ReproductionWorkspace":
        if source_mode not in ("managed-tools", "self-built-tools"):
            raise ValueError("invalid source mode")
        if not SAFE_ID.fullmatch(source_id):
            raise ValueError("invalid source id")
        topology: Path | None = None
        if topology_path is not None:
            topology = topology_path.resolve()
            if not topology.is_file():
                raise FileNotFoundError("finalized topology is missing")
            # Validate before exposing the file to the downstream Agent.
            parsed = json.loads(topology.read_text(encoding="utf-8"))
            if not isinstance(parsed, dict):
                raise ValueError("finalized topology must be an object")
        elif source_mode == "managed-tools":
            raise FileNotFoundError("managed exploration requires a finalized topology")
        # Check before material generation and, crucially, before creating the
        # output/artifact directories. This also avoids a second slow Docker
        # call from cleanup when the daemon is unreachable.
        ensure_docker_available()
        ensure_materials()

        handoff_id = f"{source_id}-{secrets.token_hex(3)}"
        output = (OUTPUT_ROOT / handoff_id).resolve()
        if output.parent != OUTPUT_ROOT.resolve():
            raise ValueError("output escapes website_output")
        container = "bbb-repro-" + re.sub(r"[^a-z0-9-]", "-", handoff_id.lower())
        artifacts: Path | None = None
        try:
            output.mkdir(parents=True, exist_ok=False)
            artifacts = _artifact_bundle(handoff_id, source_mode,
                                         exploration_files)
            workspace = cls(handoff_id, source_mode, source_id, topology,
                            output, container, artifacts)
            # Concurrent handoffs (one per agent) must not race the shared
            # image tag; the first build populates the cache for the rest.
            with lock_for(f"image:{IMAGE_NAME}"):
                _docker("build", "-t", IMAGE_NAME, str(IMAGE_CONTEXT))
            run_args = [
                "run", "-d", "--name", container,
                "--network", "none", "--read-only",
                "--cap-drop", "ALL", "--security-opt", "no-new-privileges:true",
                "--pids-limit", "128", "--memory", "1g", "--cpus", "2",
                "--tmpfs", "/tmp:rw,noexec,nosuid,size=128m,uid=1000,gid=1000,mode=0700",
                "--mount", f"type=bind,source={output},target=/workspace",
                "--mount", f"type=bind,source={MATERIALS_ROOT.resolve()},target=/materials,readonly",
            ]
            if topology is not None:
                run_args.extend([
                    "--mount",
                    f"type=bind,source={topology},target=/input/functional_topology.json,readonly",
                ])
            run_args.extend([
                "--mount", f"type=bind,source={artifacts},target=/exploration,readonly",
                IMAGE_NAME,
            ])
            _docker(*run_args)
        except Exception:
            _docker("rm", "-f", container, check=False, timeout=60)
            # Only this freshly-created, validated child is removed.
            shutil.rmtree(output, ignore_errors=True)
            if artifacts is not None:
                _remove_artifact_bundle(artifacts, handoff_id)
            raise
        return workspace

    def _host_path(self, relative: PurePosixPath, *, for_write: bool = False) -> Path:
        current = self.output_dir
        parts = list(relative.parts)
        check_parts = parts[:-1] if for_write else parts
        for part in check_parts:
            current = current / part
            if current.exists() and stat.S_ISLNK(current.lstat().st_mode):
                raise ValueError("symbolic links are not accepted")
        result = self.output_dir.joinpath(*parts)
        if result.exists() and stat.S_ISLNK(result.lstat().st_mode):
            raise ValueError("symbolic links are not accepted")
        return result

    def list_files(self, value: object = ".") -> dict:
        relative = _relative(value)
        path = self._host_path(relative)
        if not path.is_dir():
            raise ValueError("not a directory")
        entries = []
        for child in sorted(path.iterdir(), key=lambda p: p.name)[:500]:
            mode = child.lstat().st_mode
            kind = ("symlink" if stat.S_ISLNK(mode) else
                    "directory" if child.is_dir() else "file")
            entries.append({"name": child.name, "kind": kind,
                            "size": child.stat().st_size if kind == "file" else None})
        return {"stage": "reproduction", "path": relative.as_posix() or ".",
                "entries": entries}

    def read_file(self, value: object) -> list[dict]:
        relative = _relative(value, allow_root=False)
        path = self._host_path(relative)
        if not path.is_file():
            raise ValueError("not a file")
        size = path.stat().st_size
        if size > MAX_READ:
            raise ValueError(f"file exceeds {MAX_READ} bytes")
        suffix = path.suffix.lower()
        if suffix in (".png", ".jpg", ".jpeg"):
            mime = "image/png" if suffix == ".png" else "image/jpeg"
            return [{"type": "image",
                     "data": base64.b64encode(path.read_bytes()).decode("ascii"),
                     "mimeType": mime},
                    {"type": "text", "text": json.dumps(
                        {"path": relative.as_posix(), "bytes": size})}]
        try:
            content = path.read_text(encoding="utf-8")
        except UnicodeDecodeError as exc:
            raise ValueError("binary output must first be converted to PNG or JPEG") from exc
        return [{"type": "text", "text": json.dumps(
            {"path": relative.as_posix(), "content": content}, ensure_ascii=False)}]

    def write_file(self, value: object, content: object) -> dict:
        relative = _relative(value, allow_root=False)
        if not isinstance(content, str):
            raise ValueError("content must be text")
        if len(content.encode("utf-8")) > MAX_READ:
            raise ValueError(f"content exceeds {MAX_READ} bytes")
        path = self._host_path(relative, for_write=True)
        path.parent.mkdir(parents=True, exist_ok=True)
        path = self._host_path(relative, for_write=True)
        path.write_text(content, encoding="utf-8")
        return {"stage": "reproduction", "written": relative.as_posix(),
                "bytes": len(content.encode("utf-8"))}

    def run_program(self, argv: object, cwd: object = ".",
                    timeout_seconds: object = 30) -> dict:
        if not isinstance(argv, list) or not argv or not all(
                isinstance(item, str) and item for item in argv):
            raise ValueError("argv must be a non-empty string array")
        timeout = int(timeout_seconds)
        if not 1 <= timeout <= 120:
            raise ValueError("timeout_seconds must be between 1 and 120")
        relative = _relative(cwd)
        workdir = "/workspace" + (
            "/" + relative.as_posix() if relative.parts else "")
        result = _docker(
            "exec", "-i", "--workdir", workdir, self.container_name, *argv,
            check=False, timeout=timeout,
        )
        return {"stage": "reproduction", "exit_code": result.returncode,
                "stdout": result.stdout[-MAX_OUTPUT:],
                "stderr": result.stderr[-MAX_OUTPUT:],
                "truncated": (len(result.stdout) > MAX_OUTPUT or
                              len(result.stderr) > MAX_OUTPUT)}

    def finish(self, *, review_summary: Mapping | None = None) -> dict:
        if not self.running:
            return {"finished": True, "handoff_id": self.handoff_id}
        entries = list(self.output_dir.rglob("*"))
        if any(path.is_symlink() for path in entries):
            raise ValueError("reproduction output must not contain symlinks")
        files = [path for path in entries if path.is_file()]
        if not files:
            raise ValueError("reproduction output is empty")
        _docker("rm", "-f", self.container_name, check=False, timeout=60)
        self.running = False
        if review_summary is not None:
            review_dir = self.output_dir / ".blackboxbench"
            review_dir.mkdir(parents=True, exist_ok=True)
            (review_dir / "review_summary.json").write_text(
                json.dumps(dict(review_summary), ensure_ascii=False, indent=2),
                encoding="utf-8")
            files = [path for path in self.output_dir.rglob("*")
                     if path.is_file()]
        if self.artifact_dir is not None:
            _remove_artifact_bundle(self.artifact_dir, self.handoff_id)
            self.artifact_dir = None
        return {"finished": True, "handoff_id": self.handoff_id,
                "source_mode": self.source_mode,
                "output": f"website_output/{self.handoff_id}",
                "files": len(files)}

    def close(self) -> None:
        if self.running:
            _docker("rm", "-f", self.container_name, check=False, timeout=60)
            self.running = False
        if self.artifact_dir is not None:
            _remove_artifact_bundle(self.artifact_dir, self.handoff_id)
            self.artifact_dir = None

    def started_payload(self) -> dict:
        sandbox = {
            "materials": "/materials",
            "exploration_artifacts": "/exploration",
            "exploration_manifest": "/exploration/manifest.json",
            "workspace": "/workspace",
            "inputs_read_only": True,
            "network": "none",
        }
        if self.topology_path is not None:
            sandbox["topology"] = "/input/functional_topology.json"
        return {"stage": "reproduction", "ready": True,
                "source_mode": self.source_mode,
                "handoff_id": self.handoff_id,
                "output": f"website_output/{self.handoff_id}",
                "sandbox": sandbox}
