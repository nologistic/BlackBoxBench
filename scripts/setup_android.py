"""Bootstrap the pinned host Android emulator toolchain.

The script is explicit because the SDK/system image download is large and the
Google license acceptance must be an operator action.  It never modifies a
system-wide Android installation.
"""
from __future__ import annotations

import argparse
import os
import shutil
import subprocess
import sys
import tempfile
import urllib.request
import zipfile
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DEFAULT_SDK = ROOT / "vendor" / "android" / "sdk"
DEFAULT_AVD = ROOT / "vendor" / "android" / "avd"
sys.path.insert(0, str(ROOT))
from benchmark.android.toolchain import AndroidToolchain, load_toolchain_lock

LOCK = load_toolchain_lock()
PLATFORM_KEY = "windows" if os.name == "nt" else "linux"
TOOLS_URL = os.environ.get(
    "BBB_ANDROID_CMDLINE_TOOLS_URL",
    LOCK["command_line_tools"][f"{PLATFORM_KEY}_url"])
TOOLS_SHA256 = LOCK["command_line_tools"][f"{PLATFORM_KEY}_sha256"]
SYSTEM_IMAGE = "system-images;android-35;google_apis;x86_64"


def _exe(name: str) -> str:
    return f"{name}.bat" if os.name == "nt" and name in (
        "sdkmanager", "avdmanager") else (f"{name}.exe" if os.name == "nt" else name)


def _java17(java_home: Path | None) -> Path:
    candidates = []
    if java_home:
        candidates.append(java_home / "bin" / ("java.exe" if os.name == "nt" else "java"))
    candidates.append(ROOT / "vendor" / "jdk17" / "bin" /
                      ("java.exe" if os.name == "nt" else "java"))
    env = os.environ.get("BBB_JAVA_HOME") or os.environ.get("JAVA_HOME")
    if env:
        candidates.append(Path(env) / "bin" / ("java.exe" if os.name == "nt" else "java"))
    found = shutil.which("java.exe" if os.name == "nt" else "java")
    if found:
        candidates.append(Path(found))
    for candidate in candidates:
        if not candidate.is_file():
            continue
        result = subprocess.run([str(candidate), "-version"], text=True,
                                capture_output=True, check=False)
        version = result.stderr + result.stdout
        match = re.search(r'version "(\d+)', version)
        if match and int(match.group(1)) >= 17:
            return candidate
    raise RuntimeError(
        "JDK 17+ is required. Install Temurin/OpenJDK 17 and pass --java-home.")


def _install_tools(sdk: Path) -> None:
    latest = sdk / "cmdline-tools" / "latest"
    manager = latest / "bin" / _exe("sdkmanager")
    if manager.is_file():
        return
    with tempfile.TemporaryDirectory(prefix="bbb-android-tools-") as temp:
        archive = Path(temp) / "tools.zip"
        print(f"Downloading Android command-line tools from {TOOLS_URL}")
        urllib.request.urlretrieve(TOOLS_URL, archive)
        import hashlib
        actual = hashlib.sha256(archive.read_bytes()).hexdigest()
        if actual != TOOLS_SHA256:
            raise RuntimeError(
                f"Android command-line tools checksum mismatch: {actual}")
        extracted = Path(temp) / "unpacked"
        with zipfile.ZipFile(archive) as package:
            package.extractall(extracted)
        source = extracted / "cmdline-tools"
        latest.parent.mkdir(parents=True, exist_ok=True)
        if latest.exists():
            shutil.rmtree(latest)
        shutil.move(str(source), str(latest))


def main() -> None:
    parser = argparse.ArgumentParser(description="Install BlackBoxBench Android toolchain")
    parser.add_argument("--sdk-root", type=Path, default=DEFAULT_SDK)
    parser.add_argument("--avd-root", type=Path, default=DEFAULT_AVD)
    parser.add_argument("--java-home", type=Path)
    parser.add_argument("--accept-licenses", action="store_true",
                        help="confirm acceptance of Android SDK licenses")
    parser.add_argument("--preflight", action="store_true",
                        help="only report the current setup")
    args = parser.parse_args()
    java = _java17(args.java_home)
    if args.preflight:
        toolchain = AndroidToolchain(
            args.sdk_root.expanduser().resolve(),
            args.sdk_root.expanduser().resolve() / "platform-tools" /
            ("adb.exe" if os.name == "nt" else "adb"),
            args.sdk_root.expanduser().resolve() / "emulator" /
            ("emulator.exe" if os.name == "nt" else "emulator"),
            args.avd_root.expanduser().resolve() /
            f"{LOCK['avd']['name']}.avd", java.parent.parent)
        missing = toolchain.missing()
        print("Android host preflight: " + ("ready" if not missing else "incomplete"))
        for item in missing:
            print(f"  missing: {item}")
        raise SystemExit(0 if not missing else 2)
    if not args.accept_licenses:
        raise SystemExit(
            "Refusing the large SDK install without --accept-licenses. Review "
            "the Android SDK licenses, then rerun with that flag.")
    sdk = args.sdk_root.expanduser().resolve()
    avd = args.avd_root.expanduser().resolve()
    _install_tools(sdk)
    env = dict(os.environ)
    env["ANDROID_SDK_ROOT"] = str(sdk); env["ANDROID_HOME"] = str(sdk)
    env["JAVA_HOME"] = str(java.parent.parent)
    env["ANDROID_AVD_HOME"] = str(avd)
    manager = sdk / "cmdline-tools" / "latest" / "bin" / _exe("sdkmanager")
    subprocess.run([str(manager), "--sdk_root=" + str(sdk), "--licenses"],
                   input="y\n" * 100, text=True, check=True, env=env)
    subprocess.run([str(manager), "--sdk_root=" + str(sdk),
                    *LOCK["sdk_packages"].keys()], check=True, env=env)
    avd.mkdir(parents=True, exist_ok=True)
    avdmanager = sdk / "cmdline-tools" / "latest" / "bin" / _exe("avdmanager")
    subprocess.run([str(avdmanager), "create", "avd", "--force", "--name",
                    LOCK["avd"]["name"], "--package", SYSTEM_IMAGE, "--device",
                    LOCK["avd"]["device"]],
                   input="no\n", text=True, check=True, env=env)
    print("Android toolchain ready.")
    print(f"Set BBB_ANDROID_SDK_ROOT={sdk}")
    print(f"Set BBB_ANDROID_AVD_TEMPLATE={avd / 'BBB_Base.avd'}")
    print(f"Set BBB_JAVA_HOME={java.parent.parent}")
    AndroidToolchain(
        sdk, sdk / "platform-tools" / ("adb.exe" if os.name == "nt" else "adb"),
        sdk / "emulator" / ("emulator.exe" if os.name == "nt" else "emulator"),
        avd / f"{LOCK['avd']['name']}.avd", java.parent.parent).require()


if __name__ == "__main__":
    main()
