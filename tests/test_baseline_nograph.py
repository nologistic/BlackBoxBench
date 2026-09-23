"""baseline-nograph condition: exploration-only tool surface.

The condition is a deliberate ablation of baseline: every discovery-record
tool must be absent from the MCP surface, while the exploration and
reproduction tools stay identical. These tests pin that contract so a later
refactor cannot silently reintroduce recording requirements — nor leak the
ablation into baseline itself.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

RECORD_TOOLS = {
    "record_state", "record_feature", "record_data", "record_edge",
    "record_hypothesis", "resolve_hypothesis", "revise",
}
EXPLORE_TOOLS = {
    "observe", "tap", "long_press", "swipe", "type_text",
    "press_back", "press_enter", "restart_app", "wait",
    "list_targets", "start_session", "finalize",
}


def _nograph_tools() -> dict:
    import agents.baseline_nograph.mcp_server as m
    return m._TOOLS


def _baseline_tools() -> dict:
    import agents.android_baseline.mcp_server as m
    return m._TOOLS


def test_no_record_tools_registered():
    leaked = RECORD_TOOLS & set(_nograph_tools())
    assert not leaked, f"nograph must not expose record tools: {sorted(leaked)}"


def test_exploration_surface_intact():
    missing = EXPLORE_TOOLS - set(_nograph_tools())
    assert not missing, f"nograph lost exploration tools: {sorted(missing)}"


def test_reproduction_surface_intact():
    tools = _nograph_tools()
    for name in ("workspace_write", "workspace_run",
                 "start_reproduction_review", "review_observe",
                 "finish_reproduction", "input_list", "input_read"):
        assert name in tools, f"nograph lost reproduction tool: {name}"


def test_baseline_still_has_record_tools():
    """Guard against the ablation leaking into baseline itself."""
    missing = RECORD_TOOLS - set(_baseline_tools())
    assert not missing, f"baseline lost record tools: {sorted(missing)}"


def test_only_difference_is_record_tools():
    """The condition must differ from baseline by exactly the record tools."""
    diff = set(_baseline_tools()) - set(_nograph_tools())
    assert diff == RECORD_TOOLS, (
        f"unexpected extra differences from baseline: {sorted(diff - RECORD_TOOLS)}")
    assert not (set(_nograph_tools()) - set(_baseline_tools())), (
        "nograph must add no tools beyond baseline")
