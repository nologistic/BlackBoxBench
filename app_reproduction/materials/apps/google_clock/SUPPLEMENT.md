# Google Clock reproduction supplementary information (non-entity materials)

## Repeat-rule summary text

After selecting any combination of Monday–Sunday for an alarm, the list row shows a summary:

| Weekday combination | Summary |
|---|---|
| All seven days | Every day |
| Once only (nearest future day) | Once |
| Monday–Friday | Mon–Fri |
| Saturday and Sunday only | Sat, Sun |
| Other combinations | The list of selected weekdays (e.g. "Mon, Wed, Fri") |

- The summary updates when the weekdays change.

## Next-trigger computation

```
Next trigger = the next date that is "today or later" with weekday ∈ the selected set,
                at the alarm time; if that time has not yet passed today, today counts.
```

- "Next ring" is shown as: 7:00 AM tomorrow / rings in 3 hours / 10:00 PM tonight.
- When the alarm toggle is off: it is excluded from the "next alarm" computation while its configuration stays;
  turning it back on restores participation and recomputes.
- "Pause" for a repeating alarm: set start/end dates; it does not fire within the range,
  and the original repeat rule resumes automatically after the range ends (**not deleted**).

## World clock and home time

- Add a city via "+" on the clock page → after saving it appears there with the city name,
  current time, and offset from local time ("6 hours behind").
- Home time: after enabling "automatically show home location time", the extra row shows only
  when the device time zone differs from home; not shown when identical.
- Time-zone table in `world_clocks.json`; cross-zone conversion uses UTC offsets.

## Timer and stopwatch

- Timer: numeric keypad enters HH:MM:SS, backspace removes the last digit, empty time cannot start;
  Start → count down → Pause freezes → Resume continues → switching away and back keeps
  the remaining time → Reset returns to the entered value (non-zero initial state).
- At zero → finished state (ringing + stop/extend buttons); state clears after dismissing.
- Stopwatch: start → keeps accumulating → pause freezes → start resumes from the value →
  Reset returns to 00:00 (same instance, not a new one).
- The clock page's current time updates continuously (seconds optional), with date and weekday in sync.
