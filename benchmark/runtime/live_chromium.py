"""Live-target runtime: headed Chromium against a real website (e.g. Douyin).

SECURITY CRITICAL — same rules as local_chromium.py:
The CDP connection is a capability whitelist (Page navigate+screenshot,
Input.*, Emulation viewport, Browser.close). NO Runtime.evaluate, DOM.*,
Accessibility.*, Network.*, Storage.* — the agent receives pixels and only
pixels, exactly as for local targets.

Differences from LocalChromiumRuntime (see docs/security_model.md, live targets):
- headed, not headless: real consumer sites bot-detect headless Chrome and
  serve captcha walls before the first page (verified against douyin.com).
- persistent browser profile holds the operator-maintained login state, with
  a golden backup restored as a self-heal step when the precheck fails.
- normal public network access is available so redirects, authentication, and
  cross-domain assets work. The entry URL is not a hostname/navigation
  allowlist; network-use restrictions are expressed in the agent task brief.
- NO reset-to-S0: reset() is a cold browser restart back to the entry URL.
  Server-side state is untouched; determinism does not apply to live targets.
"""
from __future__ import annotations

import os
import random
import shutil
import subprocess
import time
from pathlib import Path

from .. import config
from .local_chromium import LocalChromiumRuntime, _CDP, _free_port, find_browser


def live_state_dir(app_id: str) -> Path:
    """Operator-owned per-target state: profile/, profile_golden/, header_ref."""
    return config.RUNS_DIR / "live_targets" / app_id


def profile_dir(app_id: str) -> Path:
    return live_state_dir(app_id) / "profile"


def golden_dir(app_id: str) -> Path:
    return live_state_dir(app_id) / "profile_golden"


def ensure_profile(app_id: str, require_golden: bool = False) -> Path:
    """First use: seed the working profile from the golden backup. Targets
    with a registered login precheck REQUIRE a golden (fail fast with
    operator guidance); ad-hoc targets may start from an empty profile."""
    prof = profile_dir(app_id)
    if not prof.exists():
        golden = golden_dir(app_id)
        if golden.exists():
            shutil.copytree(golden, prof)
        elif require_golden:
            raise RuntimeError(
                f"no login profile for {app_id} yet — run "
                f"scripts/live_login.py --capture first")
        else:
            prof.mkdir(parents=True)
    return prof


def restore_golden_files(app_id: str) -> None:
    """Overwrite the working profile with the golden backup (browser must be
    stopped first — profile files are locked while Chromium runs)."""
    golden = golden_dir(app_id)
    if not golden.exists():
        raise RuntimeError(f"no golden profile for {app_id}")
    prof = profile_dir(app_id)
    shutil.rmtree(prof, ignore_errors=True)
    shutil.copytree(golden, prof)


def _clear_stale_singleton(profile_dir):
    """Remove a leftover Chrome singleton lock whose owner process is dead.

    A killed/crashed browser leaves SingletonLock behind; the next headed
    start then prints "Opening in existing browser session." and exits at
    once - that took down the live web batch's browsers on 2026-09-17.
    """
    import os
    lock = profile_dir / "SingletonLock"
    if not (lock.is_symlink() or lock.exists()):
        return
    owner = 0
    try:
        target = os.readlink(lock) if lock.is_symlink() else ""
        if "-" in target:
            owner = int(target.rsplit("-", 1)[1])
    except (OSError, ValueError):
        owner = 0
    if owner > 0:
        try:
            os.kill(owner, 0)
            return  # owner alive: keep the lock
        except OSError:
            pass
    for name in ("SingletonLock", "SingletonCookie", "SingletonSocket"):
        path = profile_dir / name
        try:
            if path.is_symlink() or path.exists():
                path.unlink()
        except OSError:
            pass


class LiveChromiumRuntime(LocalChromiumRuntime):
    """Headed Chromium + persistent profile, pointed at a real website.

    Inherits the whitelisted CDP client and all pixel/HID primitives from
    LocalChromiumRuntime; only the lifecycle (no app subprocess, no gateway,
    headed flags, profile persistence) differs.
    """

    def __init__(self, spec, work_dir: Path):
        self._spec = spec
        self._work_dir = Path(work_dir)
        self._w = config.VIEWPORT_WIDTH
        self._h = config.VIEWPORT_HEIGHT
        self._cdp_port = _free_port()
        self._browser = None
        self._cdp: _CDP | None = None
        # tab-follow tracking (used by _attach/_follow_latest in the base)
        self._ws_url: str | None = None
        self._current_target_id: str | None = None
        self._seen_target_ids: set = set()
        self._tab_order: list = []
        self._last_target_poll = 0.0
        self._profile_dir = ensure_profile(
            spec.app_id, require_golden=bool(spec.precheck))
        self._pointer = (0, 0)
        # base-class machinery we deliberately do not use
        self._app = None
        self._app_cmd = None
        self._app_port = None
        self._secret = None
        self._gw_port = None
        self._gateway = None
        self._gateway_server = None

    # ------------------------------------------------------------ lifecycle

    def start(self) -> None:
        self._work_dir.mkdir(parents=True, exist_ok=True)
        # Concurrent cold starts (a fresh batch boots many browsers at once)
        # can leave the debugger port unreachable or the first load timing
        # out: 2026-09-17 four of eight concurrent runs failed exactly this
        # way while the host had ample CPU. Retry with backoff + jitter
        # instead of bouncing the failure back to the agent.
        delays = (0.0, 5.0, 15.0, 45.0)
        last: Exception | None = None
        for delay in delays:
            if delay:
                time.sleep(delay + random.uniform(0.0, delay * 0.3))
            try:
                self._start_browser()
                return
            except Exception as exc:  # retried below
                last = exc
                try:
                    self._kill_browser()
                except Exception:
                    pass
        raise RuntimeError(
            f"live browser failed to start after {len(delays)} attempts: "
            f"{last!r}") from last

    # stop()/health()/input/screenshot inherited. reset() inherited: cold
    # browser restart on the same profile (NOT a state reset — live targets
    # have no S0).

    def restore_golden(self) -> None:
        """Self-heal path for a failed login precheck."""
        self._kill_browser()
        restore_golden_files(self._spec.app_id)
        self._start_browser()

    # ------------------------------------------------------------ browser

    def _start_browser(self) -> None:
        exe = find_browser()
        self._profile_dir.mkdir(parents=True, exist_ok=True)
        _clear_stale_singleton(self._profile_dir)
        log = open(self._work_dir / "browser.log", "ab")
        args = [
            str(exe),
            # headed on purpose — headless is captcha-walled by the target
            "--no-sandbox", "--disable-crashpad", "--mute-audio",
            f"--remote-debugging-port={self._cdp_port}",
            "--remote-allow-origins=*",
            f"--user-data-dir={self._profile_dir}",
            # extra height for browser chrome; Emulation pins the viewport
            f"--window-size={self._w},{self._h + 100}",
            "--force-device-scale-factor=1",
            "--no-first-run", "--no-default-browser-check",
            "--disable-extensions", "--disable-component-update",
            "--disable-sync", "--disable-translate",
            "--password-store=basic", "--disable-features=TranslateUI",
            "--autoplay-policy=user-gesture-required",
            "--disable-blink-features=AutomationControlled",
            "--lang=zh-CN",
        ]
        # Avoid inheriting a host proxy by default: live targets keep normal
        # public DNS/navigation so site-owned redirects, authentication, and
        # cross-domain assets keep working. BBB_LIVE_PROXY opts the live
        # browser into one explicit proxy instead — for a host that cannot
        # reach overseas targets directly (e.g. a CN server exploring
        # google/youtube/reddit targets through a local proxy).
        live_proxy = os.environ.get("BBB_LIVE_PROXY", "").strip()
        args.append(f"--proxy-server={live_proxy}" if live_proxy
                    else "--no-proxy-server")
        args.append("about:blank")
        self._browser = subprocess.Popen(
            args, stdout=log, stderr=subprocess.STDOUT)
        self._attach(self._await_debugger())
        self._cdp.call("Page.navigate", {"url": self._spec.live_url})
        self._cdp.wait_event("Page.loadEventFired", timeout=20)
        time.sleep(4.0)  # real sites keep rendering after load; settle a bit

    def _kill_browser(self) -> None:
        """Like the base implementation but NEVER deletes the profile — it is
        shared operator state holding the login session."""
        if self._cdp:
            try:
                self._cdp.call("Browser.close", timeout=3)
            except Exception:
                pass
            self._cdp.close()
            self._cdp = None
        if self._browser:
            try:
                self._browser.wait(timeout=5)
            except Exception:
                self._browser.kill()
                self._browser.wait(timeout=5)
            self._browser = None
