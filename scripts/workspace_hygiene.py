"""Audit or remove only known transient files outside benchmark run data."""
from __future__ import annotations

import argparse
import json
import shutil
import stat
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TRANSIENT_DIRS = (ROOT / ".playwright-cli", ROOT / ".test-tmp")


def _inside_root(path: Path) -> bool:
    try:
        path.resolve().relative_to(ROOT.resolve())
        return True
    except ValueError:
        return False


def audit() -> list[dict]:
    items = []
    for path in TRANSIENT_DIRS:
        if path.exists():
            items.append({"path": path.relative_to(ROOT).as_posix(),
                          "kind": "transient_directory"})
    for path in sorted(ROOT.glob("test_*.txt")):
        if path.is_file():
            items.append({"path": path.relative_to(ROOT).as_posix(),
                          "kind": "root_test_diagnostic"})
    return items


def _retry_readonly(function, path, _error) -> None:
    Path(path).chmod(stat.S_IWRITE | stat.S_IREAD)
    function(path)


def clean(items: list[dict]) -> list[dict]:
    errors = []
    for item in items:
        path = (ROOT / item["path"]).resolve()
        if not _inside_root(path) or path == ROOT.resolve():
            raise RuntimeError(f"refusing unsafe cleanup target: {path}")
        try:
            if path in tuple(candidate.resolve() for candidate in TRANSIENT_DIRS):
                shutil.rmtree(path, ignore_errors=False, onexc=_retry_readonly)
            elif path.parent == ROOT.resolve() and path.match("test_*.txt"):
                path.unlink(missing_ok=True)
            else:
                raise RuntimeError(f"target is not on the transient allowlist: {path}")
        except OSError as exc:
            errors.append({"path": item["path"], "error": str(exc)})
    return errors


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--clean-transient", action="store_true")
    args = parser.parse_args()
    items = audit()
    errors = []
    if args.clean_transient:
        errors = clean(items)
    print(json.dumps({"cleaned": bool(args.clean_transient), "items": items,
                      "errors": errors},
                     ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
