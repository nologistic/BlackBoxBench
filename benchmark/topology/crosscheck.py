"""Cross-session contamination screening for finalized topologies.

Blocks handover when a finalized topology reproduces, verbatim, distinctive
lines from ANOTHER agent's deliverables (finalized topologies under runs/,
reproduction outputs under website_output/). This targets the specific
threat of an agent reading completed benchmark runs instead of exploring —
the managed-mode counterpart of the self-built integrity screening. It is
implemented independently on purpose: the two exploration conditions must
not share code (AGENTS.md).
"""
from __future__ import annotations

import re
from pathlib import Path

MIN_LINE_LEN = 48          # normalized chars; keeps natural overlap harmless
BLOCK_MIN_HITS = 3         # distinct matched lines required to refuse finalize
MAX_SOURCE_FILES = 400
MAX_SOURCE_BYTES = 2 * 1024 * 1024
_SOURCE_SUFFIXES = {".json", ".md", ".txt", ".jsonl", ".csv", ".html", ".htm"}


def _normalize(text: str) -> str:
    return re.sub(r"\s+", " ", text).strip().lower()


def _fingerprints_from(path: Path, out: set[str]) -> None:
    try:
        if path.stat().st_size > MAX_SOURCE_BYTES:
            return
        content = path.read_text(encoding="utf-8", errors="ignore")
    except OSError:
        return
    for line in content.splitlines():
        normalized = _normalize(line)
        if len(normalized) >= MIN_LINE_LEN:
            out.add(normalized)


def collect_fingerprints(runs_dir: Path, website_output: Path,
                         current_session: str) -> set[str]:
    """Distinctive lines from every OTHER agent's finalized deliverables."""
    fingerprints: set[str] = set()
    count = 0

    if runs_dir.is_dir():
        for child in sorted(runs_dir.iterdir()):
            if count >= MAX_SOURCE_FILES:
                return fingerprints
            if not child.is_dir() or child.name == current_session:
                continue
            topology = child / "functional_topology.json"
            if topology.is_file():
                _fingerprints_from(topology, fingerprints)
                count += 1

    if website_output.is_dir():
        for child in sorted(website_output.rglob("*")):
            if count >= MAX_SOURCE_FILES:
                break
            if (child.is_file()
                    and child.suffix.lower() in _SOURCE_SUFFIXES
                    and not child.is_symlink()):
                _fingerprints_from(child, fingerprints)
                count += 1
    return fingerprints


def matched_lines(text: str, fingerprints: set[str]) -> int:
    """Count how many foreign fingerprint lines appear verbatim in text."""
    if not fingerprints:
        return 0
    flat = _normalize(text)
    return sum(1 for fingerprint in fingerprints if fingerprint in flat)


def assert_clean(graph_json: str, runs_dir: Path, website_output: Path,
                 current_session: str) -> None:
    """Refuse finalize when the topology copies another agent's deliverables.

    Raises ValueError with a message safe to return to the agent (category
    only — never the matched content).
    """
    try:
        fingerprints = collect_fingerprints(runs_dir, website_output,
                                            current_session)
        hits = matched_lines(graph_json, fingerprints)
    except OSError:
        # Unreadable sources must not block legitimate finalization.
        return
    if hits >= BLOCK_MIN_HITS:
        raise ValueError(
            "cross-session contamination detected: the finalized topology "
            f"matches {hits} distinctive lines from other agents' completed "
            "benchmark deliverables. Rebuild every discovery from your own "
            "exploration evidence and retry finalize.")
