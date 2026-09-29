# Librera Reader reproduction supplementary information (non-entity materials)

## Format support

| Format | Highlights |
|---|---|
| EPUB | XHTML chapters + CSS styles, table of contents (TOC), reflowable |
| PDF | Fixed-layout pagination, zoom, page jumps |
| FB2 | XML structure (description + body), reflowable |
| MOBI | Legacy Kindle format, reflowable |

- One library mixes all four formats; the format decides the reading mode (reflow vs. original layout).

## Library

- Scan: after configuring "included folders", books inside are indexed automatically → the list shows cover,
  title, author, format, progress.
- New books placed in a scanned folder appear after a re-scan; deleted files disappear.
- "Folder as book": a folder containing several books (e.g. a collection) appears as a single entry,
  opening into an in-folder book picker.
- Recent list and reading progress: opening resumes the last position; the list shows a percentage.

## Reading modes

- Page-turn (tap the left/right area) / scroll / musician mode (wide horizontal pages) are switchable;
  night mode (inverted filter), font size, line spacing, and margins take effect instantly and are remembered per book.
- Status bar: current page/percentage configurable (top/bottom/off).

## Bookmarks / highlights / TTS

- Bookmark: current reading position → add bookmark → jump back from the list; each book keeps its own.
- Highlight: select text → highlight (multiple colors) / annotate → still there after saving and reopening.
- TTS: books with a text layer can be read aloud with automatic page turning; the sandbox may use silent
  synthesized audio or just drive the progress.

## Profiles and backup

- Profiles: multiple reading-preference sets (font/theme/margins), nameable and switchable, independent of each other.
- Backup: bookmarks/progress/config/book list export to one file; importing restores in a new environment.
- Library path migration: after books move to a new directory, a backup restore re-associates them.

## Key reproduction behaviors

- Library (grid/list) → open a book → reader (page turn/progress) → bookmark jump-back
  is the core loop; cross-format interaction consistency matters more than reflow precision.
