"""Human-authored requirement checklist loading and validation.

A checklist is the operator's contribution to evaluation: a short list of
functional requirements written by hand, in terms of observable behaviour rather
than implementation. Both platforms consume the same shape so that a web
checklist and an Android checklist can be graded by the same rules.

Schema (JSON):

    {
      "checklist_id": "android_commerce_demo",
      "platform": "android" | "web" | "any",
      "features": [
        {
          "id": "favorite",
          "name": "收藏持久化",
          "steps": ["收藏商品", "重启 App"],
          "expected": "收藏状态在重启后保留",
          "persistence": true,       // optional: full grade needs a write probe
          "multi_user": false        // optional: needs an explicit caveat
        }
      ],
      "exclusions": [                // optional, web only: operator-curated
        {                            // out-of-scope surfaces (never graded)
          "feature": "实时协作",
          "treatment": "Hard Exclude",
          "reason": "需要多客户端、网络同步、presence、冲突解决。"
        }
      ]
    }

`persistence` and `multi_user` are the only two machine-meaningful flags. They do
not describe *how* to implement a feature; they describe what a single-device
black-box judge is structurally able to prove about it.

`exclusions` is a third, separate concept: surfaces the operator deliberately
kept out of the benchmark (multi-user flows, accounts/payments, real-time data,
generative AI, external services…). They are context for the agent — not to be
explored, not to be reproduced, and never graded. A checklist without them
(including every Android checklist) simply carries an empty list.
"""
from __future__ import annotations

import hashlib
import json
import re
from pathlib import Path
from typing import Mapping

SAFE_ID = re.compile(r"^[a-z][a-z0-9_-]{0,63}$")
MAX_FEATURES = 200
PLATFORMS = {"web", "android", "any"}


class Checklist:
    """Validated, immutable view of a human requirement list."""

    def __init__(self, *, checklist_id: str, platform: str,
                 features: list[dict], sha256: str, source: Path | None = None,
                 exclusions: list[dict] | None = None):
        self.checklist_id = checklist_id
        self.platform = platform
        self.features = features
        self.sha256 = sha256
        self.source = source
        self._exclusions = list(exclusions or [])

    # ------------------------------------------------------------ construction

    @classmethod
    def load(cls, path: Path) -> "Checklist":
        resolved = Path(path).resolve()
        if not resolved.is_file():
            raise FileNotFoundError("checklist file is missing")
        raw_text = resolved.read_text(encoding="utf-8")
        digest = hashlib.sha256(raw_text.encode("utf-8")).hexdigest()
        return cls.from_object(json.loads(raw_text), sha256=digest,
                              source=resolved,
                              default_id=resolved.stem)

    @classmethod
    def from_object(cls, raw: object, *, sha256: str = "",
                    source: Path | None = None,
                    default_id: str = "checklist") -> "Checklist":
        if isinstance(raw, list):
            payload: Mapping = {"features": raw}
        elif isinstance(raw, dict):
            payload = raw
        else:
            raise ValueError("checklist must be an object or an array")

        checklist_id = str(payload.get("checklist_id")
                           or payload.get("app_id") or default_id)
        if not SAFE_ID.fullmatch(checklist_id):
            raise ValueError("invalid checklist_id")
        platform = str(payload.get("platform") or "any")
        if platform not in PLATFORMS:
            raise ValueError("checklist platform must be web, android or any")

        features = payload.get("features")
        if not isinstance(features, list) or not features:
            raise ValueError("checklist needs a non-empty features array")
        if len(features) > MAX_FEATURES:
            raise ValueError("checklist has too many features")

        cleaned: list[dict] = []
        seen: set[str] = set()
        for item in features:
            if not isinstance(item, dict):
                raise ValueError("each checklist feature must be an object")
            feature_id = str(item.get("id") or "")
            name = str(item.get("name") or "").strip()
            if not SAFE_ID.fullmatch(feature_id):
                raise ValueError(f"invalid feature id: {feature_id!r}")
            if not name:
                raise ValueError(f"feature {feature_id} needs a name")
            if feature_id in seen:
                raise ValueError(f"duplicate feature id: {feature_id}")
            seen.add(feature_id)
            steps = item.get("steps") or []
            if not isinstance(steps, list) or any(
                    not isinstance(step, str) for step in steps):
                raise ValueError(f"feature {feature_id} steps must be strings")
            cleaned.append({
                "id": feature_id,
                "name": name,
                "steps": [step.strip() for step in steps if step.strip()],
                "expected": str(item.get("expected") or "").strip(),
                "persistence": bool(item.get("persistence")),
                "multi_user": bool(item.get("multi_user")),
            })
        # Exclusions get the same strictness as features: a corrupt entry is
        # a silent boundary change, not a cosmetic typo. A mistyped key name
        # must fail loudly instead of quietly widening what gets graded.
        exclusions: list[dict] = []
        raw_exclusions = payload.get("exclusions")
        if raw_exclusions is not None:
            if not isinstance(raw_exclusions, list):
                raise ValueError("checklist exclusions must be an array")
            for index, item in enumerate(raw_exclusions):
                if not isinstance(item, dict):
                    raise ValueError(
                        f"exclusion #{index} must be an object")
                feature = str(item.get("feature") or "").strip()
                if not feature:
                    raise ValueError(
                        f"exclusion #{index} needs a feature")
                exclusions.append({
                    "feature": feature,
                    "treatment": (str(item.get("treatment") or "").strip()
                                  or "Exclude"),
                    "reason": str(item.get("reason") or "").strip(),
                })
        return cls(checklist_id=checklist_id, platform=platform,
                   features=cleaned, sha256=sha256, source=source,
                   exclusions=exclusions)

    # --------------------------------------------------------------- querying

    @property
    def ids(self) -> list[str]:
        return [item["id"] for item in self.features]

    def get(self, requirement_id: str) -> dict:
        for item in self.features:
            if item["id"] == requirement_id:
                return item
        raise ValueError("unknown requirement id")

    def missing(self, results: Mapping[str, object]) -> list[str]:
        return [item["id"] for item in self.features if item["id"] not in results]

    def requirements(self) -> list[dict]:
        """Agent-facing view: the wording a judge needs, nothing else."""
        return [dict(item) for item in self.features]

    def exclusions(self) -> list[dict]:
        """Agent-facing view of out-of-scope surfaces (never graded).

        Context for exploration/reproduction: do not spend time on these,
        and a handoff missing them is not defective.
        """
        return [dict(item) for item in self._exclusions]

    def expect_platform(self, platform: str) -> None:
        if self.platform not in (platform, "any"):
            raise ValueError(
                f"checklist targets {self.platform}, not {platform}")
