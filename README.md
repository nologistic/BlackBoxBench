# Recreate Anything

**A black-box benchmark that asks AI agents to recreate unknown apps from pixels and coordinate-level input alone.**

The agent gets no DOM, no accessibility tree, no logs, no APK — only screenshots and a synthetic "hand".
It exists to answer one question: **when an agent may only look and act the way a human does, how far can it recreate an unfamiliar app?**

<p>
  <img alt="platform" src="https://img.shields.io/badge/platform-Web%20%7C%20Android-1f6feb">
  <img alt="python" src="https://img.shields.io/badge/python-3.12-3776ab">
  <img alt="judge" src="https://img.shields.io/badge/judge-gpt--6--astra-6e40c9">
  <img alt="metrics" src="https://img.shields.io/badge/metrics-13%20standardized-2ea043">
  <img alt="conditions" src="https://img.shields.io/badge/conditions-managed%20%2F%20nograph-d29922">
  <img alt="status" src="https://img.shields.io/badge/status-active%20development-orange">
</p>

**English** · [简体中文](README.zh-CN.md)

---

## Table of contents

- [What it measures](#what-it-measures)
- [End-to-end pipeline](#end-to-end-pipeline)
- [Condition matrix](#condition-matrix)
- [Evaluation](#evaluation)
- [The 13 standardized metrics](#the-13-standardized-metrics)
- [Minimal architecture](#minimal-architecture)
- [Quick start](#quick-start)
- [Repository layout](#repository-layout)
- [Non-negotiable boundaries](#non-negotiable-boundaries)
- [Documentation](#documentation)

---

## What it measures

Most GUI-agent benchmarks allow reading the widget tree, the DOM, or the accessibility APIs — or calling the app's own HTTP API.
That makes tasks easier and conclusions incomparable: you cannot tell whether a model *understood the interface* or merely *read the structure*.

Recreate Anything collapses the observation channel to the minimum:

```text
Unknown App ──pixels──▶  Agent  ──human-like HID──▶  Unknown App
                            │
                            ├── evidence ──▶ Functional Topology (managed condition)
                            │                     │
                            ▼                     ▼
                    Reproduction Workspace ──▶  LLM Judge  ──▶  Metrics
```

- **Input**: a PNG screenshot (a single frame)
- **Output**: coordinate-level click / drag / touch / keystroke
- **Forbidden**: DOM, accessibility, script execution, selectors, URLs, network, cookies, storage, ADB, logs, decompilation
- **Deliverable**: an installable APK or a runnable static site, plus a scoreable functional topology

## End-to-end pipeline

| Stage | What happens | Artifacts |
|---|---|---|
| **1. Explore** | The agent drives the target inside a controlled runtime, under an action budget and a hard boundary | Frames + action trace (JSONL) |
| **2. Record** (managed only) | The agent files states, features, data, edges and hypotheses via discovery tools | Evidence-validated functional topology |
| **3. Reproduce** | On finalize, an isolated workspace rebuilds the app from the topology | Android: Kotlin + Compose project and signed APK<br>Web: static site |
| **4. Self-review** | Pixel re-checks on a second, independent device/browser; iterate | `review_summary.json` (rounds, decisions) |
| **5. Evaluate** | An independent LLM judge operates the reproduction against a hand-written checklist | `evaluation_report.json` (four grades) |
| **6. Measure** | All 13 metrics are computed offline from artifacts | Comparable tables across models and conditions |

The reproduction workspace is **physically isolated** from the exploration environment (`network=none`, read-only rootfs, capability drop). Its only inputs are three read-only mounts:

```text
managed finalized topology  → /input/functional_topology.json   (read-only)
public fictional materials  → /materials                        (read-only)
filtered exploration artifacts → /exploration                   (read-only)
reproduction output         → /workspace                        (read-write)
```

## Condition matrix

Two pipelines × two conditions = four combinations. **The only manipulated variable is whether structured recording tools are available.**

| Platform | Condition | Skill | What the agent gets | Recording duty |
|---|---|---|---|---|
| Web | baseline | `blackbox-explorer` | PNG, coordinate input, discovery MCP | Record along a prescribed DAG |
| Web | ablation | `web-nograph` | PNG, coordinate input (no discovery tools) | Free-form or none |
| Android | baseline | `android-blackbox-explorer` | PNG, coordinate touch, discovery MCP | Record along a prescribed DAG |
| Android | ablation | `baseline-nograph` | PNG, coordinate touch (no discovery tools) | Free-form or none |

The ablation condition is the baseline **minus seven discovery tools**:

```text
record_state · record_feature · record_data · record_edge
record_hypothesis · resolve · revise
```

Exploration discipline (budgets, boundaries, finalize flow) and the whole reproduction pipeline are identical, so *"does the agent keep structured notes?"* is the single clean variable — which makes it possible to quantify how recording tools affect exploration quality and reproduction fidelity.

A third, self-tooling condition (the agent designs its own tools and workflow) is archived under `self_explorer/` and is no longer an active experimental entry point; its code and docs are kept so historical runs remain reproducible.

## Evaluation

Evaluation is carried out by an independent LLM judge (`gpt-6-astra`, `xhigh` reasoning). It receives a hand-written functional checklist and operates the reproduction like a user would, grading each requirement.

| Grade | Weight | Meaning |
|---|:---:|---|
| `full` | 1.00 | Fully implemented, including state persistence |
| `partial` | 0.50 | Core works but incomplete |
| `placeholder` | 0.25 | UI exists but does nothing |
| `broken` | 0.00 | Missing or erroring |

The weighted score **WFS = Σ(weight × grade count) / number of requirements** aggregates across targets.

Checklists are written by humans and cover typical usage paths:

```text
review_specs/<app_id>.json
  ├─ checklist_id / platform
  ├─ features[]   each requirement: id / name / steps / expected
  │                                 / persistence / multi_user
  └─ exclusions[] explicit exclusions: feature / treatment / reason / covered_by
```

The repository currently ships **57 checklists**: 28 for Web targets and 29 for Android targets.

## The 13 standardized metrics

Every metric is computed offline from produced artifacts; no extra agent interaction is required.

| Family | Metrics | Source |
|---|---|---|
| **A Fidelity** | Grade distribution · WFS · persistence-WFS | `evaluation_report.json` |
| **B Exploration** | Coverage · effective-action rate · invalid-action rate · exploration span | `actions.jsonl` + coverage judging |
| **C Visual similarity** | CLIP-L/14 coverage-discounted similarity | exploration frames × evaluation frames |
| **D Process** | Revision rounds · hallucination rate | `review_summary.json` |
| **E Robustness** | Platform-attributed failures | `crash_report.json` |
| **F Efficiency** | Per-stage tokens · tokens per fidelity point | CLI usage logs |

- **Coverage**: a visual judge passively measures the intersection of exploration keyframes and the human checklist.
- **Visual similarity**: per checklist item, cosine similarity between exploration and reproduction frames (CLIP); items never covered or never reproduced score zero.
- **Hallucination rate**: share of requirements the reproduction claims but the judge cannot actually reach.

## Minimal architecture

```text
Agent framework (any MCP client: kimi / zcode / dsh / codex / your own)
  └─ MCP or Python SDK
       └─ Controller (benchmark.server)
            ├─ Session  budget / lifecycle / action validation
            ├─ Runtime  screenshot in, HID out
            │    ├─ LocalChromiumRuntime      local development
            │    ├─ LiveChromiumRuntime       live websites
            │    ├─ DockerX11Runtime          isolated web evaluation
            │    └─ AndroidEmulatorRuntime    Android emulator
            ├─ Recorder  frames + JSONL trace
            └─ Topology  evidence-validated discoveries
                 └─ Reference App / Android APK
```

Self-review convergence is bounded by two gates:

| Gate | Env var | Default | Meaning |
|---|---|:---:|---|
| Observation budget | `BBB_REVIEW_BUDGET` | 150 | Screenshot observations allowed per self-review |
| Revision cap | `BBB_REPRO_REVISIONS` | 6 | Maximum find-and-fix cycles |

## Quick start

```bash
# 1. Environment (Python 3.12)
python3 -m venv vendor/python
vendor/python/bin/python3 -m pip install -r requirements.txt

# 2. Bootstrap (fetch emulator, Chrome, materials; idempotent)
vendor/python/bin/python3 scripts/bootstrap.py

# 3. Start the controller
vendor/python/bin/python3 -m benchmark.server --port 7800

# 4. Run one exploration (Android ablation condition shown)
vendor/python/bin/python3 scripts/launch_batch.py \
    explore kimi nonogram nograph

# 5. Evaluate a reproduction
vendor/python/bin/python3 scripts/launch_batch.py \
    eval nonogram sess_20260101_000000_abcdef-cdef01 nonogram.json

# 6. Compute metrics
vendor/python/bin/python3 scripts/compute_metrics.py --runs runs
```

Android targets must be registered first. This repository ships metadata for 25 targets in
`android_targets/registry.json`, with APKs under `android_apks/`:

```bash
vendor/python/bin/python3 scripts/android_launch_preflight.py --app ankidroid
vendor/python/bin/python3 scripts/launch_batch.py explore codex ankidroid nograph
```

Handy helpers:

```bash
vendor/python/bin/python3 scripts/bench_status.py          # batch status
vendor/python/bin/python3 scripts/check_batch_health.py    # health check
docker compose up -d                                       # isolated runtimes
```

## Repository layout

```text
agents/              agent entry points per condition (MCP server / skill installer)
  android_baseline/    Android baseline (full discovery toolset)
  baseline_nograph/    Android ablation
  web_nograph/         Web ablation
  app_review/          review entry for Android reproductions
  web_review/          review entry for web reproductions
  cli_explorer/        self-tooling condition (archived)
benchmark/           controller: sessions, runtimes, recorder, topology, orchestration
app_evaluation/      four-grade scoring and checklist loading
app_reproduction/    Android reproduction workspace and build
web_evaluation/      evaluation of web reproductions
reproduction/        web reproduction workspace
docker/              isolated runtimes and image definitions
scripts/             bootstrap, launch, diagnostics, metrics, export utilities
review_specs/        hand-written functional checklists (57) and exclusions
android_apks/        target APKs with checksums
android_seeds/       seed materials for target apps
android_targets/     target registry (25 targets)
sample_apps/         bundled demo app (includes ground truth)
schemas/             schemas for topology, checklists and evaluation reports
docs/                architecture, protocols, platform, metrics, security model
tests/               unit and contract tests
```

## Non-negotiable boundaries

- The agent may only reach `/agent/{sid}/...` for screenshots, budgets, coordinate input and discovery tools.
- DOM, accessibility, script execution, selectors, URLs, network, cookies and storage are forbidden.
- Local CDP is limited to Page navigation/screenshot, Input, a fixed viewport and `Browser.close`.
- Action receipts carry no semantic feedback such as "what was clicked".
- A `confirmed` feature or edge must cite a real step/frame of the current session.
- `sample_apps/*/ground_truth.json` is neither served over HTTP nor mounted into agent containers.
- Android agents never touch APK files, ADB, the widget tree, logs, or decompiled output.

See [docs/security_model.md](docs/security_model.md).

## Documentation

| Document | Contents |
|---|---|
| [architecture.md](docs/architecture.md) | Controller, runtimes, sessions, artifact model |
| [api_contract.md](docs/api_contract.md) | Agent-facing HTTP / MCP contract |
| [topology_schema.md](docs/topology_schema.md) | Functional-topology data structures |
| [evaluation_contract.md](docs/evaluation_contract.md) | Scoring rules and four-grade definitions |
| [benchmark_metrics.md](docs/benchmark_metrics.md) | How the 13 metrics are computed |
| [android_platform.md](docs/android_platform.md) | Emulators, locks, snapshots, network policy |
| [security_model.md](docs/security_model.md) | Isolation boundaries and attack surface |
| [running_agent.md](docs/running_agent.md) | Plugging in your own agent |
| [adding_reference_app.md](docs/adding_reference_app.md) | Adding a new target app |
| [dependency_requirements.md](docs/dependency_requirements.md) | Toolchain and image dependencies |

---

<p align="center">
  <sub>Recreate Anything — see only pixels, act only by hand, recreate anything</sub>
</p>
