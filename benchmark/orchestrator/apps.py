"""Reference app registry.

An AppSpec describes how to launch a black-box reference app for a session.
Adding a new app means registering a spec here (see docs/adding_reference_app.md).
Ground-truth files live with the app and are NEVER read by the controller's
agent-facing paths — they exist only for future evaluator tooling.

Two kinds of targets:
- local: seeded app subprocess behind the loopback gateway (full isolation,
  deterministic reset to S0). This is the benchmark-grade mode.
- live:  a real website (e.g. douyin_web) driven through a headed persistent
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
    "起始 URL 只是目标入口,不是域名或导航白名单;允许网站正常重定向、跨域加载"
    "以及站内类人导航。不得把网络能力用于主动访问搜索引擎、代码托管站、网页"
    "归档或其他站点来查找目标的原始实现,也不得直接请求、下载或读取目标页面"
    "源码、接口响应等语义内容。")


_REGISTRY: dict[str, AppSpec] = {
    "ecommerce_demo": AppSpec(
        app_id="ecommerce_demo",
        description="Nimbus Market — seeded ecommerce demo (auth, catalog, cart, "
                    "coupon, checkout, orders, persistence).",
        module="sample_apps.ecommerce_demo.app",
        brief=("这是一个电商购物应用。你拥有一个测试账号: 用户名 alice, "
               "密码 alice123。登录它不是必须的——你可以先匿名探索,在需要时"
               "(例如遇到登录门槛)再使用它。"),
    ),
    # Live target: the real Douyin web app. Login state is maintained by a
    # human operator (scripts/live_login.py --app douyin_web --capture); every
    # session runs a pixel precheck and refuses to start logged-out.
    "douyin_web": AppSpec(
        app_id="douyin_web",
        kind="live",
        description="抖音网页版 (live target: 登录态人工维护, 无 S0 reset / "
                    "确定性保证, 仅像素+HID 通道不变)。",
        live_url="https://www.douyin.com/",
        precheck="avatar_logged_in",
        seed="live",
        brief=("这是一个短视频内容平台的网页版,你当前处于已登录状态。"
               "它可以搜索、浏览视频、直播等。注意: 这是一个真实线上应用,"
               "内容会实时变化,请不要执行发帖、评论、关注、点赞、私信等"
               "任何会修改账号公开状态的操作,只读探索和站内搜索是允许的。"
               + _LIVE_SOFT_NETWORK_RULE),
    ),
    # Live target: bilibili web. Same operator-maintained login model as
    # douyin_web (scripts/live_login.py --app bilibili_web --capture).
    "bilibili_web": AppSpec(
        app_id="bilibili_web",
        kind="live",
        description="哔哩哔哩网页版 (live target: 登录态人工维护, 无 S0 reset / "
                    "确定性保证, 仅像素+HID 通道不变)。",
        live_url="https://www.bilibili.com/",
        precheck="avatar_logged_in",
        seed="live",
        brief=("这是一个视频内容平台的网页版,你当前处于已登录状态。"
               "它可以搜索、浏览视频、番剧、直播等。注意: 这是一个真实线上应用,"
               "内容会实时变化,请不要执行发帖、评论、关注、点赞、投币、收藏、"
               "私信等任何会修改账号公开状态的操作,只读探索和站内搜索是允许的。"
               + _LIVE_SOFT_NETWORK_RULE),
    ),
    "yuque_web": AppSpec(
        app_id="yuque_web",
        kind="live",
        description="语雀网页版 (live target: 登录态人工维护, 无 S0 reset / "
                    "确定性保证, 仅像素+HID 通道不变)。",
        live_url="https://www.yuque.com/",
        seed="live",
        brief=("这是语雀文档与知识库网页版,登录态由人工预先维护。"
               "这是一个真实线上应用,内容会实时变化。请只读探索和搜索,"
               "不要新建、编辑、删除、分享文档或修改账号、团队与权限设置。"
               + _LIVE_SOFT_NETWORK_RULE),
    ),
}


_LIVE_BRIEF = (
    "这是一个真实线上网站,你通过浏览器访问它的网页版。内容会实时变化。"
    "请只读探索(浏览、搜索、查看),不要执行任何会修改服务端或账号状态的"
    "操作(发帖、评论、点赞、关注、购买、删除、设置变更等)。"
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
