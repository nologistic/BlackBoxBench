"""Formal regression tests for agents.our_method (V0.2.4).

Covers the reviewed fixes that have no baseline-test coverage:
input path whitelisting, the asset-utilization gates (pre-write and
finish), handoff-bound read counters, /input listing hygiene, handoff
INDEX generation, action-audited persistence probes, and evidence-verified
write-flow acceptance.
"""
from __future__ import annotations

import json
import shutil
import sys
from pathlib import Path

import pytest

PROJECT_ROOT = Path(__file__).resolve().parent.parent
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

import agents.our_method.mcp_server as m  # noqa: E402
import agents.our_method.reproduction_review as rr  # noqa: E402


# ---------------------------------------------------------------- helpers


class FakeReproduction:
    def __init__(self, artifact_dir, topology_path, handoff_id="h1"):
        self.artifact_dir = artifact_dir
        self.topology_path = topology_path
        self.handoff_id = handoff_id


@pytest.fixture()
def bundle(tmp_path: Path) -> Path:
    """A minimal handoff bundle: 3 screenshots + INDEX + topology md."""
    shots = bundle = tmp_path / "bundle"
    (shots / "screenshots").mkdir(parents=True)
    for i in range(3):
        (shots / "screenshots" / f"frame_{i:06d}.png").write_bytes(b"png")
    (shots / "INDEX.md").write_text("# idx", encoding="utf-8")
    (shots / "functional_topology.md").write_text("topo", encoding="utf-8")
    return bundle


@pytest.fixture()
def topology(tmp_path: Path) -> Path:
    path = tmp_path / "functional_topology.json"
    path.write_text(json.dumps({
        "nodes": [
            {"type": "STATE", "name": "s1", "visual_evidence": [1]},
            {"type": "FEATURE", "name": "f1",
             "evidence": [{"step": 1, "before_frame": 0, "after_frame": 2}]},
        ],
        "edges": [],
    }), encoding="utf-8")
    return path


@pytest.fixture(autouse=True)
def clean_gate_state(monkeypatch):
    monkeypatch.delenv("BBB_OUR_MIN_FRAME_READS", raising=False)
    monkeypatch.delenv("BBB_OUR_REQUIRE_ASSET_READS", raising=False)
    m._reset_asset_reads()
    m._reproduction = None
    m._finalized_source = ""
    m._finalize_degraded = False
    yield
    m._reset_asset_reads()
    m._reproduction = None
    m._finalized_source = ""
    m._finalize_degraded = False


# ---------------------------------------------------- input path whitelist


def test_input_read_resolves_whitelisted_roots(bundle, topology):
    rep = FakeReproduction(bundle, topology)
    assert m._resolve_input_host(
        "/exploration/screenshots/frame_000000.png", rep).is_file()
    assert m._resolve_input_host(
        "/exploration/INDEX.md", rep).is_file()
    assert m._resolve_input_host(
        "/input/functional_topology.json", rep).is_file()


@pytest.mark.parametrize("bad", [
    "/workspace/index.html",
    "/exploration",
    "/exploration/../../etc/passwd",
    "/input/other.json",
    "/input/functional_topology.json/../session.json",
    "relative.png",
    "",
])
def test_input_read_rejects_everything_else(bundle, topology, bad):
    rep = FakeReproduction(bundle, topology)
    with pytest.raises(ValueError):
        m._resolve_input_host(bad, rep)


def test_input_list_input_root_shows_only_topology(bundle, topology):
    rep = FakeReproduction(bundle, topology)
    m._reproduction = rep
    try:
        result = m._t_input_list({"path": "/input"})
        payload = json.loads(result["content"][0]["text"])
        names = [entry["name"] for entry in payload["entries"]]
        assert names == ["functional_topology.json"]
    finally:
        m._reproduction = None


# ------------------------------------------------------- asset gate logic


def test_gate_requires_handoff_match(bundle, topology):
    rep = FakeReproduction(bundle, topology, handoff_id="h1")
    m._reproduction = rep
    m._reset_asset_reads("h-other", available_frames=3)
    m._note_asset_read("/exploration/screenshots/frame_000000.png")
    m._note_asset_read("/input/functional_topology.json")
    err = m._asset_gate_error()
    assert err is not None and "0/" in err


def test_required_frames_clamped_by_available(bundle, topology):
    rep = FakeReproduction(bundle, topology)
    m._reproduction = rep
    m._reset_asset_reads(rep.handoff_id, available_frames=3)
    for i in range(3):
        m._note_asset_read(f"/exploration/screenshots/frame_{i:06d}.png")
    m._note_asset_read("/input/functional_topology.json")
    assert m._asset_gate_error() is None


def test_prewrite_gate_blocks_first_write(bundle, topology):
    rep = FakeReproduction(bundle, topology)
    m._reproduction = rep
    m._reset_asset_reads(rep.handoff_id, available_frames=10)
    err = m._prewrite_gate_error()
    assert err is not None and "topology" in err
    m._note_asset_read("/exploration/functional_topology.md")
    err = m._prewrite_gate_error()
    assert err is not None and "frame" in err
    for i in range(4):
        m._note_asset_read(f"/exploration/screenshots/frame_{i:06d}.png")
    assert m._prewrite_gate_error() is None


def test_prewrite_gate_clamped_for_small_handoffs(bundle, topology):
    rep = FakeReproduction(bundle, topology)
    m._reproduction = rep
    m._reset_asset_reads(rep.handoff_id, available_frames=2)
    m._note_asset_read("/exploration/functional_topology.md")
    m._note_asset_read("/exploration/screenshots/frame_000000.png")
    m._note_asset_read("/exploration/screenshots/frame_000001.png")
    assert m._prewrite_gate_error() is None


def test_reset_conversation_clears_reads(bundle, topology):
    rep = FakeReproduction(bundle, topology)
    m._reproduction = rep
    m._reset_asset_reads(rep.handoff_id, available_frames=3)
    m._note_asset_read("/exploration/screenshots/frame_000000.png")
    m._reset_conversation()
    assert m._asset_reads["handoff"] is None
    assert not m._asset_reads["frames"]


def test_gate_can_be_disabled(monkeypatch):
    monkeypatch.setenv("BBB_OUR_REQUIRE_ASSET_READS", "0")
    assert m._asset_gate_error() is None
    assert m._prewrite_gate_error() is None


# ------------------------------------------- tool-handler level gating
# The pre-write gate must also fire when the agent calls the MCP tool
# handlers directly (workspace_write AND workspace_run), not just the
# internal helper — otherwise a shell/python heredoc bypasses the check.


class StubWorkspace:
    """Records calls; stands in for ReproductionWorkspace."""

    def __init__(self, artifact_dir, topology_path, handoff_id="h1"):
        self.artifact_dir = artifact_dir
        self.topology_path = topology_path
        self.handoff_id = handoff_id
        self.calls = []

    def write_file(self, path, content):
        self.calls.append(("write", path))
        return {"wrote": path}

    def run_program(self, argv, cwd=".", timeout_seconds=30):
        self.calls.append(("run", argv))
        return {"exit_code": 0, "stdout": "", "stderr": "", "truncated": False}


def test_workspace_write_handler_blocked_before_reading(
        bundle, topology):
    rep = StubWorkspace(bundle, topology)
    m._reproduction = rep
    try:
        with pytest.raises(ValueError, match="pre-write gate"):
            m._t_workspace_write({"path": "index.html", "content": "<html>"})
        assert rep.calls == []
    finally:
        m._reproduction = None


def test_workspace_run_handler_blocked_before_reading(
        bundle, topology):
    rep = StubWorkspace(bundle, topology)
    m._reproduction = rep
    try:
        with pytest.raises(ValueError, match="pre-write gate"):
            m._t_workspace_run({"argv": ["python", "-c", "print(1)"]})
        assert rep.calls == []
    finally:
        m._reproduction = None


def test_workspace_handlers_allowed_after_digesting(bundle, topology):
    rep = StubWorkspace(bundle, topology)
    m._reproduction = rep
    try:
        m._reset_asset_reads(rep.handoff_id, available_frames=10)
        m._note_asset_read("/exploration/functional_topology.md")
        for i in range(4):
            m._note_asset_read(f"/exploration/screenshots/frame_{i:06d}.png")
        m._t_workspace_write({"path": "index.html", "content": "<html>"})
        m._t_workspace_run({"argv": ["python", "-c", "print(1)"]})
        assert [c[0] for c in rep.calls] == ["write", "run"]
    finally:
        m._reproduction = None


# ------------------------------------------- exemption vs topology check


def test_exemption_conflict_rejected_when_topology_has_write_features(
        bundle, topology):
    rep = StubWorkspace(bundle, topology)
    m._reproduction = rep
    try:
        # The topology fixture contains FEATURE f1 (no write words), so a
        # read-only topology accepts an exemption…
        read_only = Path(topology)
        summary = {"rounds": [{"decision": "accept",
                                "write_flow_exemption": "no write flows"}]}
        assert m._exemption_conflict_error(summary) is None
        # …but a topology naming a create/edit feature must reject it.
        writing = bundle / "topology_write.json"
        writing.write_text(json.dumps({
            "nodes": [{"type": "FEATURE", "name": "新建文档并保存",
                       "description": "create and save documents"}],
            "edges": [],
        }), encoding="utf-8")
        rep.topology_path = writing
        err = m._exemption_conflict_error(summary)
        assert err is not None and "conflicts" in err
        # No exemption -> no conflict.
        assert m._exemption_conflict_error(
            {"rounds": [{"decision": "accept",
                          "write_flow_exemption": None}]}) is None
    finally:
        m._reproduction = None


def test_old_revise_exemption_does_not_poison_final_accept(bundle, topology):
    rep = StubWorkspace(bundle, topology)
    writing = bundle / "topology_write.json"
    writing.write_text(json.dumps({
        "nodes": [{"type": "FEATURE", "name": "文档操作",
                   "behavior": {"postconditions": ["保存新建文档"]}}],
        "edges": [],
    }), encoding="utf-8")
    rep.topology_path = writing
    m._reproduction = rep
    try:
        summary = {"rounds": [
            {"decision": "revise", "write_flow_exemption": "早期误判"},
            {"decision": "accept", "write_flow_exemption": None,
             "write_flow_evidence": [{"flow": "保存文档"}]},
        ]}
        assert m._exemption_conflict_error(summary) is None
    finally:
        m._reproduction = None


# --------------------------------------------------------- handoff index


def test_handoff_index_marks_evidence_frames(tmp_path, bundle, topology):
    run_dir = tmp_path / "run"
    frames = run_dir / "frames"
    frames.mkdir(parents=True)
    for i in range(3):
        (frames / f"frame_{i:06d}.png").write_bytes(b"png")
    (run_dir / "functional_topology.md").write_text("topo", encoding="utf-8")
    shutil.copy2(topology, run_dir / "functional_topology.json")

    files = m._managed_exploration_files(run_dir)
    assert "INDEX.md" in files
    text = (run_dir / "reproduction_index.md").read_text(encoding="utf-8")
    # STATE cites frame 1, FEATURE evidence cites frames 0 and 2.
    assert "frame_000000.png" in text
    assert "frame_000001.png" in text
    assert "frame_000002.png" in text
    assert "Evidence frames cited" in text


# ------------------------------------------- write-flow evidence on accept


def _review_stub(tmp_path: Path, marks, hashes, action_log=None):
    """marks: [(observation_number, actions_at_capture), ...]"""
    review = rr.ManagedReproductionReview.__new__(
        rr.ManagedReproductionReview)
    out = tmp_path / "out"
    out.mkdir(exist_ok=True)
    (out / "index.html").write_text("<html>x</html>", encoding="utf-8")
    fp = rr._output_fingerprint(out)
    review.output_dir = out
    review.handoff_id = "t"
    review.max_revisions = 3
    review._runtime_factory = None
    review._runtime = object()
    review._temp_dir = None
    review._rounds = []
    review._revision_count = 0
    review._pending_revision = False
    review._revision_baseline = None
    review._accepted_fingerprint = None
    action_count = max(3, max((count for _, count in marks), default=0))
    if action_log is None:
        action_log = [{"type": "wait"} for _ in range(action_count)]
    assert len(action_log) == action_count
    review._active = {
        "round": 1, "entry_path": ".", "started_at": 0,
        "actions": action_count,
        "observations": len(hashes), "last_observed_action": action_count,
        "action_types": {}, "observation_hashes": list(hashes),
        "observation_marks": list(marks),
        "action_log": list(action_log),
        "start_fingerprint": fp,
    }
    return review


def test_review_reload_is_logged_as_trusted_probe():
    class Runtime:
        def __init__(self):
            self.keys = []

        def key(self, key, kind):
            self.keys.append((key, kind))

    review = rr.ManagedReproductionReview.__new__(rr.ManagedReproductionReview)
    runtime = Runtime()
    review._runtime = runtime
    review._active = {
        "round": 1, "actions": 0, "action_types": rr.Counter(),
        "action_log": [],
    }
    result = review.action("reload", {})
    assert result["action_accepted"] is True
    assert runtime.keys == [("F5", "press")]
    assert review._active["action_log"] == [{"type": "reload"}]


def test_action_classifiers_reject_passive_and_partial_navigation():
    assert rr._is_write_interaction({"type": "click"})
    assert not rr._is_write_interaction({"type": "wait"})
    assert not rr._is_write_interaction({"type": "key_press", "key": "F5"})
    assert rr._has_persistence_probe([{"type": "reload"}])
    assert rr._has_persistence_probe(
        [{"type": "key_press", "key": "F5"}])
    assert not rr._has_persistence_probe(
        [{"type": "key_press", "key": "BrowserBack"}])
    assert rr._has_persistence_probe([
        {"type": "key_press", "key": "BrowserBack"},
        {"type": "key_press", "key": "BrowserForward"},
    ])


def test_revise_needs_no_write_flow_evidence(tmp_path):
    review = _review_stub(
        tmp_path, marks=[(1, 0), (2, 1)], hashes=["a", "b"])
    out = review.complete_round(
        decision="revise", checked_flows=["页面能切换"],
        findings=["首屏空白"])
    assert out["decision"] == "revise"


def test_accept_without_evidence_or_exemption_rejected(tmp_path):
    review = _review_stub(
        tmp_path, marks=[(1, 0), (2, 1)], hashes=["a", "b"])
    with pytest.raises(ValueError, match="write_flow_evidence"):
        review.complete_round(
            decision="accept",
            checked_flows=["新建文档后列表出现新条目"],
            findings=["ok"])


def test_accept_with_identical_hash_pair_rejected(tmp_path):
    # identical screenshots across the pair -> not real evidence
    review = _review_stub(
        tmp_path, marks=[(1, 0), (2, 1), (3, 3)], hashes=["a", "a", "a"],
        action_log=[{"type": "click"}, {"type": "reload"},
                    {"type": "wait"}])
    with pytest.raises(ValueError, match="identical"):
        review.complete_round(
            decision="accept",
            checked_flows=["新建文档后列表出现新条目"],
            findings=["ok"],
            write_flow_evidence=[{"flow": "新建文档",
                                  "before_observation": 1,
                                  "after_observation": 2,
                                  "persisted_observation": 3}])


def test_accept_without_interaction_between_observations_rejected(tmp_path):
    # hash differs, but no action happened between the observations —
    # an animation or late-rendering diff cannot masquerade as a write.
    review = _review_stub(
        tmp_path, marks=[(1, 0), (2, 0), (3, 1)], hashes=["a", "b", "c"])
    with pytest.raises(ValueError, match="no interaction"):
        review.complete_round(
            decision="accept",
            checked_flows=["新建文档后列表出现新条目"],
            findings=["ok"],
            write_flow_evidence=[{"flow": "新建文档",
                                  "before_observation": 1,
                                  "after_observation": 2,
                                  "persisted_observation": 3}])


def test_accept_without_reload_before_persisted_rejected(tmp_path):
    # a write + visible change happened, but nothing between `after` and
    # `persisted` — the agent never re-entered the page.
    review = _review_stub(
        tmp_path, marks=[(1, 0), (2, 1), (3, 1)], hashes=["a", "b", "c"],
        action_log=[{"type": "click"}, {"type": "wait"},
                    {"type": "wait"}])
    with pytest.raises(ValueError, match="persisted observation"):
        review.complete_round(
            decision="accept",
            checked_flows=["新建文档后列表出现新条目"],
            findings=["ok"],
            write_flow_evidence=[{"flow": "新建文档",
                                  "before_observation": 1,
                                  "after_observation": 2,
                                  "persisted_observation": 3}])


def test_accept_with_out_of_range_observation_rejected(tmp_path):
    review = _review_stub(
        tmp_path, marks=[(1, 0), (2, 1)], hashes=["a", "b"])
    with pytest.raises(ValueError, match="1 <= before < after < persisted"):
        review.complete_round(
            decision="accept",
            checked_flows=["新建文档后列表出现新条目"],
            findings=["ok"],
            write_flow_evidence=[{"flow": "新建文档",
                                  "before_observation": 1,
                                  "after_observation": 9,
                                  "persisted_observation": 10}])


def test_accept_with_full_three_stage_evidence_passes(tmp_path):
    review = _review_stub(
        tmp_path, marks=[(1, 0), (2, 1), (3, 3)], hashes=["a", "b", "c"],
        action_log=[{"type": "click"}, {"type": "reload"},
                    {"type": "wait"}])
    out = review.complete_round(
        decision="accept",
        checked_flows=["新建文档后列表出现新条目"],
        findings=["ok"],
        write_flow_evidence=[{"flow": "新建小记并发布",
                              "before_observation": 1,
                              "after_observation": 2,
                              "persisted_observation": 3}])
    assert out["decision"] == "accept"
    record = review._rounds[-1]
    assert record["write_flow_evidence"][0]["flow"] == "新建小记并发布"
    assert record["write_flow_exemption"] is None


def test_accept_with_wait_only_write_interval_rejected(tmp_path):
    review = _review_stub(
        tmp_path, marks=[(1, 0), (2, 1), (3, 3)], hashes=["a", "b", "c"],
        action_log=[{"type": "wait"}, {"type": "reload"},
                    {"type": "wait"}])
    with pytest.raises(ValueError, match="passive activity"):
        review.complete_round(
            decision="accept", checked_flows=["新建文档"], findings=["ok"],
            write_flow_evidence=[{"flow": "新建文档",
                                  "before_observation": 1,
                                  "after_observation": 2,
                                  "persisted_observation": 3}])


def test_accept_without_explicit_persistence_probe_rejected(tmp_path):
    review = _review_stub(
        tmp_path, marks=[(1, 0), (2, 1), (3, 3)], hashes=["a", "b", "c"],
        action_log=[{"type": "click"}, {"type": "wait"},
                    {"type": "move_pointer"}])
    with pytest.raises(ValueError, match="no explicit reload/return"):
        review.complete_round(
            decision="accept", checked_flows=["新建文档"], findings=["ok"],
            write_flow_evidence=[{"flow": "新建文档",
                                  "before_observation": 1,
                                  "after_observation": 2,
                                  "persisted_observation": 3}])


def test_accept_when_reload_returns_to_before_state_rejected(tmp_path):
    review = _review_stub(
        tmp_path, marks=[(1, 0), (2, 1), (3, 3)], hashes=["a", "b", "a"],
        action_log=[{"type": "click"}, {"type": "reload"},
                    {"type": "wait"}])
    with pytest.raises(ValueError, match="matches the before state"):
        review.complete_round(
            decision="accept", checked_flows=["新建文档"], findings=["未保留"],
            write_flow_evidence=[{"flow": "新建文档",
                                  "before_observation": 1,
                                  "after_observation": 2,
                                  "persisted_observation": 3}])


def test_accept_with_exemption_passes(tmp_path):
    review = _review_stub(
        tmp_path, marks=[(1, 0), (2, 1)], hashes=["a", "b"])
    out = review.complete_round(
        decision="accept",
        checked_flows=["导航切换正常"],
        findings=["ok"],
        write_flow_exemption="目标为纯静态展示站，无任何写路径功能")
    assert out["decision"] == "accept"
    assert review._rounds[-1]["write_flow_exemption"].startswith("目标为")


def test_empty_exemption_rejected(tmp_path):
    review = _review_stub(
        tmp_path, marks=[(1, 0), (2, 1)], hashes=["a", "b"])
    with pytest.raises(ValueError, match="exemption"):
        review.complete_round(
            decision="accept", checked_flows=["x"], findings=["ok"],
            write_flow_exemption="   ")


# ------------------------------------------- handoff failure retry state


def test_failed_handoff_keeps_retry_path(monkeypatch, tmp_path):
    """复现启动失败必须进入可重试态：不得自动新建默认会话，finalize 可重试。

    回归：2026-09-15 语雀探索定稿后复现启动失败，下一个工具调用自动创建了
    默认目标(ecommerce_demo)的新会话，把对话锁死在错误目标上。
    """
    sid = "sess_20260915_120000_feed1234"
    topology = tmp_path / "runs" / sid / "functional_topology.json"
    topology.parent.mkdir(parents=True)
    topology.write_text('{"nodes":[],"edges":[]}', encoding="utf-8")
    starts: list[dict] = []

    class Failing:
        @classmethod
        def start(cls, **kwargs):
            starts.append(kwargs)
            raise RuntimeError("docker handoff exploded")

    class Working:
        handoff_id = "h1"

        @classmethod
        def start(cls, **kwargs):
            starts.append(kwargs)
            return cls()

        def started_payload(self):
            return {"stage": "reproduction", "ready": True, "handoff_id": "h1"}

    monkeypatch.setattr(m.benchmark_config, "RUNS_DIR", tmp_path / "runs")
    monkeypatch.setattr(m, "_session", sid)
    monkeypatch.setattr(m, "_review", None)
    monkeypatch.setattr(m, "_bound_target", "app:miniapp")
    monkeypatch.delenv("BBB_REPRODUCTION_AUTOSTART", raising=False)
    monkeypatch.setattr(m, "ReproductionWorkspace", Failing)

    # 1) finalize: 会话已定稿，复现启动抛异常 → 待重试态
    result = m._t_finalize({})
    assert result["isError"]
    assert m._finalize_degraded is True
    assert m._finalized_source == sid

    # 2) 会话关闭后绑定被释放：不得静默创建默认目标的会话
    monkeypatch.setattr(m, "_session", "")
    with pytest.raises(RuntimeError, match="finalize"):
        m._ensure_session()
    data, err = m._post("/observe", {})
    assert data is None
    assert err is not None and "finalize" in err and "新开一个对话" in err
    assert m._session == ""

    # 3) start_session 也不能把对话引向第二次探索
    result = m._t_start_session({"app_id": "miniapp"})
    assert result["isError"]
    assert "finalize" in result["content"][0]["text"]

    # 4) finalize 重试：不需要 _session，用定稿锚点重新启动并恢复状态
    monkeypatch.setattr(m, "ReproductionWorkspace", Working)
    result = m._t_finalize({})
    assert not result["isError"]
    payload = json.loads(result["content"][0]["text"])
    assert payload["stage"] == "reproduction"
    assert payload["exploration_finished"] is True
    assert starts[-1]["source_id"] == sid
    assert m._finalize_degraded is False


def test_mcp_log_written_to_file(monkeypatch, tmp_path):
    """诊断日志必须落盘：工具调用超时时 stdio 通道不会留下任何记录。"""
    log_path = tmp_path / "mcp_server.log"
    monkeypatch.setenv("BBB_MCP_LOG", str(log_path))
    m._log("reproduction handoff failed: RuntimeError: boom")

    assert log_path.is_file()
    text = log_path.read_text(encoding="utf-8")
    assert "[our-method-mcp]" in text
    assert "reproduction handoff failed: RuntimeError: boom" in text
