# Cashew reproduction supplementary materials

This directory is the target-specific material pack for the `cashew` (personal budgeting) reproduction workspace, mounted read-only as
`/materials/app`. All accounts, transactions, and budgets are fictional test data.

## Entity materials

- `accounts.json`: 4 fictional accounts (cash/salary card/travel card/savings), bound to
  CNY/USD/EUR multi-currency.
- `categories.json`: a two-level category system (expense groups/subcategories + income categories).
- `transactions.json`: 16 fictional transactions covering expense/income/transfer, multi-currency,
  multiple categories, and date distribution.
- `budgets.json`: per-category budgets and an All Transactions dynamic budget (with excluded categories and
  account scope).

## Non-entity supplementary information

See `SUPPLEMENT.md`: account and primary-account semantics, the three-way transaction taxonomy, fixed-rate conversion,
budget-progress conventions, and recurring transactions with statistics views.

## Usage rules

Do not modify this directory. To use a file, copy it into the `/workspace` project first and reference it there.
