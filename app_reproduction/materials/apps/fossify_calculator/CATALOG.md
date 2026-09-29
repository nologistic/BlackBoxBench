# Fossify Calculator reproduction supplementary materials

This directory is the target-specific material pack for the `fossify_calculator` reproduction workspace, mounted read-only as
`/materials/app`. All content is fictional or public specification.

## Entity materials

- `units.json`: complete conversion-factor tables for nine unit families (length/area/volume/mass/temperature/time/
  speed/pressure/energy), including the affine temperature formulas. **Necessary data** for unit conversion.
- `expressions.txt`: calculator expression samples with expected results (precedence, parentheses, percent
  semantics, chained operations), usable as an implementation and self-check reference.

## Non-entity supplementary information

See `SUPPLEMENT.md`: arithmetic behavior specs, percent semantics, the unit-conversion interaction model,
and history with state retention.

## Usage rules

Do not modify this directory. To use a file, copy it into the `/workspace` project first and reference it there.
