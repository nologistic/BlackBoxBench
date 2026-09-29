# Tasks.org reproduction supplementary information (non-entity materials)

## Task data model

| Field | Description |
|---|---|
| Title / notes | Required / optional |
| List | The task list it belongs to (work/life/shopping…) |
| Due date | May include a time; an optional "start date" forms a range |
| Repeat | On completion, the next occurrence is generated per the rule |
| Priority | High/medium/low (0 = none), affects sorting and visual markers |
| Tags | Multi-valued, cross-list grouping |
| Subtasks | Two-level nesting; completing a parent does not force its subtasks |
| Completion state | Records a completion timestamp; can be restored |

- Quick date options: Today / Tomorrow / Next Sunday / No date.

## Repeat rules and next occurrence

- When a repeating task is completed, the current instance is marked complete and **the next occurrence is generated on the same entry immediately**
  (not a new row, but the same task's next instance), advanced per the rule:

| Rule | Next occurrence |
|---|---|
| Daily | Same time tomorrow |
| Weekly (Monday) | Next Monday |
| Monthly on the 15th | The 15th of next month |
| Overdue completion | Advance from the completion day; skipped instances are not back-filled |

## Filtering and sorting

- List filter: view by list; the "hide completed" switch removes completed items from the list
  while the data remains (visible again when switched off).
- Sorting: by due date / priority / title / manual drag, ascending or descending.
- Tags page: tap a tag to see same-tag tasks across lists.

## Complete / restore / delete

- Check complete → checkmark + completion timestamp; moves to the bottom by default or hides per settings.
- Restore: uncheck, returning to the incomplete area.
- Delete: disappears from the list; deleted items take part in no filtering or statistics.

## Key reproduction behaviors

- Four complete core screens: list page (with badge: incomplete count), task details,
  create/edit form, and filters.
- Toggling "hide completed" makes completed items disappear while the data stays; switching back shows them.
- Completing a repeating task makes the new instance appear immediately with the correct date.
- All write operations survive a restart.
