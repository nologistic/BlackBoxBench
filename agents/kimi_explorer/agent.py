"""The model-agnostic VLM exploration loop.

    observe → build prompt (screenshot + context) → VLM decides →
    narrate thought → execute action → record discoveries → repeat → finalize

Black-box invariant: the only app information entering the prompt is the PNG
pixels from env.observe() plus the agent's own prior notes. The harness never
reads DOM/URL/app internals.

Evidence helpers understood in discoveries:
  "evidence": "last"        → the action executed this turn (real step/frames
                              filled in by the harness)
  "visual_evidence": "current" → the current screenshot's frame id
"""
from __future__ import annotations

import json
import re
import time
from dataclasses import dataclass, field
from typing import Optional

from blackbox_bench_sdk import (ActionRejectedError, BudgetExhaustedError,
                                Environment, SessionClosedError)

from .client import VLMClient, image_part, text_part
from .prompts import SYSTEM_PROMPT, turn_prompt

EDGE_TYPES = {"REQUIRES", "TRANSITIONS_TO", "MUTATES", "ENABLES", "DISABLES",
              "PERSISTS_TO", "REVEALS", "DEPENDS_ON", "CONFLICTS_WITH",
              "VALIDATES"}


@dataclass
class ExplorerStats:
    turns: int = 0
    actions: int = 0
    discoveries: int = 0
    api_errors: int = 0
    parse_retries: int = 0
    started_at: float = field(default_factory=time.monotonic)


class Explorer:
    def __init__(self, env: Environment, vlm: VLMClient, *,
                 max_turns: int = 250, history_images: int = 6,
                 verbose: bool = True):
        self.env = env
        self.vlm = vlm
        self.max_turns = max_turns
        self.history_images = history_images
        self.verbose = verbose
        self.stats = ExplorerStats()
        self._messages: list[dict] = []
        self._image_turns: list[int] = []      # indexes of msgs with images
        self._latest_frame: Optional[int] = None
        self._last_action_ev: Optional[dict] = None
        self._recent_actions: list[str] = []
        self._node_ids: list[str] = []          # created discovery ids
        self._open_questions: list[str] = []
        self._last_error: Optional[str] = None
        self._brief: str = ""                   # task brief from the environment
        self._done_check_used = False           # one "are you sure?" per run
        self._done_check_pending = False

    # ------------------------------------------------------------ main loop

    def run(self) -> dict:
        try:
            while self.stats.turns < self.max_turns:
                self.stats.turns += 1
                try:
                    if self.step_once():
                        break  # model declared done
                except BudgetExhaustedError:
                    self._log("预算耗尽,收尾 finalize")
                    break
                except SessionClosedError:
                    self._log("会话已关闭,停止")
                    break
        finally:
            try:
                return self.env.finalize()
            except Exception as e:
                self._log(f"finalize 失败: {e!r}")
                return {"error": repr(e)}

    def step_once(self) -> bool:
        """One full turn. Returns True when the model declares done."""
        obs = self.env.observe()  # may raise BudgetExhausted → handled in run()
        self._latest_frame = obs.frame_id
        self._last_action_ev = None
        if not self._brief and getattr(obs, "brief", None):
            self._brief = obs.brief  # type: ignore[attr-defined]

        prompt = turn_prompt(
            step=self.stats.actions, budget=obs.budget, cursor=obs.cursor,
            recent_actions=self._recent_actions,
            graph_summary=self._graph_summary(),
            open_questions=self._open_questions,
            last_error=self._last_error, brief=self._brief,
            done_check=self._done_check_pending)
        self._push_turn(obs.screenshot_png, prompt)

        raw = self._call_vlm()
        decision = self._parse(raw)
        if decision is None:
            self.stats.parse_retries += 1
            self._last_error = "你的输出不是合法 JSON,请只输出一个 JSON 对象"
            return False
        self._last_error = None

        thought = (decision.get("thought") or "").strip()
        if thought:
            self._log(f"💭 {thought}")  # harness 自身的控制台输出即思考展示

        action = decision.get("action")
        if action:
            self._do_action(action)

        for d in decision.get("discoveries") or []:
            self._do_discovery(d)

        self._prune_images()
        if decision.get("done"):
            if not self._done_check_used:
                # first done → one self-check turn (models often quit mid-probe)
                self._done_check_used = True
                self._done_check_pending = True
                self._log("模型请求结束 → 做一次完成度自检")
                return False
            return True
        self._done_check_pending = False
        return False

    # ------------------------------------------------------------ VLM I/O

    def _push_turn(self, png: bytes, prompt: str) -> None:
        if not self._messages:
            self._messages.append({"role": "system",
                                   "content": SYSTEM_PROMPT})
        self._messages.append({"role": "user", "content": [
            image_part(png), text_part(prompt)]})
        self._image_turns.append(len(self._messages) - 1)

    def _call_vlm(self) -> str:
        try:
            return self.vlm.decide(self._messages)
        except Exception as e:
            self.stats.api_errors += 1
            self._log(f"VLM API 错误: {e}")
            time.sleep(min(2 * self.stats.api_errors, 10))
            if self.stats.api_errors > 8:
                raise SessionClosedError("vlm_api_unavailable")
            return '{"thought": "API 暂时不可用,先观察", "action": null}'

    @staticmethod
    def _parse(raw: str) -> Optional[dict]:
        text = raw.strip()
        text = re.sub(r"^```(?:json)?\s*|\s*```$", "", text,
                      flags=re.MULTILINE).strip()
        try:
            return json.loads(text)
        except Exception:
            m = re.search(r"\{.*\}", text, re.DOTALL)
            if m:
                try:
                    return json.loads(m.group(0))
                except Exception:
                    return None
        return None

    def _prune_images(self) -> None:
        """Keep only the most recent N screenshots in context."""
        while len(self._image_turns) > self.history_images:
            idx = self._image_turns.pop(0)
            msg = self._messages[idx]
            if isinstance(msg.get("content"), list):
                msg["content"] = [p for p in msg["content"]
                                  if p.get("type") == "text"]
                msg["content"].append(text_part("[较早的截图已省略]"))

    # ------------------------------------------------------------ actions

    def _do_action(self, action: dict) -> None:
        atype = action.get("type")
        desc = f"{atype}(" + ",".join(
            str(action[k]) for k in ("x", "y", "x1", "y1", "x2", "y2",
                                     "text", "key", "dx", "dy", "ms")
            if k in action) + ")"
        try:
            r = self.env._action(**{k: v for k, v in action.items() if v is not None})
            self.stats.actions += 1
            self._recent_actions.append(f"{self.stats.actions}:{desc}")
            self._last_action_ev = {
                "step": r.step, "before_frame": self._latest_frame or 0,
                "action": desc, "after_frame": r.frame_id}
            self._latest_frame = r.frame_id
            self._log(f"▶ {desc} → frame {r.frame_id}")
        except ActionRejectedError as e:
            self._last_error = f"动作 {desc} 被拒绝: {e}"
            self._recent_actions.append(f"{self.stats.actions}:{desc}✗")
            self._log(f"✗ {desc} rejected: {e}")
        # anti-loop nudge: same signature 4+ times in a row
        if len(self._recent_actions) >= 4 and \
                len({a.split(":", 1)[1] for a in self._recent_actions[-4:]}) == 1:
            self._last_error = ("你连续重复同一个动作且没有新进展。"
                                "换一个探索方向,或把疑问登记为 hypothesis。")

    # ------------------------------------------------------------ discoveries

    def _resolve_evidence(self, d: dict) -> list[dict]:
        ev = d.get("evidence")
        if ev == "last":
            return [self._last_action_ev] if self._last_action_ev else []
        if isinstance(ev, list):
            return ev
        return []

    def _resolve_frames(self, d: dict) -> list[int]:
        ve = d.get("visual_evidence")
        if ve == "current":
            return [self._latest_frame] if self._latest_frame is not None else []
        if isinstance(ve, list):
            return [int(x) for x in ve]
        return []

    def _do_discovery(self, d: dict) -> None:
        kind = d.get("kind")
        try:
            if kind == "state":
                nid = self.env.record_state(
                    d["name"], d.get("description", ""),
                    visual_evidence=self._resolve_frames(d),
                    entry_conditions=d.get("entry_conditions", []),
                    observed_elements=d.get("observed_elements", []),
                    confidence=d.get("confidence", 0.5))
            elif kind == "feature":
                nid = self.env.record_feature(
                    d["name"], d.get("description", ""),
                    behavior=d.get("behavior", {}),
                    evidence=self._resolve_evidence(d),
                    confidence=d.get("confidence", 0.5),
                    status=d.get("status", "confirmed"))
            elif kind == "data":
                nid = self.env.record_data(d["name"], d.get("description", ""),
                                           confidence=d.get("confidence", 0.5))
            elif kind == "edge":
                if d.get("type") not in EDGE_TYPES:
                    raise ValueError(f"未知边类型 {d.get('type')}")
                nid = self.env.record_edge(
                    d["source"], d["target"], d["type"],
                    evidence=self._resolve_evidence(d),
                    confidence=d.get("confidence", 0.5),
                    status=d.get("status", "confirmed"),
                    note=d.get("note"))
            elif kind == "hypothesis":
                nid = self.env.record_hypothesis(
                    d["statement"], confidence=d.get("confidence", 0.5),
                    next_probe=d.get("next_probe", ""))
                self._open_questions.append(d["statement"])
            elif kind == "resolve":
                self.env.resolve_hypothesis(
                    d["hypothesis_id"], d["status"], note=d.get("note"),
                    evidence=self._resolve_evidence(d))
                self._open_questions = [
                    q for q in self._open_questions
                    if not q.startswith(d.get("note", "\0")[:12])]
                nid = d["hypothesis_id"]
            elif kind == "revise":
                self.env.revise(d["target_kind"], d["id"], d["op"],
                                fields=d.get("fields"), into=d.get("into"),
                                reason=d.get("reason"))
                nid = d["id"]
            else:
                self._last_error = f"未知 discovery kind: {kind}"
                return
            self.stats.discoveries += 1
            if nid and nid not in self._node_ids:
                self._node_ids.append(nid)
            self._log(f"✎ {kind} → {nid}")
        except (ActionRejectedError, KeyError, ValueError, TypeError) as e:
            self._last_error = f"discovery {kind} 被驳回: {e}(修正后重发)"
            self._log(f"✗ discovery {kind} rejected: {e}")
        except Exception as e:  # the loop must survive any discovery failure
            self._last_error = f"discovery {kind} 出错: {e}(修正后重发)"
            self._log(f"✗ discovery {kind} error: {e}")

    # ------------------------------------------------------------ misc

    def _graph_summary(self) -> str:
        if not self._node_ids:
            return "(尚未记录任何发现)"
        return f"已记录 {len(self._node_ids)} 个节点: " + ", ".join(
            self._node_ids[-14:])

    def _log(self, msg: str) -> None:
        if self.verbose:
            print(f"[explorer] {msg}", flush=True)
