#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Build android_seeds/<app_id>/ content for every target that needs it.

Everything is generated locally (ffmpeg / PIL / hand-rolled EPUB-OPML-ICS /
genanki) — no downloads, no copyright questions, deterministic output.
Re-run after edits: the runtime pushes whatever is under android_seeds/.
"""
import json
import random
import struct
import subprocess
import sys
import zipfile
from datetime import date, datetime, timedelta
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
SEEDS = ROOT / "android_seeds"
RNG = random.Random(20260920)


# ---------------------------------------------------------------- photo ----
def _exif(dt: datetime, orientation: int = 1) -> bytes:
    # Minimal EXIF with DateTime + Orientation so gallery timelines group.
    def tiff(*entries):
        return b"".join(entries)

    dt_str = dt.strftime("%Y:%m:%d %H:%M:%S").encode() + b"\x00"
    dt_off = 8
    return b"Exif\x00\x00" + struct.pack(
        "<HH", 0x4949, 0x2A00) + struct.pack("<I", 8) + tiff(
        struct.pack("<HHI", 0x0112, 3, 1) + struct.pack("<HH", orientation, 0),  # orientation
        struct.pack("<HHI", 0x9003, 2, 20) + struct.pack("<HHH", 7, 1, dt_off + 26 - 8),
        # placeholder replaced below by caller appending dt bytes; keep simple:
    ) + dt_str if False else _exif_full(dt, orientation)


def _exif_full(dt: datetime, orientation: int) -> bytes:
    dt_str = dt.strftime("%Y:%m:%d %H:%M:%S").encode() + b"\x00"
    header = b"Exif\x00\x00" + struct.pack("<HH", 0x4949, 0x2A00) + struct.pack("<I", 8)
    # IFD: 2 entries (orientation SHORT, datetime ASCII) + next-IFD 0
    body = struct.pack("<H", 2)
    body += struct.pack("<HHI", 0x0112, 3, 1) + struct.pack("<HH", orientation, 0)
    data_off = 8 + 2 + 24 + 4  # tiff hdr + count + entries + next
    body += struct.pack("<HHI", 0x9003, 2, len(dt_str)) + struct.pack("<HHH", 7, 1, data_off - 8)
    body += struct.pack("<I", 0)
    return header + body + dt_str


def _photo(path: Path, size, dt: datetime, palette):
    img = Image.new("RGB", size)
    px = img.load()
    w, h = size
    for x in range(0, w, 8):
        for y in range(0, h, 8):
            c = palette[(x // 8 + y // 8) % len(palette)]
            for dx in range(8):
                for dy in range(8):
                    if x + dx < w and y + dy < h:
                        px[x + dx, y + dy] = c
    img.save(path, "JPEG", quality=85, exif=_exif_full(dt, 1))


def build_photos(dst: Path, n: int = 30):
    dst.mkdir(parents=True, exist_ok=True)
    base = datetime(2026, 6, 1, 10, 0, 0)
    palettes = [
        [(180, 60, 60), (240, 160, 60), (250, 230, 120)],
        [(60, 120, 180), (120, 200, 160), (220, 240, 250)],
        [(90, 60, 140), (200, 120, 200), (250, 200, 230)],
        [(70, 130, 90), (150, 200, 90), (230, 240, 200)],
    ]
    for i in range(n):
        dt = base + timedelta(days=i * 3 + RNG.randint(0, 2),
                              hours=RNG.randint(0, 10))
        size = RNG.choice([(1080, 1920), (1920, 1080), (1440, 1440), (2400, 1600)])
        _photo(dst / f"IMG_{dt.strftime('%Y%m%d')}_{i:03d}.jpg",
               size, dt, palettes[i % len(palettes)])


# ---------------------------------------------------------------- audio ----
def _tone(path: Path, seconds: int, freq: int, meta: dict):
    subprocess.run(
        ["ffmpeg", "-y", "-loglevel", "error",
         "-f", "lavfi", "-i", f"sine=frequency={freq}:duration={seconds}",
         "-f", "lavfi", "-i", f"sine=frequency={freq * 1.5}:duration={seconds}",
         "-filter_complex", "[0][1]amix=inputs=2,volume=0.6",
         "-metadata", f"title={meta['title']}",
         "-metadata", f"artist={meta['artist']}",
         "-metadata", f"album={meta['album']}",
         "-metadata", f"genre={meta['genre']}",
         "-metadata", f"track={meta['track']}/{meta['tracks']}",
         "-codec:a", "libmp3lame", "-b:a", "128k", str(path)], check=True)


ALBUMS = [
    ("Neon Harbour", "The Chromatics", "Synthwave", 4),
    ("Paper Lanterns", "Mei & The Foxes", "Indie Folk", 4),
    ("Deep Field", "Orbit Nine", "Ambient", 3),
    ("Kitchen Tapes", "The Wilhelms", "Lo-fi", 3),
    ("Copper Sky", "Ana Vela", "Latin Jazz", 3),
]


def build_music(dst: Path):
    dst.mkdir(parents=True, exist_ok=True)
    track = 0
    for album, artist, genre, n in ALBUMS:
        for i in range(1, n + 1):
            track += 1
            _tone(
                dst / f"{track:02d} {album} - Track {i}.mp3",
                RNG.randint(45, 180), RNG.choice([220, 262, 294, 330, 392]),
                {"title": f"Track {i}", "artist": artist,
                 "album": album, "genre": genre,
                 "track": i, "tracks": n})


def build_podcasts(dst: Path):
    dst.mkdir(parents=True, exist_ok=True)
    for i, (show, ep) in enumerate([
            ("Signal & Noise", "Episode 12: Latency"),
            ("Signal & Noise", "Episode 13: Drift"),
            ("The Backlog", "Special: Deep Work"),
            ("Field Notes", "Reading List Q3")]):
        _tone(dst / f"{show} - {ep}.mp3", RNG.randint(300, 600),
              180 + i * 40,
              {"title": ep, "artist": show, "album": show,
               "genre": "Podcast", "track": i + 1, "tracks": 4})


# ---------------------------------------------------------------- video ----
def build_videos(dst: Path):
    dst.mkdir(parents=True, exist_ok=True)
    for i, (w, h) in enumerate([(1280, 720), (1920, 1080), (720, 1280)]):
        subprocess.run(
            ["ffmpeg", "-y", "-loglevel", "error",
             "-f", "lavfi", "-i", f"testsrc2=size={w}x{h}:rate=24:duration={10+i*5}",
             "-f", "lavfi", "-i", "sine=frequency=440:duration=1",
             "-metadata", f"title=Sample Clip {i+1}",
             "-pix_fmt", "yuv420p", "-c:v", "libx264", "-preset", "fast",
             "-c:a", "aac", "-shortest", str(dst / f"clip_{i+1}_{w}x{h}.mp4")],
            check=True)


# -------------------------------------------------------------- markdown ----
def build_markdown(dst: Path):
    dst.mkdir(parents=True, exist_ok=True)
    notes = [
        ("welcome.md", "# Welcome\n\nFirst note: **bold**, *italic*, `code`.\n\n- [ ] todo one\n- [ ] todo two\n- [x] done\n"),
        ("recipes/pasta.md", "# Pasta Carbonara\n\n## Ingredients\n\n- 200g spaghetti\n- 2 eggs\n- pecorino\n\n## Steps\n\n1. Boil water\n2. Fry guanciale\n3. Mix eggs & cheese\n"),
        ("projects/bench.md", "# Benchmark Notes\n\n```python\nprint('hello bench')\n```\n\n| Model | Score |\n|---|---|\n| A | 92 |\n| B | 88 |\n"),
        ("journal/2026-06.md", "# June 2026\n\nWent hiking. Weather was [great](https://example.com).\n\n> Quote of the month.\n"),
        ("reading.md", "# Reading List\n\n1. Structure of Science\n2. Designing Data-Intensive Applications\n3. The Pragmatic Programmer\n"),
        ("meeting-notes/standup.md", "# Standup 06-12\n\n- ship seeds\n- fix flaky boot\n- review checklist\n"),
        ("ideas.md", "# Ideas\n\n## App ideas\n\n- offline-first journal\n- habit heat map\n"),
        ("finance/budget.md", "# Budget\n\n| Category | Amount |\n|---|---|\n| Rent | 3200 |\n| Food | 800 |\n"),
        ("howto/git.md", "# Git Tips\n\n```\ngit rebase -i HEAD~3\n```\n\nSquash early, squash often.\n"),
        ("quotes.md", "# Quotes\n\n> Simplicity is a great virtue.\n\n> Make it work, make it right, make it fast.\n"),
    ]
    for rel, text in notes:
        p = dst / rel
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(text, encoding="utf-8")


# ------------------------------------------------------------------ pdf ----
def build_pdfs(dst: Path, n: int = 5):
    dst.mkdir(parents=True, exist_ok=True)
    for i in range(n):
        pages = []
        for pg in range(3 + i):
            img = Image.new("RGB", (850, 1100), (250, 248, 242))
            px = img.load()
            for x in range(60, 790, 4):
                for y in range(80, 1020, 4):
                    px[x, y] = ((x * 7 + y * 3 + pg * 30 + i * 60) % 220 + 20,) * 3
            pages.append(img)
        pages[0].save(dst / f"document_{i+1}.pdf", save_all=True,
                      append_images=pages[1:])


# ----------------------------------------------------------------- epub ----
def _epub(path: Path, title: str, author: str, chapters: list[str]):
    def xml(s):
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    items = [
        ("mimetype", "application/epub+zip"),
        ("META-INF/container.xml",
         '<?xml version="1.0"?><container version="1.0" '
         'xmlns="urn:oasis:names:tc:opendocument:xmlns:container">'
         '<rootfiles><rootfile full-path="OEBPS/content.opf" '
         'media-type="application/oebps-package+xml"/></rootfiles></container>'),
    ]
    manifest, spine, files = [], [], []
    for i, (ch_title, body) in enumerate(chapters, 1):
        cid = f"chap{i}"
        manifest.append(f'<item id="{cid}" href="{cid}.xhtml" '
                        f'media-type="application/xhtml+xml"/>')
        spine.append(f'<itemref idref="{cid}"/>')
        files.append((f"OEBPS/{cid}.xhtml",
                      f'<?xml version="1.0" encoding="utf-8"?>'
                      f'<html xmlns="http://www.w3.org/1999/xhtml">'
                      f'<head><title>{xml(ch_title)}</title></head><body>'
                      f'<h1>{xml(ch_title)}</h1>{body}</body></html>'))
    opf = ('<?xml version="1.0" encoding="utf-8"?>'
           '<package xmlns="http://www.idpf.org/2007/opf" version="2.0" '
           'unique-identifier="bid"><metadata '
           'xmlns:dc="http://purl.org/dc/elements/1.1/">'
           f'<dc:title>{xml(title)}</dc:title>'
           f'<dc:creator>{xml(author)}</dc:creator>'
           '<dc:language>en</dc:language>'
           '<dc:identifier id="bid">urn:uuid:'
           f'{RNG.getrandbits(64):016x}</dc:identifier></metadata>'
           '<manifest><item id="ncx" href="toc.ncx" '
           'media-type="application/x-dtbncx+xml"/>' + "".join(manifest) +
           '</manifest><spine toc="ncx">' + "".join(spine) +
           '</spine></package>')
    files.append(("OEBPS/content.opf", opf))
    files.append(("OEBPS/toc.ncx",
                  '<?xml version="1.0"?><ncx xmlns='
                  '"http://www.daisy.org/z3986/2005/ncx/" version="2005-1">'
                  f'<head/><docTitle><text>{xml(title)}</text></docTitle>'
                  '<navMap>' + "".join(
                      f'<navPoint id="n{i}" playOrder="{i}"><navLabel>'
                      f'<text>Chapter {i}</text></navLabel>'
                      f'<content src="chap{i}.xhtml"/></navPoint>'
                      for i in range(1, len(chapters) + 1)) +
                  '</navMap></ncx>'))
    with zipfile.ZipFile(path, "w") as z:
        for name, data in items + files:
            if name == "mimetype":
                z.writestr(zipfile.ZipInfo(name), data,
                           compress_type=zipfile.ZIP_STORED)
            else:
                z.writestr(name, data)


def build_ebooks(dst: Path):
    dst.mkdir(parents=True, exist_ok=True)
    _epub(dst / "the-lantern-keepers.epub", "The Lantern Keepers",
          "I. Marsh",
          [(f"Chapter {i}", f"<p>{'Lorem ipsum dolor sit amet. ' * 40}</p>")
           for i in range(1, 7)])
    _epub(dst / "notes-on-machinery.epub", "Notes on Machinery",
          "R. Okafor",
          [("Gears", "<p>Teeth mesh, forces transfer.</p>" * 10),
           ("Bearings", "<p>Friction is the enemy.</p>" * 10),
           ("Governors", "<p>Feedback holds speed steady.</p>" * 10)])
    _epub(dst / "a-field-guide.epub", "A Field Guide to Quiet Places",
          "S. Lindqvist",
          [(f"Place {i}", f"<p>{'The moss grows softly here. ' * 30}</p>")
           for i in range(1, 5)])


# ----------------------------------------------------------------- anki ----
def build_anki(dst: Path):
    try:
        import genanki
    except ImportError:
        print("  [skip] genanki not installed")
        return
    dst.mkdir(parents=True, exist_ok=True)
    model = genanki.Model(
        160739231900, "BBB Basic",
        fields=[{"name": "Question"}, {"name": "Answer"}],
        templates=[{"name": "Card 1",
                    "qfmt": "{{Question}}",
                    "afmt": "{{FrontSide}}<hr id=answer>{{Answer}}"}])
    decks = [
        (2059400110, "Capitals",
         [(f"Capital of {c}?", a) for c, a in [
             ("Japan", "Tokyo"), ("Canada", "Ottawa"),
             ("Australia", "Canberra"), ("Brazil", "Brasilia"),
             ("Egypt", "Cairo"), ("Norway", "Oslo")]]),
        (2059400111, "Spanish Vocab",
         [(f"Translate: {s}", e) for s, e in [
             ("el gato", "the cat"), ("el perro", "the dog"),
             ("la casa", "the house"), ("el agua", "the water"),
             ("el libro", "the book")]]),
        (2059400112, "Math Facts",
         [(f"{a} × {b}?", str(a * b)) for a, b in [
             (7, 8), (6, 9), (12, 12), (11, 11), (9, 7)]]),
    ]
    for i, (did, name, notes) in enumerate(decks, 1):
        deck = genanki.Deck(did, name)
        for q, a in notes:
            deck.add_note(genanki.Note(
                model=model, fields=[q, a]))
        genanki.Package(deck).write_to_file(dst / f"deck_{i}_{name.lower().replace(' ', '_')}.apkg")


# ---------------------------------------------------------------- feeds ----
def build_feeds(dst: Path):
    dst.mkdir(parents=True, exist_ok=True)
    feed_items = "".join(
        f"<item><title>Post {i}: {t}</title>"
        f"<link>https://example.org/p/{i}</link>"
        f"<pubDate>{(date(2026, 6, 1) + timedelta(days=i)).strftime('%a, %d %b %Y 08:00:00 GMT')}</pubDate>"
        f"<description>{d}</description></item>"
        for i, (t, d) in enumerate([
                ("On Seeds", "Everything starts from a seed."),
                ("Latency Budgets", "Measure, then cut."),
                ("Field Report", "Three weeks in the logs."),
                ("Quiet Tools", "Software that does not shout."),
                ("Rebuild", "Starting over, better."),
                ("Maps We Drew", "Paper beats GPS sometimes."),
                ("Small Apps", "One feature, done well."),
                ("Archive Dive", "Old mailing lists shine.")], 1))
    rss = ('<?xml version="1.0"?><rss version="2.0"><channel>'
           "<title>Example Digest</title>"
           "<link>https://example.org</link>"
           "<description>Sample feed for exploration</description>"
           + feed_items + "</channel></rss>")
    (dst / "feeds" / "example-digest.xml").parent.mkdir(parents=True, exist_ok=True)
    (dst / "feeds" / "example-digest.xml").write_text(rss, encoding="utf-8")
    (dst / "feeds" / "tech-weekly.xml").write_text(
        rss.replace("Example Digest", "Tech Weekly"), encoding="utf-8")
    opml = ('<?xml version="1.0" encoding="UTF-8"?>'
            '<opml version="2.0"><head><title>Subscriptions</title></head>'
            "<body>" + "".join(
                f'<outline type="rss" text="{n}" '
                f'xmlUrl="file:///sdcard/Download/BlackBoxBench/feeds/{f}"/>'
                for n, f in [("Example Digest", "example-digest.xml"),
                             ("Tech Weekly", "tech-weekly.xml")]) +
            "</body></opml>")
    (dst / "subscriptions.opml").write_text(opml, encoding="utf-8")


# ---------------------------------------------------------------- files ----
def build_filetree(dst: Path):
    dst.mkdir(parents=True, exist_ok=True)
    (dst / "Documents" / "reports").mkdir(parents=True, exist_ok=True)
    (dst / "Documents" / "reports" / "q2-summary.txt").write_text(
        "Q2 summary\n\nRevenue up. Bugs down.\n", encoding="utf-8")
    (dst / "Documents" / "notes.txt").write_text(
        "scratch notes\n- buy milk\n- call bank\n", encoding="utf-8")
    (dst / "Archives").mkdir(exist_ok=True)
    with zipfile.ZipFile(dst / "Archives" / "backup.zip", "w") as z:
        z.writestr("a.txt", "alpha\n")
        z.writestr("sub/b.txt", "beta\n")
        z.writestr("sub/c.log", "gamma\n")
    (dst / "Spreadsheets").mkdir(exist_ok=True)
    (dst / "Spreadsheets" / "expenses.csv").write_text(
        "date,amount,category\n2026-06-01,12.40,food\n2026-06-02,30.00,transport\n",
        encoding="utf-8")


# ------------------------------------------------------------------ main ----
PLAN = [
    ("vinyl", build_music, {}),
    ("fossify_gallery", build_photos, {"n": 30}),
    ("snapseed", build_photos, {"n": 10}),
    ("vlc", build_videos, {}),
    ("vlc", build_music, {}),
    ("antennapod", build_podcasts, {}),
    ("antennapod", build_feeds, {}),
    ("markor", build_markdown, {}),
    ("librera", build_ebooks, {}),
    ("librera", build_pdfs, {"n": 2}),
    ("mj_pdf", build_pdfs, {"n": 5}),
    ("material_files", build_filetree, {}),
    ("ankidroid", build_anki, {}),
    ("feeder", build_feeds, {}),
]

if __name__ == "__main__":
    SEEDS.mkdir(exist_ok=True)
    for app_id, fn, kw in PLAN:
        print(f"[{app_id}] {fn.__name__} ...")
        fn(SEEDS / app_id, **kw)
    # per-target destination manifest (read by operators / docs)
    print("\nDone. Layout:")
    for p in sorted(SEEDS.rglob("*")):
        if p.is_file():
            print(" ", p.relative_to(ROOT))
