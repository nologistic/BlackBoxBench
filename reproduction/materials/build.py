"""Build deterministic, synthetic media and the reusable SQLite fixture.

The source JSON is human-reviewable. Generated binaries are derived from it so
the material pack can be rebuilt without downloading anything from the web.
"""
from __future__ import annotations

import hashlib
import json
import math
import shutil
import sqlite3
import struct
import tempfile
import wave
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

from reproduction.filelock import lock_for

MATERIALS_DIR = Path(__file__).resolve().parent
SOURCE_PATH = MATERIALS_DIR / "source.json"
GENERATED_DIR = MATERIALS_DIR / "generated"
BUILD_VERSION = "2"


def _source_hash() -> str:
    h = hashlib.sha256()
    h.update(BUILD_VERSION.encode())
    h.update(SOURCE_PATH.read_bytes())
    return h.hexdigest()


def _font(size: int):
    for name in ("arial.ttf", "DejaVuSans.ttf"):
        try:
            return ImageFont.truetype(name, size=size)
        except OSError:
            pass
    return ImageFont.load_default()


def _card(path: Path, size: tuple[int, int], label: str,
          colors: tuple[str, str], motif: int) -> None:
    image = Image.new("RGB", size, colors[0])
    draw = ImageDraw.Draw(image)
    width, height = size
    for index in range(7):
        inset = 24 + index * 22
        color = colors[(index + motif) % 2]
        draw.rounded_rectangle(
            (inset, inset, width - inset, height - inset),
            radius=max(12, 52 - index * 5), outline=color,
            width=max(2, 10 - index),
        )
    radius = min(width, height) // 8
    cx = width * (0.27 + (motif % 3) * 0.18)
    cy = height * (0.34 + (motif % 2) * 0.24)
    draw.ellipse((cx - radius, cy - radius, cx + radius, cy + radius),
                 fill=colors[1])
    font = _font(max(18, min(width, height) // 13))
    bbox = draw.textbbox((0, 0), label, font=font)
    text_w = bbox[2] - bbox[0]
    draw.rounded_rectangle(
        (width // 2 - text_w // 2 - 18, height - 74,
         width // 2 + text_w // 2 + 18, height - 22),
        radius=16, fill="#ffffffdd",
    )
    draw.text((width // 2 - text_w // 2, height - 65), label,
              font=font, fill="#25303b")
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, optimize=True)


def _avatar(path: Path, initials: str, colors: tuple[str, str], motif: int) -> None:
    image = Image.new("RGB", (256, 256), colors[0])
    draw = ImageDraw.Draw(image)
    draw.ellipse((28, 28, 228, 228), fill=colors[1])
    draw.arc((52, 42, 204, 214), 190 + motif * 8, 350 + motif * 8,
             fill="#ffffff", width=8)
    font = _font(66)
    bbox = draw.textbbox((0, 0), initials, font=font)
    draw.text(((256 - bbox[2] + bbox[0]) / 2,
               (256 - bbox[3] + bbox[1]) / 2 - 5), initials,
              font=font, fill="#ffffff")
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, optimize=True)


def _write_tone(path: Path, duration: float, notes: list[tuple[float, float]],
                volume: float = 0.25) -> None:
    rate = 22050
    total = int(duration * rate)
    samples: list[int] = []
    for index in range(total):
        t = index / rate
        value = 0.0
        for frequency, weight in notes:
            value += math.sin(2 * math.pi * frequency * t) * weight
        fade = min(1.0, t * 12, (duration - t) * 12)
        samples.append(int(32767 * volume * fade * value / len(notes)))
    path.parent.mkdir(parents=True, exist_ok=True)
    with wave.open(str(path), "wb") as audio:
        audio.setnchannels(1)
        audio.setsampwidth(2)
        audio.setframerate(rate)
        audio.writeframes(b"".join(struct.pack("<h", sample)
                                   for sample in samples))


def _write_video(path: Path, duration: float, colors: tuple[str, str],
                 title: str) -> None:
    import imageio_ffmpeg

    width, height, fps = 640, 360, 15
    path.parent.mkdir(parents=True, exist_ok=True)
    writer = imageio_ffmpeg.write_frames(
        str(path), (width, height), fps=fps, codec="libx264", quality=7,
        macro_block_size=1,
        output_params=["-pix_fmt", "yuv420p", "-movflags", "+faststart"],
    )
    writer.send(None)
    try:
        for index in range(int(duration * fps)):
            phase = index / max(1, duration * fps - 1)
            frame = Image.new("RGB", (width, height), colors[0])
            draw = ImageDraw.Draw(frame)
            x = int(80 + phase * 480)
            y = int(180 + math.sin(phase * math.pi * 4) * 65)
            draw.rounded_rectangle((x - 54, y - 54, x + 54, y + 54),
                                   radius=28, fill=colors[1])
            draw.ellipse((width - x - 36, 72, width - x + 36, 144),
                         outline="#ffffff", width=8)
            draw.text((28, 28), title, font=_font(28), fill="#ffffff")
            draw.text((28, height - 48), "Synthetic local fixture",
                      font=_font(18), fill="#ffffff")
            writer.send(frame.tobytes())
    finally:
        writer.close()


def _build_images(root: Path, data: dict) -> None:
    palettes = [
        ("#496a81", "#f2b880"), ("#655a7c", "#d7c0d0"),
        ("#477998", "#c4d6b0"), ("#7a6c5d", "#f4d58d"),
        ("#5c7457", "#d8e2dc"), ("#7c4f59", "#f2cc8f"),
    ]
    for index, user in enumerate(data["users"]):
        _avatar(root / user["avatar"], user["username"][:2].upper(),
                palettes[index % len(palettes)], index)
    for index, product in enumerate(data["products"]):
        _card(root / product["image"], (640, 480), product["sku"],
              palettes[index % len(palettes)], index)
    for index, article in enumerate(data["articles"]):
        _card(root / article["cover"], (1200, 675), article["slug"],
              palettes[(index + 2) % len(palettes)], index + 3)
    post_labels = {
        "images/posts/lake.png": "LAKE WALK",
        "images/posts/shelf.png": "WINDOW SHELF",
        "images/posts/cookies.png": "WEEKEND BAKE",
        "images/posts/mint.png": "NEW LEAVES",
    }
    for index, (relative, label) in enumerate(post_labels.items()):
        _card(root / relative, (900, 900), label,
              palettes[(index + 4) % len(palettes)], index + 8)
    _card(root / "images/placeholders/hero.png", (1440, 720), "LUMEN YARD",
          palettes[0], 12)
    _card(root / "images/placeholders/empty.png", (640, 480), "NO CONTENT",
          palettes[3], 13)


def _build_media(root: Path) -> None:
    _write_tone(root / "audio/notification.wav", 0.7,
                [(660, 1.0), (880, 0.6)])
    _write_tone(root / "audio/success.wav", 1.1,
                [(523.25, 0.7), (659.25, 0.8), (783.99, 1.0)])
    _write_tone(root / "audio/ambient.wav", 6.0,
                [(110, 0.8), (164.81, 0.5), (220, 0.35)], volume=0.16)
    _write_video(root / "videos/product_demo.mp4", 4.0,
                 ("#344e5c", "#f4a261"), "PRODUCT DEMO")
    _write_video(root / "videos/story_loop.mp4", 3.0,
                 ("#5f4b66", "#84a59d"), "STORY LOOP")


def _build_database(path: Path, data: dict) -> None:
    db = sqlite3.connect(path)
    db.executescript("""
        PRAGMA journal_mode=DELETE;
        CREATE TABLE site (key TEXT PRIMARY KEY, value_json TEXT NOT NULL);
        CREATE TABLE users (id INTEGER PRIMARY KEY, username TEXT UNIQUE NOT NULL,
            password TEXT NOT NULL, display_name TEXT NOT NULL, email TEXT NOT NULL,
            role TEXT NOT NULL, bio TEXT NOT NULL, location TEXT NOT NULL,
            avatar TEXT NOT NULL);
        CREATE TABLE products (id INTEGER PRIMARY KEY, sku TEXT UNIQUE NOT NULL,
            name TEXT NOT NULL, category TEXT NOT NULL, price_cents INTEGER NOT NULL,
            stock INTEGER NOT NULL, rating REAL NOT NULL, image TEXT NOT NULL,
            description TEXT NOT NULL);
        CREATE TABLE articles (id INTEGER PRIMARY KEY, slug TEXT UNIQUE NOT NULL,
            title TEXT NOT NULL, excerpt TEXT NOT NULL, author_id INTEGER NOT NULL,
            published_at TEXT NOT NULL, tags_json TEXT NOT NULL, cover TEXT NOT NULL,
            body TEXT NOT NULL);
        CREATE TABLE posts (id INTEGER PRIMARY KEY, author_id INTEGER NOT NULL,
            content TEXT NOT NULL, media TEXT, created_at TEXT NOT NULL,
            likes INTEGER NOT NULL DEFAULT 0);
        CREATE TABLE comments (id INTEGER PRIMARY KEY, target_type TEXT NOT NULL,
            target_id INTEGER NOT NULL, author_id INTEGER NOT NULL,
            content TEXT NOT NULL, created_at TEXT NOT NULL);
        CREATE TABLE messages (id INTEGER PRIMARY KEY, sender_id INTEGER NOT NULL,
            recipient_id INTEGER NOT NULL, content TEXT NOT NULL,
            created_at TEXT NOT NULL, read INTEGER NOT NULL DEFAULT 0);
        CREATE TABLE notifications (id INTEGER PRIMARY KEY, user_id INTEGER NOT NULL,
            kind TEXT NOT NULL, text TEXT NOT NULL, created_at TEXT NOT NULL,
            read INTEGER NOT NULL DEFAULT 0);
        CREATE TABLE orders (id INTEGER PRIMARY KEY, user_id INTEGER NOT NULL,
            status TEXT NOT NULL, total_cents INTEGER NOT NULL,
            created_at TEXT NOT NULL);
        CREATE TABLE order_items (order_id INTEGER NOT NULL, product_id INTEGER NOT NULL,
            quantity INTEGER NOT NULL, price_cents INTEGER NOT NULL);
        CREATE TABLE sessions (token TEXT PRIMARY KEY, user_id INTEGER NOT NULL,
            created_at TEXT NOT NULL);
        CREATE TABLE cart_items (user_id INTEGER NOT NULL, product_id INTEGER NOT NULL,
            quantity INTEGER NOT NULL, PRIMARY KEY (user_id, product_id));
        CREATE TABLE follows (follower_id INTEGER NOT NULL, followed_id INTEGER NOT NULL,
            PRIMARY KEY (follower_id, followed_id));
        CREATE TABLE media (id TEXT PRIMARY KEY, type TEXT NOT NULL, path TEXT NOT NULL,
            title TEXT NOT NULL, duration_s REAL NOT NULL);
    """)
    for key, value in data["site"].items():
        db.execute("INSERT INTO site VALUES (?, ?)",
                   (key, json.dumps(value, ensure_ascii=False)))
    for user in data["users"]:
        db.execute("INSERT INTO users VALUES (?,?,?,?,?,?,?,?,?)", tuple(user.values()))
    for product in data["products"]:
        db.execute("INSERT INTO products VALUES (?,?,?,?,?,?,?,?,?)",
                   tuple(product.values()))
    for article in data["articles"]:
        db.execute("INSERT INTO articles VALUES (?,?,?,?,?,?,?,?,?)", (
            article["id"], article["slug"], article["title"], article["excerpt"],
            article["author_id"], article["published_at"],
            json.dumps(article["tags"], ensure_ascii=False), article["cover"],
            article["body"],
        ))
    for post in data["posts"]:
        db.execute("INSERT INTO posts VALUES (?,?,?,?,?,?)", tuple(post.values()))
    for comment in data["comments"]:
        db.execute("INSERT INTO comments VALUES (?,?,?,?,?,?)", tuple(comment.values()))
    for message in data["messages"]:
        values = list(message.values())
        values[-1] = int(values[-1])
        db.execute("INSERT INTO messages VALUES (?,?,?,?,?,?)", values)
    for notification in data["notifications"]:
        values = list(notification.values())
        values[-1] = int(values[-1])
        db.execute("INSERT INTO notifications VALUES (?,?,?,?,?,?)", values)
    for order in data["orders"]:
        db.execute("INSERT INTO orders VALUES (?,?,?,?,?)", (
            order["id"], order["user_id"], order["status"],
            order["total_cents"], order["created_at"],
        ))
        for item in order["items"]:
            db.execute("INSERT INTO order_items VALUES (?,?,?,?)", (
                order["id"], item["product_id"], item["quantity"],
                item["price_cents"],
            ))
    for item in data["media"]:
        db.execute("INSERT INTO media VALUES (?,?,?,?,?)", tuple(item.values()))
    db.commit()
    db.close()


def _manifest(root: Path, source_hash: str) -> dict:
    files = []
    for path in sorted(p for p in root.rglob("*") if p.is_file()):
        if path.name == "manifest.json":
            continue
        files.append({
            "path": path.relative_to(root).as_posix(),
            "size": path.stat().st_size,
            "sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
        })
    return {"build_version": BUILD_VERSION, "source_sha256": source_hash,
            "files": files}


def _manifest_fresh() -> bool:
    """True when the on-disk pack matches the current source (fast path)."""
    expected_hash = _source_hash()
    manifest_path = GENERATED_DIR / "manifest.json"
    if not manifest_path.exists():
        return False
    try:
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        if manifest.get("source_sha256") != expected_hash:
            return False
        for item in manifest.get("files", []):
            path = GENERATED_DIR / item["path"]
            if (not path.is_file() or path.is_symlink() or
                    path.stat().st_size != item["size"] or
                    hashlib.sha256(path.read_bytes()).hexdigest() !=
                    item["sha256"]):
                return False
        return True
    except (OSError, json.JSONDecodeError, KeyError, TypeError):
        return False


def ensure_materials(force: bool = False) -> Path:
    """Return a complete material pack, rebuilding atomically when stale.

    Several reproduction handoffs may start concurrently (one per agent);
    the rebuild itself is serialized by a cross-process lock and re-checked
    inside, so at most one process rebuilds while the others wait briefly
    and reuse the fresh pack.
    """
    if not force and _manifest_fresh():
        return GENERATED_DIR

    with lock_for(f"materials:{MATERIALS_DIR}"):
        # Another process may have rebuilt while we waited for the lock.
        if not force and _manifest_fresh():
            return GENERATED_DIR

        expected_hash = _source_hash()
        data = json.loads(SOURCE_PATH.read_text(encoding="utf-8"))
        temp = Path(tempfile.mkdtemp(prefix="materials-", dir=MATERIALS_DIR))
        try:
            _build_images(temp, data)
            _build_media(temp)
            _build_database(temp / "library.db", data)
            (temp / "source.json").write_text(
                json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
            manifest = _manifest(temp, expected_hash)
            (temp / "manifest.json").write_text(
                json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
            if GENERATED_DIR.exists():
                resolved = GENERATED_DIR.resolve()
                if resolved.parent != MATERIALS_DIR.resolve() or resolved.name != "generated":
                    raise RuntimeError(f"unsafe generated directory: {resolved}")
                shutil.rmtree(resolved)
            temp.replace(GENERATED_DIR)
        except Exception:
            shutil.rmtree(temp, ignore_errors=True)
            raise
    return GENERATED_DIR


if __name__ == "__main__":
    print(ensure_materials(force=True))
