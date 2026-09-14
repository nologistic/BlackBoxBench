"""Shared fixtures: register the deterministic mini test app and provide
controller clients. Tests spawn real browser+app subprocesses; keep one
session per module to bound runtime."""
from __future__ import annotations

import os
import shutil
import sys
import tempfile
from pathlib import Path

import pytest

# Set all mutable benchmark roots before importing benchmark.config.  Pytest's
# normal temp fixture starts too late because module imports initialize the
# global Controller manager.  _TEST_ROOT is placed directly under the
# workspace tmp/ (sibling of the repo) rather than relying on the TMP
# redirection inside benchmark.config: BBB_* env vars must be set before that
# import (config.py reads them at import time), so importing config first is
# not an option.  It used to land in the system temp and leak there on every
# aborted session (32 leftovers found 2026-09-15).
_REPO = Path(__file__).resolve().parent.parent
_TEST_ROOT = Path(tempfile.mkdtemp(
    prefix="blackboxbench-tests-", dir=str(_REPO.parent / "tmp")))
os.environ.setdefault("BBB_RUNS_DIR", str(_TEST_ROOT / "runs"))
os.environ.setdefault("BBB_APP_OUTPUT_DIR", str(_TEST_ROOT / "app_output"))
os.environ.setdefault("BBB_ANDROID_TARGETS_DIR", str(_TEST_ROOT / "android_targets"))

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from benchmark.orchestrator import apps
from benchmark.orchestrator.apps import AppSpec

apps._REGISTRY.setdefault("miniapp", AppSpec(
    app_id="miniapp", description="deterministic test app",
    module="tests.fixtures.miniapp"))

SMALL_BUDGET = {"max_actions": 60, "max_duration_s": 600,
                "max_observations": 200}


@pytest.fixture(scope="session", autouse=True)
def isolated_test_runs(tmp_path_factory):
    """Never let tests create benchmark sessions in the real runs/ folder."""
    from benchmark.server import manager

    original_runs_dir = manager.runs_dir
    manager.runs_dir = tmp_path_factory.mktemp("blackboxbench-runs")
    yield
    manager.shutdown_all()
    manager._sessions.clear()
    manager._runtimes.clear()
    manager.runs_dir = original_runs_dir
    shutil.rmtree(_TEST_ROOT, ignore_errors=True)


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
