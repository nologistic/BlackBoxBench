"""Pixels-only functional checklist evaluator for an installable Android APK."""
from __future__ import annotations

import hashlib
import json
import secrets
import time
from enum import Enum
from pathlib import Path
from types import SimpleNamespace

from benchmark import config
from benchmark.android.runtime import AndroidEmulatorRuntime


class Grade(str, Enum):
    FULL = "full"
    PARTIAL = "partial"
    PLACEHOLDER = "placeholder"
    BROKEN = "broken"


GRADE_LABELS = {Grade.FULL: "完整", Grade.PARTIAL: "部分",
                Grade.PLACEHOLDER: "占位", Grade.BROKEN: "失效"}


class AppEvaluator:
    def __init__(self, *, apk: Path, package_name: str, launch_activity: str,
                 checklist_path: Path, output_root: Path | None = None,
                 runtime_factory=None):
        self.apk = Path(apk).resolve()
        self.checklist_path = Path(checklist_path).resolve()
        if not self.apk.is_file() or self.apk.suffix.lower() != ".apk":
            raise FileNotFoundError("evaluation APK is missing")
        raw = json.loads(self.checklist_path.read_text(encoding="utf-8"))
        features = raw.get("features") if isinstance(raw, dict) else raw
        if not isinstance(features, list) or not features:
            raise ValueError("evaluation checklist needs a non-empty features array")
        seen = set()
        for item in features:
            if not isinstance(item, dict) or not item.get("id") or not item.get("name"):
                raise ValueError("each feature needs id and name")
            if item["id"] in seen:
                raise ValueError("duplicate feature id")
            seen.add(item["id"])
        self.features = features
        self.spec = SimpleNamespace(
            app_id="evaluation", apk_path=str(self.apk),
            package_name=package_name, launch_activity=launch_activity,
            orientation="portrait", reset_strategy="clear_data",
            network_policy="offline", profile_snapshot="", platform="android",
            kind="android", source_type="generated")
        run_id = "eval_" + secrets.token_hex(5)
        self.output_dir = Path(output_root or config.APP_OUTPUT_DIR / "evaluations") / run_id
        self.output_dir.mkdir(parents=True, exist_ok=False)
        factory = runtime_factory or (
            lambda spec, directory: AndroidEmulatorRuntime(spec, directory))
        self.runtime = factory(self.spec, self.output_dir / "runtime")
        self.started = False
        self.actions: list[dict] = []
        self.observations: list[dict] = []
        self.results: dict[str, dict] = {}

    def start(self) -> None:
        if not self.started:
            self.runtime.start(); self.started = True

    def requirements(self) -> list[dict]:
        return [{"id": item["id"], "name": item["name"],
                 "steps": item.get("steps", []),
                 "expected": item.get("expected", "")}
                for item in self.features]

    def observe(self) -> tuple[bytes, dict]:
        self.start()
        png = self.runtime.screenshot()
        number = len(self.observations) + 1
        path = self.output_dir / f"frame_{number:04d}.png"
        path.write_bytes(png)
        record = {"number": number, "actions": len(self.actions),
                  "sha256": hashlib.sha256(png).hexdigest(),
                  "path": path.name}
        self.observations.append(record)
        info = self.runtime.info()
        return png, {"observation": number, "width": info.width,
                     "height": info.height, "orientation": info.orientation,
                     "pixels_only": True}

    def action(self, kind: str, args: dict) -> dict:
        self.start(); rt = self.runtime
        info = rt.info()
        def point(x: int, y: int) -> tuple[int, int]:
            if not 0 <= x < info.width or not 0 <= y < info.height:
                raise ValueError("invalid Android evaluation coordinates")
            return x, y
        if kind == "tap":
            x, y = point(int(args["x"]), int(args["y"])); rt.tap(x, y)
        elif kind == "long_press":
            x, y = point(int(args["x"]), int(args["y"])); rt.long_press(x, y,
                                                  int(args.get("duration_ms", 700)))
        elif kind == "swipe":
            x1, y1 = point(int(args["x1"]), int(args["y1"]))
            x2, y2 = point(int(args["x2"]), int(args["y2"]))
            rt.swipe(x1, y1, x2, y2, int(args.get("duration_ms", 400)))
        elif kind == "type_text": rt.type_text(str(args["text"]))
        elif kind == "press_back": rt.key("Back")
        elif kind == "press_enter": rt.key("Enter")
        elif kind == "restart_app": rt.restart_app()
        elif kind == "reset_app": rt.reset()
        elif kind == "wait": time.sleep(min(10_000, max(0, int(args["ms"]))) / 1000)
        else: raise ValueError("unsupported evaluation action")
        # Evaluation reports need sequencing, not private typed values or exact
        # coordinates.  Frames are the evidence; keep the trace minimal.
        self.actions.append({"type": kind})
        return {"accepted": True, "action": len(self.actions)}

    def record_result(self, *, feature_id: str, grade: str, rationale: str,
                      evidence_observations: list[int]) -> dict:
        if feature_id not in {item["id"] for item in self.features}:
            raise ValueError("unknown feature id")
        value = Grade(grade)
        existing = {item["number"] for item in self.observations}
        if not evidence_observations or any(n not in existing
                                            for n in evidence_observations):
            raise ValueError("result must cite saved observations")
        if not rationale.strip():
            raise ValueError("result rationale is required")
        self.results[feature_id] = {
            "feature_id": feature_id, "grade": value.value,
            "grade_label": GRADE_LABELS[value], "rationale": rationale,
            "evidence_observations": evidence_observations}
        return self.results[feature_id]

    def finish(self) -> dict:
        missing = [item["id"] for item in self.features
                   if item["id"] not in self.results]
        if missing:
            raise ValueError("ungraded features: " + ", ".join(missing))
        report = {"schema_version": 1, "platform": "android",
                  "apk_sha256": hashlib.sha256(self.apk.read_bytes()).hexdigest(),
                  "features": [self.results[item["id"]] for item in self.features],
                  "actions": self.actions, "observations": self.observations}
        (self.output_dir / "evaluation_report.json").write_text(
            json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
        self.close()
        return {"finished": True,
                "report": str(self.output_dir / "evaluation_report.json"),
                "counts": {grade.value: sum(1 for r in self.results.values()
                                             if r["grade"] == grade.value)
                           for grade in Grade}}

    def close(self) -> None:
        if self.started:
            self.runtime.stop(); self.started = False
