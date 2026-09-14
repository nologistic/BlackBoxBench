# Functional Topology — cashew

- session: `sess_20260911_170308_b40b50`
- generated: 2026-09-11T18:06:56.036812+00:00
- coverage: 51 states · 10 features · 3 data · 3 edges (confirmed ratio 100%, 155 actions)

## Graph
```
cashew
├─ States
│  ├─ [✓] Onboarding_SpendingHabits (1.00) `state_onboarding_spendinghabits`
│  │    ─TRANSITIONS_TO→ Onboarding_CreateBudget
│  ├─ [✓] Onboarding_CreateBudget (1.00) `state_onboarding_createbudget`
│  ├─ [✓] CurrencyPicker (1.00) `state_currencypicker`
│  ├─ [✓] AmountCalculator (1.00) `state_amountcalculator`
│  ├─ [✓] PeriodLengthInput (1.00) `state_periodlengthinput`
│  ├─ [✓] PeriodPicker (1.00) `state_periodpicker`
│  ├─ [✓] BudgetStartDatePicker (1.00) `state_budgetstartdatepicker`
│  ├─ [✓] Onboarding_SignIn (1.00) `state_onboarding_signin`
│  ├─ [✓] NotificationPermission (1.00) `state_notificationpermission`
│  ├─ [✓] Home_Empty (1.00) `state_home_empty`
│  ├─ [✓] AddTransaction_Title (1.00) `state_addtransaction_title`
│  ├─ [✓] TransactionCategoryPicker (1.00) `state_transactioncategorypicker`
│  ├─ [✓] Home_WithExpense (1.00) `state_home_withexpense`
│  ├─ [✓] Transactions_MonthList (1.00) `state_transactions_monthlist`
│  ├─ [✓] EditTransaction (1.00) `state_edittransaction`
│  ├─ [✓] TransactionSearch (1.00) `state_transactionsearch`
│  ├─ [✓] TransactionSearch_Empty (1.00) `state_transactionsearch_empty`
│  ├─ [✓] TransactionFilters (1.00) `state_transactionfilters`
│  ├─ [✓] Budgets_List (1.00) `state_budgets_list`
│  ├─ [✓] BudgetDetail (1.00) `state_budgetdetail`
│  ├─ [✓] EditBudget (1.00) `state_editbudget`
│  ├─ [✓] CategorySpendingGoals (1.00) `state_categoryspendinggoals`
│  ├─ [✓] BudgetHistory (1.00) `state_budgethistory`
│  ├─ [✓] BudgetTypePicker (1.00) `state_budgettypepicker`
│  ├─ [✓] BudgetDirectionPicker (1.00) `state_budgetdirectionpicker`
│  ├─ [✓] ExtrasTop (1.00) `state_extrastop`
│  ├─ [✓] SettingsTop (1.00) `state_settingstop`
│  ├─ [✓] SettingsLower (1.00) `state_settingslower`
│  ├─ [✓] AdvancedSettingsTop (1.00) `state_advancedsettingstop`
│  ├─ [✓] AdvancedSettingsMiddle (1.00) `state_advancedsettingsmiddle`
│  ├─ [✓] AdvancedSettingsLower (1.00) `state_advancedsettingslower`
│  ├─ [✓] AllSpendingSummary (1.00) `state_allspendingsummary`
│  ├─ [✓] AccountList (1.00) `state_accountlist`
│  ├─ [✓] EditAccount (1.00) `state_editaccount`
│  ├─ [✓] CorrectAccountBalance (1.00) `state_correctaccountbalance`
│  ├─ [✓] AddAccount (1.00) `state_addaccount`
│  ├─ [✓] AccountsMultiple (0.99) `state_accountsmultiple`
│  ├─ [✓] CategoryManagement (0.99) `state_categorymanagement`
│  ├─ [✓] EditCategory (0.99) `state_editcategory`
│  ├─ [✓] TitleManagement (0.99) `state_titlemanagement`
│  ├─ [✓] ScheduledEmpty (0.99) `state_scheduledempty`
│  ├─ [✓] SubscriptionsEmpty (0.99) `state_subscriptionsempty`
│  ├─ [✓] GoalsEmpty (0.99) `state_goalsempty`
│  ├─ [✓] GoalTypePicker (0.99) `state_goaltypepicker`
│  ├─ [✓] AddGoal (0.99) `state_addgoal`
│  ├─ [✓] LoansEmpty (0.99) `state_loansempty`
│  ├─ [✓] LoanTypePicker (0.99) `state_loantypepicker`
│  ├─ [✓] HomeMultipleAccounts (0.99) `state_homemultipleaccounts`
│  ├─ [✓] TransactionsMixed (0.99) `state_transactionsmixed`
│  ├─ [✓] BalanceTransfer (0.99) `state_balancetransfer`
│  ├─ [✓] EditHome (0.99) `state_edithome`
├─ Features
│  ├─ [✓] Onboarding_Navigation (1.00) `feature_onboarding_navigation`
│  ├─ [✓] CreateExpenseTransaction (1.00) `feature_createexpensetransaction`
│  ├─ [✓] ExcludeTransactionAmount (1.00) `feature_excludetransactionamount`
│  ├─ [✓] EditTransactionDetails (1.00) `feature_edittransactiondetails`
│  ├─ [✓] SearchTransactions (1.00) `feature_searchtransactions`
│  ├─ [✓] CreateBudget (1.00) `feature_createbudget`
│  ├─ [✓] CreateAccount (0.99) `feature_createaccount`
│  ├─ [✓] CreateIncomeTransaction (0.99) `feature_createincometransaction`
│  ├─ [✓] TransferBetweenAccounts (0.99) `feature_transferbetweenaccounts`
│  ├─ [✓] PersistFinanceData (0.99) `feature_persistfinancedata`
├─ Data
│  ├─ [✓] Account (1.00) `data_account`
│  ├─ [✓] Budget (1.00) `data_budget`
│  ├─ [✓] Transaction (0.90) `data_transaction`
│  │    ─MUTATES→ Account
│  │    ─MUTATES→ Budget
```

## Features
### Onboarding_Navigation `feature_onboarding_navigation`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 引导页可通过左右箭头前进与后退；点击第 1 页右下角箭头后切换至预算设置页。
- evidence: step 1 (frame 1 → 2)

### CreateExpenseTransaction `feature_createexpensetransaction`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 用户选择支出分类并输入正数金额后可保存；保存会扣减账户余额和预算剩余、增加交易计数并更新首页图表。
- evidence: step 28 (frame 55 → 56)

### ExcludeTransactionAmount `feature_excludetransactionamount`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 编辑交易展开更多选项后可关闭计入金额；保存后交易仍保留但金额置灰，月度支出、净额与总现金流均不再计入该交易。
- evidence: step 33 (frame 65 → 66)

### EditTransactionDetails `feature_edittransactiondetails`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 编辑交易可修改自定义标题并切换是否计入金额；保存后列表立即显示新标题并重算月度汇总。
- evidence: step 40 (frame 79 → 80)

### SearchTransactions `feature_searchtransactions`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 搜索框会按标题筛选交易；无匹配时显示专用空状态。
- evidence: step 42 (frame 83 → 84)

### CreateBudget `feature_createbudget`
- status: ClaimStatus.CONFIRMED · confidence: 1.00
- 可新建命名预算并选择支出/储蓄方向、手动加入/全部交易范围、金额、周期、起始日与颜色；保存后与现有预算并列显示独立进度。
- evidence: step 72 (frame 143 → 144)

### CreateAccount `feature_createaccount`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- Users can add a named account with a color, starting balance, decimal precision, and currency. The new account appears in the account list and its starting balance counts as a transaction.
- evidence: step 102 (frame 202 → 203)

### CreateIncomeTransaction `feature_createincometransaction`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- Users can switch the transaction type to Income, choose the income category and target account, enter an amount, and save it. The account balance and transaction count update while an expense budget remains unchanged.
- evidence: step 140 (frame 263 → 264)

### TransferBetweenAccounts `feature_transferbetweenaccounts`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- A transfer moves value between two accounts, creates linked incoming and outgoing rows, and leaves the expense, income and net summary unchanged.
- evidence: step 150 (frame 280 → 281)

### PersistFinanceData `feature_persistfinancedata`
- status: ClaimStatus.CONFIRMED · confidence: 0.99
- After restarting, onboarding stays completed and the two accounts, balances, transactions, budgets and selected Home account remain present. The denied notification prompt is requested again.
- evidence: step 154 (frame 287 → 288)
