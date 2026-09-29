# Google Clock reproduction supplementary materials

This directory is the target-specific material pack for the `google_clock` reproduction workspace, mounted read-only as
`/materials/app`. All content is fictional or public specification.

## Entity materials

- `world_clocks.json`: 24 fictional friendly cities with UTC offsets, covering on-the-hour and
  half-hour time zones, for the world clock / home time feature.

## Non-entity supplementary information

See `SUPPLEMENT.md`: the summary-text generation rules for repeating alarms, next-trigger computation,
world-clock and home-time semantics, and timer/stopwatch behavior specs.

## Usage rules

Do not modify this directory. To use a file, copy it into the `/workspace` project first and reference it there;
alarm sounds can reuse the WAV samples in `/materials/mobile/audio/`.
