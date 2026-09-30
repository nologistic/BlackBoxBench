#!/usr/bin/env python3
"""Copy fictional material media into the reproduction APK assets and emit library.json."""
import json, os, shutil, datetime

SRC = "/materials/mobile"
OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "app", "src", "main", "assets")
MEDIA = os.path.join(OUT, "media")

SOURCES = [
    "images/avatars/ayan.png",
    "images/covers/baking.png",
    "images/posts/lake.png",
    "images/products/bag.png",
    "images/avatars/beichen.png",
    "images/covers/morning.png",
    "images/posts/mint.png",
    "images/products/bottle.png",
    "images/avatars/linxi.png",
    "images/covers/notifications.png",
    "images/posts/cookies.png",
    "images/products/camera.png",
    "images/avatars/momo.png",
    "images/covers/room.png",
    "images/posts/shelf.png",
    "images/products/keyboard.png",
    "images/avatars/qiaoqiao.png",
    "images/covers/walk.png",
    "images/placeholders/hero.png",
    "images/products/lamp.png",
    "images/avatars/zhouye.png",
    "images/placeholders/empty.png",
    "images/products/mug.png",
    "images/products/notebook.png",
    "images/products/pens.png",
]

def copy(src_rel, dst_dir, dst_name):
    src = os.path.join(SRC, src_rel)
    dst = os.path.join(dst_dir, dst_name)
    os.makedirs(dst_dir, exist_ok=True)
    shutil.copyfile(src, dst)
    return os.path.getsize(dst)

def main():
    if os.path.isdir(MEDIA):
        shutil.rmtree(MEDIA)
    cam = os.path.join(MEDIA, "camera")
    bbb = os.path.join(MEDIA, "blackboxbench")
    os.makedirs(cam, exist_ok=True)
    os.makedirs(bbb, exist_ok=True)

    albums = []

    # BlackBoxBench album: one image + one video (mirrors the observed fixture album)
    bbb_items = []
    for src_rel, name in (("file_picker/sample_video.mp4", "sample_video.mp4"),
                          ("file_picker/sample_photo.png", "sample_photo.png")):
        size = copy(src_rel, bbb, name)
        bbb_items.append({
            "file": "media/blackboxbench/" + name,
            "name": name,
            "video": name.endswith(".mp4"),
            "size": size,
            "duration": 3000 if name.endswith(".mp4") else 0,
            "date": "24 九月 2026, 14:20",
        })
    # keep the video first so it becomes the album cover (as observed)
    albums.append({
        "name": "BlackBoxBench",
        "path": "/storage/emulated/0/Pictures/BlackBoxBench",
        "items": bbb_items,
    })

    # Camera album: 30 photos drawn from the fictional image pack
    cam_items = []
    start = datetime.date(2026, 6, 1)
    seq = list(range(len(SOURCES))) + [0, 1, 2, 3, 4]
    for i, si in enumerate(seq):
        src_rel = SOURCES[si]
        d = start + datetime.timedelta(days=i * 3 + (i // 7))
        ext = os.path.splitext(src_rel)[1]
        name = "IMG_%s_%03d%s" % (d.strftime("%Y%m%d"), i, ext)
        size = copy(src_rel, cam, name)
        cam_items.append({
            "file": "media/camera/" + name,
            "name": name,
            "video": False,
            "size": size,
            "duration": 0,
            "date": "%d %s %d, %02d:%02d" % (d.day, ["一月","二月","三月","四月","五月","六月","七月","八月","九月","十月","十一月","十二月"][d.month-1], d.year, 9 + (i % 8), (i * 7) % 60),
        })
    albums.append({
        "name": "Camera",
        "path": "/storage/emulated/0/DCIM/Camera",
        "items": cam_items,
    })

    with open(os.path.join(OUT, "library.json"), "w", encoding="utf-8") as fh:
        json.dump({"albums": albums}, fh, ensure_ascii=False, indent=1)

    total = sum(len(a["items"]) for a in albums)
    print("albums=%d items=%d" % (len(albums), total))

if __name__ == "__main__":
    main()
