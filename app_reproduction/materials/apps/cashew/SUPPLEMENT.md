# Cashew reproduction supplementary information (non-entity materials)

## Accounts and primary account

- Account types: cash, bank card, savings, etc.; each account is bound to one currency.
- **Primary account**: globally unique; new transactions default to its account and currency.
- Switching the primary account only changes defaults, never historical transactions.

## Three-way transaction taxonomy

| Type | Behavior |
|---|---|
| Expense | Deducted from an account, counted in expense statistics |
| Income | Added to an account, counted in income statistics |
| Transfer | From account A to B (may cross currencies), **not counted in income/expense statistics** |

- Each transaction: amount, date, category (required for expense/income, optional for transfers), account, notes.
- Cross-currency transfers are booked into the target currency at a fixed rate.

## Currencies and fixed rates

- Currency set and conversion base (the reproduction uses **fixed rates**, not live quotes):

| Currency | 1 unit ≈ CNY |
|---|---|
| CNY | 1 |
| USD | 7.2 |
| EUR | 7.8 |
| JPY | 0.048 |
| GBP | 9.1 |

- Summaries (net worth, this month's income/expense, budget progress) are displayed converted into the primary account's currency.

## Budget model

- **Category budget**: a monthly cap per expense subcategory; progress = that category's spend this month / cap,
  overspending shows a warning color.
- **All Transactions dynamic budget**: configurable included transaction types (expenses only, etc.),
  participating accounts, and included categories, with the ability to **exclude specific categories**; progress uses the same formula
  over the filtered transaction set.
- The budget period is the calendar month; progress resets across months while the cap stays.

## Recurring transactions

- Transactions can carry repeat rules (weekly/monthly/yearly); instances generate automatically when due,
  and an instance can be edited without affecting future generation.

## Statistics and key reproduction behaviors

- Home: each account's balance (original currency + primary-currency conversion), this month's income/expense.
- Statistics page: category shares (ring/bar), income/expense trend (by month).
- Deleting a transaction asks for confirmation; editing keeps the original date.
- All write operations survive a restart.
