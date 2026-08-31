"""Isolated Android project workspace and offline APK builder."""
from __future__ import annotations

import base64
import hashlib
import json
import os
import re
import secrets
import shutil
import stat
import tempfile
import zipfile
from io import BytesIO
from dataclasses import dataclass
from pathlib import Path, PurePosixPath
from typing import Mapping

from PIL import Image, ImageDraw, ImageOps, ImageStat

from benchmark import config
from reproduction.filelock import lock_for
from reproduction.materials.build import ensure_materials
from reproduction.workspace import (
    DockerUnavailableError, _docker, _relative, ensure_docker_available,
)
from .materials.build import ensure_app_materials

PROJECT_ROOT = config.PROJECT_ROOT
OUTPUT_ROOT = config.APP_OUTPUT_DIR
SCAFFOLD_ROOT = Path(__file__).resolve().parent / "scaffold"
IMAGE_CONTEXT = Path(__file__).resolve().parent
IMAGE_NAME = "blackboxbench/android-reproduction-workbench:1"
GRADLE_BASE_IMAGE = (
    "gradle@sha256:"
    "c2900027f3f0681c2cbfb09d527813851ad67aeafbb409997297efa2df20e748"
)
SAFE_ID = re.compile(r"^[A-Za-z0-9_.-]{1,96}$")
MAX_READ = 2 * 1024 * 1024
MAX_OUTPUT = 96 * 1024
MAX_ARTIFACT_FILES = 10_000
DANGEROUS_PERMISSIONS = {
    "android.permission.READ_CALENDAR", "android.permission.WRITE_CALENDAR",
    "android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS",
    "android.permission.GET_ACCOUNTS",
    "android.permission.READ_CALL_LOG", "android.permission.WRITE_CALL_LOG",
    "android.permission.READ_PHONE_STATE", "android.permission.READ_PHONE_NUMBERS",
    "android.permission.CALL_PHONE", "android.permission.ANSWER_PHONE_CALLS",
    "android.permission.ADD_VOICEMAIL", "android.permission.USE_SIP",
    "android.permission.PROCESS_OUTGOING_CALLS", "android.permission.READ_SMS",
    "android.permission.SEND_SMS", "android.permission.RECEIVE_SMS",
    "android.permission.RECEIVE_MMS", "android.permission.RECEIVE_WAP_PUSH",
    "android.permission.RECORD_AUDIO", "android.permission.CAMERA",
    "android.permission.ACCESS_FINE_LOCATION",
    "android.permission.ACCESS_COARSE_LOCATION",
    "android.permission.ACCESS_BACKGROUND_LOCATION",
    "android.permission.BODY_SENSORS", "android.permission.BODY_SENSORS_BACKGROUND",
    "android.permission.ACTIVITY_RECOGNITION",
    "android.permission.BLUETOOTH_SCAN", "android.permission.BLUETOOTH_CONNECT",
    "android.permission.BLUETOOTH_ADVERTISE",
    "android.permission.NEARBY_WIFI_DEVICES",
    "android.permission.READ_MEDIA_IMAGES", "android.permission.READ_MEDIA_VIDEO",
    "android.permission.READ_MEDIA_AUDIO", "android.permission.READ_EXTERNAL_STORAGE",
    "android.permission.WRITE_EXTERNAL_STORAGE",
    "android.permission.MANAGE_EXTERNAL_STORAGE",
    "android.permission.REQUEST_INSTALL_PACKAGES",
    "android.permission.SYSTEM_ALERT_WINDOW", "android.permission.WRITE_SETTINGS",
    "android.permission.PACKAGE_USAGE_STATS",
}
TEXT_SUFFIXES = {
    ".kt", ".kts", ".java", ".xml", ".json", ".txt", ".md", ".properties",
    ".toml", ".yaml", ".yml", ".gradle",
}
HANDOFF_SUFFIXES = {".png", ".jpg", ".jpeg", ".json", ".md", ".txt"}
IMAGE_SUFFIXES = {".png", ".jpg", ".jpeg", ".webp", ".bmp"}
VISUAL_HASH_BITS = 256
VISUAL_HASH_MAX_DISTANCE = 3


def _region_box(region: Mapping, width: int, height: int) -> tuple[int, int, int, int]:
    try:
        x = max(0, int(region["x"])); y = max(0, int(region["y"]))
        w = max(1, int(region["width"])); h = max(1, int(region["height"]))
    except (KeyError, TypeError, ValueError) as exc:
        raise ValueError("invalid protected screenshot region") from exc
    return x, y, min(width, x + w), min(height, y + h)


def _visual_signature(image: Image.Image) -> dict:
    source = ImageOps.exif_transpose(image).convert("RGB")
    width, height = source.size
    luminance = source.resize((17, 16), Image.Resampling.LANCZOS).convert("L")
    pixels = list(luminance.getdata())
    bits = 0
    for y in range(16):
        row = y * 17
        for x in range(16):
            bits = (bits << 1) | int(pixels[row + x] > pixels[row + x + 1])
    mean = tuple(round(value, 2) for value in
                 ImageStat.Stat(source.resize((32, 32))).mean[:3])
    return {"width": width, "height": height,
            "dhash": f"{bits:0{VISUAL_HASH_BITS // 4}x}", "mean": mean}


def _signature_matches(left: Mapping, right: Mapping) -> bool:
    try:
        left_ratio = float(left["width"]) / max(1.0, float(left["height"]))
        right_ratio = float(right["width"]) / max(1.0, float(right["height"]))
        if abs(left_ratio - right_ratio) > 0.015:
            return False
        distance = (int(str(left["dhash"]), 16) ^
                    int(str(right["dhash"]), 16)).bit_count()
        mean_distance = max(abs(float(a) - float(b)) for a, b in
                            zip(left["mean"], right["mean"]))
        return distance <= VISUAL_HASH_MAX_DISTANCE and mean_distance <= 6.0
    except (KeyError, TypeError, ValueError, ZeroDivisionError):
        return False


def _source_visual_signatures(files: Mapping[str, Path] | None,
                              protected_regions: list[dict]) -> tuple[list[dict], list[dict]]:
    """Build trusted-only fingerprints before the handoff is redacted."""
    frames: list[dict] = []
    patches: list[dict] = []
    for source_value in (files or {}).values():
        source = Path(source_value)
        if not source.is_file() or source.suffix.lower() not in IMAGE_SUFFIXES:
            continue
        with Image.open(source) as opened:
            image = ImageOps.exif_transpose(opened).convert("RGB")
            frames.append(_visual_signature(image))
            for region in protected_regions:
                box = _region_box(region, *image.size)
                if box[0] >= box[2] or box[1] >= box[3]:
                    continue
                patch = image.crop(box)
                # Uniform blocks carry no useful identity and create excessive
                # false positives against ordinary Compose backgrounds.
                if max(ImageStat.Stat(patch).stddev) >= 4.0:
                    patches.append(_visual_signature(patch))
    return frames, patches


def android_build_image_available() -> bool:
    """Return whether the explicitly prepared, offline build image exists."""
    result = _docker("image", "inspect", IMAGE_NAME, check=False, timeout=30)
    return result.returncode == 0


def require_android_build_image() -> None:
    if not android_build_image_available():
        raise DockerUnavailableError(
            "Android build image is not prepared. Review the Android SDK "
            "licenses, then run: vendor/python/python.exe "
            "scripts/build_android_reproduction_image.py --accept-licenses")


def build_android_build_image(*, accept_licenses: bool) -> None:
    """Build the pinned image only after an explicit operator confirmation."""
    if not accept_licenses:
        raise ValueError("explicit Android SDK license acceptance is required")
    ensure_docker_available()
    with lock_for(f"image:{IMAGE_NAME}"):
        # Docker Desktop's normal image-pull path reliably honors its container
        # proxy, while some BuildKit versions resolve an absent FROM image via
        # a separate direct authentication path.  Seed the exact immutable
        # base first, but stay offline-friendly when it is already cached.
        cached = _docker("image", "inspect", GRADLE_BASE_IMAGE,
                         check=False, timeout=30)
        if cached.returncode:
            pulled = _docker("pull", GRADLE_BASE_IMAGE,
                             check=False, timeout=900)
            if pulled.returncode:
                detail = (pulled.stderr or pulled.stdout or "").strip()
                if len(detail) > 4_000:
                    detail = "...\n" + detail[-4_000:]
                raise RuntimeError(
                    "Pinned Gradle base image pull failed"
                    + (f":\n{detail}" if detail else
                       " (Docker returned no diagnostics)")
                )
        result = _docker(
            "build", "--build-arg", "ANDROID_SDK_LICENSES_ACCEPTED=true",
            "-f", str(IMAGE_CONTEXT / "agent_image" / "Dockerfile"),
            "-t", IMAGE_NAME, str(IMAGE_CONTEXT), check=False, timeout=1800,
        )
        if result.returncode:
            # Docker/BuildKit diagnostics are normally written to stderr.  Keep
            # the tail bounded so CLI/MCP clients receive the useful failure
            # without being overwhelmed by an entire image-build transcript.
            detail = (result.stderr or result.stdout or "").strip()
            if len(detail) > 8_000:
                detail = "...\n" + detail[-8_000:]
            raise RuntimeError(
                "Android reproduction image build failed"
                + (f":\n{detail}" if detail else " (Docker returned no diagnostics)")
            )


def _safe_artifact_path(value: str) -> PurePosixPath:
    return _relative(value, allow_root=False)


def _tree_fingerprint(root: Path) -> str:
    digest = hashlib.sha256()
    for path in sorted(root.rglob("*")):
        if not path.is_file() or path.is_symlink():
            continue
        digest.update(path.relative_to(root).as_posix().encode("utf-8"))
        digest.update(b"\0"); digest.update(path.read_bytes())
    return digest.hexdigest()


def _create_handoff_bundle(handoff_id: str, source_mode: str,
                           files: Mapping[str, Path] | None,
                           protected_strings: list[str],
                           protected_regions: list[dict]) -> Path:
    """Always-copy handoff with text redaction and irreversible region masks."""
    root = Path(tempfile.mkdtemp(prefix=f"bbb-app-repro-{handoff_id}-"))
    items = []
    try:
        entries = sorted((files or {}).items())
        if len(entries) > MAX_ARTIFACT_FILES:
            raise ValueError("too many exploration artifacts")
        for target_name, source_value in entries:
            relative = _safe_artifact_path(target_name)
            if relative == PurePosixPath("manifest.json"):
                raise ValueError("manifest.json is reserved")
            source = Path(source_value)
            if not source.is_file() or source.is_symlink():
                raise ValueError("exploration artifact must be a regular file")
            destination = root.joinpath(*relative.parts)
            destination.parent.mkdir(parents=True, exist_ok=True)
            suffix = source.suffix.lower()
            if suffix not in HANDOFF_SUFFIXES:
                raise ValueError(
                    "exploration handoff accepts only visible frames and text reports")
            if suffix in (".png", ".jpg", ".jpeg") and protected_regions:
                with Image.open(source) as image:
                    safe = ImageOps.exif_transpose(image).convert("RGB")
                    draw = ImageDraw.Draw(safe)
                    for region in protected_regions:
                        box = _region_box(region, *safe.size)
                        if box[0] < box[2] and box[1] < box[3]:
                            draw.rectangle(box, fill=(24, 24, 24))
                    safe.save(destination, "PNG" if suffix == ".png" else "JPEG")
            elif suffix in TEXT_SUFFIXES:
                text = source.read_text(encoding="utf-8")
                for secret in protected_strings:
                    if secret:
                        text = re.sub(re.escape(secret), "[REDACTED]", text,
                                      flags=re.IGNORECASE)
                destination.write_text(text, encoding="utf-8")
            else:
                shutil.copy2(source, destination)
            items.append({"path": relative.as_posix(),
                          "bytes": destination.stat().st_size,
                          "sha256": hashlib.sha256(
                              destination.read_bytes()).hexdigest()})
        (root / "manifest.json").write_text(json.dumps({
            "source_mode": source_mode, "handoff_id": handoff_id,
            "read_only": True, "privacy_filtered": True, "items": items,
        }, ensure_ascii=False, indent=2), encoding="utf-8")
        return root
    except Exception:
        shutil.rmtree(root, ignore_errors=True)
        raise


def _remove_bundle(path: Path, handoff_id: str) -> None:
    resolved = path.resolve()
    temp = Path(tempfile.gettempdir()).resolve()
    if resolved.parent != temp or not resolved.name.startswith(
            f"bbb-app-repro-{handoff_id}-"):
        raise ValueError("refusing to remove unknown app handoff bundle")
    shutil.rmtree(resolved, ignore_errors=True)


@dataclass
class AppReproductionWorkspace:
    handoff_id: str
    source_mode: str
    source_id: str
    topology_path: Path
    output_dir: Path
    project_dir: Path
    review_dir: Path
    artifacts_dir: Path
    container_name: str
    artifact_dir: Path | None
    protected_strings: list[str]
    protected_regions: list[dict] | None = None
    source_visual_signatures: list[dict] | None = None
    protected_patch_signatures: list[dict] | None = None
    running: bool = True
    accepted_project_hash: str = ""
    accepted_apk_hash: str = ""
    common_material_hash: str = ""
    mobile_material_hash: str = ""

    @classmethod
    def start(cls, *, source_mode: str, source_id: str,
              topology_path: Path,
              exploration_files: Mapping[str, Path] | None = None,
              protected_strings: list[str] | None = None,
              protected_regions: list[dict] | None = None,
              ) -> "AppReproductionWorkspace":
        if source_mode not in ("android-baseline", "android-our-method"):
            raise ValueError("invalid Android source mode")
        if not SAFE_ID.fullmatch(source_id):
            raise ValueError("invalid source id")
        topology = Path(topology_path).resolve()
        if not topology.is_file():
            raise FileNotFoundError("finalized Android topology is missing")
        parsed = json.loads(topology.read_text(encoding="utf-8"))
        if not isinstance(parsed, dict):
            raise ValueError("finalized topology must be an object")
        ensure_docker_available()
        require_android_build_image()
        common_materials = ensure_materials().resolve()
        mobile_materials = ensure_app_materials().resolve()

        handoff_id = f"{source_id}-{secrets.token_hex(3)}"
        output = (OUTPUT_ROOT / handoff_id).resolve()
        if output.parent != OUTPUT_ROOT.resolve():
            raise ValueError("output escapes app_output")
        project = output / "project"
        review = output / "review"
        artifacts = output / "artifacts"
        container = "bbb-app-repro-" + re.sub(
            r"[^a-z0-9-]", "-", handoff_id.lower())
        bundle: Path | None = None
        try:
            output.mkdir(parents=True, exist_ok=False)
            shutil.copytree(SCAFFOLD_ROOT, project)
            review.mkdir(); artifacts.mkdir()
            regions = list(protected_regions or [])
            frame_signatures, patch_signatures = _source_visual_signatures(
                exploration_files, regions)
            bundle = _create_handoff_bundle(
                handoff_id, source_mode, exploration_files,
                list(protected_strings or []), regions)
            for path in bundle.rglob("*"):
                if path.is_file() and path.suffix.lower() in IMAGE_SUFFIXES:
                    with Image.open(path) as image:
                        frame_signatures.append(_visual_signature(image))
            # The JSON topology is mounted from the privacy-filtered bundle,
            # never directly from the run directory.
            safe_topology = bundle / "input" / "functional_topology.json"
            safe_topology.parent.mkdir(parents=True, exist_ok=True)
            topology_text = topology.read_text(encoding="utf-8")
            for secret in protected_strings or []:
                if secret:
                    topology_text = re.sub(re.escape(secret), "[REDACTED]",
                                           topology_text,
                                           flags=re.IGNORECASE)
            safe_topology.write_text(topology_text, encoding="utf-8")
            workspace = cls(
                handoff_id, source_mode, source_id, safe_topology, output, project,
                review, artifacts, container, bundle,
                list(protected_strings or []), regions, frame_signatures,
                patch_signatures)
            workspace.common_material_hash = _tree_fingerprint(common_materials)
            workspace.mobile_material_hash = _tree_fingerprint(mobile_materials)
            _docker(
                "run", "-d", "--name", container, "--network", "none",
                "--read-only", "--cap-drop", "ALL", "--security-opt",
                "no-new-privileges:true", "--pids-limit", "256", "--memory",
                "4g", "--cpus", "4", "--tmpfs",
                "/tmp:rw,noexec,nosuid,size=256m,uid=1000,gid=1000,mode=0700",
                "--mount", "type=volume,target=/opt/gradle-cache",
                "--env", "ANDROID_USER_HOME=/tmp/android-home",
                "--mount", f"type=bind,source={project},target=/workspace",
                "--mount", f"type=bind,source={common_materials},target=/materials/common,readonly",
                "--mount", f"type=bind,source={mobile_materials},target=/materials/mobile,readonly",
                "--mount", f"type=bind,source={safe_topology},target=/input/functional_topology.json,readonly",
                "--mount", f"type=bind,source={bundle},target=/exploration,readonly",
                IMAGE_NAME, timeout=120)
        except Exception:
            _docker("rm", "-f", "-v", container, check=False, timeout=60)
            shutil.rmtree(output, ignore_errors=True)
            if bundle is not None:
                _remove_bundle(bundle, handoff_id)
            raise
        return workspace

    def _path(self, relative: PurePosixPath, *, for_write: bool = False) -> Path:
        current = self.project_dir
        parts = list(relative.parts)
        for part in (parts[:-1] if for_write else parts):
            current = current / part
            if current.exists() and current.is_symlink():
                raise ValueError("symbolic links are not accepted")
        result = self.project_dir.joinpath(*parts)
        if result.exists() and result.is_symlink():
            raise ValueError("symbolic links are not accepted")
        return result

    def list_files(self, value: object = ".") -> dict:
        relative = _relative(value)
        path = self._path(relative)
        if not path.is_dir():
            raise ValueError("not a directory")
        entries = []
        for child in sorted(path.iterdir(), key=lambda p: p.name)[:500]:
            mode = child.lstat().st_mode
            kind = ("symlink" if stat.S_ISLNK(mode) else
                    "directory" if child.is_dir() else "file")
            entries.append({"name": child.name, "kind": kind,
                            "size": child.stat().st_size if kind == "file" else None})
        return {"stage": "app_reproduction", "path": relative.as_posix() or ".",
                "entries": entries}

    def read_file(self, value: object) -> list[dict]:
        relative = _relative(value, allow_root=False)
        path = self._path(relative)
        if not path.is_file():
            raise ValueError("not a file")
        size = path.stat().st_size
        if size > MAX_READ:
            raise ValueError(f"file exceeds {MAX_READ} bytes")
        if path.suffix.lower() in (".png", ".jpg", ".jpeg"):
            mime = "image/png" if path.suffix.lower() == ".png" else "image/jpeg"
            return [{"type": "image", "data": base64.b64encode(
                path.read_bytes()).decode("ascii"), "mimeType": mime},
                {"type": "text", "text": json.dumps(
                    {"path": relative.as_posix(), "bytes": size})}]
        try:
            content = path.read_text(encoding="utf-8")
        except UnicodeDecodeError as exc:
            raise ValueError("binary project files are not directly readable") from exc
        return [{"type": "text", "text": json.dumps(
            {"path": relative.as_posix(), "content": content},
            ensure_ascii=False)}]

    def write_file(self, value: object, content: object) -> dict:
        relative = _relative(value, allow_root=False)
        if not isinstance(content, str):
            raise ValueError("content must be text")
        encoded = content.encode("utf-8")
        if len(encoded) > MAX_READ:
            raise ValueError(f"content exceeds {MAX_READ} bytes")
        path = self._path(relative, for_write=True)
        path.parent.mkdir(parents=True, exist_ok=True)
        self._path(relative, for_write=True).write_text(content, encoding="utf-8")
        return {"stage": "app_reproduction", "written": relative.as_posix(),
                "bytes": len(encoded)}

    def run_program(self, argv: object, cwd: object = ".",
                    timeout_seconds: object = 60) -> dict:
        if not isinstance(argv, list) or not argv or not all(
                isinstance(item, str) and item for item in argv):
            raise ValueError("argv must be a non-empty string array")
        timeout = int(timeout_seconds)
        if not 1 <= timeout <= 300:
            raise ValueError("timeout_seconds must be between 1 and 300")
        relative = _relative(cwd)
        workdir = "/workspace" + (
            "/" + relative.as_posix() if relative.parts else "")
        result = _docker("exec", "-i", "--workdir", workdir,
                         self.container_name, *argv, check=False,
                         timeout=timeout)
        return {"stage": "app_reproduction", "exit_code": result.returncode,
                "stdout": result.stdout[-MAX_OUTPUT:],
                "stderr": result.stderr[-MAX_OUTPUT:],
                "truncated": len(result.stdout) > MAX_OUTPUT or
                             len(result.stderr) > MAX_OUTPUT}

    def project_fingerprint(self) -> str:
        digest = hashlib.sha256()
        for path in sorted(self.project_dir.rglob("*")):
            if not path.is_file() or path.is_symlink():
                continue
            rel = path.relative_to(self.project_dir)
            if any(part in ("build", ".gradle") for part in rel.parts):
                continue
            digest.update(rel.as_posix().encode("utf-8"))
            digest.update(b"\0")
            digest.update(path.read_bytes())
        return digest.hexdigest()

    def build_apk(self) -> dict:
        result = self.run_program(
            ["gradle", "--offline", "--no-daemon", "assembleDebug"],
            timeout_seconds=300)
        if result["exit_code"] != 0:
            raise RuntimeError("offline Android build failed: " +
                               result["stderr"][-1000:])
        source = (self.project_dir / "app" / "build" / "outputs" / "apk" /
                  "debug" / "app-debug.apk")
        if not source.is_file():
            raise FileNotFoundError("Gradle succeeded but app-debug.apk is missing")
        metadata = self._verify_built_apk(source)
        destination = self.artifacts_dir / "app-debug.apk"
        shutil.copy2(source, destination)
        self._apk_archive_gate(destination)
        digest = hashlib.sha256(destination.read_bytes()).hexdigest()
        summary = {"apk": "artifacts/app-debug.apk", "bytes": destination.stat().st_size,
                   "sha256": digest, "project_sha256": self.project_fingerprint(),
                   **metadata}
        (self.artifacts_dir / "build_summary.json").write_text(
            json.dumps(summary, indent=2), encoding="utf-8")
        return summary

    def _verify_built_apk(self, source: Path) -> dict:
        relative = source.relative_to(self.project_dir).as_posix()
        signer = self.run_program([
            "/opt/android-sdk/build-tools/35.0.0/apksigner", "verify",
            "--verbose", "--print-certs", relative], timeout_seconds=60)
        if signer["exit_code"] != 0:
            raise ValueError("generated APK has an invalid signature")
        match = re.search(
            r"certificate SHA-256 digest:\s*([0-9a-f:]+)",
            signer["stdout"], re.IGNORECASE)
        expected = self.run_program(
            ["cat", "/opt/benchmark/cert.sha256"], timeout_seconds=10)
        expected_digest = expected["stdout"].strip().replace(":", "").casefold()
        actual_digest = (match.group(1).replace(":", "").casefold()
                         if match else "")
        if (expected["exit_code"] != 0 or not expected_digest or
                actual_digest != expected_digest):
            raise ValueError("generated APK is not signed by the benchmark key")
        permissions = self.run_program([
            "/opt/android-sdk/build-tools/35.0.0/aapt2", "dump", "permissions",
            relative], timeout_seconds=60)
        if permissions["exit_code"] != 0:
            raise ValueError("generated APK permissions could not be inspected")
        found = sorted(permission for permission in DANGEROUS_PERMISSIONS
                       if permission in permissions["stdout"])
        if found:
            raise ValueError("unapproved dangerous Android permissions: " +
                             ", ".join(found))
        declared = sorted(set(re.findall(
            r"android\.permission\.[A-Z0-9_]+", permissions["stdout"])))
        return {"signature_verified": True,
                "benchmark_certificate_sha256": actual_digest,
                "declared_permissions": declared}

    def _apk_archive_gate(self, apk: Path) -> None:
        """Reject malformed packages, nested APKs and protected byte strings."""
        try:
            with zipfile.ZipFile(apk) as archive:
                names = archive.namelist()
                if archive.testzip() is not None:
                    raise ValueError("generated APK contains a corrupt entry")
                required = {"AndroidManifest.xml", "classes.dex"}
                if not required.issubset(names):
                    raise ValueError("generated APK is missing required Android entries")
                nested = [name for name in names if name.lower().endswith(".apk")]
                if nested:
                    raise ValueError("generated APK embeds another APK")
                probes = []
                for secret in self.protected_strings:
                    if secret:
                        probes.extend((secret.encode("utf-8"),
                                       secret.encode("utf-16le"),
                                       secret.encode("utf-16be")))
                if probes:
                    for info in archive.infolist():
                        if info.is_dir():
                            continue
                        with archive.open(info) as stream:
                            overlap = b""
                            while True:
                                chunk = stream.read(1024 * 1024)
                                if not chunk:
                                    break
                                data = overlap + chunk
                                if any(probe in data for probe in probes):
                                    raise ValueError(
                                        "protected target information leaked into APK")
                                overlap = data[-512:]
                signatures = list(self.source_visual_signatures or [])
                patches = list(self.protected_patch_signatures or [])
                if signatures or patches:
                    for info in archive.infolist():
                        if (info.is_dir() or Path(info.filename).suffix.lower()
                                not in IMAGE_SUFFIXES):
                            continue
                        try:
                            with Image.open(BytesIO(archive.read(info))) as image:
                                candidate = _visual_signature(image)
                        except (OSError, ValueError):
                            continue
                        if any(_signature_matches(candidate, item)
                               for item in signatures + patches):
                            raise ValueError(
                                "protected exploration image leaked into APK")
        except zipfile.BadZipFile as exc:
            raise ValueError("generated APK is not a valid ZIP package") from exc

    def _permission_gate(self) -> None:
        manifest = self.project_dir / "app" / "src" / "main" / "AndroidManifest.xml"
        text = manifest.read_text(encoding="utf-8") if manifest.is_file() else ""
        found = sorted(p for p in DANGEROUS_PERMISSIONS if p in text)
        if found:
            raise ValueError("unapproved dangerous Android permissions: " +
                             ", ".join(found))

    def _privacy_gate(self) -> None:
        secrets_lower = [item.casefold() for item in self.protected_strings if item]
        if not secrets_lower:
            return
        for path in self.project_dir.rglob("*"):
            if not path.is_file() or path.is_symlink() or \
                    path.suffix.lower() not in TEXT_SUFFIXES:
                continue
            text = path.read_text(encoding="utf-8", errors="ignore").casefold()
            if any(secret in text for secret in secrets_lower):
                raise ValueError(
                    f"protected target information leaked into project: "
                    f"{path.relative_to(self.project_dir).as_posix()}")

    def _handoff_copy_gate(self) -> None:
        """Screenshots are visual references, not shippable reproduction assets."""
        if self.artifact_dir is None:
            return
        manifest = self.artifact_dir / "manifest.json"
        if not manifest.is_file():
            return
        data = json.loads(manifest.read_text(encoding="utf-8"))
        frame_hashes = {
            item.get("sha256") for item in data.get("items", [])
            if str(item.get("path", "")).lower().endswith(
                (".png", ".jpg", ".jpeg"))
        }
        frame_hashes.discard(None)
        for path in self.project_dir.rglob("*"):
            if not path.is_file() or path.is_symlink():
                continue
            relative = path.relative_to(self.project_dir)
            if any(part in ("build", ".gradle") for part in relative.parts):
                continue
            if hashlib.sha256(path.read_bytes()).hexdigest() in frame_hashes:
                raise ValueError(
                    "exploration screenshot was copied into the Android project; "
                    "use synthetic materials instead")
            if path.suffix.lower() in IMAGE_SUFFIXES:
                try:
                    with Image.open(path) as image:
                        candidate = _visual_signature(image)
                except (OSError, ValueError):
                    continue
                signatures = list(self.source_visual_signatures or [])
                signatures.extend(self.protected_patch_signatures or [])
                if any(_signature_matches(candidate, item)
                       for item in signatures):
                    raise ValueError(
                        "exploration image was copied or transformed into the "
                        "Android project; use synthetic materials instead")

    def _review_visual_privacy_gate(self) -> None:
        """Reject protected source regions resurfacing in trusted review frames."""
        patches = list(self.protected_patch_signatures or [])
        regions = list(self.protected_regions or [])
        if not patches or not regions:
            return
        for path in self.review_dir.rglob("*.png"):
            if path.is_symlink():
                raise ValueError("Android review must not contain symlinks")
            try:
                with Image.open(path) as opened:
                    image = ImageOps.exif_transpose(opened).convert("RGB")
                    for region in regions:
                        box = _region_box(region, *image.size)
                        if box[0] >= box[2] or box[1] >= box[3]:
                            continue
                        candidate = _visual_signature(image.crop(box))
                        if any(_signature_matches(candidate, item)
                               for item in patches):
                            raise ValueError(
                                "protected target pixels leaked into Android "
                                "review evidence")
            except OSError as exc:
                raise ValueError("Android review contains an invalid image") from exc

    def mark_review_accepted(self) -> str:
        self.accepted_project_hash = self.project_fingerprint()
        apk = self.artifacts_dir / "app-debug.apk"
        if not apk.is_file():
            raise FileNotFoundError("final APK is missing")
        self.accepted_apk_hash = hashlib.sha256(apk.read_bytes()).hexdigest()
        return self.accepted_project_hash

    def finish(self, *, review_summary: Mapping) -> dict:
        if not review_summary.get("accepted"):
            raise ValueError("Android reproduction review has not been accepted")
        if not (self.artifacts_dir / "app-debug.apk").is_file():
            raise FileNotFoundError("final APK is missing")
        accepted_hash = str(review_summary.get("accepted_project_hash") or "")
        if not accepted_hash or accepted_hash != self.project_fingerprint():
            raise ValueError("Android project changed after review acceptance")
        apk = self.artifacts_dir / "app-debug.apk"
        if (not self.accepted_apk_hash or
                hashlib.sha256(apk.read_bytes()).hexdigest() !=
                self.accepted_apk_hash):
            raise ValueError("Android APK changed after review acceptance")
        entries = list(self.project_dir.rglob("*"))
        if any(path.is_symlink() for path in entries):
            raise ValueError("Android output must not contain symlinks")
        self._permission_gate()
        self._privacy_gate()
        self._handoff_copy_gate()
        self._review_visual_privacy_gate()
        self._apk_archive_gate(apk)
        common = ensure_materials().resolve()
        mobile = ensure_app_materials().resolve()
        if (self.common_material_hash != _tree_fingerprint(common) or
                self.mobile_material_hash != _tree_fingerprint(mobile)):
            raise ValueError("public Android materials changed during reproduction")
        summary_text = json.dumps(dict(review_summary), ensure_ascii=False)
        for secret in self.protected_strings:
            if secret and secret.casefold() in summary_text.casefold():
                raise ValueError("protected target information leaked into review summary")
        self.close()
        (self.review_dir / "review_summary.json").write_text(
            json.dumps(dict(review_summary), ensure_ascii=False, indent=2),
            encoding="utf-8")
        return {"finished": True, "handoff_id": self.handoff_id,
                "source_mode": self.source_mode,
                "output": f"app_output/{self.handoff_id}",
                "apk": f"app_output/{self.handoff_id}/artifacts/app-debug.apk"}

    def close(self) -> None:
        if self.running:
            _docker("rm", "-f", "-v", self.container_name,
                    check=False, timeout=60)
            self.running = False
        if self.artifact_dir is not None:
            _remove_bundle(self.artifact_dir, self.handoff_id)
            self.artifact_dir = None

    def started_payload(self) -> dict:
        return {"stage": "app_reproduction", "ready": True,
                "source_mode": self.source_mode, "handoff_id": self.handoff_id,
                "output": f"app_output/{self.handoff_id}",
                "sandbox": {"workspace": "/workspace",
                            "topology": "/input/functional_topology.json",
                            "exploration_artifacts": "/exploration",
                            "common_materials": "/materials/common",
                            "mobile_materials": "/materials/mobile",
                            "inputs_read_only": True, "network": "none",
                            "build": "gradle --offline assembleDebug"}}


__all__ = [
    "AppReproductionWorkspace", "DockerUnavailableError",
    "android_build_image_available", "build_android_build_image",
    "require_android_build_image",
]
