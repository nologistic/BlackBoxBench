"""Interactive pixels-only evaluation session for an installable Android APK.

This is the judge-facing counterpart of ``app_reproduction.review``: an LLM
judge drives it turn by turn (observe -> decide -> act -> observe ...) instead
of declaring a whole coordinate plan up front.  Adaptive probing is the entire
point — a judge must be able to look at a frame before choosing the next tap.

Division of responsibility (docs/evaluation_contract.md):

- The judge (a Skill running in a commercial Agent) owns every semantic call:
  which flows to probe, how deep to go, whether a requirement is functionally
  satisfied, and the rationale text.
- This module owns only structural discipline: evidence must cite frames that
  were actually captured, grades are a closed four-value set, no requirement may
  be left ungraded, and the report shape is pinned.  It never inspects source
  code and never decides whether a feature "works".
"""
from __future__ import annotations

import hashlib
import json
import time
from dataclasses import dataclass
from pathlib import Path
from types import SimpleNamespace
from typing import Callable

from benchmark import config
from benchmark.android.runtime import AndroidEmulatorRuntime

from .checklist import Checklist
from .grades import GRADE_LABELS, Grade

# Mirrors app_reproduction.review so that persistence probes mean the same
# thing in reproduction self-review and in third-party evaluation.
WRITE_INTERACTIONS = {"tap", "long_press", "type_text", "press_enter"}
REENTRY_INTERACTIONS = {"tap", "long_press", "press_enter"}

MAX_RATIONALE = 2000
MAX_NOTE = 2000


@dataclass
class _Observation:
    number: int
    action_count: int
    sha256: str
    path: str


class AppEvaluationSession:
    """Install an APK in a throwaway emulator and grade a human checklist.

    Pixels in, coordinate input out.  No UIAutomator, no Accessibility, no
    selectors, no ADB surface, no logcat, no APK introspection.
    """

    def __init__(self, *, apk: Path, checklist: Checklist,
                 package_name: str = "com.blackboxbench.reproduction",
                 launch_activity: str = ".MainActivity",
                 output_root: Path | None = None,
                 network_policy: str = "public",
                 runtime_factory: Callable | None = None):
        apk_path = Path(apk).resolve()
        if not apk_path.is_file() or apk_path.suffix.lower() != ".apk":
            raise FileNotFoundError("evaluation APK is missing")
        self.apk = apk_path
        self.checklist = checklist
        self.package_name = str(package_name)
        self.launch_activity = str(launch_activity)
        if network_policy not in ("offline", "public"):
            raise ValueError("network_policy must be offline or public")
        # The evaluation emulator must match the exploration environment:
        # network-dependent reproductions (feed subscriptions, map tiles,
        # podcast downloads) are unverifiable on an offline judge. The
        # hardcoded "offline" here silently zeroed every network feature of
        # the feeder/organic_maps evaluations on 2026-09-10 while the
        # explorations themselves had already moved to controlled public
        # networking.
        self.network_policy = network_policy
        root = Path(output_root) if output_root is not None else (
            config.APP_OUTPUT_DIR / "evaluations")
        self.run_id = "eval_" + hashlib.sha256(
            f"{self.apk}|{time.time_ns()}".encode("utf-8")).hexdigest()[:10]
        self.output_dir = (root / self.run_id).resolve()
        self.output_dir.mkdir(parents=True, exist_ok=False)
        self.frames_dir = self.output_dir / "frames"
        self.frames_dir.mkdir()
        self._runtime_factory = runtime_factory or (
            lambda spec, directory: AndroidEmulatorRuntime(spec, directory))
        self.runtime = None
        self.started = False
        self.finished = False
        self.observations: list[_Observation] = []
        self.actions: list[dict] = []
        self.results: dict[str, dict] = {}

    # ------------------------------------------------------------- lifecycle

    def _target_seed_files(self) -> list:
        """Content seeds of the ORIGINAL target (checklist id), so the judge
        tests the reproduction on the same populated content the explorer
        saw. Without this, media/library reproductions open onto empty
        libraries and checklist items like "play a song" deadlock the judge
        in an observe-wait loop (observed twice on vinyl 2026-09-21)."""
        try:
            from benchmark.android.targets import _load_registered
            spec = _load_registered().get(
                getattr(self.checklist, "checklist_id", ""))
            return list(getattr(spec, "seed_files", None) or []) if spec \
                else []
        except Exception:
            return []

    def _spec(self) -> SimpleNamespace:
        return SimpleNamespace(
            app_id=f"evaluation_{self.run_id}", apk_path=str(self.apk),
            package_name=self.package_name,
            launch_activity=self.launch_activity, orientation="portrait",
            reset_strategy="clear_data", network_policy=self.network_policy,
            profile_snapshot="", platform="android", kind="android",
            source_type="generated",
            seed_files=self._target_seed_files(),
            # seed sources live under android_seeds/<original target id>,
            # NOT under this synthetic evaluation_* app_id.
            seed_root_id=str(getattr(self.checklist, "checklist_id", "")))

    def start(self) -> dict:
        """Boot the evaluation emulator and install the APK under test."""
        if self.finished:
            raise ValueError("evaluation session is already finished")
        if self.started:
            raise ValueError("evaluation session is already started")
        self.runtime = self._runtime_factory(
            self._spec(), self.output_dir / "runtime")
        try:
            self.runtime.start()
        except Exception:
            # A failed boot leaves a worthless eval_*/frames shell behind;
            # the judge retries with a fresh run_id anyway. Clean it up so
            # evaluations/ does not accumulate crash residue.
            import shutil
            self.runtime = None
            shutil.rmtree(self.output_dir, ignore_errors=True)
            raise
        self.started = True
        return {"started": True, "run_id": self.run_id,
                "requirements": self.checklist.requirements(),
                "grades": [grade.value for grade in Grade],
                "pixels_only": True}

    def _require_active(self):
        if self.finished:
            raise ValueError("evaluation session is already finished")
        if not self.started or self.runtime is None:
            raise ValueError("evaluation session is not started")
        return self.runtime

    # ---------------------------------------------------------- observation

    def observe(self) -> tuple[bytes, dict]:
        """Capture the current screen. The only channel the judge may see."""
        rt = self._require_active()
        png = rt.screenshot()
        number = len(self.observations) + 1
        path = self.frames_dir / f"observation_{number:03d}.png"
        path.write_bytes(png)
        mark = _Observation(number, len(self.actions),
                            hashlib.sha256(png).hexdigest(),
                            path.relative_to(self.output_dir).as_posix())
        self.observations.append(mark)
        info = rt.info()
        return png, {"stage": "app_evaluation", "observation": number,
                     "observations": len(self.observations),
                     "actions": len(self.actions),
                     "width": info.width, "height": info.height,
                     "orientation": getattr(info, "orientation", "portrait"),
                     "graded": len(self.results),
                     "remaining": self.checklist.missing(self.results),
                     "pixels_only": True}

    # -------------------------------------------------------------- actions

    def action(self, action_type: str, args: dict | None = None) -> dict:
        """Human-like coordinate input. Receipts carry no semantic result."""
        rt = self._require_active()
        args = dict(args or {})
        info = rt.info()

        def point(x: int, y: int) -> tuple[int, int]:
            if not 0 <= x < info.width or not 0 <= y < info.height:
                raise ValueError("invalid Android evaluation coordinates")
            return x, y

        if action_type == "tap":
            x, y = point(int(args["x"]), int(args["y"]))
            rt.tap(x, y)
        elif action_type == "long_press":
            x, y = point(int(args["x"]), int(args["y"]))
            rt.long_press(x, y, int(args.get("duration_ms", 700)))
        elif action_type == "swipe":
            x1, y1 = point(int(args["x1"]), int(args["y1"]))
            x2, y2 = point(int(args["x2"]), int(args["y2"]))
            rt.swipe(x1, y1, x2, y2, int(args.get("duration_ms", 400)))
        elif action_type == "type_text":
            rt.type_text(str(args["text"]))
        elif action_type == "press_back":
            rt.key("Back")
        elif action_type == "press_enter":
            rt.key("Enter")
        elif action_type == "restart_app":
            rt.restart_app()
        elif action_type == "reset_app":
            rt.reset()
        elif action_type == "wait":
            time.sleep(min(10_000, max(0, int(args["ms"]))) / 1000)
        else:
            raise ValueError("unsupported evaluation action")
        # Only the action kind is retained: exact coordinates and typed text are
        # not needed for structural evidence, and keeping them would let private
        # input reach the evaluation report.
        self.actions.append({"type": action_type})
        return {"accepted": True, "action": len(self.actions)}

    # --------------------------------------------------------- write probes

    def _validate_persistence(self, requirement_id: str,
                              evidence: dict | None) -> dict | None:
        """Structural check for a before -> after -> persisted triple.

        This verifies *shape*, never meaning: that a write-class interaction
        happened, that pixels actually changed, that a restart or re-entry probe
        followed, and that the persisted frame did not fall back to `before`.
        Whether the changed pixels represent the right data stays with the judge.
        """
        if evidence is None:
            return None
        if not isinstance(evidence, dict):
            raise ValueError("persistence_evidence must be an object")
        by_number = {item.number: item for item in self.observations}
        try:
            before = by_number.get(int(evidence["before_observation"]))
            after = by_number.get(int(evidence["after_observation"]))
            persisted = by_number.get(int(evidence["persisted_observation"]))
        except (KeyError, TypeError, ValueError) as exc:
            raise ValueError(
                "persistence_evidence needs before/after/persisted observation "
                "numbers") from exc
        if not before or not after or not persisted:
            raise ValueError("persistence_evidence cites unknown observations")
        if not before.number < after.number < persisted.number:
            raise ValueError(
                "persistence_evidence must be ordered before < after < persisted")
        if after.action_count <= before.action_count:
            raise ValueError("no interaction between before and after")
        write_actions = self.actions[before.action_count:after.action_count]
        if not any(a["type"] in WRITE_INTERACTIONS for a in write_actions):
            raise ValueError(
                "persistence evidence interval contains no write-class interaction")
        if before.sha256 == after.sha256:
            raise ValueError("write interaction produced no visible change")
        if persisted.action_count <= after.action_count:
            raise ValueError("no persistence probe after the visible change")
        probe = self.actions[after.action_count:persisted.action_count]
        restarted = any(a["type"] in ("restart_app", "reset_app")
                        for a in probe)
        back_index = next((index for index, action in enumerate(probe)
                           if action["type"] == "press_back"), None)
        reentered = (back_index is not None and any(
            action["type"] in REENTRY_INTERACTIONS
            for action in probe[back_index + 1:]))
        if not restarted and not reentered:
            raise ValueError(
                "persistence evidence needs restart_app or press_back followed "
                "by a visible re-entry interaction")
        if before.sha256 == persisted.sha256:
            raise ValueError("persisted state matches the before state")
        return {"requirement_id": requirement_id,
                "before_observation": before.number,
                "after_observation": after.number,
                "persisted_observation": persisted.number}

    # --------------------------------------------------------------- grading

    def record_result(self, *, requirement_id: str, grade: str, rationale: str,
                      evidence_observations: list[int],
                      persistence_evidence: dict | None = None,
                      not_verifiable_reason: str | None = None) -> dict:
        """Grade one requirement. Semantic judgement belongs to the caller."""
        self._require_active()
        requirement = self.checklist.get(requirement_id)
        value = Grade(grade)
        text = (rationale or "").strip()
        if not text:
            raise ValueError("result rationale is required")
        if len(text) > MAX_RATIONALE:
            raise ValueError("result rationale is too long")
        captured = {item.number for item in self.observations}
        numbers = [int(n) for n in (evidence_observations or [])]
        if not numbers:
            raise ValueError("result must cite saved observations")
        unknown = [n for n in numbers if n not in captured]
        if unknown:
            raise ValueError("result cites observations that were never captured")

        reason = (not_verifiable_reason or "").strip() or None
        if reason and len(reason) > MAX_NOTE:
            raise ValueError("not_verifiable_reason is too long")
        if requirement.get("multi_user") and not reason:
            raise ValueError(
                "multi-user requirements need not_verifiable_reason explaining "
                "what a single-device black box could and could not confirm")

        persistence = self._validate_persistence(
            requirement_id, persistence_evidence)
        if requirement.get("persistence") and value is Grade.FULL and (
                persistence is None):
            raise ValueError(
                "persistence requirement graded full needs persistence_evidence")

        record = {"requirement_id": requirement_id,
                  "name": requirement["name"],
                  "grade": value.value, "grade_label": GRADE_LABELS[value],
                  "rationale": text,
                  "evidence_observations": sorted(set(numbers)),
                  "persistence_evidence": persistence,
                  "not_verifiable_reason": reason}
        self.results[requirement_id] = record
        return {"recorded": requirement_id, "grade": value.value,
                "graded": len(self.results),
                "remaining": self.checklist.missing(self.results)}

    # ---------------------------------------------------------------- report

    def finish(self) -> dict:
        """Freeze the four-level report. Refuses while requirements are ungraded."""
        self._require_active()
        missing = self.checklist.missing(self.results)
        if missing:
            raise ValueError("ungraded requirements: " + ", ".join(missing))
        if len(self.observations) < 2:
            raise ValueError("evaluation must contain at least two observations")
        report = {
            "schema_version": 1,
            "platform": "android",
            "run_id": self.run_id,
            "checklist_id": self.checklist.checklist_id,
            "checklist_sha256": self.checklist.sha256,
            "apk_sha256": hashlib.sha256(self.apk.read_bytes()).hexdigest(),
            "package_name": self.package_name,
            "counts": {grade.value: sum(
                1 for record in self.results.values()
                if record["grade"] == grade.value) for grade in Grade},
            "requirements": [self.results[item["id"]]
                             for item in self.checklist.features],
            "actions": self.actions,
            "observations": [item.__dict__ for item in self.observations],
        }
        path = self.output_dir / "evaluation_report.json"
        path.write_text(json.dumps(report, ensure_ascii=False, indent=2),
                        encoding="utf-8")
        self.finished = True
        self.close()
        return {"finished": True, "report": str(path),
                "counts": report["counts"], "run_id": self.run_id}

    def status_payload(self) -> dict:
        return {"started": self.started, "finished": self.finished,
                "run_id": self.run_id,
                "observations": len(self.observations),
                "actions": len(self.actions),
                "graded": len(self.results),
                "total_requirements": len(self.checklist.features),
                "remaining": self.checklist.missing(self.results)}

    def close(self) -> None:
        if self.runtime is not None:
            try:
                self.runtime.stop()
            finally:
                self.runtime = None
        self.started = False
