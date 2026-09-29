# Joplin reproduction supplementary materials

This directory is the target-specific material pack for the `joplin` (Markdown notes) reproduction workspace,
mounted read-only as `/materials/app`. All content is fictional test data.

## Entity materials

- `notes/welcome.md`: a getting-started note (covers heading/list/link/to-do syntax).
- `notes/project-notes.md`: a project note (covers tables/code blocks/image references/blockquotes).
- `notebooks.json`: structured definitions of the notebook hierarchy, tag set, and to-do set.

## Non-entity supplementary information

See `SUPPLEMENT.md`: the notebook/note/tag/to-do model, the supported Markdown rendering scope,
editor vs. preview switching, and attachments with import/export.

## Usage rules

Do not modify this directory. To use a file, copy it into the `/workspace` project first and reference it there;
images embedded in notes can be copied from `/materials/mobile/images/` into the project's
assets folder and referenced from there.
