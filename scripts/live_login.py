"""Live-target login maintenance (operator tool, not benchmark code).

  vendor/python/python.exe scripts/live_login.py --app yuque_web --capture
  vendor/python/python.exe scripts/live_login.py --url https://example.com --capture
      Open the live browser on the target site. Log in manually in the window
      (QR scan / password + verification — whatever the site asks). Press
      Enter here once logged in: the browser profile becomes the golden
      backup every later session of this target relies on. Targets with a
      registered avatar precheck also save the header reference crop.

  vendor/python/python.exe scripts/live_login.py --app yuque_web --check
      Launch the live browser and run the target's registered precheck.

Profiles are per-target (runs/live_targets/<app_id>/), so login state for
each site persists independently. Never run this while a benchmark session
of the same target is active: the browser profile is a singleton.
"""
from __future__ import annotations

import argparse
import shutil
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from benchmark.orchestrator import prechecks
from benchmark.orchestrator.apps import get_app, make_live_spec_for_url
from benchmark.runtime import live_chromium as live


def _runtime(spec) -> live.LiveChromiumRuntime:
    work = live.live_state_dir(spec.app_id) / "_login_work"
    work.mkdir(parents=True, exist_ok=True)
    return live.LiveChromiumRuntime(spec, work_dir=work)


def capture(spec) -> None:
    # first-ever capture has no golden to seed from — start from empty profile
    live.profile_dir(spec.app_id).mkdir(parents=True, exist_ok=True)
    rt = _runtime(spec)
    rt.start()
    try:
        print(f"[live-login] 浏览器已打开 {spec.live_url}")
        print("[live-login] 若站点需要登录,请在该窗口中手动完成(扫码/密码+验证)。")
        input("[live-login] 确认已处于期望的登录状态后,回到这里按 Enter 收尾…")
        png = rt.screenshot() if spec.precheck else None
    finally:
        rt.stop()
    state = live.live_state_dir(spec.app_id)
    if png is not None:
        from PIL import Image
        import io
        img = Image.open(io.BytesIO(png)).convert("RGB")
        zone = prechecks.zone_for(spec.app_id)
        if zone:
            ref = state / "header_ref.png"
            img.crop(zone["box"]).save(ref)
            print(f"[live-login] 头像参考图已保存: {ref}")
        else:
            full = state / "capture_full.png"
            img.save(full)
            print(f"[live-login] {spec.app_id} 未校准头像区域,已存全屏截图: {full}")
            print("[live-login] 请在其中定位头像位置,登记 prechecks._AVATAR_ZONES "
                  "后据此裁剪 header_ref.png")
    golden = live.golden_dir(spec.app_id)
    shutil.rmtree(golden, ignore_errors=True)
    shutil.copytree(live.profile_dir(spec.app_id), golden)
    print(f"[live-login] golden profile 已更新: {golden}")
    print(f"[live-login] 完成。之后 {spec.app_id} 的会话将以该登录态启动。")


def check(spec) -> None:
    if not spec.precheck:
        print(f"[live-login] {spec.app_id} 未注册 precheck,无需检查;"
              f"登录态以 capture 时为准。")
        return
    rt = _runtime(spec)
    rt.start()
    try:
        prechecks.PRECHECKS[spec.precheck](rt, spec.app_id)
        print("[live-login] OK: 当前为登录状态。")
    except prechecks.PreflightError as e:
        print(f"[live-login] FAIL: {e}")
        sys.exit(1)
    finally:
        rt.stop()


def main() -> None:
    p = argparse.ArgumentParser(description=__doc__)
    target = p.add_mutually_exclusive_group()
    target.add_argument("--app", default=None,
                        help="已注册目标(默认 yuque_web)")
    target.add_argument("--url", default=None,
                        help="任意站点 URL(临时目标,profile 按站点持久化)")
    g = p.add_mutually_exclusive_group(required=True)
    g.add_argument("--capture", action="store_true", help=argparse.SUPPRESS)
    g.add_argument("--check", action="store_true", help=argparse.SUPPRESS)
    args = p.parse_args()

    spec = make_live_spec_for_url(args.url) if args.url \
        else get_app(args.app or "yuque_web")
    if spec.kind != "live":
        raise SystemExit(f"{spec.app_id} 不是 live target")
    if args.capture:
        capture(spec)
    else:
        check(spec)


if __name__ == "__main__":
    main()
