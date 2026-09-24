"""Core data models for the Functional Topology Graph and discovery protocol.

These models are the single source of truth for the discovery schema.
The JSON Schemas in schemas/ mirror these models and must be kept in sync.

Hard rules enforced here (see docs/security_model.md, docs/topology_schema.md):
- confidence is a continuous value in [0, 1]
- evidence references must point at real frames/steps (validated by the store,
  which has session context; models only validate shape)
- free-text fields are screened against implementation-leak patterns
"""
from __future__ import annotations

import re
from enum import Enum
from typing import Annotated, Literal, Optional

from pydantic import BaseModel, Field, field_validator


# ---------------------------------------------------------------- enums

class NodeType(str, Enum):
    STATE = "STATE"
    FEATURE = "FEATURE"
    DATA = "DATA"


class EdgeType(str, Enum):
    REQUIRES = "REQUIRES"
    TRANSITIONS_TO = "TRANSITIONS_TO"
    MUTATES = "MUTATES"
    ENABLES = "ENABLES"
    DISABLES = "DISABLES"
    PERSISTS_TO = "PERSISTS_TO"
    REVEALS = "REVEALS"
    DEPENDS_ON = "DEPENDS_ON"
    CONFLICTS_WITH = "CONFLICTS_WITH"
    VALIDATES = "VALIDATES"


class ClaimStatus(str, Enum):
    HYPOTHESIZED = "hypothesized"
    CONFIRMED = "confirmed"
    REJECTED = "rejected"
    UNCERTAIN = "uncertain"


class HypothesisStatus(str, Enum):
    UNVERIFIED = "unverified"
    CONFIRMED = "confirmed"
    REJECTED = "rejected"
    UNCERTAIN = "uncertain"


class ActionType(str, Enum):
    CLICK = "click"
    DOUBLE_CLICK = "double_click"
    MOVE_POINTER = "move_pointer"
    MOUSE_DOWN = "mouse_down"
    MOUSE_UP = "mouse_up"
    DRAG = "drag"
    TYPE_TEXT = "type_text"
    KEY_PRESS = "key_press"
    KEY_DOWN = "key_down"
    KEY_UP = "key_up"
    SCROLL = "scroll"
    WAIT = "wait"
    # tab-strip control (window-management level; no page semantics exposed)
    SWITCH_TAB = "switch_tab"
    CLOSE_TAB = "close_tab"
    # Mobile-only human input.  They deliberately carry no selector, package,
    # accessibility or device semantics.
    TAP = "tap"
    LONG_PRESS = "long_press"
    SWIPE = "swipe"
    PRESS_BACK = "press_back"
    PRESS_ENTER = "press_enter"
    RESTART_APP = "restart_app"


# ---------------------------------------------------------------- leak screening

LEAK_PATTERN = re.compile(
    r"(/api/|\.tsx?\b|\.jsx\b|localhost|127\.0\.0\.1|0\.0\.0\.0|"
    r"\bSELECT\b|\bINSERT\b|\bUPDATE\s+\w+\s+SET\b|\bDELETE\s+FROM\b|"
    r"React|Vue\.js|\bVue\b|Angular|Svelte|Next\.js|Django|Flask|Rails|Laravel|"
    r"\bDOM\b|querySelector|xpath|css\s*selector|sourcemap|source\s*map|graphql|"
    r"src/[A-Za-z]|node_modules|webpack|vite)",
    re.IGNORECASE,
)


def _check_no_leak(value: Optional[str], field: str) -> Optional[str]:
    if value is not None and LEAK_PATTERN.search(value):
        raise ValueError(
            f"leak_detected in '{field}': text references implementation details "
            "(source paths, API routes, frameworks, selectors). "
            "Describe observable behavior only."
        )
    return value


def _screen_texts(values: list[str]) -> list[str]:
    for v in values:
        _check_no_leak(v, "text")
    return values


Confidence = Annotated[float, Field(ge=0.0, le=1.0)]


# ---------------------------------------------------------------- evidence

class Evidence(BaseModel):
    """One auditable piece of evidence tying a claim to the interaction trace."""

    step: int = Field(ge=0)
    before_frame: int = Field(ge=0)
    # human-readable, e.g. "click(811,432)" — informational only. Optional
    # since 2026-09-16: agents frequently omitted it (15× 422 in one batch)
    # for a field the validator never uses.
    action: Optional[str] = None
    after_frame: int = Field(ge=0)
    note: Optional[str] = None

    @field_validator("action", "note")
    @classmethod
    def _no_leak(cls, v, info):
        return _check_no_leak(v, info.field_name)


# ---------------------------------------------------------------- requests (agent writes)

class TriggerModel(BaseModel):
    type: str  # click / type_text / key_press / ... (free-form target description allowed)
    target_description: str

    @field_validator("target_description")
    @classmethod
    def _no_leak(cls, v):
        return _check_no_leak(v, "trigger.target_description")


class InputModel(BaseModel):
    name: str
    kind: str = "text"  # text / number / choice / ...
    constraints: str = ""

    @field_validator("name", "constraints")
    @classmethod
    def _no_leak(cls, v, info):
        return _check_no_leak(v, info.field_name)


class FeatureBehavior(BaseModel):
    preconditions: list[str] = []
    trigger: Optional[TriggerModel] = None
    inputs: list[InputModel] = []
    postconditions: list[str] = []
    persistent_effects: list[str] = []
    constraints: list[str] = []
    error_cases: list[str] = []

    @field_validator(
        "preconditions", "postconditions", "persistent_effects",
        "constraints", "error_cases",
    )
    @classmethod
    def _no_leak(cls, v):
        return _screen_texts(v)


class StateCreate(BaseModel):
    name: str
    description: str = ""
    visual_evidence: list[int] = []  # frame ids
    entry_conditions: list[str] = []
    observed_elements: list[str] = []
    confidence: Confidence = 0.5

    @field_validator("name", "description")
    @classmethod
    def _no_leak(cls, v, info):
        return _check_no_leak(v, info.field_name)

    @field_validator("entry_conditions", "observed_elements")
    @classmethod
    def _no_leak_list(cls, v):
        return _screen_texts(v)


class FeatureCreate(BaseModel):
    name: str
    description: str = ""
    behavior: FeatureBehavior = FeatureBehavior()
    evidence: list[Evidence] = []
    confidence: Confidence = 0.5
    status: ClaimStatus = ClaimStatus.CONFIRMED

    @field_validator("name", "description")
    @classmethod
    def _no_leak(cls, v, info):
        return _check_no_leak(v, info.field_name)


class DataCreate(BaseModel):
    name: str
    description: str = ""
    confidence: Confidence = 0.5

    @field_validator("name", "description")
    @classmethod
    def _no_leak(cls, v, info):
        return _check_no_leak(v, info.field_name)


class EdgeCreate(BaseModel):
    source: str
    target: str
    type: EdgeType
    evidence: list[Evidence] = []
    confidence: Confidence = 0.5
    status: ClaimStatus = ClaimStatus.CONFIRMED
    note: Optional[str] = None

    @field_validator("note")
    @classmethod
    def _no_leak(cls, v, info):
        return _check_no_leak(v, info.field_name)


class HypothesisCreate(BaseModel):
    statement: str
    evidence: list[Evidence] = []
    confidence: Confidence = 0.5
    next_probe: str = ""

    @field_validator("statement", "next_probe")
    @classmethod
    def _no_leak(cls, v, info):
        return _check_no_leak(v, info.field_name)


class HypothesisResolve(BaseModel):
    status: Literal["confirmed", "rejected", "uncertain"]
    evidence: list[Evidence] = []
    note: Optional[str] = None

    @field_validator("note")
    @classmethod
    def _no_leak(cls, v, info):
        return _check_no_leak(v, info.field_name)


class RevisionRequest(BaseModel):
    """Revision ops for nodes: update fields / merge into another node / delete."""

    op: Literal["update", "merge", "delete"]
    fields: Optional[dict] = None   # op=update
    into: Optional[str] = None      # op=merge: surviving node id
    reason: Optional[str] = None    # op=delete


# ---------------------------------------------------------------- graph nodes/edges (stored)

class StateNode(StateCreate):
    id: str
    type: Literal[NodeType.STATE] = NodeType.STATE
    status: ClaimStatus = ClaimStatus.CONFIRMED
    created_step: int = 0
    updated_step: int = 0


class FeatureNode(FeatureCreate):
    id: str
    type: Literal[NodeType.FEATURE] = NodeType.FEATURE
    created_step: int = 0
    updated_step: int = 0


class DataNode(DataCreate):
    id: str
    type: Literal[NodeType.DATA] = NodeType.DATA
    status: ClaimStatus = ClaimStatus.CONFIRMED
    created_step: int = 0
    updated_step: int = 0


GraphNode = StateNode | FeatureNode | DataNode


class Edge(EdgeCreate):
    id: str
    created_step: int = 0


class Hypothesis(BaseModel):
    id: str
    statement: str
    evidence: list[Evidence] = []
    confidence: Confidence = 0.5
    next_probe: str = ""
    status: HypothesisStatus = HypothesisStatus.UNVERIFIED
    note: Optional[str] = None
    created_step: int = 0
    resolved_step: Optional[int] = None


# ---------------------------------------------------------------- topology document

class CoverageSummary(BaseModel):
    states: int = 0
    features: int = 0
    data_entities: int = 0
    edges: int = 0
    confirmed_ratio: float = 0.0
    actions_used: int = 0


class TopologyGraph(BaseModel):
    app_id: str
    graph_version: str = "1.0"
    session_id: str
    generated_at: str = ""
    nodes: list[GraphNode] = []
    edges: list[Edge] = []
    unresolved_questions: list[Hypothesis] = []
    coverage_summary: CoverageSummary = CoverageSummary()


# ---------------------------------------------------------------- actions / observations (trace)

class Action(BaseModel):
    type: ActionType
    x: Optional[int] = None
    y: Optional[int] = None
    x1: Optional[int] = None
    y1: Optional[int] = None
    x2: Optional[int] = None
    y2: Optional[int] = None
    duration_ms: Optional[int] = None
    text: Optional[str] = None
    key: Optional[str] = None
    dx: Optional[int] = None
    dy: Optional[int] = None
    ms: Optional[int] = None
    tab_index: Optional[int] = None   # switch_tab: 0-based, in tab-open order


class ActionRecord(BaseModel):
    step: int
    timestamp: str
    action: Action
    accepted: bool
    error: Optional[str] = None
    duration_ms: float = 0.0
    before_frame: Optional[int] = None
    after_frame: Optional[int] = None


class ObservationRecord(BaseModel):
    step: int
    frame_id: int
    timestamp: str
    path: str
    cursor: dict
    diff_score: float = 0.0     # internal telemetry: diff vs previous frame
    settle_ms: float = 0.0      # internal telemetry: time until visually stable
    # Why the settle loop stopped: "still" (screen went quiet), "animation"
    # (a small area keeps repainting — a clock, stopwatch or spinner), "timeout"
    # (gave up) or "none" (no settle requested). Internal telemetry: it never
    # reaches the Agent, and it exists so a later diagnosis can tell a genuinely
    # slow transition apart from a target that simply never goes still.
    settle_reason: str = "none"
