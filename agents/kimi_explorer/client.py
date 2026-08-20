"""VLM client abstraction + Kimi (OpenAI-compatible) implementation.

The explorer loop talks to a `VLMClient`: one method, `decide(messages) -> str`,
where messages follow the OpenAI chat schema with optional base64 image parts.
Any multimodal model with an OpenAI-compatible endpoint can be dropped in by
changing base_url/model — or by implementing the protocol for another API.
"""
from __future__ import annotations

import base64
from typing import Protocol

import httpx

from .config import VLMConfig


class VLMClient(Protocol):
    def decide(self, messages: list[dict]) -> str:
        """Send chat messages (text + optional image parts), return raw text."""


def image_part(png: bytes) -> dict:
    return {"type": "image_url",
            "image_url": {"url": "data:image/png;base64,"
                                 + base64.b64encode(png).decode()}}


def text_part(text: str) -> dict:
    return {"type": "text", "text": text}


class KimiClient:
    """OpenAI-compatible chat.completions client for Kimi Code / Kimi Platform.

    trust_env=True is intentional here: this calls the public internet, where a
    system proxy may be required (unlike controller traffic, which is loopback).
    """

    def __init__(self, config: VLMConfig):
        self._cfg = config
        self._client = httpx.Client(
            base_url=config.base_url.rstrip("/"),
            headers={"Authorization": f"Bearer {config.api_key}"},
            timeout=httpx.Timeout(config.request_timeout_s, connect=10.0),
            trust_env=True,
        )

    def decide(self, messages: list[dict]) -> str:
        payload = {
            "model": self._cfg.model,
            "messages": messages,
            "max_tokens": self._cfg.max_tokens,
        }
        if self._cfg.temperature is not None:
            payload["temperature"] = self._cfg.temperature
        r = self._client.post("/chat/completions", json=payload)
        if r.status_code != 200:
            raise RuntimeError(f"VLM API {r.status_code}: {r.text[:500]}")
        data = r.json()
        return data["choices"][0]["message"]["content"]

    def close(self) -> None:
        self._client.close()
