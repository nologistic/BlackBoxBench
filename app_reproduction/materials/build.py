"""Build the deterministic, synthetic Android material pack."""
from __future__ import annotations

import hashlib
import json
import shutil
from pathlib import Path

from PIL import Image

from reproduction.materials.build import ensure_materials
from reproduction.filelock import lock_for

ROOT = Path(__file__).resolve().parent
GENERATED = ROOT / "generated"
PACK_VERSION = 1


def _sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def ensure_app_materials() -> Path:
    with lock_for("android-materials", timeout=300.0):
        return _ensure_app_materials_locked()


def _ensure_app_materials_locked() -> Path:
    manifest_path = GENERATED / "manifest.json"
    if manifest_path.is_file():
        try:
            manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
            if manifest.get("pack_version") == PACK_VERSION and all(
                    (GENERATED / item["path"]).is_file() and
                    _sha(GENERATED / item["path"]) == item["sha256"]
                    for item in manifest.get("files", [])):
                return GENERATED
        except (OSError, ValueError, KeyError):
            pass
    common = ensure_materials()
    if GENERATED.exists():
        shutil.rmtree(GENERATED)
    for name in ("content", "images", "audio", "video", "file_picker", "data"):
        (GENERATED / name).mkdir(parents=True, exist_ok=True)

    source = json.loads((common / "source.json").read_text(encoding="utf-8"))
    for key in ("users", "products", "articles", "posts", "messages", "orders"):
        value = source.get(key, [])
        (GENERATED / "content" / f"{key}.json").write_text(
            json.dumps(value, ensure_ascii=False, indent=2), encoding="utf-8")
    shutil.copy2(common / "library.db", GENERATED / "data" / "library.db")

    image_sources = sorted((common / "images").rglob("*.png"))
    for src in image_sources:
        rel = src.relative_to(common / "images")
        dst = GENERATED / "images" / rel
        dst.parent.mkdir(parents=True, exist_ok=True)
        with Image.open(src) as image:
            image.convert("RGB").resize((720, 540)).save(dst, "PNG")
    icon_source = common / "images" / "placeholders" / "hero.png"
    with Image.open(icon_source) as image:
        square = image.convert("RGB").resize((512, 512))
        square.save(GENERATED / "images" / "launcher_icon.png", "PNG")
        image.convert("RGB").resize((1080, 1920)).save(
            GENERATED / "images" / "splash_portrait.png", "PNG")

    for src in sorted((common / "audio").glob("*.wav")):
        shutil.copy2(src, GENERATED / "audio" / src.name)
    for src in sorted((common / "videos").glob("*.mp4")):
        shutil.copy2(src, GENERATED / "video" / src.name)
    picker = GENERATED / "file_picker"
    shutil.copy2(common / "images" / "posts" / "lake.png",
                 picker / "sample_photo.png")
    shutil.copy2(common / "audio" / "notification.wav",
                 picker / "sample_audio.wav")
    shutil.copy2(common / "videos" / "story_loop.mp4",
                 picker / "sample_video.mp4")
    (picker / "sample_note.txt").write_text(
        "这是 BlackBoxBench 提供的虚构测试文档，不包含真实用户信息。\n",
        encoding="utf-8")

    files = []
    for path in sorted(p for p in GENERATED.rglob("*") if p.is_file() and
                       p.name != "manifest.json"):
        files.append({"path": path.relative_to(GENERATED).as_posix(),
                      "bytes": path.stat().st_size, "sha256": _sha(path)})
    manifest_path.write_text(json.dumps({
        "pack_version": PACK_VERSION,
        "synthetic_only": True,
        "read_only_source": True,
        "files": files,
    }, ensure_ascii=False, indent=2), encoding="utf-8")
    return GENERATED


if __name__ == "__main__":
    print(ensure_app_materials())
