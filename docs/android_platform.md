# Android black-box exploration and APK reproduction

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

For a registered public target that needs login, run
`vendor/python/python.exe scripts/android_live_login.py --app <target_id>`.
It opens a trusted headed emulator and captures a golden profile after the
operator confirms login. A login-maintenance lease prevents it from overlapping
exploration of the same target.

## Agent conditions

```powershell
# Managed baseline
vendor/python/python.exe -m agents.android_baseline.install --cli codex

# Improved condition
vendor/python/python.exe -m agents.android_our_method.install --cli codex
```

Invoke `$android-blackbox-explorer android_commerce_demo` or
`$android-our-method android_commerce_demo` in a new Agent task. Formal
experiments use one condition per task. Both MCPs may remain registered: each
Skill forbids discovering or borrowing another condition, while separate tasks
may run concurrently. Each runtime clones its own AVD and reserves a distinct
emulator port.

For formal runs, launch the Agent client in the condition-specific strict
container (use an image containing the chosen CLI):

```powershell
vendor/python/python.exe -m agents.android_baseline.agent_runtime `
  --app android_commerce_demo --image bbb-agent-runtime
vendor/python/python.exe -m agents.android_our_method.agent_runtime `
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

`app_evaluation.AppEvaluator` installs an APK in a fresh emulator and grades a
human checklist using only visible frames and coordinate input. Results use
`full`, `partial`, `placeholder`, and `broken` (完整/部分/占位/失效) and must cite
saved observation numbers. The deterministic checklist is
`review_specs/android_commerce_demo.json`.

The evaluator also has a runnable operator entry point. Omitting `--plan`
prints the checklist and coordinate-plan schema without starting an emulator;
supplying a plan installs the APK, executes only screenshot/coordinate steps,
and writes the four-level report:

```powershell
vendor/python/python.exe -m app_evaluation `
  --apk app_output/<handoff>/artifacts/app-debug.apk `
  --package com.blackboxbench.reproduction --activity .MainActivity `
  --checklist review_specs/android_commerce_demo.json
```

The deterministic Commerce Demo stores login, favorites, cart and order state
in an on-device SQLite database. Reset clears that database; restart and cold
re-entry preserve it, so persistence evidence exercises a real local store.
