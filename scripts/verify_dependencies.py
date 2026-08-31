"""Read-only verification of the project's reproducibility prerequisites."""
from __future__ import annotations

import argparse
import importlib.metadata
import json
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))

from benchmark.android.toolchain import AndroidToolchain

EXPECTED_PYTHON = (3, 12)


def _requirements(path: Path) -> dict[str, str]:
    values = {}
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        name, expected = line.split("==", 1)
        values[name.casefold().replace("_", "-")] = expected
    return values


def _run(command: list[str], timeout: int = 30) -> subprocess.CompletedProcess:
    return subprocess.run(command, text=True, capture_output=True, check=False,
                          timeout=timeout,
                          creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))


def _docker_images() -> list[dict]:
    lock = json.loads((ROOT / "requirements" /
                       "container-images.lock.json").read_text(encoding="utf-8"))
    results = []
    for image in lock["images"]:
        immutable = f"{image['name'].split(':', 1)[0]}@{image['digest']}"
        probe = _run(["docker", "image", "inspect", immutable,
                      "--format", "{{.Id}}"])
        results.append({"name": image["name"], "digest": image["digest"],
                        "ready": probe.returncode == 0})
    return results


def _python_packages() -> list[dict]:
    results = []
    for name, expected in _requirements(
            ROOT / "requirements" / "runtime.lock.txt").items():
        try:
            actual = importlib.metadata.version(name)
        except importlib.metadata.PackageNotFoundError:
            actual = ""
        results.append({"name": name, "expected": expected, "actual": actual,
                        "ready": actual == expected})
    return results


def report(full: bool = False) -> dict:
    docker = _run(["docker", "version", "--format", "{{.Server.Version}}"])
    android_missing = AndroidToolchain.discover().missing()
    direct = _requirements(ROOT / "requirements.txt")
    locked = _requirements(ROOT / "requirements" / "runtime.lock.txt")
    lock_consistent = all(locked.get(name) == version
                          for name, version in direct.items())
    python_ready = sys.version_info[:2] == EXPECTED_PYTHON
    value = {
        "schema_version": 1,
        "python": {"version": sys.version.split()[0],
                   "ready": python_ready,
                   "lock_consistent": lock_consistent,
                   "packages": _python_packages()},
        "docker": {"ready": docker.returncode == 0,
                   "server_version": docker.stdout.strip(),
                   "base_images": _docker_images() if docker.returncode == 0 else []},
        "android": {"ready": not android_missing, "problems": android_missing},
    }
    if full and docker.returncode == 0:
        generated = []
        for name in (
            "blackboxbench/android-reproduction-workbench:1",
            "blackboxbench/reproduction-workbench:latest",
            "blackboxbench/reference-raw:latest",
            "blackboxbench/self-tool-builder:latest",
        ):
            probe = _run(["docker", "image", "inspect", name,
                          "--format", "{{.Id}}"])
            generated.append({"name": name, "ready": probe.returncode == 0})
        value["docker"]["generated_images"] = generated
    return value


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--full", action="store_true",
                        help="also require locally built project images")
    parser.add_argument("--json", action="store_true")
    args = parser.parse_args()
    value = report(args.full)
    if args.json:
        print(json.dumps(value, ensure_ascii=False, indent=2))
    else:
        package_failures = [item for item in value["python"]["packages"]
                            if not item["ready"]]
        image_failures = [item for item in value["docker"].get("base_images", [])
                          if not item["ready"]]
        generated_failures = [item for item in
                              value["docker"].get("generated_images", [])
                              if not item["ready"]]
        print(f"Python {value['python']['version']}: " +
              ("ready" if (value["python"]["ready"] and
                            value["python"]["lock_consistent"] and
                            not package_failures) else "dependency mismatch"))
        if not value["python"]["ready"]:
            print(f"  Python: {EXPECTED_PYTHON[0]}.{EXPECTED_PYTHON[1]} required")
        if not value["python"]["lock_consistent"]:
            print("  Python: requirements.txt differs from runtime.lock.txt")
        print("Docker: " + (value["docker"]["server_version"] or "unavailable"))
        print("Pinned base images: " +
              ("ready" if not image_failures else "missing"))
        print("Android toolchain: " +
              ("ready" if value["android"]["ready"] else "incomplete"))
        for item in package_failures:
            print(f"  Python: {item['name']} {item['expected']} required, "
                  f"found {item['actual'] or 'missing'}")
        for item in image_failures + generated_failures:
            print(f"  Image missing: {item['name']}")
        for item in value["android"]["problems"]:
            print(f"  Android: {item}")
    failures = (
        not value["python"]["ready"] or
        not value["python"]["lock_consistent"] or
        any(not item["ready"] for item in value["python"]["packages"]) or
        not value["docker"]["ready"] or
        any(not item["ready"] for item in value["docker"].get("base_images", [])) or
        not value["android"]["ready"] or
        any(not item["ready"] for item in
            value["docker"].get("generated_images", [])))
    raise SystemExit(1 if failures else 0)


if __name__ == "__main__":
    main()
