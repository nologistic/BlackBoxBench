"""Canonical trusted-side Android public-network policy constants."""
from __future__ import annotations

import ipaddress
from urllib.parse import urlparse

PUBLIC_BLOCK_V4 = (
    "0.0.0.0/8", "10.0.0.0/8", "100.64.0.0/10", "127.0.0.0/8",
    "169.254.0.0/16", "172.16.0.0/12", "192.0.0.0/24",
    "192.168.0.0/16", "198.18.0.0/15", "224.0.0.0/4", "240.0.0.0/4",
)
PUBLIC_BLOCK_V6 = ("::1/128", "fc00::/7", "fe80::/10", "ff00::/8")
EMULATOR_DNS = "10.0.2.3"


def parse_controlled_proxy(value: str) -> tuple[str, int]:
    if any(character in value for character in "\r\n\0"):
        raise ValueError("invalid Android public proxy")
    parsed = urlparse(value if "://" in value else "http://" + value)
    if (parsed.scheme != "http" or parsed.username or parsed.password or
            parsed.path not in ("", "/") or parsed.query or parsed.fragment or
            not parsed.hostname or parsed.port is None):
        raise ValueError(
            "Android public proxy must be an HTTP host:port without credentials")
    host = parsed.hostname
    if host in ("localhost", "127.0.0.1", "::1"):
        host = "10.0.2.2"
    try:
        ipaddress.ip_address(host)
    except ValueError as exc:
        raise ValueError("Android public proxy host must be a literal IP") from exc
    return host, parsed.port


__all__ = [
    "EMULATOR_DNS", "PUBLIC_BLOCK_V4", "PUBLIC_BLOCK_V6",
    "parse_controlled_proxy",
]
