# Fossify Calculator reproduction supplementary information (non-entity materials)

## Basic calculator behavior specs

- Arithmetic follows mathematical precedence: `^` (power) > `×`/`÷` > `+`/`−`, parentheses first.
- **Percent semantics**: `50+10%` = 55 (10% applies to the preceding operand 50, i.e. add 5);
  `100−25%` = 75; `200×10%` = 20. This is the common Android-calculator semantics,
  not `50 + 0.1`.
- Decimal point: at most one in the input; `0.5` may drop the leading 0 and display as `.5`.
- Backspace (DEL) removes the last digit; long-press or `AC` clears the expression and result.
- `=` ends the current expression and shows the result; typing a new digit starts a new expression,
  while typing an operator continues from the result.
- History: completed expressions are saved newest-first (expression = result), tapping refills,
  and clearing history asks for confirmation.

## Unit-conversion interaction model

- Entering conversion from the main screen gives nine category entries: length, area, volume, mass, temperature,
  time, speed, pressure, energy — **every category page keeps the same interaction skeleton**:
  a numeric keypad on top + two unit pickers (from/to) + a live result.
- Input maps to the result live; switching the from/to units recomputes immediately.
- Unit pairs are swappable (tap the swap button or select symmetrically).
- Each category page remembers its last unit selection independently and keeps it on re-entry.
- The factors come from `units.json`; temperature uses formulas rather than factors.

## State and persistence

- The conversion category, unit selections, and history survive a restart.
- Rotating the screen does not lose the current input (or it is fixed by orientation=portrait).
