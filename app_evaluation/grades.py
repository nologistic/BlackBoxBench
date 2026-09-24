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
    Grade.FULL: "完整",
    Grade.PARTIAL: "部分",
    Grade.PLACEHOLDER: "占位",
    Grade.BROKEN: "失效",
}

GRADE_CRITERIA = {
    Grade.FULL: "功能在可见行为上达成目标，包含状态变化与必要的持久化。",
    Grade.PARTIAL: "主要路径可用，但存在缺失分支、缺少校验或部分子流程不可用。",
    Grade.PLACEHOLDER: "界面存在但没有真实行为：点击无变化、数据不落地、仅有静态外观。",
    Grade.BROKEN: "入口缺失、崩溃、报错或完全无法进入该功能。",
}
