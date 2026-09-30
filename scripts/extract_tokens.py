#!/usr/bin/env python3
"""Extract per-run token usage from the agent CLIs (F1 metric source).

- kimi  : ~/.kimi-code/sessions/<wd>/session_*/agents/main/wire.jsonl
          each "usage" event: {inputOther, output, inputCacheRead, inputCacheCreation}
          matched to the exploration window (session created_at → close).
- zcode : ~/.zcode/cli/db/db.sqlite → model_usage table
          (input_tokens + output_tokens + cache_creation + cache_read),
          matched by the run window (session.json created_at ↔ finalized time).
- dsh   : NOT AVAILABLE — the deepseek CLI keeps no local token log.
- codex (evaluate stage): codex exec stdout carries no token fields;
  pass --tokens-evaluate if captured externally.

Usage:
  python3 scripts/extract_tokens.py --agent kimi  --sid <platform_sid> --app <app>
  python3 scripts/extract_tokens.py --agent zcode --sid <platform_sid> --app <app>
Output (stdout): JSON {"explore_reproduce": N, "evaluate": M, "total": T,
                        "note": "..."}
"""
from __future__ import annotations
import argparse, glob, json, os, sqlite3, sys
from datetime import datetime, timezone

KIMI_SESSIONS = os.path.expanduser("~/.kimi-code/sessions")
ZCODE_DB = os.path.expanduser("~/.zcode/cli/db/db.sqlite")


def _parse_ts(s):
    if not s:
        return None
    try:
        return datetime.fromisoformat(s.replace("Z", "+00:00"))
    except Exception:
        return None


def _window_of_run(repo, sid):
    """(start, end) datetimes of the platform run (exploration span)."""
    sj = os.path.join(repo, "runs", sid, "session.json")
    ss = os.path.join(repo, "runs", sid, "session_summary.json")
    start = end = None
    try:
        d = json.load(open(sj)); start = _parse_ts(d.get("created_at"))
    except Exception:
        pass
    try:
        d = json.load(open(ss))
        start = start or _parse_ts(d.get("started_at"))
        end = _parse_ts(d.get("finished_at")) or _parse_ts(d.get("closed_at"))
    except Exception:
        pass
    if not end:  # exploration elapsed fallback
        try:
            d = json.load(open(ss))
            el = float(d.get("elapsed_s") or 0)
            if start and el:
                from datetime import timedelta
                end = start + timedelta(seconds=el * 2)  # generous window
        except Exception:
            pass
    return start, end


def kimi_tokens(repo, sid, app):
    start, end = _window_of_run(repo, sid)
    total = {"inputOther": 0, "output": 0, "inputCacheRead": 0, "inputCacheCreation": 0}
    hit = 0
    # candidate wires: all, filtered by mtime window
    for wire in glob.glob(os.path.join(KIMI_SESSIONS, "*", "session_*", "agents", "main", "wire.jsonl")):
        try:
            mt = datetime.fromtimestamp(os.path.getmtime(wire), tz=timezone.utc)
        except Exception:
            continue
        if start and mt < start - __import__("datetime").timedelta(hours=1):
            continue
        if end and mt > end + __import__("datetime").timedelta(hours=12):
            continue
        try:
            for line in open(wire, encoding="utf-8", errors="replace"):
                if '"usage"' not in line:
                    continue
                try:
                    d = json.loads(line)
                except Exception:
                    continue
                ev = d.get("event") or d
                u = (ev.get("usage") or {}) if isinstance(ev, dict) else {}
                if not u:
                    continue
                for k in total:
                    total[k] += int(u.get(k) or 0)
                hit += 1
        except Exception:
            continue
    if not hit:
        return {"explore_reproduce": 0, "evaluate": 0, "total": 0,
                "note": "no kimi usage events in window"}
    t = sum(total.values())
    return {"explore_reproduce": t, "evaluate": 0, "total": t,
            "detail": total,
            "note": "kimi: inputOther+output+inputCacheRead+inputCacheCreation"}


def zcode_tokens(repo, sid, app):
    start, end = _window_of_run(repo, sid)
    con = sqlite3.connect(f"file:{ZCODE_DB}?mode=ro", uri=True)
    # Join sessions whose title names the same app, so concurrent runs on
    # other apps are not mixed into this run's window.
    rows = con.execute(
        "select mu.started_at, mu.input_tokens, mu.output_tokens, "
        "coalesce(mu.cache_creation_input_tokens,0), coalesce(mu.cache_read_input_tokens,0) "
        "from model_usage mu join session s on mu.session_id = s.id "
        "where s.title like ?", (f"%{app}%",)).fetchall()
    lo = start.timestamp() * 1000 if start else 0
    hi = (end.timestamp() * 1000) if end else float("inf")
    tin = tout = tcc = tcr = 0
    for ts, i, o, cc, cr in rows:
        if ts is None:
            continue
        if start and ts < lo - 3.6e6:   # 1h grace
            continue
        if end and ts > hi + 4.32e7:    # 12h grace (reproduction continues)
            continue
        tin += int(i or 0); tout += int(o or 0)
        tcc += int(cc or 0); tcr += int(cr or 0)
    total = tin + tout + tcc + tcr
    con.close()
    return {"explore_reproduce": total, "evaluate": 0, "total": total,
            "detail": {"input": tin, "output": tout,
                       "cache_creation": tcc, "cache_read": tcr},
            "note": "zcode: input+output+cache_creation+cache_read (model_usage)"}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--agent", required=True, choices=("kimi", "zcode"))
    ap.add_argument("--sid", required=True)
    ap.add_argument("--app", default="")
    ap.add_argument("--repo", default=os.getcwd())
    ap.add_argument("--tokens-evaluate", type=int, default=0)
    a = ap.parse_args()
    fn = kimi_tokens if a.agent == "kimi" else zcode_tokens
    out = fn(a.repo, a.sid, a.app)
    if a.tokens_evaluate:
        out["evaluate"] = a.tokens_evaluate
        out["total"] = out["explore_reproduce"] + a.tokens_evaluate
    print(json.dumps(out, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
