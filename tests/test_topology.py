"""TopologyStore unit tests — no browser needed."""
from __future__ import annotations

import pytest
from pydantic import ValidationError

from benchmark.recorder.trace import TraceRecorder
from benchmark.topology import models as m
from benchmark.topology.store import EvidenceError, TopologyStore


@pytest.fixture()
def store(tmp_path):
    rec = TraceRecorder(tmp_path)
    # two fake frames and one accepted action to reference as evidence
    f0 = rec.save_frame(b"png0")
    f1 = rec.save_frame(b"png1")
    rec.log_action(m.ActionRecord(
        step=1, timestamp="t", action=m.Action(type="click", x=1, y=2),
        accepted=True, before_frame=f0, after_frame=f1))
    st = TopologyStore("sess_test", "miniapp", rec, tmp_path)
    st.set_step(1)
    yield st, f0, f1
    rec.close()


def _ev(f0, f1):
    return [m.Evidence(step=1, before_frame=f0, action="click(1,2)",
                       after_frame=f1)]


class TestCreateAndValidate:
    def test_canonical_ids(self, store):
        st, f0, f1 = store
        a = st.add_state(m.StateCreate(name="Cart Page", confidence=0.9))
        b = st.add_state(m.StateCreate(name="Cart Page", confidence=0.5))
        assert a.id == "state_cart_page"
        assert b.id == "state_cart_page_2"  # no silent overwrite

    def test_feature_with_evidence(self, store):
        st, f0, f1 = store
        node = st.add_feature(m.FeatureCreate(
            name="Do thing", evidence=_ev(f0, f1), confidence=0.9))
        assert node.id == "feature_do_thing"

    def test_confirmed_needs_evidence(self, store):
        st, _, _ = store
        with pytest.raises(EvidenceError):
            st.add_feature(m.FeatureCreate(name="No proof", confidence=0.9))
        # but hypothesized is allowed without evidence
        n = st.add_feature(m.FeatureCreate(
            name="Maybe", confidence=0.3, status=m.ClaimStatus.HYPOTHESIZED))
        assert n.status == m.ClaimStatus.HYPOTHESIZED

    def test_bad_frame_rejected(self, store):
        st, _, _ = store
        with pytest.raises(EvidenceError):
            st.add_state(m.StateCreate(name="X", visual_evidence=[999]))

    def test_bad_step_rejected(self, store):
        st, f0, f1 = store
        with pytest.raises(EvidenceError):
            st.add_feature(m.FeatureCreate(
                name="Y", confidence=0.5,
                evidence=[m.Evidence(step=999, before_frame=f0,
                                     action="x", after_frame=f1)]))

    def test_frames_must_belong_to_evidence_step(self, store):
        st, f0, f1 = store
        with pytest.raises(EvidenceError, match="does not belong"):
            st.add_feature(m.FeatureCreate(
                name="Mismatched proof", confidence=0.5,
                evidence=[m.Evidence(step=1, before_frame=f1,
                                     action="click", after_frame=f0)]))

    def test_edge_endpoints_must_exist(self, store):
        st, f0, f1 = store
        a = st.add_state(m.StateCreate(name="A"))
        with pytest.raises(EvidenceError):
            st.add_edge(m.EdgeCreate(source=a.id, target="state_missing",
                                     type=m.EdgeType.REQUIRES,
                                     evidence=_ev(f0, f1)))

    def test_leak_screening(self):
        with pytest.raises(ValidationError):
            m.FeatureCreate(name="x", description="uses POST /api/cart")
        with pytest.raises(ValidationError):
            m.StateCreate(name="x", description="defined in src/components/Cart.tsx")


class TestRevisions:
    def test_update(self, store):
        st, _, _ = store
        n = st.add_state(m.StateCreate(name="S", confidence=0.4))
        st.revise("state", n.id, m.RevisionRequest(
            op="update", fields={"confidence": 0.8, "description": "d"}))
        assert st.graph().nodes[0].confidence == 0.8
        assert st.graph().nodes[0].description == "d"

    def test_merge_redirects_edges(self, store):
        st, f0, f1 = store
        a = st.add_state(m.StateCreate(name="Same A"))
        b = st.add_state(m.StateCreate(name="Same B"))
        f = st.add_feature(m.FeatureCreate(name="F", evidence=_ev(f0, f1)))
        st.add_edge(m.EdgeCreate(source=f.id, target=b.id,
                                 type=m.EdgeType.TRANSITIONS_TO,
                                 evidence=_ev(f0, f1)))
        st.revise("state", b.id, m.RevisionRequest(op="merge", into=a.id))
        g = st.graph()
        assert len(g.nodes) == 2
        assert g.edges[0].target == a.id

    def test_delete_removes_attached_edges(self, store):
        st, f0, f1 = store
        a = st.add_state(m.StateCreate(name="A"))
        f = st.add_feature(m.FeatureCreate(name="F", evidence=_ev(f0, f1)))
        st.add_edge(m.EdgeCreate(source=f.id, target=a.id,
                                 type=m.EdgeType.TRANSITIONS_TO,
                                 evidence=_ev(f0, f1)))
        st.revise("state", a.id, m.RevisionRequest(op="delete"))
        g = st.graph()
        assert len(g.nodes) == 1 and len(g.edges) == 0

    def test_merge_different_types_rejected(self, store):
        st, f0, f1 = store
        a = st.add_state(m.StateCreate(name="A"))
        f = st.add_feature(m.FeatureCreate(name="F", evidence=_ev(f0, f1)))
        with pytest.raises(ValueError):
            st.revise("state", a.id, m.RevisionRequest(op="merge", into=f.id))


class TestHypotheses:
    def test_lifecycle(self, store):
        st, f0, f1 = store
        h = st.add_hypothesis(m.HypothesisCreate(
            statement="maybe persists", confidence=0.4, next_probe="refresh"))
        assert h.status == m.HypothesisStatus.UNVERIFIED
        g = st.graph()
        assert len(g.unresolved_questions) == 1
        st.resolve_hypothesis(h.id, m.HypothesisResolve(
            status="confirmed", evidence=_ev(f0, f1)))
        g = st.graph()
        assert len(g.unresolved_questions) == 0
        assert st.hypotheses()[0].status == m.HypothesisStatus.CONFIRMED


class TestFinalize:
    def test_finalize_writes_documents(self, store, tmp_path):
        st, f0, f1 = store
        st.add_state(m.StateCreate(name="Home", confidence=0.9))
        st.add_feature(m.FeatureCreate(name="Do", evidence=_ev(f0, f1),
                                       confidence=0.8))
        summary = st.finalize()
        assert summary["nodes"] == 2
        assert (tmp_path / "functional_topology.json").exists()
        md = (tmp_path / "functional_topology.md").read_text(encoding="utf-8")
        assert "Home" in md and "Do" in md
