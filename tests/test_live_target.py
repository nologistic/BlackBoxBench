"""Live-target (douyin_web) plumbing tests.

No real browser and no network: the pixel precheck runs on synthetic frames,
the runtime dispatch only constructs objects, and the busy guard never gets
past the registry lookup. End-to-end live runs are operator-verified.
"""
from __future__ import annotations

import io
import types

import pytest
from PIL import Image

from benchmark.orchestrator import apps, prechecks
from benchmark.orchestrator.apps import AppSpec, get_app
from benchmark.runtime import live_chromium
from benchmark.runtime.base import Runtime, RuntimeInfo


# ------------------------------------------------------------------ registry

class TestRegistry:
    def test_douyin_web_spec(self):
        spec = get_app("douyin_web")
        assert spec.kind == "live"
        assert spec.live_url.startswith("https://")
        assert spec.precheck in prechecks.PRECHECKS
        assert spec.seed == "live"

    def test_yuque_web_spec_uses_soft_network_constraint(self):
        spec = get_app("yuque_web")
        assert spec.kind == "live"
        assert spec.live_url == "https://www.yuque.com/"
        assert not spec.precheck
        assert "不是域名或导航白名单" in spec.brief
        assert "不得" in spec.brief and "原始实现" in spec.brief

    def test_live_spec_has_no_local_launch(self):
        with pytest.raises(RuntimeError):
            get_app("douyin_web").launch_command(1234, "x")

    def test_local_default_kind_unchanged(self):
        spec = get_app("ecommerce_demo")
        assert spec.kind == "local"
        assert spec.module  # still launchable


# ------------------------------------------------------------------ precheck

def _png(fill=(20, 20, 30), avatar=None) -> bytes:
    """Synthetic 1440x900 frame; `avatar` paints the header check zone."""
    img = Image.new("RGB", (1440, 900), fill)
    if avatar is not None:
        img.paste(Image.new("RGB", (47, 48), avatar),
                  (prechecks._AVATAR_ZONES["douyin_web"]["box"][0], prechecks._AVATAR_ZONES["douyin_web"]["box"][1]))
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    return buf.getvalue()


class _ShotRuntime(Runtime):
    """Headless fake: only screenshot() is exercised by the precheck."""

    def __init__(self, png: bytes):
        self._png = png

    def start(self): pass
    def stop(self): pass
    def reset(self): pass
    def screenshot(self) -> bytes: return self._png
    def info(self) -> RuntimeInfo: return RuntimeInfo(1440, 900, 1.0)
    def mouse_move(self, x, y): pass
    def mouse_down(self, x, y, button="left"): pass
    def mouse_up(self, x, y, button="left"): pass
    def click(self, x, y, button="left", count=1): pass
    def scroll(self, dx, dy): pass
    def type_text(self, text): pass
    def key(self, key, kind="press"): pass
    def health(self) -> bool: return True


class TestPrecheck:
    @pytest.fixture
    def state_dir(self, tmp_path, monkeypatch):
        monkeypatch.setattr(prechecks, "live_state_dir", lambda app_id: tmp_path)
        return tmp_path

    def test_missing_reference_fails(self, state_dir):
        with pytest.raises(prechecks.PreflightError, match="参考图"):
            prechecks.avatar_logged_in(_ShotRuntime(_png()), "douyin_web")

    def test_matching_avatar_passes(self, state_dir):
        logged_in = _png(avatar=(200, 120, 60))
        ref = Image.open(io.BytesIO(logged_in)).crop(prechecks._AVATAR_ZONES["douyin_web"]["box"])
        ref.save(state_dir / "header_ref.png")
        prechecks.avatar_logged_in(_ShotRuntime(logged_in), "douyin_web")

    def test_different_header_fails(self, state_dir):
        logged_in = _png(avatar=(200, 120, 60))
        ref = Image.open(io.BytesIO(logged_in)).crop(prechecks._AVATAR_ZONES["douyin_web"]["box"])
        ref.save(state_dir / "header_ref.png")
        logged_out = _png(avatar=(254, 44, 85))  # red 登录 button zone
        with pytest.raises(prechecks.PreflightError, match="不是登录状态"):
            prechecks.avatar_logged_in(_ShotRuntime(logged_out), "douyin_web")


# ------------------------------------------------------------------ dispatch

class TestDispatch:
    def test_live_spec_builds_live_runtime(self, tmp_path, monkeypatch):
        monkeypatch.setattr(live_chromium, "ensure_profile",
                            lambda app_id, require_golden=False:
                            tmp_path / "profile")
        from benchmark.orchestrator.manager import SessionManager
        mgr = SessionManager(tmp_path / "runs")
        rt = mgr._make_runtime(get_app("douyin_web"), tmp_path / "d",
                               tmp_path / "s")
        assert isinstance(rt, live_chromium.LiveChromiumRuntime)
        assert rt._profile_dir == tmp_path / "profile"
        # tab-follow tracking fields must exist (regression: missing attrs
        # once made _await_debugger swallow AttributeError → startup timeout)
        for attr in ("_ws_url", "_current_target_id", "_seen_target_ids",
                     "_last_target_poll"):
            assert hasattr(rt, attr), attr

    def test_live_runtime_has_no_dns_or_url_allowlist(self):
        import inspect
        source = inspect.getsource(
            live_chromium.LiveChromiumRuntime._start_browser)
        assert "host-resolver-rules" not in source
        assert "~NOTFOUND" not in source

    def test_live_busy_guard(self, tmp_path, monkeypatch):
        # hermetic: never touch the real operator profile in tests
        monkeypatch.setattr(
            live_chromium, "ensure_profile",
            lambda app_id, require_golden=False:
                (_ for _ in ()).throw(RuntimeError("no_profile")))
        from benchmark.orchestrator.manager import SessionManager
        mgr = SessionManager(tmp_path / "runs")
        fake = types.SimpleNamespace(
            spec=types.SimpleNamespace(app_id="douyin_web"), status="running",
            id="sess_fake")
        mgr._sessions["sess_fake"] = fake
        with pytest.raises(RuntimeError, match="busy"):
            mgr.create("douyin_web")
        # a closed session with the same app_id must NOT block new ones
        fake.status = "closed"
        with pytest.raises(RuntimeError, match="no_profile"):
            mgr.create("douyin_web")

    def test_docker_shared_reference_busy_guard(self, tmp_path, monkeypatch):
        """docker-mode sessions share ONE reference browser: a second
        concurrent session must be refused instead of contaminating it."""
        from benchmark.orchestrator.manager import SessionManager
        monkeypatch.setenv("BBB_RUNTIME", "docker")
        mgr = SessionManager(tmp_path / "runs")
        running = types.SimpleNamespace(
            spec=types.SimpleNamespace(app_id="ecommerce_demo"),
            status="running", id="sess_docker_one",
            runtime=types.SimpleNamespace(shared_environment=True))
        mgr._sessions["sess_docker_one"] = running
        with pytest.raises(RuntimeError, match="docker reference environment busy"):
            mgr.create("ecommerce_demo")
        # a finished docker session frees the shared environment, and
        # local-mode (BBB_RUNTIME unset) ignores the shared guard entirely:
        # execution must get PAST the guard into runtime construction.
        running.status = "closed"
        monkeypatch.delenv("BBB_RUNTIME")

        def _sentinel(spec, data_dir, sdir):
            raise RuntimeError("past the guard")
        monkeypatch.setattr(mgr, "_make_runtime", _sentinel)
        with pytest.raises(RuntimeError, match="past the guard"):
            mgr.create("ecommerce_demo")


# ------------------------------------------------------------------ ad-hoc URL targets

class TestAdhocLiveSpec:
    def test_url_parsing(self):
        from benchmark.orchestrator.apps import make_live_spec_for_url as mk
        s = mk("https://www.example.com/")
        assert s.app_id == "live_www_example_com"
        assert s.kind == "live" and s.live_url == "https://www.example.com/"
        assert not s.precheck and s.brief  # generic read-only brief attached
        s2 = mk("example.com/path?x=1")          # schemeless + path preserved
        assert s2.live_url == "https://example.com/path?x=1"
        assert s2.app_id == "live_example_com"

    def test_url_validation(self):
        from benchmark.orchestrator.apps import make_live_spec_for_url as mk
        for bad in ("not a url", "http://localhost/", "ftp://x.com",
                    "http://127.0.0.1:8000/", "http://reference-app.internal/"):
            with pytest.raises(ValueError, match="live_url|invalid"):
                mk(bad)

    def test_live_specs_have_no_network_allowlist(self):
        from benchmark.orchestrator.apps import make_live_spec_for_url as mk
        for spec in (get_app("douyin_web"), get_app("yuque_web"),
                     mk("https://www.example.com/")):
            assert not hasattr(spec, "allowed_hosts")
            assert "不是域名或导航白名单" in spec.brief

    def test_live_url_session_validation(self, client):
        # invalid URLs are rejected BEFORE any browser would launch
        r = client.post("/api/sessions", json={"live_url": "http://localhost/"})
        assert r.status_code == 400
        r = client.post("/api/sessions", json={})
        assert r.status_code == 400


# ------------------------------------------------------------------ live reset

class TestLiveReset:
    def test_live_reset_is_browser_restart_only(self, tmp_path):
        """Live reset: runtime.reset(), no appdata wipe, no stop/start."""
        from benchmark.orchestrator.session import Budget, Session
        calls = []

        class _Rt(_ShotRuntime):
            def start(self): calls.append("start")
            def stop(self): calls.append("stop")
            def reset(self): calls.append("reset")

        sess = Session(session_id="sess_t", spec=get_app("douyin_web"),
                       runtime=_Rt(_png()), session_dir=tmp_path / "sess",
                       budget=Budget(10, 60, 10))
        sess.reset()
        assert calls == ["reset"], calls
        assert sess.status == "running"

    def test_f12_blocked_on_live_target(self, tmp_path):
        from benchmark.orchestrator.session import Budget, Session
        from benchmark.topology import models as m
        sess = Session(session_id="sess_t2", spec=get_app("douyin_web"),
                       runtime=_ShotRuntime(_png()), session_dir=tmp_path / "s2",
                       budget=Budget(10, 60, 10))
        with pytest.raises(ValueError, match="key_not_allowed"):
            sess.execute(m.Action(type=m.ActionType.KEY_PRESS, key="F12"))
        assert sess.status == "running"  # rejected without failing the session

    def test_failed_session_stops_runtime(self, tmp_path):
        """A runtime error marks the session failed AND releases the browser
        (live profile singleton lock) immediately, not at close()/exit."""
        from benchmark.orchestrator.session import Budget, Session
        from benchmark.topology import models as m

        class _BoomRt(_ShotRuntime):
            stopped = False

            def click(self, x, y, button="left", count=1):
                raise RuntimeError("boom")

            def stop(self):
                self.stopped = True

        rt = _BoomRt(_png())
        sess = Session(session_id="sess_t3", spec=get_app("douyin_web"),
                       runtime=rt, session_dir=tmp_path / "s3",
                       budget=Budget(10, 60, 10))
        with pytest.raises(RuntimeError, match="boom"):
            sess.execute(m.Action(type=m.ActionType.CLICK, x=10, y=10))
        assert sess.status == "failed"
        assert rt.stopped


# ------------------------------------------------------------------ budget freeze

class TestBudgetFreeze:
    """Terminal sessions must stop ticking. Before the freeze, status_dict()
    kept computing elapsed/seconds_remaining from time.monotonic(), so the
    operator status showed closed sessions with ever-growing Elapsed and budgets
    draining to 0."""

    def test_close_freezes_elapsed_and_budget(self, tmp_path):
        import time
        from benchmark.orchestrator.session import Budget, Session
        sess = Session(session_id="sess_f1", spec=get_app("ecommerce_demo"),
                       runtime=_ShotRuntime(_png()), session_dir=tmp_path / "f1",
                       budget=Budget(10, 600, 10))
        sess.close()
        first = sess.status_dict()
        time.sleep(1.1)  # int-second granularity: a live clock would tick
        second = sess.status_dict()
        assert first["elapsed_s"] == second["elapsed_s"]
        assert (first["budget"]["seconds_remaining"]
                == second["budget"]["seconds_remaining"])

    def test_finalize_freezes_elapsed(self, tmp_path):
        import time
        from benchmark.orchestrator.session import Budget, Session
        sess = Session(session_id="sess_f2", spec=get_app("ecommerce_demo"),
                       runtime=_ShotRuntime(_png()), session_dir=tmp_path / "f2",
                       budget=Budget(10, 600, 10))
        sess.finalize()
        assert sess.status == "closed"
        first = sess.status_dict()
        time.sleep(1.1)
        assert sess.status_dict()["elapsed_s"] == first["elapsed_s"]

    def test_running_session_still_ticks(self, tmp_path):
        import time
        from benchmark.orchestrator.session import Budget, Session
        sess = Session(session_id="sess_f3", spec=get_app("ecommerce_demo"),
                       runtime=_ShotRuntime(_png()), session_dir=tmp_path / "f3",
                       budget=Budget(10, 600, 10))
        first = sess.status_dict()["elapsed_s"]
        time.sleep(1.1)
        assert sess.status_dict()["elapsed_s"] > first
