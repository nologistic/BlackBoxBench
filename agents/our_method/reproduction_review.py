"""Pixels-only review of a managed reproduction result.

This is deliberately separate from reference-app exploration.  It serves only
the Agent-owned ``website_output/<handoff>`` directory through the existing
whitelisted Chromium Runtime, so the review surface remains screenshots in and
coordinate/keyboard input out.  No DOM, selector, URL, network, repository, or
reference-app channel is added.
"""
from __future__ import annotations

import hashlib
import re
import secrets
import shutil
import socket
import sys
import tempfile
import time
from collections import Counter
from pathlib import Path, PurePosixPath
from typing import Callable

from benchmark.runtime.base import Runtime
from benchmark.runtime.local_chromium import LocalChromiumRuntime


MIN_REVIEW_ACTIONS = 3
DEFAULT_MAX_REVISIONS = 3
# Keywords (EN/CN) marking a write-path flow in review checked_flows.
_WRITE_FLOW_HINT = re.compile(
    r"save|create|edit|new|write|publish|delete|favorite|remove|"
    r"submit|login|register|upload|保存|创建|新建|编辑|发布|删除|收藏|"
    r"移除|提交|写入|登录|注册|上传", re.IGNORECASE)

# V0.2.4: observations retain only action *classes* (and non-sensitive key
# names), never typed text or coordinates.  This lets the server distinguish a
# real write interaction from passive waits/animation, and distinguish an
# explicit persistence probe from arbitrary activity after the write.
_PERSISTENCE_KEYS = frozenset({"f5", "browserback", "browserforward"})


def _is_write_interaction(action: dict) -> bool:
    """Return whether an action can directly submit or alter visible data."""
    kind = action.get("type")
    if kind in {"click", "double_click", "drag", "type_text"}:
        return True
    if kind == "key_press":
        return str(action.get("key") or "").lower() not in _PERSISTENCE_KEYS
    return False


def _has_persistence_probe(actions: list[dict]) -> bool:
    """Require an explicit reload, or a complete back-then-forward re-entry."""
    if any(action.get("type") == "reload" for action in actions):
        return True
    keys = [str(action.get("key") or "").lower() for action in actions
            if action.get("type") == "key_press"]
    if "f5" in keys:
        return True
    try:
        back = keys.index("browserback")
        keys.index("browserforward", back + 1)
        return True
    except ValueError:
        return False


def _free_port() -> int:
    with socket.socket() as probe:
        probe.bind(("127.0.0.1", 0))
        return int(probe.getsockname()[1])


def _output_fingerprint(root: Path) -> str:
    """Fingerprint the complete deliverable without following symlinks."""
    digest = hashlib.sha256()
    for path in sorted(root.rglob("*"), key=lambda item: item.as_posix()):
        if path.is_symlink():
            raise ValueError("reproduction output must not contain symlinks")
        if not path.is_file():
            continue
        relative = path.relative_to(root).as_posix()
        digest.update(relative.encode("utf-8"))
        digest.update(b"\0")
        with path.open("rb") as stream:
            while chunk := stream.read(1024 * 1024):
                digest.update(chunk)
        digest.update(b"\0")
    return digest.hexdigest()


def _preview_root(output_dir: Path, value: object) -> tuple[Path, str]:
    raw = str(value or ".").replace("\\", "/")
    relative = PurePosixPath(raw)
    if relative.is_absolute() or ".." in relative.parts:
        raise ValueError("preview path must stay inside the reproduction output")
    parts = [part for part in relative.parts if part not in ("", ".")]
    current = output_dir
    for part in parts:
        current = current / part
        if current.exists() and current.is_symlink():
            raise ValueError("preview path must not traverse symlinks")
    resolved = current.resolve()
    try:
        resolved.relative_to(output_dir.resolve())
    except ValueError as exc:
        raise ValueError(
            "preview path must stay inside the reproduction output") from exc
    if not resolved.is_dir() or not (resolved / "index.html").is_file():
        raise ValueError("preview path must contain index.html")
    return resolved, PurePosixPath(*parts).as_posix() if parts else "."


def _default_runtime(preview_root: Path, work_dir: Path) -> Runtime:
    port = _free_port()
    return LocalChromiumRuntime(
        [sys.executable, "-m", "http.server", str(port),
         "--bind", "127.0.0.1", "--directory", str(preview_root)],
        port, secrets.token_urlsafe(24), work_dir,
        width=1440, height=900,
    )


RuntimeFactory = Callable[[Path, Path], Runtime]


class ManagedReproductionReview:
    """Finite review/revision lifecycle for one managed handoff."""

    def __init__(self, output_dir: Path, handoff_id: str, *,
                 max_revisions: int = DEFAULT_MAX_REVISIONS,
                 runtime_factory: RuntimeFactory = _default_runtime):
        if not 0 <= int(max_revisions) <= 5:
            raise ValueError("max_revisions must be between 0 and 5")
        self.output_dir = output_dir.resolve()
        self.handoff_id = handoff_id
        self.max_revisions = int(max_revisions)
        self._runtime_factory = runtime_factory
        self._runtime: Runtime | None = None
        self._temp_dir: Path | None = None
        self._active: dict | None = None
        self._rounds: list[dict] = []
        self._revision_count = 0
        self._pending_revision = False
        self._revision_baseline: str | None = None
        self._accepted_fingerprint: str | None = None

    @property
    def active(self) -> bool:
        return self._runtime is not None and self._active is not None

    @property
    def accepted(self) -> bool:
        return self._accepted_fingerprint is not None

    def _capture(self) -> tuple[bytes, dict]:
        if not self.active:
            raise ValueError("start_reproduction_review must be called first")
        png = self._runtime.screenshot()
        self._active["observations"] += 1
        self._active["last_observed_action"] = self._active["actions"]
        self._active["observation_hashes"].append(
            hashlib.sha256(png).hexdigest())
        # V0.2.4: (observation_number, actions_so_far) marks let the server
        # verify that real interactions happened between the observations an
        # agent cites as write-flow evidence.
        self._active["observation_marks"].append(
            (self._active["observations"], self._active["actions"]))
        info = self._runtime.info()
        tabs = self._runtime.list_tabs()
        meta = {
            "stage": "reproduction_review",
            "handoff_id": self.handoff_id,
            "round": self._active["round"],
            "max_revisions": self.max_revisions,
            "actions": self._active["actions"],
            "observations": self._active["observations"],
            "width": info.width,
            "height": info.height,
            "pixels_only": True,
        }
        if tabs is not None:
            meta["tabs"] = tabs
        return png, meta

    def start_round(self, entry_path: object = ".") -> tuple[bytes, dict]:
        if self.active:
            raise ValueError("the current reproduction review round is still active")
        if self.accepted:
            raise ValueError("the reproduction has already been accepted")
        current = _output_fingerprint(self.output_dir)
        if self._pending_revision:
            if current == self._revision_baseline:
                raise ValueError(
                    "the previous review requested revision but the output did not change")
            self._pending_revision = False
            self._revision_baseline = None
        preview_root, normalized_entry = _preview_root(
            self.output_dir, entry_path)
        temp_dir = Path(tempfile.mkdtemp(
            prefix=f"bbb-review-{self.handoff_id}-"))
        runtime = self._runtime_factory(preview_root, temp_dir)
        try:
            runtime.start()
            self._runtime = runtime
            self._temp_dir = temp_dir
            self._active = {
                "round": len(self._rounds) + 1,
                "entry_path": normalized_entry,
                "started_at": time.time(),
                "start_fingerprint": current,
                "actions": 0,
                "observations": 0,
                "last_observed_action": -1,
                "action_types": Counter(),
                "action_log": [],
                "observation_hashes": [],
                "observation_marks": [],
            }
            return self._capture()
        except Exception:
            try:
                runtime.stop()
            except Exception:
                pass
            shutil.rmtree(temp_dir, ignore_errors=True)
            self._runtime = None
            self._temp_dir = None
            self._active = None
            raise

    def observe(self) -> tuple[bytes, dict]:
        return self._capture()

    @staticmethod
    def _coordinate(value: object, name: str) -> int:
        coordinate = int(value)
        limit = 1439 if name.startswith("x") else 899
        if not 0 <= coordinate <= limit:
            raise ValueError(f"{name} must be between 0 and {limit}")
        return coordinate

    def action(self, action_type: str, args: dict) -> dict:
        if not self.active:
            raise ValueError("start_reproduction_review must be called first")
        runtime = self._runtime
        action_record = {"type": action_type}
        if action_type in ("click", "double_click", "move_pointer",
                           "mouse_down", "mouse_up"):
            x = self._coordinate(args.get("x"), "x")
            y = self._coordinate(args.get("y"), "y")
            if action_type == "click":
                runtime.click(x, y)
            elif action_type == "double_click":
                runtime.click(x, y, count=2)
            elif action_type == "move_pointer":
                runtime.mouse_move(x, y)
            elif action_type == "mouse_down":
                runtime.mouse_down(x, y)
            else:
                runtime.mouse_up(x, y)
        elif action_type == "drag":
            x1 = self._coordinate(args.get("x1"), "x1")
            y1 = self._coordinate(args.get("y1"), "y1")
            x2 = self._coordinate(args.get("x2"), "x2")
            y2 = self._coordinate(args.get("y2"), "y2")
            runtime.mouse_down(x1, y1)
            runtime.mouse_move(x2, y2)
            runtime.mouse_up(x2, y2)
        elif action_type == "type_text":
            text = args.get("text")
            if not isinstance(text, str) or len(text) > 10_000:
                raise ValueError("text must be a string of at most 10000 characters")
            runtime.type_text(text)
        elif action_type in ("key_press", "key_down", "key_up"):
            key = args.get("key")
            if not isinstance(key, str) or not key:
                raise ValueError("key is required")
            kind = {"key_press": "press", "key_down": "down",
                    "key_up": "up"}[action_type]
            runtime.key(key, kind)
            action_record["key"] = key
        elif action_type == "reload":
            runtime.key("F5", "press")
        elif action_type == "scroll":
            runtime.scroll(int(args.get("dx", 0)), int(args.get("dy", 0)))
        elif action_type == "wait":
            milliseconds = int(args.get("ms", 0))
            if not 0 <= milliseconds <= 10_000:
                raise ValueError("ms must be between 0 and 10000")
            time.sleep(milliseconds / 1000)
        elif action_type == "switch_tab":
            runtime.switch_tab(int(args.get("tab_index")))
        elif action_type == "close_tab":
            runtime.close_tab()
        else:
            raise ValueError(f"unsupported review action: {action_type}")
        self._active["actions"] += 1
        self._active["action_types"][action_type] += 1
        self._active["action_log"].append(action_record)
        return {
            "stage": "reproduction_review",
            "action_accepted": True,
            "round": self._active["round"],
            "actions": self._active["actions"],
            "note": "call review_observe to inspect the visible result",
        }

    def _stop_active(self) -> None:
        runtime, temp_dir = self._runtime, self._temp_dir
        self._runtime = None
        self._temp_dir = None
        if runtime is not None:
            try:
                runtime.stop()
            except Exception:
                pass
        if temp_dir is not None:
            resolved = temp_dir.resolve()
            temp_root = Path(tempfile.gettempdir()).resolve()
            if (resolved.parent == temp_root and
                    resolved.name.startswith(f"bbb-review-{self.handoff_id}-")):
                shutil.rmtree(resolved, ignore_errors=True)

    def complete_round(self, *, decision: object,
                       checked_flows: object, findings: object,
                       write_flow_exemption: object = None,
                       write_flow_evidence: object = None) -> dict:
        if not self.active:
            raise ValueError("there is no active reproduction review round")
        if decision not in ("accept", "revise"):
            raise ValueError("decision must be accept or revise")
        if not isinstance(checked_flows, list) or not checked_flows or not all(
                isinstance(item, str) and item.strip() for item in checked_flows):
            raise ValueError("checked_flows must be a non-empty string array")
        exemption = None
        if write_flow_exemption is not None:
            if not isinstance(write_flow_exemption, str) or not write_flow_exemption.strip():
                raise ValueError(
                    "write_flow_exemption must be a non-empty string stating "
                    "why the target has no write-path flows")
            exemption = write_flow_exemption.strip()
        # V0.2.4: write-path acceptance needs structural pixel evidence plus
        # auditable action classes between each observation.  Passive actions
        # cannot masquerade as writes, and the persistence leg requires an
        # explicit reload/navigation probe.
        # Each write_flow_evidence item cites three review_observe numbers:
        #   before    — captured before the write operation
        #   after     — captured after it (hash must differ; actions must
        #               have advanced between the two observations)
        #   persisted — captured again after a reload/return/re-entry action
        #               (actions must have advanced after `after`)
        # This still cannot prove semantics (any visible change passes), but
        # a bare textual claim or an accidental two-screenshot diff can no
        # longer satisfy the check.
        evidence = None
        if write_flow_evidence is not None:
            if not isinstance(write_flow_evidence, list) or not write_flow_evidence:
                raise ValueError(
                    "write_flow_evidence must be a non-empty array of "
                    "{flow, before_observation, after_observation, "
                    "persisted_observation} objects")
            for item in write_flow_evidence:
                if not isinstance(item, dict) or not str(
                        item.get("flow") or "").strip():
                    raise ValueError(
                        "each write_flow_evidence item needs a non-empty "
                        "'flow' description")
                try:
                    before = int(item.get("before_observation"))
                    after = int(item.get("after_observation"))
                    persisted = int(item.get("persisted_observation"))
                except (TypeError, ValueError):
                    raise ValueError(
                        "write_flow_evidence observations must be integers "
                        "(observation numbers as returned by review_observe)")
                total = self._active["observations"]
                hashes = self._active["observation_hashes"]
                marks = {n: a for n, a in self._active["observation_marks"]}
                action_log = self._active["action_log"]
                if not (1 <= before < after < persisted <= total):
                    raise ValueError(
                        "write_flow_evidence observation numbers must "
                        "satisfy 1 <= before < after < persisted <= %d "
                        "(current round observation count); persisted is "
                        "the re-entry observation after a reload/return" % total)
                before_actions = marks.get(before, -1)
                after_actions = marks.get(after, -1)
                persisted_actions = marks.get(persisted, -1)
                if after_actions <= before_actions:
                    raise ValueError(
                        "write_flow_evidence pair (%d, %d) has no interaction "
                        "between the observations — perform the write "
                        "operation (review_click / review_type_text / ...) "
                        "between the two review_observe calls" % (before, after))
                write_actions = action_log[before_actions:after_actions]
                if not any(_is_write_interaction(action)
                           for action in write_actions):
                    raise ValueError(
                        "write_flow_evidence pair (%d, %d) contains only "
                        "passive activity — perform a write-capable interaction "
                        "such as click/type/submit between the observations" %
                        (before, after))
                if hashes[after - 1] == hashes[before - 1]:
                    raise ValueError(
                        "write_flow_evidence pair (%d, %d) shows identical "
                        "screenshots — the write flow did not visibly change "
                        "the page; capture the real before/after states with "
                        "review_observe" % (before, after))
                if persisted_actions <= after_actions:
                    raise ValueError(
                        "write_flow_evidence persisted observation %d has no "
                        "interaction after %d — reload/return/re-enter the "
                        "page (review_reload, review_key_press F5, or "
                        "BrowserBack+BrowserForward), "
                        "then review_observe again and cite that number as "
                        "persisted_observation" % (persisted, after))
                persistence_actions = action_log[after_actions:persisted_actions]
                if not _has_persistence_probe(persistence_actions):
                    raise ValueError(
                        "write_flow_evidence persisted observation %d has "
                        "activity after %d, but no explicit reload/return "
                        "probe — call review_reload (recommended) or "
                        "review_key_press F5 or BrowserBack+BrowserForward "
                        "before observing" %
                        (persisted, after))
                if hashes[persisted - 1] == hashes[before - 1]:
                    raise ValueError(
                        "write_flow_evidence persisted observation %d matches "
                        "the before state — the visible change did not survive "
                        "the reload/return probe" % persisted)
            evidence = [dict(item) for item in write_flow_evidence]
        # Enforced only on accept; revise rounds report problems and are not
        # blocked.  Targets without write flows may pass an explicit exemption.
        if decision == "accept" and exemption is None:
            if not any(_WRITE_FLOW_HINT.search(item) for item in checked_flows):
                raise ValueError(
                    "accept requires at least one write-path verification in "
                    "checked_flows (create/edit/save/favorite/...) — or pass "
                    "write_flow_exemption when the target genuinely has no "
                    "write flows")
            if evidence is None:
                raise ValueError(
                    "accept requires write_flow_evidence: for one write flow, "
                    "review_observe before the operation, perform it, then "
                    "review_observe after, and pass those two observation "
                    "numbers — the server verifies the screenshots differ")
        if not isinstance(findings, list) or not findings or not all(
                isinstance(item, str) and item.strip() for item in findings):
            raise ValueError("findings must be a non-empty string array")
        if self._active["actions"] < MIN_REVIEW_ACTIONS:
            raise ValueError(
                f"review at least {MIN_REVIEW_ACTIONS} visible interactions before deciding")
        if self._active["last_observed_action"] != self._active["actions"]:
            raise ValueError("call review_observe after the latest interaction")
        current = _output_fingerprint(self.output_dir)
        if current != self._active["start_fingerprint"]:
            raise ValueError(
                "the output changed during review; restart the round on a stable build")
        if decision == "revise" and self._revision_count >= self.max_revisions:
            raise ValueError(
                "the revision limit is reached; accept with documented findings")

        active = self._active
        record = {
            "round": active["round"],
            "entry_path": active["entry_path"],
            "started_at": active["started_at"],
            "finished_at": time.time(),
            "actions": active["actions"],
            "observations": active["observations"],
            "action_types": dict(sorted(active["action_types"].items())),
            "action_log": active["action_log"],
            "write_flow_exemption": exemption,
            "write_flow_evidence": evidence,
            "observation_hashes": active["observation_hashes"],
            "checked_flows": [item.strip() for item in checked_flows],
            "findings": [item.strip() for item in findings],
            "decision": decision,
        }
        self._stop_active()
        self._active = None
        self._rounds.append(record)
        if decision == "revise":
            self._revision_count += 1
            self._pending_revision = True
            self._revision_baseline = current
        else:
            self._accepted_fingerprint = current
        return {
            "stage": "reproduction_review",
            "round": record["round"],
            "decision": decision,
            "revision_count": self._revision_count,
            "max_revisions": self.max_revisions,
            "next": ("modify the output, then start_reproduction_review again"
                     if decision == "revise" else "finish_reproduction"),
        }

    def ensure_accepted(self) -> None:
        if self.active:
            raise ValueError("complete the active reproduction review first")
        if not self.accepted:
            raise ValueError(
                "finish_reproduction requires an accepted reproduction review")
        if _output_fingerprint(self.output_dir) != self._accepted_fingerprint:
            self._accepted_fingerprint = None
            raise ValueError(
                "the output changed after acceptance; review the new build again")

    def status_payload(self) -> dict:
        return {
            "review_required": True,
            "review_active": self.active,
            "review_accepted": self.accepted,
            "review_rounds": len(self._rounds),
            "revision_count": self._revision_count,
            "max_revisions": self.max_revisions,
        }

    def summary(self) -> dict:
        self.ensure_accepted()
        return {
            "schema_version": 1,
            "handoff_id": self.handoff_id,
            "pixels_only": True,
            "accepted": True,
            "revision_count": self._revision_count,
            "max_revisions": self.max_revisions,
            "rounds": self._rounds,
        }

    def close(self) -> None:
        self._stop_active()
        self._active = None
