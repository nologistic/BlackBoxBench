"""Configuration for the Kimi VLM explorer agent.

Everything is env-var driven so no secret ever lands in the repo:

  BBB_VLM_API_KEY   (required at runtime) — Kimi Code Console API key
  BBB_VLM_BASE_URL  default https://api.kimi.com/coding/v1 (Kimi Code 会员,
                    OpenAI 兼容)。也可指向 Kimi Platform
                    https://api.moonshot.cn/v1 (按量,多模态)。
  BBB_VLM_MODEL     default k3 (Kimi Code)。Kimi Platform 侧可换视觉模型。
  BBB_VLM_MAX_TOKENS / BBB_VLM_TEMPERATURE / BBB_VLM_HISTORY_IMAGES 可调。
"""
from __future__ import annotations

import os
from dataclasses import dataclass


@dataclass
class VLMConfig:
    api_key: str
    base_url: str = "https://api.kimi.com/coding/v1"
    model: str = "k3"
    max_tokens: int = 4096
    temperature: float | None = None   # k3 仅允许 1;None = 不下发该参数
    history_images: int = 6        # how many recent turns keep their screenshot
    request_timeout_s: float = 120.0

    @classmethod
    def from_env(cls) -> "VLMConfig":
        key = os.environ.get("BBB_VLM_API_KEY") or os.environ.get("KIMI_API_KEY")
        return cls(
            api_key=key or "",
            base_url=os.environ.get("BBB_VLM_BASE_URL",
                                    "https://api.kimi.com/coding/v1"),
            model=os.environ.get("BBB_VLM_MODEL", "k3"),
            max_tokens=int(os.environ.get("BBB_VLM_MAX_TOKENS", "4096")),
            temperature=(float(os.environ["BBB_VLM_TEMPERATURE"])
                         if os.environ.get("BBB_VLM_TEMPERATURE") else None),
            history_images=int(os.environ.get("BBB_VLM_HISTORY_IMAGES", "6")),
            request_timeout_s=float(os.environ.get("BBB_VLM_TIMEOUT", "120")),
        )

    def validate(self) -> None:
        if not self.api_key:
            raise SystemExit(
                "缺少 API key: 请设置环境变量 BBB_VLM_API_KEY(或 KIMI_API_KEY)。\n"
                "Kimi Code 会员在 Kimi Code Console 创建 key;\n"
                "也可以把 BBB_VLM_BASE_URL 指向 Kimi Platform "
                "(https://api.moonshot.cn/v1) 使用按量 key。")
