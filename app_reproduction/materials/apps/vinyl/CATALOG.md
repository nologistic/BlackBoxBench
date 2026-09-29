# Vinyl Music Player reproduction supplementary materials

This directory is the target-specific material pack for the `vinyl` (local music player) reproduction workspace, mounted as
read-only `/materials/app`. All artists and tracks are fictional.

## Entity materials

- `music_library.json`: a fictional library (4 artists, 6 albums, 24 tracks,
  with duration/track number/year/genre) — **necessary data** for the music-library feature.
- `playlists.m3u`: two M3U playlist samples (morning commute / late-night coding).

## Non-entity supplementary information

See `SUPPLEMENT.md`: the library aggregation model (artist/album/genre/year), playback queue and
shuffle/repeat semantics, the vinyl animation, and the sleep timer.

## Usage rules

Do not modify this directory. To use a file, copy it into the `/workspace` project first and reference it there;
playback audio reuses the WAV samples in `/materials/mobile/audio/` (rotated per track).
