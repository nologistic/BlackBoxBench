"""Operator tool: open a registered Android target for MANUAL exploration.

This is the human counterpart of an Agent exploration session. It boots a
dedicated emulator clone, installs the registered APK, and hands the window to
you so you can use the App yourself and write the evaluation checklist by hand.

It is deliberately NOT an exploration condition: nothing is recorded as
evidence, no topology is produced, and no handoff is created. It only exists so
an operator can understand a target well enough to author
`review_specs/<target>.json`.

    vendor/python/python.exe scripts/android_manual_session.py --app google_clock

Add `--network public` to give the phone controlled internet access for this
session only. It starts the trusted loopback public-only proxy, forces the guest
through it, and has the guard verifier confirm the guest firewall: loopback,
private, link-local, reserved and multicast destinations stay rejected and only
ports 80/443 are reachable. The registered target keeps its own recorded policy,
so Agent exploration conditions are unaffected.

Interactive commands (typed in this terminal while the emulator is open):

    s   save a screenshot into the session folder
    r   reset the App to its initial state (clears app data)
    h   restart the App without clearing data
    n   print the current guest network status
    q   quit and tear the emulator down
"""
from __future__ import annotations

import argparse
import contextlib
import os
import shutil
import socket
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))

from benchmark import config
from benchmark.android.runtime import AndroidEmulatorRuntime
from benchmark.android.targets import get_android_target

PROXY_SCRIPT = ROOT / "docker" / "public_web_proxy.py"
GUARD_SCRIPT = ROOT / "scripts" / "android_network_guard.py"


def _free_loopback_port() -> int:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as probe:
        probe.bind(("127.0.0.1", 0))
        return probe.getsockname()[1]


@contextlib.contextmanager
def _public_only_proxy():
    """Run the trusted loopback public-only proxy for this session.

    This is the same proxy the trusted web browser uses: it re-validates every
    connection against the resolved address and refuses anything that is not
    globally routable, so a guest App cannot reach the host, the private LAN or
    a link-local metadata service even if DNS says otherwise.
    """
    port = _free_loopback_port()
    endpoint = f"http://127.0.0.1:{port}"
    process = subprocess.Popen(
        [sys.executable, str(PROXY_SCRIPT), "--listen", f"127.0.0.1:{port}"],
        cwd=str(ROOT), stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
        creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))
    try:
        deadline = time.monotonic() + 20
        while True:
            if process.poll() is not None:
                raise RuntimeError("public-only proxy exited during startup")
            try:
                with socket.create_connection(("127.0.0.1", port), timeout=1):
                    break
            except OSError:
                if time.monotonic() >= deadline:
                    raise TimeoutError("public-only proxy did not start")
                time.sleep(0.2)
        print(f"[manual] public-only proxy listening on {endpoint}")
        yield endpoint
    finally:
        process.terminate()
        try:
            process.wait(timeout=10)
        except subprocess.TimeoutExpired:
            process.kill()
        print("[manual] public-only proxy stopped")


def _save_screenshot(runtime: AndroidEmulatorRuntime, directory: Path,
                     counter: int) -> Path:
    directory.mkdir(parents=True, exist_ok=True)
    path = directory / f"manual_{counter:03d}.png"
    path.write_bytes(runtime.screenshot())
    return path


def _network_status(runtime: AndroidEmulatorRuntime) -> str:
    airplane = runtime._run("shell", "settings", "get", "global",
                            "airplane_mode_on", check=False)
    proxy = runtime._run("shell", "settings", "get", "global", "http_proxy",
                         check=False)
    return (f"airplane_mode={(airplane.stdout or '').strip() or '?'} "
            f"http_proxy={(proxy.stdout or '').strip() or 'none'}")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--app", required=True,
                        help="registered Android target id")
    parser.add_argument("--network", choices=["target", "offline", "public"],
                        default="target",
                        help="override the guest network for THIS session only "
                             "(default: use the registered target policy)")
    parser.add_argument("--keep", action="store_true",
                        help="keep the session folder (screenshots) afterwards")
    args = parser.parse_args()

    spec = get_android_target(args.app)
    registered_policy = spec.network_policy
    if args.network != "target" and args.network != registered_policy:
        print(f"[manual] session-only network override: "
              f"{registered_policy} -> {args.network} "
              f"(registered target stays {registered_policy})")
        spec.network_policy = args.network

    session_dir = (config.ANDROID_TARGETS_DIR / "manual_sessions" /
                   f"{args.app}_{time.strftime('%Y%m%d_%H%M%S')}")

    with contextlib.ExitStack() as stack:
        if spec.network_policy == "public":
            endpoint = stack.enter_context(_public_only_proxy())
            # The trusted runtime refuses to start a public target unless BOTH
            # the controlled proxy and the guard verifier are configured; it
            # never silently falls back to unrestricted networking.
            os.environ["BBB_ANDROID_PUBLIC_PROXY"] = endpoint
            os.environ["BBB_ANDROID_NETWORK_GUARD"] = str(GUARD_SCRIPT)

        runtime = AndroidEmulatorRuntime(
            spec, session_dir / "runtime", headed=True, lease_mode="explore")

        print(f"[manual] target        : {spec.app_id}")
        print(f"[manual] package       : {spec.package_name}")
        print(f"[manual] network       : {spec.network_policy}")
        print(f"[manual] session folder: {session_dir}")
        print("[manual] booting a dedicated emulator (this can take a minute) …")

        counter = 0
        try:
            runtime.start()
            info = runtime.info()
            print(f"[manual] emulator ready: {info.width}x{info.height} "
                  f"({runtime.serial})")
            print(f"[manual] network status: {_network_status(runtime)}")
            print()
            print("The emulator window is now open — use the App directly with your")
            print("mouse and keyboard. While exploring, note down the functional")
            print("requirements you want the judge to verify later.")
            print()
            print("Commands: [s]creenshot  [r]eset app data  re[h]start app  "
                  "[n]etwork  [q]uit")
            while True:
                try:
                    command = input("[manual] > ").strip().lower()
                except EOFError:
                    break
                if command in ("q", "quit", "exit"):
                    break
                if command in ("s", "screenshot"):
                    counter += 1
                    path = _save_screenshot(runtime,
                                            session_dir / "screenshots", counter)
                    print(f"[manual] saved {path}")
                elif command in ("r", "reset"):
                    runtime.reset()
                    print("[manual] App data cleared and App relaunched")
                elif command in ("h", "restart"):
                    runtime.restart_app()
                    print("[manual] App restarted (data kept)")
                elif command in ("n", "network"):
                    print(f"[manual] {_network_status(runtime)}")
                elif command:
                    print("[manual] unknown command; use s / r / h / n / q")
        finally:
            print("[manual] shutting the emulator down …")
            runtime.stop()

    screenshots = session_dir / "screenshots"
    if args.keep or (screenshots.is_dir() and any(screenshots.iterdir())):
        print(f"[manual] session kept at: {session_dir}")
        if screenshots.is_dir():
            print(f"[manual] screenshots   : {screenshots}")
    else:
        shutil.rmtree(session_dir, ignore_errors=True)

    print()
    print("Next step — write the human checklist for this target:")
    print(f"  review_specs/{args.app}.json")
    print("  schema and grading rules: docs/evaluation_contract.md")


if __name__ == "__main__":
    main()
