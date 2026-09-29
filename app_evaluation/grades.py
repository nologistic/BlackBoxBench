"""Four-level grading vocabulary shared by web and Android evaluation.

The same closed set is used on both platforms so that reports from the four
exploration conditions can be aggregated and compared. Adding a fifth level, or
letting a judge invent wording like "mostly working", would silently break that
comparability — hence a hard enum instead of free text.
"""
from __future__ import annotations

from enum import Enum


class Grade(str, Enum):
    FULL = "full"
    PARTIAL = "partial"
    PLACEHOLDER = "placeholder"
    BROKEN = "broken"


GRADE_LABELS = {
    Grade.FULL: "Complete",
    Grade.PARTIAL: "Partial",
    Grade.PLACEHOLDER: "Placeholder",
    Grade.BROKEN: "Broken",
}

GRADE_CRITERIA = {
    Grade.FULL: "The function achieves its goal in visible behavior, including state change and required persistence.",
    Grade.PARTIAL: "The main path works, but branches are missing, validation is absent, or some sub-flows do not work.",
    Grade.PLACEHOLDER: "The UI exists but has no real behavior: clicks change nothing, data does not land, only a static appearance.",
    Grade.BROKEN: "The entry is missing, it crashes, errors, or the feature cannot be reached at all.",
}
