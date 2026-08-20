"""Shared fixtures: register the deterministic mini test app and provide
controller clients. Tests spawn real browser+app subprocesses; keep one
session per module to bound runtime."""
from __future__ import annotations

import sys
from pathlib import Path

import pytest

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from benchmark.orchestrator import apps
from benchmark.orchestrator.apps import AppSpec

apps._REGISTRY.setdefault("miniapp", AppSpec(
    app_id="miniapp", description="deterministic test app",
    module="tests.fixtures.miniapp"))

SMALL_BUDGET = {"max_actions": 60, "max_duration_s": 600,
                "max_observations": 200}


@pytest.fixture(scope="module")
def client():
    from fastapi.testclient import TestClient
    from benchmark.server import app
    with TestClient(app) as c:
        yield c


@pytest.fixture(scope="module")
def mini_session(client):
    r = client.post("/api/sessions", json={"app_id": "miniapp",
                                           "budget": SMALL_BUDGET})
    assert r.status_code == 200, r.text
    sid = r.json()["session_id"]
    yield sid
    client.post(f"/api/sessions/{sid}/close")


@pytest.fixture(scope="module")
def manager():
    from benchmark.server import manager
    return manager
