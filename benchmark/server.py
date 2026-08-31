"""FastAPI controller server: agent channel + minimal operator API.

Route families (see docs/api_contract.md):
- /agent/{sid}/...   the ONLY agent-reachable surface; whitelist response schemas
- /api/...           session lifecycle and live inspection
"""
from __future__ import annotations

from fastapi import FastAPI, HTTPException
from fastapi.responses import FileResponse, JSONResponse
from pydantic import BaseModel, ConfigDict, Field

from . import config
from .orchestrator.manager import SessionManager
from .orchestrator.session import BudgetExhausted, SessionClosed
from .topology import models as m
from .topology.store import EvidenceError

manager = SessionManager()


class SessionBudget(BaseModel):
    model_config = ConfigDict(extra="forbid")

    max_actions: int = Field(default=config.DEFAULT_MAX_ACTIONS, gt=0)
    max_duration_s: int = Field(default=config.DEFAULT_MAX_DURATION_S, gt=0)
    max_observations: int = Field(default=config.DEFAULT_MAX_OBSERVATIONS, gt=0)


class CreateSession(BaseModel):
    model_config = ConfigDict(extra="forbid")

    app_id: str | None = None
    budget: SessionBudget | None = None
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
        try:
            summary = _session(sid).finalize()
        except ValueError as e:
            # e.g. cross-session contamination: the session stays runnable so
            # the agent can correct its discoveries and retry.
            raise HTTPException(409, detail=str(e))
        manager.release_runtime(sid)
        return {"topology_path": f"runs/{sid}/functional_topology.json",
                "summary": summary}

    # ---------------------------------------------------------- operator API
    @app.post("/api/sessions")
    def create_session(req: CreateSession):
        if not req.app_id and not req.live_url:
            raise HTTPException(400, detail="app_id or live_url required")
        try:
            budget = req.budget.model_dump() if req.budget else None
            if req.live_url:
                from .orchestrator.apps import make_live_spec_for_url
                try:
                    spec = make_live_spec_for_url(req.live_url)
                except ValueError as e:
                    raise HTTPException(400, detail=str(e))
                sess = manager.create_spec(spec, budget)
            else:
                sess = manager.create(req.app_id, budget)
        except HTTPException:
            raise
        except KeyError as e:
            raise HTTPException(404, detail=str(e))
        except Exception as e:
            raise HTTPException(503, detail=f"runtime_start_failed: {e}")
        return {"session_id": sess.id, "brief": sess.spec.brief,
                "app_id": sess.spec.app_id,
                "platform": getattr(sess.spec, "platform", "web")}

    @app.get("/api/sessions")
    def list_sessions():
        return manager.list()

    @app.get("/api/apps")
    def list_apps_route(platform: str = "web"):
        try:
            return manager.apps(platform)
        except ValueError as exc:
            raise HTTPException(400, detail=str(exc))

    @app.get("/api/sessions/{sid}")
    def session_status(sid: str):
        return _session(sid).status_dict()

    @app.post("/api/sessions/{sid}/reset")
    def reset_session(sid: str):
        _session(sid)
        manager.reset(sid)
        return {"ok": True}

    @app.post("/api/sessions/{sid}/close")
    def close_session(sid: str):
        _session(sid).close()
        return {"ok": True}

    @app.get("/api/sessions/{sid}/frames/{frame_id}.png")
    def frame(sid: str, frame_id: int):
        p = _session(sid).recorder.frame_path(frame_id)
        if not p.exists():
            raise HTTPException(404, detail="frame_not_found")
        return FileResponse(p, media_type="image/png")

    @app.get("/api/sessions/{sid}/live.png")
    def live(sid: str):
        sess = _session(sid)
        fid = sess.status_dict().get("current_frame")
        p = sess.recorder.frame_path(fid) if fid is not None else None
        if p is None or not p.exists():
            raise HTTPException(404, detail="no_frame_yet")
        return FileResponse(p, media_type="image/png",
                            headers={"Cache-Control": "no-store"})

    @app.get("/api/sessions/{sid}/trace")
    def trace(sid: str):
        sess = _session(sid)
        return {"actions": sess.recorder.read_actions(),
                "observations": sess.recorder.read_observations()}

    @app.get("/api/sessions/{sid}/topology")
    def topology(sid: str):
        return _session(sid).store.graph().model_dump()

    @app.get("/api/sessions/{sid}/hypotheses")
    def hypotheses(sid: str):
        return [h.model_dump() for h in _session(sid).store.hypotheses()]

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
