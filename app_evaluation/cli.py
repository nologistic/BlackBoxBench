"""Deprecated: batch `--plan` execution was removed.

Evaluation is now interactive by design: the judge observes a frame, decides the
next coordinate action, and only then grades. A pre-declared plan cannot do that.

Register the judge condition and evaluate through the Skill instead:

    vendor/python/python.exe -m agents.app_review.install --cli codex
    # then, in a new Agent task:  $app-review android_commerce_demo.json

See docs/evaluation_contract.md.
"""
from __future__ import annotations

import sys

_MESSAGE = __doc__


def main() -> None:
    print(_MESSAGE, file=sys.stderr)
    raise SystemExit(2)


if __name__ == "__main__":
    main()
