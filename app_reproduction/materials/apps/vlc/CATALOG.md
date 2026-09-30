# VLC reproduction supplementary materials

This directory is the target-specific material pack for the `vlc` (multimedia player) reproduction workspace, mounted as
read-only `/materials/app`. All content is fictional test data.

## Entity materials

- `subtitle_sample.srt`: an SRT subtitle sample (bilingual paragraphs), for external-subtitle
  loading.
- `playlists.m3u`: two M3U playlist samples.

## Non-entity supplementary information

See `SUPPLEMENT.md`: media library grouping, player gestures, subtitles and audio tracks, equalizer and
playback speed, and playlist semantics.

## Usage rules

Do not modify this directory. To use a file, copy it into the `/workspace` project first and reference it there;
playback media reuses the MP4 files in `/materials/mobile/video/` and the WAV samples in
`/materials/mobile/audio/`.
