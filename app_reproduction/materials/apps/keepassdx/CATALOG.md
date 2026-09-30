# KeePassDX reproduction supplementary materials

This directory is the target-specific material pack for the `keepassdx` (password manager) reproduction workspace,
mounted read-only as `/materials/app`. All entries are fictional test data.

## Entity materials

- `sample_database.json`: the full structure of a fictional database (group tree, entry fields,
  recycle bin, settings); every unlocked screen state can be driven from it.

## Non-entity supplementary information

See `SUPPLEMENT.md`: kdbx concepts and the master key (password + key file), the group and entry
model, recycle-bin semantics, locking and timeouts, and search with security behavior.

## Usage rules

Do not modify this directory. To use a file, copy it into the `/workspace` project first and reference it there.
