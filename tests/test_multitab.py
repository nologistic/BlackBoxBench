"""Multi-tab / multi-page navigation tracking.

Clicking a target=_blank link opens a new tab: the runtime must FOLLOW it —
screenshots and input move to the newest content tab — so exploration
continues on the new page instead of staring at the old one. Same-tab
history navigation is covered by the single-key BrowserBack.
"""
from __future__ import annotations

import io
import shutil

import pytest
from PIL import Image

from benchmark.recorder import diff as imgdiff
from benchmark.runtime.local_chromium import LocalChromiumRuntime

MARKER = (90, 90, 410, 190)      # #detail-marker region
NEWTAB_AT = (220, 481)           # #newtab-link (left:100 top:470, 22px)
BACKLINK_AT = (220, 262)         # #back-link on /detail (left:100 top:250)

_follow = LocalChromiumRuntime._target_to_follow


def _frame(client, sid, fid) -> Image.Image:
    r = client.get(f"/api/sessions/{sid}/frames/{fid}.png")
    assert r.status_code == 200
    return Image.open(io.BytesIO(r.content)).convert("RGB")


class TestTargetPick:
    BLANK = {"id": "t0", "url": "about:blank", "webSocketDebuggerUrl": "ws0"}
    HOME = {"id": "t1", "url": "http://reference-app.internal:1/",
            "webSocketDebuggerUrl": "ws1"}
    DETAIL = {"id": "t2", "url": "http://reference-app.internal:1/detail",
              "webSocketDebuggerUrl": "ws2"}

    def test_startup_blank(self):
        assert _follow([self.BLANK], set(), None) == self.BLANK

    def test_stay_on_current_when_nothing_new(self):
        assert _follow([self.HOME], {"t1"}, "t1") == self.HOME

    def test_follow_newly_opened_tab(self):
        # list order must not matter (DevTools ordering varies)
        assert _follow([self.DETAIL, self.HOME], {"t1"}, "t1") == self.DETAIL
        assert _follow([self.HOME, self.DETAIL], {"t1"}, "t1") == self.DETAIL

    def test_current_vanished_falls_back(self):
        assert _follow([self.HOME], {"t1"}, "tGONE") == self.HOME

    def test_never_devtools_or_blank_when_content_exists(self):
        dt = {"id": "tD", "url": "devtools://x", "webSocketDebuggerUrl": "wsD"}
        assert _follow([dt, self.HOME], {"t1"}, "t1") == self.HOME

    def test_no_targets(self):
        assert _follow([], set(), None) is None


class TestTabControl:
    @staticmethod
    def _t(tid, url):
        return {"id": tid, "url": url, "type": "page",
                "webSocketDebuggerUrl": f"ws://x/{tid}"}

    def _fake_rt(self, targets):
        import types
        rt = object.__new__(LocalChromiumRuntime)
        rt._ws_url = "ws://x/t1"
        rt._current_target_id = "t1"
        rt._seen_target_ids = {"t1"}
        rt._tab_order = []
        rt._last_target_poll = 0.0
        rt._targets = list(targets)
        rt._page_targets = lambda: list(rt._targets)
        log = {"attached": [], "closed": []}

        def _attach(self, ws):
            tid = ws.rsplit("/", 1)[-1]
            log["attached"].append(tid)
            self._ws_url = ws
            self._current_target_id = tid
            self._seen_target_ids.add(tid)

        def _cdp_call(self, method, params=None, **kw):
            if method == "Page.close":
                log["closed"].append(self._current_target_id)
                rt._targets = [t for t in rt._targets
                               if t["id"] != self._current_target_id]
            return {}

        rt._attach = types.MethodType(_attach, rt)
        rt._cdp_call = types.MethodType(_cdp_call, rt)
        return rt, log

    def test_list_and_switch(self):
        rt, log = self._fake_rt([
            self._t("t1", "http://a/"), self._t("t2", "http://a/detail")])
        assert rt.list_tabs() == {"count": 2, "active": 0}
        rt.switch_tab(1)
        assert log["attached"] == ["t2"]
        assert rt.list_tabs() == {"count": 2, "active": 1}
        rt.switch_tab(0)
        assert rt.list_tabs()["active"] == 0
        with pytest.raises(ValueError, match="invalid_tab_index"):
            rt.switch_tab(5)

    def test_tab_order_stable_and_pruned(self):
        rt, _ = self._fake_rt([self._t("t1", "http://a/"),
                               self._t("t2", "http://a/d")])
        assert rt.list_tabs() == {"count": 2, "active": 0}
        # DevTools list order shuffles → indices stay first-seen ordered
        rt._targets.reverse()
        assert rt.list_tabs() == {"count": 2, "active": 0}
        # t1 closes externally → pruned, t2 takes index 0
        rt._targets = [t for t in rt._targets if t["id"] != "t1"]
        assert rt.list_tabs() == {"count": 1, "active": -1}

    def test_close_tab_falls_back_and_guards_last(self):
        rt, log = self._fake_rt([self._t("t1", "http://a/"),
                                 self._t("t2", "http://a/d")])
        rt.switch_tab(1)                      # current = t2
        rt.close_tab()                        # close t2 → fall back to t1
        assert log["closed"] == ["t2"]
        assert log["attached"][-1] == "t1"
        assert rt.list_tabs() == {"count": 1, "active": 0}
        with pytest.raises(ValueError, match="cannot_close_last_tab"):
            rt.close_tab()


class TestFollowNewTab:
    def test_blank_tab_followed_once_navigated(self):
        """Regression: a tab first seen as about:blank must still be followed
        after it finishes navigating (it must not be marked 'seen' early)."""
        import types
        rt = object.__new__(LocalChromiumRuntime)
        rt._ws_url = "ws1"
        rt._current_target_id = "t1"
        rt._seen_target_ids = {"t1"}
        rt._last_target_poll = 0.0
        attached = []

        def _fake_attach(self, ws):
            attached.append(ws)
            self._ws_url = ws
            self._current_target_id = ws.rsplit("/", 1)[-1]
            self._seen_target_ids.add(self._current_target_id)
        rt._attach = types.MethodType(_fake_attach, rt)

        home = dict(TestTargetPick.HOME)
        blank_new = {"id": "t2", "url": "about:blank",
                     "webSocketDebuggerUrl": "ws2"}
        detail_new = dict(TestTargetPick.DETAIL)  # id t2 → ws2
        polls = [[home, blank_new], [home, detail_new]]
        rt._page_targets = lambda: polls.pop(0)

        rt._follow_latest()
        assert attached == []          # blank new tab: not followed yet
        rt._last_target_poll = 0.0     # bypass throttle
        rt._follow_latest()
        assert attached == ["ws2"]     # followed as soon as it has content

    def _region_hash(self, client, sid) -> int:
        obs = client.post(f"/agent/{sid}/observe", json={}).json()
        return imgdiff.phash(_frame(client, sid, obs["frame_id"]).crop(MARKER))

    def _await_region(self, client, sid, home: int, want_different: bool,
                      timeout: float = 15.0) -> None:
        """Poll observe until the marker region matches expectation. The tab
        switch is async: the new tab may still be about:blank at the first
        post-action frame, and the runtime follows as soon as it has content."""
        import time
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            diff = imgdiff.hamming(self._region_hash(client, sid), home)
            if (diff > 6) == want_different:
                return
            time.sleep(1.0)
        raise AssertionError(
            f"region never reached expected state (want_different="
            f"{want_different}, last diff={diff})")

    def test_follow_new_tab_and_history_back(self, client, manager):
        r = client.post("/api/sessions", json={
            "app_id": "miniapp",
            "budget": {"max_actions": 40, "max_duration_s": 300,
                       "max_observations": 120}})
        assert r.status_code == 200, r.text
        sid = r.json()["session_id"]
        try:
            home = self._region_hash(client, sid)

            # click the target=_blank link → screenshot must come from the
            # NEW tab (orange detail marker now fills the region)
            r = client.post(f"/agent/{sid}/action",
                            json={"type": "click", "x": NEWTAB_AT[0], "y": NEWTAB_AT[1]})
            assert r.status_code == 200
            self._await_region(client, sid, home, want_different=True)

            # input followed too: clicking the same-tab back link lands there
            r = client.post(f"/agent/{sid}/action",
                            json={"type": "click", "x": BACKLINK_AT[0], "y": BACKLINK_AT[1]})
            assert r.status_code == 200
            self._await_region(client, sid, home, want_different=False)

            # same-tab history: BrowserBack returns to the detail page
            r = client.post(f"/agent/{sid}/action",
                            json={"type": "key_press", "key": "BrowserBack"})
            assert r.status_code == 200
            self._await_region(client, sid, home, want_different=True)
        finally:
            client.post(f"/api/sessions/{sid}/close")
            shutil.rmtree(manager.runs_dir / sid, ignore_errors=True)

    def test_agent_tab_control(self, client, manager):
        """switch_tab / close_tab + tabs info in receipts."""
        r = client.post("/api/sessions", json={
            "app_id": "miniapp",
            "budget": {"max_actions": 40, "max_duration_s": 300,
                       "max_observations": 120}})
        assert r.status_code == 200, r.text
        sid = r.json()["session_id"]
        try:
            home = self._region_hash(client, sid)

            # open the _blank link → auto-followed; tabs must show 2/active 1
            r = client.post(f"/agent/{sid}/action",
                            json={"type": "click", "x": NEWTAB_AT[0], "y": NEWTAB_AT[1]})
            assert r.status_code == 200
            self._await_region(client, sid, home, want_different=True)
            r = client.post(f"/agent/{sid}/action",
                            json={"type": "wait", "ms": 50})
            assert r.json()["tabs"] == {"count": 2, "active": 1}

            # agent decides to switch back to the first tab
            r = client.post(f"/agent/{sid}/action",
                            json={"type": "switch_tab", "tab_index": 0})
            assert r.status_code == 200
            assert r.json()["tabs"]["active"] == 0
            self._await_region(client, sid, home, want_different=False)

            # and forward to the new tab again
            r = client.post(f"/agent/{sid}/action",
                            json={"type": "switch_tab", "tab_index": 1})
            assert r.status_code == 200
            self._await_region(client, sid, home, want_different=True)

            # close the current (detail) tab → back to the first one
            r = client.post(f"/agent/{sid}/action", json={"type": "close_tab"})
            assert r.status_code == 200
            assert r.json()["tabs"] == {"count": 1, "active": 0}
            self._await_region(client, sid, home, want_different=False)

            # the last tab cannot be closed; the session survives the 400
            r = client.post(f"/agent/{sid}/action", json={"type": "close_tab"})
            assert r.status_code == 400
            r = client.post(f"/agent/{sid}/observe", json={})
            assert r.status_code == 200
            assert r.json()["tabs"]["count"] == 1
        finally:
            client.post(f"/api/sessions/{sid}/close")
            shutil.rmtree(manager.runs_dir / sid, ignore_errors=True)
