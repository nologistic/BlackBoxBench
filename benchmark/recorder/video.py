"""Session video generation: frames + burned-in overlays -> mp4.

Uses the imageio-ffmpeg bundled binary so no system ffmpeg is required.
Overlay: step counter, action text, click marker. Frames without an action
carry forward the previous label.
"""
from __future__ import annotations

import io
import json
from pathlib import Path

from PIL import Image, ImageDraw

from ..recorder.trace import TraceRecorder

_FONT = None


def _font(size: int = 22):
    global _FONT
    if _FONT is None:
        from PIL import ImageFont
        try:
            _FONT = ImageFont.truetype("arial.ttf", size)
        except Exception:
            _FONT = ImageFont.load_default()
    return _FONT


def _action_label(rec: dict) -> str:
    a = rec["action"]
    t = a["type"]
    if t in ("click", "double_click"):
        return f"{t.upper()}  x={a['x']} y={a['y']}"
    if t == "type_text":
        txt = (a.get("text") or "")[:28]
        return f'TYPE  "{txt}"'
    if t == "key_press":
        return f"KEY  {a.get('key')}"
    if t == "scroll":
        return f"SCROLL  dx={a.get('dx')} dy={a.get('dy')}"
    if t == "drag":
        return f"DRAG  ({a.get('x1')},{a.get('y1')})→({a.get('x2')},{a.get('y2')})"
    if t == "wait":
        return f"WAIT  {a.get('ms')}ms"
    return t.upper()


def render_video(session_dir: Path, recorder: TraceRecorder,
                 fps: int = 2) -> Path | None:
    """Burn overlays and encode runs/<sid>/video/session.mp4. Returns path or None."""
    try:
        import imageio_ffmpeg
    except ImportError:
        return None
    actions = [r for r in recorder.read_actions() if r.get("accepted")]
    if not actions:
        return None
    ffmpeg = imageio_ffmpeg.get_ffmpeg_exe()
    out_dir = Path(session_dir) / "video"
    out_dir.mkdir(exist_ok=True)
    out = out_dir / "session.mp4"

    # map frame_id -> action record (after_frame)
    by_frame = {r["after_frame"]: r for r in actions if r.get("after_frame") is not None}
    import subprocess
    proc = subprocess.Popen(
        [ffmpeg, "-y", "-f", "image2pipe", "-framerate", str(fps),
         "-i", "-", "-c:v", "libx264", "-pix_fmt", "yuv420p",
         "-vf", "scale=trunc(iw/2)*2:trunc(ih/2)*2", str(out)],
        stdin=subprocess.PIPE, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

    label = "OBSERVE"
    marker = None
    for fid in range(recorder.frame_count):
        p = recorder.frame_path(fid)
        if not p.exists():
            continue
        img = Image.open(p).convert("RGB")
        if fid in by_frame:
            rec = by_frame[fid]
            label = f"step {rec['step']}  {_action_label(rec)}"
            a = rec["action"]
            if a["type"] in ("click", "double_click"):
                marker = (a["x"], a["y"])
            else:
                marker = None
        draw = ImageDraw.Draw(img)
        # bottom bar
        draw.rectangle([0, img.height - 34, img.width, img.height], fill=(0, 0, 0))
        draw.text((10, img.height - 28), label, fill=(255, 255, 255), font=_font())
        if marker:
            x, y = marker
            draw.ellipse([x - 14, y - 14, x + 14, y + 14], outline=(255, 60, 60), width=3)
            draw.line([x - 20, y, x + 20, y], fill=(255, 60, 60), width=2)
            draw.line([x, y - 20, x, y + 20], fill=(255, 60, 60), width=2)
        buf = io.BytesIO()
        img.save(buf, format="PNG")
        try:
            proc.stdin.write(buf.getvalue())
        except BrokenPipeError:
            break
    try:
        proc.stdin.close()
        proc.wait(timeout=60)
    except Exception:
        proc.kill()
        return None
    return out if out.exists() else None
