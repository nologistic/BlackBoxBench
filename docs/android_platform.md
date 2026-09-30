# Android black-box exploration and APK reproduction

## Content Seeds (种子素材)

Media/library apps cold-start onto empty screens, which flattens exploration
onto empty-list UI (established in the 2026-09-20 vinyl / fossify_gallery
retrospective). `AndroidTargetSpec.seed_files` pushes content from
`android_seeds/<app_id>/` into the emulator **after the APK install and
before the first launch**, and broadcasts `MEDIA_SCANNER_SCAN_FILE` for
media files so MediaStore indexes them immediately.

```python
seed_files = [{"src": ".", "dst": "/sdcard/Music"}]   # src is seeds-relative
```

- All material is generated locally: `python3 scripts/build_android_seeds.py`
  (ffmpeg audio/video, PIL photos with EXIF, hand-rolled EPUB/OPML, genanki decks)
- Declarations: `python3 scripts/register_android_seeds.py` (idempotent,
  writes targets.json)
- Covers 11 targets: vinyl / fossify_gallery / snapseed / vlc / antennapod /
  markor / librera / mj_pdf / material_files / ankidroid / feeder
- **Requires a controller restart to take effect** (running processes hold
  the old code)

Android is a parallel vertical slice beside the existing web benchmark. It
uses the same evidence-validated topology and recorder, but has its own target
registry, trusted Emulator Runtime, MCP conditions and APK output domain.

```text
protected APK -> trusted emulator -> PNG/touch trace -> finalized topology
              -> filtered handoff + synthetic materials -> Compose project
              -> offline build -> review emulator -> accepted APK
```

## Host preparation

The host needs Hyper-V-compatible virtualization, JDK 17 and a project-local
Android SDK/AVD. Nothing is installed into a system Android SDK by the setup
script.

```powershell
# JDK 17 may be supplied through BBB_JAVA_HOME. Then review and accept the
# Google SDK licenses before the large SDK/system-image download:
$env:BBB_JAVA_HOME = "$PWD/vendor/jdk17"
vendor/python/python.exe scripts/setup_android.py --accept-licenses

# Separately prepare the pinned, offline reproduction image. This is the only
# build-image step that accepts the Android SDK licenses:
vendor/python/python.exe scripts/build_android_reproduction_image.py --accept-licenses

# Verify paths without downloading:
vendor/python/python.exe scripts/setup_android.py --preflight

# Build the deterministic reference APK (Docker Desktop must be running):
vendor/python/python.exe scripts/build_android_sample.py

# Optional operator E2E: clone an AVD, install/start the sample, capture pixels,
# exercise one coordinate input, restart the App, and tear the clone down:
vendor/python/python.exe scripts/android_runtime_smoke.py `
  --output "$env:TEMP\bbb_android_runtime_smoke"
```

Both license-accepting commands require the explicit flag. Normal exploration,
generation and review use the already prepared SDK, AVD and offline image; they
never download dependencies or accept licenses automatically.

The runtime resolves `BBB_ANDROID_SDK_ROOT`, `ANDROID_SDK_ROOT`, then
`vendor/android/sdk`. The base AVD resolves `BBB_ANDROID_AVD_TEMPLATE`, then
`vendor/android/avd/BBB_Base.avd`.

## Guest UI language

Every emulator the project starts — exploration, reproduction review and
third-party evaluation — pins the guest UI language from `BBB_ANDROID_LOCALE`
(default `zh-CN`). The AVD is re-cloned from the template on each start, so a
language set by hand inside one session would vanish with that clone; the
runtime therefore applies it on every boot, before the target APK is installed.

```powershell
$env:BBB_ANDROID_LOCALE = "en-US"   # or "" to keep the system image default
```

The value is validated as a BCP-47 subset because it reaches an `adb shell`
command. Applying it restarts the guest framework (never the emulator) and waits
for the package manager to come back, so the install that follows cannot race
it. A locale that the image refuses is non-fatal: a wrong UI language degrades
checklist comparability, not isolation.

Keeping this pinned is what allows an operator-authored checklist, the strings an
exploring Agent sees, and the strings a judge sees to be the same language.


## Register a local APK

```powershell
vendor/python/python.exe scripts/android_target.py register `
  --id my_local_app --apk D:\targets\app.apk `
  --description "Local test app" `
  --package com.example.app --activity .MainActivity
```

Omit package/activity when `apkanalyzer` is installed and should extract them.
Targets default to offline. The trusted runtime installs guest IPv4/IPv6 rules
before installing the APK: offline denies all egress; `--network public` rejects
loopback, host/private/link-local/reserved networks while allowing public
destinations and normal redirects. Public mode additionally requires both
`BBB_ANDROID_NETWORK_GUARD` and `BBB_ANDROID_PUBLIC_PROXY`; either missing causes
startup to fail rather than falling back to unrestricted networking. Protect
real values with repeated `--protected-string` and optional
`--protected-regions-json` screenshot rectangles.

The project includes a trusted verifier for the guest firewall. A typical local
public-mode configuration is:

```powershell
$env:BBB_ANDROID_NETWORK_GUARD = "$PWD/scripts/android_network_guard.py"
$env:BBB_ANDROID_PUBLIC_PROXY = "http://127.0.0.1:8899"
```

The runtime maps host loopback to the Emulator host alias and permits only that
proxy endpoint plus the Emulator's fixed DNS proxy on port 53 ahead of the
private-range reject rules. DNS answers resolving to private destinations are
still rejected. The HTTP proxy itself must be a public-only proxy operated
outside the Agent environment.

APKs, profiles and registry metadata live below `runs/android_targets/`. They
are never mounted into Agent or reproduction containers.

### Extract a pre-installed system app

Some useful targets ship inside the emulator system image rather than as a
standalone file. The trusted extractor boots a throwaway clone, pulls the APK
into the protected target tree and registers it:

```powershell
# See what the system image offers
vendor/python/python.exe scripts/android_extract_system_app.py --list

# Extract and register (non-ASCII brief/description via --meta-json)
vendor/python/python.exe scripts/android_extract_system_app.py `
  --id google_clock --package com.google.android.deskclock `
  --meta-json runs/android_targets/google_clock_meta.json --replace
```

The android-35 `google_apis` image contains no Calculator; Google Desk Clock
(`com.google.android.deskclock`) is the simplest fully offline stock app and is
registered as `google_clock`. Split-APK packages are rejected rather than
partially extracted.

### Datasets vs the bundled sample

Two different kinds of target coexist, and they must not be confused:

| Target | Kind | Role |
|---|---|---|
| `android_commerce_demo` | `sample` | Written by this project. A deterministic fixture for smoke tests and development. |
| `google_clock` | `external` | **The first real Android dataset.** Third-party software, the Android counterpart of `yuque_web` on the web side. |

A dataset is real software the project did not write, explored under a
human-authored checklist, and used for cross-condition comparison. Because it is
well-known software, the training-contamination caveat that applies to web live
targets applies here too: the exploration chain is still validated, but capability
conclusions must be discounted (see `docs/security_model.md`, threat T9).

Its checklist lives at `review_specs/google_clock.json`, alongside the web
checklist `review_specs/yuque_28_features.md`. Targets can be renamed once a
provisional id turns out to be misleading:

```powershell
vendor/python/python.exe scripts/android_target.py rename `
  --from provisional_id --to real_dataset_id
vendor/python/python.exe scripts/android_target.py unregister --id obsolete_id
```

Renaming preserves every recorded field (package, activity, policy, protected
strings, profile) and re-points the stored APK; the bundled sample cannot be
renamed or unregistered.


### Confirm and manually explore a target

```powershell
# Headless smoke check: install, capture, one tap, restart
vendor/python/python.exe scripts/android_target_smoke.py --app google_clock

# Headed manual session for an operator (no evidence, no topology, no handoff)
vendor/python/python.exe scripts/android_manual_session.py --app google_clock

# Real launch chain through the condition's MCP, stopping at the first frame
vendor/python/python.exe scripts/android_launch_preflight.py `
  --app google_clock --condition nograph --close
```

The manual session is not an exploration condition. It exists so an operator can
use the App directly and author `review_specs/<target>.json` by hand. Commands
inside it: `s` screenshot, `r` reset app data, `h` restart App, `q` quit.


For a registered public target that needs login, run
`vendor/python/python.exe scripts/android_live_login.py --app <target_id>`.
It opens a trusted headed emulator and captures a golden profile after the
operator confirms login. A login-maintenance lease prevents it from overlapping
exploration of the same target.

## Agent conditions

```powershell
# Managed baseline
vendor/python/python.exe -m agents.android_baseline.install --cli codex

# Ablation variant (record-free)
vendor/python/python.exe -m agents.baseline_nograph.install --cli codex
```

Invoke `$android-blackbox-explorer android_commerce_demo` or
`$baseline-nograph android_commerce_demo` in a new Agent task. Formal
experiments use one condition per task. Both MCPs may remain registered: each
Skill forbids discovering or borrowing another condition, while separate tasks
may run concurrently. Each runtime clones its own AVD and reserves a distinct
emulator port.

### Concurrency and host resources

Every session kind — exploration, manual, smoke, evaluation, reproduction
review — competes for the same emulator console ports on one host, so all of
them reserve from a single namespace at `runs/android_targets/port_locks/`. A
reservation whose owning process is gone is reclaimed automatically, and the
console/adb port pair is probed as well, because a killed launcher can leave an
orphaned qemu still answering on that serial. Handing the same serial to two
sessions is what makes `adb -s emulator-XXXX` address a dead or foreign device
and surface as `screencap` exit -1 mid-exploration.

An emulator also refuses to boot when the host cannot feed it:

```powershell
$env:BBB_ANDROID_MIN_FREE_MB = "5120"   # default; 0 disables the check
```

Under memory pressure adb does not fail cleanly — individual commands start
returning exit -1 or timing out while the device still looks alive. An explicit
refusal before the AVD clone is both honest and cheap to recover from.

Each session clones the base AVD, which costs several GB. Those clones — like
Chromium's user-data dirs on the web side — are volatile runtime state, so they
are created **outside the repository**, under
`%TEMP%/blackboxbench-scratch` (override with `BBB_SCRATCH_DIR`). `runs/` is the
evidence tree: frames, traces and topology, not emulator disk images or browser
cache. Keeping them inside a session directory used to make every teardown
delete thousands of workspace files, and left 7.8 GB behind when deletion failed
— one of those leftovers came from a session that had finalized cleanly.

Scratch directories carry their owning PID in the name, so a clone left by a
crashed or force-killed session is reclaimed before the next emulator boots,
without any registry. Login maintenance is the one exception: its clone stays at
a stable path under `runs/android_targets/login_work/` because
`scripts/android_live_login.py` copies the golden profile out of it.

Inspect and reclaim this state with:

```powershell
vendor/python/python.exe scripts/android_locks.py            # report only
vendor/python/python.exe scripts/android_locks.py --reclaim  # drop stale locks
vendor/python/python.exe scripts/android_locks.py --reclaim --kill-emulators
```

A port reported as answering while its lock is stale is an orphaned qemu: the
reservation can be reclaimed, but the slot stays unusable until that process is
killed.

### Parallel Agents on one target

Multiple Agents may explore the same target at the same time. Verify a host with:

```powershell
vendor/python/python.exe scripts/android_parallel_check.py --app google_clock
```

It starts two conditions concurrently, exercises real taps, and reports the
shared reservations while both boot. What parallel runs rely on, per resource:

| Resource | Sharing rule |
|---|---|
| Target lease | `explore-*` allows many holders; `login` is exclusive |
| Emulator port | One host-wide namespace, exclusive create + port probe |
| AVD clone | One per session, in scratch space, reclaimed by owner PID |
| Target APK | Read-only, shared safely |
| `app_output/<handoff_id>/` | Random suffix plus exclusive create — a collision aborts, never overwrites |
| `app_output/evaluations/<run_id>/` | Same exclusive-create rule |
| Android build image | Serialized by a cross-process file lock |
| Host memory | Admission check before boot, reserving headroom for emulators still booting so two simultaneous starts cannot overcommit |
| Scratch disk | Shared; each clone is several GB, so watch `%TEMP%` capacity |

### Transient device failures

A long exploration issues thousands of adb calls and the emulator occasionally
drops one. Such a blip is an environment artefact, not an observation about the
Agent, so it never ends a session on its own:

1. The runtime retries the command after waiting for the device.
2. If it still fails, the runtime asks whether the device is *responsive*.
3. A responsive device yields a recoverable failure — the Agent is told this one
   step did not land (HTTP 409) and may simply retry. An unresponsive one ends
   the session, which immediately hands back the emulator process, target lease
   and port reservation.

Nothing about *how* the failure happened reaches the Agent. A raw adb error
carries the tool path, the command line and the emulator serial; the runtime
converts it to a `DeviceError` whose message is a neutral sentence and keeps the
technical cause in the local trace only. Disclosing it would tell the Agent that
ADB exists and what it was asked to do, which the pixels-only boundary forbids.

Blips are recorded as `environment_blip` events in `actions.jsonl`, and a
sustained latency drift as `environment_degrading`, so a later review can tell an
environment problem from Agent behaviour (see `docs/evaluation_contract.md` §11).

### Text input

`type_text` injects ASCII only. `adb shell input text` resolves characters
through the device KeyCharacterMap, which has no CJK entries: the emulator
answers any CJK argument with `NullPointerException: Attempt to get length of
null array` regardless of quoting or escaping (reproduce with
`scripts/android_typing_diagnose.py`). Non-ASCII text is therefore refused as a
recoverable invalid action — the session keeps running — and the Agent is
pointed at the App's on-screen keyboard, which plain taps can reach. That is
also what a person without an IME shortcut does, so no capability is lost.

For formal runs, launch the Agent client in the condition-specific strict
container (use an image containing the chosen CLI):

```powershell
vendor/python/python.exe -m agents.android_baseline.agent_runtime `
  --app android_commerce_demo --image bbb-agent-runtime
vendor/python/python.exe -m agents.baseline_nograph.agent_runtime `
  --app android_commerce_demo --image bbb-agent-runtime
```

These containers mount only the generic TCP relay and that condition's Skill;
they do not mount the repository, Docker socket, target APK, AVD, runs, or other
condition. Each host MCP binds loopback and uses a per-run token.

## Outputs and privacy

Each reproduction lives at `app_output/<handoff_id>/`:

- `project/`: the only Agent-writable Android project.
- `review/`: saved review frames and the accepted report.
- `artifacts/app-debug.apk`: final benchmark-signed installable APK.

The container sees the project at `/workspace`, filtered exploration at
`/exploration`, topology at `/input/functional_topology.json`, and fictional
materials at `/materials/common` and `/materials/mobile`. It has no network and
does not mount the repository, target APK, emulator profile or ground truth.

## Evaluation

The independent `app-review` condition installs a reproduced APK in a fresh
offline emulator. An LLM judge adaptively observes visible frames, chooses
coordinate-level actions, and grades every item in a human checklist as
`full`, `partial`, `placeholder`, or `broken` (完整/部分/占位/失效). Every result
must cite saved observations; persistence requirements additionally need a
before/after/restart-or-reentry evidence triple.

Install the judge MCP and Skill once, then invoke it in a new Agent task:

```powershell
vendor/python/python.exe -m agents.app_review.install --cli codex
# In a new task:
# $app-review android_commerce_demo.json
```

The judge reads only `app_output/<handoff>/artifacts/app-debug.apk` and a
checklist below `review_specs/`; it never receives the target APK, exploration
MCPs, source code, UI trees, ADB or network output. The full report is written
below `app_output/evaluations/<run_id>/`. See
`docs/evaluation_contract.md` for the shared checklist and report schema.

The deterministic Commerce Demo stores login, favorites, cart and order state
in an on-device SQLite database. Reset clears that database; restart and cold
re-entry preserve it, so persistence evidence exercises a real local store.
