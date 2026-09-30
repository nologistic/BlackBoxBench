# Fossify Calendar reproduction supplementary materials

This directory is the target-specific material pack for the `fossify_calendar` reproduction workspace, mounted read-only as
`/materials/app`. All content is fictional test data.

## Entity materials

- `events.json`: 3 fictional calendars + 14 events covering one-off, daily, weekly (including
  weekday combinations), monthly, yearly, and end-date-bounded recurrence rules.
- `rrules.txt`: a quick reference for a RRULE subset with expansion examples — the **necessary data**
  for "next occurrence" computation.

## Non-entity supplementary information

See `SUPPLEMENT.md`: the event/task/reminder data model, RRULE semantics, multi-calendar and color
assignment, view switching, and week-start day.

## Usage rules

Do not modify this directory. To use a file, copy it into the `/workspace` project first and reference it there.
