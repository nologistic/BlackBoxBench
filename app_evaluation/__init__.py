"""Android APK functional evaluation (LLM-as-judge harness).

Structure:

- ``grades``    four-level vocabulary shared with the web judge
- ``checklist`` human requirement list loading and validation
- ``session``   interactive pixels-only evaluation session (judge drives it)

The semantic decision — "is this requirement functionally implemented?" — is
made by an LLM judge running as the ``app-review`` Skill. This package only
supplies pixels plus coordinate input and enforces evidence/grade/completeness
discipline so reports stay comparable across conditions.
"""
from .checklist import Checklist
from .grades import GRADE_CRITERIA, GRADE_LABELS, Grade
from .session import AppEvaluationSession

__all__ = ["AppEvaluationSession", "Checklist", "Grade",
           "GRADE_LABELS", "GRADE_CRITERIA"]
