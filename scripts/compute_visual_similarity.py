#!/usr/bin/env python3
"""Coverage-discounted visual similarity (CLIP) — end-to-end "seen AND reproduced".

Reuses EXPLORATION frames as the original anchor (no separate reference capture):
per checklist task the coverage judge cites the exploration frame that shows it.

For EACH checklist task (denominator = ALL tasks):
  - task not reached in exploration (no original anchor)            -> score 0
  - reached, but reproduction graded 'broken' / has no matching frame -> score 0
  - reached AND reproduced -> max CLIP cosine(original frame, reproduction frame)
Overall = mean over all tasks. This bakes exploration coverage INTO the visual
score (you can only reproduce-visually what you explored), which is exactly the
end-to-end signal wanted, and costs nothing beyond frames we already have.

Usage (inside bbb-clip container):
  python3 scripts/compute_visual_similarity.py --sid <sid> --app <app> \
     --eval-report website_output/evaluations/<run>/evaluation_report.json \
     --coverage runs/<sid>/coverage_judge.json \
     --model-dir /models/.../clip-vit-large-patch14 [--json out.json]
"""
from __future__ import annotations
import argparse, json, os
from pathlib import Path


def _json(p):
    return json.loads(Path(p).read_text(encoding="utf-8")) if p and os.path.exists(p) else {}


def _load(model_dir):
    import torch
    from transformers import CLIPModel, CLIPProcessor
    return torch, CLIPModel.from_pretrained(model_dir).eval(), CLIPProcessor.from_pretrained(model_dir)


def _embed_one(path, torch, model, proc):
    from PIL import Image
    if not path or not os.path.exists(path):
        return None
    inp = proc(images=[Image.open(path).convert("RGB")], return_tensors="pt")
    with torch.no_grad():
        f = model.get_image_features(pixel_values=inp["pixel_values"])
    if not torch.is_tensor(f):
        for a in ("image_embeds", "pooler_output"):
            v = getattr(f, a, None)
            if v is not None:
                f = v; break
        else:
            f = f.last_hidden_state.mean(1)
    return (f / f.norm(dim=-1, keepdim=True))[0]
# _APPEND
def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--sid", required=True)
    ap.add_argument("--app", required=True)
    ap.add_argument("--eval-report", required=True)
    ap.add_argument("--coverage", required=True,
                    help="coverage_judge.json [{id,reached,evidence_frame}] — reuses EXPLORATION frames as the original anchor")
    ap.add_argument("--runs", default="runs")
    ap.add_argument("--model-dir", required=True)
    ap.add_argument("--json", default="")
    a = ap.parse_args()

    orig_dir = os.path.join(a.runs, a.sid, "frames")
    rep = _json(a.eval_report)
    eval_frames_dir = os.path.join(os.path.dirname(a.eval_report), "frames")
    obs = {o.get("number"): o.get("path") for o in rep.get("observations", [])}
    cov = {c.get("id"): c for c in _json(a.coverage)} if os.path.exists(a.coverage) else {}

    torch, model, proc = _load(a.model_dir)
    ecache = {}
    def emb(p):
        if p and p not in ecache:
            ecache[p] = _embed_one(p, torch, model, proc)
        return ecache.get(p)

    # Denominator is ALL checklist tasks. A task scores >0 only if it was BOTH
    # reached in exploration (original anchor exists) AND present in the
    # reproduction; anything else (not explored, broken, or no repro frame) = 0.
    per, scores = [], []
    for r in rep.get("requirements", []):
        rid = r.get("requirement_id")
        c = cov.get(rid) or {}
        of = c.get("evidence_frame") if c.get("reached") else None
        oemb = emb(os.path.join(orig_dir, os.path.basename(of))) if of else None
        if oemb is None:
            scores.append(0.0); per.append({"id": rid, "score": 0.0, "reason": "not_explored"}); continue
        if r.get("grade") == "broken":
            scores.append(0.0); per.append({"id": rid, "score": 0.0, "reason": "broken"}); continue
        cand = [os.path.join(eval_frames_dir, os.path.basename(obs[n]))
                for n in (r.get("evidence_observations") or []) if n in obs]
        best, got = 0.0, False
        for cp in cand:
            e = emb(cp)
            if e is not None:
                got = True; best = max(best, float((oemb * e).sum()))
        s = round(best, 4) if got else 0.0
        scores.append(s); per.append({"id": rid, "score": s, "reason": "matched" if got else "no_repro_frame"})

    out = {"sid": a.sid, "app": a.app,
           "visual_similarity": round(sum(scores) / len(scores), 4) if scores else None,
           "total_reqs": len(scores),
           "nonzero_tasks": sum(1 for s in scores if s > 0),
           "zero_tasks": sum(1 for s in scores if s == 0)}
    print(json.dumps(out, ensure_ascii=False))
    if a.json:
        Path(a.json).write_text(json.dumps({**out, "per_requirement": per}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()

