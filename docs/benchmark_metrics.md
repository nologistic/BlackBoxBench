# BlackBoxBench — Web (nograph) Evaluation & Metrics

Reference for the paper on *benchmarking agents' ability to autonomously explore
and reproduce complex, long‑horizon web/app applications*. Covers the pipeline,
every metric (meaning + exact computation + data source + script), the concurrent
runner, a worked example, and honest caveats.

Condition documented here: **web‑nograph** (the no‑graph ablation — the agent
gets pixels + coordinate HID only, **no discovery/topology‑recording tools**; it
organizes findings however it likes). Model split used for the suite:
exploration + reproduction = **Opus 4.8**, evaluation judge = **gpt‑6‑astra**.

---

## 1. Pipeline & artifacts

Each target runs as three separate agent conversations:

| Stage | Skill / tools | Produces |
|---|---|---|
| **A. Explore** | `web-nograph` skill, pixel MCP (`observe`/`click`/`scroll`/`type_text`/…, `finalize`) | `runs/<sid>/` : `actions.jsonl`, `observations.jsonl`, `frames/`, `functional_topology.json`, `session_summary.json`, `crash_report.json` |
| **B. Reproduce** | bound to the finalized session; `workspace_*` + `review_*` tools; offline sandbox (`--network none`) | `website_output/<handoff>/` (the reproduced site) + `.blackboxbench/review_summary.json` |
| **C. Evaluate** | `web-review` skill vs `review_specs/<app>.json` | `website_output/evaluations/<run>/evaluation_report.json` + `frames/observation_*.png` |

`review_specs/<app>.json` is a human‑authored checklist: a list of `features`
(each `id`, `name`, `steps`, `expected`, and flags `persistence` / `multi_user`)
plus `exclusions` (surfaces deliberately out of scope).

All metrics below are computed **from these artifacts** — no change to the agent,
no extra tools during exploration/reproduction.

---

## 2. Metrics

Notation: a checklist has `N` requirements. Grade weights
`W = {full:1.0, partial:0.5, placeholder:0.25, broken:0.0}`.
<!-- APPEND2 -->
### A. Reproduction fidelity

**A1. Four‑tier grades — `full / partial / placeholder / broken`.**
The core per‑requirement judgement, produced by the `web-review` LLM judge which
drives a headless Chromium over the served reproduction and grades each checklist
requirement. `full` = works incl. persistence; `partial` = works but incomplete;
`placeholder` = looks present but non‑functional (fake save / decorative);
`broken` = absent or errors. *Source:* `evaluation_report.json → counts`. Raw
per‑requirement records (grade, rationale, evidence frames, persistence evidence)
are kept verbatim.

**A2. Weighted Fidelity Score `WFS = (Σ_r W[grade_r]) / N`.**
A single 0–1 number per target (aggregatable across the suite). *Meaning:* overall
reproduction quality. *Script:* `compute_metrics.py` (`wfs`). *zhihu:* 0.317.

**A3. Persistence‑subset WFS** — WFS restricted to requirements with checklist
flag `persistence:true`. *Meaning:* the **long‑horizon / stateful** core — can the
reproduction keep state across reload (a hallmark of complex apps). *Source:*
checklist `persistence` flags ∩ grades. *Script:* `compute_metrics.py`
(`persistence_wfs`, `persistence_n`). *zhihu:* 0.417 over 9 reqs.

### B. Exploration quality

**B1. Exploration coverage (nograph) — `reached / N`.** Fraction of checklist
tasks the agent *demonstrably reached/exercised during exploration*. Because
nograph records nothing, this is computed **passively, post‑hoc**: a *coverage
judge* (`gpt‑6‑astra`, vision) is shown the exploration keyframes + the checklist
and decides, per task, reached / not, citing an evidence frame. Zero effect on the
agent's run. *Script:* `coverage_judge.sh` → `runs/<sid>/coverage_judge.json`.
*zhihu:* 12/30 = 40 %. *Caveat:* judge‑based estimate, but every "reached" cites an
auditable frame.

**B2. Effective‑action rate.** Fraction of observations whose `diff_score`
(pixel delta vs previous frame) exceeds `ε` (default 0.002) → share of actions
that produced a **visible change** (purposeful vs wasted motion). *Source:*
`observations.jsonl`. *Script:* `compute_metrics.py`. *zhihu:* 0.764.

**B3. Invalid‑action rate.** Fraction of actions with `accepted:false` or an
`error` (illegal / mis‑targeted). *Source:* `actions.jsonl`. *zhihu:* 0.0.

**B4. Exploration horizon.** `steps`, `duration_s`, and distinct frames seen —
characterises how long‑horizon the exploration was. *Source:*
`session_summary.json` + `observations.jsonl`. *zhihu:* 58 steps, ~1706 s.

### C. Visual similarity (CLIP, coverage‑discounted)

**C1. Visual similarity.** Per checklist task, over **all `N` tasks**:
* task not reached in exploration (no original anchor) → **0**
* reached, but reproduction `broken` / has no matching frame → **0**
* reached **and** reproduced → `max` cosine of CLIP(ViT‑L/14) image embeddings
  between the **exploration frame** the coverage judge cited (the real site) and
  the reproduction's evidence frame(s).

Average over all tasks = visual similarity. *Meaning:* end‑to‑end "**seen AND
reproduced** visually" — exploration coverage is baked in (you can only reproduce
what you saw). Reuses exploration frames (no separate capture) → cheap. *Script:*
`compute_visual_similarity.py` (CLIP‑L/14 on CPU). *zhihu:* **0.340** (12 non‑zero
of 30). *Caveat:* CLIP absolute values for UI screenshots run high; the signal is
the **relative** comparison across models/targets.
<!-- APPEND3 -->
### D. Reproduction process

**D1. Self‑correction rounds.** `revision_count` / number of review rounds the
reproduction agent needed before its pixel self‑review accepted the site. *Meaning:*
proxy for reproduction difficulty / agent self‑critique. *Source:*
`.blackboxbench/review_summary.json`. *zhihu:* 0 revisions, 1 round, accepted.

**D2. Illusion rate.** Fraction of requirements graded `placeholder`/`broken`
**or** whose rationale matches illusion keywords (fake / shell / decorative /
占位 / 装饰 / no‑op …). *Meaning:* how often the agent produced UI that *looks*
present but isn't functional — a novel "superficial‑vs‑functional" signal.
*Source:* `evaluation_report.json` grades + rationale. *Script:* `compute_metrics.py`.
*zhihu:* 0.467.

### E. Robustness / benchmark validity

**E1. Platform‑attributed failures.** Count of `crash_report.json` events +
`actions.jsonl` entries flagged `environment_degrading` (e.g. a CDP websocket
timeout). Per `docs/evaluation_contract.md §11`, these separate **platform
defects** from **agent capability** so a score isn't silently polluted by
infrastructure blips. Report it to defend the benchmark's validity. *zhihu:* 1
(a transient CDP timeout; the session self‑healed and finalized).

### F. Efficiency (token‑based cost)

**F1. Total tokens.** Per stage and summed: `input + output +
cache_creation + cache_read` from each stage's final `result` event.
*Note:* dominated by `cache_read` (multi‑turn agents re‑read the cached context
each turn); this is the literal *total tokens processed*. Switch to
`input+output` only if you want "new compute" instead. *Source:* the stage
`stream-json` logs. *zhihu:* explore 11.75M, reproduce 2.95M, evaluate 26.59M,
**total 41.29M**.

**F2. Tokens‑per‑fidelity‑point** `= tokens_total / (WFS · N)`. Token cost per
unit of reproduction success — the key efficiency axis for model comparison.
*zhihu:* 4.35M tokens / fidelity‑point.

---

## 3. Scripts

| script | role |
|---|---|
| `scripts/run_web_suite.sh` | concurrent suite runner; per target runs A→B→C then all metric scripts, writes `suite_results/results.csv` + `summary_*.md` |
| `scripts/compute_metrics.py` | artifact metrics (WFS, persistence‑WFS, action rates, horizon, platform failures, revisions, illusion, token efficiency) |
| `scripts/coverage_judge.sh` | passive nograph coverage judge (+ helpers `_pick_keyframes.py`, `_checklist_text.py`, `_extract_coverage.py`) |
| `scripts/compute_visual_similarity.py` | coverage‑discounted CLIP visual similarity |
| `scripts/_row.py` | assembles one CSV row from the metric JSONs |
| `scripts/_clip_setup.sh` | one‑time CLIP env build (CPU torch + ModelScope CLIP‑L/14) |

**Run one metric manually** (inside a `python:3.12` container with the repo mounted):
```
python3 scripts/compute_metrics.py --sid <sid> --app <app> --handoff <handoff> \
   --eval-report website_output/evaluations/<run>/evaluation_report.json \
   --tokens-explore N --tokens-reproduce N --tokens-evaluate N --json metrics.json
```
Visual similarity needs the `blackboxbench/clip` image + the cached CLIP‑L/14
weights and the coverage JSON (see `compute_visual_similarity.py` header).

**Run the whole suite:**
```
BBB_SUITE_GEN_MODEL="Opus 4.8" BBB_SUITE_EVAL_MODEL="gpt-6-astra" \
  bash scripts/run_web_suite.sh [target ...]
```
Prereqs: docker images `blackboxbench/{live-controller,reproduction-workbench,clip}`,
network `bbb-net`, and per‑target login profile `runs/live_targets/<t>/profile`
(no‑login tool sites may use an empty profile dir).
<!-- APPEND4 -->
---

## 4. Result table columns (`suite_results/results.csv`)

`target, session, full, partial, placeholder, broken, wfs, persistence_wfs,
coverage, visual_sim, eff_action, invalid, platform_fail, revisions, illusion,
tok_explore, tok_reproduce, tok_evaluate, tok_total, tok_per_fidelity`

## 5. Worked example — `zhihu_web` (C1, validated)

| metric | value |
|---|---|
| full / partial / placeholder / broken | 0 / 16 / 6 / 8 |
| WFS | 0.317 |
| persistence‑WFS (9 reqs) | 0.417 |
| exploration coverage | 12/30 (40 %) |
| visual similarity (coverage‑discounted) | 0.340 |
| effective‑action rate | 0.764 |
| invalid‑action rate | 0.0 |
| platform failures | 1 (CDP timeout, self‑healed) |
| self‑correction revisions | 0 |
| illusion rate | 0.467 |
| tokens (explore / reproduce / evaluate / total) | 11.75M / 2.95M / 26.59M / 41.29M |
| tokens per fidelity‑point | 4.35M |

*(This C1 run used Opus 5 for all stages; the suite uses Opus 4.8 + gpt‑6‑astra.)*

## 6. Caveats (state these in the paper)

1. **Coverage & visual similarity are judge/model‑assisted**, not ground truth:
   coverage uses a vision‑LLM judge over keyframes; each claim cites an auditable
   frame, but it is an estimate.
2. **Visual similarity is coverage‑discounted by design** — unexplored or
   unreproduced tasks score 0. It measures end‑to‑end "seen AND reproduced",
   not the isolated visual quality of what was reproduced. CLIP absolute values
   are high for UI; use relative comparison.
3. **Token totals include cache reads** (re‑read context), so they track
   *processed* tokens, not billed dollars. Consistent within the suite.
4. **Platform failures must be reviewed** before attributing a low score to the
   agent (evaluation_contract §11): agent acted legally but got no result =
   platform defect (fix + rerun); platform served correctly but agent did poorly
   = capability difference (keep).
5. **Live targets are non‑deterministic** (real sites, hand‑maintained login, no
   S0 reset); only the pixel+HID channel is fixed. Re‑runs will differ.
6. **nograph vs baseline ablation** (does forcing topology recording help/hurt
   reproduction?) is planned but **not yet run** here.

## 7. Reproducibility

- Images: `blackboxbench/live-controller` (headed Chromium + Xvfb + repo deps,
  runs the controller with `LiveChromiumRuntime`), `reproduction-workbench`
  (offline `--network none` build sandbox), `clip` (CPU torch + transformers).
- CLIP model: `AI-ModelScope/clip-vit-large-patch14` via ModelScope (HF blocked
  on the host), cached under `clip_models/`.
- Mirrors used on the offline host: pip→tuna, apt→163, docker→`docker.1panel.live`,
  CPU torch→`download.pytorch.org/whl/cpu`. Base‑image digests were repinned to
  currently‑fetchable ones (documented in the Dockerfiles).



