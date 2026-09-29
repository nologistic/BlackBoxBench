# AnkiDroid reproduction supplementary information (non-entity materials)

## Deck hierarchy

- Decks separate levels with `::`: `Language::Japanese::Kana` is a three-level deck.
- Same-named paths merge; shown as a collapsible tree, a parent deck's due count = itself + all child decks.
- Cards always belong to a leaf or any level; parent levels aggregate statistics.

## Notes and cards

- A **note** is a set of fields (front/back/extra fields);
- A **card** is generated from a note + a template. The Basic template makes 1 card per note
  (front→back); "Basic (and reversed card)" makes 2 (front→back, back→front).
- The cloze template blanks text: `{{c1::Tokyo}} is the capital of Japan` shows
  `[…] is the capital of Japan` on the front and the full sentence on the back. One sentence can have several blanks `c1/c2/c3`,
  and each blank generates its own card.

## Scheduling (simplified SM-2)

Card states: new → learning → review.

- **New-card queue**: enters learning one by one up to the daily cap (default 20).
- **Learning**: short-step intervals (1 min → 10 min); a wrong answer goes back to the first step.
- **Review**: intervals in days (1 day → 3 days → 7 days → 2 weeks → 1 month…),
  multiplied or reduced by the rating.
- Four rating buttons:

| Button | Meaning | Effect on interval |
|---|---|---|
| Again | Wrong answer | Interval resets, back to learning (1 min) |
| Hard | Right but struggled | Interval grows slightly (about ×1.2) |
| Good | Normal correct answer | Interval multiplies (about ×2.5) |
| Easy | Instant answer | Interval grows strongly (about ×4) |

- Each card has its own due date; the "due today" badge = review cards due ≤ today +
  today's new-card quota. After one is answered the next appears automatically (front → tap to show back →
  rate with the four buttons).
- **Ratings must affect the next due date**: an "Again" card reappears the same day.

## Browser and statistics

- The browser filters by deck/tag/template/due; list columns show question, answer, deck, due.
- Sort keys: deck, card template, due date, ascending or descending.
- Statistics page: today's study volume (new/review/relearning), accuracy, and a 30-day due forecast.

## Import and export

- TSV import: one note per line, fields separated by tabs, optional first line with directives
  such as `#separator:tab`; the target deck and whether a tag column exists can be specified.
- Export: the selected deck's note set can be re-imported unchanged (round-trip set equality).

## Key reproduction behaviors

- A real SM-2 implementation is not required; but the four buttons' qualitative effect on "when a card appears next"
  (Again = back immediately, Good = longer interval) must be observable.
- Deck-tree aggregate numbers, the due-today badge, and the study flow (flip → rate) are the core visible behaviors.
