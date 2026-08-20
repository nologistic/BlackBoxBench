"""Cursor rendering: draws the virtual pointer into frames.

Headless screenshots contain no cursor. To keep frames self-contained
(human-equivalent observation, replay, video), the controller renders the
tracked virtual cursor into every stored frame. Cursor position is known to
the controller because all pointer movement flows through it.
"""
from __future__ import annotations

import io

from PIL import Image, ImageDraw

from .. import config

# classic arrow pointer polygon (11x17-ish, scaled x1.4 for visibility)
_ARROW = [(0, 0), (0, 16), (4, 12.5), (6.5, 18), (9, 17), (6.5, 11.5), (11, 11)]


def draw_cursor(png: bytes, x: int, y: int) -> bytes:
    img = Image.open(io.BytesIO(png)).convert("RGB")
    draw = ImageDraw.Draw(img)
    pts = [(x + px, y + py) for px, py in _ARROW]
    outline = config.CURSOR_OUTLINE
    fill = config.CURSOR_COLOR
    # outline pass (slightly larger, behind)
    grow = [(x + px * 1.15 - 0.5, y + py * 1.15 - 0.5) for px, py in _ARROW]
    draw.polygon(grow, fill=outline)
    draw.polygon(pts, fill=fill)
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    return buf.getvalue()
