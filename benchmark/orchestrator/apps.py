"""Reference app registry.

An AppSpec describes how to launch a black-box reference app for a session.
Adding a new app means registering a spec here (see docs/adding_reference_app.md).
Ground-truth files live with the app and are NEVER read by the controller's
agent-facing paths — they exist only for future evaluator tooling.

Two kinds of targets:
- local: seeded app subprocess behind the loopback gateway (full isolation,
  deterministic reset to S0). This is the benchmark-grade mode.
- live:  a real website (e.g. yuque_web) driven through a headed persistent
  browser profile. No S0 reset, no determinism, internet required — the
  pixel/HID whitelist still applies. See docs/security_model.md (live targets).
"""
from __future__ import annotations

import re
import secrets
import sys
from dataclasses import dataclass, field
from pathlib import Path
from urllib.parse import urlparse

from .. import config


@dataclass
class AppSpec:
    app_id: str
    description: str
    module: str = ""                     # local apps: python -m <module>
    kind: str = "local"                  # "local" | "live"
    seed: str = "seed_001"
    extra_env: dict = field(default_factory=dict)
    # Task brief handed to the agent: what a human tester would be given
    # (e.g. a demo account). NEVER implementation details.
    brief: str = ""
    live_url: str = ""                   # kind=live: entry URL
    precheck: str = ""                   # kind=live: orchestrator.prechecks key
    platform: str = "web"

    def launch_command(self, port: int, data_dir: Path) -> list[str]:
        if self.kind != "local":
            raise RuntimeError(f"{self.app_id}: live targets have no local app")
        return [sys.executable, "-m", self.module,
                "--port", str(port), "--data-dir", str(data_dir)]


_LIVE_SOFT_NETWORK_RULE = (
    "The start URL is only the target's entry point, not a domain or navigation whitelist; the site may redirect, load cross-origin resources, "
    "and be navigated like a human. Do not use network capabilities to actively visit search engines, code-hosting sites, web "
    "archives, or other sites to find the target's original implementation, and do not directly request, download, or read the target page's "
    "source, API responses, or other semantic content.")


_REGISTRY: dict[str, AppSpec] = {
    "ecommerce_demo": AppSpec(
        app_id="ecommerce_demo",
        description="Nimbus Market — seeded ecommerce demo (auth, catalog, cart, "
                    "coupon, checkout, orders, persistence).",
        module="sample_apps.ecommerce_demo.app",
        brief=("This is an e-commerce shopping app. You have a test account: username alice, "
               "password alice123. Logging in is not required — you can explore anonymously first and use it "
               "when needed (e.g. when you hit a login gate)."),
    ),
    "yuque_web": AppSpec(
        app_id="yuque_web",
        kind="live",
        description="Yuque web (live target: login state maintained by hand, no S0 reset / "
                    "determinism guarantees; only the pixel+HID channel is unchanged).",
        live_url="https://www.yuque.com/",
        seed="live",
        brief=("This is the Yuque docs & knowledge-base web version; its login state is maintained by hand. "
               "This is a real online service whose content changes in real time. Explore and search read-only; "
               "do not create, edit, delete, or share documents, or change account, team, or permission settings."
               + _LIVE_SOFT_NETWORK_RULE),
    ),
    "youtube_web": AppSpec(
        app_id="youtube_web",
        kind="live",
        description="YouTube web (live target: login state maintained by hand, no S0 reset / "
                    "determinism guarantees; only the pixel+HID channel is unchanged).",
        live_url="https://www.youtube.com/",
        seed="live",
        brief=("This is the YouTube video-platform web version; its login state is maintained by hand. "
               "This is a real online service whose content changes in real time. Explore and search read-only; "
               "do not upload videos, comment, like, subscribe, or change playlists or account settings."
               + _LIVE_SOFT_NETWORK_RULE),
    ),
    "taobao_web": AppSpec(
        app_id="taobao_web",
        kind="live",
        description="Taobao web (live target: login state maintained by hand, no S0 reset / "
                    "determinism guarantees; only the pixel+HID channel is unchanged).",
        live_url="https://www.taobao.com/",
        seed="live",
        brief=("This is the Taobao e-commerce web version; its login state is maintained by hand. "
               "This is a real online service whose content changes in real time. Explore and search read-only; "
               "do not place orders, pay, review, favorite, add to cart, or change account settings."
               + _LIVE_SOFT_NETWORK_RULE),
    ),
    "zhihu_web": AppSpec(
        app_id="zhihu_web",
        kind="live",
        description="Zhihu web (live target: login state maintained by hand, no S0 reset / "
                    "determinism guarantees; only the pixel+HID channel is unchanged).",
        live_url="https://www.zhihu.com/follow",
        seed="live",
        brief=("This is the Zhihu Q&A community web version; its login state is maintained by hand. "
               "This is a real online service whose content changes in real time. Explore and search read-only; "
               "do not ask questions, answer, comment, like, favorite, or change account settings."
               + _LIVE_SOFT_NETWORK_RULE),
    ),
    "xiaohongshu_web": AppSpec(
        app_id="xiaohongshu_web",
        kind="live",
        description="Xiaohongshu web (live target: login state maintained by hand, no S0 reset / "
                    "determinism guarantees; only the pixel+HID channel is unchanged).",
        live_url="https://www.xiaohongshu.com/explore",
        seed="live",
        brief=("This is the Xiaohongshu content-community web version; its login state is maintained by hand. "
               "This is a real online service whose content changes in real time. Explore and search read-only; "
               "do not publish notes, comment, like, favorite, or change account settings."
               + _LIVE_SOFT_NETWORK_RULE),
    ),
    "weibo_web": AppSpec(
        app_id="weibo_web",
        kind="live",
        description="Weibo web (live target: login state maintained by hand, no S0 reset / "
                    "determinism guarantees; only the pixel+HID channel is unchanged).",
        live_url="https://weibo.com/",
        seed="live",
        brief=("This is the Weibo social web version; its login state is maintained by hand. "
               "This is a real online service whose content changes in real time. Explore and search read-only; "
               "do not post, comment, repost, like, or change account settings."
               + _LIVE_SOFT_NETWORK_RULE),
    ),
    "douban_web": AppSpec(
        app_id="douban_web",
        kind="live",
        description="Douban web (live target: login state maintained by hand, no S0 reset / "
                    "determinism guarantees; only the pixel+HID channel is unchanged).",
        live_url="https://www.douban.com/",
        seed="live",
        brief=("This is the Douban community web version; its login state is maintained by hand. "
               "This is a real online service whose content changes in real time. Explore and search read-only; "
               "do not post, comment, like, favorite, or change account settings."
               + _LIVE_SOFT_NETWORK_RULE),
    ),
    "dianping_web": AppSpec(
        app_id="dianping_web",
        kind="live",
        description="Dianping local-life web (live target: login state maintained by hand, no S0 reset / "
                    "determinism guarantees; only the pixel+HID channel is unchanged).",
        live_url="https://www.dianping.com/",
        seed="live",
        brief=("This is the Dianping local-life web version; its login state is maintained by hand. "
               "This is a real online service whose content changes in real time. Explore and search read-only; "
               "do not write reviews, upload photos, check in, or change account settings."
               + _LIVE_SOFT_NETWORK_RULE),
    ),
    "ctrip_web": AppSpec(
        app_id="ctrip_web",
        kind="live",
        description="Ctrip travel-booking web (live target: login state maintained by hand, no S0 reset / "
                    "determinism guarantees; only the pixel+HID channel is unchanged).",
        live_url="https://www.ctrip.com/",
        seed="live",
        brief=("This is the Ctrip travel-booking web version; its login state is maintained by hand. "
               "This is a real online service whose content changes in real time. Explore and search read-only; "
               "do not place orders, pay, book, or change account settings."
               + _LIVE_SOFT_NETWORK_RULE),
    ),
    "reddit_web": AppSpec(
        app_id="reddit_web",
        kind="live",
        description="Reddit web (live target: login state maintained by hand, no S0 reset / "
                    "determinism guarantees; only the pixel+HID channel is unchanged).",
        live_url="https://www.reddit.com/",
        seed="live",
        brief=("This is the Reddit community web version; its login state is maintained by hand. "
               "This is a real online service whose content changes in real time. Explore and search read-only; "
               "do not post, comment, vote, join communities, or change account settings."
               + _LIVE_SOFT_NETWORK_RULE),
    ),
    "quora_web": AppSpec(
        app_id="quora_web",
        kind="live",
        description="Quora web (live target: login state maintained by hand, no S0 reset / "
                    "determinism guarantees; only the pixel+HID channel is unchanged).",
        live_url="https://www.quora.com/",
        seed="live",
        brief=("This is the Quora Q&A web version; its login state is maintained by hand. "
               "This is a real online service whose content changes in real time. Explore and search read-only; "
               "do not ask questions, answer, comment, vote, or change account settings."
               + _LIVE_SOFT_NETWORK_RULE),
    ),
    "notion_web": AppSpec(
        app_id="notion_web",
        kind="live",
        description="Notion workspace web (live target: login state maintained by hand, no S0 reset / "
                    "determinism guarantees; only the pixel+HID channel is unchanged).",
        live_url="https://app.notion.com/",
        seed="live",
        brief=("This is the Notion workspace web version; its login state is maintained by hand. "
               "This is a real online service whose content changes in real time. Explore and search read-only; "
               "do not create, edit, delete, or share pages, or change workspace settings."
               + _LIVE_SOFT_NETWORK_RULE),
    ),
    "trello_web": AppSpec(
        app_id="trello_web",
        kind="live",
        description="Trello boards web (live target: login state maintained by hand, no S0 reset / "
                    "determinism guarantees; only the pixel+HID channel is unchanged).",
        live_url="https://trello.com/",
        seed="live",
        brief=("This is the Trello boards web version; its login state is maintained by hand. "
               "This is a real online service whose content changes in real time. Explore and search read-only; "
               "do not create, edit, delete, or archive cards, or change board settings."
               + _LIVE_SOFT_NETWORK_RULE),
    ),
    "todoist_web": AppSpec(
        app_id="todoist_web",
        kind="live",
        description="Todoist task-management web (live target: login state maintained by hand, no S0 reset / "
                    "determinism guarantees; only the pixel+HID channel is unchanged).",
        live_url="https://app.todoist.com/",
        seed="live",
        brief=("This is the Todoist task-management web version; its login state is maintained by hand. "
               "This is a real online service whose content changes in real time. Explore and search read-only; "
               "do not create, edit, complete, or delete tasks, or change project settings."
               + _LIVE_SOFT_NETWORK_RULE),
    ),
    "airtable_web": AppSpec(
        app_id="airtable_web",
        kind="live",
        description="Airtable spreadsheet web (live target: login state maintained by hand, no S0 reset / "
                    "determinism guarantees; only the pixel+HID channel is unchanged).",
        live_url="https://airtable.com/",
        seed="live",
        brief=("This is the Airtable spreadsheet web version; its login state is maintained by hand. "
               "This is a real online service whose content changes in real time. Explore and search read-only; "
               "do not create, edit, or delete records, or change table structure."
               + _LIVE_SOFT_NETWORK_RULE),
    ),
    "google_calendar_web": AppSpec(
        app_id="google_calendar_web",
        kind="live",
        description="Google Calendar web (live target: login state maintained by hand, no S0 reset / "
                    "determinism guarantees; only the pixel+HID channel is unchanged).",
        live_url="https://calendar.google.com/",
        seed="live",
        brief=("This is the Google Calendar web version; its login state is maintained by hand. "
               "This is a real online service whose content changes in real time. Explore and search read-only; "
               "do not create, edit, or delete events, or change calendar settings."
               + _LIVE_SOFT_NETWORK_RULE),
    ),
    "dropbox_web": AppSpec(
        app_id="dropbox_web",
        kind="live",
        description="Dropbox cloud-storage web (live target: login state maintained by hand, no S0 reset / "
                    "determinism guarantees; only the pixel+HID channel is unchanged).",
        live_url="https://www.dropbox.com/",
        seed="live",
        brief=("This is the Dropbox cloud-storage web version; its login state is maintained by hand. "
               "This is a real online service whose content changes in real time. Explore and search read-only; "
               "do not upload, edit, delete, or share files, or change account settings."
               + _LIVE_SOFT_NETWORK_RULE),
    ),
    "google_forms_web": AppSpec(
        app_id="google_forms_web",
        kind="live",
        description="Google Forms web (live target: login state maintained by hand, no S0 reset / "
                    "determinism guarantees; only the pixel+HID channel is unchanged).",
        live_url="https://forms.google.com/",
        seed="live",
        brief=("This is the Google Forms web version; its login state is maintained by hand. "
               "This is a real online service whose content changes in real time. Explore and search read-only; "
               "do not create, edit, or delete forms, or submit responses."
               + _LIVE_SOFT_NETWORK_RULE),
    ),
    "excalidraw_web": AppSpec(
        app_id="excalidraw_web",
        kind="live",
        description="Excalidraw whiteboard web (live target: login state maintained by hand, no S0 reset / "
                    "determinism guarantees; only the pixel+HID channel is unchanged).",
        live_url="https://excalidraw.com/",
        seed="live",
        brief=("This is the Excalidraw whiteboard web version; its login state is maintained by hand. "
               "This is a real online service whose content changes in real time. Explore and search read-only; "
               "do not save, export, or share the canvas, or change account settings."
               + _LIVE_SOFT_NETWORK_RULE),
    ),
    "diagrams_net_web": AppSpec(
        app_id="diagrams_net_web",
        kind="live",
        description="diagrams.net diagram-tool web (live target: login state maintained by hand, no S0 reset / "
                    "determinism guarantees; only the pixel+HID channel is unchanged).",
        live_url="https://app.diagrams.net/",
        seed="live",
        brief=("This is the diagrams.net diagram-tool web version; its login state is maintained by hand. "
               "This is a real online service whose content changes in real time. Explore and search read-only; "
               "do not save, export, or share diagrams, or change account settings."
               + _LIVE_SOFT_NETWORK_RULE),
    ),
    "spotify_web": AppSpec(
        app_id="spotify_web",
        kind="live",
        description="Spotify music web (live target: login state maintained by hand, no S0 reset / "
                    "determinism guarantees; only the pixel+HID channel is unchanged).",
        live_url="https://open.spotify.com/",
        seed="live",
        brief=("This is the Spotify music web version; its login state is maintained by hand. "
               "This is a real online service whose content changes in real time. Explore and search read-only; "
               "do not create playlists, follow, favorite, or change account settings."
               + _LIVE_SOFT_NETWORK_RULE),
    ),
    "desmos_web": AppSpec(
        app_id="desmos_web",
        kind="live",
        description="Desmos graphing-calculator web (live target: login state maintained by hand, no S0 reset / "
                    "determinism guarantees; only the pixel+HID channel is unchanged).",
        live_url="https://www.desmos.com/calculator/",
        seed="live",
        brief=("This is the Desmos graphing-calculator web version; its login state is maintained by hand. "
               "This is a real online service whose content changes in real time. Explore and search read-only; "
               "do not save or share graphs, or change account settings."
               + _LIVE_SOFT_NETWORK_RULE),
    ),
    "desmos_geometry_web": AppSpec(
        app_id="desmos_geometry_web",
        kind="live",
        description="Desmos Geometry web (live target: login state maintained by hand, no S0 reset / "
                    "determinism guarantees; only the pixel+HID channel is unchanged).",
        live_url="https://www.desmos.com/geometry",
        seed="live",
        brief=("This is the Desmos Geometry web version; its login state is maintained by hand. "
               "This is a real online service whose content changes in real time. Explore and search read-only; "
               "do not save or share constructions, or change account settings."
               + _LIVE_SOFT_NETWORK_RULE),
    ),
    "desmos_3d_web": AppSpec(
        app_id="desmos_3d_web",
        kind="live",
        description="Desmos 3D calculator web (live target: login state maintained by hand, no S0 reset / "
                    "determinism guarantees; only the pixel+HID channel is unchanged).",
        live_url="https://www.desmos.com/3d",
        seed="live",
        brief=("This is the Desmos 3D calculator web version; its login state is maintained by hand. "
               "This is a real online service whose content changes in real time. Explore and search read-only; "
               "do not save or share graphs, or change account settings."
               + _LIVE_SOFT_NETWORK_RULE),
    ),
    "google_maps_web": AppSpec(
        app_id="google_maps_web",
        kind="live",
        description="Google Maps web (live target: login state maintained by hand, no S0 reset / "
                    "determinism guarantees; only the pixel+HID channel is unchanged).",
        live_url="https://maps.google.com/",
        seed="live",
        brief=("This is the Google Maps web version; its login state is maintained by hand. "
               "This is a real online service whose content changes in real time. Explore and search read-only; "
               "do not edit maps, upload photos, save places, or change account settings."
               + _LIVE_SOFT_NETWORK_RULE),
    ),
    "kleki_web": AppSpec(
        app_id="kleki_web",
        kind="live",
        description="Kleki online-painting web (live target: login state maintained by hand, no S0 reset / "
                    "determinism guarantees; only the pixel+HID channel is unchanged).",
        live_url="https://kleki.com/",
        seed="live",
        brief=("This is the Kleki online-painting web version; its login state is maintained by hand. "
               "This is a real online service whose content changes in real time. Explore and search read-only; "
               "do not save, export, or share artwork, or change account settings."
               + _LIVE_SOFT_NETWORK_RULE),
    ),
    "jspaint_web": AppSpec(
        app_id="jspaint_web",
        kind="live",
        description="JS Paint online-painting web (live target: login state maintained by hand, no S0 reset / "
                    "determinism guarantees; only the pixel+HID channel is unchanged).",
        live_url="https://jspaint.app/",
        seed="live",
        brief=("This is the JS Paint online-painting web version; its login state is maintained by hand. "
               "This is a real online service whose content changes in real time. Explore and search read-only; "
               "do not save, export, or share artwork, or change account settings."
               + _LIVE_SOFT_NETWORK_RULE),
    ),
    "squoosh_web": AppSpec(
        app_id="squoosh_web",
        kind="live",
        description="Squoosh image-compressor web (live target: login state maintained by hand, no S0 reset / "
                    "determinism guarantees; only the pixel+HID channel is unchanged).",
        live_url="https://squoosh.app/",
        seed="live",
        brief=("This is the Squoosh image-compressor web version; its login state is maintained by hand. "
               "This is a real online service whose content changes in real time. Explore and search read-only; "
               "do not process images containing personal privacy."
               + _LIVE_SOFT_NETWORK_RULE),
    ),
}


_LIVE_BRIEF = (
    "This is a real online website; you visit its web version through the browser. Its content changes in real time. "
    "Explore read-only (browse, search, view); do not perform any operation that changes server-side or account state "
    "(posting, commenting, liking, following, purchasing, deleting, changing settings, etc.). "
    + _LIVE_SOFT_NETWORK_RULE)


def make_live_spec_for_url(url: str) -> AppSpec:
    """Ad-hoc live target from a bare URL (no registry entry needed).

    The derived app_id is a stable per-host slug (live_www_example_com), so
    the browser profile — and any login state captured for the site —
    persists across sessions. The URL is only the browser's entry point;
    redirects and cross-domain resources use normal public networking.
    """
    raw = url.strip()
    if "://" not in raw:
        raw = f"https://{raw}"
    p = urlparse(raw)
    if p.scheme not in ("http", "https") or not p.netloc or "@" in p.netloc:
        raise ValueError(f"invalid live_url: {url!r}")
    host = p.netloc.split(":")[0].lower()
    if not re.fullmatch(r"[a-z0-9.-]+", host) or "." not in host or \
            host in ("localhost", "127.0.0.1") or host.endswith(".internal"):
        raise ValueError(f"live_url must be a real remote site: {url!r}")
    slug = re.sub(r"[^a-z0-9]+", "_", host).strip("_")
    entry = raw if (p.path not in ("", "/") or p.query) else \
        f"{p.scheme}://{host}/"
    return AppSpec(
        app_id=f"live_{slug}",
        kind="live",
        description=f"ad-hoc live target: {entry}",
        live_url=entry,
        seed="live",
        brief=_LIVE_BRIEF,
    )


def list_apps(platform: str = "web") -> list[dict]:
    items = [{"app_id": s.app_id, "description": s.description,
              "platform": s.platform, "kind": s.kind}
             for s in _REGISTRY.values()]
    if platform == "android":
        try:
            from ..android.targets import list_android_targets
            return list_android_targets()
        except Exception:
            return []
    if platform not in ("web", "all"):
        raise ValueError("platform must be web, android or all")
    if platform == "all":
        try:
            from ..android.targets import list_android_targets
            items.extend(list_android_targets())
        except Exception:
            pass
    return items


def get_app(app_id: str) -> AppSpec:
    if app_id in _REGISTRY:
        return _REGISTRY[app_id]
    try:
        from ..android.targets import get_android_target
        return get_android_target(app_id)  # type: ignore[return-value]
    except KeyError:
        available = [item["app_id"] for item in list_apps("all")]
        raise KeyError(f"unknown app_id: {app_id!r}; available: {sorted(available)}")


def new_gateway_secret() -> str:
    return secrets.token_hex(24)
