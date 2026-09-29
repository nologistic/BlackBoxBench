# Loop Habit Tracker reproduction supplementary information (non-entity materials)

## Habit types and frequency

| Type | Description | Check-in form |
|---|---|---|
| Boolean (yes/no) | Done counts as complete | A checkmark |
| Measurable | With a unit and target value | Enter a number; only reaching the target completes it |

- Frequency model:
  - Daily (once a day)
  - N times per week (complete any N days within the week; the color ring aggregates by week)
  - Specific weekdays (e.g. only Mon/Wed/Fri; only those days count)
- Target value: the target and direction for measurable habits (≥ target = complete); the unit is free text
  (pages, minutes, km…).

## Check-in semantics

- Repeated taps on the same day add a record (accumulating the value for measurable habits); long-press or the details page
  allows undoing/deleting records.
- A completed today shows a checkmark/closed color ring in the main list; otherwise it shows progress.
- Multiple records in one day are shown as a count in the details calendar.

## Score (habit strength)

Score measures recent consistency, ranging 0–100:

```
score_today = (1 - λ) × score_yesterday + λ × (completed today ? 1 : 0)
λ ≈ 0.05 (freshness weight, about a two-week half-life)
```

- Consistent completion → Score approaches 100; repeated misses → it decays toward 0.
- **After editing a past day's record, Score and streak are recomputed from the new history** (semantically
  equivalent to replaying up to today).
- Streak: consecutive days/weeks meeting the frequency; N-per-week habits are judged
  by week, with streaks counted in weeks.

## History backfill and correction

- The details page opens a calendar/history: back-fill any past day or undo a mistaken check.
- Backfill reflects immediately in the main list's Score, streak, and monthly statistics.

## Archiving and reminders

- Archive: habits no longer tracked disappear from the main list (a filter can show archived items),
  history is kept; unarchiving resumes tracking.
- Reminder: a notification at a fixed daily time; fires when today is incomplete and not again after completion.

## Key reproduction behaviors

- Main list: habit name, frequency summary, today's state (check/color ring), Score, streak.
- Details page: 30-day/year calendar heatmap, monthly statistics, a history-backfill entry.
- Write operations (check-in/backfill/archive/edit habit configuration) survive a restart.
