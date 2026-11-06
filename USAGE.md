# Recreate Anything — Operator's Manual (Full Linux Deployment)

> Audience: operators running the full loop — black-box exploration → APK / web reproduction → independent evaluation → results pushed to the cloud — on this machine (or an identically laid out Linux host).
> Compiled against the **2026-09-28 machine environment**; every command below was verified locally. Machine-specific absolute paths (`/path/to`, `/home/user/...`) can be replaced on another host as long as the layout stays isomorphic.
> Authoritative architecture and contract documents live in `docs/` (`architecture.md`, `api_contract.md`, `evaluation_contract.md`, `android_platform.md`, `security_model.md`); this file is the **operations manual**.

---

## Table of contents

1. Quick start (TL;DR)
2. Host virtualisation setup
3. Virtual machine (Android AVD) and Chrome installation
4. Environment setup (repository / Python / environment variables)
5. Target registration and evaluation checklists
6. Controller and MCP services
7. Downloading and configuring agent versions
8. Skill installation
9. Exploration commands
10. Evaluation commands
11. Publishing results (linux branch)
12. Appendix

---

## 1. Quick start (TL;DR)

```bash
export REPO=/path/to

# 0) Environment variables (data-disk policy; must be sourced first)
source /path/to

# 1) Readiness self-check (~15s; add --boot for a warm emulator boot smoke test)
bash /path/to

# 2) Controller (start it if not running; MCP can self-start on demand, but a resident process is recommended)
cd "$REPO" && nohup vendor/python/bin/python3 -m benchmark.server --port 7800 >/tmp/bbb_controller.log 2>&1 &

# 3) Batch exploration: dsh (V4.1f) and zcode (GLM-5.3) each run nonogram under the nograph condition
cd /path/to && \
BBB_EXPLORE_CWD=/path/to BBB_BATCH_LOG_DIR=/path/to \
"$REPO/scripts/launch_batch.py" --max-parallel 2 \
  explore dsh nonogram nograph explore zcode nonogram nograph

# 4) Single evaluation (kimi/k3 as judge; install the app-review skill first, see §8)
cd /path/to && nohup env DISPLAY=:0 /home/user/.local/bin/kimi -m kimi-code/k3 \
  -p '/skill:app-review minesweeper(handoff_id=sess_XXXXXXXX_XXXXXX_XXXXXX-XXXXXX , checklist=minesweeper.json)' \
  > /tmp/judge.log 2>&1 &

# 5) Health check and publish results
"$REPO/vendor/python/bin/python3" "$REPO/scripts/check_batch_health.py"
cd "$REPO" && git add -A && git commit -m "publish results" && git push origin linux
```

---

## 2. Host virtualisation setup

### 2.1 Hardware and OS baseline (measured on this machine)

| Item | This machine | Notes |
|---|---|---|
| CPU | Sized by the number of concurrent targets | Each concurrent emulator uses roughly 2–3 cores |
| Memory | Sized by the number of concurrent targets | Each resident emulator takes ~4.1 GB (2 GB guest + ~2.1 GB qemu/swiftshader) |
| Data disk | `/path/to` (dedicated large volume) | Repository, scratch, caches, Docker root all live here |
| Root disk | `/` | Never write run data to the root disk |
| Kernel | 6.x | See the emulator version notes in §2.2 |

- Per-session disk usage: **an AVD clone of several GB** (kept in scratch, see §4.3) + roughly 80–90 MB of artifacts (`app_output/sess_*`) + witness frames.
- Suggested headroom: `disk ≥ 64 GB × concurrency`, `memory ≥ 5 GB × concurrency` (including slack).

### 2.2 KVM (required by the Android emulator)

```bash
ls -l /dev/kvm                  # expect crw-rw---- root kvm
sg kvm -c "true" && echo KVM-OK # this is exactly the check preflight.sh uses
groups | tr ' ' '\n' | grep -x kvm
```

- Not in the kvm group: run `sudo usermod -aG kvm $USER` and **log in again** (a new session) before it takes effect.
- If you are already logged in but the group is not active, wrap the controller/emulator launch in `sg kvm`: `BBB_SG_GROUP=kvm scripts/controller_supervisor.sh` (rationale in lines 16–22 of that script).
- **Emulator version note**: on some hosts emulator **37.x can never start a vCPU**; pin **36.3.10** (see the notes field in `requirements/android-toolchain.lock.json`). Before upgrading the emulator on a new machine, verify with `scripts/android_runtime_smoke.py`.
- The project does **not** perform an automatic KVM check: `scripts/verify_dependencies.py` only covers Python / Docker / toolchain. KVM is defined by the single `sg kvm` line in `preflight.sh`.
- The emulator runs **headless with swiftshader_indirect** (software rendering); **no GPU or display server is required**.

### 2.3 Resource admission and concurrency (automatic gates)

| Mechanism | Default | Source |
|---|---|---|
| Free-memory threshold before launch | `BBB_ANDROID_MIN_FREE_MB=4200` | `benchmark/config.py:73` |
| Reservation while an emulator is starting | `BBB_ANDROID_EMULATOR_MB=4100` | `benchmark/config.py:85` |
| Cross-process boot concurrency quota | `BBB_ANDROID_BOOT_CONCURRENCY=16` | `benchmark/config.py:97` |

- The memory check and the reservation happen **under the same lock**; exceeding the limit rejects the launch with `insufficient free memory ...`.
- The boot quota uses file-lock slots (`benchmark/filelock.py:57`); exceeding it **queues** instead of failing. Batch `--max-parallel` defaults to 16, aligned with the quota.
- Rule of thumb: 16 concurrent boots peak at roughly 40 cores and reserve about 66 GB of memory.

### 2.4 Port plan

| Port | Purpose |
|---|---|
| `7800` | Controller (Controller HTTP API) |
| Proxy port | Public proxy (required by targets with `network_policy=public`; port configured per host) |
| `8300` | Docker Runtime RPC (managed mode) |
| `5554–5680` (step 2) | Per-session emulator console port (adb = port + 1), globally unique via an `O_EXCL` port lock |
| `5584` | Snapshot-baking port (`scripts/build_android_snapshot.py`) |
| `8400` | Default port for operator UI / MCP relay (Docker mode) |
| WAN proxy | Used for GitHub and other external traffic (a separate path from the public proxy; port configured per host) |

### 2.5 Network policy (`network_policy`)

- `offline`: the target runs in airplane mode; no proxy needed.
- `public`: **requires both** ① `BBB_ANDROID_NETWORK_GUARD` (default `scripts/android_network_guard.py`) and ② `BBB_ANDROID_PUBLIC_PROXY` (default `http://127.0.0.1:<proxy-port>`). Missing either one **rejects the launch** (`benchmark/android/runtime.py:637-675`).
- Proxy allow-list: only 80/443, and non-public addresses are rejected (DNS-rebinding defence). See `docker/public_web_proxy.py`.
- Check: `ss -ltn | grep "$PROXY_PORT"`; without a running proxy no `public` target can start.

## 3. Virtual machine (Android AVD) and Chrome installation

### 3.1 Android toolchain (pinned versions)

`requirements/android-toolchain.lock.json` pins:

| Component | Version |
|---|---|
| platform-tools | 37.0.1 |
| emulator | **36.3.10** (37.x is unusable on this host, see §2.2) |
| platforms | android-35 (rev 2) |
| build-tools | 35.0.0 |
| system-image | android-35;google_apis;x86_64 (rev 9) |
| AVD template | `BBB_Base` (pixel_6 / x86_64 / google_apis) |

Installation (first run; needs network and explicit licence acceptance):

```bash
cd /path/to
export BBB_JAVA_HOME="$PWD/vendor/jdk17"        # or the system JDK 17
vendor/python/bin/python3 scripts/setup_android.py --accept-licenses
vendor/python/bin/python3 scripts/setup_android.py --preflight     # post-install self-check
```

- Toolchain discovery order (`benchmark/android/toolchain.py:49-68`):
  - SDK: `BBB_ANDROID_SDK_ROOT` → `ANDROID_SDK_ROOT` → `ANDROID_HOME` → `vendor/android/sdk`
  - AVD template: `BBB_ANDROID_AVD_TEMPLATE` → `vendor/android/avd/BBB_Base.avd`
  - JDK: `BBB_JAVA_HOME` → `vendor/jdk17` → `JAVA_HOME`
- SDK + AVD total about 5.3 GB; nothing is installed into a system-level Android SDK.

### 3.2 AVD template, cloning and session lifecycle

- **One clone per session**, placed in scratch outside the repository; the AVD is always named `BBB_Snapshot` (`benchmark/android/runtime.py:474-504`) and the template stays read-only.
- Cloning: copy the template → fix the backslashes in `image.sysdir.1` → overlay the target's `profile_snapshot` → write `BBB_Snapshot.ini`.
- Scratch directories carry the owner PID (`<kind>_<label>_<pid>_<rand>`); when the owner dies the next start reclaims them (`benchmark/scratch.py`).

**Warm snapshot (strongly recommended, bake once)**:

```bash
vendor/python/bin/python3 scripts/build_android_snapshot.py
# Note: this script has no --help; it starts cold-boot baking immediately (5–7 min, uses port 5584)
# Output: vendor/android/avd/BBB_Base.avd/snapshots/bbb_base  → later sessions warm-boot in ~5s
```

- The runtime snapshot name is controlled by `BBB_ANDROID_SNAPSHOT` (default `bbb_base`; set it empty to disable snapshots and force a cold boot).
- A failed snapshot load automatically falls back to a cold boot and retries once (`runtime.py:599-611`).
- Cold-boot timeout `boot_timeout` defaults to 300s; the launch command line includes `-no-window -gpu swiftshader_indirect -dns-server 1.1.1.1,8.8.8.8` (`runtime.py:540`).
- Bring-up order: boot → network → locale (`BBB_ANDROID_LOCALE`, default zh-CN) → install APK → pre-grant permissions → data/profile → file picker → seed files → orientation → restart the app (`runtime.py:555-564`).

Smoke tests:

```bash
vendor/python/bin/python3 scripts/android_runtime_smoke.py                      # runtime smoke
vendor/python/bin/python3 scripts/android_launch_preflight.py --app nonogram    # real MCP path smoke
vendor/python/bin/python3 scripts/android_launch_preflight.py --app nonogram --condition nograph --close
```

### 3.3 Chrome (web reproduction / evaluation side)

- `python3 scripts/bootstrap.py` downloads **Chrome for Testing (stable)** into `vendor/chromium/` (Linux binary at `vendor/chromium/chrome-linux64/chrome`).
- Runtime lookup order (`benchmark/runtime/local_chromium.py:155-182`): `BBB_BROWSER` → `vendor/chromium/...` → system Edge/Chrome/`/usr/bin/chromium`; if nothing is found it prints “Run scripts/bootstrap.py or set BBB_BROWSER.”.
- In Docker managed mode the browser lives inside the container: the image ships `chromium + Xvfb` (`docker/Dockerfile.reference`), so no host Chrome is required.

### 3.4 Docker (managed mode and build sandbox, optional)

| Image | Purpose |
|---|---|
| `blackboxbench/controller:latest` | Controller container (`:7800`) |
| `blackboxbench/reference:latest` | Web reference (Xvfb + kiosk chromium + gateway + RPC) |
| `blackboxbench/agent:latest` | Agent container (`agents/` only, stdio MCP forwarding) |
| `blackboxbench/android-reproduction-workbench:1` | **Android build-stage sandbox** (used for reproduction builds) |

```bash
# Build the build-sandbox image (preflight checks that it exists)
vendor/python/bin/python3 scripts/build_android_reproduction_image.py --accept-licenses

# Local up/down helper (docker needs the sg docker shim; override.yml runs containers as the host uid so ./runs is writable)
bash /path/to up | status | logs | down | api
```

| Compose file | Purpose |
|---|---|
| `docker-compose.yml` | Production three-container deployment (reference + controller + agent, internal network isolation) |
| `docker-compose.self-explorer.yml` | Self-tooling condition (bundled samples, no network, device volume) |
| `docker-compose.self-explorer.url.yml` | Network override of the above for public-URL targets |
| `docker-compose.self-login.yml` | Operator-side native login capture through Docker (no agent) |
| `docker-compose.self-explorer.profile.yml` | Mounts the golden profile read-only into reference_raw |

- The local Docker data root already lives on the data disk (preflight section 1 checks `docker info --format '{{.DockerRootDir}}'`).

---

## 4. Environment setup (repository / Python / environment variables)

### 4.1 Clone and bootstrap

```bash
git clone https://github.com/nologistic/recreate-anything.git
cd recreate-anything
python3 scripts/bootstrap.py          # downloads vendor/python 3.12.14 + installs deps + pip install -e agent_sdk
vendor/python/bin/python3 scripts/verify_dependencies.py --full     # dependency acceptance (--full also checks 4 project images)
```

- `scripts/bootstrap.py` uses python-build-standalone (`PY_RELEASE=3.12.14`) and installs **everything under `vendor/`**, never touching the system Python.
- Linux interpreter: `vendor/python/bin/python3`; Windows: `vendor/python/python.exe`.

### 4.2 Python and dependencies

- Direct dependencies (`requirements.txt`, all pinned): `fastapi==0.115.12, uvicorn==0.34.2, pydantic==2.11.4, jinja2==3.1.6, pillow==11.2.1, imageio-ffmpeg==0.5.1, websocket-client==1.8.0, httpx==0.28.1, pytest==8.3.5, pytest-timeout==2.3.1, numpy==2.1.3, python-multipart==0.0.20`.
- Full closure: `requirements/runtime.lock.txt`; image digests: `requirements/container-images.lock.json`; Android toolchain: `requirements/android-toolchain.lock.json`.
- `pyproject.toml` requires `requires-python >= 3.12`; pytest only runs `tests/`.

### 4.3 Environment variables and disk layout (critical)

At **import time**, `benchmark/config.py:28-54` redirects `TMP/TEMP` to `<repo parent>/tmp` (an explicit environment variable wins; `BBB_KEEP_SYSTEM_TMP=1` opts out entirely), so every `tempfile` artifact lands inside the workspace.

This machine's policy (`/path/to`, **source it in every new terminal**):

```bash
source /path/to
# Exports:
#   BBB_DATA_ROOT=/path/to
#   TMP / TEMP / TMPDIR = $BBB_DATA_ROOT
#   PIP_CACHE_DIR / NPM_CONFIG_CACHE / XDG_CACHE_HOME = data disk
#   BBB_SCRATCH_DIR=/path/to      (AVD clones, browser profiles)
#   BBB_RUNS_DIR=/path/to  (evidence tree)
#   BBB_KEEP_SYSTEM_TMP=0
```

Core directory contract:

| Directory | Contents | Published? |
|---|---|---|
| `runs/` | Session evidence (frames, traces, topology), `android_targets/` (target registry) | **No** (`.gitignore` ignores `runs/*`, except `android_targets`) |
| `app_output/` | Android reproduction artifacts (`sess_<handoff>/`) and evaluation reports (`evaluations/<run_id>/`) | On demand (see §11) |
| `website_output/` | The only writable directory for web reproductions | On demand |
| `<scratch>` | AVD clones, browser profiles and other **volatile runtime state** | Never (and outside the repository) |
| `review_specs/` | Evaluation checklists (checklist JSON) | Yes |
| `android_apks/`, `android_seeds/` | APK material (26 targets) and seed files | Yes |
| `vendor/` | Python / Android SDK / JDK17 / Chromium | **No** (`.gitignore`) |

### 4.4 Readiness self-check (preflight.sh)

```bash
bash /path/to          # fast (~15s)
bash /path/to --boot   # additionally smoke-tests a warm emulator boot (~40s)
```

It runs 10 sections: ① Docker daemon / 4 images / compose validity / data root ② emulator + AVD + warm snapshot + **sg kvm** ③ target APKs and seeds ④ agent CLI availability and credentials (kimi/zcode) + a `launch_batch` command-construction dry run ⑤ port 7800 occupancy ⑥ network (mihomo `<proxy-port>`, GitHub proxy, direct access to the three vendor APIs) ⑦ disk and data root ⑧ repo branch / uncommitted changes / evidence dirs / stale locks ⑨ DAG input-channel regression ⑩ (`--boot`) warm-boot timing.

Exit codes: `0` = READY; `1` = BLOCKERS (listed one by one).

---

## 5. Target registration and evaluation checklists

### 5.1 Registering Android targets

```bash
vendor/python/bin/python3 scripts/android_target.py register \
  --id my_local_app --apk /path/to/app.apk \
  --description "Local test app" \
  --package com.example.app --activity .MainActivity
```

- Subcommands: `list / preflight / rename / unregister / register`. Key `register` flags:
  `--network {offline,public}` (default offline), `--orientation {auto,portrait,landscape}`, `--reset {clear_data,restart,snapshot}`, `--profile-snapshot`, `--protected-string`, `--protected-regions-json`, `--brief`, `--replace`.
- On-disk rules (`benchmark/android/targets.py:202-257`): the APK is copied to `runs/android_targets/artifacts/<id>/<sha256>.apk`; `seed = sha256[:16]`; profiles are copied to `profiles/<id>`; the registry is `android_targets/registry.json`.
- The agent-visible `list_targets` **does not** expose APK paths, package names or activities (leak prevention).
- Bulk restore / registration: `scripts/restore_android_apks.py` (restores APKs from `android_apks/`); seed files: drop material into `android_seeds/<app_id>/` and register it with `scripts/register_android_seeds.py` (e.g. `vinyl → /sdcard/Music`).
- Current state: the shipped registry provides **25** Android targets (`android_commerce_demo / ankidroid / antennapod / cashew / feeder / fossify_* / futoshiki / game2048 / google_clock / joplin / librera / loop_habit_tracker / markor / material_files / metronome / minesweeper / mj_pdf / nonogram / organic_maps / ...`).

Inspect them:

```bash
curl -s "http://127.0.0.1:7800/api/apps?platform=android" | head -c 400
vendor/python/bin/python3 scripts/android_target.py list
```

### 5.2 Evaluation checklists (`review_specs/*.json`)

Top-level structure (`app_evaluation/checklist.py:8-41`):

```json
{
  "checklist_id": "minesweeper",
  "platform": "android",
  "features": [
    {
      "id": "f01",
      "name": "Initialisation and counter",
      "steps": ["Launch the app", "Look at the top bar"],
      "expected": "The counter shows 10 remaining mines and the timer starts at 0",
      "persistence": "Settings survive an app restart"
    }
  ]
}
```

- `platform` ∈ `{web, android, any}`; every feature requires `id/name/steps/expected`, with optional `persistence` and `multi_user`; at most `MAX_FEATURES=200` entries.
- Web checklists may carry an `exclusions` section (strictly validated).
- 57 checklists exist today: Android examples include `minesweeper.json / nonogram.json / snapseed.json / vlc.json / tasks.json / markor.json`; web examples include `yuque_web.json / google_maps_web.json`.

---

## 6. Controller and MCP services

### 6.1 Controller (:7800)

```bash
cd /path/to
vendor/python/bin/python3 -m benchmark.server --port 7800            # 127.0.0.1 by default
# Resident supervisor (can borrow sg to escalate in sessions where the kvm group is not active):
BBB_SG_GROUP=kvm bash scripts/controller_supervisor.sh               # logs to /tmp/bbb_controller.log
```

- **MCP boots the controller on demand** (`benchmark/orchestrator/controller_process.py:222-269`, guarded by a cross-process lock so only one starts, loopback only), so starting it manually is optional — but a resident process is more stable.
- Startup self-healing: leftover `running` sessions are rewritten as `failed` and empty session directories are cleaned (`benchmark/server.py:43-110`).
- Key endpoints:
  - Agent channel (only `/agent/{sid}/...`): `observe`, `action`, `discovery/*`, `finalize`
  - Operator: `GET /api/sessions`, `GET /api/apps?platform=`, `GET /api/sessions/{sid}`, `POST /api/sessions/{sid}/{reset|reopen|close}`, `GET .../frames/{fid}.png`, `GET .../live.png`, `.../trace`, `.../topology`
- Status at a glance: `vendor/python/bin/python3 scripts/bench_status.py` or `curl -s 127.0.0.1:7800/api/sessions`.

### 6.2 MCP server inventory (stdio on demand, launched by each CLI)

| Condition | Module | Registered name (SERVER_NAME) | Transport |
|---|---|---|---|
| Web exploration (baseline) | `agents.cli_explorer.mcp_server` | `blackboxbench` | stdio (optionally `--tcp --token`) |
| Android exploration (baseline) | `agents.android_baseline.mcp_server` | `android-blackboxbench` | same |
| Android exploration (nograph) | `agents.baseline_nograph.mcp_server` | `android-blackboxbench-nograph` | same |
| Web exploration (nograph ablation) | `agents.web_nograph.mcp_server` | `blackboxbench-nograph` | same |
| Android evaluation (judge) | `agents.app_review.mcp_server` | `app-review` | stdio |
| Web evaluation (judge) | `agents.web_review.mcp_server` | `web-review` | stdio / tcp (127.0.0.1 + token) |

- Logs: appended to `runs/mcp_server.log` by default (override with `BBB_MCP_LOG`).
- Manual smoke tests:
  ```bash
  vendor/python/bin/python3 -m agents.baseline_nograph.mcp_server      # stdio; Ctrl-C to exit
  vendor/python/bin/python3 scripts/evaluation_mcp_smoke.py            # initialize→tools/list→list_checklists
  ```
- Key environment variables (MCP side): `BBB_APP_REVIEW_APK` (APK under evaluation), `BBB_ANDROID_APP_ID` (default android_commerce_demo), `BBB_REPRO_REVISIONS` (revision cap, default 3), `BBB_REPRODUCTION_NEW/PREWARM`.

### 6.3 Locks and workspace hygiene

```bash
vendor/python/bin/python3 scripts/android_locks.py                  # report only: port locks / leases / live emulators / scratch
vendor/python/bin/python3 scripts/android_locks.py --reclaim        # reclaim stale locks whose owner is gone
vendor/python/bin/python3 scripts/android_locks.py --reclaim --kill-emulators   # also kill orphan qemu (locked but port answers)
vendor/python/bin/python3 scripts/workspace_hygiene.py --clean-transient        # clean .test-tmp / .playwright-cli / root test_*.txt
```

- Session leases are **multi-holdable**: several exploration sessions may run against the same target; only login maintenance is mutually exclusive with exploration.
- Reminder: `.test-tmp/` is removed by `workspace_hygiene.py`, but **pytest's `--basetemp=.test-tmp/...` requires the directory to pre-exist** — recreate it with `mkdir .test-tmp` (see AGENTS.md).

---

## 7. Downloading and configuring agent versions

### 7.1 Support matrix (measured on this machine)

| CLI | Version | Real binary | Launcher | Config directory |
|---|---|---|---|---|
| **dsh** (DeepSeek Harness) | `0.1.5-rc.3` | `/path/to` (dedicated prefix install) | `~/.local/bin/dsh` | `~/.dsh/profiles/` |
| **zcode** (Z.AI / GLM) | `zcode-app-cli 3.12.3-26` | npm global (conda env `llm`, node v20.20.2 / npm 10.8.2) | `~/.local/bin/zcode` | `~/.zcode/{cli,v2}/` |
| **kimi** (kimi-code) | `2.1.1` | `~/.kimi-code/bin/kimi` | `~/.local/bin/kimi` | `~/.kimi-code/` |
| **codex** | `codex-cli 0.155.1` | `~/.local/share/codex/x86_64-unknown-linux-musl/bin/codex` | `~/.local/bin/codex` | `~/.codex/` |
| **opencode** | `1.18.31` | `~/.opencode/bin/opencode` | `~/.local/bin/opencode` | `~/.config/opencode/` |
| claude | (not installed) | — | — | the installer only prints commands |

### 7.2 Installation / upgrade

- **zcode**: `npm i -g zcode-app-cli@3.12.3-26` (or `@latest`). Models and credentials live in `~/.zcode/v2/provider_config.json` (international Z.AI Coding Plan / GLM).
- **dsh**: official package `@deepseek-ai/dsh`. This machine installs it into a **dedicated prefix** at `/path/to` (to avoid mixing with an older conda-installed copy that breaks plugin activation); `~/.local/bin/dsh` forwards to it.
- **kimi / codex / opencode**: each uses its official installer; all are wrapped by a `~/.local/bin/<name>` launcher here.
- **Launchers and proxies**: every launcher first sources `~/.local/bin/_agent-env {direct|auto}` and then execs the real binary:
  - kimi / zcode use `direct` (`api.moonshot.cn` / `api.z.ai` are faster when reached directly from this host);
  - codex / opencode use `auto` (probe the locally available proxy ports).
  - After upgrading a CLI, run `--version` and section 4 of `preflight.sh` to confirm the launcher still points at the new binary.

### 7.3 Unified reasoning settings (cross-agent alignment)

| Item | dsh (explore / reproduce) | zcode (explore / reproduce) | kimi (judge) |
|---|---|---|---|
| Model | `deepseek-flash` (DeepSeek-V41-Flash) | `GLM-5.3` | `kimi-code/k3` |
| Reasoning effort | `reasoningEffort: high` (= DeepSeek API top tier) | `reasoningLevel: max` | model default |
| Context window | `272000` | `272000` | — |
| Tool-call timeout | `600000 ms` | `600000 ms` | — |
| Run mode | headless profile | yolo / zcode-agent | one-shot `-p` |

> 272,000 is the cross-model convention set on 2026-09-24 (aligned with the codex official default). dsh pins `reasoningEffort` to `high` to avoid drift; zcode uses GLM's top tier `max` — both mean "the highest tier available for that model".

### 7.4 Where each CLI keeps its key configuration

**dsh**: `~/.dsh/profiles/headless-nograph/cordis.patch.yml` (for nograph exploration) and `headless` (for baseline exploration):

```yaml
# excerpt
defaultContextWindow: 272000
reasoningEffort: high
# MCP registration
command: /path/to
args: [-m, agents.baseline_nograph.mcp_server]
toolCallTimeoutMs: 600000
customSkillDirs:
  - /path/to
```

**zcode**: `~/.zcode/cli/setting.json`:

```json
{
  "mcp": { "servers": { "android-blackboxbench-nograph": {
    "command": "/path/to",
    "args": ["-m", "agents.baseline_nograph.mcp_server"],
    "cwd": "/path/to",
    "env": { "PYTHONPATH": "", "PYTHONNOUSERSITE": "1" },
    "timeoutMs": 600000, "enabled": true } } },
  "skills": { "enabled": true, "includeInstructions": true,
              "metadataBudget": 20000,
              "roots": ["/path/to"] }
}
```

**kimi**: `~/.kimi-code/mcp.json` (same `mcpServers` shape) plus `~/.kimi-code/skills/<skill>/`.
**codex**: `~/.codex/config.toml`:

```toml
[mcp_servers.app-review]
command = "/path/to"
args = ["-m", "agents.app_review.mcp_server"]
```

- **zcode plugin allow-list**: `browser-use` and `document-skills` (the latter bundles the image_search MCP) must be `disabled`, leaving only the tool surface the exploration needs:
  ```bash
  zcode plugins list
  zcode plugins disable browser-use@zcode-plugins-official
  zcode plugins disable document-skills@zcode-plugins-official
  ```

---

## 8. Skill installation

### 8.1 Installer matrix (writes configuration automatically)

Each condition ships its own installer `agents/<cond>/install.py`:

| Condition | Module | `--cli` support | SERVER_NAME | Skill name |
|---|---|---|---|---|
| Web exploration | `agents.cli_explorer.install` | kimi / claude / codex / codebuddy | `blackboxbench` | `blackbox-explorer` |
| Android exploration (baseline) | `agents.android_baseline.install` | + opencode | `android-blackboxbench` | `android-blackbox-explorer` |
| Android exploration (nograph) | `agents.baseline_nograph.install` | + opencode | `android-blackboxbench-nograph` | `baseline-nograph` |
| Android evaluation | `agents.app_review.install` | + opencode, plus `--checklist` | `app-review` | `app-review` |
| Web evaluation | `agents.web_review.install` | kimi / claude / codex / codebuddy | `web-review` | `web-review` |

Where things land (automatic):

| CLI | MCP config | Skill directory |
|---|---|---|
| kimi | `~/.kimi-code/mcp.json` | `~/.kimi-code/skills/<skill>` |
| codex | `~/.codex/config.toml` | `~/.codex/skills/<skill>` |
| codebuddy | `~/.codebuddy/.mcp.json` | `~/.codebuddy/skills/` and project-level `.codebuddy/skills/` |
| opencode | `~/.config/opencode/opencode.jsonc` | `~/.config/opencode/skills/<skill>` |
| claude | (only prints the `claude mcp add ...` command) | — |

Usage:

```bash
cd /path/to
vendor/python/bin/python3 -m agents.baseline_nograph.install --cli codex --app nonogram
vendor/python/bin/python3 -m agents.app_review.install   --cli codex --checklist minesweeper.json
vendor/python/bin/python3 -m agents.app_review.install   --uninstall --cli kimi      # uninstall
```

### 8.2 Manual wiring for dsh / zcode (not covered by the installers)

For these two, wiring means pointing their own configuration at the in-repo skill directory (**back up first**):

- **dsh**: point the profile's `customSkillDirs` at `<repo>/agents/baseline_nograph/skill` (see §7.4). Invoke it as `$baseline-nograph` inside dsh.
- **zcode**: `skills.roots` + `mcp.servers` in `setting.json` (see §7.4). Invoke it as `$baseline-nograph`; `zcode skills list` should show `baseline-nograph`.

### 8.3 Skill content, language and stage layout

- All skill bodies are **in English** (translated on 2026-09-28; the Chinese originals are backed up locally at `/path/to`).
- Four conditions map to four exploration skills: `android → android-blackbox-explorer`, `web → blackbox-explorer`, `nograph → baseline-nograph` (the Android ablation: the tool surface drops the 7 recording tools), `web-nograph → web-nograph` (the web ablation, same 7 tools removed; added 2026-09-29).
- Each skill directory may carry `agents/openai.yaml` (`short_description` / `dependencies.tools` declaring the MCP names it needs).
- Stage layout (nograph shown): exploration (`observe/tap/long_press/swipe/type_text/...`) → `finalize` to enter reproduction → `workspace_*` edits + `start_reproduction_review` → `review_*` revisit → `revise/accept` → `finish_reproduction`.
- The evaluation skill (`app-review`) is a separate condition: it only exposes pixels/coordinates plus `list_checklists / start_evaluation / observe / record_result / evaluation_status / finish_evaluation / abort_evaluation`, and **may never modify the subject under test**.

---

## 9. Exploration commands

### 9.1 Pre-flight

```bash
source /path/to
bash /path/to            # expect READY at the end
vendor/python/bin/python3 scripts/android_locks.py --reclaim
ss -ltn | grep -E ':7800'                            # controller is up
ss -ltn | grep -E ":$PROXY_PORT"                     # required when public targets are involved
curl -s 127.0.0.1:7800/api/apps?platform=android | head -c 200   # targets are registered
```

### 9.2 Single exploration (three ways)

```bash
# A) The batch launcher's single-shot form (recommended: builds the command, writes the log)
cd /path/to && BBB_EXPLORE_CWD=/path/to BBB_BATCH_LOG_DIR=/path/to \
  /path/to explore kimi minesweeper

# B) Launch the CLI directly (equivalent, manual)
cd /path/to
dsh --profile headless-nograph '$baseline-nograph nonogram'
zcode --prompt '$baseline-nograph nonogram'
kimi -p '/baseline-nograph nonogram'
codex exec --skip-git-repo-check -s danger-full-access '$baseline-nograph nonogram'

# C) Talk to MCP directly (for debugging): start the stdio server and send JSON-RPC by hand, or use --tcp
vendor/python/bin/python3 -m agents.baseline_nograph.mcp_server
```

> Skill prefix: dsh / codex / zcode use `$`, kimi and friends use `/` (normalised by `SKILL_PREFIX` in `launch_batch.py`).

### 9.3 Batch exploration (launch_batch.py)

```text
Usage:
  launch_batch.py [--jobs JSON] [--max-parallel N] [--wait] \
      [eval TARGET HANDOFF CHECKLIST] | [explore AGENT TARGET [android|web|nograph]] ...

Examples:
  # three explorations in one submission (at most 4 in parallel)
  scripts/launch_batch.py --max-parallel 4 \
      explore kimi markor explore dsh nonogram explore zcode librera

  # web condition (the 4th token is the condition)
  scripts/launch_batch.py explore codex ecommerce_demo web

  # web ablation (no-graph: no recording tools)
  scripts/launch_batch.py explore kimi ecommerce_demo web-nograph

  # this machine: dsh + zcode each run nonogram under nograph
  cd /path/to && BBB_EXPLORE_CWD=/path/to BBB_BATCH_LOG_DIR=/path/to \
    /path/to --max-parallel 2 \
      explore dsh nonogram nograph explore zcode nonogram nograph
```

- **Agent list**: `dsh | kimi | zcode | codex` (resolution order: `BBB_AGENT_<NAME>` environment variable > PATH > `/home/user/bin/<name>`).
- **Condition list**: `android` (default, android-blackbox-explorer), `web` (blackbox-explorer), `nograph` (baseline-nograph, the Android ablation), `web-nograph` (the web ablation: MCP without the 7 recording tools, see §6.2).
- **dsh + nograph** automatically rewrites `--profile headless` to `headless-nograph` (to prevent accidentally attaching the baseline MCP; see the 2026-09-27 incident note at lines 131-136 of the script).
- Related environment variables: `BBB_EXPLORE_CWD` (agent working directory), `BBB_BATCH_LOG_DIR` (log directory, default `/tmp`), `BBB_BATCH_PARALLEL` (default concurrency 16), `BBB_AGENT_<NAME>` (absolute CLI path).
- Log naming: `<LOG_DIR>/explore_<agent>_<target>[_<cond>]_<MMDD_HHMMSS>.log`; each task runs inside `script -qec` (so it gets a TTY) with `NO_PROXY=127.0.0.1,localhost` and `DISPLAY=:99` forced.
- The batch launcher **actively clears** `BBB_RUNS_DIR` so evidence is never written to the wrong place.
- `--jobs` accepts a JSON task table; `--wait` blocks until everything finishes and reports the exit code.

### 9.4 Monitoring and health

```bash
vendor/python/bin/python3 scripts/check_batch_health.py      # finds "interrupted workflows" (exit 1 = found)
vendor/python/bin/python3 scripts/bench_status.py            # controller session status
curl -s 127.0.0.1:7800/api/sessions | head -c 400
ls runs/sess_*/frames | wc -l                                # frame growth
pgrep -cf qemu-system ;  pgrep -cf mcp_server                # emulator / MCP counts
tail -f /path/to*_<MMDD_HHMMSS>.log     # live log
```

Three signals that a job is actually working: the log keeps growing, `frames/` gains new files, and the controller step counter increases.

### 9.5 Common failures and recovery

| Symptom | Action |
|---|---|
| `start_session` returns 503 / busy / timeout | Cold-boot queue (minutes when concurrency is high). **Wait 5 minutes and retry once**; do not hammer it |
| `404 session_not_found` / `410 session_closed` | The session is gone: `start_session(app_id=<same target>, resume_from=<old session>)`; if finalize was interrupted, simply retry `finalize` |
| Stale locks reject a launch | `scripts/android_locks.py --reclaim [--kill-emulators]` |
| Emulator wedged / port answers but locked | `--kill-emulators` clears orphan qemu processes |
| Controller died | `scripts/controller_supervisor.sh` or restart it manually; MCP boots a fresh controller; on restart leftover running sessions become failed |
| A job silently stalled | `check_batch_health.py` names them: `review/round_*` without `review_summary.json`, `evaluations/*/frames` without `evaluation_report.json`, `session.json` neither closed nor failed and idle for a long time |
| Out of disk / memory | Lower `--max-parallel`; clear scratch (`android_locks.py` reports usage); `workspace_hygiene.py --clean-transient` |

---

## 10. Evaluation commands

### 10.1 Contract highlights (read before judging; `docs/evaluation_contract.md`)

- Four grades: `full` (complete implementation) / `partial` (partial or incomplete) / `placeholder` (shell only, or fake data) / `broken` (missing or unusable).
- The judge decides **from pixel evidence only**; it may never modify the APK or page under test; every grade needs reviewable evidence (action + observation).
- **Judge-model uniqueness**: one comparison batch must use one judge model (default: `gpt-6-astra` + `xhigh`; on this machine `kimi-code/k3` stands in, and the three comparison reports share that same setting).
- The report must cover every checklist item; omissions are rejected by `finish_evaluation`.

### 10.2 Installing the review skill / MCP

```bash
cd /path/to
# codex side (the English app-review is already installed here)
vendor/python/bin/python3 -m agents.app_review.install --cli codex --checklist minesweeper.json
# kimi side (uninstalled on 2026-09-28; restore it when kimi acts as judge)
vendor/python/bin/python3 -m agents.app_review.install --cli kimi
# web side
vendor/python/bin/python3 -m agents.web_review.install --cli codex
```

### 10.3 Single evaluation (the command used on this machine)

```bash
cd /path/to
nohup env DISPLAY=:0 /home/user/.local/bin/kimi -m kimi-code/k3 \
  -p '/skill:app-review minesweeper(handoff_id=sess_<sample> , checklist=minesweeper.json)' \
  > /tmp/judge_minesweeper.log 2>&1 &
```

- Arguments: `handoff_id` = the exploration session under test (it reads `app_output/sess_<handoff_id>/artifacts/app-debug.apk`); `checklist` = the file name under `review_specs/<name>.json`.
- Time reference: 22 requirements take about **30–60 minutes** (the judge really drives an emulator and captures evidence item by item).
- The equivalent codex / astra form (as constructed in `launch_batch.py:140-150`):

```bash
codex exec -m gpt-6-astra -c model_reasoning_effort=xhigh --skip-git-repo-check -s danger-full-access \
  '/app-review minesweeper(handoff_id=sess_XXXX , checklist=minesweeper.json)'
```

### 10.4 Batch evaluation

```bash
scripts/launch_batch.py --max-parallel 2 \
  eval minesweeper sess_<sample> minesweeper.json \
  eval nonogram    sess_<sample> nonogram.json
```

### 10.5 Artifacts and reading the report

- Directory: `app_output/evaluations/<run_id>/` (`run_id = "eval_" + sha256(apk|time)[:10]`, created exclusively to avoid collisions), containing `evaluation_report.json` plus `frames/observation_*.png` (native-resolution screenshots captured while grading).
- Key report fields: `counts` (per-grade counts), `requirements[]` (per item: `requirement_id / name / grade / rationale`), `actions / observations`.
- Comparison tool: `scripts/compare_evaluations.py`; before comparing several reports, check that their `checklist_sha256` values match.
- Auditing advice: **copy a report before aggregating** it once frozen, so later changes cannot contaminate the numbers.

---

## 11. Publishing results (linux branch)

### 11.1 Current publishing policy

| Content | Published? | Notes |
|---|---|---|
| `app_output/sess_*` (exploration artifacts: APK / project / review) | Yes, on demand | Finalised (accepted) runs are pushed in batches |
| `app_output/evaluations/*` (evaluation reports) | Case by case | **Decided 2026-09-28: the three early k3 reports (old and new baseline, plus nograph) are not published**; the rest on demand |
| `runs/` (session evidence / frames / traces) | No | Excluded by `.gitignore` (large, and contains footage of real sites) |
| `vendor/`, `*.log`, `__pycache__`, `.test-tmp/`, `website_output/*` (except .gitkeep) | No | `.gitignore` |
| `review_specs/`, `android_apks/`, `android_seeds/`, code / docs | Yes | — |

### 11.2 Sync command sequence

```bash
cd /path/to

git status --porcelain                 # see exactly what will be committed
git add -A                             # or list the runs/files explicitly
git status --porcelain | head -30      # re-check (make sure nothing unwanted slipped in)
git commit -m "publish results (N/N): finalised artifacts + operator manual"
git push origin linux                  # large pushes: run under nohup and poll the log

# Safe pattern for big pushes:
#   nohup git push origin linux > /tmp/push_linux.log 2>&1 &
#   tail -f /tmp/push_linux.log
```

- Branch: `linux` (remote `origin` = `https://github.com/nologistic/recreate-anything`).
- **Push channel (measured 2026-09-29)**: prefer a **direct** connection — a 30 MB test branch pushed to GitHub at about **2.5 MB/s**; through the local mihomo proxy it was only ~0.1 MB/s (about 30× slower), so large packs take a long time.
  ```bash
  # Direct (clear any dead proxy left in the environment; env.sh already unsets these)
  unset http_proxy https_proxy all_proxy HTTP_PROXY HTTPS_PROXY ALL_PROXY
  git push origin linux
  # When the direct path is unstable, go through mihomo
  https_proxy=<proxy-url> http_proxy=<proxy-url> git push origin <branch>
  ```
- Before pushing: confirm no credential or key file was `git add`ed; check the scale with `git diff --cached --stat | tail -3`.
- If the remote has commits you do not have locally: `git pull --rebase origin linux` first, then push.

---

## 12. Appendix

### A. `BBB_` environment variables (core)

| Variable | Default | Purpose |
|---|---|---|
| `BBB_RUNS_DIR` | `<repo>/runs` | Evidence tree root |
| `BBB_APP_OUTPUT_DIR` | `<repo>/app_output` | Android artifact root |
| `BBB_WEBSITE_OUTPUT_DIR` | `<repo>/website_output` | Web artifact root |
| `BBB_ANDROID_TARGETS_DIR` | `<repo>/runs/android_targets` | APK / lease / port_lock root |
| `BBB_SCRATCH_DIR` | `<repo parent>/scratch` | Volatile runtime state (AVD clones, browser profiles) |
| `BBB_KEEP_SYSTEM_TMP` | (unset) | When `1`, TMP / TEMP are not redirected |
| `BBB_ANDROID_SDK_ROOT` | — | SDK path (overrides the vendor default) |
| `BBB_ANDROID_AVD_TEMPLATE` | `vendor/android/avd/BBB_Base.avd` | AVD template |
| `BBB_JAVA_HOME` | `vendor/jdk17` | JDK17 |
| `BBB_ANDROID_SNAPSHOT` | `bbb_base` | Warm snapshot name; empty disables it |
| `BBB_ANDROID_LOCALE` | `zh-CN` | Guest UI language |
| `BBB_ANDROID_MIN_FREE_MB` | `4200` | Free-memory threshold before launching an emulator |
| `BBB_ANDROID_EMULATOR_MB` | `4100` | Memory reserved while starting |
| `BBB_ANDROID_BOOT_CONCURRENCY` | `16` | Concurrent boot quota |
| `BBB_ANDROID_NETWORK_GUARD` | `scripts/android_network_guard.py` | Public-network guard |
| `BBB_ANDROID_PUBLIC_PROXY` | `http://<proxy-host>:<proxy-port>` | Public proxy |
| `BBB_BROWSER` | (empty) | Explicit browser path |
| `BBB_VIEWPORT_WIDTH/HEIGHT` | `1440/900` | Viewport |
| `BBB_SESSION_STALL_AFTER_S` | `600` | Stalled-session threshold |
| `BBB_EXPLORE_CWD` / `BBB_EVAL_CWD` | `<scratch>/explore`, `<scratch>/review` | Batch working directories |
| `BBB_BATCH_LOG_DIR` | `/tmp` | Batch log directory |
| `BBB_BATCH_PARALLEL` | `16` | Batch concurrency |
| `BBB_AGENT_<NAME>` | — | Absolute path of a given CLI |
| `BBB_EVAL_MODEL` / `BBB_EVAL_EFFORT` | `gpt-6-astra` / `xhigh` | Judge model settings |
| `BBB_APP_REVIEW_APK` | — | APK under evaluation (used by the judge MCP) |
| `BBB_MCP_LOG` | `runs/mcp_server.log` | MCP log |

### B. Port table

See §2.4.

### C. Repository map

```text
recreate-anything/
├── agents/                # per condition: cli_explorer / android_baseline / baseline_nograph / app_review / web_review
├── app_evaluation/        # evaluation core (session / checklist / grades; the CLI is gone, driven by MCP + skill)
├── app_output/            # Android artifacts (sess_*) and evaluations (evaluations/*)
├── app_reproduction/      # APK reproduction workspace logic
├── benchmark/             # controller, sessions, runtimes, recorder, topology, locks
├── docker/                # 4 Dockerfiles + proxy / gateway / RPC / login UI
├── docs/                  # architecture / contracts / operating notes (authoritative)
├── reproduction/          # reproduction materials and workspace
├── review_specs/          # evaluation checklists (57)
├── runs/                  # local evidence (never published): sess_*, android_targets/
├── sample_apps/           # bundled samples (ecommerce_demo and friends)
├── scripts/               # operator scripts (bootstrap / setup / preflight / batch / locks / ...)
├── self_explorer/         # self-tooling condition (archived)
├── tests/                 # pytest (run with --basetemp=.test-tmp/<name>)
├── vendor/                # python / android sdk / jdk17 / chromium (never published)
├── website_output/        # web artifacts (the only writable output dir)
└── USAGE.md               # this document
```

### D. Local helper scripts (`/path/to`, outside the repository)

| Script | Purpose |
|---|---|
| `env.sh` | Data-disk environment variables (source it at the start of every terminal) |
| `preflight.sh` | Ten-section readiness check (`--boot` adds a warm-boot smoke test) |
| `stack.sh` | Docker stack control: `up / status / logs / down / api` |
| `clean_run_state.sh` / `disable_loops.sh` / `auto_gen_watch.sh` | Runtime cleanup, loop prevention, batch babysitting |
| `override.yml` | Compose override (containers run as the host uid) |
| `shim/`, `openai-node.sh/py` | Local gateway / adapter shims |

### E. `docs/` index

| Document | Contents |
|---|---|
| `architecture.md` | Minimal exploration architecture (components, deployment modes, lifecycle) |
| `api_contract.md` | Controller HTTP contract (agent channel and operator API) |
| `evaluation_contract.md` | Evaluation semantics (four grades, evidence requirements, judge uniqueness) |
| `android_platform.md` | The Android pipeline end to end (host prep → registration → conditions → concurrency → evaluation) |
| `running_agent.md` | Guide for external researchers writing their own agent |
| `security_model.md` | Threat model T1–T10 and mitigations |
| `topology_schema.md` | Topology JSON schema |
| `dependency_requirements.md` | External dependency inventory and reproduction order |
| `adding_reference_app.md` | Adding a new reference app |

### F. FAQ

- **Q: Can several agents run against the same app at once?** Yes. Target leases are multi-holdable; each session gets its own AVD clone and port (5554 + 2n, `O_EXCL`); only login maintenance is mutually exclusive with exploration.
- **Q: A `public` target will not start?** Confirm that the public proxy and the network guard are both available (§2.5).
- **Q: A full pytest setup run errors out?** `mkdir .test-tmp` (the given basetemp does not create its parent, see AGENTS.md).
- **Q: Can an evaluation report be edited?** No. Once frozen a report is only ever copied; to change a verdict, re-run with the same judge setting.
- **Q: Does `launch_batch.py` have a dry run?** No switch; call `Task.build()` from `python -c` to print the command it would execute (that is exactly what section 4 of preflight.sh does).

---

*Last updated: 2026-09-28. Maintainer: the operator; record date and motivation in commit messages (following the repository's convention).*
