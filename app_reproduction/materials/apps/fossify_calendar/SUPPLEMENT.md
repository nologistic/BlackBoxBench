# Fossify Calendar reproduction supplementary information (non-entity materials)

## Data model

| Object | Fields |
|---|---|
| Calendar | Name, color, visibility toggle; several can be created locally |
| Event | Title, start/end datetime, all-day toggle, owning calendar, location, description, reminders, repeat rule |
| Task | Title, due date, completion state, owning calendar |
| Reminder | Notify n minutes before the event (multiple offsets optional) |

- An event belongs to one calendar; the calendar color renders all its events (color bar/dot).
- The default calendar for new items is configurable; concurrent events from multiple calendars show side by side.

## Repeat rules (RRULE subset)

The reproduction must support the following combinations, with iCalendar RRULE semantics:

- `FREQ=DAILY;INTERVAL=1` every day; `INTERVAL=2` every other day.
- `FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR` weekdays; `BYDAY=SA,SU` weekends;
  a single day `BYDAY=WE` every Wednesday.
- `FREQ=MONTHLY;BYMONTHDAY=15` the 15th monthly.
- `FREQ=YEARLY;BYMONTH=9;BYMONTHDAY=10` September 10 yearly.
- `UNTIL=20261231` stops after that date; `COUNT=10` stops after 10 occurrences.

**Next-trigger computation**: from "now", find the first rule-matching date ≥ today;
modify exceptions of a repeating event with EXDATE (a simplified "cancel this occurrence" semantics is acceptable, but
deleting one occurrence and "delete all" must be distinguished).

- End time = start + duration; multi-day events render across cells in week/month views.

## Views and settings

- Views: day / week / month / year / agenda, current date highlighted, swipe
  to the previous/next period, a "Today" button returns to today.
- Week-start setting: Sunday / Monday / Saturday, affecting the first column of week and month views.
- All-day events form their own row at the top of day/week views.
- The system calendar permission is unavailable (sandbox limitation); all data is stored in-app and persists;
  this is an **intentional difference** from a real device, with the functionality itself complete.
