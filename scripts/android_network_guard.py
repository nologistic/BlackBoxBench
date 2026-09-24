"""Trusted verifier for Android Emulator guest egress rules.

The runtime installs the rules. This separately invoked helper verifies them
before a public target starts and records a short operator-only lease. It is
never mounted or exposed to an Agent.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))

from benchmark import config
from benchmark.android.network_policy import (
    EMULATOR_DNS, PUBLIC_BLOCK_V4, PUBLIC_BLOCK_V6, parse_controlled_proxy)
from benchmark.android.toolchain import AndroidToolchain


def _rules(serial: str, family: str) -> str:
    toolchain = AndroidToolchain.discover()
    result = subprocess.run(
        [str(toolchain.adb), "-s", serial, "shell", family, "-S", "OUTPUT"],
        text=True, capture_output=True, check=True, timeout=20,
        creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))
    return result.stdout


def _lease(serial: str) -> Path:
    if not re.fullmatch(r"emulator-\d{4,5}", serial):
        raise ValueError("invalid emulator serial")
    root = config.ANDROID_TARGETS_DIR / "network_guard"
    root.mkdir(parents=True, exist_ok=True)
    return root / f"{serial}.json"


def _lines(rules: str) -> list[str]:
    return [" ".join(line.split()) for line in rules.splitlines() if line.strip()]


def _rule_index(rules: str, *, destination: str, jump: str,
                protocol: str = "", port: str = "") -> int | None:
    for index, line in enumerate(_lines(rules)):
        tokens = line.split()
        if len(tokens) < 4 or tokens[:2] != ["-A", "OUTPUT"]:
            continue
        try:
            actual_destination = tokens[tokens.index("-d") + 1]
            actual_jump = tokens[tokens.index("-j") + 1]
        except (ValueError, IndexError):
            continue
        destination_matches = (actual_destination == destination or
                               actual_destination.split("/", 1)[0] == destination)
        if not destination_matches or actual_jump != jump:
            continue
        if protocol and not ("-p" in tokens and
                             tokens[tokens.index("-p") + 1] == protocol):
            continue
        if port and not ("--dport" in tokens and
                         tokens[tokens.index("--dport") + 1] == port):
            continue
        return index
    return None


def _accept_indices(rules: str) -> set[int]:
    return {index for index, line in enumerate(_lines(rules))
            if line.startswith("-A OUTPUT ") and line.endswith("-j ACCEPT")}


def verify_rules(policy: str, proxy: str, ipv4: str, ipv6: str) -> dict:
    """Validate the complete policy emitted by the trusted runtime."""
    if policy == "deny-all":
        if "-P OUTPUT DROP" not in ipv4 or "-P OUTPUT DROP" not in ipv6:
            raise RuntimeError("offline guest firewall is not deny-all")
        return {"policy": policy, "proxy_endpoint": ""}
    if policy != "public-only":
        raise ValueError("unknown Android network policy")
    proxy_host, proxy_port = parse_controlled_proxy(proxy)
    if "-P OUTPUT ACCEPT" not in ipv4 or "-P OUTPUT ACCEPT" not in ipv6:
        raise RuntimeError("public guest firewall has an unexpected default policy")
    for network in PUBLIC_BLOCK_V4:
        if _rule_index(ipv4, destination=network, jump="REJECT") is None:
            raise RuntimeError(
                f"public guest firewall lacks IPv4 block {network}")
    for network in PUBLIC_BLOCK_V6:
        if _rule_index(ipv6, destination=network, jump="REJECT") is None:
            raise RuntimeError(
                f"public guest firewall lacks IPv6 block {network}")
    proxy_rules = ipv6 if ":" in proxy_host else ipv4
    proxy_index = _rule_index(
        proxy_rules, destination=proxy_host, jump="ACCEPT", protocol="tcp",
        port=str(proxy_port))
    if proxy_index is None:
        raise RuntimeError("public guest firewall lacks the controlled proxy rule")
    for protocol in ("udp", "tcp"):
        dns_index = _rule_index(
            ipv4, destination=EMULATOR_DNS, jump="ACCEPT",
            protocol=protocol, port="53")
        private_index = _rule_index(
            ipv4, destination="10.0.0.0/8", jump="REJECT")
        if dns_index is None or private_index is None or dns_index >= private_index:
            raise RuntimeError(
                "public guest firewall DNS exception is missing or misordered")
    expected_v4_accepts = {
        index for index in (
            proxy_index if ":" not in proxy_host else None,
            _rule_index(ipv4, destination=EMULATOR_DNS, jump="ACCEPT",
                        protocol="udp", port="53"),
            _rule_index(ipv4, destination=EMULATOR_DNS, jump="ACCEPT",
                        protocol="tcp", port="53"),
        ) if index is not None}
    expected_v6_accepts = ({proxy_index} if ":" in proxy_host else set())
    if (_accept_indices(ipv4) != expected_v4_accepts or
            _accept_indices(ipv6) != expected_v6_accepts):
        raise RuntimeError(
            "public guest firewall contains an unexpected ACCEPT exception")
    if proxy_host.startswith("10."):
        private_index = _rule_index(
            ipv4, destination="10.0.0.0/8", jump="REJECT")
        if private_index is None or proxy_index >= private_index:
            raise RuntimeError(
                "controlled proxy exception must precede the private-range block")
    return {"policy": policy,
            "proxy_endpoint": f"{proxy_host}:{proxy_port}"}


def attach(serial: str, policy: str, proxy: str = "") -> None:
    ipv4 = _rules(serial, "iptables")
    ipv6 = _rules(serial, "ip6tables")
    verified = verify_rules(policy, proxy, ipv4, ipv6)
    _lease(serial).write_text(json.dumps({
        "serial": serial, "policy": policy, "proxy_configured": bool(proxy),
        "proxy_endpoint": verified["proxy_endpoint"],
        "rules_sha256": hashlib.sha256(
            (ipv4 + "\0" + ipv6).encode("utf-8")).hexdigest(),
        "verified": True,
    }, indent=2), encoding="utf-8")


def detach(serial: str) -> None:
    _lease(serial).unlink(missing_ok=True)


def main() -> None:
    parser = argparse.ArgumentParser(description="Verify Android guest network policy")
    parser.add_argument("operation", choices=("attach", "detach"))
    parser.add_argument("serial")
    parser.add_argument("policy", nargs="?")
    parser.add_argument("proxy", nargs="?", default="")
    args = parser.parse_args()
    if args.operation == "detach":
        detach(args.serial)
    else:
        if not args.policy:
            raise SystemExit("attach requires a policy")
        attach(args.serial, args.policy, args.proxy)


if __name__ == "__main__":
    main()
