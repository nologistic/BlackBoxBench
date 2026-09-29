# AntennaPod reproduction supplementary materials

This directory is the target-specific material pack for the `antennapod` (podcast subscription and playback) reproduction workspace,
mounted read-only as `/materials/app`. All content is fictional test data.

## Entity materials

- `podcast_feed.rss`: a fictional podcast feed with iTunes extension tags (1 season, 6 episodes),
  ready for the "add podcast" demo.
- `episodes.json`: structured data for 6 episodes (title/duration/date/description/chapters),
  suitable for direct embedding.

## Non-entity supplementary information

See `SUPPLEMENT.md`: the iTunes extension fields for podcast RSS, the episode and chapter model,
queue/download/playback state, and subscription management with OPML.

## Usage rules

Do not modify this directory. To use a file, copy it into the `/workspace` project first and reference it there;
audio samples for playback reuse the three WAV files in `/materials/mobile/audio/`.
