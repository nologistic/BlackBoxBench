"""Pixels-only review loop for Agent-generated Android APKs."""
from __future__ import annotations

import hashlib
import json
import os
import time
from dataclasses import dataclass
from pathlib import Path
from types import SimpleNamespace
from typing import Callable

from benchmark.android.runtime import AndroidEmulatorRuntime
from .workspace import AppReproductionWorkspace

WRITE_INTERACTIONS = {"tap", "long_press", "type_text", "press_enter"}
REENTRY_INTERACTIONS = {"tap", "long_press", "press_enter"}


@dataclass
class _Observation:
    number: int
    action_count: int
    sha256: str
    path: str


class AndroidReproductionReview:
    def __init__(self, workspace: AppReproductionWorkspace,
                 *, max_revisions: int = 3,
                 require_write_evidence: bool = False,
                 network_policy: str = "public",
                 runtime_factory: Callable | None = None):
        self.workspace = workspace
        self.max_revisions = max(0, min(5, int(max_revisions)))
        self.require_write_evidence = require_write_evidence
        if network_policy not in ("offline", "public"):
            raise ValueError("network_policy must be offline or public")
        # The reproduction-review emulator must match the exploration
        # environment: network-dependent reproductions cannot be verified on
        # an offline judge (see the app_evaluation/session.py fix of
        # 2026-09-10 for the identical issue on the judge side).
        self.network_policy = network_policy
        # 审查设备必须在客户端（agent CLI 的 MCP 桥）单次调用容忍度内启动：
        # 超时会被放弃连接 → stdio 断开 → server 进程退出并丢掉全部内存态
        # （2026-09-25 实测：agent 因此陷入"新建工作区→再试"的死循环）。
        # 给一个显式、可调、且远小于客户端容忍度的预算；失败时 start_round
        # 还会自愈重试一次，对模型完全透明。
        self.boot_timeout = int(os.environ.get("BBB_REVIEW_BOOT_TIMEOUT_S", "150"))
        self.runtime_factory = runtime_factory or (
            lambda spec, directory: AndroidEmulatorRuntime(
                spec, directory, boot_timeout=self.boot_timeout))
        # MCP 侧在 finalize 之后会后台预热（预启动审查设备 + 预构建 APK）。
        # 这样 start_reproduction_review 的耗时从"构建+启设备"降到接近 0 ——
        # 客户端对单次调用的容忍度只有 1-2 分钟，长调用会被放弃并重置连接
        # （2026-09-25 实测：agent 因此陷入"调用失败→重建工作区"的循环）。
        self.warm_runtime = None      # 已被预热好的运行时（直接接管）
        self.warm_build = None        # 预构建结果（仅当工程指纹未变才用）
        self.warm_fingerprint = ""
        self.runtime = None
        self.active = False
        self.accepted = False
        self.revision_count = 0
        self.rounds: list[dict] = []
        self.observations: list[_Observation] = []
        self.actions: list[dict] = []
        self._last_revise_hash = ""
        self._accepted_hash = ""

    def _spec(self) -> SimpleNamespace:
        return SimpleNamespace(
            app_id=f"review_{self.workspace.handoff_id}",
            apk_path=str(self.workspace.artifacts_dir / "app-debug.apk"),
            package_name="com.blackboxbench.reproduction",
            launch_activity=".MainActivity", orientation="portrait",
            reset_strategy="clear_data", network_policy=self.network_policy,
            profile_snapshot="", platform="android", kind="android",
            source_type="generated")

    def start_round(self) -> tuple[bytes, dict]:
        if self.active:
            raise ValueError("a review round is already active")
        if self.accepted:
            raise ValueError("Android review is already accepted")
        current = self.workspace.project_fingerprint()
        if self._last_revise_hash and current == self._last_revise_hash:
            raise ValueError("Android project did not change after revise decision")
        build = self._take_warm_build()
        number = len(self.rounds) + 1
        work = self.workspace.review_dir / f"round_{number:02d}" / "runtime"
        self._start_runtime(work)
        self.active = True
        self.observations = []
        self.actions = []
        png, meta = self.observe()
        meta["build"] = build
        return png, meta

    def _start_runtime(self, work: Path) -> None:
        """启动审查运行时；失败时丢掉半成品设备自愈重试一次。

        半成品设备会继续占端口并拖慢后续 boot（实测残留过孤儿模拟器 5556），
        丢掉重来比把失败暴露给模型便宜——下一次 start() 自带孤儿清扫与端口重选。
        """
        if self.warm_runtime is not None:
            # 接管 MCP 侧在 finalize 之后后台预热好的设备：把"起设备"从这次
            # 调用里彻底拿掉（客户端对单次调用只有 1-2 分钟容忍度）。
            self.runtime, self.warm_runtime = self.warm_runtime, None
            # 预热设备里装的是【预热那一刻】的构建（通常是脚手架 APK ✗）；agent
            # 之后才写代码 ⇒ 直接接管会让首轮看到旧占位（2026-09-27 minesweeper
            # 单实测：首轮 2 帧、agent 自述"预热阶段构建的旧占位 APK"）。所以按
            # 当前构建重装一次并重启目标应用，保证首轮就是 agent 的代码。
            try:
                apk = self.workspace.artifacts_dir / "app-debug.apk"
                if apk.is_file():
                    self.runtime._run("install", "-r", "-t", str(apk),
                                      timeout=180)   # type: ignore[union-attr]
                    self.runtime.restart_app()       # type: ignore[union-attr]
            except Exception:
                pass
            return
        self.runtime = self.runtime_factory(self._spec(), work)
        try:
            self.runtime.start()
        except Exception:
            self._discard_runtime()
            self.runtime = self.runtime_factory(self._spec(), work)
            self.runtime.start()

    def _take_warm_build(self) -> dict:
        """用预热好的构建结果（仅当工程指纹未变），否则现构建。"""
        build, fingerprint = self.warm_build, self.warm_fingerprint
        self.warm_build, self.warm_fingerprint = None, ""
        if build is not None and fingerprint:
            try:
                if fingerprint == self.workspace.project_fingerprint():
                    return build
            except Exception:
                pass
        return self.workspace.build_apk()

    def _discard_runtime(self) -> None:
        runtime, self.runtime = self.runtime, None
        if runtime is None:
            return
        for name in ("stop", "close", "_kill_emulator_process"):
            closer = getattr(runtime, name, None)
            if callable(closer):
                try:
                    closer()
                    return
                except Exception:
                    continue

    def resume_active_round(self) -> None:
        """客户端重连后（server 进程被换掉）静默续接进行中的 round。

        观察截图按序号落盘在 round_NN/ 下，这里只重建运行时并从磁盘恢复观察
        清单；模型端完全无感，不需要 agent 做任何重试。
        """
        number = len(self.rounds) + 1
        directory = self.workspace.review_dir / f"round_{number:02d}"
        directory.mkdir(parents=True, exist_ok=True)
        self._start_runtime(directory / "runtime")
        self.active = True
        if not self.observations:
            restored: list[_Observation] = []
            for path in sorted(directory.glob("observation_*.png")):
                try:
                    seen = int(path.stem.rsplit("_", 1)[-1])
                except ValueError:
                    continue
                restored.append(_Observation(
                    seen, len(self.actions),
                    hashlib.sha256(path.read_bytes()).hexdigest(),
                    path.relative_to(self.workspace.output_dir).as_posix()))
            self.observations = restored

    def observe(self) -> tuple[bytes, dict]:
        if not self.active or self.runtime is None:
            raise ValueError("no active Android review")
        png = self.runtime.screenshot()
        number = len(self.observations) + 1
        round_number = len(self.rounds) + 1
        directory = self.workspace.review_dir / f"round_{round_number:02d}"
        directory.mkdir(parents=True, exist_ok=True)
        path = directory / f"observation_{number:03d}.png"
        path.write_bytes(png)
        mark = _Observation(number, len(self.actions),
                            hashlib.sha256(png).hexdigest(),
                            path.relative_to(self.workspace.output_dir).as_posix())
        self.observations.append(mark)
        info = self.runtime.info()
        return png, {"stage": "app_review", "round": round_number,
                     "observation": number, "observations": len(self.observations),
                     "width": info.width, "height": info.height,
                     "orientation": info.orientation, "pixels_only": True,
                     "actions": len(self.actions)}

    def action(self, action_type: str, args: dict) -> dict:
        if not self.active or self.runtime is None:
            raise ValueError("no active Android review")
        rt = self.runtime
        info = rt.info()
        def point(x: int, y: int) -> tuple[int, int]:
            if not 0 <= x < info.width or not 0 <= y < info.height:
                raise ValueError("invalid Android review coordinates")
            return x, y
        if action_type == "tap":
            x, y = point(int(args["x"]), int(args["y"])); rt.tap(x, y)
        elif action_type == "long_press":
            x, y = point(int(args["x"]), int(args["y"]))
            rt.long_press(x, y,
                          int(args.get("duration_ms", 700)))
        elif action_type == "swipe":
            x1, y1 = point(int(args["x1"]), int(args["y1"]))
            x2, y2 = point(int(args["x2"]), int(args["y2"]))
            rt.swipe(x1, y1, x2, y2, int(args.get("duration_ms", 400)))
        elif action_type == "type_text":
            rt.type_text(str(args["text"]))
        elif action_type == "press_back":
            rt.key("Back")
        elif action_type == "press_enter":
            rt.key("Enter")
        elif action_type == "restart_app":
            rt.restart_app()
        elif action_type == "wait":
            time.sleep(min(10_000, max(0, int(args["ms"]))) / 1000)
        else:
            raise ValueError("unsupported Android review action")
        # Coordinates and typed text are not needed for structural evidence.
        # Keeping only the action kind prevents private input from entering the
        # review summary or app_output artifacts.
        self.actions.append({"type": action_type})
        return {"accepted": True, "action": len(self.actions)}

    def _write_evidence_gate(self, evidence: list[dict] | None,
                             exemption: str | None) -> None:
        if not self.require_write_evidence:
            return
        if exemption is not None:
            if not exemption.strip():
                raise ValueError("write_flow_exemption must not be empty")
            return
        if not evidence:
            raise ValueError("write_flow_evidence is required for acceptance")
        by_number = {item.number: item for item in self.observations}
        for flow in evidence:
            if not isinstance(flow, dict) or not str(flow.get("flow") or "").strip():
                raise ValueError("each write evidence item needs a flow name")
            before = by_number.get(int(flow.get("before_observation", -1)))
            after = by_number.get(int(flow.get("after_observation", -1)))
            persisted = by_number.get(int(flow.get("persisted_observation", -1)))
            if not before or not after or not persisted or not (
                    before.number < after.number < persisted.number):
                raise ValueError("write evidence must be ordered before/after/persisted")
            if after.action_count <= before.action_count:
                raise ValueError("no write interaction between before and after")
            write_actions = self.actions[before.action_count:after.action_count]
            if not any(a["type"] in WRITE_INTERACTIONS for a in write_actions):
                raise ValueError("write evidence interval contains no write-class interaction")
            if before.sha256 == after.sha256:
                raise ValueError("write interaction produced no visible change")
            if persisted.action_count <= after.action_count:
                raise ValueError("no persistence probe after visible change")
            probe = self.actions[after.action_count:persisted.action_count]
            restarted = any(a["type"] == "restart_app" for a in probe)
            back_index = next((index for index, action in enumerate(probe)
                               if action["type"] == "press_back"), None)
            reentered = (back_index is not None and any(
                action["type"] in REENTRY_INTERACTIONS
                for action in probe[back_index + 1:]))
            if not restarted and not reentered:
                raise ValueError(
                    "persistence evidence needs restart_app or press_back "
                    "followed by a visible re-entry interaction")
            if before.sha256 == persisted.sha256:
                raise ValueError("persisted state matches the before state")

    def complete_round(self, *, decision: str,
                       checked_flows: list[str] | None,
                       findings: list[str] | None,
                       write_flow_evidence: list[dict] | None = None,
                       write_flow_exemption: str | None = None) -> dict:
        if not self.active or self.runtime is None:
            raise ValueError("no active Android review")
        if decision not in ("accept", "revise"):
            raise ValueError("decision must be accept or revise")
        if decision == "revise" and self.revision_count >= self.max_revisions:
            raise ValueError("maximum Android review revisions reached")
        if not checked_flows or not findings:
            raise ValueError("checked_flows and findings are required")
        if len(self.observations) < 2:
            raise ValueError("review must contain at least two visible observations")
        if decision == "accept":
            self._write_evidence_gate(write_flow_evidence, write_flow_exemption)
        self.runtime.stop()
        self.runtime = None
        self.active = False
        record = {"round": len(self.rounds) + 1, "decision": decision,
                  "checked_flows": checked_flows, "findings": findings,
                  "actions": self.actions, "observations": [
                      item.__dict__ for item in self.observations],
                  "write_flow_evidence": write_flow_evidence,
                  "write_flow_exemption": write_flow_exemption}
        self.rounds.append(record)
        if decision == "revise":
            self.revision_count += 1
            self._last_revise_hash = self.workspace.project_fingerprint()
        else:
            self.accepted = True
            self._accepted_hash = self.workspace.mark_review_accepted()
        return {"decision": decision, "revision_count": self.revision_count,
                "accepted": self.accepted}

    def ensure_accepted(self) -> None:
        if not self.accepted:
            raise ValueError("Android reproduction review is not accepted")
        if self._accepted_hash != self.workspace.project_fingerprint():
            raise ValueError("Android project changed after acceptance")

    def summary(self) -> dict:
        return {"schema_version": 1, "platform": "android",
                "accepted": self.accepted,
                "accepted_project_hash": self._accepted_hash,
                "accepted_apk_hash": self.workspace.accepted_apk_hash,
                "revision_count": self.revision_count,
                "max_revisions": self.max_revisions,
                "require_write_evidence": self.require_write_evidence,
                "rounds": self.rounds}

    def status_payload(self) -> dict:
        return {"review_required": True, "review_active": self.active,
                "review_accepted": self.accepted,
                "review_rounds": len(self.rounds),
                "revision_count": self.revision_count,
                "max_revisions": self.max_revisions,
                "start_tool": "start_reproduction_review"}

    def close(self) -> None:
        if self.runtime is not None:
            try:
                self.runtime.stop()
            finally:
                self.runtime = None
        self.active = False
