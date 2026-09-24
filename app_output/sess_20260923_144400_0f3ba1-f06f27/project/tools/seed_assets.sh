#!/usr/bin/env bash
# Copies public synthetic materials into the app assets and generates media/manifest.json
set -euo pipefail
cd "$(dirname "$0")/.."

A=app/src/main/assets
rm -rf "$A/media"
mkdir -p "$A/media/camera" "$A/media/blackboxbench" "$A/media/movies"

SRC=/materials/mobile
MAN="$A/media/manifest.json"

CAM=(
  "$SRC/images/products/camera.png"
  "$SRC/images/covers/morning.png"
  "$SRC/images/products/mug.png"
  "$SRC/images/covers/baking.png"
  "$SRC/images/products/lamp.png"
  "$SRC/images/posts/lake.png"
  "$SRC/images/products/notebook.png"
  "$SRC/images/covers/walk.png"
  "$SRC/images/products/keyboard.png"
  "$SRC/images/posts/cookies.png"
  "$SRC/images/avatars/momo.png"
  "$SRC/images/products/bag.png"
  "$SRC/images/covers/room.png"
  "$SRC/images/posts/mint.png"
  "$SRC/images/avatars/ayan.png"
  "$SRC/images/products/bottle.png"
  "$SRC/images/covers/notifications.png"
  "$SRC/images/posts/shelf.png"
  "$SRC/images/avatars/linxi.png"
  "$SRC/images/products/pens.png"
  "$SRC/images/placeholders/hero.png"
  "$SRC/images/avatars/beichen.png"
  "$SRC/images/avatars/qiaoqiao.png"
  "$SRC/images/avatars/zhouye.png"
)

{
  echo '{'
  echo '  "albums": ['
  echo '    {'
  echo '      "name": "Camera",'
  echo '      "items": ['
  first=1
  for i in "${!CAM[@]}"; do
    day=$(date -d "2026-08-29 -$i day" +%Y%m%d)
    hh=$(printf "%02d" $(( (i * 3 + 8) % 24 )))
    mm=$(printf "%02d" $(( (i * 7 + 12) % 60 )))
    stamp=$(date -d "2026-08-29 -$i day ${hh}:${mm}" +%s)
    ms=$((stamp * 1000))
    nnn=$(printf "%03d" $((29 - i)))
    name="IMG_${day}_${nnn}.png"
    cp "${CAM[$i]}" "$A/media/camera/$name"
    if [ $first -eq 1 ]; then first=0; else echo ','; fi
    printf '        {"file": "camera/%s", "name": "%s", "modified": %s, "taken": %s}' \
      "$name" "$name" "$ms" "$ms"
  done
  echo ''
  echo '      ]'
  echo '    },'
  echo '    {'
  echo '      "name": "BlackBoxBench",'
  echo '      "items": ['
  cp "$SRC/video/story_loop.mp4" "$A/media/blackboxbench/sample_video.mp4"
  cp "$SRC/file_picker/sample_photo.png" "$A/media/blackboxbench/sample_photo.png"
  v1=$(date -d "2026-09-15 10:20" +%s); v1=$((v1 * 1000))
  v2=$(date -d "2026-09-15 10:21" +%s); v2=$((v2 * 1000))
  printf '        {"file": "blackboxbench/sample_video.mp4", "name": "sample_video.mp4", "modified": %s, "taken": %s},\n' "$v1" "$v1"
  printf '        {"file": "blackboxbench/sample_photo.png", "name": "sample_photo.png", "modified": %s, "taken": %s}\n' "$v2" "$v2"
  echo '      ]'
  echo '    },'
  echo '    {'
  echo '      "name": "Movies",'
  echo '      "items": ['
  cp "$SRC/video/product_demo.mp4" "$A/media/movies/product_demo.mp4"
  cp "$SRC/video/story_loop.mp4" "$A/media/movies/story_loop.mp4"
  m1=$(date -d "2026-09-10 14:00" +%s); m1=$((m1 * 1000))
  m2=$(date -d "2026-09-12 16:30" +%s); m2=$((m2 * 1000))
  printf '        {"file": "movies/product_demo.mp4", "name": "product_demo.mp4", "modified": %s, "taken": %s},\n' "$m1" "$m1"
  printf '        {"file": "movies/story_loop.mp4", "name": "story_loop.mp4", "modified": %s, "taken": %s}\n' "$m2" "$m2"
  echo '      ]'
  echo '    }'
  echo '  ]'
  echo '}'
} > "$MAN"

cp "$SRC/images/launcher_icon.png" "$A/launcher_icon.png"
ls -1 "$A/media/camera" | wc -l
echo "manifest: $(wc -c < "$MAN") bytes"
