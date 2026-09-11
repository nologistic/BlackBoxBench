"""Operator-only Android APK registry.

The registry stores APKs below the sensitive runs tree.  Agent-facing listing
methods intentionally omit paths, hashes, package names, activities, profile
locations and privacy rules.
"""
from __future__ import annotations

import hashlib
import json
import re
import shutil
import subprocess
from dataclasses import asdict, dataclass, field
from pathlib import Path

from .. import config
from .toolchain import AndroidToolchain

SAFE_ID = re.compile(r"^[a-z][a-z0-9_-]{1,63}$")
PACKAGE = re.compile(r"^[A-Za-z][A-Za-z0-9_]*(?:\.[A-Za-z][A-Za-z0-9_]*)+$")
NETWORK_POLICIES = {"offline", "public"}
ORIENTATIONS = {"auto", "portrait", "landscape"}
RESET_STRATEGIES = {"clear_data", "restart", "snapshot"}


@dataclass
class AndroidTargetSpec:
    app_id: str
    description: str
    apk_path: str
    package_name: str
    launch_activity: str
    apk_sha256: str = ""
    brief: str = ""
    orientation: str = "portrait"
    reset_strategy: str = "clear_data"
    network_policy: str = "offline"
    profile_snapshot: str = ""
    protected_strings: list[str] = field(default_factory=list)
    protected_regions: list[dict] = field(default_factory=list)
    source_type: str = "external"
    platform: str = "android"
    kind: str = "android"
    seed: str = "seed_001"
    precheck: str = ""
    # Special-access grants applied right after the APK install (before the
    # first launch). MANAGE_EXTERNAL_STORAGE-style gates send the target to
    # the system All-files-access settings page, where the per-step
    # foreground-restore guard keeps failing and exploration dead-ends at the
    # permission gate (observed on the 2026-09-08 fossify_gallery baseline:
    # "target App could not be restored to foreground" until finalize).
    pregrant_permissions: list[str] = field(default_factory=list)
    pregrant_appops: list[str] = field(default_factory=list)

    @property
    def apk(self) -> Path:
        return Path(self.apk_path).resolve()


def _registry_file() -> Path:
    return config.ANDROID_TARGETS_DIR / "targets.json"


def _sample_spec() -> AndroidTargetSpec:
    apk = (config.SAMPLE_APPS_DIR / "android_commerce_demo" / "app" /
           "build" / "outputs" / "apk" / "debug" / "app-debug.apk")
    return AndroidTargetSpec(
        app_id="android_commerce_demo",
        description="Deterministic Android commerce demo for black-box exploration.",
        apk_path=str(apk),
        package_name="com.blackboxbench.commercedemo",
        launch_activity=".MainActivity",
        apk_sha256=(hashlib.sha256(apk.read_bytes()).hexdigest()
                    if apk.is_file() else ""),
        brief=("这是一个确定性的移动电商测试 App。测试账号为 linxi，密码为 "
               "demo123。请仅通过当前可见屏幕和坐标级触控探索其功能。"),
        orientation="portrait",
        reset_strategy="clear_data",
        network_policy="offline",
        source_type="sample",
    )


def _load_registered() -> dict[str, AndroidTargetSpec]:
    path = _registry_file()
    if not path.is_file():
        return {}
    raw = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(raw, dict):
        raise ValueError("Android target registry must be an object")
    result = {}
    for key, value in raw.items():
        if not isinstance(value, dict):
            raise ValueError(f"invalid Android target entry: {key}")
        spec = AndroidTargetSpec(**value)
        _validate_spec(spec)
        result[key] = spec
    return result


def _save_registered(items: dict[str, AndroidTargetSpec]) -> None:
    root = config.ANDROID_TARGETS_DIR
    root.mkdir(parents=True, exist_ok=True)
    payload = {key: asdict(value) for key, value in sorted(items.items())}
    tmp = _registry_file().with_suffix(".tmp")
    tmp.write_text(json.dumps(payload, ensure_ascii=False, indent=2),
                   encoding="utf-8")
    tmp.replace(_registry_file())


def _validate_spec(spec: AndroidTargetSpec) -> None:
    if not SAFE_ID.fullmatch(spec.app_id):
        raise ValueError("target_id must use lowercase letters, digits, _ or -")
    if not PACKAGE.fullmatch(spec.package_name):
        raise ValueError("invalid Android package name")
    if not spec.launch_activity or any(c in spec.launch_activity for c in "\r\n\0"):
        raise ValueError("invalid launch activity")
    if spec.network_policy not in NETWORK_POLICIES:
        raise ValueError("network_policy must be offline or public")
    if spec.orientation not in ORIENTATIONS:
        raise ValueError("orientation must be auto, portrait or landscape")
    if spec.reset_strategy not in RESET_STRATEGIES:
        raise ValueError("invalid reset_strategy")
    if spec.apk_sha256 and not re.fullmatch(r"[0-9a-f]{64}", spec.apk_sha256):
        raise ValueError("invalid APK SHA-256")
    if any(not isinstance(value, str) or not value or
           any(c in value for c in "\r\n\0")
           for value in spec.protected_strings):
        raise ValueError("protected strings must be non-empty single-line text")
    for region in spec.protected_regions:
        if not isinstance(region, dict):
            raise ValueError("protected regions must be objects")
        try:
            x, y = int(region["x"]), int(region["y"])
            width, height = int(region["width"]), int(region["height"])
        except (KeyError, TypeError, ValueError) as exc:
            raise ValueError("invalid protected screenshot region") from exc
        if x < 0 or y < 0 or width <= 0 or height <= 0:
            raise ValueError("invalid protected screenshot region")
    for field_name in ("pregrant_permissions", "pregrant_appops"):
        for value in getattr(spec, field_name):
            if (not isinstance(value, str) or not value.strip() or
                    any(c in value for c in " \t\r\n\0")):
                raise ValueError(
                    f"{field_name} entries must be non-empty single-line tokens")


def _extract_metadata(apk: Path) -> tuple[str, str]:
    """Trusted operator helper; its output is never returned to an Agent."""
    tc = AndroidToolchain.discover()
    candidates = [
        tc.sdk_root / "cmdline-tools" / "latest" / "bin" /
        ("apkanalyzer.bat" if __import__("os").name == "nt" else "apkanalyzer"),
    ]
    analyzer = next((p for p in candidates if p.is_file()), None)
    if analyzer is not None:
        try:
            package = subprocess.run(
                [str(analyzer), "manifest", "application-id", str(apk)],
                text=True, capture_output=True, check=True,
                timeout=30).stdout.strip()
            activity = subprocess.run(
                [str(analyzer), "manifest", "launchable-activity", str(apk)],
                text=True, capture_output=True, check=True,
                timeout=30).stdout.strip()
            if package and activity:
                return package, activity
        except (OSError, subprocess.SubprocessError):
            pass
    suffix = ".exe" if __import__("os").name == "nt" else ""
    for tool_name in ("aapt", "aapt2"):
        aapt = tc.sdk_root / "build-tools" / "35.0.0" / (tool_name + suffix)
        if not aapt.is_file():
            continue
        try:
            result = subprocess.run([str(aapt), "dump", "badging", str(apk)],
                                    text=True, capture_output=True, check=True,
                                    timeout=30)
        except (OSError, subprocess.SubprocessError):
            continue
        package_match = re.search(r"^package: name='([^']+)'", result.stdout,
                                  re.MULTILINE)
        activity_match = re.search(
            r"^launchable-activity: name='([^']+)'", result.stdout,
            re.MULTILINE)
        if package_match and activity_match:
            return package_match.group(1), activity_match.group(1)
    raise RuntimeError(
        "package/activity were not provided and apkanalyzer could not extract them")


def register_android_target(*, target_id: str, apk: Path,
                            description: str, brief: str = "",
                            package_name: str = "", launch_activity: str = "",
                            orientation: str = "auto",
                            reset_strategy: str = "clear_data",
                            network_policy: str = "offline",
                            profile_snapshot: Path | None = None,
                            protected_strings: list[str] | None = None,
                            protected_regions: list[dict] | None = None,
                            pregrant_permissions: list[str] | None = None,
                            pregrant_appops: list[str] | None = None,
                            replace: bool = False) -> AndroidTargetSpec:
    if not SAFE_ID.fullmatch(target_id):
        raise ValueError("target_id must use lowercase letters, digits, _ or -")
    source = Path(apk).expanduser().resolve()
    if not source.is_file() or source.suffix.lower() != ".apk":
        raise FileNotFoundError("a local .apk file is required")
    if not package_name or not launch_activity:
        package_name, launch_activity = _extract_metadata(source)
    digest = hashlib.sha256(source.read_bytes()).hexdigest()
    target_root = config.ANDROID_TARGETS_DIR / "artifacts" / target_id
    destination = target_root / f"{digest}.apk"
    items = _load_registered()
    if target_id in items and not replace:
        raise FileExistsError(f"Android target already exists: {target_id}")
    profile_value = ""
    profile: Path | None = None
    if profile_snapshot is not None:
        profile = Path(profile_snapshot).expanduser().resolve()
        profiles_root = (config.ANDROID_TARGETS_DIR / "profiles" / target_id)
        if not profile.is_dir():
            raise FileNotFoundError("profile_snapshot must be a directory")
        profile_value = str(profiles_root)
    spec = AndroidTargetSpec(
        app_id=target_id, description=description, apk_path=str(destination),
        package_name=package_name, launch_activity=launch_activity,
        apk_sha256=digest, brief=brief,
        orientation=orientation, reset_strategy=reset_strategy,
        network_policy=network_policy, profile_snapshot=profile_value,
        protected_strings=list(protected_strings or []),
        protected_regions=list(protected_regions or []),
        seed=digest[:16],
        pregrant_permissions=list(pregrant_permissions or []),
        pregrant_appops=list(pregrant_appops or []))
    _validate_spec(spec)
    target_root.mkdir(parents=True, exist_ok=True)
    if not destination.exists():
        shutil.copy2(source, destination)
    if profile is not None:
        profiles_root = Path(profile_value)
        if profiles_root.exists():
            shutil.rmtree(profiles_root)
        shutil.copytree(profile, profiles_root)
    items[target_id] = spec
    _save_registered(items)
    return spec


def unregister_android_target(target_id: str, *,
                              keep_artifacts: bool = False) -> None:
    """Remove an operator-registered target and, by default, its stored APK."""
    if target_id == "android_commerce_demo":
        raise ValueError("the bundled sample target cannot be unregistered")
    items = _load_registered()
    if target_id not in items:
        raise KeyError(target_id)
    del items[target_id]
    _save_registered(items)
    if keep_artifacts:
        return
    root = config.ANDROID_TARGETS_DIR
    for directory in (root / "artifacts" / target_id,
                      root / "profiles" / target_id,
                      root / "leases" / target_id):
        resolved = directory.resolve()
        # Refuse to delete anything that escaped the protected target tree.
        if root.resolve() in resolved.parents and resolved.is_dir():
            shutil.rmtree(resolved, ignore_errors=True)


def rename_android_target(old_id: str, new_id: str) -> AndroidTargetSpec:
    """Re-register a target under a new id, preserving every recorded field.

    Target ids appear in session metadata, checklists and report file names, so
    a dataset that was first registered under a provisional name can be given
    its real one without re-extracting the APK.
    """
    if not SAFE_ID.fullmatch(new_id):
        raise ValueError("target_id must use lowercase letters, digits, _ or -")
    if old_id == "android_commerce_demo":
        raise ValueError("the bundled sample target cannot be renamed")
    items = _load_registered()
    if old_id not in items:
        raise KeyError(old_id)
    if new_id in items:
        raise FileExistsError(f"Android target already exists: {new_id}")
    old = items[old_id]
    profile = Path(old.profile_snapshot) if old.profile_snapshot else None
    spec = register_android_target(
        target_id=new_id, apk=old.apk, description=old.description,
        brief=old.brief, package_name=old.package_name,
        launch_activity=old.launch_activity, orientation=old.orientation,
        reset_strategy=old.reset_strategy, network_policy=old.network_policy,
        profile_snapshot=profile if profile and profile.is_dir() else None,
        protected_strings=old.protected_strings,
        protected_regions=old.protected_regions,
        pregrant_permissions=old.pregrant_permissions,
        pregrant_appops=old.pregrant_appops)
    unregister_android_target(old_id)
    return spec


def get_android_target(target_id: str) -> AndroidTargetSpec:
    if target_id == "android_commerce_demo":
        return _sample_spec()
    items = _load_registered()
    if target_id not in items:
        raise KeyError(target_id)
    return items[target_id]


def list_android_targets() -> list[dict]:
    specs = [_sample_spec(), *_load_registered().values()]
    return [{"app_id": spec.app_id, "description": spec.description,
             "platform": "android", "kind": "sample" if spec.app_id ==
             "android_commerce_demo" else "external",
             "available": spec.apk.is_file(),
             "network_policy": spec.network_policy}
            for spec in specs]
