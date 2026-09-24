"""Build the deterministic Android Commerce Demo with the pinned container."""
from __future__ import annotations

import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))

from app_reproduction.materials.build import ensure_app_materials
from app_reproduction.workspace import IMAGE_NAME, require_android_build_image
from reproduction.workspace import _docker, ensure_docker_available

SAMPLE = ROOT / "sample_apps" / "android_commerce_demo"


def main() -> None:
    ensure_docker_available()
    require_android_build_image()
    materials = ensure_app_materials()
    drawable = SAMPLE / "app" / "src" / "main" / "res" / "drawable"
    raw = SAMPLE / "app" / "src" / "main" / "res" / "raw"
    drawable.mkdir(parents=True, exist_ok=True); raw.mkdir(parents=True, exist_ok=True)
    shutil.copy2(materials / "images" / "products" / "camera.png",
                 drawable / "product_camera.png")
    shutil.copy2(materials / "audio" / "notification.wav", raw / "notification.wav")
    shutil.copy2(materials / "video" / "product_demo.mp4", raw / "product_demo.mp4")
    result = _docker(
        "run", "--rm", "--network", "none",
        "--mount", f"type=bind,source={SAMPLE.resolve()},target=/workspace",
        IMAGE_NAME, "gradle", "--offline", "--no-daemon", "--max-workers=2",
        "assembleDebug", check=False, timeout=600,
    )
    if result.returncode:
        detail = (result.stderr or result.stdout or "").strip()
        if len(detail) > 8_000:
            detail = "...\n" + detail[-8_000:]
        raise RuntimeError(
            "Android Commerce Demo build failed"
            + (f":\n{detail}" if detail else " (Docker returned no diagnostics)")
        )
    apk = SAMPLE / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug.apk"
    if not apk.is_file():
        raise FileNotFoundError("sample APK build produced no app-debug.apk")
    print(apk)


if __name__ == "__main__":
    main()
