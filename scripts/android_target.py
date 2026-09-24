"""Operator CLI for protected local APK registration."""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))

from benchmark.android.targets import (
    list_android_targets, register_android_target, rename_android_target,
    unregister_android_target)
from benchmark.android.toolchain import AndroidToolchain


def main() -> None:
    parser = argparse.ArgumentParser(description="Manage Android benchmark targets")
    sub = parser.add_subparsers(dest="command", required=True)
    sub.add_parser("list")
    sub.add_parser("preflight")
    rename = sub.add_parser("rename")
    rename.add_argument("--from", dest="old_id", required=True)
    rename.add_argument("--to", dest="new_id", required=True)
    unregister = sub.add_parser("unregister")
    unregister.add_argument("--id", required=True)
    unregister.add_argument("--keep-artifacts", action="store_true")
    register = sub.add_parser("register")
    register.add_argument("--id", required=True)
    register.add_argument("--apk", type=Path, required=True)
    register.add_argument("--description", required=True)
    register.add_argument("--brief", default="")
    register.add_argument("--package")
    register.add_argument("--activity")
    register.add_argument("--orientation", choices=["auto", "portrait", "landscape"],
                          default="auto")
    register.add_argument("--reset", choices=["clear_data", "restart", "snapshot"],
                          default="clear_data")
    register.add_argument("--network", choices=["offline", "public"], default="offline")
    register.add_argument("--profile-snapshot", type=Path)
    register.add_argument("--protected-string", action="append", default=[])
    register.add_argument("--protected-regions-json", type=Path)
    register.add_argument("--replace", action="store_true")
    args = parser.parse_args()
    if args.command == "list":
        print(json.dumps(list_android_targets(), ensure_ascii=False, indent=2))
    elif args.command == "preflight":
        toolchain = AndroidToolchain.discover()
        print(json.dumps({"ready": not toolchain.missing(),
                          "sdk_root": str(toolchain.sdk_root),
                          "avd_template": str(toolchain.avd_template),
                          "missing": toolchain.missing()}, indent=2))
    elif args.command == "rename":
        spec = rename_android_target(args.old_id, args.new_id)
        print(json.dumps({"renamed": args.old_id, "to": spec.app_id,
                          "package": spec.package_name,
                          "network_policy": spec.network_policy}, indent=2))
    elif args.command == "unregister":
        unregister_android_target(args.id, keep_artifacts=args.keep_artifacts)
        print(json.dumps({"unregistered": args.id,
                          "artifacts_kept": args.keep_artifacts}, indent=2))
    else:
        regions = []
        if args.protected_regions_json:
            regions = json.loads(args.protected_regions_json.read_text(encoding="utf-8"))
            if not isinstance(regions, list):
                raise ValueError("protected regions JSON must be an array")
        spec = register_android_target(
            target_id=args.id, apk=args.apk, description=args.description,
            brief=args.brief, package_name=args.package or "",
            launch_activity=args.activity or "", orientation=args.orientation,
            reset_strategy=args.reset, network_policy=args.network,
            profile_snapshot=args.profile_snapshot,
            protected_strings=args.protected_string, protected_regions=regions,
            replace=args.replace)
        print(json.dumps({"registered": spec.app_id,
                          "network_policy": spec.network_policy,
                          "orientation": spec.orientation}, indent=2))


if __name__ == "__main__":
    main()
