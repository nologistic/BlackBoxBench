# Material Files reproduction supplementary materials

This directory is the target-specific material pack for the `material_files` (file manager) reproduction workspace,
mounted read-only as `/materials/app`. All content is fictional test data.

## Entity materials

- `sample_tree.json`: a fictional directory tree (documents/images/archives/hidden files/
  empty dirs) with sizes and mtimes, for browsing, sorting, and hidden-file demos.

## Non-entity supplementary information

See `SUPPLEMENT.md`: the storage model, sorting/view switching, archive behavior, bookmarks,
hidden-file rules, and the semantics of create/rename/delete.

## Usage rules

Do not modify this directory. To use a file, copy it into the `/workspace` project first and reference it there;
drill-down fake files can reuse the samples in `/materials/mobile/file_picker/`.
