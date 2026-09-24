"""Project-local Android toolchain discovery and preflight."""
from __future__ import annotations

import os
import json
import re
import shutil
import subprocess
from dataclasses import dataclass
from pathlib import Path

from .. import config

TOOLCHAIN_LOCK = config.PROJECT_ROOT / "requirements" / "android-toolchain.lock.json"


def load_toolchain_lock() -> dict:
    try:
        value = json.loads(TOOLCHAIN_LOCK.read_text(encoding="utf-8"))
    except (OSError, ValueError) as exc:
        raise RuntimeError(f"Android toolchain lock is unreadable: {TOOLCHAIN_LOCK}") from exc
    if not isinstance(value, dict) or value.get("schema_version") != 1:
        raise RuntimeError("unsupported Android toolchain lock schema")
    return value


def _property(path: Path, name: str) -> str:
    if not path.is_file():
        return ""
    for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
        key, separator, value = line.partition("=")
        if separator and key.strip() == name:
            return value.strip()
    return ""


def _exe(name: str) -> str:
    return f"{name}.exe" if os.name == "nt" else name


@dataclass(frozen=True)
class AndroidToolchain:
    sdk_root: Path
    adb: Path
    emulator: Path
    avd_template: Path
    java_home: Path | None

    @classmethod
    def discover(cls) -> "AndroidToolchain":
        sdk_value = (os.environ.get("BBB_ANDROID_SDK_ROOT") or
                     os.environ.get("ANDROID_SDK_ROOT") or
                     os.environ.get("ANDROID_HOME") or
                     str(config.VENDOR_DIR / "android" / "sdk"))
        sdk = Path(sdk_value).expanduser().resolve()
        adb = sdk / "platform-tools" / _exe("adb")
        emulator = sdk / "emulator" / _exe("emulator")
        template_value = os.environ.get("BBB_ANDROID_AVD_TEMPLATE")
        template = (Path(template_value).expanduser().resolve()
                    if template_value else
                    (config.VENDOR_DIR / "android" / "avd" /
                     "BBB_Base.avd").resolve())
        bundled_java = config.VENDOR_DIR / "jdk17"
        java_value = (os.environ.get("BBB_JAVA_HOME") or
                      (str(bundled_java) if bundled_java.is_dir() else None) or
                      os.environ.get("JAVA_HOME"))
        java_home = Path(java_value).expanduser().resolve() if java_value else None
        return cls(sdk, adb, emulator, template, java_home)

    def missing(self) -> list[str]:
        missing = []
        for label, path in (("adb", self.adb), ("emulator", self.emulator),
                            ("base AVD", self.avd_template)):
            if not path.exists():
                missing.append(f"{label}: {path}")
        java = ((self.java_home / "bin" / _exe("java"))
                if self.java_home else None)
        if java is None or not java.is_file():
            found = shutil.which(_exe("java"))
            java = Path(found).resolve() if found else None
        if java is None or not java.is_file():
            missing.append("JDK 17: BBB_JAVA_HOME/JAVA_HOME")
        else:
            try:
                result = subprocess.run([str(java), "-version"], text=True,
                                        capture_output=True, timeout=10,
                                        check=False)
                version = result.stderr + result.stdout
                match = re.search(r'version "(\d+)', version)
                expected_major = int(load_toolchain_lock()["jdk"]["major"])
                if not match or int(match.group(1)) != expected_major:
                    missing.append(f"JDK {expected_major}: {java}")
            except (OSError, subprocess.SubprocessError):
                missing.append(f"working JDK 17: {java}")
        missing.extend(self.version_mismatches())
        return missing

    def version_mismatches(self) -> list[str]:
        lock = load_toolchain_lock()
        packages = lock["sdk_packages"]
        paths = {
            "platform-tools": self.sdk_root / "platform-tools" / "source.properties",
            "emulator": self.sdk_root / "emulator" / "source.properties",
            "platforms;android-35": self.sdk_root / "platforms" / "android-35" /
                                     "source.properties",
            "build-tools;35.0.0": self.sdk_root / "build-tools" / "35.0.0" /
                                  "source.properties",
            "system-images;android-35;google_apis;x86_64": self.sdk_root /
                "system-images" / "android-35" / "google_apis" / "x86_64" /
                "source.properties",
        }
        mismatches = []
        for package, expected in packages.items():
            actual = _property(paths[package], "Pkg.Revision")
            if actual != str(expected):
                mismatches.append(
                    f"{package} revision {expected} required (found {actual or 'missing'})")
        tools_expected = str(lock["command_line_tools"]["revision"])
        tools_path = self.sdk_root / "cmdline-tools" / "latest" / "source.properties"
        tools_actual = _property(tools_path, "Pkg.Revision")
        if tools_actual != tools_expected:
            mismatches.append(
                f"cmdline-tools revision {tools_expected} required "
                f"(found {tools_actual or 'missing'})")
        avd_config = self.avd_template / "config.ini"
        avd_expected = lock["avd"]
        checks = {
            "abi.type": avd_expected["abi"],
            "tag.id": avd_expected["tag"],
            "hw.device.name": avd_expected["device"],
        }
        for key, expected in checks.items():
            actual = _property(avd_config, key)
            if actual != str(expected):
                mismatches.append(
                    f"AVD {key}={expected} required (found {actual or 'missing'})")
        avd_ini = self.avd_template.parent / f"{self.avd_template.stem}.ini"
        target = _property(avd_ini, "target")
        if target != str(avd_expected["target"]):
            mismatches.append(
                f"AVD target={avd_expected['target']} required "
                f"(found {target or 'missing'})")
        return mismatches

    def require(self) -> None:
        missing = self.missing()
        if missing:
            details = "; ".join(missing)
            raise RuntimeError(
                "Android toolchain is incomplete or differs from its lock. "
                "Run scripts/setup_android.py "
                f"and retry. Missing: {details}")

    def environment(self, avd_home: Path | None = None) -> dict[str, str]:
        env = dict(os.environ)
        env["ANDROID_SDK_ROOT"] = str(self.sdk_root)
        env["ANDROID_HOME"] = str(self.sdk_root)
        if self.java_home:
            env["JAVA_HOME"] = str(self.java_home)
        if avd_home is not None:
            env["ANDROID_AVD_HOME"] = str(avd_home)
        path_parts = [str(self.sdk_root / "platform-tools"),
                      str(self.sdk_root / "emulator")]
        if self.java_home:
            path_parts.append(str(self.java_home / "bin"))
        env["PATH"] = os.pathsep.join(path_parts + [env.get("PATH", "")])
        return env


__all__ = ["AndroidToolchain", "TOOLCHAIN_LOCK", "load_toolchain_lock"]
