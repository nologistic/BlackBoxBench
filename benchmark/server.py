"""FastAPI controller server: agent channel + minimal operator API.

Route families (see docs/api_contract.md):
- /agent/{sid}/...   the ONLY agent-reachable surface; whitelist response schemas
- /api/...           session lifecycle and live inspection
"""
from __future__ import annotations

import time
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException
from fastapi.responses import FileResponse, JSONResponse
from pydantic import BaseModel, ConfigDict, Field

from . import config
from .orchestrator.manager import SessionManager
from .orchestrator.session import (
    BudgetExhausted, SessionClosed, TransientEnvironmentError)
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


def _prune_empty_session_dirs(min_age_s: float = 1800.0) -> int:
    """Remove leftover empty session directories from bootstrap storms.

    When many MCPs race one cold Controller, session creation can fail
    repeatedly and leave orphan sess_* dirs that were never bound: no
    app_id, no frames, no discovery. On 2026-09-17 a 12-task launch left
    217 such dirs. Remove only dirs that are empty AND older than
    min_age_s; anything recent or with any content is left alone so live
    runs are never touched.
    """
    import json
    import shutil
    import time

    cutoff = time.time() - min_age_s
    removed = 0
    for sess_dir in sorted(config.RUNS_DIR.glob("sess_*")):
        meta = sess_dir / "session.json"
        if not meta.is_file():
            # Never bound to a session (crash between mkdir and the first
            # write): an EMPTY stale dir is a shell worth removing; anything
            # with content stays — it may belong to a live create race.
            try:
                if (any(sess_dir.iterdir())
                        or sess_dir.stat().st_mtime > cutoff):
                    continue
                shutil.rmtree(sess_dir, ignore_errors=True)
                removed += 1
            except OSError:
                continue
            continue
        try:
            data = json.loads(meta.read_text(encoding="utf-8"))
        except (OSError, ValueError):
            continue
        if str(data.get("app_id") or data.get("target") or ""):
            continue
        frames = sess_dir / "frames"
        try:
            if frames.is_dir() and any(frames.iterdir()):
                continue
        except OSError:
            continue
        try:
            if meta.stat().st_mtime > cutoff:
                continue
        except OSError:
            continue
        shutil.rmtree(sess_dir, ignore_errors=True)
        removed += 1
    return removed


def create_app() -> FastAPI:
    @asynccontextmanager
    async def lifespan(_app: FastAPI):
        # A force-killed Controller leaves session.json claiming "running" for
        # sessions no API call can reach again. Correct that on-disk state once,
        # at startup, so tooling never trusts a status that is certainly wrong.
        for sid in manager.mark_abandoned_sessions():
            print(f"[controller] session {sid} was abandoned by a previous "
                  f"controller; on-disk status corrected "
                  f"(recoverable if its runtime still answers)")
        pruned = _prune_empty_session_dirs()
        if pruned:
            print(f"[controller] pruned {pruned} empty session dir(s) left "
                  f"by a bootstrap storm")
        yield

    app = FastAPI(title="BlackBoxBench Controller", docs_url=None,
                  redoc_url=None, openapi_url=None, lifespan=lifespan)

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
    def _runtime_failure(sid: str, exc: Exception) -> HTTPException:
        """Report a terminal runtime failure without leaking how it happened.

        The session already froze itself and stopped its runtime; drop the
        manager's handle too so nothing keeps an emulator port or target lease
        bound to a session that can never run again. The message is the neutral
        one the session produced — never a raw adb command line, CDP method or
        host path, which would disclose the machinery behind the pixels-only
        boundary.
        """
        manager.release_runtime(sid)
        return HTTPException(503, detail=str(exc))

    @app.post("/agent/{sid}/observe")
    def observe(sid: str):
        sess = _session(sid)
        try:
            return sess.observe()
        except (BudgetExhausted, SessionClosed):
            raise
        except TransientEnvironmentError as e:
            # The environment blipped but is healthy: this observation did not
            # go through, the session continues. 409 keeps it distinct from an
            # invalid request (400) and from a dead environment (503). The
            # body carries a server-computed retry hint so the Agent waits a
            # told duration instead of guessing at one.
            raise HTTPException(409, detail={
                "message": str(e), "retry_after_s": e.retry_after_s,
                "state": "busy"})
        except Exception as e:
            raise _runtime_failure(sid, e)

    @app.post("/agent/{sid}/action")
    def action(sid: str, action: m.Action):
        sess = _session(sid)
        try:
            return sess.execute(action)
        except ValueError as e:
            # invalid action: a 400 for the agent, the session stays runnable
            raise HTTPException(400, detail=str(e))
        except (BudgetExhausted, SessionClosed):
            raise
        except TransientEnvironmentError as e:
            raise HTTPException(409, detail={
                "message": str(e), "retry_after_s": e.retry_after_s,
                "state": "busy"})
        except Exception as e:
            raise _runtime_failure(sid, e)

    # ---------------------------------------------------------- discovery
    # Every discovery route funnels through here so a refusal is auditable.
    # Without it, "the Agent never tried to record an edge" and "the Agent tried
    # and the platform refused" leave identical traces, and a platform artefact
    # gets mistaken for a limit of the Agent.
    def _discovery(sid: str, op: str, request, handler):
        sess = _session(sid)
        # Discovery writes are agent activity too: a session busy recording
        # nodes must not look stalled.
        sess.last_activity_at = time.time()
        try:
            return handler(sess)
        except (EvidenceError, ValueError, KeyError) as exc:
            try:
                payload = (request.model_dump(mode="json")
                           if hasattr(request, "model_dump") else dict(request))
            except Exception:
                payload = {}
            sess.recorder.log_discovery_rejected(op, str(exc), payload)
            raise

    @app.post("/agent/{sid}/discovery/state")
    def add_state(sid: str, req: m.StateCreate):
        node = _discovery(sid, "create_state", req,
                          lambda s: s.store.add_state(req))
        return {"state_id": node.id}

    @app.post("/agent/{sid}/discovery/feature")
    def add_feature(sid: str, req: m.FeatureCreate):
        node = _discovery(sid, "create_feature", req,
                          lambda s: s.store.add_feature(req))
        return {"feature_id": node.id}

    @app.post("/agent/{sid}/discovery/data")
    def add_data(sid: str, req: m.DataCreate):
        node = _discovery(sid, "create_data", req,
                          lambda s: s.store.add_data(req))
        return {"data_id": node.id}

    @app.post("/agent/{sid}/discovery/edge")
    def add_edge(sid: str, req: m.EdgeCreate):
        edge = _discovery(sid, "create_edge", req,
                          lambda s: s.store.add_edge(req))
        return {"edge_id": edge.id}

    @app.post("/agent/{sid}/discovery/hypothesis")
    def add_hyp(sid: str, req: m.HypothesisCreate):
        hyp = _discovery(sid, "create_hypothesis", req,
                         lambda s: s.store.add_hypothesis(req))
        return {"hypothesis_id": hyp.id}

    @app.post("/agent/{sid}/discovery/hypothesis/{hid}/resolve")
    def resolve_hyp(sid: str, hid: str, req: m.HypothesisResolve):
        try:
            _discovery(sid, "resolve_hypothesis", req,
                       lambda s: s.store.resolve_hypothesis(hid, req))
        except KeyError:
            raise HTTPException(404, detail="hypothesis_not_found")
        return {"ok": True}

    @app.patch("/agent/{sid}/discovery/{kind}/{node_id}")
    def revise(sid: str, kind: str, node_id: str, req: m.RevisionRequest):
        if kind not in ("state", "feature", "data"):
            raise HTTPException(400, detail="invalid_kind")
        try:
            return _discovery(sid, f"revise_{kind}", req,
                              lambda s: s.store.revise(kind, node_id, req))
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

    @app.post("/api/sessions/{sid}/reopen")
    async def reopen_session(sid: str):
        """Reopen a closed session for continued exploration.

        Rebuilds the runtime (emulator/browser) and recorder while
        preserving the session's topology and discovery state. Used by
        agents resuming after provider quota interruptions.
        """
        try:
            session = manager.reopen_session(sid)
            return {"session_id": sid, "app_id": session.spec.app_id,
                    "status": "running"}
        except ValueError as e:
            detail = str(e)
            code = 404 if "no such" in detail else 409
            raise HTTPException(code, detail=detail)

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
