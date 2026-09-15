"""Regression tests for the model-facing screenshot compression helper.

Exploration frames are lossless PNG captures (measured 0.5-3.8MB each) and
every frame is echoed back in later turns, so a long run used to push the
request body toward the ~50MB gateway limit (observed: a 96-frame Android
exploration produced a ~50MB request body and a 400 "Failed to buffer the
request body"). _image_item() re-encodes the model-facing copy as JPEG
(measured ~8x smaller at quality 80) while the archived PNG stays on disk,
and must never break an exploration on any failure.
"""

import base64
import importlib
import io
import random

import pytest
import numpy as np
from PIL import Image

MODULES = [
    "agents.cli_explorer.mcp_server",
    "agents.our_method.mcp_server",
    "agents.android_baseline.mcp_server",
    "agents.android_our_method.mcp_server",
]


def _content_png(w: int = 360, h: int = 720) -> bytes:
    """A PNG that behaves like a real screenshot: UI strokes + color blocks,
    plus the faint antialiasing/sensor noise that keeps real frames large as
    PNG but small as JPEG (measured on real frames: ~8x). A synthetic
    perfectly-regular pattern would compress better as PNG than as JPEG and
    would not represent the workload this helper exists for.
    """
    rng = random.Random(1)
    img = Image.new("RGB", (w, h), (255, 255, 255))
    px = img.load()
    for _ in range(400):
        y0 = rng.randrange(h)
        x0 = rng.randrange(max(1, w - 60))
        for x in range(x0, min(w, x0 + rng.randrange(8, 60))):
            for dy in range(2):
                px[x, min(h - 1, y0 + dy)] = (25, 25, 25)
    for _ in range(40):
        x0 = rng.randrange(max(1, w - 40))
        y0 = rng.randrange(max(1, h - 40))
        col = (rng.randrange(256), rng.randrange(256), rng.randrange(256))
        for x in range(x0, min(w, x0 + 40)):
            for y in range(y0, min(h, y0 + 40)):
                px[x, y] = col
    noise = np.random.randint(-12, 13, (h, w, 3))
    arr = np.clip(np.asarray(img, dtype=int) + noise, 0, 255).astype(np.uint8)
    buf = io.BytesIO()
    Image.fromarray(arr).save(buf, "PNG")
    return buf.getvalue()


@pytest.mark.parametrize("modpath", MODULES)
def test_image_item_reencodes_png_as_jpeg(modpath, monkeypatch):
    """PNG 输入应转成 JPEG：体积下降、payload 是合法 JPEG。"""
    m = importlib.import_module(modpath)
    monkeypatch.delenv("BBB_IMAGE_JPEG", raising=False)
    monkeypatch.delenv("BBB_IMAGE_JPEG_QUALITY", raising=False)
    png = _content_png()
    item = m._image_item(base64.b64encode(png).decode("ascii"))
    assert item["type"] == "image"
    assert item["mimeType"] == "image/jpeg"
    out = base64.b64decode(item["data"])
    assert out[:2] == b"\xff\xd8"  # JPEG SOI marker
    assert Image.open(io.BytesIO(out)).size == (360, 720)  # keeps resolution
    assert len(out) < len(png)  # smaller than the source PNG


@pytest.mark.parametrize("modpath", MODULES)
def test_image_item_passthrough_and_fallback(modpath, monkeypatch):
    """开关/非 PNG/坏数据/空数据 都必须原样透传，绝不抛异常。"""
    m = importlib.import_module(modpath)
    png = _content_png()
    b64 = base64.b64encode(png).decode("ascii")

    # 开关关闭 → 原样 PNG
    monkeypatch.setenv("BBB_IMAGE_JPEG", "0")
    item = m._image_item(b64)
    assert item["mimeType"] == "image/png" and item["data"] == b64
    monkeypatch.delenv("BBB_IMAGE_JPEG", raising=False)

    # JPEG 输入 → 原样（不二次压缩）
    item = m._image_item(b64, "image/jpeg")
    assert item["mimeType"] == "image/jpeg" and item["data"] == b64

    # 坏数据（不是图片）→ 回退原样，不抛
    bad = base64.b64encode(b"definitely not an image").decode("ascii")
    item = m._image_item(bad)
    assert item["mimeType"] == "image/png" and item["data"] == bad

    # 空数据 → 原样
    item = m._image_item("")
    assert item["data"] == "" and item["mimeType"] == "image/png"


@pytest.mark.parametrize("modpath", MODULES)
def test_image_item_quality_env(modpath, monkeypatch):
    """BBB_IMAGE_JPEG_QUALITY 生效：低质量应小于高质量。"""
    m = importlib.import_module(modpath)
    png = _content_png()
    b64 = base64.b64encode(png).decode("ascii")

    monkeypatch.setenv("BBB_IMAGE_JPEG_QUALITY", "95")
    hi = base64.b64decode(m._image_item(b64)["data"])
    monkeypatch.setenv("BBB_IMAGE_JPEG_QUALITY", "20")
    lo = base64.b64decode(m._image_item(b64)["data"])
    assert len(lo) < len(hi)
