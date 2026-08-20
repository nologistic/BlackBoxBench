"""FastAPI controller server: agent channel + operator/dashboard API.

Route families (see docs/api_contract.md):
- /agent/{sid}/...   the ONLY agent-reachable surface; whitelist response schemas
- /api/...           operator/dashboard API
- /                  static dashboard
"""
from __future__ import annotations

import io
from pathlib import Path

from fastapi import FastAPI, HTTPException
from fastapi.responses import FileResponse, JSONResponse
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel

from . import config
from .metrics import compute_metrics
from .orchestrator import archive
from .orchestrator.manager import SessionManager
from .orchestrator.session import BudgetExhausted, SessionClosed
from .replay.replayer import deterministic_replay
from .topology import models as m
from .topology.store import EvidenceError

manager = SessionManager()


class CreateSession(BaseModel):
    app_id: str | None = None
    seed: str | None = None
    budget: dict | None = None
    # ad-hoc live target: explore any website without a registry entry
    live_url: str | None = None


def create_app() -> FastAPI:
    app = FastAPI(title="BlackBoxBench Controller", docs_url=None,
                  redoc_url=None, openapi_url=None)

    # ---------------------------------------------------------- errors
    @app.exception_handler(BudgetExhausted)
    async def _budget(_, exc):
        return JSONResponse({"detail": "budget_exhausted"}, status_code=403)

    @app.exception_handler(SessionClosed)
    async def _closed(_, exc):
        return JSONResponse({"detail": str(exc)}, status_code=410)

    @app.exception_handler(EvidenceError)
    async def _evidence(_, exc):
        return JSONResponse({"detail": "evidence_invalid", "message": str(exc)},
                            status_code=422)

    # ---------------------------------------------------------- helpers
    def _session(sid: str):
        try:
            return manager.get(sid)
        except KeyError:
            raise HTTPException(404, detail="session_not_found")

    def _session_or_archive(sid: str):
        """(live Session, None) or (None, archived artifact dir).

        Read-only operator GET routes serve both; mutating/agent routes must
        keep using _session() (live only).
        """
        try:
            return manager.get(sid), None
        except KeyError:
            d = archive.archived_dir(manager.runs_dir, sid)
            if d is None:
                raise HTTPException(404, detail="session_not_found")
            return None, d

    # ---------------------------------------------------------- agent channel
    @app.post("/agent/{sid}/observe")
    def observe(sid: str):
        return _session(sid).observe()

    @app.post("/agent/{sid}/action")
    def action(sid: str, action: m.Action):
        sess = _session(sid)
        try:
            return sess.execute(action)
        except ValueError as e:
            raise HTTPException(400, detail=str(e))

    @app.post("/agent/{sid}/discovery/state")
    def add_state(sid: str, req: m.StateCreate):
        node = _session(sid).store.add_state(req)
        return {"state_id": node.id}

    @app.post("/agent/{sid}/discovery/feature")
    def add_feature(sid: str, req: m.FeatureCreate):
        node = _session(sid).store.add_feature(req)
        return {"feature_id": node.id}

    @app.post("/agent/{sid}/discovery/data")
    def add_data(sid: str, req: m.DataCreate):
        node = _session(sid).store.add_data(req)
        return {"data_id": node.id}

    @app.post("/agent/{sid}/discovery/edge")
    def add_edge(sid: str, req: m.EdgeCreate):
        edge = _session(sid).store.add_edge(req)
        return {"edge_id": edge.id}

    @app.post("/agent/{sid}/discovery/hypothesis")
    def add_hyp(sid: str, req: m.HypothesisCreate):
        hyp = _session(sid).store.add_hypothesis(req)
        return {"hypothesis_id": hyp.id}

    @app.post("/agent/{sid}/discovery/hypothesis/{hid}/resolve")
    def resolve_hyp(sid: str, hid: str, req: m.HypothesisResolve):
        try:
            _session(sid).store.resolve_hypothesis(hid, req)
        except KeyError:
            raise HTTPException(404, detail="hypothesis_not_found")
        return {"ok": True}

    @app.patch("/agent/{sid}/discovery/{kind}/{node_id}")
    def revise(sid: str, kind: str, node_id: str, req: m.RevisionRequest):
        if kind not in ("state", "feature", "data"):
            raise HTTPException(400, detail="invalid_kind")
        try:
            return _session(sid).store.revise(kind, node_id, req)
        except KeyError:
            raise HTTPException(404, detail="node_not_found")
        except ValueError as e:
            raise HTTPException(400, detail=str(e))

    @app.post("/agent/{sid}/finalize")
    def finalize(sid: str):
        summary = _session(sid).finalize()
        return {"topology_path": f"runs/{sid}/functional_topology.json",
                "summary": summary}

    # ---------------------------------------------------------- operator API
    @app.post("/api/sessions")
    def create_session(req: CreateSession):
        if not req.app_id and not req.live_url:
            raise HTTPException(400, detail="app_id or live_url required")
        try:
            if req.live_url:
                from .orchestrator.apps import make_live_spec_for_url
                try:
                    spec = make_live_spec_for_url(req.live_url)
                except ValueError as e:
                    raise HTTPException(400, detail=str(e))
                sess = manager.create_spec(spec, req.budget)
            else:
                sess = manager.create(req.app_id, req.budget)
        except HTTPException:
            raise
        except KeyError as e:
            raise HTTPException(404, detail=str(e))
        except Exception as e:
            raise HTTPException(503, detail=f"runtime_start_failed: {e}")
        return {"session_id": sess.id, "brief": sess.spec.brief,
                "app_id": sess.spec.app_id}

    @app.get("/api/sessions")
    def list_sessions():
        return manager.list()

    @app.get("/api/apps")
    def list_apps_route():
        return manager.apps()

    @app.get("/api/sessions/{sid}")
    def session_status(sid: str):
        sess, d = _session_or_archive(sid)
        return sess.status_dict() if sess else archive.read_status(d)

    @app.post("/api/sessions/{sid}/reset")
    def reset_session(sid: str):
        _session(sid)  # live-only: 404 for archived/unknown
        manager.reset(sid)
        return {"ok": True}

    @app.post("/api/sessions/{sid}/close")
    def close_session(sid: str):
        _session(sid).close()
        return {"ok": True}

    @app.post("/api/sessions/{sid}/replay")
    def replay_session(sid: str, mode: str = "visual"):
        sess = _session(sid)
        if mode == "deterministic":
            return deterministic_replay(sess)
        if mode == "visual":
            return {"ok": True,
                    "hint": "open the dashboard Replay view, or render video via "
                            "benchmark.recorder.video.render_video"}
        raise HTTPException(400, detail="invalid_mode")

    @app.get("/api/sessions/{sid}/frames/{frame_id}.png")
    def frame(sid: str, frame_id: int):
        sess, d = _session_or_archive(sid)
        p = sess.recorder.frame_path(frame_id) if sess else \
            archive.frame_path(d, frame_id)
        if p is None or not p.exists():
            raise HTTPException(404, detail="frame_not_found")
        return FileResponse(p, media_type="image/png")

    @app.get("/api/sessions/{sid}/live.png")
    def live(sid: str):
        sess, d = _session_or_archive(sid)
        if sess:
            status = sess.status_dict()
            fid = status.get("current_frame")
            p = sess.recorder.frame_path(fid) if fid is not None else None
        else:
            p = archive.last_frame_path(d)  # static last frame for archived
        if p is None or not p.exists():
            raise HTTPException(404, detail="no_frame_yet")
        return FileResponse(p, media_type="image/png",
                            headers={"Cache-Control": "no-store"})

    @app.get("/api/sessions/{sid}/trace")
    def trace(sid: str):
        sess, d = _session_or_archive(sid)
        if sess:
            return {"actions": sess.recorder.read_actions(),
                    "observations": sess.recorder.read_observations()}
        return archive.read_trace(d)

    @app.get("/api/sessions/{sid}/topology")
    def topology(sid: str):
        sess, d = _session_or_archive(sid)
        if sess:
            return sess.store.graph().model_dump()
        g = archive.read_topology(d)
        if g is None:
            raise HTTPException(404, detail="topology_not_found")
        return g

    @app.get("/api/sessions/{sid}/hypotheses")
    def hypotheses(sid: str):
        sess, d = _session_or_archive(sid)
        if sess:
            return [h.model_dump() for h in sess.store.hypotheses()]
        return archive.read_hypotheses(d)

    @app.get("/api/sessions/{sid}/metrics")
    def metrics(sid: str):
        sess, d = _session_or_archive(sid)
        if sess:
            import time
            return compute_metrics(sess.dir, sess.recorder,
                                   sess.budget.actions_used,
                                   sess.budget.observations_used,
                                   int(time.monotonic() - sess.budget.started_at),
                                   sess.store)
        mdata = archive.read_metrics(d)
        if mdata is None:
            raise HTTPException(404, detail="metrics_not_found")
        return mdata

    # ---------------------------------------------------------- dashboard
    static_dir = config.PROJECT_ROOT / "dashboard" / "static"
    if static_dir.exists():
        app.mount("/", StaticFiles(directory=static_dir, html=True),
                  name="dashboard")

    return app


app = create_app()


def main() -> None:
    import argparse
    import uvicorn
    parser = argparse.ArgumentParser(description="BlackBoxBench controller")
    parser.add_argument("--port", type=int, default=7800)
    parser.add_argument("--host", default="127.0.0.1")
    args = parser.parse_args()
    uvicorn.run("benchmark.server:app", host=args.host, port=args.port,
                log_level="warning")


if __name__ == "__main__":
    main()
