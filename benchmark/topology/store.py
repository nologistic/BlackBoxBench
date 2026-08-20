"""Incremental functional-topology store.

Responsibilities:
- canonical id allocation (state_/feature_/data_ + slug)
- evidence validation against the session trace (frames/steps must exist)
- revisions: update / merge / delete (agent-initiated only, never auto-merge)
- aggregation into the final TopologyGraph document + markdown rendering
- append-only audit via recorder.log_discovery
"""
from __future__ import annotations

import re
import threading
from pathlib import Path

from ..recorder.trace import TraceRecorder, utc_now
from . import models as m


def _slug(name: str) -> str:
    s = re.sub(r"[^a-z0-9]+", "_", name.lower()).strip("_")
    return s or "unnamed"


class EvidenceError(ValueError):
    pass


class TopologyStore:
    def __init__(self, session_id: str, app_id: str, recorder: TraceRecorder,
                 session_dir: Path):
        self.session_id = session_id
        self.app_id = app_id
        self._rec = recorder
        self._dir = Path(session_dir)
        self._lock = threading.Lock()
        self._nodes: dict[str, m.GraphNode] = {}
        self._edges: dict[str, m.Edge] = {}
        self._hyps: dict[str, m.Hypothesis] = {}
        self._edge_seq = 0
        self._hyp_seq = 0
        self._current_step = 0

    # ------------------------------------------------------------ context

    def set_step(self, step: int) -> None:
        self._current_step = step

    def _validate_evidence(self, evidence: list[m.Evidence]) -> None:
        for ev in evidence:
            if not self._rec.frame_exists(ev.before_frame):
                raise EvidenceError(f"before_frame {ev.before_frame} does not exist")
            if not self._rec.frame_exists(ev.after_frame):
                raise EvidenceError(f"after_frame {ev.after_frame} does not exist")
            if not self._rec.step_exists(ev.step):
                raise EvidenceError(f"step {ev.step} not found in accepted actions")

    def _validate_frames(self, frames: list[int]) -> None:
        for f in frames:
            if not self._rec.frame_exists(f):
                raise EvidenceError(f"frame {f} does not exist")

    def _alloc_id(self, prefix: str, name: str) -> str:
        slug = _slug(name)
        # agents often include the kind in the name already — don't double it
        base = slug if slug.startswith(prefix + "_") else f"{prefix}_{slug}"
        nid, i = base, 2
        while nid in self._nodes:
            nid = f"{base}_{i}"
            i += 1
        return nid

    # ------------------------------------------------------------ creates

    def add_state(self, req: m.StateCreate) -> m.StateNode:
        self._validate_frames(req.visual_evidence)
        with self._lock:
            node = m.StateNode(id=self._alloc_id("state", req.name),
                               created_step=self._current_step,
                               updated_step=self._current_step, **req.model_dump())
            self._nodes[node.id] = node
        self._rec.log_discovery("create_state", node.model_dump())
        self._persist()
        return node

    def add_feature(self, req: m.FeatureCreate) -> m.FeatureNode:
        self._validate_evidence(req.evidence)
        if req.status == m.ClaimStatus.CONFIRMED and not req.evidence:
            raise EvidenceError(
                "confirmed feature requires at least one evidence entry")
        with self._lock:
            node = m.FeatureNode(id=self._alloc_id("feature", req.name),
                                 created_step=self._current_step,
                                 updated_step=self._current_step, **req.model_dump())
            self._nodes[node.id] = node
        self._rec.log_discovery("create_feature", node.model_dump())
        self._persist()
        return node

    def add_data(self, req: m.DataCreate) -> m.DataNode:
        with self._lock:
            node = m.DataNode(id=self._alloc_id("data", req.name),
                              created_step=self._current_step,
                              updated_step=self._current_step, **req.model_dump())
            self._nodes[node.id] = node
        self._rec.log_discovery("create_data", node.model_dump())
        self._persist()
        return node

    def add_edge(self, req: m.EdgeCreate) -> m.Edge:
        if req.source not in self._nodes:
            raise EvidenceError(f"edge source {req.source!r} is not a known node")
        if req.target not in self._nodes:
            raise EvidenceError(f"edge target {req.target!r} is not a known node")
        self._validate_evidence(req.evidence)
        if req.status == m.ClaimStatus.CONFIRMED and not req.evidence:
            raise EvidenceError("confirmed edge requires at least one evidence entry")
        with self._lock:
            self._edge_seq += 1
            edge = m.Edge(id=f"edge_{self._edge_seq:04d}",
                          created_step=self._current_step, **req.model_dump())
            self._edges[edge.id] = edge
        self._rec.log_discovery("create_edge", edge.model_dump())
        self._persist()
        return edge

    def add_hypothesis(self, req: m.HypothesisCreate) -> m.Hypothesis:
        self._validate_evidence(req.evidence)
        with self._lock:
            self._hyp_seq += 1
            hyp = m.Hypothesis(id=f"hyp_{self._hyp_seq:04d}",
                               created_step=self._current_step, **req.model_dump())
            self._hyps[hyp.id] = hyp
        self._rec.log_discovery("create_hypothesis", hyp.model_dump())
        self._persist()
        return hyp

    def resolve_hypothesis(self, hid: str, req: m.HypothesisResolve) -> m.Hypothesis:
        self._validate_evidence(req.evidence)
        with self._lock:
            if hid not in self._hyps:
                raise KeyError(hid)
            hyp = self._hyps[hid]
            hyp.status = m.HypothesisStatus(req.status)
            hyp.evidence.extend(req.evidence)
            hyp.note = req.note
            hyp.resolved_step = self._current_step
        self._rec.log_discovery("resolve_hypothesis", {hid: req.model_dump()})
        self._persist()
        return hyp

    # ------------------------------------------------------------ revisions

    def revise(self, kind: str, node_id: str, req: m.RevisionRequest) -> dict:
        with self._lock:
            if node_id not in self._nodes:
                raise KeyError(node_id)
            node = self._nodes[node_id]
            if kind == "state" and not isinstance(node, m.StateNode):
                raise ValueError(f"{node_id} is not a STATE")
            if kind == "feature" and not isinstance(node, m.FeatureNode):
                raise ValueError(f"{node_id} is not a FEATURE")
            if kind == "data" and not isinstance(node, m.DataNode):
                raise ValueError(f"{node_id} is not a DATA")

            if req.op == "update":
                fields = dict(req.fields or {})
                for k in ("id", "type", "created_step"):
                    fields.pop(k, None)
                if "evidence" in fields:
                    ev = [m.Evidence(**e) for e in fields["evidence"]]
                    self._validate_evidence(ev)
                    fields["evidence"] = ev
                if "visual_evidence" in fields:
                    self._validate_frames(fields["visual_evidence"])
                data = node.model_dump()
                data.update(fields)
                data["updated_step"] = self._current_step
                self._nodes[node_id] = type(node)(**data)
                result = {"updated": node_id}
            elif req.op == "merge":
                if not req.into or req.into not in self._nodes:
                    raise EvidenceError(f"merge target {req.into!r} unknown")
                if type(self._nodes[req.into]) is not type(node):
                    raise ValueError("can only merge nodes of the same type")
                survivor = self._nodes[req.into]
                # fold evidence in, keep max confidence, drop the loser
                sdata = survivor.model_dump()
                ndata = node.model_dump()
                for key in ("evidence", "visual_evidence"):
                    if key in sdata and key in ndata:
                        merged = sdata[key] + [e for e in ndata[key]
                                               if e not in sdata[key]]
                        sdata[key] = merged
                sdata["confidence"] = max(sdata["confidence"], ndata["confidence"])
                sdata["updated_step"] = self._current_step
                self._nodes[req.into] = type(survivor)(**sdata)
                del self._nodes[node_id]
                # redirect edges
                for e in self._edges.values():
                    if e.source == node_id:
                        e.source = req.into
                    if e.target == node_id:
                        e.target = req.into
                result = {"merged": node_id, "into": req.into}
            else:  # delete
                del self._nodes[node_id]
                self._edges = {eid: e for eid, e in self._edges.items()
                               if e.source != node_id and e.target != node_id}
                result = {"deleted": node_id}
        self._rec.log_discovery(f"revise_{req.op}",
                                {"kind": kind, "id": node_id,
                                 **req.model_dump(exclude_none=True)})
        self._persist()
        return result

    # ------------------------------------------------------------ read/finalize

    def graph(self) -> m.TopologyGraph:
        nodes = list(self._nodes.values())
        edges = list(self._edges.values())
        confirmed = [n for n in nodes if n.status == m.ClaimStatus.CONFIRMED]
        return m.TopologyGraph(
            app_id=self.app_id,
            session_id=self.session_id,
            generated_at=utc_now(),
            nodes=nodes,
            edges=edges,
            unresolved_questions=[h for h in self._hyps.values()
                                  if h.status in (m.HypothesisStatus.UNVERIFIED,
                                                  m.HypothesisStatus.UNCERTAIN)],
            coverage_summary=m.CoverageSummary(
                states=sum(1 for n in nodes if isinstance(n, m.StateNode)),
                features=sum(1 for n in nodes if isinstance(n, m.FeatureNode)),
                data_entities=sum(1 for n in nodes if isinstance(n, m.DataNode)),
                edges=len(edges),
                confirmed_ratio=(len(confirmed) / len(nodes)) if nodes else 0.0,
                actions_used=self._current_step,
            ),
        )

    def hypotheses(self) -> list[m.Hypothesis]:
        return list(self._hyps.values())

    def _persist(self) -> None:
        import json
        g = self.graph()
        (self._dir / "topology.json").write_text(
            g.model_dump_json(indent=2), encoding="utf-8")
        (self._dir / "hypotheses.json").write_text(
            json.dumps([h.model_dump() for h in self._hyps.values()],
                       ensure_ascii=False, indent=2, default=str),
            encoding="utf-8")

    def finalize(self) -> dict:
        """Write final deliverables; returns summary."""
        import json
        g = self.graph()
        (self._dir / "functional_topology.json").write_text(
            g.model_dump_json(indent=2), encoding="utf-8")
        (self._dir / "functional_topology.md").write_text(
            render_markdown(g), encoding="utf-8")
        (self._dir / "coverage_report.json").write_text(json.dumps({
            "app_id": g.app_id,
            "session_id": g.session_id,
            "coverage": g.coverage_summary.model_dump(),
            "unresolved_questions": [
                {"id": h.id, "statement": h.statement, "status": h.status,
                 "confidence": h.confidence}
                for h in g.unresolved_questions],
            "nodes_by_status": {
                s: sum(1 for n in g.nodes if n.status == s)
                for s in ("confirmed", "hypothesized", "uncertain", "rejected")},
        }, ensure_ascii=False, indent=2), encoding="utf-8")
        self._rec.log_discovery("finalize", {"nodes": len(g.nodes),
                                             "edges": len(g.edges)})
        return {"nodes": len(g.nodes), "edges": len(g.edges),
                "unresolved": len(g.unresolved_questions)}


# ------------------------------------------------------------------ markdown

def render_markdown(g: m.TopologyGraph) -> str:
    lines = [f"# Functional Topology — {g.app_id}", ""]
    lines.append(f"- session: `{g.session_id}`")
    lines.append(f"- generated: {g.generated_at}")
    cs = g.coverage_summary
    lines.append(f"- coverage: {cs.states} states · {cs.features} features · "
                 f"{cs.data_entities} data · {cs.edges} edges "
                 f"(confirmed ratio {cs.confirmed_ratio:.0%}, "
                 f"{cs.actions_used} actions)")
    lines.append("")

    by_type: dict[str, list] = {"STATE": [], "FEATURE": [], "DATA": []}
    for n in g.nodes:
        by_type[n.type].append(n)

    def status_mark(s: str) -> str:
        return {"confirmed": "✓", "hypothesized": "?",
                "uncertain": "~", "rejected": "✗"}.get(s, s)

    lines.append("## Graph")
    lines.append("```")
    lines.append(f"{g.app_id}")
    for t, label in (("STATE", "States"), ("FEATURE", "Features"),
                     ("DATA", "Data")):
        lines.append(f"├─ {label}")
        for n in by_type[t]:
            lines.append(f"│  ├─ [{status_mark(n.status)}] {n.name} "
                         f"({n.confidence:.2f}) `{n.id}`")
            for e in g.edges:
                etype = e.type.value if hasattr(e.type, "value") else e.type
                if e.source == n.id:
                    tgt = next((x.name for x in g.nodes if x.id == e.target), e.target)
                    lines.append(f"│  │    ─{etype}→ {tgt}")
                if e.target == n.id and e.type in ("REQUIRES", "DEPENDS_ON"):
                    src = next((x.name for x in g.nodes if x.id == e.source), e.source)
                    lines.append(f"│  │    ←{etype}─ {src}")
    lines.append("```")
    lines.append("")

    lines.append("## Features")
    for n in by_type["FEATURE"]:
        assert isinstance(n, m.FeatureNode)
        lines.append(f"### {n.name} `{n.id}`")
        lines.append(f"- status: {n.status} · confidence: {n.confidence:.2f}")
        if n.description:
            lines.append(f"- {n.description}")
        b = n.behavior
        for label, items in (("Preconditions", b.preconditions),
                             ("Postconditions", b.postconditions),
                             ("Persistent effects", b.persistent_effects),
                             ("Constraints", b.constraints),
                             ("Error cases", b.error_cases)):
            if items:
                lines.append(f"- **{label}**:")
                for it in items:
                    lines.append(f"  - {it}")
        if n.evidence:
            ev = n.evidence[0]
            lines.append(f"- evidence: step {ev.step} "
                         f"(frame {ev.before_frame} → {ev.after_frame})")
        lines.append("")

    if g.unresolved_questions:
        lines.append("## Unresolved questions")
        for h in g.unresolved_questions:
            lines.append(f"- [{h.status}] {h.statement} ({h.confidence:.2f})"
                         + (f" — next probe: {h.next_probe}" if h.next_probe else ""))
        lines.append("")
    return "\n".join(lines)
