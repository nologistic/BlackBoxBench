"""Integration tests for the independent Android ``app-review`` condition.

The real Emulator Runtime is replaced with a pixels-only fake so these tests
exercise the MCP/checklist/report boundary without booting an AVD or touching
formal ``runs/`` data.
"""
from __future__ import annotations

import io
import json
from pathlib import Path

from PIL import Image
import pytest

from app_evaluation.session import AppEvaluationSession
from benchmark.runtime.base import RuntimeInfo


def _png(index: int) -> bytes:
    output = io.BytesIO()
    Image.new("RGB", (240, 480), (index * 30 % 255, 40, 60)).save(
        output, "PNG")
    return output.getvalue()


class _EvaluationRuntime:
    instances: list["_EvaluationRuntime"] = []

    def __init__(self, *_args):
        self.started = False
        self.observations = 0
        self.calls: list[tuple] = []
        self.__class__.instances.append(self)

    def start(self):
        self.started = True

    def stop(self):
        self.started = False

    def info(self):
        return RuntimeInfo(240, 480, 1.0, "android", "portrait", 420)

    def screenshot(self):
        self.observations += 1
        return _png(self.observations)

    def tap(self, *args):
        self.calls.append(("tap", args))

    def long_press(self, *args):
        self.calls.append(("long_press", args))

    def swipe(self, *args):
        self.calls.append(("swipe", args))

    def type_text(self, *args):
        self.calls.append(("type_text", args))

    def key(self, *args):
        self.calls.append(("key", args))

    def restart_app(self):
        self.calls.append(("restart_app", ()))

    def reset(self):
        self.calls.append(("reset", ()))


@pytest.fixture
def review_mcp(monkeypatch, tmp_path):
    from agents.app_review import mcp_server as module

    app_output = tmp_path / "app_output"
    review_specs = tmp_path / "review_specs"
    apk = app_output / "handoff" / "artifacts" / "app-debug.apk"
    apk.parent.mkdir(parents=True)
    apk.write_bytes(b"test-apk")
    review_specs.mkdir()
    (review_specs / "demo.json").write_text(json.dumps({
        "checklist_id": "demo",
        "platform": "android",
        "features": [{
            "id": "open_home", "name": "Open home",
            "steps": ["tap entry"], "expected": "home is visible",
        }],
    }), encoding="utf-8")

    monkeypatch.setattr(module, "APP_OUTPUT_ROOT", app_output.resolve())
    monkeypatch.setattr(module, "REVIEW_SPECS_ROOT", review_specs.resolve())
    _EvaluationRuntime.instances.clear()

    def make_session(**kwargs):
        return AppEvaluationSession(
            **kwargs, output_root=app_output / "evaluations",
            runtime_factory=lambda spec, directory: _EvaluationRuntime(
                spec, directory))

    monkeypatch.setattr(module, "AppEvaluationSession", make_session)
    module._shutdown()
    yield module, app_output, review_specs
    module._shutdown()


def _call(module, request_id: int, name: str, arguments: dict | None = None):
    response = module._handle({
        "jsonrpc": "2.0", "id": request_id, "method": "tools/call",
        "params": {"name": name, "arguments": arguments or {}},
    })
    assert response is not None
    assert "error" not in response
    return response["result"]


def _text_payload(result: dict) -> dict:
    block = next(item for item in result["content"] if item["type"] == "text")
    return json.loads(block["text"])


def test_app_review_mcp_full_interactive_lifecycle(review_mcp):
    module, app_output, _review_specs = review_mcp
    initialized = module._handle({
        "jsonrpc": "2.0", "id": 1, "method": "initialize", "params": {}})
    assert initialized["result"]["serverInfo"]["name"] == "app-review"
    listed = module._handle({
        "jsonrpc": "2.0", "id": 2, "method": "tools/list"})
    names = {item["name"] for item in listed["result"]["tools"]}
    assert {"start_evaluation", "observe", "tap", "record_result",
            "finish_evaluation", "abort_evaluation"} <= names
    assert not ({"adb", "logcat", "selector", "get_dom"} & names)

    started = _call(module, 3, "start_evaluation", {
        "checklist": "demo.json", "handoff_id": "handoff"})
    assert started["content"][0]["type"] == "image"
    assert _text_payload(started)["observation"] == 1
    assert _EvaluationRuntime.instances[-1].started is True

    _call(module, 4, "tap", {"x": 12, "y": 40})
    observed = _call(module, 5, "observe")
    assert _text_payload(observed)["observation"] == 2
    recorded = _call(module, 6, "record_result", {
        "requirement_id": "open_home", "grade": "full",
        "rationale": "The visible home appeared after tapping the entry.",
        "evidence_observations": [1, 2],
    })
    assert _text_payload(recorded)["remaining"] == []
    finished = _text_payload(_call(module, 7, "finish_evaluation"))

    report = Path(finished["report"])
    assert report.is_file() and app_output.resolve() in report.resolve().parents
    payload = json.loads(report.read_text(encoding="utf-8"))
    assert payload["counts"]["full"] == 1
    assert payload["actions"] == [{"type": "tap"}]
    assert module._session is None
    assert _EvaluationRuntime.instances[-1].started is False


def test_app_review_rejects_paths_outside_whitelisted_roots(review_mcp,
                                                             tmp_path):
    module, _app_output, _review_specs = review_mcp
    outside_apk = tmp_path / "outside.apk"
    outside_apk.write_bytes(b"outside")
    outside_checklist = tmp_path / "outside.json"
    outside_checklist.write_text("{}", encoding="utf-8")

    with pytest.raises(ValueError, match="must stay inside app_output"):
        module._resolve_apk(str(outside_apk))
    with pytest.raises(ValueError, match="must stay inside review_specs"):
        module._resolve_checklist(str(outside_checklist))
    with pytest.raises(ValueError, match="must stay inside app_output"):
        module._resolve_apk("../outside")
    assert module._resolve_apk("handoff").name == "app-debug.apk"
    assert module._resolve_checklist("demo.json").checklist_id == "demo"


def test_app_review_stream_auth_and_disconnect_cleanup(review_mcp):
    module, _app_output, _review_specs = review_mcp
    token = "review-token-0123456789"
    replies: list[dict] = []
    valid_messages = [
        json.dumps({"jsonrpc": "2.0", "id": "auth", "method": "auth",
                    "params": {"token": token}}),
        json.dumps({"jsonrpc": "2.0", "id": 1, "method": "tools/call",
                    "params": {"name": "start_evaluation", "arguments": {
                        "checklist": "demo.json", "handoff_id": "handoff"}}}),
    ]
    module._serve_stream(valid_messages, replies.append, token)
    assert replies[0]["result"] == {"ok": True}
    assert replies[1]["result"]["isError"] is False
    # EOF is a normal MCP disconnect and must always tear down the emulator.
    assert module._session is None
    assert _EvaluationRuntime.instances[-1].started is False

    rejected: list[dict] = []
    module._serve_stream([
        json.dumps({"jsonrpc": "2.0", "id": "auth", "method": "auth",
                    "params": {"token": "wrong-token-0123456789"}}),
        json.dumps({"jsonrpc": "2.0", "id": 2, "method": "tools/list"}),
    ], rejected.append, token)
    assert len(rejected) == 1
    assert rejected[0]["error"]["message"] == "unauthorized"
