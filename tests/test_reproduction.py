"""Public material library and fixed-output workflow tests."""
from __future__ import annotations

import hashlib
import importlib.util
import json
import shutil
import sqlite3
import subprocess
import sys
import time
import uuid
import wave
from datetime import datetime, timedelta, timezone
from pathlib import Path

from fastapi.testclient import TestClient
from PIL import Image
import pytest

from reproduction.materials.build import ensure_materials
from reproduction import workspace as reproduction_workspace
from reproduction.filelock import lock_for
from agents.cli_explorer.reproduction_review import ManagedReproductionReview
from benchmark.runtime.base import RuntimeInfo

ROOT = Path(__file__).resolve().parent.parent
BACKEND = ROOT / "reproduction" / "materials" / "backend"


def test_filelock_excludes_concurrent_processes(tmp_path, monkeypatch):
    """The reproduction build lock must be a real cross-process mutex."""
    monkeypatch.setenv("BBB_LOCK_DIR", str(tmp_path))

    holder_script = """
import os, sys, time
sys.path.insert(0, {root!r})
os.environ["BBB_LOCK_DIR"] = {lock_dir!r}
from reproduction.filelock import lock_for
with lock_for("resource-x", timeout=30):
    open({marker!r}, "w").write("held")
    time.sleep(3)
"""
    marker = tmp_path / "held.marker"
    proc = subprocess.Popen(
        [sys.executable, "-c", holder_script.format(
            root=str(ROOT), lock_dir=str(tmp_path), marker=str(marker))],
        stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    try:
        deadline = time.monotonic() + 20
        while not marker.is_file() and time.monotonic() < deadline:
            time.sleep(0.05)
        assert marker.is_file()
        # the child holds the lock → a second acquire must block, not enter
        started = time.monotonic()
        acquired = False
        with pytest.raises(TimeoutError):
            with lock_for("resource-x", timeout=1.0, poll=0.05):
                acquired = True
        assert not acquired
        assert time.monotonic() - started >= 0.9
    finally:
        proc.wait(timeout=30)
    # after the child exits, the lock is free again (the OS released it)
    with lock_for("resource-x", timeout=5.0):
        pass


def test_material_pack_is_complete_and_self_consistent():
    pack = ensure_materials()
    manifest = json.loads((pack / "manifest.json").read_text(encoding="utf-8"))
    files = manifest["files"]
    assert len(files) == 32  # excludes manifest itself
    assert sum(item["path"].endswith(".png") for item in files) == 25
    assert sum(item["path"].endswith(".wav") for item in files) == 3
    assert sum(item["path"].endswith(".mp4") for item in files) == 2
    for item in files:
        path = pack / item["path"]
        assert path.stat().st_size == item["size"]
        assert hashlib.sha256(path.read_bytes()).hexdigest() == item["sha256"]

    with Image.open(pack / "images/products/camera.png") as image:
        assert image.size == (640, 480)
    with wave.open(str(pack / "audio/notification.wav")) as audio:
        assert audio.getnchannels() == 1
        assert audio.getframerate() == 22050
    assert (pack / "videos/product_demo.mp4").read_bytes()[4:8] == b"ftyp"

    with sqlite3.connect(pack / "library.db") as db:
        assert db.execute("SELECT COUNT(*) FROM users").fetchone()[0] == 6
        assert db.execute("SELECT COUNT(*) FROM products").fetchone()[0] == 8
        assert db.execute("SELECT COUNT(*) FROM articles").fetchone()[0] == 5
        assert db.execute("SELECT COUNT(*) FROM media").fetchone()[0] == 5


def _prepare_output(path: Path) -> tuple[Path, str]:
    pack = ensure_materials()
    (path / "data").mkdir(parents=True)
    (path / "seed").mkdir()
    (path / "public/assets").mkdir(parents=True)
    shutil.copy2(pack / "library.db", path / "data/library.db")
    shutil.copy2(pack / "library.db", path / "seed/library.db")
    for directory in ("images", "audio", "videos"):
        shutil.copytree(pack / directory, path / "public/assets" / directory)
    shutil.copytree(BACKEND, path / "backend")
    source_hash = hashlib.sha256((pack / "library.db").read_bytes()).hexdigest()
    return path / "backend/server.py", source_hash


def _load_backend(server_path: Path):
    name = f"output_backend_{uuid.uuid4().hex}"
    spec = importlib.util.spec_from_file_location(name, server_path)
    module = importlib.util.module_from_spec(spec)
    assert spec and spec.loader
    spec.loader.exec_module(module)
    return module


def test_agent_can_copy_materials_to_output_without_polluting_source(tmp_path):
    pack = ensure_materials()
    server_path, source_hash = _prepare_output(tmp_path / "website_output")
    backend = _load_backend(server_path)
    with TestClient(backend.app) as client:
        assert client.get("/api/health").json()["ok"] is True
        assert len(client.get("/api/products").json()) == 8
        assert len(client.get("/api/articles").json()) == 5
        assert len(client.get("/api/posts").json()) == 6

        login = client.post("/api/auth/login", json={
            "username": "linxi", "password": "demo123"})
        headers = {"Authorization": f"Bearer {login.json()['token']}"}
        client.post("/api/cart", headers=headers,
                    json={"product_id": 101, "quantity": 2})
        assert client.post("/api/checkout", headers=headers).status_code == 200
        assert client.post("/api/posts", headers=headers,
                           json={"content": "输出目录里的新动态"}).status_code == 200

    # Runtime writes changed only the copied DB, never the public source DB.
    assert hashlib.sha256((pack / "library.db").read_bytes()).hexdigest() == source_hash
    assert hashlib.sha256(
        (tmp_path / "website_output/data/library.db").read_bytes()).hexdigest() != source_hash


def test_reproduction_handoff_mounts_only_declared_inputs(monkeypatch, tmp_path):
    topology = tmp_path / "functional_topology.json"
    topology.write_text('{"states":[]}', encoding="utf-8")
    materials = tmp_path / "materials"
    materials.mkdir()
    image_context = tmp_path / "agent_image"
    image_context.mkdir()
    output_root = tmp_path / "website_output"
    screenshot = tmp_path / "frame_000001.png"
    screenshot.write_bytes(b"visible pixels")
    calls = []

    monkeypatch.setattr(reproduction_workspace, "MATERIALS_ROOT", materials)
    monkeypatch.setattr(reproduction_workspace, "IMAGE_CONTEXT", image_context)
    monkeypatch.setattr(reproduction_workspace, "OUTPUT_ROOT", output_root)
    monkeypatch.setattr(reproduction_workspace, "ensure_materials", lambda: materials)

    def fake_docker(*args, **kwargs):
        calls.append(args)
        return subprocess.CompletedProcess(args, 0, "ok", "")

    monkeypatch.setattr(reproduction_workspace, "_docker", fake_docker)
    env = reproduction_workspace.ReproductionWorkspace.start(
        source_mode="managed-tools", source_id="sess_20260825_120000_abcd1234",
        topology_path=topology,
        exploration_files={"screenshots/frame_000001.png": screenshot})

    run = next(args for args in calls if args and args[0] == "run")
    command = "\n".join(run)
    assert "--network\nnone" in command
    assert "--read-only" in run and "--cap-drop" in run
    assert f"source={env.output_dir},target=/workspace" in command
    assert f"source={materials.resolve()},target=/materials,readonly" in command
    assert f"source={topology.resolve()},target=/input/functional_topology.json,readonly" in command
    assert f"source={env.artifact_dir},target=/exploration,readonly" in command
    assert env.artifact_dir is not None
    artifact_dir = env.artifact_dir
    manifest = json.loads((artifact_dir / "manifest.json").read_text(encoding="utf-8"))
    assert manifest["read_only"] is True
    assert manifest["items"] == [{
        "path": "screenshots/frame_000001.png", "bytes": len(b"visible pixels")}]
    assert (artifact_dir / "screenshots/frame_000001.png").read_bytes() == b"visible pixels"
    assert str(ROOT / "benchmark") not in command
    assert env.output_dir.parent == output_root.resolve()

    env.write_file("index.html", "<!doctype html><title>ok</title>")
    result = env.finish(review_summary={
        "schema_version": 1, "accepted": True, "rounds": [{"round": 1}]})
    assert result["finished"] and result["files"] == 2
    summary = json.loads((env.output_dir / ".blackboxbench" /
                          "review_summary.json").read_text(encoding="utf-8"))
    assert summary["accepted"] is True
    assert not artifact_dir.exists()
    assert any(args[:2] == ("rm", "-f") for args in calls)


def test_self_built_handoff_has_no_required_topology(monkeypatch, tmp_path):
    materials = tmp_path / "materials"
    materials.mkdir()
    image_context = tmp_path / "agent_image"
    image_context.mkdir()
    output_root = tmp_path / "website_output"
    calls = []

    monkeypatch.setattr(reproduction_workspace, "MATERIALS_ROOT", materials)
    monkeypatch.setattr(reproduction_workspace, "IMAGE_CONTEXT", image_context)
    monkeypatch.setattr(reproduction_workspace, "OUTPUT_ROOT", output_root)
    monkeypatch.setattr(reproduction_workspace, "ensure_materials", lambda: materials)

    def fake_docker(*args, **kwargs):
        calls.append(args)
        return subprocess.CompletedProcess(args, 0, "ok", "")

    monkeypatch.setattr(reproduction_workspace, "_docker", fake_docker)
    env = reproduction_workspace.ReproductionWorkspace.start(
        source_mode="self-built-tools", source_id="self_20260825_120000_abcd1234",
        topology_path=None, exploration_files={})

    run = next(args for args in calls if args and args[0] == "run")
    command = "\n".join(run)
    assert "/input/functional_topology.json" not in command
    assert f"source={materials.resolve()},target=/materials,readonly" in command
    assert f"source={env.artifact_dir},target=/exploration,readonly" in command
    assert "topology" not in env.started_payload()["sandbox"]
    env.close()


def test_managed_handoff_still_requires_topology():
    with pytest.raises(FileNotFoundError, match="managed exploration"):
        reproduction_workspace.ReproductionWorkspace.start(
            source_mode="managed-tools", source_id="sess_test",
            topology_path=None)


def test_docker_preflight_times_out_before_creating_output(monkeypatch, tmp_path):
    topology = tmp_path / "functional_topology.json"
    topology.write_text('{"states":[]}', encoding="utf-8")
    output_root = tmp_path / "website_output"
    monkeypatch.setattr(reproduction_workspace, "OUTPUT_ROOT", output_root)

    def unavailable(*args, **kwargs):
        assert args[:2] == ("version", "--format")
        raise subprocess.TimeoutExpired("docker version", kwargs["timeout"])

    monkeypatch.setattr(reproduction_workspace, "_docker", unavailable)
    with pytest.raises(reproduction_workspace.DockerUnavailableError,
                       match="did not respond within"):
        reproduction_workspace.ReproductionWorkspace.start(
            source_mode="managed-tools", source_id="sess_test",
            topology_path=topology)
    assert not output_root.exists()


def test_prune_stale_containers_sweeps_leftovers_and_zombies(monkeypatch):
    """Residue sweep: created/exited leftovers and long-dead running
    zombies are removed; a freshly started container (a concurrent
    reproduction) is untouched."""
    calls: list[tuple] = []

    def fake_docker(*args, **kwargs):
        calls.append(args)
        if args[:2] == ("ps", "-aq"):
            status = next(a for a in args if a.startswith("status="))
            body = ("c-created\n" if status.endswith("created")
                    else "c-exited\n")
        elif args[:2] == ("ps", "-q"):
            body = "c-fresh\nc-zombie\n"
        elif args[:2] == ("inspect", "--format"):
            now = datetime.now(timezone.utc)
            fresh = now.strftime("%Y-%m-%dT%H:%M:%S")
            old = (now - timedelta(hours=48)).strftime("%Y-%m-%dT%H:%M:%S")
            body = f"c-fresh {fresh}\nc-zombie {old}\n"
        elif args[0] == "rm":
            body = ""
        else:
            raise AssertionError(f"unexpected docker call: {args}")
        return subprocess.CompletedProcess(args, 0, stdout=body, stderr="")

    monkeypatch.setattr(reproduction_workspace, "_docker", fake_docker)
    removed = reproduction_workspace.prune_stale_containers("bbb-repro-")
    assert sorted(removed) == ["c-created", "c-exited", "c-zombie"]
    rm_targets = [a for call in calls if call[0] == "rm" for a in call[3:]]
    assert "c-created" in rm_targets
    assert "c-exited" in rm_targets
    assert "c-zombie" in rm_targets
    assert "c-fresh" not in rm_targets


def test_prune_stale_containers_never_raises(monkeypatch):
    """Cleanup must never block a new handoff: a dead daemon just leaves
    the residue for a later sweep."""
    def broken(*args, **kwargs):
        raise subprocess.TimeoutExpired("docker", kwargs.get("timeout", 60))

    monkeypatch.setattr(reproduction_workspace, "_docker", broken)
    assert reproduction_workspace.prune_stale_containers("bbb-repro-") == []


def test_exploration_artifact_bundle_rejects_escape_and_symlink(tmp_path):
    source = tmp_path / "frame.png"
    source.write_bytes(b"pixels")
    with pytest.raises(ValueError):
        reproduction_workspace._artifact_bundle(
            "handoff", "managed-tools", {"../escape.png": source})
    link = tmp_path / "link.png"
    try:
        link.symlink_to(source)
    except OSError:
        pytest.skip("symlink creation is unavailable")
    with pytest.raises(ValueError, match="symlinks"):
        reproduction_workspace._artifact_bundle(
            "handoff", "self-built-tools", {"evidence/link.png": link})


def test_reproduction_output_io_rejects_escape_and_symlinks(monkeypatch, tmp_path):
    output = tmp_path / "output"
    output.mkdir()
    env = reproduction_workspace.ReproductionWorkspace(
        "handoff", "self-built-tools", "self_test",
        tmp_path / "topology.json", output, "container")
    with pytest.raises(ValueError):
        env.write_file("../outside.txt", "bad")
    env.write_file("src/app.js", "console.log('ok')")
    assert env.list_files("src")["entries"][0]["name"] == "app.js"
    assert "console.log" in env.read_file("src/app.js")[0]["text"]


def test_reproduction_patch_file_replaces_unique_text(monkeypatch, tmp_path):
    """web 侧同款搜索-替换补丁（避免整文件重写的大工具调用）。"""
    output = tmp_path / "output"
    output.mkdir()
    env = reproduction_workspace.ReproductionWorkspace(
        "handoff", "self-built-tools", "self_test",
        tmp_path / "topology.json", output, "container")
    env.write_file("src/app.js", "const a = 1;\nconst b = 2;\n")
    with pytest.raises(ValueError, match="not found"):
        env.patch_file("src/app.js", "const c", "x")
    result = env.patch_file("src/app.js", "const a = 1;", "const a = 10;")
    assert result["patched"] == "src/app.js"
    assert result["line"] == 1
    assert "const a = 10;" in env.read_file("src/app.js")[0]["text"]


def test_reproduction_finish_rejects_container_created_symlink(monkeypatch, tmp_path):
    output = tmp_path / "output"
    output.mkdir()
    outside = tmp_path / "outside.txt"
    outside.write_text("host data", encoding="utf-8")
    try:
        (output / "escape").symlink_to(outside)
    except OSError:
        pytest.skip("symlink creation is unavailable")
    env = reproduction_workspace.ReproductionWorkspace(
        "handoff", "managed-tools", "sess_test",
        tmp_path / "topology.json", output, "container")
    with pytest.raises(ValueError, match="symlinks"):
        env.finish()


class _FakeReviewRuntime:
    def __init__(self):
        self.started = False
        self.counter = 0

    def start(self):
        self.started = True

    def stop(self):
        self.started = False

    def screenshot(self):
        self.counter += 1
        return b"visible-png-" + str(self.counter).encode("ascii")

    def info(self):
        return RuntimeInfo(width=1440, height=900, device_scale_factor=1.0)

    def list_tabs(self):
        return {"count": 1, "active": 0}

    def click(self, *args, **kwargs):
        pass

    def mouse_move(self, *args, **kwargs):
        pass

    def mouse_down(self, *args, **kwargs):
        pass

    def mouse_up(self, *args, **kwargs):
        pass

    def scroll(self, *args, **kwargs):
        pass

    def type_text(self, *args, **kwargs):
        pass

    def key(self, *args, **kwargs):
        pass

    def switch_tab(self, *args, **kwargs):
        pass

    def close_tab(self):
        pass


def test_managed_reproduction_requires_visible_review_and_allows_revisions(tmp_path):
    output = tmp_path / "website_output" / "handoff"
    output.mkdir(parents=True)
    page = output / "index.html"
    page.write_text("<!doctype html><button>first</button>", encoding="utf-8")
    runtimes = []

    def runtime_factory(_preview_root, _work_dir):
        runtime = _FakeReviewRuntime()
        runtimes.append(runtime)
        return runtime

    review = ManagedReproductionReview(
        output, "handoff", max_revisions=2, runtime_factory=runtime_factory)
    png, meta = review.start_round()
    assert png.startswith(b"visible-png") and meta["pixels_only"] is True
    review.action("click", {"x": 20, "y": 30})
    review.action("type_text", {"text": "test"})
    review.action("scroll", {"dx": 0, "dy": 400})
    review.observe()
    result = review.complete_round(
        decision="revise", checked_flows=["main interaction"],
        findings=["button state needs correction"])
    assert result["decision"] == "revise" and result["revision_count"] == 1
    with pytest.raises(ValueError, match="output did not change"):
        review.start_round()

    page.write_text("<!doctype html><button>fixed</button>", encoding="utf-8")
    review.start_round()
    review.action("click", {"x": 20, "y": 30})
    review.action("key_press", {"key": "Tab"})
    review.action("scroll", {"dx": 0, "dy": 400})
    review.observe()
    result = review.complete_round(
        decision="accept", checked_flows=["main interaction", "keyboard"],
        findings=["no remaining visible defect and no private data"])
    assert result["decision"] == "accept"
    review.ensure_accepted()
    assert review.summary()["revision_count"] == 1
    assert len(review.summary()["rounds"]) == 2
    assert all(not runtime.started for runtime in runtimes)

    page.write_text("<!doctype html><button>changed again</button>", encoding="utf-8")
    with pytest.raises(ValueError, match="changed after acceptance"):
        review.ensure_accepted()
