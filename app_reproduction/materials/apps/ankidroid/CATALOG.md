# AnkiDroid reproduction supplementary materials

This directory is the target-specific material pack for the `ankidroid` (spaced-repetition flashcards) reproduction workspace,
mounted read-only as `/materials/app`. All content is fictional test data.

## Entity materials

- `sample_cards.txt`: sample cards in TSV format (deck, tags, front, back),
  matching AnkiDroid's text-import format; 12 entries covering three deck branches.
- `cloze_cards.txt`: 5 cloze cards demonstrating the `{{c1::}}` syntax.
- `deck_tree.json`: the deck hierarchy (with card counts per level), matching the
  deck-tree view in the browser.

## Non-entity supplementary information

See `SUPPLEMENT.md`: deck-hierarchy syntax, the relation between notes and cards, scheduling behavior
for the four rating buttons, due/new-card queues, and import/export with statistics conventions.

## Usage rules

Do not modify this directory. To use a file, copy it into the `/workspace` project first and reference it there.
