# Librera Reader reproduction supplementary materials

This directory is the target-specific material pack for the `librera` (e-book reader) reproduction workspace,
mounted read-only as `/materials/app`. All content is fictional test data.

## Entity materials

- `library.json`: a fictional library (12 books covering EPUB/PDF/FB2/MOBI,
  multiple authors, various reading progress and tags) — **necessary data** for the library feature.
- `sample_book.json`: chapter structure and body-paragraph samples of a fictional EPUB;
the reader can render content organized from it directly.

## Non-entity supplementary information

See `SUPPLEMENT.md`: the format-support matrix, library scanning and "folder as book",
reading modes and page turning, bookmarks/highlights, TTS, configuration profiles, and backup migration.

## Usage rules

Do not modify this directory. To use a file, copy it into the `/workspace` project first and reference it there;
book covers can reuse the fictional cover images in `/materials/mobile/images/covers/`.
