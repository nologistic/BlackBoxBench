"""Pre-flight checks for live targets (benchmark-internal, pixels only).

These run controller-side BEFORE a session is exposed to the agent. They use
only runtime.screenshot() — the same pixel channel the agent gets — never
DOM/JS. A check compares a small fixed screen region against a reference crop
captured by the operator at login time (scripts/live_login.py --capture).

Why pixels: the CDP whitelist forbids semantic channels in benchmark/runtime,
and that wall is not lowered for operator tooling either.
"""
from __future__ import annotations

from pathlib import Path

from PIL import Image

from ..recorder import diff as imgdiff
from ..runtime.live_chromium import live_state_dir
from ..runtime.base import Runtime


class PreflightError(RuntimeError):
    """The live target is not in the required operator state (e.g. logged out)."""


# Per-target avatar calibration on the 1440x900 viewport:
#   box:      the logged-in avatar's exact crop at capture time
#   search_x: the horizontal strip the crop is template-matched across
#             (headers shift a few px as marketing slots come and go)
# Currently empty: no registered target carries an avatar precheck. A new
# calibration is registered here after scripts/live_login.py --capture saves
# the header reference crop for that target.
_AVATAR_ZONES: dict[str, dict] = {}

DIFF_THRESHOLD = 0.15


def zone_for(app_id: str) -> dict | None:
    return _AVATAR_ZONES.get(app_id)


def avatar_logged_in(runtime: Runtime, app_id: str) -> None:
    """Logged-in iff the target's header contains the operator's avatar.

    The reference crop is the avatar zone saved at capture time; it is
    template-matched across a header strip to tolerate small horizontal
    shifts from A/B marketing slots.
    """
    zone = _AVATAR_ZONES.get(app_id)
    if zone is None:
        raise PreflightError(
            f"{app_id}: 未校准头像区域 —— 在 prechecks._AVATAR_ZONES 中登记。")
    ref_path = live_state_dir(app_id) / "header_ref.png"
    if not ref_path.exists():
        raise PreflightError(
            f"{app_id}: 缺少登录参考图 {ref_path} —— 请先运行 "
            f"scripts/live_login.py --capture 完成一次人工登录。")
    ref = Image.open(ref_path).convert("RGB")
    tw, th = ref.size
    img = imgdiff.load(runtime.screenshot())
    y0 = zone["box"][1]
    best = 1.0
    for x in range(zone["search_x"][0], zone["search_x"][1] - tw + 1):
        d = imgdiff.diff_score(img.crop((x, y0, x + tw, y0 + th)), ref)
        if d < best:
            best = d
            if best < 0.02:
                break  # unambiguous match
    if best > DIFF_THRESHOLD:
        raise PreflightError(
            f"{app_id}: 当前不是登录状态 (header diff={best:.3f}) —— "
            f"请运行 vendor/python/python.exe scripts/live_login.py "
            f"--capture 人工重新登录。")


PRECHECKS = {
    "avatar_logged_in": avatar_logged_in,
}
