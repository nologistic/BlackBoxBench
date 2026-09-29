# Fossify Gallery reproduction supplementary information (non-entity materials)

## Album aggregation

- An album = the media set of one folder (aggregated by path); the "Albums" page shows grid covers
  + counts; the "Library" page aggregates all media by day.
- Media types: JPEG/PNG/GIF/WebP/BMP, MP4 video, optional RAW (placeholder shown).
- Folders containing `.nomedia` do not appear in albums (files remain; they show after enabling
  hidden items in settings).

## Recycle bin

- Delete → into the recycle bin (not really deleted), **kept 30 days** then auto-purged;
  items can be restored (back to their album) or deleted permanently at once.
- Deleting one item updates its album's order and count; other albums are unaffected.

## Favorites and sorting

- Favorite: item menu → add to favorites → the same MediaItem appears on the "Favorites" aggregation page;
  unfavoriting removes it from that page (the original album is unaffected).
- Sort: name / path / size / modified date / taken date / random, ascending or descending.
- The choice persists.

## Viewer and editor

- Viewer: pinch/double-tap zoom, gesture rotation, swipe to switch within the album;
  settings control brightness/swipe-down-to-exit/edge switching/keep screen on.
- Built-in editor: crop (free/aspect), rotate 90°, flip horizontal/vertical;
  saving creates a new file (original kept) — the "save as copy" semantic.
- System-level actions: Share / Open with / Set as (wallpaper/contact photo).

## Security and privacy

- App lock: PIN/pattern protects the whole app or only the "Hidden" section.
- Hide: item menu → move into the hidden folder (.nomedia takes effect) → gone from normal views,
  visible after unlocking; restoring returns it to its album.
- The "Recycle bin" and "Hidden" entries and their visibility persist.

## Storage permission note

The system media-store permission is unavailable (sandbox limitation): media comes from app-private copies of the built-in materials,
with complete functional semantics — an **intentional difference** from a real device.
