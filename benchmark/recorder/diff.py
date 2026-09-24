"""Visual difference utilities — benchmark-internal telemetry only.

These never reach the agent. Used for: settle detection, no-op detection,
visual settle detection and optional internal comparisons.
"""
from __future__ import annotations

import io

from PIL import Image


def load(png: bytes) -> Image.Image:
    return Image.open(io.BytesIO(png)).convert("RGB")


def diff_score(a: Image.Image, b: Image.Image) -> float:
    """Fraction of pixels that differ (exact) — cheap settle/no-op signal."""
    if a.size != b.size:
        return 1.0
    diff = Image.blend(a, b, 0.0)  # placeholder to keep types obvious
    # fast path: byte compare
    if a.tobytes() == b.tobytes():
        return 0.0
    from PIL import ImageChops
    d = ImageChops.difference(a, b).convert("L")
    hist = d.histogram()
    changed = sum(hist[16:])  # count pixels with noticeable change
    total = a.size[0] * a.size[1]
    return changed / total


def mse(a: Image.Image, b: Image.Image) -> float:
    """Normalized mean squared error in [0, 1]."""
    from PIL import ImageChops
    if a.size != b.size:
        return 1.0
    d = ImageChops.difference(a, b).convert("L")
    h = d.histogram()
    total = a.size[0] * a.size[1]
    return sum(i * i * c for i, c in enumerate(h)) / (total * 255 * 255)


def phash(img: Image.Image, size: int = 8) -> int:
    """64-bit perceptual hash (DCT-free, mean-threshold on 8x8 downsample)."""
    g = img.convert("L").resize((size, size), Image.LANCZOS)
    px = list(g.getdata())
    mean = sum(px) / len(px)
    bits = 0
    for p in px:
        bits = (bits << 1) | (1 if p > mean else 0)
    return bits


def hamming(a: int, b: int) -> int:
    return bin(a ^ b).count("1")
