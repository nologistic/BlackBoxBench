# Feeder reproduction supplementary materials

This directory is the target-specific material pack for the `feeder` (RSS/Atom/JSON feed reader) reproduction workspace,
mounted read-only as `/materials/app`. All entries are fictional test data.

## Entity materials

- `feeds/blackbox_times.rss`: a fictional news feed in RSS 2.0 format (5 items).
- `feeds/tech_digest.atom`: a fictional tech feed in Atom format (4 items).
- `feeds/design_notes.json`: a fictional design feed in JSON Feed 1.1 format (4 items).
- `subscriptions.opml`: a subscription list containing the three feeds above plus two extra fictional feeds,
  ready for the "OPML import" demo.

## Non-entity supplementary information

See `SUPPLEMENT.md`: structural highlights of the three feed formats and the unified Feed/Article data
model, the OPML format, sync and the "new article" rule, and image-field sources.

## Usage rules

Do not modify this directory. To use a file, copy it into the `/workspace` project first and reference it there;
article images can reuse the fictional images in `/materials/common/images/covers/` and `posts/`.
